package wizardPages;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

//maybe these should move elsewhere?
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.category.SlidingCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;
//These.


import objects.Configuration;
import objects.ExtendedConfiguration;
import objects.ExtendedSensor;
import objects.InferenceTest;
import objects.Scenario;
import objects.Sensor;
import objects.SensorSetting;
import objects.TecplotNode;
import results.ResultPrinter;
import utilities.Constants;
import utilities.Point3d;
import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;

public class Page_ReviewAndRun extends WizardPage implements AbstractWizardPage {

	private STORMData data;
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private Text outputFolder;
	private Text runs;
	private Text samples;
	private Text iterations;

	private Button showPlots;
	
	private boolean isCurrentPage = false;

	protected Page_ReviewAndRun(STORMData data) {
		super("Review");
		//	setDescription("Review");
		this.data = data;			
	}
	
	@Override
	public void createControl(Composite parent) {
		rootContainer = new Composite(parent, SWT.NULL);
		rootContainer.setLayout(GridLayoutFactory.fillDefaults().create());

		sc = new ScrolledComposite(rootContainer, SWT.V_SCROLL);
		sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 200).create());
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.addListener(SWT.Activate, new Listener() {
			public void handleEvent(Event e) {
				sc.setFocus();
			}
		});
		sc.addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(Event event) {
				int wheelCount = event.count;
				wheelCount = (int) Math.ceil(wheelCount / 3.0f);
				while (wheelCount < 0) {
					sc.getVerticalBar().setIncrement(4);
					wheelCount++;
				}

				while (wheelCount > 0) {
					sc.getVerticalBar().setIncrement(-4);
					wheelCount--;
				}
				sc.redraw();
			}
		});

		container = new Composite(sc, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 12;
		layout.verticalSpacing = 12;
		container.setLayout(layout);
		layout.numColumns = 3;

		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		setControl(rootContainer);
		setPageComplete(true);

	}

	@Override
	public void loadPage() {
		isCurrentPage = true;
		for(Control control: container.getChildren()) {
			control.dispose(); // Remove the children.
		}
		container.layout();	

		Text summary = new Text(container, SWT.MULTI | SWT.WRAP| SWT.BORDER | SWT.V_SCROLL );
		summary.setEditable(false);
		GridData summaryGD = new GridData(GridData.FILL_BOTH);
		summaryGD.verticalSpan = 10;
		summaryGD.widthHint = 260;
		summaryGD.grabExcessVerticalSpace = true;
		summary.setText(data.getSet().toString());
		summary.setLayoutData(summaryGD);
		
		final DirectoryDialog directoryDialog = new DirectoryDialog(container.getShell());
		Button buttonSelectDir = new Button(container, SWT.PUSH);
		buttonSelectDir.setText("Select an output directory");
		buttonSelectDir.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				directoryDialog.setFilterPath(outputFolder.getText());
				directoryDialog.setMessage("Please select a directory and click OK");
				String dir = directoryDialog.open();
				if (dir != null) {
					outputFolder.setText(dir);
				}
			}
		});		
	
		outputFolder= new Text(container, SWT.BORDER | SWT.SINGLE);
		File resultsFolder = new File(new File(".").getParent(), "_results");
		if(!resultsFolder.exists())
			resultsFolder.mkdir();		
		outputFolder.setText(resultsFolder.getAbsolutePath());
	
		GridData costGD = new GridData(GridData.FILL_HORIZONTAL);
		costGD.horizontalSpan = 1;
		outputFolder.setLayoutData(costGD);

		Button bestTTDTableButton = new Button(container, SWT.BALLOON);
		bestTTDTableButton.setSelection(true);
		bestTTDTableButton.setText("Best TTD Possible per Sensor-type");
		bestTTDTableButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				List<List<String>> sensorsToTest = new ArrayList<List<String>>();
				
				// Run once with each sensor type
				for(String sensorType: data.getSet().getSensorSettings().keySet()) {	
					List<String> justThisOne = new ArrayList<String>();
					justThisOne.add(sensorType);
					sensorsToTest.add(justThisOne);
				}
				// Run once with all sensor types
				sensorsToTest.add(new ArrayList<String>(data.getSet().getSensorSettings().keySet()));
				
				float percentDetectable = 0;
				
				Map<String, Float> sensorTestedToTTD = new HashMap<String, Float>();
				Map<String, List<String>> sensorTestedScenariosDetected = new HashMap<String, List<String>>();
				Map<String, Map<String, Float>> ttdPerSensorPerScenarioDetected = new TreeMap<String, Map<String, Float>>();
				
				for(List<String> sensors: sensorsToTest) {
					ExtendedConfiguration configuration = new ExtendedConfiguration();
					for(String sensorType: sensors) {	
						for(int nodeNumber: data.getSet().getSensorSettings().get(sensorType).getValidNodes(null)) {
							configuration.addSensor(new ExtendedSensor(nodeNumber, sensorType, data.getSet().getNodeStructure()));
						}
					}
					
					data.runObjective(configuration);
					
					float totalTimeToDetection = 0.0f;
					int detectedScenarios = 0;
					List<String> scenariosDetected = new ArrayList<String>();
					Map<String, Float> ttdForEachDetected = new HashMap<String, Float>();
					for(Scenario scenario: configuration.getTimesToDetection().keySet()) {
						float timeToDetection = configuration.getTimesToDetection().get(scenario);
						detectedScenarios++;
						totalTimeToDetection += timeToDetection;
						scenariosDetected.add(scenario.getScenario());
						ttdForEachDetected.put(scenario.getScenario(), timeToDetection);
						if(sensors.size() > 1){
							percentDetectable += data.getSet().getGloballyNormalizedScenarioWeight(scenario);
						}
					}
					
					String sensorTested = sensors.size() == 1 ? sensors.get(0) : "Any";
					sensorTestedToTTD.put(sensorTested, (totalTimeToDetection/detectedScenarios));
					sensorTestedScenariosDetected.put(sensorTested, scenariosDetected);
					
					ttdPerSensorPerScenarioDetected.put(sensorTested, ttdForEachDetected);
					
					
				}
				
				StringBuilder text = new StringBuilder();
				
				// Heading
				text.append("Sensor,Average TTD in detected scenarios, detected scenarios, tested scenarios");
				for(Scenario scenario: data.getScenarioSet().getScenarios()) {
					text.append("," + scenario.getScenario());
				}
				text.append("\n");
								
				for(String sensorType: sensorTestedToTTD.keySet()) {
					
					if(sensorType.equals("Any") || data.getSet().getInferenceTest().getOverallMinimum() > 0){
						text.append(sensorType + ",");
						text.append(Constants.percentageFormat.format(sensorTestedToTTD.get(sensorType)) + ",");
						text.append(sensorTestedScenariosDetected.get(sensorType).size() + ",");
						text.append(data.getScenarioSet().getScenarios().size());
						for(Scenario scenario: data.getScenarioSet().getScenarios()) {
							text.append("," + (ttdPerSensorPerScenarioDetected.get(sensorType).containsKey(scenario.getScenario()) ?
									 Constants.percentageFormat.format(ttdPerSensorPerScenarioDetected.get(sensorType).get(scenario.getScenario())) : ""));			
						}
						text.append("\n");
					}
					else{
						text.append(sensorType + ",");
						text.append("N/A" + ",");
						text.append("N/A" + ",");
						text.append("N/A");
						for(Scenario scenario: data.getScenarioSet().getScenarios()) {
							text.append(",N/A");
						}
						text.append("\n");
					}
				}
				
				text.append("\nWeighted percent of scenarios that are detectable:," + Constants.percentageFormat.format(percentDetectable*100));
								
				try {
					File outFolder = new File(outputFolder.getText());
					if(!outFolder.exists())
						outFolder.mkdirs();
					File csvOutput = new File(new File(outputFolder.getText()), "best_ttd_table.csv");
					if(!csvOutput.exists())
						csvOutput.createNewFile();
					FileUtils.writeStringToFile(csvOutput, text.toString());
					Desktop.getDesktop().open(csvOutput);
				} catch (IOException e) {		
					JOptionPane.showMessageDialog(null, "Could not write to best_ttd_table.csv, make sure the file is not currently open");
					e.printStackTrace();
				}
			}	       
		});		
		
		Label spacer = new Label(container, SWT.NONE);


		Button button = new Button(container, SWT.BALLOON);
		button.setSelection(true);
		button.setText("Run Iterative Procedure");

		//Label temp2 = new Label(container, SWT.NULL);
		runs= new Text(container, SWT.BORDER | SWT.SINGLE);
		runs.setText("1");		
		GridData runsGD = new GridData(GridData.FILL_HORIZONTAL);
		runs.setLayoutData(runsGD);

		Label iterationLabel = new Label(container, SWT.NULL);
		iterationLabel.setText("Configurations to test");
		iterations = new Text(container, SWT.BORDER | SWT.SINGLE);
		iterations.setText(String.valueOf(data.getSet().getIterations()));
		GridData iterationGD = new GridData(GridData.FILL_HORIZONTAL);
		iterations.setLayoutData(iterationGD);

		button.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				String numRuns = runs.getText();
				int runs = numRuns.isEmpty() ? 1 : Integer.parseInt(numRuns);	
				int ittr = Integer.parseInt(iterations.getText());
				data.setWorkingDirectory(outputFolder.getText());
				data.getSet().setIterations(ittr);
				try {
					Constants.random.setSeed(1);
					long startTime = System.currentTimeMillis();
					Constants.random.setSeed(10);
					data.run(runs, showPlots.getSelection());
					System.out.println("Iterative procedure took: " + (System.currentTimeMillis() - startTime) + "ms");
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}	       
		});

		showPlots = new Button(container, SWT.CHECK);
		showPlots.setText("Show Plots");
		new Label(container, SWT.NULL);
		showPlots.setSelection(true);

		Button button3 = new Button(container, SWT.BALLOON);
		button3.setSelection(true);
		button3.setText(" Run Full Enumeration  ");

		button3.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				data.setWorkingDirectory(outputFolder.getText());
				// TODO: Calculate iterations here
				try {
					data.runEnumeration();	
				} catch (Exception e) {
					e.printStackTrace();
				}
			}	       
		});
		
		button3.setVisible(Constants.buildDev);

		Button button4 = new Button(container, SWT.BALLOON);
		button4.setSelection(true);
		button4.setText("IJK to XYZ");
		button4.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				data.setWorkingDirectory(outputFolder.getText());
				FileDialog dialog = new FileDialog(container.getShell(), SWT.NULL);
				String path = dialog.open();
				if (path != null) {
					try {
						convertFile(new File(path));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}	       
		});

		button4.setVisible(Constants.buildDev);

		//	Label temp = new Label(container, SWT.NULL);
		//	Label temp3 = new Label(container, SWT.NULL);
		Button button2 = new Button(container, SWT.BALLOON);
		button2.setSelection(true);
		button2.setText("  Run Random Sample   ");
		samples= new Text(container, SWT.BORDER | SWT.SINGLE);
		samples.setText("20");
		button2.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				int numSamples = Integer.parseInt(samples.getText());
				data.setWorkingDirectory(outputFolder.getText());
				try {
					data.randomEnumeration(numSamples);
				} catch (Exception e) {

				}
			}	       
		});		

		button2.setVisible(Constants.buildDev);
		samples.setVisible(Constants.buildDev);

		Button cloudButton = new Button(container, SWT.BALLOON);
		cloudButton.setSelection(true);
		cloudButton.setText("Solution Space");
		cloudButton.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				/*
				JFrame temp = new JFrame();
				JTextArea textArea = new JTextArea();
				*/
				StringBuilder text = new StringBuilder();
				text.append("TITLE = Solution Space of Each Monitoring Parameter\n");
				text.append("VARIABLES  = \"X, m\"\t\"Y, m\"\t\"Z, m\"");
				for(String sensorType: data.getSet().getSensorSettings().keySet()) {
					text.append("\t\"" + sensorType + "\"");
				}
				Point3i ijk = data.getSet().getNodeStructure().getIJKDimensions();
				//Catherine said we had these backwards, switched for now
				int numElements = (ijk.getI()) * (ijk.getJ()) * (ijk.getK());
				int numNodes = 8*numElements;
				text.append("\n");
				text.append("ZONE NODES = " + numNodes + ", ELEMENTS = " + numElements + ", DATAPACKING = BLOCK, ZONETYPE = FEBRICK\n");
				
				StringBuilder varRanks = new StringBuilder();
				varRanks.append("[1, 2, 3, ");
				int rank = 4;
				for(String sensorType: data.getSet().getSensorSettings().keySet()) {
					varRanks.append((rank++) + ", ");
				}
				
				text.append("VARLOCATION = (" + varRanks.toString().substring(0, varRanks.length() - 2) + "]" + " = NODAL)\n");

				// X values
				for(int k = 0; k < ijk.getK(); k++) { 			
					for(int j = 0; j < ijk.getJ(); j++) { 
						float prevValue = 0.0f; // Assume center is at 0	
						for(int i = 0; i < ijk.getI(); i++) { 
							float nextValue = data.getSet().getNodeStructure().getX().get(i);	
							float var0f = prevValue;
							float var1f = prevValue + ((nextValue-prevValue)*2);
							prevValue = var1f;	
							String var0 = utilities.Constants.exponentialFormat.format(var0f);
							String var1 = utilities.Constants.exponentialFormat.format(var0f);							
							text.append(var0 + " " + var1 + " " + var0 + " " + var1 + " " + var0 + " " + var1 + " " + var0 + " " + var1 + "\n");	
						}
					}
				}
