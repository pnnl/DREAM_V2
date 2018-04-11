package gridviz;

/// Provides a custom exception for the Data Grid
public class GridError extends Exception {
	
	private static final long serialVersionUID = 7475716344945435537L;
	
	public GridError(String message) {
        super(message);
    }
}