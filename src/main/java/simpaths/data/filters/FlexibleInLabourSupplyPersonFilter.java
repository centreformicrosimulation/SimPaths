package simpaths.data.filters;

import simpaths.model.Person;
import simpaths.model.enums.Indicator;
import simpaths.model.enums.Les_c4;
import org.apache.commons.collections4.Predicate;

public class FlexibleInLabourSupplyPersonFilter<T extends Person> implements Predicate<T> {


	public FlexibleInLabourSupplyPersonFilter() {
		super();
	}

	@Override
	public boolean evaluate(T person) {

		
		return (person.getDag() >= 18 && person.getDag() <= 64 &&
				person.getLes_c4() != Les_c4.Student && person.getLes_c4() != Les_c4.Retired &&
				person.getDlltsd() != Indicator.True);
	}


}
