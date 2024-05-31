package simpaths.model.enums;

import simpaths.model.Person;

public enum Lesdf_c4 {
	BothEmployed,
	EmployedSpouseNotEmployed,
	NotEmployedSpouseEmployed,
	BothNotEmployed;

	public static Lesdf_c4 getCode(Person person, Person spouse) {
		if (person==null || spouse==null)
			throw new NullPointerException("request to retrieve lesdf_c4 code for null pointer");
		if (person.getEmployed()==0 && spouse.getEmployed()==0)
			return Lesdf_c4.BothNotEmployed;
		else if (person.getEmployed()==1 && spouse.getEmployed()==0)
			return Lesdf_c4.EmployedSpouseNotEmployed;
		else if (person.getEmployed()==0 && spouse.getEmployed()==1)
			return Lesdf_c4.NotEmployedSpouseEmployed;
		else
			return Lesdf_c4.BothEmployed;
	}
}
