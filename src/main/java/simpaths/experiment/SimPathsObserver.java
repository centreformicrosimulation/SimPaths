// define package
package simpaths.experiment;

// import Java packages
import java.awt.*;
import java.util.*;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;

// import plug-in packages
import simpaths.data.filters.*;
import simpaths.model.BenefitUnit;
import simpaths.model.SimPathsModel;
import simpaths.model.Validator;
import simpaths.model.enums.*;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.log4j.Logger;
import net.miginfocom.swing.MigLayout;

// import JAS-mine packages
import microsim.annotation.GUIparameter;
import microsim.engine.AbstractSimulationObserverManager;
import microsim.engine.SimulationCollectorManager;
import microsim.engine.SimulationManager;
import microsim.event.CommonEventType;
import microsim.event.EventGroup;
import microsim.event.EventListener;
import microsim.event.SingleTargetEvent;
import microsim.gui.GuiUtils;
import microsim.gui.plot.IndividualBarSimulationPlotter;
import microsim.gui.plot.ScatterplotSimulationPlotterRefreshable;
import microsim.gui.plot.Weighted_PyramidPlotter;
import microsim.gui.plot.TimeSeriesSimulationPlotter;
import microsim.gui.plot.Weighted_HistogramSimulationPlotter;
import microsim.statistics.IDoubleSource;
import microsim.statistics.ILongSource;
import microsim.statistics.functions.MultiTraceFunction;
import microsim.statistics.weighted.Weighted_CrossSection;
import microsim.statistics.weighted.functions.Weighted_MeanArrayFunction;
import microsim.statistics.weighted.functions.Weighted_SumArrayFunction;

// import LABOURsim packages
import simpaths.model.Person;
import simpaths.data.Parameters;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;


/**
 *
 * CLASS TO MANAGE OBSERVER OF SIMULATED OUTPUT
 *
 */
public class SimPathsObserver extends AbstractSimulationObserverManager implements EventListener, ILongSource {

	// default simulation parameters
	private static Logger log = Logger.getLogger(SimPathsObserver.class);
	
	@GUIparameter(description="Toggle to turn all charts on/off")
	private Boolean showCharts = true;

	@GUIparameter(description = "Enable additional charts")
	private Boolean showAdditionalCharts = true;

	@GUIparameter(description = "Enable validation statistics")
	private Boolean showValidationStatistics = true;
	
	@GUIparameter(description = "Set the time-period between chart updates")
	private Double displayFrequency = 1.;
	
//	@GUIparameter(description = "Set the type of histogram to display")		//Histogram types other than Frequency do not work properly with weighted histograms / cross sections
	private HistogramTypeEnum histogramType = HistogramTypeEnum.Frequency;

	@GUIparameter(description = "Set the number of bins to use in the Histograms")
    private Integer numberOfHistogramBins = 100;

//	@GUIparameter(description = "Specify the maximum number of most recent data points to show on the scatterplot of the Bowker norm of labour market demand * supply elasticities")
	private Integer convergenceElasticitiesPlotMaxSamples = 50;

//	@GUIparameter(description = "Specify the maximum number of most recent data points to show on the scatterplot of potential earnings during the convergence process")
	private Integer potentialEarningsPlotMaxSamples = 20;
	
//	@GUIparameter(description = "Specify the maximum number of most recent data points to show on the scatterplot of the aggregate labour demand and supply during the convergence process")
	private Integer labourMarketPlotMaxSamples = 20;
	
	//GUI Parameters to toggle specific charts on/off

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean educationByAge = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean educationByRegion = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean educationOfAdults = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean employmentByAge = true;

//	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean employmentByRegion = false;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean employmentOfAdults = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean femaleEmploymentByMaternity = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean grossEarningsByRegionAndEducation = true;

//	@GUIparameter(description="Toggle to turn chart on/off")
//	private boolean health = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean healthByAge = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean householdComposition = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean incomeHistograms = true;

//	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean populationPyramid = true;
	
//	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean workingHoursPyramid = false;

	@GUIparameter(description = "Toggle to turn chart on/off")
	private boolean securityIndex = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean labourSupply = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean population = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean poverty = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean studentsByAge = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean studentsByRegion = true;

	private boolean activityStatus = true;

	@GUIparameter(description="Toggle to turn chart on/off")
	private boolean homeownershipStatus = true;


//	@GUIparameter(description = "Allow convergence plots to float freely in GUI, otherwise contain plots in a frame")
	private boolean floatingConvergencePlots = false;		//Allow convergence plots to float freely in GUI, otherwise contain plots in a frame 

	private LinkedHashSet<AgeGroupCSfilter> decadeAgeGroupFilterSet;

	private LinkedHashSet<ValidEducationAgeGroupCSfilter> decadeValidEducationAgeGroupFilterSet;

	private LinkedHashSet<AgeGroupCSfilter> disabledHealthAgeGroupFilterSet;

	private LinkedHashSet<AgeGroupCSfilter> healthMentalAgeGroupFilterSet;

	private LinkedHashSet<FemalesWithChildrenByChildAgeCSfilter> childrenByAgeFilterSet;

	private ScatterplotSimulationPlotterRefreshable convergenceElasticitiesPlotter;

	private Weighted_CrossSection.Double wagesCS;
	
//	private Weighted_CrossSection.Double wagesAtRiskOfWorkCS;
		
	Set<JInternalFrame> updateChartSet;

	Set<JComponent> tabSet;

	Set<JInternalFrame> convergencePlots = new LinkedHashSet<JInternalFrame>();
	
	Map<Education, ScatterplotSimulationPlotterRefreshable> labourMarketPlots;
	
	Map<Education, ScatterplotSimulationPlotterRefreshable> potentialEarningsPlots;
	
	MultiKeyMap<Object, Weighted_MeanArrayFunction> meanPotentialEarningsMultiMap;
	
	private long countIterations = 0;

	private SimPathsModel model;

	private Validator validator;

	private int ordering = Parameters.OBSERVER_ORDERING;	//Schedule at the same time as the model and collector events, but with a higher order, so will be fired after the model and collector have updated.


	/**
	 *
	 * CONSTRUCTOR FOR SIMULATION OBSERVER
	 *
	 */
	public SimPathsObserver(SimulationManager manager, SimulationCollectorManager simulationCollectionManager) {
		super(manager, simulationCollectionManager);		
	}


	/**
	 *
	 * XXX
	 *
	 */
	@Override
	public void buildObjects() {
		
		//TODO: Change construction of objects like Weighted_CrossSection.Integer from using Java reflection to using getIntValue methods in Person class in order to improve speed?
		
		if(showCharts) {
			
			model = (SimPathsModel) getManager();
			final SimPathsCollector collector = (SimPathsCollector) getCollectorManager();
			validator = new Validator();

			//Renderers - these allow different graphs to use different look for the series displayed
			XYLineAndShapeRenderer studentAgeRenderer = new XYLineAndShapeRenderer(); //Set up a new renderer to define series colors for this chart

			Stroke dashed =  new BasicStroke(1.0f,BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {10.0f}, 0.0f);


			//Filters - can be shared amongst different Cross Sections
			AgeGroupCSfilter age0Filter = new AgeGroupCSfilter(0, 0);
			AgeGroupCSfilter age2_10Filter = new AgeGroupCSfilter(2, 10);
			AgeGroupCSfilter age11_15Filter = new AgeGroupCSfilter(11, 15);
			AgeGroupCSfilter age0_18Filter = new AgeGroupCSfilter(0,18);
			AgeGroupCSfilter age19_25Filter = new AgeGroupCSfilter(19,25);

			AgeGroupCSfilter age15_19Filter = new AgeGroupCSfilter(15,19);
			AgeGroupCSfilter age20_24Filter = new AgeGroupCSfilter(20,24);
			AgeGroupCSfilter age25_29Filter = new AgeGroupCSfilter(25,29);
			AgeGroupCSfilter age30_34Filter = new AgeGroupCSfilter(30,34);
			AgeGroupCSfilter age35_39Filter = new AgeGroupCSfilter(35,39);
			AgeGroupCSfilter age40_59Filter = new AgeGroupCSfilter(40, 59);
			AgeGroupCSfilter age60_79Filter = new AgeGroupCSfilter(60, 79);
			AgeGroupCSfilter age80_100Filter = new AgeGroupCSfilter(80, 100);

			AgeGroupCSfilter age10_19Filter = new AgeGroupCSfilter(10, 19);
			AgeGroupCSfilter age20_29Filter = new AgeGroupCSfilter(20,29);
			AgeGroupCSfilter age30_39Filter = new AgeGroupCSfilter(30,39);			
			AgeGroupCSfilter age40_49Filter = new AgeGroupCSfilter(40,49);
			AgeGroupCSfilter age50_59Filter = new AgeGroupCSfilter(50,59);
			AgeGroupCSfilter age60_69Filter = new AgeGroupCSfilter(60, 69);
			AgeGroupCSfilter age70_80Filter = new AgeGroupCSfilter(70, 80);

			AgeGroupCSfilter age0_49Filter = new AgeGroupCSfilter(0, 49);
			AgeGroupCSfilter age50_74Filter = new AgeGroupCSfilter(50, 74);
			AgeGroupCSfilter age75_100Filter = new AgeGroupCSfilter(75, 100);

			ValidEducationAgeGroupCSfilter validEdAge15_19Filter = new ValidEducationAgeGroupCSfilter(15, 19);
			ValidEducationAgeGroupCSfilter validEdAge20_29Filter = new ValidEducationAgeGroupCSfilter(20,29);
			ValidEducationAgeGroupCSfilter validEdAge30_39Filter = new ValidEducationAgeGroupCSfilter(30,39);
			ValidEducationAgeGroupCSfilter validEdAge40_49Filter = new ValidEducationAgeGroupCSfilter(40,49);
			ValidEducationAgeGroupCSfilter validEdAge50_59Filter = new ValidEducationAgeGroupCSfilter(50,59);

			FemalesWithChildrenByChildAgeCSfilter childAged0_5Filter = new FemalesWithChildrenByChildAgeCSfilter(0, 5);
			FemalesWithChildrenByChildAgeCSfilter childAged6_18Filter = new FemalesWithChildrenByChildAgeCSfilter(6, 18);


			decadeAgeGroupFilterSet = new LinkedHashSet<AgeGroupCSfilter>();
//			decadeAgeGroupFilterSet.add(age0_18Filter);
//			decadeAgeGroupFilterSet.add(age15_19Filter);
//			decadeAgeGroupFilterSet.add(age10_19Filter);
			decadeAgeGroupFilterSet.add(age20_29Filter);
			decadeAgeGroupFilterSet.add(age30_39Filter);
			decadeAgeGroupFilterSet.add(age40_49Filter);
			decadeAgeGroupFilterSet.add(age50_59Filter);
//			decadeAgeGroupFilterSet.add(age60_69Filter);
//			decadeAgeGroupFilterSet.add(age70_80Filter);


			decadeValidEducationAgeGroupFilterSet = new LinkedHashSet<ValidEducationAgeGroupCSfilter>();
			decadeValidEducationAgeGroupFilterSet.add(validEdAge20_29Filter);
			decadeValidEducationAgeGroupFilterSet.add(validEdAge30_39Filter);
			decadeValidEducationAgeGroupFilterSet.add(validEdAge40_49Filter);
			decadeValidEducationAgeGroupFilterSet.add(validEdAge50_59Filter);

			disabledHealthAgeGroupFilterSet = new LinkedHashSet<>();
			disabledHealthAgeGroupFilterSet.add(age0_49Filter);
			disabledHealthAgeGroupFilterSet.add(age50_74Filter);
			disabledHealthAgeGroupFilterSet.add(age75_100Filter);

			healthMentalAgeGroupFilterSet = new LinkedHashSet<>();
			healthMentalAgeGroupFilterSet.add(age20_29Filter);
			healthMentalAgeGroupFilterSet.add(age30_39Filter);
			healthMentalAgeGroupFilterSet.add(age40_49Filter);
			healthMentalAgeGroupFilterSet.add(age50_59Filter);

			
			
//			timeSeriesPlotSet = new LinkedHashSet<TimeSeriesSimulationPlotter>();
//			histogramPlotSet = new LinkedHashSet<Weighted_HistogramSimulationPlotter>();
			updateChartSet = new LinkedHashSet<JInternalFrame>();	//Set of all charts needed to be scheduled for updating (NOT the convergence plot!)
			tabSet = new LinkedHashSet<JComponent>();		//Set of all JInternalFrames each having a tab.  Each tab frame will potentially contain more than one chart each.
			labourMarketPlots = new LinkedHashMap<Education, ScatterplotSimulationPlotterRefreshable>();
			potentialEarningsPlots = new LinkedHashMap<Education, ScatterplotSimulationPlotterRefreshable>();						
			meanPotentialEarningsMultiMap = MultiKeyMap.multiKeyMap(new LinkedMap<>());
			for(Region region: Parameters.getCountryRegions()) {
				for(Education edu: Education.values()) {
					RegionEducationCSfilter regionEduFilter = new RegionEducationCSfilter(region, edu);
					wagesCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getHourlyWageRate1", true);
					wagesCS.setFilter(regionEduFilter);
					wagesCS.setCheckingTime(false);					//Need to set to false to enable updating during convergence process whilst the simulation time is still the same
					
					Weighted_MeanArrayFunction meanPotentialEarnings = new Weighted_MeanArrayFunction(wagesCS);
					meanPotentialEarnings.setCheckingTime(false);	//Need to set to false to enable updating during convergence process whilst the simulation time is still the same
					
					meanPotentialEarningsMultiMap.put(region,  edu, meanPotentialEarnings);
				}
			}
						

			
			//----------------------------------------------------------------------------------------------------------------------------------------
			//
			//	INTER-TIMESTEP CHARTS FOR CONVERGENCE PROCESS - those that update potentially several times in between 'time-steps' (scheduled events)
			//
			//----------------------------------------------------------------------------------------------------------------------------------------
			
		    
//			//POTENTIAL EARNINGS
//	    	potentialEarningsPlotter = new IndividualBarSimulationPlotter("Average Potential Earnings (per hour) (includes non-workers)", "currency (check whether national or euro!)");
//			for(Region region: Parameters.getCountryRegions()) {
//	    		for(Education edu: Education.values()) {
//					RegionEducationCSfilter regionEduFilter = new RegionEducationCSfilter(region, edu);
//					wagesCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getPotentialHourlyEarnings", true);
//					wagesCS.setFilter(regionEduFilter);
//					wagesCS.setCheckingTime(false);					//Need to set to false to enable updating during convergence process whilst the simulation time is still the same
//					Weighted_MeanArrayFunction meanWages = new Weighted_MeanArrayFunction(wagesCS);
//					meanWages.setCheckingTime(false);							
//					potentialEarningsPlotter.addSources("(" + region.getName() + ", " + edu.toString() + ")", meanWages, colorOfEducation(edu));
//				}
//			}
//			GuiUtils.addWindow(potentialEarningsPlotter, 500, 200, 100 * Parameters.getCountryRegions().size(), 400);
//
//	    	potentialEarningsAtRiskOfWorkPlotter = new IndividualBarSimulationPlotter("Average Potential Earnings (per hour) (excludes non-workers)", "currency (check whether national or euro!)");
//			for(Region region: Parameters.getCountryRegions()) {
//	    		for(Education edu: Education.values()) {
//					RegionEducationAtRiskOfWorkCSfilter regionEduAtRiskOfWorkFilter = new RegionEducationAtRiskOfWorkCSfilter(region, edu);
//					wagesAtRiskOfWorkCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getPotentialHourlyEarnings", true);
//					wagesAtRiskOfWorkCS.setFilter(regionEduAtRiskOfWorkFilter);
//					wagesAtRiskOfWorkCS.setCheckingTime(false);					//Need to set to false to enable updating during convergence process whilst the simulation time is still the same
//					Weighted_MeanArrayFunction meanWagesAtRiskOfWork = new Weighted_MeanArrayFunction(wagesAtRiskOfWorkCS);
//					meanWagesAtRiskOfWork.setCheckingTime(false);							
//					potentialEarningsAtRiskOfWorkPlotter.addSources("(" + region.getName() + ", " + edu.toString() + ")", meanWagesAtRiskOfWork, colorOfEducation(edu));
//				}
//			}
//			GuiUtils.addWindow(potentialEarningsAtRiskOfWorkPlotter, 1000, 200, 100 * Parameters.getCountryRegions().size(), 400);

			//POTENTIAL EARNINGS & LABOUR MARKET CONVERGENCE PLOTS
			int width = 400;
			int height = 300;
			Map<Education, Integer> chartXpos = new LinkedHashMap<>();
			Map<Education, Integer> chartYpos = new LinkedHashMap<>();
			for(Education edu: Education.values()) {
				int x = 450, y = 150;
				if(edu.equals(Education.Medium)) {
					x += width;
				}
				else if(edu.equals(Education.High)) {
					x += width*2;
				}
				chartXpos.put(edu, x);
				chartYpos.put(edu, y);
			}
			for(Education edu: Education.values()) {
				ScatterplotSimulationPlotterRefreshable labourPlot = new ScatterplotSimulationPlotterRefreshable(edu + " skill aggregate labour statistics", "iteration", "Hours per Week");
				labourPlot.setMaxSamples(labourMarketPlotMaxSamples);
		    	labourMarketPlots.put(edu, labourPlot);
		    	if(floatingConvergencePlots) {
		    		GuiUtils.addWindow(labourPlot, chartXpos.get(edu), chartYpos.get(edu), width, height);
		    	}
		    	else {
		    		convergencePlots.add(labourPlot);
		    	}
			}		    
			for(Education edu: Education.values()) {
				ScatterplotSimulationPlotterRefreshable potentialEarningsPlot = new ScatterplotSimulationPlotterRefreshable(edu + " skill mean potential earnings", "iteration", "currency (per hour)");
		    	potentialEarningsPlot.setMaxSamples(potentialEarningsPlotMaxSamples);
		    	potentialEarningsPlots.put(edu, potentialEarningsPlot);
		    	if(floatingConvergencePlots) {
		    		GuiUtils.addWindow(potentialEarningsPlot, chartXpos.get(edu), chartYpos.get(edu) + height, width, height);
		    	}
		    	else {
		    		convergencePlots.add(potentialEarningsPlot);
		    	}
			}
			
			
	    		    	
	    	//CREATE SCROLLABLE FRAME IN WHICH TO STORE ALL CONVERGENCE PLOTS FOR TIDY GUI
//			if(!floatingConvergencePlots) {
//							    
//				tabSet.add(createScrollPaneFromPlots(convergencePlots, "Labour Market Convergence", 3));
//				JInternalFrame convergenceFrame = new JInternalFrame("Labour Market Convergence Plots");				
//		    	convergenceFrame.add(createScrollPaneFromPlots(convergencePlots, "Labour Market Convergence Plots", 3));				
//				convergenceFrame.setResizable(true);
//				convergenceFrame.setMaximizable(true);
//				GuiUtils.addWindow(convergenceFrame, 500, 150, 1200, 600);
//				
//			}

			//This is the color palette used by graphs in the simulation
			ArrayList<Color> colorArrayList = new ArrayList<>();
			colorArrayList.add(new Color(162,56,255));
			colorArrayList.add(new Color(254, 131, 0));
			colorArrayList.add(new Color(151,144,0));
			colorArrayList.add(new Color(0,144,15));
			colorArrayList.add(new Color(0,53,144));
			colorArrayList.add(new Color(254,0,0));
			colorArrayList.add(new Color(198,0,190));
			colorArrayList.add(new Color(175,0,0));
			colorArrayList.add(new Color(0,0,0));
			colorArrayList.add(new Color(255, 172, 172));
			colorArrayList.add(new Color(255, 186, 132));
			colorArrayList.add(new Color(179, 129, 15));
			colorArrayList.add(new Color(175, 255, 148));
			colorArrayList.add(new Color(86, 173, 153));
			colorArrayList.add(new Color(0, 233, 255));
			
			//POPULATION CHART
			if(population) {

				// POPULATION PYRAMID GRAPH
				if (populationPyramid) {
					Set<JInternalFrame> populationPyramidPlots = new LinkedHashSet<JInternalFrame>();
					Weighted_PyramidPlotter populationAgeGenderPlotter = new Weighted_PyramidPlotter();
					// Please note that the Pyramid plotter requires a Weighted_CrossSection.Double[2]
					Weighted_CrossSection.Integer[] populationData = new Weighted_CrossSection.Integer[2];
					Weighted_CrossSection.Integer maleAgesCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "dag", false);
					maleAgesCS.setFilter(new GenderCSfilter(Gender.Male));
					populationData[0] = maleAgesCS;
					Weighted_CrossSection.Integer femaleAgesCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "dag", false);
					femaleAgesCS.setFilter(new GenderCSfilter(Gender.Female));
					populationData[1] = femaleAgesCS;

					populationAgeGenderPlotter.setScalingFactor(model.getScalingFactor());
					populationAgeGenderPlotter.addCollectionSource(populationData);

					updateChartSet.add(populationAgeGenderPlotter);			//Add to set to be updated in buildSchedule method
					populationPyramidPlots.add(populationAgeGenderPlotter);

					tabSet.add(createScrollPaneFromPlots(populationPyramidPlots, "Population Pyramid", 1));
				}

				TimeSeriesSimulationPlotter populationPlotter = new TimeSeriesSimulationPlotter("Population Statistics", "");
			//	populationPlotter.addSeries("num of simulated Persons", model, "getNumberOfSimulatedPersons", true);
			//	populationPlotter.addSeries("num of simulated Households", model, "getNumberOfSimulatedHouseholds", true);
				if (showAdditionalCharts) {
			//		populationPlotter.addSeries("Official population projection", model, "getPopulationProjection", true);
					populationPlotter.addSeries("(Scaled) Number of Households, occupants below 80 yo", model, "getWeightedNumberOfHouseholds80minus", true);
				}

				populationPlotter.addSeries("(Scaled) Population Size", model, "getWeightedNumberOfPersons", true);
			//	populationPlotter.addSeries("(Scaled) Number of Households", model, "getWeightedNumberOfHouseholds", true);


				populationPlotter.setName("Population statistics");
			    updateChartSet.add(populationPlotter);			//Add to set to be updated in buildSchedule method
			    tabSet.add(populationPlotter);
			}

