# Enums

An enum defines a fixed set of named values. In SimPaths, enums represent categories such as country, education level and labour-market status. They also identify scheduled processes and regressions.

## A SimPaths example

The model uses `simpaths.model.enums.Gender`, whose categories are `Male` and `Female`. A minimal usage example is:

```java
import simpaths.model.enums.Gender;

Gender category = Gender.Female;
boolean isFemale = category == Gender.Female;
```

This uses the existing enum; it does not create a new definition or describe every attribute of the `Person` entity. See the [Gender source](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/model/enums/Gender.java) for its complete implementation.

## Persistence and regression coding

Persisted enum fields in SimPaths use string-valued categories. For example, the ethnicity field in `Person` is declared as:

```java
@Enumerated(EnumType.STRING)
private Ethnicity demEthnC6;
```

The initial-population parser maps input category codes to the enum names. Renaming a persisted category can make existing databases incompatible. Changing a category's meaning also requires checking the input parser, regressors and parameter workbooks.

Use the variable codebook and [Introduce a New Variable](../developer-guide/how-to/new-variable.md) guide before adding categories. The [Java enum guide](https://dev.java/learn/classes-objects/enums/) explains the language feature independently of SimPaths.
