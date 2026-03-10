package simpaths.model.lifetime_incomes;

import simpaths.model.BenefitUnit;
import simpaths.model.Household;
import simpaths.model.Person;
import simpaths.model.enums.Gender;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

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

    public void matchDonorProfiles(List<Household> households) {

        if (ltIncomeNeeded(households)) {
            // require imputation of income histories
            System.out.println("Imputing income histories for simulated population");
            IntStream.range(0, households.size()).sorted().parallel().forEach(ii -> {
            //for (int ii=0; ii < households.size(); ii++) {

                // consider each household in initialisation set
                Household household = households.get(ii);

                // identify target equivalised household income
                double disposableIncomePerAnnum = 0.0;
                double equivalenceScale = 0.0;
                boolean firstAdult = true;
                for (BenefitUnit benefitUnit : household.getBenefitUnits()) {
                    disposableIncomePerAnnum += benefitUnit.getDisposableIncomeMonthly() * 12.0;
                    for (Person pp : benefitUnit.getMembers()) {
                        if (pp.getDemAge() > 13) {
                            if (firstAdult) {
                                equivalenceScale += 1.0;
                                firstAdult = false;
                            } else {
                                equivalenceScale += 0.5;
                            }
                        } else {
                            equivalenceScale += 0.3;
                        }
                    }
                }
                double targetIncome = disposableIncomePerAnnum / equivalenceScale;

                for (BenefitUnit benefitUnit : household.getBenefitUnits()) {
                    // loop through each benefit unit

                    for (Person person : benefitUnit.getMembers()) {
                        // loop through each person

                        if (person.getDemAge()==0) {
                            person.setLtIncome(endAge);
                        }
                        else {

                            // match birth cohort (birth year and gender)
                            List<Individual> individuals = getLtIncomeDonors(person);
                            if (individuals.isEmpty())
                                throw new IllegalArgumentException("No individuals found for person " + person.getId());

                            // match income
                            int lwr = 0, upr = individuals.size() - 1;
                            double lwrValue = individuals.get(lwr).getAnnualIncome(year).getValue() - targetIncome;
                            if (lwrValue > 0.0) {
                                // lower bound

                                person.setLtIncomeDonor(individuals.get(lwr));
                            } else {
                                double uprValue = individuals.get(upr).getAnnualIncome(year).getValue() - targetIncome;
                                if (uprValue < 0.0) {
                                    // upper bound

                                    person.setLtIncomeDonor(individuals.get(upr));
                                } else {
                                    // find bounded value

                                    while (lwr < upr - 1) {
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
                                    } else {
                                        person.setLtIncomeDonor(individuals.get(lwr));
                                    }
                                }
                            }
                            person.setLtIncome(endAge);
                        }
                    }
                }
            });
            //}
            System.out.println("Completed imputing income histories for simulated population");
        }
    }

    private List<Individual> getLtIncomeDonors(Person person) {
        int birthYear = year - Math.min(person.getDemAge(),endAge);
        Gender gender = person.getDemMaleFlag();
        BirthCohort targetCohort = null;
        for (BirthCohort cohort : cohorts) {

            if (cohort.getBirthYear() == birthYear && cohort.getGender() == gender) {
                targetCohort = cohort;
                break;
            }
        }
        if (targetCohort == null)
            throw new IllegalArgumentException("No cohort found for birth year " + birthYear + " and gender " + gender);
        List<Individual> individuals = targetCohort.getSortedIndividuals();
        return individuals;
    }

    private boolean ltIncomeNeeded(List<Household> households) {

        boolean ltIncomeNeeded = false;
        if (!households.isEmpty()) {

            Iterator<Household> iteratorHousehold = households.iterator();
            Household household = iteratorHousehold.next();
            Set<BenefitUnit> benefitUnits = household.getBenefitUnits();
            if (!benefitUnits.isEmpty()) {

                Iterator<BenefitUnit> iteratorBenefitUnit = benefitUnits.iterator();
                BenefitUnit benefitUnit = iteratorBenefitUnit.next();
                Set<Person> persons = benefitUnit.getMembers();
                if (!persons.isEmpty()) {

                    Iterator<Person> iteratorPerson = persons.iterator();
                    Person person = iteratorPerson.next();
                    ltIncomeNeeded = (person.getLtIncomeDonor() == null);
                }
            }
        }
        return ltIncomeNeeded;
    }
}
