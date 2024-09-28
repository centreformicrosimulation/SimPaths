package simpaths.model.taxes;


import jakarta.persistence.*;
import simpaths.data.Parameters;


/**
 *
 * CLASS TO STORE TAX-UNIT-LEVEL POLICY-DEPENDENT DATA FOR IMPUTING TAXES AND BENEFITS
 *
 */
@Entity
public class DonorTaxUnitPolicy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id", unique = true, nullable = false) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, cascade=CascadeType.REFRESH)
    @JoinColumn(name = "TUID", referencedColumnName = "id")
    private DonorTaxUnit taxUnit;

    @Column(name = "FROM_YEAR") private Integer fromYear;           // simulated year to use this policy from (ignored for earliest fromYear policy)
    @Column(name = "SYSTEM_YEAR") private Integer systemYear;       // year for which database values were projected - financials are reported in prices from this year
    @Column(name="ILS_DISPY") private Double disposableIncomePerMonth;
    @Column(name="ILS_ORIGY") private Double originalIncomePerMonth;
    @Column(name="ILS_EARNS") private Double earningsPerMonth;
    @Column(name="ILS_BENMT") private Double benMeansTestPerMonth;
    @Column(name="ILS_BENNT") private Double benNonMeansTestPerMonth;
    @Column(name="SECOND_INCOME") private Double secondIncomePerMonth;
    @Column(name="XCC") private Double childcareCostPerMonth;
    @Column(name = "DONOR_KEY0") private Integer donorKey0;
    @Column(name = "DONOR_KEY1") private Integer donorKey1;
    @Column(name = "DONOR_KEY2") private Integer donorKey2;
    @Column(name = "DONOR_KEY3") private Integer donorKey3;
    @Column(name = "DONOR_KEY4") private Integer donorKey4;


    /**
     * CONSTRUCTORS
     */
    public DonorTaxUnitPolicy(){}

    public DonorTaxUnitPolicy(int fy, DonorTaxUnit tu) {
        this.fromYear = fy;
        disposableIncomePerMonth = 0.0;
        originalIncomePerMonth = 0.0;
        earningsPerMonth = 0.0;
        secondIncomePerMonth = 0.0;
        childcareCostPerMonth = 0.0;
        benMeansTestPerMonth = 0.0;
        benNonMeansTestPerMonth = 0.0;
        taxUnit = tu;
    }


    /**
     * GETTERS AND SETTERS
     */
    public int getFromYear() {
        if (fromYear==null)
            throw new RuntimeException("attempt to get from year before instantiated");
        return this.fromYear;
    }
    public Integer getSystemYear() {
        return this.systemYear;
    }
    public void setSystemYear(int policyYear) {
        this.systemYear = policyYear;
    }
    public double getDisposableIncomePerMonth() {
        if (disposableIncomePerMonth==null)
            throw new RuntimeException("attempt to get disposable income before instantiated");
        return this.disposableIncomePerMonth;
    }
    public void setDisposableIncomePerMonth(double value) {
        this.disposableIncomePerMonth = value;
    }
    public double getOriginalIncomePerMonth() {
        if (originalIncomePerMonth==null)
            throw new RuntimeException("attempt to get original income before instantiated");
        return originalIncomePerMonth;
    }
    public double getNormalisedOriginalIncomePerMonth() {
        return Parameters.normaliseMonthlyIncome(systemYear, getOriginalIncomePerMonth());
    }
    public double getNormalisedSecondIncomePerMonth() {
        double secondIncome = Math.max(0.0, Math.min(getSecondIncomePerMonth(), getOriginalIncomePerMonth() - getSecondIncomePerMonth()));
        return Parameters.normaliseMonthlyIncome(systemYear, secondIncome);
    }
    public double getNormalisedChildcareCostPerMonth() {
        return Parameters.normaliseMonthlyIncome(systemYear, getChildcareCostPerMonth());
    }
    public void setOriginalIncomePerMonth(double value) {
        this.originalIncomePerMonth = value;
    }
    public double getBenMeansTestPerMonth() {
        if (benMeansTestPerMonth ==null)
            throw new RuntimeException("attempt to get benefit amnount before instantiated");
        return benMeansTestPerMonth;
    }
    public void setBenMeansTestPerMonth(Double ils_benmt) { this.benMeansTestPerMonth = ils_benmt; }
    public double getBenNonMeansTestPerMonth() {
        if (benNonMeansTestPerMonth ==null)
            throw new RuntimeException("attempt to get benefit amnount before instantiated");
        return benNonMeansTestPerMonth;
    }
    public void setBenNonMeansTestPerMonth(Double ils_bennt) { this.benNonMeansTestPerMonth = ils_bennt; }
    public DonorTaxUnit getTaxUnit() { return this.taxUnit;}
    public void setSecondIncomePerMonth(double inc) {
        secondIncomePerMonth = inc;
    }
    public double getSecondIncomePerMonth() {
        if (secondIncomePerMonth==null)
            throw new RuntimeException("attempt to get second income before instantiated");
        return secondIncomePerMonth;
    }
    public void setChildcareCostPerMonth(double cost) {
        childcareCostPerMonth = cost;
    }
    public double getChildcareCostPerMonth() {
        if (childcareCostPerMonth==null)
            throw new RuntimeException("attempt to get childcare costs before instantiated");
        return childcareCostPerMonth;
    }
    public void setEarningsPerMonth(double earnings) {
        earningsPerMonth = earnings;
    }
    public double getEarningsPerMonth() {
        if (earningsPerMonth==null)
            throw new RuntimeException("attempt to get earnings before instantiated");
        return earningsPerMonth;
    }
    public Integer getDonorKey(int regime) {
        if (regime==0) {
            return donorKey0;
        } else if (regime==1) {
            return donorKey1;
        } else if (regime==2) {
            return donorKey2;
        } else if (regime==3) {
            return donorKey3;
        } else if (regime==4) {
            return donorKey4;
        } else {
            throw new RuntimeException("request to get unrecognised donor key regime");
        }
    }
    public void setDonorKey(int regime, int key) {
        if (regime==0) {
            donorKey0 = key;
        } else if (regime==1) {
            donorKey1 = key;
        } else if (regime==2) {
            donorKey2 = key;
        } else if (regime==3) {
            donorKey3 = key;
        } else if (regime==4) {
            donorKey4 = key;
        } else {
            throw new RuntimeException("request to set unrecognised donor key regime");
        }
    }
}
