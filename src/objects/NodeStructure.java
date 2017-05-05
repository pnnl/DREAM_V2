package objects;

import hdf5Tool.HDF5Interface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import utilities.Constants;
import utilities.Point3i;
import utilities.Point3f;

/**
 * Basic structure the grid containing the data, including node- and edge-centered xyz values.
 * 
 * @author port091
 * @author rodr144
 */
public class NodeStructure {

	// private KdTree<XYZPoint> nodes; // the grid
	private String run;
	private List<Float> x;
	private List<Float> y;
	private List<Float> z;
	private List<Float> edgex;
	private List<Float> edgey;
	private List<Float> edgez;

	private List<TimeStep> timeSteps;
	private List<String> dataTypes;
	private Point3i ijkDimensions;
	
	private HashMap<Point3i, Float> porosityOfNode;

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
		
		porosityOfNode = new HashMap<Point3i, Float>();

		loadRun(run);

		setEdgeX();
		setEdgeY();
		setEdgeZ();
		
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
			HDF5Interface.fillNodeStructureFromFiles(this);
		} catch (Exception e) {
			// Something went wrong...
			e.printStackTrace();
		}		
	}
	
	private void setEdgeX() {
		List<Float> xs = this.x;
		List<Float> cellBoundsX = new ArrayList<Float>();		
		for(int x = 1; x < xs.size(); x++) {
			float half = (xs.get(x)-xs.get(x-1))/2;
			if(x == 1)
				cellBoundsX.add(new Float(xs.get(x-1)-half).floatValue());
			cellBoundsX.add(new Float(xs.get(x-1)+half).floatValue());
			if(x == xs.size()-1) 
				cellBoundsX.add(new Float(xs.get(x)+half).floatValue());
		}
		this.edgex = cellBoundsX;
	}

	private void setEdgeY() {
		List<Float> ys = this.y;
		List<Float> cellBoundsY = new ArrayList<Float>();
		for(int y = 1; y < ys.size(); y++) {
			float half = (ys.get(y)-ys.get(y-1))/2;
			if(y == 1)
				cellBoundsY.add(new Float(ys.get(y-1)-half).floatValue());
			cellBoundsY.add(new Float(ys.get(y-1)+half).floatValue());
			if(y == ys.size()-1) 
				cellBoundsY.add(new Float(ys.get(y)+half).floatValue());
		}
		this.edgey = cellBoundsY;
	}

	private void setEdgeZ() {
		List<Float> zs = this.z;	
		List<Float> cellBoundsZ = new ArrayList<Float>();
		for(int z = 1; z < zs.size(); z++) {
			float half = (Math.abs(zs.get(z))-Math.abs(zs.get(z-1)))/2;
			if(z == 1)
			//	cellBoundsZ.add(new Float(Math.abs(zs.get(z-1))-half).floatValue());
				cellBoundsZ.add(new Float(zs.get(z-1)-half).floatValue());
			//cellBoundsZ.add(new Float(Math.abs(zs.get(z-1))+half).floatValue());
			cellBoundsZ.add(new Float(zs.get(z-1)+half).floatValue());
			if(z == zs.size()-1) 
				//	cellBoundsZ.add(new Float(Math.abs(zs.get(z))+half).floatValue());
					cellBoundsZ.add(new Float(zs.get(z)+half).floatValue());
		}
		Collections.sort(cellBoundsZ);
		this.edgez = cellBoundsZ;
	}

	public Float getTimeAt(int index) {
		if(timeSteps.get(index).getTimeStep() == index)
			return timeSteps.get(index).getRealTime();

		// otherwise we have to search for it
		for(TimeStep step: timeSteps) {
			if(step.getTimeStep() == index)
				System.out.println("Index: " + index + "\tMaps to: " + step.getRealTime());
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

	public float getVolumeOfNode(Point3i location){
		Point3i nextCorner = new Point3i(location.getI()+1, location.getJ()+1, location.getK()+1);
		Point3f lowerxyz = getXYZEdgeFromIJK(location);
		Point3f upperxyz = getXYZEdgeFromIJK(nextCorner);
		float lengthx = upperxyz.getX() - lowerxyz.getX();
		float lengthy = upperxyz.getY() - lowerxyz.getY();
		float lengthz = upperxyz.getZ() - lowerxyz.getZ();
		float totalVolume = Math.abs(lengthx*lengthy*lengthz);
		float porosity = porosityOfNode.get(location);
		return totalVolume*porosity;
	}
	


	public Point3f getXYZCenterFromIJK(Point3i node) {
		
		// We have to compute the cell center
		if(ijkDimensions.getI() != x.size()) {
			System.err.println("Implement this!!! NodeStructure: 163");
			return null;
		}
		
		return new Point3f(x.get(node.getI()-1), y.get(node.getJ()-1), z.get(node.getK()-1));	
	}

	public Point3f getXYZEdgeFromIJK(Point3i node) {
		try {
			
			// We just have to return the right xyz, probably xyz at ijk-1?
			if(ijkDimensions.getI() != x.size()) {
				System.err.println("Implement this!!! NodeStructure: 151");
				return null;
			}
			// Calculate half the distance between this node and the next
			// Actual:  0______1______2______3______5
			// We have:     1      2     3      4
			
			// TODO: This assumes the problem starts at 0, 0, 0 -> some stomp files
			// start at other values and that would be indicated in the header of the plot files
			/*
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
			
			return  new Point3f(nodalValueX, nodalValueY,nodalValueZ);
			*/
			//This version uses the extrapolated edges instead.
			return new Point3f(edgex.get(node.getI()-1), edgey.get(node.getJ()-1), edgez.get(node.getK()-1));
		} catch(Exception e) {
			Constants.log(Level.SEVERE, "Node structure - node was out of bounds", node.toString()); // Maybe warning?
			return null;
		}
	}
	
	public Point3f getNodeCenteredXYZFromIJK(Point3i node) {
		return  new Point3f(x.get(node.getI()-1), y.get(node.getJ()-1), z.get(node.getK()-1));				
	}

	public int getNodeNumber(Point3i node) {
		return getNodeNumber(node.getI(), node.getJ(), node.getK());
	}

	public int getNodeNumber(Point3f point) {
		return getNodeNumber(getI(point.getX()), getJ(point.getY()), getK(point.getZ()));
	}

	public int getNodeNumber(int i, int j, int k) {
		int iMax = ijkDimensions.getI();
		int jMax = ijkDimensions.getJ();
		return (k-1) * iMax * jMax + (j-1) * iMax + i;
	}

	public Point3i getIJKFromXYZ(Point3f point) {
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

	public HashMap<Point3i, Float> getPorosityOfNode() {
		return porosityOfNode;
	}
	
	public boolean porosityOfNodeIsSet(){
		if(porosityOfNode.size() == 0) return false;
		return true;
	}

	public boolean setPorositiesFromIJKOrderedFile(File file) throws IOException {
		/*
		 *This function assumes that the porosities are listed in an order that increments i, then j, then k.
		 *It also ignores any lines that it cannot intepret as a float and continues on to the next.
		 *If we have found precisely the number of floats that we expect, the porosity is set and it returns true.
		 *Otherwise, porosity is not set and false is returned.
		 * 
		 */
		HashMap<Point3i, Float> porosity = new HashMap<Point3i, Float>();

		BufferedReader fileReader = new BufferedReader(new FileReader(file));
		String line = "";
		int nodeNumber=1;
		Point3i currentPoint = new Point3i(-1,-1,-1);
		while((line = fileReader.readLine()) != null){ 
			try{
				float f = Float.valueOf(line);
				currentPoint = getIJKFromNodeNumber(nodeNumber);
				porosity.put(currentPoint, f);
				nodeNumber++;
			} catch(NumberFormatException e){
				//Do nothing, just look for the next line I guess?
			}
		}
		fileReader.close();
		if(currentPoint.equals(this.ijkDimensions)){
			this.porosityOfNode = porosity;
			return true;
		}
		return false;
	}
	
	public void setPorositiesFromZone(int iMin, int iMax, int jMin, int jMax, int kMin, int kMax, float porosity){
		for(int i = iMin; i<=iMax; i++){
			for(int j = jMin; j<=jMax; j++){
				for(int k = kMin; k<=kMax; k++){
					porosityOfNode.put(new Point3i(i,j,k), porosity);
				}
			}
		}
	}
	
	public void writePorositiesToIJKFile(File file) throws FileNotFoundException, UnsupportedEncodingException{
		PrintWriter writer = new PrintWriter(file.getAbsolutePath(), "UTF-8");
		for(int k = 1; k<=ijkDimensions.getK(); k++){
			for(int j = 1; j<=ijkDimensions.getJ(); j++){
				for(int i=1; i<=ijkDimensions.getI(); i++){
					float value = porosityOfNode.get(new Point3i(i,j,k));
					writer.println(String.valueOf(value));
				}
			}
		}
		writer.close();
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

	public void setIJKDimensions(int i, int j, int k) {
		ijkDimensions = new Point3i(i,j,k);
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
		Point3f point = structure.getXYZEdgeFromIJK(startNode);


		System.out.println("Node: " + startNode.toString());
		System.out.println("\tFrom nodeNumber: " + structure.getIJKFromNodeNumber(1).toString());
		//System.out.println("\tFrom point: " + structure.getIJKFromXYZ(point).toString());


		System.out.println("Node Number: " + nodeNumber);
		System.out.println("\tFrom startNode: " + structure.getNodeNumber(startNode));
		System.out.println("\tFrom point: " + structure.getNodeNumber(point));
		System.out.println("Point: " + point.toString());
		System.out.println("\tFrom startNode: " + structure.getXYZEdgeFromIJK(startNode));



	}

	public String getRun() {
		return run;
	}

	public void setRun(String run) {
		this.run = run;
	}



}
