package simpaths.model.taxes.database;


import simpaths.data.Parameters;
import simpaths.model.enums.TimeSeriesVariable;
import simpaths.model.taxes.KeyFunction;
import simpaths.model.taxes.Match;
import simpaths.model.taxes.MatchFeature;
import simpaths.model.taxes.Matches;

import java.io.File;
import java.io.IOException;

public class DatabaseExtension {


    /**
     * METHOD TO EXTEND UKMOD INPUT DATA TO FILL GAPS
     */
    public static void extendInputData() {

        // database specific variables
        String[] variablesAll = Variables2019b.list();
        String datasetPath = "C:\\MyFiles\\99 DEV ENV\\UKMOD\\MODELS\\PRIVATE\\Input\\UK_2019_b1.txt";
        String imperfectMatchesPath = "C:\\MyFiles\\99 DEV ENV\\JAS-MINE\\tests\\behavioural solutions\\Grid size\\91x91";
        String outputDirectory = "C:\\MyFiles\\99 DEV ENV\\UKMOD\\MODELS\\PRIVATE\\Input";
        String outputFilename = "UK_2019_b1adj.txt";

        // compile data that identify database gaps
        MatchIndicesSet imperfectMatchIndices = screenImperfectMatches(false, imperfectMatchesPath);

        // load input data
        InputDataSet dataset = new InputDataSet();
        try {
            dataset.read(variablesAll, datasetPath);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        // extend dataset
        long newHouseholdId = 999900000;
        for (MatchIndices imperfectMatch : imperfectMatchIndices.getSet()) {

            CloneHousehold household = new CloneHousehold();
            long[] result = household.clone(imperfectMatch.getCandidateID(), dataset, variablesAll, newHouseholdId);
            newHouseholdId = result[0];
            long newPersonId = result[1];

            // match clone to target indices
            boolean changed;
            changed = household.matchAgeIndex(imperfectMatch.getAge());
            newPersonId = household.matchAdultIndex(imperfectMatch.getAdults(), newPersonId);
            newPersonId = household.matchChildrenIndex(imperfectMatch.getChildren(), newPersonId);
            changed = household.matchEmploymentIndex(imperfectMatch.getEmployment());
            changed = household.matchDisabledIndex(imperfectMatch.getDisability());
            changed = household.matchCarerIndex(imperfectMatch.getCareProvision());
            changed = household.matchChildcareIndex(imperfectMatch.getChildcare());
            changed = household.matchDualIncomeIndex(imperfectMatch.getDualIncome());

            // adjust income add to input data
            int priceYear = household.getPriceYear();
            double targetIncome = imperfectMatch.getTargetNormalisedOriginalIncome() *
                    Parameters.getTimeSeriesValue(priceYear, TimeSeriesVariable.Inflation) /
                    Parameters.getTimeSeriesValue(Parameters.BASE_PRICE_YEAR, TimeSeriesVariable.Inflation);
            for (double income = targetIncome - 100.0; income <= targetIncome + 101.0; income += 50.0) {

                household.matchIncome(income);
                dataset.add(household);
                if (income<targetIncome + 99.0) {
                    CloneHousehold household1 = new CloneHousehold();
                    result = household1.clone(household, variablesAll, newHouseholdId);
                    newHouseholdId = result[0];
                    household = household1;
                }
            }
        }

        // save augmented dataset
        try {
            dataset.write(variablesAll, outputDirectory, outputFilename);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    /**
     * METHOD TO SCREEN IMPERFECT DATABASE MATCHES REPORTED BY THE MODEL AND REPACKAGE DATA TO FILL GAPS
     */
    public static MatchIndicesSet screenImperfectMatches(boolean flagSave, String dir) {

        // read and screen
        Matches imperfectMatches = new Matches();
        try {
            for (int aa=18; aa<131; aa++) {
                String filePath = dir + File.separator + "poor_match_age_" + aa + ".csv";
                File file = new File(filePath);
                if (file.exists())
                    imperfectMatches.read(filePath);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        // generate working variables for expanding tax database
        MatchIndicesSet matchIndicesSet = new MatchIndicesSet();
        KeyFunction keyFunction = new KeyFunction();
        for (Match match : imperfectMatches.getSet()) {

            MatchIndices indices = new MatchIndices(match);
            for (MatchFeature feature : MatchFeature.values()) {
                indices.set(feature, keyFunction.getMatchFeatureIndex(feature, 0, match.getKey0()));
            }
            matchIndicesSet.add(indices);
        }

        // write set to CSV file for processing in Stata
        if (flagSave)
            matchIndicesSet.write(dir);

        // return
        return matchIndicesSet;
    }
}
