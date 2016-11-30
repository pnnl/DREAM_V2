package objects;

import hdf5Tool.HDF5Wrapper;

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
 *
 */
public class ScenarioSet {

	private boolean runLoaded;
	private boolean defaultWeights;
	private boolean isReady;
		
	/**
	 *  Scenarios must share common node structure
	 */
	private NodeStructure nodeStructure;
	private List<Scenario> allScenarios;
	private List<Scenario> scenarios;
	
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
	private boolean allowMultipleSensorsInWell;
	
	private Map<Scenario, Float> scenarioWeights;
	private Map<String, SensorSetting> sensorSettings;
	
	private InferenceTest inferenceTest;
	
	private boolean edgeMovesOnly = false;
	
	/**
	 * Once the algorithm is running
	 */
	private List<Well> wells;
	
	public ScenarioSet() {
		
		runLoaded = false;
		defaultWeights = true;
		isReady = false;

		scenarios = new ArrayList<Scenario>();
		allScenarios = new ArrayList<Scenario>();
		scenarioWeights = new HashMap<Scenario, Float>();
		sensorSettings = new HashMap<String, SensorSetting>();

		addPoint = new Point3i(1,1,1);
		maxWells = 10;
		iterations = 1000;
		costConstraint = 300;
		exclusionRadius = 0;
		inclusionRadius = Float.MAX_VALUE;
		wellCost = 65;
		allowMultipleSensorsInWell = true;
		
		wells = new ArrayList<Well>();
		
		Constants.log(Level.INFO, "Scenario set: initialized", null);
		Constants.log(Level.CONFIG, "Scenario set: configuration", this);
		
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("Scenario set:\r\n");
		
		if(isRunLoaded()) {
			builder.append("\tData loaded:\r\n");
			builder.append("\t\t" + nodeStructure.getRun() + "\r\n");
			builder.append("\tScenarios: " + scenarios.toString() + "\r\n");
			if(defaultWeights) {
				builder.append("\tDefault weights: ");
			} else {
				builder.append("\tWeights: ");
			}
			builder.append(scenarioWeights.toString() + "\r\n");
		} else {
			builder.append("\tNot ready - no data loaded.\r\n");
		}
		
		if(!isReady()) {
			builder.append("\tUser settings not set - using defaults:\r\n");
		} else {
			builder.append("\tUser settings:\r\n");
		}

		builder.append("\t\tAdd point: " + addPoint.toString() + "\r\n");
		builder.append("\t\tMax wells: " + maxWells + "\r\n");
		builder.append("\t\tIterations: " + iterations + "\r\n");
		builder.append("\t\tCost constraint: " + costConstraint + "\r\n");
		builder.append("\t\tExclusion radius: " + exclusionRadius + "\r\n");
		if(Constants.buildDev){
			builder.append("\t\tCost per unit depth of well: " + wellCost + "\r\n");
		}
		builder.append("\t\tAllow multiple sensors in well: " + allowMultipleSensorsInWell + "\r\n");
		builder.append(getInferenceTest());

		return builder.toString();

	}
	
	public void setUserSettings(Point3i addPoint, int maxWells, float costConstraint, float exclusionRadius, float wellCost, boolean allowMultipleSensorsInWell) {
	
		Constants.log(Level.INFO, "Scenario set: setting user settings", null);
		
		this.addPoint = addPoint;
		this.maxWells = maxWells;
		this.costConstraint = costConstraint;
		this.exclusionRadius = exclusionRadius;
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
		allScenarios.clear();
		scenarios.clear();
		
		Constants.log(Level.INFO, "Scenario set: loading run data", run);
		
		String query =  "SELECT has_scenarios, scenario_names FROM run WHERE run_name='" + run + "'";
		
		Constants.log(Level.FINE, "Scenario set: QUERY", query);
		
		List<String> scenarios = HDF5Wrapper.queryScenarioNamesFromFiles();

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
		
		for(final String type: nodeStructure.getDataTypes()) {
			sensorSettings.put(type, new SensorSetting(nodeStructure, ScenarioSet.this, type, ScenarioSet.this.scenarios));	// User should adjust these settings
		}

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

	public void setScenarioWeights(Map<Scenario, Float> scenarioWeights) {
		this.scenarioWeights = scenarioWeights;
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

	
	public void setInferenceTest(InferenceTest test) {
		this.inferenceTest = test;
	}
	
	public InferenceTest getInferenceTest() {
		return inferenceTest;
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
			cost += (maxZ - this.getNodeStructure().getXYZEdgeFromIJK(location).getZ()) * this.wellCost;
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
	
	public Map<Integer, List<Integer>> getAllPossibleWells() {
		List<Integer> cloudNodes = new ArrayList<Integer>();
		for(String sensorType: this.getSensorSettings().keySet()) {
			for(Integer node: sensorSettings.get(sensorType).getValidNodes(null)) 
				cloudNodes.add(node);
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
		
		//old addpoint logic
//		if(withAddPoint)
//			cloudNodes.add(getNodeStructure().getNodeNumber(addPoint));
		
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
			if(Constants.hdf5CloudData.containsKey(scenario) && Constants.hdf5CloudData.get(scenario).containsKey(type)) {
				Constants.hdf5CloudData.get(scenario).get(type).clear();
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
		sensorSettings.remove(dataType);
	}

	public void setSensorSettings(Map<String, SensorSetting> sensorSettings) {
		this.sensorSettings = sensorSettings;
	}
	
	public List<String> getDataTypes() {
		return new ArrayList<String>(sensorSettings.keySet());
	}	
	
	public static void main(String[] args) {
		ScenarioSet set = new ScenarioSet();
		set.loadRunData(Constants.RUN_TEST);
	}

	public void setScenarioWeight(Scenario scenario, float weight) {
		if(scenarioWeights.containsKey(scenario) && Float.compare(scenarioWeights.get(scenario), weight) == 0)
			return; // Nothing new
		defaultWeights = false;
		scenarioWeights.put(scenario, weight);
	}

	public void removeScenario(Scenario scenario) {
		// Remove the given scenario from all maps
		scenarios.remove(scenario);
		scenarioWeights.remove(scenario);
	}

	public void clearRun() {
		runLoaded = false;
		defaultWeights = true;
		isReady = false;

		scenarios.clear();
		allScenarios.clear();
		scenarioWeights.clear();
		sensorSettings.clear();

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

	
	



}
