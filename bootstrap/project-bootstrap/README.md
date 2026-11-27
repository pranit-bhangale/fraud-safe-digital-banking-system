# Project Bootstrap & Sequential Service Orchestrator (Local Development)

A production-safe, modular Ansible automation framework for local development process management. Designed for orchestrating multi-service polyglot environments and Java microservice ecosystems with **zero Docker dependencies**.

---

## 🌟 Key Features

1. **Pure Local Development (No Docker)**:
   - Designed exclusively for running native microservice processes directly on the host machine.
   - Requires zero Docker daemons, containers, or container runtimes.
2. **Dual Service Managers (`service_manager`)**:
   - `local_process`: Lightweight, non-root process manager for local dev / testing. Runs processes in the background with `nohup`, manages PIDs under `.pids/<service>.pid`, pipes stdout/stderr to `logs/<service>.log`, and guarantees graceful termination (SIGTERM followed by SIGKILL fallback).
   - `systemd`: Linux service manager. Templates service units to `/etc/systemd/system/`, reloads `systemctl`, and manages lifecycle states.
3. **Strict Sequential Startup Order**:
   - Services start in exact defined order: `1 -> 2 -> 3 ... -> N`.
   - Each service must pass its TCP port and HTTP health check (`health_url` with `startup_timeout`) before the next dependent service begins initialization.
4. **Graceful Reverse Teardown**:
   - Teardown executes in reverse order: `N -> ... -> 3 -> 2 -> 1`.
   - Ingress and gateways are taken down first to stop incoming traffic before core downstream services and configuration servers are stopped.
5. **Environment Inheritance**:
   - Injects a `global_env` baseline into every service, merged with service-specific `env` variables.
6. **Dedicated Final Verification (`is_final_check`)**:
   - Allows ingress gateways or synthetic monitors to trigger a dedicated final verification check upon stack completion.
7. **Out-of-the-Box Configurations**:
   - `project.yml`: Default configuration for the Java-based **Fraud-Safe Digital Banking System** (9 microservices: Config Server, Eureka Registry, User Auth, Account, Fraud Detection, Transaction Saga, Payment, Notification, and API Gateway).
   - `project.client.yml`: The generic polyglot configuration (**my-cloud-platform** running Node.js Auth, Python FastAPI, and Go Web Gateway).
   - `project.prod.yml`: Production environment overlay.

---

## 📁 Directory Structure

```text
bootstrap/project-bootstrap/
├── ansible.cfg                    # Optimized Ansible defaults (YAML stdout, local execution)
├── inventory.ini                  # Localhost connection settings
├── Makefile                       # Developer-friendly CLI targets
├── bootstrap.yml                  # Main orchestrator playbook (Stages 1 through 8)
├── teardown.yml                   # Safe reverse teardown playbook
├── project.yml                    # Default Java digital banking configuration (Zero Docker)
├── project.client.yml             # Polyglot client platform configuration
├── project.prod.yml               # Production overlay configuration
├── .gitignore                     # Ignores .secrets/, .pids/, logs/, etc.
├── files/                         # Static files directory
│   └── .gitkeep
├── templates/                     # Jinja2 templates
│   ├── gitignore.j2               # Project gitignore template
│   └── service.systemd.j2         # Systemd unit template
└── roles/                         # Modular Ansible roles
    ├── prerequisites/             # Validates required tools (git, java, python, etc.)
    ├── commands/                  # Pre- and post-setup shell hooks
    ├── scaffold/                  # Filesystem directories, files, symlinks, templates
    ├── secrets_env/               # Idempotent secret generation and .env templating
    ├── git_repo/                  # Git repository init and submodules
    ├── services_manager/          # Sequential service start, health barrier, reverse stop
    └── health_checks/             # Post-boot global port and HTTP health validation
```

---

## 📖 Step-by-Step Usage Guide

Follow these steps to run, inspect, and manage the services locally.

### Step 1: Install Required Tools

Ensure you have Ansible and Git installed on your system.
- **On Linux or Windows WSL2 (Ubuntu)**:
  ```bash
  sudo apt update
  sudo apt install -y ansible git make
  ```
- **Check Java JDK (for Spring Boot banking services)**:
  ```bash
  java -version
  # JDK 21+ or JDK 25 recommended
  ```

---

### Step 2: Open Terminal in the Bootstrap Directory

Navigate directly into `bootstrap/project-bootstrap`:
```bash
cd bootstrap/project-bootstrap
```

---

### Step 3: Choose Your Configuration File

Two ready-to-run configurations are provided:

| Config File | Architecture / Stack | Command |
|---|---|---|
| **`project.yml`** *(Default)* | **Java Digital Banking System** (9 microservices: Config Server, Eureka, User, Account, Fraud, Txn, Payment, Notification, API Gateway) | `make setup` |
| **`project.client.yml`** | **Polyglot Cloud Platform** (Node.js Auth Service + Python FastAPI Backend + Go Web Gateway) | `make setup-client` |

> 💡 **Tip:** To customize ports, timeouts, or environment variables, edit `project.yml` or `project.client.yml` directly.

---

### Step 4: Run a Dry Run Preview (`make check`)

Before launching any processes, run a dry run check to inspect variable substitutions and configuration validation without making actual changes:

```bash
make check
```
*Equivalent direct Ansible command:*
```bash
ansible-playbook bootstrap.yml --check --diff
```

---

### Step 5: Start All Services Sequentially (`make setup`)

Launch the sequential bootstrap. Each service will launch, and the runner will wait for each service's health check before launching the next:

#### Option A: Start Java Digital Banking Services
```bash
make setup
```
*Or via Ansible:*
```bash
ansible-playbook bootstrap.yml
```

