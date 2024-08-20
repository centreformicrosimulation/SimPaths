// define package
package simpaths.experiment;

// import Java packages
import java.awt.Dimension;
import org.apache.commons.cli.*;
import java.awt.Toolkit;
import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JInternalFrame;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.JPanel;

// third-party packages
import simpaths.data.startingpop.DataParser;
import simpaths.model.SimPathsModel;
import org.apache.commons.io.FileUtils;

// import JAS-mine packages
import microsim.data.MultiKeyCoefficientMap;
import microsim.data.excel.ExcelAssistant;
import microsim.engine.ExperimentBuilder;
import microsim.engine.SimulationEngine;
import microsim.gui.shell.MicrosimShell;

// import SimPaths packages
import simpaths.model.enums.Country;
import simpaths.data.*;
import simpaths.model.taxes.database.TaxDonorDataParser;


/**
 *
 * 	CLASS FOR SINGLE SIMULATION EXECUTION
 *
 */
public class SimPathsStart implements ExperimentBuilder {

	// default simulation parameters
	private static Country country = Country.UK;
	private static int startYear = Parameters.getMaxStartYear();

	private static boolean showGui = true;  // Show GUI by default

	private static boolean setupOnly = false;

	private static boolean rewritePolicySchedule = false;


	/**
	 *
	 * 	MAIN class for simulation entry
	 *
	 */
	public static void main(String[] args) {


		if (!parseCommandLineArgs(args)) {
			// If parseCommandLineArgs returns false (indicating help option is provided), exit main
			return;
		}

		if (showGui) {
			// display dialog box to allow users to define desired simulation
			runGUIdialog();
		} else {
			try {
				runGUIlessSetup(4);
			} catch (FileNotFoundException f) {
				System.err.println(f.getMessage());
			};
		}

		if (setupOnly) {
			System.out.println("Setup complete, exiting.");
			return;
		}

		//Adjust the country and year to the value read from Excel, which is updated when the database is rebuilt. Otherwise it will set the country and year to the last one used to build the database
		MultiKeyCoefficientMap lastDatabaseCountryAndYear = ExcelAssistant.loadCoefficientMap("input" + File.separator + Parameters.DatabaseCountryYearFilename + ".xlsx", "Data", 1, 1);
		if (lastDatabaseCountryAndYear.keySet().stream().anyMatch(key -> key.toString().equals("MultiKey[IT]"))) {
			country = Country.IT;
		} else {
			country = Country.UK;
//			country = Country.IT;
		}
		String valueYear = lastDatabaseCountryAndYear.getValue(country.toString()).toString();
		startYear = Integer.parseInt(valueYear);

		// start the JAS-mine simulation engine
		final SimulationEngine engine = SimulationEngine.getInstance();
		MicrosimShell gui = null;
		if (showGui) {
			gui = new MicrosimShell(engine);
			gui.setVisible(true);
		}
		SimPathsStart experimentBuilder = new SimPathsStart();
		engine.setExperimentBuilder(experimentBuilder);
		engine.setup();
	}

	private static boolean parseCommandLineArgs(String[] args) {
		Options options = new Options();

		Option countryOption = new Option("c", "country", true, "Country (by country code CC, e.g. 'UK'/'IT')");
		countryOption.setArgName("CC");
		options.addOption(countryOption);

		Option startYearOption = new Option("s", "startYear", true, "Start year");
		startYearOption.setArgName("year");
		options.addOption(startYearOption);

		Option setupOption = new Option("Setup", "Setup only");
		options.addOption(setupOption);

		Option rewritePolicyScheduleOption = new Option("r", "rewrite-policy-schedule",false, "Re-write policy schedule from detected policy files");
		options.addOption(rewritePolicyScheduleOption);

		Option guiOption = new Option("g", "showGui", true, "Show GUI");
		guiOption.setArgName("true/false");
		options.addOption(guiOption);

		Option helpOption = new Option("h", "help", false, "Print help message");
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

			if (cmd.hasOption("g")) {
				showGui = Boolean.parseBoolean(cmd.getOptionValue("g"));
			}

			if (cmd.hasOption("c")) {
				try {
					country = Country.valueOf(cmd.getOptionValue("c"));
				} catch (Exception e) {
					throw new IllegalArgumentException("Code '" + cmd.getOptionValue("c") + "' not a valid country.");
				}
			}

			if (cmd.hasOption("s")) {
				startYear = Integer.parseInt(cmd.getOptionValue("s"));
			}

			if (cmd.hasOption("Setup")) {
				setupOnly = true;
			}

			if (cmd.hasOption("r")) {
				rewritePolicySchedule = true;
			}
		} catch (ParseException | IllegalArgumentException e) {
			System.err.println("Error parsing command line arguments: " + e.getMessage());
			formatter.printHelp("SimPathsStart", options);
			return false;
		}

