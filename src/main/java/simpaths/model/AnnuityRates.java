package simpaths.model;

import simpaths.data.Parameters;
import simpaths.model.decisions.DecisionParams;
import simpaths.model.enums.Dcpst;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Occupancy;
import simpaths.model.decisions.DecisionParams;

public class AnnuityRates {

    private double[][][] annuityRates;      // first dim: single women / single men / couples; second dim: birth years; third dim: age

    public AnnuityRates() {

        annuityRates = new double[3][DecisionParams.maxBirthYear-DecisionParams.minBirthYear+1][Parameters.maxAge-Parameters.MIN_AGE_TO_RETIRE+1];

        evalAnnunityRates(Gender.Female);
        evalAnnunityRates(Gender.Male);
        evalAnnuityRatesCouple();
    }

    private void evalAnnunityRates(Gender gender) {

        Occupancy occupancy;
        if (Gender.Female.equals(gender)) {
            occupancy = Occupancy.Single_Female;
        } else {
            occupancy = Occupancy.Single_Male;
        }
        for (int birthYear=DecisionParams.minBirthYear; birthYear<=DecisionParams.maxBirthYear; birthYear++) {

            double annuityRate = 0.0;
            for (int age=Parameters.maxAge; age>= Parameters.MIN_AGE_TO_RETIRE; age--) {
                if (age==Parameters.maxAge) {
                    annuityRate = 1.0;
                } else {
                    annuityRate = 1.0 + annuityRate / (1.0 + Parameters.ANNUITY_RATE_OF_RETURN) *
                            (1.0 - Parameters.getMortalityProbability(gender, age, birthYear+age));
                }
                setAnnuityRates(annuityRate, occupancy, birthYear, age);
            }
        }
    }
    private void evalAnnuityRatesCouple() {

        Occupancy occupancy = Occupancy.Couple;
        for (int birthYear=DecisionParams.minBirthYear; birthYear<=DecisionParams.maxBirthYear; birthYear++) {

            double annuityRate = 0.0;
            for (int age=Parameters.maxAge; age>= Parameters.MIN_AGE_TO_RETIRE; age--) {
                if (age==Parameters.maxAge) {
                    annuityRate = 1.0;
                } else {
                    double mortM = Parameters.getMortalityProbability(Gender.Male, age, birthYear+age);
                    double mortF = Parameters.getMortalityProbability(Gender.Female, age, birthYear+age);
                    annuityRate = 1.0 + (annuityRate * (1.0 - mortM - mortF + mortM*mortF) +
                            getAnnuityRate(Occupancy.Single_Male, birthYear, age+1) * 0.5 * (mortF * (1.0 - mortM)) +
                            getAnnuityRate(Occupancy.Single_Female, birthYear, age+1) * 0.5 * (mortM * (1.0 - mortF))) /
                             (1.0 + Parameters.ANNUITY_RATE_OF_RETURN);
                }
                setAnnuityRates(annuityRate, occupancy, birthYear, age);
            }
        }
    }

    private void setAnnuityRates(double annuityRate, Occupancy occupancy, int birthYear, int age) {
        int ii;
        if (Occupancy.Single_Female.equals(occupancy)) {
            ii = 0;
        } else if (Occupancy.Single_Male.equals(occupancy)) {
            ii = 1;
        } else {
            ii = 2;
        }
        annuityRates[ii][birthYear-DecisionParams.minBirthYear][age-Parameters.MIN_AGE_TO_RETIRE] = annuityRate;
    }
    public double getAnnuityRate(Occupancy occupancy, int birthYear, int age) {
        double rate;
        int bb = Math.min( Math.max( birthYear, DecisionParams.minBirthYear), DecisionParams.maxBirthYear) - DecisionParams.minBirthYear;
        int aa = Math.min( Math.max(age, Parameters.MIN_AGE_TO_RETIRE), Parameters.maxAge) - Parameters.MIN_AGE_TO_RETIRE;
        if (Occupancy.Couple.equals(occupancy)) {
            rate = annuityRates[2][bb][aa];
        } else if (Occupancy.Single_Male.equals(occupancy)) {
            rate = annuityRates[1][bb][aa];
        } else {
            rate = annuityRates[0][bb][aa];
        }
        if (rate<1.01 && age<100)
            throw new RuntimeException("annuity rate lower than expected: " + rate);
        return rate;
    }
}
