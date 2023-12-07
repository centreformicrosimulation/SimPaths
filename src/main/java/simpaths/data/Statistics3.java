package simpaths.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import microsim.data.db.PanelEntityKey;
import microsim.statistics.CrossSection;
import microsim.statistics.IDoubleSource;
import microsim.statistics.functions.MeanArrayFunction;
import microsim.statistics.functions.PercentileArrayFunction;
import microsim.statistics.weighted.Weighted_CrossSection;
import microsim.statistics.weighted.functions.Weighted_MeanArrayFunction;
import simpaths.data.filters.AgeGroupCSfilter;
import simpaths.model.Person;
import simpaths.model.BenefitUnit;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.Indicator;
import simpaths.model.enums.TimeSeriesVariable;

@Entity
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

        AgeGroupCSfilter ageFilter = new AgeGroupCSfilter(18, 65);

        Weighted_CrossSection.Integer maleCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isPsychologicallyDistressed);
        maleCS.setFilter(ageFilter);

        // Mean dhm score
        CrossSection.Double personsDhm = new CrossSection.Double(model.getPersons(), Person.DoublesVariables.Dhm); // Get cross section of simulated individuals and their mental health using the IDoubleSource interface implemented by Person class.
        personsDhm.setFilter(ageFilter);
        MeanArrayFunction dhm_mean_f = new MeanArrayFunction(personsDhm); // Create MeanArrayFunction
        dhm_mean_f.applyFunction();
        double dhm_mean_out = dhm_mean_f.getDoubleValue(IDoubleSource.Variables.Default); // Get mean value from the MeanArrayFunction
        setDhm_mean(dhm_mean_out);

        // dhm caseness prevalence
        CrossSection.Integer personsDhmCase = new CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isPsychologicallyDistressed);
        personsDhmCase.setFilter(ageFilter);
        MeanArrayFunction dhm_case_f = new MeanArrayFunction(personsDhmCase);
        dhm_case_f.applyFunction();
        double dhm_case_out = dhm_case_f.getDoubleValue(IDoubleSource.Variables.Default);
        setDhm_case(dhm_case_out);

        // Median equivalised disposable income
        CrossSection.Double hhEqIncome = new CrossSection.Double(model.getBenefitUnits(), BenefitUnit.class, "equivalisedDisposableIncomeYearly", false);
        PercentileArrayFunction hhEqIncome_f = new PercentileArrayFunction(hhEqIncome);
        hhEqIncome_f.applyFunction();
        double hhEqIncome_out = hhEqIncome_f.getDoubleValue(PercentileArrayFunction.Variables.P50);
        setEquivalisedDisposableIncomeYearly_median(hhEqIncome_out);

    }


}
