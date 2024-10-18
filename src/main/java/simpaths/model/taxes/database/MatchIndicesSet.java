package simpaths.model.taxes.database;


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import simpaths.model.taxes.Match;
import simpaths.model.taxes.MatchFeature;
import simpaths.model.taxes.database.MatchIndices;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * CLASS TO STORE SET OF MATCHINDICES TO ASSIST EXPANSION OF TAX DATABASE INPUT DATA
 *
 */
public class MatchIndicesSet {

    private Set<MatchIndices> set = new HashSet<>();
    private final String[] HEADERS = {"candidateID", "gridAge", "simYear", "key0", "targetIncome", "Age", "Adults", "Children", "Employment", "Disability",
    "CareProvision", "Income", "DualIncome", "Childcare"};


    /**
     * CONSTRUCTORS
     */
    public MatchIndicesSet(){}


    /**
     * WORKER METHODS
     */
    public void add(MatchIndices matchIndices) {
        set.add(matchIndices);
    }

    public Set<MatchIndices> getSet() {return set;}

    public void write(String dir) {

        File chk = new File(dir);
        if (!chk.exists()) chk.mkdir();
        String filePath = dir + File.separator + "poor_matches_compiled.csv";
        try {
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath));
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(HEADERS).build();
            CSVPrinter printer = new CSVPrinter(writer, csvFormat);

            for (MatchIndices indices : set) {
                List<String> record = new ArrayList<>();
                record.add(indices.getCandidateIDString());
                record.add(indices.getGridAgeString());
                record.add(indices.getSimYearString());
                record.add(indices.getKey0String());
                record.add(indices.getTargetOriginalIncomeString());
                for (MatchFeature feature : MatchFeature.values()) {
                    if (!feature.equals(MatchFeature.Final))
                        record.add(indices.getString(feature));
                }
                printer.printRecord(record);
            }
            writer.flush();
            writer.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public void read(String dir) throws IOException {

        String filePath = dir + File.separator + "poor_matches_compiled.csv";
        File file = new File(filePath);
        if (!file.exists())
            throw new RuntimeException("failed to find csv file to read: " + filePath);

        Reader reader = new FileReader(filePath);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(HEADERS).setSkipHeaderRecord(true).build();
        Iterable<CSVRecord> records = csvFormat.parse(reader);
        for (CSVRecord record : records) {
            long candidateID = Long.parseLong(record.get("candidateID"));
            int gridAge = Integer.parseInt(record.get("gridAge"));
            int simYear = Integer.parseInt(record.get("simYear"));
            int key0 = Integer.parseInt(record.get("key0"));
            double targetNormalisedOriginalIncome = Double.parseDouble(record.get("targetIncome"));
            MatchIndices indices = new MatchIndices(candidateID, gridAge, simYear, key0, targetNormalisedOriginalIncome);
            for (MatchFeature feature : MatchFeature.values()) {
                if (!feature.equals(MatchFeature.Final)) {
                    indices.set(feature, Integer.parseInt(record.get(feature.toString())));
                }
            }
            set.add(indices);
        }
    }
}
