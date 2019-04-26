package objects;

import objects.SensorSetting.DeltaType;
import objects.SensorSetting.Trigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import utilities.Constants;
import utilities.Point3f;
import utilities.Point3i;


/**
 * Selections made by the user for a given run should be set here.
 * @author port091
 * @author rodr144
 *
 */
public class ScenarioSet {

	private boolean isReady;
		
	/**
	 *  Scenarios must share common node structure
	 */
	private NodeStructure nodeStructure;
	private List<String> allScenarios;
	private List<String> scenarios;
	private List<Well> wells;
	
	// detectionMap stores values of TTD for all scenarios and specific sensors
	// IAM files are immediately loaded into detectionMap
	// H5 files are loaded to detectionMap at Page_LeakageCriteria based on user input settings, and saved as more are added
	private Map<String, Map<String, Map<Integer, Float>>> detectionMap; //Specific Type <Scenario <Index, TTD>>
	
	/**
	 * User settings - 
	 */
	private Point3i addPoint;
	private int maxWells;
	private int iterations;
	private float sensorCostConstraint;
	private float exclusionRadius;
	private float inclusionRadius;
	private float wellCost;
	private float wellDepthCost;
	private float remediationCost;
	private String scenarioEnsemble;
	
	private Map<String, Float> scenarioWeights;
	private Map<String, SensorSetting> sensorSettings;
	//Finding nodes removes some sensor settings, causing problems going back a page and then forward again
	//When removing sensor settings, they are instead saved in this Hashmap
	private Map<String, SensorSetting> sensorSettingsRemoved;
	
