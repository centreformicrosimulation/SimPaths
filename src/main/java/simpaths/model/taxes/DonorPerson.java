package simpaths.model.taxes;

import jakarta.persistence.*;

import java.util.Set;
import java.util.HashSet;


/**
 *
 * CLASS TO STORE PERSON-LEVEL DATA FOR IMPUTING TAXES AND BENEFITS
 *
 */
@Entity
@Table(name = "DONORPERSON_UK")
public class DonorPerson {


    /**
     * ATTRIBUTES
     */
    @Id @Column(name = "ID", unique = true, nullable = false) private long id;
    @Column(name = "DAG") private Integer age;
    @Column(name = "WEIGHT") private Double weight;
    @Column(name = "HOURS_WORKED_WEEKLY") private Integer hoursWorkedWeekly;
    @Column(name = "DLLTSD") private Integer dlltsd;
    @Column(name = "CARER") private Integer carer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "TUID", nullable=false) private DonorTaxUnit taxUnit;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true) private Set<DonorPersonPolicy> policies = new HashSet<DonorPersonPolicy>(0);


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

        DonorPersonPolicy policy = null;
        for ( DonorPersonPolicy policyCheck : policies) {
            if (policyCheck.getFromYear() == startYear)  policy = policyCheck;
        }
        return policy;
    }
}
