package gridviz;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

/**
 * @brief  Facilitates traversal of a grid of data
 * @author Tucker Beck, E.Porter
 * @date   3/7/12, 4/1/13
 *
 *         This class is used to traverse non-uniform rectangular grid in which
 *         each node is aligned with neighbors along the x, y, and z axis.
 *         Each node contains values for multiple data fields.  The grid may
 *         be sub-sampled into a uniform regular grid or sliced along one of the
 *         major axes (x, y, z) at an arbitrary point.  Sub-sampling is
 *         performed using trilinear interpolation of the 8 nearest neighbors in
 *         the grid.
 */
public class DataGrid {

	/// Provides conversions between grid indices and linear indices
	private Gridder gridder;

	/// Contains the data for the nodes hashed by the field names
	private HashMap<String, FieldValues> fieldValueMap;
	
	/// Contains the data for the nodes hashed by the field names
	private HashMap<String, String> fieldUnitMap;

	// Contains a list of inactive nodes
	// private List<Integer> inactiveNodes; 

	/// The step positions along the x axis
	private float[] xSteps;

	/// The step positions along the y axis
	private float[] ySteps;

	/// The step positions along the z axis
	private float[] zSteps;

	// Can be 2d
	private boolean is2D;

	// Direction of the normal, either x, y, or z
	public enum AXIS {X, Y, Z};
	private AXIS normal;
	
	private float timeStep;

	private Vector3f origin;

	/**
	 * Constructs this DataGrid.
	 * @param size The size in 3 dimensions of the grid
	 */
	public DataGrid(Point3i size) {
		fieldValueMap = new HashMap<String, FieldValues>();
		fieldUnitMap = new HashMap<String, String>();
		gridder = new Gridder(size);
		setAxis(size);
	}

	public DataGrid(Point3i size, Vector3f origin) {		
		fieldValueMap = new HashMap<String, FieldValues>();
		fieldUnitMap = new HashMap<String, String>();
		this.origin = new Vector3f(origin);
		gridder = new Gridder(size);
		setAxis(size);
	}

	private void setAxis(Point3i size) {
		if(size.x == 1) {
			is2D = true;
			normal = AXIS.X;
		} else if(size.y == 1) {
			is2D = true;
			normal = AXIS.Y;
		} else if(size.z == 1) {
			is2D = true;
			normal = AXIS.Z;
		} else {
			is2D = false;
		}
	}

	public void setTimestep(float timestep) {
		this.timeStep = timestep; 
	}
	
	public float getTimestep() {
		return timeStep;
	}
	
	public boolean is2D() {
		return is2D;
	}

	public AXIS getNormalAxis() {
		return this.normal;
	}

	public String getNormalDirection(){
		if(normal != null) {
			if(normal.equals(AXIS.X))
				return "x";
			if(normal.equals(AXIS.Y))
				return "y";
			if(normal.equals(AXIS.Z))
				return "z";
		}
		return null;
	}

	public boolean hasOrigin() {
		return origin != null;
	}

	/**
	 * Gets the set of values for a particular field
	 * @param fieldKey		The name of the field for which to fetch values
	 * @return
	 */
	public FieldValues getFieldValues(String fieldKey) {
		// If there isn't a matching field, add it
		if(!fieldValueMap.containsKey(fieldKey)) {
			if(fieldKey=="x")
				fieldValueMap.put(fieldKey, new FieldValues(gridder.getSize().x));
			else if(fieldKey=="y")
				fieldValueMap.put(fieldKey, new FieldValues(gridder.getSize().y));
			else if(fieldKey=="z")
				fieldValueMap.put(fieldKey, new FieldValues(gridder.getSize().z));
			else
				fieldValueMap.put(fieldKey, new FieldValues(gridder.getL()));
		}
		return fieldValueMap.get(fieldKey);
	}
	
	public void setFieldUnit(String fieldKey, String units) {
		if(units.equals("null"))
			units = "";
		fieldUnitMap.put(fieldKey, units);
	}
	
