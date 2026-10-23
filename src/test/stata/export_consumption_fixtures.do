********************************************************************************
* Export Stata predictions for a deterministic synthetic consumption test grid.
* No observed LCFS rows are read or exported by this script.
* Arguments: saved m4 estimates (.ster), then destination fixture CSV.
* Saved estimates are supplied through the team's shared Drive workflow.
* From the repository root, regenerate fixtures after updating FW1a and RMSE:
* do "src/test/stata/export_consumption_fixtures.do" "path/to/model.ster" ///
*     "src/test/resources/simpaths/model/consumption_stata_fixtures.csv"
********************************************************************************
args estimates_file output_file
if (`"`estimates_file'"'=="" | `"`output_file'"'=="") {
    di as error "Supply the saved m4 .ster file and destination CSV."
    exit 198
}
clear all
set more off
set obs 700

* Full grid: 14 age bands x 2 student states x 5 years x 5 income levels.
gen byte age_group=ceil(_n/50)
gen byte age=cond(age_group==1,19,5*age_group+10)
gen byte student=mod(floor((_n-1)/25),2)
gen byte year_case=mod(floor((_n-1)/5),5)
gen int year=2008
replace year=2015 if year_case==1
replace year=2022 if year_case==2
replace year=2023 if year_case==3
replace year=2026 if year_case==4
gen byte income_case=mod(_n-1,5)
gen double income_month=.1
replace income_month=1 if income_case==1
replace income_month=500 if income_case==2
replace income_month=1800 if income_case==3
replace income_month=5000 if income_case==4

* Deterministic control combinations exercise every region and housing status.
gen byte single_female_adult=mod(_n,3)==0
gen byte single_male_adult=mod(_n,3)==1
gen byte na=cond(single_female_adult | single_male_adult,1,2)
gen byte nk=mod(floor((_n-1)/2),4)
gen byte pen_inc_any=age>=65
gen byte ft=(!student & !pen_inc_any & mod(_n,3)==0)+(na==2)
gen byte pt=!student & !pen_inc_any & mod(_n,3)==1
gen byte semp=!student & !pen_inc_any & mod(_n,3)==2
gen byte housing_status=mod(floor((_n-1)/7),3)
gen byte gorx=mod(floor((_n-1)/3),12)+1

* Match Java's GBP1 monthly floor, double square and 2023 trend freeze.
gen double ln_disp_inc=ln(max(income_month,1))
gen double ln_disp_inc2=ln_disp_inc^2
gen int actual_year=year
gen double year_c2015=min(year,2023)-2015
* 2023 is the omitted annual indicator, so it also represents future years'
* all-zero indicator vector while their centred trend remains frozen at 8.
replace year=2023 if actual_year>2023
estimates use `"`estimates_file'"'
predict double stata_xb, xb
gen double stata_rmse=e(rmse)
gen double stata_mean_month=exp(stata_xb+stata_rmse^2/2)
replace year=actual_year
keep student age nk ft pt semp pen_inc_any gorx year age_group housing_status ///
    single_female_adult single_male_adult income_month stata_xb stata_rmse stata_mean_month
assert _N==700
assert !missing(stata_xb,stata_rmse,stata_mean_month)
format stata_xb income_month stata_mean_month stata_rmse %24.17g
export delimited using `"`output_file'"', replace datafmt nolabel
