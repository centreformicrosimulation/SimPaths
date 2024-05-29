package simpaths.model.decisions;

import simpaths.data.Parameters;
import simpaths.model.taxes.Matches;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;


/**
 *
 * CLASS TO MANAGE EVALUATION OF SOLUTIONS TO POPULATE INTERTEMPORAL OPTIMISATION GRIDS
 *
 * THE SOLUTION PROCEDURE RUNS THROUGH ALL STATE COMBINATIONS DESCRIBED BY THE GRIDS
 * VIA FOUR NESTED LOOPS
 *
 * THE FIRST (OUTER-MOST) LOOP CONSIDERS SLICES OF THE GRIDS DISTINGUISHED BY AGE
 * THIS IS MOTIVATED BY THE USE OF BACKWARD INDUCTION TECHNIQUES BY THE SOLUTION METHOD
 *
 * THE SECOND LOOP CONSIDERS COMBINATIONS OF CHARACTERISTICS, FOR WHICH EXPECTATIONS
 * ARE EXOGENOUS OF CONTROL VARIABLES
 * THIS ALLOWS EVALUATIONS FOR EXPECTATIONS OVER THESE VARIABLES TO BE DONE ONCE FOR
 * ALL COMBINATIONS OF STATES CONSIDERED IN THE INNER TWO LOOPS
 *      STATES IN THIS LOOP ARE IDENTIFIED BY AXES[aa][ii][4] = 0 OR 0.5
 *
 * THE THIRD LOOP IS PARALLISED TO TAKE FULL ADVANTAGE OF COMPUTING RESOURCES
 *
 * THE FOURTH (INNER-MOST) LOOP IS SEPARATED FROM THE THIRD LOOP ONLY TO ECONOMISE
 * THE OVER-HEAD ASSOCIATED WITH PARALLELISATIONS
 *
 */
public class ManagerSolveGrids {


    /**
     * ENTRY POINT FOR MANAGER
     * @param grids refers to the look-up table that stores IO solutions (the 'grids')
     *
     * THE MANAGER IS 'run' FROM ManagerPopulateGrids
     */
    public static void run(Grids grids) {


        System.out.println("Obtaining numerical solutions for optimised decisions");

        // solve grids using backward-induction, working from the last potential period in life
        Instant beforeTotal = null, afterTotal = null;
        int solveFromAgeIndex;
        if (DecisionParams.SOLVE_FROM_INTERMEDIATE)
            solveFromAgeIndex = DecisionParams.SOLVE_FROM_AGE - Parameters.AGE_TO_BECOME_RESPONSIBLE;
        else
            solveFromAgeIndex = grids.scale.simLifeSpan - 1;
        for (int aa=solveFromAgeIndex; aa>=0; aa--) {

            Instant before = Instant.now();
            if (aa==solveFromAgeIndex) beforeTotal = before;

            // set age specific working variables
            int innerDimension = (int)grids.scale.gridDimensions[aa][0];
            int outerDimension = (int)grids.scale.gridDimensions[aa][1];
            int ageYears = aa + Parameters.AGE_TO_BECOME_RESPONSIBLE;
            Matches imperfectMatches = new Matches();
            List<Matches> imperfectMatchStore = newImperfectMatchStore((int)grids.scale.gridDimensions[aa][2]);

            // loop over outer dimensions, for which expectations are independent of IO decisions (controls)
            for (int iiOuter=0; iiOuter<outerDimension; iiOuter++) {

                // identify current state combination for outer states
                States outerStates = new States(grids.scale, ageYears);
                outerStates.populateOuterGridStates(iiOuter);
                boolean loopConsider = outerStates.checkOuterStateCombination();
                if (loopConsider) {

                    // define expectations for outer states not affected by agent decisions
                    Expectations outerExpectations = new Expectations(outerStates);

                    // loop over inner dimensions
                    if (DecisionParams.PARALLELISE_SOLUTIONS) {
                        IntStream.range(0, innerDimension).parallel().forEach(iiInner -> {
                            // identify current state combination and copy expectations
                            States currentStates = new States(outerStates);
                            currentStates.populateInnerGridStates(iiInner);
                            boolean stateConsider = currentStates.checkStateCombination();
                            if (stateConsider) {
                                ManagerSolveState.run(grids, currentStates, outerExpectations, imperfectMatchStore);
                            }
                        });
                    } else {
                        for (int iiInner=0; iiInner<innerDimension; iiInner++) {
                            // identify current state combination and copy expectations
                            States currentStates = new States(outerStates);
                            currentStates.populateInnerGridStates(iiInner);
                            boolean stateConsider = currentStates.checkStateCombination();
                            if (stateConsider) {
                                ManagerSolveState.run(grids, currentStates, outerExpectations, imperfectMatchStore);
                            }
                        }
                    }
                }
            }
            if (DecisionParams.saveImperfectTaxDbMatches) {
                for (Matches mm : imperfectMatchStore) {
                    if (!mm.isEmpty()) {
                        imperfectMatches.addSet(mm.getSet());
                    }
                }
                if (!imperfectMatches.isEmpty()) {
                    imperfectMatches.write(DecisionParams.gridsOutputDirectory, "poor_taxmatch_age_" + ageYears + ".csv");
                }
            }
            if (DecisionParams.saveIntermediateSolutions && (ageYears<80) && ((ageYears % 5)==0))
                ManagerFileGrids.unformattedWrite(grids, true);
            if (DecisionParams.saveGridSlicesToCSV)
                ManagerFileGrids.formattedWrite(grids, aa);
            Instant after = Instant.now();
            if (aa == 0) afterTotal = after;
            Duration duration = Duration.between(before, after);
            System.out.println("Calculations for age " + ageYears + " completed in " + String.format("%.3f", (double)duration.toMillis()/1000.0) + " seconds");
        }
        if (beforeTotal != null && afterTotal != null) {

            Duration durationTotal = Duration.between(beforeTotal, afterTotal);
            System.out.println("Calculations for optimal decisions completed in " + String.format("%.3f", (double)durationTotal.toSeconds()/60.0) + " minutes");
        }
    }

    private static List<Matches> newImperfectMatchStore(int size) {
        List<Matches> list = new ArrayList<>();
        for (int ii=0; ii<size; ii++) {
            list.add(new Matches());
        }
        return list;
    }
}
