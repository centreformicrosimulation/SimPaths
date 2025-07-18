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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RunSimPathsIntegrationTest {
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
        assertFileExists("input/EUROMODpolicySchedule.xlsx");
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

    @Test
    @DisplayName("Simulation runs successfully")
    @Order(4)
    void testVerifySimulationOutput() throws IOException {
        Path outputDir = Paths.get("output");

        Path latestOutputDir = Files.list(outputDir)
                .filter(Files::isDirectory)
                .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                .get();

        compareFiles(
            latestOutputDir.resolve("csv/Statistics1.csv"),
            Paths.get("src/test/java/simpaths/integrationtest/expected/Statistics1.csv")
        );
        compareFiles(
            latestOutputDir.resolve("csv/Statistics21.csv"),
            Paths.get("src/test/java/simpaths/integrationtest/expected/Statistics21.csv")
        );
        compareFiles(
            latestOutputDir.resolve("csv/Statistics31.csv"),
            Paths.get("src/test/java/simpaths/integrationtest/expected/Statistics31.csv")
        );
        compareFiles(
            latestOutputDir.resolve("csv/HealthStatistics1.csv"),
            Paths.get("src/test/java/simpaths/integrationtest/expected/HealthStatistics1.csv")
        );
    }

    void compareFiles(Path actualFile, Path expectedFile) throws IOException {
        assertTrue(Files.exists(actualFile), "Expected output file is missing: " + actualFile);
        assertEquals(-1, Files.mismatch(actualFile, expectedFile), fileMismatchMessage(actualFile, expectedFile));
    }

    String fileMismatchMessage(Path actualFile, Path expectedFile) throws IOException {
        List<String> actualLines = Files.readAllLines(actualFile);
        List<String> expectedLines = Files.readAllLines(expectedFile);
        int maxLines = Math.max(expectedLines.size(), actualLines.size());

        StringBuilder differences = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            String expectedLine = (i < expectedLines.size()) ? expectedLines.get(i) : "<MISSING>";
            String actualLine = (i < actualLines.size()) ? actualLines.get(i) : "<EXTRA>";

            if (!expectedLine.equals(actualLine)) {
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
