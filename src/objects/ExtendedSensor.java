package objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import utilities.Constants;
import utilities.Point3i;

/**
 * Extended version of the Sensor class, provides more functionality and information.
 * @author port091
 * @author rodr144
 */

public class ExtendedSensor extends Sensor {
	
    private Well well; // Reference to well, may be null
    
    private Map<String, Map<Integer, Double>> history;
    
    // If this sensor has reached inference
    private boolean triggering;
    
    // Which scenarios it was used to determine inference
    private Map<String, TreeMap<TimeStep, Double>> scenariosUsed;
    
    // Well pairing for E4D
    private int nodePairNumber;
    
    
    public ExtendedSensor(int i, int j, int k, String type, NodeStructure domain) {
    	super(i, j, k, type, domain);
    	
    	scenariosUsed = Collections.synchronizedMap(new HashMap<String, TreeMap<TimeStep, Double>>());
    	history = Collections.synchronizedMap(new HashMap<String, Map<Integer, Double>>());
    	
    	triggering = false;
    	well = null;
    }
    
    public ExtendedSensor(int nodeNumber, String type, NodeStructure domain) {
    	super(nodeNumber, type, domain);
    	
    	scenariosUsed = Collections.synchronizedMap(new HashMap<String, TreeMap<TimeStep, Double>>());

    	triggering = false;
    	well = null;
    }
    
    public ExtendedSensor(ExtendedSensor toCopy) {
    	super(toCopy);
    	
    	scenariosUsed = Collections.synchronizedMap(new HashMap<String, TreeMap<TimeStep, Double>>());
    	history = Collections.synchronizedMap(new HashMap<String, Map<Integer, Double>>());
    	scenariosUsed = toCopy.getScenariosUsed();
    	
    	triggering = toCopy.isInferred();
    	well = null;    
    	nodePairNumber = toCopy.getNodePairNumber();
    }
    
	public ExtendedSensor makeCopy() {
		return new ExtendedSensor(this);
	}
    
    @Override
    public String toString() {    	
    	String type = (getSensorType() + ":          ").substring(0, 10);
    	String ijks = (node.toString() + ",                    ").substring(0, 20);
    	String xyzs = (point != null) ? (point.toString() + ",                                        ").substring(0, 40) : "";
    	String nnum =  (nodeNumber + ",          ").substring(0, 10);
    	StringBuilder str = new StringBuilder();
    	for(String scenario: scenariosUsed.keySet()) {
    		str.append("\t" + scenario + ": ");
    		for(TimeStep ts:scenariosUsed.get(scenario).keySet()) {
    			str.append("[" + ts.getTimeStep() + ": " + Constants.decimalFormat.format(scenariosUsed.get(scenario).get(ts)) + "], ");
    		}
    	}
    	str.append("\n");
    	return type + ijks + xyzs + nnum + "\ttriggering = " + triggering + "\t" + str.toString();
    }
    
    public ExtendedSensor clone() {
    	return this.clone(); // Note: References will not be duplicated
    }
    
    // Saves the node pair for ERT technology (k = 1)
    public void setNodePair(int nodeNumber) {
    	nodePairNumber = nodeNumber;
    }
    
    // Calls the well pairing for ERT technology (k = 1)
    public int getNodePairNumber() {
		return nodePairNumber;
	}
    
	public void setWell(Well well) {
		this.well = well;
	}     

	public Well getWell() {
		return well;
	}
	
	public synchronized boolean isInferred() {
		return triggering;
	}
	
	public synchronized void setTriggered(boolean b) {
		this.triggering = b;		
	}
	
	public synchronized void clearHistory() {
		if(history == null)
			history = Collections.synchronizedMap(new HashMap<String, Map<Integer, Double>>());
	
		history.clear();
	}
	
	public boolean isInCloud(ScenarioSet scenarioSet) {		
		return scenarioSet.getSensorSettings(getSensorType()).getValidNodes().contains(getNodeNumber());
	}
	
	public synchronized boolean isTriggeredInScenario(String scenario) {
		return scenariosUsed.containsKey(scenario);
	}
	
	public synchronized Map<String, TreeMap<TimeStep, Double>> getScenariosUsed() {
		return new HashMap<String, TreeMap<TimeStep, Double>>(scenariosUsed);
	}
	
	public synchronized void clearScenariosUsed() {
		scenariosUsed.clear();
		triggering = false;
	}

