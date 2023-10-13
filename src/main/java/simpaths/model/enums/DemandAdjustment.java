package simpaths.model.enums;

/**
 * The Demand Adjustment refers to the update of the initial labour demand at each time-step, which is derived from the equilibrium
 * value of labour demand at the previous time-step with an adjustment to take into account such factors as population or income growth. 
 *
 */
public enum DemandAdjustment {
	
	PopulationGrowth,		//Demand is proportional to population growth between time-steps
	IncomeGrowth,			//Demand is proportional to aggregate disposable income (after tax/benefits) growth between time-steps
	
}
