"""Code, configuration, and build context SHA-256 hashing engine."""

import hashlib
import os
import tempfile
from pathlib import Path
from typing import Optional

from compose_pilot.constants import DEFAULT_HASH_IGNORE_DIRS, DEFAULT_HASH_IGNORE_EXTENSIONS
from compose_pilot.core.compose import ComposeManager
from compose_pilot.models import ProjectConfig
from compose_pilot.utils.process import run_command


class ServiceHasher:
    """Computes and tracks deterministic SHA-256 checksums for Docker Compose services."""

    def __init__(self, config: ProjectConfig, compose: ComposeManager) -> None:
        self.config = config
        self.compose = compose
        self._computed_hashes: dict[str, str] = {}
        self.cache_dir = config.cache_dir

    def _hash_file(self, file_path: Path) -> Optional[str]:
        """Compute SHA-256 hash for a single file."""
        if not file_path.is_file():
            return None
        hasher = hashlib.sha256()
        try:
            with open(file_path, "rb") as f:
                while chunk := f.read(65536):
                    hasher.update(chunk)
            return hasher.hexdigest()
        except (OSError, PermissionError):
            return None

    def _should_ignore_dir(self, dir_name: str) -> bool:
        """Check if a directory name matches ignore rules."""
        if dir_name in DEFAULT_HASH_IGNORE_DIRS:
            return True
        if dir_name == self.cache_dir.name or dir_name == self.config.logs_dir.name:
            return True
        return False

    def _should_ignore_file(self, file_path: Path) -> bool:
        """Check if a file should be ignored based on extension or name."""
        if file_path.suffix in DEFAULT_HASH_IGNORE_EXTENSIONS:
            return True
        if file_path.name.startswith("."):
            return True
        return False

    def compute_service_hash(self, service: str, force_refresh: bool = False) -> str:
        """Compute comprehensive SHA-256 hash representing config, compose files, and build context."""
        if not force_refresh and service in self._computed_hashes:
            return self._computed_hashes[service]

        entries: list[str] = []

        # 1. Compose config hash for this service
        cmd = self.compose.get_base_cmd() + ["config", "--hash", service]
        res = run_command(cmd, check=False)
        if res.returncode == 0 and res.stdout.strip():
            # Usually outputs: <service> <hash>
            parts = res.stdout.strip().split()
            if len(parts) >= 2:
                entries.append(f"cfg:{parts[1]}")
            else:
                entries.append(f"cfg:{res.stdout.strip()}")

        # 2. Hash active compose files
        for cf in self.config.compose_files:
            h = self._hash_file(cf)
            if h:
                entries.append(f"compose:{cf.name}:{h}")

        # 3. Hash environment file
        if self.config.env_file and self.config.env_file.is_file():
            h = self._hash_file(self.config.env_file)
            if h:
                entries.append(f"env:{h}")

        # 4. Hash build context and Dockerfile if service is buildable
        metadata = self.compose.inspect_services().get(service)
        if metadata and metadata.is_buildable:
            raw_ctx = metadata.context or "."
            ctx_path = Path(raw_ctx)
            if not ctx_path.is_absolute():
                ctx_path = (Path.cwd() / ctx_path).resolve()

            # Hash Dockerfile
            df_name = metadata.dockerfile or "Dockerfile"
            df_path = ctx_path / df_name
            if not df_path.is_file():
                df_path = Path(df_name)

            if df_path.is_file():
                h = self._hash_file(df_path)
                if h:
                    entries.append(f"dockerfile:{h}")

            # Recursively hash files in build context
            if ctx_path.is_dir():
                file_hashes: list[str] = []
                for root, dirs, files in os.walk(ctx_path, followlinks=False):
                    # Filter out ignored directories in-place
                    dirs[:] = [d for d in dirs if not self._should_ignore_dir(d)]

                    for f in files:
                        p = Path(root) / f
                        if self._should_ignore_file(p):
                            continue
                        h = self._hash_file(p)
                        if h:
                            rel = p.relative_to(ctx_path).as_posix()
                            file_hashes.append(f"{rel}:{h}")

                file_hashes.sort()
                entries.extend(file_hashes)

        # Compute aggregate SHA-256
        aggregate = hashlib.sha256()
        for item in sorted(entries):
            aggregate.update(item.encode("utf-8"))
            aggregate.update(b"\n")

        final_hash = aggregate.hexdigest()
        self._computed_hashes[service] = final_hash
        return final_hash

    def has_code_changed(self, service: str) -> bool:
        """Check whether code, Dockerfile, or configuration has changed since last cached build."""
        current_hash = self.compute_service_hash(service)
        if not current_hash:
            return True

        hash_file = self.cache_dir / f"{service}.hash"
        if not hash_file.is_file():
            return True

        try:
            cached_hash = hash_file.read_text("utf-8").strip()
            return cached_hash != current_hash
        except (OSError, PermissionError):
            return True

    def save_service_hash(self, service: str) -> None:
        """Atomically persist computed service hash to cache directory."""
        current_hash = self.compute_service_hash(service)
        if not current_hash:
            return

        self.cache_dir.mkdir(parents=True, exist_ok=True)
        hash_file = self.cache_dir / f"{service}.hash"

        # Atomic write
        temp_fd, temp_path = tempfile.mkstemp(
            dir=str(self.cache_dir), prefix=f"{service}_", suffix=".tmp"
        )
        try:
            with os.fdopen(temp_fd, "w", encoding="utf-8") as f:
                f.write(f"{current_hash}\n")
            os.replace(temp_path, hash_file)
        except Exception:
            if os.path.exists(temp_path):
                try:
                    os.unlink(temp_path)
                except OSError:
                    pass
            raise
