"""Data models and value objects for Compose-Pilot."""

from dataclasses import dataclass, field
from enum import Enum
from pathlib import Path
from typing import Optional


class ServiceState(str, Enum):
    """Normalized service status states."""
    HEALTHY = "healthy"
    RUNNING = "running"
    COMPLETED = "completed"
    STARTING = "starting"
    UNHEALTHY = "unhealthy"
    FAILED = "failed"
    NOT_FOUND = "not_found"
    UNKNOWN = "unknown"

    @property
    def is_terminal_failure(self) -> bool:
        """True if the state indicates a terminal failure requiring intervention."""
        return self in (ServiceState.FAILED, ServiceState.UNHEALTHY)

    @property
    def is_success(self) -> bool:
        """True if the state indicates healthy, running, or successfully completed."""
        return self in (ServiceState.HEALTHY, ServiceState.RUNNING, ServiceState.COMPLETED)


@dataclass(frozen=True)
class ServiceMetadata:
    """Parsed metadata for a service defined in Docker Compose."""
    name: str
    is_buildable: bool
    context: str = ""
    dockerfile: str = "Dockerfile"
    ports: list[str] = field(default_factory=list)
    image: str = ""

    @property
    def published_ports(self) -> list[str]:
        """List of published port strings, e.g. ['8080', '443']."""
        return self.ports


@dataclass
class ServiceHealth:
    """Current health and container execution status for a service."""
    service: str
    state: ServiceState = ServiceState.UNKNOWN
    container_id: Optional[str] = None
    container_status: str = "unknown"
    health_status: str = "none"
    exit_code: int = 0
    raw_info: str = ""


@dataclass
class ChangeDetectionResult:
    """Summary of code changes and container requirements across all services."""
    services_to_rebuild: list[str] = field(default_factory=list)
    services_to_recreate: list[str] = field(default_factory=list)
    up_to_date: list[str] = field(default_factory=list)
    reasons: dict[str, str] = field(default_factory=dict)


@dataclass
class ProjectConfig:
    """Active Docker Compose project configuration settings."""
    project_name: str
    compose_files: list[Path] = field(default_factory=list)
    env_file: Optional[Path] = None
    profiles: list[str] = field(default_factory=list)
    max_wait: int = 120
    interval: int = 3
    no_color: bool = False
    dry_run: bool = False
    no_build: bool = False
    push: bool = False
    registry: Optional[str] = None
    tags: list[str] = field(default_factory=list)

    @property
    def cache_dir(self) -> Path:
        """Path to cache directory for the current project."""
        return Path(f".{self.project_name}_cache")

    @property
    def logs_dir(self) -> Path:
        """Path to local logs directory."""
        return Path("logs")
