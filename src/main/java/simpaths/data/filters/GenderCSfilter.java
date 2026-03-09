package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;

public class GenderCSfilter  implements ICollectionFilter{
	private final Gender demSex;
	
	public GenderCSfilter(Gender demSex) {
		super();
		this.demSex = demSex;
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return (person.getDemMaleFlag().equals(demSex));
	}
	
	public Gender getGender() {
		return demSex;
	}
	
}
