package simpaths.model.enums;

/*
This enum extends the 4-category Les_c4, by adding Self-employed and furloughed. It is used when evaluating transitions in the reduced-form labour supply module.

 */

public enum Les_c7_covid {
	Employee,
	SelfEmployed,
	FurloughedFull,
	FurloughedFlex,
	NotEmployed,
	Student,
	Retired;    //TODO: Added retired status but need to check if it affects other processes such as inSchool / labour market
//	Sick,			//Sick or disabled

}