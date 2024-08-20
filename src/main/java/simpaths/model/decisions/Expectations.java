package simpaths.model.decisions;

import java.security.InvalidParameterException;
import java.util.*;

import simpaths.data.ManagerRegressions;
import simpaths.data.Parameters;
import simpaths.data.RegressionName;
import simpaths.model.enums.*;
import simpaths.model.BenefitUnit;
import simpaths.model.Person;
import simpaths.model.TaxEvaluation;
import simpaths.model.taxes.Match;
import simpaths.model.taxes.Matches;

import static simpaths.data.Parameters.asinh;


/**
 *
 * CLASS TO DEFINE STATE EXPECTATIONS FOR UTILITY MAXIMISATION
 *
 */
public class Expectations {


    /**
     * ATTRIBUTES
     */
    // current period characteristics
    States currentStates;           // object describing prevailing state combination from which expectations are projected
    GridScale scale;                // GridScale object defining dimensionality of IO look-up table
    boolean cohabitation;           // true if current state defines cohabiting couple
    boolean retiring;
    double equivalenceScale;        // scale to equivalise for current benefitUnit demographics
    double fullTimeHourlyEarningsPotential;     // current hourly wage rate if working
    double liquidWealth;            // current wealth available to finance consumption
    double pensionIncomePerYear;    // private pension income per year
    double availableCredit;         // maximum credit currently available
    double mortalityProbability;    // probability of death before next period

    // next period characteristics
    int ageYearsThisPeriod;         // age in years for current period
    int ageIndexNextPeriod;         // age index for state expectations
    int ageYearsNextPeriod;         // age in years for next period expectations
    int numberExpected;             // number of state combinations comprising expectations, conditional on survival
    double[] probability;           // vector recording probability associated with each anticipated state combination, conditional on survival
    States[] anticipated;           // vector to store anticipated state combinations

    // responses to controls
    double leisureTime;             // proportion of time spent in leisure
    double disposableIncomeAnnual;  // disposable income
    double cashOnHand;              // total value of pot that can be used to finance consumption within period
    Matches imperfectMatches = new Matches();

     // OBJECTS FOR EVALUATING MODEL TAX AND BENEFIT AND REGRESSION FUNCTIONS
    BenefitUnit benefitUnitProxyThisPeriod;
    Person personProxyNextPeriod;
    Person personProxyThisPeriod;


    /**
     * CONSTRUCTOR TO POPULATE EXPECTATIONS THAT ARE IN OUTER LOOP AND INVARIANT TO AGENT DECISIONS
     * @param currentStates States object for storage of current (outer) state combination
     */
    public Expectations(States currentStates) {

        // prevailing characteristics - based on currentStates
        scale = currentStates.scale;
        cohabitation = currentStates.getCohabitation();
        retiring = false;
        equivalenceScale = currentStates.oecdEquivalenceScale();

        // prospective characteristics (deterministic)
        ageYearsThisPeriod = currentStates.ageYears;
        ageIndexNextPeriod = currentStates.ageIndex + 1;
        ageYearsNextPeriod = ageYearsThisPeriod + 1;
        numberExpected = 1;
        probability = new double[numberExpected];
        anticipated = new States[numberExpected];
        probability[0] = 1.0;
        anticipated[0] = new States(scale, ageYearsNextPeriod);
        if (ageYearsNextPeriod <= DecisionParams.maxAge) {

            // birth year
            int stateIndexCurrPeriod = scale.getIndex(Axis.BirthYear, ageYearsThisPeriod);
            int stateIndexNextPeriod = scale.getIndex(Axis.BirthYear, ageYearsNextPeriod);
            for (int ii = 0; ii < numberExpected; ii++) {
                anticipated[ii].states[stateIndexNextPeriod] = currentStates.states[stateIndexCurrPeriod];
            }

            //gender (1 = female)
            stateIndexCurrPeriod = scale.getIndex(Axis.Gender, ageYearsThisPeriod);
            stateIndexNextPeriod = scale.getIndex(Axis.Gender, ageYearsNextPeriod);
            for (int ii = 0; ii < numberExpected; ii++) {
                anticipated[ii].states[stateIndexNextPeriod] = currentStates.states[stateIndexCurrPeriod];
            }
        }

        // proxy to evaluate regression projections for current period
        benefitUnitProxyThisPeriod = new BenefitUnit(true);
        benefitUnitProxyThisPeriod.setYearLocal(currentStates.getYear());
        benefitUnitProxyThisPeriod.setOccupancyLocal(currentStates.getOccupancyCode());
        benefitUnitProxyThisPeriod.setDeh_c3Local(currentStates.getEducationCode());
        benefitUnitProxyThisPeriod.setRegion(currentStates.getRegionCode());
    }

