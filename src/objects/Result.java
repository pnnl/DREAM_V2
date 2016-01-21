package objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import utilities.Point2d;
import utilities.Point2i;

public class Result implements Comparable<Result> {

	private Map<Point2i, List<Point2i>> sensorLocationsByWellIJK; // {(i,j)->(node#:k, node#:k, node#:k...), (i,j)->(node#:k, ...), ...}
	private Map<String, List<Integer>> sensorsByType; // node#->type
	private Map<Point2d, List<Point2d>> sensorLocationsByWellXYZ; // {(x,y)->(node#:z, node#:z, node#:z...), (x,y)->(node#:z, ...), ...}
	private String inferenceResult;
	private double timeToDetection;
	private int duplicateCount;
	private String iteration = "";

	public Result(ExtendedConfiguration configuration) {
		sensorLocationsByWellIJK = new TreeMap<Point2i, List<Point2i>>();
		sensorLocationsByWellXYZ = new TreeMap<Point2d, List<Point2d>>();
		sensorsByType = new HashMap<String, List<Integer>>();
		for(Well well: configuration.getWells()) {
			int i = well.getI();
			int j = well.getJ();
			Point2i ij = new Point2i(i,j);
			sensorLocationsByWellIJK.put(ij, new ArrayList<Point2i>());
			for(ExtendedSensor sensor: well.getSensors()) {
				if(sensor.getPoint() != null) {
					double x = sensor.getPoint().getX();
					double y = sensor.getPoint().getY();
					Point2d xy = new Point2d(x,y);
					if(!sensorLocationsByWellXYZ.containsKey(xy))
						sensorLocationsByWellXYZ.put(xy, new ArrayList<Point2d>());
					sensorLocationsByWellXYZ.get(xy).add(new Point2d(sensor.getNodeNumber(), sensor.getPoint().getZ()));
				}
				sensorLocationsByWellIJK.get(ij).add(new Point2i(sensor.getNodeNumber(), sensor.getIJK().getK()));
				if(!sensorsByType.containsKey(sensor.getSensorType()))
					sensorsByType.put(sensor.getSensorType(), new ArrayList<Integer>());
				sensorsByType.get(sensor.getSensorType()).add(sensor.getNodeNumber());
			}
		}
		inferenceResult = configuration.getInferenceResults();
		timeToDetection = configuration.getUnweightedTimeToDetectionInDetectingScenarios();
		duplicateCount = 0;
	}

	public Result(ExtendedConfiguration configuration, Integer iteration) {
		if(this.iteration.isEmpty())
			this.iteration = String.valueOf(iteration);
		else 
			this.iteration += String.valueOf(", " + iteration);

		sensorLocationsByWellIJK = new TreeMap<Point2i, List<Point2i>>();
		sensorLocationsByWellXYZ = new TreeMap<Point2d, List<Point2d>>();
		sensorsByType = new HashMap<String, List<Integer>>();
		for(Well well: configuration.getWells()) {
			int i = well.getI();
			int j = well.getJ();
			Point2i ij = new Point2i(i,j);
			sensorLocationsByWellIJK.put(ij, new ArrayList<Point2i>());
			for(ExtendedSensor sensor: well.getSensors()) {
				if(sensor.getPoint() != null) {
					double x = sensor.getPoint().getX();
					double y = sensor.getPoint().getY();
					Point2d xy = new Point2d(x,y);
					if(!sensorLocationsByWellXYZ.containsKey(xy))
						sensorLocationsByWellXYZ.put(xy, new ArrayList<Point2d>());
					sensorLocationsByWellXYZ.get(xy).add(new Point2d(sensor.getNodeNumber(), sensor.getPoint().getZ()));
				}
				sensorLocationsByWellIJK.get(ij).add(new Point2i(sensor.getNodeNumber(), sensor.getIJK().getK()));
				if(!sensorsByType.containsKey(sensor.getSensorType()))
					sensorsByType.put(sensor.getSensorType(), new ArrayList<Integer>());
				sensorsByType.get(sensor.getSensorType()).add(sensor.getNodeNumber());
			}
		}
		inferenceResult = configuration.getInferenceResults();
		timeToDetection = configuration.getUnweightedTimeToDetectionInDetectingScenarios();
		duplicateCount = 0;
	}


	public int getDupilcates() {
		return duplicateCount;
	}

	public String getInferenceResult() {
		return inferenceResult;
	}

	public double getTimeToDetection() {
		return timeToDetection;
	}

	public List<String> getNodesAsStrings() {
		List<String> nodes = new ArrayList<String>();
		for(String type: sensorsByType.keySet()) {
			for(Integer nodeNumber: sensorsByType.get(type))
				nodes.add(nodeNumber + ":" + type.charAt(0));
		}
		return nodes;
	}

