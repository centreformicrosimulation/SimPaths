# The JAS-mine Regression Library

The JAS-mine Core libraries support several types of regressions, including:

* Linear Regression
* Logistic (Logit) Regression
* Probit Regression
* Multinomial Logistic (Logit) Regression
* Multinomial Probit Regression

The relevant classes can be found in the microsim.statistics.regression package of the core JAS-mine libraries. The regression objects store the regression coefficients (the 'betas') of a regression. When they are passed another object such as an agent that holds the corresponding regressor or 'covariate' values (the 'x's in a regression), these objects can return a variety of values corresponding to linear regression 'scores', logit / probit 'probabilities' or random outcome from a binary event (i.e. whether an outcome takes place). Multinomial logit or probit regression objects return the random outcome from a finite set of possible outcomes.

# 1. Basic Regression Objects: linear, logit and probit

## 1.1 Creation of the regression objects

The [Demo07 demo](https://www.microsimulation.ac.uk/jas-mine/demo/demo07) example uses the LinearRegression and LogitRegression classes, and also the extra functionality provided by static methods in the RegressionUtils class.

For example, in the Parameters class, the regression objects are created, as shown below:

```java
// regression coefficients  
MultiKeyCoefficientMap coeffMarriageFit = ExcelAssistant.loadCoefficientMap("input/reg_marriage.xls", "Sheet1", 1, 1);  
  
MultiKeyCoefficientMap coeffDivorce = ExcelAssistant.loadCoefficientMap("input/reg_divorce.xls", "Sheet1", 1, 1);  
  
MultiKeyCoefficientMap coeffInWork = ExcelAssistant.loadCoefficientMap("input/reg_inwork.xls", "Sheet1", 3, 1);  
  
// definition of regression models   
LinearRegression regMarriageFit = new LinearRegression(coeffMarriageFit);  
LogitRegressio regDivorce = new LogitRegression(coeffDivorce);  
LogitRegressio regInWork = new LogitRegression(coeffInWork);
```

The first few lines take regression coefficients from Microsoft Excel .xls files that are stored in the project's input folder, and the *ExcelAssistant.loadCoefficientMap()* method converts them into MultiKeyCoefficientMap types. The *MultiKeyCoefficientMap* type, found in the microsim.data package of the core JAS-mine libraries, is a class that extends the Apache Commons MultiKeyMap type, allowing users to retrieve regression coefficients depending on multiple keys that correspond to the regressor (covariate) and the attributes of the agent. The last two arguments in the *loadCoefficientMap()* method refer to the number of key columns and the number of value columns respectively. All *MultiKeyCoefficientMaps* have one value column – the regression coefficients. The "Sheet1" argument refers to the name of the relevant Excel worksheet; an alternative approach could be to place all regression coefficients in the same .xls file called 'reg_coefficients', but store coefficients for different regressions in different worksheets, for example named 'marriage', 'divorce', 'inWork' etc.

The last three lines of the code box creates regression objects whose argument is the *MultiKeyCoefficientMap* of regression coefficients. In particular, regMarriageFit is a *LinearRegression* object, whilst regDivorce and regInWork are *LogitRegression* objects. *ProbitRegression* objects are created in a similar way.

An example of the information used in the reg_inwork.xls file is shown below; note that it has three key columns, hence the second argument of the *loadCoefficientMap()* method used to create the *coeffInWork* object is '3'. As you can see from the data below, the first column lists the regressor (or 'covariate') names, and the second and third columns specify the conditions of the agent that need to be checked when applying the regression object – different regression coefficients apply to different types of agent (in this case, the relevant agent attributes are their gender and their work status ('gender' and 'workState' are the names of the agent's relevant fields).

Note that it is important, when using .xls files to hold regression coefficients, to follow the convention of using the header 'REGRESSOR' for the regressors column and 'COEFFICIENT' for the coefficients column. This is because the JAS-mine methods check *MultiKeyCoefficientMaps* for these column headers when performing calculations for regression functionality such as bootstrapping (using the *RegressionUtils.bootstrap()* method). Indeed, REGRESSOR and COEFFICIENT are special enum constants of the Enum *RegressionColumnNames* class in the microsim.statistics.regression package.

| REGRESSOR | gender | workState | COEFFICIENT |
|-----------|--------|-----------|-------------|
| age | Male | Employed | -0.196599 |
| ageSq | Male | Employed | 0.0086552 |
| ageCub | Male | Employed | -0.000988 |
| isMarried | Male | Employed | 0.1892796 |
| workIntercept | Male | Employed | 3.554612 |
| age | Male | NotEmployed | 0.9780908 |
| ageSq | Male | NotEmployed | -0.0261765 |
| ageCub | Male | NotEmployed | 0.000199 |
| workIntercept | Male | NotEmployed | -12.39108 |
| age | Female | Employed | -0.2740483 |
| ageSq | Female | Employed | 0.0109883 |
| ageCub | Female | Employed | -0.0001159 |
| isMarried | Female | Employed | -0.0906834 |
| workIntercept | Female | Employed | 3.648706 |
| age | Female | NotEmployed | 0.8217638 |
| ageSq | Female | NotEmployed | -0.0219761 |
| ageCub | Female | NotEmployed | 0.000166 |
| isMarried | Female | NotEmployed | -0.5590975 |
| workIntercept | Female | NotEmployed | -10.48043 |

## 1.2 How to use the Linear Regression objects

Linear regression objects return the score of the linear regression, i.e. the inner product of the regression coefficients with the regressors (the sum over i of beta_i * x_i). This can be invoked as in the following example, where the Person object called 'ross' provides the regressor values (the 'x's) to the LinearRegression object, which holds the regression coefficients (the betas).

```java
double marriageScore = Parameters.getRegMarriageFit().getScore(ross, Person.Regressors.class);
```

This case uses the *getScore()* method with signature:

```java
public <T extends Enum<T>> double getScore(IDoubleSource iDblSrc, Class<T> enumType)
```

This is because the Person class implements the *IDoubleSource* interface – this is how the *Person* class retrieves the correct regressor values, using the *Person.Regressors* inner enum class. Note that these methods require the specification of the *Person.Regressors* inner enum class. As seen in the *Person* class of the Demo07 demo example model:

```java
// ---------------------------------------------------------------------  
// implements IDoubleSource for use with Regression classes  
// ---------------------------------------------------------------------   
  
  
public enum Regressors {  
  
    //For in work regression  
    age,   
    ageSq,   
    ageCub,  
    isMarried,  
    workIntercept;  
  
}  
  
  
public double getDoubleValue(Enum<?> variableID) {  
  
    switch ((Regressors) variableID) {  
  
    //For work regression  
    case age:  
        return (double) age;  
    case ageSq:  
        return (double) age * age;  
    case ageCub:  
        return (double) age * age * age;  
    case isMarried:  
        return civilState.equals(CivilState.Married)? 1. : 0.;  
    case workIntercept:  
        return 1.;            //The constant intercept, so regression coefficient is multiplied by 1  
  
    default:  
        throw new IllegalArgumentException("Unsupported regressor " + variableID.name() + " in                                                                                                 Person.getDoubleValue");  
  
    }  
  
}
```

There are other getScore() methods that also return the linear regression score but use different input arguments – see the [Javadocs](https://www.microsimulation.ac.uk/jas-mine/resources/api/) of JAS-mine-core's microsim.statistics.regression package.

## 1.3 How to use the Logit and Probit regression objects

The logit and probit regression objects return the logit or probit transforms of the linear regression score, respectively. As these transforms produce numbers bounded in the interval [0, 1], they are often interpreted as 'probabilities' that an event occurs or not (an event with a binary outcome). Hence logit and probit regressions are used to model the outcome of binary events.

The methods available in the *LinearRegression* and *ProbitRegression* classes include *getProbability()*, which returns the 'probability' (i.e. the logit and probit transform of the linear regression score), and event(), which returns a boolean representing whether the event outcome is true or false (i.e. whether the outcome is deemed to occur or not). The *event()* method generates a random boolean whose value is true with probability equal to the value returned by the *getProbability()* method; conversely the boolean is false with probability equal to 1-*getProbability()*.

In the Demo07 example model, we can see how the logit 'probability' is used to calculate the probability that *Person* object 'ross' is in work:

```java
double workProb = Parameters.getRegInWork().getProbability(ross, Person.Regressors.class, ross,                                                                                                       Person.RegressionKeys.class);
```

We could directly calculate the random boolean variable to determine whether *Person* object 'ross' is in work as follows:

```java
boolean inWork = Parameters.getRegInWork().event(ross, Person.Regressors.class, ross,                                                                                                           Person.RegressionKeys.class);
```

Note that these methods require the specification of the Person.RegressionKeys inner enum class. As seen in the Person class of the Demo07 demo example model:

```java
// ---------------------------------------------------------------------   
// implements IObjectSource for use with Regression classes   
// ---------------------------------------------------------------------   
  
  
public enum RegressionKeys {   
  
    gender,   
    workState,   
  
}   
  
  
public Object getObjectValue(Enum<?> variableID) {   
  
    switch ((RegressionKeys) variableID) {   
  
    //For marriage regression   
    case gender:   
        return gender;   
    case workState:   
        return workState;   
    default:   
        throw new IllegalArgumentException("Unsupported regressor " + variableID.name());   
  
    }   
  
}
```

Just like the *LinearRegression* class, there are several version of the *getProbability()* and *event()* methods that cater for different input arguments:

```java
boolean event(IDoubleSource, Class<T>);  
boolean event(IDoubleSource, Class<T>, IObjectSource, Class<U>);  
boolean event(Map<String, Double>);  
boolean event(Object);  
  
double getProbability(IDoubleSource, Class<T>);  
double getProbability(IDoubleSource, Class<T>, IObjectSource, Class<U>);  
double getProbability(Map<String, Double>);  
double getProbability(Object);
```

The different versions employ the corresponding methods from the *LinearRegression* class to calculate the regression score, so their usage follows the same conventions outlined in the Javadocs extract in section 1.2.

# 2. Multinomial logit and probit regression objects

Multinomial logit and probit regressions are used to determine the outcome of random events, where the outcome is taken from a finite set of possible outcomes. Respectively, they are the multi-outcome analogues of the logit and probit regressions, which is only suitable at modelling binary outcomes. In the case for N possible outcomes, it works by comparing the logistic or probit transform of the linear regression scores for N-1 outcomes, with the Nth outcome deemed to have a score of 0. From this, it creates relative probabilities of outcomes, which can then be sampled to determine which of the N outcomes occurs.

The following section discusses *MultiProbitRegression* objects, however *MultiLogitRegression* objects are used in the same way, the only difference being that the logistic transform is used to map the linear regression score to a probability, instead of the probit transform.

## 2.1 Creation of the regression objects

The creation of *MultiProbitRegression* objects are slightly more involved as the *MultiProbitRegression* class accepts a HashMap of *MultiKeyCoefficientMaps* (each *MultiKeyCoefficientMap* stores regression coefficients corresponding to a unique outcome), so we need to create the Hashmap first.

Imagine we want to create a *MultiProbitRegression* object to model a random outcome that could have three possible states, it is necessary to supply two *MultiKeyCoefficientMaps* representing two sets of coefficients to model two of the three possible states, whilst the third outcome is considered the 'default' mode.

In the example below, we model the education level of agents in a simulation by specifying the regression coefficients for low and high education levels, with medium education as the default outcome. After first creating the *MultiKeyCoefficientMaps* of the two sets of regression coefficients, possibly taking these from Microsoft Excel .xls files as described in section 1.1, we then create a HashMap whose keys map an outcome (an *Education* enum constant representing the education level) to the corresponding regression coefficients. The *MultiProbitRegression* object is then created in the following way:

```java
// Regression Coefficients  
MultiKeyCoefficientMap coeffEducationLow = ExcelAssistant.loadCoefficientMap                                                                                    ("input/reg_education.xls", "Low", 1, 1);  
  
MultiKeyCoefficientMap coeffEducationHigh = ExcelAssistant.loadCoefficientMap                                                                                ("input/reg_education.xls", "High", 1, 1);  
  
// Create HashMap to hold the regression coefficient MultiKeyCoefficientMaps  
HashMap<Education, MultiKeyCoefficientMap> educationCoefficientMap = new HashMap<Education,                                                                                     MultiKeyCoefficientMap>();  
  
educationCoefficientMap.put(Education.Low, coeffEducationLow);  
educationCoefficientMap.put(Education.High, coeffEducationHigh);  
  
// Create the MultiProbitRegression objectMultiProbitRegression regEducationLevel = new MultiProbitRegression<Education>                                                                                                (educationCoefficientMap);
```

## 2.2 How to use the regression objects

The outcome of an event modelled by the *MultiProbitRegression* object is determined in the following way for a *Person* object 'ross':

```java
Education education = Parameters.getRegEducationLevel().eventType(IDoubleSource ross, Person.Regressors.class, Education.class);
```

Note that the *Person* class implements the *IDoubleSource* interface, which is how the value of the regressors ('regression covariates') are passed to the *MultiProbitRegression* object. An example of how this might be implemented in the Person class is in section 1.2, although the cases in the *getDoubleValue()* method must correspond to the regressors (covariates) used in the regression. The last argument specifies the return type T of the *eventType()* method and should always match the type on the left hand side.

Similarly to Linear, Logit and Probit regression classes, there are several version of the *eventType()* method depending on the input arguments:

```java
T eventType(IDoubleSource, Class<E>, Class<T>);  
T eventType(Map<String, Double>);  
T eventType(Object);
```

The different versions employ the corresponding methods from the *LinearRegression* class to calculate the regression scores of each outcome, which are subsequently used to calculate the probit transforms of each outcome, so their usage follows the same conventions outlined in the Javadocs referenced in section 1.2.

# 3. Bootstrap methods to address parameter uncertainty

The sources of uncertainty within a simulation model are discussed in the [Uncertainty analysis](https://www.microsimulation.ac.uk/jas-mine/resources/focus/uncertainty-analysis/) page. In order to address the issue of parameter uncertainty, JAS-mine provides methods to 'bootstrap' the regression coefficients of the model easily. Bootstrapping involves sampling the set of regression coefficients of a regression object from a multivariate normal distribution whose vector of expected values (means) are the set of regression coefficients estimated from the data, with the covariance matrix derived from the statistical error of the estimates.

The new sample of ('bootstrapped') regression coefficients can then be used in a simulation run and the output recorded. The process can then be repeated by sampling a new set of bootstrapped regression coefficients to be used in another simulation run. By repeating this many times, an understanding of how parameter uncertainty affects the dynamics of the model can be developed, and estimates of the uncertainty of the model evolution can be quantified and visualised as in the Figure of the [Uncertainty analysis](https://www.microsimulation.ac.uk/jas-mine/resources/focus/uncertainty-analysis/) page. The [MultiRun class](https://www.microsimulation.ac.uk/jas-mine/resources/cookbook/the-multirun-class/) can be used to execute the repeated run of simulations, as described in the tutorial [How to run a simulation many times (design of experiments)](https://www.microsimulation.ac.uk/jas-mine/resources/tutorials/run-a-simulation-many-times/).

## 3.1 Linear or Binary choice (Logit / Probit) bootstrapping

There are two methods to perform bootstrapping on a single set of regression coefficients, corresponding to a Linear, Logit or Probit regression class. The difference between the use of each method depends on if you want to submit the regression coefficients and covariance matrix as separate MultiKeyCoefficientMaps to the bootstrap method, or whether you have a single MultiKeyCoefficientMap containing the numbers for both coefficients and covariance matrix.

The example below demonstrated how to bootstrap regression coefficients, where the covariance matrix is passed to the method as a separate argument:

```java
// Create MultiKeyCoefficientMaps from Excel spreadsheet containing separate worksheets  
MultiKeyCoefficientMap coeffParticipationMales = ExcelAssistant.loadCoefficientMap                                                    ("input/reg_participationMales.xls","RegressionCoefficients", 1, 1);  
  
  
MultiKeyCoefficientMap covarianceParticipationMales = ExcelAssistant.loadCoefficientMap                                                    ("input/reg_participationMales.xls", "CovarianceMatrix", 1, 10);  
   
// Call the bootstrap method for separate coefficient and covariance matrix MultiKeyCoefficientMaps  
MultiKeyCoefficientMap newCoeffParticipationMales = RegressionUtils.bootstrap(coeffParticipationMales,                                                                         covarianceParticipationMales);  
  
// Create Regression object from the new bootstrapped regression coefficients  
ProbitRegression regParticipationMales = new ProbitRegression(newCoeffParticipationMales);
```

The resulting newCoeffParticipationMales object contains the new regression coefficients to be passed to the ProbitRegression object. Note that the covariance matrix is a 10 by 10 matrix, which can be seen by the 10 values columns specified in the loadCoefficientMap function call to create the covarianceParticipationMales object.

The alternative bootstrap method only takes one MultiKeyCoefficientMap argument and relies upon the naming of the keys and values of the map. Considering the map as having been created from an Excel spreadsheet, the key column on the left hand side of the Excel worksheet must be titled 'REGRESSOR' and contain the names of all the covariates. To the right of the 'REGRESSOR' column, there must be a column containing the regression coefficients called 'COEFFICIENT', and a separate column named after each covariate that holds the covariance data between the row covariate and the column covariate. This means that when loading the data from the Excel worksheet using the .loadCoefficientMap, the number of key columns will be 1, whilst the number of values columns will be 11 (one for the regression coefficients, and 10 for the covariance matrix data), as can be seen in the example code below:

```java
// Create MultiKeyCoefficientMap containing both regression coefficients and covariance matrix  
MultiKeyCoefficientMap coeffsAndCovariance_ParticipationMales = ExcelAssistant.loadCoefficientMap                                    ("input/reg_participationMales.xls", "coeffs_And_Covariance", 1, 11);  
  
// Call the bootstrap method for combined regression coefficient and covariance matrix  
MultiKeyCoefficientMap newCoeffParticipationMales = RegressionUtils.bootstrap(coeffsAndCovariance_ParticipationMales);  
  
// Create Regression object from the new bootstrapped regression coefficients  
ProbitRegression regParticipationMales = new ProbitRegression(newCoeffParticipationMales);
```

The probit regression object is created in the same way as before.

## 3.2 Multinomial bootstrapping (for Multinomial Logit / Probit Regressions)

The bootstrap method for the case of multinomial regression is called 'bootstrapMultinomialRegression', and the method returns an object that is required by the constructor methods of the MultiLogitRegression or MultiProbitRegression classes in order to create the regression objects. As discussed in section 2.1, this object is a Map whose keys are enum constants representing the outcome of the multinomial regression, and whose values are MultiKeyCoefficientMaps storing the set of regression coefficients for the particular outcome key. The bootstrapMultinomialRegression method bootstraps the sets of regression coefficients in each MultiKeyCoefficientMap.

There are two input arguments to the bootstrapMultinomialRegression method: 1) the original outcome-coefficients Map that would normally be used to directly construct a multinomial regression object, 2) a MultiKeyCoefficientMap storing the covariance matrix data, combined for all outcomes. Note that in this case, the covariance matrix must contain all the cross-variance terms for all outcomes and all covariates – to repeat, only one covariance matrix is used and it covers all outcomes of the multinomial regression. This means that the (square) covariance matrix has dimensions of the size (N-1) times the number of covariates per outcome, or N times the number of covariates per outcome in the case where the base ('default') outcome is specified. (Note, the same covariates should appear for each outcome).

The use of the bootstrapMultinomialRegression method is demonstrated below:

```java
// Load Low Education Outcome regression coefficients from Excel spreadsheet  
MultiKeyCoefficientMap coeffEducationLow = ExcelAssistant.loadCoefficientMap                                                                                    ("input/reg_education.xls", "Low", 1, 1);  
// Load High Education Outcome regression coefficients     MultiKeyCoefficientMap coeffEducationHigh = ExcelAssistant.loadCoefficientMap                                                                                ("input/reg_education.xls", "High", 1, 1);  
// Create Outcome-Coefficients Map        
Map<Education, MultiKeyCoefficientMap> coeffEducationLowHighMap = new HashMap<Education,                                                                                     MultiKeyCoefficientMap>();  
  
coeffEducationLowHighMap.put(Education.Low, coeffEducationLow);  
coeffEducationLowHighMap.put(Education.High, coeffEducationHigh);  
  
// Load Covariance Matrix from Excel spreadsheet (combined data for Low and High Education covariances  
// There are 2 outcomes (Low and High Education), and the same 8 covariates for each outcome, hence  
// the number of values columns in the Excel worksheet is 16  
MultiKeyCoefficientMap educationLowHighCombinedCovariance = ExcelAssistant.loadCoefficientMap                                        ("input/reg_education.xls", "Covariance", 1, 16);  
  
// Bootstrap the regression coefficients for all outcomes  
Map<Education, MultiKeyCoefficientMap> newCoeffEducationLowHighMap =                                                         RegressionUtils.boostrapMultinomialRegression(coeffEducationLowHighMap,                                                     educationLowHighCombinedCovariance, Education.class);  
  
// Create regression object from the new bootstrapped regression coefficients in the outcome-  
// coefficient map        
MultiProbitRegression<Education> regEducationLevel = new MultiProbitRegression<Education>                                                                                 (newCoeffEducationLowHighMap);
```

Note that the resulting Map returned by the bootstrapMultinomialRegression method is then used as the input argument to construct the MultiProbitRegression object.
