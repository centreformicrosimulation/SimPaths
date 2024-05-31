package simpaths.model;

import simpaths.data.Parameters;
import simpaths.model.taxes.*;

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
    private Match match;
    private double socialCareSupportPerMonth = 0.0;


    /**
     * CONSTRUCTORS
     */
    public TaxEvaluation(){}
    public TaxEvaluation(int simYear, int age, int numberMembersOver17, int numberChildrenUnder5, int numberChildren5To9,
                         int numberChildren10To17, double hoursWorkedPerWeekMan, double hoursWorkedPerWeekWoman,
                         int disabilityMan, int disabilityWoman, int careProvision, double originalIncomePerMonth, double secondIncomePerMonth,
                         double childcareCostPerMonth, double randomDraw) {

        // evaluate imputed transfer payments
        keys = new DonorKeys(randomDraw);

        // all prices defined for base price year by default
        int priceYear = Parameters.BASE_PRICE_YEAR;

        // evaluate transfer payments
        evaluateImputedTaxes(simYear, priceYear, age, numberMembersOver17, numberChildrenUnder5, numberChildren5To9, numberChildren10To17,
                hoursWorkedPerWeekMan, hoursWorkedPerWeekWoman, disabilityMan, disabilityWoman, careProvision, originalIncomePerMonth,
                secondIncomePerMonth, childcareCostPerMonth);

    }
    // used for expectations and benefit unit evaluations
    public TaxEvaluation(int year, int age, int numberMembersOver17, int numberChildrenUnder5, int numberChildren5To9, int numberChildren10To17,
                         double hoursWorkedPerWeekMan, double hoursWorkedPerWeekWoman, int disabilityMan, int disabilityWoman, int careProvision,
                         double originalIncomePerMonth, double secondIncomePerMonth, double childcareCostPerMonth, double socialCareCostPerMonth,
                         Double liquidWealth, double randomDraw) {

        this(year, age, numberMembersOver17, numberChildrenUnder5, numberChildren5To9, numberChildren10To17, hoursWorkedPerWeekMan,
                hoursWorkedPerWeekWoman, disabilityMan, disabilityWoman, careProvision, originalIncomePerMonth, secondIncomePerMonth, childcareCostPerMonth, randomDraw);

        if (Parameters.flagSocialCare) {

            // consider social support for formal care expenditure
            if (liquidWealth==null)
                throw new RuntimeException("problem identifying wealth in evaluation of social care costs after transfer payments");
            boolean flagCouple = (numberMembersOver17 > 1) ? true : false;
            boolean flagSPA = (Parameters.getStatePensionAge(year, age) <= age) ? true : false;
            socialCareSupportPerMonth = new SocialCareExpenditureSupport(year, flagCouple, flagSPA, socialCareCostPerMonth, imputedTransfers.getDisposableIncomePerMonth(), liquidWealth).getSupportPerMonth();
        }
    }


    /**
     * WORKER METHODS
     */
    private void evaluateImputedTaxes(int simYear, int priceYear, int age, int numberMembersOver17, int numberChildrenUnder5, int numberChildren5To9,
                                      int numberChildren10To17, double hoursWorkedPerWeekMan, double hoursWorkedPerWeekWoman,
                                      int disabilityMan, int disabilityWoman, int careProvision, double originalIncomePerMonth, double secondIncomePerMonth,
                                      double childcareCostPerMonth) {

        double originalIncomePerWeek = originalIncomePerMonth / Parameters.WEEKS_PER_MONTH;  // can be negative
        double secondIncomePerWeek = secondIncomePerMonth / Parameters.WEEKS_PER_MONTH;
        double childcareCostPerWeek = childcareCostPerMonth / Parameters.WEEKS_PER_MONTH;
        keyFunction = new KeyFunction(simYear, priceYear, age, numberMembersOver17, numberChildrenUnder5,
                numberChildren5To9, numberChildren10To17, hoursWorkedPerWeekMan, hoursWorkedPerWeekWoman, disabilityMan,
                disabilityWoman, careProvision, originalIncomePerWeek, secondIncomePerWeek, childcareCostPerWeek);
        keys.evaluate(keyFunction);
        imputedTransfers = new DonorTaxImputation(keys);
        imputedTransfers.evaluate();
        match = new Match(keys, imputedTransfers.getDonorID(), imputedTransfers.getMatchCriterion(), Math.sinh(imputedTransfers.getTargetNormalisedOriginalIncome()));
        if (numberChildren5To9+numberChildren10To17+numberChildrenUnder5==0 && childcareCostPerMonth>0.0)
            throw new RuntimeException("call for childcare with no children");
        if (imputedTransfers.getMatchCriterion()==100996 && imputedTransfers.getDonorID()==2019282201)
            throw new RuntimeException("this is my error");
    }

    public Match getMatch() {
        return match;
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
