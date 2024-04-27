package simpaths.model.decisions;

import simpaths.data.Parameters;

import java.security.InvalidParameterException;


/**
 *
 * CLASS TO DEFINE GRIDS THAT STORE INTERTEMPORAL OPTIMISATION SOLUTIONS
 *
 * MODULES OF THE CLASS FACILITATE INPUT AND EXTRACTION OF OPTIMISED DATA
 * AS WELL AS DESCRIPTIVE DETAIL OF DATA
 *
 * THE GRIDS ARE DEFINED AS ONE-DIMENSIONAL VECTORS
 * ELEMENTS OF EACH VECTOR ARE ORGANISED TO REFLECT IMPLICIT DIMENSIONAL AXES
 * SEE DEFINITION OF THE AXES ATTRIBUTE FOR DETAILS
 *
 */
public class Grids {


    /**
     * ATTRIBUTES
     */
    GridScale scale;           // object describing dimensionality of grid
    public Grid valueFunction;        // grid to store value function
    public Grid consumption;          // grid to store utility maximising consumption decisions (proportion of cash on hand)
    public Grid employment1;          // grid to store utility maximising employment decisions (proportion time spent working by principal income earner)
    public Grid employment2;          // grid to store utility maximising employment decisions (proportion time spent working by secondary income earner)


    /**
     * CONSTRUCTOR
     */
    public Grids() {

        // constructor variables
        scale = new GridScale();

        /*
         * INITIALISE GRID VECTORS
         */
        long gridSize = scale.gridDimensions[scale.simLifeSpan-1][3] + scale.gridDimensions[scale.simLifeSpan-1][2];
        valueFunction = new Grid(scale, gridSize);
        consumption = new Grid(scale, gridSize);
        gridSize = scale.gridDimensions[DecisionParams.maxAgeFlexibleLabourSupply - Parameters.AGE_TO_BECOME_RESPONSIBLE + 1][3];
        if (DecisionParams.FLAG_IO_EMPLOYMENT1) employment1 = new Grid(scale, gridSize);
        if (DecisionParams.FLAG_IO_EMPLOYMENT2) employment2 = new Grid(scale, gridSize);
    }


    /**
     * METHOD TO REPORT GRID SCALE
     */
    public GridScale getScale() { return scale; }


    /*
     * WORKING METHODS
     */


    /**
     * METHOD TO ADD CONTROL OPTIMISATION SOLUTION TO THE GRID ARRAYS
     * @param states the state combination for which a solution applies
     * @param solution the solution obtained for the respective state combination
     */
    public void populate(States states, UtilityMaximisation solution) {

        // evaluate state index
        long gridIndex = states.returnGridIndex();

        // populate grid storage arrays
        valueFunction.put(gridIndex, solution.optimisedUtility);
        int controlCounter = 0;
        consumption.put(gridIndex, solution.controls[controlCounter]);
        controlCounter++;
        if (states.ageYears <= DecisionParams.maxAgeFlexibleLabourSupply) {
            if (DecisionParams.FLAG_IO_EMPLOYMENT1) {
                if (controlCounter < solution.controls.length) {
                    employment1.put(gridIndex, solution.controls[controlCounter]);
                    controlCounter++;
                } else {
                    throw new InvalidParameterException("problem populating solutions for control variable");
                }
            }
            if (states.getCohabitation()) {
                if (DecisionParams.FLAG_IO_EMPLOYMENT2) {
                    if (controlCounter < solution.controls.length) {
                        employment2.put(gridIndex, solution.controls[controlCounter]);
                        controlCounter++;
                    } else {
                        throw new InvalidParameterException("problem populating solutions for control variable");
                    }
                }
            }
        }
    }

    public double getValueFunction(States states) {

        // evaluate state index
        long gridIndex = states.returnGridIndex();
        return valueFunction.get(gridIndex);
    }
    public double getConsumptionShare(States states) {

        // evaluate state index
        long gridIndex = states.returnGridIndex();
        return consumption.get(gridIndex);
    }
    public double getEmployment1(States states) {

        if (states.ageYears <= DecisionParams.maxAgeFlexibleLabourSupply && DecisionParams.FLAG_IO_EMPLOYMENT1) {
            long gridIndex = states.returnGridIndex();
            return employment1.get(gridIndex);
        } else {
            return 0.0;
        }
    }
    public double getEmployment2(States states) {

        if (states.ageYears <= DecisionParams.maxAgeFlexibleLabourSupply && DecisionParams.FLAG_IO_EMPLOYMENT2 && states.getCohabitation()) {
            long gridIndex = states.returnGridIndex();
            return employment2.get(gridIndex);
        } else {
            return 0.0;
        }
    }
}
