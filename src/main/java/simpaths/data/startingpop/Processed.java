package simpaths.data.startingpop;


import jakarta.persistence.*;
import simpaths.model.BenefitUnit;
import simpaths.model.Household;
import simpaths.model.Person;
import simpaths.model.enums.Country;

import java.util.*;

/**
 *
 * CLASS TO STORE REFERENCES DESCRIBING STARTING POPULATIONS THAT HAVE BEEN EVALUATED PREVIOUSLY
 *
 */
@Entity
public class Processed {


    /**
     * ATTRIBUTES
     */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id", unique = true, nullable = false) private Long id;
    @ManyToMany(fetch = FetchType.LAZY, cascade=CascadeType.ALL, targetEntity = Household.class)
    @JoinTable(name="processed_households_mapping",
        joinColumns = {
            @JoinColumn(name = "id", referencedColumnName = "id")
        },
        inverseJoinColumns = {
            @JoinColumn(name = "hhid", referencedColumnName = "id"),
            @JoinColumn(name = "hhtime", referencedColumnName = "simulation_time"),
            @JoinColumn(name = "hhrun", referencedColumnName = "simulation_run"),
            @JoinColumn(name = "prid", referencedColumnName = "working_id")
        }
    )
    Set<Household> households;

    @Enumerated(EnumType.STRING) Country country;
    @Column(name = "start_year") int startYear;
    @Column(name = "pop_size") int popSize;
    @Transient private Set<BenefitUnit> benefitUnits = null;
    @Transient private Set<Person> persons = null;


    /**
     * CONSTRUCTOR
     */
    public Processed() {}
    public Processed(Country country, Integer startYear, Integer popSize) {
        this.country = country;
        this.startYear = startYear;
        this.popSize = popSize;
    }
    public Processed(long id, Country country, Integer startYear, Integer popSize) {
        this(country, startYear, popSize);
        this.id = id;
    }


    /**
     * GETTERS AND SETTERS
     */
    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public int getStartYear() {
        return startYear;
    }

    public void setStartYear(int startYear) {
        this.startYear = startYear;
    }

    public int getPopSize() {
        return popSize;
    }

    public void setPopSize(int popSize) {
        this.popSize = popSize;
    }

    public Set<Household> getHouseholds() {
        return households;
    }

    public void setHouseholds(Set<Household> households) {
        this.households = households;
        resetDependents();
    }

    public Set<BenefitUnit> getBenefitUnits() {
        if (benefitUnits == null && !households.isEmpty())
            resetDependents();
        return benefitUnits;
    }

    public void setBenefitUnits(Set<BenefitUnit> benefitUnits) {
        this.benefitUnits = benefitUnits;
    }

    public Set<Person> getPersons() {
        if (persons == null && !households.isEmpty())
            resetDependents();
        return persons;
    }

    public void setPersons(Set<Person> persons) {
        this.persons = persons;
    }

    public long getId() {return id;}


    /**
     * WORKER METHODS
     */
    public void resetDependents() {

        if (benefitUnits == null)
            benefitUnits = new HashSet<>();
        else
            benefitUnits.clear();
        if (persons == null)
            persons = new HashSet<>();
        else
            persons.clear();
        for (Household household : households) {
            benefitUnits.addAll(household.getBenefitUnits());
        }
        for (BenefitUnit benefitUnit : benefitUnits) {
            persons.addAll(benefitUnit.getMembers());
        }
    }
}