#### Option B: Start the Client Polyglot Stack (Node / Python / Go)
```bash
make setup-client
```
*Or via Ansible:*
```bash
ansible-playbook bootstrap.yml -e @project.client.yml
```

#### What happens during execution:
1. **Prerequisites Check**: Verifies required tools (`git`, `java`, etc.).
2. **Environment Files**: Ensures `.env` exists with required variables.
3. **Sequential Service Loop (1 &rarr; N)**:
   - Sets environment (`global_env` + service `env`).
   - Launches process in background with `nohup`.
   - Writes process ID to `.pids/<service-name>.pid`.
   - Streams standard output and errors to `logs/<service-name>.log`.
   - **Health Barrier**: Polls the TCP port and HTTP `health_url` until healthy.
   - **Next service starts only after the previous service is confirmed healthy.**
4. **Final Check**: The edge gateway (`api-gateway` or `web-gateway`) runs a dedicated end-to-end verification probe.
5. **Summary Banner**: Displays active endpoints.

---

### Step 6: Monitor Processes & Tail Logs

#### Check Process Status
To verify which services are currently running:
```bash
make status
```
Output will indicate:
```text
=== Local Process Status ===
  [RUNNING] config-server (PID 14230)
  [RUNNING] service-registry (PID 14355)
  [RUNNING] user-service (PID 14510)
  ...
```

#### View Service Process IDs
Active PIDs are stored in the `.pids/` directory:
```bash
ls -la .pids/
cat .pids/config-server.pid
```

#### Tail Live Logs
All output is captured in real-time under `logs/`:
```bash
# View specific service log
tail -f logs/config-server.log

# View gateway log
tail -f logs/api-gateway.log

# On Windows PowerShell:
Get-Content -Path logs/config-server.log -Wait
```

---

### Step 7: Access Service Endpoints

Once `make setup` completes, open the services in your browser or make API requests:

- **API Gateway (Public Ingress)**: [http://localhost:8080](http://localhost:8080)
- **Eureka Service Registry Dashboard**: [http://localhost:8761](http://localhost:8761)
- **Spring Cloud Config Server**: [http://localhost:8888/actuator/health](http://localhost:8888/actuator/health)
- **User & Auth Service**: [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)
- **Account Service**: [http://localhost:8082/actuator/health](http://localhost:8082/actuator/health)
- **Transaction Service**: [http://localhost:8083/actuator/health](http://localhost:8083/actuator/health)
- **Fraud Detection Service**: [http://localhost:8084/actuator/health](http://localhost:8084/actuator/health)
- **Payment Service**: [http://localhost:8085/actuator/health](http://localhost:8085/actuator/health)
- **Notification Service**: [http://localhost:8086/actuator/health](http://localhost:8086/actuator/health)

---

### Step 8: Targeted Stage Execution (Optional)

If you only want to restart or run specific stages of the playbook:

```bash
# Run only the services stage (starts local services)
make setup TAGS=services

# Run only health checks against currently running services
ansible-playbook bootstrap.yml --tags health

# Run prerequisites verification only
ansible-playbook bootstrap.yml --tags prereq
```

---

### Step 9: Gracefully Stop All Services (`make down`)

To safely shut down the entire stack, run:
```bash
make down
```
*Or via Ansible:*
```bash
ansible-playbook teardown.yml
```

#### Reverse Teardown Order (`N -> ... -> 3 -> 2 -> 1`):
1. **API Gateway shuts down first**: Prevents incoming requests from hitting dependent services.
2. **Business services shut down next**: Transactions and notifications finish gracefully.
3. **Registry and Config Server shut down last**: Core infra remains available until all services exit.
4. **Clean up**: Sends `SIGTERM`, waits for shutdown, applies `SIGKILL` only if stuck, and deletes `.pids/*.pid`.

---

## 🛠️ Troubleshooting

### 1. Port Already in Use (`BindException: Address already in use`)
If a port is already bound by a previous or orphaned process:
- Check what process is listening on the port (e.g. port `8080`):
  ```bash
  # Linux / WSL:
  lsof -i :8080
  # Or:
  ss -tulpn | grep 8080

  # Windows PowerShell:
  netstat -ano | findstr :8080
  ```
- Terminate the orphaned PID:
  ```bash
  kill -9 <PID>
  ```

### 2. Service Startup Timed Out (`startup_timeout`)
If a service takes longer to initialize (e.g., initial Gradle build/compile or download):
- Check the service log immediately to see what Gradle/JVM is doing:
  ```bash
  cat logs/<service-name>.log
  ```
- Increase the timeout in `project.yml`:
  ```yaml
  startup_timeout: 180   # Increase from 90s to 180s
  ```

### 3. Syntax Checking Playbooks
To verify playbook syntax without executing:
```bash
make lint
```
*(Runs `ansible-playbook --syntax-check`)*

---

## ⚙️ Configuration Reference

Each entry in `local_services` in `project.yml` supports:

```yaml
local_services:
  - name: my-service                     # [Required] Service identifier (used for logs, PIDs, systemd)
    display_name: "My Service"           # [Optional] Human-friendly name in logs
    directory: "/path/to/workdir"        # [Required] Working directory for the process
    exec_start: "./gradlew bootRun"      # [Required] Start command
    port: 8080                           # [Optional] TCP port to probe
    health_url: "http://127.0.0.1:8080/health" # [Optional] HTTP health check URL
    startup_timeout: 90                  # [Optional] Timeout in seconds to wait for readiness
    expected_status: [200]               # [Optional] Allowed HTTP status codes (default: [200])
    is_final_check: false                # [Optional] If true, executes dedicated final verification probe
    env:                                 # [Optional] Service-specific environment variables
      PORT: "8080"
```
