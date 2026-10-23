/**************************************************************************************
*
*	CONSUMPTION MODELLING USING POOLED LCFS BENEFIT-UNIT DATA
*
*	Input:
*		output_lcfs/lcfs_sorted.dta
*
*	Production v3 (22 October 2026; after local-tax alignment):
*		m4: five-year age-specific log-income slopes with common curvature
*		    reference-adult student dummy; three-category housing status
*
*	Baseline modelling choices:
*		- Real monthly monetary variables, CPIH (L522), base 2015; 365.25/7/12 conversion
*		- Disposable income: personal P177/P177p sums less allocated local tax; income_scope_ok required
*		- Positive-sample restriction for log specification
*		- Benefit-unit level regression
*		- Survey weights: weighta; covariance clustered by household within year
*		- Year fixed effects included
*		- Existing five-year age intercepts and age-specific log-income slopes
*		- Common uncentered quadratic in log disposable income
*		- Child status in the constructed input file is age <= 17
*
**************************************************************************************/

cls
//version 17
clear all
set more off

args rootdir exportdir
if ("`rootdir'" == "") local rootdir "`c(pwd)'"
if ("`exportdir'" == "") local exportdir "`rootdir'"
local outdir "`rootdir'/output_lcfs"
local writingdir "`exportdir'/output_writing"
local modelinputdir "`exportdir'/output_model_input"

capture mkdir "`writingdir'"
capture mkdir "`modelinputdir'"

capture log close
log using "`writingdir'/consumption_modelling_lcfs.log", replace text


