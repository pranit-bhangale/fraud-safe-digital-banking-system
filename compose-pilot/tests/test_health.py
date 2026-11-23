"""Unit tests for container health check states and evaluation."""

from unittest.mock import MagicMock, patch
import subprocess

from compose_pilot.core.health import HealthMonitor
from compose_pilot.models import ProjectConfig, ServiceState


def test_health_evaluation_healthy():
    cfg = ProjectConfig(project_name="demo")
    compose_mock = MagicMock()
    compose_mock.get_container_ids.return_value = ["cid_12345"]

    monitor = HealthMonitor(cfg, compose_mock)

    with patch("compose_pilot.core.health.run_command") as mock_run:
        mock_run.return_value = subprocess.CompletedProcess(
            args=[], returncode=0, stdout="running|healthy|0\n"
        )
        health = monitor.inspect_service_health("api")
        assert health.state == ServiceState.HEALTHY
        assert health.container_status == "running"
        assert health.health_status == "healthy"
        assert health.exit_code == 0


def test_health_evaluation_unhealthy():
    cfg = ProjectConfig(project_name="demo")
    compose_mock = MagicMock()
    compose_mock.get_container_ids.return_value = ["cid_12345"]

    monitor = HealthMonitor(cfg, compose_mock)

    with patch("compose_pilot.core.health.run_command") as mock_run:
        mock_run.return_value = subprocess.CompletedProcess(
            args=[], returncode=0, stdout="running|unhealthy|0\n"
        )
        health = monitor.inspect_service_health("api")
        assert health.state == ServiceState.UNHEALTHY
        assert health.state.is_terminal_failure is True


def test_health_evaluation_failed_exit():
    cfg = ProjectConfig(project_name="demo")
    compose_mock = MagicMock()
    compose_mock.get_container_ids.return_value = ["cid_12345"]

    monitor = HealthMonitor(cfg, compose_mock)

    with patch("compose_pilot.core.health.run_command") as mock_run:
        mock_run.return_value = subprocess.CompletedProcess(
            args=[], returncode=0, stdout="exited|none|137\n"
        )
        health = monitor.inspect_service_health("api")
        assert health.state == ServiceState.FAILED
        assert health.exit_code == 137
        assert health.state.is_terminal_failure is True


def test_health_evaluation_not_found():
    cfg = ProjectConfig(project_name="demo")
    compose_mock = MagicMock()
    compose_mock.get_container_ids.return_value = []

    monitor = HealthMonitor(cfg, compose_mock)
    health = monitor.inspect_service_health("missing")
    assert health.state == ServiceState.NOT_FOUND
