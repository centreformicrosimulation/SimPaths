package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;

public class AgeGroupCSfilter implements ICollectionFilter{
	
	private final int ageFrom;
	private final int ageTo;
	
	public AgeGroupCSfilter(int ageFrom, int ageTo) {
		super();
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return ( (person.getDag() >= ageFrom) && (person.getDag() <= ageTo) );
	}
	
	public int getAgeFrom() {
		return ageFrom;
	}
	
	public int getAgeTo() {
		return ageTo;
	}
	
}
