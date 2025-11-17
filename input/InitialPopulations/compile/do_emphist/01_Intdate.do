*************************************************************************************************
* PROJECT:   UKMOD update – create employment history data from UKHLS
* FILE:      01_Intdate.do
*
* PURPOSE:
*   Creates a *cross-wave file of interview dates* for all waves (a–n) of UKHLS.
*   This file is later used in 02_Lwintdate.do to determine the *previous*
*   interview date for each respondent.
*
* CONTEXT:
*   - Reads xwaveid (cross-wave identifier file) and merges interview date
*     variables from each wave’s individual response file (`_indresp`).
*   - Excludes proxy interviews (where `ivfio > 1`).
*   - Converts interview month/year into a continuous “months since 2009”
*     variable (`_mns09`).
*
* OUTPUTS:
*   - intdate.dta : combined dataset with interview dates and months-since-2009
*************************************************************************************************

cap log close 
log using "${dir_log_emphist}/01_Intdate.log", replace

* The list of waves is defined *globally* in 00_Master.do 
local waves $UKHLSwaves                        // copy global into a local for use here
local n: word count `waves'          // number of waves

********************************************************************
* MERGE INTERVIEW DATES FROM EACH WAVE
********************************************************************
use "${dir_ukhls_data}/xwaveid", clear

forvalues i = 1/`n' {
    local w : word `i' of `waves'
    
    * Merge interview date variables from each wave’s individual response file
merge 1:1 pidp using "${dir_ukhls_data}/`w'_indresp" , ///
    keepusing(`w'_intdatd_dv `w'_intdatm_dv `w'_intdaty_dv)

    
    * Exclude proxy interviews (ivfio > 1 means proxy or non-response)
    replace `w'_intdatd_dv = . if `w'_ivfio > 1
    replace `w'_intdatm_dv = . if `w'_ivfio > 1
    replace `w'_intdaty_dv = . if `w'_ivfio > 1
    
    drop _merge

    * Compute months since 2009 for timeline consistency
    gen `w'_mns09 = 12 * (`w'_intdaty_dv - 2009) + `w'_intdatm_dv ///
        if `w'_intdaty_dv > 0 & `w'_intdatm_dv > 0
    
    //tab `w'_mns09
}


save intdate, replace


clear
cap log close
