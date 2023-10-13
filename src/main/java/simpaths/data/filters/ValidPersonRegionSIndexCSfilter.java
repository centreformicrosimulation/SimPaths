package simpaths.data.filters;

import simpaths.model.Person;
import simpaths.model.enums.Region;
import microsim.statistics.ICollectionFilter;

public class ValidPersonRegionSIndexCSfilter implements ICollectionFilter{

	private Region region;

	public ValidPersonRegionSIndexCSfilter(Region region) {
		super();
		this.region = region;
	}

	public boolean isFiltered(Object object) {
			Person person = (Person) object;
//			return (person.getAtRiskOfPoverty() != null);
			return (person.getsIndex() > 0. && person.getsIndex() != Double.NaN && //Removes cases where security index is invalid
					person.getRegion().equals(region));
	}
	
}
