package simpaths.model.decisions;


import simpaths.data.Parameters;

/**
 *
 * CLASS TO DEFINE GRID AXES
 *
 */
public class GridScale {


    /**
     * ATTRIBUTES
     */
    int simLifeSpan;           // maximum number of periods to make intertemporal optimised decisions
    int numberOfStates;        // number of state variables
    long[][] gridDimensions;   // vector storing summary references for grid dimensions - see constructor for definition
    double[][][] axes;         // vector storing detailed description of grid axes - see constructor for definition


    /**
     * CONSTRUCTOR
     */
    public GridScale() {

        // constructor variables
        int dimIndex, ageHh;
        double value;

        // sim_life_span
        simLifeSpan = DecisionParams.maxAge - Parameters.AGE_TO_BECOME_RESPONSIBLE + 1;

        // number_of_states
        numberOfStates = 1;                                       // liquid wealth
        numberOfStates++;                                         // full-time wage potential
        if (DecisionParams.flagPrivatePension) numberOfStates++;      // private pension income
        numberOfStates++;                                         // birth year
        if (DecisionParams.flagLowWageOffer1) numberOfStates++;   // wage offer of principal earner (1 = receive wage offer)
        if (DecisionParams.FLAG_WAGE_OFFER2) numberOfStates++;    // wage offer of secondary earner (1 = receive wage offer)
        if (DecisionParams.flagRetirement) numberOfStates++;      // retirement status
        if (DecisionParams.flagHealth) numberOfStates++;          // health status
        if (DecisionParams.flagDisability) numberOfStates++;      // disability status
        if (Parameters.flagSocialCare) numberOfStates++;          // social care receipt
        if (Parameters.flagSocialCare) numberOfStates++;          // social care provision
        if (DecisionParams.flagRegion) numberOfStates++;          // region
        if (DecisionParams.flagEducation) numberOfStates++;       // student
        if (DecisionParams.flagEducation) numberOfStates++;       // education
        numberOfStates += DecisionParams.NUMBER_BIRTH_AGES;       // dependent children
        numberOfStates++;                                         // cohabitation (1 = cohabiting)
        numberOfStates++;                                         // gender (1 = female)
        axes = new double[simLifeSpan][numberOfStates][5];
        gridDimensions = new long[simLifeSpan][7];

        /*
         * POPULATE AXES
         *
         * AXES IS AN ARRAY OF DESCRIPTIVE PARAMETERS FOR THE AXES OF THE DECISION GRID
         *      AXES[aa][ii][0] - denotes no of points in dimension ii at age aa
         *      AXES[aa][ii][1] - denotes minimum in dimension ii at age aa
         *      AXES[aa][ii][2] - denotes maximum in dimension ii at age aa
         *      AXES[aa][ii][3] - indicator for continuous state variables (discrete=0, ambiguous=0.5, continuous=1)
         *                        ambiguous is treated as discrete for solutions and continuous for population projections
         *      AXES[aa][ii][4] - indicator for whether state set within inner loop
         *                          axes[aa][ii][4] = 0 indicates the outer-most loop for age index aa
         *                          axes[aa][ii][4] = 1 indicates the inner-most loop for age index aa
         *
         * THE ORDER OF AXES FOR THE DECISION GRID IS AS FOLLOWS (from lowest to highest index):
         *      liquid wealth (w)
         *      full-time wage potential (y)
         *      ... 1 ...
         *      birth year (b)
         *      wage offer (wo)
         *      ... 2 ...
         *      retirement status (o)
         *      health status (h)
         *      disability status (d)
         *      social care (sc)
         *      region (r) based on (12) Government Office Regions in UK
         *      student status (s)
         *      highest education attainment (e) (integers for education categories, with higher number reflecting higher education)
         *      dependent children (k) (number by birth year)
         *      cohabitation (c) (0=single, 1=couple)
         *      gender (g) of reference person (0=male, 1=female)
         *      age (a) in years
         *
         * THE ORDER OF CHARACTERISTICS FOLLOWS A SET OF RULES:
         *      1) continuous variables at top, discrete variables at bottom
         *          a) this is needed to facilitate application of interpolation routines
         *      2) new continuous variables should be inserted immediately above "... 1 ..."
         *      3) new discrete variables should be inserted immediately below "... 2 ..."
         *
         * THE INTERPOLATION ROUTINES ASSUME THAT CONTINUOUS STATES ARE GROUPED CONTIGUOUSLY
         * THE IO SOLUTION ROUTINES ASSUME THAT INNER CHARACTERISTICS ARE ALSO GROUPED CONTIGUOUSLY
         *
         * EXOGENOUS STATES GROUPED AT THE BOTTOM FACILITATES EFFICIENT EVALUATION OF EXPECTATIONS
         * ABOUT PARALLELISED CODE LOOPS IN ManagerSolveGrids
         *
         * THE ORDERING DESCRIBED ABOVE FACILITATES USE OF FLAGS AND MODULAR CODE BLOCS TO TURN STATES ON AND OFF
         *      1) continuous variables are indexed from the top
         *          a) except birth year, which is a discrete variable in the decision solutions and a continuous variable when projecting the population
         *              i) wage offers are "special" discrete variables as they do not require separate solutions to be evaluated
         *      2) discrete variables are indexed from the bottom
         */
        for (int aa = 0; aa < simLifeSpan; aa++) {

            ageHh = Parameters.AGE_TO_BECOME_RESPONSIBLE + aa;
            dimIndex = 0;

            // liquid wealth
            if (ageHh <= DecisionParams.maxAgeFlexibleLabourSupply) {
                axes[aa][dimIndex][0] = DecisionParams.PTS_LIQUID_WEALTH_WKG;
            } else {
                axes[aa][dimIndex][0] = DecisionParams.PTS_LIQUID_WEALTH_RTD;
            }
            axes[aa][dimIndex][1] = Math.log(DecisionParams.getMinWealthByAge(ageHh) + DecisionParams.C_LIQUID_WEALTH);
            axes[aa][dimIndex][2] = Math.log(DecisionParams.getMaxWealthByAge(ageHh) + DecisionParams.C_LIQUID_WEALTH);
            axes[aa][dimIndex][3] = 1;
            axes[aa][dimIndex][4] = 1;
            dimIndex++;

            // full-time wage potential
            if (ageHh <= DecisionParams.maxAgeFlexibleLabourSupply) {
                axes[aa][dimIndex][0] = DecisionParams.PTS_WAGE_POTENTIAL;
                axes[aa][dimIndex][1] = Math.log(DecisionParams.MIN_WAGE_PHOUR + DecisionParams.C_WAGE_POTENTIAL);
                axes[aa][dimIndex][2] = Math.log(DecisionParams.MAX_WAGE_PHOUR + DecisionParams.C_WAGE_POTENTIAL);
                axes[aa][dimIndex][3] = 1;
                axes[aa][dimIndex][4] = 1;
                dimIndex++;
            }

            // pension income
            if (DecisionParams.flagPrivatePension && ageHh > DecisionParams.minAgeToRetire) {
                axes[aa][dimIndex][0] = DecisionParams.PTS_PENSION;
                axes[aa][dimIndex][1] = Math.log(DecisionParams.C_PENSION);
                axes[aa][dimIndex][2] = Math.log(DecisionParams.maxPensionPYear + DecisionParams.C_PENSION);
                axes[aa][dimIndex][3] = 1;
                axes[aa][dimIndex][4] = 1;
                dimIndex++;
            }

            ///////////////////  END OF INNER STATES /////////////////////
            ///////////////////  END OF CONTINUOUS STATES /////////////////////

            // birth year
            gridDimensions[aa][6] = dimIndex;
            axes[aa][dimIndex][0] = DecisionParams.ptsBirthYear;
            axes[aa][dimIndex][1] = DecisionParams.minBirthYear;
            axes[aa][dimIndex][2] = DecisionParams.maxBirthYear;
            axes[aa][dimIndex][3] = 0.5;
            axes[aa][dimIndex][4] = 0;
            dimIndex++;

            // wage offer of principal earner (1 = receive wage offer)
            if (ageHh <= DecisionParams.maxAgeFlexibleLabourSupply && DecisionParams.flagLowWageOffer1) {
                axes[aa][dimIndex][0] = 2;
                axes[aa][dimIndex][1] = 0;
                axes[aa][dimIndex][2] = 1;
                axes[aa][dimIndex][3] = 0;
                axes[aa][dimIndex][4] = 0;
                dimIndex++;
            }

            // wage offer of principal earner (1 = receive wage offer)
            if (ageHh <= DecisionParams.maxAgeFlexibleLabourSupply && DecisionParams.FLAG_WAGE_OFFER2) {
                axes[aa][dimIndex][0] = 2;
                axes[aa][dimIndex][1] = 0;
                axes[aa][dimIndex][2] = 1;
                axes[aa][dimIndex][3] = 0;
                axes[aa][dimIndex][4] = 0;
                dimIndex++;
            }

            ///////////////////  START OF DISCRETE STATES /////////////////////

            // retirement status
            if (DecisionParams.flagRetirement && ageHh > DecisionParams.minAgeToRetire && ageHh <= DecisionParams.maxAgeFlexibleLabourSupply) {
                axes[aa][dimIndex][0] = 2;
                axes[aa][dimIndex][1] = 0;
                axes[aa][dimIndex][2] = 1;
                axes[aa][dimIndex][3] = 0;
                axes[aa][dimIndex][4] = 0;
                dimIndex++;
            }

            // health status
            if (DecisionParams.flagHealth && ageHh >= DecisionParams.minAgeForPoorHealth) {
                axes[aa][dimIndex][0] = DecisionParams.PTS_HEALTH;
                axes[aa][dimIndex][1] = DecisionParams.MIN_HEALTH;
                axes[aa][dimIndex][2] = DecisionParams.MAX_HEALTH;
                axes[aa][dimIndex][3] = 0;
                axes[aa][dimIndex][4] = 0;
                dimIndex++;
            }

            //disability status
            if (DecisionParams.flagDisability && ageHh >= DecisionParams.minAgeForPoorHealth && ageHh <= DecisionParams.maxAgeForDisability()) {
                axes[aa][dimIndex][0] = 2;
                axes[aa][dimIndex][1] = 0;
                axes[aa][dimIndex][2] = 1;
                axes[aa][dimIndex][3] = 0;
                axes[aa][dimIndex][4] = 0;
                dimIndex++;
            }

            // social care receipt
            if (Parameters.flagSocialCare && ageHh >= DecisionParams.minAgeReceiveFormalCare) {
                axes[aa][dimIndex][0] = 4; // none needed, no formal, informal and formal, only formal (see Enum SocialCareReceiptState)
                axes[aa][dimIndex][1] = 0;
                axes[aa][dimIndex][2] = 3;
                axes[aa][dimIndex][3] = 0;
                axes[aa][dimIndex][4] = 0;
                dimIndex++;
            }

            // social care provision
            if (Parameters.flagSocialCare) {
                axes[aa][dimIndex][0] = 4; // none, partner only, partner and non-partner, non-partner only (see Enum SocialCareProvision)
                axes[aa][dimIndex][1] = 0;
                axes[aa][dimIndex][2] = 3;
                axes[aa][dimIndex][3] = 0;
                axes[aa][dimIndex][4] = 0;
                dimIndex++;
            }

            //region
            if (DecisionParams.flagRegion) {
                axes[aa][dimIndex][0] = DecisionParams.PTS_REGION;
                axes[aa][dimIndex][1] = 1;
                axes[aa][dimIndex][2] = DecisionParams.PTS_REGION;
                axes[aa][dimIndex][3] = 0;
                axes[aa][dimIndex][4] = 0;
                dimIndex++;
            }

            //student
            if (ageHh <= Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION && DecisionParams.flagEducation) {
                axes[aa][dimIndex][0] = DecisionParams.PTS_STUDENT;
                axes[aa][dimIndex][1] = 0;
                axes[aa][dimIndex][2] = DecisionParams.PTS_STUDENT - 1;
                axes[aa][dimIndex][3] = 0;
                axes[aa][dimIndex][4] = 0;
                dimIndex++;
            }

            //education
            if (DecisionParams.flagEducation) {
                axes[aa][dimIndex][0] = DecisionParams.PTS_EDUCATION;
                axes[aa][dimIndex][1] = 0;
                axes[aa][dimIndex][2] = DecisionParams.PTS_EDUCATION - 1;
                axes[aa][dimIndex][3] = 0;
                axes[aa][dimIndex][4] = 0;
                dimIndex++;
            }

            //dependent children
            for (int ii = 0; ii < DecisionParams.NUMBER_BIRTH_AGES; ii++) {
                if (ageHh >= DecisionParams.BIRTH_AGE[ii] && ageHh < (DecisionParams.BIRTH_AGE[ii] + Parameters.AGE_TO_BECOME_RESPONSIBLE)) {
                    axes[aa][dimIndex][0] = DecisionParams.MAX_BIRTHS[ii] + 1;
                    axes[aa][dimIndex][1] = 0;
                    axes[aa][dimIndex][2] = DecisionParams.MAX_BIRTHS[ii];
                    axes[aa][dimIndex][3] = 0;
                    axes[aa][dimIndex][4] = 0;
                    dimIndex++;
                }
            }

            //cohabitation (1 = cohabiting)
            if (ageHh <= DecisionParams.MAX_AGE_COHABITATION) {
                axes[aa][dimIndex][0] = 2;
                axes[aa][dimIndex][1] = 0;
                axes[aa][dimIndex][2] = 1;
                axes[aa][dimIndex][3] = 0;
                axes[aa][dimIndex][4] = 0;
                dimIndex++;
            }

            //gender (1 = female)
            axes[aa][dimIndex][0] = 2;
            axes[aa][dimIndex][1] = 0;
            axes[aa][dimIndex][2] = 1;
            axes[aa][dimIndex][3] = 0;
            axes[aa][dimIndex][4] = 0;
            dimIndex++;
        }

        /*
         * POPULATE gridDimensions
         *
         * gridDimensions IS AN ARRAY OF SUMMARY STATISTICS USED TO ASSIST LOOPS THROUGH THE GRIDS
         *      gridDimensions[aa][0] - number of discrete combinations associated with axes on inner loop at age aa
         *      gridDimensions[aa][1] - number of discrete combinations associated with axes on outer loop at age aa
         *      gridDimensions[aa][2] - total size of grid dimensions at age aa
         *      gridDimensions[aa][3] - total number of grid points for all ages up to age aa-1
         *      gridDimensions[aa][4] - number of inner states at age aa
         *      gridDimensions[aa][5] - number of outer states at age aa
         *      gridDimensions[aa][6] - index in axes array of birth year state at age aa
         *
         *          HENCE: gridDimensions[aa][2] = PRODUCT OF gridDimensions[aa][0:1]
         *                 gridDimensions[aa][3] = SUM OF gridDimensions[0:aa-1][2]
         */
        long startSliceIndex = 0;
        for (int aa = 0; aa < simLifeSpan; aa++) {
            gridDimensions[aa][0] = innerGridSize(aa);
            gridDimensions[aa][1] = outerGridSize(aa);
            gridDimensions[aa][2] = gridDimensions[aa][0] * gridDimensions[aa][1];
            gridDimensions[aa][3] = startSliceIndex;
            gridDimensions[aa][4] = innerGridStates(aa);
            gridDimensions[aa][5] = outerGridStates(aa);
            startSliceIndex += gridDimensions[aa][2];
        }
    }


