/*******************************************************************************
* PROJECT:  		SimPaths UK 
* SECTION:			Validation
* OBJECT: 			Economic Activity Status plots
* AUTHORS:			Ashley Burdett 
* LAST UPDATE:		9/25
* COUNTRY: 			UK 
* DESCRIPTION: 		This do file plots validation graphs for economic activity 
* 					status (4 cat). 
******************************************************************************** 
* NOTES: 			
*******************************************************************************/

clear all 

********************************************************************************
* 0 : Programmes
********************************************************************************

* Time series plot, all activity statuses 
cap program drop make_activity_plot

program define make_activity_plot
    syntax, subtitle(string) saving(string) note(string)

    twoway ///
    (rarea sim_employed_high sim_employed_low year, sort color(green%20) legend(label(1 "Employed, SimPaths"))) ///
    (line valid_employed year, sort color(green) legend(label(2 "Employed, UKHLS"))) ///
    (rarea sim_student_high sim_student_low year, sort color(blue%20) legend(label(3 "Students, SimPaths"))) ///
    (line valid_student year, sort color(blue) legend(label(4 "Students, UKHLS"))) ///
    (rarea sim_inactive_high sim_inactive_low year, sort color(red%20) legend(label(5 "Non-employed, SimPaths"))) ///
    (line valid_inactive year, sort color(red) legend(label(6 "Non-employed, UKHLS"))) ///
    (rarea sim_retired_high sim_retired_low year, sort color(grey%20) legend(label(7 "Retired, SimPaths"))) ///
    (line valid_retired year, sort color(grey) legend(label(8 "Retired, UKHLS"))), ///
        title("Economic Activity Status") ///
		subtitle("`subtitle'") ///
        xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		xlabel(, labsize(small)) ///
		ylabel(, labsize(small)) ///			
        graphregion(color(white)) ///
		legend(size(small)) ///
	note(`note', size(vsmall))  
    
	graph export "$dir_output_files/economic_activity/`saving'.jpg", replace width(2400) height(1350) quality(100)

end


* Time series plot, non-employed statuses only 
* Time series plot all 
cap program drop make_activity_ne_plot

program define make_activity_ne_plot
    syntax, subtitle(string) saving(string) note(string)

    twoway ///
    (rarea sim_student_high sim_student_low year, sort color(blue%20) legend(label(1 "Students, SimPaths"))) ///
    (line valid_student year, sort color(blue) legend(label(2 "Students, UKHLS"))) ///
    (rarea sim_inactive_high sim_inactive_low year, sort color(red%20) legend(label(3 "Non-employed, SimPaths"))) ///
    (line valid_inactive year, sort color(red) legend(label(4 "Non-employed, UKHLS"))) ///
    (rarea sim_retired_high sim_retired_low year, sort color(grey%20) legend(label(5 "Retired, SimPaths"))) ///
    (line valid_retired year, sort color(grey) legend(label(6 "Retired, UKHLS"))), ///
        title("Non-Employed Economic Activity Status") ///
		subtitle("`subtitle'") ///
        xtitle("Year", size(small)) ///
		ytitle("Share", size(small)) ///
		xlabel(, labsize(small)) ///
		ylabel(, labsize(small)) ///			
        graphregion(color(white)) ///
		legend(size(small)) ///
	note(`note', size(vsmall))  
    
	graph export "$dir_output_files/economic_activity/`saving'.jpg", replace width(2400) height(1350) quality(100)
end


********************************************************************************
* 1 : Mean values over time
********************************************************************************
********************************************************************************
* 1.1 : Mean values over time - Economic activity status  
********************************************************************************
********************************************************************************
* 1.1.1 : Young people (16-30)
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive demAge  ///
	valid_retired using "$dir_data/ukhls_validation_sample.dta", ///
	clear
	
drop if demAge > 30 
drop if demAge < 16 	

collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demAge using ///
	"$dir_data/simulation_sample.dta", clear

drop if demAge > 30 
drop if demAge < 16  
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)
		 
* Compute 95% confidence interval 		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure 
make_activity_plot, ///
	subtitle("Ages 16-30") ///
	saving("validation_${country}_activity_status_ts_16_30_both") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')

	
********************************************************************************
* 1.1.1.1 : Young people (16-30), by gender
********************************************************************************	

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive demAge demMaleFlag  ///
	valid_retired using "$dir_data/ukhls_validation_sample.dta", ///
	clear
	
drop if demAge > 30 
drop if demAge < 16
	
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demAge ///
	demMaleFlag using "$dir_data/simulation_sample.dta", clear

drop if demAge > 30 
drop if demAge < 16 
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year demMaleFlag)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year demMaleFlag)

* Compute 95% confidence interval		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figures 

* Males 
preserve 

keep if demMaleFlag == 1

make_activity_plot, ///
	subtitle("Ages 16-30, males") ///
	saving("validation_${country}_activity_status_ts_16_30_male") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')

restore 

* Female 

keep if demMaleFlag == 0 

* Plot figure
make_activity_plot, ///
	subtitle("Ages 16-30, females") ///
	saving("validation_${country}_activity_status_ts_16_30_female") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')

	
********************************************************************************
* 1.1.2 : Working age (16-65)
********************************************************************************

* Prepare validation data
use idPers year dwt demMaleFlag demAge valid_employed valid_student ///
	valid_inactive valid_retired using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 	
keep if inrange(demAge,16,65)

collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demMaleFlag ///
	demAge using "$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if inrange(demAge,16,65)	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure 
make_activity_plot, ///
	subtitle("Ages 16-65") ///
	saving("validation_${country}_activity_status_ts_16_65_both") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')

	
********************************************************************************
* 1.1.2.1 : Working age (16-65), by gender
********************************************************************************

* Prepare validation data
use idPers year dwt demMaleFlag demAge valid_employed valid_student ///
	valid_inactive valid_retired using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Select sample 	
keep if inrange(demAge,16,65)

	
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demMaleFlag ///
	demAge using "$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if inrange(demAge,16,65)	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year demMaleFlag)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year demMaleFlag)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figures

