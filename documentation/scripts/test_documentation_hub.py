"""Source contracts for the documentation directory's sections and destinations."""

from pathlib import Path
import re
import unittest

from test_site_typography import blocks


class DocumentationHubTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.css = (Path(__file__).resolve().parents[1] / "wiki/assets/css/04-landing-components.css").read_text()

    def test_masthead_keeps_the_mark_next_to_the_title(self):
        mark = blocks(self.css, ".md-typeset .docs-index__mark")
        heading = blocks(self.css, ".md-typeset .docs-index__heading")
        self.assertEqual(heading[0]["display"], "flex")
        self.assertEqual(heading[0]["gap"], "0.6rem")
        self.assertEqual(mark[0]["flex-shrink"], "0")

    def test_logo_reserves_a_compact_white_frame_without_a_dark_mode_image_swap(self):
        mark = blocks(self.css, ".md-typeset .docs-index__mark")
        self.assertEqual(mark[0]["background"], "#fff")
        self.assertEqual(mark[0]["height"], "2.34rem")
        self.assertEqual(mark[0]["padding"], "0.1rem 0.3rem")
        self.assertEqual(mark[0]["border-radius"], "4px")
        self.assertTrue(any(rule.get("width") == "2.5rem" for rule in mark))
        self.assertTrue(any(rule.get("padding-inline") == "0.1rem" for rule in mark))
        self.assertNotIn("aspect-ratio", mark[0])
        dark_prefix = '[data-md-color-scheme="slate"] .md-typeset .docs-index__mark'
        for suffix in ("", " svg.docs-index__mark-image--light", " svg.docs-index__mark-image--dark"):
            self.assertFalse(blocks(self.css, dark_prefix + suffix))

    def test_links_use_only_arrow_motion_without_highlight_fills(self):
        self.assertNotIn("--docs-panel-hover", self.css)
        self.assertNotIn("background-color 180ms", self.css)
        self.assertNotIn("text-decoration: underline", self.css)
        for selectors, declarations in re.findall(r"([^{}]+)\{([^{}]*)\}", self.css):
            if any(f":{state}" in selectors for state in ("hover", "focus-visible", "active")):
                self.assertNotRegex(declarations, r"background(?:-color)?\s*:")
        motion = self.css.split("@media (prefers-reduced-motion: reduce)", 1)[1]
        link = ".md-typeset .docs-hub--index .docs-index__section a.docs-index__link"
        self.assertEqual(blocks(self.css, link)[0]["transition"], "none")
        self.assertEqual(blocks(motion, ".md-typeset .docs-index__arrow")[0]["transition"], "none")
        for state in ("hover", "focus-visible", "active"):
            selector = f".md-typeset .docs-index__link:{state} .docs-index__arrow"
            self.assertEqual(blocks(self.css, selector)[0], {"transform": "translateX(0.18rem)"})
            self.assertEqual(blocks(motion, selector)[0]["transform"], "none")

    def test_links_are_grouped_by_purpose_without_losing_destinations(self):
        source = (Path(__file__).resolve().parents[1] / "wiki/documentation/index.md").read_text()
        groups = dict(re.findall(r'<section[^>]+aria-labelledby="([^"]+)"[^>]*>(.*?)</section>', source, re.S))
        expected = {
            "guides": ["../getting-started/", "../user-guide/", "../developer-guide/",
                       "../developer-guide/how-to/"],
            "resources": ["../getting-started/video-tutorials/", "https://github.com/simpaths/SimPaths"],
            "reference": ["../jasmine-reference/", "../developer-guide/internals/api/"],
        }
        self.assertEqual(set(groups), set(expected))
        for name, destinations in expected.items():
            with self.subTest(section=name):
                self.assertIn(f'<h2 id="{name}">{name.title()}</h2>', groups[name])
                self.assertEqual(re.findall(r'href="([^"]+)"', groups[name]), destinations)
                self.assertEqual(groups[name].count('class="docs-index__link"'), len(destinations))
                self.assertEqual(groups[name].count('class="docs-index__description"'), len(destinations))
                self.assertEqual(groups[name].count('class="docs-index__arrow" aria-hidden="true"'), len(destinations))
        self.assertNotIn("docs-index__card", source)


if __name__ == "__main__":
    unittest.main()
