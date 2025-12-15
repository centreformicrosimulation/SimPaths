package simpaths.data.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import microsim.data.db.PanelEntityKey;

@Entity
public class Statistics {

	@Id
	private PanelEntityKey key = new PanelEntityKey(1L);

	@Column(name = "Gini_coefficient_individual_market_income_nationally")
	private double statYMktNatGini;

	@Column(name = "Gini_coefficient_equivalised_household_disposable_income_nationally")
	private double statYHhDispEquivNatGini;

	@Column(name = "Median_equivalised_household_disposable_income")
	private double yHhDispEquivP50;
	
	//Percentiles of ydses:
	@Column(name = "Ydses_p20")
	private double yHhQuintilesC5P20;
	
	@Column(name = "Ydses_p40")
	private double yHhQuintilesC5P40;
	
	@Column(name = "Ydses_p60")
	private double yHhQuintilesC5P60;
	
	@Column(name = "Ydses_p80")
	private double yHhQuintilesC5P80;

	//Percentiles of gross labour income:
	@Column(name = "Gross_Labour_Income_p20")
	private double yLabP20;

	@Column(name = "Gross_Labour_Income_p40")
	private double yLabP40;

	@Column(name = "Gross_Labour_Income_p60")
	private double yLabP60;

	@Column(name = "Gross_Labour_Income_p80")
	private double yLabP80;

	//Equivalised disposable income
	@Column(name = "EDI_p50")
	private double edi_p50;

	//Percentiles of SIndex:
	@Column(name = "SIndex_p50")
	private double sIndex_p50;

	////	Risk-of-poverty threshold is set at 60% of the national median equivalised household disposable income.
//	@Column(name = "Risk_of_poverty_threshold")
//	private double riskOfPovertyThreshold;
	
	public void setGiniPersonalGrossEarningsNational(double statYMktNatGini) {
		this.statYMktNatGini = statYMktNatGini;
	}
	
	public void setGiniEquivalisedHouseholdDisposableIncomeNational(double statYHhDispEquivNatGini) {
		this.statYHhDispEquivNatGini = statYHhDispEquivNatGini;
	}

	public double getMedianEquivalisedHouseholdDisposableIncome() {
		return yHhDispEquivP50;
	}

	public void setMedianEquivalisedHouseholdDisposableIncome(double yHhDispEquivP50) {
		this.yHhDispEquivP50 = yHhDispEquivP50;
	}
	
	public double getYdses_p20() {
		return yHhQuintilesC5P20;
	}

	public void setYdses_p20(double yHhQuintilesC5P20) {
		this.yHhQuintilesC5P20 = yHhQuintilesC5P20;
	}

	public double getYdses_p40() {
		return yHhQuintilesC5P40;
	}

	public void setYdses_p40(double yHhQuintilesC5P40) {
		this.yHhQuintilesC5P40 = yHhQuintilesC5P40;
	}

	public double getYdses_p60() {
		return yHhQuintilesC5P80;
	}

	public void setYdses_p60(double yHhQuintilesC5P80) {
		this.yHhQuintilesC5P80 = yHhQuintilesC5P80;
	}

	public double getYdses_p80() {
		return yHhQuintilesC5P80;
	}

	public void setYdses_p80(double yHhQuintilesC5P80) {
		this.yHhQuintilesC5P80 = yHhQuintilesC5P80;
	}

	public double getsIndex_p50() {
		return sIndex_p50;
	}

	public void setsIndex_p50(double sIndex_p50) {
		this.sIndex_p50 = sIndex_p50;
	}

	public double getGrossLabourIncome_p20() {
		return yLabP20;
	}

	public void setGrossLabourIncome_p20(double yLabP20) {
		this.yLabP20 = yLabP20;
	}

	public double getGrossLabourIncome_p40() {
		return yLabP40;
	}

	public void setGrossLabourIncome_p40(double yLabP40) {
		this.yLabP40 = yLabP40;
	}

	public double getGrossLabourIncome_p60() {
		return yLabP60;
	}

	public void setGrossLabourIncome_p60(double yLabP60) {
		this.yLabP60 = yLabP60;
	}

	public double getGrossLabourIncome_p80() {
		return yLabP80;
	}

	public void setGrossLabourIncome_p80(double yLabP80) {
		this.yLabP80 = yLabP80;
	}

	public double getEdi_p50() {
		return edi_p50;
	}

	public void setEdi_p50(double edi_p50) {
		this.edi_p50 = edi_p50;
	}

//	public double getRiskOfPovertyThreshold() {
//		return riskOfPovertyThreshold;
//	}
//
//	public void setRiskOfPovertyThreshold(double riskOfPovertyThreshold) {
//		this.riskOfPovertyThreshold = riskOfPovertyThreshold;
//	}

}