* Male
preserve 

keep if demMaleFlag == 1

make_activity_plot, ///
	subtitle("Ages 16-65, males") ///
	saving("validation_${country}_activity_status_ts_16_65_male") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')

restore

* Female

keep if demMaleFlag == 0

make_activity_plot, ///
	subtitle("Ages 16-65, females") ///
	saving("validation_${country}_activity_status_ts_16_65_female") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')

graph drop _all
	
	
********************************************************************************
* 1.1.2.2 : Working age (18-65) by partnership status 
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive valid_retired ///
	demPartnerStatus demMaleFlag demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample 	
keep if inrange(demAge,18,65)	
	
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year demPartnerStatus)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired ///
	demPartnerStatus demMaleFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if inrange(demAge,18,65)	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year demPartnerStatus)

collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year demPartnerStatus)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demPartnerStatus using "$dir_data/temp_valid_stats.dta", ///
	keep(3) nogen
	
* Plot figures 

* Partnered 	
preserve

keep if demPartnerStatus == 1

make_activity_plot, ///
	subtitle("Ages 18-65, partnered") ///
	saving("validation_${country}_activity_status_ts_18_65_both_partnered") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')

restore

* Single 
keep if demPartnerStatus == 2

make_activity_plot, ///
	subtitle("Ages 18-65, single") ///
	saving("validation_${country}_activity_status_ts_18_65_both_single") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')
	
graph drop _all 


********************************************************************************
* 1.1.2.2.1 : Working age (18-65), by partnership status, by gender
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive valid_retired ///
	demPartnerStatus demMaleFlag demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample 	
keep if inrange(demAge,18,65)	
	
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year demPartnerStatus demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired ///
	demPartnerStatus demMaleFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if inrange(demAge,18,65)	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year demPartnerStatus demMaleFlag)

collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year demPartnerStatus demMaleFlag)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demPartnerStatus demMaleFlag using ///
	"$dir_data/temp_valid_stats.dta", keep(3) nogen

	
