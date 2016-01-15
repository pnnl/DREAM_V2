package objects;

import java.util.*;

import utilities.Constants;
import utilities.Point3i;

public class Well 
{

	protected int i;
	protected int j;

	protected List<ExtendedSensor> sensors;

	public Well(int i, int j, ScenarioSet scenarioSet) {

		this.i = i;
		this.j = j;

		sensors = new ArrayList<ExtendedSensor>();
	}

	@Override 
	public String toString()
	{
		return "[i=" + i + ", j=" + j + "]";	
	}

	public int getI() {
		return i;
	}

	public int getJ() {
		return j;
	}

	public boolean addSensor(ExtendedSensor sensor)
	{
		sensors.add(sensor);
		return true;
	}

	public boolean isEmpty()
	{
		return sensors.isEmpty();
	}

	public void removeSensor(ExtendedSensor sensor) {
		sensors.remove(sensor);
	}

	public boolean isAt(ExtendedSensor sensor) {
		return i == sensor.getIJK().getI() && j == sensor.getIJK().getJ();
	}

	public List<ExtendedSensor> getSensors() {
		return sensors;
	}

	public boolean move(ExtendedConfiguration configuration, ScenarioSet scenarioSet) {

		// Find possible wells
	//	System.out.println("----------------------Moving well-----------------------------");
		Map<String, List<Integer>> typeCount =  getSensorCountByType();

		List<Integer> sizes = new ArrayList<Integer>();
		Map<String, List<Integer>> validNodes = new HashMap<String, List<Integer>>();
		for(String type: typeCount.keySet()) {
			// Get all the nodes where we can fit each type
			List<Integer> nodes = scenarioSet.getValidNodes(type, configuration, false, false, false);
			validNodes.put(type, nodes);
			sizes.add(nodes.size());
		}
		
		Collections.sort(sizes); // We want to start with the most constraining list
	
		boolean initial = true;
		Map<Integer, Map<Integer, Well>> wellsByIJ = new HashMap<Integer, Map<Integer, Well>>();
		for(Integer size: sizes) {
			List<Integer> toRemove = null;
			for(String type : validNodes.keySet()) {
				List<Integer> temp = validNodes.get(type);
				if(temp.size() == size) {
					
					//System.out.println("Spots needed for " + type + ": " + typeCount.get(type).size());
					// Build wells using these constraints

					// Process the nodes
					Map<Integer, Map<Integer, List<Integer>>> validKsByIJ = new HashMap<Integer, Map<Integer,  List<Integer>>>();
					for(Integer nodeNumber: temp) {
						Point3i point = scenarioSet.getNodeStructure().getIJKFromNodeNumber(nodeNumber);
						if(!initial) {
							if(!wellsByIJ.containsKey(point.getI()) || !wellsByIJ.get(point.getI()).containsKey(point.getJ())) {
								continue;	// Skip this one, its not worth considering the well location
							}
						}
						if(!validKsByIJ.containsKey(point.getI()))
							validKsByIJ.put(point.getI(), new HashMap<Integer, List<Integer>>()); // By i's
						if(!validKsByIJ.get(point.getI()).containsKey(point.getJ()))
							validKsByIJ.get(point.getI()).put(point.getJ(), new ArrayList<Integer>()); // By j's
						
						validKsByIJ.get(point.getI()).get(point.getJ()).add(nodeNumber);						
					}

					int currentConstraint = typeCount.get(type).size();
					// Make some valid wells out of the processed values
					for(Integer i: validKsByIJ.keySet()) {
						for(Integer j: validKsByIJ.get(i).keySet()) {
							if(validKsByIJ.get(i).get(j).size() >= currentConstraint) {
								// This is a valid well location for the given type
								if(initial) {
									Well tempWell = new Well(i,j, scenarioSet);
									if(!wellsByIJ.containsKey(i)) {
										wellsByIJ.put(i, new HashMap<Integer, Well>()); // By i's
									}
									wellsByIJ.get(i).put(j, tempWell);
								} 
								//System.out.println(validKsByIJ.get(i).get(j).size() + " " + type + " spots for well " + wellsByIJ.get(i).get(j) + ": " + validKsByIJ.get(i).get(j).toString());
							} else {
								if(!initial) {
									wellsByIJ.get(i).remove(j); // This one is not valid, remove any well that is there
									if(wellsByIJ.get(i).isEmpty())
										wellsByIJ.remove(i);
								}
							}
						}
					}

					toRemove = temp;
					break;
				}
			}
			if(initial)
				initial = false;
			validNodes.remove(toRemove);
		}
		List<Well> validWells = new ArrayList<Well>();
		
		
		List<Well> neighboringWells = new ArrayList<Well>();
		for(Integer i: wellsByIJ.keySet()) {
			for(Integer j: wellsByIJ.get(i).keySet()) {
				if(i == getI() && j == getJ())
					continue; // Not the same spot
				// if this is a neighbor
				Well well = wellsByIJ.get(i).get(j);
				boolean iNeighbor = i + 1 == getI() || i - 1 == getI() || i == getI();
				boolean jNeighbor = j + 1 == getJ() || j - 1 == getJ() || j == getJ();
				if(iNeighbor && jNeighbor)
					neighboringWells.add(well);
				validWells.add(well);
			}
		}
	//	System.out.println("Neighboring wells: " + neighboringWells.size());
	//	System.out.println("Other wells: " + validWells.size());

		// Chose a neighbor to move to 80% of the time
		if(neighboringWells.size() > 0 && Double.compare(Constants.random.nextDouble(), .8) < 0) {
			Collections.shuffle(neighboringWells, Constants.random);
			Well toMove = neighboringWells.get(0);
			//System.out.println("Moving "+toString()+" to a neighbor: " + toMove.toString());
			return move(configuration, scenarioSet, toMove); // This shouldn't fail, we're only considering valid wells
		}

		if(validWells.size() > 0) {
			Collections.shuffle(validWells, Constants.random);
			Well toMove = validWells.get(0);
			//System.out.println("Moving "+toString()+" to a valid well: " + toMove.toString());
			return move(configuration, scenarioSet, toMove); // This shouldn't fail, we're only considering valid wells
		}

	//	System.out.println("-------------------------Done-----------------------------");

		return false;


	}
	
