package simpaths.model;

import jakarta.persistence.*;
import microsim.agent.Weight;
import microsim.data.db.PanelEntityKey;
import microsim.engine.SimulationEngine;
import microsim.event.EventListener;
import microsim.statistics.IDoubleSource;
import microsim.statistics.IIntSource;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.math3.util.FastMath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import simpaths.data.ManagerRegressions;
import simpaths.data.MultiValEvent;
import simpaths.data.Parameters;
import simpaths.data.RegressionName;
import simpaths.experiment.SimPathsCollector;
import simpaths.data.filters.Filters;
import simpaths.model.annotations.Lag;
import simpaths.model.annotations.NullInitialised;
import simpaths.model.annotations.UpdateManager;
import simpaths.model.decisions.Axis;
import simpaths.model.decisions.DecisionParams;
import simpaths.model.enums.*;
import simpaths.model.lifetime_incomes.AnnualIncome;
import simpaths.model.lifetime_incomes.Individual;
import simpaths.model.person.PrivatePension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static simpaths.data.Parameters.*;

@Entity
public class Person implements EventListener, IDoubleSource, IIntSource, Weight, Comparable<Person> {

    @Transient private static Logger log = LogManager.getLogger(Person.class);
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
    private Integer demAge; //Age
    @Transient private Integer demAgeSq; //Age squared
    private Dcpst demPartnerStatus;
    @Enumerated(EnumType.STRING) private Indicator demAdultChildFlag;
    @Transient private Boolean demIoFlag = false;                           // true if a dummy person instantiated for IO decision solution
    @Enumerated(EnumType.STRING) private Gender demMaleFlag;                // gender
    @Enumerated(EnumType.STRING) private Education eduHighestC4;            //Education level (4 categories incl. in education)
    @Lag(field="eduHighestC4") @Transient private Education eduHighestC4L1;  //Lag(1) of education level
    @Enumerated(EnumType.STRING) private Education eduHighestMotherC4;      //Mother's education level
    @Enumerated(EnumType.STRING) private Education eduHighestFatherC4;      //Father's education level
    @Enumerated(EnumType.STRING) private Ethnicity demEthnC6;          //Ethnicity
    @Enumerated(EnumType.STRING) private Indicator eduSpellFlag;          // in continuous education
    @Lag(field="eduSpellFlag") @Enumerated(EnumType.STRING) private Indicator eduSpellFlagL1;          // in continuous education
    @Enumerated(EnumType.STRING) private Indicator eduReturnFlag;          // return to education
    @NullInitialised  @Enumerated(EnumType.STRING) private Les_c4 labC4;      //Activity (employment) status
    @Enumerated(EnumType.STRING) private Les_c7_covid labC7Covid; //Activity (employment) status used in the Covid-19 models
    @Lag(field="labC4") @Transient private Les_c4 labC4L1;		//Lag(1) of activity_status
    @Lag(field="labC7Covid") @Transient private Les_c7_covid labC7CovidL1;     //Lag(1) of 7-category activity status
    @Column(name="labWorkHist") private Integer labEmpNyear;                  //Work history in months (number of months in employment) (Note: this is monthly in EM, but simulation updates annually so increment by 12 months).
    @Enumerated(EnumType.STRING) private Indicator healthDsblLongtermFlag;	//Long-term sick or disabled if = 1
    @Lag(field="healthDsblLongtermFlag") @Transient private Indicator healthDsblLongtermFlagL1; //Lag(1) of long-term sick or disabled
    @Enumerated(EnumType.STRING) @Column(name="careNeedFlag") private Indicator careNeedFlag;
    @Column(name="careHrsFormal") private Double careHrsFormalWeek;
    @Column(name="careFormalX") private Double careFormalX;
    @Column(name="careHrsInformal") private Double careHrsInformalWeek;
    private Boolean labWageOfferLowFlag;
    @Lag(getter="getLowWageOffer") @Transient private Boolean labWageOfferLowFlagL1;
    @NullInitialised @Transient private SocialCareReceipt careReceivedFlag;
    @NullInitialised @Transient private Boolean careFormalFlag;
    @NullInitialised @Transient private Boolean careFromInformalFlag;
    @Column(name="careHrsProvidedWeek") private Double careHrsProvidedWeek;
    @Transient private Indicator careProvidedFlag;
    @Lag(field="careProvidedFlag") @Transient private Indicator careProvidedFlagL1;
    @Lag(field="careNeedFlag") @Transient private Indicator careNeedFlagL1;
    @Lag(field="careHrsFormalWeek") @Transient private Double careHrsFormalWeekL1;
    @Lag(field="careHrsProvidedWeek") @Transient private Double careHrsProvidedWeekL1;
    @Lag(field="careHrsInformalWeek") @Transient private Double careHrsInformalWeekL1;
    @Lag(getter="isHousingOwned") @Transient private Boolean demPrptyFlagL1;

    // partner lags
    @Lag(field="demPartnerStatusL1") @Transient private Dcpst demPartnerStatusL2;            // lag (2) partnership status
    @Lag(getter="getDemPartnerStatus") @Transient private Dcpst demPartnerStatusL1;            // lag partnership status
    @Lag(getter="getEduHighestPartner") @Transient private Education eduHighestPartnerC4L1;     //Lag(1) of partner's education
    @Lag(getter="getHealthPartner") @Transient private Dhe healthPartnerSelfRatedL1;
    @Lag(getter="getLabStatusPartnerAndOwnC4") @Transient private Lesdf_c4 labStatusPartnerAndOwnC4L1;      //Lag(1) of own and partner's activity status
    @Lag(getter="getIdPartner") @Transient private Long idPartnerL1;
    @Lag(getter="getHouseholdStatus") @Transient private HouseholdStatus demStatusHhL1;		//Lag(1) of household_status
    @Lag(getter="getAgeDifferencePartner") @Transient private Integer demAgePartnerDiffL1;        //Lag(1) of difference between ages of partners in union
    @Lag(getter="getYPersAndPartnerGrossDiffMonth") @Transient private Double yPersAndPartnerGrossDiffMonthL1;      //Lag(1) of difference between own and partner's gross personal non-benefit income

    @Enumerated(EnumType.STRING) private Indicator eduExitSampleFlag;    // year left education
    @NullInitialised @Transient private Boolean demGiveBirthFlag;
    @NullInitialised @Transient private Boolean eduLeaveSchoolFlag;
    @NullInitialised @Transient private Boolean demBePartnerFlag;
    @NullInitialised @Transient private Boolean demAlignPartnerProcess;
    @NullInitialised @Transient private Boolean demLeavePartnerFlag; // Used in partnership alignment process. Indicates that this person has found partner in a test run of union matching.
    @Column(name="wgt") private Double wgt;
    @Column(name="healthPsyDstrss0to12") private Double healthPsyDstrss0to12; //Psychological distress GHQ-12 0-12 caseness score
    @Lag(field="healthPsyDstrss0to12") @Transient private Double healthPsyDstrss0to12L1;
    @Lag(field="healthSelfRated") @Transient private Dhe healthSelfRatedL1;
    @Enumerated(EnumType.STRING) private Dhe healthSelfRated;
    private Double healthWbScore0to36; //Psychological distress GHQ-12 Likert scale
    @Lag(field="healthWbScore0to36") @Transient private Double healthWbScore0to36L1; //Lag(1) of dhm
    private Double healthMentalMcs;  //mental well-being: SF12 mental component summary score
    @Lag(field="healthMentalMcs") @Transient private Double healthMentalMcsL1;  //mental well-being: SF12 mental component summary score lag 1
    private Double healthPhysicalPcs;  //physical well-being: SF12 physical component summary score
    @Lag(field="healthPhysicalPcs") @Transient private Double healthPhysicalPcsL1;  //physical well-being: SF12 physical component summary score lag 1
    private Double healthMentalPartnerMcs; //mental well-being: SF12 mental component summary score (partner)
    private Double healthPhysicalPartnerPcs; //physical well-being: SF12 physical component summary score (partner)
    private Double demLifeSatScore0to10;      //life satisfaction - score 0-10
    @Lag(field="demLifeSatScore0to10") @Transient private Double demLifeSatScore0to10L1;      //life satisfaction - score 0-10 lag 1
    @Column(name="demLifeSatEQ5D") private Double demLifeSatEQ5D;
    @Column(name="yFinDstrssFlag") private Boolean yFinDstrssFlag;
    @Lag(field="yBenReceivedFlag") @Transient private Boolean yBenReceivedFlagL1; // Lag(1) of whether person receives benefits
    @NullInitialised @Transient private Boolean yBenReceivedFlag; // Does person receive benefits
    @Column(name="yBenUCReceivedFlag") private Boolean yBenUCReceivedFlag; // Person receives UC
    @Lag(field="yBenUCReceivedFlag") @Transient private Boolean yBenUCReceivedFlagL1;
    @Column(name="yBenNonUCReceivedFlag") private Boolean yBenNonUCReceivedFlag;  // Person receives a benefit which is not UC
    @Lag(field="yBenNonUCReceivedFlag") @Transient private Boolean yBenNonUCReceivedFlagL1;
    @NullInitialised @Column(name="yLifeTime") private Double yLifeTime;                  // mean annual equivalised household disposable income by age

    @Enumerated(EnumType.STRING) private Labour labHrsWorkEnumWeek;			//Number of hours of labour supplied each week
    @Lag(getter="getLabourSupplyWeekly") @Transient private Labour labHrsWorkEnumWeekL1; // Lag(1) (previous year's value) of weekly labour supply
    @Column(name = "HOURS_WORKED_WEEKLY") private Integer labHrsWorkWeek;
    @Lag(field="labHrsWorkWeek") @Transient private Integer labHrsWorkWeekL1; // Lag(1) of hours worked weekly - use to initialise labour supply weekly_L1 (TODO)

//	Potential earnings is the gross hourly wage an individual can earn while working
//	and is estimated, for each individual, on the basis of observable characteristics as
//	age, education, civil status, number of children, etc. Hence, potential earnings
//	is a separate process in the simulation, and it is computed for every adult
//	individual in the simulated population, in each simulated period.
    @Column(name="labWageHrly") private Double labWageFullTimeHrly;		//Is hourly rate.  Initialised with value: ils_earns / (4.34 * lhw), where lhw is the weekly hours a person worked in EUROMOD input data
    @Lag(field="labWageFullTimeHrly") @Transient private Double labWageFullTimeHrlyL1; // Lag(1) of potentialHourlyEarnings
    @Transient private Double yDispEquivYear;
    @NullInitialised private Double xEquivYear;
    @Lag(field="xEquivYear") @Transient private Double xEquivYearL1;
    private Integer demPartnerNYear; //Number of years in partnership
    @Transient private Integer demPartnerNYearL1; //Lag(1) of number of years in partnership

    // capital and pension income
    private Double yNonBenPersGrossMonth; // asinh of personal non-benefit income per month
    @Lag(getter="getYNonBenPersGrossMonth") @Transient private Double yNonBenPersGrossMonthL1; //Lag(1) of gross personal non-benefit income
    private Double yMiscPersGrossMonth; // asinh of non-employment non-benefit income per month (capital and pension)
    private Double yCapitalPersMonth; // asinh of capital income per month
    @Lag(field="yCapitalPersMonthL1") @Transient private Double yCapitalPersMonthL2; //Lag(2) of capital income
    @Lag(getter="getYCapitalPersMonth") @Transient private Double yCapitalPersMonthL1; //Lag(1) of ypncp
    @Lag(field="yMiscPersGrossMonthL2") @Transient private Double yMiscPersGrossMonthL3; //Lag(3) of gross personal non-benefit non-employment income
    @Lag(field="yMiscPersGrossMonthL1") @Transient private Double yMiscPersGrossMonthL2; //Lag(2) of gross personal non-benefit non-employment income
    @Lag(getter="getYMiscPersGrossMonth") @Transient private Double yMiscPersGrossMonthL1; //Lag(1) of gross personal non-benefit non-employment income
    private Double yEmpPersGrossMonth;       // asinh transform of personal labour income per month
    @Lag(field="yEmpPersGrossMonthL2") @Transient private Double yEmpPersGrossMonthL3; //Lag(3) of gross personal employment income
    @Lag(field="yEmpPersGrossMonthL1") @Transient private Double yEmpPersGrossMonthL2; //Lag(2) of gross personal employment income
    @Lag(getter="getYEmpPersGrossMonth") @Transient private Double yEmpPersGrossMonthL1; //Lag(1) of gross personal employment income

    // pension wealth
    @NullInitialised @Transient private PrivatePension privatePension;
    @Lag(getter="getPrivatePension") @Transient private PrivatePension privatePensionL1;
    private Double yPensPersGrossMonth; // asinh of pension income per month
    @Lag(getter="getYPensPersGrossMonth") @Transient private Double yPensPersGrossMonthL1; //Lag(1) of pension income
    @Lag(field="yPensPersGrossMonthL1") @Transient private Double yPensPersGrossMonthL2; //Lag(2) of pension income
    @NullInitialised private Double wealthPensValue;
    @NullInitialised private Double contRateOPEe;           //employee contribution rate to occupation pension
    @NullInitialised private Double contRateOPEr;           //employer contribution rate to occupation pension
    @NullInitialised private Double contRatePP;             //employee contribution rate to personal pension

    // non-pension wealth
    @NullInitialised private Double wealthNonPensValue;
    @Lag(field="wealthNonPensValue") @Transient private Double wealthNonPensValueL1;

    //For matching process
    @Transient private Double demAgeDiffDesired;
    @Transient private Double yWageDesired;
    @NullInitialised @Transient private Integer demAgeGroup;

    //This is set to true at the point when individual leaves education and never reset. So if true, individual has not always been in continuous education.
    @Transient private Boolean eduLeftEduFlag;

    //This is set to true at the point when individual leaves partnership and never reset. So if true, individual has been / is in a partnership
    @NullInitialised @Transient private Boolean demLeftPartnerFlag;
    @NullInitialised @Transient private Integer labHrsWorkNewL1; // Define a variable to keep previous month's value of work hours to be used in the Covid-19 module
    @NullInitialised @Transient private Double covidYLabGrossL1;
    @Transient private Indicator covidSEISSReceivedFlag = Indicator.False;
    @NullInitialised @Transient private Double covidYLabGross;
    @NullInitialised private Quintiles covidYLabGrossXt5;
    @NullInitialised @Transient private Double labWageRegressRandomCompoponentEmp;
    @NullInitialised @Transient private Double labWageRegressRandomCompoponentNotEmp;
    @Transient private Map<Labour, Integer> personContinuousHoursLabourSupplyMap = new EnumMap<>(Labour.class);

    // local variables interact with regression models
    @NullInitialised @Transient private Integer i_demYear;
    @NullInitialised @Transient private Region i_demRgn;
    @NullInitialised @Transient private Dhhtp_c4 i_demCompHhC4L1;
    @NullInitialised @Transient private Ydses_c5 i_yHhQuintilesC5;
    @NullInitialised @Transient private Integer i_demNchildL1;
    @NullInitialised @Transient private Integer i_demNchild;
    @NullInitialised @Transient private Integer i_demNchild0to2L1;
    @NullInitialised @Transient private Integer i_demNchild0to17;
    @NullInitialised @Transient private Indicator i_demNChild0to2;
    @NullInitialised @Transient private Dcpst i_demPartnerStatus;
    @NullInitialised @Transient private Quintiles i_yEarningsQuintileC5;

    // innovations
    @Transient Innovations statInnovations;

    //TODO: Remove when no longer needed.  Used to calculate mean score of employment selection regression.
    @NullInitialised @Transient public static Double statMScore;
    @NullInitialised @Transient public static Double statFScore;
    @NullInitialised @Transient public static Double countMale;
    @NullInitialised @Transient public static Double countFemale;
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
        eduHighestMotherC4 = mother.getEduHighestC4();
        if (mother.getPartner()==null) {
            idFather = null;
            eduHighestFatherC4 = mother.getEduHighestC4();
        } else {
            idFather = mother.getPartner().getId();
            eduHighestFatherC4 = mother.getPartner().getEduHighestC4();
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
        demAgeSq = demAge * demAge;
        eduSpellFlag = Indicator.True ;
        eduReturnFlag = Indicator.False;
        wgt = mother.getWgt();			//Newborn has same weight as mother (the number of newborns will then be aligned in fertility alignment)
        healthSelfRated = Dhe.VeryGood;
        healthWbScore0to36 = 10.;			//Set to median for under 18's as a placeholder
        healthPsyDstrss0to12 = 0.;
        healthMentalMcs = 48.;
        healthPhysicalPcs = 56.;
        demLifeSatScore0to10 = 6.;
        eduHighestC4 = Education.InEducation;
        demEthnC6 = mother.getDemEthnC6();
        labC4 = Les_c4.Student;				//Set lag activity status as Student, i.e. in education from birth
        labC4L1 = Les_c4.Student;
        eduLeftEduFlag = false;
        labC7Covid = Les_c7_covid.Student;
        labHrsWorkEnumWeek = Labour.ZERO;			//Will be updated in Labour Market Module when the person stops being a student
        labHrsWorkWeek = getLabourSupplyWeekly().getHours(this);
        idHh = mother.getBenefitUnit().getHousehold().getId();
//		setDeviationFromMeanRetirementAge();			//This would normally be done within initialisation, but the line above has been commented out for reasons given...
        yDispEquivYear = getBenefitUnit().getEquivalisedDisposableIncomeYearly();
        xEquivYear = 0.;
        yLifeTime = 0.;
        demBornInSimFlag = true;
        yBenReceivedFlag = false;
        yBenNonUCReceivedFlag = false;
        yBenUCReceivedFlag = false;
        yFinDstrssFlag = mother.getYFinDstrssFlag();
    }

