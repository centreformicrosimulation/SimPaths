package simpaths.data;

public class RootSearch {

    private int nn;                     // number of arguments of function - currently limited to 1 dimension
    private double epsOrdinates;        // measurement precision of ordinates
    private double epsFunction;         // measurement precision of function
    double[] lowerBounds;               // lower bounds of continuous control variables to optimise
    double[] upperBounds;               // upper bounds of continuous control variables to optimise
    double[] target;                    // starting co-ordinates at entry and co-ordinates of root at exit
    IEvaluation function;               // function to find root for
    boolean targetAltered = false;      // indicates if target was altered from starting value

    public RootSearch(double[] lowerBounds, double[] upperBounds, double[] target, IEvaluation function, double epsOrdinates, double epsFunction) {
        this.lowerBounds = lowerBounds;
        this.upperBounds = upperBounds;
        this.target = target;
        this.function = function;
        this.epsOrdinates = epsOrdinates;
        this.epsFunction = epsFunction;
        nn = lowerBounds.length;
        if (nn > 1)
            throw new RuntimeException("Root search currently only adapted to search over single dimension");
    }

    public void evaluate() {
        target = bisection();
    }

    private double[] bisection() {

        // search conditions
        final int ITMAX = 200;

        double[] xg, xs, xn=null, xp=null;
        double fg, fs;

        xg = target;
        fg = function.evaluate(xg);
        if (Math.abs(fg) > epsFunction) {
            // need to conduct search

            targetAltered = true;

            // check starting points
            xs = lowerBounds;
            fs = function.evaluate(xs);
            if (fs < 0.0) {
                xn = xs;
            } else {
                xp = xs;
            }
            xs = upperBounds;
            fs = function.evaluate(xs);
            if (fs < 0.0) {
                if (xn!=null)
                    throw new RuntimeException("Root search supplied boundaries that are both negative");
                xn = xs;
            } else {
                if (xp!=null)
                    throw new RuntimeException("Root search supplied boundaries that are both positive");
                xp = xs;
            }

            // start search
            int nn = 0;
            while ( (euclideanDistance(xg, xp) > epsOrdinates) || (Math.abs(fg) > epsFunction) ) {

                if (fg < 0.0) {
                    xn = xg;
                } else {
                    xp = xg;
                }
                xg = midPoint(xn, xp);
                fg = function.evaluate(xg);

                nn++;
                if (nn>ITMAX)
                    throw new RuntimeException("Root search failed to identify solution");
            }
        }
        return xg;
    }

    private double euclideanDistance(double[] xa, double[] xb) {
        double distance = 0.0;
        for (int ii=0; ii<nn; ii++) {
            distance += Math.pow(xa[ii] - xb[ii], 2.0);
        }
        return Math.pow(distance, 0.5);
    }

    private double[] midPoint(double[] xa, double[] xb) {
        double[] xc = new double[nn];
        for (int ii=0; ii<nn; ii++) {
            xc[ii] = (xa[ii] + xb[ii]) / 2.0;
        }
        return xc;
    }

    public boolean isTargetAltered() {return targetAltered;}
    public double[] getTarget() {return target;}

}
