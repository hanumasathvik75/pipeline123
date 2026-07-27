"""
IgnitionAI — Dashboard Panel
Overview/home screen with statistics and quick-access.
"""

import os
from PySide6.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QLabel, QFrame,
    QPushButton, QScrollArea
)
from PySide6.QtCore import Qt, Signal
from PySide6.QtGui import QPixmap

from app.config import (
    COLOR_BG_PANEL, COLOR_BG_CARD, COLOR_BORDER, COLOR_CYAN,
    COLOR_TEXT_PRIMARY, COLOR_TEXT_SECONDARY, COLOR_TEXT_MUTED, COLOR_SUCCESS,
    COLOR_WARNING, COLOR_DANGER, COLOR_ACCENT_BLUE, COLOR_LIGHTER_BLUE
)
from app.ui.styles import get_outlined_logo

LOGO_PATH = os.path.join(os.path.dirname(__file__), "..", "..", "assets", "logo.png")


def _make_stat_card(value: str, label: str, color: str = None) -> QFrame:
    card = QFrame()
    card.setObjectName("stat_card")
    card.setMinimumSize(180, 100)
    layout = QVBoxLayout(card)
    layout.setContentsMargins(20, 16, 20, 16)

    val_label = QLabel(value)
    val_label.setObjectName("stat_value")
    val_label.setStyleSheet(f"font-size: 32px; font-weight: 800; color: {color or COLOR_CYAN};")
    layout.addWidget(val_label)

    lbl = QLabel(label)
    lbl.setObjectName("stat_label")
    lbl.setStyleSheet(f"color: {COLOR_TEXT_SECONDARY}; font-size: 11px; font-weight: 500;")
    layout.addWidget(lbl)
    return card


