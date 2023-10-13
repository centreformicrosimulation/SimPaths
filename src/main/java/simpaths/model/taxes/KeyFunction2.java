package simpaths.model.taxes;

import simpaths.data.Parameters;
import simpaths.model.decisions.DecisionParams;
import simpaths.model.enums.UpratingCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 * CLASS TO MANAGE ONE SPECIFICATION FOR EVALUATING DONOR KEYS USED TO IMPUTE TAX AND BENEFIT PAYMENTS
 *
 */
public class KeyFunction2 {


    /**
     * ATTRIBUTES
     */
    private final int MID_AGE = 45;
    private final int INCOME_REF_YEAR = 2017;
    private final double LO_INCOME = 225.0;
    private final double HI_INCOME = 710.0;


    /**
     * CONSTRUCTORS
     */
    public KeyFunction2() {}

    /**
     * METHOD TO EVALUATE DONOR KEYS FOR COARSE EXACT MATCHING
     * @param simYear simulated year
     * @param priceYear year of prices used to measure financial statistics
     * @param age simulated age
     * @param numberMembersOver17 family members aged 18+
     * @param numberChildrenUnder5 family members under age 5
     * @param numberChildren5To17 family members aged 5 to 17
     * @param hoursWorkedPerWeekMan employment hours per week of adult male
     * @param hoursWorkedPerWeekWoman employment hours per week of adult female
     * @param dlltsdMan disability status of man
     * @param dlltsdWoman disability status of woman
     * @param originalIncomePerWeek original income per week of family (possibly negative)
     * @return Integer list of keys, ordered from most fine (0) to most coarse (2)
     */
    public List<Integer> evaluateKeys(int simYear, int priceYear, int age, int numberMembersOver17, int numberChildrenUnder5, int numberChildren5To17,
                                      double hoursWorkedPerWeekMan, double hoursWorkedPerWeekWoman, int dlltsdMan, int dlltsdWoman, double originalIncomePerWeek) {

        // initialise working variables
        int spa = getStatePensionAge(age, simYear);
        Map<MatchFeature, Map<Integer, Integer>> taxdbCounter = getTaxdbCounter();
        Map<MatchFeature, Map<Integer, Integer>> units = new HashMap<>();
        List<Integer> result = new ArrayList<>();
        Map<Integer, Integer> localMap;

        // discretise hours worked variables
        int partTimeEmployed = 0, fullTimeEmployed = 0;
        if (hoursWorkedPerWeekMan >= DecisionParams.PARTTIME_HOURS_WEEKLY) {
            fullTimeEmployed += 1;
        } else if (hoursWorkedPerWeekMan > DecisionParams.MIN_WORK_HOURS_WEEKLY) {
            partTimeEmployed += 1;
        }
        if (hoursWorkedPerWeekWoman >= DecisionParams.PARTTIME_HOURS_WEEKLY) {
            fullTimeEmployed += 1;
        } else if (hoursWorkedPerWeekWoman > DecisionParams.MIN_WORK_HOURS_WEEKLY) {
            partTimeEmployed += 1;
        }

        //------------------------------------------------------
        // evaluate characteristic-specific steps
        //------------------------------------------------------

        // age
        localMap = new HashMap<>();
        if (age >= spa) {
            localMap.put(0,2);
            localMap.put(1,1);
            localMap.put(2,1);
        } else if (age >= MID_AGE) {
            localMap.put(0,1);
            localMap.put(1,0);
            localMap.put(2,0);
        } else {
            localMap.put(0,0);
            localMap.put(1,0);
            localMap.put(2,0);
        }
        units.put(MatchFeature.Age, localMap);

        // adults
        localMap = new HashMap<>();
        if (numberMembersOver17 > 1) {
            localMap.put(0,1);
            localMap.put(1,1);
            localMap.put(2,1);
        } else {
            localMap.put(0,0);
            localMap.put(1,0);
            localMap.put(2,0);
        }
        units.put(MatchFeature.Adults, localMap);

        // children
        localMap = new HashMap<>();
        if ( age < spa ) {

            localMap.put(0, Math.min(numberChildrenUnder5,2) + 3 * Math.min(numberChildren5To17,3));
            localMap.put(1, Math.min(numberChildrenUnder5,1) + 2 * Math.min(numberChildren5To17,3));
            localMap.put(2, Math.min(numberChildrenUnder5 + numberChildren5To17,3));
        } else {

            localMap.put(0, 0);
            localMap.put(1, 0);
            localMap.put(2, 0);
        }
        units.put(MatchFeature.Children, localMap);

        // employment
        localMap = new HashMap<>();
        if (partTimeEmployed + fullTimeEmployed == 0) {
            // no employment
            localMap.put(0,0);
            localMap.put(1,0);
            localMap.put(2,0);
        } else if ( fullTimeEmployed == 0 ){
            // only part-time employed
            localMap.put(0,1);
            localMap.put(1,1);
            localMap.put(2,0);
        } else if ( partTimeEmployed + fullTimeEmployed == 1 ){
            // one full-time employed
            localMap.put(0,2);
            localMap.put(1,1);
            localMap.put(2,0);
        } else if ( partTimeEmployed == 1 & fullTimeEmployed == 1 ){
            // one full-time and one part-time employed
            localMap.put(0,3);
            localMap.put(1,2);
            localMap.put(2,0);
        } else {
            // two full-time employed
            localMap.put(0,4);
            localMap.put(1,2);
            localMap.put(2,0);
        }
        units.put(MatchFeature.Employment, localMap);

        // long-term sick and disabled
        localMap = new HashMap<>();
        if (dlltsdMan > 0 || dlltsdWoman > 0) {
            // one adult disabled
            localMap.put(0,1);
            localMap.put(1,1);
            localMap.put(2,0);
        } else {
            // no disabled
            localMap.put(0,0);
            localMap.put(1,0);
            localMap.put(2,0);
        }
        units.put(MatchFeature.Disability, localMap);

        // original income
        localMap = new HashMap<>();
        double originalIncomePerWeekAdjusted = originalIncomePerWeek * Parameters.getTimeSeriesIndex(INCOME_REF_YEAR, UpratingCase.TaxDonor) /
                Parameters.getTimeSeriesIndex(priceYear, UpratingCase.TaxDonor);
        if (originalIncomePerWeekAdjusted < LO_INCOME) {
            // low income
            localMap.put(0,0);
            localMap.put(1,0);
            localMap.put(2,0);
        } else if ( originalIncomePerWeekAdjusted < HI_INCOME ) {
            // mid income
            localMap.put(0,1);
            localMap.put(1,1);
            localMap.put(2,1);
        } else {
            // high income
            localMap.put(0,2);
            localMap.put(1,1);
            localMap.put(2,1);
        }
        units.put(MatchFeature.Income, localMap);

        //------------------------------------------------------
        // compile results
        //------------------------------------------------------
        for (int ii=0; ii<Parameters.TAXDB_REGIMES; ii++) {
            int index=0;
            for (MatchFeature feature : MatchFeature.values()) {
                if (units.containsKey(feature))
                    index += units.get(feature).get(ii) * taxdbCounter.get(feature).get(ii);
            }
            result.add(index);
        }

        // return
        return result;
    }

