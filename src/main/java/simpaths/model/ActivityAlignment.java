package simpaths.model;

import microsim.data.MultiKeyCoefficientMap;
import microsim.engine.SimulationEngine;
import simpaths.data.IEvaluation;
import simpaths.data.Parameters;
import simpaths.model.enums.Occupancy;
import simpaths.model.enums.TargetShares;

import java.util.Set;

/**
 * ActivityAlignment adjusts the share of individuals in employment to values observed in the data.
 * It modifies the utility of leisure in the labour supply module.
 *
 * To find the value by which the intercept should be adjusted, it uses a search routine.
 * The routine adjusts utility, and checks the share of employed persons in the model. If the
 * results differ from the targets based on the data by more than a specified threshold, the adjustment is repeated.
 *
 * Importantly, the adjustment needs to be only found once. Adjustment factors can then be (re)used in subsequent simulations.
 */

public class ActivityAlignment implements IEvaluation {

    private double targetAggregateShareOfEmployedBU;
    private double utilityAdjustment;
    boolean utilityAdjustmentChanged;
    MultiKeyCoefficientMap regressionCoefficientsMap;
    double originalRegressionCoefficient;
    double originalRegressionCoefficient2;
    String regressorToModify;
    String regressor2ToModify;
    Occupancy benefitUnitType;
    private Set<Person> persons;
    private Set<BenefitUnit> benefitUnits;
    private SimPathsModel model;

    public ActivityAlignment(Set<Person> persons, Set<BenefitUnit> benefitUnits, MultiKeyCoefficientMap originalRegressionCoefficientsMap, String[] regressorsToModify, Occupancy benefitUnitType, double utilityAdjustment) {
        this.model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        this.persons = persons;
        this.benefitUnits = benefitUnits;
        this.utilityAdjustment = utilityAdjustment;
        this.regressorToModify = regressorsToModify[0];
        this.regressor2ToModify = null;
        this.originalRegressionCoefficient2 = 0;
        this.regressionCoefficientsMap = originalRegressionCoefficientsMap;
        Object[] valuesOriginalCopy = (Object[]) originalRegressionCoefficientsMap.getValue(regressorToModify);
        this.originalRegressionCoefficient = ((Number) valuesOriginalCopy[0]).doubleValue();
        this.benefitUnitType = benefitUnitType;

        // Share of employed benefit unit of a particular type (Single man / single woman / couple). Couple BU counts as employed if either of the responsible persons works.
        switch (benefitUnitType) {
            case Single_Male -> targetAggregateShareOfEmployedBU = Parameters.getTargetShare(model.getYear(), TargetShares.EmploymentSingleMales);
            case Single_Female -> targetAggregateShareOfEmployedBU = Parameters.getTargetShare(model.getYear(), TargetShares.EmploymentSingleFemales);
            case Couple -> {
                targetAggregateShareOfEmployedBU = Parameters.getTargetShare(model.getYear(), TargetShares.EmploymentCouples);
                regressor2ToModify = regressorsToModify[1];
                Object[] valuesOriginalCopy2 = (Object[]) originalRegressionCoefficientsMap.getValue(regressor2ToModify);
                this.originalRegressionCoefficient2 = ((Number) valuesOriginalCopy2[0]).doubleValue();
            }
        }
    }

    /**
     * Evaluates the discrepancy between the simulated and target aggregate share of employed persons and adjusts utility of leisure if necessary.
     *
     *
     * The error is then calculated as the difference between the target and the actual aggregate share of employed persons.
     * This error value is returned and serves as the stopping condition in root search routines.
     *
     * @param args An array of parameters, where args[0] represents the adjustment parameter.
     * @return The error in the target aggregate share of partnered persons after potential adjustments.
     */
    @Override
    public double evaluate(double[] args) {

        adjustEmployment(args[0]);

        double error = targetAggregateShareOfEmployedBU - evalAggregateShareOfEmployedBU();
        return error;
    }

    /**
     * Evaluates the aggregate share of employed persons.
     *
     * @return The aggregate share of employed persons among those eligible, or 0.0 if no eligible persons are found.
     */
    private double evalAggregateShareOfEmployedBU() {
        long numBU = benefitUnits.stream()
                .filter(benefitUnit -> benefitUnit.getOccupancy().equals(benefitUnitType))
                .count();

        long numBUEmployed = benefitUnits.stream()
                .filter(benefitUnit -> benefitUnit.getOccupancy().equals(benefitUnitType))
                .filter(BenefitUnit::isEmployed)
                .count();

        return numBU > 0
                ? (double) numBUEmployed / numBU
                : 0.0;
    }

    /**
     * Adjusts the utility function used in the labour supply process.
     *
     * @param newUtilityAdjustment The new adjustment value for the utility function.
     */
    private void adjustEmployment(double newUtilityAdjustment) {

        String regressionCoefficientKey = regressorToModify; // Name of the regressor to modify
        MultiKeyCoefficientMap map = regressionCoefficientsMap; // Map of regressors and estimated coefficients
        Object[] currentValues = (Object[]) map.getValue(regressionCoefficientKey);
        double newValue = originalRegressionCoefficient + newUtilityAdjustment; // Adjust regression coefficient
        currentValues[0] = newValue;
        map.replaceValue(regressionCoefficientKey, currentValues); // Put adjusted value back in the map

        if (regressor2ToModify != null) {
            String regressionCoefficientKey2 = regressor2ToModify; // Name of the regressor to modify
            Object[] currentValues2 = (Object[]) map.getValue(regressionCoefficientKey2);
            double newValue2 = originalRegressionCoefficient2 + newUtilityAdjustment; // Adjust regression coefficient
            currentValues2[0] = newValue2;
            map.replaceValue(regressionCoefficientKey2, currentValues2); // Put adjusted value back in the map
        }

        benefitUnits.parallelStream()
                .filter(BenefitUnit::getAtRiskOfWork)
                .filter(benefitUnit -> benefitUnit.getOccupancy().equals(benefitUnitType))
                .forEach(benefitUnit -> benefitUnit.updateLabourSupplyAndIncome()); // Update labour supply and income

        // Update les_c4 variable before (re)calculating share of employed persons
        benefitUnits.parallelStream()
                .filter(benefitUnit -> benefitUnit.getOccupancy().equals(benefitUnitType))
                .forEach(BenefitUnit::updateActivityOfPersonsWithinBenefitUnit);

        utilityAdjustment = newUtilityAdjustment;
        utilityAdjustmentChanged = true;
    }

}
