package simpaths.model;

import java.util.*;
import java.util.random.RandomGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import simpaths.data.ManagerRegressions;
import simpaths.data.filters.ValidHomeownersCSfilter;
import simpaths.model.enums.*;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.Logger;

import simpaths.data.Parameters;
import simpaths.model.decisions.DecisionParams;
import simpaths.model.decisions.States;
import simpaths.experiment.SimPathsCollector;
import microsim.agent.Weight;
import microsim.data.db.PanelEntityKey;
import microsim.engine.SimulationEngine;
import microsim.event.EventListener;
import microsim.statistics.IDoubleSource;
import simpaths.model.enums.Les_c4;

import static java.lang.Math.max;
import static java.lang.StrictMath.min;

@Entity
public class BenefitUnit implements EventListener, IDoubleSource, Weight, Comparable<BenefitUnit> {

	@Transient
	private static Logger log = Logger.getLogger(BenefitUnit.class);

	@Transient
	private final SimPathsModel model;

	@Transient
	private final SimPathsCollector collector;

	//TODO: Needs to be set above the maximum BenefitUnitId number in the input population to prevent collisions (i.e. the creation of a household with the same ID as one already existing).
	//Note: Set to start at 1 now, as the benefitUnitId in the initial population starts at above 8 million
	//Note that although the input population may follow the convention that the person ID is a related to the household ID, this will be difficult to maintain as new benefitUnits will be created as the population leaves their existing benefitUnits to form new benefitUnits (either when they are 18 and leave home, or when they match with a new partner, for example).  This should not be a problem, as we can maintain the link between person and household by reference.
	@Transient
	public static long benefitUnitIdCounter = 1;        //2701500 is the current maximum in EU-SILC, more like 27,000 for EUROMOD.

	@Id
	private final PanelEntityKey key;

	@Column(name="idfemale")    //XXX: This column is not present in the household table of the input database
	private Long idFemale;

	@Transient
	private Person female;        //The female head of the household and the mother of the children

	@Column(name="idmale")        //XXX: This column is not present in the household table of the input database
	private Long idMale;

	@Transient
	private Person male;        //The male head of the household and the (possibly step) father of the children

	@Column(name="idhh")
	private Long idHousehold;

	@Transient
	private Household household;

	@Transient
	private States states;

	@Transient
	private double investmentIncomeAnnual;

	@Transient
	private double pensionIncomeAnnual;

	@Column(name="consumption_annual")
	private Double discretionaryConsumptionPerYear;

	@Column(name="household_weight")
	private double weight = 1.0d;

	@Column(name="liquid_wealth")
	private Double liquidWealth;

	@Enumerated(EnumType.STRING)
	Occupancy occupancy;

	@Column(name="disposable_household_income_monthly")
	private Double disposableIncomeMonthly;

	private Double grossIncomeMonthly;

	@Transient
	private Double benefitsReceivedPerMonth;

	@Transient
	private boolean disposableIncomeMonthlyImputedFlag;

	@Column(name="equivalised_household_disposable_income_yearly")
	private Double equivalisedDisposableIncomeYearly;

	@Transient
	private Double equivalisedDisposableIncomeYearly_lag1;

	@Transient
	private Double yearlyChangeInLogEDI;

	@Column(name="at_risk_of_poverty")
	private Integer atRiskOfPoverty;        //1 if at risk of poverty, defined by an equivalisedDisposableIncomeYearly < 60% of median household's

	@Transient
	private Integer atRiskOfPoverty_lag1 = atRiskOfPoverty; //TODO: calculate in the data and load through the initial population?

//	@Transient
//	private Map<Gender, Person> responsiblePersons;	//Even though we work with male, female most of the time (for historic reasons), this map should make it easier to inspect the objects in certain situations

	@Transient
	private Set<Person> children;

	private Integer size;

	@Transient    //Temporarily added as new input database does not contain this information
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_3under;                //Dummy variable for whether the person has children under 4 years old.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.

	@Transient    //Temporarily added as new input database does not contain this information
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_4_12;                //Dummy variable for whether the person has children between 4 and 12 years old.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.

	@Transient
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_3under_lag;                //Lag(1) of d_children_3under;

	@Transient
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_4_12_lag;                //Lag(1) of d_children_4_12;

	@Transient    //Temporarily added as new input database does not contain this information
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_2under;                //Dummy variable for whether the person has children under 3 years old.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.

	@Transient    //Temporarily added as new input database does not contain this information
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_3_6;                //Dummy variable for whether the person has children between 3 and 6 years old inclusive.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.

	@Transient    //Temporarily added as new input database does not contain this information
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_7_12;                //Dummy variable for whether the person has children between 7 and 12 years old inclusive.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.

	@Transient    //Temporarily added as new input database does not contain this information
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_13_17;                //Dummy variable for whether the person has children between 13 and 17 years old inclusive.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.

	@Transient    //Temporarily added as new input database does not contain this information
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_18over;                //Dummy variable for whether the person has children over 18 years old inclusive.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age0")
	private Integer n_children_0;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age1")
	private Integer n_children_1;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age2")
	private Integer n_children_2;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age3")
	private Integer n_children_3;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age4")
	private Integer n_children_4;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age5")
	private Integer n_children_5;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age6")
	private Integer n_children_6;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age7")
	private Integer n_children_7;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age8")
	private Integer n_children_8;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age9")
	private Integer n_children_9;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age10")
	private Integer n_children_10;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age11")
	private Integer n_children_11;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age12")
	private Integer n_children_12;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age13")
	private Integer n_children_13;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age14")
	private Integer n_children_14;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age15")
	private Integer n_children_15;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age16")
	private Integer n_children_16;

	//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age17")
	private Integer n_children_17;

	@Column(name="child_care_cost_per_week")
	private Double childcareCostPerWeek;

	@Column(name="social_care_cost_per_week")
	private Double socialCareCostPerWeek;

	@Transient
	private Integer n_children_allAges = 0; //Number of children of all ages in the household

	@Transient
	private Integer n_children_allAges_lag1 = 0; //Lag(1) of the number of children of all ages in the household

	@Transient
	private Integer n_children_02 = 0; //Number of children aged 0-2 in the household

	@Transient
	private Integer n_children_04 = 0; //Number of children aged 0-4 in the household
	@Transient
	private Integer n_children_59 = 0; //Number of children aged 5-9 in the household
	@Transient
	private Integer n_children_1017 = 0; //Number of children aged 10-17 in the household
	@Transient
	private Integer n_children_517 = 0; //Number of children aged 5-17 in the household

    @Transient
    private Integer n_children_017 = 0; //Number of children aged 0-17 in the household

	@Transient
	private Integer n_children_02_lag1 = 0; //Lag(1) of the number of children aged 0-2 in the household

	@Enumerated(EnumType.STRING)
	private Region region;        //Region of household.  Also used in findDonorHouseholdsByLabour method

	//New variables:
	//ydses_c5: household income quantiles
	@Enumerated(EnumType.STRING)
	@Column(name="ydses_c5")
	private Ydses_c5 ydses_c5;

	//ydses_c5_lag1: lag(1) of household income quantiles
	@Enumerated(EnumType.STRING)
	@Transient
	private Ydses_c5 ydses_c5_lag1 = null;

	//Used in calculation of ydses_c5
	@Transient
	private double tmpHHYpnbihs_dv_asinh = 0.;

	//dhhtp_c4: household composition
	@Enumerated(EnumType.STRING)
	@Column(name="dhhtp_c4")
	private Dhhtp_c4 dhhtp_c4;

	//dhhtp_c4_lag1: lag(1) of household composition
	@Enumerated(EnumType.STRING)
	@Transient
	private Dhhtp_c4 dhhtp_c4_lag1 = null;

	//Equivalised weight to use with variables that are equivalised by household composition
	@Transient
	private double equivalisedWeight = 1.;

	private String createdByConstructor;

	@Column(name="dhh_owned")
	private boolean dhh_owned; // Is any of the individuals in the benefit unit a homeowner? True / false

	@Transient
	ArrayList<Triple<Les_c7_covid, Double, Integer>> covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale = new ArrayList<>();

	@Transient
	ArrayList<Triple<Les_c7_covid, Double, Integer>> covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale = new ArrayList<>(); // This ArrayList stores monthly values of labour market states and gross incomes, to be sampled from by the LabourMarket class, for the female member of the benefit unit

	@Transient
	RandomGenerator childCareInnov;
	@Transient
	RandomGenerator labourInnov;
	@Transient
	RandomGenerator homeOwnerInnov;
	@Transient
	RandomGenerator taxInnov;

	@Transient
	private Integer yearLocal;
	@Transient
	private Education deh_c3Local;
	@Transient
	private Integer labourHoursWeekly1Local;
	@Transient
	private Integer labourHoursWeekly2Local;



	/*********************************************************************
	 * CONSTRUCTOR FOR OBJECT USED ONLY TO INTERACT WITH REGRESSION MODELS
	 ********************************************************************/
	public BenefitUnit() {
		super();
		model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
		collector = (SimPathsCollector) SimulationEngine.getInstance().getManager(SimPathsCollector.class.getCanonicalName());
		key  = new PanelEntityKey();        //Sets up key

		children = new LinkedHashSet<Person>();
		size = 0;
		createdByConstructor = "Empty";
	}

	public BenefitUnit(boolean regressionModel) {
		this.model = null;
		key = null;
		collector = null;
	}

	public BenefitUnit(BenefitUnit originalBenefitUnit, boolean regressionProxy) {

		if (regressionProxy) {
			this.model = null;
			key = null;
			collector = null;
			yearLocal = originalBenefitUnit.yearLocal;
			occupancy = originalBenefitUnit.occupancy;
			deh_c3Local = originalBenefitUnit.deh_c3Local;
			region = originalBenefitUnit.region;
			n_children_0 = originalBenefitUnit.n_children_0;
			n_children_1 = originalBenefitUnit.n_children_1;
			n_children_2 = originalBenefitUnit.n_children_2;
			n_children_3 = originalBenefitUnit.n_children_3;
			n_children_4 = originalBenefitUnit.n_children_4;
			n_children_5 = originalBenefitUnit.n_children_5;
			n_children_6 = originalBenefitUnit.n_children_6;
			n_children_7 = originalBenefitUnit.n_children_7;
			n_children_8 = originalBenefitUnit.n_children_8;
			n_children_9 = originalBenefitUnit.n_children_9;
			n_children_10 = originalBenefitUnit.n_children_10;
			n_children_11 = originalBenefitUnit.n_children_11;
			n_children_12 = originalBenefitUnit.n_children_12;
			n_children_13 = originalBenefitUnit.n_children_13;
			n_children_14 = originalBenefitUnit.n_children_14;
			n_children_15 = originalBenefitUnit.n_children_15;
			n_children_16 = originalBenefitUnit.n_children_16;
			n_children_17 = originalBenefitUnit.n_children_17;
		} else {
			throw new RuntimeException("error accessing copy constructor of benefitUnit for use with regression models");
		}
	}
	public BenefitUnit(Long id) {
		super();
		model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
		collector = (SimPathsCollector) SimulationEngine.getInstance().getManager(SimPathsCollector.class.getCanonicalName());
		key  = new PanelEntityKey(id);        //Sets up key

		children = new LinkedHashSet<Person>();
		size = 0;
		childCareInnov = new Random(SimulationEngine.getRnd().nextLong());
		labourInnov = new Random(SimulationEngine.getRnd().nextLong());
		homeOwnerInnov = new Random(SimulationEngine.getRnd().nextLong());
		taxInnov = new Random(SimulationEngine.getRnd().nextLong());

		this.d_children_3under = Indicator.False;
		this.d_children_4_12 = Indicator.False;
		this.d_children_3under_lag = Indicator.False;
		this.d_children_4_12_lag = Indicator.False;
		this.d_children_2under = Indicator.False;
		this.d_children_3_6 = Indicator.False;
		this.d_children_7_12 = Indicator.False;
		this.d_children_13_17 = Indicator.False;
		this.d_children_18over = Indicator.False;
		this.n_children_0 = 0;
		this.n_children_1 = 0;
		this.n_children_2 = 0;
		this.n_children_3 = 0;
		this.n_children_4 = 0;
		this.n_children_5 = 0;
		this.n_children_6 = 0;
		this.n_children_7 = 0;
		this.n_children_8 = 0;
		this.n_children_9 = 0;
		this.n_children_10 = 0;
		this.n_children_11 = 0;
		this.n_children_12 = 0;
		this.n_children_13 = 0;
		this.n_children_14 = 0;
		this.n_children_15 = 0;
		this.n_children_16 = 0;
		this.n_children_17 = 0;
		this.n_children_allAges = 0;
		this.n_children_allAges_lag1 = 0;
		this.n_children_02 = 0;
		this.n_children_04 = 0;
		this.n_children_59 = 0;
		this.n_children_1017 = 0;
		this.n_children_517 = 0;
		this.n_children_02_lag1 = 0;
		this.childcareCostPerWeek = 0.0;
		this.socialCareCostPerWeek = 0.0;
		this.disposableIncomeMonthly = 0.;
		this.grossIncomeMonthly = 0.;
		this.equivalisedDisposableIncomeYearly = 0.;
		this.benefitsReceivedPerMonth = 0.;
		this.createdByConstructor = "LongID";
		this.disposableIncomeMonthlyImputedFlag = false;
		if (Parameters.enableIntertemporalOptimisations) {
			this.liquidWealth = 0.;
		}
	}

	public BenefitUnit(Person person, Set<Person> childrenToNewBenefitUnit, Household newHousehold) {

		// initialise benefit unit
		this(benefitUnitIdCounter++);
		region = person.getRegion();

		if (Parameters.enableIntertemporalOptimisations) {
			// transfer wealth between benefit units

			BenefitUnit fromBenefitUnit = person.getBenefitUnit();
			liquidWealth = person.getLiquidWealth();
			if (this != fromBenefitUnit) {
				fromBenefitUnit.setLiquidWealth(fromBenefitUnit.getLiquidWealth() - person.getLiquidWealth());
			}
		}

		// add new members
		for (Person child : childrenToNewBenefitUnit) {
			addChild(child);
		}
		addResponsiblePerson(person);

		setHousehold(newHousehold);

		// finalise
		this.createdByConstructor = "Singles";
	}

	public BenefitUnit(Person p1, Person p2, Set<Person> childrenToNewBenefitUnit, Household newHousehold) {

		// initialise benefit unit
		this(benefitUnitIdCounter++);
		region = p1.getRegion();

		if (region != p2.getRegion()) {
			throw new RuntimeException("ERROR - region of responsible male and female must match!");
		}

		if (Parameters.enableIntertemporalOptimisations) {
			// transfer wealth between benefit units

			liquidWealth = p1.getLiquidWealth() + p2.getLiquidWealth();
			BenefitUnit pBU = p1.getBenefitUnit();
			if (this!=pBU) {
				pBU.setLiquidWealth(pBU.getLiquidWealth() - p1.getLiquidWealth());
			}
			pBU = p2.getBenefitUnit();
			if (this!=pBU) {
				pBU.setLiquidWealth(pBU.getLiquidWealth() - p2.getLiquidWealth());
			}
		}

		// add new members
		for (Person child : childrenToNewBenefitUnit) {
			addChild(child);
		}
		addResponsibleCouple(p1, p2);

		setHousehold(newHousehold);

		// finalise
		createdByConstructor = "Couples";
	}

	// Below is a "copy constructor" for benefitUnits: it takes an original benefit unit as input, changes the ID, copies
	// the rest of the benefit unit's properties, and creates a new benefit unit.
	public BenefitUnit(BenefitUnit originalBenefitUnit) {

		this(benefitUnitIdCounter++);

		this.log = originalBenefitUnit.log;
		this.weight = originalBenefitUnit.weight;
		this.occupancy = originalBenefitUnit.occupancy;
		if (originalBenefitUnit.getDisposableIncomeMonthly() != null) {
			this.disposableIncomeMonthly = originalBenefitUnit.getDisposableIncomeMonthly();
		} else {
			this.disposableIncomeMonthly = 0.;
		}
		if (originalBenefitUnit.getGrossIncomeMonthly() != null) {
			this.grossIncomeMonthly = originalBenefitUnit.getGrossIncomeMonthly();
		} else {
			this.grossIncomeMonthly = 0.;
		}
		if (originalBenefitUnit.equivalisedDisposableIncomeYearly != null) {
			this.equivalisedDisposableIncomeYearly = originalBenefitUnit.equivalisedDisposableIncomeYearly;
		} else {
			this.equivalisedDisposableIncomeYearly = 0.;
		}
		if (originalBenefitUnit.getBenefitsReceivedPerMonth() != null) {
			this.benefitsReceivedPerMonth = originalBenefitUnit.getBenefitsReceivedPerMonth();
		} else {
			this.benefitsReceivedPerMonth = 0.;
		}
		if (originalBenefitUnit.equivalisedDisposableIncomeYearly_lag1 != null) {
			this.equivalisedDisposableIncomeYearly_lag1 = originalBenefitUnit.equivalisedDisposableIncomeYearly_lag1;
		} else {
			this.equivalisedDisposableIncomeYearly_lag1 = this.equivalisedDisposableIncomeYearly;
		}
		this.atRiskOfPoverty = originalBenefitUnit.atRiskOfPoverty;
		if (originalBenefitUnit.atRiskOfPoverty_lag1 != null) {
			this.atRiskOfPoverty_lag1 = originalBenefitUnit.atRiskOfPoverty_lag1;
		} else {
			this.atRiskOfPoverty_lag1 = originalBenefitUnit.atRiskOfPoverty;
		}
		if (Parameters.enableIntertemporalOptimisations) {
			this.liquidWealth = originalBenefitUnit.liquidWealth;
		}
		this.children = new LinkedHashSet<Person>();
		this.size = originalBenefitUnit.size;
		this.d_children_3under = originalBenefitUnit.d_children_3under;
		this.d_children_4_12 = originalBenefitUnit.d_children_4_12;
		this.d_children_3under_lag = originalBenefitUnit.d_children_3under_lag;
		this.d_children_4_12_lag = originalBenefitUnit.d_children_4_12_lag;
		this.d_children_2under = originalBenefitUnit.d_children_2under;
		this.d_children_3_6 = originalBenefitUnit.d_children_3_6;
		this.d_children_7_12 = originalBenefitUnit.d_children_7_12;
		this.d_children_13_17 = originalBenefitUnit.d_children_13_17;
		this.d_children_18over = originalBenefitUnit.d_children_18over;
		this.n_children_0 = originalBenefitUnit.n_children_0;
		this.n_children_1 = originalBenefitUnit.n_children_1;
		this.n_children_2 = originalBenefitUnit.n_children_2;
		this.n_children_3 = originalBenefitUnit.n_children_3;
		this.n_children_4 = originalBenefitUnit.n_children_4;
		this.n_children_5 = originalBenefitUnit.n_children_5;
		this.n_children_6 = originalBenefitUnit.n_children_6;
		this.n_children_7 = originalBenefitUnit.n_children_7;
		this.n_children_8 = originalBenefitUnit.n_children_8;
		this.n_children_9 = originalBenefitUnit.n_children_9;
		this.n_children_10 = originalBenefitUnit.n_children_10;
		this.n_children_11 = originalBenefitUnit.n_children_11;
		this.n_children_12 = originalBenefitUnit.n_children_12;
		this.n_children_13 = originalBenefitUnit.n_children_13;
		this.n_children_14 = originalBenefitUnit.n_children_14;
		this.n_children_15 = originalBenefitUnit.n_children_15;
		this.n_children_16 = originalBenefitUnit.n_children_16;
		this.n_children_17 = originalBenefitUnit.n_children_17;
		this.n_children_allAges = originalBenefitUnit.n_children_allAges;
		this.n_children_allAges_lag1 = originalBenefitUnit.n_children_allAges_lag1;
		this.n_children_02 = originalBenefitUnit.n_children_02;
		this.n_children_04 = originalBenefitUnit.n_children_04;
		this.n_children_59 = originalBenefitUnit.n_children_59;
		this.n_children_1017 = originalBenefitUnit.n_children_1017;
		this.n_children_517 = originalBenefitUnit.n_children_517;
		this.n_children_017 = originalBenefitUnit.n_children_017;
		this.n_children_02_lag1 = originalBenefitUnit.n_children_02_lag1;
		this.childcareCostPerWeek = originalBenefitUnit.childcareCostPerWeek;
		this.socialCareCostPerWeek = originalBenefitUnit.socialCareCostPerWeek;
		this.region = originalBenefitUnit.region;
		this.ydses_c5 = originalBenefitUnit.getYdses_c5();
		this.ydses_c5_lag1 = originalBenefitUnit.ydses_c5_lag1;
		this.dhhtp_c4 = originalBenefitUnit.dhhtp_c4;
		this.dhhtp_c4_lag1 = originalBenefitUnit.dhhtp_c4_lag1;
		this.equivalisedWeight = originalBenefitUnit.getEquivalisedWeight();
		this.dhh_owned = originalBenefitUnit.dhh_owned;
		this.disposableIncomeMonthlyImputedFlag = false;
		if (originalBenefitUnit.createdByConstructor != null) {
			this.createdByConstructor = originalBenefitUnit.createdByConstructor;
		} else {
			this.createdByConstructor = "CopyConstructor";
		}
	}


