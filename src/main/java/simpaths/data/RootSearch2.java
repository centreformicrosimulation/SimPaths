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
    private Double lastEvalX = null;
    private Double lastEvalFx = null;
    // Probe distances are fractions of the max reachable distance within current bounds.
    private static final double[] BRACKET_DISTANCE_RATIOS =
            new double[]{(1.0 / 5.0), (2.0 / 5.0), (3.0 / 5.0), (4.0 / 5.0), 1.0};
    private BoundSearchDiagnostics boundDiagnostics = new BoundSearchDiagnostics();


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
        targetAltered = false;
        lastEvalX = null;
        lastEvalFx = null;

        double[] xg, xn=null, xp=null;
        double fg, fn, fp;

        xg = copyPoint(target);
        xg[0] = clampToBounds(xg[0]);
        fg = evalAt(xg[0]);
        boundDiagnostics = new BoundSearchDiagnostics();
        boundDiagnostics.lowerBound = lowerBounds[0];
        boundDiagnostics.upperBound = upperBounds[0];
        boundDiagnostics.initialX = xg[0];
        boundDiagnostics.initialFx = fg;

        iterationHistory.add(new IterationInfo(0, xg[0], fg, Double.NaN,
                Math.abs(fg) <= epsFunction, false));

        if (Math.abs(fg) > epsFunction) {
            // need to conduct search

            targetAltered = true;
            boundDiagnostics.bracketingAttempted = true;

            double probeStart = (lowerBounds[0] <= 0.0 && upperBounds[0] >= 0.0) ? 0.0 : xg[0];
            double probeF = fg;
            boundDiagnostics.probeStartX = probeStart;
            boundDiagnostics.usedZeroAnchor = (probeStart == 0.0 && xg[0] != 0.0);
            if (probeStart != xg[0]) {
                probeF = evalAt(probeStart);
                iterationHistory.add(new IterationInfo(
                        iterationHistory.size(), probeStart, probeF, Double.NaN,
                        Math.abs(probeF) <= epsFunction, false));
            }
            boundDiagnostics.probeStartFx = probeF;

            BracketResult bounds = directionalBracketFromStart(probeStart, probeF);
            xn = bounds.xn;
            fn = bounds.fn;
            xp = bounds.xp;
            fp = bounds.fp;
            boundDiagnostics.bracketed = bounds.bracketed;
            if (bounds.bracketed) {
                boundDiagnostics.bracketLower = xn[0];
                boundDiagnostics.bracketUpper = xp[0];
            }

            if (bounds.bracketed) {
                // Keep the model state aligned with the endpoint used by zbrent's initial fb.
                fp = evalAt(xp[0]);
                target = zbrent(xn, xp, fn, fp);
            } else {
                target = copyPoint(bounds.bestX);
                final double chosenX = target[0];
                // Ensure final state corresponds exactly to chosen target.
                final double fChosen = evalAt(chosenX);

                iterationHistory.add(new IterationInfo(
                        iterationHistory.size(), chosenX, fChosen, Double.NaN,
                        Math.abs(fChosen) <= epsFunction, true));
            }
            boundDiagnostics.finalX = target[0];
        } else {
            boundDiagnostics.convergedAtInitial = true;
            boundDiagnostics.probeStartX = xg[0];
            boundDiagnostics.probeStartFx = fg;
            boundDiagnostics.finalX = xg[0];
            boundDiagnostics.bracketed = false;
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
            fgg = evalAt(currX);

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
            fb = evalAt(bb);

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

    private BracketResult directionalBracketFromStart(double startX, double startFx) {
        BracketResult result = new BracketResult();
        result.bestX = asPoint(startX);
        result.bestFx = startFx;

        // Bound-consistent probing ladder:
        // W = furthest reachable distance from start within [lowerBound, upperBound].
        // Distances = ratio * W, using BRACKET_DISTANCE_RATIOS.
        final double[] bracketDistances = buildBracketDistances(startX);
        if (bracketDistances.length == 0) {
            return result;
        }

        final double firstDistance = bracketDistances[0];
        final double leftX = clampToBounds(startX - firstDistance);
        final double rightX = clampToBounds(startX + firstDistance);

        ProbePoint left = null;
        ProbePoint right = null;

        if (leftX != startX) {
            left = new ProbePoint(leftX, evalAt(leftX));
            addProbeToHistory(left);
            updateBest(result, left);
        }
        if (rightX != startX) {
            right = new ProbePoint(rightX, evalAt(rightX));
            addProbeToHistory(right);
            updateBest(result, right);
        }

        if (left != null && right != null && oppositeSigns(left.fx, right.fx)) {
            setBracket(result, left, right);
            return result;
        }

        ProbePoint current;
        int direction;
        if (left != null && right != null) {
            if (Math.abs(left.fx) <= Math.abs(right.fx)) {
                current = left;
                direction = -1;
            } else {
                current = right;
                direction = 1;
            }
        } else if (left != null) {
            current = left;
            direction = -1;
        } else if (right != null) {
            current = right;
            direction = 1;
        } else {
            return result;
        }

        for (int ii = 1; ii < bracketDistances.length; ii++) {
            double candidateX = clampToBounds(startX + direction * bracketDistances[ii]);
            if (candidateX == current.x) {
                break;
            }
            ProbePoint candidate = new ProbePoint(candidateX, evalAt(candidateX));
            addProbeToHistory(candidate);
            updateBest(result, candidate);

            if (oppositeSigns(current.fx, candidate.fx)) {
                setBracket(result, current, candidate);
                return result;
            }
            current = candidate;
        }

        return result;
    }

    private double[] buildBracketDistances(double startX) {
        final double maxReachWithinBounds = Math.max(
                Math.abs(startX - lowerBounds[0]),
                Math.abs(upperBounds[0] - startX)
        );
        final double w = maxReachWithinBounds;
        if (w <= 0.0) {
            return new double[0];
        }

        List<Double> distances = new ArrayList<>();
        double prev = -1.0;
        for (double ratio : BRACKET_DISTANCE_RATIOS) {
            double d = ratio * w;
            // keep strictly increasing unique distances and avoid zero-sized probes
            if (d > prev + 1.0e-12 && d > 1.0e-12) {
                distances.add(d);
                prev = d;
            }
        }
        if (distances.isEmpty()) {
            return new double[0];
        }
        double[] out = new double[distances.size()];
        for (int i = 0; i < distances.size(); i++) {
            out[i] = distances.get(i);
        }
        return out;
    }

    private void addProbeToHistory(ProbePoint p) {
        iterationHistory.add(new IterationInfo(
                iterationHistory.size(),
                p.x,
                p.fx,
                Double.NaN,
                Math.abs(p.fx) <= epsFunction,
                false));
    }

    private void updateBest(BracketResult result, ProbePoint p) {
        if (Math.abs(p.fx) < Math.abs(result.bestFx)) {
            result.bestX = asPoint(p.x);
            result.bestFx = p.fx;
        }
    }

    private void setBracket(BracketResult result, ProbePoint a, ProbePoint b) {
        result.bracketed = true;
        if (a.fx < 0.0) {
            result.xn = asPoint(a.x);
            result.fn = a.fx;
            result.xp = asPoint(b.x);
            result.fp = b.fx;
        } else {
            result.xn = asPoint(b.x);
            result.fn = b.fx;
            result.xp = asPoint(a.x);
            result.fp = a.fx;
        }
    }

    private boolean oppositeSigns(double fa, double fb) {
        return (fa < 0.0 && fb > 0.0) || (fa > 0.0 && fb < 0.0);
    }

    private double clampToBounds(double x) {
        return Math.max(lowerBounds[0], Math.min(upperBounds[0], x));
    }

    private double[] asPoint(double x) {
        return new double[]{x};
    }

    private double evalAt(double x) {
        if (lastEvalX != null && Double.compare(lastEvalX, x) == 0) {
            return lastEvalFx;
        }
        final double fx = function.evaluate(asPoint(x));
        lastEvalX = x;
        lastEvalFx = fx;
        return fx;
    }

    private double[] copyPoint(double[] x) {
        return java.util.Arrays.copyOf(x, x.length);
    }

    private static final class ProbePoint {
        final double x;
        final double fx;

        ProbePoint(double x, double fx) {
            this.x = x;
            this.fx = fx;
        }
    }

    private static final class BracketResult {
        boolean bracketed = false;
        double[] xn;
        double[] xp;
        double fn;
        double fp;
        double[] bestX;
        double bestFx;
    }

    private static final class BoundSearchDiagnostics {
        double lowerBound;
        double upperBound;
        double bracketLower = Double.NaN;
        double bracketUpper = Double.NaN;
        double initialX;
        double initialFx;
        double probeStartX;
        double probeStartFx;
        boolean usedZeroAnchor;
        boolean convergedAtInitial;
        boolean bracketingAttempted;
        boolean bracketed;
        double finalX;
    }

    public boolean isTargetAltered() {return targetAltered;}
    public double[] getTarget() {return target;}

    public int getIterationCount() {
        return iterationCount;
    }

    public List<IterationInfo> getIterationHistory() {
        return Collections.unmodifiableList(iterationHistory);
    }

    public String getBoundSearchDiagnosticsSummary() {
        final String bracketingStatus;
        if (boundDiagnostics.convergedAtInitial) {
            bracketingStatus = "initial point met tolerance";
        } else if (boundDiagnostics.bracketingAttempted && boundDiagnostics.bracketed) {
            bracketingStatus = "root bracket found within bounds";
        } else if (boundDiagnostics.bracketingAttempted) {
            bracketingStatus = "no sign-change bracket found within bounds";
        } else {
            bracketingStatus = "bracketing status unavailable";
        }

        if (boundDiagnostics.convergedAtInitial) {
            return "init=(" + boundDiagnostics.initialX + ", f=" + boundDiagnostics.initialFx + ")"
                    + ", " + bracketingStatus;
        }

        final String boundsForSummary;
        if (boundDiagnostics.bracketingAttempted && boundDiagnostics.bracketed) {
            final double lo = Math.min(boundDiagnostics.bracketLower, boundDiagnostics.bracketUpper);
            final double hi = Math.max(boundDiagnostics.bracketLower, boundDiagnostics.bracketUpper);
            boundsForSummary = "Bounds=[" + lo + ", " + hi + "]";
        } else {
            boundsForSummary = "Bounds=[" + boundDiagnostics.lowerBound + ", " + boundDiagnostics.upperBound + "]";
        }

        return boundsForSummary
                + ", init=(" + boundDiagnostics.initialX + ", f=" + boundDiagnostics.initialFx + ")"
                + ", " + bracketingStatus;
    }

}
