package simpaths.data;

import microsim.data.MultiKeyCoefficientMap;
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
        String[] goodValueVector = new String[] {"Dag", "D_Home_owner", "PovertyToNonPoverty"};
        String[] keyVector = new String[] {"REGRESSOR"};

        MultiKeyCoefficientMap badMap = new MultiKeyCoefficientMap(keyVector, badValueVector);
        for (String badValue : badValueVector) {badMap.putValue(badValue, 0);}

        MultiKeyCoefficientMap goodMap = new MultiKeyCoefficientMap(keyVector, goodValueVector);
        for (String goodValue : goodValueVector) {goodMap.putValue(goodValue, 0);}

        assertThrows(RuntimeException.class, () -> Parameters.validateRegressors(badMap, "A map designed to contain invalid values", "Sheet1"));
        assertDoesNotThrow(() -> Parameters.validateRegressors(goodMap, "A map designed to contain valid values", "Sheet1"));

    }
}