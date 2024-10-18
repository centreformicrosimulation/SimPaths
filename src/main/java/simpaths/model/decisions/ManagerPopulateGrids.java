package simpaths.model.decisions;


import java.time.Duration;
import java.time.Instant;

import simpaths.model.SimPathsModel;


/**
 * CLASS TO MANAGE IDENTIFICATION OF LOOK-UP TABLE FOR INTERTEMPORAL OPTIMISATION DECISIONS
 *
 * THE LOOK-UP TABLE IS REFERRED TO THROUGHOUT AS THE 'GRIDS'
 */
public class ManagerPopulateGrids {


    /**
     * ENTRY POINT FOR MANAGER
     * THE MANAGER IS 'run' FROM SimPathsModel, AT THE START OF THE FIRST SIMULATED YEAR
     *
     * @param useSavedGrids boolean that indicates whether grids should be populated from disk
     */
    public static Grids run(SimPathsModel model, boolean useSavedGrids, boolean saveGrids) {

        System.out.println("Populating optimised decision matrix");
        Instant beforeTotal = Instant.now();

        // initiate the decision grids
        Grids grids = new Grids();

        // populate the decision grids
        if (useSavedGrids) {
            ManagerFileGrids.read(grids);
        } else {
            if (DecisionParams.SOLVE_FROM_INTERMEDIATE)
                throw new RuntimeException("cannot solve from intermediate solutions without loading intermediate solutions");
        }
        if (!useSavedGrids || DecisionParams.SOLVE_FROM_INTERMEDIATE) {
            // need to solve for intertemporal optimisations

            model.addRegressionStochasticComponent = false;
            ManagerSolveGrids.run(grids);
            model.addRegressionStochasticComponent = true;
        }

        // save populated grids if necessary
        if (saveGrids || DecisionParams.saveIntermediateSolutions)
            ManagerFileGrids.unformattedWrite(grids);

        // reporting
        Instant afterTotal = Instant.now();
        Duration durationTotal = Duration.between(beforeTotal, afterTotal);
        System.out.println("All operations for identification of optimal decisions completed in " +
                String.format("%.3f", (double)durationTotal.toSeconds()/60.0) + " minutes");

        return grids;
    }
}
