package simpaths.data;


import microsim.statistics.IDoubleSource;
import microsim.statistics.regression.LinearRegression;
import microsim.statistics.regression.MultiLogitRegression;
import microsim.statistics.regression.OrderedProbitRegression;
import microsim.statistics.regression.ProbitRegression;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import simpaths.model.Person;
import simpaths.model.enums.IntegerValuedEnum;
import simpaths.model.enums.Labour;

import java.security.InvalidParameterException;
import java.util.*;


/**
 *
 * CLASS TO MANAGE EVALUATION OF SUPPLEMENTARY DATA FOR INTERACTING WITH JAS-MINE REGRESSION METHODS
 *
 */
public class ManagerRegressions {

    public static ProbitRegression getProbitRegression(RegressionName regression) {

        if (!RegressionType.StandardProbit.equals(regression.getType()) &&
                !RegressionType.AdjustedStandardProbit.equals(regression.getType()) &&
                !RegressionType.ReversedProbit.equals(regression.getType()))
            throw new RuntimeException("requested ProbitRegression object is not a probit");

        switch (regression) {
            case EducationE1a -> {
                return Parameters.getRegEducationE1a();
            }
            case EducationE1b -> {
                return Parameters.getRegEducationE1b();
            }
            case HealthH2b -> {
                return Parameters.getRegHealthH2b();
            }
            case UnemploymentU1a -> {
                return Parameters.getRegUnemploymentMaleGraduateU1a();
            }
            case UnemploymentU1b -> {
                return Parameters.getRegUnemploymentMaleNonGraduateU1b();
            }
            case UnemploymentU1c -> {
                return Parameters.getRegUnemploymentFemaleGraduateU1c();
            }
            case UnemploymentU1d -> {
                return Parameters.getRegUnemploymentFemaleNonGraduateU1d();
            }
            case SocialCareS2a -> {
                return Parameters.getRegNeedCareS2a();
            }
            case SocialCareS2b -> {
                return Parameters.getRegReceiveCareS2b();
            }
            case SocialCareS3c -> {
                return Parameters.getRegNoPartnerProvCareToOtherS3c();
            }
            case PartnershipU1a -> {
                return Parameters.getRegPartnershipU1a();
            }
            case PartnershipU1b -> {
                return Parameters.getRegPartnershipU1b();
            }
            case PartnershipU2b -> {
                return Parameters.getRegPartnershipU2b();
            }
            case FertilityF1a -> {
                return Parameters.getRegFertilityF1a();
            }
            case FertilityF1b -> {
                return Parameters.getRegFertilityF1b();
            }
            default -> {
                throw new RuntimeException("unrecognised regression (1)");
            }
        }
    }

    public static OrderedProbitRegression getOrderedProbitRegression(RegressionName regression) {

        if (!RegressionType.OrderedProbit.equals(regression.getType()))
            throw new RuntimeException("requested OrderedProbitRegression object is not an ordered probit");

        switch (regression) {
            case HealthH1a -> {
                return Parameters.getRegHealthH1a();
            }
            case HealthH1b -> {
                return Parameters.getRegHealthH1b();
            }
            case EducationE2a -> {
                return Parameters.getRegEducationE2a();
            }
            default -> {
                throw new RuntimeException("unrecognised regression (2)");
            }
        }
    }

    public static MultiLogitRegression getMultiLogitRegression(RegressionName regression) {

        if (!RegressionType.MultinomialLogit.equals(regression.getType()))
            throw new RuntimeException("requested MultiLogitRegression object is not a multinomial logit");

        switch (regression) {
            case SocialCareS2c -> {
                return Parameters.getRegSocialCareMarketS2c();
            }
            case SocialCareS3d -> {
                return Parameters.getRegInformalCareToS3d();
            }
            default -> {
                throw new RuntimeException("unrecognised regression (2)");
            }
        }
    }

