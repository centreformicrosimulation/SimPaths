
xtset idperson swv

* --------------------------------------------
* 1. Handle Missing Values and Basic Setup
* --------------------------------------------

// Recode -9 as missing for all variables
foreach var of varlist _all {
    replace `var' = . if `var' == -9
}

// Sort data by individual and wave
sort idperson swv

// Recode year to two-digit format
replace stm = stm - 2000

// cap generate COVID year dummies
cap cap gen y2020 = (stm == 20)
cap cap gen y2021 = (stm == 21)


* --------------------------------------------
* 2. Correct Inconsistencies
* --------------------------------------------

// Fix inconsistent student coding
replace ded = 0 if idperson == idperson[_n-1] & ded == 1 & ded[_n-1] == 0


* --------------------------------------------
* 3. Construct New Variables
* --------------------------------------------

// Partnership status in the first year
cap cap gen new_rel = 0 if dcpst == 1
replace new_rel = 1 if dcpen == 1
label var new_rel "Partnerhip in first year"

// Household type: 8 categories
cap cap gen dhhtp_c8 = . 
label var dhhtp_c8 "Household Type: 8 Category"
replace dhhtp_c8 = 1 if dhhtp_c4 == 1 & lessp_c3 == 1
replace dhhtp_c8 = 2 if dhhtp_c4 == 1 & lessp_c3 == 2
replace dhhtp_c8 = 3 if dhhtp_c4 == 1 & lessp_c3 == 3	
replace dhhtp_c8 = 4 if dhhtp_c4 == 2 & lessp_c3 == 1
replace dhhtp_c8 = 5 if dhhtp_c4 == 2 & lessp_c3 == 2
replace dhhtp_c8 = 6 if dhhtp_c4 == 2 & lessp_c3 == 3	
replace dhhtp_c8 = 7 if dhhtp_c4 == 3
replace dhhtp_c8 = 8 if dhhtp_c4 == 4
cap label define dhhtp_c8 1 "Couple with no children, spouse employed" 2 "Couple with no children, spouse student" 3 "Couple with no children, spouse not employed" 4 "Couple with children, spouse employed" 5 "Couple with children, spouse student" 6 "Couple with children, spouse not employed" 7 "Single with no children" 8 "Single with children"
label values dhhtp_c8 dhhtp_c8	

// Squared income variable
cap cap gen ypnbihs_dv_sq = ypnbihs_dv^2
label variable ypnbihs_dv_sq "Personal Non-benefit Gross Income Squared"

// Dummy for receiving capital income
cap cap gen receives_ypncp = (ypncp > 0 & !missing(ypncp))

// Transform capital income from IHS to level + log
cap drop ypncp_lvl
cap gen ypncp_lvl = sinh(ypncp)
cap gen ln_ypncp = ln(ypncp_lvl)

// Dummy and transformation for private pension income
cap drop ypnoab_lvl
cap gen ypnoab_lvl = sinh(ypnoab)
cap cap gen ln_ypnoab = ln(ypnoab_lvl)
cap cap gen receives_ypnoab = (ypnoab_lvl > 0 & !missing(ypnoab_lvl))

// Dummy for state pension age
cap cap gen state_pension_age = (dag >= 68)


* --------------------------------------------------
* 4. Lag Variables + Handle Missing Lags at Age 16
* --------------------------------------------------

// Create basic lags
sort idperson swv
cap cap gen l_ydses_c5 = ydses_c5[_n-1] if idperson == idperson[_n-1] & swv == swv[_n-1] + 1 
cap cap gen l_dhe = dhe[_n-1] if idperson == idperson[_n-1] & swv == swv[_n-1] + 1 
cap cap gen l_les_c3 = les_c3[_n-1] if idperson == idperson[_n-1] & swv == swv[_n-1] + 1 
cap cap gen l_lesnr_c2 = lesnr_c2[_n-1] if idperson == idperson[_n-1] & swv == swv[_n-1] + 1 
cap cap gen l_dhhtp_c4 = dhhtp_c4[_n-1] if idperson == idperson[_n-1] & swv == swv[_n-1] + 1 
cap cap gen l_dhe_pcs = dhe_pcs[_n-1] if idperson == idperson[_n-1] & swv == swv[_n-1] + 1 
cap cap gen l_dhe_mcs = dhe_mcs[_n-1] if idperson == idperson[_n-1] & swv == swv[_n-1] + 1 
cap cap gen l_dlltsd = dlltsd[_n-1] if idperson == idperson[_n-1] & swv == swv[_n-1] + 1 
cap cap gen l_dlltsd01 = dlltsd01[_n-1] if idperson == idperson[_n-1] & swv == swv[_n-1] + 1 

// Fill in missing lags using current values at age 16
gsort +idperson -stm
bys idperson: carryforward dhe if dag <= 16, replace 
bys idperson: carryforward dhe_pcs if dag <= 16, replace 
bys idperson: carryforward dhe_mcs if dag <= 16, replace 

