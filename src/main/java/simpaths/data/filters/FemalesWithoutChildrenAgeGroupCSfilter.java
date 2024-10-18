package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Indicator;

public class FemalesWithoutChildrenAgeGroupCSfilter implements ICollectionFilter{
	
	private int ageFrom;
	private int ageTo;
	
	public FemalesWithoutChildrenAgeGroupCSfilter(int ageFrom, int ageTo) {
		super();
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return ( person.getDgn().equals(Gender.Female) && 
				(person.getDag() >= ageFrom) && (person.getDag() <= ageTo) &&
				!( person.getBenefitUnit().getIndicatorChildren(0,3).equals(Indicator.True) ||
						person.getBenefitUnit().getIndicatorChildren(4,12).equals(Indicator.True) ||
						person.getBenefitUnit().getIndicatorChildren(13,17).equals(Indicator.True) )
		);
	}
}

