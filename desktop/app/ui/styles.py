"""
IgnitionAI - Global UI Stylesheet
Dark Blue + Light Blue premium theme
"""

from app.config import *


def get_stylesheet() -> str:
    return f"""
    /* ═══════════════════════════════════════════════════════════
       IGNITIONAI GLOBAL STYLESHEET
       Theme: Dark Navy + Light Blue
    ═══════════════════════════════════════════════════════════ */

    /* ── Base Window ─────────────────────────────────────── */
    QMainWindow, QWidget {{
        background-color: {COLOR_BG_DARK};
        color: {COLOR_TEXT_PRIMARY};
        font-family: "Segoe UI", "Inter", sans-serif;
        font-size: 13px;
    }}

    /* ── Panels / Frames ─────────────────────────────────── */
    QFrame#card {{
        background-color: {COLOR_BG_PANEL};
        border: 1px solid {COLOR_BORDER};
        border-radius: 12px;
        padding: 4px;
    }}
    QFrame#sidebar {{
        background-color: {COLOR_SIDEBAR};
        border-right: 1px solid {COLOR_BORDER};
    }}
    QFrame#header {{
        background-color: {COLOR_BG_DARKEST};
        border-bottom: 1px solid {COLOR_BORDER};
    }}
    QFrame#stat_card {{
        background-color: {COLOR_BG_CARD};
        border: 1px solid {COLOR_BORDER};
        border-radius: 10px;
    }}

    /* ── Labels ──────────────────────────────────────────── */
    QLabel {{
        color: {COLOR_TEXT_PRIMARY};
        background: transparent;
    }}
    QLabel#title {{
        color: {COLOR_TEXT_PRIMARY};
        font-size: 22px;
        font-weight: 700;
    }}
    QLabel#subtitle {{
        color: {COLOR_TEXT_SECONDARY};
        font-size: 13px;
    }}
    QLabel#section_header {{
        color: {COLOR_LIGHTER_BLUE};
        font-size: 11px;
        font-weight: 600;
        letter-spacing: 1.5px;
    }}
    QLabel#stat_value {{
        color: {COLOR_CYAN};
        font-size: 28px;
        font-weight: 700;
    }}
    QLabel#stat_label {{
        color: {COLOR_TEXT_SECONDARY};
        font-size: 11px;
    }}
    QLabel#severity_critical {{
        color: {COLOR_DANGER};
        font-weight: 700;
    }}
    QLabel#severity_high {{
        color: #FF8A65;
        font-weight: 600;
    }}
    QLabel#severity_medium {{
        color: {COLOR_WARNING};
        font-weight: 600;
    }}
    QLabel#severity_low {{
        color: {COLOR_SUCCESS};
        font-weight: 600;
    }}
    QLabel#ai_badge {{
        background-color: {COLOR_ACCENT_BLUE};
        color: white;
        border-radius: 10px;
        padding: 2px 8px;
        font-size: 10px;
        font-weight: 600;
    }}

    /* ── Buttons ─────────────────────────────────────────── */
    QPushButton {{
        background-color: {COLOR_ACCENT_BLUE};
        color: white;
        border: none;
        border-radius: 8px;
        padding: 10px 20px;
        font-size: 13px;
        font-weight: 600;
        min-height: 36px;
    }}
    QPushButton:hover {{
        background-color: #1976D2;
    }}
    QPushButton:pressed {{
        background-color: {COLOR_BG_PANEL};
    }}
    QPushButton#primary_btn {{
        background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
            stop:0 {COLOR_ACCENT_BLUE}, stop:1 #0D47A1);
        color: white;
        border-radius: 10px;
        padding: 12px 28px;
        font-size: 14px;
        font-weight: 700;
        min-height: 44px;
    }}
    QPushButton#primary_btn:hover {{
        background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
            stop:0 #1976D2, stop:1 {COLOR_ACCENT_BLUE});
    }}
    QPushButton#secondary_btn {{
        background-color: transparent;
        color: {COLOR_LIGHT_BLUE};
        border: 1px solid {COLOR_LIGHT_BLUE};
        border-radius: 8px;
    }}
    QPushButton#secondary_btn:hover {{
        background-color: rgba(91, 155, 213, 0.1);
    }}
    QPushButton#danger_btn {{
        background-color: {COLOR_DANGER};
    }}
    QPushButton#danger_btn:hover {{
        background-color: #D32F2F;
    }}
    QPushButton#sim_active {{
        background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
            stop:0 #00897B, stop:1 #00695C);
        color: white;
        border-radius: 8px;
        font-weight: 700;
    }}

    /* ── Sidebar nav buttons ─────────────────────────────── */
    QPushButton#nav_btn {{
        background-color: transparent;
        color: {COLOR_TEXT_SECONDARY};
        border: none;
        border-radius: 0;
        padding: 14px 20px;
        font-size: 13px;
        font-weight: 500;
        text-align: left;
        min-height: 44px;
    }}
    QPushButton#nav_btn:hover {{
        background-color: {COLOR_HOVER};
        color: {COLOR_TEXT_PRIMARY};
    }}
    QPushButton#nav_btn_active {{
        background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
            stop:0 rgba(21,101,192,0.5), stop:1 transparent);
        color: {COLOR_CYAN};
        border: none;
        border-left: 3px solid {COLOR_CYAN};
        border-radius: 0;
        padding: 14px 20px;
        font-size: 13px;
        font-weight: 700;
        text-align: left;
        min-height: 44px;
    }}

    /* ── Input Fields ────────────────────────────────────── */
    QLineEdit, QTextEdit, QPlainTextEdit {{
        background-color: {COLOR_BG_DARKEST};
        color: {COLOR_TEXT_PRIMARY};
        border: 1px solid {COLOR_BORDER};
        border-radius: 8px;
        padding: 8px 12px;
        font-size: 13px;
        selection-background-color: {COLOR_ACCENT_BLUE};
    }}
    QLineEdit:focus, QTextEdit:focus, QPlainTextEdit:focus {{
        border: 1px solid {COLOR_LIGHT_BLUE};
    }}
    QLineEdit::placeholder {{
        color: {COLOR_TEXT_MUTED};
    }}

    /* ── Tables ──────────────────────────────────────────── */
    QTableWidget {{
        background-color: {COLOR_BG_DARKEST};
        color: {COLOR_TEXT_PRIMARY};
        border: 1px solid {COLOR_BORDER};
        border-radius: 8px;
        gridline-color: {COLOR_BORDER};
        selection-background-color: {COLOR_ACCENT_BLUE};
    }}
    QTableWidget::item {{
        padding: 6px 10px;
    }}
    QTableWidget::item:selected {{
        background-color: {COLOR_ACCENT_BLUE};
        color: white;
    }}
    QHeaderView::section {{
        background-color: {COLOR_BG_PANEL};
        color: {COLOR_LIGHTER_BLUE};
        padding: 8px 10px;
        border: none;
        border-bottom: 1px solid {COLOR_BORDER};
        font-weight: 600;
        font-size: 11px;
        letter-spacing: 0.8px;
    }}

    /* ── Lists ───────────────────────────────────────────── */
    QListWidget {{
        background-color: {COLOR_BG_DARKEST};
        color: {COLOR_TEXT_PRIMARY};
        border: 1px solid {COLOR_BORDER};
        border-radius: 8px;
        padding: 4px;
    }}
    QListWidget::item {{
        padding: 8px 12px;
        border-radius: 6px;
        margin: 2px 4px;
    }}
    QListWidget::item:hover {{
        background-color: {COLOR_HOVER};
    }}
    QListWidget::item:selected {{
        background-color: {COLOR_ACCENT_BLUE};
    }}

    /* ── ComboBox ────────────────────────────────────────── */
    QComboBox {{
        background-color: {COLOR_BG_DARKEST};
        color: {COLOR_TEXT_PRIMARY};
        border: 1px solid {COLOR_BORDER};
        border-radius: 8px;
        padding: 8px 12px;
        min-height: 36px;
    }}
    QComboBox:hover {{
        border: 1px solid {COLOR_LIGHT_BLUE};
    }}
    QComboBox::drop-down {{
        border: none;
        width: 24px;
    }}
    QComboBox::down-arrow {{
        width: 12px;
        height: 12px;
    }}
    QComboBox QAbstractItemView {{
        background-color: {COLOR_BG_PANEL};
        color: {COLOR_TEXT_PRIMARY};
        border: 1px solid {COLOR_BORDER};
        selection-background-color: {COLOR_ACCENT_BLUE};
    }}

    /* ── ScrollBars ──────────────────────────────────────── */
    QScrollBar:vertical {{
        background: {COLOR_BG_DARKEST};
        width: 8px;
        border-radius: 4px;
        margin: 0;
    }}
    QScrollBar::handle:vertical {{
        background: {COLOR_BORDER};
        border-radius: 4px;
        min-height: 20px;
    }}
    QScrollBar::handle:vertical:hover {{
        background: {COLOR_TEXT_MUTED};
    }}
    QScrollBar::add-line:vertical, QScrollBar::sub-line:vertical {{
        height: 0;
    }}
    QScrollBar:horizontal {{
        background: {COLOR_BG_DARKEST};
        height: 8px;
        border-radius: 4px;
    }}
    QScrollBar::handle:horizontal {{
        background: {COLOR_BORDER};
        border-radius: 4px;
        min-width: 20px;
    }}

    /* ── Splitters ───────────────────────────────────────── */
    QSplitter::handle {{
        background-color: {COLOR_BORDER};
        width: 1px;
        height: 1px;
    }}

    /* ── Tooltips ────────────────────────────────────────── */
    QToolTip {{
        background-color: {COLOR_BG_PANEL};
        color: {COLOR_TEXT_PRIMARY};
        border: 1px solid {COLOR_BORDER};
        border-radius: 6px;
        padding: 6px 10px;
        font-size: 12px;
    }}

    /* ── Progress Bar ────────────────────────────────────── */
    QProgressBar {{
        background-color: {COLOR_BG_DARKEST};
        border: 1px solid {COLOR_BORDER};
        border-radius: 6px;
        height: 8px;
        text-align: center;
    }}
    QProgressBar::chunk {{
        background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
            stop:0 {COLOR_ACCENT_BLUE}, stop:1 {COLOR_CYAN});
        border-radius: 6px;
    }}

    /* ── Tab Widget ──────────────────────────────────────── */
    QTabWidget::pane {{
        border: 1px solid {COLOR_BORDER};
        border-radius: 8px;
        background: {COLOR_BG_PANEL};
    }}
    QTabBar::tab {{
        background: {COLOR_BG_DARKEST};
        color: {COLOR_TEXT_SECONDARY};
        padding: 8px 20px;
        border-top-left-radius: 8px;
        border-top-right-radius: 8px;
        margin-right: 2px;
    }}
    QTabBar::tab:selected {{
        background: {COLOR_BG_PANEL};
        color: {COLOR_CYAN};
        font-weight: 600;
    }}
    QTabBar::tab:hover {{
        background: {COLOR_BG_PANEL};
        color: {COLOR_TEXT_PRIMARY};
    }}

    /* ── Message Box ─────────────────────────────────────── */
    QMessageBox {{
        background-color: {COLOR_BG_PANEL};
        color: {COLOR_TEXT_PRIMARY};
    }}
    QMessageBox QPushButton {{
        min-width: 80px;
    }}
    """


def get_severity_color(score: float) -> str:
    """Return a color based on a 0-10 points_weight score."""
    if score >= 9:
        return "#FF5252"   # Critical - Red
    elif score >= 7:
        return "#FF8A65"   # High - Orange-red
    elif score >= 5:
        return "#FFB300"   # Medium - Amber
    else:
        return "#00E676"   # Low - Green


def get_confidence_color(confidence: float) -> str:
    """Return a color based on 0.0-1.0 confidence."""
    if confidence >= 0.75:
        return "#00E676"
    elif confidence >= 0.5:
        return "#FFB300"
    else:
        return "#FF8A65"
