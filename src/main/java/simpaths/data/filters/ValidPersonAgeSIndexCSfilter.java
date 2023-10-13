package simpaths.data.filters;

import simpaths.model.Person;
import microsim.statistics.ICollectionFilter;

public class ValidPersonAgeSIndexCSfilter implements ICollectionFilter{

	private int ageFrom;
	private int ageTo;

	public ValidPersonAgeSIndexCSfilter(int ageFrom, int ageTo) {
		super();
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
	}

	public boolean isFiltered(Object object) {
			Person person = (Person) object;
//			return (person.getAtRiskOfPoverty() != null);
			return (person.getsIndex() > 0. && person.getsIndex() != Double.NaN && //Removes cases where security index is invalid
					(person.getDag() >= ageFrom) && (person.getDag() <= ageTo));
	}
	
}
