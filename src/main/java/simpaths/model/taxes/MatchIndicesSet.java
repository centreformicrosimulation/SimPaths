package simpaths.model.taxes;


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

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
 * CLASS TO STORE SET OF MATCHINDICES TO ASSIST EXPANSION OF TAX DATABASE INPUT DATA
 *
 */
public class MatchIndicesSet {

    private Set<MatchIndices> set = new HashSet<>();
    private final String[] HEADERS = {"candidateID", "targetIncome", "age", "adults", "children", "employment", "disability",
    "careprovision", "income", "dualincome", "childcare"};


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
}
