libname health "D:\Home\cbooker\UKHLS\Data\SAS Files";
libname inapp "D:\Home\cbooker\Papers in Progress\INAPP\Data\SAS";
libname mac "D:\Home\cbooker\Misc SAS\Macros";

options fmtsearch = (health);
options mstored sasmstore=mac;
ods html close;

proc format library=health;
value JBF	1 = 'Employed'
			2 = 'Education'
			3 = 'Not Employed';

value edd	1 = 'Degree'
			2 = 'Other Higher/A-level/GCSE'
			3 = 'Other/No Qualification';

value nwq 	0 = 'Did not Re-enter Education'
			1 = 'Re-entered Education'
			2 = 'Same Level of Education after Re-entered Education'
			3 = 'Gained Higher level of Education after Re-entered Education'
			4 = 'Gained Other Level of Education after Re-entered Education';

value hht	1 = 'Households Pensioners and No Children'
			2 = 'Households with No Children'
			3 = 'Couples with Children'
			4 = 'Single Parents with Children'
			5 = 'Other types of Households with Children';
quit;

data UK_Long18;
	set health.UK_Long_19;

if pid = . then delete;
if wave = 9 then delete;

/*Missing Values*/
*Changing out of range coding values of some variables to missing;
array aa (*) _numeric_;
	do i=1 to dim(aa);
		if  aa(i) < 0 then aa(i) =.;
drop i;
end;

/*Using only UKHLS GPS Sample*/
if BHPSpid ne . then delete;

*Deleting all IEMB Sample Members as they are new at wave 6;
if hhorig = 8 then delete;

run;

/*Creating wave file in order to rectangularize dataset to 'impute' data where dates of spells are known*/
data wave;
	do wave = 1 to 8;
output;
end;
run;

proc sql;
create table lookup as
	select distinct pid, wave.wave
	from UK_Long18, wave
	order by pid, wave;
quit;

proc sort data=UK_Long18;
	by pid wave;
run;

data UK_Long18a;
	merge UK_Long18 lookup;
		by pid wave;
run;

/*Getting variables for Stata simulation*/
data UK_Sim (rename=(sex_dv=sex nkids_dv=nkids nch02_dv=nchldrn_02 hhtype_dv=hhtype hhresp_dv=hhresp gor_dv=GOR urban_dv=Urban hiqual_dv=hiqual 
	sf12pcs_dv=SF12_PCS sf12mcs_dv=SF12_MCS));
	set inapp.UK_Long18a;
	by pid;

*Interview Date;
Int_Date = mdy(istrtdatm,istrtdatd,istrtdaty);

*Age Squared;
AgeSQ = age_dv*age_dv;

*Birth Cohort;
if birthy in (1908,1909) then Brth_Chrt = 1;
if birthy in (1910,1911,1912,1913,1914,1915,1916,1917,1918,1919) then Brth_Chrt = 1;
if birthy in (1920,1921,1922,1923,1924,1925,1926,1927,1928,1929) then Brth_Chrt = 2;
if birthy in (1930,1931,1932,1933,1934,1935,1936,1937,1938,1939) then Brth_Chrt = 3;
if birthy in (1940,1941,1942,1943,1944,1945,1946,1947,1948,1949) then Brth_Chrt = 4;
if birthy in (1950,1951,1952,1953,1954,1955,1956,1957,1958,1959) then Brth_Chrt = 5;
if birthy in (1960,1961,1962,1963,1964,1965,1966,1967,1968,1969) then Brth_Chrt = 6;
if birthy in (1970,1971,1972,1973,1974,1975,1976,1977,1978,1979) then Brth_Chrt = 7;
if birthy in (1980,1981,1982,1983,1984,1985,1986,1987,1988,1989) then Brth_Chrt = 8;
if birthy in (1990,1991,1992,1993,1994,1995,1996,1997,1998,1999) then Brth_Chrt = 9;
if birthy in (2000,2001) then Brth_Chrt = 10;

/*Adult Variable Management*/
/*Recoding Ethnicity*/
if racel in (1,2) then Eth = 1;
if racel in (9,10,11) then Eth = 2;
if racel in (14,15) then Eth = 3;
if racel in (3,4,12,13,16,17,97) then Eth = 4;
if racel in (5,6,7,8) then Eth = 5;

/*Marital Status*/
*MARSTAT: 3 Category;
if mastat_dv in (2,3,10) then PARTNER = 1;
if mastat_dv = 1 then PARTNER = 2;
if mastat_dv in (4,5,6,7,8,9) then PARTNER = 3;

*Same partner from last wave;
if relup in (1,2) then same_p = 1;
if relup in (3,4,5) then same_p = 0;

*Length of cohabitation;
array chb [*] currpart1-currpart7;
array chbms [*] lmcbm1-lmcbm7;
array chbys [*] lmcby41-lmcby47;

do i=1 to dim(chb);
	if chb(i) = 1 then do;
		if chbms(i) = . and chbys(i) ne . then do;
			if mod(pid,2)=1 then chbms(i) = 3;
			else chbms(i) = 10;	
		end;
	D_CChb = mdy(chbms(i),15,chbys(i));
	end;
drop i;
end;

if LCMARM = . and LCMARY4 ne . then do;
	if mod(pid,2)=1 then LCMARM = 5;
	else LCMARM = 6;	
end;

if LCMCBM = . and LCMCBY4 ne . then do;
	if mod(pid,2)=1 then LCMCBM = 3;
	else LCMCBM = 10;	
end;

D_Cohab = mdy(lcmcbm,15,lcmcby4);
D_Mrrg = mdy(lcmarm,15,lcmary4);

Cohab_Dur = intck('year',D_Cohab,Int_Date,'C');
if Cohab_Dur = . and D_Mrrg ne . then do;
	Cohab_Dur = intck('year',D_Mrrg,Int_Date,'C');
end;

if Cohab_Dur = . and D_CChb ne . then do;
	Cohab_Dur = intck('year',D_CChb,Int_Date,'C');
end;

Prtnrshp_Dur = intck('year',D_Mrrg,Int_Date,'C'); 
if Prtnrshp_Dur ne . and Cohab_Dur ne . then do;
	if Prtnrshp_Dur < Cohab_Dur then Prtnrshp_Dur = Cohab_Dur;
end;

*Getting dates of last and current partnership;
array ccp [*] currpart1-currpart7;
array ycp [*] lmcby41 lmcby42 lmcby43 lmcby44 lmcby45 lmcby46 lmcby47;
array ycpe [*] lmspy41 lmspy42 lmspy43 lmspy44 lmspy45 lmspy46 lmspy47;
array cme [*] mstatch1-mstatch5;
array ycme [*] statcy41 statcy42 statcy43 statcy44 statcy45;     

do i=1 to dim(ccp);
	if ccp(i) = 1 then y_cprtnr_s = ycp(i);
	if ccp(i) = 2 then y_cprtnr_e = ycpe(i);
drop i;
end;

do i=1 to dim(cme);
	if cme(i) in (4,5,6,7,8,9) then y_prtnr_e = ycme(i);
drop i;
end;

/*Limited by Illness/Disability*/
*Recoding health so that 2 = 0;
if HEALTH = 2 then HEALTH = 0;

*Recoding Disabilities so if Health = 0 then they are also equal to 0;
array disab [12] disdif1 disdif2 disdif3 disdif4 disdif5 disdif6 disdif7 disdif8 disdif9 disdif10 disdif11 disdif12;

do i=1 to 12;
	if HEALTH = 0 then disdif96 = 1;
	if HEALTH = 0 or disdif96 = 1 then disab(i) = 0;
drop i;
end;

*Limiting Illness;
if HEALTH = 1 then do;
	if disdif96 = 1 then LIMIT = 1; 
	if disdif96 = 0 then LIMIT = 2;
end;
if HEALTH = 0 then LIMIT = 0;

*Limiting Long-standing Illness (No LTI vs. LLTI);
if HEALTH = 1 and disdif96 = 0 then LLTI = 1;
if HEALTH = 1 and disdif96 = 1 then LLTI = 0;
if HEALTH = 0 then LLTI = 0;

*Self-rated Health: Reverse scored;
if wave = 1 then SRH = 6-sf1;
if wave ne 1 then SRH = 6-scsf1;

/*Education*/
*In Education;
if jbstat = 7 then Edctn = 1;
if jbstat in (1,2,3,4,5,6,8,9,10,11,97) then Edctn = 0;

*Level of Education;
if hiqual_dv in (5,9) then Edu_lvl = 3;
if hiqual_dv in (2,3,4) then Edu_lvl = 2;
if hiqual_dv  = 1 then Edu_lvl = 1;

*Have had a break;
if first.pid then do;
	if jbstat = 7 and fenow in (1,2) then edu_brk = 1;
end;

*Change in University Fees;
Edu_fees_Chng = 0;
if Country = 1 and istrtdaty in (2012,2017) then Edu_fees_Chng = 1;

*University Fees by Country and Year;
if country in (1,2) then do;
	if istrtdaty in (2009,2010,2011) then Uni_Fees = 3225;
	if istrtdaty in (2012,2013,2014,2015,2016) then Uni_Fees = 9000;
	if istrtdaty in (2017,2018) then Uni_Fees = 9250;
end;

if country = 3 then do;
	if nkids_dv = 0 and partner = 2 then Uni_Fees = 0;	
	else Uni_Fees = 1820;
end;

if country = 4 then do;
	if istrtdaty in (2009,2010) then Uni_Fees = 300;
	if istrtdaty = 2011 then Uni_Fees = 3375;
	if istrtdaty = 2012 then Uni_Fees = 3465;
	if istrtdaty = 2013 then Uni_Fees = 3575;
	if istrtdaty = 2014 then Uni_Fees = 3685;
	if istrtdaty = 2015 then Uni_Fees = 3805;
	if istrtdaty = 2016 then Uni_Fees = 3925;
	if istrtdaty = 2017 then Uni_Fees = 4020;
	if istrtdaty = 2018 then Uni_Fees = 4200;
end;
	
*Parental Education;
array pe [2] paedqf maedqf;
array pec [2] FEDU MEDU;

do i=1 to 2;
	if pe(i) in (1,2,97) then pec(i) = 3;
	if pe(i) in (3,4) then pec(i) = 2;
	if pe(i) = 5 then pec(i) = 1;
drop i;
end;

/*Employment Status*/
*JBSTAT: 3 Category;
if JBSTAT in (1,2) then JBSTAT3 = 1;
if JBSTAT = 7 or Edctn = 1 then JBSTAT3 = 2;
if JBSTAT in (3,4,5,6,8,9,10,11,97) then JBSTAT3 = 3;

*JBSTAT: No students;
if JBSTAT in (1,2) then JBSTAT_NS = 1;
if JBSTAT in (3,4,5,6,8,9,10,11,97) then JBSTAT_NS = 2;

*JBSTAT: No Retired;
if JBSTAT in (1,2) then JBSTAT_NR = 1;
if JBSTAT in (3,5,6,8,9,10,11,97) then JBSTAT_NR = 2;

*Long-term Sick/Disabled;
if jbstat = 8 then dlltsd = 1;
if jbstat ne 8 then dlltsd = 0;

*Retired;
if jbstat = 4 then dlrtrd = 1;
if jbstat ne 4 then dlrtrd = 0;

*Income;
if istrtdaty=2009 then CPI = 0.866;
if istrtdaty=2010 then CPI = 0.894;
if istrtdaty=2011 then CPI = 0.934;
if istrtdaty=2012 then CPI = 0.961;
if istrtdaty=2013 then CPI = 0.985;
if istrtdaty=2014 then CPI = 1.000;
if istrtdaty=2015 then CPI = 1.000;
if istrtdaty=2016 then CPI = 1.007;
if istrtdaty=2017 then CPI = 1.034;
if istrtdaty=2018 then CPI = 1.059;

/*UK General Fertility Rate: From ONS 2019*/
if istrtdaty = 2009 then UK_GFR = 62.0;
if istrtdaty = 2010 then UK_GFR = 63.4;
if istrtdaty = 2011 then UK_GFR = 63.4;
if istrtdaty = 2012 then UK_GFR = 64.1;
if istrtdaty = 2013 then UK_GFR = 61.6;
if istrtdaty = 2014 then UK_GFR = 61.6;
if istrtdaty = 2015 then UK_GFR = 61.7;
if istrtdaty = 2016 then UK_GFR = 61.7;
if istrtdaty = 2017 then UK_GFR = 60.3;
if istrtdaty = 2018 then UK_GFR = 58.5;

