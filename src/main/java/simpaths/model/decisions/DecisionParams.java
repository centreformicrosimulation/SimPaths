package simpaths.model.decisions;

import simpaths.data.Parameters;
import simpaths.model.AnnuityRates;
import simpaths.model.enums.*;

import java.io.File;

/**
 * CLASS TO STORE FIXED PARAMETERS FOR INTERTEMPORAL OPTIMISATION DECISIONS
 */
public class DecisionParams {

    // RUNNING OPTIONS
    public static final boolean PARALLELISE_SOLUTIONS = true;
    public static boolean saveGridSlicesToCSV = false;
    public static boolean saveIntermediateSolutions = false;
    public static boolean saveImperfectTaxDbMatches = false;

    public static final boolean FILTER_LOCAL_EXPECTATIONS = true;    // screens expectations to omit low probability events
    public static final double MIN_STATE_PROBABILITY = 0.01;          // if FILTER_LOCAL_EXPECTATIONS, omits state-specific events with probability under this threshold
    public static final double MIN_FACTOR_PROBABILITY = 0.05;         // if FILTER_LOCAL_EXPECTATIONS, omits events with probability less than mean probability multiplied by this threshold

    public static final boolean SOLVE_FROM_INTERMEDIATE = false;
    public static final int SOLVE_FROM_AGE = 74;                     // if SOLVE_FROM_INTERMEDIATE

    // MODEL SETTINGS
    public static final double GRID_DEFAULT_VALUE = 999.0;
    public static boolean flagRetirement;                             // model retirement state
    public static boolean flagPrivatePension;
    public static int optionsEmployment1;                             // number of discrete employment alternatives to consider for principal earner
    public static int optionsEmployment2;                             // number of discrete employment alternatives to consider for secondary earner
    public static int startYear;                                      // first year considered for simulation
    public static boolean flagHealth;                                 // user option to include health in state space for IO solution
    public static int minAgeForPoorHealth;                            // minimum age for expecting less than perfect health
    public static boolean flagDisability;                             // user option to include disability in state space for IO solution
    public static boolean flagRegion;                                 // user option to indicate region in state space for IO solution
    public static boolean flagEducation;                              // user option to indicate student and education in state space for IO solution
    public static boolean flagLowWageOffer1 = false;                  // flag identifying whether to allow for wage offers in state space for principal earner - THIS IS HARD-CODED AT PRESENT AND ONLY PARTLY IMPLEMENTED
    public static final boolean FLAG_WAGE_OFFER2 = false;             // flag identifying whether to allow for wage offers in state space for secondary earner - THIS IS HARD-CODED AT PRESENT AND ONLY PARTLY IMPLEMENTED
    public static final boolean FLAG_IO_EMPLOYMENT1 = true;           // flag identifying whether to project employment of principal earner as an intertemporal optimisation decision - THIS IS HARD-CODED AT PRESENT AND ONLY PARTLY IMPLEMENTED
    public static final boolean FLAG_IO_EMPLOYMENT2 = true;           // flag identifying whether to project employment of secondary earner as an intertemporal optimisation decision - THIS IS HARD-CODED AT PRESENT AND ONLY PARTLY IMPLEMENTED

    // TIME PARAMETERS
    public static final double FULLTIME_HOURS_WEEKLY = 35;            // hours per week associated with full-time work
    public static final double PARTTIME_HOURS_WEEKLY = 16;            // hours per week associated with part-time work
    public static final double MIN_WORK_HOURS_WEEKLY = 5;             // minimum hours per week to be considered working

    // DIRECTORIES
    public static String gridsOutputDirectory;                        // directory to read/write grids data
    public static String gridsInputDirectory;                         // directory to read/write grids data

    // GAUSSIAN QUADRATURE
    public static final int PTS_IN_QUADRATURE = 5;                    // number of points used to approximate expectations for normally distributed wage expectations
    public static Quadrature quadrature = new Quadrature(DecisionParams.PTS_IN_QUADRATURE);

    // AGE
    public static int maxAge;                                         // evaluation of simpaths.data.Parameters.getMaxAge() returned value 0

    // ASSUMED YEARS IN RELATIONSHIP
    public static final int DEFAULT_YEARS_MARRIED = 5;                // used by regression equation to model likelihood of separation
    public static final int DEFAULT_AGE_DIFFERENCE = 0;               // used by regression equation to model likelihood of separation

    // CONSUMPTION PARAMETERS
    public static final double MIN_CONSUMPTION_PER_YEAR = 5 * 52;              // minimum feasible consumption per year

