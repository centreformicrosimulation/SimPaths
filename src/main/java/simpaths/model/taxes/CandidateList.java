package simpaths.model.taxes;

public class CandidateList {
    private DonorTaxUnit candidate;
    private double sampleWeight;
    private double distance;

    public CandidateList(){}
    public CandidateList(DonorTaxUnit ind, double weight, double dist) {
        candidate = ind;
        sampleWeight = weight;
        distance = dist;
    }
    public DonorTaxUnit getCandidate() {
        return candidate;
    }
    public double getWeight() {
        return sampleWeight/(0.1 + distance);
    }
    public double getDistance() {
        return distance;
    }
}