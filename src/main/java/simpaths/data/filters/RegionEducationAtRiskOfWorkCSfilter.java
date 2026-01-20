package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Education;
import simpaths.model.enums.Region;

public class RegionEducationAtRiskOfWorkCSfilter implements ICollectionFilter{
	
	private Region demRgn;
	private Education education;
	
	public RegionEducationAtRiskOfWorkCSfilter(Region demRgn, Education education) {
		super();
		this.demRgn = demRgn;
		this.education = education;			
	}
	
	public boolean isFiltered(Object object) {
		if(object instanceof Person) {
			Person person = (Person) object;
			return (person.getRegion().equals(demRgn) && person.getDeh_c3().equals(education) && person.atRiskOfWork());
		}
		else throw new IllegalArgumentException("Object passed to RegionEducationCSfilter must be of type Person!");
	}			
}
