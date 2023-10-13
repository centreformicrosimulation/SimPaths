package simpaths.model.decisions;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import simpaths.data.ManagerRegressions;
import simpaths.data.Parameters;
import simpaths.data.RegressionNames;
import simpaths.model.enums.*;
import simpaths.model.BenefitUnit;
import simpaths.model.Person;
import simpaths.model.TaxEvaluation;


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
    int ageIndexNextPeriod;         // age index for state expectations
    int ageYearsNextPeriod;         // age in years for expectations
    int numberExpected;             // number of state combinations comprising expectations, conditional on survival
    double[] probability;           // vector recording probability associated with each anticipated state combination, conditional on survival
    States[] anticipated;           // vector to store anticipated state combinations

    // responses to controls
    double leisureTime;             // proportion of time spent in leisure
    double disposableIncomeAnnual;  // disposable income
    double cashOnHand;              // total value of pot that can be used to finance consumption within period


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
        ageIndexNextPeriod = currentStates.ageIndex + 1;
        ageYearsNextPeriod = currentStates.ageYears + 1;
        numberExpected = 1;
        probability = new double[numberExpected];
        anticipated = new States[numberExpected];
        probability[0] = 1;
        anticipated[0] = new States(scale, ageYearsNextPeriod);
        if (ageYearsNextPeriod <= DecisionParams.maxAge) {

            // birth year
            int stateIndexCurrPeriod = scale.getIndex(Axis.BirthYear, ageYearsNextPeriod - 1);
            int stateIndexNextPeriod = scale.getIndex(Axis.BirthYear, ageYearsNextPeriod);
            for (int ii = 0; ii < numberExpected; ii++) {
                anticipated[ii].states[stateIndexNextPeriod] = currentStates.states[stateIndexCurrPeriod];
            }

            //gender (1 = female)
            stateIndexCurrPeriod = scale.getIndex(Axis.Gender, ageYearsNextPeriod - 1);
            stateIndexNextPeriod = scale.getIndex(Axis.Gender, ageYearsNextPeriod);
            for (int ii = 0; ii < numberExpected; ii++) {
                anticipated[ii].states[stateIndexNextPeriod] = currentStates.states[stateIndexCurrPeriod];
            }

            // region (assume that expect to remain in current region)
            if (DecisionParams.flagRegion) {
                stateIndexCurrPeriod = scale.getIndex(Axis.Region, ageYearsNextPeriod - 1);
                stateIndexNextPeriod = scale.getIndex(Axis.Region, ageYearsNextPeriod);
                for (int ii = 0; ii < numberExpected; ii++) {
                    anticipated[ii].states[stateIndexNextPeriod] = currentStates.states[stateIndexCurrPeriod];
                }
            }
        }

        // proxy for next period expectations
        benefitUnitProxyThisPeriod = new BenefitUnit(true);
        benefitUnitProxyThisPeriod.setYearLocal(currentStates.getYear());
        benefitUnitProxyThisPeriod.setOccupancy(currentStates.getOccupancyCode());
        benefitUnitProxyThisPeriod.setDeh_c3Local(currentStates.getEducationCode());
        benefitUnitProxyThisPeriod.setRegion(currentStates.getRegionCode());
        for (int ii=0; ii<18; ii++) {
            benefitUnitProxyThisPeriod.setN_children_byAge(ii, currentStates.getChildrenByAge(ii));
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
        equivalenceScale = outerExpectations.equivalenceScale;
        ageIndexNextPeriod = outerExpectations.ageIndexNextPeriod;
        ageYearsNextPeriod = outerExpectations.ageYearsNextPeriod;
        numberExpected = outerExpectations.numberExpected;
        probability = outerExpectations.probability;
        anticipated = outerExpectations.anticipated;
        benefitUnitProxyThisPeriod = new BenefitUnit(outerExpectations.benefitUnitProxyThisPeriod, true);

        // prevailing characteristics - based on currentStates
        this.currentStates = currentStates;
        fullTimeHourlyEarningsPotential = DecisionParams.MIN_WAGE_PHOUR;
        if (currentStates.ageYears <= DecisionParams.maxAgeFlexibleLabourSupply) {
            int scaleIndex = scale.getIndex(Axis.WagePotential, currentStates.ageYears);
            fullTimeHourlyEarningsPotential = (Math.exp(currentStates.states[scaleIndex]) - DecisionParams.C_WAGE_POTENTIAL);
        }
        liquidWealth = Math.exp(currentStates.states[0]) - DecisionParams.C_LIQUID_WEALTH;
        if (currentStates.ageYears == DecisionParams.maxAge) {
            availableCredit = 0;
            mortalityProbability = 1;
        } else {
            availableCredit = - (Math.exp(scale.axes[ageIndexNextPeriod][0][1]) - DecisionParams.C_LIQUID_WEALTH);
            mortalityProbability = Parameters.getMortalityProbability(currentStates.getGenderCode(), currentStates.ageYears, currentStates.getYear());
        }
        pensionIncomePerYear = currentStates.getPensionPerYear();
    }

    /**
     * CONSTRUCTOR TO COPY AND EXPAND ON invariantExpectations
     * @param invariantExpectations set of expectations that are invariant to control variables
     */
    public Expectations(Expectations invariantExpectations) {

        // prevailing characteristics - based on currentStates
        currentStates = invariantExpectations.currentStates;
        scale = invariantExpectations.scale;
        cohabitation = invariantExpectations.cohabitation;
        equivalenceScale = invariantExpectations.equivalenceScale;
        fullTimeHourlyEarningsPotential = invariantExpectations.fullTimeHourlyEarningsPotential;
        liquidWealth = invariantExpectations.liquidWealth;
        pensionIncomePerYear = invariantExpectations.pensionIncomePerYear;
        availableCredit = invariantExpectations.availableCredit;
        mortalityProbability = invariantExpectations.mortalityProbability;
        benefitUnitProxyThisPeriod = new BenefitUnit(invariantExpectations.benefitUnitProxyThisPeriod, true);

        // prospective characteristics
        ageIndexNextPeriod = invariantExpectations.ageIndexNextPeriod;
        ageYearsNextPeriod = invariantExpectations.ageYearsNextPeriod;
        numberExpected = invariantExpectations.numberExpected;
        probability = new double[numberExpected];
        anticipated = new States[numberExpected];
        for (int ii = 0; ii< numberExpected; ii++) {
            probability[ii] = invariantExpectations.probability[ii];
            anticipated[ii] = new States(invariantExpectations.anticipated[ii]);
        }

        // add person proxy for this period
        personProxyThisPeriod = new Person(true);
        personProxyThisPeriod.setRegionLocal(currentStates.getRegionCode());
        personProxyThisPeriod.setDgn(currentStates.getGenderCode());
        personProxyThisPeriod.setDhe(currentStates.getHealthCode());
        personProxyThisPeriod.setDeh_c3(currentStates.getEducationCode());
        if (SocialCareMarketAll.Informal.equals(currentStates.getSocialCareMarketCode()) ||
                SocialCareMarketAll.Mixed.equals(currentStates.getSocialCareMarketCode()))
            personProxyThisPeriod.setCareFromOther(true);

        // add person proxy for next period expectations
        personProxyNextPeriod = new Person(true);
        personProxyNextPeriod.setYearLocal(currentStates.getYearByAge(ageYearsNextPeriod));
        personProxyNextPeriod.setRegionLocal(currentStates.getRegionCode());
        personProxyNextPeriod.setDhhtp_c4_lag1Local(currentStates.getHouseholdTypeCode());
        personProxyNextPeriod.setYdses_c5_lag1Local(Ydses_c5.Q3);
        personProxyNextPeriod.setN_children_allAges_lag1Local(currentStates.getChildrenAll());
        personProxyNextPeriod.setN_children_allAges_Local(currentStates.getChildrenAll());
        personProxyNextPeriod.setN_children_02_lag1Local(currentStates.getChildren02());
        personProxyNextPeriod.setDag(ageYearsNextPeriod);
        personProxyNextPeriod.setDgn(currentStates.getGenderCode());
        personProxyNextPeriod.setDlltsd(currentStates.getDlltsd());
        personProxyNextPeriod.setDlltsd_lag1(currentStates.getDlltsd());
        personProxyNextPeriod.setDhe(currentStates.getHealthCode());
        personProxyNextPeriod.setDhe_lag1(currentStates.getHealthCode());
        if (SocialCareMarketAll.Informal.equals(currentStates.getSocialCareMarketCode()) ||
                SocialCareMarketAll.Mixed.equals(currentStates.getSocialCareMarketCode()))
            personProxyThisPeriod.setCareHoursFromOtherWeekly_lag1(10.0);
        if (SocialCareMarketAll.Formal.equals(currentStates.getSocialCareMarketCode()) ||
                SocialCareMarketAll.Mixed.equals(currentStates.getSocialCareMarketCode()))
            personProxyThisPeriod.setCareHoursFromFormalWeekly_lag1(10.0);
        personProxyNextPeriod.setDed(currentStates.getStudentIndicator());
        personProxyNextPeriod.setDeh_c3(currentStates.getEducationCode());
        personProxyNextPeriod.setDeh_c3_lag1(currentStates.getEducationCode());
        personProxyNextPeriod.setDehf_c3(DecisionParams.EDUCATION_FATHER);
        personProxyNextPeriod.setDehm_c3(DecisionParams.EDUCATION_MOTHER);
        personProxyNextPeriod.setDcpst(currentStates.getDcpst());
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

        // labour and labour income in current period - to evaluate taxes and benefits
        double labourIncome1Weekly = 0;
        double labourIncome2Weekly = 0;
        double hourlyWageRate1, hourlyWageRate2;
        Integer labourHours1Weekly = null;
        Integer labourHours2Weekly = null;
        leisureTime = 1;
        if (currentStates.ageYears <= DecisionParams.maxAgeFlexibleLabourSupply) {
            labourHours1Weekly = (int) Math.round(DecisionParams.FULLTIME_HOURS_WEEKLY * emp1Pr);
            hourlyWageRate1 = getHourlyWageRate(labourHours1Weekly);
            labourIncome1Weekly = hourlyWageRate1 * (double)labourHours1Weekly;
            if (cohabitation) {
                labourHours2Weekly = (int) Math.round(DecisionParams.FULLTIME_HOURS_WEEKLY * emp2Pr);
                hourlyWageRate2 = getHourlyWageRate(labourHours2Weekly);
                labourIncome2Weekly = hourlyWageRate2 * (double)labourHours2Weekly;
                leisureTime = 1 - ((double)(labourHours1Weekly + labourHours2Weekly)) / (2.0 * DecisionParams.LIVING_HOURS_WEEKLY);
            } else {
                leisureTime = 1 - ((double)labourHours1Weekly) / DecisionParams.LIVING_HOURS_WEEKLY;
            }
        } else if (emp1Pr>1.0E-5 || emp2Pr>1.0E-5) {
            throw new InvalidParameterException("inconsistent labour decisions supplied for updating expectations");
        }

        // pension income
        double pensionIncome1Annual, pensionIncome2Annual;
        if (DecisionParams.flagRetirement && ageYearsNextPeriod > DecisionParams.minAgeToRetire) {
            if (currentStates.getRetirement() == 0 && emp1Pr == 0 && pensionIncomePerYear == 0 && liquidWealth > 0) {
                // allow for annuitisation at retirement
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
                labourHours2Weekly, disability2, labourIncome2Weekly, investmentIncome2Annual, pensionIncome2Annual, childcareCostAnnual, socialCareCostAnnual, liquidWealth);

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
                    labourIncome1Weekly*Parameters.WEEKS_PER_MONTH + (investmentIncome1Annual + pensionIncome1Annual) / 12.0);
            if (cohabitation) {
                personProxyNextPeriod.setYnbcpdf_dv_lag1(
                        labourIncome1Weekly*Parameters.WEEKS_PER_MONTH + (investmentIncome1Annual + pensionIncome1Annual) / 12.0 -
                                (labourIncome2Weekly*Parameters.WEEKS_PER_MONTH + (investmentIncome2Annual + pensionIncome2Annual) / 12.0));
            } else {
                personProxyNextPeriod.setYnbcpdf_dv_lag1(0.0);
            }

            // retirement - not a state included in personProxyNextPeriod (don't track changes)
            if (DecisionParams.flagRetirement && ageYearsNextPeriod > DecisionParams.minAgeToRetire && ageYearsNextPeriod <= DecisionParams.maxAgeFlexibleLabourSupply) {
                stateIndexNextPeriod = scale.getIndex(Axis.Retirement, ageYearsNextPeriod);
                int currentRetirement = currentStates.getRetirement();
                if (currentRetirement==0 && emp1Pr==0) {
                    // retire this period
                    for (int ii = 0; ii < numberExpected; ii++) {
                        anticipated[ii].states[stateIndexNextPeriod] = 1.0;
                    }
                } else {
                    // no change to retirement state
                    for (int ii = 0; ii < numberExpected; ii++) {
                        anticipated[ii].states[stateIndexNextPeriod] = currentRetirement;
                    }
                }
            }

            // student - don't need to track separately from education (no need for flagStudentVaries)
            if (ageYearsNextPeriod <= Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION && DecisionParams.flagEducation) {
                stateIndexCurrPeriod = scale.getIndex(Axis.Student, currentStates.ageYears);
                stateIndexNextPeriod = scale.getIndex(Axis.Student, ageYearsNextPeriod);
                LocalExpectations lexpect;
                if (currentStates.getStudent() == 0) {
                    lexpect = new LocalExpectations(currentStates.states[stateIndexCurrPeriod]);
                } else {
                    lexpect = new LocalExpectations(personProxyNextPeriod, RegressionNames.EducationE1a);
                }
                expandExpectationsAllIndices(stateIndexNextPeriod, lexpect.probabilities, lexpect.values);
            }

            // education
            boolean flagEducationVaries = false;
            if (DecisionParams.flagEducation) {
                stateIndexCurrPeriod = scale.getIndex(Axis.Education, currentStates.ageYears);
                stateIndexNextPeriod = scale.getIndex(Axis.Education, ageYearsNextPeriod);
                if (currentStates.getStudent() == 0) {
                    // not student - no change in education state possible

                    LocalExpectations lexpect = new LocalExpectations(currentStates.states[stateIndexCurrPeriod]);
                    expandExpectationsAllIndices(stateIndexNextPeriod, lexpect.probabilities, lexpect.values);
                } else {
                    // student in current period - allow for exit from education

                    // set-up probabilities and values
                    Map<Education,Double> probsStudent = simpaths.data.Parameters.getRegEducationE2a().getProbabilities(personProxyNextPeriod, Person.DoublesVariables.class);

                    // update expectations array
                    int numberExpectedInitial = numberExpected;
                    for (int ii = 0; ii < numberExpectedInitial; ii++) {
                        if (anticipated[ii].getStudent() == 1) {
                            // continuing student
                            anticipated[ii].states[stateIndexNextPeriod] = currentStates.states[stateIndexCurrPeriod];
                        } else {
                            // allow for exit from education
                            expandExpectationsSingleIndex(ii, stateIndexNextPeriod, probsStudent);
                            flagEducationVaries = true;
                        }
                    }
                }
            }

            // health
            boolean flagHealthVaries = false;
            if (DecisionParams.flagHealth && ageYearsNextPeriod >= DecisionParams.minAgeForPoorHealth) {

                // state indices
                flagHealthVaries = true;
                stateIndexNextPeriod = scale.getIndex(Axis.Health, ageYearsNextPeriod);

                // populate expectations
                if (!flagEducationVaries) {
                    // both student status and education given

                    Map<Dhe,Double> probs = Parameters.getRegHealthH1b().getProbabilities(personProxyNextPeriod, Person.DoublesVariables.class);
                    expandExpectationsAllIndices(stateIndexNextPeriod, probs);
                } else {
                    // need to allow for education change

                    // for continuing students
                    updatePersonNextPeriod(currentStates, Axis.Education);
                    Map<Dhe,Double> studentProbs = Parameters.getRegHealthH1a().getProbabilities(personProxyNextPeriod, Person.DoublesVariables.class);

                    // begin loop over existing expectations
                    int numberExpectedInitial = numberExpected;
                    for (int ii=0; ii<numberExpectedInitial; ii++) {
                        if (anticipated[ii].getStudent()==1) {
                            // continuing student
                            expandExpectationsSingleIndex(ii, stateIndexNextPeriod, studentProbs);
                        } else {
                            // exit from education - allow for education change
                            updatePersonNextPeriod(anticipated[ii], Axis.Education);
                            Map<Dhe,Double> probs = Parameters.getRegHealthH1b().getProbabilities(personProxyNextPeriod, Person.DoublesVariables.class);
                            expandExpectationsSingleIndex(ii, stateIndexNextPeriod, probs);
                        }
                    }
                }
           }

            // disability
            boolean flagDisabilityVaries = false;
            if (DecisionParams.flagDisability  && ageYearsNextPeriod >= DecisionParams.minAgeForPoorHealth) {
                flagDisabilityVaries = true;
                stateIndexNextPeriod = scale.getIndex(Axis.Disability, ageYearsNextPeriod);
                indicatorExpectations(flagEducationVaries, flagHealthVaries, stateIndexNextPeriod, RegressionNames.HealthH2b);
            }

            // cohabitation (1 = cohabiting)
            boolean flagCohabitationVaries = false;
            if (ageYearsNextPeriod <= DecisionParams.MAX_AGE_COHABITATION) {
                flagCohabitationVaries = true;
                stateIndexNextPeriod = scale.getIndex(Axis.Cohabitation, ageYearsNextPeriod);
                if (cohabitation) {
                    indicatorExpectations(flagEducationVaries, stateIndexNextPeriod, 0.0, 1.0, RegressionNames.PartnershipU2b);
                } else {
                    indicatorExpectations(flagEducationVaries, stateIndexNextPeriod, RegressionNames.PartnershipU1a, RegressionNames.PartnershipU1b);
                }
            }

            // dependent children
            // loop over each birth age
            boolean flagChildrenVaries = false;
            for (int jj = 0; jj < DecisionParams.NUMBER_BIRTH_AGES; jj++) {

                if (ageYearsNextPeriod >= DecisionParams.BIRTH_AGE[jj] && ageYearsNextPeriod < (DecisionParams.BIRTH_AGE[jj] + simpaths.data.Parameters.AGE_TO_BECOME_RESPONSIBLE)) {
                    // may have children from this age in next period

                    if (ageYearsNextPeriod == DecisionParams.BIRTH_AGE[jj]) {
                        // next year is birth age - number of children uncertain

                        flagChildrenVaries = true;
                        stateIndexNextPeriod = scale.getIndex(Axis.Child, ageYearsNextPeriod, jj);
                        int options = (int)scale.axes[ageIndexNextPeriod][stateIndexNextPeriod][0];

                        // begin loop over existing expectations
                        int numberExpectedInitial = numberExpected;
                        for (int ii=0; ii<numberExpectedInitial; ii++) {

                            // update states
                            updatePersonNextPeriod(anticipated[ii], Axis.Child);
                            updatePersonNextPeriod(anticipated[ii], Axis.Education);

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
                    } else {
                        // assume next year have same number of children as this year

                        stateIndexCurrPeriod = scale.getIndex(Axis.Child, currentStates.ageYears, jj);
                        stateIndexNextPeriod = scale.getIndex(Axis.Child, ageYearsNextPeriod, jj);
                        LocalExpectations lexpect = new LocalExpectations(currentStates.states[stateIndexCurrPeriod]);
                        expandExpectationsAllIndices(stateIndexNextPeriod, lexpect.probabilities, lexpect.values);
                    }
                }
            }

            // social care market
            boolean flagSocialCareMarketVaries = false;
            if (Parameters.flagSocialCare  && ageYearsNextPeriod >= DecisionParams.minAgeFormalSocialCare) {

                flagSocialCareMarketVaries = true;
                stateIndexNextPeriod = scale.getIndex(Axis.SocialCareMarket, ageYearsNextPeriod);
                Map<SocialCareMarketAll,Double> probs = null;
                if (flagEducationVaries || flagHealthVaries || flagCohabitationVaries) {

                    int numberExpectedInitial = numberExpected;
                    for (int ii=0; ii<numberExpectedInitial; ii++) {

                        boolean flagEval = false;
                        if (probs == null) {
                            flagEval = true;
                        }
                        if (flagEducationVaries) {
                            boolean flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Education);
                            if (flagChange) flagEval = true;
                        }
                        if (flagHealthVaries) {
                            boolean flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Health);
                            if (flagChange) flagEval = true;
                        }
                        if (flagCohabitationVaries) {
                            boolean flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Cohabitation);
                            if (flagChange) flagEval = true;
                        }
                        if (flagEval) {
                            double prob0 = Parameters.getRegReceiveCareS2b().getProbability(personProxyNextPeriod, Person.DoublesVariables.class);
                            Map<SocialCareMarket,Double> probs1 = Parameters.getRegSocialCareMarketS2c().getProbabilites(personProxyNextPeriod, Person.DoublesVariables.class, SocialCareMarket.class);
                            probs = compileSocialCareProbs(prob0, probs1);
                        }
                        if (probs==null)
                            throw new RuntimeException("attempt to assign expectations for social care market before evaluated");
                        expandExpectationsSingleIndex(ii, stateIndexNextPeriod, probs);
                    }
                } else {

                    double prob0 = Parameters.getRegReceiveCareS2b().getProbability(personProxyNextPeriod, Person.DoublesVariables.class);
                    Map<SocialCareMarket,Double> probs1 = Parameters.getRegSocialCareMarketS2c().getProbabilites(personProxyNextPeriod, Person.DoublesVariables.class, SocialCareMarket.class);
                    probs = compileSocialCareProbs(prob0, probs1);
                    expandExpectationsAllIndices(stateIndexNextPeriod, probs);
                }
            }

            // full-time wage potential
            if (ageYearsNextPeriod <= DecisionParams.maxAgeFlexibleLabourSupply) {

                // state indices
                stateIndexNextPeriod = scale.getIndex(Axis.WagePotential, ageYearsNextPeriod);

                double minValue = Math.log(DecisionParams.MIN_WAGE_PHOUR);
                double maxValue = Math.log(DecisionParams.MAX_WAGE_PHOUR);
                int numberExpectedInitial = numberExpected;
                boolean flagChange, flagEval = true;
                LocalExpectations lexpect = null;
                Double rmse;
                if (currentStates.getGenderCode()==Gender.Male) {
                    rmse = ManagerRegressions.getRmse(RegressionNames.WagesMalesE);
                } else {
                    rmse = ManagerRegressions.getRmse(RegressionNames.WagesFemalesE);
                }
                for (int ii=0; ii<numberExpectedInitial; ii++) {

                    // update regression variables
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
                    if (flagSocialCareMarketVaries) {
                        flagChange = updatePersonNextPeriod(anticipated[ii], Axis.SocialCareMarket);
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

                    // update expectations
                    if (flagEval) {

                        Double score;
                        if (currentStates.getGenderCode()==Gender.Male) {
                            score = ManagerRegressions.getScore(personProxyNextPeriod, RegressionNames.WagesMalesE);
                        } else {
                            score = ManagerRegressions.getScore(personProxyNextPeriod, RegressionNames.WagesFemalesE);
                        }
                        lexpect = new LocalExpectations(score, rmse, minValue, maxValue);
                        for (int jj=0; jj<lexpect.values.length; jj++) {
                            lexpect.values[jj] = Math.log(Math.exp(lexpect.values[jj]) + DecisionParams.C_WAGE_POTENTIAL);
                        }
                    }
                    expandExpectationsSingleIndex(ii, stateIndexNextPeriod, lexpect.probabilities, lexpect.values);
                    flagEval = false;
                }
            }

            // pension income
            if (DecisionParams.flagRetirement && ageYearsNextPeriod > DecisionParams.minAgeToRetire) {
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
            if (ageYearsNextPeriod <= DecisionParams.maxAgeFlexibleLabourSupply && DecisionParams.FLAG_WAGE_OFFER1) {
                stateIndexNextPeriod = scale.getIndex(Axis.WageOffer1, ageYearsNextPeriod);
                LocalExpectations lexpect = new LocalExpectations(1.0, 0.0, DecisionParams.PROBABILITY_WAGE_OFFER1);
                expandExpectationsAllIndices(stateIndexNextPeriod, lexpect.probabilities, lexpect.values);
            }

            // check evaluated probabilities
            double probabilityCheck = 0;
            for (int ii = 0; ii< numberExpected; ii++) {
                probabilityCheck += probability[ii];
            }
            if (Math.abs(probabilityCheck-1) > 1.0E-5) {
                throw new InvalidParameterException("problem with probabilities supplied to outer expectations");
            }
        }
    }

    private double evalChildcareCostWeekly() {

        double childcareCostWeekly = 0.0;
        if (Parameters.flagFormalChildcare && currentStates.hasChildrenEligibleForCare()) {

            double probFormalChildCare = Parameters.getRegChildcareC1a().getProbability(benefitUnitProxyThisPeriod, BenefitUnit.Regressors.class);
            double logChildcareCostScore = Parameters.getRegChildcareC1b().getScore(benefitUnitProxyThisPeriod, BenefitUnit.Regressors.class);
            double logChildcareRSME = ManagerRegressions.getRmse(RegressionNames.ChildcareValue);
            childcareCostWeekly = Math.exp(logChildcareCostScore + logChildcareRSME*logChildcareRSME/2.0) * probFormalChildCare;
        }
        return childcareCostWeekly;
    }

    private double evalSocialCareCostWeekly() {

        double socialCareCostWeekly = 0.0;
        if (Parameters.flagSocialCare && (currentStates.getAgeYears()>=DecisionParams.minAgeFormalSocialCare)) {

            SocialCareMarketAll market = currentStates.getSocialCareMarketCode();
            if (SocialCareMarketAll.Mixed.equals(market) || SocialCareMarketAll.Formal.equals(market)) {

                double score = Parameters.getRegFormalCareHoursS2k().getScore(personProxyThisPeriod,Person.DoublesVariables.class);
                double rmse = Parameters.getRMSEForRegression("S2k");
                double hours = Math.min(150.0, Math.exp(score + rmse*rmse/2.0));
                socialCareCostWeekly = hours * Parameters.getTimeSeriesValue(currentStates.getYear(), TimeSeriesVariable.CarerWageRate);
            }
        }
        return socialCareCostWeekly;
    }

    private Map<SocialCareMarketAll,Double> compileSocialCareProbs(double prob0, Map<SocialCareMarket,Double> probs1) {
        Map<SocialCareMarketAll,Double> probs = new HashMap<>();
        probs.put(SocialCareMarketAll.None, 1-prob0);
        for (SocialCareMarket key : SocialCareMarket.values()) {
            probs.put(SocialCareMarketAll.getCode(key), probs1.get(key) * prob0);
        }
        return probs;
    }

    /**
     * METHOD TO CALL TO TAX AND BENEFIT FUNCTION
     * @return disposable income per annum of benefitUnit
     */
    public double taxBenefitFunction(Integer labourHours1Weekly,
                                     Integer disability1,
                                     double labourIncome1Weekly,
                                     double investmentIncome1Annual,
                                     double pensionIncome1Annual,
                                     Integer labourHours2Weekly,
                                     Integer disability2,
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
                numberChildrenAged10To17, hoursWorkPerWeek1, hoursWorkPerWeek2, disability1, disability2, originalIncomePerMonth, secondIncomePerMonth,
                childcareCostPerMonth, socialCareCostPerMonth, liquidWealth, -1.0);

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
            throw new InvalidParameterException("problem with probabilities supplied to outer expectations");
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
                personProxyNextPeriod.setN_children_allAges_lag1Local(childrenAll + ii);
                personProxyNextPeriod.setN_children_02_lag1Local(birthsHere02);
                double proportionBirths = ManagerRegressions.getProbability(personProxyNextPeriod, regression);
                probabilities[ii+1] += probabilities[ii] * proportionBirths;
                probabilities[ii] *= (1 - proportionBirths);
            }
        }

        // expand expectations array
        expandExpectationsSingleIndex(expandIndex, stateIndex, probabilities, values);

        // restore benefitUnit and person characteristics
        personProxyNextPeriod.setDag(ageYearsNextPeriod);
        personProxyNextPeriod.setN_children_allAges_lag1Local(childrenAll);
        personProxyNextPeriod.setN_children_02_lag1Local(children02);
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

    private void indicatorExpectations(boolean variableEducation, boolean variableHealth, int stateIndexNextPeriod, RegressionNames reg) {
        indicatorExpectations(variableEducation, variableHealth, 1.0, 0.0, stateIndexNextPeriod, reg, reg);
    }
    private void indicatorExpectations(boolean variableEducation, int stateIndexNextPeriod, double valTrue, double valFalse, RegressionNames reg) {
        indicatorExpectations(variableEducation, false, valTrue, valFalse, stateIndexNextPeriod, reg, reg);
    }
    private void indicatorExpectations(boolean variableEducation, int stateIndexNextPeriod, RegressionNames studentReg, RegressionNames nonstudentReg) {
        indicatorExpectations(variableEducation, false, 1.0, 0.0, stateIndexNextPeriod, studentReg, nonstudentReg);
    }
    private void indicatorExpectations(boolean variableEducation, boolean variableHealth, double valTrue, double valFalse, int stateIndexNextPeriod, RegressionNames studentReg, RegressionNames nonstudentReg) {

        LocalExpectations lexpect = null;
        if (variableEducation || variableHealth) {

            int numberExpectedInitial = numberExpected;
            for (int ii=0; ii<numberExpectedInitial; ii++) {

                boolean flagEval = false;
                if (lexpect == null) {
                    flagEval = true;
                }
                if (variableEducation) {
                    boolean flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Education);
                    if (flagChange) flagEval = true;
                }
                if (variableHealth) {
                    boolean flagChange = updatePersonNextPeriod(anticipated[ii], Axis.Health);
                    if (flagChange) flagEval = true;
                }
                if (flagEval) {
                    if(anticipated[ii].getStudent() == 1) {
                        lexpect = new LocalExpectations(personProxyNextPeriod, valTrue, valFalse, studentReg);
                    } else {
                        lexpect = new LocalExpectations(personProxyNextPeriod, valTrue, valFalse, nonstudentReg);
                    }
                }
                expandExpectationsSingleIndex(ii, stateIndexNextPeriod, lexpect.probabilities, lexpect.values);
            }
        } else {

            if(anticipated[0].getStudent() == 1) {
                lexpect = new LocalExpectations(personProxyNextPeriod, valTrue, valFalse, studentReg);
            } else {
                lexpect = new LocalExpectations(personProxyNextPeriod, valTrue, valFalse, nonstudentReg);
            }
            expandExpectationsAllIndices(stateIndexNextPeriod, lexpect.probabilities, lexpect.values);
        }
    }

    private boolean updatePersonNextPeriod(States states, Axis axis) {
        boolean changed = false;
        Object val0, val1;
        if (Axis.Education.equals(axis)) {
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
        } else if (Axis.SocialCareMarket.equals(axis)) {
            val0 = personProxyNextPeriod.getSocialCareMarketAll();
            val1 = states.getSocialCareMarketCode();
        } else if (Axis.Cohabitation.equals(axis)) {
            val0 = personProxyNextPeriod.getDcpst();
            val1 = states.getDcpst();
        } else if (Axis.Child.equals(axis)) {
            val0 = personProxyNextPeriod.getN_children_017Local();
            val1 = states.getChildren017();
            if (val0==val1) {
                val0 = personProxyNextPeriod.getD_children_2underLocal();
                val1 = states.getChildrenUnder3Indicator();
            }
            if (val0==val1) {
                val0 = personProxyNextPeriod.getN_children_allAges_Local();
                val1 = states.getChildren017();
            }
        } else {
            throw new RuntimeException("unrecognised axis for considering change in person proxy states");
        }
        if (val0!=val1) {
            changed = true;

            if (Axis.Education.equals(axis)) {
                personProxyNextPeriod.setDeh_c3(states.getEducationCode());
                personProxyNextPeriod.setDed(states.getStudentIndicator());
            } else if (Axis.Health.equals(axis)) {
                personProxyNextPeriod.setDhe(states.getHealthCode());
            } else if (Axis.Disability.equals(axis)) {
                personProxyNextPeriod.setDlltsd(states.getDlltsd());
            } else if (Axis.SocialCareMarket.equals(axis)) {
                personProxyNextPeriod.setSocialCareMarketAll(states.getSocialCareMarketCode());
            } else if (Axis.Cohabitation.equals(axis)) {
                personProxyNextPeriod.setDcpst(states.getDcpst());
            } else if (Axis.Child.equals(axis)) {
                personProxyNextPeriod.setN_children_017Local(states.getChildren017());
                personProxyNextPeriod.setD_children_2underLocal(states.getChildrenUnder3Indicator());
                personProxyNextPeriod.setN_children_allAges_Local(states.getChildren017());
            }
        }
        return changed;
    }
}
