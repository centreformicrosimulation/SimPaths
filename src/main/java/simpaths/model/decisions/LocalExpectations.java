package simpaths.model.decisions;


import microsim.statistics.IDoubleSource;
import simpaths.data.ManagerRegressions;
import simpaths.data.RegressionNames;
import simpaths.model.enums.DoubleValuedEnum;
import simpaths.model.enums.Education;

import java.util.Map;


public class LocalExpectations {

    double[] probabilities;         // array of probabilities
    double[] values;                // array of values


    public LocalExpectations(double[] probs, double[] vals) {
        probabilities = probs;
        values = vals;
    }

    public LocalExpectations(IDoubleSource obj, RegressionNames regression) {
        if (regression.getValue()==1) {
            // binomial regression
            evaluateIndicator(obj, regression);
        } else if (regression.getValue()==2) {
            // multinomial regression
            evaluateMultinomial(obj, regression);
        } else {
            throw new RuntimeException("unexpected regression specification submitted for evaluation of local expectations");
        }
    }

    public LocalExpectations(IDoubleSource obj, RegressionNames regression, double minValue, double maxValue, double cTransform) {
        if (regression.getValue()==3) {
            // gaussian regression
            evaluateGaussian(obj, regression, minValue, maxValue, cTransform);
        } else {
            throw new RuntimeException("unexpected regression specification submitted for evaluation of local expectations");
        }
    }

    public LocalExpectations(IDoubleSource obj, RegressionNames regression, boolean reversePolarity) {
        evaluateIndicator(obj, regression, reversePolarity);
    }

    public LocalExpectations(double valueTrue) {
        this(valueTrue, 0.0, 1.0);
    }

    public LocalExpectations(double valueTrue, double valueFalse, double probabilityTrue) {
        if (Math.abs(probabilityTrue-1.0)<1.0E-5) {
            probabilities = new double[] {1.0};
            values = new double[] {valueTrue};
        } else {
            values = new double[] {valueFalse, valueTrue};
            probabilities = new double[] {1.0-probabilityTrue, probabilityTrue};
        }
    }


    /**
     * WORKER METHODS
     */
    private void evaluateIndicator(IDoubleSource obj, RegressionNames regression) {
        evaluateIndicator(obj, regression, false);
    }

    private void evaluateIndicator(IDoubleSource obj, RegressionNames regression, boolean reversePolarity) {
        double prob = ManagerRegressions.getProbability(obj, regression);
        probabilities = new double[] {1.0-prob, prob};
        if (reversePolarity)
            values = new double[] {1.0, 0.0};
        else
            values = new double[] {0.0, 1.0};
    }

    private <E extends Enum<E> & DoubleValuedEnum> void evaluateMultinomial(IDoubleSource obj, RegressionNames regression) {
        Map<E,Double> probs = ManagerRegressions.getMultinomialProbabilities(obj, regression);
        int nn = probs.size();
        if (nn<2)
            throw new RuntimeException("call to evaluate multinomial probabilities returned fewer than 2 results");
        values = new double[nn];
        probabilities = new double[nn];
        int ii = 0;
        for (E key : probs.keySet()) {
            probabilities[ii] = probs.get(key);
            values[ii] = key.getValue();
            ii++;
        }
    }

    private void evaluateGaussian(IDoubleSource obj, RegressionNames regression, double minValue, double maxValue, double cTransform) {

        Double rmse = ManagerRegressions.getRmse(regression);
        Double score = ManagerRegressions.getScore(obj, regression);
        probabilities = new double[DecisionParams.PTS_IN_QUADRATURE];
        values = new double[DecisionParams.PTS_IN_QUADRATURE];
        for (int ii = 0; ii< DecisionParams.PTS_IN_QUADRATURE; ii++) {
            probabilities[ii] = DecisionParams.quadrature.weights[ii];
            double value = score + rmse * DecisionParams.quadrature.abscissae[ii];
            value = Math.min(value, maxValue);
            value = Math.max(value, minValue);
            values[ii] = Math.log(Math.exp(value) + cTransform);
        }
    }
}
