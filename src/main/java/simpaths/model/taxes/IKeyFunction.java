package simpaths.model.taxes;

import java.util.Map;

public interface IKeyFunction {
    Integer[] evaluateKeys(int simYear, int priceYear, int age, int numberMembersOver17, int numberChildrenUnder5, int numberChildren5To9,
                           int numberChildren10To17, double hoursWorkedPerWeekMan, double hoursWorkedPerWeekWoman, int dlltsdMan, int dlltsdWoman,
                           int careProvision, double originalIncomePerWeek, double secondIncomePerWeek, double childcareCostPerWeek);
    int getMatchFeatureIndex(MatchFeature feature, int taxDBRegime, int keyValue);
    boolean[] isLowIncome(Integer[] keys);
    Map<MatchFeature, Map<Integer, Integer>> getTaxdbCounter();
}
