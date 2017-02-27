package objects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import utilities.Point2i;
import utilities.Point3i;

public class E4DSensors {
	private HashMap<String, HashMap<Point2i, HashMap<Point2i, Integer>>> detectionTimes; //Should be stored/access with the first location index being less than the second
	
	public static void writeWellLocations(){ //TODO: What objects do we need to accomplish this?
		//TODO: Move well writing logic here, that way we have all of the methods int he same place.
	}
	
	public E4DSensors(String filePath){
		//TODO: Read in file path and use this to populate the detectionTimes.
	}
	
	public E4DSensors(ScenarioSet set, HashSet<Point2i> locations){
		Random rand = new Random(); //Temporarily using this to create grid
		detectionTimes = new HashMap<String, HashMap<Point2i, HashMap<Point2i, Integer>>>();
		HashMap<Scenario, Integer> scenarioValues = new HashMap<Scenario, Integer>();
		for(int i=0; i<set.getScenarios().size(); ++i){
			scenarioValues.put(set.getScenarios().get(i), i*10);
			detectionTimes.put(set.getScenarios().get(i).toString(), new HashMap<Point2i, HashMap<Point2i, Integer>>());
		}
		for(Point2i location1: locations){
			for(Scenario scenario: set.getScenarios()){ //this loop goes one too far.
				detectionTimes.get(scenario.toString()).put(location1, new HashMap<Point2i, Integer>());
			}
			for(Point2i location2: locations){
				if(location1.compareTo(location2) != -1) break;
				for(Scenario scenario: set.getScenarios()){
					detectionTimes.get(scenario.toString()).get(location1).put(location2, rand.nextInt(10)+scenarioValues.get(scenario.toString()));
				}
			}
		}
	}
	
	public void printDetectionTimes(){
		System.out.println("~~~~~ E4D DETECTION TIME MAP ~~~~~~~~\n");
		for(String scenario: detectionTimes.keySet()){
			System.out.println("~~ Scenario " + scenario + ":");
			for(Point2i location1: detectionTimes.get(scenario).keySet()){
				for(Point2i location2: detectionTimes.get(scenario).get(location1).keySet()){
					System.out.println(location1 + "\t" + location2 + "\t" + detectionTimes.get(scenario).get(location1).get(location2));
				}
			}
		}
	}
	
	public Integer getDetectionTime(String scenario, ExtendedConfiguration config){
		HashSet<Point2i> wellLocations = new HashSet<Point2i>();
		for(Sensor sensor: config.getSensors()){
			Point3i location = sensor.getIJK();
			Point2i wellLocation = new Point2i(location.getI(),location.getJ());
			wellLocations.add(wellLocation);
		}
		Integer result = null;
		Point2i[] locationArray= (Point2i[]) wellLocations.toArray();
		for(int i=0; i<wellLocations.size(); ++i){
			for(int j=0; j<i; ++j){
				Integer newTime = getDetectionTime(scenario, locationArray[i], locationArray[j]);
				if(newTime < result){ //If the new time is before the current result to return, swap them.
					result = newTime;
				}
			}
		}
		return result; //Should only be null if this configuration actually has no sensors
	}
	
	private void addDetectionTime(String scenario, Point2i location1, Point2i location2, Integer time){
		if(location1.compareTo(location2) == 1){
			Point2i temp = location1;
			location1 = location2;
			location2 = temp;
		}
		detectionTimes.get(scenario).get(location1).put(location2, time);
	}
	
	private Integer getDetectionTime(String scenario, Point2i location1, Point2i location2){
		if(location1.compareTo(location2) == 1){
			Point2i temp = location1;
			location1 = location2;
			location2 = temp;
		}
		return detectionTimes.get(scenario).get(location1).get(location2);
	}
	
	public static void main(String[] args){
		
	}
}
