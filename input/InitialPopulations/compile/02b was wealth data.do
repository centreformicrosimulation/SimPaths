/**************************************************************/
*														
*	FILE TO COMPILE WAS DATA FOR ESTIMATING EQUATIONS USED TO IMPUTE WEALTH INTO UKHLS DATA
*
*	AUTH: Justin van de Ven (JV)
*	LAST EDIT: 09/09/2023 (JV)
*
/**************************************************************/


/**************************************************************/
*
*	Set location of working directories
*
/**************************************************************/

/*
* working directory
global workingDir "C:\MyFiles\00 CURRENT\03 PROJECTS\Essex\SimPaths\02 PARAMETERISE\STARTING DATA\data"

/* Please list the location of the WAS *.dta files here */
global WASdir "C:\MyFiles\01 DATA\UK\was\wave7\stata\stata13_se"

/* Please list name of round 6 household level file here */
global hhFileWAS6 "was_round_6_hhold_eul_april_2022.dta"

/* Please list name of round 6 person level file here */
global ppFileWAS6 "was_round_6_person_eul_april_2022.dta"

/* Please list name of wave 5 person level file here */
global ppFileWAS5 "was_wave_5_person_eul_oct_2020.dta"

/* Please list name of wave 4 person level file here */
global ppFileWAS4 "was_wave_4_person_eul_oct_2020.dta"

/* Please list name of wave 3 person level file here */
global ppFileWAS3 "was_wave_3_person_eul_oct_2020.dta"

/* Please list name of wave 2 person level file here */
global ppFileWAS2 "was_wave_2_person_eul_nov_2020.dta"

/* Please list name of wave 1 person level file here */
global ppFileWAS1 "was_wave_1_person_eul_nov_2020.dta"

/* Please list the location of the WAS *.dta files here */
global WASdir "C:\MyFiles\01 DATA\UK\was\wave7\stata\stata13_se"

/* Please list name of round 6 household level file here */
global hhFileWAS6 "was_round_6_hhold_eul_april_2022.dta"

/* Please list name of round 6 person level file here */
global ppFileWAS6 "was_round_6_person_eul_april_2022.dta"

/* Please list name of wave 5 person level file here */
global ppFileWAS5 "was_wave_5_person_eul_oct_2020.dta"

/* Please list name of wave 4 person level file here */
global ppFileWAS4 "was_wave_4_person_eul_oct_2020.dta"

/* Please list name of wave 3 person level file here */
global ppFileWAS3 "was_wave_3_person_eul_oct_2020.dta"

/* Please list name of wave 2 person level file here */
global ppFileWAS2 "was_wave_2_person_eul_nov_2020.dta"

/* Please list name of wave 1 person level file here */
global ppFileWAS1 "was_wave_1_person_eul_nov_2020.dta"
*/


/**************************************************************/
*
*	Start of automated program
*
*	NOTE: remaining code should be independent of computer used
*
/**************************************************************/

/* Define additional directory locations */
global outputdir "$workingDir"


/**************************************************************/
*	
*	Extract data from household level datasets
*
/**************************************************************/

/* round 6 dataset */
use "$WASdir\\$hhFileWAS6", clear
rename *, l
keep caser6 dvhvaluer6 totmortr6 allendwr6 accomw6 hsetypew6 ten1r6 hrpnssec3w6

foreach vv in dvhvalue totmort allendw ten1 {

	rename `vv'r6 `vv'
}
foreach vv in accom hsetype hrpnssec3 {

	rename `vv'w6 `vv'
}


/* value of main home */
desc dvhvalue
gen main_mort = totmort + allendw
label var main_mort "total mortgage debt owed on main home"

/* other variables for wage regression	*/
gen case = _n
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

/* discard unwanted variables and save	*/
sort caser6
keep caser6 dvhvalue main_mort flat room house_d house_s house_t accm_o accm_m accm_r accm_f hrp_manager hrp_intermediate
save "$workingDir\\temp_hh6.dta", replace


