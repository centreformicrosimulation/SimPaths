package simpaths.data.filters;

import simpaths.model.Person;
import microsim.statistics.ICollectionFilter;

public class ValidPersonSIndexCSfilter implements ICollectionFilter{

	public boolean isFiltered(Object object) {
			Person person = (Person) object;
//			return (person.getAtRiskOfPoverty() != null);
			return (person.getsIndex() > 0. && person.getsIndex() != Double.NaN ); //Removes cases where security index is invalid
	}
	
}
