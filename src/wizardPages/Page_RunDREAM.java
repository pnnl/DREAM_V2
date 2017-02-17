package wizardPages;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
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
import utilities.ComparisonDialog;
import utilities.Constants;
import utilities.EnsembleDialog;
import utilities.Point3f;
import utilities.Point3i;
import utilities.PorosityDialog;
import wizardPages.DREAMWizard.STORMData;

/**
 * Review the summary of your setup, and choose what kind of run you would like to execute with DREAM.
 * See line 186
 * @author port091
 * @author rodr144
 */

public class Page_RunDREAM extends WizardPage implements AbstractWizardPage {

	private STORMData data;
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private Text outputFolder;
	private Text runs;
	private Text samples;
	private Text iterations;
	private Text minNumSensors;
	private Text maxNumSensors;
	private Text iterationsPerSensorNumber;

	private Button showPlots;
	
	private boolean isCurrentPage = false;
	
	protected Page_RunDREAM(STORMData data) {
		super("Run DREAM");
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
		
		Font boldFont = new Font( container.getDisplay(), new FontData( "Helvetica", 12, SWT.BOLD ) );		

		Label spacerLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		spacerLabel.setText("Run DREAM");
		GridData spacerLabelData = new GridData(GridData.FILL_HORIZONTAL);
		spacerLabelData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns - 1;
		spacerLabelData.verticalSpan = 2;
		spacerLabel.setLayoutData(spacerLabelData);
		spacerLabel.setFont(boldFont);
		
		GridData infoLinkData = new GridData(GridData.FILL_HORIZONTAL);
		infoLinkData.horizontalSpan = 1;
		infoLinkData.verticalSpan = 2;
		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.setAlignment(SWT.RIGHT);
		infoLink.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				// TODO: Catherine edit text here!
				MessageDialog.openInformation(container.getShell(), "Additional information", "The Best TTD Possible per Sensor-type button allows the user to generate a summary of the average times to detection for all scenarios and all sensor-types individually and as a whole. The \"Weighted percent of scenarios that are detectable\" is also presented, giving the user an idea for how many of the leakage scenarios read into DREAM had leaks detected according to the leakage criteria specified. The algorithm behind this button assumes an unlimited budget and an unlimited number of wells to achieve this goal. In other words, a monitoring point is placed in every node in the solution space; therefore, the results give no indication of optimal monitoring configurations. The purpose of this button is to allow the user to have an understanding of the problem before running the iterative procedure. Results identify the best possible time to detection and highest percent of scenarios detecting a leak possible.\nThe Run Iterative Procedure button will run the simulated annealing optimization algorithm the number of times specified on the number of configurations specified.");	
			}			
		});
		infoLink.setLayoutData(infoLinkData);
		
		Text summary = new Text(container, SWT.MULTI | SWT.WRAP| SWT.BORDER | SWT.V_SCROLL );
		summary.setEditable(false);
		GridData summaryGD = new GridData(GridData.FILL_BOTH);
		summaryGD.verticalSpan = 20;
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
	
		outputFolder = new Text(container, SWT.BORDER | SWT.SINGLE);
		outputFolder.setText(System.getProperty("user.dir") + "\\_results");
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
				// Run once with all sensor types - if there is more than one such type
				if(sensorsToTest.size() > 1) sensorsToTest.add(new ArrayList<String>(data.getSet().getSensorSettings().keySet()));
				
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
					
					data.runObjective(configuration, Constants.runThreaded);
					
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
						if(sensorsToTest.size() == 1 || sensors.size() > 1){
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
				text.append("Sensor,Average TTD in detected scenarios,Percentage of scenarios detected,Detected scenarios,Tested scenarios");
				for(Scenario scenario: data.getScenarioSet().getScenarios()) {
					text.append("," + scenario.getScenario());
				}
				text.append("\n");
								
				for(String sensorType: sensorTestedToTTD.keySet()) {
					
					if(sensorType.equals("Any") || data.getSet().getInferenceTest().getOverallMinimum() > 0){
						text.append(sensorType + ",");
						text.append(Constants.percentageFormat.format(sensorTestedToTTD.get(sensorType)) + ",");
						int detectedScenarios = sensorTestedScenariosDetected.get(sensorType).size();
						int scenariosTested = data.getScenarioSet().getScenarios().size();
						text.append(((float)detectedScenarios)/scenariosTested*100);
						text.append(detectedScenarios + ",");
						text.append(scenariosTested);
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
		
		Button vadButton = new Button(container, SWT.BALLOON);
		vadButton.setSelection(true);
		vadButton.setText("VAD");
		vadButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				
				HashMap<Float, Float> averages = SensorSetting.getAverageVolumeDegradedAtTimesteps();
				HashMap<Float, Float> maximums = SensorSetting.getMaxVolumeDegradedAtTimesteps();
				HashMap<Float, Float> minimums = SensorSetting.getMinVolumeDegradedAtTimesteps();
				
				StringBuilder text = new StringBuilder();
				
				// Heading
				text.append("Timestep,Average VAD over all scenarios,Minimum VAD,Maximum VAD");
				
				ArrayList<Float> years = new ArrayList<Float>(averages.keySet());
				Collections.sort(years);
				
				for(Float time: years){
					text.append("\n");
					text.append(time);
					text.append(",");
					text.append(averages.get(time));
					text.append(",");
					text.append(minimums.get(time));
					text.append(",");
					text.append(maximums.get(time));
				}
								
				try {
					File outFolder = new File(outputFolder.getText());
					if(!outFolder.exists())
						outFolder.mkdirs();
					File csvOutput = new File(new File(outputFolder.getText()), "VAD.csv");
					if(!csvOutput.exists())
						csvOutput.createNewFile();
					FileUtils.writeStringToFile(csvOutput, text.toString());
					Desktop.getDesktop().open(csvOutput);
				} catch (IOException e) {		
					JOptionPane.showMessageDialog(null, "Could not write to VAD.csv, make sure the file is not currently open");
					e.printStackTrace();
				}
			}	       
		});
		
		//Label spacer = new Label(container, SWT.NONE);


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
				printSolutionSpaceTab();
				String numRuns = runs.getText();
				int runs = numRuns.isEmpty() ? 1 : Integer.parseInt(numRuns);	
				int ittr = Integer.parseInt(iterations.getText());
				data.setWorkingDirectory(outputFolder.getText());
				data.getSet().setIterations(ittr);
				try {
					Constants.random.setSeed(1);
					long startTime = System.currentTimeMillis();
					Constants.random.setSeed(10);
					ResultPrinter.runScripts = true;
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
				Method for creating ntab file of the cloud.
				Note that neither of the authors were familiar with ntab, so this is a setup that seems to work but may have underlying issues.
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
						int nodeNumber = data.getSet().getNodeStructure().getNodeNumber(i, j, k);
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
					try {
						Desktop.getDesktop().open(outFile);
					} catch(IOException e){
						System.out.println("No default opener for .dat file extension");
					}
				}catch (IOException e) {		
					JOptionPane.showMessageDialog(null, "Could not write to solution_space.dat, make sure the file is not currently open");
					e.printStackTrace();
				}

				printSolutionSpaceTab();
			}
		});
		
		cloudButton.setVisible(Constants.buildDev);
			
		
		//TODO: These!!
		Label spacer2 = new Label(container, SWT.NULL);
		
		//This probably needs a new name. Used to create a scatterplot, hence the button, but this is the multi-run ensemble code.
		//We can probably get rid of the scatterplot functionality.
		Button scatterplotButton = new Button(container, SWT.BALLOON);
		scatterplotButton.setSelection(true);
		scatterplotButton.setText("Multi-Run Ensemble");
		scatterplotButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				EnsembleDialog dialog = new EnsembleDialog(container.getShell(), data, Integer.valueOf(iterations.getText()));
				dialog.open();
				if(dialog.readyToRun){
					//Run the ensemble (formerly scatterplot)
					printSolutionSpaceTab();
					int minNum = dialog.getMin();
					int maxNum = dialog.getMax();
					int its = dialog.getIterationsPerSensor();
					
					float oldExclusionRadius = data.getSet().getExclusionRadius();
					HashMap<String, Float> costPerType = new HashMap<String, Float>();
					for(String sensorType: data.getSet().getSensorSettings().keySet()){
						costPerType.put(sensorType, data.getSet().getCost(sensorType));
						data.getSet().getSensorSettings(sensorType).setCost(100);
					}
					
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
//					for(Float budgeti: budgets){
//						for(Integer wellj: wells){
					for(int i=minNum; i<=maxNum; ++i){
						for(int j=0; j<its; ++j){
							//For each budget and well number, run the iterative procedure and get the best configurations by TTED
							int innerWells = i;
							int innerSensors = i*100;
							System.out.println(innerSensors + " " + innerWells);
							data.getSet().setUserSettings(data.getSet().getAddPoint(), innerWells, innerSensors, dialog.getDistanceBetweenWells(), data.getSet().getWellCost(), data.getSet().getAllowMultipleSensorsInWell());
							int ittr = dialog.getIterationsPerRun();
							data.setWorkingDirectory(outputFolder.getText());
							data.getSet().setIterations(ittr);
							try {
								ResultPrinter.runScripts = false;
								if(data.run(1, false)){ //This should never show plots, we are running too many iterations.
									//this was cancelled at some point, we'd better just return now.
									return;
								}
								//get the best TTD
								
								float ttd = ResultPrinter.results.bestObjValue;
								HashSet<ExtendedConfiguration> bestConfigs = ResultPrinter.results.bestConfigSumList;
								for(Configuration config: bestConfigs){
									configurationTTDs.add(ttd);
									float cost = data.getScenarioSet().costOfConfiguration(config);
									if(cost < minCost) minCost = cost;
									if(cost > maxCost) maxCost = cost;
									configurationCosts.add(cost);
									configurationAverageTTDs.add(ResultPrinter.results.bestConfigSumTTDs.get(config));
									configurationPercentDetected.add(ResultPrinter.results.bestConfigSumPercents.get(config)*100);
									averageVolumeDegraded.add(SensorSetting.getVolumeDegradedByTTDs(config.getTimesToDetection(), data.getSet().getScenarios().size()));
									configs.add(config);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
//						}
//					}
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

					//Reset parameters that we messed with
					data.getSet().setExclusionRadius(oldExclusionRadius);
					for(String sensorType: costPerType.keySet()){
						data.getSet().getSensorSettings(sensorType).setCost(costPerType.get(sensorType));
					}
				}
			}
			});
		
		scatterplotButton.setVisible(Constants.buildDev);
		
		Button comparisonButton = new Button(container, SWT.BALLOON);
		comparisonButton.setSelection(true);
		comparisonButton.setText("Compare two sensors");
		//TODO: This is an area for future improvement to functionality. I believe Catherine has specific vision?
		comparisonButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				ComparisonDialog dialog = new ComparisonDialog(container.getShell(), data, Integer.valueOf(iterations.getText()));
				dialog.open();
				if(dialog.readyToRun){
					String sensor1 = dialog.getSensor1();
					String sensor2 = dialog.getSensor2();
					HashMap<String, Float> costStorage = new HashMap<String, Float>();
					for(String sensor: data.getSet().getDataTypes()){
						if(sensor != sensor1){
							costStorage.put(sensor, data.getScenarioSet().getCost(sensor));
							data.getSet().getSensorSettings().get(sensor).setCost(Float.MAX_VALUE);
						}
					}
					try {
						data.run(1, false); //showPlots.getSelection();
						List<List<ExtendedConfiguration>> fullSet = new ArrayList<List<ExtendedConfiguration>>();
						for(ExtendedConfiguration config: ResultPrinter.results.bestConfigSumList){
							List<ExtendedConfiguration> innerSet = new ArrayList<ExtendedConfiguration>();
							//For each config, let's run through the iterations of replacing the sensors with our original ones.
							data.runObjective(config, true);
							innerSet.add(config);
							int numSensors = config.getExtendedSensors().size();
							for(int i=1; i<=numSensors; i++){
								//Get the config with the best ttd for this
								ExtendedConfiguration bestConfig = null;
								float bestValue = Float.MAX_VALUE;
								for(int[] combo: getAllCombinations(numSensors,i)){
									//Try swapping out the sensors in these positions, and keep the best result
									ExtendedConfiguration newConfig = new ExtendedConfiguration();
									for(int j=0; j<numSensors; j++){
										boolean contains = false;
										for(int k=0; k<i; k++){
											if(combo[k] == j) contains = true;
										}
										if(contains){ //add a sensor of type 2 in the same spot
											ExtendedSensor oldSensor = config.getExtendedSensors().get(j);
											newConfig.addSensor(data.getScenarioSet(), new ExtendedSensor(
													oldSensor.getNodeNumber(),
													sensor2,
													data.getSet().getNodeStructure()));
										}
										else newConfig.addSensor(data.getScenarioSet(), config.getExtendedSensors().get(j).makeCopy()); //add this right back in
									}
									data.runObjective(newConfig, true);
									float objective = newConfig.getObjectiveValue();
									if(objective < bestValue){
										bestValue = objective;
										bestConfig = newConfig;
									}
								}
								innerSet.add(bestConfig);
							}
							fullSet.add(innerSet);
						}
						int i=0;
						StringBuilder sb = new StringBuilder();
						sb.append("Replaced Sensors,Volume Degraded,Objective Result");
						List<Scenario> scenarios = data.getSet().getScenarios();
						for(Scenario s: scenarios) sb.append("," + s.toString());
						sb.append(",Sensor Types and Locations\n");
						for(List<ExtendedConfiguration> innerSet: fullSet){
							int j=0;
							for(ExtendedConfiguration config: innerSet){
								sb.append(j == 0 ? "Original": String.valueOf(j));
								sb.append(",");
								sb.append(SensorSetting.getVolumeDegradedByTTDs(config.getTimesToDetection(), data.getSet().getScenarios().size()));
								sb.append(",");
								sb.append(config.getObjectiveValue());
								Map<Scenario, Float> ttds = config.getTimesToDetection();
								for(Scenario s: scenarios){
									sb.append(",");
									sb.append(ttds.get(s) == null ? "N/A" : ttds.get(s));
								}
								for(Sensor sensor: config.getSensors()){
									Point3f point = data.getSet().getNodeStructure().getNodeCenteredXYZFromIJK(sensor.getIJK());
									sb.append("," + sensor.getSensorType() + "("
										+ point.getX() + " " 
										+ point.getY() + " " 
										+ point.getZ() + ")");
								}
								sb.append("\n");
								j++;
							}
						}
						try{
							File outFolder = new File(outputFolder.getText());
							if(!outFolder.exists())
								outFolder.mkdirs();
							File outFile = new File(new File(outputFolder.getText()), "sensor_comparison.csv");
							if(!outFile.exists())
								outFile.createNewFile();
							FileUtils.writeStringToFile(outFile, sb.toString());
						}catch (IOException e) {		
							JOptionPane.showMessageDialog(null, "Could not write to sensor_comparison.csv, make sure the file is not currently open");
							e.printStackTrace();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					for(String type: costStorage.keySet()){
						data.getSet().getSensorSettings().get(type).setCost(costStorage.get(type));
					}
				}
				else{
					System.out.println("CANCELLED");
				}
			}
		});
		GridData sampleGD = new GridData(GridData.FILL_HORIZONTAL);
		samples.setLayoutData(sampleGD);
		comparisonButton.setVisible(Constants.buildDev);
		
		container.layout();	
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		
		DREAMWizard.visLauncher.setEnabled(true);
		DREAMWizard.convertDataButton.setEnabled(false);
	}
	
	private List<int[]> getAllCombinations(int n, int k){
		// get all combinations of size k of the first n integers
		ArrayList<int[]> combos = new ArrayList<int[]>();
		int[] x = new int[k];
		for(int i=0; i<k; i++) x[i] = i;
		combos.add(x.clone());
		while(true){
			int i;
			for(i=k-1; i>= 0 && x[i] == n-k+i; i--);
			if(i<0) break;
			else{
				x[i]++;
				for(++i; i<k; i++){
					x[i] = x[i-1] + 1;
				}
				combos.add(x.clone());
			}
		}
		
		return combos;
	}
	
	/*SCATTERPLOT BUTTON FUNCTION STORAGE - a place for old code that was once useful that could be again if we continue to re-imagine this functionality.
	 * 
				printSolutionSpaceTab();
				int minNum = Integer.parseInt(minNumSensors.getText());
				int maxNum = Integer.parseInt(maxNumSensors.getText());
				int its = Integer.parseInt(iterationsPerSensorNumber.getText());
				
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
				for(int i=minNum; i<=maxNum; ++i){
					for(int j=0; j<its; ++j){
						//For each budget and well number, run the iterative procedure and get the best configurations by TTED
						int innerWells = i;
						int innerSensors = i*100;
						System.out.println(innerSensors + " " + innerWells);
						data.getSet().setUserSettings(data.getSet().getAddPoint(), innerWells, innerSensors, data.getSet().getExclusionRadius(), data.getSet().getWellCost(), data.getSet().getAllowMultipleSensorsInWell());
						int ittr = Integer.parseInt(iterations.getText());
						data.setWorkingDirectory(outputFolder.getText());
						data.getSet().setIterations(ittr);
						try {
							ResultPrinter.runScripts = false;
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
								averageVolumeDegraded.add(SensorSetting.getVolumeDegradedByTTDs(config.getTimesToDetection(), data.getSet().getScenarios().size()));
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
	 * 
	 */
	
	public void printSolutionSpaceTab(){
		StringBuilder text = new StringBuilder();
		Point3i ijk = data.getSet().getNodeStructure().getIJKDimensions();
		text.append("x y z");
		for(String type: data.getSet().getSensorSettings().keySet()) text.append(" \"" + type + "\"");
		for(int k = 1; k <= ijk.getK(); k++) { 			
			for(int j = 1; j <= ijk.getJ(); j++) { 
				for(int i = 1; i <= ijk.getI(); i++) {
					Point3i node = new Point3i(i, j, k);
					int nodeNumber = data.getSet().getNodeStructure().getNodeNumber(node);
					Point3f xyz = data.getSet().getNodeStructure().getNodeCenteredXYZFromIJK(node);
					text.append("\n" + xyz.getX() + " " + xyz.getY() + " " + xyz.getZ());
					for(String type: data.getSet().getSensorSettings().keySet()){
						String var = ((data.getSet().getSensorSettings().get(type).getValidNodes(null).contains(nodeNumber)) ? "1" : "0");
						text.append(" " + var);
					}
				}
			}
		}
		text.append("\n");
		
		try{
			File outFolder = new File(outputFolder.getText());
			if(!outFolder.exists())
				outFolder.mkdirs();
			File outFile = new File(new File(outputFolder.getText()), "solution_space.tab");
			if(!outFile.exists())
				outFile.createNewFile();
			FileUtils.writeStringToFile(outFile, text.toString());
		}catch (IOException e) {		
			JOptionPane.showMessageDialog(null, "Could not write to solution_space.tab, make sure the file is not currently open");
			e.printStackTrace();
		}
	}

	public void convertFile(File file) throws IOException {

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
						Point3f xyz = data.getSet().getNodeStructure().getXYZEdgeFromIJK(data.getSet().getNodeStructure().getIJKFromNodeNumber(nodeNumber));
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
