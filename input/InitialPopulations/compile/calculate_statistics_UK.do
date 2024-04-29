/**********************************************************************/

cd "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations"

tempfile temp

********************************************************************************
*Students by Age
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

*Define age groups
gen ageGroup = .
replace ageGroup = 0 if dag >= 0 & dag <= 14
replace ageGroup = 1 if dag >= 15 & dag <= 19
replace ageGroup = 2 if dag >= 20 & dag <= 24
replace ageGroup = 3 if dag >= 25 & dag <= 29
replace ageGroup = 4 if dag >= 30 & dag <= 34
replace ageGroup = 5 if dag >= 35 & dag <= 39
replace ageGroup = 6 if dag >= 40 & dag <= 59
replace ageGroup = 7 if dag >= 60 & dag <= 79
replace ageGroup = 8 if dag >= 80 & dag <= 100


la def ageGrouplb /// 
	0 "ageGroup_0_14" ///
	1 "ageGroup_15_19" ///
	2 "ageGroup_20_24" ///
	3 "ageGroup_25_29" ///
	4 "ageGroup_30_34" ///
	5 "ageGroup_35_39" ///
	6 "ageGroup_40_59" ///
	7 "ageGroup_60_79" ///
	8 "ageGroup_80_100" ///
	
la val ageGroup ageGrouplb

gen student = (les_c3 == 2)
cap drop _*

preserve
collapse (mean) mean=student [aweight=dwt], by(stm)
rename mean ageGroup_All
save `temp', replace
restore

collapse (mean) mean=student [aweight=dwt], by(ageGroup stm)
drop if missing(ageGroup)
reshape wide mean, i(stm) j(ageGroup)

rename mean0 ageGroup_0_14
rename mean1 ageGroup_15_19 
rename mean2 ageGroup_20_24
rename mean3 ageGroup_25_29
rename mean4 ageGroup_30_34
rename mean5 ageGroup_35_39
rename mean6 ageGroup_40_59
rename mean7 ageGroup_60_79
rename mean8 ageGroup_80_100

merge 1:1 stm using `temp', nogen

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_studentsByAge") firstrow(variables) replace

********************************************************************************
*Students by Region
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

/*
*Define regions
*Regions defined in the drgn1 variable:
	UKC("North East", 1),					//North East					//1
	UKD("North West", 2),					//North West					//2
	UKE("Yorkshire and the Humber", 4),		//Yorkshire and the Humber		//4		//Note, there is no 3 in the definition!
	UKF("East Midlands", 5),				//East Midlands					//5
	UKG("West Midlands", 6),				//West Midlands					//6
	UKH("East of England", 7),				//East of England				//7
	UKI("London", 8),						//London						//8
	UKJ("South East", 9),					//South East					//9
	UKK("South West", 10),					//South West					//10
	UKL("Wales", 11),						//Wales							//11
	UKM("Scotland", 12),					//Scotland						//12
	UKN("Northern Ireland", 13);			//Northern Ireland				//13?

*/

la def drgn1_lb /// 
	1 "region_UKC" ///
	2 "region_UKD" ///
	4 "region_UKE" ///
	5 "region_UKF" ///
	6 "region_UKG" ///
	7 "region_UKH" ///
	8 "region_UKI" ///
	9 "region_UKJ" ///
	10 "region_UKK" ///
	11 "region_UKL" ///
	12 "region_UKM" ///
	13 "region_UKN" ///

la val drgn1 drgn1_lb

gen student = (les_c3 == 2)
cap drop _*

preserve
collapse (mean) mean=student [aweight=dwt], by(stm)
rename mean region_All
save `temp', replace
restore

collapse (mean) mean=student [aweight=dwt], by(drgn1 stm)
drop if missing(drgn1)
reshape wide mean, i(stm) j(drgn1)

rename mean1 region_UKC
rename mean2 region_UKD
rename mean4 region_UKE
rename mean5 region_UKF
rename mean6 region_UKG
rename mean7 region_UKH
rename mean8 region_UKI
rename mean9 region_UKJ
rename mean10 region_UKK
rename mean11 region_UKL
rename mean12 region_UKM
rename mean13 region_UKN

merge 1:1 stm using `temp', nogen

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_studentsByRegion", replace) firstrow(variables)

********************************************************************************
*Education level over 17 years old (excluding students)
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear



la def deh_c3_lb /// 
	1 "High" ///
	2 "Medium" ///
	3 "Low"


la val deh_c3 deh_c3_lb

gen educ_high = (deh_c3 == 1)
gen educ_med = (deh_c3 == 2)
gen educ_low = (deh_c3 == 3)

gen student = (les_c3 == 2)
cap drop _*

drop if student == 1
drop if dag < 18


collapse (mean) mean1=educ_high mean2=educ_med mean3=educ_low [aweight=dwt], by(stm)

