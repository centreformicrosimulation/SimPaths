package simpaths.model;

import microsim.engine.SimulationEngine;
import simpaths.data.Parameters;
import simpaths.model.enums.Education;
import simpaths.model.enums.MacroScenarioPopulation;
import simpaths.model.enums.Region;

import java.util.*;
import java.util.random.RandomGenerator;


//Significantly simplified LabourMarket module. Should just contain simple labour supply for now and matching with euromod donor population.
public class LabourMarket {

    private final SimPathsModel model;

    private String EUROMODpolicyNameForThisYear;

    private final LinkedHashMap<Region, Set<BenefitUnit>> benefitUnitsByRegion; //Don't think we care about regions, but maybe benefitUnits are matched by region?

    private final Set<BenefitUnit> benefitUnitsAllRegions;

    Set<BenefitUnit> benefitUnits;

    RandomGenerator labourInnov;


    //Constructor:
    LabourMarket(Set<BenefitUnit> benefitUnits) {

        model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
//		observer = (SimPathsObserver) SimulationEngine.getInstance().getManager(SimPathsObserver.class.getCanonicalName());	//To allow intra-time-step updates of convergence plots
        this.benefitUnits = benefitUnits;
        EUROMODpolicyNameForThisYear = Parameters.getEUROMODpolicyForThisYear(model.getYear());

        benefitUnitsByRegion = new LinkedHashMap<Region, Set<BenefitUnit>>();		//For use in labour market module
        benefitUnitsAllRegions = new LinkedHashSet<>();

        for(Region region : Parameters.getCountryRegions()) {
            benefitUnitsByRegion.put(region, new LinkedHashSet<BenefitUnit>());
        }

        labourInnov = new Random(SimulationEngine.getRnd().nextLong());
    }


    protected void update(int year) {

            // Otherwise, use the default model of labour supply

            EUROMODpolicyNameForThisYear = Parameters.getEUROMODpolicyForThisYear(year);    //Update EUROMOD policy to apply to this year

            for (Region region : Parameters.getCountryRegions()) {
                benefitUnitsByRegion.get(region).clear();
            }
            benefitUnitsAllRegions.clear();
            for (BenefitUnit benefitUnit : benefitUnits) {

                if (benefitUnit.getAtRiskOfWork()) { //Update HHs at risk of work, i.e. either of the adults are not under age, retired, student, in bad health
                    benefitUnitsByRegion.get(benefitUnit.getRegion()).add(benefitUnit);        //This is the collection of benefitUnits that will enter the labour market
                    benefitUnitsAllRegions.add(benefitUnit);
                } else {
                    benefitUnit.updateNonLabourIncome();
                    benefitUnit.updateDisposableIncomeIfNotAtRiskOfWork();
                }
            }

            if (model.isAlignEmployment() & model.getYear() <= 2023 & !model.isMacroShocksOn()) {
                model.activityAlignmentSingleMales();
                model.activityAlignmentSingleACMales();
                model.activityAlignmentSingleFemales();
                model.activityAlignmentSingleACFemales();
                model.activityAlignmentCouples();
                model.activityAlignmentMaleWithDependents();
                model.activityAlignmentFemaleWithDependents();
            }

            if (model.isMacroShocksOn()) {
                model.activityAlignmentMacroShock();
            }

            //Update Labour Supply
            benefitUnitsAllRegions.parallelStream()
                    .forEach(BenefitUnit::updateLabourSupplyAndIncome);

            Map<Education, Double> potentialHourlyEarningsByEdu = new LinkedHashMap<Education, Double>();
            Map<Education, Integer> countByEdu = new LinkedHashMap<Education, Integer>();
            for (Education ed : Education.values()) {

                potentialHourlyEarningsByEdu.put(ed, 0.);
                countByEdu.put(ed, 0);
            }
            for (BenefitUnit benefitUnit : benefitUnitsAllRegions) {

                if (benefitUnit.getMale() != null) {

                    Person person = benefitUnit.getMale();
                    if (person.atRiskOfWork()) {

                        Education ed = person.getDeh_c3();
                        double newVal = person.getFullTimeHourlyEarningsPotential();
                        potentialHourlyEarningsByEdu.put(ed, potentialHourlyEarningsByEdu.get(ed) + newVal);
                        int oldCount = countByEdu.get(ed);
                        countByEdu.put(ed, oldCount + 1);
                    }
                }
                if (benefitUnit.getFemale() != null) {

                    Person person = benefitUnit.getFemale();
                    if (person.atRiskOfWork()) {

                        Education ed = person.getDeh_c3();
                        double newVal = person.getFullTimeHourlyEarningsPotential();
                        potentialHourlyEarningsByEdu.put(ed, potentialHourlyEarningsByEdu.get(ed) + newVal);
                        int oldCount = countByEdu.get(ed);
                        countByEdu.put(ed, oldCount + 1);
                    }
                }
            }

            // Update activity status of persons residing within the benefit unit
            benefitUnits.stream()
                    .forEach(BenefitUnit::updateActivityOfPersonsWithinBenefitUnit);

    }
}
