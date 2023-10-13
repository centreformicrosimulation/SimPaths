package simpaths.model.taxes;

import java.security.InvalidParameterException;
import java.util.Map;


/**
 *
 * WRAPPER CLASS TO CONSOLIDATE SWITCHING OF FUNCTION FOR EVALUATING DONOR KEYS TO IMPUTE TAX AND BENEFIT PAYMENTS
 *
 */
public class KeyFunction {


    /**
     * ATTRIBUTES
     */
    private int simYear = -999, priceYear = -999, age, numberMembersOver17, numberChildrenUnder5, numberChildren5To9, numberChildren10To17;
    private int dlltsdMan = -1, dlltsdWoman = -1;
    private double hoursWorkedPerWeekMan, hoursWorkedPerWeekWoman, originalIncomePerWeek, secondIncomePerWeek, childcareCostPerWeek;

    // define key function here - switchable
    //private KeyFunction1 keyFunction;
    //private KeyFunction2 keyFunction;
    private KeyFunction3 keyFunction;


    /**
     * CONSTRUCTORS
     */
    public KeyFunction() {

        // instantiate key function variant
        //this.keyFunction = new KeyFunction1();
        //this.keyFunction = new KeyFunction2();
        this.keyFunction = new KeyFunction3();
    }
    public KeyFunction(int simYear, int priceYear, int age, int numberMembersOver17, int numberChildrenUnder5, int numberChildren5To9, int numberChildren10To17,
                       double hoursWorkedPerWeekMan, double hoursWorkedPerWeekWoman, int dlltsdMan, int dlltsdWoman, double originalIncomePerWeek) {

        this();

        // check initialisation data
        if (simYear<1900 || simYear>2200)
            throw new RuntimeException("Key function supplied odd simulation year");
        if (age<16 || age>131)
            throw new RuntimeException("Key function supplied odd age");
        if (numberMembersOver17<1 || numberMembersOver17>2)
            throw new RuntimeException("Key function supplied odd number of adults");
        if (numberChildrenUnder5<0 || numberChildrenUnder5>5)
            throw new RuntimeException("Key function supplied odd number of children under 5");
        if (numberChildren5To9<0 || numberChildren5To9>5)
            throw new RuntimeException("Key function supplied odd number of children aged between 5 and 9");
        if (numberChildren10To17<0 || numberChildren10To17>7)
            throw new RuntimeException("Key function supplied odd number of children aged between 10 and 17");
        if (hoursWorkedPerWeekMan<0.0 || hoursWorkedPerWeekMan>200.0)
            throw new RuntimeException("Key function supplied odd hours worked by man");
        if (hoursWorkedPerWeekWoman<0.0 || hoursWorkedPerWeekWoman>200.0)
            throw new RuntimeException("Key function supplied odd hours worked by woman");
        if ((dlltsdMan<0 && numberMembersOver17==2) || dlltsdMan>1)
            throw new RuntimeException("Key function supplied odd disability status for man");
        if ((dlltsdWoman<0 && numberMembersOver17==2) || dlltsdWoman>1)
            throw new RuntimeException("Key function supplied odd disability status for woman");
        if (dlltsdMan<0 && dlltsdWoman<0)
            throw new RuntimeException("Key function supplied odd disability status for man and woman");
        if (originalIncomePerWeek<-10000.0 || originalIncomePerWeek>100000.0)
            throw new RuntimeException("Key function supplied odd original income per week");

        // set attributes
        this.simYear = simYear;
        this.priceYear = priceYear;
        this.age = age;
        this.numberMembersOver17 = numberMembersOver17;
        this.numberChildrenUnder5 = numberChildrenUnder5;
        this.numberChildren5To9 = numberChildren5To9;
        this.numberChildren10To17 = numberChildren10To17;
        this.hoursWorkedPerWeekMan = hoursWorkedPerWeekMan;
        this.hoursWorkedPerWeekWoman = hoursWorkedPerWeekWoman;
        this.originalIncomePerWeek = originalIncomePerWeek;
        this.dlltsdMan = dlltsdMan;
        this.dlltsdWoman = dlltsdWoman;
    }
    public KeyFunction(int simYear, int priceYear, int age, int numberMembersOver17, int numberChildrenUnder5, int numberChildren5To9, int numberChildren10To17,
                       double hoursWorkedPerWeekMan, double hoursWorkedPerWeekWoman, int dlltsdMan, int dlltsdWoman, double originalIncomePerWeek,
                       double secondIncomePerWeek, double childcareCostPerWeek) {

        this(simYear, priceYear, age, numberMembersOver17, numberChildrenUnder5, numberChildren5To9, numberChildren10To17,
                hoursWorkedPerWeekMan, hoursWorkedPerWeekWoman, dlltsdMan, dlltsdWoman, originalIncomePerWeek);
        this.childcareCostPerWeek = childcareCostPerWeek;
        this.secondIncomePerWeek = Math.max(0.0, Math.min(secondIncomePerWeek, originalIncomePerWeek - secondIncomePerWeek));
    }

    public Integer[] evaluateKeys() {

        if (keyFunction == null) {
            throw new InvalidParameterException("call to evaluate donor keys before KeyFunction populated");
        }
        //return keyFunction.evaluateKeys(simYear, priceYear, age, numberMembersOver17, numberChildrenUnder5, numberChildren5To17, hoursWorkedPerWeekMan, hoursWorkedPerWeekWoman, dlltsdMan, dlltsdWoman, originalIncomePerWeek);
        return keyFunction.evaluateKeys(simYear, priceYear, age, numberMembersOver17, numberChildrenUnder5, numberChildren5To9, numberChildren10To17,
                hoursWorkedPerWeekMan, hoursWorkedPerWeekWoman, dlltsdMan, dlltsdWoman, originalIncomePerWeek, secondIncomePerWeek, childcareCostPerWeek);
    }

    public boolean isLowIncome() {

        if (keyFunction == null) {
            throw new InvalidParameterException("call to evaluate donor keys before KeyFunction populated");
        }
        return keyFunction.isLowIncome(priceYear, originalIncomePerWeek);
    }

    public int getSimYear() { return simYear; }
    public int getStatePensionAge() {
        return keyFunction.getStatePensionAge(age, simYear);
    }
    public int getPriceYear() { return priceYear; }
    public Map<MatchFeature, Map<Integer, Integer>> getTaxdbCounter() {
        return keyFunction.getTaxdbCounter();
    }

    public double getOriginalIncomePerWeek() { return originalIncomePerWeek; }
    public double getSecondIncomePerWeek() { return secondIncomePerWeek; }
    public double getChildcareCostPerWeek() { return childcareCostPerWeek; }
}
