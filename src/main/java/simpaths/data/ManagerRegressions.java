package simpaths.data;


import microsim.statistics.IDoubleSource;
import simpaths.model.Person;
import simpaths.model.enums.DoubleValuedEnum;

import java.security.InvalidParameterException;
import java.util.Map;


/**
 *
 * CLASS TO MANAGE EVALUATION OF SUPPLEMENTARY DATA FOR INTERACTING WITH JAS-MINE REGRESSION METHODS
 *
 */
public class ManagerRegressions {

    public static double getScore(IDoubleSource person, Enum<?> regression) {

        switch ((RegressionNames) regression) {
            case HealthH1a:
                return Parameters.getRegHealthH1a().getScore(person, Person.DoublesVariables.class);
            case HealthH1b:
                return Parameters.getRegHealthH1b().getScore(person, Person.DoublesVariables.class);
            case WagesMales:
                return Parameters.getRegWagesMales().getScore(person, Person.DoublesVariables.class);
            case WagesFemales:
                return Parameters.getRegWagesFemales().getScore(person, Person.DoublesVariables.class);
            case WagesMalesE:
                return Parameters.getRegWagesMalesE().getScore(person, Person.DoublesVariables.class);
            case WagesFemalesE:
                return Parameters.getRegWagesFemalesE().getScore(person, Person.DoublesVariables.class);
            case WagesMalesNE:
                return Parameters.getRegWagesMalesNE().getScore(person, Person.DoublesVariables.class);
            case WagesFemalesNE:
                return Parameters.getRegWagesFemalesNE().getScore(person, Person.DoublesVariables.class);
            default:
                throw new RuntimeException("Score requested for unrecognised regression equation");
        }
    }

    public static double getRmse(Enum<?> regression) {

        String code;
        switch ((RegressionNames) regression) {
            case HealthH1a:
                code = "H1a";
                break;
            case HealthH1b:
                code = "H1b";
                break;
            case WagesMales:
                code = "Wages_Males";
                break;
            case WagesMalesE:
                code = "Wages_MalesE";
                break;
            case WagesMalesNE:
                code = "Wages_MalesNE";
                break;
            case WagesFemales:
                code = "Wages_Females";
                break;
            case WagesFemalesE:
                code = "Wages_FemalesE";
                break;
            case WagesFemalesNE:
                code = "Wages_FemalesNE";
                break;
            case ChildcareC1b:
                code = "C1b";
                break;
            default:
                throw new InvalidParameterException("RMSE requested for unrecognised regression equation");
        }
        return getRegressionCoeff(RegressionNames.RMSE, code);
    }

    public static double getProbability(IDoubleSource obj, RegressionNames regression) {

        if (regression.getValue()!=1)
            throw new RuntimeException("probability requested from non-binary regression equation");
        double probability;
        switch (regression) {
            case EducationE1a:
                probability = Parameters.getRegEducationE1a().getProbability(obj, Person.DoublesVariables.class);
                break;
            case EducationE1b:
                probability = Parameters.getRegEducationE1b().getProbability(obj, Person.DoublesVariables.class);
                break;
            case HealthH2b:
                probability = Parameters.getRegHealthH2b().getProbability(obj, Person.DoublesVariables.class);
                break;
            case UnemploymentU1a:
                probability = Parameters.getRegUnemploymentMaleGraduateU1a().getProbability(obj, Person.DoublesVariables.class);
                break;
            case UnemploymentU1b:
                probability = Parameters.getRegUnemploymentMaleNonGraduateU1b().getProbability(obj, Person.DoublesVariables.class);
                break;
            case UnemploymentU1c:
                probability = Parameters.getRegUnemploymentFemaleGraduateU1c().getProbability(obj, Person.DoublesVariables.class);
                break;
            case UnemploymentU1d:
                probability = Parameters.getRegUnemploymentFemaleNonGraduateU1d().getProbability(obj, Person.DoublesVariables.class);
                break;
            case SocialCareS2a:
                probability = Parameters.getRegNeedCareS2a().getProbability(obj, Person.DoublesVariables.class);
                break;
            case SocialCareS2b:
                probability = Parameters.getRegReceiveCareS2b().getProbability(obj, Person.DoublesVariables.class);
                break;
            case SocialCareS3c:
                probability = Parameters.getRegNoPartnerProvCareToOtherS3c().getProbability(obj, Person.DoublesVariables.class);
                break;
            case PartnershipU1a:
                probability = Parameters.getRegPartnershipU1a().getProbability(obj, Person.DoublesVariables.class);
                break;
            case PartnershipU1b:
                probability = Parameters.getRegPartnershipU1b().getProbability(obj, Person.DoublesVariables.class);
                break;
            case PartnershipU2b:
                probability = Parameters.getRegPartnershipU2b().getProbability(obj, Person.DoublesVariables.class);
                break;
            case FertilityF1a:
                probability = Parameters.getRegFertilityF1a().getProbability(obj, Person.DoublesVariables.class);
                break;
            case FertilityF1b:
                probability = Parameters.getRegFertilityF1b().getProbability(obj, Person.DoublesVariables.class);
                break;
            default:
                throw new InvalidParameterException("Probability requested for unrecognised probit regression equation");
        }
        if (probability > 1.0 || probability < 0.0) {
            throw new InvalidParameterException("Problem evaluating probability from probit regression equation");
        }
        return probability;
    }

