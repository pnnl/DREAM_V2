package objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import utilities.Point2i;
import utilities.Point3i;

public class E4DSensors {
	private HashMap<String, HashMap<Point2i, HashMap<Point2i, Float>>> detectionTimes; //Should be stored/access with the first location index being less than the second
	
	public static void writeWellLocations(){ //TODO: What objects do we need to accomplish this?
		//TODO: Move well writing logic here, that way we have all of the methods in the same place.
	}
	
	public E4DSensors(ScenarioSet set, String filePath){
		//TODO: Read in file path and use this to populate the detectionTimes.
	}
	
	public E4DSensors(ScenarioSet set, HashSet<Point2i> locations){
		Random rand = new Random(); //Temporarily using this to create grid
		detectionTimes = new HashMap<String, HashMap<Point2i, HashMap<Point2i, Float>>>();
		HashMap<Scenario, Integer> scenarioValues = new HashMap<Scenario, Integer>();
		for(int i=0; i<set.getScenarios().size(); ++i){
			scenarioValues.put(set.getScenarios().get(i), i*10);
			detectionTimes.put(set.getScenarios().get(i).toString(), new HashMap<Point2i, HashMap<Point2i, Float>>());
		}
		for(Point2i location1: locations){
			for(Scenario scenario: set.getScenarios()){ //this loop goes one too far.
				detectionTimes.get(scenario.toString()).put(location1, new HashMap<Point2i, Float>());
			}
			for(Point2i location2: locations){
				if(location1.compareTo(location2) != -1) continue; //Make sure we're not adding twice as many as we need to, this makes sure that the first well comes "before" the second
																	//in this case, "before" means lower i or lower j if equal i.
				for(Scenario scenario: set.getScenarios()){
					detectionTimes.get(scenario.toString()).get(location1).put(location2, (float) (rand.nextInt(9)+1+scenarioValues.get(scenario)));
				}
			}
		}
	}
	
	public String printDetectionTimes(){
		StringBuilder s = new StringBuilder();
		s.append("~~~~~ E4D DETECTION TIME MAP ~~~~~~~~\n\n");
		for(String scenario: detectionTimes.keySet()){
			s.append("~~ Scenario " + scenario + ":\n");
			for(Point2i location1: detectionTimes.get(scenario).keySet()){
				for(Point2i location2: detectionTimes.get(scenario).get(location1).keySet()){
					s.append(location1 + "\t" + location2 + "\t" + detectionTimes.get(scenario).get(location1).get(location2) + "\n");
				}
			}
		}
		return s.toString();
	}
	
	public Float getDetectionTime(String scenario, ExtendedConfiguration config){
		HashSet<Point2i> wellLocations = new HashSet<Point2i>();
		for(Sensor sensor: config.getSensors()){
			Point3i location = sensor.getIJK();
			Point2i wellLocation = new Point2i(location.getI(),location.getJ());
			wellLocations.add(wellLocation);
		}
		if(wellLocations.size() < 2) return Float.MAX_VALUE; //There aren't enough well locations to return a detection for E4D, so pick maximum possible detection time
		Float result = Float.MAX_VALUE;
		Object[] locationArray =  wellLocations.toArray();
		for(int i=0; i<wellLocations.size(); ++i){
			for(int j=0; j<i; ++j){
				Float newTime = getDetectionTime(scenario, (Point2i)locationArray[j], (Point2i)locationArray[i]);
				if(newTime < result){ //If the new time is before the current result to return, swap them.
					result = newTime;
				}
			}
		}
		return result; //Should never be null.
	}

	
	private Float getDetectionTime(String scenario, Point2i location1, Point2i location2){
		if(location1.compareTo(location2) == 1){
			Point2i temp = location1;
			location1 = location2;
			location2 = temp;
		}
//		System.out.println(detectionTimes.get(scenario).get(location1).get(location2));
		return detectionTimes.get(scenario).get(location1).get(location2);
	}
	
	
	private void addDetectionTime(String scenario, Point2i location1, Point2i location2, Float time){
		if(location1.compareTo(location2) == 1){
			Point2i temp = location1;
			location1 = location2;
			location2 = temp;
		}
		detectionTimes.get(scenario).get(location1).put(location2, time);
	}
}
