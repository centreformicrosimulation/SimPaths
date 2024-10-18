package simpaths.model.taxes;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashSet;


/**
 *
 * CLASS TO STORE PERSON-LEVEL DATA FOR IMPUTING TAXES AND BENEFITS
 *
 */
@Entity
public class DonorPerson {

    @Id @Column(name = "ID", unique = true, nullable = false) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, cascade=CascadeType.REFRESH)
    @JoinColumn(name = "TUID", referencedColumnName = "id")
    private DonorTaxUnit taxUnit;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "person")
    private Set<DonorPersonPolicy> policies = new LinkedHashSet<>();

    @Column(name = "DAG") private Integer age;
    @Column(name = "WEIGHT") private Double weight;
    @Column(name = "HOURS_WORKED_WEEKLY") private Integer hoursWorkedWeekly;
    @Column(name = "DLLTSD") private Integer dlltsd;
    @Column(name = "CARER") private Integer carer;


    /**
     * CONSTRUCTORS
     */
    public DonorPerson(){}


    /**
     * GETTERS AND SETTERS
     */
    public long getId() {
        return this.id;
    }
    public Integer getAge() { return this.age; }
    public int getHoursWorkedWeekly() { return this.hoursWorkedWeekly; }
    public int getDlltsd() { return this.dlltsd; }
    public int getCarer() { return this.carer; }
    public double getWeight() { return this.weight; }
    public Set<DonorPersonPolicy> getPolicies() { return policies; }
    public DonorPersonPolicy getPolicy(int startYear) {
        for ( DonorPersonPolicy policy : policies) {
            if (policy.getFromYear() == startYear)
                return policy;
        }
        return null;
    }
}
