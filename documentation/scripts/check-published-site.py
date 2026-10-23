"""Check the single public website before uploading to Pages."""
import sys
from pathlib import Path
root = Path(sys.argv[1])
assert not (root / "develop").exists(), "Develop must not be published"
assert not (root / "version.json").exists(), "No public version manifest"
for path in ("api/index.html", "search/search_index.json", "index.html"):
    assert (root / path).is_file(), path
for page in root.rglob("*.html"):
    if "api" in page.relative_to(root).parts:
        continue
    html = page.read_text()
    assert "data-version-target" not in html, page
    assert 'class="sp-version"' not in html, page
    assert "https://simpaths.org/develop/" not in html, page
print("Single public site, API and search verified; no develop site or version controls.")
