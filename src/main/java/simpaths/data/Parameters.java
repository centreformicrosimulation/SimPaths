// define package
package simpaths.data;

// import Java packages

import microsim.data.MultiKeyCoefficientMap;
import microsim.data.excel.ExcelAssistant;
import microsim.statistics.regression.*;
// import plug-in packages
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.io.FileUtils;
import simpaths.data.startingpop.DataParser;
import simpaths.model.AnnuityRates;
import simpaths.model.enums.*;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.util.Pair;
import simpaths.model.decisions.Grids;
import simpaths.model.lifetime_incomes.EquivalisedIncomeCDF;
import simpaths.model.taxes.DonorTaxUnit;
import simpaths.model.taxes.MatchFeature;
import simpaths.model.taxes.database.TaxDonorDataParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static microsim.statistics.regression.RegressionUtils.appendCoefficientMaps;


/**
 *
 * CLASS TO STORE MODEL PARAMETERS FOR GLOBAL ACCESS
 *
 */
public class Parameters {

//    private static String getCountryInputDir(Country country) {
//        return INPUT_DIRECTORY + country + File.separator;
//    }
//
//    private static String resolveCountryFile(Country country, String fileName) {
//        return getCountryInputDir(country) + fileName;
//    }

    public static final boolean TESTING_FLAG = false;

    // EUROMOD variables


    // Insert here names of EUROMOD variables to use in Person and BenefitUnit tables of the input database
    // NOTE: The model economises set-up costs for the donor population by considering policy projections generated
    // for multiple "system years" for the same input data. All non-financial statistics should consequently be the
    // same for the donor population (described under DONOR_STATIC_VARIABLES). Memory is further economised by assuming
    // a constant inflation adjustment for all factors in EUROMOD, so that exogenous financial inputs vary by system year
    // only by the inflation rate. Setting this rate to the rate of inflation loaded in via scenario_uprating_factor.xls
    // permits these exogenous inputs to be inferred across system years without storing them separately for each year
    // The only financial statistics that vary by system should consequently be policy dependant variables,
    // (described below under DONOR_POLICY_VARIABLES). The model performs checks to ensure that these conditions are
    // met when it loads in data.
    public static final String[] DONOR_STATIC_VARIABLES = new String[] {
		"idhh",					//id of household
		"idperson", 			//id of person
		"idfather", 			//id of father
		"idmother", 			//id of mother
		"idpartner", 			//id of partner
		"dag", 					//age
		"dct", 					//country
		"deh", 					//highest education level
		"dgn", 					//gender
		"drgn1", 			    //region (NUTS1)
		"dwt", 					//household weight
		"les", 					//labour employment status + health status
		"lcs", 					//labour civil servant dummy indicator
        "lcr01",                //carer status for benefits (0 no 1 yes)
		"lhw", 					//hours worked per week
        "yem", 					//employment income - used to construct work sector *NOT VALID FOR POLICY ANALYSIS*
        "yse", 					//self-employment income - used to construct work sector *NOT VALID FOR POLICY ANALYSIS*
        "bdioa",                //Attendance Allowance
        "bdisc",                //Disability Living Allowance
        "bdimb",                //Disability Living Allowance (mobility)
        "bdiscwa",              //Personal Independence Payment living allowance
        "bdimbwa",              //Personal Independence Payment mobility
        "bdict01",              //Incapacity Benefit
        "bdict02",              //Contributory Employment and Support Allowance
        "bsadi_s",              //income-related Employment and Support Allowance
        "bdiwi",                //Industrial injuries pension
        "bdisv",                //Severe Disablement Allowance
    };

    public static final String[] DONOR_POLICY_VARIABLES = new String[] {
        "xcc",                  //childcare costs
        "ils_earns",			//EUROMOD output variable:- total labour earnings (employment + self-employment income + potentially other labour earnings like temporary employment, depending on country classification)
        "ils_origy",			//EUROMOD output variable:- all gross income from labour, private pensions, investment income, property income, private transfers etc.
		"ils_dispy",			//Disposable income : from EUROMOD output data after tax / benefit transfers (monthly demYear-scale)
		"ils_benmt",			//EUROMOD output variable: income list: monetary benefits
		"ils_bennt",			//EUROMOD output variable: income list: non-monetary benefits
        "bsauc_s",               //EUROMOD output variable: simulated UC receipt
        "bho_s",
        "bwkmt_s",
        "bfamt_s",
        "bunct_s",
        "bsa_s",
        "bsadi_s"
    };

    public static final String[] HOUSEHOLD_VARIABLES_INITIAL = new String[] {
		"idHh",				//id of household (can contain multiple benefit units)
    };

    public static final String[] BENEFIT_UNIT_VARIABLES_INITIAL = new String[] {
		"idHh",				//id of household (can contain multiple benefit units)
		"idBu",	//id of a benefit unit
		"demRgn", 			//demRgn (NUTS1)
        "yDispMonth",         //disposable income
		"yHhQuintilesMonthC5",			//household income quantile
		"wealthPrptyFlag",		//flag indicating if benefit unit owns a house
		"wealthTotValue",	    //benefit unit total net wealth (includes pensions assets and housing)
        "wealthPensValue",   //benefit unit total private (personal and occupational) pensions
        "wealthPrptyValue",   //benefit unit value of main home (gross of mortgage debt)
        "wealthMortgageDebtValue",    //benefit unit value of mortgage debt
    };

    public static final String[] PERSON_VARIABLES_INITIAL = new String[] {
		"idHh",					//id of household (can contain multiple benefit units)
		"idBu",		//id of a benefit unit
		"idPers", 			//id of person
		"wgtHhCross", 					//household dem
		"idFather", 			//id of father
		"idMother", 			//id of mother
		"demAge", 					//age
		"eduHighestC3", 				//highest education level
		"eduHighestMotherC3",				//highest education level of mother
		"eduHighestFatherC3",				//highest education level of father
		"eduSpellFlag",					//in education dummy
		"eduReturnFlag",					//return to education dummy
		"demEthnC4",					//ethnicity
        "demEthnC6",				//ethnicity 6 categories
		"healthSelfRated",					//health status
        "healthMentalMcs",              //mental health - SF12 score MCS
        "healthPhysicalPcs",              //physical health - SF12 score PCS
        "healthMentalPartnerMcs",            //mental health - SF12 score MCS (partner)
        "healthPhysicalPartnerPcs",            //physical health - SF12 score PCS (partner)
        "healthWbScore0to36",					//mental health status
		"healthPsyDstrss0to12",				// mental health status 0 to 12
        "demLifeSatScore0to10",                  //life satisfaction
        "yFinDstrssFlag",	//financial distress
		"demPartnerNYear",				//years in partnership
		"demAgePartnerDiff",				//partners age difference
		"demNChild0to2",				//number children aged 0-2
		"demNChild",					//number children
		"yNonBenPersGrossMonth",			//gross personal non-benefit income
		"yMiscPersGrossMonth",			//gross personal non-employment non-benefit income
		"yCapitalPersMonth",  				//gross personal capital income
		"yPensPersGrossMonth",				//gross personal pension (public / occupational) income
		"yEmpPersGrossMonth",			//gross personal employment income
		"yPersAndPartnerGrossDiffMonth",			//difference partner income
		"healthDsblLongtermFlag",				//long-term sick or disabled (we use this -and not healthDsblLongtermFlag- in the DataParser)
		"eduExitSampleFlag",				//year left education
		"statInterviewYear",					//system variable - year
		"statCollectionWave",					//system variable - wave
		"demMaleFlag", 					//demSex
		"labC4", 				//labour employment status
		"labHrsWorkWeek", 					//hours worked per week
        "labHrsWorkWeekL1",               //hours worked per week in the previous year
		"demAdultChildFlag",		//flag indicating adult child living at home in the data
		"labWageHrly", //initial value of hourly earnings from the data
		"labWageHrlyL1", //lag(1) of initial value of hourly earnings from the data
        "careNeedFlag",          //indicator that the individual needs social care
        "careHrsFormal",    //number of hours of formal care received
        "careFormalX",   //cost of formal care received
        "careHrsInformal",   //number of hours of informal care received
        "careHrsProvidedWeek", // number of informal care hour provided per week
        "yBenReceivedFlag",        //indicator of benefit receipt
        "yBenUCReceivedFlag",     //indicator of UC receipt
        "yBenNonUCReceivedFlag",  //indicator of other benefit receipt
        // "labWorkHist",                //Total years in employment since Jan 2007
		//"yem", 					//employment income
		//"yse", 					//self-employment income

		//From EUROMOD output data before tax / benefit transfers, so not affected by EUROMOD policy scenario (monthly demYear-scale).  We just use them calculated from EUROMOD output because EUROMOD has the correct way of aggregating each country's different component definitions
		//"ils_earns", 			//EUROMOD output variable:- total labour earnings (employment + self-employment income + potentially other labour earnings like temporary employment, depending on country classification)
		//"ils_origy"			//EUROMOD output variable:- all gross income from labour, private pensions, investment income, property income, private transfers etc.
    };

    //Parameters for managing tax and benefit imputations
    public static final int TAXDB_REGIMES = 5;
    private static Map<MatchFeature, Map<Integer, Integer>> taxdbCounter = new HashMap<MatchFeature, Map<Integer, Integer>>();			// records, for each of the three donor keys (first Integer), the increments (second Integer) associated with one unit change in characteristic (String).  The properties of taxdbCounter are specific to the KeyFunction used (and are populated by the associated function)
    private static List<DonorTaxUnit> donorPool;													// list of donors for tax imputation, in ascending order by private (original) income
    private static Map<Triple<Integer,Integer,Integer>,List<Integer>> taxdbReferences = new HashMap<>();	    // for Triple <system year, matching regime, regime index> returns a list of indices to donorPool that describes members of grouping, in ascending order by private income
    private static MahalanobisDistance mdDualIncome;
    private static MahalanobisDistance mdChildcare;
    private static MahalanobisDistance mdDualIncomeChildcare;

    //Labour Demand/Supply Convergence parameters
    public static final double INITIAL_DECAY_FACTOR = 1.;	//Initialisation of field that is used to check whether labour market equilibrium is progressing fast enough (is multiplied by a factor at each iteration when needed, so the factor gets exponentially smaller)
    public static final DemandAdjustment demandAdjustment = DemandAdjustment.PopulationGrowth;		//Choose the method by which we derive the factor applied to the converged labour demand in order to get the initial labour demand at the following timestep
    public static final double ETA = 0.2;		//(eta in Matteo's document).  If excess labour demand is greater than this value (0.1 == 10%), then iterate convergence procedure again
    public static final double CHI_minus = 0.1;		//Lambda in Matteo's document is adjusted by reducing by lambda -> lambda * (1 - CHI_minus), or increasing lambda by lambda -> lambda * (1 + CHI_plus)
    public static final double CHI_plus = 0.1;		//Lambda in Matteo's document is adjusted by reducing by lambda -> lambda * (1 - CHI_minus), or increasing lambda by lambda -> lambda * (1 + CHI_plus)
    public static final Map<Education, Double> adjustmentFactorByEducation;	//Lambda_s in Matteo's document, where s is the education level
    public static final double initialLambda = ETA/5.;	//Ensure adjustment is smaller than relative excess labour demand threshold
    public static final int MinimumIterationsBeforeTestingConvergenceCriteria = 20;	//Run this number of iterations to accumulate estimates of (aggregate) labour supply (cross) elasticities before testing the convergence criterion (i.e. the norm of (supply * demand elasticities) matrix < 1)
    public static final int MaxConvergenceAttempts = 2 * MinimumIterationsBeforeTestingConvergenceCriteria;		//Allow the equilibrium convergence criterion to fail the test this number of times before potentially terminating the simulation.
    public static final double RateOfConvergenceFactor = 0.9;
    public static final double MAX_EMPLOYMENT_ALIGNMENT = 5.0; // the amount by which the coefficient used in the employment alignment can be shifted up or down;

    //Alignment parameters
    public static final int EMPLOYMENT_ALIGNMENT_END_YEAR = 2023;

    // parameters to manage simulation of optimised decisions
    public static boolean projectLiquidWealth = false;
    public static boolean projectPensionWealth = false;
    public static boolean projectHousingWealth = false;
    public static boolean enableIntertemporalOptimisations = false;
    public static Grids grids = null;

    static {
        adjustmentFactorByEducation = new LinkedHashMap<Education, Double>();		//Initialise adjustment factor to the same value for all education levels
        for (Education edu: Education.values()) {
            adjustmentFactorByEducation.put(edu, initialLambda);
        }
    }

    public static void resetAdjustmentFactorByEducation() {
        for(Education edu: Education.values()) {
            adjustmentFactorByEducation.put(edu, initialLambda);
        }
    }

    public static final double childrenNumDiscrepancyConstraint(double numberOfChildren) {
        if(numberOfChildren <= 1) {
            return 0.;
        }
        else if(numberOfChildren <= 3) {
            return 1.;
        }
        else if(numberOfChildren <= 5) {
            return 2.;
        }
        else {
            return 3.;
        }
    }
    public static final double AgeDiscrepancyConstraint = 10;	//Difference of age must equal of be below this value (so we allow 10 year cumulated age difference)
    public static final TreeSet<Double> EarningsDiscrepancyConstraint = new TreeSet<>(Arrays.asList(0.01, 0.02, 0.03, 0.04, 0.05));	//Proportional difference

    //Initial matching differential bounds - the initial bounds that a match must satisfy, before being relaxed
    public static final double UNMATCHED_TOLERANCE_THRESHOLD = 0.1;		//Smallest proportion of a demSex left unmatched (we take the minimum of the male proportion and female proportions).  If there are more than this, we will relax the constraints (e.g. the bounds on age difference and potential earnings difference) until this target has been reached
    public static final int MAXIMUM_ATTEMPTS_MATCHING = 10;
    public static final double RELAXATION_FACTOR = 1.5;

    public static final int AGE_DIFFERENCE_INITIAL_BOUND = 999;
    public static final double POTENTIAL_EARNINGS_DIFFERENCE_INITIAL_BOUND = 999.;

    public static final double WEEKS_PER_MONTH = 365.25/(7.*12.);	// = 4.348214286
    public static final double WEEKS_PER_YEAR = 365.25 / 7.;

    // Determine probability of yearly labour supply matches persisting from previous year
    public static double labour_innovation_employment_persistence_probability = 0.9;
    public static double labour_innovation_notinemployment_persistence_probability = 0.1;

    public static final int HOURS_IN_WEEK = 18 * 7; //This is used to calculate leisure in labour supply (18 = 24 - 6 hours of sleep)
    //Is it possible for people to start going to the labour module (e.g. age 17) while they are living with parents (until age 18)?
    //Cannot see how its possible if it is the household that decides how much labour to supply.  If someone finishes school at 17, they need to leave home before they can enter the labour market.  So set age for finishing school and leaving home to 18.
    public static final int MAX_LABOUR_HOURS_IN_WEEK = 48;
    public static final boolean USE_CONTINUOUS_LABOUR_SUPPLY_HOURS = true; // If true, a random number of hours of weekly labour supply within each bracket will be generated. Otherwise, each discrete choice of labour supply corresponds to a fixed number of hours of labour supply, which is the same for all persons
    public static int maxAge;										// maximum age possible in simulation
    public static final int AGE_TO_BECOME_RESPONSIBLE = 18;			// Age become reference person of own benefit unit
    public static final int MIN_AGE_TO_LEAVE_EDUCATION = 16;		// Minimum age for a person to leave (full-demYear) education
    public static final int MAX_AGE_TO_STAY_IN_CONTINUOUS_EDUCATION = 29;
    public static final int MIN_AGE_COHABITATION = AGE_TO_BECOME_RESPONSIBLE;  	// Min age a person can marry
    public static final int MIN_AGE_TO_HAVE_INCOME = 16; //Minimum age to have non-employment non-benefit income
    public static final int MIN_AGE_TO_RETIRE = 50; //Minimum age to consider retirement
    public static final int DEFAULT_AGE_TO_RETIRE = 67; //if pension included, but retirement decision not
    public static final int MIN_AGE_SOCIAL_CARE = 65; //Minimum age to receive formal social care
    public static final int MIN_AGE_FLEXIBLE_LABOUR_SUPPLY = 16; //Used when filtering people who can be "flexible in labour supply"
    public static final int MAX_AGE_FLEXIBLE_LABOUR_SUPPLY = 75;
    public static final double SHARE_OF_WEALTH_TO_ANNUITISE_AT_RETIREMENT = 0.25;
    public static final double ANNUITY_RATE_OF_RETURN = 0.015;
    public static AnnuityRates annuityRates;
    public static final int MIN_HOURS_FULL_TIME_EMPLOYED = 25;	// used to distinguish full-demYear from part-demYear employment (needs to be consistent with Labour enum)
    public static final double MIN_HOURLY_WAGE_RATE = 1.5;
    public static final double MAX_HOURLY_WAGE_RATE = 150.0;
    public static final double MAX_HOURS_WEEKLY_FORMAL_CARE = 150.0;
    public static final double MAX_HOURS_WEEKLY_INFORMAL_CARE = 16 * 7;
    public static final double CHILDCARE_COST_EARNINGS_CAP = 0.5;  // maximum share of earnings payable as childcare (for benefit units with some earnings)
    public static final int MIN_DIFFERENCE_AGE_MOTHER_CHILD_IN_ALIGNMENT = 15; //When assigning children to mothers in the population alignment, specify how much older (at the minimum) the mother must be than the child
    public static final int MAX_EM_DONOR_RATIO = 3; // Used by BenefitUnit => convertGrossToDisposable() to decide whether gross-to-net ratio should be applied or disposable income from the donor used directly
    public static final double PERCENTAGE_OF_MEDIAN_EM_DONOR = 0.2; // Used by BenefitUnit => convertGrossToDisposable() to decide whether gross-to-net ratio should be applied or disposable income from the donor used directly
    public static final double PSYCHOLOGICAL_DISTRESS_GHQ12_CASES_CUTOFF = 4; // Define cut-off on the GHQ12 Likert scale above which individuals are classified as psychologically distressed

    //Initial value for the savings rate and demPopSurveyShare for capital income:
    public static double SAVINGS_RATE; //This is set in the country-specific part of this file

    //public static int MAX_AGE_IN_EDUCATION;// = MAX_AGE;//30;			// Max age a person can stay in education	//Cannot set here, as MAX_AGE is not known yet.  Now set to MAX_AGE in buildObjects in Model class.
    //public static int MAX_AGE_MARRIAGE;// = MAX_AGE;//75;  			// Max age a person can marry		//Cannot set here, as MAX_AGE is not known yet.  Now set to MAX_AGE in buildObjects in Model class.
    private static int MIN_START_YEAR = 2011; //Minimum allowed starting point. Should correspond to the oldest initial population.
    private static int MAX_START_YEAR = 2023; //Maximum allowed starting point. Should correspond to the most recent initial population.
    public static int startYear;
    public static int endYear;
    private static final int MIN_START_YEAR_TESTING = 2019;
    private static final int MAX_START_YEAR_TESTING = 2019; //Maximum allowed starting point. Should correspond to the most recent initial population.
    private static final int MIN_START_YEAR_TRAINING = 2019;
    private static final int MAX_START_YEAR_TRAINING = 2019; //Maximum allowed starting point. Should correspond to the most recent initial population.
    public static final int MIN_AGE_MATERNITY = 18;  			// Min age a person can give birth
    public static final int MAX_AGE_MATERNITY = 49;  			// Max age a person can give birth
    public static final boolean FLAG_SINGLE_MOTHERS = true;
    public static boolean flagUnemployment = false;
    public static ArrayList<Integer> includeYears;

    public static int BASE_PRICE_YEAR = 2015; 			// Base price year of model parameters

    public static double PROB_NEWBORN_IS_MALE = 0.5;            // Must be strictly greater than 0.0 and less than 1.0

    public static boolean UC_ROLLOUT = true;              // Whether UC is available in population or not

    public static final boolean systemOut = true;

    //Bootstrap all the regression coefficients if true
    public static final boolean bootstrapAll = true;

    //Scheduling
    public static final int MODEL_ORDERING = 0;
    public static final int COLLECTOR_ORDERING = 1; //-2
    public static final int OBSERVER_ORDERING = 2; //-1

    //Initialise values specifying domain of original sick probability curves
    public static int femaleMinAgeSick = Integer.MAX_VALUE;
    public static int maleMinAgeSick = Integer.MAX_VALUE;
    public static int femaleMaxAgeSick = Integer.MIN_VALUE;
    public static int maleMaxAgeSick = Integer.MIN_VALUE;

    //For use with EUROMOD and h2 input database construction
    public static String WORKING_DIRECTORY = System.getProperty("user.dir");
    public static String INPUT_DIRECTORY = WORKING_DIRECTORY + File.separator + "input" + File.separator;
    public static boolean trainingFlag = false;
    public static String INPUT_DIRECTORY_INITIAL_POPULATIONS = INPUT_DIRECTORY + "InitialPopulations" + File.separator; //Path to directory containing initial population for each year
    public static String EUROMOD_OUTPUT_DIRECTORY = INPUT_DIRECTORY + "EUROMODoutput" + File.separator;
    public static String EUROMOD_TRAINING_DIRECTORY = EUROMOD_OUTPUT_DIRECTORY + "training" + File.separator;
    public static String EUROMODpolicyScheduleFilename = "EUROMODpolicySchedule";
    public static String DatabaseCountryYearFilename = "DatabaseCountryYear";

    //Headings in Excel file of EUROMOD policy scenarios
    public static final String EUROMODpolicyScheduleHeadingFilename = "Filename";
    public static final String EUROMODpolicyScheduleHeadingScenarioSystemYear = "Policy_System_Year";
    public static final String EUROMODpolicyScheduleHeadingScenarioYearBegins = "Policy_Start_Year";
    public static final String EUROMODpolicySchedulePlanHeadingDescription = "Description";
    //Names of donor attributes that depend on EUROMOD policy parameters
    public static final String DISPOSABLE_INCOME_VARIABLE_NAME = "DISPOSABLE_INCOME_MONTHLY";
    public static final String EMPLOYER_SOCIAL_INSURANCE_VARIABLE_NAME = "EMPLOYER_SOCIAL_INSURANCE_CONTRIBUTION_PER_HOUR";
    public static final String GROSS_EARNINGS_VARIABLE_NAME = "GROSS_EARNINGS_MONTHLY";
    public static final String ORIGINAL_INCOME_VARIABLE_NAME = "ORIGINAL_INCOME_MONTHLY";
    public static final String HOURLY_WAGE_VARIABLE_NAME = "HOURLY_WAGE";
    public static final String ILS_BENMT_NAME = "ILS_BENMT";
    public static final String ILS_BENNT_NAME = "ILS_BENNT";
    //public static final String SELF_EMPLOY_SOCIAL_INSURANCE_VARIABLE_NAME = "SELF_EMPLOY_SOC_INSUR_CONTR_PER_HOUR";
    public static final String HOURS_WORKED_WEEKLY = "HOURS_WORKED_WEEKLY";

    public static final double MIN_CAPITAL_INCOME_PER_MONTH = 0.0;
    public static final double MAX_CAPITAL_INCOME_PER_MONTH = 4000.0;
    public static final double MIN_PERSONAL_PENSION_PER_MONTH = 0.0;
    public static final double MAX_PERSONAL_PENSION_PER_MONTH = 15000.0;

    private static String taxDonorInputFileName;
    private static String populationInitialisationInputFileName;
    private static MultiKeyMap<Object, Double> populationGrowthRatiosByRegionYear;

    public static boolean saveImperfectTaxDBMatches = false;
    public static final int IMPERFECT_THRESHOLD = 5999;

    public static String eq5dConversionParameters = "lawrence";