    /**
    * CONSTRUCTOR TO POPULATE EXPECTATIONS THAT ARE INVARIANT TO AGENT DECISIONS
    * @param currentStates States object for storage of current (outer) state combination
    * @param outerExpectations Expectations object for storage of expectations of outer states that are independent of decisions
    */
    public Expectations(States currentStates, Expectations outerExpectations) {

        // copy outerExpectations
        scale = outerExpectations.scale;
        cohabitation = outerExpectations.cohabitation;
        retiring = outerExpectations.retiring;
        equivalenceScale = outerExpectations.equivalenceScale;
        ageYearsThisPeriod = outerExpectations.ageYearsThisPeriod;
        ageIndexNextPeriod = outerExpectations.ageIndexNextPeriod;
        ageYearsNextPeriod = outerExpectations.ageYearsNextPeriod;
        numberExpected = outerExpectations.numberExpected;
        probability = outerExpectations.probability;
        anticipated = outerExpectations.anticipated;
        benefitUnitProxyThisPeriod = new BenefitUnit(outerExpectations.benefitUnitProxyThisPeriod, true);

        // prevailing characteristics - based on currentStates
        this.currentStates = currentStates;
        fullTimeHourlyEarningsPotential = currentStates.getFullTimeHourlyEarningsPotential();
        liquidWealth = currentStates.getLiquidWealth();
        if (ageYearsThisPeriod == DecisionParams.maxAge) {
            availableCredit = 0;
            mortalityProbability = 1;
        } else {
            availableCredit = - (Math.exp(scale.axes[ageIndexNextPeriod][0][1]) - DecisionParams.C_LIQUID_WEALTH);
            mortalityProbability = Parameters.getMortalityProbability(currentStates.getGenderCode(), ageYearsThisPeriod, currentStates.getYear());
        }
        pensionIncomePerYear = currentStates.getPensionPerYear();
    }

