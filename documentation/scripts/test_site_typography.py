"""Source-level contracts for the approved site-wide typography (not visual QA)."""

from pathlib import Path
import re
import unittest


CSS_DIR = Path(__file__).resolve().parents[1] / "wiki" / "assets" / "css"


def blocks(source, selector):
    """Read leaf declaration blocks for a simple selector, including media rules."""
    source = re.sub(r"/\*.*?\*/", "", source, flags=re.S)
    found = []
    for selectors, body in re.findall(r"([^{}]+)\{([^{}]*)\}", source):
        names = [" ".join(name.split()) for name in selectors.split(",")]
        if selector in names:
            found.append(dict(re.findall(r"([\w-]+)\s*:\s*([^;]+);", body)))
    return found


class SiteTypographyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.styles = {path.name: path.read_text() for path in CSS_DIR.glob("*.css")}

    def test_approved_scale_is_defined_once_in_global_foundation(self):
        expected = {
            "--sp-title-size": "1.8rem", "--sp-title-weight": "560",
            "--sp-section-size": "1.16rem", "--sp-section-weight": "520",
            "--sp-subheading-size": "0.95rem", "--sp-subheading-weight": "560",
            "--sp-reading-copy-size": "0.82rem", "--sp-reading-line-height": "1.75",
            "--sp-reading-measure": "42rem", "--sp-section-gap": "2.1rem",
            "--sp-subheading-gap": "1.35rem", "--sp-label-size": "0.7rem",
            "--sp-label-weight": "550", "--sp-meta-size": "0.62rem",
            "--sp-nav-size": "0.675rem", "--sp-toc-size": "0.67rem",
        }
        roots = blocks(self.styles["01-foundation.css"], ":root")
        for token, value in expected.items():
            with self.subTest(token=token):
                definitions = [(name, match) for name, source in self.styles.items()
                               for match in re.findall(re.escape(token) + r"\s*:\s*([^;]+);", source)]
                self.assertEqual(definitions, [("01-foundation.css", value)])
                self.assertTrue(any(root.get(token) == value for root in roots))

    def assert_uses(self, filename, selector, property_name, token):
        rules = blocks(self.styles[filename], selector)
        self.assertTrue(rules, f"Missing selector: {selector}")
        declarations = [rule[property_name] for rule in rules if property_name in rule]
        self.assertTrue(declarations, f"Missing {property_name}: {selector}")
        self.assertEqual(set(declarations), {f"var({token})"})

    def test_base_heading_scale_is_not_limited_to_selected_routes(self):
        for level, role in ((1, "title"), (2, "section"), (3, "subheading")):
            self.assert_uses("03-content.css", f".md-typeset h{level}",
                             "font-size", f"--sp-{role}-size")
        for level, role in ((1, "title"), (2, "section")):
            self.assert_uses("03-content.css", f".md-typeset h{level}",
                             "font-weight", f"--sp-{role}-weight")

    def test_page_specific_headings_do_not_restore_heavy_weights(self):
        headings = {
            "04-landing-components.css": [".md-typeset .docs-hub--index h1", ".md-typeset .docs-hub--index h2"],
            "06-page-sections.css": [".md-typeset .funding-panel h2", ".md-typeset .cite-section > h2",
                                     ".md-typeset .module-detail > h1", ".md-typeset .module-detail > h2"],
            "07-roadmap.css": [".md-typeset .roadmap-stage__heading h2", ".md-typeset .roadmap-item h3",
                               ".md-typeset .roadmap-contact h2"],
            "08-home.css": [".md-typeset .simpaths-home-paths__header h2",
                            ".md-typeset .simpaths-home-features dt"],
        }
        for filename, selectors in headings.items():
            for selector in selectors:
                with self.subTest(selector=selector):
                    rules = blocks(self.styles[filename], selector)
                    self.assertTrue(rules)
                    for rule in rules:
                        if "font-weight" in rule:
                            pattern = (r"^(var\(--sp-home-section-weight\)|calc\(var\(--sp-subheading-weight\) - 20\))$"
                                       if filename == "08-home.css" else
                                       r"^var\(--sp-(title|section|subheading)-weight\)$")
                            self.assertRegex(rule["font-weight"], pattern)

    def test_main_prose_uses_the_same_scale_including_mobile(self):
        prose = {
            "03-content.css": [".md-typeset .page-intro", ".md-typeset .page-intro--support"],
            "06-page-sections.css": [".md-typeset .funding-lead", ".md-typeset .research-page__intro",
                                     ".md-typeset .cite-entry",
                                     ".md-typeset .module-detail__lead"],
            "07-roadmap.css": [".md-typeset .roadmap-lede", ".md-typeset .roadmap-stage__heading p",
                               ".md-typeset .roadmap-item > p", ".md-typeset .roadmap-contact p"],
            "08-home.css": [".md-typeset .simpaths-home-intro-band__lede",
                            ".md-typeset .simpaths-home-features dd"],
        }
        for filename, selectors in prose.items():
            for selector in selectors:
                with self.subTest(selector=selector):
                    self.assert_uses(filename, selector, "font-size", "--sp-reading-copy-size")
                    self.assert_uses(filename, selector, "line-height", "--sp-reading-line-height")

    def test_homepage_intro_keeps_the_lighter_reading_weight(self):
        source = self.styles["08-home.css"]
        weights = [rule["font-weight"]
                   for rule in blocks(source, ".md-typeset .simpaths-home-intro-band__lede")
                   if "font-weight" in rule]
        self.assertTrue(weights)
        self.assertEqual(set(weights), {"450"})

    def test_documentation_description_keeps_its_original_compact_scale(self):
        rules = blocks(self.styles["04-landing-components.css"], ".md-typeset .docs-index__intro")
        self.assertTrue(rules)
        self.assertEqual({rule["font-size"] for rule in rules if "font-size" in rule}, {"0.72rem"})
        self.assertEqual({rule["line-height"] for rule in rules if "line-height" in rule}, {"1.6"})

    def test_navigation_and_reading_flow_use_shared_rules(self):
        self.assert_uses("02-shell-navigation.css", ".md-nav__link", "font-size", "--sp-nav-size")
        self.assert_uses("02-shell-navigation.css", ".md-sidebar--secondary .sp-toc .md-nav__link",
                         "font-size", "--sp-toc-size")
        flow = blocks(self.styles["03-content.css"], ".md-content .md-typeset")
        self.assertEqual(flow[0]["text-align"], "left")
        self.assertEqual(flow[0]["hyphens"], "none")
        self.assertEqual(flow[0]["line-height"], "var(--sp-reading-line-height)")

    def test_footer_description_keeps_sentence_case_and_normal_spacing(self):
        rules = blocks(self.styles["05-site-chrome.css"], ".md-copyright__highlight")
        self.assertTrue(rules)
        for rule in rules:
            self.assertEqual(rule["text-transform"], "none")
            self.assertEqual(rule["letter-spacing"], "normal")
        config = (CSS_DIR.parents[3] / "mkdocs.yml").read_text()
        self.assertIn("An open-source microsimulation initiative.", config)

    def test_documentation_hub_hides_toc(self):
        docs_dir = CSS_DIR.parents[2]
        landing = (docs_dir / "wiki/documentation/index.md").read_text()
        self.assertIn("hide:\n  - toc\n", landing.split("---", 2)[1])


if __name__ == "__main__":
    unittest.main()
