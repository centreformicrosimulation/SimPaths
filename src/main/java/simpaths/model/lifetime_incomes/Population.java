package simpaths.model.lifetime_incomes;

import jakarta.persistence.*;
import simpaths.model.enums.Gender;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
public class Population {


    /**
     * ATTRIBUTES
     */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id", unique = true, nullable = false) private Long id;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "population")
    @OrderBy("id ASC")
    private Set<BirthCohort> birthCohorts = new LinkedHashSet<>();

    @Column(name="birth_year") private Integer birthYear;
    @Column(name="end_age") private Integer endAge;


    /**
     * CONSTRUCTOR
     */
    public Population() {}
    public Population(Integer birthYear, Integer endAge) {
        this.birthYear = birthYear;
        this.endAge = endAge;
    }

    public void addBirthCohort(BirthCohort birthCohort) {
        birthCohorts.add(birthCohort);
    }

    public BirthCohort getBirthCohort(int birthYear, Gender gender) {
        BirthCohort cohort = null;
        for (BirthCohort birthCohort : birthCohorts) {
            if (birthCohort.getBirthYear() == birthYear && birthCohort.getGender() == gender) {
                cohort = birthCohort;
                break;
            }
        }
        return cohort;
    }
}
