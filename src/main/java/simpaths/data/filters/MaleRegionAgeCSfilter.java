package simpaths.data.filters;

import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Region;
import microsim.statistics.ICollectionFilter;

public class MaleRegionAgeCSfilter implements ICollectionFilter{

	private Region region;
	private int ageFrom;
	private int ageTo;

	public MaleRegionAgeCSfilter(Region region, int ageFrom, int ageTo) {
		super();
		this.region = region;
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return person.getRegion().equals(region) && person.getDgn().equals(Gender.Male) && person.getDag() >= ageFrom && person.getDag() <= ageTo;
	}
	
}
