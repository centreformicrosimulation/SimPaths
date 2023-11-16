package simpaths.model;

import microsim.engine.SimulationEngine;
import simpaths.data.IEvaluation;
import simpaths.data.Parameters;
import simpaths.model.enums.Les_c4;
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

    private double targetAggregateShareOfEmployedPersons;
    private double utilityAdjustment;
    boolean utilityAdjustmentChanged;
    private Set<Person> persons;
    private Set<BenefitUnit> benefitUnits;
    private SimPathsModel model;

    public ActivityAlignment(Set<Person> persons, Set<BenefitUnit> benefitUnits, double utilityAdjustment) {
        this.model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        this.persons = persons;
        this.benefitUnits = benefitUnits;
        this.utilityAdjustment = utilityAdjustment;
        targetAggregateShareOfEmployedPersons = Parameters.getTargetShare(model.getYear(), TargetShares.Employment);
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

        double error = targetAggregateShareOfEmployedPersons - evalAggregateShareOfEmployedPersons();
        return error;
    }

    /**
     * Evaluates the aggregate share of employed persons.
     *
     * @return The aggregate share of employed persons among those eligible, or 0.0 if no eligible persons are found.
     */
    private double evalAggregateShareOfEmployedPersons() {
        long numPersons = persons.stream()
                .filter(person -> person.getDag() >= Parameters.MIN_AGE_FLEXIBLE_LABOUR_SUPPLY)
                .count();

        long numPersonsEmployed = persons.stream()
                .filter(person -> person.getDag() >= Parameters.MIN_AGE_FLEXIBLE_LABOUR_SUPPLY)
                .filter(person -> person.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed))
                .count();

        return numPersons > 0
                ? (double) numPersonsEmployed / numPersons
                : 0.0;
    }

    /**
     * Adjusts the utility function used in the labour supply process.
     *
     * @param newUtilityAdjustment The new adjustment value for the utility function.
     */
    private void adjustEmployment(double newUtilityAdjustment) {
        benefitUnits.stream()
                .filter(BenefitUnit::getAtRiskOfWork)
                .filter(benefitUnit -> !benefitUnit.occupancy.equals(Occupancy.Couple))
                .forEach(benefitUnit -> benefitUnit.updateLabourSupplyAndIncome(newUtilityAdjustment));

        // Update les_c4 variable before (re)calculating share of employed persons
        benefitUnits.stream()
                .forEach(BenefitUnit::updateActivityOfPersonsWithinBenefitUnit);

        utilityAdjustment = newUtilityAdjustment;
        utilityAdjustmentChanged = true;
    }

}
