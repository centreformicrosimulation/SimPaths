package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Education;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Indicator;

public class FemalesWithChildrenAgeGroupEducationCSfilter implements ICollectionFilter{
	
	private int ageFrom;
	private int ageTo;
	private Education edu;
	
	public FemalesWithChildrenAgeGroupEducationCSfilter(int ageFrom, int ageTo, Education edu) {
		super();
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
		this.edu = edu;
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return ( person.getDgn().equals(Gender.Female) && 
				(person.getDag() >= ageFrom) && (person.getDag() <= ageTo) &&
				( person.getBenefitUnit().getIndicatorChildren(0,3).equals(Indicator.True) ||
						person.getBenefitUnit().getIndicatorChildren(4,12).equals(Indicator.True) ) &&
				(person.getDeh_c3().equals(edu))
		);
	}
	
}

