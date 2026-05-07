/*********************************************************************
 MASTER VARIABLE CONSTRUCTION AND TRANSFORMATIONS DO-FILE
*********************************************************************/

*==================================================
* Ensure missing is coded as missing
*==================================================
foreach var in idhh idperson idpartner idfather idmother dct drgn1 dwt dnc02 dnc dgn dgnsp dag dagsq dhe dhesp dcpst ///
    ded deh_c3 deh_c4 der dehsp_c3 dehsp_c4 dehm_c3 dehf_c3 dehmf_c3 dcpen dcpyy dcpex ///
    dlltsd dlltsd01 dlrtrd drtren dlftphm dhhtp_c4 dhhtp_c8 dhm dhm_ghq ///
    jbhrs jshrs j2hrs jbstat les_c3 les_c4 lessp_c3 lessp_c4 lesdf_c4 ydses_c5 scghq2_dv ///
    ypnbihs_dv yptciihs_dv yplgrs_dv swv sedex ssscp sprfm sedag stm dagsp lhw l1_lhw ///
    pno ppno hgbioad1 hgbioad2 der obs_earnings_hourly l1_obs_earnings_hourly ///
    dhh_owned econ_benefits econ_benefits_nonuc econ_benefits_uc ///
    scghq2_dv_miss_flag dchpd dagpns dagpns_sp CPI lesnr_c2 dlltsd01 dlltsd_sp dlltsd01_sp ///
    ypnoab flag* dhe_mcs dhe_pcs dhe_mcssp dhe_pcssp dls dot dot01 unemp new_rel {
        qui recode `var' (-9/-1=.)
}

* Set data
xtset idperson swv
sort idperson swv

*==================================================
* Student flag
*==================================================
cap drop Dst
gen Dst = .
replace Dst = 0 if les_c3 != 2
replace Dst = 1 if les_c3 == 2
replace Dst = . if les_c3 == .

*==================================================
* Age transformations
*==================================================

gen Dag    = dag
gen Dag_sq = dagsq
gen Age = dag
gen AgeSquared = dag^2

gen Dag_c    = dag - 23
gen Dag_c_sq = Dag_c^2

gen Dag_post25 = (dag > 25) * (dag - 25)

gen Dag_post21    = (dag > 21) * (dag - 21)
gen Dag_post18_sq = (dag > 18) * (dag - 18)^2
gen Dag_post21_sq = (dag > 21) * (dag - 21)^2
gen Dag_post25_sq = (dag > 25) * (dag - 25)^2
gen Dag_post26_sq = (dag > 26) * (dag - 26)^2

mkspline rcs = dag, cubic knots(18 21 23 26)

*==================================================
* Time transformations
*==================================================

replace stm = stm - 2000

foreach y of numlist 11/25 {
    gen y20`y' = (stm == `y')
}

foreach y of numlist 2011/2025 {
    gen Y`y' = y`y'
}

gen year_post2020 = (stm > 20) * (stm - 20)
gen Y2223 = inlist(stm, 22, 23)
gen Year_transformed = stm

gen Year = stm
gen YearSquared = stm^2

*==================================================
* Income variables
*==================================================

gen receives_ypncp = ypncp > 0 & !missing(ypncp) //capital income

gen receives_ypnoab = ypnoab > 0 & !missing(ypnoab) //private pension income 

recode ynbcpdf_dv (-999 = .)

tab ydses_c5 , gen(Ydses_c5_Q)

*==================================================
* Education recoding
*==================================================
cap drop deh_c3_recoded
recode deh_c3 (1 = 3) (3 = 1), gen(deh_c3_recoded)

cap lab define deh_c3_recoded   1 "Low" 2 "Medium" 3 "High"
label values deh_c3_recoded deh_c3_recoded


*==================================================
* Region dummies
*==================================================

gen Dgn = dgn
tab drgn1, gen(UK)

rename UK1  UKC
rename UK2  UKD
rename UK3  UKE
rename UK4  UKF
rename UK5  UKG
rename UK6  UKH
rename UK7  UKI
rename UK8  UKJ
rename UK9  UKK
rename UK10 UKL
rename UK11 UKM
rename UK12 UKN

*==================================================
* Employment dummies
*==================================================

tab les_c3, gen(Les_c3_)
rename Les_c3_1 Les_c3_Employed
rename Les_c3_2 Les_c3_Student
rename Les_c3_3 Les_c3_NotEmployed

tab lessp_c3, gen(Lessp_c3_)
rename Lessp_c3_1 Lessp_c3_Employed
rename Lessp_c3_2 Lessp_c3_Student
rename Lessp_c3_3 Lessp_c3_NotEmployed

tab les_c4, gen(Les_c4_)
rename Les_c4_1 Les_c4_Employed
rename Les_c4_2 Les_c4_Student
rename Les_c4_3 Les_c4_NotEmployed
rename Les_c4_4 Les_c4_Retired

tab lesdf_c4, gen(Lesdf_c4_)
rename Lesdf_c4_1 Lesdf_c4_BothEmployed
rename Lesdf_c4_2 Lesdf_c4_EmpSpouseNotEmp
rename Lesdf_c4_3 Lesdf_c4_NotEmpSpouseEmp
rename Lesdf_c4_4 Lesdf_c4_BothNotEmployed

