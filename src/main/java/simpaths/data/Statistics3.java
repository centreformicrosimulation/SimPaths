package simpaths.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import microsim.data.db.PanelEntityKey;
import microsim.statistics.CrossSection;
import microsim.statistics.IDoubleSource;
import microsim.statistics.functions.CountArrayFunction;
import microsim.statistics.functions.MeanArrayFunction;
import microsim.statistics.functions.PercentileArrayFunction;
import microsim.statistics.functions.SumArrayFunction;
import simpaths.data.filters.AgeGenderCSfilter;
import simpaths.data.filters.EmploymentAgeGenderCSfilter;
import simpaths.data.filters.LabourSupplyAgeGenderCSfilter;
import simpaths.model.Person;
import simpaths.model.BenefitUnit;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Labour;
import simpaths.model.enums.Les_c4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Entity
public class Statistics3 {

    @Id
    private PanelEntityKey key = new PanelEntityKey(1L);

    @Column(name = "gender")
    private String gender;

    // mental health numeric
    @Column(name = "dhm_mean")
    private double dhm_mean;

    @Column(name = "dhm_median")
    private double dhm_median;

    @Column(name = "dhm_p_10")
    private double dhm_p_10;

    @Column(name = "dhm_p_90")
    private double dhm_p_90;

    @Column(name = "dhm_p_25")
    private double dhm_p_25;

    @Column(name = "dhm_p_75")
    private double dhm_p_75;


    // mental health case
    @Column(name = "dhm_case")
    private double dhm_case;


    // eq disposable income
    @Column(name = "equivalisedDisposableIncomeYearly_median")
    private double equivalisedDisposableIncomeYearly_median;

    @Column(name = "equivalisedDisposableIncomeYearly_p_10")
    private double equivalisedDisposableIncomeYearly_p_10;

    @Column(name = "equivalisedDisposableIncomeYearly_p_90")
    private double equivalisedDisposableIncomeYearly_p_90;

    @Column(name = "equivalisedDisposableIncomeYearly_p_25")
    private double equivalisedDisposableIncomeYearly_p_25;

    @Column(name = "equivalisedDisposableIncomeYearly_p_75")
    private double equivalisedDisposableIncomeYearly_p_75;


    // employed numeric
    @Column(name = "employed_mean")
    private double employed_mean;

    @Column(name = "employed_n")
    private int employed_n;


    // at risk of poverty prevalence
    @Column(name = "atRiskOfPoverty_mean")
    private double atRiskOfPoverty_mean;

    // labour supply numeric
    @Column(name = "labour_supply_numeric_median")
    private double labour_supply_numeric_median;

    @Column(name = "labour_supply_numeric_p_10")
    private double labour_supply_numeric_p_10;

    @Column(name = "labour_supply_numeric_p_90")
    private double labour_supply_numeric_p_90;

    @Column(name = "labour_supply_numeric_p_25")
    private double labour_supply_numeric_p_25;

    @Column(name = "labour_supply_numeric_p_75")
    private double labour_supply_numeric_p_75;

    @Column(name = "labour_supply_numeric_mean")
    private double labour_supply_numeric_mean;

    // labour supply categories
    @Column(name = "n_labour_ZERO")
    private int n_labour_ZERO;

    @Column(name = "n_labour_THIRTY")
    private int n_labour_THIRTY;

    @Column(name = "n_labour_FORTY")
    private int n_labour_FORTY;

    @Column(name = "n_labour_TEN")
    private int n_labour_TEN;

    @Column(name = "n_labour_TWENTY")
    private int n_labour_TWENTY;

    //N
    @Column(name = "N")
    private int N;

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setEmployed_mean(double employed_mean) {
        this.employed_mean = employed_mean;
    }

    public void setDhm_mean(double dhm_mean) {
        this.dhm_mean = dhm_mean;
    }

    public void setDhm_case(double dhm_case) {
        this.dhm_case = dhm_case;
    }

