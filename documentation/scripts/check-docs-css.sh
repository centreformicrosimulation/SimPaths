#!/usr/bin/env bash
set -euo pipefail

styles=(
  documentation/wiki/assets/css/01-foundation.css
  documentation/wiki/assets/css/02-shell-navigation.css
  documentation/wiki/assets/css/03-content.css
  documentation/wiki/assets/css/04-landing-components.css
  documentation/wiki/assets/css/05-site-chrome.css
  documentation/wiki/assets/css/06-page-sections.css
  documentation/wiki/assets/css/07-roadmap.css
  documentation/wiki/assets/css/08-home.css
  documentation/wiki/assets/css/09-model-explorer.css
  documentation/wiki/assets/css/10-developer-guide.css
)

max_file_lines=1000
max_total_lines=3900
max_important=260
max_has_selectors=0

for style in "${styles[@]}"; do
  if [[ ! -s "$style" ]]; then
    echo "Missing or empty stylesheet: $style" >&2
    exit 1
  fi

  lines=$(wc -l < "$style")
  if (( lines > max_file_lines )); then
    echo "$style has $lines lines; split it before adding more rules." >&2
    exit 1
  fi

  mkdocs_path=${style#documentation/wiki/}
  if ! grep -Fq "$mkdocs_path" mkdocs.yml; then
    echo "$style is not loaded by mkdocs.yml." >&2
    exit 1
  fi
done

if [[ -e documentation/wiki/assets/css/extra.css ]]; then
  echo "Do not recreate extra.css; use the component stylesheets." >&2
  exit 1
fi

total_lines=$(wc -l "${styles[@]}" | awk 'END { print $1 }')
important_count=$({ grep -ho '!important' "${styles[@]}" || true; } | wc -l | tr -d ' ')
has_count=$({ grep -Fho ':has(' "${styles[@]}" || true; } | wc -l | tr -d ' ')

retired_selector_pattern='(^|[^[:alnum:]_-])\.(hero|card-grid|feature-card|guide-grid|guide-card|countries|country-tag)([-_][[:alnum:]_-]+)?([^[:alnum:]_-]|$)'
if grep -En "$retired_selector_pattern" "${styles[@]}"; then
  echo "Retired generic component selectors must not be restored; use a scoped component class." >&2
  exit 1
fi

if ! grep -Fq 'assets/js/site-state.js' mkdocs.yml; then
  echo "site-state.js must remain loaded so CSS can use explicit state classes." >&2
  exit 1
fi

if (( total_lines > max_total_lines )); then
  echo "Documentation CSS has grown to $total_lines lines." >&2
  exit 1
fi

if (( important_count > max_important )); then
  echo "Documentation CSS now contains $important_count !important declarations." >&2
  exit 1
fi

if (( has_count > max_has_selectors )); then
  echo "Documentation CSS contains $has_count :has() selectors; add an explicit state in site-state.js instead." >&2
  exit 1
fi

echo "CSS architecture check passed: $total_lines lines, $important_count !important declarations, $has_count :has() selectors."
