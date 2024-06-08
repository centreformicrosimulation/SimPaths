package simpaths.model.decisions;

import java.security.InvalidParameterException;
import java.util.*;

import simpaths.data.ManagerRegressions;
import simpaths.data.Parameters;
import simpaths.data.RegressionNames;
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


    // flags to indicate expectations variation
    boolean flagRegionVaries=false, flagEducationVaries=false, flagHealthVaries=false, flagDisabilityVaries=false, flagSocialCareReceiptVaries=false;
    boolean flagSocialCareProvisionVaries=false, flagCohabitationVaries=false, flagChildrenVaries=false, flagWageVaries=false, flagUnemploymentVaries = false;


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
        probability[0] = 1;
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
        benefitUnitProxyThisPeriod.setOccupancy(currentStates.getOccupancyCode());
        benefitUnitProxyThisPeriod.setDeh_c3Local(currentStates.getEducationCode());
        benefitUnitProxyThisPeriod.setRegion(currentStates.getRegionCode());
        for (int ii=0; ii<18; ii++) {
            benefitUnitProxyThisPeriod.setNumberChildrenByAge(ii, currentStates.getChildrenByAge(ii));
        }
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
        personProxyThisPeriod.setDcpst(currentStates.getDcpst());
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
            personProxyNextPeriod.setDcpst(currentStates.getDcpst());
        } else {
            if (currentStates.getDcpst().equals(Dcpst.Partnered))
                personProxyNextPeriod.setDcpst(Dcpst.PreviouslyPartnered);
            else
                personProxyNextPeriod.setDcpst(Dcpst.SingleNeverMarried);
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
        int disability1 = currentStates.getDisability(), disability2 = -1;
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

            // region
            if (DecisionParams.flagRegion) {
                // assume that expect to remain in current region as placeholder

                stateIndexCurrPeriod = scale.getIndex(Axis.Region, ageYearsThisPeriod);
                stateIndexNextPeriod = scale.getIndex(Axis.Region, ageYearsNextPeriod);
                for (int ii = 0; ii < numberExpected; ii++) {
                    anticipated[ii].states[stateIndexNextPeriod] = currentStates.states[stateIndexCurrPeriod];
                }
                personProxyNextPeriod.setRegionLocal(currentStates.getRegionCode());
                throw new RuntimeException("Please validate code for regions in expectations object");
            }

            // retirement - not a state included in personProxyNextPeriod (don't track changes)
            if (DecisionParams.flagRetirement && ageYearsNextPeriod > DecisionParams.minAgeToRetire && ageYearsNextPeriod <= DecisionParams.maxAgeFlexibleLabourSupply) {
                stateIndexNextPeriod = scale.getIndex(Axis.Retirement, ageYearsNextPeriod);
                if (retiring) {
                    // retire this period
                    for (int ii = 0; ii < numberExpected; ii++) {
                        anticipated[ii].states[stateIndexNextPeriod] = 1.0;
                    }
                } else {
                    // no change to retirement state
                    for (int ii = 0; ii < numberExpected; ii++) {
                        anticipated[ii].states[stateIndexNextPeriod] = currentStates.getRetirement();
                    }
                }
                throw new RuntimeException("Please validate code for retirement in expectations object");
            }

            // student - don't need to track separately from education (no need for flagStudentVaries)
            if (DecisionParams.flagEducation && ageYearsNextPeriod<=Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION) {
                stateIndexCurrPeriod = scale.getIndex(Axis.Student, ageYearsThisPeriod);
                stateIndexNextPeriod = scale.getIndex(Axis.Student, ageYearsNextPeriod);
                LocalExpectations lexpect;
                if (anyVaries() && currentStates.getStudent()==1) {
                    int numberExpectedInitial = numberExpected;
                    boolean flagEval;
                    lexpect = new LocalExpectations(personProxyNextPeriod, RegressionNames.EducationE1a);
                    for (int ii=0; ii<numberExpectedInitial; ii++) {

                        flagEval = updatePersonNextPeriod(ii);
                        if (flagEval) {
                            lexpect = new LocalExpectations(personProxyNextPeriod, RegressionNames.EducationE1a);
                        }
                        expandExpectationsSingleIndex(ii, stateIndexNextPeriod, lexpect);
                    }
                } else {
                    if (currentStates.getStudent() == 0) {
                        lexpect = new LocalExpectations(currentStates.states[stateIndexCurrPeriod]);
                    } else {
                        lexpect = new LocalExpectations(personProxyNextPeriod, RegressionNames.EducationE1a);
                    }
                    expandExpectationsAllIndices(stateIndexNextPeriod, lexpect);
                }
                if (currentStates.getStudent()==1)
                    flagEducationVaries = true;
            }

            // education
            if (DecisionParams.flagEducation) {
                stateIndexCurrPeriod = scale.getIndex(Axis.Education, ageYearsThisPeriod);
                stateIndexNextPeriod = scale.getIndex(Axis.Education, ageYearsNextPeriod);
                if (!flagEducationVaries) {
                    // no change in education state possible

                    LocalExpectations lexpect = new LocalExpectations(currentStates.states[stateIndexCurrPeriod]);
                    expandExpectationsAllIndices(stateIndexNextPeriod, lexpect);
                } else {
                    // allow for change in education state

                    int numberExpectedInitial = numberExpected;
                    boolean flagEval = false;
                    LocalExpectations lexpect = new LocalExpectations(personProxyNextPeriod, RegressionNames.EducationE2a);
                    for (int ii = 0; ii < numberExpectedInitial; ii++) {

                        if (anyVaries()) {
                            flagEval = updatePersonNextPeriod(ii);
                        }
                        if (flagEval) {
                            lexpect = new LocalExpectations(personProxyNextPeriod, RegressionNames.EducationE2a);
                        }

                        if (anticipated[ii].getStudent() == 1) {
                            // continuing student
                            anticipated[ii].states[stateIndexNextPeriod] = currentStates.states[stateIndexCurrPeriod];
                        } else {
                            // allow for exit from education
                            expandExpectationsSingleIndex(ii, stateIndexNextPeriod, lexpect);
                        }
                    }
                }
            }

            // health
            if (DecisionParams.flagHealth && ageYearsNextPeriod >= DecisionParams.minAgeForPoorHealth) {
                updateExpectations(Axis.Health, RegressionNames.HealthH1b);
                flagHealthVaries = true;
            }

            // disability
            if (DecisionParams.flagDisability  && ageYearsNextPeriod >= DecisionParams.minAgeForPoorHealth && ageYearsNextPeriod <= DecisionParams.maxAgeForDisability()) {
                updateExpectations(Axis.Disability, RegressionNames.HealthH2b);
                flagDisabilityVaries = true;
            }

            // cohabitation (1 = cohabiting)
            if (ageYearsNextPeriod <= DecisionParams.MAX_AGE_COHABITATION) {

                if (cohabitation) {
                    updateExpectations(Axis.Cohabitation, RegressionNames.PartnershipU2b, 0.0);
                } else {
                    updateExpectations(Axis.Cohabitation, RegressionNames.PartnershipU1b, RegressionNames.PartnershipU1a);
                }
                flagCohabitationVaries = true;
            }

            // dependent children
            for (int jj = 0; jj < DecisionParams.NUMBER_BIRTH_AGES; jj++) {
                // loop over each birth age

                if (ageYearsNextPeriod >= DecisionParams.BIRTH_AGE[jj] && ageYearsNextPeriod < (DecisionParams.BIRTH_AGE[jj] + Parameters.AGE_TO_BECOME_RESPONSIBLE)) {
                    // may have children from this age in next period

                    if (ageYearsNextPeriod == DecisionParams.BIRTH_AGE[jj]) {
                        // next year is birth age - number of children uncertain

                        stateIndexNextPeriod = scale.getIndex(Axis.Child, ageYearsNextPeriod, jj);
                        int options = (int)scale.axes[ageIndexNextPeriod][stateIndexNextPeriod][0];

                        // begin loop over existing expectations
                        int numberExpectedInitial = numberExpected;
                        for (int ii=0; ii<numberExpectedInitial; ii++) {

                            // update person characteristics
                            if (anyVaries()) {
                                updatePersonNextPeriod(ii);
                            }

                            // expand expectations
                            if (Gender.Female == currentStates.getGenderCode() || anticipated[ii].getCohabitation()) {
                                // birth possible

                                if (anticipated[ii].getStudent()==1) {
                                    expandExpectationsFertility(ii, stateIndexNextPeriod, jj, options, RegressionNames.FertilityF1a);
                                } else {
                                    expandExpectationsFertility(ii, stateIndexNextPeriod, jj, options, RegressionNames.FertilityF1b);
                                }
                            } else {
                                // birth not possible

                                stateIndexNextPeriod = scale.getIndex(Axis.Child, ageYearsNextPeriod, jj);
                                for (int kk = 0; kk< numberExpected; kk++) {
                                    anticipated[kk].states[stateIndexNextPeriod] = 0.0;
                                }
                            }
                        }
                        flagChildrenVaries = true;
                    } else {
                        // assume next year have same number of children as this year

                        stateIndexCurrPeriod = scale.getIndex(Axis.Child, ageYearsThisPeriod, jj);
                        stateIndexNextPeriod = scale.getIndex(Axis.Child, ageYearsNextPeriod, jj);
                        LocalExpectations lexpect = new LocalExpectations(currentStates.states[stateIndexCurrPeriod]);
                        expandExpectationsAllIndices(stateIndexNextPeriod, lexpect);
                    }
                }
            }

            // social care receipt
            if (Parameters.flagSocialCare  && ageYearsNextPeriod >= DecisionParams.minAgeReceiveFormalCare) {
                updateExpectations(Axis.SocialCareReceiptState, 4);
                flagSocialCareReceiptVaries = true;
            }

            // social care provision
            if (Parameters.flagSocialCare) {
                updateExpectations(Axis.SocialCareProvision, RegressionNames.SocialCareS3c, RegressionNames.SocialCareS3d, 5);
                flagSocialCareProvisionVaries = true;
            }

            // full-time wage potential
            if (ageYearsNextPeriod <= DecisionParams.maxAgeFlexibleLabourSupply) {
                double minValue = Math.log(DecisionParams.MIN_WAGE_PHOUR);
                double maxValue = Math.log(DecisionParams.MAX_WAGE_PHOUR);
                if (Gender.Male.equals(currentStates.getGenderCode()))
                    updateExpectations(Axis.WagePotential, RegressionNames.WagesMalesE, minValue, maxValue, DecisionParams.C_WAGE_POTENTIAL);
                else
                    updateExpectations(Axis.WagePotential, RegressionNames.WagesFemalesE, minValue, maxValue, DecisionParams.C_WAGE_POTENTIAL);
                flagWageVaries = true;
            }

            // pension income
            if (DecisionParams.flagPrivatePension && ageYearsNextPeriod > DecisionParams.minAgeToRetire) {
                stateIndexNextPeriod = scale.getIndex(Axis.PensionIncome, ageYearsNextPeriod);
                int numberExpectedInitial = numberExpected;
                double val;
                for (int ii=0; ii<numberExpectedInitial; ii++) {
                    val = pensionIncomePerYear;
                    if (cohabitation && !anticipated[ii].getCohabitation()) {
                        val /= 2.0;
                    } else if (!cohabitation && anticipated[ii].getCohabitation()) {
                        val *= 2.0;
                    }
                    val = Math.min( Math.max( val, 0.0 ), DecisionParams.maxPensionPYear );
                    val = Math.log(val + DecisionParams.C_PENSION);
                    anticipated[ii].states[stateIndexNextPeriod] = val;
                }
            }

            // wage offer
            if (ageYearsNextPeriod <= DecisionParams.maxAgeFlexibleLabourSupply && DecisionParams.flagLowWageOffer1) {
                updateExpectations(Axis.WageOffer1, getUnemploymentRegressionName(), 0.0);
                flagUnemploymentVaries = true;
            }

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

    private RegressionNames getUnemploymentRegressionName() {
        if (currentStates.getGenderCode().equals(Gender.Male)) {
            if (currentStates.getEducationCode().equals(Education.High)) {
                return RegressionNames.UnemploymentU1a;
            } else {
                return RegressionNames.UnemploymentU1b;
            }
        } else {
            if (currentStates.getEducationCode().equals(Education.High)) {
                return RegressionNames.UnemploymentU1c;
            } else {
                return RegressionNames.UnemploymentU1d;
            }
        }
    }

    private double evalChildcareCostWeekly() {

        double childcareCostWeekly = 0.0;
        if (Parameters.flagFormalChildcare && !Parameters.flagSuppressCareCosts && currentStates.hasChildrenEligibleForCare()) {

            double probFormalChildCare = Parameters.getRegChildcareC1a().getProbability(benefitUnitProxyThisPeriod, BenefitUnit.Regressors.class);
            double logChildcareCostScore = Parameters.getRegChildcareC1b().getScore(benefitUnitProxyThisPeriod, BenefitUnit.Regressors.class);
            double logChildcareRSME = ManagerRegressions.getRmse(RegressionNames.ChildcareC1b);
            childcareCostWeekly = Math.exp(logChildcareCostScore + logChildcareRSME*logChildcareRSME/2.0) * probFormalChildCare;
        }
        return childcareCostWeekly;
    }

    private double evalSocialCareCostWeekly() {

        double socialCareCostWeekly = 0.0;
        if (Parameters.flagSocialCare && !Parameters.flagSuppressCareCosts && (ageYearsThisPeriod>=DecisionParams.minAgeReceiveFormalCare)) {

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
        if (Parameters.flagSocialCare && !Parameters.flagSuppressCareCosts) {

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

    private <E extends Enum<E> & DoubleValuedEnum> void expandExpectationsSingleIndex(int expandIndex, int stateIndex, Map<E, Double> probs) {

        double[] probabilities = new double[probs.size()];
        double[] values = new double[probs.size()];
        int ii = 0;
        for (E key : probs.keySet()) {
            probabilities[ii] = probs.get(key);
            values[ii] = key.getValue();
            ii++;
        }
        expandExpectationsSingleIndex(expandIndex, stateIndex, probabilities, values);
    }

    private void expandExpectationsSingleIndex(int expandIndex, int stateIndex, LocalExpectations lexpect) {
        expandExpectationsSingleIndex(expandIndex, stateIndex, lexpect.probabilities, lexpect.values);
    }

    private void expandExpectationsSingleIndex(int expandIndex, int stateIndex, double[] probabilities, double[] values) {

        // expand expectations array
        if (probabilities.length > 1) {
            probability = Arrays.copyOf(probability, numberExpected + probabilities.length - 1);
            anticipated = Arrays.copyOf(anticipated, numberExpected + probabilities.length - 1);
            for (int ii=0; ii<probabilities.length-1; ii++) {
                probability[numberExpected +ii] = probability[expandIndex];
                anticipated[numberExpected +ii] = new States(anticipated[expandIndex]);
            }
        }

        // update expectations arrays
        double probabilityCheck = 0.0;
        for (int ii=probabilities.length-1; ii>=0; ii--) {
            probabilityCheck += probabilities[ii];
            if (ii>0) {
                probability[numberExpected - 1 + ii] = probability[numberExpected - 1 + ii] * probabilities[ii];
                anticipated[numberExpected - 1 + ii].states[stateIndex] = values[ii];
            } else {
                probability[expandIndex] = probability[expandIndex] * probabilities[ii];
                anticipated[expandIndex].states[stateIndex] = values[ii];
            }
        }

        // check supplied probabilities
        if (Math.abs(probabilityCheck-1) > 1.0E-5) {
            throw new InvalidParameterException("problem with probabilities supplied to outer expectations 2");
        }

        // update indices
        numberExpected = numberExpected + probabilities.length - 1;
    }

    /**
     * METHOD TO EXPAND EXPECTATIONS ARRAYS TO ALLOW FOR FERTILITY BIRTH YEARS
     * @param expandIndex the index of the anticipated array taken as a starting point
     * @param stateIndex the state index for the respective birth year
     * @param birthYear the current birth year (e.g. 0, 1 or 2)
     * @param options the number of potential alternatives at birth age (= max no. births + 1)
     * @param regression the regression equation used to update probabilities
     */
    private void expandExpectationsFertility(int expandIndex, int stateIndex, int birthYear, int options, RegressionNames regression) {

        // initialise storage arrays - 100% probability to zero children at birth year
        double[] probabilities = new double[options];
        double[] values = new double[options];
        for (int ii=0; ii<options; ii++) {
            if (ii==0) {
                probabilities[ii] = 1.0;
            } else {
                probabilities[ii] = 0.0;
            }
            values[ii] = ii;
        }

        // identify age pool for birth year
        int[] ageVector = currentStates.getFertilityAgeBand(birthYear);
        int age0 = ageVector[0];
        int age1 = ageVector[1];

        // evaluate probabilities
        int childrenAll = currentStates.getChildrenAll();
        int children02 = currentStates.getChildren02();

        // loop over age pool for birth year
        for (int age=age0; age<=age1; age++) {

            personProxyNextPeriod.setDag(age);
            // at each age in the pool, data for n+1 births are a flow from n births
            // loop consequently works in reverse order through number of births, starting
            // at the pen-ultimate group (as flows from upper bound are ignored)
            for (int ii=options-2; ii>=0; ii--) {

                // ii = number of previous births for this birth age
                int birthsHere02 = Math.min(ii + children02, 2);  // assume at most 2 children under 3
                personProxyNextPeriod.setNumberChildrenAllLocal_lag1(childrenAll + ii);
                personProxyNextPeriod.setNumberChildren02Local_lag1(birthsHere02);
                double proportionBirths = ManagerRegressions.getProbability(personProxyNextPeriod, regression);
                probabilities[ii+1] += probabilities[ii] * proportionBirths;
                probabilities[ii] *= (1 - proportionBirths);
            }
        }

        // expand expectations array
        expandExpectationsSingleIndex(expandIndex, stateIndex, probabilities, values);

        // restore benefitUnit and person characteristics
        personProxyNextPeriod.setDag(ageYearsNextPeriod);
        personProxyNextPeriod.setNumberChildrenAllLocal_lag1(childrenAll);
        personProxyNextPeriod.setNumberChildren02Local_lag1(children02);
    }

    private <E extends Enum<E> & DoubleValuedEnum> void expandExpectationsAllIndices(int stateIndex, Map<E, Double> probs) {

        double[] probabilities = new double[probs.size()];
        double[] values = new double[probs.size()];
        int ii = 0;
        for (E key : probs.keySet()) {
            probabilities[ii] = probs.get(key);
            values[ii] = key.getValue();
            ii++;
        }
        expandExpectationsAllIndices(stateIndex, probabilities, values);
    }

    private void expandExpectationsAllIndices(int stateIndex, LocalExpectations lexpect) {
        int numberExpectedInitial = numberExpected;
        for (int ii=0; ii<numberExpectedInitial; ii++) {
            expandExpectationsSingleIndex(ii, stateIndex, lexpect);
        }
    }

    private void expandExpectationsAllIndices(int stateIndex, double[] probabilities, double[] values) {
        int numberExpectedInitial = numberExpected;
        for (int ii=0; ii<numberExpectedInitial; ii++) {
            expandExpectationsSingleIndex(ii, stateIndex, probabilities, values);
        }
    }

    private double getHourlyWageRate(int labourHoursWeekly) {

        if (labourHoursWeekly >= Parameters.MIN_HOURS_FULL_TIME_EMPLOYED) {
            return fullTimeHourlyEarningsPotential;
        } else {
            double ptPremium;
            if (currentStates.getGenderCode()==Gender.Male) {
                ptPremium = ManagerRegressions.getRegressionCoeff(RegressionNames.WagesMalesE, "Pt");
            } else {
                ptPremium = ManagerRegressions.getRegressionCoeff(RegressionNames.WagesFemalesE, "Pt");
            }
            return Math.exp( Math.log(fullTimeHourlyEarningsPotential) + ptPremium);
        }
    }

    private boolean updatePersonNextPeriod(int ii) {

        boolean flagEval, flagChange;
        flagEval = false;
        if (flagRegionVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Region);
            if (flagChange) flagEval = true;
        }
        if (flagEducationVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Education);
            if (flagChange) flagEval = true;
        }
        if (flagHealthVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Health);
            if (flagChange) flagEval = true;
        }
        if (flagDisabilityVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Disability);
            if (flagChange) flagEval = true;
        }
        if (flagSocialCareReceiptVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.SocialCareReceiptState);
            if (flagChange) flagEval = true;
        }
        if (flagSocialCareProvisionVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.SocialCareProvision);
            if (flagChange) flagEval = true;
        }
        if (flagCohabitationVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Cohabitation);
            if (flagChange) flagEval = true;
        }
        if (flagChildrenVaries) {
            flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Child);
            if (flagChange) flagEval = true;
        }
        return flagEval;
    }

    private boolean updatePersonNextPeriod(States states, Axis axis) {
        boolean changed = false;
        Object val0, val1;
        if (Axis.Region.equals(axis)) {
            val0 = personProxyNextPeriod.getRegion();
            val1 = states.getRegionCode();
        } else if (Axis.Education.equals(axis)) {
            val0 = personProxyNextPeriod.getDeh_c3();
            val1 = states.getEducationCode();
            if (val0==val1) {
                val0 = personProxyNextPeriod.getDed();
                val1 = states.getStudentIndicator();
            }
        } else if (Axis.Health.equals(axis)) {
            val0 = personProxyNextPeriod.getDhe();
            val1 = states.getHealthCode();
        } else if (Axis.Disability.equals(axis)) {
            val0 = personProxyNextPeriod.getDlltsd();
            val1 = states.getDlltsd();
        } else if (Axis.SocialCareReceiptState.equals(axis)) {
            val0 = personProxyNextPeriod.getSocialCareReceipt();
            val1 = states.getSocialCareReceiptCode();
        } else if (Axis.SocialCareProvision.equals(axis)) {
            val0 = personProxyNextPeriod.getSocialCareProvision();
            val1 = states.getSocialCareProvisionCode();
        } else if (Axis.Cohabitation.equals(axis)) {
            val0 = personProxyNextPeriod.getDcpst();
            val1 = states.getDcpst();
        } else if (Axis.Child.equals(axis)) {
            val0 = personProxyNextPeriod.getNumberChildren017Local();
            val1 = states.getChildren017();
            if (val0==val1) {
                val0 = personProxyNextPeriod.getIndicatorChildren02Local();
                val1 = states.getChildrenUnder3Indicator();
            }
            if (val0==val1) {
                val0 = personProxyNextPeriod.getNumberChildrenAllLocal();
                val1 = states.getChildren017();
            }
        } else {
            throw new RuntimeException("unrecognised axis for considering change in person proxy states");
        }
        if (val0!=val1) {
            changed = true;

            if (Axis.Region.equals(axis)) {
                personProxyNextPeriod.setRegion(states.getRegionCode());
            } else if (Axis.Education.equals(axis)) {
                personProxyNextPeriod.setDeh_c3(states.getEducationCode());
                personProxyNextPeriod.setDed(states.getStudentIndicator());
            } else if (Axis.Health.equals(axis)) {
                personProxyNextPeriod.setDhe(states.getHealthCode());
            } else if (Axis.Disability.equals(axis)) {
                personProxyNextPeriod.setDlltsd(states.getDlltsd());
            } else if (Axis.SocialCareReceiptState.equals(axis)) {
                personProxyNextPeriod.setSocialCareReceipt(states.getSocialCareReceiptCode());
            } else if (Axis.SocialCareProvision.equals(axis)) {
                personProxyNextPeriod.setSocialCareProvision(states.getSocialCareProvisionCode());
            } else if (Axis.Cohabitation.equals(axis)) {
                personProxyNextPeriod.setDcpst(states.getDcpst());
            } else if (Axis.Child.equals(axis)) {
                personProxyNextPeriod.setNumberChildren017Local(states.getChildren017());
                personProxyNextPeriod.setIndicatorChildren02Local(states.getChildrenUnder3Indicator());
                personProxyNextPeriod.setNumberChildrenAllLocal(states.getChildren017());
            }
        }
        return changed;
    }

    private boolean anyVaries() {
        if ( flagRegionVaries || flagEducationVaries || flagHealthVaries || flagDisabilityVaries || flagSocialCareReceiptVaries ||
                flagSocialCareProvisionVaries || flagCohabitationVaries || flagChildrenVaries )
            return true;
        else
            return false;
    }


    private void updateExpectations(Axis axis, RegressionNames regressionName) {
        updateExpectations(axis, regressionName, null, 1.0, null, null, null, 0);
    }

    private void updateExpectations(Axis axis, RegressionNames regressionName, Double valueTrue) {
        updateExpectations(axis, regressionName, null, valueTrue, null, null, null, 1);
    }

    private void updateExpectations(Axis axis, RegressionNames regressionName1, RegressionNames regressionName2) {
        updateExpectations(axis, regressionName1, regressionName2, 1.0, null, null, null, 2);
    }

    private void updateExpectations(Axis axis, RegressionNames regressionName1, RegressionNames regressionName2, int method) {
        updateExpectations(axis, regressionName1, regressionName2, 3.0, null, null, null, method);
    }

    private void updateExpectations(Axis axis, RegressionNames regressionName, double minValue, double maxValue, double cTransform) {
        updateExpectations(axis, regressionName, null, 1.0, minValue, maxValue, cTransform, 3);
    }

    private void updateExpectations(Axis axis, int method) {
        updateExpectations(axis, null, null, 1.0, null, null, null, method);
    }

    private void updateExpectations(Axis axis, RegressionNames regressionName1, RegressionNames regressionName2,
                                    Double valueTrue, Double minValue, Double maxValue, Double cTransform,
                                    int method) {

        // check consistency of method and inputs
        checkParameterConsistency(regressionName1, regressionName2, valueTrue, minValue, maxValue, cTransform, method);

        // state indices
        int stateIndexNextPeriod = scale.getIndex(axis, ageYearsNextPeriod);

        // populate expectations
        LocalExpectations lexpect = null;
        if (anyVaries()) {
            boolean flagEval;
            int numberExpectedInitial = numberExpected;
            for (int ii=0; ii<numberExpectedInitial; ii++) {

                flagEval = updatePersonNextPeriod(ii);
                if (flagEval || lexpect==null) {
                    lexpect = lexpectEval(regressionName1, regressionName2, valueTrue, minValue, maxValue, cTransform, method);
                }
                expandExpectationsSingleIndex(ii, stateIndexNextPeriod, lexpect);
            }
        } else {
            lexpect = lexpectEval(regressionName1, regressionName2, valueTrue, minValue, maxValue, cTransform, method);
            expandExpectationsAllIndices(stateIndexNextPeriod, lexpect);
        }
    }

    private void checkParameterConsistency(RegressionNames regressionName1, RegressionNames regressionName2, Double valueTrue, Double minValue,
                                           Double maxValue, Double cTransform, int method) {
        if (method==0) {
            if (regressionName1==null || regressionName2!=null || (Math.abs(valueTrue-1.0)>1.0E-5) || minValue!=null || maxValue!=null || cTransform!=null )
                throw new RuntimeException("updateExpectations method (0) inconsistent with supplied inputs");
        } else if (method==1) {
            if (regressionName1==null || regressionName2!=null || (Math.abs(valueTrue)>1.0E-5) || minValue!=null || maxValue!=null || cTransform!=null )
                throw new RuntimeException("updateExpectations method (1) inconsistent with supplied inputs");
        } else if (method==2) {
            if (regressionName1==null || regressionName2==null || (Math.abs(valueTrue-1.0)>1.0E-5) || minValue!=null || maxValue!=null || cTransform!=null )
                throw new RuntimeException("updateExpectations method (2) inconsistent with supplied inputs");
        } else if (method==3) {
            if (regressionName1==null || regressionName2!=null || (Math.abs(valueTrue-1.0)>1.0E-5) || minValue==null || maxValue==null || cTransform==null )
                throw new RuntimeException("updateExpectations method (3) inconsistent with supplied inputs");
        } else if (method==4) {
            if (regressionName1!=null || regressionName2!=null || (Math.abs(valueTrue-1.0)>1.0E-5) || minValue!=null || maxValue!=null || cTransform!=null )
                throw new RuntimeException("updateExpectations method (4) inconsistent with supplied inputs");
        } else if (method==5) {
            if (regressionName1==null || regressionName2==null || (Math.abs(valueTrue-3.0)>1.0E-5) || minValue!=null || maxValue!=null || cTransform!=null )
                throw new RuntimeException("updateExpectations method (5) inconsistent with supplied inputs");
        } else
            throw new RuntimeException("unrecognised method to update local expectations");
    }


    private LocalExpectations lexpectEval(RegressionNames regressionName1, RegressionNames regressionName2,
                                          Double valueTrue, Double minValue, Double maxValue, Double cTransform, int method) {
        // method = 0 default
        //          1 reverse polarity
        //          2 student/nonStudent regression names
        //          3 gaussian regression
        //          4 multi-level social care receipt
        //          5 probit singles / mlogit for couples

        if (method==0) {
            return new LocalExpectations(personProxyNextPeriod, regressionName1);
        } else if (method==1) {
            return new LocalExpectations(personProxyNextPeriod, regressionName1, valueTrue);
        } else if (method==2) {
            if (personProxyNextPeriod.getStudent()==0)
                return new LocalExpectations(personProxyNextPeriod, regressionName1);
            else
                return new LocalExpectations(personProxyNextPeriod, regressionName2);
        } else if (method==3) {
            return new LocalExpectations(personProxyNextPeriod, regressionName1, minValue, maxValue, cTransform);
        } else if (method==4) {
            return compileSocialCareReceiptProbs();
        } else if (method==5) {
            if (Dcpst.Partnered.equals(personProxyNextPeriod.getDcpst()))
                return new LocalExpectations(personProxyNextPeriod, regressionName2);
            else
                return new LocalExpectations(personProxyNextPeriod, regressionName1, valueTrue);
        } else
            throw new RuntimeException("unrecognised method to generate local expectations");
    }

    private LocalExpectations compileSocialCareReceiptProbs() {

        // raw inputs
        double probNeedCare = Parameters.getRegNeedCareS2a().getProbability(personProxyNextPeriod, Person.DoublesVariables.class);
        double probRecCare = Parameters.getRegReceiveCareS2b().getProbability(personProxyNextPeriod, Person.DoublesVariables.class);
        Map<SocialCareReceiptS2c,Double> probsCareFrom = Parameters.getRegSocialCareMarketS2c().getProbabilites(personProxyNextPeriod, Person.DoublesVariables.class);

        // compile and package outputs
        int ii = 0;
        double probHere, probCheck = 0.0;
        double[] probs = new double[SocialCareReceiptState.values().length];
        double[] vals = new double[SocialCareReceiptState.values().length];

        // no care needed
        probHere = 1.0 - probNeedCare;
        probs[ii] = probHere;
        vals[ii] = SocialCareReceiptState.NoneNeeded.getValue();
        probCheck += probHere;
        ii++;

        // no formal care
        probHere = probNeedCare * ((1.0 - probRecCare) + probRecCare * probsCareFrom.get(SocialCareReceiptS2c.Informal));
        probs[ii] = probHere;
        vals[ii] = SocialCareReceiptState.NoFormal.getValue();
        probCheck += probHere;
        ii++;

        // mixed care
        probHere = probNeedCare * probRecCare * probsCareFrom.get(SocialCareReceiptS2c.Mixed);
        probs[ii] = probHere;
        vals[ii] = SocialCareReceiptState.Mixed.getValue();
        probCheck += probHere;
        ii++;

        // formal care
        probHere = probNeedCare * probRecCare * probsCareFrom.get(SocialCareReceiptS2c.Formal);
        probs[ii] = probHere;
        vals[ii] = SocialCareReceiptState.Formal.getValue();
        probCheck += probHere;
        ii++;

        // check results
        if (Math.abs(probCheck-1.0)>1.0E-5)
            throw new RuntimeException("problem evaluating probabilities for social care receipt");

        // return
        return new LocalExpectations(probs, vals);
    }
}
