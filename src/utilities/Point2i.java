package utilities;

public class Point2i implements Comparable<Point2i> {

	private int i;
	private int j;
		
	public Point2i(int i, int j) {
		setI(i);
		setJ(j);
	}
	
	public Point2i(Point2i toCopy) {
		this.i = toCopy.getI();
		this.j = toCopy.getJ();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		builder.append(i).append(", ");
		builder.append(j);
		builder.append(")");
		return builder.toString();
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

	@Override
	public int compareTo(Point2i pt) {
		// Compare by i
		int is = new Integer(i).compareTo(pt.getI());
		if(is != 0)
			return is;
		// Then by j
		return new Integer(j).compareTo(pt.getJ());
	}
	
}
