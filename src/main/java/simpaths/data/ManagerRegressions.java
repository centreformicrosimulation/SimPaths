package simpaths.data;


import microsim.statistics.IDoubleSource;
import microsim.statistics.regression.*;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import simpaths.model.Person;
import simpaths.model.enums.Labour;

import java.security.InvalidParameterException;
import java.util.*;


/**
 *
 * CLASS TO MANAGE EVALUATION OF SUPPLEMENTARY DATA FOR INTERACTING WITH JAS-MINE REGRESSION METHODS
 *
 */
public class ManagerRegressions {

    public static LinearRegression getLinearRegression(RegressionName regression) {

        if (!RegressionType.Linear.equals(regression.getType()))
            throw new RuntimeException("requested LinearRegression object is not a linear regression");

        switch (regression) {
            case ChildcareC1b -> {
                return Parameters.getRegChildcareC1b();
            }
            case HealthHM1Level -> {
                return Parameters.getRegHealthHM1Level();
            }
            case HealthHM2LevelMales -> {
                return Parameters.getRegHealthHM2LevelMales();
            }
            case HealthHM2LevelFemales -> {
                return Parameters.getRegHealthHM2LevelFemales();
            }
            case HealthMCS1 -> {
                return Parameters.getRegHealthMCS1();
            }
            case HealthMCS2Males -> {
                return Parameters.getRegHealthMCS2Males();
            }
            case HealthMCS2Females -> {
                return Parameters.getRegHealthMCS2Females();
            }
            case HealthPCS1 -> {
                return Parameters.getRegHealthPCS1();
            }
            case HealthPCS2Males -> {
                return Parameters.getRegHealthPCS2Males();
            }
            case HealthPCS2Females -> {
                return Parameters.getRegHealthPCS2Females();
            }
            case LifeSatisfaction1 -> {
                return Parameters.getRegLifeSatisfaction1();
            }
            case LifeSatisfaction2Males -> {
                return Parameters.getRegLifeSatisfaction2Males();
            }
            case LifeSatisfaction2Females -> {
                return Parameters.getRegLifeSatisfaction2Females();
            }
            case HealthEQ5D -> {
                return Parameters.getRegEQ5D();
            }
            case SocialCareS1b -> {
                return Parameters.getRegCareHoursS1b();
            }
            case SocialCareS2g -> {
                return Parameters.getRegPartnerCareHoursS2g();
            }
            case SocialCareS2h -> {
                return Parameters.getRegDaughterCareHoursS2h();
            }
            case SocialCareS2i -> {
                return Parameters.getRegSonCareHoursS2i();
            }
            case SocialCareS2j -> {
                return Parameters.getRegOtherCareHoursS2j();
            }
            case SocialCareS2k -> {
                return Parameters.getRegFormalCareHoursS2k();
            }
            case SocialCareS3e -> {
                return Parameters.getRegCareHoursProvS3e();
            }
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

    public static BinomialRegression getBinomialRegression(RegressionName regression) {

        if (!RegressionType.Probit.equals(regression.getType()) && !RegressionType.Logit.equals(regression.getType()))
            throw new RuntimeException("requested Binomial regression is not recognised: " + regression.name());

        switch (regression) {
            case EducationE1a -> {
                return Parameters.getRegEducationE1a();
            }
            case EducationE1b -> {
                return Parameters.getRegEducationE1b();
            }
            case FertilityF1a -> {
                return Parameters.getRegFertilityF1a();
            }
            case FertilityF1b -> {
                return Parameters.getRegFertilityF1b();
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
            case HealthH2b -> {
                return Parameters.getRegHealthH2b();
            }
            case HealthHM1Case -> {
                return Parameters.getRegHealthHM1Case();
            }
            case HealthHM2CaseMales -> {
                return Parameters.getRegHealthHM2CaseMales();
            }
            case HealthHM2CaseFemales -> {
                return Parameters.getRegHealthHM2CaseFemales();
            }
            case SocialCareS1a -> {
                return Parameters.getRegReceiveCareS1a();
            }
            case SocialCareS2a -> {
                return Parameters.getRegNeedCareS2a();
            }
            case SocialCareS2b -> {
                return Parameters.getRegReceiveCareS2b();
            }
            case SocialCareS2d -> {
                return Parameters.getRegReceiveCarePartnerS2d();
            }
            case SocialCareS3a -> {
                return Parameters.getRegCarePartnerProvCareToOtherS3a();
            }
            case SocialCareS3b -> {
                return Parameters.getRegNoCarePartnerProvCareToOtherS3b();
            }
            case SocialCareS3c -> {
                return Parameters.getRegNoPartnerProvCareToOtherS3c();
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
            default -> {
                throw new RuntimeException("unrecognised regression (1)");
            }
        }
    }

    public static OrderedRegression getOrderedRegression(RegressionName regression) {

        if (!RegressionType.OrderedLogit.equals(regression.getType()) && !RegressionType.OrderedProbit.equals(regression.getType()))
            throw new RuntimeException("requested ordered regression is not recognised: " + regression.name());

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
                throw new RuntimeException("unrecognised regression (1)");
            }
        }
    }

    public static GeneralisedOrderedRegression getGeneralisedOrderedRegression(RegressionName regression) {

        if (!RegressionType.GenOrderedLogit.equals(regression.getType()) && !RegressionType.GenOrderedProbit.equals(regression.getType()))
            throw new RuntimeException("requested generalised ordered regression is not recognised: " + regression.name());

        switch (regression) {
            default -> {
                throw new RuntimeException("unrecognised regression (1)");
            }
        }
    }

    public static MultinomialRegression getMultinomialRegression(RegressionName regression) {

        if (!RegressionType.MultinomialLogit.equals(regression.getType()))
            throw new RuntimeException("requested multinomial regression is not recognised: " + regression.name());

        switch (regression) {
            case SocialCareS2c -> {
                return Parameters.getRegSocialCareMarketS2c();
            }
            case SocialCareS2e -> {
                return Parameters.getRegPartnerSupplementaryCareS2e();
            }
            case SocialCareS2f -> {
                return Parameters.getRegNotPartnerInformalCareS2f();
            }
            case SocialCareS3d -> {
                return Parameters.getRegInformalCareToS3d();
            }
            default -> {
                throw new RuntimeException("unrecognised regression (1)");
            }
        }
    }

    public static boolean isDiscreteChoiceModel(RegressionName regression) {

        switch (regression.getType()) {

            case Logit, Probit, OrderedLogit, OrderedProbit, GenOrderedLogit, GenOrderedProbit, MultinomialLogit-> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public static IDiscreteChoiceModel getDiscreteVariableRegression(RegressionName regression) {

        switch (regression.getType()) {

            case Logit, Probit -> {
                return getBinomialRegression(regression);
            }
            case OrderedLogit, OrderedProbit -> {
                return getOrderedRegression(regression);
            }
            case GenOrderedLogit, GenOrderedProbit -> {
                return getGeneralisedOrderedRegression(regression);
            }
            case MultinomialLogit -> {
                return getMultinomialRegression(regression);
            }
            default ->
                throw new InvalidParameterException("Unrecognised call for discrete variable regression: " + regression.name());
        }
    }

    public static double getScore(IDoubleSource person, RegressionName regression) {

        if (RegressionType.Linear.equals(regression.getType()))
            return getLinearRegression(regression).getScore(person, Person.DoublesVariables.class);

        throw new RuntimeException("unrecognised regression in getScore");
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

    public static double getProbability(IDoubleSource obj, RegressionName regression) {

        if (!RegressionType.Logit.equals(regression.getType()) && !RegressionType.Probit.equals(regression.getType()))
            throw new InvalidParameterException("Failed to retrieve probability for unrecognised regression: " + regression.name());

        return getBinomialRegression(regression).getProbability(obj, Person.DoublesVariables.class);
    }

    public static <E extends Enum<E> & IntegerValuedEnum> double getProbability(E event, IDoubleSource obj, RegressionName regression) {

        return getDiscreteVariableRegression(regression).getProbability(event, obj, Person.DoublesVariables.class);
    }

    public static <E extends Enum<E> & IntegerValuedEnum> Map<E, Double> getProbabilities(IDoubleSource obj, RegressionName regression) {

        return getDiscreteVariableRegression(regression).getProbabilities(obj, Person.DoublesVariables.class);
    }

    public static <E extends Enum<E> & IntegerValuedEnum> E getEvent(Map<E, Double> probs, double rand) {

        List<E> eventList = (List<E>) probs.keySet();
        eventList.sort(Comparator.comparingInt(IntegerValuedEnum::getValue));

        double prob = 0.0;
        for (E event : eventList) {
            prob += probs.get(event);
            if (rand < prob) {
                return event;
            }
        }
        throw new RuntimeException("failed to select event from discrete set");
    }

    public static <E extends Enum<E> & IntegerValuedEnum> E getEvent(IDoubleSource obj, RegressionName regression, double rand) {

        Map<E, Double> probs = getProbabilities(obj, regression);
        return getEvent(probs, rand);
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
