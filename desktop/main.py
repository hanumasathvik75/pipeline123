"""
IgnitionAI Desktop Application
Entry Point — Splash Screen → Main Window
"""

import sys
import os
import time

# Add the desktop directory to the Python path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# Copy logo to assets folder if not already there
def _setup_assets():
    assets_dir = os.path.join(os.path.dirname(__file__), "assets")
    os.makedirs(assets_dir, exist_ok=True)
    logo_src = os.path.join(os.path.dirname(__file__), "..", "logo.png")
    logo_dst = os.path.join(assets_dir, "logo.png")
    if not os.path.exists(logo_dst) and os.path.exists(logo_src):
        import shutil
        shutil.copy2(logo_src, logo_dst)


from PySide6.QtWidgets import QApplication
from PySide6.QtCore import Qt, QTimer
from PySide6.QtGui import QFont

from app.config import APP_NAME
from app.ui.splash import IgnitionSplash
from app.ui.main_window import MainWindow


def main():
    _setup_assets()

    app = QApplication(sys.argv)
    app.setApplicationName(APP_NAME)
    app.setOrganizationName("IgnitionAI")

    # Set a better default font
    default_font = QFont("Segoe UI", 10)
    app.setFont(default_font)

    # ── Splash Screen ─────────────────────────────────────────────────────
    splash = IgnitionSplash()
    splash.show()
    app.processEvents()

    # Simulate loading steps
    steps = [
        (10, "Loading diagnostic database..."),
        (25, "Initializing ML engine..."),
        (40, "Setting up OBD simulator..."),
        (60, "Configuring Gemini AI..."),
        (80, "Building user interface..."),
        (95, "Finalizing..."),
        (100, "Ready!"),
    ]

    main_window = None
    for progress, message in steps:
        splash.set_progress(progress, message)
        time.sleep(0.18)
        if progress == 80:
            main_window = MainWindow()

    time.sleep(0.3)
    splash.finish(main_window)
    main_window.show()

    sys.exit(app.exec())


if __name__ == "__main__":
    main()