    /////////////////////////////////////////////////////////////////// INITIALISATION OF DATA STRUCTURES //////////////////////////////////
    public static Map<Integer, String> EUROMODpolicySchedule = new TreeMap<Integer, String>();
    public static Map<Integer, Pair<String, Integer>> EUROMODpolicyScheduleSystemYearMap = new TreeMap<>(); // This map stores year from which policy applies, and then a Pair of <name of policy, policy system year as specified in EM>. This is used when uprating values from the policy system year to a current simulated year.
    private static MultiKeyMap<Object, Double> fertilityRateByRegionYear;
    private static Map<Integer, Double> fertilityRateByYear;
    private static MultiKeyCoefficientMap populationProjections;
    public static final int ALIGN_MIN_AGE_ASSUME_DEATH = 65;
    public static final int ALIGN_MAX_AGE_REQUIRE_MATCH = 65;
    private static int populationProjectionsMaxYear;
    private static int populationProjectionsMinYear;
    private static int populationProjectionsMaxAge;
    private static MultiKeyCoefficientMap benefitUnitVariableNames;

    //RMSE for linear regressions
    private static MultiKeyCoefficientMap coefficientMapRMSE;

    //Uprating factor
    private static boolean flagDefaultToTimeSeriesAverages;
    private static Double averageSavingReturns, averageDebtCostLow, averageDebtCostHigh;
    private static MultiKeyCoefficientMap upratingIndexMapRealGDP, mapRealGDPperCapita, upratingIndexMapInflation, socialCareProvisionTimeAdjustment,
            partnershipTimeAdjustment, studentsTimeAdjustment, fertilityTimeAdjustment,
            utilityTimeAdjustmentSingleMales, utilityTimeAdjustmentACMales, utilityTimeAdjustmentSingleFemales, utilityTimeAdjustmentACFemales,
            utilityTimeAdjustmentCouples, utilityTimeAdjustmentSingleDepMen, utilityTimeAdjustmentSingleDepWomen,
            upratingIndexMapRealWageGrowth, priceMapRealSavingReturns, priceMapRealDebtCostLow, priceMapRealDebtCostHigh,
            wageRateFormalSocialCare, socialCarePolicy, partneredShare,
            employedShareACMales, employedShareACFemales, employedShareSingleDepMales, employedShareSingleDepFemales,
            employedShareSingleMales, employedShareSingleFemales, employedShareCouples, studentShare;
    public static Map<Integer, Double> partnershipAlignAdjustment, fertilityAlignAdjustment;
    public static MultiKeyMap upratingFactorsMap = new MultiKeyMap<>();

    //Education level projections
    private static MultiKeyCoefficientMap projectionsHighEdu;			//Alignment projections for High Education
    private static MultiKeyCoefficientMap projectionsLowEdu;			//Alignment projections for Medium Education

    //Student share projections for alignment
    private static MultiKeyCoefficientMap studentShareProjections;		//Alignment projections for Student share of population

    //Employment alignment targets
    private static MultiKeyCoefficientMap employmentAlignment;

    //For marriage types:
    private static MultiKeyCoefficientMap marriageTypesFrequency;
    private static Map<Gender, MultiKeyMap<Region, Double>> marriageTypesFrequencyByGenderAndRegion;

    //Mean and covariances for parametric matching
    private static MultiKeyCoefficientMap meanCovarianceParametricMatching;

    private static MultiKeyCoefficientMap fixedRetireAge;
//	private static MultiKeyCoefficientMap rawProbSick;
    private static MultiKeyCoefficientMap unemploymentRates;
//	private static MultiKeyMap probSick;

    //MultivariateNormalDistribution of age and potential earnings differential to use in the parametric partnership process
    public final static boolean MARRIAGE_MATCH_TO_MEANS = false;
    public static double targetMeanWageDifferential, targetMeanAgeDifferential;
    private static MultivariateNormalDistribution wageAndAgeDifferentialMultivariateNormalDistribution;

    //Parameters for projecting lifetime incomes
    private static MultiKeyCoefficientMap equivalisedIncomeByGenderAgeYear; //Load as MultiKeyCoefficientMap as all values are in the Excel file and just need to be accessible
    private static int equivalisedIncomeMaxYear;
    private static int equivalisedIncomeMinYear;
    private static int equivalisedIncomeMaxAge;
    private static MultiKeyCoefficientMap equivalisedIncomeCDFData;
    private static MultiKeyCoefficientMap equivalisedIncomeCDFData2;
    private static EquivalisedIncomeCDF equivalisedIncomeCDF;
    private static EquivalisedIncomeCDF equivalisedIncomeCDF2;
    private static MultiKeyCoefficientMap coeffCovarianceEquivalisedIncomeMales;
    private static MultiKeyCoefficientMap coeffCovarianceEquivalisedIncomeFemales;
    private static MultiKeyCoefficientMap coeffCovarianceEquivalisedIncomeDynamics;
    private static MultiKeyCoefficientMap coeffCovarianceEquivalisedIncomeDynamics2;
    private static LinearRegression regEquivalisedIncomeMales;
    private static LinearRegression regEquivalisedIncomeFemales;
    private static LinearRegression regEquivalisedIncomeDynamics;
    private static LinearRegression regEquivalisedIncomeDynamics2;

    //Mortality, fertility, and unemployment tables for the intertemporal optimisation model
    private static MultiKeyCoefficientMap mortalityProbabilityByGenderAgeYear; //Load as MultiKeyCoefficientMap as all values are in the Excel file and just need to be accessible
    private static int mortalityProbabilityMaxYear;
    private static int mortalityProbabilityMinYear;
    private static int mortalityProbabilityMaxAge;
    private static MultiKeyCoefficientMap fertilityProjectionsByYear; //NB: these currently only go up to 2043
    public static int fertilityProjectionsMaxYear;
    public static int fertilityProjectionsMinYear;
    private static MultiKeyCoefficientMap unemploymentRatesMaleGraduatesByAgeYear; //Load as MultiKeyCoefficientMap as all values are in the Excel file and just need to be accessible
    private static int unemploymentRatesMaleGraduatesMaxYear;
    private static int unemploymentRatesMaleGraduatesMinYear;
    private static int unemploymentRatesMaleGraduatesMaxAge;
    private static MultiKeyCoefficientMap unemploymentRatesMaleNonGraduatesByAgeYear; //Load as MultiKeyCoefficientMap as all values are in the Excel file and just need to be accessible
    private static int unemploymentRatesMaleNonGraduatesMaxYear;
    private static int unemploymentRatesMaleNonGraduatesMinYear;
    private static int unemploymentRatesMaleNonGraduatesMaxAge;
    private static MultiKeyCoefficientMap unemploymentRatesFemaleGraduatesByAgeYear; //Load as MultiKeyCoefficientMap as all values are in the Excel file and just need to be accessible
    private static int unemploymentRatesFemaleGraduatesMaxYear;
    private static int unemploymentRatesFemaleGraduatesMinYear;
    private static int unemploymentRatesFemaleGraduatesMaxAge;
    private static MultiKeyCoefficientMap unemploymentRatesFemaleNonGraduatesByAgeYear; //Load as MultiKeyCoefficientMap as all values are in the Excel file and just need to be accessible
    private static int unemploymentRatesFemaleNonGraduatesMaxYear;
    private static int unemploymentRatesFemaleNonGraduatesMinYear;
    private static int unemploymentRatesFemaleNonGraduatesMaxAge;

    //Number of employments on full and flexible furlough from HMRC statistics, used as regressors in the Covid-19 module
    private static MultiKeyCoefficientMap employmentsFurloughedFull;
    private static MultiKeyCoefficientMap employmentsFurloughedFlex;

    /////////////////////////////////////////////////////////////////// REGRESSION COEFFICIENTS //////////////////////////////////////////

    //Health
    private static MultiKeyCoefficientMap coeffCovarianceHealthH1;
    private static MultiKeyCoefficientMap coeffCovarianceHealthH2; //Prob. long-term sick or disabled

    //Social care
    // private static MultiKeyCoefficientMap coeffCovarianceSocialCareS1b; // retired process
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2a; // prob of needing social care 65+
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2b;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2c;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2d;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2e;
    // private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2f; // retired process
    // private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2g; // retired process
    // private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2h; // retired process
    // private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2i; // retired process
    // private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2j; // retired process
    // private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2k; // retired process
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS3a;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS3b;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS3c;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS3d;
    // private static MultiKeyCoefficientMap coeffCovarianceSocialCareS3e; // retired process

    //Unemployment
    private static MultiKeyCoefficientMap coeffCovarianceUnemploymentU1a;
    private static MultiKeyCoefficientMap coeffCovarianceUnemploymentU1b;
    private static MultiKeyCoefficientMap coeffCovarianceUnemploymentU1c;
    private static MultiKeyCoefficientMap coeffCovarianceUnemploymentU1d;

    //Financial distress
    private static MultiKeyCoefficientMap coeffCovarianceFinancialDistress;

    //Mental health
    private static MultiKeyCoefficientMap coeffCovarianceHM1Level; //Step 1 coefficients for mental health
    private static MultiKeyCoefficientMap coeffCovarianceHM2LevelMales; //Step 2 coefficients for mental health for males
    private static MultiKeyCoefficientMap coeffCovarianceHM2LevelFemales;

    private static MultiKeyCoefficientMap coeffCovarianceHM1Case;
    private static MultiKeyCoefficientMap coeffCovarianceHM2CaseMales;
    private static MultiKeyCoefficientMap coeffCovarianceHM2CaseFemales;

    //Health
    private static MultiKeyCoefficientMap coeffCovarianceDHE_MCS1;
    private static MultiKeyCoefficientMap coeffCovarianceDHE_MCS2Males;
    private static MultiKeyCoefficientMap coeffCovarianceDHE_MCS2Females;

    private static MultiKeyCoefficientMap coeffCovarianceDHE_PCS1;
    private static MultiKeyCoefficientMap coeffCovarianceDHE_PCS2Males;
    private static MultiKeyCoefficientMap coeffCovarianceDHE_PCS2Females;

    private static MultiKeyCoefficientMap coeffCovarianceDLS1;
    private static MultiKeyCoefficientMap coeffCovarianceDLS2Males;
    private static MultiKeyCoefficientMap coeffCovarianceDLS2Females;

    private static MultiKeyCoefficientMap coeffCovarianceEQ5D;

    //Education
    private static MultiKeyCoefficientMap coeffCovarianceEducationE1a;
    private static MultiKeyCoefficientMap coeffCovarianceEducationE1b;
    private static MultiKeyCoefficientMap coeffCovarianceEducationE2;

    //Partnership
    private static MultiKeyCoefficientMap coeffCovariancePartnershipU1; //Probit enter partnership if in continuous education
    // private static MultiKeyCoefficientMap coeffCovariancePartnershipU1b; //Probit enter partnership if not in continuous education
    private static MultiKeyCoefficientMap coeffCovariancePartnershipU2; //Probit exit partnership (females)

    //Partnership for Italy
    private static MultiKeyCoefficientMap coeffCovariancePartnershipITU1; //Probit enter partnership for Italy
    private static MultiKeyCoefficientMap coeffCovariancePartnershipITU2; //Probit exit partnership for Italy

    //Fertility
    private static MultiKeyCoefficientMap coeffCovarianceFertilityF1; //Probit fertility if in continuous education

    //Income
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI1a; //Linear regression non-employment non-benefit income if in continuous education
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI1b; //Linear regression non-employment non-benefit income if not in continuous education
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI2b;
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI3a; //Capital income if in continuous education
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI3b; //Capital income if not in continuous education
    // private static MultiKeyCoefficientMap coeffCovarianceIncomeI3c; //Pension income for those aged over 50 who are not in continuous education
    // private static MultiKeyCoefficientMap coeffCovarianceIncomeI4a;
    // private static MultiKeyCoefficientMap coeffCovarianceIncomeI4b; //Pension income for those already retired
    // private static MultiKeyCoefficientMap coeffCovarianceIncomeI5a; //Pension income for those moving from employment to retirement
    // private static MultiKeyCoefficientMap coeffCovarianceIncomeI6a_selection, coeffCovarianceIncomeI6b_amount; // Selection equation for receiving pension income for those in retirement (I6a) and amount in levels (I6b), in the initial simulated year
    // private static MultiKeyCoefficientMap coeffCovarianceIncomeI3a_selection; //Probability of receiving capital income if in continuous education
    // private static MultiKeyCoefficientMap coeffCovarianceIncomeI3b_selection; //Probability of receiving capital income if not in continuous education
    // private static MultiKeyCoefficientMap coeffCovarianceIncomeI5a_selection; //Selection equation for receiving pension income for those moving from employment to retirement

    //Homeownership
    private static MultiKeyCoefficientMap coeffCovarianceHomeownership; //Probit regression assigning homeownership status

    //Wages
    private static MultiKeyCoefficientMap coeffCovarianceWagesMales, coeffCovarianceWagesMalesNE, coeffCovarianceWagesMalesE;
    private static MultiKeyCoefficientMap coeffCovarianceWagesFemales, coeffCovarianceWagesFemalesNE, coeffCovarianceWagesFemalesE;

    //Labour Market
    private static MultiKeyCoefficientMap coeffCovarianceEmploymentSelectionMales, coeffCovarianceEmploymentSelectionMalesNE, coeffCovarianceEmploymentSelectionMalesE;
    private static MultiKeyCoefficientMap coeffCovarianceEmploymentSelectionFemales, coeffCovarianceEmploymentSelectionFemalesNE, coeffCovarianceEmploymentSelectionFemalesE;
    private static MultiKeyCoefficientMap coeffLabourSupplyUtilityMales;
    private static MultiKeyCoefficientMap coeffLabourSupplyUtilityFemales;
    private static MultiKeyCoefficientMap coeffLabourSupplyUtilityACMales; //Adult children, male
    private static MultiKeyCoefficientMap coeffLabourSupplyUtilityACFemales; //Adult children, female
    private static MultiKeyCoefficientMap coeffLabourSupplyUtilityCouples;
    private static MultiKeyCoefficientMap coeffLabourSupplyUtilitySingleDep;

    // coefficients for Covid-19 labour supply models below
    // Initialisation
    private static MultiKeyCoefficientMap coeffCovarianceC19LS_SE;
    // Transitions
    // From lagged state "employed"
    private static MultiKeyCoefficientMap coeffC19LS_E1_NE; // For multi probit regressions, have to specify coefficients for each possible outcome, with "no changes" being the baseline. NE is not-employed.
    private static MultiKeyCoefficientMap coeffC19LS_E1_SE; // self-employed
    private static MultiKeyCoefficientMap coeffC19LS_E1_FF; // furloughed full
    private static MultiKeyCoefficientMap coeffC19LS_E1_FX; // furloughed flex
    private static MultiKeyCoefficientMap coeffC19LS_E1_SC; // some changes
    // From lagged state "furloughed full"
    private static MultiKeyCoefficientMap coeffC19LS_FF1_E; // employed
    private static MultiKeyCoefficientMap coeffC19LS_FF1_FX; // furloughed flex
    private static MultiKeyCoefficientMap coeffC19LS_FF1_NE; // not-employed
    private static MultiKeyCoefficientMap coeffC19LS_FF1_SE; // self-employed
    // From lagged state "furloughed flex"
    private static MultiKeyCoefficientMap coeffC19LS_FX1_E; // employed
    private static MultiKeyCoefficientMap coeffC19LS_FX1_FF; // furloughed flex
    private static MultiKeyCoefficientMap coeffC19LS_FX1_NE; // not-employed
    private static MultiKeyCoefficientMap coeffC19LS_FX1_SE; // self-employed
    // From lagged state "self-employed"
    private static MultiKeyCoefficientMap coeffC19LS_S1_E; // employed
    private static MultiKeyCoefficientMap coeffC19LS_S1_NE; // not-employed
    // From lagged state "not-employed"
    private static MultiKeyCoefficientMap coeffC19LS_U1_E; // employed
    private static MultiKeyCoefficientMap coeffC19LS_U1_SE; // self-employed

    // Define maps that the above coefficients are bundled into
    private static Map<Les_transitions_E1, MultiKeyCoefficientMap> coeffC19LS_E1Map;
    private static Map<Les_transitions_FF1, MultiKeyCoefficientMap> coeffC19LS_FF1Map;
    private static Map<Les_transitions_FX1, MultiKeyCoefficientMap> coeffC19LS_FX1Map;
    private static Map<Les_transitions_S1, MultiKeyCoefficientMap> coeffC19LS_S1Map;
    private static Map<Les_transitions_U1, MultiKeyCoefficientMap> coeffC19LS_U1Map;

    // Hours of work
    private static MultiKeyCoefficientMap coeffC19LS_E2a;
    private static MultiKeyCoefficientMap coeffC19LS_E2b;
    private static MultiKeyCoefficientMap coeffC19LS_F2a;
    private static MultiKeyCoefficientMap coeffC19LS_F2b;
    private static MultiKeyCoefficientMap coeffC19LS_F2c;
    private static MultiKeyCoefficientMap coeffC19LS_S2a;
    private static MultiKeyCoefficientMap coeffC19LS_U2a;

    // Probability of receiving SEISS
    private static MultiKeyCoefficientMap coeffC19LS_S3;

    //Leaving parental home
    private static  MultiKeyCoefficientMap coeffCovarianceLeaveHomeP1;

    //Retirement
    private static MultiKeyCoefficientMap coeffCovarianceRetirementR1a;
    private static MultiKeyCoefficientMap coeffCovarianceRetirementR1b;

    //Childcare

    public static final int MAX_CHILD_AGE_FOR_FORMAL_CARE = 14;
    private static MultiKeyCoefficientMap coeffCovarianceChildcareC1a;
    private static MultiKeyCoefficientMap coeffCovarianceChildcareC1b;

    ///////////////////////////////////////////////////////////STATISTICS FOR VALIDATION/////////////////////////////////////////////
    //Share of students by age
    private static MultiKeyCoefficientMap validationStudentsByAge;

    //Share of students by demRgn
    private static MultiKeyCoefficientMap validationStudentsByRegion;

    //Education level of over 17 year olds
    private static MultiKeyCoefficientMap validationEducationLevel;

    //Education level by age group
    private static MultiKeyCoefficientMap validationEducationLevelByAge;

    //Education level by demRgn
    private static MultiKeyCoefficientMap validationEducationLevelByRegion;

    //Share of couple by demRgn
    private static MultiKeyCoefficientMap validationPartneredShareByRegion;

    //Share of disabled by age
    private static MultiKeyCoefficientMap validationDisabledByAge;

    private static MultiKeyCoefficientMap validationDisabledByGender;

    //Health by age
    private static MultiKeyCoefficientMap validationHealthByAge;

    //Mental health by age and demSex
    private static MultiKeyCoefficientMap validationMentalHealthByAge;

    //Psychological distress cases by age and demSex
    private static MultiKeyCoefficientMap validationPsychDistressByAge, validationPsychDistressByAgeLow, validationPsychDistressByAgeMed, validationPsychDistressByAgeHigh;


    // Health
    private static MultiKeyCoefficientMap validationHealthMCSByAge, validationHealthPCSByAge;

    // Life Satisfaction
    private static MultiKeyCoefficientMap validationLifeSatisfactionByAge;

    //Employment by demSex
    private static MultiKeyCoefficientMap validationEmploymentByGender;

    //Employment by demSex and age
    private static MultiKeyCoefficientMap validationEmploymentByAgeAndGender;

    //Employment by maternity
    private static MultiKeyCoefficientMap validationEmploymentByMaternity;

    //Employment by demSex and demRgn
    private static MultiKeyCoefficientMap validationEmploymentByGenderAndRegion;

    private static MultiKeyCoefficientMap validationLabourSupplyByEducation;

    //Activity status
    private static MultiKeyCoefficientMap validationActivityStatus;

    //Homeownership status for benefit units
    private static MultiKeyCoefficientMap validationHomeownershipBenefitUnits;

    //Gross earnings yearly by education and demSex (for employed persons)
    private static MultiKeyCoefficientMap validationGrossEarningsByGenderAndEducation;

    //Hourly wages by education and demSex (for employed persons)
    private static MultiKeyCoefficientMap validationLhwByGenderAndEducation;

    //Hours worked weekly by education and demSex (for employed persons)
    private static MultiKeyCoefficientMap hourlyWageByGenderAndEducation;

    /////////////////////////////////////////////////////////////////// REGRESSION OBJECTS //////////////////////////////////////////

    //Health
    private static GeneralisedOrderedRegression regHealthH1;
    private static GeneralisedOrderedRegression regHealthH1b;
    private static BinomialRegression regHealthH2;

    //Social care
    // private static LinearRegression regSocialCareS1b; // retired process
    private static BinomialRegression regNeedCareS2a;
    private static BinomialRegression regReceiveCareS2b;
    private static MultinomialRegression regSocialCareMarketS2c;
    private static LinearRegression regInformalCareHoursS2d;
    private static LinearRegression regFormalCareHoursS2e;
    // private static MultinomialRegression regNotPartnerInformalCareS2f; // retired process
    // private static LinearRegression regPartnerCareHoursS2g; // retired process
    // private static LinearRegression regDaughterCareHoursS2h; // retired process
    // private static LinearRegression regSonCareHoursS2i; // retired process
    // private static LinearRegression regOtherCareHoursS2j; // retired process
    // private static LinearRegression regFormalCareHoursS2k; // retired process
    private static BinomialRegression regCarePartnerProvCareToOtherS3a;
    private static BinomialRegression regNoCarePartnerProvCareToOtherS3b;
    private static LinearRegression regCareHoursProvS3c;
    private static LinearRegression regCareHoursProvS3d;
    // private static LinearRegression regCareHoursProvS3e; // retired process

    //Unemployment
    private static BinomialRegression regUnemploymentMaleGraduateU1a;
    private static BinomialRegression regUnemploymentMaleNonGraduateU1b;
    private static BinomialRegression regUnemploymentFemaleGraduateU1c;
    private static BinomialRegression regUnemploymentFemaleNonGraduateU1d;

    // Financial distress
    private static BinomialRegression regFinancialDistress;

    //Health mental
    private static LinearRegression regHealthHM1Level;
    private static LinearRegression regHealthHM2LevelMales;
    private static LinearRegression regHealthHM2LevelFemales;

    private static OrderedRegression regHealthHM1Case;
    private static LinearRegression regHealthHM2CaseMales;
    private static LinearRegression regHealthHM2CaseFemales;

    //Health
    private static LinearRegression regHealthMCS1;
    private static LinearRegression regHealthMCS2Males;
    private static LinearRegression regHealthMCS2Females;

    private static LinearRegression regHealthPCS1;
    private static LinearRegression regHealthPCS2Males;
    private static LinearRegression regHealthPCS2Females;

    private static LinearRegression regLifeSatisfaction1;
    private static LinearRegression regLifeSatisfaction2Males;
    private static LinearRegression regLifeSatisfaction2Females;

    private static LinearRegression regHealthEQ5D;

    //Education
    private static BinomialRegression regEducationE1a;
    private static BinomialRegression regEducationE1b;
    private static GeneralisedOrderedRegression regEducationE2;

    //Partnership
    private static BinomialRegression regPartnershipU1;
    private static BinomialRegression regPartnershipU1b;
    private static BinomialRegression regPartnershipU2;

    private static BinomialRegression regPartnershipITU1;
    private static BinomialRegression regPartnershipITU2;

    //Fertility
    private static BinomialRegression regFertilityF1;

