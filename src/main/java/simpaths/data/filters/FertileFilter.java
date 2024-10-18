package simpaths.data.filters;

import org.apache.commons.collections4.Predicate;

import simpaths.data.Parameters;
import simpaths.model.Person;
import simpaths.model.enums.Gender;
import simpaths.model.enums.Region;

public class FertileFilter<T extends Person> implements Predicate<T> {
	
	private Region region = null;

	public FertileFilter() {}
	public FertileFilter(Region region) {
		super();
		this.region = region;
	}

	@Override
	public boolean evaluate(T agent) {
		
		int age = agent.getDag();
		boolean fertile = false;
		if ( (agent.getDgn().equals(Gender.Female)) &&
				( region==null || agent.getRegion().equals(region)) &&
//				( !agent.getLes_c3().equals(Les_c4.Student) || agent.isToLeaveSchool() ) &&	//2 processes for fertility, for those in and out of education - specified in alignment
				( age >= Parameters.MIN_AGE_MATERNITY ) &&
				( age <= Parameters.MAX_AGE_MATERNITY ) &&
				( ( agent.getPartner() != null)	|| (Parameters.FLAG_SINGLE_MOTHERS) ) )
			fertile = true;
		return fertile;
	}
}
