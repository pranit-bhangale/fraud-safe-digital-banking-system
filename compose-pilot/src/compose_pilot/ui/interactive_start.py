"""Interactive real-time Terminal UI for compose-pilot deployment lifecycle.

Provides a live visual dashboard displaying:
- What phase the deployment is currently executing
- Which services are building, starting, and health checking in real time
- Overall completion progress bar with live percentage and elapsed time
- Real-time rolling activity feed
"""

import datetime
import os
import re
import sys
import time
from typing import Optional

from rich import box
from rich.console import Group
from rich.layout import Layout
from rich.live import Live
from rich.panel import Panel
from rich.table import Table
from rich.text import Text

from compose_pilot.constants import SPINNER_FRAMES
from compose_pilot.core.controller import LifecycleController
from compose_pilot.exceptions import (
    BuildError,
    ComposePilotError,
    HealthCheckTimeoutError,
    ProcessCancelledError,
)
from compose_pilot.models import ProjectConfig, ServiceHealth, ServiceState
from compose_pilot.ui.console import PilotConsole
from compose_pilot.ui.tables import render_endpoints_table, render_health_table
from compose_pilot.utils.process import run_command


class InteractiveDeployer:
    """Manages the full real-time interactive UI during compose-pilot start."""

    def __init__(self, config: ProjectConfig, controller: LifecycleController) -> None:
        self.config = config
        self.controller = controller
        self.console = PilotConsole(no_color=config.no_color)
        self.start_time = time.time()
        self.frame_idx = 0
        self.active_phase = "Initializing..."
        self.phase_number = 1
        self.total_phases = 4
        self.overall_percent = 0
        self.events: list[str] = []
        self.services_order: list[str] = []
        self.service_data: dict[str, dict] = {}
        self.running = True
        self.metadata: dict = {}
        self.is_success = False
        self.has_error = False

    def get_key_pressed(self) -> Optional[str]:
        """Non-blocking keyboard read supporting Windows and Unix."""
        if sys.platform.startswith("win") or os.name == "nt":
            try:
                import msvcrt
                if msvcrt.kbhit():
                    ch = msvcrt.getch()
                    if ch in (b"\x00", b"\xe0"):
                        msvcrt.getch()
                        return None
                    return ch.decode("utf-8", errors="ignore").lower()
            except Exception:
                return None
            return None
        else:
            try:
                import select
                dr, _, _ = select.select([sys.stdin], [], [], 0)
                if dr:
                    return sys.stdin.read(1).lower()
            except Exception:
                return None
            return None

    def add_event(self, text: str) -> None:
        """Add a timestamped event message to the live activity feed."""
        now_str = datetime.datetime.now().strftime("%H:%M:%S")
        entry = f"[{now_str}] {text}"
        self.events.append(entry)
        if len(self.events) > 25:
            self.events.pop(0)

    def _render_header(self, term_w: int = 100) -> Panel:
        """Render context-adaptive top summary header banner."""
        now_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        files_str = ", ".join(f.name for f in self.config.compose_files) or "default"
        profiles_str = ", ".join(self.config.profiles) if self.config.profiles else "default"

        title_text = Text()
        title_text.append("🚀 COMPOSE-PILOT LIVE DEPLOYMENT ", style="bold cyan")
        title_text.append(f"[{self.config.project_name}]", style="bold yellow")

        sub_text = Text()
        if term_w < 85:
            sub_text.append(f"Compose: {files_str}  •  {now_str}", style="dim")
        else:
            sub_text.append(f"Compose: {files_str}  •  Profiles: {profiles_str}  •  Time: {now_str}", style="dim")

        border_style = "bold green" if self.is_success else ("bold red" if self.has_error else "cyan")
        return Panel(
            Text.assemble(title_text, "\n", sub_text),
            border_style=border_style,
            box=box.ROUNDED,
            padding=(0, 1),
        )

    def _render_progress_bar(self, term_w: int = 100) -> Panel:
        """Render context-adaptive progress bar, completion percentage, and active phase."""
        elapsed = time.time() - self.start_time
        spinner = SPINNER_FRAMES[self.frame_idx % len(SPINNER_FRAMES)]

        healthy_count = sum(
            1 for s in self.service_data.values()
            if s.get("state") in (ServiceState.HEALTHY, ServiceState.RUNNING, ServiceState.COMPLETED)
        )
        total_services = len(self.services_order) or 1

        pct = self.overall_percent
        # Bar width dynamically scales to viewport width without wrapping
        bar_width = max(10, min(30, term_w - 70))
        filled = int(bar_width * (pct / 100))
        unfilled = bar_width - filled
        bar_char = "━" * filled
        unfilled_char = "─" * unfilled

        progress_text = Text()
        progress_text.append(f" Phase [{self.phase_number}/{self.total_phases}]: ", style="bold")
        progress_text.append(f"{self.active_phase}  ", style="bold cyan")
        if self.has_error:
            progress_text.append("✗\n", style="bold red")
        elif pct >= 100:
            progress_text.append("✓\n", style="bold green")
        else:
            progress_text.append(f"{spinner}\n", style="yellow")

        progress_text.append(" [", style="dim")
        progress_text.append(bar_char, style="bold green" if pct == 100 else "cyan")
        progress_text.append(unfilled_char, style="dim white")
        progress_text.append(f"]  {pct}% Complete", style="bold white")
        progress_text.append(f"  ({healthy_count}/{total_services} Healthy)  •  Elapsed: {elapsed:.1f}s", style="dim cyan")

        border_style = "bold green" if self.is_success else ("bold red" if self.has_error else "blue")
        return Panel(
            progress_text,
            title="Deployment Progress",
            border_style=border_style,
            box=box.ROUNDED,
            padding=(0, 1),
        )

    def _render_services_table(self, term_w: int = 100) -> Table:
        """Render live service execution grid adapting columns to viewport width."""
        show_cid = term_w >= 105
        table = Table(
            expand=True,
            show_header=True,
            header_style="bold bright_blue",
            box=box.ROUNDED,
            border_style="dim",
            padding=(0, 1),
        )
        table.add_column("Status", width=6, justify="center")
        table.add_column("Service", style="bold white", min_width=12)
        table.add_column("Phase / Current State", min_width=18)
        table.add_column("Progress", width=8, justify="center")
        if show_cid:
            table.add_column("Container ID", style="dim", width=12)
        table.add_column("Published Endpoints", style="cyan", overflow="ellipsis")

        spinner = SPINNER_FRAMES[self.frame_idx % len(SPINNER_FRAMES)]

        for svc in self.services_order:
            data = self.service_data.get(svc, {})
            state = data.get("state", ServiceState.UNKNOWN)
            phase_msg = data.get("phase_msg", "Pending")
            cid = data.get("cid", "-")
            endpoints = data.get("endpoints", "(internal)")
            svc_pct = data.get("progress_pct", 0)

            if state in (ServiceState.HEALTHY, ServiceState.COMPLETED):
                icon = "🟢"
                state_str = f"[bold green]{phase_msg}[/bold green]"
            elif state == ServiceState.RUNNING:
                icon = "🔵"
                state_str = f"[bold green]{phase_msg}[/bold green]"
            elif "Building" in phase_msg:
                icon = f"[bold yellow]🔨 {spinner}[/bold yellow]"
                state_str = f"[bold yellow]{phase_msg}[/bold yellow]"
            elif state == ServiceState.STARTING or "Starting" in phase_msg or "Waiting" in phase_msg:
                icon = f"[bold yellow]⏳ {spinner}[/bold yellow]"
                state_str = f"[bold yellow]{phase_msg}[/bold yellow]"
            elif state == ServiceState.FAILED or state == ServiceState.UNHEALTHY:
                icon = "❌"
                state_str = f"[bold red]{phase_msg}[/bold red]"
            else:
                icon = "⚪"
                state_str = f"[dim]{phase_msg}[/dim]"

            pct_str = (
                f"[bold green]{svc_pct}%[/bold green]"
                if svc_pct == 100
                else (f"[yellow]{svc_pct}%[/yellow]" if svc_pct > 0 else f"[dim]{svc_pct}%[/dim]")
            )

            if show_cid:
                table.add_row(icon, svc, state_str, pct_str, cid[:12], endpoints)
            else:
                table.add_row(icon, svc, state_str, pct_str, endpoints)

        return table

    def _render_activity_feed(self, max_lines: int = 5) -> Panel:
        """Render live event activity ticker bounded to avoid vertical spill."""
        visible = self.events[-max_lines:] if self.events else []
        feed_text = Text()
        if not visible:
            feed_text.append("Waiting for deployment events...", style="dim")
        else:
            for i, ev in enumerate(visible):
                end_char = "\n" if i < len(visible) - 1 else ""
                if "✓" in ev or "HEALTHY" in ev or "Healthy" in ev:
                    feed_text.append(f"{ev}{end_char}", style="green")
                elif "FAIL" in ev or "Error" in ev or "failed" in ev or "✗" in ev:
                    feed_text.append(f"{ev}{end_char}", style="bold red")
                elif "Starting" in ev or "Building" in ev or "Rebuilding" in ev:
                    feed_text.append(f"{ev}{end_char}", style="yellow")
                else:
                    feed_text.append(f"{ev}{end_char}", style="dim")

        return Panel(
            feed_text,
            title="📜 Live Activity Feed",
            border_style="dim",
            box=box.ROUNDED,
            padding=(0, 1),
        )

    def _render_footer(self) -> Panel:
        """Render keyboard shortcuts or completion footer based on lifecycle state."""
        elapsed = time.time() - self.start_time
        total_services = len(self.services_order) or 1
        footer_text = Text()

        if self.is_success:
            footer_text.append("✓ ", style="bold green")
            footer_text.append("Deployment Completed Successfully", style="bold green")
            footer_text.append(f"  •  {total_services}/{total_services} Services Healthy  •  Elapsed: {elapsed:.1f}s", style="dim")
            return Panel(footer_text, border_style="bold green", box=box.ROUNDED, padding=(0, 1))

        if self.has_error:
            footer_text.append("✗ ", style="bold red")
            footer_text.append("Deployment Encountered Errors", style="bold red")
            footer_text.append("  •  See diagnostics and logs below", style="dim")
            return Panel(footer_text, border_style="bold red", box=box.ROUNDED, padding=(0, 1))

        footer_text.append("Hotkeys: ", style="bold")
        footer_text.append("[q] ", style="bold yellow")
        footer_text.append("Abort Deployment  ", style="white")
        footer_text.append("•  ", style="dim")
        footer_text.append("[Ctrl+C] ", style="bold yellow")
        footer_text.append("Force Exit", style="dim")
        return Panel(footer_text, border_style="dim", box=box.ROUNDED, padding=(0, 1))

    def _render_view(self) -> Group:
        """Construct context-adaptive composite view without empty voids."""
        size = getattr(self.console, "size", None) or getattr(getattr(self.console, "console", None), "size", None)
        term_w = size.width if size else 100
        term_h = size.height if size else 30

        # Calculate dynamic feed capacity based on terminal height & service count
        num_services = len(self.services_order)
        max_feed_lines = max(2, min(8, term_h - num_services - 13))

        return Group(
            self._render_header(term_w),
            self._render_progress_bar(term_w),
            self._render_services_table(term_w),
            self._render_activity_feed(max_feed_lines),
            self._render_footer(),
        )

    def _render_layout(self) -> Group:
        """Backward-compatible alias for _render_view."""
        return self._render_view()

    def execute(self, force_recreate: bool = False, target_services: Optional[list[str]] = None) -> int:
        """Run the complete interactive start workflow."""
        try:
            self.controller.check_prerequisites()
            self.controller.validate_compose()
            discovered = self.controller.compose.get_services()
            if target_services:
                all_services = [s for s in discovered if s in target_services]
                if not all_services:
                    all_services = target_services
            else:
                all_services = discovered
            self.services_order = all_services
            self.metadata = self.controller.compose.inspect_services()

            for svc in all_services:
                meta = self.metadata.get(svc)
                ports = meta.ports if meta else []
                endpoints_str = " • ".join(f"http://localhost:{p}" for p in ports) if ports else "(internal)"
                self.service_data[svc] = {
                    "state": ServiceState.UNKNOWN,
                    "phase_msg": "Pending",
                    "cid": "-",
                    "endpoints": endpoints_str,
                    "progress_pct": 0,
                }

            target_desc = f" ({', '.join(all_services)})" if target_services else ""
            self.add_event(f"Initialized deployment for project '{self.config.project_name}'{target_desc}")
            self.add_event(f"Active scope: {len(all_services)} service(s)")

            with Live(self._render_layout(), refresh_per_second=4, screen=False) as live:
                # -------------------------------------------------------------
                # STAGE 1: Prerequisite & Validation
                # -------------------------------------------------------------
                self.phase_number = 1
                self.active_phase = "Validating Configuration & Docker Engine"
                self.overall_percent = 10
                self.add_event("Validated Compose syntax and Docker daemon reachability")
                live.update(self._render_layout())
                time.sleep(0.5)

                # -------------------------------------------------------------
                # STAGE 2: Code Hashing & Image Builds
                # -------------------------------------------------------------
                self.phase_number = 2
                self.active_phase = "Inspecting Code Hashes & Building Images"
                self.overall_percent = 25
                live.update(self._render_layout())

                services_to_rebuild: list[str] = []
                services_to_recreate: list[str] = []

                for svc in all_services:
                    if self.get_key_pressed() == "q":
                        raise ProcessCancelledError("Deployment aborted by user.")

                    meta = self.metadata.get(svc)
                    is_buildable = meta.is_buildable if meta else False
                    image_name = self.controller.compose.get_service_image(svc)
                    image_exists = self.controller.compose.image_exists_locally(image_name)
                    container_ids = self.controller.compose.get_container_ids(svc)
                    container_exists = len(container_ids) > 0
                    code_changed = self.controller.hasher.has_code_changed(svc)

                    if force_recreate:
                        if is_buildable:
                            services_to_rebuild.append(svc)
                        services_to_recreate.append(svc)
                        self.service_data[svc]["phase_msg"] = "Force recreate"
                        self.service_data[svc]["progress_pct"] = 25
                    elif is_buildable and not image_exists:
                        services_to_rebuild.append(svc)
                        services_to_recreate.append(svc)
                        self.service_data[svc]["phase_msg"] = "Image missing (Build required)"
                        self.service_data[svc]["progress_pct"] = 20
                        self.add_event(f"Image '{image_name}' missing -> queued build for {svc}")
                    elif is_buildable and code_changed:
                        services_to_rebuild.append(svc)
                        services_to_recreate.append(svc)
                        self.service_data[svc]["phase_msg"] = "Code changed (Build required)"
                        self.service_data[svc]["progress_pct"] = 20
                        self.add_event(f"Source changes detected -> queued build for {svc}")
                    elif not is_buildable and code_changed:
                        services_to_recreate.append(svc)
                        self.service_data[svc]["phase_msg"] = "Config changed"
                        self.service_data[svc]["progress_pct"] = 30
                        self.add_event(f"Configuration changed -> recreate {svc}")
                    elif not container_exists:
                        services_to_recreate.append(svc)
                        self.service_data[svc]["phase_msg"] = "Container missing"
                        self.service_data[svc]["progress_pct"] = 30
                    else:
                        self.service_data[svc]["phase_msg"] = "Up to date (Cache hit)"
                        self.service_data[svc]["progress_pct"] = 40

                    self.frame_idx += 1
                    live.update(self._render_layout())
                    time.sleep(0.08)

                # Execute builds with live spinner if any services need building
                if services_to_rebuild:
                    self.active_phase = f"Building Images for: {', '.join(services_to_rebuild)}"
                    self.overall_percent = 35
                    self.add_event(f"Compiling Docker images: {', '.join(services_to_rebuild)}")

                    for b_svc in services_to_rebuild:
                        self.service_data[b_svc]["phase_msg"] = "Building image..."

                    build_start = time.time()
                    proc = self.controller.compose.get_base_cmd() + ["build"] + services_to_rebuild
                    import subprocess
                    sub = subprocess.Popen(proc, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)

                    while sub.poll() is None:
                        if self.get_key_pressed() == "q":
                            sub.terminate()
                            raise ProcessCancelledError("Build cancelled by user.")

                        b_elapsed = time.time() - build_start
                        for b_svc in services_to_rebuild:
                            self.service_data[b_svc]["phase_msg"] = f"Building image ({b_elapsed:.0f}s)..."

                        self.frame_idx += 1
                        live.update(self._render_layout())
                        time.sleep(0.25)

                    if sub.returncode != 0:
                        out = sub.stdout.read() if sub.stdout else ""
                        raise BuildError(f"Docker build failed for: {', '.join(services_to_rebuild)}", build_log=out)

                    for b_svc in services_to_rebuild:
                        self.controller.hasher.save_service_hash(b_svc)
                        self.service_data[b_svc]["phase_msg"] = "Build complete"
                        self.service_data[b_svc]["progress_pct"] = 50

                    self.add_event("✓ Image builds finished successfully")

                # Optional push
                if self.config.push:
                    self.active_phase = "Pushing Images to Registry"
                    self.add_event("Pushing built images to remote registry...")
                    self.controller.registry.push_all()

                # -------------------------------------------------------------
                # STAGE 3: Launch Containers
                # -------------------------------------------------------------
                self.phase_number = 3
                self.active_phase = "Launching Containers in Detached Mode"
                self.overall_percent = 55
                self.add_event("Executing 'docker compose up -d'...")

                for svc in all_services:
                    self.service_data[svc]["phase_msg"] = "Launching container..."
                    self.service_data[svc]["progress_pct"] = 55

                live.update(self._render_layout())

                # Proactively clean any conflicting containers from previous/different project runs
                stale_removed = self.controller.compose.remove_conflicting_containers()
                if stale_removed:
                    self.add_event(f"Cleaned up {len(stale_removed)} conflicting container(s)")

                if services_to_recreate:
                    recreate_cmd = self.controller.compose.get_base_cmd() + ["up", "-d", "--force-recreate"] + services_to_recreate
                    run_command(recreate_cmd, check=False)

                up_cmd = self.controller.compose.get_base_cmd() + ["up", "-d"]
                if target_services:
                    up_cmd.extend(all_services)
                res = run_command(up_cmd, check=False)
                if res.returncode != 0:
                    err_text = (res.stderr or "") + "\n" + (res.stdout or "")
                    pattern = r'Conflict\.\s+The container name\s+["\']?/?([a-zA-Z0-9_.-]+)["\']?\s+is already in use by container\s+["\']?([a-zA-Z0-9]+)["\']?'
                    conflicts = re.findall(pattern, err_text)
                    if conflicts:
                        for c_name, c_id in conflicts:
                            self.add_event(f"Resolving conflict: removing {c_name} ({c_id[:12]})...")
                            run_command(["docker", "rm", "-f", c_id], check=False)
                        time.sleep(0.5)
                        res = run_command(up_cmd, check=False)

                    if res.returncode != 0:
                        raise ComposePilotError(f"Failed to start containers:\n{res.stderr or res.stdout}")

                for svc in services_to_recreate:
                    self.controller.hasher.save_service_hash(svc)

                if target_services:
                    self.add_event(f"✓ Target container(s) launched in detached mode: {', '.join(all_services)}")
                else:
                    self.add_event("✓ All containers launched in detached mode")

                # Update container IDs
                for svc in all_services:
                    cids = self.controller.compose.get_container_ids(svc)
                    if cids:
                        self.service_data[svc]["cid"] = cids[0]
                    self.service_data[svc]["phase_msg"] = "Waiting for healthcheck..."
                    self.service_data[svc]["progress_pct"] = 65

                # -------------------------------------------------------------
                # STAGE 4: Health Gating & Readiness Polling
                # -------------------------------------------------------------
                self.phase_number = 4
                self.active_phase = "Monitoring Service Health & Readiness"
                poll_start = time.time()
                healthy_reported: set[str] = set()

                while True:
                    if self.get_key_pressed() == "q":
                        self.has_error = True
                        live.update(self._render_view())
                        raise ProcessCancelledError("Deployment cancelled by user.")

                    elapsed_wait = int(time.time() - poll_start)
                    all_done = True
                    has_failed = False

                    for svc in all_services:
                        h = self.controller.health.inspect_service_health(svc)
                        self.service_data[svc]["state"] = h.state
                        if h.container_id:
                            self.service_data[svc]["cid"] = h.container_id

                        if h.state in (ServiceState.HEALTHY, ServiceState.RUNNING, ServiceState.COMPLETED):
                            if svc not in healthy_reported:
                                healthy_reported.add(svc)
                                self.add_event(f"✓ {svc} is HEALTHY and ready")
                            self.service_data[svc]["phase_msg"] = f"Healthy ({h.health_status})"
                            self.service_data[svc]["progress_pct"] = 100
                        elif h.state == ServiceState.STARTING:
                            all_done = False
                            self.service_data[svc]["phase_msg"] = f"Starting ({elapsed_wait}s)..."
                            self.service_data[svc]["progress_pct"] = min(95, 65 + (elapsed_wait * 2))
                        elif h.state.is_terminal_failure:
                            has_failed = True
                            self.service_data[svc]["phase_msg"] = f"Failed (Exit: {h.exit_code})"
                            self.service_data[svc]["progress_pct"] = 0
                            self.add_event(f"✗ {svc} failed with exit code {h.exit_code}")
                        else:
                            all_done = False
                            self.service_data[svc]["phase_msg"] = f"Pending ({h.container_status})"

                    k = len(healthy_reported)
                    n = len(all_services)
                    self.overall_percent = min(100, 65 + int(35 * (k / max(1, n))))

                    self.frame_idx += 1
                    live.update(self._render_layout())

                    if all_done:
                        self.overall_percent = 100
                        self.active_phase = "Deployment Complete"
                        self.is_success = True
                        self.add_event("✓ All services successfully reached healthy state!")
                        live.update(self._render_view())
                        time.sleep(0.75)
                        break

                    if has_failed:
                        self.has_error = True
                        live.update(self._render_view())
                        failed_list = [
                            s for s, d in self.service_data.items()
                            if d.get("state") in (ServiceState.FAILED, ServiceState.UNHEALTHY)
                        ]
                        raise HealthCheckTimeoutError(
                            f"One or more services failed during startup: {', '.join(failed_list)}",
                            failed_services=failed_list,
                        )

                    if elapsed_wait > self.config.max_wait:
                        pending_list = [
                            s for s in all_services if s not in healthy_reported
                        ]
                        raise HealthCheckTimeoutError(
                            f"Timeout ({self.config.max_wait}s) exceeded waiting for services: {', '.join(pending_list)}",
                            failed_services=pending_list,
                        )

                    time.sleep(0.5)

            total_elapsed = time.time() - self.start_time
            self.console.print()
            svc_label = f"({', '.join(all_services)})" if target_services else f"({len(all_services)} services)"
            self.console.success(
                f"[bold green]{self.config.project_name} deployment completed successfully![/bold green] "
                f"{svc_label} healthy in {total_elapsed:.1f}s\n"
            )

            endpoints = self.controller.get_endpoints()
            if target_services:
                endpoints = [e for e in endpoints if e[0] in all_services]
            render_endpoints_table(self.console, self.config.project_name, endpoints)

            self.console.print()
            self.console.tip("Live Logs Tip", "compose-pilot live-logs [service]")
            self.console.tip("TUI Dashboard", "compose-pilot ui")
            self.console.tip("Push Tip     ", "compose-pilot push [service] -r <registry>")
            self.console.tip("Clean Tip    ", "compose-pilot clean")
            self.console.print()
            return 0

        except HealthCheckTimeoutError as e:
            self.console.print()
            self.console.error(f"Deployment failed: {e.message}\n")
            for svc in e.failed_services:
                self.console.minor_section(f"🔍 Recent logs for {svc} (last 25 lines):")
                diag = self.controller.health.get_diagnostic_logs(svc)
                self.console.print(diag)
                self.console.print()
            self.console.print("To inspect full live logs:")
            self.console.print("[cyan]compose-pilot live-logs <service>[/cyan]\n")
            return 1

        except ProcessCancelledError:
            self.console.print("\n[dim yellow]Deployment aborted by user.[/dim yellow]")
            return 130

        except ComposePilotError as e:
            self.console.error(e.message)
            return e.exit_code

        except KeyboardInterrupt:
            self.console.print("\n[dim yellow]Deployment interrupted.[/dim yellow]")
            return 130


def run_interactive_deployment(
    config: ProjectConfig,
    controller: LifecycleController,
    force_recreate: bool = False,
    target_services: Optional[list[str]] = None,
) -> int:
    """Launch the interactive deployment UI."""
    deployer = InteractiveDeployer(config, controller)
    return deployer.execute(force_recreate=force_recreate, target_services=target_services)
