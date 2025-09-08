package simpaths.data.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;

import jakarta.persistence.Transient;
import microsim.data.db.PanelEntityKey;
import microsim.statistics.CrossSection;
import microsim.statistics.IDoubleSource;
import microsim.statistics.functions.MeanArrayFunction;
import microsim.statistics.functions.SumArrayFunction;
import simpaths.data.Parameters;
import simpaths.data.filters.*;
import simpaths.experiment.SimPathsCollector;
import simpaths.model.BenefitUnit;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Les_c4;
import simpaths.model.Person;

import static simpaths.model.BenefitUnit.Regressors.UC_TakeUp;
import static simpaths.model.Person.DoublesVariables.*;

@Entity
public class EmploymentStatistics {

    @Id
    private PanelEntityKey key = new PanelEntityKey(1L);

    @Column(name = "scenario")
    private String scenario = Parameters.scenario;

    @Column(name = "gender")
    private String gender;

    @Column(name = "agegroup")
    private String agegroup;

    @Column(name= "EmpToNotEmp")
    private double EmpToNotEmp;         // Proportion of employed people becoming unemployed

    @Column(name= "NotEmpToEmp")
    private double NotEmpToEmp;         // Proportion of unemployed people becoming employed

    @Column(name = "PropEmployed")
    private double PropEmployed;

    @Column(name = "PropUnemployed")
    private double PropUnemployed;
    
    @Column(name = "PropUCTakeup")
    private double PropUCTakeup;

    @Column(name = "PropReceivedUC")
    private double PropReceivedUC;
    @Column(name = "meanLabourHours")
    private double meanLabourHours;

    @Column(name = "PropReceivedLegacyBenefits")
    private double PropReceivedLegacyBenefits;

    //N
    @Column(name = "N")
    private int N;

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setAgegroup(SimPathsCollector.AgeRange agegroup) {
        String agegroup_s = agegroup.toString();
        this.agegroup = agegroup_s;
    }


    public double getEmpToNotEmp() {
        return EmpToNotEmp;
    }

    public void setEmpToNotEmp(double empToNotEmp) {
        EmpToNotEmp = empToNotEmp;
    }

    public double getNotEmpToEmp() {
        return NotEmpToEmp;
    }

    public void setNotEmpToEmp(double notEmpToEmp) {
        NotEmpToEmp = notEmpToEmp;
    }

    public double getPropEmployed() {
        return PropEmployed;
    }

    public void setPropEmployed(double propEmployed) {
        PropEmployed = propEmployed;
    }

    public double getPropUnemployed() {
        return PropUnemployed;
    }

    public void setPropUnemployed(double propUnemployed) {
        PropUnemployed = propUnemployed;
    }

    public void setPropUCTakeup(double propUCTakeup) {
        PropUCTakeup = propUCTakeup;
    }

    public void setPropReceivedUC(double propReceivedUC) {
        PropReceivedUC = propReceivedUC;
    }

    public void setPropReceivedLegacyBenefits(double propReceivedLegacyBenefits) {
        PropReceivedLegacyBenefits = propReceivedLegacyBenefits;
    }

    public void setMeanLabourHours(double meanLabourHours) {
        this.meanLabourHours = meanLabourHours;
    }

