***************************************************************************************
* PROJECT:              ESPON: construct initial populations for SimPaths using UKHLS data 
* DO-FILE NAME:         09_finalise_input_data.do
* DESCRIPTION:          This file drops hholds and generates data for importing into SimPaths
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave n]
* AUTHORS: 				Daria Popova, Justin van de Ven
* LAST UPDATE:          9 Dec 2025 DP 
* NOTE:					Called from 00_master.do - see master file for further details
***************************************************************************************

********************************************************************************
cap log close 
log using "${dir_log}/09_finalise_input_data.log", replace
********************************************************************************
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

// If any person in the household has missing values, drop the whole household:
drop if dropHH == 1 

drop dropObs dropHH 
	
// Drop if hh weight = 0:
count if dwt==0 
drop if dwt==0 

//Final check for same sex households
assert ssscp!=1 
assert dgn!=dgnsp if idpartner>0

//Final check for number of adults 
drop adult child  //drop old vars 
gen child = dag<$age_become_responsible 
gen adult = 1 - child 
bys stm idhh: egen adult_count = sum(adult)
bys stm idbenefitunit: egen adult_count2 = sum(adult)
drop if adult_count==0 
drop if adult_count2==0 
drop if ((dag>0 & dag<$age_become_responsible) & (idfather == -9 & idmother == -9)) 
assert adult_count>0 
assert adult_count2>0 
 
//Final check for orphans 
assert  (idfather>0 | idmother>0) if (dag>0 & dag<$age_become_responsible )

//Final check for single adults with nonmissing idpartner 
bys stm idbenefitunit : egen na = sum(adult)
gen chk = (na==1 & dcpst==1 & adult==1) 
bys stm idbenefitunit : egen chk2 = max(chk)
fre chk2 
//two adults in benunit but not partnered 
gen chk3 = (na==2 & dcpst!=1 & adult==1) 
bys stm idbenefitunit : egen chk4 = max(chk3)
fre chk4 
drop if chk2==1 
drop if chk4==1  
drop na chk chk2 chk3 chk4


//check for duplicates in terms of stm and idperson
cap drop duplicate 
duplicates tag stm idperson , generate(duplicate)
assert duplicate==0 

cap drop duplicate 
duplicates tag swv idperson , generate(duplicate)
assert duplicate==0 


		
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
	
	* limit employment history to integer years
	replace liwwh = round(liwwh)
	
	*evaluate disposable income at the benefit unit level 
	gsort idhh idbenefitunit idperson
	by idhh idbenefitunit: egen disp_inc = sum(ydisp)

	*limit saved variables
	keep idhh idbenefitunit idperson idpartner idmother idfather swv dgn dag dnc02 dnc ded deh_c3 sedex dlltsd01 dhe ydses_c5 ///
	yplgrs_dv ypnbihs_dv yptciihs_dv dcpyy dcpagdf ynbcpdf_dv der dehm_c3 dehf_c3 stm dhm scghq2_dv dhh_owned lhw ///
	l1_lhw drgn1 les_c4 dhm_ghq adultchildflag dwt potential_earnings_hourly l1_potential_earnings_hourly total_wealth ///
	total_pensions housing_wealth mortgage_debt need_socare formal_socare_hrs partner_socare_hrs daughter_socare_hrs ///
	son_socare_hrs other_socare_hrs formal_socare_cost carehoursprovidedweekly econ_benefits econ_benefits_nonuc ///
	econ_benefits_uc disp_inc ypncp ypnoab aidhrs carewho dhe_mcs dhe_pcs dhe_mcssp dhe_pcssp dls dot dot01 unemp ///
	financial_distress liwwh
	
	order idhh idbenefitunit idperson idpartner idmother idfather swv dgn dag dnc02 dnc ded deh_c3 sedex dlltsd01 dhe ydses_c5 ///
	yplgrs_dv ypnbihs_dv yptciihs_dv dcpyy dcpagdf ynbcpdf_dv der dehm_c3 dehf_c3 stm dhm scghq2_dv dhh_owned lhw ///
	l1_lhw drgn1 les_c4 dhm_ghq adultchildflag dwt potential_earnings_hourly l1_potential_earnings_hourly total_wealth ///
	total_pensions housing_wealth mortgage_debt need_socare formal_socare_hrs partner_socare_hrs daughter_socare_hrs ///
	son_socare_hrs other_socare_hrs formal_socare_cost carehoursprovidedweekly econ_benefits econ_benefits_nonuc ///
	econ_benefits_uc disp_inc ypncp ypnoab aidhrs carewho dhe_mcs dhe_pcs dhe_mcssp dhe_pcssp dls dot dot01 unemp ///
	financial_distress liwwh
	
	recode idhh idbenefitunit idperson idpartner idmother idfather swv dgn dag dnc02 dnc ded deh_c3 sedex dlltsd01 dhe ydses_c5 ///
	yplgrs_dv ypnbihs_dv yptciihs_dv dcpyy dcpagdf ynbcpdf_dv der dehm_c3 dehf_c3 stm dhm scghq2_dv dhh_owned lhw ///
	l1_lhw drgn1 les_c4 dhm_ghq adultchildflag dwt potential_earnings_hourly l1_potential_earnings_hourly total_wealth ///
	total_pensions housing_wealth mortgage_debt need_socare formal_socare_hrs partner_socare_hrs daughter_socare_hrs ///
	son_socare_hrs other_socare_hrs formal_socare_cost carehoursprovidedweekly econ_benefits econ_benefits_nonuc ///
	econ_benefits_uc disp_inc ypncp ypnoab aidhrs carewho dhe_mcs dhe_pcs dhe_mcssp dhe_pcssp dls dot dot01 unemp ///
	financial_distress liwwh (missing=-9)
	
	
