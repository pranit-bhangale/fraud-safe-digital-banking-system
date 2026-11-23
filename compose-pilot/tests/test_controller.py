"""Unit tests for the master lifecycle controller logic."""

from unittest.mock import MagicMock, patch

from compose_pilot.core.controller import LifecycleController
from compose_pilot.models import ProjectConfig, ServiceMetadata


def test_check_and_prepare_services_decision_matrix():
    cfg = ProjectConfig(project_name="testapp")
    controller = LifecycleController(cfg)

    controller.compose.get_services = MagicMock(return_value=["web", "db"])
    controller.compose.inspect_services = MagicMock(return_value={
        "web": ServiceMetadata(name="web", is_buildable=True, image="testapp-web:latest"),
        "db": ServiceMetadata(name="db", is_buildable=False, image="postgres:15"),
    })
    controller.compose.get_service_image = MagicMock(side_effect=lambda s: f"testapp-{s}:latest")
    controller.compose.get_base_cmd = MagicMock(return_value=["docker", "compose"])

    # Simulate: web has code changed, db is up to date, both containers exist
    controller.compose.image_exists_locally = MagicMock(return_value=True)
    controller.compose.get_container_ids = MagicMock(return_value=["cid_1"])
    controller.hasher.has_code_changed = MagicMock(side_effect=lambda s: s == "web")
    controller.hasher.save_service_hash = MagicMock()

    with patch("compose_pilot.core.controller.run_command") as mock_run:
        mock_run.return_value = MagicMock(returncode=0)
        with patch.object(controller, "_execute_builds") as mock_build:
            res = controller.check_and_prepare_services(force_recreate=False)
            assert "web" in res.services_to_rebuild
            assert "web" in res.services_to_recreate
            assert "db" in res.up_to_date
            mock_build.assert_called_once_with(["web"], None)


def test_get_endpoints():
    cfg = ProjectConfig(project_name="demo")
    controller = LifecycleController(cfg)

    controller.compose.get_services = MagicMock(return_value=["web", "db", "secure"])
    controller.compose.inspect_services = MagicMock(return_value={
        "web": ServiceMetadata(name="web", is_buildable=True, ports=["8080"]),
        "db": ServiceMetadata(name="db", is_buildable=False, ports=[]),
        "secure": ServiceMetadata(name="secure", is_buildable=False, ports=["443"]),
    })

    endpoints = dict(controller.get_endpoints())
    assert endpoints["web"] == ["http://localhost:8080"]
    assert endpoints["db"] == ["(internal network / no published ports)"]
    assert endpoints["secure"] == ["https://localhost:443"]