    /**
     * METHOD TO INDICATE IF TAX UNIT IS MEMBER OF 'LOW INCOME' CATEGORY FOR DATABASE MATCHING
     * @param priceYear year of prices used to measure income
     * @param originalIncomePerWeek original income per week of family
     * @return boolean equal to true if family treated as low income
     */
    public boolean isLowIncome(int priceYear, double originalIncomePerWeek) {

        boolean lowIncome = false;
        double originalIncomePerWeekAdjusted = originalIncomePerWeek * Parameters.getTimeSeriesIndex(INCOME_REF_YEAR, UpratingCase.TaxDonor) /
                Parameters.getTimeSeriesIndex(priceYear, UpratingCase.TaxDonor);
        if (originalIncomePerWeekAdjusted < LO_INCOME) {
            lowIncome = true;
        }
        return lowIncome;
    }

    /**
     * WORKER METHOD TO PROVIDE STATE PENSION AGE AND YEAR
     * @param age age of eldest family member
     * @param simYear simulated year
     * @return state pension age
     */
    public int getStatePensionAge(int age, int simYear) {

        int spa;
        if (simYear - age + 65 < 2019) {
            spa = 65;
        } else if (simYear - age + 66 < 2027) {
            spa = 66;
        } else if (simYear - age + 67 < 2045) {
            spa = 67;
        } else {
            spa = 68;
        }
        return spa;
    }

    /**
     * WORKER METHOD TO CALL OR INITIALISE THE COUNTER MAPPING FOR DONOR KEYS
     * @return taxdbCounter populated as implied by current matching function
     */
    public Map<MatchFeature, Map<Integer, Integer>> getTaxdbCounter() {

        Map<MatchFeature, Map<Integer, Integer>> taxdbCounter = Parameters.getTaxdbCounter();
        if (taxdbCounter.isEmpty()) {

            // initialise working variables
            Map<Integer,Integer> mapLocal;
            int[] ptsLocal;

            // initialise starting values
            ptsLocal = new int[]{1,1,1};
            mapLocal = new HashMap<>();
            for ( int ii=0; ii<3; ii++) {
                mapLocal.put(ii,1);
            }

            // age
            mapLocal = updateMap(mapLocal, ptsLocal);
            taxdbCounter.put(MatchFeature.Age,mapLocal);
            ptsLocal = new int[]{3,2,2}; // this defines the number of age alternatives considered for each donor key set

            // number of adults
            mapLocal = updateMap(mapLocal, ptsLocal);
            taxdbCounter.put(MatchFeature.Adults,mapLocal);
            ptsLocal = new int[]{2,2,2};

            // number of children
            mapLocal = updateMap(mapLocal, ptsLocal);
            taxdbCounter.put(MatchFeature.Children,mapLocal);
            ptsLocal = new int[]{3*4,2*4,4};

            // employment status
            mapLocal = updateMap(mapLocal, ptsLocal);
            taxdbCounter.put(MatchFeature.Employment,mapLocal);
            ptsLocal = new int[]{5,3,1};

            // disability status
            mapLocal = updateMap(mapLocal, ptsLocal);
            taxdbCounter.put(MatchFeature.Disability,mapLocal);
            ptsLocal = new int[]{2,2,1};

            // original income
            mapLocal = updateMap(mapLocal, ptsLocal);
            taxdbCounter.put(MatchFeature.Income,mapLocal);
            ptsLocal = new int[]{3,2,2};

            // total size
            mapLocal = updateMap(mapLocal, ptsLocal);
            taxdbCounter.put(MatchFeature.Final,mapLocal);

            Parameters.setTaxdbCounter(taxdbCounter);
        }

        return taxdbCounter;
    }

    /**
     * WORKER METHOD TO INCREMENT THE STEP EVALUATION FOR taxdbCounter
     * @param mapPrev the immediately preceding step
     * @param ptsPrev the immediately preceding points to increment step to current state
     * @return the updated step evaluation
     */
    private Map<Integer, Integer> updateMap(Map<Integer,Integer> mapPrev, int[] ptsPrev) {

        Map<Integer, Integer> mapHere = new HashMap<>();
        for ( int ii=0; ii<3; ii++) {
            int val = mapPrev.get(ii) * ptsPrev[ii];
            mapHere.put(ii,val);
        }
        return mapHere;
    }
}
