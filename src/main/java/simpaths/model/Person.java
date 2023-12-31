package simpaths.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.random.RandomGenerator;

import jakarta.persistence.*;

import simpaths.data.ManagerRegressions;
import simpaths.data.RegressionNames;
import simpaths.model.enums.*;
import microsim.statistics.Series;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.log4j.Logger;

import simpaths.data.Parameters;
import simpaths.model.decisions.DecisionParams;
import microsim.agent.Weight;
import microsim.data.db.PanelEntityKey;
import microsim.engine.SimulationEngine;
import microsim.event.EventListener;
import microsim.statistics.IDoubleSource;
import microsim.statistics.IIntSource;

@Entity
public class Person implements EventListener, IDoubleSource, IIntSource, Weight, Comparable<Person>
{
//	@Column(name="idperson")
//	public long idPerson;

    @Transient
    private static Logger log = Logger.getLogger(Person.class);

    //Italy and UK EU-SILC data has max id number of 270150003
    //EUROMOD data has max id around 2,700,000 for Italy and just over 2 million for UK
    @Transient
    public static long personIdCounter = 1;			//Could perhaps initialise this to one above the max key number in initial population, in the same way that we pull the max Age information from the input files.
    @Transient
    private final SimPathsModel model;
    @EmbeddedId
    private final PanelEntityKey key;
    private int dag; //Age
    @Column(name="flag_dies")
    private Boolean flagDies;
    @Column(name="flag_immigrate")
    private Boolean flagImmigrate;  //entry to sample via international immigration
    @Column(name="flag_emigrate")
    private Boolean flagEmigrate;   //exit sample via international emigration
    @Column(name="flag_align_entry")
    private Boolean flagAlignEntry; //entry to sample via population alignment
    @Column(name="flag_align_exit")
    private Boolean flagAlignExit;  //exit sample via population aligment
    @Transient
    private boolean ioFlag;         // true if a dummy person instantiated for IO decision solution
    @Transient
    private int dag_sq;             //Age squared
    @Enumerated(EnumType.STRING)
    private Gender dgn;             // gender
    @Enumerated(EnumType.STRING)
    private Education deh_c3;       //Education level
    @Enumerated(EnumType.STRING)
    @Transient
    private Education deh_c3_lag1;  //Lag(1) of education level
    @Enumerated(EnumType.STRING)
    private Education dehm_c3;      //Mother's education level
    @Enumerated(EnumType.STRING)
    private Education dehf_c3;      //Father's education level
    @Enumerated(EnumType.STRING)
    private Education dehsp_c3;     //Partner's education
    @Enumerated(EnumType.STRING)
    @Transient
    private Education dehsp_c3_lag1; //Lag(1) of partner's education
    @Enumerated(EnumType.STRING)
    private Indicator ded;          // in continuous education
    @Enumerated(EnumType.STRING)
    private Indicator der;          // return to education
    @Enumerated(EnumType.STRING)
    private Household_status household_status;
    @Transient
    private Household_status household_status_lag;		//Lag(1) of household_status
    @Enumerated(EnumType.STRING)
    private Les_c4 les_c4;      //Activity (employment) status
    @Enumerated(EnumType.STRING)
    private Les_c7_covid les_c7_covid; //Activity (employment) status used in the Covid-19 models
    @Transient
    private Les_c4 les_c4_lag1;		//Lag(1) of activity_status
    @Transient
    private Les_c7_covid les_c7_covid_lag1;     //Lag(1) of 7-category activity status
    @Enumerated(EnumType.STRING)
    private Les_c4 lessp_c4;
    @Transient
    private Les_c4 activity_status_partner_lag; //Lag(1) of partner activity status
    @Enumerated(EnumType.STRING)
    private Lesdf_c4 lesdf_c4;                  //Own and partner's activity status
    @Transient
    @Enumerated(EnumType.STRING)
    private Lesdf_c4 lesdf_c4_lag1;             //Lag(1) of own and partner's activity status
    @Transient
    private Integer liwwh = 0;                  //Work history in months (number of months in employment) (Note: this is monthly in EM, but simulation updates annually so increment by 12 months).
    @Enumerated(EnumType.STRING)
    private Dcpst dcpst;                        // partnership status
    @Transient
    @Enumerated(EnumType.STRING)
    private Dcpst dcpst_lag1;   // lag partnership status
    @Enumerated(EnumType.STRING)
    private Indicator dcpen;    // enter partnership
    @Enumerated(EnumType.STRING)
    private Indicator dcpex;    // exit partnership
    @Enumerated(EnumType.STRING)
    private Indicator dlltsd;	//Long-term sick or disabled if = 1
    @Enumerated(EnumType.STRING)
    @Transient
    private Indicator dlltsd_lag1; //Lag(1) of long-term sick or disabled
    @Enumerated(EnumType.STRING)
    @Column(name="need_socare")
    private Indicator needSocialCare = Indicator.False;
    @Column(name="formal_socare_hrs")
    private Double careHoursFromFormalWeekly = 0.0;
    @Column(name="formal_socare_cost")      // cost of formal care gross of public subsidies
    private Double careFormalCostWeekly = 0.0;
    @Column(name="partner_socare_hrs")
    private Double careHoursFromPartnerWeekly = 0.0;
    @Column(name="daughter_socare_hrs")
    private Double careHoursFromDaughterWeekly = 0.0;
    @Column(name="son_socare_hrs")
    private Double careHoursFromSonWeekly = 0.0;
    @Column(name="other_socare_hrs")
    private Double careHoursFromOtherWeekly = 0.0;
    @Transient
    SocialCareMarketAll socialCareMarketAll = SocialCareMarketAll.None;
    @Transient
    boolean careFromFormal;
    @Transient
    boolean careFromPartner;
    @Transient
    boolean careFromDaughter;
    @Transient
    boolean careFromSon;
    @Transient
    boolean careFromOther;
    @Enumerated(EnumType.STRING)
    @Transient
    private Indicator needSocialCare_lag1;
    @Transient
    private Double careHoursFromFormalWeekly_lag1;
    @Transient
    private Double careHoursFromPartnerWeekly_lag1;
    @Transient
    private Double careHoursFromDaughterWeekly_lag1;
    @Transient
    private Double careHoursFromSonWeekly_lag1;
    @Transient
    private Double careHoursFromOtherWeekly_lag1;

    //Sedex is an indicator for leaving education in that year
    @Enumerated(EnumType.STRING)
    private Indicator sedex;    // year left education
    @Enumerated(EnumType.STRING)
    private Indicator partnership_samesex;
    @Enumerated(EnumType.STRING)
    private Indicator women_fertility;
    @Enumerated(EnumType.STRING)
    private Indicator education_inrange;

//	@Transient
//	private double deviationFromMeanRetirementAge;				//Set on initialisation?

    @Enumerated(EnumType.STRING)
    private Indicator adultchildflag;
    @Transient
    private boolean toGiveBirth;
    @Transient
    private boolean toLeaveSchool;
    @Transient
    private boolean toBePartnered;
    @Transient
    private Person partner;
    @Column(name="idpartner")
    private Long idPartner;		//Note, must not use primitive long, as long cannot hold 'null' value, i.e. if the person has no partner
    @Transient
    private Long idPartnerLag1;

    // EUROMOD's DWT variable - demographic weight applies to the household, rather than the individual.
    // So initialize the person weights with the household weights and let evolve separately from
    // the evolving benefitUnit and household weights (conditional on the update rules applied to benefitUnit
    // and household weights, which may evolve based on the personal weights, e.g. taking the average of the
    // weights of the unit members.
    // Note that personal weight of newborn child is set equal that of the mother.
    @Column(name="person_weight")
    private double weight;
    @Column(name="idmother")
    private Long idMother;
    @Column(name="idfather")
    private Long idFather;
    @Transient
    private BenefitUnit benefitUnit;
	@Column(name="dhm_ghq")
	private boolean dhm_ghq; //Psychological distress case-based
	@Transient
	private boolean dhm_ghq_lag1;
	@Transient
	private Dhe dhe_lag1;
    @Enumerated(EnumType.STRING)
	private Dhe dhesp;
	@Transient
	private Dhe dhesp_lag1;
    @Column(name=Parameters.BENEFIT_UNIT_VARIABLE_NAME)
    private Long idBenefitUnit;
    @Column(name="idhh")
    @Transient
    private Long idHousehold;
    @Enumerated(EnumType.STRING)
    private Dhe dhe;
    @Column(name="dhm")
    private Double dhm; //Psychological distress GHQ-12 Likert scale
    @Transient
    private Double dhm_lag1; //Lag(1) of dhm
    @Column(name="dhh_owned")
    private boolean dhh_owned; // Person is a homeowner, true / false
    @Transient
    private boolean receivesBenefitsFlag; // Does person receive benefits
    @Transient
    private boolean receivesBenefitsFlag_L1; // Lag(1) of whether person receives benefits

//	@Column(name="unit_labour_cost")	// Initialised with value: (ils_earns + ils_sicer) / (4.34 * lhw), where lhw is the weekly hours a person worked in EUROMOD input data, and ils_sicer is the monthly employer social insurance contributions
//	private double unitLabourCost;		//Hourly labour cost.  Updated as potentialHourlyEarnings + donorHouse.ils

    @Column(name="labour_supply_weekly")
    private Labour labourSupplyWeekly;			//Number of hours of labour supplied each week
    @Transient
    private Labour labourSupplyWeekly_L1; // Lag(1) (previous year's value) of weekly labour supply
    @Column(name="hours_worked_weekly")
    private Integer hoursWorkedWeekly;			//Only for initialisation of labourSupplyWeekly (for aggregate supply / demand / total cost statistics at the start of the simulation). This is set to null after initialization.

//	Potential earnings is the gross hourly wage an individual can earn while working
//	and is estimated, for each individual, on the basis of observable characteristics as
//	age, education, civil status, number of children, etc. Hence, potential earnings
//	is a separate process in the simulation, and it is computed for every adult
//	individual in the simulated population, in each simulated period.
    @Column(name="potential_earnings_hourly")
    private double fullTimeHourlyEarningsPotential;		//Is hourly rate.  Initialised with value: ils_earns / (4.34 * lhw), where lhw is the weekly hours a person worked in EUROMOD input data
    @Column(name="l1_potential_earnings_hourly")
    private Double L1_fullTimeHourlyEarningsPotential; // Lag(1) of potentialHourlyEarnings
    @Transient
    private Series.Double yearlyEquivalisedDisposableIncomeSeries;
    @Column(name="equivalised_consumption_yearly")
    private Double yearlyEquivalisedConsumption;
    @Transient
    private Series.Double yearlyEquivalisedConsumptionSeries;
    @Column(name="s_index") //Alternatively could be kept in a Series, which would allow access to previous values. But more efficient to persist a single value to CSV / database
    private Double sIndex = Double.NaN;
    @Column(name="s_index_normalised")
    private Double sIndexNormalised = Double.NaN;
    @Transient
    private LinkedHashMap<Integer, Double> sIndexYearMap;
    @Column(name="dcpyy")
    private Integer dcpyy; //Number of years in partnership
    @Transient
    private Integer dcpyy_lag1; //Lag(1) of number of years in partnership
    @Column(name="dcpagdf")
    private Integer dcpagdf; //Difference between ages of partners in union (note: this allows negative values and is the difference between own age and partner's age)
    @Transient
    private Integer dcpagdf_lag1; //Lag(1) of difference between ages of partners in union
    @Column(name="ypnbihs_dv")
    private Double ypnbihs_dv; //Gross personal non-benefit income per month
    @Transient
    private Double ypnbihs_dv_lag1 = 0.; //Lag(1) of gross personal non-benefit income
    @Column(name="yptciihs_dv")
    private double yptciihs_dv; // inverse hyperbolic sine of non-employment private income per month (capital and pension)
    @Column(name="ypncp")
    private double ypncp; // inverse hyperbolic sine of capital income per month
    @Column(name="ypnoab")
    private double ypnoab; // inverse hyperbolic sine of pension income per month
    @Transient
    private double ypncp_lag1; //Lag(1) of ypncp
    @Transient
    private double ypncp_lag2; //Lag(2) of capital income
    @Transient
    private double ypnoab_lag1; //Lag(1) of pension income
    @Transient
    private double ypnoab_lag2; //Lag(2) of pension income
    @Transient
    private double yptciihs_dv_lag1; //Lag(1) of gross personal non-benefit non-employment income
    @Transient
    private double yptciihs_dv_lag2; //Lag(2) of gross personal non-benefit non-employment income
    @Transient
    private double yptciihs_dv_lag3; //Lag(3) of gross personal non-benefit non-employment income
    @Column(name="yplgrs_dv")
    private double yplgrs_dv; //Gross personal employment income
    @Transient
    private double yplgrs_dv_lag1; //Lag(1) of gross personal employment income
    @Transient
    private double yplgrs_dv_lag2; //Lag(2) of gross personal employment income
    @Transient
    private double yplgrs_dv_lag3; //Lag(3) of gross personal employment income
    @Column(name="ynbcpdf_dv")
    private Double ynbcpdf_dv; //Difference between own and partner's gross personal non-benefit income per month
    @Transient
    private Double ynbcpdf_dv_lag1; //Lag(1) of difference between own and partner's gross personal non-benefit income

    //For matching process
    @Transient
    private double desiredAgeDiff;
    @Transient
    private double desiredEarningsPotentialDiff;
    @Column(name="original_id_person")
    private Long id_original;
    @Transient
    private Long id_bu_original;
    @Transient
    private Long id_hh_original;
    @Transient
    private Person originalPartner;
    @Transient
    private int ageGroup;

//	private int personType;

    @Transient
    private boolean clonedFlag;
    public boolean isBornInSimulation() {
        return bornInSimulation;
    }
    public void setBornInSimulation(boolean bornInSimulation) {
        this.bornInSimulation = bornInSimulation;
    }
    @Transient
    private boolean bornInSimulation; //Flag to keep track of newborns

    //This is set to true at the point when individual leaves education and never reset. So if true, individual has not always been in continuous education.
    @Transient
    private boolean leftEducation;

    //This is set to true at the point when individual leaves partnership and never reset. So if true, individual has been / is in a partnership
    @Transient
    private boolean leftPartnership;
    @Transient
    private int originalNumberChildren;
    @Transient
    private Household_status originalHHStatus;
    @Transient
    private Integer newWorkHours_lag1; // Define a variable to keep previous month's value of work hours to be used in the Covid-19 module
    @Transient
    private double covidModuleGrossLabourIncome_lag1;
    @Transient
    private Indicator covidModuleReceivesSEISS = Indicator.False;
    @Transient
    private double covidModuleGrossLabourIncome_Baseline;
    @Column(name = "covidModuleBaselinePayXt5")
    private Quintiles covidModuleGrossLabourIncomeBaseline_Xt5;
    @Transient
    private Double wageRegressionRandomComponentE;
    @Transient
    private Double wageRegressionRandomComponentNE;
    @Transient
    private Integer yearLocal;
    @Transient
    private Region regionLocal;
    @Transient
    private Dhhtp_c4 dhhtp_c4_lag1Local;
    @Transient
    private Ydses_c5 ydses_c5_lag1Local;
    @Transient
    Integer n_children_allAges_lag1Local;
    @Transient
    Integer n_children_allAges_Local;
    @Transient
    Integer n_children_02_lag1Local;
    @Transient
    private Integer n_children_017Local;
    @Transient
    private Indicator d_children_2underLocal;
    @Transient
    RandomGenerator healthInnov;
    @Transient
    RandomGenerator socialCareInnov;
    @Transient
    RandomGenerator wagesInnov;
    @Transient
    RandomGenerator capitalInnov;
    @Transient
    RandomGenerator resStanDevInnov;
    @Transient
    RandomGenerator housingInnov;
    @Transient
    RandomGenerator labourInnov;
    @Transient
    RandomGenerator cohabitInnov;
    @Transient
    RandomGenerator fertilityInnov;
    @Transient
    RandomGenerator educationInnov;
    @Transient
    RandomGenerator labourSupplyInnov;
    @Transient
    double labourSupplySingleDraw;
    @Transient
    private Map<Labour, Integer> personContinuousHoursLabourSupplyMap = new EnumMap<>(Labour.class);

    //TODO: Remove when no longer needed.  Used to calculate mean score of employment selection regression.
    public static double scoreMale;
    public static double scoreFemale;
    public static double countMale;
    public static double countFemale;
    public static double inverseMillsRatioMaxMale = Double.MIN_VALUE;
    public static double inverseMillsRatioMinMale = Double.MAX_VALUE;
    public static double inverseMillsRatioMaxFemale = Double.MIN_VALUE;
    public static double inverseMillsRatioMinFemale = Double.MAX_VALUE;


    // ---------------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------------
    public Person() {
        super();
        model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        key = new PanelEntityKey();
    }

    public Person(boolean regressionModel) {
        this.model = null;
        key = null;
    }

    public Person(Long id) {
        super();
        key = new PanelEntityKey(id);
        model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        clonedFlag = false;

        healthInnov = new Random(SimulationEngine.getRnd().nextLong());
        socialCareInnov = new Random(SimulationEngine.getRnd().nextLong());
        wagesInnov = new Random(SimulationEngine.getRnd().nextLong());
        capitalInnov = new Random(SimulationEngine.getRnd().nextLong());
        resStanDevInnov = new Random(SimulationEngine.getRnd().nextLong());
        housingInnov = new Random(SimulationEngine.getRnd().nextLong());
        labourInnov = new Random(SimulationEngine.getRnd().nextLong());
        cohabitInnov = new Random(SimulationEngine.getRnd().nextLong());
        fertilityInnov = new Random(SimulationEngine.getRnd().nextLong());
        educationInnov = new Random(SimulationEngine.getRnd().nextLong());
        labourSupplyInnov = new Random(SimulationEngine.getRnd().nextLong());
        labourSupplySingleDraw = labourSupplyInnov.nextDouble();
    }

    //For use with creating new people at the minimum Age who enter the simulation during UpdateMaternityStatus after fertility has been aligned
    public Person(Gender gender, Person mother) {

        this(personIdCounter++);

        this.dgn = gender;
        this.idMother = mother.getKey().getId();
        this.idFather = mother.getPartner().getKey().getId();
        this.dehm_c3 = mother.getDeh_c3();
        this.dehf_c3 = mother.getPartner().getDeh_c3();
        this.dcpen = Indicator.False;
        this.dcpex = Indicator.False;
        this.dlltsd = Indicator.False;
        this.dlltsd_lag1 = Indicator.False;
        this.needSocialCare = Indicator.False;
        this.needSocialCare_lag1 = Indicator.False;
        this.careHoursFromFormalWeekly = -9.0;
        this.careFormalCostWeekly = -9.0;
        this.careHoursFromFormalWeekly_lag1 = -9.0;
        this.careHoursFromPartnerWeekly = -9.0;
        this.careHoursFromPartnerWeekly_lag1 = -9.0;
        this.careHoursFromDaughterWeekly = -9.0;
        this.careHoursFromDaughterWeekly_lag1 = -9.0;
        this.careHoursFromSonWeekly = -9.0;
        this.careHoursFromSonWeekly_lag1 = -9.0;
        this.careHoursFromOtherWeekly = -9.0;
        this.careHoursFromOtherWeekly_lag1 = -9.0;
        this.women_fertility = Indicator.False;
        this.idBenefitUnit = mother.getIdBenefitUnit();
        this.benefitUnit = mother.benefitUnit;
        this.benefitUnit.setHousehold(mother.getBenefitUnit().getHousehold());
        this.dag = 0;
        this.weight = mother.getWeight();			//Newborn has same weight as mother (the number of newborns will then be aligned in fertility alignment)
        this.dhe = Dhe.VeryGood;
        this.dhm = 9.;			//Set to median for under 18's as a placeholder
        this.dhe_lag1 = dhe;
        this.dhm_lag1 = dhm;
        this.deh_c3 = Education.Low;
        this.les_c4 = Les_c4.Student;				//Set lag activity status as Student, i.e. in education from birth
        this.leftEducation = false;
        this.les_c4_lag1 = les_c4;
        this.les_c7_covid = Les_c7_covid.Student;
        this.les_c7_covid_lag1 = les_c7_covid;
        this.household_status = Household_status.Parents;
        this.labourSupplyWeekly = Labour.ZERO;			//Will be updated in Labour Market Module when the person stops being a student
        this.labourSupplyWeekly_L1 = Labour.ZERO;
        this.hoursWorkedWeekly = getLabourSupplyWeekly().getHours(this);
        this.idHousehold = mother.getBenefitUnit().getIdHousehold();
//		setDeviationFromMeanRetirementAge();			//This would normally be done within initialisation, but the line above has been commented out for reasons given...
        yearlyEquivalisedDisposableIncomeSeries = new Series.Double(this, DoublesVariables.EquivalisedIncomeYearly);
        yearlyEquivalisedConsumptionSeries = new Series.Double(this, DoublesVariables.EquivalisedConsumptionYearly);
        yearlyEquivalisedConsumption = 0.;
        sIndexYearMap = new LinkedHashMap<Integer, Double>();
        this.bornInSimulation = true;
        this.dhh_owned = false;
        this.receivesBenefitsFlag = false;
        this.receivesBenefitsFlag_L1 = receivesBenefitsFlag;
        updateVariables(false);

    }

