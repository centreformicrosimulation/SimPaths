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

    // identifiers
    private Long idOriginalPerson;
    private Long idOriginalBU;
    private Long idOriginalHH;
    private Long idMother;
    private Long idFather;
    private Boolean clonedFlag;
    private Boolean bornInSimulation; //Flag to keep track of newborns
    private Long seed;
    private Long idHousehold;
    private Long idBenefitUnit;
    @Enumerated(EnumType.STRING) private SampleEntry sampleEntry;
    @Enumerated(EnumType.STRING) private SampleExit sampleExit = SampleExit.NotYet;  //entry to sample via international immigration

    // person level variables
    private int dag; //Age
    @Enumerated(EnumType.STRING) private Indicator adultchildflag;
    @Transient private boolean ioFlag;         // true if a dummy person instantiated for IO decision solution
    @Enumerated(EnumType.STRING) private Gender dgn;             // gender
    @Enumerated(EnumType.STRING) private Education deh_c3;       //Education level
    @Transient private Education deh_c3_lag1;  //Lag(1) of education level
    @Enumerated(EnumType.STRING) private Education dehm_c3;      //Mother's education level
    @Enumerated(EnumType.STRING) private Education dehf_c3;      //Father's education level
    @Enumerated(EnumType.STRING) private Ethnicity dot;          //Ethnicity
    @Enumerated(EnumType.STRING) private Indicator ded;          // in continuous education
    @Enumerated(EnumType.STRING) private Indicator der;          // return to education
    @Enumerated(EnumType.STRING) private Les_c4 les_c4;      //Activity (employment) status
    @Enumerated(EnumType.STRING) private Les_c7_covid les_c7_covid; //Activity (employment) status used in the Covid-19 models
    @Transient private Les_c4 les_c4_lag1;		//Lag(1) of activity_status
    @Transient private Les_c7_covid les_c7_covid_lag1;     //Lag(1) of 7-category activity status
    @Transient private Integer liwwh;                  //Work history in months (number of months in employment) (Note: this is monthly in EM, but simulation updates annually so increment by 12 months).
    @Enumerated(EnumType.STRING) private Indicator dlltsd;	//Long-term sick or disabled if = 1
    @Transient private Indicator dlltsd_lag1; //Lag(1) of long-term sick or disabled
    @Enumerated(EnumType.STRING) @Column(name="need_socare") private Indicator needSocialCare;
    @Column(name="formal_socare_hrs") private Double careHoursFromFormalWeekly;
    @Column(name="formal_socare_cost") private Double careFormalExpenditureWeekly;
    @Column(name="partner_socare_hrs") private Double careHoursFromPartnerWeekly;
    @Column(name="parent_socare_hrs") private Double careHoursFromParentWeekly;
    @Column(name="daughter_socare_hrs") private Double careHoursFromDaughterWeekly;
    @Column(name="son_socare_hrs") private Double careHoursFromSonWeekly;
    @Column(name="other_socare_hrs") private Double careHoursFromOtherWeekly;
    private Boolean lowWageOffer;
    @Transient private Boolean lowWageOffer_lag1;
    @Transient private SocialCareReceipt socialCareReceipt;
    @Transient private Boolean socialCareFromFormal;
    @Transient private Boolean socialCareFromPartner;
    @Transient private Boolean socialCareFromDaughter;
    @Transient private Boolean socialCareFromSon;
    @Transient private Boolean socialCareFromOther;
    @Column(name="socare_provided_hrs") private Double careHoursProvidedWeekly;
    @Enumerated(EnumType.STRING) @Column(name="socare_provided_to") private SocialCareProvision socialCareProvision;
    @Transient private SocialCareProvision socialCareProvision_lag1;
    @Transient private Indicator needSocialCare_lag1;
    @Transient private Double careHoursFromFormalWeekly_lag1;
    @Transient private Double careHoursFromPartnerWeekly_lag1;
    @Transient private Double careHoursFromParentWeekly_lag1;
    @Transient private Double careHoursFromDaughterWeekly_lag1;
    @Transient private Double careHoursFromSonWeekly_lag1;
    @Transient private Double careHoursFromOtherWeekly_lag1;
    @Transient private Boolean isHomeOwner_lag1;

    // partner lags
    @Transient private Dcpst dcpst_lag1;            // lag partnership status
    @Transient private Education dehsp_c3_lag1;     //Lag(1) of partner's education
    @Transient private Dhe dhesp_lag1;
    @Transient private Lesdf_c4 lesdf_c4_lag1;      //Lag(1) of own and partner's activity status
    @Transient private Long idPartnerLag1;
    @Transient private HouseholdStatus household_status_lag;		//Lag(1) of household_status
    @Transient private Integer dcpagdf_lag1;        //Lag(1) of difference between ages of partners in union
    @Transient private Double ynbcpdf_dv_lag1;      //Lag(1) of difference between own and partner's gross personal non-benefit income

    @Enumerated(EnumType.STRING) private Indicator sedex;    // year left education
    @Transient private Boolean toGiveBirth;
    @Transient private Boolean toLeaveSchool;
    @Transient private Boolean toBePartnered;
    @Transient private Boolean hasTestPartner;
    @Transient private Boolean leavePartner; // Used in partnership alignment process. Indicates that this person has found partner in a test run of union matching.
    @Column(name="person_weight") private Double weight;
    @Column(name="dhm_ghq") private Boolean dhmGhq; //Psychological distress case-based
    @Transient private Boolean dhmGhq_lag1;
    @Transient private Dhe dhe_lag1;
    @Enumerated(EnumType.STRING) private Dhe dhe;
    private Double dhm; //Psychological distress GHQ-12 Likert scale
    @Transient private Double dhm_lag1; //Lag(1) of dhm
    private Double dhe_mcs;  //mental well-being: SF12 mental component summary score
    @Transient private Double dhe_mcs_lag1;  //mental well-being: SF12 mental component summary score lag 1
    private Double dhe_pcs;  //physical well-being: SF12 physical component summary score
    @Transient private Double dhe_pcs_lag1;  //physical well-being: SF12 physical component summary score lag 1
    private Integer dls;      //life satisfaction - score 1-7
    @Transient private Double dls_temp;
    @Transient private Integer dls_lag1;      //life satisfaction - score 1-7 lag 1
    @Column(name="he_eq5d")
    private Double he_eq5d;
    @Column(name="financial_distress") private Boolean financialDistress;

    @Column(name="dhh_owned") private Boolean dhhOwned; // Person is a homeowner, true / false
    @Transient private Boolean receivesBenefitsFlag_L1; // Lag(1) of whether person receives benefits
    @Transient private Boolean receivesBenefitsFlag; // Does person receive benefits
    @Column(name="econ_benefits_uc") private Boolean receivesBenefitsFlagUC; // Person receives UC
    @Transient private Boolean receivesBenefitsFlagUC_L1;
    @Column(name="econ_benefits_nonuc") private Boolean receivesBenefitsFlagNonUC;  // Person receives a benefit which is not UC
    @Transient private Boolean receivesBenefitsFlagNonUC_L1;

    @Enumerated(EnumType.STRING) private Labour labourSupplyWeekly;			//Number of hours of labour supplied each week
    @Transient private Labour labourSupplyWeekly_L1; // Lag(1) (previous year's value) of weekly labour supply
    private Integer hoursWorkedWeekly;
    private Integer l1_lhw; // Lag(1) of hours worked weekly - use to initialise labour supply weekly_L1 (TODO)

//	Potential earnings is the gross hourly wage an individual can earn while working
//	and is estimated, for each individual, on the basis of observable characteristics as
//	age, education, civil status, number of children, etc. Hence, potential earnings
//	is a separate process in the simulation, and it is computed for every adult
//	individual in the simulated population, in each simulated period.
    @Column(name="potential_earnings_hourly") private Double fullTimeHourlyEarningsPotential;		//Is hourly rate.  Initialised with value: ils_earns / (4.34 * lhw), where lhw is the weekly hours a person worked in EUROMOD input data
    @Column(name="l1_potential_earnings_hourly") private Double L1_fullTimeHourlyEarningsPotential; // Lag(1) of potentialHourlyEarnings
    @Transient private Series.Double yearlyEquivalisedDisposableIncomeSeries;
    private Double yearlyEquivalisedConsumption;
    @Transient private Series.Double yearlyEquivalisedConsumptionSeries;
    private Double sIndex;
    private Double sIndexNormalised;
    @Transient private LinkedHashMap<Integer, Double> sIndexYearMap;
    private Integer dcpyy; //Number of years in partnership
    @Transient private Integer dcpyy_lag1; //Lag(1) of number of years in partnership
    private Double ypnbihs_dv; // asinh of personal non-benefit income per month
    @Transient private Double ypnbihs_dv_lag1; //Lag(1) of gross personal non-benefit income
    private Double yptciihs_dv; // asinh of non-employment non-benefit income per month (capital and pension)
    private Double ypncp; // asinh of capital income per month
    private Double ypnoab; // asinh of pension income per month
    @Transient private Double ypncp_lag1; //Lag(1) of ypncp
    @Transient private Double ypncp_lag2; //Lag(2) of capital income
    @Transient private Double ypnoab_lag1; //Lag(1) of pension income
    @Transient private Double ypnoab_lag2; //Lag(2) of pension income
    @Transient private Double yptciihs_dv_lag1; //Lag(1) of gross personal non-benefit non-employment income
    @Transient private Double yptciihs_dv_lag2; //Lag(2) of gross personal non-benefit non-employment income
    @Transient private Double yptciihs_dv_lag3; //Lag(3) of gross personal non-benefit non-employment income
    private Double yplgrs_dv;       // asinh transform of personal labour income per month
    @Transient private Double yplgrs_dv_lag1; //Lag(1) of gross personal employment income
    @Transient private Double yplgrs_dv_lag2; //Lag(2) of gross personal employment income
    @Transient private Double yplgrs_dv_lag3; //Lag(3) of gross personal employment income

    //For matching process
    @Transient private Double desiredAgeDiff;
    @Transient private Double desiredEarningsPotentialDiff;
    @Transient private Integer ageGroup;

    //This is set to true at the point when individual leaves education and never reset. So if true, individual has not always been in continuous education.
    @Transient private Boolean leftEducation;

    //This is set to true at the point when individual leaves partnership and never reset. So if true, individual has been / is in a partnership
    @Transient private Boolean leftPartnership;
    @Transient private Integer newWorkHours_lag1; // Define a variable to keep previous month's value of work hours to be used in the Covid-19 module
    @Transient private Double covidModuleGrossLabourIncome_lag1;
    @Transient private Indicator covidModuleReceivesSEISS = Indicator.False;
    @Transient private Double covidModuleGrossLabourIncome_Baseline;
    private Quintiles covidModuleGrossLabourIncomeBaseline_Xt5;
    @Transient private Double wageRegressionRandomComponentE;
    @Transient private Double wageRegressionRandomComponentNE;
    @Transient private Map<Labour, Integer> personContinuousHoursLabourSupplyMap = new EnumMap<>(Labour.class);

    // local variables interact with regression models
    @Transient private Integer yearLocal;
    @Transient private Region regionLocal;
    @Transient private Dhhtp_c4 dhhtp_c4_lag1Local;
    @Transient private Ydses_c5 ydses_c5_lag1Local;
    @Transient private Integer numberChildrenAllLocal_lag1;
    @Transient private Integer numberChildrenAllLocal;
    @Transient private Integer numberChildren02Local_lag1;
    @Transient private Integer numberChildren017Local;
    @Transient private Indicator indicatorChildren02Local;
    @Transient private Dcpst dcpstLocal;

    // innovations
    @Transient Innovations innovations;

    //TODO: Remove when no longer needed.  Used to calculate mean score of employment selection regression.
    @Transient public static Double scoreMale;
    @Transient public static Double scoreFemale;
    @Transient public static Double countMale;
    @Transient public static Double countFemale;
    @Transient public static Double inverseMillsRatioMaxMale = Double.MIN_VALUE;
    @Transient public static Double inverseMillsRatioMinMale = Double.MAX_VALUE;
    @Transient public static Double inverseMillsRatioMaxFemale = Double.MIN_VALUE;
    @Transient public static Double inverseMillsRatioMinFemale = Double.MAX_VALUE;


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

        sampleEntry = SampleEntry.Birth;
        dgn = gender;
        idMother = mother.getId();
        dehm_c3 = mother.getDeh_c3();
        if (mother.getPartner()==null) {
            idFather = null;
            dehf_c3 = mother.getDeh_c3();
        } else {
            idFather = mother.getPartner().getId();
            dehf_c3 = mother.getPartner().getDeh_c3();
        }

        liwwh = 0;
        yptciihs_dv = 0.0;
        ypncp = 0.0;
        ypnoab = 0.0;
        fullTimeHourlyEarningsPotential = Parameters.MIN_HOURLY_WAGE_RATE;
        dlltsd = Indicator.False;
        setAllSocialCareVariablesToFalse();
        lowWageOffer = false;
        benefitUnit = mother.benefitUnit;
        idBenefitUnit = benefitUnit.getId();
        dag = 0;
        weight = mother.getWeight();			//Newborn has same weight as mother (the number of newborns will then be aligned in fertility alignment)
        dhe = Dhe.VeryGood;
        dhm = 9.;			//Set to median for under 18's as a placeholder
        dhmGhq = false;
        deh_c3 = Education.Low;
        dot = mother.getDot();
        les_c4 = Les_c4.Student;				//Set lag activity status as Student, i.e. in education from birth
        leftEducation = false;
        les_c7_covid = Les_c7_covid.Student;
        labourSupplyWeekly = Labour.ZERO;			//Will be updated in Labour Market Module when the person stops being a student
        hoursWorkedWeekly = getLabourSupplyWeekly().getHours(this);
        idHousehold = mother.getBenefitUnit().getHousehold().getId();