    /**
     * CONSTRUCTOR TO COPY AND EXPAND ON invariantExpectations
     * @param invariantExpectations set of expectations that are invariant to control variables
     */
    public Expectations(Expectations invariantExpectations) {

        // copy existing attributes
        scale = invariantExpectations.scale;
        cohabitation = invariantExpectations.cohabitation;
        retiring = invariantExpectations.retiring;
        equivalenceScale = invariantExpectations.equivalenceScale;
        ageYearsThisPeriod = invariantExpectations.ageYearsThisPeriod;
        ageIndexNextPeriod = invariantExpectations.ageIndexNextPeriod;
        ageYearsNextPeriod = invariantExpectations.ageYearsNextPeriod;
        numberExpected = invariantExpectations.numberExpected;
        probability = new double[numberExpected];
        anticipated = new States[numberExpected];
        for (int ii = 0; ii< numberExpected; ii++) {
            probability[ii] = invariantExpectations.probability[ii];
            anticipated[ii] = new States(invariantExpectations.anticipated[ii]);
        }
        benefitUnitProxyThisPeriod = new BenefitUnit(invariantExpectations.benefitUnitProxyThisPeriod, true);
        currentStates = invariantExpectations.currentStates;
        fullTimeHourlyEarningsPotential = invariantExpectations.fullTimeHourlyEarningsPotential;
        pensionIncomePerYear = invariantExpectations.pensionIncomePerYear;
        liquidWealth = invariantExpectations.liquidWealth;
        availableCredit = invariantExpectations.availableCredit;
        mortalityProbability = invariantExpectations.mortalityProbability;

        // add new data for within period regression specifications
        personProxyThisPeriod = new Person(true);
        personProxyThisPeriod.setDag(ageYearsThisPeriod);
        personProxyThisPeriod.setRegionLocal(currentStates.getRegionCode());
        personProxyThisPeriod.setDgn(currentStates.getGenderCode());
        personProxyThisPeriod.setDhe(currentStates.getHealthCode());
        personProxyThisPeriod.setDeh_c3(currentStates.getEducationCode());
        personProxyThisPeriod.setDcpstLocal(currentStates.getDcpst());
        personProxyThisPeriod.setSocialCareProvision(currentStates.getSocialCareProvisionCode());
        personProxyThisPeriod.populateSocialCareReceipt(currentStates.getSocialCareReceiptStateCode());

        // add person proxy for next period expectations
        personProxyNextPeriod = new Person(true);
        personProxyNextPeriod.setYearLocal(currentStates.getYearByAge(ageYearsNextPeriod));
        personProxyNextPeriod.setDhhtp_c4_lag1Local(currentStates.getHouseholdTypeCode());
        personProxyNextPeriod.setYdses_c5_lag1Local(Ydses_c5.Q3);
        personProxyNextPeriod.setNumberChildrenAllLocal_lag1(currentStates.getChildrenAll());
        personProxyNextPeriod.setNumberChildrenAllLocal(currentStates.getChildrenAll());
        personProxyNextPeriod.setNumberChildren02Local_lag1(currentStates.getChildren02());
        personProxyNextPeriod.setDag(ageYearsNextPeriod);
        personProxyNextPeriod.setRegionLocal(currentStates.getRegionCode());
        personProxyNextPeriod.setDgn(currentStates.getGenderCode());
        personProxyNextPeriod.setDlltsd(currentStates.getDlltsd());
        personProxyNextPeriod.setDlltsd_lag1(currentStates.getDlltsd());
        personProxyNextPeriod.setDhe(currentStates.getHealthCode());
        personProxyNextPeriod.setDhe_lag1(currentStates.getHealthCode());
        personProxyNextPeriod.populateSocialCareReceipt_lag1(currentStates.getSocialCareReceiptStateCode());
        personProxyNextPeriod.setSocialCareProvision_lag1(currentStates.getSocialCareProvisionCode());
        personProxyNextPeriod.setDed(currentStates.getStudentIndicator());
        personProxyNextPeriod.setDeh_c3(currentStates.getEducationCode());
        personProxyNextPeriod.setDeh_c3_lag1(currentStates.getEducationCode());
        personProxyNextPeriod.setDehf_c3(DecisionParams.EDUCATION_FATHER);
        personProxyNextPeriod.setDehm_c3(DecisionParams.EDUCATION_MOTHER);
        if (ageYearsNextPeriod <= DecisionParams.MAX_AGE_COHABITATION) {
            personProxyNextPeriod.setDcpstLocal(currentStates.getDcpst());
        } else {
            if (currentStates.getDcpst().equals(Dcpst.Partnered))
                personProxyNextPeriod.setDcpstLocal(Dcpst.PreviouslyPartnered);
            else
                personProxyNextPeriod.setDcpstLocal(Dcpst.SingleNeverMarried);
        }
        personProxyNextPeriod.setDcpst_lag1(currentStates.getDcpst());
        personProxyNextPeriod.setLiwwh((ageYearsNextPeriod - Parameters.AGE_TO_BECOME_RESPONSIBLE) * DecisionParams.MONTHS_EMPLOYED_PER_YEAR);
        personProxyNextPeriod.setL1_fullTimeHourlyEarningsPotential(fullTimeHourlyEarningsPotential);
        personProxyNextPeriod.setIoFlag(true);
        if (cohabitation) {
            personProxyNextPeriod.setDehsp_c3_lag1(currentStates.getEducationCode());
            personProxyNextPeriod.setDhesp_lag1(DecisionParams.DEFAULT_HEALTH);
            personProxyNextPeriod.setDcpyy_lag1(DecisionParams.DEFAULT_YEARS_MARRIED);
            personProxyNextPeriod.setDcpagdf_lag1(DecisionParams.DEFAULT_AGE_DIFFERENCE);
        }
    }


    /*
     * WORKER METHODS
     */

