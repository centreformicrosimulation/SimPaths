# SimPaths API

SimPaths APIs are published [here](https://simpaths.org/javadoc/).

# 1. Introduction

The SimPaths API documentation is generated using [Maven's Javadoc Plugin](https://maven.apache.org/plugins/maven-javadoc-plugin/).

Javadoc is a Java tool that automatically generates HTML documentation from [Javadoc comments](https://www.oracle.com/uk/technical-resources/articles/java/javadoc-tool.html) embedded in the source code.  

The documentation website is updated automatically whenever a commit is pushed to the `develop` branch of SimPaths. This process is handled via GitHub Actions using a [Javadoc-publisher workflow developed by MathieuSoysal](https://github.com/MathieuSoysal/Javadoc-publisher.yml). The workflow file is available [here](https://github.com/simpaths/SimPaths/blob/develop/.github/workflows/publish-javadoc.yml).

To update the API documentation, add or modify Javadoc comments in the source code following [this guide](https://www.oracle.com/uk/technical-resources/articles/java/javadoc-tool.html), then push your changes to the `develop` branch.

# 2. Workflow Details

[The workflow](https://github.com/simpaths/SimPaths/blob/develop/.github/workflows/publish-javadoc.yml) automates the generation and publishing of HTML documentation from Javadoc comments whenever changes are pushed to the `develop` branch.

```
on:
  push:
    branches:
      - develop  # Only publish when pushing to develop branch
```

The code is checked out from the `develop` branch, Java 19 is installed, and SimPaths is compiled.


```
jobs:
  publish-javadoc:
    runs-on: ubuntu-latest
    permissions:
      contents: write  # Needed to push to the javadoc branch

    steps:
      - name: Checkout repository
        uses: actions/checkout@v3

      - name: Set up JDK 19
        uses: actions/setup-java@v3
        with:
          java-version: '19'
          distribution: 'temurin'
          cache: maven

      - name: Build (optional if Javadoc needs compiled sources)
        run: mvn -B compile --file pom.xml
```

The documentation is then generated from the Javadoc comments in the code.


```
- name: Generate Javadoc
        run: mvn javadoc:javadoc --file pom.xml
```

Finally, the generated documentation is deployed to the `javadoc` branch of the SimPaths repository.
 

```
- name: Deploy Javadoc to branch
        uses: MathieuSoysal/Javadoc-publisher.yml@v3.0.2
        with:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          javadoc-branch: javadoc
          java-version: 19
          target-folder: javadoc # Specifies the folder in which the documentation is saved 
          project: maven
```

The published documentation is hosted using [GitHub Pages](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site). GitHub Pages is a service that hosts static websites directly from a GitHub repository.

GitHub pages is combined with the GitHub Actions workflow to ensure that the documentation is always up to date without the need for manual deployment:

- The `javadoc` branch contains the generated HTML API documentation.
- GitHub Pages is configured to use this branch as the site’s content source.
- Each time the GitHub Actions workflow updates the `javadoc` branch, GitHub Pages automatically refreshes the live site.


