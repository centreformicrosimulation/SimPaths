package simpaths.model;

import microsim.data.MultiKeyCoefficientMap;
import microsim.engine.SimulationEngine;
import simpaths.data.IEvaluation;
import simpaths.data.Parameters;
import simpaths.model.enums.Occupancy;
import simpaths.model.enums.TargetShares;

import java.util.*;

/**
 * ActivityAlignmentV2 calibrates a labor supply model by adjusting utility function coefficients,
 * so that the simulated employment rate for a specific household (benefit unit) type
 * matches a target employment rate from empirical data.
 */
public class ActivityAlignmentV2 implements IEvaluation {

    private final double targetAggregateShareOfEmployed;
    private final Map<String, CoefficientValue> originalCoefficients;
    private final MultiKeyCoefficientMap coefficientMap;
    private final List<String> regressorsToModify;
    private final Set<BenefitUnit> benefitUnits;
    private final Occupancy benefitUnitType;
    private final SimPathsModel model;

    public ActivityAlignmentV2(Set<BenefitUnit> benefitUnits,
                               MultiKeyCoefficientMap coefficientMap,
                               String[] regressorsToModify,
                               Occupancy benefitUnitType) {
        this.model = (SimPathsModel) SimulationEngine.getInstance()
                .getManager(SimPathsModel.class.getCanonicalName());
        this.benefitUnits = Collections.unmodifiableSet(benefitUnits);
        this.coefficientMap = coefficientMap;
        this.regressorsToModify = Arrays.asList(regressorsToModify.clone());
        this.benefitUnitType = benefitUnitType;
        this.originalCoefficients = extractOriginalCoefficients(coefficientMap, this.regressorsToModify);
        this.targetAggregateShareOfEmployed = determineTargetShare(model, benefitUnitType);
    }

    /**
     * Extracts the original values of the coefficients to be adjusted.
     * Handles both numeric values and single-element arrays of numbers.
     */
    private Map<String, CoefficientValue> extractOriginalCoefficients(MultiKeyCoefficientMap map, List<String> regressors) {
        Map<String, CoefficientValue> originals = new LinkedHashMap<>();
        for (String regressor : regressors) {
            Object value = map.getValue(regressor);
            if (value instanceof Number num) {
                originals.put(regressor, new CoefficientValue(num.doubleValue()));
            } else if (value instanceof Object[] arr && arr.length > 0 && arr[0] instanceof Number num) {
                originals.put(regressor, new CoefficientValue(arr, num.doubleValue()));
            } else {
                String type = value != null ? value.getClass().getSimpleName() : "null";
                throw new IllegalArgumentException(
                        "Regressor '" + regressor + "' must be numeric or a numeric array but is " + type + " (value: " + value + ")"
                );
            }
        }
        return originals;
    }

    /**
     * Determines the target employment share for the given benefit unit type and simulation year.
     */
    private double determineTargetShare(SimPathsModel model, Occupancy type) {
        switch (type) {
            case Couple:
                return Parameters.getTargetShare(model.getYear(), TargetShares.EmploymentCouples);
            case Single_Male:
                return Parameters.getTargetShare(model.getYear(), TargetShares.EmploymentSingleMales);
            case Single_Female:
                return Parameters.getTargetShare(model.getYear(), TargetShares.EmploymentSingleFemales);
            default:
                throw new IllegalArgumentException("Unsupported occupancy type: " + type);
        }
    }

    /**
     * Applies a utility adjustment, updates the model, and returns the difference between
     * target and simulated employment shares.
     */
    @Override
    public double evaluate(double[] args) {
        double newUtilityAdjustment = args[0];
        adjustEmployment(newUtilityAdjustment);
        return targetAggregateShareOfEmployed - evalAggregateShareOfEmployedBU();
    }

    /**
     * Efficiently computes the employment rate for benefit units of the specified type.
     */
    private double evalAggregateShareOfEmployedBU() {
        long[] counts = benefitUnits.stream()
                .filter(bu -> bu.getOccupancy().equals(benefitUnitType))
                .collect(() -> new long[2],
                        (arr, bu) -> {
                            arr[0]++;
                            if (bu.isEmployed()) arr[1]++;
                        },
                        (a, b) -> {
                            a[0] += b[0];
                            a[1] += b[1];
                        });
        return counts[0] > 0 ? (double) counts[1] / counts[0] : 0.0;
    }

    /**
     * Adjusts the specified utility coefficients by the given amount,
     * always relative to their original values, and updates all benefit units.
     */
    private void adjustEmployment(double newUtilityAdjustment) {
        for (String regressor : regressorsToModify) {
            CoefficientValue original = originalCoefficients.get(regressor);
            Object newValue;
            if (original.isArray()) {
                Object[] newArr = Arrays.copyOf(original.arrayValue, original.arrayValue.length);
                newArr[0] = original.baseValue + newUtilityAdjustment;
                newValue = newArr;
            } else {
                newValue = original.baseValue + newUtilityAdjustment;
            }
            coefficientMap.replaceValue(regressor, newValue);
        }

        // Update benefit units in a single parallel pass for efficiency
        benefitUnits.parallelStream().forEach(bu -> {
            bu.updateLabourSupplyAndIncome();
            bu.updateActivityOfPersonsWithinBenefitUnit();
        });
    }

    /**
     * Helper class to track coefficient types and values.
     */
    private static class CoefficientValue {
        final double baseValue;
        final Object[] arrayValue;

        CoefficientValue(double scalar) {
            this.baseValue = scalar;
            this.arrayValue = null;
        }

        CoefficientValue(Object[] array, double firstElement) {
            this.baseValue = firstElement;
            this.arrayValue = array;
        }

        boolean isArray() {
            return arrayValue != null;
        }
    }
}
