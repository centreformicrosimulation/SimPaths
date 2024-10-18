package simpaths.model;

import java.util.*;
import java.util.random.RandomGenerator;

import simpaths.data.statistics.Statistics;
import simpaths.experiment.SimPathsCollector;
import simpaths.model.enums.*;

import simpaths.data.Parameters;
//import experiment.SimPathsObserver;
//import microsim.data.MultiKeyCoefficientMap;
import microsim.engine.SimulationEngine;
//import microsim.statistics.IDoubleSource;
//import microsim.statistics.ILongSource;


//Significantly simplified LabourMarket module. Should just contain simple labour supply for now and matching with euromod donor population.
public class LabourMarket {

    private final SimPathsModel model;

//	private final SimPathsObserver observer; //Not used but maybe we want to graph labour supply and earnings etc. in the observer

    private String EUROMODpolicyNameForThisYear;

    private LinkedHashMap<Region, Set<BenefitUnit>> benefitUnitsByRegion; //Don't think we care about regions, but maybe benefitUnits are matched by region?

    private Set<BenefitUnit> benefitUnitsAllRegions;

    private Set<BenefitUnit> benefitUnitsCovid19Update;

    private Map<Region, Double> disposableIncomesByRegion;

    Set<BenefitUnit> benefitUnits;

    private int covid19TransitionsMonth;

    RandomGenerator labourInnov;


    //Constructor:
    LabourMarket(Set<BenefitUnit> benefitUnits) {

        model = (SimPathsModel) SimulationEngine.getInstance().getManager(SimPathsModel.class.getCanonicalName());
//		observer = (SimPathsObserver) SimulationEngine.getInstance().getManager(SimPathsObserver.class.getCanonicalName());	//To allow intra-time-step updates of convergence plots
        this.benefitUnits = benefitUnits;
        EUROMODpolicyNameForThisYear = Parameters.getEUROMODpolicyForThisYear(model.getYear());

        benefitUnitsByRegion = new LinkedHashMap<Region, Set<BenefitUnit>>();		//For use in labour market module
        benefitUnitsAllRegions = new LinkedHashSet<>();
        benefitUnitsCovid19Update = new LinkedHashSet<>();

        for(Region region : Parameters.getCountryRegions()) {
            benefitUnitsByRegion.put(region, new LinkedHashSet<BenefitUnit>());
        }

        labourInnov = new Random(SimulationEngine.getRnd().nextLong());
    }


