package objects;

import hdf5Tool.HDF5Wrapper;

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

import functions.CCS9_1;
import utilities.Constants;
import utilities.Point3f;

/**
 * Holds the logic for a specific sensor type and threshold
 * @author port091
 * @author rodr144
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

	private Float min;
	private Float max;
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
	private NodeStructure nodeStructure;
	
	ScenarioSet scenarioSet;
	
	public SensorSetting(NodeStructure nodeStructure, ScenarioSet scenarioSet, String type, List<Scenario> scenarios) {

		this.nodeStructure = nodeStructure;
		this.scenarioSet = scenarioSet;
		this.scenarios = scenarios;
		this.type = type;
		this.cost = 100;

		setMin();
		setMax();

		this.trigger = Trigger.MAXIMUM_THRESHOLD;
		this.setDeltaType(DeltaType.BOTH);
		this.lowerThreshold = 0;
		this.upperThreshold = 0;

		this.validNodes = new HashSet<Integer>(); // None yet
		
		this.setMaxZ(Collections.max(this.nodeStructure.getZ()));
		this.setMinZ(Collections.min(this.nodeStructure.getZ()));
		this.color = Color.GREEN;	

		this.isReady = false;
		this.nodesReady = false;

		Constants.log(Level.INFO, "Sensor settings "+type+": initialized ", null);
		Constants.log(Level.CONFIG, "Sensor settings "+type+": configuration", this);

	}
	
	public SensorSetting(NodeStructure nodeStructure, ScenarioSet scenarioSet, String type, List<Scenario> scenarios, float min, float max) {

		this.nodeStructure = nodeStructure;
		this.scenarioSet = scenarioSet;
		this.scenarios = scenarios;
		this.type = type;
		this.cost = 100;

		this.min = min;
		this.max = max;

		this.trigger = Trigger.MAXIMUM_THRESHOLD;
		this.setDeltaType(DeltaType.BOTH);
		this.lowerThreshold = 0;
		this.upperThreshold = 0;

		this.validNodes = new HashSet<Integer>(); // None yet
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
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("Sensor settings "+type+":\n");

		if(!isReady()) {
			builder.append("\tUser settings not set - using defaults:\n");
		} else {
			builder.append("\tUser settings:\n");
		}

		builder.append("\t\tCost: " + cost + "\n");
		builder.append("\t\tColor: " + color.toString() + "\n");

		builder.append("\t\tMin value: " + min + "\n");
		builder.append("\t\tMax value: " + max + "\n");
		builder.append("\t\tTriggering on: " + getTrigger().toString()  + "\n");
		
		builder.append("\t\tminZ: " + globalMinZ + "\n");
		builder.append("\t\tmaxZ: " + globalMaxZ + "\n");

		if(areNodesReady()) {
			builder.append("\t"+validNodes.size()+" Valid nodes: " + validNodes.toString() + "\n");
		} else {
			builder.append("\tValid nodes: not set\n");
		}

		return builder.toString();
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
		
		if(this.getMinZ() != minZ) {
			this.minZ = realMinZ;
			changeOccured = true;
		}
		
		if(this.getMaxZ() != maxZ) {
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

		if(getTrigger() == Trigger.MAXIMUM_THRESHOLD || getTrigger() == Trigger.MINIMUM_THRESHOLD) {

			Map<String, HashSet<Integer>> validNodesPerScenario = new HashMap<String, HashSet<Integer>>();
			for(Scenario scenario: scenarios) {
				// Query for valid nodes per scenario
				try {
					HashSet<Integer> nodes = Constants.hdf5Data.isEmpty() ? 
							HDF5Wrapper.queryNodesFromFiles(scenarioSet.getNodeStructure(), scenario.toString(), getType(), lowerThreshold, upperThreshold, monitor) : 
								HDF5Wrapper.queryNodesFromMemory(scenarioSet.getNodeStructure(), scenario.toString(), getType(), lowerThreshold, upperThreshold, monitor);
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
						if(Constants.scenarioUnion)
							allNodes.addAll(nodes); 
						else // Intersection
							allNodes.retainAll(nodes); 
					}
				}
				for(Integer node: allNodes) {
					validNodes.add(node);
				}
				//System.out.println((Constants.scenarioUnion ? "Union: " : "Intersection: ") + validNodes.size() + ": " + validNodes);
			}			
		} else {
		
			HashSet<Integer> allNodes = null;
			boolean first = true;
			for(Scenario scenario: scenarios) {
				try {
					HashSet<Integer> nodes = !Constants.hdf5Data.isEmpty() ? 
							HDF5Wrapper.queryNodesFromMemory(scenarioSet.getNodeStructure(), scenario.getScenario(), getType(), lowerThreshold, upperThreshold, getTrigger(), getDeltaType(), monitor) :
							HDF5Wrapper.queryNodesFromFiles(scenarioSet.getNodeStructure(), scenario.getScenario(),  getType(), lowerThreshold, upperThreshold, getTrigger(), getDeltaType(), monitor);
					if(first) {
						allNodes = new HashSet<Integer>(nodes);
						first = false;
					} else {
						if(Constants.scenarioUnion)
							allNodes.addAll(nodes); 
						else // Intersection
							allNodes.retainAll(nodes); 
					}	
			//		System.out.println(scenario + " " + type + " nodes: " + nodes);			
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			for(Integer nodeNumber: allNodes) {
				validNodes.add(nodeNumber);
			}
		//	System.out.println((Constants.scenarioUnion ? "Union: " : "Intersection: ") + validNodes.size() + ": " + validNodes);
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
						if(CCS9_1.paretoSensorTriggered(this, nodeStructure, timeStep, scenario, type, nodeNumber)) timeToDegredation = timeStep.getRealTime();
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
						if(CCS9_1.paretoSensorTriggered(setting, ns, timeStep, scenario, setting.type, nodeNumber)) timeToDegredation = timeStep.getRealTime();
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

	private void setMin() { min = 0f;
		if(min != null)
			return;
		long startTime = System.currentTimeMillis();
		if(Constants.hdf5Data.isEmpty() && Constants.hdf5CloudData.isEmpty()) {
			try {
				min = HDF5Wrapper.queryMinFromFiles(nodeStructure, getType());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(Constants.hdf5Data.isEmpty()) {
			min = HDF5Wrapper.queryMinFromCloud(getType());
		} else {
			min = HDF5Wrapper.queryMinFromMemory(getType());			
		}
		System.out.println("Time to query for min: " + (System.currentTimeMillis() - startTime));
	}

	private void setMax() { max =0f;
		if(max != null)
			return;
		long startTime = System.currentTimeMillis();
		if(Constants.hdf5Data.isEmpty() && Constants.hdf5CloudData.isEmpty()) {
			try {
				max = HDF5Wrapper.queryMaxFromFiles(nodeStructure, getType());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(Constants.hdf5Data.isEmpty()) {
			max = HDF5Wrapper.queryMaxFromCloud(getType());
		} else {
			max = HDF5Wrapper.queryMaxFromMemory(getType());			
		}
		System.out.println("Time to query for max: " + (System.currentTimeMillis()-startTime));
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
		if(!areNodesReady() && monitor != null)
			setValidNodes(monitor);
		if(!areNodesReady() && monitor == null) {
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

	public float getMin() {
		return min;
	}
	public float getMax() {
		return max;
	}
	
	public boolean isSet() {
		return areNodesReady();
	}

	public void removeNode(Integer node) {
		validNodes.remove(node);
	}

	public void setValidNodes(List<Integer> intersection) {
		validNodes.clear();
		validNodes.addAll(intersection);		
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
	
	public float getMinZ() {
		return globalMinZ;
	}

	public void setMinZ(float minZ) {
		globalMinZ = minZ;
	}

	public float getMaxZ() {
		return globalMaxZ;
	}

	public void setMaxZ(float maxZ) {
		globalMaxZ = maxZ;
	}
	
	public float getThisMinZ(){
		return minZ;
	}
	
	public float getThisMaxZ(){
		return maxZ;
	}
}