package simpaths.model.enums;

public enum HouseholdStatus {		//XXX: Currently there is no state for otherMembers in the household.  Should we have Other in here?
	Parents,			//Living with parents - with current assumptions, this basically acts as an indicator for being in the children set of a household
	Single,				//Living alone
	Couple,				//Living with partner (cohabiting)
}
