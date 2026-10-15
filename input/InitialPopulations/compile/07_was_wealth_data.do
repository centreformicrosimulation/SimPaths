***************************************************************************************
* PROJECT:              SimPaths UK: construct initial populations for SimPaths using UKHLS data 
* DO-FILE NAME:         07_was_wealth_data.do
* DESCRIPTION:          COMPILE WAS DATA FOR IMPUTING WEALTH INTO UKHLS DATA 
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave o]
*                       WAS EUL version - UKDA-7215-stata [to wave 7]
* AUTHORS: 				Liang Shi (LS), Justin van de Ven (JV), Daria Popova (DP)
* LAST UPDATE:          14/10/2026 (JV)
* NOTE:					Called from 00_master.do - see master file for further details
*
*	File currently compiles data to merge from 2016
*	This could be extended to at least 2011
*   Builds synthetic longitudinal ID: was_pid
*
*	Approach involves identifying total net wealth (wealth), 
*	total private pension wealth (personal and occupational), gross 
*	value of main home (dvhvalue) and value of assocaited mortgage 
*	debt (main_mort) at the benefit unit level. Total net wealth includes
*	pension and housing wealth in addition to "other" wealth variously 
*	defined. File 08_wealth_to_ukls.do uses matching methods for reference 
*	people from each benefit unit to merge in these data to the UKHLS data.
*
/**************************************************************/

/*
	WAS WAVES - 2 year periods
		WAVE 1: 2006 (7826), 2007 (15143), 2008 (7618)
		WAVE 2: 2008 (5253), 2009  (9933), 2010 (4979)
		WAVE 3: 2010 (5646), 2011 (10768), 2012 (5032)
		WAVE 4: 2012 (5161), 2013 (10235), 2014 (4844)
		WAVE 5: 2014 (7385), 2015  (9480), 2016 (2173)
		WAVE 6: 2016 (6884), 2017  (8970), 2018 (2175)
		WAVE 7: 2018 (6855), 2019  (8756), 2020 (1923)
		WAVE 8: 2020 (7634), 2021 (6362),  2022 (1132)

		LS: In the parentheses are numbers of households involved in WAS waves
        To obtain such statistics (for example wave 5), use:
		  use "$dir_was_data/was_round_5_hhold_eul_feb_20.dta", clear
          rename *, lower
          tab yearr5
*/

********************************************************************************
cap log close 
log using "${dir_log}/07_was_wealth_data.log", replace
********************************************************************************

/**************************************************************/
*	
*	Preliminaries
*
/**************************************************************/
cd "${dir_data}"
disp "analysing WAS wealth data"


