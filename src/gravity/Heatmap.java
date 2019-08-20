package gravity;

import java.awt.Color;
import java.awt.Dimension;
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

	private static final int BASE_DIMENSIONS = 20;

	private File theFolder;

	private File[] listOfFiles;

	private static List<Grid> myGrid;

	private double intervalY;

	private double intervalX;

	private double rowAmountX;

	double maxY;

	private Comparator<Grid> compare;

	private List<String> myTimeSteps;

	private boolean firstMapCompleted;

	private int divisibleTick;

	public Heatmap(final String directory) {
		myGrid = new ArrayList<Grid>();
		myTimeSteps = new ArrayList<String>();
		theFolder = new File(directory);
		listOfFiles = theFolder.listFiles((d, name) -> name.endsWith(".fwd"));
	}

	public Image getHeatMap(final int resolution, final int timeStep) throws IOException {
		return parseGridData(resolution, timeStep);
	}

	public List<String> parseTimeSteps() {
		for (File f : listOfFiles) {
			String temp = f.getName().substring(0, f.getName().indexOf("."));
			String[] tempTokens = temp.split("_");
			myTimeSteps.add(tempTokens[tempTokens.length - 1]);
		}
		return myTimeSteps;
	}

	/**
	 * When user clicks on the marker we will grab the (x,y) value and then match it
	 * with the title of the
	 * 
	 * @throws IOException
	 */
	private Image parseGridData(final int resolution, final int timeStep) throws IOException {
			String line;
			int theFile = 0;
			// The First File
			for (File f : listOfFiles) {
				String temp = f.getName().substring(0, f.getName().indexOf("."));
				String[] tempTokens = temp.split("_");
				if (timeStep == Integer.parseInt(tempTokens[tempTokens.length - 1])) {
					theFile = timeStep - 1;
					break;
				}
			}
			try (BufferedReader br = new BufferedReader(new FileReader(listOfFiles[theFile]))) {
				while ((line = br.readLine()) != null) {
					if (!line.contains("gravity") && !line.contains("x")) {
						String[] tokens = line.trim().split("\\s+");
						try {
//						min = Double.parseDouble(tokens[2]);
//						max = Double.parseDouble(tokens[2]);
							myGrid.add(new Grid(Double.parseDouble(tokens[0]), Double.parseDouble(tokens[1]),
									Double.parseDouble(tokens[2])));
						} catch (NumberFormatException theException) {
							System.out.println("File format is wrong.");
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

			maxY = Collections.max(myGrid, compare).getY();
			rowAmountX = maxX / intervalX;
			return createHeatMap(maxX, maxY, resolution);
	}

	private Image createHeatMap(double themaxX, double theMaxY, int resolution) throws IOException {
		Image myImg = null;
		int counter = 0;
		int rowCounter = 0;
		divisibleTick = (int) rowAmountX / 5;
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
		myImg = outputHeatMap(mapARR, resolution);
		if (firstMapCompleted && resolution != 1) {
			ArrayList<Double> temp = new ArrayList<Double>();
			int numOfElements = (int) ((rowAmountX - (rowAmountX % resolution)) / resolution);
			double[][] scaledMap = new double[numOfElements][];
			double average = 0;
			for (int i = 0; i < rowAmountX - (rowAmountX % resolution); i++) {
				for (int j = 0; j < rowAmountX - (rowAmountX % resolution) + 1; j++) {
					if (j % resolution == 0 && j != 0) {
						temp.add(average);
						average = 0;
					}
					average += mapARR[i][j];
				}
			}
			int counter2 = 0;
			int previousIndex = 0;
			double averageAmount = -1;
			for (int a = 0; a < numOfElements; a++) {
				double[] tempRow = new double[numOfElements];
				for (int b = 0; b < numOfElements; b++) {
					counter2 = 0;
					previousIndex++;
					averageAmount = temp.get(previousIndex);
					for (int c = 0; c < numOfElements; c++) {
						counter2 += numOfElements;
						averageAmount += temp.get(previousIndex + counter2 - 1);
					}
					tempRow[b] = (averageAmount / (resolution * resolution));
				}
				scaledMap[a] = tempRow;
			}
			myImg = outputHeatMap(scaledMap,resolution);
		}
		firstMapCompleted = true;
		return myImg;
	}

	private Image outputHeatMap(double[][] theHeatMapData, final int resolution) throws IOException {
		HeatChart map = new HeatChart(theHeatMapData);
		// Dark Blue
		Color lowValColor = Color.decode("#05469B");
		// Dark Red
		Color highValColor = Color.decode("#3f0000");
		map.setLowValueColour(lowValColor);
		map.setHighValueColour(highValColor);
		map.setColourScale(1);
		map.setXValues(intervalX, intervalX);
		map.setYValues((Double) map.getXValues()[map.getXValues().length - 1], -intervalY);
		map.setTitle("Gravity Contour Map");
		map.setXAxisLabel("X-Vals (m)");
		map.setYAxisLabel("Y-Vals (m)");
		// Default is 20
		map.setCellSize(new Dimension(BASE_DIMENSIONS * resolution, BASE_DIMENSIONS * resolution));
		return map.getChartImage();
	}

	public int getDivisibleTick() {
		return divisibleTick;
	}

}
