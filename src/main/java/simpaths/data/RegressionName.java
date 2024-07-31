package simpaths.data;


public enum RegressionName {

    EducationE1a(RegressionType.StandardBinomial),
    EducationE1b(RegressionType.StandardBinomial),
    EducationE2a(RegressionType.Multinomial),
    HealthH1a(RegressionType.Multinomial),
    HealthH1b(RegressionType.Multinomial),
    HealthH2b(RegressionType.StandardBinomial),
    SocialCareS2a(RegressionType.StandardBinomial),
    SocialCareS2b(RegressionType.StandardBinomial),
    SocialCareS2c(RegressionType.Multinomial),
    SocialCareS3c(RegressionType.StandardBinomial),
    SocialCareS3d(RegressionType.Multinomial),
    SocialCareS3e(RegressionType.Gaussian),
    PartnershipU1a(RegressionType.AdjustedStandardBinomial),
    PartnershipU1b(RegressionType.AdjustedStandardBinomial),
    PartnershipU2b(RegressionType.ReversedBinomial),
    FertilityF1a(RegressionType.AdjustedStandardBinomial),
    FertilityF1b(RegressionType.AdjustedStandardBinomial),
    WagesMales(RegressionType.Gaussian),
    WagesMalesE(RegressionType.Gaussian),
    WagesMalesNE(RegressionType.Gaussian),
    WagesFemales(RegressionType.Gaussian),
    WagesFemalesE(RegressionType.Gaussian),
    WagesFemalesNE(RegressionType.Gaussian),
    UnemploymentU1a(RegressionType.ReversedBinomial),
    UnemploymentU1b(RegressionType.ReversedBinomial),
    UnemploymentU1c(RegressionType.ReversedBinomial),
    UnemploymentU1d(RegressionType.ReversedBinomial),
    ChildcareC1b(RegressionType.Gaussian),
    RMSE(RegressionType.RMSE);

    private final RegressionType type;

    RegressionName(RegressionType tt) {
        type = tt;
    }

    public RegressionType getType() {
        return type;
    }
}
