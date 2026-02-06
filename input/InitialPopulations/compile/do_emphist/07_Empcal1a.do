/*************************************************************************************************
* PROJECT:   UKMOD update – create employment calendar and per-wave employment history
* FILE:      07_Empcal1a.do
*
* PURPOSE:
*   - Build a monthly employment calendar (2007 onward) from all employment spells.
*   - Derive per-wave employment history variables needed for UKMOD.
*   - Output one per-wave file (b_emphist, ..., n_emphist) with summary measures.
*
* INPUTS:
*   allspells1ok.dta - individual-level employment spells constructed in 06_Aspells1.do
*   ${original_data}\<wave>_indresp.dta   - wave-specific interview response data
*
* OUTPUTS:
*   ${data}\<wave>_emphist.dta  - per-wave employment history summary files
*   ${data}\temp_liwwh.dta - long file with all waves appended 
*************************************************************************************************/
local baseyr 2007 //==> All subsequent month indexing is relative to January 2007.

use allspells1ok, clear   // Load the prepared spell data

*-------------------------------------------------------------*
* Convert start and end dates into months since base year (Jan 2007)
*-------------------------------------------------------------*

gen stmy07 = 12 * (startyear - `baseyr') + startmonth
gen enmy07 = 12 * (endyear  - `baseyr') + endmonth

*-------------------------------------------------------------*
* Simplified employment status: 2 = employed, 1 = not employed
*-------------------------------------------------------------*
fre espstat
gen emp=1 if espstat > 2
replace emp=2 if espstat < 3
tab espstat emp
 
*-------------------------------------------------------------*
* Determine full observed month range
*-------------------------------------------------------------*
summ enmy07, meanonly
local maxm = r(max)
local minm = 1   // start from month 1 to avoid negatives

di as txt "Detected month range: " as res "`minm'–`maxm' (" as res `=`maxm'-`minm'+1' " months total)"

*-------------------------------------------------------------*
* Generate monthly employment indicators (esp# = status each month)
*-------------------------------------------------------------*
forvalues i = `minm'/`maxm' {
    gen esp`i' = 0
    replace esp`i' = emp if `i' >= stmy07 & `i' <= enmy07
}

*-------------------------------------------------------------*
* Collapse multiple spells per person (2 overrides 1)
*-------------------------------------------------------------*
forvalues i = `minm'/`maxm' {
    bys pidp: egen memp`i' = max(esp`i')
}

*-------------------------------------------------------------*
* Keep one row per person and retain key variables
*-------------------------------------------------------------*
bys pidp: gen seq = _n
keep if seq == 1
keep pidp memorig memp`minm'-memp`maxm'

/*-------------------------------------------------------------*
* Count employed months per financial year (April–March) ==> not sure is this is needed so coded out for now
*-------------------------------------------------------------*
summ memp*, meanonly

local fy_start = `baseyr'
local fy_end   = floor(`baseyr' + (`maxm' + 8) / 12)   // +8 ensures FY covers Apr–Mar

forvalues y = `fy_start'/`fy_end' {
    local fy = substr("`y'",3,2)                 // e.g. 2007 → "07"
    local start = (12 * (`y' - `baseyr')) + 4    // April of FY
    local end   = (12 * (`y' - `baseyr' + 1)) + 3 // March next year

    * Clip to observed range
    if `start' < `minm' local start = `minm'
    if `end' > `maxm' local end = `maxm'

    * Count months employed (status = 2)
    gen efy`fy' = 0
    forvalues i = `start'/`end' {
        replace efy`fy' = efy`fy' + 1 if memp`i' == 2
    }

    di as txt "FY" `y' "/" `= `y'+1' " → months " as res "`start'–`end'"
}
*/

save empcal1a, replace
/*we end up with a monthy calendar of activity (i.e. employed or not) for each individual from Jan 2007*/

/*************************************************************************************************
  Derive wave-specific employment history summaries
  --------------------------------------------------------------------
  For each wave, merge with interview date, calculate employment duration
  up to that interview month (liwwh), and short-term employment indicators:
     empmonth   - months employed in 6 months before interview
     mismonth   - months missing in last 6 months
     empmonth12 - months employed in 12 months before interview
*************************************************************************************************/

local waves $UKHLS_panel_waves
//local waves b

foreach w of local waves {

    di "---------------------------------------------------------"
    di "Processing WAVE `w' ..."
    di "---------------------------------------------------------"

    use empcal1a, clear

    merge 1:1 pidp using "${dir_ukhls_data}/`w'_indresp", ///
        keepusing(`w'_intdatm_dv `w'_intdaty_dv `w'_ivfio)
    keep if _merge == 3
    drop _merge
    drop if `w'_ivfio == 2     // exclude proxy interviews

    * Interview month index relative to base year (Jan 2007)
    gen inmy07 = 12*(`w'_intdaty_dv - `baseyr') + `w'_intdatm_dv

    
    *------------------------------------------
    * Total months employed up to interview
    *------------------------------------------
    gen liwwh = 0
    summarize inmy07, meanonly
    local maxm = r(max)
    forvalues i = 1/`maxm' {
        replace liwwh = liwwh + 1 if memp`i' == 2 & `i' <= inmy07
    }

	*------------------------------------------
    * Short-term employment summaries
    *------------------------------------------
    summarize inmy07, meanonly
    local maxm = r(max)
    local start6m = `maxm' - 6
    local start12m = `maxm' - 12

    gen empmonth = 0
    gen mismonth = 0
    gen empmonth12 = 0

    forvalues i = `start6m'/`maxm' {
        replace empmonth = empmonth + 1 if memp`i' == 2 & inmy07 == `maxm'
        replace mismonth = mismonth + 1 if memp`i' == 0 & inmy07 == `maxm'
    }
    forvalues i = `start12m'/`maxm' {
        replace empmonth12 = empmonth12 + 1 if memp`i' == 2 & inmy07 == `maxm'
    }
    
	*------------------------------------------
    * Keep and label key variables
    *------------------------------------------
    keep pidp `w'_intdatm_dv `w'_intdaty_dv liwwh empmonth mismonth empmonth12 //efy1 efy2
    label var liwwh      "Total months in employment up to current interview"
    label var empmonth   "Months employed in last 6 months before interview"
    label var mismonth   "Months missing in last 6 months before interview"
    label var empmonth12 "Months employed in last 12 months before interview"

    save `w'_emphist, replace
}

di as txt "All waves (B–N) processed successfully."

*------------------------------------------ 
* Combine per-wave employment history files
*------------------------------------------

* Convert global into a local list and remove first letter (because we start from wave c)
local waves $UKHLS_panel_waves
local first : word 1 of `waves'
local waves : list waves - first

