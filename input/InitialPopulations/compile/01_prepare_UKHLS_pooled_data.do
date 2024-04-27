***************************************************************************************
* PROJECT:              ESPON: construct initial populations for SimPaths using UKHLS data 
* DO-FILE NAME:         01_prepare_UKHLS_pooled_data.do
* DESCRIPTION:          Compiles pooled data from UKHLS for analysis
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave m]
* AUTHORS: 				Daria Popova, Justin van de Ven
* LAST UPDATE:          10 Apr 2024
* NOTE:					Called from 00_master.do - see master file for further details
***************************************************************************************

cd "${dir_ukhls_data}"


***************************************************************************************
cap log close 
log using "${dir_log}/01_prepare_UKHLS_pooled_data.log", replace
***************************************************************************************


/**************************************************************************************
* Select and merge UKHLS data 
**************************************************************************************/

*add variables from the all persons (Household grid) dataset
foreach w of global UKHLSwaves {

	// find the wave number
	local waveno=strpos("abcdefghijklmnopqrstuvwxyz","`w'")
	
	if (`waveno'<13) {
		use pidp `w'_ivfho `w'_ivfio `w'_hhorig `w'_buno_dv `w'_dvage `w'_sex `w'_depchl `w'_hidp `w'_pno `w'_pns1pid `w'_pns2pid `w'_month `w'_intdaty_dv ///
		`w'_mnspid `w'_fnspid `w'_ppid `w'_ppno `w'_sppid `w'_sex_dv `w'_mastat_dv `w'_gor_dv `w'_age_dv  /* `w'_hgbioad1 `w'_hgbioad2 */ ///
		`w'_intdatd_dv `w'_intdatm_dv `w'_intdaty_dv using `w'_indall.dta, clear
	}
	else {
		use pidp `w'_ivfho `w'_ivfio `w'_hhorig `w'_buno_dv `w'_dvage `w'_sex `w'_depchl `w'_hidp `w'_pno `w'_pns1pid `w'_pns2pid `w'_month `w'_intdaty_dv ///
		`w'_mnspid `w'_fnspid `w'_ppid `w'_ppno `w'_sppid `w'_sex_dv `w'_mastat_dv `w'_gor_dv `w'_age_dv  `w'_hgbioad1 `w'_hgbioad2 ///
		`w'_intdatd_dv `w'_intdatm_dv `w'_intdaty_dv using `w'_indall.dta, clear
	}

	gen swv = `waveno'
	rename `w'_* *
	if (`waveno'>1) {
		append using "$dir_data\add_vars_ukhls.dta"
	}
	save "$dir_data\add_vars_ukhls.dta", replace
}

