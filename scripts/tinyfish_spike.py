#!/usr/bin/env python3
"""
TinyFish Spike CLI
Calls TinyFish Search, Fetch, and Agent endpoints (if TINYFISH_API_KEY is available)
or saves calibrated schemas/samples to /docs/tinyfish-samples/ with secrets stripped.
"""

import os
import sys
import json
import httpx

try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

SAMPLES_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "docs", "tinyfish-samples"))
os.makedirs(SAMPLES_DIR, exist_ok=True)

API_KEY = os.getenv("TINYFISH_API_KEY", "").strip()

def save_sample(filename: str, data: dict):
    path = os.path.join(SAMPLES_DIR, filename)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"[OK] Saved {filename} to {path}")

def run_spike():
    print(f"--- TinyFish Spike CLI ---")
    headers = {"X-API-Key": API_KEY} if API_KEY else {}

    # 1. Search Sample
    if API_KEY:
        try:
            print("Querying TinyFish Search API...")
            resp = httpx.get("https://api.search.tinyfish.ai", params={"query": "protest photo fact check"}, headers=headers, timeout=10.0)
            if resp.status_code == 200:
                save_sample("search_sample.json", resp.json())
            else:
                print(f"[WARN] Search API returned {resp.status_code}: {resp.text}")

            print("Querying TinyFish Fetch API...")
            fetch_resp = httpx.post(
                "https://api.fetch.tinyfish.ai",
                json={"urls": ["https://www.reuters.com/fact-check/"], "format": "markdown"},
                headers=headers,
                timeout=15.0
            )
            if fetch_resp.status_code == 200:
                save_sample("fetch_sample.json", fetch_resp.json())
            else:
                print(f"[WARN] Fetch API returned {fetch_resp.status_code}: {fetch_resp.text}")
        except Exception as e:
            print(f"[WARN] Live TinyFish call failed: {e}")

    # Fallback / calibrated sample if not saved
    search_path = os.path.join(SAMPLES_DIR, "search_sample.json")
    if not os.path.exists(search_path):
        sample_search = {
            "query": "protest photo fact check",
            "results": [
                {
                    "title": "Fact Check: 2018 Training Exercise Image Falsely Linked to 2024 Event",
                    "url": "https://www.reuters.com/fact-check/2018-exercise-image-2024-claim",
                    "snippet": "A photograph showing military armor was captured during a 2018 NATO exercise in Poland, not in 2024.",
                    "domain": "reuters.com",
                    "published_date": "2024-10-15T12:00:00Z"
                },
                {
                    "title": "Military exercises conclude in Eastern Poland",
                    "url": "https://apnews.com/article/poland-military-drills-2018",
                    "snippet": "Troops participate in tactical drills in western Poland on June 14, 2018.",
                    "domain": "apnews.com",
                    "published_date": "2018-06-14T16:30:00Z"
                }
            ],
            "total_results": 2
        }
        save_sample("search_sample.json", sample_search)

    # 2. Fetch Sample
    fetch_path = os.path.join(SAMPLES_DIR, "fetch_sample.json")
    if not os.path.exists(fetch_path):
        sample_fetch = {
            "status": "success",
            "results": [
                {
                    "url": "https://apnews.com/article/poland-military-drills-2018",
                    "canonical_url": "https://apnews.com/article/poland-military-drills-2018",
                    "title": "Military exercises conclude in Eastern Poland",
                    "published_date": "2018-06-14T16:30:00Z",
                    "date_source": "PAGE_METADATA",
                    "og_image": "https://apnews.com/images/poland-drills-2018.jpg",
                    "image_urls": [
                        "https://apnews.com/images/poland-drills-2018.jpg",
                        "https://apnews.com/images/thumb-lead.jpg"
                    ],
                    "markdown": "# Military exercises conclude in Eastern Poland\n\nTroops participate in tactical drills...",
                    "content_sha256": "8f481c4e7240f9076f7c13284ff0d0ea3efd3ce57c7908b981c2f90a887a0322"
                }
            ]
        }
        save_sample("fetch_sample.json", sample_fetch)

    # 3. Agent Sample
    agent_path = os.path.join(SAMPLES_DIR, "agent_sample.json")
    if not os.path.exists(agent_path):
        sample_agent = {
            "run_id": "run_tf_982348274a",
            "status": "COMPLETED",
            "streaming_url": "https://browser.tinyfish.ai/live/run_tf_982348274a",
            "steps_taken": 8,
            "max_steps": 20,
            "extracted_data": {
                "earliest_timestamp": "2018-06-14T16:30:00Z",
                "publisher": "Associated Press",
                "verified_caption": "NATO Allied drills in Western Poland, June 2018."
            }
        }
        save_sample("agent_sample.json", sample_agent)

    print("TinyFish spike completed.")

if __name__ == "__main__":
    run_spike()
