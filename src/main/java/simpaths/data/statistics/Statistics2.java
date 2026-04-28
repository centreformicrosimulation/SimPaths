package simpaths.data.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import microsim.data.db.PanelEntityKey;
import simpaths.data.Parameters;
import simpaths.model.Person;
import simpaths.model.SimPathsModel;
import simpaths.model.enums.Education;
import simpaths.model.enums.Indicator;

@Entity
public class Statistics2 {

    @Id
    private PanelEntityKey key = new PanelEntityKey(1L);

    //population shares in cohabiting relationships
    @Column(name = "pr_married_18_29")
    private double demMarried18to29Share;

    @Column(name = "pr_married_30_54")
    private double demMarried30to54Share;

    @Column(name = "pr_married_55_74")
    private double demMarried55to74Share;

    //average dependent children
    @Column(name = "avkids_18_29")
    private double demNChild18to29Avg;

    @Column(name = "avkids_30_54")
    private double demNChild30to54Avg;

    @Column(name = "avkids_55_74")
    private double demNChild55to74Avg;

    //average health
    @Column(name = "health_18_29")
    private double healthScore18to29Avg;

    @Column(name = "health_30_54")
    private double healthScore30to54Avg;

    @Column(name = "health_55_74")
    private double healthScore55to74Avg;

    //population shares disabled
    @Column(name = "pr_disabled_18_29")
    private double demDsbl18to29Share;

    @Column(name = "pr_disabled_30_54")
    private double demDsbl30to54Share;

    @Column(name = "pr_disabled_55_74")
    private double demDsbl55to74Share;

    //average labour status by age and gender
    @Column(name = "work_fulltime_18_29")
    private double labWorkFullTime18to29Share;

    @Column(name = "work_fulltime_30_54")
    private double labWorkFullTime30to54Share;

    @Column(name = "work_fulltime_55_74")
    private double labWorkFullTime55to74Share;

    @Column(name = "work_parttime_18_29")
    private double labWorkPartTime18to29Share;

    @Column(name = "work_parttime_30_54")
    private double labWorkPartTime30to54Share;

    @Column(name = "work_parttime_55_74")
    private double labWorkPartTime55to74Share;

    @Column(name = "work_none_18_29")
    private double labNoWork18to29Share;

    @Column(name = "work_none_30_54")
    private double labNoWork30to54Share;

    @Column(name = "work_none_55_74")
    private double labNoWork55to74Share;

    @Column(name = "work_none_18_74")
    private double labNoWork18to54Share;

    //employment income
    @Column(name = "labourIncome_18_29")
    private double statYLab18to29Avg;

    @Column(name = "labourIncome_30_54")
    private double statYLab30to54Avg;

    @Column(name = "labourIncome_55_74")
    private double statYLab55to74Avg;

    //investment income
    @Column(name = "investmentIncome_18_29")
    private double statYInvest18to29Avg;

    @Column(name = "investmentIncome_30_54")
    private double statYInvest30to54Avg;

    @Column(name = "investmentIncome_55_74")
    private double statYInvest55to74Avg;

    //pension income
    @Column(name = "pensionIncome_18_29")
    private double statYPens18to29Avg;

    @Column(name = "pensionIncome_30_54")
    private double statYPens30to54Avg;

    @Column(name = "pensionIncome_55_74")
    private double statYPens55to74Avg;

    //disposable income
    @Column(name = "disposableIncome_18_29")
    private double statYDisp18to29Avg;

    @Column(name = "disposableIncome_30_54")
    private double statYDisp30to54Avg;

    @Column(name = "disposableIncome_55_74")
    private double statYDisp55to74Avg;

    //investment losses
    @Column(name = "investmentLosses_18_29")
    private double statInvestLoss18to29Avg;

    @Column(name = "investmentLosses_30_54")
    private double statInvestLoss30to54Avg;

    @Column(name = "investmentLosses_55_74")
    private double statInvestLoss55to74Avg;

    //disposable income gross of investment losses
    @Column(name = "dispInc_grossLosses_18_29")
    private double statYDispGrossOfLosses18to29Avg;

    @Column(name = "dispInc_grossLosses_30_54")
    private double statYDispGrossOfLosses30to54Avg;

    @Column(name = "dispInc_grossLosses_55_74")
    private double statYDispGrossOfLosses55to75Avg;

