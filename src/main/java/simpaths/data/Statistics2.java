package simpaths.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import microsim.data.db.PanelEntityKey;
import simpaths.model.Person;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.Indicator;
import simpaths.model.enums.TimeSeriesVariable;

@Entity
public class Statistics2 {

    @Id
    private PanelEntityKey key = new PanelEntityKey(1L);

    //population shares in cohabiting relationships
    @Column(name = "pr_married_20_44")
    private double prMarried20to44;

    @Column(name = "pr_married_45_64")
    private double prMarried45to64;

    @Column(name = "pr_married_65to84")
    private double prMarried65to84;

    //average dependent children
    @Column(name = "avkids_20_44")
    private double avkids20to44;

    @Column(name = "avkids_45_64")
    private double avkids45to64;

    @Column(name = "avkids_65to84")
    private double avkids65to84;

    //average health
    @Column(name = "health_20_44")
    private double health20to44;

    @Column(name = "health_45_64")
    private double health45to64;

    @Column(name = "health_65to84")
    private double health65to84;

    //population shares disabled
    @Column(name = "pr_disabled_20_44")
    private double prDisabled20to44;

    @Column(name = "pr_disabled_45_64")
    private double prDisabled45to64;

    @Column(name = "pr_disabled_65to84")
    private double prDisabled65to84;

    //average hours employed
    @Column(name = "hoursWork_20_44")
    private double hoursWork20to44;

    @Column(name = "hoursWork_45_64")
    private double hoursWork45to64;

    @Column(name = "hoursWork_65to84")
    private double hoursWork65to84;

    //employment income
    @Column(name = "labourIncome_20_44")
    private double labourIncome20to44;

    @Column(name = "labourIncome_45_64")
    private double labourIncome45to64;

    @Column(name = "labourIncome_65to84")
    private double labourIncome65to84;

    //investment income
    @Column(name = "investmentIncome_20_44")
    private double investmentIncome20to44;

    @Column(name = "investmentIncome_45_64")
    private double investmentIncome45to64;

    @Column(name = "investmentIncome_65to84")
    private double investmentIncome65to84;

    //pension income
    @Column(name = "pensionIncome_20_44")
    private double pensionIncome20to44;

    @Column(name = "pensionIncome_45_64")
    private double pensionIncome45to64;

    @Column(name = "pensionIncome_65to84")
    private double pensionIncome65to84;

    //disposable income
    @Column(name = "disposableIncome_20_44")
    private double disposableIncome20to44;

    @Column(name = "disposableIncome_45_64")
    private double disposableIncome45to64;

    @Column(name = "disposableIncome_65to84")
    private double disposableIncome65to84;

    //discretionary expenditure
    @Column(name = "dexpenditure_20_44")
    private double dexpenditure20to44;

    @Column(name = "dexpenditure_45_64")
    private double dexpenditure45to64;

    @Column(name = "dexpenditure_65to84")
    private double dexpenditure65to84;

    //committed expenditure
    @Column(name = "cexpenditure_20_44")
    private double cexpenditure20to44;

    @Column(name = "cexpenditure_45_64")
    private double cexpenditure45to64;

    @Column(name = "cexpenditure_65to84")
    private double cexpenditure65to84;

    //wealth
    @Column(name = "wealth_20_44")
    private double wealth20to44;

    @Column(name = "wealth_45_64")
    private double wealth45to64;

    @Column(name = "wealth_65to84")
    private double wealth65to84;

    @Column(name= "population_20_44")
    private double population20to44;

    @Column(name= "population_45_64")
    private double population45to64;

    @Column(name= "population_65to84")
    private double population65to84;

    @Column(name= "social_care_adj_factor")
    private double socialCareAdjustmentFactor;

    @Column(name = "partnership_adj_factor")
    private double partnershipAdjustmentFactor;

    @Column(name = "utility_adj_factor")
    private double utilityAdjustmentFactor;

    public double getPartnershipAdjustmentFactor() {
        return partnershipAdjustmentFactor;
    }

    public void setPartnershipAdjustmentFactor(double partnershipAdjustmentFactor) {
        this.partnershipAdjustmentFactor = partnershipAdjustmentFactor;
    }

    public double getUtilityAdjustmentFactor() {
        return utilityAdjustmentFactor;
    }

