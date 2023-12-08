package simpaths.experiment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class SimPathsStartTest {

    private static final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private static final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @BeforeAll
    public static void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @BeforeEach
    public void resetStreams() {
        outContent.reset();
//        errContent.reset();
    }

    @Test
    public void testSimPathsStartHelpOption() {
        String[] args = {"-h"};
        SimPathsStart.main(args);

        String expectedHelpText = "SimPathsStart will start the SimPaths run";

        assertTrue(outContent.toString().contains(expectedHelpText));
        assertEquals("", errContent.toString().trim());
    }

    @Test
    public void testSimPathsStartHelpOptionWithOtherArguments() {
        String[] args = {"-g", "false", "-h", "-c", "UK"};
        SimPathsStart.main(args);

        String expectedHelpText = "SimPathsStart will start the SimPaths run";

        assertTrue(outContent.toString().contains(expectedHelpText));
        assertEquals("", errContent.toString().trim());
    }

    // Add more test methods for other scenarios as needed
}
