"""
IgnitionAI — AI Diagnosis Panel
Gemini AI plain-English diagnosis, repair narrative, and PDF report export.
"""

import os
from PySide6.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QLabel, QFrame,
    QPushButton, QTextEdit, QScrollArea, QMessageBox, QProgressBar,
    QSizePolicy, QFileDialog
)
from PySide6.QtCore import Qt, Signal, QThread, QObject
from PySide6.QtGui import QFont

from app.config import (
    COLOR_BG_PANEL, COLOR_BG_CARD, COLOR_BORDER, COLOR_CYAN,
    COLOR_TEXT_PRIMARY, COLOR_TEXT_SECONDARY, COLOR_SUCCESS,
    COLOR_WARNING, COLOR_DANGER, COLOR_ACCENT_BLUE, COLOR_LIGHTER_BLUE,
    APP_NAME, APP_VERSION
)
from app.engine.gemini_ai import (
    generate_plain_english_diagnosis,
    generate_repair_narrative,
    generate_pdf_summary,
)


# ─── AI Worker ────────────────────────────────────────────────────────────────

class AIWorker(QObject):
    diagnosis_ready = Signal(str)
    repair_ready = Signal(str)
    error = Signal(str)
    finished = Signal()

    def __init__(self, results: dict, vehicle_info: str):
        super().__init__()
        self.results = results
        self.vehicle_info = vehicle_info

    def run(self):
        try:
            dtcs = self.results.get("input_dtcs", [])
            final_root = self.results.get("final_root", "Unknown")
            sensors = self.results.get("input_sensors", {})
            repair_steps = self.results.get("repair_steps", [])

            diagnosis = generate_plain_english_diagnosis(
                dtcs, final_root, sensors, self.vehicle_info
            )
            self.diagnosis_ready.emit(diagnosis)

            repair = generate_repair_narrative(final_root, dtcs, repair_steps)
            self.repair_ready.emit(repair)
        except Exception as e:
            self.error.emit(str(e))
        finally:
            self.finished.emit()


class AIThread(QThread):
    def __init__(self, worker):
        super().__init__()
        self.worker = worker
        self.worker.moveToThread(self)
        self.started.connect(self.worker.run)


# ─── AI Panel ─────────────────────────────────────────────────────────────────

