package simpaths.model.decisions;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


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
            readwrite(grids.valueFunction, "read", DecisionParams.gridsInputDirectory, "value_function.uft");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }

        // read in consumption
        try {
            readwrite(grids.consumption, "read", DecisionParams.gridsInputDirectory, "consumption.uft");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }

        if (grids.employment1!=null) {
            // read in employment 1
            try {
                readwrite(grids.employment1, "read", DecisionParams.gridsInputDirectory, "employment1.uft");
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
                readwrite(grids.employment2, "read", DecisionParams.gridsInputDirectory, "employment2.uft");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
        }
    }


    /**
     * METHOD TO WRITE DATA TO GRIDS
     *
     * @param grids refers to the look-up table that stores IO solutions (the 'grids')
     *
     * THE MANAGER IS ACCESSED FROM ManagerPopulateGrids
     */
    public static void write(Grids grids) {
        write(grids, false);
    }
    public static void write(Grids grids, boolean flagIntermediate) {

        System.out.println("Saving optimised decisions");

        File grFile = new File(DecisionParams.gridsOutputDirectory);
        if (!grFile.exists()) grFile.mkdir();

        // write valueFunction
        try {
            readwrite(grids.valueFunction, "write", DecisionParams.gridsOutputDirectory, "value_function.uft");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }

        if (!flagIntermediate) {

            // write consumption
            try {
                readwrite(grids.consumption, "write", DecisionParams.gridsOutputDirectory, "consumption.uft");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }

            if (grids.employment1!=null) {
                // write employment 1
                try {
                    readwrite(grids.employment1, "write", DecisionParams.gridsOutputDirectory, "employment1.uft");
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
                    readwrite(grids.employment2, "write", DecisionParams.gridsOutputDirectory, "employment2.uft");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Throwable e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        }
    }


    /**
     * METHOD TO READ/WRITE BETWEEN A GRID OBJECT AND A SYSTEM FILE
     *
     * @param grid object to write to / read from
     * @param method string = "read" for reading, and write otherwise
     * @param directory directory of file to interact with
     * @param fileName name of file to interact with
     * @throws IOException exception encountered while executing read/write routine
     */
    public static void readwrite(Grid grid, String method, String directory, String fileName) throws IOException {

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