    // LIQUID WEALTH STATE
    //public static final int PTS_LIQUID_WEALTH = 26;                   // number of discrete points used to approximate liquid wealth
    public static final int PTS_LIQUID_WEALTH_WKG = 21;
    public static final int PTS_LIQUID_WEALTH_RTD = 21;
    public static final double C_LIQUID_WEALTH = 50260.0;               // state-space summarised by logarithmic scale: w = exp(x) - c; larger c is closer to arithmetic scale
    public static double rSafeAssets;                                 // return to liquid wealth
    public static double rDebtLow;                                    // interest charge on net debt
    public static double rDebtHi;                                     // interest charge on net debt

    // FULL-TIME WAGE POTENTIAL STATE
    public static int maxAgeFlexibleLabourSupply;
    //public static final int PTS_WAGE_POTENTIAL = 26;                // number of discrete points used to approximate full-time wage potential
    public static final int PTS_WAGE_POTENTIAL = 21;
    public static final double MAX_WAGE_PHOUR = 175.0;                // maximum per hour
    public static final double MIN_WAGE_PHOUR = 1.25;                  // minimum per hour
    public static final double C_WAGE_POTENTIAL = 1.0;                // log scale adjustment (see liquid wealth above)

    // FLAGS TO RECORD OPTIONS FOR EMPLOYMENT
    public static final int MONTHS_EMPLOYED_PER_YEAR = 8;             // used to impute employment history for each year over age 18 years
    public static final double PROBABILITY_WAGE_OFFER1 = 0.97;        // probability of receiving a job offer (1 - probability of unemployment)

    // PRIVATE PENSION STATE
    //public static final int PTS_PENSION = 15;
    public static final int PTS_PENSION = 15;
    public static double maxPensionPYear;
    public static final double C_PENSION = 80705.6;                       // log scale adjustment (see liquid wealth above)


    // HEALTH STATE
    public static final int PTS_HEALTH = 5;                           // number of discrete points used to approximate health state
    public static final double MAX_HEALTH = 5.0;                      // maximum on indicator range (excellent)
    public static final double MIN_HEALTH = 1.0;                      // minimum on indicator range (poor)
    public static final Dhe DEFAULT_HEALTH = Dhe.VeryGood;

    // BIRTH COHORTS
    public static int ptsBirthYear;                                   // number of discrete points used to approximate birth years
    public static int maxBirthYear;                                // maximum on indicator range
    public static int minBirthYear;                                // minimum on indicator range

    // RETIREMENT STATE
    public static int minAgeToRetire;

    // DISABILITY STATE
    public static final Indicator DEFAULT_DISABILITY = Indicator.False;     // assumed for formulating expectations in absence of explicit value

    // FORMAL SOCIAL CARE STATE
    public static int minAgeReceiveFormalCare;

    // MAXIMUM AGE FOR COHABITATION
    public static final int MAX_AGE_COHABITATION = 115;

    // REGION STATE
    public static final int PTS_REGION = 12;                          // number of regions, starting at value 1
    public static final Region DEFAULT_REGION = Region.UKF;           // assumed for formulating expectations in absence of explicit value

    // STUDENT STATE
    public static final int PTS_STUDENT = 2;                          // number of 'types' of student, starting at 0 for non-student
    public static final Education EDUCATION_FATHER = Education.Medium;
    public static final Education EDUCATION_MOTHER = Education.Medium;

    // EDUCATION STATE
    public static final int PTS_EDUCATION = 3;                           // number of 'types' of highest education qualification attained, starting at 0 for no education; maximum is 3
    public static final Education DEFAULT_EDUCATION = Education.Medium;  // assumed for formulating expectations in absence of explicit value

    // CHILDREN STATE
    public static final int NUMBER_BIRTH_AGES = 3;                    // number of discrete ages at which a woman is assumed to be able to give
    public static final int[] BIRTH_AGE = new int[]{20, 29, 37};      // array listing discrete birth ages
    public static final int[] MAX_BIRTHS = new int[]{2, 2, 2};        // array listing the maximum number of births possible at each birth age


