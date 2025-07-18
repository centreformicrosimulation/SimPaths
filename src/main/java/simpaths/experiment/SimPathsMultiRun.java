// define package
package simpaths.experiment;

// import Java packages
import org.apache.log4j.Level;
import org.apache.commons.cli.*;
import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.Map;

import simpaths.data.Parameters;
import simpaths.data.XLSXfileWriter;
import simpaths.model.SimPathsModel;
import microsim.data.MultiKeyCoefficientMap;
import microsim.data.excel.ExcelAssistant;
import microsim.engine.MultiRun;
import microsim.engine.SimulationEngine;
import microsim.gui.shell.MultiRunFrame;
import simpaths.model.enums.Country;

// Logging and file writing
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import java.io.*;


public class SimPathsMultiRun extends MultiRun {

	// command line args
	private static String countryString;
	private static Integer popSize = 25000;
	private static int startYear;
	private static int endYear = 2020;
	private static int maxNumberOfRuns = 25;
	private static Long randomSeed = 615L;
	public static boolean executeWithGui = true;

	// innovation args
	private static boolean randomSeedInnov = true;
	private static boolean intertemporalElasticityInnov = false;
	private static boolean labourSupplyElasticityInnov = false;
	private static boolean flagDatabaseSetup = false;

	// passing args for config file
	private static Map<String, Object> modelArgs;
	private static Map<String, Object> innovationArgs;
	private static Map<String, Object> collectorArgs;
	private static Map<String, Object> parameterArgs;
	public static String configFile = "default.yml";

	// other working variables
	private static Country country;
	private static double interestRateInnov = 0.0;
	private static double disposableIncomeFromLabourInnov = 0.0;
	private Long counter = 0L;
	public static Logger log = Logger.getLogger(SimPathsMultiRun.class);

	private static boolean persist_population;
	private static boolean persist_root;

	/**
	 *
	 * 	MAIN PROGRAM ENTRY FOR MULTI-SIMULATION
	 *
	 */
	public static void main(String[] args) {

		// process Yaml config file
		if (!parseYamlConfig(args)) {
			// if parseYamlConfig returns false (indicating bad filename passed), exit main
			return;
		}
		
		if (parameterArgs != null)
			updateParameters(parameterArgs);


		// set default values for country and start year
		MultiKeyCoefficientMap lastDatabaseCountryAndYear = ExcelAssistant.loadCoefficientMap("input" + File.separator + Parameters.DatabaseCountryYearFilename + ".xlsx", "Data", 1, 1);
		if (lastDatabaseCountryAndYear.keySet().stream().anyMatch(key -> key.toString().equals("MultiKey[IT]"))) {
			countryString = "Italy";
		} else {
			countryString = "United Kingdom";
		}
		country = Country.getCountryFromNameString(countryString);
		String valueYear = lastDatabaseCountryAndYear.getValue(country.toString()).toString();
		startYear = Integer.parseInt(valueYear);

		if (innovationArgs!=null)
			updateLocalParameters(innovationArgs);

		parseYamlConfig(args);

		// Parse command line arguments to override defaults
		if (!parseCommandLineArgs(args)) {
			// If parseCommandLineArgs returns false (indicating help option is provided), exit main
			return;
		}
		country = Country.getCountryFromNameString(countryString);

		//Save the last selected country and year to Excel to use in the model
		String[] columnNames = {"Country", "Year"};
		Object[][] data = new Object[1][columnNames.length];
		data[0][0] = country.toString();
		data[0][1] = startYear;
		XLSXfileWriter.createXLSX(Parameters.INPUT_DIRECTORY, Parameters.DatabaseCountryYearFilename, "Data", columnNames, data);

		if (flagDatabaseSetup) {

			Parameters.databaseSetup(country, executeWithGui, startYear);
		} else {
			// standard simulation

			log.info("Starting run with seed = " + randomSeed);

			SimulationEngine engine = SimulationEngine.getInstance();

			SimPathsMultiRun experimentBuilder = new SimPathsMultiRun();
			engine.setExperimentBuilder(experimentBuilder);
			engine.setup();		//This is needed to update model attributes (from model_args in config file)

			if (executeWithGui)
				new MultiRunFrame(experimentBuilder, "SimPaths MultiRun", maxNumberOfRuns);
			else
				experimentBuilder.start();
		}
	}

