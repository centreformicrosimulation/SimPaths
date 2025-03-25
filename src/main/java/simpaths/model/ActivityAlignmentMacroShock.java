package simpaths.model;

import microsim.data.MultiKeyCoefficientMap;
import microsim.engine.SimulationEngine;
import simpaths.data.IEvaluation;
import simpaths.data.Parameters;
import simpaths.model.enums.Occupancy;
import simpaths.model.enums.OccupancyMacroShock;
import simpaths.model.enums.TargetShares;
import simpaths.model.enums.Les_c4;

import java.util.Map;
import java.util.Set;
import java.util.List;

public class ActivityAlignmentMacroShock implements IEvaluation {

    private double targetAggregateShareOfEmployedPersons;
    private double utilityAdjustment;
    boolean utilityAdjustmentChanged;
    Map<OccupancyMacroShock, MultiKeyCoefficientMap> coefficientMaps;
    Map<OccupancyMacroShock, List<String>> regressorsToModify;
    private Set<Person> persons;
    private Set<BenefitUnit> benefitUnits;
    private SimPathsModel model;

    public ActivityAlignmentMacroShock(Set<Person> persons, Set<BenefitUnit> benefitUnits,
                                       Map<OccupancyMacroShock, MultiKeyCoefficientMap> coefficientMaps,
                                       Map<OccupancyMacroShock, List<String>> regressorsToModify,
                                       double utilityAdjustment) {
        this.model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        this.persons = persons;
        this.benefitUnits = benefitUnits;
        this.utilityAdjustment = utilityAdjustment;
        this.coefficientMaps = coefficientMaps;
        this.regressorsToModify = regressorsToModify;

        this.targetAggregateShareOfEmployedPersons = Parameters.getTargetShare(model.getYear(), TargetShares.Employment);
    }

    @Override
    public double evaluate(double[] args) {
        adjustEmployment(args[0]);
        double error = targetAggregateShareOfEmployedPersons - evalAggregateShareOfEmployedPersons();
        return error;
    }

    private double evalAggregateShareOfEmployedPersons() {
        long numPersons = persons.stream()
                .count();

        long numPersonsEmployed = persons.stream()
                .filter(person -> person.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed))
                .count();

        return numPersons > 0
                ? (double) numPersonsEmployed / numPersons
                : 0.0;
    }

    private void adjustEmployment(double newUtilityAdjustment) {
        for (Map.Entry<OccupancyMacroShock, MultiKeyCoefficientMap> entry : coefficientMaps.entrySet()) {
            OccupancyMacroShock occupancy = entry.getKey();
            MultiKeyCoefficientMap map = entry.getValue();
            List<String> regressors = regressorsToModify.get(occupancy);

            for (String regressor : regressors) {
                Object currentValueObj = map.getValue(regressor);
                if (!(currentValueObj instanceof Number)) {
                    throw new IllegalArgumentException("Expected a numeric value for key: " + regressor);
                }

                double currentValue = ((Number) currentValueObj).doubleValue();
                double newValue = currentValue + newUtilityAdjustment;
                map.replaceValue(regressor, newValue);
            }
        }

        benefitUnits.parallelStream()
                .filter(BenefitUnit::getAtRiskOfWork)
                .forEach(benefitUnit -> benefitUnit.updateLabourSupplyAndIncome());

        benefitUnits.parallelStream()
                .forEach(BenefitUnit::updateActivityOfPersonsWithinBenefitUnit);

        utilityAdjustment = newUtilityAdjustment;
        utilityAdjustmentChanged = true;
    }
}
