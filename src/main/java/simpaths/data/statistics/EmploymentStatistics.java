package simpaths.data.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;

import microsim.data.db.PanelEntityKey;
import microsim.statistics.CrossSection;
import microsim.statistics.IDoubleSource;
import microsim.statistics.functions.MeanArrayFunction;
import simpaths.data.filters.EmploymentHistoryFilter;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.Les_c4;
import simpaths.model.Person;

@Entity
public class EmploymentStatistics {

    @Id
    private PanelEntityKey key = new PanelEntityKey(1L);

    @Column(name= "EmpToNotEmp")
    private double EmpToNotEmp;         // Proportion of employed people becoming unemployed

    @Column(name= "NotEmpToEmp")
    private double NotEmpToEmp;         // Proportion of unemployed people becoming employed


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



    }
}
