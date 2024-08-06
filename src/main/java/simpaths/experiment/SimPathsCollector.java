// define package
package simpaths.experiment;

// import Java packages
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import simpaths.data.filters.FlexibleInLabourSupplyFilter;
import simpaths.model.BenefitUnit;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.Quintiles;
import microsim.statistics.Series;
import microsim.statistics.functions.*;
// import plug-in packages
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;

// import JAS-mine packages
import microsim.annotation.GUIparameter;
import microsim.data.DataExport;
import microsim.engine.AbstractSimulationCollectorManager;
import microsim.engine.SimulationEngine;
import microsim.engine.SimulationManager;
import microsim.event.EventListener;
import microsim.event.SingleTargetEvent;
import microsim.statistics.CrossSection;
import microsim.statistics.IDoubleSource;
// import LABOURsim packages
import simpaths.data.Parameters;
import simpaths.data.statistics.Statistics;
import simpaths.data.statistics.Statistics2;
import simpaths.data.statistics.Statistics3;
import simpaths.model.Person;
import simpaths.model.enums.Region;

import static simpaths.model.Person.DoublesVariables.GrossLabourIncomeMonthly;


/**
 *
 * CLASS TO MANAGE COLLECTION OF SIMULATED OUTPUT
 *
 */
public class SimPathsCollector extends AbstractSimulationCollectorManager implements EventListener {

    // default simulation parameters
    private static Logger log = Logger.getLogger(SimPathsCollector.class);

    @GUIparameter(description="Calculate the Gini coefficients of income (also displayed in charts)")
    private boolean calculateGiniCoefficients = false;

    @GUIparameter(description="Calculate extended set of population characteristics (useful for validation)")
    private boolean persistStatistics2 = true;

    @GUIparameter(description="Report alignment adjustments")
    private boolean persistStatistics3 = true;

    @GUIparameter(description="Toggle to turn database persistence on/off")
    private boolean exportToDatabase = false;

    @GUIparameter(description="Toggle to turn export to .csv files on/off")
    private boolean exportToCSV = true;

    @GUIparameter(description="Toggle to turn persistence of statistics on/off")
    private boolean persistStatistics = true;

    @GUIparameter(description="Toggle to turn persistence of persons on/off")
    private boolean persistPersons = true;

    @GUIparameter(description="Toggle to turn persistence of benefit units on/off")
    private boolean persistBenefitUnits = true;

    @GUIparameter(description = "Toggle to turn persistence of households on/off")
    private boolean persistHouseholds = true;

    @GUIparameter(description="First time-step to dump data to database")
    private Long dataDumpStartTime = 0L;

    @GUIparameter(description="Number of time-steps in between database dumps")
    private Double dataDumpTimePeriod = 1.;

    private int ordering = Parameters.COLLECTOR_ORDERING;	//XXX: Move to Parameters?	//Schedule at the same time as the model and observer events, but with an order higher than model but less than observer, so will be fired after the model and before the observe have updated.

    private SimPathsModel model;

    private Statistics stats;

    private Statistics2 stats2;

    private Statistics3 stats3;

    private GiniPersonalGrossEarnings giniPersonalGrossEarnings;

    private GiniEquivalisedHouseholdDisposableIncome giniEquivalisedHouseholdDisposableIncome;

    private Ydses_c5 ydses_c5;

    private EDI edi; //Equivalised disposable income statistics

    private SIndex sIndex;

    private GrossLabourIncome grossLabourIncome;

    private DataExport exportPersons;

    private DataExport exportBenefitUnits;

    private DataExport exportHouseholds;

    private DataExport exportStatistics;

    private DataExport exportStatistics2;

    private DataExport exportStatistics3;

    protected MultiTraceFunction.Double fGiniPersonalGrossEarningsNational;

    protected Map<Region, MultiTraceFunction.Double> fGiniPersonalGrossEarningsRegionalMap;

    protected MultiTraceFunction.Double fGiniEquivalisedHouseholdDisposableIncomeNational;

    protected Map<Region, MultiTraceFunction.Double> fGiniEquivalisedHouseholdDisposableIncomeRegionalMap;



    /**
     *
     * CONSTRUCTOR FOR SIMULATION COLLECTOR
     *
     */
    public SimPathsCollector(SimulationManager manager) {
        super(manager);
    }

    // ---------------------------------------------------------------------
    // Event Listener
    // ---------------------------------------------------------------------

    public enum Processes {

        CalculateSIndex,
        CalculateHouseholdsGrossIncome,
        CalculateEquivalisedHouseholdDisposableIncome,
        CalculateGiniCoefficients,
        DumpPersons,
        DumpBenefitUnits,
        DumpHouseholds,
        DumpStatistics,
        DumpStatistics2,
		DumpStatistics3,
    }