    public void setUtilityAdjustmentFactor(double utilityAdjustmentFactor) {
        this.utilityAdjustmentFactor = utilityAdjustmentFactor;
    }

    public double getSocialCareAdjustmentFactor() { return socialCareAdjustmentFactor; }
    public void setSocialCareAdjustmentFactor(double factor) {socialCareAdjustmentFactor = factor;}

    public double getPopulation20to44() {
        return population20to44;
    }

    public void setPopulation20to44(double population20to44) {
        this.population20to44 = population20to44;
    }

    public double getPopulation65to84() {
        return population65to84;
    }

    public void setPopulation45to64(double population45to64) {
        this.population45to64 = population45to64;
    }

    public double getPopulation45to64() {
        return population45to64;
    }

    public void setPopulation65to84(double population65to84) {
        this.population65to84 = population65to84;
    }

    public double getPrMarried20to44() {
        return prMarried20to44;
    }

    public void setPrMarried20to44(double prMarried20to44) {
        this.prMarried20to44 = prMarried20to44;
    }

    public double getPrMarried45to64() {
        return prMarried45to64;
    }

    public void setPrMarried45to64(double prMarried45to64) {
        this.prMarried45to64 = prMarried45to64;
    }

    public double getPrMarried65to84() {
        return prMarried65to84;
    }

    public void setPrMarried65to84(double prMarried65to84) {
        this.prMarried65to84 = prMarried65to84;
    }

    public double getAvkids20to44() {
        return avkids20to44;
    }

    public void setAvkids20to44(double avkids20to44) {
        this.avkids20to44 = avkids20to44;
    }

    public double getAvkids45to64() {
        return avkids45to64;
    }

    public void setAvkids45to64(double avkids45to64) {
        this.avkids45to64 = avkids45to64;
    }

    public double getAvkids65to84() {
        return avkids65to84;
    }

    public void setAvkids65to84(double avkids65to84) {
        this.avkids65to84 = avkids65to84;
    }

    public double getHealth20to44() {
        return health20to44;
    }

    public void setHealth20to44(double health20to44) {
        this.health20to44 = health20to44;
    }

    public double getHealth45to64() {
        return health45to64;
    }

    public void setHealth45to64(double health45to64) {
        this.health45to64 = health45to64;
    }

    public double getHealth65to84() {
        return health65to84;
    }

    public void setHealth65to84(double health65to84) {
        this.health65to84 = health65to84;
    }

    public double getPrDisabled20to44() {
        return prDisabled20to44;
    }

    public void setPrDisabled20to44(double prDisabled20to44) {
        this.prDisabled20to44 = prDisabled20to44;
    }

    public double getPrDisabled45to64() {
        return prDisabled45to64;
    }

    public void setPrDisabled45to64(double prDisabled45to64) {
        this.prDisabled45to64 = prDisabled45to64;
    }

    public double getPrDisabled65to84() {
        return prDisabled65to84;
    }

    public void setPrDisabled65to84(double prDisabled65to84) {
        this.prDisabled65to84 = prDisabled65to84;
    }

    public double getHoursWork20to44() {
        return hoursWork20to44;
    }

    public void setHoursWork20to44(double hoursWork20to44) {
        this.hoursWork20to44 = hoursWork20to44;
    }

    public double getHoursWork45to64() {
        return hoursWork45to64;
    }

    public void setHoursWork45to64(double hoursWork45to64) {
        this.hoursWork45to64 = hoursWork45to64;
    }

    public double getHoursWork65to84() {
        return hoursWork65to84;
    }

    public void setHoursWork65to84(double hoursWork65to84) {
        this.hoursWork65to84 = hoursWork65to84;
    }

    public double getLabourIncome20to44() {
        return labourIncome20to44;
    }

    public void setLabourIncome20to44(double labourIncome20to44) {
        this.labourIncome20to44 = labourIncome20to44;
    }

    public double getLabourIncome45to64() {
        return labourIncome45to64;
    }

    public void setLabourIncome45to64(double labourIncome45to64) {
        this.labourIncome45to64 = labourIncome45to64;
    }

    public double getLabourIncome65to84() {
        return labourIncome65to84;
    }

    public void setLabourIncome65to84(double labourIncome65to84) {
        this.labourIncome65to84 = labourIncome65to84;
    }

    public double getInvestmentIncome20to44() {
        return investmentIncome20to44;
    }

