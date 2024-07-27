package simpaths.data;

public class CheckBoundsForRoot {

    private double[] xn = null;     // negative ordinate
    private double[] xp = null;     // positive ordinate
    private double fn;              // function value at negative ordinate
    private double fp;              // function value at positive ordinate
    private boolean bracketed = true;      // boolean indicating whether root is bracketed

    public CheckBoundsForRoot(double[] x1, double[] x2, IEvaluation function) {

        double f1, f2;

        // check starting points
        f1 = function.evaluate(x1);
        if (f1 < 0.0) {
            xn = x1;
            fn = f1;
        } else {
            xp = x1;
            fp = f1;
        }
        f2 = function.evaluate(x2);
        if (f2 < 0.0) {
            if (xn==null) {
                xn = x2;
                fn = f2;
            } else {
                bracketed = false;
                xp = x2;
                fp = f2;
            }
        } else {
            if (xp==null) {
                xp = x2;
                fp = f2;
            } else {
                bracketed = false;
                xn = x2;
                fn = f2;
            }
        }
    }

    public boolean getBracketed() { return bracketed; }
    public double[] getXn() { return xn; }
    public double[] getXp() { return xp; }
    public double getFn() { return fn; }
    public double getFp() { return fp; }
}
