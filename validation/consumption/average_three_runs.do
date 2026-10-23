********************************************************************************
* V3 validation with separate national income quintiles within each run/year.
* Run from repository root. Arguments: three simulation CSV directories,
* prepared survey output_lcfs directory, then a separate output directory.
* Exports CSVs; does not overwrite regression inputs or edit Excel workbooks.
********************************************************************************
args run606_dir run607_dir run608_dir survey_dir output_dir
if (`"`run606_dir'"'=="" | `"`run607_dir'"'=="" | `"`run608_dir'"'=="" | `"`survey_dir'"'=="" | `"`output_dir'"'=="") {
    di as error "Supply three simulation CSV directories, the survey directory and a separate output directory."
    exit 198
}
set more off
capture mkdir "`output_dir'"
foreach seed in 606 607 608 {
    capture mkdir "`output_dir'/seed_`seed'"
}
do "validation/consumption/consumption_sim_v3.do" "`run606_dir'" "`output_dir'/seed_606"
do "validation/consumption/consumption_sim_v3.do" "`run607_dir'" "`output_dir'/seed_607"
do "validation/consumption/consumption_sim_v3.do" "`run608_dir'" "`output_dir'/seed_608"
do "validation/consumption/consumption_surv_v3.do" "`survey_dir'" "`output_dir'"

tempfile stacked
local first=1
foreach seed in 606 607 608 {
    import delimited "`output_dir'/seed_`seed'/simulation_validation_stats.csv", clear asdouble
    gen int seed=`seed'
    if !`first' append using "`stacked'"
    save "`stacked'", replace
    local first=0
}
sort seed year
export delimited using "`output_dir'/individual_run_statistics.csv", replace
ds seed year, not
local metrics `r(varlist)'
preserve
collapse (mean) `metrics', by(year)
export delimited using "`output_dir'/simulation_validation_stats.csv", replace
restore
collapse (sd) `metrics', by(year)
export delimited using "`output_dir'/simulation_validation_stats_sd.csv", replace