	private InferenceTest inferenceTest;
	private boolean edgeMovesOnly = false;
	
	
	// Reset everything in ScenarioSet - do this before loading new files
	public ScenarioSet() {
		isReady = false;
		
		allScenarios = new ArrayList<String>();
		scenarios = new ArrayList<String>();
		wells = new ArrayList<Well>();
		
		detectionMap = new HashMap<String, Map<String, Map<Integer, Float>>>();
		
		scenarioWeights = new HashMap<String, Float>();
		sensorSettings = new HashMap<String, SensorSetting>();
		sensorSettingsRemoved = new HashMap<String, SensorSetting>();
		scenarioEnsemble = "";
		addPoint = new Point3i(1,1,1);
		maxWells = 10;
		iterations = 1000;
		sensorCostConstraint = 0;
		exclusionRadius = 0;
		inclusionRadius = Float.MAX_VALUE;
		wellCost = 0;
		wellDepthCost = 0;
		remediationCost = 0;
		
		Constants.log(Level.INFO, "Scenario set: initialized", null);
		Constants.log(Level.CONFIG, "Scenario set: configuration", this);
	}
	
	
	public void clearRun() {
		isReady = false;
		
		allScenarios.clear();
		scenarios.clear();
		wells.clear();
		
		detectionMap.clear();
		
		scenarioWeights.clear();
		sensorSettings.clear();
		sensorSettingsRemoved.clear();
		scenarioEnsemble = "";
		addPoint = new Point3i(0,0,0);
		maxWells = 10;
		iterations = 1000;
		sensorCostConstraint = 0;
		exclusionRadius = 0;
		inclusionRadius = Float.MAX_VALUE;
		wellCost = 0;
		wellDepthCost = 0;
		remediationCost = 0;
		
		Constants.log(Level.INFO, "Scenario set: re-initialized", null);
		Constants.log(Level.CONFIG, "Scenario set: configuration", this);
	}
	
	
	@Override
	public String toString() {
		String zUnit = nodeStructure.getUnit("z");
		StringBuilder builder = new StringBuilder();
		builder.append("---- Input Summary ----\r\n");
		
		// Details about the scenario set read from the file being used
		builder.append("Scenario ensemble: " + scenarioEnsemble + "\r\n");
		builder.append("Scenario weights:\r\n");
		for(String scenario: scenarios)
			builder.append("\t" + scenario + " = " + Constants.percentageFormat.format(scenarioWeights.get(scenario)) + "\r\n");
		
		// Leakage criteria
		builder.append("Technology settings:\r\n");
		for(String parameter: sensorSettings.keySet()) {
			String unit = nodeStructure.getUnit(parameter);
			SensorSetting sensorSetting = sensorSettings.get(parameter);
			builder.append("\t" + parameter + ":\r\n");
			builder.append("\t\tAlias: " + Sensor.sensorAliases.get(parameter) + "\r\n");
			builder.append("\t\tCost: " + Constants.percentageFormat.format(sensorSetting.getSensorCost()) + " per monitoring location\r\n");
			builder.append("\t\tTriggering on: " + sensorSetting.getTrigger() + "\r\n");
			if(sensorSetting.getTrigger() == Trigger.BELOW_THRESHOLD || sensorSetting.getTrigger() == Trigger.ABOVE_THRESHOLD)
				builder.append("\t\tLeakage threshold: " + sensorSetting.getDetectionThreshold() + "\r\n");
			else {
				if(sensorSetting.getDeltaType() == DeltaType.DECREASE)
					builder.append("\t\tLeakage threshold: Negative change of " + sensorSetting.getDetectionThreshold() + unit + "\r\n");
				else if (sensorSetting.getDeltaType() == DeltaType.INCREASE)
					builder.append("\t\tLeakage threshold: Positive change of " + sensorSetting.getDetectionThreshold() + unit + "\r\n");
				else
					builder.append("\t\tLeakage threshold: Change of " + sensorSetting.getDetectionThreshold() + unit + "\r\n");
			}
			builder.append("\t\tZone bottom: " + Constants.percentageFormat.format(sensorSetting.getThisMinZ()) + zUnit + "\r\n");
			builder.append("\t\tZone top: " + Constants.percentageFormat.format(sensorSetting.getThisMaxZ()) + zUnit + "\r\n");
			if(sensorSetting.getValidNodes().size()>0) {
				int size = nodeStructure.getIJKDimensions().getI() * nodeStructure.getIJKDimensions().getJ() * nodeStructure.getIJKDimensions().getK();
				builder.append("\t\tValid nodes: " + sensorSetting.getValidNodes().size() + " of " + size + "\r\n");
			} else {
				builder.append("\t\tValid nodes: not set\r\n");
			}
		}
		
		// Sensor minimums
		builder.append(inferenceTest);
		
		// Configuration settings page inputs
		if(!isReady())
			builder.append("Configuration settings not set - using defaults:\r\n");
		else
			builder.append("Configuration settings:\r\n");
		builder.append("\tSensor budget: " + Constants.percentageFormat.format(sensorCostConstraint) + "\r\n");
		builder.append("\tMax wells: " + maxWells + "\r\n");
		builder.append("\tMin distance between wells: " + Constants.percentageFormat.format(exclusionRadius) + zUnit + "\r\n");
		builder.append("\tCost per well: " + Constants.percentageFormat.format(wellCost) + "\r\n");
		builder.append("\tCost per " + (zUnit=="" ? "unit" : zUnit) + " depth of well: " + Constants.percentageFormat.format(wellDepthCost) + "\r\n");
		if(Constants.buildDev)
			builder.append("\tRemediation cost: " + Constants.percentageFormat.format(remediationCost) + " per " + (zUnit=="" ? "water unit" : zUnit + "^3") + "\r\n");
		
		return builder.toString();
	}
	
