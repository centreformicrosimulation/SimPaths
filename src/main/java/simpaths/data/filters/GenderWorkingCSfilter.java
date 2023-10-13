package simpaths.data.filters;

import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Les_c4;
import microsim.statistics.ICollectionFilter;

public class GenderWorkingCSfilter implements ICollectionFilter{

	private Gender gender;

	public GenderWorkingCSfilter(Gender gender) {
		super();
		this.gender = gender;
	}
	
	public boolean isFiltered(Object object) {
		if(object instanceof Person) {
			Person person = (Person) object;
			return (person.getDgn().equals(gender) && person.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed) && person.getGrossEarningsYearly() >= 0.);
		}
		else throw new IllegalArgumentException("Object passed to GenderEducationWorkingCSfilter must be of type Person!");
	}			
}
