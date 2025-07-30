package simpaths.model.lifetime_incomes;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

import simpaths.model.enums.Gender;

@Entity
public class BirthCohort {


    /**
     * ATTRIBUTES
     */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id", unique = true, nullable = false) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, cascade=CascadeType.REFRESH)
    @JoinColumns({
            @JoinColumn(name = "population_id", referencedColumnName = "id")
    })
    private Population population;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "cohort")
    @OrderBy("id ASC")
    private Set<Individual> individuals = new LinkedHashSet<>();

    @Column(name="birth_year") private Integer birthYear;
    @Enumerated(EnumType.STRING) private Gender gender;


    /**
     * CONSTRUCTOR
     */
    public BirthCohort() {}
    public BirthCohort(Integer birthYear, Gender gender, Population population) {
        this.population = population;
        this.birthYear = birthYear;
        this.gender = gender;
        population.addBirthCohort(this);
    }

    public int getBirthYear() {
        return birthYear;
    }
    public Gender getGender() {
        return gender;
    }
    public Set<Individual> getIndividuals() {
        return individuals;
    }
    public void addIndividual(Individual individual) {
        individuals.add(individual);
    }
    public String toString() {
        return id.toString();
    }
}