			//Population share by age. Display if showing diagnostic charts.
			if(showAdditionalCharts) {
				Weighted_CrossSection.Integer population0_18CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
				population0_18CS.setFilter(age0_18Filter);
				Weighted_CrossSection.Integer population0_1CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
				population0_1CS.setFilter(age0Filter);
				Weighted_CrossSection.Integer population2_10CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
				population2_10CS.setFilter(age2_10Filter);
				Weighted_CrossSection.Integer population11_15CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
				population11_15CS.setFilter(age11_15Filter);
				Weighted_CrossSection.Integer population19_25CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
				population19_25CS.setFilter(age19_25Filter);
				Weighted_CrossSection.Integer population40_59CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
				population40_59CS.setFilter(age40_59Filter);
				Weighted_CrossSection.Integer population60_79CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
				population60_79CS.setFilter(age60_79Filter);
				Weighted_CrossSection.Integer population80_100CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getPersonCount", true);
				population80_100CS.setFilter(age80_100Filter);


				TimeSeriesSimulationPlotter populationAgePlotter = new TimeSeriesSimulationPlotter("Individuals by age", "");


				/*
				//To allow diferent series to be of different colours or shapes, would have to change the TimeSeriersSimulationPlotter class in the GUI to allow changes to the renderer?
				XYLineAndShapeRenderer r1 = new XYLineAndShapeRenderer();
				r1.setSeriesPaint(0, Color.yellow);
				*/

				populationAgePlotter.addSeries("0-18 yo", new Weighted_SumArrayFunction.Integer(population0_18CS), null, new Color (162, 56, 255), false);
				populationAgePlotter.addSeries("0 yo", new Weighted_SumArrayFunction.Integer(population0_1CS)  , null, new Color (254, 131, 0), false);
				populationAgePlotter.addSeries("2-10 yo", new Weighted_SumArrayFunction.Integer(population2_10CS), null, new Color (151, 144, 0), false);
				populationAgePlotter.addSeries("11-15 yo", new Weighted_SumArrayFunction.Integer(population11_15CS), null, new Color (0, 144, 15), false);
				populationAgePlotter.addSeries("19-25 yo", new Weighted_SumArrayFunction.Integer(population19_25CS), null, new Color (0, 53, 144), false);
				populationAgePlotter.addSeries("40-59 yo", new Weighted_SumArrayFunction.Integer(population40_59CS), null, new Color (254, 0, 0), false);
				populationAgePlotter.addSeries("60-79 yo", new Weighted_SumArrayFunction.Integer(population60_79CS), null, new Color (198, 0, 190), false);
				populationAgePlotter.addSeries("80-100 yo", new Weighted_SumArrayFunction.Integer(population80_100CS), null, new Color (175, 0, 0), false); 

				//Below the series from population projections for comparison:
				if (showValidationStatistics) {

					populationAgePlotter.addSeries("0 - 18 official projection", validator, Validator.DoublesVariables.populationProjectionsByAge_0_18, new Color (162, 56, 255), true);
					populationAgePlotter.addSeries("0 official projection", validator, Validator.DoublesVariables.populationProjectionsByAge_0_0, new Color (254, 131, 0), true);
					populationAgePlotter.addSeries("2 - 10 official projection", validator, Validator.DoublesVariables.populationProjectionsByAge_2_10, new Color (151, 144, 0), true);
					populationAgePlotter.addSeries("11 - 15 official projection", validator, Validator.DoublesVariables.populationProjectionsByAge_11_15,  new Color (0, 144, 15), true);
					populationAgePlotter.addSeries("19 - 25  official projection", validator, Validator.DoublesVariables.populationProjectionsByAge_19_25, new Color (0, 53, 144), true);
					populationAgePlotter.addSeries("40 - 59 official projection", validator, Validator.DoublesVariables.populationProjectionsByAge_40_59,  new Color (254, 0, 0), true);
					populationAgePlotter.addSeries("60 - 79 official projection", validator, Validator.DoublesVariables.populationProjectionsByAge_60_79,  new Color (198, 0, 190), true);
					populationAgePlotter.addSeries("80 - 100 official projection", validator, Validator.DoublesVariables.populationProjectionsByAge_80_100, new Color (175, 0, 0), true);
//					populationAgePlotter.addSeries("0 - 18 official projection", model, "getPopulationProjectionByAge0_18", true);
//					populationAgePlotter.addSeries("0 official projection", model, "getPopulationProjectionByAge0", true);
//					populationAgePlotter.addSeries("2 - 10 official projection", model, "getPopulationProjectionByAge2_10", true);
//					populationAgePlotter.addSeries("19 - 25 official projection", model, "getPopulationProjectionByAge19_25", true);
//					populationAgePlotter.addSeries("40 - 59 official projection", model, "getPopulationProjectionByAge40_59", true);
//					populationAgePlotter.addSeries("60 - 79 official projection", model, "getPopulationProjectionByAge60_79", true);
//					populationAgePlotter.addSeries("80 - 100 official projection", model, "getPopulationProjectionByAge80_100", true);

				}


				populationAgePlotter.setName("Individuals by age");
				updateChartSet.add(populationAgePlotter);			//Add to set to be updated in buildSchedule method
				tabSet.add(populationAgePlotter);

			}

