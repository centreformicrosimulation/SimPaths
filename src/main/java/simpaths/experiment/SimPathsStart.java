// define package
package simpaths.experiment;

// import Java packages
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.JPanel;

// third-party packages
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
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
import simpaths.model.HibernateUtil;
import simpaths.model.taxes.*;


/**
 *
 * 	CLASS FOR SINGLE SIMULATION EXECUTION
 *
 */
public class SimPathsStart implements ExperimentBuilder {

	// default simulation parameters
	private static Country country = Country.UK;
	private static int startYear = Parameters.getMaxStartYear();


	/**
	 *
	 * 	MAIN class for simulation entry
	 *
	 */
	public static void main(String[] args) {

		// show GUI by default
		boolean showGui = true;

		// display dialog box to allow users to define desired simulation
		runGUIdialog();

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


	/**
	 *
	 * 	Display dialog box to allow users to define desired simulation
	 *
	 */
	private static void runGUIdialog() {

		int count;

		// initiate radio buttons to define policy environment and input database
		count = 0;
		Map<String, Integer> startUpOptionsStringsMap = new LinkedHashMap<>();
		startUpOptionsStringsMap.put("Run SimPaths GUI", count++); //Option 0
	//	startUpOptionsStringsMap.put("SimPaths GUI, create Input Database", count++);
		startUpOptionsStringsMap.put("Run SimPaths GUI <= Select policies", count++); //Option 1
		startUpOptionsStringsMap.put("Run SimPaths GUI <= Select policies <= Load input data", count++); //Option 2
		startUpOptionsStringsMap.put("Run SimPaths GUI <= Select policies <= Modify policies", count++); //Option 3
		startUpOptionsStringsMap.put("Run SimPaths GUI <= Select policies <= Modify policies <= Load input data", count++); //Option 4
	    StartUpRadioButtons startUpOptions = new StartUpRadioButtons(startUpOptionsStringsMap);

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
		FormattedDialogBox.create(title, text, width, height, initialisationFrame, true, true);

		// get data returned from GUI
		int choice_policy = startUpOptions.getChoice();

		// run through requested pre-processing steps
		boolean skip = false;

		if (choice_policy >= 3) {
			// call to modify policies

			// run EUROMOD
//			CallEUROMOD.run();

			//run EUROMOD Light
			CallEMLight.run();
		}

		if (choice_policy > 0) {
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

		// load uprating factors
		Parameters.loadTimeSeriesFactorMaps(country); // TODO: Check if this can be moved inside of Parameters.loadParameters() method and remove from here and SimPathsMultirun if possible

		if (choice_policy > 0) {
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
			FormattedDialogBoxNonStatic policyScheduleBox;

			if (choice_policy==2 || choice_policy==4) { // If full rebuild of the database is required, disabled the "Keep existing policy schedule" button by overriding the display box
				policyScheduleBox = new FormattedDialogBoxNonStatic(titleEUROMODtable, null, 900, 300 + euromodOutputTextFiles.size()*11, tableEUROMODscenarios, true, false);
			}
			else {
				policyScheduleBox = new FormattedDialogBoxNonStatic(titleEUROMODtable, null, 900, 300 + euromodOutputTextFiles.size()*11, tableEUROMODscenarios, true, true);
			}

	        // return from table
	        skip  = policyScheduleBox.isSkip();
	        if (!skip) {
				//Store a copy in the input directory so that we have a record of the policy schedule for reference
		        XLSXfileWriter.createXLSX(Parameters.INPUT_DIRECTORY, Parameters.EUROMODpolicyScheduleFilename, country.toString(), columnNames, data);
		    	constructAggregateTaxDonorPopulationCSVfile(country);
	        }
		}

		if (choice_policy==2 || choice_policy==4) {
			// rebuild databases for population cross-section used to initialise simulated population

			createPopulationCrossSectionDatabaseTables(country); // Initial database tables
		}
		if(choice_policy > 0 && !skip) {
			// call to select policies

			SQLDonorDataParser.run(country, startYear); // Donor database tables
			populateDonorTaxUnitTables(country); // Populate tax unit donor tables from person data
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
		FormattedDialogBox.create(title, text, width, height, countryAndYearFrame, true, true);

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


	/**
	 *
	 * Constructs the tax donor population based on the data stored in .txt files produced by EUROMOD.
	 * Note that in order to store this information in an efficient way, we make the important
	 * assumption that all EUROMOD output for a particular country is derived from the same
	 * input population (this is plausible, given that EUROMOD is a static microsimulation).
	 * When running EUROMOD, the most recent input population for a particular country should be
	 * used (currently IT_2014_a2.txt and UK_2013_a3.txt).
	 *
	 * This method constructs a .csv file that aggregates the information from multiple EUROMOD
	 * output .txt files, picking up the relevant columns for each EUROMOD policy scenario, that
	 * will eventually be parsed into the JAS-mine input database.
	 *
	 * @return The name of the created CSV file (without the .csv extension)
	 *
	 */
	private static void constructAggregateTaxDonorPopulationCSVfile(Country country) {

		// display a dialog box to let the user know what is happening
		String title = "Creating " + Parameters.getTaxDonorInputFileName() + ".csv file";
		String text = "<html><h2 style=\"text-align: center; font-size:120%; padding: 10pt\">"
				+ "Compiling single working file to facilitate construction of relational database for imputing transfer payments</h2>";
		JFrame csvFrame = FormattedDialogBox.create(title, text, 800, 120, null, false, false);

		// fields for exporting tables to output .csv files
		final String newLine = "\n";
		final String delimiter = ",";

		// prepare file and buffer to write data
		Map<Integer, String> euromodPolicySchedule = Parameters.calculateEUROMODpolicySchedule(country);
		Path target = FileSystems.getDefault().getPath(Parameters.INPUT_DIRECTORY, Parameters.getTaxDonorInputFileName() + ".csv");
		if (target.toFile().exists()) {
			target.toFile().delete();		// delete previous version of the file to allow the new one to be constructed
		}
		BufferedWriter bufferWriter = null;

		try {
			// read data
			Map<String, List<String[]>> allFilesByName = new LinkedHashMap<String, List<String[]>>();

			// get list of attributes (column names) from EUROMOD output files that are not policy dependent (i.e. they are like the input data)
			Set<String> policyInvariantAttributeNames = new LinkedHashSet<String>(Arrays.asList(Parameters.DONOR_STATIC_VARIABLES));

			//Append the names of country-specific variables
			Parameters.setCountryBenefitUnitName(); //Specify names of benefit unit variables
			policyInvariantAttributeNames.add((String) Parameters.getBenefitUnitVariableNames().getValue(country.getCountryName()));

			// create list of attributes (column names) of EUROMOD output files that depend on the policy parameters and that will vary between different scenarios
			Set<String> policyOutputVariables = new LinkedHashSet<String>(Arrays.asList(Parameters.DONOR_POLICY_VARIABLES));

			int numRows = -1;
			for (String policyName: euromodPolicySchedule.values()) {

				Path source;
				source = FileSystems.getDefault().getPath(Parameters.getEuromodOutputDirectory(), policyName + ".txt");
				List<String> fileContentByLine = Files.readAllLines(source);

				// Get indices of required vars. The first file should include Input & Output vars, the rest only Output vars.
				String[] tmpHeader = fileContentByLine.get(0).split("\t");
				Map<String, Integer> indices = new LinkedHashMap<String, Integer>();
				int p = 0;
				for (String vr: tmpHeader)
				{
					if (numRows == -1 && policyInvariantAttributeNames.contains(vr)) indices.put(vr, p);
					if (policyOutputVariables.contains(vr)) indices.put(vr, p);
					p++;
				}

				// check the number of rows of each .txt file (which corresponds to the number of persons) are the same
				if (numRows == -1) {	//Set the length to the first file
					numRows = fileContentByLine.size();
				}
				else {
					int newNumRows = fileContentByLine.size();
					if (newNumRows != numRows) {
						throw new IllegalArgumentException("ERROR - the EUROMOD policy scenario textfile " + policyName + ".txt has " + newNumRows + " rows, which is not the same number as at least one other EUROMOD .txt file!  All files must have the same number of rows (each row corresponds to a different person / agent)!");
					}
				}

				List<String[]> fileContentByLineSplit = new ArrayList<>(numRows);

				for (String line: fileContentByLine) {
					String[] dataArray = line.split("\t");
					List<String> usedVars = new ArrayList<>();
					for (Integer ind: indices.values()) usedVars.add(dataArray[ind]);
					fileContentByLineSplit.add(usedVars.toArray(new String[0]));
	//				fileContentByLineSplit.add(dataArray);
				}

				allFilesByName.put(policyName, fileContentByLineSplit);
			}

			// write data
			target.toFile().createNewFile();
			bufferWriter = new BufferedWriter(new FileWriter(target.toFile(), true));

			// structure to hold all data to be parsed from the EUROMOD output files. The first key is the EUROMOD output text filename, while the value is a map that maps the name of the attribute (i.e. the column name) to its array index (i.e. the column number, starting from 0) in the output file, as stored in allFilesByName.
			Map<String, LinkedHashMap<String, Integer>> attributePositionsByNameByFilename = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
			for (String filename: allFilesByName.keySet()) {
				attributePositionsByNameByFilename.put(filename, new LinkedHashMap<String, Integer>());
			}

			// policy independent attributes
			// the first filename is used to get the data for all the input attributes (i.e. those not affected by EUROMOD policy scenario, not an output).
			Iterator<String> filenameIter = allFilesByName.keySet().iterator();
			String firstFilename = filenameIter.next();
			String[] header = allFilesByName.get(firstFilename).get(0);		//Get header line
			LinkedHashMap<String, Integer> firstFilenameMapColumnNameToIndex = attributePositionsByNameByFilename.get(firstFilename);
			for (int i = 0; i < header.length; i++) {
				String columnName = header[i];
				if (policyInvariantAttributeNames.contains(columnName)) { //PB RMK: If name of variable has been misspelled for example, this will not pick it up - will proceed but produce a file with the variable missing and crash later
					firstFilenameMapColumnNameToIndex.put(header[i], i);
					bufferWriter.append(columnName + delimiter);
				}
			}

			// policy dependent attributes
			for (String filename: allFilesByName.keySet()) {
				header = allFilesByName.get(filename).get(0);		//Get header line
				LinkedHashMap<String, Integer> mapColumnNameToIndex = attributePositionsByNameByFilename.get(filename);
				for (int i = 0; i < header.length; i++) {
					String columnName = header[i];
					if (policyOutputVariables.contains(columnName)) {
						mapColumnNameToIndex.put(columnName, i);
						bufferWriter.append(columnName + "_" + filename + delimiter);		//Note that the policy dependent variable names have an additional label that follows the filename of the specific EUROMOD policy output file.
					}
				}
			}
			bufferWriter.append(newLine);

			// write data to new file
			for (int row = 1; row < numRows; row++) {
				for (String filename: attributePositionsByNameByFilename.keySet()) {
					Map<String, Integer> mapColumnNamesByIndex = attributePositionsByNameByFilename.get(filename);
					for (String columnName: mapColumnNamesByIndex.keySet()) {
						int column = mapColumnNamesByIndex.get(columnName);
						if (!columnName.equals(allFilesByName.get(filename).get(0)[column])) {
							throw new IllegalArgumentException("ERROR - column names do not match!");
						}
						bufferWriter.append(allFilesByName.get(filename).get(row)[column] + delimiter);
					}
				}
				bufferWriter.append(newLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally
		{
			try {
				bufferWriter.flush();
				bufferWriter.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// finish off
		csvFrame.setVisible(false);
//		return inputFilename;
	}


	/**
	 *
	 * GENERATE DATABASE TABLES TO INITIALISE SIMULATED POPULATION CROSS-SECTION FROM CSV FILES
	 * @param country
	 *
	 */
	private static void createPopulationCrossSectionDatabaseTables(Country country) {

		// display a dialog box to let the user know what is happening
		String title = "Building all database tables";
		String text = "<html><h2 style=\"text-align: center; font-size:120%; padding: 10pt\">"
				+ "Building database tables to initialise simulated population cross-section for " + country.getCountryName()
				+ "</h2></html>";
		JFrame databaseFrame = FormattedDialogBox.create(title, text, 800, 120, null, false, false);

		// start work
		Connection conn = null;
		Statement stat = null;
        try {
        	Class.forName("org.h2.Driver");
	        conn = DriverManager.getConnection("jdbc:h2:file:./input" + File.separator + "input;TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0;CACHE_SIZE=2097152;AUTO_SERVER=TRUE", "sa", "");

			Parameters.setPopulationInitialisationInputFileName("population_initial_" + country.toString());

	        //This calls a method creating both the donor population tables and initial populations for every year between minStartYear and maxStartYear.
	        SQLdataParser.createDatabaseForPopulationInitialisationByYearFromCSV(country, Parameters.getPopulationInitialisationInputFileName(), Parameters.getMinStartYear(), Parameters.getMaxStartYear(), conn);

	        conn.close();
        }
        catch(ClassNotFoundException|SQLException e){
        	if(e instanceof ClassNotFoundException) {
	    		 System.out.println( "ERROR: Class not found: " + e.getMessage() + "\nCheck that the input.h2.db "
	        		+ "exists in the input folder.  If not, unzip the input.h2.zip file and store the resulting "
	        		+ "input.h2.db in the input folder!\n");
	    	}
	    	else {
	    		 throw new IllegalArgumentException("SQL Exception thrown! " + e.getMessage());
	    	}
        }
		finally {
			try {
				  if (stat != null) { stat.close(); }
				  if (conn != null) { conn.close(); }
			} catch (SQLException e) {

				e.printStackTrace();
			}
		}

		// finish off
        databaseFrame.setVisible(false);
	}


	/**
	 *
	 * METHOD TO POPULATE TAX UNIT TABLES FROM PERSON LEVEL DATA
	 *
	 */
	private static void populateDonorTaxUnitTables(Country country) {

		// display a dialog box to let the user know what is happening
		String title = "Populating donor database tables";
		String text = "<html><h2 style=\"text-align: center; font-size:120%; padding: 10pt\">"
				+ "Populating database with tax-unit data evaluated from person-level data</h2></html>";
		JFrame csvFrame = FormattedDialogBox.create(title, text, 800, 120, null, false, false);

		// gather all donor tax units
		List<DonorTaxUnit> taxUnits = null;

		// establish session for database link
		EntityTransaction txn = null;
		try {

			EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
			txn = em.getTransaction();
			txn.begin();

			// check that all annual samples are the same size (system files for database should have used the same input data
			Integer checkSum = null;
			for (int fromYear : Parameters.EUROMODpolicyScheduleSystemYearMap.keySet()) {
				int systemYear = Parameters.EUROMODpolicyScheduleSystemYearMap.get(fromYear).getValue();
				String query = "SELECT pu FROM DonorPersonPolicy pu WHERE systemYear=" + systemYear;
				List<DonorPersonPolicy> persons = em.createQuery(query).getResultList();
				if (checkSum==null) {
					checkSum = persons.size();
				} else {
					if (checkSum!=persons.size())
						// All tax database files should be derived using same input dataset
						throw new RuntimeException("sample sizes for tax database vary between system years");
				}
			}

			// populate tax unit data
			taxUnits = em.createQuery("SELECT DISTINCT tu FROM DonorTaxUnit tu LEFT JOIN FETCH tu.persons tp LEFT JOIN FETCH tp.policies pl").getResultList();
			for (DonorTaxUnit taxUnit : taxUnits) {
				// loop through tax units

				int age = 0, numberMembersOver17 = 0, numberChildrenUnder5 = 0, numberChildren5To9 = 0;
				int numberChildren10To17 = 0, dlltsd1 = -1, dlltsd2 = -1;
				double hoursWorkedPerWeek1 = 0.0, hoursWorkedPerWeek2 = 0.0;
				boolean flagInitialiseDemographics = true;
				for (int fromYear : Parameters.EUROMODpolicyScheduleSystemYearMap.keySet()) {

					int systemYear = Parameters.EUROMODpolicyScheduleSystemYearMap.get(fromYear).getValue();
					double origIncome = 0.0;
					double earnings = 0.0;
					double dispIncome = 0.0;
					double benmt = 0.0;
					double bennt = 0.0;
					double secondIncome = 0.0;
					double childcare = 0.0;
					boolean flagSecondIncome = false;
					int ageTest = 0;
					for(DonorPerson person : taxUnit.getPersons()) {
						// loop through persons

						origIncome += person.getPolicy(fromYear).getOriginalIncomePerMonth();
						earnings += person.getPolicy(fromYear).getEarningsPerMonth();
						dispIncome += person.getPolicy(fromYear).getDisposableIncomePerMonth();
						benmt += person.getPolicy(fromYear).getMonetaryBenefitsAmount();
						bennt += person.getPolicy(fromYear).getNonMonetaryBenefitsAmount();
						if (person.getPolicy(fromYear).getOriginalIncomePerMonth() > 0.01) {
							if (flagSecondIncome) {
								secondIncome = person.getPolicy(fromYear).getOriginalIncomePerMonth();
							} else {
								flagSecondIncome = true;
							}
						}
						childcare += person.getPolicy(fromYear).getChildcareCostPerMonth();
						int agePerson = person.getAge();
						if (flagInitialiseDemographics) {
							// need to instantiate variables to evaluate keys

							age = Math.max(age, agePerson);
							if (agePerson < 5) {
								numberChildrenUnder5 += 1;
							} else if (agePerson < 10) {
								numberChildren5To9 += 1;
							} else if (agePerson < Parameters.AGE_TO_BECOME_RESPONSIBLE) {
								numberChildren10To17 += 1;
							} else {
								numberMembersOver17 += 1;
							}
							int hoursWorked = person.getHoursWorkedWeekly();
							if (hoursWorked > hoursWorkedPerWeek1) {
								hoursWorkedPerWeek2 = hoursWorkedPerWeek1;
								hoursWorkedPerWeek1 = hoursWorked;
							} else if (hoursWorked > hoursWorkedPerWeek2) {
								hoursWorkedPerWeek2 = hoursWorked;
							}
							if (agePerson >= Parameters.AGE_TO_BECOME_RESPONSIBLE) {
								int dlltsd = person.getDlltsd();
								if (dlltsd > dlltsd1) {
									dlltsd2 = dlltsd1;
									dlltsd1 = dlltsd;
								} else if (dlltsd > dlltsd2) {
									dlltsd2 = dlltsd;
								}
							}
						} else {
							ageTest = Math.max(ageTest, agePerson);
						}
					}
					if (!flagInitialiseDemographics && ageTest!=age)
						throw new RuntimeException("Demographic characteristics vary across system years derived from EUROMOD");
					flagInitialiseDemographics = false;
					if (Math.abs(secondIncome) > 0.01)
						secondIncome = Math.max(0.0, Math.min(secondIncome, origIncome - secondIncome));
					DonorTaxUnitPolicy taxUnitPolicy = taxUnit.getPolicyByFromYear(fromYear);
					taxUnitPolicy.setSystemYear(systemYear);
					if (numberMembersOver17==1 || numberMembersOver17==2) {

						// evaluate donor keys
						double originalIncomePerWeek = origIncome / Parameters.WEEKS_PER_MONTH;
						double childcareCostPerWeek = childcare / Parameters.WEEKS_PER_MONTH;
						double secondIncomePerWeek = secondIncome / Parameters.WEEKS_PER_MONTH;
						DonorKeys keys = new DonorKeys();
						KeyFunction keyFunction = new KeyFunction(systemYear, systemYear, age, numberMembersOver17, numberChildrenUnder5,
								numberChildren5To9, numberChildren10To17, hoursWorkedPerWeek1, hoursWorkedPerWeek2, dlltsd1, dlltsd2,
								originalIncomePerWeek, secondIncomePerWeek, childcareCostPerWeek);
						keys.evaluate(keyFunction);

						// set all taxUnitPolicy attributes
						taxUnitPolicy.setOriginalIncomePerMonth(origIncome);
						taxUnitPolicy.setEarningsPerMonth(earnings);
						taxUnitPolicy.setDisposableIncomePerMonth(dispIncome);
						taxUnitPolicy.setBenMeansTestPerMonth(benmt);
						taxUnitPolicy.setBenNonMeansTestPerMonth(bennt);
						taxUnitPolicy.setSecondIncomePerMonth(secondIncome);
						taxUnitPolicy.setChildcareCostPerMonth(childcare);
						for(int ii=0; ii<Parameters.TAXDB_REGIMES; ii++) {
							taxUnitPolicy.setDonorKey(ii, keys.getKey(ii));
						}
					} else {

						for(int ii=0; ii<Parameters.TAXDB_REGIMES; ii++) {
							taxUnitPolicy.setDonorKey(ii, -1);
						}
					}

					em.persist(taxUnitPolicy);
				}
				em.persist(taxUnit);
			}

			// close connection
			txn.commit();
			em.close();
		} catch (Exception e) {
			if (txn != null) {
				txn.rollback();
			}
			e.printStackTrace();
			throw new RuntimeException("Problem populating tax unit database for imputing tax and benefit payments");
		}

		// remove message box
		csvFrame.setVisible(false);
	}
}
