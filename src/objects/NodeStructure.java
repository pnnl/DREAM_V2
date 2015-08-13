package objects;

import hdf5Tool.HDF5Wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import utilities.Constants;
import utilities.Point3i;
import utilities.Point3d;

/**
 * Basic structure for STOMP plot file data
 * 
 * @author port091
 */
public class NodeStructure {

	// private KdTree<XYZPoint> nodes; // the grid
	private String run;
	private List<Float> x;
	private List<Float> y;
	private List<Float> z;

	private List<TimeStep> timeSteps;
	private List<String> dataTypes;
	private Point3i ijkDimensions;

	/**
	 * Loads a node structure from the database
	 * 
	 * @param run
	 */
	public NodeStructure(String run) {
		// nodes = new KdTree<XYZPoint>();

		timeSteps = new ArrayList<TimeStep>();
		dataTypes = new ArrayList<String>();
		ijkDimensions = new Point3i();

		x = new ArrayList<Float>();
		y = new ArrayList<Float>();
		z = new ArrayList<Float>();

		loadRun(run);

		Constants.log(Level.INFO, "Node structure: initialized", null);
		Constants.log(Level.CONFIG, "Node structure: configuration", this);
	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();

		builder.append("Node structure:\n");
		builder.append("\tDimensions: " + ijkDimensions.toString() + "\n");
		builder.append("\tData types: " + dataTypes.toString() + "\n");
		builder.append("\tx: " + x.toString() + "\n");
		builder.append("\ty: " + y.toString() + "\n");
		builder.append("\tz: " + z.toString() + "\n");
		builder.append("\tTime steps: " + timeSteps.toString() + "\n");

		return builder.toString();

	}

	private void loadRun(String run) {

		Constants.log(Level.INFO, "Node structure: loading run data ", run);
		this.setRun(run);
		try {
			HDF5Wrapper.fillNodeStructureFromFiles(this);
		} catch (Exception e) {
			// Something went wrong...
			e.printStackTrace();
		}		
	}

	public Float getTimeAt(int index) {
		if(timeSteps.get(index).getTimeStep() == index)
			return timeSteps.get(index).getRealTime();

		// otherwise we have to search for it
		for(TimeStep step: timeSteps) {
			if(step.getTimeStep() == index)
				return step.getRealTime();
		}

		return null; // Does not exist
	}

	public int getI(float d) {
		int iMax = ijkDimensions.getI();
		int i = 0;
		for (int x = 0; x < iMax; x++)
			if (d >= this.x.get(x))
				i++;
		return i;
	}

	public int getJ(float yLocation) {
		int jMax = ijkDimensions.getJ();
		int j = 0;
		for (int y = 0; y < jMax; y++)
			if (yLocation >= this.y.get(y))
				j++;
		return j;
	}

	public int getK(float zLocation) {
		int kMax = ijkDimensions.getK();
		int k = 0;
		for (int z = 0; z < kMax; z++)
			if (zLocation >= this.z.get(z))
				k++;
		return k;
	}


	public Point3d getXYZFromIJK(Point3i node) {
		try {
			// Calculate half the distance between this node and the next
			// Actual:  0______1______2______3______5
			// We have:     1      2     3      4
			
			boolean nodeCentered = false;
			// If its node centered, then we already know the result
			if(nodeCentered)
				return  new Point3d(x.get(node.getI()-1), y.get(node.getJ()-1), z.get(node.getK()-1));	
			
			// TODO: This assumes the problem starts at 0, 0, 0 -> some stomp files
			// start at other values and that would be indicated in the header of the plot files
			
			float nodalValueX = 0.0f;
			for(int i = 0; i < node.getI()-1; i++) {
				float currentX = x.get(i);
				float diff = currentX-nodalValueX;
				nodalValueX = currentX + diff;
			}
			float nodalValueY = 0.0f;
			for(int j = 0; j < node.getJ()-1; j++) {
				float currentY = y.get(j);
				float diff = currentY-nodalValueY;
				nodalValueY = currentY + diff;
			}
			float nodalValueZ = 0.0f;
			for(int k = 0; k < node.getK()-1; k++) {
				float currentZ = z.get(k);
				float diff = currentZ-nodalValueZ;
				nodalValueZ = currentZ + diff;
			}
	
			return  new Point3d(nodalValueX, nodalValueY,nodalValueZ);
		} catch(Exception e) {
			Constants.log(Level.SEVERE, "Node structure - node was out of bounds", node.toString()); // Maybe warning?
			return null;
		}
	}
	
	public Point3d getNodeCenteredXYZFromIJK(Point3i node) {
		return  new Point3d(x.get(node.getI()-1), y.get(node.getJ()-1), z.get(node.getK()-1));				
	}