*==================================================
* Education dummies
*==================================================

tab deh_c3, gen(Deh_c3_)
rename Deh_c3_1 Deh_c3_High
rename Deh_c3_2 Deh_c3_Medium
rename Deh_c3_3 Deh_c3_Low

tab deh_c4, gen(Deh_c4_)
rename Deh_c4_1 Deh_c4_Na
rename Deh_c4_2 Deh_c4_High
rename Deh_c4_3 Deh_c4_Medium
rename Deh_c4_4 Deh_c4_Low

tab dehmf_c3, gen(Dehmf_c3_)
rename Dehmf_c3_1 Dehmf_c3_High
rename Dehmf_c3_2 Dehmf_c3_Medium
rename Dehmf_c3_3 Dehmf_c3_Low

tab dehsp_c3, gen(Dehsp_c3_)
rename Dehsp_c3_1 Dehsp_c3_High
rename Dehsp_c3_2 Dehsp_c3_Medium
rename Dehsp_c3_3 Dehsp_c3_Low

*==================================================
* Health dummies
*==================================================
cap lab define dhe 1 "Poor" 2 "Fair" 3 "Good" 4 "VeryGood" 5 "Excellent" , modify 
lab values dhe dhe 

tab dhe, gen(Dhe_)
rename Dhe_1 Dhe_Poor
rename Dhe_2 Dhe_Fair
rename Dhe_3 Dhe_Good
rename Dhe_4 Dhe_VeryGood
rename Dhe_5 Dhe_Excellent

tab dhesp, gen(Dhesp_)
rename Dhesp_1 Dhesp_Poor
rename Dhesp_2 Dhesp_Fair
rename Dhesp_3 Dhesp_Good
rename Dhesp_4 Dhesp_VeryGood
rename Dhesp_5 Dhesp_Excellent

gen Dhe_pcs   = dhe_pcs
gen Dhe_mcs   = dhe_mcs
gen Dhe_pcssp = dhe_pcssp
gen Dhe_mcssp = dhe_mcssp

*==================================================
* Long-term sick or disabled
*==================================================

gen Dlltsd   = dlltsd
gen Dlltsd01 = dlltsd01


*==================================================
* Ethnicity
*==================================================

tab dot, gen(dot_)
rename dot_1 Ethn_White
rename dot_2 Ethn_Asian
rename dot_3 Ethn_Black
rename dot_4 Ethn_Other

*==================================================
* Household type and relationship dynamics
*==================================================

tab dhhtp_c4, gen(Dhhtp_c4_)
rename Dhhtp_c4_1 Dhhtp_c4_CoupleNoChildren
rename Dhhtp_c4_2 Dhhtp_c4_CoupleChildren
rename Dhhtp_c4_3 Dhhtp_c4_SingleNoChildren
rename Dhhtp_c4_4 Dhhtp_c4_SingleChildren

tab dhhtp_c8, gen(Dhhtp_c8_)

gen New_rel = new_rel

tab dcpst, gen(Dcpst_)
rename Dcpst_1 Dcpst_Partnered
rename Dcpst_2 Dcpst_Single
gen Partnered = Dcpst_Partnered
gen Single = Dcpst_Single

*==================================================
* Explicit lags
*==================================================
cap drop l_* L_*

xtset idperson swv

foreach v in ydses_c5 dhe dhe_pcs dhe_mcs les_c3 les_c4 dhhtp_c4 dlltsd01 need_socare dehmf_c3 {
    gen l_`v' = L.`v'
}

gen L_Ydses_c5 = l_ydses_c5
gen L_Dhe      = l_dhe
gen L_Dhe_pcs  = l_dhe_pcs
gen L_Dhe_mcs  = l_dhe_mcs
gen L_Dlltsd01 = l_dlltsd01
gen L_Need_socare = l_need_socare
gen L_Dehmf_c3 = l_dehmf_c3

tab l_les_c4, gen(L_Les_c4_)
rename L_Les_c4_1 L_Les_c4_Employed
rename L_Les_c4_2 L_Les_c4_Student
rename L_Les_c4_3 L_Les_c4_NotEmployed
rename L_Les_c4_4 L_Les_c4_Retired

tab l_ydses_c5, gen(L_Ydses_c5_Q)

tab l_dhhtp_c4, gen(L_Dhhtp_c4_)
rename L_Dhhtp_c4_1 L_Dhhtp_c4_CoupleNoChildren
rename L_Dhhtp_c4_2 L_Dhhtp_c4_CoupleChildren
rename L_Dhhtp_c4_3 L_Dhhtp_c4_SingleNoChildren
rename L_Dhhtp_c4_4 L_Dhhtp_c4_SingleChildren

tab l_dehmf_c3, gen(L_Dehmf_c3_)
rename L_Dehmf_c3_1 L_Dehmf_c3_High
rename L_Dehmf_c3_2 L_Dehmf_c3_Medium
rename L_Dehmf_c3_3 L_Dehmf_c3_Low



*==================================================
* Copy variables for nice labels 
*==================================================
gen Dnc       = dnc
gen Dnc02     = dnc02
gen Ded       = ded
gen Dhe       = dhe
gen Ydses_c5  = ydses_c5
gen Dcpyy     = dcpyy
gen Dcpagdf   = dcpagdf
gen fertilityRate = dukfr
gen Dhh_owned = dhh_owned