    public static <E extends Enum<E> & DoubleValuedEnum> Map<E, Double> getMultinomialProbabilities(IDoubleSource obj, RegressionNames regression) {

        if (regression.getValue()!=2)
            throw new RuntimeException("probabilities requested from non multinomial equation");
        switch (regression) {
            case EducationE2a:
                return Parameters.getRegEducationE2a().getProbabilities(obj, Person.DoublesVariables.class);
            case HealthH1a:
                return Parameters.getRegHealthH1a().getProbabilities(obj, Person.DoublesVariables.class);
            case HealthH1b:
                return Parameters.getRegHealthH1b().getProbabilities(obj, Person.DoublesVariables.class);
            case SocialCareS2c:
                return Parameters.getRegSocialCareMarketS2c().getProbabilites(obj, Person.DoublesVariables.class);
            case SocialCareS3d:
                return Parameters.getRegInformalCareToS3d().getProbabilites(obj, Person.DoublesVariables.class);
            default:
                throw new InvalidParameterException("Probability requested for unrecognised multinomial regression equation");
        }
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
        switch ((RegressionNames) regression) {
            case WagesMalesE:
                return (multi) ? Parameters.getCoeffCovarianceWagesMalesE().getValue(coeff, "COEFFICIENT") :
                        Parameters.getCoeffCovarianceWagesMalesE().getValue(coeff);
            case WagesMalesNE:
                return (multi) ? Parameters.getCoeffCovarianceWagesMalesNE().getValue(coeff, "COEFFICIENT") :
                        Parameters.getCoeffCovarianceWagesMalesNE().getValue(coeff);
            case WagesFemalesE:
                return (multi) ? Parameters.getCoeffCovarianceWagesFemalesE().getValue(coeff, "COEFFICIENT") :
                        Parameters.getCoeffCovarianceWagesFemalesE().getValue(coeff);
            case WagesFemalesNE:
                return (multi) ? Parameters.getCoeffCovarianceWagesFemalesNE().getValue(coeff, "COEFFICIENT") :
                        Parameters.getCoeffCovarianceWagesFemalesNE().getValue(coeff);
            case RMSE:
                return (multi) ? Parameters.getCoefficientMapRMSE().getValue(coeff, "COEFFICIENT") :
                        Parameters.getCoefficientMapRMSE().getValue(coeff);
            default:
                throw new RuntimeException("Coefficient retrieval not supported for regression type " + regression.name());
        }
    }

    public static <T> T multiEvent(Map<T, Double> probs, double rand) {

        double cprob = 0.0;
        for (T tt : probs.keySet()) {
            if (probs.get(tt) != null) {
                cprob += probs.get(tt);
            } else {
                throw new RuntimeException("problem identifying probabilities for multinomial object (2)");
            }
        }

        double prob = 0.0;
        for (T tt : probs.keySet()) {
            prob += probs.get(tt) / cprob;
            if (rand < prob) {
                return tt;
            }
        }
        throw new RuntimeException("failed to identify new enumerator for multi-event (2)");
    }
}
