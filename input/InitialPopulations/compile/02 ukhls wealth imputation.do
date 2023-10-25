/**********************************************************************
*
*	MANAGES IMPUTATION OF WEALTH DATA FROM WAS TO UKHLS FOR SIMPATHS INPUT DATA
*
*	AUTH: Justin van de Ven (JV)
*	LAST EDIT: 09/09/2023 (JV)
*
**********************************************************************/


/********************************************************************************
	local data directories - commented out when using master program
********************************************************************************/
/*
* define seed to ensure replicatability of results
global seedBase = 3141592
global seedAdjust = 0

* folder to store working data
global workingDir "C:\MyFiles\00 CURRENT\03 PROJECTS\Essex\SimPaths\02 PARAMETERISE\STARTING DATA\data"

* year to impute wealth for
global yearWealth = 2017

* prepare was data
global WASDoFile "C:\MyFiles\00 CURRENT\03 PROJECTS\Essex\SimPaths\02 PARAMETERISE\STARTING DATA\progs\02b was wealth data.do"

* dataset to impute wealth to *
global imputeWealthToDataset "population_initial_UK_$yearWealth"
global imputeWealthToDataset "preWealthTraining"
*/


/**********************************************************************
*	start analysis
**********************************************************************/
cd "$workingDir"
disp "imputing wealth to UKHLS data"
//import delimited "$workingDir\\population_initial_UK_$yearWealth.csv", clear
use $imputeWealthToDataset, clear
sort idperson
drop liquid_wealth


/**********************************************************************
*	align variable definitions
**********************************************************************/
gen dvage17 = 0
forval ii = 1/16 {
	
	replace dvage17 = `ii' if (dag>=5*(`ii'-1) & dag<5*`ii')
}
replace dvage17 = 17 if (dag>79)
gen gor = drgn1
/* Northern Ireland recoded to East Midlands for imputing wealth. This reflects
the omission of Northern Ireland from the WAS, and the observation that the 
GDP per capita of NI (25981) was closest to that of East Midlands (25946) in 
2018, as reported by Office for National Statistics - Regional economic activity 
by gross domestic product, UK: 1998 to 2018 */
gen gor2 = gor
replace gor2 = 5 if (gor==13)
gen sex = 1
replace sex=2 if (dgn==0)
gen nk = dnc
gen na = 1
replace na = 2 if (idpartner>0)

/*
dhe2 = 1 very good / excellent - reference
dhe2 = 2 good
dhe2 = 3 fair
dhe2 = 4 poor
*/
gen dhe2=0
replace dhe2=1 if (dhe>3)
replace dhe2=2 if (dhe==3)
replace dhe2=3 if (dhe==2)
replace dhe2=4 if (dhe==1)
gen dhesp2=0
replace dhesp2=4 if (dhesp>3)
replace dhesp2=3 if (dhesp==3)
replace dhesp2=2 if (dhesp==2)
replace dhesp2=1 if (dhesp==1)
gen grad = 0
replace grad=1 if (deh_c3==1)
gen gradsp = 0
replace gradsp=1 if (dehsp_c3==1)
gen emp = (les_c3==1)
gen empsp = (lessp_c3==1)

gen inci = sinh(ypnbihs_dv)
sort idbenefitunit
by idbenefitunit: egen inc = sum(inci)
replace inc = inc * 12

gen nk04i = (dag<5)
by idbenefitunit: egen nk04 = sum(nk04i)

