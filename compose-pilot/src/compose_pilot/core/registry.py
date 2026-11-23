"""Docker registry operations: image tagging, push automation, and authentication retry."""

import re
import sys
from typing import Callable, Optional, Sequence

from compose_pilot.core.compose import ComposeManager
from compose_pilot.core.hasher import ServiceHasher
from compose_pilot.exceptions import RegistryError, ServiceNotFoundError
from compose_pilot.models import ProjectConfig
from compose_pilot.utils.process import run_command


class RegistryManager:
    """Manages Docker image tagging, pushing, and interactive login retry."""

    def __init__(
        self,
        config: ProjectConfig,
        compose: ComposeManager,
        hasher: ServiceHasher,
    ) -> None:
        self.config = config
        self.compose = compose
        self.hasher = hasher

    def resolve_target_repo(self, local_image: str, registry: Optional[str]) -> str:
        """Resolve destination repository for a local image."""
        image_without_tag = local_image.split(":")[0]
        if registry:
            clean_name = image_without_tag.split("/")[-1]
            return f"{registry.rstrip('/')}/{clean_name}"

        if "/" in image_without_tag:
            return image_without_tag

        raise RegistryError(
            f"Cannot push '{local_image}': no remote registry specified.\n"
            f"Provide a registry via --registry (e.g. -r ghcr.io/org or -r docker.io/username) "
            f"or define DOCKER_REGISTRY in your .env file."
        )

    def extract_registry_domain(self, target_repo: str) -> Optional[str]:
        """Extract domain from target repo (e.g. 'ghcr.io' from 'ghcr.io/myorg/app')."""
        first_segment = target_repo.split("/")[0]
        if "." in first_segment or ":" in first_segment:
            return first_segment
        return None

    def push_service_image(
        self,
        service: str,
        tags: Sequence[str],
        registry: Optional[str] = None,
        dry_run: bool = False,
        no_build: bool = False,
        status_callback: Optional[Callable[[str], None]] = None,
        interactive_prompt: Optional[Callable[[str], bool]] = None,
    ) -> bool:
        """Tag and push all target tags for a specific service's image."""
        def log(msg: str):
            if status_callback:
                status_callback(msg)

        local_image = self.compose.get_service_image(service)
        image_exists = self.compose.image_exists_locally(local_image)
        metadata = self.compose.inspect_services().get(service)

        is_buildable = metadata.is_buildable if metadata else False
        code_changed = self.hasher.has_code_changed(service) if is_buildable else False

        if is_buildable:
            if not image_exists or (not no_build and code_changed):
                log(f"Building image for {service} before push...")
                if dry_run:
                    log(f"[dry-run] Would build image for service: {service}")
                else:
                    cmd = self.compose.get_base_cmd() + ["build", service]
                    res = run_command(cmd, check=False)
                    if res.returncode != 0:
                        raise RegistryError(f"Failed to build image for {service} before push:\n{res.stderr}")
                    self.hasher.save_service_hash(service)
                    local_image = self.compose.get_service_image(service)
        elif not image_exists:
            raise RegistryError(
                f"Image '{local_image}' for service '{service}' does not exist locally. Build or pull it first."
            )

        target_repo = self.resolve_target_repo(local_image, registry or self.config.registry)
        reg_domain = self.extract_registry_domain(target_repo)

        for tag in tags:
            remote_image = f"{target_repo}:{tag}"

            if dry_run:
                log(f"[dry-run] Would tag:  {local_image} -> {remote_image}")
                log(f"[dry-run] Would push: {remote_image}")
                continue

            if local_image != remote_image:
                tag_cmd = ["docker", "tag", local_image, remote_image]
                tag_res = run_command(tag_cmd, check=False)
                if tag_res.returncode != 0:
                    raise RegistryError(f"Failed to tag '{local_image}' as '{remote_image}': {tag_res.stderr}")
                log(f"Tagged: {local_image} -> {remote_image}")

            log(f"Uploading {remote_image} to Docker registry...")
            push_res = run_command(["docker", "push", remote_image], check=False)

            if push_res.returncode == 0:
                digest = self._get_image_digest(remote_image)
                log(f"Successfully pushed: {remote_image}")
                if digest:
                    log(f"  • Digest: {digest}")
                continue

            # Push failed - check for authentication issue
            log(f"Failed to push '{remote_image}'.")
            if reg_domain:
                log(f"Authentication may be required for {reg_domain}.")
            else:
                log("Authentication may be required for Docker Hub.")

            can_prompt = (interactive_prompt is not None) and sys.stdin.isatty()
            should_login = False
            if can_prompt:
                should_login = interactive_prompt(
                    f'Would you like to run "docker login" now to authenticate?'
                )

            if should_login:
                login_cmd = ["docker", "login"]
                if reg_domain:
                    login_cmd.append(reg_domain)
                login_res = run_command(login_cmd, check=False, capture_output=False)
                if login_res.returncode == 0:
                    log(f"Retrying upload for {remote_image}...")
                    retry_res = run_command(["docker", "push", remote_image], check=False)
                    if retry_res.returncode == 0:
                        digest = self._get_image_digest(remote_image)
                        log(f"Successfully pushed: {remote_image}")
                        if digest:
                            log(f"  • Digest: {digest}")
                        continue

            raise RegistryError(f"Failed to push '{remote_image}':\n{push_res.stderr or push_res.stdout}")

        return True

    def _get_image_digest(self, image_name: str) -> Optional[str]:
        """Fetch repo digest of pushed image."""
        cmd = ["docker", "inspect", "--format={{index .RepoDigests 0}}", image_name]
        res = run_command(cmd, check=False)
        if res.returncode == 0 and res.stdout.strip():
            return res.stdout.strip()
        return None

    def push_all(
        self,
        target_service: Optional[str] = None,
        status_callback: Optional[Callable[[str], None]] = None,
        interactive_prompt: Optional[Callable[[str], bool]] = None,
    ) -> None:
        """Push target service or all buildable services."""
        all_services = self.compose.get_services()

        services_to_push: list[str] = []
        if target_service:
            if target_service not in all_services:
                raise ServiceNotFoundError(target_service, all_services)
            services_to_push.append(target_service)
        else:
            services_to_push = self.compose.get_buildable_services()
            if not services_to_push:
                services_to_push = all_services

        if not services_to_push:
            raise RegistryError("No services found to push.")

        for svc in services_to_push:
            self.push_service_image(
                service=svc,
                tags=self.config.tags,
                registry=self.config.registry,
                dry_run=self.config.dry_run,
                no_build=self.config.no_build,
                status_callback=status_callback,
                interactive_prompt=interactive_prompt,
            )

    def login(self, target_domain: Optional[str] = None) -> None:
        """Execute docker login for specified domain or Docker Hub."""
        domain = target_domain or self.config.registry
        cmd = ["docker", "login"]
        if domain:
            extracted = self.extract_registry_domain(domain)
            if extracted:
                cmd.append(extracted)
            elif domain:
                cmd.append(domain)
        run_command(cmd, check=True, capture_output=False)