/*Weights*/
*Household;
if wave in (1,2) then dhhwt = hhdenus_xw;
if wave ge 3 then dhhwt = hhdenub_xw;

*Individual;
**Cross-sectional;
if wave in (1,2) then dimxwt = indinus_xw;
if wave ge 3 then dimxwt = indinub_xw;

if wave in (1,2) then discxwt = indscus_xw;
if wave ge 3 then discxwt = indscub_xw;

**Longitudinal;
dimlwt = indinus_lw;
disclwt = indscus_lw;

label
	Brth_Chrt = 'Birth Cohort: 10 Year Bands'
	Eth = 'Ethnicity'
	PARTNER = 'Marital Status'
	LIMIT = 'Limiting Long-standing Illness: 3 Category'
	LLTI = 'Limiting Long-standing Illness: Binary'
	Edctn = 'In Education: Binary'
	Edu_lvl = 'Educational Level: 3 Category'
	FEDU = 'Paternal Educational Level: 3 Category'
	MEDU = 'Maternal Educational Level: 3 Category'
	JBSTAT3 = 'Employment Status: 3 Category'
	JBSTAT_NS = 'Employment Status: No Students'
	JBSTAT_NR = 'Employment Status: No Retirees'
	edu_brk = 'Took Break from Education'
	Edu_fees_Chng = 'Whether University Feeds Changed'
	SRH = 'Self-rated Health'
	Uni_Fees = 'University Tuition Fees'
	Prtnrshp_Dur = "Duration of Partnership"
	UK_GFR = 'UK Generalised Fertility Rate'
	dlltsd = 'Long-term Sick or Disabled'
	dlrtrd = 'Retired/Pensioner'
	dwt = 'Household Weight: Cross-sectional'
	dimxwt = 'Individual Main Weight: Cross-sectional'
	discxwt = 'Individual Self-completion Weight: Cross-sectional'
	dimlwt = 'Individual Main Weight: Longitudinal'
	disclwt = 'Individual Self-completion Weight: Longitudinal';

format sex_dv a_sex. LLTI LLSI. hiqual_dv A_HIQUA. eth ethn. urban_dv urban_d. PARTNER pstat. LIMIT LSI. gor_dv gor_dv. hhtype_dv hhtype. 
	JBSTAT3 JBF. edu_lvl fedu MEDU edd. Edctn Edu_fees_Chng dlltsd dlrtrd yn.;

keep pid hid wave sex_dv age_dv AGESQ llti Limit Eth urban_dv partner hhsize hhtype_dv hhresp_dv country gor_dv	strata psu hiqual_dv pno jbstat ppid Brth_Chrt
	dhhwt dimxwt discxwt dimlwt disclwt istrtdaty Edctn edu_lvl fedu MEDU mnspid fnspid contft ftendy4 ftedany cohab cohabn ncrr5 lmcby41 lmcby42 lmcby43 
	lmcby44 lmcby45 lmcby46 lmcby47 lmspy41 lmspy42 lmspy43 lmspy44 lmspy45 lmspy46 lmspy47 same_p y_cprtnr_s y_cprtnr_e mstatch1-mstatch5 statcy41 statcy42 
	statcy43 statcy44 statcy45 y_prtnr_e birthy birthm contft school feend scend ftedany fenow edu_brk mnpid fnpid Edu_fees_Chng SRH sf12pcs_dv sf12mcs_dv 
	Uni_Fees JBSTAT3 Prtnrshp_Dur Int_Date UK_GFR nkids_dv nch02_dv mastat_dv fimnlabgrs_dv fimnpen_dv fimnmisc_dv fihhmnlabgrs_dv fihhmnpen_dv fihhmnmisc_dv 
	ieqmoecd_dv CPI depchl_dv dlltsd dlrtrd istrtdatm istrtdaty JBSTAT_NS JBSTAT_NR lvag14 mvyr; 
run;

/*Getting current cohabitation date from cohab dataset*/
data cohab;
	set health.a_cohab;

if a_lcsbm in (-1,-2) and a_lcsby4 ne . then do;
	if mod(pidp,2)=1 then a_lcsbm = 3;
	else a_lcsbm = 10;	
end;

if a_lcsbm < 0 then a_lcsbm = .;
if a_lcsby4 < 0 then a_lcsby4 = .;

if  a_lcsey4  = -8 then do;
	D_Cohab = mdy(a_lcsbm,15,a_lcsby4);
end;

wave = 1;

if D_Cohab = . then delete;
keep pidp D_Cohab wave;
run;

proc sql;
create table UK_Sim1 as
	select a.*, b.D_Cohab
	from UK_Sim a left join cohab b
	on a.pid=b.pidp and a.wave=b.wave
	order by pid, wave;
quit;

/*Getting Number of Children and Newborn Variables*/
data b_newborn (rename=(pidp=b_pid pid=b_BHPSpid b_hidp=b_hid));
	set health.b_newborn; 
format _all_;
run;
data c_newborn (rename=(pidp=c_pid pid=c_BHPSpid c_hidp=c_hid));
	set health.c_newborn;
format _all_;
run;
data d_newborn (rename=(pidp=d_pid pid=d_BHPSpid d_hidp=d_hid));
	set health.d_newborn;
format _all_;
run;
data e_newborn (rename=(pidp=e_pid pid=e_BHPSpid e_hidp=e_hid));
	set health.e_newborn;
format _all_;
run;
data f_newborn (rename=(pidp=f_pid pid=f_BHPSpid f_hidp=f_hid));
	set health.f_newborn;
format _all_;
run;
data g_newborn (rename=(pidp=g_pid pid=g_BHPSpid g_hidp=g_hid));
	set health.g_newborn;
format _all_;
run;
data h_newborn (rename=(pidp=h_pid pid=h_BHPSpid h_hidp=h_hid));
	set health.h_newborn;
format _all_;
run;

data UKNbrn;
	set	b_newborn (rename=(%UKHLSStrpP(b_newborn)) in=in1)
		c_newborn (rename=(%UKHLSStrpP(c_newborn)) in=in2)
		d_newborn (rename=(%UKHLSStrpP(d_newborn)) in=in3)
		e_newborn (rename=(%UKHLSStrpP(e_newborn)) in=in4)
		f_newborn (rename=(%UKHLSStrpP(f_newborn)) in=in5)
		g_newborn (rename=(%UKHLSStrpP(g_newborn)) in=in6)
		h_newborn (rename=(%UKHLSStrpP(h_newborn)) in=in7);
	format _all_;
if in1 then Wave=2;
else if in2 then Wave=3;
else if in3 then Wave=4;
else if in4 then Wave=5;
else if in5 then Wave=6;
else if in6 then Wave=7;
else if in7 then Wave=8;
run;

data brths;
	set UKNbrn;

where lchlv = 1 and BHPSpid = -8;

array aa (*) _numeric_;
	do i=1 to dim(aa);
		if  aa(i) < 0 then aa(i) =.;
drop i;
end;

nbrn = 1;

keep hid pid nbrn;
run;

proc sql;
create table brths2 as
select *, count(nbrn) as nch_0
from brths
group by hid, pid;
quit;

proc sort data=brths2 nodupkey;
by hid;
run;

proc sql;
*Keeping nbrn variable so can identify women who have had a baby;
create table UK_Sim2 as
	select a.*, b.nbrn, b.nch_0
	from UK_Sim1 a left join brths2 b
	on a.hid=b.hid 
	order by pid, wave;
quit;

*Counting number of children in the household;
data Chldrn;
	set UKlong_iall;

where 1 le age_dv le 18;

/*Missing Values*/
*Changing out of range coding values of some variables to missing;
array aa (*) _numeric_;
	do i=1 to dim(aa);
		if  aa(i) < 0 then aa(i) =.;
drop i;
end;

if 1 le age_dv le 2 then do;
	if pns1pid ne . then tdlr = 1;
	if tdlr = . and pns2pid ne . then tdlr = 1;
end;

if 1 le age_dv le 18 and depchl_dv = 1 then do;
	if pns1pid ne . then chld = 1;
	if chld = . and pns2pid ne . then chld = 1;
end;

if 1 le age_dv le 13 then do;
	if pns1pid ne . then chld_113 = 1;
	if chld_113 = . and pns2pid ne . then chld_113 = 1;
end;

if 14 le age_dv le 18 and depchl_dv = 1 then do;
	if pns1pid ne . then chld_dep = 1;
	if chld_dep = . and pns2pid ne . then chld_dep = 1;
end;
run;

proc sql;
create table nchldrn as
	select hid, wave, pns1pid, pns2pid, count(tdlr) as nch_12, count(chld) as Nchld, count(chld_113) as nchld_113, count(chld_dep) as nchld_dep, 
		age_dv as CHAge
from Chldrn
group by hid
order by hid;
quit;

data nchldrn1;
	set nchldrn;
where pns1pid ne .;
run;

data nchldrn2 (rename=(pns2pid=pid));
	set nchldrn;
where pns2pid ne .;
run;

proc sort data=nchldrn1 nodupkey;
by hid;
proc sort data=nchldrn2 nodupkey;
by pid wave;
run;

proc sql;
create table UK_Sim3 as
	select a.*, b.nch_12, b.Nchld, b.nchld_113, b.nchld_dep, b.CHAge
	from UK_Sim2 a left join nchldrn1 b
	on a.pid=b.pns1pid and a.wave=b.wave
	order by pid, wave;
quit;

data UK_Sim4;
	merge UK_Sim3 (in=a) nchldrn2;
	by pid wave;
	if a;

if nch_0 ne . then do;
	nch_02 = nch_0 + nch_12;
	nchld_013 = nch_0 + nchld_113;
	N_Chld = nch_0 + nchld;
end;
if nch_0 = . then do;
	if nch_12 ne . then nch_02 = nch_12;
	if nchld_113 ne . then nchld_013 = nchld_113;
	if nchld ne . then N_Chld = nchld;
end;

if nch_0 ne . then CH_Age = 0;
if CH_Age = . and CHAge ne . then CH_Age = min(of CHAge);

drop nchld nchld_113 nch_12 CHAge;
run;

proc sql;
create table UK_Sim5 as
	select *, min(CH_Age) as dchygag, max(ch_age) as dchoag
from UK_Sim4
group by hid
order by pid, wave;
quit;

proc expand data=UK_Sim5 out=UK_Sim6 method=none;
	by pid;

