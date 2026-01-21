package simpaths.model;

import java.util.*;

import jakarta.persistence.*;

import microsim.data.db.PanelEntityKey;
import org.hibernate.annotations.Fetch;
import simpaths.data.ManagerRegressions;
import simpaths.data.MultiValEvent;
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
import microsim.engine.SimulationEngine;
import microsim.event.EventListener;
import microsim.statistics.IDoubleSource;
import simpaths.model.enums.Les_c4;
import simpaths.model.taxes.Match;

import static java.lang.StrictMath.min;

@Entity
public class BenefitUnit implements EventListener, IDoubleSource, Weight, Comparable<BenefitUnit> {

    @Transient private static Logger log = Logger.getLogger(BenefitUnit.class);
    @Transient private final SimPathsModel model;
    @Transient private final SimPathsCollector collector;
    @Transient public static long benefitUnitIdCounter = 1L;

    // database keys
    @EmbeddedId @Column(unique = true, nullable = false) private final PanelEntityKey key;
    @ManyToOne(fetch = FetchType.EAGER, cascade=CascadeType.REFRESH)
    @JoinColumns({
            @JoinColumn(name="hhid", referencedColumnName = "id"),
            @JoinColumn(name="hhtime", referencedColumnName = "simulation_time"),
            @JoinColumn(name="hhrun", referencedColumnName = "simulation_run"),
            @JoinColumn(name="prid", referencedColumnName = "working_id")
    })
    private Household household;
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "benefitUnit")
    @Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    private Set<Person> members = new LinkedHashSet<>();

    // identifiers
    private Long idBuOriginal;
    private Long idHhOriginal;
    private Long idHh;
    private Long statSeed;

    // unit specific variables
    @Transient private States labStatesContObject;
    private Double yInvestYear;
    private Double yPensYear;
    private Double xDiscretionaryYear;
    @Column(name="wealthTotValue") private Double wealthTotValue;            // total net wealth (includes pensions assets and housing)
    @Column(name="wealthPensValue") private Double wealthPensValue;        // total private (personal and occupational) pensions
    @Column(name="wealthPrptyValue") private Double wealthPrptyValue;        // value of main home (gross of mortgage debt)
    @Column(name="wealthMortgageDebtValue") private Double wealthMortgageDebtValue;          // value of outstanding mortgage debt
    private Double yDispMonth;
    private Double yGrossMonth;
    private Double yBenAmountMonth;
    private Integer yBenUCReceivedFlag;
    private Integer yBenLegacyReceivedFlag;
    private Double yDispEquivYear;
    @Transient private Double yDispEquivYearL1;
    @Transient private Double yDiffDispEquivPrevYear;
    private Integer yPvrtyFlag;        //1 if at risk of poverty, defined by an equivalisedDisposableIncomeYearly < 60% of median household's
    @Transient private Integer yPvrtyFlagL1;
    @Transient private Indicator i_demNChild0to2L1;                //Lag(1) of d_children_3under;
    @Transient private Indicator dem4to12L1;                //Lag(1) of d_children_4_12;
    @Transient private Integer numberChildren02_lag1; //Lag(1) of the number of children aged 0-2 in the household
    @Transient private Integer numberChildrenAll_lag1; //Lag(1) of the number of children of all ages in the household
    private Double xChildCareWeek;
    private Double xCareWeek;
    private Integer careProvidedFlag;
    private Long idtaxDbDonor;
    @Transient private Match demDbMatchTax;
    @Enumerated(EnumType.STRING) private Region region;        //Region of household.  Also used in findDonorHouseholdsByLabour method
    @Enumerated(EnumType.STRING) private Ydses_c5 yHhQuintilesMonthC5;
    @Transient private Ydses_c5 yHhQuintilesC5L1;
    @Transient private Double i_yNonBenHhGrossAsinh;
    @Transient private Dhhtp_c4 demCompHhC4L1;
    private String demCreatedByConstructor;
    @Column(name="wealthPrptyFlag") private Boolean wealthPrptyFlag; // are any of the individuals in the benefit unit a homeowner? True / false
    @Transient ArrayList<Triple<Les_c7_covid, Double, Integer>> covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale = new ArrayList<>();
    @Transient ArrayList<Triple<Les_c7_covid, Double, Integer>> covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale = new ArrayList<>(); // This ArrayList stores monthly values of labour market states and gross incomes, to be sampled from by the LabourMarket class, for the female member of the benefit unit

    @Transient Innovations statInnovations;
    @Transient private Double labPersistValueLabourInnov;
    @Transient private Integer lastYear;

    @Transient private Integer i_demYear;
    @Transient private Occupancy i_demOccupancy;
    @Transient private Education i_eduHighestC3;
    @Transient private Integer i_labHrsWork1Week;
    @Transient private Integer i_labHrsWork2Week;


    /*********************************************************************
     * CONSTRUCTOR FOR OBJECT USED ONLY TO INTERACT WITH REGRESSION MODELS
     ********************************************************************/
    public BenefitUnit() {
        model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        collector = (SimPathsCollector) SimulationEngine.getInstance().getManager(SimPathsCollector.class.getCanonicalName());
        key  = new PanelEntityKey();        //Sets up key
        demCreatedByConstructor = "Empty";
    }

    public BenefitUnit(long id) {
        model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        collector = (SimPathsCollector) SimulationEngine.getInstance().getManager(SimPathsCollector.class.getCanonicalName());
        key  = new PanelEntityKey(id);        //Sets up key
        demCreatedByConstructor = "Empty";
    }

    // USED BY EXPECTATIONS OBJECT TO INTERACT WITH REGRESSION MODELS
    public BenefitUnit(boolean regressionModel) {
        if (regressionModel) {
            model = null;
            key = null;
            collector = null;
        } else {
            throw new RuntimeException("Unrecognised call to benefit unit constructor");
        }
    }

    // USED BY EXPECTATIONS OBJECT TO INTERACT WITH REGRESSION MODELS
    public BenefitUnit(BenefitUnit originalBenefitUnit, boolean regressionModel) {

        this(regressionModel);
        if (regressionModel) {
            i_demYear = originalBenefitUnit.i_demYear;
            i_demOccupancy = originalBenefitUnit.i_demOccupancy;
            i_eduHighestC3 = originalBenefitUnit.i_eduHighestC3;
            region = originalBenefitUnit.region;
        } else {
            throw new RuntimeException("error accessing copy constructor of benefitUnit for use with regression models");
        }
    }

    // USED BY OTHER CONSTRUCTORS
    public BenefitUnit(Long id, long statSeed) {
        super();
        model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
        collector = (SimPathsCollector) SimulationEngine.getInstance().getManager(SimPathsCollector.class.getCanonicalName());
        key  = new PanelEntityKey(id);        //Sets up key

        this.statSeed = statSeed;
        statInnovations = new Innovations(9, 1, statSeed);

        this.numberChildrenAll_lag1 = 0;
        this.numberChildren02_lag1 = 0;
        this.i_demNChild0to2L1 = Indicator.False;
        this.dem4to12L1 = Indicator.False;
        this.xChildCareWeek = 0.0;
        this.xCareWeek = 0.0;
        this.careProvidedFlag = 0;
        this.yDispMonth = 0.;
        this.yGrossMonth = 0.;
        this.yDispEquivYear = 0.;
        this.yBenAmountMonth = 0.;
        this.demCreatedByConstructor = "LongID";
        if (Parameters.projectLiquidWealth)
            setWealthTotValue(0.);
    }

    // USED TO CONSTRUCT NEW BENEFIT UNITS FOR PEOPLE AS NEEDED
    // SEE THE PERSON OBJECT, setupNewBenefitUnit METHOD
    public BenefitUnit(Person person, long statSeed) {

        // initialise benefit unit
        this(benefitUnitIdCounter++, statSeed);
        region = person.getRegion();
        if (Parameters.projectLiquidWealth) {
            // transfer wealth between benefit units

            BenefitUnit fromBenefitUnit = person.getBenefitUnit();
            setWealthTotValue(person.getLiquidWealth());
            if (this != fromBenefitUnit) {
                fromBenefitUnit.setWealthTotValue(fromBenefitUnit.getWealthTotValue() - person.getLiquidWealth());
            }
        }

        // finalise
        this.demCreatedByConstructor = "Singles";
    }

    public BenefitUnit(Person p1, Person p2) {

        // initialise benefit unit
        this(benefitUnitIdCounter++, (long)(p1.getBenefitUnitRandomUniform()*100000));
        region = p1.getRegion();
        if (region != p2.getRegion())
            throw new RuntimeException("ERROR - region of responsible male and female must match!");
        if (Parameters.projectLiquidWealth) {
            // transfer wealth between benefit units

            setWealthTotValue(p1.getLiquidWealth() + p2.getLiquidWealth());
            BenefitUnit pBU = p1.getBenefitUnit();
            if (this!=pBU) {
                pBU.setWealthTotValue(pBU.getWealthTotValue() - p1.getLiquidWealth());
            }
            pBU = p2.getBenefitUnit();
            if (this!=pBU) {
                pBU.setWealthTotValue(pBU.getWealthTotValue() - p2.getLiquidWealth());
            }
        }

        // finalise
        demCreatedByConstructor = "Couples";
    }

    // Below is a "copy constructor" for benefitUnits: it takes an original benefit unit as input, changes the ID, copies
    // the rest of the benefit unit's properties, and creates a new benefit unit.
    public BenefitUnit(BenefitUnit originalBenefitUnit, long benefitUnitInnov, SampleEntry sampleEntry) {

        this(benefitUnitIdCounter++, benefitUnitInnov);
        switch (sampleEntry) {
            case ProcessedInputData -> {
                key.setId(originalBenefitUnit.getId());
                idBuOriginal = originalBenefitUnit.getIdOriginalBU();
                idHhOriginal = originalBenefitUnit.getIdOriginalHH();
            }
            default -> {
                idBuOriginal = originalBenefitUnit.getId();
                idHhOriginal = originalBenefitUnit.household.getId();
            }
        }


        this.log = originalBenefitUnit.log;
        yDispMonth = Objects.requireNonNullElse(originalBenefitUnit.getDisposableIncomeMonthly(),0.0);
        xDiscretionaryYear = Objects.requireNonNullElse(originalBenefitUnit.xDiscretionaryYear, 0.0);
        yGrossMonth = Objects.requireNonNullElse(originalBenefitUnit.getGrossIncomeMonthly(),0.0);
        yDispEquivYear = Objects.requireNonNullElse(originalBenefitUnit.yDispEquivYear,0.0);
        yDispEquivYearL1 = Objects.requireNonNullElse(originalBenefitUnit.yDispEquivYearL1, yDispEquivYear);
        yBenAmountMonth = Objects.requireNonNullElse(originalBenefitUnit.getBenefitsReceivedPerMonth(),0.0);
        yPvrtyFlag = Objects.requireNonNullElse(originalBenefitUnit.yPvrtyFlag,0);
        yPvrtyFlagL1 = Objects.requireNonNullElse(originalBenefitUnit.yPvrtyFlagL1, yPvrtyFlag);
        yDiffDispEquivPrevYear = Objects.requireNonNullElse(originalBenefitUnit.yDiffDispEquivPrevYear,0.0);
        if (Parameters.projectLiquidWealth)
            initialiseLiquidWealth(
                    originalBenefitUnit.getRefPersonForDecisions().getDemAge(),
                    originalBenefitUnit.getWealthTotValue(),
                    originalBenefitUnit.getPensionWealth(false),
                    originalBenefitUnit.getHousingWealth(false)
            );
        this.numberChildrenAll_lag1 = originalBenefitUnit.numberChildrenAll_lag1;
        this.numberChildren02_lag1 = originalBenefitUnit.numberChildren02_lag1;
        this.i_demNChild0to2L1 = originalBenefitUnit.i_demNChild0to2L1;
        this.dem4to12L1 = originalBenefitUnit.dem4to12L1;
        this.xChildCareWeek = originalBenefitUnit.xChildCareWeek;
        this.xCareWeek = originalBenefitUnit.xCareWeek;
        this.careProvidedFlag = originalBenefitUnit.careProvidedFlag;
        this.region = originalBenefitUnit.region;
        this.yHhQuintilesMonthC5 = originalBenefitUnit.getYdses_c5();
        this.yHhQuintilesC5L1 = originalBenefitUnit.yHhQuintilesC5L1;
        this.demCompHhC4L1 = originalBenefitUnit.demCompHhC4L1;
        this.wealthPrptyFlag = originalBenefitUnit.wealthPrptyFlag;
        demCreatedByConstructor = Objects.requireNonNullElse(originalBenefitUnit.demCreatedByConstructor,"CopyConstructor");
        i_yNonBenHhGrossAsinh = Objects.requireNonNullElse(originalBenefitUnit.i_yNonBenHhGrossAsinh, 0.0);
        demDbMatchTax = originalBenefitUnit.getTaxDbMatch();
    }


    // ---------------------------------------------------------------------
    // Event Listener
    // ---------------------------------------------------------------------


    public enum Processes {
        Update,        //This updates the household fields, such as number of children of a certain age
        UpdateWealth,
        CalculateChangeInEDI, //Calculate change in equivalised disposable income
        Homeownership,
        ReceivesBenefits,
        UpdateStates,
        UpdateInvestmentIncome,
        ProjectDiscretionaryConsumption,
        UpdateMembers,
    }

    @Override
    public void onEvent(Enum<?> type) {
        switch ((Processes) type) {
            case Update -> {
                updateAttributes();
                clearStates();
            }
            case UpdateWealth -> {
                updateWealth();
            }
            case CalculateChangeInEDI -> {
                calculateEquivalisedDisposableIncomeYearly(); //Update BU's EDI
                calculateYearlyChangeInLogEquivalisedDisposableIncome(); //Calculate change in EDI
            }
            case Homeownership -> {
                homeownership();
            }
            case ReceivesBenefits -> {
                setReceivesBenefitsFlag();
            }
            case UpdateStates -> {
                setStates();
            }
            case ProjectDiscretionaryConsumption -> {
                updateDiscretionaryConsumption();
            }
            default -> {
                throw new RuntimeException("unrecognised BenefitUnit process: " + type);
            }
        }
    }

    protected void initializeFields() {

        if (getNumberChildrenAll()==0)
            xChildCareWeek = 0.0;
        demCompHhC4L1 = getDhhtp_c4();

        // clean-up odd ends
        if (getYdses_c5() == null) {
            yHhQuintilesMonthC5 = Ydses_c5.Q3;
        }
        if (region == null)
            throw new RuntimeException("problem identifying region of new benefit unit");
    }

    protected void updateAttributes() {

        // unit specific variables
        if (getNumberChildrenAll()==0)
            xChildCareWeek = 0.0;

        // lags
        i_demNChild0to2L1 = getIndicatorChildren(0,3);
        dem4to12L1 = getIndicatorChildren(4,12);
        numberChildrenAll_lag1 = getNumberChildrenAll();
        numberChildren02_lag1 = getNumberChildren(0,2);
        demCompHhC4L1 = getDhhtp_c4();

        yDispEquivYearL1 = getEquivalisedDisposableIncomeYearly();
        yPvrtyFlagL1 = getAtRiskOfPoverty();
        yHhQuintilesC5L1 = getYdses_c5();

        // random draws
        statInnovations.getNewDoubleDraws();
    }

    protected void updateWealth() {
        wealthTotValue += yDispMonth * 12.0 - xDiscretionaryYear - getNonDiscretionaryConsumptionPerYear();
    }


    // ---------------------------------------------------------------------
    // Labour Market Interaction
    // ---------------------------------------------------------------------
    protected void resetLabourStates() {

        Person male = getMale();
        if(male != null) {
            male.setLabourSupplyWeekly(null);
        }
        Person female = getFemale();
        if(female != null) {
            female.setLabourSupplyWeekly(null);
        }
    }

    /**
     * This method returns all possible combinations of male's and female's hours of work
     * @return
     */
    public LinkedHashSet<MultiKey<Labour>> findPossibleLabourCombinations() {
        LinkedHashSet<MultiKey<Labour>> combinationsToReturn = new LinkedHashSet<>();
        Person male = getMale();
        Person female = getFemale();
        if (male!=null && female!=null) {
            //Need to use both partners individual characteristics to determine similar benefitUnits
            //Sometimes one of the occupants of the couple will be retired (or even under the age to work,
            // which is currently the age to leave home).  For this case, the person (not at risk of work)'s
            // labour supply will always be zero, while the other person at risk of work has a choice over the
            // single person Labour Supply set.
            Labour[] labourMaleValues;
            if (male.atRiskOfWork()) {
                labourMaleValues = Labour.values();
            } else {
                labourMaleValues = new Labour[]{Labour.ZERO};
            }

            Labour[] labourFemaleValues;
            if (female.atRiskOfWork()) {
                labourFemaleValues = Labour.values();
            } else {
                labourFemaleValues = new Labour[]{Labour.ZERO};
            }

            for (Labour labourMale: labourMaleValues) {
                for(Labour labourFemale: labourFemaleValues) {
                    combinationsToReturn.add(new MultiKey<>(labourMale, labourFemale));
                }
            }
        } else {
            //For single benefitUnits, no need to check for at risk of work (i.e. retired, sick or student activity status),
            // as this has already been done when passing this household to the labour supply module (see first loop over benefitUnits
            // in LabourMarket#update()).
            if (male!=null) {
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
        Occupancy occupancy = getOccupancy();
        switch (occupancy) {
            case Couple -> {
                getMale().setReceivesBenefitsFlag(receivesBenefitsFlag);
                getFemale().setReceivesBenefitsFlag(receivesBenefitsFlag);
            }
            case Single_Male -> {
                getMale().setReceivesBenefitsFlag(receivesBenefitsFlag);
            }
            case Single_Female -> {
                getFemale().setReceivesBenefitsFlag(receivesBenefitsFlag);
            }
            default ->
                throw new IllegalStateException("Benefit Unit with the following ID has no recognised occupancy: " + getKey().getId());
        }
    }

    public void setReceivesBenefitsFlagUCNonUC() {

        // TO DO: this will require an additional test of whether UC is received or not - for now all receive UC if any ben
        boolean receivesBenefitsFlagUC = getReceivedUC() == 1 && Parameters.UC_ROLLOUT;
        boolean receivesLegacyBenefitsFlag = !receivesBenefitsFlagUC && getReceivedLegacyBenefits() == 1;
        Occupancy occupancy = getOccupancy();
        switch (occupancy) {
            case Couple -> {
                getMale().setReceivesBenefitsFlagUC(receivesBenefitsFlagUC);
                getMale().setReceivesBenefitsFlagNonUC(receivesLegacyBenefitsFlag);
                getFemale().setReceivesBenefitsFlagUC(receivesBenefitsFlagUC);
                getFemale().setReceivesBenefitsFlagNonUC(receivesLegacyBenefitsFlag);
            }
            case Single_Male -> {
                getMale().setReceivesBenefitsFlagUC(receivesBenefitsFlagUC);
                getMale().setReceivesBenefitsFlagNonUC(receivesLegacyBenefitsFlag);
            }
            case Single_Female -> {
                getFemale().setReceivesBenefitsFlagUC(receivesBenefitsFlagUC);
                getFemale().setReceivesBenefitsFlagNonUC(receivesLegacyBenefitsFlag);
            }
            default ->
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
            Person male = getMale();
            Person female = getFemale();
            if (male!=null && female!=null) {

                male.setLabourSupplyWeekly(Labour.convertHoursToLabour(hoursWorkedPerWeekM));
                getFemale().setLabourSupplyWeekly(Labour.convertHoursToLabour(hoursWorkedPerWeekF));
                double maleIncome = Math.sinh(male.getyMiscPersGrossMonth());
                double femaleIncome = Math.sinh(getFemale().getyMiscPersGrossMonth());
                if (maleIncome>0.01 && femaleIncome>0.01)
                    secondIncomePerMonth = Math.min(maleIncome, femaleIncome);
                originalIncomePerMonth = maleIncome + femaleIncome;
                dlltsdM = male.getDisability();
                dlltsdF = getFemale().getDisability();
            } else if (male!=null) {

                male.setLabourSupplyWeekly(Labour.convertHoursToLabour(hoursWorkedPerWeekM));
                originalIncomePerMonth = Math.sinh(male.getyMiscPersGrossMonth());
                dlltsdM = male.getDisability();
            } else if (female!=null){

                getFemale().setLabourSupplyWeekly(Labour.convertHoursToLabour(hoursWorkedPerWeekF));
                originalIncomePerMonth = Math.sinh(getFemale().getyMiscPersGrossMonth());
                dlltsdF = getFemale().getDisability();
            } else
                throw new RuntimeException("Benefit Unit with the following ID has no recognised occupancy: " + getKey().getId());

            // update disposable income
            TaxEvaluation evaluatedTransfers = taxWrapper(hoursWorkedPerWeekM, hoursWorkedPerWeekF, dlltsdM, dlltsdF, originalIncomePerMonth, secondIncomePerMonth);

            yDispMonth = evaluatedTransfers.getDisposableIncomePerMonth();
            yBenAmountMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
            yGrossMonth = evaluatedTransfers.getGrossIncomePerMonth();
            calculateBUIncome();
            demDbMatchTax = evaluatedTransfers.getMatch();
            idtaxDbDonor = demDbMatchTax.getCandidateID();
        } else {
            throw new RuntimeException("call to evaluate disposable income on assumption of zero risk of employment where there is risk");
        }
    }

    private TaxEvaluation taxWrapper(double hoursWorkedPerWeekM, double hoursWorkedPerWeekF, int dlltsdM, int dlltsdF, double originalIncomePerMonth, double secondIncomePerMonth) {

        xChildCareWeek = 0.0;
        double childcareCostPerMonth = 0.0;
        if (Parameters.flagFormalChildcare && !Parameters.flagSuppressChildcareCosts) {
            updateChildcareCostPerWeek(model.getYear(), getRefPersonForDecisions().getDemAge());
            childcareCostPerMonth = xChildCareWeek * Parameters.WEEKS_PER_MONTH;
        }

        xCareWeek = 0.0;
        careProvidedFlag = 0;
        double socialCareCostPerMonth = 0.0;
        if (Parameters.flagSocialCare && !Parameters.flagSuppressSocialCareCosts) {
            updateSocialCareProvision();
            updateSocialCareCostPerWeek();
            socialCareCostPerMonth = xCareWeek * Parameters.WEEKS_PER_MONTH;
        }

        if (Parameters.flagSuppressSocialCareCosts) {
            dlltsdF = 0;
            dlltsdM = 0;
        }

        // update disposable income
        TaxEvaluation evaluatedTransfers;
        double taxInnov = (Parameters.donorPoolAveraging) ? -1.0 : statInnovations.getDoubleDraw(8);
        evaluatedTransfers = new TaxEvaluation(model.getYear(), getRefPersonForDecisions().getDemAge(), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), hoursWorkedPerWeekM, hoursWorkedPerWeekF, dlltsdM, dlltsdF, careProvidedFlag, originalIncomePerMonth, secondIncomePerMonth, childcareCostPerMonth, socialCareCostPerMonth, getLiquidWealth(Parameters.enableIntertemporalOptimisations), taxInnov);

        return evaluatedTransfers;
    }

    protected void updateMonthlyLabourSupplyCovid19() {

        // This method calls on predictCovidTransition() method to set labour market state, gross income, and work hours for each month.
        // After 12 months, this should result in an array with 12 values, from which the LabourMarket class can sample one that is representative of the labour market state for the whole year.
        Person male = getMale();
        Person female = getFemale();
        if (male!=null && female!=null) {
            if (male.atRiskOfWork()) {
                Triple<Les_c7_covid, Double, Integer> stateGrossIncomeWorkHoursTriple = predictCovidTransition(male); // predictCovidTransition() applies transition models to predict transition and returns new labour market state, gross income, and work hours
                covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.add(stateGrossIncomeWorkHoursTriple); // Add the state-gross income-work hours triple to the ArrayList keeping track of all monthly values
            }
            if (female.atRiskOfWork()) {
                Triple<Les_c7_covid, Double, Integer> stateGrossIncomeWorkHoursTriple = predictCovidTransition(getFemale());
                covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.add(stateGrossIncomeWorkHoursTriple);
            }
        } else if (male!=null || female!=null) {
            Person person = Objects.requireNonNullElse(male,female);
            if (person.atRiskOfWork()) {
                Triple<Les_c7_covid, Double, Integer> stateGrossIncomeWorkHoursTriple = predictCovidTransition(person);
                covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.add(stateGrossIncomeWorkHoursTriple);
            }
        } else {System.out.println("Warning: Incorrect occupancy for benefit unit " + getKey().getId());}
    }

    /*
    chooseRandomMonthlyTransitionAndGrossIncome() method selected a random value out of all the monthly transitions (which contains state to which individual transitions, gross income, and work hours), finds donor benefit unit and calculates disposable income

     */
    private int intFromUniform(int min, int max, double uniform) {
        return (int) Math.round(uniform * (max - min) + min);
    }
    protected void chooseRandomMonthlyOutcomeCovid19() {

        double labourInnov = statInnovations.getDoubleDraw(2);
        double taxRandomUniform = statInnovations.getDoubleDraw(8);
        Person male = getMale();
        Person female = getFemale();
        if (male!=null && female!=null) {
            if(male.atRiskOfWork()) {
                if(female.atRiskOfWork()) {
                    // both male and female have flexible labour supply
                    if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size() > 0 && covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size() > 0) {

                        // Get random int which indicates which monthly value to use. Use smaller value in case male and female lists were of different length.
                        int randomIndex = intFromUniform(0, min(covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size(), covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size()), labourInnov);
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

                        double taxInnov = (Parameters.donorPoolAveraging) ? -1.0 : taxRandomUniform;
                        evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), female.getDisability(), 0, simulatedIncomeToConvertPerMonth, 0.0, 0.0, taxInnov);

                        yDispMonth = evaluatedTransfers.getDisposableIncomePerMonth();
                        yBenAmountMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
                        yGrossMonth = evaluatedTransfers.getGrossIncomePerMonth();
                    } else {
                        throw new RuntimeException("Inconsistent atRiskOfWork indicator and covid19MonthlyStateAndGrossIncomeAndWorkHoursTriples");
                    }
                } else {
                    // male has flexible labour supply, female doesn't
                    if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size() > 0) {
                        int randomIndex = intFromUniform(0, covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size(), labourInnov);
                        Triple<Les_c7_covid, Double, Integer> selectedValueMale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.get(randomIndex);

                        male.setLes_c7_covid(selectedValueMale.getLeft()); // Set labour force status for male
                        male.setLes_c4(Les_c4.convertLes_c7_To_Les_c4(selectedValueMale.getLeft()));

                        // Predicted hours need to be converted back into labour so a donor benefit unit can be found. Then, gross income can be converted to disposable.
                        male.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueMale.getRight())); // Convert predicted work hours to labour enum and update male's value
                        double simulatedIncomeToConvertPerMonth = selectedValueMale.getMiddle(); // Benefit unit's gross income to convert is the sum of incomes (labour and capital included) of male and female

                        // Find best donor and convert gross income to disposable
                        MultiKey<? extends Labour> labourKey = new MultiKey<>(male.getLabourSupplyWeekly(), Labour.ZERO);
                        TaxEvaluation evaluatedTransfers;
                        double taxInnov = (Parameters.donorPoolAveraging) ? -1.0 : taxRandomUniform;
                        evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), female.getDisability(), 0, simulatedIncomeToConvertPerMonth, 0.0, 0.0, taxInnov);
                        yDispMonth = evaluatedTransfers.getDisposableIncomePerMonth();
                        yBenAmountMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
                        yGrossMonth = evaluatedTransfers.getGrossIncomePerMonth();
                    } else {
                        throw new RuntimeException("Inconsistent atRiskOfWork indicator and covid19MonthlyStateAndGrossIncomeAndWorkHoursTriples (2)");
                    }
                }
            } else if (female.atRiskOfWork()) {
                // female has flexible labour supply, male doesn't
                if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size() > 0) {
                    int randomIndex = intFromUniform(0, covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size(), labourInnov);
                    Triple<Les_c7_covid, Double, Integer> selectedValueFemale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.get(randomIndex);

                    female.setLes_c7_covid(selectedValueFemale.getLeft()); // Set labour force status for female
                    female.setLes_c4(Les_c4.convertLes_c7_To_Les_c4(selectedValueFemale.getLeft()));

                    // Predicted hours need to be converted back into labour so a donor benefit unit can be found. Then, gross income can be converted to disposable.
                    female.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueFemale.getRight())); // Convert predicted work hours to labour enum and update female's value
                    double simulatedIncomeToConvertPerMonth = selectedValueFemale.getMiddle(); // Benefit unit's gross income to convert is the sum of incomes (labour and capital included) of male and female, but in this case male is not at risk of work

                    // Find best donor and convert gross income to disposable
                    MultiKey<? extends Labour> labourKey = new MultiKey<>(Labour.ZERO, female.getLabourSupplyWeekly());

                    TaxEvaluation evaluatedTransfers;
                    double taxInnov = (Parameters.donorPoolAveraging) ? -1.0 : taxRandomUniform;
                    evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), female.getDisability(), 0, simulatedIncomeToConvertPerMonth, 0.0, 0.0, taxInnov);
                    yDispMonth = evaluatedTransfers.getDisposableIncomePerMonth();
                    yBenAmountMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
                    yGrossMonth = evaluatedTransfers.getGrossIncomePerMonth();
                } else {
                    throw new RuntimeException("Inconsistent atRiskOfWork indicator and covid19MonthlyStateAndGrossIncomeAndWorkHoursTriples (3)");
                }
            } else {
                throw new IllegalArgumentException("In consistent indicators for at risk of work in couple benefit unit " + getKey().getId());
            }
        } else if (male!=null) {
            if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size() > 0) {
                int randomIndex = intFromUniform(0, covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size(), labourInnov);
                Triple<Les_c7_covid, Double, Integer> selectedValueMale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.get(randomIndex);
                male.setLes_c7_covid(selectedValueMale.getLeft()); // Set labour force status for male
                male.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueMale.getRight()));
                double simulatedIncomeToConvertPerMonth = selectedValueMale.getMiddle();

                // Find best donor and convert gross income to disposable
                MultiKey<? extends Labour> labourKey = new MultiKey<>(male.getLabourSupplyWeekly(), Labour.ZERO);

                TaxEvaluation evaluatedTransfers;
                double taxInnov = (Parameters.donorPoolAveraging) ? -1.0 : taxRandomUniform;
                evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), -1, 0, simulatedIncomeToConvertPerMonth, 0.0, 0.0, taxInnov);
                yDispMonth = evaluatedTransfers.getDisposableIncomePerMonth();
                yBenAmountMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
                yGrossMonth = evaluatedTransfers.getGrossIncomePerMonth();
            } else {
                throw new IllegalArgumentException("In consistent indicators for at risk of work in single male benefit unit " + getKey().getId());
            }
        } else {
            if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size() > 0) {
                int randomIndex = intFromUniform(0, covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size(), labourInnov);
                Triple<Les_c7_covid, Double, Integer> selectedValueFemale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.get(randomIndex);
                female.setLes_c7_covid(selectedValueFemale.getLeft()); // Set labour force status for female
                female.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueFemale.getRight()));
                double simulatedIncomeToConvertPerMonth = selectedValueFemale.getMiddle();

                // Find best donor and convert gross income to disposable
                MultiKey<? extends Labour> labourKey = new MultiKey<>(Labour.ZERO, female.getLabourSupplyWeekly());

                TaxEvaluation evaluatedTransfers;
                double taxInnov = (Parameters.donorPoolAveraging) ? -1.0 : taxRandomUniform;
                evaluatedTransfers = new TaxEvaluation(model.getYear(), getIntValue(Regressors.MaximumAge), getIntValue(Regressors.NumberMembersOver17), getIntValue(Regressors.NumberChildren04), getIntValue(Regressors.NumberChildren59), getIntValue(Regressors.NumberChildren1017), labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), -1, female.getDisability(), 0, simulatedIncomeToConvertPerMonth, 0.0, 0.0, taxInnov);
                yDispMonth = evaluatedTransfers.getDisposableIncomePerMonth();
                yBenAmountMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
                yGrossMonth = evaluatedTransfers.getGrossIncomePerMonth();
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
            person.setCovidYLabGrossL1(person.getCovidModuleGrossLabourIncome_Baseline());
        }


        // Define variables:
        Les_c7_covid stateFrom = person.getLes_c7_covid_lag1();
        Les_c7_covid stateTo = stateFrom; // Labour market state to which individual transitions. Initialise to stateFrom value if the outcome is "no changes"
        int newWorkHours; // Predicted work hours. Initialise to previous value if available, or hours from labour enum.
        if (person.getLabHrsWorkNewL1() != null) {
            newWorkHours = person.getLabHrsWorkNewL1();
        } else {
            newWorkHours = person.getLabourSupplyHoursWeekly(); // Note: prediction for hours is in logs, needs to be transformed to levels
            person.setNewWorkHours_lag1(newWorkHours);
        }
        double grossMonthlyIncomeToReturn = 0; // Gross income to return to updateLabourSupplyCovid19() method

        // Transitions from employment
        double labourInnov2 = statInnovations.getDoubleDraw(3), labourInnov3 = statInnovations.getDoubleDraw(4);
        if (Les_c7_covid.Employee.equals(stateFrom)) {
            Map<Les_transitions_E1,Double> probs = Parameters.getRegC19LS_E1().getProbabilities(person, Person.DoublesVariables.class);
            MultiValEvent event = new MultiValEvent(probs, labourInnov2);
            Les_transitions_E1 transitionTo = (Les_transitions_E1) event.eval();
            stateTo = transitionTo.convertToLes_c7_covid();
            person.setLes_c7_covid(stateTo); // Use convert to les c6 covid method from the enum to convert the outcome to the les c6 scale and update the variable

            if (Les_transitions_E1.SelfEmployed.equals(transitionTo) || Les_transitions_E1.SomeChanges.equals(transitionTo)) {

                newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_E2a().getScore(person, Person.DoublesVariables.class)))).getHours(person);
                grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getyMiscPersGrossMonth());
            } else if (transitionTo.equals(Les_transitions_E1.NotEmployed)) {
                newWorkHours = 0;
                grossMonthlyIncomeToReturn = 0;
            } else if (transitionTo.equals(Les_transitions_E1.FurloughedFull)) {
                // If furloughed, don't change hours of work initialised at the beginning
                grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getyMiscPersGrossMonth());
            } else if (transitionTo.equals(Les_transitions_E1.FurloughedFlex)) {
                newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_E2b().getScore(person, Person.DoublesVariables.class)))).getHours(person);
                grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getyMiscPersGrossMonth());
            } else { // Else "no changes" = employee. Use initialisation value for stateTo and newWorkHours and fill gross monthly income
                grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getyMiscPersGrossMonth());
            }

            // Transitions from furlough full
        } else if (stateFrom.equals(Les_c7_covid.FurloughedFull)) {
            Map<Les_transitions_FF1,Double> probs = Parameters.getRegC19LS_FF1().getProbabilities(person, Person.DoublesVariables.class);
            MultiValEvent event = new MultiValEvent(probs, labourInnov2);
            Les_transitions_FF1 transitionTo = (Les_transitions_FF1) event.eval();

            stateTo = transitionTo.convertToLes_c7_covid();
            person.setLes_c7_covid(stateTo); // Use convert to les c7 covid method from the enum to convert the outcome to the les c7 scale and update the variable

            if (transitionTo.equals(Les_transitions_FF1.Employee)) {
                newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2b().getScore(person, Person.DoublesVariables.class)))).getHours(person);
                grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getyMiscPersGrossMonth());
            } else if (transitionTo.equals(Les_transitions_FF1.SelfEmployed)) {
                newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2a().getScore(person, Person.DoublesVariables.class)))).getHours(person);
                grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getyMiscPersGrossMonth());
            } else if (transitionTo.equals(Les_transitions_FF1.FurloughedFlex)) {
                newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2c().getScore(person, Person.DoublesVariables.class)))).getHours(person);
                grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getyMiscPersGrossMonth());
            } else if (transitionTo.equals(Les_transitions_FF1.NotEmployed)) {
                newWorkHours = 0;
                grossMonthlyIncomeToReturn = 0;
            } else { // Else remains furloughed. Use 80% of initialisation value for stateTo and newWorkHours and fill gross monthly income
                grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getyMiscPersGrossMonth());
            }

            // Transitions from furlough flex
        } else if (stateFrom.equals(Les_c7_covid.FurloughedFlex)) {
            Map<Les_transitions_FX1,Double> probs = Parameters.getRegC19LS_FX1().getProbabilities(person, Person.DoublesVariables.class);
            MultiValEvent event = new MultiValEvent(probs, labourInnov2);
            Les_transitions_FX1 transitionTo = (Les_transitions_FX1) event.eval();
            stateTo = transitionTo.convertToLes_c7_covid();
            person.setLes_c7_covid(stateTo); // Use convert to les c7 covid method from the enum to convert the outcome to the les c7 scale and update the variable

            if (transitionTo.equals(Les_transitions_FX1.Employee)) {
                newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2b().getScore(person, Person.DoublesVariables.class)))).getHours(person);
                grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getyMiscPersGrossMonth());
            } else if (transitionTo.equals(Les_transitions_FX1.SelfEmployed)) {
                newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2a().getScore(person, Person.DoublesVariables.class)))).getHours(person);
                grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getyMiscPersGrossMonth());
            } else if (transitionTo.equals(Les_transitions_FX1.FurloughedFull)) {
                newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2a().getScore(person, Person.DoublesVariables.class)))).getHours(person);
                grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getyMiscPersGrossMonth()); // 80% of earnings they would have had working normal hours, hence hours predicted as for employed in the line above
            } else if (transitionTo.equals(Les_transitions_FX1.NotEmployed)) {
                newWorkHours = 0;
                grossMonthlyIncomeToReturn = 0;
            } else { // Else remains furloughed. Use 80% of initialisation value for stateTo and newWorkHours and fill gross monthly income
                grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getyMiscPersGrossMonth());
            }

            // Transitions from self-employment
        } else if (stateFrom.equals(Les_c7_covid.SelfEmployed)) {
            Map<Les_transitions_S1,Double> probs = Parameters.getRegC19LS_S1().getProbabilities(person, Person.DoublesVariables.class);
            MultiValEvent event = new MultiValEvent(probs, labourInnov2);
            Les_transitions_S1 transitionTo = (Les_transitions_S1) event.eval();
            stateTo = transitionTo.convertToLes_c7_covid();
            person.setLes_c7_covid(stateTo); // Use convert to les c6 covid method from the enum to convert the outcome to the les c6 scale and update the variable

            if (transitionTo.equals(Les_transitions_S1.Employee) || transitionTo.equals(Les_transitions_S1.SelfEmployed)) {
                newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_S2a().getScore(person, Person.DoublesVariables.class)))).getHours(person);
                grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getyMiscPersGrossMonth());

                // If transition to is self-employed (i.e. continues in self-employment), and earnings have decreased (gross monthly income lower than lag1 of gross monthly income, obtained from person.getCovidModuleGrossLabourIncome_lag1), predict probabiltiy of SEISS
                if (transitionTo.equals(Les_transitions_S1.SelfEmployed) && grossMonthlyIncomeToReturn < person.getCovidYLabGrossL1()) {

                    double prob = Parameters.getRegC19LS_S3().getProbability(person, Person.DoublesVariables.class);
                    if (labourInnov3 < prob) {
                        // receives SEISS

                        person.setCovidSEISSReceivedFlag(Indicator.True);
                        grossMonthlyIncomeToReturn = 0.8 * person.getCovidYLabGrossL1();
                    }
                }
            } else if (transitionTo.equals(Les_transitions_S1.NotEmployed)) {
                newWorkHours = 0;
                grossMonthlyIncomeToReturn = 0;
            } // No else here as new work hours and gross income are predicted if remains self-employed (above)

            // Transitions from non-employment
        } else if (stateFrom.equals(Les_c7_covid.NotEmployed)) {
            Map<Les_transitions_U1,Double> probs = Parameters.getRegC19LS_U1().getProbabilities(person, Person.DoublesVariables.class);
            MultiValEvent event = new MultiValEvent(probs, labourInnov2);
            Les_transitions_U1 transitionTo = (Les_transitions_U1) event.eval();
            stateTo = transitionTo.convertToLes_c7_covid();
            person.setLes_c7_covid(stateTo); // Use convert to les c6 covid method from the enum to convert the outcome to the les c6 scale and update the variable

            if (transitionTo.equals(Les_transitions_U1.Employee) || transitionTo.equals(Les_transitions_U1.SelfEmployed)) {
                newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_U2a().getScore(person, Person.DoublesVariables.class)))).getHours(person);
                grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH * person.getEarningsWeekly(newWorkHours) + Math.sinh(person.getyMiscPersGrossMonth());
            } else if (transitionTo.equals(Les_transitions_U1.NotEmployed)) {
                newWorkHours = 0;
                grossMonthlyIncomeToReturn = 0;
            }

        } else {
            //	System.out.println("Warning: Person " + person.getKey().getId() + " entered Covid-19 transitions process, but doesn't have correct starting labour market state which was " + stateFrom);
        }

        Triple<Les_c7_covid, Double, Integer> stateGrossIncomeWorkHoursTriple = Triple.of(stateTo, grossMonthlyIncomeToReturn, newWorkHours); // Triple contains outcome labour market state after transition, gross income, and work hours
        person.setCovidYLabGrossL1(grossMonthlyIncomeToReturn); // used as a regressor in the Covid-19 regressions
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

        resetLabourStates();
        Occupancy occupancy = getOccupancy();
        Person male = getMale();
        Person female = getFemale();
        if (Parameters.enableIntertemporalOptimisations && (DecisionParams.FLAG_IO_EMPLOYMENT1 || DecisionParams.FLAG_IO_EMPLOYMENT2) ) {
            // intertemporal optimisations enabled

            // labour and labour income
            double emp1, emp2, labourIncomeWeeklyM, labourIncomeWeeklyF, hoursWorkedPerWeekM, hoursWorkedPerWeekF;
            labourIncomeWeeklyM = labourIncomeWeeklyF = hoursWorkedPerWeekM = hoursWorkedPerWeekF = 0.0;
            int dlltsdM = -1, dlltsdF = -1;
            if (DecisionParams.FLAG_IO_EMPLOYMENT1 && labStatesContObject.getAgeYears() <= Parameters.MAX_AGE_FLEXIBLE_LABOUR_SUPPLY) {
                emp1 = Parameters.grids.employment1.interpolateAll(labStatesContObject, false);
            } else {
                emp1 = 0.0;
            }
            if (male!=null && female!=null) {
                // couples

                if (DecisionParams.FLAG_IO_EMPLOYMENT2 && labStatesContObject.getAgeYears() <= Parameters.MAX_AGE_FLEXIBLE_LABOUR_SUPPLY) {
                    emp2 = Parameters.grids.employment2.interpolateAll(this.labStatesContObject, false);
                } else {
                    emp2 = 0.0;
                }
                if (getRefPersonForDecisions().getGender()==0) {
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

            // evaluate original income
            double originalIncomePerMonth = Parameters.WEEKS_PER_MONTH * (labourIncomeWeeklyM + labourIncomeWeeklyF) +
                    yInvestYear /12.0 + yPensYear /12.0;
            double secondIncomePerMonth = Math.min(labourIncomeWeeklyM, labourIncomeWeeklyF) * Parameters.WEEKS_PER_MONTH;

            TaxEvaluation evaluatedTransfers = taxWrapper(hoursWorkedPerWeekM, hoursWorkedPerWeekF, dlltsdM, dlltsdF, originalIncomePerMonth, secondIncomePerMonth);

            yDispMonth = evaluatedTransfers.getDisposableIncomePerMonth();
            yBenAmountMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
            yGrossMonth = evaluatedTransfers.getGrossIncomePerMonth();
            demDbMatchTax = evaluatedTransfers.getMatch();
            idtaxDbDonor = demDbMatchTax.getCandidateID();
            setReceivedUC(evaluatedTransfers.getReceivedUC());
            setReceivedLegacyBenefits(evaluatedTransfers.getReceivedLegacyBenefit());
        } else {
            // intertemporal optimisations disabled

            updateNonLabourIncome();

            // prepare temporary storage variables
            MultiKey<? extends Labour> labourSupplyChoice = null;
            MultiKeyMap<Labour, Double> disposableIncomeMonthlyByLabourPairs = MultiKeyMap.multiKeyMap(new LinkedMap<>());
            MultiKeyMap<Labour, Double> benefitsReceivedMonthlyByLabourPairs = MultiKeyMap.multiKeyMap(new LinkedMap<>());
            MultiKeyMap<Labour, Double> grossIncomeMonthlyByLabourPairs = MultiKeyMap.multiKeyMap(new LinkedMap<>());
            MultiKeyMap<Labour, Match> taxDbMatchByLabourPairs = MultiKeyMap.multiKeyMap(new LinkedMap<>());
            LinkedHashSet<MultiKey<Labour>> possibleLabourCombinations = findPossibleLabourCombinations(); // Find possible labour combinations for this benefit unit
            MultiKeyMap<Labour, Double> labourSupplyUtilityRegressionScoresByLabourPairs = MultiKeyMap.multiKeyMap(new LinkedMap<>());

            // Sometimes one of the occupants of the couple will be retired (or even under the age to work, which is currently the age to leave home).
            // For this case, the person (not at risk of work)'s labour supply will always be zero, while the other person at risk of work has a choice over the single person Labour Supply set.
            if (Occupancy.Couple.equals(occupancy)) {

                for (MultiKey<? extends Labour> labourKey : possibleLabourCombinations) { //PB: for each possible discrete number of hours

                    // Sets values for regression score calculation
                    male.setLabourSupplyWeekly(labourKey.getKey(0));
                    female.setLabourSupplyWeekly(labourKey.getKey(1));

                    // Earnings are composed of the labour income and non-benefit non-employment income Yptciihs_dv() (this is monthly, so no need to multiply by WEEKS_PER_MONTH_RATIO)
                    double maleIncome = Parameters.WEEKS_PER_MONTH * male.getEarningsWeekly() + Math.sinh(male.getyMiscPersGrossMonth());
                    double femaleIncome = Parameters.WEEKS_PER_MONTH * female.getEarningsWeekly() + Math.sinh(female.getyMiscPersGrossMonth());
                    double originalIncomePerMonth = maleIncome + femaleIncome;
                    double secondIncomePerMonth = Math.min(maleIncome, femaleIncome);

                    TaxEvaluation evaluatedTransfers = taxWrapper(labourKey.getKey(0).getHours(male), labourKey.getKey(1).getHours(female), male.getDisability(), female.getDisability(), originalIncomePerMonth, secondIncomePerMonth);

                    yDispMonth = evaluatedTransfers.getDisposableIncomePerMonth();
                    yBenAmountMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
                    yGrossMonth = evaluatedTransfers.getGrossIncomePerMonth();
                    setReceivedUC(evaluatedTransfers.getReceivedUC());
                    setReceivedLegacyBenefits(evaluatedTransfers.getReceivedLegacyBenefit());

                    // Note that only benefitUnits at risk of work are considered, so at least one partner is at risk of work
                    double regressionScore = 0.;
                    if (male.atRiskOfWork()) { //If male has flexible labour supply
                        if (female.atRiskOfWork()) { //And female has flexible labour supply
                            //Follow utility process for couples
                            regressionScore = Parameters.getRegLabourSupplyUtilityCouples().getScore(this, BenefitUnit.Regressors.class);
                        } else if (!female.atRiskOfWork()) { //Male has flexible labour supply, female doesn't
                            //Follow utility process for single males for the UK
                            regressionScore = Parameters.getRegLabourSupplyUtilitySingleWithDependent().getScore(this, BenefitUnit.Regressors.class);
                            //In Italy, this should follow a separate set of estimates. One way is to differentiate between countries here; another would be to add a set of estimates for both countries, but for the UK have the same number as for singles
                            //Introduced a new category of estimates, Males/Females with Dependent to be used when only one of the couple is flexible in labour supply. In Italy, these have a separate set of estimates; in the UK they use the same estimates as "independent" singles
                        }
                    } else if (female.atRiskOfWork() && !male.atRiskOfWork()) { //Male not at risk of work - female must be at risk of work since only benefitUnits at risk are considered here
                        //Follow utility process for single female
                        regressionScore = Parameters.getRegLabourSupplyUtilitySingleWithDependent().getScore(this, BenefitUnit.Regressors.class);
                    } else throw new IllegalArgumentException("None of the partners are at risk of work! HHID " + getKey().getId());
                    if (!Parameters.checkFinite(regressionScore)) {
                        throw new RuntimeException("problem evaluating exponential regression score in labour supply module (1)");
                    }

                    disposableIncomeMonthlyByLabourPairs.put(labourKey, getDisposableIncomeMonthly());
                    benefitsReceivedMonthlyByLabourPairs.put(labourKey, getBenefitsReceivedPerMonth());
                    grossIncomeMonthlyByLabourPairs.put(labourKey, getGrossIncomeMonthly());
                    taxDbMatchByLabourPairs.put(labourKey, evaluatedTransfers.getMatch());
                    labourSupplyUtilityRegressionScoresByLabourPairs.put(labourKey, regressionScore); //XXX: Adult children could contribute their income to the hh, but then utility would have to be joint for a household with adult children, and they couldn't be treated separately as they are at the moment?
                }
            } else {
                // single adult

                if (Occupancy.Single_Male.equals(occupancy)) {

                    for (MultiKey<? extends Labour> labourKey : possibleLabourCombinations) {

                        male.setLabourSupplyWeekly(labourKey.getKey(0));
                        double originalIncomePerMonth = Parameters.WEEKS_PER_MONTH * male.getEarningsWeekly() + Math.sinh(male.getyMiscPersGrossMonth());
                        TaxEvaluation evaluatedTransfers = taxWrapper(labourKey.getKey(0).getHours(male), 0.0, male.getDisability(), -1, originalIncomePerMonth, 0.0);

                        yDispMonth = evaluatedTransfers.getDisposableIncomePerMonth();
                        yBenAmountMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
                        yGrossMonth = evaluatedTransfers.getGrossIncomePerMonth();
                        setReceivedUC(evaluatedTransfers.getReceivedUC());
                        setReceivedLegacyBenefits(evaluatedTransfers.getReceivedLegacyBenefit());

                        double regressionScore = 0.;
                        if (male.getAdultChildFlag() == 1) { //If adult children use labour supply estimates for male adult children
                            regressionScore = Parameters.getRegLabourSupplyUtilityACMales().getScore(this, Regressors.class);
                        } else {
                            regressionScore = Parameters.getRegLabourSupplyUtilityMales().getScore(this, Regressors.class);
                        }
                        if (!Parameters.checkFinite(regressionScore)) {
                            throw new RuntimeException("problem evaluating exponential regression score in labour supply module (2)");
                        }

                        disposableIncomeMonthlyByLabourPairs.put(labourKey, getDisposableIncomeMonthly());
                        benefitsReceivedMonthlyByLabourPairs.put(labourKey, getBenefitsReceivedPerMonth());
                        grossIncomeMonthlyByLabourPairs.put(labourKey, getGrossIncomeMonthly());
                        taxDbMatchByLabourPairs.put(labourKey, evaluatedTransfers.getMatch());
                        labourSupplyUtilityRegressionScoresByLabourPairs.put(labourKey, regressionScore);
                    }
                } else if (Occupancy.Single_Female.equals(occupancy)) {        //Occupant must be a single female

                    for (MultiKey<? extends Labour> labourKey : possibleLabourCombinations) {

                        female.setLabourSupplyWeekly(labourKey.getKey(1));
                        double originalIncomePerMonth = Parameters.WEEKS_PER_MONTH * female.getEarningsWeekly() + Math.sinh(female.getyMiscPersGrossMonth());
                        TaxEvaluation evaluatedTransfers = taxWrapper(0.0, labourKey.getKey(1).getHours(female), -1, female.getDisability(), originalIncomePerMonth, 0.0);

                        yDispMonth = evaluatedTransfers.getDisposableIncomePerMonth();
                        yBenAmountMonth = evaluatedTransfers.getBenefitsReceivedPerMonth();
                        yGrossMonth = evaluatedTransfers.getGrossIncomePerMonth();
                        setReceivedUC(evaluatedTransfers.getReceivedUC());
                        setReceivedLegacyBenefits(evaluatedTransfers.getReceivedLegacyBenefit());

                        double regressionScore = 0.;
                        if (female.getAdultChildFlag() == 1) { //If adult children use labour supply estimates for female adult children
                            regressionScore = Parameters.getRegLabourSupplyUtilityACFemales().getScore(this, BenefitUnit.Regressors.class);
                        } else {
                            regressionScore = Parameters.getRegLabourSupplyUtilityFemales().getScore(this, BenefitUnit.Regressors.class);
                        }
                        if (!Parameters.checkFinite(regressionScore)) {
                            throw new RuntimeException("problem evaluating exponential regression score in labour supply module (3)");
                        }
                        disposableIncomeMonthlyByLabourPairs.put(labourKey, getDisposableIncomeMonthly());
                        benefitsReceivedMonthlyByLabourPairs.put(labourKey, getBenefitsReceivedPerMonth());
                        grossIncomeMonthlyByLabourPairs.put(labourKey, getGrossIncomeMonthly());
                        taxDbMatchByLabourPairs.put(labourKey, evaluatedTransfers.getMatch());
                        labourSupplyUtilityRegressionScoresByLabourPairs.put(labourKey, regressionScore);
                    }
                }
            }
            if (labourSupplyUtilityRegressionScoresByLabourPairs.isEmpty()) {
                // error check

                System.out.print("\nlabourSupplyUtilityExponentialRegressionScoresByLabourPairs for household " + key.getId() + " with occupants ");
                if (male != null) {
                    System.out.print("male : " + male.getKey().getId() + ", ");
                }
                if (female != null) {
                    System.out.print("female : " + female.getKey().getId() + ", ");
                }
                System.out.print("is empty!");
            }

            //Sample labour supply from possible labour (pairs of) values
            double labourInnov = 0;

            // Check if:
            // - single occupant who was employed in preceding period
            // - or male of couple who was employed in preceding period
            // - or male is not at risk of work and female who was employed in preceding period
            // -> then persist employment innovation with probability Parameters.labour_innovation_employment_persistence_probability
            // Otherwise
            // -> persist employment innovation with probability Parameters.labour_innovation_notinemployment_persistence_probability
            if (occupancy.Single_Male.equals(occupancy) && male.atRiskOfWork() && male.getEmployed_Lag1() == 1) {
                    labourInnov = getLabourInnovation(Parameters.labour_innovation_employment_persistence_probability);
            } else if (occupancy.Single_Female.equals(occupancy) && female.atRiskOfWork() && female.getEmployed_Lag1() == 1) {
                    labourInnov = getLabourInnovation(Parameters.labour_innovation_employment_persistence_probability);
            } else if (occupancy.equals(Occupancy.Couple) &&
                    ((male.atRiskOfWork() && male.getEmployed_Lag1() == 1) ||
                            (!male.atRiskOfWork() && female.atRiskOfWork() && female.getEmployed_Lag1() == 1))) {
                labourInnov = getLabourInnovation(Parameters.labour_innovation_employment_persistence_probability);
            } else {
                labourInnov = getLabourInnovation(Parameters.labour_innovation_notinemployment_persistence_probability);
            }

            try {
                MultiKeyMap<Labour, Double> labourSupplyUtilityRegressionProbabilitiesByLabourPairs = convertRegressionScoresToProbabilities(labourSupplyUtilityRegressionScoresByLabourPairs);
                labourSupplyChoice = ManagerRegressions.multiEvent(labourSupplyUtilityRegressionProbabilitiesByLabourPairs, labourInnov);
                // labourRandomUniform is not updated here to avoid issues with search routine for labour market alignment
            } catch (RuntimeException e) {
                System.out.print("Could not determine labour supply choice for BU with ID: " + getKey().getId());
            }
            // populate labour supply
            if(model.debugCommentsOn && labourSupplyChoice!=null) {
                log.trace("labour supply choice " + labourSupplyChoice);
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
            if (Parameters.flagFormalChildcare && !Parameters.flagSuppressChildcareCosts) {
                updateChildcareCostPerWeek(model.getYear(), getRefPersonForDecisions().getDemAge());
            }
            if (Parameters.flagSocialCare && !Parameters.flagSuppressSocialCareCosts) {
                updateSocialCareCostPerWeek();
            }

            // populate disposable income
            yDispMonth = disposableIncomeMonthlyByLabourPairs.get(labourSupplyChoice);
            yBenAmountMonth = benefitsReceivedMonthlyByLabourPairs.get(labourSupplyChoice);
            yGrossMonth = grossIncomeMonthlyByLabourPairs.get(labourSupplyChoice);
            demDbMatchTax = taxDbMatchByLabourPairs.get(labourSupplyChoice);
            idtaxDbDonor = demDbMatchTax.getCandidateID();
        }

        //Update gross income variables for the household and all occupants:
        calculateBUIncome();
    }

    private MultiKeyMap<Labour, Double> convertRegressionScoresToProbabilities(MultiKeyMap<Labour, Double> regressionScoresMap) {
        MultiKeyMap<Labour, Double> labourSupplyUtilityRegressionProbabilitiesByLabourPairs = MultiKeyMap.multiKeyMap(new LinkedMap<>()); // Output map to update and return
        double logSumExp = getLogSumExp(regressionScoresMap);

        // Transform using the log-sum-exp
        for (MultiKey<? extends Labour> key : regressionScoresMap.keySet()) {
            double regressionScore = regressionScoresMap.get(key);
            double regressionProbability = Math.exp(regressionScore - logSumExp);
            labourSupplyUtilityRegressionProbabilitiesByLabourPairs.put(key, regressionProbability);
        }

        return labourSupplyUtilityRegressionProbabilitiesByLabourPairs;
    }

    private static double getLogSumExp(MultiKeyMap<Labour, Double> regressionScoresMap) {
        double maxRegressionScore = Double.NEGATIVE_INFINITY;
        double sumExpRegScoreMinusMax = 0.;
        double logSumExp = 0.;

        // Find maximum of regression scores
        for (double val : regressionScoresMap.values()) {
            if(val > maxRegressionScore){
                maxRegressionScore = val;
            }
        }

        // Calculate sum of exp() differences between each element and max
        for (double val : regressionScoresMap.values()) {
            sumExpRegScoreMinusMax += Math.exp(val - maxRegressionScore);
        }

        // Calculate log sum exp
        logSumExp = maxRegressionScore + Math.log(sumExpRegScoreMinusMax);
        return logSumExp;
    }

    /////////////////////////////////////////////////////////////////////////////////
    //
    //	Other Methods
    //
    ////////////////////////////////////////////////////////////////////////////////


    protected void calculateBUIncome() {

        /*
         * This method updates income variables for responsible persons in the household
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
        double equivalisedWeight = getEquivalisedWeight();
        Person male = getMale();
        Person female = getFemale();
        if (male != null && female != null) {

            // male
            double labourEarningsMaleMonthly = male.getEarningsWeekly(male.getLabourSupplyHoursWeekly()) * Parameters.WEEKS_PER_MONTH; //Level of monthly labour earnings
            male.setyEmpPersGrossMonth(asinh(labourEarningsMaleMonthly));
            double ypnbihsMaleMonthly = labourEarningsMaleMonthly + Math.sinh(male.getyMiscPersGrossMonth()); //personal non-benefit income per month
            male.setyNonBenPersGrossMonth(asinh(ypnbihsMaleMonthly));
            male.setCovidModuleGrossLabourIncome_Baseline(ypnbihsMaleMonthly); // Used in the Covid-19 labour supply module

            // female
            double labourEarningsFemaleMonthly = female.getEarningsWeekly(female.getLabourSupplyHoursWeekly()) * Parameters.WEEKS_PER_MONTH; //Level of monthly labour earnings
            female.setyEmpPersGrossMonth(asinh(labourEarningsFemaleMonthly)); //This follows asinh transform of labourEarnings
            double ypnbihsFemaleMonthly = labourEarningsFemaleMonthly + Math.sinh(female.getyMiscPersGrossMonth()); //In levels
            female.setyNonBenPersGrossMonth(asinh(ypnbihsFemaleMonthly)); //Set asinh transformed
            female.setCovidModuleGrossLabourIncome_Baseline(ypnbihsFemaleMonthly); // Used in the Covid-19 labour supply module

            // benefit unit income is the sum of male and female non-benefit income
            double tmpHHYpnbihs_dv = (ypnbihsMaleMonthly + ypnbihsFemaleMonthly) / equivalisedWeight; //Equivalised
            setTmpHHYpnbihs_dv_asinh(asinh(tmpHHYpnbihs_dv)); //Asinh transformation of HH non-benefit income

            //Based on the percentiles calculated by the collector, assign household to one of the quintiles of (equivalised) income distribution
            if(collector.getStats() != null) { //Collector only gets initialised when simulation starts running
                if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p20()) {
                    yHhQuintilesMonthC5 = Ydses_c5.Q1;
                } else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p40()) {
                    yHhQuintilesMonthC5 = Ydses_c5.Q2;
                } else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p60()) {
                    yHhQuintilesMonthC5 = Ydses_c5.Q3;
                } else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p80()) {
                    yHhQuintilesMonthC5 = Ydses_c5.Q4;
                } else {
                    yHhQuintilesMonthC5 = Ydses_c5.Q5;
                }
            }
        } else if(getOccupancy().equals(Occupancy.Single_Male)) {

            if (male != null) {

                double labourEarningsMaleMonthly = male.getEarningsWeekly(male.getLabourSupplyHoursWeekly()) * Parameters.WEEKS_PER_MONTH; //Level of monthly labour earnings
                male.setyEmpPersGrossMonth(asinh(labourEarningsMaleMonthly)); //This follows asinh transform of labourEarnings
                double ypnbihsMaleMonthly = labourEarningsMaleMonthly + Math.sinh(male.getyMiscPersGrossMonth()); //In levels
                male.setyNonBenPersGrossMonth(asinh(ypnbihsMaleMonthly)); //Set asinh transformed
                male.setCovidModuleGrossLabourIncome_Baseline(ypnbihsMaleMonthly); // Used in the Covid-19 labour supply module

                //BenefitUnit income is the male non-benefit income
                double tmpHHYpnbihs_dv = ypnbihsMaleMonthly / equivalisedWeight; //Equivalised
                setTmpHHYpnbihs_dv_asinh(asinh(tmpHHYpnbihs_dv)); //Asinh transformation of HH non-benefit income

                if(collector.getStats() != null) { //Collector only gets initialised when simulation starts running
                    if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p20()) {
                        yHhQuintilesMonthC5 = Ydses_c5.Q1;
                    } else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p40()) {
                        yHhQuintilesMonthC5 = Ydses_c5.Q2;
                    } else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p60()) {
                        yHhQuintilesMonthC5 = Ydses_c5.Q3;
                    } else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p80()) {
                        yHhQuintilesMonthC5 = Ydses_c5.Q4;
                    } else {
                        yHhQuintilesMonthC5 = Ydses_c5.Q5;
                    }
                }
            } else
                throw new RuntimeException("single male unit does not include a single male");
        } else {

            if (female != null) {

                //If not a couple nor a single male, occupancy must be single female
                double labourEarningsFemaleMonthly = female.getEarningsWeekly(female.getLabourSupplyHoursWeekly()) * Parameters.WEEKS_PER_MONTH; //Level of monthly labour earnings
                female.setyEmpPersGrossMonth(asinh(labourEarningsFemaleMonthly)); //This follows asinh transform of labourEarnings
                double ypnbihsFemaleMonthly = labourEarningsFemaleMonthly + Math.sinh(female.getyMiscPersGrossMonth()); //In levels
                female.setyNonBenPersGrossMonth(asinh(ypnbihsFemaleMonthly)); //Set asinh transformed
                female.setCovidModuleGrossLabourIncome_Baseline(ypnbihsFemaleMonthly); // Used in the Covid-19 labour supply module

                //BenefitUnit income is the female non-benefit income
                double tmpHHYpnbihs_dv = ypnbihsFemaleMonthly / equivalisedWeight; //Equivalised
                setTmpHHYpnbihs_dv_asinh(asinh(tmpHHYpnbihs_dv)); //Asinh transformation of HH non-benefit income

                if(collector.getStats() != null) { //Collector only gets initialised when simulation starts running

                    if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p20()) {
                        yHhQuintilesMonthC5 = Ydses_c5.Q1;
                    } else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p40()) {
                        yHhQuintilesMonthC5 = Ydses_c5.Q2;
                    } else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p60()) {
                        yHhQuintilesMonthC5 = Ydses_c5.Q3;
                    } else if(getTmpHHYpnbihs_dv_asinh() <= collector.getStats().getYdses_p80()) {
                        yHhQuintilesMonthC5 = Ydses_c5.Q4;
                    } else {
                        yHhQuintilesMonthC5 = Ydses_c5.Q5;
                    }
                }
            } else
                throw new RuntimeException("single female unit does not include a single male");
        }
    }

    public int getSize() {
        return members.size();
    }

    public void updateActivityOfPersonsWithinBenefitUnit() {
        updateActivity(getMale());
        updateActivity(getFemale());
    }

    private void updateActivity(Person person) {

        if (person!=null && !Les_c4.Student.equals(person.getLes_c4()) && !Les_c4.Retired.equals(person.getLes_c4())) {
            if (person.getLabourSupplyHoursWeekly() > 0) {
                person.setLes_c4(Les_c4.EmployedOrSelfEmployed);
            } else  {
                person.setLes_c4(Les_c4.NotEmployed);
            }
        }
    }


    // -------------------------------------------------------------------------------------------------------------
    // implements IDoubleSource for use with Regression classes - for use in DonorHousehold, not BenefitUnit objects
    // -------------------------------------------------------------------------------------------------------------

    public enum Regressors {

        Constant,
        couple_emp_2ft,
        couple_emp_2ne,
        couple_emp_2pt,
        couple_emp_ftne,
        couple_emp_ftpt,
        couple_emp_ptne,
        Cut1,       // ordered probit/logit cut points - ignore these when evaluating score
        Cut10,
        Cut2,
        Cut3,
        Cut4,
        Cut5,
        Cut6,
        Cut7,
        Cut8,
        Cut9,
        FemaleEduH_1,
        FemaleEduH_10,
        FemaleEduH_2,
        FemaleEduH_20,
        FemaleEduH_3,
        FemaleEduH_30,
        FemaleEduH_4,
        FemaleEduH_40,
        FemaleEduM_1,
        FemaleEduM_10,
        FemaleEduM_2,
        FemaleEduM_20,
        FemaleEduM_3,
        FemaleEduM_30,
        FemaleEduM_4,
        FemaleEduM_40,
        FemaleHoursAbove40,
        FemaleLeisure,                            //24*7 - labour supply weekly for Female
        FemaleLeisure_DChildren017,  //Male leisure interacted with dummy for presence of children aged 0 - 17
        FemaleLeisure_DChildren1317, //Male leisure interacted with dummy for presence of children aged 13-17
        FemaleLeisure_DChildren2Under,
        FemaleLeisure_DChildren36,   //Male leisure interacted with dummy for presence of children aged 3 - 6
        FemaleLeisure_DChildren712,  //Male leisure interacted with dummy for presence of children aged 7 - 12
        FemaleLeisure_dnc,
        FemaleLeisure_dnc02,
        FemaleLeisure_FemaleAge50Above,         //Female leisure interacted with dummy for age >= 50
        FemaleLeisure_FemaleAgeDiv100,                //Female Leisure interacted with age of Female
        FemaleLeisure_FemaleAgeSqDiv10000,
        FemaleLeisure_FemaleDeh_c3_Low,
        FemaleLeisure_FemaleDeh_c3_Medium,
        FemaleLeisure_IncomeDiv100,
        FemaleLeisure_NChildren017,
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
        FemaleLeisureSq,
        FixedCost_Female,
        FixedCost_Male,
        FixedCostByHighEducation,
        FixedCostFemale,
        FixedCostFemale_DChildren2Under,
        FixedCostFemale_Dlltsdsp,    //Fixed cost interacted with dummy for partner being long-term sick or disabled
        FixedCostFemale_Lesspc3_Student, //Fixed cost interacted with dummy for partner being a student
        FixedCostFemale_NChildren017,
        FixedCostFemale_NorthernRegions,
        FixedCostFemale_SouthernRegions,
        FixedCostFemaleByDchildren2under,
        FixedCostFemaleByNumberChildren,
        FixedCostMale,
        FixedCostMale_DChildren2Under,
        FixedCostMale_Dlltsdsp,    //Fixed cost interacted with dummy for partner being long-term sick or disabled
        FixedCostMale_Lesspc3_Student, //Fixed cost interacted with dummy for partner being a student
        FixedCostMale_NChildren017,
        FixedCostMale_NorthernRegions,
        FixedCostMale_SouthernRegions,
        FixedCostMaleByDchildren2under,
        FixedCostMaleByNumberChildren,
        Graduate,
        Homeownership_D, // Indicator: does the benefit unit own home?
        HoursFemale,
        HoursFemaleByAgeFemale,
        HoursFemaleByAgeFemaleSquared,
        HoursFemaleByDchildren13_17,
        HoursFemaleByDchildren2under,
        HoursFemaleByDchildren3_6,
        HoursFemaleByDchildren7_12,
        HoursFemaleByDelderly,
        HoursFemaleByDregion,
        HoursFemaleByIncome,
        HoursFemaleSquared,
        HoursMale,
        HoursMaleByAgeMale,
        HoursMaleByAgeMaleSquared,
        HoursMaleByDelderly,
        HoursMaleByDregion,
        HoursMaleByHoursFemale,
        HoursMaleByIncome,
        HoursMaleByNumberChildren,
        HoursMaleSquared,
        Hrs_36plus_Female,
        Hrs_36plus_Male,
        Income,
        IncomeByAge,
        IncomeByAgeSquared,
        IncomeByNumberChildren,
        IncomeDiv100,                             //Disposable monthly income from donor household divided by 100
        IncomeDiv100_DChildren2Under,            //Income divided by 100 interacted with dummy for presence of children aged 0-2 in the household
        IncomeDiv100_dnc,
        IncomeDiv100_dnc02,
        IncomeDiv100_FemaleAgeDiv100,
        IncomeDiv100_FemaleAgeSqDiv10000,
        IncomeDiv100_MaleAgeDiv100,
        IncomeDiv100_MaleAgeSqDiv10000,
        IncomeDiv100_MeanPartnersAgeDiv100,        //Income divided by 100 interacted with mean age of male and female in the household divided by 100
        IncomeDiv100_MeanPartnersAgeSqDiv10000,     //Income divided by 100 interacted with square of mean age of male and female in the household divided by 100
        IncomeDiv100_NChildren017,                 //Income divided by 100 interacted with the number of children aged 0-17
        IncomeSqDiv10000,                        //Income squared divided by 10000
        IncomeSquared,
        L1_lhw_1,
        L1_lhw_10,
        L1_lhw_2,
        L1_lhw_20,
        L1_lhw_3,
        L1_lhw_30,
        L1_lhw_4,
        L1_lhw_40,
        L1_lhw_Female_1,
        L1_lhw_Female_10,
        L1_lhw_Female_11,
        L1_lhw_Female_12,
        L1_lhw_Female_13,
        L1_lhw_Female_14,
        L1_lhw_Female_2,
        L1_lhw_Female_20,
        L1_lhw_Female_21,
        L1_lhw_Female_22,
        L1_lhw_Female_23,
        L1_lhw_Female_24,
        L1_lhw_Female_3,
        L1_lhw_Female_30,
        L1_lhw_Female_31,
        L1_lhw_Female_32,
        L1_lhw_Female_33,
        L1_lhw_Female_34,
        L1_lhw_Female_4,
        L1_lhw_Female_40,
        L1_lhw_Female_41,
        L1_lhw_Female_42,
        L1_lhw_Female_43,
        L1_lhw_Female_44,
        L1_lhw_Male_1,
        L1_lhw_Male_10,
        L1_lhw_Male_11,
        L1_lhw_Male_12,
        L1_lhw_Male_13,
        L1_lhw_Male_14,
        L1_lhw_Male_2,
        L1_lhw_Male_20,
        L1_lhw_Male_21,
        L1_lhw_Male_22,
        L1_lhw_Male_23,
        L1_lhw_Male_24,
        L1_lhw_Male_3,
        L1_lhw_Male_30,
        L1_lhw_Male_31,
        L1_lhw_Male_32,
        L1_lhw_Male_33,
        L1_lhw_Male_34,
        L1_lhw_Male_4,
        L1_lhw_Male_40,
        L1_lhw_Male_41,
        L1_lhw_Male_42,
        L1_lhw_Male_43,
        L1_lhw_Male_44,
        Liwwh_1,
        Liwwh_10,
        Liwwh_2,
        Liwwh_20,
        Liwwh_3,
        Liwwh_30,
        Liwwh_4,
        Liwwh_40,
        Liwwh_5,
        Liwwh_50,
        Liwwh_6,
        Liwwh_60,
        Liwwh_Female_01,
        Liwwh_Female_02,
        Liwwh_Female_03,
        Liwwh_Female_04,
        Liwwh_Female_05,
        Liwwh_Female_06,
        Liwwh_Female_1,
        Liwwh_Female_10,
        Liwwh_Female_11,
        Liwwh_Female_12,
        Liwwh_Female_13,
        Liwwh_Female_14,
        Liwwh_Female_15,
        Liwwh_Female_16,
        Liwwh_Female_2,
        Liwwh_Female_20,
        Liwwh_Female_21,
        Liwwh_Female_22,
        Liwwh_Female_23,
        Liwwh_Female_24,
        Liwwh_Female_25,
        Liwwh_Female_26,
        Liwwh_Female_3,
        Liwwh_Female_30,
        Liwwh_Female_31,
        Liwwh_Female_32,
        Liwwh_Female_33,
        Liwwh_Female_34,
        Liwwh_Female_35,
        Liwwh_Female_36,
        Liwwh_Female_4,
        Liwwh_Female_40,
        Liwwh_Female_41,
        Liwwh_Female_42,
        Liwwh_Female_43,
        Liwwh_Female_44,
        Liwwh_Female_45,
        Liwwh_Female_46,
        Liwwh_Female_5,
        Liwwh_Female_50,
        Liwwh_Female_51,
        Liwwh_Female_52,
        Liwwh_Female_53,
        Liwwh_Female_54,
        Liwwh_Female_55,
        Liwwh_Female_56,
        Liwwh_Female_6,
        Liwwh_Female_60,
        Liwwh_Female_61,
        Liwwh_Female_62,
        Liwwh_Female_63,
        Liwwh_Female_64,
        Liwwh_Female_65,
        Liwwh_Female_66,
        Liwwh_Male_01,
        Liwwh_Male_02,
        Liwwh_Male_03,
        Liwwh_Male_04,
        Liwwh_Male_05,
        Liwwh_Male_06,
        Liwwh_Male_1,
        Liwwh_Male_10,
        Liwwh_Male_11,
        Liwwh_Male_12,
        Liwwh_Male_13,
        Liwwh_Male_14,
        Liwwh_Male_15,
        Liwwh_Male_16,
        Liwwh_Male_2,
        Liwwh_Male_20,
        Liwwh_Male_21,
        Liwwh_Male_22,
        Liwwh_Male_23,
        Liwwh_Male_24,
        Liwwh_Male_25,
        Liwwh_Male_26,
        Liwwh_Male_3,
        Liwwh_Male_30,
        Liwwh_Male_31,
        Liwwh_Male_32,
        Liwwh_Male_33,
        Liwwh_Male_34,
        Liwwh_Male_35,
        Liwwh_Male_36,
        Liwwh_Male_4,
        Liwwh_Male_40,
        Liwwh_Male_41,
        Liwwh_Male_42,
        Liwwh_Male_43,
        Liwwh_Male_44,
        Liwwh_Male_45,
        Liwwh_Male_46,
        Liwwh_Male_5,
        Liwwh_Male_50,
        Liwwh_Male_51,
        Liwwh_Male_52,
        Liwwh_Male_53,
        Liwwh_Male_54,
        Liwwh_Male_55,
        Liwwh_Male_56,
        Liwwh_Male_6,
        Liwwh_Male_60,
        Liwwh_Male_61,
        Liwwh_Male_62,
        Liwwh_Male_63,
        Liwwh_Male_64,
        Liwwh_Male_65,
        Liwwh_Male_66,
        MaleEduH_1,
        MaleEduH_10,
        MaleEduH_2,
        MaleEduH_20,
        MaleEduH_3,
        MaleEduH_30,
        MaleEduH_4,
        MaleEduH_40,
        MaleEduM_1,
        MaleEduM_10,
        MaleEduM_2,
        MaleEduM_20,
        MaleEduM_3,
        MaleEduM_30,
        MaleEduM_4,
        MaleEduM_40,
        MaleHoursAbove40,
        MaleLeisure,                            //24*7 - labour supply weekly for male
        MaleLeisure_DChildren017,  //Male leisure interacted with dummy for presence of children aged 0 - 17
        MaleLeisure_DChildren1317, //Male leisure interacted with dummy for presence of children aged 13-17
        MaleLeisure_DChildren2Under,
        MaleLeisure_DChildren36,   //Male leisure interacted with dummy for presence of children aged 3 - 6
        MaleLeisure_DChildren712,  //Male leisure interacted with dummy for presence of children aged 7 - 12
        MaleLeisure_dnc,
        MaleLeisure_dnc02,
        MaleLeisure_FemaleLeisure,            //Male leisure interacted with female leisure
        MaleLeisure_IncomeDiv100,
        MaleLeisure_MaleAge50Above,         //Male leisure interacted with dummy for age >= 50
        MaleLeisure_MaleAgeDiv100,                //Male Leisure interacted with age of male
        MaleLeisure_MaleAgeSqDiv10000,
        MaleLeisure_MaleDeh_c3_Low,
        MaleLeisure_MaleDeh_c3_Medium,
        MaleLeisure_NChildren017,
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
        MaleLeisureSq,
        MaximumAge, // Returns maximum age of responsible individuals in the benefit (tax) unit
        MinimumAge, // Returns maximum age of all people in benefit unit
        n_children_0,
        n_children_1,
        n_children_10,
        n_children_11,
        n_children_12,
        n_children_13,
        n_children_14,
        n_children_2,
        n_children_3,
        n_children_4,
        n_children_5,
        n_children_6,
        n_children_7,
        n_children_8,
        n_children_9,
        NumberChildren04,  // Return number of children aged <0;5)
        NumberChildren1017,
        NumberChildren517, // Return number of children aged <5;17>
        NumberChildren59,
        NumberMembersOver17, // Return number of members of benefit unit aged over 17
        single_emp_ft,
        single_emp_ne,
        single_emp_pt,
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
        Year_transformed,

    }

    public int getIntValue(Enum<?> variableID) {

        switch ((Regressors) variableID) {

            case MaximumAge -> {
                Person male = getMale();
                Person female = getFemale();
                int maxAgeBu = 0;
                if (male!=null && female!=null) {
                    if (male.getDemAge() >= female.getDemAge()) {
                        maxAgeBu = male.getDemAge();
                    } else {
                        maxAgeBu = female.getDemAge();
                    }
                } else if (male!=null) {
                    maxAgeBu = male.getDemAge();
                } else {
                    maxAgeBu = female.getDemAge();
                }
                return maxAgeBu;
            }
            case MinimumAge -> {
                int age = 200;
                for (Person person : members) {
                    if (person.getDemAge() < age) age = person.getDemAge();
                }
                return age;
            }
            case NumberMembersOver17 -> { // Simulated Benefit unit can have at most 2 persons over 17
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return 2;
                } else {
                    return 1;
                }
            }
            case NumberChildren04 -> {
                return getNumberChildren(0,4);
            }
            case NumberChildren59 -> {
                return getNumberChildren(5,9);
            }
            case NumberChildren1017 -> {
                return getNumberChildren(10,17);
            }
            case NumberChildren517 -> {
                return getNumberChildren(5, 17);
            }
            default ->
                throw new IllegalArgumentException("Unsupported variable " + variableID.name() + " in DonorHousehold.getIntValue");
        }
    }

    public double getDoubleValue(Enum<?> variableID) {

        switch ((Regressors) variableID) {

            case IncomeDiv100 -> {                             //Disposable monthly income from donor household divided by 100
                return (getDisposableIncomeMonthlyUpratedToBasePriceYear() -
                        getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear()) * 1.e-2;
            }
            case IncomeSqDiv10000 -> {                        //Income squared divided by 10000
                return (getDisposableIncomeMonthlyUpratedToBasePriceYear() -
                            getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear()) *
                        (getDisposableIncomeMonthlyUpratedToBasePriceYear() -
                            getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear()) * 1.e-4;
            }
            case IncomeDiv100_MeanPartnersAgeDiv100 -> {        //Income divided by 100 interacted with mean age of male and female in the household divided by 100
                if(getFemale() == null) {        //Single so no need for mean age
                    return getDisposableIncomeMonthlyUpratedToBasePriceYear() * getMale().getDemAge() * 1.e-4;
                } else if(getMale() == null) {
                    return getDisposableIncomeMonthlyUpratedToBasePriceYear() * getFemale().getDemAge() * 1.e-4;
                } else {        //Must be a couple, so use mean age
                    double meanAge = (getFemale().getDemAge() + getMale().getDemAge()) * 0.5;
                    return getDisposableIncomeMonthlyUpratedToBasePriceYear() * meanAge * 1.e-4;
                }
            }
            case IncomeDiv100_MeanPartnersAgeSqDiv10000 -> {     //Income divided by 100 interacted with square of mean age of male and female in the household divided by 10000
                if(getFemale() == null) {        //Single so no need for mean age
                    return getDisposableIncomeMonthlyUpratedToBasePriceYear() * getMale().getDemAge() * getMale().getDemAge() * 1.e-6;
                } else if(getMale() == null) {
                    return getDisposableIncomeMonthlyUpratedToBasePriceYear() * getFemale().getDemAge() * getFemale().getDemAge() * 1.e-6;
                } else {        //Must be a couple, so use mean age
                    double meanAge = (getFemale().getDemAge() + getMale().getDemAge()) * 0.5;
                    return getDisposableIncomeMonthlyUpratedToBasePriceYear() * meanAge * meanAge * 1.e-6;
                }
            }
            case IncomeDiv100_NChildren017, IncomeDiv100_dnc -> {                 //Income divided by 100 interacted with the number of children aged 0-17
                return (getDisposableIncomeMonthlyUpratedToBasePriceYear() -
                        getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear()) * (double)getNumberChildren(0,17) * 1.e-2;
            }
            case IncomeDiv100_DChildren2Under -> {            //Income divided by 100 interacted with dummy for presence of children aged 0-2 in the household
                return (getDisposableIncomeMonthlyUpratedToBasePriceYear() -
                        getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear()) * getIndicatorChildren(0,2).ordinal() * 1.e-2;
            }
            case MaleLeisure -> {                            //24*7 - labour supply weekly for male
                return Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly();
            }
            case MaleLeisureSq -> {
                return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly()) * (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
            }
            case MaleLeisure_IncomeDiv100 -> {
                return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly()) * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-2;
            }
            case MaleLeisure_MaleAgeDiv100 -> {                //Male Leisure interacted with age of male
                return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly()) * getMale().getDemAge() * 1.e-2;
            }
            case MaleLeisure_MaleAgeSqDiv10000 -> {
                return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly()) * getMale().getDemAge() * getMale().getDemAge() * 1.e-4;
            }
            case MaleLeisure_NChildren017, MaleLeisure_dnc -> {
                return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly()) * (double)getNumberChildren(0,17);
            }
            case MaleLeisure_DChildren2Under -> {
                return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly()) * getIndicatorChildren(0,2).ordinal();
            }
            case MaleLeisure_MaleDeh_c3_Low -> {
                if(getMale().getDeh_c3().equals(Education.Low)) {
                    return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
                } else return 0.;
            }
            case MaleLeisure_MaleDeh_c3_Medium -> {
                if(getMale().getDeh_c3().equals(Education.Medium)) {
                    return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
                } else return 0.;
            }
            case MaleLeisure_UKC -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKC)) {
                        return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case MaleLeisure_UKD -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKD)) {
                        return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case MaleLeisure_UKE -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKE)) {
                        return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case MaleLeisure_UKF -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKF)) {
                        return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case MaleLeisure_UKG -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKG)) {
                        return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case MaleLeisure_UKH -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKH)) {
                        return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case MaleLeisure_UKJ -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKJ)) {
                        return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case MaleLeisure_UKK -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKK)) {
                        return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case MaleLeisure_UKL -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKL)) {
                        return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case MaleLeisure_UKM -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKM)) {
                        return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case MaleLeisure_UKN -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKN)) {
                        return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case MaleLeisure_MaleAge50Above -> {
                if (getMale().getDemAge() >= 50) {
                    return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly());
                } else return 0.;
            }
            case MaleLeisure_FemaleLeisure -> {            //Male leisure interacted with female leisure
                return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly()) * (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
            }
            case FemaleLeisure -> {                            //24*7 - labour supply weekly for Female
                return Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly();
            }
            case FemaleLeisureSq -> {
                return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly()) * (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
            }
            case FemaleLeisure_IncomeDiv100 -> {
                return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly()) * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-2;
            }
            case FemaleLeisure_FemaleAgeDiv100 -> {                //Female Leisure interacted with age of Female
                return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly()) * getFemale().getDemAge() * 1.e-2;
            }
            case FemaleLeisure_FemaleAgeSqDiv10000 -> {
                return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly()) * getFemale().getDemAge() * getFemale().getDemAge() * 1.e-4;
            }
            case FemaleLeisure_NChildren017, FemaleLeisure_dnc -> {
                return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly()) * (double)getNumberChildren(0,17);
            }
            case FemaleLeisure_DChildren2Under -> {
                return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly()) * getIndicatorChildren(0,2).ordinal();
            }
            case FemaleLeisure_FemaleDeh_c3_Low -> {
                if(getFemale().getDeh_c3().equals(Education.Low)) {
                    return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
                } else return 0.;
            }
            case FemaleLeisure_FemaleDeh_c3_Medium -> {
                if(getFemale().getDeh_c3().equals(Education.Medium)) {
                    return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
                } else return 0.;
            }
            case FemaleLeisure_UKC -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKC)) {
                        return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case FemaleLeisure_UKD -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKD)) {
                        return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case FemaleLeisure_UKE -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKE)) {
                        return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case FemaleLeisure_UKF -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKF)) {
                        return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case FemaleLeisure_UKG -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKG)) {
                        return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case FemaleLeisure_UKH -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKH)) {
                        return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case FemaleLeisure_UKJ -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKJ)) {
                        return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case FemaleLeisure_UKK -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKK)) {
                        return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case FemaleLeisure_UKL -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKL)) {
                        return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case FemaleLeisure_UKM -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKM)) {
                        return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case FemaleLeisure_UKN -> {
                if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKN)) {
                        return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
            }
            case FemaleLeisure_FemaleAge50Above -> {
                if (getFemale().getDemAge() >= 50) {
                    return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly());
                } else return 0.;
                //Note: In the previous version of the model, Fixed Cost was returning -1 to match the regression coefficients
            }
            case FixedCostMale, FixedCost_Male -> {
                if(getMale() != null && getMale().getLabourSupplyHoursWeekly() > 0) {
                    return 1.;
                } else return 0.;
            }
            case FixedCost_Female -> {
                if(getFemale() != null && getFemale().getLabourSupplyHoursWeekly() > 0) {
                    return 1.;
                } else return 0.;
            }
            case FixedCostMale_NorthernRegions -> {
                if(getMale().getLabourSupplyHoursWeekly() > 0 && (region.equals(Region.ITC) || region.equals(Region.ITH))) {
                    return 1.;
                } else return 0.;
            }
            case FixedCostMale_SouthernRegions -> {
                if(getMale().getLabourSupplyHoursWeekly() > 0 && (region.equals(Region.ITF) || region.equals(Region.ITG))) {
                    return 1.;
                } else return 0.;
            }
            case FixedCostFemale -> {
                if(getFemale().getLabourSupplyHoursWeekly() > 0) {
                    return 1.;
                } else return 0.;
            }
            case FixedCostFemale_NorthernRegions -> {
                if(getFemale().getLabourSupplyHoursWeekly() > 0 && (region.equals(Region.ITC) || region.equals(Region.ITH))) {
                    return 1.;
                } else return 0.;
            }
            case FixedCostFemale_SouthernRegions -> {
                if(getFemale().getLabourSupplyHoursWeekly() > 0 && (region.equals(Region.ITF) || region.equals(Region.ITG))) {
                    return 1.;
                } else return 0.;
            }
            case FixedCostMale_NChildren017 -> {
                if(getMale().getLabourSupplyHoursWeekly() > 0) {
                    return getNumberChildren(0,17);
                } else return 0.;
            }
            case FixedCostMale_DChildren2Under -> {
                if(getMale().getLabourSupplyHoursWeekly() > 0) {
                    return getIndicatorChildren(0,2).ordinal();
                } else return 0.;
            }
            case FixedCostFemale_NChildren017 -> {
                if(getFemale().getLabourSupplyHoursWeekly() > 0) {
                    return getNumberChildren(0,17);
                } else return 0.;
            }
            case FixedCostFemale_DChildren2Under -> {
                if(getFemale().getLabourSupplyHoursWeekly() > 0) {
                    return getIndicatorChildren(0,2).ordinal();
                } else return 0.;
            }
            case MaleHoursAbove40 -> {
                if (getMale().getLabourSupplyHoursWeekly() >= 40) {
                    return 1.;
                } else return 0.;
            }
            case FemaleHoursAbove40 -> {
                if (getFemale().getLabourSupplyHoursWeekly() >= 40) {
                    return 1.;
                } else return 0.;
                //Additional regressors for single female or single male benefitUnits:
                //Note: couples in which one person is not at risk of work have utility set according to the process for singles
            }
            case MaleLeisure_DChildren1317 -> { //Male leisure interacted with dummy for presence of children aged 13-17
                return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly()) * getIndicatorChildren(13,17).ordinal();
            }
            case MaleLeisure_DChildren712 -> {  //Male leisure interacted with dummy for presence of children aged 7 - 12
                return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly()) * getIndicatorChildren(7,12).ordinal();
            }
            case MaleLeisure_DChildren36 -> {   //Male leisure interacted with dummy for presence of children aged 3 - 6
                return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly()) * getIndicatorChildren(3,6).ordinal();
            }
            case MaleLeisure_DChildren017 -> {  //Male leisure interacted with dummy for presence of children aged 0 - 17
                if(getNumberChildren(0,17) > 0) { //Instead of creating a new variable, use number of children aged 0 - 17
                    return Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly();
                } else return 0.;
                //The following two regressors refer to a partner in single LS model - this is for those with an inactive partner, but not everyone will have a partner so check for nulls
            }
            case FixedCostMale_Dlltsdsp -> {    //Fixed cost interacted with dummy for partner being long-term sick or disabled
                if(getMale().getLabourSupplyHoursWeekly() > 0) {
                    if(getFemale() != null) {
                        return getFemale().getDlltsd().ordinal(); //==1 if partner is long-term sick or disabled
                    } else return 0.;
                } else return 0.;
            }
            case FixedCostMale_Lesspc3_Student -> { //Fixed cost interacted with dummy for partner being a student
                if(getMale().getLabourSupplyHoursWeekly() > 0) {
                    if(getFemale() != null && getFemale().getLes_c4().equals(Les_c4.Student)) {
                        return 1.; //Partner must be female - if a student, return 1
                    } else return 0.;
                } else return 0.;

            }
            case FemaleLeisure_DChildren1317 -> { //Male leisure interacted with dummy for presence of children aged 13-17
                return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly()) * getIndicatorChildren(13,17).ordinal();
            }
            case FemaleLeisure_DChildren712 -> {  //Male leisure interacted with dummy for presence of children aged 7 - 12
                return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly()) * getIndicatorChildren(7,12).ordinal();
            }
            case FemaleLeisure_DChildren36 -> {   //Male leisure interacted with dummy for presence of children aged 3 - 6
                return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly()) * getIndicatorChildren(3,6).ordinal();
            }
            case FemaleLeisure_DChildren017 -> {  //Male leisure interacted with dummy for presence of children aged 0 - 17
                if(getNumberChildren(0,17) > 0) { //Instead of creating a new variable, use number of children aged 0 - 17
                    return Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly();
                } else return 0.;
            }
            case FixedCostFemale_Dlltsdsp -> {    //Fixed cost interacted with dummy for partner being long-term sick or disabled
                if(getFemale().getLabourSupplyHoursWeekly() > 0) {
                    if(getMale() != null) {
                        return getMale().getDlltsd().ordinal(); //==1 if partner is long-term sick or disabled
                    } else return 0.;
                } else return 0.;
            }
            case FixedCostFemale_Lesspc3_Student -> {
                if(getFemale().getLabourSupplyHoursWeekly() > 0) {
                    if(getMale() != null && getMale().getLes_c4().equals(Les_c4.Student)) {
                        return 1.; //Partner must be male - if a student, return 1
                    } else return 0.;
                } else return 0.;


                //Values are divided by powers of 10, as in the tables of Bargain et al. (2014) Working Paper
            }
            case IncomeSquared -> {        //Income is disposable income, inputed from 'donor' benefitUnits in EUROMOD
                return getDisposableIncomeMonthlyUpratedToBasePriceYear() * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-4;
            }
            case HoursMaleSquared -> {
                return getMale().getLabourSupplyHoursWeekly() * getMale().getLabourSupplyHoursWeekly();
            }
            case Hrs_36plus_Male -> {
                if (getMale() != null) {
                    return getMale().getLabourSupplyHoursWeekly() >=36 ? 1.:0.;
                } else{
                    return 0.;
                }
            }
            case HoursFemaleSquared -> {
                return getFemale().getLabourSupplyHoursWeekly() * getFemale().getLabourSupplyHoursWeekly();
            }
            case Hrs_36plus_Female -> {
                if (getFemale() != null) {
                    return getFemale().getLabourSupplyHoursWeekly() >=36 ? 1.:0.;
                } else{
                    return 0.;
                }
            }
            case HoursMaleByIncome -> {
                return getMale().getLabourSupplyHoursWeekly() * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-3;
            }
            case HoursFemaleByIncome -> {
                return getFemale().getLabourSupplyHoursWeekly() * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-3;
            }
            case HoursMaleByHoursFemale -> {
                return getMale().getLabourSupplyHoursWeekly() * getFemale().getLabourSupplyHoursWeekly() * 1.e-3;
            }
            case Income -> {
                return getDisposableIncomeMonthlyUpratedToBasePriceYear();
            }
            case IncomeByAge -> {        //Use mean age for couples
                if(getFemale() == null) {        //Single so no need for mean age
                    return getDisposableIncomeMonthlyUpratedToBasePriceYear() * getMale().getDemAge() * 1.e-1;
                } else if(getMale() == null) {
                    return getDisposableIncomeMonthlyUpratedToBasePriceYear() * getFemale().getDemAge() * 1.e-1;
                } else {        //Must be a couple, so use mean age
                    double meanAge = (getFemale().getDemAge() + getMale().getDemAge()) * 0.5;
                    return getDisposableIncomeMonthlyUpratedToBasePriceYear() * meanAge * 1.e-1;
                }
            }
            case IncomeByAgeSquared -> {        //Use mean age for couples
                if(getFemale() == null) {        //Single so no need for mean age
                    return getDisposableIncomeMonthlyUpratedToBasePriceYear() * getMale().getDemAge() * getMale().getDemAge() * 1.e-2;
                } else if(getMale() == null) {
                    return getDisposableIncomeMonthlyUpratedToBasePriceYear() * getFemale().getDemAge() * getFemale().getDemAge() * 1.e-2;
                } else {        //Must be a couple, so use mean age
                    double meanAge = (getFemale().getDemAge() + getMale().getDemAge()) * 0.5;
                    return getDisposableIncomeMonthlyUpratedToBasePriceYear() * meanAge * meanAge * 1.e-2;
                }
            }
            case IncomeByNumberChildren -> {
                return getDisposableIncomeMonthlyUpratedToBasePriceYear() * getNumberChildrenAll();
            }
            case HoursMale -> {
                return getMale().getLabourSupplyHoursWeekly();
            }
            case HoursMaleByAgeMale -> {
                return getMale().getLabourSupplyHoursWeekly() * getMale().getDemAge() * 1.e-1;
            }
            case HoursMaleByAgeMaleSquared -> {
                return getMale().getLabourSupplyHoursWeekly() * getMale().getDemAge() * getMale().getDemAge() * 1.e-2;
            }
            case HoursMaleByNumberChildren -> {
                return getMale().getLabourSupplyHoursWeekly() * getNumberChildrenAll();
            }
            case HoursMaleByDelderly -> {        //Appears only in Single Males regression, not Couple.
                return 0.;        //Our model doesn't take account of elderly (as people move out of parental home when 18 years old, and we do not provide a mechanism for parents to move back in.
            }
            case HoursMaleByDregion -> {
                if(model.getCountry().equals(Country.IT)) {
                    if(getRegion().equals(Region.ITF) || getRegion().equals(Region.ITG)) {        //For South Italy (Sud) and Islands (Isole)
                        return getMale().getLabourSupplyHoursWeekly();
                    } else return 0.;
                } else if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKI)) {        //For London
                        return getMale().getLabourSupplyHoursWeekly();
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - household " + this.getId() + " has region " + getRegion() + " which is not yet handled in DonorHousehold.getDoubleValue()!");

            }
            case HoursFemale -> {
                return getFemale().getLabourSupplyHoursWeekly();
            }
            case HoursFemaleByAgeFemale -> {
                return getFemale().getLabourSupplyHoursWeekly() * getFemale().getDemAge() * 1.e-1;
            }
            case HoursFemaleByAgeFemaleSquared -> {
                return getFemale().getLabourSupplyHoursWeekly() * getFemale().getDemAge() * getFemale().getDemAge() * 1.e-2;
            }
            case HoursFemaleByDchildren2under -> {
                return getFemale().getLabourSupplyHoursWeekly() * getIndicatorChildren(0,2).ordinal();
            }
            case HoursFemaleByDchildren3_6 -> {
                return getFemale().getLabourSupplyHoursWeekly() * getIndicatorChildren(3,6).ordinal();
            }
            case HoursFemaleByDchildren7_12 -> {
                return getFemale().getLabourSupplyHoursWeekly() * getIndicatorChildren(7,12).ordinal();
            }
            case HoursFemaleByDchildren13_17 -> {
                return getFemale().getLabourSupplyHoursWeekly() * getIndicatorChildren(13,17).ordinal();
            }
            case HoursFemaleByDelderly -> {
                return 0.;        //Our model doesn't take account of elderly (as people move out of parental home when 18 years old, and we do not provide a mechanism for parents to move back in.
            }
            case HoursFemaleByDregion -> {        //Value of hours are already taken into account by multiplying regression coefficients in Parameters class
                if(model.getCountry().equals(Country.IT)) {
                    if(getRegion().equals(Region.ITF) || getRegion().equals(Region.ITG)) {        //For South Italy (Sud) and Islands (Isole)
                        return getFemale().getLabourSupplyHoursWeekly();
                    } else return 0.;
                } else if(model.getCountry().equals(Country.UK)) {
                    if(getRegion().equals(Region.UKI)) {        //For London
                        return getFemale().getLabourSupplyHoursWeekly();
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - household " + this.getKey().getId() + " has region " + getRegion() + " which is not yet handled in DonorHousehold.getDoubleValue()!");

                //The following regressors for FixedCosts appear as negative in the Utility regression, and so are multiplied by a factor of -1 below.
                //The following regressors only apply when the male hours worked is greater than 0

            }
            case FixedCostMaleByNumberChildren -> {
                if(getMale().getLabourSupplyHoursWeekly() > 0) {
                    return - getNumberChildrenAll();        //Return negative as costs appear negative in utility function equation
                } else return 0.;

            }
            case FixedCostMaleByDchildren2under -> {
                if(getMale().getLabourSupplyHoursWeekly() > 0) {
                    return - getIndicatorChildren(0,2).ordinal();        //Return negative as costs appear negative in utility function equation
                } else return 0.;

                //The following regressors only apply when the female hours worked is greater than 0

            }
            case FixedCostFemaleByNumberChildren -> {
                if(getFemale().getLabourSupplyHoursWeekly() > 0) {
                    return - getNumberChildrenAll();        //Return negative as costs appear negative in utility function equation
                } else return 0.;

            }
            case FixedCostFemaleByDchildren2under -> {
                if(getFemale().getLabourSupplyHoursWeekly() > 0) {
                    return - getIndicatorChildren(0,2).ordinal();        //Return negative as costs appear negative in utility function equation
                } else return 0.;

                //Only appears in regressions for Singles not Couples.  Applies when the single person in the household has hours worked > 0
            }
            case FixedCostByHighEducation -> {
                if(getFemale() == null) {        //For single males
                    if(getMale().getLabourSupplyHoursWeekly() > 0) {
                        return getMale().getDeh_c3().equals(Education.High) ? -1. : 0.;
                    } else return 0.;
                } else if (getMale() == null) {    //For single females
                    if(getFemale().getLabourSupplyHoursWeekly() > 0) {
                        return getFemale().getDeh_c3().equals(Education.High) ? -1. : 0.;
                    } else return 0.;
                } else throw new IllegalArgumentException("Error - FixedCostByHighEducation regressor should only be called for Households containing single people (with or without children), however household " + key.getId() + " has a couple, with male " + getMale().getKey().getId() + " and female " + getFemale().getKey().getId());

                // Additional variables for re-estimated LS processes
            }
            case IncomeDiv100_MaleAgeDiv100 -> {
                    return (getDisposableIncomeMonthlyUpratedToBasePriceYear() -
                            getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear()) * getMale().getDemAge() * 1.e-4;
            }
            case IncomeDiv100_MaleAgeSqDiv10000 -> {
                return (getDisposableIncomeMonthlyUpratedToBasePriceYear() -
                        getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear()) * getMale().getDemAge() * 1.e-6;
            }
            case IncomeDiv100_FemaleAgeDiv100 -> {
                return (getDisposableIncomeMonthlyUpratedToBasePriceYear() -
                        getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear()) * getFemale().getDemAge() * 1.e-4;
            }
            case IncomeDiv100_FemaleAgeSqDiv10000 -> {
                return (getDisposableIncomeMonthlyUpratedToBasePriceYear() -
                        getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear()) * getFemale().getDemAge() * 1.e-6;
            }
            case IncomeDiv100_dnc02 -> {
                return (getDisposableIncomeMonthlyUpratedToBasePriceYear() -
                        getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear()) * getIndicatorChildren(0,1).ordinal() * 1.e-2;
            }
            case L1_lhw_1 -> {
                // Coefficient to be applied to lagged hours of work of female member of BU interacted with "alternative 1" of hours of labour supply
                // Note: labour supply value for person under evaluation is set to the alternative being considered in the update labour supply process
                return (getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_10 -> {
                return (getMale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_2 -> {
                return (getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_20 -> {
                return (getMale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_3 -> {
                return (getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_30 -> {
                return (getMale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_4 -> {
                return (getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_40 -> {
                return (getMale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_1 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.ZERO) && getFemale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_1 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.ZERO) && getFemale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_2 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.ZERO) && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_2 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.ZERO) && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_3 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.ZERO) && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_3 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.ZERO) && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_4 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.ZERO) && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_4 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.ZERO) && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_10 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN) && getFemale().getLabourSupplyWeekly().equals(Labour.ZERO)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_10 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN) && getFemale().getLabourSupplyWeekly().equals(Labour.ZERO)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_11 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN) && getFemale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_11 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN) && getFemale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_12 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN) && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_12 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN) && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_13 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN) && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_13 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN) && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_14 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN) && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_14 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN) && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_20 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getFemale().getLabourSupplyWeekly().equals(Labour.ZERO)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_20 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getFemale().getLabourSupplyWeekly().equals(Labour.ZERO)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_21 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getFemale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_21 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getFemale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_22 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_22 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_23 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_23 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_24 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_24 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_30 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getFemale().getLabourSupplyWeekly().equals(Labour.ZERO)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_30 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getFemale().getLabourSupplyWeekly().equals(Labour.ZERO)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_31 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getFemale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_31 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getFemale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_32 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_32 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_33 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_33 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_34 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_34 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_40 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY) && getFemale().getLabourSupplyWeekly().equals(Labour.ZERO)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_40 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY) && getFemale().getLabourSupplyWeekly().equals(Labour.ZERO)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_41 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY) && getFemale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_41 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY) && getFemale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_42 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY) && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_42 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY) && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_43 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY) && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_43 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY) && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Male_44 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY) && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY)) ? getMale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case L1_lhw_Female_44 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY) && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY)) ? getFemale().getL1LabourSupplyHoursWeekly() : 0.;
            }
            case Liwwh_1 -> {
                if (Occupancy.Single_Female.equals(getOccupancy())) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getFemale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_1 for non-single woman");
                }
            }
            case Liwwh_10 -> {
                if (Occupancy.Single_Male.equals(getOccupancy())) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getMale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_10 for non-single man");
                }
            }
            case Liwwh_2 -> {
                if (Occupancy.Single_Female.equals(getOccupancy())) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getFemale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_2 for non-single woman");
                }
            }
            case Liwwh_20 -> {
                if (Occupancy.Single_Male.equals(getOccupancy())) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getMale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_20 for non-single man");
                }
            }
            case Liwwh_3 -> {
                if (Occupancy.Single_Female.equals(getOccupancy())) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getFemale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_3 for non-single woman");
                }
            }
            case Liwwh_30 -> {
                if (Occupancy.Single_Male.equals(getOccupancy())) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getMale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_30 for non-single man");
                }
            }
            case Liwwh_4 -> {
                if (Occupancy.Single_Female.equals(getOccupancy())) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY_EIGHT)) ? getFemale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_4 for non-single woman");
                }
            }
            case Liwwh_40 -> {
                if (Occupancy.Single_Male.equals(getOccupancy())) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.THIRTY_EIGHT)) ? getMale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_40 for non-single man");
                }
            }
            case Liwwh_5 -> {
                if (Occupancy.Single_Female.equals(getOccupancy())) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.FORTY_FIVE)) ? getFemale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_5 for non-single woman");
                }
            }
            case Liwwh_50 -> {
                if (Occupancy.Single_Male.equals(getOccupancy())) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.FORTY_FIVE)) ? getMale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_50 for non-single man");
                }
            }
            case Liwwh_6 -> {
                if (Occupancy.Single_Female.equals(getOccupancy())) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.FIFTY_FIVE)) ? getFemale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_6 for non-single woman");
                }
            }
            case Liwwh_60 -> {
                if (Occupancy.Single_Male.equals(getOccupancy())) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.FIFTY_FIVE)) ? getMale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_60 for non-single man");
                }
            }
            case Liwwh_Female_1 -> {
                if (Occupancy.Couple.equals(getOccupancy()) && !getMale().atRiskOfWork()) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getFemale().getLiwwh() : 0.;
                } else {
                    return 0.;
                }
            }
            case Liwwh_Female_2 -> {
                if (Occupancy.Couple.equals(getOccupancy()) && !getMale().atRiskOfWork()) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getFemale().getLiwwh() : 0.;
                } else {
                    return 0.;
                }
            }
            case Liwwh_Female_3 -> {
                if (Occupancy.Couple.equals(getOccupancy()) && !getMale().atRiskOfWork()) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getFemale().getLiwwh() : 0.;
                } else {
                    return 0.;
                }
            }
            case Liwwh_Female_4 -> {
                if (Occupancy.Couple.equals(getOccupancy()) && !getMale().atRiskOfWork()) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY_EIGHT)) ? getFemale().getLiwwh() : 0.;
                } else {
                    return 0.;
                }
            }
            case Liwwh_Female_5 -> {
                if (Occupancy.Couple.equals(getOccupancy()) && !getMale().atRiskOfWork()) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.FORTY_FIVE)) ? getFemale().getLiwwh() : 0.;
                } else {
                    return 0.;
                }
            }
            case Liwwh_Female_6 -> {
                if (Occupancy.Couple.equals(getOccupancy()) && !getMale().atRiskOfWork()) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.FIFTY_FIVE)) ? getFemale().getLiwwh() : 0.;
                } else {
                    return 0.;
                }
            }
            case Liwwh_Female_10, Liwwh_Female_20, Liwwh_Female_30, Liwwh_Female_40, Liwwh_Female_50, Liwwh_Female_60  -> {
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.ZERO)) ? getFemale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_Female_XY for non-couple");
                }
            }
            case Liwwh_Female_01, Liwwh_Female_11, Liwwh_Female_21, Liwwh_Female_31, Liwwh_Female_41, Liwwh_Female_51, Liwwh_Female_61  -> {
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getFemale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_Female_XY for non-couple");
                }
            }
            case Liwwh_Female_02, Liwwh_Female_12, Liwwh_Female_22, Liwwh_Female_32, Liwwh_Female_42, Liwwh_Female_52, Liwwh_Female_62  -> {
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getFemale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_Female_XY for non-couple");
                }
            }
            case Liwwh_Female_03, Liwwh_Female_13, Liwwh_Female_23, Liwwh_Female_33, Liwwh_Female_43, Liwwh_Female_53, Liwwh_Female_63  -> {
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getFemale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_Female_XY for non-couple");
                }
            }
            case Liwwh_Female_04, Liwwh_Female_14, Liwwh_Female_24, Liwwh_Female_34, Liwwh_Female_44, Liwwh_Female_54, Liwwh_Female_64  -> {
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY_EIGHT)) ? getFemale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_Female_XY for non-couple");
                }
            }
            case Liwwh_Female_05, Liwwh_Female_15, Liwwh_Female_25, Liwwh_Female_35, Liwwh_Female_45, Liwwh_Female_55, Liwwh_Female_65  -> {
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.FORTY_FIVE)) ? getFemale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_Female_XY for non-couple");
                }
            }
            case Liwwh_Female_06, Liwwh_Female_16, Liwwh_Female_26, Liwwh_Female_36, Liwwh_Female_46, Liwwh_Female_56, Liwwh_Female_66  -> {
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return (getFemale().getLabourSupplyWeekly().equals(Labour.FIFTY_FIVE)) ? getFemale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_Female_XY for non-couple");
                }
            }
            case Liwwh_Male_1 -> {
                if (Occupancy.Couple.equals(getOccupancy()) && !getFemale().atRiskOfWork()) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getMale().getLiwwh() : 0.;
                } else {
                    return 0.;
                }
            }
            case Liwwh_Male_2 -> {
                if (Occupancy.Couple.equals(getOccupancy()) && !getFemale().atRiskOfWork()) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getMale().getLiwwh() : 0.;
                } else {
                    return 0.;
                }
            }
            case Liwwh_Male_3 -> {
                if (Occupancy.Couple.equals(getOccupancy()) && !getFemale().atRiskOfWork()) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getMale().getLiwwh() : 0.;
                } else {
                    return 0.;
                }
            }
            case Liwwh_Male_4 -> {
                if (Occupancy.Couple.equals(getOccupancy()) && !getFemale().atRiskOfWork()) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.THIRTY_EIGHT)) ? getMale().getLiwwh() : 0.;
                } else {
                    return 0.;
                }
            }
            case Liwwh_Male_5 -> {
                if (Occupancy.Couple.equals(getOccupancy()) && !getFemale().atRiskOfWork()) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.FORTY_FIVE)) ? getMale().getLiwwh() : 0.;
                } else {
                    return 0.;
                }
            }
            case Liwwh_Male_6 -> {
                if (Occupancy.Couple.equals(getOccupancy()) && !getFemale().atRiskOfWork()) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.FIFTY_FIVE)) ? getMale().getLiwwh() : 0.;
                } else {
                    return 0.;
                }
            }
            case Liwwh_Male_01, Liwwh_Male_02, Liwwh_Male_03, Liwwh_Male_04, Liwwh_Male_05, Liwwh_Male_06  -> {
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.ZERO)) ? getMale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_Male_XY for non-couple");
                }
            }
            case Liwwh_Male_10, Liwwh_Male_11, Liwwh_Male_12, Liwwh_Male_13, Liwwh_Male_14, Liwwh_Male_15, Liwwh_Male_16  -> {
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.TEN)) ? getMale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_Male_XY for non-couple");
                }
            }
            case Liwwh_Male_20, Liwwh_Male_21, Liwwh_Male_22, Liwwh_Male_23, Liwwh_Male_24, Liwwh_Male_25, Liwwh_Male_26  -> {
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.TWENTY)) ? getMale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_Male_XY for non-couple");
                }
            }
            case Liwwh_Male_30, Liwwh_Male_31, Liwwh_Male_32, Liwwh_Male_33, Liwwh_Male_34, Liwwh_Male_35, Liwwh_Male_36  -> {
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.THIRTY)) ? getMale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_Male_XY for non-couple");
                }
            }
            case Liwwh_Male_40, Liwwh_Male_41, Liwwh_Male_42, Liwwh_Male_43, Liwwh_Male_44, Liwwh_Male_45, Liwwh_Male_46  -> {
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.THIRTY_EIGHT)) ? getMale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_Male_XY for non-couple");
                }
            }
            case Liwwh_Male_50, Liwwh_Male_51, Liwwh_Male_52, Liwwh_Male_53, Liwwh_Male_54, Liwwh_Male_55, Liwwh_Male_56  -> {
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.FORTY_FIVE)) ? getMale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_Male_XY for non-couple");
                }
            }
            case Liwwh_Male_60, Liwwh_Male_61, Liwwh_Male_62, Liwwh_Male_63, Liwwh_Male_64, Liwwh_Male_65, Liwwh_Male_66  -> {
                if (Occupancy.Couple.equals(getOccupancy())) {
                    return (getMale().getLabourSupplyWeekly().equals(Labour.FIFTY_FIVE)) ? getMale().getLiwwh() : 0.;
                } else {
                    throw new RuntimeException("request for parameter Liwwh_Male_XY for non-couple");
                }
            }
            case MaleEduM_10 -> {
                return (getMale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN) && getMale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case MaleEduH_10 -> {
                return (getMale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN) && getMale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case MaleEduM_20 -> {
                return (getMale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getMale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case MaleEduH_20 -> {
                return (getMale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getMale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case MaleEduM_30 -> {
                return (getMale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getMale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case MaleEduH_30 -> {
                return (getMale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getMale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case MaleEduM_40 -> {
                return (getMale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY) && getMale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case MaleEduH_40 -> {
                return (getMale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY) && getMale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case MaleEduM_1 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.TEN) && getMale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case MaleEduH_1 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.TEN) && getMale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case MaleEduM_2 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getMale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case MaleEduH_2 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getMale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case MaleEduM_3 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getMale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case MaleEduH_3 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getMale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case MaleEduM_4 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY) && getMale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case MaleEduH_4 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY) && getMale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case FemaleEduM_10 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN) && getFemale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case FemaleEduH_10 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TEN) && getFemale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case FemaleEduM_20 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getFemale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case FemaleEduH_20 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getFemale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case FemaleEduM_30 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getFemale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case FemaleEduH_30 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getFemale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case FemaleEduM_40 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY) && getFemale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case FemaleEduH_40 -> {
                return (getMale() != null && getFemale() != null && getMale().getLabourSupplyWeekly().equals(Labour.FORTY) && getFemale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case FemaleEduM_1 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.TEN) && getFemale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case FemaleEduH_1 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.TEN) && getFemale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case FemaleEduM_2 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getFemale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case FemaleEduH_2 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.TWENTY) && getFemale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case FemaleEduM_3 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getFemale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case FemaleEduH_3 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.THIRTY) && getFemale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case FemaleEduM_4 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY) && getFemale().getDeh_c3().equals(Education.Medium)) ? 1. : 0.;
            }
            case FemaleEduH_4 -> {
                return (getMale() != null  && getFemale() != null && getFemale().getLabourSupplyWeekly().equals(Labour.FORTY) && getFemale().getDeh_c3().equals(Education.High)) ? 1. : 0.;
            }
            case MaleLeisure_dnc02 -> {
                return (Parameters.HOURS_IN_WEEK - getMale().getLabourSupplyHoursWeekly()) * getIndicatorChildren(0,1).ordinal();
            }
            case FemaleLeisure_dnc02 -> {
                return (Parameters.HOURS_IN_WEEK - getFemale().getLabourSupplyHoursWeekly()) * getIndicatorChildren(0,1).ordinal();

            }
            case Homeownership_D -> {
                return isHousingOwned()? 1. : 0.;
            }
            case Constant -> {
                return 1.0;
            }
            case Year_transformed -> {
                return (Parameters.isFixTimeTrend && getYear() >= Parameters.timeTrendStopsIn) ? (double) Parameters.timeTrendStopsIn - 2000 : (double) getYear() - 2000;
            }
            case couple_emp_2ft -> {
                return (getCoupleBoolean() && (getMinWeeklyHoursWorked() >= Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)) ? 1.0 : 0.0;
            }
            case couple_emp_ftpt -> {
                return (getCoupleBoolean() &&
                        (getMinWeeklyHoursWorked() > 0) && (getMinWeeklyHoursWorked() < Parameters.MIN_HOURS_FULL_TIME_EMPLOYED) &&
                        (getMaxWeeklyHoursWorked() >= Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)) ? 1.0 : 0.0;
            }
            case couple_emp_2pt -> {
                return (getCoupleBoolean() &&
                        (getMinWeeklyHoursWorked() > 0) &&
                        (getMaxWeeklyHoursWorked() < Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)) ? 1.0 : 0.0;
            }
            case couple_emp_ftne -> {
                return (getCoupleBoolean() &&
                        (getMinWeeklyHoursWorked() == 0) &&
                        (getMaxWeeklyHoursWorked() >= Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)) ? 1.0 : 0.0;
            }
            case couple_emp_ptne -> {
                return (getCoupleBoolean() &&
                        (getMinWeeklyHoursWorked() == 0) &&
                        (getMaxWeeklyHoursWorked() > 0) &&
                        (getMaxWeeklyHoursWorked() < Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)) ? 1.0 : 0.0;
            }
            case couple_emp_2ne -> {
                return (getCoupleBoolean() && (getMaxWeeklyHoursWorked() == 0)) ? 1.0 : 0.0;
            }
            case single_emp_ft -> {
                return (!getCoupleBoolean() && (getMinWeeklyHoursWorked() >= Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)) ? 1.0 : 0.0;
            }
            case single_emp_pt -> {
                return (!getCoupleBoolean() &&
                        (getMinWeeklyHoursWorked() > 0) &&
                        (getMaxWeeklyHoursWorked() < Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)) ? 1.0 : 0.0;
            }
            case single_emp_ne -> {
                return (!getCoupleBoolean() && (getMaxWeeklyHoursWorked() == 0)) ? 1.0 : 0.0;
            }
            case Graduate -> {
                return (Education.High.equals(getHighestDehC3())) ? 1.0 : 0.0;
            }
            case UKC -> {
                return Region.UKC.equals(region) ? 1.0 : 0.0;
            }
            case UKD -> {
                return Region.UKD.equals(region) ? 1.0 : 0.0;
            }
            case UKE -> {
                return Region.UKE.equals(region) ? 1.0 : 0.0;
            }
            case UKF -> {
                return Region.UKF.equals(region) ? 1.0 : 0.0;
            }
            case UKG -> {
                return Region.UKG.equals(region) ? 1.0 : 0.0;
            }
            case UKH -> {
                return Region.UKH.equals(region) ? 1.0 : 0.0;
            }
            case UKI -> {
                return Region.UKI.equals(region) ? 1.0 : 0.0;
            }
            case UKJ -> {
                return Region.UKJ.equals(region) ? 1.0 : 0.0;
            }
            case UKK -> {
                return Region.UKK.equals(region) ? 1.0 : 0.0;
            }
            case UKL -> {
                return Region.UKL.equals(region) ? 1.0 : 0.0;
            }
            case UKM -> {
                return Region.UKM.equals(region) ? 1.0 : 0.0;
            }
            case UKN -> {
                return Region.UKN.equals(region) ? 1.0 : 0.0;
            }
            case n_children_0 -> {
                return getNumberChildren(0);
            }
            case n_children_1 -> {
                return getNumberChildren(1);
            }
            case n_children_2 -> {
                return getNumberChildren(2);
            }
            case n_children_3 -> {
                return getNumberChildren(3);
            }
            case n_children_4 -> {
                return getNumberChildren(4);
            }
            case n_children_5 -> {
                return getNumberChildren(5);
            }
            case n_children_6 -> {
                return getNumberChildren(6);
            }
            case n_children_7 -> {
                return getNumberChildren(7);
            }
            case n_children_8 -> {
                return getNumberChildren(8);
            }
            case n_children_9 -> {
                return getNumberChildren(9);
            }
            case n_children_10 -> {
                return getNumberChildren(10);
            }
            case n_children_11 -> {
                return getNumberChildren(11);
            }
            case n_children_12 -> {
                return getNumberChildren(12);
            }
            case n_children_13 -> {
                return getNumberChildren(13);
            }
            case n_children_14 -> {
                return getNumberChildren(14);
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
                throw new IllegalArgumentException("Unsupported regressor " + variableID.name() + " in BenefitUnit");
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
    public boolean getAtRiskOfWork() {

        boolean atRiskOfWork = false;
        Person male = getMale();
        Person female = getFemale();
        if (female != null) {
            atRiskOfWork = female.atRiskOfWork();
        }
        if (atRiskOfWork == false && male != null) {        //Can skip checking if atRiskOfWork is true already
            atRiskOfWork = male.atRiskOfWork();
        }
        return atRiskOfWork;
    }

    public boolean isEmployed() {
        boolean isEmployed = false;
        Person male = getMale();
        Person female = getFemale();
        if(female != null) {
            isEmployed = Les_c4.EmployedOrSelfEmployed.equals(female.getLes_c4());
        }
        if(!isEmployed  && male != null) {        //Can skip checking if atRiskOfWork is true already
            isEmployed = Les_c4.EmployedOrSelfEmployed.equals(male.getLes_c4());
        }
        return isEmployed;
    }

    protected void homeownership() {

        ValidHomeownersCSfilter filter = new ValidHomeownersCSfilter();
        Person male = getMale();
        Person female = getFemale();
        if (filter.isFiltered(this)) {

            boolean male_homeowner = false, female_homeowner = false;
            if (male!=null) {

                double prob = Parameters.getRegHomeownershipHO1a().getProbability(male, Person.DoublesVariables.class);
                if (statInnovations.getDoubleDraw(6) < prob) {
                    male_homeowner = true;
                }
            }
            if (female!=null) {

                double prob = Parameters.getRegHomeownershipHO1a().getProbability(female, Person.DoublesVariables.class);
                if (statInnovations.getDoubleDraw(7) < prob) {
                    female_homeowner = true;
                }
            }
            if (male_homeowner || female_homeowner) { //If neither person in the BU is a homeowner, BU not classified as owning home
                setWealthPrptyFlag(true);
            } else {
                setWealthPrptyFlag(false);
            }
        }
    }

    public double calculateEquivalisedDisposableIncomeYearly() {

        if(getDisposableIncomeMonthly() != null && Double.isFinite(getDisposableIncomeMonthly())) {
            yDispEquivYear = (getDisposableIncomeMonthly() / getEquivalisedWeight()) * 12;
        } else {
            yDispEquivYear = 0.;
        }
        return yDispEquivYear;
    }


    private double calculateYearlyChangeInLogEquivalisedDisposableIncome() {
        double yearlyChangeInLogEquivalisedDisposableIncome = 0.;
        if (yDispEquivYear != null && yDispEquivYearL1 != null && yDispEquivYear >= 0. && yDispEquivYearL1 >= 0.) {
            // Note that income is uprated to the base price year, as specified in parameters class, as the estimated change uses real income change
            // +1 added as log(0) is not defined
            yearlyChangeInLogEquivalisedDisposableIncome =
                    Math.log(yDispEquivYear / Parameters.getTimeSeriesValue(model.getYear(), TimeSeriesVariable.Inflation) + 1)
                            - Math.log(yDispEquivYearL1 / Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.Inflation) + 1);
        }
        yDiffDispEquivPrevYear = yearlyChangeInLogEquivalisedDisposableIncome;
        if (!Parameters.checkFinite(yDiffDispEquivPrevYear))
            throw new RuntimeException("problem evaluating yearly change in log edi");
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

    public void initialiseLiquidWealth(int age, double donorLiquidWealth, double donorPensionWealth, double donorHousingWealth) {
        double wealth = (1.0 - Parameters.getLiquidWealthDiscount()) * donorLiquidWealth;
        if (!Parameters.projectPensionWealth)
            wealth += (1.0 - Parameters.getPensionWealthDiscount(age)) * donorPensionWealth;
        if (!Parameters.projectHousingWealth)
            wealth += (1.0 - Parameters.getHousingWealthDiscount(age)) * donorHousingWealth;
        setWealthTotValue(wealth);
    }

    public double getWealthTotValue() {
        return getLiquidWealth(true);
    }

    public double getLiquidWealth(boolean throwError) {
        if (!Parameters.checkFinite(wealthTotValue)) {
            if (throwError)
                throw new RuntimeException("Call to get benefit unit liquid wealth before it is initialised.");
            else
                return 0.0;
        }
        return wealthTotValue;
    }

    public void setWealthTotValue(Double liquidWealth) {
        this.wealthTotValue = liquidWealth;
    }

    public double getPensionWealth() {
        return getPensionWealth(true);
    }

    public double getPensionWealth(boolean throwError) {
        if (!Parameters.checkFinite(wealthPensValue)) {
            if (throwError)
                throw new RuntimeException("Call to get benefit unit pension wealth before it is initialised.");
            else
                return 0.0;
        }
        return wealthPensValue;
    }

    public void setPensionWealth(Double wealthPensValue) {
        this.wealthPensValue = wealthPensValue;
    }

    public double getHousingWealth() {
        return getHousingWealth(true);
    }

    public double getHousingWealth(boolean throwError) {
        if (!Parameters.checkFinite(wealthPrptyValue)) {
            if (throwError)
                throw new RuntimeException("Call to get benefit unit housing wealth before it is initialised.");
            else
                return 0.0;
        }
        return wealthPrptyValue;
    }

    public void setHousingWealth(Double wealth) {
        wealthPrptyValue = wealth;
    }

    public double getChildcareCostPerWeek() {
        return getChildcareCostPerWeek(true);
    }
    public double getChildcareCostPerWeek(boolean throwError) {
        if (!Parameters.checkFinite(xChildCareWeek)) {
            if (throwError) {
                throw new RuntimeException("Call to get benefit unit childcare cost before it is initialised.");
            } else {
                return 0.0;
            }
        }
        return xChildCareWeek;
    }

    public double getSocialCareCostPerWeek() {
        return getSocialCareCostPerWeek(true);
    }
    public double getSocialCareCostPerWeek(boolean throwError) {
        if (!Parameters.checkFinite(xCareWeek)) {
            if (throwError) {
                throw new RuntimeException("Call to get benefit unit social care cost before it is initialised.");
            } else {
                return 0.0;
            }
        }
        return xCareWeek;
    }

    public Household getHousehold() {
        return household;
    }

    public void setHousehold(Household newHousehold) {

        if (household!=null && !household.equals(newHousehold))
            household.getBenefitUnits().remove(this);

        household = newHousehold;
        idHh = household.getId();
        if (household!=null)
            household.getBenefitUnits().add(this);
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public double getWeight() {
        double cumulativeWeight = 0.0;
        double size = 0.0;
        for( Person person : getMembers()) {
            cumulativeWeight += person.getWeight();
            size++;
        }
        return cumulativeWeight / size;
    }

    public Set<Person> getChildren() {
        Set<Person> children = new LinkedHashSet<>();
        for (Person member : members) {
            if (member.getDemAge() < Parameters.AGE_TO_BECOME_RESPONSIBLE)
                children.add(member);
        }
        return children;
    }

    public Long getIdFemale() {
        Person female = getFemale();
        if (female != null)
            return female.getKey().getId();
        else
            return null;
    }

    public int getNumberChildrenAll() {
        return getNumberChildren(0,Parameters.AGE_TO_BECOME_RESPONSIBLE);
    }
    public int getNumberChildren(int age) {
        return getNumberChildren(age, age);
    }
    public int getNumberChildren(int minAge, int maxAge) {
        int nChildren = 0;
        if (model==null) {
            for (int aa=minAge; aa<=maxAge; aa++) {
                nChildren += getNumberChildrenByAge(aa);
            }
        } else {
            for (Person member : members) {
                if ( (member.getDemAge()>=minAge) && (member.getDemAge()<=maxAge) )
                    nChildren++;
            }
        }
        return nChildren;
    }
    public Indicator getIndicatorChildren(int minAge, int maxAge) {
        Indicator flag = Indicator.False;
        if (model==null) {
            for (int aa=minAge; aa<=maxAge; aa++) {
                if (getNumberChildrenByAge(aa) > 0) {
                    flag = Indicator.True;
                    break;
                }
            }
        }
        return flag;
    }
    public Integer getNumberChildrenAll_lag1() {
        return numberChildrenAll_lag1;
    }
    public Integer getNumberChildren02_lag1() { return numberChildren02_lag1; }
    public Indicator getIndicatorChildren03_lag1() { return i_demNChild0to2L1; }
    public Indicator getIndicatorChildren412_lag1() {
        return dem4to12L1;
    }

    public double getEquivalisedDisposableIncomeYearly() {
        double val;
        if (yDispEquivYear != null) {
            val = yDispEquivYear;
        } else {
            val = -9999.99;
        }
        return val;
    }

    public int getAtRiskOfPoverty() {
        if (yPvrtyFlag != null) {
            return yPvrtyFlag;
        } else return 0;
    }

    public Integer getAtRiskOfPoverty_lag1() {
        return yPvrtyFlagL1;
    }

    public void setAtRiskOfPoverty(Integer yPvrtyFlag) {
        this.yPvrtyFlag = yPvrtyFlag;
    }

    public Double getDisposableIncomeMonthly() {
        return yDispMonth;
    }

    public Double getGrossIncomeMonthly() {
        return yGrossMonth;
    }

    public Double getBenefitsReceivedPerMonth() {
        return yBenAmountMonth;
    }

    public Integer getReceivedUC() {
        return (yBenUCReceivedFlag == null ? 0 : yBenUCReceivedFlag);
    }

    public void setReceivedUC(Integer yBenUCReceivedFlag) {
        this.yBenUCReceivedFlag = yBenUCReceivedFlag;
    }

    public Integer getReceivedLegacyBenefits() {
        return (yBenLegacyReceivedFlag == null ? 0 : yBenLegacyReceivedFlag);
    }

    public void setReceivedLegacyBenefits(Integer yBenLegacyReceivedFlag) {
        this.yBenLegacyReceivedFlag = yBenLegacyReceivedFlag;
    }


    public void setOccupancyLocal(Occupancy occupancy) {
        i_demOccupancy = occupancy;
    }

    public Occupancy getOccupancy() {
        if (model==null) {
            if (i_demOccupancy ==null)
                throw new RuntimeException("occupancyLocal not initialised");
            return i_demOccupancy;
        }
        if (getMale()!=null) {
            if (getFemale()!=null)
                return Occupancy.Couple;
            else
                return Occupancy.Single_Male;
        }
        if (getFemale()!=null)
            return Occupancy.Single_Female;
        else
            throw new RuntimeException("Benefit unit does not include at least one responsible adult");
    }

    public int getCoupleDummy() {
        return (getMale()!=null && getFemale()!=null) ? 1 : 0;
    }

    public boolean getCoupleBoolean() {
        return (getMale()!=null && getFemale()!=null);
    }

    public long getId() {
        return key.getId();
    }

    public Ydses_c5 getYdses_c5() {
        return yHhQuintilesMonthC5;
    }

    public Ydses_c5 getYdses_c5_lag1() {
        return yHhQuintilesC5L1;
    }

    public double getTmpHHYpnbihs_dv_asinh() {
        if (i_yNonBenHhGrossAsinh ==null)
            throw new RuntimeException("tmpHHYpnbihs_dv_asinh accessed before initialised");
        return i_yNonBenHhGrossAsinh;
    }

    public void setTmpHHYpnbihs_dv_asinh(double val) {
        i_yNonBenHhGrossAsinh = val;
    }

    public Dhhtp_c4 getDhhtp_c4() {
        if (getMale()!=null && getFemale()!=null) {
            if (getChildren().size()>0)
                return Dhhtp_c4.CoupleChildren;
            else
                return Dhhtp_c4.CoupleNoChildren;
        } else {
            if (getChildren().size()>0)
                return Dhhtp_c4.SingleChildren;
            else
                return Dhhtp_c4.SingleNoChildren;
        }
    }


    public Dhhtp_c4 getDemCompHhC4L1() {
        return demCompHhC4L1;
    }

    public double getYearlyChangeInLogEDI() {
        if (yDiffDispEquivPrevYear ==null)
            throw new RuntimeException("yearlyChangeInLogEDI requested before set");
        return yDiffDispEquivPrevYear;
    }

    public boolean isHousingOwned() {
        if (wealthPrptyFlag ==null) {
            wealthPrptyFlag = false;
        }
        return wealthPrptyFlag;
    }

    public void setWealthPrptyFlag(boolean wealthPrptyFlag) {
        this.wealthPrptyFlag = wealthPrptyFlag;
    }

    public ArrayList<Triple<Les_c7_covid, Double, Integer>> getCovid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale() {
        return covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale;
    }

    public ArrayList<Triple<Les_c7_covid, Double, Integer>> getCovid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale() {
        return covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale;
    }

    // Uprate disposable income from level of prices in any given year to 2017, as utility function was estimated on 2017 data
    public double getDisposableIncomeMonthlyUpratedToBasePriceYear() {
        return yDispMonth / Parameters.getTimeSeriesValue(model.getYear(), TimeSeriesVariable.Inflation);
    }

    public double getAdjustedDisposableIncomeMonthlyUpratedToBasePriceYear() {
        return yDispMonth / Parameters.getTimeSeriesValue(model.getYear(), TimeSeriesVariable.Inflation);
    }

    public double getNonDiscretionaryExpenditureMonthlyUpratedToBasePriceYear() {
        double cost = 0.0;
        if (Parameters.flagFormalChildcare)
            cost += xChildCareWeek;
        if (Parameters.flagSocialCare)
            cost += xCareWeek;
        if (Math.abs(cost) > 0.01)
            cost *= Parameters.WEEKS_PER_MONTH / Parameters.getTimeSeriesValue(model.getYear(), TimeSeriesVariable.Inflation);
        return cost;
    }

    public boolean isDecreaseInYearlyEquivalisedDisposableIncome() {
        return (yDispEquivYear != null && yDispEquivYearL1 != null && yDispEquivYear < yDispEquivYearL1);
    }

    public void clearStates() {
        if (labStatesContObject !=null) labStatesContObject = null;
    }

    void setStates() {

        // reset states if necessary
        if (labStatesContObject !=null) clearStates();

        // populate states object
        if ( Parameters.grids == null) {
            throw new IllegalStateException("ERROR - attempt to fetch uninitialised grids attribute from Parameters object");
        }
        labStatesContObject = new States(this, Parameters.grids.getScale());
    }

    public Person getRefPersonForDecisions() {

        Person ref;
        Person male = getMale();
        Person female = getFemale();
        if (male!=null && female!=null) {
            // reference person defined as:
            //	man if man is disabled (even if woman is also disabled)
            //	woman if woman is disabled and man is not
            //  man if man is in need of social care
            //  woman if woman is in need of social care and man is not
            //	person with highest age if both retired
            //	person retired if one is retired
            //	student if one is student (and under maximum age threshold)
            //	person with highest full-time wage potential if neither is disabled or receiving social care and both not retired or students

            if (male==null) {
                throw new IllegalStateException("ERROR - benefit unit identified as couple, but missing male adult");
            } else if (female==null) {
                throw new IllegalStateException("ERROR - benefit unit identified as couple, but missing female adult");
            } else if (male.getDlltsd() == Indicator.True) {
                ref = male;
            } else if (female.getDlltsd() == Indicator.True) {
                ref = female;
            } else if (Indicator.True.equals(male.getNeedSocialCare())) {
                ref = male;
            } else if (Indicator.True.equals(female.getNeedSocialCare())) {
                ref = female;
            } else if (male.getLes_c4()==Les_c4.Retired && female.getLes_c4()==Les_c4.Retired) {
                if (male.getDemAge() >= female.getDemAge()) {
                    ref = male;
                } else {
                    ref = female;
                }
            } else if (male.getLes_c4()==Les_c4.Retired) {
                ref = male;
            } else if (female.getLes_c4()==Les_c4.Retired) {
                ref = female;
            } else if (male.getLes_c4()==Les_c4.Student && male.getDemAge()<=Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION && female.getLes_c4()!=Les_c4.Student) {
                ref = male;
            } else if (female.getLes_c4()==Les_c4.Student && female.getDemAge()<=Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION && male.getLes_c4()!=Les_c4.Student) {
                ref = female;
            } else {
                if (male.getLabWageFullTimeHrly() >= female.getLabWageFullTimeHrly()) {
                    ref = male;
                } else {
                    ref = female;
                }
            }
        } else {
            // reference person is assigned to sole adult

            if (getMale() != null) {
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

        int health = 999;
        Person male = getMale();
        Person female = getFemale();
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
        return (double)health;
    }

    public double getRegionIndex() {

        return region.getValue();
    }

    public int getNumberChildrenByAge(int age) {
        int children = 0;
        for (Person member : members) {
            if (member.getDemAge()==age)
                children++;
        }
        return children;
    }

    public void updateNonLabourIncome() {

        if (Parameters.projectLiquidWealth) {

            updateRetirementPensions();
            setInvestmentIncomeAnnual();
        } else {

            Person male = getMale();
            Person female = getFemale();
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
            Occupancy occupancy = getOccupancy();
            if (toRetire && getWealthTotValue() > 0.0) {
                yPensYear = wealthTotValue * Parameters.SHARE_OF_WEALTH_TO_ANNUITISE_AT_RETIREMENT /
                        Parameters.annuityRates.getAnnuityRate(occupancy, getYear()-refPerson.getDemAge(), refPerson.getDemAge());
                wealthTotValue *= (1.0 - Parameters.SHARE_OF_WEALTH_TO_ANNUITISE_AT_RETIREMENT);

                // upate person variables
                double val;
                if (Occupancy.Couple.equals(occupancy)) {
                    val = asinh(yPensYear /12.0/2.0);
                    getMale().setyPensPersGrossMonth(val);
                    getFemale().setyPensPersGrossMonth(val);
                } else if (Occupancy.Single_Male.equals(occupancy)) {
                    val = asinh(yPensYear /12.0);
                    getMale().setyPensPersGrossMonth(val);
                } else {
                    val = asinh(yPensYear /12.0);
                    getFemale().setyPensPersGrossMonth(val);
                }
            } else {
                if (Occupancy.Couple.equals(occupancy)) {
                    yPensYear = getMale().getPensionIncomeAnnual();
                    yPensYear += getFemale().getPensionIncomeAnnual();
                } else {
                    yPensYear = refPerson.getPensionIncomeAnnual();
                }
            }
        } else {
            throw new RuntimeException("Unrecognised call to update retirement pensions");
        }
    }

    public void setInvestmentIncomeAnnual() {

        if ( Parameters.enableIntertemporalOptimisations ) {

            Person male = getMale();
            Person female = getFemale();
            if (getWealthTotValue() < 0) {

                double wageFactor = 0.0;
                if (male != null) wageFactor += 0.7 * male.getEarningsWeekly(DecisionParams.FULLTIME_HOURS_WEEKLY) * 52.0;
                if (female != null) wageFactor += 0.7 * female.getEarningsWeekly(DecisionParams.FULLTIME_HOURS_WEEKLY) * 52.0;
                double phi;
                if (wageFactor < 0.1) {
                    phi = 1.0;
                } else {
                    phi = -wealthTotValue / wageFactor;
                }
                phi = Math.min(phi, 1.0);
                yInvestYear = (Parameters.getTimeSeriesRate(model.getYear(), TimeVaryingRate.RealDebtCostLow)*(1.0-phi) +
                        Parameters.getTimeSeriesRate(model.getYear(), TimeVaryingRate.RealDebtCostHigh)*phi +
                        Parameters.realInterestRateInnov) * wealthTotValue;
            } else {
                yInvestYear = (Parameters.getTimeSeriesRate(model.getYear(), TimeVaryingRate.RealSavingReturns) +
                        Parameters.realInterestRateInnov) * wealthTotValue;
            }
            if ((yInvestYear < -20000000.0) || (yInvestYear > 200000000.0))
                throw new RuntimeException("odd projection for annual investment income: " + yInvestYear);

            // update person level variables
            double val;
            Occupancy occupancy = getOccupancy();
            if (Occupancy.Couple.equals(occupancy)) {
                val = asinh(yInvestYear /12.0/2.0);
                male.setyCapitalPersMonth(val);
                female.setyCapitalPersMonth(val);
                val = asinh((yInvestYear + yPensYear)/12.0/2.0);
                male.setYptciihs_dv(val);
                female.setYptciihs_dv(val);
            } else if (Occupancy.Single_Male.equals(occupancy)) {
                val = asinh(yInvestYear /12.0);
                male.setyCapitalPersMonth(val);
                val = asinh((yInvestYear + yPensYear)/12.0);
                male.setYptciihs_dv(val);
            } else {
                val = asinh(yInvestYear /12.0);
                female.setyCapitalPersMonth(val);
                val = asinh((yInvestYear + yPensYear)/12.0);
                female.setYptciihs_dv(val);
            }
        } else {
            throw new RuntimeException("Unrecognised call to update investment income");
        }
    }

    public double getInvestmentIncomeAnnual() {return (yInvestYear !=null) ? yInvestYear : 0.0;}
    public double getPensionIncomeAnnual() {return (yPensYear !=null) ? yPensYear : 0.0;}

    void updateDiscretionaryConsumption() {

        if ( Parameters.enableIntertemporalOptimisations ) {

            // project benefit unit consumption
            if (!Parameters.checkFinite(getDisposableIncomeMonthly())) {
                throw new RuntimeException("Disposable income not defined.");
            }

            double cashOnHand = Math.max(getWealthTotValue(), DecisionParams.getMinWealthByAge(getIntValue(Regressors.MaximumAge)))
                    + getDisposableIncomeMonthly()*12.0 + labStatesContObject.getAvailableCredit() - getNonDiscretionaryConsumptionPerYear();
            if (!Parameters.checkFinite(cashOnHand)) {
                throw new RuntimeException("Problem identifying cash on hand");
            }
            if (cashOnHand < 1.0E-5) {
                // allow for simulated debt exceeding assumed limit for behavioural solutions
                xDiscretionaryYear = DecisionParams.MIN_CONSUMPTION_PER_YEAR;
            } else {
                xDiscretionaryYear = Parameters.grids.consumption.interpolateAll(labStatesContObject, false);
                xDiscretionaryYear *= cashOnHand;
            }
            if ( !Parameters.checkFinite(xDiscretionaryYear) ) {
                throw new RuntimeException("annual discretionary consumption not defined (1)");
            }
        } else {
            throw new RuntimeException("Unrecognised call to update net liquid wealth");
        }
    }

    private double getNonDiscretionaryConsumptionPerYear() {
        double nonDiscretionaryConsumptionPerYear = 0.0;
        if (Parameters.flagFormalChildcare) {
            nonDiscretionaryConsumptionPerYear += getChildcareCostPerWeek() * Parameters.WEEKS_PER_YEAR;
        }
        if (Parameters.flagSocialCare) {
            nonDiscretionaryConsumptionPerYear += getSocialCareCostPerWeek() * Parameters.WEEKS_PER_YEAR;
        }
        return nonDiscretionaryConsumptionPerYear;
    }
    public double getDiscretionaryConsumptionPerYear() {
        return getDiscretionaryConsumptionPerYear(true);
    }
    public double getDiscretionaryConsumptionPerYear(boolean throwError) {
        if (!Parameters.checkFinite(xDiscretionaryYear)) {
            if (throwError) {
                throw new RuntimeException("annual consumption not defined (2)");
            } else {
                return 0.0;
            }
        } else {
            return xDiscretionaryYear;
        }
    }

    public double getEquivalisedWeight() {
        //Equivalence scale gives a weight of 1.0 to the first adult;
        //0.5 to the second and each subsequent person aged 14 and over;
        //0.3 to each child aged under 14.
        double weight = 0.0;
        boolean firstAdult = true;
        for (Person member : members) {
            if (member.getDemAge() >= Parameters.AGE_TO_BECOME_RESPONSIBLE) {
                if (firstAdult)
                    weight += 1.0;
                else
                    weight += 0.5;
            } else {
                if (member.getDemAge()<14)
                    weight += 0.3;
                else
                    weight += 0.5;
            }
        }
        return weight;
    }

    public double asinh(double xx) {
        return Math.log(xx + Math.sqrt(xx * xx + 1.0));
    }

    private void updateChildcareCostPerWeek(int year, int age) {

        xChildCareWeek = 0.0;
        if (hasChildrenEligibleForCare() && (age < Parameters.getStatePensionAge(year, age))) {

            double prob = Parameters.getRegChildcareC1a().getProbability(this, Regressors.class);
            if (statInnovations.getDoubleDraw(0) < prob) {

                double score = Parameters.getRegChildcareC1b().getScore(this, Regressors.class);
                double rmse = Parameters.getRMSEForRegression("C1b");
                double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(statInnovations.getDoubleDraw(1));
                xChildCareWeek = Math.exp(score + rmse * gauss);
                double costCap = childCareCostCapWeekly();
                if (costCap > 0.0 && costCap < getChildcareCostPerWeek()) {
                    xChildCareWeek = costCap;
                }
            }
        }
    }

    private void updateSocialCareCostPerWeek() {

        xCareWeek = 0.0;
        for (Person person : getMembers()) {
            xCareWeek += person.getSocialCareCostWeekly();
        }
    }

    private void updateSocialCareProvision() {

        careProvidedFlag = 0;
        for (Person person : getMembers()) {
            if (!SocialCareProvision.None.equals(person.getSocialCareProvision()))
                careProvidedFlag = 1;
        }
    }

    public void setDeh_c3Local(Education edu) {
        i_eduHighestC3 = edu;
    }

    private Education getHighestDehC3() {

        Education max = Education.Low;
        if (model==null) {

            if (i_eduHighestC3 == null)
                throw new RuntimeException("reference to uninitialised education status");
            max = i_eduHighestC3;
        } else {

            Person male = getMale();
            Person female = getFemale();
            if(male != null || female != null) {

                if (male != null) max = male.getDeh_c3();
                if (female != null) {

                    if (Education.High.equals(female.getDeh_c3())) {
                        max = Education.High;
                    } else if (Education.Medium.equals(female.getDeh_c3()) && !(max == Education.High)) {
                        max = Education.Medium;
                    }
                }
            }
        }

        return max;
    }

    public void setLabourHoursWeekly1Local(Integer hours) {
        i_labHrsWork1Week = hours;
    }

    public void setLabourHoursWeekly2Local(Integer hours) {
        i_labHrsWork2Week = hours;
    }

    private Integer getMinWeeklyHoursWorked() {

        Integer val = null;
        if (model==null) {

            if (i_labHrsWork1Week == null) {
                throw new RuntimeException("reference to uninitialised labourHoursWeekly attribute of benefitUnit");
            } else {
                val = i_labHrsWork1Week;
            }
            if (i_labHrsWork2Week != null)
                if (i_labHrsWork2Week < val)
                    val = i_labHrsWork2Week;
        } else {

            Person male = getMale();
            Person female = getFemale();
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
            } else
                throw new RuntimeException("problem identifying responsible adults");
        }

        return val;
    }

    private int getMaxWeeklyHoursWorked() {

        Integer val = null;
        Person male = getMale();
        Person female = getFemale();
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

            if (i_labHrsWork1Week == null) {
                throw new RuntimeException("reference to uninitialised labourHoursWeekly attribute of benefitUnit");
            } else {
                val = i_labHrsWork1Week;
            }
            if (i_labHrsWork2Week != null) {
                if (i_labHrsWork2Week > val) val = i_labHrsWork2Week;
            }
        }
        return val;
    }

    private boolean hasChildrenEligibleForCare() {
        for (Person member: members) {
            if (member.getDemAge() <= Parameters.MAX_CHILD_AGE_FOR_FORMAL_CARE)
                return true;
        }
        return false;
    }

    private double childCareCostCapWeekly() {
        double cap = -1.0;
        Person male = getMale();
        Person female = getFemale();
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
        i_demYear = year;
    }

    public int getYear() {
        if (model == null) {
            if (i_demYear == null)
                throw new RuntimeException("call to get uninitialised year in benefit unit");
            return i_demYear;
        }
        return model.getYear();
    }

    public long getTaxDbDonorId() {
        return idtaxDbDonor;
    }
    public Match getTaxDbMatch() {
        return demDbMatchTax;
    }
    public Set<Person> getMembers() {return members;}

    public void setProcessedId(long id) {
        key.setWorkingId(id);
    }

    public long getSeed(){return (statSeed !=null) ? statSeed : 0L;}

    public long getIdOriginalBU() {return idBuOriginal;}

    public long getIdOriginalHH() {return idHhOriginal;}

    public Person getFemale() {
        for (Person member : members) {
            if (member.getDemAge()>=Parameters.AGE_TO_BECOME_RESPONSIBLE && Gender.Female.equals(member.getDemMaleFlag()))
                return member;
        }
        return null;
    }

    public Person getMale() {
        for (Person member : members) {
            if (member.getDemAge()>=Parameters.AGE_TO_BECOME_RESPONSIBLE && Gender.Male.equals(member.getDemMaleFlag()))
                return member;
        }
        return null;
    }

    public void removeMember(Person member) {
        members.remove(member);
        if (getMale()==null && getFemale()==null) {
            if (!members.isEmpty()) {
                for (Person orphan : members) {
                    System.out.println("WARNING: Removed orphan aged " + orphan.getDemAge() + " in benefit unit without reference adult");
                    model.getPersons().remove(orphan);
                }
            }
            household.removeBenefitUnit(this);
            model.getBenefitUnits().remove(this);
        }
    }

    public static void setBenefitUnitIdCounter(long id) {benefitUnitIdCounter = id;}

    /**
     * Calculates the labour innovation value for the current period with persistence.
     *
     * This method implements a persistence mechanism for labour innovations where there's
     * a probability that the previous period's innovation will persist into the current period.
     * A new innovation is drawn each period, but it's only used with probability (1 - persistenceProbability).
     *
     * The persistence mechanism works as follows:
     * 1. Draw a new potential innovation value
     * 2. Draw a random number to determine if we should use the new value or persist with the old one
     * 3. If it's the first period or there was a gap in years, use the new innovation
     * 4. Otherwise, use the persistence probability to decide between new and old values
     *
     * @param persistenceProbability The probability (between 0 and 1) that the previous period's
     *                              innovation will persist. A value of 0 means always use new innovations,
     *                              while 1 means always keep the old innovation.
     * @return The labour innovation value for this period
     */
    private double getLabourInnovation(double persistenceProbability) {

        double newLabourInnovation = statInnovations.getDoubleDraw(5);

        // persistenceRandomDraw - random value to determine if we keep old innovation
        double persistenceRandomDraw = statInnovations.getDoubleDraw(6);

        int currentYear = model.getYear();

        // Initialize on first call
        if (labPersistValueLabourInnov == null || lastYear == null) {
            labPersistValueLabourInnov = newLabourInnovation;
            lastYear = currentYear;
            return newLabourInnovation;
        }

        double labourInnov;
        if (persistenceRandomDraw < persistenceProbability) {
            labourInnov = labPersistValueLabourInnov;
        } else {
            labourInnov = newLabourInnovation;
        }

        // Store values for next period
        labPersistValueLabourInnov = labourInnov;
        lastYear = currentYear;

        return labourInnov;
    }

    private Person getSingle() {
        if (getMale() != null & getFemale() == null) {
            return null;
        } else if (getMale() != null) {
            return getMale();
        } else {
            return getFemale();
        }
    }
}
