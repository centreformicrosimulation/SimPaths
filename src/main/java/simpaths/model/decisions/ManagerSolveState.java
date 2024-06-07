package simpaths.model.decisions;


import simpaths.model.taxes.Matches;

import java.util.List;

/**
 * CLASS TO MANAGE EVALUATION OF NUMERICAL SOLUTIONS FOR SPECIFIC STATE COMBINATION
 *
 * MANAGER EVALUATES THE FEASIBLE DISCRETE CONTROLS, AND OBTAINS SOLUTIONS FOR ALL
 * CONTINUOUS CONTROLS FOR EACH FEASIBLE COMBINATION OF DISCRETE CONTROLS. THE SOLUTION
 * FOR THE STATE COMBINATION IS THEN TAKEN AS THE SOLUTION TO THE COMBINATION OF DISCRETE
 * CONTROLS WITH THE HIGHEST VALUE FUNCTION
 */
public class ManagerSolveState {


    /**
     * ENTRY POINT FOR MANAGER
     * @param grids refers to the look-up table that stores IO solutions (the 'grids')
     * @param states the state combination for consideration
     *
     * THE MANAGER IS 'run' FROM ManagerSolveGrids
     */
    public static void run(Grids grids, States states, Expectations outerExpectations, List<Matches> imperfectMatchStore) {

        // instantiate expectations object with data for all states that are invariant to agent expectations
        Expectations invariantExpectations = new Expectations(states, outerExpectations);

        // identify discrete control options
        double emp1Start = 0;
        double emp1End = 0;
        double emp1Step = 1;
        double emp2Start = 0;
        double emp2End = 0;
        double emp2Step = 1;
        if (states.ageYears <= DecisionParams.maxAgeFlexibleLabourSupply) {

            // principal earner
            if (DecisionParams.FLAG_IO_EMPLOYMENT1) {
                emp1End = 1;
                if (DecisionParams.optionsEmployment1 > 1) {
                    emp1Step = 1 / (double)(DecisionParams.optionsEmployment1 - 1);
                }
            } else if (DecisionParams.flagLowWageOffer1) {
                emp1End = 1;
            } else {
                emp1Start = 1;
                emp1End = 1;
            }
            if (states.getCohabitation()) {
                // secondary earner

                if (DecisionParams.FLAG_IO_EMPLOYMENT2) {
                    emp2End = 1;
                    if (DecisionParams.optionsEmployment2 > 1) {
                        emp2Step = 1 / (double)(DecisionParams.optionsEmployment2 - 1);
                    }
                } else if (DecisionParams.FLAG_WAGE_OFFER2) {
                    emp2End = 1;
                } else {
                    emp2Start = 1;
                    emp2End = 1;
                }
            }
        }

        // instantiate storage for solutions to all discrete control options
        UtilityMaximisation solutionMax = null;
        UtilityMaximisation solutionMaxEmp1 = null;
        UtilityMaximisation solutionMaxEmp2 = null;
        Matches localImperfectMatches = new Matches();
        for (double emp1Pr=emp1Start; emp1Pr<=(emp1End+1.0E-7); emp1Pr+=emp1Step) {
            for (double emp2Pr=emp2Start; emp2Pr<=(emp2End+1.0E-7); emp2Pr+=emp2Step) {

                boolean loopConsider = checkDecisionFeasible(states, emp1Pr, emp2Pr);
                if (loopConsider) {

                    // instantiate local expectations, populated with expectations for states invariant to agent decisions
                    Expectations expectations = new Expectations(invariantExpectations);

                    // evaluate solution for current control combination
                    UtilityMaximisation solutionHere = new UtilityMaximisation(grids.valueFunction, states, expectations, emp1Pr, emp2Pr);

                    // check for imperfect matches
                    if ( DecisionParams.saveImperfectTaxDbMatches && !expectations.imperfectMatches.isEmpty()) {
                        localImperfectMatches.addSet(expectations.imperfectMatches.getSet());
                    }

                    // check for wage offer solutions for both principal and secondary earner
                    if (emp1End > emp1Start && DecisionParams.flagLowWageOffer1 &&
                            emp2End > emp2Start && DecisionParams.FLAG_WAGE_OFFER2 &&
                            emp1Pr < 1.0E-5 && emp2Pr < 1.0E-5) {
                        States targetStates = new States(states);
                        targetStates.setWageOffer1(0);
                        targetStates.setWageOffer2(0);
                        grids.populate(targetStates, solutionHere);
                    }

                    // check wage offer solutions for principal earner
                    if (emp1End > emp1Start && DecisionParams.flagLowWageOffer1 && emp1Pr < 1.0E-5) {
                        if (solutionMaxEmp1==null) {
                            solutionMaxEmp1 = solutionHere;
                        } else {
                            if (solutionMaxEmp1.optimisedUtility < solutionHere.optimisedUtility) {
                                solutionMaxEmp1 = solutionHere;
                            }
                        }
                    }

                    // check wage offer solutions for secondary earner
                    if (emp2End > emp2Start && DecisionParams.FLAG_WAGE_OFFER2 && emp2Pr < 1.0E-5) {
                        if (solutionMaxEmp2==null) {
                            solutionMaxEmp2 = solutionHere;
                        } else {
                            if (solutionMaxEmp2.optimisedUtility < solutionHere.optimisedUtility) {
                                solutionMaxEmp2 = solutionHere;
                            }
                        }
                    }

                    // track state optimum
                    if (solutionMax==null) {
                        solutionMax = solutionHere;
                    } else {
                        if (solutionMax.optimisedUtility < solutionHere.optimisedUtility) {
                            solutionMax = solutionHere;
                        }
                    }
                }
            }
        }

        // save wage offer solutions for principal earner
        if (emp1End > emp1Start && DecisionParams.flagLowWageOffer1) {
            States targetStates = new States(states);
            targetStates.setWageOffer1(0);
            grids.populate(targetStates, solutionMaxEmp1);
        }

        // save wage offer solutions for secondary earner
        if (emp2End > emp2Start && DecisionParams.FLAG_WAGE_OFFER2) {
            States targetStates = new States(states);
            targetStates.setWageOffer2(0);
            grids.populate(targetStates, solutionMaxEmp2);
        }

        // save state optimum
        grids.populate(states, solutionMax);

        if ( DecisionParams.saveImperfectTaxDbMatches && !localImperfectMatches.isEmpty()) {
            int ageSpecificIndex = (int)states.returnAgeSpecificIndex();
            imperfectMatchStore.set(ageSpecificIndex, localImperfectMatches);
        }
    }

    private static boolean checkDecisionFeasible(States states, double emp1Pr, double emp2Pr) {

        if (emp1Pr>1.0E-5 && !states.getPrincipalEligibleForWork())
            return false;
        return true;
    }
}
