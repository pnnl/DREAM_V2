package functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;

import objects.E4DSensors;
import objects.ExtendedConfiguration;
import objects.InferenceResult;
import objects.ScenarioSet;
import objects.ExtendedSensor;
import results.ResultPrinter;
import utilities.Constants;
import visualization.DomainVisualization;

/**
 * Any new functions should extend this class, that will allow pluggable pieces.
 * @author port091
 * @author whit162
 */

public class Function implements ObjectiveFunction, MutationFunction, InferenceModel {
	
	protected Integer currentIteration = -3; 
	protected Integer currentRun = 0;
	
	protected MUTATE mutate = MUTATE.SENSOR; // Default mutation
	
	protected boolean iterative; // Keep track of what type of run we are doing
	
	// Keep some history for nodes, will be faster to look here for values then from the database/file
	// Scenario, node #, time step, data type, value
	protected volatile Map<String, Map<Integer, Map<Float, Map<String, Boolean>>>> history;

	private IProgressMonitor monitor; // So we can update the status
	private DomainVisualization viewer; // Graphical representation of the run
	
	/**
	 * Main algorithm
	 */

	/**
	 * Runs a random subset of the provided scenario set
	 * @param initialConfiguration
	 * @param set
	 * @param max
	 * @param domainViewer 
	 * @param monitor 
	 */
	public void fullEnumeration(ExtendedConfiguration initialConfiguration, ScenarioSet set, int max) {
		ResultPrinter.clearResults(set, false);
		iterative = false;
		List<ExtendedSensor> sensors =  getAllPossibleSensors(set);
		this.max = max;
		Collections.shuffle(sensors, Constants.random);// Randomize
		generateConfigurations(new ArrayList<ExtendedSensor>(),  sensors, set);
		ResultPrinter.printAll(set.getNodeStructure());
	}


	/**
	 * Runs full enumeration given the provided scenario set
	 * @param initialConfiguration
	 * @param set
	 */
	public void fullEnumeration(ExtendedConfiguration initialConfiguration, ScenarioSet set) {
		ResultPrinter.clearResults(set, false);
		iterative = false;
		generateConfigurations(new ArrayList<ExtendedSensor>(),  getAllPossibleSensors(set), set);
		ResultPrinter.printAll(set.getNodeStructure());
	}

	/**
	 * Runs a set of basic algorithms on each scenario set provided
	 */
	public void run(ExtendedConfiguration initialConfiguration, List<ScenarioSet> sets) {
		if(!sets.isEmpty()) {
			ResultPrinter.clearResults(sets.get(0), false);
			for(ScenarioSet set: sets) {
				runInternal(initialConfiguration, set);
			}
			ResultPrinter.printAll(sets.get(0).getNodeStructure());
		}
	}
	

	/**
	 * Runs a set of identical basic algorithms
	 * @param monitor 
	 */
	
	
	public boolean run(ExtendedConfiguration initialConfiguration, ScenarioSet set, boolean showPlots, int sets) {
		boolean wasCancelled = false;
		ResultPrinter.clearResults(set, showPlots);
		for(int i = 0; i < sets; i++) {
			if(i !=0) ResultPrinter.newTTDPlots(set, i+1); //already set up for the first iteration
			currentRun = i;
			if(monitor != null) 
				monitor.setTaskName("Running iterative procedure " + (i+1) + "/" + sets);
			wasCancelled = runInternal(initialConfiguration, set);
			if(wasCancelled) {
				ResultPrinter.runScripts = false;
				return true;
			}
		}
		ResultPrinter.printAll(set.getNodeStructure());
		return false;
	}

	/**
	 * Runs the basic algorithm, with the mutate parameter (either well or sensors)
	 * @param modelOption 
	 * @param initialConfiguration
	 * @param set
	 * @param runs 
	 * @param monitor 
	 * @param mutate
	 */
	//public void run(Configuration initialConfiguration, ScenarioSet set, MUTATE mutate) {
	//	this.mutate = mutate;
	//	run(initialConfiguration, set, null);
	//}
	
