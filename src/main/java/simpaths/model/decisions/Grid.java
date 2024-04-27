package simpaths.model.decisions;

import java.security.InvalidParameterException;
import simpaths.model.decisions.DecisionParams;


/**
 *
 * CLASS TO STORE DATA FOR GRID ASSOCIATED WITH SINGLE OPTIMISATION VARIABLE
 *
 */
public class Grid {


    /**
     * ATTRIBUTES
     */
    private final int MAX_LEN = Integer.MAX_VALUE - 16;
    // MAX_LEN defines the maximum array length permitted for the grid object.  The limits imposed by Java
    // vary by JVM, and are currently due to use of int (4 byte) indexing used for arrays.  The "16" buffer
    // assumed here is arbitrary, accounting for sporadic reports about varying array length constraints.

    long size;              // length of grid array stored here
    GridScale scale;        // object describing dimensionality of grid
    double[] grid;          // array to store variable values at grid ordinates
    double[][] gridLong;    // array to store variable values at grid ordinates, if grid dimensions extend beyond int(4)


    /**
     * CONSTRUCTOR
     */
    public Grid(GridScale scale, long size) {

        this.scale = scale;
        this.size = size;
        if (size <= MAX_LEN) {
            grid = new double[(int)size];
            for (int jj=0; jj<(int)size; jj++) {
                grid[jj] = DecisionParams.GRID_DEFAULT_VALUE;
            }
        } else {
            int slices = 1 + (int)((double)size / (double)MAX_LEN);
            gridLong = new double[slices][];
            for (int ii=0; ii<slices; ii++) {
                if (ii==slices-1) {
                    gridLong[ii] = new double[(int)(size%MAX_LEN)];
                    for (int jj=0; jj<(int)(size%MAX_LEN); jj++) {
                        grid[jj] = DecisionParams.GRID_DEFAULT_VALUE;
                    }
                } else {
                    gridLong[ii] = new double[MAX_LEN];
                    for (int jj=0; jj<MAX_LEN; jj++) {
                        grid[jj] = DecisionParams.GRID_DEFAULT_VALUE;
                    }
                }
            }
        }
    }


    /*
     * WORKING METHODS
     */


    /**
     * METHOD TO ADD VALUE TO GRID STORE
     * @param index storage index for value
     * @param value value to add to store
     */
    public void put(long index, double value) {

        if (grid!=null) {
            grid[(int)index] = value;
        } else {
            int slice = (int)((double)index / (double)MAX_LEN);
            int ii = (int)(index%MAX_LEN);
            gridLong[slice][ii] = value;
        }
    }

    /**
     * METHOD TO GET VALUE FROM GRID STORE
     * @param index storage index for value
     * @return value retrieved from store
     */
    public double get(long index) {
        double value;
        if (grid!=null) {
            value = grid[(int)index];
        } else {
            int slice = (int)((double)index / (double)MAX_LEN);
            int ii = (int)(index%MAX_LEN);
            value = gridLong[slice][ii];
        }
        return value;
    }
    public double getChecked(States supplied, long index) {

        double value = get(index);
        if (Math.abs(value-DecisionParams.GRID_DEFAULT_VALUE) < 1.0E-10) {
            supplied.systemReportError(index);
            throw new InvalidParameterException("attempt to retrieve uninitialised grid value");
        }
        return value;
    }

    /**
     * METHOD TO RETURN A NUMERICAL APPROXIMATION FOR THE GRID VALUE ASSOCIATED WITH A COMPLETE VECTOR OF STATE
     * CHARACTERISTICS
     *
     * The method begins by identifying the grid slice associated with the supplied combination of discrete states
     * Linear states are then approximated by a linear interpolation method, interpolateContinuous
     *
     * @param supplied full state combination (continuous and discrete)
     * @param solutionCall boolean equal to true if call is from the search routine for a maximum to the IO problem
     *                      This flag is used to distinguish treatment of states that are considered discrete for the
     *                      IO solution process and continuous otherwise (e.g. birth year)
     * @return  numerical approximation of grid value
     */
    public double interpolateAll(States supplied, boolean solutionCall) {

        // find references to control for discrete state variables
        States copy = new States(supplied);
        int noStates = (int)scale.gridDimensions[supplied.ageIndex][4] + (int)scale.gridDimensions[supplied.ageIndex][5];
        double continuousCutoff = 0.3;
        if (solutionCall) continuousCutoff += 0.3;
        boolean flagAllContinuous = true;
        int numberContinuous = 0;
        for (int ii=0; ii<noStates; ii++) {
            if (scale.axes[supplied.ageIndex][ii][3] > continuousCutoff) {
                // treat as continuous
                numberContinuous++;
                copy.states[ii] = scale.axes[supplied.ageIndex][ii][1];    // set copy value to lower bound
                if (!flagAllContinuous) {
                    throw new InvalidParameterException("continuous states do not appear to have been organised contiguously");
                }
            } else {
                // treat as discrete
                flagAllContinuous = false;
            }
        }
        long startingIndex = copy.returnGridIndex();

        // return result
        return interpolateContinuous(supplied, numberContinuous, startingIndex);
    }

