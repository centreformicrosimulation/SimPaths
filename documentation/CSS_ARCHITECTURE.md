# Documentation CSS architecture

The documentation theme is divided into ordered layers under
`documentation/wiki/assets/css/`. MkDocs loads them in the order listed below.
Changing that order can alter the cascade even when no selector changes.

1. `01-foundation.css`: fonts, tokens, palette, dark mode, and global links.
2. `02-shell-navigation.css`: header, tabs, sidebars, and documentation filtering.
3. `03-content.css`: typography, equations, code, admonitions, and tables.
4. `04-landing-components.css`: the documentation landing page only.
5. `05-site-chrome.css`: footer, search, buttons, and shared responsive chrome.
6. `06-page-sections.css`: research, funding, modules, and other page-specific
   sections.
7. `07-roadmap.css`: the development roadmap and its compact contents view.
8. `08-home.css`: the homepage and its responsive refinements.
9. `09-model-explorer.css`: the optional annual-cycle inspector.
10. `10-developer-guide.css`: architecture disclosures in the developer directory.

The main template sets page classes before the first render and embeds a matching
context marker in the article. `assets/js/site-state.js` reads that marker after
Material instant navigation and adapts interaction state for the styling layer. It exposes explicit `sp-page-*`, `sp-tab-*`,
`sp-search-open`, and navigation state classes. CSS should consume those
classes rather than rediscovering page state with relational selectors.

The Documentation sidebar's local hover trial uses a flat neutral-grey row
and darker text, without shadows, movement or changing weights. Section-page
links and their separate expand controls share one row surface; individual
page links use the same feedback. Keep dimensions stable and preserve the
distinct berry current-page background and left marker. Keyboard focus adds
a visible inset outline. Light/dark tokens are `--sp-sidebar-hover-ink` and
`--sp-sidebar-hover-bg`; only colours fade, with no transition in reduced motion.
The Documentation section-home link has a compact white surface, neutral border
and the existing, preloaded SimPaths mark. Keep the mark's dimensions reserved,
its generated content decorative, and the native link/filter behaviour intact.
The home surface stays white on hover (neutral slate in dark mode); other rows
keep neutral-grey hover. On desktop, the filter and navigation share the same
`.md-sidebar__inner` and scrollport. Reset Material's fixed inner padding, nested
list padding and nav/link side margins so bars, hover surfaces and section rules
share both edges. Indent nested link text, not its row surface. Keep the filter
sticky with an opaque page background and scroll padding for keyboard navigation.
Reserve a 0.8rem strip beyond the row edges for overlay scrollbars; a stable
scrollbar gutter also prevents classic scrollbars from shifting filtered results.
Leave the native mobile drawer layout intact.

## Shared typography convention

The approved reading hierarchy is site-wide, not a page-specific experiment.
Define its tokens once in `01-foundation.css` and consume them in `03-content.css`
and the relevant component layer:

- Page titles: `--sp-title-size` / `--sp-title-weight` (1.8rem / 560).
- Section headings: `--sp-section-size` / `--sp-section-weight` (1.16rem / 520).
- Subheadings: `--sp-subheading-size` / `--sp-subheading-weight` (0.95rem / 560).
- Main prose: `--sp-reading-copy-size` / `--sp-reading-line-height` (0.82rem / 1.75).
- Supporting labels: `--sp-label-size` / `--sp-label-weight` (0.7rem / 550).
- Compact grant and status metadata: `--sp-meta-size` (0.62rem).
- Sidebar links and the contents rail: `--sp-nav-size` / `--sp-toc-size`.

Use these roles across Model, Documentation, Validation, Research, Funding,
Roadmap and the homepage. Do not reintroduce a separate heavy heading scale or
shrink main prose at mobile breakpoints. The homepage wordmarks and large band
headings, the Documentation masthead's mobile title and its compact description
(0.72rem / 1.6), featured publication titles, compact ledger titles, equations,
code and small supporting notes retain their
purpose-specific treatments. Sharing typography does not mean sharing layouts.

