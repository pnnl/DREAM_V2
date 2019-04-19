package wizardPages;

import hdf5Tool.FileConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import objects.E4DSensors;
import objects.ExtendedConfiguration;
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
import hdf5Tool.IAMInterface;
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
		ImageData imgData = new ImageData(getClass().getResourceAsStream("/dream.png")); 
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
		
		WizardDialog.setDefaultImage(new Image(Display.getDefault(), DREAMWizard.class.getResourceAsStream("/icon.png")));
		
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
						FileConverter browser = new FileConverter();
						browser.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
						browser.setVisible(true); 
					} 

					@Override 
					public void widgetDefaultSelected(SelectionEvent e) { 
						FileConverter browser = new FileConverter();
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
						// Since valid nodes can change if the user selects a different detection criteria, we need to force the reset every time
						wizard.launchVisWindow(true, true);
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
		return data.getSet();
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
		public String fileType;
		
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
		
		
		public void setupScenarioSet(final ModelOption modelOption, final MUTATE mutate, final String function, final String input) {	
			try {
				dialog.run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							// Create a list of files in the directory, excluding any that aren't .h5 or .iam files
							File inputFolder = new File(input); // We are already assured that the directory exists and contains .h5 or .iam files from previous constraints
							FilenameFilter fileNameFilter = new FilenameFilter() {
								@Override
								public boolean accept(File dir, String name) {
									if(name.endsWith(".h5") || name.endsWith(".iam"))
										return true;
									return false;
								}
							};
							File[] list = inputFolder.listFiles(fileNameFilter);
							
							// If the directory has H5 files, handle it this way...
							if(list[0].getPath().endsWith(".h5")) {
								monitor.beginTask("Loading H5 scenario set", 10);
								
								monitor.subTask("reading Node Structure from first file");
								set.setNodeStructure(HDF5Interface.readNodeStructureH5(list[0])); //Load the node structure from the first H5 file
								monitor.worked(6);
								
								monitor.subTask("reading scenarios from all files");
								set.setupScenarios(HDF5Interface.queryScenarioNamesFromFiles(list)); // Set the scenarios
								monitor.worked(3);
								
								monitor.subTask("initializing algorithm");
								STORMData.this.mutate = mutate;	//Mutate option should always be sensor... used to have other options
								STORMData.this.modelOption = modelOption; //Save the modelOption
								if(function.endsWith("SimulatedAnnealing"))
									runner = new SimulatedAnnealing(mutate); //Set the function (this will always be Simulated Annealing in this release)
								STORMData.this.fileType = "hdf5";
								monitor.worked(1);
								
								monitor.subTask("done");
							}
							
							// If the directory has IAM files, handle it this way...
							else if(list[0].getPath().endsWith(".iam")) {
								monitor.beginTask("Loading IAM scenario set", list.length + 2);
								
								monitor.subTask("reading Node Structure from first file");
								File file = new File(inputFolder.getPath(),"iam.grid");
								if(file.exists())
									set.setNodeStructure(IAMInterface.readNodeStructureIAM(file)); //Load the node structure from the iam.grid file
								else
									set.setNodeStructure(IAMInterface.readNodeStructureIAM_Uncompressed(list[0])); //Load the node structure from the first IAM file
								monitor.worked(1);
								
								monitor.subTask("reading scenarios from all files");
								IAMInterface.readIAMFiles(monitor, list, set);
								set.getNodeStructure().setDataTypes(IAMInterface.getDataTypes()); // Set the data types
								set.setupScenarios(IAMInterface.getScenarios()); // Set the scenarios
								monitor.worked(list.length);
								
								monitor.subTask("initializing algorithm");
								STORMData.this.mutate = mutate;	// Mutate option should always be sensor... used to have other options
								STORMData.this.modelOption = modelOption;
								if(function.endsWith("SimulatedAnnealing"))
									runner = new SimulatedAnnealing(mutate); // Set the function (this will always be CCS9_1 in this release)
								STORMData.this.fileType = "iam";
								monitor.worked(1);
								
								monitor.subTask("done");
							}
							if(monitor.isCanceled()) {
								set.getSensorSettings().clear();
								set.getDetectionMap().clear();
								set.getNodeStructure().clear();
							}
						} catch (Exception e) {
							System.out.println("Was the monitor cancelled?\t" + monitor.isCanceled());
						}
					}					
				});
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Loading files was canceled by the user.");
				e.printStackTrace();
			}
		}
		
		
		public void setupSensors(final ArrayList<SensorData> newSensors, final ArrayList<SensorData> activeSensors) {
			try {
				dialog.run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							int monitorSize = 900*newSensors.size() + 50*activeSensors.size();
							monitor.beginTask("Sensor settings", monitorSize);
							
							// First we generate a TTD matrix based on new selected sensors settings
							// Only do this for H5 variables, IAM is already in detectionMap
							for(SensorData sensor: newSensors) {
								if(monitor.isCanceled()) break;
								if(sensor.sensorType.contains("allSensors"))
									set.detectionMapForAllSensors(monitor, activeSensors); //Special handling - map should be lowest detection at each node
								else
									HDF5Interface.createDetectionMap(monitor, set, set.getSensorSettings(sensor.sensorType), sensor.specificType);
							}
							
							// Last we create a list of valid nodes from the new detectionMap
							for(SensorData sensor: activeSensors) {
								if(monitor.isCanceled()) break;
								monitor.subTask("calculating valid nodes: " + sensor.sensorType);
								monitor.subTask(sensor.sensorType + " - generating a list of valid nodes");
								set.getSensorSettings(sensor.sensorType).setNodes(set);
								monitor.worked(50);
							}
							
							// If the user canceled, clear any added data
							if(monitor.isCanceled()) {
								for(String scenario: set.getAllScenarios()) {
									for(SensorData sensor: newSensors) {
										set.getDetectionMap().get(scenario.toString()).remove(sensor.specificType);
										set.getSensorSettings(sensor.sensorType).clearNodes();
									}
								}
							}
							
						} catch (Exception e) {
							System.out.println("Was the monitor cancelled?\t" + monitor.isCanceled());
						}
					}
				});
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Dream is out of memory!  Please reduce your solution space.");
				e.printStackTrace();
			}
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

		public float runObjective(ExtendedConfiguration configuration) {
			return runner.objective(configuration, set, Constants.runThreaded);
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
					monitor.beginTask("Calculating E4D Wells for " + selectedParameter, 100000);
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
					for(String scenario: set.getScenarios()) {
						if(monitor.isCanceled()) return;
						monitor.subTask("Looping through the selected scenarios: " + scenario);
						
						// Before we run, do a quick check that the timesteps align between the storage and leakage files
						String storage = e4dDialog.getStorage(); //Storage File Location
						String leakage = Constants.homeDirectory + File.separator + scenario.toString() + ".h5"; //Leakage File Location
						if(!HDF5Interface.checkTimeSync(monitor, storage, leakage, data.getSet().getNodeStructure().getTimeSteps().size())) {
							System.out.println("Error: The time steps don't match between the storage and leakage files.");
							continue;
						}
						
						// Run the Python script with the following input arguments
						try {
							File e4dScript = new File(Constants.userDir, "e4d" + File.separator + "run_dream2e4d_windows.py");
							String input1 = storage;
							String input2 = leakage;
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
						monitor.worked(1000 / set.getScenarios().size() - 10);
						monitor.subTask("Writing the scenario results: " + scenario);
						// Read the result matrix from each scenario into a master file
						File detectionMatrix = new File(Constants.userDir, "e4d" + File.separator + "detection_matrix.csv");
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
						
						// Write out the current cumulative detection matrix file at each iteration
						File fullDetectionMatrix = new File(Constants.userDir, "e4d/ertResultMatrix_" + set.getScenarioEnsemble() + "_" + set.getScenarios().size() +
								"_" + input7 + ".csv");
						try {
							fullDetectionMatrix.createNewFile();
							FileUtils.writeStringToFile(fullDetectionMatrix, text.toString());
						} catch (IOException e) {
							JOptionPane.showMessageDialog(null, "Could not write to " + fullDetectionMatrix.getName() + ", make sure the file is not currently open");
							e.printStackTrace();
						}
						
						// Delete the extra e4d files that we don't want...
						// TODO: Much better option is for Jeff to not write these files at all...
						File file = new File(Constants.userDir, "e4d/e4d_mod1_log.txt");
						file.delete();
						file = new File(Constants.userDir, "e4d/e4d.log");
						file.delete();
						file = new File(Constants.userDir, "e4d/run_time.txt");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.dpd");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.sig.srv");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh_baseline.h5");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh_w_leak.h5");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.sig");
						file.delete();
						file = new File(Constants.userDir, "e4d/e4d.inp");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.out");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.srv");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.1.node");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.1.edge");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.1.ele");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.1.face");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.1.neigh");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.poly.1_orig.node");
						file.delete();
						file = new File(Constants.userDir, "e4d/mesh_build.log");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.1.edge");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.1.ele");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.1.ele.old");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.1.neigh");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.1.node");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.1.poly");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.poly");
						file.delete();
						file = new File(Constants.userDir, "e4d/surface.sig");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.cfg");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.poly");
						file.delete();
						file = new File(Constants.userDir, "e4d/test_mesh.trn");
						file.delete();
						System.out.println("Successfully deleted several e4d files (i.e. " + file + ")");
					}
				}
			});
		}
		
	}
} 