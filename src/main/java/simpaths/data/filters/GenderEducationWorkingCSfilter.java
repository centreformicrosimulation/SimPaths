package simpaths.data.filters;

import simpaths.model.Person;
import simpaths.model.enums.Education;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Les_c4;
import microsim.statistics.ICollectionFilter;

public class GenderEducationWorkingCSfilter implements ICollectionFilter{

	private Gender demSex;
	private Education education;

	public GenderEducationWorkingCSfilter(Gender demSex, Education education) {
		super();
		this.demSex = demSex;
		this.education = education;
	}
	
	public boolean isFiltered(Object object) {
		if(object instanceof Person) {
			Person person = (Person) object;
			return (person.getDemMaleFlag().equals(demSex) &&
					person.getEduHighestC4().equals(education) &&
					person.getLabC4().equals(Les_c4.EmployedOrSelfEmployed) &&
					person.getGrossEarningsYearly() >= 1. &&
					person.getLabourSupplyHoursWeekly() > 0);
		}
		else throw new IllegalArgumentException("Object passed to GenderEducationWorkingCSfilter must be of type Person!");
	}			
}
