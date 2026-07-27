"""
IgnitionAI — Main Window
Professional multi-panel UI with sidebar navigation.
"""

import os
from PySide6.QtWidgets import (
    QMainWindow, QWidget, QHBoxLayout, QVBoxLayout,
    QPushButton, QLabel, QFrame, QStackedWidget, QSizePolicy, QSpacerItem
)
from PySide6.QtCore import Qt, QSize
from PySide6.QtGui import QPixmap, QFont, QIcon

from app.config import (
    APP_NAME, APP_SUBTITLE, APP_VERSION,
    COLOR_BG_DARKEST, COLOR_SIDEBAR, COLOR_BORDER,
    COLOR_TEXT_PRIMARY, COLOR_TEXT_SECONDARY, COLOR_CYAN,
)
from app.ui.styles import get_stylesheet
from app.ui.scan_panel import ScanPanel
from app.ui.results_panel import ResultsPanel
from app.ui.ai_panel import AIPanel
from app.ui.dashboard import DashboardPanel


LOGO_PATH = os.path.join(os.path.dirname(__file__), "..", "..", "assets", "logo.png")


class MainWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle(f"{APP_NAME} — AI Diagnostic Portal")
        self.resize(1440, 880)
        self.setMinimumSize(1200, 720)
        self.setStyleSheet(get_stylesheet())

        # Set window icon
        icon_pix = QPixmap(LOGO_PATH)
        if not icon_pix.isNull():
            self.setWindowIcon(QIcon(icon_pix))

        # State shared across panels
        self.last_results: dict = {}

        self._build_ui()

    def _build_ui(self):
        central = QWidget()
        self.setCentralWidget(central)
        root_layout = QHBoxLayout(central)
        root_layout.setContentsMargins(0, 0, 0, 0)
        root_layout.setSpacing(0)

        # ── Sidebar ──────────────────────────────────────────────────────────
        sidebar = QFrame()
        sidebar.setObjectName("sidebar")
        sidebar.setFixedWidth(220)
        sidebar_layout = QVBoxLayout(sidebar)
        sidebar_layout.setContentsMargins(0, 0, 0, 0)
        sidebar_layout.setSpacing(0)

        # Logo area in sidebar
        logo_frame = QFrame()
        logo_frame.setObjectName("header")
        logo_frame.setFixedHeight(80)
        logo_layout = QHBoxLayout(logo_frame)
        logo_layout.setContentsMargins(16, 12, 16, 12)

        logo_pix = QPixmap(LOGO_PATH)
        if not logo_pix.isNull():
            logo_label = QLabel()
            logo_label.setPixmap(logo_pix.scaledToHeight(42, Qt.SmoothTransformation))
            logo_layout.addWidget(logo_label)
        else:
            name_label = QLabel(APP_NAME)
            name_label.setStyleSheet(f"font-size:18px; font-weight:800; color: {COLOR_CYAN};")
            logo_layout.addWidget(name_label)

        logo_layout.addStretch()
        sidebar_layout.addWidget(logo_frame)

        # Version tag
        ver_label = QLabel(f"  {APP_VERSION}")
        ver_label.setStyleSheet(f"color: #2A4A6A; font-size: 10px; padding: 4px 20px;")
        sidebar_layout.addWidget(ver_label)

        sidebar_layout.addSpacing(12)

        # ── Nav Section Label ─────────────────────────────────────────────
        nav_section = QLabel("  NAVIGATION")
        nav_section.setObjectName("section_header")
        nav_section.setStyleSheet(f"color: #2A4A6A; font-size: 10px; font-weight:600; letter-spacing:1.5px; padding: 4px 20px;")
        sidebar_layout.addWidget(nav_section)

        # Nav buttons
        self.nav_buttons: list[QPushButton] = []
        nav_items = [
            ("🏠  Dashboard", 0),
            ("🔍  Scan Vehicle", 1),
            ("📊  Results & Analysis", 2),
            ("🤖  AI Diagnosis", 3),
        ]

        for label, idx in nav_items:
            btn = QPushButton(label)
            btn.setObjectName("nav_btn")
            btn.clicked.connect(lambda checked, i=idx: self._nav_to(i))
            sidebar_layout.addWidget(btn)
            self.nav_buttons.append(btn)

        sidebar_layout.addSpacing(20)
        sep = QFrame()
        sep.setFrameShape(QFrame.HLine)
        sep.setStyleSheet(f"color: {COLOR_BORDER}; margin: 0 20px;")
        sidebar_layout.addWidget(sep)
        sidebar_layout.addSpacing(8)

        # Tools section
        tools_label = QLabel("  TOOLS")
        tools_label.setStyleSheet(f"color: #2A4A6A; font-size: 10px; font-weight:600; letter-spacing:1.5px; padding: 4px 20px;")
        sidebar_layout.addWidget(tools_label)

        dtc_btn = QPushButton("📋  DTC Reference")
        dtc_btn.setObjectName("nav_btn")
        dtc_btn.clicked.connect(self._show_dtc_reference)
        sidebar_layout.addWidget(dtc_btn)

        sidebar_layout.addStretch()

        # Bottom status
        status_frame = QFrame()
        status_frame.setStyleSheet(f"border-top: 1px solid {COLOR_BORDER};")
        status_layout = QVBoxLayout(status_frame)
        status_layout.setContentsMargins(16, 12, 16, 12)

        self.status_dot = QLabel("● Live")
        self.status_dot.setStyleSheet(f"color: #00E676; font-size: 11px; font-weight:600;")
        status_layout.addWidget(self.status_dot)

        self.status_label = QLabel("Ready for scan")
        self.status_label.setStyleSheet(f"color: {COLOR_TEXT_SECONDARY}; font-size: 10px;")
        status_layout.addWidget(self.status_label)

        sidebar_layout.addWidget(status_frame)
        root_layout.addWidget(sidebar)

        # ── Main Content ──────────────────────────────────────────────────
        content_area = QWidget()
        content_layout = QVBoxLayout(content_area)
        content_layout.setContentsMargins(0, 0, 0, 0)
        content_layout.setSpacing(0)

        # Top header bar
        header_bar = QFrame()
        header_bar.setObjectName("header")
        header_bar.setFixedHeight(64)
        header_h = QHBoxLayout(header_bar)
        header_h.setContentsMargins(28, 0, 28, 0)

        self.page_title = QLabel("Dashboard")
        self.page_title.setObjectName("title")
        self.page_title.setStyleSheet(f"font-size:20px; font-weight:700; color:{COLOR_TEXT_PRIMARY};")
        header_h.addWidget(self.page_title)
        header_h.addStretch()

        # AI status badge
        ai_badge = QLabel("  ⚡ Gemini AI Active  ")
        ai_badge.setObjectName("ai_badge")
        header_h.addWidget(ai_badge)

        content_layout.addWidget(header_bar)

        # ── Stacked pages ─────────────────────────────────────────────────
        self.stack = QStackedWidget()

        self.dashboard = DashboardPanel(self)
        self.scan_panel = ScanPanel(self)
        self.results_panel = ResultsPanel(self)
        self.ai_panel = AIPanel(self)

        self.stack.addWidget(self.dashboard)   # index 0
        self.stack.addWidget(self.scan_panel)  # index 1
        self.stack.addWidget(self.results_panel)  # index 2
        self.stack.addWidget(self.ai_panel)    # index 3

        content_layout.addWidget(self.stack)
        root_layout.addWidget(content_area)

        # Connect scan panel → results/AI panels
        self.scan_panel.analysis_complete.connect(self._on_analysis_complete)

        # Default nav
        self._nav_to(0)

    def _nav_to(self, index: int):
        self.stack.setCurrentIndex(index)
        page_titles = ["Dashboard", "Scan Vehicle", "Results & Analysis", "AI Diagnosis"]
        self.page_title.setText(page_titles[index])

        for i, btn in enumerate(self.nav_buttons):
            btn.setObjectName("nav_btn_active" if i == index else "nav_btn")
            btn.setStyleSheet("")  # Force re-apply via Qt object name
            self.style().polish(btn)

    def _on_analysis_complete(self, results: dict):
        """Called when scan panel finishes analysis. Propagate to other panels."""
        self.last_results = results
        self.results_panel.load_results(results)
        self.ai_panel.load_results(results)
        self.dashboard.update_stats(results)
        self.status_label.setText(f"Last scan: {results.get('final_root', 'Unknown')[:30]}...")
        self._nav_to(2)  # Auto-navigate to results

    def _show_dtc_reference(self):
        from PySide6.QtWidgets import QDialog, QVBoxLayout, QTableWidget, QTableWidgetItem, QLabel
        from app.data.dtc_database import obd2_dtc_diagnostic_database

        dialog = QDialog(self)
        dialog.setWindowTitle("DTC Reference Database")
        dialog.resize(700, 500)
        dialog.setStyleSheet(get_stylesheet())
        layout = QVBoxLayout(dialog)
        layout.addWidget(QLabel("<b>OBD-II DTC Reference</b>"))

        table = QTableWidget(len(obd2_dtc_diagnostic_database), 3)
        table.setHorizontalHeaderLabels(["Code", "Description", "Severity"])
        for i, (code, spec) in enumerate(obd2_dtc_diagnostic_database.items()):
            table.setItem(i, 0, QTableWidgetItem(code))
            desc = spec.get("description", "")[:80]
            table.setItem(i, 1, QTableWidgetItem(desc))
            pw = spec.get("points_weight", 0)
            sev = "Critical" if pw >= 9 else "High" if pw >= 7 else "Medium" if pw >= 5 else "Low"
            table.setItem(i, 2, QTableWidgetItem(sev))
        table.resizeColumnsToContents()
        layout.addWidget(table)
        dialog.exec()

    def navigate_to_ai(self):
        self._nav_to(3)

    def set_status(self, message: str, active: bool = True):
        self.status_label.setText(message)
        color = "#00E676" if active else COLOR_TEXT_SECONDARY
        self.status_dot.setStyleSheet(f"color: {color}; font-size: 11px; font-weight:600;")
