// define package
package simpaths.model;

// import Java packages
import java.io.File;
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
import jakarta.persistence.Transient;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import simpaths.data.IEvaluation;
import simpaths.data.MahalanobisDistance;
import simpaths.data.RootSearch;
import simpaths.experiment.SimPathsCollector;
import simpaths.model.decisions.DecisionParams;
import microsim.alignment.outcome.ResamplingAlignment;
import microsim.event.*;
import microsim.event.EventListener;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.apache.commons.lang3.time.StopWatch;

// import JAS-mine packages
import microsim.alignment.outcome.AlignmentOutcomeClosure;
import microsim.annotation.GUIparameter;
import microsim.data.MultiKeyCoefficientMap;
import microsim.data.db.DatabaseUtils;
import microsim.engine.AbstractSimulationManager;
import microsim.engine.SimulationEngine;
import microsim.matching.IterativeRandomMatching;
import microsim.matching.IterativeSimpleMatching;
import microsim.matching.MatchingClosure;
import microsim.matching.MatchingScoreClosure;

// import LABOURsim packages
import simpaths.data.Parameters;
import simpaths.model.decisions.ManagerPopulateGrids;
import simpaths.model.enums.*;
import simpaths.model.taxes.DonorTaxUnit;
import simpaths.data.filters.FertileFilter;
import simpaths.model.taxes.DonorTaxUnitPolicy;
import simpaths.model.taxes.Match;
import simpaths.model.taxes.Matches;


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
	private Country country; // = Country.UK;

	@GUIparameter(description = "Simulated population size (base year)")
	private Integer popSize = 170000;

	@GUIparameter(description = "Simulation first year [valid range 2011-2019]")
	private Integer startYear = 2011;

	@GUIparameter(description = "Simulation ends at year [valid range 2011-2050]")
	private Integer endYear = 2026;

	@GUIparameter(description = "Maximum simulated age")
	private Integer maxAge = 130;

	//@GUIparameter(description = "Fix year used in the regressions to one specified below")
	private boolean fixTimeTrend = true;

	@GUIparameter(description = "Fix year used in the regressions to")
	private Integer timeTrendStopsIn = 2021;

	private Integer timeTrendStopsInMonetaryProcesses = 2021; // For monetary processes, time trend always continues to 2017 (last observed year in the estimation sample) and then values are grown at the growth rate read from Excel

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

	private double interestRateInnov = 0.0;		// used to explore behavioural sensitivity to assumed interest rates

//	@GUIparameter(description = "Force recreation of input database based on the data provided by the population_[country].csv file")
//	private boolean refreshInputDatabase = false;		//Tables can be constructed in GUI dialog in launch, before JAS-mine GUI appears.  However, if skipping that, and manually altering the EUROMODpolicySchedule.xlsx file, this will need to be set to true to build new input database before simulation is run (though the new input database will only be viewable in the output/input/input.h2.db file).

//	@GUIparameter(description = "If true, set initial earnings from data in input population, otherwise, set using the wage equation regression estimates")
	private boolean initialisePotentialEarningsFromDatabase = true;

	//	@GUIparameter(description = "If unchecked, will expand population and not use weights")
	private boolean useWeights = false;

	@GUIparameter(description = "If unchecked, will use the standard matching method")
