/********************************************************************************
*
*	FILE TO EXTRACT UKHLS DATA FOR SOCIAL CARE PROVISION TO INCLUDE IN INITIAL POPULATION
*
*	AUTH: Justin van de Ven (JV)
*	LAST EDIT: Daria Popova 
*
********************************************************************************/


/********************************************************************************
	local data directories - commented out when using master program
********************************************************************************/


/**********************************************************************
*	start analysis
**********************************************************************/
cd "${dir_data}"
disp "identifying social care provision"

// pooled data
foreach waveid in $scProvWaves {

	local waveno=strpos("abcdefghijklmnopqrstuvwxyz","`waveid'")

	use "${dir_ukhls_data}/`waveid'_indresp.dta", clear
	rename *, l
	rename `waveid'_* *
	gen swv = `waveno'
	keep pidp swv aidhrs aidhu*
	save "${dir_data}/int_temp.dta", replace
	
	use "${dir_ukhls_data}/`waveid'_egoalt.dta", clear
	rename *, l
	rename `waveid'_* *
	sort pidp apno
	forval ii = 1/16 {
		gen rindiv_here = 0
		replace rindiv_here = relationship_dv if (apno == `ii')
		by pidp: egen rindiv`ii' = max(rindiv_here)
		drop rindiv_here
	}
	gen chk = 0
	replace chk = 1 if (pidp == pidp[_n-1])
	drop if (chk==1)
	drop chk
	merge 1:1 pidp using "${dir_data}/int_temp.dta", keep(2 3) nogen
	keep pidp swv aidhrs aidhu* rindiv*
	save "${dir_data}/ukhls_scprov_`waveid'.dta", replace
}
clear all
foreach waveid in $scProvWaves {
	if ("`waveid'" == "f") {
		use "${dir_data}/ukhls_scprov_`waveid'.dta", clear
	}
	else {
		append using "${dir_data}/ukhls_scprov_`waveid'.dta"
	}
}
save "${dir_data}/ukhls_scprov_pooled0.dta", replace


/**************************************************************************************
*	process variables
**************************************************************************************/
use "ukhls_scprov_pooled0.dta", clear

// provision of care
forval ii = 1/16 {
	replace rindiv`ii' = 0 if (missing(rindiv`ii'))
}

// relationship to care recipient in household
gen care_partner = 0
gen care_parent = 0
gen care_child = 0
gen care_sibling = 0
gen care_grandchild = 0
gen care_grandparent = 0
gen care_otherfamily = 0
gen care_other = 0
forval ii = 1/16 {
	// loop over each individual
	
	replace care_partner = 1 if (aidhua`ii'>0 & rindiv`ii' > 0 & rindiv`ii' < 4)
	replace care_parent = 1 if (aidhua`ii'>0 & rindiv`ii' > 3 & rindiv`ii' < 9)
	replace care_child = 1 if (aidhua`ii'>0 & rindiv`ii' > 8 & rindiv`ii' < 14)
	replace care_sibling = 1 if (aidhua`ii'>0 & rindiv`ii' > 13 & rindiv`ii' < 20)
	replace care_grandchild = 1 if (aidhua`ii'>0 & rindiv`ii' == 20)
	replace care_grandparent = 1 if (aidhua`ii'>0 & rindiv`ii' == 21)
	replace care_otherfamily = 1 if (aidhua`ii'>0 & rindiv`ii' > 21 & rindiv`ii' < 26)
	replace care_other = 1 if (aidhua`ii'>0 & rindiv`ii' > 25)
}
replace care_parent = 1 if (aidhu1 == 1 | aidhu2 == 1)
replace care_grandparent = 1 if (aidhu1 == 2 | aidhu2 == 2)
replace care_otherfamily = 1 if (aidhu1 == 3 | aidhu2 == 3)
replace care_otherfamily = 1 if (aidhu1 == 4 | aidhu2 == 4)
replace care_other = 1 if (aidhu1 > 4 | aidhu2 > 4) 
gen care_others = care_sibling + care_grandchild + care_grandparent + care_otherfamily + care_other

gen aidhrs_adj = 0
replace aidhrs_adj = 2 if (aidhrs==1)
replace aidhrs_adj = 7 if (aidhrs==2)
replace aidhrs_adj = 14.5 if (aidhrs==3)
replace aidhrs_adj = 27.5 if (aidhrs==4)
replace aidhrs_adj = 42 if (aidhrs==5)
replace aidhrs_adj = 74.5 if (aidhrs==6)
replace aidhrs_adj = 120 if (aidhrs==7)
replace aidhrs_adj = 5.48 if (aidhrs==8) //weighted average of 1 to 3
replace aidhrs_adj = 71.5 if (aidhrs==9) //weighted average of 4 to 7

gen care_nonpartner = (care_parent + care_child + care_others > 0)
gen careWho = 0
replace careWho = 1 if (care_partner==1 & care_nonpartner==0)
replace careWho = 2 if (care_partner==1 & care_nonpartner==1)
replace careWho = 3 if (care_partner==0 & care_nonpartner==1)
label variable careWho "who person provides care to"
label define careWho 1 "partner only" 2 "partner and non-partner" 3 "non-partner only"

keep pidp swv careWho aidhrs_adj
rename aidhrs_adj aidhrs
rename pidp idperson
save "ukhls_scprov_pooled1.dta", replace


/**************************************************************************************
*	merge with main data set
**************************************************************************************/
disp "merge results with existing data"

use "UKHLS_pooled_all_obs_03.dta", clear

merge 1:1 idperson swv using ukhls_scprov_pooled1, keep(1 3) nogen

foreach var of varlist careWho aidhrs {
	replace `var' = -9 if (missing(`var'))
}

sort idperson swv 
save "ukhls_pooled_all_obs_04.dta", replace 


/**************************************************************************************
* clean-up and exit
**************************************************************************************/
#delimit ;
local files_to_drop 
	int_temp.dta
	ukhls_scprov_f.dta
	ukhls_scprov_g.dta
	ukhls_scprov_h.dta
	ukhls_scprov_i.dta
	ukhls_scprov_j.dta
	ukhls_scprov_k.dta
	ukhls_scprov_l.dta
	ukhls_scprov_m.dta
	ukhls_scprov_pooled0.dta
	ukhls_scprov_pooled1.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data/`file'"
}


	
