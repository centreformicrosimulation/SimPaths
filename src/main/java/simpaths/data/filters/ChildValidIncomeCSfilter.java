package simpaths.data.filters;

import simpaths.data.Parameters;
import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;

public class ChildValidIncomeCSfilter implements ICollectionFilter{
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
//		return (person.getAge() < Parameters.AGE_TO_LEAVE_HOME && person.getAtRiskOfPoverty() != null);		//Removes cases where gross earnings are not valid (i.e. are null, or negative)
		return (person.getDag() < Parameters.AGE_TO_BECOME_RESPONSIBLE && person.getBenefitUnit().getEquivalisedDisposableIncomeYearly() >= 0.);		//Removes cases where gross earnings are not valid (i.e. are null, or negative)
	}
	
}