    public void setInvestmentIncome20to44(double investmentIncome20to44) {
        this.investmentIncome20to44 = investmentIncome20to44;
    }

    public double getInvestmentIncome45to64() {
        return investmentIncome45to64;
    }

    public void setInvestmentIncome45to64(double investmentIncome45to64) {
        this.investmentIncome45to64 = investmentIncome45to64;
    }

    public double getInvestmentIncome65to84() {
        return investmentIncome65to84;
    }

    public void setInvestmentIncome65to84(double investmentIncome65to84) {
        this.investmentIncome65to84 = investmentIncome65to84;
    }

    public double getPensionIncome20to44() {
        return pensionIncome20to44;
    }

    public void setPensionIncome20to44(double pensionIncome20to44) {
        this.pensionIncome20to44 = pensionIncome20to44;
    }

    public double getPensionIncome45to64() {
        return pensionIncome45to64;
    }

    public void setPensionIncome45to64(double pensionIncome45to64) {
        this.pensionIncome45to64 = pensionIncome45to64;
    }

    public double getPensionIncome65to84() {
        return pensionIncome65to84;
    }

    public void setPensionIncome65to84(double pensionIncome65to84) {
        this.pensionIncome65to84 = pensionIncome65to84;
    }

    public double getDisposableIncome20to44() {
        return disposableIncome20to44;
    }

    public void setDisposableIncome20to44(double disposableIncome20to44) {
        this.disposableIncome20to44 = disposableIncome20to44;
    }

    public double getDisposableIncome45to64() {
        return disposableIncome45to64;
    }

    public void setDisposableIncome45to64(double disposableIncome45to64) {
        this.disposableIncome45to64 = disposableIncome45to64;
    }

    public double getDisposableIncome65to84() {
        return disposableIncome65to84;
    }

    public void setDisposableIncome65to84(double disposableIncome65to84) {
        this.disposableIncome65to84 = disposableIncome65to84;
    }

    public double getDexpenditure20to44() {
        return dexpenditure20to44;
    }

    public void setDexpenditure20to44(double dexpenditure20to44) {
        this.dexpenditure20to44 = dexpenditure20to44;
    }

    public double getDexpenditure45to64() {
        return dexpenditure45to64;
    }

    public void setDexpenditure45to64(double dexpenditure45to64) {
        this.dexpenditure45to64 = dexpenditure45to64;
    }

    public double getDexpenditure65to84() {
        return dexpenditure65to84;
    }

    public void setDexpenditure65to84(double dexpenditure65to84) {
        this.dexpenditure65to84 = dexpenditure65to84;
    }

    public double getCexpenditure20to44() {
        return cexpenditure20to44;
    }

    public void setCexpenditure20to44(double cexpenditure20to44) {
        this.cexpenditure20to44 = cexpenditure20to44;
    }

    public double getCexpenditure45to64() {
        return cexpenditure45to64;
    }

    public void setCexpenditure45to64(double cexpenditure45to64) {
        this.cexpenditure45to64 = cexpenditure45to64;
    }

    public double getCexpenditure65to84() {
        return cexpenditure65to84;
    }

    public void setCexpenditure65to84(double cexpenditure65to84) {
        this.cexpenditure65to84 = cexpenditure65to84;
    }

    public double getWealth20to44() {
        return wealth20to44;
    }

    public void setWealth20to44(double wealth20to44) {
        this.wealth20to44 = wealth20to44;
    }

    public double getWealth45to64() {
        return wealth45to64;
    }

    public void setWealth45to64(double wealth45to64) {
        this.wealth45to64 = wealth45to64;
    }

    public double getWealth65to84() {
        return wealth65to84;
    }

    public void setWealth65to84(double wealth65to84) {
        this.wealth65to84 = wealth65to84;
    }

