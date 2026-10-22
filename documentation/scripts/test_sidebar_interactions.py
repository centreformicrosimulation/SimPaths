"""Source contracts for restrained Documentation sidebar interactions."""

from pathlib import Path
from html.parser import HTMLParser
import unittest

from test_site_typography import blocks


CSS = Path(__file__).resolve().parents[1] / "wiki/assets/css/02-shell-navigation.css"
SIDEBAR = "body.sp-docs-navigation .md-sidebar--primary"
LEAF = SIDEBAR + " .md-nav__item:not(.md-nav__item--nested) > a.md-nav__link"
SECTION = (SIDEBAR + " .md-nav--primary .md-nav__list > .md-nav__item--nested"
           " > .md-nav__container:not(.sp-nav-container-active)")
HOME = (SIDEBAR + " .md-nav--primary > .md-nav__list"
        " > .md-nav__item--active.md-nav__item--section > .md-nav__container")


class SidebarInteractionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = CSS.read_text()

    def test_inactive_link_gets_a_flat_neutral_surface(self):
        for state in ("hover", "focus-visible"):
            rules = blocks(self.source, LEAF + f":not(.md-nav__link--active):{state}")
            self.assertEqual(rules, [{"color": "var(--sp-sidebar-hover-ink) !important",
                                     "background": "var(--sp-sidebar-hover-bg) !important"}])

    def test_section_row_shares_the_same_surface_without_a_shadow(self):
        for state in ("hover", "focus-within"):
            self.assertEqual(blocks(self.source, SECTION + f":{state}"),
                             [{"background": "var(--sp-sidebar-hover-bg) !important"}])
            link = SECTION + f":{state} > a.md-nav__link:not(.md-nav__link--active)"
            self.assertEqual(blocks(self.source, link),
                             [{"color": "var(--sp-sidebar-hover-ink) !important"}])
        self.assertEqual(blocks(self.source, SIDEBAR + " .md-nav__container")[0]["box-shadow"],
                         "none !important")

    def test_section_links_keep_their_heading_weight(self):
        for level, item, weight in ((1, "section", "580"), (2, "nested", "540")):
            selector = (SIDEBAR + f' .md-nav[data-md-level="{level}"] > .md-nav__list'
                        f" > .md-nav__item--{item} > .md-nav__container > a.md-nav__link")
            self.assertEqual(blocks(self.source, selector)[0]["font-weight"], weight)

    def test_transitions_only_fade_colours_and_respect_reduced_motion(self):
        for element, transition in ((".md-nav__link", "color 120ms ease, background-color 120ms ease"),
                                    (".md-nav__container", "background-color 120ms ease")):
            rules = blocks(self.source, SIDEBAR + " " + element)
            self.assertEqual(rules[0]["transition"], transition)
            self.assertEqual(rules[-1]["transition"].replace("!important", "").strip(), "none")
        self.assertIn("@media (prefers-reduced-motion: reduce)", self.source)

    def test_current_page_keeps_its_marker(self):
        rule = blocks(self.source, LEAF + "--active")[0]
        self.assertEqual(rule["background"], "var(--sp-sidebar-active) !important")
        self.assertEqual(rule["border-left"], "2px solid var(--sp-accent)")
        self.assertEqual(rule["font-weight"], "600")

    def test_documentation_home_has_a_distinct_flat_surface(self):
        self.assertEqual(blocks(self.source, HOME), [{
            "--sp-sidebar-hover-bg": "var(--sp-sidebar-home-hover)",
            "background": "var(--sp-sidebar-home-bg) !important",
            "border": "1px solid var(--sp-sidebar-home-border)",
            "border-radius": "4px !important", "box-shadow": "none !important"
        }])
        link = HOME + " > a.md-nav__link"
        self.assertEqual(blocks(self.source, link)[-1]["font-weight"], "560")
        mark = blocks(self.source, link + "::before")[0]
        self.assertEqual(mark["background"],
                         'url("../images/homepage-hero-logo.svg") center / contain no-repeat')
        self.assertEqual(mark["width"], "1.4rem")
        self.assertEqual(mark["height"], "0.85rem")
        self.assertEqual(mark["content"], '\"\"')
        template = CSS.parents[3] / "overrides/main.html"
        self.assertIn("assets/images/homepage-hero-logo.svg", template.read_text())

    def test_documentation_home_has_both_theme_palettes(self):
        dark = '[data-md-color-scheme="slate"].sp-docs-navigation .md-sidebar--primary'
        for selector, colours in ((SIDEBAR, ("#fff", "#d9dce0", "#fff")),
                                  (dark, ("var(--sp-surface)", "var(--sp-border-strong)", "var(--sp-surface-strong)"))):
            rule = blocks(self.source, selector)[0]
            for role, value in zip(("bg", "border", "hover"), colours):
                self.assertEqual(rule["--sp-sidebar-home-" + role], value)

    def test_filter_and_navigation_share_one_width(self):
        script = (CSS.parent.parent / "js/sidebar-filter.js").read_text()
        self.assertIn('sidebarPanel.querySelector(".md-sidebar__inner")', script.replace("?.", "."))
        self.assertIn("sidebar.prepend(tools);", script)
        self.assertNotIn("sidebarPanel.prepend(tools);", script)
        self.assertIn('sidebarPanel.querySelector(".sp-sidebar-tools")?.remove();', script)
        expected = {
            " .md-sidebar__inner": {"padding": "0"},
            " .md-nav--primary .md-nav__list": {"padding-inline": "0"},
            " .md-nav--primary .md-nav__list > .md-nav__item > .md-nav": {"margin-inline": "0"},
            " .md-nav--primary .md-nav__item > .md-nav__link": {"margin-inline": "0"},
        }
        for suffix, declarations in expected.items():
            self.assertEqual(blocks(self.source, SIDEBAR + suffix), [declarations])

    def test_nested_text_is_indented_without_narrowing_the_row(self):
        nested = SIDEBAR + ' .md-nav[data-md-level="3"]'
        self.assertEqual(blocks(self.source, nested), [{"--sp-sidebar-link-indent": "0.84rem"}])
        self.assertIn("var(--sp-sidebar-link-indent)",
                      blocks(self.source, SIDEBAR + " .md-nav__link")[0]["padding"])
        self.assertEqual(blocks(self.source, LEAF + "--active")[0]["padding-left"],
                         "calc(var(--sp-sidebar-link-indent) - 2px)")

    def test_filter_stays_opaque_and_sticky_above_scrolled_links(self):
        rule = blocks(self.source, SIDEBAR + " .sp-sidebar-tools")[0]
        self.assertEqual(rule, {"position": "sticky", "top": "0", "z-index": "4",
                                "background": "var(--md-default-bg-color)"})
        scrollwrap = blocks(self.source, SIDEBAR + " .md-sidebar__scrollwrap")
        self.assertEqual(scrollwrap[-1]["scroll-padding-top"], "3.2rem")

    def test_keyboard_focus_remains_visible_and_does_not_shift_layout(self):
        for element in ("a", "label"):
            rule = blocks(self.source, SIDEBAR + f" {element}.md-nav__link:focus-visible")[0]
            self.assertEqual(rule, {"outline": "2px solid var(--sp-sidebar-hover-ink)",
                                    "outline-offset": "-2px"})

    def test_neutral_hover_has_a_dark_theme_variant(self):
        light_rule = blocks(self.source, SIDEBAR)[0]
        self.assertEqual(light_rule["--sp-sidebar-hover-ink"], "#1a1d21")
        self.assertEqual(light_rule["--sp-sidebar-hover-bg"], "#eceef0")
        dark = '[data-md-color-scheme="slate"].sp-docs-navigation .md-sidebar--primary'
        dark_rule = blocks(self.source, dark)[0]
        self.assertEqual(dark_rule["--sp-sidebar-hover-ink"], "var(--md-default-fg-color)")
        self.assertEqual(dark_rule["--sp-sidebar-hover-bg"], "var(--sp-sidebar-hover)")

    def test_header_drawer_and_search_are_named_native_buttons(self):
        class Controls(HTMLParser):
            def __init__(self):
                super().__init__()
                self.controls = {}

            def handle_starttag(self, tag, attrs):
                attrs = dict(attrs)
                if "data-sp-toggle" in attrs:
                    self.controls[attrs["data-sp-toggle"]] = (tag, attrs)

        parser = Controls()
        parser.feed((CSS.parents[3] / "overrides/partials/header.html").read_text())
        self.assertEqual(set(parser.controls), {"drawer", "search"})
        for name, controlled in (("drawer", "sp-site-navigation"), ("search", "sp-site-search")):
            tag, attrs = parser.controls[name]
            self.assertEqual(tag, "button")
            self.assertEqual(attrs["type"], "button")
            self.assertEqual(attrs["aria-controls"], controlled)
            self.assertEqual(attrs["aria-expanded"], "false")
            self.assertTrue(attrs["aria-label"])

    def test_mobile_parent_title_cannot_cover_nested_back_controls(self):
        # A global z-index on this title would paint it over Material's z=1
        # mobile subpanels, making the nested Back control unreachable.
        general = blocks(self.source, ".md-nav__title")[0]
        self.assertNotIn("z-index", general)
        self.assertNotIn("position", general)
        self.assertNotIn("box-shadow", general)
        self.assertIn("@media screen and (min-width: 76.25em)", self.source)


if __name__ == "__main__":
    unittest.main()
