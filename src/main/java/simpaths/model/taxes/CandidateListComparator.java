package simpaths.model.taxes;

public class CandidateListComparator implements java.util.Comparator<CandidateList>{
    @Override
    public int compare(CandidateList aa, CandidateList bb) {
        if (aa.getDistance() < bb.getDistance()) {
            return -1;
        } else if (aa.getDistance() == bb.getDistance()) {
            return 0;
        } else {
            return 1;
        }
    }
}