	public void setUserSettings(Point3i addPoint, int maxWells, float sensorCostConstraint, float exclusionRadius, float wellCost, float wellDepthCost, float remediationCost) {
		
		Constants.log(Level.INFO, "Scenario set: setting user settings", null);
		
		this.addPoint = addPoint;
		this.maxWells = maxWells;
		this.sensorCostConstraint = sensorCostConstraint;
		this.exclusionRadius = exclusionRadius;
		this.wellCost = wellCost;
		this.wellDepthCost = wellDepthCost;
		this.remediationCost = remediationCost;
		isReady = true;
		//TEST!!!
		//for(SensorSetting setting: sensorSettings.values()){
		//	float depth = SensorSetting.getMaxZ() - setting.getThisMinZ();
		//	inclusionRadius = Math.min(inclusionRadius, depth*(2f/3f));
		//}
		//ENDTEST!!!
		
		Constants.log(Level.CONFIG, "Scenario set: configuration", this);
	}

	public void setupScenarios(List<String> inputScenarios) {
		// Convert string to scenario and add to lists
		for(String scenario: inputScenarios) {
			if(!scenarios.contains(scenario))
				scenarios.add(scenario);
			if(!allScenarios.contains(scenario))
				allScenarios.add(scenario);
		}
		
		// Scenario weights should start at 1.0
		for(String scenario: scenarios)
			scenarioWeights.put(scenario, (float)1);
	}
	
	public void setupSensorSettings() {
		// Setup the sensor settings array
		for(final String type: nodeStructure.getDataTypes())
			sensorSettings.put(type, new SensorSetting(nodeStructure, type));
	}
	
	// Setup the inference test
	public void setupInferenceTest() {
		inferenceTest = new InferenceTest("Any Technology", 1);
	}
	
	
	/**					**\
	 * Getters & Setters *
	 * 					 *
	\*					 */
	
	public boolean isReady() {
		return isReady;
	}
	
	public List<String> getScenarios() {
		return scenarios;
	}
	
	public String getScenarioEnsemble() {
		return scenarioEnsemble;
	}
	
	public void setScenarioEnsemble(String scenarioEnsemble) {
		this.scenarioEnsemble = scenarioEnsemble;
	}
	
	public List<String> getAllScenarios() {
		return allScenarios;
	}
	
	public float getGloballyNormalizedScenarioWeight(String scenario) {
		return scenarioWeights.get(scenario) / getTotalScenarioWeight();
	}
	
	public Map<String, Float> getScenarioWeights() {
		return scenarioWeights;
	}
	
	public void setEdgeMovesOnly(boolean edgeMovesOnly) {
		this.edgeMovesOnly = edgeMovesOnly;
	}

	public Point3i getAddPoint() {
		return addPoint;
	}
	
	public int getMaxWells() {
		return maxWells;
	}

	public void setMaxWells(int maxWells) {
		this.maxWells = maxWells;
	}
	
	public float getExclusionRadius() {
		return exclusionRadius;
	}

	public void setExclusionRadius(float exclusionRadius) {
		this.exclusionRadius = exclusionRadius;
	}
	
	public float getWellCost() {
		return wellCost;
	}
	
	public void setWellCost(float wellCost) {
		this.wellCost = wellCost;
	}
	
	public float getWellDepthCost() {
		return wellDepthCost;
	}
	
	public void setWellDepthCost(float wellDepthCost) {
		this.wellDepthCost = wellDepthCost;
	}
	
	public float getRemediationCost() {
		return remediationCost;
	}
	
	public void setRemediationCost(float remediationCost) {
		this.remediationCost = remediationCost;
	}
	
