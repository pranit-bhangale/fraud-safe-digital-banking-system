"""Configuration discovery, file resolution, and environment detection."""

import os
import re
import subprocess
from pathlib import Path
from typing import Optional, Sequence

from compose_pilot.exceptions import ConfigurationError
from compose_pilot.models import ProjectConfig


def detect_compose_files(cli_files: Optional[Sequence[str | Path]] = None) -> list[Path]:
    """Detect active Docker Compose files based on CLI args, environment, or convention.
    
    Checks in order:
    1. CLI specified files (--file / -f)
    2. COMPOSE_FILE environment variable (delimited by : or ;)
    3. Standard candidate: docker-compose.yaml or docker-compose.yml
    4. Override file: docker-compose.override.yaml or docker-compose.override.yml
    """
    active_files: list[Path] = []

    if cli_files:
        for f in cli_files:
            p = Path(f)
            if not p.is_file():
                raise ConfigurationError(f"Specified Compose file not found: {f}")
            active_files.append(p)
        return active_files

    env_compose_file = os.environ.get("COMPOSE_FILE")
    if env_compose_file:
        delimiter = ";" if ";" in env_compose_file else ":"
        candidates = [Path(c.strip()) for c in env_compose_file.split(delimiter) if c.strip()]
        for c in candidates:
            if c.is_file():
                active_files.append(c)
        if active_files:
            return active_files

    primary_found: Optional[Path] = None
    for candidate in (Path("docker-compose.yaml"), Path("docker-compose.yml")):
        if candidate.is_file():
            primary_found = candidate
            active_files.append(candidate)
            break

    if not primary_found:
        return []

    for override in (Path("docker-compose.override.yaml"), Path("docker-compose.override.yml")):
        if override.is_file():
            active_files.append(override)
            break

    return active_files


def detect_env_file(cli_env_file: Optional[str | Path] = None) -> Optional[Path]:
    """Detect environment configuration file (.env).
    
    Checks in order:
    1. CLI specified env file (--env-file)
    2. Default .env in current working directory
    """
    if cli_env_file:
        p = Path(cli_env_file)
        if not p.is_file():
            raise ConfigurationError(f"Specified environment file not found: {cli_env_file}")
        return p

    default_env = Path(".env")
    if default_env.is_file():
        return default_env

    return None


def normalize_name(raw: str) -> str:
    """Normalize a directory or string name for Docker Compose project naming.
    
    Converts to lowercase and replaces non-alphanumeric chars with dashes.
    """
    lowered = raw.strip().lower()
    cleaned = re.sub(r"[^a-z0-9]+", "-", lowered)
    return cleaned.strip("-")


def detect_project_name(
    cli_name: Optional[str] = None,
    compose_files: Optional[Sequence[Path]] = None,
    compose_cmd_builder: Optional[callable] = None,
) -> str:
    """Determine the active project name.
    
    Checks:
    1. CLI argument (-p, --project-name)
    2. COMPOSE_PROJECT_NAME environment variable
    3. `name:` key inside compose files
    4. `name:` key inside docker compose config
    5. Normalized current working directory name
    """
    if cli_name and cli_name.strip():
        return cli_name.strip()

    env_name = os.environ.get("COMPOSE_PROJECT_NAME")
    if env_name and env_name.strip():
        return env_name.strip()

    # Direct inspection of compose file header for top-level `name:`
    if compose_files:
        for f in compose_files:
            try:
                p = Path(f)
                if p.is_file():
                    with open(p, "r", encoding="utf-8", errors="ignore") as fp:
                        for line in fp:
                            match = re.match(r"^name:\s+['\"]?([a-zA-Z0-9_.-]+)['\"]?", line.strip())
                            if match:
                                return match.group(1).strip()
            except Exception:
                pass

    if compose_cmd_builder:
        try:
            cmd = compose_cmd_builder(["config"])
            res = subprocess.run(cmd, capture_output=True, text=True, check=False)
            if res.returncode == 0:
                for line in res.stdout.splitlines():
                    match = re.match(r"^name:\s+([a-zA-Z0-9_.-]+)", line.strip())
                    if match:
                        return match.group(1).strip()
        except Exception:
            pass

    return normalize_name(Path.cwd().name)


def detect_registry(cli_registry: Optional[str] = None) -> Optional[str]:
    """Detect remote registry from CLI or environment variables."""
    if cli_registry and cli_registry.strip():
        return cli_registry.strip().rstrip("/")

    for var in ("DOCKER_REGISTRY", "REGISTRY"):
        val = os.environ.get(var)
        if val and val.strip():
            return val.strip().rstrip("/")

    return None


def detect_target_tags(cli_tags: Optional[Sequence[str]] = None) -> list[str]:
    """Determine target tags for image pushing.
    
    Order of precedence:
    1. CLI tags (split comma-separated values if passed)
    2. IMAGE_TAG or DOCKER_TAG env vars
    3. Git short SHA + 'latest'
    4. Fallback: ['latest']
    """
    tags: list[str] = []
    if cli_tags:
        for item in cli_tags:
            for t in item.split(","):
                clean = t.strip()
                if clean and clean not in tags:
                    tags.append(clean)
        if tags:
            return tags

    for var in ("IMAGE_TAG", "DOCKER_TAG"):
        val = os.environ.get(var)
        if val and val.strip():
            return [val.strip()]

    # Try Git short SHA
    try:
        res = subprocess.run(
            ["git", "rev-parse", "--short", "HEAD"],
            capture_output=True,
            text=True,
            check=False,
        )
        if res.returncode == 0:
            sha = res.stdout.strip()
            if sha:
                return [sha, "latest"]
    except Exception:
        pass

    return ["latest"]


def build_project_config(
    cli_compose_files: Optional[Sequence[str]] = None,
    cli_env_file: Optional[str] = None,
    cli_project_name: Optional[str] = None,
    cli_profiles: Optional[Sequence[str]] = None,
    cli_registry: Optional[str] = None,
    cli_tags: Optional[Sequence[str]] = None,
    no_build: bool = False,
    dry_run: bool = False,
    push: bool = False,
    max_wait: int = 120,
    interval: int = 3,
    no_color: bool = False,
) -> ProjectConfig:
    """Assemble and validate a ProjectConfig instance."""
    compose_files = detect_compose_files(cli_compose_files)
    env_file = detect_env_file(cli_env_file)
    project_name = detect_project_name(cli_project_name, compose_files)
    registry = detect_registry(cli_registry)
    tags = detect_target_tags(cli_tags)

    return ProjectConfig(
        project_name=project_name,
        compose_files=compose_files,
        env_file=env_file,
        profiles=list(cli_profiles or []),
        max_wait=max_wait,
        interval=interval,
        no_color=no_color,
        dry_run=dry_run,
        no_build=no_build,
        push=push,
        registry=registry,
        tags=tags,
    )
