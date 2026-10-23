"""Run with: python -m unittest discover -s documentation/scripts -p 'test_*.py'."""

from html.parser import HTMLParser
from pathlib import Path
from types import SimpleNamespace
import unittest

import markdown
import code_highlighting as colours


class TextOnly(HTMLParser):
    def __init__(self, source):
        super().__init__()
        self.text = ""
        self.feed(source)

    def handle_data(self, data):
        self.text += data


class CodeColoursTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        colours.on_config(SimpleNamespace(config_file_path=Path(__file__).resolve().parents[2] / "mkdocs.yml"))

    def render(self, content, route="developer-guide/repository-guide.md"):
        return colours.on_page_content(content, SimpleNamespace(file=SimpleNamespace(src_uri=route)))

    def test_inline_types_and_methods_use_different_colours(self):
        result = colours.highlight_inline("SimPathsModel.buildSchedule()")
        self.assertIn('class="sp-code-type">SimPathsModel', result)
        self.assertIn('class="sp-code-function">buildSchedule', result)
        self.assertEqual(TextOnly(result).text, "SimPathsModel.buildSchedule()")

    def test_neutral_fragments_are_not_guessed(self):
        for value in ("input/InitialPopulations/training/", "output/logs/run_&lt;seed&gt;.txt", "singlerun.jar",
                      "Person.java", "--rewrite-policy-schedule", "-f", "java -jar multirun.jar",
                      "5.2.0", "UK", "SomethingUnrecognised", "bootstrapAll"):
            with self.subTest(value=value):
                self.assertEqual(colours.highlight_inline(value), value)

    def test_literals_and_escaping_preserve_text(self):
        for value, kind in (('&quot;UK&quot;', "string"), ("2019", "number"), ("true", "keyword")):
            self.assertIn(f'sp-code-{kind}', colours.highlight_inline(value))
        value = "List&lt;Person&gt; &amp; &lt;unknown&gt;"
        result = colours.highlight_inline(value)
        self.assertEqual(TextOnly(result).text, TextOnly(value).text)
        self.assertNotIn("<unknown>", result)

    def test_preformatted_code_and_explicit_markup_are_left_alone(self):
        for value in ('<pre><code>SimPathsModel</code></pre>', '<code class="language-text">SimPathsModel</code>',
                      '<code><span class="custom">SimPathsModel</span></code>'):
            self.assertEqual(self.render(value), value)

    def test_validation_keeps_its_uniform_typography(self):
        source = '<p>Run <code>SimPathsStart</code> with <code>true</code>.</p>'
        self.assertEqual(self.render(source, "validation/index.md"), source)

    def test_java_blocks_keep_text_and_line_anchors(self):
        source = markdown.markdown('```java\nMap<Dhe, Double> probs =\n    ManagerRegressions.getProbabilities(this, RegressionName.HealthH1);\n```',
                                   extensions=["pymdownx.superfences", "pymdownx.highlight"],
                                   extension_configs={"pymdownx.highlight": {"line_spans": "__span", "anchor_linenums": True, "pygments_lang_class": True}})
        result = self.render(source)
        self.assertIn('class="nc">ManagerRegressions', result)
        self.assertIn('class="nf">getProbabilities', result)
        self.assertIn('class="n">HealthH1', result)
        self.assertIn('id="__span', result)
        self.assertEqual(TextOnly(result).text, TextOnly(source).text)
        self.assertEqual(self.render(result), result)

    def test_other_languages_are_not_treated_as_java(self):
        source = markdown.markdown('```bash\necho Person\n```', extensions=["pymdownx.superfences", "pymdownx.highlight"],
                                   extension_configs={"pymdownx.highlight": {"pygments_lang_class": True}})
        self.assertEqual(self.render(source), source)


if __name__ == "__main__":
    unittest.main()
