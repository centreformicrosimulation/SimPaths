package simpaths.model;

import jakarta.persistence.*;
import microsim.agent.Weight;
import microsim.data.db.PanelEntityKey;
import microsim.engine.SimulationEngine;
import microsim.event.EventListener;
import microsim.statistics.IDoubleSource;
import microsim.statistics.IIntSource;
import microsim.statistics.Series;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import simpaths.data.ManagerRegressions;
import simpaths.data.MultiValEvent;
import simpaths.data.Parameters;
import simpaths.data.RegressionName;
import simpaths.data.filters.FertileFilter;
import simpaths.model.decisions.Axis;
import simpaths.model.decisions.DecisionParams;
import simpaths.model.enums.*;
import simpaths.model.lifetime_incomes.AnnualIncome;
import simpaths.model.lifetime_incomes.Individual;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static simpaths.data.Parameters.getUnemploymentRateByGenderEducationAgeYear;

@Entity
public class Person implements EventListener, IDoubleSource, IIntSource, Weight, Comparable<Person> {

    @Transient private static Logger log = Logger.getLogger(Person.class);
    @Transient private final SimPathsModel model;
    @Transient public static long personIdCounter = 1L;			//Could perhaps initialise this to one above the max key number in initial population, in the same way that we pull the max Age information from the input files.

    // database keys
    @EmbeddedId @Column(unique = true, nullable = false) private final PanelEntityKey key;
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
    @JoinColumns({
            @JoinColumn(name = "buid", referencedColumnName = "id"),
            @JoinColumn(name = "butime", referencedColumnName = "simulation_time"),
            @JoinColumn(name = "burun", referencedColumnName = "simulation_run"),
            @JoinColumn(name = "prid", referencedColumnName = "working_id")
    }) private BenefitUnit benefitUnit;
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
    @JoinColumn(name = "income_id", referencedColumnName = "id")
    private Individual ltIncomeDonor;

    // identifiers
    private Long idPersOriginal;
    private Long idBuOriginal;
    private Long idHhOriginal;
    private Long idMother;
    private Long idFather;
    private Long idPartner;
    private Boolean demClonedFlag;
    private Boolean demBornInSimFlag; //Flag to keep track of newborns
    private Long statSeed;
    private Long idHh;
    private Long idBu;
    @Enumerated(EnumType.STRING) private SampleEntry demEnterSample;
    @Enumerated(EnumType.STRING) private SampleExit demExitSample = SampleExit.NotYet;  //entry to sample via international immigration

    // person level variables
    private int demAge; //Age
    private Dcpst demPartnerStatus;
    @Enumerated(EnumType.STRING) private Indicator demAdultChildFlag;
    @Transient private boolean demIoFlag;         // true if a dummy person instantiated for IO decision solution
    @Enumerated(EnumType.STRING) private Gender demMaleFlag;             // gender
    @Enumerated(EnumType.STRING) private Education eduHighestC3;       //Education level
    @Transient private Education eduHighestC3L1;  //Lag(1) of education level
    @Enumerated(EnumType.STRING) private Education eduHighestMotherC3;      //Mother's education level
    @Enumerated(EnumType.STRING) private Education eduHighestFatherC3;      //Father's education level
    @Enumerated(EnumType.STRING) private Ethnicity demEthnC6;          //Ethnicity
    @Enumerated(EnumType.STRING) private Indicator eduSpellFlag;          // in continuous education
    @Enumerated(EnumType.STRING) private Indicator eduReturnFlag;          // return to education
    @Enumerated(EnumType.STRING) private Les_c4 labC4;      //Activity (employment) status
    @Enumerated(EnumType.STRING) private Les_c7_covid labC7Covid; //Activity (employment) status used in the Covid-19 models
    @Transient private Les_c4 labC4L1;		//Lag(1) of activity_status
    @Transient private Les_c7_covid labC7CovidL1;     //Lag(1) of 7-category activity status
    @Column(name="labWorkHist") private Integer labEmpNyear;                  //Work history in months (number of months in employment) (Note: this is monthly in EM, but simulation updates annually so increment by 12 months).
    @Enumerated(EnumType.STRING) private Indicator healthDsblLongtermFlag;	//Long-term sick or disabled if = 1
    @Transient private Indicator healthDsblLongtermFlagL1; //Lag(1) of long-term sick or disabled
    @Enumerated(EnumType.STRING) @Column(name="careNeedFlag") private Indicator careNeedFlag;
    @Column(name="careHrsFormal") private Double careHrsFormalWeek;
    @Column(name="xCareFormalWeek") private Double xCareFormalWeek;
    @Column(name="careHrsFromPartnerWeek") private Double careHrsFromPartnerWeek;
    @Column(name="careHrsFromParentWeek") private Double careHrsFromParentWeek;
    @Column(name="careHrsFromDaughterWeek") private Double careHrsFromDaughterWeek;
    @Column(name="careHrsFromSonWeek") private Double careHrsFromSonWeek;
    @Column(name="careHrsFromOtherWeek") private Double careHrsFromOtherWeek;
    private Boolean labWageOfferLowFlag;
    @Transient private Boolean labWageOfferLowFlagL1;
    @Transient private SocialCareReceipt careReceivedFlag;
    @Transient private Boolean careFormalFlag;
    @Transient private Boolean careFromPartnerFlag;
    @Transient private Boolean careFromDaughterFlag;
    @Transient private Boolean careFromSonFlag;
    @Transient private Boolean careFromOtherFlag;
    @Column(name="careHrsProvidedWeek") private Double careHrsProvidedWeek;
    @Enumerated(EnumType.STRING) @Column(name="careProvidedFlag") private SocialCareProvision careProvidedFlag;
    @Transient private SocialCareProvision careProvidedFlagL1;
    @Transient private Indicator careNeedFlagL1;
    @Transient private Double careHrsFormalWeekL1;
    @Transient private Double careHrsFromPartnerWeekL1;
    @Transient private Double careHrsFromParentWeekL1;
    @Transient private Double careHrsFromDaughterWeekL1;
    @Transient private Double careHrsFromSonWeekL1;
    @Transient private Double careHrsFromOtherWeekL1;
    @Transient private Boolean demPrptyFlagL1;

    // partner lags
    @Transient private Dcpst demPartnerStatusL1;            // lag partnership status
    @Transient private Dcpst demPartnerStatusL2;            // lag (2) partnership status
    @Transient private Education eduHighestPartnerC3L1;     //Lag(1) of partner's education
    @Transient private Dhe healthPartnerSelfRatedL1;
    @Transient private Lesdf_c4 labStatusPartnerAndOwnC4L1;      //Lag(1) of own and partner's activity status
    @Transient private Long idPartnerL1;
    @Transient private HouseholdStatus demStatusHhL1;		//Lag(1) of household_status
    @Transient private Integer demAgePartnerDiffL1;        //Lag(1) of difference between ages of partners in union
    @Transient private Double yPersAndPartnerGrossDiffMonthL1;      //Lag(1) of difference between own and partner's gross personal non-benefit income

    @Enumerated(EnumType.STRING) private Indicator eduExitSampleFlag;    // year left education
    @Transient private Boolean demGiveBirthFlag;
    @Transient private Boolean eduLeaveSchoolFlag;
    @Transient private Boolean demBePartnerFlag;
    @Transient private Boolean demAlignPartnerProcess;
    @Transient private Boolean demLeavePartnerFlag; // Used in partnership alignment process. Indicates that this person has found partner in a test run of union matching.
    @Column(name="wgt") private Double wgt;
    @Column(name="healthPsyDstrss") private Double healthPsyDstrss; //Psychological distress GHQ-12 0-12 caseness score
    @Transient private Double healthPsyDstrssL1;
    @Transient private Dhe healthSelfRatedL1;
    @Enumerated(EnumType.STRING) private Dhe healthSelfRated;
    private Double healthWbScore0to36; //Psychological distress GHQ-12 Likert scale
    @Transient private Double healthWbScore0to36L1; //Lag(1) of dhm
    private Double healthMentalMcs;  //mental well-being: SF12 mental component summary score
    @Transient private Double healthMentalMcsL1;  //mental well-being: SF12 mental component summary score lag 1
    private Double healthPhysicalPcs;  //physical well-being: SF12 physical component summary score
    @Transient private Double healthPhysicalPcsL1;  //physical well-being: SF12 physical component summary score lag 1
    private Double healthMentalPartnerMcs; //mental well-being: SF12 mental component summary score (partner)
    private Double healthPhysicalPartnerPcs; //physical well-being: SF12 physical component summary score (partner)
    private Integer demLifeSatScore1to7;      //life satisfaction - score 1-7
    @Transient private Double demLifeSatScore1to7Pred;
    @Transient private Integer demLifeSatScore1to7L1;      //life satisfaction - score 1-7 lag 1
    @Column(name="demLifeSatEQ5D") private Double demLifeSatEQ5D;
    @Column(name="yFinDstrssFlag") private Boolean yFinDstrssFlag;
    @Transient private Boolean yBenReceivedFlagL1; // Lag(1) of whether person receives benefits
    @Transient private Boolean yBenReceivedFlag; // Does person receive benefits
    @Column(name="yBenUCReceivedFlag") private Boolean yBenUCReceivedFlag; // Person receives UC
    @Transient private Boolean yBenUCReceivedFlagL1;
    @Column(name="yBenNonUCReceivedFlag") private Boolean yBenNonUCReceivedFlag;  // Person receives a benefit which is not UC
    @Transient private Boolean yBenNonUCReceivedFlagL1;
    @Column(name="lifetimeIncome") private Double lifetimeIncome;                  // mean annual equivalised household disposable income by age

    @Enumerated(EnumType.STRING) private Labour labHrsWorkEnumWeek;			//Number of hours of labour supplied each week
    @Transient private Labour labHrsWorkEnumWeekL1; // Lag(1) (previous year's value) of weekly labour supply
    private Integer labHrsWorkWeek;
    private Integer labHrsWorkWeekL1; // Lag(1) of hours worked weekly - use to initialise labour supply weekly_L1 (TODO)

//	Potential earnings is the gross hourly wage an individual can earn while working
//	and is estimated, for each individual, on the basis of observable characteristics as
//	age, education, civil status, number of children, etc. Hence, potential earnings
//	is a separate process in the simulation, and it is computed for every adult
//	individual in the simulated population, in each simulated period.
    @Column(name="labWageHrly") private Double labWageFullTimeHrly;		//Is hourly rate.  Initialised with value: ils_earns / (4.34 * lhw), where lhw is the weekly hours a person worked in EUROMOD input data
    @Column(name="labWageFullTimeHrlyL1") private Double labWageFullTimeHrlyL1; // Lag(1) of potentialHourlyEarnings
    @Transient private Series.Double yDispEquivYear;
    private Double xEquivYear;
    @Transient private Series.Double xEquivYearL1;
    private Double sIndex;
    private Double sIndexNormalised;
    @Transient private LinkedHashMap<Integer, Double> sIndexYearMap;
    private Integer demPartnerNYear; //Number of years in partnership
    @Transient private Integer demPartnerNYearL1; //Lag(1) of number of years in partnership
    private Double yNonBenPersGrossMonth; // asinh of personal non-benefit income per month
    @Transient private Double yNonBenPersGrossMonthL1; //Lag(1) of gross personal non-benefit income
    private Double yMiscPersGrossMonth; // asinh of non-employment non-benefit income per month (capital and pension)
    private Double yCapitalPersMonth; // asinh of capital income per month
    private Double yPensPersGrossMonth; // asinh of pension income per month
    @Transient private Double yCapitalPersMonthL1; //Lag(1) of ypncp
    @Transient private Double yCapitalPersMonthL2; //Lag(2) of capital income
    @Transient private Double yPensPersGrossMonthL1; //Lag(1) of pension income
    @Transient private Double yPensPersGrossMonthL2; //Lag(2) of pension income
    @Transient private Double yMiscPersGrossMonthL1; //Lag(1) of gross personal non-benefit non-employment income
    @Transient private Double yMiscPersGrossMonthL2; //Lag(2) of gross personal non-benefit non-employment income
    @Transient private Double yMiscPersGrossMonthL3; //Lag(3) of gross personal non-benefit non-employment income
    private Double yEmpPersGrossMonth;       // asinh transform of personal labour income per month
    @Transient private Double yEmpPersGrossMonthL1; //Lag(1) of gross personal employment income
    @Transient private Double yEmpPersGrossMonthL2; //Lag(2) of gross personal employment income
    @Transient private Double yEmpPersGrossMonthL3; //Lag(3) of gross personal employment income

    //For matching process
    @Transient private Double demAgeDiffDesired;
    @Transient private Double yWageDesired;
    @Transient private Integer demAgeGroup;

    //This is set to true at the point when individual leaves education and never reset. So if true, individual has not always been in continuous education.
    @Transient private Boolean eduLeftEduFlag;

    //This is set to true at the point when individual leaves partnership and never reset. So if true, individual has been / is in a partnership
    @Transient private Boolean demLeftPartnerFlag;
    @Transient private Integer labHrsWorkNewL1; // Define a variable to keep previous month's value of work hours to be used in the Covid-19 module
    @Transient private Double covidYLabGrossL1;
    @Transient private Indicator covidSEISSReceivedFlag = Indicator.False;
    @Transient private Double covidYLabGross;
    private Quintiles covidYLabGrossXt5;
    @Transient private Double labWageRegressRandomCompoponentEmp;
    @Transient private Double labWageRegressRandomCompoponentNotEmp;
    @Transient private Map<Labour, Integer> personContinuousHoursLabourSupplyMap = new EnumMap<>(Labour.class);

    // local variables interact with regression models
    @Transient private Integer i_demYear;
    @Transient private Region i_demRgn;
    @Transient private Dhhtp_c4 i_demCompHhC4L1;
    @Transient private Ydses_c5 i_yHhQuintilesC5;
    @Transient private Integer i_demNchildL1;
    @Transient private Integer i_demNchild;
    @Transient private Integer i_demNchild0to2L1;
    @Transient private Integer i_demNchild0to17;
    @Transient private Indicator i_demNChild0to2;
    @Transient private Dcpst i_demPartnerStatus;

    // innovations
    @Transient Innovations statInnovations;

    //TODO: Remove when no longer needed.  Used to calculate mean score of employment selection regression.
    @Transient public static Double statMScore;
    @Transient public static Double statFScore;
    @Transient public static Double countMale;
    @Transient public static Double countFemale;
    @Transient public static Double statInverseMillsRatioMaxM = Double.MIN_VALUE;
    @Transient public static Double statInverseMillsRatioMinM = Double.MAX_VALUE;
    @Transient public static Double statInverseMillsRatioMaxF = Double.MIN_VALUE;
    @Transient public static Double statInverseMillsRatioMinF = Double.MAX_VALUE;


    // ---------------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------------
    public Person() {
        model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        key = new PanelEntityKey();
    }

    public Person(long id) {
        model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        key = new PanelEntityKey(id);
    }

    // used by expectations object when creating dummy person to interact with regression functions
    public Person(boolean regressionModel) {
        if (regressionModel) {
            model = null;
            key = null;
            setAllSocialCareVariablesToFalse();
        } else {
            throw new RuntimeException("Person constructor call not recognised");
        }
    }

    // used to create new people who enter the simulation during UpdateMaternityStatus
    public Person(Gender gender, Person mother) {

        this(personIdCounter++, (long)(100000*mother.getFertilityRandomUniform2()));

        demEnterSample = SampleEntry.Birth;
        demMaleFlag = gender;
        idMother = mother.getId();
        eduHighestMotherC3 = mother.getDeh_c3();
        if (mother.getPartner()==null) {
            idFather = null;
            eduHighestFatherC3 = mother.getDeh_c3();
        } else {
            idFather = mother.getPartner().getId();
            eduHighestFatherC3 = mother.getPartner().getDeh_c3();
        }

        labEmpNyear = 0;
        yMiscPersGrossMonth = 0.0;
        yCapitalPersMonth = 0.0;
        yPensPersGrossMonth = 0.0;
        labWageFullTimeHrly = Parameters.MIN_HOURLY_WAGE_RATE;
        healthDsblLongtermFlag = Indicator.False;
        setAllSocialCareVariablesToFalse();
        labWageOfferLowFlag = false;
        benefitUnit = mother.benefitUnit;
        idBu = benefitUnit.getId();
        demAge = 0;
        wgt = mother.getWgt();			//Newborn has same weight as mother (the number of newborns will then be aligned in fertility alignment)
        healthSelfRated = Dhe.VeryGood;
        healthWbScore0to36 = 10.;			//Set to median for under 18's as a placeholder
        healthPsyDstrss = 0.;
        healthMentalMcs = 48.;
        healthPhysicalPcs = 56.;
        demLifeSatScore1to10 = 6.;
        eduHighestC3 = Education.Low;
        demEthnC6 = mother.getDot01();
        labC4 = Les_c4.Student;				//Set lag activity status as Student, i.e. in education from birth
        eduLeftEduFlag = false;
        labC7Covid = Les_c7_covid.Student;
        labHrsWorkEnumWeek = Labour.ZERO;			//Will be updated in Labour Market Module when the person stops being a student
        labHrsWorkWeek = getLabourSupplyWeekly().getHours(this);
        idHh = mother.getBenefitUnit().getHousehold().getId();
//		setDeviationFromMeanRetirementAge();			//This would normally be done within initialisation, but the line above has been commented out for reasons given...
        yDispEquivYear = new Series.Double(this, DoublesVariables.EquivalisedIncomeYearly);
        xEquivYearL1 = new Series.Double(this, DoublesVariables.EquivalisedConsumptionYearly);
        xEquivYear = 0.;
        lifetimeIncome = 0.;
        sIndexYearMap = new LinkedHashMap<Integer, Double>();
        demBornInSimFlag = true;
        yBenReceivedFlag = false;
        yBenNonUCReceivedFlag = false;
        yBenUCReceivedFlag = false;
        yFinDstrssFlag = mother.getYFinDstrssFlag();
        updateVariables(false);
    }

    // a "copy constructor" for persons: used by the cloneBenefitUnit method of the SimPathsModel object
    // used to generate clones both at population load (to un-weight data) and to generate international immigrants
    public Person (Person originalPerson, long statSeed, SampleEntry demEnterSample) {

        this(personIdCounter++, statSeed);
        switch (demEnterSample) {
            case ProcessedInputData -> {
                key.setId(originalPerson.getId());
                idPersOriginal = originalPerson.getIdOriginalPerson();
                idBuOriginal = originalPerson.getIdOriginalBU();
                idHhOriginal = originalPerson.getIdOriginalHH();
            }
            default -> {
                idPersOriginal = originalPerson.key.getId();
                idHhOriginal = originalPerson.benefitUnit.getHousehold().getId();
                idBuOriginal = originalPerson.benefitUnit.getId();
            }
        }

        this.demEnterSample = demEnterSample;

        if (originalPerson.ltIncomeDonor!=null) {
            this.ltIncomeDonor = originalPerson.ltIncomeDonor;
        }
        if (Parameters.lifetimeIncomeImpute)
            this.lifetimeIncome = originalPerson.getLifetimeIncome();

        demAge = originalPerson.demAge;
        demAgeGroup = originalPerson.demAgeGroup;
        demMaleFlag = originalPerson.demMaleFlag;
        eduHighestC3 = originalPerson.eduHighestC3;

        if (originalPerson.eduHighestC3L1 != null) { //If original person misses lagged level of education, assign current level of education
            eduHighestC3L1 = originalPerson.eduHighestC3L1;
        } else {
            eduHighestC3L1 = eduHighestC3;
        }

        eduHighestFatherC3 = originalPerson.eduHighestFatherC3;
        eduHighestMotherC3 = originalPerson.eduHighestMotherC3;
        eduHighestPartnerC3L1 = originalPerson.eduHighestC3L1;

        if (originalPerson.demAge < Parameters.MIN_AGE_TO_LEAVE_EDUCATION) { //If under age to leave education, set flag for being in education to true
            eduSpellFlag = Indicator.True;
        } else {
            eduSpellFlag = originalPerson.eduSpellFlag;
        }

        eduReturnFlag = originalPerson.eduReturnFlag;
        demPartnerNYear = Objects.requireNonNullElse(originalPerson.demPartnerNYear,0);
        demPartnerNYearL1 = Objects.requireNonNullElseGet(originalPerson.demPartnerNYearL1, () -> Math.max(0, this.demPartnerNYear - 1));
        demAgePartnerDiffL1 = originalPerson.demAgePartnerDiffL1;
        demStatusHhL1 = originalPerson.demStatusHhL1;
        if (originalPerson.labC4 != null) {
            labC4 = originalPerson.labC4;
        } else if (originalPerson.demAge < Parameters.MIN_AGE_TO_LEAVE_EDUCATION) {
            labC4 = Les_c4.Student;
        } else if (originalPerson.demAge > (int)Parameters.getTimeSeriesValue(model.getYear(), originalPerson.getDemMaleFlag().toString(), TimeSeriesVariable.FixedRetirementAge)) {
            labC4 = Les_c4.Retired;
        } else if (originalPerson.getLabourSupplyWeekly() != null && originalPerson.getLabourSupplyWeekly().getHours(originalPerson) > 0) {
            labC4 = Les_c4.EmployedOrSelfEmployed;
        } else {
            labC4 = Les_c4.NotEmployed;
        }
        if (demAge < Parameters.MIN_AGE_TO_LEAVE_EDUCATION)
            eduLeftEduFlag = false;
        else if (demAge > Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION)
            eduLeftEduFlag = true;
        else
            eduLeftEduFlag = (!Les_c4.Student.equals(labC4));

        if (originalPerson.labC4L1 != null) { //If original persons misses lagged activity status, assign current activity status
            labC4L1 = originalPerson.labC4L1;
        } else {
            labC4L1 = labC4;
        }
        labC7Covid = originalPerson.labC7Covid;
        if (originalPerson.labC7CovidL1 != null) { //If original persons misses lagged activity status, assign current activity status
            labC7CovidL1 = originalPerson.labC7CovidL1;
        } else {
            labC7CovidL1 = labC7Covid;
        }

        labStatusPartnerAndOwnC4L1 = originalPerson.labStatusPartnerAndOwnC4L1;
        demPartnerStatusL1 = originalPerson.demPartnerStatusL1;
        demPartnerStatusL2 = originalPerson.demPartnerStatusL2;
        yNonBenPersGrossMonth = originalPerson.getyNonBenPersGrossMonth();
        yNonBenPersGrossMonthL1 = originalPerson.yNonBenPersGrossMonthL1;
        yMiscPersGrossMonth = Objects.requireNonNullElse(originalPerson.yMiscPersGrossMonth, 0.0);
        yEmpPersGrossMonth = originalPerson.getyEmpPersGrossMonth();
        yEmpPersGrossMonthL1 = originalPerson.yEmpPersGrossMonthL1;
        yEmpPersGrossMonthL2 = originalPerson.yEmpPersGrossMonthL2;
        yEmpPersGrossMonthL3 = originalPerson.yEmpPersGrossMonthL3;
        yPersAndPartnerGrossDiffMonthL1 = originalPerson.yPersAndPartnerGrossDiffMonthL1;
        yCapitalPersMonth = Objects.requireNonNullElse(originalPerson.yCapitalPersMonth,0.0);
        yCapitalPersMonthL1 = originalPerson.yCapitalPersMonthL1;
        yCapitalPersMonthL2 = originalPerson.yCapitalPersMonthL2;
        yPensPersGrossMonth = Objects.requireNonNullElse(originalPerson.yPensPersGrossMonth, 0.0);
        yPensPersGrossMonthL1 = originalPerson.yPensPersGrossMonthL1;
        yPensPersGrossMonthL2 = originalPerson.yPensPersGrossMonthL2;

        labEmpNyear = Objects.requireNonNullElseGet(originalPerson.labEmpNyear, () -> ((Les_c4.EmployedOrSelfEmployed.equals(labC4)) ? 12 : 0));
        healthDsblLongtermFlag = originalPerson.healthDsblLongtermFlag;
        healthDsblLongtermFlagL1 = originalPerson.healthDsblLongtermFlagL1;
        careNeedFlag = Objects.requireNonNullElse(originalPerson.careNeedFlag, Indicator.False);
        careHrsFormalWeek = Objects.requireNonNullElse(originalPerson.careHrsFormalWeek, 0.0);
        xCareFormalWeek = Objects.requireNonNullElse(originalPerson.xCareFormalWeek, 0.0);
        careHrsFromPartnerWeek = Objects.requireNonNullElse(originalPerson.careHrsFromPartnerWeek, 0.0);
        careHrsFromParentWeek = Objects.requireNonNullElse(originalPerson.careHrsFromParentWeek, 0.0);
        careHrsFromDaughterWeek = Objects.requireNonNullElse(originalPerson.careHrsFromDaughterWeek, 0.0);
        careHrsFromSonWeek = Objects.requireNonNullElse(originalPerson.careHrsFromSonWeek, 0.0);
        careHrsFromOtherWeek = Objects.requireNonNullElse(originalPerson.careHrsFromOtherWeek, 0.0);
        careFormalFlag = Objects.requireNonNullElseGet(originalPerson.careFormalFlag, () -> (careHrsFormalWeek > 0.0));
        careFromPartnerFlag = Objects.requireNonNullElseGet(originalPerson.careFromPartnerFlag, () -> (careHrsFromPartnerWeek > 0.0));
        careFromDaughterFlag = Objects.requireNonNullElseGet(originalPerson.careFromDaughterFlag, () -> (careHrsFromDaughterWeek > 0.0));
        careFromSonFlag = Objects.requireNonNullElseGet(originalPerson.careFromSonFlag, () -> (careHrsFromSonWeek > 0.0));
        careFromOtherFlag = Objects.requireNonNullElseGet(originalPerson.careFromOtherFlag, () -> (careHrsFromOtherWeek > 0.0));
        if (originalPerson.careReceivedFlag !=null)
            careReceivedFlag = originalPerson.careReceivedFlag;
        else {
            if (careFormalFlag) {
                if (careFromPartnerFlag || careFromDaughterFlag || careFromSonFlag || careFromOtherFlag)
                    careReceivedFlag = SocialCareReceipt.Mixed;
                else
                    careReceivedFlag = SocialCareReceipt.Formal;
            } else {
                if (careFromPartnerFlag || careFromDaughterFlag || careFromSonFlag || careFromOtherFlag)
                    careReceivedFlag = SocialCareReceipt.Informal;
                else
                    careReceivedFlag = SocialCareReceipt.None;
            }
        }

        careHrsProvidedWeek = Objects.requireNonNullElse(originalPerson.careHrsProvidedWeek, 0.0);
        careProvidedFlag = Objects.requireNonNullElseGet(originalPerson.careProvidedFlag, () ->
                (careHrsProvidedWeek > 0.01) ? SocialCareProvision.OnlyOther : SocialCareProvision.None);

        careNeedFlagL1 = Objects.requireNonNullElse(originalPerson.careNeedFlagL1, careNeedFlag);
        careHrsFormalWeekL1 = Objects.requireNonNullElse(originalPerson.careHrsFormalWeekL1, careHrsFormalWeek);
        careHrsFromPartnerWeekL1 = Objects.requireNonNullElse(originalPerson.careHrsFromPartnerWeekL1, careHrsFromPartnerWeek);
        careHrsFromDaughterWeekL1 = Objects.requireNonNullElse(originalPerson.careHrsFromDaughterWeekL1, careHrsFromDaughterWeek);
        careHrsFromSonWeekL1 = Objects.requireNonNullElse(originalPerson.careHrsFromSonWeekL1, careHrsFromSonWeek);
        careHrsFromOtherWeekL1 = Objects.requireNonNullElse(originalPerson.careHrsFromOtherWeekL1, careHrsFromOtherWeek);
        careProvidedFlagL1 = Objects.requireNonNullElse(originalPerson.careProvidedFlagL1, careProvidedFlag);

        labWageOfferLowFlag = originalPerson.labWageOfferLowFlag;
        labWageOfferLowFlagL1 = originalPerson.labWageOfferLowFlagL1;
        eduExitSampleFlag = originalPerson.eduExitSampleFlag;
        demGiveBirthFlag = originalPerson.demGiveBirthFlag;
        eduLeaveSchoolFlag = originalPerson.eduLeaveSchoolFlag;
        wgt = originalPerson.wgt;
        healthSelfRated = originalPerson.healthSelfRated;
        healthWbScore0to36 = originalPerson.healthWbScore0to36;

        if (originalPerson.healthSelfRatedL1 != null) { //If original person misses lagged level of health, assign current level of health as lagged value
            healthSelfRatedL1 = originalPerson.healthSelfRatedL1;
        } else {
            healthSelfRatedL1 = originalPerson.healthSelfRated;
        }

        if (originalPerson.healthWbScore0to36L1 != null) {
            healthWbScore0to36L1 = originalPerson.healthWbScore0to36L1;
        } else {
            healthWbScore0to36L1 = originalPerson.healthWbScore0to36;
        }

        healthPsyDstrss = Objects.requireNonNullElse(originalPerson.healthPsyDstrss, 0.);
        healthPsyDstrssL1 = Objects.requireNonNullElse(originalPerson.healthPsyDstrssL1, healthPsyDstrss);

        demLifeSatScore1to7 = originalPerson.demLifeSatScore1to7;
        healthMentalMcs = originalPerson.healthMentalMcs;
        healthPhysicalPcs = originalPerson.healthPhysicalPcs;
        healthMentalPartnerMcs = originalPerson.healthMentalPartnerMcs;
        healthPhysicalPartnerPcs = originalPerson.healthPhysicalPartnerPcs;

        if (originalPerson.demLifeSatScore1to7L1 != null) {
            demLifeSatScore1to7L1 = originalPerson.demLifeSatScore1to7L1;
        } else {
            demLifeSatScore1to7L1 = originalPerson.demLifeSatScore1to7;
        }

        if (originalPerson.healthWbScore0to36L1 != null) {
            healthMentalMcsL1 = originalPerson.healthMentalMcsL1;
        } else {
            healthMentalMcsL1 = originalPerson.healthMentalMcs;
        }

        if (originalPerson.healthPhysicalPcsL1 != null) {
            healthPhysicalPcsL1 = originalPerson.healthPhysicalPcsL1;
        } else {
            healthPhysicalPcsL1 = originalPerson.healthPhysicalPcs;
        }

        if (originalPerson.labHrsWorkWeekL1 != null) {
            labHrsWorkEnumWeekL1 = Labour.convertHoursToLabour(originalPerson.labHrsWorkWeekL1);
        } else {
            labHrsWorkEnumWeekL1 = null; // Update only if value know; null values handled by getter. Should throw an exception if required before initialised in the simulation.
        }

        healthPartnerSelfRatedL1 = originalPerson.healthPartnerSelfRatedL1;
        labHrsWorkWeek = originalPerson.labHrsWorkWeek;
        labHrsWorkWeekL1 = originalPerson.labHrsWorkWeekL1;
        labHrsWorkEnumWeek = originalPerson.getLabourSupplyWeekly();
        double[] sampleDifferentials = setMarriageTargets();
        demAgeDiffDesired = Objects.requireNonNullElseGet(originalPerson.demAgeDiffDesired, () -> sampleDifferentials[0]);
        yWageDesired = Objects.requireNonNullElseGet(originalPerson.yWageDesired, () -> sampleDifferentials[1]);

        statMScore = originalPerson.statMScore;
        statFScore = originalPerson.statFScore;
        countMale = originalPerson.countMale;
        countFemale = originalPerson.countFemale;
        statInverseMillsRatioMaxM = originalPerson.statInverseMillsRatioMaxM;
        statInverseMillsRatioMinM = originalPerson.statInverseMillsRatioMinM;
        statInverseMillsRatioMaxF = originalPerson.statInverseMillsRatioMaxF;
        statInverseMillsRatioMinF = originalPerson.statInverseMillsRatioMinF;

        demAdultChildFlag = originalPerson.demAdultChildFlag;
        yDispEquivYear = new Series.Double(this, DoublesVariables.EquivalisedIncomeYearly);
        xEquivYearL1 = new Series.Double(this, DoublesVariables.EquivalisedConsumptionYearly);
        xEquivYear = originalPerson.xEquivYear;
        sIndexYearMap = new LinkedHashMap<Integer, Double>();
        demEthnC6 = originalPerson.demEthnC6;
        yBenReceivedFlag = originalPerson.yBenReceivedFlag;
        yBenReceivedFlagL1 = originalPerson.yBenReceivedFlagL1;
        yBenNonUCReceivedFlag = originalPerson.yBenNonUCReceivedFlag;
        yBenNonUCReceivedFlagL1 = originalPerson.yBenNonUCReceivedFlagL1;
        yBenUCReceivedFlag = originalPerson.yBenUCReceivedFlag;
        yBenUCReceivedFlagL1 = originalPerson.yBenUCReceivedFlagL1;
        yFinDstrssFlag = originalPerson.yFinDstrssFlag;

        if (originalPerson.labWageFullTimeHrly > Parameters.MIN_HOURLY_WAGE_RATE) {
            labWageFullTimeHrly = Math.min(Parameters.MAX_HOURLY_WAGE_RATE, Math.max(Parameters.MIN_HOURLY_WAGE_RATE, originalPerson.labWageFullTimeHrly));
        } else {
            if (Les_c4.EmployedOrSelfEmployed.equals(labC4)) {
                labC4 = Les_c4.NotEmployed;
            }
            labC4L1 = labC4;
            labWageFullTimeHrly = -9.0;
        }
        if (originalPerson.labWageFullTimeHrlyL1 !=null && originalPerson.labWageFullTimeHrlyL1 >Parameters.MIN_HOURLY_WAGE_RATE) {
            labWageFullTimeHrlyL1 = Math.min(Parameters.MAX_HOURLY_WAGE_RATE, Math.max(Parameters.MIN_HOURLY_WAGE_RATE, originalPerson.labWageFullTimeHrlyL1));
        } else {
            labWageFullTimeHrlyL1 = labWageFullTimeHrly;
        }
    }