    //Below is a "copy constructor" for persons: it takes an original person as input, changes the ID, copies the rest of the person's properties, and creates a new person.
    public Person (Person originalPerson) {

        this(personIdCounter++);

        this.id_hh_original = originalPerson.idHousehold;
        this.id_bu_original = originalPerson.idBenefitUnit;
        this.id_original = originalPerson.key.getId();

        this.dag = originalPerson.dag;
        this.ageGroup = originalPerson.ageGroup;
        this.dgn = originalPerson.dgn;
        this.deh_c3 = originalPerson.deh_c3;

        if (originalPerson.deh_c3_lag1 != null) { //If original person misses lagged level of education, assign current level of education
            this.deh_c3_lag1 = originalPerson.deh_c3_lag1;
        } else {
            this.deh_c3_lag1 = deh_c3;
        }

        this.dehf_c3 = originalPerson.dehf_c3;
        this.dehm_c3 = originalPerson.dehm_c3;
        this.dehsp_c3 = originalPerson.dehsp_c3;
        this.dehsp_c3_lag1 = originalPerson.deh_c3_lag1;

        if (originalPerson.dag < Parameters.MIN_AGE_TO_LEAVE_EDUCATION) { //If under age to leave education, set flag for being in education to true
            this.ded = Indicator.True;
        } else {
            this.ded = originalPerson.ded;
        }

        this.der = originalPerson.der;
        this.dcpyy = originalPerson.dcpyy;
        this.dcpagdf = originalPerson.dcpagdf;
        this.dcpagdf_lag1 = originalPerson.dcpagdf_lag1;
        this.household_status = originalPerson.household_status;
        this.household_status_lag = originalPerson.household_status_lag;
        if (originalPerson.les_c4 != null) {
            this.les_c4 = originalPerson.les_c4;
        } else if (originalPerson.dag < Parameters.MIN_AGE_TO_LEAVE_EDUCATION) {
            this.les_c4 = Les_c4.Student;
        } else if (originalPerson.dag > Parameters.getFixedRetireAge(model.getYear(), originalPerson.getDgn())) {
            this.les_c4 = Les_c4.Retired;
        } else if (originalPerson.getLabourSupplyWeekly() != null && originalPerson.getLabourSupplyWeekly().getHours(originalPerson) > 0) {
            this.les_c4 = Les_c4.EmployedOrSelfEmployed;
        } else {
            this.les_c4 = Les_c4.NotEmployed;
        }

        if (originalPerson.les_c4_lag1 != null) { //If original persons misses lagged activity status, assign current activity status
            this.les_c4_lag1 = originalPerson.les_c4_lag1;
        } else {
            this.les_c4_lag1 = les_c4;
        }

        this.les_c7_covid = originalPerson.les_c7_covid;
        if (originalPerson.les_c7_covid_lag1 != null) { //If original persons misses lagged activity status, assign current activity status
            this.les_c7_covid_lag1 = originalPerson.les_c7_covid_lag1;
        } else {
            this.les_c7_covid_lag1 = les_c7_covid;
        }

        this.lesdf_c4 = originalPerson.lesdf_c4;
        this.lesdf_c4_lag1 = originalPerson.lesdf_c4_lag1;
        this.lessp_c4 = originalPerson.lessp_c4;
        this.activity_status_partner_lag = originalPerson.activity_status_partner_lag;
        this.dcpst = originalPerson.dcpst;
        this.dcpst_lag1 = originalPerson.dcpst_lag1;
        this.dcpen = originalPerson.dcpen;
        this.dcpex = originalPerson.dcpex;
        this.ypnbihs_dv = originalPerson.getYpnbihs_dv();
        this.ypnbihs_dv_lag1 = originalPerson.ypnbihs_dv_lag1;
        this.yptciihs_dv = originalPerson.getYptciihs_dv();
        this.yplgrs_dv = originalPerson.getYplgrs_dv();
        this.yplgrs_dv_lag1 = originalPerson.yplgrs_dv_lag1;
        this.yplgrs_dv_lag2 = originalPerson.yplgrs_dv_lag2;
        this.yplgrs_dv_lag3 = originalPerson.yplgrs_dv_lag3;
        this.ynbcpdf_dv = originalPerson.ynbcpdf_dv;
        this.ynbcpdf_dv_lag1 = originalPerson.ynbcpdf_dv_lag1;
        this.ypncp_lag2 = originalPerson.ypncp_lag2;
        this.ypncp_lag1 = originalPerson.ypncp_lag1;
        this.ypnoab_lag2 = originalPerson.ypnoab_lag2;
        this.ypnoab_lag1 = originalPerson.ypnoab_lag1;

        this.dlltsd = originalPerson.dlltsd;
        this.liwwh = originalPerson.liwwh;
        this.dlltsd_lag1 = originalPerson.dlltsd_lag1;
        this.needSocialCare = originalPerson.needSocialCare;
        this.needSocialCare_lag1 = originalPerson.needSocialCare_lag1;
        this.careHoursFromFormalWeekly = originalPerson.careHoursFromFormalWeekly;
        this.careFormalCostWeekly = originalPerson.careFormalCostWeekly;
        this.careHoursFromFormalWeekly_lag1 = originalPerson.careHoursFromFormalWeekly_lag1;
        this.careHoursFromPartnerWeekly = originalPerson.careHoursFromPartnerWeekly;
        this.careHoursFromPartnerWeekly_lag1 = originalPerson.careHoursFromPartnerWeekly_lag1;
        this.careHoursFromDaughterWeekly = originalPerson.careHoursFromDaughterWeekly;
        this.careHoursFromDaughterWeekly_lag1 = originalPerson.careHoursFromDaughterWeekly_lag1;
        this.careHoursFromSonWeekly = originalPerson.careHoursFromSonWeekly;
        this.careHoursFromSonWeekly_lag1 = originalPerson.careHoursFromSonWeekly_lag1;
        this.careHoursFromOtherWeekly = originalPerson.careHoursFromOtherWeekly;
        this.careHoursFromOtherWeekly_lag1 = originalPerson.careHoursFromOtherWeekly_lag1;
        this.sedex = originalPerson.sedex;
        this.partnership_samesex = originalPerson.partnership_samesex;
        this.women_fertility = originalPerson.women_fertility;
        this.education_inrange = originalPerson.education_inrange;
        this.toGiveBirth = originalPerson.toGiveBirth;
        this.toLeaveSchool = originalPerson.toLeaveSchool;
        this.partner = originalPerson.partner;
        this.idPartner = originalPerson.idPartner;
        this.idPartnerLag1 = originalPerson.idPartnerLag1;
        this.weight = originalPerson.weight;
        this.idMother = originalPerson.idMother;
        this.idFather = originalPerson.idFather;
        this.dhe = originalPerson.dhe;
        this.dhm = originalPerson.dhm;

        if (originalPerson.dhe_lag1 != null) { //If original person misses lagged level of health, assign current level of health as lagged value
            this.dhe_lag1 = originalPerson.dhe_lag1;
        } else {
            this.dhe_lag1 = originalPerson.dhe;
        }

        if (originalPerson.dhm_lag1 != null) {
            this.dhm_lag1 = originalPerson.dhm_lag1;
        } else {
            this.dhm_lag1 = originalPerson.dhm;
        }

		this.dhm_ghq = originalPerson.dhm_ghq;
		this.dhm_ghq_lag1 = originalPerson.dhm_ghq_lag1;

		if (originalPerson.labourSupplyWeekly_L1 != null) {
			this.labourSupplyWeekly_L1 = originalPerson.labourSupplyWeekly_L1;
		} else {
			this.labourSupplyWeekly_L1 = originalPerson.getLabourSupplyWeekly();
		}

        this.dhesp = originalPerson.dhesp; //Is it fine to assign here?
        this.dhesp_lag1 = originalPerson.dhesp_lag1;
        this.hoursWorkedWeekly = originalPerson.hoursWorkedWeekly;
        this.labourSupplyWeekly = originalPerson.getLabourSupplyWeekly();
        this.fullTimeHourlyEarningsPotential = Math.min(Parameters.MAX_HOURLY_WAGE_RATE, Math.max(Parameters.MIN_HOURLY_WAGE_RATE, originalPerson.fullTimeHourlyEarningsPotential));
        if (originalPerson.L1_fullTimeHourlyEarningsPotential != null) {
            this.L1_fullTimeHourlyEarningsPotential = Math.min(Parameters.MAX_HOURLY_WAGE_RATE, Math.max(Parameters.MIN_HOURLY_WAGE_RATE, originalPerson.L1_fullTimeHourlyEarningsPotential));
        } else {
            this.L1_fullTimeHourlyEarningsPotential = this.fullTimeHourlyEarningsPotential;
        }
        this.desiredAgeDiff = originalPerson.desiredAgeDiff;
        this.desiredEarningsPotentialDiff = originalPerson.desiredEarningsPotentialDiff;
        this.scoreMale = originalPerson.scoreMale;
        this.scoreFemale = originalPerson.scoreFemale;
        this.countMale = originalPerson.countMale;
        this.countFemale = originalPerson.countFemale;
        this.inverseMillsRatioMaxMale = originalPerson.inverseMillsRatioMaxMale;
        this.inverseMillsRatioMinMale  = originalPerson.inverseMillsRatioMinMale;
        this.inverseMillsRatioMaxFemale = originalPerson.inverseMillsRatioMaxFemale;
        this.inverseMillsRatioMinFemale = originalPerson.inverseMillsRatioMinFemale;

        this.adultchildflag = originalPerson.adultchildflag;
        yearlyEquivalisedDisposableIncomeSeries = new Series.Double(this, DoublesVariables.EquivalisedIncomeYearly);
        yearlyEquivalisedConsumptionSeries = new Series.Double(this, DoublesVariables.EquivalisedConsumptionYearly);
        yearlyEquivalisedConsumption = originalPerson.yearlyEquivalisedConsumption;
        sIndexYearMap = new LinkedHashMap<Integer, Double>();
        this.dhh_owned = originalPerson.dhh_owned;
        this.receivesBenefitsFlag = originalPerson.receivesBenefitsFlag;
        this.receivesBenefitsFlag_L1 = originalPerson.receivesBenefitsFlag_L1;
    }


    // ---------------------------------------------------------------------
    // Initialisation methods
    // ---------------------------------------------------------------------

    public void setAdditionalFieldsInInitialPopulation() {

        labourSupplyWeekly = Labour.convertHoursToLabour(model.getInitialHoursWorkedWeekly().get(key.getId()).intValue());
        receivesBenefitsFlag_L1 = receivesBenefitsFlag;
        labourSupplyWeekly_L1 = getLabourSupplyWeekly();

        if(UnionMatchingMethod.SBAM.equals(model.getUnionMatchingMethod())) {
            updateAgeGroup();
        }

        hoursWorkedWeekly = null;	//Not to be updated as labourSupplyWeekly contains this information.
        initializeHouseholdStatus();
        updateVariables(true);
    }

    //This method assign people to age groups used to define types in the SBAM matching procedure
    private void updateAgeGroup() {
        if(dag < 18) {
            ageGroup = 0;
            model.tmpPeopleAssigned++;
        } else if(dag >= 18 && dag < 21) {
            ageGroup = 1;
            model.tmpPeopleAssigned++;
        } else if(dag >= 21 && dag < 24) {
            ageGroup = 2;
            model.tmpPeopleAssigned++;
        } else if(dag >= 24 && dag < 27) {
            ageGroup = 3;
            model.tmpPeopleAssigned++;
        } else if(dag >= 27 && dag < 30) {
            ageGroup = 4;
            model.tmpPeopleAssigned++;
        } else if(dag >= 30 && dag < 33) {
            ageGroup = 5;
            model.tmpPeopleAssigned++;
        } else if(dag >= 33 && dag < 36) {
            ageGroup = 6;
            model.tmpPeopleAssigned++;
        } else if(dag >= 36 && dag < 40) {
            ageGroup = 7;
            model.tmpPeopleAssigned++;
        } else if(dag >= 40 && dag < 45) {
            ageGroup = 8;
            model.tmpPeopleAssigned++;
        } else if(dag >= 45 && dag < 55) {
            ageGroup = 9;
            model.tmpPeopleAssigned++;
        } else if(dag >= 55 && dag < 65) {
            ageGroup = 10;
            model.tmpPeopleAssigned++;
        } else if(dag >= 65) {
            ageGroup = 11;
            model.tmpPeopleAssigned++;
        } else {
            System.out.println("Could not assign age group!");
        }
    }

    private void initializeHouseholdStatus() {

        if(partner != null) {
            household_status = Household_status.Couple;
        }
        else if(idMother != null || idFather != null) {
            household_status = Household_status.Parents;
        }
        else household_status = Household_status.Single;

    }


    // ---------------------------------------------------------------------
    // Event Listener
    // ---------------------------------------------------------------------
    public enum Processes {
        Ageing,
        ProjectEquivConsumption,
        ConsiderCohabitation,
        ConsiderRetirement,
        GiveBirth,
        Health,
        SocialCareIncidence,
        HealthMentalHM1, 				//Predict level of mental health on the GHQ-12 Likert scale (Step 1)
        HealthMentalHM2,				//Modify the prediction from Step 1 by applying increments / decrements for exposure
        HealthMentalHM1HM2Cases,		//Case-based prediction for psychological distress, Steps 1 and 2 together
        InSchool,
        LeavingSchool,
        UpdatePotentialHourlyEarnings,	//Needed to union matching and labour supply
    }

    @Override
    public void onEvent(Enum<?> type) {
        switch ((Processes) type) {
        case Ageing:
//			log.debug("Ageing for person " + this.getKey().getId());
            ageing();
            break;
        case ProjectEquivConsumption:
            projectEquivConsumption();
            break;
        case ConsiderCohabitation:
//			log.debug("BenefitUnit Formation for person " + this.getKey().getId());
            considerCohabitation();
            break;
        case ConsiderRetirement:
            considerRetirement();
            break;
        case GiveBirth:
//			log.debug("Check whether to give birth for person " + this.getKey().getId());
            giveBirth();
            break;
        case Health:
//			log.debug("Health for person " + this.getKey().getId());
			health();
			break;
        case SocialCareIncidence:
            evaluateSocialCareDemand();
            evaluateSocialCareSupply();
            break;
		case HealthMentalHM1:
			healthMentalHM1Level();
			break;
		case HealthMentalHM2:
			healthMentalHM2Level();
			break;
		case HealthMentalHM1HM2Cases:
			healthMentalHM1HM2Cases();
			break;
		case InSchool:
//			log.debug("In Education for person " + this.getKey().getId());
            inSchool();
            break;
        case LeavingSchool:
            leavingSchool();
            break;
        case UpdatePotentialHourlyEarnings:
//			System.out.println("Update wage equation for person " + this.getKey().getId() + " with age " + age + " with activity_status " + activity_status + " and activity_status_lag " + activity_status_lag + " and toLeaveSchool " + toLeaveSchool + " with education " + education);
            updateFullTimeHourlyEarnings();
            break;
        }
    }


    // ---------------------------------------------------------------------
    // Processes
    // ---------------------------------------------------------------------

    private void ageing() {

        dag++;
        boolean flagDies = considerMortality();
        dag_sq = dag*dag;
        benefitUnit.clearStates(); // states object used to manage optimised decisions
        if (flagDies) {

            death();
        } else if (dag == Parameters.AGE_TO_BECOME_RESPONSIBLE) {

            setupNewBenefitUnit(true);
            considerLeavingHome();
        } else if (dag > Parameters.AGE_TO_BECOME_RESPONSIBLE && adultchildflag!=null && Indicator.True.equals(adultchildflag)) {

            considerLeavingHome();
        }

        //Update years in partnership (before the lagged value is updated)
        dcpyy_lag1 = dcpyy; //Update lag value outside of updateVariables() method
        if(partner != null) {

            if(Objects.equals(idPartner, idPartnerLag1)) {

                dcpyy++;
            } else dcpyy = 0;
        } else dcpyy = 0; //If no partner, set years in partnership to 0 TODO: or should this be set to null?

        //Update variables
        updateVariables(false);	//This also sets the lagged values
        updateAgeGroup();   //Update ageGroup as person ages
    }

    private boolean considerMortality() {

        boolean flagDies = false;
        if (model.getProjectMortality()) {

            if ( Occupancy.Couple.equals(benefitUnit.getOccupancy()) || benefitUnit.getSize() == 1 ) {
                // exclude single parents with dependent children from death

                double mortalityProbability = Parameters.getMortalityProbability(dgn, dag, model.getYear());
                if (model.getEngine().getRandom().nextDouble() < mortalityProbability) {
                    flagDies = true;
                }
            }
        }
        if (dag > Parameters.maxAge) flagDies = true;

        return flagDies;
    }

    //This process should be applied to those at the age to become responsible / leave home OR above if they have the adultChildFlag set to True (i.e. people can move out, but not move back in).
    private void considerLeavingHome() {

        //For those who are moving out, evaluate whether they should have stayed with parents and if yes, set the adultchildflag to true

        boolean toLeaveHome = (housingInnov.nextDouble() < Parameters.getRegLeaveHomeP1a().getProbability(this, Person.DoublesVariables.class)); //If true, should leave home
        if (Les_c4.Student.equals(les_c4)) {

            adultchildflag = Indicator.True; //Students not allowed to leave home to match filtering conditon
        } else {

            if (!toLeaveHome) { //If at the age to leave home but regression outcome is negative, person has adultchildflag set to true (although they still set up a new benefitUnit in the simulation, it's treated differently in the labour supply)

                adultchildflag = Indicator.True;
            } else {

                adultchildflag = Indicator.False;
                setupNewHousehold(true); //If person leaves home, they set up a new household
                //TODO: Household status and similar variables should be updated automatically. Here or elsewhere?
            }
        }
    }

    public boolean considerRetirement() {
        boolean toRetire = false;
        if (dag >= Parameters.MIN_AGE_TO_RETIRE && !Les_c4.Retired.equals(les_c4) && !Les_c4.Retired.equals(les_c4_lag1)) {
            if (Parameters.enableIntertemporalOptimisations) {
                if (Labour.ZERO.equals(getLabourSupplyWeekly())) {
                    toRetire = true;
                }
           } else {
                if (partner != null) { //Follow process R1b (couple) for retirement
                    toRetire = (labourInnov.nextDouble() < Parameters.getRegRetirementR1b().getProbability(this, Person.DoublesVariables.class));
                } else { //Follow process R1a (single) for retirement
                    toRetire = (labourInnov.nextDouble() < Parameters.getRegRetirementR1a().getProbability(this, Person.DoublesVariables.class));
                }
            }
            if (toRetire) {
                setLes_c4(Les_c4.Retired);
                labourSupplyWeekly = Labour.ZERO;
            }
        }
        return toRetire;
    }
    
	/*
	This method corresponds to Step 1 of the mental health evaluation: predict level of mental health on the GHQ-12 Likert scale based on observable characteristics
	 */
	protected void healthMentalHM1Level() {
		if (dag >= 16) {
			double score = Parameters.getRegHealthHM1Level().getScore(this, Person.DoublesVariables.class);
            double rmse = Parameters.getRMSEForRegression("HM1");
            double gauss = healthInnov.nextGaussian();
			dhm = constrainDhmEstimate(score + rmse*gauss);
		}
	}

	/*
	This method corresponds to Step 2 of the mental health evaluation: increment / decrement the outcome of Step 1 depending on exposures that individual experienced.
	Filtering: only applies to those with Age>=16 & Age<=64. Different estimates for males and females.
	 */
	protected void healthMentalHM2Level() {

		double dhmPrediction;
		if (dag >= 25 && dag <= 64) {
			if (Gender.Male.equals(getDgn())) {
				dhmPrediction = Parameters.getRegHealthHM2LevelMales().getScore(this, Person.DoublesVariables.class);
				dhm = constrainDhmEstimate(dhmPrediction+dhm);
			} else if (Gender.Female.equals(getDgn())) {
				dhmPrediction = Parameters.getRegHealthHM2LevelFemales().getScore(this, Person.DoublesVariables.class);
				dhm = constrainDhmEstimate(dhmPrediction+dhm);
			} else System.out.println("healthMentalHM2 method in Person class: Person has no gender!");
		}
	}

	/*
	Case-based measure of psychological distress, Steps 1 and 2 modelled together
 	*/
	protected void healthMentalHM1HM2Cases() {

		if (dag >= 16) {
			double tmp_step1_score = 0, tmp_step2_score = 0, tmp_total_score = 0, tmp_probability = 0;
			boolean tmp_outcome;

			tmp_step1_score = Parameters.getRegHealthHM1Case().getScore(this, Person.DoublesVariables.class); // Obtain score from Step 1 of case-based psychological distress model

			if (dag >= 25 && dag <= 64) {
				if (Gender.Male.equals(getDgn())) {
					tmp_step2_score = Parameters.getRegHealthHM2CaseMales().getScore(this, Person.DoublesVariables.class); // Obtain score from Step 2 of case-based psychological distress model
				} else if (Gender.Female.equals(getDgn())) {
					tmp_step2_score = Parameters.getRegHealthHM2CaseFemales().getScore(this, Person.DoublesVariables.class); // Obtain score from Step 2 of case-based psychological distress model
				} else System.out.println("healthMentalHM2 method in Person class: Person has no gender!");
			}

			//Put together: get total score, convert to probability, get event, set dummy
			// 1. Sum scores from Step 1 and 2. This produces the basic score modified by the effect of transitions modelled in Step 2.
			tmp_total_score = tmp_step1_score + tmp_step2_score;
			// 2. Convert to probability
			tmp_probability = 1.0 / (1.0 + Math.exp(-tmp_total_score));
			// 3. Get event outcome
			tmp_outcome = model.getEngine().getRandom().nextDouble() < tmp_probability;
			// 4. Set dhm_ghq dummy
			setDhm_ghq(tmp_outcome);
		}
	}

