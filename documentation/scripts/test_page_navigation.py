"""Template and source contracts for the in-article pager and compact footer."""

from pathlib import Path
from types import SimpleNamespace
import unittest

from jinja2 import Environment, FileSystemLoader, select_autoescape
import yaml

from test_site_typography import blocks


DOCS = Path(__file__).resolve().parents[1]


class PageNavigationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        env = Environment(loader=FileSystemLoader(DOCS / "overrides"),
                          autoescape=select_autoescape(["html"]))
        env.filters["url"] = lambda path: "/preview/" + path
        cls.template = env.get_template("partials/page-navigation.html")
        cls.css = (DOCS / "wiki/assets/css/05-site-chrome.css").read_text()

    def render(self, *, previous="Getting Started", following="Input Data", home=False,
               hide=(), features=("navigation.footer",), previous_section="setup",
               following_section="setup", reading_navigation=None):
        page = SimpleNamespace(
            is_homepage=home, meta={"hide": list(hide)}, parent="setup",
            previous_page=SimpleNamespace(title=previous, url="getting-started/", parent=previous_section) if previous else None,
            next_page=SimpleNamespace(title=following, url="getting-started/data/", parent=following_section) if following else None,
        )
        if reading_navigation is not None:
            page.meta["reading_navigation"] = reading_navigation
        language = SimpleNamespace(t=lambda key: {"footer.previous": "Previous", "footer.next": "Next"}[key])
        return self.template.render(page=page, features=features, lang=language)

    def test_previous_and_next_keep_real_titles_and_accessible_link_roles(self):
        html = self.render()
        self.assertEqual(html.count('<nav class="sp-page-nav"'), 1)
        self.assertEqual(html.count("<a "), 2)
        self.assertIn('aria-label="Page navigation"', html)
        for rel, title, url in (("prev", "Getting Started", "getting-started/"),
                                ("next", "Input Data", "getting-started/data/")):
            self.assertIn(f'rel="{rel}"', html)
            self.assertIn(f'href="/preview/{url}"', html)
            self.assertIn(f'<span class="sp-page-nav__title">{title}</span>', html)
        self.assertIn('aria-label="Previous: Getting Started"', html)
        self.assertIn('aria-label="Next: Input Data"', html)

    def test_one_sided_navigation_does_not_add_an_empty_link(self):
        for previous, following, direction in ((None, "Input Data", "next"), ("Research", None, "prev")):
            with self.subTest(direction=direction):
                html = self.render(previous=previous, following=following)
                self.assertEqual(html.count("<a "), 1)
                self.assertIn(f'rel="{direction}"', html)

    def test_home_hidden_disabled_and_unlisted_pages_do_not_render_a_pager(self):
        for args in ({"home": True}, {"hide": ("footer",)}, {"features": ()},
                     {"previous": None, "following": None}):
            with self.subTest(args=args):
                self.assertNotIn("<nav", self.render(**args))
        self.assertNotIn("<nav", self.template.render(page=None))
        self.assertIn("<nav", self.render(hide=("navigation", "toc")))

    def test_titles_are_escaped_in_both_the_label_and_display_text(self):
        html = self.render(previous='A < B & "C"')
        self.assertIn('Previous: A &lt; B &amp; &#34;C&#34;', html)
        self.assertNotIn('A < B', html)

    def test_global_section_order_does_not_create_a_reading_journey(self):
        html = self.render(previous="Validation", following="Funding",
                           previous_section="validation", following_section="funding")
        self.assertNotIn("<nav", html)
        html = self.render(following_section="other")
        self.assertIn('rel="prev"', html)
        self.assertNotIn('rel="next"', html)

    def test_explicit_reading_route_can_cross_sections_without_unrelated_fallback(self):
        html = self.render(reading_navigation={"next": {"title": "User Guide", "url": "user-guide/"}})
        self.assertIn('href="/preview/user-guide/"', html)
        self.assertNotIn('rel="prev"', html)
        self.assertNotIn("<nav", self.render(reading_navigation={}))

    def test_first_run_route_visits_data_overview_then_execution(self):
        route = (("getting-started/index.md", "getting-started/environment-setup/"),
                 ("getting-started/environment-setup.md", "getting-started/data/"),
                 ("getting-started/data/index.md", "getting-started/first-simulation/"),
                 ("getting-started/first-simulation.md", "user-guide/"))
        for source, destination in route:
            metadata = yaml.safe_load((DOCS / "wiki" / source).read_text().split("---", 2)[1])
            html = self.render(reading_navigation=metadata["reading_navigation"])
            self.assertIn(f'href="/preview/{destination}"', html)

    def test_pager_is_after_content_and_not_inside_the_site_footer(self):
        main = (DOCS / "overrides/main.html").read_text()
        content_block = main.split("{% block content %}", 1)[1].split("{% endblock %}", 1)[0]
        self.assertLess(content_block.index("super()"), content_block.index("partials/page-navigation.html"))
        footer = (DOCS / "overrides/partials/footer.html").read_text()
        self.assertIn('class="md-footer"', footer)
        self.assertIn("partials/copyright.html", footer)
        self.assertIn("partials/social.html", footer)
        self.assertNotIn("<nav", footer)
        config = (DOCS.parent / "mkdocs.yml").read_text()
        self.assertIn("generator: false", config)
        self.assertIn('<strong class="footer-brand">SimPaths</strong>\n', config)

    def test_pager_wraps_titles_stacks_on_mobile_and_supports_keyboard_focus(self):
        nav = blocks(self.css, ".md-typeset .sp-page-nav")
        self.assertEqual(nav[0]["grid-template-columns"], "repeat(2, minmax(0, 1fr))")
        self.assertEqual(nav[-1]["grid-template-columns"], "minmax(0, 1fr)")
        self.assertEqual(blocks(self.css, ".sp-page-nav__title")[0]["overflow-wrap"], "anywhere")
        self.assertNotIn("text-transform", blocks(self.css, ".sp-page-nav__direction")[0])
        self.assertEqual(blocks(self.css, ".sp-page-nav__link:focus-visible")[0]["outline"],
                         "2px solid var(--sp-accent)")
        link = ".md-content .md-typeset .sp-page-nav a.sp-page-nav__link"
        self.assertEqual(blocks(self.css, link)[-1]["transition"], "none")

    def test_homepage_has_no_theme_margin_between_the_final_band_and_footer(self):
        css = (DOCS / "wiki/assets/css/08-home.css").read_text()
        rules = blocks(css, "html.sp-page-home .md-container .md-main .md-content > .md-content__inner")
        self.assertEqual(rules[0]["margin-bottom"], "0")
        self.assertFalse(blocks(css, ".md-content .md-content__inner"))


if __name__ == "__main__":
    unittest.main()
