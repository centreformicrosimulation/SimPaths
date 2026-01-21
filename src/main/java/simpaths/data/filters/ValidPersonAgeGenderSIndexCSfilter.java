package simpaths.data.filters;

import simpaths.model.Person;
import simpaths.model.enums.Gender;
import microsim.statistics.ICollectionFilter;

public class ValidPersonAgeGenderSIndexCSfilter implements ICollectionFilter{

	private int ageFrom;
	private int ageTo;
	private Gender demSex;

	public ValidPersonAgeGenderSIndexCSfilter(int ageFrom, int ageTo, Gender demSex) {
		super();
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
		this.demSex = demSex;
	}

	public boolean isFiltered(Object object) {
			Person person = (Person) object;
//			return (person.getAtRiskOfPoverty() != null);
			return (person.getsIndex() > 0. && person.getsIndex() != Double.NaN && //Removes cases where security index is invalid
					(person.getDemAge() >= ageFrom) && (person.getDemAge() <= ageTo) &&
					person.getDemMaleFlag().equals(demSex));
	}
	
}