*add variables from the Responding adults (16+) dataset
foreach w of global UKHLSwaves {

	// find the wave number
	local waveno=strpos("abcdefghijklmnopqrstuvwxyz","`w'")

	if (`waveno'==1) {
		use pidp `w'_hidp `w'_pno `w'_buno_dv `w'_jbhrs `w'_jbot `w'_jshrs `w'_scghq1_dv `w'_scghq2_dv `w'_fimngrs_dv `w'_fimnnet_dv `w'_fimnlabnet_dv ///
		`w'_fimnmisc_dv `w'_fimnprben_dv `w'_fimninvnet_dv `w'_fimnsben_dv `w'_fimnlabgrs_dv `w'_fimnpen_dv `w'_jbstat `w'_hiqual_dv `w'_jbhrs ///
		`w'_j2hrs `w'_jshrs /*`w'_scsfl*/ `w'_scghq1_dv `w'_scghq2_dv `w'_jbsic07_cc `w'_bendis* `w'_scghq1_dv `w'_scghq2_dv ///
		/*`w'_indinus_lw `w'_indscus_lw `w'_indpxub_xw `w'_indpxui_xw `w'_relup `w'_currpart* `w'_lmcbm* `w'_lmcby4* */ ///
		using `w'_indresp.dta, clear
	}
	else if (`waveno'<6) {
		use pidp `w'_hidp `w'_pno `w'_buno_dv `w'_jbhrs `w'_jbot `w'_jshrs `w'_scghq1_dv `w'_scghq2_dv `w'_fimngrs_dv `w'_fimnnet_dv `w'_fimnlabnet_dv ///
		`w'_fimnmisc_dv `w'_fimnprben_dv `w'_fimninvnet_dv `w'_fimnsben_dv `w'_fimnlabgrs_dv `w'_fimnpen_dv `w'_jbstat `w'_hiqual_dv `w'_jbhrs ///
		`w'_j2hrs `w'_jshrs `w'_scsf1 `w'_scghq1_dv `w'_scghq2_dv `w'_jbsic07_cc `w'_bendis* `w'_scghq1_dv `w'_scghq2_dv ///
		`w'_indinus_lw `w'_indscus_lw `w'_indpxub_xw /*`w'_indpxui_xw*/ `w'_relup `w'_currpart* `w'_lmcbm* `w'_lmcby4* ///
		using `w'_indresp.dta, clear
	}
	else if (`waveno'<13) {
		use pidp `w'_hidp `w'_pno `w'_buno_dv `w'_jbhrs `w'_jbot `w'_jshrs `w'_scghq1_dv `w'_scghq2_dv `w'_fimngrs_dv `w'_fimnnet_dv `w'_fimnlabnet_dv ///
		`w'_fimnmisc_dv `w'_fimnprben_dv `w'_fimninvnet_dv `w'_fimnsben_dv `w'_fimnlabgrs_dv `w'_fimnpen_dv `w'_jbstat `w'_hiqual_dv `w'_jbhrs ///
		`w'_j2hrs `w'_jshrs `w'_scsf1 `w'_scghq1_dv `w'_scghq2_dv `w'_jbsic07_cc `w'_bendis* `w'_scghq1_dv `w'_scghq2_dv ///
		`w'_indinus_lw `w'_indscus_lw /*`w'_indpxub_xw*/ `w'_indpxui_xw `w'_relup `w'_currpart* `w'_lmcbm* `w'_lmcby4* ///
		using `w'_indresp.dta, clear
	} 
	else {
		use pidp `w'_hidp `w'_pno /*`w'_buno_dv*/ `w'_jbhrs `w'_jbot `w'_jshrs `w'_scghq1_dv `w'_scghq2_dv `w'_fimngrs_dv `w'_fimnnet_dv `w'_fimnlabnet_dv ///
		`w'_fimnmisc_dv `w'_fimnprben_dv `w'_fimninvnet_dv `w'_fimnsben_dv `w'_fimnlabgrs_dv `w'_fimnpen_dv `w'_jbstat `w'_hiqual_dv `w'_jbhrs ///
		/*`w'_j2hrs*/ `w'_jshrs `w'_scsf1 `w'_scghq1_dv `w'_scghq2_dv `w'_jbsic07_cc `w'_bendis* `w'_scghq1_dv `w'_scghq2_dv ///
		`w'_indinus_lw `w'_indscus_lw /*`w'_indpxub_xw*/ `w'_indpxui_xw `w'_relup `w'_currpart* `w'_lmcbm* `w'_lmcby4* `w'_indpxui_xw ///
		using `w'_indresp.dta, clear 
		gen m_j2hrs=-9 /*m_j2hrs not available in wave 13*/
	}
	
	gen swv = `waveno'
	rename `w'_* *
	if (`waveno'>1) {
		append using "$dir_data\add_vars_ukhls_indresp.dta"
	}
	save "$dir_data\add_vars_ukhls_indresp.dta", replace
}