convert jbstat = jbld / transformout=(lead 1);
convert jbstat = jbld2 / transformout=(lead 2);
convert age_dv = agld / transformout=(lead 1);
convert age_dv = agld2 / transformout=(lead 2);
convert partner = prtld / transformout=(lead 1);
convert partner = prtld2 / transformout=(lead 2);
convert same_p = smpld / transformout=(lead 1);
convert same_p = smpld2 / transformout=(lead 2);
convert y_cprtnr_s = ycpsld / transformout=(lead 1);
convert y_cprtnr_s = ycpsld2/ transformout=(lead 2);
convert y_cprtnr_e = ycpeld / transformout=(lead 1);
convert y_cprtnr_e = ycpeld2 / transformout=(lead 2);
convert y_prtnr_e = ypeld / transformout=(lead 1);
convert y_prtnr_e = ypeld2 / transformout=(lead 2);
convert istrtdaty = idtld / transformout=(lead 1);
convert istrtdaty = idtld2 / transformout=(lead 2);
convert istrtdatm = idtmld / transformout=(lead 1);
convert istrtdatm = idtmld2 / transformout=(lead 2);
convert mnspid = mpidld / transformout=(lead 1);
convert mnspid = mpidld2 / transformout=(lead 2);
convert fnspid = fpidld / transformout=(lead 1);
convert fnspid = fpidld2 / transformout=(lead 2);
convert ppid = ppidld / transformout=(lead 1);
convert ppid = ppidld2 / transformout=(lead 2);
convert hhsize = hhsld / transformout=(lead 1);
convert hhsize = hhsld2 / transformout=(lead 2);
convert N_chld = nkld / transformout=(lead 1);
convert N_chld = nkld2 / transformout=(lead 2);
convert nch_02 = c02ld / transformout=(lead 1);
convert nch_02 = c02ld2 / transformout=(lead 2);
convert hhtype = hhtld / transformout=(lead 1);
convert hhtype = hhtld2 / transformout=(lead 2);
convert hiqual = hqlld / transformout=(lead 1);
convert hiqual = hqlld2 / transformout=(lead 2);
convert gor = gorld / transformout=(lead 1);
convert gor = gorld2 / transformout=(lead 2);
convert urban = rbnld / transformout=(lead 1);
convert urban = rbnld2 / transformout=(lead 2);
convert srh = srhld / transformout=(lead 1);
convert srh = srhld2 / transformout=(lead 2);
convert SF12_PCS = sfpcsld / transformout=(lead 1);
convert SF12_PCS = sfpcsld2 / transformout=(lead 2);
convert SF12_MCS = sfmcsld / transformout=(lead 1);
convert SF12_MCS = sfmcsld2 / transformout=(lead 2);
convert llti = ltild / transformout=(lead 1);
convert llti = ltild2 / transformout=(lead 2);
convert Limit = lmtld / transformout=(lead 1);
convert Limit = lmtld2 / transformout=(lead 2);
convert contft = cfteld / transformout=(lead 1);
convert contft = cfteld2 / transformout=(lead 2);
convert ftendy4 = fteeld / transformout=(lead 1);
convert ftendy4 = fteeld2 / transformout=(lead 2);
convert birthm = bmld / transformout=(lead 1);
convert birthm = bmld2 / transformout=(lead 2);
convert birthy = byld / transformout=(lead 1);
convert birthy = byld2 / transformout=(lead 2);
convert	dchygag = cyagld / transformout=(lead 1);

convert jbstat = jblg / transformout=(lag 1);
convert jbstat = jblg2 / transformout=(lag 2);
convert age_dv = aglg / transformout=(lag 1);
convert age_dv = aglg2 / transformout=(lag 2);
convert partner = prtlg / transformout=(lag 1);
convert partner = prtlg2 / transformout=(lag 2);
convert same_p = smplg / transformout=(lag 1);
convert same_p = smplg2 / transformout=(lag 2);
convert y_cprtnr_s = ycpslg / transformout=(lag 1);
convert y_cprtnr_s = ycpslg2/ transformout=(lag 2);
convert y_cprtnr_e = ycpelg / transformout=(lag 1);
convert y_cprtnr_e = ycpelg2 / transformout=(lag 2);
convert y_prtnr_e = ypelg / transformout=(lag 1);
convert y_prtnr_e = ypelg2 / transformout=(lag 2);
convert istrtdaty = idtlg / transformout=(lag 1);
convert istrtdaty = idtlg2 / transformout=(lag 2);
convert istrtdatm = idtmlg / transformout=(lag 1);
convert istrtdatm = idtmlg2 / transformout=(lag 2);
convert mnspid = mpidlg / transformout=(lag 1);
convert mnspid = mpidlg2 / transformout=(lag 2);
convert fnspid = fpidlg / transformout=(lag 1);
convert fnspid = fpidlg2 / transformout=(lag 2);
convert ppid = ppidlg / transformout=(lag 1);
convert ppid = ppidlg2 / transformout=(lag 2);
convert hhsize = hhslg / transformout=(lag 1);
convert hhsize = hhslg2 / transformout=(lag 2);
convert N_chld = nklg / transformout=(lag 1);
convert N_chld = nklg2 / transformout=(lag 2);
convert nch_02 = c02lg / transformout=(lag 1);
convert nch_02 = c02lg2 / transformout=(lag 2);
convert hhtype = hhtlg / transformout=(lag 1);
convert hhtype = hhtlg2 / transformout=(lag 2);
convert hiqual = hqllg / transformout=(lag 1);
convert hiqual = hqllg2 / transformout=(lag 2);
convert gor = gorlg / transformout=(lag 1);
convert gor = gorlg2 / transformout=(lag 2);
convert urban = rbnlg / transformout=(lag 1);
convert urban = rbnlg2 / transformout=(lag 2);
convert srh = srhlg / transformout=(lag 1);
convert srh = srhlg2 / transformout=(lag 2);
convert SF12_PCS = sfpcslg / transformout=(lag 1);
convert SF12_PCS = sfpcslg2 / transformout=(lag 2);
convert SF12_MCS = sfmcslg / transformout=(lag 1);
convert SF12_MCS = sfmcslg2 / transformout=(lag 2);
convert llti = ltilg / transformout=(lag 1);
convert llti = ltilg2 / transformout=(lag 2);
convert Limit = lmtlg / transformout=(lag 1);
convert Limit = lmtlg2 / transformout=(lag 2);
convert contft = cftelg / transformout=(lag 1);
convert contft = cftelg2 / transformout=(lag 2);
convert ftendy4 = fteelg / transformout=(lag 1);
convert ftendy4 = fteelg2 / transformout=(lag 2);
convert birthm = bmlg / transformout=(lag 1);
convert birthm = bmlg2 / transformout=(lag 2);
convert birthy = bylg / transformout=(lag 1);
convert birthy = bylg2 / transformout=(lag 2);
convert	dchoag = coaglg / transformout=(lag 1);
convert	dchoag = coaglg2 / transformout=(lag 2);

run;

data UK_Sim7;
	set UK_Sim6;
	by pid;

/*Filling in Missing Values*/
array lddv [2] agld idtld;
array lddv2 [2] agld2 idtld2;
array lgdv [2] aglg idtlg; 
array lgdv2 [2] aglg2 idtlg2;
array ddv [2] age_dv istrtdaty;

array ldv [*] mpidld fpidld hhtld hqlld gorld rbnld srhld ltild lmtld idtmld bmld byld;
array ldv2 [*] mpidld2 fpidld2 hhtld2 hqlld2 gorld2 rbnld2 srhld2 ltild2 lmtld2 idtmld2 bmld2 byld2;
array lgv [*] mpidlg fpidlg hhtlg hqllg gorlg rbnlg srhlg ltilg lmtlg idtmlg bmlg bylg;
array lgv2 [*] mpidlg2 fpidlg2 hhtlg2 hqllg2 gorlg2 rbnlg2 srhlg2 ltilg2 lmtlg2 idtmlg2 bmlg2 bylg2;
array dv [*] mnspid fnspid hhtype hiqual gor urban SRH llti Limit istrtdatm birthm birthy;

array ldhhv [2] hhsld c02ld;
array ldhhv2 [2] hhsld2 c02ld2;
array lghhv [2] hhslg c02lg; 
array lghhv2 [2] hhslg2 c02lg2;
array dhhv [2] hhsize nchldrn_02;

do i=1 to 2;
*Time variables;
	if lddv(i) ne . and first.pid then do;
		if ddv(i) = . then ddv(i) = lddv(i)-1;
	end;
	if lgdv(i) ne . and last.pid then do;
		if ddv(i) = . then ddv(i) = lgdv(i)+1;
	end;
	if lddv(i) ne . and lgdv(i) ne . then do;
		if lddv(i) ne lgdv(i) and ddv(i) = . then ddv(i) = lgdv(i)+1;
	end;
	if lddv2(i) ne . and lgdv(i) ne . then do;
		if lddv2(i) ne lgdv(i) and ddv(i) = . then ddv(i) = lgdv(i)+1;
	end;
	if lddv(i) ne . and lgdv2(i) ne . then do;
		if lddv(i) ne lgdv2(i) and ddv(i) = . then ddv(i) = lddv(i)-1;
	end;
	drop i;
	
*Household variables;
	if ldhhv(i) ne . and first.pid then do;
		if dhhv(i) = . then dhhv(i) = ldhhv(i);
	end;
	if lghhv(i) ne . and last.pid then do;
		if dhhv(i) = . then dhhv(i) = lghhv(i);
	end;
	if ldhhv(i) ne . and lghhv(i) ne . then do;
		if ldhhv(i) = lghhv(i) and dhhv(i) = . then dhhv(i) = ldhhv(i);
	end;
	if ldhhv2(i) ne . and lghhv(i) ne . then do;
		if ldhhv2(i) = lghhv(i) and dhhv(i) = . then dhhv(i) = lghhv(i);
	end;
	if ldhhv(i) ne . and lghhv2(i) ne . then do;
		if ldhhv(i) = lghhv2(i) and dhhv(i) = . then dhhv(i) = lghhv2(i);
	end;
	drop i;
end;

*Stable and fixed variables;
do i=1 to dim(ldv);
	if ldv(i)  ne . and first.pid then do;
		if dv(i) = . then dv(i) = ldv(i);
	end;
	if lgv(i)  ne . and last.pid then do;
		if dv(i) = . then dv(i) = lgv(i);
	end;
	if ldv(i)  ne . and lgv(i) ne . then do;
		if ldv(i) = ldv(i) and dv(i) = . then dv(i) = ldv(i);
	end;
	if ldv(i)  ne . and lgv(i) ne . then do;
		if ldv(i) ne ldv(i) and dv(i) = . then dv(i) = lgv(i);
	end;
	if ldv2(i) ne . and lgv(i) ne . then do;
		if ldv2(i) = lgv(i) and dv(i) = . then dv(i)=lgv(i);
	end;
	if ldv(i) ne . and lgv2(i) ne . then do;
		if ldv(i) = lgv2(i) and dv(i) = . then dv(i)=lgv2(i);
	end;
	drop i;
end;

*State variables;
**Household Variables;
if nkld ne . and first.pid then do;
	if nkids = . then do;
		if nkld ge 1 and cyagld ge 1 then nkids = nkld;
	end;
end;
if nkld ne . and nklg ne . then do;
	if nkld = nklg and nkids = . then nkids = nkld;
	if nkld ne nklg and nkids = . then do;
		if coaglg = 17 then nkids = nklg-1;
		if coaglg le 16 then nkids = nkld;
	end;
end;
if nkld2 ne . and nklg ne . then do;
	if nkld2 = nklg and nkids = . then nkids = nkld2;
	if nkld2 ne nklg and nkids = . then do;
		if coaglg = 17 then nkids = nklg-1;
		if coaglg le 16 then nkids = nkld;
	end;
end;
if nkld ne . and nklg2 ne . then do;
	if nkld = nklg2 and nkids = . then nkids = nklg2;
	if nkld ne nklg2 and nkids = . then do;
		if coaglg2 = 16 then nkids = nklg2-1;
		if coaglg2 le 15 then nkids = nkld;
	end;
end;

**Labour Force Status;
if jblg = 7 and jbld = 7 and jbstat = . then jbstat = 7;
if jbld = 7 and first.pid then do;
	if jbstat = . and age_dv in (16,17,18) then jbstat = 7;
end;
if jblg = 7 and jbstat = . then do;
	if jbld2 = 7 then jbstat = 7;
	if jbld2 not in (.,7) then do;
		if cfteld2 = 2 and fteeld2 = istrtdaty then jbstat = 7;
		if cfteld2 = 2 and fteeld2 ne istrtdaty then jbstat = jbld2;
		if cfteld2 = . and fteeld2 = . then jbstat = jbld2;
	end;
end;
if jblg2 = 7 and jbstat = . then do;
	if jbld = 7 then jbstat = 7;
	if jbld ne 7 then do;
		if cfteld = 2 and fteeld = istrtdaty then jbstat = 7;
		if cfteld = 2 and fteeld ne istrtdaty then jbstat = jbld;
		if cfteld = . and fteeld = . then jbstat = jbld;
	end;
end;
if jblg not in (.,7) and jbstat = . then do;
	if jbld2 = 7 then do;
		if cfteld2 = 2 and fteeld2 = istrtdaty then jbstat = 7;
		if cfteld2 = 2 and fteeld2 ne istrtdaty then jbstat = jbld2;
		if cfteld2 = . and fteeld2 = . then jbstat = jbld2;
	end;
end;
if jblg2 not in (.,7) and jbstat = . then do;
	if jbld = 7 then do;
		if cfteld = 2 and fteeld = istrtdaty then jbstat = 7;
		if cfteld = 2 and fteeld ne istrtdaty then jbstat = jbld;
		if cfteld = . and fteeld = . then jbstat = jbld;
	end;
