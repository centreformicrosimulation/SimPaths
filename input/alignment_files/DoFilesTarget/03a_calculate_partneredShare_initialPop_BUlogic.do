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
    destring demage idpers idbu idmother idfather, replace force
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
keep file_year idbu idpers idmother idfather demage

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
* Fix: use eligible_pop as MASTER and merge partnered flags as USING,
* so unmatched master rows (the lone adults) remain with partnered = .
* then replace . with 0.

tempfile partnered_flags
save `partnered_flags', replace

* Reload full eligible population as master
use `appended', clear
destring demage idpers idbu idmother idfather, replace force
keep if demage >= 18
keep file_year idbu idpers

* Merge partnered flags in as using — unmatched master = no partner
merge 1:1 file_year idbu idpers using `partnered_flags', ///
    keep(master match) nogen
replace partnered = 0 if missing(partnered)

* ============================================================
* 3) Collapse to annual share
* ============================================================
gen byte eligible = 1

collapse (sum) n_partnered = partnered ///
         (sum) n_eligible  = eligible, ///
         by(file_year)

rename file_year year

gen double partnered_share = n_partnered / n_eligible
format partnered_share %12.7f

label var n_eligible      "Eligible persons (age >= 18)"
label var n_partnered     "Partnered persons (BU logic)"
label var partnered_share "Partnered share (initial populations, BU logic)"

order year n_eligible n_partnered partnered_share

* ============================================================
* 4) Full comparison table
* ============================================================
export excel using "`work_dir'/partnered_share_initialPop_BUlogic.xlsx", ///
    firstrow(variables) replace

* ============================================================
* 5) Slim target-format file: year + partnered_share only
* ============================================================
preserve
    keep year partnered_share
    format partnered_share %12.7f
    export excel using "`work_dir'/alignment_targets_partnered_share.xlsx", ///
        firstrow(variables) replace
restore

* Add metadata sheet so workbook provenance survives regeneration
putexcel set "`work_dir'/alignment_targets_partnered_share.xlsx", sheet("Info") modify
putexcel A1=("Field") B1=("Value")
putexcel A2=("Workbook") B2=("alignment_targets_partnered_share.xlsx")
putexcel A3=("Data sheet") B3=("partnered_share")
putexcel A4=("Relevant do file") B4=("input/alignment_files/DoFilesTarget/03a_calculate_partneredShare_initialPop_BUlogic.do")
putexcel A5=("Related do file") B5=("input/alignment_files/DoFilesTarget/03b_calculate_partnership_target.do")
putexcel A6=("Source data") B6=("Initial population CSV files for 2011-2023")
putexcel A7=("Years covered") B7=("2011-2023")
putexcel A8=("Unit of analysis") B8=("Adult person-year")
putexcel A9=("Eligible population") B9=("Adults aged 18+")
putexcel A10=("Partner definition") B10=("Partnered = 1 when another adult exists in the same BU after excluding self, mother, and father")
putexcel A11=("Partner identifier use") B11=("idpartner is not used; partnership is inferred from adult BU co-residency")
putexcel A12=("Weighting") B12=("Unweighted annual share")
putexcel A13=("How target is computed") B13=("Build adult BU roster, self-join by year and BU, drop self and parents, mark qualifying co-residents, merge flags back to the full adult population, then compute partnered_share = n_partnered / n_eligible by year")
putexcel A14=("Output meaning") B14=("Annual partnered share among adults aged 18+ under BU-logic partner inference")

list, sep(0)
