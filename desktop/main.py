"""
IgnitionAI Desktop Application
Entry Point — Splash Screen → Main Window
"""

import sys
import os

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
from PySide6.QtGui import QFont

from app.config import APP_NAME
from app.ui.main_window import MainWindow


def main():
    _setup_assets()

    app = QApplication(sys.argv)
    app.setApplicationName(APP_NAME)
    app.setOrganizationName("IgnitionAI")

    # Set a better default font
    default_font = QFont("Segoe UI", 10)
    app.setFont(default_font)

    main_window = MainWindow()
    main_window.show()
    main_window.raise_()
    main_window.activateWindow()

    sys.exit(app.exec())


if __name__ == "__main__":
    main()
