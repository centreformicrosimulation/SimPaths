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
    @Column(name = "pr_married_20_39")
    private double prMarried20to39;

    @Column(name = "pr_married_40_59")
    private double prMarried40to59;

    @Column(name = "pr_married_60_79")
    private double prMarried60to79;

    //average dependent children
    @Column(name = "avkids_20_39")
    private double avkids20to39;

    @Column(name = "avkids_40_59")
    private double avkids40to59;

    @Column(name = "avkids_60_79")
    private double avkids60to79;

    //average health
    @Column(name = "health_20_39")
    private double health20to39;

    @Column(name = "health_40_59")
    private double health40to59;

    @Column(name = "health_60_79")
    private double health60to79;

    //population shares disabled
    @Column(name = "pr_disabled_20_39")
    private double prDisabled20to39;

    @Column(name = "pr_disabled_40_59")
    private double prDisabled40to59;

    @Column(name = "pr_disabled_60_79")
    private double prDisabled60to79;

    //average labour status by age and gender
    @Column(name = "work_fulltime_20_39")
    private double workFulltime20to39;

    @Column(name = "work_fulltime_40_59")
    private double workFulltime40to59;

    @Column(name = "work_fulltime_60_79")
    private double workFulltime60to79;

    @Column(name = "work_parttime_20_39")
    private double workParttime20to39;

    @Column(name = "work_parttime_40_59")
    private double workParttime40to59;

    @Column(name = "work_parttime_60_79")
    private double workParttime60to79;

    @Column(name = "work_none_20_39")
    private double workNone20to39;

    @Column(name = "work_none_40_59")
    private double workNone40to59;

    @Column(name = "work_none_60_79")
    private double workNone60to79;

    //employment income
    @Column(name = "labourIncome_20_39")
    private double labourIncome20to39;

    @Column(name = "labourIncome_40_59")
    private double labourIncome40to59;

    @Column(name = "labourIncome_60_79")
    private double labourIncome60to79;

    //investment income
    @Column(name = "investmentIncome_20_39")
    private double investmentIncome20to39;

    @Column(name = "investmentIncome_40_59")
    private double investmentIncome40to59;

    @Column(name = "investmentIncome_60_79")
    private double investmentIncome60to79;

    //pension income
    @Column(name = "pensionIncome_20_39")
    private double pensionIncome20to39;

    @Column(name = "pensionIncome_40_59")
    private double pensionIncome40to59;

    @Column(name = "pensionIncome_60_79")
    private double pensionIncome60to79;

    //disposable income
    @Column(name = "disposableIncome_20_39")
    private double disposableIncome20to39;

    @Column(name = "disposableIncome_40_59")
    private double disposableIncome40to59;

    @Column(name = "disposableIncome_60_79")
    private double disposableIncome60to79;

    //investment losses
    @Column(name = "investmentLosses_20_39")
    private double investmentLosses20to39;

    @Column(name = "investmentLosses_40_59")
    private double investmentLosses40to59;

    @Column(name = "investmentLosses_60_79")
    private double investmentLosses60to79;

    //disposable income gross of investment losses
    @Column(name = "dispInc_grossLosses_20_39")
    private double dispIncomeGrossOfLosses20to39;

    @Column(name = "dispInc_grossLosses_40_59")
    private double dispIncomeGrossOfLosses40to59;

    @Column(name = "dispInc_grossLosses_60_79")
    private double dispIncomeGrossOfLosses60to79;

    //discretionary expenditure
    @Column(name = "dexpenditure_20_39")
    private double dexpenditure20to39;

    @Column(name = "dexpenditure_40_59")
    private double dexpenditure40to59;

    @Column(name = "dexpenditure_60_79")
    private double dexpenditure60to79;

    //committed expenditure
    @Column(name = "cexpenditure_20_39")
    private double cexpenditure20to39;

    @Column(name = "cexpenditure_40_59")
    private double cexpenditure40to59;

    @Column(name = "cexpenditure_60_79")
    private double cexpenditure60to79;

    //wealth
    @Column(name = "wealth_20_39")
    private double wealth20to39;

    @Column(name = "wealth_40_59")
    private double wealth40to59;

    @Column(name = "wealth_60_79")
    private double wealth60to79;

    @Column(name= "population_20_39")
    private double population20to39;

    @Column(name= "population_40_59")
    private double population40to59;

    @Column(name= "population_60_79")
    private double population60to79;

    @Column(name= "social_care_adj_factor")
    private double socialCareAdjustmentFactor;

    @Column(name = "partnership_adj_factor")
    private double partnershipAdjustmentFactor;

    public double getPartnershipAdjustmentFactor() {
        return partnershipAdjustmentFactor;
    }

    public void setPartnershipAdjustmentFactor(double partnershipAdjustmentFactor) {
        this.partnershipAdjustmentFactor = partnershipAdjustmentFactor;
    }
    public double getSocialCareAdjustmentFactor() { return socialCareAdjustmentFactor; }
    public void setSocialCareAdjustmentFactor(double factor) {socialCareAdjustmentFactor = factor;}

    public double getPopulation20to39() {
        return population20to39;
    }

    public void setPopulation20to39(double population20to39) {
        this.population20to39 = population20to39;
    }

    public double getPopulation60to79() {
        return population60to79;
    }

    public void setPopulation40to59(double population40to59) {
        this.population40to59 = population40to59;
    }

    public double getPopulation40to59() {
        return population40to59;
    }

    public void setPopulation60to79(double population60to79) {
        this.population60to79 = population60to79;
    }

    public double getPrMarried20to39() {
        return prMarried20to39;
    }

    public void setPrMarried20to39(double prMarried20to39) {
        this.prMarried20to39 = prMarried20to39;
    }

    public double getPrMarried40to59() {
        return prMarried40to59;
    }

    public void setPrMarried40to59(double prMarried40to59) {
        this.prMarried40to59 = prMarried40to59;
    }

    public double getPrMarried60to79() {
        return prMarried60to79;
    }

    public void setPrMarried60to79(double prMarried60to79) {
        this.prMarried60to79 = prMarried60to79;
    }

    public double getAvkids20to39() {
        return avkids20to39;
    }

    public void setAvkids20to39(double avkids20to39) {
        this.avkids20to39 = avkids20to39;
    }

    public double getAvkids40to59() {
        return avkids40to59;
    }

    public void setAvkids40to59(double avkids40to59) {
        this.avkids40to59 = avkids40to59;
    }

    public double getAvkids60to79() {
        return avkids60to79;
    }

    public void setAvkids60to79(double avkids60to79) {
        this.avkids60to79 = avkids60to79;
    }

    public double getHealth20to39() {
        return health20to39;
    }

    public void setHealth20to39(double health20to39) {
        this.health20to39 = health20to39;
    }

    public double getHealth40to59() {
        return health40to59;
    }

    public void setHealth40to59(double health40to59) {
        this.health40to59 = health40to59;
    }

    public double getHealth60to79() {
        return health60to79;
    }

    public void setHealth60to79(double health60to79) {
        this.health60to79 = health60to79;
    }

    public double getPrDisabled20to39() {
        return prDisabled20to39;
    }

    public void setPrDisabled20to39(double prDisabled20to39) {
        this.prDisabled20to39 = prDisabled20to39;
    }

    public double getPrDisabled40to59() {
        return prDisabled40to59;
    }

    public void setPrDisabled40to59(double prDisabled40to59) {
        this.prDisabled40to59 = prDisabled40to59;
    }

    public double getPrDisabled60to79() {
        return prDisabled60to79;
    }

    public void setPrDisabled60to79(double prDisabled60to79) {
        this.prDisabled60to79 = prDisabled60to79;
    }

    public double getLabourIncome20to39() {
        return labourIncome20to39;
    }

    public void setLabourIncome20to39(double labourIncome20to39) {
        this.labourIncome20to39 = labourIncome20to39;
    }

    public double getLabourIncome40to59() {
        return labourIncome40to59;
    }

    public void setLabourIncome40to59(double labourIncome40to59) {
        this.labourIncome40to59 = labourIncome40to59;
    }

    public double getLabourIncome60to79() {
        return labourIncome60to79;
    }

    public void setLabourIncome60to79(double labourIncome60to79) {
        this.labourIncome60to79 = labourIncome60to79;
    }

    public double getInvestmentIncome20to39() {
        return investmentIncome20to39;
    }

    public void setInvestmentIncome20to39(double investmentIncome20to39) {
        this.investmentIncome20to39 = investmentIncome20to39;
    }

    public double getInvestmentIncome40to59() {
        return investmentIncome40to59;
    }

    public void setInvestmentIncome40to59(double investmentIncome40to59) {
        this.investmentIncome40to59 = investmentIncome40to59;
    }

    public double getInvestmentIncome60to79() {
        return investmentIncome60to79;
    }

    public void setInvestmentIncome60to79(double investmentIncome60to79) {
        this.investmentIncome60to79 = investmentIncome60to79;
    }

    public double getPensionIncome20to39() {
        return pensionIncome20to39;
    }

    public void setPensionIncome20to39(double pensionIncome20to39) {
        this.pensionIncome20to39 = pensionIncome20to39;
    }

    public double getPensionIncome40to59() {
        return pensionIncome40to59;
    }

    public void setPensionIncome40to59(double pensionIncome40to59) {
        this.pensionIncome40to59 = pensionIncome40to59;
    }

    public double getPensionIncome60to79() {
        return pensionIncome60to79;
    }

    public void setPensionIncome60to79(double pensionIncome60to79) {
        this.pensionIncome60to79 = pensionIncome60to79;
    }

    public double getDisposableIncome20to39() {
        return disposableIncome20to39;
    }

    public void setDisposableIncome20to39(double disposableIncome20to39) {
        this.disposableIncome20to39 = disposableIncome20to39;
    }

    public double getDisposableIncome40to59() {
        return disposableIncome40to59;
    }

    public void setDisposableIncome40to59(double disposableIncome40to59) {
        this.disposableIncome40to59 = disposableIncome40to59;
    }

    public double getDisposableIncome60to79() {
        return disposableIncome60to79;
    }

    public void setDisposableIncome60to79(double disposableIncome60to79) {
        this.disposableIncome60to79 = disposableIncome60to79;
    }

    public double getInvestmentLosses20to39() {
        return investmentLosses20to39;
    }

    public void setInvestmentLosses20to39(double investmentLosses20to39) {
        this.investmentLosses20to39 = investmentLosses20to39;
    }

    public double getInvestmentLosses40to59() {
        return investmentLosses40to59;
    }

    public void setInvestmentLosses40to59(double investmentLosses40to59) {
        this.investmentLosses40to59 = investmentLosses40to59;
    }

    public double getInvestmentLosses60to79() {
        return investmentLosses60to79;
    }

    public void setInvestmentLosses60to79(double investmentLosses60to79) {
        this.investmentLosses60to79 = investmentLosses60to79;
    }

    public double getDispIncomeGrossOfLosses20to39() {
        return dispIncomeGrossOfLosses20to39;
    }

    public void setDispIncomeGrossOfLosses20to39(double dispIncomeGrossOfLosses20to39) {
        this.dispIncomeGrossOfLosses20to39 = dispIncomeGrossOfLosses20to39;
    }

    public double getDispIncomeGrossOfLosses40to59() {
        return dispIncomeGrossOfLosses40to59;
    }

    public void setDispIncomeGrossOfLosses40to59(double dispIncomeGrossOfLosses40to59) {
        this.dispIncomeGrossOfLosses40to59 = dispIncomeGrossOfLosses40to59;
    }

    public double getDispIncomeGrossOfLosses60to79() {
        return dispIncomeGrossOfLosses60to79;
    }

    public void setDispIncomeGrossOfLosses60to79(double dispIncomeGrossOfLosses60to79) {
        this.dispIncomeGrossOfLosses60to79 = dispIncomeGrossOfLosses60to79;
    }

    public double getDexpenditure20to39() {
        return dexpenditure20to39;
    }

    public void setDexpenditure20to39(double dexpenditure20to39) {
        this.dexpenditure20to39 = dexpenditure20to39;
    }

    public double getDexpenditure40to59() {
        return dexpenditure40to59;
    }

    public void setDexpenditure40to59(double dexpenditure40to59) {
        this.dexpenditure40to59 = dexpenditure40to59;
    }

    public double getDexpenditure60to79() {
        return dexpenditure60to79;
    }

    public void setDexpenditure60to79(double dexpenditure60to79) {
        this.dexpenditure60to79 = dexpenditure60to79;
    }

    public double getCexpenditure20to39() {
        return cexpenditure20to39;
    }

    public void setCexpenditure20to39(double cexpenditure20to39) {
        this.cexpenditure20to39 = cexpenditure20to39;
    }

    public double getCexpenditure40to59() {
        return cexpenditure40to59;
    }

    public void setCexpenditure40to59(double cexpenditure40to59) {
        this.cexpenditure40to59 = cexpenditure40to59;
    }

    public double getCexpenditure60to79() {
        return cexpenditure60to79;
    }

    public void setCexpenditure60to79(double cexpenditure60to79) {
        this.cexpenditure60to79 = cexpenditure60to79;
    }

    public double getWealth20to39() {
        return wealth20to39;
    }

    public void setWealth20to39(double wealth20to39) {
        this.wealth20to39 = wealth20to39;
    }

    public double getWealth40to59() {
        return wealth40to59;
    }

    public void setWealth40to59(double wealth40to59) {
        this.wealth40to59 = wealth40to59;
    }

    public double getWealth60to79() {
        return wealth60to79;
    }

    public void setWealth60to79(double wealth60to79) {
        this.wealth60to79 = wealth60to79;
    }

    public double getWorkFulltime20to39() {
        return workFulltime20to39;
    }

    public void setWorkFulltime20to39(double workFulltime20to39) {
        this.workFulltime20to39 = workFulltime20to39;
    }

    public double getWorkFulltime40to59() {
        return workFulltime40to59;
    }

    public void setWorkFulltime40to59(double workFulltime40to59) {
        this.workFulltime40to59 = workFulltime40to59;
    }

    public double getWorkFulltime60to79() {
        return workFulltime60to79;
    }

    public void setWorkFulltime60to79(double workFulltime60to79) {
        this.workFulltime60to79 = workFulltime60to79;
    }

    public double getWorkParttime20to39() {
        return workParttime20to39;
    }

    public void setWorkParttime20to39(double workParttime20to39) {
        this.workParttime20to39 = workParttime20to39;
    }

    public double getWorkParttime40to59() {
        return workParttime40to59;
    }

    public void setWorkParttime40to59(double workParttime40to59) {
        this.workParttime40to59 = workParttime40to59;
    }

    public double getWorkParttime60to79() {
        return workParttime60to79;
    }

    public void setWorkParttime60to79(double workParttime60to79) {
        this.workParttime60to79 = workParttime60to79;
    }

    public double getWorkNone20to39() {
        return workNone20to39;
    }

    public void setWorkNone20to39(double workNone20to39) {
        this.workNone20to39 = workNone20to39;
    }

    public double getWorkNone40to59() {
        return workNone40to59;
    }

    public void setWorkNone40to59(double workNone40to59) {
        this.workNone40to59 = workNone40to59;
    }

    public double getWorkNone60to79() {
        return workNone60to79;
    }

    public void setWorkNone60to79(double workNone60to79) {
        this.workNone60to79 = workNone60to79;
    }

    public void update(SimPathsModel model) {

        // initialise outputs
        double[] prMarr = {0.,0.,0.};
        double[] avkids = {0.,0.,0.};
        double[] health = {0.,0.,0.};
        double[] prDisa = {0.,0.,0.};
        double[] workFT = {0.,0.,0.};
        double[] workPT = {0.,0.,0.};
        double[] workNn = {0.,0.,0.};
        double[] labInc = {0.,0.,0.};
        double[] invInc = {0.,0.,0.};
        double[] invLosses = {0.,0.,0.};
        double[] penInc = {0.,0.,0.};
        double[] disInc = {0.,0.,0.};
        double[] grossDisInc = {0.,0.,0.};
        double[] dexpen = {0.,0.,0.};
        double[] cexpen = {0.,0.,0.};
        double[] wealth = {0.,0.,0.};
        double[] popula = {0.,0.,0.};
        for (Person person : model.getPersons()) {
            // loop over entire population

            int ii = -1;
            if (person.getDag()>=20 && person.getDag()<=39) {
                ii = 0;
            } else if (person.getDag()>=40 && person.getDag()<=59) {
                ii = 1;
            } else if (person.getDag()>=60 && person.getDag()<=79) {
                ii = 2;
            }
            if (ii>=0) {

                double es = person.getBenefitUnit().getEquivalisedWeight();
                
                prMarr[ii] += person.getCohabiting();
                avkids[ii] += person.getBenefitUnit().getNumberChildrenAll();
                health[ii] += person.getDheValue();
                prDisa[ii] += (Indicator.True.equals(person.getDlltsd()))? 1.0: 0.0;
                labInc[ii] += person.getEarningsWeekly();
                if (person.getDoubleLabourSupplyWeeklyHours() > Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)
                    workFT[ii] += 1.0;
                else if (person.getDoubleLabourSupplyWeeklyHours() > 1.0)
                    workPT[ii] += 1.0;
                else
                    workNn[ii] += 1.0;
                invInc[ii] += person.getBenefitUnit().getInvestmentIncomeAnnual() / 12.0 / es;
                penInc[ii] += person.getBenefitUnit().getPensionIncomeAnnual() / 12.0 / es;
                disInc[ii] += person.getBenefitUnit().getDisposableIncomeMonthly() / es;
                if (person.getBenefitUnit().getInvestmentIncomeAnnual()<0.0) {
                    invLosses[ii] += person.getBenefitUnit().getInvestmentIncomeAnnual() / 12.0 / es;
                    grossDisInc[ii] += (person.getBenefitUnit().getDisposableIncomeMonthly() -
                            person.getBenefitUnit().getInvestmentIncomeAnnual() / 12.0) / es;
                } else {
                    grossDisInc[ii] += person.getBenefitUnit().getDisposableIncomeMonthly() / es;
                }
                dexpen[ii] += person.getBenefitUnit().getDiscretionaryConsumptionPerYear(false) / 12.0 / es;
                cexpen[ii] += person.getBenefitUnit().getChildcareCostPerWeek(false) * Parameters.WEEKS_PER_MONTH / es;
                wealth[ii] += person.getBenefitUnit().getLiquidWealth(false) / es;
                popula[ii] += 1.0;
            }
        }
        for (int ii=0; ii<=2; ii++) {

            if (popula[ii]>=0) {

                labInc[ii] /= (workFT[ii] + workPT[ii]);
                prMarr[ii] /= popula[ii];
                avkids[ii] /= popula[ii];
                health[ii] /= popula[ii];
                prDisa[ii] /= popula[ii];
                workFT[ii] /= popula[ii];
                workPT[ii] /= popula[ii];
                workNn[ii] /= popula[ii];
                invInc[ii] /= popula[ii];
                penInc[ii] /= popula[ii];
                disInc[ii] /= popula[ii];
                invLosses[ii] /= popula[ii];
                grossDisInc[ii] /= popula[ii];
                dexpen[ii] /= popula[ii];
                cexpen[ii] /= popula[ii];
                wealth[ii] /= popula[ii];
            }
        }


        // map statistics to outputs
        setPrMarried20to39(prMarr[0]);
        setPrMarried40to59(prMarr[1]);
        setPrMarried60to79(prMarr[2]);

        setAvkids20to39(avkids[0]);
        setAvkids40to59(avkids[1]);
        setAvkids60to79(avkids[2]);

        setHealth20to39(health[0]);
        setHealth40to59(health[1]);
        setHealth60to79(health[2]);

        setPrDisabled20to39(prDisa[0]);
        setPrDisabled40to59(prDisa[1]);
        setPrDisabled60to79(prDisa[2]);

        setWorkFulltime20to39(workFT[0]);
        setWorkFulltime40to59(workFT[1]);
        setWorkFulltime60to79(workFT[2]);

        setWorkParttime20to39(workPT[0]);
        setWorkParttime40to59(workPT[1]);
        setWorkParttime60to79(workPT[2]);

        setWorkNone20to39(workNn[0]);
        setWorkNone40to59(workNn[1]);
        setWorkNone60to79(workNn[2]);

        setLabourIncome20to39(labInc[0]);
        setLabourIncome40to59(labInc[1]);
        setLabourIncome60to79(labInc[2]);

        setInvestmentIncome20to39(invInc[0]);
        setInvestmentIncome40to59(invInc[1]);
        setInvestmentIncome60to79(invInc[2]);

        setPensionIncome20to39(penInc[0]);
        setPensionIncome40to59(penInc[1]);
        setPensionIncome60to79(penInc[2]);

        setDisposableIncome20to39(disInc[0]);
        setDisposableIncome40to59(disInc[1]);
        setDisposableIncome60to79(disInc[2]);

        setInvestmentLosses20to39(invLosses[0]);
        setInvestmentLosses40to59(invLosses[1]);
        setInvestmentLosses60to79(invLosses[2]);

        setDispIncomeGrossOfLosses20to39(grossDisInc[0]);
        setDispIncomeGrossOfLosses40to59(grossDisInc[1]);
        setDispIncomeGrossOfLosses60to79(grossDisInc[2]);

        setDexpenditure20to39(dexpen[0]);
        setDexpenditure40to59(dexpen[1]);
        setDexpenditure60to79(dexpen[2]);

        setCexpenditure20to39(cexpen[0]);
        setCexpenditure40to59(cexpen[1]);
        setCexpenditure60to79(cexpen[2]);

        setWealth20to39(wealth[0]);
        setWealth40to59(wealth[1]);
        setWealth60to79(wealth[2]);

        setPopulation20to39(popula[0]);
        setPopulation40to59(popula[1]);
        setPopulation60to79(popula[2]);

        setSocialCareAdjustmentFactor(Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.CareProvisionAdjustment));
        setPartnershipAdjustmentFactor(Parameters.getTimeSeriesValue(model.getYear()-1, TimeSeriesVariable.PartnershipAdjustment));
    }
}