gen Elig_pen    = dagpns
gen Elig_pen_L1 = l.dagpns

gen Reached_Retirement_Age    = dagpns
gen Reached_Retirement_Age_Sp = dagpns_sp

gen Dlltsdsp   = dlltsd_sp
gen Dlltsd01sp = dlltsd01_sp

gen Ypncp = ypncp
gen Ypnoab =  ypnoab
gen Yplgrs_dv = yplgrs_dv
gen Ypnbihs_dv = ypnbihs_dv
gen Ypnbihs_dv_sq = ypnbihs_dv^2
gen Ynbcpdf_dv   = ynbcpdf_dv
gen Yptciihs_dv  = yptciihs_dv


*==================================================
* Interactions
*==================================================
cap drop Les_c4_*_Dgn Ded_* Reached_Retirement_Age_Les

gen Les_c4_Student_Dgn     = Dgn * Les_c4_Student
gen Les_c4_NotEmployed_Dgn = Dgn * Les_c4_NotEmployed
gen Les_c4_Retired_Dgn     = Dgn * Les_c4_Retired

gen Ded_Dag    = Ded * Dag
gen Ded_Dag_sq = Ded * Dag_sq
gen Ded_Dgn    = Ded * Dgn

gen Ded_Dnc_L1_  = Ded * l.Dnc
gen Ded_Dnc02_L1 = Ded * l.Dnc02

forvalues i = 1/5 {
    gen Ded_Ydses_c5_Q`i'_L1 = Ded * l.Ydses_c5_Q`i'
}

gen Ded_Dehsp_c3_Medium_L1 = l.Dehsp_c3_Medium * Ded
gen Ded_Dehsp_c3_Low_L1    = l.Dehsp_c3_Low * Ded

gen Ded_Dhesp_Good_L1 = l.Dhesp_Good * Ded
gen Ded_Dhesp_Fair_L1 = l.Dhesp_Fair * Ded

gen Ded_Dhe_Fair_L1      = l.Dhe_Fair * Ded
gen Ded_Dhe_Good_L1      = l.Dhe_Good * Ded
gen Ded_Dhe_VeryGood_L1  = l.Dhe_VeryGood * Ded
gen Ded_Dhe_Excellent_L1 = l.Dhe_Excellent * Ded

gen Ded_Dhe_pcs = Ded * Dhe_pcs
gen Ded_Dhe_mcs = Ded * Dhe_mcs

gen Ded_Dhe             = Dhe * Ded
gen Ded_Dcpst_Single    = Dcpst_Single * Ded
gen Ded_Dcpst_Single_L1 = l.Dcpst_Single * Ded

gen Reached_Retirement_Age_Les = Reached_Retirement_Age * l.Les_c3_NotEmployed

gen Ded_Ypncp= Ded * Ypncp
gen Ded_Yplgrs_dv = Ded *Yplgrs_dv


*==================================================
* Prepare data for social care regressions 
*==================================================
* Care received variables (Processes S2)

rename need_socare need_care

gen receive_formal_care = formal_socare_hrs > 0
gen receive_informal_care = (partner_socare_hrs + daughter_socare_hrs + son_socare_hrs + other_socare_hrs) > 0
gen receive_care = max(receive_informal_care, receive_formal_care)

gen CareMarket = .
replace CareMarket = 1 if (receive_informal_care == 0 & receive_formal_care == 0)
replace CareMarket = 2 if (receive_informal_care == 1 & receive_formal_care == 0)
replace CareMarket = 3 if (receive_informal_care == 1 & receive_formal_care == 1)
replace CareMarket = 4 if (receive_informal_care == 0 & receive_formal_care == 1)

lab def labCareMarket 1 "None" 2 "Informal" 3 "Mixed" 4 "Formal"
lab val CareMarket labCareMarket

gen HrsReceivedFormalIHS = asinh(formal_socare_hrs)
cap drop informal_socare_hrs
gen informal_socare_hrs = partner_socare_hrs + daughter_socare_hrs + son_socare_hrs + other_socare_hrs
gen HrsReceivedInformalIHS = asinh(informal_socare_hrs)


* Care provided variables (Processes S3)

gen HrsProvidedInformalIHS = asinh(careHoursProvidedWeekly)
gen provide_informal_care = (careWho >= 1)

* Age variables 
* - Categorical: 15-19, 20-24, ..., 80-84, 85+  
gen dage5 = 0
forval ii = 1/14 {
	replace dage5 = `ii' if (dag>=15+5*(`ii'-1) & dag<=19+5*(`ii'-1))
}
replace dage5 = 15 if (dag >= 85)
//table dage5, stat(min dag) stat(max dag)
tabstat dag, by(dage5) stats(min max)

* - Categorical: <35, 35-44, 45-54, 55-64, 65+ 
gen dage10prime = 0
replace dage10prime = 1 if (dag>34 & dag<45)
replace dage10prime = 2 if (dag>44 & dag<55)
replace dage10prime = 3 if (dag>54 & dag<65)
replace dage10prime = 4 if (dag>64)
//table dage10prime, stat(min dag) stat(max dag)
//table dage10prime, c(min dag max dag)
tabstat dag, by(dage10prime) stat(min max)

