/*************************************************************************************************
* PROJECT:   UKMOD update – create employment history data from UKHLS
* FILE:      04_Sp0_1_2a.do
*
* PURPOSE:
*   Constructs employment history “spells” for each UKHLS wave (b–n).
*   For each wave, it:
*     - Identifies employment/non-employment episodes and transitions
*     - Determines start and end dates of each spell
*     - Produces three datasets: sp0 (initial), sp1 (main), sp2 (reshaped)
*
* CONTEXT:
*   - Uses previous interview information from ${wp}lint.dta
*   - Requires individual respondent data from ${original_data}/${wp}indresp.dta
*
* OUTPUTS:
*   - ${wp}sp0.dta : initial spell definitions
*   - ${wp}sp1.dta : continuation spells
*   - ${wp}sp2.dta : reshaped multi-episode structure
*************************************************************************************************/

cap log close
log using "${dir_log_emphist}/04_Sp0_1_2a.log", replace


local wps  ${UKHLS_waves_prefixed}
local wvno ${UKHLS_panel_waves_numbers}

local n : word count `wps'     // number of waves to process


/*************************************************************************************************
* LOOP THROUGH EACH WAVE
*************************************************************************************************/

forvalues i = 1/`n' {

    global wp : word `i' of `wps'    // wave prefix (e.g. b_, c_, etc.)
    global wv : word `i' of `wvno'   // wave numeric label

    di as text "------------------------------------------------------"
    di as text "Processing wave ${wp} (numeric ${wv})..."
    di as text "------------------------------------------------------"


    /*************************************************************************************************
    * PREPARE INDRESP DATA AND MERGE WITH PREVIOUS INTERVIEW FILE
    *************************************************************************************************/

    use "${dir_ukhls_data}/${wp}indresp.dta", clear
    keep if ${wp}ivfio == 1                 // keep full interviews only
    drop if ${wp}hhorig == 8                // drop non-original HH members

    merge 1:1 pidp using ${wp}lint
    drop _merge

    rename ${wp}* *                         // remove wave prefix 

    keep pidp jbsemp jbstat notempchk - nxtst nxtstelse - cjbatt ///
        ff_ivlolw ff_emplw ff_jbsemp ff_jbstat intdatd_dv intdatm_dv intdaty_dv ///
        lwint lintdaty lintdatm lintdatd


    /*************************************************************************************************
    * DEFINE EMPLOYMENT FLAGS AND END DATE VARIABLES
    *************************************************************************************************/

    gen aehhas = 1
    replace aehhas = 0 if empchk == -8 & notempchk == -8
    keep if aehhas == 1

    gen enddatestat = 0
    replace enddatestat = 1 if empchk == 1
    replace enddatestat = 2 if notempchk == 1 & empchk != 1
    replace enddatestat = 3 if empchk == 2
    replace enddatestat = 4 if notempchk == 2 & empchk == -8
    replace enddatestat = 1 if enddatestat == 0 & empchk != -8
    replace enddatestat = 2 if enddatestat == 0 & notempchk != -8
    replace enddatestat = 5 if enddatestat == 1 & (jbsamr == 2 | samejob == 2)

    gen endday   = intdatd_dv if enddatestat < 3
    gen endmonth = intdatm_dv if enddatestat < 3
    gen endyear  = intdaty_dv if enddatestat < 3

    replace endday   = jbendd  if enddatestat == 5
    replace endmonth = jbendm  if enddatestat == 5
    replace endyear  = jbendy4 if enddatestat == 5

    replace endday   = empstendd  if inlist(enddatestat, 3, 4)
    replace endmonth = empstendm  if inlist(enddatestat, 3, 4)
    replace endyear  = empstendy4 if inlist(enddatestat, 3, 4)

    save ${wp}sp1a, replace        // store intermediate version


    /*************************************************************************************************
    * CREATE SPELL 0 DATASET (INITIAL EPISODE)
    *************************************************************************************************/

    gen startday   = lintdatd
    gen startmonth = lintdatm
    gen startyear  = lintdaty
    gen stdatestat = 1

    gen espstat = jbstat
    replace espstat = 1 if jbsemp == 2
    replace espstat = 2 if jbsemp == 1
    replace espstat = ff_jbstat if enddatestat == 4
    replace espstat = 1 if enddatestat == 3 & ff_jbsemp == 2
    replace espstat = 2 if enddatestat == 3 & ff_jbsemp == 1
    replace espstat = 2 if enddatestat == 5 & espstat > 2

    gen wave  = ${wv}
    gen spell = 0

    keep pidp wave spell lwint - espstat lintdatd lintdatm lintdaty intdatm_dv intdaty_dv
    save ${wp}sp0, replace


    /*************************************************************************************************
    * CREATE SPELL 1 DATASET (CONTINUATION EPISODES)
    *************************************************************************************************/

    use ${wp}sp1a, clear
    keep if enddatestat > 2

    rename endday startday
    rename endmonth startmonth
    rename endyear startyear
    gen stdatestat = 2

    rename enddatestat edstat1

    * Determine new end dates
    gen enddatestat = 0
    replace enddatestat = 1 if cjob == 1
    replace enddatestat = 3 if cjob == 2
    replace enddatestat = 2 if cstat == 2 & enddatestat == 0
    replace enddatestat = 4 if cstat == 1 & enddatestat == 0
    replace enddatestat = 1 if enddatestat == 0 & jbsemp != -8
    replace enddatestat = 2 if enddatestat == 0 & jbsemp == -8

    gen endday   = intdatd_dv if enddatestat < 3
    gen endmonth = intdatm_dv if enddatestat < 3
    gen endyear  = intdaty_dv if enddatestat < 3

    replace endday   = nxtjbendd  if enddatestat == 3
    replace endmonth = nxtjbendm  if enddatestat == 3
    replace endyear  = nxtjbendy4 if enddatestat == 3

    replace endday   = nxtstendd  if enddatestat == 4
    replace endmonth = nxtstendm  if enddatestat == 4
    replace endyear  = nxtstendy4 if enddatestat == 4

    gen espstat = jbstat if enddatestat == 2
    replace espstat = 1 if jbsemp == 2 & enddatestat == 1
    replace espstat = 2 if jbsemp == 1 & enddatestat == 1
    replace espstat = nxtstelse + 2 if enddatestat == 4 & nxtstelse > 0
    replace espstat = nxtstelse     if enddatestat == 4 & nxtstelse > -8 & nxtstelse < 0
    replace espstat = 1 if enddatestat == 3 & nxtjbes == 2
    replace espstat = 2 if enddatestat == 3 & nxtjbes > -8 & nxtjbes < 2
    replace espstat = 2 if enddatestat == 1 & missing(espstat)

    gen wave  = ${wv}
    gen spell = 1

    keep pidp wave spell lwint startday - espstat lintdatd lintdatm lintdaty intdatm_dv intdaty_dv
    save ${wp}sp1, replace


    /*************************************************************************************************
    * CREATE SPELL 2 DATASET (RESHAPED MULTI-EPISODE STRUCTURE)
    *************************************************************************************************/

    use "${dir_ukhls_data}/${wp}indresp.dta", clear
    rename ${wp}* *
    keep if ivfio == 1
    drop if hhorig == 8

    keep pidp nextstat* nextelse* currstat* nextjob* currjob* jobhours* statendd* statendm* statendy4*

    reshape long nextstat nextelse currstat nextjob currjob jobhours statendd statendm statendy4, i(pidp) j(sp2)
    drop if nextstat == -8

    quietly merge m:1 pidp using "${dir_ukhls_data}/${wp}indresp", ///
        keepusing(${wp}intdatd_dv ${wp}intdatm_dv ${wp}intdaty_dv)
    keep if _merge == 3
    drop _merge

    merge m:1 pidp using ${wp}lint
    keep if _merge == 3
    drop _merge
    rename ${wp}* *

    gen enddatestat = 0
    replace enddatestat = 1 if currjob == 1
    replace enddatestat = 3 if currjob == 2
    replace enddatestat = 4 if currstat == 1
    replace enddatestat = 2 if currstat > -8 & enddatestat == 0

    gen endday   = intdatd_dv 
    gen endmonth = intdatm_dv 
    gen endyear  = intdaty_dv 

    replace endday   = statendd  if enddatestat > 2
    replace endmonth = statendm  if enddatestat > 2
    replace endyear  = statendy4 if enddatestat > 2

    gen espstat = nextstat
    replace espstat = nextelse + 2 if nextelse > 0
    replace espstat = nextelse     if nextstat == 2 & nextelse < 0
    replace espstat = 1 if nextjob == 1
    replace espstat = 2 if nextjob > 1
    replace espstat = 2 if nextjob > -8 & nextjob < 0

    gen spell = sp2 + 1
    gen wave  = ${wv}

    keep pidp spell wave endday endmonth endyear enddatestat espstat lintdatd lintdatm lintdaty intdatm_dv intdaty_dv
    save ${wp}sp2, replace
}

cap log close