end;

if jbld ne . and first.pid then do;
	if jbstat = . then jbstat = jbld;
end;
if jblg ne . and last.pid then do;
	if jbstat = . then jbstat = jblg;
end;
if jbld not in (.,7) and jblg not in (.,7) then do;
	if jbld = jblg and jbstat = . then jbstat = jbld;
	if jbld ne jblg and jbstat = . then jbstat = jblg;
end;
if jbld not in (.,7) and jblg2 not in (.,7) then do;
	if jbld = jblg2 and jbstat = . then jbstat = jbld;
	if jbld ne jblg2 and jbstat = . then jbstat = jblg2;
end;
if jbld2 not in (.,7) and jblg not in (.,7) then do;
	if jbld2 = jblg and jbstat = . then jbstat = jbld2;
	if jbld2 ne jblg and jbstat = . then jbstat = jblg;
end;

**Partnership Status;
if prtld ne . and prtlg ne . then do;
	if prtld = prtlg then do;
		if partner = . then partner = prtlg;
		if partner = 1 and ppid = . and ppidld ne . and ppidlg ne . then do;
			if ppidld = ppidlg then ppid = ppidlg;
			if ppidld ne ppidlg then ppid = ppidlg;
		end;
	end;
	if prtld ne prtlg and partner = . then do;
		if smpld = 0 then do;
			if ycpeld = istrtdaty then partner = 0;
			if ycpeld = istrtdaty then partner = prtld;
			if ypeld = istrtdaty then partner = 0;
			if ypeld = istrtdaty then partner = prtld;
		end;
	end;
end;
if prtld2 ne . and prtlg ne . then do;
	if prtld2 = prtlg then do;
		if partner = . then partner = prtlg;
		if partner = 1 and ppid = . and ppidld2 ne . and ppidlg ne . then do;
			if ppidld2 = ppidlg then ppid = ppidlg;
			if ppidld2 ne ppidlg then ppid = ppidlg;
		end;
	end;
	if prtld2 ne prtlg and partner = . then do;
		if smpld = 0 then do;
			if ycpeld2 = istrtdaty then partner = 0;
			if ycpeld2 = istrtdaty then partner = prtld2;
			if ypeld2 = istrtdaty then partner = 0;
			if ypeld2 = istrtdaty then partner = prtld2;
		end;
	end;
end;
if prtld ne . and prtlg2 ne . then do;
	if prtld = prtlg2 then do;
		if partner = 1 and partner = . then do;
			partner = prtlg2;
			if ppid = . and ppidld ne . and ppidlg2 ne . then do;
				if ppidld = ppidlg2 then ppid = ppidlg2;
				if ppidld ne ppidlg2 then ppid = ppidlg2;
			end;
		end;
	end;
	if prtld ne prtlg2 and partner = . then do;
		if smpld = 0 then do;
			if ycpeld = istrtdaty then partner = 0;
			if ycpeld = istrtdaty then partner = prtld;
			if ypeld = istrtdaty then partner = 0;
			if ypeld = istrtdaty then partner = prtld;
		end;
	end;
end;

*SF-12: if before and after values are within .5 standard deviation (5 points) then fill in with mean, otherwise missing;
*PCS;
if sfpcsld ne . and sfpcslg ne . then do;
	PCS_Chnge = sfpcsld - sfpcslg;
	if abs(PCS_Chnge) le 5 and SF12_PCS = . then SF12_PCS = (sfpcsld + sfpcslg)/2;
end;

if sfpcsld2 ne . and sfpcslg ne . then do;
	PCS_Chnge1 = sfpcsld2 - sfpcslg;
	if abs(PCS_Chnge1) le 5 and SF12_PCS = . then SF12_PCS = (sfpcsld2 + sfpcslg)/2;
end;

if sfpcsld ne . and sfpcslg2 ne . then do;
	PCS_Chnge2 = sfpcsld - sfpcslg2;
	if abs(PCS_Chnge2) le 5 and SF12_PCS = . then SF12_PCS = (sfpcsld + sfpcslg2)/2;
end;

*MCS;
if sfmcsld ne . and sfmcslg ne . then do;
	MCS_Chnge = sfmcsld - sfmcslg;
	if abs(MCS_Chnge) le 5 and SF12_MCS = . then SF12_MCS = (sfmcsld + sfmcslg)/2;
end;

if sfmcsld2 ne . and sfmcslg ne . then do;
	MCS_Chnge1 = sfmcsld2 - sfmcslg;
	if abs(MCS_Chnge1) le 5 and SF12_MCS = . then SF12_MCS = (sfmcsld2 + sfmcslg)/2;
end;

if sfmcsld ne . and sfmcslg2 ne . then do;
	MCS_Chnge2 = sfmcsld - sfmcslg2;
	if abs(MCS_Chnge2) le 5 and SF12_MCS = . then SF12_MCS = (sfmcsld + sfmcslg2)/2;
end;

*Filling derived variables;
*Age Squared;
if AgeSQ = . and age_dv ne . then AgeSQ = age_dv*age_dv;

*Fertile women;
if sex = 2 and 18 le age_dv le 44 then frtl_wmn = 1;

*Education;
if Edctn = . and jbstat ne . then do;
	if jbstat = 7 then Edctn = 1;
	if jbstat in (1,2,3,4,5,6,8,9,10,11,97) then Edctn = 0;
end;

*Level of Education;
if Edu_lvl = . and hiqual ne . then do;
	if hiqual in (5,9) then Edu_lvl = 3;
	if hiqual in (2,3,4) then Edu_lvl = 2;
	if hiqual = 1 then Edu_lvl = 1;
end;

*Employment;
if jbstat3 = . and jbstat ne . then do;
	if JBSTAT in (1,2) then JBSTAT3 = 1;
	if JBSTAT = 7 or Edctn = 1 then JBSTAT3 = 2;
	if JBSTAT in (3,4,5,6,8,9,10,11,97) then JBSTAT3 = 3;
end;

*JBSTAT: No students;
if jbstat_ns = . and jbstat ne . then do;
	if JBSTAT in (1,2) then JBSTAT_NS = 1;
	if JBSTAT in (3,4,5,6,8,9,10,11,97) then JBSTAT_NS = 2;
end;

*JBSTAT: No Retired;
if jbstat_NR = . and jbstat ne . then do;
	if JBSTAT in (1,2) then JBSTAT_NR = 1;
	if JBSTAT in (3,5,6,8,9,10,11,97) then JBSTAT_NR = 2;
end;

if dlltsd = . and jbstat ne . then do;	
	if jbstat = 8 then dlltsd = 1;
	if jbstat ne 8 then dlltsd = 0;
end;

*Retired;
if jbstat ne . then do;
	if jbstat = 4 then dlrtrd = 1;
	if jbstat ne 4 then dlrtrd = 0;
end;

*Partnership duration;
Cohab_Dur = intck('year',D_Cohab,Int_Date,'C');
if Cohab_Dur ne . and Prtnrshp_Dur = . then Prtnrshp_Dur = Cohab_Dur;

/*Newborn Child*/
if nbrn = 1 then dchpd = 1;
if nbrn = . then dchpd = 0;

/*Living with parent*/
if mnspid ne . or fnspid ne . then dlvprnt = 1;
if mnspid = . and fnspid = . then dlvprnt = 0;

if lvag14 in (7,97) and dlvprnt = 1 then dlvprnt = 0;

/*Deleting those with no HID and age*/
if hid = . and age_dv = . then delete;

format Int_Date mmddyy10.;

drop agld idtld agld2 idtld2 aglg idtlg aglg2 idtlg2 mpidld fpidld hhtld hqlld gorld rbnld srhld ltild lmtld idtmld bmld byld mpidld2 fpidld2 hhtld2 hqlld2 
	gorld2 rbnld2 srhld2 ltild2 lmtld2 idtmld2 bmld2 byld2 mpidlg fpidlg hhtlg hqllg gorlg rbnlg srhlg ltilg lmtlg idtmlg bmlg bylg mpidlg2 fpidlg2 
	hhtlg2 hqllg2 gorlg2 rbnlg2 srhlg2 ltilg2 lmtlg2 idtmlg2 bmlg2 bylg2 hhsld c02ld hhsld2 c02ld2 hhslg c02lg hhslg2 c02lg2 nkld nkld2 nklg nklg2 
	jblg jblg2 jbld jbld2 prtld prtld2 prtlg prtlg2 sfpcsld sfpcsld2 sfpcslg sfpcslg2 sfmcsld sfmcsld2 sfmcslg sfmcslg2 cfteld2 cfteld cftelg cftelg2 
	fteeld2 fteeld fteelg fteelg2 ycpeld ycpeld2 ycpelg ycpelg2 ypeld ypeld2 ypelg ypelg2 ppidld2 ppidld ppidlg ppidlg2 smpld smpld2 smplg smplg2 
	ycpsld ycpsld2 ycpslg ycpslg2 contft ftendy4 ftedany cohab cohabn lmcby41 lmcby42 lmcby43 lmcby44 lmcby45 lmcby46 lmcby47 lmspy41 lmspy42 lmspy43 
	lmspy44 lmspy45 lmspy46 lmspy47 same_p y_cprtnr_s y_cprtnr_e mstatch1-mstatch5 statcy41 statcy42 statcy43 statcy44 statcy45 y_prtnr_e PCS_Chnge 
	PCS_Chnge1 PCS_Chnge2 MCS_Chnge MCS_Chnge1 MCS_Chnge2 Cohab_Dur D_Cohab time pns1pid lvag14 cyagld coaglg coaglg2;
run;

/*Getting Additional Income Data*/
data a_income (rename=(pidp=a_pid));
	set health.a_income; 
format _all_;

where a_ficode in (1,25,26);

keep pidp a_ficode a_frmnthimp_dv;
run;
data b_income (rename=(pidp=b_pid));
	set health.b_income; 
format _all_;

where b_ficode in (1,25,26);

keep pidp b_ficode b_frmnthimp_dv;
run;
data c_income (rename=(pidp=c_pid));
	set health.c_income;
format _all_;

where c_ficode in (1,25,26);

keep pidp c_ficode c_frmnthimp_dv;
run;
data d_income (rename=(pidp=d_pid));
	set health.d_income;
format _all_;

where d_ficode in (1,25,26);

keep pidp d_ficode d_frmnthimp_dv;
run;
data e_income (rename=(pidp=e_pid));
	set health.e_income;
format _all_;

where e_ficode in (1,25,26);

keep pidp e_ficode e_frmnthimp_dv;
run;
data f_income (rename=(pidp=f_pid));
	set health.f_income;
format _all_;

where f_ficode in (1,25,26);

keep pidp f_ficode f_frmnthimp_dv;
run;
data g_income (rename=(pidp=g_pid));
	set health.g_income;
format _all_;

where g_ficode in (1,25,26);

keep pidp g_ficode g_frmnthimp_dv;
run;
data h_income (rename=(pidp=h_pid));
	set health.h_income;
format _all_;

where h_ficode in (1,25,26);

keep pidp h_ficode h_frmnthimp_dv;
run;

data UKLong_inc;
	set	a_income (rename=(%UKHLSStrpP(a_income)) in=in1)
		b_income (rename=(%UKHLSStrpP(b_income)) in=in2)
		c_income (rename=(%UKHLSStrpP(c_income)) in=in3)
		d_income (rename=(%UKHLSStrpP(d_income)) in=in4)
		e_income (rename=(%UKHLSStrpP(e_income)) in=in5)
		f_income (rename=(%UKHLSStrpP(f_income)) in=in6)
		g_income (rename=(%UKHLSStrpP(g_income)) in=in7)
		h_income (rename=(%UKHLSStrpP(h_income)) in=in8);
	format _all_;
 if in1 then Wave=1;
 else if in2 then Wave=2;
 else if in3 then Wave=3;
 else if in4 then Wave=4;
 else if in5 then Wave=5;
 else if in6 then Wave=6;
 else if in7 then Wave=7;
 else if in8 then Wave=8;
 run;

data uklong_inc1 (rename=(frmnthimp_dv=inc_stp));
 	set uklong_inc;

