package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Education;
import simpaths.model.enums.Region;

public class RegionEducationCSfilter implements ICollectionFilter{
	
	private Region demRgn;
	private Education education;
	
	public RegionEducationCSfilter(Region demRgn, Education education) {
		super();
		this.demRgn = demRgn;
		this.education = education;			
	}
	
	public boolean isFiltered(Object object) {
		if(object instanceof Person) {
			Person person = (Person) object;
			if (person.getEduHighestC4()==null) {
				return false;
			} else {
				return (person.getRegion().equals(demRgn) && person.getEduHighestC4().equals(education));
			}
		}
		else throw new IllegalArgumentException("Object passed to RegionEducationCSfilter must be of type Person!");
	}			
}