display "Waves to append: `waves'"

use b_emphist, clear 
gen wave = "b"

foreach w of local waves {
    display "Appending wave `w'..."
    append using `w'_emphist, generate(flag_`w')
    replace wave = "`w'" if flag_`w' == 1
    drop flag_`w'
}

* generate wave identifier 
gen swv = .

local letters $UKHLS_panel_waves
local numbers $UKHLS_panel_waves_numbers

local n : word count `letters'
forval i = 1/`n' {
    local wv : word `i' of `letters'
    local num : word `i' of `numbers'
    replace swv = `num' if wave == "`wv'"
}

gen idperson=pidp 

save temp_liwwh.dta, replace

duplicates report swv idperson 
bys swv: sum liwwh

cap log close 

/**************************************************************************************
* clean-up and exit
*************************************************************************************/

#delimit ;
local files_to_drop 
allspells1.dta    
a_sp0_ne.dta      
bhps_lint.dta     
b_emphist.dta     
b_lint.dta        
b_sp0.dta   
b_sp0_ne.dta
b_sp1.dta
b_sp1a.dta
b_sp2.dta
c_emphist.dta
c_lint.dta
c_sp0.dta
c_sp0_ne.dta
c_sp1.dta
c_sp1a.dta
c_sp2.dta
d_emphist.dta
d_lint.dta
d_sp0.dta
d_sp0_ne.dta
d_sp1.dta
d_sp1a.dta
d_sp2.dta
e_emphist.dta
e_lint.dta
e_sp0.dta
e_sp0_ne.dta
e_sp1.dta
e_sp1a.dta
e_sp2.dta
f_emphist.dta
f_lint.dta
f_sp0.dta
f_sp0_ne.dta
f_sp1.dta
f_sp1a.dta
f_sp2.dta
g_emphist.dta
g_lint.dta
g_sp0.dta
g_sp0_ne.dta
g_sp1.dta
g_sp1a.dta
g_sp2.dta
h_emphist.dta
h_lint.dta
h_sp0.dta
h_sp0_ne.dta
h_sp1.dta
h_sp1a.dta
h_sp2.dta
intdate.dta
intdate1.dta
i_emphist.dta
i_lint.dta
i_sp0.dta
i_sp0_ne.dta
i_sp1.dta
i_sp1a.dta
i_sp2.dta
j_emphist.dta
j_lint.dta
j_sp0.dta
j_sp0_ne.dta
j_sp1.dta
j_sp1a.dta
j_sp2.dta
k_emphist.dta
k_lint.dta
k_sp0.dta
k_sp0_ne.dta
k_sp1.dta
k_sp1a.dta
k_sp2.dta
l_emphist.dta
l_lint.dta
l_sp0.dta
l_sp0_ne.dta
l_sp1.dta
l_sp1a.dta
l_sp2.dta
m_emphist.dta
m_lint.dta
m_sp0.dta
m_sp0_ne.dta
m_sp1.dta
m_sp1a.dta
m_sp2.dta
n_emphist.dta
n_lint.dta
n_sp0.dta
n_sp0_ne.dta
n_sp1.dta
n_sp1a.dta
n_sp2.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data_emphist/`file'"
}

