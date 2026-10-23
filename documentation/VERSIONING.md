# Website publication

https://simpaths.org/ is built only from `develop`, including matching Javadoc. There is one public site, with no version selector or separate `/develop/` site. Documentation follows ongoing model development; it may describe features not yet included in the latest release.

## Editing

Keep documentation beside its model code. Start website changes from the latest `develop`, preview and check them, and push only the intended website files. Trusted maintainers can push small website edits directly. Code, input data and publishing-system changes should receive normal review. Documentation for a model change belongs in the same change as its implementation.

Bring code and documentation from develop into main together for releases. Main is not a publishing branch. No separate documentation branch is needed.

## Publication

Documentation checks run on both branches and their pull requests without publishing. A push to `develop`, or a manual deployment explicitly run on `develop`, builds one frozen develop revision and its matching Javadoc. The workflow checks the site, uploads one Pages artifact, and rejects a superseded build. Only the deploy job has Pages write permission. The github-pages environment permits deployments from develop only.

Each successful deployment replaces the entire site. If a build fails, the existing website remains published. Main pushes do not deploy. Until main receives this workflow at release time, its old workflow may still run on main pushes, but the deployment environment blocks its publication.

## Local build

Install `documentation/requirements.txt`, JDK 25 and Maven, then run:

```sh
bash documentation/scripts/build-version.sh develop /tmp/simpaths-website
```

The branch argument identifies the checkout being checked, not a public channel. Ordinary `mkdocs serve` previews content without regenerating the API; use the full build to verify API links.

## Rollback

Revert the offending change on develop and rerun its publisher. Historical releases are identified by Git tags and commits; they do not create additional websites.
