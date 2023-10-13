package simpaths.model.taxes;


import org.apache.commons.lang3.tuple.Triple;
import simpaths.model.enums.UpratingCase;
import org.apache.commons.math3.util.Pair;

import java.util.*;

import simpaths.data.Parameters;


/**
 *
 * CLASS TO MANAGE TAX IMPUTATIONS DRAWING ON DONOR DATA
 *
 * Imputations are made via a 3-step process:
 *  1: obtain donor pool from full population by coarse-exact matching
 *  2: obtain candidate pool from donor pool based on proximity to original income
 *  3: select single candidate from donor pool or average over pool to impute disposable income
 *
 */
public class DonorTaxImputation {


    /**
     * ATTRIBUTES
     */
    private DonorKeys keys;                // keys for donor matching

    // the match criterion is a 3-digit indicator of the type of match obtained
    // first digit indicates the level of coarse-exact match obtained (1 to 3, from most fine-grained)
    // second digit indicates the size of the candidate pool considered for imputation (max 9)
    // third digit indicates the number of "accepted" candidates used to evaluate taxes and benefits (max 9)
    private int matchCriterion = 0;
    private long donorID = 0;
    private double disposableIncomePerWeek;
    private double benefitsReceivedPerWeek; // Sum of monetary and non-monetary benefits
    private double grossIncomePerWeek;

    /**
     * CONSTRUCTORS
     */
    public DonorTaxImputation() {}
    public DonorTaxImputation(DonorKeys keys) {
        this.keys = keys;
    }


    /**
     * GETTERS AND SETTERS
     */
    public DonorKeys getKeys() {
        return keys;
    }
    public int getMatchCriterion() {
        return matchCriterion;
    }
    public double getDisposableIncomePerWeek() {
        return disposableIncomePerWeek;
    }
    public double getDisposableIncomePerMonth() {
        return disposableIncomePerWeek * Parameters.WEEKS_PER_MONTH;
    }
    public double getBenefitsReceivedPerWeek() { return benefitsReceivedPerWeek; }
    public double getBenefitsReceivedPerMonth() { return benefitsReceivedPerWeek * Parameters.WEEKS_PER_MONTH; }
    public double getGrossIncomePerWeek() {
        return grossIncomePerWeek;
    }
    public double getGrossIncomePerMonth() {
        return grossIncomePerWeek * Parameters.WEEKS_PER_MONTH;
    }
    public long getDonorID() { return donorID; }


