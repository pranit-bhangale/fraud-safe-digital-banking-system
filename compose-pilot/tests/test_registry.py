"""Unit tests for registry tagging, pushing, and domain resolution."""

from unittest.mock import MagicMock
import pytest

from compose_pilot.core.registry import RegistryManager
from compose_pilot.exceptions import RegistryError
from compose_pilot.models import ProjectConfig


def test_resolve_target_repo():
    cfg = ProjectConfig(project_name="demo")
    compose_mock = MagicMock()
    hasher_mock = MagicMock()
    reg_mgr = RegistryManager(cfg, compose_mock, hasher_mock)

    # 1. Registry explicitly provided
    repo = reg_mgr.resolve_target_repo("demo-web:latest", "ghcr.io/myorg")
    assert repo == "ghcr.io/myorg/demo-web"

    # 2. Local image contains registry/namespace
    repo2 = reg_mgr.resolve_target_repo("docker.io/user/app:v1", None)
    assert repo2 == "docker.io/user/app"

    # 3. No registry and local image is unqualified
    with pytest.raises(RegistryError):
        reg_mgr.resolve_target_repo("local-only-image:latest", None)


def test_extract_registry_domain():
    cfg = ProjectConfig(project_name="demo")
    compose_mock = MagicMock()
    hasher_mock = MagicMock()
    reg_mgr = RegistryManager(cfg, compose_mock, hasher_mock)

    assert reg_mgr.extract_registry_domain("ghcr.io/org/app") == "ghcr.io"
    assert reg_mgr.extract_registry_domain("localhost:5000/app") == "localhost:5000"
    assert reg_mgr.extract_registry_domain("myuser/app") is None
