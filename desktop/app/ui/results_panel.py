"""
IgnitionAI — Results Panel
Ranked DTCs, root causes, sensor anomaly chart, and repair steps.
"""

import matplotlib
matplotlib.use("QtAgg")
from matplotlib.backends.backend_qtagg import FigureCanvasQTAgg as FigureCanvas
from matplotlib.figure import Figure
import matplotlib.pyplot as plt
import numpy as np

from PySide6.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QLabel, QFrame,
    QPushButton, QListWidget, QListWidgetItem, QTableWidget,
    QTableWidgetItem, QTextEdit, QSplitter, QScrollArea, QProgressBar
)
from PySide6.QtCore import Qt
from PySide6.QtGui import QColor, QFont

from app.config import (
    COLOR_BG_DARKEST, COLOR_BG_PANEL, COLOR_BG_CARD, COLOR_BORDER, COLOR_CYAN,
    COLOR_TEXT_PRIMARY, COLOR_TEXT_SECONDARY, COLOR_SUCCESS,
    COLOR_WARNING, COLOR_DANGER, COLOR_ACCENT_BLUE, COLOR_LIGHTER_BLUE,
)
from app.ui.styles import get_severity_color, get_confidence_color
from app.data.dtc_database import dtc_root_cause_repairs


def _hex_to_rgb(hex_color: str):
    h = hex_color.lstrip('#')
    return tuple(int(h[i:i+2], 16) / 255 for i in (0, 2, 4))


class AnomalyChart(FigureCanvas):
    def __init__(self, parent=None):
        self.fig = Figure(figsize=(5, 3), dpi=100, tight_layout=True)
        self.fig.patch.set_facecolor('#0A1628')
        self.ax = self.fig.add_subplot(111)
        super().__init__(self.fig)
        self.setParent(parent)
        self._draw_empty()

    def _draw_empty(self):
        self.ax.clear()
        self.ax.set_facecolor('#0D1F3C')
        self.ax.set_title("Sensor Anomaly Scores", color='#8BB8D4', fontsize=10, pad=10)
        self.ax.text(0.5, 0.5, 'Run a scan to see anomaly data', ha='center', va='center',
                     transform=self.ax.transAxes, color='#4A7098', fontsize=10)
        self.ax.tick_params(colors='#8BB8D4', labelsize=8)
        for spine in self.ax.spines.values():
            spine.set_color('#1A3A5C')
        self.draw()

    def plot_anomalies(self, sensor_anomalies: dict):
        self.ax.clear()
        self.ax.set_facecolor('#0D1F3C')

        # Take top 8 sensors by anomaly score
        sorted_items = sorted(sensor_anomalies.items(), key=lambda x: x[1], reverse=True)[:8]
        if not sorted_items:
            self._draw_empty()
            return

        names = [k.replace("_", "\n") for k, _ in sorted_items]
        values = [v for _, v in sorted_items]

        # Color bars by severity
        bar_colors = [
            '#FF5252' if v > 0.7 else '#FFB300' if v > 0.4 else '#00D4FF'
            for v in values
        ]

        bars = self.ax.bar(range(len(values)), values, color=bar_colors, alpha=0.85, width=0.6)

        # Add value labels on bars
        for bar, val in zip(bars, values):
            self.ax.text(
                bar.get_x() + bar.get_width() / 2,
                bar.get_height() + 0.02,
                f'{val:.2f}',
                ha='center', va='bottom', color='#E8F4FD', fontsize=7
            )

        self.ax.set_xticks(range(len(names)))
        self.ax.set_xticklabels(names, rotation=0, ha='center', fontsize=7, color='#8BB8D4')
        self.ax.set_yticks([0.0, 0.25, 0.5, 0.75, 1.0])
        self.ax.set_yticklabels(['0', '0.25', '0.50', '0.75', '1.0'], color='#8BB8D4', fontsize=7)
        self.ax.set_title("Sensor Anomaly Scores", color='#A8D1F5', fontsize=10, pad=10, fontweight='bold')
        self.ax.set_ylim(0, 1.2)
        self.ax.axhline(y=0.7, color='#FF5252', linestyle='--', alpha=0.4, linewidth=0.8)
        self.ax.axhline(y=0.4, color='#FFB300', linestyle='--', alpha=0.4, linewidth=0.8)

        for spine in self.ax.spines.values():
            spine.set_color('#1A3A5C')
        self.ax.tick_params(colors='#8BB8D4')
        self.fig.patch.set_facecolor('#0A1628')
        self.draw()