sort idperson swv
cap drop dhe_L1
bys idperson: gen dhe_L1 = l.dhe
replace dhe_L1 = dhe if missing(dhe_L1)

cap drop dhe_pcs_L1
bys idperson: gen dhe_pcs_L1 = l.dhe_pcs
replace dhe_pcs_L1 = dhe_pcs if missing(dhe_pcs_L1)

cap drop dhe_mcs_L1
bys idperson: gen dhe_mcs_L1 = l.dhe_mcs
replace dhe_mcs_L1 = dhe if missing(dhe_mcs_L1)

cap drop yplgrs_dv_L1
bys idperson: gen yplgrs_dv_L1 = l.yplgrs_dv
replace yplgrs_dv_L1 = yplgrs_dv if missing(yplgrs_dv_L1)

cap drop yplgrs_dv_L2
bys idperson: gen yplgrs_dv_L2 = l2.yplgrs_dv
replace yplgrs_dv_L2 = yplgrs_dv if missing(yplgrs_dv_L2)

cap drop ypncp_L1
bys idperson: gen ypncp_L1 = l.ypncp
replace ypncp_L1 = ypncp if missing(ypncp_L1)

cap drop ypncp_L2
bys idperson: gen ypncp_L2 = l2.ypncp
replace ypncp_L2 = ypncp if missing(ypncp_L2)

cap drop ypnoab_L1
bys idperson: gen ypnoab_L1 = l.ypnoab
replace ypnoab_L1 = ypnoab if missing(ypnoab_L1)

cap drop ypnoab_L2
bys idperson: gen ypnoab_L2 = l2.ypnoab
replace ypnoab_L2 = ypnoab if missing(ypnoab_L2)

cap drop dhhtp_c4_L1
bys idperson: gen dhhtp_c4_L1 = l.dhhtp_c4
replace dhhtp_c4_L1 = dhhtp_c4 if missing(dhhtp_c4_L1)

cap drop les_c3_L1
bys idperson: gen les_c3_L1 = l.les_c3
replace les_c3_L1 = les_c3 if missing(les_c3_L1)


* --------------------------------------------------
* 4. Labelling 
* --------------------------------------------------

* Label definitions
cap label define jbf 1 "Employed" 2 "Student" 3 "Not Employed"
cap label define jbg 1 "Employed" 2 "Student" 3 "Not employed" 4 "Retired"
cap label define edd 1 "Degree" 2 "Other Higher/A-level/GCSE" 3 "Other/No Qualification"
cap label define hht 1 "Couples with No Children" 2 "Couples with Children" 3 "Single with No Children" 4 "Single with Children"
cap label define gdr 1 "Male" 0 "Female"
cap label define rgna 1 "North East" 2 "North West" 4 "Yorkshire and the Humber" 5 "East Midlands" 6 "West Midlands" 7 "East of England" 8 "London" 9 "South East" 10 "South West" 11 "Wales" 12 "Scotland" 13 "Northern Ireland"
cap label define yn 1 "Yes" 0 "No"
cap label define dces 1 "Both Employed" 2 "Employed, Spouse Not Employed" 3 "Not Employed, Spouse Employed" 4 "Both Not Employed"
cap label define ethn 1 "White" 2 "Asian or Asian British" 3 "Black, Black British, Caribbean, or African" 4 "Other or missing ethnic group"
cap label define dhe 1 "Poor" 2 "Fair" 3 "Good" 4 "Very Good" 5 "Excellent", modify 

* Variable labels
label variable dgn "cap gender"
label variable dag "Age"
label variable dagsq "Age Squared"
label variable drgn1 "Region"
label variable stm "Year"
label variable les_c3 "Employment Status: 3 Category"
label variable les_c4 "Employment Status: 4 Category"
label variable dhe "Self-rated Health"
label variable dcpen "Entered a new Partnership"
label variable dcpex "Partnership dissolution"
label variable deh_c3 "Educational Attainment: 3 Category"
label variable ydses_c5 "Annual Household Income Quintile"
label variable dlltsd "Long-term Sick or Disabled"
label variable dhhtp_c4 "Household Type: 4 Category"
label variable dhhtp_c8 "Household Type: 8 Category"
label variable dnc "Number of Children in Household"
label variable dnc02 "Number of Children aged 0-2 in Household"
label variable dot "Ethnicity"
label variable dehmf_c3 "Highest Parental Educational Attainment: 3 Category"
label variable dhe_mcs "Subjective Self-rated health - Mental (SF12 MCS)"
label variable dhe_pcs "Subjective Self-rated health - Physical (SF12 PCS)"
label variable dagpns "Reached state retirement age"
label variable dagpns_sp "Reached state retirement age - partner"
label variable dukfr "UK Fertility Rate"
label variable lesdf_c4 "Differential Employment Status"
label variable ypnbihs_dv "Personal Non-benefit Gross Income"
label variable ynbcpdf_dv "Differential Personal Non-Benefit Gross Income"