	// ---------------------------------------------------------------------
	// Event Listener
	// ---------------------------------------------------------------------


	public enum Processes {
		Update,        //This updates the household fields, such as number of children of a certain age
		CalculateChangeInEDI, //Calculate change in equivalised disposable income
		Homeownership,
		ReceivesBenefits,
		UpdateStates,
		UpdateInvestmentIncome,
		ProjectNetLiquidWealth,
		UpdateOccupancy,
	}

	@Override
	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
			case Update:
				updateChildrenFields();
				updateOccupancy();
				updateComposition(); //Update household composition
				updateFinancialVariables();
				break;
			case CalculateChangeInEDI:
				calculateEquivalisedDisposableIncomeYearly(); //Update BU's EDI
				calculateYearlyChangeInLogEquivalisedDisposableIncome(); //Calculate change in EDI
				break;
			case Homeownership:
				homeownership();
				break;
			case ReceivesBenefits:
				setReceivesBenefitsFlag();
				break;
			case UpdateStates:
				setStates();
				break;
			case ProjectNetLiquidWealth:
				updateNetLiquidWealth();
				break;
			case UpdateOccupancy:
				updateOccupancy();
				break;
		}
	}
	private void checkState() {
		double ww = getLiquidWealth();
		if (Occupancy.Couple.equals(occupancy)) {

			if (male == null) {
				throw new IllegalStateException("ERROR - benefit unit identified as couple, but missing male adult");
			} else if (female == null) {
				throw new IllegalStateException("ERROR - benefit unit identified as couple, but missing female adult");
			}
		}
	}

	protected void initializeFields() {

		updateOccupancy();
		updateChildrenFields();
		updateComposition();

		// clean-up odd ends
		if (getYdses_c5() == null) {
			ydses_c5 = Ydses_c5.Q3;
		}
		if (region == null) {
			if (female != null) {
				region = female.getRegion();
			}
			if (male != null) {
				if (region != null) {
					if (region != male.getRegion()) {
						throw new RuntimeException("inconsistent regions between spouses at initialisation");
					}
				} else {
					region = male.getRegion();
				}
			}
		}
	}

	protected void updateChildrenFields() {

		if(children == null){
			children = new LinkedHashSet<Person>();
		}

		//Define lagged values of children variables
		d_children_3under_lag = d_children_3under;
		d_children_4_12_lag = d_children_4_12;
		n_children_allAges_lag1 = n_children_allAges;
		n_children_02_lag1 = n_children_02;

		//Reset child age variables to update
		n_children_0 = 0;
		n_children_1 = 0;
		n_children_2 = 0;
		n_children_3 = 0;
		n_children_4 = 0;
		n_children_5 = 0;
		n_children_6 = 0;
		n_children_7 = 0;
		n_children_8 = 0;
		n_children_9 = 0;
		n_children_10 = 0;
		n_children_11 = 0;
		n_children_12 = 0;
		n_children_13 = 0;
		n_children_14 = 0;
		n_children_15 = 0;
		n_children_16 = 0;
		n_children_17 = 0;
		n_children_allAges = 0;
		n_children_02 = 0;
        n_children_017 = 0;
		n_children_04 = 0;
		n_children_59 = 0;
		n_children_1017 = 0;
		n_children_517 = 0;

		d_children_18over = Indicator.False;
		for(Person child: children) {

			n_children_allAges++;
			if(child.getDag() >= 18) {
				d_children_18over = Indicator.True;    //For Labour Supply Regressions, but should always be false because children leave home when they reach 18 in our model.
			}

			switch(child.getDag()) {
				case(0) :
					n_children_0++;
					break;
				case(1) :
					n_children_1++;
					break;
				case(2) :
					n_children_2++;
					break;
				case(3) :
					n_children_3++;
					break;
				case(4) :
					n_children_4++;
					break;
				case(5) :
					n_children_5++;
					break;
				case(6) :
					n_children_6++;
					break;
				case(7) :
					n_children_7++;
					break;
				case(8) :
					n_children_8++;
					break;
				case(9) :
					n_children_9++;
					break;
				case(10) :
					n_children_10++;
					break;
				case(11) :
					n_children_11++;
					break;
				case(12) :
					n_children_12++;
					break;
				case(13) :
					n_children_13++;
					break;
				case(14) :
					n_children_14++;
					break;
				case(15) :
					n_children_15++;
					break;
				case(16) :
					n_children_16++;
					break;
				case(17) :
					n_children_17++;
					break;
			}
		}
		n_children_02 = n_children_0 + n_children_1 + n_children_2; //Number of children aged 0-2 is the sum of children in each age category
		n_children_04 = n_children_0 + n_children_1 + n_children_2 + n_children_3 + n_children_4;
		n_children_59 = n_children_5 + n_children_6 + n_children_7 + n_children_8 + n_children_9;
		n_children_1017 = n_children_10 + n_children_11 + n_children_12 + n_children_13 + n_children_14 + n_children_15 + n_children_16 + n_children_17;
		n_children_517 = n_children_59 + n_children_1017;
		n_children_017 = n_children_04 + n_children_517;
		if (n_children_allAges_lag1 == null) {
			n_children_allAges_lag1 = n_children_allAges - n_children_0;
		}
		if (n_children_02_lag1 == null) {
			n_children_02_lag1 = n_children_1 + n_children_2 + n_children_3;
		}

		//New fields for Labour Supply Utility Regression calculation
		if(n_children_0 > 0 || n_children_1 > 0 || n_children_2 > 0) {
			d_children_2under = Indicator.True;
		} else {
			d_children_2under = Indicator.False; // This will be updated if a birth occurs.
		}

		if (n_children_3 > 0 || n_children_4 > 0 || n_children_5 > 0 || n_children_6 > 0) {
			d_children_3_6 = Indicator.True;
		} else {
			d_children_3_6 = Indicator.False;
		}

		if (n_children_7 > 0 || n_children_8 > 0 || n_children_9 > 0 || n_children_10 > 0 || n_children_11 > 0 || n_children_12 > 0) {
			d_children_7_12 = Indicator.True;
		} else {
			d_children_7_12 = Indicator.False;
		}

		if (n_children_13 > 0 || n_children_14 > 0 || n_children_15 > 0 || n_children_16 > 0 || n_children_17 > 0) {
			d_children_13_17 = Indicator.True;
		} else {
			d_children_13_17 = Indicator.False;
		}

		//For fields from previous Labour Force Participation Model
		d_children_3under = d_children_2under;
		if(n_children_3 > 0) {
			d_children_3under = Indicator.True;
		}
		if (d_children_3under_lag == null) {
			if (n_children_1 + n_children_2 + n_children_3 + n_children_4 > 0) {
				d_children_3under_lag = Indicator.True;
			} else {
				d_children_3under_lag = Indicator.False;
			}
		}

		d_children_4_12 = d_children_7_12;
		if ( n_children_4 > 0 || n_children_5 > 0 || n_children_6 > 0){
			d_children_4_12 = Indicator.True;
		}
		if (d_children_4_12_lag == null) {
			if (n_children_5 + n_children_6 + n_children_7 + n_children_8 + n_children_9 + n_children_10 +
					n_children_11 + n_children_12 + n_children_13 > 0) {
				d_children_4_12_lag = Indicator.True;
			} else {
				d_children_4_12_lag = Indicator.False;
			}
		}
	}


	// ---------------------------------------------------------------------
	// Labour Market Interaction
	// ---------------------------------------------------------------------

	protected void updateFinancialVariables() {
		ydses_c5_lag1 = getYdses_c5(); //Store current value as lag(1) before updating
		atRiskOfPoverty_lag1 = atRiskOfPoverty;
		equivalisedDisposableIncomeYearly_lag1 = equivalisedDisposableIncomeYearly;
		disposableIncomeMonthlyImputedFlag = false;
		//Define process determining ydses_c5 for the household - this is currently done in the updateHouseholdsIncome() in LabourMarket because calculation of quantiles requires data on all benefitUnits
	}

	protected void updateComposition() {
		// update dhhtp_c4

		if (dhhtp_c4 != null) {
			dhhtp_c4_lag1 = dhhtp_c4; //Store current value as lag(1) before updating
		}

		//Use household occupancy and number of children to set dhhtp_c4
		if (occupancy != null) {
			if(Occupancy.Couple.equals(occupancy)) {
				if(n_children_allAges > 0) {
					dhhtp_c4 = Dhhtp_c4.CoupleChildren; //If household is occupied by a couple and number of children is positive, set dhhtp_c4 to "Couple with Children"
				} else {
					dhhtp_c4 = Dhhtp_c4.CoupleNoChildren; //Otherwise, set dhhtp_c4 to "Couple without children"
				}
			} else {                                            //Otherwise, household occupied by a single person
				if(n_children_allAges > 0) {
					dhhtp_c4 = Dhhtp_c4.SingleChildren; //If number of children positive, set dhhtp_c4 to "Single with Children"
				} else {
					dhhtp_c4 = Dhhtp_c4.SingleNoChildren; //Otherwise, set dhhtp_c4 to "Single without children"
				}
			}
		} else if (male == null && female == null) {
			throw new IllegalArgumentException("No responsible adult in benefit unit at composition update.");
		}
		if (dhhtp_c4_lag1 == null) {
			dhhtp_c4_lag1 = dhhtp_c4;
		}
	}

	protected void updateFullTimeHourlyEarnings() {

		if(male != null) {
			male.setLabourSupplyWeekly(null);
			male.updateFullTimeHourlyEarnings();
		}
		if(female != null) {
			female.setLabourSupplyWeekly(null);
			female.updateFullTimeHourlyEarnings();
		}
	}

	/**
	 * This method returns all possible combinations of male's and female's hours of work
	 * @return
	 */
	public LinkedHashSet<MultiKey<Labour>> findPossibleLabourCombinations() {
		LinkedHashSet<MultiKey<Labour>> combinationsToReturn = new LinkedHashSet<>();
		if(Occupancy.Couple.equals(occupancy)) {        //Need to use both partners individual characteristics to determine similar benefitUnits

			//Sometimes one of the occupants of the couple will be retired (or even under the age to work, which is currently the age to leave home).  For this case, the person (not at risk of work)'s labour supply will always be zero, while the other person at risk of work has a choice over the single person Labour Supply set.
			Labour[] labourMaleValues;
			if(male.atRiskOfWork()) {
				labourMaleValues = Labour.values();
			} else {
				labourMaleValues = new Labour[]{Labour.ZERO};
			}

			Labour[] labourFemaleValues;
			if(female.atRiskOfWork()) {
				labourFemaleValues = Labour.values();
			} else {
				labourFemaleValues = new Labour[]{Labour.ZERO};
			}

			for(Labour labourMale: labourMaleValues) {
				for(Labour labourFemale: labourFemaleValues) {
					combinationsToReturn.add(new MultiKey<>(labourMale, labourFemale));
				}
			}
		} else {        //For single benefitUnits, no need to check for at risk of work (i.e. retired, sick or student activity status), as this has already been done when passing this household to the labour supply module (see first loop over benefitUnits in LabourMarket#update()).
			if (Occupancy.Single_Male.equals(occupancy)) {
				for (Labour labour : Labour.values()) {
					combinationsToReturn.add(new MultiKey<>(labour, Labour.ZERO));
				}
			} else { //Must be single female
				for (Labour labour : Labour.values()) {
					combinationsToReturn.add(new MultiKey<>(Labour.ZERO, labour));
				}
			}
		}

		return combinationsToReturn;
	}


	public void setReceivesBenefitsFlag() {

		boolean receivesBenefitsFlag = (getBenefitsReceivedPerMonth() > 0);

		switch (occupancy) {
			case Couple:
				male.setReceivesBenefitsFlag(receivesBenefitsFlag);
				female.setReceivesBenefitsFlag(receivesBenefitsFlag);
				break;
			case Single_Male:
				male.setReceivesBenefitsFlag(receivesBenefitsFlag);
				break;
			case Single_Female:
				female.setReceivesBenefitsFlag(receivesBenefitsFlag);
				break;
			default:
				throw new IllegalStateException("Benefit Unit with the following ID has no recognised occupancy: " + getKey().getId());
		}
	}

	/*
    updateDisposableIncomeIfNotAtRiskOfWork process is used to calculate disposable income for benefit units in which no individual is at risk of work, and which therefore do not enter the updateLabourSupply process.
    There are two cases to consider: i) single benefit units, ii) couples where no individual is at risk of work
     */
	protected void updateDisposableIncomeIfNotAtRiskOfWork() {

		if (!getAtRiskOfWork()) {
			// Get donor benefitUnits from EUROMOD - the most similar benefitUnits for our criteria, matched by:
			// BenefitUnit characteristics: occupancy, region, number of children;
			// Individual characteristics (potentially for each partner): gender, education, number of hours worked (binned in classes), work sector, health, age;
			// and with minimum difference between gross (market) income.

			double hoursWorkedPerWeekM = 0.0;
			double hoursWorkedPerWeekF = 0.0;
			int dlltsdM = -1, dlltsdF = -1;
			double originalIncomePerMonth, secondIncomePerMonth = 0.0;
			if (Occupancy.Couple.equals(occupancy)) {

				if (male != null && female != null) {

					male.setLabourSupplyWeekly(Labour.convertHoursToLabour(hoursWorkedPerWeekM));
					female.setLabourSupplyWeekly(Labour.convertHoursToLabour(hoursWorkedPerWeekF));
					double maleIncome = Math.sinh(male.getYptciihs_dv());
					double femaleIncome = Math.sinh(female.getYptciihs_dv());
					if (maleIncome>0.01 && femaleIncome>0.01)
						secondIncomePerMonth = Math.min(maleIncome, femaleIncome);
					originalIncomePerMonth = maleIncome + femaleIncome;
					dlltsdM = male.getDisability();
					dlltsdF = female.getDisability();
				} else {
					throw new RuntimeException("inconsistent occupancy status for benefit unit");
					//updateOccupancy();
				}
			} else if (Occupancy.Single_Male.equals(occupancy)) {

				male.setLabourSupplyWeekly(Labour.convertHoursToLabour(hoursWorkedPerWeekM));
				originalIncomePerMonth = Math.sinh(male.getYptciihs_dv());
				dlltsdM = male.getDisability();
			} else if (Occupancy.Single_Female.equals(occupancy)){

				female.setLabourSupplyWeekly(Labour.convertHoursToLabour(hoursWorkedPerWeekF));
				originalIncomePerMonth = Math.sinh(female.getYptciihs_dv());
				dlltsdF = female.getDisability();
			} else {

				throw new RuntimeException("Benefit Unit with the following ID has no recognised occupancy: " + getKey().getId());
			}

			double childcareCostPerMonth = 0.0;
			if (Parameters.flagFormalChildcare) {
				updateChildcareCostPerWeek();
				childcareCostPerMonth = childcareCostPerWeek * Parameters.WEEKS_PER_MONTH;
			}
			double socialCareCostPerMonth = 0.0;
			if (Parameters.flagSocialCare) {
				updateSocialCareCostPerWeek();
				socialCareCostPerMonth = socialCareCostPerWeek * Parameters.WEEKS_PER_MONTH;
			}

			// update disposable income
			TaxEvaluation evaluatedTransfers;
			if (Parameters.donorPoolAveraging) {
				evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), hoursWorkedPerWeekM, hoursWorkedPerWeekF, dlltsdM, dlltsdF, originalIncomePerMonth, secondIncomePerMonth, childcareCostPerMonth, socialCareCostPerMonth, liquidWealth, -1.0);
			} else {
				evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), hoursWorkedPerWeekM, hoursWorkedPerWeekF, dlltsdM, dlltsdF, originalIncomePerMonth, secondIncomePerMonth, childcareCostPerMonth, socialCareCostPerMonth, liquidWealth, taxInnov.nextDouble());
			}
			disposableIncomeMonthly = evaluatedTransfers.getDisposableIncomePerMonth();
			benefitsReceivedPerMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
			grossIncomeMonthly = evaluatedTransfers.getGrossIncomePerMonth();
			calculateBUIncome();
		} else {
			throw new RuntimeException("call to evaluate disposable income on assumption of zero risk of employment where there is risk");
		}
	}

	protected void updateMonthlyLabourSupplyCovid19() {

		// This method calls on predictCovidTransition() method to set labour market state, gross income, and work hours for each month.
		// After 12 months, this should result in an array with 12 values, from which the LabourMarket class can sample one that is representative of the labour market state for the whole year.

		if (Occupancy.Couple.equals(occupancy)) {
			if (male != null && male.atRiskOfWork()) {
				Triple<Les_c7_covid, Double, Integer> stateGrossIncomeWorkHoursTriple = predictCovidTransition(male); // predictCovidTransition() applies transition models to predict transition and returns new labour market state, gross income, and work hours
				covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.add(stateGrossIncomeWorkHoursTriple); // Add the state-gross income-work hours triple to the ArrayList keeping track of all monthly values
			}
			if (female != null && female.atRiskOfWork()) {
				Triple<Les_c7_covid, Double, Integer> stateGrossIncomeWorkHoursTriple = predictCovidTransition(female);
				covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.add(stateGrossIncomeWorkHoursTriple);
			}
		} else if (Occupancy.Single_Male.equals(occupancy) || Occupancy.Single_Female.equals(occupancy)) {
			//Consider only one person, of either gender, for the transition regressions
			Person person;
			if (Occupancy.Single_Male.equals(occupancy) && male.atRiskOfWork()) {
				person = male;
				Triple<Les_c7_covid, Double, Integer> stateGrossIncomeWorkHoursTriple = predictCovidTransition(person);
				covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.add(stateGrossIncomeWorkHoursTriple);
			} else if (Occupancy.Single_Female.equals(occupancy) && female.atRiskOfWork()) {
				person = female;
				Triple<Les_c7_covid, Double, Integer> stateGrossIncomeWorkHoursTriple = predictCovidTransition(person);
				covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.add(stateGrossIncomeWorkHoursTriple);
			}
		} else {System.out.println("Warning: Incorrect occupancy for benefit unit " + getKey().getId());}
	}

	/*
    chooseRandomMonthlyTransitionAndGrossIncome() method selected a random value out of all the monthly transitions (which contains state to which individual transitions, gross income, and work hours), finds donor benefit unit and calculates disposable income

     */
	protected void chooseRandomMonthlyOutcomeCovid19() {

		if (Occupancy.Couple.equals(occupancy)) {
			if(male.atRiskOfWork()) {
				if(female.atRiskOfWork()) {
					// both male and female have flexible labour supply
					if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size() > 0 && covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size() > 0) {
						int randomIndex = model.getEngine().getRandom().nextInt(min(covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size(), covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size())); // Get random int which indicates which monthly value to use. Use smaller value in case male and female lists were of different length.
						Triple<Les_c7_covid, Double, Integer> selectedValueMale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.get(randomIndex);
						Triple<Les_c7_covid, Double, Integer> selectedValueFemale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.get(randomIndex);

						male.setLes_c7_covid(selectedValueMale.getLeft()); // Set labour force status for male
						male.setLes_c4(Les_c4.convertLes_c7_To_Les_c4(selectedValueMale.getLeft()));
						female.setLes_c7_covid(selectedValueFemale.getLeft()); // Set labour force status for female
						female.setLes_c4(Les_c4.convertLes_c7_To_Les_c4(selectedValueFemale.getLeft()));

						// Predicted hours need to be converted back into labour so a donor benefit unit can be found. Then, gross income can be converted to disposable.
						male.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueMale.getRight())); // Convert predicted work hours to labour enum and update male's value
						female.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueFemale.getRight())); // Convert predicted work hours to labour enum and update female's value
						double simulatedIncomeToConvertPerMonth = selectedValueMale.getMiddle() + selectedValueFemale.getMiddle(); // Benefit unit's gross income to convert is the sum of incomes (labour and capital included) of male and female

						// Find best donor and convert gross income to disposable
						MultiKey<? extends Labour> labourKey = new MultiKey<>(male.getLabourSupplyWeekly(), female.getLabourSupplyWeekly());
						TaxEvaluation evaluatedTransfers;
						if (Parameters.donorPoolAveraging) {
							evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), female.getDisability(), simulatedIncomeToConvertPerMonth, 0.0, 0.0, -1.0);
						} else {
							evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), female.getDisability(), simulatedIncomeToConvertPerMonth, 0.0, 0.0, taxInnov.nextDouble());
						}
						disposableIncomeMonthly = evaluatedTransfers.getDisposableIncomePerMonth();
						benefitsReceivedPerMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
						grossIncomeMonthly = evaluatedTransfers.getGrossIncomePerMonth();
					} else {
						throw new RuntimeException("Inconsistent atRiskOfWork indicator and covid19MonthlyStateAndGrossIncomeAndWorkHoursTriples");
					}
				} else {
					// male has flexible labour supply, female doesn't
					if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size() > 0) {
						int randomIndex = model.getEngine().getRandom().nextInt(covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size()); // Get random int which indicates which monthly value to use. Use smaller value in case male and female lists were of different length.
						Triple<Les_c7_covid, Double, Integer> selectedValueMale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.get(randomIndex);

						male.setLes_c7_covid(selectedValueMale.getLeft()); // Set labour force status for male
						male.setLes_c4(Les_c4.convertLes_c7_To_Les_c4(selectedValueMale.getLeft()));

						// Predicted hours need to be converted back into labour so a donor benefit unit can be found. Then, gross income can be converted to disposable.
						male.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueMale.getRight())); // Convert predicted work hours to labour enum and update male's value
						double simulatedIncomeToConvertPerMonth = selectedValueMale.getMiddle(); // Benefit unit's gross income to convert is the sum of incomes (labour and capital included) of male and female

						// Find best donor and convert gross income to disposable
						MultiKey<? extends Labour> labourKey = new MultiKey<>(male.getLabourSupplyWeekly(), Labour.ZERO);
						TaxEvaluation evaluatedTransfers;
						if (Parameters.donorPoolAveraging) {
							evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), female.getDisability(), simulatedIncomeToConvertPerMonth, 0.0, 0.0, -1.0);
						} else {
							evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), female.getDisability(), simulatedIncomeToConvertPerMonth, 0.0, 0.0, taxInnov.nextDouble());
						}
						disposableIncomeMonthly = evaluatedTransfers.getDisposableIncomePerMonth();
						benefitsReceivedPerMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
						grossIncomeMonthly = evaluatedTransfers.getGrossIncomePerMonth();
					} else {
						throw new RuntimeException("Inconsistent atRiskOfWork indicator and covid19MonthlyStateAndGrossIncomeAndWorkHoursTriples (2)");
					}
				}
			} else if (female.atRiskOfWork()) {
				// female has flexible labour supply, male doesn't
				if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size() > 0) {
					int randomIndex = model.getEngine().getRandom().nextInt(covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size()); // Get random int which indicates which monthly value to use. Use smaller value in case male and female lists were of different length.
					Triple<Les_c7_covid, Double, Integer> selectedValueFemale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.get(randomIndex);

					female.setLes_c7_covid(selectedValueFemale.getLeft()); // Set labour force status for female
					female.setLes_c4(Les_c4.convertLes_c7_To_Les_c4(selectedValueFemale.getLeft()));

					// Predicted hours need to be converted back into labour so a donor benefit unit can be found. Then, gross income can be converted to disposable.
					female.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueFemale.getRight())); // Convert predicted work hours to labour enum and update female's value
					double simulatedIncomeToConvertPerMonth = selectedValueFemale.getMiddle(); // Benefit unit's gross income to convert is the sum of incomes (labour and capital included) of male and female, but in this case male is not at risk of work

					// Find best donor and convert gross income to disposable
					MultiKey<? extends Labour> labourKey = new MultiKey<>(Labour.ZERO, female.getLabourSupplyWeekly());

					TaxEvaluation evaluatedTransfers;
					if (Parameters.donorPoolAveraging) {
						evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), female.getDisability(), simulatedIncomeToConvertPerMonth, 0.0, 0.0, -1.0);
					} else {
						evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), female.getDisability(), simulatedIncomeToConvertPerMonth, 0.0, 0.0, taxInnov.nextDouble());
					}
					disposableIncomeMonthly = evaluatedTransfers.getDisposableIncomePerMonth();
					benefitsReceivedPerMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
					grossIncomeMonthly = evaluatedTransfers.getGrossIncomePerMonth();
				} else {
					throw new RuntimeException("Inconsistent atRiskOfWork indicator and covid19MonthlyStateAndGrossIncomeAndWorkHoursTriples (3)");
				}
			} else {
				throw new IllegalArgumentException("In consistent indicators for at risk of work in couple benefit unit " + getKey().getId());
			}
		} else if (Occupancy.Single_Male.equals(occupancy)) {
			if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size() > 0) {
				int randomIndex = model.getEngine().getRandom().nextInt(covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size()); // Get random int which indicates which monthly value to use
				Triple<Les_c7_covid, Double, Integer> selectedValueMale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.get(randomIndex);
				male.setLes_c7_covid(selectedValueMale.getLeft()); // Set labour force status for male
				male.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueMale.getRight()));
				double simulatedIncomeToConvertPerMonth = selectedValueMale.getMiddle();

				// Find best donor and convert gross income to disposable
				MultiKey<? extends Labour> labourKey = new MultiKey<>(male.getLabourSupplyWeekly(), Labour.ZERO);

				TaxEvaluation evaluatedTransfers;
				if (Parameters.donorPoolAveraging) {
					evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), -1, simulatedIncomeToConvertPerMonth, 0.0, 0.0, -1.0);
				} else {
					evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), -1, simulatedIncomeToConvertPerMonth, 0.0, 0.0, taxInnov.nextDouble());
				}
				disposableIncomeMonthly = evaluatedTransfers.getDisposableIncomePerMonth();
				benefitsReceivedPerMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
				grossIncomeMonthly = evaluatedTransfers.getGrossIncomePerMonth();
			} else {
				throw new IllegalArgumentException("In consistent indicators for at risk of work in single male benefit unit " + getKey().getId());
			}
		} else {
			if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size() > 0) {
				int randomIndex = model.getEngine().getRandom().nextInt(covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size());
				Triple<Les_c7_covid, Double, Integer> selectedValueFemale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.get(randomIndex);
				female.setLes_c7_covid(selectedValueFemale.getLeft()); // Set labour force status for female
				female.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueFemale.getRight()));
				double simulatedIncomeToConvertPerMonth = selectedValueFemale.getMiddle();

				// Find best donor and convert gross income to disposable
				MultiKey<? extends Labour> labourKey = new MultiKey<>(Labour.ZERO, female.getLabourSupplyWeekly());

				TaxEvaluation evaluatedTransfers;
				if (Parameters.donorPoolAveraging) {
					evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), -1, female.getDisability(), simulatedIncomeToConvertPerMonth, 0.0, 0.0, -1.0);
				} else {
					evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), -1, female.getDisability(), simulatedIncomeToConvertPerMonth, 0.0, 0.0, taxInnov.nextDouble());
				}
				disposableIncomeMonthly = evaluatedTransfers.getDisposableIncomePerMonth();
				benefitsReceivedPerMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
				grossIncomeMonthly = evaluatedTransfers.getGrossIncomePerMonth();
			} else {
				throw new IllegalArgumentException("In consistent indicators for at risk of work in single female benefit unit " + getKey().getId());
			}
		}

		calculateBUIncome();
	}

    /*
	predictCovidTransition() operates at individual level, while typically (in the non-covid labour supply module) income is calculated at the benefit unit level.
	It returns gross income at individual level, which is then totaled in the updateLabourSupplyCovid19() method above and gross income of the benefit unit and converted to disposable at the benefit unit level in the updateLabourSupplyCovid19() method above.
	*/

	private Triple<Les_c7_covid, Double, Integer> predictCovidTransition(Person person) {

		if (person.getLes_c7_covid_lag1() == null) {
			person.initialise_les_c6_from_c4();
			person.setCovidModuleGrossLabourIncome_lag1(person.getCovidModuleGrossLabourIncome_Baseline());
		}


		// Define variables:
		Les_c7_covid stateFrom = person.getLes_c7_covid_lag1();
		Les_c7_covid stateTo = stateFrom; // Labour market state to which individual transitions. Initialise to stateFrom value if the outcome is "no changes"
		int newWorkHours; // Predicted work hours. Initialise to previous value if available, or hours from labour enum.
		if (person.getNewWorkHours_lag1() != null) {
			newWorkHours = person.getNewWorkHours_lag1();
		} else {
			newWorkHours = person.getLabourSupplyHoursWeekly(); // Note: prediction for hours is in logs, needs to be transformed to levels
			person.setNewWorkHours_lag1(newWorkHours);
		}
		double grossMonthlyIncomeToReturn = 0; // Gross income to return to updateLabourSupplyCovid19() method

		// Transitions from employment
		if (Les_c7_covid.Employee.equals(stateFrom)) {
			Map<Les_transitions_E1,Double> probs = Parameters.getRegC19LS_E1().getProbabilites(person, Person.DoublesVariables.class, Les_transitions_E1.class);
			Les_transitions_E1 transitionTo = ManagerRegressions.multiEvent(probs, labourInnov.nextDouble());
			stateTo = transitionTo.convertToLes_c7_covid();
			person.setLes_c7_covid(stateTo); // Use convert to les c6 covid method from the enum to convert the outcome to the les c6 scale and update the variable

			if (Les_transitions_E1.SelfEmployed.equals(transitionTo) || Les_transitions_E1.SomeChanges.equals(transitionTo)) {

				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_E2a().getScore(person, Person.DoublesVariables.class)))).getHours(person);
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_E1.NotEmployed)) {
				newWorkHours = 0;
				grossMonthlyIncomeToReturn = 0;
			} else if (transitionTo.equals(Les_transitions_E1.FurloughedFull)) {
				// If furloughed, don't change hours of work initialised at the beginning
				grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_E1.FurloughedFlex)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_E2b().getScore(person, Person.DoublesVariables.class)))).getHours(person);
				grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else { // Else "no changes" = employee. Use initialisation value for stateTo and newWorkHours and fill gross monthly income
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getYptciihs_dv());
			}

			// Transitions from furlough full
		} else if (stateFrom.equals(Les_c7_covid.FurloughedFull)) {
			Map<Les_transitions_FF1,Double> probs = Parameters.getRegC19LS_FF1().getProbabilites(person, Person.DoublesVariables.class, Les_transitions_FF1.class);
			Les_transitions_FF1 transitionTo = ManagerRegressions.multiEvent(probs, labourInnov.nextDouble());
			stateTo = transitionTo.convertToLes_c7_covid();
			person.setLes_c7_covid(stateTo); // Use convert to les c7 covid method from the enum to convert the outcome to the les c7 scale and update the variable

			if (transitionTo.equals(Les_transitions_FF1.Employee)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2b().getScore(person, Person.DoublesVariables.class)))).getHours(person);
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_FF1.SelfEmployed)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2a().getScore(person, Person.DoublesVariables.class)))).getHours(person);
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_FF1.FurloughedFlex)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2c().getScore(person, Person.DoublesVariables.class)))).getHours(person);
				grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_FF1.NotEmployed)) {
				newWorkHours = 0;
				grossMonthlyIncomeToReturn = 0;
			} else { // Else remains furloughed. Use 80% of initialisation value for stateTo and newWorkHours and fill gross monthly income
				grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getYptciihs_dv());
			}

			// Transitions from furlough flex
		} else if (stateFrom.equals(Les_c7_covid.FurloughedFlex)) {
			Map<Les_transitions_FX1,Double> probs = Parameters.getRegC19LS_FX1().getProbabilites(person, Person.DoublesVariables.class, Les_transitions_FX1.class);
			Les_transitions_FX1 transitionTo = ManagerRegressions.multiEvent(probs, labourInnov.nextDouble());
			stateTo = transitionTo.convertToLes_c7_covid();
			person.setLes_c7_covid(stateTo); // Use convert to les c7 covid method from the enum to convert the outcome to the les c7 scale and update the variable

			if (transitionTo.equals(Les_transitions_FX1.Employee)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2b().getScore(person, Person.DoublesVariables.class)))).getHours(person);
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_FX1.SelfEmployed)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2a().getScore(person, Person.DoublesVariables.class)))).getHours(person);
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_FX1.FurloughedFull)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2a().getScore(person, Person.DoublesVariables.class)))).getHours(person);
				grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getYptciihs_dv()); // 80% of earnings they would have had working normal hours, hence hours predicted as for employed in the line above
			} else if (transitionTo.equals(Les_transitions_FX1.NotEmployed)) {
				newWorkHours = 0;
				grossMonthlyIncomeToReturn = 0;
			} else { // Else remains furloughed. Use 80% of initialisation value for stateTo and newWorkHours and fill gross monthly income
				grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getYptciihs_dv());
			}

			// Transitions from self-employment
		} else if (stateFrom.equals(Les_c7_covid.SelfEmployed)) {
			Map<Les_transitions_S1,Double> probs = Parameters.getRegC19LS_S1().getProbabilites(person, Person.DoublesVariables.class, Les_transitions_S1.class);
			Les_transitions_S1 transitionTo = ManagerRegressions.multiEvent(probs, labourInnov.nextDouble());
			stateTo = transitionTo.convertToLes_c7_covid();
			person.setLes_c7_covid(stateTo); // Use convert to les c6 covid method from the enum to convert the outcome to the les c6 scale and update the variable

			if (transitionTo.equals(Les_transitions_S1.Employee) || transitionTo.equals(Les_transitions_S1.SelfEmployed)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_S2a().getScore(person, Person.DoublesVariables.class)))).getHours(person);
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getYptciihs_dv());

				// If transition to is self-employed (i.e. continues in self-employment), and earnings have decreased (gross monthly income lower than lag1 of gross monthly income, obtained from person.getCovidModuleGrossLabourIncome_lag1), predict probabiltiy of SEISS
				if (transitionTo.equals(Les_transitions_S1.SelfEmployed) && grossMonthlyIncomeToReturn < person.getCovidModuleGrossLabourIncome_lag1()) {
					// SEISS probability and effect on income
					boolean receivesSEISS = (labourInnov.nextDouble() < Parameters.getRegC19LS_S3().getProbability(person, Person.DoublesVariables.class));
					if (receivesSEISS) {
						person.setCovidModuleReceivesSEISS(Indicator.True);
						grossMonthlyIncomeToReturn = 0.8 * person.getCovidModuleGrossLabourIncome_lag1();
					}
				}

			} else if (transitionTo.equals(Les_transitions_S1.NotEmployed)) {
				newWorkHours = 0;
				grossMonthlyIncomeToReturn = 0;
			} // No else here as new work hours and gross income are predicted if remains self-employed (above)

			// Transitions from non-employment
		} else if (stateFrom.equals(Les_c7_covid.NotEmployed)) {
			Map<Les_transitions_U1,Double> probs = Parameters.getRegC19LS_U1().getProbabilites(person, Person.DoublesVariables.class, Les_transitions_U1.class);
			Les_transitions_U1 transitionTo = ManagerRegressions.multiEvent(probs, labourInnov.nextDouble());
			stateTo = transitionTo.convertToLes_c7_covid();
			person.setLes_c7_covid(stateTo); // Use convert to les c6 covid method from the enum to convert the outcome to the les c6 scale and update the variable

			if (transitionTo.equals(Les_transitions_U1.Employee) || transitionTo.equals(Les_transitions_U1.SelfEmployed)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_U2a().getScore(person, Person.DoublesVariables.class)))).getHours(person);
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_U1.NotEmployed)) {
				newWorkHours = 0;
				grossMonthlyIncomeToReturn = 0;
			}

		} else {
			//	System.out.println("Warning: Person " + person.getKey().getId() + " entered Covid-19 transitions process, but doesn't have correct starting labour market state which was " + stateFrom);
		}

		Triple<Les_c7_covid, Double, Integer> stateGrossIncomeWorkHoursTriple = Triple.of(stateTo, grossMonthlyIncomeToReturn, newWorkHours); // Triple contains outcome labour market state after transition, gross income, and work hours
		person.setCovidModuleGrossLabourIncome_lag1(grossMonthlyIncomeToReturn); // used as a regressor in the Covid-19 regressions
		person.setNewWorkHours_lag1(newWorkHours); // newWorkHours is not a state variable of a person and is only used from month to month in the covid module, so set lag here
		person.setLes_c7_covid_lag1(stateTo); // Update lagged value of monthly labour market state
		return stateGrossIncomeWorkHoursTriple;
	}

	private double exponentiateAndConstrainWorkHoursPrediction(double workHours) {
		double workHoursConverted = Math.exp(workHours);
		if (workHoursConverted < 0) {
			return 0.;
		} else return workHoursConverted;
	}

	protected void updateLabourSupplyAndIncome() {

		//Update potential earnings
		updateFullTimeHourlyEarnings();
		if (Parameters.enableIntertemporalOptimisations && (DecisionParams.FLAG_IO_EMPLOYMENT1 || DecisionParams.FLAG_IO_EMPLOYMENT2) ) {
			// intertemporal optimisations enabled

			// labour and labour income
			double emp1, emp2, labourIncomeWeeklyM, labourIncomeWeeklyF, hoursWorkedPerWeekM, hoursWorkedPerWeekF;
			labourIncomeWeeklyM = labourIncomeWeeklyF = hoursWorkedPerWeekM = hoursWorkedPerWeekF = 0.0;
			int dlltsdM = -1, dlltsdF = -1;
			if (DecisionParams.FLAG_IO_EMPLOYMENT1 && states.getAgeYears() <= Parameters.MAX_AGE_FLEXIBLE_LABOUR_SUPPLY) {
				emp1 = Parameters.grids.employment1.interpolateAll(states, false);
			} else {
				emp1 = 0.0;
			}
			if (Occupancy.Couple.equals(occupancy)) {
				// couples

				if (DecisionParams.FLAG_IO_EMPLOYMENT2 && states.getAgeYears() <= Parameters.MAX_AGE_FLEXIBLE_LABOUR_SUPPLY) {
					emp2 = Parameters.grids.employment2.interpolateAll(this.states, false);
				} else {
					emp2 = 0.0;
				}
				if ( getRefPersonForDecisions().getGender()==0) {
					// reference person is male

					hoursWorkedPerWeekM = DecisionParams.FULLTIME_HOURS_WEEKLY * emp1;
					hoursWorkedPerWeekF = DecisionParams.FULLTIME_HOURS_WEEKLY * emp2;
				} else {
					// reference person is female

					hoursWorkedPerWeekM = DecisionParams.FULLTIME_HOURS_WEEKLY * emp2;
					hoursWorkedPerWeekF = DecisionParams.FULLTIME_HOURS_WEEKLY * emp1;
				}
			} else {
				// single adults

				if ( male != null ) {
					hoursWorkedPerWeekM = DecisionParams.FULLTIME_HOURS_WEEKLY * emp1;
				} else {
					hoursWorkedPerWeekF = DecisionParams.FULLTIME_HOURS_WEEKLY * emp1;
				}
			}
			if (male != null) {

				male.setLabourSupplyWeekly(Labour.convertHoursToLabour(hoursWorkedPerWeekM));
				hoursWorkedPerWeekM = male.getLabourSupplyHoursWeekly();
				labourIncomeWeeklyM = male.getEarningsWeekly(hoursWorkedPerWeekM);
				dlltsdM = male.getDisability();
			}
			if (female != null) {

				female.setLabourSupplyWeekly(Labour.convertHoursToLabour(hoursWorkedPerWeekF));
				hoursWorkedPerWeekF = female.getLabourSupplyHoursWeekly();
				labourIncomeWeeklyF = female.getEarningsWeekly(hoursWorkedPerWeekF);
				dlltsdF = female.getDisability();
			}

			updateNonLabourIncome();

			double childcareCostPerMonth = 0.0;
			if (Parameters.flagFormalChildcare) {
				updateChildcareCostPerWeek();
				childcareCostPerMonth = childcareCostPerWeek * Parameters.WEEKS_PER_MONTH;
			}
			double socialCareCostPerMonth = 0.0;
			if (Parameters.flagSocialCare) {
				updateSocialCareCostPerWeek();
				socialCareCostPerMonth = socialCareCostPerWeek * Parameters.WEEKS_PER_MONTH;
			}

			// evaluate disposable income
			double originalIncomePerMonth = Parameters.WEEKS_PER_MONTH * (labourIncomeWeeklyM + labourIncomeWeeklyF) +
					investmentIncomeAnnual/12.0 + pensionIncomeAnnual/12.0;
			double secondIncomePerMonth = Math.min(labourIncomeWeeklyM, labourIncomeWeeklyF) * Parameters.WEEKS_PER_MONTH;

			TaxEvaluation evaluatedTransfers;
			if (Parameters.donorPoolAveraging) {
				evaluatedTransfers = new TaxEvaluation(model.getYear(), states.getAgeYears(), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), hoursWorkedPerWeekM, hoursWorkedPerWeekF, dlltsdM, dlltsdF, originalIncomePerMonth, secondIncomePerMonth, childcareCostPerMonth, socialCareCostPerMonth, liquidWealth, -1.0);
			} else {
				evaluatedTransfers = new TaxEvaluation(model.getYear(), states.getAgeYears(), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), hoursWorkedPerWeekM, hoursWorkedPerWeekF, dlltsdM, dlltsdF, originalIncomePerMonth, secondIncomePerMonth, childcareCostPerMonth, socialCareCostPerMonth, liquidWealth, taxInnov.nextDouble());
			}
			disposableIncomeMonthly = evaluatedTransfers.getDisposableIncomePerMonth();
			benefitsReceivedPerMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
			grossIncomeMonthly = evaluatedTransfers.getGrossIncomePerMonth();
		} else {
			// intertemporal optimisations disabled

			updateNonLabourIncome();

			// prepare temporary storage variables
			MultiKey<? extends Labour> labourSupplyChoice = null;
			MultiKeyMap<Labour, Double> disposableIncomeMonthlyByLabourPairs = MultiKeyMap.multiKeyMap(new LinkedMap<>());
			MultiKeyMap<Labour, Double> benefitsReceivedMonthlyByLabourPairs = MultiKeyMap.multiKeyMap(new LinkedMap<>());
			MultiKeyMap<Labour, Double> grossIncomeMonthlyByLabourPairs = MultiKeyMap.multiKeyMap(new LinkedMap<>());
			LinkedHashSet<MultiKey<Labour>> possibleLabourCombinations = findPossibleLabourCombinations(); // Find possible labour combinations for this benefit unit
			MultiKeyMap<Labour, Double> labourSupplyUtilityExponentialRegressionScoresByLabourPairs = MultiKeyMap.multiKeyMap(new LinkedMap<>());

			//Sometimes one of the occupants of the couple will be retired (or even under the age to work, which is currently the age to leave home).  For this case, the person (not at risk of work)'s labour supply will always be zero, while the other person at risk of work has a choice over the single person Labour Supply set.
			if(Occupancy.Couple.equals(occupancy)) {

				for(MultiKey<? extends Labour> labourKey : possibleLabourCombinations) { //PB: for each possible discrete number of hours

					//Sets values for regression score calculation
					male.setLabourSupplyWeekly(labourKey.getKey(0));
					female.setLabourSupplyWeekly(labourKey.getKey(1));

					double childcareCostPerMonth = 0.0;
					if (Parameters.flagFormalChildcare) {
						updateChildcareCostPerWeek();
						childcareCostPerMonth = childcareCostPerWeek * Parameters.WEEKS_PER_MONTH;
					}
					double socialCareCostPerMonth = 0.0;
					if (Parameters.flagSocialCare) {
						updateSocialCareCostPerWeek();
						socialCareCostPerMonth = socialCareCostPerWeek * Parameters.WEEKS_PER_MONTH;
					}

					//Earnings are composed of the labour income and non-benefit non-employment income Yptciihs_dv() (this is monthly, so no need to multiply by WEEKS_PER_MONTH_RATIO)
					double maleIncome = Parameters.WEEKS_PER_MONTH * male.getEarningsWeekly() + Math.sinh(male.getYptciihs_dv());
					double femaleIncome = Parameters.WEEKS_PER_MONTH * female.getEarningsWeekly() + Math.sinh(female.getYptciihs_dv());
					double simulatedIncomeToConvertPerMonth = maleIncome + femaleIncome;
					double secondIncomePerMonth = Math.min(maleIncome, femaleIncome);

					TaxEvaluation evaluatedTransfers;
					if (Parameters.donorPoolAveraging) {
						evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), female.getDisability(), simulatedIncomeToConvertPerMonth, secondIncomePerMonth, childcareCostPerMonth, socialCareCostPerMonth, liquidWealth, -1.0);
					} else {
						evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), female.getDisability(), simulatedIncomeToConvertPerMonth, secondIncomePerMonth, childcareCostPerMonth, socialCareCostPerMonth, liquidWealth, taxInnov.nextDouble());
					}
					disposableIncomeMonthly = evaluatedTransfers.getDisposableIncomePerMonth();
					benefitsReceivedPerMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
					grossIncomeMonthly = evaluatedTransfers.getGrossIncomePerMonth();

					//Note that only benefitUnits at risk of work are considered, so at least one partner is at risk of work
					double exponentialRegressionScore = 0.;
					if(male.atRiskOfWork()) { //If male has flexible labour supply
						if(female.atRiskOfWork()) { //And female has flexible labour supply
							//Follow utility process for couples
							exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityCouples().getScore(this, BenefitUnit.Regressors.class));
						} else if (!female.atRiskOfWork()) { //Male has flexible labour supply, female doesn't
							//Follow utility process for single males for the UK
							exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityMalesWithDependent().getScore(this, BenefitUnit.Regressors.class));
							//In Italy, this should follow a separate set of estimates. One way is to differentiate between countries here; another would be to add a set of estimates for both countries, but for the UK have the same number as for singles
							//Introduced a new category of estimates, Males/Females with Dependent to be used when only one of the couple is flexible in labour supply. In Italy, these have a separate set of estimates; in the UK they use the same estimates as "independent" singles
						}
					} else if(female.atRiskOfWork() && !male.atRiskOfWork()) { //Male not at risk of work - female must be at risk of work since only benefitUnits at risk are considered here
						//Follow utility process for single female
						exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityFemalesWithDependent().getScore(this, BenefitUnit.Regressors.class));
					} else throw new IllegalArgumentException("None of the partners are at risk of work! HHID " + getKey().getId());
					if (Double.isNaN(exponentialRegressionScore) || Double.isInfinite(exponentialRegressionScore)) {
						throw new RuntimeException("problem evaluating exponential regression score in labour supply module (1)");
					}

					disposableIncomeMonthlyByLabourPairs.put(labourKey, getDisposableIncomeMonthly());
					benefitsReceivedMonthlyByLabourPairs.put(labourKey, getBenefitsReceivedPerMonth());
					grossIncomeMonthlyByLabourPairs.put(labourKey, getGrossIncomeMonthly());
					labourSupplyUtilityExponentialRegressionScoresByLabourPairs.put(labourKey, exponentialRegressionScore); //XXX: Adult children could contribute their income to the hh, but then utility would have to be joint for a household with adult children, and they couldn't be treated separately as they are at the moment?
				}
			} else {
				// single adult

				if(Occupancy.Single_Male.equals(occupancy)) {

					for(MultiKey<? extends Labour> labourKey : possibleLabourCombinations) {

						male.setLabourSupplyWeekly(labourKey.getKey(0));

						double childcareCostPerMonth = 0.0;
						if (Parameters.flagFormalChildcare) {
							updateChildcareCostPerWeek();
							childcareCostPerMonth = childcareCostPerWeek * Parameters.WEEKS_PER_MONTH;
						}
						double socialCareCostPerMonth = 0.0;
						if (Parameters.flagSocialCare) {
							updateSocialCareCostPerWeek();
							socialCareCostPerMonth = socialCareCostPerWeek * Parameters.WEEKS_PER_MONTH;
						}

						double simulatedIncomeToConvertPerMonth = Parameters.WEEKS_PER_MONTH * male.getEarningsWeekly() + Math.sinh(male.getYptciihs_dv());

						TaxEvaluation evaluatedTransfers;
						if (Parameters.donorPoolAveraging) {
							evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), 0., male.getDisability(), -1, simulatedIncomeToConvertPerMonth, 0.0, childcareCostPerMonth, socialCareCostPerMonth, liquidWealth, -1.0);
						} else {
							evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), 0., male.getDisability(), -1, simulatedIncomeToConvertPerMonth, 0.0, childcareCostPerMonth, socialCareCostPerMonth, liquidWealth, taxInnov.nextDouble());
						}
						disposableIncomeMonthly = evaluatedTransfers.getDisposableIncomePerMonth();
						benefitsReceivedPerMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
						grossIncomeMonthly = evaluatedTransfers.getGrossIncomePerMonth();

						double exponentialRegressionScore;
						if (male.getAdultChildFlag() == 1) { //If adult children use labour supply estimates for male adult children
							exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityACMales().getScore(this, Regressors.class));
						} else {
							exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityMales().getScore(this, Regressors.class));
						}
						if (Double.isNaN(exponentialRegressionScore) || Double.isInfinite(exponentialRegressionScore)) {
							throw new RuntimeException("problem evaluating exponential regression score in labour supply module (2)");
						}

						disposableIncomeMonthlyByLabourPairs.put(labourKey, getDisposableIncomeMonthly());
						benefitsReceivedMonthlyByLabourPairs.put(labourKey, getBenefitsReceivedPerMonth());
						grossIncomeMonthlyByLabourPairs.put(labourKey, getGrossIncomeMonthly());
						labourSupplyUtilityExponentialRegressionScoresByLabourPairs.put(labourKey, exponentialRegressionScore);
					}
				} else if (Occupancy.Single_Female.equals(occupancy)) {        //Occupant must be a single female

					for(MultiKey<? extends Labour> labourKey : possibleLabourCombinations) {

						female.setLabourSupplyWeekly(labourKey.getKey(1));

						double childcareCostPerMonth = 0.0;
						if (Parameters.flagFormalChildcare) {
							updateChildcareCostPerWeek();
							childcareCostPerMonth = childcareCostPerWeek * Parameters.WEEKS_PER_MONTH;
						}
						double socialCareCostPerMonth = 0.0;
						if (Parameters.flagSocialCare) {
							updateSocialCareCostPerWeek();
							socialCareCostPerMonth = socialCareCostPerWeek * Parameters.WEEKS_PER_MONTH;
						}

						double simulatedIncomeToConvertPerMonth = Parameters.WEEKS_PER_MONTH * female.getEarningsWeekly() + Math.sinh(female.getYptciihs_dv());

						TaxEvaluation evaluatedTransfers;
						if (Parameters.donorPoolAveraging) {
							evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), 0., labourKey.getKey(1).getHours(female), -1, female.getDisability(), simulatedIncomeToConvertPerMonth, 0.0, childcareCostPerMonth, socialCareCostPerMonth, liquidWealth, -1.0);
						} else {
							evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), 0., labourKey.getKey(1).getHours(female), -1, female.getDisability(), simulatedIncomeToConvertPerMonth, 0.0, childcareCostPerMonth, socialCareCostPerMonth, liquidWealth, taxInnov.nextDouble());
						}
						disposableIncomeMonthly = evaluatedTransfers.getDisposableIncomePerMonth();
						benefitsReceivedPerMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
						grossIncomeMonthly = evaluatedTransfers.getGrossIncomePerMonth();

						double exponentialRegressionScore;
						if (female.getAdultChildFlag() == 1) { //If adult children use labour supply estimates for female adult children
							exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityACFemales().getScore(this, BenefitUnit.Regressors.class));
						} else {
							exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityFemales().getScore(this, BenefitUnit.Regressors.class));
						}
						if (Double.isNaN(exponentialRegressionScore) || Double.isInfinite(exponentialRegressionScore)) {
							throw new RuntimeException("problem evaluating exponential regression score in labour supply module (3)");
						}
						disposableIncomeMonthlyByLabourPairs.put(labourKey, getDisposableIncomeMonthly());
						benefitsReceivedMonthlyByLabourPairs.put(labourKey, getBenefitsReceivedPerMonth());
						grossIncomeMonthlyByLabourPairs.put(labourKey, getGrossIncomeMonthly());
						labourSupplyUtilityExponentialRegressionScoresByLabourPairs.put(labourKey, exponentialRegressionScore);
					}
				}
			}
			if(labourSupplyUtilityExponentialRegressionScoresByLabourPairs.isEmpty()) {
				// error check

				System.out.print("\nlabourSupplyUtilityExponentialRegressionScoresByLabourPairs for household " + key.getId() + " with occupants ");
				if(male != null) {
					System.out.print("male : " + male.getKey().getId() + ", ");
				}
				if(female != null) {
					System.out.print("female : " + female.getKey().getId() + ", ");
				}
				System.out.print("is empty!");
			}

			//Sample labour supply from possible labour (pairs of) values
			labourSupplyChoice = ManagerRegressions.multiEvent(labourSupplyUtilityExponentialRegressionScoresByLabourPairs, labourInnov.nextDouble());

			// populate labour supply
			if(model.debugCommentsOn && labourSupplyChoice!=null) {
				log.debug("labour supply choice " + labourSupplyChoice);
			}
			if(Occupancy.Couple.equals(occupancy)) {
				male.setLabourSupplyWeekly(labourSupplyChoice.getKey(0));
				female.setLabourSupplyWeekly(labourSupplyChoice.getKey(1));
			} else {
				if(Occupancy.Single_Male.equals(occupancy)) {
					male.setLabourSupplyWeekly(labourSupplyChoice.getKey(0));
				} else {        //Occupant must be single female
					female.setLabourSupplyWeekly(labourSupplyChoice.getKey(1));
				}
			}

			// allow for formal childcare costs
			if (Parameters.flagFormalChildcare) {
				updateChildcareCostPerWeek();
			}
			if (Parameters.flagSocialCare) {
				updateSocialCareCostPerWeek();
			}

			// populate disposable income
			disposableIncomeMonthly = disposableIncomeMonthlyByLabourPairs.get(labourSupplyChoice);
			benefitsReceivedPerMonth = benefitsReceivedMonthlyByLabourPairs.get(labourSupplyChoice);
			grossIncomeMonthly = grossIncomeMonthlyByLabourPairs.get(labourSupplyChoice);
		}

		//Update gross income variables for the household and all occupants:
		calculateBUIncome();
	}


	/////////////////////////////////////////////////////////////////////////////////
	//
	//	Other Methods
	//
	////////////////////////////////////////////////////////////////////////////////


	protected void calculateBUIncome() {

		/*
		 * This method updates income variables for responsible persons in the household and household
		 *
		 * BenefitUnit income quintiles are based on labour, pension, miscellaneous (see comment for definition),
		 * Trade Union / Friendly Society Payment and maintenace or alimony.
		 *
		 * Gross household income combination of spouses income if partnered or personal income if single. Adjusted for
		 * household composition using OECD-modified scale and inflation.
		 * Normalised using inverse hyperbolic sine.
		 *
		 * labour income = yplgrs_dv at person level
		 * non-employment non-benefit income = yptciihs_dv (from income process in the simulation)
		 * non-benefit income = yptciihs_dv + yplgrs_dv
		 * ydses should be based on non-benefit income, ypnbihs_dv, which is emp income + non-emp non-ben income
		 *
		 * 1. Get yptciihs_dv
		 * 2. Get yplgrs_dv
		 * 3. Update ypnbihs_dv
		 * 4. Update ydses_c5
		 */

		//Calculate equivalised weight to use with HH-income variable
		calculateEquivalisedWeight();
		if(getOccupancy().equals(Occupancy.Couple)) {

			if(male != null && female != null) {

				// male
				double labourEarningsMale = male.getEarningsWeekly(male.getLabourSupplyHoursWeekly()) * Parameters.WEEKS_PER_MONTH; //Level of monthly labour earnings
				male.setYplgrs_dv(asinh(labourEarningsMale)); //This follows asinh transform of labourEarnings
				double YpnbihsMale = labourEarningsMale + Math.sinh(male.getYptciihs_dv()); //In levels
				male.setYpnbihs_dv(asinh(YpnbihsMale)); //Set asinh transformed
				male.setCovidModuleGrossLabourIncome_Baseline(YpnbihsMale); // Used in the Covid-19 labour supply module

				// female
				double labourEarningsFemale = female.getEarningsWeekly(female.getLabourSupplyHoursWeekly()) * Parameters.WEEKS_PER_MONTH; //Level of monthly labour earnings
				female.setYplgrs_dv(asinh(labourEarningsFemale)); //This follows asinh transform of labourEarnings
				double YpnbihsFemale = labourEarningsFemale + Math.sinh(female.getYptciihs_dv()); //In levels
				female.setYpnbihs_dv(asinh(YpnbihsFemale)); //Set asinh transformed
				female.setCovidModuleGrossLabourIncome_Baseline(YpnbihsFemale); // Used in the Covid-19 labour supply module

				// benefit unit income is the sum of male and female non-benefit income
				double tmpHHYpnbihs_dv = (YpnbihsMale + YpnbihsFemale) / getEquivalisedWeight(); //Equivalised
				setTmpHHYpnbihs_dv_asinh(asinh(tmpHHYpnbihs_dv)); //Asinh transformation of HH non-benefit income

				//Based on the percentiles calculated by the collector, assign household to one of the quintiles of (equivalised) income distribution
				if(collector.getStats() != null) { //Collector only gets initialised when simulation starts running
					if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p20()) {
						ydses_c5 = Ydses_c5.Q1;
					} else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p40()) {
						ydses_c5 = Ydses_c5.Q2;
					} else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p60()) {
						ydses_c5 = Ydses_c5.Q3;
					} else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p80()) {
						ydses_c5 = Ydses_c5.Q4;
					} else {
						ydses_c5 = Ydses_c5.Q5;
					}
				}
			}
		} else if(getOccupancy().equals(Occupancy.Single_Male)) {

			if(male != null) {

				double labourEarningsMale = male.getEarningsWeekly(male.getLabourSupplyHoursWeekly()) * Parameters.WEEKS_PER_MONTH; //Level of monthly labour earnings
				male.setYplgrs_dv(asinh(labourEarningsMale)); //This follows asinh transform of labourEarnings
				double YpnbihsMale = labourEarningsMale + Math.sinh(male.getYptciihs_dv()); //In levels
				male.setYpnbihs_dv(asinh(YpnbihsMale)); //Set asinh transformed
				male.setCovidModuleGrossLabourIncome_Baseline(YpnbihsMale); // Used in the Covid-19 labour supply module

				//BenefitUnit income is the male non-benefit income
				double tmpHHYpnbihs_dv = YpnbihsMale / getEquivalisedWeight(); //Equivalised
				setTmpHHYpnbihs_dv_asinh(asinh(tmpHHYpnbihs_dv)); //Asinh transformation of HH non-benefit income

				if(collector.getStats() != null) { //Collector only gets initialised when simulation starts running
					if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p20()) {
						ydses_c5 = Ydses_c5.Q1;
					} else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p40()) {
						ydses_c5 = Ydses_c5.Q2;
					} else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p60()) {
						ydses_c5 = Ydses_c5.Q3;
					} else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p80()) {
						ydses_c5 = Ydses_c5.Q4;
					} else {
						ydses_c5 = Ydses_c5.Q5;
					}
				}
			}
		} else {

			if(female != null) {

				//If not a couple nor a single male, occupancy must be single female
				double labourEarningsFemale = female.getEarningsWeekly(female.getLabourSupplyHoursWeekly()) * Parameters.WEEKS_PER_MONTH; //Level of monthly labour earnings
				female.setYplgrs_dv(asinh(labourEarningsFemale)); //This follows asinh transform of labourEarnings
				double YpnbihsFemale = labourEarningsFemale + Math.sinh(female.getYptciihs_dv()); //In levels
				female.setYpnbihs_dv(asinh(YpnbihsFemale)); //Set asinh transformed
				female.setCovidModuleGrossLabourIncome_Baseline(YpnbihsFemale); // Used in the Covid-19 labour supply module

				//BenefitUnit income is the female non-benefit income
				double tmpHHYpnbihs_dv = YpnbihsFemale / getEquivalisedWeight(); //Equivalised
				setTmpHHYpnbihs_dv_asinh(asinh(tmpHHYpnbihs_dv)); //Asinh transformation of HH non-benefit income

				if(collector.getStats() != null) { //Collector only gets initialised when simulation starts running

					if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p20()) {
						ydses_c5 = Ydses_c5.Q1;
					} else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p40()) {
						ydses_c5 = Ydses_c5.Q2;
					} else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p60()) {
						ydses_c5 = Ydses_c5.Q3;
					} else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p80()) {
						ydses_c5 = Ydses_c5.Q4;
					} else {
						ydses_c5 = Ydses_c5.Q5;
					}
				}
			}
		}
	}

	// benefit unit weight is average of all benefit unit members
	public void updateSizeAndWeight() {

		weight = 0.0d;
		size = 0;
		if(female != null) {
			weight += female.getWeight();
			size += 1;
		}
		if(male != null) {
			weight += male.getWeight();
			size += 1;
		}
		if (!children.isEmpty()) {
			for (Person child : children) {
				weight += child.getWeight();
				size += 1;
			}
		}
		weight = weight / (double) size;
		if (household != null) household.updateSizeAndWeight();
	}

	public void addResponsiblePerson(Person person) {

		if (person.getDag() < Parameters.AGE_TO_BECOME_RESPONSIBLE) {
			throw new RuntimeException("Attempt to add responsible person under age of maturity.");
		}
		if (person == null) {
			throw new RuntimeException("Attempt to allocate null responsible person to benefit unit.");
		}

		if (person.getDgn().equals(Gender.Female)) {
			setFemale(person);
		} else {
			setMale(person);
		}
		updateSizeAndWeight();
	}

	public void addResponsibleCouple(Person person, Person partner) {

		if (!person.getRegion().equals(partner.getRegion())) {
			throw new RuntimeException("Error - couple belong to two different regions!");
		}

		if (person.getDgn().equals(Gender.Female)) {

			setFemale(person);
			setMale(partner);
		} else {

			setMale(person);
			setFemale(partner);
		}
		updateSizeAndWeight();
	}

	public void addChild(Person person) {

		if (person == null) {
			throw new IllegalArgumentException("Attempt to add null child.");
		}
		if (person.getDag() >= Parameters.AGE_TO_BECOME_RESPONSIBLE) {
			throw new IllegalArgumentException("Attempt to add child to benefit unit who is over age of maturity.");
		}

		// link person with benefit unit
		children.add(person);

		checkIfUpdatePersonReferences(person);
		if (male != null) person.setIdFather(male);
		if (female != null) person.setIdMother(female);

		updateSizeAndWeight();
	}

	public void removePerson(Person person) {

		boolean removed = false;
		if (female == person) {
			setFemale(null);
			removed = true;
		} else if (male == person) {
			setMale(null);
			removed = true;
		} else if (children.contains(person)) {
			removed = children.remove(person);
		}
		if (!removed) {
			throw new IllegalArgumentException("Person " + person.getKey().getId() + " could not be removed from benefit unit");
		}
		person.setBenefitUnit(null);
		if (male == null && female == null) {
			model.removeBenefitUnit(this);
		} else {
			updateSizeAndWeight();
		}
	}


	// -------------------------------------------------------------------------------------------------------------
	// implements IDoubleSource for use with Regression classes - for use in DonorHousehold, not BenefitUnit objects
	// -------------------------------------------------------------------------------------------------------------

	public enum Regressors {

		IncomeSquared,
		HoursMaleSquared,
		HoursFemaleSquared,
		HoursMaleByIncome,
		HoursFemaleByIncome,
		HoursMaleByHoursFemale,
		Income,
		IncomeByAge,
		IncomeByAgeSquared,
		IncomeByNumberChildren,
		HoursMale,
		HoursMaleByAgeMale,
		HoursMaleByAgeMaleSquared,
		HoursMaleByNumberChildren,
		HoursMaleByDelderly,
		HoursMaleByDregion,
		HoursFemale,
		HoursFemaleByAgeFemale,
		HoursFemaleByAgeFemaleSquared,
		HoursFemaleByDchildren2under,
		HoursFemaleByDchildren3_6,
		HoursFemaleByDchildren7_12,
		HoursFemaleByDchildren13_17,
		HoursFemaleByDelderly,
		HoursFemaleByDregion,

		FixedCostMaleByNumberChildren,
		FixedCostMaleByDchildren2under,

		FixedCostFemaleByNumberChildren,
		FixedCostFemaleByDchildren2under,
		FixedCostByHighEducation,

		//New set of regressors for LS models from Zhechun:
		IncomeDiv100,                             //Disposable monthly income from donor household divided by 100
		IncomeSqDiv10000,                        //Income squared divided by 10000
		IncomeDiv100_MeanPartnersAgeDiv100,        //Income divided by 100 interacted with mean age of male and female in the household divided by 100
		IncomeDiv100_MeanPartnersAgeSqDiv10000,     //Income divided by 100 interacted with square of mean age of male and female in the household divided by 100
		IncomeDiv100_NChildren017,                 //Income divided by 100 interacted with the number of children aged 0-17
		IncomeDiv100_DChildren2Under,            //Income divided by 100 interacted with dummy for presence of children aged 0-2 in the household
		MaleLeisure,                            //24*7 - labour supply weekly for male
		MaleLeisureSq,
		MaleLeisure_IncomeDiv100,
		MaleLeisure_MaleAgeDiv100,                //Male Leisure interacted with age of male
		MaleLeisure_MaleAgeSqDiv10000,
		MaleLeisure_NChildren017,
		MaleLeisure_DChildren2Under,
		MaleLeisure_MaleDeh_c3_Low,
		MaleLeisure_MaleDeh_c3_Medium,
		MaleLeisure_UKC,
		MaleLeisure_UKD,
		MaleLeisure_UKE,
		MaleLeisure_UKF,
		MaleLeisure_UKG,
		MaleLeisure_UKH,
		MaleLeisure_UKJ,
		MaleLeisure_UKK,
		MaleLeisure_UKL,
		MaleLeisure_UKM,
		MaleLeisure_UKN,
		MaleLeisure_MaleAge50Above,         //Male leisure interacted with dummy for age >= 50
		MaleLeisure_FemaleLeisure,            //Male leisure interacted with female leisure
		FemaleLeisure,                            //24*7 - labour supply weekly for Female
		FemaleLeisureSq,
		FemaleLeisure_IncomeDiv100,
		FemaleLeisure_FemaleAgeDiv100,                //Female Leisure interacted with age of Female
		FemaleLeisure_FemaleAgeSqDiv10000,
		FemaleLeisure_NChildren017,
		FemaleLeisure_DChildren2Under,
		FemaleLeisure_FemaleDeh_c3_Low,
		FemaleLeisure_FemaleDeh_c3_Medium,
		FemaleLeisure_UKC,
		FemaleLeisure_UKD,
		FemaleLeisure_UKE,
		FemaleLeisure_UKF,
		FemaleLeisure_UKG,
		FemaleLeisure_UKH,
		FemaleLeisure_UKJ,
		FemaleLeisure_UKK,
		FemaleLeisure_UKL,
		FemaleLeisure_UKM,
		FemaleLeisure_UKN,
		FemaleLeisure_FemaleAge50Above,         //Female leisure interacted with dummy for age >= 50
		FixedCostMale,
		FixedCostMale_NorthernRegions,
		FixedCostMale_SouthernRegions,
		FixedCostFemale,
		FixedCostFemale_NorthernRegions,
		FixedCostFemale_SouthernRegions,
		FixedCostMale_NChildren017,
		FixedCostMale_DChildren2Under,
		FixedCostFemale_NChildren017,
		FixedCostFemale_DChildren2Under,

		MaleHoursAbove40,
		FemaleHoursAbove40,

		//Additional regressors for single female or single male benefitUnits:

		MaleLeisure_DChildren1317, //Male leisure interacted with dummy for presence of children aged 13-17
		MaleLeisure_DChildren712,  //Male leisure interacted with dummy for presence of children aged 7 - 12
		MaleLeisure_DChildren36,   //Male leisure interacted with dummy for presence of children aged 3 - 6
		MaleLeisure_DChildren017,  //Male leisure interacted with dummy for presence of children aged 0 - 17
		FixedCostMale_Dlltsdsp,    //Fixed cost interacted with dummy for partner being long-term sick or disabled
		FixedCostMale_Lesspc3_Student, //Fixed cost interacted with dummy for partner being a student

		FemaleLeisure_DChildren1317, //Male leisure interacted with dummy for presence of children aged 13-17
		FemaleLeisure_DChildren712,  //Male leisure interacted with dummy for presence of children aged 7 - 12
		FemaleLeisure_DChildren36,   //Male leisure interacted with dummy for presence of children aged 3 - 6
		FemaleLeisure_DChildren017,  //Male leisure interacted with dummy for presence of children aged 0 - 17
		FixedCostFemale_Dlltsdsp,    //Fixed cost interacted with dummy for partner being long-term sick or disabled
		FixedCostFemale_Lesspc3_Student, //Fixed cost interacted with dummy for partner being a student

		//Other
		Homeownership_D, // Indicator: does the benefit unit own home?

		//Enums for use with the tax-benefit matching method
		MaximumAge, // Returns maximum age of responsible individuals in the benefit (tax) unit
		MinimumAge, // Returns maximum age of all people in benefit unit
		NumberMembersOver17, // Return number of members of benefit unit aged over 17
		NumberChildren04,  // Return number of children aged <0;5)
		NumberChildren59,
		NumberChildren1017,
		NumberChildren517, // Return number of children aged <5;17>

		// regressors for childcare costs
		Constant,
		Year_transformed,
		UKC,
		UKD,
		UKE,
		UKF,
		UKG,
		UKH,
		UKI,
		UKJ,
		UKK,
		UKL,
		UKM,
		UKN,
		n_children_0,
		n_children_1,
		n_children_2,
		n_children_3,
		n_children_4,
		n_children_5,
		n_children_6,
		n_children_7,
		n_children_8,
		n_children_9,
		n_children_10,
		n_children_11,
		n_children_12,
		n_children_13,
		n_children_14,
		couple_emp_2ft,
		couple_emp_ftpt,
		couple_emp_2pt,
		couple_emp_ftne,
		couple_emp_ptne,
		couple_emp_2ne,
		single_emp_ft,
		single_emp_pt,
		single_emp_ne,
		Graduate,
		Cut1,       // ordered probit/logit cut points - ignore these when evaluating score
		Cut2,
		Cut3,
		Cut4,
		Cut5,
		Cut6,
		Cut7,
		Cut8,
		Cut9,
		Cut10,
	}

	public int getIntValue(Enum<?> variableID) {

		switch ((Regressors) variableID) {

			case MaximumAge:
				int maxAgeBu = 0;
				if (Occupancy.Couple.equals(occupancy)) {
					if (male.getDag() >= female.getDag()) {
						maxAgeBu = male.getDag();
					} else {
						maxAgeBu = female.getDag();
					}
				} else if (Occupancy.Single_Male.equals(occupancy)) {
					maxAgeBu = male.getDag();
				} else {
					maxAgeBu = female.getDag();
				}
				return maxAgeBu;
			case MinimumAge:
				int age = 200;
				if (male != null) {
					age = male.getDag();
				}
				if (female != null) {
					if (female.getDag() < age) age = female.getDag();
				}
				for (Person person : children) {
					if (person.getDag() < age) age = person.getDag();
				}
				return age;
			case NumberMembersOver17: // Simulated Benefit unit can have at most 2 persons over 17
				if (Occupancy.Couple.equals(occupancy)) {
					return 2;
				} else {
					return 1;
				}
			case NumberChildren04:
				if (n_children_04 != null) {
					return n_children_04;
				} else {
					return 0;
				}
			case NumberChildren59:
				if (n_children_59 != null) {
					return n_children_59;
				} else {
					return 0;
				}
			case NumberChildren1017:
				if (n_children_1017 != null) {
					return n_children_1017;
				} else {
					return 0;
				}
			case NumberChildren517:
				if (n_children_517 != null) {
					return n_children_517;
				} else {
					return 0;
				}
			default:
				throw new IllegalArgumentException("Unsupported variable " + variableID.name() + " in DonorHousehold.getIntValue");

		}
	}

	public double getDoubleValue(Enum<?> variableID) {

		switch ((Regressors) variableID) {

			//New set of regressors for LS models from Zhechun: Couples:
			case IncomeDiv100:                             //Disposable monthly income from donor household divided by 100
				return (getDisposableIncomeMonthlyUpratedToBasePriceYear() -
						getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear()) * 1.e-2;
			case IncomeSqDiv10000:                        //Income squared divided by 10000
				return (getDisposableIncomeMonthlyUpratedToBasePriceYear() -
							getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear()) *
						(getDisposableIncomeMonthlyUpratedToBasePriceYear() -
							getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear()) * 1.e-4;
			case IncomeDiv100_MeanPartnersAgeDiv100:        //Income divided by 100 interacted with mean age of male and female in the household divided by 100
				if(female == null) {        //Single so no need for mean age
					return getDisposableIncomeMonthlyUpratedToBasePriceYear() * male.getDag() * 1.e-4;
				} else if(male == null) {
					return getDisposableIncomeMonthlyUpratedToBasePriceYear() * female.getDag() * 1.e-4;
				} else {        //Must be a couple, so use mean age
					double meanAge = (female.getDag() + male.getDag()) * 0.5;
					return getDisposableIncomeMonthlyUpratedToBasePriceYear() * meanAge * 1.e-4;
				}
			case IncomeDiv100_MeanPartnersAgeSqDiv10000:     //Income divided by 100 interacted with square of mean age of male and female in the household divided by 10000
				if(female == null) {        //Single so no need for mean age
					return getDisposableIncomeMonthlyUpratedToBasePriceYear() * male.getDag() * male.getDag() * 1.e-6;
				} else if(male == null) {
					return getDisposableIncomeMonthlyUpratedToBasePriceYear() * female.getDag() * female.getDag() * 1.e-6;
				} else {        //Must be a couple, so use mean age
					double meanAge = (female.getDag() + male.getDag()) * 0.5;
					return getDisposableIncomeMonthlyUpratedToBasePriceYear() * meanAge * meanAge * 1.e-6;
				}
			case IncomeDiv100_NChildren017:                 //Income divided by 100 interacted with the number of children aged 0-17
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * n_children_017 * 1.e-2;
			case IncomeDiv100_DChildren2Under:            //Income divided by 100 interacted with dummy for presence of children aged 0-2 in the household
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * d_children_2under.ordinal() * 1.e-2;
			case MaleLeisure:                            //24*7 - labour supply weekly for male
				return Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly();
			case MaleLeisureSq:
				return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly()) * (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
			case MaleLeisure_IncomeDiv100:
				return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly()) * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-2;
			case MaleLeisure_MaleAgeDiv100:                //Male Leisure interacted with age of male
				return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly()) * male.getDag() * 1.e-2;
			case MaleLeisure_MaleAgeSqDiv10000:
				return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly()) * male.getDag() * male.getDag() * 1.e-4;
			case MaleLeisure_NChildren017:
				return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly()) * n_children_017;
			case MaleLeisure_DChildren2Under:
				return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly()) * d_children_2under.ordinal();
			case MaleLeisure_MaleDeh_c3_Low:
				if(male.getDeh_c3().equals(Education.Low)) {
					return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
				} else return 0.;
			case MaleLeisure_MaleDeh_c3_Medium:
				if(male.getDeh_c3().equals(Education.Medium)) {
					return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
				} else return 0.;
			case MaleLeisure_UKC:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKC)) {
						return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case MaleLeisure_UKD:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKD)) {
						return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case MaleLeisure_UKE:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKE)) {
						return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case MaleLeisure_UKF:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKF)) {
						return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case MaleLeisure_UKG:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKG)) {
						return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case MaleLeisure_UKH:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKH)) {
						return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case MaleLeisure_UKJ:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKJ)) {
						return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case MaleLeisure_UKK:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKK)) {
						return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case MaleLeisure_UKL:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKL)) {
						return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case MaleLeisure_UKM:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKM)) {
						return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case MaleLeisure_UKN:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKN)) {
						return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case MaleLeisure_MaleAge50Above:
				if (male.getDag() >= 50) {
					return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly());
				} else return 0.;
			case MaleLeisure_FemaleLeisure:            //Male leisure interacted with female leisure
				return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly()) * (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
			case FemaleLeisure:                            //24*7 - labour supply weekly for Female
				return Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly();
			case FemaleLeisureSq:
				return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly()) * (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
			case FemaleLeisure_IncomeDiv100:
				return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly()) * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-2;
			case FemaleLeisure_FemaleAgeDiv100:                //Female Leisure interacted with age of Female
				return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly()) * female.getDag() * 1.e-2;
			case FemaleLeisure_FemaleAgeSqDiv10000:
				return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly()) * female.getDag() * female.getDag() * 1.e-4;
			case FemaleLeisure_NChildren017:
				return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly()) * n_children_017;
			case FemaleLeisure_DChildren2Under:
				return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly()) * d_children_2under.ordinal();
			case FemaleLeisure_FemaleDeh_c3_Low:
				if(female.getDeh_c3().equals(Education.Low)) {
					return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
				} else return 0.;
			case FemaleLeisure_FemaleDeh_c3_Medium:
				if(female.getDeh_c3().equals(Education.Medium)) {
					return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
				} else return 0.;
			case FemaleLeisure_UKC:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKC)) {
						return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case FemaleLeisure_UKD:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKD)) {
						return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case FemaleLeisure_UKE:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKE)) {
						return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case FemaleLeisure_UKF:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKF)) {
						return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case FemaleLeisure_UKG:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKG)) {
						return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case FemaleLeisure_UKH:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKH)) {
						return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case FemaleLeisure_UKJ:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKJ)) {
						return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case FemaleLeisure_UKK:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKK)) {
						return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case FemaleLeisure_UKL:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKL)) {
						return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case FemaleLeisure_UKM:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKM)) {
						return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case FemaleLeisure_UKN:
				if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKN)) {
						return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
					} else return 0.;
				} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
			case FemaleLeisure_FemaleAge50Above:
				if (female.getDag() >= 50) {
					return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly());
				} else return 0.;
				//Note: In the previous version of the model, Fixed Cost was returning -1 to match the regression coefficients
			case FixedCostMale:
				if(male.getLabourSupplyHoursWeekly() > 0) {
					return 1.;
				} else return 0.;
			case FixedCostMale_NorthernRegions:
				if(male.getLabourSupplyHoursWeekly() > 0 && (region.equals(Region.ITC) || region.equals(Region.ITH))) {
					return 1.;
				} else return 0.;
			case FixedCostMale_SouthernRegions:
				if(male.getLabourSupplyHoursWeekly() > 0 && (region.equals(Region.ITF) || region.equals(Region.ITG))) {
					return 1.;
				} else return 0.;
			case FixedCostFemale:
				if(female.getLabourSupplyHoursWeekly() > 0) {
					return 1.;
				} else return 0.;
			case FixedCostFemale_NorthernRegions:
				if(female.getLabourSupplyHoursWeekly() > 0 && (region.equals(Region.ITC) || region.equals(Region.ITH))) {
					return 1.;
				} else return 0.;
			case FixedCostFemale_SouthernRegions:
				if(female.getLabourSupplyHoursWeekly() > 0 && (region.equals(Region.ITF) || region.equals(Region.ITG))) {
					return 1.;
				} else return 0.;
			case FixedCostMale_NChildren017:
				if(male.getLabourSupplyHoursWeekly() > 0) {
					return n_children_017;
				} else return 0.;
			case FixedCostMale_DChildren2Under:
				if(male.getLabourSupplyHoursWeekly() > 0) {
					return d_children_2under.ordinal();
				} else return 0.;
			case FixedCostFemale_NChildren017:
				if(female.getLabourSupplyHoursWeekly() > 0) {
					return n_children_017;
				} else return 0.;
			case FixedCostFemale_DChildren2Under:
				if(female.getLabourSupplyHoursWeekly() > 0) {
					return d_children_2under.ordinal();
				} else return 0.;
			case MaleHoursAbove40:
				if (male.getLabourSupplyHoursWeekly() >= 40) {
					return 1.;
				} else return 0.;
			case FemaleHoursAbove40:
				if (female.getLabourSupplyHoursWeekly() >= 40) {
					return 1.;
				} else return 0.;
				//Additional regressors for single female or single male benefitUnits:
				//Note: couples in which one person is not at risk of work have utility set according to the process for singles
			case MaleLeisure_DChildren1317: //Male leisure interacted with dummy for presence of children aged 13-17
				return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly()) * d_children_13_17.ordinal();
			case MaleLeisure_DChildren712:  //Male leisure interacted with dummy for presence of children aged 7 - 12
				return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly()) * d_children_7_12.ordinal();
			case MaleLeisure_DChildren36:   //Male leisure interacted with dummy for presence of children aged 3 - 6
				return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly()) * d_children_3_6.ordinal();
			case MaleLeisure_DChildren017:  //Male leisure interacted with dummy for presence of children aged 0 - 17
				if(n_children_017 > 0) { //Instead of creating a new variable, use number of children aged 0 - 17
					return Parameters.HOURS_IN_WEEK - male.getLabourSupplyHoursWeekly();
				} else return 0.;
				//The following two regressors refer to a partner in single LS model - this is for those with an inactive partner, but not everyone will have a partner so check for nulls
			case FixedCostMale_Dlltsdsp:    //Fixed cost interacted with dummy for partner being long-term sick or disabled
				if(male.getLabourSupplyHoursWeekly() > 0) {
					if(female != null) {
						return female.getDlltsd().ordinal(); //==1 if partner is long-term sick or disabled
					} else return 0.;
				} else return 0.;
			case FixedCostMale_Lesspc3_Student: //Fixed cost interacted with dummy for partner being a student
				if(male.getLabourSupplyHoursWeekly() > 0) {
					if(female != null && female.getLes_c4().equals(Les_c4.Student)) {
						return 1.; //Partner must be female - if a student, return 1
					} else return 0.;
				} else return 0.;

			case FemaleLeisure_DChildren1317: //Male leisure interacted with dummy for presence of children aged 13-17
				return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly()) * d_children_13_17.ordinal();
			case FemaleLeisure_DChildren712:  //Male leisure interacted with dummy for presence of children aged 7 - 12
				return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly()) * d_children_7_12.ordinal();
			case FemaleLeisure_DChildren36:   //Male leisure interacted with dummy for presence of children aged 3 - 6
				return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly()) * d_children_3_6.ordinal();
			case FemaleLeisure_DChildren017:  //Male leisure interacted with dummy for presence of children aged 0 - 17
				if(n_children_017 > 0) { //Instead of creating a new variable, use number of children aged 0 - 17
					return Parameters.HOURS_IN_WEEK - female.getLabourSupplyHoursWeekly();
				} else return 0.;
			case FixedCostFemale_Dlltsdsp:    //Fixed cost interacted with dummy for partner being long-term sick or disabled
				if(female.getLabourSupplyHoursWeekly() > 0) {
					if(male != null) {
						return male.getDlltsd().ordinal(); //==1 if partner is long-term sick or disabled
					} else return 0.;
				} else return 0.;
			case FixedCostFemale_Lesspc3_Student:
				if(female.getLabourSupplyHoursWeekly() > 0) {
					if(male != null && male.getLes_c4().equals(Les_c4.Student)) {
						return 1.; //Partner must be male - if a student, return 1
					} else return 0.;
				} else return 0.;


				//Values are divided by powers of 10, as in the tables of Bargain et al. (2014) Working Paper
			case IncomeSquared:        //Income is disposable income, inputed from 'donor' benefitUnits in EUROMOD
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-4;
			case HoursMaleSquared:
				return male.getLabourSupplyHoursWeekly() * male.getLabourSupplyHoursWeekly();
			case HoursFemaleSquared:
				return female.getLabourSupplyHoursWeekly() * female.getLabourSupplyHoursWeekly();
			case HoursMaleByIncome:
				return male.getLabourSupplyHoursWeekly() * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-3;
			case HoursFemaleByIncome:
				return female.getLabourSupplyHoursWeekly() * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-3;
			case HoursMaleByHoursFemale:
				return male.getLabourSupplyHoursWeekly() * female.getLabourSupplyHoursWeekly() * 1.e-3;
			case Income:
				return getDisposableIncomeMonthlyUpratedToBasePriceYear();
			case IncomeByAge:        //Use mean age for couples
				if(female == null) {        //Single so no need for mean age
					return getDisposableIncomeMonthlyUpratedToBasePriceYear() * male.getDag() * 1.e-1;
				} else if(male == null) {
					return getDisposableIncomeMonthlyUpratedToBasePriceYear() * female.getDag() * 1.e-1;
				} else {        //Must be a couple, so use mean age
					double meanAge = (female.getDag() + male.getDag()) * 0.5;
					return getDisposableIncomeMonthlyUpratedToBasePriceYear() * meanAge * 1.e-1;
				}
			case IncomeByAgeSquared:        //Use mean age for couples
				if(female == null) {        //Single so no need for mean age
					return getDisposableIncomeMonthlyUpratedToBasePriceYear() * male.getDag() * male.getDag() * 1.e-2;
				} else if(male == null) {
					return getDisposableIncomeMonthlyUpratedToBasePriceYear() * female.getDag() * female.getDag() * 1.e-2;
				} else {        //Must be a couple, so use mean age
					double meanAge = (female.getDag() + male.getDag()) * 0.5;
					return getDisposableIncomeMonthlyUpratedToBasePriceYear() * meanAge * meanAge * 1.e-2;
				}
			case IncomeByNumberChildren:
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * children.size();
			case HoursMale:
				return male.getLabourSupplyHoursWeekly();
			case HoursMaleByAgeMale:
				return male.getLabourSupplyHoursWeekly() * male.getDag() * 1.e-1;
			case HoursMaleByAgeMaleSquared:
				return male.getLabourSupplyHoursWeekly() * male.getDag() * male.getDag() * 1.e-2;
			case HoursMaleByNumberChildren:
				return male.getLabourSupplyHoursWeekly() * children.size();
			case HoursMaleByDelderly:        //Appears only in Single Males regression, not Couple.
				return 0.;        //Our model doesn't take account of elderly (as people move out of parental home when 18 years old, and we do not provide a mechanism for parents to move back in.
			case HoursMaleByDregion:
				if(model.getCountry().equals(Country.IT)) {
					if(getRegion().equals(Region.ITF) || getRegion().equals(Region.ITG)) {        //For South Italy (Sud) and Islands (Isole)
						return male.getLabourSupplyHoursWeekly();
					} else return 0.;
				} else if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKI)) {        //For London
						return male.getLabourSupplyHoursWeekly();
					} else return 0.;
				} else throw new IllegalArgumentException("Error - household " + this.getKey().getId() + " has region " + getRegion() + " which is not yet handled in DonorHousehold.getDoubleValue()!");

			case HoursFemale:
				return female.getLabourSupplyHoursWeekly();
			case HoursFemaleByAgeFemale:
				return female.getLabourSupplyHoursWeekly() * female.getDag() * 1.e-1;
			case HoursFemaleByAgeFemaleSquared:
				return female.getLabourSupplyHoursWeekly() * female.getDag() * female.getDag() * 1.e-2;
			case HoursFemaleByDchildren2under:
				return female.getLabourSupplyHoursWeekly() * d_children_2under.ordinal();
			case HoursFemaleByDchildren3_6:
				return female.getLabourSupplyHoursWeekly() * d_children_3_6.ordinal();
			case HoursFemaleByDchildren7_12:
				return female.getLabourSupplyHoursWeekly() * d_children_7_12.ordinal();
			case HoursFemaleByDchildren13_17:
				return female.getLabourSupplyHoursWeekly() * d_children_13_17.ordinal();
			case HoursFemaleByDelderly:
				return 0.;        //Our model doesn't take account of elderly (as people move out of parental home when 18 years old, and we do not provide a mechanism for parents to move back in.
			case HoursFemaleByDregion:        //Value of hours are already taken into account by multiplying regression coefficients in Parameters class
				if(model.getCountry().equals(Country.IT)) {
					if(getRegion().equals(Region.ITF) || getRegion().equals(Region.ITG)) {        //For South Italy (Sud) and Islands (Isole)
						return female.getLabourSupplyHoursWeekly();
					} else return 0.;
				} else if(model.getCountry().equals(Country.UK)) {
					if(getRegion().equals(Region.UKI)) {        //For London
						return female.getLabourSupplyHoursWeekly();
					} else return 0.;
				} else throw new IllegalArgumentException("Error - household " + this.getKey().getId() + " has region " + getRegion() + " which is not yet handled in DonorHousehold.getDoubleValue()!");

				//The following regressors for FixedCosts appear as negative in the Utility regression, and so are multiplied by a factor of -1 below.
				//The following regressors only apply when the male hours worked is greater than 0

			case FixedCostMaleByNumberChildren:
				if(male.getLabourSupplyHoursWeekly() > 0) {
					return - children.size();        //Return negative as costs appear negative in utility function equation
				} else return 0.;

			case FixedCostMaleByDchildren2under:
				if(male.getLabourSupplyHoursWeekly() > 0) {
					return - d_children_2under.ordinal();        //Return negative as costs appear negative in utility function equation
				} else return 0.;

				//The following regressors only apply when the female hours worked is greater than 0


			case FixedCostFemaleByNumberChildren:
				if(female.getLabourSupplyHoursWeekly() > 0) {
					return - children.size();        //Return negative as costs appear negative in utility function equation
				} else return 0.;

			case FixedCostFemaleByDchildren2under:
				if(female.getLabourSupplyHoursWeekly() > 0) {
					return - d_children_2under.ordinal();        //Return negative as costs appear negative in utility function equation
				} else return 0.;

				//Only appears in regressions for Singles not Couples.  Applies when the single person in the household has hours worked > 0
			case FixedCostByHighEducation:
				if(female == null) {        //For single males
					if(male.getLabourSupplyHoursWeekly() > 0) {
						return male.getDeh_c3().equals(Education.High) ? -1. : 0.;
					} else return 0.;
				} else if (male == null) {    //For single females
					if(female.getLabourSupplyHoursWeekly() > 0) {
						return female.getDeh_c3().equals(Education.High) ? -1. : 0.;
					} else return 0.;
				} else throw new IllegalArgumentException("Error - FixedCostByHighEducation regressor should only be called for Households containing single people (with or without children), however household " + key.getId() + " has a couple, with male " + male.getKey().getId() + " and female " + female.getKey().getId());
			case Homeownership_D:
				return isDhh_owned()? 1. : 0.;
			case Constant:
				return 1.0;
			case Year_transformed:
				return (Parameters.isFixTimeTrend && getYear() >= Parameters.timeTrendStopsIn) ? (double) Parameters.timeTrendStopsIn - 2000 : (double) getYear() - 2000;
			case couple_emp_2ft:
				return (Occupancy.Couple.equals(occupancy) && (getMinWeeklyHoursWorked() >= Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)) ? 1.0 : 0.0;
			case couple_emp_ftpt:
				return (Occupancy.Couple.equals(occupancy) &&
						(getMinWeeklyHoursWorked() > 0) && (getMinWeeklyHoursWorked() < Parameters.MIN_HOURS_FULL_TIME_EMPLOYED) &&
						(getMaxWeeklyHoursWorked() >= Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)) ? 1.0 : 0.0;
			case couple_emp_2pt:
				return (Occupancy.Couple.equals(occupancy) &&
						(getMinWeeklyHoursWorked() > 0) &&
						(getMaxWeeklyHoursWorked() < Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)) ? 1.0 : 0.0;
			case couple_emp_ftne:
				return (Occupancy.Couple.equals(occupancy) &&
						(getMinWeeklyHoursWorked() == 0) &&
						(getMaxWeeklyHoursWorked() >= Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)) ? 1.0 : 0.0;
			case couple_emp_ptne:
				return (Occupancy.Couple.equals(occupancy) &&
						(getMinWeeklyHoursWorked() == 0) &&
						(getMaxWeeklyHoursWorked() > 0) &&
						(getMaxWeeklyHoursWorked() < Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)) ? 1.0 : 0.0;
			case couple_emp_2ne:
				return (Occupancy.Couple.equals(occupancy) && (getMaxWeeklyHoursWorked() == 0)) ? 1.0 : 0.0;
			case single_emp_ft:
				return (!Occupancy.Couple.equals(occupancy) && (getMinWeeklyHoursWorked() >= Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)) ? 1.0 : 0.0;
			case single_emp_pt:
				return (!Occupancy.Couple.equals(occupancy) &&
						(getMinWeeklyHoursWorked() > 0) &&
						(getMaxWeeklyHoursWorked() < Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)) ? 1.0 : 0.0;
			case single_emp_ne:
				return (!Occupancy.Couple.equals(occupancy) && (getMaxWeeklyHoursWorked() == 0)) ? 1.0 : 0.0;
			case Graduate:
				return (Education.High.equals(getHighestDehC3())) ? 1.0 : 0.0;
			case UKC:
				return Region.UKC.equals(region) ? 1.0 : 0.0;
			case UKD:
				return Region.UKD.equals(region) ? 1.0 : 0.0;
			case UKE:
				return Region.UKE.equals(region) ? 1.0 : 0.0;
			case UKF:
				return Region.UKF.equals(region) ? 1.0 : 0.0;
			case UKG:
				return Region.UKG.equals(region) ? 1.0 : 0.0;
			case UKH:
				return Region.UKH.equals(region) ? 1.0 : 0.0;
			case UKI:
				return Region.UKI.equals(region) ? 1.0 : 0.0;
			case UKJ:
				return Region.UKJ.equals(region) ? 1.0 : 0.0;
			case UKK:
				return Region.UKK.equals(region) ? 1.0 : 0.0;
			case UKL:
				return Region.UKL.equals(region) ? 1.0 : 0.0;
			case UKM:
				return Region.UKM.equals(region) ? 1.0 : 0.0;
			case UKN:
				return Region.UKN.equals(region) ? 1.0 : 0.0;
			case n_children_0:
				return (n_children_0 == null) ? 0.0 : n_children_0;
			case n_children_1:
				return (n_children_1 == null) ? 0.0 : n_children_1;
			case n_children_2:
				return (n_children_2 == null) ? 0.0 : n_children_2;
			case n_children_3:
				return (n_children_3 == null) ? 0.0 : n_children_3;
			case n_children_4:
				return (n_children_4 == null) ? 0.0 : n_children_4;
			case n_children_5:
				return (n_children_5 == null) ? 0.0 : n_children_5;
			case n_children_6:
				return (n_children_6 == null) ? 0.0 : n_children_6;
			case n_children_7:
				return (n_children_7 == null) ? 0.0 : n_children_7;
			case n_children_8:
				return (n_children_8 == null) ? 0.0 : n_children_8;
			case n_children_9:
				return (n_children_9 == null) ? 0.0 : n_children_9;
			case n_children_10:
				return (n_children_10 == null) ? 0.0 : n_children_10;
			case n_children_11:
				return (n_children_11 == null) ? 0.0 : n_children_11;
			case n_children_12:
				return (n_children_12 == null) ? 0.0 : n_children_12;
			case n_children_13:
				return (n_children_13 == null) ? 0.0 : n_children_13;
			case n_children_14:
				return (n_children_14 == null) ? 0.0 : n_children_14;
			case Cut1:
				// ordered probit/logit cut points ignored when calculating score
				return 0.;
			case Cut2:
				return 0.;
			case Cut3:
				return 0.;
			case Cut4:
				return 0.;
			case Cut5:
				return 0.;
			case Cut6:
				return 0.;
			case Cut7:
				return 0.;
			case Cut8:
				return 0.;
			case Cut9:
				return 0.;
			case Cut10:
				return 0.;

			default:
				throw new IllegalArgumentException("Unsupported regressor " + variableID.name() + " in BenefitUnit");

		}
	}


	////////////////////////////////////////////////////////////////////////////////
	//
	//	Override equals and hashCode to make unique BenefitUnit determined by Key.getId()
	//
	////////////////////////////////////////////////////////////////////////////////

	@Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof BenefitUnit)) {
            return false;
        }

        BenefitUnit h = (BenefitUnit) o;

        boolean idIsEqual = new EqualsBuilder()
                .append(key.getId(), h.key.getId())        //Add more fields to compare to check for equality if desired
                .isEquals();
        return idIsEqual;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(key.getId())
                .toHashCode();
    }

	@Override
    public int compareTo(BenefitUnit benefitUnit) {
		return (int) (key.getId() - benefitUnit.getKey().getId());
    }


	////////////////////////////////////////////////////////////////////////////////
	//
	//	Other methods
	//
	////////////////////////////////////////////////////////////////////////////////


	public void newBornUpdate() {        //For use in birth process
		n_children_0++;
		d_children_2under = Indicator.True;
		d_children_3under = Indicator.True;
	}

	public void updateOccupancy() {
		// updates occupancy

		if (female != null) {
			if (female.getBenefitUnit() == null) {
				throw new RuntimeException("Female member of benefit unit does not reference the benefit unit");
			}
			if (female.getPartner() != null) {
				if (female.getPartner().getBenefitUnit() == null) {
					throw new RuntimeException("Partner of female member of benefit unit does not reference the benefit unit");
				}
			}
		}
		if (male != null) {
			if (male.getBenefitUnit() == null) {
				throw new RuntimeException("Male member of benefit unit does not reference the benefit unit");
			}
			if (male.getPartner() != null) {
				if (male.getPartner().getBenefitUnit() == null) {
					throw new RuntimeException("Partner of male member of benefit unit does not reference the benefit unit");
				}
			}
		}
		if (female == null && male == null) {

			throw new IllegalArgumentException("Error - update occupancy for benefit unit with no adult");
		} else if (female!=null && male!=null) {

			occupancy = Occupancy.Couple;
		} else if (male!=null) {

			if (male.getPartner() == null) {

				occupancy = Occupancy.Single_Male;
			} else if (male.getBenefitUnit().getKey().getId() == male.getPartner().getBenefitUnit().getKey().getId()) {

				setFemale(male.getPartner());
				occupancy = Occupancy.Couple;
				if (male.getBenefitUnit() != this) {
					male.setBenefitUnit(this);
					female.setBenefitUnit(this);
				}
			} else {

				occupancy = Occupancy.Single_Male;
				male.setPartner(null);
			}
		} else {

			if (female.getPartner() == null) {

				occupancy = Occupancy.Single_Female;
			} else if (female.getBenefitUnit().getKey().getId() == female.getPartner().getBenefitUnit().getKey().getId()) {

				setMale(female.getPartner());
				occupancy = Occupancy.Couple;
				if (female.getBenefitUnit() != this) {
					female.setBenefitUnit(this);
					male.setBenefitUnit(this);
				}
			} else {

				occupancy = Occupancy.Single_Female;
				female.setPartner(null);
			}
		}
		updateSizeAndWeight();
	}

	/*
	 * If any single member of a household is at risk of work, the household is at risk of work
	 */
	public boolean getAtRiskOfWork() {

		boolean atRiskOfWork = false;
		if(female != null) {
			atRiskOfWork = female.atRiskOfWork();
		}
		if(atRiskOfWork == false && male != null) {        //Can skip checking if atRiskOfWork is true already
			atRiskOfWork = male.atRiskOfWork();
		}
		return atRiskOfWork;
	}

	protected void homeownership() {

		ValidHomeownersCSfilter filter = new ValidHomeownersCSfilter();
		if(filter.isFiltered(this)) {

			if (Occupancy.Couple.equals(occupancy)) {
				boolean male_homeowner = (homeOwnerInnov.nextDouble() < Parameters.getRegHomeownershipHO1a().getProbability(male, Person.DoublesVariables.class));
				boolean female_homeowner = (homeOwnerInnov.nextDouble() < Parameters.getRegHomeownershipHO1a().getProbability(female, Person.DoublesVariables.class));

				male.setDhh_owned(male_homeowner);
				female.setDhh_owned(female_homeowner);

				if (!male_homeowner && !female_homeowner) { //If neither person in the BU is a homeowner, BU not classified as owning home
					setDhh_owned(false);
				} else {
					setDhh_owned(true);
				}

			} else if (Occupancy.Single_Female.equals(occupancy)) {
				boolean female_homeowner = (homeOwnerInnov.nextDouble() < Parameters.getRegHomeownershipHO1a().getProbability(female, Person.DoublesVariables.class));

				female.setDhh_owned(female_homeowner);
				setDhh_owned(female_homeowner);

			} else if (Occupancy.Single_Male.equals(occupancy)) {
				boolean male_homeowner = (homeOwnerInnov.nextDouble() < Parameters.getRegHomeownershipHO1a().getProbability(male, Person.DoublesVariables.class));

				male.setDhh_owned(male_homeowner);
				setDhh_owned(male_homeowner);

			} else {
				throw new IllegalArgumentException("Benefit unit " + getKey().getId() + " has incorrect occupancy.");
			}
		}
	}

	/*
	 * For variables that are equivalised, this method calculates the household's equivalised weight to use with them
	 */
	public void calculateEquivalisedWeight() {
		//Equivalence scale gives a weight of 1.0 to the first adult;
		//0.5 to the second and each subsequent person aged 14 and over;
		//0.3 to each child aged under 14.

		if(Occupancy.Couple.equals(occupancy)) {
			equivalisedWeight = 1.5;        //1 for the first person, 0.5 for the second of the couple.
		} else equivalisedWeight = 1.;        //Must be a single responsible adult

		for(Person child : children) {
			if(child.getDag() < 14) {
				equivalisedWeight += 0.3;
			} else {
				equivalisedWeight += 0.5;
			}
		}
	}

	public double calculateEquivalisedDisposableIncomeYearly() {

		calculateEquivalisedWeight();

		if(getDisposableIncomeMonthly() != null && Double.isFinite(getDisposableIncomeMonthly())) {
			equivalisedDisposableIncomeYearly = (getDisposableIncomeMonthly() / getEquivalisedWeight()) * 12;
		} else {
			equivalisedDisposableIncomeYearly = 0.;
		}
		return equivalisedDisposableIncomeYearly;
	}


	/*
        This method calculates the change in benefit unit's equivalised disposable income for use with Step 2 of mental health determination
    */
	private double calculateYearlyChangeInLogEquivalisedDisposableIncome() {
		double yearlyChangeInLogEquivalisedDisposableIncome = 0.;
		if (equivalisedDisposableIncomeYearly != null && equivalisedDisposableIncomeYearly_lag1 != null && equivalisedDisposableIncomeYearly >= 0. && equivalisedDisposableIncomeYearly_lag1 >= 0.) {
			// Note that income is uprated to the base price year, as specified in parameters class, as the estimated change uses real income change
			// +1 added as log(0) is not defined
			yearlyChangeInLogEquivalisedDisposableIncome =
					Math.log(equivalisedDisposableIncomeYearly / Parameters.getTimeSeriesValue(model.getYear(), TimeSeriesVariable.Inflation) + 1)
							- Math.log(equivalisedDisposableIncomeYearly_lag1 / Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.Inflation) + 1);
		}
		yearlyChangeInLogEDI = yearlyChangeInLogEquivalisedDisposableIncome;
		return yearlyChangeInLogEquivalisedDisposableIncome;
	}


	////////////////////////////////////////////////////////////////////////////////
	//
	//	Access Methods
	//
	////////////////////////////////////////////////////////////////////////////////


    /**
	 *
	 * Returns a defensive copy of the field.
	 * The caller of this method can do anything they want with the
	 * returned Key object, without affecting the internals of this
	 * class in any way.
	 *
	 */
	public PanelEntityKey getKey() {
		return new PanelEntityKey(key.getId());
	}

	public double getLiquidWealth() {
		return getLiquidWealth(true);
	}
	public double getLiquidWealth(boolean throwError) {
		if (liquidWealth == null) {
			if (throwError) {
				throw new RuntimeException("Call to get benefit unit wealth before it is initialised.");
			} else {
				return 0.0;
			}
		} else {
			return liquidWealth;
		}
	}

	public double getChildcareCostPerWeek() {
		return getChildcareCostPerWeek(true);
	}
	public double getChildcareCostPerWeek(boolean throwError) {
		if (childcareCostPerWeek == null) {
			if (throwError) {
				throw new RuntimeException("Call to get benefit unit childcare cost before it is initialised.");
			} else {
				return 0.0;
			}
		} else {
			return childcareCostPerWeek;
		}
	}

	public double getSocialCareCostPerWeek() {
		return getSocialCareCostPerWeek(true);
	}
	public double getSocialCareCostPerWeek(boolean throwError) {
		if (socialCareCostPerWeek == null) {
			if (throwError) {
				throw new RuntimeException("Call to get benefit unit social care cost before it is initialised.");
			} else {
				return 0.0;
			}
		} else {
			return socialCareCostPerWeek;
		}
	}

	public void setLiquidWealth(double liquidWealth) {
		this.liquidWealth = liquidWealth;
	}

	public void setChildren(Set<Person> children) {
		this.children = children;
	}

	public void setFemale(Person person) {

		if (person != null) {

			if (female != null) {
				throw new IllegalArgumentException("Benefit unit " + key.getId() + " already has a female.  Remove existing female before adding another one.");
			}
			if (!person.getDgn().equals(Gender.Female)) {
				throw new IllegalArgumentException("Person " + person.getKey().getId() + " does not have gender = Female, so cannot be the responsible female of the benefit unit.");
			}

			// link person with benefit unit
			female = person;
			idFemale = person.getKey().getId();

			checkIfUpdatePersonReferences(person);
			if (male != null) {
				male.setPartner(person);
				person.setPartner(male);
				occupancy = Occupancy.Couple;
			} else {
				occupancy = Occupancy.Single_Female;
			}
			for (Person child : children) {
				child.setIdMother(female);
			}
		} else {

			female = null;
			idFemale = null;
			if (male != null) {
				male.setPartner(null);
				occupancy = Occupancy.Single_Male;
			}
			for (Person child : children) {
				child.setIdMother((Person)null);
			}
		}
	}

	public void setMale(Person person) {

		if (person != null) {

			if (male != null) {
				throw new RuntimeException("Benefit unit " + key.getId() + " already has a male.  Remove existing male before adding another one.");
			}
			if (!person.getDgn().equals(Gender.Male)) {
				throw new RuntimeException("Person " + person.getKey().getId() + " does not have gender = Male, so cannot be the responsible male of the benefit unit.");
			}

			// link person with benefit unit
			male = person;
			idMale = person.getKey().getId();

			checkIfUpdatePersonReferences(person);
			if (female != null) {
				female.setPartner(person);
				person.setPartner(female);
				occupancy = Occupancy.Couple;
			} else {
				occupancy = Occupancy.Single_Male;
			}
			for (Person child : children) {
				child.setIdFather(male);
			}
		} else {
			male = null;
			idMale = null;
			if (female != null) {
				female.setPartner(null);
				occupancy = Occupancy.Single_Female;
			}
			for (Person child : children) {
				child.setIdFather((Person)null);
			}
		}
	}

	public void checkIfUpdatePersonReferences(Person person) {

		if (!this.equals(person.getBenefitUnit())) {
			if (person.getBenefitUnit() != null) {
				person.getBenefitUnit().removePerson(person);
			}
			person.setBenefitUnit(this);
		}
	}

	public Household getHousehold() {
		return household;
	}

	public void setHousehold(Household newHousehold) {

		if (newHousehold == null) {

			household = null;
			idHousehold = null;
		} else {

			if (household != null) {
				household.removeBenefitUnit(this);
			}
			household = newHousehold;
			idHousehold = newHousehold.getId();
			if(!newHousehold.getBenefitUnitSet().contains(this)) newHousehold.addBenefitUnit(this);
		}

		// update benefit unit members
		Set<Person> persons = getPersonsInBU();
		for (Person person : persons) {
			person.setIdHousehold(idHousehold);
		}
	}

	public Person getFemale() {
		return female;
	}

	public Person getMale() {
		return male;
	}

	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public Set<Person> getChildren() {
		return children;
	}

	public Long getIdFemale() {
		return idFemale;
	}

	public Long getIdMale() {
		return idMale;
	}

	public Indicator getD_children_3under() {
		return d_children_3under;
	}

	public Indicator getD_children_4_12() {
		return d_children_4_12;
	}

	public Indicator getD_children_2under() {
		return d_children_2under;
	}

	public Indicator getD_children_3_6() {
		return d_children_3_6;
	}

	public Indicator getD_children_7_12() {
		return d_children_7_12;
	}

	public Indicator getD_children_13_17() {
		return d_children_13_17;
	}

	public Indicator getD_children_18over() {
		return d_children_18over;
	}

	public Indicator getD_children_3under_lag() {
		return d_children_3under_lag;
	}

	public Indicator getD_children_4_12_lag() {
		return d_children_4_12_lag;
	}

	public Integer getN_children_0() {
		return n_children_0;
	}

	public Integer getN_children_1() {
		return n_children_1;
	}

	public Integer getN_children_2() {
		return n_children_2;
	}

	public Integer getN_children_3() {
		return n_children_3;
	}

	public Integer getN_children_4() {
		return n_children_4;
	}

	public Integer getN_children_5() {
		return n_children_5;
	}

	public Integer getN_children_6() {
		return n_children_6;
	}

	public Integer getN_children_7() {
		return n_children_7;
	}

	public Integer getN_children_8() {
		return n_children_8;
	}

	public Integer getN_children_9() {
		return n_children_9;
	}

	public Integer getN_children_10() {
		return n_children_10;
	}

	public Integer getN_children_11() {
		return n_children_11;
	}

	public Integer getN_children_12() {
		return n_children_12;
	}

	public Integer getN_children_13() {
		return n_children_13;
	}

	public Integer getN_children_14() {
		return n_children_14;
	}

	public Integer getN_children_15() {
		return n_children_15;
	}

	public Integer getN_children_16() {
		return n_children_16;
	}

	public Integer getN_children_17() {
		return n_children_17;
	}

	public int getN_children_allAges() {
		return getN_children_allAges(true);
	}

	public int getN_children_allAges(boolean throwError) {
		if (n_children_allAges == null) {
			if (throwError) {
				throw new RuntimeException("get number of children all ages before initialised");
			} else {
				return 0;
			}
		} else {
			return n_children_allAges;
		}
	}


	public void setN_children_allAges(Integer n_children_allAges) {
		this.n_children_allAges = n_children_allAges;
	}

	public Integer getN_children_allAges_lag1() {
		return n_children_allAges_lag1;
	}


	public Integer getN_children_02() {
		return n_children_02;
	}


	public Integer getN_children_02_lag1() {
		return n_children_02_lag1;
	}

    public Integer getN_children_017() {
        return n_children_017;
    }

	public double getEquivalisedDisposableIncomeYearly() {
		double val;
		if (equivalisedDisposableIncomeYearly != null) {
			val = equivalisedDisposableIncomeYearly;
		} else {
			val = -9999.99;
		}
		return val;
	}

	public int getAtRiskOfPoverty() {
		if (atRiskOfPoverty != null) {
			return atRiskOfPoverty;
		} else return 0;
	}

	public Integer getAtRiskOfPoverty_lag1() {
		return atRiskOfPoverty_lag1;
	}

	public void setAtRiskOfPoverty(Integer atRiskOfPoverty) {
		this.atRiskOfPoverty = atRiskOfPoverty;
	}

	public Double getDisposableIncomeMonthly() {
		return disposableIncomeMonthly;
	}

	public Double getGrossIncomeMonthly() {
		return grossIncomeMonthly;
	}

	public Double getBenefitsReceivedPerMonth() {
		return benefitsReceivedPerMonth;
	}

	public Occupancy getOccupancy() {
		return occupancy;
	}

	public int getCoupleOccupancy() {

		if(Occupancy.Couple.equals(occupancy)) {
			return 1;
		} else return 0;

	}

	public long getId() {
		return key.getId();
	}

	public int getSize() {
		if (size == null){
			return 0;
		} else {
			return size;
		}
	}


	public Ydses_c5 getYdses_c5() {
		return ydses_c5;
	}

	public Ydses_c5 getYdses_c5_lag1() {
		return ydses_c5_lag1;
	}

	public double getTmpHHYpnbihs_dv_asinh() {
		return tmpHHYpnbihs_dv_asinh;
	}

	public void setTmpHHYpnbihs_dv_asinh(double val) {
		tmpHHYpnbihs_dv_asinh = val;
	}

	public Dhhtp_c4 getDhhtp_c4() {
		return dhhtp_c4;
	}


	public Dhhtp_c4 getDhhtp_c4_lag1() {
		return dhhtp_c4_lag1;
	}

	public void setN_children_allAges_lag1(int n_children_allAges_lag1) { this.n_children_allAges_lag1 =  n_children_allAges_lag1; }

	public void setN_children_02_lag1(int n_children_02_lag1) { this.n_children_02_lag1 =  n_children_02_lag1; }

	public void setOccupancy(Occupancy occupancy) { this.occupancy =  occupancy; }

	public void setN_children_017(Integer n_children_017) { this.n_children_017 = n_children_017; }

	public void setD_children_2under(Indicator d_children_2under) { this.d_children_2under = d_children_2under; }

	public Long getIdHousehold() {
		return idHousehold;
	}

	public void setDhhtp_c4_lag1(Dhhtp_c4 dhhtp_c4_lag1) {
		this.dhhtp_c4_lag1 = dhhtp_c4_lag1;
	}

	public void setYdses_c5_lag1(Ydses_c5 ydses_c5_lag1) {
		this.ydses_c5_lag1 = ydses_c5_lag1;
	}

	public Double getYearlyChangeInLogEDI() {
		return yearlyChangeInLogEDI;
	}

	public boolean isDhh_owned() {
		return dhh_owned;
	}

	public void setDhh_owned(boolean dhh_owned) {
		this.dhh_owned = dhh_owned;
	}

	public ArrayList<Triple<Les_c7_covid, Double, Integer>> getCovid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale() {
		return covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale;
	}

	public ArrayList<Triple<Les_c7_covid, Double, Integer>> getCovid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale() {
		return covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale;
	}

	public double isDisposableIncomeMonthlyImputedFlag() {
		return disposableIncomeMonthlyImputedFlag? 1. : 0.;
	}

	// Uprate disposable income from level of prices in any given year to 2017, as utility function was estimated on 2017 data
	public double getDisposableIncomeMonthlyUpratedToBasePriceYear() {
		return disposableIncomeMonthly / Parameters.getTimeSeriesValue(model.getYear(), TimeSeriesVariable.Inflation);
	}

	public double getAdjustedDisposableIncomeMonthlyUpratedToBasePriceYear() {
		return disposableIncomeMonthly / Parameters.getTimeSeriesValue(model.getYear(), TimeSeriesVariable.Inflation);
	}

	public double getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear() {
		double cost = 0.0;
		if (Parameters.flagFormalChildcare)
			cost += childcareCostPerWeek;
		if (Parameters.flagSocialCare)
			cost += socialCareCostPerWeek;
		if (Math.abs(cost) > 0.01)
			cost *= Parameters.WEEKS_PER_MONTH / Parameters.getTimeSeriesValue(model.getYear(), TimeSeriesVariable.Inflation);
		return cost;
	}

	public boolean isDecreaseInYearlyEquivalisedDisposableIncome() {
		return (equivalisedDisposableIncomeYearly != null && equivalisedDisposableIncomeYearly_lag1 != null && equivalisedDisposableIncomeYearly < equivalisedDisposableIncomeYearly_lag1);
	}

	public void clearStates() {
		if (states!=null) states = null;
	}

	void setStates() {

		// reset states if necessary
		if (states!=null) clearStates();

		// populate states object
		if ( Parameters.grids == null) {
			throw new IllegalStateException("ERROR - attempt to fetch uninitialised grids attribute from Parameters object");
		}
		states = new States(this, Parameters.grids.getScale());
	}

	public Person getRefPersonForDecisions() {

		Person ref;
		if (Occupancy.Couple.equals(occupancy)) {
			// reference person defined as:
			//	woman if man is disabled (even if woman is also disabled)
			//	man if woman is disabled and man is not
			//	person with highest age if both retired
			//	person retired if one is retired
			//	student if one is student (and under maximum age threshold)
			//	person with highest full-time wage potential if neither is disabled and both not retired

			if (male==null) {
				throw new IllegalStateException("ERROR - benefit unit identified as couple, but missing male adult");
			} else if (female==null) {
				throw new IllegalStateException("ERROR - benefit unit identified as couple, but missing female adult");
			} else if (male.getDlltsd() == Indicator.True) {
				ref = female;
			} else if (female.getDlltsd() == Indicator.True) {
				ref = male;
			} else if (male.getLes_c4()==Les_c4.Retired && female.getLes_c4()==Les_c4.Retired) {
				if (male.getDag() >= female.getDag()) {
					ref = male;
				} else {
					ref = female;
				}
			} else if (male.getLes_c4()==Les_c4.Retired) {
				ref = male;
			} else if (female.getLes_c4()==Les_c4.Retired) {
				ref = female;
			} else if (male.getLes_c4()==Les_c4.Student && male.getDag()<=Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION && female.getLes_c4()!=Les_c4.Student) {
				ref = male;
			} else if (female.getLes_c4()==Les_c4.Student && female.getDag()<=Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION && male.getLes_c4()!=Les_c4.Student) {
				ref = female;
			} else {
				if (male.getFullTimeHourlyEarningsPotential() >= female.getFullTimeHourlyEarningsPotential()) {
					ref = male;
				} else {
					ref = female;
				}
			}
		} else {
			// reference person is assigned to sole adult

			if (male != null) {
				ref = male;
			} else if (female != null) {
				ref = female;
			} else {
				throw new IllegalStateException("ERROR - benefit unit missing adult");
			}
		}
		return ref;
	}

	public double getHealthValForBehaviour() {
		// health is ordered from poorest (low) to best (high)

		double health = 999.0;
		if (male != null) {
			if (male.getDhe() != null) {
				health = male.getDhe().getValue();
			}
		}
		if (female != null) {
			if ( female.getDhe() != null ) {
				if (female.getDhe().getValue() < health) health = female.getDhe().getValue();
			}
		}
		return health;
	}

	public double getRegionIndex() {

		return region.getDrgn1EUROMODvariable();
	}

	public int getChildrenByAge(int age) {
		int children = 0;
		if (age==0) {
			if (n_children_0 != null) children = n_children_0;
		} else if (age==1) {
			if (n_children_1 != null) children = n_children_1;
		} else if (age==2) {
			if (n_children_2 != null) children = n_children_2;
		} else if (age==3) {
			if (n_children_3 != null) children = n_children_3;
		} else if (age==4) {
			if (n_children_4 != null) children = n_children_4;
		} else if (age==5) {
			if (n_children_5 != null) children = n_children_5;
		} else if (age==6) {
			if (n_children_6 != null) children = n_children_6;
		} else if (age==7) {
			if (n_children_7 != null) children = n_children_7;
		} else if (age==8) {
			if (n_children_8 != null) children = n_children_8;
		} else if (age==9) {
			if (n_children_9 != null) children = n_children_9;
		} else if (age==10) {
			if (n_children_10 != null) children = n_children_10;
		} else if (age==11) {
			if (n_children_11 != null) children = n_children_11;
		} else if (age==12) {
			if (n_children_12 != null) children = n_children_12;
		} else if (age==13) {
			if (n_children_13 != null) children = n_children_13;
		} else if (age==14) {
			if (n_children_14 != null) children = n_children_14;
		} else if (age==15) {
			if (n_children_15 != null) children = n_children_15;
		} else if (age==16) {
			if (n_children_16 != null) children = n_children_16;
		} else if (age==17) {
			if (n_children_17 != null) children = n_children_17;
		}
		return children;
	}
	public void setN_children_byAge(int age, int number) {

		switch(age) {
			case (0):
				n_children_0 = number;
				break;
			case (1):
				n_children_1 = number;
				break;
			case (2):
				n_children_2 = number;
				break;
			case (3):
				n_children_3 = number;
				break;
			case (4):
				n_children_4 = number;
				break;
			case (5):
				n_children_5 = number;
				break;
			case (6):
				n_children_6 = number;
				break;
			case (7):
				n_children_7 = number;
				break;
			case (8):
				n_children_8 = number;
				break;
			case (9):
				n_children_9 = number;
				break;
			case (10):
				n_children_10 = number;
				break;
			case (11):
				n_children_11 = number;
				break;
			case (12):
				n_children_12 = number;
				break;
			case (13):
				n_children_13 = number;
				break;
			case (14):
				n_children_14 = number;
				break;
			case (15):
				n_children_15 = number;
				break;
			case (16):
				n_children_16 = number;
				break;
			case (17):
				n_children_17 = number;
				break;
		}
	}

	Set<Person> getPersonsInBU() {

		Set<Person> personsInBU = new HashSet<>();
		if ( male != null ) personsInBU.add(male);
		if ( female != null ) personsInBU.add(female);
		if ( children.size() != 0 ) personsInBU.addAll(children);
		return personsInBU;
	}

	public void updateNonLabourIncome() {

		if (Parameters.enableIntertemporalOptimisations) {

			updateRetirementPensions();
			setInvestmentIncomeAnnual();
		} else {

			if (male != null)
				male.updateNonLabourIncome();
			if (female != null)
				female.updateNonLabourIncome();
		}
	}

	private void updateRetirementPensions() {

		if ( Parameters.enableIntertemporalOptimisations ) {

			// check if need to update retirement status
			Person refPerson = getRefPersonForDecisions();
			boolean toRetire = refPerson.considerRetirement();
			if (toRetire && liquidWealth > 0.0) {
				pensionIncomeAnnual = liquidWealth * Parameters.SHARE_OF_WEALTH_TO_ANNUITISE_AT_RETIREMENT /
						Parameters.annuityRates.getAnnuityRate(occupancy, getYear()-refPerson.getDag(), getYear());
				liquidWealth *= (1.0 - Parameters.SHARE_OF_WEALTH_TO_ANNUITISE_AT_RETIREMENT);

				// upate person variables
				double val;
				if (Occupancy.Couple.equals(occupancy)) {
					val = asinh(pensionIncomeAnnual/12.0/2.0);
					male.setYpnoab(val);
					female.setYpnoab(val);
				} else if (Occupancy.Single_Male.equals(occupancy)) {
					val = asinh(pensionIncomeAnnual/12.0);
					male.setYpnoab(val);
				} else {
					val = asinh(pensionIncomeAnnual/12.0);
					female.setYpnoab(val);
				}
			} else {
				if (Occupancy.Couple.equals(occupancy)) {
					pensionIncomeAnnual = male.getPensionIncomeAnnual();
					pensionIncomeAnnual += female.getPensionIncomeAnnual();
				} else {
					pensionIncomeAnnual = refPerson.getPensionIncomeAnnual();
				}
			}
		}
	}

	public void setInvestmentIncomeAnnual() {

		if ( Parameters.enableIntertemporalOptimisations ) {

			if (liquidWealth == null) {
				throw new RuntimeException("attempt to identify investment income when wealth is null");
			}

			if (liquidWealth < 0) {

				double wageFactor = 0.0;
				if (male != null) wageFactor += 0.7 * male.getEarningsWeekly(DecisionParams.FULLTIME_HOURS_WEEKLY) * 52.0;
				if (female != null) wageFactor += 0.7 * female.getEarningsWeekly(DecisionParams.FULLTIME_HOURS_WEEKLY) * 52.0;
				double phi;
				if (wageFactor < 0.1) {
					phi = 1.0;
				} else {
					phi = - liquidWealth / wageFactor;
				}
				phi = Math.min(phi, 1.0);
				investmentIncomeAnnual = (Parameters.getTimeSeriesRate(model.getYear(), TimeVaryingRate.DebtCostLow)*(1.0-phi) +
						Parameters.getTimeSeriesRate(model.getYear(), TimeVaryingRate.DebtCostHigh)*phi) * liquidWealth;
			} else {

				investmentIncomeAnnual = Parameters.getTimeSeriesRate(model.getYear(), TimeVaryingRate.SavingReturns) * liquidWealth;
			}

			// update person level variables
			double val;
			if (Occupancy.Couple.equals(occupancy)) {
				val = asinh(investmentIncomeAnnual/12.0/2.0);
				male.setYpncp(val);
				female.setYpncp(val);
				val = asinh((investmentIncomeAnnual + pensionIncomeAnnual)/12.0/2.0);
				male.setYptciihs_dv(val);
				female.setYptciihs_dv(val);
			} else if (Occupancy.Single_Male.equals(occupancy)) {
				val = asinh(investmentIncomeAnnual/12.0);
				male.setYpncp(val);
				val = asinh((investmentIncomeAnnual + pensionIncomeAnnual)/12.0);
				male.setYptciihs_dv(val);
			} else {
				val = asinh(investmentIncomeAnnual/12.0);
				female.setYpncp(val);
				val = asinh((investmentIncomeAnnual + pensionIncomeAnnual)/12.0);
				female.setYptciihs_dv(val);
			}
		}
	}

	public double getInvestmentIncomeAnnual() {return investmentIncomeAnnual;}
	public double getPensionIncomeAnnual() {return pensionIncomeAnnual;}

	void updateNetLiquidWealth() {

		if ( Parameters.enableIntertemporalOptimisations ) {

			// project benefit unit consumption
			if ((getDisposableIncomeMonthly()==null) || (Double.isNaN(getDisposableIncomeMonthly()))) {
				throw new RuntimeException("Disposable income not defined.");
			}
			double nonDiscretionaryConsumptionPerYear = 0.0;
			if (Parameters.flagFormalChildcare) {
				nonDiscretionaryConsumptionPerYear += getChildcareCostPerWeek() * Parameters.WEEKS_PER_YEAR;
			}
			if (Parameters.flagSocialCare) {
				nonDiscretionaryConsumptionPerYear += getSocialCareCostPerWeek() * Parameters.WEEKS_PER_YEAR;
			}

			double cashOnHand = Math.max(liquidWealth, DecisionParams.MIN_LIQUID_WEALTH) + getDisposableIncomeMonthly()*12.0 +
					states.getAvailableCredit() - nonDiscretionaryConsumptionPerYear;
			if (Double.isNaN(cashOnHand)) {
				throw new RuntimeException("Problem identifying cash on hand");
			}
			if (cashOnHand < 1.0E-5) {
				// allow for simulated debt exceeding assumed limit for behavioural solutions
				discretionaryConsumptionPerYear = DecisionParams.MIN_CONSUMPTION_PER_YEAR;
			} else {
				discretionaryConsumptionPerYear = Parameters.grids.consumption.interpolateAll(states, false);
				discretionaryConsumptionPerYear *= cashOnHand;
			}
			if ( Double.isNaN(discretionaryConsumptionPerYear) ) {
				throw new RuntimeException("annual discretionary consumption not defined (1)");
			}

			// apply accounting identity
			double liquidWealth_lag1 = liquidWealth;
			liquidWealth = liquidWealth_lag1 + getDisposableIncomeMonthly() * 12.0 - discretionaryConsumptionPerYear - nonDiscretionaryConsumptionPerYear;
		}
	}

	public double getDiscretionaryConsumptionPerYear() {
		return getDiscretionaryConsumptionPerYear(true);
	}
	public double getDiscretionaryConsumptionPerYear(boolean throwError) {
		if (discretionaryConsumptionPerYear ==null) {
			if (throwError) {
				throw new RuntimeException("annual consumption not defined (2)");
			} else {
				return 0.0;
			}
		} else {
			return discretionaryConsumptionPerYear;
		}
	}

	public double getEquivalisedWeight() {
		return equivalisedWeight;
	}

	public double asinh(double xx) {
		return Math.log(xx + Math.sqrt(xx * xx + 1.0));
	}

	private void updateChildcareCostPerWeek() {

		childcareCostPerWeek = 0.0;
		if (hasChildrenEligibleForCare()) {

			double prob = Parameters.getRegChildcareC1a().getProbability(this, Regressors.class);
			if (childCareInnov.nextDouble() < prob) {
				double score = Parameters.getRegChildcareC1b().getScore(this, Regressors.class);
				double rmse = Parameters.getRMSEForRegression("C1b");
				double gauss = childCareInnov.nextGaussian();
				childcareCostPerWeek = Math.exp(score + rmse * gauss);
//				if (model.getYear() > ) {
//					childcareCostPerWeek *= Parameters.getYearUpratingIndex(model.getYear(), ???);
//				}
				double costCap = childCareCostCapWeekly();
				if (costCap > 0.0 && costCap < getChildcareCostPerWeek()) {
					childcareCostPerWeek = costCap;
				}
			}
		}
	}

	private void updateSocialCareCostPerWeek() {

		socialCareCostPerWeek = 0.0;
		for (Person person : getPersonsInBU()) {
			socialCareCostPerWeek += person.getSocialCareCostWeekly();
		}
	}

	private Education getHighestDehC3() {
		Education max = Education.Low;
		if(male != null || female != null) {

			if (male != null) max = male.getDeh_c3();
			if (female != null) {

				if (Education.High.equals(female.getDeh_c3())) {
					max = Education.High;
				} else if (Education.Medium.equals(female.getDeh_c3()) && !(max == Education.High)) {
					max = Education.Medium;
				}
			}
		} else {
			if (deh_c3Local == null) {
				throw new RuntimeException("reference to uninitialised education status");
			}
			max = deh_c3Local;
		}
		return max;
	}

	private int getMinWeeklyHoursWorked() {

		Integer val = null;
		if (male != null || female != null) {

			if (male != null) {
				val = male.getLabourSupplyHoursWeekly();
			}
			if (female != null) {
				if (val != null) {
					val = Math.min(val, female.getLabourSupplyHoursWeekly());
				} else {
					val = female.getLabourSupplyHoursWeekly();
				}
			}
		} else {

			if (labourHoursWeekly1Local == null) {
				throw new RuntimeException("reference to uninitialised labourHoursWeekly attribute of benefitUnit");
			} else {
				val = labourHoursWeekly1Local;
			}
			if (labourHoursWeekly2Local != null) {
				if (labourHoursWeekly2Local < val) val = labourHoursWeekly2Local;
			}
		}
		return val;
	}

	private int getMaxWeeklyHoursWorked() {

		Integer val = null;
		if (male != null || female != null) {

			if (male != null) {
				val = male.getLabourSupplyHoursWeekly();
			}
			if (female != null) {
				if (val != null) {
					val = Math.max(val, female.getLabourSupplyHoursWeekly());
				} else {
					val = female.getLabourSupplyHoursWeekly();
				}
			}
		} else {

			if (labourHoursWeekly1Local == null) {
				throw new RuntimeException("reference to uninitialised labourHoursWeekly attribute of benefitUnit");
			} else {
				val = labourHoursWeekly1Local;
			}
			if (labourHoursWeekly2Local != null) {
				if (labourHoursWeekly2Local > val) val = labourHoursWeekly2Local;
			}
		}
		return val;
	}

	private boolean hasChildrenEligibleForCare() {

		for(Person child: children) {

			if (child.getDag() <= Parameters.MAX_CHILD_AGE_FOR_FORMAL_CARE) {
				return true;
			}
		}
		return false;
	}

	private double childCareCostCapWeekly() {
		double cap = -1.0;
		if (male != null) {
			if (male.getLabourSupplyHoursWeekly() > 0) {
				cap = male.getEarningsWeekly();
			}
		}
		if (female != null) {
			if (female.getLabourSupplyHoursWeekly() > 0) {
				cap = Math.max(0.0, cap) + female.getEarningsWeekly();
			}
		}
		if (cap > 0.0) {
			cap = cap * Parameters.CHILDCARE_COST_EARNINGS_CAP;
		}
		return cap;
	}
	public void setYearLocal(Integer year) {
		yearLocal = year;
	}
	public int getYear() {
		if (model != null) {
			return model.getYear();
		} else {
			if (yearLocal == null) {
				throw new RuntimeException("call to get uninitialised year in benefit unit");
			}
			return yearLocal;
		}
	}
	public void setDeh_c3Local(Education edu) {
		deh_c3Local = edu;
	}
	public void setLabourHoursWeekly1Local(Integer hours) {
		labourHoursWeekly1Local = hours;
	}
	public void setLabourHoursWeekly2Local(Integer hours) {
		labourHoursWeekly2Local = hours;
	}
}
