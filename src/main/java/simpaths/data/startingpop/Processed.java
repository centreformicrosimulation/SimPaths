package simpaths.data.startingpop;


import jakarta.persistence.*;
import microsim.data.db.DatabaseUtils;
import microsim.data.db.PanelEntityKey;
import simpaths.model.BenefitUnit;
import simpaths.model.HibernateUtil;
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
    @EmbeddedId @Column(unique = true, nullable = false) private final PopKey key;
//    @ManyToMany(fetch = FetchType.LAZY, cascade=CascadeType.REFRESH, targetEntity = Household.class)
//    @JoinTable(name="processed_households_mapping",
//        joinColumns = {
//            @JoinColumn(name = "country"),
//            @JoinColumn(name = "start_year"),
//            @JoinColumn(name = "size")
//        },
//        inverseJoinColumns = {
//            @JoinColumn(name = "hhid"),
//            @JoinColumn(name = "hhtime"),
//            @JoinColumn(name = "hhrun")
//        }
//    ) private Set<Household> households = new HashSet<>();
//    @Transient private Set<BenefitUnit> benefitUnits = new HashSet<>();
//    @Transient private Set<Person> persons = new HashSet<>();


    /**
     * CONSTRUCTOR
     */
    public Processed() {
        key = new PopKey();
    }
    public Processed(Country country, Integer startYear, Integer size) {
        key = new PopKey(country, startYear, size);
        queryDatabase();
    }


    /**
     * GETTERS AND SETTERS
     */
    public PopKey getKey() {
        return key;
    }

//    public Set<Household> getHouseholds() {
//        return households;
//    }
//
//    public void setHouseholds(Set<Household> households) {
//        this.households = households;
//    }
//
//    public Set<BenefitUnit> getBenefitUnits() {
//        return benefitUnits;
//    }
//
//    public void setBenefitUnits(Set<BenefitUnit> benefitUnits) {
//        this.benefitUnits = benefitUnits;
//    }
//
//    public Set<Person> getPersons() {
//        return persons;
//    }
//
//    public void setPersons(Set<Person> persons) {
//        this.persons = persons;
//    }


    /**
     * WORKER METHODS
     */
    private void queryDatabase() {

        // establish session for database link
        EntityTransaction txn = null;
        try {

            // query database
            Map propertyMap = new HashMap();
            propertyMap.put("hibernate.connection.url", "jdbc:h2:file:" + DatabaseUtils.databaseInputUrl);
            EntityManager em = Persistence.createEntityManagerFactory("starting-population", propertyMap).createEntityManager();
            txn = em.getTransaction();
            txn.begin();
            //String query = "SELECT tr FROM Processed tr LEFT JOIN FETCH tr.households th LEFT JOIN FETCH th.benefitUnits tb LEFT JOIN FETCH tb.members tp";
            String query = "SELECT th FROM Household th";
            List<Household> processedList = em.createQuery(query).getResultList();
            //List<Processed> processedList = em.createQuery(query).getResultList();

//            if (!processedList.isEmpty()) {
//
//                // populate attributes
//                households = new LinkedHashSet<>(processedList.get(0).getHouseholds());
//                if (!households.isEmpty()) {
//                    for (Household household : households) {
//                        benefitUnits.addAll(household.getBenefitUnits());
//                    }
//                    for (BenefitUnit benefitUnit : benefitUnits) {
//                        persons.addAll(benefitUnit.getMembers());
//                    }
//                }
//            }


            // close database connection
            txn.commit();
            em.close();
        } catch (Exception e) {
            if (txn != null) {
                txn.rollback();
            }
            e.printStackTrace();
            throw new RuntimeException("Problem sourcing data for starting population");
        }
    }
}
