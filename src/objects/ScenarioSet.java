package objects;

import hdf5Tool.HDF5Interface;
import objects.SensorSetting.Trigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import utilities.Constants;
import utilities.Constants.ModelOption;
import utilities.Point3f;
import utilities.Point3i;


/**
 * Selections made by the user for a given run should be set here.
 * @author port091
 * @author rodr144
 *
 */
public class ScenarioSet {

	private boolean runLoaded;
	private boolean isReady;
		
	/**
	 *  Scenarios must share common node structure
	 */
	private NodeStructure nodeStructure;
	private List<Scenario> allScenarios;
	private List<Scenario> scenarios;
	private List<String> sensors;
	
	/**
	 * User settings - 
	 */
	private Point3i addPoint;
	private int maxWells;
	private int iterations;
	private float costConstraint;
	private float exclusionRadius;
	private float inclusionRadius;
	private float wellCost;
	private float wellDepthCost;
	private float remediationCost;
	private boolean allowMultipleSensorsInWell;
	private String scenarioEnsemble;
	
	private Map<Scenario, Float> scenarioWeights;
	private Map<String, SensorSetting> sensorSettings;
	//Finding nodes removes some sensor settings, causing problems going back a page and then forward again
	//When removing sensor settings, they are instead saved in this Hashmap
	private Map<String, SensorSetting> sensorSettingsRemoved;
	
	private InferenceTest inferenceTest;
	
	private boolean edgeMovesOnly = false;
	
	private List<Well> wells;
	
	/**
	 * Once the algorithm is running
	 */
	
	
	public ScenarioSet() {
		
		runLoaded = false;
		isReady = false;
		
		scenarios = new ArrayList<Scenario>();
		allScenarios = new ArrayList<Scenario>();
		scenarioWeights = new HashMap<Scenario, Float>();
		sensorSettings = new HashMap<String, SensorSetting>();
		sensorSettingsRemoved = new HashMap<String, SensorSetting>();
		sensors = new ArrayList<String>();
		
		scenarioEnsemble = "";
		addPoint = new Point3i(1,1,1);
		maxWells = 10;
		iterations = 1000;
		costConstraint = 300;
		exclusionRadius = 0;
		inclusionRadius = Float.MAX_VALUE;
		wellCost = 0;
		wellDepthCost = 0;
		remediationCost = 0;
		allowMultipleSensorsInWell = true;
		
		wells = new ArrayList<Well>();
		
		Constants.log(Level.INFO, "Scenario set: initialized", null);
		Constants.log(Level.CONFIG, "Scenario set: configuration", this);
	}
	
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("———— Input Summary ————\r\n");
		
		// Details about the scenario set read from the file being used
		builder.append("Scenario ensemble: " + scenarioEnsemble + "\r\n");
		builder.append("Scenario weights:\r\n");
		for(Scenario scenario: scenarios)
			builder.append("\t" + scenario + " = " + scenarioWeights.get(scenario) + "\r\n");
		
