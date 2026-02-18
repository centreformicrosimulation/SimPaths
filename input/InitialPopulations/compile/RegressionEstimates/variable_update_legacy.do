/*********************************************************************
 Additional variables from the legacy version of variable_update.do

 These are required for the following estimation scripts:
 - reg_financial_distress.do
 - reg_health_mental.do
 - reg_health_wellbeing.do

 Longer-term, these should be migrated into variable_update.do, and
 the patterns established there (naming conventions etc.) used.
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
bysort swv: egen temp_swvMedianIncome = wpctile(econ_realequivinc), p(50) weights(dhhwt)
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
