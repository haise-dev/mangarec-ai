# Alembic & Models Integration Plan

**Goal:** Implement the database schema defined in `docs/data_architecture_and_strategy.md` (v1.1) and set up Alembic for production-ready migrations.

## Steps
1. **Dependency Management:** Add `alembic` via `uv`.
2. **Initialization:** Run `alembic init alembic` to create the migration environment.
3. **Model Creation:** Translate the 7 ERD entities into strict SQLAlchemy 2.0 models using `Mapped` and `mapped_column` in `app/db/models.py`.
4. **Environment Config:** Update `alembic/env.py` to dynamic load the database connection string from `app.core.config.settings` and register `Base.metadata`.
5. **App Integration:** Refactor `app/main.py` `lifespan` hook. Replace the legacy `Base.metadata.create_all` with `command.upgrade("head")` to ensure auto-migration on Docker startup.
6. **First Migration:** Run `--autogenerate` to create the initial `Init tables` script.

**Status:** Completed.