rename mean1 educ_high
rename mean2 educ_med
rename mean3 educ_low

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_educationLevel", replace) firstrow(variables)

********************************************************************************
*Activity status
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear


la def les_c4_lb /// 
	1 "Employed" ///
	2 "Student" ///
	3 "Not employed" ///
	4 "Retired"


la val les_c4 les_c4_lb

gen as_employed = (les_c4 == 1)
gen as_student = (les_c4 == 2)
gen as_notemployedretired = (les_c4 == 3 | les_c4 == 4)
*gen as_retired = (les_c4 == 4)

cap drop _*

collapse (mean) as_* [aweight=dwt], by(stm)

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_activityStatus", replace) firstrow(variables)

********************************************************************************
*Education level by age group
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

*Define age groups
gen ageGroup = .
replace ageGroup = 0 if dag >= 0 & dag <= 9
replace ageGroup = 1 if dag >= 10 & dag <= 19
replace ageGroup = 2 if dag >= 20 & dag <= 29
replace ageGroup = 3 if dag >= 30 & dag <= 39
replace ageGroup = 4 if dag >= 40 & dag <= 49
replace ageGroup = 5 if dag >= 50 & dag <= 59
replace ageGroup = 6 if dag >= 60 & dag <= 69
replace ageGroup = 7 if dag >= 70 & dag <= 79
replace ageGroup = 8 if dag >= 80 & dag <= 100


la def ageGrouplb /// 
	0 "ageGroup_0_9" ///
	1 "ageGroup_10_19" ///
	2 "ageGroup_20_29" ///
	3 "ageGroup_30_39" ///
	4 "ageGroup_40_49" ///
	5 "ageGroup_50_59" ///
	6 "ageGroup_60_69" ///
	7 "ageGroup_70_79" ///
	8 "ageGroup_80_100" ///
	
la val ageGroup ageGrouplb

la def deh_c3_lb /// 
	1 "High" ///
	2 "Medium" ///
	3 "Low"


la val deh_c3 deh_c3_lb

gen educ_high = (deh_c3 == 1)
gen educ_med = (deh_c3 == 2)
gen educ_low = (deh_c3 == 3)

gen student = (les_c3 == 2)
cap drop _*

drop if student == 1
drop if dag < 18


collapse (mean) educ_high = educ_high educ_med=educ_med educ_low=educ_low [aweight=dwt], by(ageGroup stm)
drop if missing(ageGroup)
reshape wide educ*, i(stm) j(ageGroup)

