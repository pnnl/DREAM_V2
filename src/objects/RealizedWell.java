package objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utilities.Constants;
import utilities.Point3i;

/**
 * Same as a regular well, but will have one of each sensor in all z's
 * Not currently used anywhere, but could be with different run parameters - Luke
 * @author port091
 */
public class RealizedWell extends Well {

	public RealizedWell(int i, int j, ScenarioSet scenarioSet) {
		super(i, j, scenarioSet);
		
		for(String dataType: scenarioSet.getDataTypes()) {
			for(int k = 1; k <= scenarioSet.getNodeStructure().getIJKDimensions().getK(); k++) {
				this.sensors.add(new ExtendedSensor(i, j, k, dataType, scenarioSet.getNodeStructure()));
			}
		}
	}	

	@Override
	public String toString() {
		return "Realized well at [i=" + i + ", j=" + j + "]";	
	}

	public boolean isOutOfBounds(ScenarioSet scenarioSet) {
		for(ExtendedSensor sensor: sensors) 
		{
			if(sensor.isInCloud(scenarioSet)) {
				return false; // All we need is one sensor to be in the cloud for us to count this spot as in bounds
			}
		}
		return true;
	}
	
	@Override
	public boolean move(ExtendedConfiguration configuration, ScenarioSet scenarioSet) {
	
		// Find out where we can move to
		Set<Integer> validNodes = new HashSet<Integer>();
		
		for(String dataType: scenarioSet.getDataTypes()) {
			validNodes.addAll(scenarioSet.getSensorSettings().get(dataType).getValidNodes());
		}
		
		// Loop through all the i, j's looking for well spots
		Map<Integer, Map<Integer, Boolean>> validIJs = new HashMap<Integer, Map<Integer, Boolean>>();
		Map<Integer, Map<Integer, Boolean>> neighborIJs = new HashMap<Integer, Map<Integer, Boolean>>();
		for(Integer nodeNumber: validNodes) {
			Point3i point = scenarioSet.getNodeStructure().getIJKFromNodeNumber(nodeNumber);
			int i = point.getI();
			int j = point.getJ();
			for(Well well: configuration.getWells()) {
				if(i == well.getI() && j == well.getJ()) {
					continue; // Spot is already taken
				}
			}
			if(!validIJs.containsKey(i))
				validIJs.put(i, new HashMap<Integer, Boolean>());
			validIJs.get(i).put(j, true);				
			
			boolean iNeighbor = i + 1 == getI() || i - 1 == getI() || i == getI();
			boolean jNeighbor = j + 1 == getJ() || j - 1 == getJ() || j == getJ();
			
			if(iNeighbor && jNeighbor) {
				if(!neighborIJs.containsKey(i))
					neighborIJs.put(i, new HashMap<Integer, Boolean>());
				neighborIJs.get(i).put(j, true);	
			}			
			
		}
		
		if(!neighborIJs.isEmpty() && Constants.random.nextDouble() < .8) {
			makeMove(scenarioSet.getNodeStructure(), neighborIJs);
			return true;
		}
		if(!validIJs.isEmpty()) {
			makeMove(scenarioSet.getNodeStructure(), validIJs);
			return true;
		}
		
		return false;
	}
	
	private void makeMove(NodeStructure nodeStructure, Map<Integer, Map<Integer, Boolean>> ijs) {
	
		List<Integer> is = new ArrayList<Integer>();
		is.addAll(ijs.keySet());
		int i = is.get(Constants.random.nextInt(is.size()));
		List<Integer> js = new ArrayList<Integer>();
		js.addAll(ijs.get(i).keySet());
		int j = js.get(Constants.random.nextInt(js.size()));
		
		this.i = i;
		this.j = j;
		for(ExtendedSensor sensor: sensors) {
			sensor.moveTo(i, j, nodeStructure);
		}		
	}
	
}
