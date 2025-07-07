// define package
package simpaths.model;

// import Java packages
import java.io.*;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.random.RandomGenerator;

// import plug-in packages
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.Transient;
import microsim.data.MultiKeyCoefficientMap;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import simpaths.data.IEvaluation;
import simpaths.data.MahalanobisDistance;
import simpaths.data.RootSearch;
import simpaths.data.startingpop.Processed;
import simpaths.experiment.SimPathsCollector;
import simpaths.model.decisions.DecisionParams;
import microsim.alignment.outcome.ResamplingAlignment;
import microsim.event.*;
import microsim.event.EventListener;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.apache.commons.lang3.time.StopWatch;

// import JAS-mine packages
import microsim.alignment.outcome.AlignmentOutcomeClosure;
import microsim.annotation.GUIparameter;
import microsim.data.db.DatabaseUtils;
import microsim.engine.AbstractSimulationManager;
import microsim.engine.SimulationEngine;

// import LABOURsim packages
import simpaths.data.Parameters;
import simpaths.model.decisions.ManagerPopulateGrids;
import simpaths.model.enums.*;
import simpaths.model.taxes.DonorTaxUnit;
import simpaths.model.taxes.DonorTaxUnitPolicy;
import simpaths.model.taxes.Match;
import simpaths.model.taxes.Matches;
import simpaths.model.taxes.database.TaxDonorDataParser;


/**
 *
 * CLASS TO MANAGE SIMULATION PROJECTIONS
 *
 */
public class SimPathsModel extends AbstractSimulationManager implements EventListener {

    public boolean isFirstRun() {
        return isFirstRun;
    }

    public void setFirstRun(boolean firstRun) {
        isFirstRun = firstRun;
    }

    private boolean isFirstRun = true;		// set default to true - this is required to support single run simulations

    // default simulation parameters
    private static Logger log = Logger.getLogger(SimPathsModel.class);

    //@GUIparameter(description = "Country to be simulated")
    private Country country;

    private boolean flagUpdateCountry = false;  // set to true if switch between countries

    @GUIparameter(description = "Simulated population size (base year)")
    private Integer popSize = 170000;

    @GUIparameter(description = "Simulation first year [valid range 2011-2019]")
    private Integer startYear = 2019;

    @GUIparameter(description = "Simulation ends at year")
    private Integer endYear = 2040;

    @GUIparameter(description = "Maximum simulated age")
    private Integer maxAge = 81;

    //@GUIparameter(description = "Fix year used in the regressions to one specified below")
    private boolean fixTimeTrend = true;

    @GUIparameter(description = "Fix year used in the regressions to")
    private Integer timeTrendStopsIn = 2022;

    private Integer timeTrendStopsInMonetaryProcesses = 2022; // For monetary processes, time trend always continues to 2017 (last observed year in the estimation sample) and then values are grown at the growth rate read from Excel

//	@GUIparameter(description="Age at which people in initial population who are not employed are forced to retire")
//	private Integer ageNonWorkPeopleRetire = 65;	//The problem is that it is difficult to find donor benefitUnits for non-zero labour supply for older people who are in the Nonwork category but not Retired.  They should, in theory, still enter the Labour Market Module, but if we cannot find donor benefitUnits, how should we proceed?  We avoid this problem by defining that people over the age specified here are retired off if they have activity_status equal to Nonwork.

//	@GUIparameter(description="Minimum age for males to retire")
//	private Integer minRetireAgeMales = 45;
//
//	@GUIparameter(description="Maximum age for males to retire")
//	private Integer maxRetireAgeMales = 75;
//
//	@GUIparameter(description="Minimum age for females to retire")
//	private Integer minRetireAgeFemales = 45;
//
//	@GUIparameter(description="Maximum age for females to retire")
//	private Integer maxRetireAgeFemales = 75;

    @GUIparameter(description = "Fix random seed?")
    private Boolean fixRandomSeed = true;

    @GUIparameter(description = "If random seed is fixed, set to this number")
    private Long randomSeedIfFixed = 606L;

    @GUIparameter(description = "Time window in years for (in)security index calculation")
    private Integer sIndexTimeWindow = 5;

    @GUIparameter(description = "Value of risk aversion parameter (alpha)")
    private Double sIndexAlpha = 2.;

    @GUIparameter(description = "Value of discount factor (delta)")
    private Double sIndexDelta = 0.98;

    //The data comes from here: https://data.oecd.org/hha/household-savings.htm
    @GUIparameter(description = "Value of saving rate (s). The default value is based on average % of household disposable income saved between 2000 - 2019 reported by the OECD")
    private Double savingRate = 0.056;

    private double interestRateInnov = 0.0;			// used to explore behavioural sensitivity to assumed interest rates (intertemporal elasticity of substitution)

    private double disposableIncomeFromLabourInnov = 0.0;		// used to explore behavioural sensitivity to disposable income (Marshallian labour supply elasticity)

//	@GUIparameter(description = "Force recreation of input database based on the data provided by the population_[country].csv file")
//	private boolean refreshInputDatabase = false;		//Tables can be constructed in GUI dialog in launch, before JAS-mine GUI appears.  However, if skipping that, and manually altering the EUROMODpolicySchedule.xlsx file, this will need to be set to true to build new input database before simulation is run (though the new input database will only be viewable in the output/input/input.h2.db file).

//	@GUIparameter(description = "If true, set initial earnings from data in input population, otherwise, set using the wage equation regression estimates")
    private boolean initialisePotentialEarningsFromDatabase = true;

    //	@GUIparameter(description = "If unchecked, will expand population and not use weights")
    private boolean useWeights = false;

    private boolean ignoreTargetsAtPopulationLoad = false;

    @GUIparameter(description = "If unchecked, will use the standard matching method")
//	private boolean useSBAMMatching = false;
    private UnionMatchingMethod unionMatchingMethod = UnionMatchingMethod.ParametricNoRegion;

    @GUIparameter(description = "tick to project mortality based on gender, age, and year specific probabilities")
    private boolean projectMortality = true;

    private boolean alignPopulation = true; //TODO: routine fails to replicate results for minor variations between simulations

    //	@GUIparameter(description = "If checked, will align fertility")
    private boolean alignFertility = true;
    private boolean alignRetirement = false;

    private boolean alignDisability = true;
    private boolean alignEducation = false; //Set to true to align level of education

    private boolean alignInSchool = false; //Set to true to align share of students among 16-29 age group

    private boolean alignCohabitation = true; //Set to true to align share of couples (cohabiting individuals)

    private boolean alignEmployment = true; //Set to true to align employment share

    public boolean addRegressionStochasticComponent = true; //If set to true, and regression contains ResStanDev variable, will evaluate the regression score including stochastic part, and omits the stochastic component otherwise.

    public boolean fixRegressionStochasticComponent = false; // If true, only draw stochastic component once and use the same value throughout the simulation. Currently applies to wage equations.

    public boolean commentsOn = true;

    public boolean debugCommentsOn = true;

    public boolean donorFinderCommentsOn = true;

    @GUIparameter(description = "If checked, will use Covid-19 labour supply module")
    public boolean labourMarketCovid19On = false; // Set to true to use reduced-form labour market module for years affected by Covid-19 (2020, 2021)

    @GUIparameter(description = "Simulate formal childcare costs")
    public boolean projectFormalChildcare = false;

    @GUIparameter(description = "Average over donor pool when imputing transfer payments")
    public boolean donorPoolAveraging = false;

    private int ordering = Parameters.MODEL_ORDERING;    //Used in Scheduling of model events.  Schedule model events at the same time as the collector and observer events, but a lower order, so will be fired before the collector and observer have updated.

    private Set<Person> persons;

    //For marriage matching - types based on region and gender:
    //private Map<Gender, LinkedHashMap<Region, Double>> marriageTargetsGenderRegion;
    private MultiKeyMap<Object, Double> marriageTargetsByGenderAndRegion;

    private LinkedHashMap<String, Double>  marriageTargetsByKey;

    private long elapsedTime0;

    private long timerStartSim;

    private int year;

    private Set<BenefitUnit> benefitUnits;

    private Set<Household> households;

    private Map<Gender, LinkedHashMap<Region, Set<Person>>> personsToMatch;

    private LinkedHashMap<String, Set<Person>> personsToMatch2;

    private double scalingFactor;

    private Map<Long, Double> initialHoursWorkedWeekly;

    private LabourMarket labourMarket;

    public int tmpPeopleAssigned = 0;

    public int lowEd = 0;
    public int medEd = 0;
    public int highEd = 0;
    public int nothing = 0;

    Map<String, Double> policyNameIncomeMedianMap = new LinkedHashMap<>(); // Initialise a <String, Double> map to store names of policies and median incomes

    private Tests tests;

    @Transient
    SimPathsCollector collector;

    EventGroup firstYearSched = new EventGroup();
    EventGroup yearlySchedule = new EventGroup();

    @GUIparameter(description = "tick to project social care")
    private boolean projectSocialCare = false;

    private boolean flagSuppressChildcareCosts = true;

    private boolean flagSuppressSocialCareCosts = true;

    @GUIparameter(description = "tick to enable intertemporal optimised consumption and labour decisions")
    private boolean enableIntertemporalOptimisations = false;

    @GUIparameter(description = "tick to use behavioural solutions saved by a previous simulation")
    private boolean useSavedBehaviour = false;

    @GUIparameter(description = "simulation name to read in grids from:")
    private String readGrid = "test1";

    // flag to project population using time series average statistics (dampens temporal variation)
    private boolean flagDefaultToTimeSeriesAverages = false;

    //	@GUIparameter(description = "tick to save behavioural solutions assumed for simulation")
    private boolean saveBehaviour = true;

    // save imperfect tax database matches to potentially expand input database
    private boolean saveImperfectTaxDBMatches = false;

    //	@GUIparameter(description = "the number of employment options from which a household's principal wage earner can choose")
    private Integer employmentOptionsOfPrincipalWorker = 3;

    //	@GUIparameter(description = "the number of employment options from which a household's secondary wage earner can choose")
    private Integer employmentOptionsOfSecondaryWorker = 3;

    @GUIparameter(description = "whether to include student and education status in state space for IO behavioural solutions")
    private boolean responsesToEducation = true;

    @GUIparameter(description = "whether to include private pensions in the state space for IO behavioural solutions")
    private boolean responsesToPension = false;

    @GUIparameter(description = "whether to include low wage offers (unemployment) in the state space for IO behavioural solutions")
    private boolean responsesToLowWageOffer = true;

    @GUIparameter(description = "whether to include retirement (and private pensions) in the state space for IO behavioural solutions")
    private boolean responsesToRetirement = false;

    @GUIparameter(description = "whether to include health in state space for IO behavioural solutions")
    private boolean responsesToHealth = true;

    @GUIparameter(description = "whether to include disability in state space for IO behavioural solutions")
    private boolean responsesToDisability = true;

    @GUIparameter(description = "minimum age for expecting less than perfect health in IO solutions")
    private Integer minAgeForPoorHealth = 45;

    @GUIparameter(description = "whether to include geographic region in state space for IO behavioural solutions")
    private boolean responsesToRegion = false;

    // Controls for macro shocks
    @GUIparameter(description = "macro shock: population")
    private MacroScenarioPopulation macroShockPopulation = MacroScenarioPopulation.Baseline;

    @GUIparameter(description = "macro shock: productivity")
    private MacroScenarioProductivity macroShockProductivity = MacroScenarioProductivity.Baseline;

    @GUIparameter(description = "macro shock: green policy")
    private MacroScenarioGreenPolicy macroShockGreenPolicy = MacroScenarioGreenPolicy.No;

    @GUIparameter(description = "macro shocks: on")
    private boolean macroShocksOn = false;

    RandomGenerator cohabitInnov;
    Random initialiseInnov1;
    Random initialiseInnov2;
    Random popAlignInnov;
    Random educationInnov;


    /**
     *
     * CONSTRUCTOR FOR SIMULATION PROJECTIONS
     * @param country
     * @param startYear
     *
     */
    public SimPathsModel(Country country, int startYear) {
        super();
        this.country = country;
        this.startYear = startYear;
    }

    public SimPathsModel(Country country) {
        super();
        this.country = country;
    }


    /**
     *
     * METHOD TO BUILD THE MODEL SO THAT IT CAN BE EXECUTED
     *
     * This method is launched by JAS-mine when you press the
     * 'Build simulation model' button of the GUI
     *
     */
    @Override
    public void buildObjects() {


        // time check
        elapsedTime0 = System.currentTimeMillis();
        timerStartSim = elapsedTime0;

        // set seed for random number generator
        if (fixRandomSeed) SimulationEngine.getRnd().setSeed(randomSeedIfFixed);
        cohabitInnov = new Random(SimulationEngine.getRnd().nextLong());
        initialiseInnov1 = new Random(SimulationEngine.getRnd().nextLong());
        initialiseInnov2 = new Random(SimulationEngine.getRnd().nextLong());
        educationInnov = new Random(SimulationEngine.getRnd().nextLong());
        popAlignInnov = new Random(SimulationEngine.getRnd().nextLong());

        // load model parameters
        Parameters.loadParameters(country, maxAge, enableIntertemporalOptimisations, projectFormalChildcare,
                projectSocialCare, donorPoolAveraging, fixTimeTrend, flagDefaultToTimeSeriesAverages, saveImperfectTaxDBMatches,
                timeTrendStopsIn, startYear, endYear, interestRateInnov, disposableIncomeFromLabourInnov, flagSuppressChildcareCosts,
                flagSuppressSocialCareCosts, macroShockPopulation, macroShockProductivity, macroShockGreenPolicy, macroShocksOn);
        if (enableIntertemporalOptimisations) {

            alignEmployment = false;
            DecisionParams.loadParameters(employmentOptionsOfPrincipalWorker, employmentOptionsOfSecondaryWorker,
                    responsesToHealth, minAgeForPoorHealth, responsesToDisability, responsesToRegion, responsesToEducation,
                    responsesToPension, responsesToLowWageOffer, responsesToRetirement, saveBehaviour,
                    readGrid, getEngine().getCurrentExperiment().getOutputFolder(), startYear, endYear);
        }
        long elapsedTime1 = System.currentTimeMillis();
        System.out.println("Time to load parameters: " + (elapsedTime1 - elapsedTime0)/1000. + " seconds.");
        elapsedTime0 = elapsedTime1;

        // populate tax donor references
        if (flagUpdateCountry) {
            taxDatabaseUpdate();
            TaxDonorDataParser.populateDonorTaxUnitTables(country, false); // Populate tax unit donor tables from person data
        }
        populateTaxdbReferences();
        //TestTaxRoutine.run();
        elapsedTime1 = System.currentTimeMillis();
        System.out.println("Time to load tax database references: " + (elapsedTime1 - elapsedTime0)/1000. + " seconds.");
        elapsedTime0 = elapsedTime1;

        // set start year for simulation
        year = startYear;
        // EUROMODpolicyNameForThisYear = Parameters.getEUROMODpolicyForThisYear(year);

        //Display current country and start year in the console
        System.out.println("Country: " + country + ". Running simulation from: " + startYear + " to " + endYear);

        // creates initial population (Person and BenefitUnit objects) based on data in input database.
        // Note that the population may be cropped to simulate a smaller population depending on user choices in the GUI.
        createInitialPopulationDataStructures();
        elapsedTime1 = System.currentTimeMillis();
        System.out.println("Time to create initial population structures: " + (elapsedTime1 - elapsedTime0)/1000. + " seconds.");
        elapsedTime0 = elapsedTime1;

        // initialise variables used to match marriage unions
        createDataStructuresForMarriageMatching();

        // earnings potential
        labourMarket = new LabourMarket(benefitUnits);
        if (!initialisePotentialEarningsFromDatabase) initialisePotentialEarningsByWageEquationAndEmployerSocialInsurance();

        // calculate the scaling factor for population alignment
        double popSizeBaseYear = 0;
        for (Gender gender : Gender.values()) {

            for (Region region : Parameters.getCountryRegions()) {

                for (int age = 0; age < maxAge; age++) {

                    popSizeBaseYear += Parameters.getPopulationProjections(gender, region, age, year);
                }
            }
        }
        scalingFactor = (double)popSizeBaseYear / (double)persons.size();
        System.out.println("Scaling factor is " + scalingFactor);

        //Set up tests class
        tests = new Tests();

        // save current simulation parameters
        saveRunParameters();

        // finalise
        elapsedTime1 = System.currentTimeMillis();
        log.debug("Time to build objects: " + (elapsedTime1 - timerStartSim)/1000. + " seconds.");
        System.out.println("Time to complete initialisation " + (System.currentTimeMillis() - timerStartSim)/1000.0/60.0 + " minutes.");
        elapsedTime0 = elapsedTime1;
    }


