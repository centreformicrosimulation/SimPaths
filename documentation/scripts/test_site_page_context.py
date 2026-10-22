"""Verify navigation context is present before scripts mount the page."""

from pathlib import Path
from types import SimpleNamespace
import re
import unittest

from jinja2 import ChoiceLoader, DictLoader, Environment, FileSystemLoader, select_autoescape


DOCS = Path(__file__).resolve().parents[1]


class SitePageContextTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        env = Environment(
            loader=ChoiceLoader([
                DictLoader({"base.html": "{% block extrahead %}{% endblock %}{% block content %}{% endblock %}"}),
                FileSystemLoader(DOCS / "overrides"),
            ]), autoescape=select_autoescape(["html"]),
        )
        env.filters["url"] = lambda path: "/" + path
        cls.template = env.get_template("main.html")

    def render(self, url, ancestors=()):
        page = SimpleNamespace(url=url, is_homepage=not url, toc=[], meta={}, ancestors=ancestors)
        return self.template.render(page=page, features=[], config={"extra": {"docs_version": {"channel": "main", "sha": "a" * 40, "tags": []}}, "site_name": "SimPaths", "site_url": "https://simpaths.org/"})

    def test_standalone_pages_have_the_same_head_and_instant_navigation_context(self):
        for url in ("research/", "funding/", "validation/"):
            with self.subTest(url=url):
                html = self.render(url)
                head = re.search(r'const classes = "([^"]*)"', html).group(1)
                marker = re.search(r'data-sp-page-classes="([^"]*)"', html).group(1)
                self.assertEqual(head, marker)
                self.assertIn("sp-no-section-nav", head.split())

    def test_reading_trail_links_to_real_parent_indexes(self):
        index = SimpleNamespace(is_page=True, url="overview/", file=SimpleNamespace(name="index"))
        leaf = SimpleNamespace(is_page=True, url="overview/modules/ageing/", file=SimpleNamespace(name="ageing"))
        model = SimpleNamespace(title="Model", children=[index])
        modules = SimpleNamespace(title="Simulated Modules", children=[leaf])
        html = self.render("overview/modules/education/", [modules, model])
        self.assertIn('aria-label="Breadcrumb"', html)
        self.assertIn('<a href="/overview/">Model</a>', html)
        self.assertIn('<span>Simulated Modules</span>', html)
        self.assertNotIn('href="/overview/modules/ageing/"', html)

    def test_section_landing_does_not_link_to_itself(self):
        index = SimpleNamespace(is_page=True, url="overview/", file=SimpleNamespace(name="index"))
        model = SimpleNamespace(title="Model", children=[index])
        self.assertNotIn('class="sp-breadcrumbs"', self.render("overview/", [model]))


if __name__ == "__main__":
    unittest.main()