* Plot figures 
foreach g in 0 1 {
    local gname = cond(`g'==1, "male", "female")
    
    foreach p in 1 2 {
        local pname = cond(`p'==1, "partnered", "single")
        
        preserve
            keep if demMaleFlag == `g' & demPartnerStatus == `p'
            
            make_activity_plot, ///
            subtitle("Ages 18-65, `pname' `gname's") ///
            saving("validation_${country}_activity_status_ts_18_65_`gname'_`pname'") ///
            note(`""Notes: ..." "..." "')
        restore
    }
}

graph drop _all 
	
	
********************************************************************************
* 1.1.3 : Female working age (16-60)
********************************************************************************	

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive demMaleFlag demAge ///
	valid_retired using "$dir_data/ukhls_validation_sample.dta", ///
	clear
	
* Select sample 	
keep if inrange(demAge,16,60)	
	
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demMaleFlag ///
	demAge using "$dir_data/simulation_sample.dta", clear
	
* Select sample 	
keep if inrange(demAge,16,60)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year demMaleFlag)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year demMaleFlag)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Females 
keep if demMaleFlag == 0

* Plot figure 
make_activity_plot, ///
	subtitle("Ages 16-60, females") ///
	saving("validation_${country}_activity_status_ts_16_60_female") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')

	
********************************************************************************
* 1.1.4 : All ages 
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive demMaleFlag ///
	valid_retired demAge labC4 using ///
	"$dir_data/ukhls_validation_sample.dta", clear
		
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demMaleFlag ///
	demAge labC4 using "$dir_data/simulation_sample.dta", clear	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figure 
make_activity_plot, ///
	subtitle("All ages") ///
	saving("validation_${country}_activity_status_ts_all_both") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')
	
graph drop _all	


********************************************************************************
* 1.1.4.1 : All ages, by gender
********************************************************************************
	
* Prepare validation data
use year dwt valid_employed valid_student valid_inactive demMaleFlag ///
	valid_retired demAge labC4 using ///
	"$dir_data/ukhls_validation_sample.dta", clear
		
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demMaleFlag ///
	demAge labC4 using "$dir_data/simulation_sample.dta", clear	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year demMaleFlag)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year demMaleFlag)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen	
	
* Plot figure 	
	
* Males 
preserve
keep if demMaleFlag == 1 

make_activity_plot, ///
	subtitle("All ages, males") ///
	saving("validation_${country}_activity_status_ts_all_male") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')

restore

* Females 
keep if demMaleFlag == 0 

make_activity_plot, ///
	subtitle("All ages, females") ///
	saving("validation_${country}_activity_status_ts_all_female") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')	
	
graph drop _all 	


********************************************************************************
* 1.1.5 : Adult population 18+
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive demMaleFlag demAge ///
	valid_retired using "$dir_data/ukhls_validation_sample.dta", clear
		
* Select sample		
drop if demAge < 18 		
		
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demMaleFlag ///
	demAge using "$dir_data/simulation_sample.dta", clear

drop if demAge < 18		
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(yea)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure 
make_activity_plot, ///
	subtitle("Ages 18+") ///
	saving("validation_${country}_activity_status_ts_18plus_both") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')	
		

********************************************************************************
* 1.1.5.1 : Adult population 18+, by gender
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive demMaleFlag demAge ///
	valid_retired using "$dir_data/ukhls_validation_sample.dta", clear
		
* Select sample		
drop if demAge < 18 		
		
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demMaleFlag ///
	demAge using "$dir_data/simulation_sample.dta", clear

drop if demAge < 18		
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year demMaleFlag)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(yea demMaleFlag)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figures 

* Males 
preserve 

keep if demMaleFlag == 1 

make_activity_plot, ///
	subtitle("Ages 18+, males") ///
	saving("validation_${country}_activity_status_ts_18plus_male") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')	

restore
	
* Females 
keep if demMaleFlag == 0 

make_activity_plot, ///
	subtitle("Ages 18+, females") ///
	saving("validation_${country}_activity_status_ts_18plus_female") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')		

graph drop _all 	


********************************************************************************
* 1.1.6 : Labour supply age group  (16-75)
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive demMaleFlag demAge ///
	valid_retired using "$dir_data/ukhls_validation_sample.dta", ///
	clear
	
* Select sample 	
keep if inrange(demAge,18,75)
	
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demMaleFlag ///
	demAge using "$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if inrange(demAge,18,75)	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figure 
make_activity_plot, ///
	subtitle("Ages 16-75") ///
	saving("validation_${country}_activity_status_ts_16_75_both") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')	

	
********************************************************************************
* 1.1.6.1 : Labour supply age group  (16-75), by gender
********************************************************************************	

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive demMaleFlag demAge ///
	valid_retired using "$dir_data/ukhls_validation_sample.dta", ///
	clear
	
* Select sample 	
keep if inrange(demAge,18,75)
	
collapse (mean) valid_employed valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demMaleFlag ///
	demAge using "$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if inrange(demAge,18,75)	
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired, ///
	by(run year demMaleFlag)
	
collapse (mean) sim_employed sim_student sim_inactive sim_retired ///
		 (sd) sim_employed_sd = sim_employed ///
		 sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year demMaleFlag)
		 
foreach varname in sim_employed sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figures

* Male
preserve 

keep if demMaleFlag == 1 

make_activity_plot, ///
	subtitle("Ages 16-75, males") ///
	saving("validation_${country}_activity_status_ts_16_75_male") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')	

restore

* Female
keep if demMaleFlag == 0 

* Plot figure 
make_activity_plot, ///
	subtitle("Ages 16-75, females") ///
	saving("validation_${country}_activity_status_ts_16_75_female") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired.""')	

graph drop _all	


********************************************************************************
* 1.2 : Mean values over time, share employed
********************************************************************************

********************************************************************************
* 1.2.1 : Mean values over time, share employed, by age group, by gender
********************************************************************************

* Prepare validation data
use year dwt demMaleFlag ageGroup valid_employed demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
gen employed_f = (valid_employed) if demMaleFlag == 0
gen employed_m = (valid_employed) if demMaleFlag == 1

drop if ageGroup == 0 | ageGroup == 8  

collapse (mean) employed_f employed_m [aw=dwt], ///
	by(ageGroup year)
	
drop if missing(ageGroup)

reshape wide employed_f employed_m, i(year) j(ageGroup)

forvalues i = 1(1)7 {
	
	rename employed_f`i' employed_f_`i'_valid
	rename employed_m`i' employed_m_`i'_valid

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demMaleFlag ageGroup sim_employed using ///
	"$dir_data/simulation_sample.dta", clear

gen employed_f = (sim_employed) if demMaleFlag == 0
gen employed_m = (sim_employed) if demMaleFlag == 1

drop if ageGroup == 0 | ageGroup == 8  

collapse (mean)  employed_f employed_m, by(ageGroup run year)
drop if missing(ageGroup)

reshape wide employed_f employed_m, i(year run) j(ageGroup)

forvalues i = 1(1)7 { 
	
	rename employed_f`i' employed_f_`i'_sim
	rename employed_m`i' employed_m_`i'_sim
	
}

collapse (mean) employed* ///
	(sd) sd_employed_f_1_sim=employed_f_1_sim ///
		 sd_employed_f_2_sim=employed_f_2_sim ///
		 sd_employed_f_3_sim=employed_f_3_sim ///
		 sd_employed_f_4_sim=employed_f_4_sim ///
		 sd_employed_f_5_sim=employed_f_5_sim ///
		 sd_employed_f_6_sim=employed_f_6_sim ///
		 sd_employed_f_7_sim=employed_f_7_sim ///
		 sd_employed_m_1_sim=employed_m_1_sim ///
		 sd_employed_m_2_sim=employed_m_2_sim ///
		 sd_employed_m_3_sim=employed_m_3_sim ///
		 sd_employed_m_4_sim=employed_m_4_sim ///
		 sd_employed_m_5_sim=employed_m_5_sim ///
		 sd_employed_m_6_sim=employed_m_6_sim ///
		 sd_employed_m_7_sim=employed_m_7_sim ///
		 , by(year)
		 

forvalues i = 1(1)7 {
	
	gen employed_f_`i'_sim_high = ///
		employed_f_`i'_sim + 1.96*sd_employed_f_`i'_sim
	gen employed_f_`i'_sim_low = ///
		employed_f_`i'_sim - 1.96*sd_employed_f_`i'_sim
	gen employed_m_`i'_sim_high = ///
		employed_m_`i'_sim + 1.96*sd_employed_m_`i'_sim
	gen employed_m_`i'_sim_low = ///
		employed_m_`i'_sim - 1.96*sd_employed_m_`i'_sim	

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figures

* Define the age labels in a local macro
local age_labels `" "16-19" "20-24" "25-29" "30-34" "35-39" "40-59" "60-79" "'

foreach vble in "employed_f" "employed_m" {
    
    *Loop through the 7 age groups
    forvalues i = 1/7 {
        
        * Extract the label for the current index i
        local title : word `i' of `age_labels'
        
        twoway (rarea `vble'_`i'_sim_high `vble'_`i'_sim_low year, ///
            sort color(green%20) ///
            legend(label(1 "SimPaths") position(6) rows(1))) ///
        (line `vble'_`i'_valid year, sort color(green) ///
            legend(label(2 "UKHLS"))), ///
            title("Age `title'") ///
            name(`vble'_`i', replace) ///
            ylabel(0.2(0.4)1) ///  
            xtitle("") ///
            graphregion(color(white)) 
    }

    * Determine gender subtitle for the combined plot
    local gsubtitle = cond("`vble'" == "employed_m", "Males", "Females")
    local gsuffix   = cond("`vble'" == "employed_m", "male", "female")

    * Combine plots
    grc1leg `vble'_1 `vble'_2 `vble'_3 `vble'_4 `vble'_5 `vble'_6 `vble'_7, ///
        title("Share Employed by Age Group") ///
        subtitle("`gsubtitle'") ///
        legendfrom(`vble'_1) ///
		ycomm ///
        graphregion(color(white)) ///
        note("Notes: ", size(vsmall))

    * 5. Export
    graph export "$dir_output_files/economic_activity/validation_${country}_employed_ts_age_groups_`gsuffix'.jpg", ///
        replace width(2400) height(1350) quality(100)
	
}

graph drop _all


********************************************************************************
* 1.3 : Mean values over time, non-employed shares
********************************************************************************

********************************************************************************
* 1.3.1 : Non-employed shares, working age (16-65)
********************************************************************************

* Prepare validation data
use idPers year dwt valid_employed valid_student valid_inactive ///
	valid_retired demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample 
keep if inrange(demAge,16,65)	
	
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demAge ///
	using "$dir_data/simulation_sample.dta", clear
	
* Select sample 
keep if inrange(demAge,16,65)
	
collapse (mean) sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean) sim_student sim_inactive sim_retired ///
		(sd) sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)
		 
foreach varname in sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figure 
make_activity_ne_plot, ///
	subtitle("Ages 16-65") ///
	saving("validation_${country}_activity_status_ts_not_employed_16_65_both") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. Demonimator is the full population.""')	
	
	
********************************************************************************
* 1.3.1.1 : Non-employed shares, Working age (16-65), by gender
********************************************************************************

* Prepare validation data
use idPers year dwt valid_employed valid_student valid_inactive ///
	valid_retired demAge demMaleFlag using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample 
keep if inrange(demAge,16,65)	
	
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demAge ///
	demMaleFlag using "$dir_data/simulation_sample.dta", clear
	
* Select sample 
keep if inrange(demAge,16,65)
	
collapse (mean) sim_student sim_inactive sim_retired, ///
	by(run year demMaleFlag)
	
collapse (mean) sim_student sim_inactive sim_retired ///
		(sd) sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year demMaleFlag)
		 
foreach varname in sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd
	
}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* PLot figures 

