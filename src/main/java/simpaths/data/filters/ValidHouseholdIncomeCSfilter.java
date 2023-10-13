package simpaths.data.filters;

import simpaths.model.BenefitUnit;
import microsim.statistics.ICollectionFilter;

public class ValidHouseholdIncomeCSfilter implements ICollectionFilter{	
	
	public boolean isFiltered(Object object) {
			BenefitUnit house = (BenefitUnit) object;
//			return (house.getAtRiskOfPoverty() != null);
			return (house.getEquivalisedDisposableIncomeYearly() >= 0. && house.getEquivalisedDisposableIncomeYearly() < 1000000.);		//Removes cases where gross earnings are not valid (i.e. are null, or negative)
	}
	
}