	/*
	Psychological distress on the GHQ-12 scale has no meaning outside of the original values between 0 and 36, but we model this variable on a continuous scale. If the predicted value is outside of this interval, limit it to fall within these values.
	 */
	protected Double constrainDhmEstimate(Double dhm) {
		if (dhm < 0.) {
			dhm = 0.;
		} else if (dhm > 36.) {
			dhm = 36.;
		}
		return dhm;
	}

	//Health process defines health using H1a or H1b process
	protected void health() {		

		if((dag >= 16 && dag <= 29) && Les_c4.Student.equals(les_c4) && leftEducation == false) {
            //If age is between 16 - 29 and individual has always been in education, follow process H1a:

            Map<Dhe,Double> probs = Parameters.getRegHealthH1a().getProbabilities(this, Person.DoublesVariables.class);
            dhe = ManagerRegressions.multiEvent(probs, healthInnov.nextDouble());
        } else if(dag >= 16) {

            Map<Dhe,Double> probs = Parameters.getRegHealthH1b().getProbabilities(this, Person.DoublesVariables.class);
            dhe = ManagerRegressions.multiEvent(probs, healthInnov.nextDouble());

            //If age is over 16 and individual is not in continuous education, also follow process H2b to calculate the probability of long-term sickness / disability:
            boolean becomeLTSickDisabled = (healthInnov.nextDouble() < Parameters.getRegHealthH2b().getProbability(this, Person.DoublesVariables.class));
            //TODO: Do we want to allow long-term sick or disabled to recover?
            if(becomeLTSickDisabled == true) {
                dlltsd = Indicator.True;
            } else if(becomeLTSickDisabled == false) {
                dlltsd = Indicator.False;
            }
        }
    }

    protected void evaluateSocialCareDemand() {

        needSocialCare = Indicator.False;
        socialCareMarketAll = SocialCareMarketAll.None;
        careHoursFromFormalWeekly = 0.0;
        careFormalCostWeekly = 0.0;
        careHoursFromPartnerWeekly = 0.0;
        careHoursFromDaughterWeekly = 0.0;
        careHoursFromSonWeekly = 0.0;
        careHoursFromOtherWeekly = 0.0;
        careFromFormal = false;
        careFromPartner = false;
        careFromDaughter = false;
        careFromSon = false;
        careFromOther = false;
        if (dag >= Parameters.MIN_AGE_FORMAL_SOCARE) {
            // need care only projected for 65 and over due to limitations of data used for parameterisation

            double rnd1 = socialCareInnov.nextDouble();
            double probNeedCare = Parameters.getRegNeedCareS2a().getProbability(this, Person.DoublesVariables.class);
            if (rnd1 < probNeedCare) {
                // need care
                needSocialCare = Indicator.True;
            }

            double probRecCare = Parameters.getRegReceiveCareS2b().getProbability(this, Person.DoublesVariables.class);
            if (rnd1 < probRecCare) {
                // receive care

                needSocialCare = Indicator.True;
                Map<SocialCareMarket,Double> probs1 = Parameters.getRegSocialCareMarketS2c().getProbabilites(this, Person.DoublesVariables.class, SocialCareMarket.class);
                SocialCareMarket socialCareMarket = ManagerRegressions.multiEvent(probs1, socialCareInnov.nextDouble());
                socialCareMarketAll = SocialCareMarketAll.getCode(socialCareMarket);
                if (!SocialCareMarket.Informal.equals(socialCareMarketAll))
                    careFromFormal = true;

                if (!SocialCareMarket.Formal.equals(socialCareMarketAll)) {
                    // some informal care received

                    if (partner!=null) {
                        // check if receive care from partner

                        double probPartnerCare = Parameters.getRegReceiveCarePartnerS2d().getProbability(this, Person.DoublesVariables.class);
                        if (socialCareInnov.nextDouble() < probPartnerCare) {
                            // receive care from partner - check for supplementary carers

                            careFromPartner = true;
                            Map<PartnerSupplementaryCarer,Double> probs2 = Parameters.getRegPartnerSupplementaryCareS2e().getProbabilites(this, Person.DoublesVariables.class, PartnerSupplementaryCarer.class);
                            PartnerSupplementaryCarer cc = ManagerRegressions.multiEvent(probs2, socialCareInnov.nextDouble());
                            if (PartnerSupplementaryCarer.Daughter.equals(cc))
                                careFromDaughter = true;
                            if (PartnerSupplementaryCarer.Son.equals(cc))
                                careFromSon = true;
                            if (PartnerSupplementaryCarer.Other.equals(cc))
                                careFromOther = true;
                        }
                    }
                    if (!careFromPartner) {
                        // no care from partner - identify who supplies informal care

                        Map<NotPartnerInformalCarer,Double> probs2 = Parameters.getRegNotPartnerInformalCareS2f().getProbabilites(this, Person.DoublesVariables.class, NotPartnerInformalCarer.class);
                        NotPartnerInformalCarer cc = ManagerRegressions.multiEvent(probs2, socialCareInnov.nextDouble());
                        if (NotPartnerInformalCarer.DaughterOnly.equals(cc) || NotPartnerInformalCarer.DaughterAndSon.equals(cc) || NotPartnerInformalCarer.DaughterAndOther.equals(cc))
                            careFromDaughter = true;
                        if (NotPartnerInformalCarer.SonOnly.equals(cc) || NotPartnerInformalCarer.DaughterAndSon.equals(cc) || NotPartnerInformalCarer.SonAndOther.equals(cc))
                            careFromSon = true;
                        if (NotPartnerInformalCarer.OtherOnly.equals(cc) || NotPartnerInformalCarer.SonAndOther.equals(cc) || NotPartnerInformalCarer.DaughterAndOther.equals(cc))
                            careFromOther = true;
                    }
                }
                if (careFromPartner) {
                    double score = Parameters.getRegPartnerCareHoursS2g().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2g");
                    careHoursFromPartnerWeekly = Math.min(150.0, Math.exp(score + rmse * socialCareInnov.nextGaussian()));
                }
                if (careFromDaughter) {
                    double score = Parameters.getRegDaughterCareHoursS2h().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2h");
                    careHoursFromDaughterWeekly = Math.min(150.0, Math.exp(score + rmse * socialCareInnov.nextGaussian()));
                }
                if (careFromSon) {
                    double score = Parameters.getRegSonCareHoursS2i().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2i");
                    careHoursFromSonWeekly = Math.min(150.0, Math.exp(score + rmse * socialCareInnov.nextGaussian()));
                }
                if (careFromOther) {
                    double score = Parameters.getRegOtherCareHoursS2j().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2j");
                    careHoursFromOtherWeekly = Math.min(150.0, Math.exp(score + rmse * socialCareInnov.nextGaussian()));
                }
                if (careFromFormal) {
                    double score = Parameters.getRegFormalCareHoursS2k().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2k");
                    careHoursFromFormalWeekly = Math.min(150.0, Math.exp(score + rmse * socialCareInnov.nextGaussian()));
                    careFormalCostWeekly = careHoursFromFormalWeekly * Parameters.getTimeSeriesValue(model.getYear(), TimeSeriesVariable.CarerWageRate);
                }
            }
            if (Indicator.False.equals(needSocialCare)) {
                if (careHoursFromFormalWeekly + careHoursFromPartnerWeekly + careHoursFromDaughterWeekly + careHoursFromSonWeekly + careHoursFromOtherWeekly > 0.0)
                    throw new RuntimeException("projected non-zero social care hours when no need of social care");
            }
        }
    }


    protected void evaluateSocialCareSupply() {

    }

    
    protected void inSchool() {

        //Min age to leave education set to 16 (from 18 previously) but note that age to leave home is 18.
        if(les_c4.equals(Les_c4.Retired) || dag < Parameters.MIN_AGE_TO_LEAVE_EDUCATION || dag > Parameters.MAX_AGE_TO_ENTER_EDUCATION) {		//Only apply module for persons who are old enough to consider leaving education, but not retired
            return;
        }

        //If age is between 16 - 29 and individual has always been in education, follow process E1a:
        else if(les_c4.equals(Les_c4.Student) && !leftEducation && dag >= Parameters.MIN_AGE_TO_LEAVE_EDUCATION) { //leftEducation is initialised to false and updated to true when individual leaves education (and never reset).
            if (dag <= Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION) {
                double toStayAtSchoolScore = Parameters.getRegEducationE1a().getScore(this, Person.DoublesVariables.class);
                toLeaveSchool = (labourInnov.nextDouble() >= Parameters.getRegEducationE1a().getProbability(this, Person.DoublesVariables.class)); //If event is true, stay in school.  If event is false, leave school.
            } else {
                toLeaveSchool = true; //Hasn't left education until 30 - force out
            }
        }

        //If age is between 16 - 45 and individual has not continuously been in education, follow process E1b:
        //Either individual is currently a student and has left education at some point in the past (so returned) or individual is not a student so has not been in continuous education:
        else if(dag <= 45 && (!les_c4.equals(Les_c4.Student) || leftEducation)) { //leftEducation is initialised to false and updated to true when individual leaves education for the first time (and never reset).
            //TODO: If regression outcome of process E1b is true, set activity status to student and der (return to education indicator) to true?
            if (labourInnov.nextDouble() < Parameters.getRegEducationE1b().getProbability(this, Person.DoublesVariables.class)) { //If event is true, re-enter education.  If event is false, leave school

//				System.out.println("Persid " + getKey().getId() + " Aged: " + dag + " With activity status: " + les_c4 + " Assigned to the E1b process");

                setLes_c4(Les_c4.Student);
                setDer(Indicator.True);
                setDed(Indicator.True);
                labourSupplyWeekly = Labour.ZERO; //Assume no part-time work while studying
                //TODO: Note that individuals re-entering education do not have a new level of education set. (They don't "leave school")
            }
            else if (les_c4.equals(Les_c4.Student)){ //If activity status is student but regression to be in education was evaluated to false, remove student status
                setLes_c4(Les_c4.NotEmployed);
                setDed(Indicator.False);
                toLeaveSchool = true; //Test what happens if people who returned to education leave again
            }
        }
        else if (dag > 45 && les_c4.equals(Les_c4.Student)) { //People above 45 shouldn't be in education, so if someone re-entered at 45 in previous step, force out
            setLes_c4(Les_c4.NotEmployed);
            setDed(Indicator.False);
        }



    }

    protected void leavingSchool() {
        if(toLeaveSchool) {
            setEducationLevel(); //If individual leaves school follow process E2a to assign level of education
            setSedex(Indicator.True); //Set variable left education (sedex) if leaving school
            setDed(Indicator.False); //Set variable in education (ded) to false if leaving school
            setLeftEducation(true); //This is not reset and indicates if individual has ever left school - used with health process
            setLes_c4(Les_c4.NotEmployed); //Set activity status to NotEmployed when leaving school to remove Student status
        }
    }


    protected void considerCohabitation() {

        toBePartnered = false;
        if (model.getCountry().equals(Country.UK)) {

            //Apply only to individuals above age defined in MIN_AGE_MARRIAGE
            if (dag >= Parameters.MIN_AGE_COHABITATION) {

                //If not in partnership, follow U1a or U1b to determine probability of entering partnership
                if (partner == null) {

                    //Follow process U1a for individuals aged MIN_AGE_MARRIAGE-29 who are in continuous education:
                    if (dag <= 29 && les_c4.equals(Les_c4.Student) && leftEducation == false) {

                        toBePartnered = (cohabitInnov.nextDouble() < Parameters.getRegPartnershipU1a().getProbability(this, Person.DoublesVariables.class));
                        if (toBePartnered) { //If true, look for a partner
                            model.getPersonsToMatch().get(dgn).get(getRegion()).add(this); //Will look for partner in model's unionMatching process
                        }
                    }
                    //Follow process U1b for individuals who are not in continuous education:
                    else if ((les_c4.equals(Les_c4.Student) && leftEducation == true) || !les_c4.equals(Les_c4.Student)) {

                        toBePartnered = (cohabitInnov.nextDouble() < Parameters.getRegPartnershipU1b().getProbability(this, Person.DoublesVariables.class));
                        if (toBePartnered) {
                            model.getPersonsToMatch().get(dgn).get(getRegion()).add(this);
                        }
                    }
                }

                //If in partnership, follow U2b to determine the probability of exiting partnership by female member of the couple not in education
                //Note: this implies a 0 probability of splitting for couples in which female is in continuous education
                else if (partner != null) {

                    if (dgn.equals(Gender.Female) && ((les_c4.equals(Les_c4.Student) && leftEducation == true) || !les_c4.equals(Les_c4.Student))) {

                        if (cohabitInnov.nextDouble() < Parameters.getRegPartnershipU2b().getProbability(this, Person.DoublesVariables.class)) { //If true, leave partner

                            leavePartner();
                        }
                    }
                }
            }
        }
        else if (model.getCountry().equals(Country.IT)) {

            if (dag >= Parameters.MIN_AGE_COHABITATION) {

                //If not in partnership, follow U1a or U1b to determine probability of entering partnership
                if (partner == null) {

                    //Follow process U1 for individuals who are not in continuous education:
                    if ((les_c4.equals(Les_c4.Student) && leftEducation == true) || !les_c4.equals(Les_c4.Student)) {

                        toBePartnered = (cohabitInnov.nextDouble() < Parameters.getRegPartnershipITU1().getProbability(this, Person.DoublesVariables.class));
                        if (toBePartnered) {

                            model.getPersonsToMatch().get(dgn).get(getRegion()).add(this);
                        }
                    }
                }

                //If in partnership, follow U2 to determine the probability of exiting partnership by female member of the couple not in education
                //Note: this implies a 0 probability of splitting for couples in which female is in continuous education
                else if (partner != null) {

                    if (dgn.equals(Gender.Female) && ((les_c4.equals(Les_c4.Student) && leftEducation == true) || !les_c4.equals(Les_c4.Student))) {

                        if (cohabitInnov.nextDouble() < Parameters.getRegPartnershipITU2().getProbability(this, Person.DoublesVariables.class)) { //If true, leave partner

                            leavePartner();
                        }
                    }
                }
            }
        }
    }


    private void giveBirth() {				//To be called once per year after fertility alignment

        if(toGiveBirth) {		//toGiveBirth is determined by fertility alignment (in the model class)

            Gender babyGender = (fertilityInnov.nextDouble() < Parameters.PROB_NEWBORN_IS_MALE) ? Gender.Male : Gender.Female;

            //Give birth to new person and add them to benefitUnit.
            Person child = new Person(babyGender, this);
            model.getPersons().add(child);
            benefitUnit.addChild(child);

            //Update maternity status
            benefitUnit.newBornUpdate();			//Update benefitUnit fields related to new born children
            toGiveBirth = false;						//Reset boolean for next year
        }
    }


    protected void initialisePotentialHourlyEarnings() {

        double logPotentialHourlyEarnings, score, rmse, gauss = wagesInnov.nextGaussian();
        if (dgn.equals(Gender.Male)) {
            score = Parameters.getRegWagesMales().getScore(this, Person.DoublesVariables.class);
            rmse = Parameters.getRMSEForRegression("Wages_Males");
        } else {
            score = Parameters.getRegWagesFemales().getScore(this, Person.DoublesVariables.class);
            rmse = Parameters.getRMSEForRegression("Wages_Females");
        }
        logPotentialHourlyEarnings = score + rmse * gauss;
        double upratedLevelPotentialHourlyEarnings = Math.exp(logPotentialHourlyEarnings);
        setFullTimeHourlyEarningsPotential(upratedLevelPotentialHourlyEarnings);
        setL1_fullTimeHourlyEarningsPotential(upratedLevelPotentialHourlyEarnings);
    }


    protected void updateFullTimeHourlyEarnings() {

        double rmse;
        if (les_c4_lag1.equals(Les_c4.EmployedOrSelfEmployed)) {
            if (wageRegressionRandomComponentE == null || !model.fixRegressionStochasticComponent) {
                if (Gender.Male.equals(dgn)) {
                    rmse = Parameters.getRMSEForRegression("Wages_MalesE");
                } else {
                    rmse = Parameters.getRMSEForRegression("Wages_FemalesE");
                }
                wageRegressionRandomComponentE = rmse * wagesInnov.nextGaussian();
            }
        } else {
            if (wageRegressionRandomComponentNE == null || !model.fixRegressionStochasticComponent) {
                if (Gender.Male.equals(dgn)) {
                    rmse = Parameters.getRMSEForRegression("Wages_MalesNE");
                } else {
                    rmse = Parameters.getRMSEForRegression("Wages_FemalesNE");
                }
                wageRegressionRandomComponentNE = rmse * wagesInnov.nextGaussian();
            }
        }

        double logFullTimeHourlyEarnings;
        if(Gender.Male.equals(dgn)) {
            if (Les_c4.EmployedOrSelfEmployed.equals(les_c4_lag1)) {
                logFullTimeHourlyEarnings = Parameters.getRegWagesMalesE().getScore(this, Person.DoublesVariables.class) + wageRegressionRandomComponentE;
            }
            else {
                logFullTimeHourlyEarnings = Parameters.getRegWagesMalesNE().getScore(this, Person.DoublesVariables.class) + wageRegressionRandomComponentNE;
            }
        } else {
            if (Les_c4.EmployedOrSelfEmployed.equals(les_c4_lag1)) {
                logFullTimeHourlyEarnings = Parameters.getRegWagesFemalesE().getScore(this, Person.DoublesVariables.class) + wageRegressionRandomComponentE;
            }
            else {
                logFullTimeHourlyEarnings = Parameters.getRegWagesFemalesNE().getScore(this, Person.DoublesVariables.class) + wageRegressionRandomComponentNE;
            }
        }

        // Uprate and set level of potential earnings
        double upratedFullTimeHourlyEarnings = Math.exp(logFullTimeHourlyEarnings);
        if (upratedFullTimeHourlyEarnings < Parameters.MIN_HOURLY_WAGE_RATE) {
            setFullTimeHourlyEarningsPotential(Parameters.MIN_HOURLY_WAGE_RATE);
        } else if (upratedFullTimeHourlyEarnings > Parameters.MAX_HOURLY_WAGE_RATE) {
            setFullTimeHourlyEarningsPotential(Parameters.MAX_HOURLY_WAGE_RATE);
        } else {
            setFullTimeHourlyEarningsPotential(upratedFullTimeHourlyEarnings);
        }
    }
    public void setYpncp(double val) {
        ypncp = val;
    }
    public void setYpnoab(double val) {
        ypnoab = val;
    }
    public double getPensionIncomeAnnual() {
        return Math.sinh(ypnoab)*12.0;
    }
    private double setIncomeBySource(double score, double rmse, IncomeSource source) {

        double income = 0.0, gauss, minInc, maxInc;
        boolean redraw = true;
        if (IncomeSource.PrivatePension.equals(source)) {
            minInc = Parameters.MIN_PERSONAL_PENSION_PER_MONTH;
            maxInc = Parameters.MAX_PERSONAL_PENSION_PER_MONTH;
        } else if (IncomeSource.CapitalIncome.equals(source)) {
            minInc = Parameters.MIN_CAPITAL_INCOME_PER_MONTH;
            maxInc = Parameters.MAX_CAPITAL_INCOME_PER_MONTH;
        } else {
            throw new RuntimeException("source not recognised for setting income");
        }
        while (redraw) {

            gauss = capitalInnov.nextGaussian();
            income = Math.max(minInc, Math.sinh( score + rmse * gauss ));
//                                * Parameters.getYearUpratingIndex(model.getYear(), UpratingCase.Capital);
            if (income < maxInc) {
                redraw = false;
            }
        }
        return income;
    }
    protected void updateNonLabourIncome() {

        if (Parameters.enableIntertemporalOptimisations) {
            throw new RuntimeException("request to update non-labour income in person object when wealth is explicit");
        } else {
            // ypncp: inverse hyperbolic sine of capital income per month
            // ypnoab: inverse hyperbolic sine of pension income per month
            // yptciihs_dv: inverse hyperbolic sine of capital and pension income per month
            // variables updated with labour supply when enableIntertemporalOptimisations (as retirement can affect wealth and pension income)
            if (dag >= Parameters.MIN_AGE_TO_HAVE_INCOME) {
                if (dag <= 29 && Les_c4.Student.equals(les_c4) && leftEducation == false) {
                    boolean hasCapitalIncome = (capitalInnov.nextDouble() < Parameters.getRegIncomeI3a_selection().getProbability(this, Person.DoublesVariables.class)); // If true, individual receives capital income ypncp. Amount modelled in the next step.
                    if (hasCapitalIncome) {
                        double score = Parameters.getRegIncomeI3a().getScore(this, Person.DoublesVariables.class);
                        double rmse = Parameters.getRMSEForRegression("I3a");
                        double capinclevel = setIncomeBySource(score, rmse, IncomeSource.CapitalIncome);
                        ypncp = Parameters.asinh(capinclevel); //Capital income amount
                    }
                    else ypncp = 0.; //If no capital income, set amount to 0
                } else if ((Les_c4.Student.equals(les_c4) && leftEducation == true) || !les_c4.equals(Les_c4.Student)) {
                    boolean hasCapitalIncome = (capitalInnov.nextDouble() < Parameters.getRegIncomeI3b_selection().getProbability(this, Person.DoublesVariables.class)); // If true, individual receives capital income ypncp. Amount modelled in the next step.
                    if (hasCapitalIncome) {
                        double score = Parameters.getRegIncomeI3b().getScore(this, Person.DoublesVariables.class);
                        double rmse = Parameters.getRMSEForRegression("I3b");
                        double capinclevel = setIncomeBySource(score, rmse, IncomeSource.CapitalIncome);
                        ypncp = Parameters.asinh(capinclevel); //Capital income amount
                    }
                    else ypncp = 0.; //If no capital income, set amount to 0
                }
                if (Les_c4.Retired.equals(les_c4)) { // Retirement decision is modelled in the retirement process. Here only the amount of pension income for retired individuals is modelled.

                    double score, rmse;
                    if (Les_c4.Retired.equals(les_c4_lag1)) { // If person was retired in the previous period, use process I4b
                        score = Parameters.getRegIncomeI4b().getScore(this, Person.DoublesVariables.class);
                        rmse = Parameters.getRMSEForRegression("I4b");
                    } else { // For individuals in the first year of retirement, use process I4a
                        score = Parameters.getRegIncomeI4a().getScore(this, Person.DoublesVariables.class);
                        rmse = Parameters.getRMSEForRegression("I4a");
                    }

                    double pensioninclevel = setIncomeBySource(score, rmse, IncomeSource.PrivatePension);
                    ypnoab = Parameters.asinh(pensioninclevel);
                    if (ypnoab < 0 ) {
                        ypnoab = 0.;
                    }
                }

                double capital_income_multiplier = model.getSavingRate()/Parameters.SAVINGS_RATE;
                double yptciihs_dv_tmp_level = capital_income_multiplier*(Math.sinh(ypncp) + Math.sinh(ypnoab)); //Multiplied by the capital income multiplier, defined as chosen savings rate divided by the long-term average (specified in Parameters class)
                yptciihs_dv = Parameters.asinh(yptciihs_dv_tmp_level); //Non-employment non-benefit income is the sum of capital income and, for retired individuals, pension income.

                if (yptciihs_dv > 13.0) {
                    yptciihs_dv = 13.5;
                }
            }
        }
    }


