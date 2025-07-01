package simpaths.model;

import microsim.data.MultiKeyCoefficientMap;
import microsim.engine.SimulationEngine;
import simpaths.data.IEvaluation;
import simpaths.data.Parameters;
import simpaths.model.enums.Occupancy;
import simpaths.model.enums.OccupancyExtended;
import simpaths.model.enums.TargetShares;

import java.util.*;

/**
 * ActivityAlignmentV2 calibrates the labor supply model by adjusting utility coefficients
 * so that the simulated employment rate for a specific benefit-unit subgroup matches its target.
 * Uses OccupancyExtended flags and the atRiskOfWork()/getAdultChildFlag() flow for classification.
 */
public class ActivityAlignmentV2 implements IEvaluation {

    private final double targetAggregateShareOfEmployed;
    private final Map<String, CoefficientValue> originalCoefficients;
    private final MultiKeyCoefficientMap coefficientMap;
    private final List<String> regressorsToModify;
    private final Set<BenefitUnit> benefitUnits;
    private final OccupancyExtended subgroupFlag;
    private final SimPathsModel model;

    /**
     * Constructor accepting an OccupancyExtended flag to specify subgroup.
     *
     * @param benefitUnits          all benefit units in the simulation
     * @param coefficientMap        map of utility coefficients
     * @param regressorsToModify    array of coefficient keys to adjust
     * @param subgroupFlag          extended occupancy flag for subgroup classification
     */
    public ActivityAlignmentV2(Set<BenefitUnit> benefitUnits,
                               MultiKeyCoefficientMap coefficientMap,
                               String[] regressorsToModify,
                               OccupancyExtended subgroupFlag) {
        this.model = (SimPathsModel) SimulationEngine.getInstance()
                .getManager(SimPathsModel.class.getCanonicalName());
        this.benefitUnits = Collections.unmodifiableSet(benefitUnits);
        this.coefficientMap = coefficientMap;
        this.regressorsToModify = Arrays.asList(regressorsToModify.clone());
        this.subgroupFlag = subgroupFlag;
        this.originalCoefficients = extractOriginalCoefficients(coefficientMap, this.regressorsToModify);
        this.targetAggregateShareOfEmployed = determineTargetShare(model.getYear(), subgroupFlag);
    }

    /**
     * Extracts the original coefficient values for later restoration and relative adjustment.
     */
    private Map<String, CoefficientValue> extractOriginalCoefficients(
            MultiKeyCoefficientMap map, List<String> regressors) {
        Map<String, CoefficientValue> originals = new LinkedHashMap<>();
        for (String reg : regressors) {
            Object v = map.getValue(reg);
            if (v instanceof Number num) {
                originals.put(reg, new CoefficientValue(num.doubleValue()));
            } else if (v instanceof Object[] arr
                    && arr.length > 0 && arr[0] instanceof Number num) {
                originals.put(reg, new CoefficientValue(arr, num.doubleValue()));
            } else {
                String type = v != null ? v.getClass().getSimpleName() : "null";
                throw new IllegalArgumentException(
                        "Regressor '" + reg + "' must be numeric or numeric array but is " + type);
            }
        }
        return originals;
    }

    /**
     * Determines the target employment share for the given subgroup.
     */
    private double determineTargetShare(int year, OccupancyExtended flag) {
        return switch (flag) {
            case Couple ->
                    Parameters.getTargetShare(year, TargetShares.EmploymentCouples);
            case Male_With_Dependent ->
                    Parameters.getTargetShare(year, TargetShares.EmploymentMaleWithDependent);
            case Female_With_Dependent ->
                    Parameters.getTargetShare(year, TargetShares.EmploymentFemaleWithDependent);
            case Male_AC ->
                    Parameters.getTargetShare(year, TargetShares.EmploymentMaleAdultChildren);
            case Female_AC ->
                    Parameters.getTargetShare(year, TargetShares.EmploymentFemaleAdultChildren);
            case Single_Male ->
                    Parameters.getTargetShare(year, TargetShares.EmploymentSingleMales);
            case Single_Female ->
                    Parameters.getTargetShare(year, TargetShares.EmploymentSingleFemales);
        };
    }