			if (activityStatus) {
				Weighted_CrossSection.Integer employedCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isEmployed); //Get directly from the enum instead of going through a method
				Weighted_CrossSection.Integer notEmployedCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isNotEmployed);
				Weighted_CrossSection.Integer studentCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isStudent);
				Weighted_CrossSection.Integer retiredCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isRetired);
				Weighted_CrossSection.Integer notEmployedRetiredCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isNotEmployedOrRetired);

				TimeSeriesSimulationPlotter activityStatusPlotter = new TimeSeriesSimulationPlotter("Share of individuals by activity status", "");
				activityStatusPlotter.addSeries("Employed", new Weighted_MeanArrayFunction(employedCS), null, colorArrayList.get(0), false);
				activityStatusPlotter.addSeries("Not Employed / Retired", new Weighted_MeanArrayFunction(notEmployedRetiredCS), null, colorArrayList.get(1), false);
				activityStatusPlotter.addSeries("Not Employed", new Weighted_MeanArrayFunction(notEmployedCS), null, colorArrayList.get(4), false);
				activityStatusPlotter.addSeries("Student", new Weighted_MeanArrayFunction(studentCS), null, colorArrayList.get(2), false);
				activityStatusPlotter.addSeries("Retired", new Weighted_MeanArrayFunction(retiredCS), null, colorArrayList.get(3), false);

				activityStatusPlotter.addSeries("Employed validation", validator, Validator.DoublesVariables.activityStatus_Employed, colorArrayList.get(0), true);
				activityStatusPlotter.addSeries("Not Employed / Retired validation", validator, Validator.DoublesVariables.activityStatus_NotEmployedRetired, colorArrayList.get(1), true);
				activityStatusPlotter.addSeries("Student validation", validator, Validator.DoublesVariables.activityStatus_Student, colorArrayList.get(2), true);
		//		activityStatusPlotter.addSeries("Retired validation", validator, Validator.DoublesVariables.activityStatus_Retired, colorArrayList.get(3), true);

				activityStatusPlotter.setName("Activity status");
				updateChartSet.add(activityStatusPlotter);
				tabSet.add(activityStatusPlotter);
			}

			//HOMEOWNERSHIP STATUS
			if (homeownershipStatus) {
				Weighted_CrossSection.Double homeownersBUsCS = new Weighted_CrossSection.Double(model.getBenefitUnits(), BenefitUnit.Regressors.Homeownership_D);
				TimeSeriesSimulationPlotter homeownershipStatusPlotter = new TimeSeriesSimulationPlotter("Share of benefit units owning homes", "");
				homeownershipStatusPlotter.addSeries("Homeowners", new Weighted_MeanArrayFunction(homeownersBUsCS), null, colorArrayList.get(0), false);
				homeownershipStatusPlotter.addSeries("Homeowners validation", validator, Validator.DoublesVariables.homeownership_BenefitUnit, colorArrayList.get(0), true);
				homeownershipStatusPlotter.setName("Homeownership status");
				updateChartSet.add(homeownershipStatusPlotter);
				tabSet.add(homeownershipStatusPlotter);
			}

			//STUDENT ENROLLMENT CHARTS			
			if(studentsByAge) {
				//Students by Age Group
				Weighted_CrossSection.Integer student15_19CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getStudent", true);
				student15_19CS.setFilter(age15_19Filter);
				Weighted_CrossSection.Integer student20_24CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getStudent", true);
				student20_24CS.setFilter(age20_24Filter);
				Weighted_CrossSection.Integer student25_29CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getStudent", true);
				student25_29CS.setFilter(age25_29Filter);
				Weighted_CrossSection.Integer student30_34CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getStudent", true);
				student30_34CS.setFilter(age30_34Filter);
				Weighted_CrossSection.Integer student35_39CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getStudent", true);
				student35_39CS.setFilter(age35_39Filter);
				Weighted_CrossSection.Integer student40_59CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getStudent", true);
				student40_59CS.setFilter(age40_59Filter);
				Weighted_CrossSection.Integer student60_79CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getStudent", true);
				student60_79CS.setFilter(age60_79Filter);
				Weighted_CrossSection.Integer student80_100CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getStudent", true);
				student80_100CS.setFilter(age80_100Filter);
				
				//Unfiltered student cross-section (nationally, for all ages)
				Weighted_CrossSection.Integer studentCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getStudent", true);

				TimeSeriesSimulationPlotter studentAgePlotter = new TimeSeriesSimulationPlotter("Proportion of students by age", "");
				studentAgePlotter.setRenderer(studentAgeRenderer); //Assign new renderer to the graph before adding data series
			    studentAgePlotter.addSeries("15-19 yo", new Weighted_MeanArrayFunction(student15_19CS), null, new Color (162, 56, 255), false);		//'yo' means "years old"
			    studentAgePlotter.addSeries("20-24 yo", new Weighted_MeanArrayFunction(student20_24CS), null, new Color (254, 131, 0), false);
			    studentAgePlotter.addSeries("25-29 yo", new Weighted_MeanArrayFunction(student25_29CS), null, new Color (151, 144, 0), false);

			    if (showAdditionalCharts) {
					studentAgePlotter.addSeries("30-34 yo", new Weighted_MeanArrayFunction(student30_34CS), null, new Color (0, 144, 15), false);
					studentAgePlotter.addSeries("35-39 yo", new Weighted_MeanArrayFunction(student35_39CS), null, new Color (0, 53, 144), false);
					studentAgePlotter.addSeries("40-59 yo", new Weighted_MeanArrayFunction(student40_59CS), null, new Color (254, 0, 0), false);
					studentAgePlotter.addSeries("60-79 yo", new Weighted_MeanArrayFunction(student60_79CS), null, new Color (198, 0, 190), false);
					studentAgePlotter.addSeries("80-100 yo", new Weighted_MeanArrayFunction(student80_100CS), null, new Color (175, 0, 0), false);
					studentAgePlotter.addSeries("all ages", new Weighted_MeanArrayFunction(studentCS), null, new Color (0, 0, 0), false);
				}


			    studentAgePlotter.setName("Students by age");	
			    updateChartSet.add(studentAgePlotter);			//Add to set to be updated in buildSchedule method
			    tabSet.add(studentAgePlotter);

			    if (showValidationStatistics) {

					studentAgePlotter.addSeries("Validation 15-19 yo", validator, Validator.DoublesVariables.studentsByAge_15_19, new Color (162, 56, 255), true);
					studentAgePlotter.addSeries("Validation 20-24 yo", validator, Validator.DoublesVariables.studentsByAge_20_24, new Color (254, 131, 0), true);
					studentAgePlotter.addSeries("Validation 25-29 yo", validator, Validator.DoublesVariables.studentsByAge_25_29, new Color (151, 144, 0), true);

					if (showAdditionalCharts) {
						studentAgePlotter.addSeries("Validation 30-34 yo", validator, Validator.DoublesVariables.studentsByAge_30_34, new Color (0, 144, 15), true);
						studentAgePlotter.addSeries("Validation 35-39 yo", validator, Validator.DoublesVariables.studentsByAge_35_39, new Color (0, 53, 144), true);
						studentAgePlotter.addSeries("Validation 40-59 yo", validator, Validator.DoublesVariables.studentsByAge_40_59, new Color (254, 0, 0), true);
						studentAgePlotter.addSeries("Validation 60-79 yo", validator, Validator.DoublesVariables.studentsByAge_60_79, new Color (198, 0, 190), true);
						studentAgePlotter.addSeries("Validation 80-100 yo", validator, Validator.DoublesVariables.studentsByAge_80_100, new Color (175, 0, 0), true);
						studentAgePlotter.addSeries("Validation all ages", validator, Validator.DoublesVariables.studentsByAge_All, new Color (0, 0, 0), true);
					}

				}

			}

			if(studentsByRegion && showAdditionalCharts) {
			    //Student chart by Region 
			    TimeSeriesSimulationPlotter studentRegionPlotter = new TimeSeriesSimulationPlotter("Proportion of students by region", "");
			    int colorCounter = 0;
			    for(Region region: Parameters.getCountryRegions()) {
					RegionCSfilter regionFilter = new RegionCSfilter(region);
					Weighted_CrossSection.Integer regionCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getStudent", true);
					regionCS.setFilter(regionFilter);
			    	studentRegionPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(regionCS), null, colorArrayList.get(colorCounter), false);		//'yo' means "years old"
					if (showValidationStatistics) {
						studentRegionPlotter.addSeries("Validation "+region.getName(), validator, Validator.DoublesVariables.valueOf("studentsByRegion_"+region), colorArrayList.get(colorCounter), true);
					}
					colorCounter++;
			    }		    
			    studentRegionPlotter.setName("Students by region");
			    updateChartSet.add(studentRegionPlotter);			//Add to set to be updated in buildSchedule method
			    tabSet.add(studentRegionPlotter);


			}
			
			//EDUCATION LEVEL CHARTS
		    
		    //Education levels for all adults (18 years old and over)
			if(educationOfAdults) {
				ValidEducationAgeGroupCSfilter over17yoFilter = new ValidEducationAgeGroupCSfilter(18,100);		//So we exclude children
				Weighted_CrossSection.Integer lowEducationAdultCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getLowEducation", true);
				lowEducationAdultCS.setFilter(over17yoFilter);
				Weighted_CrossSection.Integer midEducationAdultCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getMidEducation", true);
				midEducationAdultCS.setFilter(over17yoFilter);
				Weighted_CrossSection.Integer highEducationAdultCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getHighEducation", true);
				highEducationAdultCS.setFilter(over17yoFilter);
			    
				TimeSeriesSimulationPlotter eduPlotter = new TimeSeriesSimulationPlotter("Education level of over-17 yo's \n(excluding students)", "");		//'yo' means "years old"
			    eduPlotter.addSeries("Low", new Weighted_MeanArrayFunction(lowEducationAdultCS), null, colorArrayList.get(0), false);
			    eduPlotter.addSeries("Medium", new Weighted_MeanArrayFunction(midEducationAdultCS), null, colorArrayList.get(1), false);
			    eduPlotter.addSeries("High", new Weighted_MeanArrayFunction(highEducationAdultCS), null, colorArrayList.get(2), false);
			    eduPlotter.setName("Education");
			    updateChartSet.add(eduPlotter);			//Add to set to be updated in buildSchedule method
			    tabSet.add(eduPlotter);

			    if (showValidationStatistics) {
			    	eduPlotter.addSeries("Validation Low", validator, Validator.DoublesVariables.educationLevelLow, colorArrayList.get(0), true);
					eduPlotter.addSeries("Validation Medium", validator, Validator.DoublesVariables.educationLevelMedium, colorArrayList.get(1), true);
					eduPlotter.addSeries("Validation High", validator, Validator.DoublesVariables.educationLevelHigh, colorArrayList.get(2), true);
				}
			}
			
		    //Education levels by age groups
			if(educationByAge && showAdditionalCharts) {
			    Set<JInternalFrame> eduAgePlots = new LinkedHashSet<JInternalFrame>();
			    for(ValidEducationAgeGroupCSfilter ageFilter : decadeValidEducationAgeGroupFilterSet) {
			    	
					Weighted_CrossSection.Integer lowEducationCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getLowEducation", true);
					lowEducationCS.setFilter(ageFilter);
					Weighted_CrossSection.Integer midEducationCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getMidEducation", true);
					midEducationCS.setFilter(ageFilter);
					Weighted_CrossSection.Integer highEducationCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getHighEducation", true);
					highEducationCS.setFilter(ageFilter);
					
					TimeSeriesSimulationPlotter eduAgePlotter = new TimeSeriesSimulationPlotter("Education level by age: " + ageFilter.getAgeFrom() + " - " + ageFilter.getAgeTo() + "\n(excluding students)", "");		//'yo' means "years old"
				    eduAgePlotter.addSeries("low", new Weighted_MeanArrayFunction(lowEducationCS), null, colorArrayList.get(0), false);
				    eduAgePlotter.addSeries("mid", new Weighted_MeanArrayFunction(midEducationCS), null, colorArrayList.get(1), false);
				    eduAgePlotter.addSeries("high", new Weighted_MeanArrayFunction(highEducationCS), null, colorArrayList.get(2), false);

					if (showValidationStatistics) {
						eduAgePlotter.addSeries("Validation Low", validator, Validator.DoublesVariables.valueOf("educationLevelLowByAge_"+ageFilter.getAgeFrom()+"_"+ageFilter.getAgeTo()), colorArrayList.get(0), true); //Note the use of valueOf instead of calling the enum in validator's doubles variables directly
						eduAgePlotter.addSeries("Validation Medium", validator, Validator.DoublesVariables.valueOf("educationLevelMediumByAge_"+ageFilter.getAgeFrom()+"_"+ageFilter.getAgeTo()), colorArrayList.get(1), true);
						eduAgePlotter.addSeries("Validation High", validator, Validator.DoublesVariables.valueOf("educationLevelHighByAge_"+ageFilter.getAgeFrom()+"_"+ageFilter.getAgeTo()), colorArrayList.get(2), true);
					}

					updateChartSet.add(eduAgePlotter);			//Add to set to be updated in buildSchedule method
					eduAgePlots.add(eduAgePlotter);


			    }
			    tabSet.add(createScrollPaneFromPlots(eduAgePlots, "Education by age", 2));
			}
			
		    //Low & High Education By Region
			if(educationByRegion && showAdditionalCharts) {
			    Set<JInternalFrame> eduLowHighRegionalPlots = new LinkedHashSet<JInternalFrame>();
	
			    //Low Education by region
			    TimeSeriesSimulationPlotter eduLowRegionPlotter = new TimeSeriesSimulationPlotter("Low education level by region", "");
				int colorCounter = 0;
			    for(Region region: Parameters.getCountryRegions()) {
					ValidEducationRegionCSfilter regionFilter = new ValidEducationRegionCSfilter(region);
					Weighted_CrossSection.Integer regionCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getLowEducation", true);
					regionCS.setFilter(regionFilter);
					eduLowRegionPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(regionCS), null, colorArrayList.get(colorCounter), false);		//'yo' means "years old"
					eduLowRegionPlotter.addSeries("Validation "+region.getName(), validator, Validator.DoublesVariables.valueOf("educationLevelLowByRegion_"+region), colorArrayList.get(colorCounter), true);
					colorCounter++;
			    }		    			    
			    updateChartSet.add(eduLowRegionPlotter);			//Add to set to be updated in buildSchedule method
			    eduLowHighRegionalPlots.add(eduLowRegionPlotter);
			    
			    //High Education by region
			    TimeSeriesSimulationPlotter eduHighRegionPlotter = new TimeSeriesSimulationPlotter("High education level by region", "");
			    colorCounter = 0; //Reset the color counter
			    for(Region region: Parameters.getCountryRegions()) {
					ValidEducationRegionCSfilter regionFilter = new ValidEducationRegionCSfilter(region);
					Weighted_CrossSection.Integer regionCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getHighEducation", true);
					regionCS.setFilter(regionFilter);
					eduHighRegionPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(regionCS), null, colorArrayList.get(colorCounter), false);		//'yo' means "years old"
					eduHighRegionPlotter.addSeries("Validation "+region.getName(), validator, Validator.DoublesVariables.valueOf("educationLevelHighByRegion_"+region), colorArrayList.get(colorCounter), true);
					colorCounter++;
			    }		    			    
			    updateChartSet.add(eduHighRegionPlotter);			//Add to set to be updated in buildSchedule method
			    eduLowHighRegionalPlots.add(eduHighRegionPlotter);
			    
			    tabSet.add(createScrollPaneFromPlots(eduLowHighRegionalPlots, "Education by region (excluding students)", 2));
			}		    
		    
		    
		    //HOUSEHOLD COMPOSITION CHART
		    if(householdComposition) {
			    //Proportion of households with couple occupancy (i.e. there is both a responsible male and female in the household) by region
			    TimeSeriesSimulationPlotter houseCompositionRegionPlotter = new TimeSeriesSimulationPlotter("Share of couples", "");
				int colorCounter = 0;
			    for(Region region: Parameters.getCountryRegions()) {
					RegionCSfilter regionFilter = new RegionCSfilter(region);
					Weighted_CrossSection.Integer regionCS = new Weighted_CrossSection.Integer(model.getBenefitUnits(), BenefitUnit.class, "getCoupleDummy", true);
					regionCS.setFilter(regionFilter);
					houseCompositionRegionPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(regionCS), null, colorArrayList.get(colorCounter), false);		//'yo' means "years old"
					houseCompositionRegionPlotter.addSeries("Validation "+region.getName(), validator, Validator.DoublesVariables.valueOf("partneredShare_"+region), colorArrayList.get(colorCounter), true);
					colorCounter++;
			    }		    
			    Weighted_CrossSection.Integer coupleCS = new Weighted_CrossSection.Integer(model.getBenefitUnits(), BenefitUnit.class, "getCoupleDummy", true);
			    houseCompositionRegionPlotter.addSeries("national", new Weighted_MeanArrayFunction(coupleCS), null, colorArrayList.get(colorCounter), false);		//'yo' means "years old"
				houseCompositionRegionPlotter.addSeries("Validation national", validator, Validator.DoublesVariables.valueOf("partneredShare_All"), colorArrayList.get(colorCounter), true);
			    houseCompositionRegionPlotter.setName("Cohabitation status");
			    updateChartSet.add(houseCompositionRegionPlotter);			//Add to set to be updated in buildSchedule method
			    tabSet.add(houseCompositionRegionPlotter);

			    /*
			    //Add a graph showing share of individuals with a partner:
				TimeSeriesSimulationPlotter personsWithPartnerPlotter = new TimeSeriesSimulationPlotter("Share of individuals with partner", "");
				Weighted_CrossSection.Integer personsWithPartner = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getCoupleDummy", true);
				personsWithPartnerPlotter.addSeries("Share of persons with partner", new Weighted_MeanArrayFunction(personsWithPartner));
				personsWithPartnerPlotter.setName("Share of persons with partner");
				updateChartSet.add(personsWithPartnerPlotter);
				tabSet.add(personsWithPartnerPlotter);
				*/

		    }

			//Number of males and females who want to cohabit
			if (householdComposition) {
				TimeSeriesSimulationPlotter cohabitationDesireByGender = new TimeSeriesSimulationPlotter("Individuals looking for partner, by gender", "");
				Weighted_CrossSection.Integer toBePartneredMales = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isToBePartnered);
				Weighted_CrossSection.Integer toBePartneredFemales = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isToBePartnered);
				toBePartneredMales.setFilter(new GenderCSfilter(Gender.Male));
				toBePartneredFemales.setFilter(new GenderCSfilter(Gender.Female));
				cohabitationDesireByGender.addSeries("Males", new Weighted_SumArrayFunction.Integer(toBePartneredMales), null, colorArrayList.get(0), false);
				cohabitationDesireByGender.addSeries("Females", new Weighted_SumArrayFunction.Integer(toBePartneredFemales), null, colorArrayList.get(1), false);
				cohabitationDesireByGender.setName("Individuals looking for partner");
				updateChartSet.add(cohabitationDesireByGender);
				tabSet.add(cohabitationDesireByGender);
			}
		    
			MaleAgeGroupCSfilter males18_64Filter = new MaleAgeGroupCSfilter(18, 64);
			FemaleAgeGroupCSfilter females18_64Filter = new FemaleAgeGroupCSfilter(18, 64);

		    //HEALTH CHARTS
			
			//Overall population by gender - because it is aligned, this chart is fairly boring, so do by age group below to provide more detailed information.
