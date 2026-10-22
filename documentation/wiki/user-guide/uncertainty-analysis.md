# Uncertainty Analysis

Uncertainty analysis examines how precisely a model can estimate an outcome or policy effect, given the evidence and assumptions on which it depends. In SimPaths, an estimated employment rate, poverty rate or change in household income depends on the starting population, the behavioural and transition equations, their parameters, and the random events generated during simulation. Different experiments are needed to assess uncertainty in each of these elements.

## Sources of uncertainty {#decide-what-to-vary}

Following the [JAS-mine methodological discussion](https://www.microsimulation.ac.uk/jas-mine/resources/focus/uncertainty-analysis/), it is useful to distinguish five sources. The distinction between model structure and specification separates uncertainty about the general modelling approach from uncertainty about the particular equations used within it.

| Source | Meaning | How it can be investigated |
| --- | --- | --- |
| Input data | Sampling, measurement and data-construction uncertainty in the population and other inputs | Use resampling appropriate to the survey design, alternative data constructions or sensitivity analyses |
| Model structure | The choice of processes, mechanisms and relationships represented by the model | Compare alternative modelling approaches and assess their empirical support |
| Model specification | Covariates, functional forms and assumptions about whether estimated relationships remain applicable in the future | Compare specifications and external scenarios; examine predictive performance |
| Model parameters | Imprecision in estimated coefficients and externally supplied parameters | Propagate their uncertainty through the simulation using justified joint distributions |
| Monte Carlo variation | Variation in simulated statistics due to finite stochastic simulation, even with all inputs and parameters fixed | Repeat fixed-parameter runs and assess precision as population size and replication count increase |

These sources need not be independent. For example, a survey can contribute both to the initial population and to coefficient estimation. An analysis that varies both should consider their dependence rather than simply adding separately estimated variances.

Good data and sampling weights do not eliminate sampling or measurement uncertainty. Similarly, a good fit to estimation data does not establish that the model's mechanisms are correct or that relationships will remain stable under a new policy. Larger simulated populations can reduce Monte Carlo variation for many aggregate statistics, but cannot create additional empirical information or resolve uncertainty about the model itself.

**Sensitivity analysis** asks how results change when assumptions change. **Probabilistic uncertainty analysis** additionally assigns distributions to uncertain quantities and propagates them into results. A collection of alternative specifications or policy scenarios is not a probability distribution unless a basis for assigning probabilities is supplied.

## Propagating parameter uncertainty

Regression coefficients are estimated from finite samples. Using only their point estimates produces a projection conditional on one set of parameter values. To assess how estimation uncertainty affects the projection, the model can instead be run repeatedly with alternative coefficient vectors.

For an equation with estimated vector \(\hat{\boldsymbol\beta}\) and estimated covariance matrix \(\hat{\mathbf V}\), a common large-sample approximation is

\[
\boldsymbol\beta^{(r)} \sim
\mathcal N\!\left(\hat{\boldsymbol\beta},\hat{\mathbf V}\right),
\qquad r=1,\ldots,R.
\]

The coefficients are drawn **jointly**, preserving the estimated covariance between them. Drawing each coefficient independently from its own standard error would generally represent a different distribution. The normal approximation must also be appropriate for the estimator and respect any parameter restrictions, for example through a suitable transformation.

In each replication, draw the coefficients before the simulation begins and retain that draw throughout the projected life courses. A new replication receives a new draw. Redrawing coefficients every year would introduce time-varying parameters, changing the model rather than representing uncertainty about a fixed estimated relationship. Random individual transitions remain a separate part of each simulation.

The [JAS-mine regression library](../jasmine-reference/regression-library.md#3-bootstrap-methods-to-address-parameter-uncertainty) provides coefficient-drawing utilities for this approach. Its use of the term *bootstrap* refers here to draws from an estimated parameter distribution; it does not itself resample the survey observations and re-estimate the equations. Covariance within a supplied coefficient vector is represented, while dependence between separately estimated processes requires additional information or an estimation and resampling design that preserves it.

### Empirical distributions and normal approximations

[Creedy, Kalb and Kew (2007)](https://doi.org/10.1111/j.0307-3378.2007.00250.x) compare repeated parameter draws with a less computationally intensive approach that assumes a normal distribution for the resulting aggregate statistic. With repeated draws, empirical quantiles can describe an asymmetric distribution of results. A normal approximation summarises that distribution through its location and variance, but can be unsuitable for skewed outcomes or statistics concerning small groups. Normal coefficient draws do not imply normally distributed model outputs: behavioural choices, thresholds and interactions can make the mapping nonlinear.

The projection obtained from point-estimate coefficients should be reported separately where relevant. In a nonlinear model, it need not equal the mean or median across parameter draws.

## Interpreting uncertainty in results

### Simulation precision and uncertainty intervals

With parameters and inputs fixed, independent stochastic replications estimate the model's conditional expected outcome. If the run-level statistic has sample standard deviation \(s\) across \(R\) replications, the Monte Carlo standard error of its estimated mean is \(s/\sqrt{R}\). This measures numerical precision in estimating that expectation; it is not an interval for uncertainty in the underlying parameters or model structure.

When replications vary parameters, their spread describes the consequences of those parameter draws, together with any simulation variation left in the runs. More replications improve the estimation of this distribution; they do not make the represented parameter uncertainty disappear. To distinguish the components, a designed experiment can repeat stochastic runs within each parameter draw. This requires control over parameter draws and simulation seeds, rather than a single loop that changes both together.

Percentile ranges from parameter draws may be used as approximate confidence intervals when the sampling approximation and propagation method justify that interpretation. State the uncertainty sources they cover. They are not automatically prediction intervals for realised future outcomes, nor do they cover omitted processes or structural change. Choose the simulation size and replication count by checking stability of the particular means, quantiles and subgroup results needed for the analysis.

### Policy comparisons

For a policy effect, calculate the reform-minus-baseline difference within each matched replication and summarise those differences. Use the same input population and parameter draw in each pair where appropriate. Shared simulation random numbers can also improve precision if the implementation maintains the intended pairing.

For reform and baseline statistics \(Y_R\) and \(Y_B\), the covariance matters:

\[
\begin{aligned}
\operatorname{Var}(Y_R-Y_B)
&=\operatorname{Var}(Y_R)+\operatorname{Var}(Y_B)\\
&\quad -2\operatorname{Cov}(Y_R,Y_B).
\end{aligned}
\]

Consequently, overlap between two separate outcome intervals is not a test of the policy difference. [Goedemé et al. (2013)](https://www.microsimulation.pub/articles/00088) explain the importance of accounting for this dependence in microsimulation comparisons. Report the magnitude and uncertainty of the effect together; statistical precision alone does not establish its substantive importance.

## Implementation in SimPaths

### Check coefficient resampling in your version

In the [development revision checked for this guide](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/data/Parameters.java), `Parameters.bootstrapAll` is a `static final boolean` set to `true`. The parameter-loading routine resamples the coefficient maps covered by its bootstrap block before constructing the regression objects.

This is a source-level setting in that revision, not a supported run-time YAML or command-line toggle. Adding `bootstrapAll: false` to a configuration file does not provide a reliable way to disable it. A fixed-coefficient comparison requires a separately built and checked version with the setting changed, or a later version that explicitly exposes this control.

Not every input parameter is necessarily covered by that bootstrap block. Inspect the loader for the processes used in your analysis.

### Set up repeated runs

1. Follow [Multiple Runs](multiple-runs.md) and copy its configuration into a named file for the experiment.
2. Record the code revision, parameter workbooks, input population, donor data and policy schedule.
3. Set `maxNumberOfRuns` and the initial `randomSeed`. Set `innovation_args.randomSeedInnov` to `true` if the seed should increase between runs.
4. Hold other scenario and innovation settings fixed unless varying them is part of the design.
5. Check the effective settings and output files for each run, then summarise the distribution of the outcomes and policy differences you actually need.

In the checked implementation, the model seeds its random generator before loading parameters. With coefficient resampling enabled, changing the seed can change both coefficients and simulation draws. Such runs should not be labelled as measuring only Monte Carlo variation.

For scenario comparisons, using matched seeds can help comparisons, but does not guarantee identical random events if scenarios change the order or number of draws. Verify the comparison design and document what is paired.

## Report the scope

State which uncertainties were varied, which were held fixed, the replication count, population size, seeds and summary method. Explain how scenario comparisons were paired, what the reported intervals represent, and which sources of uncertainty remain outside the analysis.

The [Development Roadmap](../overview/roadmap.md) describes planned improvements to uncertainty controls. A planned control should not be treated as available in the executable being used.

## Further reading

- Bilcke J, Beutels P, Brisson M, Jit M (2011). Accounting for Methodological, Structural, and Parameter Uncertainty in Decision-Analytic Models: A Practical Guide. Medical Decision Making 31(4): 675-692.
- Creedy J, Kalb G, Kew H (2007). [Confidence intervals for policy reforms in behavioural tax microsimulation modelling](https://doi.org/10.1111/j.0307-3378.2007.00250.x). Bulletin of Economic Research 59(1): 37-65.
- Goedemé T, Van den Bosch K, Salanauskaite L, Verbist G (2013). [Testing the Statistical Significance of Microsimulation Results: A Plea](https://www.microsimulation.pub/articles/00088). International Journal of Microsimulation 6(3): 50-77.
- Mitton L, Sutherland H, Weeks M (2000). Microsimulation Modelling for Policy Analysis. Challenges and Innovations. Cambridge University Press.
