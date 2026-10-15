version 17.0
clear all
set more off

* Run from the SimPaths repository root. The source workbook is never changed.
local source "input/alignment_files/align_popProjections.xlsx"
local output "input/alignment_files/align_popProjections_no_ageing_2026.xlsx"

* The UK sheet has Gender, Region, Age in A:C and years 2011-2120 in D:DI.
* S is 2026; T:DI are 2027-2120. Retaining the full horizon keeps the
* counterfactual workbook usable if a run is extended beyond 2070.
import excel using "`source'", sheet("UK") cellrange(A2:DI2665) clear
confirm variable DI
assert _N == 2664
isid A B C
assert !missing(S) & S >= 0

* Save the source cells for an independent check of the exported workbook.
tempfile source_values
preserve
keep A B C R-DI
foreach column of varlist R-DI {
    rename `column' source_`column'
}
save `source_values'
restore

* Freeze the 2026 age distribution separately within each gender and region.
* Keep each group's projected annual population total, so the experiment
* changes age composition while retaining projected population growth and
* the projected gender/region totals. Years through 2026 remain untouched.
generate long __source_row = _n
bysort A B: egen double __base_total = total(S)
assert __base_total > 0

foreach column of varlist T-DI {
    bysort A B: egen double __projected_total = total(`column')
    assert __projected_total > 0
    replace `column' = S * (__projected_total / __base_total)
    assert !missing(`column') & `column' >= 0
    drop __projected_total
}

sort __source_row
mkmat T-DI, matrix(counterfactual)

* Copy the source so its Info sheet, year headers, keys, and formatting remain.
copy "`source'" "`output'", replace
putexcel set "`output'", sheet("UK") modify
putexcel T2 = matrix(counterfactual)

* Reopen the saved workbook, not the in-memory calculations.
import excel using "`output'", sheet("UK") cellrange(A2:DI2665) clear
isid A B C
merge 1:1 A B C using `source_values', assert(match) nogen
assert R == source_R & S == source_S
bysort A B: egen double __check_base_total = total(S)
foreach column of varlist T-DI {
    bysort A B: egen double __check_target_total = total(`column')
    bysort A B: egen double __check_source_total = total(source_`column')
    assert abs(__check_target_total - __check_source_total) < ///
        1e-8 * __check_source_total + 1e-6
    assert abs(`column' / __check_target_total - S / __check_base_total) < 1e-9
    drop __check_target_total __check_source_total
}

display as result "Created `output' with 2026 age shares and projected gender/region totals."