//		    if(health) {
////				MaleAgeGroupCSfilter males18_64Filter = new MaleAgeGroupCSfilter(18, 64);					
//				Weighted_CrossSection.Integer males18_64CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getGoodHealth", true);
//				males18_64CS.setFilter(males18_64Filter);
////				FemaleAgeGroupCSfilter females18_64Filter = new FemaleAgeGroupCSfilter(18, 64);
//				Weighted_CrossSection.Integer females18_64CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getGoodHealth", true);
//				females18_64CS.setFilter(females18_64Filter);
//				TimeSeriesSimulationPlotter healthPlotter = new TimeSeriesSimulationPlotter("Good Health proportion (18 - 64)", "");
//			    healthPlotter.addSeries("males", new Weighted_MeanArrayFunction(males18_64CS));
//			    healthPlotter.addSeries("females", new Weighted_MeanArrayFunction(females18_64CS));		 
//			    healthPlotter.setName("Health");
//				updateChartSet.add(healthPlotter);			//Add to set to be updated in buildSchedule method
//				tabSet.add(healthPlotter);
//		    }

		    //Male/Female health by age groups
		    if(healthByAge) {
//			    Set<JInternalFrame> healthAgePlots = new LinkedHashSet<JInternalFrame>();
//			    for(AgeGroupCSfilter ageFilter : decadeAgeGroupFilterSet) {
//			    	int ageFrom = ageFilter.getAgeFrom();
//			    	int ageTo = ageFilter.getAgeTo();
//			    	
//			    	MaleAgeGroupCSfilter maleAgeFilter = new MaleAgeGroupCSfilter(ageFrom, ageTo);
//			    	FemaleAgeGroupCSfilter femaleAgeFilter = new FemaleAgeGroupCSfilter(ageFrom, ageTo);
//			    	Weighted_CrossSection.Integer maleCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getGoodHealth", true);
//					maleCS.setFilter(maleAgeFilter);
//					Weighted_CrossSection.Integer femaleCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getGoodHealth", true);
//					femaleCS.setFilter(femaleAgeFilter);
//	
//					TimeSeriesSimulationPlotter healthAgePlotter = new TimeSeriesSimulationPlotter("Proportion with good health: ages " + ageFilter.getAgeFrom() + " - " + ageFilter.getAgeTo(), "");
//				    healthAgePlotter.addSeries("males", new Weighted_MeanArrayFunction(maleCS));
//				    healthAgePlotter.addSeries("females", new Weighted_MeanArrayFunction(femaleCS));
//				    
//					updateChartSet.add(healthAgePlotter);			//Add to set to be updated in buildSchedule method
//					healthAgePlots.add(healthAgePlotter);
//				}
//			    tabSet.add(createScrollPaneFromPlots(healthAgePlots, "Health: age/gender", 2));			

				Set<JInternalFrame> disabledAgePlots = new LinkedHashSet<>();
				//		for (AgeGroupCSfilter ageFilter : disabledHealthAgeGroupFilterSet) {
				//			int ageFrom = ageFilter.getAgeFrom();
				//			int ageTo = ageFilter.getAgeTo();

				MaleAgeGroupCSfilter maleAgeFilterDisabled = new MaleAgeGroupCSfilter(16, 100);
				FemaleAgeGroupCSfilter femaleAgeFilterDisabled = new FemaleAgeGroupCSfilter(16, 100);
				Weighted_CrossSection.Integer maleCSDisabled = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getBadHealth", true);
				maleCSDisabled.setFilter(maleAgeFilterDisabled);
				Weighted_CrossSection.Integer femaleCSDisabled = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getBadHealth", true);
				femaleCSDisabled.setFilter(femaleAgeFilterDisabled);

				TimeSeriesSimulationPlotter disabledAgePlotter = new TimeSeriesSimulationPlotter("Disability rate", "");
				disabledAgePlotter.addSeries("males", new Weighted_MeanArrayFunction(maleCSDisabled), null, colorArrayList.get(0), false);
				disabledAgePlotter.addSeries("females", new Weighted_MeanArrayFunction(femaleCSDisabled), null, colorArrayList.get(1), false);
				disabledAgePlotter.addSeries("Validation males", validator, Validator.DoublesVariables.valueOf("disabledMale"), colorArrayList.get(0), true);
				disabledAgePlotter.addSeries("Validation females", validator, Validator.DoublesVariables.valueOf("disabledFemale"), colorArrayList.get(1), true);

				updateChartSet.add(disabledAgePlotter);
				disabledAgePlots.add(disabledAgePlotter);
				//		}


				tabSet.add(createScrollPaneFromPlots(disabledAgePlots, "Disability: gender", 2));

				Set<JInternalFrame> healthAgePlots = new LinkedHashSet<>();
				for (AgeGroupCSfilter ageFilter : disabledHealthAgeGroupFilterSet) {
					int ageFrom = ageFilter.getAgeFrom();
					int ageTo = ageFilter.getAgeTo();

					MaleAgeGroupCSfilter maleAgeFilter = new MaleAgeGroupCSfilter(ageFrom, ageTo);
					FemaleAgeGroupCSfilter femaleAgeFilter = new FemaleAgeGroupCSfilter(ageFrom, ageTo);
					Weighted_CrossSection.Double maleCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getDheValue", true);
					maleCS.setFilter(maleAgeFilter);
					Weighted_CrossSection.Double femaleCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getDheValue", true);
					femaleCS.setFilter(femaleAgeFilter);

					TimeSeriesSimulationPlotter healthAgePlotter = new TimeSeriesSimulationPlotter("Health score by age: " + ageFilter.getAgeFrom() + " - " + ageFilter.getAgeTo(), "");
					healthAgePlotter.addSeries("males", new Weighted_MeanArrayFunction(maleCS), null, colorArrayList.get(0), false);
					healthAgePlotter.addSeries("females", new Weighted_MeanArrayFunction(femaleCS), null, colorArrayList.get(1), false);
					healthAgePlotter.addSeries("Validation males", validator, Validator.DoublesVariables.valueOf("healthMale_" + ageFrom + "_" + ageTo), colorArrayList.get(0), true);
					healthAgePlotter.addSeries("Validation females", validator, Validator.DoublesVariables.valueOf("healthFemale_" + ageFrom + "_" + ageTo), colorArrayList.get(1), true);

					updateChartSet.add(healthAgePlotter);
					healthAgePlots.add(healthAgePlotter);
				}

				tabSet.add(createScrollPaneFromPlots(healthAgePlots, "Health: age/gender", 2));

				Set<JInternalFrame> healthMentalAgePlots = new LinkedHashSet<>();
				for (AgeGroupCSfilter ageFilter : healthMentalAgeGroupFilterSet) {
					int ageFrom = ageFilter.getAgeFrom();
					int ageTo = ageFilter.getAgeTo();

					MaleAgeGroupCSfilter maleAgeFilter = new MaleAgeGroupCSfilter(ageFrom, ageTo);
					FemaleAgeGroupCSfilter femaleAgeFilter = new FemaleAgeGroupCSfilter(ageFrom, ageTo);
					Weighted_CrossSection.Double maleCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getDhm", true);
					maleCS.setFilter(maleAgeFilter);
					Weighted_CrossSection.Double femaleCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getDhm", true);
					femaleCS.setFilter(femaleAgeFilter);

					TimeSeriesSimulationPlotter healthMentalAgePlotter = new TimeSeriesSimulationPlotter("Psychological distress score by age: " + ageFilter.getAgeFrom() + " - " + ageFilter.getAgeTo(), "");
					healthMentalAgePlotter.addSeries("males", new Weighted_MeanArrayFunction(maleCS), null, colorArrayList.get(0), false);
					healthMentalAgePlotter.addSeries("females", new Weighted_MeanArrayFunction(femaleCS), null, colorArrayList.get(1), false);
					healthMentalAgePlotter.addSeries("Validation males", validator, Validator.DoublesVariables.valueOf("mentalHealthMale_" + ageFrom + "_" + ageTo), colorArrayList.get(0), true);
					healthMentalAgePlotter.addSeries("Validation females", validator, Validator.DoublesVariables.valueOf("mentalHealthFemale_" + ageFrom + "_" + ageTo), colorArrayList.get(1), true);

					updateChartSet.add(healthMentalAgePlotter);
					healthMentalAgePlots.add(healthMentalAgePlotter);
				}

				tabSet.add(createScrollPaneFromPlots(healthMentalAgePlots, "Psychological distress score: age/gender", 2));

				Set<JInternalFrame> psychologicalDistressCasesAgePlots = new LinkedHashSet<>();
				for (AgeGroupCSfilter ageFilter : healthMentalAgeGroupFilterSet) {
					int ageFrom = ageFilter.getAgeFrom();
					int ageTo = ageFilter.getAgeTo();

					MaleAgeGroupCSfilter maleAgeFilter = new MaleAgeGroupCSfilter(ageFrom, ageTo);
					FemaleAgeGroupCSfilter femaleAgeFilter = new FemaleAgeGroupCSfilter(ageFrom, ageTo);
					Weighted_CrossSection.Integer maleCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isPsychologicallyDistressed);
					maleCS.setFilter(maleAgeFilter);
					Weighted_CrossSection.Integer femaleCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isPsychologicallyDistressed);
					femaleCS.setFilter(femaleAgeFilter);

					TimeSeriesSimulationPlotter psychDistressAgePlotter = new TimeSeriesSimulationPlotter("Share in psychological distress by age: " + ageFilter.getAgeFrom() + " - " + ageFilter.getAgeTo(), "");
					psychDistressAgePlotter.addSeries("males", new Weighted_MeanArrayFunction(maleCS), null, colorArrayList.get(0), false);
					psychDistressAgePlotter.addSeries("females", new Weighted_MeanArrayFunction(femaleCS), null, colorArrayList.get(1), false);
					psychDistressAgePlotter.addSeries("Validation males", validator, Validator.DoublesVariables.valueOf("psychDistressMale_" + ageFrom + "_" + ageTo), colorArrayList.get(0), true);
					psychDistressAgePlotter.addSeries("Validation females", validator, Validator.DoublesVariables.valueOf("psychDistressFemale_" + ageFrom + "_" + ageTo), colorArrayList.get(1), true);

					updateChartSet.add(psychDistressAgePlotter);
					psychologicalDistressCasesAgePlots.add(psychDistressAgePlotter);
				}

				tabSet.add(createScrollPaneFromPlots(psychologicalDistressCasesAgePlots, "Share in psychological distress (case-based): age/gender", 2));
				
				// Psychological distress (case-based) by education
				Set<JInternalFrame> psychologicalDistressCasesAgeEducationPlots = new LinkedHashSet<>();
				for (Education education : Education.values()) {
					for (AgeGroupCSfilter ageFilter : healthMentalAgeGroupFilterSet) {
						int ageFrom = ageFilter.getAgeFrom();
						int ageTo = ageFilter.getAgeTo();

						MaleAgeGroupEducationCSfilter maleAgeEducationFilter = new MaleAgeGroupEducationCSfilter(ageFrom, ageTo, education);
						FemaleAgeGroupEducationCSfilter femaleAgeEducationFilter = new FemaleAgeGroupEducationCSfilter(ageFrom, ageTo, education);
						Weighted_CrossSection.Integer maleCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isPsychologicallyDistressed);
						maleCS.setFilter(maleAgeEducationFilter);
						Weighted_CrossSection.Integer femaleCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isPsychologicallyDistressed);
						femaleCS.setFilter(femaleAgeEducationFilter);

						TimeSeriesSimulationPlotter psychDistressAgeEducationPlotter = new TimeSeriesSimulationPlotter("Share in psychological distress by age: " + ageFilter.getAgeFrom() + " - " + ageFilter.getAgeTo(), "");
						psychDistressAgeEducationPlotter.addSeries("males " + education + " educ", new Weighted_MeanArrayFunction(maleCS), null, colorArrayList.get(0), false);
						psychDistressAgeEducationPlotter.addSeries("females " + education + " educ", new Weighted_MeanArrayFunction(femaleCS), null, colorArrayList.get(1), false);
						psychDistressAgeEducationPlotter.addSeries("Validation males", validator, Validator.DoublesVariables.valueOf("psychDistressMale_"+ageFrom+"_"+ageTo), colorArrayList.get(0), true);
						psychDistressAgeEducationPlotter.addSeries("Validation females", validator, Validator.DoublesVariables.valueOf("psychDistressFemale_"+ageFrom+"_"+ageTo), colorArrayList.get(1), true);
						updateChartSet.add(psychDistressAgeEducationPlotter);
						psychologicalDistressCasesAgeEducationPlots.add(psychDistressAgeEducationPlotter);
					}
			}

				tabSet.add(createScrollPaneFromPlots(psychologicalDistressCasesAgeEducationPlots, "Share in psychological distress (case-based): age/gender/education", 2));

				// Psychological distress (case-based) by education
				Set<JInternalFrame> psychologicalDistressCasesEducationPlots = new LinkedHashSet<>();
				for (Education education : Education.values()) {
						int ageFrom = 25;
						int ageTo = 64;

						MaleAgeGroupEducationCSfilter maleAgeEducationFilter = new MaleAgeGroupEducationCSfilter(ageFrom, ageTo, education);
						FemaleAgeGroupEducationCSfilter femaleAgeEducationFilter = new FemaleAgeGroupEducationCSfilter(ageFrom, ageTo, education);
						Weighted_CrossSection.Integer maleCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isPsychologicallyDistressed);
						maleCS.setFilter(maleAgeEducationFilter);
						Weighted_CrossSection.Integer femaleCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.IntegerVariables.isPsychologicallyDistressed);
						femaleCS.setFilter(femaleAgeEducationFilter);

						TimeSeriesSimulationPlotter psychDistressEducationPlotter = new TimeSeriesSimulationPlotter("Share in psychological distress by education:", "");
						psychDistressEducationPlotter.addSeries("males " + education + " educ", new Weighted_MeanArrayFunction(maleCS), null, colorArrayList.get(0), false);
						psychDistressEducationPlotter.addSeries("females " + education + " educ", new Weighted_MeanArrayFunction(femaleCS), null, colorArrayList.get(1), false);
						//	psychDistressAgeEducationPlotter.addSeries("Validation males", validator, Validator.DoublesVariables.valueOf("psychDistressMale_"+ageFrom+"_"+ageTo), colorArrayList.get(0), true);
						//	psychDistressAgeEducationPlotter.addSeries("Validation females", validator, Validator.DoublesVariables.valueOf("psychDistressFemale_"+ageFrom+"_"+ageTo), colorArrayList.get(1), true);

						updateChartSet.add(psychDistressEducationPlotter);
						psychologicalDistressCasesEducationPlots.add(psychDistressEducationPlotter);
					}

				tabSet.add(createScrollPaneFromPlots(psychologicalDistressCasesEducationPlots, "Share in psychological distress (case-based): gender/education", 2));
				/*
				TimeSeriesSimulationPlotter disabledAgePlotter = new TimeSeriesSimulationPlotter("Proportion of long-term sick or disabled by age & gender", "");
			    disabledAgePlotter.setName("Disabled: age/gender");
			    for(AgeGroupCSfilter ageFilter : decadeAgeGroupFilterSet) {
			    	int ageFrom = ageFilter.getAgeFrom();
			    	int ageTo = ageFilter.getAgeTo();
			    	
			    	MaleAgeGroupCSfilter maleAgeFilter = new MaleAgeGroupCSfilter(ageFrom, ageTo);
			    	FemaleAgeGroupCSfilter femaleAgeFilter = new FemaleAgeGroupCSfilter(ageFrom, ageTo);
			    	Weighted_CrossSection.Integer maleCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getBadHealth", true);
					maleCS.setFilter(maleAgeFilter);
					Weighted_CrossSection.Integer femaleCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getBadHealth", true);
					femaleCS.setFilter(femaleAgeFilter);
				
				    disabledAgePlotter.addSeries("males (" + ageFrom + " - " + ageTo + ")", new Weighted_MeanArrayFunction(maleCS));
				    disabledAgePlotter.addSeries("females (" + ageFrom + " - " + ageTo + ")", new Weighted_MeanArrayFunction(femaleCS));
				    
				}
				updateChartSet.add(disabledAgePlotter);			//Add to set to be updated in buildSchedule method
			    tabSet.add(disabledAgePlotter);	



			    //Chart for continuous health status below:
			    TimeSeriesSimulationPlotter healthAgePlotter = new TimeSeriesSimulationPlotter("Health status by age & gender", "");
			    healthAgePlotter.setName("Health: age/gender");
			    for(AgeGroupCSfilter ageFilter : decadeAgeGroupFilterSet) {
			    	int ageFrom = ageFilter.getAgeFrom();
			    	int ageTo = ageFilter.getAgeTo();
			    	
			    	MaleAgeGroupCSfilter maleAgeFilter = new MaleAgeGroupCSfilter(ageFrom, ageTo);
			    	FemaleAgeGroupCSfilter femaleAgeFilter = new FemaleAgeGroupCSfilter(ageFrom, ageTo);
			    	Weighted_CrossSection.Double maleCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getDhe", true);
					maleCS.setFilter(maleAgeFilter);
					Weighted_CrossSection.Double femaleCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getDhe", true);
					femaleCS.setFilter(femaleAgeFilter);
				
				    healthAgePlotter.addSeries("males (" + ageFrom + " - " + ageTo + ")", new Weighted_MeanArrayFunction(maleCS));
				    healthAgePlotter.addSeries("females (" + ageFrom + " - " + ageTo + ")", new Weighted_MeanArrayFunction(femaleCS));
			    }
			    
			    updateChartSet.add(healthAgePlotter);
			    tabSet.add(healthAgePlotter);

		    	*/
		    }
		    
		    
		    //EMPLOYMENT CHARTS
		    if(employmentOfAdults) {
//				MaleAgeGroupCSfilter males18_64Filter = new MaleAgeGroupCSfilter(18, 64);
				FlexibleInLabourSupplyByAgeAndGenderFilter maleAgeFilter = new FlexibleInLabourSupplyByAgeAndGenderFilter(18, 64, Gender.Male);
				Weighted_CrossSection.Integer males18_64CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
				males18_64CS.setFilter(males18_64Filter);
//				males18_64CS.setFilter(maleAgeFilter);
				
//				Weighted_CrossSection.Integer males18_64CSNE = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getNonwork", true);
//				males18_64CSNE.setFilter(males18_64Filter);
				
//				FemaleAgeGroupCSfilter females18_64Filter = new FemaleAgeGroupCSfilter(18, 64);
				FlexibleInLabourSupplyByAgeAndGenderFilter femaleAgeFilter = new FlexibleInLabourSupplyByAgeAndGenderFilter(18, 64, Gender.Female);
				Weighted_CrossSection.Integer females18_64CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
				females18_64CS.setFilter(females18_64Filter);
//				females18_64CS.setFilter(femaleAgeFilter);

				TimeSeriesSimulationPlotter emplPlotter = new TimeSeriesSimulationPlotter("Employment rate (18 - 64)", "");
			    emplPlotter.addSeries("males", new Weighted_MeanArrayFunction(males18_64CS), null, colorArrayList.get(0), false);
//			    emplPlotter.addSeries("males_NE", new Weighted_MeanArrayFunction(males18_64CSNE));
			    emplPlotter.addSeries("females", new Weighted_MeanArrayFunction(females18_64CS), null, colorArrayList.get(1), false);
			    emplPlotter.addSeries("Validation males", validator, Validator.DoublesVariables.employmentMale, colorArrayList.get(0), true);
				emplPlotter.addSeries("Validation females", validator, Validator.DoublesVariables.employmentFemale, colorArrayList.get(1), true);

			    emplPlotter.setName("Employment");
				updateChartSet.add(emplPlotter);			//Add to set to be updated in buildSchedule method
				tabSet.add(emplPlotter);
		    }
		    
		    //Male/Female employment rates by age groups
		    if(employmentByAge) {
			    Set<JInternalFrame> emplAgePlots = new LinkedHashSet<JInternalFrame>();
			    for(AgeGroupCSfilter ageFilter : decadeAgeGroupFilterSet) {
			    	int ageFrom = ageFilter.getAgeFrom();
			    	int ageTo = ageFilter.getAgeTo();
			    	
			    	MaleAgeGroupCSfilter maleAgeFilter = new MaleAgeGroupCSfilter(ageFrom, ageTo);
//					FlexibleInLabourSupplyByAgeAndGenderFilter maleAgeFilter = new FlexibleInLabourSupplyByAgeAndGenderFilter(ageFrom, ageTo, Gender.Male); //New filter to obtain a cross-section of those who are flexible in labour supply
			    	FemaleAgeGroupCSfilter femaleAgeFilter = new FemaleAgeGroupCSfilter(ageFrom, ageTo);
//					FlexibleInLabourSupplyByAgeAndGenderFilter femaleAgeFilter = new FlexibleInLabourSupplyByAgeAndGenderFilter(ageFrom, ageTo, Gender.Female);
			    	Weighted_CrossSection.Integer maleCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
					maleCS.setFilter(maleAgeFilter);
					Weighted_CrossSection.Integer femaleCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
					femaleCS.setFilter(femaleAgeFilter);
	
					TimeSeriesSimulationPlotter emplAgePlotter = new TimeSeriesSimulationPlotter("Employment rate by age: " + ageFilter.getAgeFrom() + " - " + ageFilter.getAgeTo(), "");
				    emplAgePlotter.addSeries("males", new Weighted_MeanArrayFunction(maleCS), null, colorArrayList.get(0), false);
				    emplAgePlotter.addSeries("females", new Weighted_MeanArrayFunction(femaleCS), null, colorArrayList.get(1), false);
					emplAgePlotter.addSeries("Validation males", validator, Validator.DoublesVariables.valueOf("employmentMaleByAge_"+ageFrom+"_"+ageTo), colorArrayList.get(0), true);
					emplAgePlotter.addSeries("Validation females", validator, Validator.DoublesVariables.valueOf("employmentFemaleByAge_"+ageFrom+"_"+ageTo), colorArrayList.get(1), true);
					updateChartSet.add(emplAgePlotter);			//Add to set to be updated in buildSchedule method
					emplAgePlots.add(emplAgePlotter);
				}
			    tabSet.add(createScrollPaneFromPlots(emplAgePlots, "Employment: age/gender", 2));
		    }

		    /*
		    if(femaleEmploymentByMaternity) {
				//Females with/without children by two lowest age groups
			    Set<JInternalFrame> emplAgeMaternityPlots = new LinkedHashSet<JInternalFrame>();
			    for(AgeGroupCSfilter ageFilter : decadeAgeGroupFilterSet) {
			    	int ageFrom = ageFilter.getAgeFrom();
			    	int ageTo = ageFilter.getAgeTo();
			    	
			    	if(ageFrom == 20 || ageFrom == 30) {		//Only do the lowest 2 decade age groups
						FemalesWithChildrenAgeGroupCSfilter withChildrenFilter = new FemalesWithChildrenAgeGroupCSfilter(ageFrom, ageTo);
						FemalesWithoutChildrenAgeGroupCSfilter withoutChildrenFilter = new FemalesWithoutChildrenAgeGroupCSfilter(ageFrom, ageTo);
						Weighted_CrossSection.Integer withChildrenCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
						withChildrenCS.setFilter(withChildrenFilter);
						Weighted_CrossSection.Integer withoutChildrenCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
						withoutChildrenCS.setFilter(withoutChildrenFilter);
						
						TimeSeriesSimulationPlotter emplChildPlotter = new TimeSeriesSimulationPlotter("Female employment rate: maternity for ages " + ageFrom + " - " + ageTo, "");
					    emplChildPlotter.addSeries("with children under 18 yo", new Weighted_MeanArrayFunction(withChildrenCS));
					    emplChildPlotter.addSeries("without children under 18 yo", new Weighted_MeanArrayFunction(withoutChildrenCS));		    
						updateChartSet.add(emplChildPlotter);			//Add to set to be updated in buildSchedule method
						emplAgeMaternityPlots.add(emplChildPlotter);
			    	}
			    }
			    tabSet.add(createScrollPaneFromPlots(emplAgeMaternityPlots, "Employment (female): age/maternity", 2));
		    }
			*/


		    //One graph for employment age by maternity status, conditional on age of children
		    if (femaleEmploymentByMaternity) {
				Set<JInternalFrame> emplAgeMaternityPlots = new LinkedHashSet<JInternalFrame>();
//				for(AgeGroupCSfilter ageFilter : decadeAgeGroupFilterSet) {
//					int ageFrom = ageFilter.getAgeFrom();
//					int ageTo = ageFilter.getAgeTo();

//					if(ageFrom == 20 || ageFrom == 30 || ageFrom == 40) {		//Only do the lowest 3 decade age groups

						FemalesWithoutChildrenAgeGroupCSfilter withoutChildrenFilter = new FemalesWithoutChildrenAgeGroupCSfilter(20, 65);

						Weighted_CrossSection.Integer withChildrenAged0_5CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
						withChildrenAged0_5CS.setFilter(childAged0_5Filter);
						Weighted_CrossSection.Integer withChildrenAged6_18CS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
						withChildrenAged6_18CS.setFilter(childAged6_18Filter);
						Weighted_CrossSection.Integer withoutChildrenCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
						withoutChildrenCS.setFilter(withoutChildrenFilter);

						TimeSeriesSimulationPlotter emplChildPlotter = new TimeSeriesSimulationPlotter("Female employment rate, by age of children \n Women aged 20 - 65", "");
						emplChildPlotter.addSeries("with children aged 0 - 5 yo", new Weighted_MeanArrayFunction(withChildrenAged0_5CS), null, colorArrayList.get(0), false);
						emplChildPlotter.addSeries("with children aged 6 - 18 yo", new Weighted_MeanArrayFunction(withChildrenAged6_18CS), null, colorArrayList.get(1), false);
						emplChildPlotter.addSeries("without children under 18 yo", new Weighted_MeanArrayFunction(withoutChildrenCS), null, colorArrayList.get(2), false);
						emplChildPlotter.addSeries("Validation with children aged 0 - 5 yo", validator, Validator.DoublesVariables.employmentFemaleChild_0_5, colorArrayList.get(0), true);
						emplChildPlotter.addSeries("Validation with children aged 6 - 18 yo", validator, Validator.DoublesVariables.employmentFemaleChild_6_18, colorArrayList.get(1), true);
						emplChildPlotter.addSeries("Validation without children under 18 yo", validator, Validator.DoublesVariables.employmentFemaleNoChild, colorArrayList.get(2), true);

						updateChartSet.add(emplChildPlotter);			//Add to set to be updated in buildSchedule method
						emplAgeMaternityPlots.add(emplChildPlotter);
//					}
//				}
				tabSet.add(createScrollPaneFromPlots(emplAgeMaternityPlots, "Employment (female): age/maternity", 2));


		    	/*
		    	Set<JInternalFrame> emplMaternityPlots = new LinkedHashSet<>();
		    	for (FemalesWithChildrenByChildAgeCSfilter childAgeFilter : childrenByAgeFilterSet) {
		    		//Need employment rate for females with children in a given age group, and general employment rate for females
					Weighted_CrossSection.Integer withChildrenCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
		    		withChildrenCS.setFilter(childAgeFilter);

		    		TimeSeriesSimulationPlotter emplChildByAgePlotter = new TimeSeriesSimulationPlotter("Female employment rate by age of children", "");
		    		emplChildByAgePlotter.addSeries("", new Weighted_MeanArrayFunction(withChildrenCS));


		    	}
		    	 */

			}
		    
		    //Employment by region
		    if(employmentByRegion) {
			    Set<JInternalFrame> emplGenderRegionPlots = new LinkedHashSet<JInternalFrame>();
			    TimeSeriesSimulationPlotter emplMaleRegionPlotter = new TimeSeriesSimulationPlotter("Male employment rate by region\n Age 18 - 64", "");
			    TimeSeriesSimulationPlotter emplFemaleRegionPlotter = new TimeSeriesSimulationPlotter("Female employment rate by region\n Age 18 - 64", "");
			    int colorCounter = 0;
			    for(Region region: Parameters.getCountryRegions()) {
//					MaleRegionCSfilter maleRegionFilter = new MaleRegionCSfilter(region);
					MaleRegionAgeCSfilter maleRegionFilter = new MaleRegionAgeCSfilter(region, 18, 64);
					Weighted_CrossSection.Integer maleRegionCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
					maleRegionCS.setFilter(maleRegionFilter);
					emplMaleRegionPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(maleRegionCS), null, colorArrayList.get(colorCounter), false);
					emplMaleRegionPlotter.addSeries("Validation " + region.getName(), validator, Validator.DoublesVariables.valueOf("employed_male_"+region), colorArrayList.get(colorCounter), true);
					
//					FemaleRegionCSfilter femaleRegionFilter = new FemaleRegionCSfilter(region);
					FemaleRegionAgeCSfilter femaleRegionFilter = new FemaleRegionAgeCSfilter(region, 18, 64);
					Weighted_CrossSection.Integer femaleRegionCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getEmployed", true);
					femaleRegionCS.setFilter(femaleRegionFilter);
					emplFemaleRegionPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(femaleRegionCS), null, colorArrayList.get(colorCounter), false);
					emplFemaleRegionPlotter.addSeries("Validation " + region.getName(), validator, Validator.DoublesVariables.valueOf("employed_female_"+region), colorArrayList.get(colorCounter), true);
					colorCounter++;
			    }		    		    
				updateChartSet.add(emplMaleRegionPlotter);			//Add to set to be updated in buildSchedule method		    
				updateChartSet.add(emplFemaleRegionPlotter);			//Add to set to be updated in buildSchedule method
				emplGenderRegionPlots.add(emplFemaleRegionPlotter);
				emplGenderRegionPlots.add(emplMaleRegionPlotter);
				tabSet.add(createScrollPaneFromPlots(emplGenderRegionPlots, "Employment: gender/region", 2));
		    }
		    
		    //LABOUR SUPPLY CHART
		    if(labourSupply) {
				TimeSeriesSimulationPlotter supplyPlotter = new TimeSeriesSimulationPlotter("Labour supply by education", "Yearly hours worked");		//'yo' means "years old"
				int colorCounter = 0;
				for(Education edu: Education.values()) {
					FlexibleInLabourSupplyByEducationFilter eduFilter = new FlexibleInLabourSupplyByEducationFilter(edu);
					Weighted_CrossSection.Double supplyCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getLabourSupplyHoursYearly", true);
					supplyCS.setFilter(eduFilter);
					supplyPlotter.addSeries(edu.toString(), new Weighted_MeanArrayFunction(supplyCS), null, colorArrayList.get(colorCounter), false);
					supplyPlotter.addSeries("Validation " + edu.toString(), validator, Validator.DoublesVariables.valueOf("labour_supply_"+edu), colorArrayList.get(colorCounter), true);
					colorCounter++;
				}
				supplyPlotter.setName("Labour supply");
			    updateChartSet.add(supplyPlotter);			//Add to set to be updated in buildSchedule method
				tabSet.add(supplyPlotter);
		    }

			//INCOME CHARTS - GROSS WAGES BY REGION AND EDUCATION LEVEL
		    if(grossEarningsByRegionAndEducation) {
		    	IndividualBarSimulationPlotter earningsPlotter;
		    	if (model.getCountry().equals(Country.UK)) {
					earningsPlotter = new IndividualBarSimulationPlotter("Yearly Gross Earnings by Education and Region (excludes non-workers)", "£");
				}
		    	else {
					earningsPlotter = new IndividualBarSimulationPlotter("Yearly Gross Earnings by Education and Region (excludes non-workers)", "Euro");
				}

				for(Region region: Parameters.getCountryRegions()) {
		    		for(Education edu: Education.values()) {
						RegionEducationWorkingCSfilter regionEduWorkingFilter = new RegionEducationWorkingCSfilter(region, edu);
						Weighted_CrossSection.Double wagesCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getGrossEarningsYearly", true);
						wagesCS.setFilter(regionEduWorkingFilter);
						earningsPlotter.addSources("(" + region.getName() + ", " + edu.toString() + ")", new Weighted_MeanArrayFunction(wagesCS), colorOfEducation(edu));
					}
				}
				earningsPlotter.setName("Gross Earnings");
			    updateChartSet.add(earningsPlotter);			//Add to set to be updated in buildSchedule method
				tabSet.add(earningsPlotter);
		    }

			//INCOME CHARTS B: GROSS EARNINGS BY EDUCATION
			if (grossEarningsByRegionAndEducation) {
				TimeSeriesSimulationPlotter grossEarningsByGenderAndEducationPlotter;
				int colorCounter = 0;
				if (model.getCountry().equals(Country.UK)) {
					grossEarningsByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("Yearly Gross Earnings by Gender And Education", "£");
				}
				else {
					grossEarningsByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("Yearly Gross Earnings by Gender And Education", "Euro");
				}
				for(Education edu: Education.values()) {
					for (Gender gender : Gender.values()) {
						GenderEducationWorkingCSfilter genderEducationWorkingFilter = new GenderEducationWorkingCSfilter(gender, edu);
						Weighted_CrossSection.Double wagesCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getGrossEarningsYearly", true); // Note: these are nominal values for each simulated year
						wagesCS.setFilter(genderEducationWorkingFilter);
						grossEarningsByGenderAndEducationPlotter.addSeries("(" + gender.toString() + ", " + edu.toString() + ")", new Weighted_MeanArrayFunction(wagesCS), null, colorArrayList.get(colorCounter), false);
						grossEarningsByGenderAndEducationPlotter.addSeries("Validation (" + gender + ", " + edu + ")", validator, Validator.DoublesVariables.valueOf("grossEarnings_"+ gender +"_"+ edu), colorArrayList.get(colorCounter), true);
						colorCounter++;
					}
				}
				grossEarningsByGenderAndEducationPlotter.setName("Gross Earnings by Gender / Education");
				updateChartSet.add(grossEarningsByGenderAndEducationPlotter);
				tabSet.add(grossEarningsByGenderAndEducationPlotter);
			}

			if (grossEarningsByRegionAndEducation) {
				TimeSeriesSimulationPlotter hourlyWagesByGenderAndEducationPlotter;
				int colorCounter = 0;
				if (model.getCountry().equals(Country.UK)) {
					hourlyWagesByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("Hourly Wages by Gender And Education", "£");
				}
				else {
					hourlyWagesByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("Hourly Wages by Gender And Education", "Euro");
				}
				for(Education edu: Education.values()) {
					for (Gender gender : Gender.values()) {
						GenderEducationWorkingCSfilter genderEducationWorkingFilter = new GenderEducationWorkingCSfilter(gender, edu);
						Weighted_CrossSection.Double wagesCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getHourlyWageRate1", true); // Note: these are nominal values for each simulated year
						wagesCS.setFilter(genderEducationWorkingFilter);
						hourlyWagesByGenderAndEducationPlotter.addSeries("(" + gender.toString() + ", " + edu.toString() + ")", new Weighted_MeanArrayFunction(wagesCS), null, colorArrayList.get(colorCounter), false);
						hourlyWagesByGenderAndEducationPlotter.addSeries("Validation (" + gender + ", " + edu + ")", validator, Validator.DoublesVariables.valueOf("hourlyWage_"+ gender +"_"+ edu), colorArrayList.get(colorCounter), true);
						colorCounter++;
					}
				}
				hourlyWagesByGenderAndEducationPlotter.setName("Hourly Wages by Gender / Education");
				updateChartSet.add(hourlyWagesByGenderAndEducationPlotter);
				tabSet.add(hourlyWagesByGenderAndEducationPlotter);
			}

			if (grossEarningsByRegionAndEducation) {
				TimeSeriesSimulationPlotter hoursOfWorkByGenderPlotter;
				int colorCounter = 0;
				hoursOfWorkByGenderPlotter = new TimeSeriesSimulationPlotter("Hours of Work Weekly by Gender", "Hours");
				for (Gender gender : Gender.values()) {
					GenderWorkingCSfilter genderWorkingFilter = new GenderWorkingCSfilter(gender);
					Weighted_CrossSection.Double hoursCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getDoubleLabourSupplyHoursWeekly", true); // Note: these are nominal values for each simulated year
					hoursCS.setFilter(genderWorkingFilter);
					hoursOfWorkByGenderPlotter.addSeries(gender.toString(), new Weighted_MeanArrayFunction(hoursCS), null, colorArrayList.get(colorCounter), false);
					hoursOfWorkByGenderPlotter.addSeries("Validation " + gender, validator, Validator.DoublesVariables.valueOf("lhw_"+ gender), colorArrayList.get(colorCounter), true);
					colorCounter++;
					}
				hoursOfWorkByGenderPlotter.setName("Hours of Work by Gender");
				updateChartSet.add(hoursOfWorkByGenderPlotter);
				tabSet.add(hoursOfWorkByGenderPlotter);
			}
		    
			//Statistics dependent charts
		    if(collector.isCalculateGiniCoefficients()) {	//As these charts need statistics to be calculated within the simulation, turn off these charts if the statistics are not calculated
		    	
				//INCOME CHARTS - GINI
			    Set<JInternalFrame> giniIncomeRegionPlots = new LinkedHashSet<JInternalFrame>();			    
			    //Gini coefficient of market (gross) individual income
			    TimeSeriesSimulationPlotter personalGrossEarningsGiniPlotter = new TimeSeriesSimulationPlotter("Gini: Gross individual earnings", "Gini coefficient");
			    //Add Series at national and regional level
			    for(Region region: Parameters.getCountryRegions()) {
			    	personalGrossEarningsGiniPlotter.addSeries(region.getName(), (IDoubleSource)collector.fGiniPersonalGrossEarningsRegionalMap.get(region));
			    }
			    personalGrossEarningsGiniPlotter.addSeries("national", (IDoubleSource)collector.fGiniPersonalGrossEarningsNational);
			    updateChartSet.add(personalGrossEarningsGiniPlotter);			//Add to set to be updated in buildSchedule method
			    giniIncomeRegionPlots.add(personalGrossEarningsGiniPlotter);
			    
			    //Gini coefficient of equivalised household disposable income
			    TimeSeriesSimulationPlotter equivalisedHouseholdDisposableIncomeGiniPlotter = new TimeSeriesSimulationPlotter("Gini: Equivalised household disposable income", "Gini coefficient");
			    //Add Series at national and regional level
			    for(Region region: Parameters.getCountryRegions()) {
			    	equivalisedHouseholdDisposableIncomeGiniPlotter.addSeries(region.getName(), (IDoubleSource)collector.fGiniEquivalisedHouseholdDisposableIncomeRegionalMap.get(region));
			    }
			    equivalisedHouseholdDisposableIncomeGiniPlotter.addSeries("national", (IDoubleSource)collector.fGiniEquivalisedHouseholdDisposableIncomeNational);
			    updateChartSet.add(equivalisedHouseholdDisposableIncomeGiniPlotter);			//Add to set to be updated in buildSchedule method		    
			    giniIncomeRegionPlots.add(equivalisedHouseholdDisposableIncomeGiniPlotter);
			    
			    tabSet.add(createScrollPaneFromPlots(giniIncomeRegionPlots, "Gini income", 2));
		    }			
			
		    
		    //POVERTY CHARTS
		    if(poverty) { 
			    Set<JInternalFrame> povertyPlots = new LinkedHashSet<JInternalFrame>();
			    TimeSeriesSimulationPlotter housePovertyPlotter = new TimeSeriesSimulationPlotter("Share of Households at risk of poverty", "");
			    TimeSeriesSimulationPlotter childPovertyPlotter = new TimeSeriesSimulationPlotter("Share of Children at risk of poverty", "");
			    for(Region region: Parameters.getCountryRegions()) {
			    	//Households
					ValidHouseholdIncomeRegionalCSfilter validHouseholdIncomeRegionalFilter = new ValidHouseholdIncomeRegionalCSfilter(region);				
					Weighted_CrossSection.Integer validHousesAtRiskOfPovertyRegionCS = new Weighted_CrossSection.Integer(model.getBenefitUnits(), BenefitUnit.class, "getAtRiskOfPoverty", true);
					validHousesAtRiskOfPovertyRegionCS.setFilter(validHouseholdIncomeRegionalFilter);
					housePovertyPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(validHousesAtRiskOfPovertyRegionCS));
					
					//Children
					ChildValidIncomeRegionalCSfilter childValidIncomeRegionalFilter = new ChildValidIncomeRegionalCSfilter(region);				
					Weighted_CrossSection.Integer childAtRiskOfPovertyRegionCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getAtRiskOfPoverty", true);
					childAtRiskOfPovertyRegionCS.setFilter(childValidIncomeRegionalFilter);
					childPovertyPlotter.addSeries(region.getName(), new Weighted_MeanArrayFunction(childAtRiskOfPovertyRegionCS));		    		
			    }
			    //Households
			    ValidHouseholdIncomeCSfilter validHouseholdIncomeFilter = new ValidHouseholdIncomeCSfilter();
			    Weighted_CrossSection.Integer validHousesAtRiskOfPovertyCS = new Weighted_CrossSection.Integer(model.getBenefitUnits(), BenefitUnit.class, "getAtRiskOfPoverty", true);
			    validHousesAtRiskOfPovertyCS.setFilter(validHouseholdIncomeFilter);
			    housePovertyPlotter.addSeries("national", new Weighted_MeanArrayFunction(validHousesAtRiskOfPovertyCS));		    
				updateChartSet.add(housePovertyPlotter);			//Add to set to be updated in buildSchedule method
				povertyPlots.add(housePovertyPlotter);
				
				//Children
				ChildValidIncomeCSfilter childValidIncomeFilter = new ChildValidIncomeCSfilter();				
				Weighted_CrossSection.Integer childAtRiskOfPovertyCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "getAtRiskOfPoverty", true);
				childAtRiskOfPovertyCS.setFilter(childValidIncomeFilter);
				childPovertyPlotter.addSeries("national", new Weighted_MeanArrayFunction(childAtRiskOfPovertyCS));		    				    
				updateChartSet.add(childPovertyPlotter);			//Add to set to be updated in buildSchedule method
			    povertyPlots.add(childPovertyPlotter);
			    
			    tabSet.add(createScrollPaneFromPlots(povertyPlots, "Poverty", 2));
		    }
		    
		    // HISTOGRAMS OF INCOME
		    if(incomeHistograms) {
			    Set<JInternalFrame> histogramIncomePlots = new LinkedHashSet<JInternalFrame>();
			    
			    ValidPersonEarningsCSfilter validEarningsFilter = new ValidPersonEarningsCSfilter();
			    Weighted_HistogramSimulationPlotter grossEarningsHistPlotter = new Weighted_HistogramSimulationPlotter("Individual Gross Earnings (yearly)", "Euro", histogramType.getHistogramType(), numberOfHistogramBins);
			    Weighted_CrossSection.Double grossEarningsCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getGrossEarningsYearly", true);
	//		    Weighted_CrossSection.Double grossEarningsCS = new Weighted_CrossSection.Double(model.getPersons(), Person.DoublesVariables.GrossEarningsYearly);
			    grossEarningsCS.setFilter(validEarningsFilter);
			    
			    grossEarningsHistPlotter.addCollectionSource("Gross Earnings", grossEarningsCS);
			    updateChartSet.add(grossEarningsHistPlotter);			//Add to set to be updated in buildSchedule method
			    histogramIncomePlots.add(grossEarningsHistPlotter);
			    
			    ValidHouseholdIncomeCSfilter validHouseholdIncomeFilter = new ValidHouseholdIncomeCSfilter();
			    Weighted_HistogramSimulationPlotter dispIncomeHistPlotter = new Weighted_HistogramSimulationPlotter("Equivalised Disposable Income of Benefit Unit (yearly)", "Euro", histogramType.getHistogramType(), numberOfHistogramBins);
			    Weighted_CrossSection.Double equivalisedDisposableIncomeCS = new Weighted_CrossSection.Double(model.getBenefitUnits(), BenefitUnit.class, "getEquivalisedDisposableIncomeYearly", true);
	//		    Weighted_CrossSection.Double equivalisedDisposableIncomeCS = new Weighted_CrossSection.Double(model.getHouseholds(), BenefitUnit.DoublesVariables.EquivalisedDisposableIncomeYearly);
			    equivalisedDisposableIncomeCS.setFilter(validHouseholdIncomeFilter);
			    dispIncomeHistPlotter.addCollectionSource("Equivalised BenefitUnit Disposable Income", equivalisedDisposableIncomeCS);
			    updateChartSet.add(dispIncomeHistPlotter);			//Add to set to be updated in buildSchedule method
			    histogramIncomePlots.add(dispIncomeHistPlotter);
			    
			    tabSet.add(createScrollPaneFromPlots(histogramIncomePlots, "Income", 2));
		    }


			if (incomeHistograms) {
				TimeSeriesSimulationPlotter EDIByGenderAndEducationPlotter;
				int colorCounter = 0;
				if (model.getCountry().equals(Country.UK)) {
					EDIByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("EDI by Gender And Education", "£");
				}
				else {
					EDIByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("EDI by Gender And Education", "Euro");
				}
				for(Education edu: Education.values()) {
					for (Gender gender : Gender.values()) {
						GenderEducationWorkingCSfilter genderEducationWorkingFilter = new GenderEducationWorkingCSfilter(gender, edu);
						Weighted_CrossSection.Double EDIWorkingCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getEquivalisedDisposableIncomeYearly", true); // Note: these are nominal values for each simulated year
						EDIWorkingCS.setFilter(genderEducationWorkingFilter);
						GenderEducationCSfilter genderEducationCSfilter = new GenderEducationCSfilter(gender, edu);
						Weighted_CrossSection.Double EDIAllCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getEquivalisedDisposableIncomeYearly", true); // Note: these are nominal values for each simulated year
						EDIAllCS.setFilter(genderEducationCSfilter);
						EDIByGenderAndEducationPlotter.addSeries("Workers (" + gender.toString() + ", " + edu.toString() + ")", new Weighted_MeanArrayFunction(EDIWorkingCS), null, colorArrayList.get(colorCounter), false);
						colorCounter++;
						EDIByGenderAndEducationPlotter.addSeries("All (" + gender.toString() + ", " + edu.toString() + ")", new Weighted_MeanArrayFunction(EDIAllCS), null, colorArrayList.get(colorCounter), false);
	//					EDIByGenderAndEducationPlotter.addSeries("Validation (" + gender + ", " + edu + ")", validator, Validator.DoublesVariables.valueOf("grossEarnings_"+ gender +"_"+ edu), colorArrayList.get(colorCounter), true);
						colorCounter++;
					}
				}
				EDIByGenderAndEducationPlotter.setName("EDI by Gender / Education");
				updateChartSet.add(EDIByGenderAndEducationPlotter);
				tabSet.add(EDIByGenderAndEducationPlotter);
			}

			if (incomeHistograms) {
				TimeSeriesSimulationPlotter DispIncByGenderAndEducationPlotter;
				int colorCounter = 0;
				if (model.getCountry().equals(Country.UK)) {
					DispIncByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("Disp income by Gender And Education", "£");
				}
				else {
					DispIncByGenderAndEducationPlotter = new TimeSeriesSimulationPlotter("Disp income by Gender And Education", "Euro");
				}
				for(Education edu: Education.values()) {
					for (Gender gender : Gender.values()) {
						GenderEducationWorkingCSfilter genderEducationWorkingFilter = new GenderEducationWorkingCSfilter(gender, edu);
						Weighted_CrossSection.Double DispIncWorkingCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getDisposableIncomeMonthly", true); // Note: these are nominal values for each simulated year
						DispIncWorkingCS.setFilter(genderEducationWorkingFilter);
						GenderEducationCSfilter genderEducationCSfilter = new GenderEducationCSfilter(gender, edu);
						Weighted_CrossSection.Double DispIncAllCS = new Weighted_CrossSection.Double(model.getPersons(), Person.class, "getDisposableIncomeMonthly", true); // Note: these are nominal values for each simulated year
						DispIncAllCS.setFilter(genderEducationCSfilter);
						DispIncByGenderAndEducationPlotter.addSeries("Workers (" + gender.toString() + ", " + edu.toString() + ")", new Weighted_MeanArrayFunction(DispIncWorkingCS), null, colorArrayList.get(colorCounter), false);
						colorCounter++;
						DispIncByGenderAndEducationPlotter.addSeries("All (" + gender.toString() + ", " + edu.toString() + ")", new Weighted_MeanArrayFunction(DispIncAllCS), null, colorArrayList.get(colorCounter), false);
						//					EDIByGenderAndEducationPlotter.addSeries("Validation (" + gender + ", " + edu + ")", validator, Validator.DoublesVariables.valueOf("grossEarnings_"+ gender +"_"+ edu), colorArrayList.get(colorCounter), true);
						colorCounter++;
					}
				}
				DispIncByGenderAndEducationPlotter.setName("Disp income by Gender / Education");
				updateChartSet.add(DispIncByGenderAndEducationPlotter);
				tabSet.add(DispIncByGenderAndEducationPlotter);
			}

		    if (securityIndex) {

		    	/*
		    	Set<JInternalFrame> sIndexPyramidPlots = new LinkedHashSet<>();
		    	Weighted_PyramidPlotter sIndexAgeGenderPlotter = new Weighted_PyramidPlotter();
		    	Weighted_CrossSection.Double[] populationData = new Weighted_CrossSection.Double[2];
				Weighted_CrossSection.Double maleAgesCS = new Weighted_CrossSection.Double(model.getPersons(), Person.DoublesVariables.sIndex);
				maleAgesCS.setFilter(new GenderCSfilter(Gender.Male));
				populationData[0] = maleAgesCS;
				Weighted_CrossSection.Double femaleAgesCS = new Weighted_CrossSection.Double(model.getPersons(), Person.DoublesVariables.sIndex);
				femaleAgesCS.setFilter(new GenderCSfilter(Gender.Female));
				populationData[1] = femaleAgesCS;

				sIndexAgeGenderPlotter.setScalingFactor(model.getScalingFactor());
				sIndexAgeGenderPlotter.addCollectionSource(populationData);

				updateChartSet.add(sIndexAgeGenderPlotter);
				sIndexPyramidPlots.add(sIndexAgeGenderPlotter);

				tabSet.add(createScrollPaneFromPlots(sIndexPyramidPlots, "SIndex Pyramid", 1));
		    	 */

				//Filters
				ValidPersonSIndexCSfilter validSIndexFilter = new ValidPersonSIndexCSfilter();
				ValidPersonAgeGenderSIndexCSfilter validSIndexMalesFilter = new ValidPersonAgeGenderSIndexCSfilter(0,100,Gender.Male);
				ValidPersonAgeGenderSIndexCSfilter validSIndexFemalesFilter = new ValidPersonAgeGenderSIndexCSfilter(0,100,Gender.Female);
				ValidPersonAgeGenderSIndexCSfilter validSIndexMales_50_100_Filter = new ValidPersonAgeGenderSIndexCSfilter(50,100,Gender.Male);
				ValidPersonAgeGenderSIndexCSfilter validSIndexFemales_50_100_Filter = new ValidPersonAgeGenderSIndexCSfilter(50,100,Gender.Female);
				ValidPersonAgeSIndexCSfilter validSIndexAge_0_29_Filter = new ValidPersonAgeSIndexCSfilter(0,29);
				ValidPersonAgeSIndexCSfilter validSIndexAge_30_49_Filter = new ValidPersonAgeSIndexCSfilter(30,49);
				ValidPersonAgeSIndexCSfilter validSIndexAge_50_100_Filter = new ValidPersonAgeSIndexCSfilter(50,100);


				//Histogram
				Set<JInternalFrame> histogramSIndex = new LinkedHashSet<>();
				Weighted_HistogramSimulationPlotter sIndexHistPlotter = new Weighted_HistogramSimulationPlotter("Security Index (" + model.getsIndexTimeWindow() + " years lag)", "Value", histogramType.getHistogramType(), numberOfHistogramBins, 0., 5., true);
				Weighted_CrossSection.Double sIndexCS = new Weighted_CrossSection.Double(model.getPersons(), Person.DoublesVariables.sIndexNormalised);
				sIndexCS.setFilter(validSIndexFilter);
				sIndexHistPlotter.addCollectionSource("Security Index", sIndexCS);
				updateChartSet.add(sIndexHistPlotter);
				histogramSIndex.add(sIndexHistPlotter);


				//Time series plot
				TimeSeriesSimulationPlotter sIndexTSPlot = new TimeSeriesSimulationPlotter("Security Index (" + model.getsIndexTimeWindow() + " years lag) \n By Gender", "");
				sIndexTSPlot.addSeries("Everyone", new Weighted_MeanArrayFunction(sIndexCS));

				Weighted_CrossSection.Double sIndexCS_males = new Weighted_CrossSection.Double(model.getPersons(), Person.DoublesVariables.sIndexNormalised);
				sIndexCS_males.setFilter(validSIndexMalesFilter);

				Weighted_CrossSection.Double sIndexCS_females = new Weighted_CrossSection.Double(model.getPersons(), Person.DoublesVariables.sIndexNormalised);
				sIndexCS_females.setFilter(validSIndexFemalesFilter);

				Weighted_CrossSection.Double sIndexCS_males_50_100 = new Weighted_CrossSection.Double(model.getPersons(), Person.DoublesVariables.sIndexNormalised);
				sIndexCS_males_50_100.setFilter(validSIndexMales_50_100_Filter);

				Weighted_CrossSection.Double sIndexCS_females_50_100 = new Weighted_CrossSection.Double(model.getPersons(), Person.DoublesVariables.sIndexNormalised);
				sIndexCS_females_50_100.setFilter(validSIndexFemales_50_100_Filter);

				sIndexTSPlot.addSeries("Male", new Weighted_MeanArrayFunction(sIndexCS_males)); //But note this is current age, while s Index refer to t-specified_time_window_for_s_index
				sIndexTSPlot.addSeries("Female", new Weighted_MeanArrayFunction(sIndexCS_females));
	//			sIndexTSPlot.addSeries("Female above 50", new Weighted_MeanArrayFunction(sIndexCS_females_50_100));
	//			sIndexTSPlot.addSeries("Male above 50", new Weighted_MeanArrayFunction(sIndexCS_males_50_100));


				//By region:
				TimeSeriesSimulationPlotter sIndexTSPlotRegion = new TimeSeriesSimulationPlotter("Security Index (" + model.getsIndexTimeWindow() + " years lag) \n By Region", "");
				for(Region region: Parameters.getCountryRegions()) {
					ValidPersonRegionSIndexCSfilter validSIndexRegion = new ValidPersonRegionSIndexCSfilter(region);
					Weighted_CrossSection.Double sIndexCSRegion = new Weighted_CrossSection.Double(model.getPersons(), Person.DoublesVariables.sIndexNormalised);
					sIndexCSRegion.setFilter(validSIndexRegion);
					sIndexTSPlotRegion.addSeries(region.getName(), new Weighted_MeanArrayFunction(sIndexCSRegion));
				}

				//By age
				TimeSeriesSimulationPlotter sIndexTSPlotAge = new TimeSeriesSimulationPlotter("Security Index (" + model.getsIndexTimeWindow() + " years lag) \n By Age", "");
				Weighted_CrossSection.Double sIndexCS_Age_0_29 = new Weighted_CrossSection.Double(model.getPersons(), Person.DoublesVariables.sIndexNormalised);
				sIndexCS_Age_0_29.setFilter(validSIndexAge_0_29_Filter);
				Weighted_CrossSection.Double sIndexCS_Age_30_49 = new Weighted_CrossSection.Double(model.getPersons(), Person.DoublesVariables.sIndexNormalised);
				sIndexCS_Age_30_49.setFilter(validSIndexAge_30_49_Filter);
				Weighted_CrossSection.Double sIndexCS_Age_50_100 = new Weighted_CrossSection.Double(model.getPersons(), Person.DoublesVariables.sIndexNormalised);
				sIndexCS_Age_50_100.setFilter(validSIndexAge_50_100_Filter);
				sIndexTSPlotAge.addSeries("Under 30", new Weighted_MeanArrayFunction(sIndexCS_Age_0_29));
				sIndexTSPlotAge.addSeries("30 - 49", new Weighted_MeanArrayFunction(sIndexCS_Age_30_49));
				sIndexTSPlotAge.addSeries("Above 50", new Weighted_MeanArrayFunction(sIndexCS_Age_50_100));


				updateChartSet.add(sIndexTSPlot);
				updateChartSet.add(sIndexTSPlotRegion);
				updateChartSet.add(sIndexTSPlotAge);
				histogramSIndex.add(sIndexTSPlot);
				histogramSIndex.add(sIndexTSPlotRegion);
				histogramSIndex.add(sIndexTSPlotAge);

				tabSet.add(createScrollPaneFromPlots(histogramSIndex, "Security Index", 2));

			}
		    
		    // WORKING HOURS PYRAMID GRAPH
		    if (workingHoursPyramid) {
		    	Set<JInternalFrame> workingHoursPyramidPlots = new LinkedHashSet<JInternalFrame>();
			    Weighted_PyramidPlotter populationAgeGenderPlotter = new Weighted_PyramidPlotter("Working hours over time", "Total hours worked", Weighted_PyramidPlotter.DEFAULT_YAXIS, Weighted_PyramidPlotter.DEFAULT_LEFT_CAT, Weighted_PyramidPlotter.DEFAULT_RIGHT_CAT);
			    // Please note that the Pyramid plotter requires a Weighted_CrossSection[2]
			    // The exact type (int, double etc) must match the variable in Person  
			    Weighted_CrossSection.Integer[] populationData = new Weighted_CrossSection.Integer[2];
			    Weighted_CrossSection.Integer maleAgesCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "liwwh", false);
			    maleAgesCS.setFilter(new GenderCSfilter(Gender.Male));
			    populationData[0] = maleAgesCS; 
			    Weighted_CrossSection.Integer femaleAgesCS = new Weighted_CrossSection.Integer(model.getPersons(), Person.class, "liwwh", false);
			    femaleAgesCS.setFilter(new GenderCSfilter(Gender.Female)); 
			    populationData[1] = femaleAgesCS; 
			    
			    populationAgeGenderPlotter.setScalingFactor(model.getScalingFactor());
				populationAgeGenderPlotter.addCollectionSource(populationData); 
				
			    updateChartSet.add(populationAgeGenderPlotter);			//Add to set to be updated in buildSchedule method
			    workingHoursPyramidPlots.add(populationAgeGenderPlotter);
		    	
			    tabSet.add(createScrollPaneFromPlots(workingHoursPyramidPlots, "Working Hours Pyramid", 1));
		    }
		    
		    
		    //-------------------------------------------------------------------------------------------------------
		    //
	    	//	BUILD A TABBED PANE HOLDING ALL THE CHARTS THAT ONLY UPDATE AT EACH TIME-STEP (not convergence plots)
		    //
	    	//-------------------------------------------------------------------------------------------------------
		    
	    	JInternalFrame chartsFrame = new JInternalFrame("Charts");
			JTabbedPane tabbedPane = new JTabbedPane();
			chartsFrame.add(tabbedPane);
			
			for(JComponent plot: tabSet) {
				tabbedPane.addTab(plot.getName(), plot);
			}
			tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
	    	chartsFrame.setResizable(true);
	    	chartsFrame.setMaximizable(true);
			GuiUtils.addWindow(chartsFrame, 300, 0, 1560, 660);
		    
			
			
			//---------------------------------------------------------------------------
			//	LABOUR MARKET CONVERGENCE UPDATED BETWEEN TIME-STEPS
			//---------------------------------------------------------------------------
			
			//BOWKER NORM OF LABOUR DEMAND*SUPPLY ELASTICITIES		    
		    //The condition for convergence (||AB||<1) for different skill levels (3 lines), at the regional level.  These charts are updated at each iteration of the convergence process.
		    //This uses cross section objects and mean array functions to calculate averages on population aggregates and presents them in a scatterplot.