		return true;
	}

	private static void printHelpMessage(HelpFormatter formatter, Options options) {
		String header = "SimPathsStart will start the SimPaths run. " +
				"When using the argument `Setup`, this will create the population database " +
				"and exit before starting the first run. " +
				"It takes the following options:";
		String footer = "When running with no display, `-g` must be set to `false`.";
		formatter.printHelp("SimPathsStart", header, options, footer, true);
	}


	/**
	 *
	 * METHOD TO START SELECTED EXPERIMENT
	 * ROUTED FROM JAS-mine 'engine.setup()'
	 * @param engine
	 *
	 */
	@Override
	public void buildExperiment(SimulationEngine engine) {

		// instantiate simulation processes
		SimPathsModel model = new SimPathsModel(country, startYear);
		SimPathsCollector collector = new SimPathsCollector(model);
		SimPathsObserver observer = new SimPathsObserver(model, collector);

		engine.addSimulationManager(model);
		engine.addSimulationManager(collector);
		engine.addSimulationManager(observer);

		model.setCollector(collector);
	}

	private static void runGUIlessSetup(int option) throws FileNotFoundException {

		// Detect if data available; set to testing data if not
		Collection<File> testList = FileUtils.listFiles(new File(Parameters.getInputDirectoryInitialPopulations()), new String[]{"csv"}, false);
		if (testList.size()==0)
			Parameters.setTrainingFlag(true);

		// set country donor input file
		String taxDonorInputFilename = "tax_donor_population_" + country;
		Parameters.setTaxDonorInputFileName(taxDonorInputFilename);

		// Create EUROMODPolicySchedule input from files
		if (!rewritePolicySchedule &&
				!new File("input" + File.separator + Parameters.EUROMODpolicyScheduleFilename + ".xlsx").exists()) {
			throw new FileNotFoundException("Policy Schedule file '"+ File.separator + "input" + File.separator +
					Parameters.EUROMODpolicyScheduleFilename + ".xlsx` doesn't exist. " +
					"Provide excel file or use `--rewrite-policy-schedule` to re-construct from available policy files.");
		};
		if (rewritePolicySchedule) writePolicyScheduleExcelFile();
		//Save the last selected country and year to Excel to use in the model if GUI launched straight away
		String[] columnNames = {"Country", "Year"};
		Object[][] data = new Object[1][columnNames.length];
		data[0][0] = country.toString();
		data[0][1] = startYear;
		XLSXfileWriter.createXLSX(Parameters.INPUT_DIRECTORY, Parameters.DatabaseCountryYearFilename, "Data", columnNames, data);

		// load uprating factors
		Parameters.loadTimeSeriesFactorMaps(country);
		Parameters.instantiateAlignmentMaps();
		TaxDonorDataParser.constructAggregateTaxDonorPopulationCSVfile(country, showGui);

		// Create initial and donor population database tables
		DataParser.databaseFromCSV(country, showGui);
		TaxDonorDataParser.databaseFromCSV(country, startYear, false);
		Parameters.loadTimeSeriesFactorForTaxDonor(country);
		TaxDonorDataParser.populateDonorTaxUnitTables(country, showGui);

	}

	public static void writePolicyScheduleExcelFile() {

		Collection<File> euromodOutputTextFiles = FileUtils.listFiles(new File(Parameters.getEuromodOutputDirectory()), new String[]{"txt"}, false);
		Iterator<File> fIter = euromodOutputTextFiles.iterator();
		while (fIter.hasNext()) {
			File file = fIter.next();
			if (file.getName().endsWith("_EMHeader.txt")) {
				fIter.remove();
			}
		}

		// create table to allow user specification of policy environment
		String[] columnNames = {
				Parameters.EUROMODpolicyScheduleHeadingFilename,
				Parameters.EUROMODpolicyScheduleHeadingScenarioYearBegins.replace('_', ' '),
				Parameters.EUROMODpolicyScheduleHeadingScenarioSystemYear.replace('_', ' '),
				Parameters.EUROMODpolicySchedulePlanHeadingDescription
		};
		Object[][] data = new Object[euromodOutputTextFiles.size()][columnNames.length];
		int row = 0;
		for (File file: euromodOutputTextFiles) {
			String name = file.getName();
			data[row][0] = name;
			data[row][1] = name.split("_")[1];
			data[row][2] = name.split("_")[1];
			data[row][3] = "";
			row++;
		}

		XLSXfileWriter.createXLSX(Parameters.INPUT_DIRECTORY, Parameters.EUROMODpolicyScheduleFilename, country.toString(), columnNames, data);
	}


	/**
	 *
	 * 	Display dialog box to allow users to define desired simulation
	 *
	 */
	private static void runGUIdialog() {

		int count;

		// initiate radio buttons to define policy environment and input database
		count = 0;
		Map<String,Integer> startUpOptionsStringsMap = new LinkedHashMap<>();
		startUpOptionsStringsMap.put("Change country and/or simulation start year", count++);
		startUpOptionsStringsMap.put("Load new input data for starting populations", count++);
		startUpOptionsStringsMap.put("Use UKMOD Light to alter description of tax and benefit systems", count++);
		startUpOptionsStringsMap.put("Load new input data for tax and benefit systems", count++);
		startUpOptionsStringsMap.put("Select tax and benefit systems for analysis", count++);
		StartUpCheckBoxes startUpOptions = new StartUpCheckBoxes(startUpOptionsStringsMap);

	    // combine button groups into a single form component
		JInternalFrame initialisationFrame = new JInternalFrame();
		BasicInternalFrameUI bi = (BasicInternalFrameUI)initialisationFrame.getUI();
		bi.setNorthPane(null);
		initialisationFrame.setBorder(null);
		startUpOptions.setBorder(BorderFactory.createTitledBorder("options for policy environment and input database"));
		initialisationFrame.setLayout(new BoxLayout(initialisationFrame.getContentPane(), BoxLayout.PAGE_AXIS));
		initialisationFrame.add(startUpOptions);
		JPanel border = new JPanel();
		initialisationFrame.add(border);
		initialisationFrame.setVisible(true);

		// text for GUI
        String title = "Start-up Options";
		String text = "<html><h2 style=\"text-align: center; font-size:120%;\">Choose the start-up processes for the simulation</h2>";

		// sizing for GUI
		int height = 280, width = 550;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (screenSize.width < 850) {
			width = (int) (screenSize.width * 0.95);
		}

		// display GUI
		FormattedDialogBox.create(title, text, width, height, initialisationFrame, true, true, true);

		// get data returned from GUI
		boolean[] choices = startUpOptions.getChoices();

		if (choices[0]) {
			// choose the country and the simulation start year
			// this information can be used when constructing a new donor population
			// and referenced when adjusting the EUROMOD policy schedule (scenario plan).
			Collection<File> testList = FileUtils.listFiles(new File(Parameters.getInputDirectoryInitialPopulations()), new String[]{"csv"}, false);
			if (testList.size()==0)
				Parameters.setTrainingFlag(true);
			chooseCountryAndStartYear();
		}
		String taxDonorInputFilename = "tax_donor_population_" + country;
		Parameters.setTaxDonorInputFileName(taxDonorInputFilename);

		if (choices[0] || choices[1]) {
			// rebuild databases for population cross-section used to initialise simulated population
			DataParser.databaseFromCSV(country, showGui); // Initial database tables
		}

		if (choices[2]) {
			// call to modify policies

			// run EUROMOD
			//CallEUROMOD.run();

			//run EUROMOD Light
			CallEMLight.run();
		}

		if (choices[0] || choices[2] || choices[3] || choices[4]) {
			// call to select policies

			// load previously stored values for policy description and initiation year
			MultiKeyCoefficientMap previousEUROMODfileInfo = ExcelAssistant.loadCoefficientMap("input" + File.separator + Parameters.EUROMODpolicyScheduleFilename + ".xlsx", country.toString(), 1, 3);
			Collection<File> euromodOutputTextFiles = FileUtils.listFiles(new File(Parameters.getEuromodOutputDirectory()), new String[]{"txt"}, false);
			Iterator<File> fIter = euromodOutputTextFiles.iterator();
			while (fIter.hasNext()) {
				File file = fIter.next();
				if (file.getName().endsWith("_EMHeader.txt")) {
					fIter.remove();
				}
			}

			// create table to allow user specification of policy environment
	        String[] columnNames = {
					Parameters.EUROMODpolicyScheduleHeadingFilename,
					Parameters.EUROMODpolicyScheduleHeadingScenarioYearBegins.replace('_', ' '),
					Parameters.EUROMODpolicyScheduleHeadingScenarioSystemYear.replace('_', ' '),
	        		Parameters.EUROMODpolicySchedulePlanHeadingDescription
			};
	        Object[][] data = new Object[euromodOutputTextFiles.size()][columnNames.length];
	        int row = 0;
	        for (File file: euromodOutputTextFiles) {
	        	String name = file.getName();
	        	data[row][0] = name;
        		if (previousEUROMODfileInfo.getValue(name, Parameters.EUROMODpolicyScheduleHeadingScenarioYearBegins) != null) {
        			data[row][1] = previousEUROMODfileInfo.getValue(name, Parameters.EUROMODpolicyScheduleHeadingScenarioYearBegins).toString();
	        	} else {
	        		data[row][1] = "";
	        	}
				if (previousEUROMODfileInfo.getValue(name, Parameters.EUROMODpolicyScheduleHeadingScenarioSystemYear) != null) {
					data[row][2] = previousEUROMODfileInfo.getValue(name, Parameters.EUROMODpolicyScheduleHeadingScenarioSystemYear).toString();
				} else {
					data[row][2] = "";
				}
        		if (previousEUROMODfileInfo.getValue(name, Parameters.EUROMODpolicySchedulePlanHeadingDescription) != null) {
        			data[row][3] = previousEUROMODfileInfo.getValue(name, Parameters.EUROMODpolicySchedulePlanHeadingDescription).toString();
	        	} else {
	        		data[row][3] = "";
	        	}
	        	row++;
	        }

	        // create GUI display content
	        String titleEUROMODtable = "Update EUROMOD Policy Schedule";
	        String textEUROMODtable =
				"<html><h2 style=\"text-align: center; font-size:120%;\">Select EUROMOD policies to use in simulation by entering a valid 'policy start year' and 'policy system year'</h2>" +
				"<p style=\"text-align:center; font-size:120%;\">Policies for which no start year is provided will be omitted from the simulation.<br />" +
				"<p style=\"text-align:center; font-size:120%;\">Policy system year must match the year selected in EUROMOD / UKMOD when creating the policy.<br />" +
				"If no policy is selected for the start year of the simulation (<b>" + startYear + "</b>), then the earliest policy will be applied.<br />" +
				"<b>Optional</b>: add a description of the scenario policy to record what the policy refers to.</p>";
	        ScenarioTable tableEUROMODscenarios = new ScenarioTable(textEUROMODtable, columnNames, data);

	        // pass content to display
			FormattedDialogBoxNonStatic policyScheduleBox = new FormattedDialogBoxNonStatic(titleEUROMODtable, null, 900, 300 + euromodOutputTextFiles.size()*11, tableEUROMODscenarios, true);

			//Store a copy in the input directory so that we have a record of the policy schedule for reference
			XLSXfileWriter.createXLSX(Parameters.INPUT_DIRECTORY, Parameters.EUROMODpolicyScheduleFilename, country.toString(), columnNames, data);
		}

		if(choices[0] || choices[2] || choices[3] || choices[4]) {
			// call to import new tax data
			TaxDonorDataParser.constructAggregateTaxDonorPopulationCSVfile(country, showGui);
			TaxDonorDataParser.databaseFromCSV(country, startYear, true); // Donor database tables
			Parameters.loadTimeSeriesFactorForTaxDonor(country);
			TaxDonorDataParser.populateDonorTaxUnitTables(country, showGui); // Populate tax unit donor tables from person data
		}
	}


	/**
	 *
	 * METHOD FOR DISPLAYING GUI FOR SELECTING COUNTRY AND START YEAR OF SIMULATION
	 *
	 */
	private static void chooseCountryAndStartYear() {

		// set-up combo-boxes
		String textC = null;
		ComboBoxCountry cbCountry = new ComboBoxCountry(textC);
		String textY = null;
		ComboBoxYear cbStartYear = new ComboBoxYear(textY);

		// combine combo-boxes into a single form component
		JInternalFrame countryAndYearFrame = new JInternalFrame();
		BasicInternalFrameUI bi = (BasicInternalFrameUI)countryAndYearFrame.getUI();
		bi.setNorthPane(null);
		countryAndYearFrame.setBorder(null);
        cbCountry.setBorder(BorderFactory.createTitledBorder("Country selection drop-down menu"));
        cbStartYear.setBorder(BorderFactory.createTitledBorder("Start year selection drop-down menu"));
        countryAndYearFrame.setLayout(new BoxLayout(countryAndYearFrame.getContentPane(), BoxLayout.PAGE_AXIS));
        countryAndYearFrame.add(cbCountry);
        JPanel border = new JPanel();
        countryAndYearFrame.add(border);
        countryAndYearFrame.add(cbStartYear);
        JPanel border2 = new JPanel();
        countryAndYearFrame.add(border2);
        countryAndYearFrame.setVisible(true);

		// text for GUI
		String title = "Country and Start Year";
		String text = "<html><h2 style=\"text-align: center; font-size:120%;\">Select simulation country and start year</h2>";

		// sizing for GUI
		int height = 350, width = 600;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (screenSize.width < 850) {
			width = (int) (screenSize.width * 0.95);
		}

		// display GUI
		FormattedDialogBox.create(title, text, width, height, countryAndYearFrame, true, true, true);

		//Set country and start year from dialog box.  These values will appear in JAS-mine GUI.
        //TODO: There is a danger that these values could be reset in the JAS-mine GUI.  While the
        // reset of the start year is not a problem, the country could create erroneous results
        // (i.e. the incorrect regression coefficients would be applied to the population).
        // Therefore, we should pass some sort of setting to the model blocking the alteration of
        // the country if possible.  Could this be done by making the setCountry method ineffective
        // when the country has been set by the user in the dialog box in the start class?
      	country = cbCountry.getCountryEnum();	 //Temporarily commented out to disallow choice of the country
        //country = Country.IT;
		startYear = cbStartYear.getYear();

		//Save the last selected country and year to Excel to use in the model if GUI launched straight away
		String[] columnNames = {"Country", "Year"};
		Object[][] data = new Object[1][columnNames.length];
		data[0][0] = country.toString();
		data[0][1] = startYear;
		XLSXfileWriter.createXLSX(Parameters.INPUT_DIRECTORY, Parameters.DatabaseCountryYearFilename, "Data", columnNames, data);
	}
}