//				text.append("\n");
				// Y values	
				for(int k = 0; k < ijk.getK(); k++) { 
					float prevValue = 0.0f; // Assume center is at 0
					for(int j = 0; j < ijk.getJ(); j++) {
						float nextValue = data.getSet().getNodeStructure().getY().get(j);					
						float var0f = prevValue;
						float var1f = prevValue + ((nextValue-prevValue)*2);
						prevValue = var1f;	
						String var0 = utilities.Constants.exponentialFormat.format(var0f);
						String var1 = utilities.Constants.exponentialFormat.format(var0f);
						for(int i = 0; i < ijk.getI(); i++) { 
							text.append(var0 + " " + var0 + " " + var1 + " " + var1 + " " + var0 + " " + var0 + " " + var1 + " " + var1 + "\n");	
						}
					}
				}
//				text.append("\n");
				// Z values
				float prevValue = 0.0f; // Assume center is at 0				
				for(int k = 0; k < ijk.getK(); k++) { 
					float nextValue = data.getSet().getNodeStructure().getZ().get(k);					
					float var0f = prevValue;
					float var1f = prevValue + ((nextValue-prevValue)*2);
					prevValue = var1f;
					String var0 = utilities.Constants.exponentialFormat.format(var0f);
					String var1 = utilities.Constants.exponentialFormat.format(var0f);	
					for(int j = 0; j < ijk.getJ(); j++) {		
						for(int i = 0; i < ijk.getI(); i++) {				
							text.append(var0 + " " + var0 + " " + var0 + " " + var0 + " " + var1 + " " + var1 + " " + var1 + " " + var1 + "\n");	
						}
					}
				}
				//text.append("\n");
				
				// Variables
				for(String sensorType: data.getSet().getSensorSettings().keySet()) {	
					for(int k = 1; k <= ijk.getK(); k++) { for(int j = 1; j <= ijk.getJ(); j++) { for(int i = 1; i <= ijk.getI(); i++) { 
						int nodeNumber = data.getSet().getNodeStructure().getNodeNumber(new Point3i(i, j, k));
						String var0 = (data.getSet().getSensorSettings().get(sensorType).getValidNodes(null).contains(nodeNumber) ? "1" : "0");		
						text.append(var0 + " " + var0 + " " + var0 + " " + var0 + " " + var0 + " " + var0 + " " + var0 + " " + var0 + "\n");	
					}}}
					//text.append("\n");
				}
				//text.append("\n");

				//Connection List
				text.append(TecplotNode.getStringOutput(ijk.getI(), ijk.getJ(), ijk.getK()));
				
				//text.append("\n");
			
				try{
					File outFolder = new File(outputFolder.getText());
					if(!outFolder.exists())
						outFolder.mkdirs();
					File outFile = new File(new File(outputFolder.getText()), "solution_space.dat");
					if(!outFile.exists())
						outFile.createNewFile();
					FileUtils.writeStringToFile(outFile, text.toString());
					Desktop.getDesktop().open(outFile);
				}catch (IOException e) {		
					JOptionPane.showMessageDialog(null, "Could not write to solution_space.dat, make sure the file is not currently open");
					e.printStackTrace();
				}
				/*
				textArea.setText(text.toString());
				temp.add(new JScrollPane(textArea));
				temp.setSize(400, 400);
				temp.setVisible(true);
				*/
			}	       
		});
		
		cloudButton.setVisible(Constants.buildDev);
		

