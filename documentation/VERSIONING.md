# Website publication

The default site at https://simpaths.org/ is built from main. The selectable develop site at https://simpaths.org/develop/ describes unreleased code. Each build displays its source commit and any tag pointing exactly at that commit. Channels are not release identifiers.

## Editing

Keep model documentation changes in the same pull request as their code. Main corrections should also be merged into develop. Shared theme changes follow the same route. Do not publish from the retired documentation branch.

## Publication

Documentation checks build the selected branch and its Javadoc without write permissions. A publisher running from main builds frozen revisions of both branches and publishes one Pages artifact. A successful push check on develop triggers that publisher; pull-request checks never deploy. Both builds must pass. The deploy job rejects a build if either branch has advanced. Only that job has Pages write permission.

The selector preserves the current page and anchor when the page exists in the destination version. Otherwise it shows the destination homepage with an explanation. Develop pages carry noindex metadata and separate search and API output.

## Local build

Install documentation/requirements.txt, JDK 25 and Maven. Run:

```sh
bash documentation/scripts/build-version.sh main /tmp/simpaths-main-site
# In a develop checkout:
bash documentation/scripts/build-version.sh develop /tmp/simpaths-develop-site
```

Serve the main output with develop output copied into its develop/ directory to test the selector. Ordinary mkdocs serve works without generating the API; use the full build to verify API links.

## Initial migration and rollback

Land the develop migration first, then the main migration after its required review. The existing documentation deployment remains available until the main publisher succeeds. Verify both channels, source commits, search, assets, selector and APIs at simpaths.org. Only then disable the old documentation-branch deployment and archive that branch. Do not delete either source branch or the existing Pages deployment during migration.

If either source build fails, the existing website remains published. To roll back, revert the offending source change and rerun the publisher from main. Historical tags remain in Git; this migration does not publish a separate site for every historical release.
