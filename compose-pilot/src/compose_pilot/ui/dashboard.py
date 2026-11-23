"""Interactive real-time Terminal UI (TUI) dashboard with live status cards and hotkeys."""

import datetime
import os
import sys
import time
from typing import Optional

from rich.layout import Layout
from rich.live import Live
from rich.panel import Panel
from rich.table import Table
from rich.text import Text

from compose_pilot.constants import SPINNER_FRAMES, Symbols
from compose_pilot.core.controller import LifecycleController
from compose_pilot.models import ProjectConfig, ServiceHealth, ServiceState


class DashboardApp:
    """Live interactive terminal dashboard for Docker Compose lifecycle control."""

    def __init__(self, config: ProjectConfig, controller: LifecycleController) -> None:
        self.config = config
        self.controller = controller
        self.running = True
        self.frame_idx = 0
        self.status_message = "Ready. Press a hotkey to trigger actions."
        self.status_style = "green"
        self._last_health_cache: dict[str, ServiceHealth] = {}
        self._last_poll_time = 0.0

    def get_key_pressed(self) -> Optional[str]:
        """Non-blocking keyboard read supporting Windows and Unix."""
        if sys.platform.startswith("win") or os.name == "nt":
            import msvcrt
            if msvcrt.kbhit():
                try:
                    ch = msvcrt.getch()
                    # Handle special keys
                    if ch in (b"\x00", b"\xe0"):
                        msvcrt.getch()
                        return None
                    return ch.decode("utf-8", errors="ignore").lower()
                except Exception:
                    return None
            return None
        else:
            import select
            dr, _, _ = select.select([sys.stdin], [], [], 0)
            if dr:
                try:
                    return sys.stdin.read(1).lower()
                except Exception:
                    return None
            return None

    def _poll_status(self) -> dict[str, ServiceHealth]:
        """Poll container status every 2 seconds or return cached result."""
        now = time.time()
        if now - self._last_poll_time > 2.0 or not self._last_health_cache:
            try:
                services = self.controller.compose.get_services()
                results = {}
                for svc in services:
                    results[svc] = self.controller.health.inspect_service_health(svc)
                self._last_health_cache = results
                self._last_poll_time = now
            except Exception as e:
                self.status_message = f"Poll error: {e}"
                self.status_style = "red"
        return self._last_health_cache

    def generate_header(self) -> Panel:
        """Create header panel showing project details and time."""
        now_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        files_str = ", ".join(f.name for f in self.config.compose_files) or "default"
        profiles_str = ", ".join(self.config.profiles) if self.config.profiles else "default"

        title_text = Text()
        title_text.append("🚀 COMPOSE-PILOT DASHBOARD ", style="bold cyan")
        title_text.append(f"[{self.config.project_name}]", style="bold yellow")

        sub_text = Text()
        sub_text.append(f"Compose: {files_str} | Profiles: {profiles_str} | Time: {now_str}", style="dim")

        return Panel(
            Text.assemble(title_text, "\n", sub_text),
            border_style="cyan",
            padding=(0, 1),
        )

    def generate_services_table(self, health_map: dict[str, ServiceHealth]) -> Table:
        """Create real-time service health table."""
        table = Table(
            expand=True,
            show_header=True,
            header_style="bold blue",
            border_style="dim",
        )
        table.add_column("Status", width=4, justify="center")
        table.add_column("Service", style="bold white", min_width=16)
        table.add_column("State", min_width=14)
        table.add_column("Container ID", style="dim", width=14)
        table.add_column("Image", style="dim", min_width=22)
        table.add_column("Endpoints", style="cyan", min_width=24)

        metadata = self.controller.compose.inspect_services()
        spinner = SPINNER_FRAMES[self.frame_idx % len(SPINNER_FRAMES)]

        for svc, h in health_map.items():
            meta = metadata.get(svc)
            image_name = meta.image if meta else "-"
            ports = meta.ports if meta else []
            endpoints_str = ", ".join(f"http://localhost:{p}" for p in ports) if ports else "(internal)"

            if h.state == ServiceState.HEALTHY:
                icon = "🟢"
                state_str = "[bold green]Healthy[/bold green]"
            elif h.state == ServiceState.RUNNING:
                icon = "🔵"
                state_str = "[bold green]Running[/bold green]"
            elif h.state == ServiceState.COMPLETED:
                icon = "🟢"
                state_str = "[bold green]Completed[/bold green]"
            elif h.state == ServiceState.STARTING:
                icon = f"[yellow]{spinner}[/yellow]"
                state_str = "[bold yellow]Starting...[/bold yellow]"
            elif h.state == ServiceState.UNHEALTHY:
                icon = "🔴"
                state_str = "[bold red]Unhealthy[/bold red]"
            elif h.state == ServiceState.FAILED:
                icon = "❌"
                state_str = f"[bold red]Failed ({h.exit_code})[/bold red]"
            elif h.state == ServiceState.NOT_FOUND:
                icon = "⚪"
                state_str = "[dim]Not Found[/dim]"
            else:
                icon = "❓"
                state_str = f"[dim]{h.container_status}[/dim]"

            cid_str = (h.container_id or "")[:12] or "-"
            table.add_row(icon, svc, state_str, cid_str, image_name, endpoints_str)

        return table

    def generate_footer(self) -> Panel:
        """Create footer showing status messages and hotkey commands."""
        hotkeys = Text()
        hotkeys.append("[s] ", style="bold yellow")
        hotkeys.append("Start  ", style="white")
        hotkeys.append("[r] ", style="bold yellow")
        hotkeys.append("Restart  ", style="white")
        hotkeys.append("[x] ", style="bold yellow")
        hotkeys.append("Recreate  ", style="white")
        hotkeys.append("[d] ", style="bold yellow")
        hotkeys.append("Down  ", style="white")
        hotkeys.append("[c] ", style="bold yellow")
        hotkeys.append("Clean  ", style="white")
        hotkeys.append("[h] ", style="bold yellow")
        hotkeys.append("Refresh  ", style="white")
        hotkeys.append("[q] ", style="bold yellow")
        hotkeys.append("Quit", style="white")

        msg = Text()
        msg.append("Action Status: ", style="bold")
        msg.append(self.status_message, style=self.status_style)

        return Panel(
            Text.assemble(msg, "\n", hotkeys),
            border_style="dim",
            padding=(0, 1),
        )

    def handle_hotkey(self, key: str) -> None:
        """Execute action corresponding to user hotkey."""
        if key == "q":
            self.running = False
            self.status_message = "Exiting dashboard..."
        elif key == "h":
            self._last_poll_time = 0.0
            self.status_message = "Refreshed service status."
            self.status_style = "cyan"
        elif key == "s":
            self.status_message = "Starting services..."
            self.status_style = "yellow"
            try:
                self.controller.start(force_recreate=False)
                self._last_poll_time = 0.0
                self.status_message = "Services started successfully."
                self.status_style = "green"
            except Exception as e:
                self.status_message = f"Start failed: {e}"
                self.status_style = "red"
        elif key == "r":
            self.status_message = "Restarting services..."
            self.status_style = "yellow"
            try:
                self.controller.restart_service()
                self._last_poll_time = 0.0
                self.status_message = "Restart complete."
                self.status_style = "green"
            except Exception as e:
                self.status_message = f"Restart failed: {e}"
                self.status_style = "red"
        elif key == "x":
            self.status_message = "Force recreating services..."
            self.status_style = "yellow"
            try:
                self.controller.recreate_service()
                self._last_poll_time = 0.0
                self.status_message = "Recreation complete."
                self.status_style = "green"
            except Exception as e:
                self.status_message = f"Recreate failed: {e}"
                self.status_style = "red"
        elif key == "d":
            self.status_message = "Stopping containers..."
            self.status_style = "yellow"
            try:
                self.controller.stop()
                self._last_poll_time = 0.0
                self.status_message = "Containers stopped."
                self.status_style = "cyan"
            except Exception as e:
                self.status_message = f"Stop failed: {e}"
                self.status_style = "red"
        elif key == "c":
            self.status_message = "Clean requested (stopping and purging containers)..."
            self.status_style = "yellow"
            try:
                self.controller.cleaner.clean(clean_volumes=False, clean_images=False, force=True)
                self._last_poll_time = 0.0
                self.status_message = "Clean completed."
                self.status_style = "green"
            except Exception as e:
                self.status_message = f"Clean failed: {e}"
                self.status_style = "red"

    def run(self) -> None:
        """Run the interactive live dashboard loop."""
        layout = Layout()
        layout.split_column(
            Layout(name="header", size=4),
            Layout(name="body"),
            Layout(name="footer", size=4),
        )

        with Live(layout, refresh_per_second=4, screen=True) as live:
            while self.running:
                key = self.get_key_pressed()
                if key:
                    self.handle_hotkey(key)

                health_map = self._poll_status()
                layout["header"].update(self.generate_header())
                layout["body"].update(self.generate_services_table(health_map))
                layout["footer"].update(self.generate_footer())

                self.frame_idx += 1
                time.sleep(0.25)


def launch_dashboard(config: ProjectConfig, controller: LifecycleController) -> None:
    """Entry point for launching the interactive TUI dashboard."""
    app = DashboardApp(config, controller)
    app.run()
