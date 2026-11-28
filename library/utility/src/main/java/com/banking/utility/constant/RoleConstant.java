package com.banking.utility.constant;

public final class RoleConstant {

    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String CUSTOMER = "CUSTOMER";

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_MANAGER = "ROLE_MANAGER";
    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";

    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";
    public static final String HAS_ROLE_MANAGER = "hasRole('MANAGER')";
    public static final String HAS_ROLE_CUSTOMER = "hasRole('CUSTOMER')";
    public static final String HAS_ANY_ROLE_STAFF = "hasAnyRole('ADMIN', 'MANAGER')";
    public static final String HAS_ANY_ROLE_AUTHENTICATED = "hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')";

    private RoleConstant() {
        // Utility constant class
    }
}
