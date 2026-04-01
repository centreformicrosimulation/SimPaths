/*******************************************************************************
* PROJECT:  		SimPaths UK
* SECTION:			Non-employment/non-benefit income
* OBJECT: 			Final Regresion Models 
* AUTHORS:			Patryk Bronka, Daria Popova, Justin van de Ven, 
* 					Ashley Burdett
* LAST UPDATE:		26 Mar 2026 (AB)
* COUNTRY: 			UK

* NOTES: 			 Models for split income variable
*                    - Capital returns
*                    - Private pension income  
*                       
*                       The income  do file must be run after
* 						reg_wages.do because it uses predicted wages. 
******************************************************************************/

clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000


/********************************* SET LOG FILE *******************************/
cap log close 
log using "${dir_log}/reg_income.log", replace


/********************************* SET EXCEL FILE *****************************/

putexcel set "$dir_results/reg_income", sheet("Info") replace
putexcel A1 = "Description:", bold
putexcel B1 = "This file contains regression estimates used by processes I1 (capital income), I2 (private pension, retired last year), I3 (private pension income, not retired last year) "
putexcel A2 = "Authors:"	
putexcel B2 = "Patryk Bronka, Justin Van de Ven, Daria Popova, Aleksandra Kolndrekaj" 
putexcel A3 = "Last edit: 18 Feb 2026 AK"

putexcel A5 = "Process:", bold
putexcel B5 = "Description:", bold

putexcel A7 = "Process I1a"
putexcel B7 = "Prob. receive capital income "

putexcel A8 = "Process I1b"
putexcel B8 = "Capital income amount"

putexcel A9 = "Process I2b"
putexcel B9 = "Private pension income amount"

putexcel A10 = "Process I3a"
putexcel B10 = "Prob. receive private pension income"

putexcel A11 = "Process I3b"
putexcel B11 = "Private pension income amount"


putexcel A17 = "Notes:", bold
putexcel B17 = "Estimation sample: UK_ipop2.dta with grossing up weight dwt" 
putexcel B18 = "Conditions for processes are defined as globals in master.do"
putexcel B19 = "Combined former capital income processes I3a and I3b and renamed as I1a and I1b"
putexcel B20 = "Income variables are IHS transformed."

putexcel set "$dir_results/reg_income", sheet("Gof") modify
putexcel A1 = "Goodness of fit", bold	

/********************************* PREPARE DATA *******************************/

* Prepare data on real growth of wages 
/*
import excel "${dir_external_data}/time_series_factor.xlsx", ///
	sheet("UK_gdp") firstrow clear // Import real growth index
	
rename Year stm
rename Value growth
gen base_val = growth if stm == 2015
sum base_val
replace base_val = r(mean)
replace growth= growth/base_val
drop base_val
replace stm = stm - 2000

save "$dir_external_data\growth_rates", replace
*/

* Load data 
use "${estimation_sample2}", clear //panel with predicted wages 

* Merge in growth rates 
merge m:1 stm using "$dir_external_data/growth_rates", keep(3) nogen ///
	keepusing(growth)

* Set data 
xtset idperson swv
sort idperson swv 

* adjust capital income 
sum ypncp, det
scalar p99 = r(p99)
replace ypncp = . if ypncp >= p99

* adjust pension income 
sum ypnoab, det
scalar p99 = r(p99)
replace ypnoab = . if ypnoab >= p99

*rename pedicted wage 
capture confirm variable pred_hourly_wage
if _rc == 0 {
    gen Hourly_wage = pred_hourly_wage
}

cap drop in_sample
cap drop p


/********************************** ESTIMATION ********************************/

* Run Stata programs to produce Excel file 
do "${dir_do}/programs.do" 


/*************** I1a: PROBABILITY OF RECEIVEING CAPITAL INCOME ****************/

display "${i1a_if_condition}"

logit receives_ypncp ///
    i.Ded i.Dgn c.Dag c.Dag_sq ///
	l.Dhe_pcs l.Dhe_mcs ///
	lc.Ypncp lc.Yplgrs_dv ///
	l2c.Yplgrs_dv l2c.Ypncp ///
	Ded_Dgn /*Ded_Dag Ded_Dag_sq*/ ///
	l.Ded_Dhe_pcs l.Ded_Dhe_mcs ///
	l.Ded_Ypncp l.Ded_Yplgrs_dv l2.Ded_Yplgrs_dv l2.Ded_Ypncp ///
	i.Deh_c4_Low i.Deh_c4_Medium i.Deh_c4_High ///
	li.Les_c4_Student li.Les_c4_NotEmployed li.Les_c4_Retired ///
	li.Dhhtp_c4_CoupleChildren li.Dhhtp_c4_SingleNoChildren ///
	li.Dhhtp_c4_SingleChildren ///
	$regions Year_transformed Y2020 Y2021 $ethnicity if ///
	 ${i1a_if_condition} [pw=${weight}], ///
	 vce(cluster idperson) base
	 
