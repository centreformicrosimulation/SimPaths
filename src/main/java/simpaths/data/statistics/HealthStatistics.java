package simpaths.data.statistics;

import jakarta.persistence.*;
import microsim.data.db.PanelEntityKey;
import microsim.statistics.CrossSection;
import microsim.statistics.ICollectionFilter;
import microsim.statistics.IDoubleSource;
import microsim.statistics.functions.SumArrayFunction;
import simpaths.data.Parameters;
import simpaths.data.filters.AgeGenderCSfilter;
import simpaths.data.filters.EducationCSfilter;
import simpaths.data.filters.SingleCoupledChildrenCSfilter;
import simpaths.experiment.SimPathsCollector;
import simpaths.model.Person;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.Education;
import simpaths.model.enums.Gender;

@Entity
public class HealthStatistics extends StatisticsHelper {

    @Id
    private PanelEntityKey key = new PanelEntityKey(1L);

    @Column(name = "scenario")
    private String scenario = Parameters.scenario;

    @Column(name = "gender")
    private String gender;

    @Column(name = "agegroup")
    private String agegroup;

    @Column(name = "HouseholdStructure")
    private String HouseholdStructure;

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

    // mental health caseness
    @Column(name="dhm_ghq_prop")
    private double dhm_ghq_prop;

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

    public void setAgegroup(SimPathsCollector.AgeRange agegroup) {
        String agegroup_s = agegroup.toString();
        this.agegroup = agegroup_s;
    }

    public void setHouseholdStructure(SimPathsCollector.HouseholdStructure householdStructure) {
        HouseholdStructure = householdStructure.toString();
    }

    public void setHouseholdStructure(String householdStructure) {
        HouseholdStructure = householdStructure;
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

    public void setKey(PanelEntityKey key) {
        this.key = key;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public void setDhm_ghq_prop(double dhm_ghq_prop) {
        this.dhm_ghq_prop = dhm_ghq_prop;
    }

    public HealthStatistics(PanelEntityKey key) {
        super();
        this.setKey(key);
    }


    public void update(SimPathsModel model, String gender_s, SimPathsCollector.AgeRange ageRange) {


        AgeGenderCSfilter ageGenderCSfilter;

        if (gender_s.equals("Total")) {
            ageGenderCSfilter = new AgeGenderCSfilter(ageRange.lowerBound(), ageRange.upperBound());
        } else {
            ageGenderCSfilter = new AgeGenderCSfilter(ageRange.lowerBound(), ageRange.upperBound(), Gender.valueOf(gender_s));
        }

        // set gender
        setGender(gender_s);

        // set agegroup
        setAgegroup(ageRange);

        // set household structure
        setHouseholdStructure("Total");

        calculateFilteredStats(model, ageGenderCSfilter);

    }

    public void update(SimPathsModel model, SimPathsCollector.HouseholdStructure householdStructure) {

        SingleCoupledChildrenCSfilter singleCoupledChildrenCSfilter = new SingleCoupledChildrenCSfilter(householdStructure.coupled(), householdStructure.children(), householdStructure.gender());

        setGender(householdStructure.gender().toString());

        setAgegroup(new SimPathsCollector.AgeRange(16, 64));

        setHouseholdStructure(householdStructure.toString());

        calculateFilteredStats(model, singleCoupledChildrenCSfilter);

    }

    public void update(SimPathsModel model, Education education) {

        EducationCSfilter educationCSfilter = new EducationCSfilter(education);

        setGender("Total");

        setAgegroup(new SimPathsCollector.AgeRange(24, 64));

        setHouseholdStructure("Total");

        calculateFilteredStats(model, educationCSfilter);

    }

    public void calculateFilteredStats(SimPathsModel model, ICollectionFilter filter) {

        // dhm score
        CrossSection.Double personsDhm = new CrossSection.Double(model.getPersons(), Person.DoublesVariables.Dhm); // Get cross section of simulated individuals and their mental health using the IDoubleSource interface implemented by Person class.
        personsDhm.setFilter(filter);

        setDhm_mean(calculateMean(personsDhm));

        calculateAndSetPercentiles(personsDhm, this::setDhm_p_10, this::setDhm_p_25, this::setDhm_median, this::setDhm_p_75, this::setDhm_p_90);


        // mcs score
        CrossSection.Double personsMCS = new CrossSection.Double(model.getPersons(), Person.DoublesVariables.Dhe_mcs);
        personsMCS.setFilter(filter);

        setDhe_mcs_mean(calculateMean(personsMCS));

        calculateAndSetPercentiles(personsMCS, this::setDhe_mcs_p_10, this::setDhe_mcs_p_25, this::setDhe_mcs_median, this::setDhe_mcs_p_75, this::setDhe_mcs_p_90);


        // pcs score
        CrossSection.Double personsPCS = new CrossSection.Double(model.getPersons(), Person.DoublesVariables.Dhe_pcs);
        personsPCS.setFilter(filter);

        setDhe_pcs_mean(calculateMean(personsPCS));

        calculateAndSetPercentiles(personsPCS, this::setDhe_pcs_p_10, this::setDhe_pcs_p_25, this::setDhe_pcs_median, this::setDhe_pcs_p_75, this::setDhe_pcs_p_90);

        // Life Satisfaction score
        CrossSection.Double personsDls = new CrossSection.Double(model.getPersons(), Person.DoublesVariables.Dls);
        personsDls.setFilter(filter);

        setDls_mean(calculateMean(personsDls));

        calculateAndSetPercentiles(personsDls, this::setDls_p_10, this::setDls_p_25, this::setDls_median, this::setDls_p_75, this::setDls_p_90);

        // GHQ caseness
        CrossSection.Double personsDhm_ghq = new CrossSection.Double(model.getPersons(), Person.DoublesVariables.Dhmghq_L1);
        personsDhm_ghq.setFilter(filter);

        setDhm_ghq_prop(calculateMean(personsDhm_ghq));

        // QALYS as sum of EQ5D
        CrossSection.Double personEQ5D = new CrossSection.Double(model.getPersons(), Person.DoublesVariables.He_eq5d);
        personEQ5D.setFilter(filter);

        SumArrayFunction.Double qalys = new SumArrayFunction.Double(personEQ5D);
        qalys.applyFunction();
        setQalys(qalys.getDoubleValue(IDoubleSource.Variables.Default));

        // WELLBYs as sum of 'points' in 0-10-scale life satisfaction (adjusted)

        SumArrayFunction.Double wellbys = new SumArrayFunction.Double(personsDls);
        wellbys.applyFunction();


        setWellbys(wellbys.getDoubleValue(IDoubleSource.Variables.Default) * WELLBEING_MEASURE_ADJUSTMENT);

        // count
        CrossSection.Integer n_persons = new CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
        n_persons.setFilter(filter);

        calculateAndSetCount(n_persons, this::setN);



    }
}
