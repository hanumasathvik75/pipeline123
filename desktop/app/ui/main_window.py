"""Minimal monochrome native shell for IgnitionAI's telemetry engine."""
from __future__ import annotations

import ctypes
import os
import sys

from PySide6.QtCore import Qt
from PySide6.QtGui import QColor, QIcon, QImage, QPixmap
from PySide6.QtWidgets import QFrame, QHBoxLayout, QLabel, QMainWindow, QVBoxLayout, QWidget

from app.config import APP_NAME, APP_VERSION
from app.ui.telemetry_engine import TelemetryEnginePanel


LOGO_PATH = os.path.join(os.path.dirname(__file__), "..", "..", "assets", "logo_transparent_white.png")


def exact_wordmark(path: str, width: int, height: int) -> QPixmap:
    image = QImage(path)
    if image.isNull():
        return QPixmap()
    # In this supplied PNG the canvas is opaque and the white logo is encoded
    # as inverse alpha. Correct that encoding without changing its geometry.
    crop = image.copy(203, 174, 621, 213)
    corrected = QImage(crop.size(), QImage.Format_ARGB32)
    corrected.fill(Qt.transparent)
    for y in range(crop.height()):
        for x in range(crop.width()):
            source_alpha = crop.pixelColor(x, y).alpha()
            corrected_alpha = min(255, (255 - source_alpha) * 6)
            corrected.setPixelColor(x, y, QColor(246, 246, 244, corrected_alpha))
    return QPixmap.fromImage(corrected.scaled(width, height, Qt.KeepAspectRatio, Qt.SmoothTransformation))


