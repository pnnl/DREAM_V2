package gravity;

/**
 * This object is the (x, y, gz) grid of our Gravity Heat Map.
 * 
 * @author huan482
 *
 */
public class Grid {
	
	private double x;
	
	private double y;

	public Grid(final double x, final double y) {
		this.x = x;
		this.y = y;
		
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	@Override
	public String toString() {
		return "x: " + x + " y: " + y; 
	}
}
