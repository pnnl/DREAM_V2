package wizardPages;

import hdf5Tool.FileBrowser;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import objects.E4DSensors;
import objects.ExtendedConfiguration;
import objects.Scenario;
import objects.ScenarioSet;
import objects.SensorSetting;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import hdf5Tool.HDF5Interface;
import utilities.Constants;
import utilities.E4DRunDialog;
import utilities.Point3i;
import utilities.Constants.ModelOption;
import visualization.DomainVisualization;
import wizardPages.Page_LeakageCriteria.SensorData;
import functions.SimulatedAnnealing;
import functions.Function;
import functions.MutationFunction.MUTATE;

/**
 * Parent class of UI, contains information about which pages to add and in what order
 * Also contains main function for launching DREAM
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class DREAMWizard extends Wizard {

	private STORMData data;
	private DomainVisualization domainViewer;
	private WizardDialog dialog;

	public static Label errorMessage;
	public static Button convertDataButton;
	public static Button visLauncher;
	public static Button nextButton;
	
	boolean wasCancelled;

	public DREAMWizard() {
		super();
		setWindowTitle(null);
		setWindowTitle("DREAM Wizard");
		setNeedsProgressMonitor(true);	
		ImageData imgData = new ImageData("./img/dream.png"); 
		this.setDefaultPageImageDescriptor(ImageDescriptor.createFromImageData(imgData));
		this.setTitleBarColor(new RGB(255,255,255));
		data = new STORMData(this);
	}

	public void setDialog(WizardDialog dialog) {
		this.dialog = dialog;		
	}

	public void createViewer(Boolean show) {
		closeViewer(); // Close the old viewer
		try {
			// Create a new viewer, this crashes sometimes when it is first called
			this.domainViewer = new DomainVisualization(Display.getCurrent(), getScenarioSet(), show);			
		} catch (Exception e) {
			try {
				Thread.sleep(1000);
				this.domainViewer = new DomainVisualization(Display.getCurrent(), getScenarioSet(), show);	
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public boolean viewerExists() {
		return this.domainViewer != null;
	}
	
	public void closeViewer() {
		if(this.domainViewer != null) {
			this.domainViewer.dispose();
			this.domainViewer = null;
		}
	}
	
	public void hideViewer() {
		if(this.domainViewer != null)
			this.domainViewer.hide();
	}
	
	public void showViewer() {
		if(this.domainViewer != null) 
			this.domainViewer.show();
	}
	
	@Override
	public IWizardPage getNextPage(IWizardPage current) {

		AbstractWizardPage currentPage = ((AbstractWizardPage)current);
		IWizardPage next = super.getNextPage(current);	

		// If we haven't loaded this page yet, load it
		if(!currentPage.isPageCurrent()) {
			// Float check that the next page isn't already loaded
			if(next != null && ((AbstractWizardPage)next).isPageCurrent()) 
				((AbstractWizardPage)next).setPageCurrent(false);

			System.out.println("LOAD: " + currentPage);
			currentPage.loadPage();	
			return next;
		}

		// Otherwise finalize this page
		System.out.println("COMPLETE " + currentPage);
		try {
			currentPage.completePage();
			return next;
		} catch (Exception e) {
			currentPage.loadPage();
			System.out.println("Something went wrong, stay on this page.");
			return current;
		}
	}

	@Override
	public void addPages() {

		addPage(new Page_Welcome());	
		addPage(new Page_InputDirectory(data));	
		addPage(new Page_ScenarioWeighting(data));
		addPage(new Page_LeakageCriteria(data));
		addPage(new Page_DetectionCriteria(data));
		addPage(new Page_ConfigurationSettings(data));
		addPage(new Page_ExcludeLocations(data));
		addPage(new Page_RunDREAM(data));
	}

	@Override
	public boolean performFinish() {
		return true;
	}


	public static void main(String[] args) {

		try {
			//			UIManager.setLookAndFeel(
			//					UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		} 

		final Display display = Display.getDefault();
		final Shell shell = new Shell(display);

		// Pop up the disclaimer, exit on cancel
		MessageBox messageBox = new MessageBox(shell, SWT.OK | SWT.CANCEL );
		messageBox.setMessage("The Software was produced by Battelle under Contract No. DE-AC05-76RL01830 with the Department of Energy.  The U.S. Government is granted for itself and others acting on its behalf a nonexclusive, paid-up, irrevocable worldwide license in this data to reproduce, prepare derivative works, distribute copies to the public, perform publicly and display publicly, and to permit others to do so.  The specific term of the license can be identified by inquiry made to Battelle or DOE."
				+ "\n\nNeither the United States Government, nor any of their employees, makes any warranty, express or implied, or assumes any legal liability or responsibility for the accuracy, completeness, or usefulness of any information, apparatus, product, or process disclosed, or represents that its use would not infringe privately owned rights. Reference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer, or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United States Government or any agency thereof. The views and opinions of authors expressed herein do not necessarily state or reflect those of the United States Government or any agency thereof.");
		messageBox.setText("NOTICE TO USERS");
		int response = messageBox.open();
		if (response == SWT.CANCEL)
			System.exit(0); // Exit if they don't accept

		final DREAMWizard wizard = new DREAMWizard();

		WizardDialog.setDefaultImage(new Image(Display.getDefault(),"./img/icon.png"));

		WizardDialog wizardDialog = new WizardDialog(null, wizard) {
			{
				setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE | SWT.MAX | SWT.MIN | SWT.ICON);
			}

			@Override
			protected void finishPressed(){
				//On a mac, this was being "pressed" when enter was hit. This way it does nothing and does not exit.
			}

			
			@Override
			protected void createButtonsForButtonBar(Composite parent) {
				
				GridData errorMessageData = new GridData(GridData.HORIZONTAL_ALIGN_END);
				errorMessageData.horizontalSpan = 4;
				errorMessageData.grabExcessHorizontalSpace = true;
				errorMessageData.horizontalAlignment = GridData.FILL;
				errorMessage = new Label(parent, SWT.RIGHT);
				errorMessage.setForeground(display.getSystemColor(SWT.COLOR_RED));
				errorMessage.setLayoutData(errorMessageData);
				
				convertDataButton = new Button(parent, SWT.PUSH); 	
				convertDataButton.setText("Launch Converter");
				convertDataButton.setToolTipText("Convert simulation data to DREAM h5 format"); 	
				convertDataButton.addSelectionListener(new SelectionListener() 
				{ 
					@Override 
					public void widgetSelected(SelectionEvent e) { 
						FileBrowser browser = new FileBrowser();
						browser.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
						browser.setVisible(true); 
					} 

					@Override 
					public void widgetDefaultSelected(SelectionEvent e) { 
						FileBrowser browser = new FileBrowser();
						browser.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
						browser.setVisible(true); 
					} 
				}); 

				visLauncher = new Button(parent, SWT.PUSH); 	
				visLauncher.setText("Launch Visualization"); 	
				visLauncher.addSelectionListener(new SelectionListener() 
				{ 
					@Override 
					public void widgetSelected(SelectionEvent e) { 
						wizard.launchVisWindow(false, true);
					} 

					@Override 
					public void widgetDefaultSelected(SelectionEvent e) { 
						widgetSelected(e); 
					} 
				}); 


				super.createButtonsForButtonBar(parent);
				nextButton = super.getButton(IDialogConstants.NEXT_ID);

				// Hide the buttons we don't use
				Button cancel = super.getButton(IDialogConstants.CANCEL_ID);	
				Button finish = super.getButton(IDialogConstants.FINISH_ID);
				((GridData)cancel.getLayoutData()).exclude = true;
				((GridData)finish.getLayoutData()).exclude = true;
			}
		};

		wizard.setDialog(wizardDialog);
		wizardDialog.setTitleAreaColor(new RGB(255, 255, 255));//32,62,72));
		wizardDialog.open();
	}

	public void launchVisWindow(boolean reset, boolean show) {
		// If we don't want to reset the vis window and 
		if(!reset && viewerExists())
		{
			showViewer();
			return;
		}
		// Otherwise create a new one, this will close the old one of there was one open previously
		createViewer(show);

	}
	
	public ScenarioSet getScenarioSet() {
		return data.getScenarioSet();
	}

	public class STORMData {
		//booleans that help us keep track of when we need to reset stuff
		public boolean needToResetWells = true;
		public boolean needToResetMonitoringParameters = true;

		private ScenarioSet set;
		private Function runner;
		private MUTATE mutate;
		private ExtendedConfiguration initialConfiguration;
		private DREAMWizard wizard;
		private ArrayList<Point3i> wells;
		
		public Constants.ModelOption modelOption;

		public STORMData(DREAMWizard wizard) {
			this.wizard = wizard;
			set = new ScenarioSet();
			initialConfiguration = new ExtendedConfiguration();
			mutate = MUTATE.SENSOR;
		}
		
		public void reset() {
			//Reset this data
			set = new ScenarioSet();
			initialConfiguration = new ExtendedConfiguration();
			mutate = MUTATE.SENSOR;
			needToResetWells = true;
			needToResetMonitoringParameters = true;
		}

		public ScenarioSet getScenarioSet() {
			return set;
		}

		public void setupScenarioSet(final ModelOption modelOption, final MUTATE mutate, final String function, final String hdf5) throws Exception {	
			dialog.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

					// Run tasks:
					monitor.beginTask("Loading scenario set", 10);

					monitor.subTask("reading hdf5 files");

					if(!hdf5.isEmpty()) 
						HDF5Interface.loadHdf5Files(hdf5);	// Load the hdf5 files into the constants
					monitor.worked(5);

					monitor.subTask("clearing previous data");				
					set.clearRun();	// Clear any old run data
					monitor.worked(1);

					monitor.subTask("loading new data");
					set.loadRunData(""); // Load the new data (this will be hdf5)
					monitor.worked(1);

					monitor.subTask("applying user settings");
					STORMData.this.mutate = mutate;	// Set the mutate option
					STORMData.this.modelOption = modelOption;

					monitor.worked(1);

					monitor.subTask("initializing algorithm");
					if(function.endsWith("SimulatedAnnealing"))
						runner = new SimulatedAnnealing(mutate); // Set the function (this will always be CCS9_1 in this release)	
					monitor.worked(1);

					monitor.subTask("done");					
					monitor.worked(1);					
				}					
			});
		}
		
		
		public void setupSensors(final boolean reset, final Map<String, SensorData> sensorData, final int count) throws Exception {
			try {
				dialog.run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							if(sensorData.containsKey("Electrical Conductivity"))
								monitor.beginTask("Sensor settings", 1000*(count-1)+301); //e4d sensors are 300 each
							else
								monitor.beginTask("Sensor settings", 1000*count+1); //normal sensors are 1000 each
							if(HDF5Interface.paretoMap.size()>15) //if the user tries many different leakage thresholds, we don't want this map getting too big
								HDF5Interface.paretoMap.clear();
							for(String sensorType: sensorData.keySet()) {
								if(monitor.isCanceled()) break;
								SensorData data = sensorData.get(sensorType);
								if(data.isIncluded) {
									monitor.subTask(sensorType.toLowerCase() + " - saving data " + sensorType);
									if(!set.getSensorSettings().containsKey(sensorType)) { // Reset it, user  must have re selected it?
										set.resetSensorSettings(sensorType, data.lowerThreshold, data.upperThreshold);
									}
									set.getSensorSettings().get(sensorType).setUserSettings(data.cost, Color.BLUE, data.lowerThreshold, data.upperThreshold,
											data.trigger, reset, data.deltaType, data.maxZ, data.minZ);
									if(!set.getSensorSettings().get(sensorType).areNodesReady()) {
										monitor.subTask(sensorType.toLowerCase() + " - searching for valid nodes");
										set.getSensorSettings(sensorType).getValidNodes(monitor); // This should re-query for the valid nodes
										// Clear the vis window
										DREAMWizard.this.closeViewer();
									}
								} else {
									monitor.subTask(sensorType.toLowerCase() + " - removing");
									set.removeSensorSettings(sensorType);
									set.getInferenceTest().setMinimumForType(sensorType, 0);
								}
							}
							if(modelOption == ModelOption.ALL_SENSORS){
								HashSet<Integer> nodes = new HashSet<Integer>();
								float cost = 0;
								for(String setting: set.getSensorSettings().keySet()){
									if(setting.equals("all")) continue;
									nodes.addAll(set.getSensorSettings().get(setting).getCloudNodes(monitor));
									cost += set.getSensorSettings().get(setting).getCost();
								}
								set.addSensorSetting("all", "all");
								set.getSensorSettings().get("all").setCost(cost);
								set.getSensorSettings().get("all").setFullCloudNodes(nodes);
								set.getSensorSettings().get("all").setValidNodes(SensorSetting.paretoOptimalAll(set, nodes, set.getAllScenarios(), set.getNodeStructure(), set.getSensorSettings()));
							}

							// If the user canceled, should we clear the data????
							if(monitor.isCanceled()) {
								for(String sensorType: sensorData.keySet()) {							
									SensorData data = sensorData.get(sensorType);
									if(data.isIncluded) {
										if(!set.getSensorSettings().containsKey(sensorType)) { // Reset it, user  must have re selected it?
											set.resetSensorSettings(sensorType, data.lowerThreshold, data.upperThreshold);
											set.getSensorSettings().get(sensorType).setUserSettings(data.cost, Color.BLUE, data.lowerThreshold, data.upperThreshold,
													data.trigger, reset, data.deltaType, data.maxZ, data.minZ);
										}
										set.getSensorSettings().get(sensorType).setNodesReady(false);
									} else {
										set.removeSensorSettings(sensorType);
										set.getInferenceTest().setMinimumForType(sensorType, 0);							
									}
								}
							}
							monitor.worked(1);
						} catch (Exception e) {
							System.out.println("Was the monitor cancelled?\t" + monitor.isCanceled());
							e.printStackTrace();
						}

					}
				});
			} catch (Exception e) {
				float totalNodes = 0;
				for(String sc : HDF5Interface.hdf5CloudData.keySet()) {
					for(float ts: HDF5Interface.hdf5CloudData.get(sc).keySet()) {
						for(String dt: HDF5Interface.hdf5CloudData.get(sc).get(ts).keySet()) {
							totalNodes += HDF5Interface.hdf5CloudData.get(sc).get(ts).get(dt).keySet().size();
							if(HDF5Interface.hdf5CloudData.get(sc).get(ts).get(dt).keySet().size() > 1000)
								System.out.println(sc + ", " + ts + ", " + dt + " , " + HDF5Interface.hdf5CloudData.get(sc).get(ts).get(dt).keySet().size());
						}
					}
				}
				System.out.print("Cloud currently has: ");
				System.out.println(totalNodes);
				JOptionPane.showMessageDialog(null, "Dream is out of memory!  Please reduce your solution space, current space: " + totalNodes);
				e.printStackTrace();
			}
		}

		public void setupInferenceTest(final Map<String, Integer> requiredSensors, final int totalMinimum) throws Exception {
			dialog.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Inference test ", requiredSensors.size());	
					for(String sensorType: requiredSensors.keySet()) {
						monitor.subTask("setting sensors required for " + sensorType.toLowerCase());
						set.getInferenceTest().setMinimumForType(sensorType, requiredSensors.get(sensorType));
						monitor.worked(1);
					}
					set.getInferenceTest().setOverallMinimum(totalMinimum);
				}
			});			
		}

		public ScenarioSet getSet() {
			return set;
		}
		
		public SensorSetting getSensorSettings(String scenario) {
			return set.getSensorSettings(scenario);
		}

		public MUTATE getMutate() {
			return mutate;
		}

		public ExtendedConfiguration getInitialConfiguration() {
			return initialConfiguration;
		}

		//Returns whether or not the run was cancelled for future use
		public boolean run(final int runs, final boolean showPlots) throws Exception {
			// Resets the vis window
			wizard.launchVisWindow(true, showPlots);
			runner.setDomainViewer(wizard.domainViewer);
			dialog.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Running iterative procedure ", set.getIterations()*runs);	
					runner.setMonitor(monitor);
					if(runs > 1) {
						wasCancelled = runner.run(data.modelOption, initialConfiguration, set, showPlots, runs);	
					} else {
						wasCancelled = runner.run(data.modelOption, initialConfiguration, set, showPlots);	
					}					
				}
			});
			System.gc();
			return wasCancelled;
		}

		public float runObjective(ExtendedConfiguration configuration, boolean runThreaded) {
			return runner.objective(configuration, set, runThreaded);
		}

		public void randomEnumeration(final int max) throws Exception {		
			// Resets the vis window	
			wizard.launchVisWindow(true, true);
			runner.setDomainViewer(wizard.domainViewer);
			dialog.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Running random enumeration ", max);	
					runner.setMonitor(monitor);
					runner.fullEnumeration(initialConfiguration, set, max);
				}
			});
		}

		public void runEnumeration() throws Exception {
			// Resets the vis window
			wizard.launchVisWindow(true, true);
			runner.setDomainViewer(wizard.domainViewer);
			dialog.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Running full enumeration ", 1000); // Not sure who much work this will end up being	
					runner.setMonitor(monitor);
					runner.fullEnumeration(initialConfiguration, set);	
				}
			});
		}

		public void setWorkingDirectory(String dir) {
			runner.setResultsDirectory(dir);
		}
		
		public ArrayList<Point3i> runWellOptimizationE4D(final String selectedParameter, final int maximumWells) throws Exception {
			dialog.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Calculating E4D Wells for " + selectedParameter, 1000);
					try {
						wells = E4DSensors.calculateE4DWells(data, selectedParameter, maximumWells, monitor);
					} catch (Exception e) {
						wells = null;
						e.printStackTrace();
					}
				}

			});
			return wells;
		}
		
		public void runE4DWindows(final E4DRunDialog e4dDialog, final File e4dWellList) throws Exception {
			dialog.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Running E4D.", 1000);
					StringBuilder text = new StringBuilder();
					String input7 = ""; //We want to add the detection threshold to the file name
					
					// Loop through all the scenarios - E4D needs to run once for each scenario
					for(Scenario scenario: data.getSet().getScenarios()) {
						if(monitor.isCanceled()) return;
						monitor.subTask("Looping through the selected scenarios: " + scenario.getScenario());
						
						// Run the Python script with the following input arguments
						try {
							File e4dScript = new File(Constants.userDir, "e4d/run_dream2e4d_windows.py");
							String input1 = e4dDialog.getStorage(); //Storage File Location
							String input2 = Constants.homeDirectory + "\\" + scenario.toString() + ".h5"; //Leakage File Location
							String input3 = e4dWellList.getPath(); //Well List Location
							String input4 = e4dDialog.getBrineSaturation(); //Brine Saturation Mapping
							String input5 = e4dDialog.getGasSaturation(); //Gas Saturation Mapping
							String input6 = e4dDialog.getSaltConcentration(); //Salt Concentration Mapping
							input7 = String.valueOf(e4dDialog.getDetectionThreshold()); //Detection Threshold
							String command = "python \"" +e4dScript.getAbsolutePath()+ "\" \"" +input1+ "\" \"" +input2+ "\" \"" +input3+ "\" \"" +input4+ "\" \"" +input5+ "\" \"" +input6+ "\" \"" +input7+ "\"";
							File wDirectory = new File(Constants.userDir,"e4d");
							
							Process p = Runtime.getRuntime().exec(command, null, wDirectory);
							
							//Read all the Python outputs to console
							BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
							BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
							String s = null;
							System.out.println("This is the standard output from the E4D code for " + scenario.toString() + ":");
							while((s = stdInput.readLine()) != null)
								System.out.println(s);
							System.out.println("This is the error output from the E4D code for " + scenario.toString() + ":");
							while((s = stdError.readLine()) != null)
								System.out.println(s);
						} catch(Exception e) {
							System.out.println(e);
							System.out.println("Install python3 and required libraries to run E4D");
						}
						monitor.worked(1000 / data.getSet().getScenarios().size() - 10);
						monitor.subTask("Writing the scenario results: " + scenario.getScenario());
						// Read the result matrix from each scenario into a master file
						File detectionMatrix = new File(Constants.userDir, "e4d/detection_matrix.csv");
						String line = "";
						int lineNum = 0;
						try (BufferedReader br = new BufferedReader(new FileReader(detectionMatrix))) {
							// Read each line, comma delimited
							while ((line = br.readLine()) != null) {
								if(lineNum==0) {
									String[] lineList = line.split(",");
									lineList[0] = scenario.toString();
									line = String.join(",", lineList);
								}
								text.append(line + "\n");
								lineNum++;
							}
							text.append("\n");
						} catch(Exception e) {
							e.printStackTrace();
						}
						monitor.worked(10);
					}
					File fullDetectionMatrix = new File(Constants.userDir, "e4d/ertResultMatrix_" + data.getSet().getScenarioEnsemble() + "_" + data.getSet().getScenarios().size() +
							"_" + input7 + ".csv");
					try {
						fullDetectionMatrix.createNewFile();
						FileUtils.writeStringToFile(fullDetectionMatrix, text.toString());
					} catch (IOException e) {
						JOptionPane.showMessageDialog(null, "Could not write to " + fullDetectionMatrix.getName() + ", make sure the file is not currently open");
						e.printStackTrace();
					}
				}
			});
		}
		
	}
} 