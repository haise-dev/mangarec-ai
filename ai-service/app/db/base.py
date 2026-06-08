"""Declarative base for all SQLAlchemy ORM models.

Import this `Base` in every model module so that all tables are registered
on the same metadata object.  Then call `Base.metadata.create_all(engine)`
once at startup (handled by `init_db()` in session.py).
"""

from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    """Shared declarative base — all ORM models must inherit from this."""

    pass
