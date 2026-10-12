package simpaths.model.person;

import simpaths.data.ManagerRegressions;
import simpaths.data.Parameters;
import simpaths.data.RegressionName;
import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Les_c4;
import simpaths.model.enums.PensionContribRate;
import simpaths.model.enums.TimeSeriesVariable;

public class PrivatePension {

    private boolean memberPP;                            //indicator that is active member of personal pension in current year
    private boolean memberOP;                            //indicator that is active member of occupational pension in current year
    private double contRateOPEe;                         //contribution rate of employee to occupational pension
    private double contRateOPEr;                         //contribution rate of employer to occupational pension
    private double contRatePP;                           //contribution rate to personal pension
    private double wealth;                               //wealth held in private pensions
    private boolean isRetired;                           //indicator of whether the person is retired
    private double lumpSumPayment;                       //value of lump sum payment at time of pension access
    private double pensionIncomeAnnual;                 //private pension annuity income per year

    public PrivatePension() {
        memberPP = false;
        memberOP = false;
        isRetired = false;
        contRateOPEr = 0.0;
        contRateOPEe = 0.0;
        contRatePP = 0.0;
        wealth = 0.0;
        pensionIncomeAnnual = 0.0;
        lumpSumPayment = 0.0;
    }

    public PrivatePension(PrivatePension original) {
        this.memberPP = original.memberPP;
        this.memberOP = original.memberOP;
        this.isRetired = original.isRetired;
        this.contRateOPEr = original.contRateOPEr;
        this.contRateOPEe = original.contRateOPEe;
        this.contRatePP = original.contRatePP;
        this.wealth = original.wealth;
        this.pensionIncomeAnnual = original.pensionIncomeAnnual;
        this.lumpSumPayment = original.lumpSumPayment;
    }


    /**************************************************************
     * DEDICATED METHODS
     **************************************************************/

    public void membership(Person person, boolean memberOPL1, boolean memberPPL1, double innov1, double innov2) {

        if (Les_c4.EmployedOrSelfEmployed.equals(person.getLabC4())) {

            memberOP = ManagerRegressions.getAnnualEventFromBiennial(person, memberOPL1, innov1, RegressionName.WealthPensionPW1a, RegressionName.WealthPensionPW1b);
            memberPP = ManagerRegressions.getAnnualEventFromBiennial(person, memberPPL1, innov2, RegressionName.WealthPensionPW2a, RegressionName.WealthPensionPW2b);
        } else {

            memberOP = false;
            memberPP = false;
        }
    }

    public void contributionRates(Person person, double innov1, double innov2, double innov3, double innov4, double innov5) {

        if (memberOP) {

            // consider employee contributions first
            PensionContribRate pcrDiscrete = ManagerRegressions.getEvent(person, RegressionName.WealthPensionPW1c, innov1);
            if (pcrDiscrete.equals(PensionContribRate.Other)) {

                Double val;
                double score, rmse, gauss;
                score = Parameters.getRegPW1d().getScore(person, Person.Variables.class);
                rmse = Parameters.getRMSEForRegression("PW1d");
                gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innov2);
                val = Math.sinh(score + gauss * rmse);
                if (!Parameters.isFinite(val))
                    throw new RuntimeException("contRateOPEe is not finite");
                val = Math.min(100.0, Math.max(0.0, val));
                int test = (int)Math.round(val);
                while (test == 0 || test == 3 || test == 5) {

                    // need to re-draw
                    if (innov2 > 0.5) {
                        innov2 = (innov2 - 0.5) / 0.5;
                    } else {
                        innov2 = innov2 / 0.5;
                    }
                    gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innov2);
                    val = Math.sinh(score + gauss * rmse);
                    if (!Parameters.isFinite(val))
                        throw new RuntimeException("contRateOPEe is not finite");
                    val = Math.min(100.0, Math.max(0.0, val));
                    test = (int)Math.round(val);
                }
                contRateOPEe = val / 100.0;
            } else {
                contRateOPEe = pcrDiscrete.getValue() / 100.0;
            }

