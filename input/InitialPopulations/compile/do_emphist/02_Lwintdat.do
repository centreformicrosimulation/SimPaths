*************************************************************************************************
* PROJECT:   UKMOD update – create employment history data from UKHLS
* FILE:      02_Lwintdate.do
*
* PURPOSE:
*   Creates variables identifying the *previous interview date* for each wave
*   (from c to n) based on cross-wave interview date information.
*
* CONTEXT:
*   - Uses the cross-wave dataset created in 01_Intdate.do.
*   - For each wave, finds the respondent’s most recent previous interview
*     (if any) and records its date (year, month, day) and wave number.
*   - The first two waves (a, b) have no valid "previous" interview, so
*     processing starts from wave c.
*
* OUTPUTS:
*   - Variables: <wave>_lwint, <wave>_lintdaty, <wave>_lintdatm, <wave>_lintdatd
*   - Files:     <wave>_lint.dta (for each wave c–n)
*************************************************************************************************

cap log close
log using "${dir_log_emphist}/02_Lwintdate.log", replace

use intdate, clear


*************************************************************************************************
* DEFINE WAVES AND ASSOCIATED VARIABLES
*************************************************************************************************
local waves $UKHLSwaves // copy global into a local for use here
           

* Build lists of corresponding variable names for each wave
local rvars
local yvars
local mvars
local dvars

foreach w of local waves {
    local rvars "`rvars' `w'_ivfio"         // fieldwork outcome (1 = full interview)
    local yvars "`yvars' `w'_intdaty_dv"    // interview year
    local mvars "`mvars' `w'_intdatm_dv"    // interview month
    local dvars "`dvars' `w'_intdatd_dv"    // interview day
}

local nwaves : word count `waves'


*************************************************************************************************
* CREATE VARIABLES FOR PREVIOUS INTERVIEW DATES
*************************************************************************************************
forvalues w = 3/`nwaves' {                         // start from wave c
    local curwave : word `w' of `waves'            // current wave (e.g., "c")
    local prevmax = `w' - 1                        // number of prior waves

    di as text "Processing wave `curwave' (previous up to wave `prevmax')"

    * Initialise variables for this wave
    gen `curwave'_lwint     = 0                   // previous wave index number
    gen `curwave'_lintdaty  = -9                  // previous interview year
    gen `curwave'_lintdatm  = -9                  // previous interview month
    gen `curwave'_lintdatd  = -9                  // previous interview day

    * Check all earlier waves to find last valid interview
    forvalues i = 1/`prevmax' {
        local rw : word `i' of `rvars'
        local yw : word `i' of `yvars'
        local mw : word `i' of `mvars'
        local dw : word `i' of `dvars'

        * Replace if respondent was interviewed in both current and earlier wave
        replace `curwave'_lwint    = `i'  if `curwave'_ivfio==1 & `rw'==1
        replace `curwave'_lintdaty = `yw' if `curwave'_ivfio==1 & `rw'==1
        replace `curwave'_lintdatm = `mw' if `curwave'_ivfio==1 & `rw'==1
        replace `curwave'_lintdatd = `dw' if `curwave'_ivfio==1 & `rw'==1
    }
}


*************************************************************************************************
* SAVE INTERMEDIATE DATASET WITH ALL WAVES
*************************************************************************************************
save intdate1, replace
drop if memorig==8   // exclude temporary or non-original household members


*************************************************************************************************
* EXPORT WAVE-SPECIFIC FILES
*************************************************************************************************
foreach w of local waves {
    if inlist("`w'", "a", "b") continue           // skip first two waves (no prior interviews)

    di as text "Saving previous interview data for wave `w'..."

    keep if `w'_ivfio==1                          // respondents with valid interview
    keep pidp `w'_lwint `w'_lintdaty `w'_lintdatm `w'_lintdatd

    save `w'_lint, replace                        // e.g., "c_lint.dta", "d_lint.dta", etc.

    use intdate1, clear                           // reload full dataset for next wave
    drop if memorig==8
}



clear
cap log close
