class DashboardPanel(QWidget):
    navigate_requested = Signal(int)

    def __init__(self, parent=None):
        super().__init__(parent)
        self._build_ui()

    def _build_ui(self):
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setFrameShape(QFrame.NoFrame)

        container = QWidget()
        layout = QVBoxLayout(container)
        layout.setContentsMargins(32, 28, 32, 28)
        layout.setSpacing(24)

        # ── Welcome header ────────────────────────────────────────────────
        welcome_card = QFrame()
        welcome_card.setObjectName("card")
        welcome_card.setStyleSheet(f"""
            QFrame#card {{
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
                    stop:0 #0A1628, stop:0.6 #0D1F3C, stop:1 #0A1628);
                border: 1px solid {COLOR_BORDER};
                border-radius: 16px;
            }}
        """)
        wc_layout = QHBoxLayout(welcome_card)
        wc_layout.setContentsMargins(28, 24, 28, 24)

        wc_text = QVBoxLayout()
        title = QLabel("Welcome to IgnitionAI")
        title.setStyleSheet(f"font-size: 26px; font-weight: 800; color: {COLOR_TEXT_PRIMARY};")
        sub = QLabel("Intelligent OBD-II Vehicle Diagnostics powered by Gemini AI")
        sub.setStyleSheet(f"font-size: 13px; color: {COLOR_TEXT_SECONDARY}; margin-top: 4px;")
        wc_text.addWidget(title)
        wc_text.addWidget(sub)
        wc_layout.addLayout(wc_text, 3)

        # Logo on the right (outlined)
        logo_outlined = get_outlined_logo(LOGO_PATH, 110)
        if not logo_outlined.isNull():
            logo_lbl = QLabel()
            logo_lbl.setPixmap(logo_outlined)
            logo_lbl.setAlignment(Qt.AlignRight | Qt.AlignVCenter)
            wc_layout.addWidget(logo_lbl, 1)

        layout.addWidget(welcome_card)

        # ── Stats row ─────────────────────────────────────────────────────
        stats_label = QLabel("SYSTEM STATUS")
        stats_label.setStyleSheet(f"color: #2A5A8A; font-size: 11px; font-weight: 600; letter-spacing: 1.5px;")
        layout.addWidget(stats_label)

        stats_row = QHBoxLayout()
        stats_row.setSpacing(16)

        self.stat_dtcs = _make_stat_card("—", "DTCs Detected", COLOR_TEXT_MUTED)
        self.stat_root = _make_stat_card("—", "Root Causes Found", COLOR_TEXT_MUTED)
        self.stat_conf = _make_stat_card("—", "AI Confidence", COLOR_TEXT_MUTED)
        self.stat_sev  = _make_stat_card("—", "Severity Level", COLOR_TEXT_MUTED)

        for card in [self.stat_dtcs, self.stat_root, self.stat_conf, self.stat_sev]:
            stats_row.addWidget(card)

        layout.addLayout(stats_row)

        # ── Quick start card ──────────────────────────────────────────────
        qs_label = QLabel("QUICK START")
        qs_label.setStyleSheet(f"color: #2A5A8A; font-size: 11px; font-weight: 600; letter-spacing: 1.5px;")
        layout.addWidget(qs_label)

        qs_row = QHBoxLayout()
        qs_row.setSpacing(16)

        actions = [
            ("Start New Scan", "Connect to OBD simulator or load real data", COLOR_ACCENT_BLUE, 1),
            ("View Last Results", "See ranked root causes and sensor analysis", "#0D47A1", 2),
            ("AI Diagnosis", "Get Gemini AI explanation and repair narrative", "#1565C0", 3),
        ]

        for title_text, desc, color, nav_idx in actions:
            card = QFrame()
            card.setObjectName("stat_card")
            card.setMinimumHeight(130)
            card.setStyleSheet(f"""
                QFrame#stat_card {{
                    background-color: {COLOR_BG_CARD};
                    border: 1px solid {COLOR_BORDER};
                    border-left: 3px solid {color};
                    border-radius: 10px;
                }}
                QFrame#stat_card:hover {{
                    background-color: #142540;
                    border-left: 3px solid {COLOR_CYAN};
                }}
            """)
            cl = QVBoxLayout(card)
            cl.setContentsMargins(20, 16, 20, 16)
            t = QLabel(title_text)
            t.setStyleSheet(f"font-size: 14px; font-weight: 700; color: {COLOR_TEXT_PRIMARY};")
            d = QLabel(desc)
            d.setStyleSheet(f"font-size: 11px; color: {COLOR_TEXT_SECONDARY}; margin-top: 4px;")
            d.setWordWrap(True)
            btn = QPushButton("Open →")
            btn.setObjectName("secondary_btn")
            btn.setFixedHeight(28)
            nav_i = nav_idx  # capture
            btn.clicked.connect(lambda checked, i=nav_i: self.navigate_requested.emit(i))
            cl.addWidget(t)
            cl.addWidget(d)
            cl.addStretch()
            cl.addWidget(btn, 0, Qt.AlignLeft)
            qs_row.addWidget(card)

        layout.addLayout(qs_row)

        # ── How it works ──────────────────────────────────────────────────
        how_label = QLabel("HOW IGNITIONAI WORKS")
        how_label.setStyleSheet(f"color: #2A5A8A; font-size: 11px; font-weight: 600; letter-spacing: 1.5px;")
        layout.addWidget(how_label)

        steps_card = QFrame()
        steps_card.setObjectName("card")
        steps_layout = QHBoxLayout(steps_card)
        steps_layout.setContentsMargins(24, 20, 24, 20)
        steps_layout.setSpacing(0)

        steps = [
            ("1", "OBD Scan", "Read DTCs & PIDs from vehicle or simulator"),
            ("2", "ML Analysis", "Rule engine + anomaly detection pipeline"),
            ("3", "AI Reasoning", "Gemini AI generates plain-English diagnosis"),
            ("4", "Repair Report", "Mechanic-ready steps + exportable PDF"),
        ]

        for i, (num, title_s, desc_s) in enumerate(steps):
            step_col = QVBoxLayout()
            step_col.setAlignment(Qt.AlignCenter)

            num_lbl = QLabel(num)
            num_lbl.setStyleSheet(f"""
                background-color: {COLOR_ACCENT_BLUE};
                color: white;
                font-size: 18px;
                font-weight: 800;
                border-radius: 22px;
                min-width: 44px;
                max-width: 44px;
                min-height: 44px;
                max-height: 44px;
                padding: 0;
            """)
            num_lbl.setAlignment(Qt.AlignCenter)

            t_lbl = QLabel(title_s)
            t_lbl.setStyleSheet(f"font-size: 13px; font-weight: 700; color: {COLOR_TEXT_PRIMARY}; margin-top: 8px;")
            t_lbl.setAlignment(Qt.AlignCenter)

            d_lbl = QLabel(desc_s)
            d_lbl.setStyleSheet(f"font-size: 11px; color: {COLOR_TEXT_SECONDARY};")
            d_lbl.setAlignment(Qt.AlignCenter)
            d_lbl.setWordWrap(True)
            d_lbl.setMaximumWidth(160)

            step_col.addWidget(num_lbl, 0, Qt.AlignCenter)
            step_col.addWidget(t_lbl)
            step_col.addWidget(d_lbl)
            steps_layout.addLayout(step_col)

            if i < len(steps) - 1:
                arrow = QLabel("→")
                arrow.setStyleSheet(f"font-size: 20px; color: {COLOR_BORDER}; padding: 0 12px; margin-top: -20px;")
                arrow.setAlignment(Qt.AlignCenter)
                steps_layout.addWidget(arrow)

        layout.addWidget(steps_card)
        layout.addStretch()

        scroll.setWidget(container)
        outer = QVBoxLayout(self)
        outer.setContentsMargins(0, 0, 0, 0)
        outer.addWidget(scroll)

    def update_stats(self, results: dict):
        """Update stat cards after analysis."""
        dtc_count = len(results.get("dtc_rankings", []))
        root_count = len(results.get("root_rankA", []))
        confidence = results.get("final_decision", {}).get("confidence", 0)
        pw = 0
        if results.get("dtc_rankings"):
            pw = results["dtc_rankings"][0].get("points_weight", 0)
        sev = "Critical" if pw >= 9 else "High" if pw >= 7 else "Medium" if pw >= 5 else "Low"

        # Determine semantic colors from fixed palette based on values
        dtc_color = COLOR_DANGER if dtc_count > 0 else COLOR_CYAN
        root_color = COLOR_WARNING if root_count > 0 else COLOR_CYAN
        conf_color = COLOR_SUCCESS if confidence >= 0.75 else COLOR_WARNING if confidence >= 0.5 else COLOR_DANGER
        sev_color = COLOR_DANGER if sev == "Critical" else COLOR_WARNING if sev in ("High", "Medium") else COLOR_SUCCESS

        updates = [
            (self.stat_dtcs, str(dtc_count), dtc_color),
            (self.stat_root, str(root_count), root_color),
            (self.stat_conf, f"{confidence*100:.0f}%", conf_color),
            (self.stat_sev, sev, sev_color),
        ]

        for card, val, color in updates:
            lbl = card.layout().itemAt(0).widget()
            if isinstance(lbl, QLabel):
                lbl.setText(val)
                lbl.setStyleSheet(f"font-size: 32px; font-weight: 800; color: {color};")
