/********************************************************************************
*
*	FILE TO EXTRACT UKHLS DATA FOR INITIALISING LABSIM POPULATION 
*
*	AUTH: Patryk Bronka (PB), Justin van de Ven (JV)
*	LAST EDIT: 09/09/2023 (JV)
*
********************************************************************************/


set more off
/********************************************************************************
	local data directories - commented out when using master program
********************************************************************************/

/*

* folder to store working data
global workingDir "C:\MyFiles\00 CURRENT\03 PROJECTS\Essex\SimPaths\02 PARAMETERISE\STARTING DATA\data"

* folder containing UKHLS (Understanding Society - 6614) data
global UKHLSDir "C:\MyFiles\01 DATA\UK\us2009-21\stata\stata13_se\ukhls"

* path to UK_Mcrsmltn_w_hrs.dta
global startData "C:\MyFiles\00 CURRENT\03 PROJECTS\Essex\SimPaths\02 PARAMETERISE\STARTING DATA\data\base data\UK_Mcrsmltn_w_hrs.dta"

* R script
* NOTE THAT WORKING DATA PATH ALSO NEEDS TO BE ADDED TO R SCRIPT
global rScript "C:/MyFiles/00 CURRENT/03 PROJECTS/Essex/SimPaths/02 PARAMETERISE/STARTING DATA/progs/01b Rscript.R"

* R program files
* NOTE THAT R NEEDS TO HAVE THE DATA.TABLE PACKAGE ADDED
global RtermPath `"C:/Program Files/R/R-4.3.1/bin/x64/Rterm.exe"'
global RtermOptions `"--vanilla"'
global RshellPath "C:/Program Files/R/R-4.3.1/bin/R.exe"

* youngest age for someone to be independent in the model
global ageMaturity 18

* waves reported by ukhls
global UKHLSWaves "a b c d e f g h i j k l"

* waves reporting social care module in ukhls
global socCareWaves "g i k"

* sample window for input data
global firstSimYear = 2010
global lastSimYear = 2017

* inflation adjustments
global cpi_minyear = 2009
global cpi_maxyear = 2019
global matrix cpi = (0.866 \ /// 2009
0.894 \ /// 2010
0.934 \ /// 2011
0.961 \ /// 2012
0.985 \ /// 2013
1.000 \ /// 2014
1.000 \ /// 2015
1.007 \ /// 2016
1.034 \ /// 2017
1.059 \ /// 2018
1.078)

*/


/********************************************************************************

Notes

*02/08/21, PB: I think lhw (hours worked weekly) can be set to 0 for the initial
population as it does not seem to be used before the labour supply model updates
it. If not the case, will have to be loaded from Understanding Society and merged

*Income variables need to be added when the UK_Mcrsmltn file has been updated 
by Cara - they seem to be missing.

********************************************************************************/


********************************************************************************
*Go to working directory
********************************************************************************
cd "$workingDir"


********************************************************************************
*Prepare additional variables from the UKHLS to merge in
*Currently using: pidp, hidp, pno, intdaty_dv
********************************************************************************

// loop through the relevant waves of Understanding Society
foreach w of global UKHLSWaves {

	local waveno=strpos("abcdefghijklmnopqrstuvwxyz","`w'")

	use `w'_hidp `w'_hsownd using "$UKHLSDir/`w'_hhresp", clear
	rename `w'_hidp idhh 
	gen dhh_owned = (`w'_hsownd == 1 | `w'_hsownd == 2 | `w'_hsownd == 3)
	gen swv = `waveno'
	cap rename `w'_* *
	if (`waveno' > 1) append using "add_vars_ukhls_hhresp.dta"
	save "add_vars_ukhls_hhresp.dta", replace

	use pidp `w'_hidp `w'_pno `w'_jbhrs `w'_jbot `w'_jshrs `w'_scghq1_dv `w'_scghq2_dv `w'_fimninvnet_dv `w'_fimngrs_dv `w'_fimnnet_dv `w'_fimnmisc_dv `w'_fimnprben_dv using "$UKHLSDir/`w'_indresp", clear
	rename pidp idperson
	rename `w'_hidp idhh
	gen swv = `waveno'
	rename `w'_* *
	if (`waveno' > 1) append using "add_vars_ukhls_indresp.dta"
	save "add_vars_ukhls_indresp.dta", replace
	
	use pidp `w'_hidp `w'_pno `w'_intdaty_dv `w'_mnspid `w'_fnspid `w'_ppno using "$UKHLSDir/`w'_indall", clear
	rename pidp idperson
	rename `w'_hidp idhh
	gen swv = `waveno'
	rename `w'_* *
	if (`waveno' > 1) append using "add_vars_ukhls.dta"
	save "add_vars_ukhls.dta", replace
}
merge 1:1 idperson idhh pno swv using "add_vars_ukhls_indresp.dta", keep(1 3) nogen
save "add_vars_ukhls.dta", replace


