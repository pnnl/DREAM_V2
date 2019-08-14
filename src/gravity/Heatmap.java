package gravity;

import java.io.File;

// Format for a .FWD File 
// Header
// x , y, gz, gy
// #   #  #   #
//We only need (x,y,gz) as our points for the heat map.
public class Heatmap {
		
	private File[] myListOfFiles;

	public Heatmap(final File[] theListOfValidFiles) {
		myListOfFiles = theListOfValidFiles;	
	}
	/**
	 * When user clicks on the marker we will grab the (x,y) value and then match it with the title of the 
	 */
	public void createSpecificHeatMap() {
		
	}
}