	public boolean run(ExtendedConfiguration initialConfiguration, ScenarioSet set, boolean showPlots) {
		boolean wasCancelled = false;
		ResultPrinter.clearResults(set, showPlots);
		wasCancelled = runInternal(initialConfiguration, set);
		ResultPrinter.printAll(set.getNodeStructure());
		return wasCancelled;
	}

	public void run(ExtendedConfiguration initialConfiguration, ScenarioSet set, String resultTag) {
		ResultPrinter.clearResults(set, false);
		runInternal(initialConfiguration, set);
		ResultPrinter.printAll(set.getNodeStructure());
	}
//	/**
//	 * Specifically only for multi-run ensemble.
//	 */
//	public boolean run(ExtendedConfiguration initialConfiguration, ScenarioSet set, boolean showPlots
//			,final int theIteration, final boolean multiRun) {
//		if(monitor != null)  {
//			monitor.setTaskName("Running iterative procedure " + "for full run number " + (theIteration +1) );
//		}
//		boolean wasCancelled = false;
//		ResultPrinter.clearResults(set, showPlots);
//		wasCancelled = runInternal(initialConfiguration, set);
//		ResultPrinter.printAll(set.getNodeStructure());
//		return wasCancelled;
//			
//	}

	/**
	 * Runs the basic algorithm
	 * @param initialConfiguration
	 * @param set
	 */
	private boolean runInternal(final ExtendedConfiguration initialConfiguration, ScenarioSet set) {
		iterative = true;
		float percent = (float) 0.2;
		
		Constants.log(Level.INFO, "Function: running", null);
		Constants.log(Level.FINER, "Function: running - initial configuration", initialConfiguration);
		
		currentIteration = -3;
		
		// Create three more configurations from the initial configuration
		ExtendedConfiguration currentConfiguration = initialConfiguration.makeCopy();
		ExtendedConfiguration newConfiguration = initialConfiguration.makeCopy();
		ExtendedConfiguration bestConfiguration = initialConfiguration.makeCopy();
		float currentValue = objective(currentConfiguration, set, Constants.runThreaded);
		float newValue = currentValue;
		float bestValue = currentValue;
		ResultPrinter.storeResults(currentRun, currentIteration, newConfiguration, bestConfiguration, currentConfiguration, set);
		
		// Apply first mutation
		mutate(newConfiguration, set);
		
		Constants.log(Level.FINER, "Function: running - new configuration", newConfiguration);
		currentIteration = -2;
		currentValue = objective(currentConfiguration, set, Constants.runThreaded);
		ResultPrinter.storeResults(currentRun, currentIteration, newConfiguration, bestConfiguration, currentConfiguration, set);
		
		currentIteration = -1;
		newValue = objective(newConfiguration, set, Constants.runThreaded);
		bestValue = currentValue;
		double temperature = 1;
		
		float totalMutateTime = 0;
		float totalObjectiveTime = 0;
		
		Constants.log(Level.FINER, "Function: running - iteration", "-1\tCurrent: " + currentValue + "\tNew: " + newValue + "\tBest: " + bestValue);
		ResultPrinter.storeResults(currentRun, currentIteration, newConfiguration, bestConfiguration, currentConfiguration, set);
		
		// Did not detect in any scenario
		if(bestValue < 0)
			bestValue = Integer.MAX_VALUE;
		
		// Hack to add a well pairing for ERT technology
		newConfiguration = E4DSensors.ertAddPairing(newConfiguration, currentConfiguration, set.getNodeStructure());
		
		int counter = 0; //count the number of iterations since the last "best"
		for(currentIteration = 0; currentIteration < set.getIterations(); currentIteration++) {
			//temperature= temperature * 0.99f;
			temperature = java.lang.Math.pow(0.01,(double)currentIteration/(double)set.getIterations()); //exponential decay
			//float randomValue = 1;
			float randomValue = Constants.random.nextFloat(); //seeded random number for consistent results
			counter++;
			
			if(monitor.isCanceled()) {
				ResultPrinter.runScripts = false;
				return true;
			}
			if(monitor != null)
				monitor.subTask("iteration " + currentIteration);
			System.out.println("Iteration "+currentIteration+", Current "+currentValue+", New "+newValue+"("+newConfiguration.countScenariosDetected()+" detected), Best "+bestValue);
			
			// If new configuration is better than current, set current equal to new
			if(newValue < currentValue) {
				Constants.log(Level.FINER, "Function: running - new configuration was better than current, swapping them.", "newValue="+newValue+", currentValue="+currentValue+", Temp="+temperature);
				// Make a copy of the new configuration and save it into the current configuration
				currentConfiguration.matchConfiguration(newConfiguration);
				currentValue = newValue;
			}
			// If new configuration is worse than current, compare with temperature to decide whether to swap
			else if (newValue >= currentValue) {
				if (temperature > randomValue) {
					Constants.log(Level.FINER, "Function: running - new configuration was worse than current, but swapping them anyway.", "newValue="+newValue+", currentValue="+currentValue+", Temp="+temperature+", rand="+randomValue);
					currentConfiguration.matchConfiguration(newConfiguration);
					currentValue = newValue;
				} else {
					Constants.log(Level.FINER, "Function: running - new configuration was worse than current, NOT swapping.", "newValue="+newValue+", currentValue="+currentValue+", Temp="+temperature+", rand="+randomValue);
				}
			}
			
			// If our current value is better or equal to our best value
			if(newValue <= bestValue) {
				Constants.log(Level.FINER, "Function: running - new configuration was better then best, swapping them.", "currentValue="+currentValue+", bestValue="+bestValue+", Temp="+temperature);
				// Make a copy of the current configuration and save it into the best configuration
				bestConfiguration.matchConfiguration(newConfiguration);
				bestValue = newValue;
				counter = 0; //reset the counter to 0
			}
			
			// Starting the next iteration - newConfiguration starts as the currentConfiguration
			// Unless we haven't found a new best configuration for a while, then rebase to bestConfiguration
			if(counter > set.getIterations() * percent) {
				newConfiguration.matchConfiguration(bestConfiguration);
				counter = 0; //reset the counter to 0
				System.out.println("Rebase to the best configuration, "+set.getIterations()*percent+" iterations since a new best");
			} else
				newConfiguration.matchConfiguration(currentConfiguration);
			
			// Mutate the new configuration
			long start = System.currentTimeMillis();
			mutate(newConfiguration, set);
			newConfiguration.orderSensors(); //Order the sensors so we can avoid saving duplicate configurations
			newConfiguration = E4DSensors.ertAddPairing(newConfiguration, currentConfiguration, set.getNodeStructure()); // Hack to add a well pairing for ERT technology
			float ttm = System.currentTimeMillis()-start;
			Constants.log(Level.FINE, "Function: running - time taken to mutate", (ttm) + " ms");
			totalMutateTime += ttm;
			Constants.log(Level.FINER, "Function: running - new configuration", newConfiguration);
			
			// Get the new objective value
			start = System.currentTimeMillis();
			newValue = objective(newConfiguration, set, Constants.runThreaded);
			float tto = System.currentTimeMillis()-start;
			Constants.log(Level.FINE, "Function: running - time taken to run objective", (tto) + " ms");
			totalObjectiveTime += tto;
			Constants.log(Level.FINER, "Function: running - iteration", currentIteration + "\tCurrent: " + currentValue + "\tNew: " + newValue + "\tBest: " + bestValue);
			
			// Save the new configuration in results
			ResultPrinter.storeResults(currentRun, currentIteration, newConfiguration, bestConfiguration, currentConfiguration, set);
			
			// Save the new configuration in the viewer
			if(viewer != null)
				viewer.addConfiguration(newConfiguration);
			
			// Add completed work to the monitor
			if(monitor != null)
				monitor.worked(1);
		}
		
		if(viewer != null) {
			//After running iterations, scan through the viewer configurations and remove duplicates
			viewer.sortConfigurations();
			//After running iterations, clear the last displayed configuration
			viewer.clearViewer();
		}
		
		currentIteration = null;

		Constants.log(Level.FINE, "Function: running - total time taken to run objective", (totalMutateTime) + " ms");
		Constants.log(Level.FINE, "Function: running - total time taken to run objective", (totalObjectiveTime) + " ms");

		Constants.log(Level.INFO, "Function: done running", null);
		Constants.log(Level.CONFIG, "Function: best configuration", bestConfiguration);		
		
		System.out.println(Constants.timer);
		return false;
	}
	
	
	public List<ExtendedSensor> getAllPossibleSensors(ScenarioSet set) {
		List<ExtendedSensor> sensors = new ArrayList<ExtendedSensor>();
		for (String type : set.getDataTypes()) {
			for (Integer nodePosition : set.getSensorSettings(type).getValidNodes()) {
				sensors.add(new ExtendedSensor(nodePosition, type, set.getNodeStructure()));
			}
		}
		return sensors;
	}
	
	
	private int bruteForceEnumCount = 0;
	private int max = 0;

