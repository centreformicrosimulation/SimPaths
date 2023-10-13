package simpaths.model.decisions;

import java.security.InvalidParameterException;


/**
 *
 * OBJECT TO EVALUATE AND STORE ABSCISSAE AND WEIGHTS FOR GAUSS HERMITE QUADRATURE
 *
 * TOOL FOR APPROXIMATING EXPECTATIONS, OF STANDARD NORMAL DISTRIBUTION (MEAN 0, STDEV 1) BETWEEN -INF AND +INF
 * CODE BASED ON Numerical Recipes, 1987, CHAPTER 4
 *
 */
public class Quadrature {


    /**
     * ATTRIBUTES
     */
    double[] abscissae;     // zeros of the Hermite polynomial
    double[] weights;       // weight coefficients of the Hermite polynomial


    /**
     * CONSTRUCTOR
     */
    public Quadrature(int nn) {

        // initialise attributes
        abscissae = new double[nn];
        weights = new double[nn];

        // working variables
        int jj;
        double eps = 1.0E-14;
        double pim4 = Math.pow(Math.PI, -0.25);
        double zz, zz1, p1, p2, p3, pp, weightSum;
        zz = 0;
        int maxit = 10;
        int mm = (int)(((double)nn+1)/2);   // roots are symmetric, so only calculate half of them
        for (int ii=0; ii<mm; ii++) {

            // define initial guesses for roots
            if ( ii == 0 ) {
                zz = ( Math.pow((2.0*(double)nn+1.0),0.5) - 1.85575 * Math.pow(2.0*(double)nn+1.0,-0.16667) );
            } else if ( ii == 1 ) {
                zz = zz - 1.14 * Math.pow(nn,0.426) / zz;
            } else if ( ii == 2 ) {
                zz = 1.86 * zz - 0.86 * abscissae[0];
            } else if ( ii == 3 ) {
                zz = 1.91 * zz - 0.91 * abscissae[1];
            } else {
                zz = 2.0 * zz - abscissae[ii-2];
            }

            // use Newton's method to refine roots
            jj = 0;
            pp = 0;
            while ( jj < maxit ) {

                p1 = pim4;
                p2 = 0.0;
                for (int kk=1; kk<=nn; kk++) {
                    p3 = p2;
                    p2 = p1;
                    p1 = zz * Math.pow(2.0/(double)kk,0.5) * p2 - Math.pow(((double)kk-1.0)/(double)kk,0.5) * p3;
                }
                pp = Math.pow(2.0*(double)nn,0.5) * p2;
                zz1 = zz;
                zz = zz1 - p1/pp;
                if ( Math.abs(zz-zz1) <= eps ) {
                    jj = maxit + 2;
                } else {
                    jj = jj + 1;
                }
            }
            if ( jj < maxit+2 ) {
                throw new InvalidParameterException("Too many iterations in quadrature evaluation");
            }
            abscissae[nn-1-ii] = -zz;
            abscissae[ii] = zz;
            weights[ii] = 2.0 / (pp*pp);
            weights[nn-1-ii] = weights[ii];
        }
        weightSum = 0;
        for (double vv : weights) {
            weightSum += vv;
        }
        for (int ii = 0; ii<nn; ii++) {
            abscissae[ii] = abscissae[ii] * Math.pow(2,0.5);    // this line controls for omission of 1/2 in exp for weight function
            weights[ii] = weights[ii] / weightSum;             // this line controls for 1/sqrt(pi) in weight function
        }
    }
}
