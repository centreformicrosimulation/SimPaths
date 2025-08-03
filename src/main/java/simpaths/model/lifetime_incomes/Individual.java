package simpaths.model.lifetime_incomes;

import jakarta.persistence.*;
import microsim.event.EventListener;
import microsim.statistics.IDoubleSource;
import simpaths.data.Parameters;
import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.TimeSeriesVariable;

import java.util.LinkedHashSet;

import java.util.Set;

@Entity
public class Individual implements IDoubleSource, Comparable<Individual> {


    /**
     * ATTRIBUTES
     */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id", unique = true, nullable = false) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, cascade=CascadeType.REFRESH)
    @JoinColumns({
        @JoinColumn(name = "cohort_id", referencedColumnName = "id")
    })
    private BirthCohort cohort;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "individual")
    @OrderBy("id ASC")
    private Set<AnnualIncome> incomes = new LinkedHashSet<>();
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "ltIncomeID")
    private Set<Person> persons = new LinkedHashSet<>();

    @Transient int year;

    @Override
    public int compareTo(Individual otherIndividual) {
        return Double.compare(getAnnualIncome(Parameters.startYear).getValue(), otherIndividual.getAnnualIncome(Parameters.startYear).getValue());
    }



    /**
     * CONSTRUCTOR
     */
    public Individual() {
    }
    public Individual(BirthCohort cohort) {
        this.cohort = cohort;
        //cohort.addIndividual(this);
    }

    public Long getId() {return id;}
    public Integer getAge() {
        return (cohort!=null) ? year - cohort.getBirthYear() : null;
    }
    public Integer getAge(int year) {
        return (cohort!=null) ? year - cohort.getBirthYear() : null;
    }
    public Gender getGender() {
        return (cohort != null) ? cohort.getGender() : null;
    }
    public Integer getBirthYear() {
        return (cohort != null) ? cohort.getBirthYear() : null;
    }
    public AnnualIncome getAnnualIncome(int year) {
        AnnualIncome income = null;
        for (AnnualIncome inc : incomes) {
            if (inc.getYear()==year) {
                income = inc;
                break;
            }
        }
        return income;
    }
    public void setCohort(BirthCohort cohort) {this.cohort = cohort;}
    public void setYear(int year) {this.year = year;}
    public void addAnnualIncome(AnnualIncome income) {
        incomes.add(income);
    }

    public enum DoublesVariables {

        Age0,
        Age1,
        Age2,
        Age3,
        Age4,
        Age5,
        Age6,
        Age7,
        Age8,
        Age9,

        Age10,
        Age11,
        Age12,
        Age13,
        Age14,
        Age15,
        Age16,
        Age17,
        Age18,
        Age19,
        Age20,
        Age21,
        Age22,
        Age23,
        Age24,
        Age25,
        Age26,
        Age27,
        Age28,
        Age29,
        Age30,
        Age31,
        Age32,
        Age33,
        Age34,
        Age35,
        Age36,
        Age37,
        Age38,
        Age39,
        Age40,
        Age41,
        Age42,
        Age43,
        Age44,
        Age45,
        Age46,
        Age47,
        Age48,
        Age49,
        Age50,
        Age51,
        Age52,
        Age53,
        Age54,
        Age55,
        Age56,
        Age57,
        Age58,
        Age59,
        Age60,
        Age61,
        Age62,
        Age63,
        Age64,
        Age65,
        Age66,
        Age67,
        Age68,
        Age69,
        Age70,
        Age71,
        Age72,
        Age73,
        Age74,
        Age75,
        Age76,
        Age77,
        Age78,
        Age79,
        Age80plus,
        Log_GDP_per_capita
    }
    public double getDoubleValue(Enum<?> variableID) {

        switch ((Individual.DoublesVariables) variableID) {
            case Age0 -> {return (year-getBirthYear()==0) ? 1: 0;}
            case Age1 -> {return (year-getBirthYear()==1) ? 1: 0;}
            case Age2 -> {return (year-getBirthYear()==2) ? 1: 0;}
            case Age3 -> {return (year-getBirthYear()==3) ? 1: 0;}
            case Age4 -> {return (year-getBirthYear()==4) ? 1: 0;}
            case Age5 -> {return (year-getBirthYear()==5) ? 1: 0;}
            case Age6 -> {return (year-getBirthYear()==6) ? 1: 0;}
            case Age7 -> {return (year-getBirthYear()==7) ? 1: 0;}
            case Age8 -> {return (year-getBirthYear()==8) ? 1: 0;}
            case Age9 -> {return (year-getBirthYear()==9) ? 1: 0;}
            case Age10 -> {return (year-getBirthYear()==10) ? 1: 0;}
            case Age11 -> {return (year-getBirthYear()==11) ? 1: 0;}
            case Age12 -> {return (year-getBirthYear()==12) ? 1: 0;}
            case Age13 -> {return (year-getBirthYear()==13) ? 1: 0;}
            case Age14 -> {return (year-getBirthYear()==14) ? 1: 0;}
            case Age15 -> {return (year-getBirthYear()==15) ? 1: 0;}
            case Age16 -> {return (year-getBirthYear()==16) ? 1: 0;}
            case Age17 -> {return (year-getBirthYear()==17) ? 1: 0;}
            case Age18 -> {return (year-getBirthYear()==18) ? 1: 0;}
            case Age19 -> {return (year-getBirthYear()==19) ? 1: 0;}
            case Age20 -> {return (year-getBirthYear()==20) ? 1: 0;}
            case Age21 -> {return (year-getBirthYear()==21) ? 1: 0;}
            case Age22 -> {return (year-getBirthYear()==22) ? 1: 0;}
            case Age23 -> {return (year-getBirthYear()==23) ? 1: 0;}
            case Age24 -> {return (year-getBirthYear()==24) ? 1: 0;}
            case Age25 -> {return (year-getBirthYear()==25) ? 1: 0;}
            case Age26 -> {return (year-getBirthYear()==26) ? 1: 0;}
            case Age27 -> {return (year-getBirthYear()==27) ? 1: 0;}
            case Age28 -> {return (year-getBirthYear()==28) ? 1: 0;}
            case Age29 -> {return (year-getBirthYear()==29) ? 1: 0;}
            case Age30 -> {return (year-getBirthYear()==30) ? 1: 0;}
            case Age31 -> {return (year-getBirthYear()==31) ? 1: 0;}
            case Age32 -> {return (year-getBirthYear()==32) ? 1: 0;}
            case Age33 -> {return (year-getBirthYear()==33) ? 1: 0;}
            case Age34 -> {return (year-getBirthYear()==34) ? 1: 0;}
            case Age35 -> {return (year-getBirthYear()==35) ? 1: 0;}
            case Age36 -> {return (year-getBirthYear()==36) ? 1: 0;}
            case Age37 -> {return (year-getBirthYear()==37) ? 1: 0;}
            case Age38 -> {return (year-getBirthYear()==38) ? 1: 0;}
            case Age39 -> {return (year-getBirthYear()==39) ? 1: 0;}
            case Age40 -> {return (year-getBirthYear()==40) ? 1: 0;}
            case Age41 -> {return (year-getBirthYear()==41) ? 1: 0;}
            case Age42 -> {return (year-getBirthYear()==42) ? 1: 0;}
            case Age43 -> {return (year-getBirthYear()==43) ? 1: 0;}
            case Age44 -> {return (year-getBirthYear()==44) ? 1: 0;}
            case Age45 -> {return (year-getBirthYear()==45) ? 1: 0;}
            case Age46 -> {return (year-getBirthYear()==46) ? 1: 0;}
            case Age47 -> {return (year-getBirthYear()==47) ? 1: 0;}
            case Age48 -> {return (year-getBirthYear()==48) ? 1: 0;}
            case Age49 -> {return (year-getBirthYear()==49) ? 1: 0;}
            case Age50 -> {return (year-getBirthYear()==50) ? 1: 0;}
            case Age51 -> {return (year-getBirthYear()==51) ? 1: 0;}
            case Age52 -> {return (year-getBirthYear()==52) ? 1: 0;}
            case Age53 -> {return (year-getBirthYear()==53) ? 1: 0;}
            case Age54 -> {return (year-getBirthYear()==54) ? 1: 0;}
            case Age55 -> {return (year-getBirthYear()==55) ? 1: 0;}
            case Age56 -> {return (year-getBirthYear()==56) ? 1: 0;}
            case Age57 -> {return (year-getBirthYear()==57) ? 1: 0;}
            case Age58 -> {return (year-getBirthYear()==58) ? 1: 0;}
            case Age59 -> {return (year-getBirthYear()==59) ? 1: 0;}
            case Age60 -> {return (year-getBirthYear()==60) ? 1: 0;}
            case Age61 -> {return (year-getBirthYear()==61) ? 1: 0;}
            case Age62 -> {return (year-getBirthYear()==62) ? 1: 0;}
            case Age63 -> {return (year-getBirthYear()==63) ? 1: 0;}
            case Age64 -> {return (year-getBirthYear()==64) ? 1: 0;}
            case Age65 -> {return (year-getBirthYear()==65) ? 1: 0;}
            case Age66 -> {return (year-getBirthYear()==66) ? 1: 0;}
            case Age67 -> {return (year-getBirthYear()==67) ? 1: 0;}
            case Age68 -> {return (year-getBirthYear()==68) ? 1: 0;}
            case Age69 -> {return (year-getBirthYear()==69) ? 1: 0;}
            case Age70 -> {return (year-getBirthYear()==70) ? 1: 0;}
            case Age71 -> {return (year-getBirthYear()==71) ? 1: 0;}
            case Age72 -> {return (year-getBirthYear()==72) ? 1: 0;}
            case Age73 -> {return (year-getBirthYear()==73) ? 1: 0;}
            case Age74 -> {return (year-getBirthYear()==74) ? 1: 0;}
            case Age75 -> {return (year-getBirthYear()==75) ? 1: 0;}
            case Age76 -> {return (year-getBirthYear()==76) ? 1: 0;}
            case Age77 -> {return (year-getBirthYear()==77) ? 1: 0;}
            case Age78 -> {return (year-getBirthYear()==78) ? 1: 0;}
            case Age79 -> {return (year-getBirthYear()==79) ? 1: 0;}
            case Age80plus -> {return (year-getBirthYear()>79) ? 1: 0;}
            case Log_GDP_per_capita -> {
                return Math.log(Parameters.getTimeSeriesValue(year, TimeSeriesVariable.GDPperCapita));
            }
            default -> {
                throw new RuntimeException("request for unrecognised variable");
            }
        }
    }
    public String toString() {
        return id.toString();
    }
}