    public void setEquivalisedDisposableIncomeYearly_median(double equivalisedDisposableIncomeYearly_median) {
        this.equivalisedDisposableIncomeYearly_median = equivalisedDisposableIncomeYearly_median;
    }

    public void setAtRiskOfPoverty_mean(double atRiskOfPoverty_mean) {
        this.atRiskOfPoverty_mean = atRiskOfPoverty_mean;
    }

    public void setLabour_supply_numeric_median(double labour_supply_numeric_median) {
        this.labour_supply_numeric_median = labour_supply_numeric_median;
    }

    public void setLabour_supply_numeric_p_10(double labour_supply_numeric_p_10) {
        this.labour_supply_numeric_p_10 = labour_supply_numeric_p_10;
    }

    public void setLabour_supply_numeric_p_90(double labour_supply_numeric_p_90) {
        this.labour_supply_numeric_p_90 = labour_supply_numeric_p_90;
    }

    public void setLabour_supply_numeric_p_25(double labour_supply_numeric_p_25) {
        this.labour_supply_numeric_p_25 = labour_supply_numeric_p_25;
    }

    public void setLabour_supply_numeric_p_75(double labour_supply_numeric_p_75) {
        this.labour_supply_numeric_p_75 = labour_supply_numeric_p_75;
    }

    public void setLabour_supply_numeric_mean(double labour_supply_numeric_mean) {
        this.labour_supply_numeric_mean = labour_supply_numeric_mean;
    }

    public void setEquivalisedDisposableIncomeYearly_p_10(double equivalisedDisposableIncomeYearly_p_10) {
        this.equivalisedDisposableIncomeYearly_p_10 = equivalisedDisposableIncomeYearly_p_10;
    }

    public void setEquivalisedDisposableIncomeYearly_p_90(double equivalisedDisposableIncomeYearly_p_90) {
        this.equivalisedDisposableIncomeYearly_p_90 = equivalisedDisposableIncomeYearly_p_90;
    }

    public void setEquivalisedDisposableIncomeYearly_p_25(double equivalisedDisposableIncomeYearly_p_25) {
        this.equivalisedDisposableIncomeYearly_p_25 = equivalisedDisposableIncomeYearly_p_25;
    }

    public void setEquivalisedDisposableIncomeYearly_p_75(double equivalisedDisposableIncomeYearly_p_75) {
        this.equivalisedDisposableIncomeYearly_p_75 = equivalisedDisposableIncomeYearly_p_75;
    }

    public void setDhm_median(double dhm_median) {
        this.dhm_median = dhm_median;
    }

    public void setDhm_p_10(double dhm_p_10) {
        this.dhm_p_10 = dhm_p_10;
    }

    public void setDhm_p_90(double dhm_p_90) {
        this.dhm_p_90 = dhm_p_90;
    }

    public void setDhm_p_25(double dhm_p_25) {
        this.dhm_p_25 = dhm_p_25;
    }

    public void setDhm_p_75(double dhm_p_75) {
        this.dhm_p_75 = dhm_p_75;
    }

    public void setEmployed_n(int employed_n) {
        this.employed_n = employed_n;
    }

    public void setN_labour_ZERO(int n_labour_ZERO) {
        this.n_labour_ZERO = n_labour_ZERO;
    }

    public void setN_labour_TEN(int n_labour_TEN) {
        this.n_labour_TEN = n_labour_TEN;
    }

    public void setN_labour_TWENTY(int n_labour_TWENTY) {
        this.n_labour_TWENTY = n_labour_TWENTY;
    }

    public void setN_labour_THIRTY(int n_labour_THIRTY) {
        this.n_labour_THIRTY = n_labour_THIRTY;
    }

    public void setN_labour_FORTY(int n_labour_FORTY) {
        this.n_labour_FORTY = n_labour_FORTY;
    }

    public void setN(int n) {
        N = n;
    }

