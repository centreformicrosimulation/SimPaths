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
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "processed")
    @OrderBy("key ASC")
    private Set<Household> households = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING) Country country;
    @Column(name = "start_year") int startYear;
    @Column(name = "pop_size") int popSize;
    @Column(name = "no_targets") boolean noTargets;
    @Transient private Set<BenefitUnit> benefitUnits = null;
    @Transient private Set<Person> persons = null;


    /**
     * CONSTRUCTOR
     */
    public Processed() {}
    public Processed(Country country, Integer startYear, Integer popSize, Boolean noTargets) {
        this.country = country;
        this.startYear = startYear;
        this.popSize = popSize;
        this.noTargets = noTargets;
    }
    public Processed(long id, Country country, Integer startYear, Integer popSize, Boolean noTargets) {
        this(country, startYear, popSize, noTargets);
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

    public boolean isNoTargets() {
        return noTargets;
    }

    public void setNoTargets(boolean noTargets) {
        this.noTargets = noTargets;
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
            benefitUnits = new LinkedHashSet<>();
        else
            benefitUnits.clear();
        if (persons == null)
            persons = new LinkedHashSet<>();
        else
            persons.clear();
        for (Household household : households) {
            benefitUnits.addAll(household.getBenefitUnits());
        }
        for (BenefitUnit benefitUnit : benefitUnits) {
            persons.addAll(benefitUnit.getMembers());  // don't update members, as person level variables (male/female/children) not populated
        }
    }
}
