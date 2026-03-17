# SimPaths

by Matteo Richiardi, Patryk Bronka, Justin van de Ven

## What is SimPaths and how to use it?

SimPaths is an open-source framework for modelling individual and household life course events across multiple domains. The framework projects life histories over time, developing detailed representations of career paths, family and intergenerational relationships, health, and financial circumstances. As a family of models, SimPaths offers a dynamic simulation of how life events evolve and interact within populations.

SimPaths models currently exist for the UK, Greece, Hungary, Italy, and Poland. This page refers to the UK model; the other European models are available at the corresponding [SimPathsEU](https://github.com/centreformicrosimulation/SimPathsEU) page. 

The entire SimPaths documentation is available on its [website](https://centreformicrosimulation.github.io/SimPaths/), which includes: a detailed description of its building blocks; instructions on how to set up and run the model; information about contributing to the model's development.

## Quick start

### Prerequisites

- Java 19
- Maven 3.8+
- Optional IDE: IntelliJ IDEA (import as a Maven project)

### Build and run

```bash
mvn clean package
java -jar multirun.jar -DBSetup
java -jar multirun.jar
```

The first command builds the JARs. The second creates the H2 donor database from the input data. The third runs the simulation using `default.yml`.

To use a different config file:

```bash
java -jar multirun.jar -config my_run.yml
```

For configuration options, see the annotated `config/default.yml`. For the data pipeline and further reference, see [`documentation/`](documentation/README.md).

<!-- Projections for a workhorse model parameterised to the UK context are reported in [Bronka, P. et al. (2023). *SimPaths: an open-source microsimulation model for life course analysis* (No. CEMPA6/23), Centre for Microsimulation and Policy Analysis at the Institute for Social and Economic Research*](https://www.microsimulation.ac.uk/publications/publication-557738/), which closely reflect observed data throughout a 10-year validation window. -->


<!--
## Getting Started

To contribute to this project, you need to fork the repository and set up your development environment.

### Access to Data

We are committed to maintaining transparency and open-source principles in this project. All the code, documentation, and resources related to our project are available on GitHub for you to explore, use, and contribute to.

The data used by this project is not freely shareable. If you are interested in accessing the data necessary to run the simulation, get in touch with the repository maintainers for further instructions.

However, please note that _training_ data is provided. It allows the simulation to be run and developed, but results obtained on the basis of the training dataset should not be interpreted, except for the purpose of training and development. 

**How to Request Access to Data:**

If you have a need for the data, please contact the repository maintainers through the [issue tracker](https://github.com/centreformicrosimulation/SimPaths/issues).


### Forking the Repository

1. Click the "Fork" button at the top-right corner of this repository.
2. Untick the `Copy only the main branch` box.
3. This will create a copy of the repository in your own GitHub account.
4. Follow [instructions here](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/working-with-forks/syncing-a-fork) to periodically synchronize your fork with the most recent version of this ("upstream") repository. This will ensure you use an up-to-date version of the model.

### Setting up your development environment
1. **Java Development Kit (JDK):** Ensure you have a JDK (Java Development Kit) installed on your system. You can download and install the latest version of OpenJDK from [Adoptium](https://adoptium.net/).
2. **Download an IDE** (integrated development environment) of your choice - we recommend [IntelliJ IDEA](https://www.jetbrains.com/idea/download/); download the Community (free) or Ultimate (paid) edition, depending on your needs.
3. Clone your forked repository to your local machine. Import the cloned repository into IntelliJ as a Maven project 

### Compiling and running SimPaths with Maven in the CLI

SimPaths can also be compiled by Maven ([installation instructions here](https://maven.apache.org/install.html)) and run from the command line without an IDE. After cloning the repository and setting up the JDK, in the root directory you can run:
```
$ mvn clean package
```
... to create two runnable jars for single- and multi-run SimPaths:
```
.
SimPaths/
      ...
      |-- multirun.jar
      |-- singlerun.jar
      `-- src
```

To run the SimPathsStart setup phases and set up a population for subsequent multiruns, `singlerun.jar` takes the following options:

- `-c` Country ['UK' or 'IT']
- `-s` Start year
- `-g` [true/false] show/hide gui
- `--rewrite-policy-schedule` Re-write policy schedule from detected policy files
- `-Setup` do setup phases (creating input populations database) only

e.g.
```
$ java -jar singlerun.jar -c UK -s 2017 -g false -Setup
```
For multiple runs, `multirun.jar` takes the following options:

- `-r` random seed for first run (incremented by +1 for subsequent runs)
- `-p` simulated population size
- `-n` number of runs
- `-s` start year of runs
- `-e` end year of runs
- `-g` [true/false] show/hide gui
- `-f` write console output and logs to file (in 'output/logs/run_[seed].txt')

e.g.
```
$ java -jar multirun.jar -r 100 -p 50000 -n 20 -s 2017 -e 2020 -g false -f
```

Run `java -jar singlerun.jar -h` or `java -jar multirun.jar -h` to show these help messages.

### Contributing

1. Create a new branch for your contributions. This will likely be based on either the `main` branch of this repository (if you seek to modify the stable version of the model) or `develop` (if you seek to modify the most recent version of the model).  Please see branch naming convention below.
2. Make your changes, add your code, and write tests if applicable.
3. Commit your changes.
4. Push your changes to your fork.
5. Open a Pull Request (PR) on this repository from your fork. Be sure to provide a detailed description of your changes in the PR.

### Branch Naming Conventions

In our open-source project, we follow a clear and consistent branch naming convention to streamline the development process and maintain a structured repository. These conventions help our team of contributors collaborate effectively. Here are the primary branch naming patterns:

1. **Main Branches:**
    - `main`: Represents the stable version of our model.
    - `develop`: Used for ongoing development and integration of new features.

2. **Feature Branches:**
    - `feature/your-feature-name`: Create feature branches for developing new features.

3. **Bug Fix Branches:**
    - `bugfix/issue-number-description`: Use bug fix branches for specific issue resolutions. For example, `bugfix/123-fix-health-process-issue`.

6. **Experimental or Miscellaneous Branches:**
    - `experimental/your-description`: For experimental or miscellaneous work not tied to specific features or bug fixes. For instance, `experimental/new-architecture`.

7. **Documentation Branches:**
    - `docs/documentation-topic`: Prefix documentation branches with `docs` for updating or creating documentation. For example, `docs/update-readme`.

These branch naming conventions are designed to make it easy for our contributors to understand the purpose of each branch and maintain consistency within our repository. Please adhere to these conventions when creating branches for your contributions.
-->