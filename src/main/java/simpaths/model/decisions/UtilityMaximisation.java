package simpaths.model.decisions;


import java.security.InvalidParameterException;


/**
 *
 * CLASS TO ENCAPSULATE SOLUTION TO UTILITY OPTIMISATION
 *
 */
public class UtilityMaximisation {


    /**
     * ATTRIBUTES
     */
    double optimisedUtility;   // numerical approximation for value function
    double[] controls;          // numerical approximation for control variables


    /**
     * CONSTRUCTOR
     */
    public UtilityMaximisation(Grid valueFunction, States states, Expectations expectations, double emp1Pr, double emp2Pr) {

        // update expectations for combination of discrete control variables
        expectations.updateForDiscreteControls(emp1Pr, emp2Pr);

        // instantiate assumed utility function object
        CESUtility function = new CESUtility(valueFunction, expectations);

        // initialise controls for optimisation problem
        int numberControls = 1;     // for consumption

        // initialise arrays for continuous controls
        double[] target = new double[numberControls];
        double[] lowerBounds = new double[numberControls];
        double[] upperBounds = new double[numberControls];

        // add in discrete control variables
        if (states.ageYears <= DecisionParams.maxAgeFlexibleLabourSupply) {
            if (DecisionParams.FLAG_IO_EMPLOYMENT1) {
                numberControls++;          // for principal employment
            }
            if (states.getCohabitation()) {
                if (DecisionParams.FLAG_IO_EMPLOYMENT2) {
                    numberControls++;      // for secondary employment
                }
            }
        }
        controls = new double[numberControls];

        // populate control arrays
        int dim = 0;
        if (expectations.cashOnHand <= DecisionParams.MIN_CONSUMPTION_PER_YEAR) {
            lowerBounds[dim] = DecisionParams.MIN_CONSUMPTION_PER_YEAR;
            upperBounds[dim] = DecisionParams.MIN_CONSUMPTION_PER_YEAR;
            target[dim] = DecisionParams.MIN_CONSUMPTION_PER_YEAR;
        } else {
            lowerBounds[dim] = DecisionParams.MIN_CONSUMPTION_PER_YEAR;
            upperBounds[dim] = expectations.cashOnHand;
            target[dim] = lowerBounds[dim] * 0.8 +upperBounds[dim] * 0.2;
        }
        dim++;


        // **********************************
        // pass for minimisation
        // **********************************
        Minimiser problem = new Minimiser(lowerBounds, upperBounds, target, function);
        problem.minimise();


        // pack for delivery
        optimisedUtility = - problem.minimisedValue;

        // pack consumption solution
        dim = 0;
        if (expectations.cashOnHand < DecisionParams.MIN_CONSUMPTION_PER_YEAR) {
            controls[dim] = 1.0;
        } else {
            controls[dim] = problem.target[dim] / expectations.cashOnHand;
        }
        dim++;

        if (dim != problem.target.length) {
            throw new InvalidParameterException("minimisation function has not delivered results for anticipated number of continuous controls");
        }

        // allow for employment controls
        if (states.ageYears <= DecisionParams.maxAgeFlexibleLabourSupply) {
            if (DecisionParams.FLAG_IO_EMPLOYMENT1) {
                controls[dim] = emp1Pr;
                dim++;
            }
            if (states.getCohabitation()) {
                if (DecisionParams.FLAG_IO_EMPLOYMENT2) {
                    controls[dim] = emp2Pr;
                    dim++;
                }
            }
        }
    }
}
