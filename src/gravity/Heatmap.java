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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gravity.HeatChart;

/**
 * Heatmap creation class.
 * 
 * @author huan482
 *
 */
// Format for a .FWD File 
// Header
// x   y  gz  gy
// #   #  #   #
//We only need (x,y,gz) as our points for the heat map.
public class Heatmap {

	private static final int BASE_DIMENSIONS = 20;

//	private double[][] firstHeatMap;

	private File theFolder;

	private File[] listOfFiles;

	private static List<Grid> myGrid;

	private double intervalY;

	private double intervalX;

	private double sizeOfSquare;

	private double maxY;

	private Font baseFont;

	private Comparator<Grid> compare;

	private List<String> myTimeSteps;

	private int divisibleTick;

	private double max;

	private double min;

	private Image colorScaleImage;

	private Image differenceMap;

	private boolean colorScaleCreated;

	private static final String LOW_VAL_COLOUR = "#05469B";

	private static final String HIGH_VAL_COLOUR = "#3f0000";

	private boolean doOnce;

	private double[][] firstHeatMap;

	private double[][] differenceArr;

	private double[][] firstTimeStep;

	public Heatmap(final String directory) {
		// Base Font can be subject to change.
		// Format (Font, Font Attribute, Font Size)
		baseFont = new Font("Arial", Font.BOLD, 16);
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
		myGrid.clear();
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
		sizeOfSquare = maxX / intervalX;
		// build the color scale
		if (!colorScaleCreated) {
			compare = Comparator.comparing(Grid::getgz);
			max = Collections.max(myGrid, compare).getgz();
			min = Collections.min(myGrid, compare).getgz();
			double intervalForColorScale = (max - min) / sizeOfSquare;
			double[][] colorScale = new double[(int) sizeOfSquare][];
			colorScaleImage = null;
			double originalMax = max;
			for (int i = 0; i < sizeOfSquare; i++) {
				double[] rowForScale = new double[2];
				rowForScale[0] = max;
				rowForScale[1] = max;
				max -= intervalForColorScale;
				colorScale[i] = rowForScale;
			}
			colorScaleImage = outputColorScale(colorScale, originalMax, intervalForColorScale);
		}
		colorScaleCreated = true;
		differenceArr = new double[(int) sizeOfSquare][(int) sizeOfSquare];
		firstHeatMap = new double[(int) sizeOfSquare][(int) sizeOfSquare];
		return createHeatMap(maxX, maxY, resolution);
	}

