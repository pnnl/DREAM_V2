package utilities;

public class Point3i {

	private int i;
	private int j;
	private int k;
		
	public Point3i() {
		setI(0);
		setJ(0);
		setK(0);
	}
	
	public Point3i(int i, int j, int k) {
		this.i = i;
		this.j = j;
		this.k = k;
	}

	public Point3i(Point3i toCopy) {
		this.i = toCopy.getI();
		this.j = toCopy.getJ();
		this.k = toCopy.getK();
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
		int[] array = new int[3];
		array[0] = i;
		array[1] = j;
		array[2] = k;
		return java.util.Arrays.hashCode(array);
	}
	
	public int getI() {
		return i;
	}

	public void setI(int i) {
		this.i = i;
	}

	public int getJ() {
		return j;
	}

	public void setJ(int j) {
		this.j = j;
	}

	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}
	
}
