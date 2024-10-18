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
    public static final double ANNUAL_CONSUMPTION_NORMALISATION_FACTOR = 15600.0;   // used to improve the curvature of the utility function with respect to consumption
    public static final double BEQUEST_NORMALISATION_FACTOR = 250000.0;
    public static final double EPSILON = 0.34;                                      // elasticity of substitution between equivalised consumption and leisure within each year
    public static final double ALPHA_YOUNG = 1.26;                                   // utility price of leisure for single adults
    public static final double ALPHA_MID = 1.26;                                     // utility price of leisure for single adults
    public static final double ALPHA_OLD = 1.26;                                     // utility price of leisure for couples
    public static final double GAMMA = 2.0;                                         // (constant) coefficient of risk aversion equal to inverse of intertemporal elasticity
    public static final double ZETA0 = 17.0;                                        // warm-glow bequests parameter for singles - additive
    public static final double ZETA1 = 0.4;                                         // warm-glow bequests parameter for singles - slope
    public static final double DELTA_SINGLES = 0.98;                                // exponential intertemporal discount factor for singles
    public static final double DELTA_COUPLES = 0.98;                                // exponential intertemporal discount factor for couples


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
        double alpha;
        if (expectations.ageYearsThisPeriod<30) {
            alpha = ALPHA_YOUNG;
        } else if (expectations.ageYearsThisPeriod<40) {
            alpha = (ALPHA_YOUNG * (double) (40 - expectations.ageYearsThisPeriod) + ALPHA_MID * (double) (expectations.ageYearsThisPeriod - 30)) / 10.0;
        } else if (expectations.ageYearsThisPeriod<55) {
            alpha = ALPHA_MID;
        } else if (expectations.ageYearsThisPeriod<65) {
            alpha = (ALPHA_MID * (double) (65 - expectations.ageYearsThisPeriod) + ALPHA_OLD * (double) (expectations.ageYearsThisPeriod - 55)) / 10.0;
        } else {
            alpha = ALPHA_OLD;
        }
        double priceOfLeisure = Math.pow(alpha, 1.0/EPSILON);
        double periodUtility = Math.pow(consumptionComponent + priceOfLeisure * leisureComponent, (1.0 - GAMMA)/(1.0 - 1.0/EPSILON));
        periodUtility /= (1.0 - GAMMA);

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
        if (expectations.anticipated.length>0) {
            for (int ii=0; ii<expectations.anticipated.length; ii++) {
                if (expectations.probability[ii] > probThreshold) {
                    sumProb += expectations.probability[ii];
                    Double expectedV=0.0, utilBequest=0.0;
                    double bequest;
                    if ( 1.0 - expectations.mortalityProbability > probThreshold ) {
                        expectedV = valueFunction.interpolateAll(expectations.anticipated[ii], true);
                        if (expectedV.isNaN())
                            throw new RuntimeException("expected utility expected utility 1");
                    }
                    if (expectations.mortalityProbability > probThreshold && ZETA0 > 0) {
                        bequest = Math.max(0, Math.exp(expectations.anticipated[ii].states[0])- DecisionParams.C_LIQUID_WEALTH);
                        utilBequest = ZETA0 * Math.pow(bequest / BEQUEST_NORMALISATION_FACTOR, ZETA1);
                        if (utilBequest.isNaN())
                            throw new RuntimeException("expected utility expected utility 2");
                    }
                    expectedUtility += expectations.probability[ii] *
                            ((1.0-expectations.mortalityProbability) * expectedV + expectations.mortalityProbability * utilBequest);
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
        Double totalUtility = periodUtility + discountFactor * expectedUtility;

        if (totalUtility.isNaN())
            throw new RuntimeException("failed to evaluate lifetime utility");

        // return
        return -totalUtility;
    }
}