where ficode = 1;

if frmnthimp_dv = 0 then delete;
run;

 data uklong_inc1a (rename=(frmnthimp_dv=inc_tu));
 	set uklong_inc;

where ficode = 25;

if frmnthimp_dv = 0 then delete;
run;

 data uklong_inc1b (rename=(frmnthimp_dv=inc_ma));
 	set uklong_inc;

where ficode = 26;

if frmnthimp_dv = 0 then delete;
run;

proc sort data=uklong_inc1;
by pid wave;
proc sort data=uklong_inc1a;
by pid wave;
proc sort data=uklong_inc1b;
by pid wave;
run;

data uklong_inc2;
merge uklong_inc1 uklong_inc1a uklong_inc1b;
by pid wave;
run;

proc sql;
create table UK_Sim8 as
select a.*, b.inc_stp, b.inc_tu, b.inc_ma
from UK_Sim7 a left join uklong_inc2 b
on a.pid=b.pid and a.wave=b.wave
order by a.pid, a.wave;

create table UK_Sim9 as
select *, max(fedu) as mx_fedu, max(medu) as mx_medu, max(eth) as mx_eth, max(sex) as mx_gndr, max(strata) as mx_strt, max(psu) as mx_psu, 
	max(Brth_Chrt) as mx_bc, max(birthy) as mx_brthy, max(birthm) as mx_brthm, min(mvyr) as mn_mvyr
from UK_Sim8
group by pid
order by pid, wave;
quit;

data UK_Sim10 (rename=(fimnlabgrs_dv=yplgrs));	
	set UK_Sim9;

if birthy = . and mx_brthy ne . then birthy = mx_brthy;
if birthm = . and mx_brthm ne . then birthm = mx_brthm;
if fedu = . and mx_fedu ne . then fedu=Mx_Fedu;
if medu = . and mx_medu ne . then medu=Mx_medu;
if eth = . and mx_eth ne . then eth=mx_eth;
if sex = . and mx_gndr ne . then sex=mx_gndr;
if strata = . and mx_strt ne . then strata=mx_strt;
if psu = . and mx_psu ne . then psu=mx_psu;
if Brth_chrt = . and mx_bc ne . then Brth_chrt=mx_bc;

*Birth Cohort;
if Brth_Chrt = . then do;
	if birthy in (1908,1909) then Brth_Chrt = 1;
	if birthy in (1910,1911,1912,1913,1914,1915,1916,1917,1918,1919) then Brth_Chrt = 1;
	if birthy in (1920,1921,1922,1923,1924,1925,1926,1927,1928,1929) then Brth_Chrt = 2;
	if birthy in (1930,1931,1932,1933,1934,1935,1936,1937,1938,1939) then Brth_Chrt = 3;
	if birthy in (1940,1941,1942,1943,1944,1945,1946,1947,1948,1949) then Brth_Chrt = 4;
	if birthy in (1950,1951,1952,1953,1954,1955,1956,1957,1958,1959) then Brth_Chrt = 5;
	if birthy in (1960,1961,1962,1963,1964,1965,1966,1967,1968,1969) then Brth_Chrt = 6;
	if birthy in (1970,1971,1972,1973,1974,1975,1976,1977,1978,1979) then Brth_Chrt = 7;
	if birthy in (1980,1981,1982,1983,1984,1985,1986,1987,1988,1989) then Brth_Chrt = 8;
	if birthy in (1990,1991,1992,1993,1994,1995,1996,1997,1998,1999) then Brth_Chrt = 9;
	if birthy in (2000,2001) then Brth_Chrt = 10;
end;

*Fertile women;
if frtl_wmn = . then do;
	if sex = 2 and 18 le age_dv le 44 then frtl_wmn = 1;
end;

*Pension Age;
bdt = mdy(birthm,15,birthy);
dagpns = 0;
if sex = 2 then do;
	if birthy le 1949 then dagpns = 1;
	if birthy = 1950 and birthm in (1,2,3) then dagpns = 1;
end;
*if sex = 2 and age_dv ge 66 then dagpns = 1;
if sex = 1 and intck('month',bdt,Int_Date,'C') ge 781 then dagpns = 1;

%macro mpa (w=,x=,y=,z=);
if sex = 2 then do;
	ped=mdy(&z,15,&y);
	if Int_Date ge ped then do;
		if birthy = &w and birthm = &x then dagpns = 1;
	end;
end;
%mend mpa;
%mpa(w=1950,x=4,y=2010,z=5);
%mpa(w=1950,x=5,y=2010,z=7);
%mpa(w=1950,x=6,y=2010,z=9);
%mpa(w=1950,x=7,y=2010,z=11);
%mpa(w=1950,x=8,y=2011,z=1);
%mpa(w=1950,x=9,y=2011,z=3);
%mpa(w=1950,x=10,y=2011,z=5);
%mpa(w=1950,x=11,y=2011,z=7);
%mpa(w=1950,x=12,y=2011,z=9);
%mpa(w=1951,x=1,y=2011,z=11);
%mpa(w=1951,x=2,y=2012,z=1);
%mpa(w=1951,x=3,y=2012,z=3);
%mpa(w=1951,x=4,y=2012,z=5);
%mpa(w=1951,x=5,y=2012,z=7);
%mpa(w=1951,x=6,y=2012,z=9);
%mpa(w=1951,x=7,y=2012,z=11);
%mpa(w=1951,x=8,y=2013,z=1);
%mpa(w=1951,x=9,y=2013,z=3);
%mpa(w=1951,x=10,y=2013,z=5);
%mpa(w=1951,x=11,y=2013,z=7);
%mpa(w=1951,x=12,y=2013,z=9);
%mpa(w=1952,x=1,y=2013,z=11);
%mpa(w=1952,x=2,y=2014,z=1);
%mpa(w=1952,x=3,y=2014,z=3);
%mpa(w=1952,x=4,y=2014,z=5);
%mpa(w=1952,x=5,y=2014,z=7);
%mpa(w=1952,x=6,y=2014,z=9);
%mpa(w=1952,x=7,y=2014,z=11);
%mpa(w=1952,x=8,y=2015,z=1);
%mpa(w=1952,x=9,y=2015,z=3);
%mpa(w=1952,x=10,y=2015,z=5);
%mpa(w=1952,x=11,y=2015,z=7);
%mpa(w=1952,x=12,y=2015,z=9);
%mpa(w=1953,x=1,y=2015,z=11);
%mpa(w=1953,x=2,y=2016,z=1);
%mpa(w=1953,x=3,y=2016,z=3);
%mpa(w=1951,x=4,y=2016,z=7);
%mpa(w=1951,x=5,y=2016,z=11);
%mpa(w=1951,x=6,y=2017,z=3);
%mpa(w=1951,x=7,y=2017,z=7);
%mpa(w=1951,x=8,y=2017,z=11);
%mpa(w=1951,x=9,y=2018,z=3);

if GOR in (1,2,3,4,5,6,7,8,9) and Country = . then country = 1;
if GOR = 10 and country = . then country = 2;
if GOR = 11 and country = . then country = 3;
if GOR = 12 and country = . then country = 4;

/*Household Composition*/
*Setting children variables to 0 if missing;
if n_chld = . then do;
	n_chld = 0;
	nch_02 = 0;
	nchld_013 = 0;
	nchld_dep = 0;
end;

if partner = 1 then do;
	if N_chld = 0 then hhcomp = 1;
	if N_chld ge 1 then hhcomp = 2;
end;
if partner in (2,3) then do;
	if N_chld = 0 then hhcomp = 3;
	if N_chld ge 1 then hhcomp = 4;
end;

if hhcomp = 1 then moecd_eq = 1.5;
if hhcomp = 2 then moecd_eq = 0.3*nchld_013 + 0.5*nchld_dep + 1.5;
if hhcomp = 3 then moecd_eq = 1.0;
if hhcomp = 4 then moecd_eq = 0.3*nchld_013 + 0.5*nchld_dep + 1.0;

*Creating income variables;
*Personal;
ypnb = sum(of fimnlabgrs_dv fimnpen_dv fimnmisc_dv inc_stp inc_tu inc_ma);
yptci = sum(of fimnpen_dv fimnmisc_dv inc_stp inc_tu inc_ma);

*Household;
hh_nb = sum(of fihhmnlabgrs_dv fihhmnpen_dv fihhmnmisc_dv);
hh_tci = sum(of fihhmnpen_dv fihhmnmisc_dv);

drop mx_fedu mx_medu mx_eth mx_gndr mx_strt mx_psu mx_bc mx_brthy nchld_013 nchld_dep fimnpen_dv fimnmisc_dv inc_stp inc_tu inc_ma
	fihhmnlabgrs_dv fihhmnpen_dv fihhmnmisc_dv bdt ped birthy birthm;
run;

data uk_sim11;
set uk_sim10;
if pno = . then delete;
where hhtype in (19,20,21,22,23);
keep hid pno age_dv hhcomp depchl_dv hh_nb hh_tci ypnb yptci pid;
run;

proc sort data=uk_sim11;
by hid pno;
run;

%MultiTranspose(out=uk_sim12, data=uk_sim11, vars=age_dv hhcomp depchl_dv hh_nb hh_tci ypnb yptci pid, by=hid, pivot=pno, library=work); 

data uk_sim13;
set uk_sim12;

array hhc [*] hhcomp1-hhcomp14;
array dch [*] depchl_dv1-depchl_dv14;
array hnb [*] hh_nb1-hh_nb14;
array htci [*] hh_tci1-hh_tci14;
array pnb [*] ypnb1-ypnb14;
array ptci [*] yptci1-yptci14;

do i=1 to dim(hhc)-1;
	do j=i+1 to dim(hhc);
		if hhc(i) ne hhc(j) then do;
			if dch(j) = 2 then do;
				if pnb(j) not in (.,0) then hnb(i) = hnb(i)-pnb(j);
				if ptci(j) not in (.,0) then htci(i) = htci(i)-ptci(j);
			end;
		end;
		if hhc(i) = 3 and hhc(j) = 3 then do;
			if dch(j) = 2 then do;
				if pnb(j) not in (.,0) then hnb(i) = hnb(i)-pnb(j);
				if ptci(j) not in (.,0) then htci(i) = htci(i)-ptci(j);
			end;
		end;
	end;
	drop i j;
end;

do i=1 to dim(hhc);
	if hnb(i) < 0 then hnb(i) = 0;
	if htci(i)< 0 then htci(i) = 0;
drop i;
end;
run;

%macro tran(out=,var_v=,out2=,);
proc transpose data=uk_sim13 out=&out prefix=&var_v;
by hid;
var &var_v:;
run;

data &out2;
	set &out;
	by hid;

if &var_v.1 = . then delete;

	retain pno1;
	if first.hid then do;	
		pno1 = 0;
	end;

	pno1+1;

drop _NAME_ _LABEL_;

run;
%mend;
%tran(out=uk_sim13a,out2=uk_sim13a1,var_v=hh_nb);
%tran(out=uk_sim13b,out2=uk_sim13b1,var_v=hh_tci);
%tran(out=uk_sim13c,out2=uk_sim13c1,var_v=ypnb);
%tran(out=uk_sim13d,out2=uk_sim13d1,var_v=yptci);

data uk_sim14;
	merge uk_sim13c1 (in=a) uk_sim13a1 uk_sim13b1 uk_sim13d1;
	by hid pno1;
	if a;
run;

proc sql;
create table uk_sim15 as
	select a.*, b.* 
	from uk_sim10 a left join uk_sim14 b
	on a.hid=b.hid and a.pno=b.pno1
	order by pid, wave;
quit;

data uk_sim16;
	set uk_sim15;

array oiv [4] hh_nb hh_tci ypnb yptci;
array niv [4] hh_nb1 hh_tci1 ypnb1 yptci1;

do i=1 to 4;
	if oiv(i) ne . and niv(i) ne . then do;
		if oiv(i) ne niv(i) then oiv(i) = niv(i);
	end;
	drop i;
end;

*Creating income variables; 
*Single;
*Equivalised;
ypnb_e = ypnb*(1/CPI);
yptci_e = yptci*(1/CPI);

*Transformed;
ypnbihs_dv=arsinh(ypnb_e);
yptciihs_dv=arsinh(yptci_e);

