package simpaths.model.decisions;


import microsim.statistics.IDoubleSource;
import simpaths.data.ManagerRegressions;
import simpaths.data.RegressionNames;


/**
 *
 * CLASS TO FACILITATE EVALUATION OF PROBABILITIES AND VALUES FOR ISOLATED EVENTS
 *
 */
public class LocalExpectations {


    /**
     * ATTRIBUTES
     */
    double[] probabilities;         // array of probabilities
    double[] values;                // array of values


    /**
     * CONSTRUCTOR FOR DEFAULT INDICATOR VARIABLE (TRUE = 1.0)
     */
    public LocalExpectations(double val) {
        probabilities = new double[] {1.0};
        values = new double[] {val};
    }
    public LocalExpectations(IDoubleSource person, RegressionNames regression) {
        evaluateIndicator(person, 1.0, 0.0, regression);
    }
    public LocalExpectations(IDoubleSource person, double valueTrue, double valueFalse, RegressionNames regression) {
        evaluateIndicator(person, valueTrue, valueFalse, regression);
    }
    public LocalExpectations(double valueTrue, double valueFalse, double probabilityTrue) {
        evaluateIndicator(valueTrue, valueFalse, probabilityTrue);
    }


    /**
     * CONSTRUCTOR FOR GAUSSIAN DISTRIBUTION
     * @param expectation of Gaussian distribution
     * @param standardDeviation of Gaussian distribution
     * @param minValue lower bound imposed on abscissae
     * @param maxValue upper bound imposed on abscissae
     */
    public LocalExpectations(double expectation, double standardDeviation, double minValue, double maxValue) {
        probabilities = new double[DecisionParams.PTS_IN_QUADRATURE];
        values = new double[DecisionParams.PTS_IN_QUADRATURE];
        for (int ii = 0; ii< DecisionParams.PTS_IN_QUADRATURE; ii++) {
            probabilities[ii] = DecisionParams.quadrature.weights[ii];
            double value = expectation + standardDeviation * DecisionParams.quadrature.abscissae[ii];
            value = Math.min(value, maxValue);
            value = Math.max(value, minValue);
            values[ii] = value;
        }
    }


    /**
     * WORKER METHODS
     * @param person object to evaluate probability from probit regression
     * @param regression regression equation to evaluate
     */
    private void evaluateIndicator(IDoubleSource person, double valueTrue, double valueFalse, RegressionNames regression) {
        values = new double[2];
        probabilities = new double[2];
        double prob = ManagerRegressions.getProbability(person, regression);
        values[0] = valueFalse;
        values[1] = valueTrue;
        probabilities[0] = (1 - prob);
        probabilities[1] = prob;
    }

    private void evaluateIndicator(double valueTrue, double valueFalse, double probabilityTrue) {
        values = new double[2];
        probabilities = new double[2];
        values[0] = valueFalse;
        values[1] = valueTrue;
        probabilities[0] = (1 - probabilityTrue);
        probabilities[1] = probabilityTrue;
    }
}