* - Categorical: 65-66, 67-68, 69-70, 71-72..., 85+
gen dage2old = 0
forval ii = 1/10 {
	replace dage2old = `ii' if (dag >= 65+2*(`ii'-1) & dag < 67+2*(`ii'-1))
}
replace dage2old = 11 if (dag >= 85)
//table dage2old, stat(min dag) stat(max dag)
//table dage2old, c(min dag max dag)
tabstat dag, by(dage2old) stat(min max)

* Poor health flag
gen poor_health = (dhe == 1)

* Adjust for missing values
replace need_care = . if (need_care<0)
foreach var of varlist formal_socare_hrs partner_socare_hrs daughter_socare_hrs son_socare_hrs other_socare_hrs {
	replace `var' = 0 if (`var' < 0)
}

* Prepare vars for automatic labelling
xtset idperson stm

tab dage5, gen(Age_)
//table dage5, stat(min dag) stat(max dag)	// RMK: AgeXX categories start at 1, hence shifted by 1
tabstat dag, by(dage5) stats(min max)

drop Age_1 Age_2
cap rename Age_3 Age20to24
cap rename Age_4 Age25to29
cap rename Age_5 Age30to34
cap rename Age_6 Age35to39
cap rename Age_7 Age40to44
cap rename Age_8 Age45to49
cap rename Age_9 Age50to54
cap rename Age_10 Age55to59
cap rename Age_11 Age60to64
cap rename Age_12 Age65to69
cap rename Age_13 Age70to74
cap rename Age_14 Age75to79
cap rename Age_15 Age80to84
cap rename Age_16 Age85plus

tab dage10prime, gen(Age_)
//table dage10prime, stat(min dag) stat(max dag)	// RMK: AgeXX categories start at 1, hence shifted by 1
//table dage10prime, c(min dag max dag)	
tabstat dag, by(dage10prime) stat(min max)
drop Age_1
rename Age_2 Age35to44
rename Age_3 Age45to54
rename Age_4 Age55to64
rename Age_5 Age65plus

tab dage2old, gen(Age_)
//table dage2old, stat(min dag) stat(max dag)	// RMK: AgeXX categories start at 1, hence shifted by 1
//table dage2old, c(min dag max dag)
tabstat dag, by(dage2old) stat(min max)
drop Age_1	
rename Age_2 Age65to66
rename Age_3 Age67to68
rename Age_4 Age69to70
rename Age_5 Age71to72
rename Age_6 Age73to74
rename Age_7 Age75to76
rename Age_8 Age77to78
rename Age_9 Age79to80
rename Age_10 Age81to82
rename Age_11 Age83to84
drop Age_12

gen NeedCare = need_care
gen ReceiveCare = receive_care
gen ProvideCare = provide_informal_care

tab CareMarket
gen CareMarketInformal = (CareMarket == 2)
gen CareMarketMixed = (CareMarket == 3)
gen CareMarketFormal = (CareMarket == 4)

tab ydses_c5, gen(HHincomeQ)

gen NeedCare_L1 = L.NeedCare
gen ReceiveCare_L1 = L.ReceiveCare
gen CareMarketFormal_L1 = L.CareMarketFormal
gen CareMarketInformal_L1 = L.CareMarketInformal
gen CareMarketMixed_L1 = L.CareMarketMixed
gen HrsReceivedFormalIHS_L1 = L.HrsReceivedFormalIHS
gen HrsReceivedInformalIHS_L1 = L.HrsReceivedInformalIHS
gen ProvideCare_L1 = L.ProvideCare
gen HrsProvidedInformalIHS_L1 = L.HrsProvidedInformalIHS

* Add partner's outcome variables
preserve
drop if idpartner == -9
keep idperson stm NeedCare ReceiveCare CareMarketFormal CareMarketInformal CareMarketMixed
rename idperson idpartner
rename NeedCare NeedCarePartner
rename ReceiveCare ReceiveCarePartner
rename CareMarketFormal CareMarketFormalPartner
rename CareMarketInformal CareMarketInformalPartner
rename CareMarketMixed CareMarketMixedPartner
save "$dir_work/partner.dta", replace
restore

merge m:1 idpartner stm using "$dir_work/partner.dta"
keep if _merge == 1 | _merge==3 
drop _merge

erase "$dir_work/partner.dta"


/*********************************************************************
 Additional variables required for the following estimation scripts:
 - reg_financial_distress.do
 - reg_health_mental.do
 - reg_health_wellbeing.do
*********************************************************************/

*==================================================
* Modified OECD equivalence scale
*==================================================

bysort swv idhh: egen temp_NinHH0013 = sum(dag >= 0 & dag <= 13)
bysort swv idhh: egen temp_NinHH14up = sum(dag >= 14)

* There needs to be at least one adult in every household, so that
* (temp_NinHH14up - 1) gives us the number of "additional" adults for the
* purposes of the OECD equivalence scale.
assert temp_NinHH14up >= 1

* Modified OECD equivalence scale: 1 for the first adult in the household
* (dag >= 14), 0.5 for each additional adult (dag >= 14), 0.3 for each child
* (dag <= 13)
gen moecd_eq = 1 + (temp_NinHH14up - 1) * 0.5 + (temp_NinHH0013) * 0.3