    /*
     * WORKING METHODS
     */


    /**
     * METHOD TO EVALUATE THE NUMBER OF STATES CONSIDERED IN THE INNER LOOP FOR GRID SOLUTIONS AT AGE AA
     * @param aa defines the age index of interest (age = aa + simpaths.data.Parameters.AGE_TO_BECOME_RESPONSIBLE)
     * @return defines the number of states
     */
    public int innerGridStates(int aa) {
        int states, ii;
        states = 0;
        ii = 0;
        do {
            if (axes[aa][ii][0] > 0.1) {
                if (axes[aa][ii][4] > 0.1) {
                    states += 1;
                }
                ii++;
            } else {
                ii = numberOfStates;
            }
        } while (ii < numberOfStates);
        return states;
    }

    /**
     * METHOD TO EVALUATE THE NUMBER OF STATES CONSIDERED IN THE OUTER LOOP FOR GRID SOLUTIONS AT AGE AA
     * @param aa defines the age index of interest (age = aa + simpaths.data.Parameters.AGE_TO_BECOME_RESPONSIBLE)
     * @return defines the number of elements in the given grid slice
     */
    public int outerGridStates(int aa) {
        int states, ii;
        states = 0;
        ii = 0;
        do {
            if (axes[aa][ii][0] > 0.1) {
                if (axes[aa][ii][4] < 0.1) {
                    states += 1;
                }
                ii++;
            } else {
                ii = numberOfStates;
            }
        } while (ii < numberOfStates);
        return states;
    }