	public void generateConfigurations(List<ExtendedSensor> sensors, List<ExtendedSensor> possibleSensors, ScenarioSet set) {
		
		if(max != 0 && bruteForceEnumCount == max)
			return; // Done
		
		// Every configuration will go through here so long as there is at least
		// enough sensors of each kind to trigger inference
		boolean result = isDone(sensors, set);
		if (result) {
			// Create a configuration
			ExtendedConfiguration configuration = new ExtendedConfiguration();
			for (ExtendedSensor sensor : sensors)
				configuration.addSensor(sensor);
			
			// Hack to add a well pairing for ERT technology
			//E4DSensors.ertAddPairing(configuration); //TODO: Make sure this doesn't need to be called during Full Enumeration
			
			objective(configuration, set, Constants.runThreaded);
			bruteForceEnumCount++;
			if(monitor != null)
				monitor.worked(1);
			if(monitor != null)
				monitor.subTask("Iteration " + bruteForceEnumCount);
			System.out.println("Iteration " + bruteForceEnumCount);
		}
		// Setup the recursion
		for (int i = 0; i < possibleSensors.size(); i++) {
			// We want only unique configurations without duplicates ABC gives
			// [[A], [A, B], [A, B, C], [A, C], [B], [B, C], [C]]
			List<ExtendedSensor> subSensors = new ArrayList<ExtendedSensor>();
			for (int j = i + 1; j < possibleSensors.size(); j++) {
				subSensors.add(possibleSensors.get(j));
			}
			// We want a different list for each step branch of the recursion
			List<ExtendedSensor> copy = new ArrayList<ExtendedSensor>();
			for (ExtendedSensor sensor : sensors) {
				copy.add(sensor);
			}
			copy.add(possibleSensors.get(i));
			// Don't continue the recursion if we've exceeded our cost
			// constraint
			if (isValidConfiguration(copy, set)) // We can afford the
				// configuration and the #
				// of wells is <= constraint
				generateConfigurations(copy, subSensors, set);
		}
	}
	
	
	// This checks that the configuration meets constraints
	private boolean isValidConfiguration(List<ExtendedSensor> sensors, ScenarioSet set) {
		Map<String, Integer> sensorsPerType = new HashMap<String, Integer>();
		List<String> ijs = new ArrayList<String>();
		float totalCost = 0;
		
		// Loop through sensors in configuration and count sensors per type and well locations
		for(ExtendedSensor sensor: sensors) {
			totalCost += set.getSensorSettings(sensor.getSensorType()).getSensorCost();
			String IJ = sensor.getIJK().getI() + "_" + sensor.getIJK().getJ();
			if(!ijs.contains(IJ))
				ijs.add(IJ);
			if(!sensorsPerType.containsKey(sensor.getSensorType()))
				sensorsPerType.put(sensor.getSensorType(), 0);
			sensorsPerType.put(sensor.getSensorType(), sensorsPerType.get(sensor.getSensorType()) + 1);
		}
		
		// Verify that the configuration can pass at least one inference test
		boolean inference = set.getInferenceTest().reachedInference(sensorsPerType);
		// Verify that the cost of the configuration is less than the constraint
		boolean canAffordConfiguration = totalCost <= set.getSensorCostConstraint();
		// Verify that the number of wells is not more than the total
		boolean canDrillWells = ijs.size() <= set.getMaxWells();
		
		return inference && canAffordConfiguration && canDrillWells;
	}
	
	
	private boolean isDone(List<ExtendedSensor> sensors, ScenarioSet set) {
		// return !sensors.isEmpty(); <- for testing this will give all sets
		Map<String, Integer> sensorsPerType = new HashMap<String, Integer>();
		// Count the number of sensors per type
		for(ExtendedSensor sensor: sensors) {
			if(!sensorsPerType.containsKey(sensor.getSensorType()))
				sensorsPerType.put(sensor.getSensorType(), 0);
			sensorsPerType.put(sensor.getSensorType(), sensorsPerType.get(sensor.getSensorType()) + 1);

		}
		// Check that we have enough of each type of sensor
		return set.getInferenceTest().reachedInference(sensorsPerType);
	}