drop temp*

*==================================================
* Real equivalised household income
*==================================================

bysort swv idhh: egen temp_HH_ydisp = sum(ydisp)
gen temp_realnetinc=temp_HH_ydisp/CPI

*Winsorise income variable
winsor temp_realnetinc, gen(temp_inc_wins) p(0.001)
summ temp_inc_wins, detail

* Generate equivalised household income
gen econ_realequivinc=temp_inc_wins/moecd_eq
label var econ_realequivinc "Real equivalised household income"
drop temp_*

*==================================================
* Log income
*==================================================

gen log_income=ln(econ_realequivinc)
label var log_income "Log of real equivalised household net income"

*==================================================
* Income change (binary, increased or decreased)
*==================================================

sort idperson swv
xtset idperson swv
gen temp_incchange=econ_realequivinc - L.econ_realequivinc

gen exp_incchange=.
replace exp_incchange=1 if (econ_realequivinc < L.econ_realequivinc) & econ_realequivinc!=. & L.econ_realequivinc!=.
replace exp_incchange=0 if (econ_realequivinc == L.econ_realequivinc) & econ_realequivinc!=. & L.econ_realequivinc!=.
replace exp_incchange=0 if (econ_realequivinc > L.econ_realequivinc) & econ_realequivinc!=. & L.econ_realequivinc!=.

label define incchangecat 1 "Decreased income" 0 "Increased or stable income"
label values exp_incchange incchangecat
drop temp_*

*==================================================
* Poverty transition
*==================================================

* Generate median income for sample
bysort swv: egen temp_swvMedianIncome = wpctile(econ_realequivinc), p(50) weights(${weight})
* ONS uses net income, before or after housing costs.
* Here we use disposable income (ydisp).
gen temp_swvPovertyThreshold = temp_swvMedianIncome*0.60
label var temp_swvPovertyThreshold "Poverty threshold"

tabstat temp_swvPovertyThreshold, by(swv)
summ temp_swvPovertyThreshold, detail

* Generate poverty marker
gen temp_HHinPoverty = (econ_realequivinc <= temp_swvPovertyThreshold)
replace temp_HHinPoverty=. if missing(econ_realequivinc) | missing(temp_swvPovertyThreshold)
tab temp_HHinPoverty swv, col

* Generate poverty transition variable
sort idperson swv
gen exp_poverty= .
replace exp_poverty=0 if temp_HHinPoverty==0 & L.temp_HHinPoverty==0
replace exp_poverty=1 if temp_HHinPoverty==1 & L.temp_HHinPoverty==0
replace exp_poverty=2 if temp_HHinPoverty==0 & L.temp_HHinPoverty==1
replace exp_poverty=3 if temp_HHinPoverty==1 & L.temp_HHinPoverty==1
label define poverty_trans 0 "No Poverty" 1 "Entering poverty" 2 "Exiting poverty" 3 "Continuous poverty"
label values exp_poverty poverty_trans
label var exp_poverty "Poverty transition"
tab exp_poverty swv, m column
drop temp_*

*==================================================
* Employment transitions
*==================================================

* Generate employment volatility exposure
* Only interested in 
* employment (1) to employment (1)
* employment (1) to not employed (3)
* not employed (3) to employed (1)
* not employed (3) to not employed (3)
sort idperson swv
gen  exp_emp=.
* Starting state: employed or self-employed
replace exp_emp=11 if L.les_c4==1 & les_c4==1
replace exp_emp=13 if L.les_c4==1 & les_c4==3

* Starting state: not employed
replace exp_emp=31 if L.les_c4==3 & les_c4==1
replace exp_emp=33 if L.les_c4==3 & les_c4==3
label define exp_emp 11 "Continuous employment" 13 "Exiting employment" 31 "Entering employment" 33 "Continuously non-employed"
label value exp_emp exp_emp
tab exp_emp swv, col miss

*==================================================
* Working hour categories
*==================================================

gen lhw_c5=.
replace lhw_c5=0 if (lhw<=5)
replace lhw_c5=10 if (lhw>=6 & lhw<=15)
replace lhw_c5=20 if (lhw>=16 & lhw<=25)
replace lhw_c5=30 if (lhw>=26 & lhw<=35)
replace lhw_c5=40 if (lhw>=36 & lhw!=.)

label define lhwsp 0 "Zero" 10 "Ten" 20 "Twenty" 30 "Thirty" 40 "Forty"
label value lhw_c5 lhwsp
la var lhw_c5 "Hours worked per week (category)"


*==================================================
* Refactoring variable names 
*==================================================
xtset idperson stm

gen demMaleFlag	= dgn
gen demAge = dag
gen demAgeSq = dagsq
gen eduSampleFlag = ded


gen eduSampleFlagL1 = l.ded

gen eduHighestParentC3 = dehmf_c3 
tab eduHighestParentC3, gen(eduHighestParentC3_)

rename eduHighestParentC3_1 eduHighestParentC3High
rename eduHighestParentC3_2 eduHighestParentC3Medium
rename eduHighestParentC3_3 eduHighestParentC3Low