    /**
     * METHOD TO EVALUATE THE SIZE OF THE GRID SEGMENT ASSOCIATED WITH ALL INNER STATES AT A GIVEN AGE FOR IO SOLUTIONS
     * @param aa defines the age index of interest (age = aa + simpaths.data.Parameters.AGE_TO_BECOME_RESPONSIBLE)
     * @return defines the number of elements in the given grid slice
     */
    public int innerGridSize(int aa) {
        int size, ii;
        size = 1;
        ii = 0;
        do {
            if (axes[aa][ii][0] > 0.1) {
                if (axes[aa][ii][4] > 0.1) {
                    size *= (int) Math.round(axes[aa][ii][0]);
                }
                ii++;
            } else {
                ii = numberOfStates;
            }
        } while (ii < numberOfStates);
        return size;
    }

    /**
     * METHOD TO EVALUATE THE SIZE OF THE GRID SEGMENT ASSOCIATED WITH ALL OUTER STATES AT A GIVEN AGE
     * @param aa defines the age index of interest (age = aa + simpaths.data.Parameters.AGE_TO_BECOME_RESPONSIBLE)
     * @return defines the number of elements in the given grid slice
     */
    public int outerGridSize(int aa) {
        int size, ii;
        size = 1;
        ii = 0;
        do {
            if (axes[aa][ii][0] > 0.1) {
                if (axes[aa][ii][4] < 0.1) {
                    size *= Math.round(axes[aa][ii][0]);
                }
                ii++;
            } else {
                ii = numberOfStates;
            }
        } while (ii < numberOfStates);
        return size;
    }

