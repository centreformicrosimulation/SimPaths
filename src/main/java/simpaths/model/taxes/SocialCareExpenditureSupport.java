package simpaths.model.taxes;

import simpaths.data.Parameters;

public class SocialCareExpenditureSupport {

    double supportPerMonth;
    double migPerWeek;
    double lowerCapitalLimit;
    double upperCapitalLimit;


    public SocialCareExpenditureSupport(){}
    public SocialCareExpenditureSupport(int year, boolean flagCouple, boolean flagSPA, double socialCareCostPerMonth, double disposableIncomePerMonth, double liquidWealth) {

        supportPerMonth = 0.0;
        if (socialCareCostPerMonth > 0.01) {

            if (flagCouple) {
                if (flagSPA) {
                    migPerWeek = Parameters.getSocialCarePolicyValue(year, "mig_couple_from_spa");
                } else {
                    migPerWeek = Parameters.getSocialCarePolicyValue(year, "mig_couple_under_spa");
                }
            } else {
                if (flagSPA) {
                    migPerWeek = Parameters.getSocialCarePolicyValue(year, "mig_single_from_spa");
                } else {
                    migPerWeek = Parameters.getSocialCarePolicyValue(year, "mig_single_under_spa");
                }
            }
            lowerCapitalLimit = Parameters.getSocialCarePolicyValue(year, "lower_capital_limit");
            upperCapitalLimit = Parameters.getSocialCarePolicyValue(year, "upper_capital_limit");
            if (liquidWealth < upperCapitalLimit) {

                double incomeMeansTestPerMonth = Math.max(0.0, disposableIncomePerMonth - migPerWeek * Parameters.WEEKS_PER_MONTH);
                double wealthMeansTestPerMonth = Math.max(0.0, liquidWealth - lowerCapitalLimit) / 250.0 * Parameters.WEEKS_PER_MONTH;
                supportPerMonth = Math.max(0.0, socialCareCostPerMonth - incomeMeansTestPerMonth - wealthMeansTestPerMonth);
            }
        }
    }

    public double getSupportPerMonth() {
        return supportPerMonth;
    }
}