//		Button bestTTDButton = new Button(container, SWT.BALLOON);
//		bestTTDButton.setSelection(true);
//		bestTTDButton.setText("Best TTD Possible");
//		bestTTDButton.addListener(SWT.Selection, new Listener() {
//			@Override
//			public void handleEvent(Event arg0) {
//				ExtendedConfiguration configuration = new ExtendedConfiguration();
//				for(String sensorType: data.getSet().getSensorSettings().keySet()) {	
//					for(int nodeNumber: data.getSet().getSensorSettings().get(sensorType).getValidNodes()) {
//						configuration.addSensor(new ExtendedSensor(nodeNumber, sensorType, data.getSet().getNodeStructure()));
//					}
//				}
//				data.runObjective(configuration);
//				String text = "";
//				
//				float totalTimeToDetection = 0.0f;
//				int detectedScenarios = 0;
//				int totalScenarios = 0;
//				for(Scenario scenario: configuration.getTimesToDetection().keySet()) {
//					float timeToDetection = configuration.getTimesToDetection().get(scenario);
//					if(timeToDetection == 1000000) {
//						text += scenario.getScenario() + ": did not detect\n";
//					} else {
//						detectedScenarios++;
//						totalTimeToDetection += timeToDetection;
//						text += scenario.getScenario() + ":" + timeToDetection + "\n";
//					}
//					totalScenarios++;
//				}
//				
//				text = "TTD in detected scenarios: " + totalTimeToDetection/detectedScenarios + "\n"
//				     + "Detected scenarios: " + detectedScenarios + "/" + totalScenarios + "\n\n" 
//				     + text;
//				
//				JFrame temp = new JFrame();
//				temp.setTitle("Best possible time to detection");
//				JTextArea textArea = new JTextArea();
//				textArea.setText(text);
//				temp.add(new JScrollPane(textArea));
//				temp.setSize(400, 400);
//				temp.setVisible(true);
//			}	       
//		});		
		
		
				 
		Button scatterplotButton = new Button(container, SWT.BALLOON);
		scatterplotButton.setSelection(true);
		scatterplotButton.setText("Build Scatterplot");
		scatterplotButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				//these will be an ordered list corresponding to x and y coordinates on the scatterplots
				List<Float> configurationCosts = new ArrayList<Float>();
				List<Float> configurationAverageTTDs = new ArrayList<Float>();
				List<Float> configurationPercentDetected = new ArrayList<Float>();
				List<Float> configurationTTDs = new ArrayList<Float>();
				List<Float> averageVolumeDegraded = new ArrayList<Float>();
				List<Configuration> configs = new ArrayList<Configuration>();
				List<Float> budgets = new ArrayList<Float>();
				List<Integer> wells = new ArrayList<Integer>();
				float budget = data.getSet().getCostConstraint();
				int well = data.getSet().getMaxWells();
				//Generate the set of budges and well numbers to run over
				float budgetIncrement = budget/2;
				int wellIncrement = well/2;
				for(int i=1; i<=3; i++){
					budgets.add(budgetIncrement*i);
					wells.add(wellIncrement*i);
				}
				float minCost = Float.MAX_VALUE;
				float maxCost = 0;
