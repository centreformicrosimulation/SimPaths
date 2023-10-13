package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;

public class GenderCSfilter  implements ICollectionFilter{
	private final Gender gender;
	
	public GenderCSfilter(Gender gender) {
		super();
		this.gender = gender;
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return (person.getDgn().equals(gender));
	}
	
	public Gender getGender() {
		return gender;
	}
	
}
