/********************************************************************************

FILE TO EXTRACT UKHLS DATA FOR INITIALISING SIMPATHS POPULATION 

AUTH: Patryk Bronka (PB), Justin van de Ven (JV)
DATE: 07/09/2023 (JV)

NOTES:
dwt:	weight, hhdenus_xw(1) hhdenub_xw(2-5) hhdenui_xw(6+)
dgn:	gender, sex
dag:	age, 	dvage

********************************************************************************/


clear
set more off


/********************************************************************************
	local data directories
********************************************************************************/

* folder to store working data
global data_dir "C:\MyFiles\99 DEV ENV\JAS-MINE\SimPaths\input\InitialPopulations\compile\data"

* folder containing UKHLS (Understanding Society - 6614) data
global ukhls_data "C:\MyFiles\01 DATA\UK\us2009-21\stata\stata13_se\ukhls"


/********************************************************************************
	local working variables
********************************************************************************/
global age_become_responsible 18
global UKHLSwaves_bh "a b c d e f g h i j k l"


/********************************************************************************
	pool data
********************************************************************************/
cd "$data_dir"
foreach w of global UKHLSwaves_bh {

	local waveno=strpos("abcdefghijklmnopqrstuvwxyz","`w'")

	use "$ukhls_data/`w'_indall", clear
	merge m:1 `w'_hidp using "$ukhls_data/`w'_hhresp", keep(1 3) nogen
	
	rename *, l
	rename `w'_* * 
	gen swv = `waveno'
	
	if (`waveno' > 1) append using "$data_dir/pool_indall.dta"
	save "pool_indall.dta", replace
}


/********************************************************************************
	evaluate variables of interest
********************************************************************************/
use "pool_indall.dta", clear
sort pidp swv

// household cross-sectional weights
gen dwt = .
replace dwt = hhdenus_xw if (swv==1)
replace dwt = hhdenub_xw if (swv>=2 & swv<=5)
replace dwt = hhdenui_xw if (swv>=6)
drop if missing(dwt)

// gender (male = 1)
gen dgn = (sex==1)
replace dgn = 1 if (sex<0 & pidp[_n-1]==pidp & sex[_n-1]==1)
replace dgn = 1 if (sex<0 & pidp[_n+1]==pidp & sex[_n+1]==1)

// age
gen dag = dvage
replace dag = intdaty_if - intdaty_if[_n-1] + dvage[_n-1] if (dvage<0 & pidp[_n-1]==pidp & intdaty_if[_n-1]>0 & dvage[_n-1]>=0)
replace dag = intdaty_if - intdaty_if[_n+1] + dvage[_n+1] if (dvage<0 & pidp[_n+1]==pidp & intdaty_if[_n+1]>0 & dvage[_n+1]>=0)
drop if dag<0
gen dag_sq = dag*dag

