package hdf5Tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import objects.NodeStructure;
import objects.ScenarioSet;
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
	
	/*
	 * IAM file structure goes:
	 * IAM, Scenario, Parameter, Trigger, Threshold (5)
	 * x, y, z, TTD (4)
	 * x, y, z, TTD (4)
	 * ...
	 */
	
	// Read one file to extract the Node Structure information from H5 files
	// TODO: Remove when done support uncompressed files
	public static NodeStructure readNodeStructureIAM_Uncompressed (File file) {
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
	
	// Read iam.grid file to extract the Node Structure information
	public static NodeStructure readNodeStructureIAM(File file) {
		NodeStructure nodeStructure = null;
		List<TimeStep> times = new ArrayList<TimeStep>();
		List<Float> xValues = new ArrayList<Float>();
		List<Float> yValues = new ArrayList<Float>();
		List<Float> zValues = new ArrayList<Float>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			for(int i=0; i<3; i++) {
				String line = br.readLine();
				String[] lineList = line.split(","); //comma delimited
				for(String value: lineList) {
					float dimension = Float.parseFloat(value);
					if(i==0) xValues.add(dimension);
					if(i==1) yValues.add(dimension);
					if(i==2) zValues.add(dimension);
				}
			}
			java.util.Collections.sort(xValues);
			java.util.Collections.sort(yValues);
			java.util.Collections.sort(zValues);
			nodeStructure = new NodeStructure(xValues, yValues, zValues, times);
			br.close();
		} catch (Exception e) {
			System.out.println("Error loading Node Structure from " + file.getName());
			e.printStackTrace();
		}
		return nodeStructure;
	}
	
	// Loops though all the files and reads detections into the detectionMap
	public static void readIAMFiles(IProgressMonitor monitor, File[] list, ScenarioSet set) {
		Point3i structure = set.getNodeStructure().getIJKDimensions();
		List<Float> times = new ArrayList<Float>();
		String line;
		// Need to loop through all the files and read the files directly into the detectionMap
		for(File file: list) {
			monitor.subTask("reading data from " + file);
			if(monitor.isCanceled()) return;
			String specificType = "";
			String scenario = "";
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				while ((line = br.readLine()) != null) {
					String[] lineList = line.split(","); //comma delimited
					if(lineList.length==5) {//header
						scenario = lineList[1];
						if(!scenarios.contains(scenario)) scenarios.add(scenario); // Add unique scenarios
						lineList[2] = lineList[2].replaceAll("_", " "); // Underscores cause problems for specificType
						lineList[3] = consistentTrigger(lineList[3]);
						// Convert the threshold to a float and back to string so it matches later
						if(lineList[4].contains("+"))
							lineList[4] = "+" + Float.toString(Float.parseFloat(lineList[4])); // Needed to maintain plus sign
						else
							lineList[4] = Float.toString(Float.parseFloat(lineList[4]));
						if(!dataTypes.contains(lineList[2])) dataTypes.add(lineList[2]); // Add unique parameters
						specificType = lineList[2] + "_" + lineList[3] + "_" + lineList[4]; // parameter_trigger_threshold (i.e. tds_rel_2)
						if(!set.getDetectionMap().containsKey(specificType))
							set.getDetectionMap().put(specificType, new HashMap<String, Map<Integer, Float>>());
						if(!set.getDetectionMap().get(specificType).containsKey(scenario)) //scenario
							set.getDetectionMap().get(specificType).put(scenario, new HashMap<Integer, Float>());
						// Add the sensorSettings now, since we have all the information
						if(!set.getSensorSettings().containsKey(lineList[2]))
							set.addSensorSetting(lineList[2], lineList[3], lineList[4]);
					} else if(lineList.length==4) {//data
						// Since IAM files only list detections, we need to determine the index from the NodeStructure
						int i = set.getNodeStructure().getX().indexOf(Float.parseFloat(lineList[0]));
						int j = set.getNodeStructure().getY().indexOf(Float.parseFloat(lineList[1]));
						int k = set.getNodeStructure().getZ().indexOf(Float.parseFloat(lineList[2]));
						int index = k + j*structure.getK() + i*structure.getJ()*structure.getK();
						// Now determine the node number and add the detection time to the DetectionMap
						float time = Float.parseFloat(lineList[3]);
						if(time < 1e25) { // No detection is usually represented by 1e30, don't add those
							set.getDetectionMap().get(specificType).get(scenario).put(Constants.getNodeNumber(structure, index), time);
							if(!times.contains(time)) times.add(time);
						}
					}
				}
				// In case a IAM file has no detections for a scenario, remove from detection map
				if(set.getDetectionMap().get(specificType).get(scenario).size()==0)
					set.getDetectionMap().get(specificType).remove(scenario);
				br.close();
				monitor.worked(1);
			} catch (Exception e) {
				System.out.println("Unable to read detection values from the IAM files...");
				e.printStackTrace();
			}
		}
		
		// Store all found time steps in node structure
		java.util.Collections.sort(times);
		List<TimeStep> timeSteps = new ArrayList<TimeStep>();
		for(int i=0; i<times.size(); i++) {
			TimeStep timeStep = new TimeStep(i, times.get(i), Math.round(times.get(i)));
			timeSteps.add(timeStep);
		}
		set.getNodeStructure().setTimeSteps(timeSteps);
	}
	
	// This can be used to map to alternative entries for IAM files
	private static String consistentTrigger(String trigger) {
		String temp = null;
		if(trigger.contains("below")) temp = "below";
		else if(trigger.contains("above")) temp = "above";
		else if(trigger.contains("relative")) temp = "rel";
		else if(trigger.contains("absolute")) temp = "abs";
		return temp;
	}
	
	/**					**\
	 * Getters & Setters *
	 * 					 *
	\*					 */
	
	public static List<String> getDataTypes() {
		return dataTypes;
	}
	
	public static List<String> getScenarios() {
		//Sort the scenarios - needed a special comparator for strings + numbers
		Collections.sort(scenarios, new Comparator<String>() {
			public int compare(String o1, String o2) {
		        return extractInt(o1) - extractInt(o2);
		    }

		    int extractInt(String s) {
		        String num = s.replaceAll("\\D", "");
		        // return 0 if no digits found
		        return num.isEmpty() ? 0 : Integer.parseInt(num);
		    }
		});
		return scenarios;
	}
}
