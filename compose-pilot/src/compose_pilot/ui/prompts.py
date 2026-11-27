"""Interactive terminal menus and safety confirmation dialogs."""

import re
import sys
from typing import Optional

from compose_pilot.ui.console import PilotConsole


def prompt_stop_scope(console: PilotConsole, project_name: str) -> str:
    """Display interactive menu for selecting stop teardown action.
    
    Returns:
        'stop': Just stop and remove containers (keep volumes & images)
        'volumes': Stop and remove containers + delete volumes (reset data)
        'all': Full clean: remove containers + volumes + built images & cache
        'cancel': Abort stop operation
    """
    console.major_section(f"🛑 Stop Project [{project_name}]")
    console.minor_section("Select teardown action:")
    console.print("  [info]1)[/info] Just stop containers (keep database volumes & built images) [default]")
    console.print("  [info]2)[/info] Stop & remove volumes (reset database & persistent data)")
    console.print("  [info]3)[/info] Full clean (remove containers + volumes + images + cache)")
    console.print("  [info]4)[/info] Cancel")
    console.print()

    try:
        choice = input("Select option [1-4] (default: 1): ").strip()
    except (EOFError, KeyboardInterrupt):
        return "cancel"

    if choice in ("1", ""):
        return "stop"
    elif choice == "2":
        return "volumes"
    elif choice == "3":
        return "all"
    else:
        return "cancel"


def prompt_clean_scope(console: PilotConsole, project_name: str) -> tuple[bool, bool]:
    """Display interactive menu for selecting cleanup scope.
    
    Returns:
        (clean_volumes, clean_images)
    """
    console.major_section(f"🧹 Clean & Purge [{project_name}]")
    console.minor_section("Select cleanup scope:")
    console.print("  [info]1)[/info] All (Containers + Project Volumes + Project Images + Cache)")
    console.print("  [info]2)[/info] Volumes only (Reset project persistent databases, caches & logs)")
    console.print("  [info]3)[/info] Project Images only (Remove project container images)")
    console.print("  [info]4)[/info] Cancel")
    console.print()

    try:
        choice = input("Select option [1-4]: ").strip()
    except (EOFError, KeyboardInterrupt):
        return (False, False)

    if choice == "1":
        return (True, True)
    elif choice == "2":
        return (True, False)
    elif choice == "3":
        return (False, True)
    else:
        return (False, False)


def prompt_clean_confirmation(
    console: PilotConsole,
    project_name: str,
    volumes: list[str],
    images: list[str],
    clean_volumes: bool,
    clean_images: bool,
    cache_dir: str,
) -> bool:
    """Display detailed warning and prompt user for explicit destructive confirmation."""
    console.major_section(f"⚠️  Clean Confirmation [{project_name}]")
    console.print("[warning]The following actions will be performed:[/warning]")
    console.print("  • Stop and remove running project containers")

    if clean_volumes:
        console.print("  • [error]PERMANENTLY DELETE project persistent volumes[/error]:")
        if volumes:
            for vol in volumes:
                console.print(f"      - {vol}")
        else:
            console.print("      (No project persistent volumes detected)")

    if clean_images:
        console.print("  • [error]Remove built project container images[/error]:")
        if images:
            for img in images:
                console.print(f"      - {img}")
        else:
            console.print("      (No project container images detected)")
        console.print(f"  • Clear build hash cache ({cache_dir})")

    console.print()
    try:
        answer = input("Are you sure you want to proceed? [y/N]: ").strip().lower()
        return answer in ("y", "yes")
    except (EOFError, KeyboardInterrupt):
        return False


def prompt_service_selection(console: PilotConsole, services: list[str]) -> Optional[str]:
    """Display numbered list of services for interactive log tailing."""
    console.minor_section("Select a service to view live logs:")
    for idx, s in enumerate(services, 1):
        console.print(f"  [info]{idx})[/info] {s:<18}")
    all_idx = len(services) + 1
    console.print(f"  [info]{all_idx})[/info] all services")
    console.print()

    try:
        val = input(f"Select service [1-{all_idx}]: ").strip()
        if not val:
            return ""
        num = int(val)
        if 1 <= num <= len(services):
            return services[num - 1]
        elif num == all_idx:
            return ""
        else:
            console.warning("Invalid choice. Streaming logs for all services...")
            return ""
    except (ValueError, EOFError, KeyboardInterrupt):
        return ""


def prompt_docker_login(prompt_text: str = 'Would you like to run "docker login" now to authenticate?') -> bool:
    """Ask whether to run docker login to authenticate with registry."""
    try:
        print(f"\n{prompt_text} [Y/n]: ", end="", flush=True)
        val = input().strip().lower()
        if not val or val in ("y", "yes"):
            return True
        return False
    except (EOFError, KeyboardInterrupt):
        return False
