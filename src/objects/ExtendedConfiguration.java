package objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import utilities.Constants;
import utilities.Point3f;
import utilities.Point3i;

/**
 * Extension of the configuration class that provides much more functionality and information
 * @author port091
 * @author rodr144
 * @author huan482
 */

public class ExtendedConfiguration extends Configuration {
	private static final int WEIGHT_OF_ADD_SENSOR = 6;
	
	private static final int NUMBER_OF_MUTATIONS = 6;
	// Cost of configuration
	private float configCost = 0;
	
	// List of wells
	private List<Well> wells;

	// Weighted with penalty for scenarios that do not detect
	private Map<String, Float> objectiveValues;
	
	private Map<String, InferenceResult> inferenceResults;
	
	private List<Integer> pickRand = new ArrayList<Integer>();
	
	public ExtendedConfiguration() {
		this(false);
	}

	public ExtendedConfiguration(boolean copy) {
		
		wells = Collections.synchronizedList(new ArrayList<Well>());
		sensors = Collections.synchronizedList(new ArrayList<Sensor>());
		timesToDetection = Collections.synchronizedMap(new HashMap<String, Float>());
		objectiveValues = Collections.synchronizedMap(new HashMap<String, Float>());		
		inferenceResults = Collections.synchronizedMap(new HashMap<String, InferenceResult>());
		clearAndRepopulateArray();
		if(!copy) {
			Constants.log(Level.INFO, "Sensor configuration: initialized", null);
			Constants.log(Level.CONFIG, "Sensor configuration: configuration", this);
		} else {
			// Could log a message saying we're making a copy?
		}
	}	

	public synchronized void matchConfiguration(ExtendedConfiguration toDuplicate) {

		if(!toDuplicate.wells.isEmpty() && toDuplicate.wells.get(0) instanceof RealizedWell) {
			this.sensors.clear();
			for(Well well: toDuplicate.wells) {
				addRealizedWell((RealizedWell)well);
			}
		} else {
			this.sensors.clear();
			for(ExtendedSensor sensor: toDuplicate.getExtendedSensors()) {
				this.addSensor(sensor.makeCopy());
			}
		}

		timesToDetection.clear();
		objectiveValues.clear();
		inferenceResults.clear();

		if(toDuplicate.getTimesToDetection() != null) {
			for(String key: toDuplicate.getTimesToDetection().keySet()) {
				addTimeToDetection(key, toDuplicate.getTimesToDetection().get(key));
			}
		}

		if(toDuplicate.getTimesToDetection() != null) {
			for(String key: toDuplicate.objectiveValues.keySet()) {
				addObjectiveValue(key, toDuplicate.objectiveValues.get(key));
			}
		}

		if(toDuplicate.inferenceResults != null) {		
			for(String key: toDuplicate.inferenceResults.keySet()) {
				addInferenceResult(key, toDuplicate.inferenceResults.get(key));
			}
		}
	}


	public synchronized ExtendedConfiguration makeCopy() {

		ExtendedConfiguration copy = new ExtendedConfiguration(true);

		// Copy sensors, this will add the wells
		for(ExtendedSensor sensor: getExtendedSensors()) {
			ExtendedSensor sensorCopy = sensor.makeCopy();
			copy.addSensor(sensorCopy);
		}
		if(!wells.isEmpty() && wells.get(0) instanceof RealizedWell) {
			for(Well well: wells) {
				copy.addRealizedWell((RealizedWell)well);
			}
		} else {
			for(ExtendedSensor sensor: getExtendedSensors()) {
				ExtendedSensor sensorCopy = sensor.makeCopy();
				copy.addSensor(sensorCopy);
			}
		}
		
		copy.timesToDetection = new HashMap<String, Float>(timesToDetection);
		copy.objectiveValues = new HashMap<String, Float>(objectiveValues);
		copy.inferenceResults = new HashMap<String, InferenceResult>(inferenceResults);
		
		return copy;		
	}


