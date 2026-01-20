package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Region;

public class MaleRegionCSfilter implements ICollectionFilter{
	
	private Region demRgn;
	
	public MaleRegionCSfilter(Region demRgn) {
		super();
		this.demRgn = demRgn;
		
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return person.getRegion().equals(demRgn) && person.getDemMaleFlag().equals(Gender.Male);
	}
	
}
