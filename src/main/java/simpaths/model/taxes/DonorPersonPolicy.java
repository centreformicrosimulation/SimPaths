package simpaths.model.taxes;


import jakarta.persistence.*;


/**
 *
 * CLASS TO STORE PERSON-LEVEL POLICY-DEPENDENT DATA FOR IMPUTING TAXES AND BENEFITS
 *
 */
@Entity
public class DonorPersonPolicy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id", unique = true, nullable = false) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.REFRESH)
    @JoinColumn(name = "pid", referencedColumnName = "id")
    private DonorPerson person;

    @Column(name = "FROM_YEAR") private Integer fromYear;
    @Column(name = "SYSTEM_YEAR") private Integer systemYear;
    @Column(name="ILS_DISPY") private Double disposableIncomePerMonth;
    @Column(name="ILS_BENMT") private Double ilsBenmtPerMonth;
    @Column(name="ILS_BENNT") private Double ilsBenntPerMonth;
    @Column(name="ILS_ORIGY") private Double originalIncomePerMonth;
    @Column(name="ILS_EARNS") private Double earningsPerMonth;
    @Column(name="XCC") private Double childcareCostPerMonth;


    /**
     * CONSTRUCTORS
     */
    public DonorPersonPolicy(){}


    /**
     * GETTERS AND SETTERS
     */
    public int getFromYear() {
        if (fromYear==null)
            throw new RuntimeException("from year net set in donor person policy object");
        return fromYear;
    }
    public double getDisposableIncomePerMonth() {
        return (disposableIncomePerMonth==null) ? 0. : disposableIncomePerMonth;
    }
    public double getOriginalIncomePerMonth() {
        return (originalIncomePerMonth==null) ? 0. : originalIncomePerMonth;
    }
    public double getMonetaryBenefitsAmount() {
        return (ilsBenmtPerMonth ==null) ? 0. : ilsBenmtPerMonth;
    }
    public double getNonMonetaryBenefitsAmount() {
        return (ilsBenntPerMonth ==null) ? 0. : ilsBenntPerMonth;
    }
    public double getChildcareCostPerMonth() {
        return (childcareCostPerMonth ==null) ? 0. : childcareCostPerMonth;
    }
    public double getEarningsPerMonth() {
        return (earningsPerMonth ==null) ? 0. : earningsPerMonth;
    }
}
