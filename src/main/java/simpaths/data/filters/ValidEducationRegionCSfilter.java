package simpaths.data.filters;

import simpaths.model.BenefitUnit;
import simpaths.model.Person;
import simpaths.model.enums.Les_c4;
import simpaths.model.enums.Region;
import microsim.statistics.ICollectionFilter;

/*
For individuals who have completed education and had education level assigned (i.e. excludes students)

 */

public class ValidEducationRegionCSfilter implements ICollectionFilter{

	private Region demRgn;

	public ValidEducationRegionCSfilter(Region demRgn) {
		super();
		this.demRgn = demRgn;
		
	}
	
	public boolean isFiltered(Object object) {
		if(object instanceof Person) {
			Person person = (Person) object;
			return (person.getRegion().equals(demRgn) && !person.getLes_c4().equals(Les_c4.Student) && person.getDemAge() >= 18 && person.getDeh_c3() != null);
		}
		else if(object instanceof BenefitUnit) {
			BenefitUnit benefitUnit = (BenefitUnit) object;
			return benefitUnit.getRegion().equals(demRgn);
		}
		else throw new IllegalArgumentException("Object passed to RegionCSfilter must be of type Person or BenefitUnit!");
	}
	
}
