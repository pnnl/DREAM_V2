package hdf5Tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import objects.NodeStructure;
import objects.ScenarioSet;
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
	
	// Points to all the hdf5 files in the directory
	public static Map<String, H5File> hdf5Files = new HashMap<String, H5File>();
	// Stores global statistics - min, average, max
	public static Map<String, float[]> statistics = new HashMap<String, float[]>();
	
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
		statistics.clear();
		try {
			H5File hdf5File  = new H5File(file.getAbsolutePath(), HDF5Constants.H5F_ACC_RDONLY);
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
			String scenario = file.getName().replaceAll("\\.h5" , "");
			scenarios.add(scenario);
			H5File hdf5File  = new H5File(file.getAbsolutePath(), HDF5Constants.H5F_ACC_RDONLY);
			hdf5Files.put(scenario, hdf5File);
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
	
	/**					**\
	 * Getters & Setters *
	 * 					 *
	\*					 */
	
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
	
	public static Float getStatistic(String dataType, int index) {
		// 0 = minimum
		// 1 = average
		// 2 = maximum
		if (!statistics.isEmpty() && !dataType.contains("all") && !dataType.contains("Electrical Conductivity"))
			return statistics.get(dataType)[index];
		return null;
	}
	
	
	// Instead of querying files for each value, generate a map with TTD at each node number for specific sensor settings
	public static void createDetectionMap(ScenarioSet set, SensorSetting setting, String specificType) {
		
		long startTime = System.currentTimeMillis();
		
		Map<Integer, Float> baseline = new HashMap<Integer, Float>(); //stores values at the initial timestep
		Point3i structure = set.getNodeStructure().getIJKDimensions();
		for(H5File hdf5File: hdf5Files.values()) { // For every scenario
			String scenario = null;  //Scenarios have an ID, and we want those used in detectionMap to match the list of scenarios in nodeStructure
			for(String scenarioCompare: set.getScenarios()) {
				if(scenarioCompare.contains(hdf5File.getName().replaceAll("\\.h5" , ""))) {
					scenario = scenarioCompare;
					break;
				}
			}
			try {
				if(!set.getDetectionMap().containsKey(specificType))
					set.getDetectionMap().put(specificType, new HashMap<String, Map<Integer, Float>>());
				if(!set.getDetectionMap().get(specificType).containsKey(scenario))
					set.getDetectionMap().get(specificType).put(scenario, new HashMap<Integer, Float>());
				hdf5File.open();
				Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
				boolean plotsAreTimeIndices = plotFileHack(set.getNodeStructure(), root);
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
								float[] dataRead = new float[set.getNodeStructure().getTotalNodes()];
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
								float[] dataRead = new float[set.getNodeStructure().getTotalNodes()];
								H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
								((Dataset)child).close(dataset_id);
								for(int index=0; index<dataRead.length; index++) {
									int nodeNumber = Constants.getNodeNumber(structure, index);
									// If the node triggers, save the timestep
									if(sensorTriggered(setting, dataRead[index], baseline.get(nodeNumber))) {
										if(set.getDetectionMap().get(specificType).get(scenario).get(nodeNumber)==null || set.getDetectionMap().get(specificType).get(scenario).get(nodeNumber) > timestep)
											set.getDetectionMap().get(specificType).get(scenario).put(nodeNumber, timestep);
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
		long elapsedTime = (System.currentTimeMillis() - startTime)/1000;
		System.out.println("You just created a detection map for " + specificType + " in " + elapsedTime + " seconds! Awesome! So Fast!");
	}
	
	public static Boolean sensorTriggered(SensorSetting setting, Float currentValue, Float valueAtTime0) {
		Boolean triggered = false;
		if(currentValue==null) return triggered;
		
		// See if we exceeded threshold
		if(setting.getTrigger()==Trigger.MINIMUM_THRESHOLD) {
			triggered = setting.getDetectionThreshold() <= currentValue;
		} else if(setting.getTrigger()==Trigger.MAXIMUM_THRESHOLD) {
			triggered = setting.getDetectionThreshold() >= currentValue;
		} else if(setting.getTrigger()==Trigger.RELATIVE_DELTA) {
			float change = valueAtTime0 == 0 ? 0 : ((currentValue - valueAtTime0) / valueAtTime0);
			if(setting.getDeltaType()==DeltaType.INCREASE) triggered = setting.getDetectionThreshold() <= change;
			else if(setting.getDeltaType()==DeltaType.DECREASE) triggered = setting.getDetectionThreshold() >= change;
			else if(setting.getDeltaType()==DeltaType.BOTH) triggered = setting.getDetectionThreshold() <= Math.abs(change);
		} else { //if(setting.getTrigger()==Trigger.ABSOLUTE_DELTA)
			float change = currentValue - valueAtTime0;
			if(setting.getDeltaType() == DeltaType.INCREASE) triggered = setting.getDetectionThreshold() <= change;
			else if(setting.getDeltaType() == DeltaType.DECREASE) triggered = setting.getDetectionThreshold() >= change;
			else if(setting.getDeltaType() == DeltaType.BOTH) triggered = setting.getDetectionThreshold() <= Math.abs(change);
		}
		if(triggered==true)
			triggered = true;
		return triggered;
	}
	
}