	public String getFieldUnit(String fieldKey) {
		return fieldUnitMap.get(fieldKey.trim());
	}
	
	
	/**
	 * Fetches two points representing the extents of the grid
	 * @param lo The point at the low extreme of all axes
	 * @param hi The point at the high extreme of all axes
	 */
	public void getExtents(Vector3f lo, Vector3f hi ) {

		float[] xVals = getXAxisSteps();
		float[] yVals = getYAxisSteps();
		float[] zVals = getZAxisSteps();

		lo.set(xVals[0], yVals[0], zVals[0]);
		hi.set(xVals[xVals.length-1], yVals[yVals.length-1], zVals[zVals.length-1]);

	}


	public Point3i getSize() {
		return gridder.getSize();
	}


	public Gridder getGridder() {
		return gridder;
	}


	/**
	 * Fetches the names of all the data fields in this grid
	 * @return
	 */
	public Set<String> getFieldNames() {

		Set<String> fieldNames = new TreeSet<String>(fieldValueMap.keySet());

		// Remove the names of the position fields
		fieldNames.remove("x");
		fieldNames.remove("y");
		fieldNames.remove("z");

		return fieldNames;
	}


	/**
	 * Dumps one of the data fields to a binary file
	 * @param fileName		The name of the binary file in which to dump
	 * @param fieldKey		The field to dump to binary file
	 * @throws GridError
	 */
	public void dumpData(String fileName, String fieldKey) throws GridError {
		try {
			DataOutputStream byteFile = new DataOutputStream(new FileOutputStream(fileName));

			float[] values = this.fieldValueMap.get(fieldKey).getValues();

			for(int i=0; i<gridder.getL(); i++) {
				float value = values[i];
				byteFile.writeFloat(value);
			}
			byteFile.close();

		} catch (FileNotFoundException ex) {
			throw new GridError("Error creating file: " + ex.getMessage());
		} catch (IOException ex) {
			throw new GridError("Error writing to file: " + ex.getMessage());
		}
	}