	public int getIterations() {
		return iterations;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public InferenceTest getInferenceTest() {
		return inferenceTest;
	}
	
	// This returns the cost of all sensors in the configuration
	public float getSensorCost(ExtendedConfiguration configuration) {
		float cost = 0;
		for(ExtendedSensor sensor: configuration.getExtendedSensors()) {
			cost += getSensorSettings(sensor.getSensorType()).getSensorCost();
		}
		return cost;
	}
	
	public int countWells(ExtendedConfiguration configuration){
		List<ExtendedSensor> sensors = configuration.getExtendedSensors();
		List<String> ijs = new ArrayList<String>();
		// Count the number of wells in the configuration
		for(ExtendedSensor sensor: sensors) {
			String ij = sensor.getIJK().getI() + "_" + sensor.getIJK().getJ();
			if(!ijs.contains(ij))
				ijs.add(ij);
			if(sensor.getSensorType().contains("Electrical Conductivity")) {
				Point3i pair = nodeStructure.getIJKFromNodeNumber(sensor.getNodePairNumber());
				String ijPair = pair.getI() + "_" + pair.getJ();
				if(!ijs.contains(ijPair))
					ijs.add(ijPair);
			}
		}
		return ijs.size();
	}
	
	// This returns the cost for post-processing cost-analysis.
	public float costOfConfiguration(ExtendedConfiguration configuration){
		if(configuration.getConfigCost() != 0)
			return configuration.getConfigCost();
		float cost = 0;
		List<ExtendedSensor> sensors = configuration.getExtendedSensors();
		List<Point3i> locations = new ArrayList<Point3i>();
		boolean addLocation;
		boolean addPairLocation;
		for(ExtendedSensor sensor: sensors){
			addLocation = true;
			Point3i point = sensor.getIJK();
			List<Point3i> originalLocations = new ArrayList<Point3i>();
			originalLocations.addAll(locations); // Avoids concurrent modification error (edit array within a loop of array)
			for(Point3i location: originalLocations){
				if(location.getI()==point.getI() && location.getJ()==point.getJ()) { //Same well location
					if(point.getK() < location.getK()) {
						locations.remove(location);
						locations.add(point);
					}
					addLocation = false;
				}
			}
			if(addLocation)
				locations.add(point);
			if(sensor.getSensorType().contains("Electrical Conductivity")) { //ERT will have a second paired well that also needs to be factored into cost
				addPairLocation = true;
				Point3i pointPair = this.getNodeStructure().getIJKFromNodeNumber(sensor.getNodePairNumber()); //Paired point from ERT technology
				originalLocations = new ArrayList<Point3i>();
				originalLocations.addAll(locations); // Avoids concurrent modification error (edit array within a loop of array)
				for(Point3i location: originalLocations){
					if(location.getI()==pointPair.getI() && location.getJ()==pointPair.getJ()) { //Same well location
						if(pointPair.getK() < location.getK()) {
							locations.remove(location);
							locations.add(pointPair);
						}
					}
				}
				if(addPairLocation)
					locations.add(pointPair);
			}
			cost += sensorSettings.get(sensor.getSensorType()).getSensorCost();
		}
		for(Point3i location: locations){
			float maxZ = SensorSetting.globalMaxZ > 0 ? SensorSetting.globalMaxZ : 0; //Use 0 as the top if the locations are negative, otherwise use globalMaxZ
			cost += (maxZ - this.getNodeStructure().getXYZEdgeFromIJK(location).getZ()) * this.wellDepthCost;
			cost += this.wellCost;
		}
		configuration.setConfigCost(cost);
		return cost;
	}
	
	public float getSensorCostConstraint() {
		return sensorCostConstraint;
	}
	
	public void setSensorCostConstraint(float sensorCostConstraint) {
		this.sensorCostConstraint = sensorCostConstraint;
	}
	
	public NodeStructure getNodeStructure() {
		return nodeStructure;
	}
	
	public void setNodeStructure(NodeStructure nodeStructure) {
		this.nodeStructure = nodeStructure;
	}
	
	public Map<Integer, List<Integer>> getAllPossibleWells() {
		List<Integer> cloudNodes = new ArrayList<Integer>();
				
		Map<Integer, List<Integer>> ijs = new HashMap<Integer, List<Integer>>();
		for(Integer node: cloudNodes) {
			Point3i ijk = this.getNodeStructure().getIJKFromNodeNumber(node);
			if(!ijs.containsKey(ijk.getI()))
				ijs.put(ijk.getI(), new ArrayList<Integer>());
			if(!ijs.get(ijk.getI()).contains(ijk.getJ()))
				ijs.get(ijk.getI()).add(ijk.getJ());
		}
		return ijs;
	}
	
	/**
	 * Returns a list of affordable unoccupied node numbers.
	 * All nodes will be in the cloud and unoccupied.
	 * 
	 * @param sensorType
	 * @param configuration
	 * @return
	 */
	public List<Integer> getValidNodes(String sensorType, ExtendedConfiguration configuration, boolean withAddPoint, boolean cost, boolean wellConstraint) {
		
		List<Integer> validNodes = new ArrayList<Integer>();
		
		// Make sure we can afford adding a new sensor of the given type
		if(cost) {
			float configurationCost = getSensorCost(configuration);
			float sensorCost = getSensorSettings(sensorType).getSensorCost();
			
			if(configurationCost+sensorCost > getSensorCostConstraint())
				return validNodes; // Can't afford a new sensor of this type
		}
		
		List<Well> wells = configuration.getWells();	
		
		int tempMaxWells = maxWells;
				
		List<Integer> cloudNodes = new ArrayList<Integer>();
		for(Integer node: sensorSettings.get(sensorType).getValidNodes()) 
			cloudNodes.add(node);
		
		// Remove all but edges
		if(edgeMovesOnly) {
			List<Integer> tempNodes = new ArrayList<Integer>();
			tempNodes.addAll(cloudNodes);
			for(int tempNode: tempNodes) {
				// All neighbors
				List<Integer> allNeighbors = getNodeStructure().getNeighborNodes(getNodeStructure().getIJKFromNodeNumber(tempNode));
				// count neighbors in cloud
				int neighbors = 0;
				for(int node: tempNodes) {
					if(allNeighbors.contains((Object)node))
						neighbors++;
				}
				boolean is2D = true;
				if(neighbors == (is2D ? 8 : 26)) {
					cloudNodes.remove((Object)tempNode);
				}
			}
		}
		
		//System.out.println("Spots I can move to: " + cloudNodes.size());
		List<Integer> occupiedNodes = configuration.getSensorPositions(sensorType);
//		System.out.println("Cloud nodes after occupied check: " + cloudNodes.size());
		for(Integer node: cloudNodes) {
			// Sensor is unoccupied
//			if(!occupiedNodes.contains(node) || (withAddPoint && node.equals(getNodeStructure().getNodeNumber(addPoint)))) { //old addpoint logic
			if(!occupiedNodes.contains(node)) {
			if(wellConstraint) {
					if(wells.size() < tempMaxWells) // We can add a new well
						validNodes.add(node);
					else {
						// We can't create another well, lets see if there is a spot left in one that already exists
						Point3i point = getNodeStructure().getIJKFromNodeNumber(node);
						for(Well well: configuration.getWells()) {
							if(well.getI() == point.getI() && well.getJ() == point.getJ()) {
								validNodes.add(node); // Well is at node point
								break;
							}
						}
					}
				} else {
					validNodes.add(node);
				}
			}
		}
		
		
		// if you want to run the "old" way, comment out everything below this until the matching comment
		
		//Find all well locations
		HashMap<Point3i, Boolean> locations = new HashMap<Point3i, Boolean>();
		//Initialize all (i,j)s to be allowed
		for(int i=0; i <= getNodeStructure().getIJKDimensions().getI(); i++)
			for(int j=0; j<= getNodeStructure().getIJKDimensions().getJ(); j++)
				locations.put(new Point3i(i, j, 1), true);
		//Set everything within exclusionRadius to false for each well
		//Note that we don't have to check to make sure that a point to be set false isn't a well, because there should be no way that it was there in the first place.
		for(Well well : configuration.getWells()){
			if(well.i == 0 || well.j == 0){
				continue;
			} //makes sure we're not looking at the add point. Can probably remove.
			Point3f wellxyz = getNodeStructure().getNodeCenteredXYZFromIJK(new Point3i(well.i, well.j, 1));
			for(int i=1; i <= getNodeStructure().getIJKDimensions().getI(); i++){
				for(int j=1; j<= getNodeStructure().getIJKDimensions().getJ(); j++){
					Point3f otherxyz = getNodeStructure().getNodeCenteredXYZFromIJK(new Point3i(i, j, 1));
					float distance = otherxyz.euclideanDistance(wellxyz);
					if(distance <= exclusionRadius || distance >= inclusionRadius){
						if(otherxyz.equals(wellxyz)){ //are we looking at this well?
							//NOTE: This logic would make more sense up above in this function, but this keeps it all in one place.
							continue;
						}
						else{ //if not, reject it by default
							locations.put(new Point3i(i,j,1), false);
						}
					}
				}
			}
		}
		List<Integer> tempValidNodes = new ArrayList<Integer>();
		for(Integer node: validNodes){
			if(locations.get(new Point3i(getNodeStructure().getIJKFromNodeNumber(node).getI(),getNodeStructure().getIJKFromNodeNumber(node).getJ(),1))){
				tempValidNodes.add(node);
			}
		}
		return tempValidNodes;
		
		//This is the matching comment. Uncomment the line below this two for it to work like it used to.
		//return validNodes;
	}
	
	public List<String> getValidSwitchTypes(String currentType, ExtendedConfiguration configuration){
		//Get all candidate types
		List<String> types = getDataTypes();
		//Decrement the current cost by the sensor whose type we might be changing
		float configurationCost = getSensorCost(configuration);
		configurationCost -= getSensorSettings(currentType).getSensorCost();
		List<String> toRemove = new ArrayList<String>();
		for(String type : types)
			if(configurationCost + getSensorSettings(type).getSensorCost() > getSensorCostConstraint()) toRemove.add(type);
		for(String type : toRemove)
			types.remove(type);
		return types;
	}
	
	public List<String> getAllPossibleDataTypes() {
		return nodeStructure.getDataTypes();
	}

	public void resetSensorSettings(String type) {
		if(sensorSettings.containsKey(type))
			return; // Keep those
		sensorSettings.put(type, new SensorSetting(nodeStructure, type));	// User should adjust these settings
	}

	public SensorSetting getSensorSettings(String sensorType) {
		return sensorSettings.get(sensorType);
	}
	
	public Map<String, SensorSetting> getSensorSettings() {
		return sensorSettings;
	}
	
	public void addSensorSetting(String name, String type) {
		sensorSettings.put(name, new SensorSetting(nodeStructure, type));	// User should adjust these settings
	}
	
	public void addSensorSetting(String type, String trigger, String threshold) {
		sensorSettings.put(type, new SensorSetting(nodeStructure, type, trigger, threshold));	// User should adjust these settings
	}
	
	public void removeSensorSettings(String dataType) {
		sensorSettingsRemoved.put(dataType, sensorSettings.get(dataType));
		sensorSettings.remove(dataType);
	}
	
	public SensorSetting getRemovedSensorSettings(String sensorType) {
		return sensorSettingsRemoved.get(sensorType);
	}
	
	public void resetRemovedSensorSettings() {
		sensorSettingsRemoved = new HashMap<String, SensorSetting>();
	}
	
	public List<String> getDataTypes() {
		return new ArrayList<String>(sensorSettings.keySet());
	}	
	
	public Float getTTD(String specificType, String scenario, Integer nodeNumber) {
		return detectionMap.get(specificType).get(scenario).get(nodeNumber);
	}
	
	public void setDetectionMap(Map<String, Map<String, Map<Integer, Float>>> detectionMap) {
		this.detectionMap = detectionMap;
	}
	
	public Map<String, Map<String, Map<Integer, Float>>> getDetectionMap() {
		return detectionMap;
	}
	
	public float getTotalScenarioWeight() {
		float totalScenarioWeight = 0;
		for(float value: scenarioWeights.values()) totalScenarioWeight += value;
		return totalScenarioWeight;
	}
}
