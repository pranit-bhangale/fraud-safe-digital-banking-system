"""Structured exception hierarchy for Compose-Pilot."""

from typing import Optional


class ComposePilotError(Exception):
    """Base exception for all Compose-Pilot errors."""
    def __init__(self, message: str, exit_code: int = 1) -> None:
        super().__init__(message)
        self.message = message
        self.exit_code = exit_code


class PrerequisiteError(ComposePilotError):
    """Raised when Docker CLI is missing or daemon is unreachable."""
    pass


class ConfigurationError(ComposePilotError):
    """Raised when Docker Compose or .env configuration is invalid or missing."""
    pass


class ServiceNotFoundError(ComposePilotError):
    """Raised when a specified service does not exist in the compose configuration."""
    def __init__(self, service: str, available_services: list[str]) -> None:
        msg = f"Unknown service: {service}. Available: {', '.join(available_services)}"
        super().__init__(msg)
        self.service = service
        self.available_services = available_services


class BuildError(ComposePilotError):
    """Raised when docker compose build fails."""
    def __init__(self, message: str, build_log: Optional[str] = None) -> None:
        super().__init__(message)
        self.build_log = build_log


class HealthCheckTimeoutError(ComposePilotError):
    """Raised when one or more services fail health checks or exceed max timeout."""
    def __init__(self, message: str, failed_services: list[str]) -> None:
        super().__init__(message)
        self.failed_services = failed_services


class RegistryError(ComposePilotError):
    """Raised when tagging, authentication, or pushing to a Docker registry fails."""
    pass


class ProcessCancelledError(ComposePilotError):
    """Raised when an operation is cancelled by the user via SIGINT/Ctrl+C."""
    def __init__(self, message: str = "Operation cancelled by user.") -> None:
        super().__init__(message, exit_code=130)
