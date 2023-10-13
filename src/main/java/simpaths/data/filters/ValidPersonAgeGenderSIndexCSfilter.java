package simpaths.data.filters;

import simpaths.model.Person;
import simpaths.model.enums.Gender;
import microsim.statistics.ICollectionFilter;

public class ValidPersonAgeGenderSIndexCSfilter implements ICollectionFilter{

	private int ageFrom;
	private int ageTo;
	private Gender gender;

	public ValidPersonAgeGenderSIndexCSfilter(int ageFrom, int ageTo, Gender gender) {
		super();
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
		this.gender = gender;
	}

	public boolean isFiltered(Object object) {
			Person person = (Person) object;
//			return (person.getAtRiskOfPoverty() != null);
			return (person.getsIndex() > 0. && person.getsIndex() != Double.NaN && //Removes cases where security index is invalid
					(person.getDag() >= ageFrom) && (person.getDag() <= ageTo) &&
					person.getDgn().equals(gender));
	}
	
}