//	private boolean useSBAMMatching = false;
	private UnionMatchingMethod unionMatchingMethod = UnionMatchingMethod.ParametricNoRegion;

	@GUIparameter(description = "tick to project mortality based on gender, age, and year specific probabilities")
	private boolean projectMortality = true;

	//	@GUIparameter(description = "If checked, will align fertility")
	private boolean alignFertility = true;

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
	public boolean projectFormalChildcare = true;

	@GUIparameter(description = "Average over donor pool when imputing transfer payments")
	public boolean donorPoolAveraging = true;

	private int ordering = Parameters.MODEL_ORDERING;    //Used in Scheduling of model events.  Schedule model events at the same time as the collector and observer events, but a lower order, so will be fired before the collector and observer have updated.

	private Set<Person> persons;

	//For marriage matching - types based on region and gender:
	//private Map<Gender, LinkedHashMap<Region, Double>> marriageTargetsGenderRegion;
	private MultiKeyMap<Object, Double> marriageTargetsByGenderAndRegion;

	private LinkedHashMap<String, Double>  marriageTargetsByKey;

	private long elapsedTime;

	private long timerYearStart;
	private long timerStartSim;

	private int year;

	private Set<BenefitUnit> benefitUnits;

	private Set<Household> households;

	private Map<Gender, LinkedHashMap<Region, Set<Person>>> personsToMatch;

	private LinkedHashMap<String, Set<Person>> personsToMatch2;

	private double ageDiffBound = Parameters.AGE_DIFFERENCE_INITIAL_BOUND;

	private double potentialHourlyEarningsDiffBound = Parameters.POTENTIAL_EARNINGS_DIFFERENCE_INITIAL_BOUND;

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
	private boolean projectSocialCare = true;

	@GUIparameter(description = "tick to enable intertemporal optimised consumption and labour decisions")
	private boolean enableIntertemporalOptimisations = false;

	@GUIparameter(description = "tick to use behavioural solutions saved by a previous simulation")
	private boolean useSavedBehaviour = false;

	@GUIparameter(description = "simulation name to read in grids from:")
	private String readGrid = "test1";

	//	@GUIparameter(description = "tick to save behavioural solutions assumed for simulation")
	private boolean saveBehaviour = true;

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
	private boolean responsesToDisability = false;

	@GUIparameter(description = "minimum age for expecting less than perfect health in IO solutions")
	private Integer minAgeForPoorHealth = 50;

	@GUIparameter(description = "whether to include geographic region in state space for IO behavioural solutions")
	private boolean responsesToRegion = false;

	RandomGenerator cohabitInnov;
	RandomGenerator fertilityInnov;
	Random initialiseInnov;
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

		// set seed for random number generator
		if (fixRandomSeed) SimulationEngine.getRnd().setSeed(randomSeedIfFixed);
		fertilityInnov = new Random(SimulationEngine.getRnd().nextLong());
		cohabitInnov = new Random(SimulationEngine.getRnd().nextLong());
		initialiseInnov = new Random(SimulationEngine.getRnd().nextLong());
		educationInnov = new Random(SimulationEngine.getRnd().nextLong());
		popAlignInnov = new Random(SimulationEngine.getRnd().nextLong());

		// load model parameters
		Parameters.loadParameters(country, maxAge, enableIntertemporalOptimisations, projectFormalChildcare, projectSocialCare, donorPoolAveraging, fixTimeTrend, timeTrendStopsIn, startYear, endYear, interestRateInnov);
		if (enableIntertemporalOptimisations) {

			alignEmployment = false;
			DecisionParams.loadParameters(employmentOptionsOfPrincipalWorker, employmentOptionsOfSecondaryWorker,
					responsesToHealth, minAgeForPoorHealth, responsesToDisability, responsesToRegion, responsesToEducation,
					responsesToPension, responsesToLowWageOffer, responsesToRetirement, readGrid,
					getEngine().getCurrentExperiment().getOutputFolder(), startYear, endYear);
			//DecisionTests.compareGrids();
			//DatabaseExtension.extendInputData();
		}

        log.debug("Parameters loaded");

		// populate tax donor references
		populateTaxdbReferences();
		//TestTaxRoutine.run();


		if (fixRandomSeed) Parameters.getWageAndAgeDifferentialMultivariateNormalDistribution().reseedRandomGenerator(randomSeedIfFixed);

        // set start year for simulation
        year = startYear;
        // EUROMODpolicyNameForThisYear = Parameters.getEUROMODpolicyForThisYear(year);

		//Display current country and start year in the console
		System.out.println("Country: " + country + ". Running simulation from: " + startYear + " to " + endYear);

		// time check
		elapsedTime = System.currentTimeMillis();

        // create country-specific tables in the input database and parse the EUROMOD policy scenario data for initializing the donor population
		try {
			inputDatabaseInteraction();
		} catch (InterruptedException interruptedException) {
			log.debug(interruptedException.getMessage());
			return;
		}

		// creates initial population (Person and BenefitUnit objects) based on data in input database.
		// Note that the population may be cropped to simulate a smaller population depending on user choices in the GUI.
		createInitialPopulationDataStructures();

		// initialise variables used to match marriage unions
		createDataStructuresForMarriageMatching();

		// earnings potential
		labourMarket = new LabourMarket(persons, benefitUnits);
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

		// finalise
		log.debug("Time to build objects: " + (System.currentTimeMillis() - elapsedTime)/1000. + " seconds.");
		System.out.println("Time to complete initialisation " + (System.currentTimeMillis() - elapsedTime)/1000. + " seconds.");
		elapsedTime = System.currentTimeMillis();
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

		if (enableIntertemporalOptimisations) firstYearSched.addEvent(this, Processes.RationalOptimisation);

		addEventToAllYears(Processes.UpdateParameters);
		//yearlySchedule.addEvent(this, Processes.CheckForEmptyHouseholds);
		addCollectionEventToAllYears(benefitUnits, BenefitUnit.Processes.UpdateLags);

		// DEMOGRAPHIC MODULE
		// Ageing
		yearlySchedule.addCollectionEvent(persons, Person.Processes.Ageing, false);        //Read only mode as agents are removed when they become older than Parameters.getMAX_AGE();
		addEventToAllYears(Processes.CheckForEmptyBenefitUnits);

		// Population Alignment - adjust population to projections by Gender and Age, and creates new population for minimum age
		addEventToAllYears(Processes.PopulationAlignment);

		addCollectionEventToAllYears(benefitUnits, BenefitUnit.Processes.Update);
		//yearlySchedule.addEvent(this, Processes.CheckForEmptyHouseholds);

		// Health Alignment - redrawing alignment used adjust state of individuals to projections by Gender and Age
		//Turned off for now as health determined below based on individual characteristics
		//yearlySchedule.addEvent(this, Processes.HealthAlignment);
		//yearlySchedule.addEvent(this, Processes.CheckForEmptyHouseholds);

		// Check whether persons have reached retirement Age
		addCollectionEventToAllYears(persons, Person.Processes.ConsiderRetirement, false);

		// EDUCATION MODULE
		// Check In School - check whether still in education, and if leaving school, reset Education Level
		yearlySchedule.addCollectionEvent(persons, Person.Processes.InSchool);

		// In School alignment
		addEventToAllYears(Processes.InSchoolAlignment);
		addCollectionEventToAllYears(persons, Person.Processes.LeavingSchool);

		// Align the level of education if required
		addEventToAllYears(Processes.EducationLevelAlignment);

		// Homeownership status
		yearlySchedule.addCollectionEvent(benefitUnits, BenefitUnit.Processes.Homeownership);

		// HEALTH MODULE
		// Update Health - determine health (continuous) based on regression models: done here because health depends on education
		yearlySchedule.addCollectionEvent(persons, Person.Processes.Health);

		// Update mental health - determine (continuous) mental health level based on regression models
		yearlySchedule.addCollectionEvent(persons, Person.Processes.HealthMentalHM1); //Step 1 of mental health

		// HOUSEHOLD COMPOSITION MODULE: Decide whether to enter into a union (marry / cohabit), and then perform union matching (marriage) between a male and female

		// Update potential earnings so that as up to date as possible to decide partner in union matching.
		yearlySchedule.addCollectionEvent(persons, Person.Processes.UpdatePotentialHourlyEarnings);

		// Consider whether in consensual union (cohabiting)
		yearlySchedule.addEvent(this, Processes.CohabitationRegressionAlignment);
		yearlySchedule.addCollectionEvent(persons, Person.Processes.ConsiderCohabitation);

		// Union matching
		yearlySchedule.addEvent(this, Processes.UnionMatching);
		yearlySchedule.addCollectionEvent(benefitUnits, BenefitUnit.Processes.UpdateOccupancy);
		//yearlySchedule.addEvent(this, Processes.CheckForEmptyHouseholds);
		//yearlySchedule.addEvent(this, Processes.Timer);

		// Fertility
		yearlySchedule.addEvent(this, Processes.FertilityAlignment);        //Align to fertility rates implied by projected population statistics.
		yearlySchedule.addCollectionEvent(persons, Person.Processes.GiveBirth, false);        //Cannot use read-only collection schedule as newborn children cause concurrent modification exception.  Need to specify false in last argument of Collection event.

		// TIME USE MODULE
		// Social care
		if (projectSocialCare) {
			yearlySchedule.addCollectionEvent(persons, Person.Processes.SocialCareIncidence);
			//yearlySchedule.addEvent(this, Processes.SocialCareMarketClearing);
		}

		// Unemployment
		addCollectionEventToAllYears(persons, Person.Processes.Unemployment);

		// update references for optimising behaviour
		// needs to be positioned after all decision states for the current period have been simulated
		if (enableIntertemporalOptimisations)
			addCollectionEventToAllYears(benefitUnits, BenefitUnit.Processes.UpdateStates, false);

		addEventToAllYears(Processes.LabourMarketAndIncomeUpdate);

		// Assign benefit status to individuals in benefit units, from donors. Based on donor tax unit status.
		addCollectionEventToAllYears(benefitUnits, BenefitUnit.Processes.ReceivesBenefits);

		// CONSUMPTION AND SAVINGS MODULE
		if (Parameters.projectWealth)
			addCollectionEventToAllYears(benefitUnits, BenefitUnit.Processes.ProjectNetLiquidWealth);
		addCollectionEventToAllYears(persons, Person.Processes.ProjectEquivConsumption);

		// equivalised disposable income
		addCollectionEventToAllYears(benefitUnits, BenefitUnit.Processes.CalculateChangeInEDI);

		// MENTAL HEALTH MODULE
		// Update mental health - determine (continuous) mental health level based on regression models + caseness
		addCollectionEventToAllYears(persons, Person.Processes.HealthMentalHM1); //Step 1 of mental health
		// modify the outcome of Step 1 depending on individual's exposures + caseness
		addCollectionEventToAllYears(persons, Person.Processes.HealthMentalHM2); //Step 2 of mental health.
		// update case-based measure
		addCollectionEventToAllYears(persons, Person.Processes.HealthMentalHM1HM2Cases);

		// REPORTING OF IMPERFECT TAX DATABASE MATCHES
		addEventToAllYears(Processes.CheckForImperfectTaxDBMatches);

		// END OF YEAR PROCESSES
		addEventToAllYears(Processes.CheckForEmptyBenefitUnits); //Check all household before the end of the year
		addEventToAllYears(tests, Tests.Processes.RunTests); //Run tests
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
//		getEngine().getEventQueue().scheduleOnce(new SingleTargetEvent(this, Processes.Stop), endYear, orderEarlier);

		log.debug("Time to build schedule " + (System.currentTimeMillis() - elapsedTime)/1000. + " seconds.");
		elapsedTime = System.currentTimeMillis();
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
		CohabitationRegressionAlignment,
		// HealthAlignment,
		InSchoolAlignment,
		EducationLevelAlignment,

		//Other processes
		Timer,
		UpdateParameters,
		RationalOptimisation,
		UpdateYear,
		CheckForEmptyBenefitUnits,
		CheckForImperfectTaxDBMatches,
	}

	@Override
	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {

			case StartYear:
				timerYearStart = System.currentTimeMillis();
				if (year==startYear) timerStartSim = timerYearStart;
				System.out.println("Starting year " + year);
				if (commentsOn) log.info("Starting year " + year);
				break;
			case EndYear:
				double timerForYear = (System.currentTimeMillis() - timerYearStart)/1000.0;
				System.out.println("Finished year " + year + " (in " + timerForYear + " seconds)");
				if (commentsOn) log.info("Finished year " + year + " (in " + timerForYear + " seconds)");
				break;
			case PopulationAlignment:
				if ( year <= Parameters.getPopulationProjectionsMaxYear() ) {

					if (!useWeights) {
						populationAlignmentUnweighted();
					} else {
						populationAlignmentWeighted();
					}
					if (commentsOn) log.info("Population alignment complete.");
				} else {

					if (commentsOn) log.info("Population alignment skipped as simulated year exceeds period covered by population projections.");
				}
				break;
			case CohabitationRegressionAlignment:
				if (alignCohabitation) {
					partnershipAlignment();
				}
				if (commentsOn) log.info("Cohabitation alignment complete.");
				break;
//			case HealthAlignment:
//				healthAlignment();
//				if (commentsOn) log.info("Health alignment complete.");
//				break;
			case UnionMatching:
				if(unionMatchingMethod.equals(UnionMatchingMethod.SBAM)) {
					unionMatchingSBAM();
				} else if (unionMatchingMethod.equals(UnionMatchingMethod.Parametric)) {
					unionMatching(false);
				} else {
					unionMatching(false);
					unionMatchingNoRegion(false); //Run matching again relaxing regions this time
				}
				if (commentsOn) log.info("Union matching complete.");
				break;
			case SocialCareMarketClearing:
				socialCareMarketClearning();
				break;
			case FertilityAlignment:
				if(alignFertility) {
					fertility(); //First determine which individuals should give birth according to our processes
					fertilityAlignment(); //Then align to meet the numbers implied by population projections by region
				} else {
					fertility();
				}
				if (commentsOn) log.info("Fertility alignment complete.");
				break;
			case InSchoolAlignment:
				if (alignInSchool) {
					inSchoolAlignment();
					System.out.println("Proportion of students will be aligned.");
				}
				break;
			case EducationLevelAlignment:
				if (alignEducation) {
					educationLevelAlignment();
					System.out.println("Education levels will be aligned.");
				}
				break;
			case LabourMarketAndIncomeUpdate:
				labourMarket.update(year);
				if (commentsOn) log.info("Labour market update complete.");
				break;
			case Timer:
				printElapsedTime();
				break;
			case RationalOptimisation:
				// injection point to allow for intertemporal optimisation decisions
				// this needs to be after the labourMarket object is instantiated, to permit evaluation of taxes and benefits
				Parameters.grids = ManagerPopulateGrids.run(this, useSavedBehaviour, saveBehaviour);
				break;
			case UpdateParameters:
				updateParameters();
				clearPersonsToMatch();
				if (commentsOn) log.info("Update Parameters Complete.");
				break;
			case UpdateYear:
				if (commentsOn) log.info("It's New Year's Eve of " + year);
				System.out.println("It's New Year's Eve of " + year);
				if (year==endYear) {
					double timerForSim = (System.currentTimeMillis() - timerStartSim)/1000.0/60.0;
					System.out.println("Finished simulating population in " + timerForSim + " minutes");
					if (commentsOn) log.info("Finished simulating population in " + timerForSim + " minutes");
				}
				year++;
				break;
			case CheckForEmptyBenefitUnits:

				List<BenefitUnit> benefitUnitsWithoutAdult = new ArrayList<>();
				for (BenefitUnit benefitUnit: benefitUnits) {
					if (benefitUnit.getMale() == null && benefitUnit.getFemale() == null) {
						benefitUnitsWithoutAdult.add(benefitUnit);
					}
				}
				for (BenefitUnit benefitUnit: benefitUnitsWithoutAdult) {
					log.warn("Benefit unit " + benefitUnit.getKey().getId() + " has no responsible adult, comprising " + benefitUnit.getChildren().size() + " child(ren) and " + benefitUnit.getSize() + " total members.");
					for (Person person: benefitUnit.getChildren()) {
						log.warn("person " + person.getKey().getId() + ", age " + person.getDag() + ", is in benefit unit");
					}
				}
				for (BenefitUnit benefitUnit : benefitUnitsWithoutAdult) {
					removeBenefitUnit(benefitUnit);
				}
				break;
			case CheckForImperfectTaxDBMatches:
				if (Parameters.SAVE_IMPERFECT_TAXDB_MATCHES) {
					screenForImperfectTaxDbMatches();
				}
			default:
				break;
		}
	}


	/**
	 *
	 * METHODS IMPLEMENTING PROCESS LEVEL COMPUTATIONS
	 *
	 */


	/**
	 *
	 * PROCESS - SCREEN FOR IMPERFECT TAX DATABASE MATCHES
	 *
	 */
	private void screenForImperfectTaxDbMatches() {

		Matches imperfectMatches = new Matches();
		for (BenefitUnit benefitUnit: benefitUnits) {

			Match match = benefitUnit.getTaxDbMatch();
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
			Gender gender = person.getDgn();
			Region region = person.getRegion();
			int age = Math.min(person.getDag(), maxAlignAge);
			double weight = ((Number)weightsByGenderRegionAndAge.get(gender, region, age)).doubleValue();
			person.setWeight(weight);
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
		for (int age = 1; age <= maxAlignAge; age++) {

			for (Gender gender : Gender.values()) {

				boolean flagSufficientMigrants = populationAlignmentDomesticMigration(age, gender, maxAlignAge, personsByAlignmentGroup, migrantPoolByAlignmentGroup);
				populationAlignmentInternationalMigration(age, gender, maxAlignAge, personsByAlignmentGroup, flagSufficientMigrants, migrantPoolByAlignmentGroup);
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
			int age = Math.min(person.getDag(), maxAlignAge);
			List<Person> listHere = personsByAlignmentGroup.get(person.getDgn(), person.getRegion(), age);
			if (listHere==null)
				throw new RuntimeException("failed to identify requested person alignment list");
			listHere.add(person);
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
	private MultiKeyMap<Object, List<Person>>
	migrantPoolByAlignmentGroupInit(int maxAlignAge, MultiKeyMap<Object, List<Person>> personsByAlignmentGroup) {

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
						if ( person.getBenefitUnit().getHousehold().getBenefitUnitSet().size() != 1) {
							flagAccept = false;
						} else {
							for (Person member : person.getBenefitUnit().getPersonsInBU()) {
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
			Set<Person> migrants = migrantBU.getPersonsInBU();
			Region fromRegion = migrantBU.getRegion();
			Region toRegion = immigrantRegionsIterator.next();
			migrantBU.setRegion(toRegion);

			// update working references
			for (Person person : migrants) {

				int ageHere = Math.min(person.getDag(), maxAlignAge);
				Gender genderHere = person.getDgn();

				personsByAlignmentGroup.get(genderHere, fromRegion, ageHere).remove(person);
				personsByAlignmentGroup.get(genderHere, toRegion, ageHere).add(person);
				if (ageHere == age) {

					migrantPoolByAlignmentGroup.get(genderHere, fromRegion, age).remove(person);
					migrantPoolByAlignmentGroup.get(genderHere, toRegion, age).add(person);
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
	private void populationAlignmentInternationalMigration(int age, Gender gender, int maxAlignAge,
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
						Set<Person> emigrants = emigrantBU.getPersonsInBU();
						removeBenefitUnit(emigrantBU);

						// update working references
						for (Person person : emigrants) {

							int ageHere = Math.min(person.getDag(), maxAlignAge);
							Gender genderHere = person.getDgn();
							personsByAlignmentGroup.get(genderHere, region, ageHere).remove(person);
							if (ageHere == age && person!=emigrant) {
								migrantPoolByAlignmentGroup.get(genderHere, region, age).remove(person);
							}
						}
						migrantPoolByAlignmentGroupIterator.remove();
						simulatedNumber --;
					}
				}
				Iterator<Person> personIterator = personsByAlignmentGroup.get(gender, region, age).iterator();
				while (targetNumber < simulatedNumber && personIterator.hasNext()) {
					// simulate death

					Person candidate = personIterator.next();
					if (candidate.getBenefitUnit() == null) {
						throw new RuntimeException("Missing benefit unit for candidate to kill in population alignment.");
					}
					if (candidate.getBenefitUnit().getOccupancy().equals(Occupancy.Couple) ||
							candidate.getBenefitUnit().getPersonsInBU().size() == 1) {
						// death of person should not affect existence of any other person in model

						candidate.death();

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
					BenefitUnit immigrantBU = cloneBenefitUnit(immigrant.getBenefitUnit(), newHousehold);
					immigrantBU.setRegion(region);

					// update counters and references
					for (Person person : immigrantBU.getPersonsInBU()) {

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
	private Household cloneHousehold(Household originalHousehold) {

		Household newHousehold = new Household();
		households.add(newHousehold);
		for (BenefitUnit originalBenefitUnit : originalHousehold.getBenefitUnitSet()) {
			cloneBenefitUnit(originalBenefitUnit, newHousehold);
		}
		return newHousehold;
	}


	/**********************************************************
	 *
	 * METHOD TO CLONE EXISTING BENEFIT UNIT AND ADD TO SIMULATED POPULATION
	 *
	 *********************************************************/
	private BenefitUnit cloneBenefitUnit(BenefitUnit originalBenefitUnit, Household newHousehold) {

		// initialise objects
		BenefitUnit newBenefitUnit = new BenefitUnit(originalBenefitUnit);
		newBenefitUnit.setHousehold(newHousehold);
		benefitUnits.add(newBenefitUnit);

		Set<Person> originalPersons = originalBenefitUnit.getPersonsInBU();
		for (Person originalPerson : originalPersons) {

			Person newPerson = new Person(originalPerson);
			newPerson.setBenefitUnit(newBenefitUnit);
			persons.add(newPerson);
		}

		// ensure consistent labour statistics to allow for recoding at SQLdataParser
		if (Occupancy.Couple.equals(newBenefitUnit.getOccupancy())) {
			newBenefitUnit.getFemale().setLessp_c4(newBenefitUnit.getMale().getLes_c4());
			newBenefitUnit.getMale().setLessp_c4(newBenefitUnit.getFemale().getLes_c4());
			if (Les_c4.EmployedOrSelfEmployed.equals(newBenefitUnit.getFemale().getLes_c4()) &&
					Les_c4.EmployedOrSelfEmployed.equals(newBenefitUnit.getMale().getLes_c4())) {
				newBenefitUnit.getFemale().setLesdf_c4(Lesdf_c4.BothEmployed);
				newBenefitUnit.getMale().setLesdf_c4(Lesdf_c4.BothEmployed);
			} else if (Les_c4.EmployedOrSelfEmployed.equals(newBenefitUnit.getFemale().getLes_c4())) {
				newBenefitUnit.getFemale().setLesdf_c4(Lesdf_c4.EmployedSpouseNotEmployed);
				newBenefitUnit.getMale().setLesdf_c4(Lesdf_c4.NotEmployedSpouseEmployed);
			} else if (Les_c4.EmployedOrSelfEmployed.equals(newBenefitUnit.getMale().getLes_c4())) {
				newBenefitUnit.getMale().setLesdf_c4(Lesdf_c4.EmployedSpouseNotEmployed);
				newBenefitUnit.getFemale().setLesdf_c4(Lesdf_c4.NotEmployedSpouseEmployed);
			} else {
				newBenefitUnit.getMale().setLesdf_c4(Lesdf_c4.BothNotEmployed);
				newBenefitUnit.getFemale().setLesdf_c4(Lesdf_c4.BothNotEmployed);
			}
		}

		newHousehold.addBenefitUnit(newBenefitUnit);

		return newBenefitUnit;
	}


	/**
	 *
	 * PROCESS - HEALTH ALIGNMENT OF SIMULATED POPULATION
	 *
	 */
    /*
	//TODO: The health alignment might have to be handled differently with continuous health
	private void healthAlignment() {
		
		for (Gender gender: Gender.values()) {
			for (int age = Parameters.MIN_AGE_TO_ALIGN_HEALTH; age <= Parameters.getFixedRetireAge(year, gender); age++) {
				
				//Target proportion
				double proportionWithBadHealth = ((Number)Parameters.getProbSick().get(gender, age)).doubleValue();
				
				//People to be aligned in gender-age specific cell
				Set<Person> personsWithGenderAndAge = new LinkedHashSet<Person>();
				for (Region region: Parameters.getCountryRegions()) {
					personsWithGenderAndAge.addAll(personsByGenderRegionAndAge.get(gender,  region,  age));
				}
				
				//Align
				new ResamplingWeightedAlignment<Person>().align(
						personsWithGenderAndAge, 
						null,
						new AlignmentOutcomeClosure<Person>() {

							@Override
							public boolean getOutcome(Person agent) {
								return agent.getDhe() == 1.; //TODO: Check the new continuous health status 
							}

							@Override
							public void resample(Person agent) {	
								//Swap health status
								if (agent.getDhe() > 1.) {
									agent.setDhe(1.);
								} else {
									agent.setDhe(3.); //TODO: What numerical value should correspond to "good" health?
								}
							}
							
						},
						proportionWithBadHealth
				);
			}						
		}
	}
	*/

	/*
	 * unionMatchingSBAM implements a marriage matching method presented by Stephensen 2013.
	 */

	int partnershipsCreated = 0;
	int malesUnmatched = 0;
	int femalesUnmatched = 0;

	@SuppressWarnings("unchecked")
	private void unionMatchingSBAM() {

		int malesToBePartnered = 0;
		int femalesToBePartnered = 0;
		partnershipsCreated = 0;

		for (Person p : persons) {
			if (p.isToBePartnered() == true) {
				if (p.getDgn().equals(Gender.Male)) malesToBePartnered++;
				else if (p.getDgn().equals(Gender.Female)) femalesToBePartnered++;
			}
		}

//		System.out.println("Number of males to be partnered is " + malesToBePartnered + " , number of females to be partnered is " + femalesToBePartnered);

		/* If adjustZeroEntries = true, zero frequencies are set to a very small number (1.e-6) for combinations of types that theoretically could occur. This are unlikely to result in any actual matches,
		 * as they are set to nearest integer, but allow the matches we are interested in to be adjusted. (One possibility is to introduce matching with probability equal to the frequency for such combinations). 
		 */
		boolean adjustZeroEntries = true;

		//1. Load distribution of marriages observed in Excel using ExcelLoader from marriageTypes2.xlsx file. Store a copy in marriageTypesToAdjust, which is
		// a MultiKeyCoefficientMap - it has 2 string keys that identify a value. 1st key is person type, 2nd key is partner type, value is the number of marriages between these types
		// observed in the data.
		MultiKeyCoefficientMap marriageTypesToAdjustMap = Parameters.getMarriageTypesFrequency().clone(); //Clone the original map loaded from Excel to adjust frequencies on a copy

		//Create a set of keys on which the types are defined: currently Gender, Region, Education, Age Group
		Set<MultiKey> keysMultiKeySet = new LinkedHashSet<MultiKey>();
		Set<String> keysStringSet = new LinkedHashSet<String>();
		for(Gender gender : Gender.values()) {

			for(Region region : Parameters.getCountryRegions()) {

				Set<Person> tmpPersonsSet = new LinkedHashSet<Person>();
				tmpPersonsSet.addAll(personsToMatch.get(gender).get(region)); //Using currently defined process for cohabitation, add all people who want to match to a set

				//The set of people who want to match can be further divided based on observables, e.g. we include Education. This must match the Excel file with frequencies, also in order of variables
				for (Education education : Education.values()) {

					for(int ageGroup = 0; ageGroup <= 11; ageGroup++) {

						Set<Person> tmpPersonsSet2 = new LinkedHashSet<Person>(); //Add to this set people from tmpPersonsSet selected on further observables
						String tmpKeyString = gender + " " + region + " " + education + " " + ageGroup; //MultiKey defined above, but for most methods we use a composite String key instead as MultiKeyMap has a limit of keys
						for (Person person : tmpPersonsSet) {

							if (person.getDeh_c3().equals(education) && person.getAgeGroup() == ageGroup) tmpPersonsSet2.add(person); //If education level matches add person to the set
						}

						personsToMatch2.put(tmpKeyString, tmpPersonsSet2); //Add a key and set of people to set of persons to match. Each key corresponds to a set of people of certain Gender, Region, and Education who want to match

						//Now add the number of people to match for gender, region, education as target
						double tmpTargetDouble = personsToMatch2.get(tmpKeyString).size();

						//Create a set containing row keys of marriageTypesToAdjust:
						Set<String> tmpKeysStringSet = new LinkedHashSet<String>();
						MapIterator frequenciesIterator = marriageTypesToAdjustMap.mapIterator();
						while (frequenciesIterator.hasNext()) {

							frequenciesIterator.next();
							MultiKey tmpKeyMultiKey = (MultiKey) frequenciesIterator.getKey();
							String key0String = tmpKeyMultiKey.getKey(0).toString();
							tmpKeysStringSet.add(key0String); //The only types not in the set should be those that don't have any matches in the data
						}

						if(tmpKeysStringSet.contains(tmpKeyString)) { //Check if the target is contained in frequencies from the data - if not, 0 entries cannot be adjusted anyway

							marriageTargetsByKey.put(tmpKeyString, tmpTargetDouble); //Update marriageTargetByKey
							MultiKey tmpKeyMultiKey = new MultiKey(gender, region, education, ageGroup);
							keysMultiKeySet.add(tmpKeyMultiKey); //Add MultiKey to set of keys
							keysStringSet.add(tmpKeyString);
						}
					}
				}
			}
		}

		//For sparse matrix with only few positive entries, convergence is not very good. One way to deal with it is to use very small numbers instead of 0 for matches that
		//theoretically could occur? (i.e. no same sex matches, and no cross-region matches) but were not observed in the data.
		if(adjustZeroEntries) {

			for (MultiKey key1 : keysMultiKeySet) { //For each row in the frequency matrix

				Gender gender1 = (Gender) key1.getKey(0);
				Region region1 = (Region) key1.getKey(1);
				Education education1 = (Education) key1.getKey(2);
				int ageGroup1 = (int) key1.getKey(3);
				String key1String = gender1 + " " + region1 + " " + education1 + " " + ageGroup1;
				// System.out.println();
				for(MultiKey key2 : keysMultiKeySet) { //For each column

					Gender gender2 = (Gender) key2.getKey(0);
					Region region2 = (Region) key2.getKey(1);
					Education education2 = (Education) key2.getKey(2);
					int ageGroup2 = (int) key2.getKey(3);
					String key2String = gender2 + " " + region2 + " " + education2 + " " + ageGroup2;
					if(marriageTypesToAdjustMap.get(key1String, key2String) != null) { //Value present, do nothing

					} else if(!key1String.equals(key2String))  { //Null value, if not the same type, set to small number

						marriageTypesToAdjustMap.put(key1String, key2String, 1.e-6);
					}
					//else {
					//	marriageTypesToAdjust.put(key1String, key2String, 0.); //Same sex and cross-region matches to have 0 frequency -> this is now handled in the matching closure, by not matching such couples
					//}
					// System.out.print(marriageTypesToAdjust.get(key1String, key2String) + " ");
				}
			}
		}

		//Iterate as on a matrix until the cumulative difference between frequencies in marriageTypesToAdjust and the targets from marriageTargetsByKey is smaller than the specified precision:
		int tmpCountInt = 0;
		double errorDouble = Double.MAX_VALUE;
		double precisionDouble = 1.e-1;
		while ((errorDouble >= precisionDouble) && tmpCountInt < 10)  { //100 iteration should be enough for the algorithm to converge, but this can be relaxed

			errorDouble = 0.;

			//These maps will hold row and column sums (updated in each iteration)
			LinkedHashMap<String, Double> rowSumsMap = new LinkedHashMap<String, Double>();
			LinkedHashMap<String, Double> colSumsMap = new LinkedHashMap<String, Double>();

			//These maps will hold row and column multipliers (updated in each iteration, and defined as Target/Sum_of_frequencies)
			LinkedHashMap<String, Double> rowMprMap = new LinkedHashMap<String, Double>();
			LinkedHashMap<String, Double> colMprMap = new LinkedHashMap<String, Double>();

			//Instead of iterating through rows and columns, go through every element of the map and add to the row / col sum depending on key1 and key2
			//marriageTypesToAdjust is a map, where key is a MultiKey with two values (Strings): first value identifies one type, second value identifies second type, value stores the frequency of matches.
			//Instead of iterating through rows and columns, can iterate through each cell of the map and add it to rowSum (and later on to colSum).
			MapIterator frequenciesIterator = marriageTypesToAdjustMap.mapIterator();

			while (frequenciesIterator.hasNext()) {

				frequenciesIterator.next();
				MultiKey tmpKeyMultiKey = (MultiKey) frequenciesIterator.getKey(); //Get MultiKey identifying each cell (mk.getKey(0) is row, mk.getKey(1) is column)
				double tmpValueDouble = 0.;
				if (rowSumsMap.get(tmpKeyMultiKey.getKey(0).toString()) == null) { //If null value in rowSumsMap, then just put the current value, otherwise add

					tmpValueDouble = ((Number) frequenciesIterator.getValue()).doubleValue();
				} else {

					tmpValueDouble = rowSumsMap.get(tmpKeyMultiKey.getKey(0).toString()) + ((Number) frequenciesIterator.getValue()).doubleValue();
				}

				//To get row sums add value to a map where key0 is the key
				rowSumsMap.put(tmpKeyMultiKey.getKey(0).toString(), tmpValueDouble);
			}
			//Get target by key and divide by row sum for that key to get row multiplier, same for column later on
			marriageTargetsByKey.keySet().iterator().forEachRemaining(key -> rowMprMap.put(key, marriageTargetsByKey.get(key)/rowSumsMap.get(key)));

			//After the first iteration, rowSum might = 0 which means division is undefined resulting in null rowMpr entry - adjust to 0 if that happens
			rowMprMap.keySet().iterator().forEachRemaining(key -> {

				if(rowMprMap.get(key).isNaN()) rowMprMap.put(key, 0.);
				if(rowMprMap.get(key).isInfinite()) rowMprMap.put(key, 0.);
			});

			//Now knowing the row multiplier, multiply entries in the frequency map (marriageTypesToAdjust)
			frequenciesIterator = marriageTypesToAdjustMap.mapIterator();
			while (frequenciesIterator.hasNext()) {

				frequenciesIterator.next();
				MultiKey tmpKeyMultiKey = (MultiKey) frequenciesIterator.getKey();
				double tmpValueDouble = ((Number) frequenciesIterator.getValue()).doubleValue();
				tmpValueDouble *= rowMprMap.get(tmpKeyMultiKey.getKey(0).toString());
				frequenciesIterator.setValue(tmpValueDouble);
			}

			//Have to repeat for columns:
			frequenciesIterator = marriageTypesToAdjustMap.mapIterator();
			while (frequenciesIterator.hasNext()) {

				frequenciesIterator.next();
				MultiKey tmpKeyMultiKey = (MultiKey) frequenciesIterator.getKey();
				double tmpValueDouble = 0.;
				if (colSumsMap.get(tmpKeyMultiKey.getKey(1).toString()) == null) {

					tmpValueDouble = ((Number) frequenciesIterator.getValue()).doubleValue();
				} else {

					tmpValueDouble = colSumsMap.get(tmpKeyMultiKey.getKey(1).toString()) + ((Number) frequenciesIterator.getValue()).doubleValue();
				}

				//To get column sums add value to a map where key1 is the key
				colSumsMap.put(tmpKeyMultiKey.getKey(1).toString(), tmpValueDouble);
			}

			marriageTargetsByKey.keySet().iterator().forEachRemaining(key -> colMprMap.put(key, marriageTargetsByKey.get(key)/colSumsMap.get(key)));

			//As for rows, make sure multipliers are defined
			colMprMap.keySet().iterator().forEachRemaining(key -> {

				if(colMprMap.get(key).isNaN()) colMprMap.put(key, 0.);
				if(colMprMap.get(key).isInfinite()) colMprMap.put(key, 0.);
			});

			//Now knowing the col multiplier, multiply entries in the map with frequencies
			frequenciesIterator = marriageTypesToAdjustMap.mapIterator();
			while (frequenciesIterator.hasNext()) {

				frequenciesIterator.next();
				MultiKey tmpKeyMultiKey = (MultiKey) frequenciesIterator.getKey();
				double tmpValueDouble = ((Number) frequenciesIterator.getValue()).doubleValue();
				tmpValueDouble *= colMprMap.get(tmpKeyMultiKey.getKey(1).toString());
				frequenciesIterator.setValue(tmpValueDouble);
			}

			//Calculate error as the cumulative difference between targets and row and column sums
			for (String key : marriageTargetsByKey.keySet()) {

				errorDouble += Math.abs(marriageTargetsByKey.get(key) - rowSumsMap.get(key));
				errorDouble += Math.abs(marriageTargetsByKey.get(key) - colSumsMap.get(key));
			}

			// System.out.println("Error is " + error + " and iteration is " + tmpCount);
			// System.out.print(".");
			tmpCountInt++;
		}

		//Print out adjusted frequencies
		marriageTypesToAdjustMap.keySet().iterator().forEachRemaining(key -> System.out.println(key + "=" + marriageTypesToAdjustMap.get(key)));

		/*
		 * Use matching method provided with JAS-mine:
		 */
		for(String key : keysStringSet) {

			for(String keyOther : keysStringSet) {

				//Get number of people that should be matched for key, keyOther combination
				int tmpTargetInt;
				if(marriageTypesToAdjustMap.get(key, keyOther) != null) {

					tmpTargetInt = (int) Math.round(((Number) marriageTypesToAdjustMap.getValue(key, keyOther)).doubleValue());
				} else tmpTargetInt = 0;

				//Check if for the combination of key, keyOther matches should be formed:
				if (tmpTargetInt > 0) {

					double initialSizeQ1Double = personsToMatch2.get(key).size(); //Number of people to match ("row")
					Set<Person> unmatchedQ1Set = new LinkedHashSet<Person>(); //Empty set to store people to match
					unmatchedQ1Set.addAll(personsToMatch2.get(key)); //Add people to match

//					unmatchedQ1.stream().iterator().forEachRemaining(persontodisp -> System.out.println("PID " + persontodisp.getKey().getId() + " HHID " + persontodisp.getHousehold().getKey().getId()));

//					System.out.println("Matching "+ initialSizeQ1 +"  persons from " + key + " to " + keyOther);

					double initialSizeQ2Double = personsToMatch2.get(keyOther).size(); //Number of people to match with ("column")
					Set<Person> unmatchedQ2FullSet = new HashSet<Person>(); // Empty set to store people to match with (note that HashSet does not preserve order, so we will sample at random from it)
					unmatchedQ2FullSet.addAll(personsToMatch2.get(keyOther)); //Add people to match with
					Set<Person> unmatchedQ2Set = new LinkedHashSet<Person>();

					//Keep only the number of people in unmatchedQ2 that is equal to the adjusted number of matches to create from marriageTypesToAdjust:
					Iterator<Person> unmatchedQ2FullSetIterator = unmatchedQ2FullSet.iterator();
					for(int n = 0; n < tmpTargetInt && unmatchedQ2FullSetIterator.hasNext(); n++) {

						Person person = unmatchedQ2FullSetIterator.next();
						unmatchedQ2Set.add(person);
					}

                    /*
					System.out.println("Currently matching " + key + " with " + keyOther + ". The target is " + tmpTarget + " and there are " + unmatchedQ1.size() +
										" people in Q1 and " + unmatchedQ2.size() + " in Q2. (Originally Q2 had " + unmatchedQ2full.size() + " people.");
					unmatchedQ2.stream().iterator().forEachRemaining(persontodisp -> System.out.println("PID " + persontodisp.getKey().getId() + " HHID " + persontodisp.getHousehold().getKey().getId()));
					 */
					Pair<Set<Person>, Set<Person>> unmatchedSetsPair = new Pair<>(unmatchedQ1Set, unmatchedQ2Set);
					//System.out.println("People in Q1 = " + unmatched.getFirst().size() + " People in Q2 = " + unmatched.getSecond().size());
					unmatchedSetsPair = IterativeSimpleMatching.getInstance().matching(
							unmatchedSetsPair.getFirst(), null, null, unmatchedSetsPair.getSecond(), null,

							//This closure calculates the score for potential couple
							new MatchingScoreClosure<Person>() {
								@Override
								public Double getValue(Person male, Person female) {

									return cohabitInnov.nextDouble(); //Random matching score
								}
							},

							new MatchingClosure<Person>() {
								@Override
								public void match(Person p1, Person p2) {

									//If two people have the same gender or different region, simply don't match and do nothing?
									if(p1.getDgn().equals(p2.getDgn()) || !p1.getRegion().equals(p2.getRegion())) {
										// throw new RuntimeException("Error - both parties to match have the same gender!");
									} else {

                                    	p1.setPartner(p2);
										p2.setPartner(p1);
										p1.setDcpyy(0); //Set years in partnership to 0
										p2.setDcpyy(0);
										p1.setDcpst(Dcpst.Partnered);
										p2.setDcpst(Dcpst.Partnered);

										// update benefit unit and household
										p1.setupNewBenefitUnit(true);
										p1.setToBePartnered(false);         //Probably could be removed
										p2.setToBePartnered(false);
										personsToMatch2.get(key).remove(p1); //Remove matched persons and keep everyone else in the matching queue
										personsToMatch2.get(keyOther).remove(p2);
										personsToMatch.get(p1.getDgn()).get(p1.getRegion()).remove(p1);
										personsToMatch.get(p2.getDgn()).get(p2.getRegion()).remove(p2);
										partnershipsCreated++;
									}
								}
							}
					);
				}
			}

			Set<Person> unmatchedSet = new LinkedHashSet<>();
			unmatchedSet.addAll(personsToMatch2.get(key));
			for (Person unmatchedPerson : unmatchedSet) {

				if (unmatchedPerson.getDgn().equals(Gender.Male)) malesUnmatched++;
				else if (unmatchedPerson.getDgn().equals(Gender.Female)) femalesUnmatched++;
			}

            /*
			personsToMatch2.get(key).clear();
			for(Gender gender: Gender.values()) {
				for(Region region : Parameters.getCountryRegions()) {
					personsToMatch.get(gender).get(region).clear();
				}
			}
			 */
		}

		// System.out.println("Total over all years of unmatched males is " + malesUnmatched + " and females " + femalesUnmatched);
		for (BenefitUnit benefitUnit : benefitUnits) {
			benefitUnit.updateOccupancy();
		}
	}

	/**
	 *
	 * PROCESS - UNION MATCHING OF SIMULATED POPULATION
	 * Matching Based On Earning Potential differential AND age differential
	 * (option C in Lia & Matteo's document 'BenefitUnit formation')
	 *
	 */
	int allMatches = 0;
	int yearMatches = 0;
	int unmatchedSize = 0;

	/**
	 *
	 * @param alignmentRun If true, real unions will not be formed. Instead, flags will be set for individual to indicate those who would have formed a union.
	 */
	protected void unionMatching(boolean alignmentRun) {

		Set<Person> matches = new LinkedHashSet<Person>();

		int countAttempts = 0;
		unmatchedSize = 0;
		for (Region region : Parameters.getCountryRegions()) {

			log.debug("Number of females to match: " + personsToMatch.get(Gender.Female).get(region).size() +
					", number of males to match: " + personsToMatch.get(Gender.Male).get(region).size());
			double initialMalesSize = personsToMatch.get(Gender.Male).get(region).size();
			double initialFemalesSize = personsToMatch.get(Gender.Female).get(region).size();
			Set<Person> unmatchedMales = new LinkedHashSet<Person>();
			Set<Person> unmatchedFemales = new LinkedHashSet<Person>();
			unmatchedMales.addAll(personsToMatch.get(Gender.Male).get(region));
			unmatchedFemales.addAll(personsToMatch.get(Gender.Female).get(region));
			ageDiffBound = Parameters.AGE_DIFFERENCE_INITIAL_BOUND;
			potentialHourlyEarningsDiffBound = Parameters.POTENTIAL_EARNINGS_DIFFERENCE_INITIAL_BOUND;

			// System.out.println("There are " + unmatchedMales.size() + " unmatched males and " + unmatchedFemales.size() + " unmatched females at the start");
			Pair<Set<Person>, Set<Person>> unmatched = new Pair<>(unmatchedMales, unmatchedFemales);
			do {

				// unmatched = IterativeSimpleMatching.getInstance().matching(
				unmatched = IterativeRandomMatching.getInstance().matching(

						unmatched.getFirst(),    //Males.  Allows to iterate (initially it is personsToMatch.get(Gender.Male).get(region))
						null,                     //No need for filter sub-population as group is already filtered by gender and region.
						null,                     //By not declaring a Comparator, the 'natural ordering' of the Persons will be used to determine the priority with which they get to choose their match.  In the case of Person, there is no natural ordering, so the Matching algorithm randomizes the males, so their priority to choose is random.
						unmatched.getSecond(),   //Females. Allows to iterate (initially it is personsToMatch.get(Gender.Female).get(region))
						null,                     //No need for filter sub-population as group is already filtered by gender and region.

						new MatchingScoreClosure<Person>() {
							@Override
							public Double getValue(Person male, Person female) {

								if (!male.getDgn().equals(Gender.Male)) {

									throw new RuntimeException("Error - male in getValue() does not actually have the Male gender type!");
								}
								if (!female.getDgn().equals(Gender.Female)) {

									throw new RuntimeException("Error - female in getValue() does not actually have the Female gender type!");
								}

								// Differentials are defined in a way that (in case we break symmetry later), a higher
								// ageDiff and a higher earningsPotentialDiff favours this person, on the assumption that we
								// all want younger, wealthier partners.  However, it is probably not going to be used as we
								// will probably end up just trying to minimise the square difference between that observed
								// in data and here.
								double ageDiff = male.getDag() - female.getDag();            //If male.getDesiredAgeDiff > 0, favours younger women
								double potentialHourlyEarningsDiff = male.getFullTimeHourlyEarningsPotential() - female.getFullTimeHourlyEarningsPotential();        //If female.getDesiredEarningPotential > 0, favours wealthier men
								double earningsMatch = (potentialHourlyEarningsDiff - female.getDesiredEarningsPotentialDiff());
								double ageMatch = (ageDiff - male.getDesiredAgeDiff());

								if (ageMatch < ageDiffBound && earningsMatch < potentialHourlyEarningsDiffBound) {

									// Score currently based on an equally weighted measure.  The Iterative (Simple and Random) Matching algorithm prioritises matching to the potential partner that returns the lowest score from this method (therefore, on aggregate we are trying to minimize the value below).
									return earningsMatch * earningsMatch + ageMatch * ageMatch;
								} else return Double.POSITIVE_INFINITY;        //Not to be included in possible partners
							}
						},

						new MatchingClosure<Person>() {
							@Override
							public void match(Person p1, Person p2) {        //The SimpleMatching.getInstance().matching() assumes the first collection in the argument (males in this case) is also the collection that the first argument of the MatchingClosure.match() is sampled from.

								if (alignmentRun) {
									p1.setHasTestPartner(true);
									p2.setHasTestPartner(true);
									unmatchedMales.remove(p1);
									unmatchedFemales.remove(p2);
									personsToMatch.get(p1.getDgn()).get(region).remove(p1);
									personsToMatch.get(p2.getDgn()).get(region).remove(p2);
									matches.add(p1);
								} else {

									if (!p1.getRegion().equals(p2.getRegion())) { //If persons to match have different regions, move female to male

										p2.setRegion(p1.getRegion());
									}
									if (p1.getDgn().equals(p2.getDgn())) {

										throw new RuntimeException("Error - both parties to match have the same gender!");
									} else {

										p1.setPartner(p2);
										p2.setPartner(p1);
										p1.setHousehold_status(Household_status.Couple);
										p2.setHousehold_status(Household_status.Couple);
										p1.setDcpyy(0); //Set years in partnership to 0
										p2.setDcpyy(0);
										p1.setDcpst(Dcpst.Partnered);
										p2.setDcpst(Dcpst.Partnered);

										//Update household
										p1.setupNewBenefitUnit(true);        //All the lines below are executed within the setupNewHome() method for both p1 and p2.  Note need to have partner reference before calling setupNewHome!

										unmatchedMales.remove(p1); //Remove matched people from unmatched sets (but keep those who were not matched so they can try next year)
										unmatchedFemales.remove(p2);
										personsToMatch.get(p1.getDgn()).get(region).remove(p1);
										personsToMatch.get(p2.getDgn()).get(region).remove(p2);
										matches.add(p1);
									}
								}
							}
						}
				);

				// Relax differential bounds for next iteration (in the case where there has not been a high enough proportion of matches)
				ageDiffBound *= Parameters.RELAXATION_FACTOR;
				potentialHourlyEarningsDiffBound *= Parameters.RELAXATION_FACTOR;
				countAttempts++;
				// System.out.println("unmatched males proportion " + unmatchedMales.size() / (double) initialMalesSize);
				// System.out.println("unmatched females proportion " + unmatchedFemales.size() / (double) initialFemalesSize);
			} while (
					(Math.min((unmatchedMales.size() / (double) initialMalesSize), (unmatchedFemales.size() / (double) initialFemalesSize)) >
							Parameters.UNMATCHED_TOLERANCE_THRESHOLD) &&
							(countAttempts < Parameters.MAXIMUM_ATTEMPTS_MATCHING));

			// System.out.println("There are (overall stock of)" + unmatchedMales.size() + " unmatched males and " + unmatchedFemales.size() + " unmatched females at the end. Number of matches made for " + region + " is " + matches.size());
			if (!alignmentRun) {
				for (Gender gender : Gender.values()) {

					// Turned off to allow unmatched people try again next year without the need to go through considerCohabitation process
					// personsToMatch.get(gender).get(region).clear();		//Nothing happens to unmatched people.  The next time they considerCohabitation, they will (probabilistically) have the opportunity to enter the matching pool again.
					unmatchedSize += personsToMatch.get(gender).get(region).size();
				}

				yearMatches = matches.size();
				allMatches += matches.size();
				// System.out.println("Total number of matches made in the year " + matches.size() + " and total number of matches in all years is " + allMatches);
				if (commentsOn) log.debug("Marriage matched.");
				for (BenefitUnit benefitUnit : benefitUnits) {
					benefitUnit.updateOccupancy();
				}
			}
		}
	}

	/**
	 * PROCESS - UNION MATCHING WITH REGION RELAXED
	 *
	 */
	protected void unionMatchingNoRegion(boolean alignmentRun) {
		int countAttempts = 0;

		double initialMalesSize = 0.;
		double initialFemalesSize = 0.;
		Set<Person> unmatchedMales = new LinkedHashSet<Person>();
		Set<Person> unmatchedFemales = new LinkedHashSet<Person>();

		Set<Person> matches = new LinkedHashSet<Person>();

		for (Region region : Parameters.getCountryRegions()) {

			initialMalesSize += personsToMatch.get(Gender.Male).get(region).size();
			initialFemalesSize += personsToMatch.get(Gender.Female).get(region).size();
			unmatchedMales.addAll(personsToMatch.get(Gender.Male).get(region));
			unmatchedFemales.addAll(personsToMatch.get(Gender.Female).get(region));
		}

//		System.out.println("There are " + unmatchedMales.size() + " unmatched males and " + unmatchedFemales.size() + " unmatched females at the start");

		Pair<Set<Person>, Set<Person>> unmatched = new Pair<>(unmatchedMales, unmatchedFemales);

		do {
//				unmatched = IterativeSimpleMatching.getInstance().matching(
			unmatched = IterativeRandomMatching.getInstance().matching(

					unmatched.getFirst(),    //Males.  Allows to iterate (initially it is personsToMatch.get(Gender.Male).get(region))

					null,        //No need for filter sub-population as group is already filtered by gender and region.

					null,    //By not declaring a Comparator, the 'natural ordering' of the Persons will be used to determine the priority with which they get to choose their match.  In the case of Person, there is no natural ordering, so the Matching algorithm randomizes the males, so their priority to choose is random.

					unmatched.getSecond(),    //Females. Allows to iterate (initially it is personsToMatch.get(Gender.Female).get(region))

					null,        //No need for filter sub-population as group is already filtered by gender and region.

					new MatchingScoreClosure<Person>() {
						@Override
						public Double getValue(Person male, Person female) {
							if (!male.getDgn().equals(Gender.Male)) {
								throw new RuntimeException("Error - male in getValue() does not actually have the Male gender type!");
							}
							if (!female.getDgn().equals(Gender.Female)) {
								throw new RuntimeException("Error - female in getValue() does not actually have the Female gender type!");
							}

							// Differentials are defined in a way that (in case we break symmetry later), a higher ageDiff
							// and a higher earningsPotentialDiff favours this person, on the assumption that we all want
							// younger, wealthier partners.  However, it is probably not going to be used as we will
							// probably end up just trying to minimise the square difference between that observed in data
							// and here.
							double ageDiff = male.getDag() - female.getDag();            //If male.getDesiredAgeDiff > 0, favours younger women
							double potentialHourlyEarningsDiff = male.getFullTimeHourlyEarningsPotential() - female.getFullTimeHourlyEarningsPotential();        //If female.getDesiredEarningPotential > 0, favours wealthier men
							double earningsMatch = (potentialHourlyEarningsDiff - female.getDesiredEarningsPotentialDiff());
							double ageMatch = (ageDiff - male.getDesiredAgeDiff());

							if (ageMatch < ageDiffBound && earningsMatch < potentialHourlyEarningsDiffBound) {
								// Score currently based on an equally weighted measure.  The Iterative (Simple and Random) Matching algorithm prioritises matching to the potential partner that returns the lowest score from this method (therefore, on aggregate we are trying to minimize the value below).

								return earningsMatch * earningsMatch + ageMatch * ageMatch;
							} else return Double.POSITIVE_INFINITY;        //Not to be included in possible partners
						}
					},

					new MatchingClosure<Person>() {
						@Override
						public void match(Person p1, Person p2) {        //The SimpleMatching.getInstance().matching() assumes the first collection in the argument (males in this case) is also the collection that the first argument of the MatchingClosure.match() is sampled from.
							//						log.debug("Person " + p1.getKey().getId() + " marries person " + p2.getKey().getId());
							Region originalRegionP2 = p2.getRegion();

							if (alignmentRun) {
								p1.setHasTestPartner(true);
								p2.setHasTestPartner(true);
								unmatchedMales.remove(p1); //Remove matched people from unmatched sets (but keep those who were not matched so they can try next year)
								unmatchedFemales.remove(p2);
								personsToMatch.get(p1.getDgn()).get(p1.getRegion()).remove(p1);
								personsToMatch.get(p2.getDgn()).get(originalRegionP2).remove(p2);
								matches.add(p1);
							} else {

								if (!p1.getRegion().equals(p2.getRegion())) { //If persons to match have different regions, move female to male

									p2.setRegion(p1.getRegion());
	//								System.out.println("Region changed");
								}
								if (p1.getDgn().equals(p2.getDgn())) {

									throw new RuntimeException("Error - both parties to match have the same gender!");
								} else {

									p1.setPartner(p2);
									p2.setPartner(p1);
									p1.setHousehold_status(Household_status.Couple);
									p2.setHousehold_status(Household_status.Couple);
									p1.setDcpyy(0); //Set years in partnership to 0
									p2.setDcpyy(0);
									p1.setDcpst(Dcpst.Partnered);
									p2.setDcpst(Dcpst.Partnered);

									//Update household
									p1.setupNewBenefitUnit(true);        //All the lines below are executed within the setupNewHome() method for both p1 and p2.  Note need to have partner reference before calling setupNewHome!

									unmatchedMales.remove(p1); //Remove matched people from unmatched sets (but keep those who were not matched so they can try next year)
									unmatchedFemales.remove(p2);
									personsToMatch.get(p1.getDgn()).get(p1.getRegion()).remove(p1);
									personsToMatch.get(p2.getDgn()).get(originalRegionP2).remove(p2);
									matches.add(p1);
								}
							}
						}
					}
			);

			//Relax differential bounds for next iteration (in the case where there has not been a high enough proportion of matches)
			ageDiffBound *= Parameters.RELAXATION_FACTOR;
			potentialHourlyEarningsDiffBound *= Parameters.RELAXATION_FACTOR;
			countAttempts++;
//			System.out.println("unmatched males proportion " + unmatchedMales.size() / (double) initialMalesSize);
//			System.out.println("unmatched females proportion " + unmatchedFemales.size() / (double) initialFemalesSize);
		} while ((Math.min((unmatchedMales.size() / (double) initialMalesSize), (unmatchedFemales.size() / (double) initialFemalesSize)) > Parameters.UNMATCHED_TOLERANCE_THRESHOLD) && (countAttempts < Parameters.MAXIMUM_ATTEMPTS_MATCHING));

		if (!alignmentRun) {
			allMatches += matches.size();
		} else {
			// Clear set if used within the matching procedure
			for (Gender gender : Gender.values()) {
				for (Region region : Region.values()) {
					personsToMatch.get(gender).get(region).clear();
				}
			}
		}
//		System.out.println("There are " + unmatchedMales.size() + " unmatched males and " + unmatchedFemales.size() + " unmatched females at the end. Number of matches made " + matches.size() + " and total number of matches in all years is " + allMatches);
	}

	private void socialCareMarketClearning() {

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

	public void activityAlignmentSingleMales() {
		double utilityAdjustment = Parameters.getTimeSeriesValue(getYear(), TimeSeriesVariable.UtilityAdjustmentSingleMales);
		ActivityAlignment activityAlignmentSingleMales = new ActivityAlignment(persons, benefitUnits, Parameters.getCoeffLabourSupplyUtilityMales(), new String[]{"MaleLeisure"}, Occupancy.Single_Male, utilityAdjustment);
		RootSearch search = getRootSearch(utilityAdjustment, activityAlignmentSingleMales, 1.0E-2, 1.0E-2, 0.5); // epsOrdinates and epsFunction determine the stopping condition for the search.
		if (search.isTargetAltered()) {
			Parameters.putTimeSeriesValue(getYear(), search.getTarget()[0], TimeSeriesVariable.UtilityAdjustmentSingleMales); // If adjustment is altered from the initial value, update the map
			System.out.println("Utility adjustment for single males was " + search.getTarget()[0]);
		}
	}

	public void activityAlignmentSingleFemales() {
		double utilityAdjustment = Parameters.getTimeSeriesValue(getYear(), TimeSeriesVariable.UtilityAdjustmentSingleFemales);
		ActivityAlignment activityAlignmentSingleFemales = new ActivityAlignment(persons, benefitUnits, Parameters.getCoeffLabourSupplyUtilityFemales(), new String[]{"FemaleLeisure"}, Occupancy.Single_Female, utilityAdjustment);
		RootSearch search = getRootSearch(utilityAdjustment, activityAlignmentSingleFemales, 1.0E-2, 1.0E-2, 2); // epsOrdinates and epsFunction determine the stopping condition for the search.
		if (search.isTargetAltered()) {
			Parameters.putTimeSeriesValue(getYear(), search.getTarget()[0], TimeSeriesVariable.UtilityAdjustmentSingleFemales); // If adjustment is altered from the initial value, update the map
			System.out.println("Utility adjustment for single females was " + search.getTarget()[0]);
		}
	}

	public void activityAlignmentCouples() {
		double utilityAdjustment = Parameters.getTimeSeriesValue(getYear(), TimeSeriesVariable.UtilityAdjustmentCouples);
		ActivityAlignment activityAlignmentCouples = new ActivityAlignment(persons, benefitUnits, Parameters.getCoeffLabourSupplyUtilityCouples(), new String[]{"MaleLeisure","FemaleLeisure"}, Occupancy.Couple, utilityAdjustment);
		RootSearch search = getRootSearch(utilityAdjustment, activityAlignmentCouples, 1.0E-2, 1.0E-2, 1); // epsOrdinates and epsFunction determine the stopping condition for the search.
		if (search.isTargetAltered()) {
			Parameters.putTimeSeriesValue(getYear(), search.getTarget()[0], TimeSeriesVariable.UtilityAdjustmentCouples); // If adjustment is altered from the initial value, update the map
			System.out.println("Utility adjustment for couples was " + search.getTarget()[0]);
		}
	}

	private void partnershipAlignment() {
		double partnershipAdjustment = Parameters.getTimeSeriesValue(getYear(), TimeSeriesVariable.PartnershipAdjustment); // Initial values of adjustment to be applied to considerCohabitation probit
		PartnershipAlignment partnershipAlignment = new PartnershipAlignment(persons, partnershipAdjustment);
		RootSearch search = getRootSearch(partnershipAdjustment, partnershipAlignment, 1.0E-2, 1.0E-2, 4); // epsOrdinates and epsFunction determine the stopping condition for the search. For partnershipAlignment error term is the difference between target and observed share of partnered individuals.
		if (search.isTargetAltered()) {
			Parameters.putTimeSeriesValue(getYear(), search.getTarget()[0], TimeSeriesVariable.PartnershipAdjustment); // If adjustment is altered from the initial value, update the map
			System.out.println("Partnership adjustment value was " + search.getTarget()[0]);
		}
	}

	@NotNull
	private static RootSearch getRootSearch(double initialAdjustment, IEvaluation alignmentClass, double epsOrdinates, double epsFunction, double modifier) {
		double[] startVal = new double[] {initialAdjustment}; // Starting values for the adjustment
		double[] lowerBound = new double[] {initialAdjustment - modifier};
		double[] upperBound = new double[] {initialAdjustment + modifier};
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
	 * PROCESS - ALIGN THE SHARE OF EMPLOYED IN THE SIMULATED POPULATION
	 */

	private void employmentAlignment() {

		//Create a nested map to store persons by gender and region
		LinkedHashMap<Gender, LinkedHashMap<Region, Set<Person>>> personsByGenderAndRegion;
		personsByGenderAndRegion = new LinkedHashMap<Gender, LinkedHashMap<Region, Set<Person>>>();

		EnumSet<Region> regionEnumSet = null;
		if (country.equals(Country.IT)) {
			regionEnumSet = EnumSet.of(Region.ITC, Region.ITH, Region.ITI, Region.ITF, Region.ITG);
		} else if (country.equals(Country.UK)) {
			regionEnumSet = EnumSet.of(Region.UKC, Region.UKD, Region.UKE, Region.UKF, Region.UKG, Region.UKH, Region.UKI, Region.UKJ, Region.UKK, Region.UKL, Region.UKM, Region.UKN);
		}

		for (Gender gender : Gender.values()) {
			personsByGenderAndRegion.put(gender, new LinkedHashMap<Region, Set<Person>>());
			for (Region region : regionEnumSet) {
				personsByGenderAndRegion.get(gender).put(region, new LinkedHashSet<Person>());
			}
		}

		//Iterate over persons and add them to the nested map above
		for (Person person : persons) {
			if (person.getDag() >= 18 && person.getDag() <= 64) {
				personsByGenderAndRegion.get(person.getDgn()).get(person.getRegion()).add(person);
			}
		}

		//For all gender and region combinations, compare the share of employed persons with the alignment target
		for (Gender gender : Gender.values()) {
			for (Region region : regionEnumSet) {
				double numberEmployed = 0;
				Set<Person> personsToIterateOver = personsByGenderAndRegion.get(gender).get(region);

				for (Person person : personsToIterateOver) {
					numberEmployed += person.getEmployed();
				}

				double sizeSimulatedSet = personsToIterateOver.size();

				double shareEmployedSimulated = numberEmployed/sizeSimulatedSet;
				double shareEmployedTargeted = ((Number) Parameters.getEmploymentAlignment().getValue(gender.toString(), region.toString(), year)).doubleValue();

				int targetNumberEmployed = (int) (shareEmployedTargeted*sizeSimulatedSet);


				//Simulated share of employment exceeds projections => move some individuals at random to non-employment
				if ((int) numberEmployed > targetNumberEmployed) {
					new ResamplingAlignment<Person>().align(
							personsToIterateOver,
							null,
							new AlignmentOutcomeClosure<Person>() {
								@Override
								public boolean getOutcome(Person person) {
									return person.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed);
								}

								@Override
								public void resample(Person person) {
									person.setLes_c4(Les_c4.NotEmployed);
									person.setLabourSupplyWeekly(Labour.ZERO);
								}
							},
							targetNumberEmployed);
				}
			}
		}

	}

	/**
	 *
	 * PROCESS - ALIGN THE SHARE OF STUDENTS IN THE SIMULATED POPULATION
	 *
	 */
	private void inSchoolAlignment() {

		int numStudents = 0;
		int num16to29 = 0;
		ArrayList<Person> personsLeavingSchool = new ArrayList<Person>();
		for (Person person : persons) {
			if (person.getDag() > 15 && person.getDag() < 30) { //Could introduce separate alignment for different age groups, but this is more flexible as it depends on the regression process within the larger alignment target
				num16to29++;
				if (person.getLes_c4().equals(Les_c4.Student)) {
					numStudents++;
				}
				if (person.isToLeaveSchool()) { //Only those who leave school for the first time have toLeaveSchool set to true
					personsLeavingSchool.add(person);
				}
			}
		}

		int targetNumberOfPeopleLeavingSchool = numStudents - (int)( (double)num16to29 * ((Number) Parameters.getStudentShareProjections().getValue(country.toString(), year)).doubleValue() );

		System.out.println("Number of students < 30 is " + numStudents + " Persons set to leave school " + personsLeavingSchool.size() + " Number of people below 30 " + num16to29
				+ " Target number of people leaving school " + targetNumberOfPeopleLeavingSchool);

		if (targetNumberOfPeopleLeavingSchool <= 0) {
			for(Person person : personsLeavingSchool) {
				person.setToLeaveSchool(false);                    //Best case scenario is to prevent anyone from leaving school in this year as the target share of students is higher than the number of students.  Although we cannot match the target, this is the nearest we can get to it.
				if(Parameters.systemOut) {
					System.out.println("target number of school leavers is not positive.  Force all school leavers to stay at school.");
				}
			}
		} else if (targetNumberOfPeopleLeavingSchool < personsLeavingSchool.size()) {
			if(Parameters.systemOut) {
				System.out.println("Schooling alignment: target number of students is " + targetNumberOfPeopleLeavingSchool);
			}
			new ResamplingAlignment<Person>().align(
					personsLeavingSchool,
					null,
					new AlignmentOutcomeClosure<Person>() {
						@Override
						public boolean getOutcome(Person agent) {
							return agent.isToLeaveSchool();
						}

						@Override
						public void resample(Person agent) {
							agent.setToLeaveSchool(false);
						}
					},
					targetNumberOfPeopleLeavingSchool);

			int numPostAlign = 0;
			for(Person person : persons) {
				if(person.isToLeaveSchool()) {
					numPostAlign++;
				}
			}
			System.out.println("Schooling alignment: aligned number of students is " + numPostAlign);
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
			double highEducationRateTarget = ((Number)Parameters.getHighEducationRateInYear().getValue(year, gender.toString())).doubleValue();
			int numPersonsWithHighEduAlignmentTarget = (int) (highEducationRateTarget * (double)numPersonsOfThisGender);
			//Medium Education
			double lowEducationRateTarget = ((Number)Parameters.getLowEducationRateInYear().getValue(year, gender.toString())).doubleValue();
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
	 *
	 * PROCESS - FERTILITY ALIGNMENT OF SIMULATED POPULATION
	 *
	 */
	private void fertilityAlignment() {

		//With new fertility alignment for the target number instead of fertility rate

		for (Region region: Parameters.getCountryRegions()) {
			double fertilityRate = Parameters.getFertilityRateByRegionYear(region, year) / 1000.0;

			int numberNewbornsProjected = 0;
			for (Gender gender : Gender.values()) {
				numberNewbornsProjected += Parameters.getPopulationProjections(gender, region, 0, year);
			}

			int simNewborns = 0;
			for (Person person : persons) {
				if (person.getRegion().equals(region) && person.isToGiveBirth()) {
					simNewborns++;
				}
			}

			int targetNumberNewborns = (int) Math.round(numberNewbornsProjected/scalingFactor);

			new ResamplingAlignment<Person>().align(
					getPersons(),
					new FertileFilter<Person>(region),
					new AlignmentOutcomeClosure<Person>() {
						@Override
						public boolean getOutcome(Person agent) {
							//Note: fertility method runs before the alignment
							return agent.isToGiveBirth(); //Returns either true or false to be used by the closure getOutcome
						}

						@Override
						public void resample(Person agent) {
							if (!agent.isToGiveBirth()) {
								agent.setToGiveBirth(true);
							} else {
								agent.setToGiveBirth(false);
							}
						}
					},
					targetNumberNewborns,//align to this number of newborns per region
					20
			);

/*
			//Type of alignment performed depends on whether weights are used, or not
			AbstractProbabilityAlignment<Person> alignmentProcedure; 
			
			if(useWeights) { //If using weights use Logit Scaling Binary Weighted Alignment
				alignmentProcedure = new LogitScalingBinaryWeightedAlignment<Person>(); 
			}
			else { //Otherwise, use unweighted Logit Scaling Binary Alignment
				alignmentProcedure = new LogitScalingBinaryAlignment<Person>();
			}
			
			alignmentProcedure.align(
					getPersons(), //Collection to align: persons from person set (but note the filter applied below)
					new FertileFilter<Person>(region), 		//New restriction is that the Female needs to have a partner to be 'fertile' (i.e. considered in the fertility alignment process). This filters the collection specified above. 
					new AlignmentProbabilityClosure<Person>() { //a piece of code that i) for each element of the filtered collection computes a probability for the event (in the case that the alignment method is aligning probabilities, as in the SBD algorithm)

						@Override
						public double getProbability(Person agent) { //i) calculate probability for each element of the filtered collection
							if(agent.getDag() <= 29 && agent.getLes_c3().equals(Les_c4.Student) && agent.isLeftEducation() == false) { //If age below or equal to 29 and in continuous education follow process F1a
								return Parameters.getRegFertilityF1a().getProbability(agent, Person.DoublesVariables.class);
							}
							else { //Otherwise if not in continuous education, follow process F1b 
								return Parameters.getRegFertilityF1b().getProbability(agent, Person.DoublesVariables.class); 
							}
						}

						@Override
						public void align(Person agent, double alignedProabability) {
							if ( RegressionUtils.event( alignedProabability, SimulationEngine.getRnd() ) ) { //RegressionUtils.event samples an event where all events have equal probability
								agent.setToGiveBirth(true);
							} else agent.setToGiveBirth(false);
						}
					},
					fertilityRate //the share or number of elements in the filtered collection that are expected to experience the transition. In this case, fertility rate by region and year
			);
*/
		}
	}

	/**
	 *
	 * PROCESS TO DETERMINE FERTILITY WITHOUT ALIGNMENT TO TARGET FERTILITY RATES
	 *
	 */
	private void fertility() {
		for (Region region: Parameters.getCountryRegions()) { //Select fertile persons from each region and determine if they give birth
			List<Person> fertilePersons = new ArrayList<Person>();
			CollectionUtils.select(getPersons(), new FertileFilter<Person>(region), fertilePersons);

			for (Person person : fertilePersons) {

				if (country.equals(Country.UK)) {

					if (person.getDag() <= 29 && person.getLes_c4().equals(Les_c4.Student) && person.isLeftEducation() == false) { //If age below or equal to 29 and in continuous education follow process F1a
						person.setToGiveBirth(fertilityInnov.nextDouble() < Parameters.getRegFertilityF1a().getProbability(person, Person.DoublesVariables.class)); //If regression event true, give birth
//					System.out.println("Followed process F1a");
					} else { //Otherwise if not in continuous education, follow process F1b
						person.setToGiveBirth(fertilityInnov.nextDouble() < Parameters.getRegFertilityF1b().getProbability(person, Person.DoublesVariables.class)); //If regression event true, give birth
//					System.out.println("Followed process F1b");
					}
				} else if (country.equals(Country.IT)) { //In Italy, there is a single fertiltiy process
					person.setToGiveBirth(fertilityInnov.nextDouble() < Parameters.getRegFertilityF1().getProbability(person, Person.DoublesVariables.class));
				}
			}
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
		log.debug("Year: " + year + ", Elapsed time: " + (System.currentTimeMillis() - elapsedTime)/1000. + " seconds.");
	}


	/**
	 *
	 * CREATE INPUT DATABASE TABLES BASED ON .txt FILES CREATED IN GUI DIALOG FROM EUROMOD
	 *
	 */
	private void inputDatabaseInteraction() throws InterruptedException {

		Connection conn = null;
		Statement stat = null;

		boolean retry = false;

		do {
			try {
				Class.forName("org.h2.Driver");
				System.out.print("Reading from database at " + DatabaseUtils.databaseInputUrl);
				try {
					conn = DriverManager.getConnection("jdbc:h2:"+DatabaseUtils.databaseInputUrl + ";AUTO_SERVER=FALSE", "sa", "");
				}
				catch (SQLException e) {
					log.info(e.getMessage());
					throw new RuntimeException("SQL Exception! " + e.getMessage());
				}
//        	//Create input database from input file (population_[country].csv)
//			if(refreshInputDatabase) {
//				SQLdataParser.createDatabaseTablesFromCSVfile(country, Parameters.getInputFileName(), startYear, conn);
//			}
				System.out.print("... Success!\n");
				retry = false;

				//If user chooses start year that is higher than the last available initial population, last available population should be used but with uprated monetary values
				boolean uprateInitialPopulation = (startYear > Parameters.getMaxStartYear());

				stat = conn.createStatement();

				if (isFirstRun) {
					//Create database tables to be used in simulation from country-specific tables
					String[] tableNames = new String[]{"PERSON", "DONORPERSON", "BENEFITUNIT", "DONORTAXUNIT", "HOUSEHOLD"};
					String[] tableNamesInitial = new String[]{"PERSON", "BENEFITUNIT", "HOUSEHOLD"};
					String[] tableNamesDonor = new String[]{"DONORPERSON", "DONORTAXUNIT"};
					for (String tableName : tableNamesDonor) {
						stat.execute("DROP TABLE IF EXISTS " + tableName);
						stat.execute("CREATE TABLE " + tableName + " AS SELECT * FROM " + tableName + "_" + country);
					}
					for (String tableName : tableNamesInitial) {
						stat.execute("DROP TABLE IF EXISTS " + tableName);
						if (uprateInitialPopulation) {
							stat.execute("CREATE TABLE " + tableName + " AS SELECT * FROM " + tableName + "_" + country + "_" + Parameters.getMaxStartYear()); // Load the last available initial population from all available in tables of the database
						} else {
							stat.execute("CREATE TABLE " + tableName + " AS SELECT * FROM " + tableName + "_" + country + "_" + startYear); // Load the country-year specific initial population from all available in tables of the database
						}
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
		} while (retry);
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

		System.out.println("Creating population structures");


		/*
		 * initialisations
		 */
		persons = new LinkedHashSet<Person>();
		benefitUnits = new LinkedHashSet<BenefitUnit>();
		households = new LinkedHashSet<Household>(); //Also initialise set of families to which benefitUnits belong

		double aggregatePersonsWeight = 0.;            //Aggregate Weight of simulated individuals (a weighted sum of the simulated individuals)
		double aggregateHouseholdsWeight = 0.;        //Aggregate Weight of simulated benefitUnits (a weighted sum of the simulated households)


        /*
         * load data from the input database
         * NOTE: don't need to worry about duplicates, as database ensures all items are unique
         * NOTE: use lists to ensure that simulated population is replicable
         */

		//Persons
		List<Person> inputPersonList = (List<Person>) DatabaseUtils.loadTable(Person.class);               //RHS returns an ArrayList
        if (inputPersonList.isEmpty()) {
			throw new RuntimeException("Error - there are no Persons for country " + country + " in the input database");
        }

        //Benefit units
		List<BenefitUnit> inputBenefitUnitList = (List<BenefitUnit>) DatabaseUtils.loadTable(BenefitUnit.class);    //RHS returns an ArrayList
        if (inputBenefitUnitList.isEmpty()) {
			throw new RuntimeException("Error - there are no Benefit Units for country " + country + " in the input database");
        }

        //Households
		List<Household> inputHouseholdList = (List<Household>) DatabaseUtils.loadTable(Household.class);
		if (inputHouseholdList.isEmpty()) {
			throw new RuntimeException("Error - there are no Households for country " + country + " in the input database");
		}

		// establish links between units
		LinkedList<BenefitUnit> benefitUnitsToAllocate = new LinkedList<>(inputBenefitUnitList);
		LinkedList<Person> personsToAllocate = new LinkedList<>(inputPersonList);
		Double minWeight = null;
		for (Household household : inputHouseholdList) {

			Iterator<BenefitUnit> benefitUnitIterator = benefitUnitsToAllocate.iterator();
			LinkedList<Person> orphans = new LinkedList<>();
			while (benefitUnitIterator.hasNext()) {

				BenefitUnit benefitUnit = benefitUnitIterator.next();
				if (benefitUnit.getIdHousehold().equals(household.getId())) {
					// found benefit unit in household

					benefitUnit.setHousehold(household);
					benefitUnitIterator.remove();
					Iterator<Person> personIterator = personsToAllocate.iterator();
					while(personIterator.hasNext()) {

						Person person = personIterator.next();
						if (person.getIdBenefitUnit().equals(benefitUnit.getId())) {
							// found member of benefit unit

							person.setBenefitUnit(benefitUnit);
							if (!person.getBenefitUnit().getPersonsInBU().contains(person)) {
								orphans.add(person);
							}
							personIterator.remove();
						}
					}

					// establish responsible adults and relationship status
					if ( benefitUnit.getMale() == null && benefitUnit.getFemale() == null ) {
						household.removeBenefitUnit(benefitUnit);
						for (Person person : benefitUnit.getChildren()) {
							orphans.add(person);
						}
					} else {
						for (Person person : benefitUnit.getPersonsInBU()) {
							if (person.getDag() < Parameters.AGE_TO_BECOME_RESPONSIBLE) {
								if (person.getIdPartner() != null) {
									orphans.add(person);
								} else {
									checkChildRelations(benefitUnit, person);
								}
							} else if (person.getIdPartner()!=null) {
								for (Person partner : benefitUnit.getPersonsInBU()) {
									if (person.getIdPartner().equals(partner.getKey().getId())) person.setPartner(partner);
								}
							}
							if (!orphans.contains(person)) {
								person.setAdditionalFieldsInInitialPopulation();
							}
						}
						benefitUnit.initializeFields();
					}
				}
			}
			if (!orphans.isEmpty()) {

				Iterator<Person> orphansIterator = orphans.iterator();
				while (orphansIterator.hasNext()) {

					Person orphan = orphansIterator.next();
					Person partner = null;
					Person parent = null;
					for(BenefitUnit benefitUnit : household.getBenefitUnitSet()) {

						for (Person person : benefitUnit.getPersonsInBU()) {

							if (person != orphan) {
								if (orphan.getIdPartner() != null && orphan.getIdPartner().equals(person.getKey().getId())) {
									partner = person;
								}
								if (orphan.getIdMother() != null && orphan.getIdMother().equals(person.getKey().getId())) {
									parent = person;
								}
								if (orphan.getIdFather() != null && orphan.getIdFather().equals(person.getKey().getId())) {
									parent = person;
								}
							}
						}
					}
					if (parent != null) {

						orphansIterator.remove();
						if (orphan.getIdPartner() != null) {
							orphan.setPartner(null);
						}
						if (partner != null) {
							partner.setPartner(null);
						}
						orphan.setBenefitUnit(null);
						orphan.setBenefitUnit(parent.getBenefitUnit());
						checkChildRelations(parent.getBenefitUnit(), orphan);
						orphan.setAdditionalFieldsInInitialPopulation();
						parent.getBenefitUnit().initializeFields();
					} else if (partner != null) {

						orphansIterator.remove();
						orphan.setIdMother((Person)null);
						orphan.setIdFather((Person)null);
						orphan.setDag(Parameters.AGE_TO_BECOME_RESPONSIBLE);
						orphan.setBenefitUnit(null);
						orphan.setBenefitUnit(partner.getBenefitUnit());
						orphan.setAdditionalFieldsInInitialPopulation();
						partner.getBenefitUnit().initializeFields();
					}
				}
			}
			if (!orphans.isEmpty()) {
				throw new RuntimeException("Children not associated with a qualifying benefit unit included in population load.");
			}
			if (minWeight == null || household.getWeight() < minWeight) {
				minWeight = household.getWeight();
			}
		}
		if (!benefitUnitsToAllocate.isEmpty()) {
			throw new RuntimeException("Not all benefit units associated with a household at population load.");
		}
		if (!personsToAllocate.isEmpty()) {
			throw new RuntimeException("Not all persons associated with a benefit unit at population load.");
		}


		/**********************************************************
		 * instantiate simulated population
		 **********************************************************/
        if (!useWeights) {
			// Expand population, sample, and remove weights

			StopWatch stopwatch = new StopWatch();
			stopwatch.start();
			System.out.println("Will expand the initial population to " + popSize + " individuals, each of whom has an equal weight.");

			// approach to resampling considered here is designed to allow for surveys that over-sample some population
			// subgroups. In this case you may have many similar observations in the sample all with low survey weights
			// so that replicating by a factor adjustment on weight of each observation may result in none of the
			// observations being included in the simulated sample (unless the simulated sample was very large).
			List<Household> randomHouseholdSampleList = new LinkedList<>();
			double replicationFactor = 1.0 / minWeight;    // ensures each sampled household represented at least 5 times in list
			for (Household household : inputHouseholdList) {

				int numberOfClones = (int) Math.round(household.getWeight() * replicationFactor);
				for (int ii=0; ii<numberOfClones; ii++) {
					randomHouseholdSampleList.add(household);
				}
			}
			Collections.shuffle(randomHouseholdSampleList, initialiseInnov);
			ListIterator<Household> randomHouseholdSampleListIterator = randomHouseholdSampleList.listIterator();
			while (randomHouseholdSampleListIterator.hasNext()) {

				Household originalHousehold = randomHouseholdSampleListIterator.next();
				Household newHousehold = cloneHousehold(originalHousehold);
				newHousehold.resetWeights(1.0d);
				if (persons.size() >= popSize ) break;
			}

			stopwatch.stop();
			System.out.println("Time elapsed " + stopwatch.getTime() + " milliseconds");
        } else {
			// use population weights

			households.addAll(inputHouseholdList);
			benefitUnits.addAll(inputBenefitUnitList);
			persons.addAll(inputPersonList);
        }

		// finalise
		log.info("Number of simulated individuals (persons.size()) is " + persons.size() + " living in " + benefitUnits.size() + " simulated benefitUnits.");
		log.info("Representative size of population is " + aggregatePersonsWeight + " living in " + aggregateHouseholdsWeight + " representative benefitUnits.");
		initialHoursWorkedWeekly = null;
		System.gc();
	}

	private void checkChildRelations(BenefitUnit benefitUnit, Person child) {

		if (child.getIdFather() != null && benefitUnit.getIdMale() != null && !child.getIdFather().equals(benefitUnit.getIdMale())) {
			if (child.getIdPartner().equals(benefitUnit.getIdMale())) {
				benefitUnit.removePerson(child);
				child.setDag(Parameters.AGE_TO_BECOME_RESPONSIBLE);
				child.setBenefitUnit(benefitUnit);
			} else {
				throw new RuntimeException("Problem identifying father at population load.");
			}
		} else if (child.getIdFather() != null && benefitUnit.getMale() == null) {
			child.setIdFather((Long) null);
		} else if (child.getIdFather() == null && benefitUnit.getMale() != null) {
			child.setIdFather(benefitUnit.getMale().getKey().getId());
		}
		if (child.getIdMother() != null && benefitUnit.getIdFemale() != null && !child.getIdMother().equals(benefitUnit.getIdFemale())) {
			if (child.getIdPartner().equals(benefitUnit.getIdFemale())) {
				benefitUnit.removePerson(child);
				child.setDag(Parameters.AGE_TO_BECOME_RESPONSIBLE);
				child.setBenefitUnit(benefitUnit);
			} else if (!benefitUnit.getIdFemale().equals(child.getKey().getId())) {
				throw new RuntimeException("Problem identifying mother at population load.");
			}
		} else if (child.getIdMother() != null && benefitUnit.getFemale() == null) {
			child.setIdMother((Long) null);
		} else if (child.getIdMother() == null && benefitUnit.getFemale() != null) {
			child.setIdMother(benefitUnit.getFemale().getKey().getId());
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

	public void removePerson(Person person) {

		// remove from benefit unit
		if (person.getBenefitUnit() != null) person.getBenefitUnit().removePerson(person);

		// remove person from model
		if (persons.contains(person)) {

			boolean removed = persons.remove(person);
			if(!removed) {
				throw new RuntimeException("Person " + person.getKey().getId() + " could not be removed from persons set");
			}
		}
	}

	public void removeBenefitUnit(BenefitUnit benefitUnit) {

		// remove all benefit unit members from model
		for (Person person : benefitUnit.getPersonsInBU()) {
			person.setBenefitUnit(null);
			removePerson(person);
		}

		// remove benefit unit from the household
		if (benefitUnit.getHousehold() != null) benefitUnit.getHousehold().removeBenefitUnit(benefitUnit);

		// remove benefit unit from model
		if ( benefitUnits.contains(benefitUnit) ) {

			boolean removed = benefitUnits.remove(benefitUnit);
			if(!removed) {
				throw new RuntimeException("BenefitUnit " + benefitUnit.getKey().getId() + " could not be removed from benefitUnits set");
			}
		}
	}

	public void removeHousehold(Household household) {

		// remove any child benefit units
		for (BenefitUnit benefitUnit : household.getBenefitUnitSet()) {
			benefitUnit.setHousehold(null);
			removeBenefitUnit(benefitUnit);
		}

		if (households.contains(household)) {

			boolean removed = households.remove(household);
			if (!removed) {
				throw new RuntimeException("Household " + household.getId() + " could not be removed from Households set");
			}
		}
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

	public int getAllMatches() {
		return allMatches;
	}

	public int getUnmatchedSize() {
		return unmatchedSize;
	}

	public int getYearMatches() {
		return yearMatches;
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
				EntityManager em = HibernateUtil.getEntityManagerFactory(propertyMap).createEntityManager();
				txn = em.getTransaction();
				txn.begin();
				String query = "SELECT tu FROM DonorTaxUnit tu LEFT JOIN FETCH tu.policies tp ORDER BY tp.originalIncomePerMonth";
				List<DonorTaxUnit> donorPool = em.createQuery(query).getResultList();
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
				MahalanobisDistance mdChildcare = new MahalanobisDistance(dataChildcare);
				MahalanobisDistance mdDualIncomeChildcare = new MahalanobisDistance(dataDualIncomeChildcare);

				// instantiate Parameters for retrieval
				Parameters.setTaxdbReferences(taxdbReferences);
				Parameters.setDonorPool(donorPool);
				Parameters.setMdDualIncome(mdDualIncome);
				Parameters.setMdChildcare(mdChildcare);
				Parameters.setMdDualIncomeChildcare(mdDualIncomeChildcare);

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
				for(Education education: Education.values()) {
					for(int ageGroup = 0; ageGroup <= 6; ageGroup++) { //Age groups with numerical values are created in Person class setAdditionalFieldsInInitialPopulation() method and must match Excel marriageTypes2.xlsx file.
						String tmpKey = gender + " " + region + " " + education + " " + ageGroup;
						personsToMatch2.get(tmpKey).clear();
					}
				}
			}
		}
	}
}