    // used by other constructors
    public Person(Long id, long statSeed) {
        super();
        key = new PanelEntityKey(id);
        model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        demClonedFlag = false;

        // initialise random draws
        this.statSeed = statSeed;
        statInnovations = new Innovations(33, 1, 1, statSeed);

        //Draw desired age and wage differential for parametric partnership formation for people above age to get married:
        double[] sampleDifferentials = setMarriageTargets();
        demAgeDiffDesired = sampleDifferentials[0];
        yWageDesired = sampleDifferentials[1];
    }


    // ---------------------------------------------------------------------
    // Initialisation methods
    // ---------------------------------------------------------------------
    public void cloneCleanup() {

        if (labWageFullTimeHrly < Parameters.MIN_HOURLY_WAGE_RATE) {
            updateFullTimeHourlyEarnings();
            if (labWageFullTimeHrlyL1 < Parameters.MIN_HOURLY_WAGE_RATE)
                labWageFullTimeHrlyL1 = labWageFullTimeHrly;
        }
    }

    private double[] setMarriageTargets() {

        double[] sampleDifferentials = new double[2];
        if (Parameters.MARRIAGE_MATCH_TO_MEANS) {
            sampleDifferentials[0] = Parameters.targetMeanAgeDifferential;
            sampleDifferentials[1] = Parameters.targetMeanWageDifferential;
        } else {
            sampleDifferentials = Parameters.getWageAndAgeDifferentialMultivariateNormalDistribution(statInnovations.getSingleDrawLongInnov(0));
        }
        return sampleDifferentials;
    }

    private void setAllSocialCareVariablesToFalse() {
        careNeedFlag = Indicator.False;
        careHrsFormalWeek = -9.0;
        careHrsFromPartnerWeek = -9.0;
        careHrsFromParentWeek = -9.0;
        careHrsFromDaughterWeek = -9.0;
        careHrsFromSonWeek = -9.0;
        careHrsFromOtherWeek = -9.0;
        careHrsProvidedWeek = -9.0;
        xCareFormalWeek = -9.0;
        careReceivedFlag = SocialCareReceipt.None;
        careFormalFlag = false;
        careFromPartnerFlag = false;
        careFromDaughterFlag = false;
        careFromSonFlag = false;
        careFromOtherFlag = false;
        careProvidedFlag = SocialCareProvision.None;
        careNeedFlagL1 = Indicator.False;
        careHrsFormalWeekL1 = -9.0;
        careHrsFromPartnerWeekL1 = -9.0;
        careHrsFromParentWeekL1 = -9.0;
        careHrsFromDaughterWeekL1 = -9.0;
        careHrsFromSonWeekL1 = -9.0;
        careHrsFromOtherWeekL1 = -9.0;
        careProvidedFlagL1 = SocialCareProvision.None;
    }

    public void setAdditionalFieldsInInitialPopulation() {

        if (labHrsWorkEnumWeek ==null)
            labHrsWorkEnumWeek = Labour.convertHoursToLabour(model.getInitialHoursWorkedWeekly().get(key.getId()).intValue()); // TODO: this can be simplified to obtain value from already initialised hours worked weekly variable? The entire database query on setup is redundant? See initialisation of the lag below.
        yBenReceivedFlagL1 = yBenReceivedFlag;
        labHrsWorkEnumWeekL1 = Labour.convertHoursToLabour(labHrsWorkWeekL1);
        yBenNonUCReceivedFlagL1 = yBenNonUCReceivedFlag;
        yBenUCReceivedFlagL1 = yBenUCReceivedFlag;

        if(UnionMatchingMethod.SBAM.equals(model.getUnionMatchingMethod())) {
            updateAgeGroup();
        }

        labHrsWorkWeek = null;	//Not to be updated as labourSupplyWeekly contains this information.
        updateVariables(true);
    }

    //This method assign people to age groups used to define types in the SBAM matching procedure
    private void updateAgeGroup() {
        if (demAge < 18) {
            demAgeGroup = 0;
            model.tmpPeopleAssigned++;
        } else if(demAge >= 18 && demAge < 21) {
            demAgeGroup = 1;
            model.tmpPeopleAssigned++;
        } else if(demAge >= 21 && demAge < 24) {
            demAgeGroup = 2;
            model.tmpPeopleAssigned++;
        } else if(demAge >= 24 && demAge < 27) {
            demAgeGroup = 3;
            model.tmpPeopleAssigned++;
        } else if(demAge >= 27 && demAge < 30) {
            demAgeGroup = 4;
            model.tmpPeopleAssigned++;
        } else if(demAge >= 30 && demAge < 33) {
            demAgeGroup = 5;
            model.tmpPeopleAssigned++;
        } else if(demAge >= 33 && demAge < 36) {
            demAgeGroup = 6;
            model.tmpPeopleAssigned++;
        } else if(demAge >= 36 && demAge < 40) {
            demAgeGroup = 7;
            model.tmpPeopleAssigned++;
        } else if(demAge >= 40 && demAge < 45) {
            demAgeGroup = 8;
            model.tmpPeopleAssigned++;
        } else if(demAge >= 45 && demAge < 55) {
            demAgeGroup = 9;
            model.tmpPeopleAssigned++;
        } else if(demAge >= 55 && demAge < 65) {
            demAgeGroup = 10;
            model.tmpPeopleAssigned++;
        } else if(demAge >= 65) {
            demAgeGroup = 11;
            model.tmpPeopleAssigned++;
        } else {
            System.out.println("Could not assign age group!");
        }
    }


    // ---------------------------------------------------------------------
    // Event Listener
    // ---------------------------------------------------------------------
    public enum Processes {
        Aging,
        Cohabitation,
        ConsiderMortality,
        ConsiderRetirement,
        Fertility,
        FinancialDistress,
        GiveBirth,
        Health,
        HealthEQ5D,
        HealthMentalHM1, 				//Predict level of mental health on the GHQ-12 Likert scale (Step 1)
        HealthMentalHM2,				//Modify the prediction from Step 1 by applying increments / decrements for exposure
        HealthMentalHM1HM2Cases,		//Case-based prediction for psychological distress, Steps 1 and 2 together (no longer used)
        HealthMentalHM1Case,		//Case-based prediction for psychological distress, Step 1
        HealthMentalHM2Case,		//Case-based prediction for psychological distress, Step 2
        HealthMCS1,
        HealthMCS2,
        HealthPCS1,
        HealthPCS2,
        LifeSatisfaction1,
        LifeSatisfaction2,
        InSchool,
        LeavingSchool,
        PartnershipDissolution,
        ProjectEquivConsumption,
        ReviseLifetimeIncome,
        SocialCareReceipt,
        SocialCareProvision,
        Unemployment,
        Update,
        UpdateOutputVariables,
        UpdatePotentialHourlyEarnings,	//Needed to union matching and labour supply
    }

    @Override
    public void onEvent(Enum<?> type) {
        switch ((Processes) type) {
            case Aging -> {
                aging();
            }
            case Update -> {
                updateVariables(false);
            }
            case UpdateOutputVariables ->  {
                updateOutputVariables();
            }
            case ProjectEquivConsumption -> {
                projectEquivConsumption();
            }
            case Cohabitation -> {
    //			log.debug("BenefitUnit Formation for person " + this.getKey().getId());
                cohabitation();
            }
            case PartnershipDissolution -> {
                partnershipDissolution();
            }
            case ConsiderMortality -> {
                considerMortality();
            }
            case ConsiderRetirement -> {
                considerRetirement();
            }
            case Fertility -> {
                fertility();
            }
            case FinancialDistress -> {
                updateFinancialDistress();
            }
            case GiveBirth -> {
    //			log.debug("Check whether to give birth for person " + this.getKey().getId());
                giveBirth();
            }
            case Health -> {
    //			log.debug("Health for person " + this.getKey().getId());
                health();
            }
            case ReviseLifetimeIncome -> {
                updateLtIncome();
            }
            case SocialCareReceipt -> {
                evaluateSocialCareReceipt();
            }
            case SocialCareProvision -> {
                evaluateSocialCareProvision();
            }
            case HealthMentalHM1 -> {
                healthMentalHM1Level();
            }
            case HealthMentalHM2 -> {
                healthMentalHM2Level();
            }
            case HealthMCS1 -> {
                healthMCS1();
            }
            case HealthMCS2 -> {
                healthMCS2();
            }
            case HealthPCS1 -> {
                healthPCS1();
            }
            case HealthPCS2 -> {
                healthPCS2();
            }
            case LifeSatisfaction1 -> {
                lifeSatisfaction1();
            }
            case LifeSatisfaction2 -> {
                lifeSatisfaction2();
            }
            case HealthMentalHM1Case -> {
                healthMentalHM1Case();
            }
            case HealthMentalHM2Case -> {
                healthMentalHM2Case();
            }
            case HealthEQ5D -> {
                healthEQ5D();
            }
            case InSchool -> {
    //			log.debug("In Education for person " + this.getKey().getId());
                inSchool();
            }
            case LeavingSchool -> {
                leavingSchool();
            }
            case UpdatePotentialHourlyEarnings -> {
    //			System.out.println("Update wage equation for person " + this.getKey().getId() + " with age " + age + " with activity_status " + activity_status + " and activity_status_lag " + activity_status_lag + " and toLeaveSchool " + toLeaveSchool + " with education " + education);
                updateFullTimeHourlyEarnings();
            }
            case Unemployment -> {
                updateUnemploymentState();
            }
            default -> {
                throw new RuntimeException("failed to identify process type in Person.onEvent");
            }
        }
    }


    // ---------------------------------------------------------------------
    // Processes
    // ---------------------------------------------------------------------

    public void fertility() {
        double probitAdjustment = (model.isAlignFertility()) ? Parameters.getAlignmentValue(getYear(), AlignmentVariable.FertilityAlignment) : 0.0;
        fertility(probitAdjustment);
    }

    public void fertility(double probitAdjustment) {

        demGiveBirthFlag = false;
        FertileFilter filter = new FertileFilter();
        if (filter.evaluate(this)) {

            double prob;
            if (model.getCountry().equals(Country.UK)) {

                if (getDemAge() <= 29 && getLes_c4().equals(Les_c4.Student) && !isLeftEducation()) {
                    //If age below or equal to 29 and in continuous education follow process F1a
                    double score = Parameters.getRegFertilityF1a().getScore(this, Person.DoublesVariables.class);
                    prob = Parameters.getRegFertilityF1a().getProbability(score + probitAdjustment);
                } else {
                    //Otherwise if not in continuous education, follow process F1b
                    double score = Parameters.getRegFertilityF1b().getScore(this, Person.DoublesVariables.class);
                    prob = Parameters.getRegFertilityF1b().getProbability(score + probitAdjustment);
                }
            } else if (model.getCountry().equals(Country.IT)) {

                prob = Parameters.getRegFertilityF1().getProbability(this, Person.DoublesVariables.class);
            } else
                throw new RuntimeException("Country not recognised when evaluating fertility status");

            if (statInnovations.getDoubleDraw(29)<prob)
                demGiveBirthFlag = true;
        }
    }

    private void updateUnemploymentState() {
        labWageOfferLowFlag = false;
        if (Parameters.flagUnemployment) {

            if (this == benefitUnit.getRefPersonForDecisions()) {
                // unemployment currently limited to reference person for decisions

                double prob;
                if (demMaleFlag.equals(Gender.Male)) {
                    if (eduHighestC3.equals(Education.High)) {
                        prob = Parameters.getRegUnemploymentMaleGraduateU1a().getProbability(this, Person.DoublesVariables.class);
                    } else {
                        prob = Parameters.getRegUnemploymentMaleNonGraduateU1b().getProbability(this, Person.DoublesVariables.class);
                    }
                } else {
                    if (eduHighestC3.equals(Education.High)) {
                        prob = Parameters.getRegUnemploymentFemaleGraduateU1c().getProbability(this, Person.DoublesVariables.class);
                    } else {
                        prob = Parameters.getRegUnemploymentFemaleNonGraduateU1d().getProbability(this, Person.DoublesVariables.class);
                    }
                }
                labWageOfferLowFlag = (statInnovations.getDoubleDraw(22) < prob);
            }
        }
    }

    //********************************************************
    // method to adjust for one year increment
    //********************************************************
    private void aging() {

        // iterate years in cohabiting partnership
        Person partner = getPartner();
        if (partner != null) {
            if (Objects.equals(partner.getId(), idPartnerL1)) {
                if (demPartnerNYear ==null)
                    throw new RuntimeException("problem identifying dcpyy");
                demPartnerNYear++;
            } else
                demPartnerNYear = 0;
        } else
            demPartnerNYear = 0;

        // iterate employment history
        if (Les_c4.EmployedOrSelfEmployed.equals(labC4)) {
            labEmpNyear = labEmpNyear +12;
        }

        // iterate age and update for maturity
        demAge++;
        if (demAge == Parameters.AGE_TO_BECOME_RESPONSIBLE) {
            setupNewBenefitUnit(true);
            considerLeavingHome();
        } else if (demAge > Parameters.AGE_TO_BECOME_RESPONSIBLE && Indicator.True.equals(demAdultChildFlag)) {
            considerLeavingHome();
        }
        updateAgeGroup();   //Update ageGroup as person ages
     }

    private void considerMortality() {

        boolean flagDies = false;
        if (model.getProjectMortality()) {

            if ( Occupancy.Couple.equals(benefitUnit.getOccupancy()) || benefitUnit.getSize() == 1 ) {
                // exclude single parents with dependent children from death

                double mortalityProbability = Parameters.getMortalityProbability(demMaleFlag, demAge, model.getYear());
                if (statInnovations.getDoubleDraw(0) < mortalityProbability) {
                    flagDies = true;
                }
            }
        }
        if (flagDies || demAge > Parameters.maxAge)
            demExitSample = SampleExit.Death;
    }

    //This process should be applied to those at the age to become responsible / leave home OR above if they have the adultChildFlag set to True (i.e. people can move out, but not move back in).
    private void considerLeavingHome() {

        //For those who are moving out, evaluate whether they should have stayed with parents and if yes, set the adultchildflag to true

        double prob = Parameters.getRegLeaveHomeP1a().getProbability(this, Person.DoublesVariables.class);
        boolean toLeaveHome = (statInnovations.getDoubleDraw(21) < prob);
        if (Les_c4.Student.equals(labC4)) {

            demAdultChildFlag = Indicator.True; //Students not allowed to leave home to match filtering conditon
        } else {

            if (!toLeaveHome) { //If at the age to leave home but regression outcome is negative, person has adultchildflag set to true (although they still set up a new benefitUnit in the simulation, it's treated differently in the labour supply)

                demAdultChildFlag = Indicator.True;
            } else {

                demAdultChildFlag = Indicator.False;
                setupNewHousehold(); //If person leaves home, they set up a new household
            }
        }
    }

    public boolean considerRetirement() {
        boolean toRetire = false;
        if (demAge >= Parameters.MIN_AGE_TO_RETIRE && !Les_c4.Retired.equals(labC4) && !Les_c4.Retired.equals(labC4L1)) {
            if (Parameters.enableIntertemporalOptimisations && DecisionParams.flagRetirement) {
                if (Labour.ZERO.equals(labHrsWorkEnumWeekL1)) {
                    toRetire = true;
                }
           } else {
                double prob;
                if (getPartner() != null) {
                    prob = Parameters.getRegRetirementR1b().getProbability(this, Person.DoublesVariables.class);
                } else {
                    prob = Parameters.getRegRetirementR1a().getProbability(this, Person.DoublesVariables.class);
                }
                toRetire = (statInnovations.getDoubleDraw(23) < prob);
            }
            if (toRetire) {
                setLes_c4(Les_c4.Retired);
            }
        }
        return toRetire;
    }

    private void updateFinancialDistress() {
        double prob = Parameters.getRegFinancialDistress().getProbability(this, Person.DoublesVariables.class);
        yFinDstrssFlag = statInnovations.getDoubleDraw(32) < prob;
    }

    // * HEALTH AND WELLBEING ********************************************************************

    /**
     * Health and wellbeing - GHQ-12 subjective wellbeing, Likert scale 0-36 step 1
     *
     * <p>Calculates the 'baseline' yearly update to the GHQ-12 likert (level) score ({@code healthWbScore0to36}) based on demographic variables.
     * Runs <b>before</b> {@link #healthMentalHM2Level()}.</p>
     *
     * @filter Age 16+
     * @updates {@code Person.healthWbScore0to36}
     * @see <a href="https://www.understandingsociety.ac.uk/documentation/mainstage/variables/scghq1_dv/">scghq1_dv</a>
     */
    protected void healthMentalHM1Level() {
        if (demAge >= 16) {
            double score = Parameters.getRegHealthHM1Level().getScore(this, Person.DoublesVariables.class);
            double rmse = Parameters.getRMSEForRegression("HM1_L");
            double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(statInnovations.getDoubleDraw(1));
            healthWbScore0to36 = score + rmse*gauss;
        }
    }


    /**
     * Health and wellbeing - GHQ-12 subjective wellbeing, Likert scale 0-36 step 2
     *
     * <p>Updates GHQ-12 likert (level) score ({@code healthWbScore0to36}) from the causal effects of economic transitions.
     * Applies separate estimates for Male and Female Persons.
     * Runs <b>after</b> {@link #healthMentalHM1Level()}</p>
     *
     * @filter Age 25-64
     * @updates {@code Person.healthWbScore0to36}
     * @see <a href="https://www.understandingsociety.ac.uk/documentation/mainstage/variables/scghq1_dv/">scghq1_dv</a>
     *
     */
    protected void healthMentalHM2Level() {

        double dhmPrediction;
        if (demAge >= 25 && demAge <= 64) {
            if (Gender.Male.equals(getDemMaleFlag())) {
                dhmPrediction = Parameters.getRegHealthHM2LevelMales().getScore(this, Person.DoublesVariables.class);
                healthWbScore0to36 = constrainDhmEstimate(dhmPrediction+ healthWbScore0to36);
            } else if (Gender.Female.equals(getDemMaleFlag())) {
                dhmPrediction = Parameters.getRegHealthHM2LevelFemales().getScore(this, Person.DoublesVariables.class);
                healthWbScore0to36 = constrainDhmEstimate(dhmPrediction+ healthWbScore0to36);
            } else System.out.println("healthMentalHM2 method in Person class: Person has no gender!");
        } else if (healthWbScore0to36 != null) {
            healthWbScore0to36 = constrainDhmEstimate(healthWbScore0to36);
        }
    }


    /**
     * Health and wellbeing - GHQ-12 subjective wellbeing, Caseness scale 0-12 step 1
     *
     * <p>Calculates the 'baseline' yearly update to the GHQ-12 Caseness (cases) score ({@code healthPsyDstrss}) based on demographic variables.
     * Runs <b>before</b> {@link #healthMentalHM2Case()}.</p>
     *
     * @filter Age 16+
     * @updates {@code Person.healthPsyDstrss}
     * @see <a href="https://www.understandingsociety.ac.uk/documentation/mainstage/variables/scghq2_dv/">scghq2_dv</a>
     */
    protected void healthMentalHM1Case() {
        if (dag >= 16) {
            Map<DhmGhq,Double> probs = ManagerRegressions.getProbabilities(this, RegressionName.HealthHM1Case);
            MultiValEvent event = new MultiValEvent(probs, innovations.getDoubleDraw(36));
            healthPsyDstrss = Double.valueOf(event.eval().getValue());
        }
    }

    /**
     * Health and wellbeing - GHQ-12 subjective wellbeing, Caseness scale 0-12 step 2
     *
     * <p>Updates GHQ-12 Caseness (cases) score ({@code healthPsyDstrss}) from the causal effects of economic transitions.
     * Applies separate estimates for Male and Female Persons.
     * Runs <b>after</b> {@link #healthMentalHM1Case()}</p>
     *
     * @filter Age 25-64
     * @updates {@code Person.healthPsyDstrss}
     * @see <a href="https://www.understandingsociety.ac.uk/documentation/mainstage/variables/scghq2_dv/">scghq2_dv</a>
     */
    protected void healthMentalHM2Case() {
        double dhmGhqPrediction;
        if (dag >= 25 && dag <= 64) {
            if (Gender.Male.equals(getDgn())) {
                dhmGhqPrediction = Parameters.getRegHealthHM2CaseMales().getScore(this, Person.DoublesVariables.class);
                healthPsyDstrss = constrainhealthPsyDstrssEstimate(dhmGhqPrediction+ healthPsyDstrss);
            } else if (Gender.Female.equals(getDgn())) {
                dhmGhqPrediction = Parameters.getRegHealthHM2CaseFemales().getScore(this, Person.DoublesVariables.class);
                healthPsyDstrss = constrainhealthPsyDstrssEstimate(dhmGhqPrediction+ healthPsyDstrss);
            } else System.out.println("healthMentalHM2 method in Person class: Person has no gender!");
        } else if (healthPsyDstrss != null) {
            healthPsyDstrss = constrainhealthPsyDstrssEstimate(healthPsyDstrss);
        }
    }