* Attach value labels to variables
label values dgn gdr
label values drgn1 rgna
label values les_c3 lessp_c3 jbf 
label values les_c4 jbg 
label values deh_c3 dehsp_c3 edd 
label values dcpen dcpex yn
label values lesdf_c4 dces
label values dhhtp_c4 hht 
label values dhhtp_c8 dhhtp_c8
label values dot ethn 
label values dhe dhe
label value ded yn
label value dlltsd yn
label value dlltsd01 yn

* Alter names and create dummies for automatic labelling 
*(required for gologit) 

cap gen Dgn = dgn 
cap gen Dag = dag  
cap gen Dag_sq = dagsq 


capture drop UK*
capture drop Deh_c3_*
capture drop Dehmf_c3_*
capture drop Les_c4_*
capture drop L_Les_c3_*
capture drop Ydses_c5_Q*
capture drop L_Ydses_c5_Q*
capture drop Dhe_*
capture drop L_Dhe_c5_*
capture drop Dhhtp_c4_*
capture drop L_Dhhtp_c4_*
capture drop dot_*
cap drop Ethn_White Ethn_Asian Ethn_Black Ethn_Other

tab drgn1, gen(UK) 
rename UK1 UKC //North East
rename UK2 UKD //North West
rename UK3 UKE //Yorkshire and the Humber
rename UK4 UKF //East Midlands
rename UK5 UKG //West Midlands
rename UK6 UKH //East of England
rename UK7 UKI //London
rename UK8 UKJ //South East
rename UK9 UKK //South West
rename UK10 UKL //Wales
rename UK11 UKM //Scotland
rename UK12 UKN //Northern Ireland

tab deh_c3, gen(Deh_c3_)
rename Deh_c3_1 Deh_c3_High
rename Deh_c3_2 Deh_c3_Medium
rename Deh_c3_3 Deh_c3_Low

tab dehmf_c3, gen(Dehmf_c3_)
rename Dehmf_c3_1 Dehmf_c3_High
rename Dehmf_c3_2 Dehmf_c3_Medium
rename Dehmf_c3_3 Dehmf_c3_Low

tab les_c4, gen(Les_c4_)
rename Les_c4_1 Les_c4_Employed
rename Les_c4_2 Les_c4_Student
rename Les_c4_3 Les_c4_NotEmployed
rename Les_c4_4 Les_c4_Retired

tab l_les_c3, gen(L_Les_c3_)
rename L_Les_c3_1 L_Les_c3_Employed
rename L_Les_c3_2 L_Les_c3_Student
rename L_Les_c3_3 L_Les_c3_NotEmployed

tab ydses_c5, gen(Ydses_c5_Q)

tab l_ydses_c5, gen(L_Ydses_c5_Q)

tab dhe, gen(Dhe_)
rename Dhe_1 Dhe_Poor
rename Dhe_2 Dhe_Fair
rename Dhe_3 Dhe_Good
rename Dhe_4 Dhe_VeryGood
rename Dhe_5 Dhe_Excellent

tab l_dhe, gen(L_Dhe_c5_)

tab dhhtp_c4, gen(Dhhtp_c4_)
rename Dhhtp_c4_1 Dhhtp_c4_CoupleNoChildren
rename Dhhtp_c4_2 Dhhtp_c4_CoupleChildren
rename Dhhtp_c4_3 Dhhtp_c4_SingleNoChildren
rename Dhhtp_c4_4 Dhhtp_c4_SingleChildren

tab l_dhhtp_c4, gen(L_Dhhtp_c4_)
rename L_Dhhtp_c4_1 L_Dhhtp_c4_CoupleNoChildren
rename L_Dhhtp_c4_2 L_Dhhtp_c4_CoupleChildren
rename L_Dhhtp_c4_3 L_Dhhtp_c4_SingleNoChildren
rename L_Dhhtp_c4_4 L_Dhhtp_c4_SingleChildren

tab dot, gen(dot_)
rename dot_1 Ethn_White
rename dot_2 Ethn_Asian
rename dot_3 Ethn_Black
rename dot_4 Ethn_Other




cap gen Year_transformed = stm  

cap gen Y2020 = y2020
cap gen Y2021 = y2021

cap gen Dhe = dhe 
cap gen Dhe_pcs = dhe_pcs
cap gen Dhe_mcs = dhe_mcs

cap gen Ydses_c5 = ydses_c5 

cap gen L_Ydses_c5 = l_ydses_c5

cap gen L_Dhe = l_dhe
cap gen L_Dhe_pcs = l_dhe_pcs
cap gen L_Dhe_mcs = l_dhe_mcs

cap gen Dlltsd = dlltsd
cap gen Dlltsd01 = dlltsd01

cap gen L_Dlltsd = l_dlltsd
cap gen L_Dlltsd01 = l_dlltsd01



