"""Unit tests for the environment cleaner module."""

from unittest.mock import MagicMock, patch
import pytest

from compose_pilot.core.cleaner import EnvironmentCleaner
from compose_pilot.exceptions import ComposePilotError
from compose_pilot.models import ProjectConfig


def test_cleaner_non_interactive_without_force():
    cfg = ProjectConfig(project_name="myproj")
    compose_mock = MagicMock()
    cleaner = EnvironmentCleaner(cfg, compose_mock)

    with patch("sys.stdin.isatty", return_value=False):
        with pytest.raises(ComposePilotError) as exc_info:
            cleaner.clean(force=False)
        assert "non-interactive" in str(exc_info.value)


def test_cleaner_forced_execution():
    cfg = ProjectConfig(project_name="myproj")
    compose_mock = MagicMock()
    compose_mock.get_base_cmd.return_value = ["docker", "compose"]
    compose_mock.get_project_volumes.return_value = ["myproj_db_data"]
    compose_mock.get_project_images.return_value = ["myproj-web:latest"]

    cleaner = EnvironmentCleaner(cfg, compose_mock)

    with patch("compose_pilot.core.cleaner.run_command") as mock_run:
        mock_run.return_value = MagicMock(returncode=0)
        logs = []
        result = cleaner.clean(
            clean_volumes=True,
            clean_images=True,
            force=True,
            status_callback=logs.append,
        )
        assert result is True
        assert any("Containers stopped and removed" in l for l in logs)
        assert any("Removed volume: myproj_db_data" in l for l in logs)
        assert any("Removed image: myproj-web:latest" in l for l in logs)
