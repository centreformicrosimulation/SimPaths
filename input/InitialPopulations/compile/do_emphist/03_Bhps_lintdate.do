/*************************************************************************************************
* PROJECT:   UKMOD update – create employment history data from UKHLS & BHPS
* FILE:      b_lint.do
*
* PURPOSE:
*   Bridges the BHPS and UKHLS panels by identifying the *most recent BHPS interview*
*   for each respondent before their first UKHLS interview (wave B).
*
* CONTEXT:
*   - The BHPS (1991–2008) sample was incorporated into UKHLS starting in wave B (2009–10).
*   - This script links the BHPS interview history to the first UKHLS observation
*     so that employment and household histories remain continuous across the two panels.
*   - It uses BHPS individual response data (waves L–R) and the combined UKHLS intdate file.
*
* OUTPUTS:
*   - bhps_lint.dta : most recent BHPS interview date before UKHLS
*   - b_lint.dta     : previous interview info for wave B (merged BHPS or wave A)
*************************************************************************************************/

cap log close
log using "${dir_log_emphist}/03_Bhps_lintdate.log", replace

/*************************************************************************************************
* BUILD BHPS LAST INTERVIEW FILE (1991–2008)
*************************************************************************************************/

use "${dir_bhps_data}/xwaveid_bh", clear

gen lwint     = 0
gen lintdatd  = 0
gen lintdatm  = 0
gen lintdaty  = 0

* Define BHPS waves included (update global once in master file)
local waves $BHPS_waves
local nwaves : word count `waves'

forvalues i = 1/`nwaves' {
    local w : word `i' of `waves'

    * Merge BHPS individual response data for this wave
    merge 1:1 pidp using "${dir_bhps_data}/b`w'_indresp", ///
        keepusing(b`w'_istrtdatd b`w'_istrtdatm b`w'_istrtdaty b`w'_ivfio)
    
    * Keep valid (non-proxy) interviews
    replace lintdatd = b`w'_istrtdatd if b`w'_ivfio == 1
    replace lintdatm = b`w'_istrtdatm if b`w'_ivfio == 1
    replace lintdaty = b`w'_istrtdaty if b`w'_ivfio == 1
    replace lwint    = `i' if b`w'_ivfio == 1
    
    drop _merge
}

keep if lwint > 0
keep pidp lwint lintdatd lintdatm lintdaty
save bhps_lint, replace


/*************************************************************************************************
* LINK BHPS TO UKHLS WAVE B 
*************************************************************************************************/

use intdate1, clear

* Merge with BHPS last interview info
merge 1:1 pidp using bhps_lint
drop if _merge == 2   // BHPS-only cases (not in UKHLS)

* Keep only those with full interviews in wave B
keep if b_ivfio == 1

tab memorig

* Initialise
gen b_lwint     = 0
gen b_lintdaty  = -9
gen b_lintdatm  = -9
gen b_lintdatd  = -9

* Link to UKHLS wave A (if available)
replace b_lwint    = 1             if a_ivfio == 1
replace b_lintdatd = a_intdatd_dv  if a_ivfio == 1
replace b_lintdatm = a_intdatm_dv  if a_ivfio == 1
replace b_lintdaty = a_intdaty_dv  if a_ivfio == 1

* Replace with BHPS last interview info where available (merge==3)
replace b_lwint    = lwint + 11    if _merge == 3
replace b_lintdatd = lintdatd      if _merge == 3
replace b_lintdatm = lintdatm      if _merge == 3
replace b_lintdaty = lintdaty      if _merge == 3

tab b_lwint

keep pidp b_lwint b_lintdaty b_lintdatm b_lintdatd
save b_lint, replace

clear
cap log close
