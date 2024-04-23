***************************************************************************************
* PROJECT:              ESPON: construct initial populations for SimPaths using UKHLS data 
* DO-FILE NAME:         09_fill_in_missing_spouses.do
* DESCRIPTION:          This file generates data for importing into SimPaths
***************************************************************************************
* COUNTRY:              UK
* DATA:         	    UKHLS EUL version - UKDA-6614-stata [to wave m]
* AUTHORS: 				Justin van de Ven
* LAST UPDATE:          10 Apr 2024
* NOTE:					Called from 00_master.do - see master file for further details
***************************************************************************************
clear all
set more off
set mem 200m
set type double
//set maxvar 120000
set maxvar 30000


***************************************************************************************
* pool all waves
***************************************************************************************
forvalues year = $firstSimYear/$lastSimYear {
* load pooled data with missing values removed  
	
	if (`year'==$firstSimYear) {
		use "$dir_data/population_initial_fs_UK_`year'.dta", clear
	}
	else {
		append using "$dir_data/population_initial_fs_UK_`year'.dta"
	}
}
save "$dir_data\ukhls_pooled_all_obs_09.dta", replace


***************************************************************************************
* identify sub-sample of couples with and without missing spouses
***************************************************************************************
use "$dir_data\ukhls_pooled_all_obs_09.dta", clear
keep if dcpst==1

order swv idperson idbenefitunit idpartner ypnbihs_dv ynbcpdf_dv
gsort idbenefitunit swv idperson

// remove if age is missing (20 obs)
drop if (dag<0)

by idbenefitunit : egen na = sum(adult)
order swv idperson idbenefitunit idpartner na


***************************************************************************************
* generate variables for coarsened matching
***************************************************************************************
gen dvage = floor(dag/5)  // 5 year age bands
gen id_nc02 = (dnc02>0)   // dummy for children under 3
gen nk = dnc
replace nk = 3 if (dnc>3)  // top-code number of children
gen ee2 = runiform()	   // random variable to select donor


***************************************************************************************
* use matching to impute spouse data for observations without spouse data reported by survey
***************************************************************************************
gsort na idbenefitunit swv idperson
gen treat = (na==1)
gen sprnk = -9
replace sprnk = 0 if (treat==1)
qui {
	sum treat, mean
	local nn = r(mean) * r(N)
}
forval kk = 1/`nn' {
// loop over each observation in dataset

	qui {
		gen chk = 1-treat 		// consider data points in "from" dataset
		local rnk = 1
		foreach vv of varlist swv dgn drgn1 dhhtp_c4 ydses_c5 dhh_owned dag dagsp deh_c3 dehsp_c3 dhe dhesp dlltsd dlltsd_sp dnc02 dnc les_c4 {
			replace chk = 0 if (`vv'!=`vv'[`kk'])  // limit data point in from dataset to those with the same discrete characteristics
		}
		sum chk, mean
	}
	if (r(mean)==0) {
	// no match obtained to rnk 1 coarse matching criteria - consider rnk 2 criteria

		qui {
			drop chk
			gen chk = 1-treat
			local rnk = 2
			foreach vv of varlist dgn dhhtp_c4 ydses_c5 dhh_owned dag dagsp deh_c3 dhe dhesp dlltsd dlltsd_sp dnc02 dnc les_c4 {
				replace chk = 0 if (`vv'!=`vv'[`kk'])
			}
			sum chk, mean
		}
		if (r(mean)==0) {
		// no match obtained to rnk 2 coarse matching criteria - consider rnk 3 criteria
			
			qui {
				drop chk
				gen chk = 1-treat
				local rnk = 3
				foreach vv of varlist dgn dhhtp_c4 ydses_c5 dhh_owned dvage deh_c3 dhe dhesp dlltsd dlltsd_sp id_nc02 nk les_c4 {
					replace chk = 0 if (`vv'!=`vv'[`kk'])
				}
				sum chk, mean
			}
			if (r(mean)==0) {
			// no match obtained to rnk 3 coarse matching criteria - consider rnk 3 criteria
				
				qui {
					drop chk
					gen chk = 1-treat
					local rnk = 4
					foreach vv of varlist dgn dhhtp_c4 ydses_c5 dhh_owned dvage deh_c3 dhesp dlltsd dlltsd_sp {
						replace chk = 0 if (`vv'!=`vv'[`kk'])
					}
					sum chk, mean
				}
				if (r(mean)==0) {
					disp "failed to find match"
					local rnk = 5
				}
			}
		}
	}
	qui {
		if (r(mean)>0) {
		// the matching pool is not empty
		
			// obtain donor
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
			local idpartner = idpartner[1]
			local swv = swv[1]
			local les_c4 = les_c4[1]
			local les_c3 = les_c3[1]
			restore
			preserve
			keep if (idperson==`idpartner' & swv==`swv')
			gen newDonor = 1
			save "$dir_data/temp", replace
			
			// add observation to dataset and align variables for consistency
			restore
			append using "$dir_data/temp"
			foreach vv of varlist idhh idbenefitunit swv stm liquid_wealth dnc02 dnc drgn1 dcpyy CPI dukfr dimlwt disclwt dimxwt dhhwt dwt{
				replace `vv' = `vv'[`kk'] if (newDonor==1)
			}
			replace idpartner = idperson[`kk'] if (newDonor==1)
			replace idperson = idpartner[`kk'] if (newDonor==1)
			replace pno = ppno[`kk'] if (newDonor==1)
			replace deh_c3 = dehsp_c3[`kk'] if (newDonor==1)
			replace dehsp_c3 = deh_c3[`kk'] if (newDonor==1)
			replace dagsp = dag[`kk'] if (newDonor==1)
			replace dag = dagsp[`kk'] if (newDonor==1)
			replace dagsq = dagsp[`kk'] * dagsp[`kk'] if (newDonor==1)
			replace drgn1 = drgn1[`kk'] if (newDonor==1)
			replace ppno = pno[`kk'] if (newDonor==1)
			replace ynbcpdf_dv = - ynbcpdf_dv[`kk'] if (newDonor==1)
			replace ypnbihs_dv = ypnbihs_dv[`kk'] - ynbcpdf_dv[`kk'] if (newDonor==1)
			replace lessp_c4 = `les_c4' if (_n==`kk')
			replace lessp_c3 = `les_c3' if (_n==`kk')
			replace lesdf_c4 = 1 if (_n==`kk' & les_c4==1 & `les_c4'==1)
			replace lesdf_c4 = 2 if (_n==`kk' & les_c4==1 & `les_c4'!=1)
			replace lesdf_c4 = 3 if (_n==`kk' & les_c4!=1 & `les_c4'==1)
			replace lesdf_c4 = 3 if (_n==`kk' & les_c4!=1 & `les_c4'!=1)
			replace lesdf_c4 = lesdf_c4[`kk'] if (newDonor==1)
			drop newDonor
		}
		replace sprnk=`rnk' if (_n==`kk')
		drop chk
	}
	if (mod(`kk',10)==0) disp "matched to observation `kk'"
}


***************************************************************************************
* finalise
***************************************************************************************
drop na id_nc02 nk dvage ee2 treat
save "$dir_data\ukhls_pooled_all_obs_09b.dta", replace


#delimit ;
local files_to_drop 
	temp.dta
	;
#delimit cr // cr stands for carriage return

foreach file of local files_to_drop { 
	erase "$dir_data/`file'"
}

cap log close


***************************************************************************************
* end
***************************************************************************************