class ResultsPanel(QWidget):
    def __init__(self, parent=None):
        super().__init__(parent)
        self._current_results = {}
        self._build_ui()

    def _build_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(24, 20, 24, 20)
        layout.setSpacing(16)

        # ── Top: Final Decision Banner ─────────────────────────────────────
        self.banner = QFrame()
        self.banner.setObjectName("card")
        self.banner.setStyleSheet(f"""
            QFrame#card {{
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
                    stop:0 #0A1A30, stop:1 #0D1F3C);
                border: 1px solid {COLOR_BORDER};
                border-left: 4px solid {COLOR_CYAN};
                border-radius: 12px;
                padding: 4px;
            }}
        """)
        banner_layout = QHBoxLayout(self.banner)
        banner_layout.setContentsMargins(20, 14, 20, 14)

        banner_left = QVBoxLayout()
        self.banner_title = QLabel("🔍 Run a scan to see results")
        self.banner_title.setStyleSheet(f"font-size: 16px; font-weight: 700; color: {COLOR_TEXT_PRIMARY};")
        self.banner_subtitle = QLabel("No analysis data available")
        self.banner_subtitle.setStyleSheet(f"font-size: 12px; color: {COLOR_TEXT_SECONDARY};")
        banner_left.addWidget(self.banner_title)
        banner_left.addWidget(self.banner_subtitle)

        banner_right = QVBoxLayout()
        banner_right.setAlignment(Qt.AlignRight | Qt.AlignVCenter)
        self.confidence_label = QLabel("")
        self.confidence_label.setStyleSheet(f"font-size: 22px; font-weight: 800; color: {COLOR_SUCCESS};")
        self.method_label = QLabel("")
        self.method_label.setStyleSheet(f"font-size: 11px; color: {COLOR_TEXT_SECONDARY};")
        banner_right.addWidget(self.confidence_label, 0, Qt.AlignRight)
        banner_right.addWidget(self.method_label, 0, Qt.AlignRight)

        banner_layout.addLayout(banner_left, 3)
        banner_layout.addLayout(banner_right, 1)
        layout.addWidget(self.banner)

        # ── Middle: 3-column layout ────────────────────────────────────────
        mid_splitter = QSplitter(Qt.Horizontal)

        # Left: DTC Rankings
        dtc_frame = QFrame()
        dtc_frame.setObjectName("card")
        dtc_layout = QVBoxLayout(dtc_frame)
        dtc_layout.setContentsMargins(16, 14, 16, 14)
        dtc_layout.setSpacing(10)

        dtc_hdr = QLabel("RANKED FAULT CODES")
        dtc_hdr.setObjectName("section_header")
        dtc_layout.addWidget(dtc_hdr)

        self.dtc_table = QTableWidget(0, 4)
        self.dtc_table.setHorizontalHeaderLabels(["Code", "Score", "Weight", "Confidence"])
        self.dtc_table.horizontalHeader().setStretchLastSection(True)
        self.dtc_table.setSelectionBehavior(QTableWidget.SelectRows)
        self.dtc_table.verticalHeader().setVisible(False)
        dtc_layout.addWidget(self.dtc_table)
        mid_splitter.addWidget(dtc_frame)

        # Center: Root Causes + Chart
        center_frame = QFrame()
        center_frame.setObjectName("card")
        center_layout = QVBoxLayout(center_frame)
        center_layout.setContentsMargins(16, 14, 16, 14)
        center_layout.setSpacing(10)

        rc_hdr = QLabel("ROOT CAUSE RANKING")
        rc_hdr.setObjectName("section_header")
        center_layout.addWidget(rc_hdr)

        self.roots_list = QListWidget()
        self.roots_list.setMinimumHeight(120)
        self.roots_list.itemClicked.connect(self._show_repair)
        center_layout.addWidget(self.roots_list)

        chart_hdr = QLabel("SENSOR ANOMALY MAP")
        chart_hdr.setObjectName("section_header")
        center_layout.addWidget(chart_hdr)

        self.anom_chart = AnomalyChart()
        center_layout.addWidget(self.anom_chart)
        mid_splitter.addWidget(center_frame)

        # Right: Repair steps
        repair_frame = QFrame()
        repair_frame.setObjectName("card")
        repair_layout = QVBoxLayout(repair_frame)
        repair_layout.setContentsMargins(16, 14, 16, 14)
        repair_layout.setSpacing(10)

        repair_hdr = QHBoxLayout()
        repair_title = QLabel("REPAIR GUIDANCE")
        repair_title.setObjectName("section_header")
        repair_hdr.addWidget(repair_title)
        repair_hdr.addStretch()

        self.ai_btn = QPushButton("🤖 Get AI Explanation")
        self.ai_btn.setObjectName("secondary_btn")
        self.ai_btn.setFixedHeight(28)
        self.ai_btn.clicked.connect(self._go_to_ai)
        repair_hdr.addWidget(self.ai_btn)
        repair_layout.addLayout(repair_hdr)

        self.repair_text = QTextEdit()
        self.repair_text.setReadOnly(True)
        self.repair_text.setPlaceholderText("Click a root cause to see repair steps...")
        repair_layout.addWidget(self.repair_text)

        mid_splitter.addWidget(repair_frame)
        mid_splitter.setSizes([350, 480, 380])
        layout.addWidget(mid_splitter)

    def load_results(self, results: dict):
        self._current_results = results
        final = results.get("final_decision", {})
        final_root = results.get("final_root", "Unknown")
        confidence = final.get("confidence", 0)
        method = final.get("method", "heuristic")

        # Update banner
        conf_color = get_confidence_color(confidence)
        self.banner.setStyleSheet(f"""
            QFrame#card {{
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
                    stop:0 #0A1A30, stop:1 #0D1F3C);
                border: 1px solid {COLOR_BORDER};
                border-left: 4px solid {conf_color};
                border-radius: 12px;
            }}
        """)
        self.banner_title.setText(f"⚡ Root Cause: {final_root}")
        dtc_list = ", ".join(results.get("input_dtcs", []))
        self.banner_subtitle.setText(f"DTCs: {dtc_list}  •  Method: {method}")
        self.confidence_label.setText(f"{confidence*100:.0f}%")
        self.confidence_label.setStyleSheet(f"font-size: 24px; font-weight: 800; color: {conf_color};")
        self.method_label.setText(f"AI Confidence")

        # Populate DTC table
        dtc_rankings = results.get("dtc_rankings", [])
        self.dtc_table.setRowCount(0)
        for row in dtc_rankings:
            r = self.dtc_table.rowCount()
            self.dtc_table.insertRow(r)
            pw = row.get("points_weight", 0)
            sev_color = get_severity_color(pw)

            code_item = QTableWidgetItem(row["dtc"])
            code_item.setForeground(QColor(COLOR_CYAN))
            code_item.setFont(QFont("Consolas", 11, QFont.Bold))
            self.dtc_table.setItem(r, 0, code_item)

            score_item = QTableWidgetItem(f"{row['score']:.2f}")
            score_item.setForeground(QColor(sev_color))
            self.dtc_table.setItem(r, 1, score_item)

            self.dtc_table.setItem(r, 2, QTableWidgetItem(str(pw)))

            plaus_pct = f"{row.get('avg_plaus', 0)*100:.0f}%"
            plaus_item = QTableWidgetItem(plaus_pct)
            plaus_item.setForeground(QColor(get_confidence_color(row.get("avg_plaus", 0))))
            self.dtc_table.setItem(r, 3, plaus_item)

        self.dtc_table.resizeColumnsToContents()

        # Populate root causes
        self.roots_list.clear()
        for i, rc in enumerate(results.get("root_rankA", [])[:8]):
            root = rc.get("root", "")
            score = rc.get("score", 0)
            prefix = "🥇" if i == 0 else "🥈" if i == 1 else "🥉" if i == 2 else f"{i+1}."
            item = QListWidgetItem(f"{prefix}  {root}  ({score:.1f})")
            item.setData(Qt.UserRole, rc)
            if i == 0:
                item.setForeground(QColor(COLOR_CYAN))
                font = QFont()
                font.setBold(True)
                item.setFont(font)
            self.roots_list.addItem(item)

        # Plot anomaly chart
        self.anom_chart.plot_anomalies(results.get("sensor_anomalies", {}))

        # Auto-show repair for top root cause
        if results.get("root_rankA"):
            self._show_repair_for(results["root_rankA"][0])

    def _show_repair(self, item):
        data = item.data(Qt.UserRole)
        self._show_repair_for(data)

    def _show_repair_for(self, rc: dict):
        root = rc.get("root", "")
        dtcs = self._current_results.get("input_dtcs", [])

        # Find repair steps
        steps = []
        for dtc in dtcs:
            for entry in dtc_root_cause_repairs.get(dtc, []):
                if entry.get("root_cause") == root:
                    steps = entry.get("repair_steps", [])
                    break
            if steps:
                break

        # Also search all DTCs in db
        if not steps:
            for dtc, entries in dtc_root_cause_repairs.items():
                for entry in entries:
                    if entry.get("root_cause") == root:
                        steps = entry.get("repair_steps", [])
                        break
                if steps:
                    break

        if steps:
            category = ""
            for dtc, entries in dtc_root_cause_repairs.items():
                for e in entries:
                    if e.get("root_cause") == root:
                        category = e.get("root_cause_category", "")
                        break

            txt = f"📍 Root Cause: {root}\n"
            if category:
                txt += f"📂 Category: {category}\n"
            txt += f"\n{'─'*40}\n\nRepair Procedure:\n\n"
            for i, step in enumerate(steps, 1):
                txt += f"Step {i}: {step}\n\n"
        else:
            txt = f"Root Cause: {root}\n\nNo specific repair steps found in the database for this root cause.\nConsult a certified technician."

        self.repair_text.setPlainText(txt)

    def _go_to_ai(self):
        main = self.parent()
        if hasattr(main, "navigate_to_ai"):
            main.navigate_to_ai()
        elif hasattr(main, "parent") and hasattr(main.parent(), "navigate_to_ai"):
            main.parent().navigate_to_ai()