* Males
preserve 

keep if demMaleFlag == 1 

make_activity_ne_plot, ///
	subtitle("Ages 16-65, males") ///
	saving("validation_${country}_activity_status_ts_not_employed_16_65_male") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. Demonimator is the full population.""')	

restore

* Females
keep if demMaleFlag == 0 

* Plot figure
make_activity_ne_plot, ///
	subtitle("Ages 16-65, females") ///
	saving("validation_${country}_activity_status_ts_not_employed_16_65_female") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. Demonimator is the full population.""')	

graph drop _all


********************************************************************************
* 1.3.1.2 : Non-employed shares, working age (16-65), by partnership status
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive valid_retired ///
	demPartnerStatus demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample 	
keep if inrange(demAge,18,65)	
	
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year demPartnerStatus)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired ///
	demPartnerStatus demMaleFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if inrange(demAge,18,65)	

* Compute mean and sd
collapse (mean) sim_student sim_inactive sim_retired, ///
	by(run year demPartnerStatus)

collapse (mean)  sim_student sim_inactive sim_retired ///
		 (sd) sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year demPartnerStatus)
	
* Approx 95% confidence interval 	
foreach varname in sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demPartnerStatus using "$dir_data/temp_valid_stats.dta", ///
	keep(3) nogen


* Plot figure

* Partnered 
preserve 

keep if demPartnerStatus == 1

make_activity_ne_plot, ///
	subtitle("Ages 18-65, partnered") ///
	saving("validation_${country}_activity_status_ts_not_employed_18_65_partnered") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. Demonimator is the full population.""')		

restore

* Single
keep if demPartnerStatus == 2

make_activity_ne_plot, ///
	subtitle("Ages 18-65, singles") ///
	saving("validation_${country}_activity_status_ts_not_employed_18_65_single") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. Demonimator is the full population.""')	

graph drop _all 


********************************************************************************
* 1.3.1.3 : Non-employed shares, working age (18-65), by partnership status, 
* 				by gender
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive valid_retired ///
	demPartnerStatus demMaleFlag demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear
	
* Select sample 	
keep if inrange(demAge,18,65)	
	
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year demPartnerStatus demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired ///
	demPartnerStatus demMaleFlag demAge using ///
	"$dir_data/simulation_sample.dta", clear

* Select sample 	
keep if inrange(demAge,18,65)	

* Compute mean and sd
collapse (mean) sim_student sim_inactive sim_retired, ///
	by(run year demPartnerStatus demMaleFlag)

collapse (mean)  sim_student sim_inactive sim_retired ///
		 (sd) sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year demPartnerStatus demMaleFlag)
	
* Approx 95% confidence interval 	
foreach varname in sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demPartnerStatus demMaleFlag using ///
	"$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figures 	

foreach g in 1 0 {
    * Define gender labels
    local gname = cond(`g' == 1, "male", "female")
    local gtitle = cond(`g' == 1, "males", "females")

    foreach p in 1 2 {
        * Define partnership labels
        local pname = cond(`p' == 1, "partnered", "single")
        
        preserve
            * Filter data
            keep if demMaleFlag == `g' & demPartnerStatus == `p'
            
            * Generate the plot
            make_activity_ne_plot, ///
                subtitle("Ages 18-65, `pname' `gtitle'") ///
                saving("validation_${country}_activity_status_ts_not_employed_18_65_`gname'_`pname'") ///
                note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. Demonimator is the full population.""')	
        restore
		
    }
}

graph drop _all

	
********************************************************************************
* 1.3.2 : Non-employed shares, Female working age (16-60)
********************************************************************************
	
* Prepare validation data
use year dwt valid_employed valid_student valid_inactive demMaleFlag demAge ///
	valid_retired using "$dir_data/ukhls_validation_sample.dta", ///
	clear
	
* Select sample 
keep if inrange(demAge,16,60)

drop if demMaleFlag == 1	
drop valid_employed	demMaleFlag 
	
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demMaleFlag ///
	demAge using "$dir_data/simulation_sample.dta", clear
	
* Select sample 
keep if inrange(demAge,16,60)

drop if demMaleFlag == 1
drop sim_employed demMaleFlag 
	
collapse (mean) sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean)  sim_student sim_inactive sim_retired ///
		 (sd) sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)
		 
