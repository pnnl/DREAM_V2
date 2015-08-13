package gridviz;

/**
 * @brief  Provides a simple structure for minimal and maximal values
 * @author Tucker Beck
 * @date   3/7/12
 */
public class Extrema {

    public float min;
    public float max;

    public Extrema() {
        min = Float.MAX_VALUE;
        max = -Float.MAX_VALUE;
    }

    public Extrema(Extrema other) {
        min = other.min;
        max = other.max;
    }
	
	public boolean contains(float value) {
		return value >= min && value <= max;
	}

}
