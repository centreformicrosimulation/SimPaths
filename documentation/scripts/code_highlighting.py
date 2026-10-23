"""Conservative code colours, applied at build time without changing code text.

Plain paths, CLI options and unknown identifiers stay neutral. Java types come
from source filenames/imports, not capitalisation guesses; methods need visible
call syntax. This is syntax assistance, not an IDE's semantic analysis. Existing
Pygments output keeps its line anchors, whitespace and copy-button behaviour.
"""

import html
import re
from pathlib import Path


JAVA_TYPES = set()
JAVA_KEYWORDS = set("boolean byte char double float int long short void true false null this super".split())
TOKEN = re.compile(r'"(?:[^"\\]|\\.)*"|\'(?:[^\'\\]|\\.)*\'|[A-Za-z_$][\w$]*|\d+(?:\.\d+)?|.', re.S)
CODE_OR_PRE = re.compile(r"<pre\b[^>]*>.*?</pre>|<code>([^<]*)</code>", re.S)
JAVA_BLOCK = re.compile(r'(<div class="language-java highlight"[^>]*>)(.*?)(</div>)', re.S)
JAVA_NAME = re.compile(r'<span class="(?:n|na)">([A-Za-z_$][\w$]*)</span>')
JAVA_CALL = re.compile(r'(?:<span class="w">\s*</span>)*<span class="[po]">\(')


def on_config(config):
    """Reuse source names so docs do not need a hand-maintained class inventory."""
    JAVA_TYPES.clear()
    JAVA_TYPES.update("String Object Boolean Byte Character Double Float Integer Long Short Void Math System Exception Map List Set Collection Optional".split())
    root = Path(config.config_file_path).parent / "src/main/java"
    for source in root.rglob("*.java"):
        JAVA_TYPES.add(source.stem)
        text = source.read_text(encoding="utf-8")
        JAVA_TYPES.update(re.findall(r"^import\s+(?!static\b)[\w.]+\.([A-Z]\w*);", text, re.M))
    return config


def highlight_inline(encoded):
    value = html.unescape(encoded)
    # Keep whole paths, file names, CLI fragments and versions uncoloured.
    quoted = re.fullmatch(r'"(?:[^"\\]|\\.)*"|\'(?:[^\'\\]|\\.)*\'', value)
    if not quoted and (
        re.search(r"[/\\]|(?:^|\s)--?[A-Za-z]|\.(?:java|jar|xlsx?|csv|ya?ml|md|txt|do|xml|zip|png|log|h2|db|svg|css|json|html)$", value, re.I)
        or re.fullmatch(r"v?\d+(?:\.\d+){2,}", value)
    ):
        return encoded
    parts = []
    for token in TOKEN.finditer(value):
        word = token.group()
        kind = None
        if word.startswith(('"', "'")) and len(word) > 1:
            kind = "string"
        elif word in JAVA_TYPES:
            kind = "type"
        elif re.fullmatch(r"[a-z_$][\w$]*", word) and re.match(r"\s*\(", value[token.end():]):
            kind = "function"
        elif word in JAVA_KEYWORDS:
            kind = "keyword"
        elif re.fullmatch(r"\d+(?:\.\d+)?", word):
            kind = "number"
        escaped = html.escape(word, quote=False)
        parts.append(f'<span class="sp-code-{kind}">{escaped}</span>' if kind else escaped)
    return "".join(parts)


def highlight_java(match):
    opening, body, closing = match.groups()

    def name(token):
        word = token.group(1)
        kind = "nc" if word in JAVA_TYPES else "nf" if JAVA_CALL.match(body[token.end():]) else "n"
        return f'<span class="{kind}">{word}</span>'

    return opening + JAVA_NAME.sub(name, body) + closing


def on_page_content(content, page, **kwargs):
    # Validation deliberately uses the same typeface/colour for prose and code.
    if page.file.src_uri == "validation/index.md":
        return content

    def inline(match):
        if match.group(1) is None:
            return match.group()  # Do not colour untyped fenced/preformatted code.
        return "<code>" + highlight_inline(match.group(1)) + "</code>"

    return JAVA_BLOCK.sub(highlight_java, CODE_OR_PRE.sub(inline, content))
