package simpaths.model;

import simpaths.data.Parameters;
import simpaths.model.taxes.DonorKeys;
import simpaths.model.taxes.DonorTaxImputation;
import simpaths.model.taxes.KeyFunction;
import simpaths.model.taxes.SocialCareSupport;

/**
 *
 * CLASS TO MANAGE EVALUATION OF TAX AND BENEFIT PAYMENTS
 *
 * Combines imputation from the "taxes" module with functions included in this object
 *
 */
public class TaxEvaluation {


    /**
     * ATTRIBUTES
     */
    private DonorTaxImputation imputedTransfers;
    private KeyFunction keyFunction;
    private DonorKeys keys;
    private double socialCareSupportPerMonth = 0.0;


    /**
     * CONSTRUCTORS
     */
    public TaxEvaluation(){}
    public TaxEvaluation(int year, int age, int numberMembersOver17, int numberChildrenUnder5, int numberChildren5To9,
                         int numberChildren10To17, double hoursWorkedPerWeekMan, double hoursWorkedPerWeekWoman,
                         int disabilityMan, int disabilityWoman, double originalIncomePerMonth, double secondIncomePerMonth,
                         double childcareCostPerMonth, double randomDraw) {

        // evaluate imputed transfer payments
        keys = new DonorKeys(randomDraw);
        evaluateImputedTaxes(year, age, numberMembersOver17, numberChildrenUnder5, numberChildren5To9, numberChildren10To17, hoursWorkedPerWeekMan, hoursWorkedPerWeekWoman, disabilityMan, disabilityWoman, originalIncomePerMonth, secondIncomePerMonth, childcareCostPerMonth);
    }
    public TaxEvaluation(int year, int age, int numberMembersOver17, int numberChildrenUnder5, int numberChildren5To9, int numberChildren10To17,
                         double hoursWorkedPerWeekMan, double hoursWorkedPerWeekWoman, int disabilityMan, int disabilityWoman,
                         double originalIncomePerMonth, double secondIncomePerMonth, double childcareCostPerMonth, double socialCareCostPerMonth,
                         Double liquidWealth, double randomDraw) {

        this(year, age, numberMembersOver17, numberChildrenUnder5, numberChildren5To9, numberChildren10To17, hoursWorkedPerWeekMan,
                hoursWorkedPerWeekWoman, disabilityMan, disabilityWoman, originalIncomePerMonth, secondIncomePerMonth, childcareCostPerMonth, randomDraw);

        if (Parameters.flagSocialCare) {
            // consider social care costs
            if (liquidWealth==null)
                throw new RuntimeException("projecting social care support requires liquid wealth to be explicit");
            boolean flagCouple = (numberMembersOver17 > 1) ? true : false;
            boolean flagSPA = (keyFunction.getStatePensionAge() <= age) ? true : false;
            socialCareSupportPerMonth = new SocialCareSupport(year, flagCouple, flagSPA, socialCareCostPerMonth, imputedTransfers.getDisposableIncomePerMonth(), liquidWealth).getSupportPerMonth();
        }
    }


    /**
     * WORKER METHODS
     */
    private void evaluateImputedTaxes(int year, int age, int numberMembersOver17, int numberChildrenUnder5, int numberChildren5To9,
                                      int numberChildren10To17, double hoursWorkedPerWeekMan, double hoursWorkedPerWeekWoman,
                                      int disabilityMan, int disabilityWoman, double originalIncomePerMonth, double secondIncomePerMonth,
                                      double childcareCostPerMonth) {

        double originalIncomePerWeek = originalIncomePerMonth / Parameters.WEEKS_PER_MONTH;  // can be negative
        double secondIncomePerWeek = secondIncomePerMonth / Parameters.WEEKS_PER_MONTH;
        double childcareCostPerWeek = childcareCostPerMonth / Parameters.WEEKS_PER_MONTH;
        keyFunction = new KeyFunction(year, Parameters.BASE_PRICE_YEAR, age, numberMembersOver17, numberChildrenUnder5,
                numberChildren5To9, numberChildren10To17, hoursWorkedPerWeekMan, hoursWorkedPerWeekWoman, disabilityMan,
                disabilityWoman, originalIncomePerWeek, secondIncomePerWeek, childcareCostPerWeek);
        keys.evaluate(keyFunction);
        imputedTransfers = new DonorTaxImputation(keys);
        imputedTransfers.evaluate();
    }

    public double getDisposableIncomePerMonth() {
        return imputedTransfers.getDisposableIncomePerMonth() + socialCareSupportPerMonth;
    }
    public double getDisposableIncomePerWeek() {
        return imputedTransfers.getDisposableIncomePerWeek() + socialCareSupportPerMonth / Parameters.WEEKS_PER_MONTH;
    }
    public double getBenefitsReceivedPerMonth() {
        return imputedTransfers.getBenefitsReceivedPerMonth() + socialCareSupportPerMonth;
    }
    public double getGrossIncomePerMonth() {
        return imputedTransfers.getGrossIncomePerMonth();
    }
    public DonorKeys getKeys() {
        return keys;
    }
    public DonorTaxImputation getImputedTransfers() {
        return imputedTransfers;
    }
}
