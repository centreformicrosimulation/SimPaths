package simpaths.data;


public enum RegressionName {

    EducationE1a(RegressionType.StandardProbit),
    EducationE1b(RegressionType.StandardProbit),
    EducationE2a(RegressionType.OrderedProbit),
    HealthH1a(RegressionType.OrderedProbit),
    HealthH1b(RegressionType.OrderedProbit),
    HealthH2b(RegressionType.StandardProbit),
    SocialCareS2a(RegressionType.StandardProbit),
    SocialCareS2b(RegressionType.StandardProbit),
    SocialCareS2c(RegressionType.MultinomialLogit),
    SocialCareS3c(RegressionType.StandardProbit),
    SocialCareS3d(RegressionType.MultinomialLogit),
    SocialCareS3e(RegressionType.Linear),
    PartnershipU1a(RegressionType.AdjustedStandardProbit),
    PartnershipU1b(RegressionType.AdjustedStandardProbit),
    PartnershipU2b(RegressionType.ReversedProbit),
    FertilityF1a(RegressionType.AdjustedStandardProbit),
    FertilityF1b(RegressionType.AdjustedStandardProbit),
    WagesMales(RegressionType.Linear),
    WagesMalesE(RegressionType.Linear),
    WagesMalesNE(RegressionType.Linear),
    WagesFemales(RegressionType.Linear),
    WagesFemalesE(RegressionType.Linear),
    WagesFemalesNE(RegressionType.Linear),
    UnemploymentU1a(RegressionType.ReversedProbit),
    UnemploymentU1b(RegressionType.ReversedProbit),
    UnemploymentU1c(RegressionType.ReversedProbit),
    UnemploymentU1d(RegressionType.ReversedProbit),
    ChildcareC1b(RegressionType.Linear),
    RMSE(RegressionType.RMSE);

    private final RegressionType type;

    RegressionName(RegressionType tt) {
        type = tt;
    }

    public RegressionType getType() {
        return type;
    }
}