process_regression, domain("income") process("I1a") sheet("I1a") ///
	title("Process I1a: Prob. recieve capital income") ///
	gofrow(3) goflabel("I1a - Receive capital income ") ///
	ifcond("${i1a_if_condition}") probit	 
  	

/********************** I1b: AMOUNT OF CAPITAL INCOME *************************/

* DV: ypncp = Inverse hyperbolic sine (IHS) of gross capital income
display "${i1b_if_condition}"

reg ypncp i.Dgn c.Dag c.Dag_sq ///
          i.Deh_c4_Low i.Deh_c4_Medium i.Deh_c4_High /// 
          li.Les_c4_Student li.Les_c4_NotEmployed li.Les_c4_Retired /// 
          li.Dhhtp_c4_CoupleChildren li.Dhhtp_c4_SingleNoChildren ///
		  li.Dhhtp_c4_SingleChildren l.Dhe_pcs l.Dhe_mcs ///
		  lc.Ypncp l2c.Ypncp lc.Yplgrs_dv l2c.Yplgrs_dv  ///
          Ded_Dgn /*Ded_Dag Ded_Dag_sq*/ ///
		  l.Ded_Ypncp l.Ded_Yplgrs_dv l2.Ded_Yplgrs_dv l2.Ded_Ypncp ///
		  $regions Year_transformed Y2020 Y2021 $ethnicity ///
		  if ${i1b_if_condition} [pw=${weight}], vce(cluster idperson)

process_regression, domain("income") process("I1b") sheet("I1b") ///
	title("Process I1b: Amount of capital income") ///
	gofrow(7) goflabel("I1b - Amount of capital income") ///
	ifcond("${i1b_if_condition}")		  
		    
		  
/****************** I2b: AMOUNT OF PENSION INCOME, RETIRED L1 *****************/		  

*Sample: Retired individuals who were retired in the previous year. 
*ypnoab = Inverse hyperbolic sine transformation of Gross personal private 
* pension income

display "${i2b_if_condition}"

reg ypnoab i.Dgn c.Dag ///
           i.Deh_c4_High i.Deh_c4_Medium  i.Deh_c4_Na /// 
          li.Dhhtp_c4_CoupleChildren li.Dhhtp_c4_SingleNoChildren ///
		  li.Dhhtp_c4_SingleChildren ///
          l.Dhe_pcs l.Dhe_mcs ///
		  lc.Ypnoab l2c.Ypnoab ///
          $regions Year_transformed Y2020 Y2021 $ethnicity ///
		  if ${i2b_if_condition} [pw=${weight}], vce(cluster idperson)

process_regression, domain("income") process("I2b") sheet("I2b") ///
	title("Process I2b: Amount of private pension income, retired L1") ///
	gofrow(11) goflabel("I2b - Amount of private pension income") ///
	ifcond("${i2b_if_condition}")			  
		  
		  
/**** I3a: PROBABILITY OF RECEIVING PRIVATE PENSION INCOME, NOT RETIRED L1 ****/

*Sample: Retired individuals who were not retired in the previous year.

display "${i3a_if_condition}"

logit receives_ypnoab ///
    i.Dgn i.Reached_Retirement_Age ///
	 i.Deh_c4_High i.Deh_c4_Medium i.Deh_c4_Na ///
	li.Les_c4_NotEmployed  ///
	li.Dhhtp_c4_CoupleChildren li.Dhhtp_c4_SingleNoChildren ///
	li.Dhhtp_c4_SingleChildren ///
	l.Dhe_pcs l.Dhe_mcs ///
	l.Hourly_wage ///
	$regions Year_transformed Y2020 Y2021 $ethnicity if ///
	${i3a_if_condition} [pw=${weight}], vce(cluster idperson) base
	
process_regression, domain("income") process("I3a") sheet("I3a") ///
	title("Process I3a: Amount of private pension income, not retired L1") ///
	gofrow(15) goflabel("I3a - Receive private pension income ") ///
	ifcond("${i3a_if_condition}") probit	
	

/******************* I3b: AMOUNT PRIVATE PENSION, NOT RETIRED L1 **************/		  

*Sample: Retired individuals who were not retired in the previous year.
*ypnoab = Inverse hyperbolic sine transformation of Gross personal private 
*pension income

display "${i3b_if_condition}"

reg ypnoab i.Dgn c.Dag ///
           i.Deh_c4_High i.Deh_c4_Medium i.Deh_c4_Na /// 
          li.Les_c4_NotEmployed ///
          li.Dhhtp_c4_CoupleChildren li.Dhhtp_c4_SingleNoChildren ///
		  li.Dhhtp_c4_SingleChildren ///
          l.Dhe_pcs l.Dhe_mcs ///
		  l.Hourly_wage ///
		  $regions Year_transformed Y2020 Y2021 $ethnicity ///
		  if ${i3b_if_condition} [pw=${weight}], vce(cluster idperson)
	
process_regression, domain("income") process("I3b") sheet("I3b") ///
	title("Process I3b: Amount of private pension income, retired L1") ///
	gofrow(19) goflabel("I3b - Amount of private pension income") ///
	ifcond("${i3b_if_condition}")		
	

display "Income analysis complete!" 


capture log close 