	private static boolean parseCommandLineArgs(String[] args) {

		Options options = new Options();

		Option popSizeOption = new Option("p", "popSize", true, "Population size");
		popSizeOption.setArgName("int");
		options.addOption(popSizeOption);

		Option startYearOption = new Option("s", "startYear", true, "Start year");
		startYearOption.setArgName("year");
		options.addOption(startYearOption);

		Option endYearOption = new Option("e", "endYear",true, "End year");
		endYearOption.setArgName("year");
		options.addOption(endYearOption);

		Option setupOption = new Option("DBSetup", "Setup only");
		options.addOption(setupOption);

		Option maxRunsOption = new Option("n", "maxNumberOfRuns", true, "Maximum number of runs");
		maxRunsOption.setArgName("int");
		options.addOption(maxRunsOption);

		Option seedOption = new Option("r", "randomSeed", true, "Random seed");
		seedOption.setArgName("int");
		options.addOption(seedOption);

		Option guiOption = new Option("g", "executeWithGui", true, "Show GUI");
		guiOption.setArgName("true/false");
		options.addOption(guiOption);

		Option configOption = new Option("config", true, "Specify custom config file (default: default.yml)");
		configOption.setArgName("file");
		options.addOption(configOption);

		Option fileOption = new Option("f", "Output to file");
		options.addOption(fileOption);

		Option persistRoot = new Option("P", "persist", true,
				"Write and read processed database to root or run-specific database. Accepted arguments:" +
				"\n - root: persist to root output folder (input/)" +
				"\n - run: persist to run output folder (output/[yyyymmdd_seed]/input/)" +
				"\n - none: do not write/read processed dataset.\n" +
				"(default: `run` - multirun copy in output folder)");
		persistRoot.setArgName("persist");
		options.addOption(persistRoot);

		Option helpOption = new Option("h", "help", false, "Print this help message");
		options.addOption(helpOption);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);

		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption("h")) {
				printHelpMessage(formatter, options);
				return false; // Exit without reporting an error
			}
			if (cmd.hasOption("n")) {
				maxNumberOfRuns = Integer.parseInt(cmd.getOptionValue("n"));
			}

			if (cmd.hasOption("g")) {
				executeWithGui = Boolean.parseBoolean(cmd.getOptionValue("g"));
			}

			if (cmd.hasOption("r")) {
				randomSeed = Long.parseLong(cmd.getOptionValue("r"));
			}

			if (cmd.hasOption("s")) {
				startYear = Integer.parseInt(cmd.getOptionValue("s"));
			}

			if (cmd.hasOption("e")) {
				endYear = Integer.parseInt(cmd.getOptionValue("e"));
			}

			if (cmd.hasOption("DBSetup")) {
				flagDatabaseSetup = true;
			}

			if (cmd.hasOption("p")) {
				popSize = Integer.parseInt(cmd.getOptionValue("p"));
			}

				switch (cmd.getOptionValue("P", "run")) {
					case "root":
						log.info("Persisting processed data to root folder");
						persist_population = true;
						persist_root = true;
						break;
					case "run":
						log.info("Persisting processed data to run folder");
						persist_population = true;
						persist_root = false;
						break;
					case "none":
						log.info("Not persisting processed data");
						persist_population = false;
						persist_root = false;
						break;
					default:
						System.out.println("Persist option `" + cmd.getOptionValue("P") + "` not recognised. Valid values: `none`, `root`, `run`. Persisting processed data to run folder");
						persist_population = true;
						persist_root = false;
				}

			if (cmd.hasOption("f")) {
				try {
					File logDir = new File("output/logs");
					if (!logDir.exists()) {
						logDir.mkdirs();
					}
					// Writing console outputs to `run_[seed].txt
					System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(logDir.getPath() + "/run_" + randomSeed + ".txt")), true));

					// Writing logs to `run_[seed].log`
					FileAppender appender = new FileAppender();
					appender.setName("Run logging");
					appender.setFile(logDir.getPath() + "/run_" + randomSeed + ".log");
					appender.setAppend(false);
					appender.setLayout(new PatternLayout("%d{yyyy MMM dd HH:mm:ss} - %m%n"));
					appender.activateOptions();
					Logger.getRootLogger().setLevel(Level.DEBUG);
					Logger.getRootLogger().addAppender(appender);
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		} catch (ParseException e) {
			System.err.println("Error parsing command line arguments: " + e.getMessage());
			formatter.printHelp("SimPathsMultiRun", options);
			return false;
		}

		return true;
	}

	private static void printHelpMessage(HelpFormatter formatter, Options options) {
		String header = "SimPathsMultiRun can run multiple sequential runs, " +
				"resetting the population to the start year and iterating from the start seed. " +
				"It takes the following options:";
		String footer = "When running with no display, `-g` must be set to `false`.";
		formatter.printHelp("SimPathsMultiRun", header, options, footer, true);
	}

	private static boolean parseYamlConfig(String[] args) {

		boolean customConfig = false;

		// Check if an alternative config file is specified in the command line
		for (int i = 0; i < args.length - 1; i++) {
			if (args[i].equals("-config")) {
				configFile = args[i + 1];
				customConfig = true;
				break;
			}
		}

		// Parse YAML config file and update parameters
		try {
			Yaml yaml = new Yaml();
			String configFilePath = "config" + File.separator + configFile;
			FileInputStream inputStream = new FileInputStream(configFilePath);
			Map<String, Object> config = yaml.load(inputStream);

			// Update parameters from the config file
			for (Map.Entry<String, Object> entry : config.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				if ("model_args".equals(key)) {
					modelArgs = (Map<String, Object>) value;
					continue;
				}

				if ("innovation_args".equals(key)) {
					innovationArgs = (Map<String, Object>) value;
					continue;
				}

				if ("collector_args".equals(key)) {
					collectorArgs = (Map<String, Object>) value;
					continue;
				}

				// Read in parameter arguments - to be handled differently as no Parameters object
				if ("parameter_args".equals(key)) {
					parameterArgs = (Map<String, Object>) value;
					continue;
				}

				// Use reflection to dynamically set the field based on the key
				updateLocalParameters(key, value);
			}

		} catch (FileNotFoundException e) {
			// Config file specified but not found, continue with defaults
			if (customConfig) {
				System.err.println("Config file " + configFile + " not found; please supply a valid config file.");
				return false;
			}
		}
		return true;
	}

	public static void updateLocalParameters(String key, Object value) {
		try {
			Field field = SimPathsMultiRun.class.getDeclaredField(key);
			field.setAccessible(true);

			// Determine the field type
			Class<?> fieldType = field.getType();

			// Convert the YAML value to the field type
			Object convertedValue = convertToType(value, fieldType);

			// Set the field value
			field.set(null, convertedValue);

			field.setAccessible(false);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			// Handle exceptions if the field is not found or inaccessible
			e.printStackTrace();
		}
	}

	public static void updateLocalParameters(Map<String, Object> args) {

		for (Map.Entry<String, Object> entry : args.entrySet()) {
			updateLocalParameters(entry.getKey(), entry.getValue());
		}
	}

	public static void updateParameters(Object object, Map<String, Object> args) {

		for (Map.Entry<String, Object> entry : args.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			try {
				Field field = object.getClass().getDeclaredField(key);
				field.setAccessible(true);

				// Determine the field type
				Class<?> fieldType = field.getType();

				// Convert the YAML value to the field type
				Object convertedValue = convertToType(value, fieldType);

				// Set the field value
				field.set(object, convertedValue);

				field.setAccessible(false);
			} catch (NoSuchFieldException | IllegalAccessException e) {
				// Handle exceptions if the field is not found or inaccessible
				e.printStackTrace();
			}
		}
	}

	// Specifically for updating parameters when no object called - i.e. Parameters.java
	public static void updateParameters(Map<String, Object> parameter_args) {

		for (Map.Entry<String, Object> entry : parameter_args.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			switch (key) {
				default:
					try {
						Field field = Parameters.class.getDeclaredField(key);
						field.setAccessible(true);

						// Determine the field type
						Class<?> fieldType = field.getType();

						// Convert the YAML value to the field type
						Object convertedValue = convertToType(value, fieldType);

						// Set the field value
						field.set(Parameters.class, convertedValue);

						field.setAccessible(false);
					} catch (NoSuchFieldException | IllegalAccessException e) {
						// Handle exceptions if the field is not found or inaccessible
						e.printStackTrace();
					}

			}

		}

	}

	private static Object convertToType(Object value, Class<?> targetType) {
		// Convert the YAML value to the target type
		if (int.class.equals(targetType)) {
			return ((Number) value).intValue();
		} else if (Integer.class.equals(targetType)) {
			return Integer.parseInt(value.toString());
		} else if (long.class.equals(targetType) || Long.class.equals(targetType)) {
			return ((Number) value).longValue();
		} else if (boolean.class.equals(targetType) || Boolean.class.equals(targetType)) {
			return Boolean.parseBoolean(value.toString());
		} else if (double.class.equals(targetType)) {
			return ((Number) value).doubleValue();
		} else if (Double.class.equals(targetType)) {
			return Double.parseDouble(value.toString());
		} else {
			// If it's none of the known types, return the value as is
			return value;
		}
	}

	@Override
	public void buildExperiment(SimulationEngine engine) {

		SimPathsModel model = new SimPathsModel(Country.getCountryFromNameString(countryString), startYear);
		if (persist_population) model.setPersistPopulation(true);
		if (persist_root) model.setPersistDatabasePath("./input/input");
		updateLocalParameters(model);
		if (modelArgs != null)
			updateParameters(model, modelArgs);

		engine.addSimulationManager(model);

		SimPathsCollector collector = new SimPathsCollector(model);
		if (collectorArgs != null)
			updateParameters(collector, collectorArgs);
		engine.addSimulationManager(collector);
		model.setCollector(collector);

	}

	private void updateLocalParameters(SimPathsModel model) {
		model.setEndYear(endYear);
		model.setFirstRun(counter == 0);
//		SimPathsModel model = new SimPathsModel();
		model.setPopSize(popSize);
		model.setRandomSeedIfFixed(randomSeed);
		model.setInterestRateInnov(interestRateInnov);
		model.setDisposableIncomeFromLabourInnov(disposableIncomeFromLabourInnov);
	}

	private void iterateParameters(Long counter) {

		if (randomSeedInnov) {
			randomSeed++;
			System.out.println("Random seed " + randomSeed);
		}
		if (intertemporalElasticityInnov) {
			if (counter==1)
				interestRateInnov = 0.0075;
			else if (counter==2)
				interestRateInnov = -0.0075;
		}
		if (labourSupplyElasticityInnov) {
			if (counter==1)
				disposableIncomeFromLabourInnov = 0.01;
			else if (counter==2)
				disposableIncomeFromLabourInnov = -0.01;
		}
	}
	
	@Override
	public boolean nextModel() {
		counter++;
		if (counter < maxNumberOfRuns) {
			iterateParameters(counter);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String setupRunLabel() {
		return randomSeed.toString() + "_" + counter.toString();
	}
}
