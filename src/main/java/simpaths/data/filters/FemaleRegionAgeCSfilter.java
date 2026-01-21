package simpaths.data.filters;

import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Region;
import microsim.statistics.ICollectionFilter;

public class FemaleRegionAgeCSfilter implements ICollectionFilter{

	private Region demRgn;
	private int ageFrom;
	private int ageTo;

	public FemaleRegionAgeCSfilter(Region demRgn, int ageFrom, int ageTo) {
		super();
		this.demRgn = demRgn;
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return person.getRegion().equals(demRgn) && person.getDemMaleFlag().equals(Gender.Female) && person.getDemAge() >= ageFrom && person.getDemAge() <= ageTo;
	}
	
}
