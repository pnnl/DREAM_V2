package hdf5Tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import org.eclipse.core.runtime.IProgressMonitor;

import objects.NodeStructure;
import objects.SensorSetting.DeltaType;
import objects.SensorSetting.Trigger;
import objects.TimeStep;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.h5.H5File;
import utilities.Constants;
import utilities.Point3i;

import objects.SensorSetting;

/**
 * Utility functions for use in reading and parsing hdf5 files to DREAM formats
 * @author port091
 * @author whit162
 */

public class HDF5Interface {
	// Storing the files
	public static Map<String, H5File> hdf5Files;
	// If we can store this in memory
	public static Map<String, Map<Float, Map<String, float[]>>> hdf5Data = new HashMap<String, Map<Float, Map<String, float[]>>>();
	// Represents the optimized solution
	public static Map<String, Map<Float, Map<String, Map<Integer, Float>>>> hdf5CloudData = new HashMap<String, Map<Float, Map<String, Map<Integer, Float>>>>();
	// Stores global statistics - min, average, max
	public static Map<String, float[]> statistics = new HashMap<String, float[]>();
	
	
	public static Float queryValue(NodeStructure nodeStructure, String scenario, TimeStep timestep, String dataType, int index) throws Exception {
		// If cloud data exists, use me first
		if(!hdf5CloudData.isEmpty())
	        return queryValueFromCloud(nodeStructure, scenario, timestep, dataType, index);
	    // Otherwise, try memory, but the full set
	    else if(!hdf5Data.isEmpty())
	        return queryValueFromMemory(nodeStructure, scenario, timestep, dataType, index);                     
	    // Sad day, read from the file :(
	    else
	        return queryValueFromFile(nodeStructure, scenario, timestep, dataType, index);
	}
	
	public static Float queryValueFromCloud(NodeStructure nodeStructure, String scenario, TimeStep timestep, String dataType, int index) throws Exception {
		float years = timestep.getTimeStep();
		if(hdf5CloudData.containsKey(scenario) && 
		   hdf5CloudData.get(scenario).containsKey(years) &&
		   hdf5CloudData.get(scenario).get(years).containsKey(dataType) && 
		   hdf5CloudData.get(scenario).get(years).get(dataType).containsKey(index)) {
			return hdf5CloudData.get(scenario).get(years).get(dataType).get(index);
		}
		return null;
	}
	
	public static float queryValueFromMemory(NodeStructure nodeStructure, String scenario, TimeStep timestep, String dataType, int index) {
		float years = timestep.getRealTime();
		return hdf5Data.get(scenario).get(years).get(dataType)[index-1];
	}
	