/*Rename variables following the new Codebook */
	* --- Identifiers ---
rename idhh idHh
rename idbenefitunit idBu
rename idperson idPers
rename idpartner idPartner
rename idmother idMother
rename idfather idFather
rename swv statCollectionWave

* --- Demographics ---
rename dgn demMaleFlag
rename dag demAge
//rename dcpst demPartnerStatus
rename dnc02 demNChild0to2
rename dnc demNChild
rename ded eduSpellFlag
rename deh_c3 eduHighestC3
rename sedex eduExitSampleFlag
//rename jbstat labStatus
//rename les_c3 labStatusC3
rename dlltsd01 healthDsblLongtermFlag
rename dhe healthSelfRated
rename ydses_c5 yHhQuintilesMonthC5
//rename dhhtp_c4 demCompHhC4
//rename ssscp demPartnerSameSexFlag
//rename dcpen demEnterPartnerFlag
rename dcpyy demPartnerNYear
//rename dcpex demExitPartnerFlag
rename dcpagdf demAgePartnerDiff
rename ynbcpdf_dv yPersAndPartnerGrossDiffMonth
rename der eduReturnFlag
//rename sedag demAgeEduRangeFlag
//rename sprfm demFertFlag
//rename dchpd demNChild0
//rename dagsp demAgePartner
//rename dehsp_c3 eduHighestPartnerC3
//rename dhesp healthPartnerSelfRated
//rename lessp_c3 labStatusPartnerC3
rename dehm_c3 eduHighestMotherC3
rename dehf_c3 eduHighestFatherC3
rename stm statInterviewYear
//rename lesdf_c4 labStatusPartnerAndOwnC4
rename dhm healthWbScore0to36
rename scghq2_dv demWbScore0to12
rename dhh_owned wealthPrptyFlag
rename lhw labHrsWorkWeek
rename l1_lhw labHrsWorkWeekL1
rename drgn1 demRgn
//rename dct demCountry
rename les_c4 labC4
rename dhm_ghq healthPsyDstrssFlag
//rename lessp_c4 labStatusPartnerC4
rename adultchildflag demAdultChildFlag
//rename multiplier demPopSurveyShare
rename dwt wgtHhCross

* --- Income, labour, wealth ---
rename potential_earnings_hourly labWageHrly
rename l1_potential_earnings_hourly labWageHrlyL1
//rename liquid_wealth wealthLiq
//rename tot_pen wealthPensValue
//rename nvmhome wealthPrptyValue

rename disp_inc yDispMonth //disposable income at the benefit unit level                 
rename total_wealth wealthTotValue   //total wealth net of liabilities of benefit unit including housing, business and
rename mortgage_debt wealthMortgageDebtValue  //total mortgage debt owed on main home of benefit unit
rename housing_wealth wealthPrptyValue //value of main home gross of mortgage debt of benefit unit
rename total_pensions wealthPensValue //value of all private (personal and occupational) pensions of benefit unit

rename econ_benefits yBenReceivedFlag
rename econ_benefits_nonuc yBenNonUCReceivedFlag
rename econ_benefits_uc yBenUCReceivedFlag

rename ypncp yCapitalPersMonth
rename ypnoab yPensPersGrossMonth
rename yplgrs_dv yEmpPersGrossMonth
rename ypnbihs_dv yNonBenPersGrossMonth
rename yptciihs_dv yMiscPersGrossMonth

rename unemp labUnempFlag
rename liwwh labEmpNyear

* --- Social care ---
rename need_socare careNeedFlag
rename formal_socare_hrs careHrsFormal
rename partner_socare_hrs careHrsFromPartner
rename daughter_socare_hrs careHrsFromDaughter
rename son_socare_hrs careHrsFromSon
rename other_socare_hrs careHrsFromOther
rename formal_socare_cost careCareFormal
//rename aidhrs careHrsProvidedWeek
rename carehoursprovidedweekly careHrsProvidedWeek
rename carewho careWho

* --- Health & wellbeing ---
rename dhe_mcs healthMentalMcs
rename dhe_pcs healthPhysicalPcs
rename dhe_mcssp healthMentalPartnerMcs
rename dhe_pcssp healthPhysicalPartnerPcs
rename dls demLifeSatScore1to7
rename dot demEthnC4
rename dot01 demEthnC6
rename financial_distress yFinDstrssFlag	
		
	
	gsort idHh idBu idPers
	save "$dir_data/population_initial_UK_$year.dta", replace
	
	recode demMaleFlag yDispMonth wealthTotValue  wealthMortgageDebtValue  wealthPrptyValue wealthPensValue ///
	careNeedFlag careHrsFormal careHrsFromPartner careHrsFromDaughter careHrsFromSon careHrsFromOther careCareFormal careHrsProvidedWeek careWho (-9=0)

	export delimited using "$dir_data/population_initial_UK_$year.csv", nolabel replace
}

cap log close


/***************************************************************************************
* finalise
***************************************************************************************
#delimit ;
local files_to_drop 
	ukhls_wealthtemp.dta
	ukhls_wealthtemp1.dta
	ukhls_wealthtemp2.dta
	ukhls_wealthtemp3.dta
	was_wealthdata.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data/`file'"
}


***************************************************************************************
* end
***************************************************************************************