    public static LinearRegression getLinearRegression(RegressionName regression) {

        if (!RegressionType.Linear.equals(regression.getType()))
            throw new RuntimeException("requested LinearRegression object is not a linear regression");

        switch (regression) {
            case WagesMales -> {
                return Parameters.getRegWagesMales();
            }
            case WagesFemales -> {
                return Parameters.getRegWagesFemales();
            }
            case WagesMalesE -> {
                return Parameters.getRegWagesMalesE();
            }
            case WagesFemalesE -> {
                return Parameters.getRegWagesFemalesE();
            }
            case WagesMalesNE -> {
                return Parameters.getRegWagesMalesNE();
            }
            case WagesFemalesNE -> {
                return Parameters.getRegWagesFemalesNE();
            }
            default -> {
                throw new RuntimeException("unrecognised regression (3)");
            }
        }
    }

    public static double getScore(IDoubleSource person, RegressionName regression) {

        if (RegressionType.Linear.equals(regression.getType())) {

            return getLinearRegression(regression).getScore(person, Person.DoublesVariables.class);
        } else if (RegressionType.OrderedProbit.equals(regression.getType())) {

            return getOrderedProbitRegression(regression).getScore(person, Person.DoublesVariables.class);
        } else {

            return getProbitRegression(regression).getScore(person, Person.DoublesVariables.class);
        }
    }

    public static double getRmse(RegressionName regression) {

        String code;
        switch (regression) {
            case HealthH1a -> {
                code = "H1a";
            }
            case HealthH1b -> {
                code = "H1b";
            }
            case WagesMales -> {
                code = "Wages_Males";
            }
            case WagesMalesE -> {
                code = "Wages_MalesE";
            }
            case WagesMalesNE -> {
                code = "Wages_MalesNE";
            }
            case WagesFemales -> {
                code = "Wages_Females";
            }
            case WagesFemalesE -> {
                code = "Wages_FemalesE";
            }
            case WagesFemalesNE -> {
                code = "Wages_FemalesNE";
            }
            case ChildcareC1b -> {
                code = "C1b";
            }
            default -> {
                throw new InvalidParameterException("RMSE requested for unrecognised regression equation");
            }
        }
        return getRegressionCoeff(RegressionName.RMSE, code);
    }

    public static double getProbability(IDoubleSource person, RegressionName regression) {

        double probability = getProbitRegression(regression).getProbability(person, Person.DoublesVariables.class);
        if (probability > 1.0 || probability < 0.0) {
            throw new InvalidParameterException("Problem evaluating probability from probit regression equation");
        }
        return probability;
    }

    public static double getProbability(double score, RegressionName regression) {

        double probability = getProbitRegression(regression).getProbability(score);
        if (probability > 1.0 || probability < 0.0) {
            throw new InvalidParameterException("Problem evaluating probability from probit regression equation");
        }
        return probability;
    }

    public static <E extends Enum<E> & IntegerValuedEnum> Map<E, Double> getMultinomialProbabilities(IDoubleSource obj, RegressionName regression) {

        if (RegressionType.OrderedProbit.equals(regression.getType())) {
            return getOrderedProbitRegression(regression).getProbabilities(obj, Person.DoublesVariables.class);
        } else if (RegressionType.MultinomialLogit.equals(regression.getType())) {
            return getMultiLogitRegression(regression).getProbabilites(obj, Person.DoublesVariables.class);
        } else
            throw new InvalidParameterException("Probability requested for unrecognised multinomial regression equation");
    }

