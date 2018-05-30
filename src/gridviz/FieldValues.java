package gridviz;

/**
 * @brief  Provides a class that keeps a collection of values and their extrema
 * @author Tucker Beck
 * @date   3/7/12
 */
public class FieldValues {
    
    private float[] values; // The values for this field
    private Extrema extrema; // The extrema for this field
    private float[] vertex; // The vertex for this field (only x, y, and z)
    
    // Constructs this field container
    public FieldValues(int size) {
        values = new float[size];
        extrema = new Extrema();
        vertex = new float[size+1];
    }
    
    
    // Sets a value for the field at the given index and updates the extrema
    public void setValue(int index, float value) throws GridError {
        if (index >= values.length)
        	return;
        
        values[index] = value;
        
        extrema.min = Math.min(extrema.min, value);
        extrema.max = Math.max(extrema.max, value);
    }
    
    
    // Gets the value for a given index
    public float getValue(int index) {
        return values[index];
    }
    
    
	// Gets the value for a given index
    public float getVertex(int index) {
        return vertex[index];
    }
    
    
    // Gets the extrema for the field
    public Extrema getExtrema() {
        return extrema;
    }
    
    
    // Overrides the computed extrema with a user defined extrema
    public void overrideExtrema(Extrema extrema) {
        this.extrema = extrema;
    }
    
    
    // Fetches the values for the grid
    public float[] getValues() {
        return values;
    }
    
    
    // Fetches the vertices for the grid
    public float[] getVertices() {
        return vertex;
    }

	public void addNodalValue(int nodeIndex, String[] vertices) {
		// Average them all!
		float average = 0;
		int total = 0;
		for(String vertex: vertices) {
			average += Float.parseFloat(vertex);
			total++;
		}
		float value = average/total;
		values[nodeIndex] = value;
		
        extrema.min = Math.min(extrema.min, value); // Keep this up to date
        extrema.max = Math.max(extrema.max, value);
	}
	
	
	public void addVertex(int nodeIndex, String[] vertices, int column){
		vertex[nodeIndex] = Float.parseFloat(vertices[0]);
		vertex[nodeIndex + 1] = Float.parseFloat(vertices[column]);
	}
	
	
	public void addValues(float[] values) {
		this.values = values;
	}
}

