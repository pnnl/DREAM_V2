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
	
	private float OffsetX;
	
	private float OffsetY;
	
	public ExistingWell(final Float x, final Float y, final Float z,
			final Float theOffsetX, final Float theOffsetY) {
		this.x = x;
		this.y = y;
		this.z = z;
		OffsetX = theOffsetX;
		OffsetY = theOffsetY;
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
	
	public float getOriginalXLocation() {
		return OffsetX;
	}
	
	public float getOriginalYLocation() {
		return OffsetY;
	}
	
	@Override
	public String toString() {
		return "x-value: " + x + " y-value: " + y + " z-value: " + z;
	}
}
