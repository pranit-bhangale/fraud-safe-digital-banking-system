"""Service health check monitoring, container inspection, and diagnostic reporting."""

import time
from typing import Callable, Optional

from compose_pilot.constants import (
    DIAGNOSTIC_LOG_TAIL,
    SPINNER_FRAMES,
)
from compose_pilot.core.compose import ComposeManager
from compose_pilot.exceptions import HealthCheckTimeoutError
from compose_pilot.models import ProjectConfig, ServiceHealth, ServiceState
from compose_pilot.utils.process import run_command


class HealthMonitor:
    """Monitors container execution status, health check states, and diagnostic log collection."""

    def __init__(self, config: ProjectConfig, compose: ComposeManager) -> None:
        self.config = config
        self.compose = compose

    def inspect_service_health(self, service: str) -> ServiceHealth:
        """Inspect the current health and status of a single service."""
        container_ids = self.compose.get_container_ids(service)
        if not container_ids:
            return ServiceHealth(
                service=service,
                state=ServiceState.NOT_FOUND,
                container_status="not_found",
            )

        # In case multiple replicas exist, evaluate combined status
        any_failed = False
        any_unhealthy = False
        any_pending = False
        all_completed = True
        all_healthy = True
        has_running = False

        latest_exit_code = 0
        primary_cid = container_ids[0]
        primary_raw = ""

        for cid in container_ids:
            inspect_cmd = [
                "docker", "inspect",
                "--format",
                "{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}|{{.State.ExitCode}}",
                cid,
            ]
            res = run_command(inspect_cmd, check=False)
            if res.returncode != 0:
                any_failed = True
                continue

            info = res.stdout.strip()
            primary_raw = info
            parts = info.split("|")
            status = parts[0] if len(parts) > 0 else "unknown"
            health = parts[1] if len(parts) > 1 else "none"
            try:
                exit_code = int(parts[2]) if len(parts) > 2 else 0
            except ValueError:
                exit_code = 1

            latest_exit_code = exit_code

            if status == "running":
                all_completed = False
                if health == "healthy":
                    has_running = True
                elif health == "starting":
                    any_pending = True
                    all_healthy = False
                elif health == "unhealthy":
                    any_unhealthy = True
                    all_healthy = False
                elif health == "none":
                    has_running = True
                else:
                    any_pending = True
                    all_healthy = False
            elif status in ("created", "restarting"):
                any_pending = True
                all_completed = False
                all_healthy = False
            elif status == "exited":
                all_healthy = False
                if exit_code != 0:
                    any_failed = True
            elif status == "dead":
                any_failed = True
                all_healthy = False
            else:
                any_pending = True
                all_completed = False
                all_healthy = False

        if any_failed:
            state = ServiceState.FAILED
        elif any_unhealthy:
            state = ServiceState.UNHEALTHY
        elif any_pending:
            state = ServiceState.STARTING
        elif all_completed:
            state = ServiceState.COMPLETED
        elif all_healthy and has_running:
            state = ServiceState.HEALTHY
        elif has_running:
            state = ServiceState.RUNNING
        else:
            state = ServiceState.STARTING

        return ServiceHealth(
            service=service,
            state=state,
            container_id=primary_cid,
            container_status=parts[0] if parts else "unknown",
            health_status=parts[1] if len(parts) > 1 else "none",
            exit_code=latest_exit_code,
            raw_info=primary_raw,
        )

    def poll_health(
        self,
        services: Optional[list[str]] = None,
        progress_callback: Optional[Callable[[int, int, str], None]] = None,
    ) -> dict[str, ServiceHealth]:
        """Poll service health until all services reach terminal or healthy state, or timeout expires."""
        if not services:
            services = self.compose.get_services()

        start_time = time.time()
        results: dict[str, ServiceHealth] = {}
        frame_idx = 0

        while True:
            elapsed = int(time.time() - start_time)
            all_done = True
            terminal_failure = False

            if progress_callback:
                spinner = SPINNER_FRAMES[frame_idx % len(SPINNER_FRAMES)]
                msg = f"{spinner} Checking service health... ({elapsed}s / {self.config.max_wait}s)"
                progress_callback(elapsed, self.config.max_wait, msg)
                frame_idx += 1

            for service in services:
                health = self.inspect_service_health(service)
                results[service] = health

                if health.state.is_terminal_failure:
                    terminal_failure = True
                elif health.state == ServiceState.STARTING or health.state == ServiceState.NOT_FOUND:
                    all_done = False

            if terminal_failure or all_done:
                break

            if elapsed >= self.config.max_wait:
                break

            time.sleep(self.config.interval)

        return results

    def get_diagnostic_logs(self, service: str, tail: int = DIAGNOSTIC_LOG_TAIL) -> str:
        """Fetch recent tail logs for a failed or unhealthy service."""
        cmd = self.compose.get_base_cmd() + ["logs", f"--tail={tail}", service]
        res = run_command(cmd, check=False)
        return res.stdout or res.stderr
