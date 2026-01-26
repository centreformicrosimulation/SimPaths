package simpaths.data.filters;

import simpaths.model.Person;
import simpaths.model.enums.Education;
import simpaths.model.enums.Gender;
import microsim.statistics.ICollectionFilter;

public class GenderEducationCSfilter implements ICollectionFilter{

	private Gender demSex;
	private Education education;

	public GenderEducationCSfilter(Gender demSex, Education education) {
		super();
		this.demSex = demSex;
		this.education = education;
	}
	
	public boolean isFiltered(Object object) {
		if(object instanceof Person) {
			Person person = (Person) object;
			return (person.getDemMaleFlag().equals(demSex) && person.getDeh_c3().equals(education) && person.getGrossEarningsYearly() >= 0.);
		}
		else throw new IllegalArgumentException("Object passed to GenderEducationWorkingCSfilter must be of type Person!");
	}			
}
