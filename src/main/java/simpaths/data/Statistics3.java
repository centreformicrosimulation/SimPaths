package simpaths.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import microsim.data.db.PanelEntityKey;
import microsim.statistics.CrossSection;
import microsim.statistics.IDoubleSource;
import microsim.statistics.functions.MeanArrayFunction;
import simpaths.model.Person;
import simpaths.model.BenefitUnit;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.Indicator;
import simpaths.model.enums.TimeSeriesVariable;

public class Statistics3 {

    @Id
    private PanelEntityKey key = new PanelEntityKey(1L);


    @Column(name = "dhm_mean")
    private double dhm_mean;

    @Column(name = "dhm_case")
    private double dhm_case;

    @Column(name = "equivalisedDisposableIncomeYearly_median")
    private double equivalisedDisposableIncomeYearly_median;

    public void setDhm_mean(double dhm_mean) {
        this.dhm_mean = dhm_mean;
    }

    public void setDhm_case(double dhm_case) {
        this.dhm_case = dhm_case;
    }

    public void setEquivalisedDisposableIncomeYearly_median(double equivalisedDisposableIncomeYearly_median) {
        this.equivalisedDisposableIncomeYearly_median = equivalisedDisposableIncomeYearly_median;
    }

    public void update(SimPathsModel model) {

        CrossSection.Double personsMentalHealthsCS = new CrossSection.Double(model.getPersons(), Person.class, "dhm", true); // Get cross section of simulated individuals and their mental health using the IDoubleSource interface implemented by Person class.
        MeanArrayFunction dhm_mean_f = new MeanArrayFunction(personsMentalHealthsCS); // Create MeanArrayFunction
        dhm_mean_f.applyFunction();
        double dhm_mean_out = dhm_mean_f.getDoubleValue(IDoubleSource.Variables.Default); // Get mean value from the MeanArrayFunction
        setDhm_mean(dhm_mean_out);

    }


}
