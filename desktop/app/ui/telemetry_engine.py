"""Native evidence dashboard for IgnitionAI's real telemetry engine."""
from __future__ import annotations

import sys
from pathlib import Path

from PySide6.QtCore import QObject, QThread, Qt, Signal
from PySide6.QtWidgets import (
    QAbstractItemView, QComboBox, QDialog, QDialogButtonBox, QDoubleSpinBox,
    QFileDialog, QFormLayout, QFrame, QGridLayout, QHBoxLayout, QHeaderView,
    QLabel, QLineEdit, QMessageBox, QPushButton, QSpinBox, QSplitter, QTabBar,
    QTabWidget, QTableWidget, QTableWidgetItem, QTextEdit, QVBoxLayout, QWidget,
)

ROOT = Path(__file__).resolve().parents[3]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from serving.diagnostics.explanation import explain_structured_diagnosis
from serving.diagnostics.pipeline import analyze_file
from serving.diagnostics.persistence import persist_analysis
from serving.diagnostics.synthetic import SCENARIOS, generate_synthetic_telemetry
from app.ui.telemetry_report import generate_telemetry_pdf_report


class AnalysisWorker(QObject):
    finished = Signal(dict)
    failed = Signal(str)

    def __init__(self, source_path: str, generate_report: bool) -> None:
        super().__init__()
        self.source_path = source_path
        self.generate_report = generate_report

    def run(self) -> None:
        try:
            result = analyze_file(self.source_path)
            if self.generate_report:
                result["mechanic_report"] = explain_structured_diagnosis({
                    "vehicle_health_index": result["fusion"]["vehicle_health_index"],
                    "fault_candidates": result["fusion"]["fault_candidates"],
                })
            else:
                result["database_session_id"] = persist_analysis(result)
            self.finished.emit(result)
        except Exception as exc:
            self.failed.emit(str(exc))


class NoWheelTabBar(QTabBar):
    """Tabs change only by an intentional click, never by mouse-wheel drift."""

    def wheelEvent(self, event) -> None:
        event.accept()


class SyntheticGeneratorDialog(QDialog):
    def __init__(self, parent: QWidget | None = None) -> None:
        super().__init__(parent)
        self.setWindowTitle("Synthetic telemetry generator")
        self.setModal(True)
        self.setMinimumWidth(450)
        box = QVBoxLayout(self)
        title = QLabel("Physics-based validation trace")
        title.setObjectName("dialogTitle")
        note = QLabel("Generated traces are explicitly marked SYNTHETIC and run through the same diagnostic pipeline as real recordings.")
        note.setWordWrap(True)
        note.setObjectName("tabIntro")
        box.addWidget(title)
        box.addWidget(note)
        form = QFormLayout()
        form.setVerticalSpacing(12)
        self.scenario = QComboBox()
        for key, label in SCENARIOS.items():
            self.scenario.addItem(label, key)
        self.duration = QSpinBox()
        self.duration.setRange(1, 30)
        self.duration.setValue(10)
        self.duration.setSuffix(" min")
        self.rate = QDoubleSpinBox()
        self.rate.setRange(1.0, 20.0)
        self.rate.setValue(5.0)
        self.rate.setSuffix(" Hz")
        self.seed = QSpinBox()
        self.seed.setRange(0, 999999)
        self.seed.setValue(42)
        form.addRow("Scenario", self.scenario)
        form.addRow("Duration", self.duration)
        form.addRow("Sample rate", self.rate)
        form.addRow("Random seed", self.seed)
        box.addLayout(form)
        buttons = QDialogButtonBox(QDialogButtonBox.Cancel | QDialogButtonBox.Ok)
        buttons.button(QDialogButtonBox.Ok).setText("Generate and analyze")
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)
        box.addWidget(buttons)


