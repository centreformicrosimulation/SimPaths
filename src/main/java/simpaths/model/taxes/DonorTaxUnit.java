package simpaths.model.taxes;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashSet;


/**
 *
 * CLASS TO STORE DATA FOR IMPUTING TAXES AND BENEFITS
 *
 */
@Entity
public class DonorTaxUnit {

    @Id @Column(name = "ID", unique = true, nullable = false) private Long id;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "taxUnit")
    @OrderBy("id ASC")
    private Set<DonorPerson> persons = new LinkedHashSet<>();
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "taxUnit")
    private Set<DonorTaxUnitPolicy> policies = new LinkedHashSet<>();

    @Column(name = "WEIGHT") private Double weight;


    /**
     * CONSTRUCTORS
     */
    public DonorTaxUnit(){}

    /**
     * GETTERS AND SETTERS
     */
    public long getId() {return this.id;}
    public void setId(long id) {this.id = id;}
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
