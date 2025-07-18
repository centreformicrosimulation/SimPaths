// define package
package simpaths.data;

// import Java packages

import microsim.data.MultiKeyCoefficientMap;
import microsim.data.excel.ExcelAssistant;
import microsim.statistics.regression.*;
// import plug-in packages
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
import simpaths.data.startingpop.DataParser;
import simpaths.model.AnnuityRates;
import simpaths.model.decisions.Grids;
import simpaths.model.enums.*;
import simpaths.model.taxes.DonorTaxUnit;
import simpaths.model.taxes.MatchFeature;
import simpaths.model.taxes.database.TaxDonorDataParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static microsim.statistics.regression.RegressionUtils.appendCoefficientMaps;


/**
 *
 * CLASS TO STORE MODEL PARAMETERS FOR GLOBAL ACCESS
 *
 */
public class Parameters {

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
		"ddi",					//disability status
        "yem", 					//employment income - used to construct work sector *NOT VALID FOR POLICY ANALYSIS*
        "yse", 					//self-employment income - used to construct work sector *NOT VALID FOR POLICY ANALYSIS*
    };

    public static final String[] DONOR_POLICY_VARIABLES = new String[] {
        "xcc",                  //childcare costs
        "ils_earns",			//EUROMOD output variable:- total labour earnings (employment + self-employment income + potentially other labour earnings like temporary employment, depending on country classification)
        "ils_origy",			//EUROMOD output variable:- all gross income from labour, private pensions, investment income, property income, private transfers etc.
		"ils_dispy",			//Disposable income : from EUROMOD output data after tax / benefit transfers (monthly time-scale)
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
		"idhh",				//id of household (can contain multiple benefit units)
    };

    public static final String[] BENEFIT_UNIT_VARIABLES_INITIAL = new String[] {
		"idhh",				//id of household (can contain multiple benefit units)
		"idbenefitunit",	//id of a benefit unit
		"drgn1", 			//region (NUTS1)
		"ydses_c5",			//household income quantile
		"dhh_owned",		//flag indicating if benefit unit owns a house
		"liquid_wealth",	//benefit unit net wealth non-pension non-housing wealth
        "tot_pen",	        //benefit unit net pension wealth
        "nvmhome",	        //benefit unit net housing wealth
    };

    public static final String[] PERSON_VARIABLES_INITIAL = new String[] {
		"idhh",					//id of household (can contain multiple benefit units)
		"idbenefitunit",		//id of a benefit unit
		"idperson", 			//id of person
		"dwt", 					//household weight
		"idfather", 			//id of father
		"idmother", 			//id of mother
		"dag", 					//age
		"deh_c3", 				//highest education level
		"dehm_c3",				//highest education level of mother
		"dehf_c3",				//highest education level of father
		"ded",					//in education dummy
		"der",					//return to education dummy
		"dot",					//ethnicity
		"dhe",					//health status
		"dhm",					//mental health status
		"scghq2_dv",			//mental health status case based
		"dhm_ghq",				//mental health status case based dummy (1 = psychologically distressed)
        "dls",                  //life satisfaction
        "dhe_mcs",              //mental health - SF12 score MCS
        "dhe_pcs",              //physical health - SF12 score PCS
        "financial_distress",	//financial distress
		"dcpyy",				//years in partnership
		"dcpagdf",				//partners age difference
		"dnc02",				//number children aged 0-2
		"dnc",					//number children
		"ypnbihs_dv",			//gross personal non-benefit income
		"yptciihs_dv",			//gross personal non-employment non-benefit income
		"ypncp",  				//gross personal capital income
		"ypnoab",				//gross personal pension (public / occupational) income
		"yplgrs_dv",			//gross personal employment income
		"ynbcpdf_dv",			//difference partner income
		"dlltsd",				//long-term sick or disabled
		"sedex",				//year left education
		"stm",					//system variable - year
		"swv",					//system variable - wave
		"dgn", 					//gender
		"les_c4", 				//labour employment status
		"lhw", 					//hours worked per week
        "l1_lhw",               //hours worked per week in the previous year
		"adultchildflag",		//flag indicating adult child living at home in the data
		"dhh_owned",			//flag indicating if individual is a homeowner
		"potential_earnings_hourly", //initial value of hourly earnings from the data
		"l1_potential_earnings_hourly", //lag(1) of initial value of hourly earnings from the data
        "need_socare",          //indicator that the individual needs social care
        "formal_socare_hrs",    //number of hours of formal care received
        "formal_socare_cost",   //cost of formal care received
        "partner_socare_hrs",   //number of hours of informal care received from partner
        "daughter_socare_hrs",  //number of hours of informal care received from daughter
        "son_socare_hrs",       //number of hours of informal care received from son
        "other_socare_hrs",     //number of hours of informal care received from other
        "aidhrs",               //number of hours of informal care provided (total)
        "careWho",              //indicator for whom informal care is provided
        "econ_benefits",        //indicator of benefit receipt
        "econ_benefits_uc",     //indicator of UC receipt
        "econ_benefits_nonuc"   //indicator of other benefit receipt
		//"yem", 					//employment income
		//"yse", 					//self-employment income

		//From EUROMOD output data before tax / benefit transfers, so not affected by EUROMOD policy scenario (monthly time-scale).  We just use them calculated from EUROMOD output because EUROMOD has the correct way of aggregating each country's different component definitions
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
    public static final double UNMATCHED_TOLERANCE_THRESHOLD = 0.1;		//Smallest proportion of a gender left unmatched (we take the minimum of the male proportion and female proportions).  If there are more than this, we will relax the constraints (e.g. the bounds on age difference and potential earnings difference) until this target has been reached
    public static final int MAXIMUM_ATTEMPTS_MATCHING = 10;
    public static final double RELAXATION_FACTOR = 1.5;

    public static final int AGE_DIFFERENCE_INITIAL_BOUND = 999;
    public static final double POTENTIAL_EARNINGS_DIFFERENCE_INITIAL_BOUND = 999.;

    public static final double WEEKS_PER_MONTH = 365.25/(7.*12.);	// = 4.348214286
    public static final double WEEKS_PER_YEAR = 365.25 / 7.;

    public static final int HOURS_IN_WEEK = 24 * 7; //This is used to calculate leisure in labour supply
    //Is it possible for people to start going to the labour module (e.g. age 17) while they are living with parents (until age 18)?
    //Cannot see how its possible if it is the household that decides how much labour to supply.  If someone finishes school at 17, they need to leave home before they can enter the labour market.  So set age for finishing school and leaving home to 18.
    public static final int MAX_LABOUR_HOURS_IN_WEEK = 48;
    public static final boolean USE_CONTINUOUS_LABOUR_SUPPLY_HOURS = true; // If true, a random number of hours of weekly labour supply within each bracket will be generated. Otherwise, each discrete choice of labour supply corresponds to a fixed number of hours of labour supply, which is the same for all persons
    public static int maxAge;										// maximum age possible in simulation
    public static final int AGE_TO_BECOME_RESPONSIBLE = 18;			// Age become reference person of own benefit unit
    public static final int MIN_AGE_TO_LEAVE_EDUCATION = 16;		// Minimum age for a person to leave (full-time) education
    public static final int MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION = 29;
    public static final int MAX_AGE_TO_ENTER_EDUCATION = 45;
    public static final int MIN_AGE_COHABITATION = AGE_TO_BECOME_RESPONSIBLE;  	// Min age a person can marry
    public static final int MIN_AGE_TO_HAVE_INCOME = 16; //Minimum age to have non-employment non-benefit income
    public static final int MIN_AGE_TO_RETIRE = 50; //Minimum age to consider retirement
    public static final int DEFAULT_AGE_TO_RETIRE = 67; //if pension included, but retirement decision not
    public static final int MIN_AGE_FORMAL_SOCARE = 65; //Minimum age to receive formal social care
    public static final int MIN_AGE_FLEXIBLE_LABOUR_SUPPLY = 16; //Used when filtering people who can be "flexible in labour supply"
    public static final int MAX_AGE_FLEXIBLE_LABOUR_SUPPLY = 75;
    public static final double SHARE_OF_WEALTH_TO_ANNUITISE_AT_RETIREMENT = 0.25;
    public static final double ANNUITY_RATE_OF_RETURN = 0.015;
    public static AnnuityRates annuityRates;
    public static final int MIN_HOURS_FULL_TIME_EMPLOYED = 25;	// used to distinguish full-time from part-time employment (needs to be consistent with Labour enum)
    public static final double MIN_HOURLY_WAGE_RATE = 1.5;
    public static final double MAX_HOURLY_WAGE_RATE = 150.0;
    public static final double MAX_HOURS_WEEKLY_FORMAL_CARE = 150.0;
    public static final double MAX_HOURS_WEEKLY_INFORMAL_CARE = 16 * 7;
    public static final double CHILDCARE_COST_EARNINGS_CAP = 0.5;  // maximum share of earnings payable as childcare (for benefit units with some earnings)
    public static final int MIN_DIFFERENCE_AGE_MOTHER_CHILD_IN_ALIGNMENT = 15; //When assigning children to mothers in the population alignment, specify how much older (at the minimum) the mother must be than the child
    public static final int MAX_EM_DONOR_RATIO = 3; // Used by BenefitUnit => convertGrossToDisposable() to decide whether gross-to-net ratio should be applied or disposable income from the donor used directly
    public static final double PERCENTAGE_OF_MEDIAN_EM_DONOR = 0.2; // Used by BenefitUnit => convertGrossToDisposable() to decide whether gross-to-net ratio should be applied or disposable income from the donor used directly
    public static final double PSYCHOLOGICAL_DISTRESS_GHQ12_CASES_CUTOFF = 4; // Define cut-off on the GHQ12 Likert scale above which individuals are classified as psychologically distressed

    //Initial value for the savings rate and multiplier for capital income:
    public static double SAVINGS_RATE; //This is set in the country-specific part of this file

    //public static int MAX_AGE_IN_EDUCATION;// = MAX_AGE;//30;			// Max age a person can stay in education	//Cannot set here, as MAX_AGE is not known yet.  Now set to MAX_AGE in buildObjects in Model class.
    //public static int MAX_AGE_MARRIAGE;// = MAX_AGE;//75;  			// Max age a person can marry		//Cannot set here, as MAX_AGE is not known yet.  Now set to MAX_AGE in buildObjects in Model class.
    private static int MIN_START_YEAR = 2011; //Minimum allowed starting point. Should correspond to the oldest initial population.
    private static int MAX_START_YEAR = 2021; //Maximum allowed starting point. Should correspond to the most recent initial population.
    public static int startYear;
    public static int endYear;
    private static final int MIN_START_YEAR_TESTING = 2019;
    private static final int MAX_START_YEAR_TESTING = 2019; //Maximum allowed starting point. Should correspond to the most recent initial population.
    private static final int MIN_START_YEAR_TRAINING = 2019;
    private static final int MAX_START_YEAR_TRAINING = 2019; //Maximum allowed starting point. Should correspond to the most recent initial population.
    public static final int MIN_AGE_MATERNITY = 18;  			// Min age a person can give birth
    public static final int MAX_AGE_MATERNITY = 44;  			// Max age a person can give birth
    public static final boolean FLAG_SINGLE_MOTHERS = true;
    public static boolean flagUnemployment = false;

    public static int BASE_PRICE_YEAR = 2015; 			// Base price year of model parameters

    public static double PROB_NEWBORN_IS_MALE = 0.5;            // Must be strictly greater than 0.0 and less than 1.0

    public static boolean UC_ROLLOUT = true;              // Whether UC is available in population or not

    public static final boolean systemOut = true;

    //Bootstrap all the regression coefficients if true, or only the female labour participation regressions when false
    public static final boolean bootstrapAll = false;

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
    private static MultiKeyCoefficientMap upratingIndexMapRealGDP, upratingIndexMapInflation, socialCareProvisionTimeAdjustment,
            partnershipTimeAdjustment, fertilityTimeAdjustment, utilityTimeAdjustmentSingleMales, utilityTimeAdjustmentSingleFemales,
            utilityTimeAdjustmentCouples, upratingIndexMapRealWageGrowth, priceMapRealSavingReturns, priceMapRealDebtCostLow, priceMapRealDebtCostHigh,
            wageRateFormalSocialCare, socialCarePolicy, partneredShare, employedShareSingleMales, employedShareSingleFemales, employedShareCouples;
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
    private static MultiKeyCoefficientMap coeffCovarianceHealthH1a;
    private static MultiKeyCoefficientMap coeffCovarianceHealthH1b;
    private static MultiKeyCoefficientMap coeffCovarianceHealthH2b; //Prob. long-term sick or disabled

    //Social care
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS1a; // prob of needing social care under 65
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS1b;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2a; // prob of needing social care 65+
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2b;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2c;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2d;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2e;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2f;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2g;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2h;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2i;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2j;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS2k;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS3a;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS3b;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS3c;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS3d;
    private static MultiKeyCoefficientMap coeffCovarianceSocialCareS3e;

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
    private static MultiKeyCoefficientMap coeffCovarianceEducationE2a;

    //Partnership
    private static MultiKeyCoefficientMap coeffCovariancePartnershipU1a; //Probit enter partnership if in continuous education
    private static MultiKeyCoefficientMap coeffCovariancePartnershipU1b; //Probit enter partnership if not in continuous education
    private static MultiKeyCoefficientMap coeffCovariancePartnershipU2b; //Probit exit partnership (females)

    //Partnership for Italy
    private static MultiKeyCoefficientMap coeffCovariancePartnershipITU1; //Probit enter partnership for Italy
    private static MultiKeyCoefficientMap coeffCovariancePartnershipITU2; //Probit exit partnership for Italy

    //Fertility
    private static MultiKeyCoefficientMap coeffCovarianceFertilityF1a; //Probit fertility if in continuous education
    private static MultiKeyCoefficientMap coeffCovarianceFertilityF1b; //Probit fertility if not in continuous education

    //Fertility for Italy
    private static MultiKeyCoefficientMap coeffCovarianceFertilityF1; //Probit fertility for Italy

    //Income
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI1a; //Linear regression non-employment non-benefit income if in continuous education
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI1b; //Linear regression non-employment non-benefit income if not in continuous education
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI3a; //Capital income if in continuous education
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI3b; //Capital income if not in continuous education
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI3c; //Pension income for those aged over 50 who are not in continuous education
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI4a, coeffCovarianceIncomeI4b; // Pension income for those moving from employment to retirement (I4a) and those already retired (I4b)
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI5a_selection, coeffCovarianceIncomeI5b_amount; // Selection equation for receiving pension income for those moving from employment to retirement (I5a) and amount in levels (I5b)
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI6a_selection, coeffCovarianceIncomeI6b_amount; // Selection equation for receiving pension income for those in retirement (I6a) and amount in levels (I6b), in the initial simulated year
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI3a_selection; //Probability of receiving capital income if in continuous education
    private static MultiKeyCoefficientMap coeffCovarianceIncomeI3b_selection; //Probability of receiving capital income if not in continuous education

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
    private static MultiKeyCoefficientMap coeffLabourSupplyUtilityMalesWithDependent; //For use with couples where only male is flexible in labour supply (so has a dependent)
    private static MultiKeyCoefficientMap coeffLabourSupplyUtilityFemalesWithDependent;
    private static MultiKeyCoefficientMap coeffLabourSupplyUtilityACMales; //Adult children, male
    private static MultiKeyCoefficientMap coeffLabourSupplyUtilityACFemales; //Adult children, female
    private static MultiKeyCoefficientMap coeffLabourSupplyUtilityCouples;

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
    private static  MultiKeyCoefficientMap coeffCovarianceLeaveHomeP1a;

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

    //Share of students by region
    private static MultiKeyCoefficientMap validationStudentsByRegion;

    //Education level of over 17 year olds
    private static MultiKeyCoefficientMap validationEducationLevel;

    //Education level by age group
    private static MultiKeyCoefficientMap validationEducationLevelByAge;

    //Education level by region
    private static MultiKeyCoefficientMap validationEducationLevelByRegion;

    //Share of couple by region
    private static MultiKeyCoefficientMap validationPartneredShareByRegion;

    //Share of disabled by age
    private static MultiKeyCoefficientMap validationDisabledByAge;

    private static MultiKeyCoefficientMap validationDisabledByGender;

    //Health by age
    private static MultiKeyCoefficientMap validationHealthByAge;

    //Mental health by age and gender
    private static MultiKeyCoefficientMap validationMentalHealthByAge;

    //Psychological distress cases by age and gender
    private static MultiKeyCoefficientMap validationPsychDistressByAge, validationPsychDistressByAgeLow, validationPsychDistressByAgeMed, validationPsychDistressByAgeHigh;


    // Health
    private static MultiKeyCoefficientMap validationHealthMCSByAge, validationHealthPCSByAge;

    // Life Satisfaction
    private static MultiKeyCoefficientMap validationLifeSatisfactionByAge;

    //Employment by gender
    private static MultiKeyCoefficientMap validationEmploymentByGender;

    //Employment by gender and age
    private static MultiKeyCoefficientMap validationEmploymentByAgeAndGender;

    //Employment by maternity
    private static MultiKeyCoefficientMap validationEmploymentByMaternity;

    //Employment by gender and region
    private static MultiKeyCoefficientMap validationEmploymentByGenderAndRegion;

    private static MultiKeyCoefficientMap validationLabourSupplyByEducation;

    //Activity status
    private static MultiKeyCoefficientMap validationActivityStatus;

    //Homeownership status for benefit units
    private static MultiKeyCoefficientMap validationHomeownershipBenefitUnits;

    //Gross earnings yearly by education and gender (for employed persons)
    private static MultiKeyCoefficientMap validationGrossEarningsByGenderAndEducation;

    //Hourly wages by education and gender (for employed persons)
    private static MultiKeyCoefficientMap validationLhwByGenderAndEducation;

    //Hours worked weekly by education and gender (for employed persons)
    private static MultiKeyCoefficientMap hourlyWageByGenderAndEducation;

    /////////////////////////////////////////////////////////////////// REGRESSION OBJECTS //////////////////////////////////////////

    //Health
    private static OrderedRegression regHealthH1a;
    private static OrderedRegression regHealthH1b;
    private static BinomialRegression regHealthH2b;

    //Social care
    private static BinomialRegression regReceiveCareS1a;
    private static LinearRegression regCareHoursS1b;
    private static BinomialRegression regNeedCareS2a;
    private static BinomialRegression regReceiveCareS2b;
    private static MultinomialRegression regSocialCareMarketS2c;
    private static BinomialRegression regReceiveCarePartnerS2d;
    private static MultinomialRegression regPartnerSupplementaryCareS2e;
    private static MultinomialRegression regNotPartnerInformalCareS2f;
    private static LinearRegression regPartnerCareHoursS2g;
    private static LinearRegression regDaughterCareHoursS2h;
    private static LinearRegression regSonCareHoursS2i;
    private static LinearRegression regOtherCareHoursS2j;
    private static LinearRegression regFormalCareHoursS2k;
    private static BinomialRegression regCarePartnerProvCareToOtherS3a;
    private static BinomialRegression regNoCarePartnerProvCareToOtherS3b;
    private static BinomialRegression regNoPartnerProvCareToOtherS3c;
    private static MultinomialRegression regInformalCareToS3d;
    private static LinearRegression regCareHoursProvS3e;

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

    private static BinomialRegression regHealthHM1Case;
    private static BinomialRegression regHealthHM2CaseMales;
    private static BinomialRegression regHealthHM2CaseFemales;

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
    private static OrderedRegression regEducationE2a;

    //Partnership
    private static BinomialRegression regPartnershipU1a;
    private static BinomialRegression regPartnershipU1b;
    private static BinomialRegression regPartnershipU2b;

    private static BinomialRegression regPartnershipITU1;
    private static BinomialRegression regPartnershipITU2;

    //Fertility
    private static BinomialRegression regFertilityF1a;
    private static BinomialRegression regFertilityF1b;
    private static BinomialRegression regFertilityF1;

    //Income
    private static LinearRegression regIncomeI1a;
    private static LinearRegression regIncomeI1b;
    private static LinearRegression regIncomeI3a;
    private static LinearRegression regIncomeI3b;
    private static LinearRegression regIncomeI3c;
    private static LinearRegression regIncomeI4a;
    private static LinearRegression regIncomeI4b;
    private static LinearRegression regIncomeI5b_amount;
    private static LinearRegression regIncomeI6b_amount;
    private static BinomialRegression regIncomeI3a_selection;
    private static BinomialRegression regIncomeI3b_selection;
    private static BinomialRegression regIncomeI5a_selection;
    private static BinomialRegression regIncomeI6a_selection;

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

    private static LinearRegression regLabourSupplyUtilityMalesWithDependent;
    private static LinearRegression regLabourSupplyUtilityFemalesWithDependent;

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

    public static double realInterestRateInnov;
    public static double disposableIncomeFromLabourInnov;


    /**
     *
     * METHOD TO LOAD PARAMETERS FOR GIVEN COUNTRY
     * @param country
     *
     */
    public static void loadParameters(Country country, int maxAgeModel, boolean enableIntertemporalOptimisations,
                                      boolean projectFormalChildcare, boolean projectSocialCare, boolean donorPoolAveraging1,
                                      boolean fixTimeTrend, boolean defaultToTimeSeriesAverages, boolean taxDBMatches,
                                      Integer timeTrendStops, int startYearModel, int endYearModel, double interestRateInnov1,
                                      double disposableIncomeFromLabourInnov1, boolean flagSuppressChildcareCosts1,
                                      boolean flagSuppressSocialCareCosts1) {

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

//		unemploymentRatesByRegion = new LinkedHashMap<>();
//		unemploymentRates = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "scenario_unemploymentRates.xlsx", countryString, 1, 46);
        fixedRetireAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "scenario_retirementAgeFixed.xlsx", countryString, 1, 2);
        /*
        rawProbSick = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "scenario_probSick.xls", country.toString(), 2, 1);
        for (Object o: rawProbSick.keySet()) {
            MultiKey mk = (MultiKey)o;
            int age = ((Number)mk.getKey(1)).intValue();
            if (((String)mk.getKey(0)).equals(Gender.Female.toString())) {
                if (age > femaleMaxAgeSick) {
                    femaleMaxAgeSick = age;
//				} else if(age < femaleMinAgeSick) {
//					femaleMinAgeSick = age;
                }
            } else {
                // gender in multikey must be male
                if (age > maleMaxAgeSick) {
                    maleMaxAgeSick = age;
//				} else if(age < maleMinAgeSick) {
//					maleMinAgeSick = age;
                }
            }
        }

        probSick = new MultiKeyMap();
        */

        // alignment parameters
        populationProjections = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "align_popProjections.xlsx", countryString, 3, 110);
        setMapBounds(MapBounds.Population, countryString);

        //Alignment of education levels
        projectionsHighEdu = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "align_educLevel.xlsx", countryString + "_High", 1, 2);
        projectionsLowEdu = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "align_educLevel.xlsx", countryString + "_Low", 1, 2);

        studentShareProjections = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "align_student_under30.xlsx", countryString, 1, 40);

        //Employment alignment
        employmentAlignment = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "align_employment.xlsx", countryString, 2, 40);

        //Marriage types frequencies:
        marriageTypesFrequency = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "marriageTypes2.xlsx", countryString, 2, 1);
        marriageTypesFrequencyByGenderAndRegion = new LinkedHashMap<Gender, MultiKeyMap<Region, Double>>();	//Create a map of maps to store the frequencies

        //Mortality rates
        mortalityProbabilityByGenderAgeYear = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "projections_mortality.xlsx", countryString + "_MortalityByGenderAgeYear", 2, 120);
        setMapBounds(MapBounds.Mortality, countryString);

        //Fertility rates:
        fertilityProjectionsByYear = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "projections_fertility.xlsx", countryString + "_FertilityByYear", 1, 71);
        setMapBounds(MapBounds.Fertility, countryString);

        //Unemployment rates
        unemploymentRatesMaleGraduatesByAgeYear = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", countryString + "_RatesMaleGraduates", 1, 49);
        setMapBounds(MapBounds.UnemploymentMaleGraduates, countryString);
        unemploymentRatesMaleNonGraduatesByAgeYear = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", countryString + "_RatesMaleNonGraduates", 1, 49);
        setMapBounds(MapBounds.UnemploymentMaleNonGraduates, countryString);
        unemploymentRatesFemaleGraduatesByAgeYear = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", countryString + "_RatesFemaleGraduates", 1, 49);
        setMapBounds(MapBounds.UnemploymentFemaleGraduates, countryString);
        unemploymentRatesFemaleNonGraduatesByAgeYear = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", countryString + "_RatesFemaleNonGraduates", 1, 49);
        setMapBounds(MapBounds.UnemploymentFemaleNonGraduates, countryString);

        //RMSE
        coefficientMapRMSE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_RMSE.xlsx", countryString, 1, 1);

        //Employments on furlough
        employmentsFurloughedFull = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "scenario_employments_furloughed.xlsx", countryString + "_FullFurlough", 2, 1);
        employmentsFurloughedFlex = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "scenario_employments_furloughed.xlsx", countryString + "_FlexibleFurlough", 2, 1);

        //Load country specific data
        int columnsWagesMales = -1;
        int columnsWagesMalesNE = -1;
        int columnsWagesMalesE = -1;
        int columnsWagesFemales = -1;
        int columnsWagesFemalesNE = -1;
        int columnsWagesFemalesE = -1;
        int columnsEmploymentSelectionMales = -1;
        int columnsEmploymentSelectionMalesNE = -1;
        int columnsEmploymentSelectionMalesE = -1;
        int columnsEmploymentSelectionFemales = -1;
        int columnsEmploymentSelectionFemalesNE = -1;
        int columnsEmploymentSelectionFemalesE = -1;
        int columnsLabourSupplyUtilityMales = -1;
        int columnsLabourSupplyUtilityFemales = -1;
        int columnsLabourSupplyUtilityMalesWithDependent = -1;
        int columnsLabourSupplyUtilityFemalesWithDependent = -1;
        int columnsLabourSupplyUtilityACMales = -1;
        int columnsLabourSupplyUtilityACFemales = -1;
        int columnsLabourSupplyUtilityCouples = -1;
        int columnsLabourCovid19_SE = -1;
        int columnsLabourCovid19_2a_processes = -1;
        int columnsHealthH1a = -1;
        int columnsHealthH1b = -1;
        int columnsHealthH2b = -1;
        int columnsHealthHM1 = -1;
        int columnsHealthHM2Males = -1;
        int columnsHealthHM2Females = -1;
        int columnsHealthMCS1 = -1;
        int columnsHealthMCS2Males = -1;
        int columnsHealthMCS2Females = -1;
        int columnsHealthPCS1 = -1;
        int columnsHealthPCS2Males = -1;
        int columnsHealthPCS2Females = -1;
        int columnsLifeSatisfaction1 = -1;
        int columnsLifeSatisfaction2Males = -1;
        int columnsLifeSatisfaction2Females = -1;
        int columnsHealthEQ5D = -1;
        int columnsSocialCareS1a = -1;
        int columnsSocialCareS1b = -1;
        int columnsSocialCareS2a = -1;
        int columnsSocialCareS2b = -1;
        int columnsSocialCareS2c = -1;
        int columnsSocialCareS2d = -1;
        int columnsSocialCareS2e = -1;
        int columnsSocialCareS2f = -1;
        int columnsSocialCareS2g = -1;
        int columnsSocialCareS2h = -1;
        int columnsSocialCareS2i = -1;
        int columnsSocialCareS2j = -1;
        int columnsSocialCareS2k = -1;
        int columnsSocialCareS3a = -1;
        int columnsSocialCareS3b = -1;
        int columnsSocialCareS3c = -1;
        int columnsSocialCareS3d = -1;
        int columnsSocialCareS3e = -1;
        int columnsUnemploymentU1a = -1;
        int columnsUnemploymentU1b = -1;
        int columnsUnemploymentU1c = -1;
        int columnsUnemploymentU1d = -1;
        int columnsFinancialDistress = -1;
        int columnsEducationE1a = -1;
        int columnsEducationE1b = -1;
        int columnsEducationE2a = -1;
        int columnsPartnershipU1a = -1;
        int columnsPartnershipU1b = -1;
        int columnsPartnershipU2b = -1;
        int columnsPartnershipU1 = -1;
        int columnsPartnershipU2 = -1;
        int columnsFertilityF1a = -1;
        int columnsFertilityF1b = -1;
        int columnsFertilityF1 = -1;
        int columnsIncomeI1a = -1;
        int columnsIncomeI1b = -1;
        int columnsIncomeI3a = -1;
        int columnsIncomeI3b = -1;
        int columnsIncomeI3c = -1;
        int columnsIncomeI4a = -1;
        int columnsIncomeI4b = -1;
        int columnsIncomeI5a = -1;
        int columnsIncomeI5b = -1;
        int columnsIncomeI6a = -1;
        int columnsIncomeI6b = -1;
        int columnsIncomeI3a_selection = -1;
        int columnsIncomeI3b_selection = -1;
        int columnsLeaveHomeP1a = -1;
        int columnsHomeownership = -1;
        int columnsRetirementR1a = -1;
        int columnsRetirementR1b = -1;
        int columnsChildcareC1a = -1;
        int columnsChildcareC1b = -1;
        //For validation below:
        int columnsValidationStudentsByAge = -1;
        int columnsValidationStudentsByRegion = -1;
        int columnsValidationEducationLevel = -1;
        int columnsValidationEducationLevelByAge = -1;
        int columnsValidationEducationLevelByRegion = -1;
        int columnsValidationPartneredBUShareByRegion = -1;
        int columnsValidationDisabledByGender = -1;
        int columnsValidationDisabledByAgeGroup = -1;
        int columnsValidationHealthByAgeGroup = -1;
        int columnsValidationMentalHealthByAgeGroup = -1;
        int columnsValidationHealthMCSByAgeGroup = -1;
        int columnsValidationPhysicalHealthByAgeGroup = -1;
        int columnsValidationLifeSatisfactionByAgeGroup = -1;
        int columnsValidationEmploymentByGender = -1;
        int columnsValidationEmploymentByGenderAndAge = -1;
        int columnsValidationEmploymentByMaternity = -1;
        int columnsValidationEmploymentByGenderAndRegion = -1;
        int columnsValidationActivityStatus = -1;
        int columnsValidationHomeownership = -1;
        int columnsValidationByGenderAndEducation = -1;
        int columnsValidationLabourSupplyByEducation = -1;
        if(country.equals(Country.IT)) {
            columnsWagesMales = 11;
            columnsWagesFemales = 11;
            columnsEmploymentSelectionMales = 14;
            columnsEmploymentSelectionFemales = 14;
            columnsLabourSupplyUtilityMales = 11;
            columnsLabourSupplyUtilityFemales = 11;
            columnsLabourSupplyUtilityMalesWithDependent = 10;
            columnsLabourSupplyUtilityFemalesWithDependent = 9;
            columnsLabourSupplyUtilityACMales = 12;
            columnsLabourSupplyUtilityACFemales = 9;
            columnsLabourSupplyUtilityCouples = 17;
            columnsHealthH1a = 15; //1 for coeffs + 15 for var-cov matrix
            columnsHealthH1b = 22;
            columnsHealthH2b = 24;
            columnsHealthHM1 = 6;
            columnsHealthHM2Males = 9;
            columnsHealthHM2Females = 9;
            columnsEducationE1a = 14;
            columnsEducationE1b = 19;
            columnsEducationE2a = 22;
            columnsPartnershipU1 = 21;
            columnsPartnershipU2 = 27;
            columnsFertilityF1 = 22;
            columnsIncomeI1a = 13;
            columnsIncomeI1b = 22;
            columnsIncomeI3a = 13;
            columnsIncomeI3b = 22;
            columnsIncomeI3c = 23;
            columnsIncomeI3a_selection = 13;
            columnsIncomeI3b_selection = 22;
            columnsLeaveHomeP1a = 19;
            columnsHomeownership = 32;
            columnsRetirementR1a = 19;
            columnsRetirementR1b = 24;
            columnsChildcareC1a = 37;
            columnsChildcareC1b = 37;
            columnsValidationStudentsByAge = 10;
            columnsValidationStudentsByRegion = 6;
            columnsValidationEducationLevel = 3;
            columnsValidationEducationLevelByAge = 24;
            columnsValidationEducationLevelByRegion = 15;
            columnsValidationPartneredBUShareByRegion = 6;
            columnsValidationDisabledByGender = 6;
            columnsValidationDisabledByAgeGroup = 2;
            columnsValidationHealthByAgeGroup = 6;
            columnsValidationEmploymentByGender = 2;
            columnsValidationEmploymentByGenderAndAge = 18;
            columnsValidationEmploymentByMaternity = 3;
            columnsValidationEmploymentByGenderAndRegion = 10;
            columnsValidationActivityStatus = 3;
            columnsValidationLabourSupplyByEducation = 3;
        }
        else if(country.equals(Country.UK)) {
            columnsWagesMalesNE = 30;
            columnsWagesMalesE = 31;
            columnsWagesFemalesNE = 30;
            columnsWagesFemalesE = 31;
            columnsEmploymentSelectionMalesNE = 30;
            columnsEmploymentSelectionMalesE = 29;
            columnsEmploymentSelectionFemalesNE = 30;
            columnsEmploymentSelectionFemalesE = 29;
            columnsLabourSupplyUtilityMales = 19;
            columnsLabourSupplyUtilityFemales = 12;
            columnsLabourSupplyUtilityMalesWithDependent = 23;
            columnsLabourSupplyUtilityFemalesWithDependent = 23;
            columnsLabourSupplyUtilityACMales = 17;
            columnsLabourSupplyUtilityACFemales = 17;
            columnsLabourSupplyUtilityCouples = 64;
            columnsLabourCovid19_SE = 1;
            columnsLabourCovid19_2a_processes = 1;
            columnsHealthH1a = 28;
            columnsHealthH1b = 35;
            columnsHealthH2b = 35;
            columnsHealthHM1 = 30;
            columnsHealthHM2Males = 15;
            columnsHealthHM2Females = 15;
            columnsHealthMCS1 = 30;
            columnsHealthMCS2Males = 15;
            columnsHealthMCS2Females = 15;
            columnsHealthPCS1 = 30;
            columnsHealthPCS2Males = 15;
            columnsHealthPCS2Females = 15;
            columnsLifeSatisfaction1 = 30;
            columnsLifeSatisfaction2Males = 15;
            columnsLifeSatisfaction2Females = 15;
            columnsHealthEQ5D = 8;
            columnsSocialCareS1a = 17;
            columnsSocialCareS1b = 18;
            columnsSocialCareS2a = 32;
            columnsSocialCareS2b = 32;
            columnsSocialCareS2c = 39;
            columnsSocialCareS2d = 17;
            columnsSocialCareS2e = 16;
            columnsSocialCareS2f = 36;
            columnsSocialCareS2g = 21;
            columnsSocialCareS2h = 21;
            columnsSocialCareS2i = 21;
            columnsSocialCareS2j = 21;
            columnsSocialCareS2k = 18;
            columnsSocialCareS3a = 36;
            columnsSocialCareS3b = 38;
            columnsSocialCareS3c = 37;
            columnsSocialCareS3d = 79;
            columnsSocialCareS3e = 37;
            columnsUnemploymentU1a = 19;
            columnsUnemploymentU1b = 19;
            columnsUnemploymentU1c = 19;
            columnsUnemploymentU1d = 19;
            columnsFinancialDistress = 50;
            columnsEducationE1a = 19;
            columnsEducationE1b = 25;
            columnsEducationE2a = 11;
            columnsPartnershipU1a = 27;
            columnsPartnershipU1b = 31;
            columnsPartnershipU2b = 38;
            columnsFertilityF1a = 6;
            columnsFertilityF1b = 26;
            columnsIncomeI1a = 19;  //*
            columnsIncomeI1b = 31;  //*
            columnsIncomeI3a = 20;
            columnsIncomeI3b = 29;
            columnsIncomeI3c = 28;  //*
            columnsIncomeI4a = 24;  //*
            columnsIncomeI4b = 25;
            columnsIncomeI5a = 25;
            columnsIncomeI5b = 25;
            columnsIncomeI6a = 22;  //*
            columnsIncomeI6b = 22;  //*
            columnsIncomeI3a_selection = 20;
            columnsIncomeI3b_selection = 29;
            columnsLeaveHomeP1a = 25;
            columnsHomeownership = 33;
            columnsRetirementR1a = 26;
            columnsRetirementR1b = 31;
            columnsChildcareC1a = 37;
            columnsChildcareC1b = 37;
            columnsValidationStudentsByAge = 10;
            columnsValidationStudentsByRegion = 13;
            columnsValidationEducationLevel = 3;
            columnsValidationEducationLevelByAge = 24;
            columnsValidationEducationLevelByRegion = 36;
            columnsValidationPartneredBUShareByRegion = 13;
            columnsValidationDisabledByGender = 2;
            columnsValidationDisabledByAgeGroup = 6;
            columnsValidationHealthByAgeGroup = 6;
            columnsValidationMentalHealthByAgeGroup = 18;
            columnsValidationHealthMCSByAgeGroup = 18;
            columnsValidationPhysicalHealthByAgeGroup = 18;
            columnsValidationLifeSatisfactionByAgeGroup = 18;
            columnsValidationEmploymentByGender = 2;
            columnsValidationEmploymentByGenderAndAge = 18;
            columnsValidationEmploymentByMaternity = 3;
            columnsValidationEmploymentByGenderAndRegion = 24;
            columnsValidationActivityStatus = 3;
            columnsValidationHomeownership = 1;
            columnsValidationLabourSupplyByEducation = 3;
            columnsValidationByGenderAndEducation = 6;
        }
        else throw new IllegalArgumentException("Country not recognised in Parameters.loadParameters()!");

        //The Raw maps contain the estimates and covariance matrices, from which we bootstrap at the start of each simulation

        //Heckman model employment selection
        //coeffCovarianceEmploymentSelectionMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_employmentSelection.xlsx", countryString + "_EmploymentSelection_Males", 1, columnsEmploymentSelectionMales);
        coeffCovarianceEmploymentSelectionMalesE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_employmentSelection.xlsx", countryString + "_EmploymentSelection_MaleE", 1, columnsEmploymentSelectionMalesE);
        coeffCovarianceEmploymentSelectionMalesNE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_employmentSelection.xlsx", countryString + "_EmploymentSelection_MaleNE", 1, columnsEmploymentSelectionMalesNE);
        //coeffCovarianceEmploymentSelectionFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_employmentSelection.xlsx", countryString + "_EmploymentSelection_Females", 1, columnsEmploymentSelectionFemales);
        coeffCovarianceEmploymentSelectionFemalesE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_employmentSelection.xlsx", countryString + "_EmploymentSelection_FemaleE", 1, columnsEmploymentSelectionFemalesE);
        coeffCovarianceEmploymentSelectionFemalesNE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_employmentSelection.xlsx", countryString + "_EmploymentSelection_FemaleNE", 1, columnsEmploymentSelectionFemalesNE);

        // Wages
        //coeffCovarianceWagesMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_wages.xlsx", countryString + "_Wages_Males", 1, columnsWagesMales);
        coeffCovarianceWagesMalesE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_wages.xlsx", countryString + "_Wages_MalesE", 1, columnsWagesMalesE);
        coeffCovarianceWagesMalesNE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_wages.xlsx", countryString + "_Wages_MalesNE", 1, columnsWagesMalesNE);
        //coeffCovarianceWagesFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_wages.xlsx", countryString + "_Wages_Females", 1, columnsWagesFemales);
        coeffCovarianceWagesFemalesE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_wages.xlsx", countryString + "_Wages_FemalesE", 1, columnsWagesFemalesE);
        coeffCovarianceWagesFemalesNE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_wages.xlsx", countryString + "_Wages_FemalesNE", 1, columnsWagesFemalesNE);

        //Labour Supply coefficients from Zhechun's estimates on the EM input data
        coeffLabourSupplyUtilityMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourSupplyUtility.xlsx", countryString + "_Single_Males", 1, columnsLabourSupplyUtilityMales);
        coeffLabourSupplyUtilityFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourSupplyUtility.xlsx", countryString + "_Single_Females", 1, columnsLabourSupplyUtilityFemales);
        coeffLabourSupplyUtilityMalesWithDependent = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourSupplyUtility.xlsx", countryString + "_Males_With_Dep", 1, columnsLabourSupplyUtilityMalesWithDependent);
        coeffLabourSupplyUtilityFemalesWithDependent = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourSupplyUtility.xlsx", countryString + "_Females_With_Dep", 1, columnsLabourSupplyUtilityFemalesWithDependent);
        coeffLabourSupplyUtilityACMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourSupplyUtility.xlsx", countryString + "_SingleAC_Males", 1, columnsLabourSupplyUtilityACMales);
        coeffLabourSupplyUtilityACFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourSupplyUtility.xlsx", countryString + "_SingleAC_Females", 1, columnsLabourSupplyUtilityACFemales);
        coeffLabourSupplyUtilityCouples = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourSupplyUtility.xlsx", countryString + "_Couples", 1, columnsLabourSupplyUtilityCouples);

        // Load coefficients for Covid-19 labour supply models
        // Coefficients for process assigning simulated people to self-employment
        coeffCovarianceC19LS_SE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_SE", 1, columnsLabourCovid19_SE);
        // Transitions from lagged state: employed
        coeffC19LS_E1_NE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_E1_NE", 1, 1);
        coeffC19LS_E1_SE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_E1_SE", 1, 1);
        coeffC19LS_E1_FF = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_E1_FF", 1, 1);
        coeffC19LS_E1_FX = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_E1_FX", 1, 1);
        coeffC19LS_E1_SC = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_E1_SC", 1, 1);
        // Transitions from lagged state: furloughed full
        coeffC19LS_FF1_E = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_FF1_E", 1, 1);
        coeffC19LS_FF1_FX = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_FF1_FX", 1, 1);
        coeffC19LS_FF1_NE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_FF1_NE", 1, 1);
        coeffC19LS_FF1_SE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_FF1_SE", 1, 1);
        // Transitions from lagged state: furloughed flex
        coeffC19LS_FX1_E = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_FX1_E", 1, 1);
        coeffC19LS_FX1_FF = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_FX1_FF", 1, 1);
        coeffC19LS_FX1_NE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_FX1_NE", 1, 1);
        coeffC19LS_FX1_SE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_FX1_SE", 1, 1);
        // Transitions from lagged state: self-employed
        coeffC19LS_S1_E = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_S1_E", 1, 1);
        coeffC19LS_S1_NE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_S1_NE", 1, 1);
        // Transitions from lagged state: not-employed
        coeffC19LS_U1_E = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_U1_E", 1, 1);
        coeffC19LS_U1_SE = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_U1_SE", 1, 1);

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
        coeffC19LS_E2a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_E2a", 1, columnsLabourCovid19_2a_processes);
        coeffC19LS_E2b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_E2b", 1, columnsLabourCovid19_2a_processes);
        coeffC19LS_F2a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_F2a", 1, columnsLabourCovid19_2a_processes);
        coeffC19LS_F2b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_F2b", 1, columnsLabourCovid19_2a_processes);
        coeffC19LS_F2c = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_F2c", 1, columnsLabourCovid19_2a_processes);
        coeffC19LS_S2a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_S2a", 1, columnsLabourCovid19_2a_processes);
        coeffC19LS_U2a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_U2a", 1, columnsLabourCovid19_2a_processes);

        // Coefficients for probability of SEISS
        coeffC19LS_S3 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_labourCovid19.xlsx", countryString + "_C19LS_S3", 1, 1);

        //Health
        coeffCovarianceHealthH1a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health.xlsx", countryString + "_H1a", 1, columnsHealthH1a);
        coeffCovarianceHealthH1b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health.xlsx", countryString + "_H1b", 1, columnsHealthH1b);
        coeffCovarianceHealthH2b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health.xlsx", countryString + "_H2b", 1, columnsHealthH2b);

        //Social care
        coeffCovarianceSocialCareS1a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S1a", 1, columnsSocialCareS1a);
        coeffCovarianceSocialCareS1b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S1b", 1, columnsSocialCareS1b);
        coeffCovarianceSocialCareS2a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S2a", 1, columnsSocialCareS2a);
        coeffCovarianceSocialCareS2b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S2b", 1, columnsSocialCareS2b);
        coeffCovarianceSocialCareS2c = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S2c", 1, columnsSocialCareS2c);
        coeffCovarianceSocialCareS2d = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S2d", 1, columnsSocialCareS2d);
        coeffCovarianceSocialCareS2e = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S2e", 1, columnsSocialCareS2e);
        coeffCovarianceSocialCareS2f = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S2f", 1, columnsSocialCareS2f);
        coeffCovarianceSocialCareS2g = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S2g", 1, columnsSocialCareS2g);
        coeffCovarianceSocialCareS2h = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S2h", 1, columnsSocialCareS2h);
        coeffCovarianceSocialCareS2i = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S2i", 1, columnsSocialCareS2i);
        coeffCovarianceSocialCareS2j = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S2j", 1, columnsSocialCareS2j);
        coeffCovarianceSocialCareS2k = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S2k", 1, columnsSocialCareS2k);
        coeffCovarianceSocialCareS3a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S3a", 1, columnsSocialCareS3a);
        coeffCovarianceSocialCareS3b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S3b", 1, columnsSocialCareS3b);
        coeffCovarianceSocialCareS3c = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S3c", 1, columnsSocialCareS3c);
        coeffCovarianceSocialCareS3d = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S3d", 1, columnsSocialCareS3d);
        coeffCovarianceSocialCareS3e = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_socialcare.xlsx", countryString + "_S3e", 1, columnsSocialCareS3e);

        //Unemployment
        coeffCovarianceUnemploymentU1a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", countryString + "_U1a", 1, columnsUnemploymentU1a);
        coeffCovarianceUnemploymentU1b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", countryString + "_U1b", 1, columnsUnemploymentU1b);
        coeffCovarianceUnemploymentU1c = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", countryString + "_U1c", 1, columnsUnemploymentU1c);
        coeffCovarianceUnemploymentU1d = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_unemployment.xlsx", countryString + "_U1d", 1, columnsUnemploymentU1d);

        //Financial distress
        coeffCovarianceFinancialDistress = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_financial_distress.xlsx", countryString, 1, columnsFinancialDistress);

        //Health mental: level and case-based
        coeffCovarianceHM1Level = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_mental.xlsx", countryString + "_HM1_L", 1, columnsHealthHM1);
        coeffCovarianceHM2LevelMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_mental.xlsx", countryString + "_HM2_Males_L", 1, columnsHealthHM2Males);
        coeffCovarianceHM2LevelFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_mental.xlsx", countryString + "_HM2_Females_L", 1, columnsHealthHM2Females);
        coeffCovarianceHM1Case = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_mental.xlsx", countryString + "_HM1_C", 1, columnsHealthHM1);
        coeffCovarianceHM2CaseMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_mental.xlsx", countryString + "_HM2_Males_C", 1, columnsHealthHM2Males);
        coeffCovarianceHM2CaseFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_mental.xlsx", countryString + "_HM2_Females_C", 1, columnsHealthHM2Females);


        //Health
        coeffCovarianceDHE_MCS1 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", countryString + "_DHE_MCS1", 1, columnsHealthMCS1);
        coeffCovarianceDHE_MCS2Males = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", countryString + "_DHE_MCS2_Males", 1, columnsHealthMCS2Males);
        coeffCovarianceDHE_MCS2Females = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", countryString + "_DHE_MCS2_Females", 1, columnsHealthMCS2Females);

        coeffCovarianceDHE_PCS1 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", countryString + "_DHE_PCS1", 1, columnsHealthPCS1);
        coeffCovarianceDHE_PCS2Males = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", countryString + "_DHE_PCS2_Males", 1, columnsHealthPCS2Males);
        coeffCovarianceDHE_PCS2Females = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", countryString + "_DHE_PCS2_Females", 1, columnsHealthPCS2Females);

        coeffCovarianceDLS1 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", countryString + "_DLS1", 1, columnsLifeSatisfaction1);
        coeffCovarianceDLS2Males = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", countryString + "_DLS2_Males", 1, columnsLifeSatisfaction2Males);
        coeffCovarianceDLS2Females = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_health_wellbeing.xlsx", countryString + "_DLS2_Females", 1, columnsLifeSatisfaction2Females);

        loadEQ5DParameters(countryString, columnsHealthEQ5D);

        //Life satisfaction