    //Income
    private static BinomialRegression regIncomeI1a;
    private static LinearRegression regIncomeI1b;
    private static LinearRegression regIncomeI2b;
    private static BinomialRegression regIncomeI3a;
    private static LinearRegression regIncomeI3b;
    // private static LinearRegression regIncomeI3c;
    // private static LinearRegression regIncomeI4a;
    // private static LinearRegression regIncomeI4b;
    // private static LinearRegression regIncomeI5a;
    // private static LinearRegression regIncomeI6b_amount;
    // private static BinomialRegression regIncomeI3a_selection;
    // private static BinomialRegression regIncomeI3b_selection;
    // private static BinomialRegression regIncomeI5a_selection;
    // private static BinomialRegression regIncomeI6a_selection;

    //Homeownership
    private static BinomialRegression regHomeownershipHO1a;

    private static MultinomialRegression<Education> regEducationLevel;

    //New simple educ level
    private static MultinomialRegression<Education> regSimpleEducLevel;

    //For Labour market
    private static LinearRegression regWagesMales;
    private static LinearRegression regWagesMalesE;
    private static LinearRegression regWagesMalesNE;
    private static LinearRegression regWagesFemales, regWagesFemalesE, regWagesFemalesNE;

    private static LinearRegression regEmploymentSelectionMale, regEmploymentSelectionMaleE, regEmploymentSelectionMaleNE;		//To calculate Inverse Mills Ratio for Heckman Two-Step Procedure
    private static LinearRegression regEmploymentSelectionFemale, regEmploymentSelectionFemaleE, regEmploymentSelectionFemaleNE;	//To calculate Inverse Mills Ratio for Heckman Two-Step Procedure
    private static NormalDistribution standardNormalDistribution;	//To sample the inverse mills ratio

    private static LinearRegression regLabourSupplyUtilityMales;
    private static LinearRegression regLabourSupplyUtilityFemales;
    private static LinearRegression regLabourSupplyUtilitySingleDep;
    private static LinearRegression regLabourSupplyUtilityACMales;
    private static LinearRegression regLabourSupplyUtilityACFemales;
    private static LinearRegression regLabourSupplyUtilityCouples;

    // Covid-19 labour transitions regressions below
    // Initialisation
    private static BinomialRegression regC19LS_SE; // Assigns self-employed status in the simulated population
    // Transitions
    private static MultinomialRegression<Les_transitions_E1> regC19LS_E1;  // Models transitions from employment
    private static MultinomialRegression<Les_transitions_FF1> regC19LS_FF1;  // Models transitions from furlough full
    private static MultinomialRegression<Les_transitions_FX1> regC19LS_FX1;  // Models transitions from furlough flex
    private static MultinomialRegression<Les_transitions_S1> regC19LS_S1;  // Models transitions from self-employment
    private static MultinomialRegression<Les_transitions_U1> regC19LS_U1;  // Models transitions from non-employment
    // Hours of work
    private static LinearRegression regC19LS_E2a;
    private static LinearRegression regC19LS_E2b;
    private static LinearRegression regC19LS_F2a;
    private static LinearRegression regC19LS_F2b;
    private static LinearRegression regC19LS_F2c;
    private static LinearRegression regC19LS_S2a;
    private static LinearRegression regC19LS_U2a;

    // Probability of SEISS
    private static BinomialRegression regC19LS_S3;

    //Leaving parental home
    private static BinomialRegression regLeaveHomeP1a;

    //Retirement
    private static BinomialRegression regRetirementR1a;
    private static BinomialRegression regRetirementR1b;

    //Childcare
    private static BinomialRegression regChildcareC1a;
    private static LinearRegression regChildcareC1b;

    private static BinomialRegression regBirthFemales;
    private static BinomialRegression regUnionFemales;
    private static Set<Region> countryRegions;
    private static Map<Region, Double> unemploymentRatesByRegion;
    public static boolean isFixTimeTrend;
    public static Integer timeTrendStopsIn;
    public static boolean flagFormalChildcare;
    public static boolean flagSocialCare;
    public static boolean flagSuppressChildcareCosts;
    public static boolean flagSuppressSocialCareCosts;
    public static boolean donorPoolAveraging;
    public static boolean lifetimeIncomeImpute;

    public static double realInterestRateInnov;
    public static double disposableIncomeFromLabourInnov;


    // Add missing alignment regressors with zero values so alignment can adjust them at runtime.
    private static void addFixedCostRegressors(MultiKeyCoefficientMap map, List<String> regressors) {
        for (String reg : regressors) {
            if ((reg.equals("AlignmentFixedCostMen") || reg.equals("AlignmentFixedCostWomen")
                    || reg.equals("AlignmentSingleDepMen") || reg.equals("AlignmentSingleDepWomen"))
                    && map.getValue(reg) == null) {
                // Infer the format from an existing coefficient
                Object sample = map.getValue("IncomeDiv100");
                if (sample instanceof Object[]) {
                    map.putValue(reg, new Object[]{0.0});
                } else {
                    map.putValue(reg, 0.0);
                }
            }
        }
    }
    /**
     *
     * METHOD TO LOAD PARAMETERS FOR GIVEN COUNTRY
     * @param country
     */
    public static void loadParameters(Country country, int maxAgeModel, boolean enableIntertemporalOptimisations,
                                      boolean projectFormalChildcare, boolean projectSocialCare, boolean donorPoolAveraging1,
                                      boolean fixTimeTrend, boolean defaultToTimeSeriesAverages, boolean taxDBMatches,
                                      Integer timeTrendStops, int startYearModel, int endYearModel, double interestRateInnov1,
                                      double disposableIncomeFromLabourInnov1, boolean flagSuppressChildcareCosts1,
                                      boolean flagSuppressSocialCareCosts1, boolean lifetimeIncomeImpute1) {

        // display a dialog box to let the user know what is happening
        System.out.println("Loading model parameters");
        System.out.flush();

        maxAge = maxAgeModel;
        startYear = startYearModel;
        endYear = endYearModel;

        EUROMODpolicySchedule = calculateEUROMODpolicySchedule(country);
        taxDonorInputFileName = "population_" + country;
        populationInitialisationInputFileName = "population_initial_" + country;
        setCountryRegions(country);
        setEnableIntertemporalOptimisations(enableIntertemporalOptimisations);
        setProjectLiquidWealth();
        String countryString = country.toString();
        loadTimeSeriesFactorMaps(country);
        instantiateAlignmentMaps();

        // scenario parameters
        if (country.equals(Country.IT)) {
            SAVINGS_RATE = 0.056;
        } else {
            SAVINGS_RATE = 0.056;
        }
        saveImperfectTaxDBMatches = taxDBMatches;

        flagDefaultToTimeSeriesAverages = defaultToTimeSeriesAverages;
        isFixTimeTrend = fixTimeTrend;
        timeTrendStopsIn = timeTrendStops;
        flagFormalChildcare = projectFormalChildcare;
        flagSocialCare = projectSocialCare;
        flagSuppressChildcareCosts = flagSuppressChildcareCosts1;
        flagSuppressSocialCareCosts = flagSuppressSocialCareCosts1;
        donorPoolAveraging = donorPoolAveraging1;
        realInterestRateInnov = interestRateInnov1;
        disposableIncomeFromLabourInnov = disposableIncomeFromLabourInnov1;
        lifetimeIncomeImpute = lifetimeIncomeImpute1;
        fixedRetireAge = ExcelAssistant.loadCoefficientMap(getInputDirectory() + "scenario_retirementAgeFixed.xlsx", countryString, 1);

        // alignment parameters
        populationProjections = ExcelAssistant.loadCoefficientMap(getInputDirectory() + "align_popProjections.xlsx", countryString, 3);
        setMapBounds(MapBounds.Population, countryString);

        //Alignment of education levels
        projectionsHighEdu = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "align_educLevel.xlsx", "High", 1);
        projectionsLowEdu = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "align_educLevel.xlsx", "Low", 1);

        studentShareProjections = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "align_student_under30.xlsx", countryString, 1);

        //Employment alignment
        employmentAlignment = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "align_employment.xlsx", countryString, 2);

        //Marriage types frequencies:
        marriageTypesFrequency = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "marriageTypes2.xlsx", countryString, 2);
        marriageTypesFrequencyByGenderAndRegion = new LinkedHashMap<Gender, MultiKeyMap<Region, Double>>();	//Create a map of maps to store the frequencies

        //Mortality rates
        mortalityProbabilityByGenderAgeYear = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "projections_mortality.xlsx", "MortalityByGenderAgeYear", 2);
        setMapBounds(MapBounds.Mortality, countryString);

        //Fertility rates:
        fertilityProjectionsByYear = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "projections_fertility.xlsx", "FertilityByYear", 1);
        setMapBounds(MapBounds.Fertility, countryString);

        //Lifetime incomes
        equivalisedIncomeByGenderAgeYear = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_lifetime_incomes.xlsx", "geometric_means", 2);
        setMapBounds(MapBounds.EquivalisedIncome, countryString);
        equivalisedIncomeCDFData = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_lifetime_incomes.xlsx", "LI2b", 1, 1);
        equivalisedIncomeCDF = new EquivalisedIncomeCDF(equivalisedIncomeCDFData);
        equivalisedIncomeCDFData2 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_lifetime_incomes.xlsx", "LI3b", 1);
        equivalisedIncomeCDF2 = new EquivalisedIncomeCDF(equivalisedIncomeCDFData2);
        mapRealGDPperCapita = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_lifetime_incomes.xlsx", "gdp_pc", 1, 1);
        coeffCovarianceEquivalisedIncomeMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_lifetime_incomes.xlsx", "LI1a", 1);
        coeffCovarianceEquivalisedIncomeFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_lifetime_incomes.xlsx", "LI1b", 1);
        coeffCovarianceEquivalisedIncomeDynamics = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_lifetime_incomes.xlsx", "LI2a", 1);
        coeffCovarianceEquivalisedIncomeDynamics2 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_lifetime_incomes.xlsx", "LI3a", 1);

        //Unemployment rates
        unemploymentRatesMaleGraduatesByAgeYear = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", "RatesMaleGraduates", 1);
        setMapBounds(MapBounds.UnemploymentMaleGraduates, countryString);
        unemploymentRatesMaleNonGraduatesByAgeYear = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", "RatesMaleNonGraduates", 1);
        setMapBounds(MapBounds.UnemploymentMaleNonGraduates, countryString);
        unemploymentRatesFemaleGraduatesByAgeYear = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", "RatesFemaleGraduates", 1);
        setMapBounds(MapBounds.UnemploymentFemaleGraduates, countryString);
        unemploymentRatesFemaleNonGraduatesByAgeYear = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", "RatesFemaleNonGraduates", 1);
        setMapBounds(MapBounds.UnemploymentFemaleNonGraduates, countryString);

        //RMSE
        coefficientMapRMSE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_RMSE.xlsx", countryString, 1);

        //Employments on furlough
        employmentsFurloughedFull = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "scenario_employments_furloughed.xlsx", "FullFurlough", 2);
        employmentsFurloughedFlex = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "scenario_employments_furloughed.xlsx", "FlexibleFurlough", 2);

        //The Raw maps contain the estimates and covariance matrices, from which we bootstrap at the start of each simulation

        //Heckman model employment selection
        coeffCovarianceEmploymentSelectionMalesE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_employment_selection.xlsx", "W1mb-sel", 1);
        coeffCovarianceEmploymentSelectionMalesNE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_employment_selection.xlsx", "W1ma-sel", 1);
        coeffCovarianceEmploymentSelectionFemalesE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_employment_selection.xlsx", "W1fb-sel", 1);
        coeffCovarianceEmploymentSelectionFemalesNE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_employment_selection.xlsx", "W1fa-sel", 1);

        // Wages
        coeffCovarianceWagesMalesE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_wages.xlsx", "W1mb", 1);
        coeffCovarianceWagesMalesNE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_wages.xlsx", "W1ma", 1);
        coeffCovarianceWagesFemalesE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_wages.xlsx", "W1fb", 1);
        coeffCovarianceWagesFemalesNE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_wages.xlsx", "W1fa", 1);

        //Labour Supply coefficients from Zhechun's estimates on the EM input data
        //Employment alignment adjusts *fixed-cost* -> add the relevant alignment fixed-cost regressors to each subgroup
        coeffLabourSupplyUtilityMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourSupplyUtility.xlsx", "Single_Males", 1);

        coeffLabourSupplyUtilityFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourSupplyUtility.xlsx", "Single_Females", 1);

        coeffLabourSupplyUtilitySingleDep = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourSupplyUtility.xlsx", "SingleDep", 1);

        coeffLabourSupplyUtilityACMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourSupplyUtility.xlsx", "SingleAC_Males", 1);

        coeffLabourSupplyUtilityACFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourSupplyUtility.xlsx", "SingleAC_Females", 1);

        coeffLabourSupplyUtilityCouples = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourSupplyUtility.xlsx", "Couples", 1);

        //Heckman model employment selection
        coeffCovarianceEmploymentSelectionMalesE = ExcelAssistant.loadCoefficientMap(getInputDirectory() + "reg_employment_selection.xlsx", "W1mb-sel", 1);
        coeffCovarianceEmploymentSelectionMalesNE = ExcelAssistant.loadCoefficientMap(getInputDirectory() + "reg_employment_selection.xlsx", "W1ma-sel", 1);
        coeffCovarianceEmploymentSelectionFemalesE = ExcelAssistant.loadCoefficientMap(getInputDirectory() + "reg_employment_selection.xlsx", "W1fb-sel", 1);
        coeffCovarianceEmploymentSelectionFemalesNE = ExcelAssistant.loadCoefficientMap(getInputDirectory() + "reg_employment_selection.xlsx", "W1fa-sel", 1);

        // Load coefficients for Covid-19 labour supply models
        // Coefficients for process assigning simulated people to self-employment
        coeffCovarianceC19LS_SE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_SE", 1);

        // Transitions from lagged state: employed
        coeffC19LS_E1_NE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_E1_NE", 1);
        coeffC19LS_E1_SE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_E1_SE", 1);
        coeffC19LS_E1_FF = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_E1_FF", 1);
        coeffC19LS_E1_FX = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_E1_FX", 1);
        coeffC19LS_E1_SC = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_E1_SC", 1);

        // Transitions from lagged state: furloughed full
        coeffC19LS_FF1_E = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_FF1_E", 1);
        coeffC19LS_FF1_FX = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_FF1_FX", 1);
        coeffC19LS_FF1_NE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_FF1_NE", 1);
        coeffC19LS_FF1_SE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_FF1_SE", 1);

        // Transitions from lagged state: furloughed flex
        coeffC19LS_FX1_E = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_FX1_E", 1);
        coeffC19LS_FX1_FF = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_FX1_FF", 1);
        coeffC19LS_FX1_NE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_FX1_NE", 1);
        coeffC19LS_FX1_SE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_FX1_SE", 1);

        // Transitions from lagged state: self-employed
        coeffC19LS_S1_E = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_S1_E", 1);
        coeffC19LS_S1_NE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_S1_NE", 1);

        // Transitions from lagged state: not-employed
        coeffC19LS_U1_E = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_U1_E", 1);
        coeffC19LS_U1_SE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_U1_SE", 1);

        // For multi logit regressions, put coefficients loaded below into maps
        coeffC19LS_E1Map = new LinkedHashMap<>(); //Add only categories from Les_transitions_E1 enum which are possible destinations for transitions from employment
        coeffC19LS_E1Map.put(Les_transitions_E1.NotEmployed, coeffC19LS_E1_NE);
        coeffC19LS_E1Map.put(Les_transitions_E1.SelfEmployed, coeffC19LS_E1_SE);
        coeffC19LS_E1Map.put(Les_transitions_E1.FurloughedFull, coeffC19LS_E1_FF);
        coeffC19LS_E1Map.put(Les_transitions_E1.FurloughedFlex, coeffC19LS_E1_FX);
        coeffC19LS_E1Map.put(Les_transitions_E1.SomeChanges, coeffC19LS_E1_SC);

        coeffC19LS_FF1Map = new LinkedHashMap<>(); //Add only categories from Les_transitions_FF1 enum which are possible destinations for transitions from furlough full
        coeffC19LS_FF1Map.put(Les_transitions_FF1.Employee, coeffC19LS_FF1_E);
        coeffC19LS_FF1Map.put(Les_transitions_FF1.FurloughedFlex, coeffC19LS_FF1_FX);
        coeffC19LS_FF1Map.put(Les_transitions_FF1.NotEmployed, coeffC19LS_FF1_NE);
        coeffC19LS_FF1Map.put(Les_transitions_FF1.SelfEmployed, coeffC19LS_FF1_SE);

        coeffC19LS_FX1Map = new LinkedHashMap<>(); //Add only categories from Les_transitions_FF1 enum which are possible destinations for transitions from furlough flex
        coeffC19LS_FX1Map.put(Les_transitions_FX1.Employee, coeffC19LS_FX1_E);
        coeffC19LS_FX1Map.put(Les_transitions_FX1.FurloughedFull, coeffC19LS_FX1_FF);
        coeffC19LS_FX1Map.put(Les_transitions_FX1.NotEmployed, coeffC19LS_FX1_NE);
        coeffC19LS_FX1Map.put(Les_transitions_FX1.SelfEmployed, coeffC19LS_FX1_SE);

        coeffC19LS_S1Map = new LinkedHashMap<>(); //Add only categories from Les_transitions_S1 enum which are possible destinations for transitions from self-employment
        coeffC19LS_S1Map.put(Les_transitions_S1.Employee, coeffC19LS_S1_E);
        coeffC19LS_S1Map.put(Les_transitions_S1.NotEmployed, coeffC19LS_S1_NE);

        coeffC19LS_U1Map = new LinkedHashMap<>(); //Add only categories from Les_transitions_U1 enum which are possible destinations for transitions from non-employment
        coeffC19LS_U1Map.put(Les_transitions_U1.Employee, coeffC19LS_U1_E);
        coeffC19LS_U1Map.put(Les_transitions_U1.SelfEmployed, coeffC19LS_U1_SE);

        // Coefficients for new working hours
        coeffC19LS_E2a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_E2a", 1);
        coeffC19LS_E2b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_E2b", 1);
        coeffC19LS_F2a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_F2a", 1);
        coeffC19LS_F2b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_F2b", 1);
        coeffC19LS_F2c = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_F2c", 1);
        coeffC19LS_S2a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_S2a", 1);
        coeffC19LS_U2a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_U2a", 1);

        // Coefficients for probability of SEISS
        coeffC19LS_S3 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", "C19LS_S3", 1);

        //Health
        coeffCovarianceHealthH1 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health.xlsx", "H1", 1);
        coeffCovarianceHealthH2 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health.xlsx", "H2", 1);

        //Social care
        // coeffCovarianceSocialCareS1b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S1b", 1); // retired process
        coeffCovarianceSocialCareS2a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S2a", 1);
        coeffCovarianceSocialCareS2b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S2b", 1);
        coeffCovarianceSocialCareS2c = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S2c", 1);
        coeffCovarianceSocialCareS2d = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S2d", 1);
        coeffCovarianceSocialCareS2e = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S2e", 1);
        // coeffCovarianceSocialCareS2f = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S2f", 1); // retired process
        // coeffCovarianceSocialCareS2g = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S2g", 1); // retired process
        // coeffCovarianceSocialCareS2h = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S2h", 1); // retired process
        // coeffCovarianceSocialCareS2i = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S2i", 1); // retired process
        // coeffCovarianceSocialCareS2j = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S2j", 1); // retired process
        // coeffCovarianceSocialCareS2k = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S2k", 1); // retired process
        coeffCovarianceSocialCareS3a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S3a", 1);
        coeffCovarianceSocialCareS3b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S3b", 1);
        coeffCovarianceSocialCareS3c = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S3c", 1);
        coeffCovarianceSocialCareS3d = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S3d", 1);
        // coeffCovarianceSocialCareS3e = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", "S3e", 1); // retired process

        //Unemployment
        coeffCovarianceUnemploymentU1a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", "U1a", 1);
        coeffCovarianceUnemploymentU1b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", "U1b", 1);
        coeffCovarianceUnemploymentU1c = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", "U1c", 1);
        coeffCovarianceUnemploymentU1d = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", "U1d", 1);

        //Financial distress
        coeffCovarianceFinancialDistress = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_financial_distress.xlsx", countryString, 1);

        //Health mental: level and case-based
        coeffCovarianceHM1Level = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_mental.xlsx", "HM1_L", 1);
        coeffCovarianceHM2LevelMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_mental.xlsx", "HM2_Males_L", 1);
        coeffCovarianceHM2LevelFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_mental.xlsx", "HM2_Females_L", 1);
        coeffCovarianceHM1Case = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_mental.xlsx", "HM1_C", 1);
        coeffCovarianceHM2CaseMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_mental.xlsx", "HM2_Males_C", 1);
        coeffCovarianceHM2CaseFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_mental.xlsx", "HM2_Females_C", 1);

        //Health
        coeffCovarianceDHE_MCS1 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", "DHE_MCS1", 1);
        coeffCovarianceDHE_MCS2Males = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", "DHE_MCS2_Males", 1);
        coeffCovarianceDHE_MCS2Females = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", "DHE_MCS2_Females", 1);

        coeffCovarianceDHE_PCS1 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", "DHE_PCS1", 1);
        coeffCovarianceDHE_PCS2Males = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", "DHE_PCS2_Males", 1);
        coeffCovarianceDHE_PCS2Females = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", "DHE_PCS2_Females", 1);

        coeffCovarianceDLS1 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", "DLS1", 1);
        coeffCovarianceDLS2Males = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", "DLS2_Males", 1);
        coeffCovarianceDLS2Females = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", "DLS2_Females", 1);

        loadEQ5DParameters(countryString);

        //Life satisfaction
