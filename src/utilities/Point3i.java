package utilities;

public class Point3i {

	private final int i;
	private final int j;
	private final int k;
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
	
}
