/**************************************************************************************
*
*	PROGRAM TO COLLECT LCFS DATA FOR CONSUMPTION MODELLING
*	First draft adapted from:
*		1 collect_fes_efs_lcfs_v3.do
*
*	SCOPE:
*		- LCFS waves only
*		- Discover local files from LCFS wave folders under ./data
*		- Harmonise renamed modern LCFS variables
*		- Construct wave-specific benefit-unit files
*		- Append wave files into pooled lcfs_sorted.dta
*
*	UPDATED 22 OCTOBER 2026: own income and broad expenditure after local tax.
*	Council Tax and NI domestic rates use the same adult-share BU allocation.
*	Original before-local-tax aggregates are retained for v2 replication and auditing.
*
*	NOTES:
*		- This draft keeps the original LCFS-style derivations where possible.
*		- File discovery excludes quarter and urban/rural files.
*		- Region (gor) is optional in later waves.
*
**************************************************************************************/

cls
clear all
set more off


/**************************************************************************************
*	Helper: deterministic LCFS file map
**************************************************************************************/
capture program drop _get_lcfs_files
program define _get_lcfs_files, rclass
	syntax, WAVE(string)

	if ("`wave'" == "LCFS2008") {
		return local dvper "2008_dvper_ukanon.dta"
		return local dvhh "2008_dvhh_ukanon.dta"
	}
	else if ("`wave'" == "LCFS2009") {
		return local dvper "2009_dvper_ukanon.dta"
		return local dvhh "2009_dvhh_ukanon.dta"
	}
	else if ("`wave'" == "LCFS2010") {
		return local dvper "2010_dvper_ukanon.dta"
		return local dvhh "2010_dvhh_ukanon.dta"
	}
	else if ("`wave'" == "LCFS2011") {
		return local dvper "2011_dvper_ukanon_v2.dta"
		return local dvhh "2011_dvhh_ukanon.dta"
	}
	else if ("`wave'" == "LCFS2012") {
		return local dvper "2012_dvper_ukanon.dta"
		return local dvhh "2012_dvhh_ukanon.dta"
	}
	else if ("`wave'" == "LCFS2013") {
		return local dvper "2013_dvper_ukanon.dta"
		return local dvhh "2013_dvhh_ukanon.dta"
	}
	else if ("`wave'" == "LCFS2014") {
		return local dvper "2014_dvper_ukanon.dta"
		return local dvhh "2014_dvhh_ukanon.dta"
	}
	else if ("`wave'" == "LCFS2015-2016") {
		return local dvper "2015-16_dvper_ukanon.dta"
		return local dvhh "2015-16_dvhh_ukanon.dta"
	}
	else if ("`wave'" == "LCFS2016-2017") {
		return local dvper "2016_17_dvper_ukanon.dta"
		return local dvhh "2016_17_dvhh_ukanon.dta"
	}
	else if ("`wave'" == "LCFS2017-2018") {
		return local dvper "dvper_ukanon_2017-18.dta"
		return local dvhh "dvhh_ukanon_2017-18.dta"
	}
	else if ("`wave'" == "LCFS2018-2019") {
		return local dvper "2018_dvper_ukanon201819.dta"
		return local dvhh "2018_dvhh_ukanon.dta"
	}
	else if ("`wave'" == "LCFS2019-2020") {
		return local dvper "lcfs_2019_dvper_ukanon201920.dta"
		return local dvhh "lcfs_2019_dvhh_ukanon.dta"
	}
	else if ("`wave'" == "LCFS2020-2021") {
		return local dvper "lcfs_2020_dvper_ukanon202021.dta"
		return local dvhh "lcfs_2020_dvhh_ukanon.dta"
	}
	else if ("`wave'" == "LCFS2021-2022") {
		return local dvper "lcfs_2021_dvper_ukanon202122.dta"
		return local dvhh "lcfs_2021_dvhh_ukanon.dta"
	}
	else if ("`wave'" == "LCFS2022-2023") {
		return local dvper "dvper_ukanon_2022-23.dta"
		return local dvhh "dvhh_ukanon_2022.dta"
	}
	else if ("`wave'" == "LCFS2023-2024") {
		return local dvper "dvper_ukanon_202324_2023.dta"
		return local dvhh "dvhh_ukanon_v2_2023.dta"
	}
	else {
		di as error "No file map defined for `wave'"
		exit 198
	}