	private Image createHeatMap(double themaxX, double theMaxY, int resolution) throws IOException {
		Image myImg = null;
		differenceMap = null;
		int counter = 0;
		int rowCounter = 0;
		divisibleTick = (int) sizeOfSquare / 5;
		// Creates the 1:1 resolution map.
		double[][] mapARR = new double[(int) sizeOfSquare][];
		for (double yVal = theMaxY; yVal >= intervalY; yVal -= intervalY) {
			counter = 0;
			double[] tempRow = new double[(int) sizeOfSquare];
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
		if (!doOnce) {
			firstTimeStep = Arrays.stream(mapARR).map(r -> r.clone()).toArray(double[][]::new);
			doOnce = true;
		}
		firstHeatMap = Arrays.stream(firstTimeStep).map(r -> r.clone()).toArray(double[][]::new);
		myImg = outputHeatMap(mapARR);
		differenceMap = outputHeatMap(createFirstDifferenceMap(mapARR, firstHeatMap));
		// This section of the code is what we use for different resolutions
		if (resolution != 1) {
			myImg = outputHeatMap(createDifferentResolutionMap(mapARR, resolution));
			differenceMap = outputHeatMap(
					createFirstDifferenceMap(mapARR, createDifferentResolutionMap(firstHeatMap, resolution)));
		}
		return myImg;
	}

	/**
	 * This method deals with creating a HeatMap with different resolutions.
	 * (resolution != 1)
	 * 
	 * @param mapARR     - The map we're changing and which has all of our
	 *                   information in.
	 * @param resolution - The resolution requested.
	 * @return - The new HeatMap
	 * @throws IOException - We're turning an image which has some risks inherently.
	 */
	private double[][] createDifferentResolutionMap(final double[][] mapARR, final int resolution) throws IOException {
		ArrayList<Double> temp = new ArrayList<Double>();
		double average = 0;
		int remainder = (int) (sizeOfSquare - (sizeOfSquare % resolution));
		int cc = 0;
		double outlierAverageRow = 0;
		double outlierAverageColumn = 0;
		ArrayList<Double> rowOutlier = new ArrayList<Double>();
		ArrayList<Double> columnOutlier = new ArrayList<Double>();
		// This part of the code calculates the average of the (resolution x resolution)
		// square we're requesting for.
		// THe 4 for loops is just a method to average the values in the square.
		for (int row = 0; row < sizeOfSquare; row += resolution) {
			for (int column = 0; column < sizeOfSquare; column += resolution) {
				for (int k = 0; k < resolution; k++) {
					for (int l = 0; l < resolution; l++) {
						if (row < remainder && column < remainder) {
							if ((row + k + 1) % resolution == 0 && (column + l + 1) % resolution == 0) {
								temp.add(average / Math.pow(resolution, 2));
								average = 0;
							}
							average += mapARR[row + k][column + l];
						} else if (column + l < sizeOfSquare && row + k < sizeOfSquare && column + l >= remainder) {
							// Purpose of this branch is too average the outlier's rows/columns.
							// We add the averaged values into a separate list.
							outlierAverageRow += mapARR[row + k][column + l];
							outlierAverageColumn += mapARR[column + l][row + k];
							cc++;
							if (cc == resolution) {
								rowOutlier.add(outlierAverageRow / resolution);
								columnOutlier.add(outlierAverageColumn / resolution);
								cc = 0;
								outlierAverageRow = 0;
								outlierAverageColumn = 0;
							}
						}
					}
				}
			}
		}
		int squareCounter = 0;
		int outlierCounter = 0;
		int otherC = 0;
		// This section of the for loop places all the averages we calculated for the
		// square inside the square.
		for (int row = 0; row < sizeOfSquare; row += resolution) {
			for (int column = 0; column < sizeOfSquare; column += resolution) {
				for (int k = 0; k < resolution; k++) {
					for (int l = 0; l < resolution; l++) {
						if (row < remainder && column < remainder) {
							// This is the replacement part of the code where we replace the elements inside
							// our array.
							mapARR[row + k][column + l] = temp.get(squareCounter);
							if ((row + k + 1) % resolution == 0 && (column + l + 1) % resolution == 0) {
								// When we move onto the next (# x # square) we're going to go to the next value
								// we found averaged
								squareCounter++;
							}
						} else if (column + l < sizeOfSquare && row + k < sizeOfSquare && column + l >= remainder) {
							if (otherC == resolution) {
								// We need to replace the outlier's now.
								outlierCounter++;
								otherC = 0;
								if (outlierCounter == rowOutlier.size()) {
									outlierCounter--;
								}
							}
							mapARR[row + k][column + l] = rowOutlier.get(outlierCounter);
							mapARR[column + l][row + k] = columnOutlier.get(outlierCounter);
							otherC++;
						}
					}
				}
			}
		}
		return mapARR;
	}

	private double[][] createFirstDifferenceMap(final double[][] absoluteHeatMap, final double[][] first)
			throws IOException {
		for (int i = 0; i < absoluteHeatMap.length; i++) {
			for (int j = 0; j < absoluteHeatMap[i].length; j++) {
				differenceArr[i][j] = absoluteHeatMap[i][j] - first[i][j];
			}
		}
		return differenceArr;
	}

	/**
	 * This method generates are entire heatmap.
	 * 
	 * @param theHeatMapData - Our entire 2-D heatmap array.
	 * @return - The heatmap as an image.
	 * @throws IOException
	 */
	private Image outputHeatMap(double[][] theHeatMapData) throws IOException {
		HeatChart map = new HeatChart(theHeatMapData);
		// To change the font please change the variable baseFont
		map.setAxisValuesFont(baseFont);
		map.setAxisLabelsFont(baseFont);
		// Dark Blue
		map.setLowValueColour(Color.decode(LOW_VAL_COLOUR));
		// Dark Red
		map.setHighValueColour(Color.decode(HIGH_VAL_COLOUR));
		map.setColourScale(1);
		map.setXAxisValuesFrequency(2);
		map.setYAxisValuesFrequency(2);
		map.setXValues((int) intervalX, (int) intervalX);
		map.setYValues(maxY, -intervalY);
		map.setXAxisLabel("Easting (m)");
		map.setYAxisLabel("Northing (m)");
		// Default is 20, change static variable to get different cell size.
		map.setCellSize(new Dimension(BASE_DIMENSIONS, BASE_DIMENSIONS));
		return map.getChartImage();
	}

	/**
	 * This method outputs the color legend shown on the right most part of the
	 * container.
	 * 
	 * @param theColourScale - The colour scale arrays we need to build the legend.
	 * @param max            - max double that our colourscale has.
	 * @param interval       - Interval for each mark.
	 * @return - A colour legend image for our heatmap.
	 * @throws IOException
	 */
	private Image outputColorScale(double[][] theColourScale, final double max, final double interval)
			throws IOException {
		HeatChart map = new HeatChart(theColourScale);
		map.setAxisValuesFont(baseFont);
		// Dark Blue
		map.setLowValueColour(Color.decode(LOW_VAL_COLOUR));
		// Dark Red
		map.setHighValueColour(Color.decode(HIGH_VAL_COLOUR));
		map.setColourScale(1);
		map.setShowXAxisValues(false);
		map.setYAxisValuesFrequency((int) sizeOfSquare / 4);
		map.setYValues(max, -interval);
		map.setCellSize(new Dimension(BASE_DIMENSIONS, BASE_DIMENSIONS));
		return map.getChartImage();
	}

	public int getDivisibleTick() {
		return divisibleTick;
	}

	public Image getColorScale() {
		return colorScaleImage;
	}

	public Image getDifferenceMap() {
		return differenceMap;
	}

}