	private Map<String, List<Integer>> getNodesByType() {
		Map<String, List<Integer>> nodeNumbers = new HashMap<String, List<Integer>>();
		for(String type: sensorsByType.keySet()) {
			nodeNumbers.put(type, new ArrayList<Integer>());
			for(Integer nodeNumber: sensorsByType.get(type))
				nodeNumbers.get(type).add(nodeNumber);
		}
		return nodeNumbers;
	}

	/**
	 * Returns the summary using i,j,k 
	 * 
	 * size->{(i,j)->(node#:k:type, node#:k:type, node#:k:type...), (i,j)->(node#:k:type, ...), ...}
	 * 
	 * @return
	 */

	public String getSummaryIJK() {

		StringBuffer nodePositions = new StringBuffer();
		if(sensorsByType.isEmpty())
			return "Empty";	
		Map<String, List<Integer>> nodesByType = getNodesByType();
		nodePositions.append("{");
		int totalSensors = 0;
		for(Point2i well: sensorLocationsByWellIJK.keySet()) {
			nodePositions.append("("+well.getI()+", "+well.getJ()+")->(");
			int sensors = sensorLocationsByWellIJK.get(well).size();
			for(int i = 0; i < sensors; i++) {
				Point2i sensor = sensorLocationsByWellIJK.get(well).get(i);
				String myType = "";
				for(String type: nodesByType.keySet()) {
					if(nodesByType.get(type).contains(sensor.getI())) {
						myType = type;
						break;
					}
				}				
				nodePositions.append(sensor.getI() + ":" + sensor.getJ() + ":" + (myType.length() > 4 ? myType.charAt(4) : myType));
				nodesByType.get(myType).remove((Object)sensor.getI());
				nodePositions.append(i+1 != sensors ?  ", ": ") ");	
				totalSensors++;
			}
		}
		return totalSensors + "->" + nodePositions.toString();		
	}


	/**
	 * Returns the summary using x, y, z
	 * 
	 * size->{(x,y)->(node#:z:type, node#:z:type, node#:z:type...), (x,y)->(node#:z:type, ...), ...}
	 * 
	 * @return
	 */

	public String getSummaryXYZ() {

		StringBuffer nodePositions = new StringBuffer();
		if(sensorsByType.isEmpty())
			return "Empty";	
		Map<String, List<Integer>> nodesByType = getNodesByType();
		nodePositions.append("{");
		int totalSensors = 0;
		for(Point2d well: sensorLocationsByWellXYZ.keySet()) {

			nodePositions.append("("+well.getX()+", "+well.getY()+")->(");
			int sensors = sensorLocationsByWellXYZ.get(well).size();
			for(int i = 0; i < sensors; i++) {
				try {
					Point2d sensor = sensorLocationsByWellXYZ.get(well).get(i);
					String myType = "";
					for(String type: nodesByType.keySet()) {
						if(nodesByType.get(type).contains(sensor.getX())) {
							myType = type;
							break;
						}
					}				
					nodePositions.append(sensor.getX() + ":" + sensor.getY() + ":" + (myType.length() > 4 ? myType.charAt(4) : myType));
					if(sensor != null && nodesByType.containsKey(myType) && nodesByType.get(myType).contains((Object)sensor.getX()))
						nodesByType.get(myType).remove((Object)sensor.getX());
					nodePositions.append(i+1 != sensors ?  ", ": ") ");	
					totalSensors++;
				} catch (Exception e) {
					System.out.println("Failed to do something!");
					e.printStackTrace();
				}
			}

		}
		return totalSensors + "->" + nodePositions.toString();		
	}

	public String getIteration() {
		if(iteration.isEmpty())
			return "";
		else 
			return iteration + "\t";
	}

	@Override
	public String toString() {
		// List of node#:T, ...
		return getNodesAsStrings().toString();
	}

	@Override
	public boolean equals(Object result) 
	{
		if(result instanceof Result)
			return compareTo((Result)result) == 0;
		return false;
	}

	@Override
	public int compareTo(Result result) {
		Map<String, List<Integer>>  othersNodes = result.getNodesByType();
		Map<String, List<Integer>>  myNodes = getNodesByType();
		for(String type: othersNodes.keySet()) {	
			List<Integer> others = othersNodes.get(type);
			for(int node: others) {
				if(!myNodes.containsKey(type) || !myNodes.get(type).contains(node)) {
					return -1; // My nodes didn't contain one of theirs
				}
				myNodes.get(type).remove((Object)node);
				if(myNodes.get(type).isEmpty())
					myNodes.remove(type);
			}
		}
		if(myNodes.isEmpty()) {
			if(Double.compare(getTimeToDetection(), result.getTimeToDetection()) != 0) // these should be equal
			{
				//	System.out.println("ERROR, identical configurations gave different ttds!!!");
				return -1;
			}
			return 0;	// We have the same nodes!
		}
		return 1;	// Their nodes did not contain one of mine
	}

	public void incrementDupliateCount(String iteration) {
		this.iteration += ", " + iteration;
		duplicateCount++;
	}
}
