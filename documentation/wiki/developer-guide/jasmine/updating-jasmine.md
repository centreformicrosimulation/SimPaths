# Updating JAS-mine

SimPaths manages JAS-mine through Maven. Use the dependency declared in the checked-out `pom.xml`; do not add a second copy of the library manually to an IDE build path.

## Check the dependency {#1-using-apache-maven}

From the repository root:

```bash
mvn dependency:tree
```

Locate `com.github.jasmineRepo:JAS-mine-core`. The [verified development revision](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/pom.xml) declares version `5.2.0`; this is a version reference, not a claim that it is the latest available release.

## Evaluate an update

1. Create a branch from the appropriate SimPaths development revision.
2. Review the proposed JAS-mine release and its compatibility with the project's Java version and dependent libraries.
3. Update the dependency in `pom.xml`, then reload the Maven project in your IDE.
4. Run the unit and integration tests:

    ```bash
    mvn verify
    ```

5. Build the executables and check the first training-data run, multi-run execution, GUI, regression loading and persistence paths affected by the update.
6. Include the old and new dependency versions, compatibility changes and test results in the pull request.

A successful compilation alone does not establish that the simulation behaves identically after a library update.

## IDE setup {#2-manual-update}

Import SimPaths as a Maven project using its root `pom.xml`. Maven should supply the dependencies consistently for the IDE, command-line build and CI. The old Eclipse Luna/manual-JAR workflow is not required for this project.

See [Environment Setup](../../getting-started/environment-setup.md), [Working in GitHub](../working-in-github.md) and the [JAS-mine core repository](https://github.com/jasmineRepo/JAS-mine-core) for the corresponding project and library guidance.
