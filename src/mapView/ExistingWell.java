package mapView;
/**
 * Object that represents an existing well.
 * @author huan482
 *
 */
public class ExistingWell {
	
	private float x;
	
	private float y;
	
	private float z;
	
	public ExistingWell(final Float x, final Float y, final Float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}
	
	public float getZ() {
		return z;
	}
	
	@Override
	public String toString() {
		return "x-value: " + x + " y-value: " + y + " z-value: " + z;
	}
}
