***************************************************************************************
* PROJECT:              ESPON: construct initial populations for SimPaths using UKHLS data 
* DO-FILE NAME:         09_finalise_input_data.do
* DESCRIPTION:          This file generates data for importing into SimPaths
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave m]
* AUTHORS: 				Daria Popova, Justin van de Ven
* LAST UPDATE:          10 Apr 2024
* NOTE:					Called from 00_master.do - see master file for further details
***************************************************************************************


***************************************************************************************
* pool all waves
***************************************************************************************
forvalues year = $firstSimYear/$lastSimYear {
* load pooled data with missing values removed  
	
	if (`year'==$firstSimYear) {
		use "$dir_data/population_initial_fs_UK_`year'.dta", clear
	}
	else {
		append using "$dir_data/population_initial_fs_UK_`year'.dta"
	}
}
save "$dir_data\ukhls_pooled_all_obs_09.dta", replace


***************************************************************************************
* start
***************************************************************************************
use "$dir_data\ukhls_pooled_all_obs_09.dta", clear


***************************************************************************************
* limit sample
***************************************************************************************

// If any person in the household has missing values, drop the whole household ( 339,941 obs):
drop if dropHH == 1 /*61,846 obs deleted*/
drop dropObs dropHH 
	
// Drop if hh weight = 0:
count if dwt==0 /*  60,814 obs*/
drop if dwt==0 

// Drop same sex households
bys stm: fre ssscp
/* 1,429 obs deleted*/
drop if ssscp==1 
bys stm: fre idhh if (dgn==dgnsp) & (dgn>=0 & dgnsp>=0)
/*1,361 obs deleted*/
drop if (dgn==dgnsp) & (dgn>=0 & dgnsp>=0)	
 
// drop orphans
drop adult child adult_count* //drop old vars 
gen adult = dag>=$age_become_responsible 
//replace adult = 1 if (adult==0 & dcpst==1)
gen child = 1 - adult
bys  idhh stm: egen adult_count = sum(adult)
bys  idbenefitunit stm: egen adult_count2 = sum(adult)
drop if adult_count==0 
drop if adult_count2==0 
drop if ((dag>0 & dag<$age_become_responsible) & (idfather == -9 & idmother == -9)) 
/*197 obs deleted*/
assert adult_count>0 
assert adult_count2>0 
 
// check for duplicates in terms of stm amd idperson
cap drop duplicate 
duplicates tag stm idperson , generate(duplicate)
assert duplicate==0 

cap drop duplicate 
duplicates tag swv idperson , generate(duplicate)
assert duplicate==0 

sort idbenefitunit
by idbenefitunit : egen na = sum(adult)
gen chk = (na==1 & dcpst==1 & adult==1)
by idbenefitunit : egen chk2 = max(chk)
drop if (chk2==1)
drop chk chk2
gen chk = (na==2 & dcpst!=1 & adult==1)
by idbenefitunit : egen chk2 = max(chk)
drop if (chk2==1)
drop chk chk2

		
***************************************************************************************
* save limited sample
***************************************************************************************
save "$dir_data\UKHLS_pooled_ipop.dta", replace /*panel dataset with missing values removed*/


***************************************************************************************
* generate frequency weights
***************************************************************************************
cap gen uk_pop=0 
replace uk_pop = 26240000 if stm == 2010
replace uk_pop = 26409000 if stm == 2011 //26,409,158 
replace uk_pop = 26620000 if stm == 2012
replace uk_pop = 26663000 if stm == 2013
replace uk_pop = 26734000 if stm == 2014
replace uk_pop = 27046000 if stm == 2015
replace uk_pop = 27109000 if stm == 2016
replace uk_pop = 27226000 if stm == 2017 //27,225,825
replace uk_pop = 27576000 if stm == 2018
replace uk_pop = 27824000 if stm == 2019
replace uk_pop = 27893000 if stm == 2020
replace uk_pop = 28119000 if stm == 2021
replace uk_pop = 28243000 if stm == 2022
replace uk_pop = 28243000 if stm == 2023

cap drop surv_pop
//bys stm: gen surv_pop = _N //gen survey hhs population for each calendar year 
bys stm: egen surv_pop = total(dwt_adjusted)
bys stm: sum surv_pop

cap drop  multiplier
gen multiplier = uk_pop / surv_pop 

cap gen dwtfq = round(dwt * multiplier) 
//cap drop dwt_sampling
//rename dwt dwt_sampling
replace dwt = dwtfq 
bys stm: sum dwt*

save "$dir_data\UKHLS_pooled_ipop.dta", replace /*panel dataset with missing values removed*/


