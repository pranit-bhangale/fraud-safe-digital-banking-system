-- Migration V2: Role-Based Access Control (RBAC) Schema in Strict BCNF with Database Optimization
CREATE TABLE IF NOT EXISTS roles
(
    id          SMALLINT      NOT NULL,
    name        VARCHAR(20)   NOT NULL,
    description VARCHAR(100),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT ck_roles_name CHECK (name IN ('ADMIN', 'MANAGER', 'CUSTOMER'))
);

-- Seed standardized core banking roles
INSERT INTO roles (id, name, description)
VALUES
    (1, 'ADMIN', 'System Administrator with full management and audit privileges'),
    (2, 'MANAGER', 'Bank Branch Manager with operational review and override authority'),
    (3, 'CUSTOMER', 'Standard Banking Customer with personal account and transfer access')
ON CONFLICT (id) DO NOTHING;

-- User Roles Mapping Table (Optimized Composite Primary Key for O(1) membership lookups)
CREATE TABLE IF NOT EXISTS user_roles
(
    user_id     UUID          NOT NULL,
    role_id     SMALLINT      NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

-- Optimization: Reverse index to rapidly fetch users by role
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);

-- Assign default CUSTOMER role to any existing users
INSERT INTO user_roles (user_id, role_id)
SELECT id, 3 FROM users
ON CONFLICT DO NOTHING;
