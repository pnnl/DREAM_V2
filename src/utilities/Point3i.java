package utilities;

import java.util.Comparator;

/**
 * 3-dimensional integer class
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class Point3i {

	private int i;
	private int j;
	private int k;
	private final int hash;
		
	public Point3i() {
		this.i = 0;
		this.j = 0;
		this.k = 0;
		this.hash = getHash();
	}
	
	public Point3i(int i, int j, int k) {
		this.i = i;
		this.j = j;
		this.k = k;
		this.hash = getHash();
	}

	public Point3i(Point3i toCopy) {
		this.i = toCopy.getI();
		this.j = toCopy.getJ();
		this.k = toCopy.getK();
		this.hash = getHash();
	}
	
	private int getHash(){
		/*
		int[] array = new int[3];
		array[0] = i;
		array[1] = j;
		array[2] = k;
		return java.util.Arrays.hashCode(array);
		*/
		return 1000000*i+1000*j+k;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		builder.append(i).append(", ");
		builder.append(j).append(", ");
		builder.append(k);
		builder.append(")");
		return builder.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Point3i) {
			Point3i pt = (Point3i)obj;
			return i == pt.i && j == pt.j && k == pt.k;
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return hash;
	}
	
	public int getI() {
		return i;
	}

	public int getJ() {
		return j;
	}

	public int getK() {
		return k;
	}
	
	public void setI(int i) {
		this.i = i;
	}
	
	public void setJ(int j) {
		this.j = j;
	}
	
	public void setK(int k) {
		this.k = k;
	}
	
	public static String toCleanString(Point3i ijk) {
		String line = ijk.getI() + "," + ijk.getJ() + "," + ijk.getK();
		return line;

	}
	
	public static final Comparator<Point3i> IJ_COMPARATOR = new Comparator<Point3i>() {
		@Override
		public int compare(Point3i o1, Point3i o2) {
			if (o1.i < o2.i)
				return -1;
			else if (o1.i == o2.i && o1.j < o2.j)
				return -1;
			else if (o1.i == o2.i && o1.j == o2.j)
				return 0;
			return 1;
		}
	};
}
