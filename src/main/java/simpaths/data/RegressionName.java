package simpaths.data;


public enum RegressionName {

    ChildcareC1b(RegressionType.Linear),

    EducationE1a(RegressionType.StandardProbit),
    EducationE1b(RegressionType.StandardProbit),
    EducationE2a(RegressionType.OrderedProbit),

    FertilityF1a(RegressionType.AdjustedStandardProbit),
    FertilityF1b(RegressionType.AdjustedStandardProbit),

    PartnershipU1a(RegressionType.AdjustedStandardProbit),
    PartnershipU1b(RegressionType.AdjustedStandardProbit),
    PartnershipU2b(RegressionType.ReversedProbit),

    HealthH1a(RegressionType.GeneralisedOrderedLogit),
    HealthH1b(RegressionType.GeneralisedOrderedLogit),
    HealthH2b(RegressionType.StandardProbit),

    HealthHM1Level(RegressionType.Linear),
    HealthHM2LevelMales(RegressionType.Linear),
    HealthHM2LevelFemales(RegressionType.Linear),
    HealthHM1Case(RegressionType.Logit),
    HealthHM2CaseMales(RegressionType.Logit),
    HealthHM2CaseFemales(RegressionType.Logit),

    RMSE(RegressionType.RMSE),

    SocialCareS1a(RegressionType.StandardProbit),
    SocialCareS1b(RegressionType.Linear),
    SocialCareS2a(RegressionType.StandardProbit),
    SocialCareS2b(RegressionType.StandardProbit),
    SocialCareS2c(RegressionType.MultinomialLogit),
    SocialCareS2d(RegressionType.StandardProbit),
    SocialCareS2e(RegressionType.MultinomialLogit),
    SocialCareS2f(RegressionType.MultinomialLogit),
    SocialCareS2g(RegressionType.Linear),
    SocialCareS2h(RegressionType.Linear),
    SocialCareS2i(RegressionType.Linear),
    SocialCareS2j(RegressionType.Linear),
    SocialCareS2k(RegressionType.Linear),
    SocialCareS3a(RegressionType.StandardProbit),
    SocialCareS3b(RegressionType.StandardProbit),
    SocialCareS3c(RegressionType.StandardProbit),
    SocialCareS3d(RegressionType.MultinomialLogit),
    SocialCareS3e(RegressionType.Linear),

    UnemploymentU1a(RegressionType.ReversedProbit),
    UnemploymentU1b(RegressionType.ReversedProbit),
    UnemploymentU1c(RegressionType.ReversedProbit),
    UnemploymentU1d(RegressionType.ReversedProbit),

    WagesMales(RegressionType.Linear),
    WagesMalesE(RegressionType.Linear),
    WagesMalesNE(RegressionType.Linear),
    WagesFemales(RegressionType.Linear),
    WagesFemalesE(RegressionType.Linear),
    WagesFemalesNE(RegressionType.Linear);

    private final RegressionType type;

    RegressionName(RegressionType tt) {
        type = tt;
    }

    public RegressionType getType() {
        return type;
    }
}
