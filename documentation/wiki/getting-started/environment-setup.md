# Environment Setup

!!! warning "In progress"
    This page is under development. Contributions welcome —
    see the [Getting Started](index.md) overview.

## Requirements

- Java Development Kit (JDK) 19 (the project targets Java 19 — earlier versions will not compile)
- Apache Maven 3.8 or later
- Git

## Cloning the repository

```bash
git clone https://github.com/centreformicrosimulation/SimPaths.git
cd SimPaths
```

## Building the project

```bash
mvn clean package
```

This produces `singlerun.jar` and `multirun.jar` at the repository root.

Refer to the [Working in GitHub](../developer-guide/working-in-github.md) guide for the full development workflow.
