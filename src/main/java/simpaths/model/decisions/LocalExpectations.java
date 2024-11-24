package simpaths.model.decisions;


import microsim.statistics.regression.RegressionType;
import simpaths.data.ManagerRegressions;
import simpaths.data.RegressionName;
import simpaths.model.Person;
import microsim.statistics.regression.IntegerValuedEnum;
import simpaths.model.enums.ReversedIndicator;

import java.util.Map;


public class LocalExpectations {

    double[] probabilities;         // array of probabilities
    double[] values;                // array of values


    public LocalExpectations(){}


    /**
     * WORKER METHODS
     */
    public void screenAndAssign(double[] probs, double[] vals) {

        double sumProbs = 0.0, delProbs = 0.0;
        int drop = 0;
        for (double prob : probs) {
            if (DecisionParams.FILTER_LOCAL_EXPECTATIONS && prob<DecisionParams.MIN_STATE_PROBABILITY) {
                drop++;
                delProbs += prob;
            } else {
                sumProbs += prob;
            }
        }
        if (Math.abs(sumProbs+delProbs-1.0)>=1.0E-5)
            throw new RuntimeException("local expectations supplied probability vector that does not sum to 1.0");
        if (drop==0 && Math.abs(sumProbs-1.0)<1.0E-5) {
            probabilities = probs;
            values = vals;
        } else {
            int ii = 0;
            double probCheck = 0.0;
            probabilities = new double[probs.length - drop];
            values = new double[probs.length - drop];
            for (int jj=0; jj<probs.length; jj++) {
                if (probs[jj]>=DecisionParams.MIN_STATE_PROBABILITY) {
                    double probHere = probs[jj] / sumProbs;
                    probCheck += probHere;
                    probabilities[ii] = probHere;
                    values[ii] = vals[jj];
                    ii++;
                }
            }
            if (ii!=probs.length - drop || Math.abs(probCheck-1.0)>1.0E-5)
                throw new RuntimeException("problem evaluating probabilities for local expectations");
        }
    }

    public void assignValue(double value) {
        probabilities = new double[] {1.0};
        values = new double[] {value};
    }

    public void evaluateLabelledIndicator(Person person, RegressionName regression, Double valueTrue) {
        double[] probs, vals;
        double prob = ManagerRegressions.getProbability(person, regression);
        probs = new double[] {1.0-prob, prob};
        vals = new double[] {0.0, valueTrue};
        screenAndAssign(probs, vals);
    }

    public <E extends Enum<E> & IntegerValuedEnum> void evaluateDiscrete(Person person, RegressionName regression) {
        double[] probs, vals;
        Map<E,Double> probsMap = ManagerRegressions.getProbabilities(person, regression);
        int nn = probsMap.size();
        if (nn<2)
            throw new RuntimeException("call to evaluate multinomial probabilities returned fewer than 2 results");
        vals = new double[nn];
        probs = new double[nn];
        int ii = 0;
        for (E key : probsMap.keySet()) {
            double prob = probsMap.get(key);
            if (prob<0.0 && !RegressionType.GenOrderedProbit.equals(regression.getType()) && !RegressionType.GenOrderedLogit.equals(regression.getType()))
                throw new RuntimeException("negative probability evaluated for local expectations");
            probs[ii] = Math.max(0.0,probsMap.get(key));
            if (key instanceof ReversedIndicator)
                vals[ii] = 1.0 - (double)key.getValue();
            else
                vals[ii] = (double)key.getValue();
            ii++;
        }
        screenAndAssign(probs, vals);
    }

    public void evaluateGaussian(Person person, RegressionName regression, double minValue, double maxValue, double cTransform) {

        if (!RegressionType.Linear.equals(regression.getType()))
            throw new RuntimeException("unexpected regression specification submitted for evaluation of local expectations");

        double[] probs, vals;
        Double rmse = ManagerRegressions.getRmse(regression);
        Double score = ManagerRegressions.getScore(person, regression);
        probs = new double[DecisionParams.PTS_IN_QUADRATURE];
        vals = new double[DecisionParams.PTS_IN_QUADRATURE];
        for (int ii = 0; ii< DecisionParams.PTS_IN_QUADRATURE; ii++) {
            probs[ii] = DecisionParams.quadrature.weights[ii];
            double value = score + rmse * DecisionParams.quadrature.abscissae[ii];
            value = Math.min(value, maxValue);
            value = Math.max(value, minValue);
            vals[ii] = Math.log(Math.exp(value) + cTransform);
        }
        screenAndAssign(probs, vals);
    }
}