    public void update(SimPathsModel model) {

        // initialise outputs
        double[] prMarr = {0.,0.,0.};
        double[] avkids = {0.,0.,0.};
        double[] health = {0.,0.,0.};
        double[] prDisa = {0.,0.,0.};
        double[] hoursW = {0.,0.,0.};
        double[] labInc = {0.,0.,0.};
        double[] invInc = {0.,0.,0.};
        double[] penInc = {0.,0.,0.};
        double[] disInc = {0.,0.,0.};
        double[] dexpen = {0.,0.,0.};
        double[] cexpen = {0.,0.,0.};
        double[] wealth = {0.,0.,0.};
        double[] popula = {0.,0.,0.};
        for (Person person : model.getPersons()) {
            // loop over entire population

            int ii = -1;
            if (person.getDag()>=20 && person.getDag()<=44) {
                ii = 0;
            } else if (person.getDag()>=45 && person.getDag()<=64) {
                ii = 1;
            } else if (person.getDag()>=65 && person.getDag()<=84) {
                ii = 2;
            }
            if (ii>=0) {

                double es = person.getBenefitUnit().getEquivalisedWeight();
                
                prMarr[ii] += person.getCohabiting();
                avkids[ii] += person.getBenefitUnit().getN_children_allAges();
                health[ii] += person.getDheValue();
                prDisa[ii] += (Indicator.True.equals(person.getDlltsd()))? 1.0: 0.0;
                hoursW[ii] += person.getDoubleLabourSupplyWeeklyHours();
                invInc[ii] += person.getBenefitUnit().getInvestmentIncomeAnnual() / 12.0 / es;
                penInc[ii] += person.getBenefitUnit().getPensionIncomeAnnual() / 12.0 / es;
                disInc[ii] += person.getBenefitUnit().getDisposableIncomeMonthly() / es;
                dexpen[ii] += person.getBenefitUnit().getDiscretionaryConsumptionPerYear(false) / 12.0 / es;
                cexpen[ii] += person.getBenefitUnit().getChildcareCostPerWeek(false) * Parameters.WEEKS_PER_MONTH / es;
                wealth[ii] += person.getBenefitUnit().getLiquidWealth(false) / es;
                popula[ii] += 1.0;
            }
        }
        for (int ii=0; ii<=2; ii++) {

            if (popula[ii]>=0) {

                prMarr[ii] /= popula[ii];
                avkids[ii] /= popula[ii];
                health[ii] /= popula[ii];
                prDisa[ii] /= popula[ii];
                hoursW[ii] /= popula[ii];
                invInc[ii] /= popula[ii];
                penInc[ii] /= popula[ii];
                disInc[ii] /= popula[ii];
                dexpen[ii] /= popula[ii];
                cexpen[ii] /= popula[ii];
                wealth[ii] /= popula[ii];
            }
        }


        // map statistics to outputs
        setPrMarried20to44(prMarr[0]);
        setPrMarried45to64(prMarr[1]);
        setPrMarried65to84(prMarr[2]);

        setAvkids20to44(avkids[0]);
        setAvkids45to64(avkids[1]);
        setAvkids65to84(avkids[2]);

        setHealth20to44(health[0]);
        setHealth45to64(health[1]);
        setHealth65to84(health[2]);

        setPrDisabled20to44(prDisa[0]);
        setPrDisabled45to64(prDisa[1]);
        setPrDisabled65to84(prDisa[2]);

        setHoursWork20to44(hoursW[0]);
        setHoursWork45to64(hoursW[1]);
        setHoursWork65to84(hoursW[2]);

        setLabourIncome20to44(labInc[0]);
        setLabourIncome45to64(labInc[1]);
        setLabourIncome65to84(labInc[2]);

        setInvestmentIncome20to44(invInc[0]);
        setInvestmentIncome45to64(invInc[1]);
        setInvestmentIncome65to84(invInc[2]);

        setPensionIncome20to44(penInc[0]);
        setPensionIncome45to64(penInc[1]);
        setPensionIncome65to84(penInc[2]);

        setDisposableIncome20to44(disInc[0]);
        setDisposableIncome45to64(disInc[1]);
        setDisposableIncome65to84(disInc[2]);

        setDexpenditure20to44(dexpen[0]);
        setDexpenditure45to64(dexpen[1]);
        setDexpenditure65to84(dexpen[2]);

        setCexpenditure20to44(cexpen[0]);
        setCexpenditure45to64(cexpen[1]);
        setCexpenditure65to84(cexpen[2]);

        setWealth20to44(wealth[0]);
        setWealth45to64(wealth[1]);
        setWealth65to84(wealth[2]);

        setPopulation20to44(popula[0]);
        setPopulation45to64(popula[1]);
        setPopulation65to84(popula[2]);

        setSocialCareAdjustmentFactor(Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.CareProvisionAdjustment));
        setPartnershipAdjustmentFactor(Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.PartnershipAdjustment));
        setUtilityAdjustmentFactor(Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.UtilityAdjustment));
    }
}