***************************************************************************************
* slice the pooled dataset into intitial populations
***************************************************************************************
forvalues yy = $firstSimYear/$lastSimYear {
* load pooled data with missing values removed  
	use "$dir_data\ukhls_pooled_ipop.dta", clear
	rename *, l
	
	* limit year
	global year = `yy'
	keep if stm == $year 

	*check for duplicates 
	duplicates report idhh idperson
	cap drop duplicate 
	duplicates tag idhh idperson , generate(duplicate)
	assert duplicate ==0 

	duplicates report idperson
	cap drop duplicate 
	duplicates tag idperson , generate(duplicate)
	assert duplicate ==0 

	*check for same sex couples 
	assert ssscp!=1 
	assert dgn!=dgnsp if dgn>=0 & dgnsp>=0

	* check for orphans
	cap drop adult child adult_count* //drop old vars 
	gen adult = dag>=$age_become_responsible 
	gen child = 1 - adult
	bys idhh: egen adult_count = sum(adult)
	bys idbenefitunit: egen adult_count2 = sum(adult)
	drop if adult_count==0| adult_count2==0 
	assert adult_count>0
	assert adult_count2>0
	 
	*check weight is not zero and non-missing 
	drop if (dwt==0 | dwt>=.)
	assert dwt>0 & dwt<. 
	//sum of weights
	cap gen one =1
	sum one [w=dwt]

	*limit saved variables
	keep idhh idbenefitunit idperson idpartner idmother idfather pno swv dgn dag dcpst dnc02 dnc ded deh_c3 sedex jbstat les_c3 dlltsd dhe ydses_c5 yplgrs_dv ypnbihs_dv yptciihs_dv dhhtp_c4 ssscp dcpen dcpyy dcpex dcpagdf ynbcpdf_dv der sedag sprfm dagsp dehsp_c3 dhesp lessp_c3 dehm_c3 dehf_c3 stm lesdf_c4 ppno dhm scghq2_dv dhh_owned lhw drgn1 dct dwt_sampling les_c4 dhm_ghq lessp_c4 adultchildflag multiplier dwt potential_earnings_hourly l1_potential_earnings_hourly liquid_wealth tot_pen nvmhome need_socare formal_socare_hrs partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs formal_socare_cost ypncp ypnoab aidhrs carewho
	order idhh idbenefitunit idperson idpartner idmother idfather pno swv dgn dag dcpst dnc02 dnc ded deh_c3 sedex jbstat les_c3 dlltsd dhe ydses_c5 yplgrs_dv ypnbihs_dv yptciihs_dv dhhtp_c4 ssscp dcpen dcpyy dcpex dcpagdf ynbcpdf_dv der sedag sprfm dagsp dehsp_c3 dhesp lessp_c3 dehm_c3 dehf_c3 stm lesdf_c4 ppno dhm scghq2_dv dhh_owned lhw drgn1 dct dwt_sampling les_c4 dhm_ghq lessp_c4 adultchildflag multiplier dwt potential_earnings_hourly l1_potential_earnings_hourly liquid_wealth tot_pen nvmhome need_socare formal_socare_hrs partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs formal_socare_cost ypncp ypnoab aidhrs carewho
	
	recode idhh idbenefitunit idperson idpartner idmother idfather pno swv dgn dag dcpst dnc02 dnc ded deh_c3 sedex jbstat les_c3 dlltsd dhe ydses_c5 yplgrs_dv ypnbihs_dv yptciihs_dv dhhtp_c4 ssscp dcpen dcpyy dcpex dcpagdf ynbcpdf_dv der sedag sprfm dagsp dehsp_c3 dhesp lessp_c3 dehm_c3 dehf_c3 stm lesdf_c4 ppno dhm scghq2_dv dhh_owned lhw drgn1 dct dwt_sampling les_c4 dhm_ghq lessp_c4 adultchildflag multiplier dwt potential_earnings_hourly l1_potential_earnings_hourly liquid_wealth tot_pen nvmhome need_socare formal_socare_hrs partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs formal_socare_cost ypncp ypnoab aidhrs carewho (missing=-9)
	
	gsort idhh idbenefitunit idperson
	save "$dir_data/population_initial_UK_$year.dta", replace
	
	recode dgn liquid_wealth tot_pen nvmhome need_socare formal_socare_hrs partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs formal_socare_cost aidhrs carewho (-9=0)
	export delimited using "$dir_data/population_initial_UK_$year.csv", nolabel replace
}


***************************************************************************************
* finalise
***************************************************************************************
#delimit ;
local files_to_drop 
	was_wealthdata.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data/`file'"
}

cap log close


***************************************************************************************
* end
***************************************************************************************


