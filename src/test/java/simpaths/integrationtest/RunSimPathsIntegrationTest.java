package simpaths.integrationtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RunSimPathsIntegrationTest {
    /** Absolute epsilon for numeric comparison. */
    private static final double ABS_EPSILON = 1e-9;
    /** Relative epsilon for numeric comparison. */
    private static final double REL_EPSILON = 1e-6;

    @Test
    @DisplayName("Initial database setup runs successfully")
    @Order(1)
    void testRunSetup() {
        runCommand(
                "java", "-jar", "multirun.jar", "-DBSetup", "-config", "test_create_database.yml"
        );
    }

    @Test
    @DisplayName("Database and configuration files are created")
    @Order(2)
    void testVerifySetupOutput() {
        assertFileExists("input/input.mv.db");
        assertFileExists("input/EUROMOD/EUROMODpolicySchedule.xlsx");
        assertFileExists("input/DatabaseCountryYear.xlsx");
    }

    @Test
    @DisplayName("Simulation runs successfully")
    @Order(3)
    void testRunSimulation() {
        runCommand(
            "java", "-jar", "multirun.jar", "-config", "test_run.yml", "-P", "root"
        );
    }

    @Nested
    @DisplayName("Simulation runs successfully")
    @Order(4)
    class testVerifySimulationOutput {

        public static Path latestOutputDir;

        @BeforeAll
        public static void loadResults() throws IOException {

        Path outputDir = Paths.get("output");

        latestOutputDir = Files.list(outputDir)
                .filter(Files::isDirectory)
                .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                .get();
        }

        @Test
        public void compareWealthIncomeStatistics() throws IOException {
            compareFiles(
                    latestOutputDir.resolve("csv/WealthIncomeStatistics.csv"),
                    Paths.get("src/test/java/simpaths/integrationtest/expected/WealthIncomeStatistics.csv")
            );
        }
        @Test
        public void compareDemographicStatistics() throws IOException {
        compareFiles(
            latestOutputDir.resolve("csv/DemographicStatistics.csv"),
            Paths.get("src/test/java/simpaths/integrationtest/expected/DemographicStatistics.csv")
        );
        }
        @Test
        public void verifyAlignmentStatisticsExported() {
            assertTrue(
                    Files.exists(latestOutputDir.resolve("csv/AlignmentStatistics.csv")),
                    "Expected output file is missing: " + latestOutputDir.resolve("csv/AlignmentStatistics.csv")
            );
        }
        @Test
        public void compareWellbeingByGender() throws IOException {
            compareFiles(
                    latestOutputDir.resolve("csv/WellbeingByGender.csv"),
                    Paths.get("src/test/java/simpaths/integrationtest/expected/WellbeingByGender.csv")
            );
        }
        @Test
        public void compareHealthStatistics() throws IOException {
            compareFiles(
                    latestOutputDir.resolve("csv/HealthStatistics.csv"),
                    Paths.get("src/test/java/simpaths/integrationtest/expected/HealthStatistics.csv")
            );
        }
        @Test
        public void compareLabourStatistics() throws IOException {
            compareFiles(
                    latestOutputDir.resolve("csv/LabourStatistics.csv"),
                    Paths.get("src/test/java/simpaths/integrationtest/expected/LabourStatistics.csv")
            );
        }
    }

    void compareFiles(Path actualFile, Path expectedFile) throws IOException {
        assertTrue(Files.exists(actualFile), "Expected output file is missing: " + actualFile);
        assertTrue(filesMatchWithTolerance(actualFile, expectedFile), fileMismatchMessage(actualFile, expectedFile));
    }

    String fileMismatchMessage(Path actualFile, Path expectedFile) throws IOException {
        List<String> actualLines = Files.readAllLines(actualFile);
        List<String> expectedLines = Files.readAllLines(expectedFile);
        int maxLines = Math.max(expectedLines.size(), actualLines.size());

        StringBuilder differences = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            String expectedLine = (i < expectedLines.size()) ? expectedLines.get(i) : "<MISSING>";
            String actualLine = (i < actualLines.size()) ? actualLines.get(i) : "<EXTRA>";

            if (!linesMatchWithTolerance(expectedLine, actualLine)) {
                differences.append(String.format("""
                    Line %d:
                    Expected: %s
                    Actual  : %s
                    """,
                            i + 1, expectedLine, actualLine));
            }
        }

        return String.format("""

            The actual output from the integration test does not match the expected output.

            Actual output file  : %s
            Expected output file: %s

            Differences:

            %s
            IF THIS IS EXPECTED - for example, if you have changed substantive processes within the model
            or the structure of the output, please:

            1. Verify that the output is correct and as expected.
            2. Replace the expected output file with the new output file:
                cp %s %s
            3. Commit this change to Git, so that the changes are visible in your pull request and this test passes.

            """, actualFile, expectedFile, differences, actualFile, expectedFile);
    }

    private boolean filesMatchWithTolerance(Path actualFile, Path expectedFile) throws IOException {
        List<String> actualLines = Files.readAllLines(actualFile);
        List<String> expectedLines = Files.readAllLines(expectedFile);

        if (actualLines.size() != expectedLines.size()) {
            return false;
        }

        for (int i = 0; i < expectedLines.size(); i++) {
            if (!linesMatchWithTolerance(expectedLines.get(i), actualLines.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean linesMatchWithTolerance(String expectedLine, String actualLine) {
        String[] expectedTokens = expectedLine.split(",", -1);
        String[] actualTokens = actualLine.split(",", -1);

        if (expectedTokens.length != actualTokens.length) {
            return false;
        }

        for (int i = 0; i < expectedTokens.length; i++) {
            if (!tokensMatchWithTolerance(expectedTokens[i], actualTokens[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean tokensMatchWithTolerance(String expectedToken, String actualToken) {
        String expectedTrimmed = expectedToken.trim();
        String actualTrimmed = actualToken.trim();

        Double expectedNumber = tryParseDouble(expectedTrimmed);
        Double actualNumber = tryParseDouble(actualTrimmed);

        if (expectedNumber != null && actualNumber != null) {
            double e = expectedNumber;
            double a = actualNumber;
            if (Double.isNaN(e) && Double.isNaN(a)) {
                return true;
            }
            if (Double.isNaN(e) || Double.isNaN(a)) {
                return false;
            }
            if (Double.isInfinite(e) || Double.isInfinite(a)) {
                return Double.compare(e, a) == 0;
            }
            double diff = Math.abs(e - a);
            double tolerance = Math.max(ABS_EPSILON, REL_EPSILON * Math.max(Math.abs(e), Math.abs(a)));
            return diff <= tolerance;
        }

        return expectedToken.equals(actualToken);
    }

    private Double tryParseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void runCommand(String... args) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(args);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line); // Log output to console when running in Maven
                }
            }
            int exitCode = process.waitFor();

            assertEquals(0, exitCode, "Process exited with error code: " + exitCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to run: " + e.getMessage(), e);
        }
    }

    private void assertFileExists(String path) {
        assertTrue(new File(path).exists(), "Missing file " + path);
    }
}