    /**
     * Health and wellbeing - Mental Component Summary update step 1
     *
     * <p>Calculates the 'baseline' yearly update to the MCS score ({@code healthMentalMcs}) based on demographic variables.
     * Runs <b>before</b> {@link #healthMCS2()}.</p>
     *
     * @filter Age 16+
     * @updates {@code Person.healthMentalMcs}
     * @see <a href="https://www.understandingsociety.ac.uk/documentation/mainstage/variables/sf12mcs_dv/">sf12mcs_dv</a>
     */
    protected void healthMCS1() {

        if (dag >= 16) {
            double mcsPrediction = Parameters.getRegHealthMCS1().getScore(this, Person.DoublesVariables.class);
            double rmse = Parameters.getRMSEForRegression("DHE_MCS1");
            double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innovations.getDoubleDraw(33));
            healthMentalMcs = mcsPrediction + rmse * gauss;
        }
    }

    /**
     * Health and wellbeing - Mental Component Summary update step 2
     *
     * <p>Updates MCS score ({@code healthMentalMcs}) from the causal effects of economic transitions.
     * Applies separate estimates for Male and Female Persons.
     * Runs <b>after</b> {@link #healthMCS1()}</p>
     *
     * @filter Age 25-64
     * @updates {@code Person.healthMentalMcs}
     * @see <a href="https://www.understandingsociety.ac.uk/documentation/mainstage/variables/sf12mcs_dv/">sf12mcs_dv</a>
     */
    protected void healthMCS2() {

        double mcsPrediction;
        if (dag >= 25 && dag <= 64) {
            if (Gender.Male.equals(getDgn())) {
                mcsPrediction = Parameters.getRegHealthMCS2Males().getScore(this, Person.DoublesVariables.class);
                healthMentalMcs = constrainSF12Estimate(mcsPrediction + healthMentalMcs);
            } else if (Gender.Female.equals(getDgn())) {
                mcsPrediction = Parameters.getRegHealthMCS2Females().getScore(this, Person.DoublesVariables.class);
                healthMentalMcs = constrainSF12Estimate(mcsPrediction + healthMentalMcs);
            }
        } else if (healthMentalMcs != null) {
            healthMentalMcs = constrainSF12Estimate(healthMentalMcs);
        }
    }

    /**
     * Health and wellbeing - Physical Component Summary update step 1
     *
     * <p>Calculates the 'baseline' yearly update to the PCS score ({@code healthPhysicalPcs}) based on demographic variables.
     * Runs <b>before</b> {@link #healthPCS2()}.</p>
     *
     * @filter Age 16+
     * @updates {@code Person.healthPhysicalPcs}
     * @see <a href="https://www.understandingsociety.ac.uk/documentation/mainstage/variables/sf12pcs_dv/">sf12pcs_dv</a>
     */
    protected void healthPCS1() {

        if (dag >= 16) {
            double pcsPrediction = Parameters.getRegHealthPCS1().getScore(this, Person.DoublesVariables.class);
            double rmse = Parameters.getRMSEForRegression("DHE_PCS1");
            double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innovations.getDoubleDraw(34));
            healthPhysicalPcs = pcsPrediction + rmse * gauss;
        }
    }

    /**
     * Health and wellbeing - Physical Component Summary update step 2
     *
     * <p>Updates PCS score ({@code healthPhysicalPcs}) from the causal effects of economic transitions.
     * Applies separate estimates for Male and Female Persons.
     * Runs <b>after</b> {@link #healthPCS1()}</p>
     *
     * @filter Age 25-64
     * @updates {@code Person.healthPhysicalPcs}
     * @see <a href="https://www.understandingsociety.ac.uk/documentation/mainstage/variables/sf12pcs_dv/">sf12pcs_dv</a>
     */
    protected void healthPCS2() {

        double pcsPrediction;
        if (dag >= 25 && dag <= 64) {
            if (Gender.Male.equals(getDgn())) {
                pcsPrediction = Parameters.getRegHealthPCS2Males().getScore(this, Person.DoublesVariables.class);
                healthPhysicalPcs = constrainSF12Estimate(pcsPrediction + healthPhysicalPcs);
            } else if (Gender.Female.equals(getDgn())) {
                pcsPrediction = Parameters.getRegHealthPCS2Females().getScore(this, Person.DoublesVariables.class);
                healthPhysicalPcs = constrainSF12Estimate(pcsPrediction + healthPhysicalPcs);
            }
        } else if (healthPhysicalPcs != null) {
            healthPhysicalPcs = constrainSF12Estimate(healthPhysicalPcs);
        }
    }


    /**
     * Health and wellbeing - Life satisfaction score 0-10 step 1
     *
     * <p>Calculates the 'baseline' yearly update to the Life Satisfaction score ({@code demLifeSatScore1to10}) based on demographic variables.
     * Runs <b>before</b> {@link #lifeSatisfaction2()}.</p>
     *
     * @filter Age 16+
     * @updates {@code Person.demLifeSatScore1to10}
     * @see <a href="https://www.understandingsociety.ac.uk/documentation/mainstage/variables/sclfsato/">sclfsato</a>
     */
    protected void lifeSatisfaction1() {

        if (dag >= 16) {
            double dlsPrediction = Parameters.getRegLifeSatisfaction1().getScore(this, Person.DoublesVariables.class);
            double rmse = Parameters.getRMSEForRegression("DLS1");
            double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innovations.getDoubleDraw(35));
            demLifeSatScore1to10 = dlsPrediction + rmse*gauss;
        }

    }

    /**
     * Health and wellbeing - Life satisfaction score 0-10 step 2
     *
     * <p>Updates Life Satisfaction score ({@code demLifeSatScore1to10}) from the causal effects of economic transitions.
     * Applies separate estimates for Male and Female Persons.
     * Runs <b>after</b> {@link #lifeSatisfaction1()}</p>
     *
     * @filter Age 25-64
     * @updates {@code Person.demLifeSatScore1to10}
     * @see <a href="https://www.understandingsociety.ac.uk/documentation/mainstage/variables/sclfsato/">sclfsato</a>
     */
    protected void lifeSatisfaction2() {

        if (dag >= 25 && dag <= 64) {

            double dlsPrediction;
            if (Gender.Male.equals(getDgn())) {
                dlsPrediction = Parameters.getRegLifeSatisfaction2Males().getScore(this, Person.DoublesVariables.class);
                demLifeSatScore1to10 = constrainLifeSatisfactionEstimate(dlsPrediction + demLifeSatScore1to10);
            } else if (Gender.Female.equals(getDgn())) {
                dlsPrediction = Parameters.getRegLifeSatisfaction2Females().getScore(this, Person.DoublesVariables.class);
                demLifeSatScore1to10 = constrainLifeSatisfactionEstimate(dlsPrediction + demLifeSatScore1to10);
            }
        } else if (demLifeSatScore1to10 != null) {
            demLifeSatScore1to10 = constrainLifeSatisfactionEstimate(demLifeSatScore1to10);
        }
    }


    /**
     * Generate EQ5D score from MCS and PCS
     *
     * <p>Calculates an EQ5D score per person from previously updated MCS and PCS scores.
     * Loads conversion parameters selected as 'franks' or 'lawrence' at {@link Parameters#eq5dConversionParameters}.
     * Estimates are truncated to -0.594 to 1.0.</p>
     *
     * @filter Age 16+
     * @updates {@code Person.demLifeSatEQ5D}
     * @see <a href="https://journals.sagepub.com/doi/epdf/10.1177/0272989X04265477">Franks et al., 2004</a>
     * @see <a href="https://journals.sagepub.com/doi/abs/10.1177/0272989X04264015">Lawrence &amp; Fleishman, 2004</a>
     */
    protected void healthEQ5D() {

        double eq5dPrediction;
        if (dag >= 16) {

            eq5dPrediction = Parameters.getRegEQ5D().getScore(this, Person.DoublesVariables.class);
            if (eq5dPrediction > 1) {
                demLifeSatEQ5D = 1.0;
            }
            else if (eq5dPrediction < -0.594) {
                demLifeSatEQ5D = -0.594;
            }
            else {
                demLifeSatEQ5D = eq5dPrediction;
            }
        }

    }


    /**
     * Constrains GHQ12 Level estimates to valid range, 0-36
     *
     * @param healthWbScore0to36 predicted GHQ12 Level score
     * @return {@code Person.healthWbScore0to36}  Level score constrained to 0-36
     */
    protected Double constrainDhmEstimate(Double healthWbScore0to36) {
        if (healthWbScore0to36 < 0.) {
            healthWbScore0to36 = 0.;
        } else if (healthWbScore0to36 > 36.) {
            healthWbScore0to36 = 36.;
        }
        return healthWbScore0to36;
    }

    /**
     * Constrains GHQ12 Caseness estimates to valid range, 0-12
     *
     * @param healthPsyDstrssFlag predicted GHQ12 Caseness score
     * @return {@code Person.healthPsyDstrss} Caseness score constrained to 0-12
     */
    protected Double constrainhealthPsyDstrssEstimate(Double healthPsyDstrssFlag) {
        if (healthPsyDstrssFlag < 0.) {
            healthPsyDstrssFlag = 0.;
        } else if (healthPsyDstrssFlag > 12.) {
            healthPsyDstrssFlag = 12.;
        }
        return healthPsyDstrssFlag;
    }


    /**
     * Constrains SF12 (MCS or PCS) estimates to valid range, 0-100
     *
     * @param sf12 predicted MCS or PCS score
     * @return {@code Person.healthMentalMcs} or {@code Person.healthPhysicalPcs} score constrained to 0-100
     */
    protected Double constrainSF12Estimate(double sf12) {
        if (sf12 < 0.) {
            sf12 = 0.;
        } else if (sf12 > 100.) {
            sf12 = 100.;
        }
        return sf12;
    }

    /**
     * Constrains life satisfaction score to valid range, 0-10
     *
     * @param dls_estimate predicted life satisfaction score
     * @return {@code Person.demLifeSatScore1to10} score constrained to 0-10
     */
    protected Double constrainLifeSatisfactionEstimate(double dls_estimate) {
        if (!Parameters.checkFinite(dls_estimate)) {
            return null;
        }

        if (dls_estimate < 1.) {
            dls_estimate = 1.;
        } else if (dls_estimate > 10.) {
            dls_estimate = 10.;
        }

        return dls_estimate;
    }

    //Health process defines health using H1a or H1b process
    protected void health() {

        double healthInnov1 = statInnovations.getDoubleDraw(3);
        double healthInnov2 = statInnovations.getDoubleDraw(4);
        if((demAge >= 16 && demAge <= 29) && Les_c4.Student.equals(labC4) && eduLeftEduFlag == false) {
            //If age is between 16 - 29 and individual has always been in education, follow process H1a:

            Map<Dhe,Double> probs = ManagerRegressions.getProbabilities(this, RegressionName.HealthH1a);
            MultiValEvent event = new MultiValEvent(probs, healthInnov1);
            healthSelfRated = (Dhe) event.eval();
            if (event.isProblemWithProbs())
                model.addCounterErrorH1a();
        } else if (demAge >= 16) {

            Map<Dhe,Double> probs = ManagerRegressions.getProbabilities(this, RegressionName.HealthH1b);
            MultiValEvent event = new MultiValEvent(probs, healthInnov1);
            healthSelfRated = (Dhe) event.eval();
            if (event.isProblemWithProbs())
                model.addCounterErrorH1b();

            //If age is over 16 and individual is not in continuous education, also follow process H2b to calculate the probability of long-term sickness / disability:
            boolean becomeLTSickDisabled = false;
            if (!Parameters.enableIntertemporalOptimisations || DecisionParams.flagDisability) {

                double prob = Parameters.getRegHealthH2b().getProbability(this, Person.DoublesVariables.class);
                becomeLTSickDisabled = (healthInnov2 < prob);
            }
            if (becomeLTSickDisabled) {
                healthDsblLongtermFlag = Indicator.True;
            } else {
                healthDsblLongtermFlag = Indicator.False;
            }
        }
    }

    protected void evaluateSocialCareReceipt() {

        if (demAge < Parameters.MIN_AGE_FORMAL_SOCARE || getYear()>getStartYear()) {

            careNeedFlag = Indicator.False;
            careHrsFormalWeek = 0.0;
            xCareFormalWeek = 0.0;
            careHrsFromPartnerWeek = 0.0;
            careHrsFromParentWeek = 0.0;
            careHrsFromDaughterWeek = 0.0;
            careHrsFromSonWeek = 0.0;
            careHrsFromOtherWeek = 0.0;
            careReceivedFlag = SocialCareReceipt.None;
            careFormalFlag = false;
            careFromPartnerFlag = false;
            careFromDaughterFlag = false;
            careFromSonFlag = false;
            careFromOtherFlag = false;
        }
        if (!Parameters.checkFinite(careHrsFromParentWeek))
            careHrsFromParentWeek = 0.0;

        if ((demAge < Parameters.MIN_AGE_FORMAL_SOCARE) && Indicator.True.equals(healthDsblLongtermFlag)) {
            // under 65 years old with disability

            careNeedFlag = Indicator.True;
            double probRecCare;
            if (Indicator.False.equals(healthDsblLongtermFlagL1) || getYear()==getStartYear()) {
                // need to identify receipt of social care

                probRecCare = Parameters.getRegReceiveCareS1a().getProbability(this, Person.DoublesVariables.class);
            } else {
                // persist preceding receipt

                if (getTotalHoursSocialCare_L1()>0.5) {
                    probRecCare = 1.1;
                } else {
                    probRecCare = -0.1;
                }
            }

            if (statInnovations.getDoubleDraw(5) < probRecCare) {
                // receive social care

                double score = Parameters.getRegCareHoursS1b().getScore(this,Person.DoublesVariables.class);
                double rmse = Parameters.getRMSEForRegression("S1b");
                double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(statInnovations.getDoubleDraw(6));
                double careHours = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.exp(score + rmse * gauss));
                Person partner = getPartner();
                if (partner!=null && partner.getDemAge() < 75) {
                    careFromPartnerFlag = true;
                    careHrsFromPartnerWeek = careHours;
                } else if (demAge < 50) {
                    careFromOtherFlag = true;
                    careHrsFromParentWeek = careHours;
                } else {
                    careFromOtherFlag = true;
                    careHrsFromOtherWeek = careHours;
                }
            }
        }

        if (demAge >= Parameters.MIN_AGE_FORMAL_SOCARE && getYear()>getStartYear()) {
            // need care only projected for 65 and over due to limitations of data used for parameterisation

            double probNeedCare = Parameters.getRegNeedCareS2a().getProbability(this, Person.DoublesVariables.class);
            double recCareInnov = statInnovations.getDoubleDraw(7);
            if (recCareInnov < probNeedCare) {
                // need care
                careNeedFlag = Indicator.True;
            }

            double probRecCare = Parameters.getRegReceiveCareS2b().getProbability(this, Person.DoublesVariables.class);
            if (recCareInnov < probRecCare) {
                // receive care

                Map<SocialCareReceiptS2c,Double> probs1 = Parameters.getRegSocialCareMarketS2c().getProbabilities(this, Person.DoublesVariables.class);
                MultiValEvent event = new MultiValEvent(probs1, statInnovations.getDoubleDraw(8));
                SocialCareReceiptS2c socialCareReceiptS2c = (SocialCareReceiptS2c) event.eval();
                careReceivedFlag = SocialCareReceipt.getCode(socialCareReceiptS2c);
                if (SocialCareReceipt.Mixed.equals(careReceivedFlag) || SocialCareReceipt.Formal.equals(careReceivedFlag))
                    careFormalFlag = true;

                if (SocialCareReceipt.Mixed.equals(careReceivedFlag) || SocialCareReceipt.Informal.equals(careReceivedFlag)) {
                    // some informal care received

                    if (getPartner()!=null) {
                        // check if receive care from partner

                        double probPartnerCare = Parameters.getRegReceiveCarePartnerS2d().getProbability(this, Person.DoublesVariables.class);
                        if (statInnovations.getDoubleDraw(9) < probPartnerCare) {
                            // receive care from partner - check for supplementary carers

                            careFromPartnerFlag = true;
                            Map<PartnerSupplementaryCarer,Double> probs2 =
                                    Parameters.getRegPartnerSupplementaryCareS2e().getProbabilities(this, Person.DoublesVariables.class);
                            event = new MultiValEvent(probs2, statInnovations.getDoubleDraw(10));
                            PartnerSupplementaryCarer cc = (PartnerSupplementaryCarer) event.eval();
                            if (PartnerSupplementaryCarer.Daughter.equals(cc))
                                careFromDaughterFlag = true;
                            if (PartnerSupplementaryCarer.Son.equals(cc))
                                careFromSonFlag = true;
                            if (PartnerSupplementaryCarer.Other.equals(cc))
                                careFromOtherFlag = true;
                        }
                    }
                    if (!careFromPartnerFlag) {
                        // no care from partner - identify who supplies informal care

                        Map<NotPartnerInformalCarer,Double> probs2 =
                                Parameters.getRegNotPartnerInformalCareS2f().getProbabilities(this, Person.DoublesVariables.class);
                        event = new MultiValEvent(probs2, statInnovations.getDoubleDraw(11));
                        NotPartnerInformalCarer cc = (NotPartnerInformalCarer) event.eval();
                        if (NotPartnerInformalCarer.DaughterOnly.equals(cc) || NotPartnerInformalCarer.DaughterAndSon.equals(cc) || NotPartnerInformalCarer.DaughterAndOther.equals(cc))
                            careFromDaughterFlag = true;
                        if (NotPartnerInformalCarer.SonOnly.equals(cc) || NotPartnerInformalCarer.DaughterAndSon.equals(cc) || NotPartnerInformalCarer.SonAndOther.equals(cc))
                            careFromSonFlag = true;
                        if (NotPartnerInformalCarer.OtherOnly.equals(cc) || NotPartnerInformalCarer.SonAndOther.equals(cc) || NotPartnerInformalCarer.DaughterAndOther.equals(cc))
                            careFromOtherFlag = true;
                    }
                }
                double careHoursInnov = statInnovations.getDoubleDraw(12);
                if (careFromPartnerFlag) {
                    double score = Parameters.getRegPartnerCareHoursS2g().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2g");
                    double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                    careHrsFromPartnerWeek = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.exp(score + rmse * gauss));
                }
                careHoursInnov = Parameters.updateProbability(careHoursInnov);
                if (careFromDaughterFlag) {
                    double score = Parameters.getRegDaughterCareHoursS2h().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2h");
                    double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                    careHrsFromDaughterWeek = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.exp(score + rmse * gauss));
                }
                careHoursInnov = Parameters.updateProbability(careHoursInnov);
                if (careFromSonFlag) {
                    double score = Parameters.getRegSonCareHoursS2i().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2i");
                    double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                    careHrsFromSonWeek = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.exp(score + rmse * gauss));
                }
                careHoursInnov = Parameters.updateProbability(careHoursInnov);
                if (careFromOtherFlag) {
                    double score = Parameters.getRegOtherCareHoursS2j().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2j");
                    double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                    careHrsFromOtherWeek = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.exp(score + rmse * gauss));
                }
                careHoursInnov = Parameters.updateProbability(careHoursInnov);
                if (careFormalFlag) {
                    double score = Parameters.getRegFormalCareHoursS2k().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2k");
                    double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                    careHrsFormalWeek = Math.min(Parameters.MAX_HOURS_WEEKLY_FORMAL_CARE, Math.exp(score + rmse * gauss));
                    xCareFormalWeek = careHrsFormalWeek * Parameters.getTimeSeriesValue(model.getYear(), TimeSeriesVariable.CarerWageRate);
                }
            }
        }
        if (Parameters.flagSuppressSocialCareCosts)
            xCareFormalWeek = 0.0;
    }

    protected void evaluateSocialCareProvision() {
        evaluateSocialCareProvision(Parameters.getTimeSeriesValue(model.getYear(),TimeSeriesVariable.CareProvisionAdjustment));
    }

    public void evaluateSocialCareProvision(double probitAdjustment) {

        careProvidedFlag = SocialCareProvision.None;
        careHrsProvidedWeek = 0.0;
        boolean careToPartner = false;
        boolean careToOther;
        double careHoursToPartner = 0.0;
        if (demAge >= Parameters.AGE_TO_BECOME_RESPONSIBLE) {

            // check if care provided to partner
            // identified in method evaluateSocialCareReceipt
            Person partner = getPartner();
            if (partner!=null && partner.careFromPartnerFlag) {
                careToPartner = true;
                careHoursToPartner = partner.getCareHoursFromPartnerWeekly();
            }

            // check if care provided to "other"
            double prob;
            if (careToPartner) {
                double score = Parameters.getRegCarePartnerProvCareToOtherS3a().getScore(this, Person.DoublesVariables.class);
                prob = Parameters.getRegCarePartnerProvCareToOtherS3a().getProbability(score + probitAdjustment);
            } else {
                double score = Parameters.getRegNoCarePartnerProvCareToOtherS3b().getScore(this, Person.DoublesVariables.class);
                prob = Parameters.getRegNoCarePartnerProvCareToOtherS3b().getProbability(score + probitAdjustment);
            }
            careToOther = (statInnovations.getDoubleDraw(13) < prob);

            // update care provision states
            if (careToPartner || careToOther) {

                if (careToPartner && careToOther) {
                    careProvidedFlag = SocialCareProvision.PartnerAndOther;
                } else if (careToPartner) {
                    careProvidedFlag = SocialCareProvision.OnlyPartner;
                } else {
                    careProvidedFlag = SocialCareProvision.OnlyOther;
                }
                if (!Parameters.flagSuppressSocialCareCosts) {
                    if (SocialCareProvision.OnlyPartner.equals(careProvidedFlag)) {
                        careHrsProvidedWeek = careHoursToPartner;
                    } else {
                        double score = Parameters.getRegCareHoursProvS3e().getScore(this,Person.DoublesVariables.class);
                        double rmse = Parameters.getRMSEForRegression("S3e");
                        double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(statInnovations.getDoubleDraw(14));
                        careHrsProvidedWeek = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE,
                                Math.max(careHoursToPartner + 1.0, Math.exp(score + rmse * gauss)));
                    }
                }
            }
        }
    }

    public void cohabitation() {
        double probitAdjustment = (model.isAlignCohabitation()) ? Parameters.getAlignmentValue(getYear(), AlignmentVariable.PartnershipAlignment) : 0.0;
        cohabitation(probitAdjustment);
    }

    protected void cohabitation(double probitAdjustment) {

        // parameter check
        if (probitAdjustment>(4.0+1.0E-5) || probitAdjustment<(-4.0-1.0E-5))
            throw new RuntimeException("odd value for probit adjustment supplied to considerCohabitation method: " + probitAdjustment);

        demBePartnerFlag = false;
        demLeavePartnerFlag = false;
        demAlignPartnerProcess = false;
        double cohabitInnov = statInnovations.getDoubleDraw(25);
        Person partner = getPartner();
        if (demAge >= Parameters.MIN_AGE_COHABITATION) {
            // cohabitation possible

            if (model.getCountry() == Country.UK) {

                double prob;
                if (partner == null) {
                    // partnership formation

                    if (demAge <= 29 && labC4 == Les_c4.Student && !eduLeftEduFlag) {

                        double score = Parameters.getRegPartnershipU1a().getScore(this, Person.DoublesVariables.class);
                        prob = Parameters.getRegPartnershipU1a().getProbability(score + probitAdjustment);
                    } else {

                        double score = Parameters.getRegPartnershipU1b().getScore(this, Person.DoublesVariables.class);
                        prob = Parameters.getRegPartnershipU1b().getProbability(score + probitAdjustment);
                    }
                    demBePartnerFlag = (cohabitInnov < prob);
                    if (demBePartnerFlag)
                        model.getPersonsToMatch().get(demMaleFlag).get(getRegion()).add(this);
                } else if (demMaleFlag == Gender.Female && (demAge > 29 || !Les_c4.Student.equals(labC4) || eduLeftEduFlag)) {
                    // partnership dissolution

                    double score = Parameters.getRegPartnershipU2b().getScore(this, Person.DoublesVariables.class);
                    prob = Parameters.getRegPartnershipU2b().getProbability(score - probitAdjustment);
                    if (cohabitInnov < prob) {
                        demLeavePartnerFlag = true;
                    }
                }
            } else if (model.getCountry() == Country.IT) {

                if (partner == null) {
                    if ((labC4 == Les_c4.Student && eduLeftEduFlag) || !labC4.equals(Les_c4.Student)) {

                        double prob = Parameters.getRegPartnershipITU1().getProbability(this, Person.DoublesVariables.class);
                        demBePartnerFlag = (cohabitInnov < prob);
                        if (demBePartnerFlag)
                            model.getPersonsToMatch().get(demMaleFlag).get(getRegion()).add(this);
                    }
                } else if (partner != null && demMaleFlag == Gender.Female && ((labC4 == Les_c4.Student && eduLeftEduFlag) || !labC4.equals(Les_c4.Student))) {

                    double prob = Parameters.getRegPartnershipITU2().getProbability(this, Person.DoublesVariables.class);
                    if (cohabitInnov < prob) {
                        demLeavePartnerFlag = true;
                    }
                }
            }
        }
    }

    protected void partnershipDissolution() {

        if (demLeavePartnerFlag) {

            // update partner's variables first
            Person partner = getPartner();
            partner.setDemPartnerNYear(null);
            partner.setDemLeftPartnerFlag(true); //Set to true if leaves partnership to use with fertility regression, this is never reset

            setDemPartnerNYear(null); 		  //Set number of years in partnership to null if leaving partner
            setDemLeftPartnerFlag(true); //Set to true if leaves partnership to use with fertility regression, this is never reset
            idHh = null;

            setupNewBenefitUnit(true);
        }
    }

    protected void inSchool() {

        double labourInnov = statInnovations.getDoubleDraw(24);
        //Min age to leave education set to 16 (from 18 previously) but note that age to leave home is 18.
        if (Les_c4.Retired.equals(labC4) || demAge < Parameters.MIN_AGE_TO_LEAVE_EDUCATION || demAge > Parameters.MAX_AGE_TO_ENTER_EDUCATION) {		//Only apply module for persons who are old enough to consider leaving education, but not retired
            return;
        } else if (Les_c4.Student.equals(labC4) && !eduLeftEduFlag && demAge >= Parameters.MIN_AGE_TO_LEAVE_EDUCATION) { //leftEducation is initialised to false and updated to true when individual leaves education (and never reset).
            //If age is between 16 - 29 and individual has always been in education, follow process E1a:

            if (demAge <= Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION) {

                double prob = Parameters.getRegEducationE1a().getProbability(this, Person.DoublesVariables.class);
                eduLeaveSchoolFlag = (labourInnov >= prob); //If event is true, stay in school.  If event is false, leave school.
            } else {
                eduLeaveSchoolFlag = true; //Hasn't left education until 30 - force out
            }
        } else if (demAge <= 45 && (!Les_c4.Student.equals(labC4) || eduLeftEduFlag)) { //leftEducation is initialised to false and updated to true when individual leaves education for the first time (and never reset).
            //If age is between 16 - 45 and individual has not continuously been in education, follow process E1b:
            //Either individual is currently a student and has left education at some point in the past (so returned) or individual is not a student so has not been in continuous education:
            //TODO: If regression outcome of process E1b is true, set activity status to student and der (return to education indicator) to true?

            double prob = Parameters.getRegEducationE1b().getProbability(this, Person.DoublesVariables.class);
            if (labourInnov < prob) {
                //If event is true, re-enter education.  If event is false, leave school

                setLes_c4(Les_c4.Student);
                setDer(Indicator.True);
                setDed(Indicator.True);
            } else if (Les_c4.Student.equals(labC4)){
                //If activity status is student but regression to be in education was evaluated to false, remove student status

                setLes_c4(Les_c4.NotEmployed);
                setDed(Indicator.False);
                eduLeaveSchoolFlag = true; //Test what happens if people who returned to education leave again
            }
        } else if (demAge > 45 && labC4.equals(Les_c4.Student)) {
            //People above 45 shouldn't be in education, so if someone re-entered at 45 in previous step, force out

            setLes_c4(Les_c4.NotEmployed);
            setDed(Indicator.False);
        }
    }

    protected void leavingSchool() {

        if (eduLeaveSchoolFlag) {

            setEducationLevel(); //If individual leaves school follow process E2a to assign level of education
            setSedex(Indicator.True); //Set variable left education (sedex) if leaving school
            setDed(Indicator.False); //Set variable in education (ded) to false if leaving school
            setDer(Indicator.False);
            setEduLeftEduFlag(true); //This is not reset and indicates if individual has ever left school - used with health process
            setLes_c4(Les_c4.NotEmployed); //Set activity status to NotEmployed when leaving school to remove Student status
        }
    }


    private void giveBirth() {				//To be called once per year after fertility alignment

        if (demGiveBirthFlag) {		//toGiveBirth is determined by fertility process

            Gender babyGender = (statInnovations.getDoubleDraw(27) < Parameters.PROB_NEWBORN_IS_MALE) ? Gender.Male : Gender.Female;

            //Give birth to new person and add them to benefitUnit.
            Person child = new Person(babyGender, this);
            model.getPersons().add(child);
            benefitUnit.getMembers().add(child);
        }
    }


    protected void initialisePotentialHourlyEarnings() {

        double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(statInnovations.getDoubleDraw(15));
        double logPotentialHourlyEarnings, score, rmse;
        if (demMaleFlag.equals(Gender.Male)) {
            score = Parameters.getRegWagesMales().getScore(this, Person.DoublesVariables.class);
            rmse = Parameters.getRMSEForRegression("Wages_Males");
        } else {
            score = Parameters.getRegWagesFemales().getScore(this, Person.DoublesVariables.class);
            rmse = Parameters.getRMSEForRegression("Wages_Females");
        }
        logPotentialHourlyEarnings = score + rmse * gauss;
        double upratedLevelPotentialHourlyEarnings = Math.exp(logPotentialHourlyEarnings);
        setLabWageFullTimeHrly(upratedLevelPotentialHourlyEarnings);
        setLabWageFullTimeHrlyL1(upratedLevelPotentialHourlyEarnings);
    }


    protected void updateFullTimeHourlyEarnings() {

        double rmse, wagesInnov = statInnovations.getDoubleDraw(16);
        if (Les_c4.EmployedOrSelfEmployed.equals(labC4L1)) {
            if (labWageRegressRandomCompoponentEmp == null || !model.fixRegressionStochasticComponent) {
                if (Gender.Male.equals(demMaleFlag)) {
                    rmse = Parameters.getRMSEForRegression("Wages_MalesE");
                } else {
                    rmse = Parameters.getRMSEForRegression("Wages_FemalesE");
                }
                double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(wagesInnov);
                labWageRegressRandomCompoponentEmp = rmse * gauss;
            }
        } else {
            if (labWageRegressRandomCompoponentNotEmp == null || !model.fixRegressionStochasticComponent) {
                if (Gender.Male.equals(demMaleFlag)) {
                    rmse = Parameters.getRMSEForRegression("Wages_MalesNE");
                } else {
                    rmse = Parameters.getRMSEForRegression("Wages_FemalesNE");
                }
                double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(wagesInnov);
                labWageRegressRandomCompoponentNotEmp = rmse * gauss;
            }
        }

        double logFullTimeHourlyEarnings;
        if(Gender.Male.equals(demMaleFlag)) {
            if (Les_c4.EmployedOrSelfEmployed.equals(labC4L1)) {
                logFullTimeHourlyEarnings = Parameters.getRegWagesMalesE().getScore(this, Person.DoublesVariables.class) + labWageRegressRandomCompoponentEmp;
            } else {
                logFullTimeHourlyEarnings = Parameters.getRegWagesMalesNE().getScore(this, Person.DoublesVariables.class) + labWageRegressRandomCompoponentNotEmp;
            }
        } else {
            if (Les_c4.EmployedOrSelfEmployed.equals(labC4L1)) {
                logFullTimeHourlyEarnings = Parameters.getRegWagesFemalesE().getScore(this, Person.DoublesVariables.class) + labWageRegressRandomCompoponentEmp;
            } else {
                logFullTimeHourlyEarnings = Parameters.getRegWagesFemalesNE().getScore(this, Person.DoublesVariables.class) + labWageRegressRandomCompoponentNotEmp;
            }
        }

        // Uprate and set level of potential earnings
        double upratedFullTimeHourlyEarnings = Math.exp(logFullTimeHourlyEarnings);
        if (upratedFullTimeHourlyEarnings < Parameters.MIN_HOURLY_WAGE_RATE) {
            setLabWageFullTimeHrly(Parameters.MIN_HOURLY_WAGE_RATE);
        } else if (upratedFullTimeHourlyEarnings > Parameters.MAX_HOURLY_WAGE_RATE) {
            setLabWageFullTimeHrly(Parameters.MAX_HOURLY_WAGE_RATE);
        } else {
            setLabWageFullTimeHrly(upratedFullTimeHourlyEarnings);
        }
    }
    public void setyCapitalPersMonth(double val) {
        yCapitalPersMonth = val;
    }
    public void setyPensPersGrossMonth(double val) {
        yPensPersGrossMonth = val;
    }
    public double getPensionIncomeAnnual() {
        return Math.sinh(yPensPersGrossMonth)*12.0;
    }
    private double setIncomeBySource(double score, double rmse, IncomeSource source, RegressionScoreType scoreType) {

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
        double capitalInnov = statInnovations.getDoubleDraw(17);
        while (redraw) {

            gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(capitalInnov);
            double incomeVal = 0.;
            if (RegressionScoreType.Asinh.equals(scoreType)) {

                incomeVal = Math.sinh( score + rmse * gauss );
            } else if (RegressionScoreType.Level.equals(scoreType)) {

                incomeVal = score + rmse * gauss;
            }
            income = Math.max(minInc, incomeVal);
            if (income < maxInc) {
                redraw = false;
            } else {
                capitalInnov /= 2.0;
            }
        }
        return income;
    }
    protected void updateNonLabourIncome() {
        
        if (Parameters.enableIntertemporalOptimisations)
            throw new RuntimeException("request to update non-labour income in person object when wealth is explicit");

        // ypncp: inverse hyperbolic sine of capital income per month
        // ypnoab: inverse hyperbolic sine of pension income per month
        // yptciihs_dv: inverse hyperbolic sine of capital and pension income per month
        // variables updated with labour supply when enableIntertemporalOptimisations (as retirement can affect wealth and pension income)
        if (demAge >= Parameters.MIN_AGE_TO_HAVE_INCOME) {

            double capitalInnov = statInnovations.getDoubleDraw(18);
            if (demAge <= 29 && Les_c4.Student.equals(labC4) && !eduLeftEduFlag) {
                // full-time students

                double prob = Parameters.getRegIncomeI3a_selection().getProbability(this, Person.DoublesVariables.class);
                boolean hasCapitalIncome = (capitalInnov < prob);
                if (hasCapitalIncome) {

                    double score = Parameters.getRegIncomeI3a().getScore(this, Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("I3a");
                    double capinclevel = setIncomeBySource(score, rmse, IncomeSource.CapitalIncome, RegressionScoreType.Asinh);
                    yCapitalPersMonth = Parameters.asinh(capinclevel); //Capital income amount
                }
                else yCapitalPersMonth = 0.; //If no capital income, set amount to 0
            } else if (eduLeftEduFlag || !Les_c4.Student.equals(labC4)) {

                double prob = Parameters.getRegIncomeI3b_selection().getProbability(this, Person.DoublesVariables.class);
                boolean hasCapitalIncome = (capitalInnov < prob);
                if (hasCapitalIncome) {

                    double score = Parameters.getRegIncomeI3b().getScore(this, Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("I3b");
                    double capinclevel = setIncomeBySource(score, rmse, IncomeSource.CapitalIncome, RegressionScoreType.Asinh);
                    yCapitalPersMonth = Parameters.asinh(capinclevel); //Capital income amount
                }
                else yCapitalPersMonth = 0.; //If no capital income, set amount to 0
            }
            if (Les_c4.Retired.equals(labC4)) {
                // Retirement decision is modelled in the retirement process. Here only the amount of pension income for retired individuals is modelled.
                /*
                    Private pension income when individual was retired in the previous period is modelled using process I4b.

                    Private pension income when individual moves from non-retirement to retirement is modelled using:
                    i) process I5a_selection, to determine who receives private pension income
                    ii) process I5a_amount, for those who are determined to receive private pension income by process I5a_selection. I5a_amount is modelled in levels using linear regression.
                */

                double score, rmse, pensionIncLevel = 0.;
                if (Les_c4.Retired.equals(labC4L1)) {
                    // If person was retired in the previous period (and the simulation is not in its initial year), use process I4b

                    score = Parameters.getRegIncomeI4b().getScore(this, Person.DoublesVariables.class);
                    rmse = Parameters.getRMSEForRegression("I4b");
                    pensionIncLevel = setIncomeBySource(score, rmse, IncomeSource.PrivatePension, RegressionScoreType.Asinh);
                } else {
                    // For individuals in the first year of retirement, use processes I5a_selection and I5a

                    double prob = Parameters.getRegIncomeI5a_selection().getProbability(this, Person.DoublesVariables.class);
                    boolean hasPrivatePensionIncome = (statInnovations.getDoubleDraw(19) < prob);
                    if (hasPrivatePensionIncome) {

                        score = Parameters.getRegIncomeI5a().getScore(this, Person.DoublesVariables.class);
                        rmse = Parameters.getRMSEForRegression("I5a");
                        pensionIncLevel = setIncomeBySource(score, rmse, IncomeSource.PrivatePension, RegressionScoreType.Level);
                    }
                }
                yPensPersGrossMonth = Parameters.asinh(pensionIncLevel);
            }
        }

        double capital_income_multiplier = model.getSavingRate()/Parameters.SAVINGS_RATE;
        double yptciihs_dv_tmp_level = capital_income_multiplier*(Math.sinh(yCapitalPersMonth) + Math.sinh(yPensPersGrossMonth)); //Multiplied by the capital income multiplier, defined as chosen savings rate divided by the long-term average (specified in Parameters class)
        yMiscPersGrossMonth = Parameters.asinh(yptciihs_dv_tmp_level); //Non-employment non-benefit income is the sum of capital income and, for retired individuals, pension income.

        if (yMiscPersGrossMonth > 13.0) {
            yMiscPersGrossMonth = 13.5;
        }
        if (Parameters.enableIntertemporalOptimisations)
            throw new RuntimeException("request to update non-labour income in person object when wealth is explicit");
    }


    // ---------------------------------------------------------------------
    // Other Methods
    // ---------------------------------------------------------------------
    public boolean atRiskOfWork() {

        /*
        Person "flexible in labour supply" must meet the following conditions:
        age >= 16 and <= 75
        not a student or retired
        not disabled
         */

        if (demAge < Parameters.MIN_AGE_FLEXIBLE_LABOUR_SUPPLY)
            return false;
        if (demAge > Parameters.MAX_AGE_FLEXIBLE_LABOUR_SUPPLY)
            return false;
        if (Les_c4.Retired.equals(labC4) && !Parameters.enableIntertemporalOptimisations)
            return false;
        if (Les_c4.Student.equals(labC4) && !Parameters.enableIntertemporalOptimisations)
            return false;
        if (Indicator.True.equals(healthDsblLongtermFlag) && !Parameters.flagSuppressSocialCareCosts)
            return false;
        if (Indicator.True.equals(careNeedFlag) && !Parameters.flagSuppressSocialCareCosts)
            return false;

        //For cases where the participation equation used for the Heckmann Two-stage correction of the wage equation results in divide by 0 errors.
        //These people will not work for any wage (their activity status will be set to Nonwork in the Labour Market Module
        Double inverseMillsRatio = getInverseMillsRatio();
        if (!Double.isFinite(inverseMillsRatio))
            return false;

        return true;		//Else return true
    }


    // Assign education level to school leavers using MultiProbitRegression
    // Note that persons are now assigned a Low education level by default at birth (to prevent null pointer exceptions when persons become old enough to marry while still being a student
    // (we now allow students to marry, given they can re-enter school throughout their lives).
    // The module only applies to students who are leaving school (activityStatus == Student and toLeaveSchool == true) - see inSchool()
    private void setEducationLevel() {

        Map<Education,Double> probs = Parameters.getRegEducationE2a().getProbabilities(this, Person.DoublesVariables.class);
        MultiValEvent event = new MultiValEvent(probs, statInnovations.getDoubleDraw(30));
        Education newEducationLevel = (Education) event.eval();

        //Education has been set to Low by default for all new born babies, so it should never be null.
        //This is because we no longer prevent people in school to get married, given that people can re-enter education throughout their lives.
        //Note that by not filtering out students, we must assign a low education level by default to persons at birth to prevent a null pointer exception when new born persons become old enough to marry if they have not yet left school because
        //their education level has not yet been assigned.
        if (newEducationLevel.equals(Education.Low)) {
            model.lowEd++;
        } else if (newEducationLevel.equals(Education.Medium)) {
            model.medEd++;
        } else if (newEducationLevel.equals(Education.High)) {
            model.highEd++;
        } else {
            model.nothing++;
        }
        if(eduHighestC3 != null) {
            if(newEducationLevel.ordinal() > eduHighestC3.ordinal()) {		//Assume Education level cannot decrease after re-entering school.
                eduHighestC3 = newEducationLevel;
            }
        } else {
            eduHighestC3 = newEducationLevel;
        }
    }

    public double getLiquidWealth() {

        if (demAge <= Parameters.AGE_TO_BECOME_RESPONSIBLE) {
            return 0.0;
        } else if (benefitUnit != null) {
            return (Occupancy.Couple.equals(benefitUnit.getOccupancy())) ? benefitUnit.getWealthTotValue() / 2.0 : benefitUnit.getWealthTotValue();
        } else {
            throw new RuntimeException("Call to get liquid wealth for person object without benefit unit assigned");
        }
    }

    protected void projectEquivConsumption() {

        if (Parameters.enableIntertemporalOptimisations) {

            xEquivYear = benefitUnit.getDiscretionaryConsumptionPerYear() / benefitUnit.getEquivalisedWeight();
        } else {

            if (getLes_c4().equals(Les_c4.Retired)) {
                xEquivYear = benefitUnit.getEquivalisedDisposableIncomeYearly();
            } else {
                xEquivYear = Math.max(0., (1-model.getSavingRate())*benefitUnit.getEquivalisedDisposableIncomeYearly());
            }
        }
    }

    protected void updateVariables(boolean initialUpdate) {

        //Reset flags to default values
        eduLeaveSchoolFlag = false;
        demGiveBirthFlag = false;
        demBePartnerFlag = false;
        demLeavePartnerFlag = false;
        eduSpellFlag = (Les_c4.Student.equals(labC4)) ? Indicator.True : Indicator.False;
        if (initialUpdate && !Parameters.checkFinite(careHrsFromParentWeek))
            careHrsFromParentWeek = 0.0;
        if (demAge <Parameters.AGE_TO_BECOME_RESPONSIBLE) {
            Person mother = benefitUnit.getFemale();
            if (mother!=null)
                idMother = mother.getId();
            else
                idMother = null;
            Person father = benefitUnit.getMale();
            if (father!=null)
                idFather = father.getId();
            else
                idFather = null;
        }

        // Mental and Physical health status of the partner
        if (this.getPartner() != null) {
            this.setHealthMentalPartnerMcs(getPartner().getHealthMentalMcs());
            this.setHealthPhysicalPartnerPcs(getPartner().getHealthPhysicalPcs());
        }

        //Lagged variables
        updateLaggedVariables(initialUpdate);

        // generate year specific random draws
        if (!initialUpdate) {
            if (Parameters.enableIntertemporalOptimisations && !DecisionParams.flagDisability) {
                healthDsblLongtermFlag = Indicator.False;
                healthDsblLongtermFlagL1 = Indicator.False;
            }
            if (!Parameters.flagSocialCare) {
                setAllSocialCareVariablesToFalse();
            }
            statInnovations.getNewDoubleDraws();
        }
    }

    private void updateOutputVariables() {
        idPartner = getPartnerID();
        demPartnerStatus = getDcpst();
    }

    private void updateLaggedVariables(boolean initialUpdate) {

        labC4L1 = labC4;
        labC7CovidL1 = labC7Covid;
        demStatusHhL1 = getHouseholdStatus();
        healthSelfRatedL1 = healthSelfRated; //Update lag(1) of health
        healthWbScore0to36L1 = healthWbScore0to36; //Update lag(1) of mental health
        healthPsyDstrssL1 = healthPsyDstrss;
        demLifeSatScore1to7L1 = demLifeSatScore1to7;
        healthMentalMcsL1 = healthMentalMcs;
        healthPhysicalPcsL1 = healthPhysicalPcs;
        demPrptyFlagL1 = getBenefitUnit().isHousingOwned();
        healthDsblLongtermFlagL1 = healthDsblLongtermFlag; //Update lag(1) of long-term sick or disabled status
        careNeedFlagL1 = careNeedFlag;
        careHrsFormalWeekL1 = careHrsFormalWeek;
        careHrsFromPartnerWeekL1 = careHrsFromPartnerWeek;
        careHrsFromParentWeekL1 = careHrsFromParentWeek;
        careHrsFromDaughterWeekL1 = careHrsFromDaughterWeek;
        careHrsFromSonWeekL1 = careHrsFromSonWeek;
        careHrsFromOtherWeekL1 = careHrsFromOtherWeek;
        careProvidedFlagL1 = careProvidedFlag;
        labWageOfferLowFlagL1 = getLowWageOffer();
        eduHighestC3L1 = eduHighestC3; //Update lag(1) of education level
        yNonBenPersGrossMonthL1 = getyNonBenPersGrossMonth(); //Update lag(1) of gross personal non-benefit income
        labHrsWorkEnumWeekL1 = getLabourSupplyWeekly(); // Lag(1) of labour supply
        yBenReceivedFlagL1 = yBenReceivedFlag; // Lag(1) of flag indicating if individual receives benefits
        yBenNonUCReceivedFlagL1 = yBenNonUCReceivedFlag; // Lag(1) of flag indicating if individual receives non-UC benefits
        yBenUCReceivedFlagL1 = yBenUCReceivedFlag; // Lag(1) of flag indicating if individual receives UC
        labWageFullTimeHrlyL1 = labWageFullTimeHrly; // Lag(1) of potential hourly earnings

        if (initialUpdate) {
            yEmpPersGrossMonthL1 = getyEmpPersGrossMonth(); //Lag(1) of gross personal employment income
            yEmpPersGrossMonthL2 = getyEmpPersGrossMonth();
            yEmpPersGrossMonthL3 = getyEmpPersGrossMonth();

            yMiscPersGrossMonthL1 = getyMiscPersGrossMonth();
            yMiscPersGrossMonthL2 = getyMiscPersGrossMonth();
            yMiscPersGrossMonthL3 = getyMiscPersGrossMonth();

            yCapitalPersMonthL1 = getyCapitalPersMonth();
            yCapitalPersMonthL2 = getyCapitalPersMonth();

            yPensPersGrossMonthL1 = getyPensPersGrossMonth();
            yPensPersGrossMonthL2 = getyPensPersGrossMonth();
        } else {
            yEmpPersGrossMonthL3 = yEmpPersGrossMonthL2; //Lag(3) of gross personal employment income
            yEmpPersGrossMonthL2 = yEmpPersGrossMonthL1; //Lag(2) of gross personal employment income
            yEmpPersGrossMonthL1 = getyEmpPersGrossMonth(); //Lag(1) of gross personal employment income

            yMiscPersGrossMonthL3 = yMiscPersGrossMonthL2; //Lag(3) of gross personal non-employment non-benefit income
            yMiscPersGrossMonthL2 = yMiscPersGrossMonthL1; //Lag(2) of gross personal non-employment non-benefit income
            yMiscPersGrossMonthL1 = getyMiscPersGrossMonth(); //Lag(1) of gross personal non-employment non-benefit income

            yCapitalPersMonthL2 = yCapitalPersMonthL1;
            yCapitalPersMonthL1 = getyCapitalPersMonth();

            yPensPersGrossMonthL2 = yPensPersGrossMonthL1;
            yPensPersGrossMonthL1 = getyPensPersGrossMonth();
        }

        demPartnerStatusL2 = demPartnerStatusL1; // Updating of this lag must occur before parnters variables are updated

        // partner variables
        Person partner = getPartner();
        if (partner!=null) {
            eduHighestPartnerC3L1 = partner.eduHighestC3;
            healthPartnerSelfRatedL1 = partner.healthSelfRated;
            demPartnerStatusL1 = Dcpst.Partnered;
            demAgePartnerDiffL1 = demAge - partner.demAge;
            idPartnerL1 = partner.getId();
        } else {
            eduHighestPartnerC3L1 = null;
            healthPartnerSelfRatedL1 = null;
            demAgePartnerDiffL1 = null;
            demPartnerStatusL1 = getDcpst();
            idPartnerL1 = null;
        }
        yPersAndPartnerGrossDiffMonthL1 = getYnbcpdf_dv(); //Lag(1) of difference between own and partner's gross personal non-benefit income
        labStatusPartnerAndOwnC4L1 = getLesdf_c4(); //Lag(1) of own and partner's activity status
    }

    // used when children leave home
    protected void setupNewHousehold() {

        Household newHousehold = new Household();
        model.getHouseholds().add(newHousehold);
        benefitUnit.setHousehold(newHousehold);
        idHh = newHousehold.getId();
        idBu = benefitUnit.getId();
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
     */
    public void setupNewBenefitUnit(boolean automaticUpdateOfBenefitUnits) {
        setupNewBenefitUnit(null, automaticUpdateOfBenefitUnits);
    }
    public void setupNewBenefitUnit(Person partner, boolean automaticUpdateOfBenefitUnits) {

        // identify children transferring to new benefit unit
        // this needs to be done before new benefit units are created because the new benefit units
        // are instantiated with the transferred responsible adults, at which time the old benefit unit
        // references are also transferred
        Set<Person> childrenToNewBenefitUnit = childrenToFollowPerson(this);
        if (partner != null) {
            childrenToNewBenefitUnit.addAll(childrenToFollowPerson(partner));
        }

        Household newHousehold;
        BenefitUnit newBenefitUnit;
        if (partner != null) {
            // relationship forms
            // when a relationship forms newBenefitUnit is populated with data described by person and partner, both of whom
            // are re-assigned from their previous benefitUnits to newBenefitUnit and households to newHousehold

            newHousehold = new Household();
            newBenefitUnit = new BenefitUnit(this, partner);
        } else {
            // includes when reach age of maturity and when relationship dissolves.
            // When relationship dissolves person is the female, and this routine sets up a new benefit unit for the woman,
            // updates characteristics for both newBenefitUnit and benefitUnit, and then leaves benefitUnit to the man

            if (demAge ==Parameters.AGE_TO_BECOME_RESPONSIBLE) {
                newHousehold = benefitUnit.getHousehold();
            } else {
                newHousehold = new Household();
            }
            long seed = (long)(getBenefitUnitRandomUniform()*100000);
            newBenefitUnit = new BenefitUnit(this, seed);
            if (model.getBenefitUnits().contains(newBenefitUnit)) {
                throw new RuntimeException("New benefit unit already found in benefitUnits - Hint: Primary keys may be corrupted");
            }
        }

        // establish links between objects
        newBenefitUnit.setHousehold(newHousehold);
        for (Person child : childrenToNewBenefitUnit) {
            child.setBenefitUnit(newBenefitUnit);
        }
        if (partner!=null)
            partner.setBenefitUnit(newBenefitUnit);
        this.setBenefitUnit(newBenefitUnit);
        newBenefitUnit.initializeFields();


        // Automatic update of collections if required
        // Removing children (above) from the benefitUnit should not lead to the removal of the benefitUnit (due to becoming an empty benefitUnit)
        // because there should still be the responsible male or female (this person or partner) in the benefitUnit, which we shall now remove from
        // their benefitUnit below.
        //
        // automaticUpdateOfBenefitUnits is a toggle to prevent automatic update to the model's set of benefitUnits, which would cause concurrent
        // modification exception to be thrown if called within an iteration through benefitUnits.  In this case, set parameter to false and use the
        // iterator on the benefitUnits to add manually e.g. (houseIterator.add(newBU)).
        model.getHouseholds().add(newHousehold);
        if (automaticUpdateOfBenefitUnits) {

            model.getBenefitUnits().add(newBenefitUnit);	//This will cause concurrent modification if setupNewHome is called in an iteration through benefitUnits
        }
    }

    private Set<Person> childrenToFollowPerson(Person person) {
        // by default children follow their mother, but if no mother then they follow their father

        Set<Person> childrenToNewBenefitUnit = new LinkedHashSet<>();
        if (person.demAge <Parameters.AGE_TO_BECOME_RESPONSIBLE)
            throw new RuntimeException("problem identifying allocation of children to new benefit unit");
        if (Gender.Female.equals(person.demMaleFlag))
            for (Person child : person.getBenefitUnit().getChildren()) {
                if (child.getIdMother()!=null && person.getId()==child.getIdMother())
                    childrenToNewBenefitUnit.add(child);
            }
        else {
            for (Person child : person.getBenefitUnit().getChildren()) {
                if (child.idMother==null && child.getIdFather()!=null && person.getId()==child.getIdFather())
                    childrenToNewBenefitUnit.add(child);
            }
        }

        return childrenToNewBenefitUnit;
    }

    @Override
    public int compareTo(Person person) {
        if (benefitUnit==null || person.benefitUnit==null)
            throw new IllegalArgumentException("attempt to compare benefit units prior to initialisation");
        return (int) (benefitUnit.getId() - person.getBenefitUnit().getId());
    }

    /*
    This method takes les_c4 (which is a more aggregated version of labour force activity statuses) and returns les_c6 version.
    Used when setting initial values of les_c6 from existing les_c4.
    */
    public void initialise_les_c6_from_c4() {
        if (labC4.equals(Les_c4.EmployedOrSelfEmployed)) {
            labC7Covid = Les_c7_covid.Employee;
        } else if (labC4.equals(Les_c4.NotEmployed)) {
            labC7Covid = Les_c7_covid.NotEmployed;
        } else if (labC4.equals(Les_c4.Student)) {
            labC7Covid = Les_c7_covid.Student;
        } else if (labC4.equals(Les_c4.Retired)) {
            labC7Covid = Les_c7_covid.Retired;
        }

        //In the first period the lagged value will be equal to the contemporaneous value
        if (labC7CovidL1 == null) {
            labC7CovidL1 = labC7Covid;
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
            if (labC4 == null) return 0;		//For inactive people, who don't participate in the labour market
            else if (labC4.equals(Les_c4.EmployedOrSelfEmployed)) return 1;
            else return 0;		//For unemployed case

        case isNotEmployed:
            if (labC4 == null) return 0;
            else if (labC4.equals(Les_c4.NotEmployed)) return 1;
            else return 0;

        case isRetired:
            if (labC4 == null) return 0;
            else if (labC4.equals(Les_c4.Retired)) return 1;
            else return 0;

        case isStudent:
            if (labC4 == null) return 0;
            else if (labC4.equals(Les_c4.Student)) return 1;
            else return 0;

        case isNotEmployedOrRetired:
            if (labC4 == null) return 0;
            else if (labC4.equals(Les_c4.NotEmployed) || labC4.equals(Les_c4.Retired)) return 1;
            else return 0;

        case isToBePartnered:
            return (isToBePartnered())? 1 : 0;

        case isPsychologicallyDistressed:
            return (healthPsyDstrss >= 4)? 1 : 0;

        case isNeedSocialCare:
            return (Indicator.True.equals(careNeedFlag)) ? 1 : 0;

        default:
            throw new RuntimeException("Unsupported variable " + variableID.name() + " in Person#getIntValue");
        }
    }


    // ---------------------------------------------------------------------
    // implements IDoubleSource for use with Regression classes
    // ---------------------------------------------------------------------

    public enum DoublesVariables {
        // ORGANISED ALPHABETICALLY TO ASSIST IDENTIFICATION

        Age,
        AgeSquared,
        AgeCubed,
        Age20to24,
        Age21to27,						// Indicator for whether the person is in the Age category (see below for definition)
        Age23to25,						// Indicator for whether the person is in the Age category (see below for definition)
        Age25to29,
        Age25to39,
        Age26to30,						// Indicator for whether the person is in the Age category (see below for definition)
        Age28to30,						// Indicator for whether the person is in the Age category (see below for definition)
        Age30to34,
        Age35to39,
        Age35to44,
        Age40to44,
        Age45to49,
        Age45to54,
        Age50to54,
        Age55to59,
        Age55to64,
        Age60to64,
        Age65plus,
        Age65to69,
        Age67to68,
        Age69to70,
        Age70to74,
        Age71to72,
        Age73to74,
        Age75to76,
        Age75to79,
        Age77to78,
        Age79to80,
        Age80to84,
        Age81to82,
        Age83to84,
        Age85plus,
        AgeOver39,
        AgeUnder25,
        Blpay_Q2,
        Blpay_Q3,
        Blpay_Q4,
        Blpay_Q5,
        CareFromDaughter,
        CareFromDaughter_L1,
        CareFromDaughterOther_L1,
        CareFromDaughterOnly_L1,
        CareFromDaughterSon_L1,
        CareFromFormal,
        CareFromInformal,
        CareFromOther_L1,
        CareFromOtherOnly_L1,
        CareFromPartner,
        CareFromPartner_L1,
        CareFromSon,
        CareFromSon_L1,
        CareFromSonOnly_L1,
        CareFromOther,
        CareFromSonOther_L1,
        CareMarketInformal_L1,
        CareMarketFormal_L1,
        CareMarketMixed_L1,
        CareToOtherOnly,
        CareToOtherOnly_L1,
        CareToPartnerAndOther,
        CareToPartnerAndOther_L1,
        CareToPartnerOnly,
        CareToPartnerOnly_L1,
        Cohort,
        Constant, 						// For the constant (intercept) term of the regression
        Covid_D,
        Covid_2020_D,
        Covid_2021_D,
        Covid19GrossPayMonthly_L1,
        Covid19ReceivesSEISS_L1,
        CovidTransitionsMonth,
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
        Cut11,
        Cut12,
        D_children,
        D_Children,
        D_children_2under,				// Indicator (dummy variables for presence of children of certain ages in the benefitUnit)
        D_children_3_6,
        D_children_7_12,
        D_children_13_17,
        D_children_18over,				//Currently this will return 0 (false) as children leave home when they are 18
        D_Econ_benefits,
        D_Econ_benefits_NonUC,
        D_Econ_benefits_UC,
        D_Econ_benefits_UC_Lhw_ZERO,
        D_Econ_benefits_UC_Lhw_TEN,
        D_Econ_benefits_UC_Lhw_TWENTY,
        D_Econ_benefits_UC_Lhw_THIRTY,
        D_Econ_benefits_UC_Lhw_FORTY,
        D_Home_owner,
        D_Home_owner_L1,
        Dag,
        Dag_L1,
        Dag_sq,
        Dag_sq_L1,
        Dag_u22,
        Dag_sq_u22,
        Dag_cb_u22,
        Dag_22o,
        Dag_sq_22o,
        Dag_cb_22o,
        DagCeiling54,
        Dag_sqCeiling54,
        Dcpagdf_L1, 					//Lag(1) of age difference between partners
        Dcpyy_L1, 						//Lag(1) number of years in partnership
        Dcpst_Partnered,				//Partnered
        Dcpst_PreviouslyPartnered,		//Previously partnered
        Dcpst_PreviouslyPartnered_L1,   //Lag(1) of partnership status is previously partnered
        New_rel_L1,
        Dcpst_Single,					//Single never married
        Dcpst_Single_L1, 				//Lag(1) of partnership status is Single
        Dcrisis,
        Ded,
        Deh_c3_High,
        Deh_c3_Low,
        Deh_c3_Low_Dag,
        Deh_c3_Low_L1,					//Education level lag(1) equals low
        Deh_c3_Medium,
        Deh_c3_Medium_Dag,
        Deh_c3_Medium_L1, 				//Education level lag(1) equals medium
        Dehf_c3_High,					//Father's education == High indicator
        Dehf_c3_Low,					//Father's education == Low indicator
        Dehf_c3_Medium,					//Father's education == Medium indicator
        Dehm_c3_High,					//Mother's education == High indicator
        Dehm_c3_Low,					//Mother's education == Low indicator
        Dehm_c3_Medium,					//Mother's education == Medium indicator
        Dehmf_c3_High,
        Dehmf_c3_Medium,
        Dehmf_c3_Low,
        Dehsp_c3_Low_L1,				//Partner's education == Low at lag(1)
        Dehsp_c3_Medium_L1,				//Partner's education == Medium at lag(1)
        He_eq5d,                          //EQ5D quality of life score
        Dgn,							//Gender: returns 1 if male
        Dgn_baseline,
        Dgn_Dag,
        Dgn_Lhw_L1,
        Dhe,							//Health status
        Dhe_2,
        Dhe_3,
        Dhe_4,
        Dhe_5,
        Dhe_c5_1_L1,
        Dhe_c5_2_L1,
        Dhe_c5_3_L1,
        Dhe_c5_4_L1,
        Dhe_c5_5_L1,
        Dhe_L1, 						//Health status lag(1)
        Dhe_Poor,
        Dhe_Fair,
        Dhe_Good,
        Dhe_VeryGood,
        Dhe_Excellent,
        Dhe_Poor_L1,
        Dhe_Fair_L1,
        Dhe_Good_L1,
        Dhe_VeryGood_L1,
        Dhe_Excellent_L1,
        Dhesp_L1, 						//Lag(1) of partner's health status
        Dhesp_Poor_L1,
        Dhesp_Fair_L1,
        Dhesp_Good_L1,
        Dhesp_VeryGood_L1,
        Dhesp_Excellent_L1,
        Dhh_owned_L1,
        Dhhtp_c4_CoupleChildren_L1,
        Dhhtp_c4_CoupleNoChildren_L1,
        Dhhtp_c4_SingleChildren_L1,
        Dhhtp_c4_SingleNoChildren_L1,
        L_Dhhtp_c4_CoupleChildren,
        L_Dhhtp_c4_SingleChildren,
        L_Dhhtp_c4_SingleNoChildren,
        Dhhtp_c8_2_L1,
        Dhhtp_c8_3_L1,
        Dhhtp_c8_4_L1,
        Dhhtp_c8_5_L1,
        Dhhtp_c8_6_L1,
        Dhhtp_c8_7_L1,
        Dhhtp_c8_8_L1,
        Dhm,							//Mental health status
        Dhm_L1,							//Mental health status lag(1)
        Dls,                            //Life satisfaction status
        Dls_L1,                         //Life satisfaction status lag(1)
        Dhe_mcs,                        //Mental well-being status
        dhe_mcs,                        //Mental well-being status (lowercase)
        Dhe_Mcs,
        Dhe_mcs_L1,                     //Mental well-being status lag(1)
        L_Dhe_mcs,
        dhe_mcs_L1,                     //Mental well-being status lag(1) (lowercase)
        Dhe_mcs_sq,                     //MCS score squared
        Dhe_mcs_times_pcs,              //MCS times PCS
        Dhe_mcs_c_times_pcs_c,          //Centralised MCS times PCS
        Dhe_mcs_c,                      //MCS centralised by subtracting population mean
        Dhe_mcs_c_sq,                   //Square of centralised MCS
        Dhe_mcssp_L1,                      //Mental well-being status of the partner
        Dhe_pcs,                        //Physical well-being status
        dhe_pcs,                        //Physical well-being status (lowercase)
        Dhe_Pcs,
        Dhe_pcs_L1,                     //Physical well-being status lag(1)
        L_Dhe_pcs,
        dhe_pcs_L1,                     //Physical well-being status lag(1) (lowercase)
        Dhe_pcs_sq,                     //PCS score squared
        Dhe_pcs_cb,                     //PCS score cubed
        Dhe_pcs_c,                      //MCS centralised by subtracting population mean
        Dhe_pcs_c_sq,                   //Square of centralised MCS
        Dhe_pcssp_L1,                      //Physical well-being status of the partner
        Dhmghq_L1,
        Dlltsd,							//Long-term sick or disabled
        Dlltsd01,
        Dlltsd_L1,						//Long-term sick or disabled lag(1)
        Dlltsd01_L1,
        L_Dlltsd01,
        Dlltsd01_sp_L1,
        Dnc_L1, 						//Lag(1) of number of children of all ages in the benefitUnit
        Dnc02_L1, 						//Lag(1) of number of children aged 0-2 in the benefitUnit
        Dnc017, 						//Number of children aged 0-17 in the benefitUnit
        EduHighIT,
        EduMediumIT,
        EmployedToUnemployed,
        Employmentsonflexiblefurlough,
        Employmentsonfullfurlough,
        EquivalisedConsumptionYearly,
        EquivalisedIncomeYearly, 							//Equivalised income for use with the security index
        Ethn_White,
        // Ethn_Mixed,
        Ethn_Asian,
        EthnicityAsian,
        Ethn_Black,
        EthnicityBlack,
        Ethn_Other,
        EthnicityOther,
        EthnicityMixed,
        // Ethn_Missing,
        Female,
        FertilityRate,
        FinancialDistress,
        GrossEarningsYearly,
        GrossLabourIncomeMonthly,
        InverseMillsRatio,
        ITC,			//Italy
        ITF,
        ITG,
        ITH,
        ITI,
        LactiveIT,
        L1_hourly_wage,
        L1_log_hourly_wage,
        Hourly_wage_L1,
        L1_log_hourly_wage_sq,
        Ld_children_2under,
        Ld_children_3under,
        Ld_children_3underIT,
        Ld_children_4_12,
        Ld_children_4_12IT,
        Lemployed,
        Lhw_L1,
        Lhw_10,                         // Used by financial distress process
        Lhw_20,                         // Used by financial distress process
        Lhw_30,                         // Used by financial distress process
        Lhw_40,                         // Used by financial distress process
        Les_c3_Employed_L1,
        Les_c3_NotEmployed_L1,
        L_Les_c3_NotEmployed,
        Les_c3_Sick_L1,					//This is based on dlltsd
        Les_c3_Student_L1,
        L_Les_c3_Student,
        Les_c4_Student_L1,
        Les_c4_NotEmployed_L1,
        Les_c4_Retired_L1,
        Les_c7_Covid_Furlough_L1,
        Lesdf_c4_BothNotEmployed_L1,
        Lesdf_c4_EmployedSpouseNotEmployed_L1, 					//Own and partner's activity status lag(1)
        Lesdf_c4_NotEmployedSpouseEmployed_L1,
        Lessp_c3_NotEmployed_L1,
        Lessp_c3_Sick_L1,
        Lessp_c3_Student_L1,			//Partner variables
        Lesnr_c2_NotEmployed_L1,
        Reached_Retirement_Age_Lesnr_c2_NotEmployed_L1,
        Liwwh,									//Work history in months
        LnAge,
        Lnonwork,
        Lstudent,
        Lunion,
        LunionIT,
        NeedCare_L1,
        NonPovertyToPoverty,
        NotEmployed_L1,
        NumberChildren,
        NumberChildren_2under,
        OnleaveBenefits,
        OtherIncome,
        Parents,
        PartTime_AND_Ld_children_3under,			//Interaction term conditional on if the person had a child under 3 at the previous time-step
        PartTimeRate,
        PersistentPoverty,
        PersistentUnemployed,
        PovertyToNonPoverty,
        Pt,
        Reached_Retirement_Age,						//Indicator whether individual is at or above retirement age
        Reached_Retirement_Age_Les_c3_NotEmployed_L1, //Interaction term for being at or above retirement age and not employed in the previous year
        Reached_Retirement_Age_Sp,					//Indicator whether spouse is at or above retirement age
        RealGDPGrowth,
        RealIncomeChange, //Note: the above return a 0 or 1 value, but income variables will return the change in income or 0
        RealIncomeDecrease_D,
        RealWageGrowth,
        ReceiveCare_L1,
        ResStanDev,
        Retired,
        Sfr, 										//Scenario : fertility rate This retrieves the fertility rate by region and year to use in fertility regression
        sIndex,
        sIndexNormalised,
        Single,
        Single_kids,
        StatePensionAge,
        UnemployedToEmployed,
        UnemploymentRate,
        Union,
        Union_kids,
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
        UKmissing,
        UKN,
        Year,										//Year as in the simulation, e.g. 2009
        Year2010,
        Year2011,
        Year2012,
        Year2013,
        Year2014,
        Year2015,
        Year2016,
        Year2017,
        Year2018,
        Year2019,
        Year2020,
        Y2020,
        Year2021,
        Y2021,
        Year2022,
        Year2023,
        Year2024,
        Year2025,
        Year2026,
        Year2027,
        Year2028,
        Year2029,
        Year2030,
        Year2031,
        Year2032,
        Year2033,
        Year2034,
        Year2035,
        Year2036,
        Year2037,
        Year2038,
        Year2039,
        Year2040,
        Year2041,
        Year2042,
        Year2043,
        Year2044,
        Year2045,
        Year2046,
        Year2047,
        Year2048,
        Year2049,
        Year2050,
        Year2051,
        Year2052,
        Year2053,
        Year2054,
        Year2055,
        Year2056,
        Year2057,
        Year2058,
        Year2059,
        Year2060,
        Year2061,
        Year2062,
        Year2063,
        Year2064,
        Year2065,
        Year2066,
        Year2067,
        Year2068,
        Year2069,
        Year2070,
        Year2071,
        Year2072,
        Year2073,
        Year2074,
        Year2075,
        Year2076,
        Year2077,
        Year2078,
        Year2079,
        Ydses_c5_Q2_L1, 							//HH Income Lag(1) 2nd Quantile
        L_Ydses_c5_Q2,
        Ydses_c5_Q3_L1,								//HH Income Lag(1) 3rd Quantile
        L_Ydses_c5_Q3,
        Ydses_c5_Q4_L1,								//HH Income Lag(1) 4th Quantile
        L_Ydses_c5_Q4,
        Ydses_c5_Q5_L1,								//HH Income Lag(1) 5th Quantile
        L_Ydses_c5_Q5,
        Ydses_c5_L1,
        Year_transformed,							//Year - 2000
        Year_transformed_monetary,					//Year-2000 that stops in 2017, for use with monetary processes
        Ynbcpdf_dv_L1, 								//Lag(1) of difference between own and partner's gross personal non-benefit income
        Yplgrs_dv_L1,								//Lag(1) of gross personal employment income
        Yplgrs_dv_L2,								//Lag(2) of gross personal employment income
        Yplgrs_dv_L3,								//Lag(3) of gross personal employment income
        Ypnbihs_dv_L1,								//Gross personal non-benefit income lag(1)
        Ypnbihs_dv_L1_sq,							//Square of gross personal non-benefit income lag(1)
        Ypncp_L1,									//Lag(1) of capital income
        Ypncp_L2,									//Lag(2) of capital income
        Ypnoab_L1,									//Lag(1) of pension income
        Ypnoab_L2,									//Lag(2) of pension income
        Yptciihs_dv_L1,								//Lag(1) of gross personal non-employment non-benefit income
        Yptciihs_dv_L2,								//Lag(2) of gross personal non-employment non-benefit income
        Yptciihs_dv_L3,								//Lag(3) of gross personal non-employment non-benefit income
    }

    public double getDoubleValue(Enum<?> variableID) {

        switch ((DoublesVariables) variableID) {

            case Age -> {
                return (double) demAge;
            }
            case Dag -> {
                return (double) demAge;
            }
            case Dag_L1 -> {
                return (double) demAge - 1;
            }
            case Dag_sq -> {
                return (double) demAge * demAge;
            }
            case Dag_sq_L1 -> {
                return (double) (demAge - 1) * (demAge - 1);
            }
            case Dag_u22 -> {
                return (double) demAge < 22 ? demAge : 0;
            }
            case Dag_sq_u22 -> {
                return (double) demAge < 22 ? demAge * demAge : 0;
            }
            case Dag_cb_u22 -> {
                return (double) demAge < 22 ? demAge * demAge * demAge : 0;
            }
            case Dag_22o -> {
                return (double) demAge >= 22 ? demAge : 0;
            }
            case Dag_sq_22o -> {
                return (double) demAge >= 22 ? demAge * demAge : 0;
            }
            case Dag_cb_22o -> {
                return (double) demAge >= 22 ? demAge * demAge * demAge : 0;
            }
            case DagCeiling54 -> {
                return (double) Math.min(demAge, 54);
            }
            case Dag_sqCeiling54 -> {
                return (double) Math.min(demAge, 54) * Math.min(demAge, 54);
            }
            case AgeSquared -> {
                //			log.debug("age sq");
                return (double) demAge * demAge;
            }
            case AgeCubed -> {
                //			log.debug("age cub");
                return (double) demAge * demAge * demAge;
            }
            case LnAge -> {
                return Math.log(demAge);
            }
            case Age20to24 -> {
                return (demAge >= 20 && demAge <= 24) ? 1. : 0.;
            }
            case Age25to29 -> {
                return (demAge >= 25 && demAge <= 29) ? 1. : 0.;
            }
            case Age30to34 -> {
                return (demAge >= 30 && demAge <= 34) ? 1. : 0.;
            }
            case Age35to39 -> {
                return (demAge >= 35 && demAge <= 39) ? 1. : 0.;
            }
            case Age40to44 -> {
                return (demAge >= 40 && demAge <= 44) ? 1. : 0.;
            }
            case Age45to49 -> {
                return (demAge >= 45 && demAge <= 49) ? 1. : 0.;
            }
            case Age50to54 -> {
                return (demAge >= 50 && demAge <= 54) ? 1. : 0.;
            }
            case Age55to59 -> {
                return (demAge >= 55 && demAge <= 59) ? 1. : 0.;
            }
            case Age60to64 -> {
                return (demAge >= 60 && demAge <= 64) ? 1. : 0.;
            }
            case Age65to69 -> {
                return (demAge >= 65 && demAge <= 69) ? 1. : 0.;
            }
            case Age70to74 -> {
                return (demAge >= 70 && demAge <= 74) ? 1. : 0.;
            }
            case Age75to79 -> {
                return (demAge >= 75 && demAge <= 79) ? 1. : 0.;
            }
            case Age80to84 -> {
                return (demAge >= 80 && demAge <= 84) ? 1. : 0.;
            }
            case Age35to44 -> {
                return (demAge >= 35 && demAge <= 44) ? 1. : 0.;
            }
            case Age45to54 -> {
                return (demAge >= 45 && demAge <= 54) ? 1. : 0.;
            }
            case Age55to64 -> {
                return (demAge >= 55 && demAge <= 64) ? 1. : 0.;
            }
            case Age65plus -> {
                return (demAge >= 65) ? 1. : 0.;
            }
            case Age23to25 -> {
                return (demAge >= 23 && demAge <= 25) ? 1. : 0.;
            }
            case Age26to30 -> {
                return (demAge >= 26 && demAge <= 30) ? 1. : 0.;
            }
            case Age21to27 -> {
                return (demAge >= 21 && demAge <= 27) ? 1. : 0.;
            }
            case Age28to30 -> {
                return (demAge >= 28 && demAge <= 30) ? 1. : 0.;
            }
            case AgeUnder25 -> {
                return (demAge < 25) ? 1. : 0.;
            }
            case Age25to39 -> {
                return (demAge >= 25 && demAge <= 39) ? 1. : 0.;
            }
            case AgeOver39 -> {
                return (demAge > 39) ? 1. : 0.;
            }
            case Age67to68 -> {
                return (demAge >= 67 && demAge <= 68) ? 1. : 0.;
            }
            case Age69to70 -> {
                return (demAge >= 69 && demAge <= 70) ? 1. : 0.;
            }
            case Age71to72 -> {
                return (demAge >= 71 && demAge <= 72) ? 1. : 0.;
            }
            case Age73to74 -> {
                return (demAge >= 73 && demAge <= 74) ? 1. : 0.;
            }
            case Age75to76 -> {
                return (demAge >= 75 && demAge <= 76) ? 1. : 0.;
            }
            case Age77to78 -> {
                return (demAge >= 77 && demAge <= 78) ? 1. : 0.;
            }
            case Age79to80 -> {
                return (demAge >= 79 && demAge <= 80) ? 1. : 0.;
            }
            case Age81to82 -> {
                return (demAge >= 81 && demAge <= 82) ? 1. : 0.;
            }
            case Age83to84 -> {
                return (demAge >= 83 && demAge <= 84) ? 1. : 0.;
            }
            case Age85plus -> {
                return (demAge >= 85) ? 1. : 0.;
            }
            case StatePensionAge -> {
                return (demAge >= 68) ? 1. : 0.;
            }
            case NeedCare_L1 -> {
                return (Indicator.True.equals(careNeedFlagL1)) ? 1. : 0.;
            }
            case CareToPartnerOnly -> {
                return (SocialCareProvision.OnlyPartner.equals(careProvidedFlag)) ? 1. : 0.;
            }
            case CareToPartnerAndOther -> {
                return (SocialCareProvision.PartnerAndOther.equals(careProvidedFlag)) ? 1. : 0.;
            }
            case CareToOtherOnly -> {
                return (SocialCareProvision.OnlyOther.equals(careProvidedFlag)) ? 1. : 0.;
            }
            case CareToPartnerOnly_L1 -> {
                return (SocialCareProvision.OnlyPartner.equals(careProvidedFlagL1)) ? 1. : 0.;
            }
            case CareToPartnerAndOther_L1 -> {
                return (SocialCareProvision.PartnerAndOther.equals(careProvidedFlagL1)) ? 1. : 0.;
            }
            case CareToOtherOnly_L1 -> {
                return (SocialCareProvision.OnlyOther.equals(careProvidedFlagL1)) ? 1. : 0.;
            }
            case ReceiveCare_L1 -> {
                return (getTotalHoursSocialCare_L1() > 0.01) ? 1. : 0.;
            }
            case CareMarketMixed_L1 -> {
                return (getHoursFormalSocialCare_L1() > 0.01 && getHoursInformalSocialCare_L1() > 0.01) ? 1. : 0.;
            }
            case CareMarketInformal_L1 -> {
                return (getHoursFormalSocialCare_L1() < 0.01 && getHoursInformalSocialCare_L1() > 0.01) ? 1. : 0.;
            }
            case CareMarketFormal_L1 -> {
                return (getHoursFormalSocialCare_L1() > 0.01 && getHoursInformalSocialCare_L1() < 0.01) ? 1. : 0.;
            }
            case CareFromPartner_L1 -> {
                return (getCareHoursFromPartner_L1() > 0.01) ? 1. : 0.;
            }
            case CareFromDaughter_L1 -> {
                return (getCareHoursFromDaughter_L1() > 0.01) ? 1. : 0.;
            }
            case CareFromSon_L1 -> {
                return (getCareHoursFromSon_L1() > 0.01) ? 1. : 0.;
            }
            case CareFromOther_L1 -> {
                return (getCareHoursFromOther_L1() > 0.01) ? 1. : 0.;
            }
            case CareFromDaughterOnly_L1 -> {
                return (getCareHoursFromDaughter_L1() > 0.01 && Math.abs(getHoursInformalSocialCare_L1() - getCareHoursFromDaughter_L1()) < 0.01) ? 1. : 0.;
            }
            case CareFromDaughterSon_L1 -> {
                return (getCareHoursFromDaughter_L1() > 0.01 && getCareHoursFromSon_L1() > 0.01) ? 1. : 0.;
            }
            case CareFromDaughterOther_L1 -> {
                return (getCareHoursFromDaughter_L1() > 0.01 && getCareHoursFromOther_L1() > 0.01) ? 1. : 0.;
            }
            case CareFromSonOnly_L1 -> {
                return (getCareHoursFromSon_L1() > 0.01 && Math.abs(getHoursInformalSocialCare_L1() - getCareHoursFromSon_L1()) < 0.01) ? 1. : 0.;
            }
            case CareFromSonOther_L1 -> {
                return (getCareHoursFromSon_L1() > 0.01 && getCareHoursFromOther_L1() > 0.01) ? 1. : 0.;
            }
            case CareFromOtherOnly_L1 -> {
                return (getCareHoursFromOther_L1() > 0.01 && Math.abs(getHoursInformalSocialCare_L1() - getCareHoursFromOther_L1()) < 0.01) ? 1. : 0.;
            }
            case CareFromFormal -> {
                return (careFormalFlag) ? 1. : 0.;
            }
            case CareFromInformal -> {
                return (careFromPartnerFlag || careFromDaughterFlag || careFromSonFlag || careFromOtherFlag) ? 1. : 0.;
            }
            case CareFromPartner -> {
                return (careFromPartnerFlag) ? 1. : 0.;
            }
            case CareFromDaughter -> {
                return (careFromDaughterFlag) ? 1. : 0.;
            }
            case CareFromSon -> {
                return (careFromSonFlag) ? 1. : 0.;
            }
            case CareFromOther -> {
                return (careFromOtherFlag) ? 1. : 0.;
            }
            case Constant -> {
                return 1.;
            }
            case Dcpyy_L1 -> {
                return (demPartnerNYearL1 != null) ? (double) demPartnerNYearL1 : 0.0;
            }
            case Dcpagdf_L1 -> {
                return (demAgePartnerDiffL1 != null) ? (double) demAgePartnerDiffL1 : 0.0;
            }
            case Dcpst_Single -> {
                return (Dcpst.SingleNeverMarried.equals(getDcpst())) ? 1.0 : 0.0;
            }
            case Dcpst_Partnered -> {
                return (Dcpst.Partnered.equals(getDcpst())) ? 1.0 : 0.0;
            }
            case Dcpst_PreviouslyPartnered -> {
                return (Dcpst.PreviouslyPartnered.equals(getDcpst())) ? 1.0 : 0.0;
            }
            case Dcpst_Single_L1 -> {
                if (demPartnerStatusL1 != null) {
                    return demPartnerStatusL1.equals(Dcpst.SingleNeverMarried) ? 1. : 0.;
                } else return 0.;
            }
            case Dcpst_PreviouslyPartnered_L1 -> {
                if (demPartnerStatusL1 != null) {
                    return demPartnerStatusL1.equals(Dcpst.PreviouslyPartnered) ? 1. : 0.;
                } else return 0.;
            }
            case New_rel_L1 -> {
                return (demPartnerStatusL1.equals(Dcpst.Partnered) && !demPartnerStatusL2.equals(Dcpst.Partnered))? 1. : 0.;
            }
            case D_children_2under -> {
                return (double) benefitUnit.getIndicatorChildren(0, 2).ordinal();
            }
            case D_children_3_6 -> {
                return (double) benefitUnit.getIndicatorChildren(3, 6).ordinal();
            }
            case D_children_7_12 -> {
                return (double) benefitUnit.getIndicatorChildren(7, 12).ordinal();
            }
            case D_children_13_17 -> {
                return (double) benefitUnit.getIndicatorChildren(13, 17).ordinal();
            }
            case D_children_18over -> {
                return (double) benefitUnit.getIndicatorChildren(18, 99).ordinal();
            }
            case D_children, D_Children -> {
                return (getNumberChildrenAll() > 0) ? 1. : 0.;
            }
            case Dnc_L1 -> {
                return (double) getNumberChildrenAll_lag1();
            }
            case Dnc02_L1 -> {
                return (double) getNumberChildren02_lag1();
            }
            case Dnc017 -> {
                return (double) getNumberChildren017();
            }
            case Dgn -> {
                return (Gender.Male.equals(demMaleFlag)) ? 1.0 : 0.0;
            }
            case Dhe -> {
                return (double) healthSelfRated.getValue();
            }
            case Dhe_L1 -> {
                return (double) healthSelfRatedL1.getValue();
            }
            case Dhe_Excellent -> {
                return (Dhe.Excellent.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Dhe_VeryGood -> {
                return (Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Dhe_Good -> {
                return (Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Dhe_Fair -> {
                return (Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Dhe_Poor -> {
                return (Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Dhe_Excellent_L1 -> {
                return (Dhe.Excellent.equals(healthSelfRatedL1)) ? 1. : 0.;
            }
            case Dhe_VeryGood_L1 -> {
                return (Dhe.VeryGood.equals(healthSelfRatedL1)) ? 1. : 0.;
            }
            case Dhe_Good_L1 -> {
                return (Dhe.Good.equals(healthSelfRatedL1)) ? 1. : 0.;
            }
            case Dhe_Fair_L1 -> {
                return (Dhe.Fair.equals(healthSelfRatedL1)) ? 1. : 0.;
            }
            case Dhe_Poor_L1 -> {
                return (Dhe.Poor.equals(healthSelfRatedL1)) ? 1. : 0.;
            }
            case Dhesp_Excellent_L1 -> {
                return (Dhe.Excellent.equals(healthPartnerSelfRatedL1)) ? 1. : 0.;
            }
            case Dhesp_VeryGood_L1 -> {
                return (Dhe.VeryGood.equals(healthPartnerSelfRatedL1)) ? 1. : 0.;
            }
            case Dhesp_Good_L1 -> {
                return (Dhe.Good.equals(healthPartnerSelfRatedL1)) ? 1. : 0.;
            }
            case Dhesp_Fair_L1 -> {
                return (Dhe.Fair.equals(healthPartnerSelfRatedL1)) ? 1. : 0.;
            }
            case Dhesp_Poor_L1 -> {
                return (Dhe.Poor.equals(healthPartnerSelfRatedL1)) ? 1. : 0.;
            }
            case Dhe_2 -> {
                return (Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Dhe_3 -> {
                return (Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Dhe_4 -> {
                return (Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Dhe_5 -> {
                return (Dhe.Excellent.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Dhe_c5_1_L1 -> {
                return (Dhe.Poor.equals(healthSelfRatedL1)) ? 1.0 : 0.0;
            }
            case Dhe_c5_2_L1 -> {
                return (Dhe.Fair.equals(healthSelfRatedL1)) ? 1.0 : 0.0;
            }
            case Dhe_c5_3_L1 -> {
                return (Dhe.Good.equals(healthSelfRatedL1)) ? 1.0 : 0.0;
            }
            case Dhe_c5_4_L1 -> {
                return (Dhe.VeryGood.equals(healthSelfRatedL1)) ? 1.0 : 0.0;
            }
            case Dhe_c5_5_L1 -> {
                return (Dhe.Excellent.equals(healthSelfRatedL1)) ? 1.0 : 0.0;
            }
            case Dhm -> {
                return healthWbScore0to36;
            }
            case Dhm_L1 -> {
                if (healthWbScore0to36L1 != null && healthWbScore0to36L1 >= 0.) {
                    return healthWbScore0to36L1;
                } else return 0.;
            }
            case Dhe_mcs, dhe_mcs, Dhe_Mcs -> {
                return healthMentalMcs;
            }
            case Dhe_mcs_L1, dhe_mcs_L1, L_Dhe_mcs -> {
                if (healthMentalMcsL1 != null && healthMentalMcsL1 >= 0.) {
                    return healthMentalMcsL1;
                } else return 0.;
            }
            case Dhe_mcs_sq -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return healthMentalMcs * healthMentalMcs;
            }
            case Dhe_mcs_c -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return healthMentalMcs - 51.5;
            }
            case Dhe_mcs_c_sq -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return Math.pow(healthMentalMcs - 51.5, 2);
            }
            case Dhe_mcs_times_pcs -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return healthMentalMcs * healthPhysicalPcs;
            }
            case Dhe_mcs_c_times_pcs_c -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return (healthMentalMcs - 51.5) * (healthPhysicalPcs - 49.9);
            }
            case Dhe_mcssp_L1 -> {
                Person partner = getPartner();
                if (partner != null) {
                    if (partner.getHealthMentalMcsL1() != null) {return partner.getHealthMentalMcsL1();}
                    else throw new IllegalArgumentException(
                            "No Dhe_mcssp_L1 value." + partner.getKey().getId()
                    );
                } else throw new IllegalArgumentException(
                        "No partner found." + this.getKey().getId()
                );
            }
            case Dhe_pcs, dhe_pcs, Dhe_Pcs -> {
                return healthPhysicalPcs;
            }
            case Dhe_pcs_L1, dhe_pcs_L1, L_Dhe_pcs -> {
                if (healthPhysicalPcsL1 != null && healthPhysicalPcsL1 >= 0.) {
                    return healthPhysicalPcsL1;
                } else return 0.;
            }
            case Dhe_pcs_sq -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return healthPhysicalPcs * healthPhysicalPcs;
            }
            case Dhe_pcs_cb -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return healthPhysicalPcs * healthPhysicalPcs * healthPhysicalPcs;
            }
            case Dhe_pcs_c -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return healthPhysicalPcs - 49.9;
            }
            case Dhe_pcs_c_sq -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return Math.pow(healthPhysicalPcs - 49.9, 2);
            }
            case Dhe_pcssp_L1 -> {
                Person partner = getPartner();
                if (partner != null) {
                    if (partner.getHealthPhysicalPcsL1() != null){return partner.getHealthPhysicalPcsL1();}
                    else throw new IllegalArgumentException(
                            "No Dhe_pcssp_L1 value." + partner.getKey().getId()
                    );
                } else throw new IllegalArgumentException(
                        "No partner found." + this.getKey().getId()
                );
            }
            case Dls -> {
                return demLifeSatScore1to10;
            }
            case Dls_L1 -> {
                if (demLifeSatScore1to10L1 != null && demLifeSatScore1to10L1 >= 0.) {
                    return demLifeSatScore1to10L1;
                } else return 0.;
            }
            case Dhmghq_L1 -> {
                return gethealthPsyDstrss_lag1();
            }
            case Dhesp_L1 -> {
                return (healthPartnerSelfRatedL1 != null) ? (double) healthPartnerSelfRatedL1.getValue() : 0.0;
            }
            case Ded -> {
                return (Indicator.True.equals(eduSpellFlag)) ? 1.0 : 0.0;
            }
            case Deh_c3_High -> {
                return (Education.High.equals(eduHighestC3)) ? 1.0 : 0.0;
            }
            case Deh_c3_Medium -> {
                return (Education.Medium.equals(eduHighestC3)) ? 1.0 : 0.0;
            }
            case Deh_c3_Medium_L1 -> {
                return (Education.Medium.equals(eduHighestC3L1)) ? 1.0 : 0.0;
            }
            case Deh_c3_Low -> {
                return (Education.Low.equals(eduHighestC3)) ? 1.0 : 0.0;
            }
            case Deh_c3_Low_L1 -> {
                return (Education.Low.equals(eduHighestC3L1)) ? 1.0 : 0.0;
            }
            case Dehm_c3_High -> {
                return (Education.High.equals(eduHighestMotherC3)) ? 1.0 : 0.0;
            }
            case Dehm_c3_Medium -> {
                return (Education.Medium.equals(eduHighestMotherC3)) ? 1.0 : 0.0;
            }
            case Dehm_c3_Low -> {
                return (Education.Low.equals(eduHighestMotherC3)) ? 1.0 : 0.0;
            }
            case Dehf_c3_High -> {
                return (Education.High.equals(eduHighestFatherC3)) ? 1.0 : 0.0;
            }
            case Dehf_c3_Medium -> {
                return (Education.Medium.equals(eduHighestFatherC3)) ? 1.0 : 0.0;
            }
            case Dehf_c3_Low -> {
                return (Education.Low.equals(eduHighestFatherC3)) ? 1.0 : 0.0;
            }
            case Dehmf_c3_High -> {
                return (checkHighestParentalEducationEquals(Education.High)) ? 1.0 : 0.0;
            }
            case Dehmf_c3_Medium -> {
                return (checkHighestParentalEducationEquals(Education.Medium)) ? 1.0 : 0.0;
            }
            case Dehmf_c3_Low -> {
                return (checkHighestParentalEducationEquals(Education.Low)) ? 1.0 : 0.0;
            }
            case Dehsp_c3_Medium_L1 -> {
                return (Education.Medium.equals(eduHighestPartnerC3L1)) ? 1. : 0.;
            }
            case Dehsp_c3_Low_L1 -> {
                return (Education.Low.equals(eduHighestPartnerC3L1)) ? 1. : 0.;
            }
            case He_eq5d -> {
                return getDemLifeSatEQ5D();
            }
            case Dhhtp_c4_CoupleChildren_L1, L_Dhhtp_c4_CoupleChildren -> {
                return (Dhhtp_c4.CoupleChildren.equals(getDemCompHhC4L1())) ? 1.0 : 0.0;
            }
            case Dhhtp_c4_CoupleNoChildren_L1 -> {
                return (Dhhtp_c4.CoupleNoChildren.equals(getDemCompHhC4L1())) ? 1.0 : 0.0;
            }
            case Dhhtp_c4_SingleNoChildren_L1, L_Dhhtp_c4_SingleNoChildren -> {
                return (Dhhtp_c4.SingleNoChildren.equals(getDemCompHhC4L1())) ? 1.0 : 0.0;
            }
            case Dhhtp_c4_SingleChildren_L1, L_Dhhtp_c4_SingleChildren -> {
                return (Dhhtp_c4.SingleChildren.equals(getDemCompHhC4L1())) ? 1.0 : 0.0;
            }
            case Dhhtp_c8_2_L1 -> {
                // Couple with no children, spouse student
                Person partner = getPartner();
                if (partner != null && partner.labC4L1 != null)
                    return (partner.labC4L1.equals(Les_c4.Student) && Dhhtp_c4.CoupleNoChildren.equals(getDemCompHhC4L1())) ? 1. : 0.;
                else
                    return 0.;
            }
            case Dhhtp_c8_3_L1 -> {
                // Couple with no children, spouse not employed
                Person partner = getPartner();
                if (partner != null && partner.labC4L1 != null)
                    return ((partner.labC4L1.equals(Les_c4.NotEmployed) || partner.labC4L1.equals(Les_c4.Retired)) && Dhhtp_c4.CoupleNoChildren.equals(getDemCompHhC4L1())) ? 1. : 0.;
                else
                    return 0.;
            }
            case Dhhtp_c8_4_L1 -> {
                // Couple with children, spouse employed
                Person partner = getPartner();
                if (partner != null && partner.labC4L1 != null)
                    return (partner.labC4L1.equals(Les_c4.EmployedOrSelfEmployed) && Dhhtp_c4.CoupleChildren.equals(getDemCompHhC4L1())) ? 1. : 0.;
                else
                    return 0.;
            }
            case Dhhtp_c8_5_L1 -> {
                // Couple with children, spouse student
                Person partner = getPartner();
                if (partner != null && partner.labC4L1 != null)
                    return (partner.labC4L1.equals(Les_c4.Student) && Dhhtp_c4.CoupleChildren.equals(getDemCompHhC4L1())) ? 1. : 0.;
                else
                    return 0.;
            }
            case Dhhtp_c8_6_L1 -> {
                // Couple with children, spouse not employed
                Person partner = getPartner();
                if (partner != null && partner.labC4L1 != null)
                    return ((partner.labC4L1.equals(Les_c4.NotEmployed) || partner.labC4L1.equals(Les_c4.Retired)) && Dhhtp_c4.CoupleChildren.equals(getDemCompHhC4L1())) ? 1. : 0.;
                else
                    return 0.;
            }
            case Dhhtp_c8_7_L1 -> {
                // Single with no children
                return Dhhtp_c4.SingleNoChildren.equals(getDemCompHhC4L1()) ? 1. : 0.;
            }
            case Dhhtp_c8_8_L1 -> {
                // Single with children
                return Dhhtp_c4.SingleChildren.equals(getDemCompHhC4L1()) ? 1. : 0.;
            }
            case Dlltsd, Dlltsd01 -> {
                return Indicator.True.equals(healthDsblLongtermFlag) ? 1. : 0.;
            }
            case Dlltsd_L1, Dlltsd01_L1, Dlltsd01_sp_L1, L_Dlltsd01 -> {
                return Indicator.True.equals(healthDsblLongtermFlagL1) ? 1. : 0.;
            }
            case Ethn_White -> {
                return demEthnC6.equals(Ethnicity.White) ? 1. : 0.;
            }
            // case Ethn_Mixed -> {
            //    return dot01.equals(Ethnicity.Mixed) ? 1. : 0.;
            //}
            case Ethn_Asian, EthnicityAsian -> {
                return demEthnC6.equals(Ethnicity.Asian) ? 1. : 0.;
            }
            case Ethn_Black, EthnicityBlack -> {
                return demEthnC6.equals(Ethnicity.Black) ? 1. : 0.;
            }
            case Ethn_Other, EthnicityOther -> {
                return (demEthnC6.equals(Ethnicity.Other) || demEthnC6.equals(Ethnicity.Missing)) ? 1. : 0.;
            }
            case EthnicityMixed -> {
                return demEthnC6.equals(Ethnicity.Mixed) ? 1. : 0.;
            }
            // case Ethn_Missing -> {
            //    return dot01.equals(Ethnicity.Missing) ? 1. : 0.;
            // }
            case FertilityRate -> {
                if (demIoFlag)
                    return Parameters.getFertilityProjectionsByYear(getYear());
                else
                    return Parameters.getFertilityRateByRegionYear(getRegion(), getYear());
            }
            case Female -> {
                return demMaleFlag.equals(Gender.Female) ? 1. : 0.;
            }
            case FinancialDistress -> {
                return yFinDstrssFlag ? 1. : 0.;
            }
            case GrossEarningsYearly -> {
                return getGrossEarningsYearly();
            }
            case GrossLabourIncomeMonthly -> {
                return getCovidModuleGrossLabourIncome_Baseline();
            }
            case InverseMillsRatio -> {
                return getInverseMillsRatio();
            }
            case Ld_children_2under -> {
                return (getNumberChildren02_lag1() > 0) ? 1.0 : 0.0;
            }
            case Ld_children_3under -> {
                return benefitUnit.getIndicatorChildren03_lag1().ordinal();
            }
            case Ld_children_4_12 -> {
                return benefitUnit.getIndicatorChildren412_lag1().ordinal();
            }
            case Lemployed -> {
                if (labC4L1 != null)        //Problem will null pointer exceptions for those who are inactive and then become active as their lagged employment status is null!
                    return labC4L1.equals(Les_c4.EmployedOrSelfEmployed) ? 1. : 0.;
                else
                    return 0.;
            }            //A person who was not active but has become active in this year should have an employment_status_lag == null.  In this case, we assume this means 0 for the Employment regression, where Lemployed is used.
            case Lnonwork -> {
                return (labC4L1.equals(Les_c4.NotEmployed) || labC4L1.equals(Les_c4.Retired)) ? 1. : 0.;
            }
            case Lstudent -> {
                //			log.debug("Lstudent");
                return labC4L1.equals(Les_c4.Student) ? 1. : 0.;
            }
            case Lunion -> {
                //			log.debug("Lunion");
                return demStatusHhL1.equals(HouseholdStatus.Couple) ? 1. : 0.;
            }
            case Les_c3_Student_L1, L_Les_c3_Student -> {
                return (Les_c4.Student.equals(labC4L1)) ? 1.0 : 0.0;
            }
            case Les_c3_NotEmployed_L1, L_Les_c3_NotEmployed -> {
                return ((Les_c4.NotEmployed.equals(labC4L1)) || (Les_c4.Retired.equals(labC4L1))) ? 1.0 : 0.0;
            }
            case Les_c3_Employed_L1 -> {
                return (Les_c4.EmployedOrSelfEmployed.equals(labC4L1)) ? 1.0 : 0.0;
            }
            case Les_c3_Sick_L1 -> {
                if (healthDsblLongtermFlagL1 != null)
                    return healthDsblLongtermFlagL1.equals(Indicator.True) ? 1. : 0.;
                else
                    return 0.0;
            }
            case Les_c4_Student_L1 -> {
                return (Les_c4.Student.equals(labC4L1)) ? 1. : 0. ;
            }
            case Les_c4_NotEmployed_L1 -> {
                return (Les_c4.NotEmployed.equals(labC4L1)) ? 1. : 0. ;
            }
            case Les_c4_Retired_L1 -> {
                return (Les_c4.Retired.equals(labC4L1)) ? 1. : 0. ;
            }
            case Lessp_c3_Student_L1 -> {
                Person partner = getPartner();
                if (partner != null && partner.labC4L1 != null)
                    return partner.labC4L1.equals(Les_c4.Student) ? 1. : 0.;
                else
                    return 0.;
            }
            case Lessp_c3_NotEmployed_L1 -> {
                Person partner = getPartner();
                if (partner != null && partner.labC4L1 != null)
                    return (partner.labC4L1.equals(Les_c4.NotEmployed) || partner.labC4L1.equals(Les_c4.Retired)) ? 1. : 0.;
                else
                    return 0.;
            }
            case Lessp_c3_Sick_L1 -> {
                Person partner = getPartner();
                if (partner != null && partner.healthDsblLongtermFlagL1 != null)
                    return partner.healthDsblLongtermFlagL1.equals(Indicator.True) ? 1. : 0.;
                else
                    return 0.;
            }
            case Lesnr_c2_NotEmployed_L1 -> {
                return (Les_c4.NotEmployed.equals(labC4L1) || (Les_c4.Student.equals(labC4L1))) ? 1. : 0. ;
            }
            case Reached_Retirement_Age_Lesnr_c2_NotEmployed_L1 -> {
                int retirementAge;
                if (demMaleFlag.equals(Gender.Female)) {
                    retirementAge = (int) Parameters.getTimeSeriesValue(
                            getYear(),
                            Gender.Female.toString(),
                            TimeSeriesVariable.FixedRetirementAge
                    );
                } else {
                    retirementAge = (int) Parameters.getTimeSeriesValue(
                            getYear(),
                            Gender.Male.toString(),
                            TimeSeriesVariable.FixedRetirementAge
                    );
                }
                return (Les_c4.NotEmployed.equals(labC4L1) || (Les_c4.Student.equals(labC4L1)) && demAge >= retirementAge) ? 1.0 : 0.0;
            }
            case Retired -> {
                return Les_c4.Retired.equals(labC4) ? 1. : 0.;
            }
            case Lesdf_c4_EmployedSpouseNotEmployed_L1 -> {                    //Own and partner's activity status lag(1)
                return (Lesdf_c4.EmployedSpouseNotEmployed.equals(labStatusPartnerAndOwnC4L1)) ? 1. : 0.;
            }
            case Lesdf_c4_NotEmployedSpouseEmployed_L1 -> {
                return (Lesdf_c4.NotEmployedSpouseEmployed.equals(labStatusPartnerAndOwnC4L1)) ? 1. : 0.;
            }
            case Lesdf_c4_BothNotEmployed_L1 -> {
                if (labStatusPartnerAndOwnC4L1 != null)
                    return labStatusPartnerAndOwnC4L1.equals(Lesdf_c4.BothNotEmployed) ? 1. : 0.;
                else
                    return 0.;
            }
            case Liwwh -> {
                return (double) labEmpNyear;
            }
            case NotEmployed_L1 -> {
                return (labC4L1.equals(Les_c4.NotEmployed)) ? 1. : 0.;
            }
            case NumberChildren -> {
                return (double) benefitUnit.getNumberChildrenAll();
            }
            case NumberChildren_2under -> {
                return (double) benefitUnit.getNumberChildren(0, 2);
            }
            case OtherIncome -> {            // "Other income corresponds to other benefitUnit incomes divided by 10,000." (From Bargain et al. (2014).  From employment selection equation.
                return 0.;
            }                // Other incomes "correspond to partner's and other family members' income as well as capital income of various sources."
            case Parents -> {
                return HouseholdStatus.Parents.equals(getHouseholdStatus()) ? 1. : 0.;
            }
            case ResStanDev -> {        //Draw from standard normal distribution will be multiplied by the value in the .xls file, which represents the standard deviation
                //If model.addRegressionStochasticComponent set to true, return a draw from standard normal distribution, if false return 0.
                return (model.addRegressionStochasticComponent) ?
                        Parameters.getStandardNormalDistribution().inverseCumulativeProbability(statInnovations.getDoubleDraw(20)) : 0.0;
            }
            case Single -> {
                return HouseholdStatus.Single.equals(getHouseholdStatus()) ? 1. : 0.;
            }
            case Single_kids -> {        //TODO: Is this sufficient, or do we need to take children aged over 12 into account as well?
                if (HouseholdStatus.Single.equals(getHouseholdStatus())) {
                    if (benefitUnit.getChildren().isEmpty())
                        return 0.0;
                    else
                        return 1.0;
                } else return 0.;
            }
            case UnemploymentRate -> {
                return getUnemploymentRateByGenderEducationAgeYear(getDemMaleFlag(), getDeh_c3(), getDemAge(), getYear());
            }
            case Union -> {
                return HouseholdStatus.Couple.equals(getHouseholdStatus()) ? 1. : 0.;
            }
            case Union_kids -> {        //TODO: Is this sufficient, or do we need to take children aged over 12 into account as well?
                if (HouseholdStatus.Couple.equals(getHouseholdStatus())) {
                    if (benefitUnit.getChildren().isEmpty())
                        return 0.0;
                    else
                        return 1.0;
                } else return 0.0;
            }
            case Year -> {
                return (Parameters.isFixTimeTrend && getYear() >= Parameters.timeTrendStopsIn) ? (double) Parameters.timeTrendStopsIn : (double) getYear();
            }
            case Year2010 -> {
                return (getYear() <= 2010) ? 1. : 0.;
            }
            case Year2011 -> {
                return (getYear() == 2011) ? 1. : 0.;
            }
            case Year2012 -> {
                return (getYear() == 2012) ? 1. : 0.;
            }
            case Year2013 -> {
                return (getYear() == 2013) ? 1. : 0.;
            }
            case Year2014 -> {
                return (getYear() == 2014) ? 1. : 0.;
            }
            case Year2015 -> {
                return (getYear() == 2015) ? 1. : 0.;
            }
            case Year2016 -> {
                return (getYear() == 2016) ? 1. : 0.;
            }
            case Year2017 -> {
                return (getYear() == 2017) ? 1. : 0.;
            }
            case Year2018 -> {
                return (getYear() == 2018) ? 1. : 0.;
            }
            case Year2019 -> {
                return (getYear() == 2019) ? 1. : 0.;
            }
            case Year2020, Y2020 -> {
                return (getYear() == 2020) ? 1. : 0.;
            }
            case Year2021, Y2021 -> {
                return (getYear() == 2021) ? 1. : 0.;
            }
            case Year2022 -> {
                return (getYear() == 2022) ? 1. : 0.;
            }
            case Year2023 -> {
                return (getYear() == 2023) ? 1. : 0.;
            }
            case Year2024 -> {
                return (getYear() == 2024) ? 1. : 0.;
            }
            case Year2025 -> {
                return (getYear() == 2025) ? 1. : 0.;
            }
            case Year2026 -> {
                return (getYear() == 2026) ? 1. : 0.;
            }
            case Year2027 -> {
                return (getYear() == 2027) ? 1. : 0.;
            }
            case Year2028 -> {
                return (getYear() == 2028) ? 1. : 0.;
            }
            case Year2029 -> {
                return (getYear() == 2029) ? 1. : 0.;
            }
            case Year2030 -> {
                return (getYear() == 2030) ? 1. : 0.;
            }
            case Year2031 -> {
                return (getYear() == 2031) ? 1. : 0.;
            }
            case Year2032 -> {
                return (getYear() == 2032) ? 1. : 0.;
            }
            case Year2033 -> {
                return (getYear() == 2033) ? 1. : 0.;
            }
            case Year2034 -> {
                return (getYear() == 2034) ? 1. : 0.;
            }
            case Year2035 -> {
                return (getYear() == 2035) ? 1. : 0.;
            }
            case Year2036 -> {
                return (getYear() == 2036) ? 1. : 0.;
            }
            case Year2037 -> {
                return (getYear() == 2037) ? 1. : 0.;
            }
            case Year2038 -> {
                return (getYear() == 2038) ? 1. : 0.;
            }
            case Year2039 -> {
                return (getYear() == 2039) ? 1. : 0.;
            }
            case Year2040 -> {
                return (getYear() == 2040) ? 1. : 0.;
            }
            case Year2041 -> {
                return (getYear() == 2041) ? 1. : 0.;
            }
            case Year2042 -> {
                return (getYear() == 2042) ? 1. : 0.;
            }
            case Year2043 -> {
                return (getYear() == 2043) ? 1. : 0.;
            }
            case Year2044 -> {
                return (getYear() == 2044) ? 1. : 0.;
            }
            case Year2045 -> {
                return (getYear() == 2045) ? 1. : 0.;
            }
            case Year2046 -> {
                return (getYear() == 2046) ? 1. : 0.;
            }
            case Year2047 -> {
                return (getYear() == 2047) ? 1. : 0.;
            }
            case Year2048 -> {
                return (getYear() == 2048) ? 1. : 0.;
            }
            case Year2049 -> {
                return (getYear() == 2049) ? 1. : 0.;
            }
            case Year2050 -> {
                return (getYear() == 2050) ? 1. : 0.;
            }
            case Year2051 -> {
                return (getYear() == 2051) ? 1. : 0.;
            }
            case Year2052 -> {
                return (getYear() == 2052) ? 1. : 0.;
            }
            case Year2053 -> {
                return (getYear() == 2053) ? 1. : 0.;
            }
            case Year2054 -> {
                return (getYear() == 2054) ? 1. : 0.;
            }
            case Year2055 -> {
                return (getYear() == 2055) ? 1. : 0.;
            }
            case Year2056 -> {
                return (getYear() == 2056) ? 1. : 0.;
            }
            case Year2057 -> {
                return (getYear() == 2057) ? 1. : 0.;
            }
            case Year2058 -> {
                return (getYear() == 2058) ? 1. : 0.;
            }
            case Year2059 -> {
                return (getYear() == 2059) ? 1. : 0.;
            }
            case Year2060 -> {
                return (getYear() == 2060) ? 1. : 0.;
            }
            case Year2061 -> {
                return (getYear() == 2061) ? 1. : 0.;
            }
            case Year2062 -> {
                return (getYear() == 2062) ? 1. : 0.;
            }
            case Year2063 -> {
                return (getYear() == 2063) ? 1. : 0.;
            }
            case Year2064 -> {
                return (getYear() == 2064) ? 1. : 0.;
            }
            case Year2065 -> {
                return (getYear() == 2065) ? 1. : 0.;
            }
            case Year2066 -> {
                return (getYear() == 2066) ? 1. : 0.;
            }
            case Year2067 -> {
                return (getYear() == 2067) ? 1. : 0.;
            }
            case Year2068 -> {
                return (getYear() == 2068) ? 1. : 0.;
            }
            case Year2069 -> {
                return (getYear() == 2069) ? 1. : 0.;
            }
            case Year2070 -> {
                return (getYear() == 2070) ? 1. : 0.;
            }
            case Year2071 -> {
                return (getYear() == 2071) ? 1. : 0.;
            }
            case Year2072 -> {
                return (getYear() == 2072) ? 1. : 0.;
            }
            case Year2073 -> {
                return (getYear() == 2073) ? 1. : 0.;
            }
            case Year2074 -> {
                return (getYear() == 2074) ? 1. : 0.;
            }
            case Year2075 -> {
                return (getYear() == 2075) ? 1. : 0.;
            }
            case Year2076 -> {
                return (getYear() == 2076) ? 1. : 0.;
            }
            case Year2077 -> {
                return (getYear() == 2077) ? 1. : 0.;
            }
            case Year2078 -> {
                return (getYear() == 2078) ? 1. : 0.;
            }
            case Year2079 -> {
                return (getYear() >= 2079) ? 1. : 0.;
            }
            case Year_transformed -> {
                return (Parameters.isFixTimeTrend && getYear() >= Parameters.timeTrendStopsIn) ? (double) Parameters.timeTrendStopsIn - 2000 : (double) getYear() - 2000;
            }
            case Year_transformed_monetary -> {
                return (double) model.getTimeTrendStopsInMonetaryProcesses() - 2000;
            } //Note: this returns base price year - 2000 (e.g. 17 for 2017 as base price year) and monetary variables are then uprated from 2017 level to the simulated year
            case Ydses_c5_Q2_L1, L_Ydses_c5_Q2, Ydses_c5_L1 -> {
                return (Ydses_c5.Q2.equals(getYdses_c5_lag1())) ? 1.0 : 0.0;
            }
            case Ydses_c5_Q3_L1, L_Ydses_c5_Q3 -> {
                return (Ydses_c5.Q3.equals(getYdses_c5_lag1())) ? 1.0 : 0.0;
            }
            case Ydses_c5_Q4_L1, L_Ydses_c5_Q4 -> {
                return (Ydses_c5.Q4.equals(getYdses_c5_lag1())) ? 1.0 : 0.0;
            }
            case Ydses_c5_Q5_L1, L_Ydses_c5_Q5 -> {
                return (Ydses_c5.Q5.equals(getYdses_c5_lag1())) ? 1.0 : 0.0;
            }
            case Ypnbihs_dv_L1 -> {
                if (yNonBenPersGrossMonthL1 != null) {
                    return yNonBenPersGrossMonthL1;
                } else {
                    throw new RuntimeException("call to uninitialised ypnbihs_dv_lag1 in Person");
                }
            }
            case Ypnbihs_dv_L1_sq -> {
                if (yNonBenPersGrossMonthL1 != null) {
                    return yNonBenPersGrossMonthL1 * yNonBenPersGrossMonthL1;
                } else {
                    throw new RuntimeException("call to uninitialised ypnbihs_dv_lag1 in Person");
                }
            }
            case Ynbcpdf_dv_L1 -> {
                return (yPersAndPartnerGrossDiffMonthL1 != null) ? yPersAndPartnerGrossDiffMonthL1 : 0.0;
            }
            case Yptciihs_dv_L1 -> {
                return yMiscPersGrossMonthL1;
            }
            case Yptciihs_dv_L2 -> {
                return yMiscPersGrossMonthL2;
            }
            case Yptciihs_dv_L3 -> {
                return yMiscPersGrossMonthL3;
            }
            case Ypncp_L1 -> {
                return yCapitalPersMonthL1;
            }
            case Ypncp_L2 -> {
                return yCapitalPersMonthL2;
            }
            case Ypnoab_L1 -> {
                return yPensPersGrossMonthL1;
            }
            case Ypnoab_L2 -> {
                return yPensPersGrossMonthL2;
            }
            case Yplgrs_dv_L1 -> {
                return yEmpPersGrossMonthL1;
            }
            case Yplgrs_dv_L2 -> {
                return yEmpPersGrossMonthL2;
            }
            case Yplgrs_dv_L3 -> {
                return yEmpPersGrossMonthL3;
            }
            case Ld_children_3underIT -> {
                return model.getCountry().equals(Country.IT) ? benefitUnit.getIndicatorChildren03_lag1().ordinal() : 0.;
            }
            case Ld_children_4_12IT -> {
                return model.getCountry().equals(Country.IT) ? benefitUnit.getIndicatorChildren412_lag1().ordinal() : 0.;
            }
            case LunionIT -> {
                return (demStatusHhL1.equals(HouseholdStatus.Couple) && (getRegion().toString().startsWith(Country.IT.toString()))) ? 1. : 0.;
            }
            case EduMediumIT -> {
                return (eduHighestC3.equals(Education.Medium) && (getRegion().toString().startsWith(Country.IT.toString()))) ? 1. : 0.;
            }
            case EduHighIT -> {
                return (eduHighestC3.equals(Education.High) && (getRegion().toString().startsWith(Country.IT.toString()))) ? 1. : 0.;
            }

            case Reached_Retirement_Age -> {
                int retirementAge;
                if (demMaleFlag.equals(Gender.Female)) {
                    retirementAge = (int) Parameters.getTimeSeriesValue(getYear(), Gender.Female.toString(), TimeSeriesVariable.FixedRetirementAge);
                } else {
                    retirementAge = (int) Parameters.getTimeSeriesValue(getYear(), Gender.Male.toString(), TimeSeriesVariable.FixedRetirementAge);
                }
                return (demAge >= retirementAge) ? 1. : 0.;
            }
            case Reached_Retirement_Age_Sp -> {
                int retirementAgePartner;
                Person partner = getPartner();
                if (partner != null) {
                    if (partner.demMaleFlag.equals(Gender.Female)) {
                        retirementAgePartner = (int) Parameters.getTimeSeriesValue(getYear(), Gender.Female.toString(), TimeSeriesVariable.FixedRetirementAge);
                    } else {
                        retirementAgePartner = (int) Parameters.getTimeSeriesValue(getYear(), Gender.Male.toString(), TimeSeriesVariable.FixedRetirementAge);
                    }
                    return (partner.demAge >= retirementAgePartner) ? 1. : 0.;
                } else {
                    return 0.;
                }
            }
            case Reached_Retirement_Age_Les_c3_NotEmployed_L1 -> { //Reached retirement age and was not employed in the previous year
                int retirementAge;
                if (demMaleFlag.equals(Gender.Female)) {
                    retirementAge = (int) Parameters.getTimeSeriesValue(getYear(), Gender.Female.toString(), TimeSeriesVariable.FixedRetirementAge);
                } else {
                    retirementAge = (int) Parameters.getTimeSeriesValue(getYear(), Gender.Male.toString(), TimeSeriesVariable.FixedRetirementAge);
                }
                return ((demAge >= retirementAge) && (labC4L1.equals(Les_c4.NotEmployed) || labC4L1.equals(Les_c4.Retired))) ? 1. : 0.;
            }
            case EquivalisedIncomeYearly -> {
                return getBenefitUnit().getEquivalisedDisposableIncomeYearly();
            }
            case EquivalisedConsumptionYearly -> {
                if (xEquivYear != null) {
                    return xEquivYear;
                } else return -9999.99;
            }
            case sIndex -> {
                return getsIndex();
            }
            case sIndexNormalised -> {
                return getsIndexNormalised();
            }

            //New enums for the mental health Step 1 and 2:
            case EmployedToUnemployed -> {
                return (labC4L1.equals(Les_c4.EmployedOrSelfEmployed) && labC4.equals(Les_c4.NotEmployed) && healthDsblLongtermFlag.equals(Indicator.False)) ? 1. : 0.;
            }
            case UnemployedToEmployed -> {
                return (labC4L1.equals(Les_c4.NotEmployed) && healthDsblLongtermFlagL1.equals(Indicator.False) && labC4.equals(Les_c4.EmployedOrSelfEmployed)) ? 1. : 0.;
            }
            case PersistentUnemployed -> {
                return (labC4.equals(Les_c4.NotEmployed) && labC4L1.equals(Les_c4.NotEmployed) && healthDsblLongtermFlag.equals(Indicator.False) && healthDsblLongtermFlagL1.equals(Indicator.False)) ? 1. : 0.;
            }
            case NonPovertyToPoverty -> {
                if (benefitUnit.getAtRiskOfPoverty_lag1() != null) {
                    return (benefitUnit.getAtRiskOfPoverty_lag1() == 0 && benefitUnit.getAtRiskOfPoverty() == 1) ? 1. : 0.;
                } else return 0.;
            }
            case PovertyToNonPoverty -> {
                if (benefitUnit.getAtRiskOfPoverty_lag1() != null) {
                    return (benefitUnit.getAtRiskOfPoverty_lag1() == 1 && benefitUnit.getAtRiskOfPoverty() == 0) ? 1. : 0.;
                } else return 0.;
            }
            case PersistentPoverty -> {
                if (benefitUnit.getAtRiskOfPoverty_lag1() != null) {
                    return (benefitUnit.getAtRiskOfPoverty_lag1() == 1 && benefitUnit.getAtRiskOfPoverty() == 1) ? 1. : 0.;
                } else return 0.;
            }
            case RealIncomeChange -> {
                return (benefitUnit.getYearlyChangeInLogEDI());
            }
            case RealIncomeDecrease_D -> {
                return (benefitUnit.isDecreaseInYearlyEquivalisedDisposableIncome()) ? 1. : 0.;
            }
            case D_Econ_benefits -> {
                return isReceivesBenefitsFlag_L1() ? 1. : 0.;
            }
            case D_Econ_benefits_NonUC -> {
                return isReceivesBenefitsFlagNonUC() ? 1. : 0.;
            }
            case D_Econ_benefits_UC -> {
                return isReceivesBenefitsFlagUC() ? 1. : 0.;
            }
            case D_Econ_benefits_UC_Lhw_ZERO -> {
                return isReceivesBenefitsFlagUC() && getLabourSupplyWeekly() == Labour.ZERO ? 1. : 0.;
            }
            case D_Econ_benefits_UC_Lhw_TEN -> {
                return isReceivesBenefitsFlagUC() && getLabourSupplyWeekly() == Labour.TEN ? 1. : 0.;
            }
            case D_Econ_benefits_UC_Lhw_TWENTY -> {
                return isReceivesBenefitsFlagUC() && getLabourSupplyWeekly() == Labour.TWENTY ? 1. : 0.;
            }
            case D_Econ_benefits_UC_Lhw_THIRTY -> {
                return isReceivesBenefitsFlagUC() && getLabourSupplyWeekly() == Labour.THIRTY ? 1. : 0.;
            }
            case D_Econ_benefits_UC_Lhw_FORTY -> {
                return isReceivesBenefitsFlagUC() && getLabourSupplyWeekly() == Labour.FORTY ? 1. : 0.;
            }
            case D_Home_owner -> {
                return getBenefitUnit().isHousingOwned() ? 1. : 0.;
            } // Evaluated at the level of a benefit unit. If required, can be changed to individual-level homeownership status.
            case D_Home_owner_L1, Dhh_owned_L1 -> {
                return demPrptyFlagL1 ? 1. : 0.;
            } // Evaluated at the level of a benefit unit. If required, can be changed to individual-level homeownership status.
            case Covid_D -> {
                return (getYear() > 2019 & getYear() < 2023) ? 1. : 0.;
            }
            case Covid_2020_D -> {
                return (getYear() == 2020) ? 1. : 0.;
            }
            case Covid_2021_D -> {
                return (getYear() == 2021) ? 1. : 0.;
            }
            case Pt -> {
                return (getLabourSupplyHoursWeekly() > 0 && getLabourSupplyHoursWeekly() < Parameters.MIN_HOURS_FULL_TIME_EMPLOYED) ? 1. : 0.;
            }
            case L1_log_hourly_wage, Hourly_wage_L1 -> {
                if (labWageFullTimeHrlyL1 == null) {
                    throw new RuntimeException("call to evaluate lag potential hourly earnings before initialisation");
                } else {
                    return Math.log(labWageFullTimeHrlyL1);
                }
            }
            case L1_log_hourly_wage_sq -> {
                if (labWageFullTimeHrlyL1 > 0) {
                    return Math.pow(Math.log(labWageFullTimeHrlyL1), 2);
                } else {
                    throw new RuntimeException("call to evaluate lag potential hourly earnings before initialisation");
                }
            }
            case L1_hourly_wage -> {
                if (labWageFullTimeHrlyL1 > 0) {
                    return labWageFullTimeHrlyL1;
                } else {
                    throw new RuntimeException("call to evaluate lag potential hourly earnings before initialisation");
                }
            }
            case Deh_c3_Low_Dag -> {
                return (Education.Low.equals(eduHighestC3)) ? demAge : 0.0;
            }
            case Deh_c3_Medium_Dag -> {
                return (Education.Medium.equals(eduHighestC3)) ? demAge : 0.0;
            }
            //Italy
            case ITC -> {
                return (getRegion().equals(Region.ITC)) ? 1. : 0.;
            }
            case ITF -> {
                return (getRegion().equals(Region.ITF)) ? 1. : 0.;
            }
            case ITG -> {
                return (getRegion().equals(Region.ITG)) ? 1. : 0.;
            }
            case ITH -> {
                return (getRegion().equals(Region.ITH)) ? 1. : 0.;
            }
            case ITI -> {
                return (getRegion().equals(Region.ITI)) ? 1. : 0.;
            }
            //UK
            case UKC -> {
                return Region.UKC.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKD -> {
                return Region.UKD.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKE -> {
                return Region.UKE.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKF -> {
                return Region.UKF.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKG -> {
                return Region.UKG.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKH -> {
                return Region.UKH.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKI -> {
                return Region.UKI.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKJ -> {
                return Region.UKJ.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKK -> {
                return Region.UKK.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKL -> {
                return Region.UKL.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKM -> {
                return Region.UKM.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKN -> {
                return Region.UKN.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKmissing -> {
                return 0.;        //For our purpose, all our simulated people have a region, so this enum value is always going to be 0 (false).
                //			return (getRegion().equals(Region.UKmissing)) ? 1. : 0.;		//For people whose region info is missing.  The UK survey did not record the region in the first two waves (2006 and 2007, each for 4 years). For all those individuals we have gender, education etc but not region. If we exclude them we lose a large part of the UK sample, so this is the trick to keep them in the estimates.
            }
            // Regressors used in the Covid-19 labour market module below:
            case Dgn_Dag -> {
                if (demMaleFlag.equals(Gender.Male)) {
                    return (double) demAge;
                } else return 0.;
            }
            case Employmentsonfullfurlough -> {
                return Parameters.getEmploymentsFurloughedFullForMonthYear(model.getLabourMarket().getCovid19TransitionsMonth(), getYear());
            }
            case Employmentsonflexiblefurlough -> {
                return Parameters.getEmploymentsFurloughedFlexForMonthYear(model.getLabourMarket().getCovid19TransitionsMonth(), getYear());
            }
            case CovidTransitionsMonth -> {
                return model.getLabourMarket().getMonthForRegressor();
            }
            case Lhw_L1 -> {
                if (getLabHrsWorkNewL1() != null) {
                    return getLabHrsWorkNewL1();
                } else return 0.;
            }
            case Dgn_Lhw_L1 -> {
                if (getLabHrsWorkNewL1() != null && demMaleFlag.equals(Gender.Male)) {
                    return getLabHrsWorkNewL1();
                } else return 0.;
            }
            case Lhw_10 -> {
                return getLabourSupplyWeekly().equals(Labour.TEN) ? 1. : 0.;
            }
            case Lhw_20 -> {
                return getLabourSupplyWeekly().equals(Labour.TWENTY) ? 1. : 0.;
            }
            case Lhw_30 -> {
                return getLabourSupplyWeekly().equals(Labour.THIRTY) ? 1. : 0.;
            }
            case Lhw_40 -> {
                return getLabourSupplyWeekly().equals(Labour.FORTY) ? 1. : 0.;
            }
            case Covid19GrossPayMonthly_L1 -> {
                return getCovidYLabGrossL1();
            }
            case Covid19ReceivesSEISS_L1 -> {
                return (getCovidSEISSReceivedFlag().equals(Indicator.True)) ? 1. : 0.;
            }
            case Les_c7_Covid_Furlough_L1 -> {
                return (getLes_c7_covid_lag1().equals(Les_c7_covid.FurloughedFlex) || getLes_c7_covid_lag1().equals(Les_c7_covid.FurloughedFull)) ? 1. : 0.;
            }
            case Blpay_Q2 -> {
                return (getCovidYLabGrossXt5().equals(Quintiles.Q2)) ? 1. : 0.;
            }
            case Blpay_Q3 -> {
                return (getCovidYLabGrossXt5().equals(Quintiles.Q3)) ? 1. : 0.;
            }
            case Blpay_Q4 -> {
                return (getCovidYLabGrossXt5().equals(Quintiles.Q4)) ? 1. : 0.;
            }
            case Blpay_Q5 -> {
                return (getCovidYLabGrossXt5().equals(Quintiles.Q5)) ? 1. : 0.;
            }
            case Dgn_baseline -> {
                return 0.;
            }
            case RealWageGrowth -> { // Note: the values provided to the wage regression must be rebased to 2015, the default BASE_PRICE_YEAR.
                return Parameters.getTimeSeriesIndex(getYear(), UpratingCase.Earnings);
            }
            case RealGDPGrowth -> {
                return Parameters.getTimeSeriesIndex(getYear(), UpratingCase.Capital);
            }
            case Cut1 -> {
                // ordered probit/logit cut points ignored when calculating score
                return 0.;
            }
            case Cut2 -> {
                return 0.;
            }
            case Cut3 -> {
                return 0.;
            }
            case Cut4 -> {
                return 0.;
            }
            case Cut5 -> {
                return 0.;
            }
            case Cut6 -> {
                return 0.;
            }
            case Cut7 -> {
                return 0.;
            }
            case Cut8 -> {
                return 0.;
            }
            case Cut9 -> {
                return 0.;
            }
            case Cut10 -> {
                return 0.;
            }
            case Cut11 -> {
                return 0.;
            }
            case Cut12 -> {
                return 0.;
            }
            default -> {
                throw new IllegalArgumentException("Unsupported regressor " + variableID.name() + " in Person#getDoubleValue");
            }
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

    public int getDemAge() {
        return demAge;
    }

    public void setDemAge(Integer demAge) {
        this.demAge = demAge;
    }

    public Gender getDemMaleFlag() {
        return demMaleFlag;
    }

    public int getGender() {
         if (demMaleFlag == Gender.Male) return 0;
         else return 1;
    }

    public void setDemMaleFlag(Gender demMaleFlag) {
        this.demMaleFlag = demMaleFlag;
    }

    public Les_c4 getLes_c4() {
        return labC4;
    }

    public int getStudent() {
        return Les_c4.Student.equals(labC4)? 1 : 0;
    }

    public int getEducation() {

        if (eduHighestC3 ==Education.Medium) return 1;
        else if (eduHighestC3 ==Education.High) return (int)(DecisionParams.PTS_EDUCATION - 1.0);
        else return 0;
    }

    public void setLes_c4(Les_c4 labC4) {
        this.labC4 = labC4;
    }

    public void setLes_c7_covid(Les_c7_covid labC7Covid) { this.labC7Covid = labC7Covid; }

    public Les_c7_covid getLes_c7_covid() { return labC7Covid; }

    public Les_c4 getLes_c4_lag1() {
        return labC4L1;
    }

    public Les_c7_covid getLes_c7_covid_lag1() { return labC7CovidL1; }

    public void setLes_c7_covid_lag1(Les_c7_covid labC7CovidL1) {
         this.labC7CovidL1 = labC7CovidL1;
    }

    public HouseholdStatus getHouseholdStatus() {
         Household household = benefitUnit.getHousehold();
         if (household.getBenefitUnits().size()>1 && (idMother!=null || idFather!=null)) {
             for (BenefitUnit unit : household.getBenefitUnits()) {
                 if (unit != benefitUnit) {
                     for (Person member : unit.getMembers()) {
                         if (idMother!=null && member.getId()==idMother)
                             return HouseholdStatus.Parents;
                         if (idFather!=null && member.getId()==idFather)
                             return HouseholdStatus.Parents;
                     }
                 }
             }
         }
        if (benefitUnit.getCoupleBoolean())
            return HouseholdStatus.Couple;
        else
            return HouseholdStatus.Single;
    }

    public int getCohabiting() {
        return benefitUnit.getCoupleDummy();
    }

    public Education getDeh_c3() {
        return eduHighestC3;
    }

    public void setDeh_c3(Education eduHighestC3) {
         this.eduHighestC3 = eduHighestC3;
     }

    public void setDeh_c3_lag1(Education eduHighestC3L1) {
         this.eduHighestC3L1 = eduHighestC3L1;
     }

    public Education getDehm_c3() {
        return eduHighestMotherC3;
    }

    public void setDehm_c3(Education eduHighestMotherC3) {
        this.eduHighestMotherC3 = eduHighestMotherC3;
    }

    public Education getDehf_c3() {
        return eduHighestFatherC3;
    }

    public void setDehf_c3(Education eduHighestFatherC3) {
        this.eduHighestFatherC3 = eduHighestFatherC3;
    }

    public Indicator getDed() {
        return eduSpellFlag;
    }

    public void setDed(Indicator eduSpellFlag) {
        this.eduSpellFlag = eduSpellFlag;
    }

    public int getLowEducation() {
        if(eduHighestC3 != null) {
            if (eduHighestC3.equals(Education.Low)) return 1;
            else return 0;
        }
        else {
            return 0;
        }
    }

    public int getMidEducation() {
        if(eduHighestC3 != null) {
            if (eduHighestC3.equals(Education.Medium)) return 1;
            else return 0;
        }
        else {
            return 0;
        }
    }

    public int getHighEducation() {
        if(eduHighestC3 != null) {
            if (eduHighestC3.equals(Education.High)) return 1;
            else return 0;
        }
        else {
            return 0;
        }
    }

    public void setEducation(Education educationlevel) {
        this.eduHighestC3 = educationlevel;
    }

    public int getGoodHealth() {
        if(healthDsblLongtermFlag != null && !healthDsblLongtermFlag.equals(Indicator.True)) { //Good / bad health depends on dlltsd (long-term sick or disabled). If true, then person is in bad health.
            return 1;
        }
        else return 0;
    }

    public int getBadHealth() {
        if(healthDsblLongtermFlag != null && healthDsblLongtermFlag.equals(Indicator.True)) {
            return 1;
        }
        else return 0;
    }

    /*
     * In the initial population, there is continuous health score and an indicator for long-term sickness or disability. In EUROMOD to which we match, health is either
     * Good or Poor. This method checks the Dlltsd indicator and returns corresponding HealthStatus to use in matching the EUROMOD donor.
     */
    public HealthStatus getHealthStatusConversion() {
        if(healthDsblLongtermFlag != null && healthDsblLongtermFlag.equals(Indicator.True)) {
            return HealthStatus.Poor; //If long-term sick or disabled, return Poor HealthStatus
        }
        else return HealthStatus.Good; //Otherwise, return Good HealthStatus
    }

    public int getEmployed() {
        return (Les_c4.EmployedOrSelfEmployed.equals(labC4)) ? 1 : 0;
    }

    public int getNonwork() {
        return (Les_c4.NotEmployed.equals(labC4)) ? 1 : 0;
    }

    public int getEmployed_Lag1() {
        return (Les_c4.EmployedOrSelfEmployed.equals(labC4L1)) ? 1 : 0;
    }

    public int getNonwork_Lag1() {
        return (Les_c4.NotEmployed.equals(labC4L1)) ? 1 : 0;
    }

    public void setRegionLocal(Region region) {
        i_demRgn = region;
    }

    public Region getRegion() {
        if (benefitUnit == null) {
            if (i_demRgn ==null)
                throw new RuntimeException("attempt to access regionLocal before it has been assigned");
            return i_demRgn;
        } else {
            return benefitUnit.getRegion();
        }
    }

    public void setRegion(Region region) {
        this.benefitUnit.setRegion(region);
    }

    public HouseholdStatus getHousehold_status_lag() {
        return demStatusHhL1;
    }

//	public double getDeviationFromMeanRetirementAge() {
//		return deviationFromMeanRetirementAge;
//	}

    public boolean isToGiveBirth() {
        return demGiveBirthFlag;
    }

    public void setToGiveBirth(boolean toGiveBirth_) {
            demGiveBirthFlag = toGiveBirth_;
    }

    public boolean isToLeaveSchool() {
        return eduLeaveSchoolFlag;
    }

    public void setToLeaveSchool(boolean eduLeaveSchoolFlag) {
        this.eduLeaveSchoolFlag = eduLeaveSchoolFlag;
    }

    public double getWgt() {
        return wgt;
    }

    public void setWgt(double wgt) {
        this.wgt = wgt;
    }

    @Override
    public double getWeight() {
        // This connects the old interface to the new field
        if (wgt == null) {
            return 0.0;
        }
        return wgt;
    }

    public BenefitUnit getBenefitUnit() {
        if (benefitUnit == null) {
            return null;
        } else {
            return benefitUnit;
        }
    }

    public void setBenefitUnit(BenefitUnit newBenefitUnit) {

        if (benefitUnit!=null && !benefitUnit.equals(newBenefitUnit)) {
            benefitUnit.removeMember(this);
        }

        benefitUnit = newBenefitUnit;
        idBu = benefitUnit.getId();
        if (newBenefitUnit == null)
            idHh = null;
        else  {
            if (newBenefitUnit.getHousehold()==null)
                throw new RuntimeException("problem identifying household of benefit unit");
            idHh = newBenefitUnit.getHousehold().getId();
            benefitUnit.getMembers().add(this);
        }
    }

    public Person getPartner() {

        if (demAge >= Parameters.AGE_TO_BECOME_RESPONSIBLE) {

            for (Person member : benefitUnit.getMembers()) {

                boolean accept = true;
                if (member==this || member.getDemAge()<Parameters.AGE_TO_BECOME_RESPONSIBLE)
                    accept = false;
                if (idMother!=null && member.getId()==idMother)
                    accept = false;
                if (idFather!=null && member.getId()==idFather)
                    accept = false;
                if (accept)
                    return member;
            }
        }
        return null;
    }

    public boolean isPartnered() {
        return getPartner() != null;
    }

    public Long getPartnerID() {
        Person partner = this.getPartner();
        if (partner != null) {
            return partner.getId();
        } else return null;
    }

    private void nullPartnerVariables() {

        careHrsFromPartnerWeek = 0.0;
        demPartnerNYear = 0;
        if (SocialCareProvision.OnlyPartner.equals(careProvidedFlag))
            careProvidedFlag = SocialCareProvision.None;
        else if (SocialCareProvision.PartnerAndOther.equals(careProvidedFlag))
            careProvidedFlag = SocialCareProvision.OnlyOther;
    }

    public Labour getLabourSupplyWeekly() {
        if (labHrsWorkEnumWeek ==null)
            throw new RuntimeException("request for labourSupplyWeekly before it has been initialised");
        return labHrsWorkEnumWeek;
    }

    public int getL1LabourSupplyHoursWeekly() {
        if (labHrsWorkEnumWeekL1 ==null)
            throw new RuntimeException("request for labourSupplyWeekly_L1 before it has been initialised");
        return labHrsWorkEnumWeekL1.getHours(this);
    }

    public int getLabourSupplyHoursWeekly() {
        return (labHrsWorkEnumWeek != null) ? labHrsWorkEnumWeek.getHours(this) : 0;
    }

    public double getDoubleLabourSupplyHoursWeekly() {
        // this method is needed for the stupid observer
        return (double)getLabourSupplyHoursWeekly();
    }

    public void setLabourSupplyWeekly(Labour labourSupply) {
        labHrsWorkEnumWeek = labourSupply;
        labHrsWorkWeek = getLabourSupplyHoursWeekly(); // Update number of hours worked weekly
    }

    public double getLabourSupplyHoursYearly() {
        return (double) getLabourSupplyHoursWeekly() * Parameters.WEEKS_PER_YEAR;
    }

    public double getScaledLabourSupplyYearly() {
        return getLabourSupplyHoursYearly() * model.getScalingFactor();
    }


    public double getGrossEarningsWeekly() {
        return labWageFullTimeHrly * (double) getLabourSupplyHoursWeekly();
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

    public double getLabWageFullTimeHrly() {
        return labWageFullTimeHrly;
    }

    public double getDemAgeDiffDesired() {
        return demAgeDiffDesired;
    }

    public double getYWageDesired() {
        return yWageDesired;
    }

    public Dhe getDhe() {
        return healthSelfRated;
    }

    public void setDhe(Dhe health) {
        this.healthSelfRated = health;
    }

    public double getDheValue() {
        return (double) healthSelfRated.getValue();
    }
    public double getHealthWbScore0to36() {
        double val;
        if (healthWbScore0to36 == null) {
            val = -1.0;
        } else {
            val = healthWbScore0to36;
        }
        return val;
    }
    public Double getDemLifeSatScore1to10() {
        Double val;
        if (demLifeSatScore1to10 == null) {
            val = -1.;
        } else {
            val = demLifeSatScore1to10;
        }
        return val;
    }
    public double getHealthMentalMcs() {
        double val;
        if (healthMentalMcs == null) {
            val = -1.0;
        } else {
            val = healthMentalMcs;
        }
        return val;
    }
    public double getHealthPhysicalPcs() {
        double val;
        if (healthPhysicalPcs == null) {
            val = -1.0;
        } else {
            val = healthPhysicalPcs;
        }
        return val;
    }


    public void setHealthMentalMcs(Double healthMentalMcs) {
        this.healthMentalMcs = healthMentalMcs;
    }

    public void setHealthPhysicalPcs(Double healthPhysicalPcs) {
        this.healthPhysicalPcs = healthPhysicalPcs;
    }

    public void populateSocialCareReceipt(SocialCareReceiptState state) {
        if (SocialCareReceiptState.NoFormal.equals(state)) {
            careNeedFlag = Indicator.True;
            careReceivedFlag = SocialCareReceipt.Informal;
            careHrsFromOtherWeek = 10.0;
            careFromOtherFlag = true;
        } else if (SocialCareReceiptState.Mixed.equals(state)) {
            careNeedFlag = Indicator.True;
            careReceivedFlag = SocialCareReceipt.Mixed;
            careHrsFromOtherWeek = 10.0;
            careHrsFormalWeek = 10.0;
            xCareFormalWeek = 100.0;
            careFormalFlag = true;
            careFromOtherFlag = true;
        } else if (SocialCareReceiptState.Formal.equals(state)) {
            careNeedFlag = Indicator.True;
            careReceivedFlag = SocialCareReceipt.Formal;
            careHrsFormalWeek = 10.0;
            xCareFormalWeek = 100.0;
            careFormalFlag = true;
        }
    }

    public void populateSocialCareReceipt_lag1(SocialCareReceiptState state) {
        if (SocialCareReceiptState.NoFormal.equals(state)) {
            careNeedFlagL1 = Indicator.True;
            careHrsFromOtherWeekL1 = 10.0;
        } else if (SocialCareReceiptState.Mixed.equals(state)) {
            careNeedFlagL1 = Indicator.True;
            careHrsFromOtherWeekL1 = 10.0;
            careHrsFormalWeekL1 = 10.0;
        } else if (SocialCareReceiptState.Formal.equals(state)) {
            careNeedFlagL1 = Indicator.True;
            careHrsFormalWeekL1 = 10.0;
        }
    }

    public void setSocialCareFromOther(boolean val) {
        careFromOtherFlag = val;
    }

    public void setCareHoursFromOtherWeekly_lag1(double val) {
        careHrsFromOtherWeekL1 = val;
    }

    public void setCareHoursFromFormalWeekly_lag1(double val) {
        careHrsFormalWeekL1 = val;
    }
    public void setSocialCareProvision_lag1(SocialCareProvision careProvision) {
        careProvidedFlagL1 = careProvision;
    }

    public void setHealthWbScore0to36(Double healthWbScore0to36) {
        this.healthWbScore0to36 = healthWbScore0to36;
    }

    public void setDhe_lag1(Dhe health) {
        this.healthSelfRatedL1 = health;
    }

    public void setHealthWbScore0to36L1(Double dhm) {
        this.healthWbScore0to36L1 = dhm;
    }

    public Double getHealthPsyDstrss() {
        return healthPsyDstrss;
    }

    public void setHealthPsyDstrss(Double dhm_ghq) {
        this.healthPsyDstrss = dhm_ghq;
    }

    public Ethnicity getDot01() {
        return demEthnC6;
    }

    public void setDot01(Ethnicity demEthnC6) {
        this.demEthnC6 = demEthnC6;
    }

    public boolean getYFinDstrssFlag() {
        return yFinDstrssFlag;
    }

    public Indicator getNeedSocialCare() {
        return careNeedFlag;
    }

    public void setNeedSocialCare(Indicator careNeedFlag) {
        this.careNeedFlag = careNeedFlag;
    }

    public void setDer(Indicator eduReturnFlag) {
        this.eduReturnFlag = eduReturnFlag;
    }

    public Long getIdOriginalPerson() {
        return idPersOriginal;
    }

    public Long getIdOriginalBU() {
        return idBuOriginal;
    }

    public Long getIdOriginalHH() {
        return idHhOriginal;
    }

    public int getDemAgeGroup() {
        return demAgeGroup;
    }

    public boolean isClonedFlag() {
        return demClonedFlag;
    }

    public void setClonedFlag(boolean demClonedFlag) {
        this.demClonedFlag = demClonedFlag;
    }

    public Dcpst getDcpst() {
        if (benefitUnit==null) {
            if (i_demPartnerStatus ==null)
                throw new RuntimeException("attempt to access unassigned value for dcpstLocal");
            return i_demPartnerStatus;
        }
        if (getPartner()!=null)
            return Dcpst.Partnered;
        if (Dcpst.Partnered.equals(demPartnerStatusL1))
            return Dcpst.PreviouslyPartnered;
        return Dcpst.SingleNeverMarried;
    }

    public void setDcpstLocal(Dcpst demPartnerStatus) {
        this.i_demPartnerStatus = demPartnerStatus;
    }

    public Indicator getDlltsd() {
        return healthDsblLongtermFlag;
    }
    public void setDlltsd(Indicator healthDsblLongtermFlag) {
        this.healthDsblLongtermFlag = healthDsblLongtermFlag;
    }

    public void setSocialCareReceipt(SocialCareReceipt who) {
        careReceivedFlag = who;
    }

    public void setSocialCareProvision(SocialCareProvision who) {
        careProvidedFlag = who;
    }

    public Indicator getDlltsd_lag1() {
        return healthDsblLongtermFlagL1;
    }

    public void setDlltsd_lag1(Indicator healthDsblLongtermFlagL1) {
        this.healthDsblLongtermFlagL1 = healthDsblLongtermFlagL1;
    }

    public void setSedex(Indicator eduExitSampleFlag) {
        this.eduExitSampleFlag = eduExitSampleFlag;
    }

    public boolean isLeftEducation() {
        return eduLeftEduFlag;
    }

    public void setEduLeftEduFlag(boolean eduLeftEduFlag) {
        this.eduLeftEduFlag = eduLeftEduFlag;
    }

    public boolean isLeftPartnership() {
        return demLeftPartnerFlag;
    }

    public void setDemLeftPartnerFlag(boolean demLeftPartnerFlag) {
        this.demLeftPartnerFlag = demLeftPartnerFlag;
    }

    public Integer getDemPartnerNYear() {
        return demPartnerNYear;
    }

    public void setDemPartnerNYear(Integer demPartnerNYear) {
        this.demPartnerNYear = demPartnerNYear;
    }

    public Integer getDcpagdf() {
        Person partner = getPartner();
        if (partner!=null)
            return (demAge - partner.demAge);
        else
            return null;
    }

    public Double getyNonBenPersGrossMonth() {
        return yNonBenPersGrossMonth;
    }

    public void setyNonBenPersGrossMonth(Double val) {
        yNonBenPersGrossMonth = val;
    }

    public Double getyNonBenPersGrossMonthL1() {
        return yNonBenPersGrossMonthL1;
    }

    public double getyMiscPersGrossMonth() {
        return yMiscPersGrossMonth;
    }

    public double getyCapitalPersMonth() {
        return yCapitalPersMonth;
    }

    public double getyPensPersGrossMonth() {
        return yPensPersGrossMonth;
    }

    public void setYptciihs_dv(double yMiscPersGrossMonth) {
        this.yMiscPersGrossMonth = yMiscPersGrossMonth;
        if (!Parameters.checkFinite(this.yMiscPersGrossMonth))
            throw new IllegalArgumentException("yptciihs_dv is not finite");
    }

    public double getyMiscPersGrossMonthL1() {
        return yMiscPersGrossMonthL1;
    }

    public double getyEmpPersGrossMonth() {
        return (yEmpPersGrossMonth !=null) ? yEmpPersGrossMonth : 0.0;
    }

    public void setyEmpPersGrossMonth(double val) {
        yEmpPersGrossMonth = val;
    }

    public double getyEmpPersGrossMonthL1() {
        return yEmpPersGrossMonthL1;
    }

    public double getyEmpPersGrossMonthL2() {
        return yEmpPersGrossMonthL2;
    }

    public double getyEmpPersGrossMonthL3() {
        return yEmpPersGrossMonthL3;
    }

    public Double getYnbcpdf_dv_lag1() {
        return yPersAndPartnerGrossDiffMonthL1;
    }

    public Lesdf_c4 getLesdf_c4() {
        if (benefitUnit.getCoupleBoolean() && demAge >=Parameters.AGE_TO_BECOME_RESPONSIBLE) {
            if (getPartner()==null)
                throw new RuntimeException("inconsistency between couple and partner identifiers");
            if (Les_c4.EmployedOrSelfEmployed.equals(labC4) && Les_c4.EmployedOrSelfEmployed.equals(getPartner().labC4))
                return Lesdf_c4.BothEmployed;
            else if (Les_c4.EmployedOrSelfEmployed.equals(labC4))
                return Lesdf_c4.EmployedSpouseNotEmployed;
            else if (Les_c4.EmployedOrSelfEmployed.equals(getPartner().labC4))
                return Lesdf_c4.NotEmployedSpouseEmployed;
            else
                return Lesdf_c4.BothNotEmployed;
        }
        return null;
    }

    public Lesdf_c4 getLesdf_c4_lag1() {
        return labStatusPartnerAndOwnC4L1;
    }

    public void setLes_c4_lag1(Les_c4 labC4L1) {
        this.labC4L1 = labC4L1;
    }

    public void setLesdf_c4_lag1(Lesdf_c4 labStatusPartnerAndOwnC4L1) {
        this.labStatusPartnerAndOwnC4L1 = labStatusPartnerAndOwnC4L1;
    }

    public void setYpnbihs_dv_lag1(Double val) {
        yNonBenPersGrossMonthL1 = val;
    }

    public void setDehsp_c3_lag1(Education eduHighestPartnerC3L1) {
        this.eduHighestPartnerC3L1 = eduHighestPartnerC3L1;
    }

    public void setDhesp_lag1(Dhe healthPartnerSelfRatedL1) {
        this.healthPartnerSelfRatedL1 = healthPartnerSelfRatedL1;
    }

    public void setYnbcpdf_dv_lag1(Double val) {
        yPersAndPartnerGrossDiffMonthL1 = val;
    }

    public void setDemPartnerNYearL1(Integer demPartnerNYearL1) {
        this.demPartnerNYearL1 = demPartnerNYearL1;
    }

    public void setDcpagdf_lag1(Integer demAgePartnerDiffL1) {
        this.demAgePartnerDiffL1 = demAgePartnerDiffL1;
    }

    public void setDcpst_lag1(Dcpst demPartnerStatusL1) {
        this.demPartnerStatusL1 = demPartnerStatusL1;
    }

    public void setLabWageFullTimeHrly(double potentialHourlyEarnings) {
        this.labWageFullTimeHrly = potentialHourlyEarnings;
    }

    public double getLabWageFullTimeHrlyL1() {
        return labWageFullTimeHrlyL1;
    }

    public void setLabWageFullTimeHrlyL1(double potentialHourlyEarnings) {
        labWageFullTimeHrlyL1 = potentialHourlyEarnings;
    }


    public void setLiwwh(Integer labEmpNyear) {
        this.labEmpNyear = labEmpNyear;
    }

    public void setIoFlag(boolean demIoFlag) {
        this.demIoFlag = demIoFlag;
    }

    public boolean isToBePartnered() {
        return demBePartnerFlag != null && demBePartnerFlag;
    }

    public void setToBePartnered(boolean demBePartnerFlag) {
        this.demBePartnerFlag = demBePartnerFlag;
    }

    public int getAdultChildFlag() {
        if (demAdultChildFlag != null) {
            if (demAdultChildFlag.equals(Indicator.True)) {
                return 1;
            }
            else return 0;
        }
        else return 0;
    }

    public Long getIdHousehold() {
        return idHh;
    }

    public void setIdHousehold(Long idHh) {
        // should only be called from BenefitUnit.setHousehold
        this.idHh = idHh;
    }

    public Series.Double getYDispEquivYear() {
        return yDispEquivYear;
    }

    public void setYDispEquivYear(Series.Double yDispEquivYear) {
        this.yDispEquivYear = yDispEquivYear;
    }

    public Double getXEquivYear() {
        return xEquivYear;
    }

    public void setXEquivYear(Double xEquivYear) {
        this.xEquivYear = xEquivYear;
    }

    public Series.Double getXEquivYearL1() {
        return xEquivYearL1;
    }

    public void setXEquivYearL1(Series.Double xEquivYearL1) {
        this.xEquivYearL1 = xEquivYearL1;
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

    public Integer getLabHrsWorkNewL1() {
        return labHrsWorkNewL1;
    }

    public void setNewWorkHours_lag1(Integer labHrsWorkNewL1) {
        this.labHrsWorkNewL1 = labHrsWorkNewL1;
    }

    public double getCovidYLabGrossL1() {
        return covidYLabGrossL1;
    }

    public void setCovidYLabGrossL1(double covidYLabGrossL1) {
        this.covidYLabGrossL1 = covidYLabGrossL1;
    }

    public Indicator getCovidSEISSReceivedFlag() {
        return covidSEISSReceivedFlag;
    }

    public void setCovidSEISSReceivedFlag(Indicator covidSEISSReceivedFlag) {
        this.covidSEISSReceivedFlag = covidSEISSReceivedFlag;
    }

    public double getCovidModuleGrossLabourIncome_Baseline() {
        return (covidYLabGross !=null) ? covidYLabGross : 0.0;
    }

    public void setCovidModuleGrossLabourIncome_Baseline(double val) {
        covidYLabGross = val;
    }

    public Quintiles getCovidYLabGrossXt5() {
        return covidYLabGrossXt5;
    }

    public void setCovidYLabGrossXt5(Quintiles covidYLabGrossXt5) {
        this.covidYLabGrossXt5 = covidYLabGrossXt5;
    }

    public boolean isReceivesBenefitsFlag() {
        return yBenReceivedFlag;
    }

    public void setReceivesBenefitsFlag(boolean yBenReceivedFlag) {
        this.yBenReceivedFlag = yBenReceivedFlag;
    }

    public boolean isReceivesBenefitsFlag_L1() {
        return (yBenReceivedFlagL1 !=null) ? yBenReceivedFlagL1 : false;
    }

    public void setReceivesBenefitsFlag_L1(boolean yBenReceivedFlagL1) {
        this.yBenReceivedFlagL1 = yBenReceivedFlagL1;
    }

    public boolean isReceivesBenefitsFlagUC() {
        return yBenUCReceivedFlag;
    }

    public void setReceivesBenefitsFlagUC(boolean yBenUCReceivedFlag) {
        this.yBenUCReceivedFlag = yBenUCReceivedFlag;
    }

    public boolean isReceivesBenefitsFlagUC_L1() {
        return (null != yBenUCReceivedFlagL1) ? yBenUCReceivedFlagL1 : false;
    }

    public void setReceivesBenefitsFlagUC_L1(boolean yBenUCReceivedFlagL1) {
        this.yBenUCReceivedFlagL1 = yBenUCReceivedFlagL1;
    }

    public boolean isReceivesBenefitsFlagNonUC() {
        return yBenNonUCReceivedFlag;
    }

    public void setReceivesBenefitsFlagNonUC(boolean yBenNonUCReceivedFlag) {
        this.yBenNonUCReceivedFlag = yBenNonUCReceivedFlag;
    }

    public boolean isReceivesBenefitsFlagNonUC_L1() {
        return (null != yBenNonUCReceivedFlagL1) ? yBenNonUCReceivedFlagL1 : false;
    }

    public void setReceivesBenefitsFlagNonUC_L1(boolean yBenNonUCReceivedFlagL1) {
        this.yBenNonUCReceivedFlagL1 = yBenNonUCReceivedFlagL1;
    }


    public double getEquivalisedDisposableIncomeYearly() {
        return benefitUnit.getEquivalisedDisposableIncomeYearly();
    }

    public double getDisposableIncomeMonthly() { return benefitUnit.getDisposableIncomeMonthly();}

    public double getWageOffer() {
        if (labWageOfferLowFlag)
            return 0.0;
        else
            return 1.0;
    }

    public int getDisability() {
        return (Indicator.True.equals(getDlltsd())) ? 1 : 0;
    }

    public SocialCareReceipt getSocialCareReceipt() {
        // market = 0 for no social care
        //          1 for only informal care
        //          2 for informal and formal care
        //          3 for only formal care
        if (getHoursFormalSocialCare()<0.01 && getHoursInformalSocialCare()<0.01)
            return SocialCareReceipt.None;
        else if (getHoursFormalSocialCare()<0.01 && getHoursInformalSocialCare()>0.01)
            return SocialCareReceipt.Informal;
        else if (getHoursFormalSocialCare()>0.01 && getHoursInformalSocialCare()>0.01)
            return SocialCareReceipt.Mixed;
        else return SocialCareReceipt.Formal;
    }

    public SocialCareReceiptState getSocialCareReceiptState() {
        // market = 0 for no social care
        //          1 for only informal care
        //          2 for informal and formal care
        //          3 for only formal care
        if (Indicator.False.equals(careNeedFlag))
            return SocialCareReceiptState.NoneNeeded;
        else if (getHoursFormalSocialCare()<0.01)
            return SocialCareReceiptState.NoFormal;
        else if (getHoursInformalSocialCare()<0.01)
            return SocialCareReceiptState.Formal;
        else return SocialCareReceiptState.Mixed;
    }

    public double getSocialCareProvisionState() {
//        double val = getSocialCareProvision().getValue();
//        if (dag>DecisionParams.MAX_AGE_COHABITATION && val > 0.1 && val < 2.1)
//            val = 3.0;
        return (double)getSocialCareProvision().getValue();
    }

    public SocialCareProvision getSocialCareProvision() {
        if (careProvidedFlag ==null)
            return SocialCareProvision.None;
        else
            return careProvidedFlag;
    }

    public double getRetired() {
        return (Les_c4.Retired.equals(getLes_c4())) ? 1.0 : 0.0;
    }

    public void setYearLocal(Integer i_demYear) {
        this.i_demYear = i_demYear;
    }

    public int getYear() {
        if (model != null) {
            return model.getYear();
        } else {
            if (i_demYear == null) {
                throw new RuntimeException("call to get uninitialised year in benefit unit");
            }
            return i_demYear;
        }
    }
    private int getStartYear() {
        if (model != null) {
            return model.getStartYear();
        } else {
            return 0;
        }
    }

    public void setNumberChildren017Local(Integer nbr) {
        i_demNchild0to17 = nbr;
    }
    public void setIndicatorChildren02Local(Indicator idctr) {
        i_demNChild0to2 = idctr;
    }

    private Ydses_c5 getYdses_c5_lag1() {
        if (model!=null) {
            if (benefitUnit==null)
                throw new RuntimeException("attempt to access unassigned benefit unit");
            return benefitUnit.getYdses_c5_lag1();
        } else {
            if (i_yHhQuintilesC5 ==null)
                throw new RuntimeException("attempt to access unassigned ydses_c5_lag1Local");
            return i_yHhQuintilesC5;
        }
    }

    public void setI_yHhQuintilesC5(Ydses_c5 yHhQuintilesC5L1) {
        i_yHhQuintilesC5 = yHhQuintilesC5L1;
    }

    private Dhhtp_c4 getDemCompHhC4L1() {
        if (model!=null) {
            if (benefitUnit==null)
                throw new RuntimeException("attempt to access unassigned benefit unit");
            return benefitUnit.getDemCompHhC4L1();
        } else {
            if (i_demCompHhC4L1 ==null)
                throw new RuntimeException("attempt to access unassigned demCompHhC4L1Local");
            return i_demCompHhC4L1;
        }
    }

    public void setI_demCompHhC4L1(Dhhtp_c4 demCompHhC4L1) {
        i_demCompHhC4L1 = demCompHhC4L1;
    }

    private Integer getNumberChildrenAll_lag1() {
        if (benefitUnit != null) {
            return (benefitUnit.getNumberChildrenAll_lag1() != null) ? benefitUnit.getNumberChildrenAll_lag1() : 0;
        } else {
            return (i_demNchildL1 ==null) ? 0 : i_demNchildL1;
        }
    }

    public void setNumberChildrenAllLocal(Integer nbr) {
        i_demNchild = nbr;
    }

    private Integer getNumberChildrenAll() {
        if (model != null) {
            if (benefitUnit==null)
                throw new RuntimeException("attempt to access unassigned benefit unit");
            return benefitUnit.getNumberChildrenAll();
        } else {
            if (i_demNchild ==null)
                throw new RuntimeException("attempt to access unassigned numberChildrenAllLocal");
            return i_demNchild;
        }
    }

    public void setNumberChildrenAllLocal_lag1(Integer nbr) {
        i_demNchildL1 = nbr;
    }

    public void setNumberChildren02Local_lag1(Integer nbr) {
        i_demNchild0to2L1 = nbr;
    }

    private Integer getNumberChildren02_lag1() {
        if (model != null) {
            if (benefitUnit==null)
                throw new RuntimeException("attempt to access unassigned benefit unit");
            return benefitUnit.getNumberChildren02_lag1();
        } else {
            if (i_demNchild0to2L1 ==null)
                throw new RuntimeException("attempt to access unassigned numberChildren02Local_lag1");
            return i_demNchild0to2L1;
        }
    }

    private Integer getNumberChildren017() {
        if (model != null) {
            if (benefitUnit==null)
                throw new RuntimeException("attempt to access unassigned benefit unit");
            return benefitUnit.getNumberChildren(0,17);
        } else {
            if (i_demNchild0to17 ==null)
                throw new RuntimeException("attempt to access unassigned numberChildren017Local");
            return i_demNchild0to17;
        }
    }

    private Double getInverseMillsRatio() {

        double score;
        if(Gender.Male.equals(demMaleFlag)) {
            if (Les_c4.EmployedOrSelfEmployed.equals(labC4L1)) {
                score = Parameters.getRegEmploymentSelectionMaleE().getScore(this, Person.DoublesVariables.class);
            } else {
                score = Parameters.getRegEmploymentSelectionMaleNE().getScore(this, Person.DoublesVariables.class);
            }
        } else {
            // for females
            if (Les_c4.EmployedOrSelfEmployed.equals(labC4L1)) {
                score = Parameters.getRegEmploymentSelectionFemaleE().getScore(this, Person.DoublesVariables.class);
            } else {
                score = Parameters.getRegEmploymentSelectionFemaleNE().getScore(this, Person.DoublesVariables.class);
            }
        }
        Double inverseMillsRatio; //IMR is the PDF(x) / CDF(x) where x is score of probit of employment
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
        if (Double.isFinite(inverseMillsRatio)) {
            if(Gender.Male.equals(demMaleFlag)) {
                if (inverseMillsRatio > statInverseMillsRatioMaxM) {
                    statInverseMillsRatioMaxM = inverseMillsRatio;
                }
                if (inverseMillsRatio < statInverseMillsRatioMinM) {
                    statInverseMillsRatioMinM = inverseMillsRatio;
                }
            } else {
                if (inverseMillsRatio > statInverseMillsRatioMaxF) {
                    statInverseMillsRatioMaxF = inverseMillsRatio;
                }
                if (inverseMillsRatio < statInverseMillsRatioMinF) {
                    statInverseMillsRatioMinF = inverseMillsRatio;
                }
            }
        } else {
            log.debug("inverse Mills ratio is not finite, return 0 instead!!!   IMR: " + inverseMillsRatio + ", score: " + score/* + ", num: " + num + ", denom: " + denom*/ + ", age: " + demAge + ", gender: " + demMaleFlag + ", education " + eduHighestC3 + ", activity_status from previous time-step " + labC4);
            return 0.;
        }
        return inverseMillsRatio;		//XXX: Currently only returning non-zero IMR if it is finite
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
            return labWageFullTimeHrly;
        } else {
            double ptPremium;
            if (labC4L1.equals(Les_c4.EmployedOrSelfEmployed)) {
                if (Gender.Male.equals(demMaleFlag)) {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionName.WagesMalesE, "Pt");
                } else {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionName.WagesFemalesE, "Pt");
                }
            } else {
                if (Gender.Male.equals(demMaleFlag)) {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionName.WagesMalesNE, "Pt");
                } else {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionName.WagesFemalesNE, "Pt");
                }
            }
            return Math.exp( Math.log(labWageFullTimeHrly) + ptPremium);
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
    public Integer getNumberChildren017Local() {
        return i_demNchild0to17;
    }
    public Integer getNumberChildrenAllLocal() {
        return i_demNchild;
    }
    public Indicator getIndicatorChildren02Local() {
        return i_demNChild0to2;
    }
    public Map<Labour, Integer> getPersonContinuousHoursLabourSupplyMap() {
        return personContinuousHoursLabourSupplyMap;
    }
    public void setPersonContinuousHoursLabourSupplyMap(Map<Labour, Integer> personContinuousHoursLabourSupplyMap) {
        this.personContinuousHoursLabourSupplyMap = personContinuousHoursLabourSupplyMap;
    }
    public double getLabourSupplySingleDraw() {
        return statInnovations.getSingleDrawDoubleInnov(0);
    }
    public double getBenefitUnitRandomUniform() {return statInnovations.getDoubleDraw(31);}

    public double getHoursFormalSocialCare_L1() {
        return (careHrsFormalWeekL1 > 0.0) ? careHrsFormalWeekL1 : 0.0;
    }

    public double getHoursFormalSocialCare() {
        double hours = 0.0;
        if (careHrsFormalWeek !=null)
            if (careHrsFormalWeek >0.0)
                hours = careHrsFormalWeek;
        return hours;
    }

    public double getHoursInformalSocialCare() {
        return getCareHoursFromPartnerWeekly() + getCareHoursFromDaughterWeekly() + getCareHoursFromSonWeekly() + getCareHoursFromOtherWeekly() + getCareHoursFromParentWeekly();
    }

    public double getCareHoursFromPartnerWeekly() {
        double hours = 0.0;
        if (careHrsFromPartnerWeek != null)
            if (careHrsFromPartnerWeek >0.0)
                hours = careHrsFromPartnerWeek;
        return hours;
    }
    public void setCareHoursFromPartnerWeekly(double hours) {
        careHrsFromPartnerWeek = hours;
    }

    public double getCareHoursFromParentWeekly() {
        double hours = 0.0;
        if (Parameters.checkFinite(careHrsFromParentWeek) && careHrsFromParentWeek > 0.0)
            hours = careHrsFromParentWeek;
        return hours;
    }

    public double getCareHoursFromDaughterWeekly() {
        double hours = 0.0;
        if (Parameters.checkFinite(careHrsFromDaughterWeek) && careHrsFromDaughterWeek > 0.0)
            hours = careHrsFromDaughterWeek;
        return hours;
    }

    public double getCareHoursFromSonWeekly() {
        double hours = 0.0;
        if (Parameters.checkFinite(careHrsFromSonWeek) && careHrsFromSonWeek > 0.0)
            hours = careHrsFromSonWeek;
        return hours;
    }

    public double getCareHoursFromOtherWeekly() {
        double hours = 0.0;
        if (Parameters.checkFinite(careHrsFromOtherWeek) && careHrsFromOtherWeek > 0.0)
            hours = careHrsFromOtherWeek;
        return hours;
    }

    public double getCareHoursProvidedWeekly() {
        double hours = 0.0;
        if (Parameters.checkFinite(careHrsProvidedWeek) && careHrsProvidedWeek > 0.0)
            hours = careHrsProvidedWeek;
        return hours;
    }

    public double getHoursInformalSocialCare_L1() {
        return getCareHoursFromPartner_L1() + getCareHoursFromDaughter_L1() + getCareHoursFromSon_L1() + getCareHoursFromOther_L1() + getCareHoursFromParent_L1();
    }

    public double getTotalHoursSocialCare_L1() {
        return getHoursFormalSocialCare_L1() + getHoursInformalSocialCare_L1();
    }

    public double getCareHoursFromParent_L1() {
        return (careHrsFromParentWeekL1 >0.0) ? careHrsFromParentWeekL1 : 0.0;
    }

    public double getCareHoursFromPartner_L1() {
        return (careHrsFromPartnerWeekL1 > 0.0) ? careHrsFromPartnerWeekL1 : 0.0;
    }

    public double getCareHoursFromDaughter_L1() {
        return (careHrsFromDaughterWeekL1 >0.0) ? careHrsFromDaughterWeekL1 : 0.0;
    }

    public double getCareHoursFromSon_L1() {
        return (careHrsFromSonWeekL1 >0.0) ? careHrsFromSonWeekL1 : 0.0;
    }

    public double getCareHoursFromOther_L1() {
        return (careHrsFromOtherWeekL1 >0.0) ? careHrsFromOtherWeekL1 : 0.0;
    }

    public double getSocialCareCostWeekly() {
        double cost = 0.0;
        if (xCareFormalWeek !=null)
            if (xCareFormalWeek >0.0)
                cost = xCareFormalWeek;
        return cost;
    }

    public boolean getTestPartner() {
        return (demAlignPartnerProcess !=null) && demAlignPartnerProcess;
    }

    public void setHasTestPartner(boolean demAlignPartnerProcess) {
        this.demAlignPartnerProcess = demAlignPartnerProcess;
    }

    public boolean getLeavePartner() {
        return (demLeavePartnerFlag !=null) && demLeavePartnerFlag;
    }

    public void setLeavePartner(boolean demLeavePartnerFlag) {
        this.demLeavePartnerFlag = demLeavePartnerFlag;
    }

    public boolean getLowWageOffer() {
        return (labWageOfferLowFlag !=null) && labWageOfferLowFlag;
    }

    private boolean checkHighestParentalEducationEquals(Education ee) {
        if (eduHighestFatherC3 !=null && eduHighestMotherC3 !=null) {
            if (eduHighestFatherC3.getValue() > eduHighestMotherC3.getValue())
                return ee.equals(eduHighestFatherC3);
            else
                return ee.equals(eduHighestMotherC3);
        } else if (eduHighestFatherC3 !=null) {
            return ee.equals(eduHighestFatherC3);
        } else {
            return ee.equals(eduHighestMotherC3);
        }
    }

    public boolean getBornInSimulation() {
        return demBornInSimFlag;
    }

    public void setBornInSimulation(boolean demBornInSimFlag) {
        this.demBornInSimFlag = demBornInSimFlag;
    }

    public double getHoursWorkedWeekly() {
        return ( (labHrsWorkWeek != null) && labHrsWorkWeek > 0 ) ? (double) labHrsWorkWeek : 0.0;
    }

    public double getLeisureHoursPerWeek() {
        return Parameters.HOURS_IN_WEEK - getCareHoursProvidedWeekly() - getHoursWorkedWeekly();
    }

    public void setSampleExit(SampleExit demExitSample) {
        if (!SampleExit.NotYet.equals(this.demExitSample))
            throw new RuntimeException("Attempt to exit person from the simulated sample twice");
        this.demExitSample = demExitSample;
    }
    public SampleExit getSampleExit() {return demExitSample;}
    public double getFertilityRandomUniform2() { return statInnovations.getDoubleDraw(28); }
    public double getCohabitRandomUniform2() { return statInnovations.getDoubleDraw(26); }
    public RegressionName getRegressionName(Axis axis) {
        switch (axis) {
            case Student -> {return RegressionName.EducationE1a;}
            case Education -> {return RegressionName.EducationE2a;}
            case Health -> {return RegressionName.HealthH1b;}
            case Disability -> {return RegressionName.HealthH2b;}
            case Cohabitation -> {
                if (Dcpst.Partnered.equals(demPartnerStatusL1))
                    return RegressionName.PartnershipU2b;
                else if (getStudent()==0)
                    return RegressionName.PartnershipU1b;
                else
                    return RegressionName.PartnershipU1a;
            }
            case SocialCareProvision -> {
                if (Dcpst.Partnered.equals(getDcpst()))
                    return RegressionName.SocialCareS3d;
                else
                    return RegressionName.SocialCareS3c;
            }
            case WagePotential -> {
                if (Gender.Male.equals(demMaleFlag))
                    return RegressionName.WagesMalesE;
                else
                    return RegressionName.WagesFemalesE;
            }
            case WageOffer1 -> {
                if (Gender.Male.equals(demMaleFlag)) {
                    if (Education.High.equals(eduHighestC3L1)) {
                        return RegressionName.UnemploymentU1a;
                    } else {
                        return RegressionName.UnemploymentU1b;
                    }
                } else {
                    if (Education.High.equals(eduHighestC3L1)) {
                        return RegressionName.UnemploymentU1c;
                    } else {
                        return RegressionName.UnemploymentU1d;
                    }
                }
            }
            default -> {
                throw new RuntimeException("failed to recognise axis for regression identification");
            }
        }
    }

    public void setProcessedId(long id) {
        key.setWorkingId(id);
    }

    public long getSeed() {return (statSeed !=null) ? statSeed : 0L;}

    private Double gethealthPsyDstrss_lag1() {
        if (healthPsyDstrssL1 == null)
            throw new RuntimeException("attempt to access dhmGhq_lag1 before it has been initialised");
        return healthPsyDstrssL1;
    }

    public Double getYnbcpdf_dv() {
        Person partner = getPartner();
        if (partner != null) {
            if (partner.getyNonBenPersGrossMonth() != null && getyNonBenPersGrossMonth() != null)
                return getyNonBenPersGrossMonth() - partner.getyNonBenPersGrossMonth();
        }
        return null;
    }

    public long getId() {
        return key.getId();
    }

    public Long getIdMother() {
        return idMother;
    }

    public Long getIdFather() {
        return idFather;
    }

    public boolean getToBePartnered() {return demBePartnerFlag;}

    public static void setPersonIdCounter(long id) {personIdCounter=id;}

    public Double getDemLifeSatEQ5D() {
        return demLifeSatEQ5D;
    }

    public void setDemLifeSatEQ5D(Double demLifeSatEQ5D) {
        this.demLifeSatEQ5D = demLifeSatEQ5D;
    }

    public Double getHealthPhysicalPartnerPcs() {
        return healthPhysicalPartnerPcs;
    }

    public void setHealthPhysicalPartnerPcs(Double healthPhysicalPartnerPcs) {
        this.healthPhysicalPartnerPcs = healthPhysicalPartnerPcs;
    }

    public Double getHealthMentalPartnerMcs() {
        return healthMentalPartnerMcs;
    }

    public void setHealthMentalPartnerMcs(Double healthMentalPartnerMcs) {
        this.healthMentalPartnerMcs = healthMentalPartnerMcs;
    }

    public Double getHealthMentalMcsL1() {
        return healthMentalMcsL1;
    }

    public Double getHealthPhysicalPcsL1() {
        return healthPhysicalPcsL1;
    }

    public void setLtIncomeDonor(Individual individual) {
        ltIncomeDonor = individual;
    }

    public void setLtIncome(int maxAge) {
        lifetimeIncome = 0.0;
        if (demAge > 0) {

            int birthYear = ltIncomeDonor.getBirthYear();
            int ageLimit = Math.min(maxAge, demAge -1);
            for (int aa=0; aa<=ageLimit; aa++) {
                AnnualIncome annualIncome = ltIncomeDonor.getAnnualIncome(birthYear+aa);
                if (annualIncome == null)
                    throw new RuntimeException("Annual income for year " + (birthYear+aa) + " not found for donor " + ltIncomeDonor.getId());
                lifetimeIncome += annualIncome.getValue();
            }
            lifetimeIncome /= (double)(ageLimit+1);
        }
    }

    public double getLifetimeIncome() {
        if (Parameters.checkFinite(lifetimeIncome))
            return lifetimeIncome;
        else
            throw new RuntimeException("lifetimeIncome is not finite");
    }

    public void updateLtIncome() {
        double newVal = getBenefitUnit().getHousehold().getEquivalisedDisposableIncomeYearly();
        if (demAge == 0) {
            lifetimeIncome = newVal;
        } else {

            if (!Parameters.checkFinite(lifetimeIncome))
                throw new RuntimeException("lifetimeIncome is not defined");
            double curVal = lifetimeIncome;
            double years = demAge + 1;
            lifetimeIncome = (curVal * (years - 1) + newVal) / years;
        }
    }

    public Individual getLtIncomeDonor() {return ltIncomeDonor;}

    public Integer getLiwwh() {
        return labEmpNyear;
    }
}
