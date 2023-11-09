// define package
package simpaths.experiment;

// import Java packages
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

	private static int maxNumberOfRuns = 3;

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

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-n")){ // These options are for use from the command line
				
				try {
					maxNumberOfRuns = Integer.parseInt(args[i + 1]);
			    } catch (NumberFormatException e) {
			        System.err.println("Argument " + args[i + 1] + " must be an integer reflecting the maximum number of runs.");
			        System.exit(1);
			    }
				
				i++;
			}
			else if (args[i].equals("-g")){				//Set show GUI
				executeWithGui = Boolean.parseBoolean(args[i + 1]);
				i++;
			}
			else if (args[i].equals("-c")){				//Set country by arguments here
				countryString = args[i+1];				
				i++;
			}
			else if (args[i].equals("-r")){				//Set random seed
				randomSeed = Long.parseLong(args[i+1]);
				i++;
			}
			else if (args[i].equals("-s")) {			//Set start year
				startYear = Integer.parseInt(args[i + 1]);
				i++;
			}
			else if (args[i].equals("-e")) {			//Set end year
				endYear = Integer.parseInt(args[i + 1]);
				i++;
			}
			else if (args[i].equals("-p")){				//Set population size
				popSize = Integer.parseInt(args[i+1]);
				i++;
			}
			else if (args[i].equals("-f")){             //Output to file
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
					Logger.getRootLogger().addAppender(appender);
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
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

	@Override
	public void buildExperiment(SimulationEngine engine) {
		SimPathsModel model = new SimPathsModel(Country.IT.getCountryFromNameString(countryString), startYear);
		model.setEndYear(endYear);
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