    /**
     * METHOD TO UPDATE EXPECTATIONS FOR DISCRETE CONTROL VARIABLES
     * @param emp1Pr proportion of time reference adult spends in employment
     * @param emp2Pr proportion of time spouse spends in employment
     */
    public void updateForDiscreteControls(double emp1Pr, double emp2Pr) {

        // working variables
        int stateIndexNextPeriod, stateIndexCurrPeriod;


        //********************************************************
        // update current period variables for discrete decisions
        //********************************************************

        // care hours per week
        double careHoursProvidedWeekly = 0.0;
        int careProvision = 0;
        if (Parameters.flagSocialCare) {
            careHoursProvidedWeekly = evalSocialCareHoursProvidedWeekly();
            if (careHoursProvidedWeekly>1.0E-5)
                careProvision = 1;
        }

        // labour and labour income in current period - to evaluate taxes and benefits
        double labourIncome1Weekly = 0.0;
        double labourIncome2Weekly = 0.0;
        double hourlyWageRate1, hourlyWageRate2;
        Integer labourHours1Weekly = null;
        Integer labourHours2Weekly = null;
        leisureTime = 1.0;
        if (careHoursProvidedWeekly>0.0) {
            if (cohabitation)
                leisureTime -= careHoursProvidedWeekly / (2.0*Parameters.HOURS_IN_WEEK);
            else
                leisureTime -= careHoursProvidedWeekly / Parameters.HOURS_IN_WEEK;
        }
        if (ageYearsThisPeriod <= DecisionParams.maxAgeFlexibleLabourSupply) {
            labourHours1Weekly = (int) Math.round(DecisionParams.FULLTIME_HOURS_WEEKLY * emp1Pr);
            hourlyWageRate1 = getHourlyWageRate(labourHours1Weekly);
            labourIncome1Weekly = hourlyWageRate1 * (double)labourHours1Weekly;
            if (cohabitation) {
                labourHours2Weekly = (int) Math.round(DecisionParams.FULLTIME_HOURS_WEEKLY * emp2Pr);
                hourlyWageRate2 = getHourlyWageRate(labourHours2Weekly);
                labourIncome2Weekly = hourlyWageRate2 * (double)labourHours2Weekly;
                leisureTime -= ((double)(labourHours1Weekly + labourHours2Weekly)) / (2.0 * Parameters.HOURS_IN_WEEK);
            } else {
                leisureTime -= ((double)labourHours1Weekly) / Parameters.HOURS_IN_WEEK;
            }
        } else if (emp1Pr>1.0E-5 || emp2Pr>1.0E-5) {
            throw new InvalidParameterException("inconsistent labour decisions supplied for updating expectations");
        }
        leisureTime = Math.max(1.0/Parameters.HOURS_IN_WEEK, leisureTime);

        // pension income
        boolean retiring = false;
        double pensionIncome1Annual, pensionIncome2Annual;
        if (DecisionParams.flagPrivatePension && ageYearsThisPeriod>=DecisionParams.minAgeToRetire) {
            // allow for pension take-up (take-up in previous year accounted for at instantiation)
            if (!DecisionParams.flagRetirement && ageYearsThisPeriod == DecisionParams.minAgeToRetire) {
                retiring = true;
            } else if (DecisionParams.flagRetirement && currentStates.getRetirement()==0 && emp1Pr == 0 && liquidWealth > 0) {
                retiring = true;
            }
            if (retiring) {
                pensionIncomePerYear = liquidWealth * Parameters.SHARE_OF_WEALTH_TO_ANNUITISE_AT_RETIREMENT /
                        Parameters.annuityRates.getAnnuityRate(currentStates.getOccupancyCode(), currentStates.getBirthYear(), currentStates.getYear());
                liquidWealth *= (1.0 - Parameters.SHARE_OF_WEALTH_TO_ANNUITISE_AT_RETIREMENT);
            }
        }
        if (cohabitation) {
            pensionIncome1Annual = pensionIncomePerYear / 2;
            pensionIncome2Annual = pensionIncome1Annual;
        } else {
            pensionIncome1Annual = pensionIncomePerYear;
            pensionIncome2Annual = 0.0;
        }

        // investment income / cost of debt
        double investmentIncomeAnnual;
        double investmentIncome1Annual;
        double investmentIncome2Annual;
        if (liquidWealth < 0) {

            double wageFactor = 0.7 * fullTimeHourlyEarningsPotential * DecisionParams.FULLTIME_HOURS_WEEKLY * 52.0;
            if (cohabitation) {
                wageFactor *= 2.0;
            }
            double phi;
            if (wageFactor < 0.1) {
                phi = 1.0;
            } else {
                phi = - liquidWealth / wageFactor;
            }
            phi = Math.min(phi, 1.0);
            investmentIncomeAnnual = (DecisionParams.rDebtLow*(1.0-phi) + DecisionParams.rDebtHi*phi) * liquidWealth;
        } else {
            investmentIncomeAnnual = DecisionParams.rSafeAssets * liquidWealth;
        }
        if (cohabitation) {
            investmentIncome1Annual = investmentIncomeAnnual / 2;
            investmentIncome2Annual = investmentIncome1Annual;
        } else {
            investmentIncome1Annual = investmentIncomeAnnual;
            investmentIncome2Annual = 0.0;
        }

        // non-discretionary expenditure
        benefitUnitProxyThisPeriod.setLabourHoursWeekly1Local(labourHours1Weekly);
        if (cohabitation) {
            benefitUnitProxyThisPeriod.setLabourHoursWeekly2Local(labourHours2Weekly);
        } else {
            benefitUnitProxyThisPeriod.setLabourHoursWeekly2Local(null);
        }
        double childcareCostAnnual = evalChildcareCostWeekly() * Parameters.WEEKS_PER_YEAR;
        double socialCareCostAnnual = evalSocialCareCostWeekly() * Parameters.WEEKS_PER_YEAR;

        // disability
        int disability1 = 0, disability2;
        if (!Parameters.flagSuppressSocialCareCosts) {

            disability1 = currentStates.getDisability();
        }
        if (cohabitation) {
            disability2 = 0;
        } else {
            disability2 = -1;
        }

        // call to tax and benefit function
        disposableIncomeAnnual = taxBenefitFunction(labourHours1Weekly, disability1, labourIncome1Weekly, investmentIncome1Annual, pensionIncome1Annual,
                labourHours2Weekly, disability2, careProvision, labourIncome2Weekly, investmentIncome2Annual, pensionIncome2Annual, childcareCostAnnual,
                socialCareCostAnnual, liquidWealth);

        // cash on hand
        cashOnHand = liquidWealth + availableCredit + disposableIncomeAnnual - childcareCostAnnual - socialCareCostAnnual;


        //********************************************************
        // update expectations for succeeding period
        //********************************************************
        if (ageYearsNextPeriod <= DecisionParams.maxAge) {

            // update objects for interaction with regression models
            personProxyNextPeriod.setLes_c4_lag1(currentStates.getLesCode(emp1Pr));
            personProxyNextPeriod.setLesdf_c4_lag1(currentStates.getLesC4Code(emp1Pr, emp2Pr));
            personProxyNextPeriod.setYpnbihs_dv_lag1(
                    asinh(labourIncome1Weekly*Parameters.WEEKS_PER_MONTH + (investmentIncome1Annual + pensionIncome1Annual) / 12.0));
            if (cohabitation) {
                personProxyNextPeriod.setYnbcpdf_dv_lag1(
                        asinh(labourIncome1Weekly*Parameters.WEEKS_PER_MONTH + (investmentIncome1Annual + pensionIncome1Annual) / 12.0) -
                                asinh(labourIncome2Weekly*Parameters.WEEKS_PER_MONTH + (investmentIncome2Annual + pensionIncome2Annual) / 12.0) );
            } else {
                personProxyNextPeriod.setYnbcpdf_dv_lag1(0.0);
            }

            // instantiate expectations factory
            ExpectationsFactory futures = new ExpectationsFactory(anticipated, probability, personProxyNextPeriod, scale, ageYearsThisPeriod, currentStates, pensionIncomePerYear);

            // region
            if (DecisionParams.flagRegion) {
                futures.updateRegion();
                throw new RuntimeException("Please validate code for regions in expectations object");
            }

            // retirement - not a state included in personProxyNextPeriod (don't track changes)
            if (DecisionParams.flagRetirement && ageYearsNextPeriod > DecisionParams.minAgeToRetire && ageYearsNextPeriod <= DecisionParams.maxAgeFlexibleLabourSupply) {
                futures.updateRetirement(retiring);
                throw new RuntimeException("Please validate code for retirement in expectations object");
            }

            // student - don't need to track separately from education (no need for flagStudentVaries)
            if (DecisionParams.flagEducation && ageYearsNextPeriod<=Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION) {
                futures.updateStudent();
            }

            // education
            if (DecisionParams.flagEducation) {
                futures.updateEducation();
            }

            // health
            if (DecisionParams.flagHealth && ageYearsNextPeriod >= DecisionParams.minAgeForPoorHealth) {
                futures.updateHealth();
            }

            // disability
            if (DecisionParams.flagDisability  && ageYearsNextPeriod >= DecisionParams.minAgeForPoorHealth && ageYearsNextPeriod <= DecisionParams.maxAgeForDisability()) {
                futures.updateDisability();
            }

            // cohabitation (1 = cohabiting)
            if (ageYearsNextPeriod <= DecisionParams.MAX_AGE_COHABITATION) {
                futures.updateCohabitation();
            }

            // dependent children
            futures.updateChildren();

            // social care receipt
            if (Parameters.flagSocialCare  && ageYearsNextPeriod >= DecisionParams.minAgeReceiveFormalCare) {
                futures.updateSocialCareReceipt();
            }

            // social care provision
            if (Parameters.flagSocialCare) {
                futures.updateSocialCareProvision();
            }

            // full-time wage potential
            if (ageYearsNextPeriod <= DecisionParams.maxAgeFlexibleLabourSupply) {
                futures.updateWagePotential();
            }

            // pension income
            if (DecisionParams.flagPrivatePension && ageYearsNextPeriod > DecisionParams.minAgeToRetire) {
                futures.updatePensionIncome();
            }

            // wage offer
            if (ageYearsNextPeriod <= DecisionParams.maxAgeFlexibleLabourSupply && DecisionParams.flagLowWageOffer1) {
                futures.updateWageOffer1();
            }

            // retrieve results
            probability = futures.getProbability();
            anticipated = futures.getAnticipated();
            numberExpected = futures.getNumberExpected();

            // check evaluated probabilities
            double probabilityCheck = 0;
            for (int ii = 0; ii< numberExpected; ii++) {
                probabilityCheck += probability[ii];
            }
            if (Math.abs(probabilityCheck-1) > 1.0E-5) {
                throw new InvalidParameterException("problem with probabilities supplied to outer expectations 1");
            }
        }
    }