	@Override
	public String toString() {
		StringBuilder toString = new StringBuilder();
		toString.append("Configuration: ");
		if(wells.isEmpty()) {
			toString.append("Empty\n");
			return toString.toString();
		}

		toString.append("\nWells:\n");
		for(Well well: wells) {
			toString.append("\n\tWell at " + well.toString() + "\n");
			for(ExtendedSensor sensor: well.getSensors()) {
				toString.append("\t\t" + sensor.toString() + "\n");
			}
		}

		toString.append("\n");
		
		Map<String, String> sorted = new TreeMap<String, String>();
		for(String key: getTimesToDetection().keySet()) {
			sorted.put(key, key);
		}

		for(String scenario: sorted.keySet()) {
			float ttd = getTimesToDetection().get(sorted.get(scenario));
			String ttdStr = ttd > 10000 ? Constants.exponentialFormat.format(ttd) : Constants.decimalFormat.format(ttd);
			toString.append("\tTime to detection for " + sorted.get(scenario).toString() + ": " + ttdStr + "\n");
		}
		
		sorted.clear();
		for(String key: objectiveValues.keySet()) {
			sorted.put(key, key);
		}

		for(String scenario: sorted.keySet()) {
			float obj = objectiveValues.get(sorted.get(scenario));
			String objStr = obj > 10000 ? Constants.exponentialFormat.format(obj) : Constants.decimalFormat.format(obj);			
			toString.append("\tObjective value for " + sorted.get(scenario).toString() + ": " + objStr + "\n");
		}

		sorted.clear();
		for(String key: inferenceResults.keySet()) {
			sorted.put(key, key);
		}
		
		for(String scenario: sorted.keySet()) {

			toString.append("\tInference result for " + sorted.get(scenario).toString() + ": " + inferenceResults.get(sorted.get(scenario)).toString() + "\n");
		}

		return toString.toString();
	}

	public List<ExtendedSensor> getExtendedSensors() {
		ArrayList<ExtendedSensor> copyOfSensors = new ArrayList<ExtendedSensor>();
		for(Sensor sensor: sensors) {
			if(sensor instanceof ExtendedSensor)
			copyOfSensors.add((ExtendedSensor)sensor);
		}
		return copyOfSensors;
	}

	public List<Well> getWells() {
		return wells;
	}

	public synchronized void addObjectiveValue(String scenario, float timeInYears) {
		objectiveValues.put(scenario, timeInYears);
	}

	public synchronized void addTimeToDetection(String scenario, float timeToDetection) {
		timesToDetection.put(scenario, timeToDetection);
	}

	public synchronized void addInferenceResult(String scenario, InferenceResult inferenceResult) {
		inferenceResults.put(scenario, inferenceResult);
	}
	
	
	/**
	 * Returns the average objective value of all scenarios
	 * 
	 * A penalty has been applied for scenarios that do not detect
	 * 
	 * This value has been weighted
	 * 
	 */
	public synchronized float getObjectiveValue() {
		float sum = 0;
		for(String scenario: getObjectiveValues().keySet()) {
			sum += getObjectiveValues().get(scenario); // No weights here
		}
		return sum;
	}
	
	
	/**
	 * Returns the average time to detection of the triggering scenarios
	 * 
	 * This value has been normalized against all detecting scenarios
	 * 
	 */
	public synchronized float getNormalizedAverageTimeToDetection(Map<String, Float> scenarioWeights) {	
		float sum = 0;
		float totalWeight = 0;
		for(String scenario: getTimesToDetection().keySet()) {
			sum += getTimesToDetection().get(scenario) * scenarioWeights.get(scenario);
			totalWeight += scenarioWeights.get(scenario);
		}
		return sum / totalWeight;
	}
	
