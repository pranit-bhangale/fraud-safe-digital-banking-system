"""Live log follower with dual streaming and local file teeing."""

import subprocess
import sys
from pathlib import Path
from typing import Optional

from compose_pilot.core.compose import ComposeManager
from compose_pilot.exceptions import ServiceNotFoundError
from compose_pilot.models import ProjectConfig
from compose_pilot.ui.console import PilotConsole
from compose_pilot.ui.prompts import prompt_service_selection
from compose_pilot.utils.process import ProcessManager


def stream_live_logs(
    config: ProjectConfig,
    compose: ComposeManager,
    console: PilotConsole,
    service: Optional[str] = None,
    tail_lines: int = 100,
    save_to_file: bool = False,
) -> None:
    """Stream live logs from Docker Compose with optional teeing to disk."""
    all_services = compose.get_services()

    # Interactive service picker if not specified
    if not service and sys.stdin.isatty():
        chosen = prompt_service_selection(console, all_services)
        service = chosen or None

    if service:
        if service not in all_services:
            raise ServiceNotFoundError(service, all_services)

    config.logs_dir.mkdir(parents=True, exist_ok=True)

    cmd = compose.get_base_cmd() + ["logs", "-f", f"--tail={tail_lines}", "-t"]
    if service:
        cmd.append(service)

    log_file_path: Optional[Path] = None
    if save_to_file:
        file_name = f"{service}.log" if service else "all.log"
        log_file_path = config.logs_dir / file_name

    if service:
        console.major_section(f"📜 Live Logs: {service}")
        console.info(f"Streaming live logs for '{service}' (Press Ctrl+C to stop)...")
    else:
        console.major_section("📜 Live Logs: All Services")
        console.info("Streaming live logs for all services (Press Ctrl+C to stop)...")

    if log_file_path:
        console.success(f"Also saving logs to: {log_file_path}")
    elif service:
        console.tip("Tip", f"compose-pilot live-logs {service} --save to also save to file")

    console.print()

    # Open log file if requested
    log_fp = None
    if log_file_path:
        log_fp = open(log_file_path, "a", encoding="utf-8")

    mgr = ProcessManager.get()
    proc = subprocess.Popen(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )
    mgr.register_process(proc)

    try:
        if proc.stdout:
            for line in iter(proc.stdout.readline, ""):
                sys.stdout.write(line)
                sys.stdout.flush()
                if log_fp:
                    log_fp.write(line)
                    log_fp.flush()
        proc.wait()
    except KeyboardInterrupt:
        console.print("\n[dim]Stopped log streaming.[/dim]")
    finally:
        mgr.unregister_process(proc)
        if proc.poll() is None:
            proc.terminate()
        if log_fp:
            log_fp.close()
