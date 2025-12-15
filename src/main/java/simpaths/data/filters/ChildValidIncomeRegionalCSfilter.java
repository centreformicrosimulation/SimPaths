package simpaths.data.filters;

import simpaths.data.Parameters;
import microsim.statistics.ICollectionFilter;
import simpaths.model.Person;
import simpaths.model.enums.Region;

public class ChildValidIncomeRegionalCSfilter implements ICollectionFilter{
	
	private Region demRgn;
	
	public ChildValidIncomeRegionalCSfilter(Region demRgn) {
		super();
		this.demRgn = demRgn;
		
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;	
//		return (person.getAge() < Parameters.AGE_TO_LEAVE_HOME && person.getRegion().equals(region) && person.getAtRiskOfPoverty() != null);		//Removes cases where gross earnings are not valid (i.e. are null, or negative)
		return (person.getDemAge() < Parameters.AGE_TO_BECOME_RESPONSIBLE && person.getRegion().equals(demRgn) && person.getBenefitUnit().getEquivalisedDisposableIncomeYearly() >= 0.);		//Removes cases where gross earnings are not valid (i.e. are null, or negative)
	}
	
}