gen eduHighestParentC3L1 = l.dehmf_c3 
tab eduHighestParentC3L1, gen(eduHighestParentC3L1_)

rename eduHighestParentC3L1_1 eduHighestParentC3HighL1 
rename eduHighestParentC3L1_2 eduHighestParentC3MediumL1 
rename eduHighestParentC3L1_3 eduHighestParentC3LowL1 

gen yHhQuintilesMonthC5 = ydses_c5
tab yHhQuintilesMonthC5 , gen(yHhQuintilesMonthC5Q)

gen yHhQuintilesMonthC5L1 = l.ydses_c5
tab yHhQuintilesMonthC5L1 , gen(yHhQuintilesMonthC5L1_Q)

rename yHhQuintilesMonthC5L1_Q1 yHhQuintilesMonthC5Q1L1
rename yHhQuintilesMonthC5L1_Q2 yHhQuintilesMonthC5Q2L1
rename yHhQuintilesMonthC5L1_Q3 yHhQuintilesMonthC5Q3L1
rename yHhQuintilesMonthC5L1_Q4 yHhQuintilesMonthC5Q4L1
rename yHhQuintilesMonthC5L1_Q5 yHhQuintilesMonthC5Q5L1

gen demRgn = drgn1

tab drgn1, gen(demRgn_)

rename demRgn_1  demRgnUKC
rename demRgn_2  demRgnUKD
rename demRgn_3  demRgnUKE
rename demRgn_4  demRgnUKF
rename demRgn_5  demRgnUKG
rename demRgn_6  demRgnUKH
rename demRgn_7  demRgnUKI
rename demRgn_8  demRgnUKJ
rename demRgn_9  demRgnUKK
rename demRgn_10 demRgnUKL
rename demRgn_11 demRgnUKM
rename demRgn_12 demRgnUKN

gen demYear = stm

foreach y of numlist 11/25 {
    gen demYear20`y' = (demYear == `y')
	}

gen demEthnC4 = dot 
tab demEthnC4, gen(demEthnC4_)

rename demEthnC4_1 demEthnC4White
rename demEthnC4_2 demEthnC4Asian
rename demEthnC4_3 demEthnC4Black
rename demEthnC4_4 demEthnC4Other	
	
gen demPartnerStatus = dcpst
gen demPartnerStatusPartnered = (dcpst==1)
gen demPartnerStatusSingle = (dcpst==2)

gen demPartnerStatusL1 = l.dcpst
gen demPartnerStatusPartneredL1 = (demPartnerStatusL1==1)
gen demPartnerStatusSingleL1 = (demPartnerStatusL1==2)

gen eduHighestC4 = deh_c4
gen eduHighestC4L1 = l.deh_c4

gen eduHighestC4Na = (eduHighestC4==0)
gen eduHighestC4High = (eduHighestC4==1)
gen eduHighestC4Medium = (eduHighestC4==2)
gen eduHighestC4Low = (eduHighestC4==3)

gen eduHighestC4NaL1 = (eduHighestC4L1==0)
gen eduHighestC4HighL1 = (eduHighestC4L1==1)
gen eduHighestC4MediumL1 = (eduHighestC4L1==2)
gen eduHighestC4LowL1 = (eduHighestC4L1==3)

gen eduHighestPartnerC3	= dehsp_c3
gen eduHighestPartnerC3L1	= l.dehsp_c3

gen eduHighestPartnerC3HighL1 = (eduHighestPartnerC3L1==1)
gen eduHighestPartnerC3MediumL1 = (eduHighestPartnerC3L1==2)
gen eduHighestPartnerC3LowL1 = (eduHighestPartnerC3L1==3)

gen labStatusC3 = les_c3
gen labStatusC3L1 = l.les_c3

gen labStatusC3EmployedL1    = (labStatusC3L1==1)
gen labStatusC3StudentL1     = (labStatusC3L1==2)
gen labStatusC3NotEmployedL1 = (labStatusC3L1==3)


gen labStatusPartnerC3	= lessp_c3
gen labStatusPartnerC3L1 = l.les_c3

gen labStatusPartnerC3EmployedL1    = (labStatusPartnerC3L1==1)
gen labStatusPartnerC3StudentL1     = (labStatusPartnerC3L1==2)
gen labStatusPartnerC3NotEmplL1 = (labStatusPartnerC3L1==3)

gen labStatusC4 = les_c4
gen labStatusC4L1 = l.les_c4

gen labStatusC4EmployedL1    = (labStatusC4L1==1)
gen labStatusC4StudentL1     = (labStatusC4L1==2)
gen labStatusC4NotEmployedL1 = (labStatusC4L1==3)
gen labStatusC4RetiredL1     = (labStatusC4L1==4)

gen demNChild = dnc
gen demNChild0to2 = dnc02

gen demNChildL1 = l.demNChild
gen demNChild0to2L1 = l.demNChild0to2

gen eduSampleFlag_demMaleFlag = eduSampleFlag * demMaleFlag
gen eduSampleFlag_demNChildL1 = eduSampleFlag * demNChildL1
gen eduSampleFlag_demNChild0to2L1 = eduSampleFlag * demNChild0to2L1
gen eduSampleFlag_Single = eduSampleFlag * demPartnerStatusSingle

