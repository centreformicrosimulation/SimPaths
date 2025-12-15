package simpaths.model.taxes;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;


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
    @Column(name = "WEIGHT") private Double dem;
    @Column(name = "HOURS_WORKED_WEEKLY") private Integer labHrsWorkWeek;
    @Column(name = "DLLTSD") private Integer healthDsblLongtermFlag;
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
    public int getHoursWorkedWeekly() { return this.labHrsWorkWeek; }
    public int getDlltsd() { return this.healthDsblLongtermFlag; }
    public int getCarer() { return this.carer; }
    public double getWeight() { return this.dem; }
    public Set<DonorPersonPolicy> getPolicies() { return policies; }
    public DonorPersonPolicy getPolicy(int startYear) {
        for ( DonorPersonPolicy policy : policies) {
            if (policy.getFromYear() == startYear)
                return policy;
        }
        return null;
    }
}
