package simpaths.model.taxes;

import jakarta.persistence.*;

import java.util.Set;
import java.util.HashSet;


/**
 *
 * CLASS TO STORE DATA FOR IMPUTING TAXES AND BENEFITS
 *
 */
@Entity
@Table(name = "DONORTAXUNIT_UK")
public class DonorTaxUnit {


    /**
     * ATTRIBUTES
     */
    @Id @Column(name = "ID", unique = true, nullable = false) private long id;
    @Column(name = "WEIGHT") private Double weight;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "taxUnit", cascade = CascadeType.ALL, orphanRemoval = true) private Set<DonorPerson> persons = new HashSet<DonorPerson>(0);
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "taxUnit", cascade = CascadeType.ALL, orphanRemoval = true) private Set<DonorTaxUnitPolicy> policies = new HashSet<DonorTaxUnitPolicy>(0);


    /**
     * CONSTRUCTORS
     */
    public DonorTaxUnit(){}

    /**
     * GETTERS AND SETTERS
     */
    public long getId() {return this.id;}
    public void setId(int id) {this.id = id;}
    public Double getWeight() {return this.weight;}
    public void setWeight(double weight) { this.weight = weight;}
    public DonorTaxUnitPolicy getPolicyByFromYear(int fromYear) {

        for (DonorTaxUnitPolicy policy : policies) {
            if (policy.getFromYear() == fromYear)  {
                return policy;
            }
        }
        return new DonorTaxUnitPolicy(fromYear, this);
    }
    public DonorTaxUnitPolicy getPolicyBySystemYear(int systemYear) {

        for ( DonorTaxUnitPolicy policy : policies) {
            if (policy.getSystemYear() == systemYear)  {
                return policy;
            }
        }
        throw new RuntimeException("failed to find requested database tax policy for policy year" + systemYear);
    }
    public Set<DonorTaxUnitPolicy> getPolicies() {
        return this.policies;
    }

    public Set<DonorPerson> getPersons() {
        return this.persons;
    }
}
