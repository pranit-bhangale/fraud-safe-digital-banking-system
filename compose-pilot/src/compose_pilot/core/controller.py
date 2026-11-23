"""Master production lifecycle controller orchestrating builds, recreation, health, and status."""

import subprocess
import tempfile
import time
from pathlib import Path
from typing import Callable, Optional

from compose_pilot.constants import SPINNER_FRAMES
from compose_pilot.core.cleaner import EnvironmentCleaner
from compose_pilot.core.compose import ComposeManager
from compose_pilot.core.hasher import ServiceHasher
from compose_pilot.core.health import HealthMonitor
from compose_pilot.core.registry import RegistryManager
from compose_pilot.exceptions import (
    BuildError,
    ComposePilotError,
    HealthCheckTimeoutError,
    ServiceNotFoundError,
)
from compose_pilot.models import (
    ChangeDetectionResult,
    ProjectConfig,
    ServiceHealth,
    ServiceMetadata,
    ServiceState,
)
from compose_pilot.utils.process import ProcessManager, run_command


class LifecycleController:
    """Production Docker Compose lifecycle manager."""

    def __init__(self, config: ProjectConfig) -> None:
        self.config = config
        self.compose = ComposeManager(config)
        self.hasher = ServiceHasher(config, self.compose)
        self.health = HealthMonitor(config, self.compose)
        self.registry = RegistryManager(config, self.compose, self.hasher)
        self.cleaner = EnvironmentCleaner(config, self.compose)

    def check_prerequisites(self, check_daemon: bool = True) -> None:
        """Validate Docker CLI, daemon, and compose/env files."""
        self.compose.check_prerequisites(check_daemon=check_daemon)

    def validate_compose(self) -> str:
        """Validate Compose configuration."""
        return self.compose.validate_compose()

    def get_services_metadata(self) -> dict[str, ServiceMetadata]:
        """Get parsed service metadata."""
        return self.compose.inspect_services()

    def check_and_prepare_services(
        self,
        force_recreate: bool = False,
        status_logger: Optional[Callable[[str, str], None]] = None,
        spinner_callback: Optional[Callable[[str], None]] = None,
    ) -> ChangeDetectionResult:
        """Inspect all services, compute changes, rebuild dirty images, and recreate modified containers."""
        def log(svc: str, msg: str, level: str = "info"):
            if status_logger:
                status_logger(f"{svc}: {msg}", level)

        self.config.cache_dir.mkdir(parents=True, exist_ok=True)
        self.config.logs_dir.mkdir(parents=True, exist_ok=True)

        all_services = self.compose.get_services()
        if not all_services:
            raise ComposePilotError("No services found in Compose configuration.")

        services_to_rebuild: list[str] = []
        services_to_recreate: list[str] = []
        up_to_date: list[str] = []
        reasons: dict[str, str] = {}

        metadata = self.compose.inspect_services()

        for service in all_services:
            meta = metadata.get(service)
            is_buildable = meta.is_buildable if meta else False
            image_name = self.compose.get_service_image(service)

            image_exists = self.compose.image_exists_locally(image_name)
            container_ids = self.compose.get_container_ids(service)
            container_exists = len(container_ids) > 0
            code_changed = self.hasher.has_code_changed(service)

            if force_recreate:
                log(service, "Force recreate requested", "warning")
                if is_buildable:
                    services_to_rebuild.append(service)
                services_to_recreate.append(service)
                reasons[service] = "Force recreate"
            elif is_buildable and not image_exists:
                log(service, f"Docker image '{image_name}' not found (build required)", "warning")
                services_to_rebuild.append(service)
                services_to_recreate.append(service)
                reasons[service] = "Image missing"
            elif is_buildable and code_changed:
                log(service, "Source code changes detected (rebuild & recreate required)", "warning")
                services_to_rebuild.append(service)
                services_to_recreate.append(service)
                reasons[service] = "Source changed"
            elif not is_buildable and code_changed:
                log(service, "Configuration changes detected (recreate required)", "warning")
                services_to_recreate.append(service)
                reasons[service] = "Config changed"
            elif not container_exists:
                log(service, "Container missing (will be created)", "success")
                services_to_recreate.append(service)
                reasons[service] = "Container missing"
            else:
                log(service, "Up to date", "success")
                up_to_date.append(service)
                reasons[service] = "Up to date"

        # Execute builds if needed
        if services_to_rebuild:
            self._execute_builds(services_to_rebuild, spinner_callback)

        # Execute recreations if needed
        if services_to_recreate:
            up_cmd = self.compose.get_base_cmd() + ["up", "-d", "--force-recreate"] + services_to_recreate
            res = run_command(up_cmd, check=False)
            if res.returncode != 0:
                raise ComposePilotError(f"Failed to recreate containers:\n{res.stderr or res.stdout}")

            for svc in services_to_recreate:
                self.hasher.save_service_hash(svc)

        return ChangeDetectionResult(
            services_to_rebuild=services_to_rebuild,
            services_to_recreate=services_to_recreate,
            up_to_date=up_to_date,
            reasons=reasons,
        )

    def _execute_builds(
        self,
        services: list[str],
        spinner_callback: Optional[Callable[[str], None]] = None,
    ) -> None:
        """Run docker compose build with live spinner animation and log capture."""
        build_cmd = self.compose.get_base_cmd() + ["build"] + services
        temp_log = tempfile.NamedTemporaryFile(
            prefix=f"{self.config.project_name}_build_", suffix=".log", delete=False
        )
        temp_log_path = Path(temp_log.name)
        ProcessManager.get().register_temp_file(temp_log_path)

        frame = 0
        proc = subprocess.Popen(
            build_cmd,
            stdout=temp_log,
            stderr=subprocess.STDOUT,
            text=True,
        )
        ProcessManager.get().register_process(proc)

        try:
            while proc.poll() is None:
                if spinner_callback:
                    sp = SPINNER_FRAMES[frame % len(SPINNER_FRAMES)]
                    spinner_callback(f"{sp} Compiling and building [{', '.join(services)}]")
                    frame += 1
                time.sleep(0.1)

            ret = proc.wait()
            temp_log.close()
            ProcessManager.get().unregister_process(proc)

            if ret != 0:
                log_contents = ""
                try:
                    log_contents = temp_log_path.read_text("utf-8")
                except Exception:
                    pass
                raise BuildError(
                    f"Docker image build failed for: {', '.join(services)}",
                    build_log=log_contents,
                )

            for svc in services:
                self.hasher.save_service_hash(svc)
        finally:
            try:
                temp_log_path.unlink(missing_ok=True)
            except OSError:
                pass

    def start_containers(self) -> None:
        """Start all services in detached mode."""
        cmd = self.compose.get_base_cmd() + ["up", "-d"]
        res = run_command(cmd, check=False)
        if res.returncode != 0:
            raise ComposePilotError(f"Failed to start containers:\n{res.stderr or res.stdout}")

    def wait_for_services(
        self,
        services: Optional[list[str]] = None,
        progress_callback: Optional[Callable[[int, int, str], None]] = None,
    ) -> dict[str, ServiceHealth]:
        """Poll service health and verify all services reach healthy/running state."""
        results = self.health.poll_health(services, progress_callback)
        failed_services = [
            svc for svc, h in results.items() if not h.state.is_success
        ]
        if failed_services:
            raise HealthCheckTimeoutError(
                f"One or more services failed health checks: {', '.join(failed_services)}",
                failed_services=failed_services,
            )
        return results

    def restart_service(self, target_service: Optional[str] = None) -> None:
        """Smart restart: if code changed, rebuild and recreate; else restart."""
        all_services = self.compose.get_services()
        if target_service:
            if target_service not in all_services:
                raise ServiceNotFoundError(target_service, all_services)

            meta = self.compose.inspect_services().get(target_service)
            is_buildable = meta.is_buildable if meta else False
            code_changed = self.hasher.has_code_changed(target_service)

            if is_buildable and code_changed:
                build_cmd = self.compose.get_base_cmd() + ["build", target_service]
                run_command(build_cmd, check=True)
                up_cmd = self.compose.get_base_cmd() + [
                    "up", "-d", "--no-deps", "--force-recreate", target_service
                ]
                run_command(up_cmd, check=True)
                self.hasher.save_service_hash(target_service)
            else:
                restart_cmd = self.compose.get_base_cmd() + ["restart", target_service]
                run_command(restart_cmd, check=True)

            self.wait_for_services([target_service])
        else:
            down_cmd = self.compose.get_base_cmd() + ["down"]
            run_command(down_cmd, check=True)
            self.start(force_recreate=False)

    def recreate_service(self, target_service: Optional[str] = None) -> None:
        """Force rebuild and recreate container(s)."""
        all_services = self.compose.get_services()
        if target_service:
            if target_service not in all_services:
                raise ServiceNotFoundError(target_service, all_services)

            meta = self.compose.inspect_services().get(target_service)
            if meta and meta.is_buildable:
                build_cmd = self.compose.get_base_cmd() + ["build", target_service]
                run_command(build_cmd, check=True)
                self.hasher.save_service_hash(target_service)

            up_cmd = self.compose.get_base_cmd() + [
                "up", "-d", "--no-deps", "--force-recreate", target_service
            ]
            run_command(up_cmd, check=True)
            self.wait_for_services([target_service])
        else:
            self.check_and_prepare_services(force_recreate=True)
            self.start_containers()
            self.wait_for_services()

    def stop(self) -> None:
        """Stop and remove project containers."""
        cmd = self.compose.get_base_cmd() + ["down"]
        res = run_command(cmd, check=False)
        if res.returncode != 0:
            raise ComposePilotError(f"Failed to stop containers:\n{res.stderr or res.stdout}")

    def get_endpoints(self) -> list[tuple[str, list[str]]]:
        """Return list of (service_name, [urls_or_network_status])."""
        endpoints: list[tuple[str, list[str]]] = []
        metadata = self.compose.inspect_services()

        for svc in self.compose.get_services():
            meta = metadata.get(svc)
            ports = meta.ports if meta else []
            if ports:
                urls = []
                for p in ports:
                    scheme = "https" if p in ("443", "8443") else "http"
                    urls.append(f"{scheme}://localhost:{p}")
                endpoints.append((svc, urls))
            else:
                endpoints.append((svc, ["(internal network / no published ports)"]))

        return endpoints

    def start(self, force_recreate: bool = False) -> None:
        """Execute full start deployment pipeline."""
        self.check_prerequisites()
        self.validate_compose()
        self.check_and_prepare_services(force_recreate=force_recreate)

        if self.config.push:
            self.registry.push_all()

        self.start_containers()
        self.wait_for_services()
