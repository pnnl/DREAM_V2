package mapView;

/**
 * Class for deciding which cells on the grid should be grayed out (allowed or not allowed for well placement)
 * @author rodr144
 */
public class IJ {
	public final int i;
	public final int j;
	public boolean prohibited = true;
	public boolean selectable;
	public IJ(int i, int j, boolean prohibited, boolean selectable){
		this.i = i;
		this.j = j;
		this.prohibited = prohibited;
		this.selectable = selectable;
	}
}