    private double evalChildcareCostWeekly() {

        double childcareCostWeekly = 0.0;
        if (Parameters.flagFormalChildcare && !Parameters.flagSuppressChildcareCosts && currentStates.hasChildrenEligibleForCare()) {

            double probFormalChildCare = Parameters.getRegChildcareC1a().getProbability(benefitUnitProxyThisPeriod, BenefitUnit.Regressors.class);
            double logChildcareCostScore = Parameters.getRegChildcareC1b().getScore(benefitUnitProxyThisPeriod, BenefitUnit.Regressors.class);
            double logChildcareRSME = ManagerRegressions.getRmse(RegressionName.ChildcareC1b);
            childcareCostWeekly = Math.exp(logChildcareCostScore + logChildcareRSME*logChildcareRSME/2.0) * probFormalChildCare;
        }
        return childcareCostWeekly;
    }

    private double evalSocialCareCostWeekly() {

        double socialCareCostWeekly = 0.0;
        if (Parameters.flagSocialCare && !Parameters.flagSuppressSocialCareCosts && (ageYearsThisPeriod>=DecisionParams.minAgeReceiveFormalCare)) {

            SocialCareReceiptState market = currentStates.getSocialCareReceiptStateCode();
            if (SocialCareReceiptState.Mixed.equals(market) || SocialCareReceiptState.Formal.equals(market)) {

                double score = Parameters.getRegFormalCareHoursS2k().getScore(personProxyThisPeriod,Person.DoublesVariables.class);
                double rmse = Parameters.getRMSEForRegression("S2k");
                double hours = Math.min(Parameters.MAX_HOURS_WEEKLY_FORMAL_CARE, Math.exp(score + rmse*rmse/2.0));
                socialCareCostWeekly = hours * Parameters.getTimeSeriesValue(currentStates.getYear(), TimeSeriesVariable.CarerWageRate);
            }
        }
        return socialCareCostWeekly;
    }

