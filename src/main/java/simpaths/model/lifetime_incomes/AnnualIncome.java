package simpaths.model.lifetime_incomes;

import jakarta.persistence.*;
import microsim.statistics.IDoubleSource;
import simpaths.data.Parameters;
import simpaths.model.Person;
import simpaths.model.enums.Gender;

@Entity
public class AnnualIncome implements IDoubleSource {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id", unique = true, nullable = false) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, cascade=CascadeType.REFRESH)
    @JoinColumns({
            @JoinColumn(name = "individual_id", referencedColumnName = "id")
    })
    private Individual individual;

    @Column(name="calendar_year") private int year;
    @Column(name="income_value") private Double value;
    @Transient private Double z_m1 = null;
    @Transient private Double z_m2 = null;

    @Transient private Double rnd = null;
    @Transient private Double innov = null;
    @Transient private Double gmIncome = null;
    @Transient private Double gmIncome_m1 = null;


    /**
     * CONSTRUCTOR
     */
    public AnnualIncome() {}
    public AnnualIncome(int year, Individual individual, double rnd, double age0StdDev) {
        this.individual = individual;
        this.year = year;
        this.rnd = rnd;
        value = incomeEvaluation(rnd, age0StdDev);

        individual.addAnnualIncome(this);
    }

    public double getValue() {
        if (value==null) {
            throw new RuntimeException("AnnualIncome.getValue() called before value has been set");
        }
        else {
            return value;
        }
    }
    public int getYear() {return year;}
    public Individual getIndividual() {return individual;}
    public void setIndividual(Individual individual) {
        this.individual = individual;
    }

    public double incomeEvaluation(double rnd, double age0StdDev) {

        Double val = null;
        int age = individual.getAge(year);
        if (age==0) {
            // random draw of new income

            double gmIncome = getGMEquivalisedIncome(year);
            this.gmIncome = gmIncome;
            double gauss = Parameters.getStandardNormalDistribution().inverseCumulativeProbability(rnd);
            innov = gauss;
            val = Math.exp(age0StdDev*gauss) * gmIncome;
        }
        else if (age==1) {
            // AR1 process

            double gmIncome = getGMEquivalisedIncome(year);
            this.gmIncome = gmIncome;

            double gmIncome_m1 = getGMEquivalisedIncome(year-1);
            this.gmIncome_m1 = gmIncome_m1;
            double income_m1 = individual.getAnnualIncome(year-1).getValue();

            z_m1 = Math.log(income_m1 / gmIncome_m1);

            double z_score = Parameters.getRegEquivalisedIncomeDynamics2().getScore(this, AnnualIncome.DoublesVariables.class);
            innov = Parameters.getEquivalisedIncomeDraw2(rnd);
            val = Math.exp(z_score+innov) * gmIncome;
        }
        else {
            // AR2 process

            double gmIncome = getGMEquivalisedIncome(year);
            this.gmIncome = gmIncome;

            double gmIncome_m1 = getGMEquivalisedIncome(year-1);
            this.gmIncome_m1 = gmIncome_m1;
            double income_m1 = individual.getAnnualIncome(year-1).getValue();

            double gmIncome_m2 = getGMEquivalisedIncome( year-2);
            double income_m2 = individual.getAnnualIncome(year-2).getValue();

            z_m1 = Math.log(income_m1 / gmIncome_m1);
            z_m2 = Math.log(income_m2 / gmIncome_m2);

            double z_score = Parameters.getRegEquivalisedIncomeDynamics().getScore(this, AnnualIncome.DoublesVariables.class);
            innov = Parameters.getEquivalisedIncomeDraw(rnd);
            val = Math.exp(z_score+innov) * gmIncome;
        }
        return val;
    }

    private double getGMEquivalisedIncome(int year) {

        individual.setYear(year);
        int age = individual.getAge();
        Gender gender = individual.getGender();
        Double val = Parameters.getEquivalisedIncome(gender, age, year);
        if (val==null) {
            // outside observed range, use regression model

            if (Gender.Male.equals(gender)) {
                val = Math.exp(Parameters.getRegEquivalisedIncomeMales().getScore(individual, Individual.DoublesVariables.class));
            }
            else {
                val = Math.exp(Parameters.getRegEquivalisedIncomeFemales().getScore(individual, Individual.DoublesVariables.class));
            }
        }
        return val;
    }

    public enum DoublesVariables {
        z_lag1,
        z_lag2
    }
    public double getDoubleValue(Enum<?> variableID) {

        switch ((AnnualIncome.DoublesVariables) variableID) {
            case z_lag1 -> {return z_m1;}
            case z_lag2 -> {return z_m2;}
            default -> {
                throw new RuntimeException("request for unrecognised variable: individual");
            }
        }
    }
    public String toString() {
        return id.toString();
    }
}
