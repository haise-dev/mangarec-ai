"""Script to test MangaDex API and observe the actual data structure returned."""

import json
import logging
from typing import Any, Dict

import requests

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

MANGADEX_API_URL = "https://api.mangadex.org"


def fetch_single_manga_data() -> Dict[str, Any]:
    """Fetch a single trending manga from MangaDex API to inspect its structure.

    Returns:
        Dict[str, Any]: The raw JSON data of the manga.
        
    Raises:
        requests.RequestException: If the API request fails.
    """
    url = f"{MANGADEX_API_URL}/manga"
    params = {
        "limit": 1,
        "offset": 0,
        "includes[]": ["cover_art", "author", "artist"],
        "order[followedCount]": "desc",
        "hasAvailableChapters": "true",
    }
    
    logger.info("Fetching 1 trending manga from MangaDex...")
    try:
        response = requests.get(url, params=params, timeout=10)
        response.raise_for_status()
        data = response.json().get("data", [])
        if not data:
            logger.warning("No manga found.")
            return {}
        return data[0]
    except requests.RequestException as e:
        logger.error("Failed to fetch data from MangaDex: %s", e)
        raise


def fetch_manga_statistics(manga_id: str) -> Dict[str, Any]:
    """Fetch statistics (rating, follows) for a specific manga.

    Args:
        manga_id (str): The UUID of the manga.

    Returns:
        Dict[str, Any]: The raw JSON statistics data.
        
    Raises:
        requests.RequestException: If the API request fails.
    """
    url = f"{MANGADEX_API_URL}/statistics/manga/{manga_id}"
    
    logger.info("Fetching statistics for manga ID: %s", manga_id)
    try:
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        stats = response.json().get("statistics", {}).get(manga_id, {})
        return stats
    except requests.RequestException as e:
        logger.error("Failed to fetch statistics from MangaDex: %s", e)
        raise


def main() -> None:
    """Main execution entry point."""
    try:
        manga_data = fetch_single_manga_data()
        if not manga_data:
            return
            
        manga_id = manga_data.get("id", "")
        stats_data = fetch_manga_statistics(manga_id)
        
        # Combine and print nicely
        result = {
            "manga": manga_data,
            "statistics": stats_data
        }
        
        print("\n=== MANGADEX API RAW RESPONSE STRUCTURE ===")
        print(json.dumps(result, indent=2, ensure_ascii=False))
        
    except requests.RequestException:
        logger.error("Script execution failed due to API errors.")


if __name__ == "__main__":
    main()