	/**
	 * Returns the average time to detection of the triggering scenarios
	 * 
	 * This value has been normalized against all detecting scenarios
	 * 
	 */
	public synchronized float getNormalizedPercentScenariosDetected(Map<String, Float> scenarioWeights, float totalScenarioWeights) {
		float detectedWeight = 0;
		for(String scenario: getTimesToDetection().keySet()) {
			detectedWeight += scenarioWeights.get(scenario);
		}
		return detectedWeight / totalScenarioWeights;
	}
	
	
	public synchronized List<Integer> getSensorPositions(String type) {
		List<Integer> positions = new ArrayList<Integer>();
		for(Sensor sensor: sensors) {
			if(sensor.getSensorType().equals(type))
				positions.add(sensor.getNodeNumber());
		}
		return positions;
	}
	/*
	 * Will return a list of sensor positions excluding the sensor passed in
	 */
	public synchronized List<Integer> getSensorPositions(ExtendedSensor current) {
		List<Integer> positions = new ArrayList<Integer>();
		for(Sensor sensor: sensors) {
			if(sensor.equals(current))	// Skip this one
				continue;
			if(sensor.getSensorType().equals(current.getSensorType()))
				positions.add(sensor.getNodeNumber());
		}
		return positions;
	}
	
	
	public String getSummary(NodeStructure nodeStructure) {
		StringBuffer nodePositions = new StringBuffer();
		List<String> ijs = new ArrayList<String>();
		if(sensors.isEmpty())
			return "Empty";
		for(Sensor sensor: sensors) {
			String IJ = sensor.getIJK().getI() + "_" + sensor.getIJK().getJ();
			if(!ijs.contains(IJ))
				ijs.add(IJ);
			Point3f xyz = nodeStructure.getXYZEdgeFromIJK(sensor.getIJK());
			if(sensor.getSensorType().contains("Electrical Conductivity")) {
				Point3f xyzPair = nodeStructure.getXYZEdgeFromIJK(nodeStructure.getIJKFromNodeNumber(((ExtendedSensor)sensor).getNodePairNumber()));
				nodePositions.append(", " + sensor.getSensorType() + " (" + Constants.decimalFormat.format(xyz.getX()) + " " + Constants.decimalFormat.format(xyz.getY()));
				nodePositions.append(") (" + Constants.decimalFormat.format(xyzPair.getX()) + " " + Constants.decimalFormat.format(xyzPair.getY()) + ")");
				Point3i nodePair = nodeStructure.getIJKFromNodeNumber(((ExtendedSensor)sensor).getNodePairNumber());
				String IJPair = nodePair.getI() + "_" + nodePair.getJ();
				if(!ijs.contains(IJPair))
					ijs.add(IJPair);
			} else {
				nodePositions.append(", " + sensor.getSensorType() + " (" + Constants.decimalFormat.format(xyz.getX()) + " ");
				nodePositions.append(Constants.decimalFormat.format(xyz.getY()) + " " + Constants.decimalFormat.format(xyz.getZ()) + ")");
			}
		}
		String nodes = ijs.size() + " wells" + nodePositions.toString();
		return nodes;
	}

	public String getInferenceResults() {
		StringBuffer toString = new StringBuffer();
		for(String key: inferenceResults.keySet()) {
			toString.append("\tInference result for " + key.toString() + ": " + inferenceResults.get(key).toString() + "\n");
		}
		return toString.toString();
	}
	
	public Map<String, Float> getTimesToDetection() {
		return timesToDetection;
	}

	public Map<String, Float> getObjectiveValues() {
		return objectiveValues;
	}

	public void setTimesToDetection(Map<String, Float> timesToDetection) {
		this.timesToDetection = timesToDetection;
	}
	
	public float getConfigCost() {
		return configCost;
	}
	
	public void setConfigCost(float configCost) {
		this.configCost = configCost;
	}
	
	/*******************************************************************************************
	 * Helper Methods for Move Logic
	 * **************************************/
	
	public synchronized void addSensor(ExtendedSensor sensor) {

		if(!sensors.contains(sensor))
			sensors.add(sensor);
		updateWells();

	}
	
	public synchronized void removeSensor() {
		int index = Constants.random.nextInt(sensors.size());
		try {
			sensors.remove(index);
		} catch (Exception theException){
			theException.printStackTrace();
		}
		updateWells();
	}
	
	private synchronized void updateWells() {
		for(Sensor sensor: sensors) {
			if(sensor instanceof ExtendedSensor)
				((ExtendedSensor)sensor).setWell(null);
		}

		wells.clear();

		for(Sensor sensor: sensors)  {
			if(!(sensor instanceof ExtendedSensor))
				continue;			
			ExtendedSensor extendedSensor = (ExtendedSensor)sensor;
			boolean foundWell = false;
			for(Well well: wells){
				if(well.isAt(extendedSensor)) {
					extendedSensor.setWell(well);
					well.addSensor(extendedSensor);
					foundWell = true; // Found a well at the sensors new position
				}
			}
			if(!foundWell) {
				Well newWell = new Well(extendedSensor.getIJK().getI(), extendedSensor.getIJK().getJ());
				newWell.addSensor(extendedSensor);
				extendedSensor.setWell(newWell);
				wells.add(newWell);
			}
		}
		
	}