    // a "copy constructor" for persons: used by the cloneBenefitUnit method of the SimPathsModel object
    // used to generate clones both at population load (to un-weight data) and to generate international immigrants
    public Person (Person originalPerson, long statSeed, SampleEntry demEnterSample) {

        this(personIdCounter++, statSeed);
        switch (demEnterSample) {
            case ProcessedInputData -> {
                key.setId(originalPerson.getId());
                idPersOriginal = originalPerson.getIdPersOriginal();
                idBuOriginal = originalPerson.getIdBuOriginal();
                idHhOriginal = originalPerson.getIdHhOriginal();
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
            this.yLifeTime = originalPerson.getYLifeTime();

        demAge = originalPerson.demAge;
        demAgeSq = demAge * demAge;
        demAgeGroup = originalPerson.demAgeGroup;
        demMaleFlag = originalPerson.demMaleFlag;
        eduHighestC4 = originalPerson.eduHighestC4;

        if (originalPerson.eduHighestC4L1 != null) { //If original person misses lagged level of education, assign current level of education
            eduHighestC4L1 = originalPerson.eduHighestC4L1;
        } else {
            eduHighestC4L1 = eduHighestC4;
        }

        eduHighestFatherC4 = originalPerson.eduHighestFatherC4;
        eduHighestMotherC4 = originalPerson.eduHighestMotherC4;
        eduHighestPartnerC4L1 = originalPerson.eduHighestC4L1;

        // set eduSpellFlag
        if (originalPerson.demAge < Parameters.MIN_AGE_TO_LEAVE_EDUCATION) { //If under age to leave education, set flag for being in education to true
            eduSpellFlag = Indicator.True;
        } else {
            eduSpellFlag = originalPerson.eduSpellFlag;
        }

        if (originalPerson.eduSpellFlagL1 != null) { //If original person misses lagged level of education, assign current level of education
            eduSpellFlagL1 = originalPerson.eduSpellFlagL1;
        } else {
            eduSpellFlagL1 = eduSpellFlag;
        }

        eduReturnFlag = originalPerson.eduReturnFlag;
        demPartnerNYear = Objects.requireNonNullElse(originalPerson.demPartnerNYear,0);
        demPartnerNYearL1 = Objects.requireNonNullElseGet(originalPerson.demPartnerNYearL1, () -> Math.max(0, this.demPartnerNYear - 1));
        demAgePartnerDiffL1 = originalPerson.demAgePartnerDiffL1;
        demStatusHhL1 = originalPerson.demStatusHhL1;
        if (demAge > MAX_AGE_FLEXIBLE_LABOUR_SUPPLY)
            labC4 = Les_c4.Retired;
        else if (originalPerson.yPensPersGrossMonth > 0. && originalPerson.demAge >= MIN_AGE_TO_RETIRE)
            labC4 = Les_c4.Retired;
        else if (originalPerson.demAge < Parameters.MIN_AGE_TO_LEAVE_EDUCATION)
            labC4 = Les_c4.Student;
        else if (originalPerson.labC4 != null)
            labC4 = originalPerson.labC4;
        else if (originalPerson.demAge > (int)Parameters.getTimeSeriesValue(model.getYear(), originalPerson.getDemMaleFlag().toString(), TimeSeriesVariable.FixedRetirementAge))
            labC4 = Les_c4.Retired;
        else if (originalPerson.getLabourSupplyWeekly() != null && originalPerson.getLabourSupplyWeekly().getHours(originalPerson) > 0)
            labC4 = Les_c4.EmployedOrSelfEmployed;
        else
            labC4 = Les_c4.NotEmployed;
        labC4L1 = labC4;
        if (demAge < Parameters.MIN_AGE_TO_LEAVE_EDUCATION)
            eduLeftEduFlag = false;
        else if (demAge > Parameters.MAX_AGE_TO_STAY_IN_CONTINUOUS_EDUCATION)
            eduLeftEduFlag = true;
        else
            eduLeftEduFlag = (!Les_c4.Student.equals(labC4) || (Les_c4.Student.equals(labC4) && eduSpellFlag.equals(Indicator.False))) ;

        if (labC4.equals(Les_c4.Student))
            labC7Covid = Les_c7_covid.Student;
        else if (labC4.equals(Les_c4.Retired))
            labC7Covid = Les_c7_covid.Retired;
        else if (originalPerson.labC7Covid != null)
            labC7Covid = originalPerson.labC7Covid;
        else
            labC7Covid = Les_c7_covid.NotEmployed;
        labC7CovidL1 = labC7Covid;

        labStatusPartnerAndOwnC4L1 = originalPerson.labStatusPartnerAndOwnC4L1;
        demPartnerStatusL1 = originalPerson.demPartnerStatusL1;
        demPartnerStatusL2 = originalPerson.demPartnerStatusL2;
        yNonBenPersGrossMonth = originalPerson.getYNonBenPersGrossMonth();
        yNonBenPersGrossMonthL1 = originalPerson.yNonBenPersGrossMonthL1;
        yMiscPersGrossMonth = Objects.requireNonNullElse(originalPerson.yMiscPersGrossMonth, 0.0);
        yEmpPersGrossMonth = originalPerson.getYEmpPersGrossMonth();
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

        wealthNonPensValue = originalPerson.wealthNonPensValue;
        wealthPensValue = originalPerson.wealthPensValue;
        contRateOPEe = originalPerson.contRateOPEe;
        contRateOPEr = originalPerson.contRateOPEr;
        contRatePP = originalPerson.contRatePP;
        if (originalPerson.privatePension != null)
            privatePension = new PrivatePension(originalPerson.privatePension);
        if (originalPerson.privatePensionL1 != null)
            privatePensionL1 = new PrivatePension(originalPerson.privatePensionL1);

        labEmpNyear = Objects.requireNonNullElseGet(originalPerson.labEmpNyear, () -> ((Les_c4.EmployedOrSelfEmployed.equals(labC4)) ? 12 : 0));
        healthDsblLongtermFlag = originalPerson.healthDsblLongtermFlag;
        healthDsblLongtermFlagL1 = originalPerson.healthDsblLongtermFlagL1;
        careNeedFlag = Objects.requireNonNullElse(originalPerson.careNeedFlag, Indicator.False);
        careHrsFormalWeek = Objects.requireNonNullElse(originalPerson.careHrsFormalWeek, 0.0);
        careFormalX = Objects.requireNonNullElse(originalPerson.careFormalX, 0.0);
        careHrsInformalWeek = Objects.requireNonNullElse(originalPerson.careHrsInformalWeek, 0.0);
        careFormalFlag = Objects.requireNonNullElseGet(originalPerson.careFormalFlag, () -> (careHrsFormalWeek > 0.0));
        careFromInformalFlag = Objects.requireNonNullElseGet(originalPerson.careFromInformalFlag, () -> (careHrsInformalWeek > 0.0));
        if (originalPerson.careReceivedFlag !=null)
            careReceivedFlag = originalPerson.careReceivedFlag;
        else {
            if (careFormalFlag) {
                if (careFromInformalFlag)
                    careReceivedFlag = SocialCareReceipt.Mixed;
                else
                    careReceivedFlag = SocialCareReceipt.Formal;
            } else {
                if (careFromInformalFlag)
                    careReceivedFlag = SocialCareReceipt.Informal;
                else
                    careReceivedFlag = SocialCareReceipt.None;
            }
        }

        careHrsProvidedWeek = Objects.requireNonNullElse(originalPerson.careHrsProvidedWeek, 0.0);
        careProvidedFlag = Objects.requireNonNullElseGet(originalPerson.careProvidedFlag, () ->
                (careHrsProvidedWeek > 0.01) ? Indicator.True : Indicator.False);

        careNeedFlagL1 = Objects.requireNonNullElse(originalPerson.careNeedFlagL1, careNeedFlag);
        careHrsFormalWeekL1 = Objects.requireNonNullElse(originalPerson.careHrsFormalWeekL1, careHrsFormalWeek);
        careHrsInformalWeekL1 = Objects.requireNonNullElse(originalPerson.careHrsInformalWeekL1, careHrsInformalWeek);
        careHrsProvidedWeekL1 = Objects.requireNonNullElse(originalPerson.careHrsProvidedWeekL1, careHrsProvidedWeek);
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

        healthPsyDstrss0to12 = Objects.requireNonNullElse(originalPerson.healthPsyDstrss0to12, 0.);
        healthPsyDstrss0to12L1 = Objects.requireNonNullElse(originalPerson.healthPsyDstrss0to12L1, healthPsyDstrss0to12);

        demLifeSatScore0to10 = originalPerson.demLifeSatScore0to10;
        healthMentalMcs = originalPerson.healthMentalMcs;
        healthPhysicalPcs = originalPerson.healthPhysicalPcs;
        healthMentalPartnerMcs = originalPerson.healthMentalPartnerMcs;
        healthPhysicalPartnerPcs = originalPerson.healthPhysicalPartnerPcs;

        if (originalPerson.demLifeSatScore0to10L1 != null) {
            demLifeSatScore0to10L1 = originalPerson.demLifeSatScore0to10L1;
        } else {
            demLifeSatScore0to10L1 = originalPerson.demLifeSatScore0to10;
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

        demAdultChildFlag = originalPerson.demAdultChildFlag;
        yDispEquivYear = originalPerson.getBenefitUnit().getEquivalisedDisposableIncomeYearly();
        xEquivYear = originalPerson.xEquivYear;
        demEthnC6 = originalPerson.demEthnC6;
        yBenReceivedFlag = originalPerson.yBenReceivedFlag;
        yBenReceivedFlagL1 = originalPerson.yBenReceivedFlagL1;
        yBenNonUCReceivedFlag = originalPerson.yBenNonUCReceivedFlag;
        yBenNonUCReceivedFlagL1 = originalPerson.yBenNonUCReceivedFlagL1;
        yBenUCReceivedFlag = originalPerson.yBenUCReceivedFlag;
        yBenUCReceivedFlagL1 = originalPerson.yBenUCReceivedFlagL1;
        yFinDstrssFlag = originalPerson.yFinDstrssFlag;

        if (originalPerson.labWageFullTimeHrly >= Parameters.MIN_HOURLY_WAGE_RATE) {
            labWageFullTimeHrly = Math.min(Parameters.MAX_HOURLY_WAGE_RATE, Math.max(Parameters.MIN_HOURLY_WAGE_RATE, originalPerson.labWageFullTimeHrly));
        } else {
            labWageFullTimeHrly = 0.0;
            labHrsWorkWeek = 0;
            if (!Les_c4.Student.equals(labC4) && !Les_c4.Retired.equals(labC4)) {
                labC4 = Les_c4.NotEmployed;
            }
        }
        if (originalPerson.labWageFullTimeHrlyL1 !=null && originalPerson.labWageFullTimeHrlyL1 >= Parameters.MIN_HOURLY_WAGE_RATE) {
            labWageFullTimeHrlyL1 = Math.min(Parameters.MAX_HOURLY_WAGE_RATE, Math.max(Parameters.MIN_HOURLY_WAGE_RATE, originalPerson.labWageFullTimeHrlyL1));
        } else  {
            if (originalPerson.labWageFullTimeHrlyL1 == null)
                labWageFullTimeHrlyL1 = labWageFullTimeHrly;
            if (labWageFullTimeHrlyL1 == null || labWageFullTimeHrlyL1 < Parameters.MIN_HOURLY_WAGE_RATE) {
                labWageFullTimeHrlyL1 = 0.0;
                labHrsWorkWeekL1 = 0;
                if (!Les_c4.Student.equals(labC4L1) && !Les_c4.Retired.equals(labC4L1)) {
                    labC4L1 = Les_c4.NotEmployed;
                }
            }
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
        statInnovations = new Innovations(44, 1, 1, statSeed);

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
        careHrsInformalWeek = -9.0;
        careHrsProvidedWeek = -9.0;
        careFormalX = -9.0;
        careReceivedFlag = SocialCareReceipt.None;
        careFormalFlag = false;
        careFromInformalFlag = false;
        careProvidedFlag = Indicator.False;
        careNeedFlagL1 = Indicator.False;
        careHrsFormalWeekL1 = -9.0;
        careHrsInformalWeekL1 = -9.0;
        careHrsProvidedWeekL1 = -9.0;
        careProvidedFlagL1 = Indicator.False;
    }


    /*******************************************************
     * method to generate additional person level characteristics
     * for initialisation population
     *******************************************************/
    public void setAdditionalFieldsInInitialPopulation() {

        Person partner = getPartner();

        if (labHrsWorkEnumWeek ==null)
            labHrsWorkEnumWeek = Labour.convertHoursToLabour(model.getInitialHoursWorkedWeekly().get(key.getId()).intValue()); // TODO: this can be simplified to obtain value from already initialised hours worked weekly variable? The entire database query on setup is redundant? See initialisation of the lag below.
        if (labHrsWorkWeek == null) {
            Map<Long, Double> initialHours = model.getInitialHoursWorkedWeekly();
            if (initialHours != null && initialHours.get(key.getId()) != null) {
                labHrsWorkWeek = initialHours.get(key.getId()).intValue();
            } else if (labHrsWorkEnumWeek != null) {
                // Fallback for processed populations where initial-hours map is unavailable.
                // Use discretised enum value here to avoid requiring random draws before innovations are initialised.
                labHrsWorkWeek = labHrsWorkEnumWeek.getValue();
            }
        }

        if(UnionMatchingMethod.SBAM.equals(model.getUnionMatchingMethod())) {
            updateAgeGroup();
        }

        demGiveBirthFlag = false;
        demBePartnerFlag = false;
        demLeavePartnerFlag = false;
        eduLeaveSchoolFlag = false;
        setEduExitSampleFlag(Indicator.False); //This variable is False by default
        // is set to true only when person leaves school in this specific year
        // eduSpellFlag = (Les_c4.Student.equals(labC4)) ? Indicator.True : Indicator.False;
        // no need to update eduSpellFlag as its value is persisted from the previous year

        if (!Parameters.isFinite(careHrsInformalWeek))
            careHrsInformalWeek = 0.0;
        if (careHrsProvidedWeek < 0.01) {
            careProvidedFlag = Indicator.False;
        } else {
            careProvidedFlag = Indicator.True;
        }
        if (demAge < Parameters.AGE_TO_BECOME_RESPONSIBLE) {
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
        if (partner != null) {
            this.setHealthMentalPartnerMcs(partner.getHealthMentalMcs());
            this.setHealthPhysicalPartnerPcs(partner.getHealthPhysicalPcs());
        }

        //Lagged variables
        yEmpPersGrossMonthL1 = getYEmpPersGrossMonth();
        yEmpPersGrossMonthL2 = getYEmpPersGrossMonth();
        yMiscPersGrossMonthL1 = getYMiscPersGrossMonth();
        yMiscPersGrossMonthL2 = getYMiscPersGrossMonth();
        yCapitalPersMonthL1 = getYCapitalPersMonth();
        yPensPersGrossMonthL1 = getYPensPersGrossMonth();
        demPartnerStatusL1 = demPartnerStatus;

        // instantiate privatePension
        wealthPensValue = 0.0;
        wealthNonPensValue = 0.0;
        privatePension = new PrivatePension();
        if (demAge >= Parameters.AGE_TO_BECOME_RESPONSIBLE) {

            if (partner != null) {
                wealthPensValue = getBenefitUnit().getWealthPensValue() / 2.0;
                wealthNonPensValue = getBenefitUnit().getWealthNonPensValue() / 2.0;
            } else {
                wealthPensValue = getBenefitUnit().getWealthPensValue();
                wealthNonPensValue = getBenefitUnit().getWealthNonPensValue();
            }
            privatePension.setWealth(wealthPensValue);
            privatePension.setPensionIncomeAnnual(Math.sinh(yPensPersGrossMonth) * 12.0);
            privatePension.setContRateOPEe(contRateOPEe);
            privatePension.setContRateOPEr(contRateOPEr);
            privatePension.setContRatePP(contRatePP);
            privatePension.setRetired(Les_c4.Retired.equals(getLabC4()));
        }
    }

    //This method assign people to age groups used to define types in the SBAM matching procedure
    private void updateAgeGroup() {
        if (demAge < AGE_TO_BECOME_RESPONSIBLE) {
            demAgeGroup = 0;
            model.tmpPeopleAssigned++;
        } else if(demAge >= AGE_TO_BECOME_RESPONSIBLE && demAge < 21) {
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
        ConsiderLeavingHome,
        ConsiderMortality,
        Retirement,
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
        UpdateNonPensionWealth,
        UpdatePensionContributionStatus,
        UpdatePensionWealth,
        Unemployment,
        Update,
        UpdateOutputVariables,
        UpdatePotentialHourlyEarnings,      //Needed to union matching and labour supply
    }

    @Override
    public void onEvent(Enum<?> type) {
        switch ((Processes) type) {
            case Aging -> {
                aging();
            }
            case Update -> {
                updateAttributes();
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
            case ConsiderLeavingHome -> {
                considerLeavingHome();
            }
            case ConsiderMortality -> {
                considerMortality();
            }
            case Retirement -> {
                updateRetirementStatus();
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
            case UpdateNonPensionWealth -> {
                updateNonPensionWealth();
            }
            case UpdatePensionContributionStatus -> {
                updatePensionContributionStatus();
            }
            case UpdatePensionWealth -> {
                updatePensionWealth();
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
        fertility(model.getFertilityAdjustment());
    }

    public void fertility(double probitAdjustment) {

        demGiveBirthFlag = false;
        var filter = Filters.fertile();

        if (filter.test(this)) {

            double prob;

            if (model.getCountry().equals(Country.UK)) {

                //If age below or equal to 29 and in continuous education follow process F1a
                double score = Parameters.getRegFertilityF1().getScore(this, Variables.class);
                if (!Parameters.isFinite(score))
                    throw new ArithmeticException("Failure to evaluate score for fertility rate");
                prob = Parameters.getRegFertilityF1().getProbability(score + probitAdjustment);

                // else {
                //     //Otherwise if not in continuous education, follow process F1b
                //     double score = Parameters.getRegFertilityF1b().getScore(this, Person.DoublesVariables.class);
                //     prob = Parameters.getRegFertilityF1b().getProbability(score + probitAdjustment);
                // }
                // } else if (model.getCountry().equals(Country.IT)) {
                //     prob = Parameters.getRegFertilityF1().getProbability(this, Person.DoublesVariables.class);
            } else {
                throw new RuntimeException("Country not recognised when evaluating fertility status");
            }

            if (statInnovations.getDoubleDraw(29)<prob) {
                demGiveBirthFlag = true;
            }
        }
    }

    public void updateNonPensionWealth() {

        if (!Parameters.projectNonPensionWealth)
            return;

        if (demAge < Parameters.AGE_TO_BECOME_RESPONSIBLE) {

            wealthNonPensValue = 0.0;
        } else {

            double accrual = getBenefitUnit().getPerAdultWealthAccrual();
            if (wealthNonPensValueL1 == null)
                throw new RuntimeException("Non-pension wealth not initialised");
            wealthNonPensValue = wealthNonPensValueL1 + accrual;
        }
    }

    public void updatePensionContributionStatus() {

        if (Parameters.projectPensionWealth) {

            if (privatePension != null) {
                if (privatePension.getPensionIncomeAnnual() > 0.0)
                    throw new RuntimeException("pension contribution update requested for retiree");
            }

            privatePension = new PrivatePension();
            privatePension.membership(this, privatePensionL1.isMemberOP(), privatePensionL1.isMemberPP(), statInnovations.getDoubleDraw(5), statInnovations.getDoubleDraw(6));
            privatePension.contributionRates(this, statInnovations.getDoubleDraw(39), statInnovations.getDoubleDraw(40),
                    statInnovations.getDoubleDraw(41), statInnovations.getDoubleDraw(42), statInnovations.getDoubleDraw(43));
        } else {

            if (privatePension == null)
                privatePension = new PrivatePension();
        }
    }

    /*******************************************************
     * Method to obtain private pension contribution (omitting employer contribution)
     * Used for reducing earnings prior to evaluation of taxes and benefits
     * @return
     ********************************************************/
    public double getPrivatePensionContributionRate() {

        if (privatePension == null) {
            if (Parameters.projectPensionWealth) {
                throw new RuntimeException("Private pension not initialised");
            } else {
                privatePension = new PrivatePension();
            }
        }
        return privatePension.getContRateOPEe() + privatePension.getContRatePP();
    }

    public void updatePensionWealth() {

        if (!Parameters.projectPensionWealth)
            return;

        if (privatePension == null)
            privatePension = new PrivatePension();

        if (demAge >= Parameters.AGE_TO_BECOME_RESPONSIBLE && !Les_c4.Retired.equals(labC4))
            privatePension.projectWealth(privatePensionL1.getWealth(), getGrossEarningsYearly(), Parameters.getTimeSeriesRate(model.getYear(), TimeVaryingRate.RealPensionReturn), labC4);

        wealthPensValue = privatePension.getWealth();
        contRateOPEe = privatePension.getContRateOPEe();
        contRateOPEr = privatePension.getContRateOPEr();
        contRatePP = privatePension.getContRatePP();
    }

    private void updateUnemploymentState() {
        labWageOfferLowFlag = false;
        if (Parameters.flagUnemployment) {

            if (this == benefitUnit.getRefPersonForDecisions()) {
                // unemployment currently limited to reference person for decisions

                double prob;
                if (demMaleFlag.equals(Gender.Male)) {
                    if (eduHighestC4.equals(Education.High)) {
                        prob = Parameters.getRegUnemploymentMaleGraduateU1a().getProbability(this, Variables.class);
                    } else {
                        prob = Parameters.getRegUnemploymentMaleNonGraduateU1b().getProbability(this, Variables.class);
                    }
                } else {
                    if (eduHighestC4.equals(Education.High)) {
                        prob = Parameters.getRegUnemploymentFemaleGraduateU1c().getProbability(this, Variables.class);
                    } else {
                        prob = Parameters.getRegUnemploymentFemaleNonGraduateU1d().getProbability(this, Variables.class);
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
        demAgeSq = demAge * demAge;
        if (demAge == Parameters.AGE_TO_BECOME_RESPONSIBLE) {
            setupNewBenefitUnit(true);
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

        //Age eligibility: only those within the age range to leave the parental home are considered
        if (demAge < Parameters.MIN_AGE_LEAVE_PH || demAge > Parameters.MAX_AGE_ADULT_CHILD) {
            return;
        }

        //Above the age to leave home, only those still living with their parents are considered (people can move out, but not move back in)
        if (demAge > Parameters.MIN_AGE_LEAVE_PH && !Indicator.True.equals(demAdultChildFlag)) {
            return;
        }

        //Those in continuous education are not allowed to leave home, to match the filtering condition of the estimated equation
        if (Indicator.True.equals(eduSpellFlag)) {
            demAdultChildFlag = Indicator.True;
            return;
        }

        // Evaluate whether the person should leave the parental home:
        //  - if not, set demAdultChildFlag to true (remains living with parents)
        //  - if yes, set demAdultChildFlag to false and set up a new household

        double prob = Parameters.getRegLeaveHomeP1a().getProbability(this, Variables.class);
        boolean toLeaveHome = (statInnovations.getDoubleDraw(21) < prob);
        if (!toLeaveHome) { //If at the age to leave home but regression outcome is negative, person has adultchildflag set to true (although they still set up a new benefitUnit in the simulation, it's treated differently in the labour supply)

            demAdultChildFlag = Indicator.True;
        } else {

            demAdultChildFlag = Indicator.False;
            setupNewHousehold(); //If person leaves home, they set up a new household
        }
    }


    /*******************************************
     * considers retirement status
     * @return true if to retire
     *******************************************/
    public boolean updateRetirementStatus() {
        boolean toRetire = false;
        if (demAge >= MIN_AGE_TO_RETIRE && !Les_c4.Retired.equals(labC4L1)) {

            if (Parameters.enableIntertemporalOptimisations && DecisionParams.flagRetirement) {

                if (Labour.ZERO.equals(labHrsWorkEnumWeekL1)) {
                    toRetire = true;
                }
            } else {

                if ( demAge <= MAX_AGE_FLEXIBLE_LABOUR_SUPPLY) {

                    double prob;
                    if (getPartner() != null) {
                        prob = Parameters.getRegRetirementR1b().getProbability(this, Variables.class);
                    } else {
                        prob = Parameters.getRegRetirementR1a().getProbability(this, Variables.class);
                    }
                    toRetire = (statInnovations.getDoubleDraw(23) < prob);
                } else {
                    toRetire = true;
                }
            }
        }
        if (toRetire || Les_c4.Retired.equals(labC4L1)) {

            setLabC4(Les_c4.Retired);
        }
        return toRetire;
    }


    private void updateFinancialDistress() {

        if (demAge >= Parameters.AGE_TO_BECOME_RESPONSIBLE) {

            double prob = Parameters.getRegFinancialDistress().getProbability(this, Variables.class);
            yFinDstrssFlag = statInnovations.getDoubleDraw(32) < prob;
        } else {
            yFinDstrssFlag = false;
        }
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
        if (demAge >= MIN_AGE_TO_HAVE_INCOME) {
            double score = Parameters.getRegHealthHM1Level().getScore(this, Variables.class);
            if (!Parameters.isFinite(score))
                throw new ArithmeticException("Failure to evaluate score for mental health level");
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
        if (demAge >= 25 && demAge < MIN_AGE_SOCIAL_CARE) {
            if (Gender.Male.equals(getDemMaleFlag())) {
                dhmPrediction = Parameters.getRegHealthHM2LevelMales().getScore(this, Variables.class);
                if (!Parameters.isFinite(dhmPrediction))
                    throw new ArithmeticException("Failure to evaluate score for mental health level (HM2)");
                healthWbScore0to36 = constrainDhmEstimate(dhmPrediction+ healthWbScore0to36);
            } else if (Gender.Female.equals(getDemMaleFlag())) {
                dhmPrediction = Parameters.getRegHealthHM2LevelFemales().getScore(this, Variables.class);
                if (!Parameters.isFinite(dhmPrediction))
                    throw new ArithmeticException("Failure to evaluate score for mental health level (HM2b)");
                healthWbScore0to36 = constrainDhmEstimate(dhmPrediction+ healthWbScore0to36);
            } else System.out.println("healthMentalHM2 method in Person class: Person has no gender!");
        } else if (healthWbScore0to36 != null) {
            healthWbScore0to36 = constrainDhmEstimate(healthWbScore0to36);
        }
    }


    /**
     * Health and wellbeing - GHQ-12 subjective wellbeing, Caseness scale 0-12 step 1
     *
     * <p>Calculates the 'baseline' yearly update to the GHQ-12 Caseness (cases) score ({@code healthPsyDstrss0to12}) based on demographic variables.
     * Runs <b>before</b> {@link #healthMentalHM2Case()}.</p>
     *
     * @filter Age 16+
     * @updates {@code Person.healthPsyDstrss0to12}
     * @see <a href="https://www.understandingsociety.ac.uk/documentation/mainstage/variables/scghq2_dv/">scghq2_dv</a>
     */
    protected void healthMentalHM1Case() {
        if (demAge >= MIN_AGE_TO_HAVE_INCOME) {
            Map<DhmGhq,Double> probs = ManagerRegressions.getProbabilities(this, RegressionName.HealthHM1Case);
            var event = new MultiValEvent<DhmGhq>(probs, statInnovations.getDoubleDraw(36));
            healthPsyDstrss0to12 = Double.valueOf(event.eval().getValue());
        }
    }

    /**
     * Health and wellbeing - GHQ-12 subjective wellbeing, Caseness scale 0-12 step 2
     *
     * <p>Updates GHQ-12 Caseness (cases) score ({@code healthPsyDstrss0to12}) from the causal effects of economic transitions.
     * Applies separate estimates for Male and Female Persons.
     * Runs <b>after</b> {@link #healthMentalHM1Case()}</p>
     *
     * @filter Age 25-64
     * @updates {@code Person.healthPsyDstrss0to12}
     * @see <a href="https://www.understandingsociety.ac.uk/documentation/mainstage/variables/scghq2_dv/">scghq2_dv</a>
     */
    protected void healthMentalHM2Case() {
        double dhmGhqPrediction;
        if (demAge >= 25 && demAge < MIN_AGE_SOCIAL_CARE) {
            if (Gender.Male.equals(getDemMaleFlag())) {
                dhmGhqPrediction = Parameters.getRegHealthHM2CaseMales().getScore(this, Variables.class);
                healthPsyDstrss0to12 = constrainhealthPsyDstrssEstimate(dhmGhqPrediction+ healthPsyDstrss0to12);
            } else if (Gender.Female.equals(getDemMaleFlag())) {
                dhmGhqPrediction = Parameters.getRegHealthHM2CaseFemales().getScore(this, Variables.class);
                healthPsyDstrss0to12 = constrainhealthPsyDstrssEstimate(dhmGhqPrediction+ healthPsyDstrss0to12);
            } else System.out.println("healthMentalHM2 method in Person class: Person has no gender!");
        } else if (healthPsyDstrss0to12 != null) {
            healthPsyDstrss0to12 = constrainhealthPsyDstrssEstimate(healthPsyDstrss0to12);
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

        if (demAge >= MIN_AGE_TO_HAVE_INCOME) {
            double mcsPrediction = Parameters.getRegHealthMCS1().getScore(this, Variables.class);
            double rmse = Parameters.getRMSEForRegression("DHE_MCS1");
            double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(statInnovations.getDoubleDraw(33));
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
        if (demAge >= 25 && demAge < MIN_AGE_SOCIAL_CARE) {
            if (Gender.Male.equals(getDemMaleFlag())) {
                mcsPrediction = Parameters.getRegHealthMCS2Males().getScore(this, Variables.class);
                healthMentalMcs = constrainSF12Estimate(mcsPrediction + healthMentalMcs);
            } else if (Gender.Female.equals(getDemMaleFlag())) {
                mcsPrediction = Parameters.getRegHealthMCS2Females().getScore(this, Variables.class);
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

        if (demAge >= MIN_AGE_TO_HAVE_INCOME) {
            double pcsPrediction = Parameters.getRegHealthPCS1().getScore(this, Variables.class);
            double rmse = Parameters.getRMSEForRegression("DHE_PCS1");
            double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(statInnovations.getDoubleDraw(34));
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
        if (demAge >= 25 && demAge < MIN_AGE_SOCIAL_CARE) {
            if (Gender.Male.equals(getDemMaleFlag())) {
                pcsPrediction = Parameters.getRegHealthPCS2Males().getScore(this, Variables.class);
                healthPhysicalPcs = constrainSF12Estimate(pcsPrediction + healthPhysicalPcs);
            } else if (Gender.Female.equals(getDemMaleFlag())) {
                pcsPrediction = Parameters.getRegHealthPCS2Females().getScore(this, Variables.class);
                healthPhysicalPcs = constrainSF12Estimate(pcsPrediction + healthPhysicalPcs);
            }
        } else if (healthPhysicalPcs != null) {
            healthPhysicalPcs = constrainSF12Estimate(healthPhysicalPcs);
        }
    }


    /**
     * Health and wellbeing - Life satisfaction score 0-10 step 1
     *
     * <p>Calculates the 'baseline' yearly update to the Life Satisfaction score ({@code demLifeSatScore0to10}) based on demographic variables.
     * Runs <b>before</b> {@link #lifeSatisfaction2()}.</p>
     *
     * @filter Age 16+
     * @updates {@code Person.demLifeSatScore0to10}
     * @see <a href="https://www.understandingsociety.ac.uk/documentation/mainstage/variables/sclfsato/">sclfsato</a>
     */
    protected void lifeSatisfaction1() {

        if (demAge >= MIN_AGE_TO_HAVE_INCOME) {
            double dlsPrediction = Parameters.getRegLifeSatisfaction1().getScore(this, Variables.class);
            double rmse = Parameters.getRMSEForRegression("DLS1");
            double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(statInnovations.getDoubleDraw(35));
            demLifeSatScore0to10 = dlsPrediction + rmse*gauss;
        }

    }

    /**
     * Health and wellbeing - Life satisfaction score 0-10 step 2
     *
     * <p>Updates Life Satisfaction score ({@code demLifeSatScore0to10}) from the causal effects of economic transitions.
     * Applies separate estimates for Male and Female Persons.
     * Runs <b>after</b> {@link #lifeSatisfaction1()}</p>
     *
     * @filter Age 25-64
     * @updates {@code Person.demLifeSatScore0to10}
     * @see <a href="https://www.understandingsociety.ac.uk/documentation/mainstage/variables/sclfsato/">sclfsato</a>
     */
    protected void lifeSatisfaction2() {

        if (demAge >= 25 && demAge < MIN_AGE_SOCIAL_CARE) {

            double dlsPrediction;
            if (Gender.Male.equals(getDemMaleFlag())) {
                dlsPrediction = Parameters.getRegLifeSatisfaction2Males().getScore(this, Variables.class);
                demLifeSatScore0to10 = constrainLifeSatisfactionEstimate(dlsPrediction + demLifeSatScore0to10);
            } else if (Gender.Female.equals(getDemMaleFlag())) {
                dlsPrediction = Parameters.getRegLifeSatisfaction2Females().getScore(this, Variables.class);
                demLifeSatScore0to10 = constrainLifeSatisfactionEstimate(dlsPrediction + demLifeSatScore0to10);
            }
        } else if (demLifeSatScore0to10 != null) {
            demLifeSatScore0to10 = constrainLifeSatisfactionEstimate(demLifeSatScore0to10);
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
        if (demAge >= MIN_AGE_TO_HAVE_INCOME) {

            eq5dPrediction = Parameters.getRegEQ5D().getScore(this, Variables.class);
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
     * @param healthPsyDstrss0to12 predicted GHQ12 Caseness score
     * @return {@code Person.healthPsyDstrss0to12} Caseness score constrained to 0-12
     */
    protected Double constrainhealthPsyDstrssEstimate(Double healthPsyDstrss0to12) {
        if (healthPsyDstrss0to12 < 0.) {
            healthPsyDstrss0to12 = 0.;
        } else if (healthPsyDstrss0to12 > 12.) {
            healthPsyDstrss0to12 = 12.;
        }
        return healthPsyDstrss0to12;
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
     * @return {@code Person.demLifeSatScore0to10} score constrained to 0-10
     */
    protected Double constrainLifeSatisfactionEstimate(double dls_estimate) {
        if (!Parameters.isFinite(dls_estimate)) {
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
        if (demAge >= MIN_AGE_TO_HAVE_INCOME) {
            Map<Dhe,Double> probs = ManagerRegressions.getProbabilities(this, RegressionName.HealthH1);
            var event = new MultiValEvent<Dhe>(probs, healthInnov1);
            healthSelfRated = event.eval();
            if (event.isProblemWithProbs())
                model.addCounterErrorH1a();

            if (Indicator.False.equals(getEduSpellFlag())) {
                boolean becomeLTSickDisabled = false;
                if (!Parameters.enableIntertemporalOptimisations || DecisionParams.flagDisability) {
                    double prob = Parameters.getRegHealthH2().getProbability(this, Variables.class);
                    becomeLTSickDisabled = (healthInnov2 < prob);
                }
                if (becomeLTSickDisabled) {
                    healthDsblLongtermFlag = Indicator.True;
                } else {
                    healthDsblLongtermFlag = Indicator.False;
                }
            }
        }
    }

    protected void evaluateSocialCareReceipt() {
        int year = getYear();

        if (demAge < Parameters.MIN_AGE_SOCIAL_CARE || getYear()>getStartYear()) {

            careNeedFlag = Indicator.False;
            careHrsFormalWeek = 0.0;
            careFormalX = 0.0;
            careHrsInformalWeek = 0.0;
            careReceivedFlag = SocialCareReceipt.None;
            careFormalFlag = false;
            careFromInformalFlag = false;
        }
        if (!Parameters.isFinite(careHrsInformalWeek))
            careHrsInformalWeek = 0.0;

        if (demAge >= Parameters.MIN_AGE_SOCIAL_CARE && year > getStartYear()) {
            // need care only projected for 65 and over due to limitations of data used for parameterisation

            double recCareInnov = statInnovations.getDoubleDraw(7);
            double probNeedCare = Parameters.getRegNeedCareS2a().getProbability(this, Variables.class);
            if (recCareInnov < probNeedCare) {
                careNeedFlag = Indicator.True;
            } else {
                careNeedFlag = Indicator.False;
            }

            double probRecCare = Parameters.getRegReceiveCareS2b().getProbability(this, Variables.class);
            if (recCareInnov < probRecCare) {
            // receive care
            // if probNeedCare > probRecCare, then code here implies that anyone who receives care will also need care

                Map<SocialCareReceiptS2c,Double> probs1 = Parameters.getRegSocialCareMarketS2c().getProbabilities(this, Person.Variables.class);
                var event = new MultiValEvent<SocialCareReceiptS2c>(probs1, statInnovations.getDoubleDraw(8));
                var socialCareReceiptS2c = event.eval();
                careReceivedFlag = SocialCareReceipt.getCode(socialCareReceiptS2c);
                if (SocialCareReceipt.Mixed.equals(careReceivedFlag) || SocialCareReceipt.Formal.equals(careReceivedFlag))
                    careFormalFlag = true;

                if (SocialCareReceipt.Mixed.equals(careReceivedFlag) || SocialCareReceipt.Informal.equals(careReceivedFlag)) {
                    // informal care received

                    // S2d: hours of informal care received (conditional on receiving informal care)
                    double score = Parameters.getRegInformalCareHoursS2d().getScore(this, Variables.class);
                    double rmse = Parameters.getRMSEForRegression("S2d");
                    double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(statInnovations.getDoubleDraw(12));
                    double informalHours = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.sinh(score + rmse * gauss));
                    careFromInformalFlag = true;
                    careHrsInformalWeek = informalHours;

                /*
                 * Retired processes (kept for future reuse):
                 * - S2f: not-partner informal carer composition
                 * - S2g-S2j: informal care hours by source
                 *
                 * if (getPartner()!=null) {
                 *     double probPartnerCare = Parameters.getRegReceiveCarePartnerS2d().getProbability(this, Person.DoublesVariables.class);
                 *     if (statInnovations.getDoubleDraw(9) < probPartnerCare) {
                 *         careFromInformalFlag = true;
                 *         Map<PartnerSupplementaryCarer,Double> probs2 =
                 *                 Parameters.getRegPartnerSupplementaryCareS2e().getProbabilities(this, Person.DoublesVariables.class);
                 *         event = new MultiValEvent(probs2, statInnovations.getDoubleDraw(10));
                 *         PartnerSupplementaryCarer cc = (PartnerSupplementaryCarer) event.eval();
                 *         if (PartnerSupplementaryCarer.Daughter.equals(cc))
                 *             careFromInformalFlag = true;
                 *         if (PartnerSupplementaryCarer.Son.equals(cc))
                 *             careFromInformalFlag = true;
                 *         if (PartnerSupplementaryCarer.Other.equals(cc))
                 *             careFromInformalFlag = true;
                 *     }
                 * }
                 * if (!careFromInformalFlag) {
                 *     Map<NotPartnerInformalCarer,Double> probs2 =
                 *             Parameters.getRegNotPartnerInformalCareS2f().getProbabilities(this, Person.DoublesVariables.class);
                 *     event = new MultiValEvent(probs2, statInnovations.getDoubleDraw(11));
                 *     NotPartnerInformalCarer cc = (NotPartnerInformalCarer) event.eval();
                 *     if (NotPartnerInformalCarer.DaughterOnly.equals(cc) || NotPartnerInformalCarer.DaughterAndSon.equals(cc) || NotPartnerInformalCarer.DaughterAndOther.equals(cc))
                 *         careFromInformalFlag = true;
                 *     if (NotPartnerInformalCarer.SonOnly.equals(cc) || NotPartnerInformalCarer.DaughterAndSon.equals(cc) || NotPartnerInformalCarer.SonAndOther.equals(cc))
                 *         careFromInformalFlag = true;
                 *     if (NotPartnerInformalCarer.OtherOnly.equals(cc) || NotPartnerInformalCarer.SonAndOther.equals(cc) || NotPartnerInformalCarer.DaughterAndOther.equals(cc))
                 *         careFromInformalFlag = true;
                 * }
                 * double careHoursInnov = statInnovations.getDoubleDraw(12);
                 * if (careFromInformalFlag) {
                 *     double scorePartner = Parameters.getRegPartnerCareHoursS2g().getScore(this,Person.DoublesVariables.class);
                 *     double rmsePartner = Parameters.getRMSEForRegression("S2g");
                 *     double gaussPartner = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                 *     careHrsInformalWeek = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.exp(scorePartner + rmsePartner * gaussPartner));
                 * }
                 * careHoursInnov = Parameters.updateProbability(careHoursInnov);
                 * if (careFromInformalFlag) {
                 *     double scoreDaughter = Parameters.getRegDaughterCareHoursS2h().getScore(this,Person.DoublesVariables.class);
                 *     double rmseDaughter = Parameters.getRMSEForRegression("S2h");
                 *     double gaussDaughter = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                 *     careHrsInformalWeek = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.exp(scoreDaughter + rmseDaughter * gaussDaughter));
                 * }
                 * careHoursInnov = Parameters.updateProbability(careHoursInnov);
                 * if (careFromInformalFlag) {
                 *     double scoreSon = Parameters.getRegSonCareHoursS2i().getScore(this,Person.DoublesVariables.class);
                 *     double rmseSon = Parameters.getRMSEForRegression("S2i");
                 *     double gaussSon = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                 *     careHrsInformalWeek = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.exp(scoreSon + rmseSon * gaussSon));
                 * }
                 * careHoursInnov = Parameters.updateProbability(careHoursInnov);
                 * if (careFromInformalFlag) {
                 *     double scoreOther = Parameters.getRegOtherCareHoursS2j().getScore(this,Person.DoublesVariables.class);
                 *     double rmseOther = Parameters.getRMSEForRegression("S2j");
                 *     double gaussOther = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                 *     careHrsInformalWeek = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.exp(scoreOther + rmseOther * gaussOther));
                 * }
                 */
                }
                if (careFormalFlag) {

                    // S2e: hours of formal care received (conditional on receiving formal care)
                    double score = Parameters.getRegFormalCareHoursS2e().getScore(this, Variables.class);
                    double rmse = Parameters.getRMSEForRegression("S2e");
                    double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(statInnovations.getDoubleDraw(13));
                    careHrsFormalWeek = Math.min(Parameters.MAX_HOURS_WEEKLY_FORMAL_CARE, Math.sinh(score + rmse * gauss));
                    careFormalX = careHrsFormalWeek * Parameters.getTimeSeriesValue(model.getYear(), TimeSeriesVariable.CarerWageRate);

                    // Retired process (kept for future reuse): S2k formal care hours.
                    // double score = Parameters.getRegFormalCareHoursS2k().getScore(this,Person.DoublesVariables.class);
                    // double rmse = Parameters.getRMSEForRegression("S2k");
                    // double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                }
            }
        }
        if (Parameters.flagSuppressSocialCareCosts)
            careFormalX = 0.0;
    }

    protected void evaluateSocialCareProvision() {
        evaluateSocialCareProvision(Parameters.getTimeSeriesValue(model.getYear(),TimeSeriesVariable.CareProvisionAdjustment));
    }

    public void evaluateSocialCareProvision(double probitAdjustment) {

        careProvidedFlag = Indicator.False;
        careHrsProvidedWeek = 0.0;
        if (demAge >= Parameters.MIN_AGE_TO_PROVIDE_CARE) {

            Person partner = getPartner();
            boolean careToPartner = false;
            double careHoursToPartner = 0.0;
            if (partner != null && partner.careFromInformalFlag) {
                careToPartner = true;
                careHoursToPartner = partner.getCareHoursFromPartnerWeekly();
            }
            double probProvideAny;
            if (partner == null) {

                // S3a: probability of providing care, singles.
                double score = Parameters.getRegCarePartnerProvCareToOtherS3a().getScore(this, Variables.class);
                probProvideAny = Parameters.getRegCarePartnerProvCareToOtherS3a().getProbability(score + probitAdjustment);
            } else {

                // S3b: probability of providing care, partnered.
                double score = Parameters.getRegNoCarePartnerProvCareToOtherS3b().getScore(this, Variables.class);
                probProvideAny = Parameters.getRegNoCarePartnerProvCareToOtherS3b().getProbability(score + probitAdjustment);
            }
            boolean provideCare = (statInnovations.getDoubleDraw(37) < probProvideAny);
            if (!Parameters.flagSuppressSocialCareCosts && provideCare) {

                // S3c/S3d: ordered-logit category of informal care hours, conditional on providing care.
                RegressionName regression = (partner == null) ? RegressionName.SocialCareS3c : RegressionName.SocialCareS3d;
                CareHoursProvidedCategory category = ManagerRegressions.getEvent(this, regression, statInnovations.getDoubleDraw(14));
                careHrsProvidedWeek = category.getRepresentativeHours();
                careProvidedFlag = Indicator.True;
            }
        }
    }

    public void cohabitation() {
        cohabitation(model.getPartnershipAdjustment());
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

            double prob;
            if (partner == null) {
                // partnership formation

                double score = Parameters.getRegPartnershipU1().getScore(this, Variables.class);
                prob = Parameters.getRegPartnershipU1().getProbability(score + probitAdjustment);
                demBePartnerFlag = (cohabitInnov < prob);
                if (demBePartnerFlag)
                    model.getPersonsToMatch().get(demMaleFlag).get(getRegion()).add(this);
            } else if (demMaleFlag == Gender.Female) {
                // partnership dissolution

                double score = Parameters.getRegPartnershipU2().getScore(this, Variables.class);
                prob = Parameters.getRegPartnershipU2().getProbability(score - probitAdjustment);
                if (cohabitInnov < prob) {
                    demLeavePartnerFlag = true;
                }
            }
        }
    }

    protected void partnershipDissolution() {

        if (demLeavePartnerFlag) {

            // update partner's variables first
            Person partner = getPartner();
            partner.setDemPartnerNYear(null);
            partner.setDemLeftPartnerFlag(true);    //Set to true if leaves partnership to use with fertility regression, this is never reset

            setDemPartnerNYear(null); 		        //Set number of years in partnership to null if leaving partner
            setDemLeftPartnerFlag(true);            //Set to true if leaves partnership to use with fertility regression, this is never reset
            idHh = null;

            setupNewBenefitUnit(true);

            if (Parameters.projectNonPensionWealth) {
                // allocate wealth between benefit units

                this.getBenefitUnit().setNonPensionWealth(this);
                partner.getBenefitUnit().setNonPensionWealth(partner);
            }
        }
    }


    public boolean inSchool() {
        return inSchool(model.getInSchoolAdjustment());
    }


    public boolean inSchool(double probitAdjustment) {
        // Documentation: diagram "SimPathsEU education module - MR2"

        eduLeaveSchoolFlag = false;
        Boolean inSchool;
        if (demAge < MIN_AGE_TO_LEAVE_EDUCATION) {

            inSchool = true;
        } else {

            // Innovation for education decisions
            double labourInnov = statInnovations.getDoubleDraw(24);

            // Initial case (laggedStudent): In the previous period, was the individual a student?
            // Yes
            if (Les_c4.Student.equals(labC4L1)) {

                // Is the age of the individual above the minimum age to leave education (age >= minQuittingAge)?
                // Yes
                if (demAge >= MIN_AGE_TO_LEAVE_EDUCATION) {
                    // Is the age of the individual below the max age to leave education (age < maxQuittingAge)?
                    // Yes
                    if (demAge <= MAX_AGE_TO_STAY_IN_CONTINUOUS_EDUCATION) {
                        // --> process E1a
                        double score = Parameters.getRegEducationE1a().getScore(this, Variables.class);
                        double prob = Parameters.getRegEducationE1a().getProbability(score + probitAdjustment);

                        if (labourInnov < prob) {
                            // Remain a student *OUTCOME B*
                            inSchool = true; // Must return true as they remain in school
                        } else {
                            // Leave education --> Process E2
                            eduLeaveSchoolFlag = true; // Must set flag to true
                            inSchool = false; // Must return false as they are leaving
                        }
                    }
                    // No (demAge > MAX_AGE_TO_STAY_IN_CONTINUOUS_EDUCATION)
                    else {
                        // Leave education --> Process E2
                        eduLeaveSchoolFlag = true; // Must set flag to true
                        inSchool = false; // Must return false as they are leaving
                    }
                }
                // No (demAge < MIN_AGE_TO_LEAVE_EDUCATION)
                else {
                    inSchool = true; // The individual remains a student *OUTCOME A*
                }
            }

            // No (Not a Student in labC4L1)
            else {
                // In the previous period, was the individual a retired (laggedRetired)?
                // Yes
                if (Les_c4.Retired.equals(labC4L1)) {
                    inSchool = false;  // The individual can't be a student *OUTCOME C*
                    // Remain in current status which is retired
                }

                // No --> Process E1b
                else {

                    double score = Parameters.getRegEducationE1b().getScore(this, Variables.class);
                    double prob = Parameters.getRegEducationE1b().getProbability(score + probitAdjustment);
                    if (labourInnov < prob) {
                        // Become a student *OUTCOME E*
                        setEduReturnFlag(Indicator.True);
                        setEduSpellFlag(Indicator.False); //not needed, more of a precaution as ded should already be false
                        inSchool = true; // Must return true as they become a student
                    } else {

                        inSchool = false;
                    }
                }
            }
        }
        if (inSchool)
            setLabC4(Les_c4.Student);
        else if (demAge < AGE_TO_BECOME_RESPONSIBLE)
            setLabC4(Les_c4.NotEmployed);
        return inSchool;
    }


//        //Min age to leave education set to 16 (from 18 previously) but note that age to leave home is 18.
//        if (Les_c4.Retired.equals(labC4) || demAge < Parameters.MIN_AGE_TO_LEAVE_EDUCATION || demAge > Parameters.MAX_AGE_TO_ENTER_EDUCATION) {		//Only apply module for persons who are old enough to consider leaving education, but not retired
//            return;
//        } else if (Les_c4.Student.equals(labC4) && !eduLeftEduFlag && demAge >= Parameters.MIN_AGE_TO_LEAVE_EDUCATION) { //leftEducation is initialised to false and updated to true when individual leaves education (and never reset).
//            //If age is between 16 - 29 and individual has always been in education, follow process E1a:
//
//            if (demAge <= Parameters.MAX_AGE_TO_STAY_IN_CONTINUOUS_EDUCATION) {
//
//                double prob = Parameters.getRegEducationE1a().getProbability(this, Person.DoublesVariables.class);
//                eduLeaveSchoolFlag = (labourInnov >= prob); //If event is true, stay in school.  If event is false, leave school.
//            } else {
//                eduLeaveSchoolFlag = true; //Hasn't left education until 30 - force out
//            }
//        } else if (demAge <= 45 && (!Les_c4.Student.equals(labC4) || eduLeftEduFlag)) { //leftEducation is initialised to false and updated to true when individual leaves education for the first time (and never reset).
//            //If age is between 16 - 45 and individual has not continuously been in education, follow process E1b:
//            //Either individual is currently a student and has left education at some point in the past (so returned) or individual is not a student so has not been in continuous education:
//            //TODO: If regression outcome of process E1b is true, set activity status to student and der (return to education indicator) to true?
//
//            double prob = Parameters.getRegEducationE1b().getProbability(this, Person.DoublesVariables.class);
//            if (labourInnov < prob) {
//                //If event is true, re-enter education.  If event is false, leave school
//
//                setLabC4(Les_c4.Student);
//                setEduReturnFlag(Indicator.True);
//                setEduSpellFlag(Indicator.True);
//            } else if (Les_c4.Student.equals(labC4)){
//                //If activity status is student but regression to be in education was evaluated to false, remove student status
//
//                setLabC4(Les_c4.NotEmployed);
//                setEduSpellFlag(Indicator.False);
//                eduLeaveSchoolFlag = true; //Test what happens if people who returned to education leave again
//            }
//        } else if (demAge > 45 && labC4.equals(Les_c4.Student)) {
//            //People above 45 shouldn't be in education, so if someone re-entered at 45 in previous step, force out
//
//            setLabC4(Les_c4.NotEmployed);
//            setEduSpellFlag(Indicator.False);
//        }
//    }

    public void leavingSchool() {

        if (eduLeaveSchoolFlag) {

            setEducationLevel(); //If individual leaves school follow process E2a to assign level of education
            setEduExitSampleFlag(Indicator.True); //Set variable left education (sedex) if leaving school
            setEduSpellFlag(Indicator.False); //Set variable in education (ded) to false if leaving school
            setEduReturnFlag(Indicator.False);
            setEduLeftEduFlag(true); //This is not reset and indicates if individual has ever left school - used with health process
            setLabC4(Les_c4.NotEmployed); //Set activity status to NotEmployed when leaving school to remove Student status

            this.eduLeaveSchoolFlag = false; // Reset the flag once the leaving process is complete
        }
    }


    private void giveBirth() {				//To be called once per year after fertility alignment

        if (demGiveBirthFlag) {		//toGiveBirth is determined by fertility process

            Gender babyGender = (statInnovations.getDoubleDraw(27) < PROB_NEWBORN_IS_MALE) ? Gender.Male : Gender.Female;

            //Give birth to new person and add them to benefitUnit.
            Person child = new Person(babyGender, this);
            model.getPersons().add(child);
            benefitUnit.getMembers().add(child);
        }
    }


    protected void initialisePotentialHourlyEarnings() {

        if (demAge < Parameters.MIN_AGE_TO_HAVE_INCOME || demAge > Parameters.MAX_AGE_FLEXIBLE_LABOUR_SUPPLY) {
            return;
        }
        double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(statInnovations.getDoubleDraw(15));
        double logPotentialHourlyEarnings, score, rmse;
        if (demMaleFlag.equals(Gender.Male)) {
            score = Parameters.getRegWagesMales().getScore(this, Variables.class);
            rmse = Parameters.getRMSEForRegression("Wages_Males");
        } else {
            score = Parameters.getRegWagesFemales().getScore(this, Variables.class);
            rmse = Parameters.getRMSEForRegression("Wages_Females");
        }
        logPotentialHourlyEarnings = score + rmse * gauss;
        double upratedLevelPotentialHourlyEarnings = Math.exp(logPotentialHourlyEarnings);
        setLabWageFullTimeHrly(upratedLevelPotentialHourlyEarnings);
        setLabWageFullTimeHrlyL1(upratedLevelPotentialHourlyEarnings);
    }


    protected void updateFullTimeHourlyEarnings() {

        if (demAge < Parameters.MIN_AGE_TO_HAVE_INCOME || demAge > Parameters.MAX_AGE_FLEXIBLE_LABOUR_SUPPLY) {
            return;
        }
        double rmse, wagesInnov = statInnovations.getDoubleDraw(16);
        if (Les_c4.EmployedOrSelfEmployed.equals(labC4L1)) {
            if (labWageRegressRandomCompoponentEmp == null || !model.fixRegressionStochasticComponent) {
                if (Gender.Male.equals(demMaleFlag)) {
                    rmse = Parameters.getRMSEForRegression("W1mb");
                } else {
                    rmse = Parameters.getRMSEForRegression("W1fb");
                }
                double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(wagesInnov);
                labWageRegressRandomCompoponentEmp = rmse * gauss;
            }
        } else {
            if (labWageRegressRandomCompoponentNotEmp == null || !model.fixRegressionStochasticComponent) {
                if (Gender.Male.equals(demMaleFlag)) {
                    rmse = Parameters.getRMSEForRegression("W1ma");
                } else {
                    rmse = Parameters.getRMSEForRegression("W1fa");
                }
                double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(wagesInnov);
                labWageRegressRandomCompoponentNotEmp = rmse * gauss;
            }
        }

        double logFullTimeHourlyEarnings;
        if(Gender.Male.equals(demMaleFlag)) {
            if (Les_c4.EmployedOrSelfEmployed.equals(labC4L1)) {
                logFullTimeHourlyEarnings = Parameters.getRegWagesMalesE().getScore(this, Variables.class) + labWageRegressRandomCompoponentEmp;
            } else {
                logFullTimeHourlyEarnings = Parameters.getRegWagesMalesNE().getScore(this, Variables.class) + labWageRegressRandomCompoponentNotEmp;
            }
        } else {
            if (Les_c4.EmployedOrSelfEmployed.equals(labC4L1)) {
                logFullTimeHourlyEarnings = Parameters.getRegWagesFemalesE().getScore(this, Variables.class) + labWageRegressRandomCompoponentEmp;
            } else {
                logFullTimeHourlyEarnings = Parameters.getRegWagesFemalesNE().getScore(this, Variables.class) + labWageRegressRandomCompoponentNotEmp;
            }
        }
        if (!Parameters.isFinite(logFullTimeHourlyEarnings))
            throw new ArithmeticException("Failure to project full-time earnings estimate");

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
    public void setYPensPersGrossMonth(double val) {
        yPensPersGrossMonth = val;
    }
    public double getPensionIncomeAnnual() {
        return Math.sinh(yPensPersGrossMonth) * 12.0;
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


    /***************************************************************
     * method to project private pension income
     ***************************************************************/
    public double updatePrivatePensionIncome() {

        // Initialize to 0 here.
        // This prevents the bug where you overwrite the calculation later.
        yPensPersGrossMonth = 0.0;

        // ypnoab: inverse hyperbolic sine of pension income per month
        if (demAge >= Parameters.MIN_AGE_TO_HAVE_INCOME) {

            // Retirement decision is modelled in the retirement process. Here only the amount of pension income for retired individuals is modeled.
            /*
                Private pension income when individual was retired in the previous period is modelled using process I2b.
                Private pension income when individual moves from non-retirement to retirement is modelled using:
                i) process I3a, to determine who receives private pension income
                ii) process I3b, to determine the amount
            */

            // GLOBAL CONDITION: Process definitions require age 50+
            if (demAge >= Parameters.MIN_AGE_TO_RETIRE) {

                // --- BRANCH 1: CONTINUING PENSIONERS ---
                if (Les_c4.Retired.equals(labC4L1)) {

                    updateContinuingRetiree();
                }

                // --- BRANCH 2: NEW PENSIONERS ---
                else if (Les_c4.Retired.equals(labC4)) {

                    updateNewRetiree();
                }
            }
        }
        return yPensPersGrossMonth;
    }


    private void updateContinuingRetiree() {

        double pensionIncMonth = 0.0;
        if (Parameters.projectPensionWealth) {

            if (privatePension == null)
                privatePension = new PrivatePension();
            else
                throw new RuntimeException("privatePension should be null");

            pensionIncMonth = privatePension.projectInPaymentPension(model.getYear(), privatePensionL1.getPensionIncomeAnnual());
        } else {

            double score = Parameters.getRegIncomeI2b().getScore(this, Variables.class);
            double rmse = Parameters.getRMSEForRegression("I2b");

            // "ihs" in instructions -> Use Asinh
            pensionIncMonth = setIncomeBySource(score, rmse, IncomeSource.PrivatePension, RegressionScoreType.Asinh);
        }
        yPensPersGrossMonth = FastMath.asinh(pensionIncMonth);
    }


    private void updateNewRetiree() {

        double pensionIncMonth = 0.0;
        if (Parameters.projectPensionWealth) {

            if (privatePension == null)
                privatePension = new PrivatePension();
            else
                throw new RuntimeException("privatePension should be null");

            pensionIncMonth = privatePension.projectPensionAccess(demAge, demMaleFlag, benefitUnit.getYear(), privatePensionL1.getWealth());
        } else {

            // 1. SELECTION (Process I3a - Logit)
            double probPens = Parameters.getRegIncomeI3a().getProbability(this, Variables.class);

            // Using the same random draw index (19) as your previous pension logic
            boolean hasPrivatePensionIncome = (statInnovations.getDoubleDraw(19) < probPens);
            if (hasPrivatePensionIncome) {

                // 2. AMOUNT (Process I3b - OLS)
                double score = Parameters.getRegIncomeI3b().getScore(this, Variables.class);
                double rmse = Parameters.getRMSEForRegression("I3b");

                // "ihs" in instructions -> Use Asinh
                // (Note: Your old code used 'Level' here, but I3b is explicitly 'ihs')
                pensionIncMonth = setIncomeBySource(score, rmse, IncomeSource.PrivatePension, RegressionScoreType.Asinh);
            }
        }
        yPensPersGrossMonth = FastMath.asinh(pensionIncMonth);
    }


    public double getPensionLumpSum() {

        return (privatePension!=null) ? privatePension.getLumpSumPayment() : 0.0;
    }


    /***************************************************************
     * method to project investment income where
     * non-pension wealth is not explicitly simulated
     ***************************************************************/
    protected void updateInvestmentIncome() {

        if (Parameters.projectNonPensionWealth)
            throw new RuntimeException("request to update investment income in person object when wealth is explicit");

        // Initialize to 0 here.
        // This prevents the bug where you overwrite the calculation later.
        yCapitalPersMonth = 0.0;

        // ypncp: inverse hyperbolic sine of capital income per month
        // ypnoab: inverse hyperbolic sine of pension income per month
        // yptciihs_dv: inverse hyperbolic sine of capital and pension income per month
        if (demAge >= Parameters.MIN_AGE_TO_HAVE_INCOME) {

            double capitalInnov = statInnovations.getDoubleDraw(18);
            // 1. SELECTION STEP (Process 1a - Binomial)
            // Use I1a purely to determine the Probability of having income
            double probCap = Parameters.getRegIncomeI1a().getProbability(this, Variables.class);

            boolean hasCapitalIncome = (capitalInnov < probCap);

            if (hasCapitalIncome) {
                // 2. AMOUNT STEP (Process 1b - Linear)
                // Use I1b to calculate the Score (magnitude)
                double score = Parameters.getRegIncomeI1b().getScore(this, Variables.class);

                // Ensure you fetch the RMSE for the linear process (I1b)
                double rmse = Parameters.getRMSEForRegression("I1b");

                // Calculate level and assign
                double capinclevel = setIncomeBySource(score, rmse, IncomeSource.CapitalIncome, RegressionScoreType.Asinh);
                yCapitalPersMonth = FastMath.asinh(capinclevel);
            }
        }
    }


    /***************************************************************
     * method to summarise non-labour income
     ***************************************************************/
    protected void setNonLabourIncome() {

        //Multiplied by the capital income multiplier, defined as chosen savings rate divided by the long-term average (specified in Parameters class)
        double yptciihs_dv_tmp_level = Math.sinh(yCapitalPersMonth) + Math.sinh(yPensPersGrossMonth);
        yMiscPersGrossMonth = FastMath.asinh(yptciihs_dv_tmp_level); //Non-employment non-benefit income is the sum of capital income and, for retired individuals, pension income.
        if (yMiscPersGrossMonth > 13.0) {
            yMiscPersGrossMonth = 13.5;
        }
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
//        if (Indicator.True.equals(healthDsblLongtermFlag) && !Parameters.flagSuppressSocialCareCosts)
//            return false;
//        if (Indicator.True.equals(careNeedFlag) && !Parameters.flagSuppressSocialCareCosts)
//            return false;

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
    public void setEducationLevel() {

        // Process E2

        // --- Step 1: Regression Result ---
        // The regression determines the highest possible qualification achieved this spell.
        Map<EducationLevel,Double> probs = Parameters.getRegEducationE2().getProbabilities(this, Variables.class);
        MultiValEvent event = new MultiValEvent(probs, statInnovations.getDoubleDraw(30));
        EducationLevel regressionEducationLevel = (EducationLevel) event.eval();
        Education newEducationLevel = Education.valueOf(regressionEducationLevel.name());

        //Education has been set to Low by default for all new born babies, so it should never be null.
        //This is because we no longer prevent people in school to get married, given that people can re-enter education throughout their lives.
        //Note that by not filtering out students, we must assign a low education level by default to persons at birth to prevent a null pointer exception when new born persons become old enough to marry if they have not yet left school because
        //their education level has not yet been assigned.

        // --- Step 2: Update counters based on regression result ---
        if (newEducationLevel.equals(Education.Low)) {
            model.lowEd++;
        } else if (newEducationLevel.equals(Education.Medium)) {
            model.medEd++;
        } else if (newEducationLevel.equals(Education.High)) {
            model.highEd++;
        } else if (newEducationLevel.equals(Education.InEducation)) {
            model.inEd++;
        } else {
            model.nothing++;
        }

        // --- Step 3: process E2 in the diagaram ---
        if (eduHighestC4 != null) {
            if (newEducationLevel.getRank() > eduHighestC4.getRank()) { // highest qualification cannot decrease
                eduHighestC4 = newEducationLevel;
            }
        } /*else {
            // retain old level -> deh_c3 remains the same
        }*/
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

        xEquivYear = benefitUnit.getXDiscConsumptionAnnual() / benefitUnit.getEquivalisedWeight();
    }


    protected void updateAttributes() {

        UpdateManager.applyAnnotations(this);

        //Reset flags to default values

        setEduExitSampleFlag(Indicator.False); //This variable is False by default
                                    // is set to true only when person leaves school in this specific year
        // eduSpellFlag = (Les_c4.Student.equals(labC4)) ? Indicator.True : Indicator.False;
        // no need to update eduSpellFlag as its value is persisted from the previous year

        if (demAge < Parameters.AGE_TO_BECOME_RESPONSIBLE) {
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

        // generate year specific random draws
        if (Parameters.enableIntertemporalOptimisations && !DecisionParams.flagDisability) {
            healthDsblLongtermFlag = Indicator.False;
            healthDsblLongtermFlagL1 = Indicator.False;
        }
        if (!Parameters.flagSocialCare) {
            setAllSocialCareVariablesToFalse();
        }
        statInnovations.getNewDoubleDraws();
    }

    private void updateOutputVariables() {
        idPartner = getPartnerID();
        demPartnerStatus = getDemPartnerStatus();
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
            return this.isPsychologicallyDistressed();

        case isNeedSocialCare:
            return (Indicator.True.equals(careNeedFlag)) ? 1 : 0;

        default:
            throw new RuntimeException("Unsupported variable " + variableID.name() + " in Person#getIntValue");
        }
    }


    // ---------------------------------------------------------------------
    // implements IDoubleSource for use with Regression classes
    // ---------------------------------------------------------------------

    public enum Variables {
        // ORGANISED ALPHABETICALLY TO ASSIST IDENTIFICATION

        demAge_Sq,
        labWorkHist,
        labStatusC4Student_MaleL1,
        labStatusC4Retired_MaleL1,
        labStatusPartnerC3NotEmployedL1,
        labStatusC4NotEmployed_MaleL1,
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
        Age70plus,
        Age71to72,
        Age73to74,
        Age75to76,
        Age75to79,
        Age77to78,
        Age79to80,
        Age80to84,
        Age80plus,
        Age81to82,
        Age83to84,
        Age85plus,
        AgeOver39,
        AgeUnder25,
        Asinhop_empee,
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
        CareMarketFormalPartner,
        CareMarketInformal_L1,
        CareMarketInformalPartner,
        CareMarketFormal_L1,
        CareMarketMixed,
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
        Current_op_memb,
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
        Dcpst_Partnered_L1,             //Lag(1) of partnership status is partnered
        Dcpst_PreviouslyPartnered,		//Single (previously partnered category merged)
        Dcpst_PreviouslyPartnered_L1,   //Lag(1) of partnership status is single (merged)
        New_rel_L1,
        Dcpst_Single,					//Single
        Dcpst_Single_L1, 				//Lag(1) of partnership status is Single
        Dcrisis,
        Ded,
        Ded_L1,
        Ded_Dgn,
        Ded_Dag,
        Ded_Dag_sq,
        Ded_Dhe_pcs_L1,
        Ded_Dhe_mcs_L1,
        Ded_Ypncp_L1,
        Ded_Yplgrs_dv_L1,
        Ded_Yplgrs_dv_L2,
        Ded_Ypncp_L2,
        Ded_Ydses_c5_Q2_L1,
        Ded_Ydses_c5_Q3_L1,
        Ded_Ydses_c5_Q4_L1,
        Ded_Ydses_c5_Q5_L1,
        Ded_Dnc_L1_,
        Ded_Dnc02_L1,
        Ded_Dcpst_Single,
        demDChild,
        demDChildL1,
        demYear2010,
        demYear2011,
        demYear2012,
        demYear2013,
        demYear2014,
        demYear2015,
        demYear2016,
        demYear2017,
        demYear2018,
        demYear2019,
        demYear2022,
        demYear2023,
        demYear2024,
        demAge_10,
        demAge_11,
        demAge_12,
        demAge_13,
        demAge_14,
        Dnc_L1_,
        demCompHhC4SingleChL1,
        Deh_c3_High,
        Deh_c3_Low,
        Deh_c3_Low_Dag,
        Deh_c3_Low_L1,					//Education level lag(1) equals low
        Deh_c3_Medium,
        Deh_c3_Medium_Dag,
        Deh_c3_Medium_L1, 				//Education level lag(1) equals medium
        Deh_c4_Low,
        Deh_c4_Low_Dag,
        Deh_c4_Low_L1,
        Deh_c4_Medium,
        Deh_c4_Medium_Dag,
        Deh_c4_Medium_L1,
        Deh_c4_High,
        Deh_c4_High_L1,
        Deh_c4_Na,
        Deh_c4_Na_L1,
        Dehf_c3_High,					//Father's education == High indicator
        Dehf_c3_Low,					//Father's education == Low indicator
        Dehf_c3_Medium,					//Father's education == Medium indicator
        Dehm_c3_High,					//Mother's education == High indicator
        Dehm_c3_Low,					//Mother's education == Low indicator
        Dehm_c3_Medium,					//Mother's education == Medium indicator
        Dehmf_c3_High,
        Dehmf_c3_Medium,
        Dehmf_c3_Medium_L1,
        Dehmf_c3_Low,
        Dehmf_c3_Low_L1,
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
        Dhesp_Fair,
        Dhesp_Good,
        Dhesp_VeryGood,
        Dhesp_Excellent,
        Dhesp_Poor_L1,
        Dhesp_Fair_L1,
        Dhesp_Good_L1,
        Dhesp_VeryGood_L1,
        Dhesp_Excellent_L1,
        Dhh_owned_L1,
        Dhhtp_c4_CoupleChildren_L1,
        Dhhtp_c4_CoupleNoChildren_L1,
        Dhhtp_c4_SingleChildren_L1,
        Dhhtp_c4_SingleChildren_L1_,
        Dhhtp_c4_SingleNoChildren_L1,
        L_Dhhtp_c4_CoupleChildren,
        L_Dhhtp_c4_SingleChildren,
        L_Dhhtp_c4_SingleNoChildren,
        L_Dehmf_c3_Low_,
        L_Dehmf_c3_Low,
        L_Dehmf_c3_Medium_,
        L_Dehmf_c3_Medium,
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
        Dhe_Mcs_L1,
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
        Dhe_Pcs_L1,
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
        Earnings_Q2,
        demYearTransformed,
        Earnings_Q3,
        Earnings_Q4,
        Earnings_Q5,
        Employee_cont_0,
        Employee_cont_3,
        Employee_cont_5,
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
        Ethn_Black_,
        EthnicityBlack,
        Ethn_Other,
        Ethn_Other_,
        EthnicityOther,
        EthnicityMixed,
        // Ethn_Missing,
        Female,
        FertilityRate,
        FinancialDistress,
        L_FinancialDistress,
        GrossEarningsYearly,
        GrossLabourIncomeMonthly,
        InverseMillsRatio,
        InvMillsRatio,
        L1_hourly_wage,
        L1_log_hourly_wage,
        Hourly_wage_L1,
        L1_log_hourly_wage_sq,
        Ld_children_2under,
        Ld_children_3under,
        Ld_children_4_12,
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
        Les_c4_Student_Dgn_L1,
        Les_c4_NotEmployed_Dgn_L1,
        Les_c4_Retired_Dgn_L1,
        Les_c7_Covid_Furlough_L1,
        Lesdf_c4_BothNotEmployed_L1,
        Lesdf_c4_EmployedSpouseNotEmployed_L1, 					//Own and partner's activity status lag(1)
        Lesdf_c4_EmpSpouseNotEmp_L1,
        Lesdf_c4_NotEmployedSpouseEmployed_L1,
        Lesdf_c4_NotEmpSpouseEmp_L1,
        Lessp_c3_NotEmployed_L1,
        Lessp_c3_NotEmployed,
        Lessp_c3_Sick_L1,
        Lessp_c3_Student_L1,			//Partner variables
        Lesnr_c2_NotEmployed_L1,
        Reached_Retirement_Age_Lesnr_c2_NotEmployed_L1,
        Liwwh,
        LnAge,
        Lnonwork,
        Lnop_all,//Work history in months
        Lstudent,
        Lunion,
        HrsReceivedFormalIHS_L1,
        HrsReceivedInformalIHS_L1,
        HrsProvidedInformalIHS_L1,
        NeedCare,
        NeedCare_L1,
        NeedCarePartner,
        NonPovertyToPoverty,
        NotEmployed_L1,
        NumberChildren,
        NumberChildren_2under,
        OnleaveBenefits,
        OtherIncome,
        Parents,
        Partnered,
        PartTime_AND_Ld_children_3under,			//Interaction term conditional on if the person had a child under 3 at the previous time-step
        PartTimeRate,
        PersistentPoverty,
        PersistentUnemployed,
        PovertyToNonPoverty,
        Pt,
        Reached_Retirement_Age,						//Indicator whether individual is at or above retirement age
        Reached_Retirement_Age_Les, //Interaction term for being at or above retirement age and not employed in the previous year
        Reached_Retirement_Age_Sp,					//Indicator whether spouse is at or above retirement age
        RealGDPGrowth,
        RealIncomeChange, //Note: the above return a 0 or 1 value, but income variables will return the change in income or 0
        RealIncomeDecrease_D,
        RealWageGrowth,
        ReceiveCare,
        ReceiveCare_L1,
        ReceiveCarePartner,
        ResStanDev,
        Retired,
        Sfr, 										//Scenario : fertility rate This retrieves the fertility rate by region and year to use in fertility regression
        Single,
        Single_kids,
        StatePensionAge,
        ProvideCare_L1,
        UnemployedToEmployed,
        UnemploymentRate,
        Union,
        Union_kids,
        UKC,				//UK
        UKC_,
        UKD,
        UKD_,
        UKE,
        UKF,
        UKG,
        UKH,
        UKI,
        UKJ,
        UKK,
        UKL,
        UKL_,
        UKM,
        UKM_,
        UKmissing,
        UKN,
        Year,										//Year as in the simulation, e.g. 2009
        Year2010,
        Year2011,
        Year2012,
        Y2012,
        Year2013,
        Y2013,
        Y2013_,
        Year2014,
        Y2014,
        Y2014_,
        Year2015,
        Y2015,
        Year2016,
        Y2016,
        Year2017,
        Y2017,
        Year2018,
        Y2018,
        Year2019,
        Y2019,
        Year2020,
        Y2020,
        Year2021,
        Y2021,
        Year2022,
        Y2022,
        Year2023,
        Y2023,
        Year2024,
        Y2024,
        Y2012_Poor,
        Y2012_Fair,
        Y2012_Good,
        Y2012_VeryGood,
        Y2013_Poor,
        Y2013_Fair,
        Y2013_Good,
        Y2013_VeryGood,
        Y2014_Poor,
        Y2014_Fair,
        Y2014_Good,
        Y2014_VeryGood,
        Y2015_Poor,
        Y2015_Fair,
        Y2015_Good,
        Y2015_VeryGood,
        Y2016_Poor,
        Y2016_Fair,
        Y2016_Good,
        Y2016_VeryGood,
        Y2017_Poor,
        Y2017_Fair,
        Y2017_Good,
        Y2017_VeryGood,
        Y2018_Poor,
        Y2018_Fair,
        Y2018_Good,
        Y2018_VeryGood,
        Y2019_Poor,
        Y2019_Fair,
        Y2019_Good,
        Y2019_VeryGood,
        Y2020_Poor,
        Y2020_Fair,
        Y2020_Good,
        Y2020_VeryGood,
        Y2021_Poor,
        Y2021_Fair,
        Y2021_Good,
        Y2021_VeryGood,
        Y2022_Poor,
        Y2022_Fair,
        Y2022_Good,
        Y2022_VeryGood,
        Y2023_Poor,
        Y2023_Fair,
        Y2023_Good,
        Y2023_VeryGood,
        Y2024_Poor,
        Y2024_Fair,
        Y2024_Good,
        Y2024_VeryGood,
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
        HHincomeQ2,
        HHincomeQ3,
        HHincomeQ4,
        HHincomeQ5,
        Year_transformed,							//Year - 2000
        Year_transformed_monetary,					//Year-2000 that stops in 2017, for use with monetary processes
        Ynbcpdf_dv_L1, 								//Lag(1) of difference between own and partner's gross personal non-benefit income
        Yplgrs_dv_L1,								//Lag(1) of gross personal employment income
        Yplgrs_dv_L2,								//Lag(2) of gross personal employment income
        Yplgrs_dv_L3,								//Lag(3) of gross personal employment income
        Ypnbihs_dv_L1,								//Gross personal non-benefit income lag(1)
        Ypnbihs_dv_L1_sq,							//Square of gross personal non-benefit income lag(1)
        Ypncp_L1,									//Lag(1) of capital income
	    L_Ypncp,
        Ypncp_L2,									//Lag(2) of capital income
        Ypnoab_L1,									//Lag(1) of pension income
	    L_Ypnoab,
        Ypnoab_L2,									//Lag(2) of pension income
        Yptciihs_dv_L1,								//Lag(1) of gross personal non-employment non-benefit income
        Yptciihs_dv_L2,								//Lag(2) of gross personal non-employment non-benefit income
        Yptciihs_dv_L3,								//Lag(3) of gross personal non-employment non-benefit income
        // S2c multinomial labels with alternative suffixes
        CareMarketFormal_L1_Mixed,
        CareMarketFormal_L1_Formal,
        CareMarketInformal_L1_Mixed,
        CareMarketInformal_L1_Formal,
        CareMarketMixed_L1_Mixed,
        CareMarketMixed_L1_Formal,
        Dgn_Mixed,
        Dgn_Formal,
        Age67to68_Mixed,
        Age67to68_Formal,
        Age69to70_Mixed,
        Age69to70_Formal,
        Age71to72_Mixed,
        Age71to72_Formal,
        Age73to74_Mixed,
        Age73to74_Formal,
        Age75to76_Mixed,
        Age75to76_Formal,
        Age77to78_Mixed,
        Age77to78_Formal,
        Age79to80_Mixed,
        Age79to80_Formal,
        Age81to82_Mixed,
        Age81to82_Formal,
        Age83to84_Mixed,
        Age83to84_Formal,
        Age85plus_Mixed,
        Age85plus_Formal,
        Dhe_Fair_Mixed,
        Dhe_Fair_Formal,
        Dhe_Good_Mixed,
        Dhe_Good_Formal,
        Dhe_VeryGood_Mixed,
        Dhe_VeryGood_Formal,
        Dhe_Excellent_Mixed,
        Dhe_Excellent_Formal,
        Partnered_Mixed,
        Partnered_Formal,
        Deh_c3_Medium_Mixed,
        Deh_c3_Medium_Formal,
        Deh_c3_Low_Mixed,
        Deh_c3_Low_Formal,
        HHincomeQ2_Mixed,
        HHincomeQ2_Formal,
        HHincomeQ3_Mixed,
        HHincomeQ3_Formal,
        HHincomeQ4_Mixed,
        HHincomeQ4_Formal,
        HHincomeQ5_Mixed,
        HHincomeQ5_Formal,
        UKC_Mixed,
        UKC_Formal,
        UKD_Mixed,
        UKD_Formal,
        UKE_Mixed,
        UKE_Formal,
        UKF_Mixed,
        UKF_Formal,
        UKG_Mixed,
        UKG_Formal,
        UKH_Mixed,
        UKH_Formal,
        UKJ_Mixed,
        UKJ_Formal,
        UKK_Mixed,
        UKK_Formal,
        UKL_Mixed,
        UKL_Formal,
        UKM_Mixed,
        UKM_Formal,
        UKN_Mixed,
        UKN_Formal,
        Y2020_Mixed,
        Y2020_Formal,
        Y2021_Mixed,
        Y2021_Formal,
        Constant_Mixed,
        Constant_Formal,
        Age65to66,
        Age_10,
        Age_11,
        Age_12,
        Age_13,
        Age_14,
        Age_15,
        Age_16,
        Age_2,
        Age_3,
        Age_4,
        Age_5,
        Age_6,
        Age_7,
        Age_8,
        Age_9,
        CareMarket,
        CareMarketFormal,
        CareMarketInformal,
        CareMarketMixedPartner,
        Dag_c,
        Dag_c_sq,
        Dag_post18_sq,
        Dag_post21,
        Dag_post21_sq,
        Dag_post25,
        Dag_post25_sq,
        Dag_post26_sq,
        Dcpagdf,
        Dcpst_1,
        Dcpst_2,
        Dcpyy,
        Ded_Dcpst_Single_L1,
        Ded_Dehsp_c3_Low_L1,
        Ded_Dehsp_c3_Medium_L1,
        Ded_Dhe,
        Ded_Dhe_Excellent_L1,
        Ded_Dhe_Fair_L1,
        Ded_Dhe_Good_L1,
        Ded_Dhe_VeryGood_L1,
        Ded_Dhe_mcs,
        Ded_Dhe_pcs,
        Ded_Dhesp_Fair_L1,
        Ded_Dhesp_Good_L1,
        Ded_Ydses_c5_Q,
        Ded_Yplgrs_dv,
        Ded_Ypncp,
        Deh_c3_1,
        Deh_c3_2,
        Deh_c3_3,
        Deh_c4_1,
        Deh_c4_2,
        Deh_c4_3,
        Deh_c4_4,
        Dehmf_c3_1,
        Dehmf_c3_2,
        Dehmf_c3_3,
        Dehsp_c3_1,
        Dehsp_c3_2,
        Dehsp_c3_3,
        Dehsp_c3_High,
        Dehsp_c3_Low,
        Dehsp_c3_Medium,
        Dhe_1,
        Dhe_mcssp,
        Dhe_pcssp,
        Dhesp_1,
        Dhesp_2,
        Dhesp_3,
        Dhesp_4,
        Dhesp_5,
        Dhesp_Poor,
        Dhh_owned,
        Dhhtp_c4_1,
        Dhhtp_c4_2,
        Dhhtp_c4_3,
        Dhhtp_c4_4,
        Dhhtp_c4_CoupleChildren,
        Dhhtp_c4_CoupleNoChildren,
        Dhhtp_c4_SingleChildren,
        Dhhtp_c4_SingleNoChildren,
        Dlltsd01sp,
        Dlltsdsp,
        Dnc,
        Dnc02,
        Dst,
        Elig_pen,
        Elig_pen_L1,
        HrsProvidedInformalIHS,
        HrsReceivedFormalIHS,
        HrsReceivedInformalIHS,
        L_Dehmf_c3,
        L_Dehmf_c3_1,
        L_Dehmf_c3_2,
        L_Dehmf_c3_3,
        L_Dehmf_c3_High,
        L_Dhe,
        L_Dhhtp_c4_1,
        L_Dhhtp_c4_2,
        L_Dhhtp_c4_3,
        L_Dhhtp_c4_4,
        L_Dhhtp_c4_CoupleNoChildren,
        L_Les_c4_1,
        L_Les_c4_2,
        L_Les_c4_3,
        L_Les_c4_4,
        L_Les_c4_Employed,
        L_Les_c4_NotEmployed,
        L_Les_c4_Retired,
        L_Les_c4_Student,
        L_Need_socare,
        L_Ydses_c5,
        Les_c3_1,
        Les_c3_2,
        Les_c3_3,
        Les_c3_Employed,
        Les_c3_NotEmployed,
        Les_c3_Student,
        Les_c4_1,
        Les_c4_2,
        Les_c4_3,
        Les_c4_4,
        Les_c4_Employed,
        Les_c4_NotEmployed,
        Les_c4_NotEmployed_Dgn,
        Les_c4_Retired,
        Les_c4_Retired_Dgn,
        Les_c4_Student,
        Les_c4_Student_Dgn,
        Lesdf_c4_1,
        Lesdf_c4_2,
        Lesdf_c4_3,
        Lesdf_c4_4,
        Lesdf_c4_BothEmployed,
        Lesdf_c4_BothNotEmployed,
        Lesdf_c4_EmpSpouseNotEmp,
        Lesdf_c4_NotEmpSpouseEmp,
        Lessp_c3_1,
        Lessp_c3_2,
        Lessp_c3_3,
        Lessp_c3_Employed,
        Lessp_c3_Student,
        New_rel,
        ProvideCare,
        UK1,
        UK10,
        UK11,
        UK12,
        UK2,
        UK3,
        UK4,
        UK5,
        UK6,
        UK7,
        UK8,
        UK9,
        Y,
        Y2223,
        Ydses_c5,
        YearSquared,
        Ynbcpdf_dv,
        Yplgrs_dv,
        Ypnbihs_dv,
        Ypnbihs_dv_sq,
        Ypncp,
        Ypnoab,
        Yptciihs_dv,
        careHrsFormalIhs,
        careHrsFormalIhsL1,
        careHrsInformalIhs,
        careHrsInformalIhsL1,
        careHrsProvidedWeekIhs,
        careHrsProvidedWeekIhsL1,
        careMarket,
        careMarketFormal,
        careMarketFormalL1,
        careMarketFormalPartner,
        careMarketInformal,
        careMarketInformalL1,
        careMarketInformalPartner,
        careMarketL1,
        careMarketMixed,
        careMarketMixedL1,
        careMarketMixedPartner,
        careNeedFlag,
        careNeedFlagL1,
        careNeedPartnerFlag,
        careProvidedFlag,
        careProvidedFlagL1,
        careReceivedFlag,
        careReceivedFlagL1,
        careReceivedPartnerFlag,
        dage10prime,
        dage2old,
        dage5,
        demAge,
        demAge20to24,
        demAge25to29,
        demAge30to34,
        demAge35to39,
        demAge40to44,
        demAge45to49,
        demAge50to54,
        demAge55to59,
        demAge60to64,
        demAge65to69,
        demAge67to68,
        demAge69to70,
        demAge70to74,
        demAge71to72,
        demAge73to74,
        demAge75to76,
        demAge75to79,
        demAge77to78,
        demAge79to80,
        demAge80to84,
        demAge81to82,
        demAge83to84,
        demAge85plus,
        demAgePartnerDiff,
        demAgePartnerDiffL1,
        demAgeSq,
        demCompHhC4,
        demCompHhC4CoupleChL1,
        demCompHhC4CoupleNoChL1,
        demCompHhC4L1,
        demCompHhC4L1SingleChL1,
        demCompHhC4SingleNoChL1,
        demCompHhC8,
        demCompHhC81L1,
        demCompHhC82L1,
        demCompHhC83L1,
        demCompHhC84L1,
        demCompHhC85L1,
        demCompHhC86L1,
        demCompHhC87L1,
        demCompHhC88L1,
        demCompHhC8L1,
        demEnterPartnerFlag,
        demEnterPartnerFlagL1,
        demEthnC4,
        demEthnC4Asian,
        demEthnC4Black,
        demEthnC4Other,
        demEthnC4White,
        demEthnC4_1,
        demEthnC4_2,
        demEthnC4_3,
        demEthnC4_4,
        demMaleFlag,
        demNChild,
        demNChild0to2,
        demNChild0to2L1,
        demNChildL1,
        demPartnerNYear,
        demPartnerNYearL1,
        demPartnerStatus,
        demPartnerStatusL1,
        demPartnerStatusPartnered,
        demPartnerStatusPartneredL1,
        demPartnerStatusSingle,
        demPartnerStatusSingleL1,
        demPensAgeFlag,
        demPensAgeFlag_NotEmployedL1,
        demPensPartnerAgeFlag,
        demRgn,
        demRgnUKC,
        demRgnUKD,
        demRgnUKE,
        demRgnUKF,
        demRgnUKG,
        demRgnUKH,
        demRgnUKI,
        demRgnUKJ,
        demRgnUKK,
        demRgnUKL,
        demRgnUKM,
        demRgnUKN,
        demRgn_1,
        demRgn_10,
        demRgn_11,
        demRgn_12,
        demRgn_2,
        demRgn_3,
        demRgn_4,
        demRgn_5,
        demRgn_6,
        demRgn_7,
        demRgn_8,
        demRgn_9,
        demYear,
        demYear20,
        dot_1,
        dot_2,
        dot_3,
        dot_4,
        econ_realequivinc,
        eduHighestC4,
        eduHighestC4High,
        eduHighestC4HighL1,
        eduHighestC4HighL1_demAge,
        eduHighestC4High_demAge,
        eduHighestC4L1,
        eduHighestC4Low,
        eduHighestC4LowL1,
        eduHighestC4LowL1_demAge,
        eduHighestC4Low_demAge,
        eduHighestC4Medium,
        eduHighestC4MediumL1,
        eduHighestC4MediumL1_demAge,
        eduHighestC4Medium_demAge,
        eduHighestC4Na,
        eduHighestC4NaL1,
        eduHighestC4NaL1_demAge,
        eduHighestC4Na_demAge,
        eduHighestParentC3,
        eduHighestParentC3High,
        eduHighestParentC3HighL1,
        eduHighestParentC3L1,
        eduHighestParentC3L1_1,
        eduHighestParentC3L1_2,
        eduHighestParentC3L1_3,
        eduHighestParentC3Low,
        eduHighestParentC3LowL1,
        eduHighestParentC3Medium,
        eduHighestParentC3MediumL1,
        eduHighestParentC3_1,
        eduHighestParentC3_2,
        eduHighestParentC3_3,
        eduHighestPartnerC3HighL1,
        eduHighestPartnerC3LowL1,
        eduHighestPartnerC3MediumL1,
        eduSampleFlag,
        eduSampleFlagL1,
        eduSampleFlag_Male,
        eduSampleFlag_Mcs,
        eduSampleFlag_McsL1,
        eduSampleFlag_Pcs,
        eduSampleFlag_PcsL1,
        eduSampleFlag_Q2L1,
        eduSampleFlag_Q3L1,
        eduSampleFlag_Q4L1,
        eduSampleFlag_Q5L1,
        eduSampleFlag_Single,
        eduSampleFlag_demMaleFlag,
        eduSampleFlag_demNChild0to2L1,
        eduSampleFlag_demNChildL1,
        eduSampleFlag_yCapitalPers,
        eduSampleFlag_yCapitalPersL1,
        eduSampleFlag_yCapitalPersL2,
        eduSampleFlag_yEmpPersGross,
        eduSampleFlag_yEmpPersGrossL1,
        eduSampleFlag_yEmpPersGrossL2,
        exp_emp,
        exp_incchange,
        exp_poverty,
        fertilityRate,
        healthDsblLongtermFlag,
        healthDsblLongtermFlagL1,
        healthMentalMcs,
        healthMentalMcsL1,
        healthMentalPartnerMcs,
        healthMentalPartnerMcsL1,
        healthPartnerSelfRated,
        healthPartnerSelfRatedExcellent,
        healthPartnerSelfRatedFair,
        healthPartnerSelfRatedGood,
        healthPartnerSelfRatedPoor,
        healthPartnerSelfRatedVeryGood,
        healthPartnerSelfRated_1,
        healthPartnerSelfRated_2,
        healthPartnerSelfRated_3,
        healthPartnerSelfRated_4,
        healthPartnerSelfRated_5,
        healthPartnerSelfRatedExcel,
        healthSelfRatedExcellentL1,
        healthSelfRatedFairL1,
        healthSelfRatedGoodL1,
        healthSelfRatedVeryGoodL1,
        healthPhysicalPartnerPcs,
        healthPhysicalPartnerPcsL1,
        healthPhysicalPcs,
        healthPhysicalPcsL1,
        healthSelfRated,
        healthSelfRatedExcellent,
        healthSelfRatedFair,
        healthSelfRatedGood,
        healthSelfRatedPoor,
        healthSelfRatedVeryGood,
        healthSelfRated_1,
        healthSelfRated_2,
        healthSelfRated_3,
        healthSelfRated_4,
        healthSelfRated_5,
        idpartner,
        idperson,
        informal_socare_hrs,
        l_,
        labPt,
        labStatusC3,
        labStatusC3EmployedL1,
        labStatusC3L1,
        labStatusC3NotEmployedL1,
        labStatusC3StudentL1,
        labStatusC4,
        labStatusC4EmployedL1,
        labStatusC4EmployedL1_Male,
        labStatusC4L1,
        labStatusC4NotEmployedL1,
        labStatusC4NotEmployedL1_Male,
        labStatusC4RetiredL1,
        labStatusC4RetiredL1_Male,
        labStatusC4StudentL1,
        labStatusC4StudentL1_Male,
        labStatusPartnerAndOwnC4,
        labStatusPartnerAndOwnC41L1,
        labStatusPartnerAndOwnC42L1,
        labStatusPartnerAndOwnC43L1,
        labStatusPartnerAndOwnC44L1,
        labStatusPartnerAndOwnC4L1,
        labStatusPartnerC3,
        need_care,
        receive_care,
        // New-naming variables and interaction terms from refactored estimation files (PR #465)
        // Social care multinomial - new demAge prefix for age-group dummies
        demAge67to68_Formal,
        demAge67to68_Mixed,
        demAge69to70_Formal,
        demAge69to70_Mixed,
        demAge71to72_Formal,
        demAge71to72_Mixed,
        demAge73to74_Formal,
        demAge73to74_Mixed,
        demAge75to76_Formal,
        demAge75to76_Mixed,
        demAge77to78_Formal,
        demAge77to78_Mixed,
        demAge79to80_Formal,
        demAge79to80_Mixed,
        demAge81to82_Formal,
        demAge81to82_Mixed,
        demAge83to84_Formal,
        demAge83to84_Mixed,
        demAge85plus_Formal,
        demAge85plus_Mixed,
        // Social care multinomial - new careMarket naming
        careMarketFormalL1_Formal,
        careMarketFormalL1_Mixed,
        careMarketInformalL1_Formal,
        careMarketInformalL1_Mixed,
        careMarketMixedL1_Formal,
        careMarketMixedL1_Mixed,
        // Health outcome interaction terms (ordered probit / multinomial health regression)
        demAge_Fair,
        demAge_Good,
        demAge_Low,
        demAge_Medium,
        demAge_Poor,
        demAge_VeryGood,
        demAgeSq_Fair,
        demAgeSq_Good,
        demAgeSq_Low,
        demAgeSq_Medium,
        demAgeSq_Poor,
        demAgeSq_VeryGood,
        demMaleFlag_Fair,
        demMaleFlag_Formal,
        demMaleFlag_Good,
        demMaleFlag_Low,
        demMaleFlag_Medium,
        demMaleFlag_Mixed,
        demMaleFlag_Poor,
        demMaleFlag_VeryGood,
        demCompHhC4CoupleChL1_Fair,
        demCompHhC4CoupleChL1_Good,
        demCompHhC4CoupleChL1_Poor,
        demCompHhC4CoupleChL1_VeryGood,
        demCompHhC4SingleNoChL1_Fair,
        demCompHhC4SingleNoChL1_Good,
        demCompHhC4SingleNoChL1_Poor,
        demCompHhC4SingleNoChL1_VeryGood,
        demEthnC4Asian_Fair,
        demEthnC4Asian_Formal,
        demEthnC4Asian_Good,
        demEthnC4Asian_Mixed,
        demEthnC4Asian_Poor,
        demEthnC4Asian_VeryGood,
        demEthnC4Black_Formal,
        demEthnC4Black_Mixed,
        demEthnC4Other_Formal,
        demEthnC4Other_Mixed,
        demPartnerStatusPartnered_Formal,
        demPartnerStatusPartnered_Mixed,
        demRgnUKC_Formal,
        demRgnUKC_Mixed,
        demRgnUKD_Formal,
        demRgnUKD_Mixed,
        demRgnUKE_Fair,
        demRgnUKE_Formal,
        demRgnUKE_Good,
        demRgnUKE_Low,
        demRgnUKE_Medium,
        demRgnUKE_Mixed,
        demRgnUKE_Poor,
        demRgnUKE_VeryGood,
        demRgnUKF_Fair,
        demRgnUKF_Formal,
        demRgnUKF_Good,
        demRgnUKF_Mixed,
        demRgnUKF_Poor,
        demRgnUKF_VeryGood,
        demRgnUKG_Fair,
        demRgnUKG_Formal,
        demRgnUKG_Good,
        demRgnUKG_Mixed,
        demRgnUKG_Poor,
        demRgnUKG_VeryGood,
        demRgnUKH_Fair,
        demRgnUKH_Formal,
        demRgnUKH_Good,
        demRgnUKH_Mixed,
        demRgnUKH_Poor,
        demRgnUKH_VeryGood,
        demRgnUKJ_Fair,
        demRgnUKJ_Formal,
        demRgnUKJ_Good,
        demRgnUKJ_Mixed,
        demRgnUKJ_Poor,
        demRgnUKJ_VeryGood,
        demRgnUKK_Fair,
        demRgnUKK_Formal,
        demRgnUKK_Good,
        demRgnUKK_Mixed,
        demRgnUKK_Poor,
        demRgnUKK_VeryGood,
        demRgnUKL_Formal,
        demRgnUKL_Mixed,
        demRgnUKM_Formal,
        demRgnUKM_Low,
        demRgnUKM_Medium,
        demRgnUKM_Mixed,
        demRgnUKN_Fair,
        demRgnUKN_Formal,
        demRgnUKN_Good,
        demRgnUKN_Low,
        demRgnUKN_Medium,
        demRgnUKN_Mixed,
        demRgnUKN_Poor,
        demRgnUKN_VeryGood,
        demYear_Fair,
        demYear_Good,
        demYear_Low,
        demYear_Medium,
        demYear_Poor,
        demYear_VeryGood,
        demYear2020,
        demYear2020_Formal,
        demYear2020_Mixed,
        demYear2021,
        demYear2021_Fair,
        demYear2021_Formal,
        demYear2021_Good,
        demYear2021_Mixed,
        demYear2021_Poor,
        demYear2021_VeryGood,
        // Education interaction terms
        eduHighestC4Low_Formal,
        eduHighestC4Low_Mixed,
        eduHighestC4Medium_Formal,
        eduHighestC4Medium_Mixed,
        eduHighestC4LowL1_Fair,
        eduHighestC4LowL1_Good,
        eduHighestC4LowL1_Poor,
        eduHighestC4LowL1_VeryGood,
        eduHighestC4MediumL1_Fair,
        eduHighestC4MediumL1_Good,
        eduHighestC4MediumL1_Poor,
        eduHighestC4MediumL1_VeryGood,
        eduHighestC4NaL1_Fair,
        eduHighestC4NaL1_Good,
        eduHighestC4NaL1_Poor,
        eduHighestC4NaL1_VeryGood,
        // Health interaction terms
        healthDsblLongtermFlagL1_Fair,
        healthDsblLongtermFlagL1_Good,
        healthDsblLongtermFlagL1_Poor,
        healthDsblLongtermFlagL1_VeryGood,
        healthMentalMcsL1_Fair,
        healthMentalMcsL1_Good,
        healthMentalMcsL1_Poor,
        healthMentalMcsL1_VeryGood,
        healthPhysicalPcsL1_Fair,
        healthPhysicalPcsL1_Good,
        healthPhysicalPcsL1_Poor,
        healthPhysicalPcsL1_VeryGood,
        healthSelfRatedExcellent_Formal,
        healthSelfRatedExcellent_Mixed,
        healthSelfRatedFair_Formal,
        healthSelfRatedFair_Mixed,
        healthSelfRatedGood_Formal,
        healthSelfRatedGood_Mixed,
        healthSelfRatedVeryGood_Formal,
        healthSelfRatedVeryGood_Mixed,
        // Labour status interaction terms
        labStatusC4EmployedL1_Fair,
        labStatusC4EmployedL1_Good,
        labStatusC4EmployedL1_Poor,
        labStatusC4EmployedL1_VeryGood,
        labStatusC4RetiredL1_Fair,
        labStatusC4RetiredL1_Good,
        labStatusC4RetiredL1_Poor,
        labStatusC4RetiredL1_VeryGood,
        // Income quintile new naming (current and lag, with interaction suffixes)
        yHhQuintilesMonthC5Q2,
        yHhQuintilesMonthC5Q2L1,
        yHhQuintilesMonthC5Q2L1_Fair,
        yHhQuintilesMonthC5Q2L1_Good,
        yHhQuintilesMonthC5Q2L1_Poor,
        yHhQuintilesMonthC5Q2L1_VeryGood,
        yHhQuintilesMonthC5Q2_Formal,
        yHhQuintilesMonthC5Q2_Mixed,
        yHhQuintilesMonthC5Q3,
        yHhQuintilesMonthC5Q3L1,
        yHhQuintilesMonthC5Q3L1_Fair,
        yHhQuintilesMonthC5Q3L1_Good,
        yHhQuintilesMonthC5Q3L1_Poor,
        yHhQuintilesMonthC5Q3L1_VeryGood,
        yHhQuintilesMonthC5Q3_Formal,
        yHhQuintilesMonthC5Q3_Mixed,
        yHhQuintilesMonthC5Q4,
        yHhQuintilesMonthC5Q4L1,
        yHhQuintilesMonthC5Q4L1_Fair,
        yHhQuintilesMonthC5Q4L1_Good,
        yHhQuintilesMonthC5Q4L1_Poor,
        yHhQuintilesMonthC5Q4L1_VeryGood,
        yHhQuintilesMonthC5Q4_Formal,
        yHhQuintilesMonthC5Q4_Mixed,
        yHhQuintilesMonthC5Q5,
        yHhQuintilesMonthC5Q5L1,
        yHhQuintilesMonthC5Q5L1_Fair,
        yHhQuintilesMonthC5Q5L1_Good,
        yHhQuintilesMonthC5Q5L1_Poor,
        yHhQuintilesMonthC5Q5L1_VeryGood,
        yHhQuintilesMonthC5Q5_Formal,
        yHhQuintilesMonthC5Q5_Mixed,
        // Lowercase / legacy-naming aliases
        dag_L1,
        dagsq_L1,
        dgn,
        dhh_owned_L1,
        dlltsd01_L1,
        dnc_L1,
        drgn1_L1,
        econ_benefits_L1,
        financial_distress_L1,
        labWageHrlyL1,
        labWageHrlyLogL1,
        realWageGrowth,
        stm,
        wealthPrptyFlagL1,
        yCapitalPersMonthL1,
        yCapitalPersMonthL2,
        yEmpPersGrossMonthL1,
        yEmpPersGrossMonthL2,
        yMiscPersGrossMonthL1,
        yNonBenPersGrossMonthL1,
        yPensPersGrossMonthL1,
        yPensPersGrossMonthL2,
        ypncp_L1,
        ypnoab_L1,
        yPersAndPartnerGrossDiffMonthL1,
    }

    public double getDoubleValue(Enum<?> variableID) {

        switch ((Variables) variableID) {

            case Age80plus -> {
                return (demAge >= 80) ? 1. : 0.;
            }
            case Age70plus -> {
                return (demAge >= 70) ? 1. : 0.;
            }
            case Age, Dag, demAge -> {
                return (double) demAge;
            }
            case Dag_L1 -> {
                return (double) demAge - 1;
            }
            case demAgeSq, demAge_Sq, Dag_sq, AgeSquared -> {
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
                return (demAge >= 45 && demAge <= MAX_AGE_MATERNITY) ? 1. : 0.;
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
            case Age67to68, Age67to68_Mixed, Age67to68_Formal -> {
                return (demAge >= 67 && demAge <= 68) ? 1. : 0.;
            }
            case Age69to70, Age69to70_Mixed, Age69to70_Formal -> {
                return (demAge >= 69 && demAge <= 70) ? 1. : 0.;
            }
            case Age71to72, Age71to72_Mixed, Age71to72_Formal -> {
                return (demAge >= 71 && demAge <= 72) ? 1. : 0.;
            }
            case Age73to74, Age73to74_Mixed, Age73to74_Formal -> {
                return (demAge >= 73 && demAge <= 74) ? 1. : 0.;
            }
            case Age75to76, Age75to76_Mixed, Age75to76_Formal -> {
                return (demAge >= 75 && demAge <= 76) ? 1. : 0.;
            }
            case Age77to78, Age77to78_Mixed, Age77to78_Formal -> {
                return (demAge >= 77 && demAge <= 78) ? 1. : 0.;
            }
            case Age79to80, Age79to80_Mixed, Age79to80_Formal -> {
                return (demAge >= 79 && demAge <= 80) ? 1. : 0.;
            }
            case Age81to82, Age81to82_Mixed, Age81to82_Formal -> {
                return (demAge >= 81 && demAge <= 82) ? 1. : 0.;
            }
            case Age83to84, Age83to84_Mixed, Age83to84_Formal -> {
                return (demAge >= 83 && demAge <= 84) ? 1. : 0.;
            }
            case Age85plus, Age85plus_Mixed, Age85plus_Formal -> {
                return (demAge >= 85) ? 1. : 0.;
            }
            case StatePensionAge -> {
                return (demAge >= 68) ? 1. : 0.;
            }
            case NeedCare_L1 -> {
                return (Indicator.True.equals(careNeedFlagL1)) ? 1. : 0.;
            }
            case NeedCare -> {
                return (Indicator.True.equals(careNeedFlag)) ? 1. : 0.;
            }
            case NeedCarePartner -> {
                Person partner = getPartner();
                return (partner != null && Indicator.True.equals(partner.getCareNeedFlag())) ? 1. : 0.;
            }
            case ProvideCare_L1 -> {
                return (Indicator.True.equals(careProvidedFlagL1)) ? 1. : 0.;
            }
            case ReceiveCare_L1 -> {
                return (getCareHrsTotalWeekL1() > 0.01) ? 1. : 0.;
            }
            case ReceiveCare -> {
                return (getHoursFormalSocialCare() + getCareHrsInformalWeek() > 0.01) ? 1. : 0.;
            }
            case ReceiveCarePartner -> {
                Person partner = getPartner();
                if (partner == null) {
                    return 0.;
                }
                return (partner.getHoursFormalSocialCare() + partner.getCareHrsInformalWeek() > 0.01) ? 1. : 0.;
            }
            case HrsReceivedInformalIHS_L1 -> {
                return FastMath.asinh(getCareHrsInformalWeekL1());
            }
            case HrsReceivedFormalIHS_L1 -> {
                return FastMath.asinh(getCareHrsFormalWeekL1());
            }
            case HrsProvidedInformalIHS_L1 -> {
                double hours = (careHrsProvidedWeekL1 != null && careHrsProvidedWeekL1 > 0.0) ? careHrsProvidedWeekL1 : 0.0;
                return FastMath.asinh(hours);
            }
            case CareMarketMixed -> {
                return (getHoursFormalSocialCare() > 0.01 && getCareHrsInformalWeek() > 0.01) ? 1. : 0.;
            }
            case CareMarketMixed_L1, CareMarketMixed_L1_Mixed, CareMarketMixed_L1_Formal -> {
                return (getCareHrsFormalWeekL1() > 0.01 && getCareHrsInformalWeekL1() > 0.01) ? 1. : 0.;
            }
            case CareMarketInformal_L1, CareMarketInformal_L1_Mixed, CareMarketInformal_L1_Formal -> {
                return (getCareHrsFormalWeekL1() < 0.01 && getCareHrsInformalWeekL1() > 0.01) ? 1. : 0.;
            }
            case CareMarketInformalPartner -> {
                Person partner = getPartner();
                if (partner == null) {
                    return 0.;
                }
                return (partner.getHoursFormalSocialCare() < 0.01 && partner.getCareHrsInformalWeek() > 0.01) ? 1. : 0.;
            }
            case CareMarketFormal_L1, CareMarketFormal_L1_Mixed, CareMarketFormal_L1_Formal -> {
                return (getCareHrsFormalWeekL1() > 0.01 && getCareHrsInformalWeekL1() < 0.01) ? 1. : 0.;
            }
            case CareMarketFormalPartner -> {
                Person partner = getPartner();
                if (partner == null) {
                    return 0.;
                }
                return (partner.getHoursFormalSocialCare() > 0.01 && partner.getCareHrsInformalWeek() < 0.01) ? 1. : 0.;
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
                return (getCareHoursFromDaughter_L1() > 0.01 && Math.abs(getCareHrsInformalWeekL1() - getCareHoursFromDaughter_L1()) < 0.01) ? 1. : 0.;
            }
            case CareFromDaughterSon_L1 -> {
                return (getCareHoursFromDaughter_L1() > 0.01 && getCareHoursFromSon_L1() > 0.01) ? 1. : 0.;
            }
            case CareFromDaughterOther_L1 -> {
                return (getCareHoursFromDaughter_L1() > 0.01 && getCareHoursFromOther_L1() > 0.01) ? 1. : 0.;
            }
            case CareFromSonOnly_L1 -> {
                return (getCareHoursFromSon_L1() > 0.01 && Math.abs(getCareHrsInformalWeekL1() - getCareHoursFromSon_L1()) < 0.01) ? 1. : 0.;
            }
            case CareFromSonOther_L1 -> {
                return (getCareHoursFromSon_L1() > 0.01 && getCareHoursFromOther_L1() > 0.01) ? 1. : 0.;
            }
            case CareFromOtherOnly_L1 -> {
                return (getCareHoursFromOther_L1() > 0.01 && Math.abs(getCareHrsInformalWeekL1() - getCareHoursFromOther_L1()) < 0.01) ? 1. : 0.;
            }
            case CareFromFormal -> {
                return (careFormalFlag) ? 1. : 0.;
            }
            case CareFromInformal -> {
                return (careFromInformalFlag) ? 1. : 0.;
            }
            case CareFromPartner -> {
                return (careFromInformalFlag && getPartner() != null) ? 1. : 0.;
            }
            case CareFromDaughter -> {
                return 0.;
            }
            case CareFromSon -> {
                return 0.;
            }
            case CareFromOther -> {
                return (careFromInformalFlag && getPartner() == null) ? 1. : 0.;
            }
            case Constant, Constant_Mixed, Constant_Formal -> {
                return 1.;
            }
            case Dcpyy_L1 -> {
                return (demPartnerNYearL1 != null) ? (double) demPartnerNYearL1 : 0.0;
            }
            case Dcpagdf_L1 -> {
                return (demAgePartnerDiffL1 != null) ? (double) demAgePartnerDiffL1 : 0.0;
            }
            case Dcpst_Single -> {
                return (Dcpst.Single.equals(getDemPartnerStatus())) ? 1.0 : 0.0;
            }
            case Dcpst_Partnered, Partnered, Partnered_Mixed, Partnered_Formal -> {
                return (Dcpst.Partnered.equals(getDemPartnerStatus())) ? 1.0 : 0.0;
            }
            case Dcpst_PreviouslyPartnered -> {
                return (Dcpst.Single.equals(getDemPartnerStatus())) ? 1.0 : 0.0;
            }
            case Dcpst_Partnered_L1 -> {
                if (demPartnerStatusL1 != null) {
                    return demPartnerStatusL1.equals(Dcpst.Partnered) ? 1. : 0.;
                } else return 0.;
            }
            case Dcpst_Single_L1 -> {
                if (demPartnerStatusL1 != null) {
                    return demPartnerStatusL1.equals(Dcpst.Single) ? 1. : 0.;
                } else return 0.;
            }
            case Dcpst_PreviouslyPartnered_L1 -> {
                if (demPartnerStatusL1 != null) {
                    return demPartnerStatusL1.equals(Dcpst.Single) ? 1. : 0.;
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
            case D_children, D_Children, demDChild, demDChildL1 -> {
                return (getNumberChildrenAll() > 0) ? 1. : 0.;
            }
            case Dnc_L1 -> {
                return (double) getNumberChildrenAllL1();
            }
            case Dnc02_L1 -> {
                return (double) getNumberChildren02L1();
            }
            case Dnc017 -> {
                return (double) getNumberChildren017();
            }
            case Dgn, Dgn_Mixed, Dgn_Formal -> {
                return (Gender.Male.equals(demMaleFlag)) ? 1.0 : 0.0;
            }
            case Dhe -> {
                return (double) healthSelfRated.getValue();
            }
            case Dhe_L1 -> {
                return (double) healthSelfRatedL1.getValue();
            }
            case Dhe_Excellent, Dhe_Excellent_Mixed, Dhe_Excellent_Formal -> {
                return (Dhe.Excellent.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Dhe_VeryGood, Dhe_VeryGood_Mixed, Dhe_VeryGood_Formal -> {
                return (Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Dhe_Good, Dhe_Good_Mixed, Dhe_Good_Formal -> {
                return (Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Dhe_Fair, Dhe_Fair_Mixed, Dhe_Fair_Formal -> {
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
            case Dhesp_Excellent -> {
                Person partner = getPartner();
                return (partner != null && Dhe.Excellent.equals(partner.getHealthSelfRated())) ? 1. : 0.;
            }
            case Dhesp_VeryGood_L1 -> {
                return (Dhe.VeryGood.equals(healthPartnerSelfRatedL1)) ? 1. : 0.;
            }
            case Dhesp_VeryGood -> {
                Person partner = getPartner();
                return (partner != null && Dhe.VeryGood.equals(partner.getHealthSelfRated())) ? 1. : 0.;
            }
            case Dhesp_Good_L1 -> {
                return (Dhe.Good.equals(healthPartnerSelfRatedL1)) ? 1. : 0.;
            }
            case Dhesp_Good -> {
                Person partner = getPartner();
                return (partner != null && Dhe.Good.equals(partner.getHealthSelfRated())) ? 1. : 0.;
            }
            case Dhesp_Fair_L1 -> {
                return (Dhe.Fair.equals(healthPartnerSelfRatedL1)) ? 1. : 0.;
            }
            case Dhesp_Fair -> {
                Person partner = getPartner();
                return (partner != null && Dhe.Fair.equals(partner.getHealthSelfRated())) ? 1. : 0.;
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
            case Dhe_mcs_L1, dhe_mcs_L1, L_Dhe_mcs, Dhe_Mcs_L1 -> {
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
            case Dhe_pcs_L1, dhe_pcs_L1, L_Dhe_pcs, Dhe_Pcs_L1 -> {
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
                return demLifeSatScore0to10;
            }
            case Dls_L1 -> {
                if (demLifeSatScore0to10L1 != null && demLifeSatScore0to10L1 >= 0.) {
                    return demLifeSatScore0to10L1;
                } else return 0.;
            }
            case Dhmghq_L1 -> {
                return gethealthPsyDstrss_lag1();
            }
            case Dnc_L1_ -> {
                return getDoubleValue(Variables.Dnc_L1);
            }
            case Dhesp_L1 -> {
                return (healthPartnerSelfRatedL1 != null) ? (double) healthPartnerSelfRatedL1.getValue() : 0.0;
            }
            case Ded -> {
                return (Indicator.True.equals(eduSpellFlag)) ? 1.0 : 0.0;
            }
            case Ded_Dgn -> {
                return (Indicator.True.equals(eduSpellFlag) && Gender.Male.equals(demMaleFlag)) ? 1.0 : 0.0;
            }
            case Ded_Dag -> {
                return (Indicator.True.equals(eduSpellFlag)) ? (double) demAge : 0.0;
            }
            case Ded_Dag_sq -> {
                return (Indicator.True.equals(eduSpellFlag)) ? (double) getDemAgeSq() : 0.0;
            }
            case Ded_Dhe_pcs_L1 -> {
                return (Indicator.True.equals(eduSpellFlag)) ? getDoubleValue(Variables.Dhe_pcs_L1) : 0.0;
            }
            case Ded_Dhe_mcs_L1 -> {
                return (Indicator.True.equals(eduSpellFlag)) ? getDoubleValue(Variables.Dhe_mcs_L1) : 0.0;
            }
            case Ded_Ypncp_L1 -> {
                return (Indicator.True.equals(eduSpellFlag)) ? getDoubleValue(Variables.Ypncp_L1) : 0.0;
            }
            case Ded_Yplgrs_dv_L1 -> {
                return (Indicator.True.equals(eduSpellFlag)) ? getDoubleValue(Variables.Yplgrs_dv_L1) : 0.0;
            }
            case Ded_Yplgrs_dv_L2 -> {
                return (Indicator.True.equals(eduSpellFlag)) ? getDoubleValue(Variables.Yplgrs_dv_L2) : 0.0;
            }
            case Ded_Ypncp_L2 -> {
                return (Indicator.True.equals(eduSpellFlag)) ? getDoubleValue(Variables.Ypncp_L2) : 0.0;
            }
            case Ded_Ydses_c5_Q2_L1 -> {
                return (Indicator.True.equals(eduSpellFlag)) ? getDoubleValue(Variables.Ydses_c5_Q2_L1) : 0.0;
            }
            case Ded_Ydses_c5_Q3_L1 -> {
                return (Indicator.True.equals(eduSpellFlag)) ? getDoubleValue(Variables.Ydses_c5_Q3_L1) : 0.0;
            }
            case Ded_Ydses_c5_Q4_L1 -> {
                return (Indicator.True.equals(eduSpellFlag)) ? getDoubleValue(Variables.Ydses_c5_Q4_L1) : 0.0;
            }
            case Ded_Ydses_c5_Q5_L1 -> {
                return (Indicator.True.equals(eduSpellFlag)) ? getDoubleValue(Variables.Ydses_c5_Q5_L1) : 0.0;
            }
            case Ded_Dnc_L1_ -> {
                return (Indicator.True.equals(eduSpellFlag)) ? getDoubleValue(Variables.Dnc_L1) : 0.0;
            }
            case Ded_Dnc02_L1 -> {
                return (Indicator.True.equals(eduSpellFlag)) ? getDoubleValue(Variables.Dnc02_L1) : 0.0;
            }
            case Ded_Dcpst_Single -> {
                return (Indicator.True.equals(eduSpellFlag)) ? getDoubleValue(Variables.Dcpst_Single) : 0.0;
            }
            case Ded_L1 -> {
                return (double) eduSpellFlagL1.getValue() ;
            }
            case Deh_c3_High, Deh_c4_High -> {
                return (Education.High.equals(eduHighestC4)) ? 1.0 : 0.0;
            }
            case Deh_c4_High_L1 -> {
                return (Education.High.equals(eduHighestC4L1)) ? 1.0 : 0.0;
            }
            case Deh_c3_Medium, Deh_c4_Medium, Deh_c3_Medium_Mixed, Deh_c3_Medium_Formal -> {
                return (Education.Medium.equals(eduHighestC4)) ? 1.0 : 0.0;
            }
            case Deh_c3_Medium_L1, Deh_c4_Medium_L1 -> {
                return (Education.Medium.equals(eduHighestC4L1)) ? 1.0 : 0.0;
            }
            case Deh_c3_Low, Deh_c3_Low_Mixed, Deh_c3_Low_Formal -> {
                return (Education.Low.equals(eduHighestC4) || Education.InEducation.equals(eduHighestC4)) ? 1.0 : 0.0;
            }
            case Deh_c4_Low -> {
                return (Education.Low.equals(eduHighestC4)) ? 1.0 : 0.0;
            }
            case Deh_c4_Low_Dag -> {
                return (Education.Low.equals(eduHighestC4)) ? demAge : 0.0;
            }
            case Deh_c3_Low_L1 -> {
                return (Education.Low.equals(eduHighestC4L1) || Education.InEducation.equals(eduHighestC4L1)) ? 1.0 : 0.0;
            }
            case Deh_c4_Low_L1 -> {
                return (Education.Low.equals(eduHighestC4L1)) ? 1.0 : 0.0;
            }
            case Deh_c4_Medium_Dag -> {
                return (Education.Medium.equals(eduHighestC4)) ? demAge : 0.0;
            }
            case Deh_c4_Na -> {
                return (Education.InEducation.equals(eduHighestC4) || Indicator.True.equals(eduSpellFlag)) ? 1.0 : 0.0;
            }
            case Deh_c4_Na_L1 -> {
                return (Education.InEducation.equals(eduHighestC4L1) || Indicator.True.equals(eduSpellFlagL1)) ? 1.0 : 0.0;
            }
            case Dehm_c3_High -> {
                return (Education.High.equals(eduHighestMotherC4)) ? 1.0 : 0.0;
            }
            case Dehm_c3_Medium -> {
                return (Education.Medium.equals(eduHighestMotherC4)) ? 1.0 : 0.0;
            }
            case Dehm_c3_Low -> {
                return (Education.Low.equals(eduHighestMotherC4) || Education.InEducation.equals(eduHighestMotherC4)) ? 1.0 : 0.0;
            }
            case Dehf_c3_High -> {
                return (Education.High.equals(eduHighestFatherC4)) ? 1.0 : 0.0;
            }
            case Dehf_c3_Medium -> {
                return (Education.Medium.equals(eduHighestFatherC4)) ? 1.0 : 0.0;
            }
            case Dehf_c3_Low -> {
                return (Education.Low.equals(eduHighestFatherC4)) ? 1.0 : 0.0;
            }
            case Dehmf_c3_High -> {
                return (checkHighestParentalEducationEquals(Education.High)) ? 1.0 : 0.0;
            }
            case Dehmf_c3_Medium -> {
                return (checkHighestParentalEducationEquals(Education.Medium)) ? 1.0 : 0.0;
            }
            case Dehmf_c3_Medium_L1, L_Dehmf_c3_Medium_, L_Dehmf_c3_Medium -> {
                return (checkHighestParentalEducationEquals(Education.Medium)) ? 1.0 : 0.0;
            }
            case Dehmf_c3_Low -> {
                return (checkHighestParentalEducationEquals(Education.Low) || checkHighestParentalEducationEquals(Education.InEducation)) ? 1.0 : 0.0;
            }
            case Dehmf_c3_Low_L1, L_Dehmf_c3_Low_, L_Dehmf_c3_Low -> {
                return (checkHighestParentalEducationEquals(Education.Low) || checkHighestParentalEducationEquals(Education.InEducation)) ? 1.0 : 0.0;
            }
            case Dehsp_c3_Medium_L1 -> {
                return (Education.Medium.equals(eduHighestPartnerC4L1)) ? 1. : 0.;
            }
            case Dehsp_c3_Low_L1 -> {
                return (Education.Low.equals(eduHighestPartnerC4L1) || Education.InEducation.equals(eduHighestPartnerC4L1)) ? 1. : 0.;
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
            case Dhhtp_c4_SingleChildren_L1, L_Dhhtp_c4_SingleChildren, Dhhtp_c4_SingleChildren_L1_, demCompHhC4SingleChL1 -> {
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
                    return ((Les_c4.NotEmployed.equals(partner.labC4L1) || Les_c4.Retired.equals(partner.labC4L1)) && Dhhtp_c4.CoupleNoChildren.equals(getDemCompHhC4L1())) ? 1. : 0.;
                else
                    return 0.;
            }
            case Current_op_memb -> {
                return (privatePension.isMemberOP()) ? 1.0 : 0.0;
            }
            case Lnop_all -> {
                return (privatePension.isMemberOP()) ? Math.log(privatePension.getContRateOPEe() + privatePension.getContRateOPEr()) : 0.0;
            }
            case Earnings_Q2 -> {
                return (Quintiles.Q2.equals(getEmploymentEarningsQuintile())) ? 1.0 : 0.0;
            }
            case Earnings_Q3 -> {
                return (Quintiles.Q3.equals(getEmploymentEarningsQuintile())) ? 1.0 : 0.0;
            }
            case Earnings_Q4 -> {
                return (Quintiles.Q4.equals(getEmploymentEarningsQuintile())) ? 1.0 : 0.0;
            }
            case Earnings_Q5 -> {
                return (Quintiles.Q5.equals(getEmploymentEarningsQuintile())) ? 1.0 : 0.0;
            }
            case Employee_cont_0 -> {
                return (privatePension.getContRateOPEe() < 0.005) ? 1.0 : 0.0;
            }
            case Employee_cont_3 -> {
                return (privatePension.getContRateOPEe() > 0.025 && privatePension.getContRateOPEe() < 0.035) ? 1.0 : 0.0;
            }
            case Employee_cont_5 -> {
                return (privatePension.getContRateOPEe() > 0.045 && privatePension.getContRateOPEe() < 0.055) ? 1.0 : 0.0;
            }
            case Asinhop_empee -> {
                return FastMath.asinh(privatePension.getContRateOPEe() * 100.0);
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
            case Ethn_Black, EthnicityBlack, Ethn_Black_ -> {
                return demEthnC6.equals(Ethnicity.Black) ? 1. : 0.;
            }
            case Ethn_Other, EthnicityOther, Ethn_Other_ -> {
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
                return getCovidYLabGross();
            }
            case InverseMillsRatio, InvMillsRatio -> {
                return getInverseMillsRatio();
            }
            case Ld_children_2under -> {
                return (getNumberChildren02L1() > 0) ? 1.0 : 0.0;
            }
            case Ld_children_3under -> {
                return benefitUnit.getDem0to3L1().ordinal();
            }
            case Ld_children_4_12 -> {
                return benefitUnit.getDem4to12L1().ordinal();
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
            case Les_c4_Student_Dgn_L1 -> {
                return (Les_c4.Student.equals(labC4L1) && Gender.Male.equals(demMaleFlag)) ? 1. : 0. ;
            }
            case Les_c4_NotEmployed_L1 -> {
                return (Les_c4.NotEmployed.equals(labC4L1)) ? 1. : 0. ;
            }
            case Les_c4_NotEmployed_Dgn_L1 -> {
                return (Les_c4.NotEmployed.equals(labC4L1) && Gender.Male.equals(demMaleFlag)) ? 1. : 0. ;
            }
            case Les_c4_Retired_L1 -> {
                return (Les_c4.Retired.equals(labC4L1)) ? 1. : 0. ;
            }
            case Les_c4_Retired_Dgn_L1 -> {
                return (Les_c4.Retired.equals(labC4L1) && Gender.Male.equals(demMaleFlag)) ? 1. : 0. ;
            }
            case Lessp_c3_Student_L1 -> {
                Person partner = getPartner();
                if (partner != null && partner.labC4L1 != null)
                    return partner.labC4L1.equals(Les_c4.Student) ? 1. : 0.;
                else
                    return 0.;
            }
            case Lessp_c3_NotEmployed -> {
                Person partner = getPartner();
                if (partner != null && partner.labC4 != null)
                    return (partner.labC4.equals(Les_c4.NotEmployed) || partner.labC4.equals(Les_c4.Retired)) ? 1. : 0.;
                else
                    return 0.;
            }
            case Lessp_c3_NotEmployed_L1, labStatusPartnerC3NotEmployedL1 -> {
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
            case Lesdf_c4_EmployedSpouseNotEmployed_L1, Lesdf_c4_EmpSpouseNotEmp_L1 -> {                    //Own and partner's activity status lag(1)
                return (Lesdf_c4.EmployedSpouseNotEmployed.equals(labStatusPartnerAndOwnC4L1)) ? 1. : 0.;
            }
            case Lesdf_c4_NotEmployedSpouseEmployed_L1, Lesdf_c4_NotEmpSpouseEmp_L1 -> {
                return (Lesdf_c4.NotEmployedSpouseEmployed.equals(labStatusPartnerAndOwnC4L1)) ? 1. : 0.;
            }
            case Lesdf_c4_BothNotEmployed_L1 -> {
                if (labStatusPartnerAndOwnC4L1 != null)
                    return labStatusPartnerAndOwnC4L1.equals(Lesdf_c4.BothNotEmployed) ? 1. : 0.;
                else
                    return 0.;
            }
            case Liwwh, labWorkHist -> {
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
                return getUnemploymentRateByGenderEducationAgeYear(getDemMaleFlag(), getEduHighestC4(), getDemAge(), getYear());
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
            case Year2012, Y2012 -> {
                return (getYear() == 2012) ? 1. : 0.;
            }
            case Year2013, Y2013, Y2013_ -> {
                return (getYear() == 2013) ? 1. : 0.;
            }
            case Year2014, Y2014, Y2014_, demYear2014 -> {
                return (getYear() == 2014) ? 1. : 0.;
            }
            case Year2015, Y2015, demYear2015 -> {
                return (getYear() == 2015) ? 1. : 0.;
            }
            case Year2016, Y2016, demYear2016 -> {
                return (getYear() == 2016) ? 1. : 0.;
            }
            case Year2017, Y2017, demYear2017 -> {
                return (getYear() == 2017) ? 1. : 0.;
            }
            case Year2018, Y2018, demYear2018 -> {
                return (getYear() == 2018) ? 1. : 0.;
            }
            case Year2019, Y2019, demYear2019 -> {
                return (getYear() == 2019) ? 1. : 0.;
            }
            case Year2020, Y2020, Y2020_Mixed, Y2020_Formal, demYear2020 -> {
                return (getYear() == 2020) ? 1. : 0.;
            }
            case Year2021, Y2021, Y2021_Mixed, Y2021_Formal, demYear2021 -> {
                return (getYear() == 2021) ? 1. : 0.;
            }
            case Year2022, Y2022, demYear2022 -> {
                return (getYear() == 2022) ? 1. : 0.;
            }
            case Year2023, Y2023, demYear2023 -> {
                return (getYear() == 2023) ? 1. : 0.;
            }
            case Year2024, Y2024, demYear2024 -> {
                return (getYear() == 2024) ? 1. : 0.;
            }
            case Y2012_Poor -> {
                return (getYear() == 2012 && Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2012_Fair -> {
                return (getYear() == 2012 && Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2012_Good -> {
                return (getYear() == 2012 && Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2012_VeryGood -> {
                return (getYear() == 2012 && Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2013_Poor -> {
                return (getYear() == 2013 && Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2013_Fair -> {
                return (getYear() == 2013 && Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2013_Good -> {
                return (getYear() == 2013 && Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2013_VeryGood -> {
                return (getYear() == 2013 && Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2014_Poor -> {
                return (getYear() == 2014 && Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2014_Fair -> {
                return (getYear() == 2014 && Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2014_Good -> {
                return (getYear() == 2014 && Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2014_VeryGood -> {
                return (getYear() == 2014 && Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2015_Poor -> {
                return (getYear() == 2015 && Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2015_Fair -> {
                return (getYear() == 2015 && Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2015_Good -> {
                return (getYear() == 2015 && Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2015_VeryGood -> {
                return (getYear() == 2015 && Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2016_Poor -> {
                return (getYear() == 2016 && Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2016_Fair -> {
                return (getYear() == 2016 && Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2016_Good -> {
                return (getYear() == 2016 && Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2016_VeryGood -> {
                return (getYear() == 2016 && Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2017_Poor -> {
                return (getYear() == 2017 && Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2017_Fair -> {
                return (getYear() == 2017 && Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2017_Good -> {
                return (getYear() == 2017 && Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2017_VeryGood -> {
                return (getYear() == 2017 && Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2018_Poor -> {
                return (getYear() == 2018 && Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2018_Fair -> {
                return (getYear() == 2018 && Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2018_Good -> {
                return (getYear() == 2018 && Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2018_VeryGood -> {
                return (getYear() == 2018 && Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2019_Poor -> {
                return (getYear() == 2019 && Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2019_Fair -> {
                return (getYear() == 2019 && Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2019_Good -> {
                return (getYear() == 2019 && Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2019_VeryGood -> {
                return (getYear() == 2019 && Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2020_Poor -> {
                return (getYear() == 2020 && Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2020_Fair -> {
                return (getYear() == 2020 && Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2020_Good -> {
                return (getYear() == 2020 && Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2020_VeryGood -> {
                return (getYear() == 2020 && Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2021_Poor -> {
                return (getYear() == 2021 && Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2021_Fair -> {
                return (getYear() == 2021 && Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2021_Good -> {
                return (getYear() == 2021 && Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2021_VeryGood -> {
                return (getYear() == 2021 && Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2022_Poor -> {
                return (getYear() == 2022 && Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2022_Fair -> {
                return (getYear() == 2022 && Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2022_Good -> {
                return (getYear() == 2022 && Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2022_VeryGood -> {
                return (getYear() == 2022 && Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2023_Poor -> {
                return (getYear() == 2023 && Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2023_Fair -> {
                return (getYear() == 2023 && Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2023_Good -> {
                return (getYear() == 2023 && Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2023_VeryGood -> {
                return (getYear() == 2023 && Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2024_Poor -> {
                return (getYear() == 2024 && Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2024_Fair -> {
                return (getYear() == 2024 && Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2024_Good -> {
                return (getYear() == 2024 && Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case Y2024_VeryGood -> {
                return (getYear() == 2024 && Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
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
            case Year_transformed, demYearTransformed -> {
                return (Parameters.isFixTimeTrend && getYear() >= Parameters.timeTrendStopsIn) ? (double) Parameters.timeTrendStopsIn - 2000 : (double) getYear() - 2000;
            }
            case Year_transformed_monetary -> {
                return (double) model.getTimeTrendStopsInMonetaryProcesses() - 2000;
            } //Note: this returns base price year - 2000 (e.g. 17 for 2017 as base price year) and monetary variables are then uprated from 2017 level to the simulated year
            case Ydses_c5_Q2_L1, L_Ydses_c5_Q2, Ydses_c5_L1 -> {
                return (Ydses_c5.Q2.equals(getYHhQuintilesMonthC5L1())) ? 1.0 : 0.0;
            }
            case Ydses_c5_Q3_L1, L_Ydses_c5_Q3 -> {
                return (Ydses_c5.Q3.equals(getYHhQuintilesMonthC5L1())) ? 1.0 : 0.0;
            }
            case Ydses_c5_Q4_L1, L_Ydses_c5_Q4 -> {
                return (Ydses_c5.Q4.equals(getYHhQuintilesMonthC5L1())) ? 1.0 : 0.0;
            }
            case Ydses_c5_Q5_L1, L_Ydses_c5_Q5 -> {
                return (Ydses_c5.Q5.equals(getYHhQuintilesMonthC5L1())) ? 1.0 : 0.0;
            }
            case HHincomeQ2, HHincomeQ2_Mixed, HHincomeQ2_Formal -> {
                return (Ydses_c5.Q2.equals(getYHhQuintilesMonthC5Current())) ? 1.0 : 0.0;
            }
            case HHincomeQ3, HHincomeQ3_Mixed, HHincomeQ3_Formal -> {
                return (Ydses_c5.Q3.equals(getYHhQuintilesMonthC5Current())) ? 1.0 : 0.0;
            }
            case HHincomeQ4, HHincomeQ4_Mixed, HHincomeQ4_Formal -> {
                return (Ydses_c5.Q4.equals(getYHhQuintilesMonthC5Current())) ? 1.0 : 0.0;
            }
            case HHincomeQ5, HHincomeQ5_Mixed, HHincomeQ5_Formal -> {
                return (Ydses_c5.Q5.equals(getYHhQuintilesMonthC5Current())) ? 1.0 : 0.0;
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
            case Ypncp_L1, L_Ypncp -> {
                return yCapitalPersMonthL1;
            }
            case Ypncp_L2 -> {
                return yCapitalPersMonthL2;
            }
            case Ypnoab_L1, L_Ypnoab -> {
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
            case Reached_Retirement_Age_Les -> { //Reached retirement age and was not employed in the previous year
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
                if (benefitUnit.getYPvrtyFlagL1() != null) {
                    return (benefitUnit.getYPvrtyFlagL1() == 0 && benefitUnit.getYPvrtyFlag() == 1) ? 1. : 0.;
                } else return 0.;
            }
            case PovertyToNonPoverty -> {
                if (benefitUnit.getYPvrtyFlagL1() != null) {
                    return (benefitUnit.getYPvrtyFlagL1() == 1 && benefitUnit.getYPvrtyFlag() == 0) ? 1. : 0.;
                } else return 0.;
            }
            case PersistentPoverty -> {
                if (benefitUnit.getYPvrtyFlagL1() != null) {
                    return (benefitUnit.getYPvrtyFlagL1() == 1 && benefitUnit.getYPvrtyFlag() == 1) ? 1. : 0.;
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
                return isReceivesBenefitsFlagUC() && getLabourSupplyWeekly() == Labour.THIRTY_EIGHT ? 1. : 0.;
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
            case L1_log_hourly_wage, Hourly_wage_L1, labWageHrlyLogL1 -> {
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
                return (Education.Low.equals(eduHighestC4) || Education.InEducation.equals(eduHighestC4)) ? demAge : 0.0;
            }
            case Deh_c3_Medium_Dag -> {
                return (Education.Medium.equals(eduHighestC4)) ? demAge : 0.0;
            }
            //UK
            case UKC, UKC_Mixed, UKC_Formal, UKC_ -> {
                return Region.UKC.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKD, UKD_Mixed, UKD_Formal, UKD_ -> {
                return Region.UKD.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKE, UKE_Mixed, UKE_Formal -> {
                return Region.UKE.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKF, UKF_Mixed, UKF_Formal -> {
                return Region.UKF.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKG, UKG_Mixed, UKG_Formal -> {
                return Region.UKG.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKH, UKH_Mixed, UKH_Formal -> {
                return Region.UKH.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKI -> {
                return Region.UKI.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKJ, UKJ_Mixed, UKJ_Formal -> {
                return Region.UKJ.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKK, UKK_Mixed, UKK_Formal -> {
                return Region.UKK.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKL, UKL_Mixed, UKL_Formal, UKL_ -> {
                return Region.UKL.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKM, UKM_Mixed, UKM_Formal, UKM_ -> {
                return Region.UKM.equals(getRegion()) ? 1.0 : 0.0;
            }
            case UKN, UKN_Mixed, UKN_Formal -> {
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
                return getLabourSupplyWeekly().equals(Labour.THIRTY_EIGHT) ? 1. : 0.;
            }
            case Covid19GrossPayMonthly_L1 -> {
                return getCovidYLabGrossL1();
            }
            case Covid19ReceivesSEISS_L1 -> {
                return (getCovidSEISSReceivedFlag().equals(Indicator.True)) ? 1. : 0.;
            }
            case Les_c7_Covid_Furlough_L1 -> {
                return (getLabC7CovidL1().equals(Les_c7_covid.FurloughedFlex) || getLabC7CovidL1().equals(Les_c7_covid.FurloughedFull)) ? 1. : 0.;
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
            case RealWageGrowth, realWageGrowth -> { // Note: the values provided to the wage regression must be rebased to 2015, the default BASE_PRICE_YEAR.
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

            case Age_10, demAge_10 -> {
                return (demAge == 10) ? 1.0 : 0.0;
            }

            case Age_11, demAge_11 -> {
                return (demAge == 11) ? 1.0 : 0.0;
            }

            case Age_12, demAge_12 -> {
                return (demAge == 12) ? 1.0 : 0.0;
            }

            case Age_13, demAge_13 -> {
                return (demAge == 13) ? 1.0 : 0.0;
            }

            case Age_14, demAge_14 -> {
                return (demAge == 14) ? 1.0 : 0.0;
            }

            case Age_15 -> {
                return (demAge == 15) ? 1.0 : 0.0;
            }

            case Age_16 -> {
                return (demAge == 16) ? 1.0 : 0.0;
            }

            case Age_2 -> {
                return (demAge == 2) ? 1.0 : 0.0;
            }

            case Age_3 -> {
                return (demAge == 3) ? 1.0 : 0.0;
            }

            case Age_4 -> {
                return (demAge == 4) ? 1.0 : 0.0;
            }

            case Age_5 -> {
                return (demAge == 5) ? 1.0 : 0.0;
            }

            case Age_6 -> {
                return (demAge == 6) ? 1.0 : 0.0;
            }

            case Age_7 -> {
                return (demAge == 7) ? 1.0 : 0.0;
            }

            case Age_8 -> {
                return (demAge == 8) ? 1.0 : 0.0;
            }

            case Age_9 -> {
                return (demAge == 9) ? 1.0 : 0.0;
            }

            case Dag_c -> {
                return getDoubleValue(Variables.Dag);
            }

            case Ded_Dcpst_Single_L1 -> {
                return getDoubleValue(Variables.Ded_Dcpst_Single);
            }

            case Dhe_1 -> {
                return getDoubleValue(Variables.Dhe);
            }

            case careMarketFormalL1 -> {
                return getDoubleValue(Variables.CareMarketFormal_L1);
            }

            case careMarketFormalPartner -> {
                return getDoubleValue(Variables.CareMarketFormalPartner);
            }

            case careMarketInformalL1 -> {
                return getDoubleValue(Variables.CareMarketInformal_L1);
            }

            case careMarketInformalPartner -> {
                return getDoubleValue(Variables.CareMarketInformalPartner);
            }

            case careMarketMixed -> {
                return getDoubleValue(Variables.CareMarketMixed);
            }

            case careMarketMixedL1 -> {
                return getDoubleValue(Variables.CareMarketMixed_L1);
            }

            case demAge20to24 -> {
                return getDoubleValue(Variables.Age20to24);
            }

            case demAge25to29 -> {
                return getDoubleValue(Variables.Age25to29);
            }

            case demAge30to34 -> {
                return getDoubleValue(Variables.Age30to34);
            }

            case demAge35to39 -> {
                return getDoubleValue(Variables.Age35to39);
            }

            case demAge40to44 -> {
                return getDoubleValue(Variables.Age40to44);
            }

            case demAge45to49 -> {
                return getDoubleValue(Variables.Age45to49);
            }

            case demAge50to54 -> {
                return getDoubleValue(Variables.Age50to54);
            }

            case demAge55to59 -> {
                return getDoubleValue(Variables.Age55to59);
            }

            case demAge60to64 -> {
                return getDoubleValue(Variables.Age60to64);
            }

            case demAge65to69 -> {
                return getDoubleValue(Variables.Age65to69);
            }

            case demAge67to68 -> {
                return getDoubleValue(Variables.Age67to68);
            }

            case demAge69to70 -> {
                return getDoubleValue(Variables.Age69to70);
            }

            case demAge70to74 -> {
                return getDoubleValue(Variables.Age70to74);
            }

            case demAge71to72 -> {
                return getDoubleValue(Variables.Age71to72);
            }

            case demAge73to74 -> {
                return getDoubleValue(Variables.Age73to74);
            }

            case demAge75to76 -> {
                return getDoubleValue(Variables.Age75to76);
            }

            case demAge75to79 -> {
                return getDoubleValue(Variables.Age75to79);
            }

            case demAge77to78 -> {
                return getDoubleValue(Variables.Age77to78);
            }

            case demAge79to80 -> {
                return getDoubleValue(Variables.Age79to80);
            }

            case demAge80to84 -> {
                return getDoubleValue(Variables.Age80to84);
            }

            case demAge81to82 -> {
                return getDoubleValue(Variables.Age81to82);
            }

            case demAge83to84 -> {
                return getDoubleValue(Variables.Age83to84);
            }

            case demAge85plus -> {
                return getDoubleValue(Variables.Age85plus);
            }

            case fertilityRate -> {
                return getDoubleValue(Variables.FertilityRate);
            }

            case need_care -> {
                return getDoubleValue(Variables.NeedCare);
            }

            case receive_care -> {
                return getDoubleValue(Variables.ReceiveCare);
            }

            // -----------------------------------------------------------------------
            // social care age interaction variants
            // -----------------------------------------------------------------------
            case demAge67to68_Formal, demAge67to68_Mixed -> {
                return getDoubleValue(Variables.demAge67to68);
            }
            case demAge69to70_Formal, demAge69to70_Mixed -> {
                return getDoubleValue(Variables.demAge69to70);
            }
            case demAge71to72_Formal, demAge71to72_Mixed -> {
                return getDoubleValue(Variables.demAge71to72);
            }
            case demAge73to74_Formal, demAge73to74_Mixed -> {
                return getDoubleValue(Variables.demAge73to74);
            }
            case demAge75to76_Formal, demAge75to76_Mixed -> {
                return getDoubleValue(Variables.demAge75to76);
            }
            case demAge77to78_Formal, demAge77to78_Mixed -> {
                return getDoubleValue(Variables.demAge77to78);
            }
            case demAge79to80_Formal, demAge79to80_Mixed -> {
                return getDoubleValue(Variables.demAge79to80);
            }
            case demAge81to82_Formal, demAge81to82_Mixed -> {
                return getDoubleValue(Variables.demAge81to82);
            }
            case demAge83to84_Formal, demAge83to84_Mixed -> {
                return getDoubleValue(Variables.demAge83to84);
            }
            case demAge85plus_Formal, demAge85plus_Mixed -> {
                return getDoubleValue(Variables.demAge85plus);
            }

            // -----------------------------------------------------------------------
            // careMarket new-naming interaction variants
            // -----------------------------------------------------------------------
            case careMarketFormalL1_Formal, careMarketFormalL1_Mixed -> {
                return getDoubleValue(Variables.CareMarketFormal_L1);
            }
            case careMarketInformalL1_Formal, careMarketInformalL1_Mixed -> {
                return getDoubleValue(Variables.CareMarketInformal_L1);
            }
            case careMarketMixedL1_Formal, careMarketMixedL1_Mixed -> {
                return getDoubleValue(Variables.CareMarketMixed_L1);
            }

            // -----------------------------------------------------------------------
            // demAge interaction terms
            // -----------------------------------------------------------------------
            case demAge_Fair, demAge_Good, demAge_Low, demAge_Medium, demAge_Poor, demAge_VeryGood -> {
                return (double) demAge;
            }
            case demAgeSq_Fair, demAgeSq_Good, demAgeSq_Low, demAgeSq_Medium, demAgeSq_Poor, demAgeSq_VeryGood -> {
                return (double) demAge * demAge;
            }

            // -----------------------------------------------------------------------
            // demMaleFlag interaction terms
            // -----------------------------------------------------------------------
            case demMaleFlag_Fair, demMaleFlag_Formal, demMaleFlag_Good, demMaleFlag_Low,
                 demMaleFlag_Medium, demMaleFlag_Mixed, demMaleFlag_Poor, demMaleFlag_VeryGood -> {
                return (Gender.Male.equals(demMaleFlag)) ? 1.0 : 0.0;
            }

            // -----------------------------------------------------------------------
            // demCompHhC4 interaction terms
            // -----------------------------------------------------------------------
            case demCompHhC4CoupleChL1_Fair, demCompHhC4CoupleChL1_Good,
                 demCompHhC4CoupleChL1_Poor, demCompHhC4CoupleChL1_VeryGood -> {
                return (Dhhtp_c4.CoupleChildren.equals(getDemCompHhC4L1())) ? 1.0 : 0.0;
            }
            case demCompHhC4SingleNoChL1_Fair, demCompHhC4SingleNoChL1_Good,
                 demCompHhC4SingleNoChL1_Poor, demCompHhC4SingleNoChL1_VeryGood -> {
                return (Dhhtp_c4.SingleNoChildren.equals(getDemCompHhC4L1())) ? 1.0 : 0.0;
            }

            // -----------------------------------------------------------------------
            // demEthnC4 interaction terms
            // -----------------------------------------------------------------------
            case demEthnC4Asian_Fair, demEthnC4Asian_Formal, demEthnC4Asian_Good,
                 demEthnC4Asian_Mixed, demEthnC4Asian_Poor, demEthnC4Asian_VeryGood -> {
                return demEthnC6.equals(Ethnicity.Asian) ? 1. : 0.;
            }
            case demEthnC4Black_Formal, demEthnC4Black_Mixed -> {
                return demEthnC6.equals(Ethnicity.Black) ? 1. : 0.;
            }
            case demEthnC4Other_Formal, demEthnC4Other_Mixed -> {
                return (demEthnC6.equals(Ethnicity.Other) || demEthnC6.equals(Ethnicity.Missing)) ? 1. : 0.;
            }

            // -----------------------------------------------------------------------
            // demPartnerStatus interaction terms
            // -----------------------------------------------------------------------
            case demPartnerStatusPartnered_Formal, demPartnerStatusPartnered_Mixed -> {
                return (Dcpst.Partnered.equals(getDemPartnerStatus())) ? 1.0 : 0.0;
            }

            // -----------------------------------------------------------------------
            // region interaction terms
            // -----------------------------------------------------------------------
            case demRgnUKC_Formal, demRgnUKC_Mixed -> {
                return Region.UKC.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKD_Formal, demRgnUKD_Mixed -> {
                return Region.UKD.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKE_Fair, demRgnUKE_Formal, demRgnUKE_Good, demRgnUKE_Low,
                 demRgnUKE_Medium, demRgnUKE_Mixed, demRgnUKE_Poor, demRgnUKE_VeryGood -> {
                return Region.UKE.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKF_Fair, demRgnUKF_Formal, demRgnUKF_Good,
                 demRgnUKF_Mixed, demRgnUKF_Poor, demRgnUKF_VeryGood -> {
                return Region.UKF.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKG_Fair, demRgnUKG_Formal, demRgnUKG_Good,
                 demRgnUKG_Mixed, demRgnUKG_Poor, demRgnUKG_VeryGood -> {
                return Region.UKG.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKH_Fair, demRgnUKH_Formal, demRgnUKH_Good,
                 demRgnUKH_Mixed, demRgnUKH_Poor, demRgnUKH_VeryGood -> {
                return Region.UKH.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKJ_Fair, demRgnUKJ_Formal, demRgnUKJ_Good,
                 demRgnUKJ_Mixed, demRgnUKJ_Poor, demRgnUKJ_VeryGood -> {
                return Region.UKJ.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKK_Fair, demRgnUKK_Formal, demRgnUKK_Good,
                 demRgnUKK_Mixed, demRgnUKK_Poor, demRgnUKK_VeryGood -> {
                return Region.UKK.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKL_Formal, demRgnUKL_Mixed -> {
                return Region.UKL.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKM_Formal, demRgnUKM_Low, demRgnUKM_Medium, demRgnUKM_Mixed -> {
                return Region.UKM.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKN_Fair, demRgnUKN_Formal, demRgnUKN_Good, demRgnUKN_Low,
                 demRgnUKN_Medium, demRgnUKN_Mixed, demRgnUKN_Poor, demRgnUKN_VeryGood -> {
                return Region.UKN.equals(getRegion()) ? 1.0 : 0.0;
            }

            // -----------------------------------------------------------------------
            // demYear interaction terms
            // -----------------------------------------------------------------------
            case demYear_Fair, demYear_Good, demYear_Low, demYear_Medium, demYear_Poor, demYear_VeryGood -> {
                return (Parameters.isFixTimeTrend && getYear() >= Parameters.timeTrendStopsIn) ? (double) Parameters.timeTrendStopsIn : (double) getYear();
            }
            case demYear2020_Formal, demYear2020_Mixed -> {
                return (getYear() == 2020) ? 1. : 0.;
            }
            case demYear2021_Fair, demYear2021_Formal, demYear2021_Good,
                 demYear2021_Mixed, demYear2021_Poor, demYear2021_VeryGood -> {
                return (getYear() == 2021) ? 1. : 0.;
            }

            // -----------------------------------------------------------------------
            // education interaction terms
            // -----------------------------------------------------------------------
            case eduHighestC4Low_Formal, eduHighestC4Low_Mixed -> {
                return (Education.Low.equals(eduHighestC4)) ? 1.0 : 0.0;
            }
            case eduHighestC4Medium_Formal, eduHighestC4Medium_Mixed -> {
                return (Education.Medium.equals(eduHighestC4)) ? 1.0 : 0.0;
            }
            case eduHighestC4LowL1_Fair, eduHighestC4LowL1_Good, eduHighestC4LowL1_Poor, eduHighestC4LowL1_VeryGood -> {
                return (Education.Low.equals(eduHighestC4L1)) ? 1.0 : 0.0;
            }
            case eduHighestC4MediumL1_Fair, eduHighestC4MediumL1_Good, eduHighestC4MediumL1_Poor, eduHighestC4MediumL1_VeryGood -> {
                return (Education.Medium.equals(eduHighestC4L1)) ? 1.0 : 0.0;
            }
            case eduHighestC4NaL1_Fair, eduHighestC4NaL1_Good, eduHighestC4NaL1_Poor, eduHighestC4NaL1_VeryGood -> {
                return (Education.InEducation.equals(eduHighestC4L1) || Indicator.True.equals(eduSpellFlagL1)) ? 1.0 : 0.0;
            }

            // -----------------------------------------------------------------------
            // health interaction terms
            // -----------------------------------------------------------------------
            case healthDsblLongtermFlagL1_Fair, healthDsblLongtermFlagL1_Good,
                 healthDsblLongtermFlagL1_Poor, healthDsblLongtermFlagL1_VeryGood -> {
                return Indicator.True.equals(healthDsblLongtermFlagL1) ? 1. : 0.;
            }
            case healthMentalMcsL1_Fair, healthMentalMcsL1_Good, healthMentalMcsL1_Poor, healthMentalMcsL1_VeryGood -> {
                return (healthMentalMcsL1 != null && healthMentalMcsL1 >= 0.) ? healthMentalMcsL1 : 0.;
            }
            case healthPhysicalPcsL1_Fair, healthPhysicalPcsL1_Good, healthPhysicalPcsL1_Poor, healthPhysicalPcsL1_VeryGood -> {
                return (healthPhysicalPcsL1 != null && healthPhysicalPcsL1 >= 0.) ? healthPhysicalPcsL1 : 0.;
            }
            case healthSelfRatedExcellent_Formal, healthSelfRatedExcellent_Mixed -> {
                return (Dhe.Excellent.equals(healthSelfRated)) ? 1. : 0.;
            }
            case healthSelfRatedFair_Formal, healthSelfRatedFair_Mixed -> {
                return (Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case healthSelfRatedGood_Formal, healthSelfRatedGood_Mixed -> {
                return (Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case healthSelfRatedVeryGood_Formal, healthSelfRatedVeryGood_Mixed -> {
                return (Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }

            // -----------------------------------------------------------------------
            // labour status interaction terms
            // -----------------------------------------------------------------------
            case labStatusC4EmployedL1_Fair, labStatusC4EmployedL1_Good,
                 labStatusC4EmployedL1_Poor, labStatusC4EmployedL1_VeryGood -> {
                return (Les_c4.EmployedOrSelfEmployed.equals(labC4L1)) ? 1.0 : 0.0;
            }
            case labStatusC4RetiredL1_Fair, labStatusC4RetiredL1_Good,
                 labStatusC4RetiredL1_Poor, labStatusC4RetiredL1_VeryGood -> {
                return (Les_c4.Retired.equals(labC4L1)) ? 1. : 0.;
            }

            // -----------------------------------------------------------------------
            // income quintile new naming
            // -----------------------------------------------------------------------
            case yHhQuintilesMonthC5Q2, yHhQuintilesMonthC5Q2_Formal, yHhQuintilesMonthC5Q2_Mixed -> {
                return (Ydses_c5.Q2.equals(getYHhQuintilesMonthC5Current())) ? 1.0 : 0.0;
            }
            case yHhQuintilesMonthC5Q2L1, yHhQuintilesMonthC5Q2L1_Fair, yHhQuintilesMonthC5Q2L1_Good,
                 yHhQuintilesMonthC5Q2L1_Poor, yHhQuintilesMonthC5Q2L1_VeryGood -> {
                return (Ydses_c5.Q2.equals(getYHhQuintilesMonthC5L1())) ? 1.0 : 0.0;
            }
            case yHhQuintilesMonthC5Q3, yHhQuintilesMonthC5Q3_Formal, yHhQuintilesMonthC5Q3_Mixed -> {
                return (Ydses_c5.Q3.equals(getYHhQuintilesMonthC5Current())) ? 1.0 : 0.0;
            }
            case yHhQuintilesMonthC5Q3L1, yHhQuintilesMonthC5Q3L1_Fair, yHhQuintilesMonthC5Q3L1_Good,
                 yHhQuintilesMonthC5Q3L1_Poor, yHhQuintilesMonthC5Q3L1_VeryGood -> {
                return (Ydses_c5.Q3.equals(getYHhQuintilesMonthC5L1())) ? 1.0 : 0.0;
            }
            case yHhQuintilesMonthC5Q4, yHhQuintilesMonthC5Q4_Formal, yHhQuintilesMonthC5Q4_Mixed -> {
                return (Ydses_c5.Q4.equals(getYHhQuintilesMonthC5Current())) ? 1.0 : 0.0;
            }
            case yHhQuintilesMonthC5Q4L1, yHhQuintilesMonthC5Q4L1_Fair, yHhQuintilesMonthC5Q4L1_Good,
                 yHhQuintilesMonthC5Q4L1_Poor, yHhQuintilesMonthC5Q4L1_VeryGood -> {
                return (Ydses_c5.Q4.equals(getYHhQuintilesMonthC5L1())) ? 1.0 : 0.0;
            }
            case yHhQuintilesMonthC5Q5, yHhQuintilesMonthC5Q5_Formal, yHhQuintilesMonthC5Q5_Mixed -> {
                return (Ydses_c5.Q5.equals(getYHhQuintilesMonthC5Current())) ? 1.0 : 0.0;
            }
            case yHhQuintilesMonthC5Q5L1, yHhQuintilesMonthC5Q5L1_Fair, yHhQuintilesMonthC5Q5L1_Good,
                 yHhQuintilesMonthC5Q5L1_Poor, yHhQuintilesMonthC5Q5L1_VeryGood -> {
                return (Ydses_c5.Q5.equals(getYHhQuintilesMonthC5L1())) ? 1.0 : 0.0;
            }

            // -----------------------------------------------------------------------
            // lowercase / legacy-naming aliases
            // -----------------------------------------------------------------------
            case dag_L1 -> {
                return (double) demAge - 1;
            }
            case dagsq_L1 -> {
                return (double) (demAge - 1) * (demAge - 1);
            }
            case dgn -> {
                return (Gender.Male.equals(demMaleFlag)) ? 1.0 : 0.0;
            }
            case dhh_owned_L1 -> {
                return demPrptyFlagL1 ? 1. : 0.;
            }
            case dlltsd01_L1 -> {
                return Indicator.True.equals(healthDsblLongtermFlagL1) ? 1. : 0.;
            }
            case dnc_L1 -> {
                return (double) getNumberChildrenAllL1();
            }
            case drgn1_L1 -> {
                return (double) getRegion().getValue();
            }
            case econ_benefits_L1 -> {
                return isReceivesBenefitsFlag_L1() ? 1. : 0.;
            }
            case financial_distress_L1, L_FinancialDistress -> {
                return (yFinDstrssFlag != null && yFinDstrssFlag) ? 1. : 0.;
            }
            case labWageHrlyL1 -> {
                return (labWageFullTimeHrlyL1 != null && labWageFullTimeHrlyL1 > 0) ? labWageFullTimeHrlyL1 : 0.;
            }
            case stm -> {
                return (Parameters.isFixTimeTrend && getYear() >= Parameters.timeTrendStopsIn) ? (double) Parameters.timeTrendStopsIn : (double) getYear();
            }
            case wealthPrptyFlagL1 -> {
                return demPrptyFlagL1 ? 1. : 0.;
            }
            case yCapitalPersMonthL1 -> {
                return yCapitalPersMonthL1 != null ? yCapitalPersMonthL1 : 0.;
            }
            case yCapitalPersMonthL2 -> {
                return yCapitalPersMonthL2 != null ? yCapitalPersMonthL2 : 0.;
            }
            case yEmpPersGrossMonthL1 -> {
                return yEmpPersGrossMonthL1 != null ? yEmpPersGrossMonthL1 : 0.;
            }
            case yEmpPersGrossMonthL2 -> {
                return yEmpPersGrossMonthL2 != null ? yEmpPersGrossMonthL2 : 0.;
            }
            case yMiscPersGrossMonthL1 -> {
                return yMiscPersGrossMonthL1 != null ? yMiscPersGrossMonthL1 : 0.;
            }
            case yNonBenPersGrossMonthL1 -> {
                return yNonBenPersGrossMonthL1 != null ? yNonBenPersGrossMonthL1 : 0.;
            }
            case yPensPersGrossMonthL1 -> {
                return yPensPersGrossMonthL1 != null ? yPensPersGrossMonthL1 : 0.;
            }
            case yPensPersGrossMonthL2 -> {
                return yPensPersGrossMonthL2 != null ? yPensPersGrossMonthL2 : 0.;
            }
            case ypncp_L1 -> {
                return yCapitalPersMonthL1 != null ? yCapitalPersMonthL1 : 0.;
            }
            case ypnoab_L1 -> {
                return yPensPersGrossMonthL1 != null ? yPensPersGrossMonthL1 : 0.;
            }
            case yPersAndPartnerGrossDiffMonthL1 -> {
                return yPersAndPartnerGrossDiffMonthL1 != null ? yPersAndPartnerGrossDiffMonthL1 : 0.;
            }

            // -----------------------------------------------------------------------
            // Pre-existing enum values used in estimation files (missing cases)
            // -----------------------------------------------------------------------
            case careHrsFormalIhsL1 -> {
                return FastMath.asinh(getCareHrsFormalWeekL1());
            }
            case careHrsInformalIhsL1 -> {
                return FastMath.asinh(getCareHrsInformalWeekL1());
            }
            case careHrsProvidedWeekIhsL1 -> {
                double hrs = (careHrsProvidedWeekL1 != null && careHrsProvidedWeekL1 > 0.0) ? careHrsProvidedWeekL1 : 0.0;
                return FastMath.asinh(hrs);
            }
            case careMarketMixedPartner -> {
                Person partner = getPartner();
                if (partner == null) return 0.;
                return (partner.getHoursFormalSocialCare() > 0.01 && partner.getCareHrsInformalWeek() > 0.01) ? 1. : 0.;
            }
            case careNeedFlag -> {
                return Indicator.True.equals(careNeedFlag) ? 1. : 0.;
            }
            case careNeedFlagL1 -> {
                return Indicator.True.equals(careNeedFlagL1) ? 1. : 0.;
            }
            case careProvidedFlagL1 -> {
                return Indicator.True.equals(careProvidedFlagL1) ? 1. : 0.;
            }
            case careReceivedFlag -> {
                return (SocialCareReceipt.Formal.equals(careReceivedFlag) || SocialCareReceipt.Informal.equals(careReceivedFlag) || SocialCareReceipt.Mixed.equals(careReceivedFlag)) ? 1. : 0.;
            }
            case careReceivedFlagL1 -> {
                return (getCareHrsTotalWeekL1() > 0.01) ? 1. : 0.;
            }
            case careReceivedPartnerFlag -> {
                Person partner = getPartner();
                if (partner == null) return 0.;
                return (partner.getHoursFormalSocialCare() + partner.getCareHrsInformalWeek() > 0.01) ? 1. : 0.;
            }
            case demAgePartnerDiffL1 -> {
                return (demAgePartnerDiffL1 != null) ? (double) demAgePartnerDiffL1 : 0.0;
            }
            case demCompHhC4CoupleChL1 -> {
                return (Dhhtp_c4.CoupleChildren.equals(getDemCompHhC4L1())) ? 1.0 : 0.0;
            }
            case demCompHhC4L1SingleChL1 -> {
                return (Dhhtp_c4.SingleChildren.equals(getDemCompHhC4L1())) ? 1.0 : 0.0;
            }
            case demCompHhC4SingleNoChL1 -> {
                return (Dhhtp_c4.SingleNoChildren.equals(getDemCompHhC4L1())) ? 1.0 : 0.0;
            }
            case demCompHhC81L1 -> {
                // Couple with no children, spouse employed (baseline category)
                Person partner = getPartner();
                if (partner != null && partner.labC4L1 != null)
                    return (Les_c4.EmployedOrSelfEmployed.equals(partner.labC4L1) && Dhhtp_c4.CoupleNoChildren.equals(getDemCompHhC4L1())) ? 1. : 0.;
                else
                    return 0.;
            }
            case demCompHhC82L1 -> {
                return getDoubleValue(Variables.Dhhtp_c8_2_L1);
            }
            case demCompHhC83L1 -> {
                return getDoubleValue(Variables.Dhhtp_c8_3_L1);
            }
            case demCompHhC84L1 -> {
                return getDoubleValue(Variables.Dhhtp_c8_4_L1);
            }
            case demCompHhC85L1 -> {
                return getDoubleValue(Variables.Dhhtp_c8_5_L1);
            }
            case demCompHhC86L1 -> {
                return getDoubleValue(Variables.Dhhtp_c8_6_L1);
            }
            case demCompHhC87L1 -> {
                return getDoubleValue(Variables.Dhhtp_c8_7_L1);
            }
            case demCompHhC88L1 -> {
                return getDoubleValue(Variables.Dhhtp_c8_8_L1);
            }
            case demEnterPartnerFlagL1 -> {
                if (demPartnerStatusL1 != null && demPartnerStatusL2 != null)
                    return (Dcpst.Partnered.equals(demPartnerStatusL1) && !Dcpst.Partnered.equals(demPartnerStatusL2)) ? 1. : 0.;
                else
                    return 0.;
            }
            case demEthnC4Asian -> {
                return demEthnC6.equals(Ethnicity.Asian) ? 1. : 0.;
            }
            case demEthnC4Black -> {
                return demEthnC6.equals(Ethnicity.Black) ? 1. : 0.;
            }
            case demEthnC4Other -> {
                return (demEthnC6.equals(Ethnicity.Other) || demEthnC6.equals(Ethnicity.Missing)) ? 1. : 0.;
            }
            case demMaleFlag -> {
                return (Gender.Male.equals(demMaleFlag)) ? 1.0 : 0.0;
            }
            case demNChild -> {
                return (double) getNumberChildrenAll();
            }
            case demNChild0to2L1 -> {
                return (double) getNumberChildren02L1();
            }
            case demNChildL1 -> {
                return (double) getNumberChildrenAllL1();
            }
            case demPartnerNYearL1 -> {
                return (demPartnerNYearL1 != null) ? (double) demPartnerNYearL1 : 0.0;
            }
            case demPartnerStatusPartnered -> {
                return (Dcpst.Partnered.equals(getDemPartnerStatus())) ? 1.0 : 0.0;
            }
            case demPartnerStatusPartneredL1 -> {
                return (demPartnerStatusL1 != null && demPartnerStatusL1.equals(Dcpst.Partnered)) ? 1. : 0.;
            }
            case demPartnerStatusSingle -> {
                return (Dcpst.Single.equals(getDemPartnerStatus())) ? 1.0 : 0.0;
            }
            case demPartnerStatusSingleL1 -> {
                return (demPartnerStatusL1 != null && demPartnerStatusL1.equals(Dcpst.Single)) ? 1. : 0.;
            }
            case demPensAgeFlag -> {
                return getDoubleValue(Variables.Reached_Retirement_Age);
            }
            case demPensAgeFlag_NotEmployedL1 -> {
                return (getDoubleValue(Variables.Reached_Retirement_Age) > 0. && Les_c4.NotEmployed.equals(labC4L1)) ? 1. : 0.;
            }
            case demPensPartnerAgeFlag -> {
                return getDoubleValue(Variables.Reached_Retirement_Age_Sp);
            }
            case demRgnUKC -> {
                return Region.UKC.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKD -> {
                return Region.UKD.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKE -> {
                return Region.UKE.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKF -> {
                return Region.UKF.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKG -> {
                return Region.UKG.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKH -> {
                return Region.UKH.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKI -> {
                return Region.UKI.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKJ -> {
                return Region.UKJ.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKK -> {
                return Region.UKK.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKL -> {
                return Region.UKL.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKM -> {
                return Region.UKM.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demRgnUKN -> {
                return Region.UKN.equals(getRegion()) ? 1.0 : 0.0;
            }
            case demYear -> {
                return (Parameters.isFixTimeTrend && getYear() >= Parameters.timeTrendStopsIn) ? (double) Parameters.timeTrendStopsIn : (double) getYear();
            }
            case eduHighestC4High -> {
                return (Education.High.equals(eduHighestC4)) ? 1.0 : 0.0;
            }
            case eduHighestC4HighL1 -> {
                return (Education.High.equals(eduHighestC4L1)) ? 1.0 : 0.0;
            }
            case eduHighestC4HighL1_demAge -> {
                return (Education.High.equals(eduHighestC4L1)) ? demAge : 0.0;
            }
            case eduHighestC4Low -> {
                return (Education.Low.equals(eduHighestC4)) ? 1.0 : 0.0;
            }
            case eduHighestC4LowL1 -> {
                return (Education.Low.equals(eduHighestC4L1)) ? 1.0 : 0.0;
            }
            case eduHighestC4Low_demAge -> {
                return (Education.Low.equals(eduHighestC4)) ? demAge : 0.0;
            }
            case eduHighestC4LowL1_demAge -> {
                return (Education.Low.equals(eduHighestC4L1)) ? demAge : 0.0;
            }
            case eduHighestC4Medium -> {
                return (Education.Medium.equals(eduHighestC4)) ? 1.0 : 0.0;
            }
            case eduHighestC4MediumL1 -> {
                return (Education.Medium.equals(eduHighestC4L1)) ? 1.0 : 0.0;
            }
            case eduHighestC4Medium_demAge -> {
                return (Education.Medium.equals(eduHighestC4)) ? demAge : 0.0;
            }
            case eduHighestC4MediumL1_demAge -> {
                return (Education.Medium.equals(eduHighestC4L1)) ? demAge : 0.0;
            }
            case eduHighestC4Na -> {
                return (Education.InEducation.equals(eduHighestC4) || Indicator.True.equals(eduSpellFlag)) ? 1.0 : 0.0;
            }
            case eduHighestC4NaL1 -> {
                return (Education.InEducation.equals(eduHighestC4L1) || Indicator.True.equals(eduSpellFlagL1)) ? 1.0 : 0.0;
            }
            case eduHighestParentC3High -> {
                return checkHighestParentalEducationEquals(Education.High) ? 1.0 : 0.0;
            }
            case eduHighestParentC3Medium -> {
                return checkHighestParentalEducationEquals(Education.Medium) ? 1.0 : 0.0;
            }
            case eduHighestParentC3Low -> {
                return (checkHighestParentalEducationEquals(Education.Low) || checkHighestParentalEducationEquals(Education.InEducation)) ? 1.0 : 0.0;
            }
            case eduHighestParentC3LowL1 -> {
                return (checkHighestParentalEducationEquals(Education.Low) || checkHighestParentalEducationEquals(Education.InEducation)) ? 1.0 : 0.0;
            }
            case eduHighestParentC3MediumL1 -> {
                return checkHighestParentalEducationEquals(Education.Medium) ? 1.0 : 0.0;
            }
            case eduHighestPartnerC3LowL1 -> {
                return (Education.Low.equals(eduHighestPartnerC4L1) || Education.InEducation.equals(eduHighestPartnerC4L1)) ? 1. : 0.;
            }
            case eduHighestPartnerC3MediumL1 -> {
                return Education.Medium.equals(eduHighestPartnerC4L1) ? 1. : 0.;
            }
            case eduSampleFlag -> {
                return Indicator.True.equals(eduSpellFlag) ? 1.0 : 0.0;
            }
            case eduSampleFlagL1 -> {
                return Indicator.True.equals(eduSpellFlagL1) ? 1.0 : 0.0;
            }
            case eduSampleFlag_Male -> {
                return (Indicator.True.equals(eduSpellFlag) && Gender.Male.equals(demMaleFlag)) ? 1.0 : 0.0;
            }
            case eduSampleFlag_McsL1 -> {
                return Indicator.True.equals(eduSpellFlag) ? getDoubleValue(Variables.Dhe_mcs_L1) : 0.0;
            }
            case eduSampleFlag_PcsL1 -> {
                return Indicator.True.equals(eduSpellFlag) ? getDoubleValue(Variables.Dhe_pcs_L1) : 0.0;
            }
            case eduSampleFlag_Q2L1 -> {
                return Indicator.True.equals(eduSpellFlag) ? getDoubleValue(Variables.Ydses_c5_Q2_L1) : 0.0;
            }
            case eduSampleFlag_Q3L1 -> {
                return Indicator.True.equals(eduSpellFlag) ? getDoubleValue(Variables.Ydses_c5_Q3_L1) : 0.0;
            }
            case eduSampleFlag_Q4L1 -> {
                return Indicator.True.equals(eduSpellFlag) ? getDoubleValue(Variables.Ydses_c5_Q4_L1) : 0.0;
            }
            case eduSampleFlag_Q5L1 -> {
                return Indicator.True.equals(eduSpellFlag) ? getDoubleValue(Variables.Ydses_c5_Q5_L1) : 0.0;
            }
            case eduSampleFlag_Single -> {
                return (Indicator.True.equals(eduSpellFlag) && Dcpst.Single.equals(getDemPartnerStatus())) ? 1.0 : 0.0;
            }
            case eduSampleFlag_demMaleFlag -> {
                return Indicator.True.equals(eduSpellFlag) ? getDoubleValue(Variables.demMaleFlag) : 0.0;
            }
            case eduSampleFlag_demNChild0to2L1 -> {
                return Indicator.True.equals(eduSpellFlag) ? (double) getNumberChildren02L1() : 0.0;
            }
            case eduSampleFlag_demNChildL1 -> {
                return Indicator.True.equals(eduSpellFlag) ? (double) getNumberChildrenAllL1() : 0.0;
            }
            case eduSampleFlag_yCapitalPersL1 -> {
                return Indicator.True.equals(eduSpellFlag) ? getDoubleValue(Variables.Ypncp_L1) : 0.0;
            }
            case eduSampleFlag_yCapitalPersL2 -> {
                return Indicator.True.equals(eduSpellFlag) ? getDoubleValue(Variables.Ypncp_L2) : 0.0;
            }
            case exp_incchange -> {
                return 0.; // approximate - income change transition not directly stored
            }
            case exp_poverty -> {
                return 0.; // approximate - poverty transition code not directly stored
            }
            case healthDsblLongtermFlag -> {
                return Indicator.True.equals(healthDsblLongtermFlag) ? 1. : 0.;
            }
            case healthDsblLongtermFlagL1 -> {
                return Indicator.True.equals(healthDsblLongtermFlagL1) ? 1. : 0.;
            }
            case healthMentalMcsL1 -> {
                return (healthMentalMcsL1 != null && healthMentalMcsL1 >= 0.) ? healthMentalMcsL1 : 0.;
            }
            case healthMentalPartnerMcsL1 -> {
                Person partner = getPartner();
                return (partner != null && partner.getHealthMentalMcsL1() != null) ? partner.getHealthMentalMcsL1() : 0.;
            }
            case healthPartnerSelfRatedExcellent, healthPartnerSelfRatedExcel -> {
                Person partner = getPartner();
                return (partner != null && Dhe.Excellent.equals(partner.getHealthSelfRated())) ? 1. : 0.;
            }
            case healthPartnerSelfRatedFair -> {
                return getDoubleValue(Variables.Dhesp_Fair);
            }
            case healthPartnerSelfRatedGood -> {
                return getDoubleValue(Variables.Dhesp_Good);
            }
            case healthPartnerSelfRatedVeryGood -> {
                return getDoubleValue(Variables.Dhesp_VeryGood);
            }
            case healthPhysicalPartnerPcsL1 -> {
                Person partner = getPartner();
                return (partner != null && partner.getHealthPhysicalPcsL1() != null) ? partner.getHealthPhysicalPcsL1() : 0.;
            }
            case healthPhysicalPcsL1 -> {
                return (healthPhysicalPcsL1 != null && healthPhysicalPcsL1 >= 0.) ? healthPhysicalPcsL1 : 0.;
            }
            case healthSelfRatedExcellent -> {
                return (Dhe.Excellent.equals(healthSelfRated)) ? 1. : 0.;
            }
            case healthSelfRatedExcellentL1 -> {
                return (Dhe.Excellent.equals(healthSelfRatedL1)) ? 1. : 0.;
            }
            case healthSelfRatedFair -> {
                return (Dhe.Fair.equals(healthSelfRated)) ? 1. : 0.;
            }
            case healthSelfRatedFairL1 -> {
                return (Dhe.Fair.equals(healthSelfRatedL1)) ? 1. : 0.;
            }
            case healthSelfRatedGood -> {
                return (Dhe.Good.equals(healthSelfRated)) ? 1. : 0.;
            }
            case healthSelfRatedGoodL1 -> {
                return (Dhe.Good.equals(healthSelfRatedL1)) ? 1. : 0.;
            }
            case healthSelfRatedVeryGood -> {
                return (Dhe.VeryGood.equals(healthSelfRated)) ? 1. : 0.;
            }
            case healthSelfRatedVeryGoodL1 -> {
                return (Dhe.VeryGood.equals(healthSelfRatedL1)) ? 1. : 0.;
            }
            case healthSelfRatedPoor -> {
                return (Dhe.Poor.equals(healthSelfRated)) ? 1. : 0.;
            }
            case labPt -> {
                return (getLabourSupplyHoursWeekly() > 0 && getLabourSupplyHoursWeekly() < Parameters.MIN_HOURS_FULL_TIME_EMPLOYED) ? 1. : 0.;
            }
            case labStatusC3NotEmployedL1 -> {
                return (Les_c4.NotEmployed.equals(labC4L1) || Les_c4.Retired.equals(labC4L1)) ? 1. : 0.;
            }
            case labStatusC3StudentL1 -> {
                return Les_c4.Student.equals(labC4L1) ? 1. : 0.;
            }
            case labStatusC4EmployedL1 -> {
                return (Les_c4.EmployedOrSelfEmployed.equals(labC4L1)) ? 1.0 : 0.0;
            }
            case labStatusC4EmployedL1_Male -> {
                return (Les_c4.EmployedOrSelfEmployed.equals(labC4L1) && Gender.Male.equals(demMaleFlag)) ? 1.0 : 0.0;
            }
            case labStatusC4NotEmployedL1 -> {
                return (Les_c4.NotEmployed.equals(labC4L1)) ? 1.0 : 0.0;
            }
            case labStatusC4NotEmployedL1_Male, labStatusC4NotEmployed_MaleL1 -> {
                return (Les_c4.NotEmployed.equals(labC4L1) && Gender.Male.equals(demMaleFlag)) ? 1.0 : 0.0;
            }
            case labStatusC4RetiredL1 -> {
                return (Les_c4.Retired.equals(labC4L1)) ? 1.0 : 0.0;
            }
            case labStatusC4RetiredL1_Male -> {
                return (Les_c4.Retired.equals(labC4L1) && Gender.Male.equals(demMaleFlag)) ? 1.0 : 0.0;
            }
            case labStatusC4StudentL1 -> {
                return (Les_c4.Student.equals(labC4L1)) ? 1.0 : 0.0;
            }
            case labStatusC4StudentL1_Male, labStatusC4Student_MaleL1 -> {
                return (Les_c4.Student.equals(labC4L1) && Gender.Male.equals(demMaleFlag)) ? 1.0 : 0.0;
            }
            case labStatusC4Retired_MaleL1 -> {
                return (Les_c4.Retired.equals(labC4L1) && Gender.Male.equals(demMaleFlag)) ? 1.0 : 0.0;
            }
            case labStatusPartnerAndOwnC42L1 -> {
                return (labStatusPartnerAndOwnC4L1 != null && Lesdf_c4.EmployedSpouseNotEmployed.equals(labStatusPartnerAndOwnC4L1)) ? 1. : 0.;
            }
            case labStatusPartnerAndOwnC43L1 -> {
                return (labStatusPartnerAndOwnC4L1 != null && Lesdf_c4.NotEmployedSpouseEmployed.equals(labStatusPartnerAndOwnC4L1)) ? 1. : 0.;
            }
            case labStatusPartnerAndOwnC44L1 -> {
                return (labStatusPartnerAndOwnC4L1 != null && Lesdf_c4.BothNotEmployed.equals(labStatusPartnerAndOwnC4L1)) ? 1. : 0.;
            }

            default -> {
                throw new IllegalArgumentException("Unsupported regressor " + variableID.name() + " in Person#getDoubleValue");
            }
        }
    }


    //	Override equals and hashCode to make unique BenefitUnit determined by Key.getId()
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

    @PrePersist
    @PreUpdate
    private void syncDemAgeSq() {
        demAgeSq = demAge * demAge;
    }

     public int getPersonCount() {
        return 1;
     }

    public int getDemAge() {
        return demAge;
    }

    public int getDemAgeSq() {
        demAgeSq = demAge * demAge;
        return demAgeSq;
    }

    public void setDemAge(Integer demAge) {
        this.demAge = demAge;
        demAgeSq = demAge * demAge;
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

    public Les_c4 getLabC4() {
        return labC4;
    }

    public int getStudent() {
        return Les_c4.Student.equals(labC4)? 1 : 0;
    }

    public int getEducation() {

        if (eduHighestC4 == Education.Medium) return 1;
        else if (eduHighestC4 == Education.High) return (int)(DecisionParams.PTS_EDUCATION - 1.0);
        else return 0;
    }

    public void setLabC4(Les_c4 labC4) {
        this.labC4 = labC4;
    }

    public void setLabC7Covid(Les_c7_covid labC7Covid) { this.labC7Covid = labC7Covid; }

    public Les_c7_covid getLabC7Covid() { return labC7Covid; }

    public Les_c4 getLabC4L1() {
        return labC4L1;
    }

    public Les_c7_covid getLabC7CovidL1() { return labC7CovidL1; }

    public void setLabC7CovidL1(Les_c7_covid labC7CovidL1) {
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

    public Education getEduHighestC4() {
        return eduHighestC4;
    }

    public void setEduHighestC4(Education eduHighestC4) {
         this.eduHighestC4 = eduHighestC4;
     }

    public void setEduHighestC4L1(Education eduHighestC4L1) {
         this.eduHighestC4L1 = eduHighestC4L1;
     }

    public Education getEduHighestMotherC4() {
        return eduHighestMotherC4;
    }

    public void setEduHighestMotherC4(Education eduHighestMotherC4) {
        this.eduHighestMotherC4 = eduHighestMotherC4;
    }

    public Education getEduHighestFatherC4() {
        return eduHighestFatherC4;
    }

    public void setEduHighestFatherC4(Education eduHighestFatherC4) {
        this.eduHighestFatherC4 = eduHighestFatherC4;
    }

    public Indicator getEduSpellFlag() {
        return eduSpellFlag;
    }

    public Indicator getEduSpellFlagL1() {
        return eduSpellFlagL1;
    }

    public void setEduSpellFlag(Indicator eduSpellFlag) {
        this.eduSpellFlag = eduSpellFlag;
    }

    public void setEduSpellFlagL1(Indicator eduSpellFlagL1) {
        this.eduSpellFlagL1 = eduSpellFlagL1;
    }


    public int getLowEducation() {
        if(eduHighestC4 != null) {
            if (eduHighestC4.equals(Education.Low)) return 1;
            else return 0;
        }
        else {
            return 0;
        }
    }

    public int getMidEducation() {
        if(eduHighestC4 != null) {
            if (eduHighestC4.equals(Education.Medium)) return 1;
            else return 0;
        }
        else {
            return 0;
        }
    }

    public int getHighEducation() {
        if(eduHighestC4 != null) {
            if (eduHighestC4.equals(Education.High)) return 1;
            else return 0;
        }
        else {
            return 0;
        }
    }

    public void setEducation(Education educationlevel) {
        this.eduHighestC4 = educationlevel;
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

    public int getEmployedFlagL1() {
        return (Les_c4.EmployedOrSelfEmployed.equals(labC4L1)) ? 1 : 0;
    }

    public int getNonworkFlagL1() {
        return (Les_c4.NotEmployed.equals(labC4L1)) ? 1 : 0;
    }

    public void setI_demRgn(Region region) {
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

    public HouseholdStatus getDemStatusHhL1() {
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
        return Objects.requireNonNullElse(eduLeaveSchoolFlag,false);
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
        if (!Les_c4.Student.equals(getLabC4()) && !Les_c4.Retired.equals(getLabC4())) {
            if (labHrsWorkWeek > 0) {
                setLabC4(Les_c4.EmployedOrSelfEmployed);
            } else  {
                setLabC4(Les_c4.NotEmployed);
            }
        }
    }

    public double getLabourSupplyHoursYearly() {
        return (double) getLabourSupplyHoursWeekly() * Parameters.WEEKS_PER_YEAR;
    }

    public double getScaledLabourSupplyYearly() {
        return getLabourSupplyHoursYearly() * model.getScalingFactor();
    }

    public double getGrossEarningsYearly() {
        return getEarningsWeekly() * Parameters.WEEKS_PER_YEAR;
    }

    public int getAtRiskOfPoverty() {
        return benefitUnit.getYPvrtyFlag();
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

    public Dhe getHealthSelfRated() {
        return healthSelfRated;
    }

    public void setHealthSelfRated(Dhe health) {
        this.healthSelfRated = health;
    }

    public double getHealthSelfRatedValue() {
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
    public Double getDemLifeSatScore0to10() {
        Double val;
        if (demLifeSatScore0to10 == null) {
            val = -1.;
        } else {
            val = demLifeSatScore0to10;
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
            careHrsInformalWeek = 10.0;
            careFromInformalFlag = true;
        } else if (SocialCareReceiptState.Mixed.equals(state)) {
            careNeedFlag = Indicator.True;
            careReceivedFlag = SocialCareReceipt.Mixed;
            careHrsInformalWeek = 10.0;
            careHrsFormalWeek = 10.0;
            careFormalX = 100.0;
            careFormalFlag = true;
            careFromInformalFlag = true;
        } else if (SocialCareReceiptState.Formal.equals(state)) {
            careNeedFlag = Indicator.True;
            careReceivedFlag = SocialCareReceipt.Formal;
            careHrsFormalWeek = 10.0;
            careFormalX = 100.0;
            careFormalFlag = true;
        }
    }

    public void populateSocialCareReceipt_lag1(SocialCareReceiptState state) {
        if (SocialCareReceiptState.NoFormal.equals(state)) {
            careNeedFlagL1 = Indicator.True;
            careHrsInformalWeekL1 = 10.0;
        } else if (SocialCareReceiptState.Mixed.equals(state)) {
            careNeedFlagL1 = Indicator.True;
            careHrsInformalWeekL1 = 10.0;
            careHrsFormalWeekL1 = 10.0;
        } else if (SocialCareReceiptState.Formal.equals(state)) {
            careNeedFlagL1 = Indicator.True;
            careHrsFormalWeekL1 = 10.0;
        }
    }

    public void setCareFromInformalFlag(boolean val) {
        careFromInformalFlag = val;
    }

    public void setCareHrsInformalWeekL1(double val) {
        careHrsInformalWeekL1 = val;
    }

    public void setCareHrsFormalWeekL1(double val) {
        careHrsFormalWeekL1 = val;
    }
    public void setCareProvidedFlagL1(Indicator careProvision) {
        careProvidedFlagL1 = careProvision;
    }

    public void setHealthWbScore0to36(Double healthWbScore0to36) {
        this.healthWbScore0to36 = healthWbScore0to36;
    }

    public void setHealthSelfRatedL1(Dhe health) {
        this.healthSelfRatedL1 = health;
    }

    public void setHealthWbScore0to36L1(Double dhm) {
        this.healthWbScore0to36L1 = dhm;
    }

    public Double getHealthPsyDstrss0to12() {
        return healthPsyDstrss0to12;
    }

    public void setHealthPsyDstrss0to12(Double dhm_ghq) {
        this.healthPsyDstrss0to12 = dhm_ghq;
    }

    public int isPsychologicallyDistressed() {
        return (healthPsyDstrss0to12 >= PSYCHOLOGICAL_DISTRESS_GHQ12_CASES_CUTOFF) ? 1 : 0;
    }

    public Ethnicity getDemEthnC6() {
        return demEthnC6;
    }

    public void setDemEthnC6(Ethnicity demEthnC6) {
        this.demEthnC6 = demEthnC6;
    }

    public boolean getYFinDstrssFlag() {
        return yFinDstrssFlag;
    }

    public Indicator getCareNeedFlag() {
        return careNeedFlag;
    }

    public void setCareNeedFlag(Indicator careNeedFlag) {
        this.careNeedFlag = careNeedFlag;
    }

    public void setEduReturnFlag(Indicator eduReturnFlag) {
        this.eduReturnFlag = eduReturnFlag;
    }

    public Indicator getEduReturnFlag() {return eduReturnFlag;}

    public Indicator getEduExitSampleFlag() {return eduExitSampleFlag;}

    public Long getIdPersOriginal() {
        return idPersOriginal;
    }

    public Long getIdBuOriginal() {
        return idBuOriginal;
    }

    public Long getIdHhOriginal() {
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

    public Dcpst getDemPartnerStatus() {
        if (benefitUnit==null) {
            if (i_demPartnerStatus ==null)
                throw new RuntimeException("attempt to access unassigned value for dcpstLocal");
            return i_demPartnerStatus;
        }
        if (getPartner()!=null)
            return Dcpst.Partnered;
        return Dcpst.Single;
    }

    public void setI_demPartnerStatus(Dcpst demPartnerStatus) {
        this.i_demPartnerStatus = demPartnerStatus;
    }

    public Indicator getHealthDsblLongtermFlag() {
        return healthDsblLongtermFlag;
    }
    public void setHealthDsblLongtermFlag(Indicator healthDsblLongtermFlag) {
        this.healthDsblLongtermFlag = healthDsblLongtermFlag;
    }

    public void setSocialCareReceipt(SocialCareReceipt who) {
        careReceivedFlag = who;
    }

    public void setSocialCareProvision(Indicator provide) {
        careProvidedFlag = provide;
    }

    public Indicator getHealthDsblLongtermFlagL1() {
        return healthDsblLongtermFlagL1;
    }

    public void setHealthDsblLongtermFlagL1(Indicator healthDsblLongtermFlagL1) {
        this.healthDsblLongtermFlagL1 = healthDsblLongtermFlagL1;
    }

    public void setEduExitSampleFlag(Indicator eduExitSampleFlag) {
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

    public Integer getDemAgePartnerDiff() {
        Person partner = getPartner();
        if (partner!=null)
            return (demAge - partner.demAge);
        else
            return null;
    }

    public Double getYNonBenPersGrossMonth() {
        return yNonBenPersGrossMonth;
    }

    public void setYNonBenPersGrossMonth(Double val) {
        yNonBenPersGrossMonth = val;
    }

    public Double getYNonBenPersGrossMonthL1() {
        return yNonBenPersGrossMonthL1;
    }

    public Double getYMiscPersGrossMonth() {
        return yMiscPersGrossMonth;
    }

    public Double getYCapitalPersMonth() {
        return yCapitalPersMonth;
    }

    public Double getYPensPersGrossMonth() {
        return yPensPersGrossMonth;
    }

    public void setYMiscPersGrossMonth(double yMiscPersGrossMonth) {
        this.yMiscPersGrossMonth = yMiscPersGrossMonth;
        if (!Parameters.isFinite(this.yMiscPersGrossMonth))
            throw new IllegalArgumentException("yMiscPersGrossMonth is not finite");
    }

    public Double getYMiscPersGrossMonthL1() {
        return yMiscPersGrossMonthL1;
    }

    public double getYEmpPersGrossMonth() {
        return (yEmpPersGrossMonth !=null) ? yEmpPersGrossMonth : 0.0;
    }

    public void setYEmpPersGrossMonth(double val) {
        yEmpPersGrossMonth = val;
    }

    public Double getWealthPensValue() {
        return getWealthPensValue(true);
    }

    public double getWealthPensValue(boolean throwError) {
        if (!Parameters.isFinite(wealthPensValue)) {
            if (throwError)
                throw new IllegalArgumentException("pensionWealthValue is not finite");
            else
                return 0.0;
        }
        return wealthPensValue;
    }

    public void setWealthPensValue(Double v) {
        wealthPensValue = v;
    }

    public double getYEmpPersGrossMonthL1() {
        return yEmpPersGrossMonthL1;
    }

    public double getYEmpPersGrossMonthL2() {
        return yEmpPersGrossMonthL2;
    }

    public double getYEmpPersGrossMonthL3() {
        return yEmpPersGrossMonthL3;
    }

    public Double getYPersAndPartnerGrossDiffMonthL1() {
        return yPersAndPartnerGrossDiffMonthL1;
    }

    public Lesdf_c4 getLabStatusPartnerAndOwnC4() {
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

    public Lesdf_c4 getLabStatusPartnerAndOwnC4L1() {
        return labStatusPartnerAndOwnC4L1;
    }

    public void setLabC4L1(Les_c4 labC4L1) {
        this.labC4L1 = labC4L1;
    }

    public void setLabStatusPartnerAndOwnC4L1(Lesdf_c4 labStatusPartnerAndOwnC4L1) {
        this.labStatusPartnerAndOwnC4L1 = labStatusPartnerAndOwnC4L1;
    }

    public void setYNonBenPersGrossMonthL1(Double val) {
        yNonBenPersGrossMonthL1 = val;
    }

    public void setEduHighestPartnerC4L1(Education eduHighestPartnerC4L1) {
        this.eduHighestPartnerC4L1 = eduHighestPartnerC4L1;
    }

    public void setHealthPartnerSelfRatedL1(Dhe healthPartnerSelfRatedL1) {
        this.healthPartnerSelfRatedL1 = healthPartnerSelfRatedL1;
    }

    public void setYPersAndPartnerGrossDiffMonthL1(Double val) {
        yPersAndPartnerGrossDiffMonthL1 = val;
    }

    public void setDemPartnerNYearL1(Integer demPartnerNYearL1) {
        this.demPartnerNYearL1 = demPartnerNYearL1;
    }

    public void setDemAgePartnerDiffL1(Integer demAgePartnerDiffL1) {
        this.demAgePartnerDiffL1 = demAgePartnerDiffL1;
    }

    public void setDemPartnerStatusL1(Dcpst demPartnerStatusL1) {
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

    public Double getXEquivYear() {
        return xEquivYear;
    }

    public void setXEquivYear(Double xEquivYear) {
        this.xEquivYear = xEquivYear;
    }

    public Double getXEquivYearL1() { return xEquivYearL1; }

    public void setXEquivYearL1(Double val) {
        xEquivYearL1 = val;
    }

    public Integer getLabHrsWorkNewL1() {
        return labHrsWorkNewL1;
    }

    public void setLabHrsWorkNewL1(Integer labHrsWorkNewL1) {
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

    public double getCovidYLabGross() {
        return (covidYLabGross !=null) ? covidYLabGross : 0.0;
    }

    public void setCovidYLabGross(double covidYLabGross) {
        this.covidYLabGross = covidYLabGross;
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

    public void setYBenReceivedFlagL1(boolean yBenReceivedFlagL1) {
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

    public void setYBenUCReceivedFlagL1(boolean yBenUCReceivedFlagL1) {
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

    public void setYBenNonUCReceivedFlagL1(boolean yBenNonUCReceivedFlagL1) {
        this.yBenNonUCReceivedFlagL1 = yBenNonUCReceivedFlagL1;
    }


    public double getEquivalisedDisposableIncomeYearly() {
        return benefitUnit.getEquivalisedDisposableIncomeYearly();
    }

    public double getDisposableIncomeMonthlyNoNull() { return benefitUnit.getDisposableIncomeMonthlyNoNull();}

    public double getDisposableIncomeMonthly() { return benefitUnit.getDisposableIncomeMonthly();}

    public double getWageOffer() {
        if (labWageOfferLowFlag)
            return 0.0;
        else
            return 1.0;
    }

    public int getDisability() {
        return (Indicator.True.equals(getHealthDsblLongtermFlag())) ? 1 : 0;
    }

    public SocialCareReceipt getSocialCareReceipt() {
        // market = 0 for no social care
        //          1 for only informal care
        //          2 for informal and formal care
        //          3 for only formal care
        if (getHoursFormalSocialCare()<0.01 && getCareHrsInformalWeek()<0.01)
            return SocialCareReceipt.None;
        else if (getHoursFormalSocialCare()<0.01 && getCareHrsInformalWeek()>0.01)
            return SocialCareReceipt.Informal;
        else if (getHoursFormalSocialCare()>0.01 && getCareHrsInformalWeek()>0.01)
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
        else if (getCareHrsInformalWeek()<0.01)
            return SocialCareReceiptState.Formal;
        else return SocialCareReceiptState.Mixed;
    }

    public double getSocialCareProvisionState() {
//        double val = getSocialCareProvision().getValue();
//        if (demAge>DecisionParams.MAX_AGE_COHABITATION && val > 0.1 && val < 2.1)
//            val = 3.0;
        return (double)getSocialCareProvision().getValue();
    }

    public Indicator getSocialCareProvision() {
        return Objects.requireNonNullElse(careProvidedFlag, Indicator.False);
    }

    public double getRetired() {
        return (Les_c4.Retired.equals(getLabC4())) ? 1.0 : 0.0;
    }

    public void setI_demYear(Integer i_demYear) {
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

    public void setI_demNchild0to17(Integer nbr) {
        i_demNchild0to17 = nbr;
    }
    public void setI_demNChild0to2(Indicator idctr) {
        i_demNChild0to2 = idctr;
    }

    private Ydses_c5 getYHhQuintilesMonthC5L1() {
        if (model!=null) {
            if (benefitUnit==null)
                throw new RuntimeException("attempt to access unassigned benefit unit");
            return benefitUnit.getYHhQuintilesMonthC5L1();
        } else {
            if (i_yHhQuintilesC5 ==null)
                throw new RuntimeException("attempt to access unassigned ydses_c5_lag1Local");
            return i_yHhQuintilesC5;
        }
    }

    private Ydses_c5 getYHhQuintilesMonthC5Current() {
        if (model != null) {
            if (benefitUnit == null)
                throw new RuntimeException("attempt to access unassigned benefit unit");
            return benefitUnit.getYHhQuintilesMonthC5();
        } else {
            if (i_yHhQuintilesC5 == null)
                throw new RuntimeException("attempt to access unassigned ydses_c5Local");
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

    private Integer getNumberChildrenAllL1() {
        if (benefitUnit != null) {
            return (benefitUnit.getNumberChildrenAllL1() != null) ? benefitUnit.getNumberChildrenAllL1() : 0;
        } else {
            return (i_demNchildL1 ==null) ? 0 : i_demNchildL1;
        }
    }

    public void setI_demNchild(Integer nbr) {
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

    public void setI_demNchildL1(Integer nbr) {
        i_demNchildL1 = nbr;
    }

    public void setI_demNchild0to2L1(Integer nbr) {
        i_demNchild0to2L1 = nbr;
    }

    private Integer getNumberChildren02L1() {
        if (model != null) {
            if (benefitUnit==null)
                throw new RuntimeException("attempt to access unassigned benefit unit");
            return (benefitUnit.getNumberChildren02L1() != null) ? benefitUnit.getNumberChildren02L1() : 0;
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
                score = Parameters.getRegEmploymentSelectionMaleE().getScore(this, Variables.class);
            } else {
                score = Parameters.getRegEmploymentSelectionMaleNE().getScore(this, Variables.class);
            }
        } else {
            // for females
            if (Les_c4.EmployedOrSelfEmployed.equals(labC4L1)) {
                score = Parameters.getRegEmploymentSelectionFemaleE().getScore(this, Variables.class);
            } else {
                score = Parameters.getRegEmploymentSelectionFemaleNE().getScore(this, Variables.class);
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
            log.debug("inverse Mills ratio is not finite, return 0 instead!!!   IMR: " + inverseMillsRatio + ", score: " + score/* + ", num: " + num + ", denom: " + denom*/ + ", age: " + demAge + ", gender: " + demMaleFlag + ", education " + eduHighestC4 + ", activity_status from previous time-step " + labC4);
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
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionName.WagesMalesE, "labPt");
                } else {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionName.WagesFemalesE, "labPt");
                }
            } else {
                if (Gender.Male.equals(demMaleFlag)) {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionName.WagesMalesNE, "labPt");
                } else {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionName.WagesFemalesNE, "labPt");
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

    public double getCareHrsFormalWeekL1() {
        return (careHrsFormalWeekL1 > 0.0) ? careHrsFormalWeekL1 : 0.0;
    }

    public double getHoursFormalSocialCare() {
        double hours = 0.0;
        if (careHrsFormalWeek !=null)
            if (careHrsFormalWeek >0.0)
                hours = careHrsFormalWeek;
        return hours;
    }

    public double getCareHrsInformalWeek() {
        return (Parameters.isFinite(careHrsInformalWeek) && careHrsInformalWeek > 0.0) ? careHrsInformalWeek : 0.0;
    }

    public double getCareHoursFromPartnerWeekly() {
        if (getPartner() != null) {
            return getCareHrsInformalWeek();
        }
        return 0.0;
    }
    public void setCareHoursFromPartnerWeekly(double hours) {
        careHrsInformalWeek = hours;
    }

    public double getCareHoursFromParentWeekly() {
        return 0.0;
    }

    public double getCareHoursFromDaughterWeekly() {
        return 0.0;
    }

    public double getCareHoursFromSonWeekly() {
        return 0.0;
    }

    public double getCareHoursFromOtherWeekly() {
        if (getPartner() == null) {
            return getCareHrsInformalWeek();
        }
        return 0.0;
    }

    public double getCareHoursProvidedWeekly() {
        double hours = 0.0;
        if (Parameters.isFinite(careHrsProvidedWeek) && careHrsProvidedWeek > 0.0)
            hours = careHrsProvidedWeek;
        return hours;
    }

    public double getCareHrsInformalWeekL1() {
        return (careHrsInformalWeekL1 > 0.0) ? careHrsInformalWeekL1 : 0.0;
    }

    public double getCareHrsTotalWeekL1() {
        return getCareHrsFormalWeekL1() + getCareHrsInformalWeekL1();
    }

    public double getCareHoursFromParent_L1() {
        return 0.0;
    }

    public double getCareHoursFromPartner_L1() {
        return (Dcpst.Partnered.equals(demPartnerStatusL1) && careHrsInformalWeekL1 > 0.0) ? careHrsInformalWeekL1 : 0.0;
    }

    public double getCareHoursFromDaughter_L1() {
        return 0.0;
    }

    public double getCareHoursFromSon_L1() {
        return 0.0;
    }

    public double getCareHoursFromOther_L1() {
        return (!Dcpst.Partnered.equals(demPartnerStatusL1) && careHrsInformalWeekL1 > 0.0) ? careHrsInformalWeekL1 : 0.0;
    }

    public double getSocialCareCostWeekly() {
        double cost = 0.0;
        if (careFormalX !=null)
            if (careFormalX >0.0)
                cost = careFormalX;
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
        if (eduHighestFatherC4 !=null && eduHighestMotherC4 !=null) {
            if (eduHighestFatherC4.getRank() > eduHighestMotherC4.getRank())
                return ee.equals(eduHighestFatherC4);
            else
                return ee.equals(eduHighestMotherC4);
        } else if (eduHighestFatherC4 !=null) {
            return ee.equals(eduHighestFatherC4);
        } else {
            return ee.equals(eduHighestMotherC4);
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
            case Education -> {return RegressionName.EducationE2;}
            case Health -> {return RegressionName.HealthH1;}
            case Disability -> {return RegressionName.HealthH2;}
            case Cohabitation -> {
                if (Dcpst.Partnered.equals(demPartnerStatusL1))
                    return RegressionName.PartnershipU2;
                else if (getStudent()==0)
                    return RegressionName.PartnershipU1;
                else
                    return RegressionName.PartnershipU1;
            }
            case SocialCareProvision -> {
                if (Dcpst.Partnered.equals(getDemPartnerStatus()))
                    return RegressionName.SocialCareS3b;
                else
                    return RegressionName.SocialCareS3a;
            }
            case WagePotential -> {
                if (Gender.Male.equals(demMaleFlag))
                    return RegressionName.WagesMalesE;
                else
                    return RegressionName.WagesFemalesE;
            }
            case WageOffer1 -> {
                if (Gender.Male.equals(demMaleFlag)) {
                    if (Education.High.equals(eduHighestC4L1)) {
                        return RegressionName.UnemploymentU1a;
                    } else {
                        return RegressionName.UnemploymentU1b;
                    }
                } else {
                    if (Education.High.equals(eduHighestC4L1)) {
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
        if (healthPsyDstrss0to12L1 == null)
            throw new RuntimeException("attempt to access dhmGhq_lag1 before it has been initialised");
        return healthPsyDstrss0to12L1;
    }

    public Double getYPersAndPartnerGrossDiffMonth() {
        Person partner = getPartner();
        if (partner != null) {
            if (partner.getYNonBenPersGrossMonth() != null && getYNonBenPersGrossMonth() != null)
                return getYNonBenPersGrossMonth() - partner.getYNonBenPersGrossMonth();
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
        yLifeTime = 0.0;
        if (demAge > 0) {

            int birthYear = ltIncomeDonor.getBirthYear();
            int ageLimit = Math.min(maxAge, demAge -1);
            for (int aa=0; aa<=ageLimit; aa++) {
                AnnualIncome annualIncome = ltIncomeDonor.getAnnualIncome(birthYear+aa);
                if (annualIncome == null)
                    throw new RuntimeException("Annual income for year " + (birthYear+aa) + " not found for donor " + ltIncomeDonor.getId());
                yLifeTime += annualIncome.getValue();
            }
            yLifeTime /= (double)(ageLimit+1);
        }
    }

    public double getYLifeTime() {
        if (Parameters.isFinite(yLifeTime))
            return yLifeTime;
        else
            throw new RuntimeException("yLifeTime is not finite");
    }

    public void updateLtIncome() {
        double newVal = getBenefitUnit().getHousehold().getEquivalisedDisposableIncomeYearly();
        if (demAge == 0) {
            yLifeTime = newVal;
        } else {

            if (!Parameters.isFinite(yLifeTime))
                throw new RuntimeException("yLifeTime is not defined");
            double curVal = yLifeTime;
            double years = demAge + 1;
            yLifeTime = (curVal * (years - 1) + newVal) / years;
        }
    }

    public Individual getLtIncomeDonor() {return ltIncomeDonor;}

    public Integer getLiwwh() {
        return labEmpNyear;
    }

    public boolean isHousingOwned() {
        return getBenefitUnit().isHousingOwned();
    }

    public Education getEduHighestPartner() {
        Person partner = getPartner();
        if (partner==null) {
            return null;
        } else {
            return partner.eduHighestC4;
        }
    }

    public Dhe getHealthPartner() {
        Person partner = getPartner();
        if (partner==null) {
            return null;
        } else {
            return partner.healthSelfRated;
        }
    }

    public Integer getAgeDifferencePartner() {
        Person partner = getPartner();
        if (partner==null) {
            return null;
        } else {
            return demAge - partner.demAge;
        }
    }

    public Long getIdPartner() {
        Person partner = getPartner();
        if (partner==null) {
            return null;
        } else {
            return partner.getId();
        }
    }

    private Quintiles getEmploymentEarningsQuintile() {
        return getEmploymentEarningsQuintile(false);
    }

    private Quintiles getEmploymentEarningsQuintile(boolean refresh) {

        if (refresh || i_yEarningsQuintileC5 == null) {

            SimPathsCollector collector = getBenefitUnit().getCollector();
            if (collector.getWealthIncomeStats() != null) { //Collector only gets initialised when simulation starts running

                // quintile cut-offs are evaluated for preceding year - uprate for wage growth
                double wageGrowth = Parameters.getTimeSeriesIndex(model.getYear(), UpratingCase.Earnings) /
                        Parameters.getTimeSeriesIndex(model.getYear() - 1, UpratingCase.Earnings);
                if (getGrossEarningsYearly() < collector.getWealthIncomeStats().getEmployedEarningsP20() * wageGrowth) {
                    i_yEarningsQuintileC5 = Quintiles.Q1;
                } else if (getGrossEarningsYearly() < collector.getWealthIncomeStats().getEmployedEarningsP40() * wageGrowth) {
                    i_yEarningsQuintileC5 = Quintiles.Q2;
                } else if (getGrossEarningsYearly() < collector.getWealthIncomeStats().getEmployedEarningsP60() * wageGrowth) {
                    i_yEarningsQuintileC5 = Quintiles.Q3;
                } else if (getGrossEarningsYearly() < collector.getWealthIncomeStats().getEmployedEarningsP80() * wageGrowth) {
                    i_yEarningsQuintileC5 = Quintiles.Q4;
                } else {
                    i_yEarningsQuintileC5 = Quintiles.Q5;
                }
            } else {
                i_yEarningsQuintileC5 = Quintiles.Q3;
            }
        }
        return i_yEarningsQuintileC5;
    }

    public Double getContRateOPEe() {
        return getContRateOPEe(true);
    }
    public double getContRateOPEe(boolean throwError) {
        if (!Parameters.isFinite(contRateOPEe)) {
            if (throwError)
                throw new IllegalArgumentException("contRateOPEe is not finite");
            else
                return 0.0;
        }
        return contRateOPEe;
    }

    public void setContRateOPEe(Double contRateOPEe) {
        this.contRateOPEe = contRateOPEe;
    }

    public Double getContRateOPEr() {
        return getContRateOPEr(true);
    }
    public double getContRateOPEr(boolean throwError) {
        if (!Parameters.isFinite(contRateOPEr)) {
            if (throwError)
                throw new IllegalArgumentException("contRateOPEr is not finite");
            else
                return 0.0;
        }
        return contRateOPEr;
    }

    public void setContRateOPEr(Double contRateOPEr) {
        this.contRateOPEr = contRateOPEr;
    }

    public Double getContRatePP() {
        return getContRatePP(true);
    }
    public double getContRatePP(boolean throwError) {
        if (!Parameters.isFinite(contRatePP)) {
            if (throwError)
                throw new IllegalArgumentException("contRatePP is not finite");
            else
                return 0.0;
        }
        return contRatePP;
    }

    public void setContRatePP(Double contRatePP) {
        this.contRatePP = contRatePP;
    }

    public PrivatePension getPrivatePension() {
        if (privatePension != null)
            return new PrivatePension(privatePension);
        else
            return new PrivatePension();
    }

    public Double getWealthNonPensValue() {
        return wealthNonPensValue;
    }

    public double getWealthNonPensValue(boolean throwError) {
        if (!Parameters.isFinite(wealthNonPensValue)) {
            if (throwError)
                throw new IllegalArgumentException("wealthNonPensValue is not finite");
            else
                return 0.0;
        }
        return wealthNonPensValue;
    }

    public void setWealthNonPensValue(Double v) {
        wealthNonPensValue = v;
    }

    public Double getWealthNonPensValueL1() {
        return wealthNonPensValueL1;
    }

    public void setWealthNonPensValueL1(Double wealthNonPensValueL1) {
        this.wealthNonPensValueL1 = wealthNonPensValueL1;
    }
}