    public void update(SimPathsModel model, String gender_s) {

//        AgeGroupCSfilter ageFilter = new AgeGroupCSfilter(18, 65);
//        GenderCSfilter genderCSfilter = new GenderCSfilter(Gender.Female);
        AgeGenderCSfilter ageGenderFilter = new AgeGenderCSfilter(18, 65);
        EmploymentAgeGenderCSfilter employmentAgeGenderCSfilter = new EmploymentAgeGenderCSfilter(16, 65, Les_c4.EmployedOrSelfEmployed);


        if (gender_s != "Total") {
            Gender gender = (gender_s == "Female")? Gender.Female: Gender.Male;
            ageGenderFilter = new AgeGenderCSfilter(18, 65, gender);
            employmentAgeGenderCSfilter = new EmploymentAgeGenderCSfilter(16, 65, Les_c4.EmployedOrSelfEmployed, gender);
        }

        // set gender
        setGender(gender_s);

        // dhm score
        CrossSection.Double personsDhm = new CrossSection.Double(model.getPersons(), Person.DoublesVariables.Dhm); // Get cross section of simulated individuals and their mental health using the IDoubleSource interface implemented by Person class.
        personsDhm.setFilter(ageGenderFilter);


        MeanArrayFunction dhm_mean_f = new MeanArrayFunction(personsDhm); // Create MeanArrayFunction
        dhm_mean_f.applyFunction();
        setDhm_mean(dhm_mean_f.getDoubleValue(IDoubleSource.Variables.Default));

        PercentileArrayFunction percDhm_f = new PercentileArrayFunction(personsDhm);
        percDhm_f.applyFunction();

        setDhm_p_10(percDhm_f.getDoubleValue(PercentileArrayFunction.Variables.P10));
        setDhm_p_25(percDhm_f.getDoubleValue(PercentileArrayFunction.Variables.P25));
        setDhm_median(percDhm_f.getDoubleValue(PercentileArrayFunction.Variables.P50));
        setDhm_p_75(percDhm_f.getDoubleValue(PercentileArrayFunction.Variables.P75));
        setDhm_p_90(percDhm_f.getDoubleValue(PercentileArrayFunction.Variables.P90));



        // dhm caseness prevalence
        CrossSection.Integer personsDhmCase = new CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isPsychologicallyDistressed);
        personsDhmCase.setFilter(ageGenderFilter);

        MeanArrayFunction dhm_case_f = new MeanArrayFunction(personsDhmCase);
        dhm_case_f.applyFunction();
        setDhm_case(dhm_case_f.getDoubleValue(IDoubleSource.Variables.Default));

        // equivalised disposable income
        CrossSection.Double hhEqIncome = new CrossSection.Double(model.getBenefitUnits(), BenefitUnit.class, "equivalisedDisposableIncomeYearly", false);
        PercentileArrayFunction hhEqIncome_f = new PercentileArrayFunction(hhEqIncome);
        hhEqIncome_f.applyFunction();

        setEquivalisedDisposableIncomeYearly_p_10(hhEqIncome_f.getDoubleValue(PercentileArrayFunction.Variables.P10));
        setEquivalisedDisposableIncomeYearly_p_25(hhEqIncome_f.getDoubleValue(PercentileArrayFunction.Variables.P25));
        setEquivalisedDisposableIncomeYearly_median(hhEqIncome_f.getDoubleValue(PercentileArrayFunction.Variables.P50));
        setEquivalisedDisposableIncomeYearly_p_75(hhEqIncome_f.getDoubleValue(PercentileArrayFunction.Variables.P75));
        setEquivalisedDisposableIncomeYearly_p_90(hhEqIncome_f.getDoubleValue(PercentileArrayFunction.Variables.P90));

        // Employed prevalence
        CrossSection.Integer personsEmployed = new CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
        personsEmployed.setFilter(ageGenderFilter);

        MeanArrayFunction isEmployed_f = new MeanArrayFunction(personsEmployed);
        isEmployed_f.applyFunction();
        setEmployed_mean(isEmployed_f.getDoubleValue(IDoubleSource.Variables.Default));

