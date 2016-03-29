package wizardPages;

import hdf5Tool.FileBrowser;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import objects.ExtendedConfiguration;
import objects.Scenario;
import objects.ScenarioSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.DialogTray;
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
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import results.ResultPrinter;
import utilities.Constants;
import utilities.Point3d;
import utilities.Constants.ModelOption;
import visualization.MultiDomainViewer;
import wizardPages.Page_SensorSetup.SensorData;
import functions.CCS9_1;
import functions.Function;
import functions.MutationFunction.MUTATE;

public class DREAMWizard extends Wizard {

	private STORMData data;
	private MultiDomainViewer domainViewer;
	private WizardDialog dialog;

	public static Button convertDataButton;
	public static Button visLauncher;
	
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

	public void linkViewer(MultiDomainViewer domainViewer) {
		this.domainViewer = domainViewer;
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
			return current;	// Something went wrong stay on this page
		}
	}

	@Override
	public void addPages() {

		addPage(new Page_WelcomeScreen());	
		addPage(new Page_ScenarioSet(data));	
		addPage(new Page_ScenarioSetup(data));
		addPage(new Page_SensorSetup(data));
		addPage(new Page_InferenceTest(data));
		addPage(new Page_RunSettings(data));
		addPage(new Page_ExcludeWells(data));
		addPage(new Page_ReviewAndRun(data));
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
						wizard.launchVisWindow();
					} 

					@Override 
					public void widgetDefaultSelected(SelectionEvent e) { 
						widgetSelected(e); 
					} 
				}); 


				super.createButtonsForButtonBar(parent);

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

	public void launchVisWindow() {
		System.out.println("Main Shell handling Button press, about to create child Shell");
		try {
		MultiDomainViewer domainViewer = new MultiDomainViewer(Display.getCurrent(), getScenarioSet()); 
		linkViewer(domainViewer);
		} catch (Exception e) {
			try {
				Thread.sleep(1000);

				MultiDomainViewer domainViewer = new MultiDomainViewer(Display.getCurrent(), getScenarioSet()); 
				linkViewer(domainViewer);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		}
	}

	public ScenarioSet getScenarioSet() {
		return data.getScenarioSet();
	}

	public class STORMData {

		private ScenarioSet set;
		private Function runner;
		private MUTATE mutate;
		private ExtendedConfiguration initialConfiguration;
		private DREAMWizard wizard;

		private Constants.ModelOption modelOption;

		public STORMData(DREAMWizard wizard) {
			this.wizard = wizard;
			set = new ScenarioSet();
			initialConfiguration = new ExtendedConfiguration();
			mutate = MUTATE.SENSOR;
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
						Constants.loadHdf5Files(hdf5);	// Load the hdf5 files into the constants
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
					if(function.endsWith("CCS9.1"))
						runner = new CCS9_1(mutate); // Set the function (this will always be CCS9_1 in this release)	
					monitor.worked(1);

					monitor.subTask("done");					
					monitor.worked(1);					
				}					
			});
		}

		public void setupScenarios(final Map<Scenario, Float> scenarioWeights, final List<Scenario> scenariosToRemove) throws Exception {
			dialog.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					// Run tasks:
					monitor.beginTask("Scenario set settings", scenarioWeights.size() + scenariosToRemove.size());

					monitor.subTask("applying scenario weights");
					set.setScenarioWeights(scenarioWeights);
					monitor.worked(scenarioWeights.size());

					for(Scenario scenario: scenariosToRemove) {
						monitor.subTask("removing unused scenario: " + scenario);
						set.removeScenario(scenario);
						monitor.worked(1);
					}
				}
			});		
		}

		/*
		 * if(data.isIncluded) {
						settings.add(data.cost);
						settings.add(data.trigger == Trigger.THRESHOLD);
						settings.add(data.asPercent);
						settings.add(data.min);
						settings.add(data.max);
						settings.add(data.trigger == Trigger.DELTA);
						settings.add(-1);
						settings.add(data.delta);
					} 
		 */
		public void setupSensors(final boolean reset, final Map<String, SensorData> sensorSettings) throws Exception {
			try {
				dialog.run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							monitor.beginTask("Sensor settings", sensorSettings.size()*2);	
							for(String sensorType: sensorSettings.keySet()) {							
								if(monitor.isCanceled()) break;							
								SensorData data = sensorSettings.get(sensorType);
								if(data.isIncluded) {
									monitor.subTask(sensorType.toLowerCase() + " - saving data " + sensorType);
									if(!set.getSensorSettings().containsKey(sensorType)) { // Reset it, user  must have re selected it?
										set.resetSensorSettings(sensorType, data.min, data.max);
									}
									set.getSensorSettings().get(sensorType).setUserSettings(
											data.cost, 
											Color.BLUE,
											data.min,
											data.max,
											data.trigger, 
											reset,
											data.deltaType,
											data.maxZ,
											data.minZ);
									monitor.worked(1);
									if(!set.getSensorSettings().get(sensorType).areNodesReady()) {
										monitor.subTask(sensorType.toLowerCase() + " - searching for valid nodes");
										set.getSensorSettings(sensorType).getValidNodes(monitor); // This should re-query for the valid nodes
									}
									monitor.worked(1);
								} else {
									monitor.subTask(sensorType.toLowerCase() + " - removing");
									set.removeSensorSettings(sensorType);
									set.getInferenceTest().setMinimumRequiredForType(sensorType, 0);
									monitor.worked(2);
								}
								//TODO: remove this hard-coded z check

								float minZ = data.minZ;
								float maxZ = data.maxZ;
								//Find the nodes that fit this z restriction
								HashSet<Integer> temp = new HashSet<Integer>();
								if(set.getSensorSettings(sensorType) != null){
									for(Integer node: set.getSensorSettings(sensorType).getValidNodes(monitor)){
										if(monitor.isCanceled()) break;
										Point3d test = set.getNodeStructure().getXYZEdgeFromIJK(set.getNodeStructure().getIJKFromNodeNumber(node));
										if (minZ <= test.getZ() && test.getZ() <= maxZ)
											temp.add(node);
									}
									set.getSensorSettings(sensorType).setValidNodes(temp);
								}
							}

							// If the user canceled, should we clear the data????
							if(monitor.isCanceled()) {
								// TODO: We're probably in an invalid state? clear everything???
								for(String sensorType: sensorSettings.keySet()) {							
									SensorData data = sensorSettings.get(sensorType);
									if(data.isIncluded) {
										if(!set.getSensorSettings().containsKey(sensorType)) { // Reset it, user  must have re selected it?
											set.resetSensorSettings(sensorType, data.min, data.max);
											set.getSensorSettings().get(sensorType).setUserSettings(
													data.cost, 
													Color.BLUE,
													data.min,
													data.max,
													data.trigger, 
													reset,
													data.deltaType,
													data.maxZ,
													data.minZ);
										}
										set.getSensorSettings().get(sensorType).setNodesReady(false);
									} else {
										set.removeSensorSettings(sensorType);
										set.getInferenceTest().setMinimumRequiredForType(sensorType, 0);							
									}
								}
							} 
						} catch (Exception e) {
							System.out.println(monitor.isCanceled());
							e.printStackTrace();
						}

					}
				});
			} catch (Exception e) {
				float totalNodes = 0;
				for(String sc : Constants.hdf5CloudData.keySet()) {
					for(float ts: Constants.hdf5CloudData.get(sc).keySet()) {
						for(String dt: Constants.hdf5CloudData.get(sc).get(ts).keySet()) {
							totalNodes += Constants.hdf5CloudData.get(sc).get(ts).get(dt).keySet().size();
							if(Constants.hdf5CloudData.get(sc).get(ts).get(dt).keySet().size() > 1000)
								System.out.println(sc + ", " + ts + ", " + dt + " , " + Constants.hdf5CloudData.get(sc).get(ts).get(dt).keySet().size());
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
						set.getInferenceTest().setMinimumRequiredForType(sensorType, requiredSensors.get(sensorType));
						set.getInferenceTest().setMinimum(totalMinimum);
						monitor.worked(1);
					}

				}
			});			
		}

		public ScenarioSet getSet() {
			return set;
		}

		public MUTATE getMutate() {
			return mutate;
		}

		public ExtendedConfiguration getInitialConfiguration() {
			return initialConfiguration;
		}

		public void run(final int runs, final boolean showPlots) throws Exception {		
			if(showPlots){
				wizard.launchVisWindow();	
				runner.setDomainViewer(wizard.domainViewer);
			}
			dialog.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Running iterative procedure ", set.getIterations()*runs);	
					runner.setMonitor(monitor);
					if(runs > 1) {
						runner.run(data.modelOption, initialConfiguration, set, showPlots, runs);	
					} else {

						runner.run(data.modelOption, initialConfiguration, set, showPlots);	
					}					
				}
			});
		}

		public float runObjective(ExtendedConfiguration configuration, boolean runThreaded) {
			return runner.objective(configuration, set, runThreaded);
		}

		public void randomEnumeration(final int max) throws Exception {			
			wizard.launchVisWindow();
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
			wizard.launchVisWindow();
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
	}
} 