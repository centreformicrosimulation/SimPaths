package simpaths.model;

import microsim.engine.SimulationEngine;
import simpaths.data.IEvaluation;
import simpaths.data.Parameters;
import simpaths.data.filters.FertileFilter;
import simpaths.model.enums.Les_c4;
import simpaths.model.enums.TargetShares;

import java.util.Set;


/**
 * RetirementAlignment adjusts the probability of retiring.
 * The object is designed to assist modification of the intercept of the "retirement" models.
 *
 * A search routine is used to find the value by which the intercept should be adjusted.
 * If the projected share retired individuals in the population differs from the desired target by more than a specified threshold,
 * then the intercept is adjusted and the share re-evaluated.
 *
 * Importantly, the adjustment needs to be only found once. Modified intercepts can then be used in subsequent simulations.
 */
public class RetirementAlignment implements IEvaluation {

    private double targetRetiredShare;
    private Set<Person> persons;
    private SimPathsModel model;


    // CONSTRUCTOR
    public RetirementAlignment(Set<Person> persons) {
        this.model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        this.persons = persons;
        targetRetiredShare = Parameters.getTargetShare(model.getYear(), TargetShares.Retirement);
    }


    /**
     * Evaluates the discrepancy between the simulated and target total retired share and adjusts probabilities if necessary.
     *
     * This method focuses on the influence of the adjustment parameter 'args[0]' on the difference between the target and
     * simulated retired share (error).
     *
     * The error value is returned and serves as the stopping condition in root search routines.
     *
     * @param args An array of parameters, where args[0] represents the adjustment parameter.
     * @return The error in the target aggregate share of retired persons after potential adjustments.
     */
    @Override
    public double evaluate(double[] args) {

        persons.parallelStream()
                .forEach(person -> person.considerRetirement(args[0]));

        return targetRetiredShare - evalRetiredShare();
    }


    /**
     * Evaluates the aggregate share of persons with partners assigned in a test run of union matching among those eligible for partnership.
     *
     * This method uses Java streams to count the number of persons who meet the age criteria for cohabitation
     * and the number of persons who currently have a test partner. The aggregate share is calculated as the
     * ratio of successfully partnered persons to those eligible for partnership, with consideration for potential division by zero.
     *
     * @return The aggregate share of partnered persons among those eligible, or 0.0 if no eligible persons are found.
     */
    private double evalRetiredShare() {

        long numRetiredPersons = model.getPersons().stream()
                .filter(person -> (person.getToRetire().equals(true) || Les_c4.Retired.equals(person.getLes_c4())))
                .count();
        long numPeople = model.getPersons().stream()
                .filter(person -> person.getLes_c4() != null)
                .count();

        return (numRetiredPersons > 0) ? (double) numRetiredPersons / numPeople : 0.0;
    }
}
