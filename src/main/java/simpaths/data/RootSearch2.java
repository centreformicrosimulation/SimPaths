package simpaths.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RootSearch2 {

    private int nn;                     // number of arguments of function - currently limited to 1 dimension
    private double epsOrdinates;        // measurement precision of ordinates
    private double epsFunction;         // measurement precision of function
    double[] lowerBounds;               // lower bounds of continuous control variables to optimise
    double[] upperBounds;               // upper bounds of continuous control variables to optimise
    double[] target;                    // starting co-ordinates at entry and co-ordinates of root at exit
    IEvaluation function;               // function to find root for
    boolean targetAltered = false;      // indicates if target was altered from starting value

    private int iterationCount = 0;
    private final List<IterationInfo> iterationHistory = new ArrayList<>();


    public RootSearch2(double[] lowerBounds, double[] upperBounds, double[] target, IEvaluation function, double epsOrdinates, double epsFunction) {
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

    public static final class IterationInfo {
        private final int iteration;        // 1-based iteration index
        private final double x;             // current abscissa (since 1-D)
        private final double fx;            // function value at x
        private final double step;          // |x_k - x_{k-1}| (NaN for first iter)
        private final boolean funcTolMet;   // |f(x)| <= epsFunction ?
        private final boolean ordTolMet;    // step <= epsOrdinates ?

        public IterationInfo(int iteration, double x, double fx, double step,
                             boolean funcTolMet, boolean ordTolMet) {
            this.iteration = iteration;
            this.x = x;
            this.fx = fx;
            this.step = step;
            this.funcTolMet = funcTolMet;
            this.ordTolMet = ordTolMet;
        }
        public int getIteration() { return iteration; }
        public double getX() { return x; }
        public double getFx() { return fx; }
        public double getStep() { return step; }
        public boolean isFuncTolMet() { return funcTolMet; }
        public boolean isOrdTolMet() { return ordTolMet; }

        @Override public String toString() {
            return "it=" + iteration + ", x=" + x + ", f(x)=" + fx +
                    ", step=" + step + ", funcTol=" + funcTolMet +
                    ", ordTol=" + ordTolMet;
        }
    }

    public void evaluate() {
        // clear previous diagnostics
        iterationHistory.clear();
        iterationCount = 0;

        double[] xg, xn=null, xp=null;
        double fg, fn, fp;

        xg = target;
        fg = function.evaluate(xg);

        iterationHistory.add(new IterationInfo(0, xg[0], fg, Double.NaN,
                Math.abs(fg) <= epsFunction, false));

        if (Math.abs(fg) > epsFunction) {
            // need to conduct search

            targetAltered = true;

            // check starting points
            CheckBoundsForRoot bounds = new CheckBoundsForRoot(lowerBounds, upperBounds, function);
            xn = bounds.getXn();
            fn = bounds.getFn();
            xp = bounds.getXp();
            fp = bounds.getFp();
            if (bounds.getBracketed()) {
                target = zbrent(xn, xp, fn, fp);
            } else {
                final boolean chooseNeg = Math.abs(fn) < Math.abs(fp);
                target = chooseNeg ? xn : xp;
//                if (Math.abs(fn) < Math.abs(fp)) {
//                    target = xn;
//                } else {
//                    target = xp;
//                }
//                // log as a single “decision” step
//                double chosenX = target[0];
//                double fChosen = function.evaluate(target);

                // We already know f(target): it's fn if we chose xn, else fp.
                final double chosenX = target[0];
                final double fChosen = chooseNeg ? fn : fp;

                iterationHistory.add(new IterationInfo(
                        iterationHistory.size(), chosenX, fChosen, Double.NaN,
                        Math.abs(fChosen) <= epsFunction, true));
            }
        }
        iterationCount = iterationHistory.isEmpty() ? 0 : iterationHistory.get(iterationHistory.size() - 1).getIteration();
    }

    private double[] bisection(double[] xn, double[] xg, double[] xp, double fn, double fg, double fp) {

        final int ITMAX = 200;

        double[] xnn = xn, xpp = xp, xgg = xg;
        double fgg = fg;

        int it = 0;
        Double prevX = null;

        while ( (euclideanDistance(xgg, xpp) > epsOrdinates) || (Math.abs(fgg) > epsFunction) ) {

            if (fgg < 0.0) {
                xnn = xgg;
            } else {
                xpp = xgg;
            }
            xgg = midPoint(xnn, xpp);
            double currX = xgg[0];
            fgg = function.evaluate(xgg);

            double step = (prevX == null) ? Double.NaN : Math.abs(currX - prevX);
            boolean funcOk = Math.abs(fgg) <= epsFunction;
            boolean ordOk = (prevX != null) && step <= epsOrdinates;

            it++;
            iterationHistory.add(new IterationInfo(it, currX, fgg, step, funcOk, ordOk));
            prevX = currX;

            if (it > ITMAX)
                throw new RuntimeException("Root search failed to identify solution");
        }
        return xgg;
    }


    /************************************************************
     * Van Wijngaarden-Dekker-Brent Method for root finding in 1 dimension
     * based on Press et al (2007) (Numerical Recipes)
     * NOTE: apache commons has a related method: BrentSolver.java
     ************************************************************/
    private double[] zbrent(double[] xn, double[] xp, double fn, double fp) {

        // search conditions
        final int ITMAX = 200;

        double[] bbv = new double[nn];
        final double EPS = 3.0 * Math.ulp(1.0);
        double aa=xn[0], bb=xp[0], cc=xp[0], dd=0.0, ee=0.0, pp, qq, rr, ss, tol1, xm;
        double fa=fn, fb=fp, fc=fp;

        Double prevX = null;
        int it = (iterationHistory.isEmpty() ? 0 : iterationHistory.get(iterationHistory.size() - 1).getIteration());

        for (int iter=0; iter<ITMAX; iter++) {
            if ((fb > 0.0 && fc > 0.0) || (fb < 0.0 && fc < 0.0)) {
                cc=aa;      //Rename a, b, c and adjust bounding interval d.
                fc=fa;
                dd=bb-aa;
                ee=dd;
            }
            if (Math.abs(fc) < Math.abs(fb)) {
                aa=bb;
                bb=cc;
                cc=aa;
                fa=fb;
                fb=fc;
                fc=fa;
            }
            tol1 = 2.0 * EPS * Math.abs(bb) + 0.5 * epsFunction;      //Convergence check.
            xm = 0.5 * (cc - bb);
            if (Math.abs(xm) <= tol1 || Math.abs(fb) <= epsFunction) {
                bbv[0] = bb;

                // final logging step
                double step = (prevX == null) ? Double.NaN : Math.abs(bb - prevX);
                boolean funcOk = Math.abs(fb) <= epsFunction;
                boolean ordOk = (prevX != null) && step <= epsOrdinates;
                it++;
                iterationHistory.add(new IterationInfo(it, bb, fb, step, funcOk, ordOk));


                return bbv;
            }
            if (Math.abs(ee) >= tol1 && Math.abs(fa) > Math.abs(fb)) {
                ss = fb / fa;   //Attempt inverse quadratic interpolation.
                if (aa == cc) {
                    pp = 2.0 * xm * ss;
                    qq = 1.0 - ss;
                } else {
                    qq = fa / fc;
                    rr = fb / fc;
                    pp = ss * (2.0 * xm * qq * (qq - rr) - (bb - aa) * (rr - 1.0));
                    qq = (qq - 1.0) * (rr - 1.0) * (ss - 1.0);
                }
                if (pp > 0.0) {
                    qq = -qq;     //Check whether in bounds.
                } else {
                    pp = - pp;
                }
                double min1 = 3.0 * xm * qq - Math.abs(tol1 * qq);
                double min2 = Math.abs(ee * qq);
                if (2.0 * pp < Math.min(min1,min2)) {
                    ee = dd;  //Accept interpolation.
                    dd = pp / qq;
                } else {
                    dd = xm;  //Interpolation failed, use bisection.
                    ee = dd;
                }
            } else {
                //Bounds decreasing too slowly, use bisection.
                dd = xm;
                ee = dd;
            }
            aa = bb;  //Move last best guess to a.
            fa = fb;
            if (Math.abs(dd) > tol1)    //Evaluate new trial root.
                bb += dd;
            else if (xm < 0.0) {
                bb -= Math.abs(tol1);
            } else {
                bb += Math.abs(tol1);
            }
            bbv[0] = bb;
            fb=function.evaluate(bbv);

            // log this iteration
            double step = (prevX == null) ? Double.NaN : Math.abs(bb - prevX);
            boolean funcOk = Math.abs(fb) <= epsFunction;
            boolean ordOk = (prevX != null) && step <= epsOrdinates;
            it++;
            iterationHistory.add(new IterationInfo(it, bb, fb, step, funcOk, ordOk));
            prevX = bb;
        }
        throw new RuntimeException("Maximum number of iterations exceeded in zbrent");
    }

    // ======= helpers ======= //
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

    private double[] subtract(double[] xa, double[] xb) {
        double[] xc = new double[nn];
        for (int ii=0; ii<nn; ii++) {
            xc[ii] = (xa[ii] - xb[ii]);
        }
        return xc;
    }

    public boolean isTargetAltered() {return targetAltered;}
    public double[] getTarget() {return target;}

    public int getIterationCount() {
        return iterationCount;
    }

    public List<IterationInfo> getIterationHistory() {
        return Collections.unmodifiableList(iterationHistory);
    }

}
