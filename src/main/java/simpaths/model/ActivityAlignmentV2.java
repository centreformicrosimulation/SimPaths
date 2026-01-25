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
        addFixedCostRegressorsIfNeeded(coefficientMap, this.regressorsToModify); // Call before copying original coefficients so the adjustment can be retained
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
     * Ensures that AlignmentFixedCostMen and AlignmentFixedCostWomen exist in the coefficient map if required.
     */
    private void addFixedCostRegressorsIfNeeded(MultiKeyCoefficientMap map, List<String> regressors) {
        for (String reg : regressors) {
            if ((reg.equals("AlignmentFixedCostMen") || reg.equals("AlignmentFixedCostWomen"))
                    && map.getValue(reg) == null) {
                // Infer the format from an existing coefficient
                Object sample = map.getValue("IncomeDiv100");
                if (sample instanceof Object[]) {
                    map.putValue(reg, new Object[]{0.0});
                } else {
                    map.putValue(reg, 0.0);
                }
            }
        }
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
        return targetAggregateShareOfEmployed - computeSimulatedShareUsingFraction();
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
        // Update only benefit units in the selected(!) subgroup
        benefitUnits.parallelStream()
                .filter(this::matchesSubgroup)
                .forEach(bu -> {

                    // Avoid full labour update — too costly for alignment loop
                    // bu.updateLabourSupplyAndIncome();

                    // now use a faster alternative:
                    bu.updateFixedCostsAndLabour();
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
     * Currently UNUSED
     * Computes the simulated employment share for benefit units in the specified subgroup using employment boolean of each BU
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
     * Computes the simulated employment share for benefit units in the specified subgroup using employment fraction of each BU
     */

    private double computeSimulatedShareUsingFraction() {
        double[] totals = benefitUnits.parallelStream()
                .filter(this::matchesSubgroup)
                .collect(
                        () -> new double[2], // [0] = count of units, [1] = sum of fracEmployed
                        (a, bu) -> {
                            a[0]++; // count unit
                            a[1] += bu.fracEmployed(); // accumulate fractional employment
                        },
                        (a, b) -> {
                            a[0] += b[0];
                            a[1] += b[1];
                        }
                );

        return totals[0] > 0 ? totals[1] / totals[0] : 0.0;
    }

    /**
     * Determines whether a BenefitUnit belongs to the subgroup defined by subgroupFlag.
     * Mirrors the atRiskOfWork()/getAdultChildFlag() flow used by BenefitUnit class.
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