end


/**************************************************************************************
*	Data set-up
**************************************************************************************/
* Optional arguments allow a complete staged rebuild before installation.
args rootdir exportdir
if ("`rootdir'"=="") local rootdir "`c(pwd)'"
if ("`exportdir'"=="") local exportdir "`rootdir'"
local datadir "`rootdir'/data"
local outdir "`exportdir'/output_lcfs"
local drop_multi_child_bu_hh 0

capture mkdir "`outdir'"
capture mkdir "`outdir'/waves"

capture log close
log using "`outdir'/collect_lcfs.log", replace text

tempname diag_handle
tempfile diag_file
postfile `diag_handle' ///
	str20 wave ///
	int year ///
	long hh_pre_rule ///
	long hh_drop_rule ///
	long bu_pre_rule ///
	long bu_drop_rule ///
	using "`diag_file'", replace


tempfile pooled_bu
local first_bu 1

tempfile pooled_ind
local first_ind 1


/**************************************************************************************
*	Process each LCFS wave
**************************************************************************************/
foreach wave_dir in ///
	LCFS2008 ///
	LCFS2009 ///
	LCFS2010 ///
	LCFS2011 ///
	LCFS2012 ///
	LCFS2013 ///
	LCFS2014 ///
	LCFS2015-2016 ///
	LCFS2016-2017 ///
	LCFS2017-2018 ///
	LCFS2018-2019 ///
	LCFS2019-2020 ///
	LCFS2020-2021 ///
	LCFS2021-2022 ///
	LCFS2022-2023 ///
	LCFS2023-2024 {

	local statadir "`datadir'/`wave_dir'/stata"
	local wave_label : subinstr local wave_dir "LCFS" "", all
	local wave_stub : subinstr local wave_label "-" "_", all
	local year = real(substr("`wave_label'", 1, 4))

	_get_lcfs_files, wave("`wave_dir'")
	local dvper_file "`r(dvper)'"
	local dvhh_file "`r(dvhh)'"

	capture confirm file "`statadir'/`dvper_file'"
	if (_rc) {
		di as error "Expected dvper file not found: `statadir'/`dvper_file'"
		exit 601
	}

	capture confirm file "`statadir'/`dvhh_file'"
	if (_rc) {
		di as error "Expected dvhh file not found: `statadir'/`dvhh_file'"
		exit 601
	}

	di as text "Processing `wave_dir'"
	di as text "  dvper: `dvper_file'"
	di as text "  dvhh : `dvhh_file'"

	/*******************************************************************************
	*	Person file
	*******************************************************************************/
	use "`statadir'/`dvper_file'", clear
	rename *, lower

	capture confirm variable a006
	if (_rc) capture rename a006p a006

	capture confirm variable p008
	if (_rc) capture rename p008p p008

	capture confirm variable p011
	if (_rc) capture rename p011p p011

	capture confirm variable p037
	if (_rc) capture rename p037p p037

	capture confirm variable p047
	if (_rc) capture rename p047p p047

	* P177 is personal normal income after tax and National Insurance.
	* Preserve reported values, including negatives and separate anonymisation caps.
	capture confirm variable p177
	if (_rc) capture rename p177p p177

	capture confirm variable p049
	if (_rc) capture rename p049p p049

	capture confirm variable p116c
	if (_rc) gen p116c = 0

	capture confirm variable b352
	if (_rc) gen b352 = .

	foreach v in a006 p008 p011 p037 p047 p049 p153 p116 p116c b352 p177 {
		capture confirm variable `v'
		if (_rc) {
			di as error "Required person-level variable `v' not found in `dvper_file'"
			exit 111
		}
	}

	keep case person benunper a002 a004 a005p a006 a007 a010 a206 a221 ///
		p008 p011 p037 p047 p049 p153 p116 p116c b352 p177

	replace a010 = 0 if (a010 >= .)
	replace a006 = 0 if (a006 >= .)
	replace p008 = 0 if (p008 >= .)
	replace p011 = 0 if (p011 >= .)
	replace p037 = 0 if (p037 >= .)
	replace p047 = 0 if (p047 >= .)
	replace p049 = 0 if (p049 >= .)
	replace p116 = 0 if (p116 >= .)
	replace p116c = 0 if (p116c >= .)

	gen hrp = (a002 == 0)
	gen sps = (a002 == 1)
	gen os = (a002 == 2)
	gen student = (a007 > 0) if (a007 < .)
	gen chld = (a005p < 18) if (a005p < .)
	gen baby = chld * (a005p < 1)
	gen adlt = 1 - chld if (a005p < .)
	gen bu = benunper
	* Missing source unit identifiers prevent a defensible complete income assignment.
	by case, sort: egen byte source_missing_bu = max(missing(bu))
	drop if (bu >= .)

	gen female = (a004 == 2)
	gen age = a005p
	gen agem = a005p * (1 - female)
	gen fti = (a206 == 2) + (a206 == 1) * (a221 > 30.1)
	gen pti = (a206 == 3) + (a206 == 1) * (a221 < 30.1)
	gen ue = (a206 == 4)
	gen sempi = (a206 == 1)
	gen edn = a010 * (a010 < 40) * (a007 == 0) + a005p * (a007 > 0)
	gen nk04i = (a005p < 5) * chld
	gen nk05i = (a005p < 6) * chld
	gen nk513i = (a005p >= 5) * (a005p <= 13) * chld
	gen nk612i = (a005p >= 6) * (a005p <= 12) * chld
	gen nk1417i = (a005p >= 14) * (a005p <= 17) * chld
	gen yi = p008 + p011 + p037 + p047
	gen pers_hsgi = p116 + p116c
	gen pen_inci = p049
	gen double personal_net_i = p177

	replace fti = ((fti > 0) + (pti > 1) * (fti == 0)) * (yi > 0)
	replace pti = (pti == 1) * (fti == 0) * (yi > 0)

	save "`outdir'/waves/lcfs_`wave_stub'_ind_full.dta", replace

	preserve
	keep case person age student female fti pti ue sempi edn yi pen_inci personal_net_i
	label var age "Age"
	label var student "Continuing full-time student"
	label var female "Female"
	label var fti "FT work"
	label var pti "PT work"
	label var ue "Unemployed"
	label var sempi "Self-employed"
	label var edn "Age last in FTE"
	label var yi "Gross individual earnings"
	label var pen_inci "Individual income from pensions and annuities, p049"
	save "`outdir'/waves/lcfs_`wave_stub'_ind.dta", replace

	if (`first_ind' == 1) {
		save "`pooled_ind'", replace
		local first_ind 0
	}
	else {
		append using "`pooled_ind'"
		save "`pooled_ind'", replace
	}
	restore

	/*******************************************************************************
	*	Benefit-unit aggregation
	*******************************************************************************/
	sort case bu
	* Require complete personal incomes; never interpret a missing member as zero.
	by case bu: egen double bu_net_personsum = total(personal_net_i)
	by case bu: egen int missing_net_bu = total(missing(personal_net_i))
	replace bu_net_personsum = . if missing_net_bu>0
	by case: egen double hh_net_personsum = total(personal_net_i)
	by case: egen int missing_net_hh = total(missing(personal_net_i))
	replace hh_net_personsum = . if missing_net_hh>0
	by case bu: egen age_male = max(agem)
	by case bu: egen na = sum(adlt)
	by case: egen byte source_noadult_bu = max(na==0)
	* Source units with no adult aged 18+ are outside SimPaths' unit definition.
	* Keep retained BUs for inspection, but exclude the WHOLE household from model
	* estimation and validation; do not invent an assignment of the omitted income.
	gen byte income_scope_ok = source_noadult_bu==0 & source_missing_bu==0 & missing_net_hh==0
	by case bu: egen mar = max(sps)
	by case bu: egen nk = sum(chld)
	by case bu: egen nk04 = sum(nk04i)
	by case bu: egen nk05 = sum(nk05i)
	by case bu: egen nk513 = sum(nk513i)
	by case bu: egen nk612 = sum(nk612i)
	by case bu: egen nk1417 = sum(nk1417i)
	by case bu: egen birth = sum(baby)
	by case bu: egen ft = sum(fti)
	by case bu: egen pt = sum(pti)
	by case bu: egen semp = sum(sempi)
	by case bu: egen y = sum(yi)
	by case bu: egen pen_inc = sum(pen_inci)
	by case bu: egen pen_inc_any = max(pen_inci > 0) if (pen_inci < .)
	by case bu: egen pers_cons = sum(p153)
	by case bu: egen pers_hsg = sum(pers_hsgi)
	by case bu: egen edn_max = max(edn)
	by case bu: egen allow = sum(b352)

	sort case
	by case: egen na_hh = sum(adlt)

	keep case a002 bu age student adlt age_male female na mar nk nk04 nk05 ///
		nk513 nk612 nk1417 birth ft pt semp y pen_inc pen_inc_any pers_cons pers_hsg ///
		na_hh edn_max allow bu_net_personsum hh_net_personsum income_scope_ok source_noadult_bu source_missing_bu missing_net_bu missing_net_hh

	drop if (na == 0)
	keep if (adlt == 1)
	drop adlt

	* Keep one representative adult per benefit unit: prefer HRP, then spouse, then oldest adult.
	gen rep_priority = cond(a002 == 0, 0, cond(a002 == 1, 1, 2))
	gsort case bu rep_priority -age
	by case bu: keep if (_n == 1)
	drop rep_priority

	gen byte single_female_adult = (na == 1 & female == 1)
	gen byte single_male_adult = (na == 1 & female == 0)

	sort case bu
	gen id1 = (nk > 0)
	by case: replace id1 = id1[_n-1] + ((nk > 0) & (bu >= bu[_n-1])) if (bu > 1)
	by case: egen id_max = max(id1)

	preserve
		gen byte _drop_multi_child_bu_hh = (id_max > 1)
		egen byte _hh_tag = tag(case)
		count if _hh_tag == 1
		local hh_pre_rule = r(N)
		count if _hh_tag == 1 & _drop_multi_child_bu_hh == 1
		local hh_drop_rule = r(N)
		count
		local bu_pre_rule = r(N)
		count if _drop_multi_child_bu_hh == 1
		local bu_drop_rule = r(N)
	restore

	post `diag_handle' ///
		("`wave_label'") ///
		(`year') ///
		(`hh_pre_rule') ///
		(`hh_drop_rule') ///
		(`bu_pre_rule') ///
		(`bu_drop_rule')

	if (`drop_multi_child_bu_hh' == 1) {
		drop if (id_max > 1)
	}
	drop id1 id_max

	tempfile bu_temp
	save "`bu_temp'", replace

	/*******************************************************************************
	*	Household file
	*******************************************************************************/
	use "`statadir'/`dvhh_file'", clear
	rename *, lower

	capture confirm variable cc4121
	if (_rc) gen cc4121 = 0

	capture confirm variable cc4122
	if (_rc) gen cc4122 = 0

	capture confirm variable b265
	if (_rc) gen b265 = .

	capture confirm variable gor
	if (_rc) gen gor = .

	capture confirm variable gorx
	if (_rc) gen gorx = .

	* B038p is Council Tax; B030 is NI domestic rates, already weekly amounts.
	* Source credits (negative B030) are retained, not truncated to zero.
	capture confirm variable b038p
	if (_rc) capture rename b038 b038p
	foreach v in case p389p p536tp p550tp cc4121 cc4122 weighta a122 b265 b038p b030 {
		capture confirm variable `v'
		if (_rc) {
			di as error "Required household-level variable `v' not found in `dvhh_file'"
			exit 111
		}
	}

	assert !missing(b038p,b030)
	gen double local_tax_hh = b038p + b030
	keep case p389p p536tp p550tp cc4121 cc4122 weighta a122 gor gorx b265 local_tax_hh
	save "`outdir'/waves/lcfs_`wave_stub'_hh.dta", replace

	/*******************************************************************************
	*	Merge and final wave-level BU file
	*******************************************************************************/
	use "`bu_temp'", clear
	merge m:1 case using "`outdir'/waves/lcfs_`wave_stub'_hh.dta"
	drop if (_merge == 1)
	drop _merge

	drop if (age >= .)

	gen year = `year'
	gen survey_label = "`wave_label'"

	gen childcare = (cc4121 + cc4122) * (nk > 0)
	gen rent = (p536tp - pers_hsg) * na / na_hh
	replace rent = 0 if (rent < 0)
	drop na_hh

	gen consmpn = 0
	replace consmpn = pers_cons + rent if (bu > 1)

	sort case
	by case: egen cons_hh = sum(consmpn)
	by case: egen na_hh = sum(na)
	by case: egen nk_hh = sum(nk)

	gen wght = weighta / na
	gen chk = (p550tp < cons_hh)

	replace consmpn = p550tp - cons_hh if (bu == 1 & chk == 0)
	replace consmpn = pers_cons + rent if (bu == 1 & chk == 1)
	drop cons_hh

	by case: egen cons_hh = sum(consmpn)
	by case: egen chk2 = max(chk)
	replace consmpn = p550tp * consmpn / cons_hh if (chk2 == 1 & cons_hh > 0)
	gen cons_share = consmpn / p550tp
	* Retain the former construction only as an explicitly named audit field.
	gen double disp_inc_consumption_share = p389p * consmpn / p550tp if (p550tp > 0)
	* Production income now comes from the personal net-income components, not C.
	gen double disp_inc = bu_net_personsum
	gen double hh_reported_disp_inc = p389p
	gen double hh_net_difference = hh_net_personsum - hh_reported_disp_inc
	gen byte hh_income_gap_flag = abs(hh_net_difference)>1 if hh_net_difference<.
	* Align both sides of the budget with donor disposable income after local tax.
	* This is an explicit attribution assumption: each retained BU bears the same
	* local-tax share as its share of household adults. It is independent of income
	* and expenditure, so it does not reinstate expenditure-based income allocation.
	* Keep cash gifts/maintenance and all other broad-expenditure boundaries intact.
	gen double local_tax_adult_share = na / na_hh
	gen double local_tax_bu = local_tax_hh * local_tax_adult_share
	gen double consmpn_before_ct = consmpn
	gen double disp_inc_before_ct = disp_inc
	gen double cons_share_before_ct = cons_share
	recast double consmpn
	replace consmpn = consmpn_before_ct - local_tax_bu
	replace disp_inc = disp_inc_before_ct - local_tax_bu
	replace cons_share = consmpn / (p550tp - local_tax_hh)
	* Do not clip low/negative net amounts: retain them for sample accounting.
	* Existing estimation positivity and validation income screens apply downstream.
	by case: egen double _local_tax_sum = total(local_tax_bu)
	assert abs(_local_tax_sum-local_tax_hh)<1e-8
	assert abs((consmpn-disp_inc)-(consmpn_before_ct-disp_inc_before_ct))<1e-8 ///
		if !missing(consmpn_before_ct,disp_inc_before_ct)
	drop _local_tax_sum
	gen str16 income_definition = "personal_net_ct"
	gen str16 expenditure_definition = "broad_net_ct"

	replace childcare = 0 if (childcare >= .)
	gen caseno = case

	keep caseno bu age age_male student na mar nk nk04 nk05 nk513 nk612 nk1417 ///
		birth female single_female_adult single_male_adult edn_max ft pt semp y pen_inc pen_inc_any childcare rent consmpn ///
		disp_inc year survey_label wght weighta a122 gor gorx b265 allow cons_share ///
		disp_inc_consumption_share hh_reported_disp_inc hh_net_personsum hh_net_difference ///
		hh_income_gap_flag income_scope_ok income_definition source_noadult_bu source_missing_bu missing_net_bu missing_net_hh ///
		local_tax_hh local_tax_bu local_tax_adult_share consmpn_before_ct disp_inc_before_ct cons_share_before_ct expenditure_definition

	label var caseno "Household ID"
	label var bu "Benefit unit"
	label var age "Age of representative adult"
	label var age_male "Oldest man in BU"
	label var student "Continuing full-time student"
	label var na "Number of adults in BU"
	label var mar "Spouse/cohabitee present in BU"
	label var nk "Number of children in BU"
	label var nk04 "Children aged under 5"
	label var nk05 "Children aged under 6"
	label var nk513 "Children aged 5 to 13"
	label var nk612 "Children aged 6 to 12"
	label var nk1417 "Children aged 14 to 17"
	label var birth "Baby present in BU"
	label var female "Representative adult is female"
	label var single_female_adult "Single-adult BU with female adult"
	label var single_male_adult "Single-adult BU with male adult"
	label var edn_max "Highest age last in FTE in BU"
	label var ft "Number in FT work in BU"
	label var pt "Number in PT work in BU"
	label var semp "Number self-employed in BU"
	label var y "Total gross earnings in BU"
	label var pen_inc "BU income from pensions and annuities, p049"
	label var pen_inc_any "BU receives any pension or annuity income, p049"
	label var childcare "Childcare expenditure"
	label var rent "Original broad housing allocation before local tax"
	label var consmpn "BU broad expenditure after allocated Council Tax/NI rates"
	label var disp_inc "BU sum personal P177/P177p less allocated Council Tax/NI rates"
	label var local_tax_hh "HH weekly Council Tax and NI domestic rates: B038p+B030"
	label var local_tax_bu "BU weekly local tax, allocated by household adult share"
	label var local_tax_adult_share "BU adults divided by household adults"
	label var consmpn_before_ct "v2 broad BU expenditure before Council Tax alignment"
	label var disp_inc_before_ct "v2 BU personal net-income sum before local tax"
	label var cons_share_before_ct "v2 BU share of broad HH expenditure before local tax"
	label var disp_inc_consumption_share "Former expenditure-allocated BU income (audit only)"
	label var income_scope_ok "Complete household, no source BU without an 18+ adult"
	label var hh_income_gap_flag "Personal sum differs from reported HH income by over 1 pound/week"
	label var year "Wave start year"
	label var survey_label "Original LCFS wave label"
	label var wght "Annual weight divided by adults in BU"
	label var weighta "Annual household weight"
	label var a122 "Tenure type"
	label var gor "Government office region"
	label var gorx "Government office region modified"
	label var b265 "Regular allowances paid"
	label var allow "Regular allowances received"
	label var cons_share "BU share of household consumption"

	save "`outdir'/waves/lcfs_`wave_stub'_bu.dta", replace

	if (`first_bu' == 1) {
		save "`pooled_bu'", replace
		local first_bu 0
	}
	else {
		append using "`pooled_bu'"
		save "`pooled_bu'", replace
	}
}