//        coeffCovarianceDLS1 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_lifesatisfaction.xlsx", "DLS1", 1, columnsLifeSatisfaction1);

        //Education
        coeffCovarianceEducationE1a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_education.xlsx", "E1a", 1);
        coeffCovarianceEducationE1b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_education.xlsx", "E1b", 1);
        coeffCovarianceEducationE2 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_education.xlsx", "E2", 1);

        //Partnership
        coeffCovariancePartnershipU1 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_partnership.xlsx", "U1", 1);
        coeffCovariancePartnershipU2 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_partnership.xlsx", "U2", 1);
        meanCovarianceParametricMatching = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "scenario_parametricMatching.xlsx", "Parameters", 1);

        //Fertility
        coeffCovarianceFertilityF1 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_fertility.xlsx", "F1", 1);

        //Income
        coeffCovarianceIncomeI1a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_income.xlsx", "I1a", 1);
        coeffCovarianceIncomeI1b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_income.xlsx", "I1b", 1);
        coeffCovarianceIncomeI2b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_income.xlsx", "I2b", 1);
        coeffCovarianceIncomeI3a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_income.xlsx", "I3a", 1);
        coeffCovarianceIncomeI3b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_income.xlsx", "I3b", 1);

        //Leaving parental home
        coeffCovarianceLeaveHomeP1 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_leave_parental_home.xlsx", "P1", 1);

        //Homeownership
        coeffCovarianceHomeownership = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_home_ownership.xlsx", "HO1", 1);

        //Retirement
        coeffCovarianceRetirementR1a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_retirement.xlsx", "R1a", 1);
        coeffCovarianceRetirementR1b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_retirement.xlsx", "R1b", 1);

        //Childcare
        coeffCovarianceChildcareC1a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_childcarecost.xlsx", "C1a", 1);
        coeffCovarianceChildcareC1b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_childcarecost.xlsx", "C1b", 1);

        //Bootstrap the coefficients
        if(bootstrapAll) {
            resetBootstrapTrace();
            if (systemOut) {
                // Validate coefficient maps for null covariance cells before bootstrapping.
                validateCoefficientMapsForBootstrap(new Object[][]{
                    {"coeffCovarianceWagesMalesE", coeffCovarianceWagesMalesE},
                    {"coeffCovarianceWagesMalesNE", coeffCovarianceWagesMalesNE},
                    {"coeffCovarianceWagesFemalesE", coeffCovarianceWagesFemalesE},
                    {"coeffCovarianceWagesFemalesNE", coeffCovarianceWagesFemalesNE},
                    {"coeffCovarianceEmploymentSelectionMalesE", coeffCovarianceEmploymentSelectionMalesE},
                    {"coeffCovarianceEmploymentSelectionMalesNE", coeffCovarianceEmploymentSelectionMalesNE},
                    {"coeffCovarianceEmploymentSelectionFemalesE", coeffCovarianceEmploymentSelectionFemalesE},
                    {"coeffCovarianceEmploymentSelectionFemalesNE", coeffCovarianceEmploymentSelectionFemalesNE},
                    {"coeffLabourSupplyUtilityMales", coeffLabourSupplyUtilityMales},
                    {"coeffLabourSupplyUtilityFemales", coeffLabourSupplyUtilityFemales},
                    {"coeffLabourSupplyUtilitySingleDep", coeffLabourSupplyUtilitySingleDep},
                    {"coeffLabourSupplyUtilityACMales", coeffLabourSupplyUtilityACMales},
                    {"coeffLabourSupplyUtilityACFemales", coeffLabourSupplyUtilityACFemales},
                    {"coeffLabourSupplyUtilityCouples", coeffLabourSupplyUtilityCouples},
                    {"coeffCovarianceEducationE1a", coeffCovarianceEducationE1a},
                    {"coeffCovarianceEducationE1b", coeffCovarianceEducationE1b},
                    {"coeffCovarianceEducationE2", coeffCovarianceEducationE2},
                    {"coeffCovarianceHealthH1", coeffCovarianceHealthH1},
                    {"coeffCovarianceHealthH2", coeffCovarianceHealthH2},
                    {"coeffCovarianceHM1Level", coeffCovarianceHM1Level},
                    {"coeffCovarianceHM2LevelMales", coeffCovarianceHM2LevelMales},
                    {"coeffCovarianceHM2LevelFemales", coeffCovarianceHM2LevelFemales},
                    {"coeffCovarianceHM1Case", coeffCovarianceHM1Case},
                    {"coeffCovarianceHM2CaseMales", coeffCovarianceHM2CaseMales},
                    {"coeffCovarianceHM2CaseFemales", coeffCovarianceHM2CaseFemales},
                    {"coeffCovarianceDHE_MCS1", coeffCovarianceDHE_MCS1},
                    {"coeffCovarianceDHE_MCS2Males", coeffCovarianceDHE_MCS2Males},
                    {"coeffCovarianceDHE_MCS2Females", coeffCovarianceDHE_MCS2Females},
                    {"coeffCovarianceDHE_PCS1", coeffCovarianceDHE_PCS1},
                    {"coeffCovarianceDHE_PCS2Males", coeffCovarianceDHE_PCS2Males},
                    {"coeffCovarianceDHE_PCS2Females", coeffCovarianceDHE_PCS2Females},
                    {"coeffCovarianceDLS1", coeffCovarianceDLS1},
                    {"coeffCovarianceDLS2Males", coeffCovarianceDLS2Males},
                    {"coeffCovarianceDLS2Females", coeffCovarianceDLS2Females},
                    // {"coeffCovarianceSocialCareS1b", coeffCovarianceSocialCareS1b}, // retired process
                    {"coeffCovarianceSocialCareS2a", coeffCovarianceSocialCareS2a},
                    {"coeffCovarianceSocialCareS2b", coeffCovarianceSocialCareS2b},
                    {"coeffCovarianceSocialCareS2c", coeffCovarianceSocialCareS2c},
                    {"coeffCovarianceSocialCareS2d", coeffCovarianceSocialCareS2d},
                    {"coeffCovarianceSocialCareS2e", coeffCovarianceSocialCareS2e},
                    // {"coeffCovarianceSocialCareS2f", coeffCovarianceSocialCareS2f}, // retired process
                    // {"coeffCovarianceSocialCareS2g", coeffCovarianceSocialCareS2g}, // retired process
                    // {"coeffCovarianceSocialCareS2h", coeffCovarianceSocialCareS2h}, // retired process
                    // {"coeffCovarianceSocialCareS2i", coeffCovarianceSocialCareS2i}, // retired process
                    // {"coeffCovarianceSocialCareS2j", coeffCovarianceSocialCareS2j}, // retired process
                    // {"coeffCovarianceSocialCareS2k", coeffCovarianceSocialCareS2k}, // retired process
                    {"coeffCovarianceSocialCareS3a", coeffCovarianceSocialCareS3a},
                    {"coeffCovarianceSocialCareS3b", coeffCovarianceSocialCareS3b},
                    {"coeffCovarianceSocialCareS3c", coeffCovarianceSocialCareS3c},
                    {"coeffCovarianceSocialCareS3d", coeffCovarianceSocialCareS3d},
                    // {"coeffCovarianceSocialCareS3e", coeffCovarianceSocialCareS3e}, // retired process
                    {"coeffCovarianceEquivalisedIncomeMales", coeffCovarianceEquivalisedIncomeMales},
                    {"coeffCovarianceEquivalisedIncomeFemales", coeffCovarianceEquivalisedIncomeFemales},
                    {"coeffCovarianceEquivalisedIncomeDynamics", coeffCovarianceEquivalisedIncomeDynamics},
                    {"coeffCovarianceEquivalisedIncomeDynamics2", coeffCovarianceEquivalisedIncomeDynamics2},
                    {"coeffCovarianceUnemploymentU1a", coeffCovarianceUnemploymentU1a},
                    {"coeffCovarianceUnemploymentU1b", coeffCovarianceUnemploymentU1b},
                    {"coeffCovarianceUnemploymentU1c", coeffCovarianceUnemploymentU1c},
                    {"coeffCovarianceUnemploymentU1d", coeffCovarianceUnemploymentU1d},
                    {"coeffCovarianceIncomeI1a", coeffCovarianceIncomeI1a},
                    {"coeffCovarianceIncomeI1b", coeffCovarianceIncomeI1b},
                    {"coeffCovarianceIncomeI2b", coeffCovarianceIncomeI2b},
                    {"coeffCovarianceIncomeI3a", coeffCovarianceIncomeI3a},
                    {"coeffCovarianceIncomeI3b", coeffCovarianceIncomeI3b},
                    {"coeffCovarianceLeaveHomeP1", coeffCovarianceLeaveHomeP1},
                    {"coeffCovarianceHomeownership", coeffCovarianceHomeownership},
                    {"coeffCovarianceRetirementR1a", coeffCovarianceRetirementR1a},
                    {"coeffCovarianceRetirementR1b", coeffCovarianceRetirementR1b},
                    {"coeffCovarianceChildcareC1a", coeffCovarianceChildcareC1a},
                    {"coeffCovarianceChildcareC1b", coeffCovarianceChildcareC1b},
                    {"coeffCovariancePartnershipU1", coeffCovariancePartnershipU1},
                    {"coeffCovariancePartnershipU2", coeffCovariancePartnershipU2},
                    //{"coeffCovariancePartnershipITU1", coeffCovariancePartnershipITU1},
                    //{"coeffCovariancePartnershipITU2", coeffCovariancePartnershipITU2},
                    {"coeffCovarianceFertilityF1", coeffCovarianceFertilityF1},
                });
            }

            //Wages
            //coeffCovarianceWagesMales = RegressionUtils.bootstrap(coeffCovarianceWagesMales);
            coeffCovarianceWagesMalesE = bootstrapWithTrace("coeffCovarianceWagesMalesE", coeffCovarianceWagesMalesE);
            coeffCovarianceWagesMalesNE = bootstrapWithTrace("coeffCovarianceWagesMalesNE", coeffCovarianceWagesMalesNE);
            //coeffCovarianceWagesFemales = RegressionUtils.bootstrap(coeffCovarianceWagesFemales);
            coeffCovarianceWagesFemalesE = bootstrapWithTrace("coeffCovarianceWagesFemalesE", coeffCovarianceWagesFemalesE);
            coeffCovarianceWagesFemalesNE = bootstrapWithTrace("coeffCovarianceWagesFemalesNE", coeffCovarianceWagesFemalesNE);

            //Employment selection
            //coeffCovarianceEmploymentSelectionMales = RegressionUtils.bootstrap(coeffCovarianceEmploymentSelectionMales);
            coeffCovarianceEmploymentSelectionMalesE = bootstrapWithTrace("coeffCovarianceEmploymentSelectionMalesE", coeffCovarianceEmploymentSelectionMalesE);
            coeffCovarianceEmploymentSelectionMalesNE = bootstrapWithTrace("coeffCovarianceEmploymentSelectionMalesNE", coeffCovarianceEmploymentSelectionMalesNE);
            //coeffCovarianceEmploymentSelectionFemales = RegressionUtils.bootstrap(coeffCovarianceEmploymentSelectionFemales);
            coeffCovarianceEmploymentSelectionFemalesE = bootstrapWithTrace("coeffCovarianceEmploymentSelectionFemalesE", coeffCovarianceEmploymentSelectionFemalesE);
            coeffCovarianceEmploymentSelectionFemalesNE = bootstrapWithTrace("coeffCovarianceEmploymentSelectionFemalesNE", coeffCovarianceEmploymentSelectionFemalesNE);

            //Labour supply utility
            coeffLabourSupplyUtilityMales = bootstrapWithTrace("coeffLabourSupplyUtilityMales", coeffLabourSupplyUtilityMales);

            coeffLabourSupplyUtilityFemales = bootstrapWithTrace("coeffLabourSupplyUtilityFemales", coeffLabourSupplyUtilityFemales);
            coeffLabourSupplyUtilitySingleDep = bootstrapWithTrace("coeffLabourSupplyUtilitySingleDep", coeffLabourSupplyUtilitySingleDep);

            coeffLabourSupplyUtilityACMales = bootstrapWithTrace("coeffLabourSupplyUtilityACMales", coeffLabourSupplyUtilityACMales);
            coeffLabourSupplyUtilityACFemales = bootstrapWithTrace("coeffLabourSupplyUtilityACFemales", coeffLabourSupplyUtilityACFemales);

            coeffLabourSupplyUtilityCouples = bootstrapWithTrace("coeffLabourSupplyUtilityCouples", coeffLabourSupplyUtilityCouples);



            //Education
            coeffCovarianceEducationE1a = bootstrapWithTrace("coeffCovarianceEducationE1a", coeffCovarianceEducationE1a);
            coeffCovarianceEducationE1b = bootstrapWithTrace("coeffCovarianceEducationE1b", coeffCovarianceEducationE1b);
            coeffCovarianceEducationE2 = bootstrapWithTrace("coeffCovarianceEducationE2", coeffCovarianceEducationE2);

            //Health
            coeffCovarianceHealthH1 = bootstrapWithTrace("coeffCovarianceHealthH1", coeffCovarianceHealthH1);//Note that this overrides the original coefficient map with bootstrapped values
            coeffCovarianceHealthH2 = bootstrapWithTrace("coeffCovarianceHealthH2", coeffCovarianceHealthH2);
            coeffCovarianceHM1Level = bootstrapWithTrace("coeffCovarianceHM1Level", coeffCovarianceHM1Level);
            coeffCovarianceHM2LevelMales = bootstrapWithTrace("coeffCovarianceHM2LevelMales", coeffCovarianceHM2LevelMales);
            coeffCovarianceHM2LevelFemales = bootstrapWithTrace("coeffCovarianceHM2LevelFemales", coeffCovarianceHM2LevelFemales);
            coeffCovarianceHM1Case = bootstrapWithTrace("coeffCovarianceHM1Case", coeffCovarianceHM1Case);
            coeffCovarianceHM2CaseMales = bootstrapWithTrace("coeffCovarianceHM2CaseMales", coeffCovarianceHM2CaseMales);
            coeffCovarianceHM2CaseFemales = bootstrapWithTrace("coeffCovarianceHM2CaseFemales", coeffCovarianceHM2CaseFemales);
            coeffCovarianceDHE_MCS1 = bootstrapWithTrace("coeffCovarianceDHE_MCS1", coeffCovarianceDHE_MCS1);
            coeffCovarianceDHE_MCS2Males = bootstrapWithTrace("coeffCovarianceDHE_MCS2Males", coeffCovarianceDHE_MCS2Males);
            coeffCovarianceDHE_MCS2Females = bootstrapWithTrace("coeffCovarianceDHE_MCS2Females", coeffCovarianceDHE_MCS2Females);
            coeffCovarianceDHE_PCS1 = bootstrapWithTrace("coeffCovarianceDHE_PCS1", coeffCovarianceDHE_PCS1);
            coeffCovarianceDHE_PCS2Males = bootstrapWithTrace("coeffCovarianceDHE_PCS2Males", coeffCovarianceDHE_PCS2Males);
            coeffCovarianceDHE_PCS2Females = bootstrapWithTrace("coeffCovarianceDHE_PCS2Females", coeffCovarianceDHE_PCS2Females);
            coeffCovarianceDLS1 = bootstrapWithTrace("coeffCovarianceDLS1", coeffCovarianceDLS1);
            coeffCovarianceDLS2Males = bootstrapWithTrace("coeffCovarianceDLS2Males", coeffCovarianceDLS2Males);
            coeffCovarianceDLS2Females = bootstrapWithTrace("coeffCovarianceDLS2Females", coeffCovarianceDLS2Females);

            //Social care
            // coeffCovarianceSocialCareS1b = RegressionUtils.bootstrap(coeffCovarianceSocialCareS1b); // retired process
            coeffCovarianceSocialCareS2a = bootstrapWithTrace("coeffCovarianceSocialCareS2a", coeffCovarianceSocialCareS2a);
            coeffCovarianceSocialCareS2b = bootstrapWithTrace("coeffCovarianceSocialCareS2b", coeffCovarianceSocialCareS2b);
            coeffCovarianceSocialCareS2c = bootstrapWithTrace("coeffCovarianceSocialCareS2c", coeffCovarianceSocialCareS2c);
            coeffCovarianceSocialCareS2d = bootstrapWithTrace("coeffCovarianceSocialCareS2d", coeffCovarianceSocialCareS2d);
            coeffCovarianceSocialCareS2e = bootstrapWithTrace("coeffCovarianceSocialCareS2e", coeffCovarianceSocialCareS2e);
            // coeffCovarianceSocialCareS2f = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2f); // retired process
            // coeffCovarianceSocialCareS2g = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2g); // retired process
            // coeffCovarianceSocialCareS2h = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2h); // retired process
            // coeffCovarianceSocialCareS2i = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2i); // retired process
            // coeffCovarianceSocialCareS2j = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2j); // retired process
            // coeffCovarianceSocialCareS2k = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2k); // retired process
            coeffCovarianceSocialCareS3a = bootstrapWithTrace("coeffCovarianceSocialCareS3a", coeffCovarianceSocialCareS3a);
            coeffCovarianceSocialCareS3b = bootstrapWithTrace("coeffCovarianceSocialCareS3b", coeffCovarianceSocialCareS3b);
            coeffCovarianceSocialCareS3c = bootstrapWithTrace("coeffCovarianceSocialCareS3c", coeffCovarianceSocialCareS3c);
            coeffCovarianceSocialCareS3d = bootstrapWithTrace("coeffCovarianceSocialCareS3d", coeffCovarianceSocialCareS3d);
            // coeffCovarianceSocialCareS3e = RegressionUtils.bootstrap(coeffCovarianceSocialCareS3e); // retired process

            //lifetime incomes
            coeffCovarianceEquivalisedIncomeMales = bootstrapWithTrace("coeffCovarianceEquivalisedIncomeMales", coeffCovarianceEquivalisedIncomeMales);
            coeffCovarianceEquivalisedIncomeFemales = bootstrapWithTrace("coeffCovarianceEquivalisedIncomeFemales", coeffCovarianceEquivalisedIncomeFemales);
            coeffCovarianceEquivalisedIncomeDynamics = bootstrapWithTrace("coeffCovarianceEquivalisedIncomeDynamics", coeffCovarianceEquivalisedIncomeDynamics);
            coeffCovarianceEquivalisedIncomeDynamics2 = bootstrapWithTrace("coeffCovarianceEquivalisedIncomeDynamics2", coeffCovarianceEquivalisedIncomeDynamics2);

            //Unemployment
            coeffCovarianceUnemploymentU1a = bootstrapWithTrace("coeffCovarianceUnemploymentU1a", coeffCovarianceUnemploymentU1a);
            coeffCovarianceUnemploymentU1b = bootstrapWithTrace("coeffCovarianceUnemploymentU1b", coeffCovarianceUnemploymentU1b);
            coeffCovarianceUnemploymentU1c = bootstrapWithTrace("coeffCovarianceUnemploymentU1c", coeffCovarianceUnemploymentU1c);
            coeffCovarianceUnemploymentU1d = bootstrapWithTrace("coeffCovarianceUnemploymentU1d", coeffCovarianceUnemploymentU1d);

            //Non-labour income
            coeffCovarianceIncomeI1a = bootstrapWithTrace("coeffCovarianceIncomeI1a", coeffCovarianceIncomeI1a);// Commented out as not used any more since income is split.
            coeffCovarianceIncomeI1b = bootstrapWithTrace("coeffCovarianceIncomeI1b", coeffCovarianceIncomeI1b);// Commented out as not used any more since income is split.
            coeffCovarianceIncomeI2b = bootstrapWithTrace("coeffCovarianceIncomeI2b", coeffCovarianceIncomeI2b);
            coeffCovarianceIncomeI3a = bootstrapWithTrace("coeffCovarianceIncomeI3a", coeffCovarianceIncomeI3a);
            coeffCovarianceIncomeI3b = bootstrapWithTrace("coeffCovarianceIncomeI3b", coeffCovarianceIncomeI3b);
            //coeffCovarianceIncomeI3c = RegressionUtils.bootstrap(coeffCovarianceIncomeI3c);
            //coeffCovarianceIncomeI4a = RegressionUtils.bootstrap(coeffCovarianceIncomeI4a);
            // coeffCovarianceIncomeI4b = RegressionUtils.bootstrap(coeffCovarianceIncomeI4b);
            // coeffCovarianceIncomeI5a = RegressionUtils.bootstrap(coeffCovarianceIncomeI5a);
            //coeffCovarianceIncomeI6a_selection = RegressionUtils.bootstrap(coeffCovarianceIncomeI6a_selection);
            //coeffCovarianceIncomeI6b_amount = RegressionUtils.bootstrap(coeffCovarianceIncomeI6b_amount);
            // coeffCovarianceIncomeI3a_selection = RegressionUtils.bootstrap(coeffCovarianceIncomeI3a_selection);
            // coeffCovarianceIncomeI3b_selection = RegressionUtils.bootstrap(coeffCovarianceIncomeI3b_selection);
            // coeffCovarianceIncomeI5a_selection = RegressionUtils.bootstrap(coeffCovarianceIncomeI5a_selection);

            //Leave parental home
            coeffCovarianceLeaveHomeP1 = bootstrapWithTrace("coeffCovarianceLeaveHomeP1", coeffCovarianceLeaveHomeP1);

            //Homeownership
            coeffCovarianceHomeownership = bootstrapWithTrace("coeffCovarianceHomeownership", coeffCovarianceHomeownership);

            //Retirement
            coeffCovarianceRetirementR1a = bootstrapWithTrace("coeffCovarianceRetirementR1a", coeffCovarianceRetirementR1a);
            coeffCovarianceRetirementR1b = bootstrapWithTrace("coeffCovarianceRetirementR1b", coeffCovarianceRetirementR1b);

            //Childcare
            coeffCovarianceChildcareC1a = bootstrapWithTrace("coeffCovarianceChildcareC1a", coeffCovarianceChildcareC1a);
            coeffCovarianceChildcareC1b = bootstrapWithTrace("coeffCovarianceChildcareC1b", coeffCovarianceChildcareC1b);

            //Specification of some processes depends on the country:
            if (country.equals(Country.UK)) {
                coeffCovariancePartnershipU1 = bootstrapWithTrace("coeffCovariancePartnershipU1", coeffCovariancePartnershipU1);
                // coeffCovariancePartnershipU1b = RegressionUtils.bootstrap(coeffCovariancePartnershipU1b);
                coeffCovariancePartnershipU2 = bootstrapWithTrace("coeffCovariancePartnershipU2", coeffCovariancePartnershipU2);
                coeffCovarianceFertilityF1 = bootstrapWithTrace("coeffCovarianceFertilityF1", coeffCovarianceFertilityF1);
            } else if (country.equals(Country.IT)) {
                coeffCovariancePartnershipITU1 = bootstrapWithTrace("coeffCovariancePartnershipITU1", coeffCovariancePartnershipITU1);
                coeffCovariancePartnershipITU2 = bootstrapWithTrace("coeffCovariancePartnershipITU2", coeffCovariancePartnershipITU2);
                coeffCovarianceFertilityF1 = bootstrapWithTrace("coeffCovarianceFertilityF1", coeffCovarianceFertilityF1);
            }

        }


        addFixedCostRegressors(coeffLabourSupplyUtilityMales, List.of("AlignmentFixedCostMen"));
        addFixedCostRegressors(coeffLabourSupplyUtilityFemales, List.of("AlignmentFixedCostWomen"));
        addFixedCostRegressors(coeffLabourSupplyUtilitySingleDep, List.of(
                "AlignmentFixedCostMen",
                "AlignmentFixedCostWomen",
                "AlignmentSingleDepMen",
                "AlignmentSingleDepWomen"
        ));
        addFixedCostRegressors(coeffLabourSupplyUtilityACMales, List.of("AlignmentFixedCostMen"));
        addFixedCostRegressors(coeffLabourSupplyUtilityACFemales, List.of("AlignmentFixedCostWomen"));
        addFixedCostRegressors(coeffLabourSupplyUtilityCouples, List.of("AlignmentFixedCostMen", "AlignmentFixedCostWomen"));

        //Health
        regHealthH1 = new GeneralisedOrderedRegression<>(RegressionType.GenOrderedLogit, Dhe.class, coeffCovarianceHealthH1);
        regHealthH2 = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceHealthH2);

        //Social care
        // regSocialCareS1b = new LinearRegression(coeffCovarianceSocialCareS1b); // retired process
        regNeedCareS2a = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceSocialCareS2a);
        regReceiveCareS2b = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceSocialCareS2b);
        regSocialCareMarketS2c = new MultinomialRegression<>(RegressionType.MultinomialLogit, SocialCareReceiptS2c.class, coeffCovarianceSocialCareS2c);
        regInformalCareHoursS2d = new LinearRegression(coeffCovarianceSocialCareS2d);
        regFormalCareHoursS2e = new LinearRegression(coeffCovarianceSocialCareS2e);
        // regNotPartnerInformalCareS2f = new MultinomialRegression<>(RegressionType.MultinomialLogit, NotPartnerInformalCarer.class, coeffCovarianceSocialCareS2f); // retired process
        // regPartnerCareHoursS2g = new LinearRegression(coeffCovarianceSocialCareS2g); // retired process
        // regDaughterCareHoursS2h = new LinearRegression(coeffCovarianceSocialCareS2h); // retired process
        // regSonCareHoursS2i = new LinearRegression(coeffCovarianceSocialCareS2i); // retired process
        // regOtherCareHoursS2j = new LinearRegression(coeffCovarianceSocialCareS2j); // retired process
        // regFormalCareHoursS2k = new LinearRegression(coeffCovarianceSocialCareS2k); // retired process
        regCarePartnerProvCareToOtherS3a = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceSocialCareS3a);
        regNoCarePartnerProvCareToOtherS3b = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceSocialCareS3b);
        regCareHoursProvS3c = new LinearRegression(coeffCovarianceSocialCareS3c);
        regCareHoursProvS3d = new LinearRegression(coeffCovarianceSocialCareS3d);
        // regCareHoursProvS3e = new LinearRegression(coeffCovarianceSocialCareS3e); // retired process

        //lifetime incomes
        regEquivalisedIncomeMales = new LinearRegression(coeffCovarianceEquivalisedIncomeMales);
        regEquivalisedIncomeFemales = new LinearRegression(coeffCovarianceEquivalisedIncomeFemales);
        regEquivalisedIncomeDynamics = new LinearRegression(coeffCovarianceEquivalisedIncomeDynamics);
        regEquivalisedIncomeDynamics2 = new LinearRegression(coeffCovarianceEquivalisedIncomeDynamics2);

        //Unemployment
        regUnemploymentMaleGraduateU1a = new BinomialRegression(RegressionType.Probit, ReversedIndicator.class, coeffCovarianceUnemploymentU1a);
        regUnemploymentMaleNonGraduateU1b = new BinomialRegression(RegressionType.Probit, ReversedIndicator.class, coeffCovarianceUnemploymentU1b);
        regUnemploymentFemaleGraduateU1c = new BinomialRegression(RegressionType.Probit, ReversedIndicator.class, coeffCovarianceUnemploymentU1c);
        regUnemploymentFemaleNonGraduateU1d = new BinomialRegression(RegressionType.Probit, ReversedIndicator.class, coeffCovarianceUnemploymentU1d);

        //Financial distress
        regFinancialDistress = new BinomialRegression(RegressionType.Logit, Indicator.class, coeffCovarianceFinancialDistress);

        //Health mental
        regHealthHM1Level = new LinearRegression(coeffCovarianceHM1Level);
        regHealthHM2LevelMales = new LinearRegression(coeffCovarianceHM2LevelMales);
        regHealthHM2LevelFemales = new LinearRegression(coeffCovarianceHM2LevelFemales);

        regHealthHM1Case = new OrderedRegression(RegressionType.OrderedLogit,DhmGhq.class,coeffCovarianceHM1Case);
        regHealthHM2CaseMales = new LinearRegression(coeffCovarianceHM2CaseMales);
        regHealthHM2CaseFemales = new LinearRegression(coeffCovarianceHM2CaseFemales);

        //Health
        regHealthMCS1 = new LinearRegression(coeffCovarianceDHE_MCS1);
        regHealthMCS2Males = new LinearRegression(coeffCovarianceDHE_MCS2Males);
        regHealthMCS2Females = new LinearRegression(coeffCovarianceDHE_MCS2Females);
        regHealthPCS1 = new LinearRegression(coeffCovarianceDHE_PCS1);
        regHealthPCS2Males = new LinearRegression(coeffCovarianceDHE_PCS2Males);
        regHealthPCS2Females = new LinearRegression(coeffCovarianceDHE_PCS2Females);
        regLifeSatisfaction1 = new LinearRegression(coeffCovarianceDLS1);
        regLifeSatisfaction2Males = new LinearRegression(coeffCovarianceDLS2Males);
        regLifeSatisfaction2Females = new LinearRegression(coeffCovarianceDLS2Females);

        // Education
        regEducationE1a = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceEducationE1a);
        regEducationE1b = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceEducationE1b);
        regEducationE2 = new GeneralisedOrderedRegression<>(RegressionType.GenOrderedLogit, Education.class, coeffCovarianceEducationE2);

        //Partnership
        if (country.equals(Country.UK)) {
            MultiKeyCoefficientMap coeffPartnershipU1Appended = appendCoefficientMaps(coeffCovariancePartnershipU1, partnershipTimeAdjustment, "Year");
            // MultiKeyCoefficientMap coeffPartnershipU1bAppended = appendCoefficientMaps(coeffCovariancePartnershipU1b, partnershipTimeAdjustment, "Year");
            MultiKeyCoefficientMap coeffPartnershipU2Appended = appendCoefficientMaps(coeffCovariancePartnershipU2, partnershipTimeAdjustment, "Year", true);
            regPartnershipU1 = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffPartnershipU1Appended);
            // regPartnershipU1b = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffPartnershipU1bAppended);
            regPartnershipU2 = new BinomialRegression(RegressionType.Probit, ReversedIndicator.class, coeffPartnershipU2Appended);
        } else if (country.equals(Country.IT)) {
            regPartnershipITU1 = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovariancePartnershipITU1);
            regPartnershipITU2 = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovariancePartnershipITU2);
        }

        //Fertility
        if (country.equals(Country.UK)) {
            MultiKeyCoefficientMap coeffFertilityF1aAppended = appendCoefficientMaps(coeffCovarianceFertilityF1, fertilityTimeAdjustment, "Year");
            regFertilityF1 = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffFertilityF1aAppended);
        } else if (country.equals(Country.IT)) {
            regFertilityF1 = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceFertilityF1);
        }

        //Income
        regIncomeI1a = new BinomialRegression(RegressionType.Logit, Indicator.class, coeffCovarianceIncomeI1a);
        regIncomeI1b = new LinearRegression(coeffCovarianceIncomeI1b);
        regIncomeI2b = new LinearRegression(coeffCovarianceIncomeI2b);
        regIncomeI3a = new BinomialRegression(RegressionType.Logit, Indicator.class, coeffCovarianceIncomeI3a);
        regIncomeI3b = new LinearRegression(coeffCovarianceIncomeI3b);
        //regIncomeI3c = new LinearRegression(coeffCovarianceIncomeI3c);
        //regIncomeI4a = new LinearRegression(coeffCovarianceIncomeI4a);
        // regIncomeI4b = new LinearRegression(coeffCovarianceIncomeI4b);
        // regIncomeI5a = new LinearRegression(coeffCovarianceIncomeI5a);
        //regIncomeI6b_amount = new LinearRegression(coeffCovarianceIncomeI6b_amount);
        // regIncomeI3a_selection = new BinomialRegression(RegressionType.Logit, Indicator.class, coeffCovarianceIncomeI3a_selection);
        // regIncomeI3b_selection = new BinomialRegression(RegressionType.Logit, Indicator.class, coeffCovarianceIncomeI3b_selection);
        // regIncomeI5a_selection = new BinomialRegression(RegressionType.Logit, Indicator.class, coeffCovarianceIncomeI5a_selection);
        //regIncomeI6a_selection = new BinomialRegression(RegressionType.Logit, Indicator.class, coeffCovarianceIncomeI6a_selection);

        //Homeownership
        regHomeownershipHO1a = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceHomeownership);

        //XXX: Note: the model used for selection in Heckman procedure is a Probit, but to obtain Inverse Mills Ratio, linear prediction needs to be obtained - so linear regression used here
        //regEmploymentSelectionMale = new LinearRegression(coeffCovarianceEmploymentSelectionMales);
        regEmploymentSelectionMaleE = new LinearRegression(coeffCovarianceEmploymentSelectionMalesE);
        regEmploymentSelectionMaleNE = new LinearRegression(coeffCovarianceEmploymentSelectionMalesNE);
        //regEmploymentSelectionFemale = new LinearRegression(coeffCovarianceEmploymentSelectionFemales);
        regEmploymentSelectionFemaleE = new LinearRegression(coeffCovarianceEmploymentSelectionFemalesE);
        regEmploymentSelectionFemaleNE = new LinearRegression(coeffCovarianceEmploymentSelectionFemalesNE);
        standardNormalDistribution = new NormalDistribution();

        //Wages
        //regWagesMales = new LinearRegression(coeffCovarianceWagesMales);
        regWagesMalesE = new LinearRegression(coeffCovarianceWagesMalesE);
        regWagesMalesNE = new LinearRegression(coeffCovarianceWagesMalesNE);
        //regWagesFemales = new LinearRegression(coeffCovarianceWagesFemales);
        regWagesFemalesE = new LinearRegression(coeffCovarianceWagesFemalesE);
        regWagesFemalesNE = new LinearRegression(coeffCovarianceWagesFemalesNE);

        //Labour Supply regressions from Zhechun's estimates on the EM input data
        regLabourSupplyUtilityMales = new LinearRegression(coeffLabourSupplyUtilityMales);
        regLabourSupplyUtilityFemales = new LinearRegression(coeffLabourSupplyUtilityFemales);
        regLabourSupplyUtilitySingleDep = new LinearRegression(coeffLabourSupplyUtilitySingleDep);
        regLabourSupplyUtilityACMales = new LinearRegression(coeffLabourSupplyUtilityACMales);
        regLabourSupplyUtilityACFemales = new LinearRegression(coeffLabourSupplyUtilityACFemales);
        regLabourSupplyUtilityCouples = new LinearRegression(coeffLabourSupplyUtilityCouples);

        // Regressions for Covid-19 labour transition models below
        regC19LS_SE = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceC19LS_SE);
        regC19LS_E1 = new MultinomialRegression<>(RegressionType.MultinomialLogit, Les_transitions_E1.class, coeffC19LS_E1Map, true);
        regC19LS_FF1 = new MultinomialRegression<>(RegressionType.MultinomialLogit, Les_transitions_FF1.class, coeffC19LS_FF1Map, true);
        regC19LS_FX1 = new MultinomialRegression<>(RegressionType.MultinomialLogit, Les_transitions_FX1.class, coeffC19LS_FX1Map, true);
        regC19LS_S1 = new MultinomialRegression<>(RegressionType.MultinomialLogit, Les_transitions_S1.class, coeffC19LS_S1Map, true);
        regC19LS_U1 = new MultinomialRegression<>(RegressionType.MultinomialLogit, Les_transitions_U1.class, coeffC19LS_U1Map, true);
        regC19LS_E2a = new LinearRegression(coeffC19LS_E2a);
        regC19LS_E2b = new LinearRegression(coeffC19LS_E2b);
        regC19LS_F2a = new LinearRegression(coeffC19LS_F2a);
        regC19LS_F2b = new LinearRegression(coeffC19LS_F2b);
        regC19LS_F2c = new LinearRegression(coeffC19LS_F2c);
        regC19LS_S2a = new LinearRegression(coeffC19LS_S2a);
        regC19LS_U2a = new LinearRegression(coeffC19LS_U2a);
        regC19LS_S3 = new BinomialRegression(RegressionType.Logit, Indicator.class, coeffC19LS_S3);

        //Leaving parental home
        regLeaveHomeP1a = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceLeaveHomeP1);

        //Retirement
        regRetirementR1a = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceRetirementR1a);
        regRetirementR1b = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceRetirementR1b);

        //Childcare
        regChildcareC1a = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceChildcareC1a);
        regChildcareC1b = new LinearRegression(coeffCovarianceChildcareC1b);

        //Create the age and wage differential MultivariateNormalDistribution for partnership formation, using means and var-cov matrix loaded from Excel
        targetMeanAgeDifferential = ((Number) meanCovarianceParametricMatching.getValue("mean_dag_diff")).doubleValue();
        targetMeanWageDifferential = ((Number) meanCovarianceParametricMatching.getValue("mean_wage_diff")).doubleValue();
        double[] means = {targetMeanAgeDifferential, targetMeanWageDifferential};
        double[][] covariances = { {((Number) meanCovarianceParametricMatching.getValue("var_dag_diff")).doubleValue(), ((Number) meanCovarianceParametricMatching.getValue("cov_dag_wage_diff")).doubleValue()} , {((Number) meanCovarianceParametricMatching.getValue("cov_dag_wage_diff")).doubleValue(), ((Number) meanCovarianceParametricMatching.getValue("var_wage_diff")).doubleValue()}};
        wageAndAgeDifferentialMultivariateNormalDistribution = getMultivariateNormalDistribution(means, covariances);

        calculateFertilityRatesFromProjections();
        calculatePopulationGrowthRatiosFromProjections();

        /////////////////////////////////////////////////POPULATE STATISTICS FOR VALIDATION/////////////////////////////
        //Students by Age
        validationStudentsByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "studentsByAge", 1);

        //Students by Region
        validationStudentsByRegion = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "studentsByRegion", 1);

        //Education level of over 17 year olds
        validationEducationLevel = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "educationLevel", 1);

        //Education level by age group
        validationEducationLevelByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "educationLevelByAge", 1);

        //Education level by demRgn
        validationEducationLevelByRegion = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "educationLevelByRegion", 1);

        //Partnered BU share by demRgn
        validationPartneredShareByRegion = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "partneredBUShareByRegion", 1);

        //Disabled by age
        validationDisabledByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "disabledByAgeGroup", 1);

        validationDisabledByGender = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "disabledByGender", 1);

        //Health by age
        validationHealthByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "healthByAgeGroup", 1);

        //Mental health by age and demSex
        validationMentalHealthByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "mentalHealthByAgeGroup", 1);


        validationHealthMCSByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "healthMCSByAgeGroup", 1);
        validationHealthPCSByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "healthPCSByAgeGroup", 1);
        validationLifeSatisfactionByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "lifeSatisfactionByAgeGroup", 1);

        //Psychological distress by age and demSex
        validationPsychDistressByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "psychDistressByAgeGroup", 1);
        validationPsychDistressByAgeLow = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "psychDistressByAgeGroupLowED", 1);
        validationPsychDistressByAgeMed = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "psychDistressByAgeGroupMedED", 1);
        validationPsychDistressByAgeHigh = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "psychDistressByAgeGroupHiEd", 1);

        //Employment by demSex
        validationEmploymentByGender = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "employmentByGender", 1);

        //Employment by age and demSex
        validationEmploymentByAgeAndGender = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "employmentByGenderAndAge", 1);

        //Employment by maternity
        validationEmploymentByMaternity = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "employmentByMaternity", 1);

        //Employment by demSex and demRgn
        validationEmploymentByGenderAndRegion = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "employmentByGenderAndRegion", 1);

        //Labour supply by education
        validationLabourSupplyByEducation = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "labourSupplyByEducation", 1);

        //Activity status
        validationActivityStatus = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "activityStatus", 1);

        //Homeownership status
        validationHomeownershipBenefitUnits = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "homeownership", 1);

        //Gross earnings yearly by education and demSex (for employed persons)
        validationGrossEarningsByGenderAndEducation = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "grossEarningsByGenderAndEdu", 1);

        //Hourly wages by education and demSex (for employed persons)
        validationLhwByGenderAndEducation = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "lhwByGenderAndEdu", 1);

        //Hours worked weekly by education and demSex (for employed persons)
        hourlyWageByGenderAndEducation = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", "hourlywageByGenderAndEdu", 1);
    }

    /**
     *
     * Update the probability of sickness by age profile for those old enough to work, catering
     * for the change (increase) in retirement age in future years, by 'stretching' the
     * (approximated) curves for males and females using the instructions below.
     *
     * From Matteo:
     * " In every year we will evolve this fraction together with the state retirement age.
     * This means that the fraction of sick people at the minimum age (18) and at the maximum age
     * (the state retirement age) remain the same, and the fitted curve is scaled accordingly."
     *
     *	This means that if the fraction of people with bad health at age 60 is x% when state
     *	retirement age is 65, after state retirement age increases to 66 that fraction will be met
     *	at age 60/65*66 = 61 (rounded).
     *
     *	I don't mind using a cubic rather than a quadratic spline, try it out if you wish
     *	(differences are small). We can also use the actual numbers, I have used a spline just
     *	to smooth out the profiles.
     *
     */

    /*
    public static void updateProbSick(int year) {

        //Only need to update prob of sick profile if retirement years have changed, so check
        //For males
        boolean retirementYearsHaveChanged = ((Number)fixedRetireAge.getValue(year, Gender.Male.toString())) != ((Number)fixedRetireAge.getValue(year-1, Gender.Male.toString()));
        //For females - if case for males is already true, the boolean will always be true
        retirementYearsHaveChanged = retirementYearsHaveChanged || ((Number)fixedRetireAge.getValue(year, Gender.Female.toString())) != ((Number)fixedRetireAge.getValue(year-1, Gender.Female.toString()));

        if(retirementYearsHaveChanged) {
            LinkedHashMap<Gender, Integer> retirementAge = new LinkedHashMap<>();
            retirementAge.put(Gender.Male, ((Number)fixedRetireAge.getValue(year, Gender.Male.toString())).intValue());
            retirementAge.put(Gender.Female, ((Number)fixedRetireAge.getValue(year, Gender.Female.toString())).intValue());

            probSick.clear();
            for(Gender demSex: Gender.values()) {
                int minAge = Parameters.MIN_AGE_TO_LEAVE_EDUCATION;
                probSick.put(demSex, minAge, rawProbSick.getValue(demSex.toString(), minAge));
            }

            for(Object o: rawProbSick.keySet()) {
                MultiKey mk = (MultiKey)o;
                String demSex = (String) mk.getKey(0);
                int rawAge = (int) mk.getKey(1);
                int adjustedAge;
                if(demSex.equals(Gender.Male.toString())) {
                    adjustedAge = (int)(rawAge * retirementAge.get(Gender.Male)/(double)maleMaxAgeSick);
                    probSick.put(Gender.Male, adjustedAge, rawProbSick.get(mk));
                }
                else {
                    adjustedAge = (int)(rawAge * retirementAge.get(Gender.Female)/(double)femaleMaxAgeSick);
                    probSick.put(Gender.Female, adjustedAge, rawProbSick.get(mk));
                }

            }

            //Fill in any gaps in age, due to shift in ages, filling with linear interpolation (arithmetic
            //average of neighbouring ages)
            for(Gender demSex: Gender.values()) {
                for(int i = Parameters.MIN_AGE_TO_LEAVE_EDUCATION; i < retirementAge.get(demSex); i++) {
                    if(!probSick.containsKey(demSex, i)) {
                        double youngerVal = ((Number)probSick.get(demSex, i-1)).doubleValue();
                        double olderVal = ((Number)probSick.get(demSex, i+1)).doubleValue();
                        probSick.put(demSex, i, 0.5*(youngerVal + olderVal));	//Insert arithmetic average between ages
                    }
                }
            }

        }
    }

    public static void updateUnemploymentRate(int year) {
//		unemploymentRatesByRegion.clear();
        for(Region demRgn: countryRegions) {
            unemploymentRatesByRegion.put(demRgn, ((Number)unemploymentRates.getValue(demRgn.toString(), year)).doubleValue());
        }
    }
*/

    public static NormalDistribution getStandardNormalDistribution() {
        return standardNormalDistribution;
    }

    //For a given vector of means and a variance-covariance matrix, return a MultivariateNormalDistribution that can be sampled from using sample() method
    public static MultivariateNormalDistribution getMultivariateNormalDistribution(double[] means, double[][] covariances) {
        MultivariateNormalDistribution multivariateNormalDistribution = new MultivariateNormalDistribution(means, covariances);
        return multivariateNormalDistribution;
    }

    public static double getRMSEForRegression(String regressionName) {
        Object rmseValue = coefficientMapRMSE.getValue(regressionName);
        if (rmseValue instanceof Number) {
            return ((Number) rmseValue).doubleValue();
        }

        // Backward-compatible aliases for simplified social-care equations.
        if ("S2d".equals(regressionName)) {
            Object aliasValue = coefficientMapRMSE.getValue("S2g");
            if (aliasValue instanceof Number) {
                System.out.println("RMSE warning: using S2g RMSE as fallback for S2d");
                return ((Number) aliasValue).doubleValue();
            }
        } else if ("S2e".equals(regressionName)) {
            Object aliasValue = coefficientMapRMSE.getValue("S2k");
            if (aliasValue instanceof Number) {
                System.out.println("RMSE warning: using S2k RMSE as fallback for S2e");
                return ((Number) aliasValue).doubleValue();
            }
        }

        System.out.println("RMSE warning: missing RMSE for regression " + regressionName + ", defaulting to 0.0");
        return 0.0;
    }

    private static void calculateFertilityRatesFromProjections() {

        fertilityRateByRegionYear = MultiKeyMap.multiKeyMap(new LinkedMap<>());
        fertilityRateByYear = new HashMap<>();
        for (int year = startYear; year <= endYear; year++) {

            double projectedNumFertileWomenAll = 0.0, numNewBornAll = 0.0;
            for (Region demRgn : countryRegions) {

                double projectedNumFertileWomenByRegion = 0.0;
                for (int age = MIN_AGE_MATERNITY; age <= MAX_AGE_MATERNITY; age++) {
                    projectedNumFertileWomenByRegion += getPopulationProjections(Gender.Female, demRgn, age, year);
                }

                double numNewBornByRegion = 0.;
                for (Gender demSex: Gender.values()) {
                    numNewBornByRegion += getPopulationProjections(demSex, demRgn, 0, year);		//Number of people aged 0 in projected years
                }

                if (projectedNumFertileWomenByRegion <= 0.) {
                    throw new IllegalArgumentException("Projected Number of Females of Fertile Age is not positive!");
                }
                else {
                    projectedNumFertileWomenAll += projectedNumFertileWomenByRegion;
                    numNewBornAll += numNewBornByRegion;
                    fertilityRateByRegionYear.put(demRgn, year, numNewBornByRegion / projectedNumFertileWomenByRegion);
                }
            }
            fertilityRateByYear.put(year, numNewBornAll / projectedNumFertileWomenAll);
        }
    }


    private static void calculatePopulationGrowthRatiosFromProjections() {

        populationGrowthRatiosByRegionYear = MultiKeyMap.multiKeyMap(new LinkedMap<>());
        for(int year = startYear+1; year <= endYear; year++) {		//Year is the latter year, i.e. growth ratio for year t is Pop(t)/Pop(t-1)
            for(Region demRgn : countryRegions) {
                double numberOfPeopleInRegionThisYear = 0.;
                double numberOfPeopleInRegionPreviousYear = 0.;
                for(Gender demSex: Gender.values()) {
                    for(int age = 0; age <= maxAge; age++) {
                        numberOfPeopleInRegionThisYear += getPopulationProjections(demSex, demRgn, age, year);
                        numberOfPeopleInRegionPreviousYear += getPopulationProjections(demSex, demRgn, age, year-1);
                    }
                }
                populationGrowthRatiosByRegionYear.put(demRgn, year, numberOfPeopleInRegionThisYear / numberOfPeopleInRegionPreviousYear);
            }
        }
    }

    public static TreeMap<Integer, String> calculateEUROMODpolicySchedule(Country country) {
        //Load current values for policy description and initiation year
        MultiKeyCoefficientMap currentEUROMODpolicySchedule;

        if (trainingFlag) {
            File trainingSchedule = new File(getInputDirectory() + "EUROMODoutput" + File.separator + "training" + File.separator + EUROMODpolicyScheduleFilename + ".xlsx");
            File runSchedule = new File(getInputDirectory() + EUROMODpolicyScheduleFilename + ".xlsx");
            try {
                FileUtils.copyFile(trainingSchedule, runSchedule);
            } catch (IOException e) {
                System.err.println("Could not replace EUROMODoutput.xlsx from training data");
            }
        }
        
        currentEUROMODpolicySchedule = ExcelAssistant.loadCoefficientMap(getInputDirectory() + EUROMODpolicyScheduleFilename + ".xlsx", country.toString(), 1, 3);
        TreeMap<Integer, String> newEUROMODpolicySchedule = new TreeMap<>();

        for(Object o: currentEUROMODpolicySchedule.keySet()) {
            MultiKey k = (MultiKey)o;
            if(k.getKey(0) != null) {
                String name = k.getKey(0).toString();
                if(name != null &&
                        currentEUROMODpolicySchedule.getValue(name, EUROMODpolicyScheduleHeadingScenarioYearBegins) != null &&
                        currentEUROMODpolicySchedule.getValue(name, EUROMODpolicyScheduleHeadingScenarioSystemYear) != null) {
                    String policyStartYearString = currentEUROMODpolicySchedule.getValue(name, EUROMODpolicyScheduleHeadingScenarioYearBegins).toString();
                    String policySystemYearString = currentEUROMODpolicySchedule.getValue(name, EUROMODpolicyScheduleHeadingScenarioSystemYear).toString();
                    if(policyStartYearString != null && !policyStartYearString.isEmpty()) {
                        Integer policyStartYear = Integer.parseInt(policyStartYearString);
                        Integer policySystemYear = Integer.parseInt(policySystemYearString);
                        if(newEUROMODpolicySchedule.containsKey(policyStartYear)) {
                            throw new IllegalArgumentException("ERROR - there is more than one EUROMOD policy scenario with the same policy start year of " + policyStartYear + "!");
                        }
                        newEUROMODpolicySchedule.put(policyStartYear, name.split(".txt")[0]);

                        if (policySystemYearString == null) {
                            throw new IllegalArgumentException("ERROR - there is at least one EUROMOD policy scenario (" + name + ") with policy start year but no policy system year!");
                        } else {
                            Pair<String, Integer> policyNameSystemYearPair = new Pair<>(name.split(".txt")[0], policySystemYear);
                            EUROMODpolicyScheduleSystemYearMap.put(policyStartYear, policyNameSystemYearPair);
                        }
                    }
                }
            }
        }
        if(newEUROMODpolicySchedule.isEmpty())
            throw new IllegalArgumentException("ERROR - there are no EUROMOD policies with valid \'" + EUROMODpolicyScheduleHeadingScenarioYearBegins + "\' values specified in worksheet \'" + country + "\' of the " + EUROMODpolicyScheduleFilename + ".xlsx file");

        // validate EUROMOD input years
        boolean flagMissingBasePriceYear = true;
        for (int fromYear : EUROMODpolicyScheduleSystemYearMap.keySet()) {
            int systemYear = EUROMODpolicyScheduleSystemYearMap.get(fromYear).getValue();
            if (systemYear == BASE_PRICE_YEAR) {
                flagMissingBasePriceYear = false;
                break;
            }
        }
        if (flagMissingBasePriceYear)
            throw new RuntimeException("Must include a tax database generated for the base price year assumed for analysis (" + BASE_PRICE_YEAR + ")");

        return newEUROMODpolicySchedule;
    }

    public static String getEUROMODpolicyForThisYear(int year, Map<Integer, String> scenarioPlan) {
        String policyName;
        policyName = scenarioPlan.get(year);
        if(policyName == null) {	//Then, no EUROMOD policy begins in that year.  Instead, must apply the prevailing policy.
            for(Integer policyStartYear: scenarioPlan.keySet()) {	//Should iterate in order of increasing value of integer
                if(policyStartYear <= year) {		//In case the start year of the whole simulation is later than the first entry, assign but iterate again and check the following year.  Stop when the policyStartYear is greater than the year
                    policyName = scenarioPlan.get(policyStartYear);
                }
                else {
                    if(policyName == null) {		//Check for case when the earliest policyStartYear is later than the current year.  Need to apply the policy with the earliest policyStartYear anyway, just so there is a policy to apply!
                        policyName = scenarioPlan.get(policyStartYear);
                    }
                    break;		//Stop if policyStartYear is greater than year
                }
            }
        }
        return policyName;
    }

    public static void setCountryRegions(Country country) {
        countryRegions = new LinkedHashSet<Region>();
        for(Region demRgn : Region.values()) {			//TODO: restrict this to only regions in the simulated country
            if(demRgn.toString().startsWith(country.toString())) {			//Only assess the relevant regions for the country
                countryRegions.add(demRgn);				//Create a set of only relevant regions that we can use below TODO: This should be done in the Parameters class, once and for all!
            }
        }
    }

    //Benefit Unit ID is different in different countries of EUROMOD. This method creates a map with name of the country and name of the benefit unit variable. It is used to create input database in the
    //SQLdataParser class.
    public static LinkedHashMap<Country, String> countryBenefitUnitNames = new LinkedHashMap<>();


    public static void setCountryBenefitUnitName() {

        //Benefit unit variable has different name in each country. This method loads the correct name of the benefit unit variable from Excel file system_bu_names.xlsx in the input folder.
        benefitUnitVariableNames = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "system_bu_names.xlsx", "Names", 1, 1);
    }

    //-----------------------------------------------------------------------------------------------------
    // Access methods
    //-----------------------------------------------------------------------------------------------------

    public static MultiKeyCoefficientMap getBenefitUnitVariableNames() { return benefitUnitVariableNames; }

    public static MultiKeyCoefficientMap getStudentShareProjections() { return studentShareProjections; }

    public static MultinomialRegression getRegEducationLevel() {return regEducationLevel;}

    public static MultiKeyCoefficientMap getEmploymentsFurloughedFull() {
        return employmentsFurloughedFull;
    }

    public static double getEmploymentsFurloughedFullForMonthYear(int month, int year) {
        if (employmentsFurloughedFull.get(month, year) != null) {
            return ((Number) employmentsFurloughedFull.get(month, year)).doubleValue();
        } else return 0.;
    }

    public static void setEmploymentsFurloughedFull(MultiKeyCoefficientMap employmentsFurloughedFull) {
        Parameters.employmentsFurloughedFull = employmentsFurloughedFull;
    }

    public static MultiKeyCoefficientMap getEmploymentsFurloughedFlex() {
        return employmentsFurloughedFlex;
    }

    public static double getEmploymentsFurloughedFlexForMonthYear(int month, int year) {
        if (employmentsFurloughedFlex.get(month, year) != null) {
            return ((Number) employmentsFurloughedFlex.get(month, year)).doubleValue();
        } else return 0.;
    }

    public static void setEmploymentsFurloughedFlex(MultiKeyCoefficientMap employmentsFurloughedFlex) {
        Parameters.employmentsFurloughedFlex = employmentsFurloughedFlex;
    }

    public static GeneralisedOrderedRegression getRegHealthH1() { return regHealthH1; }
    // public static GeneralisedOrderedRegression getRegHealthH1b() { return regHealthH1b; }
    public static BinomialRegression getRegHealthH2() { return regHealthH2; }

    public static BinomialRegression getRegNeedCareS2a() { return regNeedCareS2a; }
    public static BinomialRegression getRegReceiveCareS2b() { return regReceiveCareS2b; }
    public static MultinomialRegression getRegSocialCareMarketS2c() { return regSocialCareMarketS2c; }
    public static LinearRegression getRegInformalCareHoursS2d() { return regInformalCareHoursS2d; }
    public static LinearRegression getRegFormalCareHoursS2e() { return regFormalCareHoursS2e; }
    // public static MultinomialRegression getRegNotPartnerInformalCareS2f() { return regNotPartnerInformalCareS2f; } // retired process
    // public static LinearRegression getRegCareHoursS1b() { return regSocialCareS1b; } // retired process
    // public static LinearRegression getRegPartnerCareHoursS2g() { return regPartnerCareHoursS2g; } // retired process
    // public static LinearRegression getRegDaughterCareHoursS2h() { return regDaughterCareHoursS2h; } // retired process
    // public static LinearRegression getRegSonCareHoursS2i() { return regSonCareHoursS2i; } // retired process
    // public static LinearRegression getRegOtherCareHoursS2j() { return regOtherCareHoursS2j; } // retired process
    // public static LinearRegression getRegFormalCareHoursS2k() { return regFormalCareHoursS2k; } // retired process
    public static BinomialRegression getRegCarePartnerProvCareToOtherS3a() { return regCarePartnerProvCareToOtherS3a; }
    public static BinomialRegression getRegNoCarePartnerProvCareToOtherS3b() { return regNoCarePartnerProvCareToOtherS3b; }
    public static LinearRegression getRegCareHoursProvS3c() { return regCareHoursProvS3c; }
    public static LinearRegression getRegCareHoursProvS3d() { return regCareHoursProvS3d; }
    // public static LinearRegression getRegCareHoursProvS3e() { return regCareHoursProvS3e; } // retired process

    public static LinearRegression getRegEquivalisedIncomeMales() {return regEquivalisedIncomeMales;}
    public static LinearRegression getRegEquivalisedIncomeFemales() {return regEquivalisedIncomeFemales;}
    public static LinearRegression getRegEquivalisedIncomeDynamics() {return regEquivalisedIncomeDynamics;}
    public static LinearRegression getRegEquivalisedIncomeDynamics2() {return regEquivalisedIncomeDynamics2;}

    public static BinomialRegression getRegUnemploymentMaleGraduateU1a() { return regUnemploymentMaleGraduateU1a; }
    public static BinomialRegression getRegUnemploymentMaleNonGraduateU1b() { return regUnemploymentMaleNonGraduateU1b; }
    public static BinomialRegression getRegUnemploymentFemaleGraduateU1c() { return regUnemploymentFemaleGraduateU1c; }
    public static BinomialRegression getRegUnemploymentFemaleNonGraduateU1d() { return regUnemploymentFemaleNonGraduateU1d; }

    public static BinomialRegression getRegFinancialDistress() { return regFinancialDistress; }

    public static LinearRegression getRegHealthHM1Level() { return regHealthHM1Level; }
    public static LinearRegression getRegHealthHM2LevelMales() { return regHealthHM2LevelMales; }
    public static LinearRegression getRegHealthHM2LevelFemales() { return regHealthHM2LevelFemales; }
    public static OrderedRegression getRegHealthHM1Case() {return regHealthHM1Case;}
    public static LinearRegression getRegHealthHM2CaseMales() {return regHealthHM2CaseMales;}
    public static LinearRegression getRegHealthHM2CaseFemales() {return regHealthHM2CaseFemales;}

    public static LinearRegression getRegHealthMCS1() { return regHealthMCS1; }
    public static LinearRegression getRegHealthMCS2Males() { return regHealthMCS2Males;   }
    public static LinearRegression getRegHealthMCS2Females() { return regHealthMCS2Females; }

    public static LinearRegression getRegHealthPCS1() { return regHealthPCS1; }
    public static LinearRegression getRegHealthPCS2Males() { return regHealthPCS2Males; }
    public static LinearRegression getRegHealthPCS2Females() { return regHealthPCS2Females; }

    public static LinearRegression getRegLifeSatisfaction1() { return regLifeSatisfaction1; }
    public static LinearRegression getRegLifeSatisfaction2Males() { return regLifeSatisfaction2Males; }
    public static LinearRegression getRegLifeSatisfaction2Females() { return regLifeSatisfaction2Females; }

    public static BinomialRegression getRegEducationE1a() {return regEducationE1a;}
    public static BinomialRegression getRegEducationE1b() {return regEducationE1b;}
    public static GeneralisedOrderedRegression getRegEducationE2() {return regEducationE2;}

    public static LinearRegression getRegEQ5D() { return regHealthEQ5D; };

    public static BinomialRegression getRegPartnershipU1() {return regPartnershipU1;}
    // public static BinomialRegression getRegPartnershipU1b() {return regPartnershipU1b;}
    public static BinomialRegression getRegPartnershipU2() {return regPartnershipU2;}
    public static BinomialRegression getRegPartnershipITU1() {return regPartnershipITU1;}
    public static BinomialRegression getRegPartnershipITU2() {return regPartnershipITU2;}

    public static BinomialRegression getRegFertilityF1() {return regFertilityF1;}

    public static BinomialRegression getRegIncomeI1a() {return regIncomeI1a;}
    public static LinearRegression getRegIncomeI1b() {return regIncomeI1b;}
    public static LinearRegression getRegIncomeI2b() { return regIncomeI2b; }
    public static BinomialRegression getRegIncomeI3a() { return regIncomeI3a; }
    public static LinearRegression getRegIncomeI3b() { return regIncomeI3b; }
    // public static LinearRegression getRegIncomeI3c() { return regIncomeI3c; }
    // public static LinearRegression getRegIncomeI4a() { return regIncomeI4a; }
    // public static LinearRegression getRegIncomeI4b() {return regIncomeI4b;}
    // public static LinearRegression getRegIncomeI5a() {return regIncomeI5a;}
    // public static LinearRegression getRegIncomeI6b_amount() { return regIncomeI6b_amount; }
    // public static BinomialRegression getRegIncomeI3a_selection() { return regIncomeI3a_selection; }
    // public static BinomialRegression getRegIncomeI3b_selection() { return regIncomeI3b_selection; }
    // public static BinomialRegression getRegIncomeI5a_selection() { return regIncomeI5a_selection; }
    // public static BinomialRegression getRegIncomeI6a_selection() { return regIncomeI6a_selection; }

    public static BinomialRegression getRegHomeownershipHO1a() {return regHomeownershipHO1a;}

    public static Set<Region> getCountryRegions() {
        return countryRegions;
    }

    public static MultiKeyMap getFertilityRateByRegionYear() {
        return fertilityRateByRegionYear;
    }

    public static double getFertilityRateByRegionYear(Region demRgn, int year) {
        int yearHere = Math.max(fertilityProjectionsMinYear, Math.min(fertilityProjectionsMaxYear, year));
        //We calculate the rate per woman, but the standard to report (and what is used in the estimates) is per 1000 hence multiplication
        return 1000*((Number)fertilityRateByRegionYear.get(demRgn, yearHere)).doubleValue();
    }

    public static double getUnemploymentRateByGenderEducationAgeYear(Gender demSex, Education education, int age, int year) {
        double val;
        if (demSex.equals(Gender.Male)) {
            if (education.equals(Education.High)) {
                int yearHere = Math.max(unemploymentRatesMaleGraduatesMinYear, Math.min(unemploymentRatesMaleGraduatesMaxYear, year));
                int ageHere = Math.min(unemploymentRatesMaleGraduatesMaxAge, age);
                val = ((Number)unemploymentRatesMaleGraduatesByAgeYear.getValue(ageHere, yearHere)).doubleValue();
            } else {
                int yearHere = Math.max(unemploymentRatesMaleNonGraduatesMinYear, Math.min(unemploymentRatesMaleNonGraduatesMaxYear, year));
                int ageHere = Math.min(unemploymentRatesMaleNonGraduatesMaxAge, age);
                val = ((Number) unemploymentRatesMaleNonGraduatesByAgeYear.getValue(ageHere, yearHere)).doubleValue();
            }
        } else {
            if (education.equals(Education.High)) {
                int yearHere = Math.max(unemploymentRatesFemaleGraduatesMinYear, Math.min(unemploymentRatesFemaleGraduatesMaxYear, year));
                int ageHere = Math.min(unemploymentRatesFemaleGraduatesMaxAge, age);
                val = ((Number)unemploymentRatesFemaleGraduatesByAgeYear.getValue(ageHere, yearHere)).doubleValue();
            } else {
                int yearHere = Math.max(unemploymentRatesFemaleNonGraduatesMinYear, Math.min(unemploymentRatesFemaleNonGraduatesMaxYear, year));
                int ageHere = Math.min(unemploymentRatesFemaleNonGraduatesMaxAge, age);
                val = ((Number)unemploymentRatesFemaleNonGraduatesByAgeYear.getValue(ageHere, yearHere)).doubleValue();
            }
        }
        return val;
    }

    public static LinearRegression getRegWagesMales() {
        return regWagesMales;
    }

    public static LinearRegression getRegWagesFemales() {
        return regWagesFemales;
    }

    public static String getTaxDonorInputFileName() {
        return taxDonorInputFileName;
    }

    public static void setTaxDonorInputFileName(String taxDonorInputFileName) {
        Parameters.taxDonorInputFileName = taxDonorInputFileName;
    }

    public static String getPopulationInitialisationInputFileName() {
        return populationInitialisationInputFileName;
    }
    public static String getPopulationInitialisationFilePath() {
        return getInputDirectoryInitialPopulations() + populationInitialisationInputFileName;
    }

    public static void setPopulationInitialisationInputFileName(String name) {
        populationInitialisationInputFileName = name;
    }

    public static LinearRegression getRegLabourSupplyUtilityCouples() {
        return regLabourSupplyUtilityCouples;
    }

    public static LinearRegression getRegLabourSupplyUtilityFemales() {
        return regLabourSupplyUtilityFemales;
    }

    public static LinearRegression getRegLabourSupplyUtilityMales() {
        return regLabourSupplyUtilityMales;
    }

    public static LinearRegression getRegLabourSupplyUtilitySingleDep() {
        return regLabourSupplyUtilitySingleDep;
    }

    public static LinearRegression getRegLabourSupplyUtilityACMales() {
        return regLabourSupplyUtilityACMales;
    }

    public static LinearRegression getRegLabourSupplyUtilityACFemales() {
        return regLabourSupplyUtilityACFemales;
    }

    public static LinearRegression getRegEmploymentSelectionMale() {
        return regEmploymentSelectionMale;
    }

    public static LinearRegression getRegEmploymentSelectionFemale() {
        return regEmploymentSelectionFemale;
    }

    public static MultiKeyMap<Object, Double> getPopulationGrowthRatiosByRegionYear() {
        return populationGrowthRatiosByRegionYear;
    }


    public static DemandAdjustment getDemandAdjustment() {
        return demandAdjustment;
    }

    public static int getMaxStartYear() {
        if (TESTING_FLAG)
            return MAX_START_YEAR_TESTING;
        else
            return (trainingFlag) ? MAX_START_YEAR_TRAINING : MAX_START_YEAR;
    }

    public static int getMinStartYear() {
        if (TESTING_FLAG)
            return MIN_START_YEAR_TESTING;
        else
            return (trainingFlag) ? MIN_START_YEAR_TRAINING : MIN_START_YEAR;
    }

    public static String getEuromodOutputDirectory() {
        if (TESTING_FLAG)
            return EUROMOD_OUTPUT_DIRECTORY;
        else
            return (trainingFlag) ? EUROMOD_TRAINING_DIRECTORY : EUROMOD_OUTPUT_DIRECTORY;
    }

    public static String getEUROMODpolicyForThisYear(int year) {
        return getEUROMODpolicyForThisYear(year, EUROMODpolicySchedule);
    }

    /*
    public static MultiKeyMap getProbSick() {
        return probSick;
    }
    */
    /*
    public static double getUnemploymentRatesForRegion(Region demRgn) {
        return unemploymentRatesByRegion.get(demRgn);
    }
    */

    public static MultiKeyCoefficientMap getMarriageTypesFrequency() {
        return marriageTypesFrequency;
    }

    public static MultiKeyCoefficientMap getCoeffCovarianceHealthH1() { return coeffCovarianceHealthH1; }


    public static MultiKeyCoefficientMap getCoeffCovarianceWagesMalesE() { return coeffCovarianceWagesMalesE; }
    public static MultiKeyCoefficientMap getCoeffCovarianceWagesMalesNE() { return coeffCovarianceWagesMalesNE; }
    public static MultiKeyCoefficientMap getCoeffCovarianceWagesFemalesE() { return coeffCovarianceWagesFemalesE; }
    public static MultiKeyCoefficientMap getCoeffCovarianceWagesFemalesNE() { return coeffCovarianceWagesFemalesNE; }
    public static MultiKeyCoefficientMap getCoefficientMapRMSE() { return coefficientMapRMSE; }


    public static double getMortalityProbability(Gender demSex, int age, int year) {

        double mortalityProbability;
        int yearEval = Math.min(mortalityProbabilityMaxYear, Math.max(mortalityProbabilityMinYear, year));
        int ageEval = Math.min(mortalityProbabilityMaxAge, age);
        Number prob = ((Number) mortalityProbabilityByGenderAgeYear.getValue(demSex.toString(), ageEval, yearEval));
        if (prob==null) {
            throw new IllegalAccessError("ERROR - problem evaluating mortality probability for year: " + yearEval + ", age: " + ageEval + " and demSex " + demSex.toString());
        }
        mortalityProbability = prob.doubleValue() / 100000.0;

        return mortalityProbability;
    }

    public static double getEquivalisedIncomeDraw(double rnd) {
        return equivalisedIncomeCDF.getValue(rnd);
    }

    public static double getEquivalisedIncomeDraw2(double rnd) {
        return equivalisedIncomeCDF2.getValue(rnd);
    }

    public static Double getEquivalisedIncome(Gender demSex, int age, int year) {

        Double val = null;
        if (year<=equivalisedIncomeMaxYear && year>=equivalisedIncomeMinYear && age<=equivalisedIncomeMaxAge) {
            // obtain value from observed matrix

            Number nn = ((Number) equivalisedIncomeByGenderAgeYear.getValue(demSex.toString(), age, year));
            if (nn==null) {
                throw new IllegalAccessError("ERROR - problem evaluating mean equivalised income for year: " + year + ", age: " + age + " and demSex " + demSex.toString());
            }
            val = nn.doubleValue();
        }
        return val;
    }

    public static double getPopulationProjections(Gender demSex, Region demRgn, int age, int year) {

        double populationProjection;
        int yearEval = Math.min(populationProjectionsMaxYear, Math.max(populationProjectionsMinYear, year));
        int ageEval = Math.min(populationProjectionsMaxAge, age);
        Number val = ((Number)populationProjections.getValue(demSex.toString(), demRgn.toString(), ageEval, yearEval));
        if (val==null)
            throw new IllegalAccessError("ERROR - problem evaluating population projection for year: " + yearEval + ", age: " + ageEval + ", demRgn: " + demRgn.toString() + " and demSex: " + demSex.toString());
        populationProjection = val.doubleValue();

        return populationProjection;
    }

    public static int getPopulationProjectionsMaxAge() { return populationProjectionsMaxAge; }

    public static int getPopulationProjectionsMaxYear() { return populationProjectionsMaxYear; }

    public static MultiKeyCoefficientMap getFertilityProjectionsByYear() { return fertilityProjectionsByYear; }
    public static double getFertilityProjectionsByYear(int year) {
        int yearHere = Math.min(fertilityProjectionsMaxYear, Math.max(fertilityProjectionsMinYear, year));
        return ((Number) fertilityProjectionsByYear.getValue("Value", yearHere)).doubleValue();
    }

    public synchronized static double[] getWageAndAgeDifferentialMultivariateNormalDistribution(long statSeed) {
        wageAndAgeDifferentialMultivariateNormalDistribution.reseedRandomGenerator(statSeed);
        return wageAndAgeDifferentialMultivariateNormalDistribution.sample();
    }

    public static BinomialRegression getRegLeaveHomeP1a() {
        return regLeaveHomeP1a;
    }

    public static BinomialRegression getRegRetirementR1a() {
        return regRetirementR1a;
    }

    public static BinomialRegression getRegRetirementR1b() {
        return regRetirementR1b;
    }

    public static BinomialRegression getRegChildcareC1a() { return regChildcareC1a; }

    public static LinearRegression getRegChildcareC1b() {
        return regChildcareC1b;
    }

    ///////////////////////////////////////////GETTERS FOR VALIDATION///////////////////////////////////////////////////
    public static MultiKeyCoefficientMap getValidationStudentsByAge() {
        return validationStudentsByAge;
    }

    public static MultiKeyCoefficientMap getValidationStudentsByRegion() {
        return validationStudentsByRegion;
    }

    public static MultiKeyCoefficientMap getValidationEducationLevel() {
        return validationEducationLevel;
    }

    public static MultiKeyCoefficientMap getValidationEducationLevelByAge() {
        return validationEducationLevelByAge;
    }

    public static MultiKeyCoefficientMap getValidationEducationLevelByRegion() {
        return validationEducationLevelByRegion;
    }

    public static MultiKeyCoefficientMap getValidationPartneredShareByRegion() {
        return validationPartneredShareByRegion;
    }

    public static MultiKeyCoefficientMap getValidationDisabledByAge() {
        return validationDisabledByAge;
    }

    public static MultiKeyCoefficientMap getValidationDisabledByGender() {
        return validationDisabledByGender;
    }

    public static MultiKeyCoefficientMap getValidationHealthByAge() {
        return validationHealthByAge;
    }

    public static MultiKeyCoefficientMap getValidationMentalHealthByAge() {
        return validationMentalHealthByAge;
    }

    public static MultiKeyCoefficientMap getValidationHealthMCSByAge() {
        return validationHealthMCSByAge;
    }

    public static MultiKeyCoefficientMap getValidationHealthPCSByAge() {
        return validationHealthPCSByAge;
    }

    public static MultiKeyCoefficientMap getValidationLifeSatisfactionByAge() {
        return validationLifeSatisfactionByAge;
    }


    public static MultiKeyCoefficientMap getValidationPsychDistressByAge() {
        return validationPsychDistressByAge;
    }

    public static MultiKeyCoefficientMap getValidationPsychDistressByAgeLow() {
        return validationPsychDistressByAgeLow;
    }

    public static MultiKeyCoefficientMap getValidationPsychDistressByAgeMed() {
        return validationPsychDistressByAgeMed;
    }

    public static MultiKeyCoefficientMap getValidationPsychDistressByAgeHigh() {
        return validationPsychDistressByAgeHigh;
    }

    public static MultiKeyCoefficientMap getValidationEmploymentByGender() {
        return validationEmploymentByGender;
    }

    public static MultiKeyCoefficientMap getValidationEmploymentByAgeAndGender() {
        return validationEmploymentByAgeAndGender;
    }

    public static MultiKeyCoefficientMap getValidationEmploymentByMaternity() {
        return validationEmploymentByMaternity;
    }

    public static MultiKeyCoefficientMap getValidationEmploymentByGenderAndRegion() {
        return validationEmploymentByGenderAndRegion;
    }

    public static MultiKeyCoefficientMap getValidationLabourSupplyByEducation() {
        return validationLabourSupplyByEducation;
    }

    public static MultiKeyCoefficientMap getValidationActivityStatus() {
        return validationActivityStatus;
    }

    public static MultiKeyCoefficientMap getValidationHomeownershipBenefitUnits() {
        return validationHomeownershipBenefitUnits;
    }

    public static MultiKeyCoefficientMap getValidationGrossEarningsByGenderAndEducation() {
        return validationGrossEarningsByGenderAndEducation;
    }

    public static MultiKeyCoefficientMap getValidationLhwByGenderAndEducation() {
        return validationLhwByGenderAndEducation;
    }

    public static MultiKeyCoefficientMap getHourlyWageByGenderAndEducation() {
        return hourlyWageByGenderAndEducation;
    }

    ///////////////////////////////////////////GETTERS FOR COVID-19 LABOUR TRANSITIONS//////////////////////////////////
    public static BinomialRegression getRegC19LS_SE() {
        return regC19LS_SE;
    }

    public static MultinomialRegression getRegC19LS_E1() {return regC19LS_E1;}
    public static MultinomialRegression getRegC19LS_FF1() {return regC19LS_FF1;}
    public static MultinomialRegression getRegC19LS_FX1() {return regC19LS_FX1;}
    public static MultinomialRegression getRegC19LS_S1() {return regC19LS_S1;}
    public static MultinomialRegression getRegC19LS_U1() {return regC19LS_U1;}

    public static LinearRegression getRegC19LS_E2a() {
        return regC19LS_E2a;
    }

    public static LinearRegression getRegC19LS_E2b() {
        return regC19LS_E2b;
    }

    public static LinearRegression getRegC19LS_F2a() {
        return regC19LS_F2a;
    }

    public static LinearRegression getRegC19LS_F2b() {
        return regC19LS_F2b;
    }

    public static LinearRegression getRegC19LS_F2c() {
        return regC19LS_F2c;
    }

    public static LinearRegression getRegC19LS_S2a() {
        return regC19LS_S2a;
    }

    public static LinearRegression getRegC19LS_U2a() {
        return regC19LS_U2a;
    }

    public static BinomialRegression getRegC19LS_S3() {
        return regC19LS_S3;
    }

    public static LinearRegression getRegWagesMalesE() {
        return regWagesMalesE;
    }

    public static LinearRegression getRegWagesMalesNE() {
        return regWagesMalesNE;
    }

    public static LinearRegression getRegWagesFemalesE() {
        return regWagesFemalesE;
    }

    public static LinearRegression getRegWagesFemalesNE() {
        return regWagesFemalesNE;
    }

    public static LinearRegression getRegEmploymentSelectionMaleE() {
        return regEmploymentSelectionMaleE;
    }

    public static LinearRegression getRegEmploymentSelectionMaleNE() {
        return regEmploymentSelectionMaleNE;
    }

    public static LinearRegression getRegEmploymentSelectionFemaleE() {
        return regEmploymentSelectionFemaleE;
    }

    public static LinearRegression getRegEmploymentSelectionFemaleNE() {
        return regEmploymentSelectionFemaleNE;
    }

    public static void loadTimeSeriesFactorMaps(Country country) {

        // load demYear varying rates
        priceMapRealSavingReturns = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_saving_returns", 1, 1);
        priceMapRealDebtCostLow = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_debt_cost_low", 1, 1);
        priceMapRealDebtCostHigh = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_debt_cost_hi", 1, 1);

        // load demYear varying wage rates
        wageRateFormalSocialCare = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_carer_hourly_wage", 1, 1);

        // load demYear varying indices
        upratingIndexMapRealGDP = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_gdp", 1, 1);
        upratingIndexMapInflation = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_inflation", 1, 1);
        upratingIndexMapRealWageGrowth = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_wage_growth", 1, 1);
        socialCareProvisionTimeAdjustment = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_care_adjustment", 1, 1);
        partnershipTimeAdjustment = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_cohabitation_adjustment", 1, 1);
        studentsTimeAdjustment = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() +"_students_adjustment", 1, 1);
        fertilityTimeAdjustment = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_fertility_adjustment", 1, 1);
        utilityTimeAdjustmentSingleMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_utility_adj_smales", 1, 1);
        utilityTimeAdjustmentSingleFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_utility_adj_sfemales", 1, 1);
        utilityTimeAdjustmentACMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_utility_adj_acmales", 1, 1);
        utilityTimeAdjustmentACFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_utility_adj_acfemales", 1, 1);

        utilityTimeAdjustmentCouples = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_utility_adj_couples", 1, 1);
        utilityTimeAdjustmentSingleDepMen = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_utility_adj_singledepmen", 1, 1);
        utilityTimeAdjustmentSingleDepWomen = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_utility_adj_singledepwomen", 1, 1);


        // rebase indices to base year defined by BASE_PRICE_YEAR
        rebaseIndexMap(TimeSeriesVariable.GDP);
        rebaseIndexMap(TimeSeriesVariable.Inflation);
        rebaseIndexMap(TimeSeriesVariable.WageGrowth);

        //studentShare = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "policy parameters.xlsx", "students", 1,1);
        studentShare = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "inSchool_targets.xlsx", "students", 1,1);

        // load year-specific fiscal policy parameters
        socialCarePolicy = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "policy parameters.xlsx", "social care", 1, 8);
        partneredShare = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "policy parameters.xlsx", "partnership", 1, 1);
        //employedShareSingleMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "policy parameters.xlsx", "employment_smales", 1, 1);
        //employedShareSingleFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "policy parameters.xlsx", "employment_sfemales", 1, 1);
        //employedShareCouples = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "policy parameters.xlsx", "employment_couples", 1, 1);

        employedShareSingleMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "employment_targets.xlsx", "Single_male", 1,1);
        employedShareACMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "employment_targets.xlsx", "SingleAC_Males", 1,1);
        employedShareSingleFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() +  "employment_targets.xlsx", "Single_female", 1,1);
        employedShareACFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "employment_targets.xlsx", "SingleAC_Females", 1,1);
        employedShareCouples = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "employment_targets.xlsx", "Couples", 1,1);
        employedShareSingleDepMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "employment_targets.xlsx", "SingleDep_Males", 1,1);
        employedShareSingleDepFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "employment_targets.xlsx", "SingleDep_Females", 1,1);

    }

    public static void instantiateAlignmentMaps() {
        partnershipAlignAdjustment = new HashMap<>();
        fertilityAlignAdjustment = new HashMap<>();
        for (int yy=startYear; yy<=endYear; yy++) {
            partnershipAlignAdjustment.put(yy,0.0);
            fertilityAlignAdjustment.put(yy,0.0);
        }
    }

    public static void loadTimeSeriesFactorForTaxDonor(Country country) {

        TimeSeriesVariable index = getTimeSeriesVariable(UpratingCase.TaxDonor);
        switch (index) {
            case GDP -> {
                upratingIndexMapRealGDP = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_gdp", 1, 1);
                rebaseIndexMap(TimeSeriesVariable.GDP);
            }
            case WageGrowth -> {
                upratingIndexMapRealWageGrowth = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_wage_growth", 1, 1);
                rebaseIndexMap(TimeSeriesVariable.WageGrowth);
            }
            case Inflation -> {
                upratingIndexMapInflation = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_inflation", 1, 1);
                rebaseIndexMap(TimeSeriesVariable.Inflation);
            }
        }
    }

    private static void rebaseIndexMap(TimeSeriesVariable indexType) {
        rebaseIndexMap(indexType, BASE_PRICE_YEAR, true);
    }

    private static void rebaseIndexMap(TimeSeriesVariable timeSeriesVariable, int baseYear, boolean ratioAdjust) {

        MultiKeyCoefficientMap map = getTimeSeriesValueMap(timeSeriesVariable);
        double valueBase = getTimeSeriesValue(baseYear, timeSeriesVariable);
        for (Object key: map.keySet()) {

            double valueHere = ((Number) map.getValue(key)).doubleValue();
            if (ratioAdjust) {
                map.replace(key, valueHere/valueBase);
            } else {
                map.replace(key, valueHere - valueBase);
            }
        }
    }

    private static MultiKeyCoefficientMap getTimeSeriesValueMap(TimeSeriesVariable timeSeriesVariable) {

        MultiKeyCoefficientMap map = null;
        switch (timeSeriesVariable) {
            case GDP -> {
                map = upratingIndexMapRealGDP;
            }
            case GDPperCapita -> {
                map = mapRealGDPperCapita;
            }
            case Inflation -> {
                map = upratingIndexMapInflation;
            }
            case WageGrowth -> {
                map = upratingIndexMapRealWageGrowth;
            }
            case CarerWageRate -> {
                map = wageRateFormalSocialCare;
            }
            case CareProvisionAdjustment -> {
                map = socialCareProvisionTimeAdjustment;
            }
            case PartnershipAdjustment -> {
                map = partnershipTimeAdjustment;
            }
            case FertilityAdjustment -> {
                map = fertilityTimeAdjustment;
            }
            case UtilityAdjustmentSingleMales -> {
                map = utilityTimeAdjustmentSingleMales;
            }
            case UtilityAdjustmentACMales -> {
                map = utilityTimeAdjustmentACMales;
            }
            case UtilityAdjustmentSingleFemales -> {
                map = utilityTimeAdjustmentSingleFemales;
            }
            case UtilityAdjustmentACFemales -> {
                map = utilityTimeAdjustmentACFemales;
            }
            case UtilityAdjustmentSingleDepMen -> {
                map = utilityTimeAdjustmentSingleDepMen;
            }
            case UtilityAdjustmentSingleDepWomen -> {
                map = utilityTimeAdjustmentSingleDepWomen;
            }

            case UtilityAdjustmentCouples -> {
                map = utilityTimeAdjustmentCouples;
            }
            case InSchoolAdjustment -> {
                map = studentsTimeAdjustment;
            }
            case HighEducationRate -> {
                map = projectionsHighEdu;
            }
            case LowEducationRate -> {
                map = projectionsLowEdu;
            }
            case EmploymentAlignment -> {
                map = employmentAlignment;
            }
            case FixedRetirementAge -> {
                map = fixedRetireAge;
            }
        }
        return map;
    }

    private static MultiKeyCoefficientMap getTargetShareMap(TargetShares targetShareType) {

        MultiKeyCoefficientMap map = null;
        switch (targetShareType) {
            case Partnership -> {
                map = partneredShare;
            }
            case Students -> {
                map = studentShare;
            }
            case EmploymentSingleMales -> {
                map = employedShareSingleMales;
            }
            case EmploymentACMales -> {
                map = employedShareACMales;
            }
            case EmploymentSingleFemales -> {
                map = employedShareSingleFemales;
            }
            case EmploymentACFemales -> {
                map = employedShareACFemales;
            }
            case EmploymentSingleDepMales -> {
                map = employedShareSingleDepMales;
            }
            case EmploymentSingleDepFemales -> {
                map = employedShareSingleDepFemales;
            }
            case EmploymentCouples -> {
                map = employedShareCouples;
            }
        }

        return map;
    }

    public static Double getTimeSeriesIndex(int year, UpratingCase upratingCase) {

        TimeSeriesVariable timeSeriesVariable = getTimeSeriesVariable(upratingCase);
        return getTimeSeriesValue(year, timeSeriesVariable);
    }

    private static TimeSeriesVariable getTimeSeriesVariable(UpratingCase upratingCase) {

        TimeSeriesVariable timeSeriesVariable = null;
        switch (upratingCase) {
            case Capital, ModelInitialise, Pension -> {
                timeSeriesVariable = TimeSeriesVariable.GDP;
            }
            case Earnings -> {
                timeSeriesVariable = TimeSeriesVariable.WageGrowth;
            }
            case TaxDonor -> {
                timeSeriesVariable = TimeSeriesVariable.Inflation;
            }
        }
        return timeSeriesVariable;
    }

    public static double getTimeSeriesValue(int year, TimeSeriesVariable timeSeriesVariable) {
        return getTimeSeriesValue(year, null, null, timeSeriesVariable);
    }

    public static double getTimeSeriesValue(int year, String stringKey1, TimeSeriesVariable timeSeriesVariable) {
        return getTimeSeriesValue(year, stringKey1, null, timeSeriesVariable);
    }

    public static double getTimeSeriesValue(int year, String stringKey1, String stringKey2, TimeSeriesVariable timeSeriesVariable) {

        MultiKeyCoefficientMap valueMap = getTimeSeriesValueMap(timeSeriesVariable);
        Object val = getObjectFromTimeSeriesValueMap(year, stringKey1, stringKey2, valueMap);
        if (val == null)
            val = extendValueTimeSeries(year, stringKey1, stringKey2, valueMap);
        return ((Number) val).doubleValue();
    }

    private static Object getObjectFromTimeSeriesValueMap(int year, String stringKey1, String stringKey2, MultiKeyCoefficientMap map) {
        if (stringKey1==null)
            return map.getValue(year);
        else if (stringKey2==null)
            return map.getValue(year, stringKey1);
        else
            return map.getValue(stringKey1, stringKey2, year);
    }

    public static void putTimeSeriesValue(int year, Object valPut, TimeSeriesVariable variableType) {
        putTimeSeriesValue(year, null, null, valPut, variableType);
    }

    public static void putTimeSeriesValue(int year, String stringKey1, Object valPut, TimeSeriesVariable variableType) {
        putTimeSeriesValue(year, stringKey1, null, valPut, variableType);
    }

    public static void putTimeSeriesValue(int year, String stringKey1, String stringKey2, Object valPut, TimeSeriesVariable variableType) {

        MultiKeyCoefficientMap valueMap = getTimeSeriesValueMap(variableType);
        putTimeSeriesValue(year, stringKey1, stringKey2, valPut, valueMap);
    }

    public static void putTimeSeriesValue(int year, String stringKey1, String stringKey2, Object valPut, MultiKeyCoefficientMap valueMap) {

        Object val = getObjectFromTimeSeriesValueMap(year, stringKey1, stringKey2, valueMap);
        if (val == null) {
            if (stringKey1==null)
                valueMap.putValue(year, valPut);
            else if (stringKey2==null)
                valueMap.putValue(year, stringKey1, valPut);
            else
                valueMap.putValue(stringKey1, stringKey2, year, valPut);
        } else {
            if (stringKey1==null)
                valueMap.replaceValue(year, valPut);
            else if (stringKey2==null)
                valueMap.replaceValue(year, stringKey1, valPut);
            else
                valueMap.replaceValue(stringKey1, stringKey2, year, valPut);
        }
    }

    private synchronized static Object extendValueTimeSeries(int year, String stringKey1, String stringKey2, MultiKeyCoefficientMap mapToExtend) {

        Object valObj = getObjectFromTimeSeriesValueMap(year, stringKey1, stringKey2, mapToExtend);
        if (valObj == null) {
            // assume that series is capped at end by assumed geometric growth rate

            int mapYear = year;
            while (valObj == null) {

                if ( year < getMinStartYear() ) {
                    // year must be below lower bound of series - search up
                    mapYear++;
                } else {
                    // year must be above upper bound of series - search down
                    mapYear--;
                }
                valObj = getObjectFromTimeSeriesValueMap(mapYear, stringKey1, stringKey2, mapToExtend);
            }
            double val = ((Number)valObj).doubleValue();
            double growthFactor = 0.0;
            if (year > mapYear) {
                // extend series forward through demYear

                if (Math.abs(val)>1.0E-9) {
                    // assume constant exponential growth rate

                    Object valObj1 = getObjectFromTimeSeriesValueMap(mapYear-1, stringKey1, stringKey2, mapToExtend);
                    double val1 = ((Number)valObj1).doubleValue();
                    growthFactor = val / val1;
                }
                for (int yy = mapYear + 1; yy <= year; yy++) {
                    val *= growthFactor;
                    putTimeSeriesValue(yy, stringKey1, stringKey2, val, mapToExtend);
                }
            } else {
                // extend series backward through demYear

                if (Math.abs(val)>1.0E-9) {
                    // assume constant exponential growth rate

                    Object valObj1 = getObjectFromTimeSeriesValueMap(mapYear+1, stringKey1, stringKey2, mapToExtend);
                    double val1 = ((Number)valObj1).doubleValue();
                    growthFactor = val / val1;
                }
                for (int yy = mapYear - 1; yy >= year; yy--) {
                    val *= growthFactor;
                    putTimeSeriesValue(yy, stringKey1, stringKey2, val, mapToExtend);
                }
            }
        }
        return getObjectFromTimeSeriesValueMap(year, stringKey1, stringKey2, mapToExtend);
    }

    private static MultiKeyCoefficientMap getTimeSeriesRateMap(TimeVaryingRate rateType) {

        MultiKeyCoefficientMap map = null;
        switch (rateType) {
            case RealSavingReturns -> {
                map = priceMapRealSavingReturns;
            }
            case RealDebtCostLow -> {
                map = priceMapRealDebtCostLow;
            }
            case RealDebtCostHigh -> {
                map = priceMapRealDebtCostHigh;
            }
        }

        return map;
    }

    private static Double getTimeSeriesRateParameter(TimeVaryingRate rateType) {

        switch (rateType) {
            case RealSavingReturns -> {
                return averageSavingReturns;
            }
            case RealDebtCostLow -> {
                return averageDebtCostLow;
            }
            case RealDebtCostHigh -> {
                return averageDebtCostHigh;
            }
        }
        throw new RuntimeException("failed to find requested demYear varying rate");
    }

    private static void setTimeSeriesRateParameter(TimeVaryingRate rateType, double val) {

        switch (rateType) {
            case RealSavingReturns -> {
                averageSavingReturns = val;
                return;
            }
            case RealDebtCostLow -> {
                averageDebtCostLow = val;
                return;
            }
            case RealDebtCostHigh -> {
                averageDebtCostHigh = val;
                return;
            }
        }
        throw new RuntimeException("failed to find requested demYear varying rate");
    }

    public static double getTimeSeriesRate(int year, TimeVaryingRate rateType) {

        if (flagDefaultToTimeSeriesAverages) {
            return getSampleAverageRate(rateType);
        } else {
            MultiKeyCoefficientMap rateMap = getTimeSeriesRateMap(rateType);
            Object val = rateMap.getValue(year);
            if (val == null)
                val = extendRateTimeSeries(year, rateMap);
            return ((Number) val).doubleValue();
        }
    }

    private synchronized static Object extendRateTimeSeries(int year, MultiKeyCoefficientMap mapToExtend) {

        Object val = mapToExtend.getValue(year);
        if (val == null) {
            // assume that series is capped at end by rates to assume for out-of sample projections

            Integer yearMin = null, yearMax = null;
            for (Object key: mapToExtend.keySet()) {
                // loop over all existing keys to search for min and max

                int yearHere = key.hashCode();
                if (yearHere>0) {

                    if (yearHere<1900 || yearHere>2500)
                        throw new RuntimeException("problem extending time series");
                    if (yearMin == null) {
                        yearMin = yearHere;
                        yearMax = yearHere;
                    }
                    if (yearHere < yearMin) yearMin = yearHere;
                    if (yearHere > yearMax) yearMax = yearHere;
                }
            }
            if (year > yearMax) {
                // extend series forward through demYear

                double outOfSampleRate = ((Number) mapToExtend.getValue(yearMax)).doubleValue();
                for (int yy = yearMax + 1; yy <= year; yy++) {
                    mapToExtend.putValue(yy, outOfSampleRate);
                }
                val = outOfSampleRate;
            }
            if (year < yearMin) {
                // extend series backward through demYear

                double outOfSampleRate = ((Number) mapToExtend.getValue(yearMin)).doubleValue();
                for (int yy = yearMin - 1; yy >= year; yy--) {
                    mapToExtend.putValue(yy, outOfSampleRate);
                }
                val = outOfSampleRate;
            }
        }
        return val;
    }

    public static double getSampleAverageRate(TimeVaryingRate rateType) {

        Double val = getTimeSeriesRateParameter(rateType);
        if (!checkFinite(val)) {

            val = 0.0;
            double nn = 0.0;
            MultiKeyCoefficientMap rateMapToUse = getTimeSeriesRateMap(rateType);
            for (Object key: rateMapToUse.keySet()) {
                // loop over all existing keys to obtain sample average

                nn ++;
                val += ((Number) rateMapToUse.getValue(key)).doubleValue();
            }
            val /= nn;
            setTimeSeriesRateParameter(rateType, val);
        }
        return val;
    }

    public static void setEnableIntertemporalOptimisations(boolean val) {
        enableIntertemporalOptimisations = val;
    }
    public static void setProjectLiquidWealth() {
        setProjectLiquidWealth(enableIntertemporalOptimisations);
    }
    public static void setProjectLiquidWealth(boolean val) {
        projectLiquidWealth = val;
    }

    public static double getTargetShare(int year, TargetShares targetShareType) {

        MultiKeyCoefficientMap map = getTargetShareMap(targetShareType);
        Object val = map.getValue(year);
        if (val == null)
            val = extendRateTimeSeries(year, map);
        return ((Number) val).doubleValue();
    }

    public static double getSocialCarePolicyValue(int year, String param) {

        Object val = socialCarePolicy.getRowColumnValue(year, param);
        if (val == null)
            val = extendSocialCarePolicy(year, param);
        return ((Number) val).doubleValue();
    }

    public synchronized static Object extendSocialCarePolicy(int year, String param) {

        Object val = socialCarePolicy.getRowColumnValue(year, param);
        if (val==null) {
            // assume that series is capped at end by assumed geometric growth rate

            int minYear = 9999, maxYear=0;
            for (Object key : socialCarePolicy.keySet()) {
                if (key.hashCode()>0) {
                    if (key.hashCode()<1900 || key.hashCode()>2500)
                        throw new RuntimeException("problem extending time series for social policy parameters");
                    if (key.hashCode() < minYear)
                        minYear = key.hashCode();
                    if (key.hashCode() > maxYear)
                        maxYear = key.hashCode();
                }
            }
            if (year < minYear) {
                // extend backward through demYear

                Object[] values0 = (Object[]) socialCarePolicy.getValue(minYear+1);
                Object[] values1 = (Object[]) socialCarePolicy.getValue(minYear);
                for( int yy=minYear-1; yy>=year; yy--) {
                    Object[] values2 = new Object[values1.length];
                    for (int ii=0; ii<values1.length; ii++) {
                        double val0 = ((Number) values0[ii]).doubleValue();
                        double val1 = ((Number) values1[ii]).doubleValue();
                        values2[ii] = val1 * val1 / val0;
                    }
                    socialCarePolicy.putValue(yy, values2);
                    values0 = values1;
                    values1 = values2;
                }
            }
            else {
                // extend forward through demYear

                Object[] values0 = (Object[]) socialCarePolicy.getValue(maxYear-1);
                Object[] values1 = (Object[]) socialCarePolicy.getValue(maxYear);
                for( int yy=maxYear+1; yy<=year; yy++) {
                    Object[] values2 = new Object[values1.length];
                    for (int ii=0; ii<values1.length; ii++) {
                        double val0 = ((Number) values0[ii]).doubleValue();
                        double val1 = ((Number) values1[ii]).doubleValue();
                        values2[ii] = val1 * val1 / val0;
                    }
                    socialCarePolicy.putValue(yy, values2);
                    values0 = values1;
                    values1 = values2;
                }
            }
            val = socialCarePolicy.getRowColumnValue(year, param);
        }
        return val;
    }

    public static Map<MatchFeature, Map<Integer, Integer>> getTaxdbCounter() {
        return taxdbCounter;
    }
    public static void setTaxdbCounter(Map<MatchFeature, Map<Integer, Integer>> map) {
        taxdbCounter = map;
    }

    /**
     * Triple: system year, matching regime, regime index
     * @return
     */
    public static Map<Triple<Integer,Integer,Integer>,List<Integer>> getTaxdbReferences() {
        return taxdbReferences;
    }
    public static void setTaxdbReferences(Map<Triple<Integer,Integer,Integer>,List<Integer>> map) {
        taxdbReferences = map;
    }
    public static List<DonorTaxUnit> getDonorPool() {
        return donorPool;
    }
    public static void setDonorPool(List<DonorTaxUnit> list) {
        donorPool = list;
    }
    public static double asinh(double xx) {
        return Math.log(xx + Math.sqrt(xx * xx + 1.0));
    }
    public static void setMdDualIncome(MahalanobisDistance md) {
        mdDualIncome = md;
    }
    public static MahalanobisDistance getMdDualIncome() {
        return mdDualIncome;
    }
    public static void setMdChildcare(MahalanobisDistance md) {
        mdChildcare = md;
    }
    public static MahalanobisDistance getMdChildcare() {
        return mdChildcare;
    }
    public static void setMdDualIncomeChildcare(MahalanobisDistance md) {
        mdDualIncomeChildcare = md;
    }
    public static MahalanobisDistance getMdDualIncomeChildcare() {
        return mdDualIncomeChildcare;
    }
    public static double normaliseWeeklyIncome(int priceYear, double weeklyFinancial) {
        return normaliseMonthlyIncome(priceYear, weeklyFinancial * WEEKS_PER_MONTH);
    }
    public static double normaliseMonthlyIncome(int priceYear, double monthlyFinancial) {
        double infAdj = 1.0;
        if (priceYear != BASE_PRICE_YEAR)
            infAdj = getTimeSeriesValue(BASE_PRICE_YEAR, TimeSeriesVariable.Inflation) / getTimeSeriesValue(priceYear, TimeSeriesVariable.Inflation);
        return Parameters.asinh(monthlyFinancial * infAdj);
    }
    public static void setTrainingFlag(boolean flag) {
        trainingFlag = flag;
    }
    public static String getInputDirectoryInitialPopulations() {
        if (TESTING_FLAG)
            return INPUT_DIRECTORY_INITIAL_POPULATIONS;
        else
            return (trainingFlag) ? INPUT_DIRECTORY_INITIAL_POPULATIONS + "training"  + File.separator  : INPUT_DIRECTORY_INITIAL_POPULATIONS;
    }
    private static void setMapBounds(MapBounds map, String countryString) {

        String rgn = countryString + "C";
        boolean searchBack = true;
        boolean searchForward = true;
        boolean searchAge;
        searchAge = !map.equals(MapBounds.Fertility);
        int ii = 1;
        int maxYear=0, minYear=0, maxAge=0;
        while (searchBack || searchForward || searchAge) {

            if (searchForward) {

                Number val = switch (map) {
                    case UnemploymentMaleGraduates -> (Number) unemploymentRatesMaleGraduatesByAgeYear.getValue(25, MIN_START_YEAR + ii);
                    case UnemploymentMaleNonGraduates -> (Number) unemploymentRatesMaleNonGraduatesByAgeYear.getValue(25, MIN_START_YEAR + ii);
                    case UnemploymentFemaleGraduates -> (Number) unemploymentRatesFemaleGraduatesByAgeYear.getValue(25, MIN_START_YEAR + ii);
                    case UnemploymentFemaleNonGraduates -> (Number) unemploymentRatesFemaleNonGraduatesByAgeYear.getValue(25, MIN_START_YEAR + ii);
                    case Fertility -> (Number) fertilityProjectionsByYear.getValue("Value", MIN_START_YEAR + ii);
                    case Mortality -> (Number) mortalityProbabilityByGenderAgeYear.getValue("Female", 25, MIN_START_YEAR + ii);
                    case EquivalisedIncome -> (Number) equivalisedIncomeByGenderAgeYear.getValue("Female", 25, MIN_START_YEAR + ii);
                    default -> (Number) populationProjections.getValue("Female", rgn, 25, MIN_START_YEAR + ii);
                };
                if (val==null) {
                    maxYear = MIN_START_YEAR + ii - 1;
                    searchForward = false;
                }
            }
            if (searchBack) {

                Number val = switch (map) {
                    case UnemploymentMaleGraduates -> (Number) unemploymentRatesMaleGraduatesByAgeYear.getValue(25, MIN_START_YEAR - ii);
                    case UnemploymentMaleNonGraduates -> (Number) unemploymentRatesMaleNonGraduatesByAgeYear.getValue(25, MIN_START_YEAR - ii);
                    case UnemploymentFemaleGraduates -> (Number) unemploymentRatesFemaleGraduatesByAgeYear.getValue(25, MIN_START_YEAR - ii);
                    case UnemploymentFemaleNonGraduates -> (Number) unemploymentRatesFemaleNonGraduatesByAgeYear.getValue(25, MIN_START_YEAR - ii);
                    case Fertility -> (Number) fertilityProjectionsByYear.getValue("Value", MIN_START_YEAR - ii);
                    case Mortality -> (Number) mortalityProbabilityByGenderAgeYear.getValue("Female", 25, MIN_START_YEAR - ii);
                    case EquivalisedIncome -> (Number) equivalisedIncomeByGenderAgeYear.getValue("Female", 25, MIN_START_YEAR - ii);
                    default -> (Number) populationProjections.getValue("Female", rgn, 25, MIN_START_YEAR - ii);
                };
                if (val==null) {
                    minYear = MIN_START_YEAR - ii + 1;
                    searchBack = false;
                }
            }
            if (searchAge) {

                Number val = switch (map) {
                    case UnemploymentMaleGraduates -> (Number) unemploymentRatesMaleGraduatesByAgeYear.getValue(55+ii, MIN_START_YEAR);
                    case UnemploymentMaleNonGraduates -> (Number) unemploymentRatesMaleNonGraduatesByAgeYear.getValue(55+ii, MIN_START_YEAR);
                    case UnemploymentFemaleGraduates -> (Number) unemploymentRatesFemaleGraduatesByAgeYear.getValue(55+ii, MIN_START_YEAR);
                    case UnemploymentFemaleNonGraduates -> (Number) unemploymentRatesFemaleNonGraduatesByAgeYear.getValue(55+ii, MIN_START_YEAR);
                    case Mortality -> (Number) mortalityProbabilityByGenderAgeYear.getValue("Female", 55+ii, MIN_START_YEAR);
                    case EquivalisedIncome -> (Number) equivalisedIncomeByGenderAgeYear.getValue("Female", 55+ii, MIN_START_YEAR);
                    default -> (Number) populationProjections.getValue("Female", rgn, 55+ii, MIN_START_YEAR);
                };
                if (val==null) {
                    maxAge = 55 + ii - 1;
                    searchAge = false;
                }
            }
            ii++;
        }
        switch (map) {
            case UnemploymentMaleGraduates -> {
                unemploymentRatesMaleGraduatesMaxYear = maxYear;
                unemploymentRatesMaleGraduatesMinYear = minYear;
                unemploymentRatesMaleGraduatesMaxAge = maxAge;
            }
            case UnemploymentMaleNonGraduates -> {
                unemploymentRatesMaleNonGraduatesMaxYear = maxYear;
                unemploymentRatesMaleNonGraduatesMinYear = minYear;
                unemploymentRatesMaleNonGraduatesMaxAge = maxAge;
            }
            case UnemploymentFemaleGraduates -> {
                unemploymentRatesFemaleGraduatesMaxYear = maxYear;
                unemploymentRatesFemaleGraduatesMinYear = minYear;
                unemploymentRatesFemaleGraduatesMaxAge = maxAge;
            }
            case UnemploymentFemaleNonGraduates -> {
                unemploymentRatesFemaleNonGraduatesMaxYear = maxYear;
                unemploymentRatesFemaleNonGraduatesMinYear = minYear;
                unemploymentRatesFemaleNonGraduatesMaxAge = maxAge;
            }
            case Fertility -> {
                fertilityProjectionsMaxYear = maxYear;
                fertilityProjectionsMinYear = minYear;
            }
            case Mortality -> {
                mortalityProbabilityMaxYear = maxYear;
                mortalityProbabilityMinYear = minYear;
                mortalityProbabilityMaxAge = maxAge;
            }
            case EquivalisedIncome -> {
                equivalisedIncomeMaxYear = maxYear;
                equivalisedIncomeMinYear = minYear;
                equivalisedIncomeMaxAge = maxAge;
            }
            default -> {
                populationProjectionsMaxYear = maxYear;
                populationProjectionsMinYear = minYear;
                populationProjectionsMaxAge = maxAge;
            }
        }
    }

    public static int getStatePensionAge(int year, int age) {

        int spa;
        if (year - age + 65 < 2019) {
            spa = 65;
        } else if (year - age + 66 < 2027) {
            spa = 66;
        } else if (year - age + 67 < 2045) {
            spa = 67;
        } else {
            spa = 68;
        }
        return spa;
    }

    public static MultiKeyCoefficientMap getCoeffLabourSupplyUtilityMales() {
        return coeffLabourSupplyUtilityMales;
    }

    public static MultiKeyCoefficientMap getCoeffLabourSupplyUtilityACMales() {
        return coeffLabourSupplyUtilityACMales;
    }

    public static MultiKeyCoefficientMap getCoeffLabourSupplyUtilityACFemales() {
        return coeffLabourSupplyUtilityACFemales;
    }

    public static MultiKeyCoefficientMap getCoeffLabourSupplyUtilityFemales() {
        return coeffLabourSupplyUtilityFemales;
    }

    public static MultiKeyCoefficientMap getCoeffLabourSupplyUtilityCouples() {
        return coeffLabourSupplyUtilityCouples;
    }
    public static MultiKeyCoefficientMap getCoeffLabourSupplyUtilitySingleDep() {
        return coeffLabourSupplyUtilitySingleDep;
    }


    public static double getLiquidWealthDiscount() {
        return 0.0;
    }

    public static double getPensionWealthDiscount(int age) {
        int youngAgeCeiling = 45, midAgeFloor = 55, oldAgeFloor = 65;
        double discountYoung = 0.9, discountMid = 0.9, discountOld = 0.0;
        if (age <= youngAgeCeiling) {
            return discountYoung;
        } else if (age <= midAgeFloor) {
            return (discountYoung * (double)(midAgeFloor - age) + discountMid * (double)(age - youngAgeCeiling)) /
                    (double)(midAgeFloor - youngAgeCeiling);
        } else if (age < oldAgeFloor) {
            return (discountMid * (double)(oldAgeFloor-age) + discountOld * (double)(age - midAgeFloor)) /
                    (double)(oldAgeFloor - midAgeFloor);
        } else {
            return discountOld;
        }
    }

    public static double getHousingWealthDiscount(int age) {
        int youngAgeCeiling = 45, midAgeFloor = 55, oldAgeFloor = 65;
        double discountYoung = 0.9, discountMid = 0.9, discountOld = 0.0;
        if (age <= youngAgeCeiling) {
            return discountYoung;
        } else if (age <= midAgeFloor) {
            return (discountYoung * (double)(midAgeFloor - age) + discountMid * (double)(age - youngAgeCeiling)) /
                    (double)(midAgeFloor - youngAgeCeiling);
        } else if (age < oldAgeFloor) {
            return (discountMid * (double)(oldAgeFloor-age) + discountOld * (double)(age - midAgeFloor)) /
                    (double)(oldAgeFloor - midAgeFloor);
        } else {
            return discountOld;
        }
    }

    public static double updateProbability(double init, double threshold) {

        if (init<0.0 || init>1.0)
            throw new RuntimeException("call to update probability that is not strictly within range of 0 and 1");
        if (threshold<0.0 || threshold>1.0)
            throw new RuntimeException("call to update probability where threshold is not strictly within range of 0 and 1");

        return (init<threshold) ? init/threshold : (1.0-init)/(1.0-threshold);
    }
    public static double updateProbability(double init) {

        return (init<0.5) ? init/0.5 : (1-init) / 0.5;
    }

    public static double getAlignmentValue(int year, AlignmentVariable variableType) {
        switch (variableType) {
            case PartnershipAlignment -> {
                Double val = partnershipAlignAdjustment.get(year);
                if (!checkFinite(val))
                    throw new RuntimeException("value undefined for partnershipAlignAdjustment in year " + year);
                return val;
            }
            case FertilityAlignment -> {
                Double val = fertilityAlignAdjustment.get(year);
                if (!checkFinite(val))
                    throw new RuntimeException("value undefined for fertilityAlignAdjustment in year " + year);
                return val;
            }
            default -> {
                throw new RuntimeException("failed to identify alignment value type to get");
            }
        }
    }

    public static void setAlignmentValue(int year, double val, AlignmentVariable variableType) {
        switch (variableType) {
            case PartnershipAlignment -> {
                partnershipAlignAdjustment.put(year, val);
            }
            case FertilityAlignment -> {
                fertilityAlignAdjustment.put(year, val);
            }
            default -> {
                throw new RuntimeException("failed to identify alignment value type in set");
            }
        }
    }

    public static double getFertilityRateByYear(int year) {
        Double val = fertilityRateByYear.get(year);
        if (!checkFinite(val))
            throw new RuntimeException("value undefined for getFertilityRateByYear in year " + year);
        return val;
    }

    public static void databaseSetup(Country country, boolean executeWithGui, int startYear) {

        // remove database file if it exists
        String filePath = getInputDirectory() + "input.mv.db";
        safeDelete(filePath);

        // Detect if data available; set to testing data if not
        Collection<File> testList = FileUtils.listFiles(new File(Parameters.getInputDirectoryInitialPopulations()), new String[]{"csv"}, false);
        if (testList.isEmpty())
            Parameters.setTrainingFlag(true);

        // populate new database for starting data
        try {
            DataParser.databaseFromCSV(country, executeWithGui); // Initial database tables
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error populating initial database from CSV files: " + e.getMessage());
        }

        // populate new database for tax donors
        String taxDonorInputFilename = "tax_donor_population_" + country;
        Parameters.setTaxDonorInputFileName(taxDonorInputFilename);
        Parameters.loadTimeSeriesFactorForTaxDonor(country);
        TaxDonorDataParser.constructAggregateTaxDonorPopulationCSVfile(country, executeWithGui);
        TaxDonorDataParser.databaseFromCSV(country, startYear, executeWithGui); // Donor database tables from csv data
        TaxDonorDataParser.populateDonorTaxUnitTables(country, executeWithGui); // Populate tax unit donor tables from person data
    }

    private static void safeDelete(String filePath) {
        File file = new File(filePath);
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void loadEQ5DParameters(String countryString) {

        coeffCovarianceEQ5D = ExcelAssistant.loadCoefficientMap(getInputDirectory() + "reg_eq5d.xlsx", countryString + "_EQ5D_" + eq5dConversionParameters, 1);
        regHealthEQ5D = new LinearRegression(coeffCovarianceEQ5D);
    }
    public static void setWorkingDirectory(String workingDirectory) {
        WORKING_DIRECTORY = workingDirectory;
        INPUT_DIRECTORY = WORKING_DIRECTORY + File.separator + "input" + File.separator;
        INPUT_DIRECTORY_INITIAL_POPULATIONS = INPUT_DIRECTORY + "InitialPopulations" + File.separator; //Path to directory containing initial population for each year
        EUROMOD_OUTPUT_DIRECTORY = INPUT_DIRECTORY + "EUROMODoutput" + File.separator;
        EUROMOD_TRAINING_DIRECTORY = EUROMOD_OUTPUT_DIRECTORY + "training" + File.separator;
    }

    public static void setInputDirectory(String inputDirectory) {
        File inputpath = new File(inputDirectory);
        if (inputpath.isAbsolute()){
            INPUT_DIRECTORY = inputDirectory + File.separator;
        } else {
            INPUT_DIRECTORY = WORKING_DIRECTORY + File.separator + inputDirectory + File.separator;
        }
        INPUT_DIRECTORY_INITIAL_POPULATIONS = INPUT_DIRECTORY + "InitialPopulations" + File.separator; //Path to directory containing initial population for each year
        EUROMOD_OUTPUT_DIRECTORY = INPUT_DIRECTORY + "EUROMODoutput" + File.separator;
        EUROMOD_TRAINING_DIRECTORY = EUROMOD_OUTPUT_DIRECTORY + "training" + File.separator;
    }
    public static void setInputDirectoryInitialPopulations(String inputDirectoryInitialPopulations) {
        File inputpath = new File(inputDirectoryInitialPopulations);
        if (inputpath.isAbsolute()){
            INPUT_DIRECTORY_INITIAL_POPULATIONS = inputDirectoryInitialPopulations + File.separator;
        } else {
            INPUT_DIRECTORY_INITIAL_POPULATIONS = WORKING_DIRECTORY + File.separator + inputDirectoryInitialPopulations + File.separator;
        }
    }

    public static void setEuromodOutputDirectory(String euromodOutputDirectory) {
        File inputPath = new File(euromodOutputDirectory);
        if (inputPath.isAbsolute()){
            EUROMOD_OUTPUT_DIRECTORY = euromodOutputDirectory + File.separator;
        } else {
            EUROMOD_OUTPUT_DIRECTORY = WORKING_DIRECTORY + File.separator + euromodOutputDirectory + File.separator;
        }
        EUROMOD_TRAINING_DIRECTORY = EUROMOD_OUTPUT_DIRECTORY + "training" + File.separator;
    }

    public static String getInputDirectory() {
        return INPUT_DIRECTORY;
    }

    public static boolean checkFinite(Double dd) {
        if (dd==null)
            return false;
        return !dd.isInfinite() && !dd.isNaN();
    }

    private static MultiKeyCoefficientMap bootstrapWithTrace(String mapName, MultiKeyCoefficientMap map) {
        try {
            appendBootstrapTrace("START " + mapName);
            return RegressionUtils.bootstrap(map);
        } catch (RuntimeException e) {
            appendBootstrapTrace("FAIL  " + mapName + " :: " + e.getMessage());
            System.err.println("Bootstrap failed for map: " + mapName);
            throw new RuntimeException("Bootstrap failed for map: " + mapName + ". Cause: " + e.getMessage());
        }
    }

    private static File bootstrapTraceFile() {
        return new File(System.getProperty("java.io.tmpdir"), "simpaths_bootstrap_trace.log");
    }

    private static void resetBootstrapTrace() {
        try {
            FileUtils.writeStringToFile(bootstrapTraceFile(), "", StandardCharsets.UTF_8, false);
        } catch (IOException ignored) {
            // tracing is best-effort only
        }
    }

    private static void appendBootstrapTrace(String line) {
        try {
            FileUtils.writeStringToFile(bootstrapTraceFile(), line + System.lineSeparator(), StandardCharsets.UTF_8, true);
        } catch (IOException ignored) {
            // tracing is best-effort only
        }
    }

    private static void validateCoefficientMapsForBootstrap(Object[][] namedMaps) {
        if (namedMaps == null) {
            return;
        }
        for (Object[] entry : namedMaps) {
            if (entry == null || entry.length != 2) {
                continue;
            }
            validateCoefficientMapForBootstrap((String) entry[0], (MultiKeyCoefficientMap) entry[1]);
        }
    }

    private static void validateCoefficientMapForBootstrap(String name, MultiKeyCoefficientMap map) {
        if (map == null) {
            System.out.println("Bootstrap validation: map is null: " + name);
            return;
        }
        String[] keyNames = map.getKeysNames();
        if (keyNames == null || keyNames.length == 0
                || !RegressionColumnNames.REGRESSOR.toString().equals(keyNames[0])) {
            System.out.println("Bootstrap validation: unexpected key names for " + name + ": " + Arrays.toString(keyNames));
        }
        String[] valueNames = map.getValuesNames();
        if (valueNames == null || valueNames.length == 0) {
            System.out.println("Bootstrap validation: missing value names for " + name);
            return;
        }
        for (int i = 0; i < valueNames.length; i++) {
            String valueName = valueNames[i];
            if (valueName == null || valueName.trim().isEmpty()) {
                System.out.println("Bootstrap validation: blank value name for " + name + " at index " + i
                        + " valueNames=" + Arrays.toString(valueNames));
            }
        }
        int coeffIndex = -1;
        Map<String, Integer> covariateIndex = new HashMap<>();
        for (int i = 0; i < valueNames.length; i++) {
            String valueName = valueNames[i];
            if (RegressionColumnNames.COEFFICIENT.toString().equals(valueName)) {
                coeffIndex = i;
            } else {
                covariateIndex.put(valueName, i);
            }
        }
        if (coeffIndex == -1) {
            System.out.println("Bootstrap validation: missing COEFFICIENT column for " + name);
        }
        int issueCount = 0;
        MapIterator<Object, Object> it = map.mapIterator();
        while (it.hasNext()) {
            it.next();
            MultiKey key = (MultiKey) it.getKey();
            Object rowObj = map.getValue(new Object[]{key});
            Object[] rowValues;
            if (rowObj instanceof Object[]) {
                rowValues = (Object[]) rowObj;
            } else if (rowObj != null) {
                rowValues = new Object[]{rowObj};
            } else {
                rowValues = null;
            }
            String regressor = String.valueOf(key.getKey(0));
            if (rowValues == null) {
                System.out.println("Bootstrap validation: null row for " + name + " regressor=" + regressor);
                issueCount++;
                if (issueCount >= 20) {
                    break;
                }
                continue;
            }
            if (coeffIndex >= 0) {
                if (coeffIndex >= rowValues.length || rowValues[coeffIndex] == null) {
                    System.out.println("Bootstrap validation: missing coefficient for " + name + " regressor=" + regressor);
                    issueCount++;
                }
            }
            for (Map.Entry<String, Integer> entry : covariateIndex.entrySet()) {
                Integer idx = entry.getValue();
                if (idx == null || idx >= rowValues.length || rowValues[idx] == null) {
                    System.out.println("Bootstrap validation: missing covariance for " + name
                            + " regressor=" + regressor + " covariate=" + entry.getKey());
                    issueCount++;
                    if (issueCount >= 20) {
                        break;
                    }
                }
            }
            if (issueCount >= 20) {
                System.out.println("Bootstrap validation: stopping after 20 issues for " + name);
                break;
            }
        }
    }
}