foreach varname in sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figure 
make_activity_ne_plot, ///
	subtitle("Ages 16-60, females") ///
	saving("validation_${country}_activity_status_ts_not_employed_16_60_female") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. Demonimator is the full population.""')	
	
	
********************************************************************************
* 1.3.3 : Non-employed shares, all ages
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive demMaleFlag ///
	valid_retired using "$dir_data/ukhls_validation_sample.dta", clear
		
drop valid_employed
		
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired ///
	demMaleFlag using "$dir_data/simulation_sample.dta", clear

drop sim_employed	
	
collapse (mean) sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean) sim_student sim_inactive sim_retired ///
		 (sd) sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)
		 
foreach varname in  sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen
 
* Plot figure 
make_activity_ne_plot, ///
	subtitle("All ages") ///
	saving("validation_${country}_activity_status_ts_not_employed_all_both") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. Demonimator is the full population.""')	


********************************************************************************
* 1.3.3.1 : Non-employed shares, all ages, by gender
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive demMaleFlag ///
	valid_retired using "$dir_data/ukhls_validation_sample.dta", clear
		
drop valid_employed
		
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired ///
	demMaleFlag using "$dir_data/simulation_sample.dta", clear

drop sim_employed	
	
collapse (mean) sim_student sim_inactive sim_retired, ///
	by(run year demMaleFlag)
	
collapse (mean) sim_student sim_inactive sim_retired ///
		 (sd) sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year demMaleFlag)
		 
foreach varname in  sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figures 

* Males
preserve 

keep if demMaleFlag == 1 

* Plot figure 
make_activity_ne_plot, ///
	subtitle("All ages, males") ///
	saving("validation_${country}_activity_status_ts_not_employed_all_male") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. Demonimator is the full population.""')

restore
	
* Females 
keep if demMaleFlag == 0 

* Plot figure 
make_activity_ne_plot, ///
	subtitle("All ages, females") ///
	saving("validation_${country}_activity_status_ts_not_employed_all_female") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. Demonimator is the full population.""')	

graph drop _all 


********************************************************************************
* 1.3.4 : Non-employed shares, adult population 18+ 
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive demMaleFlag demAge ///
	valid_retired using "$dir_data/ukhls_validation_sample.dta", clear
		
* Select sample 
drop if demAge < 18
				
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demMaleFlag ///
	demAge using "$dir_data/simulation_sample.dta", clear

* Select sample 
drop if demAge < 18
	
collapse (mean) sim_student sim_inactive sim_retired, ///
	by(run year)
	
collapse (mean) sim_student sim_inactive sim_retired ///
		 (sd) sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year)
		 
foreach varname in  sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen
 
* Plot figure 
make_activity_ne_plot, ///
	subtitle("Ages 18+") ///
	saving("validation_${country}_activity_status_ts_not_employed_18plus_both") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. Demonimator is the full population.""')	

	
********************************************************************************
* 1.3.4.1 : Non-employed shares, adult population 18+, by gender
********************************************************************************

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive demMaleFlag demAge ///
	valid_retired using "$dir_data/ukhls_validation_sample.dta", clear
		
* Select sample 
drop if demAge < 18
				
collapse (mean) valid_student valid_inactive valid_retired ///
	[aw = dwt], by(year demMaleFlag)

save "$dir_data/temp_valid_stats.dta", replace

* Prepare simulated data
use run year sim_employed sim_student sim_inactive sim_retired demMaleFlag ///
	demAge using "$dir_data/simulation_sample.dta", clear

* Select sample 
drop if demAge < 18
	
collapse (mean) sim_student sim_inactive sim_retired, ///
	by(run year demMaleFlag)
	
collapse (mean) sim_student sim_inactive sim_retired ///
		 (sd) sim_student_sd = sim_student ///
		 sim_inactive_sd = sim_inactive ///
		 sim_retired_sd = sim_retired ///
		 , by(year demMaleFlag)
		 
