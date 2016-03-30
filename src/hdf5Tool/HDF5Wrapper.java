package hdf5Tool;

import java.util.ArrayList;
import java.util.Arrays;
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
import ncsa.hdf.object.HObject;
import ncsa.hdf.object.h5.H5File;
import utilities.Constants;
import utilities.Point3i;

public class HDF5Wrapper {

	public static float queryMinFromMemory(String dataType) {
		float min = Float.MAX_VALUE;
		for(String scenario: Constants.hdf5Data.keySet()) {
			for(float timeStep: Constants.hdf5Data.get(scenario).keySet()) {
				for(float value: Constants.hdf5Data.get(scenario).get(timeStep).get(dataType)) {
					if(value < min)
						min = value;
				}
				break; // First time step only
			}
		}
		return min;		
	}

	public static float queryMaxFromMemory(String dataType) {
		float max = -Float.MAX_VALUE;
		for(String scenario: Constants.hdf5Data.keySet()) {
			for(float timeStep: Constants.hdf5Data.get(scenario).keySet()) {
				for(float value: Constants.hdf5Data.get(scenario).get(timeStep).get(dataType)) {
					if(value > max)
						max = value;
				}
				break; // First time step only
			}
		}
		return max;		
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
			for(float timeStep: Constants.hdf5Data.get(scenario).keySet()) {

				if(monitor.isCanceled()) return nodes;
				
				float value = Constants.hdf5Data.get(scenario).get(timeStep).get(dataType)[index];
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
				
				float valueAtStartTime = Constants.hdf5Data.get(scenario).get(timeSteps.get(startTimeIndex).getRealTime()).get(dataType)[index];	

				// Always compare from 0 in this case, end is then actually beginning
				if(startTimeIndex == 0)
					continue; // Not this one...
				float valueAtTime0 = Constants.hdf5Data.get(scenario).get(timeSteps.get(0).getRealTime()).get(dataType)[index];
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

	public static float queryValueFromMemory(NodeStructure nodeStructure, String scenario, TimeStep timestep, String dataType, int index) {
		float years = timestep.getRealTime();
		return Constants.hdf5Data.get(scenario).get(years).get(dataType)[index-1];
	}

	public static float queryMinFromCloud(String dataType) {
		float min = Float.MAX_VALUE;
		for(String scenario: Constants.hdf5CloudData.keySet()) {
			for(float timeStep: Constants.hdf5CloudData.get(scenario).keySet()) {
				for(float value: Constants.hdf5CloudData.get(scenario).get(timeStep).get(dataType).values()) {
					if(value < min)
						min = value;
				}
				break; // First time step only
			}
		}
		return min;		
	}

	public static float queryMaxFromCloud(String dataType) {
		float max = -Float.MAX_VALUE;
		for(String scenario: Constants.hdf5CloudData.keySet()) {
			for(float timeStep: Constants.hdf5CloudData.get(scenario).keySet()) {
				for(float value: Constants.hdf5CloudData.get(scenario).get(timeStep).get(dataType).values()) {
					if(value > max)
						max = value;
				}
				break; // First time step only
			}
		}
		return max;		
	}

	public static Float queryValueFromCloud(NodeStructure nodeStructure, String scenario, TimeStep timestep, String dataType, int index) throws Exception {
		float years = timestep.getRealTime();
		if(
				Constants.hdf5CloudData.containsKey(scenario) && 
				Constants.hdf5CloudData.get(scenario).containsKey(years) &&
				Constants.hdf5CloudData.get(scenario).get(years).containsKey(dataType) && 
				Constants.hdf5CloudData.get(scenario).get(years).get(dataType).containsKey(index)) {
			return Constants.hdf5CloudData.get(scenario).get(years).get(dataType).get(index);
		}
		return null;
	}

	public static void fillNodeStructureFromFiles(NodeStructure nodeStructure) throws Exception {
		// We assume all the files have the same node structure over all scenarios
		H5File hdf5File = Constants.hdf5Files.get(Constants.hdf5Files.keySet().toArray()[0]);
		hdf5File.open();
		Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
		for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) { // Search for the data node (should be first)
			if(root.getMemberList().get(rootIndex).getName().equals("data")) {	// We can get the node structure data from this one
				float[] steps, times, xValues, yValues, zValues;
				steps = times = xValues = yValues = zValues = null;				
				for(int groupIndex = 0; groupIndex < ((Group)root.getMemberList().get(rootIndex)).getMemberList().size(); groupIndex++) {
					Dataset dataset = (Dataset)((Group)root.getMemberList().get(rootIndex)).getMemberList().get(groupIndex);
					int dataset_id = dataset.open();
					//		if(dataset.getName().equals("steps")) {	steps =  (Float[])dataset.read(); }		 // TODO: Do we need steps?			
					if(dataset.getName().equals("times")) {	times =  (float[])dataset.read(); }					
					if(dataset.getName().equals("x")) {	xValues =  (float[])dataset.read(); }					
					if(dataset.getName().equals("y")) {	yValues =  (float[])dataset.read(); }					
					if(dataset.getName().equals("z")) {	zValues =  (float[])dataset.read(); }		
					dataset.close(dataset_id);
				}
				// Should have all the info we need now
				nodeStructure.getIJKDimensions().setI(xValues.length);
				nodeStructure.getIJKDimensions().setJ(yValues.length);
				nodeStructure.getIJKDimensions().setK(zValues.length);
				for(float x: xValues) nodeStructure.getX().add(x);
				for(float y: yValues) nodeStructure.getY().add(y);
				for(float z: zValues) nodeStructure.getZ().add(z);
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
		for(String key: Constants.hdf5Files.keySet()) {
			scenarios.add(key);
		}
		return scenarios;
	}

	public static float queryMinFromFiles(NodeStructure nodeStructure, String dataType) throws Exception {
		float min = Float.MAX_VALUE;
		for(H5File hdf5File: Constants.hdf5Files.values()) { // For every scenario
			int hdf5FileID = hdf5File.open();
			Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
			List<HObject> groups = root.getMemberList();
			for(HObject group: groups) {
				List<HObject> children = ((Group)group).getMemberList();
				boolean hasDataset = false;
				for(Object child: children) {
					if(child instanceof Dataset && ((Dataset)child).getName().equals(dataType)) { // If this is the data type we're interested in
						hasDataset = true;
						int dataset_id = ((Dataset)child).open();
						float[] dataRead = new float[nodeStructure.getTotalNodes()];
						H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
						((Dataset)child).close(dataset_id);
						if(min == 0)
							break;
						for(float value: dataRead) {
							if(value < min)
								min = value;
						}								
					}
					System.gc();
				}				
				if(hasDataset || min == 0) // Can't do any better than this...
					break;  // First dataset only
			}
			hdf5File.close();
		}
		System.gc();
		return min;	
	}

	public static float queryMaxFromFiles(NodeStructure nodeStructure, String dataType) throws Exception {
		float max = -Float.MAX_VALUE;
		for(H5File hdf5File: Constants.hdf5Files.values()) { // For every scenario
			int hdf5FileID = hdf5File.open();
			Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
			for(Object group: root.getMemberList()) {
				boolean hasDataset = false;
				for(Object child: ((Group)group).getMemberList()) {
					if(child instanceof Dataset && ((Dataset)child).getName().equals(dataType)) { // If this is the data type we're interested in
						hasDataset = true;
						int dataset_id = ((Dataset)child).open();
						float[] dataRead = new float[nodeStructure.getTotalNodes()];
						H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);	
						((Dataset)child).close(dataset_id);
						for(float value: dataRead) {
							if(value > max)
								max = value;
						}								
					}
					System.gc();
				}
				if(hasDataset)
					break; // First time step only
			}
			hdf5File.close();
		}
		return max;	
	}

	public static HashSet<Integer> queryNodesFromFiles(NodeStructure nodeStructure, String scenario, 
			String dataType, float lowerThreshold, float upperThreshold, IProgressMonitor monitor) throws Exception {
		HashSet<Integer> nodes = new HashSet<Integer>();		
		H5File hdf5File = Constants.hdf5Files.get(scenario); // For the given scenario
		hdf5File.open();
		Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
		TreeMap<Float, Group> sortedTimes = new TreeMap<Float, Group>();
		boolean plotAreTimeIndices = plotFileHack(nodeStructure, root);
	
		for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) { // For every time step
			Object group =  root.getMemberList().get(rootIndex);
			String name = ((Group)group).getName().replaceAll("plot", "");
			if(name.contains("data"))
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

						if(monitor.isCanceled()) return nodes;

						
						if(dataRead[i] >= lowerThreshold && dataRead[i] < upperThreshold) { // Or >=
							// Also add to the cloud?

							
							int nodeNumber = Constants.getNodeNumber(nodeStructure.getIJKDimensions(), i);
							if(nodeNumber == 1521 || nodeNumber == 1597)	System.out.println("Found value: " + scenario + "\t" + nodeNumber + "\t" + timeStep + "\t" 
									+ lowerThreshold + "\t" + upperThreshold + "\t" + dataRead[i]);
							if(!nodes.contains(nodeNumber)) {
								if(nodeNumber == 1521 || nodeNumber == 1597) System.out.println("Detected at " + timeStep);
								addNodeToCloud(scenario, timeStep, dataType, nodeNumber, dataRead[i]);
								nodes.add(nodeNumber);
							}
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
		if(Constants.hdf5CloudData == null)
			Constants.hdf5CloudData = new HashMap<String, Map<Float, Map<String, Map<Integer, Float>>>>();
		if(!Constants.hdf5CloudData.containsKey(scenario))
			Constants.hdf5CloudData.put(scenario, new HashMap<Float, Map<String, Map<Integer, Float>>>());
		if(!Constants.hdf5CloudData.get(scenario).containsKey(timeInYears))
			Constants.hdf5CloudData.get(scenario).put(timeInYears, new HashMap<String, Map<Integer, Float>>());
		if(!Constants.hdf5CloudData.get(scenario).get(timeInYears).containsKey(dataType))
			Constants.hdf5CloudData.get(scenario).get(timeInYears).put(dataType, new HashMap<Integer, Float>());
		Constants.hdf5CloudData.get(scenario).get(timeInYears).get(dataType).put(nodeNumber, value);	
		} catch (Exception e) {
			float totalNodes = 0;
			for(String sc : Constants.hdf5CloudData.keySet()) {
				for(float ts: Constants.hdf5CloudData.get(sc).keySet()) {
					for(String dt: Constants.hdf5CloudData.get(sc).get(ts).keySet()) {
						totalNodes = Constants.hdf5CloudData.get(sc).get(ts).get(dt).keySet().size();
						if(Constants.hdf5CloudData.get(sc).get(ts).get(dt).keySet().size() > 1000)
							System.out.println(sc + ", " + ts + ", " + dt + " , " + Constants.hdf5CloudData.get(sc).get(ts).get(dt).keySet().size());
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

		H5File h5file = Constants.hdf5Files.get(scenario);
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
				if(monitor.isCanceled()) return nodes;
				
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
				// Catherine, edit here!!!!
				float change = trigger == Trigger.RELATIVE_DELTA ? 
						// This is the calculation for the percentage (checked)
						valueAtTime0 == 0 ? 0 : ((valueAtCurrentTime - valueAtTime0) / valueAtTime0) :
							// This is the calculation for non percentage (not checked)
							valueAtCurrentTime - valueAtTime0;
						// Max change is what the user entered
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

	public static float queryValueFromFile(NodeStructure nodeStructure, String scenario, TimeStep timestep, String dataType, int nodeNumber) throws Exception {
		H5File hdf5File = Constants.hdf5Files.get(scenario); // Get the correct file for the scenario
		hdf5File.open();
		Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
		boolean plotsAreTimeIndices = plotFileHack(nodeStructure, root);
		for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) {
			// Found the right time step
			if(root.getMemberList().get(rootIndex).getName().contains("data"))
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
	
	
	public static boolean plotFileHack(NodeStructure nodeStructure, Group root) {
		// plotxyz is inconsitent between h5 files, it will either be plot[timeIndex] or plot[realTime]
		// For backwards compatibility, we're putting in a hack here to handle both cases... it will probably work...
		// most of the time...
		List<Integer> realTimeOrTimeIndex = new ArrayList<Integer>();
		for(int rootIndex = 0; rootIndex < root.getMemberList().size(); rootIndex++) { // For every time step
			Object group =  root.getMemberList().get(rootIndex);
			String name = ((Group)group).getName().replaceAll("plot", "");
			if(name.contains("data"))
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
	public static Integer getNodeNumber(Point3i max, int index) {
		return getNodeNumber(max.getI(), max.getJ(), max.getK(), index);
	}

	public static Integer getNodeNumber(int iMax, int jMax, int kMax, int index) {
		if(indexToNodeId.containsKey(index))
			return indexToNodeId.get(index);
		int kLoops = index;
		int jLoops = index/kMax;
		int iLoops = index/(kMax*jMax);		
		int nodeId = (kLoops % kMax) * iMax * jMax + (jLoops % jMax) * iMax + (iLoops % iMax + 1);
		indexToNodeId.put(index, nodeId);
		return nodeId;
	}

	public static Integer getIndex(Point3i max, int nodeNumber) {
		return getIndex(max.getI(), max.getJ(), max.getK(), nodeNumber);
	}

	public static Integer getIndex(int iMax, int jMax, int kMax, int nodeNumber) {
		if(nodeIdToIndex.containsKey(nodeNumber))
			return nodeIdToIndex.get(nodeNumber);
		int counter = 0;
		for(int i = 1; i <= iMax; i++) {
			for(int j = 1; j <= jMax; j++) {
				for(int k = 1; k <= kMax; k++) {
					int myNodeNumber= (k-1) * iMax * jMax + (j-1) * iMax + i;
					if(myNodeNumber == nodeNumber) {
						indexToNodeId.put(nodeNumber, counter);
						return counter;
					}
					counter++;
				}
			}	
		}
		return 0;
	}
	 */
}
