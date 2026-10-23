# Website publication

https://simpaths.org/ is published only from `main`. There is no public develop site or version selector. Documentation in `develop` stays alongside unreleased model code and becomes public when merged into `main`.

## Editing

Keep model documentation changes in the same pull request as their code. Target `develop` for development-only features, and `main` for corrections to the published website. Bring main corrections and shared theme changes into develop through the normal merge workflow. Do not publish from the retired documentation branch.

## Publication

Documentation checks build both branches and their pull requests without publishing. A push to `main`, or a manual deployment explicitly run on `main`, builds one frozen main revision and its matching Javadoc. The workflow checks the site and uploads one Pages artifact. It rejects a superseded main build before deployment. Only the deploy job has Pages write permission. Develop pushes do not trigger publication.

Each deployment replaces the entire published artifact, so the next successful main deployment removes the former `/develop/` site. No redirect to unreleased documentation is retained.

## Local build

Install `documentation/requirements.txt`, JDK 25 and Maven. From the relevant checkout, run:

```sh
bash documentation/scripts/build-version.sh main /tmp/simpaths-main-site
# To validate a develop checkout locally, without publishing:
bash documentation/scripts/build-version.sh develop /tmp/simpaths-develop-check
```

Both builds use the same root URL structure. The branch argument identifies the checkout being checked, not a public channel. Ordinary `mkdocs serve` works without generating the API; use the full build to verify API links.

## Rollback

If a build fails, the existing website remains published. Revert the offending change on main and rerun its publisher. Historical releases are identified by Git tags and commits; they do not create additional websites.
