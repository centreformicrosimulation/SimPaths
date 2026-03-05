# SimPaths

by Matteo Richiardi, Patryk Bronka, Justin van de Ven

SimPaths is an open-source microsimulation framework for modelling individual and household life-course dynamics across health, labour market activity, family structure, and income. The framework is designed for country-specific implementations, with active support for the UK and Italy.

## Documentation

Start here:

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

- `singlerun.jar`
- `multirun.jar`

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

See `license.txt`.
