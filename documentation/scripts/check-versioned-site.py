"""Assert channel isolation before uploading the combined Pages artifact."""
import json
import sys
from pathlib import Path
from html.parser import HTMLParser
root = Path(sys.argv[1])
for channel, folder in (("main", root), ("develop", root / "develop")):
    manifest = json.loads((folder / "version.json").read_text())
    assert manifest["channel"] == channel
    assert len(manifest["sha"]) == 40
    assert (folder / "api/index.html").is_file()
    assert (folder / "search/search_index.json").is_file()
    for page in manifest["pages"]:
        html = (folder / page / "index.html").read_text()
        if "window.location.replace" in html:
            assert f'href="https://simpaths.org/{"develop/" if channel == "develop" else ""}overview/"' in html
            continue
        assert f'data-docs-channel="{channel}"' in html, page
        assert ('name="robots" content="noindex, nofollow"' in html) == (channel == "develop"), page
        prefix = "https://simpaths.org/" + ("develop/" if channel == "develop" else "")
        assert f'href="{prefix}{page}"' in html, page
assert not (root / "develop/CNAME").exists()
print("Both documentation channels, canonical URLs, indexing rules, search and APIs verified.")
