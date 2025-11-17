/*************************************************************************************************
* PROJECT:   UKMOD update – create employment history data from UKHLS
* FILE:      05_Newentrant1.do
*
* PURPOSE:
*   Constructs “new entrant” employment spells for all available waves (A–latest).
*   For each wave, identifies individuals who recently entered employment,
*   infers start and end dates, and creates wave-specific spell files.
*
* NOTES:
*   Patryk’s comment said that b_jbbgdat{y,m,d} were missing from earlier release data
*   and obtained from Graham. It looks like they were added to the new release,
*   therefore special treatment of wave B is no longer needed.
*************************************************************************************************/

cap log close
log using "${dir_log_emphist}/05_Newentrant1.log", replace

/******************************************************************************
 * WAVE A: process separately (Nick Buck original logic)
 ******************************************************************************/
di as text "------------------------------------------------------"
di as text "Processing new entrant spells for wave a (numeric 1)"
di as text "------------------------------------------------------"

capture use "${dir_ukhls_data}/a_indresp.dta", clear

    tab a_jbbgm a_jbsemp
    tab a_jbbgy if a_jbbgm > 0 & a_jbsemp > 0
    tab a_jbbgy if a_jbbgm < 0 & a_jbsemp > 0
    tab a_jbhad
    drop if a_ivfio==2
    rename a_* *
    gen spell=0
    gen wave=1
    gen espstat=jbstat
    replace espstat=1 if jbsemp==2
    replace espstat=2 if jbsemp==1
    tab espstat jbsemp
    gen endyear=intdaty_dv
    gen endmonth=intdatm_dv
    gen endday=intdatd_dv
    gen startmonth=-9
    gen startyear=-9
    gen startday=-9
    replace startyear=jbbgy if jbsemp > 0
    replace startmonth=jbbgm if jbsemp > 0
    replace startday=jbbgd if jbsemp > 0
    tab jlendm jbhad
    replace startyear=jlendy if jbhad==1
    replace startmonth=jlendm if jbhad==1
    replace startday=1 if jbhad==1
    tab jbhad jbsemp
    tab jbstat if jbhad==2
    replace startyear=2007 if jbhad==2
    replace startmonth=1 if jbhad==2
    replace startday=1 if jbhad==2
    tab startyear
    tab jbstat if startyear < 0
    replace startyear=2007 if startyear < 0
    tab startyear if startmonth < 0
    replace startmonth=1 if startmonth < 0
    tab espstat
    tab jbstat
    gen ne=1
    keep pidp spell wave espstat endyear endmonth endday startmonth startyear startday intdaty_dv intdatm_dv ne
    save a_sp0_ne, replace

    di as text "Saved a_sp0_ne.dta successfully."


/******************************************************************************
 * LOOP THROUGH WAVES (b ... n) using master globals
 ******************************************************************************/
local wps  ${UKHLS_waves_prefixed}
local wvno ${UKHLS_panel_waves_numbers}

local n : word count `wps'

forvalues i = 1/`n' {
    local wp : word `i' of `wps'     // prefix e.g. b_
    local wn : word `i' of `wvno'    // numeric e.g. 2

    di as text "------------------------------------------------------"
    di as text "Processing new entrant spells for wave `wp' (numeric `wn')"
    di as text "------------------------------------------------------"

    use "${dir_ukhls_data}/`wp'indresp.dta", clear
    
    * harmonise variable names (=remove wave prefix)
    rename `wp'* *

    /*************************************************************************************************
    * CHECK THAT REQUIRED VARIABLES EXIST
    * (if some essential vars missing, warn and skip)
    *************************************************************************************************/
    local reqvars "pidp ivfio hhorig notempchk empchk jbsemp jbstat intdaty_dv intdatm_dv intdatd_dv jbhad jlendy jlendm jbbgy jbbgm jbbgd"
    local missing_vars

    foreach v of local reqvars {
        capture confirm variable `v'
        if _rc local missing_vars "`missing_vars' `v'"
    }

    if "`missing_vars'" != "" {
        di as error "WARNING: Missing variables in wave `wp': `missing_vars'"
        di as text  "Skipping this wave..."
        continue
    }

    /*************************************************************************************************
    * BASIC SUMMARY FOR DIAGNOSTICS
    *************************************************************************************************/
    di as text "Variable overview for wave `wp':"
    summarize jbsemp jbstat jbhad jbbgy jbbgm jbbgd jlendy jlendm

    /*************************************************************************************************
    * FILTER AND PROCESS
    *************************************************************************************************/
    drop if ivfio == 2           // exclude proxy interviews
    capture confirm variable hhorig
    if !_rc {
        drop if hhorig == 8          // exclude temporary members (if variable exists)
    }

    gen aehhas = 1
    replace aehhas = 0 if notempchk == -8 & empchk == -8
    keep if aehhas == 0

    * Fill jbbg values if missing
    capture confirm variable jbbgdaty
    if !_rc {
        replace jbbgy=jbbgdaty if jbbgy < 0 & jbbgdaty > 0 & jbbgdaty != .
        replace jbbgm=jbbgdatm if jbbgm < 0 & jbbgdatm > 0 & jbbgdatm != .
        replace jbbgd=jbbgdatd if jbbgd < 0 & jbbgdatd > 0 & jbbgdatd != .
    }

    gen spell = 0
    gen wave  = `wn'

    * Define employment status at spell end
    gen espstat = jbstat
    replace espstat = 1 if jbsemp == 2
    replace espstat = 2 if jbsemp == 1

    gen endyear  = intdaty_dv
    gen endmonth = intdatm_dv
    gen endday   = intdatd_dv

    * Default missing start dates
    gen startyear  = -9
    gen startmonth = -9
    gen startday   = -9

    * Fill start date from job start info (if employed)
    replace startyear  = jbbgy if jbsemp > 0
    replace startmonth = jbbgm if jbsemp > 0
    replace startday   = jbbgd if jbsemp > 0

    * For those who had a job previously (jbhad == 1)
    replace startyear  = jlendy if jbhad == 1
    replace startmonth = jlendm if jbhad == 1
    replace startday   = 1      if jbhad == 1

    * If no job since 2007, assign default early date
    replace startyear  = 2007 if jbhad == 2 | startyear < 0
    replace startmonth = 1    if jbhad == 2 | startmonth < 0
    replace startday   = 1    if jbhad == 2 | startday < 0

    * Flag for new entrant
    gen ne = 1

    /*************************************************************************************************
    * SAVE WAVE-SPECIFIC SPELL FILE
    *************************************************************************************************/
    keep pidp spell wave espstat endyear endmonth endday ///
         startmonth startyear startday intdaty_dv intdatm_dv ne

    save `wp'sp0_ne, replace

    di as text "Saved `wp'sp0_ne.dta successfully."
}

/******************************************************************************
 * END
 ******************************************************************************/
di as text "------------------------------------------------------"
di as text "All available waves processed. Check logs for warnings."
di as text "------------------------------------------------------"

cap log close
