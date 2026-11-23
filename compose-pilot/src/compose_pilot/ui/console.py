"""Rich console wrapper, colored section banners, and styled glyph formatting."""

import sys
from typing import Optional

# Ensure UTF-8 output encoding on Windows consoles
if sys.platform.startswith("win"):
    try:
        if hasattr(sys.stdout, "reconfigure"):
            sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        if hasattr(sys.stderr, "reconfigure"):
            sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

from rich.console import Console
from rich.theme import Theme

from compose_pilot.constants import PlainSymbols, Symbols
from compose_pilot.utils.platform import supports_color

_CUSTOM_THEME = Theme({
    "success": "bold green",
    "error": "bold red",
    "warning": "bold yellow",
    "info": "bold cyan",
    "secondary": "bold blue",
    "muted": "dim white",
})


class PilotConsole:
    """Provides styled, emoji-aware output with clean non-color fallback."""

    def __init__(self, no_color: bool = False) -> None:
        self.color_enabled = supports_color() and not no_color
        
        # Check if the terminal encoding can handle emojis
        encoding = getattr(sys.stdout, "encoding", "") or "utf-8"
        is_utf8 = "utf" in encoding.lower()
        use_emojis = self.color_enabled and is_utf8

        self.console = Console(
            no_color=not self.color_enabled,
            highlight=False,
            theme=_CUSTOM_THEME if self.color_enabled else None,
            legacy_windows=False,
        )
        self.symbols = Symbols if use_emojis else PlainSymbols

    def major_section(self, title: str) -> None:
        """Print prominent major section header."""
        self.console.print()
        if self.color_enabled:
            self.console.print(f"[info]{title}[/info]")
        else:
            self.console.print(f"=== {title} ===")

    def minor_section(self, title: str) -> None:
        """Print minor section header."""
        self.console.print()
        if self.color_enabled:
            self.console.print(f"[secondary]{title}[/secondary]")
        else:
            self.console.print(f"--- {title} ---")

    def success(self, message: str) -> None:
        """Print success message with checkmark."""
        if self.color_enabled:
            self.console.print(f"[success]{self.symbols.CHECK}[/success] {message}")
        else:
            self.console.print(f"{self.symbols.CHECK} {message}")

    def error(self, message: str) -> None:
        """Print error message to stderr."""
        if self.color_enabled:
            self.console.print(f"[error]{self.symbols.CROSS}[/error] {message}", style="error")
        else:
            sys.stderr.write(f"{self.symbols.CROSS} {message}\n")
            sys.stderr.flush()

    def warning(self, message: str) -> None:
        """Print warning message."""
        if self.color_enabled:
            self.console.print(f"[warning]{self.symbols.WARNING}[/warning] {message}")
        else:
            sys.stderr.write(f"{self.symbols.WARNING} {message}\n")
            sys.stderr.flush()

    def info(self, message: str) -> None:
        """Print informational message."""
        if self.color_enabled:
            self.console.print(f"[info]{message}[/info]")
        else:
            self.console.print(message)

    def tip(self, label: str, command: str) -> None:
        """Print actionable user tip."""
        if self.color_enabled:
            self.console.print(f"[secondary]{self.symbols.BULB} {label}:[/secondary]  {command}")
        else:
            self.console.print(f"{self.symbols.BULB} {label}:  {command}")

    def print(self, *args, **kwargs) -> None:
        """Forward directly to rich console."""
        self.console.print(*args, **kwargs)
