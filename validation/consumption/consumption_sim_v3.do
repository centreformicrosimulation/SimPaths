***************************************************************************************
* PROJECT:              SUSTAINWELL
* DO-FILE NAME:         consumption_sim_v3.do
* DESCRIPTION:          GENERATE SUMMARY STATISTICS FROM SIMULATED DATA FOR VALIDATING
*						PROJECTION OF CONSUMPTION
***************************************************************************************
* COUNTRY:              UK
* AUTHORS: 				Justin van de Ven
* LAST UPDATE:          23 Oct 2026 - portable v3 replication
***************************************************************************************


/**********************************************************************
*	set-up
**********************************************************************/
* Optional arguments: input directory, then CSV output directory.
* With no arguments, use the local paths below. Workbook cells are refreshed separately.
args input_dir output_dir
if (`"`input_dir'"'=="" | `"`output_dir'"'=="") {
    di as error "Supply the source-data directory and a separate output directory."
    exit 198
}
set more off
global data_dir `"`input_dir'"'
global validation_dir `"`output_dir'"'
capture mkdir "${validation_dir}"
* V3 retains the original income screens and national annual income quintiles.
* Survey uses weighta; simulation is unweighted. Age 65+ denotes 65-84.
* The 2015 simulation is initialisation and must be exported as missing.
global start_year = 2015
global end_year = 2026


/**********************************************************************
*	prepare data for analysis
**********************************************************************/
import delimited "${data_dir}/BenefitUnit.csv", clear
* Preserve the original simulation income screen: real monthly disposable income.
* Survey uses a weekly screen; these thresholds are not harmonised.
drop if (ydispmonth < 10.0)
gen cons_to_inc = xdiscconsumptionannual / ydispmonth / 12.0
rename time year
* This assertion protects the treatment of this run's initialisation year.
assert xdiscconsumptionannual == 0 if year == 2015
replace cons_to_inc = . if year == 2015
rename agerefperson age
* CSV reference age can be numeric or text containing null.
capture confirm numeric variable age
if (_rc) destring age, replace ignore("null")


/**********************************************************************
*	analysis
**********************************************************************/
* Annual income quintiles are defined over ALL ages in each dataset separately.
* Each age-specific result then subsets those national quintiles; it does not
* recalculate income quintiles within an age group.
* All-age results retain missing reference ages. Age-specific results exclude them.
* The older band is 65-84, even where the historical workbook label reads 65+.
* Each reported statistic is the mean of unit-level C/Y, not mean(C)/mean(Y).
matrix stats = J(${end_year} - ${start_year} + 1, 24, .)
forvalues yy = $start_year / $end_year {

	local col = 1
	xtile pct1 = ydispmonth if (year==`yy'), nq(5)
	forvalues aa = 1/4 {

		if (`aa'==1) {
			gen age_grp = 1
		}
		else if (`aa'==2) {
			gen age_grp = (age>=0 & age<=44)
		}
		else if (`aa'==3) {
			gen age_grp = (age>=45 & age<=64)
		}
		else {
			gen age_grp = (age>=65 & age<=84)
		}

		sum cons_to_inc if (age_grp & year==`yy'), mean
		matrix stats[`yy'-${start_year}+1,`col'] = r(mean)
		local col = `col' + 1

		forvalues qq = 1/5 {

			sum cons_to_inc if (age_grp & pct1==`qq' & year==`yy'), mean
			matrix stats[`yy'-${start_year}+1,`col'] = r(mean)
			local col = `col' + 1
		}
		drop age_grp
	}
	drop pct1
}
matrix colnames stats = all_mean all_q1 all_q2 all_q3 all_q4 all_q5 under45_mean under45_q1 under45_q2 under45_q3 under45_q4 under45_q5 age45to64_mean age45to64_q1 age45to64_q2 age45to64_q3 age45to64_q4 age45to64_q5 age65to84_mean age65to84_q1 age65to84_q2 age65to84_q3 age65to84_q4 age65to84_q5
matlist stats

* Export full-precision results without changing the loaded source data or stats matrix.
* V3 retains the v2 layout and all 18 chart styles: survey 2015-2023,
* simulation 2015-2026, with all chart axes covering 2015-2023.
* CSV generation does not itself update workbook cells or native chart caches.
preserve
clear
svmat double stats, names(col)
gen year = ${start_year} + _n - 1
export delimited using "${validation_dir}/simulation_validation_stats.csv", replace
restore
