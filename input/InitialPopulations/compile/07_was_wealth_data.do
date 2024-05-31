/**************************************************************/
*														
*	FILE TO COMPILE WAS DATA FOR IMPUTING WEALTH INTO UKHLS DATA
*
*	DATA: 		WAS EUL version - UKDA-7215-stata [to wave 7]
*	AUTH: 		Justin van de Ven (JV), Daria Popova (DP)
*	LAST EDIT: 	11/04/2024 (JV)
*
*	NOTE: 		file currently compiles data to merge from 2016
*				this could be extended to at least 2011
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
*/


/**************************************************************/
*	
*	Preliminaries
*
/**************************************************************/
cd "${dir_data}"
disp "analysing WAS wealth data"
clear all
set maxvar 10000


/**************************************************************/
*	
*	Extract WAS data from household level datasets
*
/**************************************************************/
local ww = 5
foreach file in "$dir_was_data\was_round_5_hhold_eul_feb_20.dta" ///
				"$dir_was_data\was_round_6_hhold_eul_april_2022.dta" ///
				"$dir_was_data\was_round_7_hhold_eul_march_2022.dta" {
	
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
	save "$dir_data\was_temp_hh`ww'.dta", replace
	local ww = `ww' + 1
}


/**************************************************************/
*	
*	Extract data from person level dataset
*
/**************************************************************/
local ww = 5
local ww0 = 5
foreach file in "$dir_was_data\was_round_5_person_eul_oct_2020.dta" ///
				"$dir_was_data\was_round_6_person_eul_april_2022.dta" ///
				"$dir_was_data\was_round_7_person_eul_june_2022.dta" {

	disp "compiling WAS wealth data for wave `ww'"
	qui {

		use "`file'", clear
		rename *, l
		
		if (`ww' < 6) {
			keep caser`ww' personw`ww' partnow`ww' dvage17r`ww' hasdepw`ww' dvgiser`ww' dvgippenr`ww' dvgiinvr`ww' dvoigrrannualr`ww'_i dvoiggtannualr`ww'_i dvoigegannualr`ww'_i ///
				dvoigfrannualr`ww'_i dvoigmaannualr`ww'_i dvoigroannualr`ww'_i dvoigopannualr`ww'_i dvmrdfr`ww' isdepw`ww' p_flag4w`ww' r`ww'xshhwgt ///
				btype1w`ww' qhealth1w`ww' lsillw`ww' dvisavalr`ww'_sum edlevelr`ww' enrollw`ww' coursew`ww' dvgiempr`ww' dvgiser`ww' statr`ww' ftptwkw`ww' dvecactr`ww' ///
				pemoffw`ww' pemelgw`ww' pemmemw`ww' poctyp1w`ww' poctyp2w`ww' dvbenefitannualr`ww'_i dvgippenr`ww' dvgiinvr`ww' bval* dvhsevalr`ww' dvhsedebtr`ww' ///
				dvbldvalr`ww' dvblddebtr`ww' dvlukvalr`ww' dvlukdebtr`ww' dvlosvalr`ww' dvlosdebtr`ww' dvoprvalr`ww' dvoprdebtr`ww' dvffassetsr`ww' dvfinfvalr`ww' ///
				dvvaldcosr`ww' dvpavcuvr`ww' dvpfcurvalr`ww' dvppvalr`ww' dvpfddvr`ww' dvspenr`ww' dvpinpvalr`ww' pincinpr`ww' totccr`ww'_sum dvffassetsr`ww' dvfinfvalr`ww' tothpr`ww' ///
				tot_losr`ww' totmor`ww' dvvaldbtr`ww' dvdbrwealthvalr`ww' gorr`ww' sexr`ww'
			rename totccr`ww'_sum totcscr`ww'_sum
			recode partnow`ww' (missing=17)
			recode partnow`ww' (-7=17)
		}
		else if (`ww' == 6) {
			keep caser`ww' personw`ww' partnow`ww' dvage17r`ww' hasdepw`ww' dvgiser`ww' dvgippenr`ww' dvgiinvr`ww' dvoigrrannualr`ww'_i dvoiggtannualr`ww'_i dvoigegannualr`ww'_i ///
				dvoigfrannualr`ww'_i dvoigmaannualr`ww'_i dvoigroannualr`ww'_i dvoigopannualr`ww'_i dvmrdfr`ww' isdepw`ww' p_flag4w`ww' r`ww'xshhwgt ///
				btype1w`ww' qhealth1w`ww' lsillw`ww' dvisavalr`ww'_sum edlevelr`ww' enrollw`ww' coursew`ww' dvgiempr`ww' dvgiser`ww' statr`ww' ftptwkw`ww' dvecactr`ww' ///
				pemoffw`ww' pemelgw`ww' pemmemw`ww' poctyp1w`ww' poctyp2w`ww' dvbenefitannualr`ww'_i dvgippenr`ww' dvgiinvr`ww' bval* dvhsevalr`ww' dvhsedebtr`ww' ///
				dvbldvalr`ww' dvblddebtr`ww' dvlukvalr`ww' dvlukdebtr`ww' dvlosvalr`ww' dvlosdebtr`ww' dvoprvalr`ww' dvoprdebtr`ww' dvffassetsr`ww' dvfinfvalr`ww' ///
				dvvaldcosr`ww' dvpavcuvr`ww' dvpfcurvalr`ww' dvppvalr`ww' dvpfddvr`ww' dvspenr`ww' dvpinpvalr`ww' pincinpr`ww' totcscr`ww'_sum dvffassetsr`ww' dvfinfvalr`ww' tothpr`ww' ///
				tot_losr`ww' totmor`ww' dvvaldbtr`ww' dvdbrwealthvalr`ww' gorr`ww' sexr`ww'
			recode partnow`ww' (missing=17)
		}
		else {
			keep caser`ww' personr`ww' partnor`ww' dvage17r`ww' hasdepr`ww' dvgiser`ww' dvgippenr`ww' dvgiinvr`ww' dvoigrrannualr`ww'_i dvoiggtannualr`ww'_i dvoigegannualr`ww'_i ///
				dvoigfrannualr`ww'_i dvoigmaannualr`ww'_i dvoigroannualr`ww'_i dvoigopannualr`ww'_i dvmrdfr`ww' isdepr`ww' p_flag4r`ww' r`ww'xshhwgt ///
				btype1r`ww' qhealth1r`ww' lsillr`ww' dvisavalr`ww'_sum edlevelr`ww' enrollr`ww' courser`ww' dvgiempr`ww' dvgiser`ww' statr`ww' ftptwkr`ww' dvecactr`ww' ///
				pemoffr`ww' pemelgr`ww' pemmemr`ww' poctyp1r`ww' poctyp2r`ww' dvbenefitannualr`ww'_i dvgippenr`ww' dvgiinvr`ww' bval* dvhsevalr`ww' dvhsedebtr`ww' ///
				dvbldvalr`ww' dvblddebtr`ww' dvlukvalr`ww' dvlukdebtr`ww' dvlosvalr`ww' dvlosdebtr`ww' dvoprvalr`ww' dvoprdebtr`ww' dvffassetsr`ww'_sum dvfinfvalr`ww' ///
				dvvaldcosr`ww' dvpavcuvr`ww' dvppvalr`ww' dvspenr`ww' dvpinpvalr`ww' pincinpr`ww' totcscr`ww'_sum dvfinfvalr`ww' tothpr`ww' tot_losr`ww' ///
				totmor`ww' dvvaldbtr`ww' dvdbincallr`ww' gorr`ww' sexr`ww' dvretdc_noaccessr`ww' dvretdc_accessr`ww'
			rename dvffassetsr`ww'_sum dvffassetsr`ww'
			rename dvdbincallr`ww' dvdbrwealthvalr`ww'
			gen dvpfcurvalr`ww' = dvretdc_accessr`ww' + dvretdc_noaccessr`ww'
			gen dvpfddvr`ww' = 0
			recode partnor`ww' (missing=17)
		}
		if (`ww' < 7) {
			rename *w`ww'* **
		}
		rename *r`ww'* **
		keep if (!missing(case) & !missing(person))
			
		// merge with household level data
		sort case person
		merge m:1 case using "$dir_data\was_temp_hh`ww'.dta", nogen sorted
		
		// ypnbihs
		recode dvgiemp dvgise dvgippen dvgiinv dvoigrrannual_i dvoiggtannual_i dvoigegannual_i dvoigfrannual_i dvoigmaannual_i dvoigroannual_i dvoigopannual_i (missing=0)
		gen inci = dvgiemp + dvgise + dvgippen + dvgiinv + dvoigrrannual_i + dvoiggtannual_i + dvoigegannual_i + dvoigfrannual_i + dvoigmaannual_i + dvoigroannual_i + dvoigopannual_i
		
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
		replace totcsc_sum = 0 if (totcsc_sum>=.)
		gen assts = dvffassets + dvfinfval - totcsc_sum - tothp - tot_los - totmo
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
		gen pi_temp = pincinp * (pincinp>0.01)
		egen pinc_now = sum(pi_temp), by (case bu)

		// welfare benefits
		egen benefits = sum(dvbenefitannual_i), by (case bu)

		// net wealth
		gen ww = assets + oprop - isa_fam - bus_assets
		replace ww = ww + dvhvalue if ( dvhvalue<.)
		replace ww = ww - main_mort if ( main_mort<.)
		
		save "$dir_data\chk.dta", replace

		// partner characteristics
		save "$dir_data\was_temp.dta", replace
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

		merge 1:1 case person using "$dir_data\was_temp.dta"
		tab partno _m
		drop if _m==1
		drop _m
		recode p_dlltsd p_grad p_emp (missing=0)
		recode p_healths (missing=1)
			
		// weight
		gen dwt = round(xshhwgt*10,1)
		label var dwt "cross-sectional household weight"

		/*
		dhe2 = 1 very good or better - reference
		dhe2 = 2 good
		dhe2 = 3 fair
		dhe2 = 4 bad / very bad
		*/
		gen dhe2 = 0
		replace dhe2=1 if (healths<2)
		replace dhe2=2 if (healths==2)
		replace dhe2=3 if (healths==3)
		replace dhe2=4 if (healths>3)
		label var dhe2 "General health (4 categories)"
		gen dhesp2 = 0
		replace dhesp2=1 if (p_healths<2)
		replace dhesp2=2 if (p_healths==2)
		replace dhesp2=3 if (p_healths==3)
		replace dhesp2=4 if (p_healths>3)
		rename gor gor2
		rename p_grad gradsp
		rename p_emp empsp
		rename p_dlltsd dlltsdsp

		// wealth, omitting value of state pension rights
		gen wealth = ww + isa_fam + bus_assets + tot_open + tot_pp
		label var wealth "total net wealth"
		
		// net value of main home
		gen nvmhome = dvhvalue - main_mort
		label var nvmhome "net value of main home"

		// regression variables
		gen idnk04 = (nk04>0)
		gen dhe2grad = dhe2 * grad
		gen dhe2ngrad = dhe2 * (1-grad)
		gen dlltsdgrad = dlltsd * grad
		gen dlltsdngrad = dlltsd * (1-grad)
		gen empage = emp * dvage17

		gen was = 1

		sort bu

		gen bu_rp = person_id == bu
		gen single_woman = bu_rp * (na==1) * (sex==2)
		gen single_man = bu_rp * (na==1) * (sex==1)
		gen couple_ref = bu_rp * (na==2)
		gen couple = (na==2)
		gen single = (na==1)
		
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
		
		// adjust for inflation
		/*CPIH INDEX 00: ALL ITEMS 2015=100

		TAKEN FROM 02_create_ukhls_variables.do

		CDID	L522
		Source dataset ID	MM23
		PreUnit	
		Unit	Index, base year = 100
		Release date	20-03-2024
		Next release	17 April 2024
		https://www.ons.gov.uk/economy/inflationandpriceindices/timeseries/l522/mm23
		*/
		gen CPI = .
		replace CPI = 0.879 if year == 2009
		replace CPI = 0.901 if year == 2010
		replace CPI = 0.936 if year == 2011
		replace CPI = 0.96  if year == 2012
		replace CPI = 0.982 if year == 2013
		replace CPI = 0.996 if year == 2014
		replace CPI = 1     if year == 2015
		replace CPI = 1.01  if year == 2016
		replace CPI = 1.036 if year == 2017
		replace CPI = 1.06  if year == 2018
		replace CPI = 1.078 if year == 2019
		replace CPI = 1.089 if year == 2020
		replace CPI = 1.116 if year == 2021
		replace CPI = 1.205 if year == 2022
		replace CPI = 1.286 if year == 2023
		
		replace wealth = wealth / CPI
		replace tot_pen = tot_pen / CPI
		replace nvmhome = nvmhome / CPI
		replace inc = inc / CPI
		
		// save control data
		keep case person_id bu bu_rp year sex grad gradsp dvage17 na nk* single_man single_woman couple couple_ref gor2 dhe2 healths p_healths dlltsd dlltsdsp idnk04 pct emp empsp wealth tot_pen nvmhome inc was dwt
		if (`ww' > `ww0') {
			append using "$dir_data\was_wealthdata.dta"
		}
		save "$dir_data\was_wealthdata.dta", replace
		local ww = `ww' + 1
	}
}


/**************************************************************/
*
*	ANALYSIS OF WORKING VARIABLES
*
/**************************************************************/

/*
use "$dir_data\was_wealthdata.dta", clear

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


/**************************************************************************************
* clean-up and exit
**************************************************************************************/
#delimit ;
local files_to_drop 
	chk.dta
	was_temp.dta
	was_temp_hh5.dta
	was_temp_hh6.dta
	was_temp_hh7.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data/`file'"
}



/**************************************************************/
*
*	END 
*
/**************************************************************/



