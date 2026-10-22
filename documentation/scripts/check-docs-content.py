#!/usr/bin/env python3
"""Check built page titles, internal links and leaked editorial placeholders.

Run after mkdocs build --strict. Uses only the Python standard library.
"""

from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urljoin, urlsplit
import argparse
import re


class Page(HTMLParser):
    def __init__(self, text):
        super().__init__(convert_charrefs=True)
        self.ids = set()
        self.links = []
        self.h1_count = 0
        self.title = ""
        self.in_title = False
        self.article_depth = 0
        self.article_text = []
        self.feed(text)

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if attrs.get("id"):
            self.ids.add(attrs["id"])
        if tag == "a" and attrs.get("name"):
            self.ids.add(attrs["name"])
        if tag == "title":
            self.in_title = True
        if tag == "h1":
            self.h1_count += 1
        if tag == "article":
            self.article_depth += 1
        if tag == "a" and attrs.get("href"):
            self.links.append(attrs["href"])

    def handle_endtag(self, tag):
        if tag == "title":
            self.in_title = False
        if tag == "article":
            self.article_depth = max(0, self.article_depth - 1)

    def handle_data(self, text):
        if self.in_title:
            self.title += text
        if self.article_depth:
            self.article_text.append(text)


def check(site):
    pages = {
        path: Page(path.read_text(encoding="utf-8"))
        for path in site.rglob("*.html")
        if not {"assets", "api"}.intersection(path.relative_to(site).parts)
    }
    errors = set()
    checked_links = 0
    for path, page in pages.items():
        relative = path.relative_to(site)
        if relative == Path("404.html"):
            continue
        if page.h1_count != 1:
            errors.add(f"{relative}: expected one h1, found {page.h1_count}")
        if re.match(r"Index\s*[-–]", page.title):
            errors.add(f"{relative}: generic browser title: {page.title}")
        body = " ".join(page.article_text)
        for marker in ("Screenshot omitted", "To be completed", "the FOCUS section"):
            if marker in body:
                errors.add(f"{relative}: editorial placeholder: {marker}")
        base = "https://simpaths.org/" + relative.as_posix()
        for href in page.links:
            if "undefined" in href:
                errors.add(f"{relative}: malformed link: {href}")
            url = urlsplit(urljoin(base, href))
            # Absolute URLs are external dependencies, even on the production host.
            if urlsplit(href).scheme or href.startswith("//"):
                continue
            target = site / unquote(url.path).lstrip("/")
            if target.is_dir():
                target /= "index.html"
            checked_links += 1
            if not target.exists():
                errors.add(f"{relative}: missing target: {href}")
            elif url.fragment and target in pages:
                anchor = unquote(url.fragment)
                if anchor not in pages[target].ids:
                    errors.add(f"{relative}: missing anchor: {href}")
    for error in sorted(errors):
        print(error)
    if errors:
        raise SystemExit(f"FAILED: {len(errors)} content/link errors")
    print(f"Content check passed: {len(pages)} HTML pages, {checked_links} internal links.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("site", nargs="?", type=Path, default=Path("site"))
    args = parser.parse_args()
    if not args.site.is_dir():
        parser.error("Build the site first with mkdocs build --strict.")
    check(args.site.resolve())