    private double evalSocialCareHoursProvidedWeekly() {

        double socialCareHoursProvidedWeekly = 0.0;
        if (Parameters.flagSocialCare && !Parameters.flagSuppressSocialCareCosts) {

            SocialCareProvision status = currentStates.getSocialCareProvisionCode();
            if (!SocialCareProvision.None.equals(status)) {

                double score = Parameters.getRegCareHoursProvS3e().getScore(personProxyThisPeriod,Person.DoublesVariables.class);
                double rmse = Parameters.getRMSEForRegression("S3e");
                socialCareHoursProvidedWeekly = Math.min(80.0, Math.exp(score + rmse*rmse/2.0));
            }
        }
        return socialCareHoursProvidedWeekly;
    }

    /**
     * METHOD TO CALL TO TAX AND BENEFIT FUNCTION
     * @return disposable income per annum of benefitUnit
     *
     * NOTE: ALL FINANCIALS ARE DEFINED HERE IN PRICES OF ASSUMED BASE YEAR (Parameters.BASE_PRICE_YEAR)
     */
    public double taxBenefitFunction(Integer labourHours1Weekly,
                                     Integer disability1,
                                     double labourIncome1Weekly,
                                     double investmentIncome1Annual,
                                     double pensionIncome1Annual,
                                     Integer labourHours2Weekly,
                                     Integer disability2,
                                     Integer careProvision,
                                     double labourIncome2Weekly,
                                     double investmentIncome2Annual,
                                     double pensionIncome2Annual,
                                     double childcareCostAnnual,
                                     double socialCareCostAnnual,
                                     double liquidWealth) {

        // prepare characteristics
        int year = currentStates.getYear();
        int numberAdults = (currentStates.getCohabitation()) ? 2 : 1;
        int numberChildrenUnder5 = currentStates.getChildren04();
        int numberChildrenAged5To9 = currentStates.getChildren59();
        int numberChildrenAged10To17 = currentStates.getChildren1017();
        double originalIncome1Weekly = labourIncome1Weekly + (investmentIncome1Annual + pensionIncome1Annual) / Parameters.WEEKS_PER_YEAR;
        double originalIncome2Weekly = labourIncome2Weekly + (investmentIncome2Annual + pensionIncome2Annual) / Parameters.WEEKS_PER_YEAR;
        double originalIncomePerWeek = originalIncome1Weekly + originalIncome2Weekly;
        double secondIncomePerMonth = 0.0;
        if (originalIncome1Weekly>0.01 && originalIncome2Weekly>0.01)
            secondIncomePerMonth = Math.min(originalIncome1Weekly, originalIncome2Weekly) * Parameters.WEEKS_PER_MONTH;
        double hoursWorkPerWeek1, hoursWorkPerWeek2;
        if (labourHours1Weekly != null) {
            hoursWorkPerWeek1 = (double) labourHours1Weekly;
        } else {
            hoursWorkPerWeek1 = 0.0;
        }
        if (labourHours2Weekly != null) {
            hoursWorkPerWeek2 = (double) labourHours2Weekly;
        } else {
            hoursWorkPerWeek2 = 0.0;
        }
        double childcareCostPerMonth = childcareCostAnnual / 12.0;
        double socialCareCostPerMonth = socialCareCostAnnual / 12.0;

        // evaluate disposable income
        double originalIncomePerMonth = originalIncomePerWeek * Parameters.WEEKS_PER_MONTH;
        TaxEvaluation evaluatedTransfers = new TaxEvaluation(year, ageYearsNextPeriod, numberAdults, numberChildrenUnder5, numberChildrenAged5To9,
                numberChildrenAged10To17, hoursWorkPerWeek1, hoursWorkPerWeek2, disability1, disability2, careProvision, originalIncomePerMonth, secondIncomePerMonth,
                childcareCostPerMonth, socialCareCostPerMonth, liquidWealth, -1.0);

        Match match = evaluatedTransfers.getMatch();
        if (match.getMatchCriterion()>Parameters.IMPERFECT_THRESHOLD) {
            imperfectMatches.addMatch(match);
        }

        // finalise outputs
        disposableIncomeAnnual = evaluatedTransfers.getDisposableIncomePerMonth() * 12.0;
        return disposableIncomeAnnual;
    }

    private double getHourlyWageRate(int labourHoursWeekly) {

        if (labourHoursWeekly >= Parameters.MIN_HOURS_FULL_TIME_EMPLOYED) {
            return fullTimeHourlyEarningsPotential;
        } else {
            double ptPremium;
            if (currentStates.getGenderCode()==Gender.Male) {
                ptPremium = ManagerRegressions.getRegressionCoeff(RegressionName.WagesMalesE, "Pt");
            } else {
                ptPremium = ManagerRegressions.getRegressionCoeff(RegressionName.WagesFemalesE, "Pt");
            }
            return Math.exp( Math.log(fullTimeHourlyEarningsPotential) + ptPremium);
        }
    }
}
