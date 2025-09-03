package simpaths.model.taxes;

import simpaths.model.TaxEvaluation;


public class TestTaxRoutine {

    public static void run() {

        int year, age, adults, children04, children59, children1017, disability1, disability2, careProvision, key, matchRegime, ucTakeUp;
        long donorID;
        double hoursWork1, hoursWork2, originalIncomePerMonth, secondIncomePerMonth, childcarePerMonth, disposableIncomePerMonth;
        double liquidWealth, socialCareCostPerMonth;
        TaxEvaluation evaluatedTransfers;

        // age: 18-44; 45-under spa; spa
        // adults: single; couple
        // children 04: 0, 1, 2+
        // children 59: 0, 1, 2+
        // children 1017: 0, 1+
        // hoursWork1/2: 0-5; 6-15; 16+
        // disability1/2: 0, 1
        // income: <946.17 <=2985.70, >2985.70

        year = 2025;
        age = 78;
        adults = 2;
        children04 = 0;
        children59 = 0;
        children1017 = 0;
        hoursWork1 = 0.0;
        hoursWork2 = 0.0;
        disability1 = 1;
        disability2 = 0;
        careProvision = 0;
        ucTakeUp = 1;
        originalIncomePerMonth = 1500.0;
        secondIncomePerMonth = 0.0;
        childcarePerMonth = 0.0;
        liquidWealth = 0.0;
        socialCareCostPerMonth = 0.0;
        evaluatedTransfers = new TaxEvaluation(year, age, adults, children04, children59, children1017, hoursWork1, hoursWork2, disability1, disability2, careProvision, ucTakeUp, originalIncomePerMonth, secondIncomePerMonth, childcarePerMonth, liquidWealth, socialCareCostPerMonth, -2);
        key = evaluatedTransfers.getKeys().getKey(0);
        donorID = evaluatedTransfers.getImputedTransfers().getDonorID();
        disposableIncomePerMonth = evaluatedTransfers.getDisposableIncomePerMonth();
        matchRegime = evaluatedTransfers.getImputedTransfers().getMatchCriterion();

        int fin = 0;
    }
}
