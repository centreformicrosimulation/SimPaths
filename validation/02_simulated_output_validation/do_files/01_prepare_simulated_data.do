/*******************************************************************************
* PROJECT: 			SimPaths UK
* SECTION:			Validation
* OBJECT: 			Simulation data pre-processing
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		Jan 2026
* COUNTRY: 			UK 
* DESCRIPTION: 		This file loads the simulated output from SimPaths and 
* 					preserves the relevant variables for validation. 
********************************************************************************
* NOTES:			Works in Stata 18 onwards - able to preserve case in var
* 					names.
*******************************************************************************/

// currently hh information not used 
/*
* Import required variables from household file

* Import required variables from benefit unit file
forvalues n = 1/$max_n_runs {

	import delimited "${dir_simulated_data}/run`n'/csv/Household.csv", ///
		varnames(1) case(preserve) clear

	keep run time id_Household  

	rename id_Household idHh

	keep if time <= ${max_year}	

	save "${dir_data}/household_sim`n'", replace
	
}

* Combine runs 
use "${dir_data}/household_sim1", clear 

forvalues n = 2/$max_n_runs {
	
	append using "${dir_data}/household_sim`n'"
	
}	

save "${dir_data}/household_sim", replace

* Tidy up 
forvalues n = 1/$max_n_runs {

	erase "${dir_data}/household_sim`n'.dta"

}
*/


* Import required variables from benefit unit file
forvalues n = 0/$max_n_runs {

	import delimited ///
		"${dir_simulated_data}/${folder}_`n'/csv/BenefitUnit.csv", ///
		varnames(1) case(preserve) clear

	keep run time idHh id_BenefitUnit yDispMonth yDispEquivYear yGrossMonth 
		
	rename id_BenefitUnit idBu
	
	replace run = `n'

	keep if time <= ${max_sim_year}	

	save "${dir_data}/benefitunit_sim`n'", replace
	
}

* Combine runs 
use "${dir_data}/benefitunit_sim0", clear 

forvalues n = 1/$max_n_runs {
	
	append using "${dir_data}/benefitunit_sim`n'"
	
}	

save "${dir_data}/benefitunit_sim", replace

* Tidy up 
forvalues n = 0/$max_n_runs {

	erase "${dir_data}/benefitunit_sim`n'.dta"

}


* Import required variables from person file
forvalues n = 0/$max_n_runs {
	
	import delimited "${dir_simulated_data}/${folder}_`n'/csv/Person.csv", ///
		varnames(1) case(preserve) clear

	keep run time id_Person idPartner idBu idMother idFather demAge ///
		demMaleFlag demPartnerStatus labC4 eduHighestC4 ///
		healthDsblLongtermFlag healthSelfRated healthMentalMcs ///
		healthPhysicalPcs ///
		yNonBenPersGrossMonth yEmpPersGrossMonth yCapitalPersMonth ///
		yPensPersGrossMonth yMiscPersGrossMonth ///
		labHrsWorkWeek labHrsWorkEnumWeek labHrsWorkWeek ///
		labWageFullTimeHrly ///
		careNeedFlag careHrsInformal careHrsFormal ///
		careHrsProvidedWeek careFormalX 
		
	rename id_Person idPers
	
	replace run = `n'

	keep if time <= ${max_sim_year}
											
	save "${dir_data}/person_sim`n'", replace

}

* Combine runs 
use "${dir_data}/person_sim0", clear 

forvalues n = 1/$max_n_runs {
	
	append using "${dir_data}/person_sim`n'"
	
}	

save "${dir_data}/person_sim", replace

* Tidy up 
forvalues n = 0/$max_n_runs {

	erase "${dir_data}/person_sim`n'.dta"

}	

	
* Combine simulated data 
use "${dir_data}/person_sim", clear

merge m:1 run time idBu using "${dir_data}/benefitunit_sim"

save "$dir_data/loaded_simulation_data.dta", replace


* Tidy up 
//erase "${dir_data}/household_sim.dta"
erase "${dir_data}/person_sim.dta"
erase "${dir_data}/benefitunit_sim.dta"
