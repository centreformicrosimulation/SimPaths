package simpaths.model;


import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import simpaths.data.Parameters;
import simpaths.model.enums.Dcpst;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Region;
import simpaths.model.enums.TargetShares;


/**
 * Class filter to ensure that starting population for simulation matches alignment targets.
 * The filter is based on ceiling values for numbers of individuals of various types in the
 * starting population. For each candidate household, the filter subtracts 1 from each of the ceilings
 * associated with each household member. A household is returned as a valid candidate only if all
 * ceilings associated with all household members are non-negative. At start, the ceilings are inflated
 * by an (error) margin to facilitate population identification.
 */
public class InitialPopulationFilter {

    // target population counters
    int populationSize;
    int censorAge;
    int cohabiting;
    MultiKeyMap<Object, Integer> populationByGenderRegionAndAge = MultiKeyMap.multiKeyMap(new LinkedMap<>());
    final double MARGIN = 0.05;   //


    // CONSTRUCTOR
    public InitialPopulationFilter(int populationSize, int year, int maxAge){

        this.populationSize = (int)((1.0 + MARGIN) * (double)populationSize);
        censorAge = Math.min(Parameters.getPopulationProjectionsMaxAge(), maxAge);
        evaluateCeilingValues(year);
    }


    // HELPER METHODS
    private void evaluateCeilingValues(int year) {

        double totalPopulationProjection = 0.0;
        int maxAgeProjection = Parameters.getPopulationProjectionsMaxAge();
        for (Gender gender : Gender.values()) {
            for (Region region: Parameters.getCountryRegions()) {
                for (int age = 0; age <= maxAgeProjection; age++) {
                    totalPopulationProjection += Parameters.getPopulationProjections(gender, region, age, year);
                }
            }
        }

        double adults = 0.0;
        for (Gender gender : Gender.values()) {
            for (Region region: Parameters.getCountryRegions()) {
                double valAgg = 0.0;
                for (int age = 0; age <= maxAgeProjection; age++) {
                    double val = Parameters.getPopulationProjections(gender, region, age, year);
                    if (age >= Parameters.MIN_AGE_COHABITATION)
                        adults += val;
                    if (age >= censorAge) {
                        valAgg += val;
                    }
                    if (age < censorAge) {
                        int valHere = (int)(val / totalPopulationProjection * populationSize);
                        populationByGenderRegionAndAge.put(gender, region, age, valHere);
                    }
                }
                int valHere = (int)(valAgg / totalPopulationProjection * populationSize);
                populationByGenderRegionAndAge.put(gender, region, censorAge, valHere);
            }
        }
        cohabiting = (int)(Parameters.getTargetShare(year, TargetShares.Partnership) * adults / totalPopulationProjection * populationSize);
    }

    /**
     * returns true if household is valid candidate
     * a candidate is valid only if all household members are within considered population ceilings
     * @param household
     * @return
     */
    public boolean evaluate(Household household) {

        // check thresholds
        for (BenefitUnit benefitUnit : household.getBenefitUnitSet()) {

            for (Person person : benefitUnit.getPersonsInBU()) {

                if (person.getDag() < Parameters.MIN_AGE_COHABITATION && Dcpst.Partnered.equals(person.getDcpst()))
                    return false;
                else if (Dcpst.Partnered.equals(person.getDcpst()) && cohabiting <= 0)
                    return false;
                else if (populationByGenderRegionAndAge.get(person.getDgn(), person.getRegion(), Math.min(censorAge,person.getDag())) <= 0)
                    return false;
            }
        }

        // update thresholds
        for (BenefitUnit benefitUnit : household.getBenefitUnitSet()) {

            for (Person person : benefitUnit.getPersonsInBU()) {

                if (Dcpst.Partnered.equals(person.getDcpst()))
                    cohabiting -= 1;

                int age = Math.min(censorAge, person.getDag());
                Region region = person.getRegion();
                Gender gender = person.getDgn();
                int ceil = populationByGenderRegionAndAge.get(gender, region, age);
                populationByGenderRegionAndAge.put(gender, region, age, ceil-1);
            }
        }

        return true;
    }

    public int getRemainingVacancies() {

        int vacancies = 0;
        for (Gender gender : Gender.values()) {
            for (Region region: Parameters.getCountryRegions()) {
                for (int age = 0; age <= censorAge; age++) {
                    if (populationByGenderRegionAndAge.get(gender, region, age) > 0) {
                        vacancies += populationByGenderRegionAndAge.get(gender, region, age);
                    }
                }
            }
        }
        return vacancies;
    }
}
