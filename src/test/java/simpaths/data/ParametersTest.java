package simpaths.data;

import microsim.data.MultiKeyCoefficientMap;
import org.apache.commons.math3.util.FastMath;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ParametersTest {

    /**
     * Tests regressor validation logic using valid/invalid maps
     */
    @Test
    void validateRegressors() {

        String[] badValueVector = new String[] {"Dag", "Not_a_valid_value"};
        String[] goodValueVector = new String[] {"Dag", "Dhh_owned_L1", "PovertyToNonPoverty"};
        String[] keyVector = new String[] {"REGRESSOR"};

        MultiKeyCoefficientMap badMap = new MultiKeyCoefficientMap(keyVector, badValueVector);
        for (String badValue : badValueVector) {badMap.putValue(badValue, 0);}

        MultiKeyCoefficientMap goodMap = new MultiKeyCoefficientMap(keyVector, goodValueVector);
        for (String goodValue : goodValueVector) {goodMap.putValue(goodValue, 0);}

        assertThrows(RuntimeException.class, () -> Parameters.validateRegressors(badMap, "A map designed to contain invalid values", "Sheet1"));
        assertDoesNotThrow(() -> Parameters.validateRegressors(goodMap, "A map designed to contain valid values", "Sheet1"));

    }

    @Test
    void asinhHandlesLargeNegativeValues() {

        double val = FastMath.asinh(-4.848628049616065E8);

        assertTrue(Double.isFinite(val));
        assertTrue(val < 0.0);
        assertEquals(-20.692523713076813, val, 1.0e-12);
        assertEquals(-FastMath.asinh(4.848628049616065E8), val, 1.0e-12);
    }
}
