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
     *
     * BEFORE USING THIS METHOD NEED TO ENSURE THAT THE "DATABASE SPECIFIC VARIABLES" ARE
     * DEFINED AS DESIRED
     */
    public static void extendInputData() {
        String ukmodInputDirectory = "C:\\MyFiles\\99 DEV ENV\\UKMOD\\MODELS\\PRIVATE\\Input";
        String ukmodInputFilename = "UK_2019_b1";
        String imperfectMatchesSimPath = "C:\\MyFiles\\99 DEV ENV\\JAS-MINE\\SimPaths\\output\\test";
        extendInputData(ukmodInputDirectory, ukmodInputFilename, imperfectMatchesSimPath, true);
    }

    public static void extendInputData(String imperfectMatchesSimPath) {
        String ukmodInputDirectory = "C:\\MyFiles\\99 DEV ENV\\UKMOD\\MODELS\\PRIVATE\\Input";
        String ukmodInputFilename = "UK_2019_b1";
        extendInputData(ukmodInputDirectory, ukmodInputFilename, imperfectMatchesSimPath, false);
    }

    public static void extendInputData(String ukmodInputDirectory, String ukmodInputFilename, String imperfectMatchesSimPath, boolean flagReadScreenedIndices) {

        // database specific variables
        String[] variablesAll = Variables2019b.listAll();
        String[] variablesLong = Variables2019b.listLong();
        String[] variablesInt = Variables2019b.listInt();
        String datasetPath = ukmodInputDirectory + File.separator + ukmodInputFilename + ".txt";
        String outputDirectory = ukmodInputDirectory;
        String outputFilename = ukmodInputFilename + " - augmented.txt";

        // compile data that identify database gaps
        MatchIndicesSet imperfectMatchIndices;
        if (flagReadScreenedIndices) {
            try {
                imperfectMatchIndices = new MatchIndicesSet();
                imperfectMatchIndices.read(imperfectMatchesSimPath);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        } else
            imperfectMatchIndices = screenImperfectMatches(true, imperfectMatchesSimPath);

        // load input data
        InputDataSet dataset = new InputDataSet();
        try {
            dataset.read(variablesAll, datasetPath);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        // extend dataset
        int pop = imperfectMatchIndices.getSet().size();
        System.out.println("Expanding input data to account for additional " + pop + " benefit units");
        int no = 0, dec=1;
        long newHouseholdId = Double.valueOf(dataset.getMaxValue("idhh")).longValue() + 1;
        for (MatchIndices imperfectMatch : imperfectMatchIndices.getSet()) {

            if (pop>5000) {
                no++;
                if (no > pop/10*dec) {
                    System.out.println("Completed processing " + dec * 10 + "% of targeted benefit units");
                    dec++;
                }
            }
            CloneBenefitUnit benefitUnit = new CloneBenefitUnit();
            long[] result = benefitUnit.clone(imperfectMatch.getCandidateID(), dataset, variablesAll, newHouseholdId);
            if (result[2]==0) {
                // clone successfully created

                newHouseholdId = result[0];
                long newPersonId = result[1];

                // match clone to target indices
                boolean changed;
                changed = benefitUnit.matchAgeIndex(imperfectMatch.getAge());
                newPersonId = benefitUnit.matchAdultIndex(imperfectMatch.getAdults(), newPersonId);
                newPersonId = benefitUnit.matchChildrenIndex(imperfectMatch.getChildren(), newPersonId);
                changed = benefitUnit.matchEmploymentIndex(imperfectMatch.getEmployment());
                changed = benefitUnit.matchDisabledIndex(imperfectMatch.getDisability());
                changed = benefitUnit.matchCarerIndex(imperfectMatch.getCareProvision());
                changed = benefitUnit.matchChildcareIndex(imperfectMatch.getChildcare());
                changed = benefitUnit.matchDualIncomeIndex(imperfectMatch.getDualIncome());

                // adjust income add to input data
                int priceYear = benefitUnit.getPriceYear();
                double targetIncome = imperfectMatch.getTargetNormalisedOriginalIncome() *
                        Parameters.getTimeSeriesValue(priceYear, TimeSeriesVariable.Inflation) /
                        Parameters.getTimeSeriesValue(Parameters.BASE_PRICE_YEAR, TimeSeriesVariable.Inflation);
                for (double income = targetIncome - 100.0; income <= targetIncome + 101.0; income += 50.0) {

                    benefitUnit.matchIncome(income);
                    dataset.add(benefitUnit);
                    if (income<targetIncome + 99.0) {
                        CloneBenefitUnit household1 = new CloneBenefitUnit();
                        result = household1.clone(benefitUnit, variablesAll, newHouseholdId);
                        newHouseholdId = result[0];
                        benefitUnit = household1;
                    }
                }
            }
        }

        // save augmented dataset
        try {
            dataset.write(variablesAll, variablesLong, variablesInt, outputDirectory, outputFilename);
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
            if (Parameters.enableIntertemporalOptimisations) {

                for (int aa=Parameters.AGE_TO_BECOME_RESPONSIBLE; aa<=Parameters.maxAge; aa++) {
                    String filePath = dir + File.separator + "grids\\poor_taxmatch_age_" + aa + ".csv";
                    File file = new File(filePath);
                    if (file.exists())
                        imperfectMatches.read(true, aa, filePath);
                }
            }
            for (int yy=Parameters.startYear; yy<=Parameters.endYear; yy++) {
                String filePath = dir + File.separator + "csv\\poor_taxmatch_year_" + yy + ".csv";
                File file = new File(filePath);
                if (file.exists())
                    imperfectMatches.read(false, yy, filePath);
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