    public static double getRegressionCoeff(Enum<?> regression, String coeff) {
        Object oo = getRegressionCoeffObject(regression, coeff, false);
        if (oo instanceof Double) {
            return (double) oo;
        } else {
            oo = getRegressionCoeffObject(regression, coeff, true);
            if (oo instanceof Double) {
                return (double) oo;
            } else {
                throw new RuntimeException("Regression coefficiant " + coeff + " not found in " + regression.name());
            }
        }
    }
    private static Object getRegressionCoeffObject(Enum<?> regression, String coeff, boolean multi) {
        switch ((RegressionName) regression) {
            case WagesMalesE -> {
                return (multi) ? Parameters.getCoeffCovarianceWagesMalesE().getValue(coeff, "COEFFICIENT") :
                    Parameters.getCoeffCovarianceWagesMalesE().getValue(coeff);
            }
            case WagesMalesNE -> {
                return (multi) ? Parameters.getCoeffCovarianceWagesMalesNE().getValue(coeff, "COEFFICIENT") :
                    Parameters.getCoeffCovarianceWagesMalesNE().getValue(coeff);
            }
            case WagesFemalesE -> {
                return (multi) ? Parameters.getCoeffCovarianceWagesFemalesE().getValue(coeff, "COEFFICIENT") :
                    Parameters.getCoeffCovarianceWagesFemalesE().getValue(coeff);
            }
            case WagesFemalesNE -> {
                return (multi) ? Parameters.getCoeffCovarianceWagesFemalesNE().getValue(coeff, "COEFFICIENT") :
                    Parameters.getCoeffCovarianceWagesFemalesNE().getValue(coeff);
            }
            case RMSE -> {
                return (multi) ? Parameters.getCoefficientMapRMSE().getValue(coeff, "COEFFICIENT") :
                    Parameters.getCoefficientMapRMSE().getValue(coeff);
            }
            default -> {
                throw new RuntimeException("Coefficient retrieval not supported for regression type " + regression.name());
            }
        }
    }

    public static <E extends IntegerValuedEnum> E multiEvent(Map<E, Double> probs, double rand) {

        double cprob = 0.0;
        List<E> keys = new ArrayList<>();
        for (E ee : probs.keySet()) {
            if (probs.get(ee) != null) {
                cprob += probs.get(ee);
                for (int ii=0; ii<keys.size(); ii++) {
                    E ee1 = keys.get(ii);
                    if (ee1.getValue() > ee.getValue()) {
                        keys.add(ii, ee);
                        break;
                    }
                }
                if (!keys.contains(ee))
                    keys.add(ee);
            } else {
                throw new RuntimeException("problem identifying probabilities for multinomial object (2)");
            }
        }

        double prob = 0.0;
        for (E ee : keys) {
            prob += probs.get(ee) / cprob;
            if (rand < prob) {
                return ee;
            }
        }
        throw new RuntimeException("failed to identify new enumerator for multi-event (2)");
    }

    public static MultiKey<? extends Labour> multiEvent(MultiKeyMap<Labour, Double> probs, double rand) {

        double cprob = 0.0;
        List<MultiKey<? extends Labour>> keys = new ArrayList<>();
        for (MultiKey<? extends Labour> ee : probs.keySet()) {
            if (probs.get(ee) != null) {
                cprob += probs.get(ee);
                int eeVal = getMultiKeyValue(ee);
                for (int ii=0; ii<keys.size(); ii++) {
                    MultiKey<? extends Labour> ee1 = keys.get(ii);
                    int ee1Val = getMultiKeyValue(ee1);
                    if (ee1Val > eeVal) {
                        keys.add(ii, ee);
                        break;
                    }
                }
                if (!keys.contains(ee))
                    keys.add(ee);
            } else {
                throw new RuntimeException("problem identifying probabilities for multinomial object (3)");
            }
        }

        double prob = 0.0;
        for (MultiKey<? extends Labour> ee : keys) {
            prob += probs.get(ee) / cprob;
            if (rand < prob) {
                return ee;
            }
        }
        throw new RuntimeException("failed to identify new enumerator for multi-event (3)");
    }
    private static int getMultiKeyValue(MultiKey<? extends Labour> ee) {
        int val = 0;
        int fctr = 1;
        for (int ii=0; ii<ee.size(); ii++) {
            val += ee.getKey(ii).getValue() * fctr;
            fctr *= 100;
        }
        return val;
    }
}
