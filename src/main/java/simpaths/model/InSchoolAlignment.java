package simpaths.model;

import microsim.engine.SimulationEngine;
import simpaths.data.IEvaluation;
import simpaths.data.Parameters;
import simpaths.model.enums.Les_c4;
import simpaths.model.enums.TargetShares;

import java.util.Set;


/**
 * InSchoolAlignment adjusts the probability of being a student.
 * The object is designed to assist modification of the intercept of the "inSchool" models.
 *
 * A search routine is used to find the value by which the intercept should be adjusted.
 * If the projected share of students in the population differs from the desired target by more than a specified threshold,
 * then the intercept is adjusted and the share re-evaluated.
 *
 * Importantly, the adjustment needs to be only found once. Modified intercepts can then be used in subsequent simulations.
 */
public class InSchoolAlignment implements IEvaluation {

    private final double targetStudentShare;
    private final Set<Person> persons;
    private final SimPathsModel model;


    // CONSTRUCTOR
    public InSchoolAlignment(Set<Person> persons) {
        this.model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        this.persons = persons;
        targetStudentShare = Parameters.getTargetShare(model.getYear(), TargetShares.Students);
    }


    /**
     * Evaluates the discrepancy between the simulated and target total student share and adjusts probabilities if necessary.
     *
     * This method focuses on the influence of the adjustment parameter 'args[0]' on the difference between the target and
     * simulated student share (error).
     *
     * The error value is returned and serves as the stopping condition in root search routines.
     *
     * @param args An array of parameters, where args[0] represents the adjustment parameter.
     * @return The error in the target aggregate share of students after potential adjustments.
     */
    @Override
    public double evaluate(double[] args) {

        // Ensure each trial point is evaluated from lagged status (pure function for root search).
        persons.parallelStream().forEach(person -> {
            if (person.getLabC4L1() != null) {
                person.setLabC4(person.getLabC4L1());
            }
            person.inSchool(args[0]);
        });

        return targetStudentShare - evalStudentShare();
    }

    public double getTargetStudentShare() {
        return targetStudentShare;
    }

    /**
     * Evaluates the aggregate share of students.
     *
     * This method uses Java streams to count the number of students over the total number of individuals.
     *
     * @return The aggregate share of partnered persons among those eligible, or 0.0 if no eligible persons are found.
     */
    private double evalStudentShare() {

        // Counts aligned students within education age range: 16-29 (range is defined in Model)
        long numStudents = model.getPersons().stream()
                .filter(person -> person.getDemAge() >= Parameters.MIN_AGE_TO_LEAVE_EDUCATION
                        && person.getDemAge() <= Parameters.MAX_AGE_TO_STAY_IN_CONTINUOUS_EDUCATION
                        && !person.isToLeaveSchool()
                        && Les_c4.Student.equals(person.getLabC4())) // count aligned student group only
                .count();
        // Counts individuals within education age range: 16-29 (range is defined in Model)
        long numPeople = model.getPersons().stream()
                .filter(person -> person.getDemAge() >= Parameters.MIN_AGE_TO_LEAVE_EDUCATION
                        && person.getDemAge() <= Parameters.MAX_AGE_TO_STAY_IN_CONTINUOUS_EDUCATION
                        && person.getLabC4() != null)
                .count();

        return (numStudents > 0) ? (double) numStudents / numPeople : 0.0;
    }
}
