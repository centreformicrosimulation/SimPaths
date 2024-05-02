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
    public static final double ANNUAL_CONSUMPTION_NORMALISATION_FACTOR = 15600.0;  // used to improve the curvature of the utility function with respect to consumption
    public static final double EPSILON = 0.3;                                      // elasticity of substitution between equivalised consumption and leisure within each year
    public static final double ALPHA_SINGLES = 0.05;                                // utility price of leisure for single adults
    public static final double ALPHA_COUPLES = 0.05;                                // utility price of leisure for couples
    public static final double GAMMA = 1.25;                                       // (constant) coefficient of risk aversion equal to inverse of intertemporal elasticity
    public static final double ZETA0_SINGLES = 52000.0;                            // warm-glow bequests parameter for singles - additive
    public static final double ZETA1_SINGLES = 1500.0;                             // warm-glow bequests parameter for singles - slope
    public static final double ZETA0_COUPLES = 52000.0;                            // warm-glow bequests parameter for couples - additive
    public static final double ZETA1_COUPLES = 1500.0;                             // warm-glow bequests parameter for couples - slope
    public static final double DELTA_SINGLES = 0.99;                               // exponential intertemporal discount factor for singles
    public static final double DELTA_COUPLES = 0.99;                               // exponential intertemporal discount factor for couples


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
        double consumptionNormalised = consumptionAnnual / ANNUAL_CONSUMPTION_NORMALISATION_FACTOR;
        double consumptionComponent = Math.pow(consumptionNormalised/expectations.equivalenceScale, 1.0 - 1.0/EPSILON);
        double leisureComponent = Math.pow(expectations.leisureTime, 1.0 - 1.0/EPSILON);
        double priceOfLeisure;
        if (expectations.cohabitation) {
            priceOfLeisure = Math.pow(ALPHA_COUPLES, 1.0/EPSILON);
        } else {
            priceOfLeisure = Math.pow(ALPHA_SINGLES, 1.0/EPSILON);
        }
        double periodUtility = Math.pow(consumptionComponent + priceOfLeisure * leisureComponent, (1.0 - GAMMA)/(1.0 - 1.0/EPSILON));

        // adjust expectations array
        int dim;
        double numeraire, gridValue;
        for (States states : expectations.anticipated) {
            dim = 0;
            // allow for liquid wealth
            numeraire = expectations.liquidWealth + expectations.disposableIncomeAnnual - consumptionAnnual;
            gridValue = Math.log( Math.max(1.0, numeraire + DecisionParams.C_LIQUID_WEALTH) );
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
        Double expectedUtility = 0.0;
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
                                (1.0 - expectations.mortalityProbability) * Math.pow(expectedV, 1.0 - GAMMA);
                        if (expectedUtility.isNaN())
                            throw new RuntimeException("expected utility expected utility 1");
                    }
                    if (expectations.mortalityProbability > probThreshold && zeta1 > 0) {
                        bequest = Math.max(0, Math.exp(expectations.anticipated[ii].states[0])- DecisionParams.C_LIQUID_WEALTH);
                        expectedUtility += expectations.probability[ii] * expectations.mortalityProbability *
                                zeta1 * Math.pow((zeta0 + bequest), 1.0 - GAMMA);
                        if (expectedUtility.isNaN())
                            throw new RuntimeException("expected utility expected utility 2");
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
        Double totalUtility = Math.pow(periodUtility + discountFactor * expectedUtility, 1.0/(1.0 - GAMMA));

        if (totalUtility.isNaN())
            throw new RuntimeException("failed to evaluate lifetime utility");

        // return
        return -totalUtility;
    }
}