/**************************************************************/
*	
*	variables from previous waves for earnings and employment
*
/**************************************************************/

* wave 1
use "$WASdir\\$ppFileWAS6", clear
rename *, l
keep caser* casew* personw*
keep if ((casew1<.) & (personw1<.))
sort casew1 personw1 personw6
gen chk = 0
replace chk = 1 if ((casew2==casew2[_n-1])&(personw2==personw2[_n-1]))
drop if (chk==1)
drop chk
save "$workingDir\\temp_pp6.dta", replace

use "$WASdir\\$ppFileWAS1", clear
rename *, l
keep casew1 personw1 dvgrspayw1 dvgrsjob2w1 dvgrssejobw1 dvseamtw1 statw1 ftptwkw1 empstat2w1 teaw1
foreach vv in dvgrspay dvgrsjob2 dvgrssejob dvseamt stat ftptwk empstat2 {

	rename `vv'w1 `vv'
}
gen earningsw1 = dvgrspay + dvgrsjob2 + dvgrssejob + dvseamt
label var earningsw1 "gross earnings wave 1"

gen ftiw1 = (stat==1 & ftptwk==1)
gen ptiw1 = (stat==1 & ftptwk==2)
gen ue = (empstat2==3)
replace ftiw1 = 0 if (ue==1 | earningsw1==0)
replace ptiw1 = 0 if (ue==1 | earningsw1==0)
replace ftiw1 = 1 if ( (earningsw1>15000) & (ftiw1+ptiw1==0) )
replace ptiw1 = 1 if ( (earningsw1<=15000) & (ftiw1+ptiw1==0) & (earningsw1>0) )

label var ftiw1 "FT work wave 1"
label var ptiw1 "PT work wave 1"

keep if (casew1<.)
keep casew1 personw1 earningsw1 ftiw1 ptiw1 teaw1
sort casew1 personw1

merge 1:1 casew1 personw1 using "$workingDir\\temp_pp6.dta", nogen sorted
keep if ((caser6<.) & (personw6<.))
sort caser6 personw6
save "$workingDir\\temp_pp1.dta", replace

