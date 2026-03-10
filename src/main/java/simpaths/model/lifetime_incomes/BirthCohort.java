package simpaths.model.lifetime_incomes;

import jakarta.persistence.*;

import java.util.*;

import simpaths.model.enums.Gender;

@Entity
public class BirthCohort {


    /**
     * ATTRIBUTES
     */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id", unique = true, nullable = false) private Long id;
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "cohort")
    @OrderBy("id ASC")
    private Set<Individual> individuals = new LinkedHashSet<>();

    @Column(name="birth_year") private Integer birthYear;
    @Column(name="end_age") private Integer endAge;
    @Enumerated(EnumType.STRING) private Gender demSex;

    @Transient List<Individual> sortedIndividuals = null;


    /**
     * CONSTRUCTOR
     */
    public BirthCohort() {}
    public BirthCohort(Integer birthYear, Gender demSex, Integer endAge) {
        this.birthYear = birthYear;
        this.demSex = demSex;
        this.endAge = endAge;
    }

    public Set<Individual> getIndividuals() {
        return individuals;
    }
    public void setIndividuals(Set<Individual> individuals) {
        this.individuals = individuals;
    }
    public void setSortedIndividuals(List<Individual> sortedIndividuals) {this.sortedIndividuals = sortedIndividuals;}
    public List<Individual> getSortedIndividuals() {return sortedIndividuals;}
    public int getBirthYear() {
        return birthYear;
    }
    public Gender getGender() {
        return demSex;
    }
    public int getEndAge() {return endAge;}
    public void addIndividual(Individual individual) {
        individuals.add(individual);
    }
    public String toString() {
        return id.toString();
    }
}