	public int getNodeNumber(Point3i node) {
		return getNodeNumber(node.getI(), node.getJ(), node.getK());
	}

	public int getNodeNumber(Point3d point) {
		return getNodeNumber(getI(point.getX()), getJ(point.getY()), getK(point.getZ()));
	}

	public int getNodeNumber(int i, int j, int k) {
		int iMax = ijkDimensions.getI();
		int jMax = ijkDimensions.getJ();
		return (k-1) * iMax * jMax + (j-1) * iMax + i;
	}

	public Point3i getIJKFromXYZ(Point3d point) {
		return new Point3i(getI(point.getX()), getJ(point.getY()), getK(point.getZ()));
	}

	public Point3i getIJKFromNodeNumber(int nodeNumber) {

		int iMax = ijkDimensions.getI();
		int jMax = ijkDimensions.getJ();

		int nodesInLayer = iMax * jMax;
		int nodesInRow = iMax;
		int start_position = nodeNumber - 1;
		int layer = (start_position / nodesInLayer);
		int k = layer + 1;
		int leftOver = (start_position - (layer * nodesInLayer));
		int row = (leftOver / nodesInRow);
		int j = row + 1;
		int lastOne = (leftOver - (row * nodesInRow));
		int i = lastOne + 1;

		return new Point3i(i, j, k);
	}

	public List<TimeStep> getTimeSteps() {
		return timeSteps;
	}


	public void setTimeSteps(List<TimeStep> timeSteps) {
		this.timeSteps = timeSteps;
	}
	
	public int getTotalNodes() {
		return ijkDimensions.getI() * ijkDimensions.getJ() * ijkDimensions.getK();	
	}

	public int[] getNodes() {
		return new int[] { ijkDimensions.getI(), ijkDimensions.getJ(),
				ijkDimensions.getK() };
	}

	public List<Float> getX() {
		return x;
	}

	public List<Float> getY() {
		return y;
	}

	public List<Float> getZ() {
		return z;
	}

	public List<String> getDataTypes() {
		return dataTypes;
	}

	public void setDataTypes(List<String> dataTypes) {
		this.dataTypes = dataTypes;
	}

	public Point3i getIJKDimensions() {
		return ijkDimensions;
	}

	/**
	 * Returns a list of all the neighboring nodes.
	 * 
	 * @param set
	 * @return
	 */
	public List<Integer> getNeighborNodes(Point3i node) {
		List<Integer> neighborNodes = new ArrayList<Integer>();
		Point3i nodes = getIJKDimensions();
		int iMax = nodes.getI();
		int jMax = nodes.getJ();
		int kMax = nodes.getK();

		for (int iN = node.getI() - 1; iN < node.getI() + 2; iN++) {
			for (int jN = node.getJ() - 1; jN < node.getJ() + 2; jN++) {
				for (int kN = node.getK() - 1; kN < node.getK() + 2; kN++) {
					if ((iN > 0 && iN <= iMax) && (jN > 0 && jN <= jMax)
							&& (kN > 0 && kN <= kMax)) {
						if (!(iN == node.getI() && jN == node.getJ() && kN == node.getK()))
							neighborNodes.add((kN - 1) * iMax * jMax + (jN - 1) * iMax + iN);
					}

				}
			}
		}
		return neighborNodes;
	}

	public List<Integer> getNodesInWell(Well well) {
		List<Integer> nodesInWell = new ArrayList<Integer>();
		for(int k = 1; k <= ijkDimensions.getK(); k++) {
			nodesInWell.add(this.getNodeNumber(well.getI(), well.getJ(), k));
		}
		return nodesInWell;

	}

	public static void main(String args[]) {
		NodeStructure structure = new NodeStructure(Constants.RUN_TEST);

		// Test the ijk functions

		int nodeNumber = 651;
		Point3i startNode = structure.getIJKFromNodeNumber(nodeNumber);
		Point3d point = structure.getXYZFromIJK(startNode);


		System.out.println("Node: " + startNode.toString());
		System.out.println("\tFrom nodeNumber: " + structure.getIJKFromNodeNumber(1).toString());
		//System.out.println("\tFrom point: " + structure.getIJKFromXYZ(point).toString());


		System.out.println("Node Number: " + nodeNumber);
		System.out.println("\tFrom startNode: " + structure.getNodeNumber(startNode));
		System.out.println("\tFrom point: " + structure.getNodeNumber(point));
		System.out.println("Point: " + point.toString());
		System.out.println("\tFrom startNode: " + structure.getXYZFromIJK(startNode));



	}

	public String getRun() {
		return run;
	}

	public void setRun(String run) {
		this.run = run;
	}

}
