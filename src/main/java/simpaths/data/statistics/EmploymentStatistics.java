package simpaths.data.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;

import microsim.data.db.PanelEntityKey;
import microsim.statistics.CrossSection;
import simpaths.data.Parameters;
import simpaths.data.filters.*;
import simpaths.experiment.SimPathsCollector;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Les_c4;
import simpaths.model.Person;

import static simpaths.model.BenefitUnit.Regressors.UC_TakeUp;
import static simpaths.model.Person.DoublesVariables.*;

@Entity
public class EmploymentStatistics extends StatisticsHelper {

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

    @Column(name = "PropNotEmployed")
    private double PropNotEmployed;

    @Column(name = "PropRetired")
    private double PropRetired;

    @Column(name = "PropStudent")
    private double PropStudent;

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

    public double getPropNotEmployed() {
        return PropNotEmployed;
    }

    public void setPropNotEmployed(double propNotEmployed) {
        PropNotEmployed = propNotEmployed;
    }

    public void setPropRetired(double propRetired) {
        PropRetired = propRetired;
    }

    public void setPropStudent(double propStudent) {
        PropStudent = propStudent;
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

        calculateAndSetMean(personsEmpToNotEmp, this::setEmpToNotEmp);
        calculateAndSetMean(personsNotEmpToEmp, this::setNotEmpToEmp);

        // Employed and unemployed in age-groups
        CrossSection.Integer personsEmployed = new CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isEmployed);
        CrossSection.Integer personsUnemployed = new CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isNotEmployed);
        CrossSection.Integer personsRetired = new CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isRetired);
        CrossSection.Integer personsStudent = new CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isStudent);

        personsEmployed.setFilter(ageGenderCSfilter);
        personsUnemployed.setFilter(ageGenderCSfilter);
        personsRetired.setFilter(ageGenderCSfilter);
        personsStudent.setFilter(ageGenderCSfilter);

        calculateAndSetMean(personsEmployed, this::setPropEmployed);
        calculateAndSetMean(personsUnemployed, this::setPropNotEmployed);
        calculateAndSetMean(personsRetired, this::setPropRetired);
        calculateAndSetMean(personsStudent, this::setPropStudent);


//        CrossSection.Integer benefitUnitsUCTakeup = new CrossSection.Integer(model.getBenefitUnits(), BenefitUnit.class, "getUC_takeup", true);
        CrossSection.Double benefitUnitsUCTakeup = new CrossSection.Double(model.getBenefitUnits(), UC_TakeUp);
        // Mean hours worked amongst employed
        CrossSection.Double hoursWorked = new CrossSection.Double(model.getPersons(), Person.class, "getHoursWorkedWeekly", true);
        hoursWorked.setFilter(employmentCSfilter);

        calculateAndSetMean(benefitUnitsUCTakeup, this::setPropUCTakeup);

        CrossSection.Double personsReceivedUC = new CrossSection.Double(model.getPersons(), Person.class, "isReceivesBenefitsUCDouble", true);
        CrossSection.Double personsReceivedLegacyBenefits = new CrossSection.Double(model.getPersons(), Person.class, "isReceivesBenefitsLBDouble", true);

        personsReceivedUC.setFilter(ageGenderCSfilter);
        personsReceivedLegacyBenefits.setFilter(ageGenderCSfilter);

        calculateAndSetMean(personsReceivedUC, this::setPropReceivedUC);
        calculateAndSetMean(personsReceivedLegacyBenefits, this::setPropReceivedLegacyBenefits);


        calculateAndSetMean(hoursWorked, this::setMeanLabourHours);



        // count
        CrossSection.Integer n_persons = new CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
        n_persons.setFilter(ageGenderCSfilter);
        calculateAndSetCount(n_persons, this::setN);


    }
}
