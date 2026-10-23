#!/usr/bin/env bash
set -euo pipefail
source_branch=${1:?main or develop}
output=${2:?output directory}
case "$source_branch" in main|develop) ;; *) exit 2 ;; esac
bash documentation/scripts/check-docs-css.sh
python3 -m unittest discover -s documentation/scripts -p 'test_*.py'
mvn -B -DskipTests compile javadoc:javadoc
mkdocs build --strict --site-dir "$output"
api=target/reports/apidocs
[[ -f "$api/index.html" ]] || api=target/site/apidocs
test -f "$api/index.html"
cp -R "$api" "$output/api"
python3 documentation/scripts/check-docs-content.py "$output"
