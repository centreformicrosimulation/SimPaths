package simpaths.model.decisions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import simpaths.data.Parameters;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * CLASS TO MANAGE INTERACTIONS BETWEEN THE IO GRIDS AND THE LOCAL FILE SYSTEM
 *
 */
public class ManagerFileGrids {


    /**
     * METHOD TO READ IN DATA TO GRIDS
     *
     * @param grids refers to the look-up table that stores IO solutions (the 'grids')
     *
     * THE MANAGER IS ACCESSED FROM ManagerPopulateGrids
     */
    public static void read(Grids grids) {

        System.out.println("Loading optimised decisions from disk");

        File grFile = new File(DecisionParams.gridsInputDirectory);
        if (!grFile.exists()) throw new RuntimeException("Directory to read in grids not found: " + DecisionParams.gridsInputDirectory);

        // read in valueFunction
        try {
            unformattedReadWrite(grids.valueFunction, "read", DecisionParams.gridsInputDirectory, "value_function.uft");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }

        // read in consumption
        try {
            unformattedReadWrite(grids.consumption, "read", DecisionParams.gridsInputDirectory, "consumption.uft");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }

        if (grids.employment1!=null) {
            // read in employment 1
            try {
                unformattedReadWrite(grids.employment1, "read", DecisionParams.gridsInputDirectory, "employment1.uft");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
        }

        if (grids.employment2!=null) {
            // read in employment 2
            try {
                unformattedReadWrite(grids.employment2, "read", DecisionParams.gridsInputDirectory, "employment2.uft");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
        }
    }


    /**
     * METHOD TO WRITE UNFORMATTED DATA TO FILE
     *
     * @param grids refers to the look-up table that stores IO solutions (the 'grids')
     *
     * THE MANAGER IS ACCESSED FROM ManagerPopulateGrids
     */
    public static void unformattedWrite(Grids grids) {
        unformattedWrite(grids, false);
    }
    public static void unformattedWrite(Grids grids, boolean flagIntermediate) {

        System.out.println("Saving optimised decisions");

        File grFile = new File(DecisionParams.gridsOutputDirectory);
        if (!grFile.exists()) grFile.mkdir();

        // write valueFunction
        try {
            unformattedReadWrite(grids.valueFunction, "write", DecisionParams.gridsOutputDirectory, "value_function.uft");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }

        // write consumption
        try {
            unformattedReadWrite(grids.consumption, "write", DecisionParams.gridsOutputDirectory, "consumption.uft");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }

        if (grids.employment1!=null) {
            // write employment 1
            try {
                unformattedReadWrite(grids.employment1, "write", DecisionParams.gridsOutputDirectory, "employment1.uft");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
        }

        if (grids.employment2!=null) {
            // write employment 2
            try {
                unformattedReadWrite(grids.employment2, "write", DecisionParams.gridsOutputDirectory, "employment2.uft");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public static void formattedWrite(Grids grids, int aa) {

        // set age specific working variables
        int innerDimension = (int)grids.scale.gridDimensions[aa][0];
        int outerDimension = (int)grids.scale.gridDimensions[aa][1];
        int ageYears = aa + Parameters.AGE_TO_BECOME_RESPONSIBLE;
        List<WriteGridsBean> beans = new ArrayList<WriteGridsBean>();
        for (int iiOuter=0; iiOuter<outerDimension; iiOuter++) {

            // identify current state combination for outer states
            States outerStates = new States(grids.scale, ageYears);
            outerStates.populateOuterGridStates(iiOuter);
            for (int iiInner = 0; iiInner < innerDimension; iiInner++) {

                // identify current state combination
                States currentStates = new States(outerStates);
                currentStates.populateInnerGridStates(iiInner);

                // instantiate writeBean
                WriteGridsBean bean = new WriteGridsBean();

                // populate with state combination
                bean.setCohabitation(currentStates.getCohabitationIndex());
                bean.setNk0(currentStates.getChildrenByBirthIndex(0));
                bean.setNk1(currentStates.getChildrenByBirthIndex(1));
                bean.setNk2(currentStates.getChildrenByBirthIndex(2));
                bean.setBirthYear(currentStates.getBirthYear());
                bean.setGender(currentStates.getGender());
                bean.setEducation(currentStates.getEducation());
                bean.setStudent(currentStates.getStudent());
                bean.setHealth(currentStates.getHealthVal());
                bean.setWageOffer(currentStates.getWageOffer());
                bean.setLiquidWealth(currentStates.getLiquidWealth());
                bean.setWagePotentialperHour(currentStates.getFullTimeHourlyEarningsPotential());
                bean.setPensionIncomePerYear(currentStates.getPensionPerYear());

                // populate with grid solutions
                bean.setValueFunction(grids.getValueFunction(currentStates));
                bean.setConsumptionShare(grids.getConsumptionShare(currentStates));
                bean.setEmployment1(grids.getEmployment1(currentStates));
                bean.setEmployment2(grids.getEmployment2(currentStates));

                // add to list
                beans.add(bean);
            }
        }

        // write output to csv file
        File dir = new File(DecisionParams.gridsOutputDirectory);
        if (!dir.exists()) dir.mkdir();
        String filePath = DecisionParams.gridsOutputDirectory + File.separator + "grid_age_" + ageYears + ".csv";
        String[] HEADERS = {"gender", "birthyear", "education", "student", "married", "children0", "children1", "children2", "health", "wealth", "wageperhour", "pensionperyear",
                "valuefunction", "consumptionshare", "employment1", "employment2"};
        try {
            BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath));
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(HEADERS).build();
            CSVPrinter printer = new CSVPrinter(writer, csvFormat);

            for (WriteGridsBean bean : beans) {
                List<String> record = new ArrayList<>();
                record.add(bean.getGenderString());
                record.add(bean.getBirthYearString());
                record.add(bean.getEducationString());
                record.add(bean.getStudentString());
                record.add(bean.getCohabitationString());
                record.add(bean.getNk0String());
                record.add(bean.getNk1String());
                record.add(bean.getNk2String());
                record.add(bean.getHealthString());
                record.add(bean.getWageOfferString());
                record.add(bean.getLiquidWealthString());
                record.add(bean.getWagePotentialperHourString());
                record.add(bean.getPensionIncomePerYearString());
                record.add(bean.getValueFunctionString());
                record.add(bean.getConsumptionShareString());
                record.add(bean.getEmployment1String());
                record.add(bean.getEmployment2String());
                printer.printRecord(record);
            }

            writer.flush();
            writer.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    /**
     * METHOD TO READ/WRITE UNFORMATTED DATA BETWEEN A GRID OBJECT AND A SYSTEM FILE
     *
     * @param grid object to write to / read from
     * @param method string = "read" for reading, and write otherwise
     * @param directory directory of file to interact with
     * @param fileName name of file to interact with
     * @throws IOException exception encountered while executing read/write routine
     */
    public static void unformattedReadWrite(Grid grid, String method, String directory, String fileName) throws IOException {

        // initialise file reference
        String filePath = directory + File.separator + fileName;
        if (method.equals("write")) {
            validateDirectory(directory);
            safeDelete(filePath);
        } else {
            if (!validateFileExists(filePath)) throw new IOException("file not found: " + filePath);
        }
        RandomAccessFile file;

        // set-up references for MappedByteBuffer
        final long MAX_BUFFER_BYTES = Integer.MAX_VALUE;
        long totalValsToRw = grid.size;
        int maxValsPerPartition = (int)((double)MAX_BUFFER_BYTES / (double)8);
        int numberOfPartitions = 1 + (int)(totalValsToRw / maxValsPerPartition);

        // loop over buffer partitions
        long position = 0;
        long valsThisPartition;
        for (int ii=0; ii<numberOfPartitions; ii++) {
            file = new RandomAccessFile(filePath, "rw");
            if (ii == numberOfPartitions-1) {
                valsThisPartition = totalValsToRw%maxValsPerPartition;
            } else {
                valsThisPartition = maxValsPerPartition;
            }
            FileChannel fileChannel = file.getChannel();
            MappedByteBuffer fileBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, position, 8 * valsThisPartition);
            for (long jj=(long)maxValsPerPartition*ii; jj<(long)maxValsPerPartition*ii+valsThisPartition; jj++) {
                if (method.equals("read")) {
                    grid.put(jj, fileBuffer.getDouble());
                } else {
                    fileBuffer.putDouble(grid.get(jj));
                }
            }
            position += 8 * valsThisPartition;
            fileChannel.close();
            safeClose(file);
        }
    }

    /**
     * METHOD TO CLOSE FILE
     * @param file File object to close
     */
    private static void safeClose(RandomAccessFile file) throws IOException {
        if (file != null) {
            file.close();
        }
    }

    /**
     * METHOD TO CLEAR ANY EXISTING FILE
     * @param file_path full path of file to delete if it exists
     */
    private static void safeDelete(String file_path) {
        File file = new File(file_path);
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * METHOD TO ENSURE THAT DIRECTORY EXISTS
     * @param directory full path of file to delete if it exists
     */
    private static void validateDirectory(String directory) {
        Path path = Paths.get(directory);
        if (!Files.isDirectory(path)) {
            new File(directory).mkdirs();
        }
    }

    /**
     * METHOD TO CLEAR ANY EXISTING FILE
     * @param file_path full path of file to delete if it exists
     * @return boolean true if file exists
     */
    private static boolean validateFileExists(String file_path) {
        File file = new File(file_path);
        return file.exists();
    }
}
