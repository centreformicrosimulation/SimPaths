package simpaths.model.lifetime_incomes;

import simpaths.model.BenefitUnit;
import simpaths.model.Household;
import simpaths.model.Person;
import simpaths.model.enums.Gender;

import java.util.Collections;
import java.util.List;

public class LifetimeIncomeImputation {

    Integer year;
    List<BirthCohort> cohorts;

    public LifetimeIncomeImputation(int year, List<BirthCohort> cohorts) {
        this.year = year;
        this.cohorts = cohorts;
        for (BirthCohort cohort : cohorts) {

            List<Individual> individuals = cohort.getIndividuals();
            Collections.sort(individuals);
            cohort.setIndividuals(individuals);
        }
    }

    public void matchDonorProfile(Person person) {

        // identify equivalised household income of person
        Household household = person.getBenefitUnit().getHousehold();
        double disposableIncomePerAnnum = 0.0;
        double equivalenceScale = 0.0;
        boolean firstAdult = true;
        for (BenefitUnit benefitUnit : household.getBenefitUnits()) {
            for (Person pp : benefitUnit.getMembers()) {
                disposableIncomePerAnnum += pp.getDisposableIncomeMonthly() * 12.0;
                if (pp.getDag()>13) {
                    if (firstAdult) {
                        equivalenceScale += 1.0;
                        firstAdult = false;
                    }
                    else {
                        equivalenceScale += 0.5;
                    }
                }
                else {
                    equivalenceScale += 0.3;
                }
            }
        }
        double disposableIncome = disposableIncomePerAnnum / equivalenceScale;

        // match birth cohort (birth year and gender)
        int birthYear = year - person.getDag();
        Gender gender = person.getDgn();
        BirthCohort targetCohort = null;
        for (BirthCohort cohort : cohorts) {

            if (cohort.getBirthYear() == birthYear && cohort.getGender() == gender) {
                targetCohort = cohort;
                break;
            }
        }

        // match income
        if (targetCohort == null)
            throw new IllegalArgumentException("No cohort found for birth year " + birthYear + " and gender " + gender);
        List<Individual> individuals = targetCohort.getIndividuals();
        int lwr = 0;
        Double lwrValue = null, uprValue = null;
        int upr = individuals.size()-1;
        while (lwr < upr-1) {
            int tstIndex = (upr + lwr) / 2;
            double tstValue = individuals.get(tstIndex).getAnnualIncome(year).getValue() - disposableIncome;
            if (tstValue < 0.0) {
                lwr = tstIndex;
                lwrValue = tstValue;
            } else {
                upr = tstIndex;
                uprValue = tstValue;
            }
        }
        if (uprValue==null || lwrValue==null)
            throw new IllegalArgumentException("No lifetime income donor identified for individual");

        // write match to person
        if (uprValue < -lwrValue) {
            person.setLtIncomeDonor(individuals.get(upr));
        }
        else {
            person.setLtIncomeDonor(individuals.get(lwr));
        }
    }
}
