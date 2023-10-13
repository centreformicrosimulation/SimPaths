package simpaths.model.enums;

public enum Les_c4 {
	EmployedOrSelfEmployed,
	NotEmployed,
	Student,
	Retired; 	//TODO: Added retired status but need to check if it affects other processes such as inSchool / labour market
//	Sick,			//Sick or disabled

	public static Les_c4 convertLes_c7_To_Les_c4(Les_c7_covid les_c7_covid) {
		if (les_c7_covid.equals(Les_c7_covid.Employee) || les_c7_covid.equals(Les_c7_covid.SelfEmployed) || les_c7_covid.equals(Les_c7_covid.FurloughedFull) || les_c7_covid.equals(Les_c7_covid.FurloughedFlex)) {
			return Les_c4.EmployedOrSelfEmployed;
		} else if (les_c7_covid.equals(Les_c7_covid.NotEmployed)) {
			return Les_c4.NotEmployed;
		} else if (les_c7_covid.equals(Les_c7_covid.Student)) {
			return Les_c4.Student;
		} else if (les_c7_covid.equals(Les_c7_covid.Retired)) {
			return Les_c4.Retired;
		} else return null;
	}
}
