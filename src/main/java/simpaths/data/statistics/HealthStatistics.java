package simpaths.data.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import microsim.data.db.PanelEntityKey;
import microsim.statistics.CrossSection;
import microsim.statistics.IDoubleSource;
import microsim.statistics.functions.MeanArrayFunction;
import microsim.statistics.functions.PercentileArrayFunction;
import microsim.statistics.functions.SumArrayFunction;
import simpaths.data.filters.AgeGenderCSfilter;
import simpaths.model.Person;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.Gender;

@Entity
public class HealthStatistics {

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

    // MCS score numeric
    @Column(name = "dhe_mcs_mean")
    private double dhe_mcs_mean;

    @Column(name = "dhe_mcs_median")
    private double dhe_mcs_median;

    @Column(name = "dhe_mcs_p_10")
    private double dhe_mcs_p_10;

    @Column(name = "dhe_mcs_p_90")
    private double dhe_mcs_p_90;

    @Column(name = "dhe_mcs_p_25")
    private double dhe_mcs_p_25;

    @Column(name = "dhe_mcs_p_75")
    private double dhe_mcs_p_75;

    // PCS score numeric
    @Column(name = "dhe_pcs_mean")
    private double dhe_pcs_mean;

    @Column(name = "dhe_pcs_median")
    private double dhe_pcs_median;

    @Column(name = "dhe_pcs_p_10")
    private double dhe_pcs_p_10;

    @Column(name = "dhe_pcs_p_90")
    private double dhe_pcs_p_90;

    @Column(name = "dhe_pcs_p_25")
    private double dhe_pcs_p_25;

    @Column(name = "dhe_pcs_p_75")
    private double dhe_pcs_p_75;

    // Life Satisfaction numeric
    @Column(name = "dls_mean")
    private double dls_mean;

    @Column(name = "dls_median")
    private double dls_median;

    @Column(name = "dls_p_10")
    private double dls_p_10;

    @Column(name = "dls_p_90")
    private double dls_p_90;

    @Column(name = "dls_p_25")
    private double dls_p_25;

    @Column(name = "dls_p_75")
    private double dls_p_75;

    @Column(name = "qualys")
    private double qalys;

    @Column(name = "wellbys")
    private double wellbys;

    //N
    @Column(name = "N")
    private int N;

