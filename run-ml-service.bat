@echo off
echo =======================================================================
echo [IgnitionAI] Starting Real Hybrid ML Diagnostics FastAPI Microservice
echo Serving: ml/models/best_hybrid_enginefault_model.joblib
echo Port: 8000 (http://127.0.0.1:8000)
echo =======================================================================
cd /d "%~dp0"
python ml/ml_service.py
