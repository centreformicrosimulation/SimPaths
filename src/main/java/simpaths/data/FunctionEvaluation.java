package simpaths.data;


/**
 *
 * CLASS TO ENCAPSULATE FUNCTION ORDINATES AND FUNCTION VALUE AT ORDINATES
 *
 */
public class FunctionEvaluation {


    /**
     * ATTRIBUTES
     */
    public double[] ordinates;     // function ordinates
    public double value;           // function value at ordinates


    /**
     * CONSTRUCTOR
     * @param number_of_ordinates number of arguments of function
     */
    public FunctionEvaluation(int number_of_ordinates) {
        ordinates = new double[number_of_ordinates];
    }
}
