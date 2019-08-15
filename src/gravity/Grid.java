package gravity;

/**
 * This object is the (x, y, gz) grid of our Gravity Heat Map.
 * 
 * @author huan482
 *
 */
public class Grid  {
	
	private double x;
	
	private double y;
	
	private double gz; 
	
	public Grid(final double x, final double y, final double gzIndex) {
		this.x = x;
		this.y = y;
		this.gz = gzIndex;
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public double getgz() {
		return gz;
	}
	
	public boolean contains(final double xInput, final double yInput) {
		return xInput == x && yInput == y;
	}
	
	@Override
	public String toString() {
		return "x: " + x + " y: " + y + " " + gz; 
	}
}
