"""Safe environment cleanup, persistent volume purging, image pruning, and cache clearing."""

import shutil
import sys
from pathlib import Path
from typing import Callable, Optional

from compose_pilot.core.compose import ComposeManager
from compose_pilot.exceptions import ComposePilotError
from compose_pilot.models import ProjectConfig
from compose_pilot.utils.process import run_command


class EnvironmentCleaner:
    """Safely manages destruction of containers, project persistent volumes, built images, and hash caches."""

    def __init__(self, config: ProjectConfig, compose: ComposeManager) -> None:
        self.config = config
        self.compose = compose

    def clean(
        self,
        clean_volumes: bool = False,
        clean_images: bool = False,
        force: bool = False,
        interactive_menu: Optional[Callable[[], tuple[bool, bool]]] = None,
        confirm_prompt: Optional[Callable[[list[str], list[str], bool, bool], bool]] = None,
        status_callback: Optional[Callable[[str], None]] = None,
    ) -> bool:
        """Execute cleanup with interactive menus, confirmation safety checks, and selective purging."""
        def log(msg: str):
            if status_callback:
                status_callback(msg)

        # 1. Determine scope if not specified via flags
        if not clean_volumes and not clean_images:
            if force:
                clean_volumes = True
                clean_images = True
            elif interactive_menu and sys.stdin.isatty():
                vols, imgs = interactive_menu()
                clean_volumes, clean_images = vols, imgs
                if not clean_volumes and not clean_images:
                    log("Clean operation cancelled.")
                    return False
            else:
                clean_volumes = True
                clean_images = True

        detected_vols = self.compose.get_project_volumes()
        detected_imgs = self.compose.get_project_images()

        # 2. Confirmation safety check
        if not force:
            if not sys.stdin.isatty():
                raise ComposePilotError(
                    "Running in non-interactive environment without --force. "
                    "Aborting clean to prevent accidental data loss. Pass --force or -f to proceed."
                )

            if confirm_prompt:
                confirmed = confirm_prompt(detected_vols, detected_imgs, clean_volumes, clean_images)
                if not confirmed:
                    log("Clean operation aborted.")
                    return False

        # 3. Stop and remove containers
        log("Stopping & Removing Containers...")
        down_cmd = self.compose.get_base_cmd() + ["down", "--remove-orphans"]
        if clean_volumes:
            down_cmd.append("-v")
        run_command(down_cmd, check=False)
        self.compose.remove_conflicting_containers()
        log("Containers stopped and removed")

        # 4. Clean persistent volumes
        if clean_volumes:
            log("Cleaning Project Persistent Volumes...")
            for vol in detected_vols:
                rm_res = run_command(["docker", "volume", "rm", "-f", vol], check=False)
                if rm_res.returncode == 0:
                    log(f"Removed volume: {vol}")
                else:
                    log(f"Warning: Could not remove volume: {vol} (may still be referenced)")

            # Clear local service log files
            logs_dir = self.config.logs_dir
            if logs_dir.is_dir():
                for f in logs_dir.glob("*.log"):
                    try:
                        f.unlink(missing_ok=True)
                    except OSError:
                        pass
                log(f"Cleared local service log files ({logs_dir}/)")

        # 5. Clean project container images
        if clean_images:
            log("Cleaning Project Container Images...")
            for img in detected_imgs:
                rm_res = run_command(["docker", "image", "rm", "-f", img], check=False)
                if rm_res.returncode == 0:
                    log(f"Removed image: {img}")
                else:
                    log(f"Warning: Could not remove image: {img} (may still be referenced)")

            # Clear hash cache
            cache_dir = self.config.cache_dir
            if cache_dir.is_dir():
                shutil.rmtree(cache_dir, ignore_errors=True)
                log(f"Cleared build hash cache ({cache_dir}/)")

        return True