//		    convergenceElasticitiesPlotter = new ScatterplotSimulationPlotterRefreshable("Labour Market Convergence Criterion", "iteration", "||labour demand * supply elasticities||");
//		    convergenceElasticitiesPlotter.setMaxSamples(convergenceElasticitiesPlotMaxSamples);		//Show only the previous 'convergencePlotMaxSamples' number of iterations.		    	
//	    	for(Region region: Parameters.getCountryRegions()) {
//	    		convergenceElasticitiesPlotter.addSeries(region.getName(), (ILongSource)new MultiTraceFunction.Long(model.getLabourMarket().getBowkerNormRegional(), region), new MultiTraceFunction.Double(model.getLabourMarket().getBowkerNormRegional(), region));
//	    	} 		
//	    	if(floatingConvergencePlots) {
//	    		GuiUtils.addWindow(convergenceElasticitiesPlotter, 0, 160, 450, 400);
//	    	}
//	    	else {
//	    		convergencePlots.add(convergenceElasticitiesPlotter);
//	    	}
	    	
			
//			log.debug("Observer objects created");
		}
							
	}	



	@Override
	public void buildSchedule() {
		
		if(showCharts) {
			
			EventGroup chartingEvents = new EventGroup();
			for(JInternalFrame plot: updateChartSet) {
				chartingEvents.addEvent(plot, CommonEventType.Update);
			}
//			getEngine().getEventQueue().scheduleOnce(chartingEvents, model.getStartYear(), Order.BEFORE_ALL.getOrdering()+1);
			getEngine().getEventQueue().scheduleRepeat(chartingEvents, model.getStartYear(), ordering, displayFrequency);
			getEngine().getEventQueue().scheduleRepeat(new SingleTargetEvent(this, Processes.ResetConvergenceChart), model.getStartYear(), ordering, displayFrequency);

		}
							
	}
	
	//--------------------------------------------------------------------------
	//	Other Methods 
	//--------------------------------------------------------------------------


	//For use with bar charts to specify what colour to use based on education level
	private Color colorOfEducation(Education edu) {
		if(edu.equals(Education.Low)) {
			return Color.RED;
		}
		else if(edu.equals(Education.Medium)) {
			return Color.BLUE;
		}
		else if(edu.equals(Education.High)) {
			return Color.WHITE;
		}
		else throw new IllegalArgumentException("ERROR - no color is specified for " + edu + " in SimPathsObserver class!");
	}

	
	/**
	 * Method to re-arrange JInternalFrames such as JFreeChart plots into 
	 * a single JInternalFrame (e.g. to use in a TabbedPane of plots).
	 * 
	 * @param internalFrames - a set of JInternalFrames such as JFreeChart plots 
	 * @param name - the name of the JScrollPane returned
	 * @param columns - the number of columns with which the JInternalFrames will be laid out 
	 * @return A JScrollPane laying of a set of JInternalFrames 
	 */
	private JScrollPane createScrollPaneFromPlots(Set<JInternalFrame> internalFrames, String name, int columns) {		
		
		String layoutConstraints = "wrap " + columns;
		MigLayout layout = new MigLayout(layoutConstraints, "fill, grow", "fill, grow");
		JPanel panel = new JPanel(layout);

		for(JInternalFrame internalFrame: internalFrames) {
			internalFrame.setVisible(true);
			internalFrame.setResizable(false);	//The components (charts) are not able to expand beyond their assigned row/column, so the only way to resize is to resize the whole pane. 
			panel.add(internalFrame);
		}		
		JScrollPane frame = new JScrollPane(panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		frame.setName(name);
		return frame;
	}
	

	//For bowker norm of labour supply/demand elasticities updated during convergence process
	public void updateConvergencePlotter() {		
		convergenceElasticitiesPlotter.update();
	}

/*	public void resetConvergencePlotter() {
		model.getLabourMarket().getBowkerNormRegional().reset();
		convergenceElasticitiesPlotter.refresh();
	}
*/
	//For potential earnings and aggregate labour supply/demand plots updated during convergence process
	public void updateLabourMarketPlots(Region region) {

		//Potential Earnings
		for(Education edu: Education.values()) {
			meanPotentialEarningsMultiMap.get(region, edu).updateSource();	//Only updates when both the underlying cross section AND the mean array function has setCheckingTime(false).
//			meanPotentialEarningsMultiMap.get(region, edu).applyFunction();		//Forces update of function, but still needs the underlying cross section object to have setCheckingTime(false).
		}
		for(ScatterplotSimulationPlotterRefreshable plot: potentialEarningsPlots.values()) {
			plot.update();
		}
		
		//Labour Market
		for(ScatterplotSimulationPlotterRefreshable plot: labourMarketPlots.values()) {
			plot.update();
		}
		
		
//		for(Education edu: Education.values()) {
//			System.out.println("Average Potential Earnings: count " + countIterations + ", " + region.getName() + ": " + edu + ", " + meanPotentialEarningsMultiMap.get(region, edu).getDoubleValue(null));
//		}
//		Person p = model.getPerson(202L);
//		System.out.println("Potential Earnings of person " + p.getKey().getId() + ": count " + countIterations + ", " + p.getRegion() + ": " + p.getEducation() + ", " + p.getPotentialHourlyEarnings());
		
		
		//Increment iterations count
		countIterations++;
	}
		
	public void resetLabourMarketPlots(Region region) {
		
		for(ScatterplotSimulationPlotterRefreshable plot: potentialEarningsPlots.values()) {
			plot.reset();
		}

		for(ScatterplotSimulationPlotterRefreshable plot: labourMarketPlots.values()) {
			plot.reset();
		}
		countIterations = 0;
		
		for(Education edu: Education.values()) {
			//Potential Earnings
			potentialEarningsPlots.get(edu).addSeries(region.getName(), (ILongSource)new MultiTraceFunction.Long(this, LongVariables.CountIterations), ((IDoubleSource)meanPotentialEarningsMultiMap.get(region, edu)));
			
			//Labour supply/demand stats
//			labourMarketPlots.get(edu).addSeries(region.getName() + ": Demand", (ILongSource)new MultiTraceFunction.Long(this, LongVariables.CountIterations), new MultiTraceFunction.Double(new AggregateLabourDemandStatistics(region), edu));
//			labourMarketPlots.get(edu).addSeries(region.getName() + ": Supply", (ILongSource)new MultiTraceFunction.Long(this, LongVariables.CountIterations), new MultiTraceFunction.Double(new AggregateLabourSupplyStatistics(region), edu));
		}

	}

	
	//--------------------------------------------------------------------------
	//	Event Listener implementation 
	//--------------------------------------------------------------------------
	
	
	public enum Processes {
		ResetConvergenceChart,
	}
	
	@Override
	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
		
		case ResetConvergenceChart:
//			resetConvergencePlotter();		    
			break;
			
		}
		
	}
	

	//--------------------------------------------------------------------------
	//	ILongSource implementation 
	//--------------------------------------------------------------------------
	
	
	public enum LongVariables {
		CountIterations,
	}

	@Override
	public long getLongValue(Enum<?> var) {
		switch ((LongVariables) var) {
		
		case CountIterations:
			return countIterations;
	
		default:
			throw new IllegalArgumentException("ERROR - " + var + " not found in SimPathsObserver#getLongValue()");
			
		}
	
	}

	
	//--------------------------------------------------------------------------
	//	Classes to access region and education dependent aggregate data
	//--------------------------------------------------------------------------
	
