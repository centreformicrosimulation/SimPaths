package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;

public class FemaleAgeGroupCSfilter implements ICollectionFilter{
	
	private int ageFrom;
	private int ageTo;
	
	public FemaleAgeGroupCSfilter(int ageFrom, int ageTo) {
		super();
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return ( person.getDemMaleFlag().equals(Gender.Female) && (person.getDemAge() >= ageFrom) && (person.getDemAge() <= ageTo) );
	}
	
}
