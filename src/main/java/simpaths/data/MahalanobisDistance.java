package simpaths.data;

import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.*;

/**
 * MahalanobisDistance is a class for computing the Mahalanobis distance
 * for a given data point with respect to a dataset.
 *
 * The Mahalanobis distance measures the distance between a point and a
 * distribution of points, taking into account the correlation between
 * variables and the variance within each variable.
 *
 * This class uses Apache Commons Math library to calculate the Mahalanobis
 * distance based on a provided dataset.
 *
 * Usage:
 * 1. Create an instance of MahalanobisDistance with your dataset.
 * 2. Use the getMahalanobisDistance method to calculate the distance for
 *    a given data point.
 *
 * See MahalanobisDistanceTest for JUnit5 test cases
 **/

public class MahalanobisDistance {
    RealMatrix matrix, covarianceInverse;
    RealVector meanVector;

    public MahalanobisDistance(double[][] data) {
        // Create a RealMatrix from your data
        matrix = new Array2DRowRealMatrix(data);

        // Create a Covariance object to compute the covariance matrix
        Covariance covariance = new Covariance(matrix);
        RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();

        // Compute the inverse of the covariance matrix
        covarianceInverse = new SingularValueDecomposition(covarianceMatrix).getSolver().getInverse();

        // Calculate the mean vector
        meanVector = calculateColumnMeans(matrix);
    }

    public double getMahalanobisDistance(double[] pointToEvaluate) {
        // Point for which Mahalanobis distance should be calculated
        RealVector dataPoint = MatrixUtils.createRealVector(pointToEvaluate);

        // check dimensions
        if (pointToEvaluate.length != covarianceInverse.getColumnDimension())
            throw new RuntimeException("Mahalanobis distance routine supplied vector of different dimension to covariance matrix");

        // Compute the difference vector
        RealVector diff = dataPoint.subtract(meanVector);

        // Calculate the Mahalanobis distance
        return FastMath.sqrt(diff.dotProduct(covarianceInverse.operate(diff)));
    }

    public double getMahalanobisDistance(double[] point1, double[] point2) {

        // Points for which Mahalanobis distance should be calculated
        RealVector dataPoint1 = MatrixUtils.createRealVector(point1);
        RealVector dataPoint2 = MatrixUtils.createRealVector(point2);

        // check dimensions
        if (point1.length != point2.length)
            throw new RuntimeException("request to calculate Mahalanobis distance for vectors of different lengths");
        if (point1.length != covarianceInverse.getColumnDimension())
            throw new RuntimeException("Mahalanobis distance routine supplied vector 1 of different dimension to covariance matrix");
        if (point2.length != covarianceInverse.getColumnDimension())
            throw new RuntimeException("Mahalanobis distance routine supplied vector 2 of different dimension to covariance matrix");

        // Compute the difference vector
        RealVector diff = dataPoint1.subtract(dataPoint2);

        // Calculate the Mahalanobis distance
        return FastMath.sqrt(diff.dotProduct(covarianceInverse.operate(diff)));
    }


    // Calculate the mean vector for each column in the RealMatrix
    private RealVector calculateColumnMeans(RealMatrix matrix) {
        int numColumns = matrix.getColumnDimension();
        double[] columnMeans = new double[numColumns];

        for (int col = 0; col < numColumns; col++) {
            columnMeans[col] = StatUtils.mean(matrix.getColumn(col));
        }

        return MatrixUtils.createRealVector(columnMeans);
    }
}