//		setDeviationFromMeanRetirementAge();			//This would normally be done within initialisation, but the line above has been commented out for reasons given...
        yearlyEquivalisedDisposableIncomeSeries = new Series.Double(this, DoublesVariables.EquivalisedIncomeYearly);
        yearlyEquivalisedConsumptionSeries = new Series.Double(this, DoublesVariables.EquivalisedConsumptionYearly);
        yearlyEquivalisedConsumption = 0.;
        sIndexYearMap = new LinkedHashMap<Integer, Double>();
        bornInSimulation = true;
        dhhOwned = false;
        receivesBenefitsFlag = false;
        receivesBenefitsFlagNonUC = false;
        receivesBenefitsFlagUC = false;
        financialDistress = mother.getFinancialDistress();
        updateVariables(false);
    }

    // a "copy constructor" for persons: used by the cloneBenefitUnit method of the SimPathsModel object
    // used to generate clones both at population load (to un-weight data) and to generate international immigrants
    public Person (Person originalPerson, long seed, SampleEntry sampleEntry) {

        this(personIdCounter++, seed);
        switch (sampleEntry) {
            case ProcessedInputData -> {
                key.setId(originalPerson.getId());
                idOriginalPerson = originalPerson.getIdOriginalPerson();
                idOriginalBU = originalPerson.getIdOriginalBU();
                idOriginalHH = originalPerson.getIdOriginalHH();
            }
            default -> {
                idOriginalPerson = originalPerson.key.getId();
                idOriginalHH = originalPerson.benefitUnit.getHousehold().getId();
                idOriginalBU = originalPerson.benefitUnit.getId();
            }
        }

        this.sampleEntry = sampleEntry;

        dag = originalPerson.dag;
        ageGroup = originalPerson.ageGroup;
        dgn = originalPerson.dgn;
        deh_c3 = originalPerson.deh_c3;

        if (originalPerson.deh_c3_lag1 != null) { //If original person misses lagged level of education, assign current level of education
            deh_c3_lag1 = originalPerson.deh_c3_lag1;
        } else {
            deh_c3_lag1 = deh_c3;
        }

        dehf_c3 = originalPerson.dehf_c3;
        dehm_c3 = originalPerson.dehm_c3;
        dehsp_c3_lag1 = originalPerson.deh_c3_lag1;

        if (originalPerson.dag < Parameters.MIN_AGE_TO_LEAVE_EDUCATION) { //If under age to leave education, set flag for being in education to true
            ded = Indicator.True;
        } else {
            ded = originalPerson.ded;
        }

        der = originalPerson.der;
        dcpyy = Objects.requireNonNullElse(originalPerson.dcpyy,0);
        dcpyy_lag1 = Objects.requireNonNullElseGet(originalPerson.dcpyy_lag1, () -> Math.max(0, this.dcpyy - 1));
        dcpagdf_lag1 = originalPerson.dcpagdf_lag1;
        household_status_lag = originalPerson.household_status_lag;
        if (originalPerson.les_c4 != null) {
            les_c4 = originalPerson.les_c4;
        } else if (originalPerson.dag < Parameters.MIN_AGE_TO_LEAVE_EDUCATION) {
            les_c4 = Les_c4.Student;
        } else if (originalPerson.dag > (int)Parameters.getTimeSeriesValue(model.getYear(), originalPerson.getDgn().toString(), TimeSeriesVariable.FixedRetirementAge)) {
            les_c4 = Les_c4.Retired;
        } else if (originalPerson.getLabourSupplyWeekly() != null && originalPerson.getLabourSupplyWeekly().getHours(originalPerson) > 0) {
            les_c4 = Les_c4.EmployedOrSelfEmployed;
        } else {
            les_c4 = Les_c4.NotEmployed;
        }
        if (dag < Parameters.MIN_AGE_TO_LEAVE_EDUCATION)
            leftEducation = false;
        else if (dag > Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION)
            leftEducation = true;
        else
            leftEducation = (!Les_c4.Student.equals(les_c4));

        if (originalPerson.les_c4_lag1 != null) { //If original persons misses lagged activity status, assign current activity status
            les_c4_lag1 = originalPerson.les_c4_lag1;
        } else {
            les_c4_lag1 = les_c4;
        }

        les_c7_covid = originalPerson.les_c7_covid;
        if (originalPerson.les_c7_covid_lag1 != null) { //If original persons misses lagged activity status, assign current activity status
            les_c7_covid_lag1 = originalPerson.les_c7_covid_lag1;
        } else {
            les_c7_covid_lag1 = les_c7_covid;
        }

        lesdf_c4_lag1 = originalPerson.lesdf_c4_lag1;
        dcpst_lag1 = originalPerson.dcpst_lag1;
        ypnbihs_dv = originalPerson.getYpnbihs_dv();
        ypnbihs_dv_lag1 = originalPerson.ypnbihs_dv_lag1;
        yptciihs_dv = Objects.requireNonNullElse(originalPerson.yptciihs_dv, 0.0);
        yplgrs_dv = originalPerson.getYplgrs_dv();
        yplgrs_dv_lag1 = originalPerson.yplgrs_dv_lag1;
        yplgrs_dv_lag2 = originalPerson.yplgrs_dv_lag2;
        yplgrs_dv_lag3 = originalPerson.yplgrs_dv_lag3;
        ynbcpdf_dv_lag1 = originalPerson.ynbcpdf_dv_lag1;
        ypncp = Objects.requireNonNullElse(originalPerson.ypncp,0.0);
        ypncp_lag1 = originalPerson.ypncp_lag1;
        ypncp_lag2 = originalPerson.ypncp_lag2;
        ypnoab = Objects.requireNonNullElse(originalPerson.ypnoab, 0.0);
        ypnoab_lag1 = originalPerson.ypnoab_lag1;
        ypnoab_lag2 = originalPerson.ypnoab_lag2;

        liwwh = Objects.requireNonNullElseGet(originalPerson.liwwh, () -> ((Les_c4.EmployedOrSelfEmployed.equals(les_c4)) ? 12 : 0));
        dlltsd = originalPerson.dlltsd;
        dlltsd_lag1 = originalPerson.dlltsd_lag1;
        needSocialCare = Objects.requireNonNullElse(originalPerson.needSocialCare, Indicator.False);
        careHoursFromFormalWeekly = Objects.requireNonNullElse(originalPerson.careHoursFromFormalWeekly, 0.0);
        careFormalExpenditureWeekly = Objects.requireNonNullElse(originalPerson.careFormalExpenditureWeekly, 0.0);
        careHoursFromPartnerWeekly = Objects.requireNonNullElse(originalPerson.careHoursFromPartnerWeekly, 0.0);
        careHoursFromParentWeekly = Objects.requireNonNullElse(originalPerson.careHoursFromParentWeekly, 0.0);
        careHoursFromDaughterWeekly = Objects.requireNonNullElse(originalPerson.careHoursFromDaughterWeekly, 0.0);
        careHoursFromSonWeekly = Objects.requireNonNullElse(originalPerson.careHoursFromSonWeekly, 0.0);
        careHoursFromOtherWeekly = Objects.requireNonNullElse(originalPerson.careHoursFromOtherWeekly, 0.0);
        socialCareFromFormal = Objects.requireNonNullElseGet(originalPerson.socialCareFromFormal, () -> (careHoursFromFormalWeekly > 0.0));
        socialCareFromPartner = Objects.requireNonNullElseGet(originalPerson.socialCareFromPartner, () -> (careHoursFromPartnerWeekly > 0.0));
        socialCareFromDaughter = Objects.requireNonNullElseGet(originalPerson.socialCareFromDaughter, () -> (careHoursFromDaughterWeekly > 0.0));
        socialCareFromSon = Objects.requireNonNullElseGet(originalPerson.socialCareFromSon, () -> (careHoursFromSonWeekly > 0.0));
        socialCareFromOther = Objects.requireNonNullElseGet(originalPerson.socialCareFromOther, () -> (careHoursFromOtherWeekly > 0.0));
        if (originalPerson.socialCareReceipt!=null)
            socialCareReceipt = originalPerson.socialCareReceipt;
        else {
            if (socialCareFromFormal) {
                if (socialCareFromPartner || socialCareFromDaughter || socialCareFromSon || socialCareFromOther)
                    socialCareReceipt = SocialCareReceipt.Mixed;
                else
                    socialCareReceipt = SocialCareReceipt.Formal;
            } else {
                if (socialCareFromPartner || socialCareFromDaughter || socialCareFromSon || socialCareFromOther)
                    socialCareReceipt = SocialCareReceipt.Informal;
                else
                    socialCareReceipt = SocialCareReceipt.None;
            }
        }

        careHoursProvidedWeekly = Objects.requireNonNullElse(originalPerson.careHoursProvidedWeekly, 0.0);
        socialCareProvision = Objects.requireNonNullElseGet(originalPerson.socialCareProvision, () ->
                (careHoursProvidedWeekly > 0.01) ? SocialCareProvision.OnlyOther : SocialCareProvision.None);

        needSocialCare_lag1 = Objects.requireNonNullElse(originalPerson.needSocialCare_lag1, needSocialCare);
        careHoursFromFormalWeekly_lag1 = Objects.requireNonNullElse(originalPerson.careHoursFromFormalWeekly_lag1, careHoursFromFormalWeekly);
        careHoursFromPartnerWeekly_lag1 = Objects.requireNonNullElse(originalPerson.careHoursFromPartnerWeekly_lag1, careHoursFromPartnerWeekly);
        careHoursFromDaughterWeekly_lag1 = Objects.requireNonNullElse(originalPerson.careHoursFromDaughterWeekly_lag1, careHoursFromDaughterWeekly);
        careHoursFromSonWeekly_lag1 = Objects.requireNonNullElse(originalPerson.careHoursFromSonWeekly_lag1, careHoursFromSonWeekly);
        careHoursFromOtherWeekly_lag1 = Objects.requireNonNullElse(originalPerson.careHoursFromOtherWeekly_lag1, careHoursFromOtherWeekly);
        socialCareProvision_lag1 = Objects.requireNonNullElse(originalPerson.socialCareProvision_lag1, socialCareProvision);

        lowWageOffer = originalPerson.lowWageOffer;
        lowWageOffer_lag1 = originalPerson.lowWageOffer_lag1;
        sedex = originalPerson.sedex;
        toGiveBirth = originalPerson.toGiveBirth;
        toLeaveSchool = originalPerson.toLeaveSchool;
        weight = originalPerson.weight;
        dhe = originalPerson.dhe;
        dhm = originalPerson.dhm;

        isHomeOwner_lag1 = originalPerson.dhhOwned;

        if (originalPerson.dhe_lag1 != null) { //If original person misses lagged level of health, assign current level of health as lagged value
            dhe_lag1 = originalPerson.dhe_lag1;
        } else {
            dhe_lag1 = originalPerson.dhe;
        }

        if (originalPerson.dhm_lag1 != null) {
            dhm_lag1 = originalPerson.dhm_lag1;
        } else {
            dhm_lag1 = originalPerson.dhm;
        }

        dhmGhq = Objects.requireNonNullElse(originalPerson.dhmGhq, false);
        dhmGhq_lag1 = Objects.requireNonNullElse(originalPerson.dhmGhq_lag1, dhmGhq);

        dls = originalPerson.dls;
        dhe_mcs = originalPerson.dhe_mcs;
        dhe_pcs = originalPerson.dhe_pcs;

        if (originalPerson.dls_lag1 != null) {
            dls_lag1 = originalPerson.dls_lag1;
        } else {
            dls_lag1 = originalPerson.dls;
        }

        if (originalPerson.dhm_lag1 != null) {
            dhe_mcs_lag1 = originalPerson.dhe_mcs_lag1;
        } else {
            dhe_mcs_lag1 = originalPerson.dhe_mcs;
        }

        if (originalPerson.dhe_pcs_lag1 != null) {
            dhe_pcs_lag1 = originalPerson.dhe_pcs_lag1;
        } else {
            dhe_pcs_lag1 = originalPerson.dhe_pcs;
        }

        if (originalPerson.l1_lhw != null) {
            labourSupplyWeekly_L1 = Labour.convertHoursToLabour(originalPerson.l1_lhw);
        } else {
            labourSupplyWeekly_L1 = null; // Update only if value know; null values handled by getter. Should throw an exception if required before initialised in the simulation.
        }

        dhesp_lag1 = originalPerson.dhesp_lag1;
        hoursWorkedWeekly = originalPerson.hoursWorkedWeekly;
        l1_lhw = originalPerson.l1_lhw;
        labourSupplyWeekly = originalPerson.getLabourSupplyWeekly();
        double[] sampleDifferentials = setMarriageTargets();
        desiredAgeDiff = Objects.requireNonNullElseGet(originalPerson.desiredAgeDiff, () -> sampleDifferentials[0]);
        desiredEarningsPotentialDiff = Objects.requireNonNullElseGet(originalPerson.desiredEarningsPotentialDiff, () -> sampleDifferentials[1]);

        scoreMale = originalPerson.scoreMale;
        scoreFemale = originalPerson.scoreFemale;
        countMale = originalPerson.countMale;
        countFemale = originalPerson.countFemale;
        inverseMillsRatioMaxMale = originalPerson.inverseMillsRatioMaxMale;
        inverseMillsRatioMinMale  = originalPerson.inverseMillsRatioMinMale;
        inverseMillsRatioMaxFemale = originalPerson.inverseMillsRatioMaxFemale;
        inverseMillsRatioMinFemale = originalPerson.inverseMillsRatioMinFemale;

        adultchildflag = originalPerson.adultchildflag;
        yearlyEquivalisedDisposableIncomeSeries = new Series.Double(this, DoublesVariables.EquivalisedIncomeYearly);
        yearlyEquivalisedConsumptionSeries = new Series.Double(this, DoublesVariables.EquivalisedConsumptionYearly);
        yearlyEquivalisedConsumption = originalPerson.yearlyEquivalisedConsumption;
        sIndexYearMap = new LinkedHashMap<Integer, Double>();
        dhhOwned = originalPerson.dhhOwned;
        dot = originalPerson.dot;
        receivesBenefitsFlag = originalPerson.receivesBenefitsFlag;
        receivesBenefitsFlag_L1 = originalPerson.receivesBenefitsFlag_L1;
        receivesBenefitsFlagNonUC = originalPerson.receivesBenefitsFlagNonUC;
        receivesBenefitsFlagNonUC_L1 = originalPerson.receivesBenefitsFlagNonUC_L1;
        receivesBenefitsFlagUC = originalPerson.receivesBenefitsFlagUC;
        receivesBenefitsFlagUC_L1 = originalPerson.receivesBenefitsFlagUC_L1;
        financialDistress = originalPerson.financialDistress;

        if (originalPerson.fullTimeHourlyEarningsPotential > Parameters.MIN_HOURLY_WAGE_RATE) {
            fullTimeHourlyEarningsPotential = Math.min(Parameters.MAX_HOURLY_WAGE_RATE, Math.max(Parameters.MIN_HOURLY_WAGE_RATE, originalPerson.fullTimeHourlyEarningsPotential));
        } else {
            if (Les_c4.EmployedOrSelfEmployed.equals(les_c4)) {
                les_c4 = Les_c4.NotEmployed;
            }
            les_c4_lag1 = les_c4;
            fullTimeHourlyEarningsPotential = -9.0;
        }
        if (originalPerson.L1_fullTimeHourlyEarningsPotential!=null && originalPerson.L1_fullTimeHourlyEarningsPotential>Parameters.MIN_HOURLY_WAGE_RATE) {
            L1_fullTimeHourlyEarningsPotential = Math.min(Parameters.MAX_HOURLY_WAGE_RATE, Math.max(Parameters.MIN_HOURLY_WAGE_RATE, originalPerson.L1_fullTimeHourlyEarningsPotential));
        } else {
            L1_fullTimeHourlyEarningsPotential = fullTimeHourlyEarningsPotential;
        }
    }

    // used by other constructors
    public Person(Long id, long seed) {
        super();
        key = new PanelEntityKey(id);
        model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        clonedFlag = false;

        // initialise random draws
        this.seed = seed;
        innovations = new Innovations(33, 1, 1, seed);

        //Draw desired age and wage differential for parametric partnership formation for people above age to get married:
        double[] sampleDifferentials = setMarriageTargets();
        desiredAgeDiff = sampleDifferentials[0];
        desiredEarningsPotentialDiff = sampleDifferentials[1];
    }


    // ---------------------------------------------------------------------
    // Initialisation methods
    // ---------------------------------------------------------------------
    public void cloneCleanup() {

        if (fullTimeHourlyEarningsPotential < Parameters.MIN_HOURLY_WAGE_RATE) {
            updateFullTimeHourlyEarnings();
            if (L1_fullTimeHourlyEarningsPotential < Parameters.MIN_HOURLY_WAGE_RATE)
                L1_fullTimeHourlyEarningsPotential = fullTimeHourlyEarningsPotential;
        }
    }

    private double[] setMarriageTargets() {

        double[] sampleDifferentials = new double[2];
        if (Parameters.MARRIAGE_MATCH_TO_MEANS) {
            sampleDifferentials[0] = Parameters.targetMeanAgeDifferential;
            sampleDifferentials[1] = Parameters.targetMeanWageDifferential;
        } else {
            sampleDifferentials = Parameters.getWageAndAgeDifferentialMultivariateNormalDistribution(innovations.getSingleDrawLongInnov(0));
        }
        return sampleDifferentials;
    }

    private void setAllSocialCareVariablesToFalse() {
        needSocialCare = Indicator.False;
        careHoursFromFormalWeekly = -9.0;
        careHoursFromPartnerWeekly = -9.0;
        careHoursFromParentWeekly = -9.0;
        careHoursFromDaughterWeekly = -9.0;
        careHoursFromSonWeekly = -9.0;
        careHoursFromOtherWeekly = -9.0;
        careHoursProvidedWeekly = -9.0;
        careFormalExpenditureWeekly = -9.0;
        socialCareReceipt = SocialCareReceipt.None;
        socialCareFromFormal = false;
        socialCareFromPartner = false;
        socialCareFromDaughter = false;
        socialCareFromSon = false;
        socialCareFromOther = false;
        socialCareProvision = SocialCareProvision.None;
        needSocialCare_lag1 = Indicator.False;
        careHoursFromFormalWeekly_lag1 = -9.0;
        careHoursFromPartnerWeekly_lag1 = -9.0;
        careHoursFromParentWeekly_lag1 = -9.0;
        careHoursFromDaughterWeekly_lag1 = -9.0;
        careHoursFromSonWeekly_lag1 = -9.0;
        careHoursFromOtherWeekly_lag1 = -9.0;
        socialCareProvision_lag1 = SocialCareProvision.None;
    }

    public void setAdditionalFieldsInInitialPopulation() {

        if (labourSupplyWeekly==null)
            labourSupplyWeekly = Labour.convertHoursToLabour(model.getInitialHoursWorkedWeekly().get(key.getId()).intValue()); // TODO: this can be simplified to obtain value from already initialised hours worked weekly variable? The entire database query on setup is redundant? See initialisation of the lag below.
        receivesBenefitsFlag_L1 = receivesBenefitsFlag;
        labourSupplyWeekly_L1 = Labour.convertHoursToLabour(l1_lhw);
        receivesBenefitsFlagNonUC_L1 = receivesBenefitsFlagNonUC;
        receivesBenefitsFlagUC_L1 = receivesBenefitsFlagUC;

        if(UnionMatchingMethod.SBAM.equals(model.getUnionMatchingMethod())) {
            updateAgeGroup();
        }

        hoursWorkedWeekly = null;	//Not to be updated as labourSupplyWeekly contains this information.
        updateVariables(true);
    }

    //This method assign people to age groups used to define types in the SBAM matching procedure
    private void updateAgeGroup() {
        if (dag < 18) {
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
        HealthMentalHM1HM2Cases,		//Case-based prediction for psychological distress, Steps 1 and 2 together
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
        SocialCareReceipt,
        SocialCareProvision,
        Unemployment,
        Update,
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
            case HealthMentalHM1HM2Cases -> {
                healthMentalHM1HM2Cases();
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

        toGiveBirth = false;
        FertileFilter filter = new FertileFilter();
        if (filter.evaluate(this)) {

            double prob;
            if (model.getCountry().equals(Country.UK)) {

                if (getDag() <= 29 && getLes_c4().equals(Les_c4.Student) && !isLeftEducation()) {
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

            if (innovations.getDoubleDraw(29)<prob)
                toGiveBirth = true;
        }
    }

    private void updateUnemploymentState() {
        lowWageOffer = false;
        if (Parameters.flagUnemployment) {

            if (this == benefitUnit.getRefPersonForDecisions()) {
                // unemployment currently limited to reference person for decisions

                double prob;
                if (dgn.equals(Gender.Male)) {
                    if (deh_c3.equals(Education.High)) {
                        prob = Parameters.getRegUnemploymentMaleGraduateU1a().getProbability(this, Person.DoublesVariables.class);
                    } else {
                        prob = Parameters.getRegUnemploymentMaleNonGraduateU1b().getProbability(this, Person.DoublesVariables.class);
                    }
                } else {
                    if (deh_c3.equals(Education.High)) {
                        prob = Parameters.getRegUnemploymentFemaleGraduateU1c().getProbability(this, Person.DoublesVariables.class);
                    } else {
                        prob = Parameters.getRegUnemploymentFemaleNonGraduateU1d().getProbability(this, Person.DoublesVariables.class);
                    }
                }
                lowWageOffer = (innovations.getDoubleDraw(22) < prob);
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
            if (Objects.equals(partner.getId(), idPartnerLag1)) {
                if (dcpyy==null)
                    throw new RuntimeException("problem identifying dcpyy");
                dcpyy++;
            } else
                dcpyy = 0;
        } else
            dcpyy = 0;

        // iterate employment history
        if (Les_c4.EmployedOrSelfEmployed.equals(les_c4)) {
            liwwh = liwwh+12;
        }

        // iterate age and update for maturity
        dag++;
        if (dag == Parameters.AGE_TO_BECOME_RESPONSIBLE) {
            setupNewBenefitUnit(true);
            considerLeavingHome();
        } else if (dag > Parameters.AGE_TO_BECOME_RESPONSIBLE && Indicator.True.equals(adultchildflag)) {
            considerLeavingHome();
        }
        updateAgeGroup();   //Update ageGroup as person ages
     }

    private void considerMortality() {

        boolean flagDies = false;
        if (model.getProjectMortality()) {

            if ( Occupancy.Couple.equals(benefitUnit.getOccupancy()) || benefitUnit.getSize() == 1 ) {
                // exclude single parents with dependent children from death

                double mortalityProbability = Parameters.getMortalityProbability(dgn, dag, model.getYear());
                if (innovations.getDoubleDraw(0) < mortalityProbability) {
                    flagDies = true;
                }
            }
        }
        if (flagDies || dag > Parameters.maxAge)
            sampleExit = SampleExit.Death;
    }

    //This process should be applied to those at the age to become responsible / leave home OR above if they have the adultChildFlag set to True (i.e. people can move out, but not move back in).
    private void considerLeavingHome() {

        //For those who are moving out, evaluate whether they should have stayed with parents and if yes, set the adultchildflag to true

        double prob = Parameters.getRegLeaveHomeP1a().getProbability(this, Person.DoublesVariables.class);
        boolean toLeaveHome = (innovations.getDoubleDraw(21) < prob);
        if (Les_c4.Student.equals(les_c4)) {

            adultchildflag = Indicator.True; //Students not allowed to leave home to match filtering conditon
        } else {

            if (!toLeaveHome) { //If at the age to leave home but regression outcome is negative, person has adultchildflag set to true (although they still set up a new benefitUnit in the simulation, it's treated differently in the labour supply)

                adultchildflag = Indicator.True;
            } else {

                adultchildflag = Indicator.False;
                setupNewHousehold(); //If person leaves home, they set up a new household
            }
        }
    }

    public boolean considerRetirement() {
        boolean toRetire = false;
        if (dag >= Parameters.MIN_AGE_TO_RETIRE && !Les_c4.Retired.equals(les_c4) && !Les_c4.Retired.equals(les_c4_lag1)) {
            if (Parameters.enableIntertemporalOptimisations && DecisionParams.flagRetirement) {
                if (Labour.ZERO.equals(labourSupplyWeekly_L1)) {
                    toRetire = true;
                }
           } else {
                double prob;
                if (getPartner() != null) {
                    prob = Parameters.getRegRetirementR1b().getProbability(this, Person.DoublesVariables.class);
                } else {
                    prob = Parameters.getRegRetirementR1a().getProbability(this, Person.DoublesVariables.class);
                }
                toRetire = (innovations.getDoubleDraw(23) < prob);
            }
            if (toRetire) {
                setLes_c4(Les_c4.Retired);
            }
        }
        return toRetire;
    }

    private void updateFinancialDistress() {
        double prob = Parameters.getRegFinancialDistress().getProbability(this, Person.DoublesVariables.class);
        financialDistress = innovations.getDoubleDraw(32) < prob;
    }
    
    /*
    This method corresponds to Step 1 of the mental health evaluation: predict level of mental health on the GHQ-12 Likert scale based on observable characteristics
     */
    protected void healthMentalHM1Level() {
        if (dag >= 16) {
            double score = Parameters.getRegHealthHM1Level().getScore(this, Person.DoublesVariables.class);
            double rmse = Parameters.getRMSEForRegression("HM1");
            double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innovations.getDoubleDraw(1));
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

    protected void healthMCS1() {

        double mcsPrediction;
        mcsPrediction = Parameters.getRegHealthMCS1().getScore(this, Person.DoublesVariables.class);
        dhe_mcs = mcsPrediction;

    }

    protected void healthMCS2() {

        double mcsPrediction;
        if (Gender.Male.equals(getDgn())) {
            mcsPrediction = Parameters.getRegHealthMCS2Males().getScore(this, Person.DoublesVariables.class);
            dhe_mcs = constrainSF12Estimate(mcsPrediction + dhe_mcs);
        } else if (Gender.Female.equals(getDgn())) {
            mcsPrediction = Parameters.getRegHealthMCS2Females().getScore(this, Person.DoublesVariables.class);
            dhe_mcs = constrainSF12Estimate(mcsPrediction + dhe_mcs);
        }
    }

    protected void healthPCS1() {

        double pcsPrediction;
        pcsPrediction = Parameters.getRegHealthPCS1().getScore(this, Person.DoublesVariables.class);
        dhe_pcs = pcsPrediction;

    }


    protected void healthPCS2() {

        double pcsPrediction;
        if (Gender.Male.equals(getDgn())) {
            pcsPrediction = Parameters.getRegHealthPCS2Males().getScore(this, Person.DoublesVariables.class);
            dhe_pcs = constrainSF12Estimate(pcsPrediction + dhe_pcs);
        } else if (Gender.Female.equals(getDgn())) {
            pcsPrediction = Parameters.getRegHealthPCS2Females().getScore(this, Person.DoublesVariables.class);
            dhe_pcs = constrainSF12Estimate(pcsPrediction + dhe_pcs);
        }
    }

    protected void lifeSatisfaction1() {

        double dlsPrediction;
        dlsPrediction = Parameters.getRegLifeSatisfaction1().getScore(this, Person.DoublesVariables.class);
        dls_temp = dlsPrediction;

    }


    protected void lifeSatisfaction2() {

        double dlsPrediction;
        if (Gender.Male.equals(getDgn())) {
            dlsPrediction = Parameters.getRegLifeSatisfaction2Males().getScore(this, Person.DoublesVariables.class);
            dls = constrainLifeSatisfactionEstimate(dlsPrediction + dls_temp);
        } else if (Gender.Female.equals(getDgn())) {
            dlsPrediction = Parameters.getRegLifeSatisfaction2Females().getScore(this, Person.DoublesVariables.class);
            dls = constrainLifeSatisfactionEstimate(dlsPrediction + dls_temp);
        }
    }

    private void healthEQ5D() {

        double eq5dPrediction;
        eq5dPrediction = Parameters.getRegEQ5D().getScore(this, Person.DoublesVariables.class);
        if (eq5dPrediction > 1) {
            he_eq5d = 1.0;
        }
        else if (eq5dPrediction < -0.594) {
            he_eq5d = -0.594;
        }
        else {
            he_eq5d = eq5dPrediction;
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
            tmp_outcome = (innovations.getDoubleDraw(2) < tmp_probability);
            // 4. Set dhm_ghq dummy
            setDhmGhq(tmp_outcome);
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

    protected Double constrainSF12Estimate(double sf12) {
        if (sf12 < 0.) {
            sf12 = 0.;
        } else if (sf12 > 100.) {
            sf12 = 100.;
        }
        return sf12;
    }

    protected Integer constrainLifeSatisfactionEstimate(double dls_estimate) {
        if (Double.isNaN(dls_estimate) || Double.isInfinite(dls_estimate)) {
            return null;
        }

        if (dls_estimate < 1.) {
            dls_estimate = 1.;
        } else if (dls_estimate > 7.) {
            dls_estimate = 7.;
        }

        return (int) Math.round(dls_estimate);
    }

    //Health process defines health using H1a or H1b process
    protected void health() {

        double healthInnov1 = innovations.getDoubleDraw(3);
        double healthInnov2 = innovations.getDoubleDraw(4);
        if((dag >= 16 && dag <= 29) && Les_c4.Student.equals(les_c4) && leftEducation == false) {
            //If age is between 16 - 29 and individual has always been in education, follow process H1a:

            Map<Dhe,Double> probs = ManagerRegressions.getProbabilities(this, RegressionName.HealthH1a);
            MultiValEvent event = new MultiValEvent(probs, healthInnov1);
            dhe = (Dhe) event.eval();
            if (event.isProblemWithProbs())
                model.addCounterErrorH1a();
        } else if (dag >= 16) {

            Map<Dhe,Double> probs = ManagerRegressions.getProbabilities(this, RegressionName.HealthH1b);
            MultiValEvent event = new MultiValEvent(probs, healthInnov1);
            dhe = (Dhe) event.eval();
            if (event.isProblemWithProbs())
                model.addCounterErrorH1b();

            //If age is over 16 and individual is not in continuous education, also follow process H2b to calculate the probability of long-term sickness / disability:
            boolean becomeLTSickDisabled = false;
            if (!Parameters.enableIntertemporalOptimisations || DecisionParams.flagDisability) {

                double prob = Parameters.getRegHealthH2b().getProbability(this, Person.DoublesVariables.class);
                becomeLTSickDisabled = (healthInnov2 < prob);
            }
            if (becomeLTSickDisabled) {
                dlltsd = Indicator.True;
            } else {
                dlltsd = Indicator.False;
            }
        }
    }

    protected void evaluateSocialCareReceipt() {

        if (dag < Parameters.MIN_AGE_FORMAL_SOCARE || getYear()>getStartYear()) {

            needSocialCare = Indicator.False;
            careHoursFromFormalWeekly = 0.0;
            careFormalExpenditureWeekly = 0.0;
            careHoursFromPartnerWeekly = 0.0;
            careHoursFromParentWeekly = 0.0;
            careHoursFromDaughterWeekly = 0.0;
            careHoursFromSonWeekly = 0.0;
            careHoursFromOtherWeekly = 0.0;
            socialCareReceipt = SocialCareReceipt.None;
            socialCareFromFormal = false;
            socialCareFromPartner = false;
            socialCareFromDaughter = false;
            socialCareFromSon = false;
            socialCareFromOther = false;
        }
        if (careHoursFromParentWeekly==null)
            careHoursFromParentWeekly = 0.0;

        if ((dag < Parameters.MIN_AGE_FORMAL_SOCARE) && Indicator.True.equals(dlltsd)) {
            // under 65 years old with disability

            needSocialCare = Indicator.True;
            double probRecCare;
            if (Indicator.False.equals(dlltsd_lag1) || getYear()==getStartYear()) {
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

            if (innovations.getDoubleDraw(5) < probRecCare) {
                // receive social care

                double score = Parameters.getRegCareHoursS1b().getScore(this,Person.DoublesVariables.class);
                double rmse = Parameters.getRMSEForRegression("S1b");
                double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innovations.getDoubleDraw(6));
                double careHours = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.exp(score + rmse * gauss));
                Person partner = getPartner();
                if (partner!=null && partner.getDag() < 75) {
                    socialCareFromPartner = true;
                    careHoursFromPartnerWeekly = careHours;
                } else if (dag < 50) {
                    socialCareFromOther = true;
                    careHoursFromParentWeekly = careHours;
                } else {
                    socialCareFromOther = true;
                    careHoursFromOtherWeekly = careHours;
                }
            }
        }

        if (dag >= Parameters.MIN_AGE_FORMAL_SOCARE && getYear()>getStartYear()) {
            // need care only projected for 65 and over due to limitations of data used for parameterisation

            double probNeedCare = Parameters.getRegNeedCareS2a().getProbability(this, Person.DoublesVariables.class);
            double recCareInnov = innovations.getDoubleDraw(7);
            if (recCareInnov < probNeedCare) {
                // need care
                needSocialCare = Indicator.True;
            }

            double probRecCare = Parameters.getRegReceiveCareS2b().getProbability(this, Person.DoublesVariables.class);
            if (recCareInnov < probRecCare) {
                // receive care

                Map<SocialCareReceiptS2c,Double> probs1 = Parameters.getRegSocialCareMarketS2c().getProbabilities(this, Person.DoublesVariables.class);
                MultiValEvent event = new MultiValEvent(probs1, innovations.getDoubleDraw(8));
                SocialCareReceiptS2c socialCareReceiptS2c = (SocialCareReceiptS2c) event.eval();
                socialCareReceipt = SocialCareReceipt.getCode(socialCareReceiptS2c);
                if (SocialCareReceipt.Mixed.equals(socialCareReceipt) || SocialCareReceipt.Formal.equals(socialCareReceipt))
                    socialCareFromFormal = true;

                if (SocialCareReceipt.Mixed.equals(socialCareReceipt) || SocialCareReceipt.Informal.equals(socialCareReceipt)) {
                    // some informal care received

                    if (getPartner()!=null) {
                        // check if receive care from partner

                        double probPartnerCare = Parameters.getRegReceiveCarePartnerS2d().getProbability(this, Person.DoublesVariables.class);
                        if (innovations.getDoubleDraw(9) < probPartnerCare) {
                            // receive care from partner - check for supplementary carers

                            socialCareFromPartner = true;
                            Map<PartnerSupplementaryCarer,Double> probs2 =
                                    Parameters.getRegPartnerSupplementaryCareS2e().getProbabilities(this, Person.DoublesVariables.class);
                            event = new MultiValEvent(probs2, innovations.getDoubleDraw(10));
                            PartnerSupplementaryCarer cc = (PartnerSupplementaryCarer) event.eval();
                            if (PartnerSupplementaryCarer.Daughter.equals(cc))
                                socialCareFromDaughter = true;
                            if (PartnerSupplementaryCarer.Son.equals(cc))
                                socialCareFromSon = true;
                            if (PartnerSupplementaryCarer.Other.equals(cc))
                                socialCareFromOther = true;
                        }
                    }
                    if (!socialCareFromPartner) {
                        // no care from partner - identify who supplies informal care

                        Map<NotPartnerInformalCarer,Double> probs2 =
                                Parameters.getRegNotPartnerInformalCareS2f().getProbabilities(this, Person.DoublesVariables.class);
                        event = new MultiValEvent(probs2, innovations.getDoubleDraw(11));
                        NotPartnerInformalCarer cc = (NotPartnerInformalCarer) event.eval();
                        if (NotPartnerInformalCarer.DaughterOnly.equals(cc) || NotPartnerInformalCarer.DaughterAndSon.equals(cc) || NotPartnerInformalCarer.DaughterAndOther.equals(cc))
                            socialCareFromDaughter = true;
                        if (NotPartnerInformalCarer.SonOnly.equals(cc) || NotPartnerInformalCarer.DaughterAndSon.equals(cc) || NotPartnerInformalCarer.SonAndOther.equals(cc))
                            socialCareFromSon = true;
                        if (NotPartnerInformalCarer.OtherOnly.equals(cc) || NotPartnerInformalCarer.SonAndOther.equals(cc) || NotPartnerInformalCarer.DaughterAndOther.equals(cc))
                            socialCareFromOther = true;
                    }
                }
                double careHoursInnov = innovations.getDoubleDraw(12);
                if (socialCareFromPartner) {
                    double score = Parameters.getRegPartnerCareHoursS2g().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2g");
                    double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                    careHoursFromPartnerWeekly = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.exp(score + rmse * gauss));
                }
                careHoursInnov = Parameters.updateProbability(careHoursInnov);
                if (socialCareFromDaughter) {
                    double score = Parameters.getRegDaughterCareHoursS2h().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2h");
                    double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                    careHoursFromDaughterWeekly = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.exp(score + rmse * gauss));
                }
                careHoursInnov = Parameters.updateProbability(careHoursInnov);
                if (socialCareFromSon) {
                    double score = Parameters.getRegSonCareHoursS2i().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2i");
                    double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                    careHoursFromSonWeekly = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.exp(score + rmse * gauss));
                }
                careHoursInnov = Parameters.updateProbability(careHoursInnov);
                if (socialCareFromOther) {
                    double score = Parameters.getRegOtherCareHoursS2j().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2j");
                    double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                    careHoursFromOtherWeekly = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE, Math.exp(score + rmse * gauss));
                }
                careHoursInnov = Parameters.updateProbability(careHoursInnov);
                if (socialCareFromFormal) {
                    double score = Parameters.getRegFormalCareHoursS2k().getScore(this,Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("S2k");
                    double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(careHoursInnov);
                    careHoursFromFormalWeekly = Math.min(Parameters.MAX_HOURS_WEEKLY_FORMAL_CARE, Math.exp(score + rmse * gauss));
                    careFormalExpenditureWeekly = careHoursFromFormalWeekly * Parameters.getTimeSeriesValue(model.getYear(), TimeSeriesVariable.CarerWageRate);
                }
            }
        }
        if (Parameters.flagSuppressSocialCareCosts)
            careFormalExpenditureWeekly = 0.0;
    }

    protected void evaluateSocialCareProvision() {
        evaluateSocialCareProvision(Parameters.getTimeSeriesValue(model.getYear(),TimeSeriesVariable.CareProvisionAdjustment));
    }

    public void evaluateSocialCareProvision(double probitAdjustment) {

        socialCareProvision = SocialCareProvision.None;
        careHoursProvidedWeekly = 0.0;
        boolean careToPartner = false;
        boolean careToOther;
        double careHoursToPartner = 0.0;
        if (dag >= Parameters.AGE_TO_BECOME_RESPONSIBLE) {

            // check if care provided to partner
            // identified in method evaluateSocialCareReceipt
            Person partner = getPartner();
            if (partner!=null && partner.socialCareFromPartner) {
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
            careToOther = (innovations.getDoubleDraw(13) < prob);

            // update care provision states
            if (careToPartner || careToOther) {

                if (careToPartner && careToOther) {
                    socialCareProvision = SocialCareProvision.PartnerAndOther;
                } else if (careToPartner) {
                    socialCareProvision = SocialCareProvision.OnlyPartner;
                } else {
                    socialCareProvision = SocialCareProvision.OnlyOther;
                }
                if (!Parameters.flagSuppressSocialCareCosts) {
                    if (SocialCareProvision.OnlyPartner.equals(socialCareProvision)) {
                        careHoursProvidedWeekly = careHoursToPartner;
                    } else {
                        double score = Parameters.getRegCareHoursProvS3e().getScore(this,Person.DoublesVariables.class);
                        double rmse = Parameters.getRMSEForRegression("S3e");
                        double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innovations.getDoubleDraw(14));
                        careHoursProvidedWeekly = Math.min(Parameters.MAX_HOURS_WEEKLY_INFORMAL_CARE,
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

        toBePartnered = false;
        leavePartner = false;
        hasTestPartner = false;
        double cohabitInnov = innovations.getDoubleDraw(25);
        Person partner = getPartner();
        if (dag >= Parameters.MIN_AGE_COHABITATION) {
            // cohabitation possible

            if (model.getCountry() == Country.UK) {

                double prob;
                if (partner == null) {
                    // partnership formation

                    if (dag <= 29 && les_c4 == Les_c4.Student && !leftEducation) {

                        double score = Parameters.getRegPartnershipU1a().getScore(this, Person.DoublesVariables.class);
                        prob = Parameters.getRegPartnershipU1a().getProbability(score + probitAdjustment);
                    } else {

                        double score = Parameters.getRegPartnershipU1b().getScore(this, Person.DoublesVariables.class);
                        prob = Parameters.getRegPartnershipU1b().getProbability(score + probitAdjustment);
                    }
                    toBePartnered = (cohabitInnov < prob);
                    if (toBePartnered)
                        model.getPersonsToMatch().get(dgn).get(getRegion()).add(this);
                } else if (dgn == Gender.Female && (dag > 29 || !Les_c4.Student.equals(les_c4) || leftEducation)) {
                    // partnership dissolution

                    double score = Parameters.getRegPartnershipU2b().getScore(this, Person.DoublesVariables.class);
                    prob = Parameters.getRegPartnershipU2b().getProbability(score - probitAdjustment);
                    if (cohabitInnov < prob) {
                        leavePartner = true;
                    }
                }
            } else if (model.getCountry() == Country.IT) {

                if (partner == null) {
                    if ((les_c4 == Les_c4.Student && leftEducation) || !les_c4.equals(Les_c4.Student)) {

                        double prob = Parameters.getRegPartnershipITU1().getProbability(this, Person.DoublesVariables.class);
                        toBePartnered = (cohabitInnov < prob);
                        if (toBePartnered)
                            model.getPersonsToMatch().get(dgn).get(getRegion()).add(this);
                    }
                } else if (partner != null && dgn == Gender.Female && ((les_c4 == Les_c4.Student && leftEducation) || !les_c4.equals(Les_c4.Student))) {

                    double prob = Parameters.getRegPartnershipITU2().getProbability(this, Person.DoublesVariables.class);
                    if (cohabitInnov < prob) {
                        leavePartner = true;
                    }
                }
            }
        }
    }

    protected void partnershipDissolution() {

        if (leavePartner) {

            // update partner's variables first
            Person partner = getPartner();
            partner.setDcpyy(null);
            partner.setLeftPartnership(true); //Set to true if leaves partnership to use with fertility regression, this is never reset

            setDcpyy(null); 		  //Set number of years in partnership to null if leaving partner
            setLeftPartnership(true); //Set to true if leaves partnership to use with fertility regression, this is never reset
            idHousehold = null;

            setupNewBenefitUnit(true);
        }
    }

    protected void inSchool() {

        double labourInnov = innovations.getDoubleDraw(24);
        //Min age to leave education set to 16 (from 18 previously) but note that age to leave home is 18.
        if (Les_c4.Retired.equals(les_c4) || dag < Parameters.MIN_AGE_TO_LEAVE_EDUCATION || dag > Parameters.MAX_AGE_TO_ENTER_EDUCATION) {		//Only apply module for persons who are old enough to consider leaving education, but not retired
            return;
        } else if (Les_c4.Student.equals(les_c4) && !leftEducation && dag >= Parameters.MIN_AGE_TO_LEAVE_EDUCATION) { //leftEducation is initialised to false and updated to true when individual leaves education (and never reset).
            //If age is between 16 - 29 and individual has always been in education, follow process E1a:

            if (dag <= Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION) {

                double prob = Parameters.getRegEducationE1a().getProbability(this, Person.DoublesVariables.class);
                toLeaveSchool = (labourInnov >= prob); //If event is true, stay in school.  If event is false, leave school.
            } else {
                toLeaveSchool = true; //Hasn't left education until 30 - force out
            }
        } else if (dag <= 45 && (!Les_c4.Student.equals(les_c4) || leftEducation)) { //leftEducation is initialised to false and updated to true when individual leaves education for the first time (and never reset).
            //If age is between 16 - 45 and individual has not continuously been in education, follow process E1b:
            //Either individual is currently a student and has left education at some point in the past (so returned) or individual is not a student so has not been in continuous education:
            //TODO: If regression outcome of process E1b is true, set activity status to student and der (return to education indicator) to true?

            double prob = Parameters.getRegEducationE1b().getProbability(this, Person.DoublesVariables.class);
            if (labourInnov < prob) {
                //If event is true, re-enter education.  If event is false, leave school

                setLes_c4(Les_c4.Student);
                setDer(Indicator.True);
                setDed(Indicator.True);
            } else if (Les_c4.Student.equals(les_c4)){
                //If activity status is student but regression to be in education was evaluated to false, remove student status

                setLes_c4(Les_c4.NotEmployed);
                setDed(Indicator.False);
                toLeaveSchool = true; //Test what happens if people who returned to education leave again
            }
        } else if (dag > 45 && les_c4.equals(Les_c4.Student)) {
            //People above 45 shouldn't be in education, so if someone re-entered at 45 in previous step, force out

            setLes_c4(Les_c4.NotEmployed);
            setDed(Indicator.False);
        }
    }

    protected void leavingSchool() {

        if (toLeaveSchool) {

            setEducationLevel(); //If individual leaves school follow process E2a to assign level of education
            setSedex(Indicator.True); //Set variable left education (sedex) if leaving school
            setDed(Indicator.False); //Set variable in education (ded) to false if leaving school
            setDer(Indicator.False);
            setLeftEducation(true); //This is not reset and indicates if individual has ever left school - used with health process
            setLes_c4(Les_c4.NotEmployed); //Set activity status to NotEmployed when leaving school to remove Student status
        }
    }


    private void giveBirth() {				//To be called once per year after fertility alignment

        if (toGiveBirth) {		//toGiveBirth is determined by fertility process

            Gender babyGender = (innovations.getDoubleDraw(27) < Parameters.PROB_NEWBORN_IS_MALE) ? Gender.Male : Gender.Female;

            //Give birth to new person and add them to benefitUnit.
            Person child = new Person(babyGender, this);
            model.getPersons().add(child);
            benefitUnit.getMembers().add(child);
        }
    }


    protected void initialisePotentialHourlyEarnings() {

        double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innovations.getDoubleDraw(15));
        double logPotentialHourlyEarnings, score, rmse;
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

        double rmse, wagesInnov = innovations.getDoubleDraw(16);
        if (Les_c4.EmployedOrSelfEmployed.equals(les_c4_lag1)) {
            if (wageRegressionRandomComponentE == null || !model.fixRegressionStochasticComponent) {
                if (Gender.Male.equals(dgn)) {
                    rmse = Parameters.getRMSEForRegression("Wages_MalesE");
                } else {
                    rmse = Parameters.getRMSEForRegression("Wages_FemalesE");
                }
                double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(wagesInnov);
                wageRegressionRandomComponentE = rmse * gauss;
            }
        } else {
            if (wageRegressionRandomComponentNE == null || !model.fixRegressionStochasticComponent) {
                if (Gender.Male.equals(dgn)) {
                    rmse = Parameters.getRMSEForRegression("Wages_MalesNE");
                } else {
                    rmse = Parameters.getRMSEForRegression("Wages_FemalesNE");
                }
                double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(wagesInnov);
                wageRegressionRandomComponentNE = rmse * gauss;
            }
        }

        double logFullTimeHourlyEarnings;
        if(Gender.Male.equals(dgn)) {
            if (Les_c4.EmployedOrSelfEmployed.equals(les_c4_lag1)) {
                logFullTimeHourlyEarnings = Parameters.getRegWagesMalesE().getScore(this, Person.DoublesVariables.class) + wageRegressionRandomComponentE;
            } else {
                logFullTimeHourlyEarnings = Parameters.getRegWagesMalesNE().getScore(this, Person.DoublesVariables.class) + wageRegressionRandomComponentNE;
            }
        } else {
            if (Les_c4.EmployedOrSelfEmployed.equals(les_c4_lag1)) {
                logFullTimeHourlyEarnings = Parameters.getRegWagesFemalesE().getScore(this, Person.DoublesVariables.class) + wageRegressionRandomComponentE;
            } else {
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
        double capitalInnov = innovations.getDoubleDraw(17);
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
        if (dag >= Parameters.MIN_AGE_TO_HAVE_INCOME) {

            double capitalInnov = innovations.getDoubleDraw(18);
            if (dag <= 29 && Les_c4.Student.equals(les_c4) && !leftEducation) {
                // full-time students

                double prob = Parameters.getRegIncomeI3a_selection().getProbability(this, Person.DoublesVariables.class);
                boolean hasCapitalIncome = (capitalInnov < prob);
                if (hasCapitalIncome) {

                    double score = Parameters.getRegIncomeI3a().getScore(this, Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("I3a");
                    double capinclevel = setIncomeBySource(score, rmse, IncomeSource.CapitalIncome, RegressionScoreType.Asinh);
                    ypncp = Parameters.asinh(capinclevel); //Capital income amount
                }
                else ypncp = 0.; //If no capital income, set amount to 0
            } else if (leftEducation || !Les_c4.Student.equals(les_c4)) {

                double prob = Parameters.getRegIncomeI3b_selection().getProbability(this, Person.DoublesVariables.class);
                boolean hasCapitalIncome = (capitalInnov < prob);
                if (hasCapitalIncome) {

                    double score = Parameters.getRegIncomeI3b().getScore(this, Person.DoublesVariables.class);
                    double rmse = Parameters.getRMSEForRegression("I3b");
                    double capinclevel = setIncomeBySource(score, rmse, IncomeSource.CapitalIncome, RegressionScoreType.Asinh);
                    ypncp = Parameters.asinh(capinclevel); //Capital income amount
                }
                else ypncp = 0.; //If no capital income, set amount to 0
            }
            if (Les_c4.Retired.equals(les_c4)) {
                // Retirement decision is modelled in the retirement process. Here only the amount of pension income for retired individuals is modelled.
                /*
                    Private pension income when individual was retired in the previous period is modelled using process I4b.

                    Private pension income when individual moves from non-retirement to retirement is modelled using:
                    i) process I5a_selection, to determine who receives private pension income
                    ii) process I5b_amount, for those who are determined to receive private pension income by process I5a_selection. I5b_amount is modelled in levels using linear regression.
                */

                double score, rmse, pensionIncLevel = 0.;
                if (Les_c4.Retired.equals(les_c4_lag1)) {
                    // If person was retired in the previous period (and the simulation is not in its initial year), use process I4b

                    score = Parameters.getRegIncomeI4b().getScore(this, Person.DoublesVariables.class);
                    rmse = Parameters.getRMSEForRegression("I4b");
                    pensionIncLevel = setIncomeBySource(score, rmse, IncomeSource.PrivatePension, RegressionScoreType.Asinh);
                } else {
                    // For individuals in the first year of retirement, use processes I5a_selection and I5b_amount

                    double prob = Parameters.getRegIncomeI5a_selection().getProbability(this, Person.DoublesVariables.class);
                    boolean hasPrivatePensionIncome = (innovations.getDoubleDraw(19) < prob);
                    if (hasPrivatePensionIncome) {

                        score = Parameters.getRegIncomeI5b_amount().getScore(this, Person.DoublesVariables.class);
                        rmse = Parameters.getRMSEForRegression("I5b");
                        pensionIncLevel = setIncomeBySource(score, rmse, IncomeSource.PrivatePension, RegressionScoreType.Level);
                    }
                }
                ypnoab = Parameters.asinh(pensionIncLevel);
            }
        }

        double capital_income_multiplier = model.getSavingRate()/Parameters.SAVINGS_RATE;
        double yptciihs_dv_tmp_level = capital_income_multiplier*(Math.sinh(ypncp) + Math.sinh(ypnoab)); //Multiplied by the capital income multiplier, defined as chosen savings rate divided by the long-term average (specified in Parameters class)
        yptciihs_dv = Parameters.asinh(yptciihs_dv_tmp_level); //Non-employment non-benefit income is the sum of capital income and, for retired individuals, pension income.

        if (yptciihs_dv > 13.0) {
            yptciihs_dv = 13.5;
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

        if (dag < Parameters.MIN_AGE_FLEXIBLE_LABOUR_SUPPLY)
            return false;
        if (dag > Parameters.MAX_AGE_FLEXIBLE_LABOUR_SUPPLY)
            return false;
        if (Les_c4.Retired.equals(les_c4) && !Parameters.enableIntertemporalOptimisations)
            return false;
        if (Les_c4.Student.equals(les_c4) && !Parameters.enableIntertemporalOptimisations)
            return false;
        if (Indicator.True.equals(dlltsd) && !Parameters.flagSuppressSocialCareCosts)
            return false;
        if (Indicator.True.equals(needSocialCare) && !Parameters.flagSuppressSocialCareCosts)
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
        MultiValEvent event = new MultiValEvent(probs, innovations.getDoubleDraw(30));
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
        if(deh_c3 != null) {
            if(newEducationLevel.ordinal() > deh_c3.ordinal()) {		//Assume Education level cannot decrease after re-entering school.
                deh_c3 = newEducationLevel;
            }
        } else {
            deh_c3 = newEducationLevel;
        }
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
        toBePartnered = false;
        leavePartner = false;
        ded = (Les_c4.Student.equals(les_c4)) ? Indicator.True : Indicator.False;
        if (initialUpdate && careHoursFromParentWeekly==null)
            careHoursFromParentWeekly = 0.0;
        if (dag<Parameters.AGE_TO_BECOME_RESPONSIBLE) {
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

        //Lagged variables
        updateLaggedVariables(initialUpdate);

        // generate year specific random draws
        if (!initialUpdate) {
            if (Parameters.enableIntertemporalOptimisations && !DecisionParams.flagDisability) {
                dlltsd = Indicator.False;
                dlltsd_lag1 = Indicator.False;
            }
            if (!Parameters.flagSocialCare) {
                setAllSocialCareVariablesToFalse();
            }
            innovations.getNewDoubleDraws();
        }
    }

    private void updateLaggedVariables(boolean initialUpdate) {

        les_c4_lag1 = les_c4;
        les_c7_covid_lag1 = les_c7_covid;
        household_status_lag = getHouseholdStatus();
        dhe_lag1 = dhe; //Update lag(1) of health
        dhm_lag1 = dhm; //Update lag(1) of mental health
        dhmGhq_lag1 = dhmGhq;
        dls_lag1 = dls;
        dhe_mcs_lag1 = dhe_mcs;
        dhe_pcs_lag1 = dhe_pcs;
        isHomeOwner_lag1 = getBenefitUnit().isDhhOwned();
        dlltsd_lag1 = dlltsd; //Update lag(1) of long-term sick or disabled status
        needSocialCare_lag1 = needSocialCare;
        careHoursFromFormalWeekly_lag1 = careHoursFromFormalWeekly;
        careHoursFromPartnerWeekly_lag1 = careHoursFromPartnerWeekly;
        careHoursFromParentWeekly_lag1 = careHoursFromParentWeekly;
        careHoursFromDaughterWeekly_lag1 = careHoursFromDaughterWeekly;
        careHoursFromSonWeekly_lag1 = careHoursFromSonWeekly;
        careHoursFromOtherWeekly_lag1 = careHoursFromOtherWeekly;
        socialCareProvision_lag1 = socialCareProvision;
        lowWageOffer_lag1 = getLowWageOffer();
        deh_c3_lag1 = deh_c3; //Update lag(1) of education level
        ypnbihs_dv_lag1 = getYpnbihs_dv(); //Update lag(1) of gross personal non-benefit income
        labourSupplyWeekly_L1 = getLabourSupplyWeekly(); // Lag(1) of labour supply
        receivesBenefitsFlag_L1 = receivesBenefitsFlag; // Lag(1) of flag indicating if individual receives benefits
        receivesBenefitsFlagNonUC_L1 = receivesBenefitsFlagNonUC; // Lag(1) of flag indicating if individual receives non-UC benefits
        receivesBenefitsFlagUC_L1 = receivesBenefitsFlagUC; // Lag(1) of flag indicating if individual receives UC
        L1_fullTimeHourlyEarningsPotential = fullTimeHourlyEarningsPotential; // Lag(1) of potential hourly earnings

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

        // partner variables
        Person partner = getPartner();
        if (partner!=null) {
            dehsp_c3_lag1 = partner.deh_c3;
            dhesp_lag1 = partner.dhe;
            dcpst_lag1 = Dcpst.Partnered;
            dcpagdf_lag1 = dag - partner.dag;
            idPartnerLag1 = partner.getId();
        } else {
            dehsp_c3_lag1 = null;
            dhesp_lag1 = null;
            dcpagdf_lag1 = null;
            dcpst_lag1 = getDcpst();
            idPartnerLag1 = null;
        }
        ynbcpdf_dv_lag1 = getYnbcpdf_dv(); //Lag(1) of difference between own and partner's gross personal non-benefit income
        lesdf_c4_lag1 = getLesdf_c4(); //Lag(1) of own and partner's activity status
    }

    // used when children leave home
    protected void setupNewHousehold() {

        Household newHousehold = new Household();
        model.getHouseholds().add(newHousehold);
        benefitUnit.setHousehold(newHousehold);
        idHousehold = newHousehold.getId();
        idBenefitUnit = benefitUnit.getId();
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

            if (dag==Parameters.AGE_TO_BECOME_RESPONSIBLE) {
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
        if (person.dag<Parameters.AGE_TO_BECOME_RESPONSIBLE)
            throw new RuntimeException("problem identifying allocation of children to new benefit unit");
        if (Gender.Female.equals(person.dgn))
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
            return (dhmGhq)? 1 : 0;

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
        D_children,
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
        DagCeiling54,
        Dag_sqCeiling54,
        Dcpagdf_L1, 					//Lag(1) of age difference between partners
        Dcpyy_L1, 						//Lag(1) number of years in partnership
        Dcpst_Partnered,				//Partnered
        Dcpst_PreviouslyPartnered,		//Previously partnered
        Dcpst_PreviouslyPartnered_L1,   //Lag(1) of partnership status is previously partnered
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
        Dhhtp_c4_CoupleChildren_L1,
        Dhhtp_c4_CoupleNoChildren_L1,
        Dhhtp_c4_SingleChildren_L1,
        Dhhtp_c4_SingleNoChildren_L1,
        Dhm,							//Mental health status
        Dhm_L1,							//Mental health status lag(1)
        Dls,                            //Life satisfaction status
        Dls_L1,                            //Life satisfaction status lag(1)
        Dhe_mcs,                        //Mental well-being status
        Dhe_mcs_L1,                        //Mental well-being status lag(1)
        Dhe_mcs_sq,                     //MCS score squared
        Dhe_mcs_times_pcs,              //MCS times PCS
        Dhe_mcs_c_times_pcs_c,          //Centralised MCS times PCS
        Dhe_mcs_c,                      //MCS centralised by subtracting population mean
        Dhe_mcs_c_sq,                   //Square of centralised MCS
        Dhe_pcs,                        //Physical well-being status
        Dhe_pcs_L1,                        //Physical well-being status lag(1)
        Dhe_pcs_sq,                     //PCS score squared
        Dhe_pcs_cb,                     //PCS score cubed
        Dhe_pcs_c,                      //MCS centralised by subtracting population mean
        Dhe_pcs_c_sq,                   //Square of centralised MCS
        Dhmghq_L1,
        Dlltsd,							//Long-term sick or disabled
        Dlltsd_L1,						//Long-term sick or disabled lag(1)
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
        EthnicityWhite,
        EthnicityMixed,
        EthnicityAsian,
        EthnicityBlack,
        EthnicityOther,
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
        Les_c3_Sick_L1,					//This is based on dlltsd
        Les_c3_Student_L1,
        Les_c7_Covid_Furlough_L1,
        Lesdf_c4_BothNotEmployed_L1,
        Lesdf_c4_EmployedSpouseNotEmployed_L1, 					//Own and partner's activity status lag(1)
        Lesdf_c4_NotEmployedSpouseEmployed_L1,
        Lessp_c3_NotEmployed_L1,
        Lessp_c3_Sick_L1,
        Lessp_c3_Student_L1,			//Partner variables
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
        Year2021,
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
        Ydses_c5_Q3_L1,								//HH Income Lag(1) 3rd Quantile
        Ydses_c5_Q4_L1,								//HH Income Lag(1) 4th Quantile
        Ydses_c5_Q5_L1,								//HH Income Lag(1) 5th Quantile
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
                return (double) dag;
            }
            case Dag -> {
                return (double) dag;
            }
            case Dag_L1 -> {
                return (double) dag - 1;
            }
            case Dag_sq -> {
                return (double) dag * dag;
            }
            case Dag_sq_L1 -> {
                return (double) (dag - 1) * (dag - 1);
            }
            case DagCeiling54 -> {
                return (double) Math.min(dag, 54);
            }
            case Dag_sqCeiling54 -> {
                return (double) Math.min(dag, 54) * Math.min(dag, 54);
            }
            case AgeSquared -> {
                //			log.debug("age sq");
                return (double) dag * dag;
            }
            case AgeCubed -> {
                //			log.debug("age cub");
                return (double) dag * dag * dag;
            }
            case LnAge -> {
                return Math.log(dag);
            }
            case Age20to24 -> {
                return (dag >= 20 && dag <= 24) ? 1. : 0.;
            }
            case Age25to29 -> {
                return (dag >= 25 && dag <= 29) ? 1. : 0.;
            }
            case Age30to34 -> {
                return (dag >= 30 && dag <= 34) ? 1. : 0.;
            }
            case Age35to39 -> {
                return (dag >= 35 && dag <= 39) ? 1. : 0.;
            }
            case Age40to44 -> {
                return (dag >= 40 && dag <= 44) ? 1. : 0.;
            }
            case Age45to49 -> {
                return (dag >= 45 && dag <= 49) ? 1. : 0.;
            }
            case Age50to54 -> {
                return (dag >= 50 && dag <= 54) ? 1. : 0.;
            }
            case Age55to59 -> {
                return (dag >= 55 && dag <= 59) ? 1. : 0.;
            }
            case Age60to64 -> {
                return (dag >= 60 && dag <= 64) ? 1. : 0.;
            }
            case Age65to69 -> {
                return (dag >= 65 && dag <= 69) ? 1. : 0.;
            }
            case Age70to74 -> {
                return (dag >= 70 && dag <= 74) ? 1. : 0.;
            }
            case Age75to79 -> {
                return (dag >= 75 && dag <= 79) ? 1. : 0.;
            }
            case Age80to84 -> {
                return (dag >= 80 && dag <= 84) ? 1. : 0.;
            }
            case Age35to44 -> {
                return (dag >= 35 && dag <= 44) ? 1. : 0.;
            }
            case Age45to54 -> {
                return (dag >= 45 && dag <= 54) ? 1. : 0.;
            }
            case Age55to64 -> {
                return (dag >= 55 && dag <= 64) ? 1. : 0.;
            }
            case Age65plus -> {
                return (dag >= 65) ? 1. : 0.;
            }
            case Age23to25 -> {
                return (dag >= 23 && dag <= 25) ? 1. : 0.;
            }
            case Age26to30 -> {
                return (dag >= 26 && dag <= 30) ? 1. : 0.;
            }
            case Age21to27 -> {
                return (dag >= 21 && dag <= 27) ? 1. : 0.;
            }
            case Age28to30 -> {
                return (dag >= 28 && dag <= 30) ? 1. : 0.;
            }
            case AgeUnder25 -> {
                return (dag < 25) ? 1. : 0.;
            }
            case Age25to39 -> {
                return (dag >= 25 && dag <= 39) ? 1. : 0.;
            }
            case AgeOver39 -> {
                return (dag > 39) ? 1. : 0.;
            }
            case Age67to68 -> {
                return (dag >= 67 && dag <= 68) ? 1. : 0.;
            }
            case Age69to70 -> {
                return (dag >= 69 && dag <= 70) ? 1. : 0.;
            }
            case Age71to72 -> {
                return (dag >= 71 && dag <= 72) ? 1. : 0.;
            }
            case Age73to74 -> {
                return (dag >= 73 && dag <= 74) ? 1. : 0.;
            }
            case Age75to76 -> {
                return (dag >= 75 && dag <= 76) ? 1. : 0.;
            }
            case Age77to78 -> {
                return (dag >= 77 && dag <= 78) ? 1. : 0.;
            }
            case Age79to80 -> {
                return (dag >= 79 && dag <= 80) ? 1. : 0.;
            }
            case Age81to82 -> {
                return (dag >= 81 && dag <= 82) ? 1. : 0.;
            }
            case Age83to84 -> {
                return (dag >= 83 && dag <= 84) ? 1. : 0.;
            }
            case Age85plus -> {
                return (dag >= 85) ? 1. : 0.;
            }
            case StatePensionAge -> {
                return (dag >= 68) ? 1. : 0.;
            }
            case NeedCare_L1 -> {
                return (Indicator.True.equals(needSocialCare_lag1)) ? 1. : 0.;
            }
            case CareToPartnerOnly -> {
                return (SocialCareProvision.OnlyPartner.equals(socialCareProvision)) ? 1. : 0.;
            }
            case CareToPartnerAndOther -> {
                return (SocialCareProvision.PartnerAndOther.equals(socialCareProvision)) ? 1. : 0.;
            }
            case CareToOtherOnly -> {
                return (SocialCareProvision.OnlyOther.equals(socialCareProvision)) ? 1. : 0.;
            }
            case CareToPartnerOnly_L1 -> {
                return (SocialCareProvision.OnlyPartner.equals(socialCareProvision_lag1)) ? 1. : 0.;
            }
            case CareToPartnerAndOther_L1 -> {
                return (SocialCareProvision.PartnerAndOther.equals(socialCareProvision_lag1)) ? 1. : 0.;
            }
            case CareToOtherOnly_L1 -> {
                return (SocialCareProvision.OnlyOther.equals(socialCareProvision_lag1)) ? 1. : 0.;
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
                return (socialCareFromFormal) ? 1. : 0.;
            }
            case CareFromInformal -> {
                return (socialCareFromPartner || socialCareFromDaughter || socialCareFromSon || socialCareFromOther) ? 1. : 0.;
            }
            case CareFromPartner -> {
                return (socialCareFromPartner) ? 1. : 0.;
            }
            case CareFromDaughter -> {
                return (socialCareFromDaughter) ? 1. : 0.;
            }
            case CareFromSon -> {
                return (socialCareFromSon) ? 1. : 0.;
            }
            case CareFromOther -> {
                return (socialCareFromOther) ? 1. : 0.;
            }
            case Constant -> {
                return 1.;
            }
            case Dcpyy_L1 -> {
                return (dcpyy_lag1 != null) ? (double) dcpyy_lag1 : 0.0;
            }
            case Dcpagdf_L1 -> {
                return (dcpagdf_lag1 != null) ? (double) dcpagdf_lag1 : 0.0;
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
                if (dcpst_lag1 != null) {
                    return dcpst_lag1.equals(Dcpst.SingleNeverMarried) ? 1. : 0.;
                } else return 0.;
            }
            case Dcpst_PreviouslyPartnered_L1 -> {
                if (dcpst_lag1 != null) {
                    return dcpst_lag1.equals(Dcpst.PreviouslyPartnered) ? 1. : 0.;
                } else return 0.;
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
            case D_children -> {
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
                return (Gender.Male.equals(dgn)) ? 1.0 : 0.0;
            }
            case Dhe -> {
                return (double) dhe.getValue();
            }
            case Dhe_L1 -> {
                return (double) dhe_lag1.getValue();
            }
            case Dhe_Excellent -> {
                return (Dhe.Excellent.equals(dhe)) ? 1. : 0.;
            }
            case Dhe_VeryGood -> {
                return (Dhe.VeryGood.equals(dhe)) ? 1. : 0.;
            }
            case Dhe_Good -> {
                return (Dhe.Good.equals(dhe)) ? 1. : 0.;
            }
            case Dhe_Fair -> {
                return (Dhe.Fair.equals(dhe)) ? 1. : 0.;
            }
            case Dhe_Poor -> {
                return (Dhe.Poor.equals(dhe)) ? 1. : 0.;
            }
            case Dhe_Excellent_L1 -> {
                return (Dhe.Excellent.equals(dhe_lag1)) ? 1. : 0.;
            }
            case Dhe_VeryGood_L1 -> {
                return (Dhe.VeryGood.equals(dhe_lag1)) ? 1. : 0.;
            }
            case Dhe_Good_L1 -> {
                return (Dhe.Good.equals(dhe_lag1)) ? 1. : 0.;
            }
            case Dhe_Fair_L1 -> {
                return (Dhe.Fair.equals(dhe_lag1)) ? 1. : 0.;
            }
            case Dhe_Poor_L1 -> {
                return (Dhe.Poor.equals(dhe_lag1)) ? 1. : 0.;
            }
            case Dhesp_Excellent_L1 -> {
                return (Dhe.Excellent.equals(dhesp_lag1)) ? 1. : 0.;
            }
            case Dhesp_VeryGood_L1 -> {
                return (Dhe.VeryGood.equals(dhesp_lag1)) ? 1. : 0.;
            }
            case Dhesp_Good_L1 -> {
                return (Dhe.Good.equals(dhesp_lag1)) ? 1. : 0.;
            }
            case Dhesp_Fair_L1 -> {
                return (Dhe.Fair.equals(dhesp_lag1)) ? 1. : 0.;
            }
            case Dhesp_Poor_L1 -> {
                return (Dhe.Poor.equals(dhesp_lag1)) ? 1. : 0.;
            }
            case Dhe_2 -> {
                return (Dhe.Fair.equals(dhe)) ? 1. : 0.;
            }
            case Dhe_3 -> {
                return (Dhe.Good.equals(dhe)) ? 1. : 0.;
            }
            case Dhe_4 -> {
                return (Dhe.VeryGood.equals(dhe)) ? 1. : 0.;
            }
            case Dhe_5 -> {
                return (Dhe.Excellent.equals(dhe)) ? 1. : 0.;
            }
            case Dhe_c5_1_L1 -> {
                return (Dhe.Poor.equals(dhe_lag1)) ? 1.0 : 0.0;
            }
            case Dhe_c5_2_L1 -> {
                return (Dhe.Fair.equals(dhe_lag1)) ? 1.0 : 0.0;
            }
            case Dhe_c5_3_L1 -> {
                return (Dhe.Good.equals(dhe_lag1)) ? 1.0 : 0.0;
            }
            case Dhe_c5_4_L1 -> {
                return (Dhe.VeryGood.equals(dhe_lag1)) ? 1.0 : 0.0;
            }
            case Dhe_c5_5_L1 -> {
                return (Dhe.Excellent.equals(dhe_lag1)) ? 1.0 : 0.0;
            }
            case Dhm -> {
                return dhm;
            }
            case Dhm_L1 -> {
                if (dhm_lag1 != null && dhm_lag1 >= 0.) {
                    return dhm_lag1;
                } else return 0.;
            }
            case Dhe_mcs -> {
                return dhe_mcs;
            }
            case Dhe_mcs_L1 -> {
                if (dhe_mcs_lag1 != null && dhe_mcs_lag1 >= 0.) {
                    return dhe_mcs_lag1;
                } else return 0.;
            }
            case Dhe_mcs_sq -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return dhe_mcs * dhe_mcs;
            }
            case Dhe_mcs_c -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return dhe_mcs - 51.5;
            }
            case Dhe_mcs_c_sq -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return Math.pow(dhe_mcs - 51.5, 2);
            }
            case Dhe_mcs_times_pcs -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return dhe_mcs * dhe_pcs;
            }
            case Dhe_mcs_c_times_pcs_c -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return (dhe_mcs - 51.5) * (dhe_pcs - 49.9);
            }
            case Dhe_pcs -> {
                return dhe_pcs;
            }
            case Dhe_pcs_L1 -> {
                if (dhe_pcs_lag1 != null && dhe_pcs_lag1 >= 0.) {
                    return dhe_pcs_lag1;
                } else return 0.;
            }
            case Dhe_pcs_sq -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return dhe_pcs * dhe_pcs;
            }
            case Dhe_pcs_cb -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return dhe_pcs * dhe_pcs * dhe_pcs;
            }
            case Dhe_pcs_c -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return dhe_pcs - 49.9;
            }
            case Dhe_pcs_c_sq -> {
                // Used to calculate he_eq5d in regHealthEQ5D
                return Math.pow(dhe_pcs - 49.9, 2);
            }
            case Dls -> {
                return dls;
            }
            case Dls_L1 -> {
                if (dls_lag1 != null && dls_lag1 >= 0.) {
                    return dls_lag1;
                } else return 0.;
            }
            case Dhmghq_L1 -> {
                return (getDhmGhq_lag1()) ? 1. : 0.;
            }
            case Dhesp_L1 -> {
                return (dhesp_lag1 != null) ? (double) dhesp_lag1.getValue() : 0.0;
            }
            case Ded -> {
                return (Indicator.True.equals(ded)) ? 1.0 : 0.0;
            }
            case Deh_c3_High -> {
                return (Education.High.equals(deh_c3)) ? 1.0 : 0.0;
            }
            case Deh_c3_Medium -> {
                return (Education.Medium.equals(deh_c3)) ? 1.0 : 0.0;
            }
            case Deh_c3_Medium_L1 -> {
                return (Education.Medium.equals(deh_c3_lag1)) ? 1.0 : 0.0;
            }
            case Deh_c3_Low -> {
                return (Education.Low.equals(deh_c3)) ? 1.0 : 0.0;
            }
            case Deh_c3_Low_L1 -> {
                return (Education.Low.equals(deh_c3_lag1)) ? 1.0 : 0.0;
            }
            case Dehm_c3_High -> {
                return (Education.High.equals(dehm_c3)) ? 1.0 : 0.0;
            }
            case Dehm_c3_Medium -> {
                return (Education.Medium.equals(dehm_c3)) ? 1.0 : 0.0;
            }
            case Dehm_c3_Low -> {
                return (Education.Low.equals(dehm_c3)) ? 1.0 : 0.0;
            }
            case Dehf_c3_High -> {
                return (Education.High.equals(dehf_c3)) ? 1.0 : 0.0;
            }
            case Dehf_c3_Medium -> {
                return (Education.Medium.equals(dehf_c3)) ? 1.0 : 0.0;
            }
            case Dehf_c3_Low -> {
                return (Education.Low.equals(dehf_c3)) ? 1.0 : 0.0;
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
                return (Education.Medium.equals(dehsp_c3_lag1)) ? 1. : 0.;
            }
            case Dehsp_c3_Low_L1 -> {
                return (Education.Low.equals(dehsp_c3_lag1)) ? 1. : 0.;
            }
            case He_eq5d -> {
                return getHe_eq5d();
            }
            case Dhhtp_c4_CoupleChildren_L1 -> {
                return (Dhhtp_c4.CoupleChildren.equals(getDhhtp_c4_lag1())) ? 1.0 : 0.0;
            }
            case Dhhtp_c4_CoupleNoChildren_L1 -> {
                return (Dhhtp_c4.CoupleNoChildren.equals(getDhhtp_c4_lag1())) ? 1.0 : 0.0;
            }
            case Dhhtp_c4_SingleNoChildren_L1 -> {
                return (Dhhtp_c4.SingleNoChildren.equals(getDhhtp_c4_lag1())) ? 1.0 : 0.0;
            }
            case Dhhtp_c4_SingleChildren_L1 -> {
                return (Dhhtp_c4.SingleChildren.equals(getDhhtp_c4_lag1())) ? 1.0 : 0.0;
            }
            case Dlltsd -> {
                return Indicator.True.equals(dlltsd) ? 1. : 0.;
            }
            case Dlltsd_L1 -> {
                return Indicator.True.equals(dlltsd_lag1) ? 1. : 0.;
            }
            case EthnicityWhite -> {
                return dot.equals(Ethnicity.White) ? 1. : 0.;
            }
            case EthnicityMixed -> {
                return dot.equals(Ethnicity.Mixed) ? 1. : 0.;
            }
            case EthnicityAsian -> {
                return dot.equals(Ethnicity.Asian) ? 1. : 0.;
            }
            case EthnicityBlack -> {
                return dot.equals(Ethnicity.Black) ? 1. : 0.;
            }
            case EthnicityOther -> {
                return dot.equals(Ethnicity.Other) ? 1. : 0.;
            }
            case FertilityRate -> {
                if (ioFlag)
                    return Parameters.getFertilityProjectionsByYear(getYear());
                else
                    return Parameters.getFertilityRateByRegionYear(getRegion(), getYear());
            }
            case Female -> {
                return dgn.equals(Gender.Female) ? 1. : 0.;
            }
            case FinancialDistress -> {
                return financialDistress ? 1. : 0.;
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
                if (les_c4_lag1 != null)        //Problem will null pointer exceptions for those who are inactive and then become active as their lagged employment status is null!
                    return les_c4_lag1.equals(Les_c4.EmployedOrSelfEmployed) ? 1. : 0.;
                else
                    return 0.;
            }            //A person who was not active but has become active in this year should have an employment_status_lag == null.  In this case, we assume this means 0 for the Employment regression, where Lemployed is used.
            case Lnonwork -> {
                return (les_c4_lag1.equals(Les_c4.NotEmployed) || les_c4_lag1.equals(Les_c4.Retired)) ? 1. : 0.;
            }
            case Lstudent -> {
                //			log.debug("Lstudent");
                return les_c4_lag1.equals(Les_c4.Student) ? 1. : 0.;
            }
            case Lunion -> {
                //			log.debug("Lunion");
                return household_status_lag.equals(HouseholdStatus.Couple) ? 1. : 0.;
            }
            case Les_c3_Student_L1 -> {
                return (Les_c4.Student.equals(les_c4_lag1)) ? 1.0 : 0.0;
            }
            case Les_c3_NotEmployed_L1 -> {
                return ((Les_c4.NotEmployed.equals(les_c4_lag1)) || (Les_c4.Retired.equals(les_c4_lag1))) ? 1.0 : 0.0;
            }
            case Les_c3_Employed_L1 -> {
                return (Les_c4.EmployedOrSelfEmployed.equals(les_c4_lag1)) ? 1.0 : 0.0;
            }
            case Les_c3_Sick_L1 -> {
                if (dlltsd_lag1 != null)
                    return dlltsd_lag1.equals(Indicator.True) ? 1. : 0.;
                else
                    return 0.0;
            }
            case Lessp_c3_Student_L1 -> {
                Person partner = getPartner();
                if (partner != null && partner.les_c4_lag1 != null)
                    return partner.les_c4_lag1.equals(Les_c4.Student) ? 1. : 0.;
                else
                    return 0.;
            }
            case Lessp_c3_NotEmployed_L1 -> {
                Person partner = getPartner();
                if (partner != null && partner.les_c4_lag1 != null)
                    return (partner.les_c4_lag1.equals(Les_c4.NotEmployed) || partner.les_c4_lag1.equals(Les_c4.Retired)) ? 1. : 0.;
                else
                    return 0.;
            }
            case Lessp_c3_Sick_L1 -> {
                Person partner = getPartner();
                if (partner != null && partner.dlltsd_lag1 != null)
                    return partner.dlltsd_lag1.equals(Indicator.True) ? 1. : 0.;
                else
                    return 0.;
            }
            case Retired -> {
                return Les_c4.Retired.equals(les_c4) ? 1. : 0.;
            }
            case Lesdf_c4_EmployedSpouseNotEmployed_L1 -> {                    //Own and partner's activity status lag(1)
                return (Lesdf_c4.EmployedSpouseNotEmployed.equals(lesdf_c4_lag1)) ? 1. : 0.;
            }
            case Lesdf_c4_NotEmployedSpouseEmployed_L1 -> {
                return (Lesdf_c4.NotEmployedSpouseEmployed.equals(lesdf_c4_lag1)) ? 1. : 0.;
            }
            case Lesdf_c4_BothNotEmployed_L1 -> {
                if (lesdf_c4_lag1 != null)
                    return lesdf_c4_lag1.equals(Lesdf_c4.BothNotEmployed) ? 1. : 0.;
                else
                    return 0.;
            }
            case Liwwh -> {
                return (double) liwwh;
            }
            case NotEmployed_L1 -> {
                return (les_c4_lag1.equals(Les_c4.NotEmployed)) ? 1. : 0.;
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
                        Parameters.getStandardNormalDistribution().inverseCumulativeProbability(innovations.getDoubleDraw(20)) : 0.0;
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
                return getUnemploymentRateByGenderEducationAgeYear(getDgn(), getDeh_c3(), getDag(), getYear());
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
            case Year2020 -> {
                return (getYear() == 2020) ? 1. : 0.;
            }
            case Year2021 -> {
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
            case Ydses_c5_Q2_L1 -> {
                return (Ydses_c5.Q2.equals(getYdses_c5_lag1())) ? 1.0 : 0.0;
            }
            case Ydses_c5_Q3_L1 -> {
                return (Ydses_c5.Q3.equals(getYdses_c5_lag1())) ? 1.0 : 0.0;
            }
            case Ydses_c5_Q4_L1 -> {
                return (Ydses_c5.Q4.equals(getYdses_c5_lag1())) ? 1.0 : 0.0;
            }
            case Ydses_c5_Q5_L1 -> {
                return (Ydses_c5.Q5.equals(getYdses_c5_lag1())) ? 1.0 : 0.0;
            }
            case Ypnbihs_dv_L1 -> {
                if (ypnbihs_dv_lag1 != null) {
                    return ypnbihs_dv_lag1;
                } else {
                    throw new RuntimeException("call to uninitialised ypnbihs_dv_lag1 in Person");
                }
            }
            case Ypnbihs_dv_L1_sq -> {
                if (ypnbihs_dv_lag1 != null) {
                    return ypnbihs_dv_lag1 * ypnbihs_dv_lag1;
                } else {
                    throw new RuntimeException("call to uninitialised ypnbihs_dv_lag1 in Person");
                }
            }
            case Ynbcpdf_dv_L1 -> {
                return (ynbcpdf_dv_lag1 != null) ? ynbcpdf_dv_lag1 : 0.0;
            }
            case Yptciihs_dv_L1 -> {
                return yptciihs_dv_lag1;
            }
            case Yptciihs_dv_L2 -> {
                return yptciihs_dv_lag2;
            }
            case Yptciihs_dv_L3 -> {
                return yptciihs_dv_lag3;
            }
            case Ypncp_L1 -> {
                return ypncp_lag1;
            }
            case Ypncp_L2 -> {
                return ypncp_lag2;
            }
            case Ypnoab_L1 -> {
                return ypnoab_lag1;
            }
            case Ypnoab_L2 -> {
                return ypnoab_lag2;
            }
            case Yplgrs_dv_L1 -> {
                return yplgrs_dv_lag1;
            }
            case Yplgrs_dv_L2 -> {
                return yplgrs_dv_lag2;
            }
            case Yplgrs_dv_L3 -> {
                return yplgrs_dv_lag3;
            }
            case Ld_children_3underIT -> {
                return model.getCountry().equals(Country.IT) ? benefitUnit.getIndicatorChildren03_lag1().ordinal() : 0.;
            }
            case Ld_children_4_12IT -> {
                return model.getCountry().equals(Country.IT) ? benefitUnit.getIndicatorChildren412_lag1().ordinal() : 0.;
            }
            case LunionIT -> {
                return (household_status_lag.equals(HouseholdStatus.Couple) && (getRegion().toString().startsWith(Country.IT.toString()))) ? 1. : 0.;
            }
            case EduMediumIT -> {
                return (deh_c3.equals(Education.Medium) && (getRegion().toString().startsWith(Country.IT.toString()))) ? 1. : 0.;
            }
            case EduHighIT -> {
                return (deh_c3.equals(Education.High) && (getRegion().toString().startsWith(Country.IT.toString()))) ? 1. : 0.;
            }

            case Reached_Retirement_Age -> {
                int retirementAge;
                if (dgn.equals(Gender.Female)) {
                    retirementAge = (int) Parameters.getTimeSeriesValue(getYear(), Gender.Female.toString(), TimeSeriesVariable.FixedRetirementAge);
                } else {
                    retirementAge = (int) Parameters.getTimeSeriesValue(getYear(), Gender.Male.toString(), TimeSeriesVariable.FixedRetirementAge);
                }
                return (dag >= retirementAge) ? 1. : 0.;
            }
            case Reached_Retirement_Age_Sp -> {
                int retirementAgePartner;
                Person partner = getPartner();
                if (partner != null) {
                    if (partner.dgn.equals(Gender.Female)) {
                        retirementAgePartner = (int) Parameters.getTimeSeriesValue(getYear(), Gender.Female.toString(), TimeSeriesVariable.FixedRetirementAge);
                    } else {
                        retirementAgePartner = (int) Parameters.getTimeSeriesValue(getYear(), Gender.Male.toString(), TimeSeriesVariable.FixedRetirementAge);
                    }
                    return (partner.dag >= retirementAgePartner) ? 1. : 0.;
                } else {
                    return 0.;
                }
            }
            case Reached_Retirement_Age_Les_c3_NotEmployed_L1 -> { //Reached retirement age and was not employed in the previous year
                int retirementAge;
                if (dgn.equals(Gender.Female)) {
                    retirementAge = (int) Parameters.getTimeSeriesValue(getYear(), Gender.Female.toString(), TimeSeriesVariable.FixedRetirementAge);
                } else {
                    retirementAge = (int) Parameters.getTimeSeriesValue(getYear(), Gender.Male.toString(), TimeSeriesVariable.FixedRetirementAge);
                }
                return ((dag >= retirementAge) && (les_c4_lag1.equals(Les_c4.NotEmployed) || les_c4_lag1.equals(Les_c4.Retired))) ? 1. : 0.;
            }
            case EquivalisedIncomeYearly -> {
                return getBenefitUnit().getEquivalisedDisposableIncomeYearly();
            }
            case EquivalisedConsumptionYearly -> {
                if (yearlyEquivalisedConsumption != null) {
                    return yearlyEquivalisedConsumption;
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
                return (les_c4_lag1.equals(Les_c4.EmployedOrSelfEmployed) && les_c4.equals(Les_c4.NotEmployed) && dlltsd.equals(Indicator.False)) ? 1. : 0.;
            }
            case UnemployedToEmployed -> {
                return (les_c4_lag1.equals(Les_c4.NotEmployed) && dlltsd_lag1.equals(Indicator.False) && les_c4.equals(Les_c4.EmployedOrSelfEmployed)) ? 1. : 0.;
            }
            case PersistentUnemployed -> {
                return (les_c4.equals(Les_c4.NotEmployed) && les_c4_lag1.equals(Les_c4.NotEmployed) && dlltsd.equals(Indicator.False) && dlltsd_lag1.equals(Indicator.False)) ? 1. : 0.;
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
                return getBenefitUnit().isDhhOwned() ? 1. : 0.;
            } // Evaluated at the level of a benefit unit. If required, can be changed to individual-level homeownership status.
            case D_Home_owner_L1 -> {
                return isHomeOwner_lag1 ? 1. : 0.;
            } // Evaluated at the level of a benefit unit. If required, can be changed to individual-level homeownership status.
            case Covid_2020_D -> {
                return (getYear() == 2020) ? 1. : 0.;
            }
            case Covid_2021_D -> {
                return (getYear() == 2021) ? 1. : 0.;
            }
            case Pt -> {
                return (getLabourSupplyHoursWeekly() > 0 && getLabourSupplyHoursWeekly() < Parameters.MIN_HOURS_FULL_TIME_EMPLOYED) ? 1. : 0.;
            }
            case L1_log_hourly_wage -> {
                if (L1_fullTimeHourlyEarningsPotential == null) {
                    throw new RuntimeException("call to evaluate lag potential hourly earnings before initialisation");
                } else {
                    return Math.log(L1_fullTimeHourlyEarningsPotential);
                }
            }
            case L1_log_hourly_wage_sq -> {
                if (L1_fullTimeHourlyEarningsPotential > 0) {
                    return Math.pow(Math.log(L1_fullTimeHourlyEarningsPotential), 2);
                } else {
                    throw new RuntimeException("call to evaluate lag potential hourly earnings before initialisation");
                }
            }
            case L1_hourly_wage -> {
                if (L1_fullTimeHourlyEarningsPotential > 0) {
                    return L1_fullTimeHourlyEarningsPotential;
                } else {
                    throw new RuntimeException("call to evaluate lag potential hourly earnings before initialisation");
                }
            }
            case Deh_c3_Low_Dag -> {
                return (Education.Low.equals(deh_c3)) ? dag : 0.0;
            }
            case Deh_c3_Medium_Dag -> {
                return (Education.Medium.equals(deh_c3)) ? dag : 0.0;
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
                if (dgn.equals(Gender.Male)) {
                    return (double) dag;
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
                if (getNewWorkHours_lag1() != null) {
                    return getNewWorkHours_lag1();
                } else return 0.;
            }
            case Dgn_Lhw_L1 -> {
                if (getNewWorkHours_lag1() != null && dgn.equals(Gender.Male)) {
                    return getNewWorkHours_lag1();
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
                return getCovidModuleGrossLabourIncome_lag1();
            }
            case Covid19ReceivesSEISS_L1 -> {
                return (getCovidModuleReceivesSEISS().equals(Indicator.True)) ? 1. : 0.;
            }
            case Les_c7_Covid_Furlough_L1 -> {
                return (getLes_c7_covid_lag1().equals(Les_c7_covid.FurloughedFlex) || getLes_c7_covid_lag1().equals(Les_c7_covid.FurloughedFull)) ? 1. : 0.;
            }
            case Blpay_Q2 -> {
                return (getCovidModuleGrossLabourIncomeBaseline_Xt5().equals(Quintiles.Q2)) ? 1. : 0.;
            }
            case Blpay_Q3 -> {
                return (getCovidModuleGrossLabourIncomeBaseline_Xt5().equals(Quintiles.Q3)) ? 1. : 0.;
            }
            case Blpay_Q4 -> {
                return (getCovidModuleGrossLabourIncomeBaseline_Xt5().equals(Quintiles.Q4)) ? 1. : 0.;
            }
            case Blpay_Q5 -> {
                return (getCovidModuleGrossLabourIncomeBaseline_Xt5().equals(Quintiles.Q5)) ? 1. : 0.;
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

    public int getDag() {
        return dag;
    }

    public void setDag(Integer dag) {
        this.dag = dag;
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

    public int getStudent() {
        return Les_c4.Student.equals(les_c4)? 1 : 0;
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
        return deh_c3;
    }

    public void setDeh_c3(Education deh_c3) {
         this.deh_c3 = deh_c3;
     }

    public void setDeh_c3_lag1(Education deh_c3_lag1) {
         this.deh_c3_lag1 = deh_c3_lag1;
     }

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
        return (Les_c4.EmployedOrSelfEmployed.equals(les_c4)) ? 1 : 0;
    }

    public int getNonwork() {
        return (Les_c4.NotEmployed.equals(les_c4)) ? 1 : 0;
    }

    public int getEmployed_Lag1() {
        return (Les_c4.EmployedOrSelfEmployed.equals(les_c4_lag1)) ? 1 : 0;
    }

    public int getNonwork_Lag1() {
        return (Les_c4.NotEmployed.equals(les_c4_lag1)) ? 1 : 0;
    }

    public void setRegionLocal(Region region) {
        regionLocal = region;
    }

    public Region getRegion() {
        if (benefitUnit == null) {
            if (regionLocal==null)
                throw new RuntimeException("attempt to access regionLocal before it has been assigned");
            return regionLocal;
        } else {
            return benefitUnit.getRegion();
        }
    }

    public void setRegion(Region region) {
        this.benefitUnit.setRegion(region);
    }

    public HouseholdStatus getHousehold_status_lag() {
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

        if (benefitUnit!=null && !benefitUnit.equals(newBenefitUnit)) {
            benefitUnit.removeMember(this);
        }

        benefitUnit = newBenefitUnit;
        idBenefitUnit = benefitUnit.getId();
        if (newBenefitUnit == null)
            idHousehold = null;
        else  {
            if (newBenefitUnit.getHousehold()==null)
                throw new RuntimeException("problem identifying household of benefit unit");
            idHousehold = newBenefitUnit.getHousehold().getId();
            benefitUnit.getMembers().add(this);
        }
    }

    public Person getPartner() {

        if (dag >= Parameters.AGE_TO_BECOME_RESPONSIBLE) {

            for (Person member : benefitUnit.getMembers()) {

                boolean accept = true;
                if (member==this || member.getDag()<Parameters.AGE_TO_BECOME_RESPONSIBLE)
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

    private void nullPartnerVariables() {

        careHoursFromPartnerWeekly = 0.0;
        dcpyy = 0;
        if (SocialCareProvision.OnlyPartner.equals(socialCareProvision))
            socialCareProvision = SocialCareProvision.None;
        else if (SocialCareProvision.PartnerAndOther.equals(socialCareProvision))
            socialCareProvision = SocialCareProvision.OnlyOther;
    }

    public Labour getLabourSupplyWeekly() {
        if (labourSupplyWeekly==null)
            throw new RuntimeException("request for labourSupplyWeekly before it has been initialised");
        return labourSupplyWeekly;
    }

    public int getL1LabourSupplyHoursWeekly() {
        if (labourSupplyWeekly_L1==null)
            throw new RuntimeException("request for labourSupplyWeekly_L1 before it has been initialised");
        return labourSupplyWeekly_L1.getHours(this);
    }

    public int getLabourSupplyHoursWeekly() {
        return (labourSupplyWeekly != null) ? labourSupplyWeekly.getHours(this) : 0;
    }

    public double getDoubleLabourSupplyHoursWeekly() {
        // this method is needed for the stupid observer
        return (double)getLabourSupplyHoursWeekly();
    }

    public void setLabourSupplyWeekly(Labour labourSupply) {
        labourSupplyWeekly = labourSupply;
        hoursWorkedWeekly = getLabourSupplyHoursWeekly(); // Update number of hours worked weekly
    }

    public double getLabourSupplyHoursYearly() {
        return (double) getLabourSupplyHoursWeekly() * Parameters.WEEKS_PER_YEAR;
    }

    public double getScaledLabourSupplyYearly() {
        return getLabourSupplyHoursYearly() * model.getScalingFactor();
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

    public Dhe getDhe() {
        return dhe;
    }

    public void setDhe(Dhe health) {
        this.dhe = health;
    }

    public double getDheValue() {
        return (double)dhe.getValue();
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
    public Integer getDls() {
        int val;
        if (dls == null) {
            val = -1;
        } else {
            val = dls;
        }
        return val;
    }
    public double getDhe_mcs() {
        double val;
        if (dhe_mcs == null) {
            val = -1.0;
        } else {
            val = dhe_mcs;
        }
        return val;
    }
    public double getDhe_pcs() {
        double val;
        if (dhe_pcs == null) {
            val = -1.0;
        } else {
            val = dhe_pcs;
        }
        return val;
    }


    public void setDhe_mcs(Double dhe_mcs) {
        this.dhe_mcs = dhe_mcs;
    }

    public void setDhe_pcs(Double dhe_pcs) {
        this.dhe_pcs = dhe_pcs;
    }

    public void populateSocialCareReceipt(SocialCareReceiptState state) {
        if (SocialCareReceiptState.NoFormal.equals(state)) {
            needSocialCare = Indicator.True;
            socialCareReceipt = SocialCareReceipt.Informal;
            careHoursFromOtherWeekly = 10.0;
            socialCareFromOther = true;
        } else if (SocialCareReceiptState.Mixed.equals(state)) {
            needSocialCare = Indicator.True;
            socialCareReceipt = SocialCareReceipt.Mixed;
            careHoursFromOtherWeekly = 10.0;
            careHoursFromFormalWeekly = 10.0;
            careFormalExpenditureWeekly = 100.0;
            socialCareFromFormal = true;
            socialCareFromOther = true;
        } else if (SocialCareReceiptState.Formal.equals(state)) {
            needSocialCare = Indicator.True;
            socialCareReceipt = SocialCareReceipt.Formal;
            careHoursFromFormalWeekly = 10.0;
            careFormalExpenditureWeekly = 100.0;
            socialCareFromFormal = true;
        }
    }

    public void populateSocialCareReceipt_lag1(SocialCareReceiptState state) {
        if (SocialCareReceiptState.NoFormal.equals(state)) {
            needSocialCare_lag1 = Indicator.True;
            careHoursFromOtherWeekly_lag1 = 10.0;
        } else if (SocialCareReceiptState.Mixed.equals(state)) {
            needSocialCare_lag1 = Indicator.True;
            careHoursFromOtherWeekly_lag1 = 10.0;
            careHoursFromFormalWeekly_lag1 = 10.0;
        } else if (SocialCareReceiptState.Formal.equals(state)) {
            needSocialCare_lag1 = Indicator.True;
            careHoursFromFormalWeekly_lag1 = 10.0;
        }
    }

    public void setSocialCareFromOther(boolean val) {
        socialCareFromOther = val;
    }

    public void setCareHoursFromOtherWeekly_lag1(double val) {
        careHoursFromOtherWeekly_lag1 = val;
    }

    public void setCareHoursFromFormalWeekly_lag1(double val) {
        careHoursFromFormalWeekly_lag1 = val;
    }
    public void setSocialCareProvision_lag1(SocialCareProvision careProvision) {
        socialCareProvision_lag1 = careProvision;
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

    public boolean getDhmGhq() {
        return dhmGhq;
    }

    public void setDhmGhq(boolean dhm_ghq) {
        this.dhmGhq = dhm_ghq;
    }

    public Ethnicity getDot() {
        return dot;
    }

    public boolean getFinancialDistress() {
        return financialDistress;
    }

    public Indicator getNeedSocialCare() {
        return needSocialCare;
    }

    public void setNeedSocialCare(Indicator needSocialCare) {
        this.needSocialCare = needSocialCare;
    }

    public void setDer(Indicator der) {
        this.der = der;
    }

    public Long getIdOriginalPerson() {
        return idOriginalPerson;
    }

    public Long getIdOriginalBU() {
        return idOriginalBU;
    }

    public Long getIdOriginalHH() {
        return idOriginalHH;
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

    public Dcpst getDcpst() {
        if (benefitUnit==null) {
            if (dcpstLocal==null)
                throw new RuntimeException("attempt to access unassigned value for dcpstLocal");
            return dcpstLocal;
        }
        if (getPartner()!=null)
            return Dcpst.Partnered;
        if (Dcpst.Partnered.equals(dcpst_lag1))
            return Dcpst.PreviouslyPartnered;
        return Dcpst.SingleNeverMarried;
    }

    public void setDcpstLocal(Dcpst dcpst) {
        this.dcpstLocal = dcpst;
    }

    public Indicator getDlltsd() {
        return dlltsd;
    }
    public void setDlltsd(Indicator dlltsd) {
        this.dlltsd = dlltsd;
    }

    public void setSocialCareReceipt(SocialCareReceipt who) {
        socialCareReceipt = who;
    }

    public void setSocialCareProvision(SocialCareProvision who) {
        socialCareProvision = who;
    }

    public Indicator getDlltsd_lag1() {
        return dlltsd_lag1;
    }

    public void setDlltsd_lag1(Indicator dlltsd_lag1) {
        this.dlltsd_lag1 = dlltsd_lag1;
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

    public Integer getDcpyy() {
        return dcpyy;
    }

    public void setDcpyy(Integer dcpyy) {
        this.dcpyy = dcpyy;
    }

    public Integer getDcpagdf() {
        Person partner = getPartner();
        if (partner!=null)
            return (dag - partner.dag);
        else
            return null;
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
        return (yplgrs_dv!=null) ? yplgrs_dv : 0.0;
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

    public Double getYnbcpdf_dv_lag1() {
        return ynbcpdf_dv_lag1;
    }

    public Lesdf_c4 getLesdf_c4() {
        if (benefitUnit.getCoupleBoolean() && dag>=Parameters.AGE_TO_BECOME_RESPONSIBLE) {
            if (getPartner()==null)
                throw new RuntimeException("inconsistency between couple and partner identifiers");
            if (Les_c4.EmployedOrSelfEmployed.equals(les_c4) && Les_c4.EmployedOrSelfEmployed.equals(getPartner().les_c4))
                return Lesdf_c4.BothEmployed;
            else if (Les_c4.EmployedOrSelfEmployed.equals(les_c4))
                return Lesdf_c4.EmployedSpouseNotEmployed;
            else if (Les_c4.EmployedOrSelfEmployed.equals(getPartner().les_c4))
                return Lesdf_c4.NotEmployedSpouseEmployed;
            else
                return Lesdf_c4.BothNotEmployed;
        }
        return null;
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

    public void setYpnbihs_dv_lag1(Double val) {
        ypnbihs_dv_lag1 = val;
    }

    public void setDehsp_c3_lag1(Education dehsp_c3_lag1) {
        this.dehsp_c3_lag1 = dehsp_c3_lag1;
    }

    public void setDhesp_lag1(Dhe dhesp_lag1) {
        this.dhesp_lag1 = dhesp_lag1;
    }

    public void setYnbcpdf_dv_lag1(Double val) {
        ynbcpdf_dv_lag1 = val;
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
        return toBePartnered != null && toBePartnered;
    }

    public void setToBePartnered(boolean toBePartnered) {
        this.toBePartnered = toBePartnered;
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
        return (covidModuleGrossLabourIncome_Baseline!=null) ? covidModuleGrossLabourIncome_Baseline : 0.0;
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

    public boolean isDhhOwned() {
        return dhhOwned;
    }

    public void setDhhOwned(boolean dhh_owned) {
        this.dhhOwned = dhh_owned;
    }

    public boolean isReceivesBenefitsFlag() {
        return receivesBenefitsFlag;
    }

    public void setReceivesBenefitsFlag(boolean receivesBenefitsFlag) {
        this.receivesBenefitsFlag = receivesBenefitsFlag;
    }

    public boolean isReceivesBenefitsFlag_L1() {
        return (receivesBenefitsFlag_L1!=null) ? receivesBenefitsFlag_L1 : false;
    }

    public void setReceivesBenefitsFlag_L1(boolean receivesBenefitsFlag_L1) {
        this.receivesBenefitsFlag_L1 = receivesBenefitsFlag_L1;
    }

    public boolean isReceivesBenefitsFlagUC() {
        return receivesBenefitsFlagUC;
    }

    public void setReceivesBenefitsFlagUC(boolean receivesBenefitsFlagUC) {
        this.receivesBenefitsFlagUC = receivesBenefitsFlagUC;
    }

    public boolean isReceivesBenefitsFlagUC_L1() {
        return (null != receivesBenefitsFlagUC_L1) ? receivesBenefitsFlagUC_L1 : false;
    }

    public void setReceivesBenefitsFlagUC_L1(boolean receivesBenefitsFlagUC_L1) {
        this.receivesBenefitsFlagUC_L1 = receivesBenefitsFlagUC_L1;
    }

    public boolean isReceivesBenefitsFlagNonUC() {
        return receivesBenefitsFlagNonUC;
    }

    public void setReceivesBenefitsFlagNonUC(boolean receivesBenefitsFlagNonUC) {
        this.receivesBenefitsFlagNonUC = receivesBenefitsFlagNonUC;
    }

    public boolean isReceivesBenefitsFlagNonUC_L1() {
        return (null != receivesBenefitsFlagNonUC_L1) ? receivesBenefitsFlagNonUC_L1 : false;
    }

    public void setReceivesBenefitsFlagNonUC_L1(boolean receivesBenefitsFlagNonUC_L1) {
        this.receivesBenefitsFlagNonUC_L1 = receivesBenefitsFlagNonUC_L1;
    }


    public double getEquivalisedDisposableIncomeYearly() {
        return benefitUnit.getEquivalisedDisposableIncomeYearly();
    }

    public double getDisposableIncomeMonthly() { return benefitUnit.getDisposableIncomeMonthly();}

    public double getWageOffer() {
        if (lowWageOffer)
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
        if (Indicator.False.equals(needSocialCare))
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
        if (socialCareProvision==null)
            return SocialCareProvision.None;
        else
            return socialCareProvision;
    }

    public double getRetired() {
        return (Les_c4.Retired.equals(getLes_c4())) ? 1.0 : 0.0;
    }

    public void setYearLocal(Integer yearLocal) {
        this.yearLocal = yearLocal;
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
    private int getStartYear() {
        if (model != null) {
            return model.getStartYear();
        } else {
            return 0;
        }
    }

    public void setNumberChildren017Local(Integer nbr) {
        numberChildren017Local = nbr;
    }
    public void setIndicatorChildren02Local(Indicator idctr) {
        indicatorChildren02Local = idctr;
    }

    private Ydses_c5 getYdses_c5_lag1() {
        if (model!=null) {
            if (benefitUnit==null)
                throw new RuntimeException("attempt to access unassigned benefit unit");
            return benefitUnit.getYdses_c5_lag1();
        } else {
            if (ydses_c5_lag1Local==null)
                throw new RuntimeException("attempt to access unassigned ydses_c5_lag1Local");
            return ydses_c5_lag1Local;
        }
    }

    public void setYdses_c5_lag1Local(Ydses_c5 ydses_c5_lag1) {
        ydses_c5_lag1Local = ydses_c5_lag1;
    }

    private Dhhtp_c4 getDhhtp_c4_lag1() {
        if (model!=null) {
            if (benefitUnit==null)
                throw new RuntimeException("attempt to access unassigned benefit unit");
            return benefitUnit.getDhhtp_c4_lag1();
        } else {
            if (dhhtp_c4_lag1Local==null)
                throw new RuntimeException("attempt to access unassigned dhhtp_c4_lag1Local");
            return dhhtp_c4_lag1Local;
        }
    }

    public void setDhhtp_c4_lag1Local(Dhhtp_c4 dhhtp_c4_lag1) {
        dhhtp_c4_lag1Local = dhhtp_c4_lag1;
    }

    private Integer getNumberChildrenAll_lag1() {
        if (benefitUnit != null) {
            return (benefitUnit.getNumberChildrenAll_lag1() != null) ? benefitUnit.getNumberChildrenAll_lag1() : 0;
        } else {
            return (numberChildrenAllLocal_lag1==null) ? 0 : numberChildrenAllLocal_lag1;
        }
    }

    public void setNumberChildrenAllLocal(Integer nbr) {
        numberChildrenAllLocal = nbr;
    }

    private Integer getNumberChildrenAll() {
        if (model != null) {
            if (benefitUnit==null)
                throw new RuntimeException("attempt to access unassigned benefit unit");
            return benefitUnit.getNumberChildrenAll();
        } else {
            if (numberChildrenAllLocal==null)
                throw new RuntimeException("attempt to access unassigned numberChildrenAllLocal");
            return numberChildrenAllLocal;
        }
    }

    public void setNumberChildrenAllLocal_lag1(Integer nbr) {
        numberChildrenAllLocal_lag1 = nbr;
    }

    public void setNumberChildren02Local_lag1(Integer nbr) {
        numberChildren02Local_lag1 = nbr;
    }

    private Integer getNumberChildren02_lag1() {
        if (model != null) {
            if (benefitUnit==null)
                throw new RuntimeException("attempt to access unassigned benefit unit");
            return benefitUnit.getNumberChildren02_lag1();
        } else {
            if (numberChildren02Local_lag1==null)
                throw new RuntimeException("attempt to access unassigned numberChildren02Local_lag1");
            return numberChildren02Local_lag1;
        }
    }

    private Integer getNumberChildren017() {
        if (model != null) {
            if (benefitUnit==null)
                throw new RuntimeException("attempt to access unassigned benefit unit");
            return benefitUnit.getNumberChildren(0,17);
        } else {
            if (numberChildren017Local==null)
                throw new RuntimeException("attempt to access unassigned numberChildren017Local");
            return numberChildren017Local;
        }
    }

    private Double getInverseMillsRatio() {

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
            if(Gender.Male.equals(dgn)) {
                if (inverseMillsRatio > inverseMillsRatioMaxMale) {
                    inverseMillsRatioMaxMale = inverseMillsRatio;
                }
                if (inverseMillsRatio < inverseMillsRatioMinMale) {
                    inverseMillsRatioMinMale = inverseMillsRatio;
                }
            } else {
                if (inverseMillsRatio > inverseMillsRatioMaxFemale) {
                    inverseMillsRatioMaxFemale = inverseMillsRatio;
                }
                if (inverseMillsRatio < inverseMillsRatioMinFemale) {
                    inverseMillsRatioMinFemale = inverseMillsRatio;
                }
            }
        } else {
            log.debug("inverse Mills ratio is not finite, return 0 instead!!!   IMR: " + inverseMillsRatio + ", score: " + score/* + ", num: " + num + ", denom: " + denom*/ + ", age: " + dag + ", gender: " + dgn + ", education " + deh_c3 + ", activity_status from previous time-step " + les_c4);
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
            return fullTimeHourlyEarningsPotential;
        } else {
            double ptPremium;
            if (les_c4_lag1.equals(Les_c4.EmployedOrSelfEmployed)) {
                if (Gender.Male.equals(dgn)) {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionName.WagesMalesE, "Pt");
                } else {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionName.WagesFemalesE, "Pt");
                }
            } else {
                if (Gender.Male.equals(dgn)) {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionName.WagesMalesNE, "Pt");
                } else {
                    ptPremium = ManagerRegressions.getRegressionCoeff(RegressionName.WagesFemalesNE, "Pt");
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
    public Integer getNumberChildren017Local() {
        return numberChildren017Local;
    }
    public Integer getNumberChildrenAllLocal() {
        return numberChildrenAllLocal;
    }
    public Indicator getIndicatorChildren02Local() {
        return indicatorChildren02Local;
    }
    public Map<Labour, Integer> getPersonContinuousHoursLabourSupplyMap() {
        return personContinuousHoursLabourSupplyMap;
    }
    public void setPersonContinuousHoursLabourSupplyMap(Map<Labour, Integer> personContinuousHoursLabourSupplyMap) {
        this.personContinuousHoursLabourSupplyMap = personContinuousHoursLabourSupplyMap;
    }
    public double getLabourSupplySingleDraw() {
        return innovations.getSingleDrawDoubleInnov(0);
    }
    public double getBenefitUnitRandomUniform() {return innovations.getDoubleDraw(31);}

    public double getHoursFormalSocialCare_L1() {
        return (careHoursFromFormalWeekly_lag1 > 0.0) ? careHoursFromFormalWeekly_lag1 : 0.0;
    }

    public double getHoursFormalSocialCare() {
        double hours = 0.0;
        if (careHoursFromFormalWeekly !=null)
            if (careHoursFromFormalWeekly >0.0)
                hours = careHoursFromFormalWeekly;
        return hours;
    }

    public double getHoursInformalSocialCare() {
        return getCareHoursFromPartnerWeekly() + getCareHoursFromDaughterWeekly() + getCareHoursFromSonWeekly() + getCareHoursFromOtherWeekly() + getCareHoursFromParentWeekly();
    }

    public double getCareHoursFromPartnerWeekly() {
        double hours = 0.0;
        if (careHoursFromPartnerWeekly != null)
            if (careHoursFromPartnerWeekly >0.0)
                hours = careHoursFromPartnerWeekly;
        return hours;
    }
    public void setCareHoursFromPartnerWeekly(double hours) {
        careHoursFromPartnerWeekly = hours;
    }

    public double getCareHoursFromParentWeekly() {
        double hours = 0.0;
        if (careHoursFromParentWeekly !=null)
            if (careHoursFromParentWeekly >0.0)
                hours = careHoursFromParentWeekly;
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

    public double getCareHoursProvidedWeekly() {
        double hours = 0.0;
        if (careHoursProvidedWeekly != null)
            if (careHoursProvidedWeekly > 0.0)
                hours = careHoursProvidedWeekly;
        return hours;
    }

    public double getHoursInformalSocialCare_L1() {
        return getCareHoursFromPartner_L1() + getCareHoursFromDaughter_L1() + getCareHoursFromSon_L1() + getCareHoursFromOther_L1() + getCareHoursFromParent_L1();
    }

    public double getTotalHoursSocialCare_L1() {
        return getHoursFormalSocialCare_L1() + getHoursInformalSocialCare_L1();
    }

    public double getCareHoursFromParent_L1() {
        return (careHoursFromParentWeekly_lag1 >0.0) ? careHoursFromParentWeekly_lag1 : 0.0;
    }

    public double getCareHoursFromPartner_L1() {
        return (careHoursFromPartnerWeekly_lag1 > 0.0) ? careHoursFromPartnerWeekly_lag1 : 0.0;
    }

    public double getCareHoursFromDaughter_L1() {
        return (careHoursFromDaughterWeekly_lag1 >0.0) ? careHoursFromDaughterWeekly_lag1 : 0.0;
    }

    public double getCareHoursFromSon_L1() {
        return (careHoursFromSonWeekly_lag1 >0.0) ? careHoursFromSonWeekly_lag1 : 0.0;
    }

    public double getCareHoursFromOther_L1() {
        return (careHoursFromOtherWeekly_lag1 >0.0) ? careHoursFromOtherWeekly_lag1 : 0.0;
    }

    public double getSocialCareCostWeekly() {
        double cost = 0.0;
        if (careFormalExpenditureWeekly !=null)
            if (careFormalExpenditureWeekly >0.0)
                cost = careFormalExpenditureWeekly;
        return cost;
    }

    public boolean getTestPartner() {
        return (hasTestPartner!=null) && hasTestPartner;
    }

    public void setHasTestPartner(boolean hasTestPartner) {
        this.hasTestPartner = hasTestPartner;
    }

    public boolean getLeavePartner() {
        return (leavePartner!=null) && leavePartner;
    }

    public void setLeavePartner(boolean leavePartner) {
        this.leavePartner = leavePartner;
    }

    public boolean getLowWageOffer() {
        return (lowWageOffer!=null) && lowWageOffer;
    }

    private boolean checkHighestParentalEducationEquals(Education ee) {
        if (dehf_c3!=null && dehm_c3!=null) {
            if (dehf_c3.getValue() > dehm_c3.getValue())
                return ee.equals(dehf_c3);
            else
                return ee.equals(dehm_c3);
        } else if (dehf_c3!=null) {
            return ee.equals(dehf_c3);
        } else {
            return ee.equals(dehm_c3);
        }
    }

    public boolean getBornInSimulation() {
        return bornInSimulation;
    }

    public void setBornInSimulation(boolean bornInSimulation) {
        this.bornInSimulation = bornInSimulation;
    }

    public double getHoursWorkedWeekly() {
        return ( (hoursWorkedWeekly != null) && hoursWorkedWeekly > 0 ) ? (double) hoursWorkedWeekly : 0.0;
    }

    public double getLeisureHoursPerWeek() {
        return Parameters.HOURS_IN_WEEK - getCareHoursProvidedWeekly() - getHoursWorkedWeekly();
    }

    public void setSampleExit(SampleExit sampleExit) {
        if (!SampleExit.NotYet.equals(this.sampleExit))
            throw new RuntimeException("Attempt to exit person from the simulated sample twice");
        this.sampleExit = sampleExit;
    }
    public SampleExit getSampleExit() {return sampleExit;}
    public double getFertilityRandomUniform2() { return innovations.getDoubleDraw(28); }
    public double getCohabitRandomUniform2() { return innovations.getDoubleDraw(26); }
    public RegressionName getRegressionName(Axis axis) {
        switch (axis) {
            case Student -> {return RegressionName.EducationE1a;}
            case Education -> {return RegressionName.EducationE2a;}
            case Health -> {return RegressionName.HealthH1b;}
            case Disability -> {return RegressionName.HealthH2b;}
            case Cohabitation -> {
                if (Dcpst.Partnered.equals(dcpst_lag1))
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
                if (Gender.Male.equals(dgn))
                    return RegressionName.WagesMalesE;
                else
                    return RegressionName.WagesFemalesE;
            }
            case WageOffer1 -> {
                if (Gender.Male.equals(dgn)) {
                    if (Education.High.equals(deh_c3_lag1)) {
                        return RegressionName.UnemploymentU1a;
                    } else {
                        return RegressionName.UnemploymentU1b;
                    }
                } else {
                    if (Education.High.equals(deh_c3_lag1)) {
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

    public long getSeed() {return (seed!=null) ? seed : 0L;}

    private boolean getDhmGhq_lag1() {
        if (dhmGhq_lag1 == null)
            throw new RuntimeException("attempt to access dhmGhq_lag1 before it has been initialised");
        return dhmGhq_lag1;
    }

    public Double getYnbcpdf_dv() {
        Person partner = getPartner();
        if (partner != null) {
            if (partner.getYpnbihs_dv() != null && getYpnbihs_dv() != null)
                return getYpnbihs_dv() - partner.getYpnbihs_dv();
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

    public boolean getToBePartnered() {return toBePartnered;}

    public static void setPersonIdCounter(long id) {personIdCounter=id;}

    public Double getHe_eq5d() {
        return he_eq5d;
    }

    public void setHe_eq5d(Double he_eq5d) {
        this.he_eq5d = he_eq5d;
    }
}