class MainWindow(QMainWindow):
    def __init__(self) -> None:
        super().__init__()
        self.setWindowTitle(f"{APP_NAME} — Telemetry Engine")
        self.resize(1440, 900)
        self.setMinimumSize(1120, 720)
        self.setStyleSheet(self._stylesheet())
        logo = exact_wordmark(LOGO_PATH, 128, 128)
        if not logo.isNull():
            self.setWindowIcon(QIcon(logo))
        self._build_ui()

    def _build_ui(self) -> None:
        central = QWidget(); self.setCentralWidget(central)
        root = QVBoxLayout(central); root.setContentsMargins(0, 0, 0, 0); root.setSpacing(0)
        header = QFrame(); header.setObjectName("header")
        header.setFixedHeight(94)
        header_layout = QHBoxLayout(header); header_layout.setContentsMargins(34, 0, 34, 0)
        logo = exact_wordmark(LOGO_PATH, 280, 74)
        if not logo.isNull():
            logo_label = QLabel(); logo_label.setPixmap(logo); header_layout.addWidget(logo_label)
        header_layout.addStretch()
        status = QLabel("LOCAL ENGINE   /   POSTGRESQL CONNECTED")
        status.setObjectName("status"); header_layout.addWidget(status)
        root.addWidget(header)
        self.telemetry_engine = TelemetryEnginePanel(self)
        self.telemetry_engine.analysis_ready.connect(self._set_complete)
        root.addWidget(self.telemetry_engine, 1)

    def _set_complete(self, _result: dict) -> None:
        self.statusBar().showMessage("Analysis saved locally in PostgreSQL", 5000)

    def showEvent(self, event) -> None:
        super().showEvent(event)
        if sys.platform != "win32":
            return
        try:
            hwnd = int(self.winId())
            dark = ctypes.c_int(1)
            caption = ctypes.c_int(0x00101010)
            text = ctypes.c_int(0x00F4F4F2)
            ctypes.windll.dwmapi.DwmSetWindowAttribute(hwnd, 20, ctypes.byref(dark), ctypes.sizeof(dark))
            ctypes.windll.dwmapi.DwmSetWindowAttribute(hwnd, 35, ctypes.byref(caption), ctypes.sizeof(caption))
            ctypes.windll.dwmapi.DwmSetWindowAttribute(hwnd, 36, ctypes.byref(text), ctypes.sizeof(text))
        except (AttributeError, OSError):
            pass

    @staticmethod
    def _stylesheet() -> str:
        return """
        QMainWindow, QWidget { background: #0A0A0B; color: #ECEDEB; font-family: 'Segoe UI'; font-size: 13px; }
        QFrame#header { background: #000000; border-bottom: 1px solid #262628; }
        QLabel { background: transparent; }
        QLabel#status { color: #85858A; font-size: 9px; font-weight: 700; letter-spacing: 1.2px; }
        QLabel#pageTitle { color: #F4F4F2; font-size: 25px; font-weight: 650; }
        QLabel#dialogTitle { color: #F4F4F2; font-size: 18px; font-weight: 650; }
        QLabel#pageSubtitle { color: #85858A; font-size: 12px; }
        QLabel#engineBadge { color: #A4A4A8; background: #141416; border: 1px solid #2A2A2D; border-radius: 14px; padding: 7px 12px; font-size: 9px; font-weight: 700; letter-spacing: 1px; }
        QLabel#eyebrow, QLabel#metricLabel { color: #7F8085; font-size: 9px; font-weight: 700; letter-spacing: 1.3px; }
        QLabel#statusText, QLabel#tabIntro, QLabel#metricNote { color: #838389; font-size: 11px; }
        QLabel#metricValue { color: #F3F3F1; font-size: 27px; font-weight: 650; }
        QFrame#card, QFrame#metricCard { background: #111113; border: 1px solid #28282B; border-radius: 9px; }
        QLineEdit, QTextEdit, QComboBox, QSpinBox, QDoubleSpinBox { background: #0C0C0D; color: #EDEDEB; border: 1px solid #303034; border-radius: 6px; padding: 9px 11px; selection-background-color: #EDEDEB; selection-color: #111113; }
        QLineEdit:focus, QTextEdit:focus, QComboBox:focus, QSpinBox:focus, QDoubleSpinBox:focus { border-color: #77777C; }
        QComboBox::drop-down, QSpinBox::up-button, QSpinBox::down-button, QDoubleSpinBox::up-button, QDoubleSpinBox::down-button { border: 0; width: 22px; }
        QPushButton { border-radius: 6px; padding: 9px 14px; font-size: 11px; font-weight: 650; min-height: 32px; }
        QPushButton#primaryButton { background: #F0F0EE; color: #111113; border: 1px solid #F0F0EE; }
        QPushButton#primaryButton:hover { background: #FFFFFF; }
        QPushButton#secondaryButton { background: #171719; color: #E5E5E3; border: 1px solid #353539; }
        QPushButton#secondaryButton:hover { background: #222225; border-color: #5A5A60; }
        QPushButton:disabled { background: #131315; color: #545459; border-color: #252528; }
        QTabWidget::pane { background: #101012; border: 1px solid #28282B; border-radius: 8px; top: -1px; }
        QTabBar::tab { background: transparent; color: #77777D; border: 0; padding: 11px 20px; font-size: 11px; font-weight: 600; }
        QTabBar::tab:selected { color: #F1F1EF; border-bottom: 2px solid #F1F1EF; }
        QTabBar::tab:hover { color: #C7C7C4; }
        QTableWidget { background: #0D0D0F; alternate-background-color: #111113; border: 1px solid #262629; border-radius: 6px; gridline-color: #242427; color: #DEDEDC; }
        QTableWidget::item { padding: 7px 9px; }
        QHeaderView::section { background: #151517; color: #85858A; border: 0; border-bottom: 1px solid #303034; padding: 9px; font-size: 9px; font-weight: 700; text-transform: uppercase; }
        QScrollArea { border: 0; }
        QSplitter::handle { background: #29292C; height: 1px; }
        QScrollBar:vertical { background: transparent; width: 8px; margin: 2px; }
        QScrollBar::handle:vertical { background: #3B3B3F; border-radius: 4px; min-height: 30px; }
        QStatusBar { background: #0D0D0E; color: #77777D; border-top: 1px solid #242426; }
        """
