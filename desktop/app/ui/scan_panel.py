"""
IgnitionAI — Scan Panel
DTC input, sensor values, OBD simulator toggle, and analysis trigger.
"""

import json
import os
from PySide6.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QLabel, QFrame,
    QPushButton, QLineEdit, QTextEdit, QComboBox, QFileDialog,
    QMessageBox, QScrollArea, QProgressBar, QSizePolicy
)
from PySide6.QtCore import Qt, Signal, QThread, QObject
from PySide6.QtGui import QFont

from app.config import (
    COLOR_BG_PANEL, COLOR_BG_CARD, COLOR_BORDER, COLOR_CYAN,
    COLOR_TEXT_PRIMARY, COLOR_TEXT_SECONDARY, COLOR_SUCCESS,
    COLOR_WARNING, COLOR_DANGER, COLOR_ACCENT_BLUE
)
from app.data.simulator import get_scenario_names, simulate_scan
from app.engine.diagnostic import run_full_analysis


# ─── Background Analysis Worker ───────────────────────────────────────────────

class AnalysisWorker(QObject):
    finished = Signal(dict)
    error = Signal(str)

    def __init__(self, dtcs, sensor_values):
        super().__init__()
        self.dtcs = dtcs
        self.sensor_values = sensor_values

    def run(self):
        try:
            results = run_full_analysis(self.dtcs, self.sensor_values)
            results["input_dtcs"] = self.dtcs
            results["input_sensors"] = self.sensor_values
            self.finished.emit(results)
        except Exception as e:
            self.error.emit(str(e))


class AnalysisThread(QThread):
    def __init__(self, worker):
        super().__init__()
        self.worker = worker
        self.worker.moveToThread(self)
        self.started.connect(self.worker.run)


# ─── Scan Panel ───────────────────────────────────────────────────────────────

