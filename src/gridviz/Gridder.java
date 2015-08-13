package gridviz;

import javax.vecmath.Point3i;
import javax.vecmath.Vector3d;

/**
 * @brief  Provides some simple index conversion functionality
 * @author Tucker Beck
 * @date   3/7/12
 */
public class Gridder {

    /// The size of the grid
    protected Point3i size;

    /// The maximum of linear indices for this grid
    int l;



    /// Constructs this gridder for the specified size
    public Gridder(Point3i size) {
        this.size = size;
        l = size.x * size.y * size.z;
    }

    /// Computes axial indices from a linear index for a 3d grid
    public Point3i getIndices(int linearIndex) {
    	// Assumes uniform grid...
        int temp = linearIndex;
        int z = temp / (size.y * size.x);
        temp = linearIndex % (size.y * size.x);
        int y = temp / size.x;
        int x = temp % size.x;
        return new Point3i(x, y, z);
    }

    public void setGridSize(Point3i newSize) {
    	size.x = newSize.x;
    	size.y = newSize.y;
    	size.z = newSize.z;
        l = size.x * size.y * size.z;
    }

    /// Computes a linear index from axial indices for a 3d grid
    public int getLinearIndex(Point3i position) {
        return getLinearIndex(position.x, position.y, position.z);
    }



    /// Computes a linear index from axial indices for a 3d grid
    public int getLinearIndex(int i, int j, int k) {
    	if(i >= size.x) {
    		System.out.println("ERROR i is to big!\ti=" + i);
    	}
    	if(j >= size.y) {
    		System.out.println("ERROR y is to big!\tj=" + j);
    	}
    	if(k >= size.z) {
    		System.out.println("ERROR k is to big!\tk=" + k);
    	}
        return k * size.y * size.x + j * size.x + i;
    }

    public Vector3d getXYZFromIJK(int i, int j, int k, double[] xVals, double[] yVals, double[] zVals) {
    	Vector3d xyz = new Vector3d();
    	int index = getLinearIndex(i,j,k);
    	// x, y, z should be half way between this node and the previous
    	xyz.setX((xVals[index]-xVals[index-1])/2+xVals[index-1]);
    	xyz.setY((yVals[index]-yVals[index-1])/2+yVals[index-1]);
    	xyz.setZ((zVals[index]-zVals[index-1])/2+zVals[index-1]);
    	return xyz;
    }


    /// Fetches the size of the gridder
    public Point3i getSize() {
        return size;
    }



    /// Fetches the maximal linear index for the gridder
    public int getL() {
        return l;
    }

	public Vector3d getXYZFromIJK(Point3i ijks, double[] xVals, double[] yVals, double[] zVals) {
		return getXYZFromIJK(ijks.getX(), ijks.getY(), ijks.getZ(), xVals, yVals, zVals);
	}
}