class AIPanel(QWidget):
    def __init__(self, parent=None):
        super().__init__(parent)
        self._results = {}
        self._diagnosis_text = ""
        self._repair_text = ""
        self._build_ui()

    def _build_ui(self):
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll.setFrameShape(QFrame.NoFrame)

        container = QWidget()
        layout = QVBoxLayout(container)
        layout.setContentsMargins(32, 28, 32, 28)
        layout.setSpacing(20)

        # ── AI Header Card ────────────────────────────────────────────────
        header_card = QFrame()
        header_card.setObjectName("card")
        header_card.setStyleSheet(f"""
            QFrame#card {{
                background: qlineargradient(x1:0, y1:0, x2:1, y2:0,
                    stop:0 #060E1A, stop:0.5 #0A1628, stop:1 #060E1A);
                border: 1px solid {COLOR_BORDER};
                border-top: 2px solid {COLOR_CYAN};
                border-radius: 14px;
            }}
        """)
        header_layout = QHBoxLayout(header_card)
        header_layout.setContentsMargins(24, 18, 24, 18)

        hdr_text = QVBoxLayout()
        gem_title = QLabel("Gemini AI Diagnosis")
        gem_title.setStyleSheet(f"font-size: 20px; font-weight: 800; color: {COLOR_TEXT_PRIMARY};")
        gem_sub = QLabel("Powered by gemini-2.0-flash-lite  •  Plain-English vehicle health report")
        gem_sub.setStyleSheet(f"font-size: 12px; color: {COLOR_TEXT_SECONDARY};")
        hdr_text.addWidget(gem_title)
        hdr_text.addWidget(gem_sub)
        header_layout.addLayout(hdr_text, 3)
        header_layout.addStretch(1)

        # Generate button
        self.gen_btn = QPushButton("Generate AI Report")
        self.gen_btn.setObjectName("primary_btn")
        self.gen_btn.setMinimumWidth(200)
        self.gen_btn.setMinimumHeight(44)
        self.gen_btn.clicked.connect(self._generate)
        header_layout.addWidget(self.gen_btn)
        layout.addWidget(header_card)

        # ── Progress bar ──────────────────────────────────────────────────
        self.progress = QProgressBar()
        self.progress.setRange(0, 0)
        self.progress.setFixedHeight(6)
        self.progress.hide()
        layout.addWidget(self.progress)

        self.progress_label = QLabel("")
        self.progress_label.setStyleSheet(f"color: {COLOR_CYAN}; font-size: 11px; font-style: italic;")
        self.progress_label.hide()
        layout.addWidget(self.progress_label)

        # ── Two-column output ─────────────────────────────────────────────
        cols = QHBoxLayout()
        cols.setSpacing(20)

        # Left: Diagnosis
        diag_frame = QFrame()
        diag_frame.setObjectName("card")
        diag_layout = QVBoxLayout(diag_frame)
        diag_layout.setContentsMargins(20, 16, 20, 16)
        diag_layout.setSpacing(10)

        diag_hdr = QHBoxLayout()
        diag_title = QLabel("PLAIN-ENGLISH DIAGNOSIS")
        diag_title.setObjectName("section_header")
        diag_hdr.addWidget(diag_title)
        diag_hdr.addStretch()

        self.diag_badge = QLabel("  AI Generated  ")
        self.diag_badge.setObjectName("ai_badge")
        self.diag_badge.hide()
        diag_hdr.addWidget(self.diag_badge)
        diag_layout.addLayout(diag_hdr)

        self.diagnosis_output = QTextEdit()
        self.diagnosis_output.setReadOnly(True)
        self.diagnosis_output.setPlaceholderText(
            "Generate AI report to see a plain-English explanation of the diagnosis here.\n\n"
            "Gemini AI will explain:\n• What is wrong with the vehicle\n"
            "• Why this root cause was identified\n• Urgency level and safety assessment"
        )
        self.diagnosis_output.setMinimumHeight(200)
        self.diagnosis_output.setStyleSheet(f"""
            QTextEdit {{
                background-color: #060E1A;
                color: {COLOR_TEXT_PRIMARY};
                border: 1px solid {COLOR_BORDER};
                border-radius: 8px;
                padding: 14px;
                font-size: 13px;
                line-height: 1.6;
            }}
        """)
        diag_layout.addWidget(self.diagnosis_output)
        cols.addWidget(diag_frame, 1)

        # Right: Repair narrative
        repair_frame = QFrame()
        repair_frame.setObjectName("card")
        repair_layout = QVBoxLayout(repair_frame)
        repair_layout.setContentsMargins(20, 16, 20, 16)
        repair_layout.setSpacing(10)

        repair_hdr = QHBoxLayout()
        repair_title = QLabel("REPAIR NARRATIVE")
        repair_title.setObjectName("section_header")
        repair_hdr.addWidget(repair_title)
        repair_hdr.addStretch()

        self.pdf_btn = QPushButton("Export PDF")
        self.pdf_btn.setObjectName("secondary_btn")
        self.pdf_btn.setFixedHeight(28)
        self.pdf_btn.clicked.connect(self._export_pdf)
        repair_hdr.addWidget(self.pdf_btn)
        repair_layout.addLayout(repair_hdr)

        self.repair_output = QTextEdit()
        self.repair_output.setReadOnly(True)
        self.repair_output.setPlaceholderText(
            "The repair narrative will appear here.\n\n"
            "It will include:\n• Required tools and parts estimate\n"
            "• Step-by-step procedure\n• Safety warnings\n• Time and difficulty estimate"
        )
        self.repair_output.setMinimumHeight(200)
        self.repair_output.setStyleSheet(f"""
            QTextEdit {{
                background-color: #060E1A;
                color: {COLOR_TEXT_PRIMARY};
                border: 1px solid {COLOR_BORDER};
                border-radius: 8px;
                padding: 14px;
                font-size: 13px;
                line-height: 1.6;
            }}
        """)
        repair_layout.addWidget(self.repair_output)
        cols.addWidget(repair_frame, 1)
        layout.addLayout(cols)

        # ── Info about the model ──────────────────────────────────────────
        info_card = QFrame()
        info_card.setObjectName("card")
        info_layout = QHBoxLayout(info_card)
        info_layout.setContentsMargins(20, 12, 20, 12)

        info_text = QLabel(
            f"Using <b>gemini-2.0-flash-lite</b> — optimized for fast, accurate technical text generation. "
            f"Results are AI-generated suggestions. Always validate with a certified mechanic."
        )
        info_text.setStyleSheet(f"font-size: 11px; color: {COLOR_TEXT_SECONDARY};")
        info_text.setWordWrap(True)
        info_layout.addWidget(info_text)
        layout.addWidget(info_card)
        layout.addStretch()

        scroll.setWidget(container)
        outer = QVBoxLayout(self)
        outer.setContentsMargins(0, 0, 0, 0)
        outer.addWidget(scroll)

    def load_results(self, results: dict):
        """Load results from scan — reset AI output."""
        self._results = results
        self.diagnosis_output.clear()
        self.repair_output.clear()
        self.diag_badge.hide()
        self._diagnosis_text = ""
        self._repair_text = ""

        # Show summary of what will be analyzed
        final_root = results.get("final_root", "Unknown")
        dtcs = results.get("input_dtcs", [])
        self.diagnosis_output.setPlaceholderText(
            f"Ready to analyze:\n\n"
            f"Root Cause: {final_root}\n"
            f"DTCs: {', '.join(dtcs)}\n\n"
            f"Click 'Generate AI Report' to get a Gemini AI explanation."
        )

    def _generate(self):
        if not self._results:
            QMessageBox.information(self, "No Data", "Please run a scan first.")
            return

        self.gen_btn.setEnabled(False)
        self.gen_btn.setText("Generating...")
        self.progress.show()
        self.progress_label.setText("Connecting to Gemini AI... this may take 5-10 seconds")
        self.progress_label.show()
        self.diagnosis_output.setPlainText("")
        self.repair_output.setPlainText("")

        self._worker = AIWorker(self._results, "Patient Vehicle")
        self._thread = AIThread(self._worker)
        self._worker.diagnosis_ready.connect(self._on_diagnosis)
        self._worker.repair_ready.connect(self._on_repair)
        self._worker.error.connect(self._on_error)
        self._worker.finished.connect(self._on_thread_done)
        self._thread.start()

    def _on_diagnosis(self, text: str):
        self._diagnosis_text = text
        self.diagnosis_output.setMarkdown(text)
        self.diag_badge.show()
        self.progress_label.setText("Generating repair narrative...")

    def _on_repair(self, text: str):
        self._repair_text = text
        self.repair_output.setMarkdown(text)

    def _on_error(self, msg: str):
        self.diagnosis_output.setPlainText(
            f"Gemini AI connection failed:\n{msg}\n\n"
            "Please check your API key in app/config.py and ensure you have internet access.\n\n"
            "The rule-based diagnostic results are available in the Results tab."
        )

    def _on_thread_done(self):
        self.progress.hide()
        self.progress_label.hide()
        self.gen_btn.setEnabled(True)
        self.gen_btn.setText("Generate AI Report")
        self._thread.quit()

    def _export_pdf(self):
        if not self._results:
            QMessageBox.information(self, "No Data", "Please run a scan and generate AI report first.")
            return

        path, _ = QFileDialog.getSaveFileName(
            self, "Save Diagnostic Report", "IgnitionAI_Report.pdf", "PDF Files (*.pdf)"
        )
        if not path:
            return

        try:
            from app.ui.report import generate_pdf_report
            generate_pdf_report(
                path=path,
                results=self._results,
                diagnosis_text=self._diagnosis_text,
                repair_text=self._repair_text,
            )
            QMessageBox.information(self, "PDF Exported", f"Report saved to:\n{path}")
        except Exception as e:
            QMessageBox.critical(self, "PDF Error", f"Failed to generate PDF:\n{e}")