        // at risk of poverty
        CrossSection.Integer hhAtRiskPoverty = new CrossSection.Integer(model.getBenefitUnits(), BenefitUnit.class, "getAtRiskOfPoverty", true);
        MeanArrayFunction isAtRiskPoverty = new MeanArrayFunction(hhAtRiskPoverty);
        isAtRiskPoverty.applyFunction();
        setAtRiskOfPoverty_mean(isAtRiskPoverty.getDoubleValue(IDoubleSource.Variables.Default));

        // labour supply numeric
        CrossSection.Double personLabourSupplyNumeric = new CrossSection.Double(model.getPersons(), Person.class, "getDoubleLabourSupplyWeeklyHours", true);
        personLabourSupplyNumeric.setFilter(ageGenderFilter);

        MeanArrayFunction meanLabourSupplyNumeric = new MeanArrayFunction(personLabourSupplyNumeric);
        meanLabourSupplyNumeric.applyFunction();

        PercentileArrayFunction percLabourSupplyNumeric = new PercentileArrayFunction(personLabourSupplyNumeric);
        percLabourSupplyNumeric.applyFunction();

        setLabour_supply_numeric_mean(meanLabourSupplyNumeric.getDoubleValue(IDoubleSource.Variables.Default));
        setLabour_supply_numeric_p_10(percLabourSupplyNumeric.getDoubleValue(PercentileArrayFunction.Variables.P10));
        setLabour_supply_numeric_p_25(percLabourSupplyNumeric.getDoubleValue(PercentileArrayFunction.Variables.P25));
        setLabour_supply_numeric_median(percLabourSupplyNumeric.getDoubleValue(PercentileArrayFunction.Variables.P50));
        setLabour_supply_numeric_p_75(percLabourSupplyNumeric.getDoubleValue(PercentileArrayFunction.Variables.P75));
        setLabour_supply_numeric_p_90(percLabourSupplyNumeric.getDoubleValue(PercentileArrayFunction.Variables.P95));

        // labour supply categories
        Map<Labour, SumArrayFunction.Integer> labour_fs = new HashMap<>();
        CrossSection.Integer n_labour;
        LabourSupplyAgeGenderCSfilter labour_filter;
        SumArrayFunction.Integer labour_f;

        for (Labour labour: Labour.values()) {
             labour_filter = new LabourSupplyAgeGenderCSfilter(18, 65, labour);
             n_labour = new CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);

            if (gender_s != "Total") {
                Gender gender = (gender_s == "Female")? Gender.Female: Gender.Male;
                labour_filter = new LabourSupplyAgeGenderCSfilter(18, 65, labour, gender);
            }

            n_labour.setFilter(labour_filter);
            labour_f = new SumArrayFunction.Integer(n_labour);
            labour_f.applyFunction();

            labour_fs.put(labour, labour_f);

        }

        setN_labour_ZERO(labour_fs.get(Labour.ZERO).getIntValue(IDoubleSource.Variables.Default));
        setN_labour_TEN(labour_fs.get(Labour.TEN).getIntValue(IDoubleSource.Variables.Default));
        setN_labour_TWENTY(labour_fs.get(Labour.TWENTY).getIntValue(IDoubleSource.Variables.Default));
        setN_labour_THIRTY(labour_fs.get(Labour.THIRTY).getIntValue(IDoubleSource.Variables.Default));
        setN_labour_FORTY(labour_fs.get(Labour.FORTY).getIntValue(IDoubleSource.Variables.Default));


        // employed count
        CrossSection.Integer n_emp = new CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
        n_emp.setFilter(employmentAgeGenderCSfilter);

        SumArrayFunction.Integer emp_f = new SumArrayFunction.Integer(n_emp);
        emp_f.applyFunction();
        setEmployed_n(emp_f.getIntValue(IDoubleSource.Variables.Default));

        // count
        CrossSection.Integer n_persons = new CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
        n_persons.setFilter(ageGenderFilter);

        SumArrayFunction.Integer count_f = new SumArrayFunction.Integer(n_persons);
        count_f.applyFunction();
        setN(count_f.getIntValue(IDoubleSource.Variables.Default));

    }


}