/**************************************************************************************
*	Helper: export stored model to CSV
**************************************************************************************/
capture program drop export_model_csv
program define export_model_csv
	syntax, MODEL(string) OUTFILE(string)

	estimates restore `model'

	local title "`model'"
	if ("`model'" == "m4") {
		local title "m4: five-year age-specific income slopes; common curvature; student control; three-category housing status"
	}

	quietly ereturn display
	matrix results = r(table)
	local cnames : colnames results
	local k = colsof(results)

	tempname fh
	file open `fh' using "`outfile'", write text replace
	file write `fh' `"model,`model'"' _n
	file write `fh' `"title,"`title'""' _n
	file write `fh' `"dependent variable,ln_consmpn"' _n
	file write `fh' `"observations,`=string(e(N), "%18.0g")'"' _n
	file write `fh' `"R-squared,`=string(e(r2), "%18.0g")'"' _n
	file write `fh' `"Root MSE,`=string(e(rmse), "%18.0g")'"' _n
	file write `fh' `"F statistic,`=string(e(F), "%18.0g")'"' _n
	file write `fh' `"residual df,`=string(e(df_r), "%18.0g")'"' _n
	file write `fh' `"weight,pweight = weighta"' _n
	file write `fh' `"VCE,clustered by household within year"' _n
	file write `fh' _n
	file write `fh' `"term,coefficient,std. err.,t,P>|t|"' _n

	forvalues j = 1/`k' {
		local term : word `j' of `cnames'
		if !regexm("`term'", "^[0-9]+[bo]\.") {
			local term_label "`term'"
		if ("`term'" == "year_c2015") local term_label "Year trend (since 2015)"
			if ("`term'" == "ln_disp_inc") local term_label "ln(real disposable income)"
			if ("`term'" == "ln_disp_inc2") local term_label "ln(real disposable income) squared"
			if ("`term'" == "2.age_group") local term_label "Age 20-24"
			if ("`term'" == "3.age_group") local term_label "Age 25-29"
			if ("`term'" == "4.age_group") local term_label "Age 30-34"
			if ("`term'" == "5.age_group") local term_label "Age 35-39"
			if ("`term'" == "6.age_group") local term_label "Age 40-44"
			if ("`term'" == "7.age_group") local term_label "Age 45-49"
			if ("`term'" == "8.age_group") local term_label "Age 50-54"
			if ("`term'" == "9.age_group") local term_label "Age 55-59"
			if ("`term'" == "10.age_group") local term_label "Age 60-64"
			if ("`term'" == "11.age_group") local term_label "Age 65-69"
			if ("`term'" == "12.age_group") local term_label "Age 70-74"
			if ("`term'" == "13.age_group") local term_label "Age 75-79"
			if ("`term'" == "14.age_group") local term_label "Age 80+"
			if ("`term'" == "single_female_adult") local term_label "Single female adult BU"
			if ("`term'" == "single_male_adult") local term_label "Single male adult BU"
			if ("`term'" == "nk") local term_label "Number of children"
			if ("`term'" == "ft") local term_label "Number in full-time work"
			if ("`term'" == "pt") local term_label "Number in part-time work"
			if ("`term'" == "semp") local term_label "Number self-employed"
			if ("`term'" == "pen_inc_any") local term_label "Receives pension or annuity income"
			if ("`term'" == "1.housing_status") local term_label "Owner with ongoing payments"
			if ("`term'" == "2.housing_status") local term_label "Owned outright"
			if ("`term'" == "1.gorx") local term_label "Region: North East"
			if ("`term'" == "2.gorx") local term_label "Region: North West and Merseyside"
			if ("`term'" == "3.gorx") local term_label "Region: Yorkshire and the Humber"
			if ("`term'" == "4.gorx") local term_label "Region: East Midlands"
			if ("`term'" == "5.gorx") local term_label "Region: West Midlands"
			if ("`term'" == "6.gorx") local term_label "Region: Eastern"
			if ("`term'" == "8.gorx") local term_label "Region: South East"
			if ("`term'" == "9.gorx") local term_label "Region: South West"
			if ("`term'" == "10.gorx") local term_label "Region: Wales"
			if ("`term'" == "11.gorx") local term_label "Region: Scotland"
			if ("`term'" == "12.gorx") local term_label "Region: Northern Ireland"
			if ("`term'" == "year") local term_label "Year trend"
			if ("`term'" == "2009.year") local term_label "Year 2009"
			if ("`term'" == "2010.year") local term_label "Year 2010"
			if ("`term'" == "2011.year") local term_label "Year 2011"
			if ("`term'" == "2012.year") local term_label "Year 2012"
			if ("`term'" == "2013.year") local term_label "Year 2013"
			if ("`term'" == "2014.year") local term_label "Year 2014"
			if ("`term'" == "2015.year") local term_label "Year 2015"
			if ("`term'" == "2016.year") local term_label "Year 2016"
			if ("`term'" == "2017.year") local term_label "Year 2017"
			if ("`term'" == "2018.year") local term_label "Year 2018"
			if ("`term'" == "2019.year") local term_label "Year 2019"
			if ("`term'" == "2020.year") local term_label "Year 2020"
			if ("`term'" == "2021.year") local term_label "Year 2021"
			if ("`term'" == "2022.year") local term_label "Year 2022"
			if ("`term'" == "2023.year") local term_label "Year 2023"
			if ("`term'" == "_cons") local term_label "Constant"

			scalar coefficient = results[1, `j']
			scalar std_error = results[2, `j']
			scalar t_stat = results[3, `j']
			scalar p_value = results[4, `j']

			local p_value_display = string(p_value, "%12.4g")
			if (p_value == 0) {
				local p_value_display "<1e-300"
			}

			local coefficient_s = string(coefficient, "%12.6f")
			local std_error_s = string(std_error, "%12.6f")
			local t_stat_s = string(t_stat, "%12.2f")

			file write `fh' `"`term_label',`coefficient_s',`std_error_s',`t_stat_s',`p_value_display'"' _n
		}
	}
	file close `fh'
end

capture program drop export_model_input_csv
program define export_model_input_csv
	syntax, MODEL(string) OUTFILE(string)

	tempname bmat vmat fh
	local dq = char(34)

	capture estimates restore `model'
	if _rc {
		di as err "Estimation results not found: `model'"
		exit 111
	}

	matrix `bmat' = e(b)
	matrix `vmat' = e(V)
	local cnames : colfullnames `bmat'
	local k = colsof(`bmat')

	local n_keep = 0
	forvalues j = 1/`k' {
		local term : word `j' of `cnames'
		if strpos("`term'", "b.") | strpos("`term'", "o.") {
			continue
		}

		local term_label "`term'"
		if ("`term'" == "student") local term_label "ReferenceAdultStudent"
		if ("`term'" == "year_c2015") local term_label "YearFrom2015"
		if ("`term'" == "ln_disp_inc") local term_label "LnDispInc"
		if ("`term'" == "ln_disp_inc2") local term_label "LnDispInc_sq"
		if ("`term'" == "2.age_group") local term_label "Age20to24"
		if ("`term'" == "3.age_group") local term_label "Age25to29"
		if ("`term'" == "4.age_group") local term_label "Age30to34"
		if ("`term'" == "5.age_group") local term_label "Age35to39"
		if ("`term'" == "6.age_group") local term_label "Age40to44"
		if ("`term'" == "7.age_group") local term_label "Age45to49"
		if ("`term'" == "8.age_group") local term_label "Age50to54"
		if ("`term'" == "9.age_group") local term_label "Age55to59"
		if ("`term'" == "10.age_group") local term_label "Age60to64"
		if ("`term'" == "11.age_group") local term_label "Age65to69"
		if ("`term'" == "12.age_group") local term_label "Age70to74"
		if ("`term'" == "13.age_group") local term_label "Age75to79"
		if ("`term'" == "14.age_group") local term_label "Age80plus"
		if ("`term'" == "single_female_adult") local term_label "SingleFemale"
		if ("`term'" == "single_male_adult") local term_label "SingleMale"
		if ("`term'" == "nk") local term_label "NumberMembersUnder18"
		if ("`term'" == "ft") local term_label "NumberFullTimeWork"
		if ("`term'" == "pt") local term_label "NumberPartTimeWork"
		if ("`term'" == "semp") local term_label "NumberSelfEmployed"
		if ("`term'" == "pen_inc_any") local term_label "PrivatePensionIncome"
		if ("`term'" == "1.housing_status") local term_label "MortgageHolder"
		if ("`term'" == "2.housing_status") local term_label "HomeOwnedOutright"
		if ("`term'" == "1.gorx") local term_label "UKC"
		if ("`term'" == "2.gorx") local term_label "UKD"
		if ("`term'" == "3.gorx") local term_label "UKE"
		if ("`term'" == "4.gorx") local term_label "UKF"
		if ("`term'" == "5.gorx") local term_label "UKG"
		if ("`term'" == "6.gorx") local term_label "UKH"
		if ("`term'" == "8.gorx") local term_label "UKJ"
		if ("`term'" == "9.gorx") local term_label "UKK"
		if ("`term'" == "10.gorx") local term_label "UKL"
		if ("`term'" == "11.gorx") local term_label "UKM"
		if ("`term'" == "12.gorx") local term_label "UKN"
		if ("`term'" == "year") local term_label "Year"
		if ("`term'" == "2009.year") local term_label "Year2009"
		if ("`term'" == "2010.year") local term_label "Year2010"
		if ("`term'" == "2011.year") local term_label "Year2011"
		if ("`term'" == "2012.year") local term_label "Year2012"
		if ("`term'" == "2013.year") local term_label "Year2013"
		if ("`term'" == "2014.year") local term_label "Year2014"
		if ("`term'" == "2015.year") local term_label "Year2015"
		if ("`term'" == "2016.year") local term_label "Year2016"
		if ("`term'" == "2017.year") local term_label "Year2017"
		if ("`term'" == "2018.year") local term_label "Year2018"
		if ("`term'" == "2019.year") local term_label "Year2019"
		if ("`term'" == "2020.year") local term_label "Year2020"
		if ("`term'" == "2021.year") local term_label "Year2021"
		if ("`term'" == "2022.year") local term_label "Year2022"
		if ("`term'" == "2023.year") local term_label "Year 2023"
		if ("`term'" == "_cons") local term_label "Constant"

		if ("`term'" == "2.age_group#c.ln_disp_inc") local term_label "Age20to24_LnDispInc"
		if ("`term'" == "3.age_group#c.ln_disp_inc") local term_label "Age25to29_LnDispInc"
		if ("`term'" == "4.age_group#c.ln_disp_inc") local term_label "Age30to34_LnDispInc"
		if ("`term'" == "5.age_group#c.ln_disp_inc") local term_label "Age35to39_LnDispInc"
		if ("`term'" == "6.age_group#c.ln_disp_inc") local term_label "Age40to44_LnDispInc"
		if ("`term'" == "7.age_group#c.ln_disp_inc") local term_label "Age45to49_LnDispInc"
		if ("`term'" == "8.age_group#c.ln_disp_inc") local term_label "Age50to54_LnDispInc"
		if ("`term'" == "9.age_group#c.ln_disp_inc") local term_label "Age55to59_LnDispInc"
		if ("`term'" == "10.age_group#c.ln_disp_inc") local term_label "Age60to64_LnDispInc"
		if ("`term'" == "11.age_group#c.ln_disp_inc") local term_label "Age65to69_LnDispInc"
		if ("`term'" == "12.age_group#c.ln_disp_inc") local term_label "Age70to74_LnDispInc"
		if ("`term'" == "13.age_group#c.ln_disp_inc") local term_label "Age75to79_LnDispInc"
		if ("`term'" == "14.age_group#c.ln_disp_inc") local term_label "Age80plus_LnDispInc"

		local ++n_keep
		local idx`n_keep' `j'
		local lbl`n_keep' "`term_label'"
	}

	file open `fh' using "`outfile'", write text replace
	file write `fh' `"`dq'REGRESSOR`dq',`dq'COEFFICIENT`dq'"'
	forvalues j = 1/`n_keep' {
		file write `fh' `",`dq'`lbl`j''`dq'"'
	}
	file write `fh' _n

	forvalues r = 1/`n_keep' {
		local rr = `idx`r''
		local b_i = string(`bmat'[1,`rr'], "%24.17g")
		file write `fh' `"`dq'`lbl`r''`dq',`b_i'"'

		forvalues c = 1/`n_keep' {
			local cc = `idx`c''
			local v_rc = string(`vmat'[`rr',`cc'], "%24.17g")
			file write `fh' `",`v_rc'"'
		}
		file write `fh' _n
	}
	file close `fh'
