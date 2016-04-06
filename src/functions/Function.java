package functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;

import objects.ExtendedConfiguration;
import objects.InferenceResult;
import objects.Scenario;
import objects.ScenarioSet;
import objects.ExtendedSensor;
import results.ResultPrinter;
import utilities.Constants;
import utilities.Constants.ModelOption;
import visualization.MultiDomainViewer;

/**
 * Any new functions should extend this class, that will allow pluggable pieces.
 * @author port091
 */

public class Function implements ObjectiveFunction, MutationFunction, InferenceModel {
	
	protected Integer currentIteration = -3; 
	protected Integer currentRun = 0;
	
	protected MUTATE mutate = MUTATE.SENSOR; // Default mutation
	protected Constants.ModelOption modelOption = ModelOption.INDIVIDUAL_SENSORS;
	
	protected boolean iterative; // Keep track of what type of run we are doing
	
	// Keep some history for nodes, will be faster to look here for values then from the database/file
	// Scenario, node #, time step, data type, value
	protected volatile Map<Scenario, Map<Integer, Map<Float, Map<String, Boolean>>>> history;

	private IProgressMonitor monitor; // So we can update the status
	private MultiDomainViewer viewer; // Graphical representation of the run

	int counter = 0;
	
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
		ResultPrinter.printAll();
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
		ResultPrinter.printAll();
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
			ResultPrinter.printAll();
		}
	}
	

	/**
	 * Runs a set of identical basic algorithms
	 * @param monitor 
	 */
	
	
	public void run(ModelOption modelOption, ExtendedConfiguration initialConfiguration, ScenarioSet set, boolean showPlots, int sets) {
		ResultPrinter.clearResults(set, showPlots);
		for(int i = 0; i < sets; i++) {
			if(monitor.isCanceled())
				return;
			if(i !=0) ResultPrinter.newTTDPlots(set, i+1); //already set up for the first iteration
			currentRun = i;
			if(monitor != null) 
				monitor.setTaskName("Running iterative procedure " + (i+1) + "/" + sets);
			runInternal(initialConfiguration, set);
		}
		ResultPrinter.printAll();
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

	public void run(ModelOption modelOption, ExtendedConfiguration initialConfiguration, ScenarioSet set, boolean showPlots) {
		ResultPrinter.clearResults(set, showPlots);
		this.modelOption = modelOption;
		runInternal(initialConfiguration, set);
		ResultPrinter.printAll();
	}


	public void run(ExtendedConfiguration initialConfiguration, ScenarioSet set, String resultTag) {
		ResultPrinter.clearResults(set, false);
		runInternal(initialConfiguration, set);
		ResultPrinter.printAll();
	}

	/**
	 * Runs the basic algorithm
	 * @param initialConfiguration
	 * @param set
	 */
	private void runInternal(final ExtendedConfiguration initialConfiguration, ScenarioSet set) {
		iterative = true;
		
		// resultsByType = new HashMap<String, ArrayList<String>>();
		
		// storeResultSet(new ResultSet(set, mutate));

		Constants.log(Level.INFO, "Function: running", null);
		Constants.log(Level.FINER, "Function: running - initial configuration", initialConfiguration);

		currentIteration = -3;
		
		// Create three more configurations from the initial configuration
		final ExtendedConfiguration currentConfiguration = initialConfiguration.makeCopy(set);
		final ExtendedConfiguration newConfiguration = initialConfiguration.makeCopy(set);
		final ExtendedConfiguration bestConfiguration = initialConfiguration.makeCopy(set);
		float currentValue = objective(currentConfiguration, set, true);
		float newValue = currentValue;
		float bestValue = currentValue;
		ResultPrinter.storeResults(currentRun, currentIteration, newConfiguration, bestConfiguration, currentConfiguration, set);
		
		if(viewer != null)
			viewer.assignConfigurations(initialConfiguration, newConfiguration, bestConfiguration, currentConfiguration);

		// Apply first mutation
		mutate(newConfiguration, set);

		Constants.log(Level.FINER, "Function: running - new configuration", newConfiguration);
		currentIteration = -2;
		currentValue = objective(currentConfiguration, set, true);
		ResultPrinter.storeResults(currentRun, currentIteration, newConfiguration, bestConfiguration, currentConfiguration, set);
	
		currentIteration = -1;
		newValue = objective(newConfiguration, set, true);
		bestValue = currentValue;
		float temperature = 100;

		float totalMutateTime = 0;
		float totalObjectiveTime = 0;

		Constants.log(Level.FINER, "Function: running - iteration", "-1\tCurrent: " + currentValue + "\tNew: " + newValue + "\tBest: " + bestValue);
		
		ResultPrinter.storeResults(currentRun, currentIteration, newConfiguration, bestConfiguration, currentConfiguration, set);
		
		// Did not detect in any scenario
		if(bestValue < 0)
			bestValue = Integer.MAX_VALUE;
		
		for(int iteration = 0; iteration < set.getIterations(); iteration++) {
			
			if(monitor.isCanceled())
				return;
			
			long timeToStoreResults = 0;
			long timeToMatchConfig = 0;
			long timeToMutate = 0;
			long timeForObjective = 0;
			long temp = 0;
			
			long startTime = System.currentTimeMillis();
			currentIteration = iteration;
			if(monitor != null)
				monitor.subTask("iteration " + iteration);
			
			if(viewer != null) {
				viewer.displayTools.get(MultiDomainViewer.SensorConfigType.BEST).triggerDisplayThread();
				viewer.displayTools.get(MultiDomainViewer.SensorConfigType.CURRENT).triggerDisplayThread();
				viewer.displayTools.get(MultiDomainViewer.SensorConfigType.INITIAL).triggerDisplayThread();
				viewer.displayTools.get(MultiDomainViewer.SensorConfigType.NEW).triggerDisplayThread();
			}

			float calculatedValue;
			float randomValue;
			
			
			System.out.println("Iteration " + iteration + ", Current " + currentValue + ", New " + newValue + ", Best " + bestValue);

			
			// If new configuration is better then current, set current equal to new
			if(newValue >= 0 && newValue < currentValue) {
				//storeResult(newConfiguration);
				Constants.log(Level.FINER, "Function: running - new configuration was better than current, swapping them.", "newValue="+ newValue + ", currentValue=" + currentValue + ", Temp=" + temperature);
				temp = System.currentTimeMillis();
				currentConfiguration.matchConfiguration(set, newConfiguration);
				timeToMatchConfig += System.currentTimeMillis() - temp;
				//currentConfiguration = newConfiguration.makeCopy(set);
				currentValue = newValue;				
				// If our current value is better then our best value
				if(currentValue >= 0 && currentValue < bestValue) {
					
					temp = System.currentTimeMillis();
					timeToStoreResults += System.currentTimeMillis() - temp;
					
					Constants.log(Level.FINER, "Function: running - new configuration was better then best, swapping them.", "currentValue=" + currentValue + ", bestValue=" + bestValue + ", Temp=" + temperature);
					// Make a copy of the current configuration and save it into the best configuration
					//		bestConfiguration = currentConfiguration.makeCopy(set);	
					temp = System.currentTimeMillis();
					bestConfiguration.matchConfiguration(set, currentConfiguration);
					timeToMatchConfig += System.currentTimeMillis() - temp;
					bestValue = currentValue;
				}
			} 

			// If new configuration is worse than current, evaluate temp function to decide whether to swap
			else if (newValue >= currentValue){
				temperature= temperature * 0.99f;
				calculatedValue = (float)Math.exp(-(newValue - currentValue) / temperature);
				//calculatedValue = 1;
				randomValue = Constants.random.nextFloat();
				//randomValue = 1;
				if (calculatedValue > randomValue) {
					//	currentConfiguration = newConfiguration.makeCopy(set);
					temp = System.currentTimeMillis();					
					currentConfiguration.matchConfiguration(set, newConfiguration);
					timeToMatchConfig += System.currentTimeMillis() - temp;
										
					currentValue = newValue;
					Constants.log(Level.FINER, "Function: running - new configuration was worse than current, but swapping them anyway.", "newValue=" + newValue + ", currentValue=" + currentValue + ", Temp=" + temperature + ", Temp Function=" + calculatedValue + ", rand=" + randomValue);
				}
				else {
					Constants.log(Level.FINER, "Function: running - new configuration was worse than current, NOT swapping.", "newValue=" + newValue + ", currentValue=" + currentValue + ", Temp=" + temperature + ", Temp Function=" + calculatedValue + ", rand=" + randomValue);
				}
			}

			//Do different stuff if new configuration and current are equal, using a somewhat arbitrary delta NOT DOING ANYTHING RIGHT NOW
			else { //if (newValue == currentValue){
				temperature= temperature * 0.99f;
				float fiftyAtNinetyNine = (float)(-99*Math.log(.5));
				calculatedValue = (float)Math.exp(-(fiftyAtNinetyNine) / temperature);
				//randomValue = Constants.random.nextFloat();
				randomValue = 1;
				if (calculatedValue > randomValue) {
					//	currentConfiguration = newConfiguration.makeCopy(set);
					temp = System.currentTimeMillis();
					currentConfiguration.matchConfiguration(set, newConfiguration);
					timeToMatchConfig += System.currentTimeMillis() - temp;
					
					currentValue = newValue;
					Constants.log(Level.FINER, "Function: running - new configuration was equal to current, but swapping them anyway.", "newValue=" + newValue + ", currentValue=" + currentValue + ", Temp=" + temperature + ", Temp Function=" + calculatedValue + ", rand=" + randomValue);
				}
				else{
					Constants.log(Level.FINER, "Function: running - new configuration was equal to current, Not swapping.", "newValue=" + newValue + ", currentValue=" + currentValue + ", Temp=" + temperature + ", Temp Function=" + calculatedValue + ", rand=" + randomValue);
				}
			}

			long ttmStart = System.currentTimeMillis();
			// Mutate the new configuration
			// Make sure we mutate the current
			temp = System.currentTimeMillis();
			newConfiguration.matchConfiguration(set, currentConfiguration);
			timeToMatchConfig += System.currentTimeMillis() - temp;
			
			temp = System.currentTimeMillis();
			mutate(newConfiguration, set);
			timeToMutate += System.currentTimeMillis() - temp;
			
			float ttm = System.currentTimeMillis()-ttmStart;
			Constants.log(Level.FINE, "Function: running - time taken to mutate", (ttm) + " ms");
			totalMutateTime += ttm;
			Constants.log(Level.FINER, "Function: running - new configuration", newConfiguration);

			// Get the new value
			ttmStart = System.currentTimeMillis();

			temp = System.currentTimeMillis();
			newValue = objective(newConfiguration, set, true);
			timeForObjective = System.currentTimeMillis() - temp;

			float tto = System.currentTimeMillis()-ttmStart;
			Constants.log(Level.FINE, "Function: running - time taken to run objective", (tto) + " ms");		
			totalObjectiveTime += tto;
			Constants.log(Level.FINER, "Function: running - iteration", iteration + "\tCurrent: " + currentValue + "\tNew: " + newValue + "\tBest: " + bestValue);
			
			
			ResultPrinter.storeResults(currentRun, currentIteration, newConfiguration, bestConfiguration, currentConfiguration, set);
			
			if(viewer != null) {
				viewer.displayTools.get(MultiDomainViewer.SensorConfigType.BEST).triggerDisplayThread();
				viewer.displayTools.get(MultiDomainViewer.SensorConfigType.CURRENT).triggerDisplayThread();
				viewer.displayTools.get(MultiDomainViewer.SensorConfigType.INITIAL).triggerDisplayThread();
				viewer.displayTools.get(MultiDomainViewer.SensorConfigType.NEW).triggerDisplayThread();
			}

			if(monitor != null)
				monitor.worked(1);
		}
		
		currentIteration = null;

		Constants.log(Level.FINE, "Function: running - total time taken to run objective", (totalMutateTime) + " ms");
		Constants.log(Level.FINE, "Function: running - total time taken to run objective", (totalObjectiveTime) + " ms");

		Constants.log(Level.INFO, "Function: done running", null);
		Constants.log(Level.CONFIG, "Function: best configuration", bestConfiguration);		

		if(viewer != null) {
			viewer.displayTools.get(MultiDomainViewer.SensorConfigType.BEST).triggerDisplayThread();
			viewer.displayTools.get(MultiDomainViewer.SensorConfigType.CURRENT).triggerDisplayThread();
			viewer.displayTools.get(MultiDomainViewer.SensorConfigType.INITIAL).triggerDisplayThread();
			viewer.displayTools.get(MultiDomainViewer.SensorConfigType.NEW).triggerDisplayThread();
		}
		
		System.out.println(Constants.timer);

	}


	public List<ExtendedSensor> getSensorsForTesting(ScenarioSet set) {
		List<ExtendedSensor> smallList = new ArrayList<ExtendedSensor>();
		// In well i = 1, j = 25
		smallList.add(new ExtendedSensor(601, "CO2", set.getNodeStructure()));
		smallList.add(new ExtendedSensor(1701, "CO2", set.getNodeStructure()));
		smallList.add(new ExtendedSensor(601, "Pressure", set.getNodeStructure()));
		smallList.add(new ExtendedSensor(6101, "Pressure", set.getNodeStructure()));

		// In well i = 1 j = 27
		smallList.add(new ExtendedSensor(651, "CO2", set.getNodeStructure()));
		smallList.add(new ExtendedSensor(5051, "Pressure", set.getNodeStructure()));

		// in well i = 3, j = 26
		smallList.add(new ExtendedSensor(628, "CO2", set.getNodeStructure()));
		smallList.add(new ExtendedSensor(6128, "Pressure", set.getNodeStructure()));

		// in well i = 2, j = 26
		smallList.add(new ExtendedSensor(627, "CO2", set.getNodeStructure()));
		smallList.add(new ExtendedSensor(6127, "Pressure", set.getNodeStructure()));

		return smallList;
	}

	public List<ExtendedSensor> getAllPossibleSensors(ScenarioSet set) {
		List<ExtendedSensor> sensors = new ArrayList<ExtendedSensor>();
		for (String type : set.getDataTypes()) {
			for (Integer nodePosition : set.getSensorSettings(type)
					.getValidNodes(null)) {
				sensors.add(new ExtendedSensor(nodePosition, type, set
						.getNodeStructure()));
			}
		}
		return sensors;
	}

	private int bruteForceEnumCount = 0;
	private int max = 0;

	public void generateConfigurations(List<ExtendedSensor> sensors,
			List<ExtendedSensor> possibleSensors, ScenarioSet set) {

		if(max != 0 && bruteForceEnumCount == max)
			return; // Done

		// Every configuration will go through here so long as there is at least
		// enough sensors of each kind to trigger inference
		String result = isDone(sensors, set);
		if (!result.isEmpty()) {
			// Create a configuration
			ExtendedConfiguration configuration = new ExtendedConfiguration();
			for (ExtendedSensor sensor : sensors)
				configuration.addSensor(set, sensor);

			objective(configuration, set, true);
			//storeResult(configuration);
			bruteForceEnumCount++;
			if(monitor != null)
				monitor.worked(1);
			if(viewer != null) {
				viewer.setConfiguration(configuration);
				viewer.displayTools.get(MultiDomainViewer.SensorConfigType.INITIAL).triggerDisplayThread();
			}
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

	public boolean canAffordSensors(List<ExtendedSensor> sensors, ScenarioSet set) {
		float totalCost = 0;
		for (ExtendedSensor sensor : sensors) {
			totalCost += set.getCost(sensor.getSensorType());
		}
		boolean canAffordConfiguration = totalCost <= set.getCostConstraint();
		return canAffordConfiguration;
	}

	private boolean isValidConfiguration(List<ExtendedSensor> sensors, ScenarioSet set) {
		Map<String, Integer> sensorsPerType = new HashMap<String, Integer>();
		List<String> ijs = new ArrayList<String>();
		float totalCost = 0;
		for (ExtendedSensor sensor : sensors) {
			totalCost += set.getCost(sensor.getSensorType());
			String IJ = sensor.getIJK().getI() + "_" + sensor.getIJK().getJ();
			if (!ijs.contains(IJ))
				ijs.add(IJ);
			if (!sensorsPerType.containsKey(sensor.getSensorType()))
				sensorsPerType.put(sensor.getSensorType(), 0);
			sensorsPerType.put(sensor.getSensorType(),
					sensorsPerType.get(sensor.getSensorType()) + 1);
		}
		for (String type : set.getDataTypes()) {
			int required = set.getInferenceTest().getMinimumForType(type);
			int difference = required
					- (sensorsPerType.containsKey(type) ? sensorsPerType
							.get(type) : 0);
			if (difference > 0) {
				totalCost += set.getCost(type) * difference;// We need to
				// consider the cost
				// of adding those
				// sensors
			}
		}
		float costConstraint = set.getCostConstraint();
		int wellConstraint = set.getMaxWells();
		// If we can afford it and the well constraint is satisfied
		boolean canAffordConfiguration = totalCost <= costConstraint;
		boolean canDrillWells = ijs.size() <= wellConstraint;
		boolean isValid = canAffordConfiguration && canDrillWells;
		// System.out.println("\t\t" + (isValid ? "Valid" : "Not valid: ") +
		// (canAffordConfiguration ? "\t\t\t\t" : "\tToo expensive:  " +
		// totalCost + " > " + costConstraint) +
		// (canDrillWells ? "" : "\tToo many wells:  " + ijs.size() + " > " +
		// wellConstraint));
		return isValid;
	}

	private String isDone(List<ExtendedSensor> sensors, ScenarioSet set) {
		// return !sensors.isEmpty(); <- for testing this will give all sets
		Map<String, Integer> sensorsPerType = new HashMap<String, Integer>();
		// Count the number of sensors per type
		for (ExtendedSensor sensor : sensors) {
			if (!sensorsPerType.containsKey(sensor.getSensorType()))
				sensorsPerType.put(sensor.getSensorType(), 0);
			sensorsPerType.put(sensor.getSensorType(),
					sensorsPerType.get(sensor.getSensorType()) + 1);

		}
		// System.out.println("Testing:\t"+sensorsPerType.toString());
		for (String type : set.getDataTypes()) {
			int required = set.getInferenceTest().getMinimumForType(type);
			// If we require some, but none exist return false
			if (required > 0 && !sensorsPerType.containsKey(type)) {
				return "";
			} 
			if(required == 0 && !sensorsPerType.containsKey(type)) {
				continue; // We don't require any of this type, that is fine...
			}
			// If we require more then exist, return false
			if (sensorsPerType.get(type) < required) {
				return "";
			}

		}
		return (sensorsPerType.toString()); // We have enough of each type of sensor
	}


	/**					**\
	 * Extended classes	 *
	 * should override	 *
	\* these methods	 */
	@Override
	public InferenceResult inference(ExtendedConfiguration configuration, ScenarioSet set, Scenario scenario) {
		Constants.log(Level.WARNING, "Function: inference - no child inference model defined",  null);
		return null;
	}

	@Override
	public boolean mutate(ExtendedConfiguration configruation, ScenarioSet set) {
		if(mutate.equals(MUTATE.SENSOR)) {
			return configruation.mutateSensor(set, modelOption);
		} else if(mutate.equals(MUTATE.WELL)) {
			return configruation.mutateWell(set);
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

	public void setDomainViewer(MultiDomainViewer viewer) {
		this.viewer = viewer;
	}
	
	public void setMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}


	/**					**\
	 * Helper Methods	 *
	\* 					 */

	protected synchronized void storeHistory(Scenario scenario, Integer nodeNumber, Float timeStep, String dataType, Boolean triggered) {

		if(!Constants.hdf5Data.isEmpty())
			return; // Don't need this
		
		if(history == null)
			history = Collections.synchronizedMap(new HashMap<Scenario, Map<Integer, Map<Float, Map<String, Boolean>>>>());

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


	protected synchronized Boolean getHistory(Scenario scenario, Integer nodeNumber, Float timeStep, String dataType) {

		if(!Constants.hdf5Data.isEmpty())
			return null; // Don't need this
		
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
