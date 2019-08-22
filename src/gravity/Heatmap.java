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

	private double sizeOfSquare;

	double maxY;

	private Comparator<Grid> compare;

	private List<String> myTimeSteps;

	private int divisibleTick;

	private double max;

	private double min;

	private Image colorScaleImage;

	private boolean colorScaleCreated;
	
	private static final String LOW_VAL_COLOUR =  "#05469B";
	
	private static final String HIGH_VAL_COLOUR =  "#3f0000";
	
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

		return createHeatMap(maxX, maxY, resolution);
	}

	private Image createHeatMap(double themaxX, double theMaxY, int resolution) throws IOException {
		Image myImg = null;
		int counter = 0;
		int rowCounter = 0;
		divisibleTick = (int) sizeOfSquare / 5;
		ArrayList<Double> temp = new ArrayList<Double>();
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
		myImg = outputHeatMap(mapARR);

		if (resolution != 1) {
			double average = 0;
			int remainder = (int) (sizeOfSquare - (sizeOfSquare % resolution));
			int cc = 0;
			double outlierAverageRow = 0;
			double outlierAverageColumn = 0;
			ArrayList<Double> rowOutlier = new ArrayList<Double>();
			ArrayList<Double> columnOutlier = new ArrayList<Double>();
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
			for (int row = 0; row < sizeOfSquare; row += resolution) {
				for (int column = 0; column < sizeOfSquare; column += resolution) {
					for (int k = 0; k < resolution; k++) {
						for (int l = 0; l < resolution; l++) {
							if (row < remainder && column < remainder) {
								mapARR[row + k][column + l] = temp.get(squareCounter);
								if ((row + k + 1) % resolution == 0 && (column + l + 1) % resolution == 0) {
									squareCounter++;
								}
							} else if (column + l < sizeOfSquare && row + k < sizeOfSquare && column + l >= remainder) {
								if (otherC == resolution) {
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
			myImg = outputHeatMap(mapARR);
		}
		return myImg;
	}

	private Image outputHeatMap(double[][] theHeatMapData) throws IOException {
		HeatChart map = new HeatChart(theHeatMapData);
		// Dark Blue
		map.setLowValueColour(Color.decode(LOW_VAL_COLOUR));
		// Dark Red
		map.setHighValueColour(Color.decode(HIGH_VAL_COLOUR));
		map.setColourScale(1);
		map.setXAxisValuesFrequency(2);
		map.setYAxisValuesFrequency(2);
		map.setXValues(intervalX, intervalX);
		map.setYValues((Double) map.getXValues()[map.getXValues().length - 1], -intervalY);
		map.setTitle("Gravity Contour Map");
		map.setXAxisLabel("Easting (m)");
		map.setYAxisLabel("Northing (m)");
		// Default is 20
		map.setCellSize(new Dimension(BASE_DIMENSIONS, BASE_DIMENSIONS));
		return map.getChartImage();
	}

	private Image outputColorScale(double[][] theColourScale, final double max, final double interval)
			throws IOException {
		HeatChart map = new HeatChart(theColourScale);
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

}