********************************************************************************
*Load longitudinal file used to produce estimates
********************************************************************************
use "$startData", clear

*Merge additional information from the UKHLS waves 1-8:
duplicates tag idperson idhh swv, gen(dup)
drop if dup == 1 //Only 48 obs. but could look into the reason for duplicates
drop dup
isid idperson idhh swv

merge 1:1 idperson idhh swv using "add_vars_ukhls.dta", keep(1 3) nogen
merge m:1 idhh swv using "add_vars_ukhls_hhresp.dta", keep(1 3) nogen

*If missing stm, use information from intdaty_dv
replace stm = intdaty_dv if missing(stm)

*Hours of work: lhw
recode jbhrs (-9/-1 . = .)
recode jbot (-9/-1 . = .)
recode jshrs (-9/-1 . = .)

//lhw is the sum of the above, but don't want to take -9 into account. Recode into missing value. 

*h_scghq1_dv for Likert GHQ 12 classification => rename to demographic : health : mental = dhm
rename scghq1_dv dhm
la var dhm "Psychological distress GHQ 12 Likert"
recode dhm (-9/-1 . = .)

recode scghq2_dv (-9/-1 . = .)

gen scghq2_dv_miss_flag = (scghq2_dv == .)


egen lhw=rowtotal(jbhrs jbot jshrs)
replace lhw = ceil(lhw)
replace lhw = 112 if lhw > 112 & !missing(lhw) //Working more than 112 hours a week seems unreasonable
la var lhw "Hours worked per week"

