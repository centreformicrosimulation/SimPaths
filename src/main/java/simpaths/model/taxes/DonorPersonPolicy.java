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
    @Column(name="BSAUC_S") private Double benefitUCPerMonth; //bsauc_s
    @Column(name="BHO_S") private Double benefitHousingPerMonth; //bho_s
    @Column(name="BWKMT_S") private Double benefitWorkingTaxCreditPerMonth; //bwkmt_s
    @Column(name="BFAMT_S") private Double benefitChildTaxCreditPerMonth; //bfamt_s
    @Column(name="BUNCT_S") private Double benefitJobSeekerPerMonth; //bunct_s
    @Column(name="BSA_S") private Double benefitIncomeBasedSupportPerMonth; //bsa_s
    @Column(name="BSADI_S") private Double benefitIncomeRelatedESAPerMonth; //bsadi_s

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
        return (Double.isNaN(disposableIncomePerMonth)) ? 0. : disposableIncomePerMonth;
    }
    public double getOriginalIncomePerMonth() {
        return (Double.isNaN(originalIncomePerMonth)) ? 0. : originalIncomePerMonth;
    }
    public double getMonetaryBenefitsAmount() {
        return (Double.isNaN(ilsBenmtPerMonth)) ? 0. : ilsBenmtPerMonth;
    }
    public double getNonMonetaryBenefitsAmount() {
        return (Double.isNaN(ilsBenntPerMonth)) ? 0. : ilsBenntPerMonth;
    }
    public double getChildcareCostPerMonth() {
        return (Double.isNaN(childcareCostPerMonth)) ? 0. : childcareCostPerMonth;
    }
    public double getEarningsPerMonth() {
        return (Double.isNaN(earningsPerMonth)) ? 0. : earningsPerMonth;
    }
    public double getUCAmountPerMonth() {
        return (Double.isNaN(benefitUCPerMonth)) ? 0. : benefitUCPerMonth;
    }
    public double getLegacyBenefitsPerMonth() {
        return
        ((Double.isNaN(benefitHousingPerMonth)) ? 0. : benefitHousingPerMonth) +
        ((benefitWorkingTaxCreditPerMonth == null) ? 0. : benefitWorkingTaxCreditPerMonth) +
        ((benefitChildTaxCreditPerMonth == null) ? 0. : benefitChildTaxCreditPerMonth) +
        ((benefitJobSeekerPerMonth == null) ? 0. : benefitJobSeekerPerMonth) +
        ((benefitIncomeBasedSupportPerMonth == null) ? 0. : benefitIncomeBasedSupportPerMonth) +
        ((benefitIncomeRelatedESAPerMonth == null) ? 0. : benefitIncomeRelatedESAPerMonth);
    }

    public Integer getReceivesUC() {
        return getUCAmountPerMonth() > 0. ? 1 :0 ;
    }

    public Integer getReceivesLegacyBenefit() {
        return getLegacyBenefitsPerMonth() > 0. ? 1 :0 ;
    }

}
