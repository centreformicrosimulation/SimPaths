/*************************************************************************************************
* PROJECT:   UKMOD update – create employment history data from UKHLS
* FILE:      06_Aspells1.do
*
* PURPOSE:
*   Combines all wave-specific employment spell files (sp0, sp0_ne, sp1, sp2)
*   into a single dataset covering all available waves.
*   Derives consistent start and end dates, imputes missing dates,
*   and removes invalid or inconsistent spells.
*
* NOTES:
*   - BHPS-origin members receive approximate start dates if missing.
*************************************************************************************************/

cap log close
log using "${dir_log_emphist}/06_Aspells1.log", replace

/*************************************************************************************************
 * INITIALISE AND APPEND SPELL FILES
 *************************************************************************************************/

di as text "------------------------------------------------------"
di as text "Combining wave-specific spell files into one dataset"
di as text "------------------------------------------------------"

* Start with wave a
use a_sp0_ne, clear

* Loop through later waves using global list from master file
local wps ${UKHLS_waves_prefixed}
local n : word count `wps'

forvalues i = 1/`n' {
    local wp : word `i' of `wps'
    di as text "Appending spell files for wave `wp'..."

    capture append using `wp'sp0
    capture append using `wp'sp0_ne
    capture append using `wp'sp1
    capture append using `wp'sp2
}

di as text "All wave-specific spell files appended successfully."

/*************************************************************************************************
 * MERGE WITH CROSS-WAVE IDENTIFIER
 *************************************************************************************************/

di as text "Merging with xwaveid file to obtain memorig variable..."
merge m:1 pidp using "${dir_ukhls_data}/xwaveid", keepusing(memorig)
keep if _merge == 3
drop _merge

/*************************************************************************************************
 * IMPUTE MISSING BHPS INTERVIEW DATES
 *************************************************************************************************/

di as text "Applying BHPS date fix for legacy members..."
gen bhps = 0
replace bhps = 1 if memorig > 2 & memorig < 7

replace startyear  = 2008 if bhps == 1 & startyear  == -9
replace startmonth = 9    if bhps == 1 & startmonth == -9
replace startday   = 1    if bhps == 1 & startday   == -9

/*************************************************************************************************
 * ADJUST START AND END DATES USING INTERVIEW TIMING
 *************************************************************************************************/

di as text "Adjusting spell dates relative to previous and current interviews..."

gen valdat1 = 0
replace valdat1 = 1 if lintdaty > 0 & lintdatm > 0 & endyear > 0 & endmonth > 0
gen durat1 = 12 * (endyear - lintdaty) + (endmonth - lintdatm) if valdat1 == 1

gen valdat2 = 0
replace valdat2 = 1 if lintdaty > 0 & lintdatm > 0 & startyear > 0 & startmonth > 0
gen durat2 = 12 * (startyear - lintdaty) + (startmonth - lintdatm) if valdat2 == 1

replace endyear  = lintdaty if durat1 < 0
replace endmonth = lintdatm if durat1 < 0
replace endday   = lintdatd if durat1 < 0
replace startyear  = lintdaty if durat2 < 0
replace startmonth = lintdatm if durat2 < 0
replace startday   = lintdatd if durat2 < 0

/*************************************************************************************************
 * FILL START DATES FROM PREVIOUS SPELLS
 *************************************************************************************************/

sort pidp wave spell
replace startyear  = endyear[_n-1]  if spell > 1
replace startmonth = endmonth[_n-1] if spell > 1
replace startday   = endday[_n-1]   if spell > 1

/*************************************************************************************************
 * COMPUTE MIDPOINT DATES (FOR MISSING VALUES)
 *************************************************************************************************/

gen lint00  = 12 * (lintdaty - 2000) + lintdatm if lintdaty > 0 & lintdatm > 0
gen int00   = 12 * (intdaty_dv - 2000) + intdatm_dv if intdaty_dv > 0 & intdatm_dv > 0
gen interval = int00 - lint00
gen mint00  = lint00 + round(interval / 2)
gen midyear = 2000 + int(mint00 / 12)
gen midmonth = mint00 - 12 * int(mint00 / 12)
replace midyear  = midyear - 1 if midmonth == 0
replace midmonth = 12          if midmonth == 0

/*************************************************************************************************
 * MANUAL IMPUTATIONS FOR PARTIAL MISSING MONTHS
 *************************************************************************************************/

replace endmonth   = 1  if endyear == intdaty_dv & endmonth < 0
replace endmonth   = 12 if endyear == lintdaty  & endmonth < 0
replace endmonth   = 6  if endyear > lintdaty & endyear < intdaty_dv & endmonth < 0

replace startmonth = endmonth[_n-1] if spell == 1 & startmonth < 0 & startyear > 0 & endmonth[_n-1] > 0

gen valstart = (startmonth > 0 & startyear > 0)
gen valend   = (endmonth > 0 & endyear > 0)

replace startmonth = midmonth if valstart == 0 & midmonth != .
replace startyear  = midyear  if valstart == 0 & midyear  != .
replace endmonth   = midmonth if valend   == 0 & midmonth != .
replace endyear    = midyear  if valend   == 0 & midyear  != .

/*************************************************************************************************
 * COMPUTE SPELL DURATION AND VALIDATION
 *************************************************************************************************/

gen valdat = (startyear > 0 & startmonth > 0 & endyear > 0 & endmonth > 0)
gen durat  = 12 * (endyear - startyear) + (endmonth - startmonth) if valdat == 1

save allspells1, replace

/*************************************************************************************************
 * FILTER AND CLEAN SPELLS
 *************************************************************************************************/

use allspells1, clear

gen d2 = (valdat == 0)
bys pidp: egen nd2 = sum(d2)
tab nd2

keep if nd2 == 0
keep if durat >= 0
drop if durat == .
drop if espstat < 0

save allspells1ok, replace

/*************************************************************************************************
 * END
 *************************************************************************************************/
di as text "------------------------------------------------------"
di as text "All spells processed and saved as allspells1ok.dta"
di as text "------------------------------------------------------"

cap log close