    @Transient
    final static double WELLBEING_MEASURE_ADJUSTMENT = (double) 11 / 7;

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setDhm_mean(double dhm_mean) {
        this.dhm_mean = dhm_mean;
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

    public void setDhe_mcs_mean(double dhe_mcs_mean) {
        this.dhe_mcs_mean = dhe_mcs_mean;
    }

    public void setDhe_mcs_median(double dhe_mcs_median) {
        this.dhe_mcs_median = dhe_mcs_median;
    }

    public void setDhe_mcs_p_10(double dhe_mcs_p_10) {
        this.dhe_mcs_p_10 = dhe_mcs_p_10;
    }

    public void setDhe_mcs_p_90(double dhe_mcs_p_90) {
        this.dhe_mcs_p_90 = dhe_mcs_p_90;
    }

    public void setDhe_mcs_p_25(double dhe_mcs_p_25) {
        this.dhe_mcs_p_25 = dhe_mcs_p_25;
    }

    public void setDhe_mcs_p_75(double dhe_mcs_p_75) {
        this.dhe_mcs_p_75 = dhe_mcs_p_75;
    }

    public void setDhe_pcs_mean(double dhe_pcs_mean) {
        this.dhe_pcs_mean = dhe_pcs_mean;
    }

    public void setDhe_pcs_median(double dhe_pcs_median) {
        this.dhe_pcs_median = dhe_pcs_median;
    }

    public void setDhe_pcs_p_10(double dhe_pcs_p_10) {
        this.dhe_pcs_p_10 = dhe_pcs_p_10;
    }

    public void setDhe_pcs_p_90(double dhe_pcs_p_90) {
        this.dhe_pcs_p_90 = dhe_pcs_p_90;
    }

    public void setDhe_pcs_p_25(double dhe_pcs_p_25) {
        this.dhe_pcs_p_25 = dhe_pcs_p_25;
    }

    public void setDhe_pcs_p_75(double dhe_pcs_p_75) {
        this.dhe_pcs_p_75 = dhe_pcs_p_75;
    }

    public void setDls_mean(double dls_mean) {
        this.dls_mean = dls_mean;
    }

    public void setDls_median(double dls_median) {
        this.dls_median = dls_median;
    }

    public void setDls_p_10(double dls_p_10) {
        this.dls_p_10 = dls_p_10;
    }

    public void setDls_p_90(double dls_p_90) {
        this.dls_p_90 = dls_p_90;
    }

    public void setDls_p_25(double dls_p_25) {
        this.dls_p_25 = dls_p_25;
    }

    public void setDls_p_75(double dls_p_75) {
        this.dls_p_75 = dls_p_75;
    }

    public void setN(int n) {
        N = n;
    }

    public void setQalys(double qalys) {
        this.qalys = qalys;
    }

    public void setWellbys(double wellbys) {
        this.wellbys = wellbys;
    }

    public void update(SimPathsModel model, String gender_s) {


        AgeGenderCSfilter ageGenderCSfilter;

        if (gender_s.equals("Total")) {
            ageGenderCSfilter = new AgeGenderCSfilter(18, 65);
        } else {
            ageGenderCSfilter = new AgeGenderCSfilter(18, 65, Gender.valueOf(gender_s));
        }

        // set gender
        setGender(gender_s);

        // dhm score
        CrossSection.Double personsDhm = new CrossSection.Double(model.getPersons(), Person.DoublesVariables.Dhm); // Get cross section of simulated individuals and their mental health using the IDoubleSource interface implemented by Person class.
        personsDhm.setFilter(ageGenderCSfilter);


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

        // mcs score
        CrossSection.Double personsMCS = new CrossSection.Double(model.getPersons(), Person.DoublesVariables.Dhe_mcs);
        personsMCS.setFilter(ageGenderCSfilter);


        MeanArrayFunction dhe_mcs_mean_f = new MeanArrayFunction(personsMCS); // Create MeanArrayFunction
        dhe_mcs_mean_f.applyFunction();
        setDhe_mcs_mean(dhe_mcs_mean_f.getDoubleValue(IDoubleSource.Variables.Default));

        PercentileArrayFunction perc_dhe_mcs_f = new PercentileArrayFunction(personsMCS);
        perc_dhe_mcs_f.applyFunction();

        setDhe_mcs_p_10(perc_dhe_mcs_f.getDoubleValue(PercentileArrayFunction.Variables.P10));
        setDhe_mcs_p_25(perc_dhe_mcs_f.getDoubleValue(PercentileArrayFunction.Variables.P25));
        setDhe_mcs_median(perc_dhe_mcs_f.getDoubleValue(PercentileArrayFunction.Variables.P50));
        setDhe_mcs_p_75(perc_dhe_mcs_f.getDoubleValue(PercentileArrayFunction.Variables.P75));
        setDhe_mcs_p_90(perc_dhe_mcs_f.getDoubleValue(PercentileArrayFunction.Variables.P90));

        // pcs score
        CrossSection.Double personsPCS = new CrossSection.Double(model.getPersons(), Person.DoublesVariables.Dhe_pcs);
        personsPCS.setFilter(ageGenderCSfilter);


        MeanArrayFunction dhe_pcs_mean_f = new MeanArrayFunction(personsPCS); // Create MeanArrayFunction
        dhe_pcs_mean_f.applyFunction();
        setDhe_pcs_mean(dhe_pcs_mean_f.getDoubleValue(IDoubleSource.Variables.Default));

        PercentileArrayFunction perc_dhe_pcs_f = new PercentileArrayFunction(personsPCS);
        perc_dhe_pcs_f.applyFunction();

        setDhe_pcs_p_10(perc_dhe_pcs_f.getDoubleValue(PercentileArrayFunction.Variables.P10));
        setDhe_pcs_p_25(perc_dhe_pcs_f.getDoubleValue(PercentileArrayFunction.Variables.P25));
        setDhe_pcs_median(perc_dhe_pcs_f.getDoubleValue(PercentileArrayFunction.Variables.P50));
        setDhe_pcs_p_75(perc_dhe_pcs_f.getDoubleValue(PercentileArrayFunction.Variables.P75));
        setDhe_pcs_p_90(perc_dhe_pcs_f.getDoubleValue(PercentileArrayFunction.Variables.P90));

        // Life Satisfaction score
        CrossSection.Double personsDls = new CrossSection.Double(model.getPersons(), Person.DoublesVariables.Dls);
        personsDls.setFilter(ageGenderCSfilter);


        MeanArrayFunction dls_mean_f = new MeanArrayFunction(personsDls); // Create MeanArrayFunction
        dls_mean_f.applyFunction();
        setDls_mean(dls_mean_f.getDoubleValue(IDoubleSource.Variables.Default));

        PercentileArrayFunction perc_dls_f = new PercentileArrayFunction(personsDls);
        perc_dls_f.applyFunction();

        setDls_p_10(perc_dls_f.getDoubleValue(PercentileArrayFunction.Variables.P10));
        setDls_p_25(perc_dls_f.getDoubleValue(PercentileArrayFunction.Variables.P25));
        setDls_median(perc_dls_f.getDoubleValue(PercentileArrayFunction.Variables.P50));
        setDls_p_75(perc_dls_f.getDoubleValue(PercentileArrayFunction.Variables.P75));
        setDls_p_90(perc_dls_f.getDoubleValue(PercentileArrayFunction.Variables.P90));

        // QALYS as sum of EQ5D
        CrossSection.Double personEQ5D = new CrossSection.Double(model.getPersons(), Person.DoublesVariables.He_eq5d);
        personEQ5D.setFilter(ageGenderCSfilter);

        SumArrayFunction.Double qalys = new SumArrayFunction.Double(personEQ5D);
        qalys.applyFunction();
        setQalys(qalys.getDoubleValue(IDoubleSource.Variables.Default));

        // WELLBYs as sum of 'points' in 0-10-scale life satisfaction (adjusted)

        SumArrayFunction.Double wellbys = new SumArrayFunction.Double(personsDls);
        wellbys.applyFunction();


        setWellbys(wellbys.getDoubleValue(IDoubleSource.Variables.Default) * WELLBEING_MEASURE_ADJUSTMENT);

        // count
        CrossSection.Integer n_persons = new CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
        n_persons.setFilter(ageGenderCSfilter);

        SumArrayFunction.Integer count_f = new SumArrayFunction.Integer(n_persons);
        count_f.applyFunction();
        setN(count_f.getIntValue(IDoubleSource.Variables.Default));
    }
}
