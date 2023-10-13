package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Region;

public class MaleRegionCSfilter implements ICollectionFilter{
	
	private Region region;
	
	public MaleRegionCSfilter(Region region) {
		super();
		this.region = region;
		
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return person.getRegion().equals(region) && person.getDgn().equals(Gender.Male);
	}
	
}