*Income Squared;
ypnbihs_dv_sq = ypnbihs_dv*ypnbihs_dv;

drop pno1 ypnb1 yptci1 ypnb_e yptci_e;
run;

/*Getting Spousal Data*/
data UK_SP;
	set UK_Sim16;
run;

proc sql noprint;
   select cats(name,'=',name,'_SP')
          into :renames
          separated by ' '
          from dictionary.columns
          where libname = 'WORK' and memname = 'UK_SP';
quit;

proc datasets library = work nolist;
   modify UK_SP;
   rename &renames;
run;
quit;

data UK_SP1 (rename=(wave_SP=wave));
	set UK_SP;

keep pid_SP wave_SP sex_SP age_dv_SP AGESQ_SP Brth_Chrt_SP Eth_SP Edctn_SP edu_lvl_SP SRH_SP SF12_PCS_SP SF12_MCS_SP llti_SP Limit_SP hiqual_SP 
	jbstat3_SP ypnb_SP yptci_sp ypnbihs_dv_SP yptciihs_dv_SP pno_SP Prtnrshp_Dur_SP nch_02_SP N_chld_SP dagpns_sp dlltsd_sp;

label
	sex_SP = "Spouse's Sex"
	age_dv_SP = "Spouse's Age"
	AGESQ_SP = "Spouse's Age Squared"
	Brth_Chrt_SP "Spouse's Birth Cohort"
	Eth_SP = "Spouse's Ethnicity"
	Edctn_SP = "Spouse in Education: Binary"
	edu_lvl_SP = "Spouse's Educational Level: 3 Category"
	SRH_SP = "Spouse Self-rated Health"
	SF12_PCS_SP = "Spouse's SF-12 Phsyical Component Score"
	SF12_MCS_SP = "Spouse's SF-12 Mental Component Score"
	hiqual_SP = "Spouse's highest educational qualification"
	jbstat3_SP = "Spouse's Labour Force Status: 3 Category"
	llti_SP = "Spouse's Limiting Long-standing Illness: Binary"
	Limit_SP = "Spouse's Limiting Long-standing Illness: 3 Category"
	pno_SP = "Spouse's PNO"
	Prtnrshp_Dur_SP = "Spouse's Duration of Partnership"
	dagpns_sp = "Spouse of Pension Age"
	dlltsd_sp = "Spouse's Disability Status";
run;

/*Getting Parental Information*/
data UK_Mom;
	set UK_Sim16;
run;

proc sql noprint;
   select cats(name,'=',name,'_M')
          into :renames
          separated by ' '
          from dictionary.columns
          where libname = 'WORK' and memname = 'UK_MOM';
quit;

proc datasets library = work nolist;
   modify UK_MOM;
   rename &renames;
run;
quit;

data UK_MOM1 (rename=(wave_M=wave));
	set UK_MOM;

keep pid_M age_dv_M AGESQ_M Eth_M edu_lvl_M hiqual_M jbstat_M wave_M ypnb_M mvyr_M;

label
	Eth_M = "Mother's Ethnicity"
	age_dv_M = "Mother's Age"
	AGESQ_M = "Mother's Age Squared"
	edu_lvl_M = "Mother's Educational Level: 3 Category"
	hiqual_M = "Mother's highest educational qualification"
	jbstat_M = "Mother's Labour Force Status";
run;

data UK_Dad;
	set UK_Sim16;
run;

proc sql noprint;
   select cats(name,'=',name,'_F')
          into :renames
          separated by ' '
          from dictionary.columns
          where libname = 'WORK' and memname = 'UK_DAD';
quit;

proc datasets library = work nolist;
   modify UK_DAD;
   rename &renames;
run;
quit;

data UK_DAD1 (rename=(wave_F=wave));
	set UK_DAD;

keep pid_F age_dv_F AGESQ_F Eth_F edu_lvl_F hiqual_F jbstat_F wave_f ypnb_F mvyr_F;

label
	Eth_F = "Father's Ethnicity"
	age_dv_F = "Father's Age"
	AGESQ_F = "Father's Age Squared"
	edu_lvl_F = "Father's Educational Level: 3 Category"
	hiqual_F = "Father's highest educational qualification"
	jbstat_F = "Father's Labour Force Status";
run;

/*Getting Dependent Child Income for Household Income Calculation*/
data UK_DC;
	set UK_Sim16;
run;

proc sql noprint;
   select cats(name,'=',name,'_DC')
          into :renames
          separated by ' '
          from dictionary.columns
          where libname = 'WORK' and memname = 'UK_DC';
quit;

proc datasets library = work nolist;
   modify UK_DC;
   rename &renames;
run;
quit;

data UK_DC1 (rename=(wave_DC=wave));
	set UK_DC;

where depchl_dv_DC = 1;

keep pid_DC hid_DC wave_DC mnspid_DC fnspid_DC ypnb_DC yptci_DC;
run;

data UK_DC2;
	set UK_DC1;

where mnspid_DC ne .;
keep pid_DC hid_DC wave mnspid_DC ypnb_DC yptci_DC;
run;

proc sql;
create table UK_DC2a as
	select *, sum(ypnb_dc) as ypnbhh_dc, sum(yptci_DC) as yptcihh_dc
	from UK_DC2
	group by hid_DC
	order by mnspid_DC, wave;
quit;

proc sort data=UK_DC2a nodupkey;
by hid_DC;
run;

data UK_DC3 (rename=(ypnb_dc=ypnb1_dc yptci_DC=yptci1_DC));
	set UK_DC1;

where fnspid_DC ne .;
keep pid_DC hid_DC wave fnspid_DC ypnb_DC yptci_DC;
run;

proc sql;
create table UK_DC3a as
	select *, sum(ypnb1_dc) as ypnbhh1_dc, sum(yptci1_DC) as yptcihh1_dc
	from UK_DC3
	group by hid_DC
	order by fnspid_DC, wave;
quit;

proc sort data=UK_DC3a nodupkey;
by hid_DC;
run;

/*Merging Data Together*/
proc sql;
create table UK_SIM17 as
	select a.*, b.*
	from UK_SIM16 a left join UK_SP1 b
	on a.ppid=b.pid_sp and a.wave=b.wave
	order by a.pid, a.wave;

create table UK_SIM18 as
	select a.*, b.*
	from UK_SIM17 a left join UK_MOM1 b
	on a.mnspid=b.pid_m and a.wave=b.wave
	order by a.pid, a.wave;

create table UK_SIM19 as
	select a.*, b.*
	from UK_SIM18 a left join UK_Dad1 b
	on a.fnspid=b.pid_f and a.wave=b.wave
	order by a.pid, a.wave;

create table UK_SIM20 as
	select a.*, b.ypnbhh_dc, b.yptcihh_dc
	from UK_SIM19 a left join UK_DC2a b
	on a.pid=b.mnspid_DC and a.wave=b.wave
	order pid, wave;

create table UK_SIM21 as
	select a.*, b.ypnbhh1_dc, b.yptcihh1_dc
	from UK_SIM20 a left join UK_DC3a b
	on a.pid=b.fnspid_DC and a.wave=b.wave
	order pid, wave;

create table UK_SIM22 as
	select *, max(edu_lvl_M) as mx_edu_lvl_M, max(hiqual_M) as mx_hiqual_M, min(mvyr_M) as mn_mvyr_M
	from UK_SIM21
	group by pid, mnspid
	order by pid, wave;

create table UK_SIM23 as
	select *, max(edu_lvl_F) as mx_edu_lvl_F, max(hiqual_F) as mx_hiqual_F, min(mvyr_f) as mn_mvyr_f
	from UK_SIM22
	group by pid, fnspid
	order by pid, wave;
quit;

proc expand data=UK_SIM23 out=UK_SIM24 method=none;
	by pid;
	id wave;

convert edctn = lg_edu / transformout=(lag 1);
convert hiqual = lg_hql / transformout=(lag 1);
convert jbstat = lg_jbst / transformout=(lag 1);
convert jbstat = ld_jbst / transformout=(lead 1);
convert dlvprnt = lg_lvp / transformout=(lag 1);
convert PARTNER = lg_prtnr / transformout=(lag 1);
convert PARTNER = ld_prtnr / transformout=(lead 1);
convert mastat_dv = lgmstt / transformout=(lag 1);
convert mastat_dv = ldmstt / transformout=(lead 1);
convert ppid = lgppid / transformout=(lag 1); 
convert ppid = ldppid / transformout=(lead 1); 
convert age_dv = ldage / transformout=(lead 1); 
run;

data UK_SIM25;
	set UK_SIM24;
	by pid;

array org [4] edu_lvl_M hiqual_M edu_lvl_F hiqual_F;
array fim [4] mx_edu_lvl_M mx_hiqual_M mx_edu_lvl_F mx_hiqual_F;

do i=1 to 4;
	if fim(i) ne . and org(i) = . then org(i)=fim(i);
drop i;
end;

*Filling in missing information from spouse's information;
if Prtnrshp_Dur = . and Prtnrshp_Dur_SP ne . then Prtnrshp_Dur = Prtnrshp_Dur_SP;
if nch_02 = . and nch_02_SP ne . then nch_02 = nch_02_SP;
if N_chld = . and N_chld_SP ne . then N_chld = N_chld_SP; 

if edu_lvl_M = . and medu ne . then edu_lvl_M = medu;
if edu_lvl_F = . and fedu ne . then edu_lvl_F = fedu;

*Setting Ethnicity to be parent's ethnicity;
if Eth_M ne . and Eth_F ne . then do;
	if fnpid ne . and mnpid ne . then do;
		if Eth_M = Eth_F and Eth = . then Eth = Eth_M;
		if Eth_M ne Eth_F then eth = 5;
	end;
end;
if fnpid = . and mnpid ne . then do;
	if Eth_M ne . and Eth = . then Eth = Eth_M;
end;
if fnpid ne . and mnpid = . then do;
	if Eth_F ne . and Eth = . then Eth = Eth_F;
end;

*Creating a going back to education variable;
if lg_edu ne . and edctn ne . then do;
	if lg_edu = 0 and edctn = 1 then Re_EDU = 1;
end;

if edu_brk = 1 then do;
	edctn = 0;
	if Re_EDU = . then Re_EDU = 1;
end;

if Re_EDU = 1 and edu_brk = . then edu_brk = 1;

*Creating a new partnership variable;
if lg_prtnr ne . and partner ne . then do;
	if lg_prtnr in (2,3) and partner = 1 then nw_prtnrshp = 1;
	if lg_prtnr in (2,3) and partner in (2,3) then nw_prtnrshp = 0;
end;

if lgppid ne . and ppid ne . then do;
	if lgppid = ppid and nw_prtnrshp = . then nw_prtnrshp = 0;
	if lgppid ne ppid and nw_prtnrshp in (.,0) then nw_prtnrshp = 1;
end;

if ldppid ne . and ppid ne . then do;
	if ldppid = ppid and nw_prtnrshp = . then nw_prtnrshp = 0;
	if ldppid ne ppid and nw_prtnrshp in (.,0) then nw_prtnrshp = 1;
end;

if ppid = . then nw_prtnrshp = 0;

if first.pid and ldppid = . then nw_prtnrshp = 0;

*Creating a partnership dissolution variable;
if lgmstt ne . and mastat_dv ne . then do;
	if lgmstt in (2,3,10) and mastat_dv in (1,4,5,7,8) then prtnrshp_brk = 1;
	if lgmstt in (2,3,10) and mastat_dv in (2,3,10) then prtnrshp_brk = 0;
end;

if ldmstt ne . and mastat_dv ne . then do;
	if ldmstt in (2,3,10) and mastat_dv in (2,3,10) then prtnrshp_brk = 0;
end;

if ldppid ne . and ppid ne . then do;
	if ldppid = ppid then prtnrshp_brk = 0;
	if ldppid ne ppid then prtnrshp_brk = 1;
end;

if ldppid = . and ppid = . then prtnrshp_brk = 0;

if first.pid and ldppid = . then prtnrshp_brk = 0;

*Setting Partnerhship Duration equal to 1 when new partnership;
if nw_prtnrshp = 1 and Prtnrshp_Dur = . then Prtnrshp_Dur = 1;

