package simpaths.model;

import microsim.engine.SimulationEngine;
import microsim.event.EventListener;

import jakarta.persistence.Transient;

/**
 * Tests class is run at the end of each year and subjects the population to a number of tests designed to ensure data validity
 *
 */

public class Tests implements EventListener {

    @Transient
    private final SimPathsModel model;

    /*
    Constructor
     */

    public Tests() {
        model =  (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
    }

    public void runTests() {
//        System.out.println(personBUTest1());
//        System.out.println(idCheckTest2());
    }

    /*
    Test 1: make sure that no person is missing benefit unit
     */
    private boolean personBUTest1() {
        boolean passed = true;

        for (Person person : model.getPersons()) {
            if (person.getBenefitUnit() == null)
                passed = false;
        }

        return passed;
    }

    /*
    Test 2: verify there are no discrepancies in IDs across different units (person, benefit unit, household)
     */

    /*
    Test 3: verify that each existing BenefitUnit has a (responsible) male and/or female
     */

    /*
    Event listener
     */

    public enum Processes {
        RunTests
    }

    @Override
    public void onEvent(Enum<?> anEnum) {
        switch ((Processes) anEnum) {
            case RunTests:
                runTests();
                break;
        }
    }
}
