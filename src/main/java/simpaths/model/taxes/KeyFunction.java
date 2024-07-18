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
    private int dlltsdMan = -1, dlltsdWoman = -1, careProvision = -1;
    private double hoursWorkedPerWeekMan, hoursWorkedPerWeekWoman, originalIncomePerWeek, secondIncomePerWeek, childcareCostPerWeek;

    // define key function here - switchable
    //private KeyFunction1 keyFunction;
    //private KeyFunction2 keyFunction;
    //private KeyFunction3 keyFunction;
    private KeyFunction4 keyFunction;


    /**
     * CONSTRUCTORS
     */
    public KeyFunction() {

        // instantiate key function variant
        //this.keyFunction = new KeyFunction1();
        //this.keyFunction = new KeyFunction2();
        this.keyFunction = new KeyFunction4();
    }
    public KeyFunction(int simYear, int priceYear, int age, int numberMembersOver17, int numberChildrenUnder5, int numberChildren5To9, int numberChildren10To17,
                       double hoursWorkedPerWeekMan, double hoursWorkedPerWeekWoman, int dlltsdMan, int dlltsdWoman, int careProvision, double originalIncomePerWeek) {

        this();

        // check initialisation data
        if (simYear<1900 || simYear>2200)
            throw new RuntimeException("Key function supplied odd simulation year: " + simYear);
        if (age<16 || age>131)
            throw new RuntimeException("Key function supplied odd age: " + age);
        if (numberMembersOver17<1 || numberMembersOver17>2)
            throw new RuntimeException("Key function supplied odd number of adults: " + numberMembersOver17);
        if (numberChildrenUnder5<0 || numberChildrenUnder5>8)
            throw new RuntimeException("Key function supplied odd number of children under 5: " + numberChildrenUnder5);
        if (numberChildren5To9<0 || numberChildren5To9>10)
            throw new RuntimeException("Key function supplied odd number of children aged between 5 and 9: " + numberChildren5To9);
        if (numberChildren10To17<0 || numberChildren10To17>14)
            throw new RuntimeException("Key function supplied odd number of children aged between 10 and 17: " + numberChildren10To17);
        if (hoursWorkedPerWeekMan<0.0 || hoursWorkedPerWeekMan>200.0)
            throw new RuntimeException("Key function supplied odd hours worked by man: " + hoursWorkedPerWeekMan);
        if (hoursWorkedPerWeekWoman<0.0 || hoursWorkedPerWeekWoman>200.0)
            throw new RuntimeException("Key function supplied odd hours worked by woman: " + hoursWorkedPerWeekWoman);
        if ((dlltsdMan<0 && numberMembersOver17==2) || dlltsdMan>1)
            throw new RuntimeException("Key function supplied odd disability status for man: " + dlltsdMan);
        if ((dlltsdWoman<0 && numberMembersOver17==2) || dlltsdWoman>1)
            throw new RuntimeException("Key function supplied odd disability status for woman: " + dlltsdWoman);
        if (dlltsdMan<0 && dlltsdWoman<0)
            throw new RuntimeException("Key function supplied odd disability status for man and woman: " + dlltsdMan);
        if (careProvision<0)
            throw new RuntimeException("Key function supplied odd care provision indicator: " + careProvision);

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
        this.careProvision = careProvision;
    }
    public KeyFunction(int simYear, int priceYear, int age, int numberMembersOver17, int numberChildrenUnder5, int numberChildren5To9, int numberChildren10To17,
                       double hoursWorkedPerWeekMan, double hoursWorkedPerWeekWoman, int dlltsdMan, int dlltsdWoman, int careProvision, double originalIncomePerWeek,
                       double secondIncomePerWeek, double childcareCostPerWeek) {

        this(simYear, priceYear, age, numberMembersOver17, numberChildrenUnder5, numberChildren5To9, numberChildren10To17,
                hoursWorkedPerWeekMan, hoursWorkedPerWeekWoman, dlltsdMan, dlltsdWoman, careProvision, originalIncomePerWeek);
        this.childcareCostPerWeek = childcareCostPerWeek;
        this.secondIncomePerWeek = Math.max(0.0, Math.min(secondIncomePerWeek, originalIncomePerWeek - secondIncomePerWeek));
    }


    /**
     * GETTERS AND SETTERS
     */
    public void setSimYear(int simYear) {
        this.simYear = simYear;
    }

    public void setPriceYear(int priceYear) {
        this.priceYear = priceYear;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getNumberMembersOver17() {
        return numberMembersOver17;
    }

    public void setNumberMembersOver17(int numberMembersOver17) {
        this.numberMembersOver17 = numberMembersOver17;
    }

    public int getNumberChildrenUnder5() {
        return numberChildrenUnder5;
    }

    public void setNumberChildrenUnder5(int numberChildrenUnder5) {
        this.numberChildrenUnder5 = numberChildrenUnder5;
    }

    public int getNumberChildren5To9() {
        return numberChildren5To9;
    }

    public void setNumberChildren5To9(int numberChildren5To9) {
        this.numberChildren5To9 = numberChildren5To9;
    }

    public int getNumberChildren10To17() {
        return numberChildren10To17;
    }

    public void setNumberChildren10To17(int numberChildren10To17) {
        this.numberChildren10To17 = numberChildren10To17;
    }

    public int getDlltsdMan() {
        return dlltsdMan;
    }

    public void setDlltsdMan(int dlltsdMan) {
        this.dlltsdMan = dlltsdMan;
    }

    public int getDlltsdWoman() {
        return dlltsdWoman;
    }

    public void setDlltsdWoman(int dlltsdWoman) {
        this.dlltsdWoman = dlltsdWoman;
    }

    public int getCareProvision() {
        return careProvision;
    }

    public void setCareProvision(int careProvision) {
        this.careProvision = careProvision;
    }

    public double getHoursWorkedPerWeekMan() {
        return hoursWorkedPerWeekMan;
    }

    public void setHoursWorkedPerWeekMan(double hoursWorkedPerWeekMan) {
        this.hoursWorkedPerWeekMan = hoursWorkedPerWeekMan;
    }

    public double getHoursWorkedPerWeekWoman() {
        return hoursWorkedPerWeekWoman;
    }

    public void setHoursWorkedPerWeekWoman(double hoursWorkedPerWeekWoman) {
        this.hoursWorkedPerWeekWoman = hoursWorkedPerWeekWoman;
    }

    public void setOriginalIncomePerWeek(double originalIncomePerWeek) {
        this.originalIncomePerWeek = originalIncomePerWeek;
    }

    public void setSecondIncomePerWeek(double secondIncomePerWeek) {
        this.secondIncomePerWeek = secondIncomePerWeek;
    }

    public void setChildcareCostPerWeek(double childcareCostPerWeek) {
        this.childcareCostPerWeek = childcareCostPerWeek;
    }

    public KeyFunction4 getKeyFunction() {
        return keyFunction;
    }

    public void setKeyFunction(KeyFunction4 keyFunction) {
        this.keyFunction = keyFunction;
    }

    /**
     * WORKER METHODS
     */
    public int getMatchFeatureIndex(MatchFeature feature, int taxDBRegime, int key) {
        return keyFunction.getMatchFeatureIndex(feature, taxDBRegime, key);
    }

    public Integer[] evaluateKeys() {

        if (keyFunction == null) {
            throw new InvalidParameterException("call to evaluate donor keys before KeyFunction populated");
        }
        //return keyFunction.evaluateKeys(simYear, priceYear, age, numberMembersOver17, numberChildrenUnder5, numberChildren5To17, hoursWorkedPerWeekMan, hoursWorkedPerWeekWoman, dlltsdMan, dlltsdWoman, originalIncomePerWeek);
        return keyFunction.evaluateKeys(simYear, priceYear, age, numberMembersOver17, numberChildrenUnder5, numberChildren5To9, numberChildren10To17,
                hoursWorkedPerWeekMan, hoursWorkedPerWeekWoman, dlltsdMan, dlltsdWoman, careProvision, originalIncomePerWeek, secondIncomePerWeek, childcareCostPerWeek);
    }

    public boolean[] isLowIncome(Integer[] keys) {

        if (keyFunction == null) {
            throw new InvalidParameterException("call to evaluate donor keys before KeyFunction populated");
        }
        return keyFunction.isLowIncome(keys);
    }

    public int getSimYear() { return simYear; }
    public int getPriceYear() { return priceYear; }
    public Map<MatchFeature, Map<Integer, Integer>> getTaxdbCounter() {
        return keyFunction.getTaxdbCounter();
    }

    public double getOriginalIncomePerWeek() { return originalIncomePerWeek; }
    public double getSecondIncomePerWeek() { return secondIncomePerWeek; }
    public double getChildcareCostPerWeek() { return childcareCostPerWeek; }
}
