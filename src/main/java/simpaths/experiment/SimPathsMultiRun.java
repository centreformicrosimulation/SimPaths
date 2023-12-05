// define package
package simpaths.experiment;

// import Java packages
import org.apache.log4j.Level;
import org.apache.commons.cli.*;
import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import simpaths.data.Parameters;
import simpaths.model.SimPathsModel;
import microsim.data.MultiKeyCoefficientMap;
import microsim.data.excel.ExcelAssistant;
import microsim.engine.MultiRun;
import microsim.engine.SimulationEngine;
import microsim.gui.shell.MultiRunFrame;
import simpaths.model.enums.Country;

// Logging and file writing
import simpaths.model.SimPathsModel;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import java.io.*;

public class SimPathsMultiRun extends MultiRun {

	public static boolean executeWithGui = true;
	private static int maxNumberOfRuns = 25;
	private static String countryString;
	private static int startYear;
	private static int endYear = 2020;

	private Long counter = 0L;

	private static Integer popSize = 25000;
	
	private static Long randomSeed = 615L;

	public static Logger log = Logger.getLogger(SimPathsMultiRun.class);

	private static Map<String, Object> model_args;

	/**
	 *
	 * 	MAIN PROGRAM ENTRY FOR MULTI-SIMULATION
	 *
	 */
	public static void main(String[] args) {

		//Adjust the country and year to the value read from Excel, which is updated when the database is rebuilt. Otherwise it will set the country and year to the last one used to build the database
		MultiKeyCoefficientMap lastDatabaseCountryAndYear = ExcelAssistant.loadCoefficientMap("input" + File.separator + Parameters.DatabaseCountryYearFilename + ".xlsx", "Data", 1, 1);
		if (lastDatabaseCountryAndYear.keySet().stream().anyMatch(key -> key.toString().equals("MultiKey[IT]"))) {
			countryString = "Italy";
		} else {
			countryString = "United Kingdom";
		}
		String valueYear = lastDatabaseCountryAndYear.getValue(Country.UK.getCountryFromNameString(countryString).toString()).toString();
		startYear = Integer.parseInt(valueYear);

		parseYamlConfig(args);

		// Parse command line arguments to override defaults
		if (!parseCommandLineArgs(args)) {
			// If parseCommandLineArgs returns false (indicating help option is provided), exit main
			return;
		}

		log.info("Starting run with seed = " + randomSeed);
		
		SimulationEngine engine = SimulationEngine.getInstance();
		
		SimPathsMultiRun experimentBuilder = new SimPathsMultiRun();
//		engine.setBuilderClass(SimPathsMultiRun.class);			//This works but is deprecated
		engine.setExperimentBuilder(experimentBuilder);					//This replaces the above line... but does it work?
		engine.setup();													//Do we need this?  Worked fine without it...

		if (executeWithGui)
			new MultiRunFrame(experimentBuilder, "SimPaths MultiRun", maxNumberOfRuns);
		else
			experimentBuilder.start();
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

		Option maxRunsOption = new Option("n", "maxNumberOfRuns", true, "Maximum number of runs");
		maxRunsOption.setArgName("int");
		options.addOption(maxRunsOption);

		Option seedOption = new Option("r", "randomSeed", true, "Random seed");
		seedOption.setArgName("int");
		options.addOption(seedOption);

		Option guiOption = new Option("g", "executeWithGui", true, "Show GUI");
		guiOption.setArgName("true/false");
		options.addOption(guiOption);

		Option fileOption = new Option("f", "Output to file");
		options.addOption(fileOption);

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

			if (cmd.hasOption("p")) {
				popSize = Integer.parseInt(cmd.getOptionValue("p"));
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
			System.exit(1);
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

	private static void parseYamlConfig(String[] args) {
		String configFile = "config.yml";  // Default config file name

		// Check if an alternative config file is specified in the command line
		for (int i = 0; i < args.length - 1; i++) {
			if (args[i].equals("-config")) {
				configFile = args[i + 1];
				break;
			}
		}

		// Parse YAML config file and update parameters
		try {
			Yaml yaml = new Yaml();
			FileInputStream inputStream = new FileInputStream(configFile);
			Map<String, Object> config = yaml.load(inputStream);

			// Update parameters from the config file
			for (Map.Entry<String, Object> entry : config.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				if ("model_args".equals(key)) {
					model_args = (Map<String, Object>) value;
					continue;
				}

				// Use reflection to dynamically set the field based on the key
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

		} catch (FileNotFoundException e) {
			// Config file not found, continue with defaults
		}
	}

	public static void updateModelParameters(SimPathsModel model, Map<String, Object> model_args) {

		for (Map.Entry<String, Object> entry : model_args.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			try {
				Field field = SimPathsModel.class.getDeclaredField(key);
				field.setAccessible(true);

				// Determine the field type
				Class<?> fieldType = field.getType();

				// Convert the YAML value to the field type
				Object convertedValue = convertToType(value, fieldType);

				// Set the field value
				field.set(model, convertedValue);

				field.setAccessible(false);
			} catch (NoSuchFieldException | IllegalAccessException e) {
				// Handle exceptions if the field is not found or inaccessible
				e.printStackTrace();
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
		SimPathsModel model = new SimPathsModel(Country.IT.getCountryFromNameString(countryString), startYear);
		model.setEndYear(endYear);
		model.setFirstRun(counter == 0);
//		SimPathsModel model = new SimPathsModel();
		setCountry(model);		//Set country based on input arguments.
		model.setPopSize(popSize);
		model.setRandomSeedIfFixed(randomSeed);

		if (model_args != null) updateModelParameters(model, model_args);

		engine.addSimulationManager(model);
		
		SimPathsCollector collector = new SimPathsCollector(model);
		engine.addSimulationManager(collector);

		model.setCollector(collector);

//		SimPathsObserver observer = new SimPathsObserver(model, collector);		//Not needed for MultiRun?
//		engine.addSimulationManager(observer);

		
	}

	private void setCountry(SimPathsModel model) {
		if(countryString.equalsIgnoreCase("Italy")) {
			model.setCountry(Country.IT);
		}
		else if(countryString.equalsIgnoreCase("United Kingdom")) {
			model.setCountry(Country.UK);
		}
		else throw new RuntimeException("countryString is not set to an appropriate string!");
	}
	
	@Override
	public boolean nextModel() {
		randomSeed++;
		counter++;
		System.out.println("Random seed " + randomSeed);

		if(counter < maxNumberOfRuns) {
			return true;
		}
		else return false;
	}

	@Override
	public String setupRunLabel() {
		return randomSeed.toString();
	}

}
