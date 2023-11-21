package simpaths.model.decisions;

import simpaths.data.Parameters;

public class DecisionTests {

    public static void compareGrids() {

        // load in grids for comparison
        String name1 = "laptop serial";
        DecisionParams.setGridsInputDirectory(name1);
        Grids grids1 = new Grids();
        ManagerFileGrids.read(grids1);

        String name2 = "parallel";
        DecisionParams.setGridsInputDirectory(name2);
        Grids grids2 = new Grids();
        ManagerFileGrids.read(grids2);

        // loop through grids to find differences
        for (int aa=grids1.scale.simLifeSpan - 1; aa>=0; aa--) {

            // set age specific working variables
            int innerDimension = (int)grids1.scale.gridDimensions[aa][0];
            int outerDimension = (int)grids1.scale.gridDimensions[aa][1];
            long ii0=-9;
            for (int iiOuter=0; iiOuter<outerDimension; iiOuter++) {

                // identify current state combination for outer states
                int ageYears = aa + Parameters.AGE_TO_BECOME_RESPONSIBLE;
                States outerStates = new States(grids1.scale, ageYears);
                outerStates.populateOuterGridStates(iiOuter);
                boolean loopConsider = outerStates.checkOuterStateCombination();
                if (loopConsider) {
                    for (int iiInner = 0; iiInner < innerDimension; iiInner++) {
                        // identify current state combination and copy expectations
                        States currentStates = new States(outerStates);
                        currentStates.populateInnerGridStates(iiInner);
                        boolean stateConsider = currentStates.checkStateCombination();
                        if (stateConsider) {

                            long indexHere = currentStates.returnGridIndex();
                            if (ii0>=0) {
                                if (indexHere!=ii0+1) {
                                    indexHere = currentStates.returnGridIndex();
                                }
                            }
                            double val1 = grids1.valueFunction.get(indexHere);
                            double val2 = grids2.valueFunction.get(indexHere);
                            if (Math.abs(val1 - val2) > 1.0E-3 * Math.abs(val1) ) {
                                int iii = 1;
                            }
                            ii0 = indexHere;
                        } else {
                            ii0 = -9;
                        }
                    }
                } else {
                    ii0 = -9;
                }
            }
        }
    }
}