    //expenditure
    @Column(name = "expenditure_18_29")
    private double x18to29Avg;

    @Column(name = "expenditure_30_54")
    private double x30to54Avg;

    @Column(name = "expenditure_55_74")
    private double x55to74Avg;

    @Column(name = "expenditure_18_54")
    private double x18to54Avg;

    //consumption to leisure ratios
    @Column(name = "cons_to_leis_ratio")
    private double xToLeisureRatio;

    //wealth
    @Column(name = "wealth_18_29")
    private double wealth18to29Avg;

    @Column(name = "wealth_30_54")
    private double wealth30to54Avg;

    @Column(name = "wealth_55_74")
    private double wealth55to74Avg;

    @Column(name= "population_18_29")
    private double demPop18to29N;

    @Column(name= "population_30_54")
    private double demPop30to54N;

    @Column(name= "population_55_74")
    private double demPop55to74N;

    public double getAaconsToLeisRatio() {
        return xToLeisureRatio;
    }

    public void setAaconsToLeisRatio(double consToLeis) {
        this.xToLeisureRatio = consToLeis;
    }

    public double getPopulation18to29() {
        return demPop18to29N;
    }

    public void setPopulation18to29(double demPop18to29N) {
        this.demPop18to29N = demPop18to29N;
    }

    public double getPopulation55to74() {
        return demPop55to74N;
    }

    public void setPopulation30to54(double demPop30to54N) {
        this.demPop30to54N = demPop30to54N;
    }

    public double getPopulation30to54() {
        return demPop30to54N;
    }

    public void setPopulation55to74(double demPop55to74N) {
        this.demPop55to74N = demPop55to74N;
    }

    public double getPrMarried18to29() {
        return demMarried18to29Share;
    }

    public void setPrMarried18to29(double demMarried18to29Share) {
        this.demMarried18to29Share = demMarried18to29Share;
    }

    public double getPrMarried30to54() {
        return demMarried30to54Share;
    }

    public void setPrMarried30to54(double demMarried30to54Share) {
        this.demMarried30to54Share = demMarried30to54Share;
    }

    public double getPrMarried55to74() {
        return demMarried55to74Share;
    }

    public void setPrMarried55to74(double demMarried55to74Share) {
        this.demMarried55to74Share = demMarried55to74Share;
    }

    public double getAvkids18to29() {
        return demNChild18to29Avg;
    }

    public void setAvkids18to29(double demNChild18to29Avg) {
        this.demNChild18to29Avg = demNChild18to29Avg;
    }

    public double getAvkids30to54() {
        return demNChild30to54Avg;
    }

    public void setAvkids30to54(double demNChild30to54Avg) {
        this.demNChild30to54Avg = demNChild30to54Avg;
    }

    public double getAvkids55to74() {
        return demNChild55to74Avg;
    }

    public void setAvkids55to74(double demNChild55to74Avg) {
        this.demNChild55to74Avg = demNChild55to74Avg;
    }

    public double getHealth18to29() {
        return healthScore18to29Avg;
    }

    public void setHealth18to29(double healthScore18to29Avg) {
        this.healthScore18to29Avg = healthScore18to29Avg;
    }

    public double getHealth30to54() {
        return healthScore30to54Avg;
    }

    public void setHealth30to54(double healthScore30to54Avg) {
        this.healthScore30to54Avg = healthScore30to54Avg;
    }

    public double getHealth55to74() {
        return healthScore55to74Avg;
    }

    public void setHealth55to74(double healthScore55to74Avg) {
        this.healthScore55to74Avg = healthScore55to74Avg;
    }

    public double getPrDisabled18to29() {
        return demDsbl18to29Share;
    }

    public void setPrDisabled18to29(double demDsbl18to29Share) {
        this.demDsbl18to29Share = demDsbl18to29Share;
    }

    public double getPrDisabled30to54() {
        return demDsbl30to54Share;
    }

    public void setPrDisabled30to54(double demDsbl30to54Share) {
        this.demDsbl30to54Share = demDsbl30to54Share;
    }

    public double getPrDisabled55to74() {
        return demDsbl55to74Share;
    }

    public void setPrDisabled55to74(double demDsbl55to74Share) {
        this.demDsbl55to74Share = demDsbl55to74Share;
    }