/*	public class AggregateLabourDemandStatistics implements IDoubleSource {
		
		private Region region;

		public AggregateLabourDemandStatistics(Region region) {
			this.region = region;
		}
		
		@Override
		public double getDoubleValue(Enum<?> education) {
			return model.getLabourMarket().getAggregateWeeklyLabourDemandByRegionAndEducation().get(region).get(education);			
		}
			
	}	
*/
	
	
	//--------------------------------------------------------------------------
	// Access methods
	//--------------------------------------------------------------------------
	
	public Double getDisplayFrequency() {
		return displayFrequency;
	}

	public void setDisplayFrequency(Double displayFrequency) {
		this.displayFrequency = displayFrequency;
	}
	
	public Boolean getShowCharts() {
		return showCharts;
	}

	public void setShowCharts(Boolean showCharts) {
		this.showCharts = showCharts;
	}

	public Boolean getShowAdditionalCharts() {
		return showAdditionalCharts;
	}

	public void setShowAdditionalCharts(Boolean showAdditionalCharts) {
		this.showAdditionalCharts = showAdditionalCharts;
	}

	public Boolean getShowValidationStatistics() {
		return showValidationStatistics;
	}

	public void setShowValidationStatistics(Boolean showValidationStatistics) {
		this.showValidationStatistics = showValidationStatistics;
	}

	public Integer getNumberOfHistogramBins() {
		return numberOfHistogramBins;
	}

	public void setNumberOfHistogramBins(Integer numberOfHistogramBins) {
		this.numberOfHistogramBins = numberOfHistogramBins;
	}


	public HistogramTypeEnum getHistogramType() {
		return histogramType;
	}


	public void setHistogramType(HistogramTypeEnum histogramType) {
		this.histogramType = histogramType;
	}


	public boolean isEducationByAge() {
		return educationByAge;
	}


	public void setEducationByAge(boolean educationByAge) {
		this.educationByAge = educationByAge;
	}


	public boolean isEducationByRegion() {
		return educationByRegion;
	}


	public void setEducationByRegion(boolean educationByRegion) {
		this.educationByRegion = educationByRegion;
	}


	public boolean isEducationOfAdults() {
		return educationOfAdults;
	}


	public void setEducationOfAdults(boolean educationOfAdults) {
		this.educationOfAdults = educationOfAdults;
	}


	public boolean isEmploymentByAge() {
		return employmentByAge;
	}


	public void setEmploymentByAge(boolean employmentByAge) {
		this.employmentByAge = employmentByAge;
	}


	public boolean isEmploymentByRegion() {
		return employmentByRegion;
	}


	public void setEmploymentByRegion(boolean employmentByRegion) {
		this.employmentByRegion = employmentByRegion;
	}


	public boolean isEmploymentOfAdults() {
		return employmentOfAdults;
	}


	public void setEmploymentOfAdults(boolean employmentOfAdults) {
		this.employmentOfAdults = employmentOfAdults;
	}


	public boolean isFemaleEmploymentByMaternity() {
		return femaleEmploymentByMaternity;
	}


	public void setFemaleEmploymentByMaternity(boolean femaleEmploymentByMaternity) {
		this.femaleEmploymentByMaternity = femaleEmploymentByMaternity;
	}


	public boolean isHouseholdComposition() {
		return householdComposition;
	}


	public void setHouseholdComposition(boolean householdComposition) {
		this.householdComposition = householdComposition;
	}


	public boolean isIncomeHistograms() {
		return incomeHistograms;
	}


	public void setIncomeHistograms(boolean incomeHistograms) {
		this.incomeHistograms = incomeHistograms;
	}


	public boolean isPopulationPyramid() {
		return populationPyramid;
	}


	public void setPopulationPyramid(boolean populationPyramid) {
		this.populationPyramid = populationPyramid;
	}


	public boolean isWorkingHoursPyramid() {
		return workingHoursPyramid;
	}


	public void setWorkingHoursPyramid(boolean workingHoursPyramid) {
		this.workingHoursPyramid = workingHoursPyramid;
	}


	public boolean isLabourSupply() {
		return labourSupply;
	}


	public void setLabourSupply(boolean labourSupply) {
		this.labourSupply = labourSupply;
	}


	public boolean isPopulation() {
		return population;
	}


	public void setPopulation(boolean population) {
		this.population = population;
	}


	public boolean isPoverty() {
		return poverty;
	}


	public void setPoverty(boolean poverty) {
		this.poverty = poverty;
	}


	public boolean isStudentsByAge() {
		return studentsByAge;
	}


	public void setStudentsByAge(boolean studentsByAge) {
		this.studentsByAge = studentsByAge;
	}


	public boolean isStudentsByRegion() {
		return studentsByRegion;
	}


	public void setStudentsByRegion(boolean studentsByRegion) {
		this.studentsByRegion = studentsByRegion;
	}

	public boolean isGrossEarningsByRegionAndEducation() {
		return grossEarningsByRegionAndEducation;
	}

	public void setGrossEarningsByRegionAndEducation(boolean grossEarningsByRegionAndEducation) {
		this.grossEarningsByRegionAndEducation = grossEarningsByRegionAndEducation;
	}


	public Integer getPotentialEarningsPlotMaxSamples() {
		return potentialEarningsPlotMaxSamples;
	}


	public void setPotentialEarningsPlotMaxSamples(Integer potentialEarningsPlotMaxSamples) {
		this.potentialEarningsPlotMaxSamples = potentialEarningsPlotMaxSamples;
	}


	public Integer getLabourMarketPlotMaxSamples() {
		return labourMarketPlotMaxSamples;
	}


	public void setLabourMarketPlotMaxSamples(Integer labourMarketPlotMaxSamples) {
		this.labourMarketPlotMaxSamples = labourMarketPlotMaxSamples;
	}

	public boolean isFloatingConvergencePlots() {
		return floatingConvergencePlots;
	}

	public void setFloatingConvergencePlots(boolean floatingConvergencePlots) {
		this.floatingConvergencePlots = floatingConvergencePlots;
	}


	public Integer getConvergenceElasticitiesPlotMaxSamples() {
		return convergenceElasticitiesPlotMaxSamples;
	}


	public void setConvergenceElasticitiesPlotMaxSamples(Integer convergenceElasticitiesPlotMaxSamples) {
		this.convergenceElasticitiesPlotMaxSamples = convergenceElasticitiesPlotMaxSamples;
	}

//	public boolean isHealth() {
//		return health;
//	}
//
//	public void setHealth(boolean health) {
//		this.health = health;
//	}

	public boolean isHealthByAge() {
		return healthByAge;
	}

	public void setHealthByAge(boolean healthByAge) {
		this.healthByAge = healthByAge;
	}

	public boolean isSecurityIndex() {
		return securityIndex;
	}

	public void setSecurityIndex(boolean securityIndex) {
		this.securityIndex = securityIndex;
	}

	public boolean isActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(boolean activityStatus) {
		this.activityStatus = activityStatus;
	}

	public boolean isHomeownershipStatus() {
		return homeownershipStatus;
	}

	public void setHomeownershipStatus(boolean homeownershipStatus) {
		this.homeownershipStatus = homeownershipStatus;
	}

}
