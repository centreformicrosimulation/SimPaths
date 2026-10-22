# SimPaths API

The API documentation is generated from Javadoc comments in the Java source. Use it to inspect classes and methods; use the [Repository Guide](../repository-guide.md) and [model schedule](../jasmine/model-and-schedule.md) to understand how those components work together.

## Generate the API locally {#1-introduction}

From the repository root, with the project's JDK and Maven installed:

```bash
mvn javadoc:javadoc
```

Inspect the generated HTML under `target/reports/apidocs/` or `target/site/apidocs/`, depending on the Maven Javadoc plugin version and configuration. The command output reports the destination.

When changing a public method, document its purpose, inputs, return value, side effects and relevant assumptions. Verify the generated page before submitting the change through the normal code-review process.

## Publishing workflow {#2-workflow-details}

The website builds the API from the same Git revision as the selected documentation version. Main documentation and its API are published at the default website; the develop version has its own API under `/develop/api/`.

The combined Pages publisher runs from `main` after a main update or successful development documentation checks. Both versions must build successfully before either is published. A documentation change should accompany the code change in the same branch and pull request.

Use the source revision displayed above to identify the exact build. Branch names identify moving documentation channels; release tags and commit hashes identify reproducible versions.