*Differences in Ages between couples;
Cple_Age_Diff = age_dv - age_dv_SP;

*Both partners present in data prior to dissolution of partnership;
if prtnrshp_brk = 1 then do;
	if pid ne . and pid_SP ne . then prtnrs_prsnt = 1;
end;

*Have left education;
if lg_edu = 1 then do;
	if jbstat not in (.,7) then Lft_edu = 1;
	if age_dv = 30 then Lft_edu = 1;
end;

*Entered into Retirement;
if lg_jbst ne . and jbstat ne . then do;
	if lg_jbst ne . and jbstat ne . then drtren = 0;
	if lg_jbst ne 4 and jbstat = 4 then drtren = 1;
end;

if lg_jbst = . and ld_jbst = . then drtren = 0;
if first.pid and lg_jbst = . and jbstat ne . then drtren = 0;

*Identifying and dropping same sex couples;
if sex ne . and sex_SP ne . then do;
	if sex=sex_SP then SSCple=1;
	if sex ne sex_SP then SSCple=0;
end;

if sex ne . and ncrr5 ne . and SSCple = . then do;
	if sex=ncrr5 then SSCple=1;
	if sex ne ncrr5 then SSCple=0;
end;

*Have left parental home for first time;
**First set living with parent variable to 0 if have not continuously lived with parent - this cannot be done completely but I am doing the best
I can by using year of moving into current home as proxy for having continuously lived with parent;
if mn_mvyr ne . and dlvprnt ne . then do;
	if mn_mvyr_m ne . and mn_mvyr_f = . then do;
		if mn_mvyr ne mn_mvyr_m and dlvprnt = 1 then dlvprnt = 0;
	end;
	if mn_mvyr_m = . and mn_mvyr_f ne . then do;
		if mn_mvyr ne mn_mvyr_f and dlvprnt = 1 then dlvprnt = 0;
	end;
	if mn_mvyr_m ne . and mn_mvyr_f ne . then do;
		if ((mn_mvyr ne mn_mvyr_m) or (mn_mvyr ne mn_mvyr_f)) and dlvprnt = 1 then dlvprnt = 0;
	end;
end;

if lg_lvp ne . and dlvprnt ne . then do;
	if lg_lvp = 1 and dlvprnt = 0 then dlftphm = 1;
	if lg_lvp = dlvprnt then dlftphm = 0;
end;

if first.pid and dlvprnt ne . then dlftphm = 0;

*Identifying adult children living with parents;
if 18 < age_dv le 102 then do;
	if mnspid ne . then AChild = 1;
	if mnspid = . then AChild = 0;
	if fnspid ne . then AChild = 1;
	if fnspid = . then AChild = 0;
end;

if age_dv le 18 then do;
	if mnspid ne . then AChild = 0;
	if fnspid ne . then AChild = 0;
end; 

*Creating income variables;
*Dependent Children Income;
ypnb_DC = ypnbhh_DC;
if ypnb_DC = . then ypnb_DC = ypnbhh1_dc;

yptci_dc = yptcihh_dc;
if yptci_dc =. then  yptci_dc = yptcihh1_dc;

*Household Income;
if hhcomp in (1,2) then do;
	yhhnb = sum(of ypnb ypnb_SP ypnb_DC);
	yhhtci= sum(of yptci yptci_SP yptci_DC);
end;
if hhcomp in (3,4) then do;
	yhhnb = sum(of ypnb ypnb_DC);
	yhhtci= sum(of yptci yptci_DC);
end;

*Parental Income;
yprntnb = sum(of ypnb_M ypnb_F);

*Equivalised;
yhhnb_e = (yhhnb/moecd_eq)*(1/CPI);
yhhtci_e = (yhhtci/moecd_eq)*(1/CPI);
yprntnb_e = (yprntnb/moecd_eq)*(1/CPI);
yplgrs_e = yplgrs*(1/CPI);

*Transformed;
yhhnbihs_dv=arsinh(yhhnb_e);
yhhtciihs_dv=arsinh(yhhtci_e);
yplgrs_dv=arsinh(yplgrs_e);
yprntnbihs_dv=arsinh(yprntnb_e);

*Income differential;
ynbcpdf_dv = ypnbihs_dv-ypnbihs_dv_SP;

drop Prtnrshp_Dur_SP lg_prtnr ld_prtnr lgmstt ldmstt lg_lvp yhhnb_e yhhtci_e hh_nb hh_tci yplgrs_e yplgrs yprntnb_e mn_mvyr mn_mvyr_m mn_mvyr_f
	ldppid lgppid ld_jbst ldage;
run;

data INC_Qnt;
set UK_SIM25;

if hid = . then delete;
keep hid wave hhcomp yhhnbihs_dv yprntnbihs_dv;
run;

proc sort data=INC_Qnt nodupkey;
by hid hhcomp;
run;

proc sort data=INC_Qnt;
by wave;
run;

proc rank data=INC_Qnt out=INC_Qnt1 groups=5;
by wave;
var yhhnbihs_dv yprntnbihs_dv;
ranks yhhnbihs_dv_Q5 yprntnbihs_dv_Q5;
run;

proc sql;
create table UK_SIM26 as
	select a.*, b.yhhnbihs_dv_Q5, b.yprntnbihs_dv_Q5
	from UK_SIM25 a, INC_Qnt1 b
	where a.hid=b.hid and a.hhcomp=b.hhcomp
	order by a.pid, a.wave;

create table UK_SIM27 as
	select *, max(SSCple) as mx_SSCple, count(distinct ppid) as N_prtnr
	from UK_SIM26
	group by pid
	order by pid, ppid, wave;
quit;

data UK_SIM28;
	set UK_SIM27;

	Yrs_Prtnrshp + 1;

	by pid ppid;

/*New Partnerships Spells*/
if first.ppid then do;
	Yrs_Prtnrshp = Prtnrshp_Dur;
end;

if ppid = . then do;
	Yrs_Prtnrshp = .;
	*if prtnrshp_brk ne 1 then prtnrshp_brk = .;
end;
run;

proc sort data=UK_Sim28;
by pid wave;
run;

data UK_Sim29;
	set UK_Sim28;
	by pid;

/*New Education Spells*/
retain Nw_Edu;
if first.pid then do;	
	Nw_Edu = 0;
end;

if Re_EDU = 1 then Nw_Edu + (jbstat=7);

*Creating a variable that indicates whether educational qualification changed after returning to education;
if lg_hql ne . and hiqual ne . then do;
	if Nw_edu = 1 and lg_jbst = 7 and jbstat not in (.,7) then do;
		if lg_hql < hiqual then Nw_qual = 3;
		if lg_hql = hiqual then Nw_qual = 2;
		if lg_hql not in (.,5) and hiqual = 5 then Nw_qual = 4;
	end;
end;

*Creating a left education variable for those who have returned to education;
if nw_edu ge 1 and Lft_edu = 1 then do;
	Lft_redu = 1;
	Lft_edu = 0;
	if jbstat = 7 then Lft_redu = 0;
	if age_dv = 30 then Lft_redu = 1;
end;
if nw_edu ge 1 and Lft_edu = 0 then do;
	if jbstat = 7 then Lft_redu = 0;
	if age_dv = 30 then Lft_redu = 1;
end;
*if Lft_redu and nw_edu = 1 then Lft_redu = 0;

if Nw_Edu ge 1 and jbstat ne 7 then Nw_Edu = 0;
if Nw_Edu = 0 and Nw_qual = . then Nw_qual = 0;
if Nw_Edu ge 1 and Nw_qual = . then Nw_qual = 1;

*Setting binary education variable equal to 0 if participants have returned to education;
if nw_edu ge 1 and edctn = 1 then edctn = 0;
if Re_EDU = 1 and edctn = 1 then edctn = 0;

/*Same sex couples*/
if SSCple = . and mx_SSCple ne . then SSCple=mx_SSCple;

/*Aged 16-29*/
if 16 le age_dv le 29 then EDU_Age = 1;
else if age_dv ge 30 then EDU_Age = 0;

*Categories for Educational Age;
if 16 le age_dv le 20 then EDU_Age_Cat = 1;
if 21 le age_dv le 24 then EDU_Age_Cat = 2;
if 25 le age_dv le 29 then EDU_Age_Cat = 3;

/*Simplifying household type categories*/
if hhtype in (1,2,8) then hhtype_cat = 1;
if hhtype in (3,6,16,17,19,22) then hhtype_cat = 2;
if hhtype in (10,11,12) then hhtype_cat = 3;
if hhtype in (4,5) then hhtype_cat = 4;
if hhtype in (18,20,21,23) then hhtype_cat = 5;

/*Binary employment status for estimating returning to education*/
if jbstat in (1,2) then Emplyd = 1;
if jbstat in (3,4,5,6,8,9,10,11,97) then Emplyd = 0;

/*Alternative Employment Status with Disabled as Own Category*/
if JBSTAT in (1,2) then les_c4 = 1;
if JBSTAT = 7 or Edctn = 1 then les_c4 = 2;
if JBSTAT = 8 then les_c4 = 3;
if JBSTAT in (3,4,5,6,9,10,11,97) then les_c4 = 4;

/*Changing Income Quintile Variable to Range from 1-5 Rather than 0-4*/
yhhnbihs_dv_Q5 = yhhnbihs_dv_Q5 + 1;
yprntnbihs_dv_Q5 = yprntnbihs_dv_Q5 + 1;

/*Everyone with no children = 0*/
if nch_02 = . then nch_02 = 0;
if N_chld = . then N_chld = 0;

label
	Nw_Edu = 'Returned to Education after a break'
	SSCple = 'Same Sex Couple'
	N_Prtnr = 'Number of Partners'
	AChild = 'Live with Adult Child (18+)'
	EDU_Age = 'Aged 16-29 for Education Analyses Restriction'
	Nw_qual = 'Whether New Educational Spell Led to New Qualifications'
	EDU_Age_Cat = 'Educational Age Categories'
	Emplyd = 'Employment Status: Binary'
	Lft_edu = 'Left education'
	Lft_redu = 'Left education after returning to education'
	yhhnbihs_dv_Q5 = "Adjusted Annual Household Income Quintile"
	nw_prtnrshp = "Entered a new Partnership"
	prtnrshp_brk = "Partnership dissolution"
	Cple_Age_Diff = "Difference in Age between partners in a couple"
	prtnrs_prsnt = "Both partners present in wave prior to dissolution of partnership"
	frtl_wmn = "Fertile Women Indicator"
	yhhnbihs_dv = "Inverse Hyperbolic Sine Gross Non-Benefit Household Income"
	yhhtciihs_dv = "Inverse Hyperbolic Sine Gross Non-employment or Benefit Household Income"
	ypnbihs_dv = "Inverse Hyperbolic Sine Gross Non-Benefit Personal Income"
	yptciihs_dv = "Inverse Hyperbolic Sine Gross Non-employment or Benefit Personal Income"
	yplgrs_dv = "Inverse Hyperbolic Sine Gross Employment Personal Income"
	dlvprnt = 'Live with Parent'
	dlftphm = 'Left Parental Home for First Time'
	dlrtrd = 'Retired/Pensioner'
	drtren = 'Entered Retirement'
	yprntnbihs_dv_Q5 = "Adjusted Annual Parental Income Quintile"
	dagpns = 'Pension Age'
	les_c4 = 'Employment Status: 4 Category';

format AChild dlvprnt dlftphm dlrtrd drtren yn. Nw_qual nwq. hhtype_cat hht.;

drop mx_SSCple lg_edu lg_hql lg_jbst Prtnrshp_Dur;
run;

proc sort data=UK_SIM29;
by pid wave;
run;

data UK_SIM30;
	set UK_SIM29;
	by pid;

if Nw_Edu > 1 then Nw_Edu = 1;

	retain Yrs_Nw_Edu;
	if first.pid then do;	
		Yrs_Nw_Edu = 0;
	end;

	Yrs_Nw_Edu + (Nw_Edu=1);
	Time=istrtdaty;
run;

proc sort data=UK_SIM30;
by hid;
run;

data UK_Sim31;
	set UK_Sim30;
	by hid;

if Nw_Edu = 0 and Yrs_Nw_Edu ge 1 then Yrs_Nw_Edu = 0;

