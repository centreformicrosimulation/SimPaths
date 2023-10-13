package simpaths.model.decisions;


/**
 *
 * CLASS TO STORE METHODS FOR RANKING ARRAYS
 *
 */
public class RankArray {


    /**
     * METHOD FOR EVALUATING RANK INDEX OF SUPPLIED ARRAY
     *
     * @param array array of double formatted data
     * @return integer array of indices that rank array for lowest (array[return[0]]) to highest (array[return[length-1]])
     */
    public static int[] rankDouble(double[] array) {

        // initialise output
        int[] rank = new int[array.length];

        // initialise working variables
        double xvala, xvalb;
        int[] jwrkt = new int[array.length];
        int lmtna, lmtnc, irng1, irng2, nval, iwrk, iwrkd, iwrkf, jinda, iinda, iindb;

        // start analysis
        nval = array.length;
        if (nval == 0) return rank;
        if (nval == 1) {
            rank[0] = 0;
            return rank;
        }

        // Fill-in the index array, creating ordered couples
        for (int iind = 1; iind < nval; iind += 2) {
            if (array[iind - 1] <= array[iind]) {
                rank[iind - 1] = iind - 1;
                rank[iind] = iind;
            } else {
                rank[iind - 1] = iind;
                rank[iind] = iind - 1;
            }
        }
        if (nval % 2 != 0) {
            rank[nval - 1] = nval - 1;
        }

        // We will now have ordered subsets A - B - A - B - ...
        // and merge A and B couples into     C   -   C   - ...

        lmtnc = 4;

        // First iteration. The length of the ordered subsets goes from 2 to 4
        if (nval > 2) {

            // Loop on merges of A and B into C
            iwrkd = 0;
            while (iwrkd < nval) {

                if ((iwrkd + 4) > nval) {

                    if ((iwrkd + 2) < nval) {
                        // 1 2 3
                        if (array[rank[iwrkd + 1]] <= array[rank[iwrkd + 2]]) {
                            // 1 3 2
                            iwrkd = nval;
                        }
                        if (array[rank[iwrkd]] <= array[rank[iwrkd + 2]]) {
                            irng2 = rank[iwrkd + 1];
                            rank[iwrkd + 1] = rank[iwrkd + 2];
                            rank[iwrkd + 2] = irng2;
                            // 3 1 2
                        } else {
                            irng1 = rank[iwrkd];
                            rank[iwrkd] = rank[iwrkd + 2];
                            rank[iwrkd + 2] = rank[iwrkd + 1];
                            rank[iwrkd + 1] = irng1;
                        }
                    }
                    iwrkd = nval;
                } else {
                    // 1 2 3 4
                    if (array[rank[iwrkd + 1]] > array[rank[iwrkd + 2]]) {
                        // 1 3 x x
                        if (array[rank[iwrkd]] <= array[rank[iwrkd + 2]]) {
                            irng2 = rank[iwrkd + 1];
                            rank[iwrkd + 1] = rank[iwrkd + 2];
                            if (array[irng2] <= array[rank[iwrkd + 3]]) {
                                // 1 3 2 4
                                rank[iwrkd + 2] = irng2;
                            } else {
                                // 1 3 4 2
                                rank[iwrkd + 2] = rank[iwrkd + 3];
                                rank[iwrkd + 3] = irng2;
                            }
                            // 3 x x x
                        } else {
                            irng1 = rank[iwrkd];
                            irng2 = rank[iwrkd + 1];
                            rank[iwrkd] = rank[iwrkd + 2];
                            if (array[irng1] <= array[rank[iwrkd + 3]]) {
                                rank[iwrkd + 1] = irng1;
                                if (array[irng2] <= array[rank[iwrkd + 3]]) {
                                    // 3 1 2 4
                                    rank[iwrkd + 2] = irng2;
                                } else {
                                    // 3 1 4 2
                                    rank[iwrkd + 2] = rank[iwrkd + 3];
                                    rank[iwrkd + 3] = irng2;
                                }
                            } else {
                                // 3 4 1 2
                                rank[iwrkd + 1] = rank[iwrkd + 3];
                                rank[iwrkd + 2] = irng1;
                                rank[iwrkd + 3] = irng2;
                            }
                        }
                    }
                }
                iwrkd += 4;
            }

            // The Cs become As and Bs
            lmtna = 4;

            // Iteration loop. Each time, the length of the ordered subsets is doubled
            while (lmtna < nval) {
                iwrkf = 0;
                lmtnc = 2 * lmtnc;

                // Loop on merges of A and B into C
                boolean flagContinue = true;
                while (flagContinue) {
                    iwrk = iwrkf;
                    iwrkd = iwrkf;
                    jinda = iwrkf + lmtna;
                    iwrkf = iwrkf + lmtnc;
                    if (iwrkf >= nval) {
                        if (jinda >= nval) {
                            flagContinue = false;
                        } else {
                            iwrkf = nval;
                        }
                    }
                    if (flagContinue) {
                        iinda = 0;
                        iindb = jinda;
                        if (lmtna >= 0) System.arraycopy(rank, iwrkd, jwrkt, 0, lmtna);
                        xvala = array[jwrkt[iinda]];
                        xvalb = array[rank[iindb]];
                        boolean flagContinue2 = true;
                        while (flagContinue2) {

                            //  We still have unprocessed values in both A and B
                            if (xvala > xvalb) {
                                rank[iwrk] = rank[iindb];
                                iindb = iindb + 1;
                                if (iindb < iwrkf) {
                                    xvalb = array[rank[iindb]];
                                } else {
                                    // Only A still with unprocessed values
                                    for (int ii = iwrk + 1; ii < iwrkf; ii++) {
                                        rank[ii] = jwrkt[iinda + ii - iwrk - 1];
                                    }
                                    flagContinue2 = false;
                                }
                            } else {
                                rank[iwrk] = jwrkt[iinda];
                                iinda = iinda + 1;
                                if (iinda < lmtna) {
                                    xvala = array[jwrkt[iinda]];
                                } else {
                                    flagContinue2 = false;
                                }
                            }
                            iwrk = iwrk + 1;
                        }
                    }
                }
                // The Cs become As and Bs
                lmtna = 2 * lmtna;
            }
        }

        // return
        return rank;
    }
}
