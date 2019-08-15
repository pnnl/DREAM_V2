package gravity;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import gravity.HeatChart;

// Format for a .FWD File 
// Header
// x   y  gz  gy
// #   #  #   #
//We only need (x,y,gz) as our points for the heat map.
public class Heatmap {

	private File theFolder;

	private File[] listOfFiles;

	private static List<Grid> myGrid;

	private double intervalY;

	private double intervalX;

	private double rowAmountX;

	private Comparator<Grid> compare;

	public Heatmap(final String directory) {
		myGrid = new ArrayList<Grid>();
		theFolder = new File(directory);
		listOfFiles = theFolder.listFiles((d, name) -> name.endsWith(".fwd"));
	}
	
	public Image getHeatMap() throws IOException {
		return parseGridData();
	}
	
	/**
	 * When user clicks on the marker we will grab the (x,y) value and then match it
	 * with the title of the
	 * 
	 * @throws IOException
	 */
	private Image parseGridData() throws IOException {
		String line;
		// The First File
		try (BufferedReader br = new BufferedReader(new FileReader(listOfFiles[0]))) {
			while ((line = br.readLine()) != null) {
				if (!line.contains("gravity") && !line.contains("x")) {
					String[] tokens = line.trim().split("\\s+");
					try {
						myGrid.add(new Grid(Double.parseDouble(tokens[0]), Double.parseDouble(tokens[1]),
								Double.parseDouble(tokens[2])));
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
		compare = Comparator.comparing(Grid::getX).thenComparing(Grid::getY);
		intervalY = myGrid.get(1).getY() - myGrid.get(0).getY();
		double temp = myGrid.get(0).getX();
		for (Grid g : myGrid) {
			if (g.getX() != temp) {
				intervalX = g.getX() - myGrid.get(0).getX();
				break;
			}
		}
		double maxX = Collections.max(myGrid, compare).getX();

		double maxY = Collections.max(myGrid, compare).getY();
		rowAmountX = maxX / intervalX;
		return createHeatMap(maxX, maxY);
	}

	private Image createHeatMap(double themaxX, double theMaxY) throws IOException {
		int counter = 0;
		int rowCounter = 0;
		double[][] mapARR = new double[(int) rowAmountX][];
		for (double yVal = theMaxY; yVal >= intervalY; yVal -= intervalY) {
			counter = 0;
			double[] tempRow = new double[(int) rowAmountX];
			for (double xVal = intervalX; xVal <= themaxX; xVal += intervalX) {
				for (Grid gridbox : myGrid) {
					if (gridbox.contains(xVal, yVal)) {
						tempRow[counter] = gridbox.getgz();
						counter++;
						break;
					}
				}
			}
			mapARR[rowCounter] = tempRow;
			rowCounter++;
		}
		return outputHeatMap(mapARR);
	}

	private Image outputHeatMap(double[][] theHeatMapData) throws IOException {
		HeatChart map = new HeatChart(theHeatMapData);
		// Dark Blue
		Color lowValColor = Color.decode("#05469B");
		// Dark Red
		Color highValColor = Color.decode("#3f0000");
		map.setLowValueColour(lowValColor);
		map.setHighValueColour(highValColor);
		map.setColourScale(1);
		map.setYValues(980, -intervalY);
		map.setXValues(intervalX, intervalX);
		map.setTitle("Gravity Contour Map");
		map.setXAxisLabel("X-Vals (m)");
		map.setYAxisLabel("Y Vals (m)");
		// Default is 20
		map.setCellSize(new Dimension(20, 20));
		map.setAxisLabelsFont(new Font("Sans-Serif", Font.PLAIN, 20));
		return map.getChartImage();
	}
}
