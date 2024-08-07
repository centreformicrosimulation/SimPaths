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
    @EmbeddedId @Column(unique = true, nullable = false) private final ProcessedKey key;
    @ManyToMany(fetch = FetchType.LAZY, cascade=CascadeType.ALL, targetEntity = Household.class)
    @JoinTable(name="processed_households_mapping",
        joinColumns = {
            @JoinColumn(name = "country", referencedColumnName = "country"),
            @JoinColumn(name = "start_year", referencedColumnName = "start_year"),
            @JoinColumn(name = "pop_size", referencedColumnName = "pop_size")
        },
        inverseJoinColumns = {
            @JoinColumn(name = "hhid", referencedColumnName = "id"),
            @JoinColumn(name = "hhtime", referencedColumnName = "simulation_time"),
            @JoinColumn(name = "hhrun", referencedColumnName = "simulation_run")
        }
    )
    private Set<Household> households = new HashSet<>();
    @Transient private Set<BenefitUnit> benefitUnits = null;
    @Transient private Set<Person> persons = null;


    /**
     * CONSTRUCTOR
     */
    public Processed() {
        key = new ProcessedKey();
    }
    public Processed(Country country, Integer startYear, Integer popSize) {
        key = new ProcessedKey(country, startYear, popSize);
    }


    /**
     * GETTERS AND SETTERS
     */
    public ProcessedKey getKey() {
        return key;
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


    /**
     * WORKER METHODS
     */
    private void resetDependents() {

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
