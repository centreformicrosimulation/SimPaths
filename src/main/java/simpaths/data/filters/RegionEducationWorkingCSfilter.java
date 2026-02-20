package simpaths.data.filters;

import simpaths.model.enums.Les_c4;
import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Education;
import simpaths.model.enums.Region;

public class RegionEducationWorkingCSfilter implements ICollectionFilter{
	
	private Region demRgn;
	private Education education;
	
	public RegionEducationWorkingCSfilter(Region demRgn, Education education) {
		super();
		this.demRgn = demRgn;
		this.education = education;			
	}
	
	public boolean isFiltered(Object object) {
		if(object instanceof Person) {
			Person person = (Person) object;
			return (person.getRegion().equals(demRgn) && person.getDeh_c4().equals(education) && person.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed) && person.getGrossEarningsYearly() >= 0.);
		}
		else throw new IllegalArgumentException("Object passed to RegionEducationWorkingCSfilter must be of type Person!");
	}			
}