* wave ww = 2 to 5
forval ww = 2/5 {
* local ww = 2

	use "$WASdir\\$ppFileWAS6", clear
	rename *, l
	keep caser* casew* personw*
	keep if ((casew`ww'<.) & (personw`ww'<.))
	sort casew`ww' personw`ww' personw6
	gen chk = 0
	replace chk = 1 if ((casew`ww'==casew`ww'[_n-1])&(personw`ww'==personw`ww'[_n-1]))
	drop if (chk==1)
	drop chk
	save "$workingDir\\temp_pp6.dta", replace

	if ( `ww'==2 ) use "$WASdir/$ppFileWAS2", clear
	if ( `ww'==3 ) use "$WASdir/$ppFileWAS3", clear
	if ( `ww'==4 ) use "$WASdir/$ppFileWAS4", clear
	if ( `ww'==5 ) use "$WASdir/$ppFileWAS5", clear
	rename *, l
	if ( `ww'<3.5 ) {
	
		keep casew`ww' personw`ww' dvgrspayw`ww' dvsegrspayw`ww' dvgrsempsecjobw`ww' dvgrssesecjobw`ww' dvgrsbonamtw`ww' statw`ww' ftptwkw`ww' empstat2w`ww' teaw`ww'
		rename empstat2w`ww' dvecactw`ww'
	}
	else {
	
		keep casew`ww' personw`ww' dvgrspayw`ww' dvsegrspayw`ww' dvgrsempsecjobw`ww' dvgrssesecjobw`ww' dvgrsbonamtw`ww' statw`ww' ftptwkw`ww' dvecactw`ww' teaw`ww'
	}
	foreach vv in dvgrspay dvsegrspay dvgrsempsecjob dvgrssesecjob dvgrsbonamt stat ftptwk dvecact {
	
		rename `vv'w`ww' `vv'
	}
	gen earningsw`ww' = dvgrspay*(dvgrspay>0) + dvsegrspay*(dvsegrspay>0) + dvgrsempsecjob*(dvgrsempsecjob>0) + ///
					dvgrssesecjob*(dvgrssesecjob>0) + dvgrsbonamt*(dvgrsbonamt>0) 
	label var earningsw`ww' "gross earnings wave `ww'"

	gen ftiw`ww' = (stat==1 & ftptwk==1)
	gen ptiw`ww' = (stat==1 & ftptwk==2)
	gen ue = (dvecact==3)
	replace ftiw`ww' = 0 if (ue==1 | earningsw`ww'==0)
	replace ptiw`ww' = 0 if (ue==1 | earningsw`ww'==0)
	replace ftiw`ww' = 0 if (earningsw`ww'==0)
	replace ptiw`ww' = 0 if (earningsw`ww'==0)
	replace ftiw`ww' = 1 if ( (earningsw`ww'>15000) & (ftiw`ww'+ptiw`ww'==0) )
	replace ptiw`ww' = 1 if ( (earningsw`ww'<=15000) & (ftiw`ww'+ptiw`ww'==0) & (earningsw`ww'>0) )

	label var ftiw`ww' "FT work wave `ww'"
	label var ptiw`ww' "PT work wave `ww'"

	keep if ( (casew`ww'<.) & (personw`ww'<.) )
	keep casew`ww' personw`ww' earningsw`ww' ftiw`ww' ptiw`ww' teaw`ww'
	sort casew`ww' personw`ww'
	gen chk = 0
	replace chk = 1 if ((casew`ww'==casew`ww'[_n-1])&(personw`ww'==personw`ww'[_n-1]))
	drop if (chk==1)
	drop chk

	merge 1:1 casew`ww' personw`ww' using "$workingDir\\temp_pp6.dta", nogen sorted
	keep if ((caser6<.) & (personw6<.))
	sort caser6 personw6
	save "$workingDir\\temp_pp`ww'.dta", replace
}


/**************************************************************/
*	
*	Extract data from person level dataset
*
/**************************************************************/
use "$WASdir/$ppFileWAS6", clear
rename *, l
keep if ((caser6<.) & (personw6<.))

/* Gross personal non-benefit income */
recode dvgiempr6 dvgiser6 dvgippenr6 dvgiinvr6 dvoigrrannualr6_i dvoigrrannualr6_i ///
dvoiggtannualr6_i dvoigegannualr6_i dvoigfrannualr6_i dvoigmaannualr6_i ///
dvoigroannualr6_i dvoigopannualr6_i (missing=0)

gen ypnbihs = dvgiempr6 + dvgiser6 + dvgippenr6 + dvgiinvr6 + ///
dvoigrrannualr6_i + dvoigrrannualr6_i + dvoiggtannualr6_i + ///
dvoigegannualr6_i + dvoigfrannualr6_i + dvoigmaannualr6_i + ///
dvoigroannualr6_i + dvoigopannualr6_i

/* merge with data from preceding waves */
sort caser6 personw6
forval ww = 1/5 {

	merge 1:1 caser6 personw6 using "$workingDir/temp_pp`ww'.dta", nogen sorted
	erase "$workingDir/temp_pp`ww'.dta"
}
erase "$workingDir\\temp_pp6.dta"

/* merge with household level data */
merge m:1 caser6 using  "$workingDir/temp_hh6.dta", nogen sorted
erase "$workingDir/temp_hh6.dta"

/* complete preparations */
drop dburdw5w6 dcsc* rfr6 rsr6 dvcontocc_emer_dc1w6 dvcontocc_emer_dc2w6 dvcontocc_emee_dc1w6 dvcontocc_emee_dc2w6 dvhascscw6
rename *w6* **
rename *r6* **
drop casew*
drop caser*
drop personw*
drop if (person>=.)                     /* omits empty case numbers */

/* fix some partner person numbers */
duplicates tag case partno, gen(dup)	/* Rows should be unique in case partno, assuming monogamy */
replace dup=0 if partno==17				/* partno=17 if no partner in household */
*tab dup
sort case person
gen mari = inlist(dvmrdf,1,2,8)		/* Married/cohabiting/couple */
label var mari "Married/cohabiting/in couple"
/* Person 2's spouse is person 1 if the two are married, in the same HH, and person 1's spouse is person 2 */
replace partno=1 if partno==2 & person==2 & person[_n-1]==1 & partno[_n-1]==2 & mari==1 & case==case[_n-1]
drop dup
duplicates tag case partno, gen(dup)
replace dup=0 if partno==17
*tab dup
drop if dup>0
drop dup

/* define benefit units */
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

/* employment status */
gen emp = (btype1>0)

/*	health state */
gen healths = qhealth1
gen dlltsd = (lsill==1)

/* income */
gsort case bu
by case bu: egen inc = sum(ypnbihs)

/* age left full-time education*/
gen edage = teaw1
replace edage = teaw2 if ( ( (teaw2<.)&(teaw2>edage) ) | ( (teaw2<.)&(edage>=.) ) )
replace edage = tea if ( ( (tea<.)&(tea>edage) ) | ( (tea<.)&(edage>=.) ) )
replace edage = 0 if (edage<0.5)
label var edage "age left full-time education"

/* number of adults */
egen na=sum(adlt), by (case bu)
label var na "number of adults"
egen na_hh=sum(adlt), by (case)

/* number of children */
egen nk=sum(chld), by (case bu)
label var nk "number of children"
gen nk04i = (dvage17<2)
egen nk04 = sum(nk04i), by (case bu)
label var nk "number of children aged 0-4"

/* common name for weighting variable between waves */
gen xs_wgt = xshhwgt

/* ISA and PEPs */
egen isa_fam = sum(dvisaval_sum), by (case bu)
label var isa_fam "total value of ISAs"

/* whether have degree level qualification */
gen grad=(edlevel==1)

/* whether currently a university student */
gen student = (enroll==1)*((course==6) + (course==4))*(grad==0)

/* individual earnings income */
gen earnings = 0
replace earnings = dvgiemp if ((dvgiemp<.) & (dvgiemp>0))
replace earnings = earnings + dvgise if ((dvgise<.) & (dvgise>0))
label var earnings "gross earnings"

/* individual labour force status */
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

/* current pension arrangements */
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

/* gross employment, investment and pension income */
gen wkg_grsi = dvgiemp + dvgise + dvbenefitannual_i + dvgippen + dvgiinv
gen wkg_empi = dvgiemp + dvgise
egen wkg_grs = sum(wkg_grsi), by (case bu)
egen wkg_emp = sum(wkg_empi), by (case bu)
egen wkg_pen = sum(dvgippen), by (case bu)
egen wkg_ben = sum(dvbenefitannual_i), by (case bu)

/* value of equity in own business net of business loans */
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

/* property other than main home */
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

/* financial and non-financial assets */
desc dvffassets
desc dvfinfval
replace totcsc = 0 if (totcsc>=.)
gen assts = dvffassets + dvfinfval - totcsc - tothp - tot_los - totmo
egen assets = sum(assts), by (case bu)
label var assets "net value of financial and non-financial (non-property) assets"

/* pension assets */
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
gen pi_temp = pincinp * (pincinp>0.01)
egen pinc_now = sum(pi_temp), by (case bu)

/* welfare benefits */
egen benefits = sum(dvbenefitannual_i), by (case bu)

/* partner labour force status, earnings income, DB and pension contributions */
save "$workingDir/tempWAS.dta", replace
keep case bu person_id person partno dvage17 edage ue fti pti sempi nempi earnings non_cp op_elig op_memb op_db op_dc pcr healths dlltsd pincinp grad emp
drop if (partno==17)

duplicates tag case partno, gen(dup)
egen dup_hh=max(dup), by(case)
*	tab dup_hh
drop if dup_hh>0
drop dup dup_hh

foreach xx in dvage17 ue fti pti sempi nempi earnings non_cp op_elig op_memb op_db op_dc pcr healths dlltsd pincinp grad emp { /* Add partner prefix */
	rename `xx' p_`xx'
}
drop person
rename partno person

merge 1:1 case person using "$workingDir/tempWAS.dta"
tab partno _m
drop if _m==1
drop _m
foreach X in ue fti pti sempi nempi earnings non_cp op_elig op_memb op_db op_dc pcr { /* Add partner prefix */
	replace p_`X' = 0 if (p_`X' >=.)
}
replace p_healths = 1 if (p_healths>=.)

/* allocate main home to benefit unit of household reference person */
replace dvhvalue = 0 if (hrp_bu==0)
replace main_mort = 0 if (hrp_bu==0)
save "$workingDir/tempWAS.dta", replace


/**************************************************************/
*
*	state pensions 
*
/**************************************************************/
* note that the WAS does not report benefit values separately
* the approach adopted here consequently reflects an upper bound on 
* the value of state retirement pensions received
gen id_rp = 0
forval ii = 1/3 {

	replace id_rp = 1 if (penben`ii'_i==11)
}
gen spi = 0
replace spi = min( dvbenefitannual_i / 52.0, 159.65 ) if (id_rp == 1)
egen spen_fam = sum(spi), by (case bu)


/**************************************************************/
*
*	final variable adjustments to match to model
*
/**************************************************************/
gen w = assets + oprop - isa_fam - bus_assets
replace w = w + dvhvalue if ( dvhvalue<.)
replace w = w - main_mort if ( main_mort<.)
gen wo = 1 - ue
gen wo2 = 1 - p_ue
gen tot_pp = tot_pen - tot_open
gen pcr_s = 1 + (op_elig - op_db) + 2*(op_db)
gen semp = (sempi + p_sempi)>0
drop sempi p_sempi


/**************************************************************/
*
*	Save compiled Stata dataset
*
/**************************************************************/

/* Prepare dataset for saving */
keep  case person_id bu sex grad dvage17 edage mari na nk* fti pti ue nempi earnings non_cp op_elig op_memb op_db spen_fam ///
	  op_dc pcr p_fti p_pti p_ue p_nempi p_earnings p_non_cp p_op_elig p_op_memb p_op_db p_op_dc p_pcr bus_assets ///
	  oprop assets tot_open tot_pen dvhvalue main_mort student pinc_now isa_fam w wo wo2 tot_pp pcr_s semp gor wkg_grs ///
	  healths p_healths dlltsd p_dlltsd emp p_emp p_grad wkg_emp wkg_pen wkg_ben benefits pincinp p_pincinp inc year month xs_wgt
order case person_id bu sex grad dvage17 edage mari na nk* fti pti ue nempi earnings non_cp op_elig op_memb op_db ///
	  op_dc pcr p_fti p_pti p_ue p_nempi p_earnings p_non_cp p_op_elig p_op_memb p_op_db p_op_dc p_pcr bus_assets ///
	  oprop assets tot_open tot_pen dvhvalue main_mort student pinc_now isa_fam w wo wo2 tot_pp pcr_s semp gor wkg_grs ///
	  healths p_healths dlltsd p_dlltsd emp p_emp p_grad wkg_emp wkg_pen wkg_ben benefits pincinp p_pincinp inc year month xs_wgt

compress
save "$workingDir/tempWAS.dta", replace


/**************************************************************/
*
*	adjust sample to align with ONS population estimates, and to 
*	proxy for the population in Northern Ireland
*
/**************************************************************/
*global workingDir "c:/myfiles/MODEL_LAB/MODEL"
use "$workingDir/tempWAS.dta", clear

/* align sample size to mid-2017 estimate for GB population size reported by the ONS */
global ONS_GBpop = 64169400
sort bu
gen xs_tmp = xs_wgt
replace xs_tmp = 0 if (bu==bu[_n-1])
gen psns = na + nk
gen wgt_rnd = round(xs_tmp*10000)
sum psns [fweight=wgt_rnd]
global adj = $ONS_GBpop / (r(mean) * r(N) / 10000)
replace xs_wgt = xs_wgt * $adj
drop xs_tmp wgt_rnd

/*	CHECK*/
gen wgt_rnd = round(xs_wgt*10000) * (bu~=bu[_n-1])
sum psn [fweight=wgt_rnd]
local gg = r(mean) * r(N) / 10000
display "target: $ONS_GBpop value: `gg'"
drop wgt_rnd


/**************************************************************/
*
*	clean up data for model
*
/**************************************************************/
sort person_id

/* define sample and base model directory */
*global basedir "$workingDir/base_files/UK2011_all"
drop if (dvage17<5)
*drop if (semp>0 | bus_assets>0)
*replace semp = semp + (age>50)*(earnings<1.0)
*replace semp = (semp>0)
*drop if (semp==0)
*keep if ( grad>0 | student>0 )
*keep if (semp>0)

/* generate dummy variables for data that are not loaded in for the UK */
gen not_id = 0			/* not identified as public sector employee, private sector employee, self-employed, or unemployed	*/
gen db_pen = 0			/* accrued db pension rights as a fraction of final salary - final salary terms defined in job file pen_par(46:48) */

/* variables for tax_test2 */
/*
gen emp1 = 2 * fti + pti
gen emp2 = 2 * p_fti + p_pti
replace pincinp = 0 if (pincinp >= .)
replace p_pincinp = 0 if (p_pincinp >= .)
keep age na nk emp1 emp2 earnings p_earnings pincinp p_pincinp w
export excel using "$workingDir/analysis_files/tax_test2_temp", firstrow(variables) replace
*/

/* order variables for transfer to LINDA */
keep person_id bu dvage17 edage na sex gor w earnings p_earnings xs_wgt wo wo2 ///
not_id nempi p_nempi tot_open tot_pp spen_fam student grad pcr_s op_memb isa_fam bus_assets semp ///
db_pen healths p_healths dlltsd p_dlltsd emp p_emp nk* p_grad inc year month

order person_id bu dvage17 edage na sex gor w earnings p_earnings xs_wgt wo wo2 ///
not_id nempi p_nempi tot_open tot_pp spen_fam student grad p_grad ///
pcr_s op_memb isa_fam bus_assets semp db_pen healths p_healths dlltsd p_dlltsd ///
emp p_emp nk* inc year month

/* add final labels */
label var person_id "person number"
label var dvage17 "age of person"
label var sex "gender"
label var gor "government office region"
label var w "net value of all assets excluding isas, own-businesses, and pension wealth"
label var earnings "gross earnings"
label var p_earnings "partner gross earnings"
label var xs_wgt "cross-sectional weight"
label var wo "wage offer of reference person"
label var wo2 "partner wage offer"
label var not_id "whether labour status identified at load - dummy variable"
label var nempi "whether not employed in cross-section"
label var p_nempi "whether partner not employed in cross-section"
label var tot_open "occupational pension wealth"
label var tot_pp "private pension wealth"
label var spen_fam "state pension"
label var student "whether student in cross-section"
label var grad "whether tertiary graduate"
label var pcr_s "discrete pension contribution state"
label var semp "whether self-employed"
label var db_pen "value of defined benefit pension"
label var p_healths "partner health status: 1=good, 2=limited, 3=disabled standard, 4=disabled enhanced"
label var healths "health status: 1=good, 2=limited, 3=disabled standard, 4=disabled enhanced"
label var p_dlltsd "partner longstanding illness or disability"
label var dlltsd "longstanding illness or disability"
label var inc "benefit unit non-benefit private income"
label var emp "employed"
label var p_emp "spouse employed"
label var year "survey year"
label var month "survey month"

save "$workingDir/tempWAS.dta", replace
	

/**************************************************************/
*
*	ALIGN VARIABLE DEFINITIONS
*
/**************************************************************/
use "$workingDir/tempWAS.dta", clear

gen dwt = round(xs_wgt*10,1)
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
gen dhesp2 = 0
replace dhesp2=1 if (p_healths<2)
replace dhesp2=2 if (p_healths==2)
replace dhesp2=3 if (p_healths==3)
replace dhesp2=4 if (p_healths>3)
rename gor gor2
rename p_grad gradsp
rename p_emp empsp
rename p_dlltsd dlltsdsp
gen single_woman = (na==1) * (sex==2)
gen single_man = (na==1) * (sex==1)
gen couple = (na==2)
gen single = (na==1)

/* wealth, omitting value of state pension rights */
gen wealth = w + isa_fam + bus_assets + tot_open + tot_pp
replace wealth = wealth / cpi[2016 - $cpi_minyear + 1,1] if (year==2016 | ((year==2017)&(month<4)))
replace wealth = wealth / cpi[2017 - $cpi_minyear + 1,1] if (((year==2017)&(month>3)) | ((year==2018)&(month<4)))
replace wealth = wealth / cpi[2018 - $cpi_minyear + 1,1] if (((year==2018)&(month>3)) | ((year==2019)&(month<4)))
gen wealth1 = asinh(wealth)

/* regression variables */
gen idnk04 = (nk04>0)
gen dhe2grad = dhe2 * grad
gen dhe2ngrad = dhe2 * (1-grad)
gen dlltsdgrad = dlltsd * grad
gen dlltsdngrad = dlltsd * (1-grad)
gen empage = emp * dvage17

gen was = 1

gen couple_ref = couple * (sex==1)
sort bu
by bu: egen chk2 = sum(couple_ref)
replace couple_ref = 0 if (chk2>1)
replace couple_ref = couple if (couple_ref==0 & chk2==0)
drop chk2

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

* limit to reference population
gen chk1 = (single_man + single_woman + couple_ref)
drop if (chk1==0)
drop chk1
sort bu
gen chk1 = 0
replace chk1 = 1 if (bu==bu[_n-1])
drop if (chk1==1)

* save control data
save "$workingDir/tempWAS2.dta", replace
	

/**************************************************************/
*
*	ANALYSIS OF WORKING VARIABLES
*
/**************************************************************/
/*
use "$workingDir/tempWAS2.dta", clear

tab gor [fweight=dwt], nol
tab dvage17 [fweight=dwt], nol
tab sex [fweight=dwt], nol
gen dwt2 = round(dwt/na, 1)
tab na [fweight=dwt2]
tab nk [fweight=dwt2]
tab nk04 [fweight=dwt2]
tab healths [fweight=dwt]
tab dhe2 [fweight=dwt]
tab grad [fweight=dwt]
tab dlltsd [fweight=dwt]
tab emp [fweight=dwt]

gen chk = (inc<0.1)
tab chk [fweight=dwt2]
sum inc [fweight=dwt2] if (chk==0)

sum wealth [fweight=dwt2], detail
sum wealth1 [fweight=dwt2], detail

sum wealth [fweight=dwt2] if (single_woman), detail
sum wealth1 [fweight=dwt2] if (single_woman), detail

sum wealth [fweight=dwt2] if (single_man), detail
sum wealth1 [fweight=dwt2] if (single_man), detail

sum wealth [fweight=dwt2] if (single), detail
sum wealth1 [fweight=dwt2] if (single), detail

sum wealth [fweight=dwt2] if (couple), detail
sum wealth1 [fweight=dwt2] if (couple), detail
*/


/**************************************************************************************
*	clean-up
**************************************************************************************/
rm "tempWAS.dta"



/**************************************************************/
*
*	END 
*
/**************************************************************/