/**************************************************************/
*	
*	Extract WAS data from household level datasets
*
/**************************************************************/
local ww = 5
foreach file in "${dir_was_data}/was_round_5_hhold_eul_feb_20.dta" ///
				"${dir_was_data}/was_round_6_hhold_eul_april_2022.dta" ///
				"${dir_was_data}/was_round_7_hhold_eul_march_2022.dta" ///
				"${dir_was_data}/was_round_8_hhold_eul_may_2025_230525.dta" {
	
	use "`file'", clear
	rename *, l
	if (`ww' < 7) {
		keep caser`ww' yearr`ww' dvhvaluer`ww' totmortr`ww' allendwr`ww' accomw`ww' hsetypew`ww' ten1r`ww' hrpnssec3w`ww'
		foreach vv in accom hsetype hrpnssec3 {
			rename `vv'w`ww' `vv'
		}
	}
	else {
		keep caser`ww' yearr`ww' dvhvaluer`ww' totmortr`ww' allendwr`ww' accomr`ww' hsetyper`ww' ten1r`ww' hrpnssec3r`ww'
		foreach vv in accom hsetype hrpnssec3 {
			rename `vv'r`ww' `vv'
		}
	}
	foreach vv in case year dvhvalue totmort allendw ten1 {

		rename `vv'r`ww' `vv'
	}

	/* value of main home */
	desc dvhvalue
	gen main_mort = totmort + allendw
	label var main_mort "total mortgage debt owed on main home"

	/* other variables for wage regression	*/
	gen house = accom==1
	gen flat = accom==2
	gen room = accom==3
	gen house_d = house * (hsetype==1)
	label var house_d "detached house"
	gen house_s = house * (hsetype==2)
	label var house_s "semi-detached house"
	gen house_t = house * (hsetype==3)
	label var house_d "terrace house"
	gen accm_o = house * (ten1==1)
	label var accm_o "own accomodation"
	gen accm_m = house * ((ten1==2)+(ten1==3))
	label var accm_m "mortgaged accommodation"
	gen accm_r = house * (ten1==4)
	label var accm_r "rented accommodation"
	gen accm_f = house * ((ten1==5)+(ten1==6))
	label var accm_f "rent-free accommodation"
	gen hrp_manager = (hrpnssec3==1)
	label var hrp_manager "household head Socio-economic Class 1 of 3 (managerial/professional)"
	gen hrp_intermediate = (hrpnssec3==2)
	label var hrp_intermediate "household head Socio-economic Class 2 of 3 (intermediate occupations)"

	keep case year dvhvalue main_mort flat room house_d house_s house_t accm_o accm_m accm_r accm_f hrp_manager hrp_intermediate
	
	sort case
	save "${dir_data}/was_temp_hh`ww'.dta", replace
	local ww = `ww' + 1
}


/**************************************************************/
*	
*	Extract data from person level dataset
*
/**************************************************************/
local ww = 5
local ww0 = 5
foreach file in "${dir_was_data}/was_round_5_person_eul_oct_2020.dta" ///
				"${dir_was_data}/was_round_6_person_eul_april_2022.dta" ///
				"${dir_was_data}/was_round_7_person_eul_june_2022.dta" ///
				"${dir_was_data}/was_round_8_person_eul_may_2025_230525.dta" {

	disp "compiling WAS wealth data for wave `ww'"
	qui {

		use "`file'", clear
		rename *, l
		
		if (`ww' < 6) {
			keep caser`ww' personw`ww' partnow`ww' dvage17r`ww' hasdepw`ww' dvgiser`ww' dvgippenr`ww' dvgiinvr`ww' dvoigrrannualr`ww'_i ///
				dvoiggtannualr`ww'_i dvoigegannualr`ww'_i dvoigfrannualr`ww'_i dvoigmaannualr`ww'_i dvoigroannualr`ww'_i dvoigopannualr`ww'_i ///
				dvmrdfr`ww' isdepw`ww' p_flag4w`ww' r`ww'xshhwgt btype1w`ww' qhealth1w`ww' lsillw`ww' dvisavalr`ww'_sum edlevelr`ww' ///
				enrollw`ww' coursew`ww' dvgiempr`ww' dvgiser`ww' statr`ww' ftptwkw`ww' dvecactr`ww' pprgam1w`ww' pprgam2w`ww' pprgam3w`ww' ///
				pprgpe1w`ww' pprgpe2w`ww' pprgpe3w`ww' pplsta1w`ww' pplsta2w`ww' pplsta3w`ww' pplstp1w`ww' pplstp2w`ww' pplstp3w`ww' ///
				pocnfr1w`ww' pocnfr2w`ww' pemoffw`ww' pemelgw`ww' pemmemw`ww' poctyp1w`ww' poctyp2w`ww' dvbenefitannualr`ww'_i dvgippenr`ww' ///
				dvgiinvr`ww' bval* dvhsevalr`ww' dvhsedebtr`ww' dvbldvalr`ww' dvblddebtr`ww' dvlukvalr`ww' dvlukdebtr`ww' dvlosvalr`ww' ///
				dvlosdebtr`ww' dvoprvalr`ww' dvoprdebtr`ww' dvffassetsr`ww'_sum dvfinfvalr`ww' dvvaldcosr`ww' dvpavcuvr`ww' dvpfcurvalr`ww' ///
				dvppvalr`ww' dvpfddvr`ww' dvspenr`ww' dvpinpvalr`ww' pincinpr`ww' totccr`ww'_sum dvffassetsr`ww'_sum dvfinfvalr`ww' ///
				tothpr`ww'_sum tot_losr`ww' tot_los_exc_slcr`ww' totmor`ww'_sum dvvaldbtr`ww' dvdbrwealthvalr`ww' gorr`ww' sexr`ww' ///
				dltype*w`ww' dlint*w`ww' dnewltype*w`ww' dnewlowe*w`ww' lnos*r`ww' flns*r`ww'
			rename totccr`ww'_sum totcscr`ww'_sum
			rename dvffassetsr`ww'_sum dvffassetsr`ww'
			recode partnow`ww' (missing=17)
			recode partnow`ww' (-7=17)
		}
		else if (`ww' == 6) {
			keep caser`ww' personw`ww' caser5 personw5 partnow`ww' dvage17r`ww' hasdepw`ww' dvgiser`ww' dvgippenr`ww' dvgiinvr`ww' ///
				dvoigrrannualr`ww'_i dvoiggtannualr`ww'_i dvoigegannualr`ww'_i dvoigfrannualr`ww'_i dvoigmaannualr`ww'_i ///
				dvoigroannualr`ww'_i dvoigopannualr`ww'_i dvmrdfr`ww' isdepw`ww' p_flag4w`ww' r`ww'xshhwgt btype1w`ww' qhealth1w`ww' ///
				lsillw`ww' dvisavalr`ww'_sum edlevelr`ww' enrollw`ww' coursew`ww' dvgiempr`ww' dvgiser`ww' statr`ww' ftptwkw`ww' ///
				dvecactr`ww' pprgam1w`ww' pprgam2w`ww' pprgam3w`ww' pprgpe1w`ww' pprgpe2w`ww' pprgpe3w`ww' pplsta1w`ww' pplsta2w`ww' ///
				pplsta3w`ww' pplstp1w`ww' pplstp2w`ww' pplstp3w`ww' pcontppw`ww' dvemeeamt1w`ww' dvemeeamt2w`ww' dvcontocc_emee_dc1r`ww' ///
				dvcontocc_emee_dc2r`ww' pocnfr1w`ww' pocnfr2w`ww' pemoffw`ww' pemelgw`ww' pemmemw`ww' poctyp1w`ww' poctyp2w`ww' ///
				dvbenefitannualr`ww'_i dvgippenr`ww' dvgiinvr`ww' bval* dvhsevalr`ww' dvhsedebtr`ww' dvbldvalr`ww' dvblddebtr`ww' ///
				dvlukvalr`ww' dvlukdebtr`ww' dvlosvalr`ww' dvlosdebtr`ww' dvoprvalr`ww' dvoprdebtr`ww' dvffassetsr`ww'_sum dvfinfvalr`ww' ///
				dvvaldcosr`ww' dvpavcuvr`ww' dvpfcurvalr`ww' dvppvalr`ww' dvpfddvr`ww' dvspenr`ww' dvpinpvalr`ww' pincinpr`ww' ///
				totcscr`ww'_sum totcsc_transr`ww'_sum totcsc_persr`ww'_sum dvffassetsr`ww'_sum dvfinfvalr`ww' tothpr`ww'_sum tot_losr`ww' ///
				tot_los_exc_slcr`ww' totmor`ww'_sum dvvaldbtr`ww' dvdbrwealthvalr`ww' gorr`ww' sexr`ww' dltype*w`ww' dlint*w`ww' ///
				dnewltype*w`ww' dnewlowe*w`ww' lnos*r`ww' flns*r`ww'
			rename dvffassetsr`ww'_sum dvffassetsr`ww'
			recode partnow`ww' (missing=17)
		}
		else if (`ww' == 7) {
			keep caser`ww' personr`ww' caser6 personw6 caser5 personw5 partnor`ww' dvage17r`ww' hasdepr`ww' dvgiser`ww' dvgippenr`ww' ///
				dvgiinvr`ww' dvoigrrannualr`ww'_i dvoiggtannualr`ww'_i dvoigegannualr`ww'_i dvoigfrannualr`ww'_i dvoigmaannualr`ww'_i ///
				dvoigroannualr`ww'_i dvoigopannualr`ww'_i dvmrdfr`ww' isdepr`ww' p_flag4r`ww' r`ww'xshhwgt btype1r`ww' qhealth1r`ww' ///
				lsillr`ww' dvisavalr`ww'_sum edlevelr`ww' enrollr`ww' courser`ww' dvgiempr`ww' dvgiser`ww' statr`ww' ftptwkr`ww' dvecactr`ww' ///
				pcontppr`ww' pprgam1r`ww' pprgam2r`ww' pprgam3r`ww' pprgpe1r`ww' pprgpe2r`ww' pprgpe3r`ww' pplsta1r`ww' pplsta2r`ww' ///
				pplsta3r`ww' pplstp1r`ww' pplstp2r`ww' pplstp3r`ww' dvemeeamt1r`ww' dvemeeamt2r`ww' dvcontocc_emee_dc1r`ww' ///
				dvcontocc_emee_dc2r`ww' pocnfr1r`ww'_i pocnfr2r`ww'_i pemoffr`ww' pemelgr`ww' pemmemr`ww' poctyp1r`ww' poctyp2r`ww' ///
				dvbenefitannualr`ww'_i dvgippenr`ww' dvgiinvr`ww' bval* dvhsevalr`ww' dvhsedebtr`ww' dvbldvalr`ww' dvblddebtr`ww' ///
				dvlukvalr`ww' dvlukdebtr`ww' dvlosvalr`ww' dvlosdebtr`ww' dvoprvalr`ww' dvoprdebtr`ww' dvffassetsr`ww'_sum dvfinfvalr`ww' ///
				dvvaldcosr`ww' dvpavcuvr`ww' dvppvalr`ww' dvspenr`ww' dvpinpvalr`ww' pincinpr`ww' totcscr`ww'_sum totcsc_transr`ww'_sum ///
				totcsc_persr`ww'_sum dvfinfvalr`ww' tothpr`ww'_sum tot_losr`ww' tot_los_exc_slcr`ww' totmor`ww'_sum dvvaldbtr`ww' ///
				dvdbincallr`ww' gorr`ww' sexr`ww' dvretdc_noaccessr`ww' dvretdc_accessr`ww' dltype*r`ww' dlint*r`ww' dnewltype*r`ww' ///
				dnewlowe*r`ww' lnos*r`ww' flns*r`ww'
			rename dvffassetsr`ww'_sum dvffassetsr`ww'
			rename dvdbincallr`ww' dvdbrwealthvalr`ww'
			gen dvpfcurvalr`ww' = dvretdc_accessr`ww' + dvretdc_noaccessr`ww'
			gen dvpfddvr`ww' = 0
			recode partnor`ww' (missing=17)
		}
		else { // for round/wave 8 and above
			keep caser`ww' personr`ww' caser7 personr7 caser6 personw6 caser5 personw5 partnor`ww' dvage17r`ww' hasdepr`ww' dvgiser`ww' ///
				dvgippenr`ww' dvgiinvr`ww' dvoigrrannualr`ww'_i dvoiggtannualr`ww'_i dvoigegannualr`ww'_i dvoigfrannualr`ww'_i ///
				dvoigmaannualr`ww'_i dvoigroannualr`ww'_i dvoigopannualr`ww'_i dvmrdfr`ww' isdepr`ww' p_flag4r`ww' r`ww'xshhwgt ///
				btype1r`ww' dvisavalr`ww'_sum edlevelr`ww' enrollr`ww' courser`ww' dvgiempr`ww' dvgiser`ww' statr`ww' ftptwkr`ww' dvecactr`ww' ///
				pcontppr`ww' pprgam1r`ww' pprgam2r`ww' pprgam3r`ww' pprgpe1r`ww' pprgpe2r`ww' pprgpe3r`ww' pplsta1r`ww' pplsta2r`ww' ///
				pplsta3r`ww' pplstp1r`ww' pplstp2r`ww' pplstp3r`ww' dvemeeamt1r`ww' dvemeeamt2r`ww' dvcontocc_emee_dc1r`ww' ///
				dvcontocc_emee_dc2r`ww' pocnfr1r`ww'_i pocnfr2r`ww'_i pemoffr`ww' pemelgr`ww' pemmemr`ww' poctyp1r`ww' poctyp2r`ww' ///
				dvbenefitannualr`ww'_i dvgippenr`ww' dvgiinvr`ww' bval* dvhsevalr`ww' dvhsedebtr`ww' dvbldvalr`ww' dvblddebtr`ww' ///
				dvlukvalr`ww' dvlukdebtr`ww' dvlosvalr`ww' dvlosdebtr`ww' dvoprvalr`ww' dvoprdebtr`ww' dvffassetsr`ww'_sum dvfinfvalr`ww'_sum ///
				dvvaldcosr`ww' dvpavcuvr`ww' dvppvalr`ww' dvspen_oldr`ww' dvpinpval_oldr`ww' pincinp_oldr`ww' totcscr`ww'_sum ///
				totcsc_transr`ww'_sum totcsc_persr`ww'_sum tothpr`ww'_sum tot_losr`ww' tot_los_exc_slcr`ww' totmor`ww'_sum dvvaldbt_oldr`ww' ///
				dvdbincallr`ww' gorr`ww' sexr`ww' dvretdc_noaccessr`ww' dvretdc_accessr`ww' dltype*r`ww' dlint*r`ww' dnewltype*r`ww' ///
				dnewlowe*r`ww' lnos*r`ww' flns*r`ww'
			rename dvffassetsr`ww'_sum dvffassetsr`ww'   // similar rename as round 7
			rename dvfinfvalr`ww'_sum dvfinfvalr`ww'     
			rename dvvaldbt_oldr`ww' dvvaldbtr`ww'       // Total value of defined benefit occupational scheme - Previous pension methodology
			rename dvspen_oldr`ww' dvspenr`ww'           // Value of pensions expected from former spouse/partner - Previous pension methodology
			rename dvpinpval_oldr`ww' dvpinpvalr`ww'
			rename pincinp_oldr`ww' pincinpr`ww'
			rename dvdbincallr`ww' dvdbrwealthvalr`ww'   // similar remane as round 7 
			gen dvpfcurvalr`ww' = dvretdc_accessr`ww' + dvretdc_noaccessr`ww'
			gen dvpfddvr`ww' = 0
			// Round 8 person file has no direct counterpart to qhealth1r`ww' / lsillr`ww'.
			gen qhealth1r`ww' = .        // removed 
			gen lsillr`ww' = .           // 
			recode partnor`ww' (missing=17)
		}
		if (`ww' < 7) {
			rename *w`ww'* **
		}
		rename *r`ww'* **

		// Standardise unsecured-debt stock variables across rounds.
		// Round 5 has total card balances but not persistent/transient splits.
		foreach uv in totcsc_trans_sum totcsc_pers_sum tot_los_exc_slc tothp_sum totmo_sum {
			capture confirm variable `uv'
			if _rc gen `uv' = .
		}
		foreach stem in dltype dlint dnewltype dnewlowe lnos flns {
			forvalues ii = 1/9 {
				capture confirm variable `stem'`ii'
				if _rc gen `stem'`ii' = .
			}
		}

		// Ensure linkage variables exist in all rounds (missing where unavailable)
		foreach lv in caser5 caser6 caser7 personw5 personw6 personr7 {
			capture confirm variable `lv'
			if _rc gen `lv' = .
		}

		keep if (!missing(case) & !missing(person))
		gen was_round = `ww'
		label var was_round "WAS round number"
			
		// merge with household level data
		sort case person
		merge m:1 case using "${dir_data}/was_temp_hh`ww'.dta", gen(merge_hh) sorted keep(master match)

        * Expect every person-record case to have a matching household record
        count if merge_hh == 1
        assert r(N) == 0
        tab merge_hh

        drop merge_hh
		
		// ypnbihs
		recode dvgiemp dvgise dvgippen dvgiinv dvoigrrannual_i dvoiggtannual_i dvoigegannual_i dvoigfrannual_i dvoigmaannual_i dvoigroannual_i dvoigopannual_i (missing=0)
		gen inci = dvgiemp + dvgise + dvgippen + dvgiinv + dvoigrrannual_i + dvoiggtannual_i + dvoigegannual_i + dvoigfrannual_i + dvoigmaannual_i + dvoigroannual_i + dvoigopannual_i
		gen pensioner = (dvgippen > 0.01)
		
		// fix some partner person numbers
		duplicates tag case partno, gen(dup)	// Rows should be unique in case partno, assuming monogamy
		replace dup=0 if partno==17				// partno=17 if no partner in household
		tab dup
		sort case person
		gen mari = inlist(dvmrdf,1,2,8)		/* Married/cohabiting/couple */
		label var mari "Married/cohabiting/in couple"
		/* Person 2's spouse is person 1 if the two are married, in the same HH, and person 1's spouse is person 2 */
		replace partno=1 if partno==2 & person==2 & person[_n-1]==1 & partno[_n-1]==2 & mari==1 & case==case[_n-1]
		drop dup
		duplicates tag case partno, gen(dup)
		replace dup=0 if partno==17
		tab dup
		drop if dup>0
		drop dup

		// define benefit units
		gen isdep2 = isdep
		gen hrp = ((p_flag4==1) + (p_flag4==3))	/* HH ref person */
		gsort case -hrp person				    /* Place hh ref person at top of household */
		by case: replace hrp=1 if hrp[1]==0		/* to account for missing reference adults - not really important */
		by case: gen sps = partno==person[1]	/* spouse/partner of hrp */
		by case: egen pensioners = sum(pensioner)
		gen chld = isdep2==1					/* Dependent child */
		gen adlt = 1-chld						/* Adult */
		gsort case -hrp -sps person
		gen person_id = _n						/* person identifier for transferring to model */
		gen bu = person_id						/* create benefit unit variable */
		by case: replace bu = bu[1] if (sps==1) /* add spouses to bu of reference adults */
		by case: replace bu=bu[1] if chld==1 & hasdep[1]==1 /* add children to bu of reference adults */
		gsort case person                       /* allow for relationships beyond reference adults */
		by case: replace bu=bu[partno] if (sps==0) & (hrp==0) & (partno<person)
		gsort case person                       /* allow for children of non-reference adults */
		by case: replace bu=bu[_n-1] if (chld==1) & (bu==person_id)
		label var bu "benefit unit number"
		replace bu = person_id if missing(bu)
		gen bu_rp = (person_id==bu)
		label var bu_rp "benefit unit reference person"
		bysort was_round case bu: egen n_rp = total(bu_rp==1)
		bysort was_round case bu (person_id): replace bu_rp = 1 if n_rp==0 & _n==1
		replace bu_rp = 0 if missing(bu_rp)
		drop n_rp
		bysort was_round case bu: egen n_rp = total(bu_rp==1)
		assert n_rp==1
		drop n_rp
		gsort case bu
		by case bu: egen hrp_bu = sum(hrp)

		// allocate house to benefit unit of reference person
		replace dvhvalue = 0 if (hrp_bu==0)
		replace main_mort = 0 if (hrp_bu==0)
		
		// number of adults
		egen na=sum(adlt), by (case bu)
		label var na "number of adults"
		egen na_hh=sum(adlt), by (case)

		// number of children
		egen nk=sum(chld), by (case bu)
		label var nk "number of children (all)"
		gen nk04i = (dvage17<2)
		egen nk04 = sum(nk04i), by (case bu)
		label var nk04 "number of children (aged 0-4)"
		drop nk04i

		// common name for weighting variable between waves
		gen xs_wgt = xshhwgt

		// employment status
		gen emp = (btype1>0)
		label var emp "employed"

		// health state
		gen healths = qhealth1
		label var healths "General health"
		gen dlltsd = (lsill==1)
		label var dlltsd "Long term sick or disabled"

		// income
		gsort case bu
		by case bu: egen inc = sum(inci)
		label var inc "private (original) income annual"

		// ISA and PEPs
		egen isa_fam = sum(dvisaval_sum), by (case bu)
		label var isa_fam "total value of ISAs"

		// whether have degree level qualification
		gen grad=(edlevel==1)

		// whether currently a university student
		gen student = (enroll==1)*((course==6) + (course==4))*(grad==0)

		// individual earnings income
		gen earnings = 0
		replace earnings = dvgiemp if ((dvgiemp<.) & (dvgiemp>0))
		replace earnings = earnings + dvgise if ((dvgise<.) & (dvgise>0))
		label var earnings "gross earnings"

		// private pension contributions: core (regular) and fallback (last contribution)
		foreach vv in pprgam1 pprgam2 pprgam3 pprgpe1 pprgpe2 pprgpe3 pplsta1 pplsta2 pplsta3 pplstp1 pplstp2 pplstp3 pcontpp dvcontocc_emee_dc1 dvcontocc_emee_dc2 pocnfr1 pocnfr2 dvemeeamt1 dvemeeamt2 {
			capture confirm variable `vv'
			if _rc gen `vv' = .
			capture replace `vv' = . if `vv' < 0
		}

		foreach jj in 1 2 3 {
			gen pp_core_ann_`jj' = .
			replace pp_core_ann_`jj' = pprgam`jj' * (52.0 / pprgpe`jj') if pprgam`jj'>=0 & inlist(pprgpe`jj',1,2,3,4,13,26,52)
			replace pp_core_ann_`jj' = pprgam`jj' * 12 if pprgam`jj'>=0 & pprgpe`jj'==5
			replace pp_core_ann_`jj' = pprgam`jj' * pprgpe`jj' if pprgam`jj'>=0 & inlist(pprgpe`jj',8,9,10)

			gen pp_fallback_ann_`jj' = .
			replace pp_fallback_ann_`jj' = pplsta`jj' * (52.0 / pplstp`jj') if pplsta`jj'>=0 & inlist(pplstp`jj',1,2,3,4,13,26,52)
			replace pp_fallback_ann_`jj' = pplsta`jj' * 12 if pplsta`jj'>=0 & pplstp`jj'==5
			replace pp_fallback_ann_`jj' = pplsta`jj' * pplstp`jj' if pplsta`jj'>=0 & inlist(pplstp`jj',8,9,10)
		}

		egen pp_core_annual = rowtotal(pp_core_ann_1 pp_core_ann_2 pp_core_ann_3)
		egen pp_fallback_annual = rowtotal(pp_fallback_ann_1 pp_fallback_ann_2 pp_fallback_ann_3)
		gen pp_contrib_annual = pp_core_annual
		replace pp_contrib_annual = pp_fallback_annual if (missing(pp_contrib_annual) | pp_contrib_annual<=0) & pp_fallback_annual>0
		replace pp_contrib_annual = 0 if missing(pp_contrib_annual)
		gen chk = pp_contrib_annual * (earnings < 0.01)
		gsort was_round bu
		by was_round bu: egen chk2 = sum(chk)
		by was_round bu: egen earn_max = max(earnings)
		gen pp_reallocate = 0
		replace pp_reallocate = chk2 if (earnings == earn_max & earnings > 0.01)
		gen pp_contrib_annual2 = pp_contrib_annual + pp_reallocate
		replace pp_contrib_annual2 = 0 if (chk==1)
		gen pp_contrate = 0
		replace pp_contrate = pp_contrib_annual2 / (earnings) * 100 if (pp_contrib_annual2>=0 & pp_contrib_annual2<. & earnings>0 & earnings<.)
		gen pp_membi = (pp_contrate>0.01) * (pp_contrate<.) * (adlt)
		egen pp_membu = sum(pp_membi), by (case bu)

		//gen ocdc_contrate_emee = dvcontocc_emee_dc1 * (dvcontocc_emee_dc1<.) + dvcontocc_emee_dc2 * (dvcontocc_emee_dc2 <.)
		//gen ocdc_contrate_emer = dvcontocc_emer_dc1 * (dvcontocc_emer_dc1<.) + dvcontocc_emer_dc2 * (dvcontocc_emer_dc2 <.)
		

		gen byte pp_contrib_source_core = (pp_core_annual>0)
		gen byte pp_contrib_source_fallback = (pp_core_annual<=0 & pp_fallback_annual>0)
		label var pp_core_annual "Personal pension annual contribution (core regular amount-period)"
		label var pp_fallback_annual "Personal pension annual contribution (fallback last contribution amount-period)"
		label var pp_contrib_annual "Personal pension annual contribution (core with fallback)"
		label var pp_contrib_annual2 "Personal pension annual contribution (core with fallback and reallocation)"
		label var pp_contrib_source_core "1 if pp_contrib_annual sourced from core regular contribution"
		label var pp_contrib_source_fallback "1 if pp_contrib_annual sourced from fallback last contribution"
		label var pp_contrate "Personal pension contribution rate (% of gross employment income)"
// 		label var ocdc_contrate_emee "DC occupational employee pension contribution rate (% of gross employment income)"
// 		label var ocdc_contrate_emer "DC occupational employer pension contribution rate (% of gross employment income)"
		label var pp_membu "number of personal pension members in benefit unit"
		label var pensioners "number of recipients of private pension in benefit unit"

		gen occ_emp_contrib_rate = .
		replace occ_emp_contrib_rate = max(dvcontocc_emee_dc1,0) + max(dvcontocc_emee_dc2,0) if !missing(dvcontocc_emee_dc1) | !missing(dvcontocc_emee_dc2)
		replace occ_emp_contrib_rate = max(pocnfr1,0) + max(pocnfr2,0) if missing(occ_emp_contrib_rate) & (!missing(pocnfr1) | !missing(pocnfr2))
		gen occ_emp_contrib_annual = .
		replace occ_emp_contrib_annual = dvemeeamt1 + dvemeeamt2 if (dvemeeamt1>=0 | dvemeeamt2>=0)
		replace occ_emp_contrib_annual = earnings * occ_emp_contrib_rate / 100 if missing(occ_emp_contrib_annual) & earnings>=0 & occ_emp_contrib_rate>=0
		replace occ_emp_contrib_annual = 0 if missing(occ_emp_contrib_annual)
		label var occ_emp_contrib_rate "Occupational DC employee contribution rate (%)"
		label var occ_emp_contrib_annual "Occupational DC employee annual contribution"

		gen pp_contrib_rate_income = .
		replace pp_contrib_rate_income = pp_contrib_annual / inc if inc>0
		gen priv_total_contrib_rate_inc = .
		replace priv_total_contrib_rate_inc = (pp_contrib_annual + occ_emp_contrib_annual) / inc if inc>0
		label var pp_contrib_rate_income "Personal pension contribution rate relative to annual private income"
		label var priv_total_contrib_rate_inc "Personal + occupational employee contribution rate relative to annual private income"

		drop pp_core_ann_1 pp_core_ann_2 pp_core_ann_3 pp_fallback_ann_1 pp_fallback_ann_2 pp_fallback_ann_3

		// individual labour force status
		gen sempi=(stat==2)
		gen fti= (stat==1 & ftptwk==1)
		gen pti= (stat==1 & ftptwk==2)
		gen ue = (dvecact==3)
		replace fti= 0 if (ue==1 | earnings==0)
		replace pti= 0 if (ue==1 | earnings==0)
		replace sempi= 0 if (ue==1 | earnings==0)
		replace fti = 1 if ( (earnings>15000) & (fti+pti==0) & (ue==0) )
		replace pti = 1 if ( (earnings<=15000) & (fti+pti==0) & (ue==0) & (earnings>0) )
		gen nempi = 1 - sempi - fti - pti
		replace nempi = 0 if (nempi<0)

		label var fti "FT work"
		label var pti "PT work"
		label var ue "Unemployed"
		label var sempi "Self-emp"
		label var grad "Graduate"
		label var nempi "not-employed"

		// current pension arrangements
		*gen non_cp = (((pometh1==2)+(pometh2==2))>0)
		gen non_cp = 0
		label var non_cp "whether has non-contributory pension scheme"

		gen op_elig = (pemoff==1) * (pemelg==1)
		label var op_elig "whether individual eligible to belong to occupational pension"

		gen op_memb = (pemmem==1)
		label var op_memb "whether member of occupational pension"
		gen op_membui = op_memb * adlt
		egen op_membu = sum(op_membui), by (case bu)
		label var op_membu "number of occupational pension members in benefit unit"

		gen op_db = (((poctyp1==2) + (poctyp2==2))>0) * op_memb
		label var op_db "whether has current defined benefit scheme"

		gen op_dc = op_memb - op_db
		label var op_dc "whether has current defined contribution scheme"

		* should define here the pension type to which each individual is eligible
		* pension type is defined with respect to both private and employer pension contribution rates 
		* these data appear to be largely missing in the WAS
		* we therefore set the associated model variable to missing for all observations
		gen pcr = 0.0
		label var pcr "occupational pension type"

		// gross employment, investment and pension income
		gen wkg_grsi = dvgiemp + dvgise + dvbenefitannual_i + dvgippen + dvgiinv
		gen wkg_empi = dvgiemp + dvgise
		egen wkg_grs = sum(wkg_grsi), by (case bu)
		egen wkg_emp = sum(wkg_empi), by (case bu)
		egen wkg_pen = sum(dvgippen), by (case bu)
		egen wkg_ben = sum(dvbenefitannual_i), by (case bu)

		// value of equity in own business net of business loans
		gen bval_i = 0.0
		forvalue ii = 1/3 {

			gen bvalb_i =   50 * (bvalb`ii' == 1) + ///
						  5050 * (bvalb`ii' == 2) + ///
						 30000 * (bvalb`ii' == 3) + ///
						 75000 * (bvalb`ii' == 4) + ///
						175000 * (bvalb`ii' == 5) + ///
						375000 * (bvalb`ii' == 6) + ///
						750000 * (bvalb`ii' == 7) + ///
					   1500000 * (bvalb`ii' == 8) + ///
					   3500000 * (bvalb`ii' == 9)
			replace bval_i = bval_i + bval`ii' * (bval`ii'>0) + bvalb_i
			drop bvalb_i
		}
		egen bus_assets = sum(bval_i), by (case bu)
		drop bval_i
		label var bus_assets "net value of own-business assets"

		// property other than main home
		gen dvhval_h = dvhseval
		gen dvhdbt_h = dvhsedebt
		gen dvbval_h = dvbldval
		gen dvbdbt_h = dvblddebt
		gen dvlukv_h = dvlukval
		gen dvlukd_h = dvlukdebt
		gen dvlosv_h = dvlosval
		gen dvlosd_h = dvlosdebt
		gen dvopval_h = dvoprval
		gen dvopdbt_h = dvoprdebt

		gen othprop_h = dvhval_h + dvbval_h + dvlukv_h + dvlosv_h + dvopval_h
		gen othmort_h = dvhdbt_h + dvbdbt_h + dvlukd_h + dvlosd_h + dvopdbt_h
		gen nothprop = othprop_h - othmort_h
		egen oprop = sum(nothprop), by (case bu)
		label var oprop "net value of all property other than main home"

		// financial and non-financial assets
		desc dvffassets
		desc dvfinfval
		replace totcsc_sum = 0 if missing(totcsc_sum)
		replace tothp_sum = 0 if missing(tothp_sum)
		replace tot_los = 0 if missing(tot_los)
		replace totmo_sum = 0 if missing(totmo_sum)

		gen cc_debt_i = totcsc_sum
		gen cc_debt_trans_i = totcsc_trans_sum
		gen cc_debt_pers_i = totcsc_pers_sum
		gen hp_debt_i = tothp_sum
		gen loan_debt_i = tot_los
		gen loan_debt_exc_slc_i = tot_los_exc_slc
		gen mail_debt_i = totmo_sum
		gen loan_doorstep_i = 0
		gen loan_pawnbroker_i = 0
		gen loan_payday_i = 0
		gen informal_loan_debt_i = 0

		forvalues ii = 1/9 {
			replace loan_doorstep_i = loan_doorstep_i + lnos`ii' ///
				if dltype`ii' == 2 & lnos`ii' > 0 & (missing(dlint`ii') | dlint`ii' != 1)
			replace loan_pawnbroker_i = loan_pawnbroker_i + lnos`ii' ///
				if dltype`ii' == 3 & lnos`ii' > 0 & (missing(dlint`ii') | dlint`ii' != 1)
			replace loan_payday_i = loan_payday_i + lnos`ii' ///
				if dltype`ii' == 10 & lnos`ii' > 0 & (missing(dlint`ii') | dlint`ii' != 1)
			replace informal_loan_debt_i = informal_loan_debt_i + flns`ii' ///
				if dltype`ii' == 7 & flns`ii' > 0

			replace loan_doorstep_i = loan_doorstep_i + dnewlowe`ii' ///
				if dnewltype`ii' == 2 & dnewlowe`ii' > 0
			replace loan_pawnbroker_i = loan_pawnbroker_i + dnewlowe`ii' ///
				if dnewltype`ii' == 3 & dnewlowe`ii' > 0
			replace loan_payday_i = loan_payday_i + dnewlowe`ii' ///
				if dnewltype`ii' == 10 & dnewlowe`ii' > 0
			replace informal_loan_debt_i = informal_loan_debt_i + dnewlowe`ii' ///
				if dnewltype`ii' == 7 & dnewlowe`ii' > 0
		}
		gen highcost_loan_debt_i = loan_doorstep_i + loan_pawnbroker_i + loan_payday_i

		egen cc_debt = sum(cc_debt_i), by (case bu)
		egen cc_debt_trans = sum(cc_debt_trans_i), by (case bu)
		egen cc_debt_pers = sum(cc_debt_pers_i), by (case bu)
		egen hp_debt = sum(hp_debt_i), by (case bu)
		egen loan_debt = sum(loan_debt_i), by (case bu)
		egen loan_debt_exc_slc = sum(loan_debt_exc_slc_i), by (case bu)
		egen mail_debt = sum(mail_debt_i), by (case bu)
		egen loan_doorstep = sum(loan_doorstep_i), by (case bu)
		egen loan_pawnbroker = sum(loan_pawnbroker_i), by (case bu)
		egen loan_payday = sum(loan_payday_i), by (case bu)
		egen informal_loan_debt = sum(informal_loan_debt_i), by (case bu)
		egen highcost_loan_debt = sum(highcost_loan_debt_i), by (case bu)
		gen loan_debt_slc = loan_debt - loan_debt_exc_slc if !missing(loan_debt) & !missing(loan_debt_exc_slc)
		gen unsec_debt_total = cc_debt + hp_debt + loan_debt + mail_debt
		gen unsec_debt_core = cc_debt + loan_debt_exc_slc
		gen highcost_debt_proxy = cc_debt_pers + mail_debt + highcost_loan_debt
		gen lowcost_debt = hp_debt + loan_debt_exc_slc - highcost_loan_debt

		replace cc_debt_trans = . if was_round == 5
		replace cc_debt_pers = . if was_round == 5
		replace highcost_debt_proxy = . if was_round == 5

		label var cc_debt "Benefit-unit total credit/store/charge card debt"
		label var cc_debt_trans "Benefit-unit total transient card debt (waves 6-8)"
		label var cc_debt_pers "Benefit-unit total persistent card debt (waves 6-8)"
		label var hp_debt "Benefit-unit total hire purchase debt"
		label var loan_debt "Benefit-unit total formal/informal/SLC loan debt"
		label var loan_debt_exc_slc "Benefit-unit total formal/informal loan debt excluding SLC"
		label var loan_debt_slc "Benefit-unit student loan component of loan debt"
		label var loan_doorstep "Benefit-unit cash-home-collector loan debt"
		label var loan_pawnbroker "Benefit-unit pawnbroker/cash-converter loan debt"
		label var loan_payday "Benefit-unit payday-lender loan debt"
		label var informal_loan_debt "Benefit-unit informal friend/family/private loan debt"
		label var highcost_loan_debt "Benefit-unit explicit high-cost loan debt"
		label var mail_debt "Benefit-unit total mail order debt"
		label var unsec_debt_total "Benefit-unit total unsecured debt"
		label var unsec_debt_core "Benefit-unit unsecured debt: cards + non-SLC loans"
		label var highcost_debt_proxy "Benefit-unit proxy high-cost debt: persistent cards + mail order + explicit high-cost loans"
		label var lowcost_debt "Benefit-unit proxy low-cost debt: hire-purchase agreements and non-student/non-high-cost loans"

		gen assts = dvffassets + dvfinfval - cc_debt_i - hp_debt_i - loan_debt_i - mail_debt_i
		egen assets = sum(assts), by (case bu)
		label var assets "net value of financial and non-financial (non-property) assets"

		// pension assets
		gen db_op = dvvaldbt * (dvvaldbt>0) + dvdbrwealthval * (dvdbrwealthval>0)
		label var db_op "value of aggregate rights held in DB Occupational Pensions"
		gen dc_op = dvvaldcos + dvpavcuv + dvpfcurval
		label var dc_op "value of aggregate rights held in DC Occupational Pensions"
		gen	dc_pen = dvvaldcos + dvpavcuv + dvpfcurval+ dvppval + dvpfddv 
		label var dc_pen "value of aggregate rights held in Private Pensions"
		gen tot_p = db_op + dc_pen + dvspen*(dvspen>0) + dvpinpval*(dvpinpval>0)
		egen tot_pen = sum(tot_p), by (case bu)
		label var tot_pen "value of aggregate pension rights"
		gen op_tot = db_op + dc_op
		egen tot_open = sum(op_tot), by (case bu)
		label var tot_open "value of aggregate occupational pension rights"
		gen tot_pp = tot_pen - tot_open
		label var tot_pp "value of aggregate personal pension rights"
		gen pi_temp = pincinp * (pincinp>0.01) // If pincinp is greater than 0.01, pi_temp = pincinp; otherwise, pi_temp = 0.
		egen pinc_now = sum(pi_temp), by (case bu)

		// welfare benefits
		egen benefits = sum(dvbenefitannual_i), by (case bu)

		// net wealth (ww = net non-property wealth + net other property + main-home equity, excluding ISAs and business equity.)
		gen ww = assets + oprop - isa_fam - bus_assets
		replace ww = ww + dvhvalue if ( dvhvalue<.)
		replace ww = ww - main_mort if ( main_mort<.)
		label var ww "net wealth excluding ISAs and business equity"
		
		save "${dir_data}/chk.dta", replace

		// partner characteristics
		save "${dir_data}/was_temp.dta", replace
		keep case bu person_id person partno healths dlltsd grad emp
		drop if (partno==17)

		duplicates tag case partno, gen(dup)
		egen dup_hh=max(dup), by(case)
		tab dup_hh
		drop if dup_hh>0
		drop dup dup_hh

		foreach xx in healths dlltsd grad emp { /* Add partner prefix */
			rename `xx' p_`xx'
		}
		drop person
		rename partno person

		merge 1:1 case person using "${dir_data}/was_temp.dta"
		tab partno _m
		drop if _m==1
		drop _m
		recode p_dlltsd p_grad p_emp (missing=0)
		recode p_healths (missing=1)
			
		// weight
		gen dwt = round(xshhwgt*10,1)
		label var dwt "cross-sectional household weight"
		drop if missing(dwt)

		/*
		dhe2 = 1 very good or better - reference
		dhe2 = 2 good
		dhe2 = 3 fair
		dhe2 = 4 bad / very bad
		*/
		gen dhe2 = .
		replace dhe2=1 if (!missing(healths) & healths<2)
		replace dhe2=2 if (healths==2)
		replace dhe2=3 if (healths==3)
		replace dhe2=4 if (!missing(healths) & healths>3)
		label var dhe2 "General health (4 categories)"
		gen dhesp2 = .
		replace dhesp2=1 if (!missing(p_healths) & p_healths<2)
		replace dhesp2=2 if (p_healths==2)
		replace dhesp2=3 if (p_healths==3)
		replace dhesp2=4 if (!missing(p_healths) & p_healths>3)
		rename gor gor2
		rename p_grad gradsp
		rename p_emp empsp
		label var empsp "partner's employment status"  // by LS
		rename p_dlltsd dlltsdsp

		// wealth, omitting value of state pension rights
		// wealth is total net wealth built from: 
		// 1.Non-property net assets (assets); 2.Other property net (oprop); 3.Main-home equity (dvhvalue - main_mort)
		// 4.ISAs (isa_fam); 5.Business equity (bus_assets); 6. Private/occupational pensions (tot_open + tot_pp)
		gen wealth = ww + isa_fam + bus_assets + tot_open + tot_pp
		label var wealth "total net wealth"

		// regression variables
		gen idnk04 = (nk04>0)
		gen dhe2grad = dhe2 * grad
		gen dhe2ngrad = dhe2 * (1-grad)
		gen dlltsdgrad = dlltsd * grad
		gen dlltsdngrad = dlltsd * (1-grad)
		gen empage = emp * dvage17

		gen was = 1

		sort bu

		replace bu_rp = person_id == bu
		bysort was_round case bu: egen n_rp = total(bu_rp==1)
		bysort was_round case bu (person_id): replace bu_rp = 1 if n_rp==0 & _n==1
		drop n_rp
		bysort was_round case bu: egen n_rp = total(bu_rp==1)
		assert n_rp==1
		drop n_rp
		gen single_woman = bu_rp * (na==1) * (sex==2)
		gen single_man = bu_rp * (na==1) * (sex==1)
		gen couple_ref = bu_rp * (na==2)
		gen couple = (na==2)
		gen single = (na==1)

		xtile qtl = inc [fweight=dwt], nq(5)
		
		gen pct = .
		xtile pct1 = inc [fweight=dwt] if (single_woman & grad), nq(10)
		replace pct = pct1 if (pct1<.)
		drop pct1
		xtile pct1 = inc [fweight=dwt] if (single_man & grad), nq(10)
		replace pct = pct1 if (pct1<.)
		drop pct1
		xtile pct1 = inc [fweight=dwt] if (single_woman & grad==0), nq(10)
		replace pct = pct1 if (pct1<.)
		drop pct1
		xtile pct1 = inc [fweight=dwt] if (single_man & grad==0), nq(10)
		replace pct = pct1 if (pct1<.)
		drop pct1
		xtile pct1 = inc [fweight=dwt] if (couple_ref & grad), nq(10)
		replace pct = pct1 if (pct1<.)
		drop pct1
		xtile pct1 = inc [fweight=dwt] if (couple_ref & grad==0), nq(10)
		replace pct = pct1 if (pct1<.)
		drop pct1
		
		// inflation adjust
		gen CPI = .
		forvalues yy = $inflation_minyear/$inflation_maxyear {

			replace CPI = inflation[`yy'-${inflation_minyear}+1,1] if year == `yy'
		}
		foreach var in tot_pen dvhvalue main_mort wealth inc {
			
			replace `var' = `var' / CPI
		}
		

		// save control data
		// Variables below: total value of ISAs, net value of own-business assets, net value of financial and non-financial (non-property) assets, aggregate occupational pension rights, ww, private/personal pension rights (non-occupational)
		keep was_round case person person_id bu bu_rp year sex grad gradsp dvage17 na nk* single_man ///
			single_woman couple couple_ref gor2 dhe2 healths p_healths dlltsd dlltsdsp ///
			idnk04 pct qtl emp empsp tot_pen dvhvalue main_mort wealth inc was dwt  ///
			isa_fam bus_assets assets tot_open ww tot_pp oprop db_op dc_op dc_pen pinc_now benefits ///
			totcsc_sum totcsc_trans_sum totcsc_pers_sum tothp_sum tot_los tot_los_exc_slc totmo_sum ///
			cc_debt cc_debt_trans cc_debt_pers hp_debt loan_debt loan_debt_exc_slc loan_debt_slc ///
			loan_doorstep loan_pawnbroker loan_payday informal_loan_debt highcost_loan_debt ///
			mail_debt unsec_debt_total unsec_debt_core highcost_debt_proxy lowcost_debt ///
			op_memb pp_membu op_membu op_db op_dc pensioners ///
			pp_core_annual pp_fallback_annual pp_contrib_annual pp_contrib_rate_income priv_total_contrib_rate_inc ///
			pp_contrib_source_core pp_contrib_source_fallback occ_emp_contrib_rate occ_emp_contrib_annual ///
			caser5 caser6 caser7 personw5 personw6 personr7
		if (`ww' > `ww0') {
			append using "${dir_data}/was_wealthdata.dta"
		}
		drop if missing(was_round)
		save "${dir_data}/was_wealthdata.dta", replace
		local ww = `ww' + 1
	}
}


/**************************************************************/
*
*	CONSTRUCT LONGITUDINAL LINKAGE ACROSS WAS ROUNDS 5-8
*
/**************************************************************/

use "${dir_data}/was_wealthdata.dta", clear

* Build round-specific person keys (current round key + backward links carried in later rounds)
gen long obs_id = _n
label var obs_id "Row identifier before longitudinal linkage assignment"

gen case_r5 = .
gen person_r5 = .
replace case_r5 = case if was_round==5
replace person_r5 = person if was_round==5
replace case_r5 = caser5 if missing(case_r5)
replace person_r5 = personw5 if missing(person_r5)

gen case_r6 = .
gen person_r6 = .
replace case_r6 = case if was_round==6
replace person_r6 = person if was_round==6
replace case_r6 = caser6 if missing(case_r6)
replace person_r6 = personw6 if missing(person_r6)

gen case_r7 = .
gen person_r7_link = .
replace case_r7 = case if was_round==7
replace person_r7_link = person if was_round==7
replace case_r7 = caser7 if missing(case_r7)
replace person_r7_link = personr7 if missing(person_r7_link)

gen case_r8 = .
gen person_r8 = .
replace case_r8 = case if was_round==8
replace person_r8 = person if was_round==8

gen str40 key_r5 = ""
replace key_r5 = "r5:" + trim(string(case_r5, "%20.0f")) + ":" + trim(string(person_r5, "%8.0f")) if !missing(case_r5) & !missing(person_r5)

gen str40 key_r6 = ""
replace key_r6 = "r6:" + trim(string(case_r6, "%20.0f")) + ":" + trim(string(person_r6, "%8.0f")) if !missing(case_r6) & !missing(person_r6)

gen str40 key_r7 = ""
replace key_r7 = "r7:" + trim(string(case_r7, "%20.0f")) + ":" + trim(string(person_r7_link, "%8.0f")) if !missing(case_r7) & !missing(person_r7_link)

gen str40 key_r8 = ""
replace key_r8 = "r8:" + trim(string(case_r8, "%20.0f")) + ":" + trim(string(person_r8, "%8.0f")) if !missing(case_r8) & !missing(person_r8)

* Pairwise one-to-one linkage propagation across adjacent rounds
* This replaces the earlier "earliest available key wins" anchor approach.
tempfile r5_lookup r6_lookup r7_lookup assign_r5 assign_r6 assign_r7 assign_r8 assign_all

* Round 5 lookup / assignments (base chains)
preserve
    keep if was_round==5
    keep obs_id sex key_r5
    keep if key_r5!=""
    rename key_r5 r5_key
    isid r5_key
    gen str40 chain_key = r5_key
    rename sex sex_r5_lookup
    save `r5_lookup', replace

    keep obs_id chain_key
    gen byte edge56_ok = .
    gen byte edge56_sex_bad = .
    gen byte edge67_ok = .
    gen byte edge67_sex_bad = .
    gen byte edge78_ok = .
    gen byte edge78_sex_bad = .
    save `assign_r5', replace
restore

* Round 6: link to R5 via (caser5, personw5)
preserve
    keep if was_round==6
    keep obs_id sex key_r6 caser5 personw5
    keep if key_r6!=""
    rename key_r6 r6_key
    gen str40 r5_key = ""
    replace r5_key = "r5:" + trim(string(caser5, "%20.0f")) + ":" + trim(string(personw5, "%8.0f")) if !missing(caser5) & !missing(personw5)

    merge m:1 r5_key using `r5_lookup', keep(master match) keepusing(chain_key sex_r5_lookup) gen(_m56)

    duplicates tag r5_key if _m56==3, gen(dup_t56)
    replace dup_t56 = 0 if missing(dup_t56)
    duplicates tag r6_key if _m56==3, gen(dup_s56)
    replace dup_s56 = 0 if missing(dup_s56)

    gen byte edge56_sex_bad = (sex != sex_r5_lookup) if _m56==3 & !missing(sex) & !missing(sex_r5_lookup)
    replace edge56_sex_bad = 0 if missing(edge56_sex_bad)

    gen byte edge56_ok = (_m56==3 & dup_t56==0 & dup_s56==0 & edge56_sex_bad==0)

    gen str40 chain_key_out = ""
    replace chain_key_out = chain_key if edge56_ok==1
    replace chain_key_out = r6_key if chain_key_out==""

    rename sex sex_r6_lookup
    keep obs_id r6_key sex_r6_lookup chain_key_out edge56_ok edge56_sex_bad
    rename chain_key_out chain_key
    save `r6_lookup', replace

    keep obs_id chain_key edge56_ok edge56_sex_bad
    gen byte edge67_ok = .
    gen byte edge67_sex_bad = .
    gen byte edge78_ok = .
    gen byte edge78_sex_bad = .
    save `assign_r6', replace
restore

* Round 7: link to R6 via (caser6, personw6)
preserve
    keep if was_round==7
    keep obs_id sex key_r7 caser6 personw6
    keep if key_r7!=""
    rename key_r7 r7_key
    gen str40 r6_key = ""
    replace r6_key = "r6:" + trim(string(caser6, "%20.0f")) + ":" + trim(string(personw6, "%8.0f")) if !missing(caser6) & !missing(personw6)

    merge m:1 r6_key using `r6_lookup', keep(master match) keepusing(chain_key sex_r6_lookup) gen(_m67)

    duplicates tag r6_key if _m67==3, gen(dup_t67)
    replace dup_t67 = 0 if missing(dup_t67)
    duplicates tag r7_key if _m67==3, gen(dup_s67)
    replace dup_s67 = 0 if missing(dup_s67)

    gen byte edge67_sex_bad = (sex != sex_r6_lookup) if _m67==3 & !missing(sex) & !missing(sex_r6_lookup)
    replace edge67_sex_bad = 0 if missing(edge67_sex_bad)

    gen byte edge67_ok = (_m67==3 & dup_t67==0 & dup_s67==0 & edge67_sex_bad==0)

    gen str40 chain_key_out = ""
    replace chain_key_out = chain_key if edge67_ok==1
    replace chain_key_out = r7_key if chain_key_out==""

    rename sex sex_r7_lookup
    keep obs_id r7_key sex_r7_lookup chain_key_out edge67_ok edge67_sex_bad
    rename chain_key_out chain_key
    save `r7_lookup', replace

    keep obs_id chain_key edge67_ok edge67_sex_bad
    gen byte edge56_ok = .
    gen byte edge56_sex_bad = .
    gen byte edge78_ok = .
    gen byte edge78_sex_bad = .
    save `assign_r7', replace
restore

* Round 8: link to R7 via (caser7, personr7)
preserve
    keep if was_round==8
    keep obs_id sex key_r8 caser7 personr7
    keep if key_r8!=""
    rename key_r8 r8_key
    gen str40 r7_key = ""
    replace r7_key = "r7:" + trim(string(caser7, "%20.0f")) + ":" + trim(string(personr7, "%8.0f")) if !missing(caser7) & !missing(personr7)

    merge m:1 r7_key using `r7_lookup', keep(master match) keepusing(chain_key sex_r7_lookup) gen(_m78)

    duplicates tag r7_key if _m78==3, gen(dup_t78)
    replace dup_t78 = 0 if missing(dup_t78)
    duplicates tag r8_key if _m78==3, gen(dup_s78)
    replace dup_s78 = 0 if missing(dup_s78)

    gen byte edge78_sex_bad = (sex != sex_r7_lookup) if _m78==3 & !missing(sex) & !missing(sex_r7_lookup)
    replace edge78_sex_bad = 0 if missing(edge78_sex_bad)

    gen byte edge78_ok = (_m78==3 & dup_t78==0 & dup_s78==0 & edge78_sex_bad==0)

    rename chain_key chain_key_prev
    gen str40 chain_key = ""
    replace chain_key = chain_key_prev if edge78_ok==1
    replace chain_key = r8_key if chain_key==""

    keep obs_id chain_key edge78_ok edge78_sex_bad
    gen byte edge56_ok = .
    gen byte edge56_sex_bad = .
    gen byte edge67_ok = .
    gen byte edge67_sex_bad = .
    save `assign_r8', replace
restore

* Combine round-specific assignments and merge back to full file
use `assign_r5', clear
append using `assign_r6'
append using `assign_r7'
append using `assign_r8'
isid obs_id
save `assign_all', replace

use "${dir_data}/was_wealthdata.dta", clear

gen long obs_id = _n
label var obs_id "Row identifier before longitudinal linkage assignment"

merge 1:1 obs_id using `assign_all', nogen keep(master match) keepusing(chain_key)
assert !missing(chain_key)

rename chain_key was_link_key
label var was_link_key "Canonical linkage key from pairwise one-to-one propagation"

egen was_pid = group(was_link_key)
label var was_pid "Synthetic longitudinal person ID (WAS rounds 5-8 linkage)"

sort was_pid year was_round
save "${dir_data}/was_wealthdata.dta", replace


/**************************************************************/
*
*	ANALYSIS OF WORKING VARIABLES
*
/**************************************************************/

/*
use "${dir_data}/was_wealthdata.dta", clear

tab gor2 year if (year>2014 & year<2020) [fweight=dwt], nol
tab dvage17 year if (year>2014 & year<2020 & dvage17>4) [fweight=dwt], nol
tab sex year if (year>2014 & year<2020 & dvage17>4) [fweight=dwt], nol
gen dwt2 = round(dwt/na, 1)
tab na year if (year>2014 & year<2020 & bu_rp==1) [fweight=dwt], nol
tab nk year if (year>2014 & year<2020 & bu_rp==1) [fweight=dwt], nol
tab nk04 year if (year>2014 & year<2020 & bu_rp==1) [fweight=dwt], nol
tab healths year if (year>2014 & year<2020 & dvage17>4) [fweight=dwt], nol
tab dhe2 year if (year>2014 & year<2020 & dvage17>4) [fweight=dwt], nol
tab grad year if (year>2014 & year<2020 & dvage17>4) [fweight=dwt], nol
tab dlltsd year if (year>2014 & year<2020 & dvage17>4) [fweight=dwt], nol
tab emp year if (year>2014 & year<2020 & dvage17>4) [fweight=dwt], nol

gen chk = (inc<0.1)
tab chk year if (year>2014 & year<2020 & bu_rp==1) [fweight=dwt], nol
sum inc [fweight=dwt2] if (chk==0)

gen wealth1 = asinh(wealth)
sum wealth [fweight=dwt2], detail
sum wealth1 [fweight=dwt2], detail

sum wealth1 [fweight=dwt2] if (single_woman & year==2015), detail
sum wealth1 [fweight=dwt2] if (single_woman & year==2016), detail
sum wealth1 [fweight=dwt2] if (single_woman & year==2017), detail
sum wealth1 [fweight=dwt2] if (single_woman & year==2018), detail
sum wealth1 [fweight=dwt2] if (single_woman & year==2019), detail
sum wealth1 [fweight=dwt2] if (single_woman), detail

sum wealth1 [fweight=dwt2] if (single_man & year==2015), detail
sum wealth1 [fweight=dwt2] if (single_man & year==2016), detail
sum wealth1 [fweight=dwt2] if (single_man & year==2017), detail
sum wealth1 [fweight=dwt2] if (single_man & year==2018), detail
sum wealth1 [fweight=dwt2] if (single_man & year==2019), detail
sum wealth1 [fweight=dwt2] if (single_man), detail

sum wealth1 [fweight=dwt] if (couple_ref & year==2015), detail
sum wealth1 [fweight=dwt] if (couple_ref & year==2016), detail
sum wealth1 [fweight=dwt] if (couple_ref & year==2017), detail
sum wealth1 [fweight=dwt] if (couple_ref & year==2018), detail
sum wealth1 [fweight=dwt] if (couple_ref & year==2019), detail
sum wealth1 [fweight=dwt] if (couple_ref), detail


sum wealth [fweight=dwt2] if (single_woman & year==2015), detail
sum wealth [fweight=dwt2] if (single_woman & year==2016), detail
sum wealth [fweight=dwt2] if (single_woman & year==2017), detail
sum wealth [fweight=dwt2] if (single_woman & year==2018), detail
sum wealth [fweight=dwt2] if (single_woman & year==2019), detail
sum wealth [fweight=dwt2] if (single_woman), detail

sum wealth [fweight=dwt2] if (single_man & year==2015), detail
sum wealth [fweight=dwt2] if (single_man & year==2016), detail
sum wealth [fweight=dwt2] if (single_man & year==2017), detail
sum wealth [fweight=dwt2] if (single_man & year==2018), detail
sum wealth [fweight=dwt2] if (single_man & year==2019), detail
sum wealth [fweight=dwt2] if (single_man), detail

sum wealth [fweight=dwt] if (couple_ref & year==2015), detail
sum wealth [fweight=dwt] if (couple_ref & year==2016), detail
sum wealth [fweight=dwt] if (couple_ref & year==2017), detail
sum wealth [fweight=dwt] if (couple_ref & year==2018), detail
sum wealth [fweight=dwt] if (couple_ref & year==2019), detail
sum wealth [fweight=dwt] if (couple_ref), detail

sum wealth [fweight=dwt] if (bu_rp & year==2015), detail
sum wealth [fweight=dwt] if (bu_rp & year==2016), detail
sum wealth [fweight=dwt] if (bu_rp & year==2017), detail
sum wealth [fweight=dwt] if (bu_rp & year==2018), detail
sum wealth [fweight=dwt] if (bu_rp & year==2019), detail
sum wealth [fweight=dwt] if (bu_rp), detail

*/

cap log close

/**************************************************************************************
* clean-up and exit
*************************************************************************************/
#delimit ;
local files_to_drop 
	chk.dta
	was_temp.dta
	was_temp_hh5.dta
	was_temp_hh6.dta
	was_temp_hh7.dta
	was_temp_hh8.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data/`file'"
}

