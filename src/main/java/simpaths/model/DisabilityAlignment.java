package simpaths.model;

import microsim.engine.SimulationEngine;
import simpaths.data.IEvaluation;
import simpaths.data.Parameters;
import simpaths.model.enums.Indicator;
import simpaths.model.enums.Les_c4;
import simpaths.model.enums.TargetShares;

import java.util.Set;


/**
 * DisabilityAlignment adjusts the probability of becoming disabled.
 * The object is designed to assist modification of the intercept of the "H2b" disability model.
 *
 * A search routine is used to find the value by which the intercept should be adjusted.
 * If the projected share of disabled individuals in the population differs from the desired target by more than a specified threshold,
 * then the intercept is adjusted and the share re-evaluated.
 *
 * Importantly, the adjustment needs to be only found once. Modified intercepts can then be used in subsequent simulations.
 */
public class DisabilityAlignment implements IEvaluation {

    private double targetDisabledShare;
    private Set<Person> persons;
    private SimPathsModel model;


    // CONSTRUCTOR
    public DisabilityAlignment(Set<Person> persons) {
        this.model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        this.persons = persons;
        targetDisabledShare = Parameters.getTargetShare(model.getYear(), TargetShares.Disability);
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
                .forEach(person -> person.disability(args[0]));

        return targetDisabledShare - evalDisabledShare();
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
    private double evalDisabledShare() {

        long numDisabledPersons = model.getPersons().stream()
                .filter(person -> (Indicator.True.equals(person.getDlltsd())))
                .count();
        long numPeople = model.getPersons().stream()
                .filter(person -> person.getDlltsd() != null)
                .count();

        return (numDisabledPersons > 0) ? (double) numDisabledPersons / numPeople : 0.0;
    }
}