    protected void update(int year) {

        /*
        In Covid-19 years (2020, 2021) the update method uses reduced form models, if the labourMarketCovid19On boolean is set to true in the SimPathsModel class
        Need to model wages and hours of work (as the other labour supply module would do), calculate disposable income by matching with monthly EM files, and then proceed with the rest of the model
        */

        if (model.isLabourMarketCovid19On() && year >= 2020 && year <= 2021) { // If true, apply the reduced form models

            if (year == 2020) { // In 2020, start in April
                setCovid19TransitionsMonth(4);
            } else { // Otherwise start in January
                setCovid19TransitionsMonth(1);
            }

            // Clear set of benefit units to update
            benefitUnitsCovid19Update.clear();

            for (BenefitUnit benefitUnit : benefitUnits) {
                benefitUnit.updateNonLabourIncome();
                if (benefitUnit.getAtRiskOfWork()) {
                    benefitUnitsCovid19Update.add(benefitUnit); // Put benefit units at risk of work in a set to update. Could use the same set as structural model, but seems cleaner to keep the two separate
                } else {
                    benefitUnit.updateDisposableIncomeIfNotAtRiskOfWork();
                }
            }

            for (BenefitUnit benefitUnit : benefitUnitsCovid19Update) {

                // Clear objects used in the Covid-19 module at the beginning of the year:
                benefitUnit.covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.clear(); // Clear ArrayList storing monthly outcomes for male member of the benefit unit (labour market states and gross incomes)
                benefitUnit.covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.clear(); // Clear ArrayList storing monthly outcomes for female member of the benefit unit (labour market states and gross incomes)

                // Update fields of benefit units entering Covid-19 module:
                benefitUnit.resetLabourStates(); // Update potential earnings which are then used to calculate gross income in the Covid-19 module

                // TODO: At the end of the year, having saved monthly states and corresponding gross incomes, sample one at random that will be the "yearly" value. This recreates the way in which FRS data underlying EUROMOD is sampled.
                /*
                Prepare simulated objects when the Covid-19 module runs for the first time (first month of the first year - April 2020)
                In the simulation, there is no distinction between employed and self-employed individuals.
                In the first month of the first Covid-19 year, we use a new process (RegC19LS_SE) to determine which individuals, out of those in EmployedOrSelfEmployed category, are self-employed and assign that status in Les_c7_covid enum.
                The code below also copies values of les_c4 to assign starting values of les_c6.
                 */

                LinkedHashSet<Person> personsInBenefitUnit = new LinkedHashSet<>(); // Store adults from the benefit unit
                Occupancy occupancy = benefitUnit.getOccupancy();
                if ((year == 2020 && getCovid19TransitionsMonth() == 4)) {

                    if (occupancy.equals(Occupancy.Couple)) {
                        Person male = benefitUnit.getMale();
                        Person female = benefitUnit.getFemale();
                        personsInBenefitUnit.add(male);
                        personsInBenefitUnit.add(female);
                        // For male, female set initial value of les_c6 based on current value of les_c4
                        male.initialise_les_c6_from_c4();
                        female.initialise_les_c6_from_c4();

                        boolean setMaleSelfEmployed = (labourInnov.nextDouble() < Parameters.getRegC19LS_SE().getProbability(male, Person.DoublesVariables.class));
                        boolean setFemaleSelfEmployed = (labourInnov.nextDouble() < Parameters.getRegC19LS_SE().getProbability(female, Person.DoublesVariables.class));
                        if (setMaleSelfEmployed && male.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed)) {
                            male.setLes_c7_covid(Les_c7_covid.SelfEmployed);
                        }
                        if (setFemaleSelfEmployed && female.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed)) {
                            female.setLes_c7_covid(Les_c7_covid.SelfEmployed);
                        }
                    } else if (occupancy.equals(Occupancy.Single_Male)) {
                        Person male = benefitUnit.getMale();
                        male.initialise_les_c6_from_c4();
                        personsInBenefitUnit.add(male);
                        boolean setSelfEmployed = (labourInnov.nextDouble() < Parameters.getRegC19LS_SE().getProbability(male, Person.DoublesVariables.class));
                        if (setSelfEmployed && male.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed)) {
                            male.setLes_c7_covid(Les_c7_covid.SelfEmployed);
                        }
                    } else if (occupancy.equals(Occupancy.Single_Female)) {
                        Person female = benefitUnit.getFemale();
                        female.initialise_les_c6_from_c4();
                        personsInBenefitUnit.add(female);
                        boolean setSelfEmployed = (labourInnov.nextDouble() < Parameters.getRegC19LS_SE().getProbability(female, Person.DoublesVariables.class));
                        if (setSelfEmployed && female.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed)) {
                            female.setLes_c7_covid(Les_c7_covid.SelfEmployed);
                        }
                    } else {
                        throw new RuntimeException("Warning: Occupancy unknown in benefit unit " + benefitUnit.getKey().getId());
                    }
                    updateGrossLabourIncomeBaseline_Xt5(personsInBenefitUnit);
                }

                // Households are created and destroyed each year, so there might be some in 2021 who were not in the simulation in 2020 and need to be initialised
                if (year == 2021 && getCovid19TransitionsMonth() == 1) {

                    if (occupancy.equals(Occupancy.Couple)) {
                        Person male = benefitUnit.getMale();
                        Person female = benefitUnit.getFemale();
                        personsInBenefitUnit.add(male);
                        personsInBenefitUnit.add(female);
                        if (male != null && male.getLes_c7_covid() == null) {
                            male.initialise_les_c6_from_c4();
                            boolean setMaleSelfEmployed = (labourInnov.nextDouble() < Parameters.getRegC19LS_SE().getProbability(male, Person.DoublesVariables.class));
                            if (setMaleSelfEmployed && male.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed)) {
                                male.setLes_c7_covid(Les_c7_covid.SelfEmployed);
                            }
                        }
                        if (female != null && female.getLes_c7_covid() == null) {
                            female.initialise_les_c6_from_c4();
                            boolean setFemaleSelfEmployed = (labourInnov.nextDouble() < Parameters.getRegC19LS_SE().getProbability(female, Person.DoublesVariables.class));
                            if (setFemaleSelfEmployed && female.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed)) {
                                female.setLes_c7_covid(Les_c7_covid.SelfEmployed);
                            }
                        }
                    }  else if (occupancy.equals(Occupancy.Single_Male)) {
                        Person male = benefitUnit.getMale();
                        personsInBenefitUnit.add(male);
                        if (male != null && male.getLes_c7_covid() == null) {
                            male.initialise_les_c6_from_c4();
                            boolean setSelfEmployed = (labourInnov.nextDouble() < Parameters.getRegC19LS_SE().getProbability(male, Person.DoublesVariables.class));
                            if (setSelfEmployed && male.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed)) {
                                male.setLes_c7_covid(Les_c7_covid.SelfEmployed);
                            }
                        }
                    } else if (occupancy.equals(Occupancy.Single_Female)) {
                        Person female = benefitUnit.getFemale();
                        personsInBenefitUnit.add(female);
                        if (female != null && female.getLes_c7_covid() == null) {
                            female.initialise_les_c6_from_c4();
                            boolean setSelfEmployed = (labourInnov.nextDouble() < Parameters.getRegC19LS_SE().getProbability(female, Person.DoublesVariables.class));
                            if (setSelfEmployed && female.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed)) {
                                female.setLes_c7_covid(Les_c7_covid.SelfEmployed);
                            }
                        }
                    }
                    updateGrossLabourIncomeBaseline_Xt5(personsInBenefitUnit);
                }
            }

            // Monthly Covid-19 updates below
            for (int currentMonth = getCovid19TransitionsMonth(); currentMonth <= 12; currentMonth++) {

                /*
                In the reduced form the transitions depend on regressions. Different regression is applied for different past states: employed, self-employed, not-employed, furloughed.
                We store all monthly values, and at the end of the year choose one value at random to replicate sampling design of FRS and EUROMOD. But labour market states and incomes have to be updated from month to month first, as next month's state depends on the previous month.
                updateMonthlyLabourSupplyCovid19() method predicts monthly transition, work hours, gross and disposable income and adds such triplet to an array list keeping track of monthly values.
                 */
                for (BenefitUnit benefitUnit : benefitUnitsCovid19Update) {

                    benefitUnit.updateMonthlyLabourSupplyCovid19();
                }
                incrementCovid19TransitionsMonth(1);
            }

            // When all the monthly transitions in a year have been predicted, choose one monthly value to represent the whole year for each individual and set labour force status, work hours, gross and disposable income.
        //	benefitUnitsCovid19Update.parallelStream().forEach(benefitUnit -> benefitUnit.chooseRandomMonthlyOutcomeCovid19());
            for (BenefitUnit benefitUnit : benefitUnitsCovid19Update) {
                benefitUnit.chooseRandomMonthlyOutcomeCovid19();
            }

        } else {
            // Otherwise, use the default model of labour supply

            EUROMODpolicyNameForThisYear = Parameters.getEUROMODpolicyForThisYear(year);    //Update EUROMOD policy to apply to this year

            for (Region region : Parameters.getCountryRegions()) {
                benefitUnitsByRegion.get(region).clear();
            }
            benefitUnitsAllRegions.clear();
            for (BenefitUnit benefitUnit : benefitUnits) {

                if (benefitUnit.getAtRiskOfWork()) { //Update HHs at risk of work, i.e. either of the adults are not under age, retired, student, in bad health
                    benefitUnitsByRegion.get(benefitUnit.getRegion()).add(benefitUnit);        //This is the collection of benefitUnits that will enter the labour market
                    benefitUnitsAllRegions.add(benefitUnit);
                } else {
                    benefitUnit.updateNonLabourIncome();
                    benefitUnit.updateDisposableIncomeIfNotAtRiskOfWork();
                }
            }

            if (model.isAlignEmployment() & model.getYear() <= 2019) {
                model.activityAlignmentSingleMales();
                model.activityAlignmentSingleFemales();
                model.activityAlignmentCouples();
            }

            //Update Labour Supply
            benefitUnitsAllRegions.parallelStream()
                    .forEach(BenefitUnit::updateLabourSupplyAndIncome);

            Map<Education, Double> potentialHourlyEarningsByEdu = new LinkedHashMap<Education, Double>();
            Map<Education, Integer> countByEdu = new LinkedHashMap<Education, Integer>();
            for (Education ed : Education.values()) {

                potentialHourlyEarningsByEdu.put(ed, 0.);
                countByEdu.put(ed, 0);
            }
            for (BenefitUnit benefitUnit : benefitUnitsAllRegions) {

                if (benefitUnit.getMale() != null) {

                    Person person = benefitUnit.getMale();
                    if (person.atRiskOfWork()) {

                        Education ed = person.getDeh_c3();
                        double newVal = person.getFullTimeHourlyEarningsPotential();
                        potentialHourlyEarningsByEdu.put(ed, potentialHourlyEarningsByEdu.get(ed) + newVal);
                        int oldCount = countByEdu.get(ed);
                        countByEdu.put(ed, oldCount + 1);
                    }
                }
                if (benefitUnit.getFemale() != null) {

                    Person person = benefitUnit.getFemale();
                    if (person.atRiskOfWork()) {

                        Education ed = person.getDeh_c3();
                        double newVal = person.getFullTimeHourlyEarningsPotential();
                        potentialHourlyEarningsByEdu.put(ed, potentialHourlyEarningsByEdu.get(ed) + newVal);
                        int oldCount = countByEdu.get(ed);
                        countByEdu.put(ed, oldCount + 1);
                    }
                }
            }

            // Update activity status of persons residing within the benefit unit
            benefitUnits.stream()
                    .forEach(BenefitUnit::updateActivityOfPersonsWithinBenefitUnit);

        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////
    //
    //	Other Methods
    //
    ///////////////////////////////////////////////////////////////////////////////////////

    public void updateGrossLabourIncomeBaseline_Xt5(LinkedHashSet<Person> personsInBenefitUnit) {
        for (Person person : personsInBenefitUnit) {
            if (person != null && person.getCovidModuleGrossLabourIncomeBaseline_Xt5() == null) {
                double covidModuleGrossLabourIncomeBaseline = person.getCovidModuleGrossLabourIncome_Baseline();
                Statistics stats = ((SimPathsCollector) SimulationEngine.getInstance().getManager(SimPathsCollector.class.getCanonicalName())).getStats();
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

    public int getMonthForRegressor() {
        int year = model.getYear();
        int month = getCovid19TransitionsMonth();
        int returnValue = 0;

        if (year == 2020) {
            if (month == 4) {
                returnValue = 1;
            } else if (month == 5) {
                returnValue = 2;
            } else if (month == 6) {
                returnValue = 3;
            } else if (month == 7 || month == 8) {
                returnValue = 4;
            } else if (month == 9 || month == 10) {
                returnValue = 5;
            } else if (month == 11 || month == 12) {
                returnValue = 6;
            }
        } else if (year == 2021) {
            if (month == 1 || month == 2) {
                returnValue = 7;
            } else if (month == 3 || month == 4) {
                returnValue = 8;
            } else if (month >= 5) {
                returnValue = 9;
            }
        } else if (year > 2021) {
            returnValue = 9; //Keep month equal to 9 for dates later than the last month observed in the data
        }
        return returnValue;
    }


    //-------------------------------------------------------------------------
    //	Access Methods
    //-------------------------------------------------------------------------


    public String getEUROMODpolicyNameForThisYear() {
        return EUROMODpolicyNameForThisYear;
    }

    public int getCovid19TransitionsMonth() {
        return covid19TransitionsMonth;
    }

    public void setCovid19TransitionsMonth(int covid19TransitionsMonth) {
        this.covid19TransitionsMonth = covid19TransitionsMonth;
    }

    public void incrementCovid19TransitionsMonth(int increment) {
        covid19TransitionsMonth = covid19TransitionsMonth+increment;
    }
}
