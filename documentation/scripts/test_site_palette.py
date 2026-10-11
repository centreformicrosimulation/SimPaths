"""Source-level contracts for the pristine palette preview; these are not browser tests."""

from pathlib import Path
import re
import unittest

from test_site_typography import blocks


CSS_DIR = Path(__file__).resolve().parents[1] / "wiki" / "assets" / "css"


def contrast_ratio(first, second):
    def luminance(value):
        if len(value) == 4:
            value = "#" + "".join(channel * 2 for channel in value[1:])
        channels = [int(value[i:i + 2], 16) / 255 for i in (1, 3, 5)]
        linear = [c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in channels]
        return sum(c * weight for c, weight in zip(linear, (0.2126, 0.7152, 0.0722)))
    low, high = sorted((luminance(first), luminance(second)))
    return (high + 0.05) / (low + 0.05)


class SitePaletteTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.styles = {path.name: path.read_text() for path in CSS_DIR.glob("*.css")}
        cls.tokens = {key: value for rule in blocks(cls.styles["01-foundation.css"], ":root")
                      for key, value in rule.items()}

    def rule(self, filename, selector):
        found = blocks(self.styles[filename], selector)
        self.assertTrue(found, f"Missing selector: {selector}")
        return {key: value for rule in found for key, value in rule.items()}

    def test_pristine_light_palette_roles(self):
        expected = {
            "--sp-paper": "#FFFFFF", "--sp-midnight": "#343134",
            "--sp-accent": "#972A6C", "--sp-ghost": "#D5D2DA",
            "--sp-surface": "#FFFFFF",
            "--sp-surface-strong": "#FFFFFF",
        }
        for token, value in expected.items():
            self.assertEqual(self.tokens[token], value)
        for token in ("--sp-ink", "--sp-white", "--sp-stone", "--sp-tray", "--sp-muted"):
            self.assertNotIn(token, self.tokens)
        light = self.rule("01-foundation.css", '[data-md-color-scheme="default"]')
        self.assertEqual(light["--md-default-bg-color"], "var(--sp-paper)")
        self.assertEqual(light["--md-default-bg-color--light"], "#F6F6F8")
        self.assertEqual(light["--md-default-fg-color"], "#343134")

    def test_homepage_directory_and_research_bands_keep_the_approved_palette(self):
        expected = {
            ".simpaths-home-hero": "var(--sp-home-hero-bg)",
            ".simpaths-home-intro-band": "var(--sp-paper)",
            ".simpaths-home-paths": "#E8F0F5",
            ".simpaths-home-research-band": "#F7F7F9",
            ".md-typeset .simpaths-home-research-band a.research-entry": "#fff",
        }
        for selector, colour in expected.items():
            with self.subTest(selector=selector):
                self.assertEqual(self.rule("08-home.css", selector)["background"], colour)
        self.assertEqual(self.tokens["--sp-home-hero-bg"], "#27232D")

    def test_first_run_steps_have_no_card_frame_or_shadow(self):
        filename = "08-home.css"
        card = self.rule(filename, ".md-typeset .simpaths-first-run__steps a")
        frame = self.rule(filename, ".md-typeset .simpaths-first-run")
        self.assertNotIn("border", card)
        self.assertNotIn("box-shadow", frame)
        self.assertNotIn("border-radius", frame)
        self.assertNotIn("background", frame)

    def test_homepage_band_labels_follow_the_new_backgrounds(self):
        filename = "08-home.css"
        self.assertEqual(self.rule(filename, ".md-typeset .simpaths-home-paths__header h2")["color"], "var(--md-default-fg-color)")
        self.assertEqual(self.rule(filename, ".md-typeset .simpaths-home-research-band .research-header .section-heading")["color"], "#343134")
        self.assertEqual(self.rule(filename, ".md-typeset .simpaths-home-research-band .archive-link")["color"], "#343134 !important")

    def test_footer_uses_graphite_and_reserves_white_for_the_brand(self):
        filename = "05-site-chrome.css"
        footer = self.rule(filename, ".md-footer")
        self.assertEqual(footer["background"], "#27232D")
        self.assertEqual(footer["--sp-footer-muted"], "#C6C2CD")
        for selector in (".md-footer", ".md-footer-meta", ".md-copyright",
                         ".md-copyright__highlight", ".md-social__link::after",
                         ".md-social__link:hover::after"):
            with self.subTest(selector=selector):
                self.assertEqual(self.rule(filename, selector)["color"], "var(--sp-footer-muted)")
                self.assertFalse(blocks(self.styles[filename], f'[data-md-color-scheme="slate"] {selector}'))
        self.assertEqual(self.rule(filename, ".md-copyright .footer-brand")["color"], "#fff !important")
        self.assertEqual(self.rule(filename, ".md-footer-meta")["background"], "transparent")
        self.assertNotIn(".md-footer__inner", self.styles[filename])
        self.assertNotIn("visibility: hidden", self.styles[filename].split("/* ── Previous/Next", 1)[0])
        focus = ".md-footer .md-social__link:focus-visible"
        self.assertIn(f"{focus} {{\n  outline: 2px solid #fff;", self.styles[filename])

        def luminance(hex_colour):
            channels = [int(hex_colour[i:i + 2], 16) / 255 for i in (1, 3, 5)]
            linear = [c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in channels]
            return sum(c * weight for c, weight in zip(linear, (0.2126, 0.7152, 0.0722)))

        contrast = (luminance(footer["--sp-footer-muted"]) + 0.05) / (luminance(footer["background"]) + 0.05)
        self.assertGreaterEqual(contrast, 4.5)

    def test_homepage_prose_follows_the_theme_while_light_panel_metadata_stays_muted(self):
        filename = "08-home.css"
        for selector, colour in {
            ".md-typeset .simpaths-home-intro-band__lede": "var(--sp-midnight)",
            ".md-typeset .simpaths-home-intro-band__body": "var(--sp-midnight)",
            ".md-typeset .simpaths-home-features dd": "var(--sp-midnight)",
        }.items():
            with self.subTest(selector=selector):
                rule = self.rule(filename, selector)
                self.assertEqual(rule["color"], colour)
                self.assertNotIn("opacity", rule)
        for selector, colour in {
            ".md-typeset .simpaths-home-research-band .research-journal": "#64616B",
            ".md-typeset .simpaths-home-research-band .research-authors": "#64616B",
        }.items():
            self.assertEqual(self.rule(filename, selector)["color"], colour)

    def test_documentation_intro_and_funding_retain_original_treatments(self):
        self.assertEqual(self.rule("04-landing-components.css", ".md-typeset .docs-index__intro")["color"],
                         "rgba(31, 38, 48, 0.72)")
        funding = self.rule("06-page-sections.css", ".md-typeset .funding-page")
        self.assertEqual(funding["--funding-rule"], "#E1DFE5")
        self.assertEqual(funding["--funding-copy"], "#242a31")
        self.assertEqual(funding["--funding-meta"], "#64616B")
        self.assertEqual(funding["--funding-label-bg"], "#F4F4F6")

    def test_documentation_sections_have_solid_surfaces_without_individual_card_frames(self):
        filename = "04-landing-components.css"
        for section, colour in {"guides": "#C8DDE8", "resources": "#E8BEAD", "reference": "#725B73"}.items():
            selector = f".md-typeset .docs-index__section--{section}"
            self.assertEqual(self.rule(filename, selector)["--docs-panel-background"], colour)
        self.assertEqual(self.rule(filename, ".md-typeset .docs-index__section")["background"],
                         "var(--docs-panel-background)")
        self.assertEqual(self.rule(filename, ".md-typeset .docs-index__section")["border-radius"], "6px")
        link = self.rule(filename, ".md-typeset .docs-hub--index .docs-index__section a.docs-index__link")
        self.assertEqual(link["background"], "transparent")
        self.assertEqual(link["border"], "0 !important")
        self.assertNotIn("docs-card-", self.styles[filename])
        self.assertNotIn("color-mix", self.styles[filename])
        self.assertEqual(self.rule(filename, ".md-typeset .docs-index__link:focus-visible")["outline"],
                         "2px solid var(--docs-panel-ink)")

    def test_documentation_panel_text_and_focus_have_contrast(self):
        def rgb(value):
            if len(value) == 4:
                value = "#" + "".join(channel * 2 for channel in value[1:])
            return tuple(int(value[i:i + 2], 16) / 255 for i in (1, 3, 5))

        def luminance(colour):
            linear = [c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in colour]
            return sum(c * weight for c, weight in zip(linear, (0.2126, 0.7152, 0.0722)))

        def contrast(first, second):
            low, high = sorted((luminance(first), luminance(second)))
            return (high + 0.05) / (low + 0.05)

        tokens = self.rule("04-landing-components.css", ".md-typeset .docs-hub--index")
        dark = self.rule("04-landing-components.css", '[data-md-color-scheme="slate"] .md-typeset .docs-hub--index')
        for section in ("guides", "resources", "reference"):
            panel = self.rule("04-landing-components.css", f".md-typeset .docs-index__section--{section}")
            colours = {**tokens, **panel}
            surface = rgb(panel["--docs-panel-background"])
            with self.subTest(section=section):
                for role in ("--docs-panel-ink", "--docs-panel-copy"):
                    self.assertNotIn(role, dark)
                    self.assertGreaterEqual(contrast(rgb(colours[role]), surface), 4.5)
                self.assertGreaterEqual(contrast(rgb(colours["--docs-panel-ink"]), surface), 3)

    def test_reading_tables_use_the_page_surface_instead_of_a_separate_palette(self):
        for selector in (".md-typeset table:not([class])", ".md-typeset table:not([class]) th"):
            rule = self.rule("03-content.css", selector)
            self.assertEqual(rule["background"], "transparent")
        self.assertEqual(self.rule("03-content.css", ".md-typeset table:not([class])")["color"], "inherit")
        self.assertEqual(self.rule("03-content.css", ".md-typeset table:not([class]) th")["color"],
                         "inherit")

    def test_funder_and_research_accents_are_preserved(self):
        expected = {
            "nihr": "#0051c2", "horizon-europe": "#003399", "phi": "#f0d764",
            "chanse-norface": "#42bccd", "inapp": "#18376e", "health-foundation": "#de0031",
            "jpi": "#3c76bb", "erc": "#ff7d00", "espon": "#63b9ea",
        }
        for funder, colour in expected.items():
            selector = f'.md-typeset .funding-entry[data-funder="{funder}"]'
            self.assertEqual(self.rule("06-page-sections.css", selector)["--funding-brand"], colour)
        for colour in ("#c62e67", "#7040a3", "#1f70aa"):
            self.assertIn(f"--research-accent: {colour};", self.styles["08-home.css"])

    def test_white_navigation_search_and_retained_syntax_colours(self):
        self.assertEqual(self.rule("02-shell-navigation.css", ".md-tabs")["background"],
                         "var(--md-default-bg-color) !important")
        self.assertIn("--sp-search-surface: #fff;", self.styles["05-site-chrome.css"])
        self.assertIn("--sp-code-bg: #ffffff;", self.styles["03-content.css"])

    def test_dark_palette_uses_semantic_ink_and_preserves_light_panel_ink(self):
        dark = self.rule("01-foundation.css", '[data-md-color-scheme="slate"]')
        self.assertEqual(dark["--md-default-fg-color"], "var(--sp-midnight)")
        for token in ("--sp-panel-ink", "--sp-panel-muted", "--sp-panel-link", "--sp-panel-link-hover"):
            self.assertNotIn(token, dark)
            self.assertGreaterEqual(contrast_ratio(self.tokens[token], "#FFFFFF"), 4.5)
        self.assertNotIn('[data-md-color-scheme="slate"] .md-typeset a {', self.styles["01-foundation.css"])

    def test_dark_header_navigation_links_and_roadmap_have_readable_contrast(self):
        tokens = {**self.tokens, **self.rule("01-foundation.css", '[data-md-color-scheme="slate"]')}
        tokens.update(self.rule("07-roadmap.css", ".md-typeset .roadmap-page"))

        def resolve(value):
            value = value.replace(" !important", "")
            while value.startswith("var("):
                value = tokens[re.fullmatch(r"var\(([^)]+)\)", value).group(1)]
            return value

        checks = [
            ("02-shell-navigation.css", ".md-header__title", "color"),
            ("02-shell-navigation.css", ".md-tabs__link", "color"),
            ("01-foundation.css", ".md-typeset a:not(.md-button):not(.headerlink):not(.md-nav__link)", "color"),
            ("07-roadmap.css", ".md-typeset .roadmap-lede", "color"),
            ("07-roadmap.css", ".md-typeset .roadmap-status", "color"),
            ("07-roadmap.css", ".md-typeset .roadmap-item > p", "color"),
        ]
        surface = resolve(tokens["--md-default-bg-color"])
        for filename, selector, property_name in checks:
            with self.subTest(selector=selector):
                foreground = resolve(self.rule(filename, selector)[property_name])
                self.assertGreaterEqual(contrast_ratio(foreground, surface), 4.5)


if __name__ == "__main__":
    unittest.main()
