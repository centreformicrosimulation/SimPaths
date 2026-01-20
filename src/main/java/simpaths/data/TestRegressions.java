package simpaths.data;

import microsim.statistics.regression.IntegerValuedEnum;
import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Region;

import java.util.Map;

public class TestRegressions {

    public static void run(RegressionName regression) {

        if (ManagerRegressions.isDiscreteChoiceModel(regression))
            testDiscreteChoiceModel(regression);
        else
            throw new RuntimeException("unrecognised test regression");
    }

    private static <E extends Enum<E> & IntegerValuedEnum> void testDiscreteChoiceModel(RegressionName regression) {

        Person personProxy;
        personProxy = new Person(true);
        Map<E, Double> probs;


        personProxy.setDemMaleFlag(Gender.Female);
        personProxy.setDemAge(28);
        personProxy.setRegionLocal(Region.UKF);
        personProxy.setYearLocal(2010);
        probs = ManagerRegressions.getProbabilities(personProxy, regression);

        personProxy.setDemMaleFlag(Gender.Male);
        personProxy.setDemAge(23);
        personProxy.setRegionLocal(Region.UKF);
        personProxy.setYearLocal(2010);
        probs = ManagerRegressions.getProbabilities(personProxy, regression);

        personProxy.setDemMaleFlag(Gender.Female);
        personProxy.setDemAge(42);
        personProxy.setRegionLocal(Region.UKF);
        personProxy.setYearLocal(2010);
        probs = ManagerRegressions.getProbabilities(personProxy, regression);

        System.out.println("regression test complete");
    }
}
