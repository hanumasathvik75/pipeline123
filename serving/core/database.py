import os
from pathlib import Path
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from dotenv import load_dotenv

load_dotenv(Path(__file__).parents[1] / ".env")

# Default to SQLite for zero-config local development.
# Set DATABASE_URL in .env for PostgreSQL in production / Docker.
_default_db = f"sqlite:///{Path(__file__).parents[1] / 'ignitionai_local.db'}"
SQLALCHEMY_DATABASE_URL = os.getenv("DATABASE_URL", _default_db)

_connect_args = {}
if SQLALCHEMY_DATABASE_URL.startswith("sqlite"):
    _connect_args["check_same_thread"] = False

engine = create_engine(SQLALCHEMY_DATABASE_URL, connect_args=_connect_args)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