	/**					**\
	 * Extended classes	 *
	 * should override	 *
	\* these methods	 */
	@Override
	public InferenceResult inference(ExtendedConfiguration configuration, ScenarioSet set, String scenario) {
		Constants.log(Level.WARNING, "Function: inference - no child inference model defined",  null);
		return null;
	}

	@Override
	public boolean mutate(ExtendedConfiguration configuration, ScenarioSet set) {
		if(mutate.equals(MUTATE.SENSOR)) {
			return configuration.mutateSensor(set);
		} else if(mutate.equals(MUTATE.WELL)) {
			System.out.println("Mutating well");
			return configuration.mutateWell(set);
		}		
		return false;
	}

	@Override
	public Float objective(ExtendedConfiguration configuration, ScenarioSet set, boolean runThreaded) {
		// Where will run the algorithm...
		Constants.log(Level.WARNING, "Function: inference - no child objective function defined",  null);
		return null;
	}

	public MUTATE getMutate() {
		return mutate;
	}

	public void setResultsDirectory(String resultsDirectory) {
		ResultPrinter.resultsDirectory = resultsDirectory;
	}

	public void setDomainViewer(DomainVisualization domainViewer) {
		this.viewer = domainViewer;
	}
	
	public void setMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}


	/**					**\
	 * Helper Methods	 *
	\* 					 */

	protected synchronized void storeHistory(String scenario, Integer nodeNumber, Float timeStep, String dataType, Boolean triggered) {
		
		if(history == null)
			history = Collections.synchronizedMap(new HashMap<String, Map<Integer, Map<Float, Map<String, Boolean>>>>());

		if(!history.containsKey(scenario)) {
			history.put(scenario, Collections.synchronizedMap(new HashMap<Integer, Map<Float, Map<String, Boolean>>>()));
		}

		if(!history.get(scenario).containsKey(nodeNumber)) {
			history.get(scenario).put(nodeNumber, Collections.synchronizedMap(new HashMap<Float, Map<String, Boolean>>()));
		}

		if(!history.get(scenario).get(nodeNumber).containsKey(timeStep)) {
			history.get(scenario).get(nodeNumber).put(timeStep, Collections.synchronizedMap(new HashMap<String, Boolean>()));
		}

		history.get(scenario).get(nodeNumber).get(timeStep).put(dataType, triggered);
		
		

	}


	protected synchronized Boolean getHistory(String scenario, Integer nodeNumber, Float timeStep, String dataType) {
		
		if(history == null)
			return null;

	///	System.out.println("History size: " + history.size());
		if(!history.containsKey(scenario)) {
			return null;
		}
		
		if(!history.get(scenario).containsKey(nodeNumber)) {
			return null;
		}

//		System.out.println("History size: " + history.get(scenario).size());

		if(!history.get(scenario).get(nodeNumber).containsKey(timeStep)) {
			return null;
		}

//		System.out.println("History size: " + history.get(scenario).get(nodeNumber).size());

		if(!history.get(scenario).get(nodeNumber).get(timeStep).containsKey(dataType)) {
			return null;
		}

//		System.out.println("History size: " + history.get(scenario).get(nodeNumber).get(timeStep).size());

		return history.get(scenario).get(nodeNumber).get(timeStep).get(dataType);

	}
	


}