	public static float queryValueFromFile(NodeStructure nodeStructure, String scenario, TimeStep timestep, String dataType, int nodeNumber) throws Exception {
		H5File hdf5File = hdf5Files.get(scenario); // Get the correct file for the scenario
		hdf5File.open();
		Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
		boolean plotsAreTimeIndices = plotFileHack(nodeStructure, root);
		for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
			// Found the right time step
			if(root.getMemberList().get(rootIndex).getName().contains("data") || root.getMemberList().get(rootIndex).getName().contains("statistics"))
				continue;
			if(Integer.parseInt(root.getMemberList().get(rootIndex).getName().replaceAll("plot", "")) == (plotsAreTimeIndices ? timestep.getTimeStep(): (int) timestep.getRealTime())) {
				Object group =  root.getMemberList().get(rootIndex);
				for(int groupIndex = 0; groupIndex < ((Group)group).getMemberList().size(); groupIndex++) {
					Object child = ((Group)group).getMemberList().get(groupIndex);
					if(child instanceof Dataset && ((Dataset)child).getName().equals(dataType)) {
						// Found the right data type
						int dataset_id = ((Dataset)child).open();
						float[] dataRead = new float[nodeStructure.getTotalNodes()];
						H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
						((Dataset)child).close(dataset_id);
						return dataRead[Constants.getIndex(nodeStructure.getIJKDimensions().getI(), nodeStructure.getIJKDimensions().getJ(), nodeStructure.getIJKDimensions().getK(), nodeNumber)];	// Return data at the right index											
					}
				}
			}
		}
		hdf5File.close();
		return 0f;
	}
	
	public static float queryStatistic(String dataType, int index) {
		// 0 = minimum
		// 1 = average
		// 2 = maximum
		if (statistics.isEmpty()) {
			try {
				for(H5File hdf5File: hdf5Files.values()) { // For every scenario
					hdf5File.open();
					Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
					for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) { // Search for the statistics node (should be last)
						if(root.getMemberList().get(rootIndex).getName().equals("statistics")) {
							for(int groupIndex = 0; groupIndex < ((Group)root.getMemberList().get(rootIndex)).getMemberList().size(); groupIndex++) {
								Dataset dataset = (Dataset)((Group)root.getMemberList().get(rootIndex)).getMemberList().get(groupIndex);
								int dataset_id = dataset.open();
								float[] temp = (float[])dataset.read();
								if (statistics.containsKey(dataset.getName())) {
									temp[0] = Math.min(temp[0], statistics.get(dataset.getName())[0]);
									temp[1] = temp[1] + statistics.get(dataset.getName())[1]; //Sum averages now, divide by time step later
									temp[2] = Math.max(temp[2], statistics.get(dataset.getName())[2]);
								}
								statistics.put(dataset.getName(), temp);
								dataset.close(dataset_id);
							}
						}
					}
					// TODO: ---When we want to discontinue support of old h5 files without statistics, remove this code---
					if (statistics.isEmpty()) {
						for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
							if(!root.getMemberList().get(rootIndex).getName().equals("data")) {
								float min = Float.MAX_VALUE;
								float max = Float.MIN_VALUE;
								float sum = 0;
								int counter = 0;
								for(int groupIndex = 0; groupIndex < ((Group)root.getMemberList().get(rootIndex)).getMemberList().size(); groupIndex++) {
									Dataset dataset = (Dataset)((Group)root.getMemberList().get(rootIndex)).getMemberList().get(groupIndex);
									int dataset_id = dataset.open();
									float[] dataRead = new float[SensorSetting.nodeStructure.getTotalNodes()];
									H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);
									for(float value: dataRead) {
										if(value < min)
											min = value;
										if(value > max)
											max = value;
										sum = sum + value;
										counter++;
									}
									float[] temp = new float[3];
									temp[0] = min;
									temp[1] = sum/counter;
									temp[2] = max;
									statistics.put(dataset.getName(), temp);
									dataset.close(dataset_id);
								}
								break; // First time step only
							}
							System.gc();
						}
						hdf5File.close();
						break; // First scenario only
					}
					// ---End of code to remove---
					hdf5File.close();
				}
				for (String type: statistics.keySet()) {
					statistics.get(type)[1] = statistics.get(type)[1] / hdf5Files.size();
				}
			} catch (Exception e) {
				System.out.println("Something went wrong while adding statistics.");
				e.printStackTrace();
			}
		}
		//Handles duplicate variables with no statistics field of their own
		else if (dataType.contains("_") && (dataType.indexOf("_")==dataType.length()-2 || dataType.indexOf("_")==dataType.length()-3)){ //Up to 99 variables
			dataType = dataType.substring(0,dataType.indexOf("_"));
		}
		//Handles ERT
		if (dataType.contains("Electrical Conductivity"))
			return 0;
		
		return statistics.get(dataType)[index];
	}
	
	
	public static HashSet<Integer> queryNodes(NodeStructure nodeStructure, String scenario, String dataType, 
			float lowerThreshold, float upperThreshold, Trigger trigger, DeltaType deltaType, IProgressMonitor monitor) throws Exception {
		// If data exists in memory, use me first
	    if(!hdf5Data.isEmpty())
	        return queryNodesFromMemory(nodeStructure, scenario, dataType, lowerThreshold, upperThreshold, trigger, deltaType, monitor);
	    // Sad day, read from the file :(
	    else
	        return queryNodesFromFiles(nodeStructure, scenario, dataType, lowerThreshold, upperThreshold, trigger, deltaType, monitor);
	}
	
	
	public static HashSet<Integer> queryNodesFromMemory(NodeStructure nodeStructure, String scenario, String dataType, float threshold, IProgressMonitor monitor) {
		return queryNodesFromMemory(nodeStructure, scenario, dataType, threshold, Float.MAX_VALUE, monitor);
	}

	public static HashSet<Integer> queryNodesFromMemory(NodeStructure nodeStructure, String scenario, 
			String dataType, float lowerThreshold, float upperThreshold, IProgressMonitor monitor) {
		HashSet<Integer> nodes = new HashSet<Integer>();		
		int i = nodeStructure.getIJKDimensions().getI();
		int j = nodeStructure.getIJKDimensions().getJ();
		int k = nodeStructure.getIJKDimensions().getK();		
		int totalNodes = i*j*k;		
		for(int index = 0; index < totalNodes; index++) {
			int nodeNumber = Constants.getNodeNumber(nodeStructure.getIJKDimensions(), index);
			for(float timeStep: hdf5Data.get(scenario).keySet()) {

				if(monitor.isCanceled()) return nodes;
				
				float value = hdf5Data.get(scenario).get(timeStep).get(dataType)[index];
				if(value >= lowerThreshold && value < upperThreshold) {
					//This is most likely wrong. See queryNodesFromFiles to see the updated index logic that was not copied to this.
					nodes.add(nodeNumber);
					break; // Next node index
				}					
			}
		}		
		return nodes;
	}

	public static HashSet<Integer> queryNodesFromMemory(NodeStructure nodeStructure, String scenario, String dataType, 
			float lowerThreshold, float upperThreshold, Trigger trigger, DeltaType deltaType, IProgressMonitor monitor) throws Exception {

		HashSet<Integer> nodes = new HashSet<Integer>();		
		int i = nodeStructure.getIJKDimensions().getI();
		int j = nodeStructure.getIJKDimensions().getJ();
		int k = nodeStructure.getIJKDimensions().getK();		
		int totalNodes = i*j*k;		
		List<TimeStep> timeSteps = nodeStructure.getTimeSteps();

		for(int index = 0; index < totalNodes; index++) {
			int nodeNumber = Constants.getNodeNumber(nodeStructure.getIJKDimensions(), index);
			boolean exceededInThis = false;							
			for(int startTimeIndex = 0; startTimeIndex < timeSteps.size(); startTimeIndex++) {

				if(monitor.isCanceled()) return nodes;
				
				float valueAtStartTime = hdf5Data.get(scenario).get(timeSteps.get(startTimeIndex).getRealTime()).get(dataType)[index];	

				// Always compare from 0 in this case, end is then actually beginning
				if(startTimeIndex == 0)
					continue; // Not this one...
				float valueAtTime0 = hdf5Data.get(scenario).get(timeSteps.get(0).getRealTime()).get(dataType)[index];
				float valueAtCurrentTime = valueAtStartTime;

				float change = trigger == Trigger.RELATIVE_DELTA ? 
						// This is the calculation for the percentage (checked)
						valueAtTime0 == 0 ? 0 : ((valueAtCurrentTime - valueAtTime0) / valueAtTime0) :
							// This is the calculation for non percentage (not checked)
							valueAtCurrentTime - valueAtTime0;

						if(deltaType == DeltaType.INCREASE && lowerThreshold <= change) {						
							exceededInThis = true;
							break; // Done after we find one time step
						} else if(deltaType == DeltaType.DECREASE && lowerThreshold >= change) {						
							exceededInThis = true;
							break; // Done after we find one time step
						} else if(deltaType == DeltaType.BOTH && lowerThreshold <= Math.abs(change)){
							exceededInThis = true;
							break; // Done after we find one time step
						}
			}
			//This is most likely wrong. See queryNodesFromFiles to see the updated index logic that was not copied to this.
			if(exceededInThis) {
				nodes.add(nodeNumber);
			}			
		}		
		return nodes;
	}
	
	public static void fillNodeStructureFromFiles(NodeStructure nodeStructure) throws Exception {
		// We assume all the files have the same node structure over all scenarios
		H5File hdf5File = hdf5Files.get(hdf5Files.keySet().toArray()[0]);
		hdf5File.open();
		Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
		for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) { // Search for the data node (should be first)
			if(root.getMemberList().get(rootIndex).getName().equals("data")) {	// We can get the node structure data from this one
				float[] times, xValues, yValues, zValues, porosities;
				times = xValues = yValues = zValues = porosities = null;
				Dataset porosityDataset = null;
				boolean porosityFound = false;
				for(int groupIndex = 0; groupIndex < ((Group)root.getMemberList().get(rootIndex)).getMemberList().size(); groupIndex++) {
					Dataset dataset = (Dataset)((Group)root.getMemberList().get(rootIndex)).getMemberList().get(groupIndex);
					int dataset_id = dataset.open();
					//		if(dataset.getName().equals("steps")) {	steps =  (Float[])dataset.read(); }		 // TODO: Do we need steps?			
					if(dataset.getName().equals("times")) {	times =  (float[])dataset.read(); }					
					if(dataset.getName().equals("x")) {	xValues =  (float[])dataset.read(); }					
					if(dataset.getName().equals("y")) {	yValues =  (float[])dataset.read(); }					
					if(dataset.getName().equals("z")) {	zValues =  (float[])dataset.read(); }	
					if(dataset.getName().equals("porosities")){ 
						porosityDataset = dataset; 
						porosityFound = true;
					} //Need to store these for later, we need to know the xyz dimensions

					dataset.close(dataset_id);
				}
				
				for(float x: xValues) nodeStructure.getX().add(x);
				for(float y: yValues) nodeStructure.getY().add(y);
				for(float z: zValues) nodeStructure.getZ().add(z);
				
				nodeStructure.setIJKDimensions(xValues.length, yValues.length, zValues.length);
				
				// Should have all the info we need now to read the porosities
				if(porosityFound){
					int dataset_id = porosityDataset.open();
					porosities = new float[xValues.length*yValues.length*zValues.length];
					H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, porosities);	
					
					//Now we can get the porosities
					int testcounter = 0;
					for(int i=0; i<xValues.length; i++){
						for(int j=0; j<yValues.length; j++){
							for(int k = 0; k<zValues.length; k++){
								nodeStructure.getPorosityOfNode().put(new Point3i(i+1, j+1, k+1), porosities[testcounter]);
								testcounter++;
							}
						}
					}
					
					porosityDataset.close(dataset_id);
				}
				
				for(int i = 0; i < times.length; i++) {
					nodeStructure.getTimeSteps().add(new TimeStep(i, times[i]));
				}
			} else if(root.getMemberList().get(rootIndex).getName().startsWith("plot")) { // Need to get the data types from this one
				for(int groupIndex = 0; groupIndex < ((Group)root.getMemberList().get(rootIndex)).getMemberList().size(); groupIndex++) {
					nodeStructure.getDataTypes().add(((Dataset)((Group)root.getMemberList().get(rootIndex)).getMemberList().get(groupIndex)).getName());
				}
				return; // done
			}
		}
		hdf5File.close();
	}

	public static List<String> queryScenarioNamesFromFiles() {
		List<String> scenarios = new ArrayList<String>();
		for(String key: hdf5Files.keySet()) {
			scenarios.add(key);
		}
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
	
	public static HashSet<Integer> queryNodesFromFiles(NodeStructure nodeStructure, String scenario, 
			String dataType, float lowerThreshold, float upperThreshold, IProgressMonitor monitor) throws Exception {
		HashSet<Integer> nodes = new HashSet<Integer>();		
		H5File hdf5File = hdf5Files.get(scenario); // For the given scenario
		hdf5File.open();
		Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
		TreeMap<Float, Group> sortedTimes = new TreeMap<Float, Group>();
		boolean plotAreTimeIndices = plotFileHack(nodeStructure, root);
	
		for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) { // For every time step
			Object group =  root.getMemberList().get(rootIndex);
			String name = ((Group)group).getName().replaceAll("plot", "");
			if(name.contains("data") || name.contains("statistics"))
				continue;
			int timeIndex = Integer.parseInt(name);
			// These have to be in order...
			sortedTimes.put(plotAreTimeIndices ? nodeStructure.getTimeAt(timeIndex) : (float)timeIndex, (Group)group);
				
		}
		for(Float timeStep: sortedTimes.keySet()) {
			Group group = sortedTimes.get(timeStep);			
			for(int groupIndex = 0; groupIndex < ((Group)group).getMemberList().size(); groupIndex++) {
				Object child = ((Group)group).getMemberList().get(groupIndex);
				if(child instanceof Dataset && ((Dataset)child).getName().equals(dataType)) { // If this is the data type we're interested in
					int dataset_id = ((Dataset)child).open();
					float[] dataRead = new float[nodeStructure.getTotalNodes()];
					H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
					((Dataset)child).close(dataset_id);
					for(int i = 0; i < dataRead.length; i++) {

						if(monitor != null && monitor.isCanceled()) return nodes;

						// Checks if node is within threshold, then add to validNodes and cloud
						if(dataRead[i] >= lowerThreshold && dataRead[i] < upperThreshold) {
							int nodeNumber = Constants.getNodeNumber(nodeStructure.getIJKDimensions(), i);
							if(!nodes.contains(nodeNumber)) {
								addNodeToCloud(scenario, timeStep, dataType, nodeNumber, dataRead[i]);
								nodes.add(nodeNumber);
							}
						// Outside of threshold, ignore
						} else {
							//System.out.println("Skipping");
						}
					}
				}
			}
		}
		hdf5File.close();
		return nodes;
	}

	private static void addNodeToCloud(String scenario, float timeInYears, String dataType, int nodeNumber, float value) {
		try {
		if(hdf5CloudData == null)
			hdf5CloudData = new HashMap<String, Map<Float, Map<String, Map<Integer, Float>>>>();
		if(!hdf5CloudData.containsKey(scenario))
			hdf5CloudData.put(scenario, new HashMap<Float, Map<String, Map<Integer, Float>>>());
		if(!hdf5CloudData.get(scenario).containsKey(timeInYears))
			hdf5CloudData.get(scenario).put(timeInYears, new HashMap<String, Map<Integer, Float>>());
		if(!hdf5CloudData.get(scenario).get(timeInYears).containsKey(dataType))
			hdf5CloudData.get(scenario).get(timeInYears).put(dataType, new HashMap<Integer, Float>());
		hdf5CloudData.get(scenario).get(timeInYears).get(dataType).put(nodeNumber, value);	
		} catch (Exception e) {
			float totalNodes = 0;
			for(String sc : hdf5CloudData.keySet()) {
				for(float ts: hdf5CloudData.get(sc).keySet()) {
					for(String dt: hdf5CloudData.get(sc).get(ts).keySet()) {
						totalNodes = hdf5CloudData.get(sc).get(ts).get(dt).keySet().size();
						if(hdf5CloudData.get(sc).get(ts).get(dt).keySet().size() > 1000)
							System.out.println(sc + ", " + ts + ", " + dt + " , " + hdf5CloudData.get(sc).get(ts).get(dt).keySet().size());
					}
				}
			}
			System.out.print("Cloud currently has: ");
			System.out.println(totalNodes);
			JOptionPane.showMessageDialog(null, "Dream is out of memory!  Please reduce your solution space, current space: " + totalNodes);
			e.printStackTrace();
		}
	}

	public static HashSet<Integer> queryNodesFromFiles(NodeStructure nodeStructure, String scenario, String dataType, 
			float lowerThreshold, float upperThreshold, Trigger trigger, DeltaType deltaType, IProgressMonitor monitor) throws Exception {

		H5File h5file = hdf5Files.get(scenario);
		h5file.open();
		
		// Get the root node:
		Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)h5file.getRootNode()).getUserObject();
	
		Map<Float, float[]> valuesByScenarioAndTime = new TreeMap<Float, float[]>();

		int totalNodes = nodeStructure.getTotalNodes();

		boolean plotAreTimeIndices = plotFileHack(nodeStructure, root);
	
		// Get the data group
		for(int timestep = 0; timestep < root.getMemberList().size(); timestep++) {
			Group group =  (Group)root.getMemberList().get(timestep); // time steps
			if(group.getName().startsWith("plot")) {		
				for(int groupId = 0; groupId < group.getMemberList().size(); groupId++) {
					Dataset dataset = (Dataset)group.getMemberList().get(groupId);		
					if(!dataset.getName().equals(dataType))
						continue;
					int dataset_id = dataset.open();
					float[] dataRead = new float[totalNodes];	 // expecting an array with size of the grid
					H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);						
					String name = ((Group)group).getName().replaceAll("plot", "");
					int timeIndex = Integer.parseInt(name);
					valuesByScenarioAndTime.put(plotAreTimeIndices ? nodeStructure.getTimeAt(timeIndex) : (float)timeIndex, dataRead);
					break; //found the correct parameter, move on now...
				}
			}
		}

		Object[] orderedTimes = (Object[]) valuesByScenarioAndTime.keySet().toArray();
		HashSet<Integer> nodes = new HashSet<Integer>();

		for(int index = 0; index < totalNodes; index++) {	
			int nodeNumber = Constants.getNodeNumber(nodeStructure.getIJKDimensions(), index);
			boolean exceededInThis = false;	
			int startTimeIndex = 0;
			for(startTimeIndex = 0; startTimeIndex < orderedTimes.length; startTimeIndex++) {
				if(monitor!=null && monitor.isCanceled())
					return nodes;
				
				float startTime = (Float) orderedTimes[startTimeIndex];
				float valueAtStartTime = valuesByScenarioAndTime.get(startTime)[index];
				//	int valueAtStartTimeInt = (int)(valueAtStartTime * epsilon);

				// Always compare from 0 in this case, end is then actually beginning
				if(startTimeIndex == 0)
					continue; // Not this one...
				float valueAtCurrentTime = valueAtStartTime;
				// Grab the first time step
				float timeStepAt0 = (Float) orderedTimes[0];
				float valueAtTime0 = valuesByScenarioAndTime.get(timeStepAt0)[index]; // Get the value there
				float change = 0;
				
				// This is the calculation for the percentage change
				if (trigger == Trigger.RELATIVE_DELTA) change = (valueAtCurrentTime - valueAtTime0) / valueAtTime0;
				// This is the calculation for absolute change
				else change = valueAtCurrentTime - valueAtTime0;
				
				// Handling the delta type, which can limit change in one direction (both by default)
				if(deltaType == DeltaType.INCREASE && lowerThreshold <= change) {
					exceededInThis = true;
					if(!nodes.contains(nodeNumber)) {
						addNodeToCloud(scenario, timeStepAt0, dataType, nodeNumber, valueAtTime0);
						addNodeToCloud(scenario, startTime, dataType, nodeNumber, valueAtCurrentTime);
					}
					break; // Done after we find one time step
				} else if(deltaType == DeltaType.DECREASE && lowerThreshold >= change) {
					exceededInThis = true;
					if(!nodes.contains(nodeNumber)) {
						addNodeToCloud(scenario, timeStepAt0, dataType, nodeNumber, valueAtTime0);
						addNodeToCloud(scenario, startTime, dataType, nodeNumber, valueAtCurrentTime);
					}
					break; // Done after we find one time step
				} else if(deltaType == DeltaType.BOTH && lowerThreshold <= Math.abs(change)){
					exceededInThis = true;
					if(!nodes.contains(nodeNumber)) {
						addNodeToCloud(scenario, timeStepAt0, dataType, nodeNumber, valueAtTime0);
						addNodeToCloud(scenario, startTime, dataType, nodeNumber, valueAtCurrentTime);
					}
					break; // Done after we find one time step
				}
			}
			if(exceededInThis) {
				nodes.add(nodeNumber);
			}				
		}
		h5file.close();

		return nodes;

	}
	
	public static boolean plotFileHack(NodeStructure nodeStructure, Group root) {
		// plotxyz is inconsitent between h5 files, it will either be plot[timeIndex] or plot[realTime]
		// For backwards compatibility, we're putting in a hack here to handle both cases... it will probably work...
		// most of the time...
		List<Integer> realTimeOrTimeIndex = new ArrayList<Integer>();
		for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) { // For every time step
			Object group =  root.getMemberList().get(rootIndex);
			String name = ((Group)group).getName().replaceAll("plot", "");
			if(name.contains("data") || name.contains("statistics"))
				continue;
			realTimeOrTimeIndex.add(Integer.parseInt(name));
		}
		
		boolean plotsAreTimeIndices = false;
		for(TimeStep timeStep: nodeStructure.getTimeSteps()) {
			if(!realTimeOrTimeIndex.contains((int)timeStep.getRealTime())) {
				plotsAreTimeIndices = true;
				break;
			}
		}
		return plotsAreTimeIndices;
	}
	
	/*	
	 *  Example of node number vs. index. Each cell has: 
	 *  1) i,j,k			- each of the three dimensions are 1 <= dim <= dimMax
	 *  2) node number		- 1-indexed, used by DREAM to store which nodes are triggered and to query from nodes
	 *  3) index			- 0-indexed, used in reading values from the hdf5 files.
	 *  _________________________    _________________________    _________________________    
	 * 	| 1,1,1 | 1,2,1 | 1,3,1 |    | 1,1,2 | 1,2,2 | 1,3,2 |    | 1,1,3 | 1,2,3 | 1,3,3 |
	 * 	| 1     | 4     | 7     |    | 10    | 13    | 16    |    | 19    | 22    | 25    |
	 * 	| 0     | 3     | 6     |    | 1     | 4     | 7     |    | 2     | 5     | 8     |
	 * 	|_______|_______|_______|    |_______|_______|_______|    |_______|_______|_______|    
	 * 	| 2,1,1 | 2,2,1 | 2,3,1 |    | 2,1,2 | 2,2,2 | 2,3,2 |    | 2,1,3 | 2,2,3 | 2,3,3 |
	 * 	| 2     | 5     | 8     |    | 11    | 14    | 17    |    | 20    | 23    | 26    |
	 * 	| 9     | 12    | 15    |    | 10    | 13    | 16    |    | 11    | 14    | 17    |
	 * 	|_______|_______|_______|    |_______|_______|_______|    |_______|_______|_______|    
	 * 	| 3,1,1 | 3,2,1 | 3,3,1 |    | 3,1,2 | 3,2,2 | 3,3,2 |    | 3,1,3 | 3,2,3 | 3,3,3 |
	 * 	| 3     | 6     | 9     |    | 12    | 15    | 18    |    | 21    | 24    | 27    |
	 * 	| 18    | 21    | 24    |    | 19    | 22    | 25    |    | 20    | 23    | 26    |
	 * 	|_______|_______|_______|    |_______|_______|_______|    |_______|_______|_______|
	 * 
	 */
	
	public static void loadHdf5Files(String location) {
		
		File hdf5Folder = new File(location);
		hdf5Files = new HashMap<String, H5File>();
		
		NodeStructure nodeStructure = null;
		int totalNodes = 0;
		int iMax = 0;
		int jMax = 0;
		int kMax = 0;
		
		long floatCount = 0;
		if(hdf5Folder.exists() && hdf5Folder.isDirectory()) {
			long size = 0;
			for(File file: hdf5Folder.listFiles())
				size += file.length();
			System.out.println("Directory size: " + size);
			
			long freeMemory = Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
			System.out.println("Free Memory: " + freeMemory);
			
			// Loop through contents for hdf5 files
			for(File file: hdf5Folder.listFiles()) {
				String scenario = file.getName().replaceAll("\\.h5" , "");
				// FileFormat hdf5Format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
				try {
					H5File hdf5File  = new H5File(file.getAbsolutePath(), HDF5Constants.H5F_ACC_RDONLY);	
					hdf5Files.put(scenario,hdf5File);

					if(size > freeMemory)
						continue;
					
					if(nodeStructure == null) {
						nodeStructure = new NodeStructure("");
						System.out.println("Domain size: " + nodeStructure.getIJKDimensions());
						totalNodes = nodeStructure.getIJKDimensions().getI()*nodeStructure.getIJKDimensions().getJ()*nodeStructure.getIJKDimensions().getK();
						iMax = nodeStructure.getIJKDimensions().getI();
						jMax = nodeStructure.getIJKDimensions().getJ();
						kMax = nodeStructure.getIJKDimensions().getK();
					}					
					
					// Try and store it into memory?
					if(hdf5Data == null)
						hdf5Data = new HashMap<String, Map<Float, Map<String, float[]>>>(hdf5Folder.listFiles().length, 0.9f);
					if(!hdf5Data.containsKey(scenario))
						hdf5Data.put(scenario, new HashMap<Float, Map<String, float[]>>(nodeStructure.getTimeSteps().size(), 0.9f));
					
					// Get the root node:
					Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
					// Get the data group
					for(int ts = 0; ts < root.getMemberList().size(); ts++) {
						String name = root.getMemberList().get(ts).getName();
						if(!name.startsWith("plot"))
							continue;
						Integer timeStep = Integer.parseInt(name.replaceAll("plot", ""));
						boolean plotsAreTimeIndices = HDF5Interface.plotFileHack(nodeStructure, root);
						timeStep = plotsAreTimeIndices ? nodeStructure.getTimeAt(timeStep).intValue() : timeStep;
						Object group =  root.getMemberList().get(ts); // timesteps	
						
						if(!hdf5Data.get(scenario).containsKey(timeStep))
							hdf5Data.get(scenario).put(nodeStructure.getTimeAt(timeStep), new HashMap<String, float[]>(((Group)group).getMemberList().size(), 0.9f));
						//Object group =  root.getMemberList().get(timestep+1); // timesteps
						for(int i = 0; i < ((Group)group).getMemberList().size(); i++) {
							Object child = ((Group)group).getMemberList().get(i);// z is index 0
							if(child instanceof Dataset) {
								Dataset dataset = (Dataset)child;
								String dataName = dataset.getName();
								int dataset_id = dataset.open();
								float[] dataRead = new float[totalNodes];	 // expecting an array with size of the grid
								H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);				
								float[] nodeOrder = new float[totalNodes];
								for(int j = 0; j < totalNodes; j++) {
									nodeOrder[Constants.getNodeNumber(iMax, jMax, kMax, j)-1] = dataRead[j];
								}								
								hdf5Data.get(scenario).get(nodeStructure.getTimeAt(timeStep)).put(dataName, nodeOrder);
								dataset.close(dataset_id);
								floatCount+=totalNodes;
					//			System.out.println(floatCount);
							}
						}
						
					}	
														
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("#Floats stored in memory: " + floatCount);
	}
}
