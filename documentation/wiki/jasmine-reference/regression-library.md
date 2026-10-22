# The JAS-mine Regression Library

JAS-mine Core provides regression objects for applying externally estimated
coefficients within a microsimulation. It does not estimate regression models:
coefficients are normally estimated in statistical software, imported into
Java, and then evaluated for simulated agents.

The library used by SimPaths supports linear regression; binary logit and
probit; ordered and generalised ordered logit and probit; and multinomial
logit. Multinomial probit is not supported in JAS-mine Core 4.3.25.

The relevant classes are in the `microsim.statistics.regression` package. A
regression object stores the estimated coefficients (the beta values) and
combines them with covariate values supplied by an agent. Depending on the
model, it returns a linear score or probabilities over the possible outcomes.
Drawing a random outcome from those probabilities is a separate step, described
in [Section 2.5](#25-obtaining-probabilities-and-drawing-an-outcome).

## 1. Basic regression objects: linear, logit and probit

### 1.1 Creating regression objects and coefficient maps

Regression parameters are stored in a `MultiKeyCoefficientMap`, part of the
`microsim.data` package. A basic coefficient table has the following form:

| REGRESSOR | COEFFICIENT |
|---|---:|
| Constant | -0.50 |
| Age | 0.04 |
| Female | 0.20 |

When a coefficient table is imported from Excel, the regressor-name column
must be called `REGRESSOR` and the parameter column must be called
`COEFFICIENT`. These names are read by the regression and bootstrapping
utilities; they are not merely presentation labels.

```java
MultiKeyCoefficientMap coefficients =
        ExcelAssistant.loadCoefficientMap(
                "input/reg_example.xlsx",
                "Model1",
                1);
```

The first two arguments identify the workbook and worksheet. The final
argument is the number of leading key columns; all remaining columns are
loaded as values. A simple regression map has one key column, `REGRESSOR`, and
one value column, `COEFFICIENT`. Further value columns, named after the
regressors, can hold the covariance matrix used for coefficient bootstrapping.

An object evaluated by a regression normally implements `IDoubleSource`. It
uses an enum to identify the values corresponding to the coefficient names:

```java
public enum Regressors {
    Constant,
    Age,
    Female
}

@Override
public double getDoubleValue(Enum<?> variableId) {
    return switch ((Regressors) variableId) {
        case Constant -> 1.0;
        case Age -> age;
        case Female -> gender == Gender.Female ? 1.0 : 0.0;
    };
}
```

Coefficient names are case sensitive and must match constants in the supplied
regressor enum.

### 1.2 Linear regression

`LinearRegression` evaluates the linear predictor:

\[
\eta(\mathbf{x})
= \mathbf{x}^{\mathsf T}\boldsymbol{\beta}
= \sum_{k=1}^{K} x_k\beta_k
\]

It is constructed directly from a coefficient map:

```java
LinearRegression regression = new LinearRegression(coefficients);
```

The score for an agent is obtained using:

```java
double score = regression.getScore(
        person,
        Person.Regressors.class);
```

The returned value is the linear predictor. Any transformation, random
residual, truncation, or retransformation required by the substantive model
must be applied separately.

### 1.3 Binary logit and probit

Binary logit and probit models are represented by `BinomialRegression`. The
outcome enum must contain exactly two alternatives and implement
`IntegerValuedEnum`.

A binary probit is created as follows:

```java
BinomialRegression<Indicator> regression =
        new BinomialRegression<>(
                RegressionType.Probit,
                Indicator.class,
                coefficients);
```

For a binary logit, use `RegressionType.Logit` instead. If `y_0` and `y_1` are
the lower- and higher-valued alternatives, the probabilities are:

\[
\begin{aligned}
\Pr(Y=y_1 \mid \mathbf{x})
  &= F\!\left(\mathbf{x}^{\mathsf T}\boldsymbol{\beta}\right), \\
\Pr(Y=y_0 \mid \mathbf{x})
  &= 1-F\!\left(\mathbf{x}^{\mathsf T}\boldsymbol{\beta}\right).
\end{aligned}
\]

`F` is the logistic cumulative distribution function for a logit model and the
standard normal cumulative distribution function for a probit model.

The probability of a specified outcome can be requested directly:

```java
double probability = regression.getProbability(
        Indicator.True,
        person,
        Person.Regressors.class);
```

Probabilities for both alternatives are available through
`getProbabilities()`.

## 2. Regression objects for outcomes with more than two categories

### 2.1 Outcome enums and ordering

Ordered, generalised ordered, and multinomial regression classes use an enum to
represent the possible outcomes. The enum must implement `IntegerValuedEnum`:

```java
public enum EducationLevel implements IntegerValuedEnum {
    Low(1),
    Medium(2),
    High(3);

    private final int value;

    EducationLevel(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return value;
    }
}
```

JAS-mine sorts alternatives by `getValue()`, rather than by their declaration
order or names. This ordering defines the cumulative equations in ordered and
generalised ordered models. These classes, and `MultinomialRegression`, require
at least three alternatives.

### 2.2 Ordered logit and probit

`OrderedRegression` is used when outcome categories have a natural ordering
and the proportional-odds or parallel-lines restriction is imposed. One
coefficient vector determines the linear predictor, and the same coefficients
apply at each threshold.

An ordered logit is created as follows:

```java
OrderedRegression<OutcomeLevel> regression =
        new OrderedRegression<>(
                RegressionType.OrderedLogit,
                OutcomeLevel.class,
                coefficients);
```

For an ordered probit, use `RegressionType.OrderedProbit`.

For alternatives `y_0, ..., y_(J-1)`, the coefficient map must contain `J-1`
increasing cut points named `Cut1`, `Cut2`, and so on. Writing these cut points
as \(c_1, \ldots, c_{J-1}\), the class calculates category probabilities as
differences between successive cumulative probabilities:

\[
\Pr(Y=y_j \mid \mathbf{x})
= F\!\left(c_j-\mathbf{x}^{\mathsf T}\boldsymbol{\beta}\right)
- F\!\left(c_{j-1}-\mathbf{x}^{\mathsf T}\boldsymbol{\beta}\right).
\]

The lower and upper endpoints are zero and one in probability space. If the
cut points are not increasing, the class throws an exception because they do
not define a valid ordered probability distribution.

The score calculation visits every row in the coefficient map before the cut
points are read separately. `Cut1`, `Cut2`, and any further cut-point names
must therefore also appear in the regressor enum and return zero at agent
level:

```java
case Cut1, Cut2, Cut3 -> 0.0;
```

### 2.3 Generalised ordered logit and probit

`GeneralisedOrderedRegression` relaxes the parallel-lines restriction by
allowing coefficients to differ between cumulative equations. For ordered
alternatives `y_0, ..., y_(J-1)`, it models:

\[
q_j(\mathbf{x})
= \Pr(Y>y_j \mid \mathbf{x})
= F\!\left(\mathbf{x}^{\mathsf T}\boldsymbol{\beta}_j\right),
\qquad j=0,\ldots,J-2.
\]

The category probabilities are recovered from adjacent cumulative
probabilities:

\[
\begin{aligned}
\Pr(Y=y_0 \mid \mathbf{x})
  &= 1-q_0(\mathbf{x}), \\
\Pr(Y=y_j \mid \mathbf{x})
  &= q_{j-1}(\mathbf{x})-q_j(\mathbf{x}),
  && j=1,\ldots,J-2, \\
\Pr(Y=y_{J-1} \mid \mathbf{x})
  &= q_{J-2}(\mathbf{x}).
\end{aligned}
\]

A generalised ordered logit is created as follows:

```java
GeneralisedOrderedRegression<EducationLevel> regression =
        new GeneralisedOrderedRegression<>(
                RegressionType.GenOrderedLogit,
                EducationLevel.class,
                coefficients);
```

For a generalised ordered probit, use `RegressionType.GenOrderedProbit`.

#### 2.3.1 Coefficient naming

The constructor accepts one flattened, single-key coefficient map. A
category-specific coefficient is identified by an underscore followed by the
matching enum constant:

| REGRESSOR | Meaning |
|---|---|
| `Constant_Low` | Intercept for `P(Y > Low)` |
| `Age_Low` | Age coefficient for `P(Y > Low)` |
| `Constant_Medium` | Intercept for `P(Y > Medium)` |
| `Age_Medium` | Age coefficient for `P(Y > Medium)` |

For the three-category example, no `High` equation is required because `High`
is the final residual category. Suffixes are case sensitive and must exactly
match the Java enum constant.

A coefficient constrained to be equal across cumulative equations is stored
once with a trailing underscore and no category name. For example, `Female_`
is added to every supplied threshold equation. This supports partial
proportional-odds specifications in which some variables satisfy the
parallel-lines restriction and others do not.

#### 2.3.2 Crossing cumulative probabilities

Because cumulative equations are evaluated separately, a generalised ordered
model does not guarantee monotonic cumulative probabilities for every
covariate pattern. A valid vector requires:

\[
q_0(\mathbf{x}) \ge q_1(\mathbf{x}) \ge \cdots \ge q_{J-2}(\mathbf{x}).
\]

If adjacent cumulative probabilities cross, the current JAS-mine
implementation returns `-1.0` for the affected category. This value is a
diagnostic sentinel, not a probability, so consuming code must define how it
will handle the crossing before drawing an outcome.

SimPaths handles this condition in `MultiValEvent` by setting negative category
probabilities to zero and normalising the remaining probabilities. The repair
produces a valid vector, but it is no longer the unmodified probability vector
implied by the fitted model. Code using a generalised ordered regression should
therefore monitor `MultiValEvent.isProblemWithProbs()` and investigate frequent
crossings.

### 2.4 Multinomial logit

`MultinomialRegression` is used when the alternatives form an unordered finite
set. JAS-mine currently supports multinomial logit, but not multinomial probit.

For each non-baseline alternative `j`, the class calculates:

\[
\Pr(Y=j \mid \mathbf{x})
= \frac{\exp\!\left(\mathbf{x}^{\mathsf T}\boldsymbol{\beta}_j\right)}
       {1+\displaystyle\sum_{k \ne b}
       \exp\!\left(\mathbf{x}^{\mathsf T}\boldsymbol{\beta}_k\right)},
\qquad j \ne b,
\]

where \(b\) denotes the baseline alternative.

The baseline alternative has numerator one. A model with `J` alternatives
therefore requires coefficient maps for exactly `J-1` alternatives.

```java
MultinomialRegression<CareOutcome> regression =
        new MultinomialRegression<>(
                RegressionType.MultinomialLogit,
                CareOutcome.class,
                coefficients);
```

As in the generalised ordered class, coefficients in a flattened map use
suffixes matching the outcome enum, such as `Constant_Formal` and
`Age_Formal`. The alternative without a category-specific coefficient set is
the baseline.

### 2.5 Obtaining probabilities and drawing an outcome

The discrete-outcome classes implement `IDiscreteChoiceModel` and expose a
common interface:

```java
getEventList()
getProbability(...)
getProbabilities(...)
```

For example:

```java
Map<EducationLevel, Double> probabilities =
        regression.getProbabilities(
                person,
                Person.Regressors.class);
```

Regression objects calculate probabilities; random outcome selection is a
separate operation. A valid probability map can be sampled with
`RegressionUtils.event(probabilities)` or with an application-specific helper
such as SimPaths' `MultiValEvent`.

Within SimPaths, `ManagerRegressions` is the project-level dispatch layer.
Call it with a `RegressionName` rather than retrieving regression objects
directly from `Parameters`:

```java
// Binary model: obtain the probability for one outcome.
double healthProbability = ManagerRegressions.getProbability(
        person,
        RegressionName.HealthH2);

// Ordered model: calculate the full distribution, then draw an outcome.
Map<Dhe, Double> healthProbabilities = ManagerRegressions.getProbabilities(
        person,
        RegressionName.HealthH1);

Dhe nextHealthState = new MultiValEvent<>(
        healthProbabilities,
        healthInnovation).eval();
```

This separation keeps model selection, probability calculation, and random
outcome selection explicit. It also lets SimPaths reuse a supplied innovation
when processes require correlated or reproducible random draws.

## 3. Bootstrap methods to address parameter uncertainty

JAS-mine provides methods in `RegressionUtils` for drawing regression
coefficients from a multivariate normal distribution. The estimated
coefficient vector supplies the means and the estimated covariance matrix
supplies the covariance structure. See [Uncertainty Analysis](../user-guide/uncertainty-analysis.md)
for the role of repeated parameter draws in SimPaths.

### 3.1 Single-equation models

For linear and binomial regressions, a single `MultiKeyCoefficientMap` can
contain:

- one `REGRESSOR` key column;
- one `COEFFICIENT` value column; and
- one covariance value column for every regressor.

A new coefficient draw is obtained using:

```java
MultiKeyCoefficientMap bootstrapped =
        RegressionUtils.bootstrap(coefficientsAndCovariance);
```

The resulting map contains one sampled coefficient for each regressor and is
passed to the same constructor as the central estimates:

```java
BinomialRegression<Indicator> regression =
        new BinomialRegression<>(
                RegressionType.Probit,
                Indicator.class,
                bootstrapped);
```

The coefficient vector and covariance matrix can instead be supplied as two
maps:

```java
MultiKeyCoefficientMap bootstrapped =
        RegressionUtils.bootstrap(
                coefficients,
                covarianceMatrix);
```

The maps must describe the same regressors. Standard ordered models can use the
same approach when their slopes and cut points are stored in one parameter
vector.

### 3.2 Multiple-equation models

Multinomial and generalised ordered models contain several related coefficient
vectors. Where the estimation procedure reports covariance between equations,
the full parameter vector should be drawn jointly so that this cross-equation
covariance is retained.

If coefficients and covariance values are held in one flattened map,
`RegressionUtils.bootstrap()` can draw the complete vector before that map is
passed to `GeneralisedOrderedRegression` or `MultinomialRegression`.

For a multinomial model whose coefficient maps have already been separated by
outcome, JAS-mine also provides:

```java
Map<Outcome, MultiKeyCoefficientMap> bootstrapped =
        RegressionUtils.bootstrapMultinomialRegression(
                coefficientMaps,
                covarianceMatrix,
                Outcome.class);
```

The covariance matrix must cover every coefficient in every non-baseline
equation. Bootstrapping each equation independently would discard
cross-equation covariance and should be avoided when those covariances are
available.
