package objects;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;

import hdf5Tool.HDF5Interface;
import objects.SensorSetting.DeltaType;
import objects.SensorSetting.Trigger;
import utilities.Constants;
import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;

public class E4DSensors {
	
	
	// Called when a results matrix is found in the correct location, the matrix is read and stored
	public static void addERTSensor(STORMData data) {
		String ertInput = Constants.userDir + "\\e4d\\ertResultMatrix_" + data.getSet().getScenarioEnsemble() + "_" + data.getSet().getAllScenarios().size() + ".csv";
		File ertFile = new File(ertInput);
		if (ertFile.exists() && data.getSet().getERTDetectionTimes().isEmpty()) {
			data.getSet().getSensors().add("Electrical Conductivity");
			data.getSet().getSensorSettings().put("Electrical Conductivity", new SensorSetting(data.getSet().getNodeStructure(), data.getSet(), "Electrical Conductivity", data.getSet().getScenarios(), 0, 0));
			data.getSet().getSensorSettings().get("Electrical Conductivity").setUserSettings(100, Color.BLUE, 0, 0, Trigger.MINIMUM_THRESHOLD, false, DeltaType.BOTH, 0, 0);
			data.getSet().getNodeStructure().getDataTypes().add("Electrical Conductivity");
			
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
			data.getSet().setERTDetectionTimes(detectionTimesPerScenario);
		}
	}
	
	
	// This method returns valid nodes for E4D
	public static HashSet<Integer> setValidNodesERT(ScenarioSet set) {
		HashSet<Integer> validNodes = new HashSet<Integer>();
		for(Scenario scenario: set.ertDetectionTimes.keySet())
			validNodes.addAll(set.ertDetectionTimes.get(scenario).keySet());
		return validNodes;
	}
	
	
	// This method tells the Simulated Annealing process whether sensors have been triggered by ERT (reads matrix)
	public static Boolean ertSensorTriggered(ScenarioSet set, TimeStep timestep, Scenario scenario, Integer nodeNumber) throws Exception{
		Boolean triggered = false;
		
		Map<Integer, Float> detection = set.getERTDetectionTimes().get(scenario).get(nodeNumber);
		if(detection==null)
			return triggered;
		Integer bestWell = null; //TODO: this best well pairing needs to show up on the animation
		Float bestTTD = Float.MAX_VALUE;
		for (Map.Entry<Integer, Float> entry : detection.entrySet()) {
			if (entry.getValue() < bestTTD) {
				bestWell = entry.getKey();
				bestTTD = entry.getValue();
			}
		}
		if (timestep.getTimeStep() >= bestTTD)
			triggered = true;
		
		return triggered;
	}
	
	
	// This method determines which wells should be passed along to E4D
	public static ArrayList<Point3i> calculateE4DWells(STORMData data, String parameter, IProgressMonitor monitor) throws Exception {
				
		float threshold = 0.01f; //Hard number determined by Catherine
		int maximumWells = 30; //Hard number given by E4D coders
		NodeStructure nodeStructure = data.getSet().getNodeStructure();
		
		ArrayList<Point3i> wellList = new ArrayList<Point3i>();
		HashSet<Integer> allNodes = new HashSet<Integer>(); //All nodes that meet the above threshold
		
		
		// Successively remove a level of magnitude to the search threshold until nodes are found
		int iteration = 1;
		while (allNodes.size()==0) {
			if(iteration>1) monitor.worked(-600/iteration);
			// Loop through scenarios and add all nodes that trigger in all
			monitor.subTask("Scanning for valid nodes with threshold = " + threshold);
			for(Scenario scenario: data.getSet().getScenarios()) {
				if(monitor.isCanceled()) return null;
				try {
					HashSet<Integer> nodes = null;
					nodes = HDF5Interface.queryNodes(nodeStructure, scenario.getScenario(), parameter, threshold, threshold, Trigger.RELATIVE_DELTA, DeltaType.BOTH, monitor);
					allNodes.addAll(nodes);
				} catch (Exception e) {
					System.out.println("Unable to query nodes from files.");
					e.printStackTrace();
				}
			}
			threshold /= 10;
			monitor.worked(600/iteration); //This loop will be 60% of the progress bar
			iteration++;
		}
		
		// Count how many unique wells locations were found in the above nodes
		for(Integer node: allNodes) {
			Point3i temp = nodeStructure.getIJKFromNodeNumber(node);
			Point3i well = new Point3i(temp.getI(), temp.getJ(), 1); //set k to 1 to get the single well location
			if(!wellList.contains(well))
				wellList.add(well);
		}
		
		// If a reasonable number of wells is found, sort and return
		monitor.subTask("Valid wells = " + wellList.size() + "; starting goal seek.");
		if(wellList.size() <= maximumWells) {
			Collections.sort(wellList, Point3i.IJ_COMPARATOR);
		// If too many wells are found, goal seek to the desired number
		} else {
			wellList = new ArrayList<Point3i>(); //reset
			TreeMap<Float, Integer> changePerNode = new TreeMap<Float, Integer>();
			
			// Find the relative difference for all the nodes found above
			TimeStep firstStep = nodeStructure.getTimeSteps().get(0);
			TimeStep lastStep = nodeStructure.getTimeSteps().get(nodeStructure.getTimeSteps().size()-1);
			for(Integer node: allNodes) {
				float maxDifference = 0;
				for(Scenario scenario: data.getSet().getScenarios()) {
					if(monitor.isCanceled()) return null;
					float start = HDF5Interface.queryValue(nodeStructure, scenario.getScenario(), firstStep, parameter, node);
					float end = HDF5Interface.queryValue(nodeStructure, scenario.getScenario(), lastStep, parameter, node);
					float difference = Math.abs((end - start) / start);
					if(difference > maxDifference)
						maxDifference = difference;
				}
				changePerNode.put(maxDifference, node);
				monitor.worked(400/allNodes.size());
			}
			
			// Sort the nodes by greatest relative change
			ArrayList<Float> keys = new ArrayList<Float>(changePerNode.keySet());
			ArrayList<Integer> values = new ArrayList<Integer>(keys.size());
			Collections.sort(keys, Collections.reverseOrder());
			for(Float key: keys)
				values.add(changePerNode.get(key));
			
			// Convert to Point3i and sort
			for(Integer node: values) {
				Point3i temp = nodeStructure.getIJKFromNodeNumber(node);
				Point3i well = new Point3i(temp.getI(), temp.getJ(), 1); //set k to 1 to get the single well location
				if(!wellList.contains(well))
					wellList.add(well);
				if(wellList.size() == maximumWells)
					break;
			}
			Collections.sort(wellList, Point3i.IJ_COMPARATOR);
		}
		return wellList;
	}
}
