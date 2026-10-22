"""Source/rendering contracts for reading tables and repository presentation."""

from pathlib import Path
import re
import unittest

import markdown

from test_site_typography import blocks


DOCS_DIR = Path(__file__).resolve().parents[1]
TABLE = ".md-typeset table:not([class])"


class ReadingPresentationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.css = (DOCS_DIR / "wiki/assets/css/03-content.css").read_text()
        cls.guide = (DOCS_DIR / "wiki/developer-guide/repository-guide.md").read_text()

    def rule(self, selector):
        found = blocks(self.css, selector)
        self.assertTrue(found, f"Missing selector: {selector}")
        return {key: value for rule in found for key, value in rule.items()}

    def test_repository_subheadings_do_not_add_an_extra_bold_layer(self):
        rendered = markdown.markdown(self.guide, extensions=["toc", "pymdownx.superfences"])
        for anchor, title in (("1-entry-points", "1. Entry Points"),
                              ("2-core-model", "2. Core Model"),
                              ("3-data-parameters", "3. Data &amp; Parameters")):
            self.assertIn(f'<h3 id="{anchor}">{title}</h3>', rendered)
        self.assertIn("<strong>Key methods</strong>", rendered)
        self.assertIn(".md-typeset :is(h1, h2, h3, h4, h5, h6, th) :is(strong, b) "
                      "{ font-weight: inherit; }", self.css)

    def test_only_the_repository_tree_opts_out_of_copy(self):
        rendered = markdown.markdown(
            self.guide,
            extensions=["pymdownx.superfences", "pymdownx.highlight", "attr_list"],
            extension_configs={"pymdownx.highlight": {"pygments_lang_class": True}},
        )
        fences = re.findall(r'<div class="([^"]*\bhighlight\b[^"]*)">', rendered)
        self.assertEqual(fences[0].split(), ["language-text", "no-copy", "highlight"])
        self.assertEqual(sum("no-copy" in fence.split() for fence in fences), 1)
        self.assertTrue(any("language-java" in fence for fence in fences[1:]))
        self.assertTrue(any("language-bash" in fence for fence in fences[1:]))

    def test_tables_use_reading_size_and_unboxed_surfaces(self):
        table = self.rule(TABLE)
        self.assertEqual(table["font-size"], "inherit")
        self.assertEqual(table["border"], "0")
        self.assertNotIn("border-top", table)
        self.assertEqual(table["background"], "transparent")
        self.assertEqual(table["box-shadow"], "none")
        self.assertNotRegex(self.css, r"\.setup-guide\s+table")

    def test_cells_use_light_separators_and_normal_code_style(self):
        for cell in ("th", "td"):
            rule = self.rule(f"{TABLE} {cell}")
            self.assertEqual(rule["border"], "0")
            expected = "2px solid currentColor" if cell == "th" else "1px solid var(--sp-border-strong)"
            self.assertEqual(rule["border-bottom"], expected)
            self.assertEqual(rule["vertical-align"], "top")
        self.assertEqual(self.rule(f"{TABLE} th")["font-weight"], "var(--sp-label-weight)")
        self.assertNotIn(f"{TABLE} code", self.css)
        self.assertEqual(self.rule(f"{TABLE} tbody tr:hover")["box-shadow"], "none")

    def test_column_labels_and_values_have_distinct_roles(self):
        header = self.rule(f"{TABLE} th")
        self.assertEqual(header["font-size"], "var(--sp-label-size)")
        self.assertEqual(header["font-family"], "var(--md-code-font-family)")
        self.assertEqual(header["text-transform"], "none")
        self.assertEqual(header["letter-spacing"], "normal")
        self.assertEqual(self.rule(f"{TABLE} td")["padding"], "0.48rem 0.8rem")
        self.assertEqual(self.rule(f"{TABLE} td:first-child")["font-weight"], "500")
        self.assertEqual(self.rule(TABLE)["font-variant-numeric"], "tabular-nums")
        self.assertNotIn("text-transform", self.rule(TABLE))

    def test_headers_and_their_rule_inherit_text_colour_in_both_schemes(self):
        header = self.rule(f"{TABLE} th")
        self.assertEqual(header["border-bottom"], "2px solid currentColor")
        self.assertEqual(header["color"], "inherit")
        self.assertFalse(blocks(self.css, f'[data-md-color-scheme="slate"] {TABLE} th'))

    def test_scroll_wrapper_and_author_column_alignment_are_preserved(self):
        self.assertEqual(self.rule(".md-typeset .md-typeset__scrollwrap")["overflow-x"], "auto")
        self.assertEqual(self.rule(".md-typeset .md-typeset__table")["padding"], "0")
        self.assertEqual(self.rule(TABLE)["width"], "100%")
        rendered = markdown.markdown(
            "| Item | Value |\n| :--- | ---: |\n| Example | 42 |", extensions=["tables"])
        self.assertIn('<th style="text-align: right;">Value</th>', rendered)
        self.assertIn('<td style="text-align: right;">42</td>', rendered)
        self.assertNotIn("text-align", self.rule(f"{TABLE} td"))


if __name__ == "__main__":
    unittest.main()