    /**
     *
     * XXX
     *
     */
    @Override
    public void onEvent(Enum<?> type) {
        switch ((Processes) type) {

        case CalculateSIndex:
            calculateSIndex();
        case CalculateHouseholdsGrossIncome:
            calculateGrossIncome();
        case CalculateEquivalisedHouseholdDisposableIncome:
            calculateEquivalisedHouseholdDisposableIncome();
            calculateEDI();
            break;
        case CalculateGiniCoefficients:
            calculateGiniCoefficients();
            break;
        //To output data:
        case DumpPersons:
            try {
                exportPersons.export();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            break;
        case DumpBenefitUnits:
            try {
                exportBenefitUnits.export();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            break;
        case DumpHouseholds:
            try {
                exportHouseholds.export();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            break;
        case DumpStatistics:
            try {
                exportStatistics.export();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            break;
        case DumpStatistics2:
            stats2.update(model);
            try {
                exportStatistics2.export();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            break;
		case DumpStatistics3:
			stats3.update(model);
			try {
				exportStatistics3.export();
			} catch (Exception e) {
				log.error(e.getMessage());
			}
			break;
        }
    }


    // ---------------------------------------------------------------------
    // Manager
    // ---------------------------------------------------------------------

    @Override
    public void buildObjects() {

        model = (SimPathsModel) getManager();

        stats = new Statistics();
        stats2 = new Statistics2();
        stats3 = new Statistics3();

        //For export to database or .csv files.
        if(persistPersons)
            exportPersons = new DataExport(model.getPersons(), exportToDatabase, exportToCSV);
        if(persistBenefitUnits)
            exportBenefitUnits = new DataExport(model.getBenefitUnits(), exportToDatabase, exportToCSV);
        if (persistHouseholds)
            exportHouseholds = new DataExport(model.getHouseholds(), exportToDatabase, exportToCSV);
        if (persistStatistics)
            exportStatistics = new DataExport(stats, exportToDatabase, exportToCSV);
        if (persistStatistics2)
            exportStatistics2 = new DataExport(stats2, exportToDatabase, exportToCSV);
        if (persistStatistics3)
            exportStatistics3 = new DataExport(stats3, exportToDatabase, exportToCSV);


        if (calculateGiniCoefficients) {

            giniPersonalGrossEarnings = new GiniPersonalGrossEarnings();
            fGiniPersonalGrossEarningsNational = new MultiTraceFunction.Double(giniPersonalGrossEarnings, "getGiniPersonalGrossEarningsNational", true);
            fGiniPersonalGrossEarningsRegionalMap = new LinkedHashMap<Region, MultiTraceFunction.Double>();
            for(Region region: Parameters.getCountryRegions()) {
                MultiTraceFunction.Double fGiniPersonalGrossEarningsRegion = new MultiTraceFunction.Double(giniPersonalGrossEarnings, region);
                fGiniPersonalGrossEarningsRegionalMap.put(region, fGiniPersonalGrossEarningsRegion);
            }

            giniEquivalisedHouseholdDisposableIncome = new GiniEquivalisedHouseholdDisposableIncome();
            fGiniEquivalisedHouseholdDisposableIncomeNational = new MultiTraceFunction.Double(giniEquivalisedHouseholdDisposableIncome, "getGiniEquivalisedHouseholdDisposableIncomeNational", true);
            fGiniEquivalisedHouseholdDisposableIncomeRegionalMap = new LinkedHashMap<Region, MultiTraceFunction.Double>();
            for(Region region: Parameters.getCountryRegions()) {
                MultiTraceFunction.Double fGiniEquivalisedHouseholdDisposableIncomeRegion = new MultiTraceFunction.Double(giniEquivalisedHouseholdDisposableIncome, region);
                fGiniEquivalisedHouseholdDisposableIncomeRegionalMap.put(region, fGiniEquivalisedHouseholdDisposableIncomeRegion);
            }
        }

        ydses_c5 = new Ydses_c5();
        edi = new EDI();
        sIndex = new SIndex();
        grossLabourIncome = new GrossLabourIncome();


    }

    @Override
    public void buildSchedule() {

        SimPathsModel model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());

        getEngine().getEventQueue().scheduleRepeat(new SingleTargetEvent(this, Processes.CalculateHouseholdsGrossIncome), model.getStartYear(), ordering, dataDumpTimePeriod);

//		getEngine().getEventQueue().scheduleRepeat(new CollectionTargetEvent(model.getHouseholds(), BenefitUnit.Processes.CalculateEquivalisedDisposableIncome, true), model.getStartYear() + dataDumpStartTime, ordering, dataDumpTimePeriod);
//		getEngine().getEventQueue().scheduleOnce(new SingleTargetEvent(this, Processes.CalculateEquivalisedHouseholdDisposableIncome), model.getStartYear(), Order.BEFORE_ALL.getOrdering());
//		getEngine().getEventQueue().scheduleOnce(new SingleTargetEvent(this, Processes.CalculateEquivalisedHouseholdDisposableIncome), model.getStartYear(), -2); //Run once in the start year, before the model?
//		getEngine().getEventQueue().scheduleRepeat(new SingleTargetEvent(this, Processes.CalculateEquivalisedHouseholdDisposableIncome), model.getStartYear(), ordering, dataDumpTimePeriod);
//		getEngine().getEventQueue().scheduleOnce(new SingleTargetEvent(this, Processes.CalculateEquivalisedHouseholdDisposableIncome), model.getEndYear(), -2);
        getEngine().getEventQueue().scheduleRepeat(new SingleTargetEvent(this, Processes.CalculateSIndex), model.getStartYear(), ordering, dataDumpTimePeriod);

        if (calculateGiniCoefficients) {
            getEngine().getEventQueue().scheduleRepeat(new SingleTargetEvent(this, Processes.CalculateGiniCoefficients), model.getStartYear() + dataDumpStartTime, ordering, dataDumpTimePeriod);
        }

        if (persistStatistics) {
            getEngine().getEventQueue().scheduleRepeat(new SingleTargetEvent(this, Processes.DumpStatistics), model.getStartYear() + dataDumpStartTime, ordering, dataDumpTimePeriod);
//			getEngine().getEventQueue().scheduleOnce(new SingleTargetEvent(this, Processes.DumpStatistics), model.getEndYear(), -2);		//Ensures the database is persisted on the last time-step
        }

        if (persistStatistics2) {
            getEngine().getEventQueue().scheduleRepeat(new SingleTargetEvent(this, Processes.DumpStatistics2), model.getStartYear() + dataDumpStartTime, ordering, dataDumpTimePeriod);
        }

		if (persistStatistics3) {
			getEngine().getEventQueue().scheduleRepeat(new SingleTargetEvent(this, Processes.DumpStatistics3), model.getStartYear() + dataDumpStartTime, ordering, dataDumpTimePeriod);
		}

        if (persistPersons) {
            getEngine().getEventQueue().scheduleRepeat(new SingleTargetEvent(this, Processes.DumpPersons), model.getStartYear() + dataDumpStartTime, ordering, dataDumpTimePeriod);
        }

        if (persistBenefitUnits) {
            getEngine().getEventQueue().scheduleRepeat(new SingleTargetEvent(this, Processes.DumpBenefitUnits), model.getStartYear() + dataDumpStartTime, ordering, dataDumpTimePeriod);
        }

        if (persistHouseholds) {
            getEngine().getEventQueue().scheduleRepeat(new SingleTargetEvent(this, Processes.DumpHouseholds), model.getStartYear() + dataDumpStartTime, ordering, dataDumpTimePeriod);
        }
    }

    // ---------------------------------------------------------------------
    //	Inner classes for data collection
    // ---------------------------------------------------------------------

    private class SIndex implements IDoubleSource {

        final SimPathsModel model = (SimPathsModel) getManager();

        private CrossSection.Double personsSIndexCS;

        private PercentileArrayFunction percentileFunctionSIndexCS;

        double sIndexMedianForNormalisation;

        //What I probably need is a map with <person, array of incomes>
        //Or, person would have to have a series object in which their incomes over time are collected. Then, each year, update that object to record new income, and calculate the security index looking at last T elements in that series
/*
        public void update() {
            for (Person person : model.getPersons()) { //For each person, get values of income over time in a series object
                person.getYearlyEquivalisedDisposableIncomeSeries().updateSource(); //Update values in the series of incomes

                Series.Double incomeSeries = person.getYearlyEquivalisedDisposableIncomeSeries();
                double[] incomeValues = incomeSeries.getDoubleArray();
                int timeWindow = model.getsIndexTimeWindow(); //Time window in which the S Index should be calculated
                int lengthIncomeSeries = incomeValues.length;

                if (lengthIncomeSeries >= timeWindow) { //Only start calculating the index when enough years for the specified time window elapsed
                    int t = timeWindow;
                    double numeratorSum = 0.;
                    double denominatorSum = 0.;
                    for (int i = lengthIncomeSeries-1; i >= (lengthIncomeSeries - timeWindow); i--) { //Start at the end of the income series and iterate back within the time window

                        double numeratorValue, denominatorValue, incomeValue;

                        //different formula depending on the person here: if working or if retired
                        //TODO: do we still need to account for different amount of capital income depending on age?
                        //Formula to calculate here
                        if (person.getLes_c4().equals(Les_c4.Retired)) {
                            incomeValue = incomeValues[i];
                        } else {
                            incomeValue = (1-model.getsIndexS())*incomeValues[i]; //If not retired, use (1-s)Y
                        }
                        numeratorValue = Math.pow(incomeValue, 1/model.getsIndexAlpha())*Math.pow(model.getsIndexDelta(), t); //Formula to calculate SIndex
                        denominatorValue = Math.pow(model.getsIndexDelta(), t);

                        numeratorSum += numeratorValue; //Note that for any period in which income was missing (-9999.99), this will result in a NaN sIndex. Should we use 0 for income instead?
                        denominatorSum += denominatorValue;
                        t--; //Because t is used in the formula
                    }
                    double sIndex = numeratorSum/denominatorSum;

                    //Update SIndex for the person
                    person.setsIndex(sIndex);
                //	person.getsIndexYearMap().put(model.getYear()-timeWindow, sIndex);
                //	System.out.println(person.getsIndexYearMap());

                //	System.out.println("For person " + person.getKey().getId() + " the sIndex is " + sIndex + " in year " + model.getYear());

                    //TODO: plot a histogram or a pyramid with sIndex for different categories: by gender, education, employment status

                }
            }
        }
*/

        public void update() {
            for (Person person : model.getPersons()) { //For each person, get values of income over time in a series object
                person.getYearlyEquivalisedConsumptionSeries().updateSource(); //Update values in the series of consumption

                Series.Double consumptionSeries = person.getYearlyEquivalisedConsumptionSeries();
                double[] consumptionValues = consumptionSeries.getDoubleArray();
                int timeWindow = model.getsIndexTimeWindow(); //Time window in which the S Index should be calculated
                int lengthConsumptionSeries = consumptionValues.length;

                if (lengthConsumptionSeries >= timeWindow) { //Only start calculating the index when enough years for the specified time window elapsed
                    int t = timeWindow;
                    double numeratorSum = 0.;
                    double denominatorSum = 0.;
                    for (int i = lengthConsumptionSeries-1; i >= (lengthConsumptionSeries - timeWindow); i--) { //Start at the end of the income series and iterate back within the time window

                        double numeratorValue, denominatorValue, consumptionValue;

                        consumptionValue = consumptionValues[i];

                        //TODO: do we still need to account for different amount of capital income depending on age?
                        numeratorValue = Math.pow(consumptionValue, 1/model.getsIndexAlpha())*Math.pow(model.getsIndexDelta(), t); //Formula to calculate SIndex
                        denominatorValue = Math.pow(model.getsIndexDelta(), t);

                        numeratorSum += numeratorValue; //Note that for any period in which income was missing (-9999.99), this will result in a NaN sIndex. Should we use 0 for income instead?
                        denominatorSum += denominatorValue;
                        t--; //Because t is used in the formula
                    }
                    double sIndex = numeratorSum/denominatorSum;

                    //Update SIndex for the person
                    person.setsIndex(sIndex);

                    //	person.getsIndexYearMap().put(model.getYear()-timeWindow, sIndex);
                    //	System.out.println(person.getsIndexYearMap());

                    //	System.out.println("For person " + person.getKey().getId() + " the sIndex is " + sIndex + " in year " + model.getYear());

                    //TODO: plot a histogram or a pyramid with sIndex for different categories: by gender, education, employment status

                }
            }

            //Now calculate the median value of the sIndex
            personsSIndexCS = new CrossSection.Double(model.getPersons(), Person.DoublesVariables.sIndex);
            percentileFunctionSIndexCS = new PercentileArrayFunction(personsSIndexCS);
            percentileFunctionSIndexCS.updateSource();

            //Set value in the statistics object
            stats.setsIndex_p50(percentileFunctionSIndexCS.getDoubleValue(PercentileArrayFunction.Variables.P50));

            if (model.getYear() == model.getStartYear()+model.getsIndexTimeWindow()+1) { //+1 added to the RHS because model increments the year and model runs before the collector
                sIndexMedianForNormalisation = stats.getsIndex_p50();
            }

            //If SIndex is calculated, normalise it
            if (model.getYear() >= model.getStartYear()+model.getsIndexTimeWindow()) {
                for (Person person : model.getPersons()) {
                    double normalisedSIndex = person.getsIndex()/sIndexMedianForNormalisation;
                    person.setsIndexNormalised(normalisedSIndex);
                }
            }

        }

        @Override
        public double getDoubleValue(Enum<?> anEnum) {
            return 0;
        }
    }

    private class GrossLabourIncome {

        final SimPathsModel model = (SimPathsModel) getManager();
        private CrossSection.Double personsGrossLabourIncomesCS;
        private PercentileArrayFunction percentileFunctionGrossLabourIncomes;

        public void update() {
            personsGrossLabourIncomesCS = new CrossSection.Double(model.getPersons(), GrossLabourIncomeMonthly); // Retrieve Gross Labour Income monthly value using native access through IDoubleSource
            FlexibleInLabourSupplyFilter flexibleInLabourSupplyFilter = new FlexibleInLabourSupplyFilter();
            personsGrossLabourIncomesCS.setFilter(flexibleInLabourSupplyFilter); // Filter only those who could work for calculation of quintiles of gross labour income

            percentileFunctionGrossLabourIncomes = new PercentileArrayFunction(personsGrossLabourIncomesCS);
            percentileFunctionGrossLabourIncomes.updateSource();

            stats.setGrossLabourIncome_p20(percentileFunctionGrossLabourIncomes.getDoubleValue(PercentileArrayFunction.Variables.P20));
            stats.setGrossLabourIncome_p40(percentileFunctionGrossLabourIncomes.getDoubleValue(PercentileArrayFunction.Variables.P40));
            stats.setGrossLabourIncome_p60(percentileFunctionGrossLabourIncomes.getDoubleValue(PercentileArrayFunction.Variables.P60));
            stats.setGrossLabourIncome_p80(percentileFunctionGrossLabourIncomes.getDoubleValue(PercentileArrayFunction.Variables.P80));

            for (Person person : model.getPersons()) {
                double covidModuleGrossLabourIncomeBaseline = person.getCovidModuleGrossLabourIncome_Baseline();
                if (covidModuleGrossLabourIncomeBaseline <= stats.getGrossLabourIncome_p20()) {
                    person.setCovidModuleGrossLabourIncomeBaseline_Xt5(Quintiles.Q1);
                } else if (covidModuleGrossLabourIncomeBaseline <= stats.getGrossLabourIncome_p40()) {
                    person.setCovidModuleGrossLabourIncomeBaseline_Xt5(Quintiles.Q2);
                } else if (covidModuleGrossLabourIncomeBaseline <= stats.getGrossLabourIncome_p60()) {
                    person.setCovidModuleGrossLabourIncomeBaseline_Xt5(Quintiles.Q3);
                } else if (covidModuleGrossLabourIncomeBaseline <= stats.getGrossLabourIncome_p80()) {
                    person.setCovidModuleGrossLabourIncomeBaseline_Xt5(Quintiles.Q4);
                } else {
                    person.setCovidModuleGrossLabourIncomeBaseline_Xt5(Quintiles.Q5);
                }
            }

        }
    }


    /*
     *This method calculates quintiles of household gross income
     *
     */
    private class Ydses_c5 implements IDoubleSource {

        final SimPathsModel model = (SimPathsModel) getManager();

        private CrossSection.Double householdsGrossIncomesCS;

        private PercentileArrayFunction percentileFunctionHouseholdsGrossIncomes;

        private double p50HouseholdsGrossIncome;

        private double p20HouseholdsGrossIncome;

        private double p40HouseholdsGrossIncome;

        private double p60HouseholdsGrossIncome;

        private double p80HouseholdsGrossIncome;

        public void update() {

            //Ydses_c5
            householdsGrossIncomesCS = new CrossSection.Double(model.getBenefitUnits(), BenefitUnit.class, "getTmpHHYpnbihs_dv_asinh", true); //Populate CS

            percentileFunctionHouseholdsGrossIncomes = new PercentileArrayFunction(householdsGrossIncomesCS); //Get p50
            percentileFunctionHouseholdsGrossIncomes.updateSource();
            p50HouseholdsGrossIncome = percentileFunctionHouseholdsGrossIncomes.getDoubleValue(PercentileArrayFunction.Variables.P50); //Retrieve P50 value
            p20HouseholdsGrossIncome = percentileFunctionHouseholdsGrossIncomes.getDoubleValue(PercentileArrayFunction.Variables.P20);
            p40HouseholdsGrossIncome = percentileFunctionHouseholdsGrossIncomes.getDoubleValue(PercentileArrayFunction.Variables.P40);
            p60HouseholdsGrossIncome = percentileFunctionHouseholdsGrossIncomes.getDoubleValue(PercentileArrayFunction.Variables.P60);
            p80HouseholdsGrossIncome = percentileFunctionHouseholdsGrossIncomes.getDoubleValue(PercentileArrayFunction.Variables.P80);
//			System.out.println("P50 value from the percentile function: " + p50HouseholdsGrossIncome + " P20: " + p20HouseholdsGrossIncome + " P40: " + p40HouseholdsGrossIncome +
//								" P60: " + p60HouseholdsGrossIncome + " P80: " + p80HouseholdsGrossIncome);

            stats.setYdses_p20(p20HouseholdsGrossIncome);
            stats.setYdses_p40(p40HouseholdsGrossIncome);
            stats.setYdses_p60(p60HouseholdsGrossIncome);
            stats.setYdses_p80(p80HouseholdsGrossIncome);


        }


        @Override
        public double getDoubleValue(Enum<?> variableID) {
            // TODO Auto-generated method stub
            return 0;
        }

    }

    private class EDI implements IDoubleSource {
        final SimPathsModel model = (SimPathsModel) getManager();
        private CrossSection.Double householdsEDICS;
        private PercentileArrayFunction percentileFunctionHouseholdsEDI;
        private double p50HouseholdsEDI;

        public void update() {
            householdsEDICS = new CrossSection.Double(model.getBenefitUnits(), BenefitUnit.class, "getEquivalisedDisposableIncomeYearly", true);
            percentileFunctionHouseholdsEDI = new PercentileArrayFunction(householdsEDICS);
            percentileFunctionHouseholdsEDI.updateSource();
            p50HouseholdsEDI = percentileFunctionHouseholdsEDI.getDoubleValue(PercentileArrayFunction.Variables.P50);
            stats.setEdi_p50(p50HouseholdsEDI);
//			System.out.println("Median EDI (collector) "+p50HouseholdsEDI);
        }

        @Override
        public double getDoubleValue(Enum<?> variableID) {
            // TODO Auto-generated method stub
            return 0;
        }
    }


    private class GiniPersonalGrossEarnings implements IDoubleSource {
        //I calculate that the Gini coefficient for household-weights w_i and variables x_i:
        //	G = [ sum_i sum_j w_i * w_j * abs( x_i - x_j) ] / [ 2 * (sum_i w_i) * (sum_j w_j * x_j) ]
        //Note in this particular case, the x_i are the personal (individual) gross income (potential earnings * labour supply)

        final SimPathsModel model = (SimPathsModel) getManager();

        private Map<Region, Double> giniWeightedPersonalGrossEarningsRegionalMap = new LinkedHashMap<Region, Double>();

        private double giniWeightedPersonalGrossEarningsNational;

        //Update gini coefficient of personal gross earnings
        public void update() {

            //Note that weighted means individual person weighted measures
            double weightedAbsDiffPersonalGrossEarningsNational = 0.;	//Sum of absolute difference between two person's weighted gross earnings (personal weight * potential earnings * labour supply)

            Map<Region, Double> weightedAbsDiffPersonalGrossEarningsRegional = new LinkedHashMap<Region, Double>();	//Sum of absolute difference between two person's weighted gross earnings (personal weight * potential earnings * labour supply)
            Map<Region, Double> totalWeightedPersonalGrossEarningsRegional = new LinkedHashMap<Region, Double>();	//Sum of (personal weight * potential earnings * labour supply)
            Map<Region, Double> totalPersonWeightRegional = new LinkedHashMap<Region, Double>();	//Sum of personal weights

            for(Region region: Parameters.getCountryRegions()) {
                weightedAbsDiffPersonalGrossEarningsRegional.put(region,  0.);
                totalWeightedPersonalGrossEarningsRegional.put(region,  0.);
                totalPersonWeightRegional.put(region,  0.);
            }

            //Filter out people with non-finite or negative gross earnings
            Map<Person, Double> validPersonalGrossEarningsMap = new LinkedHashMap<Person, Double>();
            for(Person person: model.getPersons()) {
                Double grossEarnings = person.getGrossEarningsWeekly();
                if(grossEarnings != null && Double.isFinite(grossEarnings) && grossEarnings >= 0.) {
                    validPersonalGrossEarningsMap.put(person, grossEarnings);
                }
            }

            for(Map.Entry<Person, Double> e1 : validPersonalGrossEarningsMap.entrySet()) {

                Person person1 = e1.getKey();
                Region region1 = person1.getRegion();
                double personWeight1 = person1.getWeight();
                double grossEarnings1 = e1.getValue();
                double weightedPersonalGrossEarnings1 = personWeight1 * grossEarnings1;

                Double thw = totalPersonWeightRegional.get(region1);
                totalPersonWeightRegional.put(region1,  thw + personWeight1);

                Double tir = totalWeightedPersonalGrossEarningsRegional.get(region1);
                totalWeightedPersonalGrossEarningsRegional.put(region1, tir + weightedPersonalGrossEarnings1);

                for(Map.Entry<Person, Double> e2 : validPersonalGrossEarningsMap.entrySet()) {

                    Person person2 = e2.getKey();
                    Region region2 = person2.getRegion();
                    double personWeight2 = person2.getWeight();
                    double grossEarnings2 = e2.getValue();

                    double weightedAbsDiffGrossEarnings = personWeight1 * personWeight2 * Math.abs(grossEarnings1 - grossEarnings2);
                    weightedAbsDiffPersonalGrossEarningsNational += weightedAbsDiffGrossEarnings;

                    if(region1.equals(region2)) {
                        double adGrossEarningsR = weightedAbsDiffPersonalGrossEarningsRegional.get(region1);
                        weightedAbsDiffPersonalGrossEarningsRegional.put(region1, adGrossEarningsR + weightedAbsDiffGrossEarnings);
                    }
                }
            }

            double totalWeightedPersonalGrossEarningsNational = 0.;	//Sum of (personal weight * potential earnings * labour supply)
            double totalPersonWeightNational = 0.;	//Sum of (person Weight)
            for(Region region : Parameters.getCountryRegions()) {
                double totalPersonWeightForRegion = totalPersonWeightRegional.get(region);
                totalPersonWeightNational += totalPersonWeightForRegion;

                double totalWeightedPersonalGrossEarningsForRegion = totalWeightedPersonalGrossEarningsRegional.get(region);
                totalWeightedPersonalGrossEarningsNational += totalWeightedPersonalGrossEarningsForRegion;

                double giniRegional = weightedAbsDiffPersonalGrossEarningsRegional.get(region) / (2. * totalPersonWeightForRegion * totalWeightedPersonalGrossEarningsForRegion);
                giniWeightedPersonalGrossEarningsRegionalMap.put(region, giniRegional);
                log.info("giniWeightedPersonalGrossEarningsRegionalMap for " + region + " = " + giniWeightedPersonalGrossEarningsRegionalMap.get(region) + ", weightedAbsDiffEquivalisedIncomeRegional.get(region) = " + weightedAbsDiffPersonalGrossEarningsRegional.get(region) + ", totalPersonWeightForRegion = " + totalPersonWeightForRegion + ", totalWeightedPersonalGrossEarningsForRegion = " + totalWeightedPersonalGrossEarningsForRegion);
            }
            giniWeightedPersonalGrossEarningsNational = weightedAbsDiffPersonalGrossEarningsNational / (2. * totalPersonWeightNational * totalWeightedPersonalGrossEarningsNational);
            stats.setGiniPersonalGrossEarningsNational(giniWeightedPersonalGrossEarningsNational);
            log.info("giniWeightedPersonalGrossEarningsNational = " + giniWeightedPersonalGrossEarningsNational + ", weightedAbsDiffPersonalGrossEarningsNational = " + weightedAbsDiffPersonalGrossEarningsNational + ", totalPersonWeightNational = " + totalPersonWeightNational + ", totalWeightedPersonalGrossEarningsNational = " + totalWeightedPersonalGrossEarningsNational);

        }

        //Getter methods to pass values to functions

        //For national level
        public double getGiniPersonalGrossEarningsNational() {
            return giniWeightedPersonalGrossEarningsNational;
        }

        //For regional level
        @Override
        public double getDoubleValue(Enum<?> region) {
            return giniWeightedPersonalGrossEarningsRegionalMap.get(region);
        }

    }


    private class GiniEquivalisedHouseholdDisposableIncome implements IDoubleSource {

        //I calculate that the Gini coefficient for household-weights w_i and variables x_i:
        //	G = [ sum_i sum_j w_i * w_j * abs( x_i - x_j) ] / [ 2 * (sum_i w_i) * (sum_j w_j * x_j) ]
        //Note in this particular case, the x_i are the equivalised household income, so the variable itself also contains an 'equivalised weight', which is treated as part of the income (and different from the household-weight)

        final SimPathsModel model = (SimPathsModel) getManager();

        private Map<Region, Double> giniWeightedEquivalisedHouseholdDisposableIncomeRegionalMap = new LinkedHashMap<Region, Double>();

        private double giniWeightedEquivalisedHouseholdDisposableIncomeNational;

        //Update gini coefficient of household disposable income
        public void update() {

            //Note that weighted means household weighted measures
            double weightedAbsDiffEquivalisedIncomeNational = 0.;	//Sum of absolute difference between two household's weighted equivalised income (household weight * equivalised weight * household disposable income)

            Map<Region, Double> weightedAbsDiffEquivalisedIncomeRegional = new LinkedHashMap<Region, Double>();	//Sum of absolute difference between two household's weighted equivalised income (household weight * equivalised weight * household disposable income)
            Map<Region, Double> totalWeightedEquivalisedHouseholdIncomeRegional = new LinkedHashMap<Region, Double>();	//Sum of (household weight * equivalised weight * household disposable income)
            Map<Region, Double> totalHouseholdWeightRegional = new LinkedHashMap<Region, Double>();	//Sum of (household weight)

            for(Region region: Parameters.getCountryRegions()) {
                weightedAbsDiffEquivalisedIncomeRegional.put(region,  0.);
                totalWeightedEquivalisedHouseholdIncomeRegional.put(region,  0.);
                totalHouseholdWeightRegional.put(region,  0.);
            }

            //Filter out households with non-finite or negative disposable income
            Map<BenefitUnit, Double> validHousesEquivalisedIncomeMap = new LinkedHashMap<BenefitUnit, Double>();
            for(BenefitUnit house: model.getBenefitUnits()) {
                Double income = house.getEquivalisedDisposableIncomeYearly();
                if(income != null && Double.isFinite(income) && income >= 0.) {
                    validHousesEquivalisedIncomeMap.put(house, income);
                }
            }

            for(Map.Entry<BenefitUnit, Double> e1 : validHousesEquivalisedIncomeMap.entrySet()) {

                BenefitUnit house1 = e1.getKey();
                Region region1 = house1.getRegion();
                double houseWeight1 = house1.getWeight();
                double equivalisedIncome1 = e1.getValue();
                double weightedEquivalisedHouseholdIncome1 = houseWeight1 * equivalisedIncome1;	//Equivalised income * BenefitUnit-Weight

                Double thw = totalHouseholdWeightRegional.get(region1);
                totalHouseholdWeightRegional.put(region1,  thw + houseWeight1);

                Double tir = totalWeightedEquivalisedHouseholdIncomeRegional.get(region1);
                totalWeightedEquivalisedHouseholdIncomeRegional.put(region1, tir + weightedEquivalisedHouseholdIncome1);

                for(Map.Entry<BenefitUnit, Double> e2 : validHousesEquivalisedIncomeMap.entrySet()) {

                    BenefitUnit house2 = e2.getKey();
                    Region region2 = house2.getRegion();
                    double houseWeight2 = house2.getWeight();
                    double equivalisedIncome2 = e2.getValue();

                    double weightedAbsDiffEquivalisedIncome = houseWeight1 * houseWeight2 * Math.abs(equivalisedIncome1 - equivalisedIncome2);
                    weightedAbsDiffEquivalisedIncomeNational += weightedAbsDiffEquivalisedIncome;

                    if(region1.equals(region2)) {
                        double adIncomeR = weightedAbsDiffEquivalisedIncomeRegional.get(region1);
                        weightedAbsDiffEquivalisedIncomeRegional.put(region1, adIncomeR + weightedAbsDiffEquivalisedIncome);
                    }
                }
            }

            double totalWeightedEquivalisedHouseholdIncomeNational = 0.;	//Sum of (household weight * equivalised weight * household disposable income)
            double totalHouseholdWeightNational = 0.;	//Sum of (household weight)
            for(Region region : Parameters.getCountryRegions()) {
                double totalHouseholdWeightForRegion = totalHouseholdWeightRegional.get(region);
                totalHouseholdWeightNational += totalHouseholdWeightForRegion;

                double totalWeightedEquivalisedHouseholdIncomeForRegion = totalWeightedEquivalisedHouseholdIncomeRegional.get(region);
                totalWeightedEquivalisedHouseholdIncomeNational += totalWeightedEquivalisedHouseholdIncomeForRegion;

                double giniRegional = weightedAbsDiffEquivalisedIncomeRegional.get(region) / (2. * totalHouseholdWeightForRegion * totalWeightedEquivalisedHouseholdIncomeForRegion);
                giniWeightedEquivalisedHouseholdDisposableIncomeRegionalMap.put(region, giniRegional);
                log.info("giniHouseholdDisposableIncomeRegional for " + region + " = " + giniWeightedEquivalisedHouseholdDisposableIncomeRegionalMap.get(region) + ", weightedAbsDiffEquivalisedIncomeRegional.get(region) = " + weightedAbsDiffEquivalisedIncomeRegional.get(region) + ", totalHouseholdWeightForRegion = " + totalHouseholdWeightForRegion + ", totalWeightedEquivalisedHouseholdIncomeForRegion = " + totalWeightedEquivalisedHouseholdIncomeForRegion);
            }
            giniWeightedEquivalisedHouseholdDisposableIncomeNational = weightedAbsDiffEquivalisedIncomeNational / (2. * totalHouseholdWeightNational * totalWeightedEquivalisedHouseholdIncomeNational);
            stats.setGiniEquivalisedHouseholdDisposableIncomeNational(giniWeightedEquivalisedHouseholdDisposableIncomeNational);
            log.info("giniWeightedEquivalisedHouseholdDisposableIncomeNational = " + giniWeightedEquivalisedHouseholdDisposableIncomeNational + ", weightedAbsDiffEquivalisedIncomeNational = " + weightedAbsDiffEquivalisedIncomeNational + ", totalHouseholdWeightNational = " + totalHouseholdWeightNational + ", totalWeightedEquivalisedHouseholdIncomeNational = " + totalWeightedEquivalisedHouseholdIncomeNational);

        }

        //Getter methods to pass values to functions

        //For national level
        public double getGiniEquivalisedHouseholdDisposableIncomeNational() {
            return giniWeightedEquivalisedHouseholdDisposableIncomeNational;
        }

        //For regional level
        @Override
        public double getDoubleValue(Enum<?> region) {
            return giniWeightedEquivalisedHouseholdDisposableIncomeRegionalMap.get(region);
        }

    }


    // ---------------------------------------------------------------------
    // methods
    // ---------------------------------------------------------------------

    private void calculateEquivalisedHouseholdDisposableIncome() {

        ArrayList<Pair<BenefitUnit, Double>> arrHouse_eqHouseholdDispIncome = new ArrayList<Pair<BenefitUnit, Double>>();
        double totalWeight = 0.;
        for(BenefitUnit house: model.getBenefitUnits()) {
            double hedi = house.calculateEquivalisedDisposableIncomeYearly();
            if(hedi >= 0.) {
                arrHouse_eqHouseholdDispIncome.add(new Pair<BenefitUnit, Double>(house, hedi));
                totalWeight += house.getWeight();
            }
            else {		//Cannot include house in statistics as unable to calculate eq disp income
                house.setAtRiskOfPoverty(1);		//If benefit unit has equivalised disposable income < 0, it should be classified as at risk of poverty
            }
        }

        arrHouse_eqHouseholdDispIncome.sort(new Comparator<Pair<BenefitUnit, Double>>(){
                @Override
                public int compare(Pair<BenefitUnit, Double> pair1, Pair<BenefitUnit, Double> pair2) {
                    return (int) Math.signum(pair1.getSecond() - pair2.getSecond());
                }
            }
        );

        double WeightCounter = 0.;
        Double median = null;
//		log.info("arrHouse_eqHouseholdDispIncome " + arrHouse_eqHouseholdDispIncome + ", size " + arrHouse_eqHouseholdDispIncome.size());
        for(Pair<BenefitUnit, Double> pairHouse_Income: arrHouse_eqHouseholdDispIncome) {

            WeightCounter += pairHouse_Income.getFirst().getWeight();
//			log.info("eq hh disp income " + pairHouse_Income.getSecond() + ", WeightCounter " + WeightCounter + ", total Weight " + totalWeight + ", proportion so far " + WeightCounter/totalWeight);
            if(WeightCounter >= totalWeight/2.) {
                median = pairHouse_Income.getSecond();
//				log.info("WeightCounter " + WeightCounter + ", median " + median);
                break;
            }
        }

        double atRiskOfPovertyThreshold = median * 0.6;
//		log.info("atRiskOfPovertyThreshold = " + atRiskOfPovertyThreshold);
        stats.setMedianEquivalisedHouseholdDisposableIncome(median);		//Save median household equivalised disposable income in statistics object
//		stats.setRiskOfPovertyThreshold(atRiskOfPovertyThreshold);			//Risk-of-poverty threshold is set at 60% of the national median equivalised household disposable income.
//		System.out.println("Median EDI " + median + " Poverty threshold " + atRiskOfPovertyThreshold);

        //For use in charts
        for(Pair<BenefitUnit, Double> pairHouse_Income: arrHouse_eqHouseholdDispIncome) {
            BenefitUnit house = pairHouse_Income.getFirst();
            if(house.getEquivalisedDisposableIncomeYearly() < atRiskOfPovertyThreshold) {
                house.setAtRiskOfPoverty(1);
            }
            else {
                house.setAtRiskOfPoverty(0);
            }
        }

    }

    private void calculateGiniCoefficients() {			//Called just before database dump of statistics entity

        giniPersonalGrossEarnings.update();
        giniEquivalisedHouseholdDisposableIncome.update();

    }

    private void calculateEDI() {
        edi.update();
    }

    private void calculateGrossIncome() {
        ydses_c5.update();
        grossLabourIncome.update();
    }

    private void calculateSIndex() {
        /*
        This method calculates the in(security) S Index for each individual in the simulation, using the time window and alpha and delta parameters specified in the model class.
         */
        sIndex.update();
    }

    // ---------------------------------------------------------------------
    // getters and setters
    // ---------------------------------------------------------------------

    public boolean isPersistPersons() {
        return persistPersons;
    }

    public void setPersistPersons(boolean persistPersons) {
        this.persistPersons = persistPersons;
    }

    public boolean isPersistBenefitUnits() {
        return persistBenefitUnits;
    }

    public void setPersistBenefitUnits(boolean persistBenefitUnits) {
        this.persistBenefitUnits = persistBenefitUnits;
    }

    public boolean isPersistHouseholds() { return persistHouseholds; }

    public void setPersistHouseholds(boolean persistHouseholds) { this.persistHouseholds = persistHouseholds; }

    public Long getDataDumpStartTime() {
        return dataDumpStartTime;
    }

    public void setDataDumpStartTime(Long dataDumpStartTime) {
        this.dataDumpStartTime = dataDumpStartTime;
    }

    public Double getDataDumpTimePeriod() {
        return dataDumpTimePeriod;
    }

    public void setDataDumpTimePeriod(Double dataDumpTimePeriod) {
        this.dataDumpTimePeriod = dataDumpTimePeriod;
    }

    public Statistics getStats() {
        return stats;
    }

    public void setStats(Statistics stats) {
        this.stats = stats;
    }

    public Statistics2 getStats2() { return stats2; }

    public void setStats2(Statistics2 stats2) { this.stats2 = stats2; }

	public Statistics3 getStats3() { return stats3; }

	public void setStats3(Statistics3 stats3) {
		this.stats3 = stats3;
	}

    public boolean isExportToDatabase() {
        return exportToDatabase;
    }

    public void setExportToDatabase(boolean exportToDatabase) {
        this.exportToDatabase = exportToDatabase;
    }

    public boolean isExportToCSV() {
        return exportToCSV;
    }

    public void setExportToCSV(boolean exportToCSV) {
        this.exportToCSV = exportToCSV;
    }

    public boolean isPersistStatistics() {
        return persistStatistics;
    }

    public void setPersistStatistics(boolean persistStatistics) {
        this.persistStatistics = persistStatistics;
    }

    public boolean isCalculateGiniCoefficients() {
        return calculateGiniCoefficients;
    }

    public void setCalculateGiniCoefficients(boolean calculateGiniCoefficients) {
        this.calculateGiniCoefficients = calculateGiniCoefficients;
    }

    public boolean isPersistStatistics2() {
        return persistStatistics2;
    }

    public void setPersistStatistics2(boolean val) {
        persistStatistics2 = val;
    }

	public boolean isPersistStatistics3() {
		return persistStatistics3;
	}

	public void setPersistStatistics3(boolean val) {
		persistStatistics3 = val;
	}

    public void calculateAtRiskOfPoverty() {
        calculateEquivalisedHouseholdDisposableIncome();
    }
}