	public synchronized boolean mutateWell(ScenarioSet set) {

		Constants.log(Level.FINER, "Sensor configuration: mutating wells", null);

		Object addedWell = addRealizedWell(set);
		if(addedWell  != null) {
			Constants.log(Level.FINER, "Sensor configuration: mutated, ADDED REALIZED WELL", addedWell);
			return true;
		}
		Object movedRealizedWellInBounds = moveRealizedWellInBounds(set);
		if(movedRealizedWellInBounds != null) {
			Constants.log(Level.FINER, "Sensor configuration: mutated, MOVED REALIZED WELL IN BOUNDS", movedRealizedWellInBounds);
			return true;
		}
		Object movedRealizedWell = moveRealizedWell(set);
		if(movedRealizedWell != null) {
			Constants.log(Level.FINER, "Sensor configuration: mutated, MOVED REALIZED WELL", movedRealizedWell);
			return true;
		}

		return false;
	}
//	public synchronized boolean mutateSensor(ScenarioSet scenarioSet) {	
////while (!pickRand.isEmpty()) {
////	int index = Constants.random.nextInt(pickRand.size());
////	int num = pickRand.get(index);
//	
//	// If we can afford a new sensor add it at the add point:
//	Constants.log(Level.FINER, "Sensor configuration: mutating sensors", null);
//	//	boolean debug = true;
//	
////	if (num <= WEIGHT_OF_ADD_SENSOR) {
//		Object addedSensor = addSensor(scenarioSet);
////		for (int i = WEIGHT_OF_ADD_SENSOR; i >= 0; i--) {
////			pickRand.remove(i);
////		}
//		if(addedSensor != null) {
//			Constants.log(Level.FINER, "Sensor configuration: mutated, ADDED SENSOR", addedSensor);
////			clearAndRepopulateArray();
//			return true;
//		}
////	}
//		
//		
////	if (num == WEIGHT_OF_ADD_SENSOR + 1) {
//		//Have to skip this if we're running as one sensor, this is pretty much guaranteed to have out of bounds sensors we don't want to move (outside of their clouds)
//		Object movedSensorInBounds = moveSensorInBounds(scenarioSet);
////		pickRand.remove(index);
//		if(movedSensorInBounds != null) {
////			clearAndRepopulateArray();
//			Constants.log(Level.FINER, "Sensor configuration: mutated, MOVED SENSOR IN BOUNDS", movedSensorInBounds);
//			return true;
//		}
////	}
//	
////	if (num == WEIGHT_OF_ADD_SENSOR + 2) {
//		Object movedSensor = moveSensor(scenarioSet);
////		pickRand.remove(index);
//		if(movedSensor != null) {
////			clearAndRepopulateArray();
//			Constants.log(Level.FINER, "Sensor configuration: mutated, MOVED SENSOR", movedSensor);
//			return true;
//		}
////	}	
//		Object removedSensor = removeSensor(scenarioSet);
//		if (removedSensor != null) {
//			Constants.log(Level.FINER, "Sensor configuratino: Mutated, REMOVED SENSOR", removedSensor);
//			return true;
//		}
//	
////	if (num == WEIGHT_OF_ADD_SENSOR + 3) {
//		// Prioritize shuffling a well
//		Object shuffledWell = shuffleWell(scenarioSet);
////		pickRand.remove(index);
//		if(shuffledWell != null) {
////			clearAndRepopulateArray();
//			Constants.log(Level.FINER, "Sensor configuration: mutated, SHUFFLED WELL", shuffledWell);
//			return true;
//		}
////	}
//	
////	if (num == WEIGHT_OF_ADD_SENSOR + 4) {
//		Object movedWell = moveWell(scenarioSet);
////		pickRand.remove(index);
//		if(movedWell != null) {
////			clearAndRepopulateArray();
//			Constants.log(Level.FINER, "Sensor configuration: mutated, MOVED WELL", movedWell);
//			return true;
//		}
////	}
//	Constants.log(Level.WARNING, "Sensor configuration: couldn't mutate", null);
////}
////clearAndRepopulateArray();
//return false;
//}
	
	
	public synchronized boolean mutateSensor(final ScenarioSet scenarioSet) {	
	while (!pickRand.isEmpty()) {
		int index = Constants.random.nextInt(pickRand.size());
		int num = pickRand.get(index);
		
		// If we can afford a new sensor add it at the add point:
		Constants.log(Level.FINER, "Sensor configuration: mutating sensors", null);
		//	boolean debug = true;
		
		if (num <= WEIGHT_OF_ADD_SENSOR) {
			Object addedSensor = addSensor(scenarioSet);
			for (int i = WEIGHT_OF_ADD_SENSOR; i >= 0; i--) {
				pickRand.remove(i);
			}
			if(addedSensor != null) {
				Constants.log(Level.FINER, "Sensor configuration: mutated, ADDED SENSOR", addedSensor);
				clearAndRepopulateArray();
				return true;
			}
		}
		
		if (num == WEIGHT_OF_ADD_SENSOR + 1) {
			//Have to skip this if we're running as one sensor, this is pretty much guaranteed to have out of bounds sensors we don't want to move (outside of their clouds)
			Object movedSensorInBounds = moveSensorInBounds(scenarioSet);
			pickRand.remove(index);
			if(movedSensorInBounds != null) {
				clearAndRepopulateArray();
				Constants.log(Level.FINER, "Sensor configuration: mutated, MOVED SENSOR IN BOUNDS", movedSensorInBounds);
				return true;
			}
		}
		
		if (num == WEIGHT_OF_ADD_SENSOR + 2) {
			Object movedSensor = moveSensor(scenarioSet);
			pickRand.remove(index);
			if(movedSensor != null) {
				clearAndRepopulateArray();
				Constants.log(Level.FINER, "Sensor configuration: mutated, MOVED SENSOR", movedSensor);
				return true;
			}
		}
		
		if (num == WEIGHT_OF_ADD_SENSOR + 3) {
			// Prioritize shuffling a well
			Object shuffledWell = shuffleWell(scenarioSet);
			pickRand.remove(index);
			if(shuffledWell != null) {
				clearAndRepopulateArray();
				Constants.log(Level.FINER, "Sensor configuration: mutated, SHUFFLED WELL", shuffledWell);
				return true;
			}
		}
		
		if (num == WEIGHT_OF_ADD_SENSOR + 4) {
			Object movedWell = moveWell(scenarioSet);
			pickRand.remove(index);
			if(movedWell != null) {
				clearAndRepopulateArray();
				Constants.log(Level.FINER, "Sensor configuration: mutated, MOVED WELL", movedWell);
				return true;
			}
		}
		
		if (num == WEIGHT_OF_ADD_SENSOR + 5) {
			try {
				removeSensor();
				clearAndRepopulateArray();
				Constants.log(Level.FINER, "Sensor configuration: mutated, REMOVED SENSOR", scenarioSet);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		Constants.log(Level.WARNING, "Sensor configuration: couldn't mutate", null);
	}
	clearAndRepopulateArray();
	return false;
}
	
	

	public boolean mutateSensorToEdgeOnly(final ScenarioSet set) {	
		boolean sensorMutate = mutateSensor(set);
		return sensorMutate;
	}

	private Object addRealizedWell(final ScenarioSet scenarioSet) {

		// Add well at add point
		if(wells.size() < scenarioSet.getMaxWells()) {
			RealizedWell well = new RealizedWell(scenarioSet.getAddPoint().getI(), scenarioSet.getAddPoint().getJ(), scenarioSet);
			addRealizedWell(well);
			return well;
		}
		// Too many wells
		return null;
	}

	private Object moveRealizedWellInBounds(final ScenarioSet set) {
		List<Well> outOfBoundsWells = new ArrayList<Well>();
		for(Well well: wells) {
			if(well instanceof RealizedWell) {
				if(((RealizedWell)well).isOutOfBounds(set)) {
					outOfBoundsWells.add(well);
				}
			}
		}
		return moveRealizedWell(outOfBoundsWells, set); // Any well can move
	}

	private Object moveRealizedWell(final ScenarioSet scenarioSet) {
		return moveRealizedWell(wells, scenarioSet); // Any well can move
	}
	
//	private Object removeSensor(final ScenarioSet scenarioSet) {
//		int removePoint = scenarioSet.getNodeStructure().getNodeNumber(scenarioSet.getAddPoint());
//		Map<String, List<Integer>> removeableSensors = new HashMap<String, List<Integer>>();
//		List<String> types = new ArrayList<String>();
//		boolean atRemovePoint = false;
//		for (String type: scenarioSet.getDataTypes()) {
//			List<Integer> validNodesToRemove = scenarioSet.getValidNodes(type, this, false, false, false);
//			if (!validNodesToRemove.isEmpty()) {
//				removeableSensors.put(type, validNodesToRemove);
//				types.add(type);
//				if (validNodesToRemove.contains(removePoint)) {
//					atRemovePoint = true;
//				}
//			}
//		}
//		if  (removeableSensors.isEmpty()) {
//			return null;
//		}
//		String type = types.get(Constants.random.nextInt(types.size()));
//		if (atRemovePoint) {
//			ExtendedSensor toRemove = new ExtendedSensor(removePoint, type, scenarioSet.getNodeStructure());
//			removeSensor(toRemove);
//			return toRemove;
//		} else {
//			int index = Constants.random.nextInt(removeableSensors.get(type).size());
//			ExtendedSensor toRemove = new ExtendedSensor(removeableSensors.get(type).get(index),
//					type, scenarioSet.getNodeStructure());
//			removeSensor(toRemove);
//			return toRemove;
//		}
//	}
	
//	private Object removeSensor(final ScenarioSet scenarioSet) {
//		try {
//			removeSensor();
//		} catch (Exception e) {
//			e.printStackTrace();
//			System.out.println("No Sensor to remove");
//		}
//		return scenarioSet;
//	}
	
	private Object addSensor(final ScenarioSet scenarioSet) {
		// Timer
		// long startTime = System.currentTimeMillis();

		// We will try to add here first.
		int addPoint = scenarioSet.getNodeStructure().getNodeNumber(scenarioSet.getAddPoint());
		Map<String, List<Integer>> affordableSensors = new HashMap<String, List<Integer>>();
		List<String> types = new ArrayList<String>();
		boolean atAddPoint = false;
		for(String type: scenarioSet.getDataTypes()) {
			List<Integer> validNodes = scenarioSet.getValidNodes(type, this, true, true, true);
			if(!validNodes.isEmpty()) {
//				affordableSensors.put(type, scenarioSet.getValidNodes(type, this, true, true, true));
				affordableSensors.put(type, validNodes);
				types.add(type);
				if(validNodes.contains(addPoint)) {
					atAddPoint = true; // We can add a sensor at the add point
				}
			}
		}
		if(affordableSensors.isEmpty()) {
			// System.out.println("Could not add sensor, time taken: " + (System.currentTimeMillis()-startTime));
			return null;
		}

		String type = types.get(Constants.random.nextInt(types.size()));
		if(atAddPoint) {
			ExtendedSensor toAdd = new ExtendedSensor(addPoint, type, scenarioSet.getNodeStructure());
			addSensor(toAdd);
			return toAdd;
		} else {
			int index = Constants.random.nextInt(affordableSensors.get(type).size());
			ExtendedSensor toAdd = new ExtendedSensor(affordableSensors.get(type).get(index), type, scenarioSet.getNodeStructure());
			addSensor(toAdd);
			return toAdd;
		}

		// System.out.println("Added sensor, time taken: " + (System.currentTimeMillis()-startTime));

	}
	
	private Object moveSensorInBounds(final ScenarioSet scenarioSet) {
		// First see if have any out of bounds sensors
		List<Sensor> outOfBounds = new ArrayList<Sensor>();
		for(ExtendedSensor sensor: getExtendedSensors()) {
			// The current sensor is not in the cloud, or there are multiple sensors at that location
			if(!sensor.isInCloud(scenarioSet) || getSensorPositions(sensor).contains(sensor.getNodeNumber()))
				outOfBounds.add(sensor);
		}

		return moveSensor(outOfBounds, scenarioSet);

	}

	private Object moveSensor(final ScenarioSet scenarioSet) {
		return moveSensor(sensors, scenarioSet);	// Otherwise just move a random one
	}

	/*private Object moveAllSensor(ScenarioSet scenarioSet) {
		return moveAllSensor(sensors, scenarioSet);	// Otherwise just move a random one
	}*/
	
	private Object moveWell(final ScenarioSet scenarioSet) {
		if(wells.size() == 0)
			return null; // No wells to move
		// Otherwise randomize and try to move the well
		List<Well> wells = getWells();
		Collections.shuffle(wells, Constants.random);
		for(Well well: wells) {
			if(well.move(this, scenarioSet)) {
				updateWells();
				return well;
			}
		}
		return null;
	}

	private Object shuffleWell(final ScenarioSet scenarioSet) {
		if(wells.size() == 0)
			return null; // No wells to move

		// Otherwise randomize and try to move the well
		List<Well> wells = getWells();
		Collections.shuffle(wells, Constants.random);
		for(Well well: wells) {
			if(well.shuffle(this, scenarioSet)) {
				updateWells();
				return well;
			}
		}


		return null;
	}

	// level 2
	private Object moveSensor(final List<Sensor> sensors, final ScenarioSet scenarioSet) {
		// Randomize the list
		Collections.shuffle(sensors, Constants.random);
		for(Sensor sensor: sensors) {
			if(!(sensor instanceof ExtendedSensor)) 
				continue;
			ExtendedSensor extendedSensor = (ExtendedSensor)sensor;
			if(extendedSensor.move(this, scenarioSet)) {
				addSensor(extendedSensor);	
				return extendedSensor; // We were able to move an out of bounds sensor into the cloud
			}
		}
		return null;
	}
	
	/*private Object moveAllSensor(List<Sensor> sensors, ScenarioSet scenarioSet) {
		// Randomize the list
		Collections.shuffle(sensors, Constants.random);
		//Will break if not extended sensors
		ExtendedSensor sensor1 = (ExtendedSensor) sensors.get(0);
		Integer nodeToMove = sensor1.getNodeNumber();
		ArrayList<ExtendedSensor> sensorsToMove = new ArrayList<ExtendedSensor>();
		for(Sensor sensor: sensors) {
			ExtendedSensor extendedSensor = (ExtendedSensor)sensor;
			if(sensor.getNodeNumber().equals(nodeToMove)) sensorsToMove.add(extendedSensor);
		}
		if(ExtendedSensor.move(sensorsToMove, this, scenarioSet)){
			for(ExtendedSensor sensor: sensorsToMove){
				addSensor(scenarioSet, sensor);
			}
			return new Object();
		}
		return null;
	}*/

	private Object moveRealizedWell(final List<Well> wellsToMove, final ScenarioSet scenarioSet) {
		// Randomize the list
		Collections.shuffle(wellsToMove, Constants.random);
		for(Well well: wellsToMove) {
			if(well.move(this, scenarioSet)) {
				return well;
			}
		}
		return null;
	}

	private void addRealizedWell(final RealizedWell realizedWell) {
		for(ExtendedSensor sensor: realizedWell.getSensors()) {
			sensors.add(sensor);
		}
		wells.add(realizedWell);
	}
	
	public boolean checkForMatch(final ExtendedConfiguration configuration2) {
		if(this.sensors.size() != configuration2.sensors.size())
			return false;
		for(int i=0; i<this.sensors.size(); i++) {
			if(this.sensors.get(i).getSensorType() != configuration2.sensors.get(i).getSensorType())
				return false;
			if(this.sensors.get(i).getNodeNumber().intValue() != configuration2.sensors.get(i).getNodeNumber().intValue())
				return false;
			if(this.getExtendedSensors().get(i).getNodePairNumber() != configuration2.getExtendedSensors().get(i).getNodePairNumber())
				return false;
		}
		return true;
	}
	
	public void orderSensors() {
		
	    Collections.sort(sensors, new Comparator<Sensor>() {
	    	
	        public int compare(Sensor sensor1, Sensor sensor2) {
	        	
	        	String s1 = sensor1.getSensorType();
				String s2 = sensor2.getSensorType();
				int sensorNameComp = s1.compareTo(s2);
				
				if(sensorNameComp != 0)
					return sensorNameComp;
				
				Integer i1 = sensor1.getNodeNumber();
				Integer i2 = sensor2.getNodeNumber();
				return i1.compareTo(i2);
	        }
	    });
	}
	
	private void clearAndRepopulateArray() {
		pickRand.clear();
		for (int i = 0; i < WEIGHT_OF_ADD_SENSOR + NUMBER_OF_MUTATIONS; i++) {
			pickRand.add(i);
		}
	}
}
