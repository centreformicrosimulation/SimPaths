package simpaths.model.taxes;


import jakarta.persistence.*;
import simpaths.data.Parameters;


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
        return (Parameters.checkFinite(disposableIncomePerMonth)) ? disposableIncomePerMonth : 0.0;
    }
    public double getOriginalIncomePerMonth() {
        return (Parameters.checkFinite(originalIncomePerMonth)) ? originalIncomePerMonth : 0.0;
    }
    public double getMonetaryBenefitsAmount() {
        return (Parameters.checkFinite(ilsBenmtPerMonth)) ? ilsBenmtPerMonth : 0.0;
    }
    public double getNonMonetaryBenefitsAmount() {
        return (Parameters.checkFinite(ilsBenntPerMonth)) ? ilsBenntPerMonth : 0.0;
    }
    public double getChildcareCostPerMonth() {
        return (Parameters.checkFinite(childcareCostPerMonth)) ? childcareCostPerMonth : 0.0;
    }
    public double getEarningsPerMonth() {
        return (Parameters.checkFinite(earningsPerMonth)) ? earningsPerMonth : 0.0;
    }
    public double getUCAmountPerMonth() {
        return (Parameters.checkFinite(benefitUCPerMonth)) ? benefitUCPerMonth : 0.0;
    }
    public double getLegacyBenefitsPerMonth() {
        return
        ((Parameters.checkFinite(benefitHousingPerMonth)) ? benefitHousingPerMonth : 0.0) +
        (Parameters.checkFinite(benefitWorkingTaxCreditPerMonth) ? benefitWorkingTaxCreditPerMonth : 0.0) +
        (Parameters.checkFinite(benefitChildTaxCreditPerMonth) ? benefitChildTaxCreditPerMonth : 0.0) +
        (Parameters.checkFinite(benefitJobSeekerPerMonth) ? benefitJobSeekerPerMonth : 0.0) +
        (Parameters.checkFinite(benefitIncomeBasedSupportPerMonth) ? benefitIncomeBasedSupportPerMonth : 0.0) +
        (Parameters.checkFinite(benefitIncomeRelatedESAPerMonth) ? benefitIncomeRelatedESAPerMonth : 0.0);
    }

    public Integer getReceivesUC() {
        return getUCAmountPerMonth() > 0. ? 1 :0 ;
    }

    public Integer getReceivesLegacyBenefit() {
        return getLegacyBenefitsPerMonth() > 0. ? 1 :0 ;
    }

}
