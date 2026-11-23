"""Command-line interface, contextual argument parsing, and command dispatch."""

import argparse
import sys
from pathlib import Path
from typing import Optional, Sequence

from compose_pilot.config import (
    build_project_config,
    detect_compose_files,
    detect_project_name,
)
from compose_pilot.constants import DEFAULT_INTERVAL, DEFAULT_MAX_WAIT
from compose_pilot.core.controller import LifecycleController
from rich.markup import escape as rich_escape
from compose_pilot.exceptions import (
    ComposePilotError,
    HealthCheckTimeoutError,
    ProcessCancelledError,
)
from compose_pilot.ui.console import PilotConsole
from compose_pilot.ui.dashboard import launch_dashboard
from compose_pilot.ui.logs import stream_live_logs
from compose_pilot.ui.prompts import (
    prompt_clean_confirmation,
    prompt_clean_scope,
    prompt_docker_login,
)
from compose_pilot.ui.tables import (
    render_config_table,
    render_endpoints_table,
    render_health_table,
)


def create_parser() -> argparse.ArgumentParser:
    """Construct command-line argument parser."""
    parser = argparse.ArgumentParser(
        prog="compose-pilot",
        description="Production Docker Compose Controller with smart rebuilds, health checks, and TUI.",
        add_help=False,
    )

    # Global Options (can appear before or after command)
    parser.add_argument(
        "-f", "--file",
        dest="compose_files",
        action="append",
        help="Specify an alternate Compose file (default: docker-compose.yaml or docker-compose.yml)",
    )
    parser.add_argument(
        "--profile",
        dest="profiles",
        action="append",
        help="Specify an active Compose profile name (repeatable)",
    )
    parser.add_argument(
        "--env-file",
        dest="env_file",
        help="Specify an alternate environment file (default: .env)",
    )
    parser.add_argument(
        "-p", "--project-name",
        dest="project_name",
        help="Specify an alternate project name (default: from compose or dir)",
    )
    parser.add_argument(
        "-r", "--registry",
        dest="registry",
        help="Specify target Docker registry (e.g. ghcr.io/org, docker.io/user)",
    )
    parser.add_argument(
        "-t", "--tag",
        dest="tags",
        action="append",
        help="Specify image tag(s) to push (comma-separated or repeatable)",
    )
    parser.add_argument(
        "--push",
        action="store_true",
        help="Push images to registry during deployment (start --push)",
    )
    parser.add_argument(
        "--no-build",
        action="store_true",
        help="Skip rebuilding images before pushing",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Preview tagging and push operations without executing",
    )
    parser.add_argument(
        "--max-wait",
        type=int,
        default=DEFAULT_MAX_WAIT,
        help=f"Max seconds to wait for service health (default: {DEFAULT_MAX_WAIT})",
    )
    parser.add_argument(
        "--interval",
        type=int,
        default=DEFAULT_INTERVAL,
        help=f"Polling interval for health check (default: {DEFAULT_INTERVAL})",
    )
    parser.add_argument(
        "--no-color",
        action="store_true",
        help="Disable ANSI color codes and emojis (auto-detected in non-TTY)",
    )
    parser.add_argument(
        "-h", "--help",
        action="store_true",
        help="Show help message and exit",
    )

    return parser


