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
    private long candidateID;
    private int matchCriterion;
    double targetNormalisedOriginalIncome;  // normalised income is per month in BASE_PRICE_YEAR year prices


    /**
     * CONSTRUCTORS
     */
    public Match(){}
    public Match(DonorKeys key, long candidate, int matchCriterion, double targetNormalisedOriginalIncome) {
        this.key = key;
        this.candidateID = candidate;
        this.matchCriterion = matchCriterion;
        this.targetNormalisedOriginalIncome = targetNormalisedOriginalIncome;
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

    /**
     * WORKER METHODS
     */
    public String getKey0String() {
        return String.valueOf(key.getKey(0));
    }
    public String getCandidateIDString() {
        return String.valueOf(candidateID);
    }
    public String getTargetNormalisedOriginalIncomeString() {
        return Double.toString(targetNormalisedOriginalIncome);
    }
}
