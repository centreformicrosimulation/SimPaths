package simpaths.model.taxes;

import simpaths.data.Parameters;

import java.util.List;


/**
 *
 * CLASS TO MANAGE TAX-UNIT KEYS FOR IMPUTING TAXES AND BENEFITS
 *
 */
public class DonorKeys {


    /**
     * ATTRIBUTES
     */
    private Integer[] keys = new Integer[Parameters.TAXDB_REGIMES];       // from most fine to most coarse
    private Integer simYear = null;     // year for which keys evaluated
    private Integer priceYear = null;   // year of prices used to measure financial statistics
    private boolean[] lowIncome;  // low income identifier used to determine income imputation method
    private double hoursWorkedPerWeekMan, hoursWorkedPerWeekWoman;
    private Double originalIncomePerWeek = null;
    private Double secondIncomePerWeek = null;
    private Double childcareCostPerWeek = null;
    private double randomDraw = -1.0;  // random innovation used to select candidate for imputation - initialised to -1.0 results in averaging over set of "preferred candidates"


    /**
     * CONSTRUCTORS
     */
    public DonorKeys() {}
    public DonorKeys(double randomDraw) {
        this.randomDraw = randomDraw;
    }


    /**
     * GETTERS AND SETTERS
     */
    public int getKey(int regime) {
        if (regime<0 || regime>=Parameters.TAXDB_REGIMES)
            throw new RuntimeException("request for unrecognised key");
        if (keys[regime]==null)
            throw new RuntimeException("request for uninitialised key");
        return keys[regime];
    }
    public Integer getSimYear() {
        return simYear;
    }
    public Integer getPriceYear() {
        return priceYear;
    }
    public Double getOriginalIncomePerWeek() {
        return originalIncomePerWeek;
    }
    public Double getSecondIncomePerWeek() { return secondIncomePerWeek; }
    public Double getChildcareCostPerWeek() { return childcareCostPerWeek; }
    public double getHoursWorkedPerWeekMan() { return hoursWorkedPerWeekMan; }
    public double getHoursWorkedPerWeekWoman() { return hoursWorkedPerWeekWoman; }

    public double getRandomDraw() { return randomDraw; }
    public boolean isLowIncome(int regime) {
        if (regime<0)
            throw new RuntimeException("attempt to retrieve low income identifier for null regime");
        return lowIncome[regime];
    }


    /**
     * EVALUATE ATTRIBUTES USING KEYFUNCTION
     */
    public void evaluate(KeyFunction function) {

        originalIncomePerWeek = function.getOriginalIncomePerWeek();
        secondIncomePerWeek = function.getSecondIncomePerWeek();
        childcareCostPerWeek = function.getChildcareCostPerWeek();
        simYear = function.getSimYear();
        priceYear = function.getPriceYear();
        keys = function.evaluateKeys();
        lowIncome = function.isLowIncome(keys);
        hoursWorkedPerWeekMan = function.getHoursWorkedPerWeekMan();
        hoursWorkedPerWeekWoman = function.getHoursWorkedPerWeekWoman();
    }
}