def parse_contextual_args(raw_argv: Sequence[str]) -> tuple[argparse.Namespace, list[str]]:
    """Handle contextual flags like clean -f (which means --force) vs -f <file>
    and allow flags both before and after subcommands (matching bash behavior).
    """
    preprocessed: list[str] = []
    i = 0
    detected_command: Optional[str] = None
    positional_args: list[str] = []

    # Two-pass parsing: first handle contextual -f
    while i < len(raw_argv):
        arg = raw_argv[i]
        if not arg.startswith("-"):
            if detected_command is None:
                detected_command = arg
            else:
                positional_args.append(arg)
            preprocessed.append(arg)
            i += 1
            continue

        if arg == "-f":
            if detected_command in ("clean", "purge", "prune", "stop"):
                # Interpret as --force for clean/stop
                preprocessed.append("--force")
                i += 1
                continue
            elif i + 1 < len(raw_argv) and not raw_argv[i + 1].startswith("-"):
                # Global -f <file>
                preprocessed.extend(["-f", raw_argv[i + 1]])
                i += 2
                continue
            elif detected_command is not None:
                preprocessed.append("--force")
                i += 1
                continue

        preprocessed.append(arg)
        i += 1

    parser = create_parser()
    parsed, unknown = parser.parse_known_args(preprocessed)

    # Extract command and remaining positional/unknown arguments
    command = None
    extra: list[str] = []

    for item in unknown:
        if not item.startswith("-") and command is None:
            command = item
        else:
            extra.append(item)

    parsed.command = command
    return parsed, extra


def print_usage(console: PilotConsole, controller: Optional[LifecycleController] = None) -> None:
    """Print comprehensive usage guide matching original bash interface with discovered services."""
    proj_name = controller.config.project_name if controller else "compose-pilot"
    
    console.print()
    if console.color_enabled:
        console.print(f"[bold]Production Docker Compose Controller [{rich_escape(proj_name)}][/bold]")
        console.print(f"Usage: compose-pilot [OPTIONS] [COMMAND] [ARGS...]\n", markup=False)
        console.print("[info]Global Options:[/info]")
    else:
        console.print(f"Production Docker Compose Controller [{proj_name}]", markup=False)
        console.print("Usage: compose-pilot [OPTIONS] [COMMAND] [ARGS...]\n", markup=False)
        console.print("Global Options:", markup=False)

    global_opts = [
        ("-f, --file <file>", "Specify an alternate Compose file (default: docker-compose.yaml or .yml)"),
        ("    --env-file <file>", "Specify an alternate environment file (default: .env)"),
        ("-p, --project-name <name>", "Specify an alternate project name (default: from compose or dir)"),
        ("-r, --registry <url>", "Specify target Docker registry (e.g. ghcr.io/org, docker.io/user)"),
        ("-t, --tag <tag>", "Specify image tag(s) to push (comma-separated or repeatable)"),
        ("    --push", "Push images to registry during deployment (start --push)"),
        ("    --no-build", "Skip rebuilding images before pushing"),
        ("    --dry-run", "Preview tagging and push operations without executing"),
        ("    --max-wait <seconds>", "Max seconds to wait for service health (default: 120)"),
        ("    --interval <seconds>", "Polling interval for health check (default: 3)"),
        ("    --no-color", "Disable ANSI color codes and emojis (auto-detected in non-TTY)"),
    ]

    for opt, desc in global_opts:
        console.print(f"  {opt:<28} {desc}", markup=False)

    console.print()
    if console.color_enabled:
        console.print("[info]Lifecycle Commands:[/info]")
    else:
        console.print("Lifecycle Commands:", markup=False)

    commands = [
        ("start", "Smart start (rebuilds and recreates only services with detected changes)"),
        ("start --recreate", "Force rebuild and recreate all containers"),
        ("stop", "Stop and remove containers"),
        ("stop -v", "Stop containers and remove project persistent volumes"),
        ("restart [service]", "Restart service(s) (rebuilds automatically if code changed)"),
        ("recreate [service]", "Force rebuild and recreate service container(s)"),
        ("push [service]", "Upload/push built container image(s) to Docker registry"),
        ("upload [service]", "Alias for push"),
        ("login [registry]", "Log in to Docker registry (Docker Hub, GHCR, etc.)"),
        ("clean", "Safe interactive clean (containers, volumes, project images)"),
        ("clean -v, --volumes", "Clean project persistent Docker volumes"),
        ("clean -i, --images", "Remove Docker container images built for this project"),
        ("clean -a, --all", "Clean all: containers, persistent volumes & built images"),
        ("clean -f, --force", "Bypass confirmation prompt (safe for CI/CD)"),
        ("purge", "Alias for clean"),
        ("live-logs [svc] [n] [--save]", "Follow live logs (interactive menu if svc omitted, --save logs to file)"),
        ("logs [service]", "Alias for live-logs"),
        ("status, ps", "Show service status, health, and port mappings"),
        ("config, validate", "Validate and display resolved Compose configuration"),
        ("ui, dashboard, tui", "Launch full-terminal real-time interactive dashboard"),
        ("help", "Show this help message"),
    ]

    for cmd, desc in commands:
        console.print(f"  {cmd:<28} {desc}", markup=False)

    console.print()

    if controller:
        try:
            metadata = controller.compose.inspect_services()
            if metadata:
                if console.color_enabled:
                    console.print("[info]Discovered Services in Compose:[/info]")
                else:
                    console.print("Discovered Services in Compose:", markup=False)
                for s, meta in metadata.items():
                    b_flag = f"(buildable: {meta.context or '.'})" if meta.is_buildable else "(pre-built)"
                    console.print(f"  • {s:<18} {b_flag}", markup=False)
                console.print()
        except Exception:
            pass


