package simpaths.data.filters;

import simpaths.model.Person;
import simpaths.model.enums.Les_c4;
import microsim.statistics.ICollectionFilter;

/*
This filter is like an age group filter, but only selects individuals who are not Students

 */

public class ValidEducationAgeGroupCSfilter implements ICollectionFilter{

	private final int ageFrom;
	private final int ageTo;

	public ValidEducationAgeGroupCSfilter(int ageFrom, int ageTo) {
		super();
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return ( (person.getDag() >= ageFrom) && (person.getDag() <= ageTo) && !person.getLes_c4().equals(Les_c4.Student));
	}
	
	public int getAgeFrom() {
		return ageFrom;
	}
	
	public int getAgeTo() {
		return ageTo;
	}
	
}
