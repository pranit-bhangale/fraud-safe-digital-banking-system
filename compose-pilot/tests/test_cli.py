"""Unit tests for contextual CLI argument parsing."""

from compose_pilot.cli import parse_contextual_args


def test_cli_clean_contextual_force():
    parsed, extra = parse_contextual_args(["clean", "-f"])
    assert parsed.command == "clean"
    assert "--force" in extra


def test_cli_global_file_flag():
    parsed, extra = parse_contextual_args(["-f", "prod.yaml", "start"])
    assert parsed.command == "start"
    assert parsed.compose_files == ["prod.yaml"]


def test_cli_profiles_and_push():
    parsed, extra = parse_contextual_args([
        "--profile", "frontend",
        "--profile", "backend",
        "--push",
        "start",
        "--recreate"
    ])
    assert parsed.command == "start"
    assert parsed.profiles == ["frontend", "backend"]
    assert parsed.push is True
    assert "--recreate" in extra


def test_cli_push_command():
    parsed, extra = parse_contextual_args([
        "push", "web",
        "-r", "ghcr.io/org",
        "-t", "v1.0.0",
        "--dry-run"
    ])
    assert parsed.command == "push"
    assert parsed.registry == "ghcr.io/org"
    assert parsed.tags == ["v1.0.0"]
    assert parsed.dry_run is True
    assert "web" in extra
