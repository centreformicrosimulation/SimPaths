package simpaths.data;


public enum RegressionName {

    ChildcareC1b(RegressionTypejjj.Linear),

    EducationE1a(RegressionTypejjj.StandardProbit),
    EducationE1b(RegressionTypejjj.StandardProbit),
    EducationE2a(RegressionTypejjj.OrderedProbit),

    FertilityF1a(RegressionTypejjj.AdjustedStandardProbit),
    FertilityF1b(RegressionTypejjj.AdjustedStandardProbit),

    PartnershipU1a(RegressionTypejjj.AdjustedStandardProbit),
    PartnershipU1b(RegressionTypejjj.AdjustedStandardProbit),
    PartnershipU2b(RegressionTypejjj.ReversedProbit),

    HealthH1a(RegressionTypejjj.GeneralisedOrderedLogit),
    HealthH1b(RegressionTypejjj.GeneralisedOrderedLogit),
    HealthH2b(RegressionTypejjj.StandardProbit),

    HealthHM1Level(RegressionTypejjj.Linear),
    HealthHM2LevelMales(RegressionTypejjj.Linear),
    HealthHM2LevelFemales(RegressionTypejjj.Linear),
    HealthHM1Case(RegressionTypejjj.Logit),
    HealthHM2CaseMales(RegressionTypejjj.Logit),
    HealthHM2CaseFemales(RegressionTypejjj.Logit),

    RMSE(RegressionTypejjj.RMSE),

    SocialCareS1a(RegressionTypejjj.StandardProbit),
    SocialCareS1b(RegressionTypejjj.Linear),
    SocialCareS2a(RegressionTypejjj.StandardProbit),
    SocialCareS2b(RegressionTypejjj.StandardProbit),
    SocialCareS2c(RegressionTypejjj.MultinomialLogit),
    SocialCareS2d(RegressionTypejjj.StandardProbit),
    SocialCareS2e(RegressionTypejjj.MultinomialLogit),
    SocialCareS2f(RegressionTypejjj.MultinomialLogit),
    SocialCareS2g(RegressionTypejjj.Linear),
    SocialCareS2h(RegressionTypejjj.Linear),
    SocialCareS2i(RegressionTypejjj.Linear),
    SocialCareS2j(RegressionTypejjj.Linear),
    SocialCareS2k(RegressionTypejjj.Linear),
    SocialCareS3a(RegressionTypejjj.StandardProbit),
    SocialCareS3b(RegressionTypejjj.StandardProbit),
    SocialCareS3c(RegressionTypejjj.StandardProbit),
    SocialCareS3d(RegressionTypejjj.MultinomialLogit),
    SocialCareS3e(RegressionTypejjj.Linear),

    UnemploymentU1a(RegressionTypejjj.ReversedProbit),
    UnemploymentU1b(RegressionTypejjj.ReversedProbit),
    UnemploymentU1c(RegressionTypejjj.ReversedProbit),
    UnemploymentU1d(RegressionTypejjj.ReversedProbit),

    WagesMales(RegressionTypejjj.Linear),
    WagesMalesE(RegressionTypejjj.Linear),
    WagesMalesNE(RegressionTypejjj.Linear),
    WagesFemales(RegressionTypejjj.Linear),
    WagesFemalesE(RegressionTypejjj.Linear),
    WagesFemalesNE(RegressionTypejjj.Linear);

    private final RegressionTypejjj type;

    RegressionName(RegressionTypejjj tt) {
        type = tt;
    }

    public RegressionTypejjj getType() {
        return type;
    }
}