    public double getLabourIncome18to29() {
        return statYLab18to29Avg;
    }

    public void setLabourIncome18to29(double statYLab18to29Avg) {
        this.statYLab18to29Avg = statYLab18to29Avg;
    }

    public double getLabourIncome30to54() {
        return statYLab30to54Avg;
    }

    public void setLabourIncome30to54(double statYLab30to54Avg) {
        this.statYLab30to54Avg = statYLab30to54Avg;
    }

    public double getLabourIncome55to74() {
        return statYLab55to74Avg;
    }

    public void setLabourIncome55to74(double statYLab55to74Avg) {
        this.statYLab55to74Avg = statYLab55to74Avg;
    }

    public double getInvestmentIncome18to29() {
        return statYInvest18to29Avg;
    }

    public void setInvestmentIncome18to29(double statYInvest18to29Avg) {
        this.statYInvest18to29Avg = statYInvest18to29Avg;
    }

    public double getInvestmentIncome30to54() {
        return statYInvest30to54Avg;
    }

    public void setInvestmentIncome30to54(double statYInvest30to54Avg) {
        this.statYInvest30to54Avg = statYInvest30to54Avg;
    }

    public double getInvestmentIncome55to74() {
        return statYInvest55to74Avg;
    }

    public void setInvestmentIncome55to74(double statYInvest55to74Avg) {
        this.statYInvest55to74Avg = statYInvest55to74Avg;
    }

    public double getPensionIncome18to29() {
        return statYPens18to29Avg;
    }

    public void setPensionIncome18to29(double statYPens18to29Avg) {
        this.statYPens18to29Avg = statYPens18to29Avg;
    }

    public double getPensionIncome30to54() {
        return statYPens30to54Avg;
    }

    public void setPensionIncome30to54(double statYPens30to54Avg) {
        this.statYPens30to54Avg = statYPens30to54Avg;
    }

    public double getPensionIncome55to74() {
        return statYPens55to74Avg;
    }

    public void setPensionIncome55to74(double statYPens55to74Avg) {
        this.statYPens55to74Avg = statYPens55to74Avg;
    }

    public double getAadisposableIncome18to29() {
        return statYDisp18to29Avg;
    }

    public void setAadisposableIncome18to29(double statYDisp18to29Avg) {
        this.statYDisp18to29Avg = statYDisp18to29Avg;
    }

    public double getAadisposableIncome30to54() {
        return statYDisp30to54Avg;
    }

    public void setAadisposableIncome30to54(double statYDisp30to54Avg) {
        this.statYDisp30to54Avg = statYDisp30to54Avg;
    }

    public double getAadisposableIncome55to74() {
        return statYDisp55to74Avg;
    }

    public void setAadisposableIncome55to74(double statYDisp55to74Avg) {
        this.statYDisp55to74Avg = statYDisp55to74Avg;
    }

    public double getInvestmentLosses18to29() {
        return statInvestLoss18to29Avg;
    }

    public void setInvestmentLosses18to29(double statInvestLoss18to29Avg) {
        this.statInvestLoss18to29Avg = statInvestLoss18to29Avg;
    }

    public double getInvestmentLosses30to54() {
        return statInvestLoss30to54Avg;
    }

    public void setInvestmentLosses30to54(double statInvestLoss30to54Avg) {
        this.statInvestLoss30to54Avg = statInvestLoss30to54Avg;
    }

    public double getInvestmentLosses55to74() {
        return statInvestLoss55to74Avg;
    }

    public void setInvestmentLosses55to74(double statInvestLoss55to74Avg) {
        this.statInvestLoss55to74Avg = statInvestLoss55to74Avg;
    }

    public double getDispIncomeGrossOfLosses18to29() {
        return statYDispGrossOfLosses18to29Avg;
    }

    public void setDispIncomeGrossOfLosses18to29(double statYDispGrossOfLosses18to29Avg) {
        this.statYDispGrossOfLosses18to29Avg = statYDispGrossOfLosses18to29Avg;
    }

    public double getDispIncomeGrossOfLosses30to54() {
        return statYDispGrossOfLosses30to54Avg;
    }

    public void setDispIncomeGrossOfLosses30to54(double statYDispGrossOfLosses30to54Avg) {
        this.statYDispGrossOfLosses30to54Avg = statYDispGrossOfLosses30to54Avg;
    }