end

capture program drop export_income_elasticity_csv
program define export_income_elasticity_csv
    syntax, OUTFILE(string) C25(real) C50(real) C75(real) C90(real) MEAN(real)
    tempname fh
    file open `fh' using "`outfile'", write text replace
    file write `fh' "age_group,point,ln_disp_inc,elasticity,std_error,p_value" _n
    forvalues g=1/14 {
        foreach point in p25 mean p50 p75 p90 {
            if "`point'"=="p25" local at=`c25'
            if "`point'"=="mean" local at=`mean'
            if "`point'"=="p50" local at=`c50'
            if "`point'"=="p75" local at=`c75'
            if "`point'"=="p90" local at=`c90'
            local contrast "_b[ln_disp_inc] + 2*_b[ln_disp_inc2]*(`at')"
            if `g'>1 local contrast "`contrast' + _b[`g'.age_group#c.ln_disp_inc]"
            quietly lincom `contrast'
            file write `fh' "`g',`point',`=string(`at',"%24.17g")',`=string(r(estimate),"%24.17g")',`=string(r(se),"%24.17g")',`=string(r(p),"%24.17g")'" _n
        }
    }
    file close `fh'
end


/**************************************************************************************
*	1. Estimation sample and modelling variables
**************************************************************************************/
use "`outdir'/lcfs_sorted.dta", clear

* Require the revised construction and use the same population scope in validation.
confirm variable income_definition income_scope_ok
* 22 October 2026: both own income and broad expenditure exclude the same
* adult-share allocation of Council Tax/NI rates. Other cash outflows remain.
assert income_definition=="personal_net_ct"
confirm variable expenditure_definition local_tax_bu
assert expenditure_definition=="broad_net_ct"
keep if income_scope_ok==1
di as text "Loaded personal-net-income LCFS benefit-unit file"
describe

gen byte sample_log = (consmpn > 0 & disp_inc > 0) if !missing(consmpn, disp_inc)
count
local N_all = r(N)
count if sample_log == 1
local N_log = r(N)
di as text "Total BU observations: `N_all'"
di as text "Log-usable BU observations: `N_log'"

keep if sample_log == 1

global inflation_minyear = 2008
global inflation_maxyear = 2025
matrix inflation = ( ///
0.862 \ /// 2008
0.879 \ /// 2009
0.901 \ /// 2010
0.936 \ /// 2011
0.96  \ /// 2012
0.982 \ /// 2013
0.996 \ /// 2014
1     \ /// 2015
1.01  \ /// 2016
1.036 \ /// 2017
1.06  \ /// 2018
1.078 \ /// 2019
1.089 \ /// 2020
1.116 \ /// 2021
1.205 \ /// 2022
1.286 \ /// 2023
1.329 \ /// 2024
1.329)  /// 2025 //to update when becomes available

gen CPI = .
forvalues yy = $inflation_minyear/$inflation_maxyear {

	replace CPI = inflation[`yy'-${inflation_minyear}+1,1] if year == `yy'
}
foreach var in consmpn disp_inc {

	gen `var'r = `var' / CPI
}

gen ln_consmpn = ln(consmpnr * 365.25/7/12)
gen ln_disp_inc = ln(disp_incr * 365.25/7/12)
gen ln_disp_inc2 = ln_disp_inc^2

summ ln_disp_inc
local mean_ln_disp_inc = r(mean)
gen c_ln_disp_inc = ln_disp_inc - `mean_ln_disp_inc'
gen c_ln_disp_inc2 = c_ln_disp_inc^2

quietly summ ln_disp_inc, detail
local p25_ln_disp_inc = r(p25)
local p50_ln_disp_inc = r(p50)
local p75_ln_disp_inc = r(p75)
local p90_ln_disp_inc = r(p90)
local c25_ln_disp_inc = `p25_ln_disp_inc' - `mean_ln_disp_inc'
local c50_ln_disp_inc = `p50_ln_disp_inc' - `mean_ln_disp_inc'
local c75_ln_disp_inc = `p75_ln_disp_inc' - `mean_ln_disp_inc'
local c90_ln_disp_inc = `p90_ln_disp_inc' - `mean_ln_disp_inc'

recode age ///
	(min/19 = 1 "Under 20") ///
	(20/24 = 2 "20-24") ///
	(25/29 = 3 "25-29") ///
	(30/34 = 4 "30-34") ///
	(35/39 = 5 "35-39") ///
	(40/44 = 6 "40-44") ///
	(45/49 = 7 "45-49") ///
	(50/54 = 8 "50-54") ///
	(55/59 = 9 "55-59") ///
	(60/64 = 10 "60-64") ///
	(65/69 = 11 "65-69") ///
	(70/74 = 12 "70-74") ///
	(75/79 = 13 "75-79") ///
	(80/max = 14 "80+"), gen(age_group)

label var sample_log "Positive consmpn and disp_inc sample"
label var ln_consmpn "Log broad BU expenditure after local tax per month"
label var ln_disp_inc "Log benefit-unit disposable income per month"
label var c_ln_disp_inc "Centered log benefit-unit disposable income"
label var c_ln_disp_inc2 "Centered log benefit-unit disposable income squared"
label var age_group "Age group"

gen byte housing_status = .
replace housing_status = 0 if inlist(a122, 1, 2, 3, 4, 8)
replace housing_status = 1 if inlist(a122, 5, 6)
replace housing_status = 2 if (a122 == 7)
label define housing_status_lbl ///
	0 "Non-owner" ///
	1 "Owner with ongoing payments" ///
	2 "Owned outright", replace
label values housing_status housing_status_lbl
label var housing_status "Housing status from a122, with non-owner as benchmark"

save "`writingdir'/lcfs_model_sample.dta", replace


/**************************************************************************************
*	2. Sample profile
**************************************************************************************/
di as text "Sample profile after log restriction"
summ ln_consmpn ln_disp_inc ln_disp_inc2 age na nk ft pt semp y pen_inc ///
	wght weighta, detail

tab year
tab na
tab nk
tab mar
tab single_female_adult
tab single_male_adult
tab pen_inc_any
tab age_group
tab housing_status, missing
tab gorx


/**************************************************************************************
*	3. Production regression
**************************************************************************************/
drop single_female_adult single_male_adult
gen single_female_adult = (na==1) * female
gen single_male_adult = (na==1) * (1 - female)
local demo_controls_agegrp i.age_group single_female_adult single_male_adult nk
* Full-time student status belongs to the same reference adult as age.
* Keep the existing age bands and income slopes; retain common log-income curvature.
assert inlist(student,0,1)
local work_controls ft pt semp pen_inc_any student
local hh_controls ib0.housing_status ib7.gorx
di as text "Model m4: age-specific income slopes, common curvature, non-owner benchmark"
egen long regression_hh = group(year caseno)
* Centre the calendar trend without changing its fitted relationship.
gen double year_c2015 = year - 2015
reg ln_consmpn ln_disp_inc ln_disp_inc2 i.age_group#c.ln_disp_inc `demo_controls_agegrp' `work_controls' `hh_controls' year_c2015 i.year ///
	[pweight=weighta] if (na<3 & na>0), vce(cluster regression_hh)
estimates store m4
estimates save "`writingdir'/consumption_model_housing_status.ster", replace

* Age-specific elasticities are exported below using joint coefficient contrasts.

/**************************************************************************************
*	4. Export CSV output
**************************************************************************************/
export_model_csv, model("m4") outfile("`writingdir'/consumption_model_housing_status.csv")
export_income_elasticity_csv, ///
	outfile("`writingdir'/consumption_model_housing_status_elasticities.csv") ///
	c25(`p25_ln_disp_inc') c50(`p50_ln_disp_inc') c75(`p75_ln_disp_inc') c90(`p90_ln_disp_inc') mean(`mean_ln_disp_inc')
export_model_input_csv, model("m4") outfile("`modelinputdir'/consumption_model_housing_status.csv")

di as text "CSV results exported to:"
di as text "  `writingdir'/consumption_model_housing_status.csv"
di as text "  `writingdir'/consumption_model_housing_status_elasticities.csv"
di as text "  `modelinputdir'/consumption_model_housing_status.csv"


* FW1a residual dispersion is exported separately from coefficient covariance.
estimates restore m4
tempname rmsefile
file open `rmsefile' using "`modelinputdir'/consumption_model_housing_status_rmse.csv", write text replace
file write `rmsefile' "REGRESSOR,COEFFICIENT" _n
file write `rmsefile' "FW1a,`=string(e(rmse),"%24.17g")'" _n
file close `rmsefile'
tempname metafile
file open `metafile' using "`modelinputdir'/consumption_model_housing_status_fit.csv", write text replace
file write `metafile' "model,observations,r_squared,rmse,household_clusters" _n
file write `metafile' "m4,`=e(N)',`=string(e(r2),"%24.17g")',`=string(e(rmse),"%24.17g")',`=e(N_clust)'" _n
file close `metafile'

log close
