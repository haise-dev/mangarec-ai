"""AI Worker Scheduler.

Periodically runs the MangaDex ingestion script.
Handles initial Auto-Seed if database is empty.
"""

import logging
import os
import subprocess
import sys
import time

import schedule

# Ensure app modules can be imported
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
from app.db.models import Manga, EtlCheckpoint
from app.db.session import SessionLocal, init_db

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger("scheduler")


def run_seed():
    logger.info("Executing Seed Sync (1000 items)...")
    subprocess.run(
        [sys.executable, "scripts/ingest_manga.py", "--mode", "seed", "--limit", "1000"],
        check=False,
    )


def run_daily():
    try:
        db = SessionLocal()
        checkpoint = db.query(EtlCheckpoint).filter_by(job_name="daily").first()
        updated_since_arg = []
        if checkpoint and checkpoint.started_at:
            iso_time = checkpoint.started_at.strftime("%Y-%m-%dT%H:%M:%S")
            updated_since_arg = ["--updated-since", iso_time]
        db.close()
    except Exception as e:
        logger.error(f"Error querying checkpoint: {e}")
        updated_since_arg = []

    logger.info(f"Executing Daily Sync (Delta Sync for updated items)... Args: {updated_since_arg}")
    subprocess.run(
        [sys.executable, "scripts/ingest_manga.py", "--mode", "daily", "--limit", "500"] + updated_since_arg,
        check=False,
    )
    logger.info("Executing Daily Sync (500 top followed items)...")
    subprocess.run(
        [sys.executable, "scripts/ingest_manga.py", "--mode", "seed", "--limit", "500"],
        check=False,
    )


def check_and_seed():
    """Checks if DB is empty, runs seed if it is. Retries if DB is not ready."""
    max_retries = 10
    retry_delay = 5
    
    for attempt in range(max_retries):
        try:
            db = SessionLocal()
            count = db.query(Manga).count()
            db.close()

            if count == 0:
                logger.info("Database is empty. Initiating Auto-Seed process...")
                run_seed()
            else:
                logger.info(f"Database already has {count} mangas. Skipping Auto-Seed.")
            return # Success, exit retry loop
            
        except Exception as e:
            logger.warning(f"Attempt {attempt + 1}/{max_retries}: Error checking DB for seed (DB might not be initialized yet by ai-service). Retrying in {retry_delay}s... Error: {e}")
            time.sleep(retry_delay)
            
    logger.error("Failed to check DB for seed after maximum retries. Auto-Seed skipped.")


if __name__ == "__main__":
    logger.info("Starting AI Worker Scheduler...")

    # Wait a bit for DB/Qdrant volumes to be fully ready
    time.sleep(5)

    check_and_seed()

    # Schedule daily job at 03:00 AM
    schedule.every().day.at("03:00").do(run_daily)

    logger.info("Scheduler is running. Waiting for scheduled jobs...")
    while True:
        schedule.run_pending()
        time.sleep(60)
