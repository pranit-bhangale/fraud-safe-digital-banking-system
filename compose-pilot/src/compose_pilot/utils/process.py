"""Process execution, signal handling, process group management, and cleanup traps."""

import atexit
import os
import signal
import subprocess
import sys
from pathlib import Path
from typing import Callable, Optional, Sequence


class ProcessManager:
    """Manages active subprocesses, temporary files, and graceful signal cleanup."""
    _instance: Optional["ProcessManager"] = None

    def __init__(self) -> None:
        self.active_processes: set[subprocess.Popen] = set()
        self.temp_files: set[Path] = set()
        self._handlers_installed = False

    @classmethod
    def get(cls) -> "ProcessManager":
        """Get or initialize singleton instance."""
        if cls._instance is None:
            cls._instance = cls()
            cls._instance.install_handlers()
        return cls._instance

    def install_handlers(self) -> None:
        """Install signal handlers and exit callbacks."""
        if self._handlers_installed:
            return

        atexit.register(self.cleanup)

        def signal_handler(signum, frame):
            self.cleanup()
            sys.exit(128 + signum)

        for sig in (signal.SIGINT, signal.SIGTERM):
            try:
                signal.signal(sig, signal_handler)
            except (ValueError, AttributeError):
                pass

        if hasattr(signal, "SIGHUP"):
            try:
                signal.signal(signal.SIGHUP, signal_handler)
            except (ValueError, AttributeError):
                pass

        self._handlers_installed = True

    def register_process(self, proc: subprocess.Popen) -> None:
        """Register a running child process to be killed on exit."""
        self.active_processes.add(proc)

    def unregister_process(self, proc: subprocess.Popen) -> None:
        """Unregister a finished child process."""
        self.active_processes.discard(proc)

    def register_temp_file(self, path: Path | str) -> None:
        """Register a temporary file to be deleted on exit."""
        self.temp_files.add(Path(path))

    def cleanup(self) -> None:
        """Terminate all active child processes, delete temp files, and restore cursor."""
        for proc in list(self.active_processes):
            try:
                if proc.poll() is None:
                    proc.terminate()
                    try:
                        proc.wait(timeout=2.0)
                    except subprocess.TimeoutExpired:
                        proc.kill()
            except Exception:
                pass
        self.active_processes.clear()

        for temp in list(self.temp_files):
            try:
                if temp.is_file():
                    temp.unlink(missing_ok=True)
            except Exception:
                pass
        self.temp_files.clear()

        # Unhide cursor if terminal
        if hasattr(sys.stdout, "isatty") and sys.stdout.isatty():
            try:
                sys.stdout.write("\033[?25h")
                sys.stdout.flush()
            except Exception:
                pass


def run_command(
    cmd: Sequence[str],
    check: bool = True,
    capture_output: bool = True,
    text: bool = True,
    cwd: Optional[Path | str] = None,
    env: Optional[dict[str, str]] = None,
) -> subprocess.CompletedProcess:
    """Execute a command synchronously and safely within the ProcessManager lifecycle."""
    mgr = ProcessManager.get()
    
    current_env = os.environ.copy()
    if env:
        current_env.update(env)

    try:
        proc = subprocess.Popen(
            cmd,
            stdin=subprocess.DEVNULL if capture_output else None,
            stdout=subprocess.PIPE if capture_output else None,
            stderr=subprocess.PIPE if capture_output else None,
            text=text,
            cwd=str(cwd) if cwd else None,
            env=current_env,
        )
        mgr.register_process(proc)
        stdout, stderr = proc.communicate()
        mgr.unregister_process(proc)

        res = subprocess.CompletedProcess(
            args=cmd,
            returncode=proc.returncode,
            stdout=stdout or "",
            stderr=stderr or "",
        )
        if check and proc.returncode != 0:
            raise subprocess.CalledProcessError(
                proc.returncode, cmd, output=stdout, stderr=stderr
            )
        return res
    except KeyboardInterrupt:
        mgr.cleanup()
        raise


def stream_command(
    cmd: Sequence[str],
    line_callback: Optional[Callable[[str], None]] = None,
    cwd: Optional[Path | str] = None,
    env: Optional[dict[str, str]] = None,
) -> int:
    """Stream command output line by line in real time."""
    mgr = ProcessManager.get()
    current_env = os.environ.copy()
    if env:
        current_env.update(env)

    proc = subprocess.Popen(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
        cwd=str(cwd) if cwd else None,
        env=current_env,
    )
    mgr.register_process(proc)

    try:
        if proc.stdout:
            for line in iter(proc.stdout.readline, ""):
                if line_callback:
                    line_callback(line)
        proc.wait()
        return proc.returncode
    except KeyboardInterrupt:
        mgr.cleanup()
        raise
    finally:
        mgr.unregister_process(proc)
