"""Build-time channel metadata; ordinary local previews remain usable."""
import json
import os
import subprocess
from pathlib import Path

def on_config(config):
    channel = os.environ.get("DOCS_CHANNEL", "main")
    if channel not in ("main", "develop"):
        raise ValueError("DOCS_CHANNEL must be main or develop")
    sha = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
    tags = subprocess.check_output(["git", "tag", "--points-at", "HEAD"], text=True).splitlines()
    config.site_url = "https://simpaths.org/" + ("develop/" if channel == "develop" else "")
    config.extra["docs_version"] = dict(channel=channel, sha=sha, tags=tags)
    return config

def on_post_build(config):
    root = Path(config.site_dir)
    pages = sorted(str(p.relative_to(root)).removesuffix("index.html") for p in root.rglob("index.html"))
    (root / "version.json").write_text(json.dumps({**config.extra["docs_version"], "pages": pages}))
    if config.extra["docs_version"]["channel"] == "develop":
        (root / "CNAME").unlink(missing_ok=True)
