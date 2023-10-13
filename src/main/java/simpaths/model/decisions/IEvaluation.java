package simpaths.model.decisions;

/**
 * INTERFACE TO FACILITATE SWAPPING OF FUNCTIONS FOR NUMERICAL MINIMISATION ROUTINES
 */
public interface IEvaluation {
    double evaluate(double[] args);
}
