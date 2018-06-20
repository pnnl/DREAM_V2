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

import functions.SimulatedAnnealing;
import objects.NodeStructure;
import objects.SensorSetting;
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
	// Stores pareto optimal values to speed up that process
	public static Map<String, Map<String, Map<Integer, Float>>> paretoMap = new HashMap<String, Map<String, Map<Integer, Float>>>();
	
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
	
	// Read one file to extract the Node Structure information from H5 files
	public static NodeStructure readNodeStructureH5 (File file) {
		NodeStructure nodeStructure = null;
		String scenario = file.getName().replaceAll("\\.h5" , "");
		try {
			H5File hdf5File  = new H5File(file.getAbsolutePath(), HDF5Constants.H5F_ACC_RDONLY);
			hdf5Files.put(scenario, hdf5File);//TODO: remove this line and call it when counting scenarios
			hdf5File.open();
			
			// Get the root node
			Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
			// Get the data group
			for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
				String name = root.getMemberList().get(rootIndex).getName();
				
				if(name.startsWith("data")) {
					HashMap<Point3i, Float> porosity = new HashMap<Point3i, Float>();
					List<TimeStep> times = new ArrayList<TimeStep>();
					List<Float> xValues = new ArrayList<Float>();
					List<Float> yValues = new ArrayList<Float>();
					List<Float> zValues = new ArrayList<Float>();
					List<Float> edgex = new ArrayList<Float>();
					List<Float> edgey = new ArrayList<Float>();
					List<Float> edgez = new ArrayList<Float>();
					for(int groupIndex = 0; groupIndex < ((Group)root.getMemberList().get(rootIndex)).getMemberList().size(); groupIndex++) {
						//List<Float> dataRead = new ArrayList<Float>();
						Dataset dataset = (Dataset)((Group)root.getMemberList().get(rootIndex)).getMemberList().get(groupIndex);
						int dataset_id = dataset.open();
						float[] temp =  (float[])dataset.read();
	
						if(dataset.getName().equals("times")) {
							for(int i=0; i<temp.length; i++)
								times.add(new TimeStep(i, temp[i], Math.round(temp[i])));
						}
						else if(dataset.getName().equals("porosity") || dataset.getName().equals("porosities")) { // Should be porosity, but for a little while it was named porosities... remove second option when ready to discontinue
							long size = dataset.getDims()[0] * dataset.getDims()[1] * dataset.getDims()[2];
							temp = new float[(int)size];
							H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, temp);
							int counter = 0;
							for(int i=1; i<=dataset.getDims()[0]; i++) {
								for(int j=1; j<=dataset.getDims()[1]; j++) {
									for(int k = 1; k<=dataset.getDims()[2]; k++) {
										porosity.put(new Point3i(i, j, k), temp[counter]);
										counter++;
									}
								}
							}
						}
						else if(dataset.getName().equals("x")) xValues = Constants.arrayToList(temp);
						else if(dataset.getName().equals("y")) yValues = Constants.arrayToList(temp);
						else if(dataset.getName().equals("z")) zValues = Constants.arrayToList(temp);
						else if(dataset.getName().equals("vertex-x")) edgex = Constants.arrayToList(temp);
						else if(dataset.getName().equals("vertex-y")) edgey = Constants.arrayToList(temp);
						else if(dataset.getName().equals("vertex-z")) edgez = Constants.arrayToList(temp);
						dataset.close(dataset_id);
					}
					if(edgex.size()>0)
						nodeStructure = new NodeStructure(xValues, yValues, zValues, edgex, edgey, edgez, times, porosity);
					else //support for old file structures that don't have vertex information
						nodeStructure = new NodeStructure(xValues, yValues, zValues, times);
					
				} else if(name.startsWith("plot") && nodeStructure.getDataTypes().isEmpty()) {
					for(int groupIndex = 0; groupIndex < ((Group)root.getMemberList().get(rootIndex)).getMemberList().size(); groupIndex++) {
						Dataset dataset = (Dataset)((Group)root.getMemberList().get(rootIndex)).getMemberList().get(groupIndex);
						nodeStructure.getDataTypes().add(dataset.getName());
					}
					
				} else if(name.startsWith("statistics")) {
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
		} catch (Exception e) {
			System.out.println("Error loading Node Struture from " + file.getName());
			e.printStackTrace();
		}
		return nodeStructure;
	}
	
	public static List<String> queryScenarioNamesFromFiles(File[] list) {
		List<String> scenarios = new ArrayList<String>();
		for(File file: list) {
			scenarios.add(file.getName().replaceAll("\\.h5" , ""));
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
	
	
	
	
	
	
	
	
	
	
	
	public static Float queryValue(NodeStructure nodeStructure, String scenario, TimeStep timestep, String dataType, int index) throws Exception {
		// If cloud data exists, use me first
		//if(!hdf5CloudData.isEmpty()) //This was commented out because of a null pointer... and accessing from memory should be similar speed
	        //return queryValueFromCloud(nodeStructure, scenario, timestep, dataType, index);
	    // Otherwise, try memory, but the full set
	    if(!hdf5Data.isEmpty())
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
						return dataRead[Constants.getIndex(nodeStructure.getIJKDimensions().getI(), nodeStructure.getIJKDimensions().getJ(), nodeStructure.getIJKDimensions().getK(), nodeNumber)];										
					}
				}
			}
		}
		hdf5File.close();
		return 0f;
	}
	
	
	public static HashSet<Integer> queryNodes(NodeStructure nodeStructure, String scenario, String dataType, float lowerThreshold, float upperThreshold,
			Trigger trigger, DeltaType deltaType, IProgressMonitor monitor) throws Exception {
	    if(!hdf5Data.isEmpty())
	        return queryNodesFromMemory(nodeStructure, scenario, dataType, lowerThreshold, upperThreshold, trigger, deltaType, monitor);
	    else
	        return queryNodesFromFiles(nodeStructure, scenario, dataType, lowerThreshold, upperThreshold, trigger, deltaType, monitor);
	}
	
	
	public static HashSet<Integer> queryNodesFromMemory(NodeStructure nodeStructure, String scenario, String dataType, 
			float lowerThreshold, float upperThreshold, Trigger trigger, DeltaType deltaType, IProgressMonitor monitor) throws Exception {
		
		HashSet<Integer> nodes = new HashSet<Integer>();
		List<TimeStep> timeSteps = nodeStructure.getTimeSteps();
		
		for(int index = 0; index < nodeStructure.getTotalNodes(); index++) {
			int nodeNumber = Constants.getNodeNumber(nodeStructure.getIJKDimensions(), index);
			boolean exceededInThis = false;
			for(int startTimeIndex = 0; startTimeIndex < timeSteps.size(); startTimeIndex++) {
				
				if(monitor.isCanceled()) return null;
				
				float startTime = timeSteps.get(startTimeIndex).getRealTime();
				float valueAtStartTime = hdf5Data.get(scenario).get(startTime).get(dataType)[index];
				
				// Always compare from 0 in this case, end is then actually beginning
				if(startTimeIndex == 0)
					continue; // Not this one...
				float valueAtCurrentTime = valueAtStartTime;
				// Grab the first time step
				float timeStepAt0 = timeSteps.get(0).getRealTime();
				float valueAtTime0 = hdf5Data.get(scenario).get(timeStepAt0).get(dataType)[index];
				float change = 0;
				
				// This is the calculation for the percentage change
				if (trigger == Trigger.RELATIVE_DELTA) change = (valueAtCurrentTime - valueAtTime0) / valueAtTime0;
				// This is the calculation for absolute change
				else change = valueAtCurrentTime - valueAtTime0;
				
				// Handling the delta type, which can limit change in one direction (both by default)
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
			if(exceededInThis) {
				nodes.add(nodeNumber);
			}
		}
		return nodes;
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
			for(int startTimeIndex = 0; startTimeIndex < orderedTimes.length; startTimeIndex++) {
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
	
	public static Float queryStatistic(String dataType, int index) {
		// 0 = minimum
		// 1 = average
		// 2 = maximum
		if (!statistics.isEmpty() && !dataType.contains("Electrical Conductivity"))
			return statistics.get(dataType)[index];			
		return null;
	}
	
	// Instead of querying files for each value, generate a map with TTD at each node number for specific sensor settings
	public static void createParetoMap(NodeStructure nodeStructure, SensorSetting setting, String specificType) {
		Map<Integer, Float> baseline = new HashMap<Integer, Float>(); //stores values at the initial timestep
		Point3i structure = nodeStructure.getIJKDimensions();
		paretoMap.put(specificType, new HashMap<String, Map<Integer, Float>>());
		
		for(H5File hdf5File: hdf5Files.values()) { // For every scenario
			String scenario = hdf5File.getName().replaceAll("\\.h5" , "");
			try {
				paretoMap.get(specificType).put(scenario, new HashMap<Integer, Float>());
				hdf5File.open();
				Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
				boolean plotsAreTimeIndices = plotFileHack(nodeStructure, root);
				for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
					
					// Skip these
					if(root.getMemberList().get(rootIndex).getName().contains("data") || root.getMemberList().get(rootIndex).getName().contains("statistics"))
						continue;
					
					// First time step sets the baseline
					else if(Integer.parseInt(root.getMemberList().get(rootIndex).getName().replaceAll("plot", "")) == 0) {
						Object group =  root.getMemberList().get(rootIndex);
						for(int groupIndex = 0; groupIndex < ((Group)group).getMemberList().size(); groupIndex++) {
							Object child = ((Group)group).getMemberList().get(groupIndex);
							if(child instanceof Dataset && ((Dataset)child).getName().equals(setting.getType())) {
								// Found the right data type
								int dataset_id = ((Dataset)child).open();
								float[] dataRead = new float[nodeStructure.getTotalNodes()];
								H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
								((Dataset)child).close(dataset_id);
								for(int index=0; index<dataRead.length; index++) {
									baseline.put(Constants.getNodeNumber(structure, index), dataRead[index]);
								}
							}
						}
					
					// When looping through other timesteps, compare with the baseline
					} else {
						int timeIndex = Integer.parseInt(root.getMemberList().get(rootIndex).getName().replaceAll("plot", ""));
						float timestep = (float)timeIndex;
						if(plotsAreTimeIndices)
							System.out.println("The timestep isn't a time index... check to make sure this isn't causing problems.");
						Object group =  root.getMemberList().get(rootIndex);
						for(int groupIndex = 0; groupIndex < ((Group)group).getMemberList().size(); groupIndex++) {
							Object child = ((Group)group).getMemberList().get(groupIndex);
							if(child instanceof Dataset && ((Dataset)child).getName().equals(setting.getType())) {
								// Found the right data type
								int dataset_id = ((Dataset)child).open();
								float[] dataRead = new float[nodeStructure.getTotalNodes()];
								H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
								((Dataset)child).close(dataset_id);
								for(int index=0; index<dataRead.length; index++) {
									int nodeNumber = Constants.getNodeNumber(structure, index);
									// If the node triggers, save the timestep
									if(SimulatedAnnealing.paretoSensorTriggered(setting, dataRead[index], baseline.get(nodeNumber))) {
										if(paretoMap.get(specificType).get(scenario).get(nodeNumber)==null || paretoMap.get(specificType).get(scenario).get(nodeNumber) > timestep)
											paretoMap.get(specificType).get(scenario).put(nodeNumber, timestep);
									}
								}
							}
						}
						
					}
				}
			} catch (Exception e) {
				System.out.println("Unable to read detection values from the hdf5 files...");
				e.printStackTrace();
			}
		}
	}
}