	public boolean shuffle(ExtendedConfiguration configuration, ScenarioSet scenarioSet) {
		return move(configuration, scenarioSet, this);
	}

	/**
	 * If for some reason we are trying to shuffle a well that contains more sensors of a given type then are valid, 
	 * this will fail.  Otherwise shuffle should always return true.
	 * 
	 * @param configuration
	 * @param scenarioSet
	 * @param well
	 * @return
	 */
	private boolean move(ExtendedConfiguration configuration, ScenarioSet scenarioSet, Well well) {
		
		boolean debug = false; 
		
		// Actually makes the move
		Map<String, List<Integer>> validKs = new HashMap<String, List<Integer>>();
		// Get the node numbers in the well
		List<Integer> nodesInWell = scenarioSet.getNodeStructure().getNodesInWell(well);
		
		for(String type: scenarioSet.getDataTypes()) {
			List<Integer> nodes = scenarioSet.getValidNodes(type, configuration, false, false, false);
			for(Integer inWell: nodesInWell) {
				// We know the node is valid for this data type
				if(nodes.contains(inWell)) {
					if(!validKs.containsKey(type))
						validKs.put(type, new ArrayList<Integer>());
					validKs.get(type).add(inWell);
				}
			}
		}
		if(debug) {
			System.out.println("Nodes in the current well: " + nodesInWell.toString());
			System.out.println("Well before move: " + toString());
		}
		
		if(validKs.isEmpty()) return false; //no way that we can shuffle this - it is full of all sensor types that it has.
		// Do a quick check to make sure we can shuffle this well...
		
		Map<String, List<Integer>> typeCount =  getSensorCountByType();
		for(String type: validKs.keySet()) {
			List<Integer> nodes = validKs.get(type);
			if(typeCount.containsKey(type) && nodes.size() < typeCount.get(type).size()) {
				if(debug) {
					System.out.println("Cannot perform shuffle, we have " + typeCount.get(type).size() + " " + type + 
							" nodes and we are trying to shuffle them into a well that can only support " + nodes.size());
				}
				return false; // Can't shuffle this one
			}
		}
		for(String type: validKs.keySet()) {
			List<Integer> nodes = validKs.get(type);
			Collections.shuffle(nodes, Constants.random); // Shuffle up  the nodes to get a random order
			if(debug) {
				System.out.println(nodes.size() + " valid nodes for " + type + ": " + nodes.toString());		
			}
			for(ExtendedSensor sensor: sensors) {
				if(sensor.getSensorType().equals(type)) {
					if(debug) {
						System.out.print("Moving sensor: " + sensor.toString());
					}
					if(nodes.isEmpty())
						// Something went wrong
						return false;
					sensor.moveTo(nodes.remove(0), scenarioSet.getNodeStructure());
					if(debug) {
						System.out.println(" to " + sensor.toString());
					}
				}
			}
			
		}
		i = well.getI();
		j = well.getJ();
		
		if(debug) {
			System.out.println("Well after move: " + toString());
		}
		
		return true;
	}


	private Map<String, List<Integer>> getSensorCountByType() {
		Map<String, List<Integer>> typeCount = new HashMap<String, List<Integer>>();
		for(ExtendedSensor sensor: sensors) {
			if(!typeCount.containsKey(sensor.getSensorType())) 
				typeCount.put(sensor.getSensorType(), new ArrayList<Integer>());
			typeCount.get(sensor.getSensorType()).add(sensor.getNodeNumber());

		}
		return typeCount;
	}

	public void moveTo(Point3i ijk) {
		this.i = (ijk.getI());
		this.j = (ijk.getJ());
	}





















}