            // consider employer contributions
            pcrDiscrete = ManagerRegressions.getEvent(person, RegressionName.WealthPensionPW1e, innov3);
            if (pcrDiscrete.equals(PensionContribRate.Other)) {

                Double val;
                double score, rmse, gauss;
                score = Parameters.getRegPW1f().getScore(person, Person.Variables.class);
                rmse = Parameters.getRMSEForRegression("PW1f");
                gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innov4);
                val = Math.sinh(score + gauss * rmse);
                if (!Parameters.isFinite(val))
                    throw new RuntimeException("contRateOPEr is not finite");
                val = Math.min(100.0, Math.max(0.0, val));
                int test = (int)Math.round(val);
                while (test == 0 || test == 3 || test == 5) {

                    // need to re-draw
                    if (innov4 > 0.5) {
                        innov4 = (innov4 - 0.5) / 0.5;
                    } else {
                        innov4 = innov4 / 0.5;
                    }
                    gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innov4);
                    val = Math.sinh(score + gauss * rmse);
                    if (!Parameters.isFinite(val))
                        throw new RuntimeException("contRateOPEr is not finite");
                    val = Math.min(100.0, Math.max(0.0, val));
                    test = (int)Math.round(val);
                }
                contRateOPEr = val / 100.0;
            } else {
                contRateOPEr = pcrDiscrete.getValue() / 100.0;
            }
        } else {

            contRateOPEe = 0.0;
            contRateOPEr = 0.0;
        }
        if (memberPP) {

            double score, rmse, gauss;
            score = Parameters.getRegPW2c().getScore(person, Person.Variables.class);
            rmse = Parameters.getRMSEForRegression("PW2c");
            gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innov5);
            contRatePP = Math.exp(score + gauss * rmse);
            if (!Parameters.isFinite(contRatePP))
                throw new RuntimeException("contRatePP is not finite");
            contRatePP = Math.min(100.0, Math.max(0.0, contRatePP)) / 100.0;
        } else {

            contRatePP = 0.0;
        }
    }

    public void projectWealth(double wealthL1, double employmentIncomeAnnual, double growthRate, Les_c4 labC4) {

        wealth = wealthL1 * (1.0 + growthRate);
        if (!Les_c4.EmployedOrSelfEmployed.equals(labC4)) {

            contRateOPEe = 0.0;
            contRateOPEr = 0.0;
            contRatePP = 0.0;
        } else {

            if (!memberOP) {
                contRateOPEe = 0.0;
                contRateOPEr = 0.0;
            }
            if (!memberPP) {
                contRatePP = 0.0;
            }
            wealth += (contRatePP + contRateOPEe + contRateOPEr) * employmentIncomeAnnual;
        }
    }

    public double projectInPaymentPension(int year, double pensionIncomePerYearL1) {

        double inflationIndexL1 = Parameters.getTimeSeriesValue(year-1, TimeSeriesVariable.Inflation);
        double inflationIndex = Parameters.getTimeSeriesValue(year, TimeSeriesVariable.Inflation);
        isRetired = true;
        pensionIncomeAnnual = pensionIncomePerYearL1 * inflationIndexL1 / inflationIndex;
        return pensionIncomeAnnual / 12.0;
    }

    public double projectPensionAccess(int age, Gender male, int year, double wealth) {

        lumpSumPayment = Math.min(wealth * Parameters.pensionLumpSumShare, Parameters.pensionLumpSumCap);
        Double annuityPrice = Parameters.annuityRates.getAnnuityRateByGenderAgeYear(male, age, year);
        if (annuityPrice == null)
            throw new RuntimeException("Annuity price not found for " + male + " " + age + " " + year);
        pensionIncomeAnnual = (wealth - lumpSumPayment) / annuityPrice;
        return pensionIncomeAnnual / 12.0;
    }


    /**************************************************************
     * GETTERS AND SETTERS
     **************************************************************/

    public double getLumpSumPayment() {
        return lumpSumPayment;
    }

    public void setLumpSumPayment(double lumpSumPayment) {
        this.lumpSumPayment = lumpSumPayment;
    }

    public boolean isRetired() {
        return isRetired;
    }

    public void setRetired(boolean retired) {
        isRetired = retired;
    }

    public double getPensionIncomeAnnual() {
        return pensionIncomeAnnual;
    }

    public void setPensionIncomeAnnual(double pensionIncomeAnnual) {
        this.pensionIncomeAnnual = pensionIncomeAnnual;
    }

    public boolean isMemberPP() {
        return memberPP;
    }

    public void setMemberPP(boolean memberPP) {
        this.memberPP = memberPP;
    }

    public boolean isMemberOP() {
        return memberOP;
    }

    public void setMemberOP(boolean memberOP) {
        this.memberOP = memberOP;
    }

    public double getContRateOPEr() {
        return contRateOPEr;
    }

    public void setContRateOPEr(double contRateOPEr) {

        this.contRateOPEr = contRateOPEr;
        memberOP = (contRateOPEe + contRateOPEr > 0.001);
    }

    public double getContRateOPEe() {
        return contRateOPEe;
    }

    public void setContRateOPEe(double contRateOPEe) {

        this.contRateOPEe = contRateOPEe;
        memberOP = (contRateOPEe + contRateOPEr > 0.001);
    }

    public double getContRatePP() {
        return contRatePP;
    }

    public void setContRatePP(double contRatePP) {

        this.contRatePP = contRatePP;
        memberPP = (contRatePP > 0.001);
    }

    public double getWealth() {
        return wealth;
    }

    public void setWealth(double wealth) {
        this.wealth = wealth;
    }

}
