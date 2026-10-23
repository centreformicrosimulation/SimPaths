# Working in GitHub

SimPaths development uses Git branches and pull requests. Code changes normally target `develop`; stable releases are maintained on `main`. Documentation lives alongside the model: edit `develop`, which publishes the website, and bring code and documentation into `main` together for releases.

## Prepare a fork and local clone

You need a GitHub account, Git, a supported JDK and Maven. An IDE such as IntelliJ IDEA is optional.

1. Open the [SimPaths repository](https://github.com/simpaths/SimPaths) and select **Fork**.
2. Include the `develop` branch in the fork, either by copying all branches or adding it from the repository's Branches page.
3. Clone the fork using its **Code** menu, GitHub Desktop, or:

    ```bash
    git clone https://github.com/YOUR-USERNAME/SimPaths.git
    cd SimPaths
    ```

4. Import the root `pom.xml` as a Maven project in the IDE.
5. Configure the upstream repository if using the command line:

    ```bash
    git remote add upstream https://github.com/simpaths/SimPaths.git
    git fetch upstream
    ```

GitHub's [fork guide](https://docs.github.com/en/pull-requests/how-tos/work-with-forks/fork-a-repo) covers browser, Desktop and command-line routes.

## Create a change branch

Synchronise with the current upstream branch before beginning. For a code change:

```bash
git fetch upstream
git switch develop
git merge --ff-only upstream/develop
git switch -c short-purposeful-name
```

Use the equivalent fetch, branch and checkout controls in GitHub Desktop if preferred. For model changes, use a separate branch rather than working directly on `main` or `develop`. Do not mix unrelated changes in one branch.

For documentation work, start from the latest `develop`. It publishes the website at simpaths.org, including documentation for ongoing development. Trusted maintainers can push small website edits directly after checking them; model changes should follow the reviewed pull-request workflow. Bring code and documentation into `main` together when preparing a release.

## Make and verify the change

Read the repository's `AGENTS.md` and the relevant [Repository Guide](repository-guide.md) section. Before modifying entity fields, parameters or scheduling, check the codebook, parameter mapping or schedule workbook identified there.

Use proportionate checks:

```bash
mvn test
mvn verify
```

`mvn test` runs unit tests; `mvn verify` also runs integration tests. Run the relevant training-data scenario for behaviour that is not covered by tests.

For documentation changes:

```bash
mkdocs build --strict
```

Inspect the built page as well as the Markdown source.

## Commit and push

Review the diff before staging. Never stage licensed survey data, credentials, generated databases or research outputs.

```bash
git status --short
git diff
git add PATHS-YOU-REVIEWED
git commit -m "Describe the change"
git push -u origin short-purposeful-name
```

A commit records changes locally; pushing publishes the branch to the fork.

## Open the pull request

Open a pull request from the fork's change branch to the appropriate branch in `simpaths/SimPaths`. Explain:

- the problem and intended result;
- the implementation and affected modules;
- data, parameter or compatibility implications;
- tests and simulation checks performed;
- limitations or follow-up work.

Check the automated results and address failures. Add reviewers with relevant model or domain knowledge. Do not merge solely because the project compiles.

Keep the fork synchronised before later changes. GitHub's [syncing guide](https://docs.github.com/en/pull-requests/how-tos/work-with-forks/syncing-a-fork) describes the supported options.