    public double getDispIncomeGrossOfLosses55to74() {
        return statYDispGrossOfLosses55to75Avg;
    }

    public void setDispIncomeGrossOfLosses55to74(double statYDispGrossOfLosses55to75Avg) {
        this.statYDispGrossOfLosses55to75Avg = statYDispGrossOfLosses55to75Avg;
    }

    public double getAaexpenditure18to29() {
        return x18to29Avg;
    }

    public void setAaexpenditure18to29(double x18to29Avg) {
        this.x18to29Avg = x18to29Avg;
    }

    public double getAaexpenditure30to54() {
        return x30to54Avg;
    }

    public void setAaexpenditure30to54(double x30to54Avg) {
        this.x30to54Avg = x30to54Avg;
    }

    public double getAaexpenditure55to74() {
        return x55to74Avg;
    }

    public void setAaexpenditure55to74(double x55to74Avg) {
        this.x55to74Avg = x55to74Avg;
    }

    public double getAaworkNone18to74() {
        return labNoWork18to54Share;
    }

    public void setAaworkNone18to74(double labNoWork18to54Share) {
        this.labNoWork18to54Share = labNoWork18to54Share;
    }

    public double getAaexpenditure18to54() {
        return x18to54Avg;
    }

    public void setAaexpenditure18to54(double x18to54Avg) {
        this.x18to54Avg = x18to54Avg;
    }

    public double getWealth18to29() {
        return wealth18to29Avg;
    }

    public void setWealth18to29(double wealth18to29Avg) {
        this.wealth18to29Avg = wealth18to29Avg;
    }

    public double getWealth30to54() {
        return wealth30to54Avg;
    }

    public void setWealth30to54(double wealth30to54Avg) {
        this.wealth30to54Avg = wealth30to54Avg;
    }

    public double getWealth55to74() {
        return wealth55to74Avg;
    }

    public void setWealth55to74(double wealth55to74Avg) {
        this.wealth55to74Avg = wealth55to74Avg;
    }

    public double getAworkFulltime18to29() {
        return labWorkFullTime18to29Share;
    }

    public void setAworkFulltime18to29(double labWorkFullTime18to29Share) {
        this.labWorkFullTime18to29Share = labWorkFullTime18to29Share;
    }

    public double getAworkFulltime30to54() {
        return labWorkFullTime30to54Share;
    }

    public void setAworkFulltime30to54(double labWorkFullTime30to54Share) {
        this.labWorkFullTime30to54Share = labWorkFullTime30to54Share;
    }

    public double getAworkFulltime55to74() {
        return labWorkFullTime55to74Share;
    }

    public void setAworkFulltime55to74(double labWorkFullTime55to74Share) {
        this.labWorkFullTime55to74Share = labWorkFullTime55to74Share;
    }

    public double getAworkParttime18to29() {
        return labWorkPartTime18to29Share;
    }

    public void setAworkParttime18to29(double labWorkPartTime18to29Share) {
        this.labWorkPartTime18to29Share = labWorkPartTime18to29Share;
    }

    public double getAworkParttime30to54() {
        return labWorkPartTime30to54Share;
    }

    public void setAworkParttime30to54(double labWorkPartTime30to54Share) {
        this.labWorkPartTime30to54Share = labWorkPartTime30to54Share;
    }

    public double getAworkParttime55to74() {
        return labWorkPartTime55to74Share;
    }

    public void setAworkParttime55to74(double labWorkPartTime55to74Share) {
        this.labWorkPartTime55to74Share = labWorkPartTime55to74Share;
    }

    public double getAaworkNone18to29() {
        return labNoWork18to29Share;
    }

    public void setAaworkNone18to29(double labNoWork18to29Share) {
        this.labNoWork18to29Share = labNoWork18to29Share;
    }

    public double getAaworkNone30to54() {
        return labNoWork30to54Share;
    }

    public void setAaworkNone30to54(double labNoWork30to54Share) {
        this.labNoWork30to54Share = labNoWork30to54Share;
    }

    public double getAaworkNone55to74() {
        return labNoWork55to74Share;
    }

    public void setAaworkNone55to74(double labNoWork55to74Share) {
        this.labNoWork55to74Share = labNoWork55to74Share;
    }

