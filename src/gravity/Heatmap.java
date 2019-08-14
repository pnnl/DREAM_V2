package gravity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Format for a .FWD File 
// Header
// x   y  gz  gy
// #   #  #   #
//We only need (x,y,gz) as our points for the heat map.
public class Heatmap {
	
	/**
	 * When user clicks on the marker we will grab the (x,y) value and then match it with the title of the 
	 */
	public void parseData(final File theFileUsed) {
		List<Grid>  myGrid = new ArrayList<Grid>();
		List<Double> myGZVals = new ArrayList<Double>();
		String line;
		try (BufferedReader br = new BufferedReader(new FileReader(theFileUsed))) {
			while ((line = br.readLine()) != null) {
				if (!line.contains("gravity") && !line.contains("x")) {
					String[] tokens = line.trim().split("\\s+");
					try {
						myGrid.add(new Grid(Double.parseDouble(tokens[0]), Double.parseDouble(tokens[1])));
						myGZVals.add(Double.parseDouble(tokens[2]));
					} catch (NumberFormatException theException) {
						System.out.println(tokens[0] + " " + tokens[1] + " " + tokens[2]);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		createHeatMap(myGrid, myGZVals);
	}
	
	private void createHeatMap(final List<Grid> theGrid, final List<Double> theGZVals) {
		
	}
}