    /**
     *
     * METHOD TO PROJECT THE POPULATION THROUGH TIME
     *
     * This method is run once for each simulated year after
     * the simulation is launched by JAS-mine when you press the
     * 'Start simulation' button of the GUI. This method defines
     * the order that simulated processes are executed. There are
     * three key categories of processes:
     *   Processes	 			These are processes applicable to the simulation population in aggregate
     *	 Person.Processes		Defined in Person Class of model package. These modules define processes specific to simulated 'individuals'
     *	 BenefitUnit.Processes	Defined in BenefitUnit Class of model package. These modules define processes specific to simulated 'benefitUnits'
     *
     * All processes are initialised as 'Events' within the JAS-mine simulation engine
     *
     * First year involves fewer processes than subsequent years, as many of the simulated characteristics are described by the input data
     * Characteristics simulated in the first year limited to control variables for the utility maximisation problem and states affected by
     * control variables (time use, transfer system, consumption/savings, and mental health 2 modules).
     * First year also allows for alignment routines
     *
     */
    @Override
    public void buildSchedule() {

        addEventToAllYears(Processes.StartYear);

        addEventToAllYears(Processes.UpdateParameters);
        addEventToAllYears(Processes.GarbageCollection);
        addCollectionEventToAllYears(benefitUnits, BenefitUnit.Processes.Update);
        addCollectionEventToAllYears(persons, Person.Processes.Update);

        yearlySchedule.addCollectionEvent(persons, Person.Processes.Aging);

        // Health Alignment - redrawing alignment used adjust state of individuals to projections by Gender and Age
        //Turned off for now as health determined below based on individual characteristics
        //yearlySchedule.addEvent(this, Processes.HealthAlignment);
        //yearlySchedule.addEvent(this, Processes.CheckForEmptyHouseholds);

        // Check whether persons have reached retirement Age
        yearlySchedule.addEvent(this, Processes.RetirementAlignment);
        yearlySchedule.addCollectionEvent(persons, Person.Processes.ConsiderRetirement, false);

        // EDUCATION MODULE
        // Check In School - check whether still in education, and if leaving school, reset Education Level
        yearlySchedule.addCollectionEvent(persons, Person.Processes.InSchool);

        // In School alignment
        yearlySchedule.addEvent(this, Processes.InSchoolAlignment);
        yearlySchedule.addCollectionEvent(persons, Person.Processes.LeavingSchool);

        // Align the level of education if required
        yearlySchedule.addEvent(this, Processes.EducationLevelAlignment);

        // Homeownership status
        yearlySchedule.addCollectionEvent(benefitUnits, BenefitUnit.Processes.Homeownership);

        // HEALTH MODULE
        // Update Health - determine health (continuous) based on regression models: done here because health depends on education
        yearlySchedule.addEvent(this, Processes.DisabilityAlignment);
        yearlySchedule.addCollectionEvent(persons, Person.Processes.Health);

        // HOUSEHOLD COMPOSITION MODULE: Decide whether to enter into a union (marry / cohabit), and then perform union matching (marriage) between a male and female

        // Update potential earnings so that as up to date as possible to decide partner in union matching.
        yearlySchedule.addCollectionEvent(persons, Person.Processes.UpdatePotentialHourlyEarnings);

        // Consider whether in consensual union (cohabiting)
        yearlySchedule.addEvent(this, Processes.CohabitationAlignment);
        yearlySchedule.addCollectionEvent(persons, Person.Processes.Cohabitation);

        // partnership variation
        yearlySchedule.addCollectionEvent(persons, Person.Processes.PartnershipDissolution);
        yearlySchedule.addEvent(this, Processes.UnionMatching);
        //yearlySchedule.addEvent(this, Processes.CheckForEmptyHouseholds);
        //yearlySchedule.addEvent(this, Processes.Timer);

        // Fertility
        yearlySchedule.addEvent(this, Processes.FertilityAlignment);        //Align to fertility rates implied by projected population statistics.
        yearlySchedule.addCollectionEvent(persons, Person.Processes.Fertility);
        yearlySchedule.addCollectionEvent(persons, Person.Processes.GiveBirth, false);        //Cannot use read-only collection schedule as newborn children cause concurrent modification exception.  Need to specify false in last argument of Collection event.

        // TIME USE MODULE
        addEventToAllYears(Processes.LabourMarketAndIncomeUpdate);

        // Assign benefit status to individuals in benefit units, from donors. Based on donor tax unit status.
        addCollectionEventToAllYears(benefitUnits, BenefitUnit.Processes.ReceivesBenefits);

        // CONSUMPTION AND SAVINGS MODULE
        addCollectionEventToAllYears(persons, Person.Processes.ProjectEquivConsumption);

        // equivalised disposable income
        addCollectionEventToAllYears(benefitUnits, BenefitUnit.Processes.CalculateChangeInEDI);

        // mortality (migration) and population alignment at year's end
        addCollectionEventToAllYears(persons, Person.Processes.ConsiderMortality);
        addEventToAllYears(Processes.PopulationAlignment);

        // END OF YEAR PROCESSES
        addEventToAllYears(Processes.CheckForImperfectTaxDBMatches);
        addEventToAllYears(tests, Tests.Processes.RunTests); //Run tests
        addCollectionEventToAllYears(persons, Person.Processes.UpdateOutputVariables); // Update idPartner, dhhtp_c4
        addCollectionEventToAllYears(benefitUnits, BenefitUnit.Processes.UpdateOutputVariables); // Update dhhtp_c4
        addEventToAllYears(Processes.EndYear);

        // UPDATE YEAR
        addEventToAllYears(Processes.UpdateYear);

        // UPDATE EVENT QUEUE
        getEngine().getEventQueue().scheduleOnce(firstYearSched, startYear, ordering);
        getEngine().getEventQueue().scheduleRepeat(yearlySchedule, startYear+1, ordering, 1.);

        // at termination of simulation
        int orderEarlier = -1;            //Set less than order so that this is called before the yearlySchedule in the endYear.
        SystemEvent end = new SystemEvent(SimulationEngine.getInstance(), SystemEventType.End);
        getEngine().getEventQueue().scheduleOnce(end, endYear+1, orderEarlier);

        log.debug("Time to build schedule " + (System.currentTimeMillis() - elapsedTime0)/1000. + " seconds.");
        elapsedTime0 = System.currentTimeMillis();
    }
    private void addEventToAllYears(Tests tt, Enum ee) {
        firstYearSched.addEvent(tt, ee);
        yearlySchedule.addEvent(tt, ee);
    }
    private void addEventToAllYears(SimPathsCollector cc, Enum ee) {
        firstYearSched.addEvent(cc, ee);
        yearlySchedule.addEvent(cc, ee);
    }
    void addEventToAllYears(Enum ee) {

        firstYearSched.addEvent(this, ee);
        yearlySchedule.addEvent(this, ee);
    }
    private void addCollectionEventToAllYears(Set set, Enum ee, boolean readOnly) {

        firstYearSched.addCollectionEvent(set, ee, readOnly);
        yearlySchedule.addCollectionEvent(set, ee, readOnly);
    }
    private void addCollectionEventToAllYears(Set set, Enum ee) {

        firstYearSched.addCollectionEvent(set, ee);
        yearlySchedule.addCollectionEvent(set, ee);
    }

