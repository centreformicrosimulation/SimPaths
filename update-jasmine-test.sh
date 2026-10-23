#!/bin/bash

# usually develop
ON_BRANCH="develop"

git switch --quiet ${ON_BRANCH} || exit
git pull --quiet || exit
git switch --quiet am/against-jasmine-dev || exit
git rebase --quiet ${ON_BRANCH} || exit
git -C ../JAS-mine-core show --no-patch || exit
HASH=$(git -C ../JAS-mine-core rev-parse HEAD)
sed -i "s|^            <version>.*</version> <!--JASMINE-->$|            <version>${HASH}</version> <!--JASMINE-->|" pom.xml
echo
git diff
git commit --quiet -a --amend --no-edit
