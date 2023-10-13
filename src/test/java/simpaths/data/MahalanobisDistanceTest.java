package simpaths.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MahalanobisDistanceTest {

    @Test
    void getMahalanobisDistance1() {
        // Test data
        double[][] data = {
                {64.0, 580.0, 29.0},
                {66.0, 570.0, 33.0},
                {68.0, 590.0, 37.0},
                {69.0, 660.0, 46.0},
                {73.0, 600.0, 55.0}
        };

        // Create an instance of the MahalanobisDistance class
        MahalanobisDistance mahalanobis = new MahalanobisDistance(data);

        // Test data point for which to calculate Mahalanobis distance
        double[] testPoint = {66.0, 640.0, 44.0};

        // Expected Mahalanobis distance
        double expectedDistance = 5.33;

        // Calculate the Mahalanobis distance
        double calculatedDistance = mahalanobis.getMahalanobisDistance(testPoint);

        // Compare the calculated distance with the expected distance
        assertEquals(expectedDistance, calculatedDistance, 0.01); // adjust the tolerance (0.01) as needed
    }

    @Test
    void getMahalanobisDistance2() {
        // Test data
        double[][] data = {
                {1.0, 2.0, 3.0},
                {1.0, 2.0, 3.0}
        };

        // Create an instance of the MahalanobisDistance class
        MahalanobisDistance mahalanobis = new MahalanobisDistance(data);

        // Test data point for which to calculate Mahalanobis distance
        double[] testPoint = {1.0, 2.0, 3.0};

        // Expected Mahalanobis distance
        double expectedDistance = 0.0;

        // Calculate the Mahalanobis distance
        double calculatedDistance = mahalanobis.getMahalanobisDistance(testPoint);

        // Compare the calculated distance with the expected distance
        assertEquals(expectedDistance, calculatedDistance, 0.01); // adjust the tolerance (0.01) as needed
    }
}