    private void saveRunParameters() {

        String filePath = DatabaseUtils.databaseInputUrl;
        filePath = filePath.substring(0, filePath.length()-5) + "options.txt";
        try ( FileWriter fw = new FileWriter(filePath, true);
              BufferedWriter bw = new BufferedWriter(fw);
              PrintWriter pw = new PrintWriter(bw)
        ) {

            String line;
            line = "---------------------------------------------------";
            pw.println(line);
            line = "country: " + country;
            pw.println(line);
            line = "startYear: " + startYear;
            pw.println(line);
            line = "endYear: " + endYear;
            pw.println(line);
            line = "popSize: " + popSize;
            pw.println(line);
            line = "maxAge: " + maxAge;
            pw.println(line);
            line = "fixTimeTrend: " + fixTimeTrend;
            pw.println(line);
            line = "timeTrendStopsIn: " + timeTrendStopsIn;
            pw.println(line);
            line = "timeTrendStopsInMonetaryProcesses: " + timeTrendStopsInMonetaryProcesses;
            pw.println(line);
            line = "flagDefaultToTimeSeriesAverages: " + flagDefaultToTimeSeriesAverages;
            pw.println(line);
            line = "fixRandomSeed: " + fixRandomSeed;
            pw.println(line);
            line = "randomSeedIfFixed: " + randomSeedIfFixed;
            pw.println(line);
            line = "sIndexAlpha: " + sIndexAlpha;
            pw.println(line);
            line = "sIndexDelta: " + sIndexDelta;
            pw.println(line);
            line = "savingRate: " + savingRate;
            pw.println(line);
            line = "addRegressionStochasticComponent: " + addRegressionStochasticComponent;
            pw.println(line);
            line = "fixRegressionStochasticComponent: " + fixRegressionStochasticComponent;
            pw.println(line);
            line = "commentsOn: " + commentsOn;
            pw.println(line);
            line = "debugCommentsOn: " + debugCommentsOn;
            pw.println(line);
            line = "donorFinderCommentsOn: " + donorFinderCommentsOn;
            pw.println(line);
            line = "labourMarketCovid19On: " + labourMarketCovid19On;
            pw.println(line);
            line = "projectFormalChildcare: " + projectFormalChildcare;
            pw.println(line);
            line = "donorPoolAveraging: " + donorPoolAveraging;
            pw.println(line);
            line = "initialisePotentialEarningsFromDatabase: " + initialisePotentialEarningsFromDatabase;
            pw.println(line);
            line = "useWeights: " + useWeights;
            pw.println(line);
            line = "projectMortality: " + projectMortality;
            pw.println(line);
            line = "alignPopulation: " + alignPopulation;
            pw.println(line);
            line = "alignFertility: " + alignFertility;
            pw.println(line);
            line = "alignEducation: " + alignEducation;
            pw.println(line);
            line = "alignInSchool: " + alignInSchool;
            pw.println(line);
            line = "alignCohabitation: " + alignCohabitation;
            pw.println(line);
            line = "alignEmployment: " + alignEmployment;
            pw.println(line);
            line = "saveImperfectTaxDBMatches: " + saveImperfectTaxDBMatches;
            pw.println(line);
            line = "enableIntertemporalOptimisations: " + enableIntertemporalOptimisations;
            pw.println(line);
            line = "useSavedBehaviour: " + useSavedBehaviour;
            pw.println(line);
            line = "readGrid: " + readGrid;
            pw.println(line);
            line = "saveBehaviour: " + saveBehaviour;
            pw.println(line);
            line = "employmentOptionsOfPrincipalWorker: " + employmentOptionsOfPrincipalWorker;
            pw.println(line);
            line = "employmentOptionsOfSecondaryWorker: " + employmentOptionsOfSecondaryWorker;
            pw.println(line);
            line = "responsesToLowWageOffer: " + responsesToLowWageOffer;
            pw.println(line);
            line = "responsesToEducation: " + responsesToEducation;
            pw.println(line);
            line = "responsesToHealth: " + responsesToHealth;
            pw.println(line);
            line = "minAgeForPoorHealth: " + minAgeForPoorHealth;
            pw.println(line);
            line = "responsesToDisability: " + responsesToDisability;
            pw.println(line);
            line = "projectSocialCare: " + projectSocialCare;
            pw.println(line);
            line = "flagSuppressChildcareCosts: " + flagSuppressChildcareCosts;
            pw.println(line);
            line = "flagSuppressSocialCareCosts: " + flagSuppressSocialCareCosts;
            pw.println(line);
            line = "responsesToRegion: " + responsesToRegion;
            pw.println(line);
            line = "responsesToPension: " + responsesToPension;
            pw.println(line);
            line = "responsesToRetirement: " + responsesToRetirement;
            pw.println(line);
            line = "interestRateInnov: " + interestRateInnov;
            pw.println(line);
            line = "disposableIncomeInnov: " + disposableIncomeFromLabourInnov;
            pw.println(line);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    /**
     *
     * METHOD DEFINING PROCESSES APPLICABLE TO THE SIMULATED POPULATION IN AGGREGATE
     *
     */
    public enum Processes {

        StartYear,
        EndYear,
        UnionMatching,
        LabourMarketAndIncomeUpdate,
        SocialCareMarketClearing,

        //Alignment Processes
        FertilityAlignment,
        PopulationAlignment,
        CohabitationAlignment,
        RetirementAlignment,
        DisabilityAlignment,
        // HealthAlignment,
        InSchoolAlignment,
        EducationLevelAlignment,

        //Other processes
        Timer,
        UpdateParameters,
        RationalOptimisation,
        UpdateYear,
        CheckForEmptyBenefitUnits,
        GarbageCollection,
        CheckForImperfectTaxDBMatches,
    }

    @Override
    public void onEvent(Enum<?> type) {
        switch ((Processes) type) {
            case StartYear -> {

                elapsedTime0 = System.currentTimeMillis();
                System.out.println("Starting year " + year);
                if (commentsOn) log.info("Starting year " + year);
            }
            case EndYear -> {
                //System.out.println("Model assigned " + lowEd + "low education levels, " + medEd + "medium, " + highEd + "high ed");
                long elapsedTime1 = System.currentTimeMillis();
                double timerForYear = (elapsedTime1 - elapsedTime0)/1000.0;
                System.out.println("Finished year " + year + " (in " + timerForYear + " seconds)");
                if (commentsOn) log.info("Finished year " + year + " (in " + timerForYear + " seconds)");
                elapsedTime0 = elapsedTime1;
            }
            case PopulationAlignment -> {

                if (alignPopulation) {

                    if ( year <= Parameters.getPopulationProjectionsMaxYear() ) {

                        if (useWeights) {
                            populationAlignmentWeighted();
                        } else {
                            populationAlignmentUnweighted();
                        }
                        if (commentsOn) log.info("Population alignment complete.");
                    } else {

                        if (commentsOn) log.info("Population alignment skipped as simulated year exceeds period covered by population projections.");
                    }
                }
            }
            case CohabitationAlignment -> {

                if (alignCohabitation) {
                    partnershipAlignment();
                    if (commentsOn) log.info("Cohabitation alignment complete.");
                }
                clearPersonsToMatch();
            }
            case RetirementAlignment -> {
                if (alignRetirement) {
                    retirementAlignment();
                    if (commentsOn) log.info("Retirement alignment complete.");
                }
            }
            case DisabilityAlignment -> {
                if (alignDisability) {
                    disabilityAlignment();
                    if (commentsOn) log.info("Disability alignment complete.");
                }
            }
//			case HealthAlignment -> {
//				healthAlignment();
//				if (commentsOn) log.info("Health alignment complete.");
//			}
            case UnionMatching -> {

                if (UnionMatchingMethod.Parametric.equals(unionMatchingMethod)) {
                    unionMatching(false);
                } else {
                    unionMatching(false);
                    unionMatchingNoRegion(false); //Run matching again relaxing regions this time
                }
                if (commentsOn) log.info("Union matching complete.");
            }
            case SocialCareMarketClearing -> {
                socialCareMarketClearing();
            }
            case FertilityAlignment -> {

                if (alignFertility) {
                    fertilityAlignment(); //Then align to meet the numbers implied by population projections by region
                    if (commentsOn) log.info("Fertility alignment complete.");
                }
            }
            case InSchoolAlignment -> {

                if (alignInSchool) {
                    inSchoolAlignment();
                    System.out.println("Proportion of students will be aligned.");
                }
            }
            case EducationLevelAlignment -> {

                if (alignEducation) {
                    educationLevelAlignment();
                    System.out.println("Education levels will be aligned.");
                }
            }
            case LabourMarketAndIncomeUpdate -> {

                labourMarket.update(year);
                if (commentsOn) log.info("Labour market update complete.");
            }
            case Timer -> {
                printElapsedTime();
            }
            case RationalOptimisation -> {
                Parameters.grids = ManagerPopulateGrids.run(this, useSavedBehaviour, saveBehaviour);
            }
            case UpdateParameters -> {
                updateParameters();
                if (commentsOn) log.info("Update Parameters Complete.");
            }
            case UpdateYear -> {

                if (commentsOn) log.info("It's New Year's Eve of " + year);
                System.out.println("It's New Year's Eve of " + year);
                if (year==endYear) {

                    double timerForSim = (System.currentTimeMillis() - timerStartSim)/1000.0/60.0;
                    System.out.println("Finished simulating population in " + timerForSim + " minutes");
                    if (commentsOn) log.info("Finished simulating population in " + timerForSim + " minutes");
                }
                year++;
            }
            case GarbageCollection -> {
                screenForExitingObjects();
            }
            case CheckForImperfectTaxDBMatches -> {

                if (Parameters.saveImperfectTaxDBMatches) {
                    screenForImperfectTaxDbMatches();
                }
            }
            default -> {
                throw new RuntimeException("failed to identify process type in SimPathsModel.onEvent");
            }
        }
    }


    /**
     *
     * METHODS IMPLEMENTING PROCESS LEVEL COMPUTATIONS
     *
     */


    private void screenForExitingObjects() {

        // screen for persons exiting the sample
        persons.removeIf(person -> (!SampleExit.NotYet.equals(person.getSampleExit())));
        for (BenefitUnit benefitUnit: benefitUnits) {
            benefitUnit.getMembers().removeIf(person -> !persons.contains(person));
        }

        // screen for empty benefit units
        benefitUnits.removeIf(benefitUnit -> (benefitUnit.getMale()==null && benefitUnit.getFemale()==null));
        for (Household household: households) {
            household.getBenefitUnits().removeIf(benefitUnit -> !benefitUnits.contains(benefitUnit));
        }

        // screen for empty households
        households.removeIf(household -> household.getBenefitUnits().isEmpty());

        // screen for benefit units not associated with a valid household
        benefitUnits.removeIf(benefitUnit -> (!households.contains(benefitUnit.getHousehold())));

        // screen for persons not associated with a valid benefit unit
        persons.removeIf(person -> (!benefitUnits.contains(person.getBenefitUnit())));

        // screen for residual problems
        for (Person person : persons) {
            if (!benefitUnits.contains(person.getBenefitUnit()))
                throw new RuntimeException("person included in model in benefit unit that is not included in model");
        }
        for (BenefitUnit benefitUnit: benefitUnits) {
            if (!households.contains(benefitUnit.getHousehold()))
                throw new RuntimeException("benefit unit included in model in household that is not included in model");
            if (benefitUnit.getMale()==null && benefitUnit.getFemale()==null)
                throw new RuntimeException("problem screening out benefit units with no responsible adults");
            int male = 0;
            int female = 0;
            for (Person person: benefitUnit.getMembers()) {
                if (!person.getBenefitUnit().equals(benefitUnit))
                    throw new RuntimeException("inconsistent linkages between benefit units and members");
                if (person.getDag()>=Parameters.AGE_TO_BECOME_RESPONSIBLE) {
                    if (Gender.Male.equals(person.getDgn()))
                        male++;
                    else
                        female++;
                }
            }
            if (male>1)
                throw new RuntimeException("more than one mature male in benefit unit");
            if (female>1)
                throw new RuntimeException("more than one mature female in benefit unit");
            if (male+female<1)
                throw new RuntimeException("no mature adults in benefit unit");
        }
        for (Household household: households) {
            for (BenefitUnit benefitUnit : household.getBenefitUnits()) {
                if (!benefitUnit.getHousehold().equals(household))
                    throw new RuntimeException("inconsistent linkages between households and benefit units");
            }
        }
    }

    private void screenForImperfectTaxDbMatches() {

        Matches imperfectMatches = new Matches();
        for (BenefitUnit benefitUnit: benefitUnits) {

            Match match = benefitUnit.getTaxDbMatch();
            if (match==null)
                throw new RuntimeException("failed to identify tax database match");
            if (match.getMatchCriterion()>Parameters.IMPERFECT_THRESHOLD) {
                imperfectMatches.addMatch(match);
            }
        }
        if (!imperfectMatches.isEmpty()) {
            String dir = getEngine().getCurrentExperiment().getOutputFolder() + File.separator + "csv";
            imperfectMatches.write(dir, "poor_taxmatch_year_" + year + ".csv");
        }
    }


    /**
     *
     * PROCESS - POPULATION ALIGNMENT WHERE POPULATION IS WEIGHTED
     *
     */
    private void populationAlignmentWeighted() {

        int maxAlignAge = Math.min(maxAge, Parameters.getPopulationProjectionsMaxAge());
        MultiKeyMap<Object, Double> weightsByGenderRegionAndAge = MultiKeyMap.multiKeyMap(new LinkedMap<>());
        MultiKeyMap<Object, List<Person>> personsByGenderRegionAndAge = personsByAlignmentGroupInit(maxAlignAge);

        //Calculate Weights
        for (Gender gender : Gender.values()) {
            for (Region region: Parameters.getCountryRegions()) {
                for (int age = 0; age <= maxAlignAge; age++) {
                    double weight = 0.0;
                    for ( Person person : personsByGenderRegionAndAge.get(gender, region, age) ) {
                        weight += person.getWeight();
                    }
                    weight = Parameters.getPopulationProjections(gender, region, age, year) / weight;
                    weightsByGenderRegionAndAge.put(gender, region, age, weight);
                }
            }
        }

        //Re-weight simulation
        for (Person person : persons) {
            if (SampleExit.NotYet.equals(person.getSampleExit())) {

                Gender gender = person.getDgn();
                Region region = person.getRegion();
                int age = Math.min(person.getDag(), maxAlignAge);
                double weight = ((Number)weightsByGenderRegionAndAge.get(gender, region, age)).doubleValue();
                person.setWeight(weight);
            }
        }
    }


    /**************************************************************
     *
     * PROCESS - POPULATION ALIGNMENT WHERE POPULATION IS UNWEIGHTED
     *
     * Alignment starts with youngest age and loops to oldest age
     * Method here implicitly attributes alignment to 2 processes:
     * 	1) domestic migration (between regions)
     * 	2) international migration (between countries) and death
     * Migratory flows managed at benefit unit level, and reference age of youngest member of each benefit unit
     * Process 1 exhausted when there are no domestic regions requiring opposing flows
     * Process 2 assumes:
     * 		international immigration if an increase in population number is required
     * 		international emigration if age is less than threshold (Parameters.ALIGN_MIN_AGE_ASSUME_DEATH) and
     * 			at least one benefit has a youngest member of the consiered age
     * 		death otherwise
     *
     **************************************************************/
    private void populationAlignmentUnweighted() {

        int maxAlignAge = Math.min(maxAge, Parameters.getPopulationProjectionsMaxAge());
        MultiKeyMap<Object, List<Person>> personsByAlignmentGroup = personsByAlignmentGroupInit(maxAlignAge);
        MultiKeyMap<Object, List<Person>> migrantPoolByAlignmentGroup = migrantPoolByAlignmentGroupInit(maxAlignAge, personsByAlignmentGroup);

        //Align to targets
        for (int age = 0; age <= maxAlignAge; age++) {

            for (Gender gender : Gender.values()) {

                boolean flagSufficientMigrants = populationAlignmentDomesticMigration(age, gender, maxAlignAge, personsByAlignmentGroup, migrantPoolByAlignmentGroup);
                populationAlignmentResidual(age, gender, maxAlignAge, personsByAlignmentGroup, flagSufficientMigrants, migrantPoolByAlignmentGroup);
            }
        }
    }


    /**************************************************************
     *
     * METHOD TO ORGANISE SIMULATED POPULATION INTO SUBSETS FOR ALIGNMENT
     *
     * @param maxAlignAge upper age for population alignment (lesser of maximum simulation age and age of supplied population projections)
     * @return Mapping of subsets and population maps
     *
     **************************************************************/
    private MultiKeyMap<Object, List<Person>> personsByAlignmentGroupInit(int maxAlignAge) {

        MultiKeyMap<Object, List<Person>> personsByAlignmentGroup = MultiKeyMap.multiKeyMap(new LinkedMap<>());

        //identify simulated personsByGenderRegionAndAge
        for (Gender gender : Gender.values()) {
            for (Region region: Parameters.getCountryRegions()) {
                for (int age = 0; age <= maxAlignAge; age++) {
                    personsByAlignmentGroup.put(gender, region, age, new LinkedList<>());
                }
            }
        }
        for (Person person : persons) {

            if (SampleExit.NotYet.equals(person.getSampleExit())) {

                int age = Math.min(person.getDag(), maxAlignAge);
                List<Person> listHere = personsByAlignmentGroup.get(person.getDgn(), person.getRegion(), age);
                if (listHere==null)
                    throw new RuntimeException("failed to identify requested person alignment list");
                listHere.add(person);
            }
        }

        // order subgroup lists by id
        // NOTE: lists are used to ensure that simulated populations are replicable
        for (Gender gender : Gender.values()) {
            for (Region region : Parameters.getCountryRegions()) {
                for (int age = 0; age <= maxAlignAge; age++) {
                    Collections.sort(personsByAlignmentGroup.get(gender, region, age));
                    Collections.shuffle(personsByAlignmentGroup.get(gender, region, age), popAlignInnov);
                }
            }
        }

        return personsByAlignmentGroup;
    }


    /**************************************************************
     *
     * METHOD TO ORGANISE SIMULATED POPULATION OF POTENTIAL MIGRANTS INTO SUBGROUPS FOR POPULATION ALIGNMENT
     * 	as for personsByAlignmentGroup, but limited to population of individuals who are the exclusive youngest member of
     * 	their respective benefit units and are in households comprised of a single benefit unit
     *
     **************************************************************/
    private MultiKeyMap<Object, List<Person>> migrantPoolByAlignmentGroupInit(int maxAlignAge, MultiKeyMap<Object,
            List<Person>> personsByAlignmentGroup) {

        MultiKeyMap<Object, List<Person>> migrantsByAlignmentGroup;

        //identify simulated personsByGenderRegionAndAge
        migrantsByAlignmentGroup = MultiKeyMap.multiKeyMap(new LinkedMap<>());
        for (Gender gender : Gender.values()) {
            for (Region region: Parameters.getCountryRegions()) {
                for (int age = 0; age <= maxAlignAge; age++) {
                    migrantsByAlignmentGroup.put(gender, region, age, new LinkedList<>());
                    List<Person> personsInGroup = personsByAlignmentGroup.get(gender, region, age);
                    for (Person person : personsInGroup) {

                        boolean flagAccept = true;
                        if ( person.getBenefitUnit().getHousehold().getBenefitUnits().size() != 1) {
                            flagAccept = false;
                        } else {
                            for (Person member : person.getBenefitUnit().getMembers()) {
                                if ( member.getDag() <= person.getDag() && member != person ) flagAccept = false;
                            }
                        }
                        if (flagAccept) migrantsByAlignmentGroup.get(gender, region, age).add(person);
                    }
                }
            }
        }

        return migrantsByAlignmentGroup;
    }


    /*********************************************
     *
     * Method that uses domestic migration to adjust for population alignment
     *
     * @param age the currently considered age (in annual years)
     * @param gender the currently considered gender
     * @param maxAlignAge the upper age bound considered for population alignment
     * @param personsByAlignmentGroup a mapping organising current simulated population by gender region and age
     * @param migrantPoolByAlignmentGroup as for personsByAlignment group, but limited to youngest benefit unit members
     *
     *********************************************/
    private boolean populationAlignmentDomesticMigration(int age, Gender gender, int maxAlignAge,
                                                         MultiKeyMap<Object, List<Person>> personsByAlignmentGroup,
                                                         MultiKeyMap<Object, List<Person>> migrantPoolByAlignmentGroup) {

        boolean flagSufficientMigrants = true;

        // identify to and from regions
        List<Region> immigrantRegions = new LinkedList<>();
        List<Region> regions = new LinkedList<>(Parameters.getCountryRegions());
        Collections.sort(regions);
        List<Person> emigrantPool = new LinkedList<>();
        for (Region region : regions) {

            int targetNumber = (int) Math.round(Parameters.getPopulationProjections(gender, region, age, year) / scalingFactor);
            int simulatedNumber = personsByAlignmentGroup.get(gender, region, age).size();
            if (targetNumber > simulatedNumber) {

                for(int ii=0; ii<targetNumber-simulatedNumber; ii++) {
                    immigrantRegions.add(region);
                }
            }
            if (targetNumber < simulatedNumber) {

                for (Person person : migrantPoolByAlignmentGroup.get(gender, region, age)) {
                    emigrantPool.add(person);
                    simulatedNumber--;
                    if (targetNumber == simulatedNumber) break;
                }
                if (targetNumber < simulatedNumber) flagSufficientMigrants = false;
            }
        }
        Collections.shuffle(emigrantPool, popAlignInnov);
        Collections.shuffle(immigrantRegions, popAlignInnov);
        Iterator<Region> immigrantRegionsIterator = immigrantRegions.iterator();
        Iterator<Person> emigrantPoolIterator = emigrantPool.iterator();
        while ( immigrantRegionsIterator.hasNext() && emigrantPoolIterator.hasNext() ) {

            // simulate domestic migration
            Person migrant = emigrantPoolIterator.next();
            BenefitUnit migrantBU = migrant.getBenefitUnit();
            Set<Person> migrants = migrantBU.getMembers();
            Region fromRegion = migrantBU.getRegion();
            Region toRegion = immigrantRegionsIterator.next();
            migrantBU.setRegion(toRegion);

            // update working references
            for (Person person : migrants) {

                if (SampleExit.NotYet.equals(person.getSampleExit())) {

                    int ageHere = Math.min(person.getDag(), maxAlignAge);
                    Gender genderHere = person.getDgn();

                    personsByAlignmentGroup.get(genderHere, fromRegion, ageHere).remove(person);
                    personsByAlignmentGroup.get(genderHere, toRegion, ageHere).add(person);
                    if (ageHere == age) {

                        migrantPoolByAlignmentGroup.get(genderHere, fromRegion, age).remove(person);
                        migrantPoolByAlignmentGroup.get(genderHere, toRegion, age).add(person);
                    }
                }
            }

            // update counters and references
            emigrantPoolIterator.remove();
            immigrantRegionsIterator.remove();
        }

        return flagSufficientMigrants;
    }


    /*********************************************
     *
     * Method that uses international migration and death to adjust for population alignment
     *
     * @param age the currently considered age (in annual years)
     * @param gender the currently considered gender
     * @param personsByAlignmentGroup a mapping organising current simulated population by gender region and age
     * @param flagSufficientMigrants flag to indicate that internal emigration in all regions covered by available pools
     * @param migrantPoolByAlignmentGroup as for personsByAlignmentGroup, but limited to youngest benefit unit members
     *
     *********************************************/
    private void populationAlignmentResidual(int age, Gender gender, int maxAlignAge,
                                             MultiKeyMap<Object, List<Person>> personsByAlignmentGroup,
                                             boolean flagSufficientMigrants,
                                             MultiKeyMap<Object, List<Person>> migrantPoolByAlignmentGroup) {

        int indicatorError = 0;
        List<Region> regions = new LinkedList<>(Parameters.getCountryRegions());
        Collections.sort(regions);
        for (Region region : regions) {

            int targetNumber = (int) Math.round(Parameters.getPopulationProjections(gender, region, age, year) / scalingFactor);
            int simulatedNumber = personsByAlignmentGroup.get(gender, region, age).size();
            if (targetNumber < simulatedNumber) {
                // emigration or death

                // consistency checks
                if (indicatorError == 0) indicatorError = -1;
                if (flagSufficientMigrants && indicatorError == 1) {
                    throw new RuntimeException("Inconsistent treatment of population alignment across regions");
                }

                if (age < Parameters.ALIGN_MIN_AGE_ASSUME_DEATH) {
                    // simulate emigration

                    Iterator<Person> migrantPoolByAlignmentGroupIterator = migrantPoolByAlignmentGroup.get(gender, region, age).iterator();
                    while (targetNumber < simulatedNumber && migrantPoolByAlignmentGroupIterator.hasNext()) {

                        Person emigrant = migrantPoolByAlignmentGroupIterator.next();
                        BenefitUnit emigrantBU = emigrant.getBenefitUnit();
                        Set<Person> emigrants = emigrantBU.getMembers();

                        // update working references
                        for (Person person : emigrants) {

                            if (SampleExit.NotYet.equals(person.getSampleExit())) {

                                int ageHere = Math.min(person.getDag(), maxAlignAge);
                                Gender genderHere = person.getDgn();
                                personsByAlignmentGroup.get(genderHere, region, ageHere).remove(person);
                                if (ageHere == age && person!=emigrant) {
                                    migrantPoolByAlignmentGroup.get(genderHere, region, age).remove(person);
                                }
                                person.setSampleExit(SampleExit.EmigrationAlignment);
                            }
                        }
                        migrantPoolByAlignmentGroupIterator.remove();
                        simulatedNumber --;
                    }
                }
                Iterator<Person> personIterator = personsByAlignmentGroup.get(gender, region, age).iterator();
                while (targetNumber < simulatedNumber && personIterator.hasNext()) {
                    // simulate death - may be of any age

                    Person candidate = personIterator.next();
                    BenefitUnit benefitUnit = candidate.getBenefitUnit();
                    if ( benefitUnit == null ) {
                        throw new RuntimeException("Missing benefit unit for candidate to kill in population alignment.");
                    }
                    if ( (!candidate.equals(benefitUnit.getMale()) && !candidate.equals(benefitUnit.getFemale())) ||
                        benefitUnit.getOccupancy().equals(Occupancy.Couple) || benefitUnit.getMembers().size() == 1 ) {
                        // death of person should not affect existence of any other person in model

                        candidate.setSampleExit(SampleExit.DeathAlignment);

                        // update counters and references
                        personIterator.remove();
                        if (migrantPoolByAlignmentGroup.get(gender, region,age).contains(candidate)) {
                            migrantPoolByAlignmentGroup.get(gender, region,age).remove(candidate);
                        }
                        simulatedNumber --;
                    }
                }
            }
            if (targetNumber > simulatedNumber) {
                // simulate international immigration

                // consistency checks
                if (indicatorError == 0) indicatorError = 1;
                if (flagSufficientMigrants && indicatorError == -1) {
                    throw new RuntimeException("Inconsistent treatment of population alignment across regions");
                }

                List<Person> migrantPool = migrantPoolByAlignmentGroup.get(gender, region, age);
                for (Region region1 : regions) {
                    if (region1 != region) {
                        migrantPool.addAll(migrantPoolByAlignmentGroup.get(gender, region1, age));
                    }
                }
                Iterator<Person> migrantPoolIterator = migrantPool.iterator();
                while (targetNumber>simulatedNumber && migrantPoolIterator.hasNext()) {

                    Person immigrant = migrantPoolIterator.next();
                    Household newHousehold = new Household();
                    households.add(newHousehold);
                    BenefitUnit immigrantBU = cloneBenefitUnit(immigrant.getBenefitUnit(), newHousehold, SampleEntry.ImmigrationAlignment);
                    immigrantBU.setRegion(region);

                    // update counters and references
                    for (Person person : immigrantBU.getMembers()) {

                        int ageHere = Math.min(person.getDag(), maxAlignAge);
                        Gender genderHere = person.getDgn();
                        personsByAlignmentGroup.get(genderHere, region, ageHere).add(person);
                    }
                    simulatedNumber ++;
                }
            }
        }
    }


    /**********************************************************
     *
     * METHOD TO CLONE EXISTING HOUSEHOLD AND ADD TO SIMULATED POPULATION
     *
     *********************************************************/
    private void cloneHousehold(Household originalHousehold, SampleEntry sampleEntry) {

        Household newHousehold = new Household(originalHousehold, sampleEntry);
        households.add(newHousehold);
        if (originalHousehold.getBenefitUnits().isEmpty())
            throw new RuntimeException("problem identifying household benefit units to clone");
        for (BenefitUnit originalBenefitUnit : originalHousehold.getBenefitUnits()) {
            cloneBenefitUnit(originalBenefitUnit, newHousehold, sampleEntry);
        }
        newHousehold.resetWeights(1.0d);
    }


    /**********************************************************
     *
     * METHOD TO CLONE EXISTING BENEFIT UNIT AND ADD TO SIMULATED POPULATION
     *
     *********************************************************/
    private BenefitUnit cloneBenefitUnit(BenefitUnit originalBenefitUnit, Household newHousehold, SampleEntry sampleEntry) {

        // initialise objects

        double seed0 = SimulationEngine.getRnd().nextDouble();
        long seed = (SampleEntry.ProcessedInputData.equals(sampleEntry)) ? originalBenefitUnit.getSeed() : (long)(seed0*100000);

        BenefitUnit newBenefitUnit = new BenefitUnit(originalBenefitUnit, seed, sampleEntry);
        newBenefitUnit.setHousehold(newHousehold);
        benefitUnits.add(newBenefitUnit);

        if (originalBenefitUnit.getMembers().isEmpty())
            throw new RuntimeException("problem identifying benefit unit members to clone");
        Set<Person> originalPersons = originalBenefitUnit.getMembers();
        for (Person originalPerson : originalPersons) {

            seed0 = SimulationEngine.getRnd().nextDouble();
            seed = (SampleEntry.ProcessedInputData.equals(sampleEntry)) ? originalPerson.getSeed() : (long)(seed0*100000);
            Person newPerson = new Person(originalPerson, seed, sampleEntry);
            newPerson.setBenefitUnit(newBenefitUnit);
            persons.add(newPerson);
        }

        // final clean-up
        for (Person person : newBenefitUnit.getMembers()) {
            person.cloneCleanup();
        }

        return newBenefitUnit;
    }


    /**
     *
     * PROCESS - UNION MATCHING OF SIMULATED POPULATION
     * Matching Based On Earning Potential differential AND age differential
     * (option C in Lia & Matteo's document 'BenefitUnit formation')
     *
     */
    List<Pair<Person,Person>> matches = new ArrayList<>();


    /**
     *
     * @param alignmentRun If true, real unions will not be formed. Instead, flags will be set for individual to indicate those who would have formed a union.
     *
     */
    protected void unionMatching(boolean alignmentRun) {

        matches.clear();
        for (Region region : Parameters.getCountryRegions()) {

            Set<Person> unmatchedMales = new LinkedHashSet<>();
            Set<Person> unmatchedFemales = new LinkedHashSet<>();
            unmatchedMales.addAll(personsToMatch.get(Gender.Male).get(region));
            unmatchedFemales.addAll(personsToMatch.get(Gender.Female).get(region));
            Pair<Set<Person>, Set<Person>> unmatched = new Pair<>(unmatchedMales, unmatchedFemales);

            evalMatches(unmatched, alignmentRun);
        }
    }

    protected void unionMatchingNoRegion(boolean alignmentRun) {

        Set<Person> unmatchedMales = new LinkedHashSet<>();
        Set<Person> unmatchedFemales = new LinkedHashSet<>();
        for (Region region : Parameters.getCountryRegions()) {
            unmatchedMales.addAll(personsToMatch.get(Gender.Male).get(region));
            unmatchedFemales.addAll(personsToMatch.get(Gender.Female).get(region));
        }
        Pair<Set<Person>, Set<Person>> unmatched = new Pair<>(unmatchedMales, unmatchedFemales);

        evalMatches(unmatched, alignmentRun);

        if (!alignmentRun) {
            if (commentsOn) log.debug("Marriage matched");
        }
    }

    private void evalMatches(Pair<Set<Person>, Set<Person>> unmatched, boolean alignmentRun) {

        if (!unmatched.getFirst().isEmpty() && !unmatched.getSecond().isEmpty()) {

            UnionMatching unionMatching = new UnionMatching(unmatched, alignmentRun);
            unionMatching.evaluate("GM");
            List<Pair<Person,Person>> matchesHere = unionMatching.getMatches();
            for (Pair<Person,Person> match : matchesHere) {
                Person male = match.getFirst();
                Person female = match.getSecond();
                personsToMatch.get(male.getDgn()).get(male.getRegion()).remove(male);
                for (Region region : Parameters.getCountryRegions()) {
                    personsToMatch.get(female.getDgn()).get(region).remove(female);
                }
            }
            matches.addAll(matchesHere);
        }
    }

    private void socialCareMarketClearing() {

        // adjust provision so that aggregate provision broadly matches aggregate receipt
        double careProvisionAdjustment = Parameters.getTimeSeriesValue(getYear(),TimeSeriesVariable.CareProvisionAdjustment);
        SocialCareAlignment socialCareAlignment = new SocialCareAlignment(persons, careProvisionAdjustment);
        double[] startVal = new double[] {careProvisionAdjustment};
        double[] lowerBound = new double[] {careProvisionAdjustment - 1.5};
        double[] upperBound = new double[] {careProvisionAdjustment + 1.5};
        RootSearch search = new RootSearch(lowerBound, upperBound, startVal, socialCareAlignment, 1.0E-2, 0.001);
        search.evaluate();
        if (search.isTargetAltered()) {
            Parameters.putTimeSeriesValue(getYear(), search.getTarget()[0], TimeSeriesVariable.CareProvisionAdjustment);
        }
    }

    public void retirementAlignment() {
        RetirementAlignment retirementAlignment = new RetirementAlignment(persons);
        double retirementAdjustment = Parameters.getTimeSeriesValue(getYear(), TimeSeriesVariable.RetirementAdjustment);
        RootSearch search = getRootSearch(retirementAdjustment, retirementAlignment, 1.0E-2, 1.0E-2, 10); // epsOrdinates and epsFunction determine the stopping condition for the search. For retirementAlignment error term is the difference between target and observed share of partnered individuals.

        // update and exit
        if (search.isTargetAltered()) {
            Parameters.putTimeSeriesValue(getYear(), search.getTarget()[0], TimeSeriesVariable.RetirementAdjustment); // If adjustment is altered from the initial value, update the map
            System.out.println("Retirement adjustment value was " + search.getTarget()[0]);
        }
    }

    public void disabilityAlignment() {
        DisabilityAlignment disabilityAlignment = new DisabilityAlignment(persons);
        double disabilityAdjustment = Parameters.getTimeSeriesValue(getYear(), TimeSeriesVariable.DisabilityAdjustment);
        RootSearch search = getRootSearch(disabilityAdjustment, disabilityAlignment, 5.0E-3, 5.0E-3, 2);

        // update and exit
        if (search.isTargetAltered()) {
            Parameters.putTimeSeriesValue(getYear(), search.getTarget()[0], TimeSeriesVariable.DisabilityAdjustment); // If adjustment is altered from the initial value, update the map
            System.out.println("Disability adjustment value was " + search.getTarget()[0]);
        }
    }

    public void activityAlignmentMacroShock() {
        Map<OccupancyExtended, MultiKeyCoefficientMap> coefficientMaps = new HashMap<>();
        coefficientMaps.put(OccupancyExtended.Single_Male, Parameters.getCoeffLabourSupplyUtilityMales());
        coefficientMaps.put(OccupancyExtended.Single_Female, Parameters.getCoeffLabourSupplyUtilityFemales());
        coefficientMaps.put(OccupancyExtended.Couple, Parameters.getCoeffLabourSupplyUtilityCouples());
        coefficientMaps.put(OccupancyExtended.Male_AC, Parameters.getCoeffLabourSupplyUtilityACMales());
        coefficientMaps.put(OccupancyExtended.Female_AC, Parameters.getCoeffLabourSupplyUtilityACFemales());
        coefficientMaps.put(OccupancyExtended.Male_With_Dependent, Parameters.getCoeffLabourSupplyUtilityMalesWithDependent());
        coefficientMaps.put(OccupancyExtended.Female_With_Dependent, Parameters.getCoeffLabourSupplyUtilityFemalesWithDependent());

        Map<OccupancyExtended, List<String>> regressorsToModify = new HashMap<>();
        regressorsToModify.put(OccupancyExtended.Single_Male, List.of("Hrs_40plus_Male"));
        regressorsToModify.put(OccupancyExtended.Single_Female, List.of("Hrs_40plus_Female"));
        regressorsToModify.put(OccupancyExtended.Couple, List.of("Hrs_40plus_Male", "Hrs_40plus_Female"));
        regressorsToModify.put(OccupancyExtended.Male_AC, List.of("Hrs_40plus_Male"));
        regressorsToModify.put(OccupancyExtended.Female_AC, List.of("Hrs_40plus_Female"));
        regressorsToModify.put(OccupancyExtended.Male_With_Dependent, List.of("Hrs_40plus_Male"));
        regressorsToModify.put(OccupancyExtended.Female_With_Dependent, List.of("Hrs_40plus_Female"));

        double initialUtilityAdjustment = Parameters.getTimeSeriesValue(getYear(), TimeSeriesVariable.UtilityAdjustment);

        ActivityAlignmentMacroShock activityAlignment = new ActivityAlignmentMacroShock(
                persons, benefitUnits, coefficientMaps, regressorsToModify, initialUtilityAdjustment
        );

        RootSearch search = getRootSearch(initialUtilityAdjustment, activityAlignment, 1.0E-2, 1.0E-2, Parameters.MAX_EMPLOYMENT_ALIGNMENT);

        if (search.isTargetAltered()) {
            double newAdjustment = search.getTarget()[0];
            Parameters.putTimeSeriesValue(getYear(), newAdjustment, TimeSeriesVariable.UtilityAdjustment);
            System.out.println("Utility adjustment for all types was " + newAdjustment);
        }
    }

    /*
    Private helper method used to set up alignment for different occupancy types
     */
    private void activityAlignment(
            TimeSeriesVariable adjustmentMap, // map storing adjustment values used in the alignment process
            MultiKeyCoefficientMap coefficientMap, // map storing original labour supply utility regression coefficients
            String[] regressionCoefficientName, // name of regression coefficient to adjust
            OccupancyExtended occupancy, // benefit unit occupancy extended to allow all types used in labour supply module
            String occupancyLabel // displays the type of benefit unit to which adjustment is applied
    ) {
        double utilityAdjustment = Parameters.getTimeSeriesValue(getYear(), adjustmentMap);
        ActivityAlignmentV2 activityAlignment = new ActivityAlignmentV2(benefitUnits, coefficientMap, regressionCoefficientName, occupancy);
        RootSearch search = getRootSearch(utilityAdjustment, activityAlignment, 1.0E-2, 1.0E-2, Parameters.MAX_EMPLOYMENT_ALIGNMENT);
        if (search.isTargetAltered()) {
            Parameters.putTimeSeriesValue(getYear(), search.getTarget()[0], adjustmentMap);
            System.out.println("Utility adjustment for " + occupancyLabel + " was " + search.getTarget()[0]);
        }
    }

    public void activityAlignmentSingleMales() {
        activityAlignment(
                TimeSeriesVariable.UtilityAdjustmentSingleMales,
                Parameters.getCoeffLabourSupplyUtilityMales(),
                new String[]{"Hrs_40plus_Male"},
                OccupancyExtended.Single_Male,
                "single males"
        );
    }

    public void activityAlignmentSingleACMales() {
        activityAlignment(
                TimeSeriesVariable.UtilityAdjustmentACMales,
                Parameters.getCoeffLabourSupplyUtilityACMales(),
                new String[]{"Hrs_40plus_Male"},
                OccupancyExtended.Male_AC,
                "single AC males"
        );
    }

    public void activityAlignmentSingleACFemales() {
        activityAlignment(
                TimeSeriesVariable.UtilityAdjustmentACFemales,
                Parameters.getCoeffLabourSupplyUtilityACFemales(),
                new String[]{"Hrs_40plus_Female"},
                OccupancyExtended.Female_AC,
                "single AC females"
        );
    }

    public void activityAlignmentSingleFemales() {
        activityAlignment(
                TimeSeriesVariable.UtilityAdjustmentSingleFemales,
                Parameters.getCoeffLabourSupplyUtilityFemales(),
                new String[]{"Hrs_40plus_Female"},
                OccupancyExtended.Single_Female,
                "single females"
        );
    }

    public void activityAlignmentCouples() {
        activityAlignment(
                TimeSeriesVariable.UtilityAdjustmentCouples,
                Parameters.getCoeffLabourSupplyUtilityCouples(),
                new String[]{"Hrs_40plus_Male","Hrs_40plus_Female"},
                OccupancyExtended.Couple,
                "couples"
        );
    }

    public void activityAlignmentMaleWithDependents() {
        activityAlignment(
                TimeSeriesVariable.UtilityAdjustmentMaleWithDep,
                Parameters.getCoeffLabourSupplyUtilityMalesWithDependent(),
                new String[]{"Hrs_40plus_Male"},
                OccupancyExtended.Male_With_Dependent,
                "males with dependents"
        );
    }

    public void activityAlignmentFemaleWithDependents() {
        activityAlignment(
                TimeSeriesVariable.UtilityAdjustmentFemaleWithDep,
                Parameters.getCoeffLabourSupplyUtilityFemalesWithDependent(),
                new String[]{"Hrs_40plus_Female"},
                OccupancyExtended.Female_With_Dependent,
                "females with dependents"
        );
    }


    private void partnershipAlignment() {

        // Instantiate alignment object
        PartnershipAlignment partnershipAlignment = new PartnershipAlignment(persons);

        // define limits of search algorithm
        double partnershipAdjustment = Parameters.getTimeSeriesValue(getYear(), TimeSeriesVariable.PartnershipAdjustment);
        double minVal = Math.max(-4.0, - partnershipAdjustment - 4.0);
        double maxVal = Math.min(4.0, - partnershipAdjustment + 4.0);

        // run search
        RootSearch search = getRootSearch(0.0, minVal, maxVal, partnershipAlignment, 5.0E-3, 5.0E-3); // epsOrdinates and epsFunction determine the stopping condition for the search. For partnershipAlignment error term is the difference between target and observed share of partnered individuals.

        // check result
        //double val = partnershipAlignment.evaluate(search.getTarget());

        // update and exit
        if (search.isTargetAltered()) {
            Parameters.setAlignmentValue(getYear(), search.getTarget()[0], AlignmentVariable.PartnershipAlignment); // If adjustment is altered from the initial value, update the map
            System.out.println("Partnership adjustment value was " + search.getTarget()[0]);
        }
    }

    @NotNull
    private static RootSearch getRootSearch(double initialAdjustment, IEvaluation alignmentClass, double epsOrdinates, double epsFunction, double modifier) {
        double minVal = initialAdjustment - modifier;
        double maxVal = initialAdjustment + modifier;
        return getRootSearch(initialAdjustment, minVal, maxVal, alignmentClass, epsOrdinates, epsFunction);
    }

    @NotNull
    private static RootSearch getRootSearch(double initialAdjustment, double minVal, double maxVal, IEvaluation alignmentClass, double epsOrdinates, double epsFunction) {
        double[] startVal = new double[] {initialAdjustment}; // Starting values for the adjustment
        double[] lowerBound = new double[] {minVal};
        double[] upperBound = new double[] {maxVal};
        RootSearch search = new RootSearch(lowerBound, upperBound, startVal, alignmentClass, epsOrdinates, epsFunction);
        search.evaluate();
        return search;
    }


    /**
     *
     * PROCESS - ALIGN THE SHARE OF COHABITING INDIVIDUALS IN THE SIMULATED POPULATION
     *
     */
    private void considerCohabitationAlignment() {

        //Create a list of individuals who are allowed to enter a partnership for whom alignment should be performed
        int numPersonsWhoCanBePartnered = 0;
        int numPersonsToBePartnered = 0;
        ArrayList<Person> personsWhoCanBePartnered = new ArrayList<>();
        for (Person person : persons) {
            if (person.getDag() >= Parameters.MIN_AGE_COHABITATION && person.getPartner() == null) {
                numPersonsWhoCanBePartnered++;
                personsWhoCanBePartnered.add(person);
                if (person.isToBePartnered()) {
                    numPersonsToBePartnered++;
                }
            }
        }

        int targetNumberToBePartnered = (int) ( (double) numPersonsWhoCanBePartnered * 0.3); // - numPersonsToBePartnered;

        if ((targetNumberToBePartnered - numPersonsToBePartnered) > 0) {
            new ResamplingAlignment<Person>().align(
                    personsWhoCanBePartnered,
                    null,
                    new AlignmentOutcomeClosure<Person>() {
                        @Override
                        public boolean getOutcome(Person agent) {
                            return agent.isToBePartnered();
                        }

                        @Override
                        public void resample(Person agent) {
                            agent.setToBePartnered(true);
                            personsToMatch.get(agent.getDgn()).get(agent.getBenefitUnit().getRegion()).add(agent);
                        }
                    },
                    targetNumberToBePartnered);
        }
    }


    /**
     *
     * PROCESS - ALIGN THE SHARE OF STUDENTS IN THE SIMULATED POPULATION
     *
     */
    private void inSchoolAlignment() {

        InSchoolAlignment inSchoolAlignment = new InSchoolAlignment(persons);
        double inSchoolAdjustment = Parameters.getTimeSeriesValue(getYear(), TimeSeriesVariable.InSchoolAdjustment);
        RootSearch search = getRootSearch(inSchoolAdjustment, inSchoolAlignment, 1.0E-2, 1.0E-2, 4); // epsOrdinates and epsFunction determine the stopping condition for the search. For inSchoolAlignment error term is the difference between target and observed share of partnered individuals.

        // update and exit
        if (search.isTargetAltered()) {
            Parameters.putTimeSeriesValue(getYear(), search.getTarget()[0], TimeSeriesVariable.InSchoolAdjustment); // If adjustment is altered from the initial value, update the map
            System.out.println("InSchool adjustment value was " + search.getTarget()[0]);
        }
    }


    /**
     *
     * PROCESS - EDUCATION LEVEL ALIGNMENT OF SIMULATED POPULATION
     *
     */

    private void educationLevelAlignment() {

        HashMap<Gender, ArrayList<Person>> personsLeavingEducation = new HashMap<Gender, ArrayList<Person>>();
        for(Gender gender : Gender.values()) {
            personsLeavingEducation.put(gender, new ArrayList<Person>());
        }


        for(Person person : persons) {
            if(person.isToLeaveSchool()) {
                personsLeavingEducation.get(person.getDgn()).add(person);
            }
        }

        for(Gender gender : Gender.values()) {

            //Check pre-aligned population for education level statistics
            int numPersonsOfThisGenderWithLowEduPreAlignment = 0, numPersonsOfThisGenderWithHighEduPreAlignment = 0, numPersonsOfThisGender = 0;
            for(Person person : persons) {
                if( person.getDgn().equals(gender) && person.getDag() >= 16 && person.getDag() <= 45) {        //Alignment projections are based only on persons younger than 66 years old
                    if (person.isToLeaveSchool()) { //Align only people leaving school?
                        if(person.getDeh_c3() != null) {
                            if (person.getDeh_c3().equals(Education.Low)) {
                                numPersonsOfThisGenderWithLowEduPreAlignment++;
                            } else if (person.getDeh_c3().equals(Education.High)) {
                                numPersonsOfThisGenderWithHighEduPreAlignment++;
                            }
                            numPersonsOfThisGender++;
                        }
                    }
                }
            }

            //Calculate alignment targets
            //High Education
            double highEducationRateTarget = Parameters.getTimeSeriesValue(year, gender.toString(), TimeSeriesVariable.HighEducationRate);
            int numPersonsWithHighEduAlignmentTarget = (int) (highEducationRateTarget * (double)numPersonsOfThisGender);
            //Medium Education
            double lowEducationRateTarget = Parameters.getTimeSeriesValue(year, gender.toString(), TimeSeriesVariable.LowEducationRate);
//			int numPersonsWithLowEduAlignmentTarget = (int) (proportionInitialPopWithMediumEdu * numPersonsOfThisGender);		//Based on initial population - this ensures that proportion of medium educated people can never decrease below initial values
            int numPersonsWithLowEduAlignmentTarget = (int) (lowEducationRateTarget * (double)numPersonsOfThisGender);
            if(Parameters.systemOut) {
                System.out.println("Gender " + gender + ", highEduRateTarget, " + highEducationRateTarget + ", lowEduRateTarget, " + lowEducationRateTarget);
            }
            //Sort the list of school leavers by age
            Collections.shuffle(personsLeavingEducation.get(gender), educationInnov);        //To remove any source of bias in borderline cases because the first subset of school leavers of same age are assigned a higher education level.  (I.e. if education level is deemed to be associated with age, so that higher ages are assigned higher education levels, then if the boundary between high and medium education levels is e.g. at the people aged 27, the first few people aged 27 will be assigned a high education level and the rest will have medium (or low) education levels.  To avoid any sort of regularity in the iteration order of school leavers, we shuffle here.
            Collections.sort(personsLeavingEducation.get(gender),
                    (Comparator<Person>) (arg0, arg1) -> {
                        return arg1.getDag() - arg0.getDag();    //Sort school leavers by descending order in age
                    });

            //Perform alignment
            int countHigh = 0, countLow = 0;
            for(Person schoolLeaver : personsLeavingEducation.get(gender)) {        //This tries to maintain the naturally generated number of school-leavers with medium education, so that an increase in the number of school-leavers with high education is achieved through a reduction in the number of school-leavers with low education.  However, in the event that the number of school-leavers with either high or medium education are more than the total number of school leavers (in this year), we end up having no school leavers with low education and we have to reduce the number of school leavers with medium education

                if (schoolLeaver.getDeh_c3().equals(Education.Medium)) {
                    if(numPersonsOfThisGenderWithHighEduPreAlignment + countHigh < numPersonsWithHighEduAlignmentTarget) {                //Only align if number of people in population with high education is too low.
                        schoolLeaver.setEducation(Education.High);            //As the personsLeavingEducation list is sorted by descending age, the oldest people leaving education are assigned to have high education levels
                        countHigh++;
                    } else if(numPersonsOfThisGenderWithLowEduPreAlignment + countLow< numPersonsWithLowEduAlignmentTarget) {
                        schoolLeaver.setEducation(Education.Low);        //When the number of high education level people have been assigned, the next oldest people are assigned to have medium education levels
                        countLow++;
                    }
                } else if (schoolLeaver.getDeh_c3().equals(Education.High)) {
                    if (numPersonsOfThisGenderWithHighEduPreAlignment + countHigh > numPersonsWithHighEduAlignmentTarget) { //If too many people with high education
                        schoolLeaver.setEducation(Education.Medium);
                        countHigh--;
                    }
                } else if (schoolLeaver.getDeh_c3().equals(Education.Low)) {
                    if (numPersonsOfThisGenderWithLowEduPreAlignment + countLow > numPersonsWithLowEduAlignmentTarget) {
                        schoolLeaver.setEducation(Education.Medium);
                        countLow--;
                    }
                }

//				System.out.println(schoolLeaver.getAge() + ", " + schoolLeaver.getEducation().toString());		//Test
            }
            personsLeavingEducation.get(gender).clear();    //Clear for re-use in the next year

            if(Parameters.systemOut) {
                //Check result of alignment
                int countHighEdPeople = 0, countMediumEdPeople = 0;
                for(Person person : persons) {
                    if( person.getDgn().equals(gender) && (person.getDag() <= 65) ) {        //Alignment projections are based only on persons younger than 66 years old
                        if (person.isToLeaveSchool()) {
                            if(person.getDeh_c3() != null) {
                                if(person.getDeh_c3().equals(Education.High)) {
                                    countHighEdPeople++;
                                } else if(person.getDeh_c3().equals(Education.Medium)) {
                                    countMediumEdPeople++;
                                }
                            }
                        }
                    }
                }
                System.out.println("Year is " + year);
                System.out.println("Gender " + gender + ", Proportions of High Edu " + ((double)countHighEdPeople/(double)numPersonsOfThisGender) + ", Medium Edu " + ((double)countMediumEdPeople/(double)numPersonsOfThisGender));
            }
        }
    }


    /**
     * PROCESS - FERTILITY ALIGNMENT OF SIMULATED POPULATION
     *
     * Process aligns to period fertility rate. This ensures "sensible" numbers of children per simulated woman
     * Population alignment is at the end of the simulated schedule, which ensures that population aggregates
     * match official estimates.
     */
    private void fertilityAlignment() {

        // Instantiate alignment object
        FertilityAlignment fertilityAlignment = new FertilityAlignment(persons);

        // define limits of search algorithm
        double fertilityAdjustment = Parameters.getTimeSeriesValue(getYear(), TimeSeriesVariable.FertilityAdjustment);
        double minVal = Math.max(-10.0, - fertilityAdjustment - 10.0);
        double maxVal = Math.min(10.0, - fertilityAdjustment + 10.0);

        // run search
        RootSearch search = getRootSearch(0.0, minVal, maxVal, fertilityAlignment, 5.0E-3, 5.0E-3); // epsOrdinates and epsFunction determine the stopping condition for the search. For partnershipAlignment error term is the difference between target and observed share of partnered individuals.

        // update and exit
        if (search.isTargetAltered()) {
            Parameters.setAlignmentValue(getYear(), search.getTarget()[0], AlignmentVariable.FertilityAlignment); // If adjustment is altered from the initial value, update the map
            System.out.println("Fertility adjustment value was " + search.getTarget()[0]);
        }
    }


    /**
     *
     * UPDATE MODEL PARAMETERS TO REFLECT YEAR
     *
     */
    private void updateParameters() {
//		Parameters.updateProbSick(year);		//Make any adjustments to the sickness probability profile by age depending on retirement age
//		Parameters.updateUnemploymentRate(year);
    }


    /**
     *
     * REPORT ELAPSED TIME TO LOG
     *
     */
    private void printElapsedTime() {
        log.debug("Year: " + year + ", Elapsed time: " + (System.currentTimeMillis() - elapsedTime0)/1000. + " seconds.");
    }

    private void taxDatabaseUpdate() {

        System.out.println("Updating country reference for tax database");

        Connection conn = null;
        try {
            if (isFirstRun) {

                Class.forName("org.h2.Driver");
                conn = DriverManager.getConnection("jdbc:h2:file:./input" + File.separator + "input;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0;AUTO_SERVER=TRUE", "sa", "");
                TaxDonorDataParser.updateDefaultDonorTables(conn, country);
            }
        }
        catch(ClassNotFoundException|SQLException e){
            throw new RuntimeException("SQL Exception thrown! " + e.getMessage());
        } finally {
            try {
                if (conn != null) { conn.close(); }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    private void inputDatabaseInteraction() {

        Connection conn = null;
        Statement stat = null;
        try {
            Class.forName("org.h2.Driver");
            System.out.println("Reading from database at " + DatabaseUtils.databaseInputUrl);
            try {
                conn = DriverManager.getConnection("jdbc:h2:"+DatabaseUtils.databaseInputUrl + ";TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0;AUTO_SERVER=TRUE", "sa", "");
            }
            catch (SQLException e) {
                log.info(e.getMessage());
                throw new RuntimeException("SQL Exception! " + e.getMessage());
            }

            //If user chooses start year that is higher than the last available initial population, last available population should be used but with uprated monetary values
            boolean uprateInitialPopulation = (startYear > Parameters.getMaxStartYear());

            stat = conn.createStatement();

            System.out.println("Database connection established");
            if (isFirstRun) {
                //Create database tables to be used in simulation from country and year specific tables
                String[] tableNamesInitial = new String[]{"HOUSEHOLD", "BENEFITUNIT", "PERSON"};
                for (String tableName : tableNamesInitial) {
                    stat.execute("DROP TABLE IF EXISTS " + tableName + " CASCADE");
                    int year;
                    if (uprateInitialPopulation) {
                        year = Parameters.getMaxStartYear(); // Load the last available initial population from all available in tables of the database
                    } else {
                        year = startYear; // Load the country-year specific initial population from all available in tables of the database
                    }
                    stat.execute("CREATE TABLE " + tableName + " AS SELECT * FROM " + tableName + "_" + country + "_" + year);
                    System.out.println("Completed reading from " + tableName + "_" + country + "_" + year);
                }
            }

            initialHoursWorkedWeekly = new LinkedHashMap<Long, Double>();

            //Add hours from initial population which is now different than donor
            String query2 = "SELECT ID, " + Parameters.HOURS_WORKED_WEEKLY + " FROM PERSON";
            ResultSet rs2 = stat.executeQuery(query2);
            while (rs2.next()) {
                initialHoursWorkedWeekly.put(rs2.getLong("ID"), rs2.getDouble(Parameters.HOURS_WORKED_WEEKLY));
            }

            //If start year is higher than the last available population, calculate the uprating factor and apply it to the monetary values in the database:
            if (uprateInitialPopulation & isFirstRun) {
                double upratingFactor =
                        Parameters.getTimeSeriesIndex(startYear, UpratingCase.ModelInitialise) /
                                Parameters.getTimeSeriesIndex(Parameters.getMaxStartYear(), UpratingCase.ModelInitialise);
                //Modify the underlying initial population being used by applying the upratingFactor to its monetary values
                String[] columnsToUprate = new String[]{"YPNBIHS_DV", "YPTCIIHS_DV", "YPNCP", "YPNOAB", "YPLGRS_DV"};
                for (String columnToUprateName : columnsToUprate) {
                    stat.execute(
                            "ALTER TABLE PERSON ALTER COLUMN " + columnToUprateName + " NUMERIC(30,6);"
                                    + "UPDATE PERSON SET " + columnToUprateName + " = SINH(" + columnToUprateName + ") * " + upratingFactor + ";"
                                    + "UPDATE PERSON SET " + columnToUprateName + " = LOG(" + columnToUprateName + " + SQRT(POWER(" + columnToUprateName + ",2) + 1));"
                    );
                }
                stat.execute(
                        "UPDATE PERSON SET SIMULATION_TIME = " + startYear + ";"
                                + "UPDATE PERSON SET SYSTEM_YEAR = " + startYear + ";"
                );
                System.out.println("Completed amending Person table");
            }
        }
        catch(ClassNotFoundException|SQLException e){
            if(e instanceof ClassNotFoundException) {
                log.debug( "ERROR: Class not found: " + e.getMessage() + "\nCheck that the input.h2.db "
                        + "exists in the input folder.  If not, unzip the input.h2.zip file and store the resulting "
                        + "input.h2.db in the input folder!\n");
            } else {
                log.debug("SQL Exception thrown: " + e.getMessage());
                throw new RuntimeException("SQL Exception thrown! " + e.getMessage());
//	    			throw new RuntimeException("SQL Exception thrown! " + e.getMessage());
            }
        } finally {
            try {
                if (stat != null) { stat.close(); }
                if (conn != null) { conn.close(); }
            } catch (SQLException e) {
                log.debug("SQL Exception thrown: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    //----------------------------------------------------------
    //
    //	CREATE INPUT POPULATION FROM INPUT DATABASE DATA
    //
    //----------------------------------------------------------

    /**
     * Note that this method makes minimal assumptions on the input database data.  It does NOT assume the data
     * is from EUROMOD data files.  This makes the simulation very flexible with regards to input database data,
     * but it does mean that the method is not optimised for speed.
     *
     */
    private void createInitialPopulationDataStructures() {

        // initialisations
        System.out.println("Creating population structures");
        persons = new LinkedHashSet<Person>();
        benefitUnits = new LinkedHashSet<BenefitUnit>();
        households = new LinkedHashSet<Household>(); //Also initialise set of families to which benefitUnits belong

        double aggregatePersonsWeight = 0.;            //Aggregate Weight of simulated individuals (a weighted sum of the simulated individuals)
        double aggregateHouseholdsWeight = 0.;        //Aggregate Weight of simulated benefitUnits (a weighted sum of the simulated households)

        //TODO: Slight differences between otherwise identical simulations arise when loading "processed" vs "unprocessed" data (distinguished by the if statement below)
        Processed processed = getProcessed();
        if (processed!=null) {
            Set<Household> households = processed.getHouseholds();
            if (households.isEmpty())
                throw new RuntimeException("No households in processed set");
            System.out.println("Found processed dataset - preparing for simulation");
            long householdIdCounter = 1L, benefitUnitIdCounter = 1L, personIdCounter = 1L;
            for ( Household originalHousehold : processed.getHouseholds()) {
                if (originalHousehold.getId() > householdIdCounter)
                    householdIdCounter = originalHousehold.getId();
                for (BenefitUnit benefitUnit : originalHousehold.getBenefitUnits()) {
                    if (benefitUnit.getId() > benefitUnitIdCounter)
                        benefitUnitIdCounter = benefitUnit.getId();
                    for (Person person : benefitUnit.getMembers()) {
                        if (person.getId() > personIdCounter)
                            personIdCounter = person.getId();
                        person.setAdditionalFieldsInInitialPopulation();
                    }
                    benefitUnit.initializeFields();
                }
                cloneHousehold(originalHousehold, SampleEntry.ProcessedInputData);
            }
            Household.setHouseholdIdCounter(householdIdCounter+1);
            BenefitUnit.setBenefitUnitIdCounter(benefitUnitIdCounter+1);
            Person.setPersonIdCounter(personIdCounter+1);
        } else {

            StopWatch stopwatch = new StopWatch();
            stopwatch.start();

            System.out.println("Initialising input dataset for assumed start year");
            inputDatabaseInteraction();
            System.out.println("Completed initialising input dataset");
            System.out.println("Loading survey data for starting population");
            List<Household> inputHouseholdList = loadStaringPopulation();
            System.out.println("completed loading survey data for starting population");
            if (!useWeights) {
                // Expand population, sample, and remove weights

                System.out.println("Will expand the initial population to " + popSize + " individuals, each of whom has an equal weight.");

                // approach to resampling considered here is designed to allow for surveys that over-sample some population
                // subgroups. In this case you may have many similar observations in the sample all with low survey weights
                // so that replicating by a factor adjustment on weight of each observation may result in none of the
                // observations being included in the simulated sample (unless the simulated sample was very large).
                List<Household> householdList1 = new LinkedList<>();
                List<Household> householdList2 = new LinkedList<>();
                Double minWeight = null;
                for (Household household : inputHouseholdList) {
                    double hhweight = household.getWeight();
                    boolean hasChild = false;
                    for (BenefitUnit benefitUnit : household.getBenefitUnits()) {
                        for (Person person : benefitUnit.getMembers()) {
                            person.setAdditionalFieldsInInitialPopulation();
                            if (person.getDag()<Parameters.AGE_TO_BECOME_RESPONSIBLE)
                                hasChild = true;
                        }
                        benefitUnit.initializeFields();
                    }
                    if (ignoreTargetsAtPopulationLoad || hasChild)
                        householdList1.add(household);
                    else
                        householdList2.add(household);
                    if (hhweight>0.1 && (minWeight == null || hhweight < minWeight))
                        minWeight = hhweight;
                }
                double replicationFactor = 1.0 / minWeight;    // ensures each sampled household represented at least 1 time in list

                List<Household> householdList1Deweighted = new LinkedList<>();
                for (Household household : householdList1) {

                    int numberOfClones = (int) Math.round(household.getWeight() * replicationFactor);
                    for (int ii=0; ii<numberOfClones; ii++) {
                        householdList1Deweighted.add(household);
                    }
                }
                List<Household> householdList2Deweighted = new LinkedList<>();
                for (Household household : householdList2) {

                    int numberOfClones = (int) Math.round(household.getWeight() * replicationFactor);
                    for (int ii=0; ii<numberOfClones; ii++) {
                        householdList2Deweighted.add(household);
                    }
                }

                Collections.shuffle(householdList1Deweighted, initialiseInnov1);
                Collections.shuffle(householdList2Deweighted, initialiseInnov2);
                InitialPopulationFilter filter;
                if (!ignoreTargetsAtPopulationLoad)
                    filter = new InitialPopulationFilter(popSize, startYear, maxAge);
                else
                    filter = new InitialPopulationFilter();
                boolean flagSearch1 = true, flagConsiderRegion = true;
                int counter = 0;
                while (persons.size() < (int)((double)popSize*0.999)) {

                    counter++;
                    int populationLag = persons.size();

                    if (flagSearch1)
                        searchForClones(householdList1Deweighted, flagConsiderRegion, filter);
                    else
                        searchForClones(householdList2Deweighted, flagConsiderRegion, filter);

                    if (!ignoreTargetsAtPopulationLoad) {
                        // reporting

                        if (flagSearch1 && flagConsiderRegion)
                            System.out.println("Resampling child households with region: iteration " + counter + " for population size " + persons.size());
                        else if (flagSearch1 && !flagConsiderRegion)
                            System.out.println("Resampling child households without region: iteration " + counter + " for population size " + persons.size());
                        else if (!flagSearch1 && flagConsiderRegion)
                            System.out.println("Resampling adult households with region: iteration " + counter + " for population size " + persons.size());
                        else
                            System.out.println("Resampling adult households without region: iteration " + counter + " for population size " + persons.size());
                    }

                    // consider next iteration
                    int increment = persons.size() - populationLag;
                    if (increment < 50 || ((double)increment/(double)popSize) < 0.005) {
                        // switch search strategy
                        if (flagConsiderRegion)
                            flagConsiderRegion = false;
                        else if (flagSearch1) {
                            flagSearch1 = false;
                            flagConsiderRegion = true;
                        } else
                           break;
                    }
                }
                //filter.getRemainingVacancies();
            } else {
                // use population weights

                households.addAll(inputHouseholdList);
                for (Household household : inputHouseholdList) {
                    benefitUnits.addAll(household.getBenefitUnits());
                    for (BenefitUnit benefitUnit : household.getBenefitUnits()) {
                        persons.addAll(benefitUnit.getMembers());
                    }
                }
            }

            // save to processed repository
            System.out.println("Saving compiled input data for future reference");
    //        persistProcessed();

            stopwatch.stop();
            System.out.println("Time elapsed " + stopwatch.getTime()/1000 + " seconds");
        }

        // finalise
        System.out.println("Number of simulated individuals (persons.size()) is " + persons.size() + " living in " + benefitUnits.size() + " simulated benefitUnits.");
        initialHoursWorkedWeekly = null;
        System.gc();
    }

    private void searchForClones(List<Household> householdList, boolean flagConsiderRegion, InitialPopulationFilter filter) {

        for (Household originalHousehold : householdList) {

            if (flagConsiderRegion) {
                if (ignoreTargetsAtPopulationLoad || filter.evaluate(originalHousehold)) {
                    cloneHousehold(originalHousehold, SampleEntry.InputData);
                }
            } else {
                for (Region region : Parameters.getCountryRegions()) {
                    if (ignoreTargetsAtPopulationLoad || filter.evaluate(originalHousehold, region)) {
                        cloneHousehold(originalHousehold, SampleEntry.InputData);
                    }
                }
            }
            if (persons.size() >= popSize) break;
        }
    }

    //Requires Labour Market to be initialised and EUROMOD policy scenario for start year to be specified, hence it is called after creating the Labour Market object
    private void initialisePotentialEarningsByWageEquationAndEmployerSocialInsurance() {
        for(Person person: persons) {
            person.initialisePotentialHourlyEarnings();    //XXX: We override the initialisation of persons' earnings using the estimated wage equation (this may be necessary to ensure the proportional change in unit labour cost is not ridiculously large ~10^100, due to discrepancy between the earnings data in the input database population and the estimated earnings from the wage equation of Bargain et al.  Only once the current discrepancy is fixed (hopefully using our own wage equation), can we start to re-initialise using values in the input data).
        }
    }

    // initialise attributes used to manage marriage matching
    private void createDataStructuresForMarriageMatching() {

        personsToMatch = new LinkedHashMap<>();
        for (Gender gender: Gender.values()) {
            personsToMatch.put(gender, new LinkedHashMap<>());
            for (Region region: Region.values()) {
                personsToMatch.get(gender).put(region, new LinkedHashSet<>());
            }
        }
        personsToMatch2 = new LinkedHashMap<>();
        for (Gender gender : Gender.values()) {
            for (Region region : Region.values()) {
                for(Education education: Education.values()) {
                    for(int ageGroup = 0; ageGroup <= 6; ageGroup++) { //Age groups with numerical values are created in Person class setAdditionalFieldsInInitialPopulation() method and must match Excel marriageTypes2.xlsx file.
                        String tmpKey = gender + " " + region + " " + education + " " + ageGroup;
                        personsToMatch2.put(tmpKey, new LinkedHashSet<>());
                    }
                }
            }
        }

        // For SBAM matching, initial targets:
        marriageTargetsByGenderAndRegion = MultiKeyMap.multiKeyMap(new LinkedMap<>());
        for (Gender gender : Gender.values()) {
            for (Region region : Parameters.getCountryRegions()) {
                double tmpTarget = personsToMatch.get(gender).get(region).size();
                marriageTargetsByGenderAndRegion.put(gender, region, tmpTarget);
            }
        }
        marriageTargetsByKey = new LinkedHashMap<>();
    }

    // ---------------------------------------------------------------------
    // Access methods
    // ---------------------------------------------------------------------

    public Integer getStartYear() {
        return startYear;
    }

    public Set<Person> getPersons() {
        return persons;
    }

    public Set<BenefitUnit> getBenefitUnits() {
        return benefitUnits;
    }

    public Set<Household> getHouseholds() {
        return households;
    }

    public Integer getEndYear() { return endYear; }

    public Integer getMaxAge() { return maxAge; }

    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }

    public Person getPerson(Long id) {

        for (Person person : persons) {
            if ((person.getKey() != null) && (person.getKey().getId() == id))
                return person;
        }
        throw new RuntimeException("Person with id " + id + " is not present!");
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public boolean getProjectMortality() { return projectMortality; }

    public void setProjectMortality(boolean projectMortality) { this.projectMortality = projectMortality; }

    public boolean getProjectFormalChildcare() { return projectFormalChildcare; }
    public void setProjectFormalChildcare(boolean projectFormalChildcare) { this.projectFormalChildcare = projectFormalChildcare; }
    public boolean getDonorPoolAveraging() { return donorPoolAveraging; }
    public void setDonorPoolAveraging(boolean val) { donorPoolAveraging = val; }

    public Integer getPopSize() {
        return popSize;
    }

    public void setPopSize(Integer popSize) {
        this.popSize = popSize;
    }

    public int getYear() {
        return year;
    }

//	public Integer getMinRetireAgeMales() {
//		return minRetireAgeMales;
//	}
//
//	public Integer getMinRetireAgeFemales() {
//		return minRetireAgeFemales;
//	}
//
//	public void setMinRetireAgeFemales(Integer minRetireAgeFemales) {
//		this.minRetireAgeFemales = minRetireAgeFemales;
//	}
//
//	public void setMinRetireAgeMales(Integer minRetireAgeMales) {
//		this.minRetireAgeMales = minRetireAgeMales;
//	}
//
//	public int getMinRetireAge(Gender gender) {
//		if(gender.equals(Gender.Female)) {
//			return minRetireAgeFemales;
//		} else {
//			return minRetireAgeMales;
//		}
//	}

//	public Integer getMaxRetireAgeMales() {
//		return maxRetireAgeMales;
//	}
//
//	public void setMaxRetireAgeMales(Integer maxRetireAgeMales) {
//		this.maxRetireAgeMales = maxRetireAgeMales;
//	}

//	public Integer getMaxRetireAgeFemales() {
//		return maxRetireAgeFemales;
//	}
//
//	public void setMaxRetireAgeFemales(Integer maxRetireAgeFemales) {
//		this.maxRetireAgeFemales = maxRetireAgeFemales;
//	}
//
//	public int getMaxRetireAge(Gender gender) {
//		if(gender.equals(Gender.Female)) {
//			return maxRetireAgeFemales;
//		} else {
//			return maxRetireAgeMales;
//		}
//	}

    public void setStartYear(Integer startYear) {
        this.startYear = startYear;
    }

    public void setEndYear(Integer endYear) {
        this.endYear = endYear;
    }

    public Integer getTimeTrendStopsIn() {
        return timeTrendStopsIn;
    }

    public Integer getTimeTrendStopsInMonetaryProcesses() {
        return timeTrendStopsInMonetaryProcesses;
    }

    public boolean isFixTimeTrend() {
        return fixTimeTrend;
    }

    public void setFixTimeTrend(boolean fixTimeTrend) {
        this.fixTimeTrend = fixTimeTrend;
    }

    public void setTimeTrendStopsIn(Integer timeTrendStopsIn) {
        this.timeTrendStopsIn = timeTrendStopsIn;
    }

    public Boolean getFixRandomSeed() {
        return fixRandomSeed;
    }

    public void setFixRandomSeed(Boolean fixRandomSeed) {
        this.fixRandomSeed = fixRandomSeed;
    }

    public Long getRandomSeedIfFixed() {
        return randomSeedIfFixed;
    }

    public void setRandomSeedIfFixed(Long randomSeedIfFixed) {
        this.randomSeedIfFixed = randomSeedIfFixed;
    }

    public void setInterestRateInnov(double innov) {
        interestRateInnov = innov;
    }

    public void setDisposableIncomeFromLabourInnov(double innov) {
        disposableIncomeFromLabourInnov = innov;
    }

    public Integer getsIndexTimeWindow() {
        return sIndexTimeWindow;
    }

    public void setsIndexTimeWindow(Integer sIndexTimeWindow) {
        this.sIndexTimeWindow = sIndexTimeWindow;
    }

    public Double getsIndexAlpha() {
        return sIndexAlpha;
    }

    public void setsIndexAlpha(Double sIndexAlpha) {
        this.sIndexAlpha = sIndexAlpha;
    }

    public Double getsIndexDelta() {
        return sIndexDelta;
    }

    public void setsIndexDelta(Double sIndexDelta) {
        this.sIndexDelta = sIndexDelta;
    }

    public Double getSavingRate() {
        return savingRate;
    }

    public void setSavingRate(Double savingRate) {
        this.savingRate = savingRate;
    }

    public Map<Gender, LinkedHashMap<Region, Set<Person>>> getPersonsToMatch() {
        return personsToMatch;
    }

    public int getNumberOfSimulatedPersons() {
        return persons.size();
    }

    public int getNumberOfSimulatedHouseholds() {
        return benefitUnits.size();
    }

    public int getPopulationProjectionByAge0_18() {
        return getPopulationProjectionByAge(0,18);
    }

    public int getPopulationProjectionByAge0() {
        return getPopulationProjectionByAge(0,0);
    }

    public int getPopulationProjectionByAge2_10() {
        return getPopulationProjectionByAge(2,10);
    }

    public int getPopulationProjectionByAge19_25() {
        return getPopulationProjectionByAge(19,25);
    }

    public int getPopulationProjectionByAge40_59() {
        return getPopulationProjectionByAge(40,59);
    }

    public int getPopulationProjectionByAge60_79() {
        return getPopulationProjectionByAge(60,79);
    }

    public int getPopulationProjectionByAge80_100() {
        return getPopulationProjectionByAge(80,100);
    }

    public int getPopulationProjectionByAge(int startAge, int endAge) {
        double numberOfPeople = 0.;
        for (Gender gender : Gender.values()) {
            for (Region region : Parameters.getCountryRegions()) {
                for (int age = startAge; age <= endAge; age++) {
                    numberOfPeople += Parameters.getPopulationProjections(gender, region, age, year);
                }
            }
        }
        int numberOfPeopleScaled = (int) Math.round(numberOfPeople / scalingFactor);
        return numberOfPeopleScaled;
    }

    public double getWeightedNumberOfPersons() {
        double sum = 0.;
        for(Person person : persons) {
            if(!useWeights) { //If not using weights everyone has weight 1 multiplied by scaling factor
                sum += person.getWeight() * scalingFactor;
            } else { //If using weights
                sum += person.getWeight();
            }
        }
        return sum;
    }

    public double getWeightedNumberOfHouseholds() {
        double sum = 0.;
        for(BenefitUnit house: benefitUnits) {

            if(!useWeights) { //If not using weights everyone has weight 1 multiplied by scaling factor
                sum += house.getWeight() * scalingFactor;
            } else { //If using weights
                sum += house.getWeight();
            }
        }
        return sum;
    }

    public double getWeightedNumberOfHouseholds80minus() {
        double sum = 0.;
        for(BenefitUnit house: benefitUnits) {

            int maleAge = 0;
            int femaleAge = 0;
            if (house.getMale() != null) {
                if(house.getFemale() != null) {
                    maleAge = house.getMale().getDag();
                    femaleAge = house.getFemale().getDag();
                } else {
                    maleAge = house.getMale().getDag();
                }
            } else {
                femaleAge = house.getFemale().getDag();
            }


            if (Math.max(maleAge, femaleAge) <= 80 && Math.min(maleAge, femaleAge) >= 0) {
                if(!useWeights) { //If not using weights everyone has weight 1 multiplied by scaling factor
                    sum += house.getWeight() * scalingFactor;
                } else { //If using weights
                    sum += house.getWeight();
                }
            }
        }
        return sum;
    }

    //	public Integer getAgeNonWorkPeopleRetire() {
//		return ageNonWorkPeopleRetire;
//	}
//
//	public void setAgeNonWorkPeopleRetire(Integer ageNonWorkPeopleRetire) {
//		this.ageNonWorkPeopleRetire = ageNonWorkPeopleRetire;
//	}

    public Map<Long, Double> getInitialHoursWorkedWeekly() {
        return initialHoursWorkedWeekly;
    }

//	public boolean isRefreshInputDatabase() {
//		return refreshInputDatabase;
//	}
//
//	public void setRefreshInputDatabase(boolean refreshInputDatabase) {
//		this.refreshInputDatabase = refreshInputDatabase;
//	}

    public boolean isInitialisePotentialEarningsFromDatabase() {
        return initialisePotentialEarningsFromDatabase;
    }

    public void setInitialisePotentialEarningsFromDatabase(boolean initialisePotentialEarningsFromDatabase) {
        this.initialisePotentialEarningsFromDatabase = initialisePotentialEarningsFromDatabase;
    }


    public LabourMarket getLabourMarket() {
        return labourMarket;
    }

    public boolean isUseWeights() {
        return useWeights;
    }

    public void setUseWeights(boolean useWeights) {
        this.useWeights = useWeights;
    }


    public UnionMatchingMethod getUnionMatchingMethod() {
        return unionMatchingMethod;
    }

    public void setUnionMatchingMethod(UnionMatchingMethod unionMatchingMethod) {
        this.unionMatchingMethod = unionMatchingMethod;
    }

    public boolean isAlignFertility() {
        return alignFertility;
    }

    public void setAlignFertility(boolean alignFertility) {
        this.alignFertility = alignFertility;
    }

    public boolean isAlignRetirement() {
        return alignRetirement;
    }

    public boolean isAlignDisability() {
        return alignDisability;
    }

    public void setAlignDisability(boolean alignDisability) {
        this.alignDisability = alignDisability;
    }

    public void setAlignRetirement(boolean alignRetirement) {
        this.alignRetirement = alignRetirement;
    }

    public void setSaveImperfectTaxDBMatches(boolean flag) {
        saveImperfectTaxDBMatches = flag;
    }

    public void setAlignPopulation(boolean flag) {
        alignPopulation = flag;
    }

    public void setAlignCohabitation(boolean flag) {
        alignCohabitation = flag;
    }

    public void setAlignEducation(boolean flag) {
        alignEducation = flag;
    }

    public void setAlignInSchool(boolean flag) {
        alignInSchool = flag;
    }

    public boolean isAlignInSchool() {
        return alignInSchool;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public boolean getProjectSocialCare() { return projectSocialCare; }
    public void setProjectSocialCare(boolean projectSocialCare) {
        this.projectSocialCare = projectSocialCare;
    }

    public boolean getEnableIntertemporalOptimisations() {
        return enableIntertemporalOptimisations;
    }

    public void setEnableIntertemporalOptimisations(boolean enableIntertemporalOptimisations) {
        this.enableIntertemporalOptimisations = enableIntertemporalOptimisations;
    }

    public boolean getUseSavedBehaviour() { return useSavedBehaviour; }

    public void setUseSavedBehaviour(boolean useSavedBehaviour) {
        this.useSavedBehaviour = useSavedBehaviour;
    }

    public String getReadGrid() { return readGrid; }

    public void setReadGrid(String readGrid) {
        this.readGrid = readGrid;
    }

    public int getEmploymentOptionsOfPrincipalWorker() { return employmentOptionsOfPrincipalWorker; }

    public void setEmploymentOptionsOfPrincipalWorker(int employmentOptionsOfPrincipalWorker) {
        this.employmentOptionsOfPrincipalWorker = employmentOptionsOfPrincipalWorker;
    }

    public int getEmploymentOptionsOfSecondaryWorker() { return employmentOptionsOfSecondaryWorker; }

    public void setEmploymentOptionsOfSecondaryWorker(int employmentOptionsOfSecondaryWorker) {
        this.employmentOptionsOfSecondaryWorker = employmentOptionsOfSecondaryWorker;
    }

    public boolean getResponsesToHealth() { return responsesToHealth; }

    public void setResponsesToHealth(boolean responsesToHealth) {
        this.responsesToHealth = responsesToHealth;
    }

    public boolean getResponsesToDisability() { return responsesToDisability; }

    public void setResponsesToDisability(boolean responsesToDisability) {
        this.responsesToDisability = responsesToDisability;
    }

    public int getMinAgeForPoorHealth() { return minAgeForPoorHealth; }

    public void setMinAgeForPoorHealth(int minAgeForPoorHealth) {
        this.minAgeForPoorHealth = minAgeForPoorHealth;
    }

    public double getScalingFactor() {
        return scalingFactor;
    }


    public boolean getResponsesToEducation() { return responsesToEducation; }

    public void setResponsesToEducation(boolean responsesToEducation) {
        this.responsesToEducation = responsesToEducation;
    }


    public boolean getResponsesToRegion() { return responsesToRegion; }

    public void setResponsesToRegion(boolean responsesToRegion) {
        this.responsesToRegion = responsesToRegion;
    }

    public MacroScenarioPopulation getMacroShockPopulation() {
        return macroShockPopulation;
    }

    public void setMacroShockPopulation(MacroScenarioPopulation macroShockPopulation) {
        this.macroShockPopulation = macroShockPopulation;
    }

    public MacroScenarioProductivity getMacroShockProductivity() {
        return macroShockProductivity;
    }

    public void setMacroShockProductivity(MacroScenarioProductivity macroShockProductivity) {
        this.macroShockProductivity = macroShockProductivity;
    }

    public MacroScenarioGreenPolicy getMacroShockGreenPolicy() {
        return macroShockGreenPolicy;
    }

    public void setMacroShockGreenPolicy(MacroScenarioGreenPolicy macroShockGreenPolicy) {
        this.macroShockGreenPolicy = macroShockGreenPolicy;
    }

    public boolean isMacroShocksOn() {
        return macroShocksOn;
    }

    public void setMacroShocksOn(boolean macroShocksOn) {
        this.macroShocksOn = macroShocksOn;
    }

    public boolean getFlagDefaultToTimeSeriesAverages() { return flagDefaultToTimeSeriesAverages; }
    public void setFlagDefaultToTimeSeriesAverages(boolean val) { flagDefaultToTimeSeriesAverages = val; }
    public boolean getResponsesToLowWageOffer() { return responsesToLowWageOffer; }
    public void setResponsesToLowWageOffer(boolean val) { responsesToLowWageOffer = val; }
    public boolean getResponsesToRetirement() { return responsesToRetirement;}
    public void setResponsesToRetirement(boolean val) { responsesToRetirement = val;}
    public boolean getResponsesToPension() { return responsesToPension;}
    public void setResponsesToPension(boolean val) { responsesToPension = val;}

    public Map<String, Double> getPolicyNameIncomeMedianMap() {
        return policyNameIncomeMedianMap;
    }

    /*
    policyNameIncomeMedianMap is calculated before the first year is simulated and therefore contains non-uprated gross income values.
    This is then uprated in this method, before being used in BenefitUnit to decide if a ratio or imputation of disposable income should be used.
     */
    public Double getUpratedMedianIncomeForCurrentYear() {
        String policyName = Parameters.getEUROMODpolicyForThisYear(year);
        Double value = policyNameIncomeMedianMap.get(policyName) * (double) Parameters.upratingFactorsMap.get(year, policyName);
        return value;
    }

    public boolean isLabourMarketCovid19On() {
        return labourMarketCovid19On;
    }

    public void setLabourMarketCovid19On(boolean labourMarketCovid19On) {
        this.labourMarketCovid19On = labourMarketCovid19On;
    }

    public SimPathsCollector getCollector() {
        return collector;
    }

    public void setCollector(SimPathsCollector collector) {
        this.collector = collector;
    }

    public boolean isAlignCohabitation() {
        return alignCohabitation;
    }

    public boolean isAlignEmployment() {
        return alignEmployment;
    }


    /**
     *
     * POPULATE PARAMETERS.taxdbReferences FOR EVALUATING TAX AND BENEFIT PAYMENTS
     *
     */
    private static void populateTaxdbReferences() {

        Map<Triple<Integer,Integer,Integer>,List<Integer>> taxdbReferences = Parameters.getTaxdbReferences();
        if (taxdbReferences.size() == 0) {
            // Checks if the map is already populated; if not proceed with the code below. This should happen only on the first run of the model.

            System.out.println("Populating donor database indices");

            //------------------------------------------------------------
            // start work
            //------------------------------------------------------------
            EntityTransaction txn = null;
            try {

                // access database and obtain donor pool
                Map propertyMap = new HashMap();
                propertyMap.put("hibernate.connection.url", "jdbc:h2:file:" + DatabaseUtils.databaseInputUrl);
                EntityManager em = Persistence.createEntityManagerFactory("tax-database", propertyMap).createEntityManager();
                txn = em.getTransaction();
                txn.begin();
                String query = "SELECT tu FROM DonorTaxUnit tu LEFT JOIN FETCH tu.policies tp ORDER BY tp.originalIncomePerMonth";
                List<DonorTaxUnit> donorPool = em.createQuery(query).getResultList();
                System.out.println("Completed accessing donor data from the database");

                double[][] dataDualIncome = {}, dataChildcare = {}, dataDualIncomeChildcare = {};

                // loop over each donor
                for (int ii = 0; ii < donorPool.size(); ii++) {

                    DonorTaxUnit donor = donorPool.get(ii);
                    for (int fromYear : Parameters.EUROMODpolicyScheduleSystemYearMap.keySet()) {

                        // populate taxdbReferences
                        int systemYear = Parameters.EUROMODpolicyScheduleSystemYearMap.get(fromYear).getValue();
                        DonorTaxUnitPolicy donorPolicy = donor.getPolicyBySystemYear(systemYear);
                        for (int jj = 0; jj < Parameters.TAXDB_REGIMES; jj++) {

                            int index = donorPolicy.getDonorKey(jj);

                            Triple<Integer, Integer, Integer> key =  Triple.of(systemYear, jj, index);
                            List<Integer> donors = taxdbReferences.get(key);
                            if (donors == null) {
                                // need to instantiate list

                                taxdbReferences.put(key, new ArrayList<>());
                                taxdbReferences.get(key).add(ii);
                            } else {
                                // list exists - need to find correct insertion point

                                int kk = donors.size() - 1;
                                while (kk >= -1) {
                                    if (kk == -1) {
                                        // reached bottom of existing list

                                        taxdbReferences.get(key).add(0, ii);
                                    } else {
                                        // internal to list

                                        DonorTaxUnit lastDonor = donorPool.get(donors.get(kk));
                                        if (lastDonor.getPolicyBySystemYear(systemYear).getOriginalIncomePerMonth() <=
                                                donor.getPolicyBySystemYear(systemYear).getOriginalIncomePerMonth()) {
                                            // append donor to end of list

                                            taxdbReferences.get(key).add(kk + 1, ii);
                                            kk = -1;
                                        }
                                    }
                                    kk--;
                                }
                            }
                        }
                    }


                    // collect data for populating MahalanobisDistance objects
                    double originalIncome = donor.getPolicyBySystemYear(Parameters.BASE_PRICE_YEAR).getNormalisedOriginalIncomePerMonth();
                    double secondIncome = donor.getPolicyBySystemYear(Parameters.BASE_PRICE_YEAR).getNormalisedSecondIncomePerMonth();
                    double childcareCost = donor.getPolicyBySystemYear(Parameters.BASE_PRICE_YEAR).getNormalisedChildcareCostPerMonth();;
                    if (secondIncome > 0.01 && childcareCost < 0.01) {
                        double[] datum = {originalIncome, secondIncome};
                        dataDualIncome = ArrayUtils.add(dataDualIncome, datum);
                    }
                    if (childcareCost > 0.01 && secondIncome < 0.01) {
                        double[] datum = {originalIncome, childcareCost};
                        dataChildcare = ArrayUtils.add(dataChildcare, datum);
                    }
                    if (secondIncome>0.01 && childcareCost>0.01) {
                        double[] datum = {originalIncome, secondIncome, childcareCost};
                        dataDualIncomeChildcare = ArrayUtils.add(dataDualIncomeChildcare, datum);
                    }
                }
                MahalanobisDistance mdDualIncome = new MahalanobisDistance(dataDualIncome);
            //    MahalanobisDistance mdChildcare = new MahalanobisDistance(dataChildcare);
            //    MahalanobisDistance mdDualIncomeChildcare = new MahalanobisDistance(dataDualIncomeChildcare);

                // instantiate Parameters for retrieval
                Parameters.setTaxdbReferences(taxdbReferences);
                Parameters.setDonorPool(donorPool);
                Parameters.setMdDualIncome(mdDualIncome);
            //    Parameters.setMdChildcare(mdChildcare);
            //    Parameters.setMdDualIncomeChildcare(mdDualIncomeChildcare);

                // close database connection
                txn.commit();
                em.close();
            } catch (Exception e) {
                if (txn != null && txn.isActive()) {
                    txn.rollback();
                }
                e.printStackTrace();
            }
        }
    }

    public void clearPersonsToMatch() {

        for (Gender gender: Gender.values()) {
            for (Region region: Region.values()) {
                personsToMatch.get(gender).get(region).clear();
            }
        }
        for (Gender gender : Gender.values()) {
            for (Region region : Region.values()) {
                for (Education education: Education.values()) {
                    for (int ageGroup = 0; ageGroup <= 6; ageGroup++) { //Age groups with numerical values are created in Person class setAdditionalFieldsInInitialPopulation() method and must match Excel marriageTypes2.xlsx file.
                        String tmpKey = gender + " " + region + " " + education + " " + ageGroup;
                        personsToMatch2.get(tmpKey).clear();
                    }
                }
            }
        }
    }

    private Processed getProcessed() {
        return getProcessed(country, startYear, popSize, ignoreTargetsAtPopulationLoad);
    }

    private Processed getProcessed(Country country, int startYear, int popSize, boolean ignoreTargetsAtPopulationLoad) {

        Processed processed = null;

        EntityTransaction txn = null;
        try {

            // query database
            EntityManager em = Persistence.createEntityManagerFactory("starting-population").createEntityManager();
            txn = em.getTransaction();
            txn.begin();
            String query = "SELECT processed FROM Processed processed LEFT JOIN FETCH processed.households households LEFT JOIN FETCH households.benefitUnits benefitUnits LEFT JOIN FETCH benefitUnits.members members WHERE processed.startYear = " + startYear + " AND processed.popSize = " + popSize + " AND processed.country = " + country + " AND processed.noTargets = " + ignoreTargetsAtPopulationLoad + " ORDER BY households.key.id";

            List<Processed> processedList = em.createQuery(query).getResultList();
            if (!processedList.isEmpty()) {

                if (processedList.size()>1)
                    throw new RuntimeException("more than one relevant dataset returned from database");
                processed = processedList.get(0);
                processed.resetDependents();
            }

            // close database connection
            em.close();
        } catch (Exception e) {
            if (txn != null) {
                txn.rollback();
            }
            e.printStackTrace();
            throw new RuntimeException("Problem sourcing data for starting population");
        }

        return processed;
    }

    private List<Household> loadStaringPopulation() {

        List<Household> households;

        EntityTransaction txn = null;
        try {

            Map propertyMap = new HashMap();
            propertyMap.put("hibernate.connection.url", "jdbc:h2:file:" + DatabaseUtils.databaseInputUrl);
            EntityManager em = Persistence.createEntityManagerFactory("starting-population", propertyMap).createEntityManager();
            txn = em.getTransaction();
            txn.begin();
            String query = "SELECT households FROM Household households LEFT JOIN FETCH households.benefitUnits benefitUnits LEFT JOIN FETCH benefitUnits.members members";
            households = em.createQuery(query).getResultList();

            // close database connection
            em.close();
        } catch (Exception e) {
            if (txn != null) {
                txn.rollback();
            }
            e.printStackTrace();
            throw new RuntimeException("Problem sourcing data for starting population");
        }

        return households;
    }

    private void persistProcessed() {
        persistProcessed(households, country, startYear, popSize, ignoreTargetsAtPopulationLoad);
    }

    private void persistProcessed(Set<Household> households, Country country, int startYear, int popSize, boolean ignoreTargetsAtPopulationLoad) {

        EntityTransaction txn = null;
        try {

            EntityManager em = Persistence.createEntityManagerFactory("starting-population").createEntityManager();
            txn = em.getTransaction();
            txn.begin();

            Processed processed = new Processed(country, startYear, popSize, ignoreTargetsAtPopulationLoad);
            em.persist(processed);  // generates processed id

            for (Household household : households) {
                household.setProcessed(processed);
                for (BenefitUnit benefitUnit : household.getBenefitUnits()) {
                    benefitUnit.setProcessedId(processed.getId());
                    for (Person person : benefitUnit.getMembers()) {
                        person.setProcessedId(processed.getId());
                    }
                }
            }
            processed.setHouseholds(households);

            em.persist(processed);
            txn.commit();
            em.close();
        } catch (Exception e) {
            if (txn != null) {
                txn.rollback();
            }
            e.printStackTrace();
            throw new RuntimeException("Problem sourcing data for starting population");
        }
    }
}
