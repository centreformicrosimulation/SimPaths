package simpaths.model.decisions;

import simpaths.data.IEvaluation;

import java.security.InvalidParameterException;


/**
 * CLASS TO MANAGE EVALUATION OF NESTED CES INTERTEMPORAL UTILITY FUNCTION
 */
public class CESUtility implements IEvaluation {


    /**
     * ATTRIBUTES
     */
    Expectations expectations;      // expectations initialised and updated to account for discrete control variables
    Grid valueFunction;            // storage of preceding solutions for value function

    // CES utility options
    public static final double EPSILON = 0.6;                                      // elasticity of substitution between equivalised consumption and leisure within each year
    public static final double ALPHA_SINGLES = 2.2 * 0.01756;                      // utility price of leisure for single adults
    public static final double ALPHA_COUPLES = 2.2 * 0.47 * 0.01756;               // utility price of leisure for couples
    public static final double GAMMA = 1.55;                                       // (constant) coefficient of risk aversion equal to inverse of intertemporal elasticity
    public static final double ZETA0_SINGLES = 1000;                               // warm-glow bequests parameter for singles - additive
    public static final double ZETA1_SINGLES = 0;                                  // warm-glow bequests parameter for singles - slope
    public static final double ZETA0_COUPLES = 1000;                               // warm-glow bequests parameter for couples - additive
    public static final double ZETA1_COUPLES = 0;                                  // warm-glow bequests parameter for couples - slope
    public static final double DELTA_SINGLES = 0.97;                               // exponential intertemporal discount factor for singles
    public static final double DELTA_COUPLES = 0.93;                               // exponential intertemporal discount factor for couples

    /**
     * CONSTRUCTOR
     */
    public CESUtility(Grid valueFunction, Expectations expectations) {

        // initialise data for evaluation of expectations
        this.valueFunction = valueFunction;
        this.expectations = expectations;
    }


    /*
     * WORKER METHODS
     */


    /**
     * METHOD TO EVALUATE EXPECTED LIFETIME UTILITY
     *
     * @param args arguments of utility function
     * @return expected lifetime utility, assuming optimising behaviour in the future
     */
    @Override
    public double evaluate(double[] args) {

        if (args.length != 1) {
            throw new InvalidParameterException("CESUtility function not supplied with expected number of arguments");
        }

        // evaluate within period utility
        double consumptionAnnual = args[0];
        double consumptionComponent = Math.pow(consumptionAnnual/expectations.equivalenceScale, 1 - 1/ EPSILON);
        double leisureComponent = Math.pow(expectations.leisureTime, 1 - 1/ EPSILON);
        double priceOfLeisure;
        if (expectations.cohabitation) {
            priceOfLeisure = Math.pow(ALPHA_COUPLES, 1/ EPSILON);
        } else {
            priceOfLeisure = Math.pow(ALPHA_SINGLES, 1/ EPSILON);
        }
        double periodUtility = Math.pow(consumptionComponent + priceOfLeisure * leisureComponent, (1- GAMMA)/(1-1/ EPSILON));

        // adjust expectations array
        int dim;
        double numeraire, gridValue;
        for (States states : expectations.anticipated) {
            dim = 0;
            // allow for liquid wealth
            numeraire = expectations.liquidWealth + expectations.disposableIncomeAnnual - consumptionAnnual;
            gridValue = Math.log(numeraire + DecisionParams.C_LIQUID_WEALTH);
            if (expectations.ageIndexNextPeriod < valueFunction.scale.axes.length) {
                gridValue = Math.max(gridValue, valueFunction.scale.axes[expectations.ageIndexNextPeriod][0][1]);
                gridValue = Math.min(gridValue, valueFunction.scale.axes[expectations.ageIndexNextPeriod][0][2]);
            }
            states.states[dim] = gridValue;
        }

        // evaluate expected utility
        double sumProb = 0.0;
        double probThreshold;
        if (DecisionParams.FILTER_LOCAL_EXPECTATIONS)
            probThreshold = DecisionParams.MIN_FACTOR_PROBABILITY / (double) expectations.probability.length;
        else
            probThreshold = 1.0E-12;
        double expectedUtility = 0.0;
        double expectedV;
        double zeta0, zeta1;
        if (expectations.cohabitation) {
            zeta0 = ZETA0_COUPLES;
            zeta1 = ZETA1_COUPLES;
        } else {
            zeta0 = ZETA0_SINGLES;
            zeta1 = ZETA1_SINGLES;
        }
        double bequest;
        if (expectations.anticipated.length>0) {
            for (int ii=0; ii<expectations.anticipated.length; ii++) {
                if (expectations.probability[ii] > probThreshold) {
                    sumProb += expectations.probability[ii];
                    if ( 1.0 - expectations.mortalityProbability > probThreshold ) {
                        expectedV = valueFunction.interpolateAll(expectations.anticipated[ii], true);
                        expectedUtility += expectations.probability[ii] *
                                (1.0 - expectations.mortalityProbability) * Math.pow(expectedV, 1 - GAMMA);
                    }
                    if (expectations.mortalityProbability > probThreshold && zeta1 > 0) {
                        bequest = Math.max(0, Math.exp(expectations.anticipated[ii].states[0])- DecisionParams.C_LIQUID_WEALTH);
                        expectedUtility += expectations.probability[ii] *
                                        expectations.mortalityProbability * Math.pow(zeta1 * (zeta0 + bequest), 1 - GAMMA);
                    }
                }
            }
        } else {
            sumProb = 1.0;
        }
        if (sumProb<0.8)
            throw new RuntimeException("utility expectation associated with low aggregate probability vector");
        expectedUtility /= sumProb;

        // evaluate total utility for passing to minimisation function
        double discountFactor;
        if (expectations.cohabitation) {
            discountFactor = DELTA_COUPLES;
        } else {
            discountFactor = DELTA_SINGLES;
        }
        double totalUtility = Math.pow(periodUtility + discountFactor * expectedUtility, 1/(1- GAMMA));

        // return
        return - totalUtility;
    }
}