    // ---------------------------------------------------------------------
    // Other Methods
    // ---------------------------------------------------------------------


    private boolean becomeResponsibleInHouseholdIfPossible() {
        if(dgn.equals(Gender.Male)) {
            if(benefitUnit.getMale() == null) {
                benefitUnit.setMale(this);	//This person is male, so becomes responsible male of benefitUnit
                return benefitUnit.getMale() == this;			//Should return true;
            }
            else return false;
//			else throw new IllegalArgumentException("ERROR - BenefitUnit " + benefitUnit.getKey().getId() + " already has a responsible male!  Cannot set resonsible male as person " + key.getId());
        }
        else {	//This person is female
            if(benefitUnit.getFemale() == null) {
                benefitUnit.setFemale(this);	//This person is female, so becomes responsible female of benefitUnit
                return benefitUnit.getFemale() == this;		//Should return true;
            }
            else return false;
//			else throw new IllegalArgumentException("ERROR - BenefitUnit " + benefitUnit.getKey().getId() + " already has a responsible female!  Cannot set resonsible female as person " + key.getId());
        }
    }

    public boolean atRiskOfWork() {

        /*
        Person "flexible in labour supply" must meet the following conditions:
        age >= 16 and <= 75
        not a student or retired
        not disabled
         */

        if(dag < Parameters.MIN_AGE_FLEXIBLE_LABOUR_SUPPLY)
            return false;
        if(dag > Parameters.MAX_AGE_FLEXIBLE_LABOUR_SUPPLY)
            return false;
        if (les_c4.equals(Les_c4.Retired))
            return false;
        if(les_c4.equals(Les_c4.Student))
            return false;
        if(dlltsd.equals(Indicator.True))
            return false;

        //For cases where the participation equation used for the Heckmann Two-stage correction of the wage equation results in divide by 0 errors.
        //These people will not work for any wage (their activity status will will be set to Nonwork in the Labour Market Module
        double inverseMillsRatio = getDoubleValue(DoublesVariables.InverseMillsRatio);
        if(!Double.isFinite(inverseMillsRatio)) {
            return false;
        }

        return true;		//Else return true
    }


    // Assign education level to school leavers using MultiProbitRegression
    // Note that persons are now assigned a Low education level by default at birth (to prevent null pointer exceptions when persons become old enough to marry while still being a student
    // (we now allow students to marry, given they can re-enter school throughout their lives).
    // The module only applies to students who are leaving school (activityStatus == Student and toLeaveSchool == true) - see inSchool()
    //TODO: Follow process E2a to assign education level
    private void setEducationLevel() {

//		if( activity_status.equals(Les_c4.Student) && toLeaveSchool) {

            //TODO: Need to see if it is possible to change MultiProbitRegression, so that we don't have to pass the Education class below, as it could
            // potentially cause problems if this variable would ever be set as a different class to the type that MultiProbitRegression has been
            // initialised with (i.e. the T type in the classes).


            Map<Education,Double> probs = Parameters.getRegEducationE2a().getProbabilities(this, Person.DoublesVariables.class);
            Education newEducationLevel = ManagerRegressions.multiEvent(probs, educationInnov.nextDouble());

//			System.out.println("Persid " + getKey().getId() + " Aged: " + dag + " With activity status: " + les_c4 + " Was set to leave school?  " + toLeaveSchool +  " Predicted education level "
//					+ " by process E2a is " + newEducationLevel + " And previous level was " + deh_c3);

            //Education has been set to Low by default for all new born babies, so it should never be null.
            //This is because we no longer prevent people in school to get married, given that people can re-enter education throughout their lives.
            //Note that by not filtering out students, we must assign a low education level by default to persons at birth to prevent a null pointer exception when new born persons become old enough to marry if they have not yet left school because
            //their education level has not yet been assigned.

//			System.out.println("Person with " + "age " + dag + " age sq " + dag_sq + " gender " + dgn + " mother ed " + dehm_c3 + " father ed " + dehf_c3 + " region " + getHousehold().getRegion() + " was assigned educ level " + newEducationLevel);

            if (newEducationLevel.equals(Education.Low)) {
                model.lowEd++;
            }
            else if (newEducationLevel.equals(Education.Medium)) {
                model.medEd++;
            }
            else if (newEducationLevel.equals(Education.High)) {
                model.highEd++;
            }
            else {
                model.nothing++;
            }


            if(deh_c3 != null) {
                if(newEducationLevel.ordinal() > deh_c3.ordinal()) {		//Assume Education level cannot decrease after re-entering school.
                    deh_c3 = newEducationLevel;
                }
            }
            else {
                deh_c3 = newEducationLevel;
            }


//			System.out.println("Education level is " + education + " new education level is " + newEducationLevel);
//			education = newEducationLevel;

//		}
    }

    public double getLiquidWealth() {

        if (dag <= Parameters.AGE_TO_BECOME_RESPONSIBLE) {
            return 0.0;
        } else if (benefitUnit != null) {
            return (Occupancy.Couple.equals(benefitUnit.getOccupancy())) ? benefitUnit.getLiquidWealth() / 2.0 : benefitUnit.getLiquidWealth();
        } else {
            throw new RuntimeException("Call to get liquid wealth for person object without benefit unit assigned");
        }
    }

    protected void projectEquivConsumption() {

        if (Parameters.enableIntertemporalOptimisations) {

            yearlyEquivalisedConsumption = benefitUnit.getDiscretionaryConsumptionPerYear() / benefitUnit.getEquivalisedWeight();
        } else {

            if (getLes_c4().equals(Les_c4.Retired)) {
                yearlyEquivalisedConsumption = benefitUnit.getEquivalisedDisposableIncomeYearly();
            } else {
                yearlyEquivalisedConsumption = Math.max(0., (1-model.getSavingRate())*benefitUnit.getEquivalisedDisposableIncomeYearly());
            }
        }
    }