    public void setKey(PanelEntityKey key) {
        this.key = key;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public void setN(int n) {
        N = n;
    }

    public EmploymentStatistics(PanelEntityKey key) {
        super();
        this.setKey(key);
    }

    public void update(SimPathsModel model, String gender_s, SimPathsCollector.AgeRange ageRange) {

        AgeGenderCSfilter ageGenderCSfilter;
        EmploymentAgeGenderCSfilter employmentCSfilter;
        EmploymentHistoryFilter employmentHistoryEmployed;
        EmploymentHistoryFilter employmentHistoryUnemployed;

        if (gender_s.equals("Total")) {
            ageGenderCSfilter = new AgeGenderCSfilter(ageRange.lowerBound(), ageRange.upperBound());
            employmentCSfilter = new EmploymentAgeGenderCSfilter(Les_c4.EmployedOrSelfEmployed, ageRange.lowerBound(), ageRange.upperBound());

            employmentHistoryEmployed = new EmploymentHistoryFilter(Les_c4.EmployedOrSelfEmployed, ageRange.lowerBound(), ageRange.upperBound());
            employmentHistoryUnemployed = new EmploymentHistoryFilter(Les_c4.NotEmployed, ageRange.lowerBound(), ageRange.upperBound());
        } else {
            ageGenderCSfilter = new AgeGenderCSfilter(ageRange.lowerBound(), ageRange.upperBound(), Gender.valueOf(gender_s));
            employmentCSfilter = new EmploymentAgeGenderCSfilter(Les_c4.EmployedOrSelfEmployed, ageRange.lowerBound(), ageRange.upperBound(), Gender.valueOf(gender_s));

            employmentHistoryEmployed = new EmploymentHistoryFilter(Les_c4.EmployedOrSelfEmployed, ageRange.lowerBound(), ageRange.upperBound(), Gender.valueOf(gender_s));
            employmentHistoryUnemployed = new EmploymentHistoryFilter(Les_c4.NotEmployed, ageRange.lowerBound(), ageRange.upperBound(), Gender.valueOf(gender_s));
        }

        // set gender
        setGender(gender_s);

        // set agegroup
        setAgegroup(ageRange);



        // Entering employment transition rate
        CrossSection.Integer personsNotEmpToEmp = new CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
        personsNotEmpToEmp.setFilter(employmentHistoryUnemployed);
        // Entering not employed transition rate
        CrossSection.Integer personsEmpToNotEmp = new CrossSection.Integer(model.getPersons(), Person.class, "getNonwork", true);
        personsEmpToNotEmp.setFilter(employmentHistoryEmployed);


        MeanArrayFunction isNotEmpToEmp = new MeanArrayFunction(personsNotEmpToEmp);
        isNotEmpToEmp.applyFunction();
        setNotEmpToEmp(isNotEmpToEmp.getDoubleValue(IDoubleSource.Variables.Default));

        MeanArrayFunction isEmpToNotEmp = new MeanArrayFunction(personsEmpToNotEmp);
        isEmpToNotEmp.applyFunction();
        setEmpToNotEmp(isEmpToNotEmp.getDoubleValue(IDoubleSource.Variables.Default));

        // Employed and unemployed in age-groups
        CrossSection.Integer personsEmployed = new CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
        CrossSection.Integer personsUnemployed = new CrossSection.Integer(model.getPersons(), Person.class, "getNonwork", true);

        personsEmployed.setFilter(ageGenderCSfilter);
        personsUnemployed.setFilter(ageGenderCSfilter);

        MeanArrayFunction isEmployed = new MeanArrayFunction(personsEmployed);
        isEmployed.applyFunction();
        setPropEmployed(isEmployed.getDoubleValue(IDoubleSource.Variables.Default));

        MeanArrayFunction isUnemployed = new MeanArrayFunction(personsUnemployed);
        isUnemployed.applyFunction();
        setPropUnemployed(isUnemployed.getDoubleValue(IDoubleSource.Variables.Default));

//        CrossSection.Integer benefitUnitsUCTakeup = new CrossSection.Integer(model.getBenefitUnits(), BenefitUnit.class, "getUC_takeup", true);
        CrossSection.Double benefitUnitsUCTakeup = new CrossSection.Double(model.getBenefitUnits(), UC_TakeUp);
        // Mean hours worked amongst employed
        CrossSection.Double hoursWorked = new CrossSection.Double(model.getPersons(), Person.class, "getHoursWorkedWeekly", true);
        hoursWorked.setFilter(employmentCSfilter);

        MeanArrayFunction isUCTakeup = new MeanArrayFunction(benefitUnitsUCTakeup);
        isUCTakeup.updateSource();
        setPropUCTakeup(isUCTakeup.getDoubleValue(IDoubleSource.Variables.Default));

        CrossSection.Double personsReceivedUC = new CrossSection.Double(model.getPersons(), D_Econ_benefits_UC);
        CrossSection.Double personsReceivedLegacyBenefits = new CrossSection.Double(model.getPersons(), D_Econ_benefits_LB);

        personsReceivedUC.setFilter(ageGenderCSfilter);
        personsReceivedLegacyBenefits.setFilter(ageGenderCSfilter);

        MeanArrayFunction isReceivedUC = new MeanArrayFunction(personsReceivedUC);
        isReceivedUC.applyFunction();
        setPropReceivedUC(isReceivedUC.getDoubleValue(IDoubleSource.Variables.Default));
        MeanArrayFunction isReceivedLegacyBenefits = new MeanArrayFunction(personsReceivedLegacyBenefits);
        isReceivedLegacyBenefits.applyFunction();
        setPropReceivedLegacyBenefits(isReceivedLegacyBenefits.getDoubleValue(IDoubleSource.Variables.Default));


        MeanArrayFunction meanHoursWorked = new MeanArrayFunction(hoursWorked);
        meanHoursWorked.applyFunction();
        setMeanLabourHours(meanHoursWorked.getDoubleValue(IDoubleSource.Variables.Default));


        // count
        CrossSection.Integer n_persons = new CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
        n_persons.setFilter(ageGenderCSfilter);

        SumArrayFunction.Integer count_f = new SumArrayFunction.Integer(n_persons);
        count_f.applyFunction();
        setN(count_f.getIntValue(IDoubleSource.Variables.Default));

    }
}