gen eduSampleFlag_Q2L1 = eduSampleFlag * yHhQuintilesMonthC5Q2L1
gen eduSampleFlag_Q3L1 = eduSampleFlag * yHhQuintilesMonthC5Q3L1
gen eduSampleFlag_Q4L1 = eduSampleFlag * yHhQuintilesMonthC5Q4L1
gen eduSampleFlag_Q5L1 = eduSampleFlag * yHhQuintilesMonthC5Q5L1


gen labStatusC4EmployedL1_Male = labStatusC4EmployedL1 * demMaleFlag
gen labStatusC4StudentL1_Male = labStatusC4StudentL1 * demMaleFlag
//gen labStatusC4NotEmployedL1_Male = labStatusC4NotEmployedL1 * demMaleFlag
gen labStatusC4RetiredL1_Male = labStatusC4RetiredL1 * demMaleFlag

gen healthPhysicalPcs = dhe_pcs
gen healthMentalMcs	= dhe_mcs

gen healthPhysicalPcsL1 = l.healthPhysicalPcs
gen healthMentalMcsL1	= l.healthMentalMcs

gen healthPhysicalPartnerPcs = dhe_pcssp
gen healthMentalPartnerMcs = dhe_mcssp

gen healthPhysicalPartnerPcsL1 = l.dhe_pcssp
gen healthMentalPartnerMcsL1	= l.dhe_mcssp

gen demPartnerNYear	= dcpyy
gen demPartnerNYearL1 = l.demPartnerNYear

gen demEnterPartnerFlag = new_rel
gen demEnterPartnerFlagL1 = l.new_rel

gen demAgePartnerDiff = dcpagdf
gen demAgePartnerDiffL1 = l.dcpagdf

gen labStatusPartnerAndOwnC4 = lesdf_c4
gen labStatusPartnerAndOwnC4L1 = l.lesdf_c4

gen labStatusPartnerAndOwnC41L1     = (labStatusPartnerAndOwnC4L1==1)
gen labStatusPartnerAndOwnC42L1     = (labStatusPartnerAndOwnC4L1==2)
gen labStatusPartnerAndOwnC43L1     = (labStatusPartnerAndOwnC4L1==3)
gen labStatusPartnerAndOwnC44L1     = (labStatusPartnerAndOwnC4L1==4)

gen yNonBenPersGrossMonth = ypnbihs_dv
gen yNonBenPersGrossMonthL1 = l.ypnbihs_dv

gen yPersAndPartnerGrossDiffMonth = ynbcpdf_dv
gen yPersAndPartnerGrossDiffMonthL1 = l.ynbcpdf_dv

gen demCompHhC4 = dhhtp_c4
gen demCompHhC4L1 = l.dhhtp_c4

gen demCompHhC4CoupleNoChL1 = (l.dhhtp_c4==1)
gen demCompHhC4CoupleChL1   = (l.dhhtp_c4==2)
gen demCompHhC4SingleNoChL1 = (l.dhhtp_c4==3)
gen demCompHhC4L1SingleChL1 = (l.dhhtp_c4==4)
					
gen healthDsblLongtermFlag = dlltsd01
gen healthDsblLongtermFlagL1 = l.dlltsd01

gen demCompHhC8= dhhtp_c8
tab demCompHhC8, gen(demCompHhC8)

gen demCompHhC8L1  = l.dhhtp_c8

gen demCompHhC81L1 = (l.dhhtp_c8==1)
gen demCompHhC82L1 = (l.dhhtp_c8==2)
gen demCompHhC83L1 = (l.dhhtp_c8==3)
gen demCompHhC84L1 = (l.dhhtp_c8==4)
gen demCompHhC85L1 = (l.dhhtp_c8==5)
gen demCompHhC86L1 = (l.dhhtp_c8==6)
gen demCompHhC87L1 = (l.dhhtp_c8==7)
gen demCompHhC88L1 = (l.dhhtp_c8==8)

gen yMiscPersGrossMonth	= yptciihs_dv
gen yMiscPersGrossMonthL1	= l.yptciihs_dv

gen wealthPrptyFlag = dhh_owned
gen wealthPrptyFlagL1 = l.dhh_owned

gen demPensAgeFlag	= dagpns
gen demPensPartnerAgeFlag = dagpns_sp

gen demPensAgeFlag_NotEmployedL1 = demPensAgeFlag * labStatusC3NotEmployedL1 


* adjust capital income 
sum ypncp, det
scalar p99 = r(p99)
replace ypncp = . if ypncp >= p99

gen yCapitalPersMonth = ypncp
gen yCapitalPersMonthL1 = l.ypncp
gen yCapitalPersMonthL2 = l2.ypncp

gen yEmpPersGrossMonth = yplgrs_dv
gen yEmpPersGrossMonthL1 = l.yplgrs_dv
gen yEmpPersGrossMonthL2 = l2.yplgrs_dv
gen yEmpPersGrossMonthL3 = l3.yplgrs_dv

gen eduSampleFlag_Male  = eduSampleFlag * demMaleFlag

gen eduSampleFlag_Pcs = eduSampleFlag * healthPhysicalPcs
gen eduSampleFlag_Mcs = eduSampleFlag * healthMentalMcs