foreach var in educ_high educ_med educ_low {
rename `var'1 `var'_10_19 
rename `var'2 `var'_20_29
rename `var'3 `var'_30_39
rename `var'4 `var'_40_49
rename `var'5 `var'_50_59
rename `var'6 `var'_60_69
rename `var'7 `var'_70_79
rename `var'8 `var'_80_100
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_educationLevelByAge", replace) firstrow(variables)

********************************************************************************
*Education level by region
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

la def drgn1_lb /// 
	1 "region_UKC" ///
	2 "region_UKD" ///
	4 "region_UKE" ///
	5 "region_UKF" ///
	6 "region_UKG" ///
	7 "region_UKH" ///
	8 "region_UKI" ///
	9 "region_UKJ" ///
	10 "region_UKK" ///
	11 "region_UKL" ///
	12 "region_UKM" ///
	13 "region_UKN" ///

la val drgn1 drgn1_lb

la def deh_c3_lb /// 
	1 "High" ///
	2 "Medium" ///
	3 "Low"


la val deh_c3 deh_c3_lb

gen educ_high = (deh_c3 == 1)
gen educ_med = (deh_c3 == 2)
gen educ_low = (deh_c3 == 3)

gen student = (les_c4 == 2)
cap drop _*

*drop if student == 1
*drop if dag < 18


collapse (mean) educ_high = educ_high educ_med=educ_med educ_low=educ_low [aweight=dwt], by(drgn1 stm)
drop if missing(drgn1)
reshape wide educ*, i(stm) j(drgn1)

foreach var in educ_high educ_med educ_low {
rename `var'1 `var'_UKC
rename `var'2 `var'_UKD
rename `var'4 `var'_UKE
rename `var'5 `var'_UKF
rename `var'6 `var'_UKG
rename `var'7 `var'_UKH
rename `var'8 `var'_UKI
rename `var'9 `var'_UKJ
rename `var'10 `var'_UKK
rename `var'11 `var'_UKL
rename `var'12 `var'_UKM
rename `var'13 `var'_UKN
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_educationLevelByRegion", replace) firstrow(variables)

********************************************************************************
*BenefitUnit couple occupancy by region
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

la def drgn1_lb /// 
	1 "region_UKC" ///
	2 "region_UKD" ///
	4 "region_UKE" ///
	5 "region_UKF" ///
	6 "region_UKG" ///
	7 "region_UKH" ///
	8 "region_UKI" ///
	9 "region_UKJ" ///
	10 "region_UKK" ///
	11 "region_UKL" ///
	12 "region_UKM" ///
	13 "region_UKN" ///

la val drgn1 drgn1_lb

recode dhhtp_c4 (1/2 = 1) (3/4 = 0), gen(partnered)

duplicates drop stm idbenefitunit partnered, force

preserve
collapse (mean) partnered [aweight=dwt], by(stm)
rename partnered partnered_All
save `temp', replace
restore

collapse (mean) partnered [aweight=dwt], by(drgn1 stm)
drop if missing(drgn1)
reshape wide partnered, i(stm) j(drgn1)

foreach var in partnered {
rename `var'1 `var'_UKC
rename `var'2 `var'_UKD
rename `var'4 `var'_UKE
rename `var'5 `var'_UKF
rename `var'6 `var'_UKG
rename `var'7 `var'_UKH
rename `var'8 `var'_UKI
rename `var'9 `var'_UKJ
rename `var'10 `var'_UKK
rename `var'11 `var'_UKL
rename `var'12 `var'_UKM
rename `var'13 `var'_UKN
}

merge 1:1 stm using `temp', nogen
rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_partneredBUShareByRegion", replace) firstrow(variables)

********************************************************************************
*Disability by gender
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

cap drop if dgn == -9
drop if dag < 16 

la def dgn_lb /// 
		0 "Female" ///
		1 "Male"

la val dgn dgn_lb


collapse (mean) dlltsd [aweight=dwt], by(dgn stm)
drop if missing(dgn)
reshape wide dlltsd*, i(stm) j(dgn)

foreach var in dlltsd {
rename `var'0 `var'_female
rename `var'1 `var'_male
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_disabledByGender", replace) firstrow(variables)

********************************************************************************
*Disability by age group and gender
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*
drop if dag < 16 

la def dgn_lb /// 
		0 "Female" ///
		1 "Male"

la val dgn dgn_lb

*Define age groups
gen ageGroup = .
replace ageGroup = 0 if dag >= 0 & dag <= 49
replace ageGroup = 1 if dag >= 50 & dag <= 74
replace ageGroup = 2 if dag >= 75 & dag <= 100

la def ageGrouplb /// 
	0 "ageGroup_0_49" ///
	1 "ageGroup_50_74" ///
	2 "ageGroup_75_100"

la val ageGroup ageGrouplb

gen disabled_female = (dlltsd == 1) if dgn == 0
gen disabled_male = (dlltsd == 1) if dgn == 1

collapse (mean) disabled_female disabled_male [aweight=dwt], by(ageGroup stm)
drop if missing(ageGroup)
reshape wide disabled*, i(stm) j(ageGroup)

foreach var in disabled_female disabled_male {
rename `var'0 `var'_0_49 
rename `var'1 `var'_50_74
rename `var'2 `var'_75_100
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_disabledByAgeGroup", replace) firstrow(variables)

********************************************************************************
*Health by age group and gender
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

la def dgn_lb /// 
		0 "Female" ///
		1 "Male"

la val dgn dgn_lb

*Define age groups
gen ageGroup = .
replace ageGroup = 0 if dag >= 0 & dag <= 49
replace ageGroup = 1 if dag >= 50 & dag <= 74
replace ageGroup = 2 if dag >= 75 & dag <= 100

la def ageGrouplb /// 
	0 "ageGroup_0_49" ///
	1 "ageGroup_50_74" ///
	2 "ageGroup_75_100"

la val ageGroup ageGrouplb

gen health_female = dhe if dgn == 0
gen health_male = dhe if dgn == 1

collapse (mean) health_female health_male [aweight=dwt], by(ageGroup stm)
drop if missing(ageGroup)
reshape wide health*, i(stm) j(ageGroup)

foreach var in health_female health_male {
rename `var'0 `var'_0_49 
rename `var'1 `var'_50_74
rename `var'2 `var'_75_100
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_healthByAgeGroup", replace) firstrow(variables)

********************************************************************************
*Mental health score by age group and gender
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

la def dgn_lb /// 
		0 "Female" ///
		1 "Male"

la val dgn dgn_lb

*Define age groups
gen ageGroup = .
replace ageGroup = 0 if dag >= 0 & dag <= 9
replace ageGroup = 1 if dag >= 10 & dag <= 19
replace ageGroup = 2 if dag >= 20 & dag <= 29
replace ageGroup = 3 if dag >= 30 & dag <= 39
replace ageGroup = 4 if dag >= 40 & dag <= 49
replace ageGroup = 5 if dag >= 50 & dag <= 59
replace ageGroup = 6 if dag >= 60 & dag <= 69
replace ageGroup = 7 if dag >= 70 & dag <= 79
replace ageGroup = 8 if dag >= 80 & dag <= 100


la def ageGrouplb /// 
	0 "ageGroup_0_9" ///
	1 "ageGroup_10_19" ///
	2 "ageGroup_20_29" ///
	3 "ageGroup_30_39" ///
	4 "ageGroup_40_49" ///
	5 "ageGroup_50_59" ///
	6 "ageGroup_60_69" ///
	7 "ageGroup_70_79" ///
	8 "ageGroup_80_100" ///
	
la val ageGroup ageGrouplb

gen mental_health_female = dhm if dgn == 0
gen mental_health_male = dhm if dgn == 1

collapse (mean) mental_health_female mental_health_male [aweight=dwt], by(ageGroup stm)
drop if missing(ageGroup)
reshape wide mental_health*, i(stm) j(ageGroup)

foreach var in mental_health_female mental_health_male {
rename `var'0 `var'_0_9
rename `var'1 `var'_10_19 
rename `var'2 `var'_20_29
rename `var'3 `var'_30_39
rename `var'4 `var'_40_49
rename `var'5 `var'_50_59
rename `var'6 `var'_60_69
rename `var'7 `var'_70_79
rename `var'8 `var'_80_100
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_mentalHealthByAgeGroup", replace) firstrow(variables)

********************************************************************************
*Psychological distress (caseness) by age group and gender
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

la def dgn_lb /// 
		0 "Female" ///
		1 "Male"

la val dgn dgn_lb

*Define age groups
gen ageGroup = .
replace ageGroup = 0 if dag >= 0 & dag <= 9
replace ageGroup = 1 if dag >= 10 & dag <= 19
replace ageGroup = 2 if dag >= 20 & dag <= 29
replace ageGroup = 3 if dag >= 30 & dag <= 39
replace ageGroup = 4 if dag >= 40 & dag <= 49
replace ageGroup = 5 if dag >= 50 & dag <= 59
replace ageGroup = 6 if dag >= 60 & dag <= 69
replace ageGroup = 7 if dag >= 70 & dag <= 79
replace ageGroup = 8 if dag >= 80 & dag <= 100


la def ageGrouplb /// 
	0 "ageGroup_0_9" ///
	1 "ageGroup_10_19" ///
	2 "ageGroup_20_29" ///
	3 "ageGroup_30_39" ///
	4 "ageGroup_40_49" ///
	5 "ageGroup_50_59" ///
	6 "ageGroup_60_69" ///
	7 "ageGroup_70_79" ///
	8 "ageGroup_80_100" ///
	
la val ageGroup ageGrouplb

keep if !missing(scghq2_dv)

gen psych_distress_female = (dhm_ghq == 1) if dgn == 0
gen psych_distress_male = (dhm_ghq == 1) if dgn == 1

collapse (mean) psych_distress_female psych_distress_male [aweight=dwt], by(ageGroup stm)
drop if missing(ageGroup)
reshape wide psych_distress*, i(stm) j(ageGroup)

foreach var in psych_distress_female psych_distress_male {
cap rename `var'0 `var'_0_9
rename `var'1 `var'_10_19 
rename `var'2 `var'_20_29
rename `var'3 `var'_30_39
rename `var'4 `var'_40_49
rename `var'5 `var'_50_59
rename `var'6 `var'_60_69
rename `var'7 `var'_70_79
rename `var'8 `var'_80_100
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_psychDistressByAgeGroup", replace) firstrow(variables)

********************************************************************************
*Psychological distress (caseness) by age group and gender, low education
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

keep if deh_c3 == 3 // Low education

la def dgn_lb /// 
		0 "Female" ///
		1 "Male"

la val dgn dgn_lb

*Define age groups
gen ageGroup = .
replace ageGroup = 0 if dag >= 0 & dag <= 9
replace ageGroup = 1 if dag >= 10 & dag <= 19
replace ageGroup = 2 if dag >= 20 & dag <= 29
replace ageGroup = 3 if dag >= 30 & dag <= 39
replace ageGroup = 4 if dag >= 40 & dag <= 49
replace ageGroup = 5 if dag >= 50 & dag <= 59
replace ageGroup = 6 if dag >= 60 & dag <= 69
replace ageGroup = 7 if dag >= 70 & dag <= 79
replace ageGroup = 8 if dag >= 80 & dag <= 100


la def ageGrouplb /// 
	0 "ageGroup_0_9" ///
	1 "ageGroup_10_19" ///
	2 "ageGroup_20_29" ///
	3 "ageGroup_30_39" ///
	4 "ageGroup_40_49" ///
	5 "ageGroup_50_59" ///
	6 "ageGroup_60_69" ///
	7 "ageGroup_70_79" ///
	8 "ageGroup_80_100" ///
	
la val ageGroup ageGrouplb

keep if !missing(scghq2_dv)

gen psych_distress_female = (dhm_ghq == 1) if dgn == 0
gen psych_distress_male = (dhm_ghq == 1) if dgn == 1

collapse (mean) psych_distress_female psych_distress_male [aweight=dwt], by(ageGroup stm)
drop if missing(ageGroup)
reshape wide psych_distress*, i(stm) j(ageGroup)

foreach var in psych_distress_female psych_distress_male {
cap rename `var'0 `var'_0_9
cap rename `var'1 `var'_10_19 
rename `var'2 `var'_20_29
rename `var'3 `var'_30_39
rename `var'4 `var'_40_49
rename `var'5 `var'_50_59
rename `var'6 `var'_60_69
rename `var'7 `var'_70_79
rename `var'8 `var'_80_100
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_psychDistressByAgeGroupLowED", replace) firstrow(variables)

********************************************************************************
*Psychological distress (caseness) by age group and gender, low education
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

keep if deh_c3 == 2 // Low education

la def dgn_lb /// 
		0 "Female" ///
		1 "Male"

la val dgn dgn_lb

*Define age groups
gen ageGroup = .
replace ageGroup = 0 if dag >= 0 & dag <= 9
replace ageGroup = 1 if dag >= 10 & dag <= 19
replace ageGroup = 2 if dag >= 20 & dag <= 29
replace ageGroup = 3 if dag >= 30 & dag <= 39
replace ageGroup = 4 if dag >= 40 & dag <= 49
replace ageGroup = 5 if dag >= 50 & dag <= 59
replace ageGroup = 6 if dag >= 60 & dag <= 69
replace ageGroup = 7 if dag >= 70 & dag <= 79
replace ageGroup = 8 if dag >= 80 & dag <= 100


la def ageGrouplb /// 
	0 "ageGroup_0_9" ///
	1 "ageGroup_10_19" ///
	2 "ageGroup_20_29" ///
	3 "ageGroup_30_39" ///
	4 "ageGroup_40_49" ///
	5 "ageGroup_50_59" ///
	6 "ageGroup_60_69" ///
	7 "ageGroup_70_79" ///
	8 "ageGroup_80_100" ///
	
la val ageGroup ageGrouplb

keep if !missing(scghq2_dv)

gen psych_distress_female = (dhm_ghq == 1) if dgn == 0
gen psych_distress_male = (dhm_ghq == 1) if dgn == 1

collapse (mean) psych_distress_female psych_distress_male [aweight=dwt], by(ageGroup stm)
drop if missing(ageGroup)
reshape wide psych_distress*, i(stm) j(ageGroup)

foreach var in psych_distress_female psych_distress_male {
cap rename `var'0 `var'_0_9
cap rename `var'1 `var'_10_19 
rename `var'2 `var'_20_29
rename `var'3 `var'_30_39
rename `var'4 `var'_40_49
rename `var'5 `var'_50_59
rename `var'6 `var'_60_69
rename `var'7 `var'_70_79
rename `var'8 `var'_80_100
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_psychDistressByAgeGroupMedED", replace) firstrow(variables)

********************************************************************************
*Psychological distress (caseness) by age group and gender, high education
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

keep if deh_c3 == 1 // Low education

la def dgn_lb /// 
		0 "Female" ///
		1 "Male"

la val dgn dgn_lb

*Define age groups
gen ageGroup = .
replace ageGroup = 0 if dag >= 0 & dag <= 9
replace ageGroup = 1 if dag >= 10 & dag <= 19
replace ageGroup = 2 if dag >= 20 & dag <= 29
replace ageGroup = 3 if dag >= 30 & dag <= 39
replace ageGroup = 4 if dag >= 40 & dag <= 49
replace ageGroup = 5 if dag >= 50 & dag <= 59
replace ageGroup = 6 if dag >= 60 & dag <= 69
replace ageGroup = 7 if dag >= 70 & dag <= 79
replace ageGroup = 8 if dag >= 80 & dag <= 100


la def ageGrouplb /// 
	0 "ageGroup_0_9" ///
	1 "ageGroup_10_19" ///
	2 "ageGroup_20_29" ///
	3 "ageGroup_30_39" ///
	4 "ageGroup_40_49" ///
	5 "ageGroup_50_59" ///
	6 "ageGroup_60_69" ///
	7 "ageGroup_70_79" ///
	8 "ageGroup_80_100" ///
	
la val ageGroup ageGrouplb

keep if !missing(scghq2_dv)

gen psych_distress_female = (dhm_ghq == 1) if dgn == 0
gen psych_distress_male = (dhm_ghq == 1) if dgn == 1

collapse (mean) psych_distress_female psych_distress_male [aweight=dwt], by(ageGroup stm)
drop if missing(ageGroup)
reshape wide psych_distress*, i(stm) j(ageGroup)

foreach var in psych_distress_female psych_distress_male {
cap rename `var'0 `var'_0_9
cap rename `var'1 `var'_10_19 
rename `var'2 `var'_20_29
rename `var'3 `var'_30_39
rename `var'4 `var'_40_49
rename `var'5 `var'_50_59
rename `var'6 `var'_60_69
rename `var'7 `var'_70_79
rename `var'8 `var'_80_100
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_psychDistressByAgeGroupHiED", replace) firstrow(variables)

********************************************************************************
*Employment rate by gender
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

la def dgn_lb /// 
		0 "Female" ///
		1 "Male"

la val dgn dgn_lb

keep if dag >= 18 & dag <= 64

*TODO: students in the denominator? Remove if so and check with simulation. Retired?

*Employed: les_c4 == 1 == EmployedOrSelfEmployed
gen employed = (les_c4 == 1)
*replace employed = 1 if yplgrs_dv > 0 & !missing(yplgrs_dv)


collapse (mean) employed [aweight=dwt], by(dgn stm)
drop if missing(dgn)
reshape wide employed, i(stm) j(dgn)

foreach var in employed {
rename `var'0 `var'_Female 
rename `var'1 `var'_Male
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_employmentByGender", replace) firstrow(variables)

********************************************************************************
*Employment by age group and gender
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

la def dgn_lb /// 
		0 "Female" ///
		1 "Male"

la val dgn dgn_lb


*Define age groups
gen ageGroup = .
replace ageGroup = 0 if dag >= 0 & dag <= 9
replace ageGroup = 1 if dag >= 10 & dag <= 19
replace ageGroup = 2 if dag >= 20 & dag <= 29
replace ageGroup = 3 if dag >= 30 & dag <= 39
replace ageGroup = 4 if dag >= 40 & dag <= 49
replace ageGroup = 5 if dag >= 50 & dag <= 59
replace ageGroup = 6 if dag >= 60 & dag <= 69
replace ageGroup = 7 if dag >= 70 & dag <= 79
replace ageGroup = 8 if dag >= 80 & dag <= 100


la def ageGrouplb /// 
	0 "ageGroup_0_9" ///
	1 "ageGroup_10_19" ///
	2 "ageGroup_20_29" ///
	3 "ageGroup_30_39" ///
	4 "ageGroup_40_49" ///
	5 "ageGroup_50_59" ///
	6 "ageGroup_60_69" ///
	7 "ageGroup_70_79" ///
	8 "ageGroup_80_100" ///
	
la val ageGroup ageGrouplb


/*
gen ageGroup = .
replace ageGroup = 2 if dag >= 18 & dag <= 29
replace ageGroup = 3 if dag >= 30 & dag <= 39
replace ageGroup = 4 if dag >= 40 & dag <= 49
replace ageGroup = 5 if dag >= 50 & dag <= 64

la def ageGrouplb2 /// 
	2 "ageGroup_18_29" ///
	3 "ageGroup_30_39" ///
	4 "ageGroup_40_49" ///
	5 "ageGroup_50_64" ///
	
la val ageGroup ageGrouplb2
*/

gen employed_female = (les_c4 == 1) if dgn == 0 
gen employed_male = (les_c4 == 1) if dgn == 1 

collapse (mean) employed_female employed_male [aweight=dwt], by(ageGroup stm)
drop if missing(ageGroup)
reshape wide employed*, i(stm) j(ageGroup)


foreach var in employed_female employed_male {
rename `var'0 `var'_0_9 
rename `var'1 `var'_10_19 
rename `var'2 `var'_20_29
rename `var'3 `var'_30_39
rename `var'4 `var'_40_49
rename `var'5 `var'_50_59
rename `var'6 `var'_60_69
rename `var'7 `var'_70_79
rename `var'8 `var'_80_100
}


rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_employmentByGenderAndAge", replace) firstrow(variables)

********************************************************************************
*Employment of females by age of children in the benefit unit
********************************************************************************

*These statistics are a bit weird - need to be double checked. 

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

gen child_0_5 = (dag <= 5 & !missing(dag))
gen child_6_18 = (dag <= 18 & dag >= 6 & !missing(dag))

bys idbenefitunit: egen max_child_0_5 = max(child_0_5)
bys idbenefitunit: egen max_child_6_18 = max(child_6_18)

*gen retired  = (ypnoab > 0 & dag >= 50 & les_c3 != 1)
gen emp_with_child_0_5 = (les_c4 == 1) if dgn == 0 & max_child_0_5 == 1 & dag >= 20 & dag <= 65 
gen emp_with_child_6_18 = (les_c4 == 1) if dgn == 0 & max_child_6_18 == 1 & dag >= 20 & dag <= 65 
gen emp_without_child = (les_c4 == 1) if dgn == 0 & max_child_6_18 == 0 & max_child_0_5 == 0 & dag >= 20 & dag <= 65

cap drop max* child*

collapse (mean) emp* [aweight=dwt], by(stm)
drop if missing(stm)

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_employmentByMaternity", replace) firstrow(variables)

********************************************************************************
*Employment by gender and region
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

la def drgn1_lb /// 
	1 "region_UKC" ///
	2 "region_UKD" ///
	4 "region_UKE" ///
	5 "region_UKF" ///
	6 "region_UKG" ///
	7 "region_UKH" ///
	8 "region_UKI" ///
	9 "region_UKJ" ///
	10 "region_UKK" ///
	11 "region_UKL" ///
	12 "region_UKM" ///
	13 "region_UKN" ///

la val drgn1 drgn1_lb

keep if dag >= 18 & dag <= 64

*gen retired  = (ypnoab > 0 & dag >= 50 & les_c3 != 1) => Retired now in les_c4
gen employed_female = (les_c4 == 1) if dgn == 0 
gen employed_male = (les_c4 == 1) if dgn == 1 

collapse (mean) employed_female employed_male [aweight=dwt], by(drgn1 stm)
drop if missing(drgn1)
reshape wide employed*, i(stm) j(drgn1)

foreach var in employed_female employed_male {
rename `var'1 `var'_UKC
rename `var'2 `var'_UKD
rename `var'4 `var'_UKE
rename `var'5 `var'_UKF
rename `var'6 `var'_UKG
rename `var'7 `var'_UKH
rename `var'8 `var'_UKI
rename `var'9 `var'_UKJ
rename `var'10 `var'_UKK
rename `var'11 `var'_UKL
rename `var'12 `var'_UKM
rename `var'13 `var'_UKN
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_employmentByGenderAndRegion", replace) firstrow(variables)

********************************************************************************
*Average yearly labour supply by education (for individuals flexible in labour supply)
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

recode lhw (-9 = .)

la def deh_c3_lb /// 
	1 "High" ///
	2 "Medium" ///
	3 "Low"

la val deh_c3 deh_c3_lb
drop if missing(deh_c3) | deh_c3 == -9
keep if dag >= 16 & dag <= 75 & les_c4 != 2 & les_c4 != 4 & dlltsd != 1 // Keep those flexible in labour supply

gen labour_supply = lhw * 4.33 * 12

/*
qui sum dwt
local mean_dwt = r(mean)
replace dwt = dwt/`mean_dwt'
*/

collapse (mean) labour_supply [aweight=dwt], by(deh_c3 stm)

reshape wide labour_supply, i(stm) j(deh_c3)

foreach var in labour_supply {
rename `var'1 `var'_High 
rename `var'2 `var'_Medium
rename `var'3 `var'_Low
}


rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_labourSupplyByEducation", replace) firstrow(variables)

********************************************************************************
*Homeownership 
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear


cap drop _*

collapse (mean) dhh_owned [aweight=dwt], by(idbenefitunit stm)
collapse (mean) dhh_owned, by(stm)

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_homeownership", replace) firstrow(variables)

********************************************************************************
*Gross earnings yearly by education and gender (for employed persons)
********************************************************************************
foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

la def dgn_lb /// 
		0 "Female" ///
		1 "Male"

la val dgn dgn_lb

la def deh_c3_lb /// 
	1 "High" ///
	2 "Medium" ///
	3 "Low"

la val deh_c3 deh_c3_lb

keep if les_c3 == 1
drop if missing(deh_c3) | deh_c3 == -9

*Monetary values in the estimation sample are in 2015 values. Uprate by the CPI. 
gen CPI = .
replace CPI = 0.879 if stm == 2009
replace CPI = 0.901 if stm == 2010
replace CPI = 0.936 if stm == 2011
replace CPI = 0.96  if stm == 2012
replace CPI = 0.982 if stm == 2013
replace CPI = 0.996 if stm == 2014
replace CPI = 1     if stm == 2015
replace CPI = 1.01  if stm == 2016
replace CPI = 1.036 if stm == 2017
replace CPI = 1.06  if stm == 2018
replace CPI = 1.078 if stm == 2019
replace CPI = 1.089 if stm == 2020
replace CPI = 1.116 if stm == 2021
replace CPI = 1.205 if stm == 2022
replace CPI = 1.286 if stm == 2023


gen grossearnings = sinh(yplgrs_dv)*12*CPI
gen hourlywage = sinh(yplgrs_dv)*CPI/(lhw*4.33) 
drop if hourlywage <= 1 | hourlywage >= 1000
drop if grossearnings <= 50 | grossearnings >= 1000000
gen grossearnings_female = grossearnings if dgn == 0 
gen grossearnings_male = grossearnings if dgn == 1 

collapse (mean) grossearnings_male grossearnings_female [aweight=dwt], by(deh_c3 stm)
reshape wide grossearnings*, i(stm) j(deh_c3)


foreach var in grossearnings_female grossearnings_male {
rename `var'1 `var'_dehc3_high
rename `var'2 `var'_dehc3_med
rename `var'3 `var'_dehc3_low
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_grossEarningsByGenderAndEdu", replace) firstrow(variables)


********************************************************************************
*Hourly wages by education and gender (for employed persons)
********************************************************************************
foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

la def dgn_lb /// 
		0 "Female" ///
		1 "Male"

la val dgn dgn_lb

recode lhw (-9 = .)

la def deh_c3_lb /// 
	1 "High" ///
	2 "Medium" ///
	3 "Low"

la val deh_c3 deh_c3_lb

drop if missing(deh_c3) | deh_c3 == -9
keep if les_c3 == 1


*Monetary values in the estimation sample are in 2015 values. Uprate by the CPI. 
gen CPI = .
replace CPI = 0.879 if stm == 2009
replace CPI = 0.901 if stm == 2010
replace CPI = 0.936 if stm == 2011
replace CPI = 0.96  if stm == 2012
replace CPI = 0.982 if stm == 2013
replace CPI = 0.996 if stm == 2014
replace CPI = 1     if stm == 2015
replace CPI = 1.01  if stm == 2016
replace CPI = 1.036 if stm == 2017
replace CPI = 1.06  if stm == 2018
replace CPI = 1.078 if stm == 2019
replace CPI = 1.089 if stm == 2020
replace CPI = 1.116 if stm == 2021
replace CPI = 1.205 if stm == 2022
replace CPI = 1.286 if stm == 2023


gen hourlywage = sinh(yplgrs_dv)*CPI/(lhw*4.33) 
drop if hourlywage <= 1 | hourlywage >= 1000
gen hourlywage_female = hourlywage if dgn == 0 
gen hourlywage_male = hourlywage if dgn == 1 

collapse (mean) hourlywage_male hourlywage_female [aweight=dwt], by(deh_c3 stm)
reshape wide hourlywage*, i(stm) j(deh_c3)


foreach var in hourlywage_female hourlywage_male {
rename `var'1 `var'_dehc3_high
rename `var'2 `var'_dehc3_med
rename `var'3 `var'_dehc3_low
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_hourlywageByGenderAndEdu", replace) firstrow(variables)

********************************************************************************
*Hours worked weekly by education and gender (for employed persons)
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

la def dgn_lb /// 
		0 "Female" ///
		1 "Male"

la val dgn dgn_lb

recode lhw (-9 = .)

la def deh_c3_lb /// 
	1 "High" ///
	2 "Medium" ///
	3 "Low"

la val deh_c3 deh_c3_lb

keep if les_c3 == 1

gen lhw_female = lhw if dgn == 0 
gen lhw_male = lhw if dgn == 1 

collapse (mean) lhw_male lhw_female [aweight=dwt], by(deh_c3 stm)
drop if missing(deh_c3) | deh_c3 == -9
reshape wide lhw*, i(stm) j(deh_c3)


foreach var in lhw_female lhw_male {
rename `var'1 `var'_dehc3_high
rename `var'2 `var'_dehc3_medium
rename `var'3 `var'_dehc3_low
}

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_lhwByGenderAndEdu", replace) firstrow(variables)


********************************************************************************
*Hours worked weekly by gender (for employed persons)
********************************************************************************

foreach year in 2011 2012 2013 2014 2015 2016 2017 2018 2019 2020 2021 {
import delimited "C:\Users\Patryk\git\SimPathsFork\input\InitialPopulations - Copy\population_initial_UK_`year'.csv", clear

cap drop _*

la def dgn_lb /// 
		0 "Female" ///
		1 "Male"

la val dgn dgn_lb

recode lhw (-9 = .)

la def deh_c3_lb /// 
	1 "High" ///
	2 "Medium" ///
	3 "Low"

la val deh_c3 deh_c3_lb

drop if missing(deh_c3) | deh_c3 == -9
keep if les_c3 == 1

gen lhw_female = lhw if dgn == 0 
gen lhw_male = lhw if dgn == 1 

collapse (mean) lhw_male lhw_female [aweight=dwt], by(stm)

rename stm Year

if `year' == 2011 {
save validation_statistics.dta, replace
} 
else {
append using validation_statistics
save validation_statistics.dta, replace
}

}

export excel using validation_statistics_UK.xlsx, sheet("UK_lhwByGender", replace) firstrow(variables)


********************************************************************************
*Poverty : share of households and share of children at risk of poverty : by region
********************************************************************************

/*
Adding validation for poverty would require net values of income to be added to the initial populations, to calculate the equivalised disposable yearly income required for the calculation of the households and children at risk of poverty.

Would have to check if available for the UK.
*/