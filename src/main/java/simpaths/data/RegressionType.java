package simpaths.data;

public enum RegressionType {
    StandardBinomial,           // binary (0,1)
    AdjustedStandardBinomial,   // binary (0,1) with adjusted intercept
    ReversedBinomial,           // binary (1,0)
    Multinomial,
    Gaussian,
    RMSE,
}
