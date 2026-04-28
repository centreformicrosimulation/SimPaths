package simpaths.model.taxes;


import microsim.engine.SimulationEngine;
import org.apache.commons.lang3.tuple.Triple;
import simpaths.model.enums.TimeSeriesVariable;
import simpaths.model.enums.UpratingCase;

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

    // the match criterion is a 4-digit indicator of the type of match obtained
    // matchCriterion = RppNNn;
    //  R  = level of coarse-exact match obtained (0 to 2, from most fine-grained)
    //  pp = percentage point (absolute) difference of target with (normalised) income of nearest neighbour (max 99)
    //       if low income, then values is absolute difference in weekly income (not %)
    //       normalised income is monthly in current year prices
    //  NN = size of the candidate pool considered for imputation (max 99)
    //  n  = number of "accepted" candidates used to evaluate taxes and benefits (max 9)
    private int matchCriterion = 0;
    private long donorID = 0;
    private double disposableIncomePerWeek;
    private double benefitsReceivedPerWeek; // Sum of monetary and non-monetary benefits
    private double grossIncomePerWeek;
    private double targetNormalisedOriginalIncome;

    private Integer yBenUCFlag;
    private Integer receivedLegacyBenefit;


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
    public double getTargetNormalisedOriginalIncome() { return targetNormalisedOriginalIncome; }

    public Integer getReceivedUC() {
        return yBenUCFlag;
    }
    public void setReceivedUC(Integer yBenUCFlag) {
        this.yBenUCFlag = yBenUCFlag;
    }
    public Integer getReceivedLegacyBenefit() {
        return receivedLegacyBenefit;
    }
    public void setReceivedLegacyBenefit(Integer receivedLegacyBenefit) {
        this.receivedLegacyBenefit = receivedLegacyBenefit;
    }


    /**
     * METHOD TO PERFORM IMPUTATION AND GENERATE OUTPUTS
     * OUTPUTS ARE SAVED TO OBJECT ATTRIBUTES
     */
    public void evaluate() {


        //------------------------------------------------------------
        // use keys to extract candidate pool from database
        //------------------------------------------------------------
        List<Integer> candidatePool = null;
        int matchRegime = -1;
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
                    matchRegime = ii;
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
            matchCriterion = matchRegime * 100000 + Math.min(99, getPoolSize(candidatePool)) * 10;
        }


        //------------------------------------------------------------
        // find nearest neighbour accounting only for original income (first continuous feature)
        // value between iiTarget and iiTarget-1 (if iiTarget-1 exists)
        //------------------------------------------------------------
        // The candidate pool is organised in increasing order of original income
        // ordering is controlled by database query in SimPathsModel.populateTaxdbReferences
        // normalised income is monthly in BASE_PRICE_YEAR prices (same as EUROMOD)
        // Search here uses the golden search algorithm
        targetNormalisedOriginalIncome = Parameters.normaliseWeeklyIncome(keys.getPriceYear(), keys.getOriginalIncomePerWeek());
        int lowerInd, upperInd, testInd;
        double lowerOrigInc, upperOrigInc, testOrigInc;
        final double MEAN_BIAS = 0.5;

        lowerInd = 0;
        lowerOrigInc = Parameters.getDonorPool().get(candidatePool.get(lowerInd)).getPolicyBySystemYear(systemYear).getNormalisedOriginalIncomePerMonth();
        upperInd = candidatePool.size()-1;
        upperOrigInc = Parameters.getDonorPool().get(candidatePool.get(upperInd)).getPolicyBySystemYear(systemYear).getNormalisedOriginalIncomePerMonth();

        int iiTarget;
        if (targetNormalisedOriginalIncome<lowerOrigInc) {
            iiTarget = lowerInd;
        } else if (targetNormalisedOriginalIncome>upperOrigInc) {
            iiTarget = upperInd;
        } else {

            while (upperInd > lowerInd+1) {

                double adjFactor = 0.5 * MEAN_BIAS + (targetNormalisedOriginalIncome-lowerOrigInc) / (upperOrigInc - lowerOrigInc) * (1-MEAN_BIAS);
                int adjInd = (int) ((upperInd - lowerInd) * adjFactor);
                testInd = lowerInd + Math.max(1, adjInd);
                double candidateOrigIncMonthly = Parameters.getDonorPool().get(candidatePool.get(testInd)).getPolicyBySystemYear(systemYear).getOriginalIncomePerMonth();
                if (Parameters.taxDonorUpratingByWage) {
                    // adjust wage growth to simulated year
                    candidateOrigIncMonthly = candidateOrigIncMonthly * Parameters.getTimeSeriesValue(keys.getSimYear(), TimeSeriesVariable.WageGrowth) /
                            Parameters.getTimeSeriesValue(systemYear, TimeSeriesVariable.WageGrowth);
                }
                testOrigInc = Parameters.normaliseMonthlyIncome(systemYear, candidateOrigIncMonthly);
                if (testOrigInc > targetNormalisedOriginalIncome) {
                    upperInd = testInd;
                    upperOrigInc = testOrigInc;
                } else if (testOrigInc < targetNormalisedOriginalIncome) {
                    lowerInd = testInd;
                    lowerOrigInc = testOrigInc;
                } else {
                    // target val = test val - find next highest point in candidate pool
                    lowerInd = testInd;
                    lowerOrigInc = testOrigInc;
                }
            }
            if (Math.abs(upperOrigInc-targetNormalisedOriginalIncome) < Math.abs(lowerOrigInc-targetNormalisedOriginalIncome)) {
                iiTarget = upperInd;
            } else {
                iiTarget = lowerInd;
            }
        }
        // note that actual donor is identified by focussed search below
        DonorTaxUnit targetCandidate = Parameters.getDonorPool().get(candidatePool.get(iiTarget));
        donorID = targetCandidate.getId();
        double targetIncomeDifference = Math.min(Math.abs(targetNormalisedOriginalIncome - upperOrigInc), Math.abs(targetNormalisedOriginalIncome - lowerOrigInc));
        if (!keys.isLowIncome(matchRegime)) {
            targetIncomeDifference /= Math.abs(targetNormalisedOriginalIncome);
            targetIncomeDifference *= 100;
        }
        matchCriterion += Math.max(0, Math.min(99, (int)targetIncomeDifference)) * 1000;


        //------------------------------------------------------------
        // focussed search around target
        //------------------------------------------------------------
        List<CandidateList> candidatesList = new ArrayList<>();
        int bracketPts = (!flagChildcareCost && !flagSecondIncome) ? 2 : 50;
        double[] targetVector = getMeasurementVector(Parameters.BASE_PRICE_YEAR, keys.getPriceYear(), keys.getSimYear(), keys.getSimYear(),
                keys.getOriginalIncomePerWeek(), flagSecondIncome, keys.getSecondIncomePerWeek(), flagChildcareCost, keys.getChildcareCostPerWeek());
        int targetWagesYear = (Parameters.taxDonorUpratingByWage) ? keys.getSimYear() : systemYear;
        for (int increment=-1; increment<2; increment=increment+2) {
            // search backward and then forward through candidate list

            // initialise directional search
            int bracketInd = 0; // number of consecutive points within bracket
            double localMin = 999.0;
            double bracketDist = -999.0;
            int ii;
            if (increment<0) {
                ii = iiTarget - 1;
            } else {
                ii = iiTarget;
            }
            while (ii>=0 && ii<candidatePool.size()) {

                DonorTaxUnit candidate = Parameters.getDonorPool().get(candidatePool.get(ii));
                double[] candidateVector = getCandidateMeasVector(Parameters.BASE_PRICE_YEAR, systemYear, targetWagesYear, systemYear,
                        candidate, flagSecondIncome, flagChildcareCost);
                double distance = evaluateDistance(targetVector, candidateVector, flagSecondIncome, flagChildcareCost);
                if (Math.abs(distance - bracketDist) > 1.0E-4) {
                    bracketDist = distance;
                    bracketInd++;
                }
                if (distance < localMin) {
                    // new minimum found: reset bracket
                    bracketInd = 0;
                    localMin = distance;
                }
                if (bracketInd <= bracketPts) {
                    // add candidate to list
                    candidatesList.add(new CandidateList(candidate, candidate.getWeight(), distance));
                } else {
                    break;
                }
                ii = ii + increment;
            }
        }

        // select subset of preferred candidates
        candidatesList.sort(new CandidateListComparator());
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
        if (candidateLast > 0) {
            candidatesList = candidatesList.subList(0, candidateLast);
        }

        // If focussed search produced no candidates, fall back to nearest neighbour.
        if (candidatesList.isEmpty()) {
            DonorTaxUnit candidate = Parameters.getDonorPool().get(candidatePool.get(iiTarget));
            candidatesList.add(new CandidateList(candidate, candidate.getWeight(), 0.0));
            weightSum = candidate.getWeight();
        }
        if (weightSum <= 0.0) {
            // Degenerate weights: use equal probabilities over selected candidates.
            weightSum = (double) candidatesList.size();
        }


        //------------------------------------------------------------
        // impute disposable income from set of preferred candidates
        //------------------------------------------------------------
        grossIncomePerWeek = keys.getOriginalIncomePerWeek();
        disposableIncomePerWeek = -999.0;
        benefitsReceivedPerWeek = 0.0;
        matchCriterion += Math.min(9, candidatesList.size());
        double weightHere = 0.0;
        double infAdj = 1.0;
        double UCmean = 0.;  // Take a weighted mean of whether received UC or not across all candidates
        double LBmean = 0.;  // Take a weighted mean of whether received LB or not across all candidates
        setReceivedUC(0);
        setReceivedLegacyBenefit(0);
        if (systemYear != keys.getPriceYear())
            infAdj = Parameters.getTimeSeriesValue(keys.getPriceYear(), TimeSeriesVariable.Inflation) /
                    Parameters.getTimeSeriesValue(systemYear, TimeSeriesVariable.Inflation);
        if (systemYear != keys.getSimYear() && Parameters.taxDonorUpratingByWage)
            infAdj = infAdj * Parameters.getTimeSeriesValue(keys.getSimYear(), TimeSeriesVariable.WageGrowth) /
                    Parameters.getTimeSeriesValue(systemYear, TimeSeriesVariable.WageGrowth);
        for (CandidateList candidateList : candidatesList) {
            // loop over each preferred candidate

            double baseWeight = candidateList.getWeight();
            if (baseWeight <= 0.0) {
                baseWeight = 1.0;
            }
            double weight = baseWeight / weightSum;
            weightHere += weight;
            if (keys.getRandomDraw() <= weightHere) {

                if (Math.abs(disposableIncomePerWeek+999.0)<1.0E-5)
                    disposableIncomePerWeek = 0.0;
                if (keys.getRandomDraw()>0.0 || Math.abs(keys.getRandomDraw()+2.0)<1.0E-2)
                    weight = 1.0;
                DonorTaxUnit candidate = candidateList.getCandidate();
                if ( keys.isLowIncome(matchRegime) ) {
                    // impute based on observed disposable income
                    disposableIncomePerWeek += candidate.getPolicyBySystemYear(systemYear).getDisposableIncomePerMonth() / Parameters.WEEKS_PER_MONTH * weight * infAdj;
                    benefitsReceivedPerWeek += (candidate.getPolicyBySystemYear(systemYear).getBenMeansTestPerMonth() + candidate.getPolicyBySystemYear(systemYear).getBenNonMeansTestPerMonth()) / Parameters.WEEKS_PER_MONTH * weight * infAdj;
                    UCmean += candidate.getPolicyBySystemYear(systemYear).getReceivesUC() * weight * infAdj;
                    LBmean += candidate.getPolicyBySystemYear(systemYear).getReceivesLegacyBenefit() * weight * infAdj;
                } else {
                    // impute based on ratio of disposable to original income
                    disposableIncomePerWeek += candidate.getPolicyBySystemYear(systemYear).getDisposableIncomePerMonth() / candidate.getPolicyBySystemYear(systemYear).getOriginalIncomePerMonth() * weight;
                    benefitsReceivedPerWeek += (candidate.getPolicyBySystemYear(systemYear).getBenMeansTestPerMonth() + candidate.getPolicyBySystemYear(systemYear).getBenNonMeansTestPerMonth()) / candidate.getPolicyBySystemYear(systemYear).getOriginalIncomePerMonth() * weight;
                    UCmean += candidate.getPolicyBySystemYear(systemYear).getReceivesUC() * weight * infAdj;
                    LBmean += candidate.getPolicyBySystemYear(systemYear).getReceivesLegacyBenefit() * weight * infAdj;
                }
                if (keys.getRandomDraw()>0.0 || Math.abs(keys.getRandomDraw()+2.0)<1.0E-2) {
                    donorID = candidate.getId();
                    break;
                }
            }
        }
        if (Math.abs(disposableIncomePerWeek+999.0)<1.0E-5) {
            // Deterministic fallback: use nearest neighbour directly.
            donorID = targetCandidate.getId();
            if (keys.isLowIncome(matchRegime)) {
                disposableIncomePerWeek = targetCandidate.getPolicyBySystemYear(systemYear).getDisposableIncomePerMonth()
                        / Parameters.WEEKS_PER_MONTH * infAdj;
                benefitsReceivedPerWeek = (targetCandidate.getPolicyBySystemYear(systemYear).getBenMeansTestPerMonth()
                        + targetCandidate.getPolicyBySystemYear(systemYear).getBenNonMeansTestPerMonth())
                        / Parameters.WEEKS_PER_MONTH * infAdj;
            } else {
                double origIncMonth = targetCandidate.getPolicyBySystemYear(systemYear).getOriginalIncomePerMonth();
                if (Math.abs(origIncMonth) > 1.0E-9) {
                    disposableIncomePerWeek = targetCandidate.getPolicyBySystemYear(systemYear).getDisposableIncomePerMonth() / origIncMonth;
                    benefitsReceivedPerWeek = (targetCandidate.getPolicyBySystemYear(systemYear).getBenMeansTestPerMonth()
                            + targetCandidate.getPolicyBySystemYear(systemYear).getBenNonMeansTestPerMonth()) / origIncMonth;
                } else {
                    disposableIncomePerWeek = 0.0;
                    benefitsReceivedPerWeek = 0.0;
                }
            }
            setReceivedUC(targetCandidate.getPolicyBySystemYear(systemYear).getReceivesUC());
            setReceivedLegacyBenefit(targetCandidate.getPolicyBySystemYear(systemYear).getReceivesLegacyBenefit());
        }
        if (Math.abs(disposableIncomePerWeek+999.0)<1.0E-5)
            throw new RuntimeException("Failed to populate disposable income and benefits from donor with inner key value " + keys.getKey(0));
        if ( !keys.isLowIncome(matchRegime) ) {
            disposableIncomePerWeek *= keys.getOriginalIncomePerWeek();
            benefitsReceivedPerWeek *= keys.getOriginalIncomePerWeek();
        }
        if (keys.getHoursWorkedPerWeekMan() + keys.getHoursWorkedPerWeekWoman() > 0.1) {
            disposableIncomePerWeek *= (1.0 + Parameters.disposableIncomeFromLabourInnov);
            benefitsReceivedPerWeek *= (1.0 + Parameters.disposableIncomeFromLabourInnov);
        }
        if (UCmean > SimulationEngine.getRnd().nextDouble()) {  // Weighted probability of receiving UC
            setReceivedUC(1);
        }
        if (LBmean > 0 && getReceivedUC() == 0) {  // Setting as received LB if benefits but not UC
            setReceivedLegacyBenefit(1);
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

    /**
     * Method to package array of metrics used to compare target against potential donors
     *
     * @param targetPriceYear           inflation year that monetary variables should be measured in
     * @param currentPriceYear          inflation year that monetary variables currently measured in
     * @param targetWagesYear           wage growth year that monetary variables should be measured in
     * @param currentWagesYear          wage growth year that monetary variables currently measured in
     * @param originalIncomePerWeek     current original income (per week)
     * @param flagSecondIncome          if true, second income is also measured
     * @param secondIncomePerWeek       second income (per week, if measured)
     * @param flagChildcareCost         if true, childcare cost is also measured
     * @param childcareCostPerWeek      childcare cost (per week, if measured)
     * @return
     */
    private double[] getMeasurementVector(int targetPriceYear, int currentPriceYear,
                                          int targetWagesYear, int currentWagesYear,
                                          double originalIncomePerWeek, boolean flagSecondIncome,
                                          double secondIncomePerWeek, boolean flagChildcareCost, double childcareCostPerWeek) {

        double infAdj = 1.0, wageAdj = 1.0;
        if (currentPriceYear != targetPriceYear)
            infAdj = Parameters.getTimeSeriesValue(targetPriceYear, TimeSeriesVariable.Inflation) /
                    Parameters.getTimeSeriesValue(currentPriceYear, TimeSeriesVariable.Inflation);
        if (currentWagesYear != targetWagesYear)
            wageAdj = Parameters.getTimeSeriesValue(targetWagesYear, TimeSeriesVariable.WageGrowth) /
                    Parameters.getTimeSeriesValue(currentWagesYear, TimeSeriesVariable.WageGrowth);

        double oiAdj = originalIncomePerWeek * infAdj * wageAdj;
        double siAdj = secondIncomePerWeek * infAdj * wageAdj;
        double ccAdj = childcareCostPerWeek * infAdj * wageAdj;
        if (!flagSecondIncome && !flagChildcareCost) {
            return new double[] {oiAdj};
        } else if (flagSecondIncome && !flagChildcareCost) {
            return new double[] {oiAdj, siAdj};
        } else if (!flagSecondIncome) {
            return new double[]{oiAdj, ccAdj};
        } else {
            return new double[]{oiAdj, siAdj, ccAdj};
        }
    }
    private double[] getCandidateMeasVector(int targetPriceYear, int currentPriceYear,
                                            int targetWagesYear, int currentWagesYear,
                                            DonorTaxUnit candidate, boolean flagSecondIncome, boolean flagChildcareCost) {

        double oiWeekly = candidate.getPolicyBySystemYear(currentPriceYear).getOriginalIncomePerMonth() / Parameters.WEEKS_PER_MONTH;
        double siWeekly = 0.0;
        if (flagSecondIncome)
            siWeekly = candidate.getPolicyBySystemYear(currentPriceYear).getSecondIncomePerMonth() / Parameters.WEEKS_PER_MONTH;
        double ccWeekly = 0.0;
        if (flagChildcareCost)
            ccWeekly = candidate.getPolicyBySystemYear(currentPriceYear).getChildcareCostPerMonth() / Parameters.WEEKS_PER_MONTH;
        return getMeasurementVector(targetPriceYear, currentPriceYear, targetWagesYear, currentWagesYear,
                oiWeekly, flagSecondIncome, siWeekly, flagChildcareCost, ccWeekly);
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