    /**
     * Performs one evaluation: adjusts coefficients by args[0], runs the simulation update,
     * and returns (target - simulated).
     */
    @Override
    public double evaluate(double[] args) {
        double adjustment = args[0];
        adjustCoefficients(adjustment);
        return targetAggregateShareOfEmployed - computeSimulatedShare();
    }

    /**
     * Adjusts each specified regressor coefficient by the given amount relative to its original.
     */
    private void adjustCoefficients(double adjustment) {
        for (String reg : regressorsToModify) {
            CoefficientValue orig = originalCoefficients.get(reg);
            Object newVal = orig.isArray()
                    ? makeArray(orig, adjustment)
                    : orig.baseValue + adjustment;
            coefficientMap.replaceValue(reg, newVal);
        }
        // Update all benefit units in parallel for efficiency
        benefitUnits.parallelStream().forEach(bu -> {
            bu.updateLabourSupplyAndIncome();
            bu.updateActivityOfPersonsWithinBenefitUnit();
        });
    }

    /**
     * Creates a new array value for an array-valued coefficient.
     */
    private Object[] makeArray(CoefficientValue orig, double adjustment) {
        Object[] arr = Arrays.copyOf(orig.arrayValue, orig.arrayValue.length);
        arr[0] = orig.baseValue + adjustment;
        return arr;
    }

    /**
     * Computes the simulated employment share for benefit units in the specified subgroup.
     */
    private double computeSimulatedShare() {
        long[] counts = benefitUnits.stream()
                .filter(this::matchesSubgroup)
                .collect(() -> new long[2],
                        (a, bu) -> { a[0]++; if (bu.isEmployed()) a[1]++; },
                        (a, b) -> { a[0] += b[0]; a[1] += b[1]; });
        return counts[0] > 0 ? (double) counts[1] / counts[0] : 0.0;
    }

    /**
     * Determines whether a BenefitUnit belongs to the subgroup defined by subgroupFlag.
     * Mirrors the atRiskOfWork()/getAdultChildFlag() flow in your classification code.
     *
     * Determines whether a BenefitUnit belongs to the subgroup defined by subgroupFlag.
     * Safely handles missing male/female members and retrieves the adult-child flag
     * from the Person instance.
     */
    private boolean matchesSubgroup(BenefitUnit bu) {
        Occupancy occ = bu.getOccupancy();

        // Safely retrieve the male and female Person objects (may be null)
        Person male = bu.getMale();
        boolean maleAtRisk = (male != null) && male.atRiskOfWork();

        Person female = bu.getFemale();
        boolean femaleAtRisk = (female != null) && female.atRiskOfWork();

        // Retrieve adult-child flag only for single‐person units
        int acFlag = 0;
        if (occ == Occupancy.Single_Male && male != null) {
            acFlag = male.getAdultChildFlag();
        } else if (occ == Occupancy.Single_Female && female != null) {
            acFlag = female.getAdultChildFlag();
        }

        switch (subgroupFlag) {
            case Couple:
                return occ == Occupancy.Couple
                        && maleAtRisk && femaleAtRisk;

            case Male_With_Dependent:
                return occ == Occupancy.Couple
                        && maleAtRisk && !femaleAtRisk;

            case Female_With_Dependent:
                return occ == Occupancy.Couple
                        && femaleAtRisk && !maleAtRisk;

            case Single_Male:
                return occ == Occupancy.Single_Male
                        && acFlag != 1;

            case Male_AC:
                return occ == Occupancy.Single_Male
                        && acFlag == 1;

            case Single_Female:
                return occ == Occupancy.Single_Female
                        && acFlag != 1;

            case Female_AC:
                return occ == Occupancy.Single_Female
                        && acFlag == 1;

            default:
                return false;
        }
    }


    /**
     * Helper class to store original coefficient values.
     */
    private static class CoefficientValue {
        final double baseValue;
        final Object[] arrayValue;

        CoefficientValue(double v) {
            this.baseValue = v;
            this.arrayValue = null;
        }

        CoefficientValue(Object[] arr, double v) {
            this.arrayValue = arr;
            this.baseValue = v;
        }

        boolean isArray() {
            return arrayValue != null;
        }
    }
}