    /**
     * METHOD TO EVALUATE THE SIZE OF THE GRID SLICE ASSOCIATED WITH A GIVEN AGE
     * @param aa defines the age index of interest (age = aa + simpaths.data.Parameters.AGE_TO_BECOME_RESPONSIBLE)
     * @return defines the number of elements in the given grid slice
     */
    public int totalGridSize(int aa) {
        int size, ii;
        size = 1;
        ii = 0;
        do {
            if (axes[aa][ii][0] > 0.1) {
                size *= Math.round(axes[aa][ii][0]);
                ii++;
            } else {
                ii = numberOfStates;
            }
        } while (ii < numberOfStates);
        return size;
    }

    public int getIndex(Enum<?> axisID, int ageYears) {
        return getIndex(axisID, ageYears, 0);
    }

    public int getIndex(Enum<?> axisID, int ageYears, int birthAge) {

        // liquid wealth
        int dimIndex = 0;
        if (axisID==Axis.LiquidWealth) return dimIndex;
        dimIndex++;

        // full-time wage potential
        if (ageYears <= DecisionParams.maxAgeFlexibleLabourSupply) {
            if (axisID==Axis.WagePotential) return dimIndex;
            dimIndex++;
        } else {
            if (axisID==Axis.WagePotential) return -1;
        }

        // private pension
        if (DecisionParams.flagPrivatePension && ageYears > DecisionParams.minAgeToRetire) {
            if (axisID==Axis.PensionIncome) return dimIndex;
            dimIndex++;
        } else {
            if (axisID==Axis.PensionIncome) return -1;
        }

        // birth year
        if (axisID==Axis.BirthYear) return dimIndex;
        dimIndex++;

        // wage offer of principal earner (1 = receive wage offer)
        if (ageYears <= DecisionParams.maxAgeFlexibleLabourSupply && DecisionParams.flagLowWageOffer1) {
            if (axisID==Axis.WageOffer1) return dimIndex;
            dimIndex++;
        } else {
            if (axisID==Axis.WageOffer1) return -1;
        }

        // wage offer of principal earner (1 = receive wage offer)
        if (ageYears <= DecisionParams.maxAgeFlexibleLabourSupply && DecisionParams.FLAG_WAGE_OFFER2) {
            if (axisID==Axis.WageOffer2) return dimIndex;
            dimIndex++;
        } else {
            if (axisID==Axis.WageOffer2) return -1;
        }

        // retirement
        if (DecisionParams.flagRetirement && ageYears > DecisionParams.minAgeToRetire && ageYears <= DecisionParams.maxAgeFlexibleLabourSupply) {
            if (axisID==Axis.Retirement) return dimIndex;
            dimIndex++;
        } else {
            if (axisID==Axis.Retirement) return -1;
        }

        // health status
        if (DecisionParams.flagHealth && ageYears >= DecisionParams.minAgeForPoorHealth) {
            if (axisID==Axis.Health) return dimIndex;
            dimIndex++;
        } else {
            if (axisID==Axis.Health) return -1;
        }

        // disability
        if (DecisionParams.flagDisability && ageYears >= DecisionParams.minAgeForPoorHealth && ageYears <= DecisionParams.maxAgeForDisability()) {
            if (axisID==Axis.Disability) return dimIndex;
            dimIndex++;
        } else {
            if (axisID==Axis.Disability) return -1;
        }

        // social care receipt
        if (Parameters.flagSocialCare && ageYears >= DecisionParams.minAgeReceiveFormalCare) {
            if (axisID==Axis.SocialCareReceiptState) return dimIndex;
            dimIndex++;
        } else {
            if (axisID==Axis.SocialCareReceiptState) return -1;
        }

        // social care provision
        if (Parameters.flagSocialCare) {
            if (axisID==Axis.SocialCareProvision) return dimIndex;
            dimIndex++;
        } else {
            if (axisID==Axis.SocialCareProvision) return -1;
        }

        // region
        if (DecisionParams.flagRegion) {
            if (axisID==Axis.Region) return dimIndex;
            dimIndex++;
        } else {
            if (axisID==Axis.Region) return -1;
        }

        // student
        if (ageYears <= Parameters.MAX_AGE_TO_LEAVE_CONTINUOUS_EDUCATION && DecisionParams.flagEducation) {
            if (axisID==Axis.Student) return dimIndex;
            dimIndex++;
        } else {
            if (axisID==Axis.Student) return -1;
        }

        // education
        if (DecisionParams.flagEducation) {
            if (axisID==Axis.Education) return dimIndex;
            dimIndex++;
        } else {
            if (axisID==Axis.Education) return -1;
        }

        // dependent children
        for (int ii = 0; ii < DecisionParams.NUMBER_BIRTH_AGES; ii++) {
            if (ageYears >= DecisionParams.BIRTH_AGE[ii] && ageYears < (DecisionParams.BIRTH_AGE[ii] + Parameters.AGE_TO_BECOME_RESPONSIBLE)) {
                if (axisID==Axis.Child && ii==birthAge) return dimIndex;
                dimIndex++;
            }
        }
        if (axisID==Axis.Child) return -1;

        // cohabitation (1 = cohabiting)
        if (ageYears <= DecisionParams.MAX_AGE_COHABITATION) {
            if (axisID==Axis.Cohabitation) return dimIndex;
            dimIndex++;
        } else {
            if (axisID==Axis.Cohabitation) return -1;
        }

        // gender (1 = female)
        if (axisID==Axis.Gender) return dimIndex;

        // not recognised
        return -1;
    }
}
