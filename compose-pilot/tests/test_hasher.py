"""Unit tests for the SHA-256 change detection and caching engine."""

from pathlib import Path
from unittest.mock import MagicMock

from compose_pilot.core.hasher import ServiceHasher
from compose_pilot.models import ProjectConfig, ServiceMetadata


def test_hasher_ignore_rules(tmp_path: Path):
    cfg = ProjectConfig(project_name="testapp")
    compose_mock = MagicMock()
    hasher = ServiceHasher(cfg, compose_mock)

    assert hasher._should_ignore_dir(".git") is True
    assert hasher._should_ignore_dir("node_modules") is True
    assert hasher._should_ignore_dir(".venv") is True
    assert hasher._should_ignore_dir(".testapp_cache") is True
    assert hasher._should_ignore_dir("logs") is True
    assert hasher._should_ignore_dir("src") is False

    assert hasher._should_ignore_file(Path("app.log")) is True
    assert hasher._should_ignore_file(Path("data.tmp")) is True
    assert hasher._should_ignore_file(Path(".hidden")) is True
    assert hasher._should_ignore_file(Path("main.py")) is False


def test_hasher_change_detection(tmp_path: Path, monkeypatch):
    monkeypatch.chdir(tmp_path)
    cache_dir = tmp_path / ".testapp_cache"
    cfg = ProjectConfig(project_name="testapp", compose_files=[])
    
    compose_mock = MagicMock()
    compose_mock.get_base_cmd.return_value = ["docker", "compose"]
    compose_mock.inspect_services.return_value = {
        "web": ServiceMetadata(
            name="web",
            is_buildable=True,
            context=str(tmp_path / "web"),
            dockerfile="Dockerfile",
        )
    }

    web_dir = tmp_path / "web"
    web_dir.mkdir()
    df = web_dir / "Dockerfile"
    df.write_text("FROM alpine\n")
    app_file = web_dir / "app.py"
    app_file.write_text("print('hello v1')\n")

    hasher = ServiceHasher(cfg, compose_mock)

    # Initial state: has_code_changed should be True
    assert hasher.has_code_changed("web") is True

    # Save hash
    hasher.save_service_hash("web")
    assert (cache_dir / "web.hash").is_file()

    # Create new hasher instance reading cache
    hasher2 = ServiceHasher(cfg, compose_mock)
    assert hasher2.has_code_changed("web") is False

    # Modify source code
    app_file.write_text("print('hello v2 modified')\n")
    hasher3 = ServiceHasher(cfg, compose_mock)
    assert hasher3.has_code_changed("web") is True