gen eduSampleFlag_PcsL1 = l.eduSampleFlag_Pcs
gen eduSampleFlag_McsL1 = l.eduSampleFlag_Mcs

gen eduSampleFlag_yCapitalPers = eduSampleFlag * yCapitalPersMonth
gen eduSampleFlag_yCapitalPersL1 = l.eduSampleFlag_yCapitalPers
gen eduSampleFlag_yCapitalPersL2 = l2.eduSampleFlag_yCapitalPers

gen eduSampleFlag_yEmpPersGross = eduSampleFlag * yEmpPersGrossMonth
gen eduSampleFlag_yEmpPersGrossL1 = l.eduSampleFlag_yEmpPersGross
gen eduSampleFlag_yEmpPersGrossL2 = l2.eduSampleFlag_yEmpPersGross

gen eduHighestC4Na_demAge = eduHighestC4Na * dag 
gen eduHighestC4Low_demAge = eduHighestC4Low * dag 
gen eduHighestC4Medium_demAge = eduHighestC4Medium * dag 
gen eduHighestC4High_demAge = eduHighestC4High * dag 

gen eduHighestC4NaL1_demAge   = eduHighestC4NaL1 * demAge
gen eduHighestC4LowL1_demAge    = eduHighestC4LowL1 * demAge
gen eduHighestC4MediumL1_demAge = eduHighestC4MediumL1 * demAge
gen eduHighestC4HighL1_demAge   = eduHighestC4HighL1 * demAge

gen labPt = (lhw > 0 & lhw <=25)

* adjust pension income 
sum ypnoab, det
scalar p99 = r(p99)
replace ypnoab = . if ypnoab >= p99

gen yPensPersGrossMonth = ypnoab
gen yPensPersGrossMonthL1 = l.ypnoab
gen yPensPersGrossMonthL2 = l2.ypnoab

gen healthSelfRated = dhe 

tab healthSelfRated, gen(healthSelfRated_)
rename healthSelfRated_1 healthSelfRatedPoor
rename healthSelfRated_2 healthSelfRatedFair
rename healthSelfRated_3 healthSelfRatedGood
rename healthSelfRated_4 healthSelfRatedVeryGood
rename healthSelfRated_5 healthSelfRatedExcellent


gen healthPartnerSelfRated = dhesp 

tab healthPartnerSelfRated, gen(healthPartnerSelfRated_)
rename healthPartnerSelfRated_1 healthPartnerSelfRatedPoor
rename healthPartnerSelfRated_2 healthPartnerSelfRatedFair
rename healthPartnerSelfRated_3 healthPartnerSelfRatedGood
rename healthPartnerSelfRated_4 healthPartnerSelfRatedVeryGood
rename healthPartnerSelfRated_5 healthPartnerSelfRatedExcellent



rename Age20to24 demAge20to24
rename Age25to29 demAge25to29
rename Age30to34 demAge30to34
rename Age35to39 demAge35to39
rename Age40to44 demAge40to44
rename Age45to49 demAge45to49
rename Age50to54 demAge50to54
rename Age55to59 demAge55to59
rename Age60to64 demAge60to64
rename Age65to69 demAge65to69
rename Age70to74 demAge70to74
rename Age75to79 demAge75to79
rename Age80to84 demAge80to84
rename Age85plus demAge85plus

rename Age65to66 demAge65to66
rename Age67to68 demAge67to68
rename Age69to70 demAge69to70
rename Age71to72 demAge71to72
rename Age73to74 demAge73to74
rename Age75to76 demAge75to76
rename Age77to78 demAge77to78
rename Age79to80 demAge79to80
rename Age81to82 demAge81to82
rename Age83to84 demAge83to84

gen careMarket = CareMarket
gen careMarketL1 = l.careMarket


gen careMarketInformal = (careMarket == 2)
gen careMarketMixed = (careMarket == 3)
gen careMarketFormal = (careMarket == 4)

gen careMarketInformalL1 = (l.careMarket== 2)
gen careMarketMixedL1    = (l.careMarket == 3)
gen careMarketFormalL1   = (l.careMarket == 4)

gen careHrsInformalIhs = HrsReceivedInformalIHS
gen careHrsInformalIhsL1 = l.HrsReceivedInformalIHS

gen careHrsFormalIhs = HrsReceivedFormalIHS 
gen careHrsFormalIhsL1 = l.HrsReceivedFormalIHS


gen careNeedFlag = NeedCare	
gen careNeedFlagL1 = l.NeedCare

gen careReceivedFlag = ReceiveCare 
gen careReceivedFlagL1 = l.ReceiveCare 

gen careProvidedFlag = ProvideCare	
gen careProvidedFlagL1 = l.ProvideCare

gen careNeedPartnerFlag = NeedCarePartner
gen careReceivedPartnerFlag = ReceiveCarePartner 

gen careMarketInformalPartner = CareMarketInformalPartner
gen careMarketMixedPsrtner = CareMarketMixedPartner
gen careMarketFormalPartner = CareMarketFormalPartner 

gen careHrsProvidedWeekIhs = HrsProvidedInformalIHS 
gen careHrsProvidedWeekIhsL1 = l.HrsProvidedInformalIHS 

*==================================================
* End  
*==================================================

