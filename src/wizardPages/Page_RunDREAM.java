package wizardPages;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

//maybe these should move elsewhere?
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import objects.Configuration;
import objects.ExtendedConfiguration;
import objects.ExtendedSensor;
import objects.NodeStructure;
import objects.Sensor;
import objects.SensorSetting;
import objects.TecplotNode;
import results.ResultPrinter;
import utilities.ComparisonDialog;
import utilities.Constants;
import utilities.EnsembleDialog;
import utilities.Point3f;
import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;

/**
 * Review the summary of your setup, and choose what kind of run you would like to execute with DREAM.
 * See line 186
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class Page_RunDREAM extends DreamWizardPage implements AbstractWizardPage {
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private STORMData data;
	
	private Button bestTTDTableButton;
	private Button vadButton;
	private Button iterativeProceedureButton;
	private Button showPlots;
	private Button fullEnumerationButton;
	private Button ijkToxyzButton;
	private Button randomSampleButton;
	private Button solutionSpaceButton;
	private Button multiRunEnsembleButton;
	private Button comparisonButton;
	private Button detectionMapButton;
	
	private Text outputFolder;
	private Text runsText;
	private Text iterationsText;
	private Text samplesText;
	
	private boolean outputError;
	private boolean runsError;
	private boolean iterationsError;
	private boolean samplesError;
	
	private String outputs = (Constants.runningJar ? Constants.userDir : Constants.parentDir) + File.separator + "_results";
	private int runs = 1;
	private int samples = 20;
	
	private boolean isCurrentPage = false;
	
	protected Page_RunDREAM(STORMData data) {
		super("Run DREAM");
		this.data = data;			
	}
	
	@Override
	public void createControl(final Composite parent) {
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
		layout.numColumns = 2;
		container.setLayout(layout);

		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		setControl(rootContainer);
		setPageComplete(true);

	}

	@Override
	public void loadPage() {
		isCurrentPage = true;
		removeChildren(container);
		container.layout();
		NodeStructure structure = data.getSet().getNodeStructure();
		
		Font boldFont = new Font( container.getDisplay(), new FontData( "Helvetica", 12, SWT.BOLD ) );
		Font boldFontSmall = new Font( container.getDisplay(), new FontData( "Helvetica", 10, SWT.BOLD ) );
		GridData spanData = new GridData();
		spanData.horizontalSpan = 2;

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
				MessageDialog.openInformation(container.getShell(), "Additional information", "To the left is a summary of all the inputs previously entered. The Run Iterative Procedure "
						+ "button will run the simulated annealing optimization algorithm the number of times specified on the number of configurations specified. The user may choose to hide "
						+ "the iterative performance plots and visualization tool to speed up the computational process. The multi-run ensemble increments between a minimum and maximum number of "
						+ "allowed monitoring locations, running a full DREAM optimization for each setting. Diagnostic tools are also available, with descriptions in the manual.");	
			}			
		});
		infoLink.setLayoutData(infoLinkData);
		
		// Starts the left side of the page
		GridData summaryGD = new GridData(GridData.FILL_BOTH);
		summaryGD.verticalSpan = 10;
		summaryGD.widthHint = 260;
		summaryGD.heightHint = SWT.FILL;
		summaryGD.grabExcessVerticalSpace = true;
		
		Text summary = new Text(container, SWT.MULTI | SWT.WRAP| SWT.BORDER | SWT.V_SCROLL );
		summary.setEditable(false);
		summary.setText(data.getSet().toString());
		summary.setLayoutData(summaryGD);
		
		
		// Starts the right side of the page
		Group runGroup = new Group(container, SWT.SHADOW_NONE);
		runGroup.setText("Run DREAM");
		runGroup.setFont(boldFontSmall);
		runGroup.setLayout(new GridLayout(2,false));
		runGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				
		final DirectoryDialog directoryDialog = new DirectoryDialog(container.getShell());
		Button buttonSelectDir = new Button(runGroup, SWT.PUSH);
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
	
		outputFolder = new Text(runGroup, SWT.BORDER | SWT.SINGLE);
		outputFolder.setText(outputs);
		outputFolder.setForeground(Constants.black);
		outputFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		outputFolder.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				outputError = false;
	            try {
	            	File resultsFolder = new File(((Text)e.getSource()).getText());
	            	outputs = resultsFolder.getCanonicalPath();
	            	((Text)e.getSource()).setForeground(Constants.black);
	            } catch (Exception ex) {
	            	((Text)e.getSource()).setForeground(Constants.red);
	            	outputError = true;
	            }
	            errorFound(outputError, "  Results folder must use valid characters.");
				if (outputError) {
					bestTTDTableButton.setEnabled(false);
					vadButton.setEnabled(false);
					iterativeProceedureButton.setEnabled(false);
					fullEnumerationButton.setEnabled(false);
					ijkToxyzButton.setEnabled(false);
					randomSampleButton.setEnabled(false);
					solutionSpaceButton.setEnabled(false);
					multiRunEnsembleButton.setEnabled(false);
					comparisonButton.setEnabled(false);
					detectionMapButton.setEnabled(false);
				} else {
					bestTTDTableButton.setEnabled(true);
					vadButton.setEnabled(true);
					if (!runsError && !iterationsError)
						iterativeProceedureButton.setEnabled(true);
					fullEnumerationButton.setEnabled(true);
					ijkToxyzButton.setEnabled(true);
					if (!samplesError)
						randomSampleButton.setEnabled(true);
					solutionSpaceButton.setEnabled(true);
					multiRunEnsembleButton.setEnabled(true);
					comparisonButton.setEnabled(true);
					detectionMapButton.setEnabled(true);
				}
			}
		});
		
		//Begin the process for determining array optimization
		iterativeProceedureButton = new Button(runGroup, SWT.BALLOON);
		iterativeProceedureButton.setSelection(true);
		iterativeProceedureButton.setText("Run Iterative Procedure");
		iterativeProceedureButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				printSolutionSpace();
				int runs = Integer.parseInt(runsText.getText());
				int ittr = Integer.parseInt(iterationsText.getText());
				data.setWorkingDirectory(outputFolder.getText());
				data.getSet().setIterations(ittr);
				try {
					Constants.random.setSeed(1);
					long startTime = System.currentTimeMillis();
					Constants.random.setSeed(10);
					ResultPrinter.runScripts = false;
					data.run(runs, showPlots.getSelection());
					long time = (System.currentTimeMillis() - startTime) / 1000;
					System.out.println("Iterative procedure took: " + time + "s");
					
					//create the dialog box
					MessageBox dialog = new MessageBox(container.getShell(), SWT.OK);
					dialog.setText("Completed the Dream Run");
					if(time>18000)
						dialog.setMessage("Dream just completed " + ittr + " iterations in " + time/3600 + " hours. Results can be found at: " + outputFolder.getText());
					else if(time>300)
						dialog.setMessage("Dream just completed " + ittr + " iterations in " + time/60 + " minutes. Results can be found at: " + outputFolder.getText());
					else
						dialog.setMessage("Dream just completed " + ittr + " iterations in " + time + " seconds. Results can be found at: " + outputFolder.getText());
					dialog.open();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		runsText = new Text(runGroup, SWT.BORDER | SWT.SINGLE);
		runsText.setText(String.valueOf(runs));
		runsText.setForeground(Constants.black);
		runsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		runsText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				runsError = false;
				if(Constants.isValidInt(((Text)e.getSource()).getText())) { //Valid number
					((Text)e.getSource()).setForeground(Constants.black);
					runs = Integer.parseInt(((Text)e.getSource()).getText());
				} else { //Not a valid number
					((Text)e.getSource()).setForeground(Constants.red);
					runsError = true;
				}
				errorFound(runsError, "  Runs is not a real number.");
				if (iterationsError || runsError)
					iterativeProceedureButton.setEnabled(false);
				else if (!outputError)
					iterativeProceedureButton.setEnabled(true);
			}
		});

		Label iterationLabel = new Label(runGroup, SWT.NULL);
		iterationLabel.setText("Configurations to test");
		iterationsText = new Text(runGroup, SWT.BORDER | SWT.SINGLE);
		iterationsText.setText(String.valueOf(data.getSet().getIterations()));
		iterationsText.setForeground(Constants.black);
		iterationsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		iterationsText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				iterationsError = false;
				if(Constants.isValidInt(((Text)e.getSource()).getText())) { //Valid number
					((Text)e.getSource()).setForeground(Constants.black);
					data.getSet().setIterations(Integer.parseInt(((Text)e.getSource()).getText()));
				} else { //Not a valid number
					((Text)e.getSource()).setForeground(Constants.red);
					iterationsError = true;
				}
				errorFound(iterationsError, "  Iterations is not a real number.");
				if (iterationsError || runsError)
					iterativeProceedureButton.setEnabled(false);
				else if (!outputError)
					iterativeProceedureButton.setEnabled(true);
			}
		});
		
		// Deselecting the Show Plots button will prevent the visualization from populating (runs fast)
		showPlots = new Button(runGroup, SWT.CHECK);
		showPlots.setText("Show Plots");
		showPlots.setSelection(true);
		new Label(runGroup, SWT.NULL);
		
		Group diagnosticGroup = new Group(container, SWT.SHADOW_NONE);
		diagnosticGroup.setText("Diagnostic Tools");
		diagnosticGroup.setFont(boldFontSmall);
		diagnosticGroup.setLayout(new GridLayout(2,false));
		diagnosticGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		//If a sensor were placed at every node, provide the best possible time to detection
		bestTTDTableButton = new Button(diagnosticGroup, SWT.BALLOON);
		bestTTDTableButton.setSelection(true);
		bestTTDTableButton.setText("Best TTD Possible per Technology");
		bestTTDTableButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				List<List<String>> sensorsToTest = new ArrayList<List<String>>();
				String timeUnit = structure.getUnit("times");
				
				// Run once with each sensor type
				for(String sensorType: data.getSet().getSensorSettings().keySet()) {	
					List<String> justThisOne = new ArrayList<String>();
					justThisOne.add(sensorType);
					sensorsToTest.add(justThisOne);
				}
				// Run once with all sensor types - if there is more than one such type
				if(sensorsToTest.size() > 1) sensorsToTest.add(new ArrayList<String>(data.getSet().getSensorSettings().keySet()));
								
				Map<String, Float> sensorTestedToTTD = new TreeMap<String, Float>();
				Map<String, List<String>> sensorTestedScenariosDetected = new HashMap<String, List<String>>();
				Map<String, Map<String, Float>> ttdPerSensorPerScenarioDetected = new HashMap<String, Map<String, Float>>();
				
				for(List<String> sensors: sensorsToTest) {
					ExtendedConfiguration configuration = new ExtendedConfiguration();
					for(String sensorType: sensors) {	
						for(int nodeNumber: data.getSet().getSensorSettings().get(sensorType).getValidNodes()) {
							configuration.addSensor(new ExtendedSensor(nodeNumber, sensorType, structure));
						}
					}
					
					data.runObjective(configuration);
					
					float totalTimeToDetection = 0.0f;
					int detectedScenarios = 0;
					List<String> scenariosDetected = new ArrayList<String>();
					Map<String, Float> ttdForEachDetected = new HashMap<String, Float>();
					for(String scenario: configuration.getTimesToDetection().keySet()) {
						float timeToDetection = configuration.getTimesToDetection().get(scenario);
						detectedScenarios++;
						totalTimeToDetection += timeToDetection;
						scenariosDetected.add(scenario);
						ttdForEachDetected.put(scenario, timeToDetection);
					}
					
					String sensorTested = sensors.size() == 1 ? sensors.get(0) : "Any";
					sensorTestedToTTD.put(sensorTested, (totalTimeToDetection/detectedScenarios));
					sensorTestedScenariosDetected.put(sensorTested, scenariosDetected);
					ttdPerSensorPerScenarioDetected.put(sensorTested, ttdForEachDetected);
				}
				
				// Now write out the results into a CSV file
				StringBuilder text = new StringBuilder();
				
				// Sensors
				text.append("Parameters");
				for(String sensorType: sensorTestedToTTD.keySet()) {
					text.append("," +sensorType);
				}
				// Average TTD in detected scenarios
				text.append("\nAverage TTD for detected scenarios ("+timeUnit+")");
				for(String sensorType: sensorTestedToTTD.keySet()) {
					text.append("," + Constants.percentageFormat.format(sensorTestedToTTD.get(sensorType)));
				}
				// Detected scenarios
				text.append("\nDetected scenarios");
				for(String sensorType: sensorTestedToTTD.keySet()) {
					int detectedScenarios = sensorTestedScenariosDetected.get(sensorType).size();
					int scenariosTested = data.getSet().getScenarios().size();
					text.append("," + detectedScenarios + " of " + scenariosTested);
				}
				// Weighted percentage of scenarios detected
				text.append("\nDetected scenarios (weighted %)");
				for(String sensorType: sensorTestedToTTD.keySet()) {
					float percent = 0;
					for(String scenario: sensorTestedScenariosDetected.get(sensorType))
						percent += data.getSet().getGloballyNormalizedScenarioWeight(scenario)*100;
					text.append("," + Constants.percentageFormat.format(percent) + "%");
				}
				// Now list best TTD per scenario
				for(String scenario: data.getSet().getScenarios()) {
					text.append("\nBest TTD for "+scenario+" ("+timeUnit+")");
					for(String sensorType: sensorTestedToTTD.keySet()) {
						text.append("," + (ttdPerSensorPerScenarioDetected.get(sensorType).containsKey(scenario) ?
								Constants.percentageFormat.format(ttdPerSensorPerScenarioDetected.get(sensorType).get(scenario)): ""));
					}
				}
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
		
		//Volume of aquifer degraded
		vadButton = new Button(diagnosticGroup, SWT.BALLOON);
		vadButton.setSelection(true);
		vadButton.setText("Volume of Aquifer Degraded");
		vadButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				
				HashMap<Float, Float> averages = SensorSetting.getAverageVolumeDegradedAtTimesteps();
				HashMap<Float, Float> maximums = SensorSetting.getMaxVolumeDegradedAtTimesteps();
				HashMap<Float, Float> minimums = SensorSetting.getMinVolumeDegradedAtTimesteps();
				String xUnit = structure.getUnit("x");
				String timeUnit = structure.getUnit("times");
				
				StringBuilder text = new StringBuilder();
				
				// Heading
				text.append("Time ("+timeUnit+"),Average VAD ("+xUnit+"³),Minimum VAD ("+xUnit+"^3),Maximum VAD ("+xUnit+"^3)");
				
				ArrayList<Float> years = new ArrayList<Float>(averages.keySet());
				Collections.sort(years);
				
				for(Float time: years){
					text.append("\n" + Constants.percentageFormat.format(time));
					text.append("," + Constants.percentageFormat.format(averages.get(time)));
					text.append("," + Constants.percentageFormat.format(minimums.get(time)));
					text.append("," + Constants.percentageFormat.format(maximums.get(time)));
				}
								
				try {
					File outFolder = new File(outputFolder.getText());
					if(!outFolder.exists())
						outFolder.mkdirs();
					File csvOutput = new File(new File(outputFolder.getText()), "VolumeOfAquiferDegraded.csv");
					if(!csvOutput.exists())
						csvOutput.createNewFile();
					FileUtils.writeStringToFile(csvOutput, text.toString());
					Desktop.getDesktop().open(csvOutput);
				} catch (IOException e) {		
					JOptionPane.showMessageDialog(null, "Could not write to VolumeOfAquiferDegraded.csv, make sure the file is not currently open");
					e.printStackTrace();
				}
			}	       
		});
		
		Group devGroup = new Group(container, SWT.SHADOW_NONE);
		devGroup.setText("Development Tools (Only Dev Release)");
		devGroup.setFont(boldFontSmall);
		devGroup.setLayout(new GridLayout(2,false));
		devGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		devGroup.setVisible(Constants.buildDev);
		
		fullEnumerationButton = new Button(devGroup, SWT.BALLOON);
		fullEnumerationButton.setSelection(true);
		fullEnumerationButton.setText(" Run Full Enumeration  ");
		fullEnumerationButton.addListener(SWT.Selection, new Listener() {
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
		fullEnumerationButton.setVisible(Constants.buildDev);
		
		ijkToxyzButton = new Button(devGroup, SWT.BALLOON);
		ijkToxyzButton.setSelection(true);
		ijkToxyzButton.setText("IJK to XYZ");
		ijkToxyzButton.addListener(SWT.Selection, new Listener() {
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
		ijkToxyzButton.setVisible(Constants.buildDev);

		randomSampleButton = new Button(devGroup, SWT.BALLOON);
		randomSampleButton.setSelection(true);
		randomSampleButton.setText("  Run Random Sample   ");
		randomSampleButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				int numSamples = Integer.parseInt(samplesText.getText());
				data.setWorkingDirectory(outputFolder.getText());
				try {
					data.randomEnumeration(numSamples);
				} catch (Exception e) {

				}
			}	       
		});		
		randomSampleButton.setVisible(Constants.buildDev);
		
		samplesText = new Text(devGroup, SWT.BORDER | SWT.SINGLE);
		samplesText.setText(String.valueOf(samples));
		samplesText.setForeground(Constants.black);
		samplesText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		samplesText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				samplesError = false;
				if(Constants.isValidInt(((Text)e.getSource()).getText())) { //Valid number
					((Text)e.getSource()).setForeground(Constants.black);
					samples = Integer.parseInt(((Text)e.getSource()).getText());
				} else { //Not a valid number
					((Text)e.getSource()).setForeground(Constants.red);
					samplesError = true;
				}
				errorFound(samplesError, "  Samples is not a real number.");
				if (samplesError)
					randomSampleButton.setEnabled(false);
				else if (!outputError)
					randomSampleButton.setEnabled(true);
			}
		});
		samplesText.setVisible(Constants.buildDev);

		solutionSpaceButton = new Button(devGroup, SWT.BALLOON);
		solutionSpaceButton.setSelection(true);
		solutionSpaceButton.setText("Solution Space");
		solutionSpaceButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				/*
				Method for creating Tecplot file of the cloud.
				Note that neither of the authors were familiar with Tecplot, so this is a setup that seems to work but may have underlying issues.
				*/
				String unit = structure.getUnit("x");
				StringBuilder text = new StringBuilder();
				text.append("TITLE = Solution Space of Each Monitoring Parameter\n");
				text.append("VARIABLES = ");
				text.append("\t\"X" + (unit.equals("") ? "" : ", " + unit) + "\"");
				text.append("\t\"Y" + (unit.equals("") ? "" : ", " + unit) + "\"");
				text.append("\t\"Z" + (unit.equals("") ? "" : ", " + unit) + "\"");
				for(String sensorType: data.getSet().getSensorSettings().keySet()) {
					unit = data.getSet().getNodeStructure().getUnit(sensorType);
					text.append("\t\"" + sensorType + (unit.equals("") ? "" : ", " + unit) + "\"");
				}
				Point3i ijk = structure.getIJKDimensions();
				//Catherine said we had these backwards, switched for now
				int numElements = (ijk.getI()) * (ijk.getJ()) * (ijk.getK());
				int numNodes = 8*numElements;
				text.append("\n");
				text.append("ZONE NODES = " + numNodes + ", ELEMENTS = " + numElements + ", DATAPACKING = BLOCK, ZONETYPE = FEBRICK\n");
				
				StringBuilder varRanks = new StringBuilder();
				varRanks.append("[1, 2, 3, ");
				int rank = 4;
				for(int i = 0; i < data.getSet().getSensorSettings().keySet().size(); i++)
					varRanks.append((rank++) + ", ");
				text.append("VARLOCATION = (" + varRanks.toString().substring(0, varRanks.length() - 2) + "]" + " = NODAL)\n");

				// X values
				for(int k = 0; k < ijk.getK(); k++) {
					for(int j = 0; j < ijk.getJ(); j++) {
						float prevValue = 0.0f; // Assume center is at 0
						for(int i = 0; i < ijk.getI(); i++) {
							float nextValue = structure.getX().get(i);
							float var0f = prevValue;
							float var1f = prevValue + ((nextValue-prevValue)*2);
							prevValue = var1f;
							String var0 = utilities.Constants.exponentialFormat.format(var0f);
							String var1 = utilities.Constants.exponentialFormat.format(var0f);
							text.append(var0 + " " + var1 + " " + var0 + " " + var1 + " " + var0 + " " + var1 + " " + var0 + " " + var1 + "\n");
						}
					}
				}
				// Y values
				for(int k = 0; k < ijk.getK(); k++) {
					float prevValue = 0.0f; // Assume center is at 0
					for(int j = 0; j < ijk.getJ(); j++) {
						float nextValue = structure.getY().get(j);
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
				// Z values
				float prevValue = 0.0f; // Assume center is at 0
				for(int k = 0; k < ijk.getK(); k++) {
					float nextValue = structure.getZ().get(k);
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
				// Variables
				for(String sensorType: data.getSet().getSensorSettings().keySet()) {
					for(int k = 1; k <= ijk.getK(); k++) { for(int j = 1; j <= ijk.getJ(); j++) { for(int i = 1; i <= ijk.getI(); i++) {
						int nodeNumber = structure.getNodeNumber(i, j, k);
						String var0 = (data.getSet().getSensorSettings().get(sensorType).getValidNodes().contains(nodeNumber) ? "1" : "0");
						text.append(var0 + " " + var0 + " " + var0 + " " + var0 + " " + var0 + " " + var0 + " " + var0 + " " + var0 + "\n");
					}}}
				}
				
				//Connection List
				text.append(TecplotNode.getStringOutput(ijk.getI(), ijk.getJ(), ijk.getK()));
				
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
				
				printSolutionSpace();
			}
		});
		solutionSpaceButton.setVisible(Constants.buildDev);
		
		
		//This probably needs a new name. Used to create a scatterplot, hence the button, but this is the multi-run ensemble code.
		//We can probably get rid of the scatterplot functionality.
		multiRunEnsembleButton = new Button(devGroup, SWT.BALLOON);
		multiRunEnsembleButton.setSelection(true);
		multiRunEnsembleButton.setText("Multi-Run Ensemble");
		multiRunEnsembleButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				EnsembleDialog dialog = new EnsembleDialog(container.getShell(), data, Integer.valueOf(iterationsText.getText()));
				dialog.open();
				if(dialog.readyToRun){
					//Run the ensemble (formerly scatterplot)
					printSolutionSpace();
					int minNum = dialog.getMin();
					int maxNum = dialog.getMax();
					int its = dialog.getIterationsPerSensor();
					
					float oldExclusionRadius = data.getSet().getExclusionRadius();
					HashMap<String, Float> costPerType = new HashMap<String, Float>();
					for(String sensorType: data.getSet().getSensorSettings().keySet()){
						costPerType.put(sensorType, data.getSet().getSensorSettings(sensorType).getSensorCost());
						data.getSet().getSensorSettings(sensorType).setSensorCost(100);
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
					float budget = data.getSet().getSensorCostConstraint();
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
							data.getSet().setUserSettings(data.getSet().getAddPoint(), innerWells, innerSensors, data.getSet().getExclusionRadius(), data.getSet().getWellCost(), data.getSet().getWellDepthCost(), data.getSet().getRemediationCost());
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
								for(ExtendedConfiguration config: bestConfigs){
									configurationTTDs.add(ttd);
									float cost = data.getSet().costOfConfiguration(config);
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
					data.getSet().setUserSettings(data.getSet().getAddPoint(), well, budget, data.getSet().getExclusionRadius(), data.getSet().getWellCost(), data.getSet().getWellDepthCost(), data.getSet().getRemediationCost());
					
					//Print our results in a CSV file
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
						data.getSet().getSensorSettings(sensorType).setSensorCost(costPerType.get(sensorType));
					}
				}
			}
			});
		multiRunEnsembleButton.setVisible(Constants.buildDev);
		
		comparisonButton = new Button(devGroup, SWT.BALLOON);
		comparisonButton.setSelection(true);
		comparisonButton.setText("Compare two sensors");
		//TODO: This is an area for future improvement to functionality. I believe Catherine has specific vision?
		comparisonButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				ComparisonDialog dialog = new ComparisonDialog(container.getShell(), data, Integer.valueOf(iterationsText.getText()));
				dialog.open();
				if(dialog.readyToRun){
					String sensor1 = dialog.getSensor1();
					String sensor2 = dialog.getSensor2();
					HashMap<String, Float> costStorage = new HashMap<String, Float>();
					for(String sensor: data.getSet().getDataTypes()){
						if(sensor != sensor1){
							costStorage.put(sensor, data.getSet().getSensorSettings(sensor).getSensorCost());
							data.getSet().getSensorSettings().get(sensor).setSensorCost(Float.MAX_VALUE);
						}
					}
					try {
						data.run(1, false); //showPlots.getSelection();
						List<List<ExtendedConfiguration>> fullSet = new ArrayList<List<ExtendedConfiguration>>();
						for(ExtendedConfiguration config: ResultPrinter.results.bestConfigSumList){
							List<ExtendedConfiguration> innerSet = new ArrayList<ExtendedConfiguration>();
							//For each config, let's run through the iterations of replacing the sensors with our original ones.
							data.runObjective(config);
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
											newConfig.addSensor(new ExtendedSensor(oldSensor.getNodeNumber(), sensor2, structure));
										}
										else newConfig.addSensor(config.getExtendedSensors().get(j).makeCopy()); //add this right back in
									}
									data.runObjective(newConfig);
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
						StringBuilder sb = new StringBuilder();
						sb.append("Replaced Sensors,Volume Degraded,Objective Result");
						List<String> scenarios = data.getSet().getScenarios();
						for(String s: scenarios) sb.append("," + s.toString());
						sb.append(",Sensor Types and Locations\n");
						for(List<ExtendedConfiguration> innerSet: fullSet){
							int j=0;
							for(ExtendedConfiguration config: innerSet){
								sb.append(j == 0 ? "Original": String.valueOf(j));
								sb.append(",");
								sb.append(SensorSetting.getVolumeDegradedByTTDs(config.getTimesToDetection(), data.getSet().getScenarios().size()));
								sb.append(",");
								sb.append(config.getObjectiveValue());
								Map<String, Float> ttds = config.getTimesToDetection();
								for(String s: scenarios){
									sb.append(",");
									sb.append(ttds.get(s) == null ? "N/A" : ttds.get(s));
								}
								for(Sensor sensor: config.getSensors()){
									Point3f point = structure.getNodeCenteredXYZFromIJK(sensor.getIJK());
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
						data.getSet().getSensorSettings().get(type).setSensorCost(costStorage.get(type));
					}
				}
				else{
					System.out.println("CANCELLED");
				}
			}
		});
		comparisonButton.setVisible(Constants.buildDev);
		
		// This writes out information from the detection map
		detectionMapButton = new Button(devGroup, SWT.BALLOON);
		detectionMapButton.setSelection(true);
		detectionMapButton.setText("Write Detection Map");
		detectionMapButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				
				long startTime = System.currentTimeMillis();
				List<String> scenarios = data.getSet().getScenarios(); //All scenarios in the set
				
				// Each parameter should write its own file
				for(String parameter: data.getSet().getDetectionMap().keySet()) {
					// Skip if sensorSettings doesn't contain the parameter
					if(!data.getSet().getSensorSettings().containsKey(parameter.split("_")[0])) continue;
					// Initialize arrays to save statistics across all scenarios
					List<Integer> cloudNodes = new ArrayList<Integer>(data.getSet().getSensorSettings(parameter.split("_")[0]).getCloudNodes());
					float[][] ttds = new float[cloudNodes.size()][scenarios.size()];
					float[] min = new float[cloudNodes.size()];
					Arrays.fill(min, Float.MAX_VALUE);
					float[] max = new float[cloudNodes.size()];
					Arrays.fill(max, Float.MIN_VALUE);
					float[] avg = new float[cloudNodes.size()];
					float[] count = new float[cloudNodes.size()];
					// Each scenario should be a column
					for(String scenario: data.getSet().getDetectionMap().get(parameter).keySet()) {
						// Each node should be a row
						for(int node: data.getSet().getDetectionMap().get(parameter).get(scenario).keySet()) {
							int i = cloudNodes.indexOf(node);
							float ttd = data.getSet().getDetectionMap().get(parameter).get(scenario).get(node);
							ttds[i][scenarios.indexOf(scenario)] = ttd;
							if(ttd < min[i]) min[i] = ttd;
							if(ttd > max[i]) max[i] = ttd;
							avg[i] += ttd; //sum now, average later
							count[i]++;
						}
					}
					// Writing out the header row
					StringBuilder text = new StringBuilder();
					text.append("XYZ,Detecting Scenarios,Min TTD,Avg TTD,Max TTD");
					for(String scenario: data.getSet().getScenarios())
						text.append("," + scenario);
					text.append("\n");
					// Writing out a row for each node
					for(int node: cloudNodes) {
						int i = cloudNodes.indexOf(node);
						Point3f xyz = structure.getXYZCenterfromNodeNumber(node);
						text.append(xyz+","+count[i]/(float)scenarios.size()*100.0+"%,");
						text.append((min[i]==Float.MAX_VALUE ? "" : min[i]) + ",");
						text.append((avg[i]==0 ? "" : avg[i]/count[i]) + ",");
						text.append((max[i]==Float.MIN_VALUE ? "" : max[i]));
						for(int j=0; j<scenarios.size(); j++)
							text.append("," + (ttds[i][j]==0 ? "" : ttds[i][j]));
						text.append("\n");
					}
					// Write out a file for each parameter
					try {
						File outFolder = new File(outputFolder.getText());
						if(!outFolder.exists())
							outFolder.mkdirs();
						File csvOutput = new File(new File(outputFolder.getText()), "DetectionMap_"+parameter+".csv");
						if(!csvOutput.exists())
							csvOutput.createNewFile();
						FileUtils.writeStringToFile(csvOutput, text.toString());
						//Desktop.getDesktop().open(csvOutput);
					} catch (IOException e) {		
						JOptionPane.showMessageDialog(null, "Could not write out the detection map, make sure the file is not currently open");
						e.printStackTrace();
					}
				}
				long time = (System.currentTimeMillis() - startTime) / 1000;
				MessageBox dialog = new MessageBox(container.getShell(), SWT.OK);
				dialog.setText("Finished writing the Detection Map");
				dialog.setMessage("DREAM just finished writing the detection map in "+time/60+" minutes. Results can be found at: " + outputFolder.getText());
				dialog.open();
			}
		});
		detectionMapButton.setVisible(Constants.buildDev);
		
		container.layout();	
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		
		DREAMWizard.nextButton.setVisible(false);
		DREAMWizard.visLauncher.setEnabled(true);
		DREAMWizard.convertDataButton.setEnabled(false);
	}
	
	private List<int[]> getAllCombinations(final int n, final int k){
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
	
	public void printSolutionSpace(){
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
						String var = ((data.getSet().getSensorSettings().get(type).getValidNodes().contains(nodeNumber)) ? "1" : "0");
						text.append(" " + var);
					}
				}
			}
		}
		text.append("\n");
		String ensemble = data.getSet().getScenarioEnsemble();
		
		try{
			File outFolder = new File(outputFolder.getText());
			if(!outFolder.exists())
				outFolder.mkdirs();
			File outFile = new File(new File(outputFolder.getText()), ensemble+"_solutionSpace.txt");
			if(!outFile.exists())
				outFile.createNewFile();
			FileUtils.writeStringToFile(outFile, text.toString());
		} catch (IOException e) {		
			JOptionPane.showMessageDialog(null, "Could not write to "+ensemble+"_solutionSpace.txt, make sure the file is not currently open");
			e.printStackTrace();
		}
	}

	public void convertFile(final File file) throws IOException {

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
						Point3f xyz = data.getSet().getNodeStructure().getXYZCenterfromNodeNumber(nodeNumber);
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
	public void setPageCurrent(final boolean current) {
		isCurrentPage = current;
	}

}
