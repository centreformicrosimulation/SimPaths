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

    @Column(name="start_year") private Integer startYear;
    @Column(name="end_year") private Integer endYear;
    @Column(name="end_age") private Integer endAge;


    /**
     * CONSTRUCTOR
     */
    public Population() {}
    public Population(Integer startYear, Integer endYear, Integer endAge) {
        this.startYear = startYear;
        this.endYear = endYear;
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
