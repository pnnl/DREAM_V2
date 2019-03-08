package hdf5Tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

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
	 * 
	 *  _________________________    _________________________
	 *  | 1,1,1 | 2,1,1 | 3,1,1 |    | 1,1,2 | 2,1,2 | 3,1,2 |
	 * 	| 1     | 2     | 3     |    | 13    | 14    | 15    |
	 * 	| 0     | 8     | 16    |    | 1     | 9     | 17    |
	 * 	|_______|_______|_______|    |_______|_______|_______|  
	 * 	| 1,2,1 | 2,2,1 | 3,2,1 |    | 1,2,2 | 2,2,2 | 3,2,2 |
	 * 	| 4     | 5     | 6     |    | 16    | 17    | 18    |
	 * 	| 2     | 10    | 18    |    | 3     | 11    | 19    |
	 * 	|_______|_______|_______|    |_______|_______|_______| 
	 * 	| 1,3,1 | 2,3,1 | 3,3,1 |    | 1,3,2 | 2,3,2 | 3,3,2 |
	 * 	| 7     | 8     | 9     |    | 19    | 20    | 21    |
	 * 	| 4     | 12    | 20    |    | 5     | 13    | 21    |
	 * 	|_______|_______|_______|    |_______|_______|_______|  
	 * 	| 1,4,1 | 2,4,1 | 3,4,1 |    | 1,4,2 | 2,4,2 | 3,4,2 |
	 * 	| 10    | 11    | 12    |    | 21    | 22    | 23    |
	 * 	| 6     | 14    | 22    |    | 7     | 15    | 23    |
	 * 	|_______|_______|_______|    |_______|_______|_______|
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
			hdf5File.close();
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
	
	public static Float getStatistic(String dataType, int index) {
		// 0 = minimum
		// 1 = average
		// 2 = maximum
		if (!statistics.isEmpty() && !dataType.contains("allSensors") && !dataType.contains("Electrical Conductivity"))
			if(statistics.containsKey(dataType)) //TODO: This is just a temporary fix - I need to fix the converter to get statistics for all parameters, then this line can be removed.
				return statistics.get(dataType)[index];
		return null;
	}
	
	
	// Instead of querying files for each value, generate a map with TTD at each node number for specific sensor settings
	public static void createDetectionMap(IProgressMonitor monitor, ScenarioSet set, SensorSetting setting, String specificType) {
		
		long startTime = System.currentTimeMillis();
		
		Map<Integer, Float> baseline = new HashMap<Integer, Float>(); //stores values at the initial timestep
		Point3i structure = set.getNodeStructure().getIJKDimensions();
		for(H5File hdf5File: hdf5Files.values()) { // For every scenario
			String scenario = hdf5File.getName().replaceAll("\\.h5" , "");
			if(monitor.isCanceled()) {
				set.getDetectionMap().remove(specificType);
				return;
			}
			monitor.subTask("generating detection matrix: " + setting.getType() + " - " + scenario);
			try {
				if(!set.getDetectionMap().containsKey(specificType))
					set.getDetectionMap().put(specificType, new HashMap<String, Map<Integer, Float>>());
				if(!set.getDetectionMap().get(specificType).containsKey(scenario))
					set.getDetectionMap().get(specificType).put(scenario, new HashMap<Integer, Float>());
				hdf5File.open();
				Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
				for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
					
					// Skip these
					if(root.getMemberList().get(rootIndex).getName().contains("data") || root.getMemberList().get(rootIndex).getName().contains("statistics"))
						continue;
					
					int timeIndex = Integer.parseInt(root.getMemberList().get(rootIndex).getName().replaceAll("plot", ""));
					
					// First time step sets the baseline
					if(timeIndex == 0) {
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
						float timestep = set.getNodeStructure().getTimeAt(timeIndex);
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
				hdf5File.close();
			} catch (Exception e) {
				System.out.println("Unable to read detection values from the hdf5 files...");
				e.printStackTrace();
			}
			monitor.worked(900/hdf5Files.size());
		}
		long elapsedTime = (System.currentTimeMillis() - startTime)/1000;
		System.out.println("You just created a detection map for " + specificType + " in " + elapsedTime + " seconds! Awesome! So Fast!");
	}
	
	
	// Instead of querying files for each value, generate a map with TTD at each node number for specific sensor settings
	public static HashMap<Integer, Float> goalSeek(ScenarioSet set, String parameter, Set<Integer> inputNodes) {
		HashMap<Integer, Float> absoluteChange = new HashMap<Integer, Float>();
		//Initialize
		for(Integer node: inputNodes)
			absoluteChange.put(node, (float)0);
		Point3i structure = set.getNodeStructure().getIJKDimensions();
		for(H5File hdf5File: hdf5Files.values()) { // Only checked scenarios
			String scenario = hdf5File.getName().replaceAll("\\.h5" , "");
			if(!set.getScenarios().contains(scenario)) continue; // Skip if scenario is not being looked at
			Map<Integer, Float> baseline = new HashMap<Integer, Float>(); //stores values at the initial timestep
			Map<Integer, Float> comparison = new HashMap<Integer, Float>(); //stores values at the specified timestep
			try {
				hdf5File.open();
				Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
				for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
					
					// Skip these
					if(root.getMemberList().get(rootIndex).getName().contains("data") || root.getMemberList().get(rootIndex).getName().contains("statistics"))
						continue;
					
					// First time step sets the baseline
					else if(Integer.parseInt(root.getMemberList().get(rootIndex).getName().replaceAll("plot", "")) == 0) {
						Object group =  root.getMemberList().get(rootIndex);
						for(int groupIndex = 0; groupIndex < ((Group)group).getMemberList().size(); groupIndex++) {
							Object child = ((Group)group).getMemberList().get(groupIndex);
							if(child instanceof Dataset && ((Dataset)child).getName().equals(parameter)) {
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
					}
					
					// The time step we are comparing against
					else if(Integer.parseInt(root.getMemberList().get(rootIndex).getName().replaceAll("plot", "")) == set.getNodeStructure().getTimeSteps().size()-1) {
						Object group =  root.getMemberList().get(rootIndex);
						for(int groupIndex = 0; groupIndex < ((Group)group).getMemberList().size(); groupIndex++) {
							Object child = ((Group)group).getMemberList().get(groupIndex);
							if(child instanceof Dataset && ((Dataset)child).getName().equals(parameter)) {
								// Found the right data type
								int dataset_id = ((Dataset)child).open();
								float[] dataRead = new float[set.getNodeStructure().getTotalNodes()];
								H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
								((Dataset)child).close(dataset_id);
								for(int index=0; index<dataRead.length; index++) {
									comparison.put(Constants.getNodeNumber(structure, index), dataRead[index]);
								}
							}
						}
					}
				}
				hdf5File.close();
			} catch (Exception e) {
				System.out.println("Unable to read detection values from the hdf5 files...");
				e.printStackTrace();
			}
			
			// Calculate the absolute change since baseline, aggregated across all active scenarios
			for(Integer node: inputNodes) {
				Float aggregateChange = comparison.get(node) - baseline.get(node) + absoluteChange.get(node);
				absoluteChange.put(node, aggregateChange);
			}
		}
		return absoluteChange;
	}
	
	
	public static Boolean sensorTriggered(SensorSetting setting, Float currentValue, Float valueAtTime0) {
		Boolean triggered = false;
		if(currentValue==null) return triggered;
		
		// See if we exceeded threshold
		if(setting.getTrigger()==Trigger.ABOVE_THRESHOLD) {
			triggered = setting.getDetectionThreshold() <= currentValue;
		} else if(setting.getTrigger()==Trigger.BELOW_THRESHOLD) {
			triggered = setting.getDetectionThreshold() >= currentValue;
		} else if(setting.getTrigger()==Trigger.RELATIVE_CHANGE) {
			float change = valueAtTime0 == 0 ? 0 : ((currentValue - valueAtTime0) / valueAtTime0);
			if(setting.getDeltaType()==DeltaType.INCREASE) triggered = setting.getDetectionThreshold() <= change;
			else if(setting.getDeltaType()==DeltaType.DECREASE) triggered = setting.getDetectionThreshold() >= change;
			else if(setting.getDeltaType()==DeltaType.BOTH) triggered = setting.getDetectionThreshold() <= Math.abs(change);
		} else { //if(setting.getTrigger()==Trigger.ABSOLUTE_CHANGE)
			float change = currentValue - valueAtTime0;
			if(setting.getDeltaType() == DeltaType.INCREASE) triggered = setting.getDetectionThreshold() <= change;
			else if(setting.getDeltaType() == DeltaType.DECREASE) triggered = setting.getDetectionThreshold() >= change;
			else if(setting.getDeltaType() == DeltaType.BOTH) triggered = setting.getDetectionThreshold() <= Math.abs(change);
		}
		if(triggered==true)
			triggered = true;
		return triggered;
	}
	
	
	// E4D asks for a storage file to accompany the leakage files, we need to verify that the timesteps align
	public static Boolean checkTimeSync(IProgressMonitor monitor, String location1, String location2, int size) {
		float[] times1 = new float[size];
		float[] times2 = new float[size];
		// Read times from the storage file
		try {
			H5File storageH5  = new H5File(location1, HDF5Constants.H5F_ACC_RDONLY);
			storageH5.open();
			
			// Get the root node
			Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)storageH5.getRootNode()).getUserObject();
			// Get to the "data" group
			for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
				String name = root.getMemberList().get(rootIndex).getName();
				if(!name.startsWith("data")) continue; //We only want the data group, skip all others
				// Get to the "times" variable and read in values
				for(int groupIndex = 0; groupIndex < ((Group)root.getMemberList().get(rootIndex)).getMemberList().size(); groupIndex++) {
					Dataset dataset = (Dataset)((Group)root.getMemberList().get(rootIndex)).getMemberList().get(groupIndex);
					int dataset_id = dataset.open();
					if(!dataset.getName().equals("times")) continue; //We only want the times variable, skip all others
					times1 = (float[])dataset.read();
					H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, times1);
				}
			}
			storageH5.close();
		} catch (Exception e) {
			System.out.println("Unable to read time values from the hdf5 storage file...");
			e.printStackTrace();
		}
		// Read times from the leakage file
		try {
			H5File leakageH5  = new H5File(location1, HDF5Constants.H5F_ACC_RDONLY);
			leakageH5.open();
			
			// Get the root node
			Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)leakageH5.getRootNode()).getUserObject();
			// Get to the "data" group
			for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
				String name = root.getMemberList().get(rootIndex).getName();
				if(!name.startsWith("data")) continue; //We only want the data group, skip all others
				// Get to the "times" variable and read in values
				for(int groupIndex = 0; groupIndex < ((Group)root.getMemberList().get(rootIndex)).getMemberList().size(); groupIndex++) {
					Dataset dataset = (Dataset)((Group)root.getMemberList().get(rootIndex)).getMemberList().get(groupIndex);
					int dataset_id = dataset.open();
					if(!dataset.getName().equals("times")) continue; //We only want the times variable, skip all others
					times2 = (float[])dataset.read();
					H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, times2);
				}
			}
			leakageH5.close();
		} catch (Exception e) {
			System.out.println("Unable to read time values from the hdf5 leakage file...");
			e.printStackTrace();
		}
		// Now compare the times and return a result
		for(int i=0; i<times1.length; i++) {
			if(times1[i]!=times2[i])
				return false;
		}
		return true;
	}
	
}
