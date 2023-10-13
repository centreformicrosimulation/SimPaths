package simpaths.data.filters;

import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;

public class ValidPersonEarningsCSfilter implements ICollectionFilter{	
	
	public boolean isFiltered(Object object) {
			Person person = (Person) object;
//			return (person.getAtRiskOfPoverty() != null);
			return (person.getGrossEarningsYearly() >= 0.);		//Removes cases where gross earnings are not valid (i.e. are null, or negative)
	}
	
}