/**************************************************************************************
*	Save pooled outputs
**************************************************************************************/
use "`pooled_ind'", clear
sort case person
save "`outdir'/lcfs_individuals_sorted.dta", replace

use "`pooled_bu'", clear
sort caseno year bu
save "`outdir'/lcfs_sorted.dta", replace

postclose `diag_handle'
use "`diag_file'", clear
gen hh_drop_share = hh_drop_rule / hh_pre_rule if (hh_pre_rule > 0)
gen bu_drop_share = bu_drop_rule / bu_pre_rule if (bu_pre_rule > 0)
sort year
save "`outdir'/multi_child_bu_rule_diagnostics.dta", replace

egen hh_pre_rule_total = total(hh_pre_rule)
egen hh_drop_rule_total = total(hh_drop_rule)
egen bu_pre_rule_total = total(bu_pre_rule)
egen bu_drop_rule_total = total(bu_drop_rule)
gen hh_drop_share_total = hh_drop_rule_total / hh_pre_rule_total if (hh_pre_rule_total > 0)
gen bu_drop_share_total = bu_drop_rule_total / bu_pre_rule_total if (bu_pre_rule_total > 0)

list wave year hh_pre_rule hh_drop_rule hh_drop_share bu_pre_rule bu_drop_rule bu_drop_share, noobs abbreviate(20)
list hh_pre_rule_total hh_drop_rule_total hh_drop_share_total bu_pre_rule_total bu_drop_rule_total bu_drop_share_total in 1, noobs

log close
