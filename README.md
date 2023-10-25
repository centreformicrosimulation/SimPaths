# SimPaths

by Matteo Richiardi, Patryk Bronka, Justin van de Ven

## Introduction

SimPaths is a family of models for individual and household life course events, all sharing common components. The framework is designed to project life histories through time, building up a detailed picture of career paths, family (inter)relations, health, and financial circumstances. The framework builds upon standardised assumptions and data sources, which facilitates adaptation to alternative countries – versions currently exist for the UK and Italy. Careful attention is paid to model validation, and sensitivity of projections to key assumptions. The modular nature of the SimPaths framework is designed to facilitate analysis of alternative assumptions concerning the tax and benefit system, sensitivity to parameter estimates and alternative approaches for projecting labour/leisure and consumption/savings decisions. Projections for a workhorse model parameterised to the UK context are reported in [Bronka, P., Richiardi, M., & van de Ven, J. (2023). *SimPaths: an open-source microsimulation model for life course analysis* (No. CEMPA6/23), Centre for Microsimulation and Policy Analysis at the Institute for Social and Economic Research*](https://www.microsimulation.ac.uk/publications/publication-557738/), which closely reflect observed data throughout a 10-year validation window.

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