class MetricCard(QFrame):
    def __init__(self, label: str, value: str, note: str = "") -> None:
        super().__init__()
        self.setObjectName("metricCard")
        box = QVBoxLayout(self)
        box.setContentsMargins(16, 11, 16, 11)
        box.setSpacing(2)
        caption = QLabel(label.upper())
        caption.setObjectName("metricLabel")
        self.value = QLabel(value)
        self.value.setObjectName("metricValue")
        self.note = QLabel(note)
        self.note.setObjectName("metricNote")
        box.addWidget(caption)
        box.addWidget(self.value)
        box.addWidget(self.note)


class TelemetryEnginePanel(QWidget):
    analysis_ready = Signal(dict)

    def __init__(self, parent: QWidget | None = None) -> None:
        super().__init__(parent)
        self._thread: QThread | None = None
        self._worker: AnalysisWorker | None = None
        self._last_result: dict | None = None
        self._build_ui()

    def _build_ui(self) -> None:
        root = QVBoxLayout(self)
        root.setContentsMargins(30, 18, 30, 20)
        root.setSpacing(12)

        heading = QHBoxLayout()
        title_box = QVBoxLayout()
        title = QLabel("Vehicle intelligence workspace")
        title.setObjectName("pageTitle")
        subtitle = QLabel("Physics-informed diagnostics from recorded OBD-II telemetry")
        subtitle.setObjectName("pageSubtitle")
        title_box.addWidget(title)
        title_box.addWidget(subtitle)
        heading.addLayout(title_box)
        heading.addStretch()
        badge = QLabel("EVIDENCE ENGINE  /  LOCAL COMPUTE")
        badge.setObjectName("engineBadge")
        heading.addWidget(badge)
        root.addLayout(heading)

        source_card = QFrame()
        source_card.setObjectName("card")
        source_layout = QVBoxLayout(source_card)
        source_layout.setContentsMargins(18, 15, 18, 13)
        source_layout.setSpacing(10)
        source_title = QLabel("DATASET")
        source_title.setObjectName("eyebrow")
        source_layout.addWidget(source_title)
        source_row = QHBoxLayout()
        source_row.setSpacing(10)
        self.path_input = QLineEdit()
        self.path_input.setReadOnly(True)
        self.path_input.setPlaceholderText("Select a CSV, JSON, or JSONL telemetry recording")
        self.analyze_button = QPushButton("Open telemetry")
        self.analyze_button.setObjectName("primaryButton")
        self.analyze_button.clicked.connect(self._choose_and_analyze)
        self.synthetic_button = QPushButton("Synthetic generator")
        self.synthetic_button.setObjectName("secondaryButton")
        self.synthetic_button.clicked.connect(self._generate_synthetic)
        self.report_button = QPushButton("Generate mechanic note")
        self.report_button.setObjectName("secondaryButton")
        self.report_button.setEnabled(False)
        self.report_button.clicked.connect(self._start_report)
        self.export_button = QPushButton("Export PDF")
        self.export_button.setObjectName("secondaryButton")
        self.export_button.setEnabled(False)
        self.export_button.clicked.connect(self._export_pdf)
        source_row.addWidget(self.path_input, 1)
        source_row.addWidget(self.analyze_button)
        source_row.addWidget(self.synthetic_button)
        source_row.addWidget(self.report_button)
        source_row.addWidget(self.export_button)
        source_layout.addLayout(source_row)
        self.status = QLabel("Ready - missing PIDs remain explicitly unavailable; values are never fabricated.")
        self.status.setObjectName("statusText")
        source_layout.addWidget(self.status)
        root.addWidget(source_card)

        metrics = QGridLayout()
        metrics.setHorizontalSpacing(12)
        self.vhi_card = MetricCard("Vehicle health index", "-", "Awaiting analysis")
        self.records_card = MetricCard("Samples analyzed", "-", "Real source rows")
        self.coverage_card = MetricCard("Virtual sensors", "-", "Available indices")
        self.findings_card = MetricCard("Findings", "-", "Evidence thresholds")
        for index, card in enumerate((self.vhi_card, self.records_card, self.coverage_card, self.findings_card)):
            metrics.addWidget(card, 0, index)
        root.addLayout(metrics)

        self.tabs = QTabWidget()
        self.tabs.setTabBar(NoWheelTabBar(self.tabs))
        self.tabs.setDocumentMode(True)
        self.tabs.setMinimumHeight(260)
        self.tabs.addTab(self._build_evidence_tab(), "Evidence")
        self.tabs.addTab(self._build_repair_tab(), "Repair plan")
        self.tabs.addTab(self._build_note_tab(), "Mechanic note")
        root.addWidget(self.tabs, 1)

    def _table(self, columns: list[str], height: int) -> QTableWidget:
        table = QTableWidget(0, len(columns))
        table.setHorizontalHeaderLabels(columns)
        table.setMinimumHeight(height)
        table.setAlternatingRowColors(True)
        table.setWordWrap(True)
        table.setHorizontalScrollBarPolicy(Qt.ScrollBarAlwaysOff)
        table.setSelectionMode(QAbstractItemView.NoSelection)
        table.setEditTriggers(QAbstractItemView.NoEditTriggers)
        table.verticalHeader().setVisible(False)
        table.horizontalHeader().setStretchLastSection(True)
        table.verticalHeader().setDefaultSectionSize(42)
        return table

    def _build_evidence_tab(self) -> QWidget:
        page = QWidget()
        box = QVBoxLayout(page)
        box.setContentsMargins(14, 14, 14, 14)
        box.setSpacing(0)
        splitter = QSplitter(Qt.Vertical)
        splitter.setChildrenCollapsible(False)
        sensor_section = QWidget()
        sensor_box = QVBoxLayout(sensor_section)
        sensor_box.setContentsMargins(0, 0, 0, 5)
        sensor_box.setSpacing(7)
        label = QLabel("DERIVED VIRTUAL SENSORS")
        label.setObjectName("eyebrow")
        sensor_box.addWidget(label)
        self.sensor_table = self._table(["Virtual sensor", "Health", "State", "Candidate", "Measured evidence"], 120)
        self.sensor_table.horizontalHeader().setSectionResizeMode(0, QHeaderView.ResizeToContents)
        self.sensor_table.horizontalHeader().setSectionResizeMode(1, QHeaderView.ResizeToContents)
        self.sensor_table.horizontalHeader().setSectionResizeMode(2, QHeaderView.ResizeToContents)
        self.sensor_table.horizontalHeader().setSectionResizeMode(3, QHeaderView.Interactive)
        self.sensor_table.setColumnWidth(3, 260)
        sensor_box.addWidget(self.sensor_table)
        fault_section = QWidget()
        fault_box = QVBoxLayout(fault_section)
        fault_box.setContentsMargins(0, 5, 0, 0)
        fault_box.setSpacing(7)
        fault_label = QLabel("RANKED FAULT CANDIDATES")
        fault_label.setObjectName("eyebrow")
        fault_box.addWidget(fault_label)
        self.fault_table = self._table(["Candidate", "Confidence", "Supporting evidence"], 90)
        self.fault_table.horizontalHeader().setSectionResizeMode(0, QHeaderView.ResizeToContents)
        self.fault_table.horizontalHeader().setSectionResizeMode(1, QHeaderView.ResizeToContents)
        fault_box.addWidget(self.fault_table)
        splitter.addWidget(sensor_section)
        splitter.addWidget(fault_section)
        splitter.setSizes([260, 150])
        box.addWidget(splitter)
        return page

    def _build_repair_tab(self) -> QWidget:
        page = QWidget()
        box = QVBoxLayout(page)
        box.setContentsMargins(18, 18, 18, 18)
        box.setSpacing(10)
        intro = QLabel("Evidence-gated workshop sequence. Verify first; replace parts only after a failed test.")
        intro.setObjectName("tabIntro")
        box.addWidget(intro)
        self.repair_table = self._table(["Priority / step", "Stage", "Workshop action", "Test method", "Pass condition"], 180)
        self.repair_table.setWordWrap(True)
        for column in (0, 1, 2):
            self.repair_table.horizontalHeader().setSectionResizeMode(column, QHeaderView.ResizeToContents)
        self.repair_table.horizontalHeader().setSectionResizeMode(3, QHeaderView.Stretch)
        self.repair_table.horizontalHeader().setSectionResizeMode(4, QHeaderView.Stretch)
        box.addWidget(self.repair_table)
        return page

    def _build_note_tab(self) -> QWidget:
        page = QWidget()
        box = QVBoxLayout(page)
        box.setContentsMargins(18, 18, 18, 18)
        box.setSpacing(10)
        intro = QLabel("AI is constrained to explain structured evidence; it does not diagnose from raw telemetry.")
        intro.setObjectName("tabIntro")
        box.addWidget(intro)
        self.report = QTextEdit()
        self.report.setReadOnly(True)
        self.report.setPlaceholderText("Generate a mechanic note after analysis. The deterministic repair plan is available in the adjacent tab.")
        box.addWidget(self.report)
        return page

    def _choose_and_analyze(self) -> None:
        path, _ = QFileDialog.getOpenFileName(self, "Select OBD telemetry", str(Path.home()), "Telemetry (*.csv *.json *.jsonl)")
        if path:
            self.path_input.setText(path)
            self._start_worker(generate_report=False)

    def _generate_synthetic(self) -> None:
        dialog = SyntheticGeneratorDialog(self)
        if dialog.exec() != QDialog.Accepted:
            return
        try:
            path = generate_synthetic_telemetry(
                Path.home() / "Documents" / "IgnitionAI" / "Synthetic",
                str(dialog.scenario.currentData()),
                duration_seconds=dialog.duration.value() * 60,
                sample_rate_hz=dialog.rate.value(),
                seed=dialog.seed.value(),
            )
            self.path_input.setText(str(path))
            self.status.setText("SYNTHETIC validation trace created; beginning analysis...")
            self._start_worker(generate_report=False)
        except Exception as exc:
            QMessageBox.critical(self, "Synthetic generator", str(exc))

    def _start_report(self) -> None:
        if self._last_result is not None:
            self._start_worker(generate_report=True)

    def _export_pdf(self) -> None:
        if self._last_result is None:
            return
        default_name = str(Path.home() / "Documents" / "IgnitionAI_Diagnostic_Report.pdf")
        path, _ = QFileDialog.getSaveFileName(self, "Export diagnostic report", default_name, "PDF report (*.pdf)")
        if not path:
            return
        if not path.lower().endswith(".pdf"):
            path += ".pdf"
        try:
            generate_telemetry_pdf_report(path, self._last_result, self.report.toPlainText())
            self.status.setText(f"Report exported to {path}")
            QMessageBox.information(self, "Export complete", f"Diagnostic report saved to:\n{path}")
        except Exception as exc:
            QMessageBox.critical(self, "PDF export failed", str(exc))

    def _start_worker(self, generate_report: bool) -> None:
        source = self.path_input.text().strip()
        if not source:
            QMessageBox.warning(self, "Telemetry source", "Select a telemetry file first.")
            return
        self.analyze_button.setEnabled(False)
        self.synthetic_button.setEnabled(False)
        self.report_button.setEnabled(False)
        self.export_button.setEnabled(False)
        self.status.setText("Analyzing measured signals and computing virtual sensors...")
        self._thread = QThread(self)
        self._worker = AnalysisWorker(source, generate_report)
        self._worker.moveToThread(self._thread)
        self._thread.started.connect(self._worker.run)
        self._worker.finished.connect(self._complete)
        self._worker.failed.connect(self._failed)
        self._worker.finished.connect(self._thread.quit)
        self._worker.failed.connect(self._thread.quit)
        self._thread.finished.connect(self._thread.deleteLater)
        self._thread.start()

    def _complete(self, result: dict) -> None:
        if "mechanic_report" in result and self._last_result is not None:
            self.report.setPlainText(result["mechanic_report"])
            self.status.setText("Evidence-constrained mechanic note generated.")
            self.tabs.setCurrentIndex(2)
        else:
            self._last_result = result
            self._render_result(result)
            self.analysis_ready.emit(result)
        enabled = self._last_result is not None
        self.analyze_button.setEnabled(True)
        self.synthetic_button.setEnabled(True)
        self.report_button.setEnabled(enabled)
        self.export_button.setEnabled(enabled)

    def _failed(self, message: str) -> None:
        enabled = self._last_result is not None
        self.analyze_button.setEnabled(True)
        self.synthetic_button.setEnabled(True)
        self.report_button.setEnabled(enabled)
        self.export_button.setEnabled(enabled)
        self.status.setText("Analysis failed.")
        QMessageBox.critical(self, "Telemetry Engine", message)

    @staticmethod
    def _set_row(table: QTableWidget, row: int, values: list[object]) -> None:
        for column, value in enumerate(values):
            table.setItem(row, column, QTableWidgetItem(str(value)))

    def _render_result(self, result: dict) -> None:
        source = result["source"]
        fusion = result["fusion"]
        vhi = fusion["vehicle_health_index"]
        faults = fusion["fault_candidates"]
        self.vhi_card.value.setText("N/A" if vhi is None else f"{vhi:.1f}")
        self.vhi_card.note.setText("out of 100")
        self.records_card.value.setText(f"{source['rows']:,}")
        provenance = "SYNTHETIC validation" if source.get("synthetic") else "measured telemetry"
        self.records_card.note.setText(provenance)
        self.coverage_card.value.setText(str(len(fusion["available_indices"])))
        self.coverage_card.note.setText("physics / statistical indices")
        self.findings_card.value.setText(str(len(faults)))
        self.findings_card.note.setText("ranked candidates")
        self.status.setText(f"Analysis complete - {provenance} - database session #{result['database_session_id']} - ready for export.")

        sensors = result["virtual_sensors"]
        self.sensor_table.setRowCount(len(sensors))
        for row, sensor in enumerate(sensors):
            evidence = "; ".join(f"{key.replace('_', ' ')}: {value}" for key, value in sensor["evidence"].items() if key != "flag_timestamps")
            health = "N/A" if sensor["health_score"] is None else f"{sensor['health_score']:.1f}%"
            candidate = (sensor["candidate"] or "None").replace("_", " ").title()
            self._set_row(self.sensor_table, row, [sensor["name"].replace("_", " ").title(), health, sensor["status"].replace("_", " ").title(), candidate, evidence])

        self.fault_table.setRowCount(max(1, len(faults)))
        if faults:
            for row, fault in enumerate(faults):
                evidence = "; ".join(f"{key.replace('_', ' ')}: {value}" for key, value in fault["evidence"].items() if key != "flag_timestamps")
                self._set_row(self.fault_table, row, [fault["candidate"].replace("_", " ").title(), f"{fault['confidence'] * 100:.0f}%", evidence])
        else:
            self._set_row(self.fault_table, 0, ["No configured threshold crossed", "-", "No candidate was fabricated."])

        plans = result.get("repair_plan", [])
        rows = sum(len(plan.get("steps", [])) for plan in plans)
        self.repair_table.setRowCount(max(1, rows))
        row = 0
        for plan in plans:
            candidate = plan["candidate"].replace("_", " ").title()
            for step_number, step in enumerate(plan.get("steps", []), start=1):
                self._set_row(self.repair_table, row, [f"P{plan['rank']} / {step_number}", step["stage"], step["action"], step["method"], step["pass"]])
                if step_number == 1:
                    self.repair_table.item(row, 0).setToolTip(candidate)
                row += 1
        if not plans:
            self._set_row(self.repair_table, 0, ["-", "Monitor", "No repair workflow opened", "No evidence threshold was crossed.", "Continue routine monitoring."])
        self.repair_table.resizeRowsToContents()
        self.tabs.setTabText(1, f"Repair plan ({len(plans)})")
        self.tabs.setCurrentIndex(1 if plans else 0)
        self.report.clear()