	/**
	 * Verifies there is a spot to jump in the cloud, and randomly selects one to move to
	 * @param spots
	 * @param configuration
	 * @param set
	 * @return
	 */
	public boolean move(ExtendedConfiguration configuration, ScenarioSet set) {

		if(Double.compare(Constants.random.nextDouble(), .8) < 0) {
			if(move(set.getNodeStructure().getNeighborNodes(node), configuration, set))
				return true;
		}
		
		
		// Get all sensor types that we can afford to switch to
		List<String> validTypes = set.getValidSwitchTypes(getSensorType(), configuration);
		Collections.shuffle(validTypes, Constants.random);
		
		for(String type : validTypes){
			// Get all the nodes we can afford to move to that are in the cloud and unoccupied
			List<Integer> validMoves = set.getValidNodes(type, configuration, false, false, !isWell());
			
			// Remove our current location
			if(validMoves.contains(getNodeNumber()))
				validMoves.remove(getNodeNumber());
			
			// If there's somewhere to put a sensor of this type, do it.
			if(!validMoves.isEmpty()){
				//System.out.println("changing a sensor of type " + getSensorType() + " to " + type);
				this.type = type;
				move(validMoves.get(Constants.random.nextInt(validMoves.size())), set.getNodeStructure());
				return true;
			}
		}
		// There must be no other spot to put a sensor
		return false;
	}
	

	public static boolean move(ArrayList<ExtendedSensor> sensors, ExtendedConfiguration configuration, ScenarioSet set) {

		ExtendedSensor sensor = sensors.get(0);
		
		// Get all the nodes we can afford to move to that are in the cloud and unoccupied
		List<Integer> validMoves = set.getValidNodes("all", configuration, false, false, false);
		
		// Remove our current location
		if(validMoves.contains(sensor.getNodeNumber()))
			validMoves.remove(sensor.getNodeNumber());
		
		// If there's somewhere to put a sensor of this type, do it.
		if(!validMoves.isEmpty()){
			Integer newSpot = validMoves.get(Constants.random.nextInt(validMoves.size()));
			for(ExtendedSensor extSensor: sensors){
				extSensor.moveTo(newSpot, set.getNodeStructure());
			}
			return true;
		}
		// There must be no other spot to put a sensor
		return false;
	}
	
	/**
	 * Verifies there is a spot to jump in the given list, and randomly selects one to move to
	 * @param spots
	 * @param configuration
	 * @param set
	 * @return
	 */
	public boolean move(List<Integer> spots, ExtendedConfiguration configuration, ScenarioSet set) {
		
		// Get all the nodes we can afford to move to that are in the cloud and unoccupied
		List<Integer> validNodes = set.getValidNodes(getSensorType(), configuration, false, false, !isWell());

		// Remove our current location
		if(validNodes.contains(getNodeNumber()))
			validNodes.remove(getNodeNumber());
				
		// Go through the list of spots, see if any are unoccupied and in the cloud
		List<Integer> validMoves = new ArrayList<Integer>();
		for(Integer spot: spots) {
			if(validNodes.contains(spot)) {
				validMoves.add(spot);
			}
		}
		
		// If there are no spots, we can't move, return false
		if(validMoves.isEmpty())
			return false;
		
		// Otherwise go ahead and make the move
		move(validMoves.get(Constants.random.nextInt(validMoves.size())), set.getNodeStructure());
		
		return true;
	}
	
	/**
	 * Returns true if we can confirm this is the only sensor in the well
	 * @return
	 */
	public boolean isWell() {
		if(well == null)
			return false;
		if(well.getSensors().size() == 1 && well.getSensors().get(0).equals(this))
			return true;
		return false;
	}
	
	public void moveTo(Integer nodeNumber, NodeStructure domain) {
		move(nodeNumber, domain);	
	}

	private void move(int nodeNumber, NodeStructure domain) {
    	this.nodeNumber = nodeNumber;
    	node = domain.getIJKFromNodeNumber(nodeNumber);
    	point = domain.getXYZEdgeFromIJK(node);
    	
    	clearScenariosUsed();
    	clearHistory();
    	
    	setTriggered(false);
    
    	// this is the only sensor in the well
    	if(isWell()) {
    		well.moveTo(getIJK());
    	} else {
    		well = null;
    	}
	}

	public void moveTo(int i, int j, NodeStructure domain) {
		
		node = new Point3i(i, j, node.getK());
    	nodeNumber = domain.getNodeNumber(node);    	
    	point = domain.getXYZEdgeFromIJK(node);
    	
    	clearScenariosUsed();
    	clearHistory();
    	
    	setTriggered(false);
    	
	}

	public synchronized void setTriggered(boolean triggered, String scenario, TimeStep time, Double value) {
		if(triggered && !this.triggering) {
			this.triggering = true;
		}
		if(triggered) {
			if(!scenariosUsed.containsKey(scenario))
				scenariosUsed.put(scenario, new TreeMap<TimeStep, Double>());
			scenariosUsed.get(scenario).put(time, value);	
		}
	}
}