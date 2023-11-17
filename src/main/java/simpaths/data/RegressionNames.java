package simpaths.data;

import simpaths.model.enums.IntegerValuedEnum;

public enum RegressionNames implements IntegerValuedEnum {

    EducationE1a(1),
    EducationE1b(1),
    EducationE2a(2),
    HealthH1a(2),
    HealthH1b(2),
    HealthH2b(1),
    SocialCareS2a(1),
    SocialCareS2b(1),
    SocialCareS2c(2),
    SocialCareS3c(1),
    SocialCareS3d(2),
    SocialCareS3e(3),
    PartnershipU1a(1),
    PartnershipU1b(1),
    PartnershipU2b(1),
    FertilityF1a(1),
    FertilityF1b(1),
    WagesMales(3),
    WagesMalesE(3),
    WagesMalesNE(3),
    WagesFemales(3),
    WagesFemalesE(3),
    WagesFemalesNE(3),
    ChildcareC1b(3),
    RMSE(9);

    private final int value;        // value = 1 binary, 2 multinomial, 3 gaussian, 9 RMSE

    RegressionNames(int val) {
        value = val;
    }

    @Override
    public int getValue()
    {
        return value;
    }
}