class ScanPanel(QWidget):
    analysis_complete = Signal(dict)

    def __init__(self, parent=None):
        super().__init__(parent)
        self._build_ui()
        self._sim_active = False

    def _build_ui(self):
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setFrameShape(QFrame.NoFrame)

        container = QWidget()
        layout = QVBoxLayout(container)
        layout.setContentsMargins(32, 28, 32, 28)
        layout.setSpacing(20)

        # ── Mode selector ─────────────────────────────────────────────────
        mode_card = QFrame()
        mode_card.setObjectName("card")
        mode_card.setStyleSheet(f"""
            QFrame#card {{
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
                    stop:0 #0A1A30, stop:1 #0D1F3C);
                border: 1px solid {COLOR_BORDER};
                border-radius: 14px;
            }}
        """)
        mode_layout = QHBoxLayout(mode_card)
        mode_layout.setContentsMargins(24, 20, 24, 20)
        mode_layout.setSpacing(20)

        # Real data mode
        self.real_btn = QPushButton("📡  Real OBD Data")
        self.real_btn.setCheckable(True)
        self.real_btn.setChecked(True)
        self.real_btn.setObjectName("primary_btn")
        self.real_btn.clicked.connect(self._set_real_mode)

        # Simulator mode
        self.sim_btn = QPushButton("🎮  OBD Simulator")
        self.sim_btn.setCheckable(True)
        self.sim_btn.setObjectName("secondary_btn")
        self.sim_btn.clicked.connect(self._set_sim_mode)

        mode_label = QLabel("Data Source:")
        mode_label.setStyleSheet(f"font-size: 14px; font-weight: 700; color: {COLOR_TEXT_PRIMARY};")

        mode_layout.addWidget(mode_label)
        mode_layout.addWidget(self.real_btn)
        mode_layout.addWidget(self.sim_btn)
        mode_layout.addStretch()

        # Simulator scenario picker
        self.scenario_label = QLabel("Scenario:")
        self.scenario_label.setStyleSheet(f"color: {COLOR_TEXT_SECONDARY}; font-size: 12px;")
        self.scenario_label.hide()
        self.scenario_combo = QComboBox()
        for name in get_scenario_names():
            self.scenario_combo.addItem(name)
        self.scenario_combo.hide()

        mode_layout.addWidget(self.scenario_label)
        mode_layout.addWidget(self.scenario_combo)
        layout.addWidget(mode_card)

        # ── Input area ────────────────────────────────────────────────────
        input_row = QHBoxLayout()
        input_row.setSpacing(20)

        # Left: DTC input
        dtc_card = QFrame()
        dtc_card.setObjectName("card")
        dtc_layout = QVBoxLayout(dtc_card)
        dtc_layout.setContentsMargins(20, 16, 20, 16)
        dtc_layout.setSpacing(10)

        dtc_header = QHBoxLayout()
        dtc_title = QLabel("Fault Codes (DTCs)")
        dtc_title.setStyleSheet(f"font-size: 14px; font-weight: 700; color: {COLOR_TEXT_PRIMARY};")
        dtc_header.addWidget(dtc_title)
        dtc_header.addStretch()

        self.dtc_count_label = QLabel("0 codes")
        self.dtc_count_label.setStyleSheet(f"color: {COLOR_CYAN}; font-size: 11px; font-weight: 600;")
        dtc_header.addWidget(self.dtc_count_label)
        dtc_layout.addLayout(dtc_header)

        self.dtc_input = QLineEdit()
        self.dtc_input.setPlaceholderText("e.g. P0300, P0230, P0171")
        self.dtc_input.setMinimumHeight(44)
        self.dtc_input.textChanged.connect(self._update_dtc_count)
        dtc_layout.addWidget(self.dtc_input)

        dtc_hint = QLabel("Comma-separated OBD-II fault codes from your scan tool")
        dtc_hint.setStyleSheet(f"color: {COLOR_TEXT_SECONDARY}; font-size: 11px;")
        dtc_layout.addWidget(dtc_hint)

        # Common DTC quick-add buttons
        quick_label = QLabel("Quick add:")
        quick_label.setStyleSheet(f"color: {COLOR_TEXT_SECONDARY}; font-size: 11px; margin-top:4px;")
        dtc_layout.addWidget(quick_label)

        quick_row = QHBoxLayout()
        quick_row.setSpacing(6)
        for code in ["P0300", "P0087", "P0171", "P0335", "P0420"]:
            qb = QPushButton(code)
            qb.setObjectName("secondary_btn")
            qb.setFixedHeight(26)
            qb.setFixedWidth(68)
            qb.setStyleSheet(f"font-size:11px; padding: 2px 6px;")
            qb.clicked.connect(lambda c, code=code: self._quick_add_dtc(code))
            quick_row.addWidget(qb)
        quick_row.addStretch()
        dtc_layout.addLayout(quick_row)
        input_row.addWidget(dtc_card, 1)

        # Right: Sensor input
        sensor_card = QFrame()
        sensor_card.setObjectName("card")
        sensor_layout = QVBoxLayout(sensor_card)
        sensor_layout.setContentsMargins(20, 16, 20, 16)
        sensor_layout.setSpacing(10)

        sensor_header = QHBoxLayout()
        sensor_title = QLabel("Sensor Values")
        sensor_title.setStyleSheet(f"font-size: 14px; font-weight: 700; color: {COLOR_TEXT_PRIMARY};")
        sensor_header.addWidget(sensor_title)
        sensor_header.addStretch()

        load_json_btn = QPushButton("📂 Load JSON")
        load_json_btn.setObjectName("secondary_btn")
        load_json_btn.setFixedHeight(28)
        load_json_btn.clicked.connect(self._load_json)
        sensor_header.addWidget(load_json_btn)
        sensor_layout.addLayout(sensor_header)

        self.sensor_text = QTextEdit()
        self.sensor_text.setPlaceholderText(
            "Enter sensor readings, one per line:\n\nengine_rpm:750\nfuel_rail_pressure:5\ncrankshaft_position:10\nignition_timing:-5\n\nOr use the JSON loader for bulk import."
        )
        self.sensor_text.setMinimumHeight(160)
        sensor_layout.addWidget(self.sensor_text)

        sensor_hint = QLabel("Format: sensor_name:value  (one per line) or load a JSON file")
        sensor_hint.setStyleSheet(f"color: {COLOR_TEXT_SECONDARY}; font-size: 11px;")
        sensor_layout.addWidget(sensor_hint)
        input_row.addWidget(sensor_card, 1)
        layout.addLayout(input_row)

        # ── Action buttons ────────────────────────────────────────────────
        action_row = QHBoxLayout()
        action_row.setSpacing(16)

        self.example_btn = QPushButton("🔧 Load Example")
        self.example_btn.setObjectName("secondary_btn")
        self.example_btn.clicked.connect(self._load_example)
        action_row.addWidget(self.example_btn)

        clear_btn = QPushButton("🗑 Clear")
        clear_btn.setObjectName("secondary_btn")
        clear_btn.clicked.connect(self._clear_inputs)
        action_row.addWidget(clear_btn)

        action_row.addStretch()

        self.run_btn = QPushButton("⚡  Run AI Diagnostic Analysis")
        self.run_btn.setObjectName("primary_btn")
        self.run_btn.setMinimumWidth(280)
        self.run_btn.setMinimumHeight(48)
        self.run_btn.setStyleSheet(f"""
            QPushButton {{
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
                    stop:0 {COLOR_ACCENT_BLUE}, stop:1 #0D47A1);
                color: white;
                border-radius: 12px;
                font-size: 15px;
                font-weight: 700;
                padding: 12px 32px;
            }}
            QPushButton:hover {{
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
                    stop:0 #1976D2, stop:1 {COLOR_ACCENT_BLUE});
            }}
            QPushButton:disabled {{
                background: #1A2D5A;
                color: #2A4A6A;
            }}
        """)
        self.run_btn.clicked.connect(self._run_analysis)
        action_row.addWidget(self.run_btn)
        layout.addLayout(action_row)

        # ── Progress bar ──────────────────────────────────────────────────
        self.progress_bar = QProgressBar()
        self.progress_bar.setRange(0, 0)  # Indeterminate
        self.progress_bar.hide()
        layout.addWidget(self.progress_bar)

        self.status_label = QLabel("")
        self.status_label.setStyleSheet(f"color: {COLOR_CYAN}; font-size: 12px; font-style: italic;")
        self.status_label.hide()
        layout.addWidget(self.status_label)

        layout.addStretch()
        scroll.setWidget(container)
        outer = QVBoxLayout(self)
        outer.setContentsMargins(0, 0, 0, 0)
        outer.addWidget(scroll)

    def _set_real_mode(self):
        self._sim_active = False
        self.real_btn.setObjectName("primary_btn")
        self.sim_btn.setObjectName("secondary_btn")
        self.style().polish(self.real_btn)
        self.style().polish(self.sim_btn)
        self.scenario_label.hide()
        self.scenario_combo.hide()
        self.dtc_input.setReadOnly(False)
        self.sensor_text.setReadOnly(False)

    def _set_sim_mode(self):
        self._sim_active = True
        self.real_btn.setObjectName("secondary_btn")
        self.sim_btn.setObjectName("sim_active")
        self.style().polish(self.real_btn)
        self.style().polish(self.sim_btn)
        self.scenario_label.show()
        self.scenario_combo.show()
        # Pre-fill with simulator data
        self._load_sim_scenario()

    def _load_sim_scenario(self):
        scenario_name = self.scenario_combo.currentText()
        dtcs, sensors, description = simulate_scan(scenario_name)
        self.dtc_input.setText(", ".join(dtcs))
        self.sensor_text.setPlainText("\n".join(f"{k}:{v}" for k, v in sensors.items()))
        self.dtc_input.setReadOnly(True)
        self.sensor_text.setReadOnly(True)
        self._update_dtc_count()

    def _quick_add_dtc(self, code: str):
        current = self.dtc_input.text().strip()
        codes = [c.strip() for c in current.split(",") if c.strip()]
        if code not in codes:
            codes.append(code)
        self.dtc_input.setText(", ".join(codes))

    def _update_dtc_count(self):
        codes = [c.strip() for c in self.dtc_input.text().split(",") if c.strip()]
        count = len(codes)
        self.dtc_count_label.setText(f"{count} code{'s' if count != 1 else ''}")

    def _load_example(self):
        if self._sim_active:
            self._load_sim_scenario()
            return
        self.dtc_input.setText("P0300, P0230")
        self.sensor_text.setPlainText(
            "engine_rpm:750\nfuel_rail_pressure:5\ncrankshaft_position:10\n"
            "ignition_timing:-5\nfuel_pump_voltage:0\nmaf_sensor:2.5\n"
            "o2_sensor_b1s1:0.15\nshort_term_fuel_trim_b1:18"
        )
        self._update_dtc_count()

    def _load_json(self):
        path, _ = QFileDialog.getOpenFileName(self, "Load OBD JSON", "", "JSON Files (*.json)")
        if not path:
            return
        try:
            with open(path, "r") as f:
                data = json.load(f)
            dtcs = data.get("dtcs", [])
            sensors = data.get("sensors", {})
            if not isinstance(dtcs, list) or not isinstance(sensors, dict):
                raise ValueError("Expected 'dtcs' as list and 'sensors' as dict.")
            self.dtc_input.setText(", ".join(dtcs))
            self.sensor_text.setPlainText("\n".join(f"{k}:{v}" for k, v in sensors.items()))
            self._update_dtc_count()
        except Exception as e:
            QMessageBox.critical(self, "Load Error", f"Failed to load JSON: {e}")

    def _clear_inputs(self):
        self.dtc_input.clear()
        self.sensor_text.clear()
        self._update_dtc_count()

    def _parse_sensors(self) -> dict:
        out = {}
        for line in self.sensor_text.toPlainText().strip().splitlines():
            line = line.strip()
            if not line or ":" not in line:
                continue
            k, _, v = line.partition(":")
            try:
                out[k.strip()] = float(v.strip())
            except ValueError:
                out[k.strip()] = v.strip()
        return out

    def _run_analysis(self):
        # Reload sim data if active
        if self._sim_active:
            self._load_sim_scenario()

        dtcs_raw = [d.strip().upper() for d in self.dtc_input.text().split(",") if d.strip()]
        sensors = self._parse_sensors()

        if not dtcs_raw:
            QMessageBox.warning(self, "Input Required", "Please enter at least one DTC code.")
            return

        self.run_btn.setEnabled(False)
        self.run_btn.setText("⏳  Analyzing...")
        self.progress_bar.show()
        self.status_label.setText("Running diagnostic pipeline... please wait")
        self.status_label.show()

        self._worker = AnalysisWorker(dtcs_raw, sensors)
        self._thread = AnalysisThread(self._worker)
        self._worker.finished.connect(self._on_done)
        self._worker.error.connect(self._on_error)
        self._thread.start()

    def _on_done(self, results: dict):
        self.progress_bar.hide()
        self.status_label.hide()
        self.run_btn.setEnabled(True)
        self.run_btn.setText("⚡  Run AI Diagnostic Analysis")
        self._thread.quit()
        self.analysis_complete.emit(results)

    def _on_error(self, msg: str):
        self.progress_bar.hide()
        self.status_label.hide()
        self.run_btn.setEnabled(True)
        self.run_btn.setText("⚡  Run AI Diagnostic Analysis")
        QMessageBox.critical(self, "Analysis Error", f"An error occurred:\n{msg}")