*Income CPI from ONS:
gen CPI = .
forval yy = $cpi_minyear/$cpi_maxyear {
	replace CPI = $cpi[`yy' - $cpi_minyear + 1] if stm == `yy'
}

*Income variables
*Use w_fihhmninv_dv / number of individuals in a household for investment income, and inc_stp for pension income?
*Note that fihhmniv_dv in the UKHLS has been divided by 12 to obtain a monthly value; also inc_stp is monthly as it is derived from frmnthimp_dv  
*Income values in the UKHLS are monthly. Take this into account when calculating yearly values in the simulation.

*Generate number of people in the household:
bys idhh (swv): gen nhhind = _N

*Generate sum of lodger and property income per household:
recode inc_rl inc_rp (. = 0)
bys idhh swv: egen sum_inc_rl_rp = total(inc_rl+inc_rp)

*gen ypncp = asinh(((fihhmninv_dv+sum_inc_rl_rp)/nhhind)*(1/CPI)) //Investment income from savings + rent from lodgers or property

gen gross_net_ratio = 1
replace gross_net_ratio = fimngrs_dv/fimnnet_dv 
replace gross_net_ratio = 1 if missing(gross_net_ratio)

*Looking at the data, it seems that fihhmninv_dv contains the property income already:
gen ypncp = asinh((fimninvnet_dv+fimnmisc_dv+fimnprben_dv)*gross_net_ratio*(1/CPI))

// Pension income, monthly
gen ypnoab = asinh((fimnpen_dv)*gross_net_ratio*(1/CPI))

rename drgnl drgn1 //Font in Stata is bad...second one uses number one at the end, like EUROMOD - it means demographic : region : NUTS 1 clasification

cap drop _*

*Regions in the simulation are coded as in EM: which means value == 3 is skipped
recode drgn1 (3 = 4) ///
	(4 = 5) ///
	(5 = 6) ///
	(6 = 7) ///
	(7 = 8) ///
	(8 = 9) ///
	(9 = 10) ///
	(10 = 11) ///
	(11 = 12) ///
	(12 = 13) ///
	, gen(drgn1_recode)
	
drop drgn1
rename drgn1_recode drgn1
	
*Some repeated observations, decide which to keep. Drop duplicates for now, but revise this to see if it can be improved. 
*One alternative would be to use swv instead of date of interview, and assign a year to each wave. But not sure how much that would disturb the data.

duplicates tag idperson stm, gen(dup)
replace stm = intdaty_dv if dup > 0
drop dup
duplicates tag idperson stm, gen(dup)
duplicates drop idperson stm, force

recode dgn (2 = 0) //Children added to the sample have not had sex coded as the rest of the sample, fix here

xtset idperson stm

*Try to fill in missing values 

replace idmother = mnspid if missing(idmother) & mnspid > 0
replace idfather = fnspid if missing(idfather) & fnspid > 0

//Fill in spouse's education
tempfile uk_temp_dehsp
preserve
keep idperson stm deh_c3
rename idperson idpartner
rename deh_c3 dehsp_c3_new
save uk_temp_dehsp, replace
restore

merge m:1 idpartner stm using uk_temp_dehsp, keep(1 3) nogen
replace dehsp_c3 = dehsp_c3_new if missing(dehsp_c3) & !missing(dehsp_c3_new)

//Fill in mother's education
tempfile uk_temp_dehm
preserve
keep if dgn == 0
keep idperson stm deh_c3
rename idperson idmother
rename deh_c3 dehm_c3_new
save uk_temp_dehm, replace
restore

merge m:1 idmother stm using uk_temp_dehm, keep(1 3) nogen
replace dehm_c3 = dehm_c3_new if missing(dehm_c3) & !missing(dehm_c3_new)

//Fill in father's education
tempfile uk_temp_dehf
preserve
keep if dgn == 1
keep idperson stm deh_c3
rename idperson idfather
rename deh_c3 dehf_c3_new
save uk_temp_dehf, replace
restore

merge m:1 idfather stm using uk_temp_dehf, keep(1 3) nogen
replace dehf_c3 = dehf_c3_new if missing(dehf_c3) & !missing(dehf_c3_new)

bys idperson (stm): carryforward dlrtrd, replace 
recode dlrtrd (. = 0) //Missing for children

*Dcpex can be generated on the basis of idpartner?
xtset idperson stm 
by idperson: gen dcpex2 = 1 if dcpst == 3 & L1.dcpst == 1 & idpartner == . 
by idperson: gen dcpen2 = 1 if dcpst == 1 & (L1.dcpst == 2 | L1.dcpst == 3) & idpartner != . //This turns out more restrictive than the current version


********************************************************************************
*Export dataset used to estimate parametric union matching with all the years  *
********************************************************************************
preserve
gen newMarriage = dcpen2
save "parametricUnionDataset", replace 
restore

/*
*Keep cross-section from a selected year
global year 2017 //Part of code in R doesn't run in a loop, so have to run this manually for each year
keep if stm == $year 
*/


***************************************************************************
*Prep data on children below 16 that were added to the sample earlier
***************************************************************************
gen app_flag = 1 if dag < 16 & !missing(dag)
sort stm idhh dag //Children last so values can be carriedforward
replace dag_sq = dag*dag if missing(dag_sq)
replace dcpst = 2 if app_flag == 1 //Children are single never married
replace deh_c3 = 3 if app_flag == 1 //Children have low level of education until they leave school
replace dhe = 5 if app_flag == 1 //Children have very good health TODO: could be better to impute or use predictions from our model


/******************************************************************************/

*replace dhe = 5 if app_flag == 1 //Children have very good health TODO: could be better to impute or use predictions from our model
replace dlltsd = 0 if app_flag == 1 //Children are not disabled or long-term sick TODO: could be better to impute or use predictions from our model
replace ded = 1 if app_flag == 1 //Children are in continuous education
replace der = 0 if app_flag == 1
replace les_c3 = 2 if app_flag == 1 //Children are full time students

gsort +idhh -dag
by idhh: carryforward drgn1 dhhwt dnc dnc02 ydses_c5 dhhtp_c4 if app_flag == 1, replace

*Will need partnership identifiers from UKHLS
order pno, before(swv)

order idhh idperson	idpartner idmother idfather pno
sort stm idhh idperson

*Generate dct (country code as in EM)
gen dct = 15

*Generate dwt
clonevar dwt = dhhwt 

*Might have to merge a weight in from the UKHLS as there are some missings?
bys idhh: egen max_dwt = max(dhhwt)
replace dwt = max_dwt if missing(dwt)
replace dwt = 0 if missing(dwt)

*Generate les_c4 variable in addition to the les_c3 variable. Les_c4 adds retired status. 
*Note: existing les_c4 variable had disabled as the 4th status, instead of retired
cap drop les_c4
clonevar les_c4 = les_c3
replace les_c4 = 4 if dlrtrd == 1

/*********************************************************************************/

preserve
drop if dgn < 0
eststo predict_dhe: reg dhe c.dag i.dgn i.swv if dag <= 20, vce(robust)
eststo predict_dhm: reg dhm c.dag i.dgn i.swv i.dhe if dag <= 20, vce(robust) // Physical health has a big impact, so included as covariate. Will require imputing dhe for children first. 
eststo predict_scghq2: reg scghq2_dv c.dag i.dgn i.swv i.dhe if dag <= 20, vce(robust)
restore

/*********************************************************************************/

estimates restore predict_dhe
predict dhe_prediction

replace dhe = round(dhe_prediction) if app_flag == 1
replace dhe = round(dhe_prediction) if missing(dhe) & dag <= 18


estimates restore predict_dhm
predict dhm_prediction
replace dhm = round(dhm_prediction) if app_flag == 1
replace dhm = round(dhm_prediction) if missing(dhm) & dag <= 18

estimates restore predict_scghq2
predict scghq2_prediction
replace scghq2_dv = round(scghq2_prediction) if app_flag == 1
replace scghq2_dv = round(scghq2_dv) if missing(scghq2_dv) & dag <= 18

*Generate a dummy variable for case-based psychological distress, based on the scghq2_dv variable >= 4
gen dhm_ghq = (scghq2_dv >= 4) 

* save pooled data
save temp2_here, replace


/*********************************************************************************/
*	LOOP OVER YEARS
/*********************************************************************************/
forvalues yy = $firstSimYear/$lastSimYear {
	
	* load pooled data
	use temp2_here, clear

	* limit year
	global year = `yy'
	keep if stm == $year 

	*Also generate lessp_c4 for the partner's value
	tempfile uk_temp_lesc4
	preserve
	keep idperson les_c4
	rename idperson idpartner
	rename les_c4 lessp_c4
	save "uk_temp_lesc4", replace
	restore

	merge m:1 idpartner using "uk_temp_lesc4"
	la var lessp_c4 "Partner's activity 4 categories"
	keep if _merge == 1 | _merge == 3
	drop _merge

	*Replace idpartner with -9 if missings
	recode idpartner(. = -9)
	
	*Generate adult child flag
	preserve
	keep if dgn == 0
	keep idhh idperson dag

	rename idperson idmother
	rename dag dagmother
	save "temp_mother_dag", replace
	restore, preserve
	keep if dgn == 1
	keep idhh idperson dag
	rename idperson idfather
	rename dag dagfather
	save "temp_father_dag", replace 
	restore

	merge m:1 idhh idmother using "temp_mother_dag"
	keep if _merge == 1 | _merge == 3
	drop _merge
	merge m:1 idhh idfather using "temp_father_dag"
	keep if _merge == 1 | _merge == 3
	drop _merge

	//Adult child is identified on the successful merge with mother / father in the same household and age
	gen adultChildFlag = (!missing(dagmother) | !missing(dagfather)) & dag >= $ageMaturity & idpartner <= 0
	*Introduce a condition that (adult) children cannot be older than parents-15 year of age
	replace adultChildFlag = 0 if dag >= dagfather-15 | dag >= dagmother-15 

	preserve
	keep idhh idperson dgn
	rename dgn dgn_partner
	rename idperson idpartner
	save "temp_sex", replace
	restore

	merge m:1 idhh idpartner using "temp_sex"
	keep if _merge == 1 | _merge == 3
	drop _merge
	gen same_sex_couple = 0
	replace same_sex_couple = 1 if dgn == dgn_partner & !missing(dgn) & !missing(dgn_partner)
	
	* recode same sex couples as singles
	replace idpartner = -9 if (same_sex_couple)
	replace dcpst = 2 if (same_sex_couple)
	foreach vv in dagsp dagsp_sq dehsp_c3 dhesp dlltsd_sp lessp_c3 ypnbihs_dv_sp yptciihs_dv_sp dagpns_sp lessp_c4 {
		replace `vv' = . if (same_sex_couple)
	}

	
	********************************
	*Split households
	********************************

	* adult defined as 18 or over, or if married
	gen adult = (dag>=$ageMaturity) * (dag<.)
	replace adult = 1 if (adult==0 & dcpst==1)
	gen child = 1 - adult
	
	* define benefit units
	gen long idbenefitunit = .
	replace idbenefitunit = idperson if (adult==1 & dgn==0)
	replace idbenefitunit = idpartner if (adult==1 & dgn==1 & ssscp==0 & same_sex_couple==0 & idpartner>0) // ignores same-sex couples
	replace idbenefitunit = idperson if (adult==1 & missing(idbenefitunit))
	replace idbenefitunit = idmother if (child==1)
	replace idbenefitunit = idfather if (child==1 & (missing(idbenefitunit) | idbenefitunit<0))
	replace idbenefitunit = . if (idbenefitunit<0)
	
	gsort idbenefitunit -dag
	by idbenefitunit: replace idhh = idhh[1] if (idhh != idhh[1])
	
	order idbenefitunit, after(idhh)
	sort idhh idbenefitunit
	
	/*
	*Number of people per household
	bys idhh: gen count = _N

	gen apartnum = cond(pno<ppno, pno, ppno) if ppno>0
	order apartnum, after(ppno)

	preserve
	keep idhh idperson apartnum
	rename idperson idmother
	rename apartnum apartnumm
	gen long idhhmother = idhh
	save "temp_mother", replace
	rename idmother idfather
	rename apartnum apartnumf
	gen long idhhfather = idhh
	save "temp_father", replace 
	restore

	merge m:1 idhh idmother using "temp_mother"
	keep if _merge == 1 | _merge == 3
	drop _merge
	merge m:1 idhh idfather using "temp_father"
	keep if _merge == 1 | _merge == 3
	drop _merge

	*Keep children under age to become responsible with parents unless their partner lives in the hh: (children above age to become responsible will create independent households)
	gen parent_apartnum = apartnumm
	replace parent_apartnum = apartnumf if missing(parent_apartnum) & !missing(apartnumf)
	replace apartnum = parent_apartnum if dag < $ageMaturity & missing(ppno)

	*Corresponds to point 1 of section 3.1.4 of labsim guide
	*Give new HH numbers where there is more than 1 couple / unit in the HH:
	egen newid = group(idhh apartnum)
	tostring(newid), replace
	replace newid = "999999"+newid if newid != "."
	destring(newid), replace
	replace idhh = newid if apartnum > 1 & !missing(apartnum)

	/*********************************Clean up*************************************/

	*Should people in single-person HH, who indicate that they are married, have the marital status updated to single?

	sort idhh idperson

	*Generate idfamily 
	gen double idfamily = idhh
	format idfamily %15.0g
	replace idfamily = idhhmother if adultChildFlag == 1 & !missing(idhhmother)
	replace idfamily = idhhfather if adultChildFlag == 1 & missing(idhhmother) & !missing(idhhfather)

	order idfamily, before(idhh)

	sort idfamily idhh idperson
	
	*/

	*Keep required variables*
	keep idhh idbenefitunit idperson idpartner idfather idmother dct drgn1 dwt dnc02 dnc dgn dag dhe dhm scghq2_dv scghq2_dv_miss_flag dhm_ghq dhesp dcpst ded deh_c3 der dehsp_c3 dehm_c3 dehf_c3 dcpen dcpyy dcpex dcpagdf dlltsd dhhtp_c4 les_c3 les_c4 lessp_c3 lessp_c4 lesdf_c4 ydses_c5 ypnbihs_dv yptciihs_dv ypncp ypnoab yplgrs_dv ynbcpdf_dv swv sedex ssscp sprfm sedag stm dagsp lhw pno ppno der adultChildFlag dhh_owned adult

	foreach var in idhh idbenefitunit idperson idpartner idfather idmother dct drgn1 dwt dnc02 dnc dgn dag dhe dhm scghq2_dv dhm_ghq dhesp dcpst ded deh_c3 der dehsp_c3 dehm_c3 dehf_c3 dcpen dcpyy dcpex dlltsd dhhtp_c4 les_c3 les_c4 lessp_c3 lessp_c4 lesdf_c4 ydses_c5 ypnbihs_dv yptciihs_dv ypncp ypnoab yplgrs_dv swv sedex ssscp sprfm sedag stm dagsp lhw pno ppno der dhh_owned adult{
		qui recode `var' (-9/-1=-9) (.=-9) 
	}

	********************************************************************************
	*TEMPORARY MODIFICATIONS - ADAPT IF THERE IS A BETTER SOURCE OF DATA?
	********************************************************************************
	replace deh_c3 = 2 if deh_c3 == -9 & dag >= $ageMaturity & ded == 0 
	replace dehsp_c3 = 2 if dehsp_c3 == -9 & idpartner != -9 
	replace dehf_c3 = 2 if dehf_c3 == -9 //A lot of missing values
	replace dehm_c3 = 2 if dehm_c3 == -9 //A lot of missing values

	/********************************Drop households with missing values***********/
	gen dropObs = . //Generate variable indicating whether household should be dropped

	* in 2017 193 observations missing idbenefitunit identifier - mostly children living with grandparents
	* drop these from sample, as beyond the modelling scope
	replace dropObs = 1 if (idbenefitunit == -9)

	bys idbenefitunit: egen adult_count = sum(adult)
	*tab adult_count
	* in 2017, 9.76% have zero adults - these are children who were interviewed in a year other than that of their parents (1735 obs)
	replace dropObs = 1 if (adult_count==0)
	drop adult
	
	*Remove household if missing values present: (not using previous wave's values as migration possible) 10 cases
	replace dropObs = 1 if drgn1 == -9

	*Missing age (0 cases):
	replace dropObs = 1 if dag == -9

	*Missing age of partner (but has a partner, 610 cases):
	recode idpartner (0 = -9)
	replace dropObs = 1 if dagsp == -9 & idpartner != -9

	*Health status - remove household if missing for adults but ignore children (882 cases):
	replace dropObs = 1 if dhe == -9 & dag > $ageMaturity

	*Mental health status (471 cases):
	replace dropObs = 1 if dhm == -9 & dag > $ageMaturity
	replace dropObs = 1 if dhm_ghq == -9 & dag > $ageMaturity

	*Health status of spouse - remove household if missing but individual has a spouse (467 cases)
	replace dropObs = 1 if dhesp == -9 & idpartner != -9

	*Education - remove household if missing education level for adults who are not in education (0 cases):
	replace dropObs = 1 if deh_c3 == -9 & dag >= $ageMaturity & ded == 0

	*Education of spouse - remove household if missing but individual has a spouse (0 cases)
	replace dropObs = 1 if dehsp_c3 == -9 & idpartner != -9

	*Parental education (0 cases)
	replace dropObs = 1 if dehm_c3 == -9 | dehf_c3 == -9

	*Partnership status (10 cases):
	replace dropObs = 1 if dcpst == -9

	*Activity status (3 cases):
	replace dropObs = 1 if les_c3 == -9 & dag >= $ageMaturity

	*Activity status with retirement as a separate category (0 cases)
	replace dropObs = 1 if les_c4 == -9 & dag >= $ageMaturity

	*Partner's activity status (2 cases):
	replace dropObs = 1 if lessp_c3 == -9 & idpartner != -9

	*Own and spousal activity status (0 cases):
	replace dropObs = 1 if lesdf_c4 == -9 & idpartner != -9

	*Household composition (91 cases):
	replace dropObs = 1 if dhhtp_c4 == -9

	*Income (121 cases):
	replace dropObs = 1 if ypnbihs_dv == -9 & dag >= $ageMaturity

	replace dropObs = 1 if yplgrs_dv == -9 & dag >= $ageMaturity

	replace dropObs = 1 if ydses_c5 == -9

	replace dropObs = 1 if ypncp == -9 & dag >= $ageMaturity


	*If any person in the household has missing values, drop the whole household (32.97%):
	bys idhh: egen dropHH = max(dropObs)
	*tab dropHH, mis
	drop if dropHH == 1 
	*drop if dropObs == 1
	drop dropObs dropHH

	*Drop if weight = 0: 
	*drop if dwt == 0 //Commented out as done later in master_conversion.do file

	recode idmother idfather (. = -9)

	drop if dag < $ageMaturity & idfather == -9 & idmother == -9

	*Cannot have missing values in continuous variables - recode to 0 for now: (But note this missings are valid in general - e.g. people without a partner don't have years in partnership etc.)
	recode dcpyy dcpagdf ynbcpdf_dv dnc02 dnc ypnbihs_dv yptciihs_dv ypncp ypnoab yplgrs_dv stm swv dhe dhesp dhm scghq2_dv dhm_ghq (-9 . = 0)


	/* 
	Missing values of dhe and dhm need to be imputed using a regression model for children. 
	The coefficients can be estimated on the basis of young people (several ages so possible to estimate coefficient on age) in the estimation sample.
	*/


	********************************************************************************
	*Parts of code from master_conversion.do file below
	********************************************************************************

	order idhh idbenefitunit idperson idpartner idmother idfather
	export delimited using "population_UK_initial.csv", nolabel replace

	*If missing idpartner set dcpyy to 0:
	replace dcpyy = 0 if idpartner == -9
	replace les_c4 = 2 if les_c4 == -9
	replace les_c3 = 2 if les_c3 == -9

	* run R code
	shell "$RshellPath" --vanilla <"$rScript"
	/*
	rsource, terminator(END_OF_R)
	library(data.table);
	setwd("C:\\MyFiles\\00 CURRENT\\03 PROJECTS\\LABSIM\\02 PARAMETERISE\\STARTING DATA\\data\\");
	data <- fread("population_UK_initial.csv");
	data[, check:=sapply(idpartner, function(i) any(i %in% idperson)), by = idhh];
	data[, checkm:=sapply(idmother, function(i) any(i %in% idperson)), by = idhh];
	data[, checkf:=sapply(idfather, function(i) any(i %in% idperson)), by = idhh];
	data;
	fwrite(data, "population_UK_initial_check.csv");
	END_OF_R
	*/

	* retrieve results from R
	import delimited "population_UK_initial_check.csv", clear
	gen idpartnertemp = idpartner if idpartner != -9
	bys idbenefitunit: egen pnr = count(idpartnertemp) 
	drop if pnr > 2
	drop idpartnertemp pnr

	*Remove IDs where individuals not in the HH:
	replace idpartner = -9 if idpartner != -9 & check == "FALSE"
	drop check
	replace idmother = -9 if dag < $ageMaturity & checkm == "FALSE" 
	drop checkm 
	replace idfather = -9 if dag < $ageMaturity & checkf == "FALSE"
	drop checkf

	gsort idbenefitunit -idpartner -dag
	gen chk1 = (idpartner>0)
	by idbenefitunit: egen chk2 = sum(chk1)
	by idbenefitunit: replace idpartner = idperson[1] if (idpartner==-9 & chk2==1 & _n==2)
	drop chk1 chk2
	
	*Convert hours to integer:
	replace lhw = ceil(lhw)

	*Try to create frequency weights from HH weights?
	if $year == 2010 gen uk_pop = 26240000
	if $year == 2011 gen uk_pop = 26409000
	if $year == 2012 gen uk_pop = 26620000
	if $year == 2013 gen uk_pop = 26663000
	if $year == 2014 gen uk_pop = 26734000
	if $year == 2015 gen uk_pop = 27046000
	if $year == 2016 gen uk_pop = 27109000
	if $year == 2017 gen uk_pop = 27226000
	gen surv_pop = _N
	gen multiplier = uk_pop / surv_pop 
	gen dwtfq = round(dwt * multiplier)

	*Drop those with 0 weight? 
	drop if dwtfq == 0

	rename dwt dwt_sampling
	rename dwtfq dwt
	
	*potential hourly earnings
	gen potential_earnings_hourly = 0
	gen l1_potential_earnings_hourly = 0
	
	replace potential_earnings_hourly = sinh(yplgrs_dv)/(lhw*4.33) if les_c4 == 1
	replace l1_potential_earnings_hourly = potential_earnings_hourly
	
	replace potential_earnings_hourly = 0 if missing(potential_earnings_hourly)
	replace l1_potential_earnings_hourly = 0 if missing(l1_potential_earnings_hourly)

	* initialise wealth to missing
	gen liquid_wealth = -9
	
	* sort by person id to enhance replication
	sort idperson

	* save data
	save population_initial_UK_$year, replace
	
	export delimited using "$workingDir/input data/population_initial_UK_$year.csv", nolabel replace
}


/**************************************************************************************
*	clean-up
**************************************************************************************/
rm "add_vars_ukhls_hhresp.dta"
rm "add_vars_ukhls_indresp.dta"
rm "add_vars_ukhls.dta"
rm "parametricUnionDataset.dta"
rm "uk_temp_lesc4.dta"
rm "temp_mother_dag.dta"
rm "temp_father_dag.dta"
rm "temp_sex.dta"
rm "temp2_here.dta"
rm "uk_temp_dehf.dta"
rm "uk_temp_dehm.dta"
rm "uk_temp_dehsp.dta"
