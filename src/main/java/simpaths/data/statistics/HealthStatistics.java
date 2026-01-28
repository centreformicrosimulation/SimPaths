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
    private String demSex;

    // mental health numeric
    @Column(name = "dhm_mean")
    private double healthWbScore0to36Avg;

    @Column(name = "dhm_median")
    private double healthWbScore0to36P50;

    @Column(name = "dhm_p_10")
    private double healthWbScore0to36P10;

    @Column(name = "dhm_p_90")
    private double healthWbScore0to36P90;

    @Column(name = "dhm_p_25")
    private double healthWbScore0to36P25;

    @Column(name = "dhm_p_75")
    private double healthWbScore0to36P75;

    // MCS score numeric
    @Column(name = "dhe_mcs_mean")
    private double healthMentalMcsAvg;

    @Column(name = "dhe_mcs_median")
    private double healthMentalMcsP50;

    @Column(name = "dhe_mcs_p_10")
    private double healthMentalMcsP10;

    @Column(name = "dhe_mcs_p_90")
    private double healthMentalMcsP90;

    @Column(name = "dhe_mcs_p_25")
    private double healthMentalMcsP25;

    @Column(name = "dhe_mcs_p_75")
    private double healthMentalMcsP75;

    // PCS score numeric
    @Column(name = "dhe_pcs_mean")
    private double healthPhysicalPcsAvg;

    @Column(name = "dhe_pcs_median")
    private double healthPhysicalPcsP50;

    @Column(name = "dhe_pcs_p_10")
    private double healthPhysicalPcsP10;

    @Column(name = "dhe_pcs_p_90")
    private double healthPhysicalPcsP90;

    @Column(name = "dhe_pcs_p_25")
    private double healthPhysicalPcsP25;

    @Column(name = "dhe_pcs_p_75")
    private double healthPhysicalPcsP75;

    // Life Satisfaction numeric
    @Column(name = "dls_mean")
    private double demLifeSatScore0to10Avg;

    @Column(name = "dls_median")
    private double demLifeSatScore0to10P50;

    @Column(name = "dls_p_10")
    private double demLifeSatScore0to10P10;

    @Column(name = "dls_p_90")
    private double demLifeSatScore0to10P90;

    @Column(name = "dls_p_25")
    private double demLifeSatScore0to10P25;

    @Column(name = "dls_p_75")
    private double demLifeSatScore0to10P75;

    @Column(name = "qualys")
    private double healthLifeYearQualAdj;

    @Column(name = "wellbys")
    private double healthLifeYearWbAdj;

    //N
    @Column(name = "N")
    private int healthNObsSubGroup;


    public void setGender(String demSex) {
        this.demSex = demSex;
    }

    public void setDhm_mean(double healthWbScore0to36Avg) {
        this.healthWbScore0to36Avg = healthWbScore0to36Avg;
    }

    public void setDhm_median(double healthWbScore0to36P50) {
        this.healthWbScore0to36P50 = healthWbScore0to36P50;
    }

    public void setDhm_p_10(double healthWbScore0to36P10) {
        this.healthWbScore0to36P10 = healthWbScore0to36P10;
    }

    public void setDhm_p_90(double healthWbScore0to36P90) {
        this.healthWbScore0to36P90 = healthWbScore0to36P90;
    }

    public void setDhm_p_25(double healthWbScore0to36P25) {
        this.healthWbScore0to36P25 = healthWbScore0to36P25;
    }

    public void setDhm_p_75(double healthWbScore0to36P75) {
        this.healthWbScore0to36P75 = healthWbScore0to36P75;
    }

    public void setDhe_mcs_mean(double healthMentalMcsAvg) {
        this.healthMentalMcsAvg = healthMentalMcsAvg;
    }

    public void setDhe_mcs_median(double healthMentalMcsP50) {
        this.healthMentalMcsP50 = healthMentalMcsP50;
    }

    public void setDhe_mcs_p_10(double healthMentalMcsP10) {
        this.healthMentalMcsP10 = healthMentalMcsP10;
    }

    public void setDhe_mcs_p_90(double healthMentalMcsP90) {
        this.healthMentalMcsP90 = healthMentalMcsP90;
    }

    public void setDhe_mcs_p_25(double healthMentalMcsP25) {
        this.healthMentalMcsP25 = healthMentalMcsP25;
    }

    public void setDhe_mcs_p_75(double healthMentalMcsP75) {
        this.healthMentalMcsP75 = healthMentalMcsP75;
    }

    public void setDhe_pcs_mean(double healthPhysicalPcsAvg) {
        this.healthPhysicalPcsAvg = healthPhysicalPcsAvg;
    }

    public void setDhe_pcs_median(double healthPhysicalPcsP50) {
        this.healthPhysicalPcsP50 = healthPhysicalPcsP50;
    }

    public void setDhe_pcs_p_10(double healthPhysicalPcsP10) {
        this.healthPhysicalPcsP10 = healthPhysicalPcsP10;
    }

    public void setDhe_pcs_p_90(double healthPhysicalPcsP90) {
        this.healthPhysicalPcsP90 = healthPhysicalPcsP90;
    }

    public void setDhe_pcs_p_25(double healthPhysicalPcsP25) {
        this.healthPhysicalPcsP25 = healthPhysicalPcsP25;
    }

    public void setDhe_pcs_p_75(double healthPhysicalPcsP75) {
        this.healthPhysicalPcsP75 = healthPhysicalPcsP75;
    }

    public void setDls_mean(double demLifeSatScore0to10Avg) {
        this.demLifeSatScore0to10Avg = demLifeSatScore0to10Avg;
    }

    public void setDls_median(double demLifeSatScore0to10P50) {
        this.demLifeSatScore0to10P50 = demLifeSatScore0to10P50;
    }

    public void setDls_p_10(double demLifeSatScore0to10P10) {
        this.demLifeSatScore0to10P10 = demLifeSatScore0to10P10;
    }

    public void setDls_p_90(double demLifeSatScore0to10P90) {
        this.demLifeSatScore0to10P90 = demLifeSatScore0to10P90;
    }

    public void setDls_p_25(double demLifeSatScore0to10P25) {
        this.demLifeSatScore0to10P25 = demLifeSatScore0to10P25;
    }

    public void setDls_p_75(double demLifeSatScore0to10P75) {
        this.demLifeSatScore0to10P75 = demLifeSatScore0to10P75;
    }

    public void setN(int n) {
        healthNObsSubGroup = n;
    }

    public void setQalys(double healthLifeYearQualAdj) {
        this.healthLifeYearQualAdj = healthLifeYearQualAdj;
    }

    public void setWellbys(double healthLifeYearWbAdj) {
        this.healthLifeYearWbAdj = healthLifeYearWbAdj;
    }

    public void update(SimPathsModel model, String gender_s) {


        AgeGenderCSfilter ageGenderCSfilter;

        if (gender_s.equals("Total")) {
            ageGenderCSfilter = new AgeGenderCSfilter(25, 64);
        } else {
            ageGenderCSfilter = new AgeGenderCSfilter(25, 64, Gender.valueOf(gender_s));
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


        setWellbys(wellbys.getDoubleValue(IDoubleSource.Variables.Default));

        // count
        CrossSection.Integer n_persons = new CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
        n_persons.setFilter(ageGenderCSfilter);

        SumArrayFunction.Integer count_f = new SumArrayFunction.Integer(n_persons);
        count_f.applyFunction();
        setN(count_f.getIntValue(IDoubleSource.Variables.Default));
    }
}