*add variables from the Responding households dataset
foreach w of global UKHLSwaves {

	// find the wave number
	local waveno=strpos("abcdefghijklmnopqrstuvwxyz","`w'")

	if (`waveno'==1) {
		use `w'_hidp `w'_fihhmnnet1_dv `w'_fihhmngrs1_dv /*`w'_hhdenui_xw*/ `w'_nch02_dv /*`w'_hhdenub_xw `w'_hhdenui_xw*/ `w'_hsownd using `w'_hhresp.dta, clear
	}
	else if (`waveno'<6) {
		use `w'_hidp `w'_fihhmnnet1_dv `w'_fihhmngrs1_dv /*`w'_hhdenui_xw*/ `w'_nch02_dv `w'_hhdenub_xw /*`w'_hhdenui_xw*/ `w'_hsownd using `w'_hhresp.dta, clear
	}
	else if (`waveno'<13) {
		use `w'_hidp `w'_fihhmnnet1_dv `w'_fihhmngrs1_dv /*`w'_hhdenub_xw*/ `w'_nch02_dv /*`w'_hhdenub_xw*/ `w'_hhdenui_xw `w'_hsownd using `w'_hhresp.dta, clear
	} 
	else {
		use `w'_hidp `w'_fihhmnnet1_dv `w'_fihhmngrs1_dv `w'_hhdenui_xw     `w'_nch02_dv /*`w'_hhdenub_xw*/ `w'_hhdenui_xw `w'_hsownd using `w'_hhresp.dta, clear
	}
	
	gen swv = `waveno'
	rename `w'_* *
	if (`waveno'>1) {
		append using "$dir_data\add_vars_ukhls_hhresp.dta"
	}
	save "$dir_data\add_vars_ukhls_hhresp.dta", replace
}


/**************************************************************************************
* Prepare and merge income variables:
**************************************************************************************/
foreach w of global UKHLSwaves {

	// find the wave number
	local waveno=strpos("abcdefghijklmnopqrstuvwxyz","`w'")

	use pidp `w'_hidp `w'_frmnthimp_dv `w'_ficode `w'_fiseq  using `w'_income.dta, clear
	
	gen swv = `waveno'
	rename `w'_* *
	if (`waveno'>1) {
		append using "$dir_data\add_vars_ukhls_income.dta"
	}
	save "$dir_data\add_vars_ukhls_income.dta", replace
}

//check for duplicates 
sort swv pidp hidp fiseq
duplicates report swv pidp hidp fiseq

//now collapse variables 
preserve
gen inc_stp = frmnthimp_dv if ficode == 1
gen inc_tu = frmnthimp_dv if ficode == 25
gen inc_ma = frmnthimp_dv if ficode == 26
keep swv pidp hidp inc_stp inc_tu inc_ma
drop if missing(inc_stp) & missing(inc_tu) & missing(inc_ma)
collapse (sum) inc_stp inc_tu inc_ma, by(swv pidp hidp)
save "$dir_data\tmp_income", replace
restore

//merge variables from the youth dataset 9-18 years old * 
foreach w of global UKHLSwaves {

	// find the wave number
	local waveno=strpos("abcdefghijklmnopqrstuvwxyz","`w'")

	if (`waveno'>7 | mod(`waveno',2)==0) {
		use pidp `w'_hidp `w'_ypsrhlth  using  `w'_youth.dta, clear
		
		gen swv = `waveno'
		rename `w'_* *
		if (`waveno'>2) {
			append using "$dir_data\add_vars_ukhls_youth.dta"
		}
		save "$dir_data\add_vars_ukhls_youth.dta", replace
	}
}


/**************************************************************************************
* merge all datasets together 
**************************************************************************************/
use "$dir_data\add_vars_ukhls.dta", clear
merge 1:1 pidp hidp swv using "$dir_data\add_vars_ukhls_indresp.dta", keep(1 3) nogen
merge m:1 hidp swv using "$dir_data\add_vars_ukhls_hhresp.dta", keep(1 3) nogen
merge 1:1 pidp hidp swv using "$dir_data\tmp_income", keep(1 3) nogen
merge 1:1 pidp hidp swv using "$dir_data\add_vars_ukhls_youth.dta", keep(1 3) nogen

//Merge variables from cross-wave file (Stable characteristics of individuals)
merge m:1 pidp using "$dir_ukhls_data\xwavedat.dta" , keepusing(maedqf paedqf birthy) keep(1 3) nogen 

//DP: check if this is needed as IEMB is dropped later on 
replace month = 1 if month == -10 // month not available for IEMB (Ethnic Minority Boost) sample. Treat as month 1.

//Merge cohabitation variables from wave 1 (a) & 6 (f)
merge m:1 pidp using "$dir_ukhls_data\a_indresp.dta" , keepusing(a_ppid a_intdatd_dv a_intdatm_dv a_intdaty_dv a_lcmarm a_lcmary4 a_lcmcbm a_lcmcby4) keep(1 3) nogen 
merge m:1 pidp using "$dir_ukhls_data\f_indresp.dta" , keepusing(f_ppid f_intdatd_dv f_intdatm_dv f_intdaty_dv f_lcmarm f_lcmary4 f_lcmcbm f_lcmcby4) keep(1 3) nogen

replace month = 1 if month == -10 // month not available for IEMB (Ethnic Minority Boost) sample. Treat as month 1.
//DP: IEMB is dropped later on


/**************************************************************************************
* save output
**************************************************************************************/
save "$dir_data\ukhls_pooled_all_obs_01.dta", replace
cap log close 


/**************************************************************************************
* clean-up and exit
**************************************************************************************/
#delimit ;
local files_to_drop 
	add_vars_ukhls.dta
	add_vars_ukhls_hhresp.dta 
	add_vars_ukhls_income.dta 
	add_vars_ukhls_indresp.dta 
	add_vars_ukhls_youth.dta
	tmp_income.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data/`file'"
}




