# Uncertainty Analysis

Uncertainty analysis examines how precisely a model can estimate an outcome or policy effect, given the evidence and assumptions on which it depends. In SimPaths, an estimated employment rate, poverty rate or change in household income depends on the starting population, the behavioural and transition equations, their parameters, and the random events generated during simulation. Different experiments are needed to assess uncertainty in each of these elements.

## Sources of uncertainty {#decide-what-to-vary}

Uncertainty regarding a model’s projections can arise from a variety of reasons (Creedy et al., 2007; Bilcke et al., 2011; Sharif et al., 2012). In particular, sources of uncertainty are generally distinguished in (i) input data, for instance due to sampling errors in the initial population, (ii) model structure, that is the validity of the general modelling approach used (also called “methodological uncertainty”), (iii) model specification, which concerns the choice of the covariates and the functional forms used, and in particular the crucial assumption that any regularity observed in the data will not break up in the future, (iv) model parameters, pointing to the imprecision of the estimates and/or externally provided parameters, and finally (v) Montecarlo variation of the model output, which originates from the fact that the simulated aggregate quantities are also imprecise estimates of the theoretical aggregate quantities that the model implicitly defines. None of the above sources of uncertainty is generally considered in microsimulation studies, although this is recognised and criticised (see for instance Goedemé et al, 2013). However, “the calculation of confidence intervals around model results that account for all sources of error remains a major challenge” (Mitton et al., 2000).

To varying degrees, all these sources of uncertainty can be explored by means of a combination of sensitivity analysis and statistical techniques. Sensitivity analysis asks how results change when assumptions change. Probabilistic statistical analysis assigns distributions to uncertain quantities and propagates them into results.

Generally speaking, uncertainty over the input data quality / appropriateness (source i) should be limited when high-quality survey data are used. Comparison of the input data with other available information on the population of interest helps checking this. Uncertainty over model structure and parameterisation (sources ii-iii) can be explored by systematic [validation](../validation/index.md) of the specification of each module and the overall performance of the model. Montecarlo variation of the model outcome (source v) can be brought down to negligible by appropriately scaling up simulated population size. Multiple runs with different seeds of the random number generators allow precise quantification. The remaining source of uncertainty is parameters uncertainty, stemming from sampling errors in estimation (source iv). Parameter uncertainty stems from the imprecision of the underlying estimates, and can be quantified by exploring how the model behaves under different parameterisations. This is discussed more in details in the Section below.

To be noted, these sources need not be independent. For example, the quality of survey data can influence both the initial population from which projections take off, and the estimated coefficients used in projection.

Table 1 summarises the main sources of uncertainty in dynamic microsimulation modelling and how they can be investigated.

Table 1: Sources of uncertainty in dynamic microsimulation modelling.

| Source | Meaning | How it can be investigated |
| --- | --- | --- |
| Input data | Sampling, measurement and data-construction uncertainty in the population and other inputs | Use resampling appropriate to the survey design, alternative data constructions or sensitivity analyses |
| Model structure | The choice of processes, mechanisms and relationships represented by the model | Compare alternative modelling approaches and assess their empirical support |
| Model specification | Covariates, functional forms and assumptions about whether estimated relationships remain applicable in the future | Compare specifications and external scenarios; examine predictive performance |
| Model parameters | Imprecision in estimated coefficients and externally supplied parameters | Propagate their uncertainty through the simulation using justified joint distributions |
| Monte Carlo variation | Variation in simulated statistics due to finite stochastic simulation, even with all inputs and parameters fixed | Repeat fixed-parameter runs and assess precision as population size and replication count increase |

## Modelling parameter uncertainty

To deal with parameter uncertainty, we can follow a “brute force” approach and bootstrap the coefficients of the estimated equations from their estimated distribution (e.g. multivariate normal in case of multinomial probit regressions) with mean equal to the point estimate, and variance-covariance matrix equal to the estimated variance-covariance. ‘Bootstrapping’ refers here to draws from an estimated parameter distribution; not to resampling the survey observations and re-estimating the equations.

In this approach, for each estimated process the coefficients are drawn jointly, preserving their estimated covariance.

