package simpaths.model.decisions;


import microsim.statistics.IDoubleSource;
import simpaths.data.ManagerRegressions;
import simpaths.data.Parameters;
import simpaths.data.RegressionName;
import simpaths.data.RegressionType;
import simpaths.model.Person;
import simpaths.model.enums.IntegerValuedEnum;
import simpaths.model.enums.TimeSeriesVariable;

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

    public void evaluate(Person person, RegressionName regression) {

        if (RegressionType.StandardProbit.equals(regression.getType()) || RegressionType.AdjustedStandardProbit.equals(regression.getType())) {
            // binomial regression
            evaluateIndicator(person, regression);
        } else if (RegressionType.ReversedProbit.equals(regression.getType())) {
            evaluateIndicator(person, regression, 0.0);
        } else if (RegressionType.MultinomialLogit.equals(regression.getType()) || RegressionType.OrderedProbit.equals(regression.getType())) {
            // multinomial regression
            evaluateMultinomial(person, regression);
        } else {
            throw new RuntimeException("unexpected regression specification submitted for evaluation of local expectations");
        }
    }

    public void assignValue(double value) {
        probabilities = new double[] {1.0};
        values = new double[] {value};
    }

    public void evaluateIndicator(Person person, RegressionName regression) {
        evaluateIndicator(person, regression, 1.0);
    }

    public void evaluateIndicator(Person person, RegressionName regression, Double valueTrue) {
        double[] probs, vals;
        double score = ManagerRegressions.getScore(person, regression);
        double adj = 0.0;
        if (RegressionType.AdjustedStandardProbit.equals(regression.getType())) {
           adj = Parameters.getTimeSeriesValue(person.getYear(), TimeSeriesVariable.FertilityAdjustment);
        }

        double prob = ManagerRegressions.getProbability(score + adj, regression);
        probs = new double[] {1.0-prob, prob};
        if (Math.abs(valueTrue)<1.0E-5)
            vals = new double[] {1.0, 0.0};
        else
            vals = new double[] {0.0, valueTrue};
        screenAndAssign(probs, vals);
    }

    private <E extends Enum<E> & IntegerValuedEnum> void evaluateMultinomial(Person person, RegressionName regression) {
        double[] probs, vals;
        Map<E,Double> probsMap = ManagerRegressions.getMultinomialProbabilities(person, regression);
        int nn = probsMap.size();
        if (nn<2)
            throw new RuntimeException("call to evaluate multinomial probabilities returned fewer than 2 results");
        vals = new double[nn];
        probs = new double[nn];
        int ii = 0;
        for (E key : probsMap.keySet()) {
            probs[ii] = probsMap.get(key);
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
