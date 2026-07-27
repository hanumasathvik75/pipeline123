"""
IgnitionAI — Splash Screen
Full-screen branded loading screen with animated progress bar.
"""

import os
import sys
from PySide6.QtWidgets import QSplashScreen, QApplication, QLabel, QVBoxLayout, QWidget, QProgressBar
from PySide6.QtCore import Qt, QTimer, QPropertyAnimation, QEasingCurve
from PySide6.QtGui import QPixmap, QPainter, QColor, QFont, QLinearGradient

from app.config import (
    APP_NAME, APP_VERSION, APP_SUBTITLE,
    COLOR_BG_DARKEST, COLOR_CYAN, COLOR_TEXT_SECONDARY, COLOR_ACCENT_BLUE, COLOR_TEXT_PRIMARY
)
from app.ui.styles import get_outlined_logo


LOGO_PATH = os.path.join(os.path.dirname(__file__), "..", "..", "assets", "logo.png")


class IgnitionSplash(QSplashScreen):
    def __init__(self):
        # Create a 900x500 pixmap for the splash
        pixmap = QPixmap(900, 500)
        pixmap.fill(QColor(COLOR_BG_DARKEST))
        super().__init__(pixmap, Qt.WindowStaysOnTopHint)
        self.setWindowFlag(Qt.FramelessWindowHint)
        self._progress = 0
        self._draw_static_content(pixmap)

    def _draw_static_content(self, pixmap: QPixmap):
        painter = QPainter(pixmap)
        painter.setRenderHint(QPainter.Antialiasing)

        # Background gradient
        gradient = QLinearGradient(0, 0, 900, 500)
        gradient.setColorAt(0, QColor("#060E1A"))
        gradient.setColorAt(0.5, QColor("#0A1628"))
        gradient.setColorAt(1, QColor("#071120"))
        painter.fillRect(0, 0, 900, 500, gradient)

        # Subtle grid lines (decorative)
        painter.setPen(QColor("#0D2040"))
        for x in range(0, 900, 60):
            painter.drawLine(x, 0, x, 500)
        for y in range(0, 500, 60):
            painter.drawLine(0, y, 900, y)

        # Logo (outlined)
        logo_outlined = get_outlined_logo(LOGO_PATH, 150)
        if not logo_outlined.isNull():
            # Center horizontally, place at y=70
            x = (900 - logo_outlined.width()) // 2
            painter.drawPixmap(x, 70, logo_outlined)

        # App name
        font = QFont("Segoe UI", 38, QFont.Bold)
        painter.setFont(font)
        painter.setPen(QColor(COLOR_TEXT_PRIMARY))
        painter.drawText(0, 240, 900, 60, Qt.AlignHCenter | Qt.AlignVCenter, APP_NAME)

        # Subtitle
        font2 = QFont("Segoe UI", 13)
        painter.setFont(font2)
        painter.setPen(QColor(COLOR_TEXT_SECONDARY))
        painter.drawText(0, 300, 900, 30, Qt.AlignHCenter | Qt.AlignVCenter, APP_SUBTITLE)

        # Cyan accent line under title
        painter.setPen(QColor(COLOR_CYAN))
        painter.drawLine(350, 340, 550, 340)

        painter.end()
        self.setPixmap(pixmap)

    def set_progress(self, value: int, message: str = ""):
        self._progress = value
        pix = self.pixmap().copy()
        painter = QPainter(pix)
        painter.setRenderHint(QPainter.Antialiasing)

        # Clear progress area
        painter.fillRect(100, 390, 700, 60, QColor(COLOR_BG_DARKEST))

        # Progress bar background
        painter.setPen(Qt.NoPen)
        painter.setBrush(QColor("#0D2040"))
        painter.drawRoundedRect(100, 420, 700, 10, 5, 5)

        # Progress bar fill
        fill_w = int(700 * value / 100)
        grad = QLinearGradient(100, 0, 100 + fill_w, 0)
        grad.setColorAt(0, QColor(COLOR_ACCENT_BLUE))
        grad.setColorAt(1, QColor(COLOR_CYAN))
        painter.setBrush(grad)
        painter.drawRoundedRect(100, 420, fill_w, 10, 5, 5)

        # Status message
        font = QFont("Segoe UI", 10)
        painter.setFont(font)
        painter.setPen(QColor(COLOR_TEXT_SECONDARY))
        painter.drawText(100, 395, 700, 22, Qt.AlignHCenter, message)

        # Version label
        font_sm = QFont("Segoe UI", 9)
        painter.setFont(font_sm)
        painter.setPen(QColor("#2A4A6A"))
        painter.drawText(750, 470, 140, 20, Qt.AlignRight, f"v{APP_VERSION}")

        painter.end()
        self.setPixmap(pix)
        QApplication.processEvents()