foreach varname in  sim_student sim_inactive sim_retired {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year demMaleFlag using "$dir_data/temp_valid_stats.dta", keep(3) nogen


* Plot figure 

* Males 
preserve

keep if demMaleFlag == 1 

make_activity_ne_plot, ///
	subtitle("Ages 18+, males") ///
	saving("validation_${country}_activity_status_ts_not_employed_18plus_male") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. Demonimator is the full population.""')	

restore

* Females 
keep if demMaleFlag == 0 

make_activity_ne_plot, ///
	subtitle("Ages 18+, females") ///
	saving("validation_${country}_activity_status_ts_not_employed_18plus_female") ///
	note(`""Notes: Non-employed includes the unemployed and inactive (homemakers, incapacity, carers, discouraged workers etc.)" "minus students and retired. Demonimator is the full population.""')		

graph drop _all 


********************************************************************************
* 1.4 Mean values over time, share students 
********************************************************************************

********************************************************************************
* 1.4.1 Share of students, by age group 
********************************************************************************

* Prepare validation data
use year dwt demMaleFlag ageGroup valid_student demAge using ///
	"$dir_data/ukhls_validation_sample.dta", clear

gen student = valid_student

* Select sample
drop if ageGroup == 0 | ageGroup == 8  

* Compute means 
collapse (mean) student [aw=dwt], by(ageGroup year)

drop if missing(ageGroup)

* Restructure data
reshape wide student , i(year) j(ageGroup)

forvalues i = 1(1)7 {
	
	rename student`i' student_`i'_valid

}

save "$dir_data/temp_valid_stats.dta", replace


* Prepare simulated data
use run year demMaleFlag ageGroup sim_student using ///
	"$dir_data/simulation_sample.dta", clear

gen student = sim_student

* Compute means 
collapse (mean) student, by(ageGroup run year)

drop if missing(ageGroup)

* Restructure data 
reshape wide student, i(year run) j(ageGroup)

forvalues i=1(1)7{
	
	rename student`i' student_`i'_sim

}

collapse (mean) student* ///
	(sd) sd_student_1_sim =student_1_sim ///
		 sd_student_2_sim = student_2_sim ///
		 sd_student_3_sim = student_3_sim ///
	     sd_student_4_sim = student_4_sim ///
		 sd_student_5_sim = student_5_sim ///
		 sd_student_6_sim = student_6_sim ///
		 sd_student_7_sim = student_7_sim ///
		 , by(year)


forvalues i = 1(1)7 {
	
	gen student_`i'_sim_high = student_`i'_sim + 1.96*sd_student_`i'_sim
	gen student_`i'_sim_low = student_`i'_sim - 1.96*sd_student_`i'_sim

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta", keep(3) nogen

* Plot figures
* Define the specific age titles in a local macro
local age_titles `" "16-19" "20-24" "25-29" "30-34" "35-39" "40-59" "'

* Loop through the 6 groups
forvalues i = 1/6 {
    
    * Extract the corresponding title from the macro
    local title : word `i' of `age_titles'
    
    twoway (rarea student_`i'_sim_high student_`i'_sim_low year, ///
        sort color(blue%20) ///
        legend(label(1 "SimPaths") position(6) rows(1))) ///
        (line student_`i'_valid year, sort color(blue) ///
        legend(label(2 "UKHLS"))), ///
        title("Age `title'") ///
        name(student_`i', replace) ///
        ylabel(0(0.4)0.8) ///  // Note: Standard Stata syntax is 0(step)max
        xtitle("") ///
        graphregion(color(white))
}

* 3. Combine and Save
grc1leg student_1 student_2 student_3 student_4 student_5 student_6 , ///
    title("Share of Students by Age Group") ///
    legendfrom(student_1) ///
    graphregion(color(white)) ///
    note("Notes: ", size(vsmall))
    
graph export ///
"$dir_output_files/economic_activity/validation_${country}_students_ts_age_groups_both.jpg", ///
    replace width(2400) height(1350) quality(100)

graph drop _all 	


********************************************************************************
* 1.5 Mean values over time -  Partners combined status
********************************************************************************

* LF Non-employed partners with LF Non-employed 

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive valid_retired demAge ///
	demPartnerStatus idPers idPartner idBu idHh ///
	using "$dir_data/ukhls_validation_sample.dta", clear

* Only keep those with a partner 
keep if idPartner != -9 

sort idPartner year 

* Address multiple partners 
gen to_drop = 1 if idPartner == idPartner[_n-1] & year == year[_n-1] 
replace to_drop = 1 if to_drop[_n+1] == 1 & idPartner == idPartner[_n+1]

* Collect partner employement information
preserve 

drop idPartner demAge
rename idPers idPartner  
rename valid_* valid_ptnr_*
rename to_drop to_drop_ptnr

save "$dir_data/temp_valid_partner.dta", replace

restore 
	
* Address multiple partners 	
drop if to_drop == 1 	
	
* Merge in partner info
merge 1:1 year idPartner using "$dir_data/temp_valid_partner.dta"

drop if to_drop_ptnr == 1 	
	
* Only keep those in which partner's info is available	
keep if _m == 3 	

* Select sample 
keep if valid_inactive == 1 

keep if inrange(demAge,18,65) 

collapse (mean) valid_ptnr_inactive [aw=dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

	
* Prepare simulation data
use year sim_employed sim_student sim_inactive sim_retired demAge run ///
	idPers idPartner idBu ///
	using "$dir_data/simulation_sample.dta", clear

* Only keep those with a partner 
keep if idPartner != . 
	
* Collect partner employement information
preserve 

drop idPartner demAge
rename idPers idPartner  
rename sim_* sim_ptnr_*

save "$dir_data/temp_sim_partner.dta", replace

restore 

* Merge in partner info
merge 1:1 year idPartner run using "$dir_data/temp_sim_partner.dta"
drop _m	
	
* Compute share of those who are non-employed and in a partnership whose partner 
* is also non-emplyed compared to working

* Select sample 
keep if sim_inactive == 1 

keep if inrange(demAge,18,65) 
 
	
* Compute mean and sd 	
collapse (mean) sim_ptnr_inactive, by(run year)
	
collapse (mean) sim_ptnr_inactive ///
		 (sd) sim_ptnr_inactive_sd = sim_ptnr_inactive ///
		 , by(year)
		 
* Compute 95% confidence interval 		 
foreach varname in sim_ptnr_inactive {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta"
keep if _m == 3 
drop _m

* Plot
twoway ///
(rarea sim_ptnr_inactive_high sim_ptnr_inactive_low year, sort ///
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_ptnr_inactive year, sort color(green) ///
	legend(label(2 " UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Share of LF non-employed partnered & partner LF non-employed") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(vsmall)) ///
	note("Notes: Ages 18-65 included in sample. Non-employed includes the unemployed and inactive (homemakers, incapacity, carers," "discouraged workers etc.) minus students and retired. ", ///
	size(vsmall))

graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_partnerhip_shares_non_non_18_65.jpg", ///
	replace width(2400) height(1350) quality(100)
	

* LF Non-employed partners with not LF
* Prepare validation data
use year dwt valid_employed valid_student valid_inactive valid_retired ///
	demAge demPartnerStatus idPers idPartner idBu idHh using ///
	"$dir_data/ukhls_validation_sample.dta", clear

* Only keep those with a partner 
keep if idPartner != -9 

sort idPartner year 

* Address multiple partners 
gen to_drop = 1 if idPartner == idPartner[_n-1] & year == year[_n-1] 
replace to_drop = 1 if to_drop[_n+1] == 1 & idPartner == idPartner[_n+1]

* Collect partner employement information
preserve 

drop idPartner demAge
rename idPers idPartner  
rename valid_* valid_ptnr_*
rename to_drop to_drop_ptnr

save "$dir_data/temp_valid_partner.dta", replace

restore 
	
* Address multiple partners 	
drop if to_drop == 1 	
	
* Merge in partner info
merge 1:1 year idPartner using "$dir_data/temp_valid_partner.dta"

drop if to_drop_ptnr == 1 	
	
* Only keep those in which partner's info is available	
keep if _m == 3 	

* Select sample 
keep if valid_inactive == 1 

keep if inrange(demAge,18,65) 

gen valid_partner_nlf = 0 
replace valid_partner_nlf = 1 if valid_ptnr_student == 1 | ///
	valid_ptnr_retired ==1

collapse (mean) valid_partner_nlf [aw=dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

	
* Prepare simulation data
use year sim_employed sim_student sim_inactive sim_retired demAge run ///
	idPers idPartner idBu ///
	using "$dir_data/simulation_sample.dta", clear

* Only keep those with a partner 
keep if idPartner != . 
	
* Collect partner employement information
preserve 

drop idPartner demAge
rename idPers idPartner  
rename sim_* sim_ptnr_*

save "$dir_data/temp_sim_partner.dta", replace

restore 

* Merge in partner info
merge 1:1 year idPartner run using "$dir_data/temp_sim_partner.dta"
drop _m	
	
* Compute share of those who are non-employed and in a partnership whose partner 
* is also non-emplyed compared to working

* Select sample 
keep if sim_inactive == 1 

keep if inrange(demAge,18,65) 
 
gen sim_partner_nlf = 0 
replace sim_partner_nlf = 1 if sim_ptnr_student == 1 | sim_ptnr_retired == 1
	
	
* Compute mean and sd 	
collapse (mean) sim_partner_nlf, by(run year)
	
collapse (mean) sim_partner_nlf ///
		 (sd) sim_partner_nlf_sd = sim_partner_nlf ///
		 , by(year)
		 
* Compute 95% confidence interval 		 
foreach varname in sim_partner_nlf {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

* Combine datasets
merge 1:1 year using "$dir_data/temp_valid_stats.dta"
keep if _m == 3 
drop _m

* Plot
twoway ///
(rarea sim_partner_nlf_high sim_partner_nlf_low year, sort ///
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_partner_nlf year, sort color(green) ///
	legend(label(2 " UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Share of LF non-employed partnered & partner not LF") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(vsmall)) ///
	note("Notes: Ages 18-65 included in sample. Not LF includes student and retired here.", ///
	size(vsmall))

graph export ///
	"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_partnerhip_shares_non_notlf_18_65.jpg", ///
	replace width(2400) height(1350) quality(100)
	
	
* Employed partnered with employed

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive valid_retired ///
	demAge demPartnerStatus idPers idPartner idBu idHh ///
	using "$dir_data/ukhls_validation_sample.dta", clear

* Only keep those with a partner 
keep if idPartner != -9 

sort idPartner year 

* Address multiple partners 
gen to_drop = 1 if idPartner == idPartner[_n-1] & year == year[_n-1] 
replace to_drop = 1 if to_drop[_n+1] == 1 & idPartner == idPartner[_n+1]

* Collect partner employement information
preserve 

drop idPartner demAge
rename idPers idPartner  
rename valid_* valid_ptnr_*
rename to_drop to_drop_ptnr

save "$dir_data/temp_valid_partner.dta", replace

restore 
	
* Address multiple partners 	
drop if to_drop == 1 	
	
* Merge in partner info
merge 1:1 year idPartner using "$dir_data/temp_valid_partner.dta"

drop if to_drop_ptnr == 1 	
	
* Only keep those in which partner's info is available	
keep if _m == 3 	

* Select sample 
keep if valid_employed == 1 

keep if inrange(demAge,18,65) 

collapse (mean) valid_ptnr_employed [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

	
* Prepare simulation data
use year sim_employed sim_student sim_inactive sim_retired demAge run ///
	idPers idPartner idBu ///
	using "$dir_data/simulation_sample.dta", clear

* Only keep those with a partner 
keep if idPartner != . 
	
* Collect partner employement information
preserve 

drop idPartner demAge
rename idPers idPartner  
rename sim_* sim_ptnr_*

save "$dir_data/temp_sim_partner.dta", replace

restore 

* Merge in partner info
merge 1:1 year idPartner run using "$dir_data/temp_sim_partner.dta"
drop _m	
	
* Compute share of those who are non-employed and in a partnership whose partner 
* is also non-emplyed compared to working

* Select sample 
keep if sim_employed == 1  
	
keep if inrange(demAge,18,65) 	
	
* Compute mean and sd 	
collapse (mean) sim_ptnr_employed, by(run year)
	
collapse (mean) sim_ptnr_employed ///
		 (sd) sim_ptnr_employed_sd = sim_ptnr_employed ///
		 , by(year)
		 
* Compute 95% confidence interval 		 
foreach varname in sim_ptnr_employed {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}


merge 1:1 year using "$dir_data/temp_valid_stats.dta"
keep if _m == 3 
drop _m


twoway ///
(rarea sim_ptnr_employed_high sim_ptnr_employed_low year, sort ///
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_ptnr_employed year, sort color(green) ///
	legend(label(2 " UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Share of employed partnered whose partner is also employed") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(vsmall)) ///
	note("Notes: Ages 18-65.  ", ///
	size(vsmall))

graph export ///
	"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_partnerhip_shares_emp_emp_18_65.jpg", ///
	replace width(2400) height(1350) quality(100)
	
	
	
* Employed share patterned with LF non-employed 

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive valid_retired demAge ///
	demPartnerStatus idPers idPartner idBu idHh ///
	using "$dir_data/ukhls_validation_sample.dta", clear

* Only keep those with a partner 
keep if idPartner != -9 

sort idPartner year 

* Address multiple partners 
gen to_drop = 1 if idPartner == idPartner[_n-1] & year == year[_n-1] 
replace to_drop = 1 if to_drop[_n+1] == 1 & idPartner == idPartner[_n+1]

* Collect partner employement information
preserve 

drop idPartner demAge
rename idPers idPartner  
rename valid_* valid_ptnr_*
rename to_drop to_drop_ptnr

save "$dir_data/temp_valid_partner.dta", replace

restore 
	
* Address multiple partners 	
drop if to_drop == 1 	
	
* Merge in partner info
merge 1:1 year idPartner using "$dir_data/temp_valid_partner.dta"

drop if to_drop_ptnr == 1 	
	
* Only keep those in which partner's info is available	
keep if _m == 3 	

* Select sample 
keep if valid_employed == 1 

drop if demAge > 65
drop if demAge < 18  

collapse (mean) valid_ptnr_inactive [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

	
* Prepare simulation data
use year sim_employed sim_student sim_inactive sim_retired demAge run ///
	idPers idPartner idBu ///
	using "$dir_data/simulation_sample.dta", clear

* Only keep those with a partner 
keep if idPartner != . 
	
* Collect partner employement information
preserve 

drop idPartner demAge
rename idPers idPartner  
rename sim_* sim_ptnr_*

save "$dir_data/temp_sim_partner.dta", replace

restore 

* Merge in partner info
merge 1:1 year idPartner run using "$dir_data/temp_sim_partner.dta"
drop _m	
	
* Compute share of those who are non-employed and in a partnership whose partner 
* is also non-emplyed compared to working

* Select sample 
keep if sim_employed == 1 

drop if demAge > 65
drop if demAge < 18  
	
* Compute mean and sd 	
collapse (mean) sim_ptnr_inactive, by(run year)
	
collapse (mean) sim_ptnr_inactive ///
		 (sd) sim_ptnr_inactive_sd = sim_ptnr_inactive ///
		 , by(year)
		 
* Compute 95% confidence interval 		 
foreach varname in sim_ptnr_inactive {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}


merge 1:1 year using "$dir_data/temp_valid_stats.dta"
keep if _m == 3 
drop _m


twoway ///
(rarea sim_ptnr_inactive_high sim_ptnr_inactive_low year, sort ///
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_ptnr_inactive year, sort color(green) ///
	legend(label(2 " UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Share of employed partnered & partner is non-employed") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(vsmall)) ///
	note("Notes: Ages 18-65.", ///
	size(vsmall))	
	
	
graph export ///
	"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_partnerhip_shares_emp_non_18_65.jpg", ///
	replace width(2400) height(1350) quality(100)
	
	
* Employed share patterned not not lf

* Prepare validation data
use year dwt valid_employed valid_student valid_inactive valid_retired ///
	demAge demPartnerStatus idPers idPartner idBu idHh ///
	using "$dir_data/ukhls_validation_sample.dta", clear

* Only keep those with a partner 
keep if idPartner != -9 

sort idPartner year 

* Address multiple partners 
gen to_drop = 1 if idPartner == idPartner[_n-1] & year == year[_n-1] 
replace to_drop = 1 if to_drop[_n+1] == 1 & idPartner == idPartner[_n+1]

* Collect partner employement information
preserve 

drop idPartner demAge
rename idPers idPartner  
rename valid_* valid_ptnr_*
rename to_drop to_drop_ptnr

save "$dir_data/temp_valid_partner.dta", replace

restore 
	
* Address multiple partners 	
drop if to_drop == 1 	
	
* Merge in partner info
merge 1:1 year idPartner using "$dir_data/temp_valid_partner.dta"

drop if to_drop_ptnr == 1 	
	
* Only keep those in which partner's info is available	
keep if _m == 3 	

* Select sample 
keep if valid_employed == 1 

drop if demAge > 65
drop if demAge < 18 

gen valid_ptnr_out = 0 
replace valid_ptnr_out = valid_ptnr_student + valid_ptnr_retired

collapse (mean) valid_ptnr_out [aw = dwt], by(year)

save "$dir_data/temp_valid_stats.dta", replace

	
* Prepare simulation data
use year sim_employed sim_student sim_inactive sim_retired demAge run ///
	idPers idPartner idBu ///
	using "$dir_data/simulation_sample.dta", clear

* Only keep those with a partner 
keep if idPartner != . 
	
* Collect partner employement information
preserve 

drop idPartner demAge
rename idPers idPartner  
rename sim_* sim_ptnr_*

save "$dir_data/temp_sim_partner.dta", replace

restore 

* Merge in partner info
merge 1:1 year idPartner run using "$dir_data/temp_sim_partner.dta"
drop _m	
	
* Compute share of those who are non-employed and in a partnership whose partner 
* is also non-emplyed compared to working

* Select sample 
keep if sim_employed == 1 

drop if demAge > 65
drop if demAge < 18  


gen sim_ptnr_out = 0 
replace sim_ptnr_out = sim_ptnr_student + sim_ptnr_retired

	
* Compute mean and sd 	
collapse (mean) sim_ptnr_out, by(run year)
	
collapse (mean) sim_ptnr_out ///
		 (sd) sim_ptnr_out_sd = sim_ptnr_out ///
		 , by(year)
		 
* Compute 95% confidence interval 		 
foreach varname in sim_ptnr_out {
	
	gen `varname'_high = `varname' + 1.96*`varname'_sd
	gen `varname'_low = `varname' - 1.96*`varname'_sd

}

merge 1:1 year using "$dir_data/temp_valid_stats.dta"
keep if _m == 3 
drop _m

twoway ///
(rarea sim_ptnr_out_high sim_ptnr_out_low year, sort ///
	color(green%20) legend(label(1 "SimPaths"))) ///
(line valid_ptnr_out year, sort color(green) ///
	legend(label(2 " UKHLS"))), ///
	title("Economic Activity Status") ///
	subtitle("Share of employed partnered & partner is not LF") ///
	xtitle("Year", size(small)) ///
	ytitle("Share", size(small)) ///
	graphregion(color(white)) ///
	xlabel(, labsize(small)) ///
	ylabel(, labsize(small)) ///
	legend(size(vsmall)) ///
	note("Notes: Ages 18-65.  ", ///
	size(vsmall))		
	
graph export ///
"$dir_output_files/economic_activity/validation_${country}_activity_status_ts_partnerhip_shares_emp_notlf_18_65.jpg", ///
	replace width(2400) height(1350) quality(100)
	
	
graph drop _all 	
