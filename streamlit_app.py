import os
import sys

# Set serving directory in sys.path
SERVING_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "serving")
if SERVING_DIR not in sys.path:
    sys.path.insert(0, SERVING_DIR)

# Execute main application
app_path = os.path.join(SERVING_DIR, "streamlit_app.py")
with open(app_path, "r", encoding="utf-8") as f:
    code = compile(f.read(), app_path, "exec")
    exec(code, globals())