    /**
     * METHOD TO PERFORM IMPUTATION AND GENERATE OUTPUTS
     * OUTPUTS ARE SAVED TO OBJECT ATTRIBUTES
     */
    public void evaluate() {


        //------------------------------------------------------------
        // extract candidate pool from database
        //------------------------------------------------------------
        List<Integer> candidatePool = null;
        int systemYear = getSystemYear(keys.getSimYear());
        boolean flagSecondIncome = false, flagChildcareCost = false;
        for (int ii=0; ii<Parameters.TAXDB_REGIMES; ii++) {

            Triple<Integer,Integer,Integer> key = Triple.of(systemYear,ii,keys.getKey(ii));
            candidatePool = Parameters.getTaxdbReferences().get(key);
            int jjStart;
            if ( (getCounterVal(MatchFeature.DualIncome, ii, keys.getKey(ii))==1) ||
                    (getCounterVal(MatchFeature.Childcare, ii, keys.getKey(ii))==1) ) {
                jjStart = 0;
            } else {
                jjStart = 1;
            }
            for (int jj=jjStart; jj<2; jj++) {
                int minCandidatePool;
                if (jj==0) {
                    minCandidatePool = 50;
                } else {
                    minCandidatePool = 10;
                }
                if (getPoolSize(candidatePool)>=minCandidatePool) {
                    matchCriterion = ii * 1000;
                    if (jj==0 && getCounterVal(MatchFeature.DualIncome, ii, keys.getKey(ii))==1)
                        flagSecondIncome = true;
                    if (jj==0 && getCounterVal(MatchFeature.Childcare, ii, keys.getKey(ii))==1)
                        flagChildcareCost = true;
                    jj=2;
                    ii=Parameters.TAXDB_REGIMES;
                }
            }
        }
        if (getPoolSize(candidatePool) == 0) {
            throw new RuntimeException("no donor benefitUnit found for state combination with inner key index " + keys.getKey(0));
        } else {
            matchCriterion += Math.min(99, getPoolSize(candidatePool)) * 10;
        }


        //------------------------------------------------------------
        // find nearest neighbour accounting only for original income (first continuous feature)
        // value between iiTarget and iiTarget-1 (if iiTarget-1 exists)
        //------------------------------------------------------------
        // The candidate pool is organised in increasing order of original income
        // ordering is controlled by database query in SimPathsModel.populateTaxdbReferences
        double targetOrigInc = Parameters.normaliseWeeklyIncome(keys.getPriceYear(), keys.getOriginalIncomePerWeek());
        int lowerInd, upperInd, testInd;
        double lowerOrigInc, upperOrigInc, testOrigInc;
        final double MEAN_BIAS = 0.5;

        lowerInd = 0;
        lowerOrigInc = Parameters.getDonorPool().get(candidatePool.get(lowerInd)).getPolicyBySystemYear(systemYear).getNormalisedOriginalIncome();
        upperInd = candidatePool.size()-1;
        upperOrigInc = Parameters.getDonorPool().get(candidatePool.get(upperInd)).getPolicyBySystemYear(systemYear).getNormalisedOriginalIncome();

        int iiTarget;
        if (targetOrigInc<lowerOrigInc) {
            iiTarget = lowerInd;
        } else if (targetOrigInc>upperOrigInc) {
            iiTarget = upperInd;
        } else {

            while (upperInd > lowerInd+1) {

                double adjFactor = 0.5 * MEAN_BIAS + (targetOrigInc-lowerOrigInc) / (upperOrigInc - lowerOrigInc) * (1-MEAN_BIAS);
                int adjInd = (int) ((upperInd - lowerInd) * adjFactor);
                testInd = lowerInd + Math.max(1, adjInd);
                testOrigInc = Parameters.getDonorPool().get(candidatePool.get(testInd)).getPolicyBySystemYear(systemYear).getNormalisedOriginalIncome();

                if (testOrigInc > targetOrigInc) {
                    upperInd = testInd;
                    upperOrigInc = testOrigInc;
                } else if (testOrigInc < targetOrigInc) {
                    lowerInd = testInd;
                    lowerOrigInc = testOrigInc;
                } else {
                    // target val = test val - find next highest point in candidate pool
                    lowerInd = testInd;
                    lowerOrigInc = testOrigInc;
                }
            }
            iiTarget = upperInd;
        }


        //------------------------------------------------------------
        // focussed search around target
        //------------------------------------------------------------
        List<CandidateList> candidatesList = new ArrayList<>();
        int bracketPts = (!flagChildcareCost && !flagSecondIncome) ? 2 : 60;
        double oi = keys.getOriginalIncomePerWeek();
        double si = keys.getSecondIncomePerWeek();
        double cc = keys.getChildcareCostPerWeek();
        double[] targetVector = getMeasurementVector(keys.getPriceYear(), oi, flagSecondIncome, si, flagChildcareCost, cc);
        for (int increment=-1; increment<2; increment=increment+2) {
            // search backward and then forward through candidate list

            // initialise directional search
            int bracketInd = 0;
            double localMin = 999.0;
            double bracketDist = -999.0;
            int ii;
            if (increment<0) {
                ii = iiTarget - 1;
            } else {
                ii = iiTarget;
            }
            while (ii>=0 && ii<candidatePool.size()-1) {

                DonorTaxUnit candidate = Parameters.getDonorPool().get(candidatePool.get(ii));
                double[] candidateVector = getCandidateMeasVector(candidate, flagSecondIncome, flagChildcareCost);
                double distance = evaluateDistance(targetVector, candidateVector, flagSecondIncome, flagChildcareCost);
                if (Math.abs(distance - bracketDist) > 1.0E-4) {
                    bracketDist = distance;
                    bracketInd++;
                }
                if (distance < localMin) {
                    bracketInd = 0;
                    localMin = distance;
                }
                if (bracketInd <= bracketPts) {
                    candidatesList.add(new CandidateList(candidate, candidate.getWeight(), distance));
                } else {
                    break;
                }
                ii = ii + increment;
            }
        }

        // select subset of preferred candidates
        Collections.sort(candidatesList, new CandidateListComparator());
        int candidateLast = 0;
        int bracketInd = 0;
        double bracketDist = -999.0;
        double weightSum = 0.0;
        for (CandidateList candidateList : candidatesList) {
            candidateLast++;
            if (Math.abs(candidateList.getDistance() - bracketDist) > 1.0E-4) {
                bracketDist = candidateList.getDistance();
                bracketInd++;
            }
            if (bracketInd <= 4) {
                weightSum += candidateList.getWeight();
            } else {
                break;
            }
        }
        candidatesList = candidatesList.subList(0, candidateLast);


        //------------------------------------------------------------
        // impute disposable income from set of preferred candidates
        //------------------------------------------------------------
        grossIncomePerWeek = keys.getOriginalIncomePerWeek();
        disposableIncomePerWeek = -999.0;
        benefitsReceivedPerWeek = 0.0;
        matchCriterion += Math.min(9, candidatesList.size());
        double weightHere = 0.0;
        double infAdj = 1.0;
        if (systemYear != keys.getPriceYear())
            infAdj = Parameters.getTimeSeriesIndex(keys.getPriceYear(), UpratingCase.TaxDonor) / Parameters.getTimeSeriesIndex(systemYear, UpratingCase.TaxDonor);
        for (CandidateList candidateList : candidatesList) {
            // loop over each preferred candidate

            double weight = candidateList.getWeight() / weightSum;
            weightHere += weight;
            if (keys.getRandomDraw() <= weightHere) {

                if (Math.abs(disposableIncomePerWeek+999.0)<1.0E-5)
                    disposableIncomePerWeek = 0.0;
                if (keys.getRandomDraw()>0.0 || Math.abs(keys.getRandomDraw()+2.0)<1.0E-2)
                    weight = 1.0;
                DonorTaxUnit candidate = candidateList.getCandidate();
                if ( keys.isLowIncome() ) {
                    // impute based on observed disposable income
                    disposableIncomePerWeek += candidate.getPolicyBySystemYear(systemYear).getDisposableIncomePerMonth() / Parameters.WEEKS_PER_MONTH * weight * infAdj;
                    benefitsReceivedPerWeek += (candidate.getPolicyBySystemYear(systemYear).getBenMeansTestPerMonth() + candidate.getPolicyBySystemYear(systemYear).getBenNonMeansTestPerMonth()) / Parameters.WEEKS_PER_MONTH * weight * infAdj;
                } else {
                    // impute based on ratio of disposable to original income
                    disposableIncomePerWeek += candidate.getPolicyBySystemYear(systemYear).getDisposableIncomePerMonth() / candidate.getPolicyBySystemYear(systemYear).getOriginalIncomePerMonth() * weight;
                    benefitsReceivedPerWeek += (candidate.getPolicyBySystemYear(systemYear).getBenMeansTestPerMonth() + candidate.getPolicyBySystemYear(systemYear).getBenNonMeansTestPerMonth()) / candidate.getPolicyBySystemYear(systemYear).getOriginalIncomePerMonth() * weight;
                }
                if (keys.getRandomDraw()>0.0 || Math.abs(keys.getRandomDraw()+2.0)<1.0E-2) {
                    donorID = candidate.getId();
                    break;
                }
            }
        }
        if (Math.abs(disposableIncomePerWeek+999.0)<1.0E-5)
            throw new RuntimeException("Failed to populate disposable income and benefits from donor with inner key value " + keys.getKey(0));
        if ( !keys.isLowIncome() ) {
            disposableIncomePerWeek *= keys.getOriginalIncomePerWeek();
            benefitsReceivedPerWeek *= keys.getOriginalIncomePerWeek();
        }
    }

