"""Platform compatibility, path normalization, and terminal capability detection."""

import os
import re
import sys
from pathlib import Path


def is_windows() -> bool:
    """Return True if running on a Windows platform."""
    return sys.platform.startswith("win") or os.name == "nt"


def normalize_unix_path(path_str: str) -> str:
    """Convert Windows-style paths (e.g. C:\\project\\app) into forward-slash or POSIX representation.
    
    Equivalent to to_unix_path() from the bash script.
    """
    if not path_str:
        return ""
    
    clean = path_str.replace("\\", "/")
    # Match Windows drive letter: C:/foo -> /c/foo
    match = re.match(r"^([a-zA-Z]):/(.*)$", clean)
    if match:
        drive = match.group(1).lower()
        rest = match.group(2)
        return f"/{drive}/{rest}"
    
    return clean


def supports_color() -> bool:
    """Check if the current terminal supports ANSI colors.
    
    Honors NO_COLOR standard and non-interactive (non-TTY) environments.
    """
    if os.environ.get("NO_COLOR"):
        return False
    if not hasattr(sys.stdout, "isatty") or not sys.stdout.isatty():
        return False
    if os.environ.get("TERM") == "dumb":
        return False
    return True


def get_terminal_width(default: int = 80) -> int:
    """Get the current terminal width with safe fallback."""
    try:
        columns, _ = os.get_terminal_size()
        return columns
    except (OSError, ValueError):
        return default
