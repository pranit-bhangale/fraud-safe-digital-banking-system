"""Constants, glyphs, spinner frames, and default configuration values."""

from typing import Final, Sequence

DEFAULT_MAX_WAIT: Final[int] = 120
DEFAULT_INTERVAL: Final[int] = 3
DEFAULT_LOG_TAIL: Final[int] = 100
DIAGNOSTIC_LOG_TAIL: Final[int] = 25

SPINNER_FRAMES: Final[Sequence[str]] = (
    "⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"
)

# Standard directory and file patterns to ignore during recursive hash computation
DEFAULT_HASH_IGNORE_DIRS: Final[frozenset[str]] = frozenset({
    ".git",
    "node_modules",
    ".gradle",
    "build",
    "dist",
    "target",
    ".next",
    "__pycache__",
    ".venv",
    "venv",
    ".idea",
    ".vscode",
    ".pytest_cache",
    ".coverage",
    "htmlcov",
    "logs",
})

DEFAULT_HASH_IGNORE_EXTENSIONS: Final[frozenset[str]] = frozenset({
    ".log",
    ".tmp",
    ".pyc",
    ".pyo",
    ".pyd",
    ".swp",
    ".swo",
})

# Color and Emoji Glyphs
class Symbols:
    """Symbols and glyphs for terminal output."""
    CHECK = "✓"
    CROSS = "✗"
    WARNING = "⚠"
    WAIT = "⏳"
    ROCKET = "🚀"
    HAMMER = "🔨"
    RECYCLE = "🔄"
    WHALE = "🐳"
    CHART = "📊"
    GLOBE = "🌐"
    PACKAGE = "📦"
    BROOM = "🧹"
    STOP = "🛑"
    TRASH = "🗑️"
    IMAGE = "🖼️"
    SPARKLES = "✨"
    SCROLL = "📜"
    BULB = "💡"
    CLIPBOARD = "📋"
    SEARCH = "🔍"

class PlainSymbols:
    """ASCII fallback glyphs for non-color or non-interactive terminals."""
    CHECK = "[OK]"
    CROSS = "[FAIL]"
    WARNING = "[WARN]"
    WAIT = "[WAIT]"
    ROCKET = "[START]"
    HAMMER = "[BUILD]"
    RECYCLE = "[RECREATE]"
    WHALE = "[DOCKER]"
    CHART = "[STATUS]"
    GLOBE = "[ENDPOINTS]"
    PACKAGE = "[PUSH]"
    BROOM = "[CLEAN]"
    STOP = "[STOP]"
    TRASH = "[DELETE]"
    IMAGE = "[IMAGE]"
    SPARKLES = "[DONE]"
    SCROLL = "[LOGS]"
    BULB = "[INFO]"
    CLIPBOARD = "[CONFIG]"
    SEARCH = "[CHECK]"
