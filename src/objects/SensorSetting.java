package objects;

import hdf5Tool.HDF5Interface;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;

import functions.SimulatedAnnealing;
import utilities.Constants;
import utilities.Point3f;

/**
 * Holds the logic for a specific sensor type and threshold
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class SensorSetting {
	
	public static HashMap<String, String> sensorTypeToDataType;
	
	public enum Trigger {
		
		MAXIMUM_THRESHOLD("Maximum threshold"), MINIMUM_THRESHOLD("Minimum threshold"), 
		RELATIVE_DELTA("Relative delta"), ABSOLUTE_DELTA("Absolute delta");
		
		private String trigger;
		
		private Trigger(String trigger) {
			this.trigger = trigger;
		}
		
		@Override
		public String toString() {
			return trigger;
		}
	}
	
	public enum DeltaType {
		
		INCREASE("Delta Increase"),
		DECREASE("Delta Decrease"), 
		BOTH("Delta Both");
		
		private String type;
		
		private DeltaType(String type) {
			this.type = type;
		}
		
		@Override
		public String toString() {
			return type;
		}
	}
	
	private String type;
	private float cost;

	private Float minValue;
	private Float maxValue;
	private float minZ;
	private float maxZ;

	private Trigger trigger;
	private DeltaType deltaType;

	private float lowerThreshold;
	private float upperThreshold;

	private HashSet<Integer> validNodes; //Pareto optimal locations (if set in Constants class)
	private HashSet<Integer> fullCloudNodes; //Full set of allowed locations
	private static Map<Scenario, HashMap<Float, Float>> volumeDegradedByYear;
	private static List<Float> years;
	public static float globalMinZ;
	public static float globalMaxZ;

	private Color color;

	private boolean isReady;
	private boolean nodesReady;

	private List<Scenario> scenarios; // If this changes these will need to be updated
	public static NodeStructure nodeStructure;
	
	ScenarioSet scenarioSet;
	
	public SensorSetting(NodeStructure nodeStructure, ScenarioSet scenarioSet, String type, List<Scenario> scenarios) {

		this.nodeStructure = nodeStructure;
		this.scenarioSet = scenarioSet;
		this.scenarios = scenarios;
		this.type = type;
		this.cost = 100;

		this.minValue = HDF5Interface.queryStatistic(type, 0); //Global minimum value
		this.maxValue = HDF5Interface.queryStatistic(type, 2); //Global maximum value
		this.trigger = Trigger.MAXIMUM_THRESHOLD;
		this.setDeltaType(DeltaType.BOTH);
		this.lowerThreshold = 0; //Based on the trigger, detection value, and leakage value, this represents the range for valid nodes
		this.upperThreshold = 0; //Based on the trigger, detection value, and leakage value, this represents the range for valid nodes
		this.setGlobalMaxZ(Collections.max(this.nodeStructure.getZ()));
		this.setGlobalMinZ(Collections.min(this.nodeStructure.getZ()));
		
		this.validNodes = new HashSet<Integer>(); //None yet
		this.color = Color.GREEN;	

		this.isReady = false;
		this.nodesReady = false;

		Constants.log(Level.INFO, "Sensor settings "+type+": initialized ", null);
		Constants.log(Level.CONFIG, "Sensor settings "+type+": configuration", this);

	}
	
	public SensorSetting(NodeStructure nodeStructure, ScenarioSet scenarioSet, String type, List<Scenario> scenarios, float minValue, float maxValue) {

		this.nodeStructure = nodeStructure;
		this.scenarioSet = scenarioSet;
		this.scenarios = scenarios;
		this.type = type;
		this.cost = 100;

		this.minValue = minValue; //Global minimum value
		this.maxValue = maxValue; //Global maximum value
		this.trigger = Trigger.MAXIMUM_THRESHOLD;
		this.setDeltaType(DeltaType.BOTH);
		this.lowerThreshold = 0; //Based on the trigger, detection value, and leakage value, this represents the range for valid nodes
		this.upperThreshold = 0; //Based on the trigger, detection value, and leakage value, this represents the range for valid nodes
		this.setGlobalMaxZ(Collections.max(this.nodeStructure.getZ()));
		this.setGlobalMinZ(Collections.min(this.nodeStructure.getZ()));
		
		this.validNodes = new HashSet<Integer>(); //None yet
		this.color = Color.GREEN;	

		this.isReady = false;
		this.nodesReady = false;

		Constants.log(Level.INFO, "Sensor settings "+type+": initialized ", null);
		Constants.log(Level.CONFIG, "Sensor settings "+type+": configuration", this);

	}

	public static void setVolumeDegradedByYear(Map<Scenario, HashMap<Float, Float>> volumeDegradedByYear2, ArrayList<Float> yearList){
		years = yearList;
		volumeDegradedByYear = volumeDegradedByYear2;
	}
	
	public static Map<Scenario, Float> getVolumesDegraded(Map<Scenario, Float> ttdMap){
		HashMap<Scenario, Float> vadMap = new HashMap<Scenario, Float>();
		for(Scenario scenario: ttdMap.keySet()){
			if(volumeDegradedByYear.containsKey(scenario)){
				int i=1;
				while(i <= years.size()){
					if(i == years.size()) vadMap.put(scenario, volumeDegradedByYear.get(scenario).get(years.get(i-1)));
					else if(ttdMap.get(scenario) <= years.get(i)){
						vadMap.put(scenario, volumeDegradedByYear.get(scenario).get(years.get(i)));
						break;
					}
					i++;
				}
			}
			else{
				vadMap.put(scenario, 0f);
				System.out.println("Wasn't sure I would hit this - VAD for printing");
			}
		}
		return vadMap;
	}
	
	public static float getVolumeDegradedByTTDs(Map<Scenario, Float> ttdMap, int numScenarios){ //TODO: might want to make this take weighted averages.
		float volume = 0;
		//Note that this only loops over scenarios in which some volume of aquifer is degraded
		for(Scenario scenario: volumeDegradedByYear.keySet()){
			if(!ttdMap.containsKey(scenario) && years.size() != 0){
				volume += volumeDegradedByYear.get(scenario).get(years.get(years.size()-1));
				continue;
			}
			int i=1;
			while(i <= years.size()){
				if(i == years.size()) volume += volumeDegradedByYear.get(scenario).get(years.get(i-1)); // I don't think this should ever happen, but just making sure.
				else if(ttdMap.get(scenario) <= years.get(i)){
					volume += volumeDegradedByYear.get(scenario).get(years.get(i));
					break;
				}
				i++;
			}
		}
		return volume/numScenarios;
	}
	
	public static HashMap<Float,Float> getAverageVolumeDegradedAtTimesteps(){
		HashMap<Float,Float> averageVADMap = new HashMap<Float,Float>();
		int numScenarios = volumeDegradedByYear.keySet().size();
		for(Float year: years){
			float totalVAD = 0;
			for(Scenario scenario: volumeDegradedByYear.keySet()){
				totalVAD += volumeDegradedByYear.get(scenario).get(year);
			}
			averageVADMap.put(year, totalVAD/numScenarios);
		}
		return averageVADMap;
	}
	
	public static HashMap<Float,Float> getMaxVolumeDegradedAtTimesteps(){
		HashMap<Float,Float> maxVADMap = new HashMap<Float,Float>();
		for(Float year: years){
			float maxVAD = 0;
			for(Scenario scenario: volumeDegradedByYear.keySet()){
				maxVAD = Math.max(maxVAD, volumeDegradedByYear.get(scenario).get(year));
			}
			maxVADMap.put(year, maxVAD);
		}
		return maxVADMap;
	}
	
	public static HashMap<Float,Float> getMinVolumeDegradedAtTimesteps(){
		HashMap<Float,Float> minVADMap = new HashMap<Float,Float>();
		for(Float year: years){
			float minVAD = Float.MAX_VALUE;
			for(Scenario scenario: volumeDegradedByYear.keySet()){
				minVAD = Math.min(minVAD, volumeDegradedByYear.get(scenario).get(year));
			}
			minVADMap.put(year, minVAD);
		}
		return minVADMap;
	}

	public void setUserSettings(float cost, Color color, float lowerThreshold, float upperThreshold, Trigger trigger, boolean reset,
			DeltaType deltaType, float minZ, float maxZ) {

		Constants.log(Level.INFO, "Sensor settings "+type+": setting user settings", null);
		
		boolean changeOccured = reset; // Don't re-query if we don't need to!

		float realMaxZ = Math.max(minZ, maxZ);
		float realMinZ = Math.min(minZ, maxZ);
		
		if(Float.compare(cost, this.cost) != 0) {
			this.cost = cost;
			changeOccured = true;
		}
	
		if(!this.color.equals(color)) {
			this.color = color;
			changeOccured = true;
		}			

		if(Float.compare(this.lowerThreshold, lowerThreshold) != 0) {
			this.lowerThreshold = lowerThreshold;
			changeOccured = true;
		}
		
		if(Float.compare(this.upperThreshold, upperThreshold) != 0) {
			this.upperThreshold = upperThreshold;
			changeOccured = true;
		}
		
		if(this.getTrigger() != trigger) {
			this.trigger = trigger;
			changeOccured = true;
		}
		
		if(this.getDeltaType() != deltaType) {
			this.deltaType = deltaType;
			changeOccured = true;
		}
		
		if(getThisMinZ() != minZ) {
			this.minZ = realMinZ;
			changeOccured = true;
		}
		
		if(getThisMaxZ() != maxZ) {
			this.maxZ = realMaxZ;
			changeOccured = true;
		}
		
		this.isReady = true;

		if(changeOccured) {
			// Clear the vis window
			this.nodesReady = false; // We will need to re-query the database
			
		}

		Constants.log(Level.CONFIG, "Sensor settings "+type+": configuration", this);
	}
	
	public void setValidNodes(HashSet<Integer> newSet){
		if(this.validNodes == null) this.validNodes = new HashSet<Integer>();
		else this.validNodes.clear();
		this.validNodes.addAll(newSet);
		nodesReady = true;
	}
	
	public void setFullCloudNodes(HashSet<Integer> newSet){
		if(this.fullCloudNodes == null) this.fullCloudNodes = new HashSet<Integer>();
		else this.fullCloudNodes.clear();
		this.fullCloudNodes.addAll(newSet);
	}

	// Update the valid nodes based on current settings 
	private void setValidNodes(IProgressMonitor monitor) {

		validNodes.clear();

		Constants.log(Level.INFO, "Sensor settings "+type+": setting valid nodes", null);

		if(type.contains("ERT")) {
			validNodes = E4DSensors.setValidNodesERT(scenarioSet);
		}
		
		else if(getTrigger() == Trigger.MAXIMUM_THRESHOLD || getTrigger() == Trigger.MINIMUM_THRESHOLD) {

			Map<String, HashSet<Integer>> validNodesPerScenario = new HashMap<String, HashSet<Integer>>();
			for(Scenario scenario: scenarios) {
				// Query for valid nodes per scenario
				HashSet<Integer> nodes = null;
				try {
					if(HDF5Interface.hdf5Data.isEmpty()) {
						nodes = HDF5Interface.queryNodesFromFiles(scenarioSet.getNodeStructure(), scenario.toString(), getType(), lowerThreshold, upperThreshold, monitor);
					} else {
						nodes = HDF5Interface.queryNodesFromMemory(scenarioSet.getNodeStructure(), scenario.toString(), getType(), lowerThreshold, upperThreshold, monitor);
					}
					validNodesPerScenario.put(scenario.toString(), nodes);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if(!validNodesPerScenario.isEmpty()) {
				HashSet<Integer> allNodes = null;
				boolean first = true;
				for(String scenario: validNodesPerScenario.keySet()) {
					HashSet<Integer> nodes = validNodesPerScenario.get(scenario);
				//	System.out.println(scenario + " " + type + " nodes: " + nodes);
					if(first) {
						allNodes = new HashSet<Integer>(nodes);
						first = false;
					} else {
						allNodes.addAll(nodes); 
					}
				}
				for(Integer node: allNodes) {
					validNodes.add(node);
				}
				//System.out.println("Union: " + validNodes.size() + ": " + validNodes);
			}			
		} else {
		
			HashSet<Integer> allNodes = null;
			boolean first = true;
			for(Scenario scenario: scenarios) {
				try {
					HashSet<Integer> nodes = null;
					if(!HDF5Interface.hdf5Data.isEmpty()) {
						nodes = HDF5Interface.queryNodesFromMemory(scenarioSet.getNodeStructure(), scenario.getScenario(), getType(), lowerThreshold, upperThreshold, getTrigger(), getDeltaType(), monitor);
					} else {
						nodes = HDF5Interface.queryNodesFromFiles(scenarioSet.getNodeStructure(), scenario.getScenario(),  getType(), lowerThreshold, upperThreshold, getTrigger(), getDeltaType(), monitor);
					}
					if(first) {
						allNodes = new HashSet<Integer>(nodes);
						first = false;
					} else {
						allNodes.addAll(nodes); 
					}	
			//		System.out.println(scenario + " " + type + " nodes: " + nodes);			
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			for(Integer nodeNumber: allNodes) {
				validNodes.add(nodeNumber);
			}
		//	System.out.println("Union: " + validNodes.size() + ": " + validNodes);
		}
		
	//	System.out.println(validNodes);

		isReady = true;
		nodesReady = true;
		trimZ();
		
		fullCloudNodes = new HashSet<Integer>(validNodes);
		
		if(Constants.useParetoOptimal) paretoOptimal();
		
		Constants.log(Level.CONFIG, "Sensor settings: set valid nodes", this);
	}
	
	private void trimZ(){
		//Find the nodes that fit this z restriction
		HashSet<Integer> temp = new HashSet<Integer>();
		for(Integer node: validNodes){
			Point3f test = nodeStructure.getNodeCenteredXYZFromIJK(nodeStructure.getIJKFromNodeNumber(node));
			if (getThisMinZ() <= test.getZ() && test.getZ() <= getThisMaxZ()){
				temp.add(node);
			}
			else{
				//System.out.println(test.getZ() + "\t" + minZ + ":" + maxZ);
			}
		}
		validNodes.clear();
		validNodes.addAll(temp);
	}
	
	private void paretoOptimal(){
		HashMap<Integer, ArrayList<Float>> optimalSolutions = new HashMap<Integer, ArrayList<Float>>();
		
		for(Integer nodeNumber: validNodes){
			//build up the string ID and the list of ttds (for the ones that detect)
			ArrayList<Float> ttds = new ArrayList<Float>();
			for(Scenario scenario: scenarios){
				Float timeToDegredation = Float.MAX_VALUE;
				for (TimeStep timeStep: nodeStructure.getTimeSteps()){
					try {
						if(SimulatedAnnealing.paretoSensorTriggered(this, nodeStructure, timeStep, scenario, type, nodeNumber)) timeToDegredation = timeStep.getRealTime();
					} catch (Exception e) {
						e.printStackTrace();
					}
					if(timeToDegredation != Float.MAX_VALUE) break;
				}
				ttds.add(timeToDegredation);
			}
			ArrayList<Integer> toRemove = new ArrayList<Integer>(); //If this new configuration replaces one, it might replace multiple.
			boolean everyReasonTo = false;
			boolean everyReasonNot = false;
			for(Integer paretoSolutionLocation: optimalSolutions.keySet()){
				ArrayList<Float> paretoSolution = optimalSolutions.get(paretoSolutionLocation);
				boolean greater = false;
				boolean less = false;
				for(int i=0; i<paretoSolution.size(); ++i){
					if(paretoSolution.get(i) < ttds.get(i)) greater = true;
					if(paretoSolution.get(i) > ttds.get(i)) less = true;
				}
				if(greater && less){
					//don't need to do anything, both of these are pairwise pareto optimal
				}
				else if(greater && !less){
					everyReasonNot = true; //This solution is redundant, as there is another that is parwise optimal
					break; //we don't need to look anymore, don't include this new configuration
				}
				else if(!greater && less){
					everyReasonTo = true; //This solution is pareto optimal to this stored one
					toRemove.add(paretoSolutionLocation); //We need to remove this one, it has been replaced
				}
				else if(!greater && !less){
					//everyReasonNot = true; //These two spots are equal, so we might as well get rid of the one we're looking at
					break; //We don't need to check other spots if these are equal.
				}
			}
			if(everyReasonTo){
				//We need to add this one and remove some.
				for(Integer x : toRemove){
					optimalSolutions.remove(x);
				}
				optimalSolutions.put(nodeNumber, ttds);
			}
			else if(everyReasonNot){
				//Lets not add this one, it's redundant
			}
			else{
				//No reason not to add it and it didn't replace one, it must be another pareto optimal answer. Let's add it.
				optimalSolutions.put(nodeNumber, ttds);
			}
		}
		
		validNodes.clear();
		validNodes.addAll(optimalSolutions.keySet());
	}
	

	//This is a duplication of the pareto optimal code specifically for the "all" sensor.
	public static HashSet<Integer> paretoOptimalAll(HashSet<Integer> allNodes, List<Scenario> scenarios, NodeStructure ns, Map<String, SensorSetting> sensorSettings){ //THIS SHOULD JUST BE A TEMPORARY FUNCTION!!
		HashMap<Integer, ArrayList<Float>> optimalSolutions = new HashMap<Integer, ArrayList<Float>>();
		
		for(Integer nodeNumber: allNodes){
			//build up the string ID and the list of ttds (for the ones that detect)
			ArrayList<Float> ttds = new ArrayList<Float>();
			for(Scenario scenario: scenarios){
				Float timeToDegredation = Float.MAX_VALUE;
				for (TimeStep timeStep: ns.getTimeSteps()){
					try {
						//Need a loop for each sensor type
						for(SensorSetting setting: sensorSettings.values())
						if(SimulatedAnnealing.paretoSensorTriggered(setting, ns, timeStep, scenario, setting.type, nodeNumber)) timeToDegredation = timeStep.getRealTime();
					} catch (Exception e) {
						e.printStackTrace();
					}
					if(timeToDegredation != Float.MAX_VALUE) break;
				}
				ttds.add(timeToDegredation);
			}
			ArrayList<Integer> toRemove = new ArrayList<Integer>(); //If this new configuration replaces one, it might replace multiple.
			boolean everyReasonTo = false;
			boolean everyReasonNot = false;
			for(Integer paretoSolutionLocation: optimalSolutions.keySet()){
				ArrayList<Float> paretoSolution = optimalSolutions.get(paretoSolutionLocation);
				boolean greater = false;
				boolean less = false;
				for(int i=0; i<paretoSolution.size(); ++i){
					if(paretoSolution.get(i) < ttds.get(i)) greater = true;
					if(paretoSolution.get(i) > ttds.get(i)) less = true;
				}
				if(greater && less){
					//don't need to do anything, both of these are pairwise pareto optimal
				}
				else if(greater && !less){
					everyReasonNot = true; //This solution is redundant, as there is another that is parwise optimal
					break; //we don't need to look anymore, don't include this new configuration
				}
				else if(!greater && less){
					everyReasonTo = true; //This solution is pareto optimal to this stored one
					toRemove.add(paretoSolutionLocation); //We need to remove this one, it has been replaced
				}
				else if(!greater && !less){
					//everyReasonNot = true; //These two spots are equal, so we might as well get rid of the one we're looking at
					break; //We don't need to check other spots if these are equal.
				}
			}
			if(everyReasonTo){
				//We need to add this one and remove some.
				for(Integer x : toRemove){
					optimalSolutions.remove(x);
				}
				optimalSolutions.put(nodeNumber, ttds);
			}
			else if(everyReasonNot){
				//Lets not add this one, it's redundant
			}
			else{
				//No reason not to add it and it didn't replace one, it must be another pareto optimal answer. Let's add it.
				optimalSolutions.put(nodeNumber, ttds);
			}
		}
		HashSet<Integer> solution = new HashSet<Integer>();
		solution.addAll(optimalSolutions.keySet());
		return solution;
	}


	/**					**\
	 * Getters & Setters *
	 * 					 *
	\*					 */
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
		isReady = false;
	}
	
	public float getCost() {
		return cost;
	}

	public void setCost(float cost) {
		this.cost = cost;
	}

	public boolean isReady() {
		return isReady;
	}
	
	public synchronized Set<Integer> getValidNodes(IProgressMonitor monitor) {
		if(!nodesReady && monitor != null)
			setValidNodes(monitor);
		if(!nodesReady && monitor == null) {
			System.err.println("Nodes are not ready and we didn't provide a progress monitor, fix this.");
		}
		return validNodes;
	}
	
	public synchronized Set<Integer> getCloudNodes(IProgressMonitor monitor) {
		if(!areNodesReady() && monitor != null)
			setValidNodes(monitor);
		if(!areNodesReady() && monitor == null) {
			System.err.println("Nodes are not ready and we didn't provide a progress monitor, fix this.");
		}
		return fullCloudNodes;
	}
	
	public Color getColor() {
		return color;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	public boolean areNodesReady() {
		return nodesReady;
	}
	
	public void setNodesReady(boolean nodesReady) {
		this.nodesReady = nodesReady;
	}
	
	public void setIsReady(boolean isReady) {
		this.isReady = isReady;
	}

	public float getMinValue() {
		return minValue;
	}
	public float getMaxValue() {
		return maxValue;
	}
	
	public boolean isSet() {
		return areNodesReady();
	}

	public void removeNode(Integer node) {
		validNodes.remove(node);
	}

	public Trigger getTrigger() {
		return trigger;
	}
	
	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
	}
	
	public float getLowerThreshold() {
		return lowerThreshold;
	}
	
	public void setLowerThreshold(Float lowerThreshold) {
		this.lowerThreshold = lowerThreshold;
	}

	public float getUpperThreshold() {
		return upperThreshold;
	}

	public void setUpperThreshold(Float upperThreshold) {
		this.upperThreshold = upperThreshold;
	}
	
	public DeltaType getDeltaType() {
		return deltaType;
	}

	public void setDeltaType(DeltaType deltaType) {
		this.deltaType = deltaType;
	}
	
	public float getGlobalMinZ() {
		return globalMinZ;
	}

	public void setGlobalMinZ(float minZ) {
		globalMinZ = minZ;
	}

	public float getGlobalMaxZ() {
		return globalMaxZ;
	}

	public void setGlobalMaxZ(float maxZ) {
		globalMaxZ = maxZ;
	}
	
	public float getThisMinZ(){
		return minZ;
	}
	
	public float getThisMaxZ(){
		return maxZ;
	}
}