package simpaths.model;

import microsim.engine.SimulationEngine;
import simpaths.data.IEvaluation;
import simpaths.data.Parameters;
import simpaths.data.filters.FertileFilter;
import simpaths.model.enums.Dcpst;
import simpaths.model.enums.TimeSeriesVariable;

import java.util.Set;


/**
 * FertilityAlignment adjusts the probability of fertile women having a child to match total fertility rates implicit in population projections.
 * The object is designed to assist modification of the intercept of the "fertility" probit models.
 *
 * A search routine is used to find the value by which the intercept should be adjusted.
 * If the projected share of the population having a child differs from the desired target by more than a specified threshold,
 * then the intercept is adjusted and the share re-evaluated.
 *
 * Importantly, the adjustment needs to be only found once. Modified intercepts can then be used in subsequent simulations.
 */
public class FertilityAlignment implements IEvaluation {

    private double targetFertilityRate;
    private Set<Person> persons;
    private SimPathsModel model;


    // CONSTRUCTOR
    public FertilityAlignment(Set<Person> persons) {
        this.model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        this.persons = persons;
        targetFertilityRate = Parameters.getFertilityRateByYear(model.getYear());
    }


    /**
     * Evaluates the discrepancy between the simulated and target total fertility rates adjusts probabilities if necessary.
     *
     * This method focuses on the influence of the adjustment parameter 'args[0]' on the difference between the target and
     * simulated fertility rates (error).
     *
     * The error value is returned and serves as the stopping condition in root search routines.
     *
     * @param args An array of parameters, where args[0] represents the adjustment parameter.
     * @return The error in the target aggregate share of partnered persons after potential adjustments.
     */
    @Override
    public double evaluate(double[] args) {

        double newAlignAdjustment = args[0] + Parameters.getTimeSeriesValue(model.getYear(), TimeSeriesVariable.FertilityAdjustment);
        persons.parallelStream()
                .forEach(person -> person.fertility(newAlignAdjustment));

        return targetFertilityRate - evalFertilityRate();
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
    private double evalFertilityRate() {

        FertileFilter filter = new FertileFilter();
        long numFertilePersons = model.getPersons().stream()
                .filter(person -> filter.evaluate(person))
                .count();
        long numBirths = model.getPersons().stream()
                .filter(person -> (person.isToGiveBirth()))
                .count();

        return (numFertilePersons > 0) ? (double) numBirths / numFertilePersons : 0.0;
    }
}