	/**
	 * Dumps one of the data fields to a ascii file
	 * @param fileName		The name of the ascii file in which to dump
	 * @param fieldKey		The field to dump to binary file
	 * @throws GridError
	 */
	public void dumpDataAscii(String fileName, String fieldKey) throws GridError {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

			float[] values = this.fieldValueMap.get(fieldKey).getValues();
			StringBuffer file = new StringBuffer();

			for(int i=0; i<gridder.getL(); i++) {
				float value = values[i];
				file.append(String.valueOf(value) + "\n");
			}
			writer.write(file.toString());
			writer.close();
		} catch (FileNotFoundException ex) {
			throw new GridError("Error creating file: " + ex.getMessage());
		} catch (IOException ex) {
			throw new GridError("Error writing to file: " + ex.getMessage());
		}
	}

	/**
	 * 
	 * @param xAxis AXIS axis to be displayed on the images x axis
	 * @param yAxis AXIS axis to be displayed on the images y axis
	 * @param intersection float point of intersection
	 * @param min vector3d containing the min values for the slice
	 * @param max vector3d containing the max values for the slice
	 * @param fieldKey String the data to be displayed
	 * @param sideMax int max pixels on any one side
	 * @return
	 * @throws GridError
	 */
	public DataSlice slice(AXIS xAxis, AXIS yAxis, float intersection, 
			Vector3f min, Vector3f max, String fieldKey, int sideMax) throws GridError {

		// Direction of slice =
		AXIS sliceAxis = AXIS.X; // Default, will be set -
		for(AXIS axis: AXIS.values()) {
			if(!axis.equals(xAxis) && !axis.equals(yAxis)) {
				sliceAxis = axis;
				break;
			}    		
		}

		Vector3f deltas = new Vector3f(max.x - min.x, max.y - min.y, max.z - min.z);     

		// Make sure we have data in the directions of the final image
		for(AXIS axis: new AXIS[]{xAxis, yAxis}) {
			if(Float.compare(getValue(deltas, axis), 0) == 0) {
				throw new GridError("Unable to generate slice: requested axis is dimensionless");
			}
		}

		// Make sure all our values are in bounds?
		Vector3f globalLo = new Vector3f();
		Vector3f globalHi = new Vector3f();
		getExtents(globalLo, globalHi);

		for(Vector3f vector: new Vector3f[]{min, max}) {
			for(AXIS axis: AXIS.values()) {
				float value = getValue(vector, axis);
				float rangeMin = getValue(globalLo, axis);
				float rangeMax = getValue(globalHi, axis);
				if(value < rangeMin || value > rangeMax) {
					throw new GridError("Invalid slice position "+ value + " must be in range [" +rangeMin + ", " + rangeMax + "]");
				}
			}
		}

		// Check the intersection
		float rangeMin = getValue(globalLo, sliceAxis);
		float rangeMax = getValue(globalHi, sliceAxis);
		if(intersection < rangeMin || intersection > rangeMax) {
			throw new GridError("Invalid intersection "+ intersection +" in "+ sliceAxis + " for " + xAxis + "" + yAxis + " slice.  Intersection must be in range [" +rangeMin + ", " + rangeMax + "]");
		}

		// Extents look good, lets calculate the size of the strides
		float step;
		int h;
		int w;

		// We will put the grids x axis along our slices x axis
		float distanceAlongX = getValue(deltas, xAxis);
		float distanceAlongY = getValue(deltas, yAxis);

		// Which ever one is larger will make our step size
		if(distanceAlongX > distanceAlongY)
			step = distanceAlongX/sideMax;
		else
			step = distanceAlongY/sideMax;

		// Calculate the height and width of the image (this is in pixels)
		w = (int) (distanceAlongX/step);
		h = (int) (distanceAlongY/step);
		

		List<Integer> xGridLines = new ArrayList<Integer>();
		List<Integer> yGridLines = new ArrayList<Integer>();
		List<Integer> zGridLines = new ArrayList<Integer>();

		for(float xValue: getXAxisSteps()) {
			xGridLines.add((int)Math.round((xValue-getValue(min, AXIS.X))/step));
		}
		for(float yValue: getYAxisSteps()) {
			yGridLines.add((int)Math.round((yValue-getValue(min, AXIS.Y))/step));
		}
		for(float zValue: getZAxisSteps()) {
			zGridLines.add((int)Math.round((zValue-getValue(min, AXIS.Z))/step));
		}

		// Gather the variables
		FieldValues valueField = fieldValueMap.get(fieldKey);  
		float values[] = valueField.getValues();

		// Create the new slice
		DataSlice slice = new DataSlice(w, h, min, max, xAxis, yAxis, sliceAxis, step, intersection, xGridLines, yGridLines, zGridLines, valueField.getExtrema(), fieldKey);

		// Use the pos variable to step through the new grid's space
		Vector3f pos = new Vector3f();
		setValue(pos, sliceAxis, intersection);
		// Step over the y axis both with indices and the position
		setValue(pos, yAxis, getValue(min, yAxis));
		for (int i=h-1; i>=0; i--, setValue(pos,yAxis, getValue(pos, yAxis)+step)) {
			setValue(pos,xAxis,getValue(max, xAxis));
			try {
				for (int j=w-1; j>=0; j--, setValue(pos, xAxis, getValue(pos, xAxis)-step)) {
					if(is2D)
						slice.setValue(i, j, interpolateValue2D(pos, values));
					else
						slice.setValue(i, j, interpolateValue3D(pos, values));
				} 
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new GridError(e.getMessage());
			}
		}
		return slice;

	}    
	
	/**
	 * 
	 * @param sideMax
	 * @param slice
	 * @return
	 * @throws GridError
	 */
	public DataSlice slice(int sideMax, DataSlice slice) throws GridError {

		AXIS xAxis = slice.getXAxis();
		AXIS yAxis = slice.getYAxis();
		Vector3f min = slice.getMin();
		Vector3f max = slice.getMax();
		float intersection = slice.getIntersection();
		String fieldKey = slice.getFieldKey();
		
		DataSlice newSlice =  slice(xAxis, yAxis, intersection, min, max, fieldKey, sideMax);
		newSlice.setRenderAxis(slice.getRenderAxis());
		newSlice.setRenderMesh(slice.getRenderMesh());
		newSlice.setRenderTickMarks(slice.getRenderTickMarks());
		newSlice.setAnnotationOption(slice.getAnnotationOption());
		newSlice.setAnnotations(slice.getAnnotations());
		return newSlice;
		
	}    

	/**
	 * Interpolates a value for an arbitrary point based on its neighbors
	 * @param position	The point for which to interpolate	
	 * @param values	The set of all values for the given field
	 * @return
	 */
	private float interpolateValue2D(Vector3f position,float values[]) {

		Point3i ijk;
		try {
			/// First fetch the 4 neighbors of our target point
			ijk = getIJKFromPosition2D(position);
		} catch(Exception err) {
			System.out.println("Couldn't fetch neighbors:" + err.getMessage());
			return 0.0f;
		}

		int i = ijk.getX();
		int j = ijk.getY();
		int k = ijk.getZ();

		// If we are at a far edge, this will have the same value as the previous node.
		int iPlusOne = i+1 == gridder.getSize().getX() ? i : i+1;
		int jPlusOne = j+1 == gridder.getSize().getY() ? j : j+1;
		int kPlusOne = k+1 == gridder.getSize().getZ() ? k : k+1;

		int ns[][] = new int[2][2];
		if(normal.equals(AXIS.X)) {
			ns[0][0] = gridder.getLinearIndex(  i,   j,   k);
			ns[0][1] = gridder.getLinearIndex(  i, jPlusOne,   k);
			ns[1][0] = gridder.getLinearIndex(  i,   j, kPlusOne);
			ns[1][1] = gridder.getLinearIndex(  i, jPlusOne, kPlusOne);
		} else if(normal.equals(AXIS.Y)) {
			ns[0][0] = gridder.getLinearIndex(  i,   j,   k);
			ns[0][1] = gridder.getLinearIndex(iPlusOne,   j,   k);
			ns[1][0] = gridder.getLinearIndex(  i,   j, kPlusOne);
			ns[1][1] = gridder.getLinearIndex(iPlusOne,   j, kPlusOne);
		} else if(normal.equals(AXIS.Z)) {
			ns[0][0] = gridder.getLinearIndex(  i,   j,   k);
			ns[0][1] = gridder.getLinearIndex(iPlusOne,   j,   k);
			ns[1][0] = gridder.getLinearIndex(  i, jPlusOne,   k);
			ns[1][1] = gridder.getLinearIndex(iPlusOne, jPlusOne,   k);
		}

		/* Perform tri-linear interpolation
		 * Refer for details to:
		 * http://en.wikipedia.org/wiki/Trilinear_interpolation
		 */
		float pos, pos0, pos1, val0, val1;
		float epsilon = 1e-10f;

		pos  = (!normal.equals(AXIS.X)) ? position.x : position.y;
		pos0 = (!normal.equals(AXIS.X)) ? xSteps[i] : ySteps[j];
		pos1 = (!normal.equals(AXIS.X)) ? xSteps[i+1] : ySteps[j+1]; 
		val0 = values[ns[0][0]];
		val1 = values[ns[0][1]];
		float i4 = interpolate(pos, pos0, pos1, val0, val1);
		if ((i4 + epsilon < val0 && i4 + epsilon < val1) ||
				(i4 - epsilon > val0 && i4 - epsilon > val1)) {
			System.out.format("Bad interpolation:" +
					"i4 with %g  from %f between (%g,%g) with values (%g,%g)\n",
					i4, pos, pos0, pos1, val0, val1);
		}

		val0 = values[ns[1][0]];
		val1 = values[ns[1][1]];
		float i5 = interpolate(pos, pos0, pos1, val0, val1);
		if ((i5 + epsilon < val0 && i5 + epsilon < val1) ||
				(i5 - epsilon > val0 && i5 - epsilon > val1)) {
			System.out.format("Bad interpolation:" +
					"i5 with %g  from %f between (%g,%g) with values (%g,%g)\n",
					i5, pos, pos0, pos1, val0, val1);
		}

		pos  = (!normal.equals(AXIS.Y)) ? position.y : position.z;
		pos0 = (!normal.equals(AXIS.Y)) ? ySteps[j] : zSteps[k];
		pos1 = (!normal.equals(AXIS.Y)) ? ySteps[j+1] :  zSteps[k+1];
		val0 = i4;
		val1 = i5;
		float i6 = interpolate(pos, pos0, pos1, val0, val1);
		if ((i6 + epsilon < val0 && i6 + epsilon < val1) ||
				(i6 - epsilon > val0 && i6 - epsilon > val1)) {
			System.out.format("Bad interpolation:" +
					"i6 with %g  from %f between (%g,%g) with values (%g,%g)\n",
					i6, pos, pos0, pos1, val0, val1);
		}

		if (
				( 
						i6 < values[ns[0][0]] &&
						i6 < values[ns[0][1]] &&
						i6 < values[ns[1][0]] &&
						i6 < values[ns[1][1]]

						) || (

								i6 > values[ns[0][0]] &&
								i6 > values[ns[0][1]] &&
								i6 > values[ns[1][0]] &&
								i6 > values[ns[1][1]] 
								)
				) {
			throw new RuntimeException("Bad interpolated value at position " + position.toString());
		}

		return i6;
	}

	/**
	 * Interpolates a value for an arbitrary point based on its neighbors
	 * @param position 	The point for which to interpolate
	 * @param values	The set of all values for the given field	
	 * @return
	 */
	private float interpolateValue3D(Vector3f position, float values[]) {

		Point3i ijk;

		try {
			ijk = getIJKFromPosition3D(position);/// First fetch the 8 neighbors of our target point
		} catch(Exception err) {
			System.out.println("Couldn't fetch neighbors: " + err.getMessage());
			return 0.0f;
		}

		int i = ijk.getX();
		int j = ijk.getY();
		int k = ijk.getZ();

		// If we are at a far edge, this will have the same value as the previous node.
		int iPlusOne = i+1 == gridder.getSize().getX() ? i : i+1;
		int jPlusOne = j+1 == gridder.getSize().getY() ? j : j+1;
		int kPlusOne = k+1 == gridder.getSize().getZ() ? k : k+1;
		
		int ns[][][] = new int[2][2][2];
		ns[0][0][0] = gridder.getLinearIndex(  i,   	j,   		k);
		ns[1][0][0] = gridder.getLinearIndex(iPlusOne,	j,   		k);
		ns[0][1][0] = gridder.getLinearIndex(  i, 		jPlusOne,   k);
		ns[1][1][0] = gridder.getLinearIndex(iPlusOne, 	jPlusOne,   k);
		ns[0][0][1] = gridder.getLinearIndex(  i,   	j, 			kPlusOne);
		ns[1][0][1] = gridder.getLinearIndex(iPlusOne,	j, 			kPlusOne);
		ns[0][1][1] = gridder.getLinearIndex(  i, 		jPlusOne, 	kPlusOne);
		ns[1][1][1] = gridder.getLinearIndex(iPlusOne,	jPlusOne,	kPlusOne);

		/*
		 * Perform tri-linear interpolation
		 * Refer for details to:
		 * http://en.wikipedia.org/wiki/Trilinear_interpolation
		 */
		float pos, pos0, pos1, val0, val1;
		float epsilon = 1e-10f;

		pos  = position.x;
		pos0 = xSteps[i];
		pos1 = xSteps[i+1];

		val0 = values[ns[0][0][0]];
		val1 = values[ns[1][0][0]];
		float i0 = interpolate(pos, pos0, pos1, val0, val1);
		if ((i0 + epsilon < val0 && i0 + epsilon < val1) ||
				(i0 - epsilon > val0 && i0 - epsilon > val1)) {
			System.out.format("Bad interpolation:" +
					"i0 with %g  from %f between (%g,%g) with values (%g,%g)\n",
					i0, pos, pos0, pos1, val0, val1);
		}

		val0 = values[ns[0][1][0]];
		val1 = values[ns[1][1][0]];
		float i1 = interpolate(pos, pos0, pos1, val0, val1);
		if ((i1 + epsilon < val0 && i1 + epsilon < val1) ||
				(i1 - epsilon > val0 && i1 - epsilon > val1)) {
			System.out.format("Bad interpolation:" +
					"i1 with %g  from %f between (%g,%g) with values (%g,%g)\n",
					i1, pos, pos0, pos1, val0, val1);
		}

		val0 = values[ns[0][0][1]];
		val1 = values[ns[1][0][1]];
		float i2 = interpolate(pos, pos0, pos1, val0, val1);
		if ((i2 + epsilon < val0 && i2 + epsilon < val1) ||
				(i2 - epsilon > val0 && i2 - epsilon > val1)) {
			System.out.format("Bad interpolation:" +
					"i2 with %g  from %f between (%g,%g) with values (%g,%g)\n",
					i2, pos, pos0, pos1, val0, val1);
		}

		val0 = values[ns[0][1][1]];
		val1 = values[ns[1][1][1]];
		float i3 = interpolate(pos, pos0, pos1, val0, val1);
		if ((i3 + epsilon < val0 && i3 + epsilon < val1) ||
				(i3 - epsilon > val0 && i3 - epsilon > val1)) {
			System.out.format("Bad interpolation:" +
					"i3 with %g  from %f between (%g,%g) with values (%g,%g)\n",
					i3, pos, pos0, pos1, val0, val1);
		}

		pos  = position.y;
		pos0 = ySteps[j];
		pos1 = ySteps[j+1];

		val0 = i0;
		val1 = i1;
		float i4 = interpolate(pos, pos0, pos1, val0, val1);
		if ((i4 + epsilon < val0 && i4 + epsilon < val1) ||
				(i4 - epsilon > val0 && i4 - epsilon > val1)) {
			System.out.format("Bad interpolation:" +
					"i4 with %g  from %f between (%g,%g) with values (%g,%g)\n",
					i4, pos, pos0, pos1, val0, val1);
		}
		val0 = i2;
		val1 = i3;
		float i5 = interpolate(pos, pos0, pos1, val0, val1);
		if ((i5 + epsilon < val0 && i5 + epsilon < val1) ||
				(i5 - epsilon > val0 && i5 - epsilon > val1)) {
			System.out.format("Bad interpolation:" +
					"i5 with %g  from %f between (%g,%g) with values (%g,%g)\n",
					i5, pos, pos0, pos1, val0, val1);
		}

		pos  = position.z;
		pos0 = zSteps[k];
		pos1 = zSteps[k+1];
		val0 = i4;
		val1 = i5;
		float i6 = interpolate(pos, pos0, pos1, val0, val1);
		if ((i6 + epsilon < val0 && i6 + epsilon < val1) ||
				(i6 - epsilon > val0 && i6 - epsilon > val1)) {
			System.out.format("Bad interpolation:" +
					"i6 with %g  from %f between (%g,%g) with values (%g,%g)\n",
					i6, pos, pos0, pos1, val0, val1);
		}

		if (
				( 
						i6 < values[ns[0][0][0]] &&
						i6 < values[ns[0][0][1]] &&
						i6 < values[ns[0][1][0]] &&
						i6 < values[ns[0][1][1]] &&
						i6 < values[ns[1][0][0]] &&
						i6 < values[ns[1][0][1]] &&
						i6 < values[ns[1][1][0]] &&
						i6 < values[ns[1][1][1]]
						) || (

								i6 > values[ns[0][0][0]] &&
								i6 > values[ns[0][0][1]] &&
								i6 > values[ns[0][1][0]] &&
								i6 > values[ns[0][1][1]] &&
								i6 > values[ns[1][0][0]] &&
								i6 > values[ns[1][0][1]] &&
								i6 > values[ns[1][1][0]] &&
								i6 > values[ns[1][1][1]]
								)
				) {

			for(int x = 0; x < ns.length; x++) {
				for(int y = 0; y < ns[x].length; y++) {
					for(int z = 0; z < ns[x][y].length; z++) {
						System.out.println("At point (" + x + ", " + y + ", " + z + ")\tindex = " +ns[x][y][z] +"\tvalue = " + values[ns[x][y][z]]);
					}
				}
			}
			System.out.println("value = " + i6);
			throw new RuntimeException("Bad interpolated value at point " + position.toString());
		}
		return i6;
	}

	/**
	 * Performs a linear interpolation
	 * @param pos		The position for which to interpolate a value
	 * @param pos0		The first position with a known value
	 * @param pos1		The second position with a known value
	 * @param val0		The value at the first position
	 * @param val1		The value at the second position
	 * @return
	 */
	private float interpolate(float pos, float pos0, float pos1, float val0, float val1){
		if (pos == pos0) { 
			return val0;
		} else if (pos == pos1) {
			return val1;
		} else {
			return val0 + ((val1 - val0) * (pos - pos0) / (pos1 - pos0));
		}
	}

	/**
	 * Fetches the ijk location for a given 2d xyz position
	 * @param position		The point in 3 space for which to search
	 * @return
	 * @throws GridError
	 */
	public Point3i getIJKFromPosition2D(Vector3f position) throws GridError {

		getXAxisSteps();
		getYAxisSteps();
		getZAxisSteps();

		// Find the x index
		int i = 0;
		if(!normal.equals(AXIS.X)) {
			for (int iSteps=0; iSteps < xSteps.length - 1; iSteps++) {
				if (xSteps[iSteps] <= position.x && position.x <= xSteps[iSteps + 1] ) {
					i = iSteps;
					break;
				}
			}
			if (i == -1) {
				throw new GridError("No valid x found in x axis steps");
			}
		} 

		// Find the y index
		int j = 0;
		if(!normal.equals(AXIS.Y)) {
			for (int jSteps=0; jSteps < ySteps.length - 1; jSteps++) {
				if (ySteps[jSteps] <= position.y && position.y <= ySteps[jSteps + 1] ) {
					j = jSteps;
					break;
				}
			}
			if (j == -1) {
				throw new GridError("No valid y found in y axis steps");
			}
		}

		// Find the z index
		int k = 0;
		if(!normal.equals(AXIS.Z)) {
			for (int kSteps=0; kSteps < zSteps.length - 1; kSteps++) {
				if (zSteps[kSteps] <= position.z && position.z <= zSteps[kSteps + 1] ) {
					k = kSteps;
					break;
				}
			}
			if (k == -1) {
				throw new GridError("No valid z found in z axis steps");
			}
		}

		return new Point3i(i,j,k);
	}

	/**
	 * Fetches the ijk location for a given 3d xyz position
	 * @param position The point in 3 space for which to search
	 * @return
	 * @throws GridError
	 */
	private Point3i getIJKFromPosition3D(Vector3f position) throws GridError {

		getXAxisSteps();
		getYAxisSteps();
		getZAxisSteps();

		// Find the x index
		int i = -1;
		for (int iStep=0; iStep < xSteps.length - 1; iStep++) {
			if (xSteps[iStep] <= position.x && position.x <= xSteps[iStep + 1] ) {
				i = iStep;
				break;
			}
		}
		if (i == -1) {
			throw new GridError("No valid x found in x axis steps" );
		}

		// Find the y index
		int j = -1;
		for (int jStep=0; jStep < ySteps.length - 1; jStep++) {
			if (ySteps[jStep] <= position.y && position.y <= ySteps[jStep + 1] ) {
				j = jStep;
				break;
			}
		}
		if (j == -1) {
			throw new GridError("No valid y found in y axis steps");
		}

		// Find the z index
		int k = -1;
		for (int kStep=0; kStep < zSteps.length - 1; kStep++) {
			if (zSteps[kStep] <= position.z && position.z <= zSteps[kStep + 1] ) {
				k = kStep;
				break;
			}
		}
		if (k == -1) {
			throw new GridError("No valid z found in z axis steps");
		}
		return new Point3i(i,j,k);
	}

	/**
	 * Fetches the positions of the grid steps along the x axis
	 * TODO: This assumes x steps will be the same across all y and z
	 * @return
	 */
	public float[] getXAxisSteps() {

		if(xSteps != null) 
			return xSteps;

		float[] values = fieldValueMap.get("x").getValues();
		if(origin != null) {
			xSteps = new float[gridder.getSize().getX()+1];
			xSteps[0] = origin.x;
			for(int x=1; x<gridder.getSize().getX()+1; x++) {
				xSteps[x] = (values[gridder.getLinearIndex(x-1,0,0)] - xSteps[x-1])*2 + xSteps[x-1];
			}
		} else {
			xSteps = new float[gridder.getSize().getX()];
			for(int x=0; x<gridder.getSize().getX(); x++)
				xSteps[x] = values[gridder.getLinearIndex(x, 0, 0)];
		}
		System.out.println("X Steps: " + Arrays.toString(xSteps));	
		return xSteps;
	}

	/**
	 * Fetches the positions of the grid steps along the y axis
	 * TODO: This assumes y steps will be the same across all x and z
	 * @return
	 */
	public float[] getYAxisSteps() {

		if(ySteps != null) 
			return ySteps;

		float[] values = fieldValueMap.get("y").getValues();

		if(origin != null) {
			ySteps = new float[gridder.getSize().getY()+1];
			ySteps[0] = origin.y;
			for(int y=1; y<gridder.getSize().getY()+1; y++) {
				ySteps[y] = (values[gridder.getLinearIndex(0, y-1, 0)] - ySteps[y-1])*2 + ySteps[y-1];
			}
		} else {
			ySteps = new float[gridder.getSize().getY()];
			for(int y=0; y<gridder.getSize().getY(); y++)
				ySteps[y] = values[gridder.getLinearIndex(0, y, 0)];
		}
		System.out.println("Y Steps: " + Arrays.toString(ySteps));		
		return ySteps;
	}

	/**
	 * Fetches the positions of the grid steps along the z axis
	 * TODO: This assumes z steps will be the same across all x and y
	 * @return
	 */
	public float[] getZAxisSteps() {

		if(zSteps != null) 
			return zSteps;

		float[] values = fieldValueMap.get("z").getValues();
		if(origin != null) {
			zSteps = new float[gridder.getSize().getZ()+1];
			zSteps[0] = origin.z;
			for(int z=1; z<gridder.getSize().getZ()+1; z++) {
				zSteps[z] = (values[gridder.getLinearIndex(0, 0, z-1)] - zSteps[z-1])*2 + zSteps[z-1];
			}
		} else {
			zSteps = new float[gridder.getSize().getZ()];
			for(int z=0; z<gridder.getSize().getZ(); z++)
				zSteps[z] = values[gridder.getLinearIndex(0, 0, z)];
		}
		System.out.println("Z Steps: " + Arrays.toString(zSteps));	
		return zSteps;
	}

	private void setValue(Vector3f vector, AXIS axis, float value) {
		if(axis.equals(AXIS.X))
			vector.x = value;
		if(axis.equals(AXIS.Y))
			vector.y = value;
		if(axis.equals(AXIS.Z))
			vector.z = value;
	}

	private float getValue(Vector3f vector, AXIS axis) {
		if(axis.equals(AXIS.X))
			return vector.x;
		if(axis.equals(AXIS.Y))
			return vector.y;
		if(axis.equals(AXIS.Z))
			return vector.z;
		return 0.0f;
	}


	/* TODO:
	private List<Integer> getInactiveNodes() {
		if(inactiveNodes == null) {
			inactiveNodes = new ArrayList<Integer>();
			if(fieldValueMap.containsKey("Inactive Nodes")) {
				List<Integer> allIndicies = new ArrayList<Integer>();
				for(Float value : fieldValueMap.get("Inactive Nodes").getValues()) {
					allIndicies.add(value.intValue());
				}  
				for(int i = 1; i <= gridder.getL(); i++) {
					if(!allIndicies.contains(i))
						inactiveNodes.add(i);
				}
			}
			Collections.sort(inactiveNodes);
		}
		return inactiveNodes;
	}
	 */
}
