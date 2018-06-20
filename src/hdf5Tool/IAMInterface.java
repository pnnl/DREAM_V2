package hdf5Tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import objects.NodeStructure;
import objects.TimeStep;
import utilities.Constants;
import utilities.Point3i;

/**
 * Utility functions for use in reading and parsing hdf5 files to DREAM formats
 * @author port091
 * @author whit162
 */

public class IAMInterface {
	
	private static List<String> scenarios = new ArrayList<String>();
	private static List<String> dataTypes = new ArrayList<String>();
	// Organized as: Specific Type<Scenario<Index, TTD>>
	private static Map<String, Map<String, Map<Integer, Float>>> paretoMap = new HashMap<String, Map<String, Map<Integer, Float>>>();
	
	/*
	 * IAM file structure goes:
	 * IAM, Scenario, Parameter, Trigger, Threshold (5)
	 * Timesteps (>5)
	 * x, y, z, TTD (4)
	 */
	
	// Read one file to extract the Node Structure information from H5 files
	public static NodeStructure readNodeStructureIAM (File file) {
		NodeStructure nodeStructure = null;
		List<TimeStep> times = new ArrayList<TimeStep>();
		List<Float> xValues = new ArrayList<Float>();
		List<Float> yValues = new ArrayList<Float>();
		List<Float> zValues = new ArrayList<Float>();
		String line;
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((line = br.readLine()) != null) {
				String[] lineList = line.split(","); //comma delimited
				if(lineList.length==4) { //data
					float x = Float.parseFloat(lineList[0]);
					float y = Float.parseFloat(lineList[1]);
					float z = Float.parseFloat(lineList[2]);
					if(!xValues.contains(x)) xValues.add(x);
					if(!yValues.contains(y)) yValues.add(y);
					if(!zValues.contains(z)) zValues.add(z);
				} else if(lineList.length>5) { //timesteps
					for(int i=0; i<lineList.length; i++) {
						float timestep = Float.parseFloat(lineList[i]);
						times.add(new TimeStep(i, timestep, Math.round(timestep)));
					}
				}
			}
			nodeStructure = new NodeStructure(xValues, yValues, zValues, times);
			br.close();
		} catch (Exception e) {
			System.out.println("Error loading Node Struture from " + file.getName());
			e.printStackTrace();
		}
		return nodeStructure;
	}
	
	// Loops though all the files and reads detections into the paretoMap
	public static void readIAMFiles(File[] list, NodeStructure nodeStructure) {
		Point3i structure = nodeStructure.getIJKDimensions();
		Map<String, Map<String, Map<Integer, Float>>> paretoMap = new HashMap<String, Map<String, Map<Integer, Float>>>(); // Organized as: Specific Type<Scenario<Index, TTD>>
		String line;
		try {
			// Need to loop through all the files and read the files directly into the paretoMap
			for(File file: list) {
				String specificType = "";
				String scenario = "";
				int index = 0;
				BufferedReader br = new BufferedReader(new FileReader(file));
				while ((line = br.readLine()) != null) {
					String[] lineList = line.split(","); //comma delimited
					if(lineList.length==5) {//header
						scenario = lineList[1];
						if(!scenarios.contains(scenario)) scenarios.add(scenario); // Add unique scenarios
						if(!dataTypes.contains(lineList[2])) dataTypes.add(lineList[2]); // Add unique parameters
						specificType = lineList[2] + "_" + lineList[3] + "_" + lineList[4];
						if(!paretoMap.containsKey(specificType))
							paretoMap.put(specificType, new HashMap<String, Map<Integer, Float>>());
						if(!paretoMap.get(specificType).containsKey(scenario)) //scenario
							paretoMap.get(specificType).put(scenario, new HashMap<Integer, Float>());
					} else if(lineList.length==4) {//data
						float time = Float.parseFloat(lineList[3]);
						if(time < 1e25) // No detection is usually represented by 1e30, don't add those
							paretoMap.get(specificType).get(scenario).put(Constants.getNodeNumber(structure, index), time);
						index++;
					}
				}
				br.close();
			}
		} catch (Exception e) {
			System.out.println("Error loading scenarios");
			e.printStackTrace();
		}
	}
	
	/**					**\
	 * Getters & Setters *
	 * 					 *
	\*					 */
	
	public static Map<String, Map<String, Map<Integer, Float>>> getParetoMap() {
		return paretoMap;
	}
	
	public static List<String> getDataTypes() {
		return dataTypes;
	}
	
	public static List<String> getScenarios() {
		return scenarios;
	}
}