label
	Yrs_Prtnrshp = 'Number of Years in Partnership'
	Yrs_Nw_Edu = 'Number of Years in New Educational Spell';

drop prtnrs_prsnt;
run;

proc sort data=UK_Sim31;
by pid wave;
run;

data UK_Sim32;
	set UK_Sim31;
	by pid;

if first.pid then do;
	inEducStart = 0;
	inEducEnd = 0;
	inREDUStart = 0;
	inREDUEnd = 0;
	inPHStart = 0;
	inPHEnd = 0;
	inUExStart = 0;
	inUExEnd = 0;
	inRetStart = 0;
	inRetEnd = 0;


end;
retain inEducStart inEducEnd inREDUStart inREDUEnd inPHStart inPHEnd inUExStart inUExEnd inRetStart inRetEnd;

if Nw_Edu = 1 and edctn = 1 then Nw_Edu = .;
if Nw_Prtnrshp = . and ppid = . then Nw_Prtnrshp = 0;
if prtnrshp_brk = . and ppid = . then prtnrshp_brk = 0;

if edctn = 1 then inEducStart = 1;
if edctn = 0 then inEducEnd = 0;
if Nw_edu = 1 then inREDUStart = 1;
if Nw_edu = 0 then inREDUEnd = 0;
if dlftphm = 0 then inPHStart = 0;
if dlftphm = 1 then inPHEnd = 1;
if prtnrshp_brk = 0 then inUExStart = 0;
if prtnrshp_brk = 1 then inUExEnd = 1;
if drtren = 0 then inRetStart = 0;
if drtren = 1 then inRetEnd = 1;
run;

data UK_Sim33;
	set UK_Sim32;
	by pid;

if first.pid then do;
	EDU_End = inEducEnd;
	REDU_End = inREDUEnd;
	PH_Strt = inPHStart;
	PH_End = inPHEnd;
	UEx_Start = inUExStart;
	UEx_End = inUExEnd;
	Ret_Strt = inRetStart;
	Ret_end = inRetEnd;
end;

retain EDU_End REDU_End PH_Strt PH_End UEx_End UEx_Start Ret_end;

EDU_End + (edctn=0);
REDU_End + (Nw_Edu=0);
if dlvprnt = 1 then PH_Strt + (dlftphm=0);
PH_End + (dlftphm=1);
if ppid ne . then UEx_Start + (prtnrshp_brk=0);
UEx_End + (prtnrshp_brk=1);
if jbstat in (1,2,3,5,6,7,8,9,10,11,97) then Ret_Strt + (drtren=0); 
Ret_end + (drtren=1);

if 16 le age_dv le 29 then do;
	if inEducStart = 1 and EDU_End in (0,1) then sedcsmpl = 1;
end;
if sedcsmpl = . then sedcsmpl = 0;

if sedcsmpl = 1 then scedsmpl = 1;
if sedcsmpl = 0 then scedsmpl = 0;
if sedcsmpl = 1 and edctn = 0 then scedsmpl = 0;

if 16 le age_dv le 45 then do;
	if inREDUStart = 1 and REDU_End in (0,1) then sedrsmpl = 1;
	if sedrsmpl = . then sedrsmpl = 1;
end;

run;

proc expand data=UK_SIM33 out=UK_SIM34 method=none;
	by pid;
	id wave;

convert PH_End = ldphe / transformout=(lead 1);
convert UEx_End = lduex / transformout=(lead 1);
convert Ret_end = ldre / transformout=(lead 1);

convert PH_End = lgphe / transformout=(lag 1);
convert UEx_End = lguex / transformout=(lag 1);
convert Ret_end = lgre / transformout=(lag 1);
run;

data UK_Sim35;
	set UK_Sim34;

if lgphe = 1 and PH_End = 1 then PH_End = 0;

if lguex = 1 and UEx_End = 1 then UEx_End = 0;

if lgre = 1 and Ret_end = 1 then Ret_end = 0;

if PH_Strt ge 1 and PH_End = 1 then slphsmpl = 1;
if dlvprnt = 1 and slphsmpl = . then slphsmpl = 1;

if UEx_Start ge 1 and UEx_End = 1 then suexsmpl = 1;
if partner = 1 and suexsmpl = . then suexsmpl = 1;

if age_dv ge 50 then do;
	if Ret_Strt ge 1 and Ret_end = 1 then srtrsmpl = 1;
	if jbstat ne . and srtrsmpl = . then srtrsmpl = 1;
end;

if sedcsmpl = 1 and sedrsmpl = 1 then sedrsmpl = 0;

drop inEducStart inEducEnd inREDUStart inREDUEnd inPHStart inPHEnd EDU_End REDU_End PH_Strt PH_End inUExStart inUExEnd ldphe lgphe lduex lguex ldre
	lgre UEx_Start UEx_End Ret_Strt inRetStart Ret_end inRetEnd;
run;

data UK_Sim36;

keep pid hid wave sex age_dv AGESQ Brth_Chrt Eth partner hhsize nkids nchldrn_02 nch_02 N_chld dchygag Edctn edu_lvl Lft_edu JBSTAT3 les_c4 JBSTAT_NS 
	JBSTAT_NR Emplyd dlltsd dlrtrd SRH SF12_PCS SF12_MCS llti Limit yhhnbihs_dv_Q5 yhhnbihs_dv yhhtciihs_dv yplgrs_dv ypnbihs_dv yptciihs_dv ypnbihs_dv_sq 
	hhcomp country gor urban AChild dlvprnt dlftphm SSCple N_Prtnr Nw_Prtnrshp Yrs_Prtnrshp prtnrshp_brk Cple_Age_Diff ynbcpdf_dv 
	Nw_Edu Yrs_Nw_Edu Lft_redu edu_brk Nw_qual EDU_Age EDU_Age_Cat Edu_fees_Chng Uni_Fees hiqual jbstat drtren frtl_wmn dchpd UK_GFR dagpns sex_SP 
	age_dv_SP AGESQ_SP Brth_Chrt_SP Eth_SP Edctn_SP edu_lvl_SP SRH_SP SF12_PCS_SP SF12_MCS_SP llti_SP Limit_SP dlltsd_sp hiqual_SP jbstat3_SP ypnbihs_dv_SP 
	yptciihs_dv_SP dagpns_sp age_dv_M AGESQ_M Eth_M edu_lvl_M hiqual_M jbstat_M age_dv_F AGESQ_F Eth_F edu_lvl_F hiqual_F jbstat_F yprntnbihs_dv_Q5 
	strata psu pno ppid dhhwt dimxwt discxwt dimlwt disclwt istrtdaty time sedcsmpl sedrsmpl slphsmpl suexsmpl srtrsmpl scedsmpl;

retain pid hid wave sex age_dv AGESQ Brth_Chrt Eth partner hhsize nkids nchldrn_02 nch_02 N_chld dchygag Edctn edu_lvl Lft_edu JBSTAT3 les_c4 JBSTAT_NS 
	JBSTAT_NR Emplyd dlltsd dlrtrd SRH SF12_PCS SF12_MCS llti Limit yhhnbihs_dv_Q5 yhhnbihs_dv yhhtciihs_dv yplgrs_dv ypnbihs_dv yptciihs_dv ypnbihs_dv_sq 
	hhcomp country gor urban AChild dlvprnt dlftphm SSCple N_Prtnr Nw_Prtnrshp Yrs_Prtnrshp prtnrshp_brk Cple_Age_Diff ynbcpdf_dv 
	Nw_Edu Yrs_Nw_Edu Lft_redu edu_brk Nw_qual EDU_Age EDU_Age_Cat Edu_fees_Chng Uni_Fees hiqual jbstat drtren frtl_wmn dchpd UK_GFR dagpns sex_SP 
	age_dv_SP AGESQ_SP Brth_Chrt_SP Eth_SP Edctn_SP edu_lvl_SP SRH_SP SF12_PCS_SP SF12_MCS_SP llti_SP Limit_SP dlltsd_sp hiqual_SP jbstat3_SP ypnbihs_dv_SP 
	yptciihs_dv_SP dagpns_sp age_dv_M AGESQ_M Eth_M edu_lvl_M hiqual_M jbstat_M age_dv_F AGESQ_F Eth_F edu_lvl_F hiqual_F jbstat_F yprntnbihs_dv_Q5 
	strata psu pno ppid dhhwt dimxwt discxwt dimlwt disclwt istrtdaty time sedcsmpl sedrsmpl slphsmpl suexsmpl srtrsmpl scedsmpl;
set UK_Sim35;
run;

/*Creating Permanent Datasets and Exporting Data to Stata*/
data inapp.UK_Mcrsmltn_full;
	set UK_SIM36;
run;

proc export data=UK_SIM36
	outfile= "D:\Home\cbooker\Papers in Progress\INAPP\Data\Stata\UK_Mcrsmltn_full.dta" replace;
run;

/*Creating dataset with just variables used in transition model estimations*/
data UK_SIM37 (rename=(pid=idperson hid=idhh wave=swv sex=dgn age_dv=dag AGESQ=dag_sq nch_02=dnc02 N_chld=dnc Edctn=ded edu_lvl=deh_c3 Lft_edu=sedex 
	JBSTAT3=les_c3 SRH=dhe yhhnbihs_dv_Q5=ydses_c5 hhcomp=dhhtp_c4 gor=drgnl SSCple=ssscp Nw_Prtnrshp=dcpen Yrs_Prtnrshp=dcpyy prtnrshp_brk=dcpex 
	Cple_Age_Diff=dcpagdf Nw_Edu=der EDU_Age=sedag UK_GFR=dukfr age_dv_SP=dagsp AGESQ_SP=dagsp_sq edu_lvl_SP=dehsp_c3 jbstat3_SP=lessp_c3 SRH_SP=dhesp 
	edu_lvl_M=dehm_c3 edu_lvl_F=dehf_c3 ppid=idpartner time=stm frtl_wmn=sprfm partner=dcpst JBSTAT_NS=lesns_c2 JBSTAT_NR=lesnr_c2));
	set inapp.UK_Mcrsmltn_full;

if sex=2 then sex=0;

*Differential employment status;
if JBSTAT3 = 1 and jbstat3_SP = 1 then lesdf_c4 = 1;
if JBSTAT3 = 1 and jbstat3_SP in (2,3) then lesdf_c4 = 2;
if JBSTAT3 in (2,3) and jbstat3_SP = 1 then lesdf_c4 = 3;
if JBSTAT3 in (2,3) and jbstat3_SP in (2,3) then lesdf_c4 = 4;

drop Brth_Chrt Eth hhsize nkids Emplyd SF12_PCS SF12_MCS llti Limit country urban AChild N_Prtnr hiqual jbstat Yrs_Nw_Edu Lft_redu Nw_qual edu_brk 
	EDU_Age_Cat Edu_fees_Chng Uni_Fees sex_SP Brth_Chrt_SP Eth_SP Edctn_SP SF12_PCS_SP SF12_MCS_SP llti_SP Limit_SP hiqual_SP age_dv_M AGESQ_M Eth_M hiqual_M
	jbstat_M age_dv_F AGESQ_F Eth_F hiqual_F jbstat_F strata psu pno istrtdaty; 
run;

proc sort data=UK_Sim37;
by idperson swv;
run;

/*Adding Children 0-15 into dataset*/
data UK_U16 (rename=(hid=idhh pid=idperson sex_dv=dgn age_dv=dag intdaty_dv=stm mnspid=idmother fnspid=idfather wave=swv));
	set UKLong_iall;
	where 0 le age_dv le 15;

keep hid pid sex_dv age_dv intdaty_dv mnspid fnspid wave;
run;

data UK_SIM38;
	set UK_SIM37 UK_U16;
run;

/*Creating Permanent Datasets and Exporting Data to Stata*/
data inapp.UK_Mcrsmltn;
	set UK_SIM38;
run;

proc export data=UK_SIM38
	outfile= "D:\Home\cbooker\Papers in Progress\INAPP\Data\Stata\UK_Mcrsmltn.dta" replace;
run;

*Saving UK_Long18a so don't have to take time creating that dataset;
data inapp.UK_Long18a;
	set UK_Long18a;
run;
