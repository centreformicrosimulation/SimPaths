package simpaths.model.taxes;


/**
 *
 * CLASS TO STORE INDIVIDUAL DATABASE MATCHES
 *
 */
public class Match {

    /**
     * ATTRIBUTES
     */
    private DonorKeys key;
    private int key0;
    private long candidateID;
    private int matchCriterion;
    double targetNormalisedOriginalIncome;  // normalised income is per month in BASE_PRICE_YEAR year prices
    private int gridAge;
    private int simYear;


    /**
     * CONSTRUCTORS
     */
    public Match(){}
    public Match(long candidate, int matchCriterion, double targetNormalisedOriginalIncome) {
        this.candidateID = candidate;
        this.matchCriterion = matchCriterion;
        this.targetNormalisedOriginalIncome = targetNormalisedOriginalIncome;
    }
    public Match(boolean flagGrid, int id, int key0, long candidate, int matchCriterion, double targetNormalisedOriginalIncome) {
        this(candidate, matchCriterion, targetNormalisedOriginalIncome);
        this.key0 = key0;
        if (flagGrid)
            gridAge = id;
        else
            simYear = id;
    }
    public Match(DonorKeys key, long candidate, int matchCriterion, double targetNormalisedOriginalIncome) {
        this(candidate, matchCriterion, targetNormalisedOriginalIncome);
        this.key = key;
        this.key0 = key.getKey(0);
    }


    /**
     * GETTERS AND SETTERS
     */
    public DonorKeys getKey() {
        return key;
    }

    public void setKey(DonorKeys key) {
        this.key = key;
    }

    public int getKey0() {
        return key0;
    }

    public void setKey0(int key0) {
        this.key0 = key0;
    }

    public long getCandidateID() {
        return candidateID;
    }

    public void setCandidateID(long candidateID) {
        this.candidateID = candidateID;
    }

    public int getMatchCriterion() {
        return matchCriterion;
    }

    public void setMatchCriterion(int matchCriterion) {
        this.matchCriterion = matchCriterion;
    }

    public double getTargetNormalisedOriginalIncome() {
        return targetNormalisedOriginalIncome;
    }

    public void setTargetNormalisedOriginalIncome(double targetNormalisedOriginalIncome) {
        this.targetNormalisedOriginalIncome = targetNormalisedOriginalIncome;
    }

    public int getGridAge() {
        return gridAge;
    }

    public int getSimYear() {
        return simYear;
    }


    /**
     * WORKER METHODS
     */
    public String getKey0String() {
        return String.valueOf(key0);
    }
    public String getMatchCriterionString() {
        return String.valueOf(matchCriterion);
    }
    public String getCandidateIDString() {
        return String.valueOf(candidateID);
    }
    public String getTargetNormalisedOriginalIncomeString() {
        return Double.toString(targetNormalisedOriginalIncome);
    }
}
