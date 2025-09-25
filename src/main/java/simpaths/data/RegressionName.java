package simpaths.data;


import microsim.statistics.regression.RegressionType;

public enum RegressionName {

    ChildcareC1b(RegressionType.Linear),

    EducationE1a(RegressionType.Probit),
    EducationE1b(RegressionType.Probit),
    EducationE2a(RegressionType.GenOrderedLogit),

    FertilityF1a(RegressionType.Probit),
    FertilityF1b(RegressionType.Probit),

    PartnershipU1a(RegressionType.Probit),
    PartnershipU1b(RegressionType.Probit),
    PartnershipU2b(RegressionType.Probit),

    HealthH1a(RegressionType.GenOrderedLogit),
    HealthH1b(RegressionType.GenOrderedLogit),
    HealthH2b(RegressionType.Probit),

    HealthHM1Level(RegressionType.Linear),
    HealthHM2LevelMales(RegressionType.Linear),
    HealthHM2LevelFemales(RegressionType.Linear),
    HealthHM1Case(RegressionType.Logit),
    HealthHM2CaseMales(RegressionType.Logit),
    HealthHM2CaseFemales(RegressionType.Logit),

    HealthMCS1(RegressionType.Linear),
    HealthMCS2Males(RegressionType.Linear),
    HealthMCS2Females(RegressionType.Linear),

    HealthPCS1(RegressionType.Linear),
    HealthPCS2Males(RegressionType.Linear),
    HealthPCS2Females(RegressionType.Linear),

    LifeSatisfaction1(RegressionType.Linear),
    LifeSatisfaction2Males(RegressionType.Linear),
    LifeSatisfaction2Females(RegressionType.Linear),

    HealthEQ5D(RegressionType.Linear),

    RMSE(RegressionType.Linear),

    SocialCareS1a(RegressionType.Probit),
    SocialCareS1b(RegressionType.Linear),
    SocialCareS2a(RegressionType.Probit),
    SocialCareS2b(RegressionType.Probit),
    SocialCareS2c(RegressionType.MultinomialLogit),
    SocialCareS2d(RegressionType.Probit),
    SocialCareS2e(RegressionType.MultinomialLogit),
    SocialCareS2f(RegressionType.MultinomialLogit),
    SocialCareS2g(RegressionType.Linear),
    SocialCareS2h(RegressionType.Linear),
    SocialCareS2i(RegressionType.Linear),
    SocialCareS2j(RegressionType.Linear),
    SocialCareS2k(RegressionType.Linear),
    SocialCareS3a(RegressionType.Probit),
    SocialCareS3b(RegressionType.Probit),
    SocialCareS3c(RegressionType.Probit),
    SocialCareS3d(RegressionType.MultinomialLogit),
    SocialCareS3e(RegressionType.Linear),

    UnemploymentU1a(RegressionType.Probit),
    UnemploymentU1b(RegressionType.Probit),
    UnemploymentU1c(RegressionType.Probit),
    UnemploymentU1d(RegressionType.Probit),

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
