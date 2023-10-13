package simpaths.data.filters;

import simpaths.model.Person;
import simpaths.model.enums.Education;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Les_c4;
import microsim.statistics.ICollectionFilter;

public class GenderEducationWorkingCSfilter implements ICollectionFilter{

	private Gender gender;
	private Education education;

	public GenderEducationWorkingCSfilter(Gender gender, Education education) {
		super();
		this.gender = gender;
		this.education = education;
	}
	
	public boolean isFiltered(Object object) {
		if(object instanceof Person) {
			Person person = (Person) object;
			return (person.getDgn().equals(gender) &&
					person.getDeh_c3().equals(education) &&
					person.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed) &&
					person.getGrossEarningsYearly() >= 1. &&
					person.getLabourSupplyHoursWeekly() > 0);
		}
		else throw new IllegalArgumentException("Object passed to GenderEducationWorkingCSfilter must be of type Person!");
	}			
}
