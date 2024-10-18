package simpaths.model.taxes;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import simpaths.model.decisions.DecisionParams;

import java.io.*;
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
    private final String[] HEADERS = {"key", "criterion", "candidateID", "targetIncome"};


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
        double incLimit = Math.max(50.0, 0.05*match.getTargetNormalisedOriginalIncome());
        for (Match ss : set) {
            if ( (match.getKey0() == ss.getKey0()) && (Math.abs(match.getTargetNormalisedOriginalIncome() - ss.getTargetNormalisedOriginalIncome())<incLimit) ) {
                result = true;
                break;
            }
        }
        return result;
    }
    public boolean isEmpty() {
        return set.isEmpty();
    }

    public void write(String fileDirectory, String fileName) {
        // Method to write imperfect matches to csv file for post-simulation processing

        File dir = new File(fileDirectory);
        if (!dir.exists()) dir.mkdir();
        try {
            String filePath = fileDirectory + File.separator + fileName;
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

    public void read(boolean flagGrid, int id, String filePath) throws IOException {

        File file = new File(filePath);
        if (!file.exists())
            throw new RuntimeException("failed to find csv file to read: " + filePath);

        Reader reader = new FileReader(filePath);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(HEADERS).setSkipHeaderRecord(true).build();
        Iterable<CSVRecord> records = csvFormat.parse(reader);
        for (CSVRecord record : records) {
            int key0 = Integer.parseInt(record.get("key"));
            int criterion = Integer.parseInt(record.get("criterion"));
            long candidateID = Long.parseLong(record.get("candidateID"));
            double targetNormalisedOriginalIncome = Double.parseDouble(record.get("targetIncome"));
            addMatch(new Match(flagGrid, id, key0, candidateID, criterion, targetNormalisedOriginalIncome));
        }
    }
}
