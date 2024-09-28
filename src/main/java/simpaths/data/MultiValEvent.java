package simpaths.data;

import simpaths.model.enums.IntegerValuedEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MultiValEvent class assists evaluation of multinomial events
 */
public class MultiValEvent <E extends IntegerValuedEnum> {

    private E selectedEvent;
    private Map<E, Double> probs;
    private double randomDrawInitial, randomDrawAdjusted;


    // CONSTRUCTOR
    public MultiValEvent(Map<E, Double> probs, double randomDrawInitial) {
        this.probs = probs;
        this.randomDrawInitial = randomDrawInitial;
    }


    // GETTERS / SETTERS
    public E getSelectedEvent() {
        return selectedEvent;
    }
    public void setSelectedEvent(E selectedEvent) {
        this.selectedEvent = selectedEvent;
    }
    public double getRandomDrawAdjusted() {
        return randomDrawAdjusted;
    }
    public void setRandomDrawAdjusted(double randomDrawAdjusted) {
        this.randomDrawAdjusted = randomDrawAdjusted;
    }


    // WORKER METHODS

    // METHOD TO SELECT EVENT AND ADJUST INPUT PROBABILITY
    public E eval() {

        // evaluate cumulative probability and list keys in ascending order of associated probability
        double cprob = 0.0;
        List<E> keys = new ArrayList<>();
        for (E ee : probs.keySet()) {

            if (probs.get(ee) != null) {

                cprob += probs.get(ee);
                for (int ii=0; ii<keys.size(); ii++) {

                    E ee1 = keys.get(ii);
                    if (ee1.getValue() > ee.getValue()) {

                        keys.add(ii, ee);
                        break;
                    }
                }
                if (!keys.contains(ee))
                    keys.add(ee);
            } else {
                throw new RuntimeException("problem identifying probabilities for multinomial object (2)");
            }
        }

        // identify selection and adjusted value of random draw
        double prob0 = 0.0, prob1 = 0.0;
        for (E ee : keys) {
            prob1 += probs.get(ee) / cprob;
            if (randomDrawInitial < prob1) {

                selectedEvent = ee;
                randomDrawAdjusted = (randomDrawInitial - prob0) / (prob1 - prob0);
                return ee;
            }
            prob0 = prob1;
        }
        throw new RuntimeException("failed to identify new enumerator for multi-event (2)");
    }
}
