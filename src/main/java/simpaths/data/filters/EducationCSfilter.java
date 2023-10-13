package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Education;

public class EducationCSfilter implements ICollectionFilter{
	
	private Education education;
	
	public EducationCSfilter(Education edu) {
		super();
		this.education = edu;
		
	}
	
	public boolean isFiltered(Object object) {
			Person person = (Person) object;
			return person.getDeh_c3().equals(education);
			
	}
	
}
