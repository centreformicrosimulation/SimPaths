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
gen FertilityRate = dukfr
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