Bootstrapping needs to be performed only once, at the beginning of the simulation: the entire simulation is then performed with the bootstrapped values of the coefficients.

The JAS-mine simulation library allows for a simple implementation of this approach, by exploiting the [bootstrapping feature of its Regression library](../jasmine-reference/regression-library.md#3-bootstrap-methods-to-address-parameter-uncertainty) within a [multi-run implementation](multiple-runs.md): the simulation is run many times (e.g. 1,000 times), each using a different set of coefficients. The result is a distribution of model outcomes, around the central projections obtained with the estimated coefficients.

Creedy et al. (2007) compare repeated parameter draws with a less computationally intensive approach that assumes a normal distribution for the resulting aggregate statistic. Normal coefficient draws do not imply normally distributed model outputs: behavioural choices, thresholds and interactions can make the mapping nonlinear. For this reason, SimPaths implements the more computationally intensive bootstrapping approach.

## Implementation in SimPaths

### Check coefficient resampling in your version

Coefficient resampling is controlled by `Parameters.bootstrapAll`, a source-level constant currently set to `false`. When enabled, the parameter-loading routine resamples the coefficient maps covered by its bootstrap block before constructing the regression objects.

This setting is not exposed through YAML or the command line. Enabling coefficient resampling requires changing the constant and rebuilding the model. Check this setting in the executable used for the analysis before interpreting variation across runs.

Not every input parameter is necessarily covered by that bootstrap block. Inspect the loader for the processes used in your analysis.

### Set up repeated runs

1. Follow [Multiple Runs](multiple-runs.md) and copy its configuration into a named file for the experiment.

1. Record the code revision, parameter workbooks, input population, donor data and policy schedule.

1. Set `maxNumberOfRuns` and the initial `randomSeed`. Set `innovation_args.randomSeedInnov` to `true` if the seed should increase between runs.

1. Hold other scenario and innovation settings fixed unless varying them is part of the design.

1. Check the effective settings and output files for each run, then summarise the distribution of the outcomes and policy differences you actually need.

With coefficient resampling disabled, changing the seed does not resample the regression coefficients. Repeated runs with fixed inputs, coefficients and scenario settings can therefore be used to assess Monte Carlo variation.

If coefficient resampling is enabled, changing the seed can change both coefficients and simulation draws, because the model seeds its random generator before loading parameters. Those runs combine parameter uncertainty with Monte Carlo variation. Separating the two requires an experiment that holds each coefficient draw fixed across multiple stochastic replications; changing the run seed alone does not provide that separation.

For scenario comparisons, using matched seeds can help comparisons, but does not guarantee identical random events if scenarios change the order or number of draws. Verify the comparison design and document what is paired.

## Report the scope

State which uncertainties were varied, which were held fixed, the replication count, population size, seeds and summary method. Explain how scenario comparisons were paired, what the reported intervals represent, and which sources of uncertainty remain outside the analysis.

The [Development Roadmap](../overview/roadmap.md) describes planned improvements to uncertainty controls. A planned control should not be treated as available in the executable being used.

## Further reading

- Bilcke J, Beutels P, Brisson M, Jit M (2011). Accounting for Methodological, Structural, and Parameter Uncertainty in Decision-Analytic Models: A Practical Guide. Medical Decision Making 31(4): 675-692.

- Creedy J, Kalb G, Kew H (2007). [Confidence intervals for policy reforms in behavioural tax microsimulation modelling](https://doi.org/10.1111/j.0307-3378.2007.00250.x). Bulletin of Economic Research 59(1): 37-65.

- Goedemé T, Van den Bosch K, Salanauskaite L, Verbist G (2013). [Testing the Statistical Significance of Microsimulation Results: A Plea](https://www.microsimulation.pub/articles/00088). International Journal of Microsimulation 6(3): 50-77.

- Mitton L, Sutherland H, Weeks M (2000). Microsimulation Modelling for Policy Analysis. Challenges and Innovations. Cambridge University Press.

- Sharif B, Kopec JA, Wong H, Finès P, Sayre EC, Liu RR, Wolfson MC (2012). [Uncertainty Analysis in Population-Based Disease Microsimulation Models](https://doi.org/10.1155/2012/610405). Epidemiology Research International, 2012: 610405.
