"""Docker and Docker Compose command executor and metadata parser."""

import os
import re
import subprocess
from pathlib import Path
from typing import Optional, Sequence

import yaml

from compose_pilot.exceptions import (
    BuildError,
    ConfigurationError,
    PrerequisiteError,
)
from compose_pilot.models import ProjectConfig, ServiceMetadata
from compose_pilot.utils.process import run_command


class ComposeManager:
    """Interface to Docker CLI and Docker Compose operations."""

    def __init__(self, config: ProjectConfig) -> None:
        self.config = config
        self._services_cache: Optional[dict[str, ServiceMetadata]] = None

    def get_base_cmd(self) -> list[str]:
        """Construct base docker compose command with all active flags."""
        cmd = ["docker", "compose"]
        for f in self.config.compose_files:
            cmd.extend(["-f", str(f)])
        if self.config.env_file:
            cmd.extend(["--env-file", str(self.config.env_file)])
        if self.config.project_name:
            cmd.extend(["-p", self.config.project_name])
        for profile in self.config.profiles:
            cmd.extend(["--profile", profile])
        return cmd

    def check_prerequisites(self, check_daemon: bool = True) -> None:
        """Validate that Docker is installed and the daemon is reachable."""
        try:
            run_command(["docker", "--version"], check=True)
        except Exception as e:
            raise PrerequisiteError(
                "Docker CLI is not installed or not available in system PATH."
            ) from e

        if check_daemon:
            try:
                run_command(["docker", "info"], check=True)
            except Exception as e:
                raise PrerequisiteError(
                    "Docker daemon is not running. Please start Docker and try again."
                ) from e

        if not self.config.compose_files:
            raise ConfigurationError(
                "No Docker Compose file found (checked docker-compose.yaml, docker-compose.yml)."
            )

        # Environment file is optional: only validate if one was specified or detected
        if self.config.env_file and not self.config.env_file.is_file():
            raise ConfigurationError(
                f"Specified environment file not found: {self.config.env_file}"
            )

    def validate_compose(self) -> str:
        """Validate the Compose configuration and return the resolved YAML output."""
        cmd = self.get_base_cmd() + ["config"]
        try:
            res = run_command(cmd, check=True)
            return res.stdout
        except subprocess.CalledProcessError as e:
            raise ConfigurationError(
                f"Docker Compose configuration is invalid:\n{e.stderr or e.output}"
            ) from e

    def get_services(self) -> list[str]:
        """List all service names defined in the active compose configuration."""
        cmd = self.get_base_cmd() + ["config", "--services"]
        try:
            res = run_command(cmd, check=True)
            services = [line.strip() for line in res.stdout.splitlines() if line.strip()]
            return services
        except subprocess.CalledProcessError as e:
            raise ConfigurationError(f"Failed to query services: {e.stderr}") from e

    def inspect_services(self, force_refresh: bool = False) -> dict[str, ServiceMetadata]:
        """Parse Compose config to extract buildability, context, dockerfile, ports, and image."""
        if self._services_cache is not None and not force_refresh:
            return self._services_cache

        config_yaml = self.validate_compose()
        services_dict: dict[str, ServiceMetadata] = {}

        try:
            parsed = yaml.safe_load(config_yaml) or {}
            raw_services = parsed.get("services", {})
            for svc_name, svc_info in raw_services.items():
                if not isinstance(svc_info, dict):
                    continue

                build_val = svc_info.get("build")
                is_buildable = False
                context = ""
                dockerfile = "Dockerfile"

                if isinstance(build_val, str):
                    is_buildable = True
                    context = build_val
                elif isinstance(build_val, dict):
                    is_buildable = True
                    context = str(build_val.get("context", "."))
                    dockerfile = str(build_val.get("dockerfile", "Dockerfile"))

                # Extract published ports
                raw_ports = svc_info.get("ports", [])
                published_ports: list[str] = []
                for p in raw_ports:
                    if isinstance(p, dict):
                        pub = p.get("published")
                        if pub:
                            published_ports.append(str(pub))
                    elif isinstance(p, (str, int)):
                        # Format like "8080:80" or "8080"
                        p_str = str(p)
                        parts = p_str.split(":")
                        if len(parts) >= 2:
                            published_ports.append(parts[0])
                        else:
                            published_ports.append(p_str)

                # Image name
                image = svc_info.get("image")
                if not image:
                    image = f"{self.config.project_name}-{svc_name}:latest"

                services_dict[svc_name] = ServiceMetadata(
                    name=svc_name,
                    is_buildable=is_buildable,
                    context=context,
                    dockerfile=dockerfile,
                    ports=published_ports,
                    image=image,
                )
        except Exception:
            # Fallback regex parsing if YAML parsing encounters issues
            services_dict = self._regex_parse_services(config_yaml)

        self._services_cache = services_dict
        return services_dict

    def _regex_parse_services(self, compose_cfg: str) -> dict[str, ServiceMetadata]:
        """Fallback regex parser for compose services."""
        services: dict[str, ServiceMetadata] = {}
        # Parse basic fields line by line
        current_svc: Optional[str] = None
        in_services = False
        in_build = False

        is_build = False
        ctx = ""
        df = "Dockerfile"
        img = ""
        ports: list[str] = []

        def save_current():
            if current_svc:
                services[current_svc] = ServiceMetadata(
                    name=current_svc,
                    is_buildable=is_build,
                    context=ctx,
                    dockerfile=df or "Dockerfile",
                    ports=list(ports),
                    image=img or f"{self.config.project_name}-{current_svc}:latest",
                )

        for line in compose_cfg.splitlines():
            clean = line.rstrip()
            if clean.startswith("services:"):
                in_services = True
                continue
            if in_services and re.match(r"^[a-zA-Z0-9_]+:", clean):
                in_services = False
                save_current()
                current_svc = None
                continue
            if in_services:
                svc_match = re.match(r"^  ([a-zA-Z0-9_.-]+):", clean)
                if svc_match:
                    save_current()
                    current_svc = svc_match.group(1)
                    is_build = False
                    ctx = ""
                    df = "Dockerfile"
                    img = ""
                    ports = []
                    in_build = False
                    continue

                if current_svc:
                    if clean.strip().startswith("image:"):
                        img = clean.split("image:", 1)[1].strip().strip("\"'")
                    elif clean.strip().startswith("build:"):
                        is_build = True
                        val = clean.split("build:", 1)[1].strip().strip("\"'")
                        if val:
                            ctx = val
                        else:
                            in_build = True
                    elif in_build and "context:" in clean:
                        ctx = clean.split("context:", 1)[1].strip().strip("\"'")
                    elif in_build and "dockerfile:" in clean:
                        df = clean.split("dockerfile:", 1)[1].strip().strip("\"'")
                    elif "published:" in clean:
                        pub = clean.split("published:", 1)[1].strip().strip("\"'")
                        if pub:
                            ports.append(pub)

        save_current()
        return services

    def get_buildable_services(self) -> list[str]:
        """Return list of service names that declare a build configuration."""
        metadata = self.inspect_services()
        return [svc for svc, meta in metadata.items() if meta.is_buildable]

    def get_service_image(self, service: str) -> str:
        """Get the image name associated with a service."""
        metadata = self.inspect_services()
        meta = metadata.get(service)
        if meta and meta.image:
            return meta.image
        return f"{self.config.project_name}-{service}:latest"

    def get_project_volumes(self) -> list[str]:
        """Query Docker volume names labeled with this project."""
        cmd = [
            "docker", "volume", "ls",
            "--filter", f"label=com.docker.compose.project={self.config.project_name}",
            "--format", "{{.Name}}",
        ]
        res = run_command(cmd, check=False)
        if res.returncode == 0:
            return sorted(list({line.strip() for line in res.stdout.splitlines() if line.strip()}))
        return []

    def get_project_images(self) -> list[str]:
        """Query Docker image tags labeled with this project."""
        cmd = [
            "docker", "images",
            "--filter", f"label=com.docker.compose.project={self.config.project_name}",
            "--format", "{{.Repository}}:{{.Tag}}",
        ]
        res = run_command(cmd, check=False)
        if res.returncode == 0:
            return sorted(list({line.strip() for line in res.stdout.splitlines() if line.strip() and line.strip() != "<none>:<none>"}))
        return []

    def image_exists_locally(self, image_name: str) -> bool:
        """Check if a Docker image is present in the local daemon storage."""
        cmd = ["docker", "image", "inspect", image_name]
        res = run_command(cmd, check=False)
        return res.returncode == 0

    def get_container_ids(self, service: str) -> list[str]:
        """Get running or existing container IDs for a service."""
        cmd = self.get_base_cmd() + ["ps", "-q", service]
        res = run_command(cmd, check=False)
        if res.returncode == 0:
            return [line.strip() for line in res.stdout.splitlines() if line.strip()]
        return []
