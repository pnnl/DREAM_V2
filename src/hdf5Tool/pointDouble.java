package hdf5Tool;

public class pointDouble {
	
	private double x;
	
	private double y;
	
	public pointDouble(final double xPoint, final double yPoint) {
		this.x = xPoint;
		this.y = yPoint;
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public boolean contains(final double xVal, final double yVal) {
		return x == xVal && y == yVal;
	}
}