def main(argv: Optional[Sequence[str]] = None) -> int:
    """CLI entry point and command router."""
    if argv is None:
        argv = sys.argv[1:]

    parsed, extra_args = parse_contextual_args(argv)

    command = parsed.command
    if not command or command in ("help", "--help", "-h"):
        if parsed.help or not command or command in ("help", "--help", "-h"):
            command = "help"
        else:
            command = "start"
    elif parsed.help:
        if command in (
            "push", "upload", "clean", "purge", "prune",
            "live-logs", "live_logs", "livestats", "logs",
            "start", "stop", "restart", "recreate", "rebuild"
        ):
            if "--help" not in extra_args and "-h" not in extra_args:
                extra_args.append("--help")
        else:
            command = "help"

    console = PilotConsole(no_color=parsed.no_color)

    # Build Project Configuration
    try:
        config = build_project_config(
            cli_compose_files=parsed.compose_files,
            cli_env_file=parsed.env_file,
            cli_project_name=parsed.project_name,
            cli_profiles=parsed.profiles,
            cli_registry=parsed.registry,
            cli_tags=parsed.tags,
            no_build=parsed.no_build,
            dry_run=parsed.dry_run,
            push=parsed.push,
            max_wait=parsed.max_wait,
            interval=parsed.interval,
            no_color=parsed.no_color,
        )
    except ComposePilotError as e:
        console.error(str(e))
        return e.exit_code

    controller = LifecycleController(config)

    if command == "help":
        print_usage(console, controller)
        return 0

    try:
        if command == "start":
            param1 = extra_args[0] if extra_args else ""
            force_recreate = param1 in ("--recreate", "-r", "--build")
            
            console.major_section(f"🚀 Starting Deployment [{config.project_name}]")
            controller.check_prerequisites()
            controller.validate_compose()

            console.major_section("🔍 Checking Code Changes & Docker Images")
            def status_logger(msg: str, level: str):
                if level == "warning":
                    console.warning(msg)
                else:
                    console.success(msg)

            def spinner_logger(msg: str):
                if console.color_enabled:
                    sys.stdout.write(f"\r\033[K{msg}")
                    sys.stdout.flush()

            controller.check_and_prepare_services(
                force_recreate=force_recreate,
                status_logger=status_logger,
                spinner_callback=spinner_logger,
            )
            if console.color_enabled:
                sys.stdout.write("\r\033[K")
                sys.stdout.flush()

            if config.push:
                controller.registry.push_all()

            console.major_section("🐳 Starting Services")
            controller.start_containers()
            console.success("Containers running in detached mode")

            console.major_section("⏳ Checking Service Health")
            console.print("Waiting for services to become healthy...")

            def health_spinner(elapsed: int, max_w: int, msg: str):
                if console.color_enabled:
                    sys.stdout.write(f"\r\033[K{msg}")
                    sys.stdout.flush()

            health_map = controller.wait_for_services(progress_callback=health_spinner)
            if console.color_enabled:
                sys.stdout.write("\r\033[K")
                sys.stdout.flush()

            console.major_section("📊 Service Status")
            render_health_table(console, health_map)

            endpoints = controller.get_endpoints()
            render_endpoints_table(console, config.project_name, endpoints)

            console.print()
            console.success(f"{config.project_name} deployment completed successfully.\n")
            console.tip("Live Logs Tip", f"compose-pilot live-logs [service]")
            console.tip("Push Tip     ", f"compose-pilot push [service] -r <registry>")
            console.tip("Recreate Tip ", f"compose-pilot recreate [service]")
            console.tip("Clean Tip    ", f"compose-pilot clean")
            return 0

        elif command == "stop":
            controller.check_prerequisites(check_daemon=True)
            clean_volumes = any(a in ("-v", "--volumes", "volumes", "volume", "vol") for a in extra_args)
            clean_images = any(a in ("-i", "--images", "images", "image", "img") for a in extra_args)
            clean_all = any(a in ("-a", "--all", "all") for a in extra_args)
            force = any(a in ("-f", "--force", "force", "-y", "--yes") for a in extra_args)

            if clean_volumes or clean_images or clean_all:
                vols = clean_volumes or clean_all
                imgs = clean_images or clean_all
                controller.cleaner.clean(
                    clean_volumes=vols,
                    clean_images=imgs,
                    force=force,
                    interactive_menu=lambda: prompt_clean_scope(console, config.project_name),
                    confirm_prompt=lambda v, i, cv, ci: prompt_clean_confirmation(
                        console, config.project_name, v, i, cv, ci, str(config.cache_dir)
                    ),
                    status_callback=lambda m: console.info(m),
                )
            else:
                console.major_section(f"🛑 Stopping [{config.project_name}]")
                controller.stop()
                console.print()
                console.success("All containers stopped and removed")
            return 0

        elif command == "restart":
            controller.check_prerequisites()
            target_svc = extra_args[0] if extra_args else None
            if target_svc:
                console.major_section(f"🔄 Restarting Service: {target_svc}")
            else:
                console.major_section(f"🔄 Restarting [{config.project_name}]")
            controller.restart_service(target_svc)
            console.success(f"Restart completed for: {target_svc or 'all services'}")
            return 0

        elif command in ("recreate", "rebuild"):
            controller.check_prerequisites()
            controller.validate_compose()
            target_svc = extra_args[0] if extra_args else None
            if target_svc:
                console.major_section(f"🔄 Recreating Service: {target_svc}")
            else:
                console.major_section(f"🔄 Recreating [{config.project_name}]")
            controller.recreate_service(target_svc)
            console.success(f"Recreation completed for: {target_svc or 'all services'}")
            return 0

        elif command in ("push", "upload"):
            if any(a in ("-h", "--help", "help") for a in extra_args):
                console.print("\nUsage: compose-pilot push [service] [-r|--registry <url>] [-t|--tag <tag>] [--no-build] [--dry-run]\n", markup=False)
                return 0

            controller.check_prerequisites()
            controller.validate_compose()
            target_svc = extra_args[0] if extra_args and not extra_args[0].startswith("-") else None

            console.major_section(f"📦 Pushing Images to Docker Registry [{config.project_name}]")
            reg_display = config.registry or "(using image repository configured in Compose)"
            console.print(f"Registry:        [cyan]{reg_display}[/cyan]")

            controller.registry.push_all(
                target_service=target_svc,
                status_callback=lambda m: console.info(m),
                interactive_prompt=prompt_docker_login,
            )
            console.print()
            console.success("All target images successfully uploaded to the registry.")
            return 0

        elif command == "login":
            target_reg = extra_args[0] if extra_args else config.registry
            if target_reg:
                console.info(f"Logging in to registry: {target_reg}")
            else:
                console.info("Logging in to Docker Hub...")
            controller.registry.login(target_reg)
            console.success("Login completed.")
            return 0

        elif command in ("clean", "purge", "prune"):
            if any(a in ("-h", "--help", "help") for a in extra_args):
                console.print("\nUsage: compose-pilot clean [options]", markup=False)
                console.print("Options:", markup=False)
                console.print("  -a, --all, all           Clean project persistent volumes and built container images", markup=False)
                console.print("  -v, --volumes, volumes   Clean project persistent Docker volumes", markup=False)
                console.print("  -i, --images, images     Remove Docker container images built for this project", markup=False)
                console.print("  -f, --force, force       Skip interactive confirmation prompt\n", markup=False)
                return 0

            controller.check_prerequisites()
            clean_volumes = any(a in ("-v", "--volumes", "volumes", "volume", "vol") for a in extra_args)
            clean_images = any(a in ("-i", "--images", "images", "image", "img") for a in extra_args)
            clean_all = any(a in ("-a", "--all", "all") for a in extra_args)
            force = any(a in ("-f", "--force", "force", "-y", "--yes") for a in extra_args)

            vols = clean_volumes or clean_all
            imgs = clean_images or clean_all

            success_clean = controller.cleaner.clean(
                clean_volumes=vols,
                clean_images=imgs,
                force=force,
                interactive_menu=lambda: prompt_clean_scope(console, config.project_name),
                confirm_prompt=lambda v, i, cv, ci: prompt_clean_confirmation(
                    console, config.project_name, v, i, cv, ci, str(config.cache_dir)
                ),
                status_callback=lambda m: console.info(m),
            )

            if success_clean:
                console.major_section("✨ Cleanup Complete")
                console.success(f"{config.project_name} cleanup completed successfully.")
                console.tip("To start afresh", "compose-pilot start")
            return 0

        elif command in ("status", "ps"):
            controller.check_prerequisites()
            console.major_section(f"📊 Status [{config.project_name}]")
            health_map = controller.health.poll_health()
            render_health_table(console, health_map)
            endpoints = controller.get_endpoints()
            render_endpoints_table(console, config.project_name, endpoints)
            return 0

        elif command in ("live-logs", "live_logs", "livestats", "logs"):
            if any(a in ("-h", "--help", "help") for a in extra_args):
                console.print("\nUsage: compose-pilot live-logs [service] [lines] [--save]\n", markup=False)
                return 0

            controller.check_prerequisites()
            target_svc = None
            lines = 100
            save_flag = False

            for arg in extra_args:
                if arg in ("--save", "-s"):
                    save_flag = True
                elif arg.isdigit():
                    lines = int(arg)
                elif not arg.startswith("-") and not target_svc:
                    target_svc = arg

            stream_live_logs(
                config=config,
                compose=controller.compose,
                console=console,
                service=target_svc,
                tail_lines=lines,
                save_to_file=save_flag,
            )
            return 0

        elif command in ("config", "validate"):
            controller.check_prerequisites(check_daemon=False)
            controller.validate_compose()
            console.major_section("📋 Resolved Services & Configuration")
            metadata = controller.get_services_metadata()
            render_config_table(console, metadata)
            return 0

        elif command in ("ui", "dashboard", "tui"):
            controller.check_prerequisites()
            controller.validate_compose()
            launch_dashboard(config, controller)
            return 0

        else:
            console.error(f"Unknown command: {command}")
            print_usage(console, controller)
            return 1

    except HealthCheckTimeoutError as e:
        console.print()
        console.error("One or more services failed health checks or did not start cleanly.\n")
        for svc in e.failed_services:
            console.minor_section(f"🔍 Recent logs for {svc} (last 25 lines):")
            diag = controller.health.get_diagnostic_logs(svc)
            console.print(diag)
            console.print()
        console.print("To follow live logs:")
        console.print(f"[cyan]compose-pilot live-logs <service>[/cyan]\n")
        return 1

    except ComposePilotError as e:
        console.error(e.message)
        return e.exit_code

    except ProcessCancelledError:
        console.print("\n[dim]Operation cancelled by user.[/dim]")
        return 130

    except KeyboardInterrupt:
        console.print("\n[dim]Interrupted by user.[/dim]")
        return 130

    return 0


if __name__ == "__main__":
    sys.exit(main())
