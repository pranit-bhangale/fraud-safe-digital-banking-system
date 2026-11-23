"""Unit tests for configuration discovery and resolution."""

import os
from pathlib import Path
import pytest

from compose_pilot.config import (
    build_project_config,
    detect_compose_files,
    detect_env_file,
    detect_project_name,
    detect_registry,
    detect_target_tags,
    normalize_name,
)
from compose_pilot.exceptions import ConfigurationError


def test_normalize_name():
    assert normalize_name("My Project!") == "my-project"
    assert normalize_name("---App__Demo---") == "app-demo"
    assert normalize_name("ComposePilot") == "composepilot"


def test_detect_compose_files_cli(tmp_path: Path):
    f1 = tmp_path / "custom.yml"
    f1.write_text("services: {}\n")
    files = detect_compose_files([str(f1)])
    assert len(files) == 1
    assert files[0] == f1


def test_detect_compose_files_missing():
    with pytest.raises(ConfigurationError):
        detect_compose_files(["non_existent_file.yml"])


def test_detect_compose_files_default(tmp_path: Path, monkeypatch):
    monkeypatch.chdir(tmp_path)
    # No file initially
    assert detect_compose_files() == []

    primary = tmp_path / "docker-compose.yml"
    primary.write_text("services: {}\n")
    detected = detect_compose_files()
    assert len(detected) == 1
    assert detected[0].name == "docker-compose.yml"

    # Add override
    override = tmp_path / "docker-compose.override.yml"
    override.write_text("services: {}\n")
    detected_with_override = detect_compose_files()
    assert len(detected_with_override) == 2
    assert detected_with_override[1].name == "docker-compose.override.yml"


def test_detect_env_file(tmp_path: Path, monkeypatch):
    monkeypatch.chdir(tmp_path)
    assert detect_env_file() is None

    env = tmp_path / ".env"
    env.write_text("FOO=BAR\n")
    assert detect_env_file().resolve() == env.resolve()

    custom = tmp_path / ".env.prod"
    custom.write_text("FOO=PROD\n")
    assert detect_env_file(custom).resolve() == custom.resolve()


def test_detect_target_tags(monkeypatch):
    assert detect_target_tags(["v1.0", "v1.1"]) == ["v1.0", "v1.1"]
    assert detect_target_tags(["v1.0,v2.0"]) == ["v1.0", "v2.0"]

    monkeypatch.setenv("IMAGE_TAG", "prod-2026")
    assert detect_target_tags() == ["prod-2026"]


def test_detect_registry(monkeypatch):
    assert detect_registry("ghcr.io/org/") == "ghcr.io/org"
    monkeypatch.setenv("DOCKER_REGISTRY", "docker.io/myuser/")
    assert detect_registry() == "docker.io/myuser"