		// Leakage criteria
		builder.append("Sensor settings:\r\n");
		for(String parameter: sensorSettings.keySet()) {
			builder.append("\t" + parameter + ":\r\n");
			builder.append("\t\tAlias: " + Sensor.sensorAliases.get(parameter) + "\r\n");
			builder.append("\t\tCost: " + sensorSettings.get(parameter).getCost() + " per sensor\r\n");
			if (parameter.contains("Electrical Conductivity"))
				builder.append("\t\tTriggering on: ERT matrix\r\n");
			else
				builder.append("\t\tTriggering on: " + sensorSettings.get(parameter).getTrigger() + "\r\n");
			if (parameter.contains("Electrical Conductivity"))
				builder.append("\t\tLeakage threshold: ERT matrix\r\n");
			else if(sensorSettings.get(parameter).getTrigger() == Trigger.MAXIMUM_THRESHOLD)
				builder.append("\t\tLeakage threshold: " + sensorSettings.get(parameter).getUpperThreshold() + "\r\n");
			else if(sensorSettings.get(parameter).getTrigger() == Trigger.MINIMUM_THRESHOLD)
				builder.append("\t\tLeakage threshold: " + sensorSettings.get(parameter).getLowerThreshold() + "\r\n");
			else if(sensorSettings.get(parameter).getTrigger() == Trigger.ABSOLUTE_DELTA)
				builder.append("\t\tLeakage threshold: Change in " + sensorSettings.get(parameter).getLowerThreshold() + "\r\n");
			else if(sensorSettings.get(parameter).getTrigger() == Trigger.RELATIVE_DELTA)
				builder.append("\t\tLeakage threshold: Change in " + sensorSettings.get(parameter).getLowerThreshold() + "%\r\n");
			builder.append("\t\tZone bottom: " + sensorSettings.get(parameter).getThisMinZ() + "\r\n");
			builder.append("\t\tZone top: " + sensorSettings.get(parameter).getThisMaxZ() + "\r\n");
			if(sensorSettings.get(parameter).areNodesReady()) {
				int size = nodeStructure.getIJKDimensions().getI() * nodeStructure.getIJKDimensions().getJ() * nodeStructure.getIJKDimensions().getK();
				builder.append("\t\tValid nodes: " + sensorSettings.get(parameter).getValidNodes(null).size() + " of " + size + "\r\n");
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
		builder.append("\tSensor budget: " + costConstraint + "\r\n");
		builder.append("\tMax wells: " + maxWells + "\r\n");
		builder.append("\tMax distance between wells: " + exclusionRadius + "\r\n");
		if(Constants.buildDev){
			builder.append("\tCost per well: " + wellCost + "\r\n");
			builder.append("\tCost per unit depth of well: " + wellDepthCost + "\r\n");
			builder.append("\tRemediation cost per water unit: " + remediationCost + "\r\n");
		}
		builder.append("\tAllow multiple sensors in well: " + allowMultipleSensorsInWell + "\r\n");

		return builder.toString();
	}
	
	public void setUserSettings(Point3i addPoint, int maxWells, float costConstraint, float exclusionRadius, float wellCost, float wellDepthCost, float remediationCost, boolean allowMultipleSensorsInWell) {
		
		Constants.log(Level.INFO, "Scenario set: setting user settings", null);
		
		this.addPoint = addPoint;
		this.maxWells = maxWells;
		this.costConstraint = costConstraint;
		this.exclusionRadius = exclusionRadius;
		this.wellCost = wellCost;
		this.wellDepthCost = wellDepthCost;
		this.remediationCost = remediationCost;
		this.allowMultipleSensorsInWell = allowMultipleSensorsInWell;
		isReady = true;
		//TEST!!!
		//for(SensorSetting setting: sensorSettings.values()){
		//	float depth = SensorSetting.getMaxZ() - setting.getThisMinZ();
		//	inclusionRadius = Math.min(inclusionRadius, depth*(2f/3f));
		//}
		//ENDTEST!!!
		
		Constants.log(Level.CONFIG, "Scenario set: configuration", this);
	}

	
	/**
	 * Loads data from the database, sets up scenarios
	 */
	public void loadRunData(String run) {
		
		nodeStructure = new NodeStructure(run);
		sensorSettings.clear();
		sensors.clear();
		allScenarios.clear();
		scenarios.clear();
		
		Constants.log(Level.INFO, "Scenario set: loading run data", run);
		
		String query =  "SELECT has_scenarios, scenario_names FROM run WHERE run_name='" + run + "'";
		
		Constants.log(Level.FINE, "Scenario set: QUERY", query);
		
		List<String> scenarios = HDF5Interface.queryScenarioNamesFromFiles();
		
		if(!scenarios.isEmpty()) {
			for(String scenario: scenarios) {
				Scenario s = new Scenario(scenario);
				this.scenarios.add(s);
				this.allScenarios.add(s);
			}
			for(Scenario scenario: this.scenarios) {
				scenarioWeights.put(scenario, (float)1);
			}
		} 
		
		for(final String type: nodeStructure.getDataTypes())
			sensorSettings.put(type, new SensorSetting(nodeStructure, ScenarioSet.this, type, ScenarioSet.this.scenarios));	// User should adjust these settings
		
		// Setup the inference test
		inferenceTest = new InferenceTest(sensorSettings.keySet());
		
		runLoaded = true;
		
		Constants.log(Level.CONFIG, "Scenario set: loaded run data", this);
	}

	/**					**\
	 * Getters & Setters *
	 * 					 *
	\*					 */
	
	public boolean isReady() {
		return isReady;
	}

	public List<Scenario> getScenarios() {
		return scenarios;
	}

	public void setScenarios(List<Scenario> scenarios) {
		this.scenarios = scenarios;
	}
	
	public String getScenarioEnsemble() {
		return scenarioEnsemble;
	}
	
	public void setScenarioEnsemble(String scenarioEnsemble) {
		this.scenarioEnsemble = scenarioEnsemble;
	}

	public List<Scenario> getAllScenarios() {
		return allScenarios;
	}

	public void setAllScenarios(List<Scenario> scenarios) {
		this.allScenarios = scenarios;
	}

	public float getGloballyNormalizedScenarioWeight(Scenario scenario) {
		return scenarioWeights.get(scenario) / getTotalScenarioWeight();
	}
	
	public Map<Scenario, Float> getScenarioWeights() {
		return scenarioWeights;
	}

	public void setScenarioWeights(Scenario scenario, float weight) {
		this.scenarioWeights.replace(scenario, weight);
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

	public boolean getAllowMultipleSensorsInWell() {
		return allowMultipleSensorsInWell;
	}

	public void setAllowMultipleSensorsInWell(boolean allowMultipleSensorsInWell) {
		this.allowMultipleSensorsInWell = allowMultipleSensorsInWell;
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
	
	public void setInferenceTest(InferenceTest test) {
		this.inferenceTest = test;
	}

	public float getCost(ExtendedConfiguration configuration) {
		float cost = 0;
		for(ExtendedSensor sensor: configuration.getExtendedSensors()) {
			cost += getCost(sensor.getSensorType());
		}
		return cost;
	}

	public float getCost(String type) {
		return getSensorSettings(type).getCost();
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
	
	/*
	 * This returns the cost for post-processing cost-analysis.
	 * TODO: eliminate any redundancy that might be in place with above getCost functions.
	 */
	public float costOfConfiguration(Configuration configuration){
		float cost = 0;
		List<Sensor> sensors = configuration.getSensors();
		List<Point3i> locations = new ArrayList<Point3i>();
		boolean foundWell = false;
		for(Sensor sensor: sensors){
			foundWell = false;
			Point3i point = sensor.getIJK();
			for(Point3i location: locations){
				if(location.getI() == point.getI() && location.getJ() == point.getJ()){
					//this is at least the second sensor in the well
					// k=0 is the lowest point! (or the most-negative point is the lowest)
					if(point.getK() < location.getK()){
						locations.remove(location);
						locations.add(point);
					}
					foundWell = true;
					break;
				}
			}
			if(!foundWell){
				locations.add(point);
			}
		}
		for(Point3i location: locations){
			float maxZ = SensorSetting.globalMaxZ > 0 ? SensorSetting.globalMaxZ : 0; //Use 0 as the top if the locations are negative, otherwise use globalMaxZ
			cost += (maxZ - this.getNodeStructure().getXYZEdgeFromIJK(location).getZ()) * this.wellDepthCost;
		}
		return cost;
	}
	
	public float getCostConstraint() {
		return costConstraint;
	}

	public void setCostConstraint(float costConstraint) {
		this.costConstraint = costConstraint;
	}

	public List<Well> getWells() {
		return wells;
	}

	public void setWells(List<Well> wells) {
		this.wells = wells;
	}

	public NodeStructure getNodeStructure() {
		return nodeStructure;
	}

	public void setNodeStructure(NodeStructure nodeStructure) {
		this.nodeStructure = nodeStructure;
	}

	public boolean isRunLoaded() {
		return runLoaded;
	}
	
	public Map<Integer, List<Integer>> getAllPossibleWells(ModelOption modelOption) {
		List<Integer> cloudNodes = new ArrayList<Integer>();
		
		if(modelOption != ModelOption.ALL_SENSORS){
			for(String sensorType: this.getSensorSettings().keySet()) {
				for(Integer node: sensorSettings.get(sensorType).getValidNodes(null)) 
					cloudNodes.add(node);
			}
		}
		else{
			for(Integer node: sensorSettings.get("all").getValidNodes(null)) cloudNodes.add(node);
		}
		Map<Integer, List<Integer>> ijs = new HashMap<Integer, List<Integer>>();
		for(Integer node: cloudNodes) {
			Point3i ijk = this.getNodeStructure().getIJKFromNodeNumber(node);
			if(!ijs.containsKey(ijk.getI())) {
				ijs.put(ijk.getI(), new ArrayList<Integer>());
			}
			if(!ijs.get(ijk.getI()).contains(ijk.getJ()))
				ijs.get(ijk.getI()).add(ijk.getJ());
		}
		return ijs;
		/*
		List<Well> wells = new ArrayList<Well>();
		for(Integer i: ijs.keySet()) {
			for(Integer j: ijs.get(i)) {
				wells.add(new Well(i,j, this));
			}
		}
		return wells;
		*/
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
			float configurationCost = getCost(configuration);
			float sensorCost = getCost(sensorType);
			
			if(configurationCost+sensorCost > getCostConstraint())
				return validNodes; // Can't afford a new sensor of this type
		}
		
		List<Well> wells = configuration.getWells();	
		
		int tempMaxWells = maxWells;
				
		List<Integer> cloudNodes = new ArrayList<Integer>();
		for(Integer node: sensorSettings.get(sensorType).getValidNodes(null)) 
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
							if(allowMultipleSensorsInWell) continue; //if we're allowing multiple, then we don't want to exclude this location no matter what
							for(ExtendedSensor sensor : well.sensors){ //we need to determine if this well already has a sensor of this type
								if(sensor.type.equals(sensorType)){
									locations.put(new Point3i(i,j,1), false); //it does, so exclude it
									continue;
								}
								//it doesn't, so allow it (do nothing)
							}
						}
						else{
							//if not, reject it by default
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
		float configurationCost = getCost(configuration);
		configurationCost -= getCost(currentType);
		List<String> toRemove = new ArrayList<String>();
		for(String type : types)
			if(configurationCost + getCost(type) > getCostConstraint()) toRemove.add(type);
		for(String type : toRemove)
			types.remove(type);
		return types;
	}
	
	public List<String> getAllPossibleDataTypes() {
		return nodeStructure.getDataTypes();
	}

	public void resetSensorSettings(String type, float min, float max) {
		if(sensorSettings.containsKey(type))
			return; // Keep those
		for(Scenario scenario: scenarios) {
			if(HDF5Interface.hdf5CloudData.containsKey(scenario) && HDF5Interface.hdf5CloudData.get(scenario).containsKey(type)) {
				HDF5Interface.hdf5CloudData.get(scenario).get(type).clear();
			}
		}
		sensorSettings.put(type, new SensorSetting(nodeStructure, this, type, this.scenarios, min, max));	// User should adjust these settings
	}

	public SensorSetting getSensorSettings(String sensorType) {
		return sensorSettings.get(sensorType);
	}
	
	public Map<String, SensorSetting> getSensorSettings() {
		return sensorSettings;
	}
	
	public void addSensorSetting(String name, String type){
		sensorSettings.put(name, new SensorSetting(nodeStructure, ScenarioSet.this, type, ScenarioSet.this.scenarios));	// User should adjust these settings
	}
	
	public void removeSensorSettings(String dataType) {
		sensorSettingsRemoved.put(dataType, sensorSettings.get(dataType));
		sensorSettings.remove(dataType);
	}

	public void setSensorSettings(Map<String, SensorSetting> sensorSettings) {
		this.sensorSettings = sensorSettings;
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
	
	public void setSensors(List<String> sensors) {
		this.sensors = sensors;
	}

	public List<String> getSensors() {
		return sensors;
	}
	
	public static void main(String[] args) {
		ScenarioSet set = new ScenarioSet();
		set.loadRunData(Constants.RUN_TEST);
	}

	public void removeScenario(Scenario scenario) {
		// Remove the given scenario from all maps
		scenarios.remove(scenario);
		scenarioWeights.remove(scenario);
	}

	public void clearRun() {
		runLoaded = false;
		isReady = false;
		
		scenarios.clear();
		allScenarios.clear();
		scenarioWeights.clear();
		sensorSettings.clear();
		sensors.clear();
		
		addPoint = new Point3i(0,0,0);
		maxWells = 10;
		iterations = 1000;
		costConstraint = 300;
		
		wells.clear();
		
		Constants.log(Level.INFO, "Scenario set: re-initialized", null);
		Constants.log(Level.CONFIG, "Scenario set: configuration", this);
	}

	public void setAddPoint(Point3i addPoint) {
		this.addPoint = new Point3i(addPoint);
	}

	public float getTotalScenarioWeight() {
		float totalScenarioWeight = 0;
		for(float value: scenarioWeights.values()) totalScenarioWeight += value;
		return totalScenarioWeight;
	}

	public float getMinWellCost() {
		float countMinCost = 0;
		int count = 0;
		float min = Integer.MAX_VALUE;
		for(String sensor: sensorSettings.keySet()) {
			countMinCost += inferenceTest.getMinimumForType(sensor) * sensorSettings.get(sensor).getCost();
			count += inferenceTest.getMinimumForType(sensor);
			if(sensorSettings.get(sensor).getCost() < min)
				min = sensorSettings.get(sensor).getCost();
		}
		if(inferenceTest.getOverallMinimum() > count)
			countMinCost += min * (inferenceTest.getOverallMinimum() - count);
		return countMinCost;
	}
}
