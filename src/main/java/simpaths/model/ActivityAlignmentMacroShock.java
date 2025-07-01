package simpaths.model;

import microsim.data.MultiKeyCoefficientMap;
import microsim.engine.SimulationEngine;
import simpaths.data.IEvaluation;
import simpaths.data.Parameters;
import simpaths.model.enums.OccupancyExtended;
import simpaths.model.enums.TargetShares;
import simpaths.model.enums.Les_c4;

import java.util.*;

public class ActivityAlignmentMacroShock implements IEvaluation {

    private final double targetAggregateShareOfEmployedPersons;
    private double utilityAdjustment;
    boolean utilityAdjustmentChanged;
    private final Map<OccupancyExtended, MultiKeyCoefficientMap> coefficientMaps;
    private final Map<OccupancyExtended, List<String>> regressorsToModify;
    private final Set<Person> persons;
    private final Set<BenefitUnit> benefitUnits;
    private final SimPathsModel model;

    // Stores original values of coefficients that will be modified
    private final Map<OccupancyExtended, Map<String, Double>> originalCoefficients;

    public ActivityAlignmentMacroShock(Set<Person> persons, Set<BenefitUnit> benefitUnits,
                                       Map<OccupancyExtended, MultiKeyCoefficientMap> coefficientMaps,
                                       Map<OccupancyExtended, List<String>> regressorsToModify,
                                       double utilityAdjustment) {
        this.model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        this.persons = Collections.unmodifiableSet(persons);
        this.benefitUnits = Collections.unmodifiableSet(benefitUnits);
        this.utilityAdjustment = utilityAdjustment;
        this.coefficientMaps = Collections.unmodifiableMap(coefficientMaps);
        this.regressorsToModify = Collections.unmodifiableMap(regressorsToModify);

        // Store original values of coefficients that will be modified
        this.originalCoefficients = new EnumMap<>(OccupancyExtended.class);
        for (Map.Entry<OccupancyExtended, List<String>> entry : regressorsToModify.entrySet()) {
            OccupancyExtended occupancy = entry.getKey();
            List<String> regressors = entry.getValue();
            MultiKeyCoefficientMap coefficientMap = coefficientMaps.get(occupancy);

            Map<String, Double> occupancyValues = new HashMap<>();
            for (String regressor : regressors) {
                Object value = coefficientMap.getValue(regressor);
                if (!(value instanceof Number)) {
                    throw new IllegalArgumentException("Coefficient " + regressor + " must be numeric");
                }
                occupancyValues.put(regressor, ((Number) value).doubleValue());
            }
            originalCoefficients.put(occupancy, occupancyValues);
        }

        this.targetAggregateShareOfEmployedPersons = Parameters.getTargetShare(model.getYear(), TargetShares.Employment);
    }

    @Override
    public double evaluate(double[] args) {
        adjustEmployment(args[0]);
        return targetAggregateShareOfEmployedPersons - evalAggregateShareOfEmployedPersons();
    }

    private double evalAggregateShareOfEmployedPersons() {
        long total = persons.size();
        if (total == 0) return 0.0;

        long employed = persons.stream()
                .filter(p -> p.getLes_c4() == Les_c4.EmployedOrSelfEmployed)
                .count();

        return (double) employed / total;
    }

    private void adjustEmployment(double newUtilityAdjustment) {
        for (Map.Entry<OccupancyExtended, MultiKeyCoefficientMap> entry : coefficientMaps.entrySet()) {
            OccupancyExtended occupancy = entry.getKey();
            MultiKeyCoefficientMap currentMap = entry.getValue();
            List<String> regressors = regressorsToModify.get(occupancy);
            Map<String, Double> originalValues = originalCoefficients.get(occupancy);

            for (String regressor : regressors) {
                double originalValue = originalValues.get(regressor);
                double newValue = originalValue + newUtilityAdjustment;
                currentMap.replaceValue(regressor, newValue);
            }
        }

        // Update benefit units
        benefitUnits.parallelStream()
                .filter(BenefitUnit::getAtRiskOfWork)
                .forEach(BenefitUnit::updateLabourSupplyAndIncome);

        benefitUnits.parallelStream()
                .forEach(BenefitUnit::updateActivityOfPersonsWithinBenefitUnit);

        // Update state
        utilityAdjustment = newUtilityAdjustment;
        utilityAdjustmentChanged = true;
    }
}
