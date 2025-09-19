package simpaths.model.lifetime_incomes;

import microsim.data.MultiKeyCoefficientMap;
import org.apache.commons.collections4.keyvalue.MultiKey;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

public class EquivalisedIncomeCDF {
    TreeMap<Double,Double> cdf = new TreeMap<>();
    Object[] keys;

    public EquivalisedIncomeCDF(MultiKeyCoefficientMap data) {

        for (Object key: data.keySet()) {

            double keyValue = ((Number) ((MultiKey) key).getKey(0)).doubleValue();
            double valHere = ((Number) data.getValue(key)).doubleValue();
            cdf.put(keyValue,valHere);
        }
        keys = cdf.keySet().toArray();
    }

    public double getValue(double rnd) {

        // find bracket keys
        int lwr = 0;
        int upr = keys.length-1;
        while (lwr < upr-1) {
            int tstIndex = (upr + lwr) / 2;
            double tstValue = cdf.get(keys[tstIndex]);
            if (tstValue < rnd) {
                lwr = tstIndex;
            } else {
                upr = tstIndex;
            }
        }

        // interpolate value and return
        double lwrValue = cdf.get(keys[lwr]);
        double uprValue = cdf.get(keys[upr]);
        double ratio = (rnd - lwrValue)/(uprValue - lwrValue);
        double lwrKey = (Double)keys[lwr];
        double uprKey = (Double)keys[upr];
        return (lwrKey + ratio*(uprKey - lwrKey));
    }
}
