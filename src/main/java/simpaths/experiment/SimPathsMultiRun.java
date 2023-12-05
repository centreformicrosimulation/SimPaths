// define package
package simpaths.experiment;

// import Java packages
import org.apache.log4j.Level;
import org.apache.commons.cli.*;
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

		Option helpOption = new Option("h", "Print help message");
		options.addOption(helpOption);

		Option maxRunsOption = new Option("n", true, "Maximum number of runs");
		options.addOption(maxRunsOption);

		Option guiOption = new Option("g", true, "Show GUI [true/false]");
		options.addOption(guiOption);

		Option seedOption = new Option("r", true, "Random seed");
		options.addOption(seedOption);

		Option startYearOption = new Option("s", true, "Start year");
		options.addOption(startYearOption);

		Option endYearOption = new Option("e", true, "End year");
		options.addOption(endYearOption);

		Option popSizeOption = new Option("p", true, "Population size");
		options.addOption(popSizeOption);

		Option fileOption = new Option("f", "Output to file");
		options.addOption(fileOption);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();

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

	@Override
	public void buildExperiment(SimulationEngine engine) {
		SimPathsModel model = new SimPathsModel(Country.IT.getCountryFromNameString(countryString), startYear);
		model.setEndYear(endYear);
		model.setFirstRun(counter == 0);
//		SimPathsModel model = new SimPathsModel();
		setCountry(model);		//Set country based on input arguments.
		model.setPopSize(popSize);
		model.setRandomSeedIfFixed(randomSeed);
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
