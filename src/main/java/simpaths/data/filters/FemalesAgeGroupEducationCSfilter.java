package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Education;
import simpaths.model.enums.Gender;

public class FemalesAgeGroupEducationCSfilter implements ICollectionFilter{
	
	private int ageFrom;
	private int ageTo;
	private Education edu;
	
	public FemalesAgeGroupEducationCSfilter(int ageFrom, int ageTo, Education edu) {
		super();
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
		this.edu = edu;
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		if( person.getDeh_c3() == null ) return false;		//Better just to check on Education being null, rather than assuming anything about the Students.  In future models, it may be possible to go back to education after already working.
//		if(person.getActivity_status().equals(Les_c4.Student)) {		//Need to check they are not still students, otherwise education level is not defined
//			return false;
//		}
		else return ( person.getDgn().equals(Gender.Female) && 
				(person.getDag() >= ageFrom) && (person.getDag() <= ageTo) && 
				( person.getDeh_c3().equals(edu) )
				);
	}
	
}