All interior pages use the shared layout in `05-site-chrome.css`. It owns the
reading measure, horizontal position and top spacing. Pages with section navigation
retain the Model reading column and its navigation rails. Validation, Research
and Funding use one centred column with the same 42rem maximum width and no
reserved sidebar tracks. Do not add title padding or per-page article widths. Homepage bands
retain their distinct full-width composition. Palette changes are separate
from this convention and must not change typography or layout.

MathJax loads only when a page contains `.arithmatex`. The loader handles instant
navigation, clears detached items and queues typesetting without duplicate CDN
requests. Search opens promptly without delayed input or hiding the brand.

Run `python3 -m unittest discover -s documentation/scripts -p 'test_*.py'` to check
the shared scale and component adoption as well as code highlighting.

## Approved palette and page layouts

The white, graphite and berry design was approved for publication on 7 October
2026. It replaces the earlier cream surfaces, translucent grey navigation and
navy Use SimPaths band. Use `--sp-paper` (#FFFFFF), graphite text (#343134),
secondary text (#64616B) and berry links/selection (#972A6C). Keep the white
search surface, syntax colours and individual funder/research accents. Dark
reading pages use a charcoal-plum surface and light text with berry accents.
Use semantic tokens for theme-dependent surfaces and ink. Fixed pale panels have
separate `--sp-panel-*` ink tokens; never apply a blanket light-link override to them.
The footer theme select delegates System/Light/Dark preferences to Material's native
palette controls, including persistence and OS preference changes.

The brand, main navigation and search share one header through
`partials/header.html`. `--sp-header-height` controls its height and section
scroll offsets. Within the brand, centre the logo above the SimPaths wordmark
with a 0.2rem gap on desktop and mobile. Desktop uses a 5rem header with navigation
and search lowered to the wordmark line via `--sp-header-row-offset`; the mobile
header remains 4rem. Keep the main template's tabs block empty to avoid duplicating
the navigation. The mobile drawer retains Material navigation structure, enhanced with native
buttons, focus containment, Escape dismissal and focus return. Desktop
search hides the tabs while expanded, and restores them when closed.
Keep `toc.follow` for the visible section indicator and explicit anchor links,
but leave `navigation.tracking` disabled: its deferred scroll handler can append
the departing page's fragment to a destination during instant navigation.
Preserve the sidebar filter query across same-page anchor visits even when
Material replaces the navigation DOM; reset it when the page path changes.

`sp-no-toc` identifies pages whose contents panel is hidden or empty, on both
initial load and instant navigation. Section pages retain their reading column
when it is absent; directories do not expand into the contents rail. Desktop uses equal 11.5rem navigation/contents rails,
1.2rem gaps and a central reading column capped at 42rem. Tablet keeps the
reading column and right contents rail while the primary navigation becomes
a drawer. Mobile uses one column with 0.8rem side gutters. `sp-no-section-nav`
gives Validation, Research and Funding a centred 42rem frame with no navigation
or contents rails. Validation hides its contents panel through page metadata
and proceeds directly from its introduction to the checks and reproduction instructions.
Other interior pages retain their section-navigation rail. Titles share a top
baseline; pages of each layout share a left edge. Preserve the real contents panel
on longer technical pages and the native mobile navigation drawer.
The Model overview explains the population, model components, annual simulation
and projection inputs in one reading page, with an On this Page sidebar for
its sections and subsections alongside the shared reading column. Component descriptions use the shared
prose and heading styles, with links to the detailed modules; statistical output
is explained within the annual cycle. Keep roadmap and citation links secondary
and omit the Previous/Next
pager. The former Model Description route redirects here, preserving query
strings and section fragments; retain the `model`, `model-description` and
`simulated-modules` anchors for existing links. Directory pages (`layout: directory`,
plus the Documentation landing page) keep the same article width as Model pages.
Research and Funding records stay within the shared standalone reading column.
Keep the homepage's spacing independent
of the tighter interior-page top spacing.

Research inherits the shared article column and presents complete publication
records. Put the model paper first, then selected publications by descending
year. A narrow 6.5rem column holds year and publication type; the main column
holds the actual linked title, publication details and all authors. The two
columns stack below 40em, with year and type on one line. Publication records
have their own compact scale: 0.85rem / 600 titles, 0.75rem authors and sources,
and 0.7rem type labels. Rows use 0.8rem vertical padding. Use full foreground colour, italic journal names and
ordinary citation punctuation. Keep PDF destinations labelled and keyboard
focus visible. Use open edges, light rules and consistent spacing. Do not
substitute editorial headlines for paper titles, hide citations, add summaries,
use a two-column paper grid or add detached reading links. Omit the Previous/Next
pager. Inherit the page frame and alignment from the shared no-section-navigation layout.

The homepage opens with a full-width hero band: graphite (#27232D) in light mode
and the original midnight blue (#000A2D) in dark mode, set by `--sp-home-hero-bg`. It contains
the centred SimPaths logo, wordmark and Life Course Microsimulation strapline.
The introduction and Model features share a white band in both themes, with
fixed charcoal ink and berry links from the `--sp-panel-*` tokens. Scope these
tokens to the band so dark-mode headings, links and focus outlines remain readable.
The three-paragraph introduction beneath the hero covers the framework, country
adaptation and source-code availability, in that order. These paragraphs span
the homepage content width and lead directly into model features, separated by
spacing without a divider or second navigation menu. Four short descriptions
cover connected life domains, behavioural responses, tax–benefit detail and
alternative assumptions. Arrange them in two open columns beside their shared
purpose: policy analysis over the life course. A restrained connecting rule
groups the capabilities; it does not imply an execution sequence. Place the
purpose beneath the list on smaller screens, removing the connector, and stack
the features on mobile. Keep the underlined headings linked to detailed methods
or guides, at the shared subheading scale with visible keyboard focus outlines.
Keep descriptions concrete and concise; the linked pages carry the technical
detail. Do not add a separate row of navigation or repeat the introduction's
description of modularity.
The feature descriptions are followed by the UK and EU source-code links.
Keep the text solid charcoal (#343134),
the introduction at weight 450 and feature descriptions at 400, using the shared
reading scale. Feature links use the band's fixed berry accent. Avoid numbered
badges, an oversized brace or a repeated wordmark in this section. Preserve the
muted publication metadata in the research band.

The homepage's "Use SimPaths" section introduces a first simulation using the
bundled training data. Keep the explanation, three numbered steps and a secondary
link to the full documentation, which owns the broader guide directory. Display
the steps in three open columns, stacking them on smaller screens. Keep the
training-data limitation. Use the full-width pale blue band (#E8F0F5), graphite
text, berry link accents and blue-grey dividers (#CCD9E1) in both themes, with
one supporting text and link size (0.8rem). Do not add inset boxes or restore the
separate model-assessment, research-analysis and development directories here.
"Selected Research" uses #F7F7F9 with graphite headings, white cards and the
existing topic accents. Remove Material's trailing article margin on the
homepage only so the final research band meets the footer.
The annual-cycle figure remains the primary complete sequence. An optional
inspector lets readers select a stage and inspect its agents, inputs, updates and
module links. Its nine native disclosures remain usable without JavaScript;
enhancement adds keyboard tabs on wide layouts and a labelled select on small
layouts. It has no autoplay or fabricated simulation, and respects reduced motion.

The shared footer uses graphite #27232D in both themes; only the SimPaths brand is
white, while the sentence-case description and links use readable grey #C6C2CD.
It contains site identity and links, not page navigation. Disable the theme's
generator line through `extra.generator: false` rather than hiding a line that
still occupies space.

Previous/Next lives after the article content through `partials/page-navigation.html`,
using adjacent pages within the same parent section and respecting `hide: [footer]`.
The first-run learning sequence has explicit routes so it cannot jump into an
unrelated branch. Breadcrumbs expose parent routes on nested pages.
Keep it absent on the homepage. The two compact outlined links share the article's
width, use sentence-case direction labels and wrapping page titles, and stack
on small screens. Retain native links, keyboard focus and reduced-motion support.
Do not move navigation between containers with JavaScript or restore the tall
full-width pager inside the navy footer.

The Documentation directory uses muted section colours: blue Guides (#C8DDE8)
with a two-column link grid, then terracotta Resources (#E8BEAD) and deeper plum
Reference (#725B73) side by side. Guides and Resources use graphite headings
and links (#343134), secondary copy (#55515C) and fine dividers
(rgba(52, 49, 52, 0.16)). Reference uses white headings and links, light copy
(#F4EDF3) and translucent white dividers. Keep the small 6px corner radius.
These colour pairs apply in both site themes; retain sufficient contrast for
copy and focus outlines.
Links sit directly on each section surface with fine dividing rules and visible
keyboard focus, not in individual coloured cards. Keep link surfaces unchanged on
hover, focus and press; only arrows move, without shifting text. Do not restore
highlight fills or animated underlines. Disable motion for reduced-motion preferences.
The compact masthead places the mark before the title in one flex row, separated by
0.6rem. The mark has a padded, pure-white box; the mark and full-width introduction
align with the shared reading edge.
Below 360px, hide this repeated decorative mark so the heading fits comfortably.
The white box retains the light logo variant in both themes and reserves space
before rendering. Keep the approved typography; stack the panels and links on
small screens. Do not restore
the pastel card fills, coloured edge stripes or separate card frames.

Funding's introduction and summary bar use the full width of the grant ledger.
Do not restore a separate width cap for these two elements.

`test_site_palette.py` guards these retained colours and surface treatments.
`test_site_typography.py` also protects the Documentation description's original
compact size as an intentional exception to the main prose scale. These are
source-level checks, not browser tests.

Keep the footer description in sentence case with normal letter spacing:
"An open-source microsimulation initiative." The SimPaths brand stays unchanged.

## Headings and reading tables

Do not wrap heading labels in Markdown bold. Nested `strong`/`b` elements in
headings and table headers inherit their container's weight; emphasis in body
copy remains unchanged.

Reading tables keep body-sized values, compact rows and open edges. Column
labels use the shared label size in monospace with natural capitalisation and
normal letter spacing, separated from the values by a 2px neutral rule. Both
labels and the rule inherit the surrounding text colour in light and dark mode.
Light row separators, modest first-column emphasis
and tabular figures support scanning. Use existing ink/border tokens, transparent
surfaces and the same convention in setup guides; do not restore shaded header
panels or an outer frame. Code and math in
headers retain their original case. Keep Material's scrolling wrapper for wide
tables, preserve author-specified column alignment, and exclude code-layout
tables such as `.highlighttable` from these styles.

Directory trees are reference diagrams, not commands: mark the repository tree
fence with `{.text .no-copy}`. Material's native opt-out removes only its copy
button; command and code snippets remain copyable.

## Rules for future changes

- Put a rule in the narrowest appropriate component file.
- Preserve the order in `mkdocs.yml` unless a cascade change is intentional.
- Prefer a component class over another global Material-theme override.
- Avoid new `!important` declarations. The guard rejects all `:has()`
  selectors; add a narrowly named state in `site-state.js` instead.
- Do not add page-level `<style>` blocks. Move reusable styling into the
  appropriate component file.
- Remove a component's CSS when its final markup is removed. The retired
  generic hero and card systems are guarded against accidental restoration.
- Run the architecture check and strict MkDocs build before publishing.

```bash
bash documentation/scripts/check-docs-css.sh
mkdocs build --strict
cd documentation/visual-tests && npm test
```

The budgets are set against the refactored baseline, with limited headroom for
deliberate additions. If a file approaches its cap, simplify or extract a
coherent responsibility instead of raising the limit by default.

The browser tests cover representative desktop and mobile routes, assert basic
layout invariants, and save full-page screenshots under `test-results/`. The
deployment workflow uploads those screenshots as an artifact so visual changes
can be reviewed without making pixel-level rendering differences block a
release.