    /**
     *
     * METHOD TO LOAD PARAMETERS
     *
     */
    public static void loadParameters(Integer employmentOptionsOfPrincipalWorker, Integer employmentOptionsOfSecondaryWorker,
                                      boolean respondToHealth, int minAgeForPoorHealth1, boolean respondToDisability,
                                      boolean responsesToRegion, boolean responsesToEducation, boolean responsesToPension,
                                      boolean responsesToLowWageOffer, boolean respondToRetirement,
                                      boolean saveBehaviour, String readGrid, String outputDir, Integer startYearInit, Integer endYear) {

        // read/write flags
        if (!saveBehaviour) {
            saveGridSlicesToCSV = false;
            saveIntermediateSolutions = false;
        }

        // asset returns
        rSafeAssets = Parameters.getSampleAverageRate(TimeVaryingRate.RealSavingReturns) + Parameters.realInterestRateInnov;
        rDebtLow = Parameters.getSampleAverageRate(TimeVaryingRate.RealDebtCostLow) + Parameters.realInterestRateInnov;
        rDebtHi = Parameters.getSampleAverageRate(TimeVaryingRate.RealDebtCostHigh) + Parameters.realInterestRateInnov;

        // directory structure
        setGridsInputDirectory(readGrid);
        gridsOutputDirectory = outputDir + File.separator + "grids";

        // set-up behavioural parameters
        optionsEmployment1 = employmentOptionsOfPrincipalWorker;
        optionsEmployment2 = employmentOptionsOfSecondaryWorker;
        flagHealth = respondToHealth;
        flagLowWageOffer1 = responsesToLowWageOffer;
        Parameters.flagUnemployment = responsesToLowWageOffer;
        minAgeForPoorHealth = minAgeForPoorHealth1;
        flagDisability = respondToDisability;
        if (Parameters.flagSocialCare) {
            if (!flagHealth)
                throw new RuntimeException("if project behavioural responses to social care, then need to also allow for health");
        }
        flagRegion = responsesToRegion;
        flagEducation = responsesToEducation;
        if (responsesToPension || respondToRetirement)
            flagPrivatePension = true;
        else
            flagPrivatePension = false;
        flagRetirement = respondToRetirement;
        minBirthYear = startYearInit - 80;
        ptsBirthYear = 1 + (int)((endYear - 20 - minBirthYear) / 20 + 0.5);
        maxBirthYear = minBirthYear + (ptsBirthYear - 1) * (int)((endYear - 20 - DecisionParams.minBirthYear) / (DecisionParams.ptsBirthYear - 1) + 0.5);
        startYear = startYearInit;

        maxAgeFlexibleLabourSupply = Parameters.MAX_AGE_FLEXIBLE_LABOUR_SUPPLY;
        maxAge = Parameters.maxAge;
        if (!flagRetirement && flagPrivatePension)
            minAgeToRetire = Parameters.DEFAULT_AGE_TO_RETIRE;
        else
            minAgeToRetire = Parameters.MIN_AGE_TO_RETIRE;
        minAgeReceiveFormalCare = Parameters.MIN_AGE_FORMAL_SOCARE;

        Parameters.annuityRates = new AnnuityRates();
        maxPensionPYear = getMaxWealthByAge(Parameters.MAX_AGE_FLEXIBLE_LABOUR_SUPPLY) * Parameters.SHARE_OF_WEALTH_TO_ANNUITISE_AT_RETIREMENT /
                Parameters.annuityRates.getAnnuityRate(Occupancy.Couple, minBirthYear, 65);
        if (Parameters.saveImperfectTaxDBMatches)
            saveImperfectTaxDbMatches = true;
    }

    static void setGridsInputDirectory(String simName) {
        gridsInputDirectory = Parameters.WORKING_DIRECTORY + File.separator + "output" + File.separator + simName + File.separator + "grids";
    }

    public static double getMinWealthByAge(int age) {

        int AGE1 = 35, AGE2 = 55, AGE3 = 70;
        double peakDebt = 35000.00, startDebt = 20000.00;
        if (-peakDebt + C_LIQUID_WEALTH < 0.0)
            throw new RuntimeException("minimum liquid wealth must be greater than -" + C_LIQUID_WEALTH);
        if (age <= AGE1) {
            return -startDebt - (peakDebt - startDebt) * (double)(age - Parameters.AGE_TO_BECOME_RESPONSIBLE) / (double)(AGE1 - Parameters.AGE_TO_BECOME_RESPONSIBLE);
        } else if (age <= AGE2) {
            return -peakDebt;
        } else if (age < AGE3) {
            return -peakDebt * (double)(AGE3 - age) / (double)(AGE3 - AGE2);
        } else {
            return 0.0;
        }
    }

    public static double getMaxWealthByAge(int age) {

        int AGE1 = 35, AGE2 = 65, AGE3 = 80;
        double value = 3000000.0;
        if (false) {
            // experimental code

            if ( age > Parameters.AGE_TO_BECOME_RESPONSIBLE ) {

                for (int aa=Parameters.AGE_TO_BECOME_RESPONSIBLE; aa<=age; aa++) {

                    double income;
                    if (age < AGE3) {

                        income = (rSafeAssets * value + DecisionParams.MAX_WAGE_PHOUR * FULLTIME_HOURS_WEEKLY * Parameters.WEEKS_PER_YEAR) * 0.7;
                        if (aa < AGE1) {
                            income = income * 0.15;
                        } else if (age <= AGE2) {
                            income = income * 0.25;
                        } else {
                            income = income * 0.10;
                        }
                    } else {

                        income = 0.0;
                    }
                    value = value + income;
                }
            }
        }
        return value;
    }

    public static int maxAgeForDisability() {
        int val = DecisionParams.maxAgeFlexibleLabourSupply;
        if (Parameters.flagSocialCare && val >= DecisionParams.minAgeReceiveFormalCare)
            val = DecisionParams.minAgeReceiveFormalCare - 1;
        return val;
    }
}
