package simpaths.model.taxes;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import simpaths.model.decisions.DecisionParams;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * CLASS TO STORE SETS OF DATABASE MATCHES
 *
 */
public class Matches {


    /**
     * ATTRIBUTES
     */
    private Set<Match> set = new HashSet<>();


    /**
     * CONSTRUCTORS
     */
    public Matches(){}


    /**
     * GETTERS AND SETTERS
     */
    public Set<Match> getSet() {return set;}
    public void addMatch(Match newMatch) {
        if (!isMatchInSet(newMatch))
            set.add(newMatch);
    }
    public void addSet(Set<Match> newSet) {
        for ( Match newMatch : newSet) {
            addMatch(newMatch);
        }
    }


    /**
     * WORKER METHODS
     */
    public boolean isMatchInSet(Match match) {
        boolean result = false;
        for (Match ss : set) {
            if ( (match.getKey().getKey(0) == ss.getKey().getKey(0)) &&
                    (match.getCandidateID() == ss.getCandidateID()) &&
                    (Math.abs(match.getTargetNormalisedOriginalIncome() - ss.getTargetNormalisedOriginalIncome())<5.0) ) {
                result = true;
                break;
            }
        }
        return result;
    }
    public boolean isEmpty() {
        return set.isEmpty();
    }

    public void write(int ageYears) {
        // Method to write imperfect matches to csv file for post-simulation processing

        File dir = new File(DecisionParams.gridsOutputDirectory);
        if (!dir.exists()) dir.mkdir();
        String filePath = DecisionParams.gridsOutputDirectory + File.separator + "poor_match_age_" + ageYears + ".csv";
        String[] HEADERS = {"key", "criterion", "candidateID", "targetIncome"};
        try {
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath));
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(HEADERS).build();
            CSVPrinter printer = new CSVPrinter(writer, csvFormat);

            for (Match match : set) {
                List<String> record = new ArrayList<>();
                record.add(match.getKey0String());
                record.add(match.getMatchCriterionString());
                record.add(match.getCandidateIDString());
                record.add(match.getTargetNormalisedOriginalIncomeString());
                printer.printRecord(record);
            }
            writer.flush();
            writer.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