//        coeffCovarianceDLS1 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_lifesatisfaction.xlsx", countryString + "_DLS1", 1, columnsLifeSatisfaction1);

        //Education
        coeffCovarianceEducationE1a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_education.xlsx", countryString + "_E1a", 1, columnsEducationE1a);
        coeffCovarianceEducationE1b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_education.xlsx", countryString + "_E1b", 1, columnsEducationE1b);
        coeffCovarianceEducationE2a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_education.xlsx", countryString + "_E2a", 1, columnsEducationE2a);

        //Partnership
        if (country.equals(Country.UK)) {
            coeffCovariancePartnershipU1a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_partnership.xlsx", countryString + "_U1a", 1, columnsPartnershipU1a);
            coeffCovariancePartnershipU1b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_partnership.xlsx", countryString + "_U1b", 1, columnsPartnershipU1b);
            coeffCovariancePartnershipU2b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_partnership.xlsx", countryString + "_U2b", 1, columnsPartnershipU2b);
        }
        else if (country.equals(Country.IT)) {
            coeffCovariancePartnershipITU1 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_partnership.xlsx", countryString + "_U1", 1, columnsPartnershipU1);
            coeffCovariancePartnershipITU2 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_partnership.xlsx", countryString + "_U2", 1, columnsPartnershipU2);
        }

        //Partnership - parameters for matching based on wage and age differential
        meanCovarianceParametricMatching = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "scenario_parametricMatching.xlsx", countryString, 1, 1);

        //Fertility
        if (country.equals(Country.UK)) {
            coeffCovarianceFertilityF1a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_fertility.xlsx", countryString + "_F1a", 1, columnsFertilityF1a);
            coeffCovarianceFertilityF1b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_fertility.xlsx", countryString + "_F1b", 1, columnsFertilityF1b);
        }
        else if (country.equals(Country.IT)) {
            coeffCovarianceFertilityF1 = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_fertility.xlsx", countryString + "_F1", 1, columnsFertilityF1);
        }

        //Income
        coeffCovarianceIncomeI3a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_income.xlsx", countryString + "_I3a", 1, columnsIncomeI3a);
        coeffCovarianceIncomeI3b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_income.xlsx", countryString + "_I3b", 1, columnsIncomeI3b);
        coeffCovarianceIncomeI4b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_income.xlsx", countryString + "_I4b", 1, columnsIncomeI4b);
        coeffCovarianceIncomeI5a_selection = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_income.xlsx", countryString + "_I5a", 1, columnsIncomeI5a);
        coeffCovarianceIncomeI5b_amount = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_income.xlsx", countryString + "_I5b", 1, columnsIncomeI5b);
        coeffCovarianceIncomeI3a_selection = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_income.xlsx", countryString + "_I3a_selection", 1, columnsIncomeI3a_selection);
        coeffCovarianceIncomeI3b_selection = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_income.xlsx", countryString + "_I3b_selection", 1, columnsIncomeI3b_selection);

        //Leaving parental home
        coeffCovarianceLeaveHomeP1a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_leaveParentalHome.xlsx", countryString + "_P1a", 1, columnsLeaveHomeP1a);

        //Homeownership
        coeffCovarianceHomeownership = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_home_ownership.xlsx", countryString + "_HO1a", 1, columnsHomeownership);

        //Retirement
        coeffCovarianceRetirementR1a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_retirement.xlsx", countryString + "_R1a", 1, columnsRetirementR1a);
        coeffCovarianceRetirementR1b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_retirement.xlsx", countryString + "_R1b", 1, columnsRetirementR1b);

        //Childcare
        coeffCovarianceChildcareC1a = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_childcarecost.xlsx", countryString + "_C1a", 1, columnsChildcareC1a);
        coeffCovarianceChildcareC1b = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "reg_childcarecost.xlsx", countryString + "_C1b", 1, columnsChildcareC1b);

        //Bootstrap the coefficients
        if(bootstrapAll) {

            //Wages
            //coeffCovarianceWagesMales = RegressionUtils.bootstrap(coeffCovarianceWagesMales);
            coeffCovarianceWagesMalesE = RegressionUtils.bootstrap(coeffCovarianceWagesMalesE);
            coeffCovarianceWagesMalesNE = RegressionUtils.bootstrap(coeffCovarianceWagesMalesNE);
            //coeffCovarianceWagesFemales = RegressionUtils.bootstrap(coeffCovarianceWagesFemales);
            coeffCovarianceWagesFemalesE = RegressionUtils.bootstrap(coeffCovarianceWagesFemalesE);
            coeffCovarianceWagesFemalesNE = RegressionUtils.bootstrap(coeffCovarianceWagesFemalesNE);

            //Employment selection
            //coeffCovarianceEmploymentSelectionMales = RegressionUtils.bootstrap(coeffCovarianceEmploymentSelectionMales);
            coeffCovarianceEmploymentSelectionMalesE = RegressionUtils.bootstrap(coeffCovarianceEmploymentSelectionMalesE);
            coeffCovarianceEmploymentSelectionMalesNE = RegressionUtils.bootstrap(coeffCovarianceEmploymentSelectionMalesNE);
            //coeffCovarianceEmploymentSelectionFemales = RegressionUtils.bootstrap(coeffCovarianceEmploymentSelectionFemales);
            coeffCovarianceEmploymentSelectionFemalesE = RegressionUtils.bootstrap(coeffCovarianceEmploymentSelectionFemalesE);
            coeffCovarianceEmploymentSelectionFemalesNE = RegressionUtils.bootstrap(coeffCovarianceEmploymentSelectionFemalesNE);

            //Labour supply utility
            coeffLabourSupplyUtilityMales = RegressionUtils.bootstrap(coeffLabourSupplyUtilityMales);
            coeffLabourSupplyUtilityFemales = RegressionUtils.bootstrap(coeffLabourSupplyUtilityFemales);
            coeffLabourSupplyUtilityMalesWithDependent = RegressionUtils.bootstrap(coeffLabourSupplyUtilityMalesWithDependent);
            coeffLabourSupplyUtilityFemalesWithDependent = RegressionUtils.bootstrap(coeffLabourSupplyUtilityFemalesWithDependent);
            coeffLabourSupplyUtilityACMales = RegressionUtils.bootstrap(coeffLabourSupplyUtilityACMales);
            coeffLabourSupplyUtilityACFemales = RegressionUtils.bootstrap(coeffLabourSupplyUtilityACFemales);
            coeffLabourSupplyUtilityCouples = RegressionUtils.bootstrap(coeffLabourSupplyUtilityCouples);

            //Education
            coeffCovarianceEducationE1a = RegressionUtils.bootstrap(coeffCovarianceEducationE1a);
            coeffCovarianceEducationE1b = RegressionUtils.bootstrap(coeffCovarianceEducationE1b);
            coeffCovarianceEducationE2a = RegressionUtils.bootstrap(coeffCovarianceEducationE2a);

            //Health
            coeffCovarianceHealthH1a = RegressionUtils.bootstrap(coeffCovarianceHealthH1a); //Note that this overrides the original coefficient map with bootstrapped values
            coeffCovarianceHealthH1b = RegressionUtils.bootstrap(coeffCovarianceHealthH1b);
            coeffCovarianceHealthH2b = RegressionUtils.bootstrap(coeffCovarianceHealthH2b);
            coeffCovarianceHM1Level = RegressionUtils.bootstrap(coeffCovarianceHM1Level);
            coeffCovarianceHM2LevelMales = RegressionUtils.bootstrap(coeffCovarianceHM2LevelMales);
            coeffCovarianceHM2LevelFemales = RegressionUtils.bootstrap(coeffCovarianceHM2LevelFemales);
            coeffCovarianceHM1Case = RegressionUtils.bootstrap(coeffCovarianceHM1Case);
            coeffCovarianceHM2CaseMales = RegressionUtils.bootstrap(coeffCovarianceHM2CaseMales);
            coeffCovarianceHM2CaseFemales = RegressionUtils.bootstrap(coeffCovarianceHM2CaseFemales);

            //Social care
            coeffCovarianceSocialCareS1a = RegressionUtils.bootstrap(coeffCovarianceSocialCareS1a);
            coeffCovarianceSocialCareS1b = RegressionUtils.bootstrap(coeffCovarianceSocialCareS1b);
            coeffCovarianceSocialCareS2a = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2a);
            coeffCovarianceSocialCareS2b = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2b);
            coeffCovarianceSocialCareS2c = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2c);
            coeffCovarianceSocialCareS2d = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2d);
            coeffCovarianceSocialCareS2e = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2e);
            coeffCovarianceSocialCareS2f = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2f);
            coeffCovarianceSocialCareS2g = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2g);
            coeffCovarianceSocialCareS2h = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2h);
            coeffCovarianceSocialCareS2i = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2i);
            coeffCovarianceSocialCareS2j = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2j);
            coeffCovarianceSocialCareS2k = RegressionUtils.bootstrap(coeffCovarianceSocialCareS2k);
            coeffCovarianceSocialCareS3a = RegressionUtils.bootstrap(coeffCovarianceSocialCareS3a);
            coeffCovarianceSocialCareS3b = RegressionUtils.bootstrap(coeffCovarianceSocialCareS3b);
            coeffCovarianceSocialCareS3c = RegressionUtils.bootstrap(coeffCovarianceSocialCareS3c);
            coeffCovarianceSocialCareS3d = RegressionUtils.bootstrap(coeffCovarianceSocialCareS3d);
            coeffCovarianceSocialCareS3e = RegressionUtils.bootstrap(coeffCovarianceSocialCareS3e);

            //Unemployment
            coeffCovarianceUnemploymentU1a = RegressionUtils.bootstrap(coeffCovarianceUnemploymentU1a);
            coeffCovarianceUnemploymentU1b = RegressionUtils.bootstrap(coeffCovarianceUnemploymentU1b);
            coeffCovarianceUnemploymentU1c = RegressionUtils.bootstrap(coeffCovarianceUnemploymentU1c);
            coeffCovarianceUnemploymentU1d = RegressionUtils.bootstrap(coeffCovarianceUnemploymentU1d);

            //Non-labour income
            // coeffCovarianceIncomeI1a = RegressionUtils.bootstrap(coeffCovarianceIncomeI1a); // Commented out as not used any more since income is split.
            // coeffCovarianceIncomeI1b = RegressionUtils.bootstrap(coeffCovarianceIncomeI1b); // Commented out as not used any more since income is split.
            coeffCovarianceIncomeI3a = RegressionUtils.bootstrap(coeffCovarianceIncomeI3a);
            coeffCovarianceIncomeI3b = RegressionUtils.bootstrap(coeffCovarianceIncomeI3b);
            //coeffCovarianceIncomeI3c = RegressionUtils.bootstrap(coeffCovarianceIncomeI3c);
            //coeffCovarianceIncomeI4a = RegressionUtils.bootstrap(coeffCovarianceIncomeI4a);
            coeffCovarianceIncomeI4b = RegressionUtils.bootstrap(coeffCovarianceIncomeI4b);
            coeffCovarianceIncomeI5a_selection = RegressionUtils.bootstrap(coeffCovarianceIncomeI5a_selection);
            coeffCovarianceIncomeI5b_amount = RegressionUtils.bootstrap(coeffCovarianceIncomeI5b_amount);
            //coeffCovarianceIncomeI6a_selection = RegressionUtils.bootstrap(coeffCovarianceIncomeI6a_selection);
            //coeffCovarianceIncomeI6b_amount = RegressionUtils.bootstrap(coeffCovarianceIncomeI6b_amount);
            coeffCovarianceIncomeI3a_selection = RegressionUtils.bootstrap(coeffCovarianceIncomeI3a_selection);
            coeffCovarianceIncomeI3b_selection = RegressionUtils.bootstrap(coeffCovarianceIncomeI3b_selection);

            //Leave parental home
            coeffCovarianceLeaveHomeP1a = RegressionUtils.bootstrap(coeffCovarianceLeaveHomeP1a);

            //Homeownership
            coeffCovarianceHomeownership = RegressionUtils.bootstrap(coeffCovarianceHomeownership);

            //Retirement
            coeffCovarianceRetirementR1a = RegressionUtils.bootstrap(coeffCovarianceRetirementR1a);
            coeffCovarianceRetirementR1b = RegressionUtils.bootstrap(coeffCovarianceRetirementR1b);

            //Childcare
            coeffCovarianceChildcareC1a = RegressionUtils.bootstrap(coeffCovarianceChildcareC1a);
            coeffCovarianceChildcareC1b = RegressionUtils.bootstrap(coeffCovarianceChildcareC1b);

            //Specification of some processes depends on the country:
            if (country.equals(Country.UK)) {
                coeffCovariancePartnershipU1a = RegressionUtils.bootstrap(coeffCovariancePartnershipU1a);
                coeffCovariancePartnershipU1b = RegressionUtils.bootstrap(coeffCovariancePartnershipU1b);
                coeffCovariancePartnershipU2b = RegressionUtils.bootstrap(coeffCovariancePartnershipU2b);
                coeffCovarianceFertilityF1a = RegressionUtils.bootstrap(coeffCovarianceFertilityF1a);
                coeffCovarianceFertilityF1b = RegressionUtils.bootstrap(coeffCovarianceFertilityF1b);
            } else if (country.equals(Country.IT)) {
                coeffCovariancePartnershipITU1 = RegressionUtils.bootstrap(coeffCovariancePartnershipITU1);
                coeffCovariancePartnershipITU2 = RegressionUtils.bootstrap(coeffCovariancePartnershipITU2);
                coeffCovarianceFertilityF1 = RegressionUtils.bootstrap(coeffCovarianceFertilityF1);
            }
        }

        //Health
        regHealthH1a = new OrderedRegression<>(RegressionType.GenOrderedLogit, Dhe.class, coeffCovarianceHealthH1a);
        regHealthH1b = new OrderedRegression<>(RegressionType.GenOrderedLogit, Dhe.class, coeffCovarianceHealthH1b);
        regHealthH2b = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceHealthH2b);

        //Social care
        regReceiveCareS1a = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceSocialCareS1a);
        regCareHoursS1b = new LinearRegression(coeffCovarianceSocialCareS1b);
        regNeedCareS2a = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceSocialCareS2a);
        regReceiveCareS2b = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceSocialCareS2b);
        regSocialCareMarketS2c = new MultinomialRegression<>(RegressionType.MultinomialLogit, SocialCareReceiptS2c.class, coeffCovarianceSocialCareS2c);
        regReceiveCarePartnerS2d = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceSocialCareS2d);
        regPartnerSupplementaryCareS2e = new MultinomialRegression<>(RegressionType.MultinomialLogit, PartnerSupplementaryCarer.class, coeffCovarianceSocialCareS2e);
        regNotPartnerInformalCareS2f = new MultinomialRegression<>(RegressionType.MultinomialLogit, NotPartnerInformalCarer.class, coeffCovarianceSocialCareS2f);
        regPartnerCareHoursS2g = new LinearRegression(coeffCovarianceSocialCareS2g);
        regDaughterCareHoursS2h = new LinearRegression(coeffCovarianceSocialCareS2h);
        regSonCareHoursS2i = new LinearRegression(coeffCovarianceSocialCareS2i);
        regOtherCareHoursS2j = new LinearRegression(coeffCovarianceSocialCareS2j);
        regFormalCareHoursS2k = new LinearRegression(coeffCovarianceSocialCareS2k);
        regCarePartnerProvCareToOtherS3a = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceSocialCareS3a);
        regNoCarePartnerProvCareToOtherS3b = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceSocialCareS3b);
        regNoPartnerProvCareToOtherS3c = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceSocialCareS3c);
        regInformalCareToS3d = new MultinomialRegression<>(RegressionType.MultinomialLogit, SocialCareProvision.class, coeffCovarianceSocialCareS3d);
        regCareHoursProvS3e = new LinearRegression(coeffCovarianceSocialCareS3e);

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

        regHealthHM1Case = new BinomialRegression(RegressionType.Logit, Indicator.class, coeffCovarianceHM1Case);
        regHealthHM2CaseMales = new BinomialRegression(RegressionType.Logit, Indicator.class, coeffCovarianceHM2CaseMales);
        regHealthHM2CaseFemales = new BinomialRegression(RegressionType.Logit, Indicator.class, coeffCovarianceHM2CaseFemales);

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

        regEducationE1a = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceEducationE1a);
        regEducationE1b = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceEducationE1b);
        regEducationE2a = new OrderedRegression<>(RegressionType.GenOrderedLogit, Education.class, coeffCovarianceEducationE2a);

        //Partnership
        if (country.equals(Country.UK)) {
            MultiKeyCoefficientMap coeffPartnershipU1aAppended = appendCoefficientMaps(coeffCovariancePartnershipU1a, partnershipTimeAdjustment, "Year");
            MultiKeyCoefficientMap coeffPartnershipU1bAppended = appendCoefficientMaps(coeffCovariancePartnershipU1b, partnershipTimeAdjustment, "Year");
            MultiKeyCoefficientMap coeffPartnershipU2bAppended = appendCoefficientMaps(coeffCovariancePartnershipU2b, partnershipTimeAdjustment, "Year", true);
            regPartnershipU1a = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffPartnershipU1aAppended);
            regPartnershipU1b = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffPartnershipU1bAppended);
            regPartnershipU2b = new BinomialRegression(RegressionType.Probit, ReversedIndicator.class, coeffPartnershipU2bAppended);
        } else if (country.equals(Country.IT)) {
            regPartnershipITU1 = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovariancePartnershipITU1);
            regPartnershipITU2 = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovariancePartnershipITU2);
        }

        //Fertility
        if (country.equals(Country.UK)) {
            MultiKeyCoefficientMap coeffFertilityF1aAppended = appendCoefficientMaps(coeffCovarianceFertilityF1a, fertilityTimeAdjustment, "Year");
            MultiKeyCoefficientMap coeffFertilityF1bAppended = appendCoefficientMaps(coeffCovarianceFertilityF1b, fertilityTimeAdjustment, "Year");
            regFertilityF1a = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffFertilityF1aAppended);
            regFertilityF1b = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffFertilityF1bAppended);
        } else if (country.equals(Country.IT)) {
            regFertilityF1 = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceFertilityF1);
        }

        //Income
        //regIncomeI1a = new LinearRegression(coeffCovarianceIncomeI1a);
        //regIncomeI1b = new LinearRegression(coeffCovarianceIncomeI1b);
        regIncomeI3a = new LinearRegression(coeffCovarianceIncomeI3a);
        regIncomeI3b = new LinearRegression(coeffCovarianceIncomeI3b);
        //regIncomeI3c = new LinearRegression(coeffCovarianceIncomeI3c);
        //regIncomeI4a = new LinearRegression(coeffCovarianceIncomeI4a);
        regIncomeI4b = new LinearRegression(coeffCovarianceIncomeI4b);
        regIncomeI5b_amount = new LinearRegression(coeffCovarianceIncomeI5b_amount);
        //regIncomeI6b_amount = new LinearRegression(coeffCovarianceIncomeI6b_amount);
        regIncomeI3a_selection = new BinomialRegression(RegressionType.Logit, Indicator.class, coeffCovarianceIncomeI3a_selection);
        regIncomeI3b_selection = new BinomialRegression(RegressionType.Logit, Indicator.class, coeffCovarianceIncomeI3b_selection);
        regIncomeI5a_selection = new BinomialRegression(RegressionType.Logit, Indicator.class, coeffCovarianceIncomeI5a_selection);
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
        regLabourSupplyUtilityMalesWithDependent = new LinearRegression(coeffLabourSupplyUtilityMalesWithDependent);
        regLabourSupplyUtilityFemalesWithDependent = new LinearRegression(coeffLabourSupplyUtilityFemalesWithDependent);
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
        regLeaveHomeP1a = new BinomialRegression(RegressionType.Probit, Indicator.class, coeffCovarianceLeaveHomeP1a);

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
        validationStudentsByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_studentsByAge", 1, columnsValidationStudentsByAge);

        //Students by Region
        validationStudentsByRegion = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_studentsByRegion", 1, columnsValidationStudentsByRegion);

        //Education level of over 17 year olds
        validationEducationLevel = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_educationLevel", 1, columnsValidationEducationLevel);

        //Education level by age group
        validationEducationLevelByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_educationLevelByAge", 1, columnsValidationEducationLevelByAge);

        //Education level by region
        validationEducationLevelByRegion = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_educationLevelByRegion", 1, columnsValidationEducationLevelByRegion);

        //Partnered BU share by region
        validationPartneredShareByRegion = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_partneredBUShareByRegion", 1, columnsValidationPartneredBUShareByRegion);

        //Disabled by age
        validationDisabledByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_disabledByAgeGroup", 1, columnsValidationDisabledByAgeGroup);

        validationDisabledByGender = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_disabledByGender", 1, columnsValidationDisabledByGender);

        //Health by age
        validationHealthByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_healthByAgeGroup", 1, columnsValidationHealthByAgeGroup);

        //Mental health by age and gender
        validationMentalHealthByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_mentalHealthByAgeGroup", 1, columnsValidationMentalHealthByAgeGroup);


        validationHealthMCSByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_healthMCSByAgeGroup", 1, columnsValidationHealthMCSByAgeGroup);
        validationHealthPCSByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_healthPCSByAgeGroup", 1, columnsValidationPhysicalHealthByAgeGroup);
        validationLifeSatisfactionByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_lifeSatisfactionByAgeGroup", 1, columnsValidationLifeSatisfactionByAgeGroup);

        //Psychological distress by age and gender
        validationPsychDistressByAge = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_psychDistressByAgeGroup", 1, columnsValidationMentalHealthByAgeGroup);
        validationPsychDistressByAgeLow = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_psychDistressByAgeGroupLowED", 1, columnsValidationMentalHealthByAgeGroup);
        validationPsychDistressByAgeMed = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_psychDistressByAgeGroupMedED", 1, columnsValidationMentalHealthByAgeGroup);
        validationPsychDistressByAgeHigh = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_psychDistressByAgeGroupHiEd", 1, columnsValidationMentalHealthByAgeGroup);

        //Employment by gender
        validationEmploymentByGender = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_employmentByGender", 1, columnsValidationEmploymentByGender);

        //Employment by age and gender
        validationEmploymentByAgeAndGender = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_employmentByGenderAndAge", 1, columnsValidationEmploymentByGenderAndAge);

        //Employment by maternity
        validationEmploymentByMaternity = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_employmentByMaternity", 1, columnsValidationEmploymentByMaternity);

        //Employment by gender and region
        validationEmploymentByGenderAndRegion = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_employmentByGenderAndRegion", 1, columnsValidationEmploymentByGenderAndRegion);

        //Labour supply by education
        validationLabourSupplyByEducation = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_labourSupplyByEducation", 1, columnsValidationLabourSupplyByEducation);

        //Activity status
        validationActivityStatus = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_activityStatus", 1, columnsValidationActivityStatus);

        //Homeownership status
        validationHomeownershipBenefitUnits = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_homeownership", 1, columnsValidationHomeownership);

        //Gross earnings yearly by education and gender (for employed persons)
        validationGrossEarningsByGenderAndEducation = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_grossEarningsByGenderAndEdu", 1, columnsValidationByGenderAndEducation);

        //Hourly wages by education and gender (for employed persons)
        validationLhwByGenderAndEducation = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_lhwByGenderAndEdu", 1, 8);

        //Hours worked weekly by education and gender (for employed persons)
        hourlyWageByGenderAndEducation = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "validation_statistics.xlsx", countryString + "_hourlywageByGenderAndEdu", 1, columnsValidationByGenderAndEducation);
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
            for(Gender gender: Gender.values()) {
                int minAge = Parameters.MIN_AGE_TO_LEAVE_EDUCATION;
                probSick.put(gender, minAge, rawProbSick.getValue(gender.toString(), minAge));
            }

            for(Object o: rawProbSick.keySet()) {
                MultiKey mk = (MultiKey)o;
                String gender = (String) mk.getKey(0);
                int rawAge = (int) mk.getKey(1);
                int adjustedAge;
                if(gender.equals(Gender.Male.toString())) {
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
            for(Gender gender: Gender.values()) {
                for(int i = Parameters.MIN_AGE_TO_LEAVE_EDUCATION; i < retirementAge.get(gender); i++) {
                    if(!probSick.containsKey(gender, i)) {
                        double youngerVal = ((Number)probSick.get(gender, i-1)).doubleValue();
                        double olderVal = ((Number)probSick.get(gender, i+1)).doubleValue();
                        probSick.put(gender, i, 0.5*(youngerVal + olderVal));	//Insert arithmetic average between ages
                    }
                }
            }

        }
    }

    public static void updateUnemploymentRate(int year) {
//		unemploymentRatesByRegion.clear();
        for(Region region: countryRegions) {
            unemploymentRatesByRegion.put(region, ((Number)unemploymentRates.getValue(region.toString(), year)).doubleValue());
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
        double valueRMSE = ((Number) coefficientMapRMSE.getValue(regressionName)).doubleValue();
        return valueRMSE;
    }

    private static void calculateFertilityRatesFromProjections() {

        fertilityRateByRegionYear = MultiKeyMap.multiKeyMap(new LinkedMap<>());
        fertilityRateByYear = new HashMap<>();
        for (int year = startYear; year <= endYear; year++) {

            double projectedNumFertileWomenAll = 0.0, numNewBornAll = 0.0;
            for (Region region : countryRegions) {

                double projectedNumFertileWomenByRegion = 0.0;
                for (int age = MIN_AGE_MATERNITY; age <= MAX_AGE_MATERNITY; age++) {
                    projectedNumFertileWomenByRegion += getPopulationProjections(Gender.Female, region, age, year);
                }

                double numNewBornByRegion = 0.;
                for (Gender gender: Gender.values()) {
                    numNewBornByRegion += getPopulationProjections(gender, region, 0, year);		//Number of people aged 0 in projected years
                }

                if (projectedNumFertileWomenByRegion <= 0.) {
                    throw new IllegalArgumentException("Projected Number of Females of Fertile Age is not positive!");
                }
                else {
                    projectedNumFertileWomenAll += projectedNumFertileWomenByRegion;
                    numNewBornAll += numNewBornByRegion;
                    fertilityRateByRegionYear.put(region, year, numNewBornByRegion / projectedNumFertileWomenByRegion);
                }
            }
            fertilityRateByYear.put(year, numNewBornAll / projectedNumFertileWomenAll);
        }
    }


    private static void calculatePopulationGrowthRatiosFromProjections() {

        populationGrowthRatiosByRegionYear = MultiKeyMap.multiKeyMap(new LinkedMap<>());
        for(int year = startYear+1; year <= endYear; year++) {		//Year is the latter year, i.e. growth ratio for year t is Pop(t)/Pop(t-1)
            for(Region region : countryRegions) {
                double numberOfPeopleInRegionThisYear = 0.;
                double numberOfPeopleInRegionPreviousYear = 0.;
                for(Gender gender: Gender.values()) {
                    for(int age = 0; age <= maxAge; age++) {
                        numberOfPeopleInRegionThisYear += getPopulationProjections(gender, region, age, year);
                        numberOfPeopleInRegionPreviousYear += getPopulationProjections(gender, region, age, year-1);
                    }
                }
                populationGrowthRatiosByRegionYear.put(region, year, numberOfPeopleInRegionThisYear / numberOfPeopleInRegionPreviousYear);
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
        for(Region region : Region.values()) {			//TODO: restrict this to only regions in the simulated country
            if(region.toString().startsWith(country.toString())) {			//Only assess the relevant regions for the country
                countryRegions.add(region);				//Create a set of only relevant regions that we can use below TODO: This should be done in the Parameters class, once and for all!
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

    public static OrderedRegression getRegHealthH1a() { return regHealthH1a; }
    public static OrderedRegression getRegHealthH1b() { return regHealthH1b; }
    public static BinomialRegression getRegHealthH2b() { return regHealthH2b; }

    public static BinomialRegression getRegReceiveCareS1a() { return regReceiveCareS1a; }
    public static LinearRegression getRegCareHoursS1b() { return regCareHoursS1b; }
    public static BinomialRegression getRegNeedCareS2a() { return regNeedCareS2a; }
    public static BinomialRegression getRegReceiveCareS2b() { return regReceiveCareS2b; }
    public static MultinomialRegression getRegSocialCareMarketS2c() { return regSocialCareMarketS2c; }
    public static BinomialRegression getRegReceiveCarePartnerS2d() { return regReceiveCarePartnerS2d; }
    public static MultinomialRegression getRegPartnerSupplementaryCareS2e() { return regPartnerSupplementaryCareS2e; }
    public static MultinomialRegression getRegNotPartnerInformalCareS2f() { return regNotPartnerInformalCareS2f; }
    public static LinearRegression getRegPartnerCareHoursS2g() { return regPartnerCareHoursS2g; }
    public static LinearRegression getRegDaughterCareHoursS2h() { return regDaughterCareHoursS2h; }
    public static LinearRegression getRegSonCareHoursS2i() { return regSonCareHoursS2i; }
    public static LinearRegression getRegOtherCareHoursS2j() { return regOtherCareHoursS2j; }
    public static LinearRegression getRegFormalCareHoursS2k() { return regFormalCareHoursS2k; }
    public static BinomialRegression getRegCarePartnerProvCareToOtherS3a() { return regCarePartnerProvCareToOtherS3a; }
    public static BinomialRegression getRegNoCarePartnerProvCareToOtherS3b() { return regNoCarePartnerProvCareToOtherS3b; }
    public static BinomialRegression getRegNoPartnerProvCareToOtherS3c() { return regNoPartnerProvCareToOtherS3c; }
    public static MultinomialRegression getRegInformalCareToS3d() { return regInformalCareToS3d; }
    public static LinearRegression getRegCareHoursProvS3e() { return regCareHoursProvS3e; }

    public static BinomialRegression getRegUnemploymentMaleGraduateU1a() { return regUnemploymentMaleGraduateU1a; }
    public static BinomialRegression getRegUnemploymentMaleNonGraduateU1b() { return regUnemploymentMaleNonGraduateU1b; }
    public static BinomialRegression getRegUnemploymentFemaleGraduateU1c() { return regUnemploymentFemaleGraduateU1c; }
    public static BinomialRegression getRegUnemploymentFemaleNonGraduateU1d() { return regUnemploymentFemaleNonGraduateU1d; }

    public static BinomialRegression getRegFinancialDistress() { return regFinancialDistress; }

    public static LinearRegression getRegHealthHM1Level() { return regHealthHM1Level; }
    public static LinearRegression getRegHealthHM2LevelMales() { return regHealthHM2LevelMales; }
    public static LinearRegression getRegHealthHM2LevelFemales() { return regHealthHM2LevelFemales; }
    public static BinomialRegression getRegHealthHM1Case() {return regHealthHM1Case;}
    public static BinomialRegression getRegHealthHM2CaseMales() {return regHealthHM2CaseMales;}
    public static BinomialRegression getRegHealthHM2CaseFemales() {return regHealthHM2CaseFemales;}

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
    public static OrderedRegression getRegEducationE2a() {return regEducationE2a;}

    public static LinearRegression getRegEQ5D() { return regHealthEQ5D; };

    public static BinomialRegression getRegPartnershipU1a() {return regPartnershipU1a;}
    public static BinomialRegression getRegPartnershipU1b() {return regPartnershipU1b;}
    public static BinomialRegression getRegPartnershipU2b() {return regPartnershipU2b;}
    public static BinomialRegression getRegPartnershipITU1() {return regPartnershipITU1;}
    public static BinomialRegression getRegPartnershipITU2() {return regPartnershipITU2;}

    public static BinomialRegression getRegFertilityF1a() {return regFertilityF1a;}
    public static BinomialRegression getRegFertilityF1b() {return regFertilityF1b;}
    public static BinomialRegression getRegFertilityF1() {return regFertilityF1;}

    public static LinearRegression getRegIncomeI1a() {return regIncomeI1a;}
    public static LinearRegression getRegIncomeI1b() {return regIncomeI1b;}
    public static LinearRegression getRegIncomeI3a() { return regIncomeI3a; }
    public static LinearRegression getRegIncomeI3b() { return regIncomeI3b; }
    public static LinearRegression getRegIncomeI3c() { return regIncomeI3c; }
    public static LinearRegression getRegIncomeI4a() { return regIncomeI4a; }
    public static LinearRegression getRegIncomeI4b() {return regIncomeI4b;}
    public static LinearRegression getRegIncomeI5b_amount() {return regIncomeI5b_amount;}
    public static LinearRegression getRegIncomeI6b_amount() { return regIncomeI6b_amount; }
    public static BinomialRegression getRegIncomeI3a_selection() { return regIncomeI3a_selection; }
    public static BinomialRegression getRegIncomeI3b_selection() { return regIncomeI3b_selection; }
    public static BinomialRegression getRegIncomeI5a_selection() { return regIncomeI5a_selection; }
    public static BinomialRegression getRegIncomeI6a_selection() { return regIncomeI6a_selection; }

    public static BinomialRegression getRegHomeownershipHO1a() {return regHomeownershipHO1a;}

    public static Set<Region> getCountryRegions() {
        return countryRegions;
    }

    public static MultiKeyMap getFertilityRateByRegionYear() {
        return fertilityRateByRegionYear;
    }

    public static double getFertilityRateByRegionYear(Region region, int year) {
        int yearHere = Math.max(fertilityProjectionsMinYear, Math.min(fertilityProjectionsMaxYear, year));
        //We calculate the rate per woman, but the standard to report (and what is used in the estimates) is per 1000 hence multiplication
        return 1000*((Number)fertilityRateByRegionYear.get(region, yearHere)).doubleValue();
    }

    public static double getUnemploymentRateByGenderEducationAgeYear(Gender gender, Education education, int age, int year) {
        double val;
        if (gender.equals(Gender.Male)) {
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

    public static LinearRegression getRegLabourSupplyUtilityFemalesWithDependent() {
        return regLabourSupplyUtilityFemalesWithDependent;
    }

    public static LinearRegression getRegLabourSupplyUtilityMalesWithDependent() {
        return regLabourSupplyUtilityMalesWithDependent;
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
    public static double getUnemploymentRatesForRegion(Region region) {
        return unemploymentRatesByRegion.get(region);
    }
    */

    public static MultiKeyCoefficientMap getMarriageTypesFrequency() {
        return marriageTypesFrequency;
    }

    public static MultiKeyCoefficientMap getCoeffCovarianceHealthH1a() { return coeffCovarianceHealthH1a; }

    public static MultiKeyCoefficientMap getCoeffCovarianceHealthH1b() { return coeffCovarianceHealthH1b; }

    public static MultiKeyCoefficientMap getCoeffCovarianceWagesMalesE() { return coeffCovarianceWagesMalesE; }
    public static MultiKeyCoefficientMap getCoeffCovarianceWagesMalesNE() { return coeffCovarianceWagesMalesNE; }
    public static MultiKeyCoefficientMap getCoeffCovarianceWagesFemalesE() { return coeffCovarianceWagesFemalesE; }
    public static MultiKeyCoefficientMap getCoeffCovarianceWagesFemalesNE() { return coeffCovarianceWagesFemalesNE; }
    public static MultiKeyCoefficientMap getCoefficientMapRMSE() { return coefficientMapRMSE; }


    public static double getMortalityProbability(Gender gender, int age, int year) {

        double mortalityProbability;
        int yearEval = Math.min(mortalityProbabilityMaxYear, Math.max(mortalityProbabilityMinYear, year));
        int ageEval = Math.min(mortalityProbabilityMaxAge, age);
        Number prob = ((Number) mortalityProbabilityByGenderAgeYear.getValue(gender.toString(), ageEval, yearEval));
        if (prob==null) {
            throw new IllegalAccessError("ERROR - problem evaluating mortality probability for year: " + yearEval + ", age: " + ageEval + " and gender " + gender.toString());
        }
        mortalityProbability = prob.doubleValue() / 100000.0;

        return mortalityProbability;
    }

    public static double getPopulationProjections(Gender gender, Region region, int age, int year) {

        double populationProjection;
        int yearEval = Math.min(populationProjectionsMaxYear, Math.max(populationProjectionsMinYear, year));
        int ageEval = Math.min(populationProjectionsMaxAge, age);
        Number val = ((Number)populationProjections.getValue(gender.toString(), region.toString(), ageEval, yearEval));
        if (val==null)
            throw new IllegalAccessError("ERROR - problem evaluating population projection for year: " + yearEval + ", age: " + ageEval + ", region: " + region.toString() + " and gender: " + gender.toString());
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

    public synchronized static double[] getWageAndAgeDifferentialMultivariateNormalDistribution(long seed) {
        wageAndAgeDifferentialMultivariateNormalDistribution.reseedRandomGenerator(seed);
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

        // load time varying rates
        priceMapRealSavingReturns = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_saving_returns", 1, 1);
        priceMapRealDebtCostLow = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_debt_cost_low", 1, 1);
        priceMapRealDebtCostHigh = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_debt_cost_hi", 1, 1);

        // load time varying wage rates
        wageRateFormalSocialCare = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_carer_hourly_wage", 1, 1);

        // load time varying indices
        upratingIndexMapRealGDP = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_gdp", 1, 1);
        upratingIndexMapInflation = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_inflation", 1, 1);
        upratingIndexMapRealWageGrowth = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_wage_growth", 1, 1);
        socialCareProvisionTimeAdjustment = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_care_adjustment", 1, 1);
        partnershipTimeAdjustment = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_cohabitation_adjustment", 1, 1);
        fertilityTimeAdjustment = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_fertility_adjustment", 1, 1);
        utilityTimeAdjustmentSingleMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_utility_adj_smales", 1, 1);
        utilityTimeAdjustmentSingleFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_utility_adj_sfemales", 1, 1);
        utilityTimeAdjustmentCouples = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "time_series_factor.xlsx", country.toString() + "_utility_adj_couples", 1, 1);

        // rebase indices to base year defined by BASE_PRICE_YEAR
        rebaseIndexMap(TimeSeriesVariable.GDP);
        rebaseIndexMap(TimeSeriesVariable.Inflation);
        rebaseIndexMap(TimeSeriesVariable.WageGrowth);

        // load year-specific fiscal policy parameters
        socialCarePolicy = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "policy parameters.xlsx", "social care", 1, 8);
        partneredShare = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "policy parameters.xlsx", "partnership", 1, 1);
        employedShareSingleMales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "policy parameters.xlsx", "employment_smales", 1, 1);
        employedShareSingleFemales = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "policy parameters.xlsx", "employment_sfemales", 1, 1);
        employedShareCouples = ExcelAssistant.loadCoefficientMap(Parameters.getInputDirectory() + "policy parameters.xlsx", "employment_couples", 1, 1);

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
            case UtilityAdjustmentSingleFemales -> {
                map = utilityTimeAdjustmentSingleFemales;
            }
            case UtilityAdjustmentCouples -> {
                map = utilityTimeAdjustmentCouples;
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
            case EmploymentSingleMales -> {
                map = employedShareSingleMales;
            }
            case EmploymentSingleFemales -> {
                map = employedShareSingleFemales;
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
                // extend series forward through time

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
                // extend series backward through time

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
        throw new RuntimeException("failed to find requested time varying rate");
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
        throw new RuntimeException("failed to find requested time varying rate");
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
                // extend series forward through time

                double outOfSampleRate = ((Number) mapToExtend.getValue(yearMax)).doubleValue();
                for (int yy = yearMax + 1; yy <= year; yy++) {
                    mapToExtend.putValue(yy, outOfSampleRate);
                }
                val = outOfSampleRate;
            }
            if (year < yearMin) {
                // extend series backward through time

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
        if (val==null) {

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
                // extend backward through time

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
                // extend forward through time

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

    public static void setCoeffLabourSupplyUtilityMales(MultiKeyCoefficientMap coeffLabourSupplyUtilityMales) {
        Parameters.coeffLabourSupplyUtilityMales = coeffLabourSupplyUtilityMales;
    }

    public static MultiKeyCoefficientMap getCoeffLabourSupplyUtilityFemales() {
        return coeffLabourSupplyUtilityFemales;
    }

    public static void setCoeffLabourSupplyUtilityFemales(MultiKeyCoefficientMap coeffLabourSupplyUtilityFemales) {
        Parameters.coeffLabourSupplyUtilityFemales = coeffLabourSupplyUtilityFemales;
    }

    public static MultiKeyCoefficientMap getCoeffLabourSupplyUtilityCouples() {
        return coeffLabourSupplyUtilityCouples;
    }

    public static void setCoeffLabourSupplyUtilityCouples(MultiKeyCoefficientMap coeffLabourSupplyUtilityCouples) {
        Parameters.coeffLabourSupplyUtilityCouples = coeffLabourSupplyUtilityCouples;
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
                if (val==null)
                    throw new RuntimeException("value undefined for partnershipAlignAdjustment in year " + year);
                return val;
            }
            case FertilityAlignment -> {
                Double val = fertilityAlignAdjustment.get(year);
                if (val==null)
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
        if (val==null)
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
        DataParser.databaseFromCSV(country, executeWithGui); // Initial database tables

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

    public static void loadEQ5DParameters(String countryString, int columnsHealthEQ5D) {

        coeffCovarianceEQ5D = ExcelAssistant.loadCoefficientMap(getInputDirectory() + "reg_eq5d.xlsx", countryString + "_EQ5D_" + eq5dConversionParameters, 1, columnsHealthEQ5D);

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
}