    private int getSystemYear(int simYear) {

        Integer fromYear = null;
        int fromYear0, fromYear1 = 0;
        int ii = 0;
        Iterator<Integer> iter = Parameters.EUROMODpolicyScheduleSystemYearMap.keySet().iterator();
        // this code works because the rows of Parameters.EUROMODpolicyScheduleSystemYearMap are in ascending order by fromYear
        while (ii < Parameters.EUROMODpolicyScheduleSystemYearMap.size()) {

            fromYear0 = fromYear1;
            fromYear1 = iter.next();
            if ( simYear < fromYear1 ) {
                if (ii==0) {
                    fromYear = fromYear1;
                } else {
                    fromYear = fromYear0;
                }
                break;
            }
            ii++;
        }
        if (fromYear == null)
            fromYear = fromYear1;

        return Parameters.EUROMODpolicyScheduleSystemYearMap.get(fromYear).getValue();
    }

    private Integer getPoolSize(List<Integer> pool) {

        if ( pool == null ) {
            return 0;
        } else {
            return pool.size();
        }
    }

    public static int getCounterVal(MatchFeature targetFeature, int regime, int index) {
        Map<MatchFeature, Map<Integer, Integer>> taxdbCounter = Parameters.getTaxdbCounter();
        int indexPruned = index;
        if (taxdbCounter==null)
            throw new RuntimeException("attempt to evaluate index value before tax database references have been instantiated");
        for (int ii=MatchFeature.values().length-1; ii>-1; ii--) {
            MatchFeature feature = MatchFeature.values()[ii];
            if (feature.equals(targetFeature))
                return indexPruned / taxdbCounter.get(feature).get(regime);
            indexPruned = indexPruned % taxdbCounter.get(feature).get(regime);
        }
        throw new RuntimeException("attempt to evaluate index of unrecognised target feature");
    }
    private double[] getMeasurementVector(int priceYear, double originalIncomePerWeek, boolean flagSecondIncome, double secondIncomePerWeek,
                                          boolean flagChildcareCost, double childcareCostPerWeek) {

        double oiAdj = Parameters.normaliseWeeklyIncome(priceYear, originalIncomePerWeek);
        double siAdj = Parameters.normaliseWeeklyIncome(priceYear, secondIncomePerWeek);
        double ccAdj = Parameters.normaliseWeeklyIncome(priceYear, childcareCostPerWeek);
        if (!flagSecondIncome && !flagChildcareCost) {
            return new double[] {oiAdj};
        } else if (flagSecondIncome && !flagChildcareCost) {
            return new double[] {oiAdj, siAdj};
        } else if (!flagSecondIncome && flagChildcareCost) {
            return new double[]{oiAdj, ccAdj};
        } else {
            return new double[]{oiAdj, siAdj, ccAdj};
        }
    }
    private double[] getCandidateMeasVector(DonorTaxUnit candidate, boolean flagSecondIncome, boolean flagChildcareCost) {

        int priceYear = Parameters.BASE_PRICE_YEAR;
        double oiWeekly = candidate.getPolicyBySystemYear(priceYear).getOriginalIncomePerMonth() / Parameters.WEEKS_PER_MONTH;
        double siWeekly = 0.0;
        if (flagSecondIncome)
            siWeekly = candidate.getPolicyBySystemYear(priceYear).getSecondIncomePerMonth() / Parameters.WEEKS_PER_MONTH;
        double ccWeekly = 0.0;
        if (flagChildcareCost)
            ccWeekly = candidate.getPolicyBySystemYear(priceYear).getChildcareCostPerMonth() / Parameters.WEEKS_PER_MONTH;
        return getMeasurementVector(priceYear, oiWeekly, flagSecondIncome, siWeekly, flagChildcareCost, ccWeekly);
    }
    private double evaluateDistance(double[] targetVector, double[] candidateVector, boolean flagSecondIncome, boolean flagChildcareCost) {
        if (flagSecondIncome && flagChildcareCost) {
            return Parameters.getMdDualIncomeChildcare().getMahalanobisDistance(targetVector, candidateVector);
        } else if (flagSecondIncome) {
            return Parameters.getMdDualIncome().getMahalanobisDistance(targetVector, candidateVector);
        } else if (flagChildcareCost) {
            return Parameters.getMdChildcare().getMahalanobisDistance(targetVector, candidateVector);
        } else {
            if (targetVector.length>1)
                throw new RuntimeException("unrecognised multi-dimensional target vector");
            return Math.abs(targetVector[0] - candidateVector[0]);
        }
    }
}
