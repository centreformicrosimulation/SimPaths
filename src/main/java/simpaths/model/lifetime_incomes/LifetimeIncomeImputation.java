package simpaths.model.lifetime_incomes;

import simpaths.model.BenefitUnit;
import simpaths.model.Household;
import simpaths.model.Person;
import simpaths.model.enums.Gender;

import java.util.ArrayList;
import java.util.List;

public class LifetimeIncomeImputation {

    Integer year;
    Integer endAge = null;
    List<BirthCohort> cohorts;

    public LifetimeIncomeImputation(int year, List<BirthCohort> cohorts) {
        this.year = year;
        this.cohorts = cohorts;
        IndividualComparator comparator = new IndividualComparator(year);
        for (BirthCohort cohort : cohorts) {
            
            if (endAge==null)
                endAge = cohort.getEndAge();

            if (cohort.getBirthYear() <= year && cohort.getBirthYear() + endAge >= year) {

                List<Individual> individuals = new ArrayList<>(cohort.getIndividuals());
                individuals.sort(comparator);
                cohort.setSortedIndividuals(individuals);
            }
        }
    }

    public void matchDonorProfiles(Household household) {

        // identify target equivalised household income
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
        double targetIncome = disposableIncomePerAnnum / equivalenceScale;

        for (BenefitUnit benefitUnit : household.getBenefitUnits()) {

            for (Person person : benefitUnit.getMembers()) {

                // match birth cohort (birth year and gender)
                List<Individual> individuals = getIndividuals(person);
                if (individuals.isEmpty())
                    throw new IllegalArgumentException("No individuals found for person " + person.getId());

                // match income
                int lwr = 0, upr = individuals.size()-1;
                double lwrValue = individuals.get(lwr).getAnnualIncome(year).getValue() - targetIncome;
                if (lwrValue > 0.0) {
                    // lower bound
                    person.setLtIncomeDonor(individuals.get(lwr));
                }
                else {

                    double uprValue = individuals.get(upr).getAnnualIncome(year).getValue() - targetIncome;
                    if (uprValue < 0.0) {
                        // upper bound
                        person.setLtIncomeDonor(individuals.get(upr));
                    }
                    else {

                        while (lwr < upr-1) {
                            int tstIndex = (upr + lwr) / 2;
                            double tstValue = individuals.get(tstIndex).getAnnualIncome(year).getValue() - targetIncome;
                            if (tstValue < 0.0) {
                                lwr = tstIndex;
                                lwrValue = tstValue;
                            } else {
                                upr = tstIndex;
                                uprValue = tstValue;
                            }
                        }
                        if (uprValue < -lwrValue) {
                            person.setLtIncomeDonor(individuals.get(upr));
                        }
                        else {
                            person.setLtIncomeDonor(individuals.get(lwr));
                        }
                    }
                }
            }
        }
    }

    private List<Individual> getIndividuals(Person person) {
        int birthYear = year - Math.min(person.getDag(),endAge);
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
        List<Individual> individuals = targetCohort.getSortedIndividuals();
        return individuals;
    }
}
