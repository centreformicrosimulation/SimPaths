# SimPaths

by Matteo Richiardi, Patryk Bronka, Justin van de Ven

## What is SimPaths and how to use it?

SimPaths is an open-source framework for modelling individual and household life course events across multiple domains. The framework projects life histories over time, developing detailed representations of career paths, family and intergenerational relationships, health, and financial circumstances. As a family of models, SimPaths offers a dynamic simulation of how life events evolve and interact within populations.

SimPaths models currently exist for the UK, Greece, Hungary, Italy, and Poland. This page refers to the UK model; the other European models are available at the corresponding [SimPathsEU](https://github.com/centreformicrosimulation/SimPathsEU) page. 

The entire SimPaths documentation is available on its [WikiPage](https://github.com/centreformicrosimulation/SimPaths/wiki), which includes: a detailed description of its building blocks; instructions on how to set up and run the model; information about contributing to the model's development.

<!-- Projections for a workhorse model parameterised to the UK context are reported in [Bronka, P. et al. (2023). *SimPaths: an open-source microsimulation model for life course analysis* (No. CEMPA6/23), Centre for Microsimulation and Policy Analysis at the Institute for Social and Economic Research*](https://www.microsimulation.ac.uk/publications/publication-557738/), which closely reflect observed data throughout a 10-year validation window. -->


<!--
## Getting Started

- [Documentation Home](docs/README.md)
- [Getting Started](docs/getting-started.md)
- [CLI Reference](docs/cli-reference.md)
- [Configuration](docs/configuration.md)
- [Scenario Cookbook](docs/scenario-cookbook.md)
- [Data and Outputs](docs/data-and-outputs.md)
- [Architecture](docs/architecture.md)
- [Development and Testing](docs/development.md)
- [GUI Guide](docs/gui-guide.md)
- [Troubleshooting](docs/troubleshooting.md)

## Quick Start

### 1. Requirements

- Java 19 (Temurin/OpenJDK recommended)
- Maven 3.8+

### 2. Build

```bash
mvn clean package
```

This produces runnable jars at the repository root:

- `-c` Country ['UK' or 'IT']
- `-s` Start year
- `-g` [true/false] show/hide gui
- `--rewrite-policy-schedule` Re-write policy schedule from detected policy files
- `-Setup` do setup phases (creating input populations database) only

### 3. First setup run

Create or refresh setup artifacts for a headless run:

```bash
java -jar singlerun.jar -c UK -s 2019 -g false -Setup --rewrite-policy-schedule
```

### 4. Multi-run simulation

```bash
java -jar multirun.jar -config default.yml -g false
```

Outputs are written under `output/`.

## Data Access

SimPaths code is open source, but full research input data cannot be openly redistributed.

- A training dataset is included for development and CI use.
- Full data access requests should be submitted via the repository issue tracker.

Repository issue tracker: [SimPaths Issues](https://github.com/centreformicrosimulation/SimPaths/issues)

## Citation

Bronka, P. et al. (2023). *SimPaths: an open-source microsimulation model for life course analysis*.
[https://www.microsimulation.ac.uk/publications/publication-557738/](https://www.microsimulation.ac.uk/publications/publication-557738/)

## License

These branch naming conventions are designed to make it easy for our contributors to understand the purpose of each branch and maintain consistency within our repository. Please adhere to these conventions when creating branches for your contributions.
-->
