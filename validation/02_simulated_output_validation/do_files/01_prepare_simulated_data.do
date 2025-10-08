********************************************************************************
* SECTION:			Validation
* OBJECT: 			Simulation data pre-processing
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25
* COUNTRY: 			UK 

* DESCRIPTION: 		This file loads the simulated output from SimPaths and 
* 					preserves the relevant variables for validation. 
* 
* NOTES: 			
********************************************************************************

* Import required variables from household file
import delimited "${dir_simulated_data}/Household.csv", clear 

keep run time id_household  
rename id_household idhousehold
keep if time <= ${max_year}

save "${dir_simulated_data}/household_sim", replace


* Import required variables from benefit unit file
import delimited "${dir_simulated_data}/BenefitUnit.csv", clear

keep run time idhousehold id_benefitunit disposableincome ///
	equivaliseddisposableincomeyearl grossincomemonthly /*dhhtp_c4*/
rename id_benefitunit idbenefitunit
keep if time <= ${max_year}	

save "${dir_simulated_data}/benefitunit_sim", replace


* Import required variables from person file
import delimited "${dir_simulated_data}/Person.csv", clear

keep run time id_person idpartner dcpst idbenefitunit dag les_c4 ///
	deh_c3 dhe dgn ypnbihs_dv yplgrs_dv ypncp ypnoab dhe_mcs dhe_pcs ///
	fulltimehourlyearningspotential laboursupplyweekly hoursworkedweekly ///
	needsocialcare dlltsd
rename id_person idperson
keep if time <= ${max_year}
										
save "${dir_simulated_data}/person_sim", replace


* Combine simulated data 
use "${dir_simulated_data}/person_sim", clear

merge m:1 run time idbenefitunit using ///
	"${dir_simulated_data}/benefitunit_sim", nogen keep(matched)

save "${dir_simulated_data}/baseline_sim", replace 


* Add labels 
order run time id*

destring yplgrs_dv ypnbihs_dv ypncp ypnoab, replace ignore("null")

capture label var run "Simulation repetition number"
capture label var time "year"
capture label var idhousehold "Household ID"
capture label var idbenefitunit "Benefit unit ID"
capture label var idfemale "Benefit unit responsible female ID"
capture label var idmale "Benefit unit responsible male ID"
capture label var idperson "Person ID"
capture label var idfather "Father ID"
capture label var idmother "Mother ID"
capture label var idpartner "Partner ID"
capture label var atriskofpoverty "At risk of poverty indicator using 60% of median income"
capture label var dhhtp_c4 "Benefit unit composition"
capture label var disposableincomemonthly "Disposable income, benefit unit, monthly"
capture label var equivaliseddisposableincomeyearl "Equivalised disposable income, benefit unit, yearly"
capture label var occupancy "Benefit unit occupancy (responsible persons)"
capture label var region "Region"
capture label var size "Benefit unit size"
capture label var ydses_c5 "Benefit unit gross equivalised normalised income quintile"
capture label var adultchildflag "Adult child living at home"
capture label var dag "Age"
capture label var dcpagdf "Difference in age between partners"
capture label var dcpen "Entered partnership"
capture label var dcpex "Exited partnership "
capture label var dcpst "Partnership status"
capture label var dcpyy "years in partnership"
capture label var ded "In continuous education"
capture label var deh_c3 "Education level, highest"
capture label var dehf_c3 "Father's education level, highest"
capture label var dehm_c3 "Mother's education level, highest"
capture label var dehsp_c3 "Partner's education level, highest'"
capture label var der "Returned to education"
capture label var dgn "Gender"
capture label var dhe "Self-rated health"
capture label var dhm "Pscyhological distress score"
capture label var dhm_ghq "Psychological distress case"
capture label var dhesp "Partner's self-rated health"
capture capture label var dlltsd "Long-term sick / disabled"
capture label var sedex "Left education this year"
capture capture label var women_fertility "Indicator, women aged 18 to 44 who can have children"
capture label var laboursupplyweekly "Discretized hours of labour supply, weekly"
capture label var hoursworkedweekly "Continuous hours of labour supply, weekly"
capture label var les_c4 "Activity status"
capture label var lessp_c4 "Partner's activity status"
capture label var lesdf_c4 "Own and partner's activity status (only if partnered)"
capture label var fulltimehourlyearningspotential "Potential (model-based) hourly gross wage"
capture label var sindex "Security index (5-year lead)" 
capture label var sindexnormalised "Normalised security index (5-year lead)" 
capture label var scaling_factor "Scaling factor (one individual represents this many in population)"
capture label var ynbcpdf_dv "Difference between (asinh of) own and spouse's gross personal non-benefit income"
capture gen yplgrs_dv_lvl = sinh(yplgrs_dv)
capture label var yplgrs_dv_lvl "Gross personal employment income"
capture label var yplgrs_dv "Gross personal employment income, asinh"
capture destring ypnbihs_dv, force replace
capture gen ypnbihs_dv_lvl = sinh(ypnbihs_dv)
capture label var ypnbihs_dv_lvl "Gross personal non-benefit income"
capture label var ypnbihs_dv "Gross personal non-benefit income, asinh"
capture gen ypncp_lvl = sinh(ypncp)
capture label var ypncp_lvl "Capital income"
capture label var ypncp "Capital income, asinh"
capture gen ypnoab_lvl = sinh(ypnoab)
capture label var ypnoab_lvl "Pension income,"
capture label var ypnoab "Pension income, asinh"
capture gen yptciihs_dv_lvl = sinh(yptciihs_dv)
capture label var yptciihs_dv_lvl "Gross personal non-employment"
capture label var grossincomemonthly "Gross monthly benefit unit income"
capture label define dhe_lbl 1 "Poor" 2 "Fair" 3 "Good" 4 "VeryGood" 5 "Excellent"

rename dhe dhe2
encode dhe2, gen(dhe) label(dhe_lbl)
drop dhe2 
capture drop weight

rename equivaliseddisposableincomeyearl equivalisedincome
rename time year

keep if run <= 3

save "$dir_data/simulated_data_prep1.dta", replace


* Tidy up 
erase "${dir_simulated_data}/household_sim.dta"
erase "${dir_simulated_data}/person_sim.dta"
erase "${dir_simulated_data}/benefitunit_sim.dta"
erase "${dir_simulated_data}/baseline_sim.dta"
