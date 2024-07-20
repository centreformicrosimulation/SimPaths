package simpaths.model;

import microsim.matching.IterativeRandomMatching;
import microsim.matching.MatchingClosure;
import microsim.matching.MatchingScoreClosure;
import org.apache.commons.math3.util.Pair;
import simpaths.data.Parameters;
import simpaths.model.enums.Dcpst;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Household_status;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * CLASS TO MANAGE MATCHES BETWEEN PROSPECTIVE COHABITATION CANDIDATES
 */
public class UnionMatching {

    Set<Person> matches;
    Pair<Set<Person>, Set<Person>> unmatched;
    int ageDiffBound = Parameters.AGE_DIFFERENCE_INITIAL_BOUND;
    double potentialHourlyEarningsDiffBound = Parameters.POTENTIAL_EARNINGS_DIFFERENCE_INITIAL_BOUND;


    // CONSTRUCTOR
    public UnionMatching(Pair<Set<Person>, Set<Person>> unmatched) {
        this.unmatched = unmatched;
    }


    // HELPER METHODS

    // EVALUATE MATCHES
    public void evaluate(boolean alignmentRun) {

    }
}
