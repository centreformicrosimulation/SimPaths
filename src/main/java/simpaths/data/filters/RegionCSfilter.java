package simpaths.data.filters;

import simpaths.model.BenefitUnit;
import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Region;

public class RegionCSfilter implements ICollectionFilter{
	
	private Region region;
	
	public RegionCSfilter(Region region) {
		super();
		this.region = region;
		
	}
	
	public boolean isFiltered(Object object) {
		if(object instanceof Person) {
			Person person = (Person) object;
			return person.getRegion().equals(region);
		}
		else if(object instanceof BenefitUnit) {
			BenefitUnit benefitUnit = (BenefitUnit) object;
			return benefitUnit.getRegion().equals(region);
		}
		else throw new IllegalArgumentException("Object passed to RegionCSfilter must be of type Person or BenefitUnit!");
	}
	
}