/* regression variables */
gen idnk04 = (nk04>0)
gen dhe2grad = dhe2 * grad
gen dhe2ngrad = dhe2 * (1-grad)
gen dlltsdgrad = dlltsd * grad
gen dlltsdngrad = dlltsd * (1-grad)
gen empage = emp * dvage17
gen single_woman = (na==1) * (sex==2) * (dag>17)
gen single_man = (na==1) * (sex==1) * (dag>17)
gen couple = (na==2)
gen single = (na==1)
local seed = $seedBase + $seedAdjust
global seedAdjust = $seedAdjust + 1
set seed `seed'
gen ee = rnormal()
gen ee2 = runiform()

gen was = 0

gen long bu = idbenefitunit

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


/**********************************************************************
*	save working data
**********************************************************************/
save "$workingDir\\ukhls_wealthtemp.dta", replace


/**********************************************************************
*	analyse sample
**********************************************************************/
/*
use "$workingDir\\ukhls_wealthtemp.dta", clear
drop if (dvage17<5)

tab gor2 [fweight=dwt]
tab dvage17 [fweight=dwt]
tab sex [fweight=dwt]
gen dwt2 = round(dwt/na, 1)
tab na [fweight=dwt2]
tab nk [fweight=dwt2]
tab nk04 [fweight=dwt2]
tab dhe [fweight=dwt]
tab dhe2 [fweight=dwt]
tab grad [fweight=dwt]
tab dlltsd [fweight=dwt]
tab les_c4 [fweight=dwt]
tab emp [fweight=dwt]

gen chk = (inc<0.01)
tab chk [fweight=dwt2]
sum inc [fweight=dwt2] if (chk==0)

tab dhe [fweight=dwt] if (dag>17)
tab dhesp [fweight=dwt] if (dag>17 & dhesp>0)
tab dcpst [fweight=dwt] if (dag>17)
tab ded [fweight=dwt] if (dag>17)
tab deh_c3 [fweight=dwt] if (dag>17 & deh_c3>0)
tab deh_c3 dehsp_c3 [fweight=dwt] if (dag>17 & deh_c3>0 & dehsp_c3>0)
tab dlltsd [fweight=dwt] if (dag>17)
tab dhe dlltsd [fweight=dwt] if (dag>17)
tab les_c4 [fweight=dwt] if (dag>17)
sum ypnbihs_dv [fweight=dwt] if (dag>17)
*/


/**********************************************************************
*	prepare was data
**********************************************************************/
do "$WASDoFile"


/**********************************************************************
*	coarse exact matching
**********************************************************************/

* identify non-reference population and save for retrieval
use "$workingDir\\ukhls_wealthtemp.dta", clear
gen treat = (single_woman + single_man + couple_ref)
gsort bu -treat
by bu: egen chk = sum(treat)
tab chk
replace treat = 0 if (bu==bu[_n-1])
keep if (treat==0)
save "$workingDir\\ukhls_wealthtemp2.dta", replace

* identify reference population and append WAS data
use "$workingDir\\ukhls_wealthtemp.dta", clear
gen treat = (single_woman + single_man + couple_ref)
gsort bu -treat
replace treat = 0 if (bu==bu[_n-1])
keep if (treat == 1)
append using "$workingDir/tempWAS2.dta"
gen chk = 0
sort bu
replace chk = 1 if (bu==bu[_n-1])
keep if (chk == 0)
drop chk
recode treat dlltsdsp gradsp empsp (mis=0)

* adjust matching variables
gen tt = 0
replace tt = 1 if (single_woman)
replace tt = 2 if (single_man)
replace tt = 3 if (na==2)

/*
dhe3 = 1 good
dhe3 = 2 fair
dhe3 = 3 poor
*/
gen dhe3 = 0
replace dhe3 = 1 if (dhe2<3)
replace dhe3 = 2 if (dhe2==3)
replace dhe3 = 3 if (dhe2==4)
gen dhe4 = dhe3
replace dhe4 = 2 if dhe4==3

/*
dvage07: 
1 = under 30
2 = 30-39
3 = 40-49
4 = 50-59
5 = 60-69
6 = 70-79
7 = 80+
*/
gen dvage07 = 0
replace dvage07 = 1 if (dvage17<7)
replace dvage07 = 2 if (dvage17==7 | dvage17==8)
replace dvage07 = 3 if (dvage17==9 | dvage17==10)
replace dvage07 = 4 if (dvage17==11 | dvage17==12)
replace dvage07 = 5 if (dvage17==13 | dvage17==14)
replace dvage07 = 6 if (dvage17==15 | dvage17==16)
replace dvage07 = 7 if (dvage17==17)

/*
dnk2
0, 1, 2, 3+
*/
gen nk2 = nk
replace nk2 = 3 if (nk2>3)
gen nk3 = (nk2>0)

/*
gor3:
1: North East and West Midlands 
2: Yorkshire and North West
3: Scotland, East Midlands, Northern Ireland, Wales 
4: London, East of England
5: South West,
6: South East
*/
gen gor3 = 0
replace gor3 = 1 if (gor2==1 | gor2==6)
replace gor3 = 2 if (gor2==4 | gor2==2)
replace gor3 = 3 if (gor2==5 | gor2==11 | gor2==12 | gor2==13)
replace gor3 = 4 if (gor2==8 | gor2==7)
replace gor3 = 5 if (gor2==10)
replace gor3 = 6 if (gor2==9)
gen gor4 = gor3
replace gor4 = 5 if (gor4==6)

gen pct2 = 0
replace pct2 = 1 if (pct<3)
replace pct2 = 2 if (pct==3 | pct==4)
replace pct2 = 3 if (pct==5 | pct==6)
replace pct2 = 4 if (pct==7 | pct==8)
replace pct2 = 5 if (pct>8)

sort idhh bu

gen smp = 0
gen rnk = 0
gen mtc = 0
gen wealthi = -9
qui {
	sum treat, mean
	local nn = r(mean) * r(N)
}
forval kk = 1/`nn' {

	* local kk = 6
	qui {
		gen chk = 1-treat
		local rnk = 1
		foreach vv in tt grad gor3 dhe3 idnk04 nk2 dvage07 pct emp empsp gradsp {
			replace chk = 0 if (`vv'!=`vv'[`kk'])
		}
		sum chk, mean
	}
	if (r(mean)==0) {

		qui {
			drop chk
			gen chk = 1-treat
			local rnk = 2
			foreach vv in tt grad gor3 nk3 dvage07 pct2 {
				replace chk = 0 if (`vv'!=`vv'[`kk'])
			}
			sum chk, mean
		}
		if (r(mean)==0) {
			
			qui {
				drop chk
				gen chk = 1-treat
				local rnk = 3
				foreach vv in tt grad gor4 dvage07 pct2 {
					replace chk = 0 if (`vv'!=`vv'[`kk'])
				}
				sum chk, mean
			}
			if (r(mean)==0) {
				disp "failed to find match"
				local rnk = 4
			}
		}
	}
	qui {
		replace smp = r(mean)*r(N) if (_n==`kk')
		if (r(mean)>0) {
			local ee = ee2[`kk']
			preserve
			keep if (chk==1)
			if (r(mean)*r(N)>1) {
				* multiple matches - select random observation
				
				sum dwt
				gen smp_cdf = 0
				replace smp_cdf = dwt / r(sum) if (_n==1)
				replace smp_cdf = smp_cdf[_n-1] + dwt / r(sum) if (_n>1)
				gen switch = (smp_cdf>`ee')
				gen slct = 0
				disp `ee'
				replace slct = 1 if (switch==1 & _n==1)
				replace slct = 1 if (switch > switch[_n-1])
				keep if slct==1
			}
			local mtc = bu[1]
			local ww = wealth[1]
			restore
			replace mtc=`mtc' if (_n==`kk')
			replace wealthi = `ww' if (_n==`kk')
		}
		replace rnk=`rnk' if (_n==`kk')
		drop chk
	}
	if (mod(`kk',100)==0) disp "matched to observation `kk'"
}
keep if (treat)


/**********************************************************************
*	analyse imputations
**********************************************************************/

/*
preserve
drop if (rnk==4)
gen wealth5 = asinh(wealthi)

gen dwt2 = round(dwt/na, 1)
sum wealth5 [fweight=dwt2] if (single_woman & dvage17>4), detail
sum wealth5 [fweight=dwt2] if (single_man & dvage17>4), detail
sum wealth5 [fweight=dwt2] if (single & dvage17>4), detail
sum wealth5 [fweight=dwt2] if (couple & dvage17>4), detail

*sum wealth [fweight=dwt2] if (single_woman & dvage17>4), detail
*sum wealth [fweight=dwt2] if (single_man & dvage17>4), detail
*sum wealth [fweight=dwt2] if (single & dvage17>4), detail
*sum wealth [fweight=dwt2] if (couple & dvage17>4), detail
restore
*/

/**********************************************************************
*	append non-reference population
**********************************************************************/
append using "$workingDir\\ukhls_wealthtemp2.dta"
sort bu
recode wealthi (mis=0)
by bu: egen liquid_wealth = sum(wealthi)
save ukhls_wealthtemp3, replace

/**********************************************************************
*	clean data and save
**********************************************************************/
use ukhls_wealthtemp3, clear
drop dvage17 gor gor2 sex nk na dhe2 dhesp2 grad gradsp emp empsp inci inc nk04i nk04 idnk04 dhe2grad dhe2ngrad dlltsdgrad dlltsdngrad empage single_woman single_man couple single ee ee2 was bu couple_ref pct treat person_id edage w earnings p_earnings xs_wgt wo wo2 not_id nempi p_nempi tot_open tot_pp spen_fam student pcr_s op_memb isa_fam bus_assets semp db_pen healths p_healths dlltsdsp wealth wealth1 chk1 tt dhe3 dhe4 dvage07 nk2 nk3 gor3 gor4 pct2 wealthi chk year month
label var rnk "matching level: 1 = most fine, 2, 3 = most coarse, 4=no match"
label var smp "matching sample - number of matched candidates to choose from"
label var mtc "benefit unit id (bu) of matched observation"
label var liquid_wealth "total wealth including housing, business and private (personal and occupational) pensions" 
save "$workingDir\\$imputeWealthToDataset", replace

drop rnk smp mtc
sort idperson
export delimited using "$workingDir/input data\\$imputeWealthToDataset.csv", nolabel replace


/**************************************************************************************
*	clean-up
**************************************************************************************/
rm "tempWAS.dta"
rm "ukhls_wealthtemp.dta"
rm "ukhls_wealthtemp2.dta"
rm "ukhls_wealthtemp3.dta"

