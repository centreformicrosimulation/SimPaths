"""Guard the native annual inspector against Markdown-in-HTML misnesting."""
from html.parser import HTMLParser
from pathlib import Path
import unittest

import markdown
from mkdocs.config import load_config


ROOT = Path(__file__).resolve().parents[2]


class InspectorParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.stack = []
        self.stages = []

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if "annual-explorer__stage" in attrs.get("class", "").split():
            self.stages.append((attrs.get("id"), self.stack[-1] if self.stack else None))
        if tag not in {"area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr"}:
            self.stack.append((tag, attrs.get("class", "")))

    def handle_endtag(self, tag):
        for index in range(len(self.stack) - 1, -1, -1):
            if self.stack[index][0] == tag:
                del self.stack[index:]
                break


class AnnualCycleRenderTests(unittest.TestCase):
    def test_all_modules_render_as_sibling_disclosures(self):
        config = load_config(str(ROOT / "mkdocs.yml"))
        source = (ROOT / "documentation/wiki/overview/index.md").read_text()
        rendered = markdown.markdown(
            source,
            extensions=config.markdown_extensions,
            extension_configs=config.mdx_configs,
        )
        parser = InspectorParser()
        parser.feed(rendered)
        self.assertEqual([stage[0] for stage in parser.stages], [
            "cycle-ageing", "cycle-education", "cycle-health", "cycle-family",
            "cycle-care", "cycle-investment", "cycle-employment",
            "cycle-consumption", "cycle-mental-health",
        ])
        for _, parent in parser.stages:
            self.assertEqual(parent, ("div", "annual-explorer__stages"))
        self.assertNotIn('&lt;details class=', rendered)


if __name__ == "__main__":
    unittest.main()
