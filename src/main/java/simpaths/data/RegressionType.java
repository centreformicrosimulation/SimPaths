package simpaths.data;

public enum RegressionType {
    StandardProbit,           // binary (0,1)
    AdjustedStandardProbit,   // binary (0,1) with adjusted intercept
    ReversedProbit,           // binary (1,0)
    MultinomialLogit,
    Linear,
    RMSE,
    OrderedProbit,
}
