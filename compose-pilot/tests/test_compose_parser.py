"""Unit tests for Compose YAML service metadata parsing."""

from compose_pilot.core.compose import ComposeManager
from compose_pilot.models import ProjectConfig


def test_compose_yaml_parsing():
    sample_yaml = """
services:
  web:
    build:
      context: ./frontend
      dockerfile: Dockerfile.dev
    image: custom-web:v1
    ports:
      - "3000:3000"
      - "443:443"
  api:
    image: backend-api:latest
    ports:
      - "8080:8080"
  db:
    image: postgres:15
"""
    cfg = ProjectConfig(project_name="myproj")
    mgr = ComposeManager(cfg)
    services = mgr._regex_parse_services(sample_yaml)

    assert "web" in services
    web = services["web"]
    assert web.is_buildable is True
    assert web.context == "./frontend"
    assert web.dockerfile == "Dockerfile.dev"
    assert web.image == "custom-web:v1"

    assert "api" in services
    api = services["api"]
    assert api.is_buildable is False
    assert api.image == "backend-api:latest"

    assert "db" in services
    db = services["db"]
    assert db.is_buildable is False
    assert db.image == "postgres:15"


def test_check_prerequisites_without_env_file(tmp_path):
    from unittest.mock import MagicMock, patch
    compose_file = tmp_path / "docker-compose.yaml"
    compose_file.write_text("services: {}\n")

    cfg = ProjectConfig(project_name="myproj", compose_files=[compose_file], env_file=None)
    mgr = ComposeManager(cfg)

    # Should not raise ConfigurationError when env_file is None
    with patch("compose_pilot.core.compose.run_command") as mock_run:
        mock_run.return_value = MagicMock(returncode=0)
        mgr.check_prerequisites(check_daemon=True)
