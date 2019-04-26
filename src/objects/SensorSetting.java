package objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import utilities.Constants;
import utilities.Point3f;

/**
 * Holds the logic for a specific sensor type and threshold
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class SensorSetting {
		
	public enum Trigger {
		
		ABOVE_THRESHOLD("Above threshold"), BELOW_THRESHOLD("Below threshold"), 
		RELATIVE_CHANGE("Relative change"), ABSOLUTE_CHANGE("Absolute change");
		
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
	private String alias;
	private float sensorCost;

	private float minZ;
	private float maxZ;
	
	private Trigger trigger;
	private DeltaType deltaType;

	private float detectionThreshold;

	private HashSet<Integer> validNodes; //Pareto optimal locations (if set in Constants class)
	private HashSet<Integer> fullCloudNodes; //Full set of allowed locations
	private static Map<String, HashMap<Float, Float>> volumeDegradedByYear;
	private static List<Float> years;
	public static float globalMinZ;
	public static float globalMaxZ;
	public String specificType;
	
	private NodeStructure nodeStructure;
	
	// Sensor Settings for H5 Files
	public SensorSetting(NodeStructure nodeStructure, String type) {

		this.nodeStructure = nodeStructure;
		this.type = type;
		sensorCost = 100;
		
		getTriggerFromText("below", "0"); //Sets the trigger and delta type
		detectionThreshold = 0; //Based on the trigger, this represents the range for valid nodes
		setGlobalMaxZ(Collections.max(nodeStructure.getZ()));
		setGlobalMinZ(Collections.min(nodeStructure.getZ()));
		
		fullCloudNodes = new HashSet<Integer>(); //Added later, initialize here
		validNodes = new HashSet<Integer>(); //Added later, initialize here
		
		// specificType can be set after inputting parameters in Page_LeakageCriteria for H5 files
		
		Constants.log(Level.INFO, "Sensor settings "+type+": initialized ", null);
		Constants.log(Level.CONFIG, "Sensor settings "+type+": configuration", this);
	}
	
	// Sensor Settings for IAM files
	public SensorSetting(NodeStructure nodeStructure, String type, String trigger, String threshold) {
		
		this.nodeStructure = nodeStructure;
		this.type = type;
		sensorCost = 100;
		
		getTriggerFromText(trigger, threshold); //Sets the trigger and delta type
		detectionThreshold = Float.parseFloat(threshold); //Based on the trigger, this represents the range for valid nodes
		setGlobalMaxZ(Collections.max(nodeStructure.getZ()));
		setGlobalMinZ(Collections.min(nodeStructure.getZ()));
		
		fullCloudNodes = new HashSet<Integer>(); //Added later, initialize here
		validNodes = new HashSet<Integer>(); //Added later, initialize here
		
		specificType = getSpecificType(); //Needs to be set now before the detection map is created
		
		Constants.log(Level.INFO, "Sensor settings "+type+": initialized ", null);
		Constants.log(Level.CONFIG, "Sensor settings "+type+": configuration", this);
	}
	
	public static void setVolumeDegradedByYear(Map<String, HashMap<Float, Float>> volumeDegradedByYear2, ArrayList<Float> yearList){
		years = yearList;
		volumeDegradedByYear = volumeDegradedByYear2;
	}
	
	public static List<Float> getYears() {
		return years;
	}
	
	public static Map<String, Float> getVolumesDegraded(Map<String, Float> ttdMap){
		HashMap<String, Float> vadMap = new HashMap<String, Float>();
		for(String scenario: ttdMap.keySet()){
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
	
	public static float getVolumeDegradedByTTDs(Map<String, Float> ttdMap, int numScenarios) {
		float volume = 0;
		//Note that this only loops over scenarios in which some volume of aquifer is degraded
		for(String scenario: volumeDegradedByYear.keySet()){
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
			for(String scenario: volumeDegradedByYear.keySet()){
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
			for(String scenario: volumeDegradedByYear.keySet()){
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
			for(String scenario: volumeDegradedByYear.keySet()){
				minVAD = Math.min(minVAD, volumeDegradedByYear.get(scenario).get(year));
			}
			minVADMap.put(year, minVAD);
		}
		return minVADMap;
	}

	public void setUserSettings(float sensorCost, float detectionThreshold, Trigger trigger, DeltaType deltaType, float minZ, float maxZ, String alias) {

		Constants.log(Level.INFO, "Sensor settings "+type+": setting user settings", null);
		
		float realMaxZ = Math.max(minZ, maxZ);
		float realMinZ = Math.min(minZ, maxZ);
		
		this.alias = alias;
		this.sensorCost = sensorCost;
		this.detectionThreshold = detectionThreshold;
		this.trigger = trigger;
		this.deltaType = deltaType;
		this.minZ = realMinZ;
		this.maxZ = realMaxZ;
		this.specificType = getSpecificType();

		Constants.log(Level.CONFIG, "Sensor settings "+type+": configuration", this);
	}
	
	public void setValidNodes(HashSet<Integer> newSet){
		validNodes = new HashSet<Integer>();
		validNodes.addAll(newSet);
	}
	
	
	public void setNodes(ScenarioSet set) {
		
		fullCloudNodes.clear();
		validNodes.clear();
		
		if(type.contains("Electrical Conductivity"))
			fullCloudNodes = validNodes = E4DSensors.setValidNodesERT(detectionThreshold);
		else {
			// From the detectionMap, we just need to get a list of nodes that exist across selected scenarios (fullCloudNodes)
			for(String scenario: set.getScenarios()) {
				if(set.getDetectionMap().get(specificType).get(scenario).size() > 0) {
					for(Integer node: set.getDetectionMap().get(specificType).get(scenario).keySet())
						fullCloudNodes.add(node);
				}
			}
			
			// Remove nodes outside of Z range
			trimZ();
			
			// Use pareto Optimal algorithm to get a smaller subset of good nodes (validNodes)
			validNodes.addAll(fullCloudNodes);
			if(Constants.useParetoOptimal && !type.contains("Electrical Conductivity"))
				paretoOptimal(set.getDetectionMap(), set.getScenarios());
		}
	}
	
	
	private void trimZ(){
		//Find the nodes that fit this z restriction
		for(Integer node: fullCloudNodes) {
			Point3f test = nodeStructure.getNodeCenteredXYZFromIJK(nodeStructure.getIJKFromNodeNumber(node));
			if(test.getZ() < minZ || test.getZ() > maxZ) //outside of bounds
				fullCloudNodes.remove(node);
		}
	}
	
	public int getNodeSize() {
		return validNodes.size();
	}
	
	private void paretoOptimal(Map<String, Map<String, Map<Integer, Float>>> detectionMap, List<String> scenarios) {
		HashMap<Integer, ArrayList<Float>> optimalSolutions = new HashMap<Integer, ArrayList<Float>>();
		
		for(Integer nodeNumber: validNodes) {
			//build up the string ID and the list of ttds (for the ones that detect)
			ArrayList<Float> ttds = new ArrayList<Float>();
			for(String scenario: scenarios) {
				Float timeToDegredation = Float.MAX_VALUE;
				if(detectionMap.get(specificType).get(scenario).containsKey(nodeNumber))
					timeToDegredation = detectionMap.get(specificType).get(scenario).get(nodeNumber);
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
		System.out.println("Pareto Optimal just pared down valid nodes for " + specificType + " from " + validNodes.size() + " to " + optimalSolutions.size());
		validNodes.clear();
		validNodes.addAll(optimalSolutions.keySet());
	}
	
	
	/**					**\
	 * Getters & Setters *
	 * 					 *
	\*					 */
	
	public String getType() {
		return type;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public float getSensorCost() {
		return sensorCost;
	}

	public void setSensorCost(float sensorCost) {
		this.sensorCost = sensorCost;
	}
	
	public Set<Integer> getValidNodes() {
		return validNodes;
	}
	
	public Set<Integer> getCloudNodes() {
		return fullCloudNodes;
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
	
	public float getDetectionThreshold() {
		return detectionThreshold;
	}
	
	public void setDetectionThreshold(Float detection) {
		detectionThreshold = detection;
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

	public void clearNodes() {
		fullCloudNodes.clear();
		validNodes.clear();
	}
	
	public void getTriggerFromText(String trigger, String threshold) {
		if(trigger.contains("below"))
			this.trigger = Trigger.BELOW_THRESHOLD;
		else if(trigger.contains("above"))
			this.trigger = Trigger.ABOVE_THRESHOLD;
		else if(trigger.contains("rel"))
			this.trigger = Trigger.RELATIVE_CHANGE;
		else if(trigger.contains("abs"))
			this.trigger = Trigger.ABSOLUTE_CHANGE;
		if(threshold.contains("-") && (trigger.contains("abs") || trigger.contains("rel")))
			deltaType = DeltaType.DECREASE;
		else if(threshold.contains("+") && (trigger.contains("abs") || trigger.contains("rel")))
			deltaType = DeltaType.INCREASE;
		else
			deltaType = DeltaType.BOTH;
	}
	
	public Trigger getTriggerFromSpecificType(String specificType) {
		if(specificType.contains("rel"))
			return Trigger.RELATIVE_CHANGE;
		else if (specificType.contains("abs"))
			return Trigger.ABSOLUTE_CHANGE;
		else if(specificType.contains("below"))
			return Trigger.BELOW_THRESHOLD;
		else
			return Trigger.ABOVE_THRESHOLD;
	}
	
	public String getSpecificType() {
		String specificType = "";
		if(getTrigger() == Trigger.BELOW_THRESHOLD)
			specificType = type + "_below_" + detectionThreshold;
		else if(getTrigger() == Trigger.ABOVE_THRESHOLD)
			specificType = type + "_above_" + detectionThreshold;
		else if(getTrigger() == Trigger.RELATIVE_CHANGE) {
			specificType = type + "_rel_" + detectionThreshold;
			if(getDeltaType() == DeltaType.INCREASE) // Needed to maintain plus sign
				specificType = type + "_rel_+" + detectionThreshold;
		} else if(getTrigger() == Trigger.ABSOLUTE_CHANGE) {
			specificType = type + "_abs_" + detectionThreshold;
			if(getDeltaType() == DeltaType.INCREASE) // Needed to maintain plus sign
				specificType = type + "_abs_+" + detectionThreshold;
		}
		return specificType;
	}
}