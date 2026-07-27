"""
IgnitionAI — Configuration TEMPLATE
Copy this file to config.py and fill in your actual values.
NEVER commit config.py with real API keys.
"""

# ─── Gemini AI Configuration ───────────────────────────────────────────────
# Get your key from: https://aistudio.google.com/app/apikey
GEMINI_API_KEY = "YOUR_GEMINI_API_KEY_HERE"
GEMINI_MODEL_NAME = "gemini-2.0-flash-lite"

# ─── App Metadata ──────────────────────────────────────────────────────────
APP_NAME = "IgnitionAI"
APP_VERSION = "1.0.0 MVP"
APP_SUBTITLE = "AI-Powered Vehicle Diagnostics"

# ─── Theme Colors ──────────────────────────────────────────────────────────
COLOR_BG_DARKEST   = "#060E1A"
COLOR_BG_DARK      = "#0A1628"
COLOR_BG_PANEL     = "#0D1F3C"
COLOR_BG_CARD      = "#122040"
COLOR_SIDEBAR      = "#071120"

COLOR_ACCENT_BLUE  = "#1565C0"
COLOR_LIGHT_BLUE   = "#5B9BD5"
COLOR_LIGHTER_BLUE = "#A8D1F5"
COLOR_CYAN         = "#00D4FF"
COLOR_CYAN_DIM     = "#0099BB"

COLOR_TEXT_PRIMARY  = "#E8F4FD"
COLOR_TEXT_SECONDARY = "#8BB8D4"
COLOR_TEXT_MUTED    = "#4A7098"

COLOR_SUCCESS      = "#00E676"
COLOR_WARNING      = "#FFB300"
COLOR_DANGER       = "#FF5252"
COLOR_INFO         = "#40C4FF"

COLOR_BORDER       = "#1A3A5C"
COLOR_HOVER        = "#1A3A6E"

SEVERITY_CRITICAL = COLOR_DANGER
SEVERITY_HIGH     = "#FF8A65"
SEVERITY_MEDIUM   = COLOR_WARNING
SEVERITY_LOW      = COLOR_SUCCESS
