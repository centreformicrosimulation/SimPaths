---
reading_navigation:
  previous:
    title: Environment Setup
    url: getting-started/environment-setup/
  next:
    title: Running Your First Simulation
    url: getting-started/first-simulation/
---

# Input Data

SimPaths needs a starting population, tax-benefit donor data, and model parameters. These serve different purposes and should be versioned together for each research application.

| Input | Purpose | Where to start |
| --- | --- | --- |
| Initial population | Individuals, benefit units and households at the simulation's start year | [Initial Population (UK)](initial-population-uk.md) |
| Tax-benefit donors | Precomputed UKMOD outcomes used to impute taxes and benefits | [Tax-Benefit Donors (UK)](tax-benefit-donors-uk.md) |
| Parameters and targets | Regression coefficients, alignment targets, projections and scenario assumptions | [Model Parameterisation](../../overview/parameterisation.md) |

## Training and research data

The repository includes [initial-population training data](https://github.com/simpaths/SimPaths/tree/develop/input/InitialPopulations/training) and [donor training data](https://github.com/simpaths/SimPaths/tree/develop/input/EUROMODoutput/training). Use these with the [first-simulation guide](../first-simulation.md) to check the installation. Training results are not suitable for substantive analysis.

UK research populations are derived from Understanding Society (UKHLS), with additional sources including the Wealth and Assets Survey. Tax-benefit donors are produced with UKMOD using licensed survey inputs. Access conditions apply to the underlying data and derived records; do not upload restricted data or outputs to the public repository.

Use data releases supported by the compilation scripts for your code revision. A newer survey release is not automatically compatible with an older pipeline.

## Obtain data for the initial population {#2-obtain-data-for-the-initial-population}

Follow [Initial Population (UK)](initial-population-uk.md) for access, compilation and setup. The [Repository Guide](../../developer-guide/repository-guide.md#data-pipeline-reference) explains the pipeline stages and estimation inputs.

## Obtain data for tax-benefit donors {#3-obtain-data-for-tax-benefit-donors}

Follow [Tax-Benefit Donors (UK)](tax-benefit-donors-uk.md) to prepare compatible policy-year outputs. Running UKMOD requires Windows; SimPaths can read the resulting precomputed files on other supported platforms.
