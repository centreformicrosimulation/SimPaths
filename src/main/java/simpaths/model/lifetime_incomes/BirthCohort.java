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
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "cohort")
    @OrderBy("id ASC")
    private List<Individual> individuals = new ArrayList<>();

    @Column(name="birth_year") private Integer birthYear;
    @Column(name="end_age") private Integer endAge;
    @Enumerated(EnumType.STRING) private Gender gender;


    /**
     * CONSTRUCTOR
     */
    public BirthCohort() {}
    public BirthCohort(Integer birthYear, Gender gender, Integer endAge) {
        this.birthYear = birthYear;
        this.gender = gender;
        this.endAge = endAge;
    }

    public List<Individual> getIndividuals() {
        return individuals;
    }
    public int getBirthYear() {
        return birthYear;
    }
    public Gender getGender() {
        return gender;
    }
    public void addIndividual(Individual individual) {
        individuals.add(individual);
    }
    public String toString() {
        return id.toString();
    }
}
