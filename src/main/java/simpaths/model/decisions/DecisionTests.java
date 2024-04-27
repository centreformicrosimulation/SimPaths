package simpaths.model.decisions;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import simpaths.data.Parameters;
import simpaths.model.taxes.Matches;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DecisionTests {

    public DecisionTests() {}

    public static void apacheCsvWriteTest2() {

        List<WriteGridsBean> beans = new ArrayList<>();

        WriteGridsBean bean1 = new WriteGridsBean();
        bean1.setCohabitation(0);
        bean1.setLiquidWealth(53572266.858582);
        bean1.setValueFunction(1.357372715792E-9);
        beans.add(bean1);

        WriteGridsBean bean2 = new WriteGridsBean();
        bean2.setCohabitation(1);
        bean2.setLiquidWealth(-28386.3841173);
        bean2.setValueFunction(2.343143434141E-12);
        beans.add(bean2);

        String NEW_LINE_SEPARATOR = "\n";

        // write output to csv file
        File dir = new File(DecisionParams.gridsOutputDirectory);
        if (!dir.exists()) dir.mkdir();
        String filePath = DecisionParams.gridsOutputDirectory + File.separator + "test.csv";
        try {
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath));
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader("cohabitation", "value_function", "liquid_wealth").build();
            CSVPrinter printer = new CSVPrinter(writer, csvFormat);

            for (WriteGridsBean bean : beans) {
                List<String> record = new ArrayList<>();
                record.add(bean.getCohabitationString());
                record.add(bean.getValueFunctionString());
                record.add(bean.getLiquidWealthString());
                printer.printRecord(record);
            }

            writer.flush();
            writer.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void apacheCsvWriteTest1() {

        Map<String, String> AUTHOR_BOOK_MAP = new HashMap<String, String>() {
            {
                put("Dan Simmons", "Hyperion");
                put("Douglas Adams", "The Hitchhiker's Guide to the Galaxy");
            }
        };
        String[] HEADERS = { "author", "title"};

        File dir = new File(DecisionParams.gridsOutputDirectory);
        if (!dir.exists()) dir.mkdir();
        String filePath = DecisionParams.gridsOutputDirectory + File.separator + "test.csv";
        try {
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath));
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(HEADERS).build();
            try (final CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
                AUTHOR_BOOK_MAP.forEach((author, title) -> {
                    try {
                        printer.printRecord(author, title);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void openCsvWriteTest() {
        // FAILS DUE TO OLD APACHE COMMONS BEAN UTILS DEPENDENCY IN JASMINE-GUI

        List<WriteGridsBean> beans = new ArrayList<>();

        WriteGridsBean bean1 = new WriteGridsBean();
        bean1.setCohabitation(0);
        bean1.setBirthYear(1920);
        bean1.setGender(0);
        bean1.setEducation(0);
        bean1.setHealth(0);
        bean1.setLiquidWealth(5.0);
        bean1.setWagePotentialperHour(5.0);
        bean1.setPensionIncomePerYear(5.0);
        bean1.setValueFunction(1.0E-9);
        bean1.setConsumptionShare(0.1);
        bean1.setEmployment1(0.0);
        bean1.setEmployment2(0.0);
        beans.add(bean1);

        WriteGridsBean bean2 = new WriteGridsBean();
        bean2.setCohabitation(0);
        bean2.setBirthYear(1920);
        bean2.setGender(0);
        bean2.setEducation(0);
        bean2.setHealth(0);
        bean2.setLiquidWealth(5.0);
        bean2.setWagePotentialperHour(5.0);
        bean2.setPensionIncomePerYear(5.0);
        bean2.setValueFunction(1.0E-9);
        bean2.setConsumptionShare(0.1);
        bean2.setEmployment1(0.0);
        bean2.setEmployment2(0.0);
        beans.add(bean2);

        // write output to csv file
        File dir = new File(DecisionParams.gridsOutputDirectory);
        if (!dir.exists()) dir.mkdir();
        String filePath = DecisionParams.gridsOutputDirectory + File.separator + "test.csv";
        try {
            Writer writer = new FileWriter(filePath);
            StatefulBeanToCsv<WriteGridsBean> beanToCsv = new StatefulBeanToCsvBuilder<WriteGridsBean>(writer)
                    .withQuotechar('\'').withSeparator(CSVWriter.DEFAULT_SEPARATOR).build();
            beanToCsv.write(beans);
            writer.close();
        } catch (CsvDataTypeMismatchException e) {
            throw new RuntimeException(e);
        } catch (CsvRequiredFieldEmptyException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void compareGrids() {

        double TOL = Math.ulp(1.0) * 2.0;

        // load in grids for comparison
        String name1 = "test0";
        DecisionParams.setGridsInputDirectory(name1);
        Grids grids1 = new Grids();
        ManagerFileGrids.read(grids1);

        String name2 = "test1";
        DecisionParams.setGridsInputDirectory(name2);
        Grids grids2 = new Grids();
        ManagerFileGrids.read(grids2);

        System.out.println("Testing grid solution from run " + name1 + " against solution from run " + name2);
        double maxDiff = -9.0;
        long counter1=0, counter2=0, counter3=0;

        // loop through grids to find differences
        for (int aa=grids1.scale.simLifeSpan - 1; aa>=0; aa--) {

            // set age specific working variables
            int innerDimension = (int)grids1.scale.gridDimensions[aa][0];
            int outerDimension = (int)grids1.scale.gridDimensions[aa][1];
            long ii0=-9;
            for (int iiOuter=0; iiOuter<outerDimension; iiOuter++) {

                // identify current state combination for outer states
                int ageYears = aa + Parameters.AGE_TO_BECOME_RESPONSIBLE;
                States outerStates = new States(grids1.scale, ageYears);
                outerStates.populateOuterGridStates(iiOuter);
                boolean loopConsider = outerStates.checkOuterStateCombination();
                if (loopConsider) {
                    for (int iiInner = 0; iiInner < innerDimension; iiInner++) {
                        // identify current state combination and copy expectations
                        States currentStates = new States(outerStates);
                        currentStates.populateInnerGridStates(iiInner);
                        boolean stateConsider = currentStates.checkStateCombination();
                        if (stateConsider) {

                            long indexHere = currentStates.returnGridIndex();
                            if (ii0>=0) {
                                if (indexHere!=ii0+1) {
                                    indexHere = currentStates.returnGridIndex();
                                }
                            }
                            double val1 = grids1.valueFunction.get(indexHere);
                            double val2 = grids2.valueFunction.get(indexHere);
                            double diff = Math.abs(val1 - val2);
                            if (diff > maxDiff)
                                maxDiff = diff;
                            if (diff > 0.0) {
                                counter1++;
                            }
                            if (diff > TOL) {
                                counter2++;
                            }
                            if (diff > 1.0E-7 * Math.abs(val1) ) {
                                counter3++;
                            }
                            ii0 = indexHere;
                        } else {
                            ii0 = -9;
                        }
                    }
                } else {
                    ii0 = -9;
                }
            }
        }
        System.out.println("Number of grid points with different value function estimates: " + counter1);
        System.out.println("Number of grid points with significantly different value function estimates: " + counter2);
        System.out.println("Number of grid points with substantially different value function estimates: " + counter3);
    }
}