    /**
     * METHOD TO RETURN A NUMERICAL APPROXIMATION FOR THE GRID VALUE ASSOCIATED WITH THE SUBSET OF CONTINUOUS STATE
     * CHARACTERISTICS
     *
     * Method uses linear interpolation methods to approximate value for grid slice associated with continuous
     * state variables, as supplied by the interpolateAll() method.
     *
     * @param supplied state combination to interpolate over
     * @param dimensions number of states to conduct the interpolation over
     * @param startingIndex starting index for grid slice of interpolation
     * @return  numerical approximation of grid value
     */
    public double interpolateContinuous(States supplied, int dimensions, long startingIndex) {

        //  working variables
        final double TOL = Math.ulp(1.0);
        long indexHere;
        double valueHere, weightTotal;
        int[] dims = new int[dimensions];
        int[] offset = new int[dimensions];
        int[] mm = new int[dimensions];
        int[] nn = new int[dimensions];
        int[] dd = new int[dimensions];
        double[] ss = new double[dimensions];
        double[] weight = new double[(int)Math.pow(2,dimensions)];

        //  evaluate interpolation offsets;
        dims[0] = (int)(scale.axes[supplied.ageIndex][0][0]+TOL);
        offset[0] = 1;
        if (dimensions > 1) {
            for (int ii=1; ii<dimensions; ii++) {
                dims[ii] = (int)(scale.axes[supplied.ageIndex][ii][0]+TOL);
                offset[ii] = offset[ii-1] * dims[ii-1];
            }
        }

        // identify reference points
        for (int ii = 0; ii<dimensions; ii++) {
            ss[ii] = (supplied.states[ii] - scale.axes[supplied.ageIndex][ii][1]) *
                    (scale.axes[supplied.ageIndex][ii][0]-1) /
                    (scale.axes[supplied.ageIndex][ii][2] - scale.axes[supplied.ageIndex][ii][1]);
            mm[ii] = (int)(ss[ii] + TOL);
            if ( mm[ii] == (dims[ii]-1) ) {
                // at upper bound - step one backward
                mm[ii] -= 1;
            }
            ss[ii] -= mm[ii];
        }

        // check that point is internal to grid
        for (int ii=0; ii<dimensions; ii++) {
            double err = Math.ulp(scale.axes[supplied.ageIndex][ii][2]);
            if (supplied.states[ii] < scale.axes[supplied.ageIndex][ii][1]-err) {
                supplied.systemReportError();
                throw new InvalidParameterException("interpolation point below minimum described by grid");
            } else if (supplied.states[ii] > scale.axes[supplied.ageIndex][ii][2]+err) {
                supplied.systemReportError();
                throw new InvalidParameterException("interpolation point above maximum described by grid");
            }
        }

        // interpolate states
        dd[0] = -1;
        double result = 0;
        weightTotal = 0;
        for (int ii=0; ii<Math.pow(2, dimensions); ii++) {
            // loop over each test point

            // update counter
            dd[0] += 1;
            int jj = 0;
            while (dd[jj] > 1) {
                dd[jj] = 0;
                jj++;
                dd[jj] += 1;
            }

            // calculate weights and indices
            weight[ii] = 1.0;
            for (jj=0; jj<dimensions; jj++) {
                nn[jj] = mm[jj] + dd[jj];
                weight[ii] *= (1 - Math.abs(dd[jj]-ss[jj]));
            }
            if ( weight[ii] > (1.0/Math.pow(2,dimensions)*1.0E-3)) {
                // take point into consideration

                indexHere = 0;
                for (jj = 0; jj<dimensions; jj++) {
                    indexHere += (long)nn[jj] * offset[jj];
                }
                indexHere += startingIndex;
                valueHere = getChecked(supplied, indexHere);
                result += valueHere * weight[ii];
                weightTotal += weight[ii];
            }
        }

        result /= weightTotal;
        return result;
    }
}
