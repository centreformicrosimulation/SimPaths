version 17.0
clear all
set more off

* Paths (edit if needed)
local work_dir "/Users/pineapple/Library/CloudStorage/OneDrive-UniversityofEssex/WorkCEMPA/SimPathsUK/validate_alignments"
local input_dir  "`work_dir'/input"

* ============================================================
* 1) Load and append all initial population files 2011-2023
* ============================================================
tempfile appended
clear
save `appended', emptyok replace

forval yr = 2011/2023 {
    capture import delimited using "`input_dir'/population_initial_UK_`yr'.csv", ///
        clear varnames(1) bindquote(strict)
    if _rc != 0 {
        di as error "WARNING: could not load population_initial_UK_`yr'.csv — skipping"
        continue
    }
    destring demage idpers idbu idmother idfather wgthhcross, replace force
    gen int file_year = `yr'
    append using `appended'
    save `appended', replace
}

use `appended', clear

* ============================================================
* 2) Define partnered share — BU membership logic
*
*  Eligible  : demage >= 18
*  Partnered : there exists at least one OTHER adult (age >= 18)
*              in the same benefit unit (file_year x idbu)
*              who is NOT this person's mother or father
*
*  idpartner is NOT used — partnership is inferred purely from
*  BU co-residency, consistent with getPartner() logic.
* ============================================================
keep if demage >= 18

* Keep only the variables needed for the join
keep file_year idbu idpers idmother idfather demage wgthhcross

* --- Step A: build list of BU adult members to self-join against ---
preserve
    keep file_year idbu idpers
    rename idpers other_idpers
    tempfile bu_adults
    save `bu_adults', replace
restore

* --- Step B: self-join on (file_year, idbu) ---
* Each person is expanded against all adults in their BU
joinby file_year idbu using `bu_adults'

* Remove self-matches
drop if other_idpers == idpers

* Remove cases where the co-resident is this person's parent
drop if other_idpers == idmother | other_idpers == idfather

* Each remaining row means a qualifying co-resident exists
gen byte has_partner = 1

* Reduce to one row per person: partnered = 1 if any qualifying row
collapse (max) partnered = has_partner, by(file_year idbu idpers)

* --- Step C: save partnered flags, then merge into full eligible pop ---
* joinby silently drops persons with NO qualifying co-resident, so we
* must restore them from the full dataset and assign partnered = 0.

tempfile partnered_flags
save `partnered_flags', replace

* Reload full eligible population as master
use `appended', clear
destring demage idpers idbu idmother idfather wgthhcross, replace force
keep if demage >= 18
keep file_year idbu idpers wgthhcross

* Merge partnered flags in as using — unmatched master = no partner
merge 1:1 file_year idbu idpers using `partnered_flags', ///
    keep(master match) nogen
replace partnered = 0 if missing(partnered)

* ============================================================
* 3) Collapse to annual weighted share
* ============================================================
collapse (mean) partnered_share = partnered ///
         (sum)  n_eligible = partnered      ///   raw count only for reference
         [pw = wgthhcross], by(file_year)

* n_eligible above is sum of weights — replace with unweighted count if preferred
* For unweighted N alongside weighted share, use a two-step approach:
* Step 1: save weighted share
tempfile weighted_share
save `weighted_share', replace

* Step 2: unweighted counts
use `appended', clear
destring demage idpers idbu wgthhcross, replace force
keep if demage >= 18
keep file_year idbu idpers

merge 1:1 file_year idbu idpers using `partnered_flags', ///
    keep(master match) nogen
replace partnered = 0 if missing(partnered)

collapse (count) n_eligible = idpers ///
         (sum)   n_partnered = partnered, by(file_year)

merge 1:1 file_year using `weighted_share', nogen

rename file_year year

format partnered_share %12.7f

label var n_eligible      "Eligible persons (age >= 18, unweighted N)"
label var n_partnered     "Partnered persons (unweighted N)"
label var partnered_share "Partnered share (weighted by wgthhcross)"

order year n_eligible n_partnered partnered_share

* ============================================================
* 4) Full comparison table
* ============================================================
//export excel using "`work_dir'/partnered_share_initialPop_BUlogic.xlsx", ///
//    firstrow(variables) replace

* ============================================================
* 5) Slim target-format file: year + partnered_share only
* ============================================================
preserve
    keep year partnered_share
    format partnered_share %12.7f
    export excel using "`work_dir'/partnered_share_targets_BUlogic.xlsx", ///
        firstrow(variables) replace
restore

list, sep(0)