    protected void updateVariables(boolean initialUpdate) {

        //Reset flags to default values
        toLeaveSchool = false;
        toGiveBirth = false;
        sedex = Indicator.False; //Reset left education variable
        der = Indicator.False; //Reset return to education indicator
        ded = Indicator.False; //Reset in education variable

        //Lagged variables
        les_c4_lag1 = les_c4;
        les_c7_covid_lag1 = les_c7_covid;
        household_status_lag = household_status;
        dhe_lag1 = dhe; //Update lag(1) of health
        dhm_lag1 = dhm; //Update lag(1) of mental health
        dhm_ghq_lag1 = dhm_ghq;
        dlltsd_lag1 = dlltsd; //Update lag(1) of long-term sick or disabled status
        needSocialCare_lag1 = needSocialCare;
        careHoursFromFormalWeekly_lag1 = careHoursFromFormalWeekly;
        careHoursFromPartnerWeekly_lag1 = careHoursFromPartnerWeekly;
        careHoursFromDaughterWeekly_lag1 = careHoursFromDaughterWeekly;
        careHoursFromSonWeekly_lag1 = careHoursFromSonWeekly;
        careHoursFromOtherWeekly_lag1 = careHoursFromOtherWeekly;
        deh_c3_lag1 = deh_c3; //Update lag(1) of education level
        ypnbihs_dv_lag1 = getYpnbihs_dv(); //Update lag(1) of gross personal non-benefit income
        dehsp_c3_lag1 = dehsp_c3; //Update lag(1) of partner's education status
        dhesp_lag1 = dhesp; //Update lag(1) of partner's health
        ynbcpdf_dv_lag1 = ynbcpdf_dv; //Lag(1) of difference between own and partner's gross personal non-benefit income
        idPartnerLag1 = idPartner; //Lag(1) of partner's ID
        dcpagdf_lag1 = dcpagdf; //Lag(1) of age difference between partners
        lesdf_c4_lag1 = lesdf_c4; //Lag(1) of own and partner's activity status
        dcpst_lag1 = dcpst; //Lag(1) of partnership status

        // second and further lags are not available at initialisation - set values to the contemporaneous value
        if (initialUpdate) {
            yplgrs_dv_lag1 = getYplgrs_dv(); //Lag(1) of gross personal employment income
            yplgrs_dv_lag2 = getYplgrs_dv();
            yplgrs_dv_lag3 = getYplgrs_dv();
            yptciihs_dv_lag1 = getYptciihs_dv();
            yptciihs_dv_lag2 = getYptciihs_dv();
            yptciihs_dv_lag3 = getYptciihs_dv();
            ypncp_lag1 = getYpncp();
            ypncp_lag2 = getYpncp();
            ypnoab_lag1 = getYpnoab();
            ypnoab_lag2 = getYpnoab();
        } else {
            yplgrs_dv_lag3 = yplgrs_dv_lag2; //Lag(3) of gross personal employment income
            yplgrs_dv_lag2 = yplgrs_dv_lag1; //Lag(2) of gross personal employment income
            yplgrs_dv_lag1 = getYplgrs_dv(); //Lag(1) of gross personal employment income

            yptciihs_dv_lag3 = yptciihs_dv_lag2; //Lag(3) of gross personal non-employment non-benefit income
            yptciihs_dv_lag2 = yptciihs_dv_lag1; //Lag(2) of gross personal non-employment non-benefit income
            yptciihs_dv_lag1 = getYptciihs_dv(); //Lag(1) of gross personal non-employment non-benefit income

            ypncp_lag2 = ypncp_lag1;
            ypncp_lag1 = getYpncp();

            ypnoab_lag2 = ypnoab_lag1;
            ypnoab_lag1 = getYpnoab();
        }
        labourSupplyWeekly_L1 = getLabourSupplyWeekly(); // Lag(1) of labour supply
        receivesBenefitsFlag_L1 = receivesBenefitsFlag; // Lag(1) of flag indicating if individual receives benefits
        L1_fullTimeHourlyEarningsPotential = fullTimeHourlyEarningsPotential; // Lag(1) of potential hourly earnings

        if(Les_c4.Student.equals(les_c4)) {
            labourSupplyWeekly = Labour.ZERO;			//Number of hours of labour supplied each week
            fullTimeHourlyEarningsPotential = 0.;
            setDed(Indicator.True); //Indicator if in school set to true
        }

        if (Les_c4.Retired.equals(les_c4)) {
            labourSupplyWeekly = Labour.ZERO;
            fullTimeHourlyEarningsPotential = 0.;
        }

        if (Les_c4.NotEmployed.equals(les_c4)) {
            labourSupplyWeekly = Labour.ZERO;
        }

        if(Les_c4.EmployedOrSelfEmployed.equals(les_c4)) {
            //Increment work history by 12 months for those in employment
            //TOOD: I don't think liwwh is used anywhere in the model at the moment, perhaps can be deleted (PB, 01.11.2021)
            liwwh = liwwh+12;
        }

        //Update partner's variables
        if(partner != null) {
            if(partner.getDeh_c3() != null) {
                dehsp_c3 = partner.getDeh_c3();
            } else dehsp_c3 = null;
            if(partner.getDhe() != null) {
                dhesp = partner.getDhe();
            } else dhesp = null;
            if(partner.getYpnbihs_dv() != null && getYpnbihs_dv() != null) {
                //Keep as difference between transformed variables to maintain compatibility with estimates
                ynbcpdf_dv = getYpnbihs_dv() - partner.getYpnbihs_dv();
            } else ynbcpdf_dv = null;

            dcpagdf = dag - partner.dag; //Calculate the difference between own and partner's age

            //Determine lesdf_c4 (own and partner activity status) based on own les_c4 and partner's les_c4
            if(les_c4 != null && partner.getLes_c4() != null) {
                if(les_c4.equals(Les_c4.EmployedOrSelfEmployed)) {
                    if(partner.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed)) {
                        lesdf_c4 = Lesdf_c4.BothEmployed;
                    }
                    else if(partner.getLes_c4().equals(Les_c4.NotEmployed) || partner.getLes_c4().equals(Les_c4.Student) || partner.getLes_c4().equals(Les_c4.Retired)) {
                        lesdf_c4 = Lesdf_c4.EmployedSpouseNotEmployed;
                    }
                    else lesdf_c4 = null; //TODO: Should we throw an exception?
                }
                else if(les_c4.equals(Les_c4.NotEmployed) || les_c4.equals(Les_c4.Student) || les_c4.equals(Les_c4.Retired)) {
                    if(partner.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed)) {
                        lesdf_c4 = Lesdf_c4.NotEmployedSpouseEmployed;
                    }
                    else if(partner.getLes_c4().equals(Les_c4.NotEmployed) || partner.getLes_c4().equals(Les_c4.Student) || partner.getLes_c4().equals(Les_c4.Retired)) {
                        lesdf_c4 = Lesdf_c4.BothNotEmployed;
                    }
                    else lesdf_c4 = null; //TODO: Should we throw an exception?
                }
            }
            else lesdf_c4 = null;
        }

        //Determine partnership status (dcpst):
        if(partner == null) {
            if(leftPartnership == true) { //No partner, but left partnership in the past
                dcpst = Dcpst.PreviouslyPartnered;
            }
            else {
                dcpst = Dcpst.SingleNeverMarried; //No partner and never left partnership in the past
            }
        }
        else {
            dcpst = Dcpst.Partnered;
        }

        //Draw desired age and wage differential for parametric partnership formation for people above age to get married:
        //TODO: Should it be updated yearly or only if null?
        if (dag >= Parameters.MIN_AGE_COHABITATION) {
            double[] sampledDifferentials = Parameters.getWageAndAgeDifferentialMultivariateNormalDistribution().sample(); //Sample age and wage differential from the bivariate normal distribution
                desiredAgeDiff = sampledDifferentials[0];
                desiredEarningsPotentialDiff = sampledDifferentials[1];
        }
    }

    protected void leavePartner() {
        // only considered for female - male is partner

        // update partner's variables first
        partner.setPartner(null); 		   //Do for the partner as we model the probability of couple splitting based on female leaving the partnership
        partner.setDcpyy(null);
        partner.setDcpex(Indicator.True);
        partner.setDcpst(Dcpst.PreviouslyPartnered);
        partner.setLeftPartnership(true); //Set to true if leaves partnership to use with fertility regression, this is never reset
        partner.setHousehold_status(Household_status.Single);

        setPartner(null);		  //Do this before leaveHome to ensure partner doesn't leave home with this person!
        setDcpyy(null); 		  //Set number of years in partnership to null if leaving partner
        setDcpex(Indicator.True); //Set variable indicating leaving the partnership to true
        setDcpst(Dcpst.PreviouslyPartnered);
        setLeftPartnership(true); //Set to true if leaves partnership to use with fertility regression, this is never reset
        idHousehold = null;

        setupNewBenefitUnit(true);
    }

    protected Household setupNewHousehold(boolean automaticUpdateOfHouseholds) {

        Household newHousehold = new Household(); //Set up a new empty household
        if (automaticUpdateOfHouseholds) {

            model.getHouseholds().add(newHousehold);

            //Remove benefitUnit from old household (if exists)
            if (benefitUnit.getHousehold() != null) {

                benefitUnit.getHousehold().removeBenefitUnit(benefitUnit);
            }
        }
        newHousehold.addBenefitUnit(this.benefitUnit); //Add benefit unit of the person moving out to the new household
        idHousehold = newHousehold.getId();

        return newHousehold;
    }


    /**
     *
     *   CREATE NEW BENEFIT UNIT FOR PERSON
     *
     *   A new benefit unit is created for someone when:
     *     they reach age of maturity
     *      - default to parental household (no change in household)
     *     they enter a new cohabiting relationship
     *     they leave a cohabiting relationship (in which case it is the woman that is allocated a new benefit unit)
     *
     *   @param automaticUpdateOfBenefitUnits - This is a toggle to control the automatic update of the model's set of benefitUnits,
     *   which would cause concurrent modification exception to be thrown if called within an iteration through benefitUnits.
     *
     *   There are two possible causes of concurrent modification issues: 1) adding of the new benefitUnit to the model's
     *   benefitUnits set and 2) removal of the last person from the benefitUnit when updateHousehold() method is called would
     *   lead to the automatic removal of the old house from the model's benefitUnits set.
     *
     *   To prevent concurrent modification
     *   exception being thrown, set this parameter to false and use the iterator on the benefitUnits to add the new
     *   benefitUnit manually e.g. (houseIterator.add(newHouse)).  Also the updateHousehold() method will need to be called and the person
     *   removed from the old house manually outside the iteration through benefitUnits.
     *
     *   @return new benefit unit
     */
    protected BenefitUnit setupNewBenefitUnit(boolean automaticUpdateOfBenefitUnits) {

        // identify children transferring to new benefit unit
        // this needs to be done before new benefit units are created because the new benefit units
        // are instantiated with the transferred responsible adults, at which time the old benefit unit
        // references are also transferred
        Set<Person> childrenToNewBenefitUnit = childrenToFollowPerson(this);
        if (partner != null) {
            childrenToNewBenefitUnit.addAll(childrenToFollowPerson(partner));
        }


        /*
         * Create new benefitUnit
         */
        BenefitUnit newBenefitUnit;
        if(partner != null) {
            // relationship forms
            // when a relationship forms newBenefitUnit is populated with data described by person and partner, both of whom
            // are re-assigned from their previous benefitUnits to newBenefitUnit and households to newHousehold

            // Multigeneration families are not allowed - setting up a new benefit unit with partner requires setting up new household
            Household newHousehold = new Household();
            model.getHouseholds().add(newHousehold);

            // Partner will become responsible adult, even if their age < age to move out of home if they are already living with their partner
            newBenefitUnit = new BenefitUnit(this, partner, childrenToNewBenefitUnit, newHousehold);
        } else {
            // includes when reach age of maturity and when relationship dissolves.
            // When relationship dissolves person is the female, and this routine sets up a new benefit unit for the woman,
            // updates characteristics for both newBenefitUnit and benefitUnit, and then leaves benefitUnit to the man

            Household newHousehold;
            if (idHousehold != null) {
                // this initialises maturing children to the household of their parents

                newHousehold = benefitUnit.getHousehold();
            } else {
                // this creates a new household for females following relationship dissolution

                newHousehold = new Household();
                model.getHouseholds().add(newHousehold);
            }
            newBenefitUnit = new BenefitUnit(this, childrenToNewBenefitUnit, newHousehold);
        }

        // Automatic update of collections if required
        // Removing children (above) from the benefitUnit should not lead to the removal of the benefitUnit (due to becoming an empty benefitUnit)
        // because there should still be the responsible male or female (this person or partner) in the benefitUnit, which we shall now remove from
        // their benefitUnit below.
        //
        // automaticUpdateOfBenefitUnits is a toggle to prevent automatic update to the model's set of benefitUnits, which would cause concurrent
        // modification exception to be thrown if called within an iteration through benefitUnits.  In this case, set parameter to false and use the
        // iterator on the benefitUnits to add manually e.g. (houseIterator.add(newBU)).
        if (automaticUpdateOfBenefitUnits) {

            model.getBenefitUnits().add(newBenefitUnit);	//This will cause concurrent modification if setupNewHome is called in an iteration through benefitUnits
        }

        newBenefitUnit.initializeFields();
        return newBenefitUnit;
    }

    private Set<Person> childrenToFollowPerson(Person person) {
        // by default children follow their mother, but if no mother then they follow their father

        Set<Person> childrenToNewBenefitUnit = new LinkedHashSet<Person>();
        for (Person child : person.getBenefitUnit().getChildren()) {
            if (Gender.Female.equals(person.getDgn())) {
                if (Objects.equals(person.getKey().getId(), child.getIdMother())) {
                    childrenToNewBenefitUnit.add(child);
                }
            } else if (child.getIdMother() == null) {
                if (Objects.equals(child.getIdFather(), person.getKey().getId())) {
                    childrenToNewBenefitUnit.add(child);
                }
            }
        }

        return childrenToNewBenefitUnit;
    }

    @Override
    public int compareTo(Person person) {
        if (idBenefitUnit == null || person.getIdBenefitUnit() == null)
            throw new IllegalArgumentException("attempt to compare benefit units prior to initialisation");
        return (int) (idBenefitUnit - person.getIdBenefitUnit());
    }

    /*
    This method takes les_c4 (which is a more aggregated version of labour force activity statuses) and returns les_c6 version.
    Used when setting initial values of les_c6 from existing les_c4.
    */
    public void initialise_les_c6_from_c4() {
        if (les_c4.equals(Les_c4.EmployedOrSelfEmployed)) {
            les_c7_covid = Les_c7_covid.Employee;
        } else if (les_c4.equals(Les_c4.NotEmployed)) {
            les_c7_covid = Les_c7_covid.NotEmployed;
        } else if (les_c4.equals(Les_c4.Student)) {
            les_c7_covid = Les_c7_covid.Student;
        } else if (les_c4.equals(Les_c4.Retired)) {
            les_c7_covid = Les_c7_covid.Retired;
        }

        //In the first period the lagged value will be equal to the contemporaneous value
        if (les_c7_covid_lag1 == null) {
            les_c7_covid_lag1 = les_c7_covid;
        }
    }


    //-----------------------------------------------------------------------------------
    // IIntSource implementation for the CrossSection.Integer objects in the collector
    //-----------------------------------------------------------------------------------

    public enum IntegerVariables {			//For cross section of Collector
        isEmployed,
        isNotEmployed,
        isRetired,
        isStudent,
        isNotEmployedOrRetired,
        isToBePartnered,
        isPsychologicallyDistressed,
        isNeedSocialCare,
    }

    public int getIntValue(Enum<?> variableID) {

        switch ((IntegerVariables) variableID) {

        case isEmployed:
            if (les_c4 == null) return 0;		//For inactive people, who don't participate in the labour market
            else if (les_c4.equals(Les_c4.EmployedOrSelfEmployed)) return 1;
            else return 0;		//For unemployed case

        case isNotEmployed:
            if (les_c4 == null) return 0;
            else if (les_c4.equals(Les_c4.NotEmployed)) return 1;
            else return 0;

        case isRetired:
            if (les_c4 == null) return 0;
            else if (les_c4.equals(Les_c4.Retired)) return 1;
            else return 0;

        case isStudent:
            if (les_c4 == null) return 0;
            else if (les_c4.equals(Les_c4.Student)) return 1;
            else return 0;

        case isNotEmployedOrRetired:
            if (les_c4 == null) return 0;
            else if (les_c4.equals(Les_c4.NotEmployed) || les_c4.equals(Les_c4.Retired)) return 1;
            else return 0;

        case isToBePartnered:
            return (isToBePartnered())? 1 : 0;

        case isPsychologicallyDistressed:
            return (dhm_ghq)? 1 : 0;

        case isNeedSocialCare:
            return (Indicator.True.equals(needSocialCare)) ? 1 : 0;

        default:
            throw new RuntimeException("Unsupported variable " + variableID.name() + " in Person#getIntValue");
        }
    }


    // ---------------------------------------------------------------------
    // implements IDoubleSource for use with Regression classes
    // ---------------------------------------------------------------------

    public enum DoublesVariables {

        //Variables:
        GrossEarningsYearly,
        GrossLabourIncomeMonthly,

        //Regressors:
        Age,
        AgeSquared,
        AgeCubed,
        LnAge,
        Age23to25,						// Indicator for whether the person is in the Age category (see below for definition)
        Age26to30,						// Indicator for whether the person is in the Age category (see below for definition)
        Age21to27,						// Indicator for whether the person is in the Age category (see below for definition)
        Age28to30,						// Indicator for whether the person is in the Age category (see below for definition)
        AgeUnder25,
        Age25to39,
        AgeOver39,
        Age67to68,
        Age69to70,
        Age71to72,
        Age73to74,
        Age75to76,
        Age77to78,
        Age79to80,
        Age81to82,
        Age83to84,
        Age85plus,
        StatePensionAge,
        Cohort,
        Constant, 						// For the constant (intercept) term of the regression
        D_children_2under,				// Indicator (dummy variables for presence of children of certain ages in the benefitUnit)
        D_children_3_6,
        D_children_7_12,
        D_children_13_17,
        D_children_18over,				//Currently this will return 0 (false) as children leave home when they are 18
        D_children,
        Dnc_L1, 						//Lag(1) of number of children of all ages in the benefitUnit
        Dnc02_L1, 						//Lag(1) of number of children aged 0-2 in the benefitUnit
        Dnc017, 						//Number of children aged 0-17 in the benefitUnit
        Dcrisis,
        Dgn,							//Gender: returns 1 if male
        Dag,
        Dag_sq,
        Dcpyy_L1, 						//Lag(1) number of years in partnership
        Dcpagdf_L1, 					//Lag(1) of age difference between partners
        Dcpst_Single,					//Single never married
        Dcpst_Partnered,				//Partnered
        Dcpst_PreviouslyPartnered,		//Previously partnered
        Dcpst_Single_L1, 				//Lag(1) of partnership status is Single
        Dcpst_PreviouslyPartnered_L1,   //Lag(1) of partnership status is previously partnered
        Dhe,							//Health status
        Dhe_L1, 						//Health status lag(1)
        Dhe_2,
        Dhe_3,
        Dhe_4,
        Dhe_5,
        NeedCare_L1,
        ReceiveCare_L1,
        CareMarketMixed_L1,
        CareMarketFormal_L1,
        CareFromPartner_L1,
        CareFromDaughter_L1,
        CareFromSon_L1,
        CareFromOther_L1,
        CareFromDaughterSon_L1,
        CareFromDaughterOther_L1,
        CareFromSonOnly_L1,
        CareFromSonOther_L1,
        CareFromOtherOnly_L1,
        CareFromFormal,
        CareFromInformal,
        CareFromPartner,
        CareFromDaughter,
        CareFromSon,
        CareFromOther,
        Dhe_VeryGood,
        Dhe_Good,
        Dhe_Fair,
        Dhe_Poor,
        Dhe_c5_1_L1,
        Dhe_c5_2_L1,
        Dhe_c5_3_L1,
        Dhe_c5_4_L1,
        Dhe_c5_5_L1,
        Dhm,							//Mental health status
        Dhm_L1,							//Mental health status lag(1)
        Dhesp_L1, 						//Lag(1) of partner's health status
        Ded,
        Deh_c3_High,
        Deh_c3_Medium,
        Deh_c3_Medium_L1, 				//Education level lag(1) equals medium
        Deh_c3_Low,
        Deh_c3_Low_L1,					//Education level lag(1) equals low
        Dehm_c3_High,					//Mother's education == High indicator
        Dehm_c3_Medium,					//Mother's education == Medium indicator
        Dehm_c3_Low,					//Mother's education == Low indicator
        Dehf_c3_High,					//Father's education == High indicator
        Dehf_c3_Medium,					//Father's education == Medium indicator
        Dehf_c3_Low,					//Father's education == Low indicator
        Dehsp_c3_Medium_L1,				//Partner's education == Medium at lag(1)
        Dehsp_c3_Low_L1,				//Partner's education == Low at lag(1)
        Dhhtp_c4_CoupleChildren_L1,
        Dhhtp_c4_CoupleNoChildren_L1,
        Dhhtp_c4_SingleNoChildren_L1,
        Dhhtp_c4_SingleChildren_L1,
        Dlltsd,							//Long-term sick or disabled
        Dlltsd_L1,						//Long-term sick or disabled lag(1)
        FertilityRate,
        Female,
        InverseMillsRatio,
        Ld_children_3under,
        Ld_children_4_12,
        Lemployed,
        Lnonwork,
        Lstudent,
        Lunion,
        Les_c3_Student_L1,
        Les_c3_NotEmployed_L1,
        Les_c3_Employed_L1,
        Les_c3_Sick_L1,					//This is based on dlltsd
        Lessp_c3_Student_L1,			//Partner variables
        Lessp_c3_NotEmployed_L1,
        Lessp_c3_Sick_L1,
        Retired,
        Lesdf_c4_EmployedSpouseNotEmployed_L1, 					//Own and partner's activity status lag(1)
        Lesdf_c4_NotEmployedSpouseEmployed_L1,
        Lesdf_c4_BothNotEmployed_L1,
        Liwwh,									//Work history in months
        NumberChildren,
        NumberChildren_2under,
        OnleaveBenefits,
        OtherIncome,
        Parents,
        PartTimeRate,
        PartTime_AND_Ld_children_3under,			//Interaction term conditional on if the person had a child under 3 at the previous time-step
        ResStanDev,
        Single,
        Single_kids,
        Sfr, 										//Scenario : fertility rate This retrieves the fertility rate by region and year to use in fertility regression
        Union,
        Union_kids,
        Year,										//Year as in the simulation, e.g. 2009
        Year_transformed,							//Year - 2000
        Year_transformed_monetary,					//Year-2000 that stops in 2017, for use with monetary processes
        Ydses_c5_Q2_L1, 							//HH Income Lag(1) 2nd Quantile
        Ydses_c5_Q3_L1,								//HH Income Lag(1) 3rd Quantile
        Ydses_c5_Q4_L1,								//HH Income Lag(1) 4th Quantile
        Ydses_c5_Q5_L1,								//HH Income Lag(1) 5th Quantile
        Ypnbihs_dv_L1,								//Gross personal non-benefit income lag(1)
        Ypnbihs_dv_L1_sq,							//Square of gross personal non-benefit income lag(1)
        Ynbcpdf_dv_L1, 								//Lag(1) of difference between own and partner's gross personal non-benefit income
        Yptciihs_dv_L1,								//Lag(1) of gross personal non-employment non-benefit income
        Yptciihs_dv_L2,								//Lag(2) of gross personal non-employment non-benefit income
        Yptciihs_dv_L3,								//Lag(3) of gross personal non-employment non-benefit income
        Ypncp_L1,									//Lag(1) of capital income
        Ypncp_L2,									//Lag(2) of capital income
        Ypnoab_L1,									//Lag(1) of pension income
        Ypnoab_L2,									//Lag(2) of pension income
        Yplgrs_dv_L1,								//Lag(1) of gross personal employment income
        Yplgrs_dv_L2,								//Lag(2) of gross personal employment income
        Yplgrs_dv_L3,								//Lag(3) of gross personal employment income
        Reached_Retirement_Age,						//Indicator whether individual is at or above retirement age
        Reached_Retirement_Age_Sp,					//Indicator whether spouse is at or above retirement age
        Reached_Retirement_Age_Les_c3_NotEmployed_L1, //Interaction term for being at or above retirement age and not employed in the previous year
        EquivalisedIncomeYearly, 							//Equivalised income for use with the security index
        EquivalisedConsumptionYearly,
        sIndex,
        sIndexNormalised,

        //New enums for the mental health Step 1 and 2:
        EmployedToUnemployed,
        UnemployedToEmployed,
        PersistentUnemployed,
        NonPovertyToPoverty,
        PovertyToNonPoverty,
        PersistentPoverty,
        RealIncomeChange, //Note: the above return a 0 or 1 value, but income variables will return the change in income or 0
        RealIncomeDecrease_D,
        D_Econ_benefits,
        D_Home_owner,
        Covid_2020_D,
        Covid_2021_D,
        Pt,
        L1_log_hourly_wage,
        L1_log_hourly_wage_sq,
        L1_hourly_wage,
        Deh_c3_Medium_Dag,
        Deh_c3_Low_Dag,

        //New enums to handle the covariance matrices
        Ld_children_3underIT,
        Ld_children_4_12IT,
        LactiveIT,
        EduHighIT,
        LunionIT,
        EduMediumIT,

        ITC,			//Italy
        ITF,
        ITG,
        ITH,
        ITI,

        UKC,				//UK
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
        UKmissing,

        // Covid-19 labour market module regressors below:
        Dgn_Dag,
        Employmentsonfullfurlough,
        Employmentsonflexiblefurlough,
        CovidTransitionsMonth,
        Lhw_L1,
        Dgn_Lhw_L1,
        Covid19GrossPayMonthly_L1,
        Covid19ReceivesSEISS_L1,
        Les_c7_Covid_Furlough_L1,
        Blpay_Q2,
        Blpay_Q3,
        Blpay_Q4,
        Blpay_Q5,
        Dgn_baseline,
        Dhmghq_L1,
        RealWageGrowth,
        RealGDPGrowth,
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

    public double getDoubleValue(Enum<?> variableID) {

        switch ((DoublesVariables) variableID) {

        case GrossEarningsYearly:
            return getGrossEarningsYearly();
        case GrossLabourIncomeMonthly:
            return getCovidModuleGrossLabourIncome_Baseline();
        //Regressors
        case Age:
            return (double) dag;
        case Dag:
            return (double) dag;
        case Dag_sq:
            return (double) dag*dag;
        case AgeSquared:
//			log.debug("age sq");
            return (double) dag * dag;
        case AgeCubed:
//			log.debug("age cub");
            return (double) dag * dag * dag;
        case LnAge:
            return Math.log(dag);
        case Age23to25:
//			log.debug("age 23 to 25");
            if(dag >= 23 && dag <= 25) {
                 return 1.;
             }
             else return 0.;
        case Age26to30:
//			log.debug("age 26 to 30");
             if(dag >= 26 && dag <= 30) {
                 return 1.;
             }
             else return 0.;
        case Age21to27:
//			log.debug("age 21 to 27");
             if(dag >= 21 && dag <= 27) {
                 return 1.;
             }
             else return 0.;
        case Age28to30:
//			log.debug("age 28 to 30");
             if(dag >= 28 && dag <= 30) {
                 return 1.;
             }
             else return 0.;
        case AgeUnder25:
            return (dag<25) ? 1. : 0.;
        case Age25to39:
            return (dag>=25 && dag<=39) ? 1. : 0.;
        case AgeOver39:
            return (dag>39) ? 1. : 0.;
        case Age67to68:
            return (dag>=67 && dag<=68) ? 1. : 0.;
        case Age69to70:
            return (dag>=69 && dag<=70) ? 1. : 0.;
        case Age71to72:
            return (dag>=71 && dag<=72) ? 1. : 0.;
        case Age73to74:
            return (dag>=73 && dag<=74) ? 1. : 0.;
        case Age75to76:
            return (dag>=75 && dag<=76) ? 1. : 0.;
        case Age77to78:
            return (dag>=77 && dag<=78) ? 1. : 0.;
        case Age79to80:
            return (dag>=79 && dag<=80) ? 1. : 0.;
        case Age81to82:
            return (dag>=81 && dag<=82) ? 1. : 0.;
        case Age83to84:
            return (dag>=83 && dag<=84) ? 1. : 0.;
        case Age85plus:
            return (dag>=85) ? 1. : 0.;
        case StatePensionAge:
            return (dag >= 68) ? 1. : 0.;
        case NeedCare_L1:
            return (Indicator.True.equals(needSocialCare_lag1)) ? 1. : 0.;
        case ReceiveCare_L1:
            return (getTotalHoursSocialCare_L1() > 0.01) ? 1. : 0.;
        case CareMarketMixed_L1:
            return (getHoursFormalSocialCare_L1()>0.01 && getHoursInformalSocialCare_L1()>0.01) ? 1. : 0.;
        case CareMarketFormal_L1:
            return (getHoursFormalSocialCare_L1()>0.01 && getHoursInformalSocialCare_L1()<0.01) ? 1. : 0.;
        case CareFromPartner_L1:
            return (getCareHoursFromPartner_L1() > 0.01) ? 1. : 0.;
        case CareFromDaughter_L1:
            return (getCareHoursFromDaughter_L1() > 0.01) ? 1. : 0.;
        case CareFromSon_L1:
            return (getCareHoursFromSon_L1() > 0.01) ? 1. : 0.;
        case CareFromOther_L1:
            return (getCareHoursFromOther_L1() > 0.01) ? 1. : 0.;
        case CareFromDaughterSon_L1:
            return (getCareHoursFromDaughter_L1() > 0.01 && getCareHoursFromSon_L1() > 0.01) ? 1. : 0.;
        case CareFromDaughterOther_L1:
            return (getCareHoursFromDaughter_L1() > 0.01 && getCareHoursFromOther_L1() > 0.01) ? 1. : 0.;
        case CareFromSonOnly_L1:
            return (getCareHoursFromSon_L1() > 0.01 && Math.abs(getHoursInformalSocialCare_L1() - getCareHoursFromSon_L1())<0.01) ? 1. : 0.;
        case CareFromSonOther_L1:
            return (getCareHoursFromSon_L1() > 0.01 && getCareHoursFromOther_L1() > 0.01) ? 1. : 0.;
        case CareFromOtherOnly_L1:
            return (getCareHoursFromOther_L1() > 0.01 && Math.abs(getHoursInformalSocialCare_L1() - getCareHoursFromOther_L1())<0.01) ? 1. : 0.;
        case CareFromFormal:
            return (careFromFormal) ? 1. : 0.;
        case CareFromInformal:
            return (careFromPartner || careFromDaughter || careFromSon || careFromOther) ? 1. : 0.;
        case CareFromPartner:
            return (careFromPartner) ? 1. : 0.;
        case CareFromDaughter:
            return (careFromDaughter) ? 1. : 0.;
        case CareFromSon:
            return (careFromSon) ? 1. : 0.;
        case CareFromOther:
            return (careFromOther) ? 1. : 0.;
        case Constant:
            return 1.;
        case Dcpyy_L1:
            return (dcpyy_lag1 != null) ? (double) dcpyy_lag1 : 0.0;
        case Dcpagdf_L1:
            return (dcpagdf_lag1 != null) ? (double) dcpagdf_lag1 : 0.0;
        case Dcpst_Single:
            if(dcpst != null) {
                return dcpst.equals(Dcpst.SingleNeverMarried)? 1. : 0.;
            } else return 0.;
        case Dcpst_Partnered:
            return (Dcpst.Partnered.equals(dcpst)) ? 1.0 : 0.0;
        case Dcpst_PreviouslyPartnered:
            if(dcpst != null) {
                return dcpst.equals(Dcpst.PreviouslyPartnered)? 1. : 0.;
            } else return 0.;
        case Dcpst_Single_L1:
            if(dcpst_lag1 != null) {
                return dcpst_lag1.equals(Dcpst.SingleNeverMarried)? 1. : 0.;
            }
            else return 0.;
        case Dcpst_PreviouslyPartnered_L1:
            if(dcpst_lag1 != null) {
                return dcpst_lag1.equals(Dcpst.PreviouslyPartnered)? 1. : 0.;
            }
            else return 0.;
        case D_children_2under:
            return (double) getD_children_2under().ordinal();
        case D_children_3_6:
            if (benefitUnit.getD_children_3_6() != null) {
                return (double) benefitUnit.getD_children_3_6().ordinal();
            }
            else return 0.;
        case D_children_7_12:
            if (benefitUnit.getD_children_7_12() != null) {
                return (double) benefitUnit.getD_children_7_12().ordinal();
            }
            else return 0.;
        case D_children_13_17:
            if (benefitUnit.getD_children_13_17() != null) {
                return (double) benefitUnit.getD_children_13_17().ordinal();
            }
            else return 0.;
        case D_children_18over:
            if (benefitUnit.getD_children_18over() != null) {
                return (double) benefitUnit.getD_children_18over().ordinal();	//Currently this will always return 0 (false) as children leave home when they are 18 years old
            }
            else return 0.;
        case D_children:
            return (getN_children_allAges()>0) ? 1. : 0.;
        case Dnc_L1:
            return (double) getN_children_allAges_lag1();
        case Dnc02_L1:
            return (double) getN_children_02_lag1();
        case Dnc017:
            return (double) getN_children_017();
        case Dgn:
            return (Gender.Male.equals(dgn)) ? 1.0 : 0.0;
        case Dhe:
            return dhe.getValue();
        case Dhe_L1:
            return dhe_lag1.getValue();
        case Dhe_VeryGood:
            return (Dhe.VeryGood.equals(dhe)) ? 1. : 0.;
        case Dhe_Good:
            return (Dhe.Good.equals(dhe)) ? 1. : 0.;
        case Dhe_Fair:
            return (Dhe.Fair.equals(dhe)) ? 1. : 0.;
        case Dhe_Poor:
            return (Dhe.Poor.equals(dhe)) ? 1. : 0.;
        case Dhe_2:
            return (Dhe.Fair.equals(dhe))? 1. : 0.;
        case Dhe_3:
            return (Dhe.Good.equals(dhe))? 1. : 0.;
        case Dhe_4:
            return (Dhe.VeryGood.equals(dhe))? 1. : 0.;
        case Dhe_5:
            return (Dhe.Excellent.equals(dhe))? 1. : 0.;
        case Dhe_c5_1_L1:
            return (Dhe.Poor.equals(dhe_lag1)) ? 1.0 : 0.0;
        case Dhe_c5_2_L1:
            return (Dhe.Fair.equals(dhe_lag1)) ? 1.0 : 0.0;
        case Dhe_c5_3_L1:
            return (Dhe.Good.equals(dhe_lag1)) ? 1.0 : 0.0;
        case Dhe_c5_4_L1:
            return (Dhe.VeryGood.equals(dhe_lag1)) ? 1.0 : 0.0;
        case Dhe_c5_5_L1:
            return (Dhe.Excellent.equals(dhe_lag1)) ? 1.0 : 0.0;
        case Dhm:
            return dhm;
        case Dhm_L1:
            if (dhm_lag1 != null && dhm_lag1 >= 0.) {
                return dhm_lag1;
            }
            else return 0.;
        case Dhmghq_L1:
            return (dhm_ghq_lag1)? 1. : 0.;
        case Dhesp_L1:
            if(dhesp_lag1 != null) {
                return dhesp_lag1.getValue();
            }
            else return 0.;
        case Ded:
            return (Indicator.True.equals(ded)) ? 1.0 : 0.0;
        case Deh_c3_High:
            return (Education.High.equals(deh_c3)) ? 1.0 : 0.0;
        case Deh_c3_Medium:
            return (Education.Medium.equals(deh_c3)) ? 1.0 : 0.0;
        case Deh_c3_Medium_L1:
            return (Education.Medium.equals(deh_c3_lag1)) ? 1.0 : 0.0;
        case Deh_c3_Low:
            return (Education.Low.equals(deh_c3)) ? 1.0 : 0.0;
        case Deh_c3_Low_L1:
            return (Education.Low.equals(deh_c3_lag1)) ? 1.0 : 0.0;
        case Dehm_c3_High:
            return (Education.High.equals(dehm_c3)) ? 1.0 : 0.0;
        case Dehm_c3_Medium:
            return (Education.Medium.equals(dehm_c3)) ? 1.0 : 0.0;
        case Dehm_c3_Low:
            return (Education.Low.equals(dehm_c3)) ? 1.0 : 0.0;
        case Dehf_c3_High:
            return (Education.High.equals(dehf_c3)) ? 1.0 : 0.0;
        case Dehf_c3_Medium:
            return (Education.Medium.equals(dehf_c3)) ? 1.0 : 0.0;
        case Dehf_c3_Low:
            return (Education.Low.equals(dehf_c3)) ? 1.0 : 0.0;
        case Dehsp_c3_Medium_L1:
            if(dehsp_c3_lag1 != null) {
                return dehsp_c3_lag1.equals(Education.Medium)? 1. : 0.;
            }
            else return 0.;
        case Dehsp_c3_Low_L1:
            if(dehsp_c3_lag1 != null) {
                return dehsp_c3_lag1.equals(Education.Low)? 1. : 0.;
            }
            else return 0.;
        case Dhhtp_c4_CoupleChildren_L1:
            return (Dhhtp_c4.CoupleChildren.equals(getDhhtp_c4_lag1())) ? 1.0 : 0.0;
        case Dhhtp_c4_CoupleNoChildren_L1:
            return (Dhhtp_c4.CoupleNoChildren.equals(getDhhtp_c4_lag1())) ? 1.0 : 0.0;
        case Dhhtp_c4_SingleNoChildren_L1:
            return (Dhhtp_c4.SingleNoChildren.equals(getDhhtp_c4_lag1())) ? 1.0 : 0.0;
        case Dhhtp_c4_SingleChildren_L1:
            return (Dhhtp_c4.SingleChildren.equals(getDhhtp_c4_lag1())) ? 1.0 : 0.0;
        case Dlltsd:
            if(dlltsd != null) {
            return dlltsd.equals(Indicator.True)? 1. : 0.;
            }
            else return 0.;
        case Dlltsd_L1:
            if(dlltsd_lag1 != null) {
            return dlltsd_lag1.equals(Indicator.True)? 1. : 0.;
            }
            else return 0.;
        case FertilityRate:
//			log.debug("fertility");
            if ( ioFlag ) {
                if (getYear() < DecisionParams.FERTILITY_MAX_YEAR) {
                    return ((Number) Parameters.getFertilityProjectionsByYear().getValue("Value", getYear())).doubleValue();
                } else {
                    return ((Number) Parameters.getFertilityProjectionsByYear().getValue("Value", DecisionParams.FERTILITY_MAX_YEAR)).doubleValue();
                }
            } else {
                return 1000*((Number)Parameters.getFertilityRateByRegionYear().get(getRegion(), getYear())).doubleValue(); //We calculate the rate per woman, but the standard to report (and what is used in the estimates) is per 1000 hence multiplication
            }
        case Female:
//			log.debug("female");
            return dgn.equals(Gender.Female)? 1. : 0.;
        case InverseMillsRatio:
            return getInverseMillsRatio();
        case Ld_children_3under:
//			log.debug("Ld child 3 under");
            return benefitUnit.getD_children_3under_lag().ordinal();
        case Ld_children_4_12:
//			log.debug("Ld child 4-12");
            return benefitUnit.getD_children_4_12_lag().ordinal();
        case Lemployed:
//			log.debug("Lemployed");
            if(les_c4_lag1 != null) {		//Problem will null pointer exceptions for those who are inactive and then become active as their lagged employment status is null!
                return les_c4_lag1.equals(Les_c4.EmployedOrSelfEmployed)? 1. : 0.;
            } else {
                return 0.;			//A person who was not active but has become active in this year should have an employment_status_lag == null.  In this case, we assume this means 0 for the Employment regression, where Lemployed is used.
            }
        case Lnonwork:
            return (les_c4_lag1.equals(Les_c4.NotEmployed) || les_c4_lag1.equals(Les_c4.Retired))? 1.: 0.;
        case Lstudent:
//			log.debug("Lstudent");
            return les_c4_lag1.equals(Les_c4.Student)? 1. : 0.;
        case Lunion:
//			log.debug("Lunion");
            return household_status_lag.equals(Household_status.Couple)? 1. : 0.;
        case Les_c3_Student_L1:
            return (Les_c4.Student.equals(les_c4_lag1)) ? 1.0 : 0.0;
        case Les_c3_NotEmployed_L1:
            return ((Les_c4.NotEmployed.equals(les_c4_lag1)) || (Les_c4.Retired.equals(les_c4_lag1))) ? 1.0 : 0.0;
        case Les_c3_Employed_L1:
            return (Les_c4.EmployedOrSelfEmployed.equals(les_c4_lag1)) ? 1.0 : 0.0;
        case Les_c3_Sick_L1:
            if (dlltsd_lag1 != null) {
                return dlltsd_lag1.equals(Indicator.True)? 1. : 0.;
            }
            else {return 0.0; }
        case Lessp_c3_Student_L1:
            if (partner != null && partner.les_c4_lag1 != null) {
                return partner.les_c4_lag1.equals(Les_c4.Student)? 1. : 0.;
            } else { return 0.;}
        case Lessp_c3_NotEmployed_L1:
            if (partner != null && partner.les_c4_lag1 != null) {
                return (partner.les_c4_lag1.equals(Les_c4.NotEmployed) || partner.les_c4_lag1.equals(Les_c4.Retired))? 1. : 0.;
            } else { return 0.;}
        case Lessp_c3_Sick_L1:
            if (partner != null && partner.dlltsd_lag1 != null) {
                return partner.dlltsd_lag1.equals(Indicator.True)? 1. : 0.;
            } else { return 0.;}
        case Retired:
            if (les_c4 != null) {
                return les_c4.equals(Les_c4.Retired)? 1. : 0.;
            } else return 0.;
        case Lesdf_c4_EmployedSpouseNotEmployed_L1: 					//Own and partner's activity status lag(1)
            if(lesdf_c4_lag1 != null) {
                return lesdf_c4_lag1.equals(Lesdf_c4.EmployedSpouseNotEmployed)? 1. : 0.;
            } else {
                return 0.;
            }
        case Lesdf_c4_NotEmployedSpouseEmployed_L1:
            if(lesdf_c4_lag1 != null) {
                return lesdf_c4_lag1.equals(Lesdf_c4.NotEmployedSpouseEmployed)? 1. : 0.;
            } else {
                return 0.;
            }
        case Lesdf_c4_BothNotEmployed_L1:
            if(lesdf_c4_lag1 != null) {
                return lesdf_c4_lag1.equals(Lesdf_c4.BothNotEmployed)? 1. : 0.;
            } else {
                return 0.;
            }
        case Liwwh:
            return (double) liwwh;
        case NumberChildren:
            return (double) benefitUnit.getChildren().size();
        case NumberChildren_2under:
            int count = benefitUnit.getN_children_0() + benefitUnit.getN_children_1() + benefitUnit.getN_children_2();
            return (double) count;
        case OtherIncome:			// "Other income corresponds to other benefitUnit incomes divided by 10,000." (From Bargain et al. (2014).  From employment selection equation.
            return 0.;				// Other incomes "correspond to partner's and other family members' income as well as capital income of various sources."
        case Parents:
            return household_status.equals(Household_status.Parents)? 1. : 0.;
        case ResStanDev:        //Draw from standard normal distribution will be multiplied by the value in the .xls file, which represents the standard deviation
            //If model.addRegressionStochasticComponent set to true, return a draw from standard normal distribution, if false return 0.
            return (model.addRegressionStochasticComponent) ? resStanDevInnov.nextGaussian() : 0.0;
        case Single:
            return household_status.equals(Household_status.Single)? 1. : 0.;
        case Single_kids:		//TODO: Is this sufficient, or do we need to take children aged over 12 into account as well?
            if(household_status.equals(Household_status.Single)) {
                if(benefitUnit.getChildren().size()>0) {			//XXX: Perhaps we need to check that children is not null first, if children is only initialised when new born persons are added to the children set.
                    return 1.;
                }
                else return 0.;
            }
            else return 0.;
        case Union:
            return household_status.equals(Household_status.Couple)? 1. : 0.;
        case Union_kids:		//TODO: Is this sufficient, or do we need to take children aged over 12 into account as well?
            if(household_status.equals(Household_status.Couple)) {
                if(benefitUnit.getChildren().size()>0) {			//XXX: Perhaps we need to check that children is not null first, if children is only initialised when new born persons are added to the children set.
                    return 1.;
                }
                else return 0.;
            }
            else return 0.;
        case Year:
            return (Parameters.isFixTimeTrend && getYear() >= Parameters.timeTrendStopsIn) ? (double) Parameters.timeTrendStopsIn : (double) getYear();
        case Year_transformed:
            return (Parameters.isFixTimeTrend && getYear() >= Parameters.timeTrendStopsIn) ? (double) Parameters.timeTrendStopsIn - 2000 : (double) getYear() - 2000;
        case Year_transformed_monetary:
            return (double) model.getTimeTrendStopsInMonetaryProcesses() - 2000; //Note: this returns base price year - 2000 (e.g. 17 for 2017 as base price year) and monetary variables are then uprated from 2017 level to the simulated year
        case Ydses_c5_Q2_L1:
            return (Ydses_c5.Q2.equals(getYdses_c5_lag1())) ? 1.0 : 0.0;
        case Ydses_c5_Q3_L1:
            return (Ydses_c5.Q3.equals(getYdses_c5_lag1())) ? 1.0 : 0.0;
        case Ydses_c5_Q4_L1:
            return (Ydses_c5.Q4.equals(getYdses_c5_lag1())) ? 1.0 : 0.0;
        case Ydses_c5_Q5_L1:
            return (Ydses_c5.Q5.equals(getYdses_c5_lag1())) ? 1.0 : 0.0;
        case Ypnbihs_dv_L1:
            if(ypnbihs_dv_lag1 != null) {
                return ypnbihs_dv_lag1;
            } else {
                throw new RuntimeException("call to uninitialised ypnbihs_dv_lag1 in Person");
            }
        case Ypnbihs_dv_L1_sq:
            if(ypnbihs_dv_lag1 != null) {
                return ypnbihs_dv_lag1*ypnbihs_dv_lag1;
            } else {
                throw new RuntimeException("call to uninitialised ypnbihs_dv_lag1 in Person");
            }
        case Ynbcpdf_dv_L1:
            return (ynbcpdf_dv_lag1 != null) ? ynbcpdf_dv_lag1 : 0.0;
        case Yptciihs_dv_L1:
            return yptciihs_dv_lag1;
        case Yptciihs_dv_L2:
            return yptciihs_dv_lag2;
        case Yptciihs_dv_L3:
            return yptciihs_dv_lag3;
        case Ypncp_L1:
            return ypncp_lag1;
        case Ypncp_L2:
            return ypncp_lag2;
        case Ypnoab_L1:
            return ypnoab_lag1;
        case Ypnoab_L2:
            return ypnoab_lag2;
        case Yplgrs_dv_L1:
            return yplgrs_dv_lag1;
        case Yplgrs_dv_L2:
            return yplgrs_dv_lag2;
        case Yplgrs_dv_L3:
            return yplgrs_dv_lag3;
        case Ld_children_3underIT:
            return model.getCountry().equals(Country.IT) ? benefitUnit.getD_children_3under_lag().ordinal() : 0.;
        case Ld_children_4_12IT:
            return model.getCountry().equals(Country.IT) ? benefitUnit.getD_children_4_12_lag().ordinal() : 0.;
        case LunionIT:
            return (household_status_lag.equals(Household_status.Couple) && (getRegion().toString().startsWith(Country.IT.toString())))? 1. : 0.;
        case EduMediumIT:
            return (deh_c3.equals(Education.Medium) && (getRegion().toString().startsWith(Country.IT.toString())))? 1. : 0.;
        case EduHighIT:
            return (deh_c3.equals(Education.High) && (getRegion().toString().startsWith(Country.IT.toString())))? 1. : 0.;

        case Reached_Retirement_Age:
            int retirementAge;
            if (dgn.equals(Gender.Female)) {
                retirementAge = Parameters.getFixedRetireAge(getYear(), Gender.Female);
            } else {
                retirementAge = Parameters.getFixedRetireAge(getYear(), Gender.Male);
            }
            return (dag >= retirementAge)? 1. : 0.;
        case Reached_Retirement_Age_Sp:
            int retirementAgePartner;
            if (partner != null) {
                if (partner.dgn.equals(Gender.Female)) {
                    retirementAgePartner = Parameters.getFixedRetireAge(getYear(), Gender.Female);
                } else {
                    retirementAgePartner = Parameters.getFixedRetireAge(getYear(), Gender.Male);
                }
                return (partner.dag >= retirementAgePartner)? 1. : 0.;
            } else {return 0.;}
        case Reached_Retirement_Age_Les_c3_NotEmployed_L1: //Reached retirement age and was not employed in the previous year
            if (dgn.equals(Gender.Female)) {
                retirementAge = Parameters.getFixedRetireAge(getYear(), Gender.Female);
            } else {
                retirementAge = Parameters.getFixedRetireAge(getYear(), Gender.Male);
            }
            return ((dag >= retirementAge) && (les_c4_lag1.equals(Les_c4.NotEmployed) || les_c4_lag1.equals(Les_c4.Retired)))? 1. : 0.;
        case EquivalisedIncomeYearly:
            return getBenefitUnit().getEquivalisedDisposableIncomeYearly();
        case EquivalisedConsumptionYearly:
            if (yearlyEquivalisedConsumption != null) {
                return yearlyEquivalisedConsumption;
            }
            else return -9999.99;
        case sIndex:
            return getsIndex();
        case sIndexNormalised:
            return getsIndexNormalised();

        //New enums for the mental health Step 1 and 2:
        case EmployedToUnemployed:
            return (les_c4_lag1.equals(Les_c4.EmployedOrSelfEmployed) && les_c4.equals(Les_c4.NotEmployed) && dlltsd.equals(Indicator.False))? 1. : 0.;
        case UnemployedToEmployed:
            return (les_c4_lag1.equals(Les_c4.NotEmployed) && dlltsd_lag1.equals(Indicator.False) && les_c4.equals(Les_c4.EmployedOrSelfEmployed))? 1. : 0.;
        case PersistentUnemployed:
            return (les_c4.equals(Les_c4.NotEmployed) && les_c4_lag1.equals(Les_c4.NotEmployed) && dlltsd.equals(Indicator.False) && dlltsd_lag1.equals(Indicator.False))? 1. : 0.;
        case NonPovertyToPoverty:
            if (benefitUnit.getAtRiskOfPoverty_lag1() != null) {
                return (benefitUnit.getAtRiskOfPoverty_lag1() == 0 && benefitUnit.getAtRiskOfPoverty() == 1)? 1. : 0.;
            } else return 0.;
        case PovertyToNonPoverty:
            if (benefitUnit.getAtRiskOfPoverty_lag1() != null) {
                return (benefitUnit.getAtRiskOfPoverty_lag1() == 1 && benefitUnit.getAtRiskOfPoverty() == 0)? 1. : 0.;
            } else return 0.;
        case PersistentPoverty:
            if (benefitUnit.getAtRiskOfPoverty_lag1() != null) {
                return (benefitUnit.getAtRiskOfPoverty_lag1() == 1 && benefitUnit.getAtRiskOfPoverty() == 1)? 1. : 0.;
            } else return 0.;
        case RealIncomeChange:
            return (benefitUnit.getYearlyChangeInLogEDI());
        case RealIncomeDecrease_D:
            return (benefitUnit.isDecreaseInYearlyEquivalisedDisposableIncome())? 1. : 0.;
        case D_Econ_benefits:
            return isReceivesBenefitsFlag_L1()? 1. : 0.;
        case D_Home_owner:
            return getBenefitUnit().isDhh_owned()? 1. : 0.; // Evaluated at the level of a benefit unit. If required, can be changed to individual-level homeownership status.
        case Covid_2020_D:
            return (getYear() == 2020)? 1. : 0.;
        case Covid_2021_D:
            return (getYear() == 2021)? 1. : 0.;
        case Pt:
            return (getLabourSupplyHoursWeekly() > 0 && getLabourSupplyHoursWeekly() < Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)? 1. : 0.;
        case L1_log_hourly_wage:
            if (L1_fullTimeHourlyEarningsPotential > 0) {
                return Math.log(L1_fullTimeHourlyEarningsPotential);
            } else {
                throw new RuntimeException("call to evaluate lag potential hourly earnings before initialisation");
            }
        case L1_log_hourly_wage_sq:
            if (L1_fullTimeHourlyEarningsPotential > 0) {
                return Math.pow(Math.log(L1_fullTimeHourlyEarningsPotential), 2);
            } else {
                throw new RuntimeException("call to evaluate lag potential hourly earnings before initialisation");
            }
        case L1_hourly_wage:
            if (L1_fullTimeHourlyEarningsPotential > 0) {
                return L1_fullTimeHourlyEarningsPotential;
            } else {
                throw new RuntimeException("call to evaluate lag potential hourly earnings before initialisation");
            }
        case Deh_c3_Low_Dag:
            return (Education.Low.equals(deh_c3)) ? dag : 0.0;
        case Deh_c3_Medium_Dag:
            return (Education.Medium.equals(deh_c3)) ? dag : 0.0;
        //Italy
        case ITC:
            return (getRegion().equals(Region.ITC)) ? 1. : 0.;
        case ITF:
            return (getRegion().equals(Region.ITF)) ? 1. : 0.;
        case ITG:
            return (getRegion().equals(Region.ITG)) ? 1. : 0.;
        case ITH:
            return (getRegion().equals(Region.ITH)) ? 1. : 0.;
        case ITI:
            return (getRegion().equals(Region.ITI)) ? 1. : 0.;
        //UK
        case UKC:
            return Region.UKC.equals(getRegion()) ? 1.0 : 0.0;
        case UKD:
            return Region.UKD.equals(getRegion()) ? 1.0 : 0.0;
        case UKE:
            return Region.UKE.equals(getRegion()) ? 1.0 : 0.0;
        case UKF:
            return Region.UKF.equals(getRegion()) ? 1.0 : 0.0;
        case UKG:
            return Region.UKG.equals(getRegion()) ? 1.0 : 0.0;
        case UKH:
            return Region.UKH.equals(getRegion()) ? 1.0 : 0.0;
        case UKI:
            return Region.UKI.equals(getRegion()) ? 1.0 : 0.0;
        case UKJ:
            return Region.UKJ.equals(getRegion()) ? 1.0 : 0.0;
        case UKK:
            return Region.UKK.equals(getRegion()) ? 1.0 : 0.0;
        case UKL:
            return Region.UKL.equals(getRegion()) ? 1.0 : 0.0;
        case UKM:
            return Region.UKM.equals(getRegion()) ? 1.0 : 0.0;
        case UKN:
            return Region.UKN.equals(getRegion()) ? 1.0 : 0.0;
        case UKmissing:
            return 0.;		//For our purpose, all our simulated people have a region, so this enum value is always going to be 0 (false).
//			return (getRegion().equals(Region.UKmissing)) ? 1. : 0.;		//For people whose region info is missing.  The UK survey did not record the region in the first two waves (2006 and 2007, each for 4 years). For all those individuals we have gender, education etc but not region. If we exclude them we lose a large part of the UK sample, so this is the trick to keep them in the estimates.

        // Regressors used in the Covid-19 labour market module below:
        case Dgn_Dag:
            if(dgn.equals(Gender.Male)) {
                return (double) dag;
            }
            else return 0.;
        case Employmentsonfullfurlough:
            return Parameters.getEmploymentsFurloughedFullForMonthYear(model.getLabourMarket().getCovid19TransitionsMonth(), getYear());
        case Employmentsonflexiblefurlough:
            return Parameters.getEmploymentsFurloughedFlexForMonthYear(model.getLabourMarket().getCovid19TransitionsMonth(), getYear());
        case CovidTransitionsMonth:
            model.getLabourMarket().getMonthForRegressor();
        case Lhw_L1:
            if (getNewWorkHours_lag1() != null) {
                return getNewWorkHours_lag1();
            }
            else return 0.;
        case Dgn_Lhw_L1:
            if (getNewWorkHours_lag1() != null && dgn.equals(Gender.Male)) {
                return getNewWorkHours_lag1();
            }
            else return 0.;
        case Covid19GrossPayMonthly_L1:
            return getCovidModuleGrossLabourIncome_lag1();
        case Covid19ReceivesSEISS_L1:
            return (getCovidModuleReceivesSEISS().equals(Indicator.True)) ? 1. : 0.;
        case Les_c7_Covid_Furlough_L1:
            return (getLes_c7_covid_lag1().equals(Les_c7_covid.FurloughedFlex) || getLes_c7_covid_lag1().equals(Les_c7_covid.FurloughedFull)) ? 1. : 0.;
        case Blpay_Q2:
            return (getCovidModuleGrossLabourIncomeBaseline_Xt5().equals(Quintiles.Q2)) ? 1. : 0.;
        case Blpay_Q3:
            return (getCovidModuleGrossLabourIncomeBaseline_Xt5().equals(Quintiles.Q3)) ? 1. : 0.;
        case Blpay_Q4:
            return (getCovidModuleGrossLabourIncomeBaseline_Xt5().equals(Quintiles.Q4)) ? 1. : 0.;
        case Blpay_Q5:
            return (getCovidModuleGrossLabourIncomeBaseline_Xt5().equals(Quintiles.Q5)) ? 1. : 0.;
        case Dgn_baseline:
            return 0.;
        case RealWageGrowth: // Note: the values provided to the wage regression must be rebased to 2015, the default BASE_PRICE_YEAR.
            return Parameters.getTimeSeriesIndex(getYear(), UpratingCase.Earnings);
        case RealGDPGrowth:
            return Parameters.getTimeSeriesIndex(getYear(), UpratingCase.Capital);
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
            throw new IllegalArgumentException("Unsupported regressor " + variableID.name() + " in Person#getDoubleValue");
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
        if (!(o instanceof Person)) {
            return false;
        }

        Person p = (Person) o;

        boolean idIsEqual = new EqualsBuilder()
                .append(key.getId(), p.key.getId())		//Add more fields to compare to check for equality if desired
                .isEquals();

        return idIsEqual;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(key.getId())
                .toHashCode();
    }


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

     public int getPersonCount() {
        return 1;
     }

    public int getDag() {
        return dag;
    }

    public void setDag(Integer dag) {
        this.dag = dag;
        this.dag_sq = dag*dag;
    }

    public Gender getDgn() {
        return dgn;
    }

    public int getGender() {
         if (dgn == Gender.Male) return 0;
         else return 1;
    }

    public void setDgn(Gender dgn) {
        this.dgn = dgn;
    }

    public Les_c4 getLes_c4() {
        return les_c4;
    }

    public int getStudent()
    {
        if (les_c4 != null) {
            return les_c4.equals(Les_c4.Student)? 1 : 0;
        }
        else {
            return 0;
        }
    }

    public int getEducation() {

        if (deh_c3==Education.Medium) return 1;
        else if (deh_c3==Education.High) return (int)(DecisionParams.PTS_EDUCATION - 1.0);
        else return 0;
    }

    public void setLes_c4(Les_c4 les_c4) {
        this.les_c4 = les_c4;
    }

    public void setLes_c7_covid(Les_c7_covid les_c7_covid) { this.les_c7_covid = les_c7_covid; }

    public Les_c7_covid getLes_c7_covid() { return les_c7_covid; }

    public Les_c4 getLes_c4_lag1() {
        return les_c4_lag1;
    }

    public Les_c7_covid getLes_c7_covid_lag1() { return les_c7_covid_lag1; }

    public void setLes_c7_covid_lag1(Les_c7_covid les_c7_covid_lag1) {
         this.les_c7_covid_lag1 = les_c7_covid_lag1;
    }

    public Les_c4 getLessp_c4() {
        return lessp_c4;
    }

    public void setLessp_c4(Les_c4 lessp_c4) {
        this.lessp_c4 = lessp_c4;
    }

    public Les_c4 getActivity_status_partner_lag() {
        return activity_status_partner_lag;
    }

    public void setActivity_status_partner_lag(Les_c4 activity_status_partner_lag) {
        this.activity_status_partner_lag = activity_status_partner_lag;
    }

    public Household_status getHousehold_status() {
        return household_status;
    }

    public void setHousehold_status(Household_status household_status) {
        if(this.household_status != null) {
            if(household_status.equals(Household_status.Parents) && (this.household_status.equals(Household_status.Couple) || this.household_status.equals(Household_status.Single))) {
                throw new IllegalArgumentException("Person with household_status as couple or single cannot move back to the parental home!");
            }
        }
        this.household_status = household_status;
    }

    public int getCohabiting() {
        if(household_status != null) {
            return household_status.equals(Household_status.Couple)? 1 : 0;
        }
        else {
            return 0;
        }
    }

    public Education getDeh_c3() {
        return deh_c3;
    }

    public void setDeh_c3(Education deh_c3) {this.deh_c3 = deh_c3;}

    public void setDeh_c3_lag1(Education deh_c3_lag1) {this.deh_c3_lag1 = deh_c3_lag1;}

    public Education getDehm_c3() {
        return dehm_c3;
    }

    public void setDehm_c3(Education dehm_c3) {
        this.dehm_c3 = dehm_c3;
    }

    public Education getDehf_c3() {
        return dehf_c3;
    }

    public void setDehf_c3(Education dehf_c3) {
        this.dehf_c3 = dehf_c3;
    }

    public Education getEducation_partner() {
        return dehsp_c3;
    }

    public void setEducation_partner(Education education_partner) {
        this.dehsp_c3 = education_partner;
    }

    public Indicator getDed() {
        return ded;
    }

    public void setDed(Indicator ded) {
        this.ded = ded;
    }

    public int getLowEducation() {
        if(deh_c3 != null) {
            if (deh_c3.equals(Education.Low)) return 1;
            else return 0;
        }
        else {
            return 0;
        }
    }

    public int getMidEducation() {
        if(deh_c3 != null) {
            if (deh_c3.equals(Education.Medium)) return 1;
            else return 0;
        }
        else {
            return 0;
        }
    }

    public int getHighEducation() {
        if(deh_c3 != null) {
            if (deh_c3.equals(Education.High)) return 1;
            else return 0;
        }
        else {
            return 0;
        }
    }

    public void setEducation(Education educationlevel) {
        this.deh_c3 = educationlevel;
    }

    public int getGoodHealth() {
        if(dlltsd != null && !dlltsd.equals(Indicator.True)) { //Good / bad health depends on dlltsd (long-term sick or disabled). If true, then person is in bad health.
            return 1;
        }
        else return 0;
    }

    public int getBadHealth() {
        if(dlltsd != null && dlltsd.equals(Indicator.True)) {
            return 1;
        }
        else return 0;
    }

    /*
     * In the initial population, there is continuous health score and an indicator for long-term sickness or disability. In EUROMOD to which we match, health is either
     * Good or Poor. This method checks the Dlltsd indicator and returns corresponding HealthStatus to use in matching the EUROMOD donor.
     */
    public HealthStatus getHealthStatusConversion() {
        if(dlltsd != null && dlltsd.equals(Indicator.True)) {
            return HealthStatus.Poor; //If long-term sick or disabled, return Poor HealthStatus
        }
        else return HealthStatus.Good; //Otherwise, return Good HealthStatus
    }

    public int getEmployed() {
        if(les_c4.equals(Les_c4.EmployedOrSelfEmployed) ) {
            return 1;
        }
        else return 0;
    }

    public int getNonwork() {
        if(les_c4.equals(Les_c4.NotEmployed)) {
            return 1;
        }
        else return 0;
    }

    public Region getRegion() {
        if (benefitUnit != null) {
            return benefitUnit.getRegion();
        } else {
            return regionLocal;
        }
    }

    public void setRegion(Region region) {
        this.benefitUnit.setRegion(region);
    }

    public Household_status getHousehold_status_lag() {
        return household_status_lag;
    }

//	public double getDeviationFromMeanRetirementAge() {
//		return deviationFromMeanRetirementAge;
//	}

    public boolean isToGiveBirth() {
        return toGiveBirth;
    }

    public void setToGiveBirth(boolean toGiveBirth_) {
            toGiveBirth = toGiveBirth_;
    }

    public boolean isToLeaveSchool() {
        return toLeaveSchool;
    }

    public void setToLeaveSchool(boolean toLeaveSchool) {
        this.toLeaveSchool = toLeaveSchool;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public BenefitUnit getBenefitUnit() {
        if (benefitUnit == null) {
            return null;
        } else {
            return benefitUnit;
        }
    }

    public void setBenefitUnit(BenefitUnit newBenefitUnit) {

        if (newBenefitUnit == null) {

            benefitUnit = null;
            idBenefitUnit = null;
            idHousehold = null;
        } else {

            if ( benefitUnit != null ) {
                benefitUnit.removePerson(this);
            }
            benefitUnit = newBenefitUnit;
            idBenefitUnit = newBenefitUnit.getKey().getId();
            idHousehold = newBenefitUnit.getIdHousehold();
            if (!benefitUnit.getPersonsInBU().contains(this)) {
                if (dag < Parameters.AGE_TO_BECOME_RESPONSIBLE && idPartner == null) {
                    benefitUnit.addChild(this);
                } else if (dag >= Parameters.AGE_TO_BECOME_RESPONSIBLE){
                    benefitUnit.addResponsiblePerson(this);
                }
            }
        }
    }

    public Person getPartner() {
        return partner;
    }

    public void setPartner(Person partner) {
        this.partner = partner;
        if(partner == null) {
            idPartner = null;
        } else {
            this.idPartner = partner.getKey().getId();		//Update partnerId to ensure consistency
        }
    }

    public Long getIdPartner() {
        return idPartner;
    }

    public Long getIdPartnerLag1() {
        return idPartnerLag1;
    }

    public Long getIdBenefitUnit() {
        return idBenefitUnit;
    }

    public Labour getLabourSupplyWeekly() {
        return labourSupplyWeekly;
    }

    public double getDoubleLabourSupplyWeeklyHours() {
        return (labourSupplyWeekly != null) ? (double)labourSupplyWeekly.getHours(this) : 0.0;
    }

    public int getLabourSupplyHoursWeekly() {
        return (labourSupplyWeekly != null) ? labourSupplyWeekly.getHours(this) : 0;
    }

    public void setLabourSupplyWeekly(Labour labourSupply) {
        labourSupplyWeekly = labourSupply;
    }

    public double getLabourSupplyYearly() {
        return (double) getLabourSupplyWeekly().getHours(this) * Parameters.WEEKS_PER_YEAR;
    } //TODO: add scaling factor multiplication?

    public double getScaledLabourSupplyYearly() {
        return (double) getLabourSupplyWeekly().getHours(this) * Parameters.WEEKS_PER_YEAR * model.getScalingFactor();
    }


    public double getGrossEarningsWeekly() {
        return fullTimeHourlyEarningsPotential * (double) getLabourSupplyHoursWeekly();
    }

    public double getGrossEarningsYearly() {
        Double gew = getGrossEarningsWeekly();
        if(Double.isFinite(gew) && gew > 0.) {
            return gew * Parameters.WEEKS_PER_YEAR;
        }
        else return 0.;
//		else return null;
    }

    public int getAtRiskOfPoverty() {
        return benefitUnit.getAtRiskOfPoverty();
    }

    public double getFullTimeHourlyEarningsPotential() {
        return fullTimeHourlyEarningsPotential;
    }

    public double getDesiredAgeDiff() {
        return desiredAgeDiff;
    }

    public double getDesiredEarningsPotentialDiff() {
        return desiredEarningsPotentialDiff;
    }

    public Long getIdMother() {
        return idMother;
    }

    public void setIdMother(Long id_mother) {
        idMother = id_mother;
    }

    public void setIdMother(Person mother) {
        if (mother == null) {
            idMother = null;
        } else {
            idMother = mother.getKey().getId();
        }
    }
    public Long getIdFather() {
        return idFather;
    }

    public void setIdFather(Long id_father) {
        idFather = id_father;
    }

    public void setIdFather(Person father) {
        if (father == null) {
            idFather = null;
        } else {
            idFather = father.getKey().getId();
        }
    }
    public boolean isResponsible() {
        boolean responsible;
        if(dgn.equals(Gender.Female)) {
            Person female = benefitUnit.getFemale();
            responsible = female != null && female == this;
        }
        else {
            Person male = benefitUnit.getMale();
            responsible = male != null && male == this;
        }
        return responsible;
    }

    public boolean isChild() {
        return benefitUnit.getChildren().contains(this);
    }


    public void orphanGiveParent() {
        if(dag < Parameters.AGE_TO_BECOME_RESPONSIBLE && idMother == null && idFather == null) {		//Check if orphan
            Person adoptedMother = benefitUnit.getFemale();
            if(adoptedMother != null) {
                idMother = adoptedMother.getKey().getId();
            }
            else {
                idFather = benefitUnit.getMale().getKey().getId();		//Adopted father
            }
        }
        else throw new IllegalArgumentException("ERROR - orphanGiveParent method has been called on a non-orphan!");
    }
    /*
    public double getUnitLabourCost() {
        return unitLabourCost;
    }

    public void setUnitLabourCost(double unitLabourCost) {
        this.unitLabourCost = unitLabourCost;
    }
    */
    public Dhe getDhe() {
        return dhe;
    }

    public void setDhe(Dhe health) {
        this.dhe = health;
    }

    public double getDheValue() {
        return dhe.getValue();
    }
    public double getDhm() {
        double val;
        if (dhm == null) {
            val = -1.0;
        } else {
            val = dhm;
        }
        return val;
    }

    public void setCareFromOther(boolean val) {
        careFromOther = val;
    }

    public void setCareHoursFromOtherWeekly_lag1(double val) {
        careHoursFromOtherWeekly_lag1 = val;
    }

    public void setCareHoursFromFormalWeekly_lag1(double val) {
        careHoursFromFormalWeekly_lag1 = val;
    }

    public void setDhm(Double dhm) {
        this.dhm = dhm;
    }

    public void setDhe_lag1(Dhe health) {
        this.dhe_lag1 = health;
    }

    public void setDhm_lag1(Double dhm) {
        this.dhm_lag1 = dhm;
    }

    public boolean getDhm_ghq() {
        return dhm_ghq;
    }

    public void setDhm_ghq(boolean dhm_ghq) {
        this.dhm_ghq = dhm_ghq;
    }

    public Indicator getNeedSocialCare() {
        return needSocialCare;
    }

    public void setNeedSocialCare(Indicator needSocialCare) {
        this.needSocialCare = needSocialCare;
    }

    public Indicator getDer() {
        return der;
    }

    public void setDer(Indicator der) {
        this.der = der;
    }

    public Long getId_original() {
        return id_original;
    }

    public Long getId_bu_original() {
        return id_bu_original;
    }

    public int getAgeGroup() {
        return ageGroup;
    }

    public boolean isClonedFlag() {
        return clonedFlag;
    }

    public void setClonedFlag(boolean clonedFlag) {
        this.clonedFlag = clonedFlag;
    }

    public int getOriginalNumberChildren() {
        return originalNumberChildren;
    }

    public Household_status getOriginalHHStatus() {
        return originalHHStatus;
    }

    public Dcpst getDcpst() {
        return dcpst;
    }

    public void setDcpst(Dcpst dcpst) {
        this.dcpst = dcpst;
    }

    public Indicator getDcpen() {
        return dcpen;
    }

    public void setDcpen(Indicator dcpen) {
        this.dcpen = dcpen;
    }

    public Indicator getDcpex() {
        return dcpex;
    }

    public void setDcpex(Indicator dcpex) {
        this.dcpex = dcpex;
    }

    public Indicator getDlltsd() {
        return dlltsd;
    }
    public void setDlltsd(Indicator dlltsd) {
        this.dlltsd = dlltsd;
    }

    public void setSocialCareMarketAll(SocialCareMarketAll market) {
        this.socialCareMarketAll = market;
    }

    public Indicator getDlltsd_lag1() {
        return dlltsd_lag1;
    }

    public void setDlltsd_lag1(Indicator dlltsd_lag1) {
        this.dlltsd_lag1 = dlltsd_lag1;
    }

    public Indicator getSedex() {
        return sedex;
    }

    public void setSedex(Indicator sedex) {
        this.sedex = sedex;
    }

    public boolean isLeftEducation() {
        return leftEducation;
    }

    public void setLeftEducation(boolean leftEducation) {
        this.leftEducation = leftEducation;
    }

    public boolean isLeftPartnership() {
        return leftPartnership;
    }

    public void setLeftPartnership(boolean leftPartnership) {
        this.leftPartnership = leftPartnership;
    }

    public Indicator getPartnership_samesex() {
        return partnership_samesex;
    }

    public void setPartnership_samesex(Indicator partnership_samesex) {
        this.partnership_samesex = partnership_samesex;
    }

    public Indicator getWomen_fertility() {
        return women_fertility;
    }

    public void setWomen_fertility(Indicator women_fertility) {
        this.women_fertility = women_fertility;
    }

    public Indicator getEducation_inrange() {
        return education_inrange;
    }

    public void setEducation_inrange(Indicator education_inrange) {
        this.education_inrange = education_inrange;
    }

    public Integer getDcpyy() {
        return dcpyy;
    }

    public void setDcpyy(Integer dcpyy) {
        this.dcpyy = dcpyy;
    }

    public Integer getDcpyy_lag1() {
        return dcpyy_lag1;
    }

    public Integer getDcpagdf() {
        return dcpagdf;
    }

    public void setDcpagdf(Integer dcpagdf) {
        this.dcpagdf = dcpagdf;
    }


    public Integer getDcpagdf_lag1() {
        return dcpagdf_lag1;
    }

    public Double getYpnbihs_dv() {
        return ypnbihs_dv;
    }

    public void setYpnbihs_dv(Double val) {
        ypnbihs_dv = val;
    }

    public Double getYpnbihs_dv_lag1() {
        return ypnbihs_dv_lag1;
    }

    public double getYptciihs_dv() {
        return yptciihs_dv;
    }

    public double getYpncp() {
        return ypncp;
    }

    public double getYpnoab() {
        return ypnoab;
    }

    public void setYptciihs_dv(double yptciihs_dv) {
        this.yptciihs_dv = yptciihs_dv;
        if (Double.isNaN(this.yptciihs_dv) || Double.isInfinite(this.yptciihs_dv)) throw new IllegalArgumentException("yptciihs_dv is not finite");
    }

    public double getYptciihs_dv_lag1() {
        return yptciihs_dv_lag1;
    }

    public double getYplgrs_dv() {
        return yplgrs_dv;
    }

    public void setYplgrs_dv(double val) {
        yplgrs_dv = val;
    }

    public double getYplgrs_dv_lag1() {
        return yplgrs_dv_lag1;
    }

    public double getYplgrs_dv_lag2() {
        return yplgrs_dv_lag2;
    }

    public double getYplgrs_dv_lag3() {
        return yplgrs_dv_lag3;
    }

    public Double getYnbcpdf_dv() {
        return ynbcpdf_dv;
    }

    public void setYnbcpdf_dv(Double ynbcpdf_dv) {
        this.ynbcpdf_dv = ynbcpdf_dv;
    }

    public Double getYnbcpdf_dv_lag1() {
        return ynbcpdf_dv_lag1;
    }

    public Lesdf_c4 getLesdf_c4() {
        return lesdf_c4;
    }

    public Lesdf_c4 getLesdf_c4_lag1() {
        return lesdf_c4_lag1;
    }

    public void setLes_c4_lag1(Les_c4 les_c4_lag1) {
        this.les_c4_lag1 = les_c4_lag1;
    }

    public void setLesdf_c4_lag1(Lesdf_c4 lesdf_c4_lag1) {
        this.lesdf_c4_lag1 = lesdf_c4_lag1;
    }

    public void setYpnbihs_dv_lag1(Double ynbcpdf_dv_lag1) {
        this.ynbcpdf_dv_lag1 = ynbcpdf_dv_lag1;
    }

    public void setDehsp_c3_lag1(Education dehsp_c3_lag1) {
        this.dehsp_c3_lag1 = dehsp_c3_lag1;
    }

    public void setDhesp_lag1(Dhe dhesp_lag1) {
        this.dhesp_lag1 = dhesp_lag1;
    }

    public void setYnbcpdf_dv_lag1(Double ynbcpdf_dv_lag1) {
        this.ynbcpdf_dv_lag1 = ynbcpdf_dv_lag1;
    }

    public void setDcpyy_lag1(Integer dcpyy_lag1) {
        this.dcpyy_lag1 = dcpyy_lag1;
    }

    public void setDcpagdf_lag1(Integer dcpagdf_lag1) {
        this.dcpagdf_lag1 = dcpagdf_lag1;
    }

    public void setDcpst_lag1(Dcpst dcpst_lag1) {
        this.dcpst_lag1 = dcpst_lag1;
    }

    public void setFullTimeHourlyEarningsPotential(double potentialHourlyEarnings) {
        this.fullTimeHourlyEarningsPotential = potentialHourlyEarnings;
    }

    public double getL1_fullTimeHourlyEarningsPotential() {
        return L1_fullTimeHourlyEarningsPotential;
    }

    public void setL1_fullTimeHourlyEarningsPotential(double potentialHourlyEarnings) {
        L1_fullTimeHourlyEarningsPotential = potentialHourlyEarnings;
    }


    public void setLiwwh(Integer liwwh) {
        this.liwwh = liwwh;
    }

    public void setIoFlag(boolean ioFlag) {
        this.ioFlag = ioFlag;
    }

    public boolean isToBePartnered() {
        return toBePartnered;
    }

    public void setToBePartnered(boolean toBePartnered) {
        this.toBePartnered = toBePartnered;
    }

    public int getCoupleOccupancy() {

        if(partner != null) {
            return 1;
        }
        else return 0;
    }

    public int getAdultChildFlag() {
        if (adultchildflag!= null) {
            if (adultchildflag.equals(Indicator.True)) {
                return 1;
            }
            else return 0;
        }
        else return 0;
    }

    public Person getOriginalPartner() {
        return originalPartner;
    }

    public Long getIdHousehold() {
        return idHousehold;
    }

    public void setIdHousehold(Long idHousehold) {
        // should only be called from BenefitUnit.setHousehold
        this.idHousehold = idHousehold;
    }

    public Series.Double getYearlyEquivalisedDisposableIncomeSeries() {
        return yearlyEquivalisedDisposableIncomeSeries;
    }

    public void setYearlyEquivalisedDisposableIncomeSeries(Series.Double yearlyEquivalisedDisposableIncomeSeries) {
        this.yearlyEquivalisedDisposableIncomeSeries = yearlyEquivalisedDisposableIncomeSeries;
    }

    public Double getYearlyEquivalisedConsumption() {
        return yearlyEquivalisedConsumption;
    }

    public void setYearlyEquivalisedConsumption(Double yearlyEquivalisedConsumption) {
        this.yearlyEquivalisedConsumption = yearlyEquivalisedConsumption;
    }

    public Series.Double getYearlyEquivalisedConsumptionSeries() {
        return yearlyEquivalisedConsumptionSeries;
    }

    public void setYearlyEquivalisedConsumptionSeries(Series.Double yearlyEquivalisedConsumptionSeries) {
        this.yearlyEquivalisedConsumptionSeries = yearlyEquivalisedConsumptionSeries;
    }

    /*
    public Double getsIndex() {
        if (sIndexYearMap.get(model.getYear()-model.getsIndexTimeWindow()) != null) {
            return sIndexYearMap.get(model.getYear() - model.getsIndexTimeWindow());
        } else {
            return Double.NaN;
        }
    }

    public void setsIndex(Double sIndex) {
        sIndexYearMap.put(model.getYear(), sIndex);
    }

     */

    public Double getsIndex() {
        if (sIndex != null && sIndex > 0. && !sIndex.isInfinite() && (model.getYear() >= model.getStartYear()+model.getsIndexTimeWindow())) {
            return sIndex;
        }
        else return Double.NaN;
    }

    public void setsIndex(Double sIndex) {
        this.sIndex = sIndex;
    }

    public Double getsIndexNormalised() {
        if (sIndexNormalised != null && sIndexNormalised > 0. && !sIndexNormalised.isInfinite() && (model.getYear() >= model.getStartYear()+model.getsIndexTimeWindow())) {
            return sIndexNormalised;
        }
        else return Double.NaN;
    }

    public void setsIndexNormalised(Double sIndexNormalised) {
        this.sIndexNormalised = sIndexNormalised;
    }

    public Map<Integer, Double> getsIndexYearMap() {
        return sIndexYearMap;
    }

    public Integer getNewWorkHours_lag1() {
        return newWorkHours_lag1;
    }

    public void setNewWorkHours_lag1(Integer newWorkHours_lag1) {
        this.newWorkHours_lag1 = newWorkHours_lag1;
    }

    public double getCovidModuleGrossLabourIncome_lag1() {
        return covidModuleGrossLabourIncome_lag1;
    }

    public void setCovidModuleGrossLabourIncome_lag1(double covidModuleGrossLabourIncome_lag1) {
        this.covidModuleGrossLabourIncome_lag1 = covidModuleGrossLabourIncome_lag1;
    }

    public Indicator getCovidModuleReceivesSEISS() {
        return covidModuleReceivesSEISS;
    }

    public void setCovidModuleReceivesSEISS(Indicator covidModuleReceivesSEISS) {
        this.covidModuleReceivesSEISS = covidModuleReceivesSEISS;
    }

    public double getCovidModuleGrossLabourIncome_Baseline() {
        return covidModuleGrossLabourIncome_Baseline;
    }

    public void setCovidModuleGrossLabourIncome_Baseline(double val) {
        covidModuleGrossLabourIncome_Baseline = val;
    }

    public Quintiles getCovidModuleGrossLabourIncomeBaseline_Xt5() {
        return covidModuleGrossLabourIncomeBaseline_Xt5;
    }

    public void setCovidModuleGrossLabourIncomeBaseline_Xt5(Quintiles covidModuleGrossLabourIncomeBaseline_Xt5) {
        this.covidModuleGrossLabourIncomeBaseline_Xt5 = covidModuleGrossLabourIncomeBaseline_Xt5;
    }

    public boolean isDhh_owned() {
        return dhh_owned;
    }

    public void setDhh_owned(boolean dhh_owned) {
        this.dhh_owned = dhh_owned;
    }

    public boolean isReceivesBenefitsFlag() {
        return receivesBenefitsFlag;
    }

    public void setReceivesBenefitsFlag(boolean receivesBenefitsFlag) {
        this.receivesBenefitsFlag = receivesBenefitsFlag;
    }

    public boolean isReceivesBenefitsFlag_L1() {
        return receivesBenefitsFlag_L1;
    }

    public void setReceivesBenefitsFlag_L1(boolean receivesBenefitsFlag_L1) {
        this.receivesBenefitsFlag_L1 = receivesBenefitsFlag_L1;
    }

    public double getEquivalisedDisposableIncomeYearly() {
        return benefitUnit.getEquivalisedDisposableIncomeYearly();
    }

    public double getDisposableIncomeMonthly() { return benefitUnit.getDisposableIncomeMonthly();}

    public double getWageOffer() {
        //TODO: WAGE OFFER NOT CURRENTLY WORKING
        return 1.0;
    }

    public int getDisability() {
        return (Indicator.True.equals(getDlltsd())) ? 1 : 0;
    }

    public SocialCareMarketAll getSocialCareMarketAll() {
        // market = 0 for no social care
        //          1 for only informal care
        //          2 for informal and formal care
        //          3 for only formal care
        if (getHoursFormalSocialCare()<0.01 && getHoursInformalSocialCare()<0.01)
            return SocialCareMarketAll.None;
        else if (getHoursFormalSocialCare()<0.01 && getHoursInformalSocialCare()>0.01)
            return SocialCareMarketAll.Informal;
        else if (getHoursFormalSocialCare()>0.01 && getHoursInformalSocialCare()>0.01)
            return SocialCareMarketAll.Mixed;
        else return SocialCareMarketAll.Formal;
    }

    public double getRetired() {
        return (Les_c4.Retired.equals(getLes_c4())) ? 1.0 : 0.0;
    }

    public void death() {

        if (benefitUnit == null) {
            throw new RuntimeException("simulated death of person without a benefit unit.");
        }
        benefitUnit.removePerson(this);
        model.removePerson(this);
    }
    public void setYearLocal(Integer yearLocal) {
        this.yearLocal = yearLocal;
    }
    private int getYear() {
        if (model != null) {
            return model.getYear();
        } else {
            if (yearLocal ==null) {
                throw new RuntimeException("call to get uninitialised year in benefit unit");
            }
            return yearLocal;
        }
    }
    public void setRegionLocal(Region region) {
        regionLocal = region;
    }
    public void setDhhtp_c4_lag1Local(Dhhtp_c4 dhhtp_c4_lag1) {
        dhhtp_c4_lag1Local = dhhtp_c4_lag1;
    }
    public void setYdses_c5_lag1Local(Ydses_c5 ydses_c5_lag1) {
        ydses_c5_lag1Local = ydses_c5_lag1;
    }
    public void setN_children_allAges_lag1Local(Integer n_children_allAges_lag1) {
        n_children_allAges_lag1Local = n_children_allAges_lag1;
    }
    public void setN_children_allAges_Local(Integer n_children_allAges) {
        n_children_allAges_Local = n_children_allAges;
    }
    public void setN_children_02_lag1Local(Integer n_children_02_lag1) {
        n_children_02_lag1Local = n_children_02_lag1;
    }
    public void setN_children_017Local(Integer n_children_017) {
        n_children_017Local = n_children_017;
    }
    public void setD_children_2underLocal(Indicator d_children_2under) {
        d_children_2underLocal = d_children_2under;
    }
    private Ydses_c5 getYdses_c5_lag1() {
        if (benefitUnit!=null) {
            return getBenefitUnit().getYdses_c5_lag1();
        } else {
            return ydses_c5_lag1Local;
        }
    }
    private Dhhtp_c4 getDhhtp_c4_lag1() {
        if (benefitUnit!=null) {
            return getBenefitUnit().getDhhtp_c4_lag1();
        } else {
            return dhhtp_c4_lag1Local;
        }
    }
    private Integer getN_children_allAges_lag1() {
        if (benefitUnit != null) {
            return (benefitUnit.getN_children_allAges_lag1() != null) ? benefitUnit.getN_children_allAges_lag1() : 0;
        } else {
            return n_children_allAges_lag1Local;
        }
    }
    private Integer getN_children_allAges() {
        if (benefitUnit != null) {
            return benefitUnit.getN_children_allAges(false);
        } else {
            return n_children_allAges_Local;
        }
    }
    private Integer getN_children_02_lag1() {
        if (benefitUnit != null) {
            return (benefitUnit.getN_children_02_lag1() != null) ? benefitUnit.getN_children_02_lag1() : 0;
        } else {
            return n_children_02_lag1Local;
        }
    }
    private Integer getN_children_017() {
        if (benefitUnit != null) {
            return benefitUnit.getN_children_017();
        } else {
            return n_children_017Local;
        }
    }
    private Indicator getD_children_2under() {
        if (benefitUnit != null) {
            return benefitUnit.getD_children_2under();
        } else {
            return d_children_2underLocal;
        }
    }
    private double getInverseMillsRatio() {

        double score;
        if(Gender.Male.equals(dgn)) {
            if (Les_c4.EmployedOrSelfEmployed.equals(les_c4_lag1)) {
                score = Parameters.getRegEmploymentSelectionMaleE().getScore(this, Person.DoublesVariables.class);
            } else {
                score = Parameters.getRegEmploymentSelectionMaleNE().getScore(this, Person.DoublesVariables.class);
            }
        } else {
            // for females
            if (Les_c4.EmployedOrSelfEmployed.equals(les_c4_lag1)) {
                score = Parameters.getRegEmploymentSelectionFemaleE().getScore(this, Person.DoublesVariables.class);
            } else {
                score = Parameters.getRegEmploymentSelectionFemaleNE().getScore(this, Person.DoublesVariables.class);
            }
        }
        double inverseMillsRatio; //IMR is the PDF(x) / CDF(x) where x is score of probit of employment
        double cdf = Parameters.getStandardNormalDistribution().cumulativeProbability(score);
        if(cdf != 0.) {
            String pdfString = Double.toString(Parameters.getStandardNormalDistribution().density(score));
            String cdfString = Double.toString(cdf);
            BigDecimal bigPdf = new BigDecimal(pdfString);
            BigDecimal bigCdf = new BigDecimal(cdfString);
            BigDecimal result = bigPdf.divide(bigCdf, RoundingMode.HALF_EVEN);
            inverseMillsRatio = result.doubleValue();
        } else {
            throw new RuntimeException("problem evaluating inverse Mills ratio for wage rate projections");
        }
        if(Double.isFinite(inverseMillsRatio)) {
            if(Gender.Male.equals(dgn)) {
                if(inverseMillsRatio > inverseMillsRatioMaxMale) {
                    inverseMillsRatioMaxMale = inverseMillsRatio;
                }
                if(inverseMillsRatio < inverseMillsRatioMinMale) {
                    inverseMillsRatioMinMale = inverseMillsRatio;
                }
            } else {
                if(inverseMillsRatio > inverseMillsRatioMaxFemale) {
                    inverseMillsRatioMaxFemale = inverseMillsRatio;
                }
                if(inverseMillsRatio < inverseMillsRatioMinFemale) {
                    inverseMillsRatioMinFemale = inverseMillsRatio;
                }
            }
        } else {
            log.debug("inverse Mills ratio is not finite, return 0 instead!!!   IMR: " + inverseMillsRatio + ", score: " + score/* + ", num: " + num + ", denom: " + denom*/ + ", age: " + dag + ", gender: " + dgn + ", education " + deh_c3 + ", activity_status from previous time-step " + les_c4);
            return 0.;
        }
        return inverseMillsRatio;		//XXX: Currently only returning non-zero IMR if it is finite
    }
    public void setLesdf_c4(Lesdf_c4 val) {
        lesdf_c4 = val;
    }

    public double getHourlyWageRate1() {
        return getHourlyWageRate(getLabourSupplyHoursWeekly());
    }
    public double getHourlyWageRate() {
        return getHourlyWageRate(getLabourSupplyHoursWeekly());
    }
    public double getHourlyWageRate(double labourHoursWeekly) {
        int weeklyHours = (int) Math.round(labourHoursWeekly);
        return getHourlyWageRate(weeklyHours);
    }
    public double getHourlyWageRate(int labourHoursWeekly) {

        if (labourHoursWeekly >= Parameters.MIN_HOURS_FULL_TIME_EMPLOYED) {
            return fullTimeHourlyEarningsPotential;
        } else {
            double ptPremium;
            if (les_c4_lag1.equals(Les_c4.EmployedOrSelfEmployed)) {
                if (Gender.Male.equals(dgn)) {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionNames.WagesMalesE, "Pt");
                } else {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionNames.WagesFemalesE, "Pt");
                }
            } else {
                if (Gender.Male.equals(dgn)) {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionNames.WagesMalesNE, "Pt");
                } else {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionNames.WagesFemalesNE, "Pt");
                }
            }
            return Math.exp( Math.log(fullTimeHourlyEarningsPotential) + ptPremium);
        }
    }

    public double getEarningsWeekly() {
        return getEarningsWeekly(getLabourSupplyHoursWeekly());
    }
    public double getEarningsWeekly(double labourHoursWeekly) {
        int hours = (int) Math.round(labourHoursWeekly);
        return getHourlyWageRate(hours) * labourHoursWeekly;
    }
    public double getEarningsWeekly(int labourHoursWeekly) {
        return getHourlyWageRate(labourHoursWeekly) * (double) labourHoursWeekly;
    }
    public Integer getN_children_017Local() {
        return n_children_017Local;
    }
    public Integer getN_children_allAges_Local() {
        return n_children_allAges_Local;
    }
    public int getN_children_allAges_int() {
        return (n_children_allAges_Local != null)? n_children_allAges_Local: 0;
    }
    public Indicator getD_children_2underLocal() {
        return d_children_2underLocal;
    }
    public Map<Labour, Integer> getPersonContinuousHoursLabourSupplyMap() {
        return personContinuousHoursLabourSupplyMap;
    }
    public void setPersonContinuousHoursLabourSupplyMap(Map<Labour, Integer> personContinuousHoursLabourSupplyMap) {
        this.personContinuousHoursLabourSupplyMap = personContinuousHoursLabourSupplyMap;
    }

    public RandomGenerator getLabourSupplyInnov() {
        return labourSupplyInnov;
    }

    public double getLabourSupplySingleDraw() {
        return labourSupplySingleDraw;
    }

    public void setLabourSupplySingleDraw(double labourSupplySingleDraw) {
        this.labourSupplySingleDraw = labourSupplySingleDraw;
    }

    public double getHoursFormalSocialCare_L1() {
        double hours = 0.0;
        if (careHoursFromFormalWeekly_lag1 !=null)
            if (careHoursFromFormalWeekly_lag1 >0.0)
                hours = careHoursFromFormalWeekly_lag1;
        return hours;
    }

    public double getHoursFormalSocialCare() {
        double hours = 0.0;
        if (careHoursFromFormalWeekly !=null)
            if (careHoursFromFormalWeekly >0.0)
                hours = careHoursFromFormalWeekly;
        return hours;
    }

    public double getHoursInformalSocialCare() {
        return getCareHoursFromPartnerWeekly() + getCareHoursFromDaughterWeekly() + getCareHoursFromSonWeekly() + getCareHoursFromOtherWeekly();
    }
    public double getCareHoursFromPartnerWeekly() {
        double hours = 0.0;
        if (careHoursFromPartnerWeekly !=null)
            if (careHoursFromPartnerWeekly >0.0)
                hours = careHoursFromPartnerWeekly;
        return hours;
    }

    public double getCareHoursFromDaughterWeekly() {
        double hours = 0.0;
        if (careHoursFromDaughterWeekly !=null)
            if (careHoursFromDaughterWeekly >0.0)
                hours = careHoursFromDaughterWeekly;
        return hours;
    }

    public double getCareHoursFromSonWeekly() {
        double hours = 0.0;
        if (careHoursFromSonWeekly !=null)
            if (careHoursFromSonWeekly >0.0)
                hours = careHoursFromSonWeekly;
        return hours;
    }

    public double getCareHoursFromOtherWeekly() {
        double hours = 0.0;
        if (careHoursFromOtherWeekly !=null)
            if (careHoursFromOtherWeekly >0.0)
                hours = careHoursFromOtherWeekly;
        return hours;
    }

    public double getHoursInformalSocialCare_L1() {
        return getCareHoursFromPartner_L1() + getCareHoursFromDaughter_L1() + getCareHoursFromSon_L1() + getCareHoursFromOther_L1();
    }

    public double getTotalHoursSocialCare_L1() {
        return getHoursFormalSocialCare_L1() + getHoursInformalSocialCare_L1();
    }

    public double getCareHoursFromPartner_L1() {
        double hours = 0.0;
        if (careHoursFromPartnerWeekly_lag1 !=null)
            if (careHoursFromPartnerWeekly_lag1 >0.0)
                hours = careHoursFromPartnerWeekly_lag1;
        return hours;
    }

    public double getCareHoursFromDaughter_L1() {
        double hours = 0.0;
        if (careHoursFromDaughterWeekly_lag1 !=null)
            if (careHoursFromDaughterWeekly_lag1 >0.0)
                hours = careHoursFromDaughterWeekly_lag1;
        return hours;
    }

    public double getCareHoursFromSon_L1() {
        double hours = 0.0;
        if (careHoursFromSonWeekly_lag1 !=null)
            if (careHoursFromSonWeekly_lag1 >0.0)
                hours = careHoursFromSonWeekly_lag1;
        return hours;
    }

    public double getCareHoursFromOther_L1() {
        double hours = 0.0;
        if (careHoursFromOtherWeekly_lag1 !=null)
            if (careHoursFromOtherWeekly_lag1 >0.0)
                hours = careHoursFromOtherWeekly_lag1;
        return hours;
    }

    public double getSocialCareCostWeekly() {
        double cost = 0.0;
        if (careFormalCostWeekly !=null)
            if (careFormalCostWeekly >0.0)
                cost = careFormalCostWeekly;
        return cost;
    }

    public RandomGenerator getSocialCareInnov() {
        return socialCareInnov;
    }

}