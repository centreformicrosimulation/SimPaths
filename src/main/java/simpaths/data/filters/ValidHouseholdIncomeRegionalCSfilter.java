package simpaths.data.filters;

import simpaths.model.BenefitUnit;
import microsim.statistics.ICollectionFilter;
import simpaths.model.enums.Region;

public class ValidHouseholdIncomeRegionalCSfilter implements ICollectionFilter{
	
	private Region demRgn;
	
	public ValidHouseholdIncomeRegionalCSfilter(Region demRgn) {
		super();
		this.demRgn = demRgn;
		
	}
	
	public boolean isFiltered(Object object) {
		BenefitUnit house = (BenefitUnit) object;
//		return (house.getRegion().equals(region) && house.getAtRiskOfPoverty() != null);
		return (house.getRegion().equals(demRgn)
				&& house.getEquivalisedDisposableIncomeYearly() >= 0.);		//Removes cases where gross earnings are not valid (i.e. are null, or negative)
	}
	
}