    public void update(SimPathsModel model) {

        // initialise outputs
        double[] prMarr = {0.,0.,0.};
        double[] avkids = {0.,0.,0.};
        double[] health = {0.,0.,0.};
        double[] prDisa = {0.,0.,0.};
        double[] workFT = {0.,0.,0.};
        double[] workPT = {0.,0.,0.};
        double[] workNn = {0.,0.,0.,0.};
        double[] labInc = {0.,0.,0.};
        double[] invInc = {0.,0.,0.};
        double[] invLosses = {0.,0.,0.};
        double[] penInc = {0.,0.,0.};
        double[] disInc = {0.,0.,0.};
        double[] grossDisInc = {0.,0.,0.};
        double[] expen = {0.,0.,0.,0.};
        double[] wealth = {0.,0.,0.};
        double[] popula = {0.,0.,0.};
        double ctlNG = 0.0, ctlG = 0.0;
        double numberNG = 0.0, numberG = 0.0;
        for (Person person : model.getPersons()) {
            // loop over entire population

            int ii = -1;
            if (person.getDemAge()>=18 && person.getDemAge()<=29) {
                ii = 0;
            } else if (person.getDemAge()>=30 && person.getDemAge()<=54) {
                ii = 1;
            } else if (person.getDemAge()>=55 && person.getDemAge()<=74) {
                ii = 2;
            }
            if (ii>=0) {

                double es = person.getBenefitUnit().getEquivalisedWeight();
                
                prMarr[ii] += person.getCohabiting();
                avkids[ii] += person.getBenefitUnit().getNumberChildrenAll();
                health[ii] += person.getHealthSelfRatedValue();
                prDisa[ii] += (Indicator.True.equals(person.getHealthDsblLongtermFlag()))? 1.0: 0.0;
                labInc[ii] += person.getEarningsWeekly();
                if ((double)person.getLabourSupplyHoursWeekly() > Parameters.MIN_HOURS_FULL_TIME_EMPLOYED)
                    workFT[ii] += 1.0;
                else if ((double)person.getLabourSupplyHoursWeekly() > 1.0)
                    workPT[ii] += 1.0;
                else {
                    workNn[ii] += 1.0;
                    workNn[3] += 1.0;
                }

                invInc[ii] += person.getBenefitUnit().getInvestmentIncomeAnnual() / 12.0 / es;
                penInc[ii] += person.getBenefitUnit().getPensionIncomeAnnual() / 12.0 / es;
                disInc[ii] += person.getBenefitUnit().getDisposableIncomeMonthlyNoNull() / es;
                if (person.getBenefitUnit().getInvestmentIncomeAnnual()<0.0) {
                    invLosses[ii] += person.getBenefitUnit().getInvestmentIncomeAnnual() / 12.0 / es;
                    grossDisInc[ii] += (person.getBenefitUnit().getDisposableIncomeMonthlyNoNull() -
                            person.getBenefitUnit().getInvestmentIncomeAnnual() / 12.0) / es;
                } else {
                    grossDisInc[ii] += person.getBenefitUnit().getDisposableIncomeMonthlyNoNull() / es;
                }
                double expenditurePerMonth = person.getBenefitUnit().getDiscretionaryConsumptionPerYear(false) / 12.0 +
                        person.getBenefitUnit().getChildcareCostPerWeek(false) * Parameters.WEEKS_PER_MONTH +
                        person.getBenefitUnit().getSocialCareCostPerWeek(false) * Parameters.WEEKS_PER_MONTH;
                if (expenditurePerMonth > 0.0) {
                    expenditurePerMonth /= es;
                    expen[ii] += Math.log(expenditurePerMonth);
                    if (person.getDemAge()>=18 && person.getDemAge()<=54) {
                        expen[3] += Math.log(expenditurePerMonth);
                    }
                }
                if (person.getDemAge()>=55 && person.getDemAge()<=60) {

                    if (Education.High.equals(person.getEduHighestC4())) {
                        numberG += 1.0;
                        ctlG += expenditurePerMonth / Parameters.WEEKS_PER_MONTH / person.getLeisureHoursPerWeek();
                    } else {
                        numberNG += 1.0;
                        ctlNG += expenditurePerMonth / Parameters.WEEKS_PER_MONTH / person.getLeisureHoursPerWeek();
                    }
                }
                wealth[ii] += person.getBenefitUnit().getLiquidWealth(false) / es;
                popula[ii] += 1.0;
            }
        }
        if (numberG>0.1) {
            ctlG /= numberG;
        }
        if (numberNG>0.1) {
            ctlNG /= numberNG;
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
                expen[ii] = Math.exp(expen[ii] / popula[ii]);
                wealth[ii] /= popula[ii];
            }
        }
        workNn[3] /= (popula[0]+popula[1]+popula[2]);
        expen[3] = Math.exp(expen[3] / (popula[0]+popula[1]));

