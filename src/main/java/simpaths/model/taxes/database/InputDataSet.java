package simpaths.model.taxes.database;


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * CLASS TO MANAGE STORAGE OF INPUT DATA
 */
public class InputDataSet {


    /**
     * ATTRIBUTES
     */
    private List<Map> set = new ArrayList<>();


    /**
     * CONSTRUCTORS
     */
    public InputDataSet(){}


    /**
     * WORKER METHODS
     */
    public void add(CloneBenefitUnit household) {
        for (Map person : household.getMembers()) {
            set.add(person);
        }
    }
    public List<Map> getSet() {return set;}
    public void read(String[] variables, String filePath) throws IOException {

        File file = new File(filePath);
        if (!file.exists())
            throw new RuntimeException("failed to find file: " + filePath);

        Reader reader = new FileReader(filePath);
        CSVFormat csvFormat = CSVFormat.TDF.builder().setHeader(variables).setSkipHeaderRecord(true).build();
        Iterable<CSVRecord> records = csvFormat.parse(reader);
        for (CSVRecord record : records) {
            Map values = new HashMap<>();
            for (String variable : variables) {
                values.put(variable, Double.parseDouble(record.get(variable)));
            }
            set.add(values);
        }
    }
    public void write(String[] variables, String[] longVars, String[] intVars, String directory, String fileName) throws IOException {

        File chk = new File(directory);
        if (!chk.exists()) chk.mkdir();
        String filePath = directory + File.separator + fileName;
        safeDelete(filePath);

        Writer writer = new FileWriter(filePath);
        CSVFormat csvFormat = CSVFormat.TDF.builder().setHeader(variables).build();
        CSVPrinter printer = new CSVPrinter(writer, csvFormat);
        for (Map obs : set) {
            List<String> record = new ArrayList<>();
            for (String variable : variables) {
                if (Arrays.stream(longVars).anyMatch(variable::equals))
                    record.add(Long.toString(Double.valueOf((double)obs.get(variable)).longValue()));
                else if (Arrays.stream(intVars).anyMatch(variable::equals))
                    record.add(String.valueOf(Double.valueOf((double)obs.get(variable)).intValue()));
                else
                    record.add(Double.toString((double)obs.get(variable)));
            }
            printer.printRecord(record);
        }
        writer.flush();
        writer.close();
    }
    private static void safeDelete(String filePath) {
        File file = new File(filePath);
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }
    public double getMaxValue(String variable) {

        Double val = null;
        for(Map obs : set) {

            Object oo = obs.get(variable);
            if (oo==null)
                throw new RuntimeException("problem identifying data set value for variable " + variable);

            Double valHere = Double.valueOf((double)oo);
            if (val==null || (valHere!=null && valHere > val))
                val = valHere;
        }
        if (val.equals(null))
            throw new RuntimeException("Failed to identify maximum value for variable " + variable);
        return val;
    }
}
