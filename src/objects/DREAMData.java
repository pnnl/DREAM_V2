package objects;

import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;

import functions.CCS9_1;
import functions.Function;
import functions.MutationFunction.MUTATE;
import utilities.Constants;
import utilities.Constants.ModelOption;
import utilities.Point3d;
import wizardPages.DREAMWizard;
import wizardPages.Page_SensorSetup.SensorData;

public class DREAMData {

	private ScenarioSet set;
	private Function runner;
	private MUTATE mutate;
	private ExtendedConfiguration initialConfiguration;
	transient private DREAMWizard wizard;
	transient private WizardDialog dialog;

	private Constants.ModelOption modelOption;

	public DREAMData(DREAMWizard wizard, WizardDialog dialog) {
		this.wizard = wizard;
		set = new ScenarioSet();
		initialConfiguration = new ExtendedConfiguration();
		mutate = MUTATE.SENSOR;
	}

	public ScenarioSet getScenarioSet() {
		return set;
	}

	public void setupScenarioSet(final ModelOption modelOption, final MUTATE mutate, final String function, final String hdf5, final boolean keepOldData) throws Exception {	
		dialog.run(true, false, new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				// Run tasks:
				monitor.beginTask("Loading scenario set", 10);

				monitor.subTask("reading hdf5 files");

				if(!hdf5.isEmpty()) 
					Constants.loadHdf5Files(hdf5);	// Load the hdf5 files into the constants
				monitor.worked(5);

				if(!keepOldData){
					monitor.subTask("clearing previous data");				
					set.clearRun();	// Clear any old run data
					monitor.worked(1);
				}
				
				monitor.subTask("loading new data");
				set.loadRunData(""); // Load the new data (this will be hdf5)
				monitor.worked(1);
				
				monitor.subTask("applying user settings");
				DREAMData.this.mutate = mutate;	// Set the mutate option
				DREAMData.this.modelOption = modelOption;

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

				for(Scenario s: set.getAllScenarios()){
					if(!set.getScenarios().contains(s)) set.getScenarios().add(s);
				}
				
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

	public void setScenarioSet(ScenarioSet set) {
		this.set = set;			
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
			runner.setDomainViewer(wizard.getDomainViewer());
		}
		dialog.run(true, true, new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.beginTask("Running iterative procedure ", set.getIterations()*runs);	
				runner.setMonitor(monitor);
				if(runs > 1) {
					runner.run(modelOption, initialConfiguration, set, showPlots, runs);	
				} else {

					runner.run(modelOption, initialConfiguration, set, showPlots);	
				}					
			}
		});
	}

	public float runObjective(ExtendedConfiguration configuration, boolean runThreaded) {
		return runner.objective(configuration, set, runThreaded);
	}

	public void randomEnumeration(final int max) throws Exception {			
		wizard.launchVisWindow();
		runner.setDomainViewer(wizard.getDomainViewer());
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
		runner.setDomainViewer(wizard.getDomainViewer());
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

	public void setDialog(WizardDialog dialog) {
		this.dialog = dialog;
	}

	public void setAs(DREAMData load) {
		this.set = load.set;
		this.mutate = load.mutate;
		this.initialConfiguration = load.initialConfiguration;
		// TODO: Luke...
	}

}