        // update calibration statistics as differences to target moments
        workNn[0] -= 0.3427271; //2019 - pooled UKHLS data (ukhls_pooled_all_obs_02.dta)
        workNn[1] -= 0.2045448;
        workNn[2] -= 0.6011462;
        workNn[3] -= 0.3808046;

        expen[0] -= 165.8951 * Parameters.WEEKS_PER_MONTH;
        expen[1] -= 309.2464 * Parameters.WEEKS_PER_MONTH;
        expen[2] -= 323.1081 * Parameters.WEEKS_PER_MONTH;
        expen[3] -= 255.13679 * Parameters.WEEKS_PER_MONTH;

        disInc[0] -= 295.8024 * Parameters.WEEKS_PER_MONTH;
        disInc[1] -= 482.8467 * Parameters.WEEKS_PER_MONTH;
        disInc[2] -= 478.0918 * Parameters.WEEKS_PER_MONTH;

        double ctlRatio = ctlG / ctlNG - 1.361384;


        // map statistics to outputs
        setPrMarried18to29(prMarr[0]);
        setPrMarried30to54(prMarr[1]);
        setPrMarried55to74(prMarr[2]);

        setAvkids18to29(avkids[0]);
        setAvkids30to54(avkids[1]);
        setAvkids55to74(avkids[2]);

        setHealth18to29(health[0]);
        setHealth30to54(health[1]);
        setHealth55to74(health[2]);

        setPrDisabled18to29(prDisa[0]);
        setPrDisabled30to54(prDisa[1]);
        setPrDisabled55to74(prDisa[2]);

        setAworkFulltime18to29(workFT[0]);
        setAworkFulltime30to54(workFT[1]);
        setAworkFulltime55to74(workFT[2]);

        setAworkParttime18to29(workPT[0]);
        setAworkParttime30to54(workPT[1]);
        setAworkParttime55to74(workPT[2]);

        setAaworkNone18to29(workNn[0]);
        setAaworkNone30to54(workNn[1]);
        setAaworkNone55to74(workNn[2]);
        setAaworkNone18to74(workNn[3]);

        setLabourIncome18to29(labInc[0]);
        setLabourIncome30to54(labInc[1]);
        setLabourIncome55to74(labInc[2]);

        setInvestmentIncome18to29(invInc[0]);
        setInvestmentIncome30to54(invInc[1]);
        setInvestmentIncome55to74(invInc[2]);

        setPensionIncome18to29(penInc[0]);
        setPensionIncome30to54(penInc[1]);
        setPensionIncome55to74(penInc[2]);

        setAadisposableIncome18to29(disInc[0]);
        setAadisposableIncome30to54(disInc[1]);
        setAadisposableIncome55to74(disInc[2]);

        setInvestmentLosses18to29(invLosses[0]);
        setInvestmentLosses30to54(invLosses[1]);
        setInvestmentLosses55to74(invLosses[2]);

        setDispIncomeGrossOfLosses18to29(grossDisInc[0]);
        setDispIncomeGrossOfLosses30to54(grossDisInc[1]);
        setDispIncomeGrossOfLosses55to74(grossDisInc[2]);

        setAaexpenditure18to29(expen[0]);
        setAaexpenditure30to54(expen[1]);
        setAaexpenditure55to74(expen[2]);
        setAaexpenditure18to54(expen[3]);

        setAaconsToLeisRatio(ctlRatio);

        setWealth18to29(wealth[0]);
        setWealth30to54(wealth[1]);
        setWealth55to74(wealth[2]);

        setPopulation18to29(popula[0]);
        setPopulation30to54(popula[1]);
        setPopulation55to74(popula[2]);
    }
}
