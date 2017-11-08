package objects;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import objects.SensorSetting.DeltaType;
import objects.SensorSetting.Trigger;
import utilities.Constants;
import utilities.Point2i;
import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;

public class E4DSensors {
	private HashMap<String, HashMap<Point2i, HashMap<Point2i, Float>>> detectionTimes; //Should be stored/access with the first location index being less than the second
	
	public static void addERTSensor(STORMData data) {
		String ertInput = Constants.parentDir + "\\e4d\\ertResultMatrix_" + data.getSet().getScenarioEnsemble() + ".csv";
		File ertFile = new File(ertInput);
		if (ertFile.exists()) {
			data.getSet().getSensorList().add("ERT");
			data.getSet().getSensorSettings().put("ERT", new SensorSetting(data.getSet().getNodeStructure(), data.getSet(), "ERT", data.getSet().getScenarios(), 0, 0));
			data.getSet().getSensorSettings().get("ERT").setUserSettings(100, Color.BLUE, 0, 0, Trigger.MINIMUM_THRESHOLD, false, DeltaType.BOTH, 0, 0);
			data.getSet().getNodeStructure().getDataTypes().add("ERT");
			
			// Here, we want to read sensor pairings and times from the matrix
			String line = "";
			List<Scenario> scenarios = data.getSet().getScenarios();
			List<Integer> orderedValidNodes = new ArrayList<Integer>(); //need an ordered list for TTD pairings
			HashSet<Integer> validNodes = new HashSet<Integer>(); //need a hashset for valid nodes
			Map<Scenario, HashSet<Integer>> validNodesPerScenario = new HashMap<Scenario, HashSet<Integer>>();
			Map<Integer, Map<Integer, Float>> detectionTimesPerWell = new HashMap<Integer, Map<Integer, Float>>();
			Map<Scenario, Map<Integer, Map<Integer, Float>>> detectionTimesPerScenario = new HashMap<Scenario, Map<Integer, Map<Integer, Float>>>();
			try (BufferedReader br = new BufferedReader(new FileReader(ertFile))) {
				// Iterate through all the scenarios
				int scenarioIteration = 0;
				// Read each line, comma delimited
				while ((line = br.readLine()) != null) {
					String[] lineList = line.split(",");

					// The first line lists the valid nodes per scenario (duplicates SensorSettings --> setValidNodes())
					if (lineList.length!=0 && lineList[0].toLowerCase().equals(scenarios.get(scenarioIteration).getScenario())) {
						orderedValidNodes.clear();
						validNodes.clear();
						for (int i=1; i<lineList.length; i++) {
							String[] ijList = lineList[i].split(":");
							orderedValidNodes.add(data.getSet().getNodeStructure().getNodeNumber(Integer.parseInt(ijList[0]), Integer.parseInt(ijList[1]), 1));
							validNodes.add(data.getSet().getNodeStructure().getNodeNumber(Integer.parseInt(ijList[0]), Integer.parseInt(ijList[1]), 1));
						}
						validNodesPerScenario.put(scenarios.get(scenarioIteration), validNodes);
					}

					// The following lines list ERT detection times for valid nodes per scenario
					else if (lineList.length!=0){
						Map<Integer, Float> timePerPairedWell = new HashMap<Integer, Float>();
						Integer key = null;
						String[] ijList = lineList[0].split(":");
						key = data.getSet().getNodeStructure().getNodeNumber(Integer.parseInt(ijList[0]), Integer.parseInt(ijList[1]), 1);
						for (int i=1; i<lineList.length; i++) {
							timePerPairedWell.put(orderedValidNodes.get(i-1), Float.parseFloat(lineList[i]));
						}
						detectionTimesPerWell.put(key, timePerPairedWell);
					}

					// The following blank line triggers the saving of detection times for the scenario
					else {
						detectionTimesPerScenario.put(scenarios.get(scenarioIteration), detectionTimesPerWell);
						detectionTimesPerWell.clear();
						scenarioIteration++;
					}
				}
				if (!detectionTimesPerWell.isEmpty()) // just in case there is no blank line at the end of the file
					detectionTimesPerScenario.put(scenarios.get(scenarioIteration), detectionTimesPerWell);
			} catch (IOException ex) {
				System.out.println("Something went wrong trying to read the ERT matrix");
				ex.printStackTrace();
			}
			// Combine scenarios into a final list of validNodes
			//boolean first = true;
			//for (Scenario scenario: scenarios) {
				//if (first) {
					//validNodes = validNodesPerScenario.get(scenario);
					//first = false;
				//} else {
					//validNodes.addAll(validNodesPerScenario.get(scenario)); 
				//}
				//System.out.println(scenario + " ERT nodes: " + validNodes);
			//}
			//data.getSet().getSensorSettings().get("ERT").setValidNodes(validNodes);
			//data.getSet().getSensorSettings().get("ERT").setIsReady(true);
			//data.getSet().getSensorSettings().get("ERT").setNodesReady(true);
			//data.getSet().getSensorSettings().get("ERT").setFullCloudNodes(new HashSet<Integer>(validNodes));
			data.getSet().setERTDetectionTimes(detectionTimesPerScenario);
		}
	}
	
	public static HashSet<Integer> setValidNodesERT(ScenarioSet set) {
		HashSet<Integer> validNodes = new HashSet<Integer>(); //TODO: Check if the list of wells varies by scenario
		for(Scenario scenario: set.ertDetectionTimes.keySet())
			validNodes.addAll(set.ertDetectionTimes.get(scenario).keySet());
		return validNodes;
	}
	
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