//				for(Float budgeti: budgets){
//					for(Integer wellj: wells){
				for(int i=1; i<=50; ++i){
					for(int j=0; j<5; ++j){
						//For each budget and well number, run the iterative procedure and get the best configurations by TTED
						int innerWells = i;
						int innerSensors = i*100;
						System.out.println(innerSensors + " " + innerWells);
						data.getSet().setUserSettings(data.getSet().getAddPoint(), innerWells, innerSensors, data.getSet().getExclusionRadius(), data.getSet().getWellCost(), data.getSet().getAllowMultipleSensorsInWell());
						String numRuns = runs.getText();
						int ittr = Integer.parseInt(iterations.getText());
						data.setWorkingDirectory(outputFolder.getText());
						data.getSet().setIterations(ittr);
						try {
							data.run(1, false); //This should never show plots, we are running too many iterations.
							//get the best TTD
							float ttd = ResultPrinter.results.bestObjValue;
							HashSet<Configuration> bestConfigs = ResultPrinter.results.bestConfigSumList;
							for(Configuration config: bestConfigs){
								configurationTTDs.add(ttd);
								float cost = data.getScenarioSet().costOfConfiguration(config);
								if(cost < minCost) minCost = cost;
								if(cost > maxCost) maxCost = cost;
								configurationCosts.add(cost);
								configurationAverageTTDs.add(ResultPrinter.results.bestConfigSumTTDs.get(config));
								configurationPercentDetected.add(ResultPrinter.results.bestConfigSumPercents.get(config)*100);
								averageVolumeDegraded.add(SensorSetting.getVolumeDegraded(config.getTimesToDetection(), data.getSet().getScenarios().size()));
								configs.add(config);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
//					}
//				}
				//Set this back to what it was so we don't mess up future runs
				data.getSet().setUserSettings(data.getSet().getAddPoint(), well, budget, data.getSet().getExclusionRadius(), data.getSet().getWellCost(), data.getSet().getAllowMultipleSensorsInWell());
				
				//Print our results in a csv file
				try {
					ResultPrinter.printPlotData(configurationPercentDetected, configurationAverageTTDs, configurationCosts, configs, averageVolumeDegraded);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//Make the plot with our points
				XYSeriesCollection result = new XYSeriesCollection();
				XYSeries highSeries = new XYSeries("High Cost");
				XYSeries mediumSeries = new XYSeries("Medium Cost");
				XYSeries lowSeries = new XYSeries("Low Cost");
				for(int i=0; i<configurationTTDs.size(); i++){
					if(configurationCosts.get(i) < (maxCost - minCost)/3 + minCost) lowSeries.add(configurationPercentDetected.get(i), configurationAverageTTDs.get(i));
					else if(configurationCosts.get(i) > -(maxCost - minCost)/3 + maxCost) highSeries.add(configurationPercentDetected.get(i), configurationAverageTTDs.get(i)); 
					else mediumSeries.add(configurationPercentDetected.get(i), configurationAverageTTDs.get(i));
					System.out.println(configurationCosts.get(i) + "\t" + configurationTTDs.get(i));
				}
				result.addSeries(highSeries);
				result.addSeries(mediumSeries);
				result.addSeries(lowSeries);
				
				JFreeChart chart = ChartFactory.createScatterPlot(
						"Average TTD of Detecting Scenarios as a Function of the Percent of Scenarios Detected", //title
						"Percent of Scenarios Detected", //x axis label
						"Average Time to Detection of Detecting Scenarios", //y axis label
						result, //data
						PlotOrientation.VERTICAL, //orientation
						true, //legend
						false, //tooltips 
						false); //urls
				ChartFrame frame = new ChartFrame("Cost-TTD Scatter", chart);
				frame.pack();
				frame.setVisible(true);
				
			}
			});
		
		scatterplotButton.setVisible(Constants.buildDev);
		
		GridData sampleGD = new GridData(GridData.FILL_HORIZONTAL);
		samples.setLayoutData(sampleGD);
		
		
		container.layout();	
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		
		DREAMWizard.visLauncher.setEnabled(true);
		DREAMWizard.convertDataButton.setEnabled(false);

	}
	

	public void convertFile(File file) throws IOException {
		/*
		System.out.println("i, x edge, x center");
		for(int i = 1; i <= data.getSet().getNodeStructure().getIJKDimensions().getI(); i++) {
			Point3i node = data.getSet().getNodeStructure().getIJKFromNodeNumber(data.getSet().getNodeStructure().getNodeNumber(i, 1, 1));
			double edge = data.getSet().getNodeStructure().getXYZFromIJK(node).getX();
			double center = data.getSet().getNodeStructure().getNodeCenteredXYZFromIJK(node).getX();
			System.out.println(i + ", " + edge + ", " + center);
		}
		System.out.println("\n\nj, y edge, y center");
		for(int j = 1; j <= data.getSet().getNodeStructure().getIJKDimensions().getJ(); j++) {
			Point3i node = data.getSet().getNodeStructure().getIJKFromNodeNumber(data.getSet().getNodeStructure().getNodeNumber(1, j, 1));
			double edge = data.getSet().getNodeStructure().getXYZFromIJK(node).getY();
			double center = data.getSet().getNodeStructure().getNodeCenteredXYZFromIJK(node).getY();
			System.out.println(j + ", " + edge + ", " + center);
		}
		System.out.println("\n\nk, z edge, z center");
		for(int k = 1; k <= data.getSet().getNodeStructure().getIJKDimensions().getK(); k++) {
			Point3i node = data.getSet().getNodeStructure().getIJKFromNodeNumber(data.getSet().getNodeStructure().getNodeNumber(1, 1, k));
			double edge = data.getSet().getNodeStructure().getXYZFromIJK(node).getZ();
			double center = data.getSet().getNodeStructure().getNodeCenteredXYZFromIJK(node).getZ();
			System.out.println(k + ", " + edge + ", " + center);
		}
		 */
		List<String> lines = FileUtils.readLines(file);
		StringBuffer fileOut = new StringBuffer();
		for(String line: lines) {
			Map<String, String> nodesToReplace = new HashMap<String, String>();
			// If the line contains any node ids, we need to convert them to xyz locations
			String[] groups = line.split("\\(");
			for(String group: groups) {
				String[] individualSensors = group.split(",");
				for(String individualSensor: individualSensors) {
					String[] parts = individualSensor.split(":");
					if(parts.length == 3) {
						int nodeNumber = Integer.parseInt(parts[0].trim());
						Point3d xyz = data.getSet().getNodeStructure().getXYZEdgeFromIJK(data.getSet().getNodeStructure().getIJKFromNodeNumber(nodeNumber));
						nodesToReplace.put(parts[0], xyz.toString());
					} 
				}
			}
			String lineOut = line;
			for(String nodeToReplace: nodesToReplace.keySet()) {
				lineOut = lineOut.replaceAll(nodeToReplace, nodesToReplace.get(nodeToReplace));
			}
			fileOut.append(lineOut + "\n");
		}
		System.out.println(fileOut.toString());
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
	}

	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}
	@Override
	public void setPageCurrent(boolean current) {
		isCurrentPage = current;
	}

}
