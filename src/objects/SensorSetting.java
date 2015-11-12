package objects;

import hdf5Tool.HDF5Wrapper;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import objects.SensorSetting.Trigger;
import utilities.Constants;

public class SensorSetting {
	
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

	private Trigger trigger;
	private DeltaType deltaType;

	private float lowerThreshold;
	private float upperThreshold;

	private Set<Integer> validNodes;
	private float minZ;
	private float maxZ;

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
		this.upperThreshold = (max-min)/2+min;

		this.validNodes = new TreeSet<Integer>(); // None yet
		
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
		this.upperThreshold = (max-min)/2+min;

		this.validNodes = new TreeSet<Integer>(); // None yet
		this.color = Color.GREEN;	

		this.isReady = false;
		this.nodesReady = false;

		Constants.log(Level.INFO, "Sensor settings "+type+": initialized ", null);
		Constants.log(Level.CONFIG, "Sensor settings "+type+": configuration", this);

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
		
		builder.append("\t\tminZ: " + minZ + "\n");
		builder.append("\t\tmaxZ: " + maxZ + "\n");

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
			this.minZ = minZ;
			changeOccured = true;
		}
		
		if(this.getMaxZ() != maxZ) {
			this.maxZ = maxZ;
			changeOccured = true;
		}
		
		this.isReady = true;

		if(changeOccured)
			this.nodesReady = false; // We will need to re-query the database

		Constants.log(Level.CONFIG, "Sensor settings "+type+": configuration", this);
	}
	
	public void setValidNodes(HashSet<Integer> newSet){
		this.validNodes.clear();
		this.validNodes.addAll(newSet);
	}

	// Update the valid nodes based on current settings 
	private void setValidNodes() {

		validNodes.clear();

		Constants.log(Level.INFO, "Sensor settings "+type+": setting valid nodes", null);

		if(getTrigger() == Trigger.MAXIMUM_THRESHOLD || getTrigger() == Trigger.MINIMUM_THRESHOLD) {

			// System.out.println("Testing threshold: " + lowerThreshold + ", " + upperThreshold);
			Map<String, HashSet<Integer>> validNodesPerScenario = new HashMap<String, HashSet<Integer>>();
			for(Scenario scenario: scenarios) {
				// Query for valid nodes per scenario
				try {
					HashSet<Integer> nodes = Constants.hdf5Data.isEmpty() ? 
							HDF5Wrapper.queryNodesFromFiles(scenarioSet.getNodeStructure(), scenario.toString(), getType(), lowerThreshold, upperThreshold) : 
								HDF5Wrapper.queryNodesFromMemory(scenarioSet.getNodeStructure(), scenario.toString(), getType(), lowerThreshold, upperThreshold);
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
							HDF5Wrapper.queryNodesFromMemory(scenarioSet.getNodeStructure(), scenario.getScenario(), getType(), lowerThreshold, upperThreshold, getTrigger(), getDeltaType()) :
							HDF5Wrapper.queryNodesFromFiles(scenarioSet.getNodeStructure(), scenario.getScenario(),  getType(), lowerThreshold, upperThreshold, getTrigger(), getDeltaType());
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

		Constants.log(Level.CONFIG, "Sensor settings: set valid nodes", this);
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

	public synchronized Set<Integer> getValidNodes() {
		if(!areNodesReady())
			setValidNodes();
		/*
		List<Integer> validNodeNumbers = new ArrayList<Integer>();
		for(Integer itgr: validNodes) {
			validNodeNumbers.add(itgr);
		}*/
		return validNodes;//validNodeNumbers;
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
	
	public Float getLowerThreshold() {
		return lowerThreshold;
	}

	public Float getUpperThreshold() {
		return upperThreshold;
	}

	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
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
		return minZ;
	}

	public void setMinZ(float minZ) {
		this.minZ = minZ;
	}

	public float getMaxZ() {
		return maxZ;
	}

	public void setMaxZ(float maxZ) {
		this.maxZ = maxZ;
	}

}
