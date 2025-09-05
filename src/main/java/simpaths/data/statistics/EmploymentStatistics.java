package simpaths.data.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;

import microsim.data.db.PanelEntityKey;
import microsim.statistics.CrossSection;
import microsim.statistics.IDoubleSource;
import microsim.statistics.functions.MeanArrayFunction;
import simpaths.data.filters.AgeGroupCSfilter;
import simpaths.data.filters.EmploymentHistoryFilter;
import simpaths.model.BenefitUnit;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.Les_c4;
import simpaths.model.Person;

import static simpaths.model.BenefitUnit.Regressors.UC_TakeUp;
import static simpaths.model.Person.DoublesVariables.*;

@Entity
public class EmploymentStatistics {

    @Id
    private PanelEntityKey key = new PanelEntityKey(1L);

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

    @Column(name = "PropReceivedLegacyBenefits")
    private double PropReceivedLegacyBenefits;

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



    public void update(SimPathsModel model) {

        EmploymentHistoryFilter employmentHistoryEmployed = new EmploymentHistoryFilter(Les_c4.EmployedOrSelfEmployed);
        EmploymentHistoryFilter employmentHistoryUnemployed = new EmploymentHistoryFilter(Les_c4.NotEmployed);


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

        // Employment and unemployment, working age adults 16-64
        AgeGroupCSfilter ageGroupCSfilter = new AgeGroupCSfilter(16, 64);

        CrossSection.Integer personsEmployed = new CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
        CrossSection.Integer personsUnemployed = new CrossSection.Integer(model.getPersons(), Person.class, "getNonwork", true);

        personsEmployed.setFilter(ageGroupCSfilter);
        personsUnemployed.setFilter(ageGroupCSfilter);

        MeanArrayFunction isEmployed = new MeanArrayFunction(personsEmployed);
        isEmployed.applyFunction();
        setPropEmployed(isEmployed.getDoubleValue(IDoubleSource.Variables.Default));

        MeanArrayFunction isUnemployed = new MeanArrayFunction(personsUnemployed);
        isUnemployed.applyFunction();
        setPropUnemployed(isUnemployed.getDoubleValue(IDoubleSource.Variables.Default));

//        CrossSection.Integer benefitUnitsUCTakeup = new CrossSection.Integer(model.getBenefitUnits(), BenefitUnit.class, "getUC_takeup", true);
        CrossSection.Double benefitUnitsUCTakeup = new CrossSection.Double(model.getBenefitUnits(), UC_TakeUp);

        MeanArrayFunction isUCTakeup = new MeanArrayFunction(benefitUnitsUCTakeup);
        isUCTakeup.updateSource();
        setPropUCTakeup(isUCTakeup.getDoubleValue(IDoubleSource.Variables.Default));

        CrossSection.Double personsReceivedUC = new CrossSection.Double(model.getPersons(), D_Econ_benefits_UC);
        CrossSection.Double personsReceivedLegacyBenefits = new CrossSection.Double(model.getPersons(), D_Econ_benefits_NonUC);

        personsReceivedUC.setFilter(ageGroupCSfilter);
        personsReceivedLegacyBenefits.setFilter(ageGroupCSfilter);

        MeanArrayFunction isReceivedUC = new MeanArrayFunction(personsReceivedUC);
        isReceivedUC.applyFunction();
        setPropReceivedUC(isReceivedUC.getDoubleValue(IDoubleSource.Variables.Default));
        MeanArrayFunction isReceivedLegacyBenefits = new MeanArrayFunction(personsReceivedLegacyBenefits);
        isReceivedLegacyBenefits.applyFunction();
        setPropReceivedLegacyBenefits(isReceivedLegacyBenefits.getDoubleValue(IDoubleSource.Variables.Default));


    }
}
