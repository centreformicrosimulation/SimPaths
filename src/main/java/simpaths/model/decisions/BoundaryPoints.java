package simpaths.model.decisions;

import java.util.Arrays;


/**
 *
 * CLASS TO EVALUATE AND STORE BOUNDARY POINTS OF LINEAR ORDINATE SEARCH CO-ORDINATES
 *
 */
public class BoundaryPoints {


    /**
     * ATTRIBUTES
     */
    boolean boundarySolution;      // indicator that the search vector describes a corner solution
    double[] lowerBoundary;        // array to store lower boundary ordinates
    double[] upperBoundary;        // array to store upper boundary ordinates


    /**
     *  CONSTRUCTOR
     *
     * @param ax lower corner of search domain
     * @param cx upper corner of search domain
     * @param bx point on linear search vector
     * @param xi gradient of linear search vector
     */
    public BoundaryPoints(double[] ax, double[] cx, double[] bx, double[] xi) {

        // initialise attributes
        int nn = ax.length;
        lowerBoundary = new double[nn];
        upperBoundary = new double[nn];
        boundarySolution = true;

        // start analysis
        int idOmit;
        final double zeps = 1.0E-10;
        double testVal, adjFactor;
        double[] testx = new double[nn];

        // rank dimensions from steepest to least steep gradient
        double[] xiTemp = new double[nn];
        for (int ii=0; ii<nn; ii++) {
            xiTemp[ii] = Math.abs(xi[ii]);
        }
        int[] rank = RankArray.rankDouble(xiTemp);

        // identify limit points
        int lpId = 1;
        for (int ii=nn-1; ii>=0; ii--) {
            // test from fastest to slowest dimension

            for (int jj = 1; jj <= 2; jj++) {
                // test on ax first and { cx

                if (lpId <= 2) {
                    // require two limit points for the line

                    if (jj == 1) {
                        // test along dimension ax
                        testVal = ax[rank[ii]];
                    } else {
                        // test along dimension cx
                        testVal = cx[rank[ii]];
                    }
                    if (bx[rank[ii]] == testVal) {
                        // no space to project - bx must denote one limit point (as we are working from fastest to slowest dimension)

                        if (lpId == 1) {
                            // allocate first limit point identified to Aax
                            lowerBoundary = Arrays.copyOf(bx, nn);
                        } else {
                            upperBoundary = Arrays.copyOf(bx, nn);
                        }
                        lpId = lpId + 1;
                    } else {
                        // project to boundary point

                        adjFactor = (testVal - bx[rank[ii]]) / xi[rank[ii]];
                        idOmit = 0;
                        for (int kk = 0; kk < nn; kk++) {

                            testx[kk] = bx[kk] + adjFactor * xi[kk];
                            if (Math.abs(testx[kk] - Math.max(ax[kk], cx[kk])) < zeps) {

                                testx[kk] = Math.max(ax[kk], cx[kk]);
                            } else if (Math.abs(testx[kk] - Math.min(ax[kk], cx[kk])) < zeps) {

                                testx[kk] = Math.min(ax[kk], cx[kk]);
                            } else if ((testx[kk] > Math.max(ax[kk], cx[kk])) || (testx[kk] < Math.min(ax[kk], cx[kk]))) {
                                // projected point outside boundary - boundary point defined by alternative limit condition

                                idOmit = 1;
                            }
                        }
                        if (idOmit == 0) {
                            // found a limit point

                            if (lpId == 1) {

                                lowerBoundary = Arrays.copyOf(testx, nn);
                            } else {

                                upperBoundary = Arrays.copyOf(testx, nn);
                            }
                            lpId = lpId + 1;
                        }
                    }
                }  // test over limit points
            }  // test over ax and cx
        }  // test over each dimension of search space

        // check if we have a corner solution
        for (int ii=0; ii<nn; ii++) {

            if ( Math.abs(lowerBoundary[ii] - upperBoundary[ii]) > 1.0E-5 ) {

                boundarySolution = false;
                break;
            }
        }
    }
}
