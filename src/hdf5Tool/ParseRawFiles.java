package hdf5Tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import utilities.Constants;

/**
 * @brief Reads a variety of input files to convert into DREAM-compatible Hdf5
 *        files
 * @author Jonathan Whiting
 * @date February 18, 2019
 */
public class ParseRawFiles {
	private static final String POSITIVE = "positive";
	private ArrayList<String> scenarios;
	private ArrayList<Float> times;
	private ArrayList<String> parameters;
	private int commonStartCount;
	private int commonEndCount;

	private ArrayList<String> selectedScenarios;
	private ArrayList<Float> selectedTimes;
	private ArrayList<String> selectedParameters;

	private ArrayList<Float> x;
	private ArrayList<Float> y;
	private ArrayList<Float> z;
	private int nodes;

	private ArrayList<Float> vertexX;

	private ArrayList<Float> vertexY;

	private ArrayList<Float> vertexZ;

	private float[] porosity;

	private Map<String, Map<String, float[][]>> dataMap; // scenario <parameter, float[time][nodes]>

	private Map<String, Map<String, float[]>> statistics; // scenario, parameter, float[min, avg, max]

	private Map<String, String> units; // parameter, units

	// Only used by STOMP
	private boolean nodal; // Determine whether parameters are given for nodes or vertices
	// Only used by NUFT and Tecplot
	private ArrayList<String> indexMap; // Maps parameters to columns or blocks
	// Only used by Tecplot
	private int elements;

	// Initialize variables
	public ParseRawFiles() {
		scenarios = new ArrayList<String>();
		times = new ArrayList<Float>();
		parameters = new ArrayList<String>();
		commonStartCount = Integer.MAX_VALUE;
		commonEndCount = Integer.MAX_VALUE;

		selectedScenarios = new ArrayList<String>();
		selectedTimes = new ArrayList<Float>();
		selectedParameters = new ArrayList<String>();

		x = new ArrayList<Float>();
		y = new ArrayList<Float>();
		z = new ArrayList<Float>();

		vertexX = new ArrayList<Float>();
		vertexY = new ArrayList<Float>();
		vertexZ = new ArrayList<Float>();

		dataMap = new HashMap<String, Map<String, float[][]>>();
		statistics = new HashMap<String, Map<String, float[]>>();
		units = new HashMap<String, String>();
		units.put(POSITIVE, "");
		indexMap = new ArrayList<String>();
	}

	// Extracting scenarios, times, parameters, and xyz from a list of STOMP
	// directories
	public void extractStompStructure(File parentDirectory) {
		// Loop through the list of directories in the parent folder
		for (File directory : parentDirectory.listFiles()) {
			if (!directory.isDirectory())
				continue; // We only want folders - skip files
			// Add all the scenarios from folder names
			String scenarioName = directory.getName();
			if (!scenarios.contains(scenarioName))
				scenarios.add(scenarioName);
			// A quick check that we actually have STOMP files in the directory
			if (directory.listFiles().length == 0) {
				System.out.println("No STOMP files were found in the selected directory.");
				return;
			}
			// Read the first file to get the parameters
			File firstFile = directory.listFiles()[0];
			String line;
			String parameter = "";
			boolean blankLine = false;
			boolean header = true;
			try (BufferedReader br = new BufferedReader(new FileReader(firstFile))) {
				while ((line = br.readLine()) != null) { // We actually have to read the whole file... parameters are
															// scattered throughout
					// We are assuming this is always the last line of the header, good until proven
					// otherwise
					if (line.contains("Number of Vertices"))
						header = false;
					// These are the criteria to isolate the parameter text above blocks
					else if (!header && blankLine) {
						parameter = line.split(",")[0].trim().replaceAll("\\(", "_").replaceAll("\\)", ""); // New clean
																											// parameter
						// If it has Nodal/Node in the name, it is giving values for edges
						if (parameter.contains("Nodal") || line.contains("Node"))
							nodal = true;
						// We want to normalize the x, y, z
						if (parameter.startsWith("X or R-Direction Node Positions")
								|| parameter.startsWith("Radial-Direction Node Positions")
								|| parameter.startsWith("X-Direction Surface Positions")
								|| parameter.startsWith("X-Direction Node Positions")
								|| parameter.startsWith("X-Direction Nodal Vertices")) {
							parameter = "x";
						} else if (parameter.startsWith("Y or Theta-Direction Node Positions")
								|| parameter.startsWith("Theta-Direction Node Positions")
								|| parameter.startsWith("Y-Direction Surface Positions")
								|| parameter.startsWith("Y-Direction Node Positions")
								|| parameter.startsWith("Y-Direction Nodal Vertices")) {
							parameter = "y";
						} else if (parameter.startsWith("Z-Direction Surface Positions")
								|| parameter.startsWith("Z-Direction Node Positions")
								|| parameter.startsWith("Z-Direction Nodal Vertices")) {
							parameter = "z";
							// Store a list of parameters
						} else if (!parameters.contains(parameter) && !parameter.equals("Node Volume")
								&& !parameter.equals("Node Map") && !parameter.toLowerCase().contains("porosity")) // Skip
																													// these
							parameters.add(parameter);
						// Save units for all parameters if they are available
						if (line.contains(",") && !line.contains("null")) { // This means they give units
							String unit = line.split(",")[1].trim();
							units.put(parameter, unit); // Save units
						}
						// These are the criteria to isolate the xyz values
					} else if (!header && !line.equals("") && (parameter.equals("x") || parameter.equals("y")
							|| parameter.equals("z") || parameter.equals("porosity"))) {
						String[] tokens = line.trim().split("\\s+"); // Split the line by any number of spaces between
																		// values
						for (String token : tokens) { // Loop through the tokens
							Float value = null;
							try {
								value = Float.parseFloat(token); // Parse value into float

								// Provided values are at the edge of each cell
								if (nodal) {
									if (parameter.equals("x") && !vertexX.contains(value))
										vertexX.add(value); // Store x values
									else if (parameter.equals("y") && !vertexY.contains(value))
										vertexY.add(value); // Store y values
									else if (parameter.equals("z") && !vertexZ.contains(value))
										vertexZ.add(value); // Store z values
								}
								// Provided values are at the center of each cell
								else {
									if (parameter.equals("x") && !x.contains(value))
										x.add(value); // Store x values
									else if (parameter.equals("y") && !y.contains(value))
										y.add(value); // Store y values
									else if (parameter.equals("z") && !z.contains(value))
										z.add(value); // Store z values
								}
							} catch (Exception e) {
								System.out.println("Error parsing the " + parameter + " value: " + token);
							}
						}
					}
					if (line.isEmpty())
						blankLine = true;
					else
						blankLine = false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (nodal) { // Provided values are at the edge of each cell, calculate the center
				x = calculateCenters(vertexX);
				y = calculateCenters(vertexY);
				z = calculateCenters(vertexZ);
			} else { // Provided values are at the center of each cell, calculate the edge
				vertexX = calculateEdges(x);
				vertexY = calculateEdges(y);
				vertexZ = calculateEdges(z);
			}
			nodes = x.size() * y.size() * z.size();
			// Read the header of every file to get the times
			for (File subFile : directory.listFiles()) {
				try (BufferedReader br = new BufferedReader(new FileReader(subFile))) {
					while ((line = br.readLine()) != null) { // We just need to read the header for each file
						if (line.contains("Time =") & line.contains(",yr")) {
							units.put("times", "Years");
							String year = line.substring(line.indexOf(",wk") + 3, line.indexOf(",yr")).trim();
							try {
								Float timeStep = Math.round(Float.parseFloat(year) * 1000f) / 1000f; // This rounds to 3
																										// decimal
																										// places
								if (!times.contains(timeStep))
									times.add(timeStep);
							} catch (Exception e) {
								System.out.println("Years Error: " + year);
							}
							break; // No need to read the rest of the file
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		cleanParameters();
	}

	// Extracting data, statistics, and porosity from a list of STOMP directories
	public void extractStompData(File directory) {
		String scenario = directory.getName();
		dataMap.put(scenario, new HashMap<String, float[][]>()); // Initialize dataMap for this scenario
		statistics.put(scenario, new HashMap<String, float[]>()); // Initialize statistics for this scenario
		System.out.println("Reading variables: " + selectedParameters.toString());
		// Loop through the list of files in each directory
		for (File dataFile : directory.listFiles()) {
			System.out.print("    Reading " + scenario + "/" + dataFile.getName() + "...");
			long startTime = System.currentTimeMillis();
			String line;
			String parameter = "";
			Integer timeIndex = 0;
			float[] tempData = new float[nodes];
			float[] tempStats = new float[3];
			int countNodes = 0;
			boolean blankLine = false;
			boolean header = true;
			try (BufferedReader br = new BufferedReader(new FileReader(dataFile))) {
				while ((line = br.readLine()) != null) { // We are reading the entire file
					// Need to skip files that aren't selected times
					if (line.contains("Time =") & line.contains(",yr")) {
						String year = line.substring(line.indexOf(",wk") + 3, line.indexOf(",yr")).trim();
						float time = Math.round(Float.parseFloat(year) * 1000f) / 1000f; // This rounds to 3 decimal
																							// places
						if (selectedTimes.contains(time))
							timeIndex = selectedTimes.indexOf(time); // Parse value into float
						else {
							System.out.println(" skipped (not selected)");
							break; // Stop reading this file and move to the next
						}
					}
					// We are assuming this is always the last line of the header, good until proven
					// otherwise
					else if (line.contains("Number of Vertices"))
						header = false;
					// These are the criteria to isolate the parameter text above blocks
					else if (!header && blankLine) {
						// STOMP orders values differently and needs to be reordered into ijk
						tempData = reorderStomp(tempData);
						// Save the stored values for the selected parameter
						if (selectedParameters.contains(parameter) && countNodes > 0) { // Make sure we actually have
																						// data for a selected parameter
							if (!dataMap.get(scenario).containsKey(parameter)) { // If data doesn't yet exist for the
																					// parameter
								dataMap.get(scenario).put(parameter, new float[selectedTimes.size()][nodes]);
								statistics.get(scenario).put(parameter, new float[3]);
							}
							dataMap.get(scenario).get(parameter)[timeIndex] = tempData;
							if (tempStats[0] < statistics.get(scenario).get(parameter)[0])
								statistics.get(scenario).get(parameter)[0] = tempStats[0]; // Min
							statistics.get(scenario).get(parameter)[1] += tempStats[1] / selectedTimes.size(); // Avg
							if (tempStats[2] > statistics.get(scenario).get(parameter)[2])
								statistics.get(scenario).get(parameter)[2] = tempStats[2]; // Max
						}
						// Save porosity values
						else if (parameter.equals("porosity") && countNodes > 0) { // Make sure we actually have data
																					// for porosity
							porosity = tempData;
						}
						// Reset for the new parameter
						tempData = new float[nodes]; // Reset these for the next parameter
						tempStats = new float[3];
						countNodes = 0;
						parameter = line.split(",")[0].trim().replaceAll("\\(", "_").replaceAll("\\)", ""); // New clean
																											// parameter
						parameter = parameter.substring(commonStartCount, parameter.length() - commonEndCount);
						if (parameter.toLowerCase().contains("porosity"))
							parameter = "porosity"; // Override if porosity
						// These are the criteria to isolate the data
					} else if (!header && !line.equals("")
							&& (selectedParameters.contains(parameter) || parameter.equals("porosity"))) {
						String[] tokens = line.trim().split("\\s+"); // Split the line by any number of spaces between
																		// values
						for (String token : tokens) { // Loop through the tokens
							Float value = null;
							try {
								value = Float.parseFloat(token); // Parse value into float
								tempData[countNodes] = value; // Save the value
								if (value < tempStats[0])
									tempStats[0] = value; // Min
								tempStats[1] += value / nodes / selectedTimes.size(); // Avg
								if (value > tempStats[2])
									tempStats[2] = value; // Max
								countNodes++;
							} catch (Exception e) {
								System.out.println("Error parsing the " + parameter + " value: " + token);
							}
						}
					}
					if (line.isEmpty())
						blankLine = true;
					else
						blankLine = false;
				}
			} catch (IOException e) {
				System.out.println(" error reading the file");
				e.printStackTrace();
			}
			long endTime = (System.currentTimeMillis() - startTime) / 1000;
			if (!parameter.equals(""))
				System.out.println(" took " + Constants.formatSeconds(endTime));
		}
	}

	// STOMP orders values differently and needs to be reordered into an ijk index
	public float[] reorderStomp(float[] original) {
		float[] replacement = new float[original.length];
		int counter = 0;
		for (int i = 0; i < x.size(); i++) {
			for (int j = 0; j < y.size(); j++) {
				for (int k = 0; k < z.size(); k++) {
					int nodeNumber = k * x.size() * y.size() + j * x.size() + i;
					replacement[counter] = original[nodeNumber];
					counter++;
				}
			}
		}
		return replacement;
	}

	// Extracting scenarios, times, parameters, and xyz from the first NUFT file
	public void extractNuftStructure(File directory) {
		FileFilter fileFilter = new WildcardFileFilter("*.ntab"); // Ignore any files in the directory that aren't NUFT
																	// files
		// A quick check that we actually have NUFT files in the directory
		if (directory.listFiles(fileFilter).length == 0) {
			System.out.println("No NUFT files were found in the selected directory.");
			return;
		}
		// Add all the scenarios and parameters from file names
		for (File subFile : directory.listFiles(fileFilter)) {
			String scenario = "Scenario" + subFile.getName().split("\\.")[0].replaceAll("\\D+", "");
			if (!scenarios.contains(scenario))
				scenarios.add(scenario);
			String parameter = subFile.getName().replace(".ntab", "").replaceAll("\\d+", "").replaceAll("\\.", "_");
			if (!parameters.contains(parameter))
				parameters.add(parameter);
		}
		// Read the first file to get the times and xyz
		File firstFile = directory.listFiles(fileFilter)[0];
		String line;
		try (BufferedReader br = new BufferedReader(new FileReader(firstFile))) {
			while ((line = br.readLine()) != null) { // We actually have to read the whole file... times are scattered
														// throughout
				// index i j k element_ref nuft_ind x y z dx dy dz volume [times]
				String[] tokens = line.split("\\s+"); // The line is space delimited
				if (line.startsWith("index")) { // The header lists all the times
					// Create a map for each column to a value
					for (String token : tokens) {
						indexMap.add(token);
						if (token.equalsIgnoreCase("volume")) {
							indexMap.add("data");
							break; // Volume is the last index before times
						}
					}
					// Now we add the time
					for (int i = indexMap.indexOf("data"); i < tokens.length; i++) {
						String temp = tokens[i];
						if (temp.contains("y")) {
							units.put("times", "Years");
						}
						String token = tokens[i].replaceAll("[^0-9.]", ""); // Replace letters
						times.add(Float.parseFloat(token));
					}
				} else { // Break when we finish reading the header
					float xValue = Float.parseFloat(tokens[indexMap.indexOf("x")]);
					float yValue = Float.parseFloat(tokens[indexMap.indexOf("y")]);
					float zValue = Float.parseFloat(tokens[indexMap.indexOf("z")]);
					if (!x.contains(xValue))
						x.add(xValue);
					if (!y.contains(yValue))
						y.add(yValue);
					if (!z.contains(zValue))
						z.add(zValue);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		nodes = x.size() * y.size() * z.size();
		// Provided values are at the nodes (center) of each cell
		vertexX = calculateEdges(x);
		vertexY = calculateEdges(y);
		vertexZ = calculateEdges(z);
		cleanParameters();
	}

	// Extracting data, statistics, and porosity from a list of NUFT files
	public void extractNuftData(File directory, String scenarioThread) {
		FileFilter fileFilter = new WildcardFileFilter("*.ntab"); // Ignore any files in the directory that aren't NUFT
																	// files
		for (File subFile : directory.listFiles(fileFilter)) {
			String scenario = "Scenario" + subFile.getName().split("\\.")[0].replaceAll("\\D+", "");
			if (!scenarioThread.equals(scenario))
				continue; // Skip all but the scenario assigned to this thread
			if (!dataMap.containsKey(scenario)) {
				dataMap.put(scenario, new HashMap<String, float[][]>()); // Initialize dataMap for this scenario
				statistics.put(scenario, new HashMap<String, float[]>()); // Initialize statistics for this scenario
			}
			String parameter = subFile.getName().replace(".ntab", "").replaceAll("\\d+", "").replaceAll("\\.", "_");
			parameter = parameter.substring(commonStartCount, parameter.length() - commonEndCount);
			if (!selectedParameters.contains(parameter))
				continue; // Skip parameters that weren't selected
			long startTime = System.currentTimeMillis();
			float[][] tempData = new float[selectedTimes.size()][nodes];
			float[] tempStats = new float[3];
			String line;
			try (BufferedReader br = new BufferedReader(new FileReader(subFile))) {
				while ((line = br.readLine()) != null) { // Read each line
					// index i j k element_ref nuft_ind x y z dx dy dz volume [times]
					String[] tokens = line.split("\\s+"); // The line is space delimited
					if (!tokens[0].equalsIgnoreCase("index")) { // Ignore the header
						int i = Integer.parseInt(tokens[indexMap.indexOf("i")]) - 1;
						int j = Integer.parseInt(tokens[indexMap.indexOf("j")]) - 1;
						int k = Integer.parseInt(tokens[indexMap.indexOf("k")]) - 1;
						int index = i * y.size() * z.size() + j * z.size() + k;
						// int index = Integer.parseInt(tokens[indexMap.indexOf("index")]) - 1;
						for (int ii = indexMap.indexOf("data"); ii < tokens.length; ii++) { // Only read data
							float time = times.get(ii - indexMap.indexOf("data"));
							if (!selectedTimes.contains(time))
								continue; // Skip times that weren't selected
							float value = Float.parseFloat(tokens[ii]);
							tempData[selectedTimes.indexOf(time)][index] = value;
							if (value < tempStats[0])
								tempStats[0] = value; // Min
							tempStats[1] += value / nodes / selectedTimes.size(); // Avg
							if (value > tempStats[2])
								tempStats[2] = value; // Max
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (parameter.toLowerCase().contains("porosity")) {
				porosity = tempData[0];
			} else {
				dataMap.get(scenario).put(parameter, tempData);
				statistics.get(scenario).put(parameter, tempStats);
			}
			long endTime = (System.currentTimeMillis() - startTime) / 1000;
			System.out.println("    Reading " + subFile.getName() + "... took " + Constants.formatSeconds(endTime));
		}
	}

	// Extracting scenarios, times, parameters, and xyz from the first TOUGH file
	public void extractToughStructure(final File parentDirectory) throws FileNotFoundException, IOException {
		File dir = null;
		double xMax = 0;
		double yMax = 0;
		double zMax = 0;
		double xMin = 0;
		double yMin = 0;
		double zMin = 0;
		// The out files don't provide column headers, so we need to read them from a
		// separate file
		// Assumes all the files have the same column indices
		File[] fileList = parentDirectory.listFiles((d, name) -> name.endsWith(".map"));
		if (fileList.length == 1) {
			String line;
			try (BufferedReader br = new BufferedReader(new FileReader(fileList[0]))) {
				while ((line = br.readLine()) != null) { // We actually have to read the whole file... times are
															// scattered throughout
					String[] tokens = line.split("\\s+"); // The line is space delimited
					String parameter = tokens[1].trim().toLowerCase();
					if (tokens[1].contains("(")) {
						parameter = parameter.split("\\(")[0].trim();
						String unit = tokens[1].substring(tokens[1].lastIndexOf("(") + 1, tokens[1].lastIndexOf(")"));
						units.put(parameter, unit);
					}
					indexMap.add(parameter);
					if (!parameter.equals("x") && !parameter.equals("y") && !parameter.equals("z")
							&& !parameter.contains("porosity")) {
						parameters.add(parameter);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		FileFilter fileFilter = new WildcardFileFilter("*.OUT"); // Ignore any files in the directory that aren't TOUGH
																	// // files
		boolean doOnce = true;
		// Loop through the list of directories in the parent folder
		for (File directory : parentDirectory.listFiles()) {
			if (!directory.isDirectory())
				continue; // We only want folders - skip files
			// Add all the scenarios from folder names
			String scenarioName = directory.getName();
			if (!scenarios.contains(scenarioName))
				scenarios.add(scenarioName);
			// A quick check that we actually have TOUGH files in the directory
			if (directory.listFiles().length == 0) {
				System.out.println("No TOUGH files were found in the selected directory.");
				return;
			}
			// Get the times from the file names in the first directory
			if (doOnce) {
				for (File subfile : directory.listFiles(fileFilter)) {
					String[] t = subfile.getName().split("\\.")[0].split("_");
					String time = t[t.length - 1].replaceAll("\\D+", "");
					times.add(Float.parseFloat(time));
				}
				doOnce = false;
			}
			// Read the first file to the min and max x/y/z values.
			File firstFile = directory.listFiles(fileFilter)[0];
			dir = directory;
			String line;
			try (BufferedReader br = new BufferedReader(new FileReader(firstFile))) {
				while ((line = br.readLine()) != null) { // We actually have to read the whole file... parameters are
															// scattered throughout
					// index is pulled from the structure map earlier
					String[] tokens = line.trim().split("\\s+"); // The line is space delimited
					// This part of the code should read every line.
					for (String parameter : indexMap.subList(0, 3)) {
						float value = Float.parseFloat(tokens[indexMap.indexOf(parameter)]);
						if (parameter.equals("x")) {
							if (value < xMin) {
								xMin = value;
							} else {
								xMax = value;
							}
						} else if (parameter.equals("y")) {
							if (value < yMin) {
								yMin = value;
							} else {
								yMax = value;
							}
						} else {
							if (value < zMin) {
								zMin = value;
							} else {
								zMax = value;
							}
						}
//						if(parameter.equals("x") && !x.contains(value))
//							x.add(value);
//						else if(parameter.equals("y") && !y.contains(value))
//							y.add(value);
//						else if(parameter.equals("z") && !z.contains(value))
//							z.add(value);
					}
//					//TODO: temporary for testing
//					if(x.size()>20 && y.size()>20 && z.size()>20);
//						break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		double intervalX = (xMax + (Math.abs(xMin))) / 50;
		double intervalY = (yMax + (Math.abs(yMin))) / 50;
		// Just going to use this grid.
		double[] xMinValues = new double[50];
		double[] yMinValues = new double[50];
		for (int i = 0; i < 50; i++) {
			xMinValues[i] = xMin;
			xMin += intervalX;
			yMinValues[i] = yMin;
			yMin += intervalY;
		}
		nodes = x.size() * y.size() * z.size();
		// Provided values are at the nodes (center) of each cell
		vertexX = calculateEdges(x);
		vertexY = calculateEdges(y);
		vertexZ = calculateEdges(z);
		cleanParameters();
		File[] listOfFiles = dir.listFiles(fileFilter);
		interpolateDataToMap(listOfFiles, xMinValues, yMinValues);
	}

	private void interpolateDataToMap(final File[] theListOfFiles, double[] xMins, final double[] yMins)
			throws FileNotFoundException, IOException {

		List<Map<pointDouble, double[]>> fileData = new ArrayList<Map<pointDouble, double[]>>();
		Map<pointDouble, double[]> gridPoints = new HashMap<pointDouble, double[]>();
		String line;
		int arrSize = 0;
		double keyX = 0;
		double keyY = 0;
		for (int i = 0; i < xMins.length; i++) {
			for (int j = 0; j < yMins.length; j++) {
				gridPoints.put(new pointDouble(xMins[i], yMins[j]), new double[22]);
			}
		}
		for (File f : theListOfFiles) {
			try (BufferedReader br = new BufferedReader(new FileReader(f))) {
				while ((line = br.readLine()) != null) {
					int counter = 3;
					String[] tokens = line.trim().split("\\s+");
					keyX = getKeyInInterval(Double.parseDouble(tokens[0]), xMins);
					keyY = getKeyInInterval(Double.parseDouble(tokens[1]), yMins);
					pointDouble thePoint = null;
					for (int i = 3; i < (tokens.length - 2) * 2; i += 2) {
						// get the interval where the key should be.
						for (pointDouble key: gridPoints.keySet()) {
							if (key.contains(keyX, keyY)) {
								thePoint = key;
								break;
							}
						}
						gridPoints.get(thePoint)[i - 3]++;
						gridPoints.get(thePoint)[i - 2] += Double.parseDouble(tokens[counter]);
						counter++;
					}
				}
			}
			arrSize = 22;
			fileData.add(gridPoints);
		}
		averageValue(fileData, arrSize);
	}

	private List<Map<pointDouble, double[]>> averageValue(List<Map<pointDouble, double[]>> valsList, final int size) {
		for (Map<pointDouble, double[]> map : valsList) {
			for (pointDouble key : map.keySet()) {
				for (int i = 0; i < size; i += 2) {
					map.get(key)[i + 1] = map.get(key)[i + 1] / map.get(key)[i];
				}
			}
		}
		return valsList;
	}

	private double getKeyInInterval(final double theValue, double[] keyList) {
		double minDiff = Double.MAX_VALUE;
		double nearest = 0;
		for (double key : keyList) {
			double diff = Math.abs(theValue - key);
			if (diff < minDiff) {
				nearest = key;
				minDiff = diff;
			}
		}
		return nearest;
	}

	// Extracting data, statistics, and porosity from a list of TOUGH directories
	public void extractToughData(File directory) {
		FileFilter fileFilter = new WildcardFileFilter("*.OUT"); // Ignore any files in the directory that aren't TOUGH
																	// files
		String scenario = directory.getName();
		if (!selectedScenarios.contains(scenario))
			return; // Make sure this is a selected scenario
		porosity = new float[nodes];
		dataMap.put(scenario, new HashMap<String, float[][]>()); // Initialize dataMap for this scenario
		statistics.put(scenario, new HashMap<String, float[]>()); // Initialize statistics for this scenario
		for (String parameter : selectedParameters) {
			dataMap.get(scenario).put(parameter, new float[selectedTimes.size()][nodes]);
			statistics.get(scenario).put(parameter, new float[3]);
		}
		System.out.println("Reading variables: " + selectedParameters.toString());
		// Loop through the list of files in each directory
		for (File dataFile : directory.listFiles(fileFilter)) {
			// Verify that the file represents a selected time step
			String[] t = dataFile.getName().split("\\.")[0].split("_");
			Float time = Float.parseFloat(t[t.length - 1].replaceAll("\\D+", ""));
			if (!selectedTimes.contains(time))
				continue;
			System.out.print("    Reading " + scenario + "/" + dataFile.getName() + "...");
			long startTime = System.currentTimeMillis();
			String line;
			try (BufferedReader br = new BufferedReader(new FileReader(dataFile))) {
				while ((line = br.readLine()) != null) { // We are reading the entire file
					// index is pulled from the structure map earlier
					String[] tokens = line.trim().split("\\s+"); // The line is space delimited
					int i = x.indexOf(Float.parseFloat(tokens[0])); // Assuming i comes first
					int j = y.indexOf(Float.parseFloat(tokens[1])); // Assuming j comes second
					int k = z.indexOf(Float.parseFloat(tokens[2])); // Assuming k comes third
					int index = i * y.size() * z.size() + j * z.size() + k;
					// i and k does not contain the parsed values.
					for (String parameter : indexMap.subList(3, indexMap.size())) {
						float value = Float.parseFloat(tokens[indexMap.indexOf(parameter)]);
						if (dataMap.get(scenario).containsKey(parameter)) {
							System.out.println(line);
//							System.out.println("Scenario: " + scenario + " Parameter: " + parameter + " Time: " + time + " index: " + index);
							dataMap.get(scenario).get(parameter)[selectedTimes.indexOf(time)][index] = value;
							// [min, avg, max]
							if (value < statistics.get(scenario).get(parameter)[0]) // Min
								statistics.get(scenario).get(parameter)[0] = value;
							else if (value > statistics.get(scenario).get(parameter)[2]) // Max
								statistics.get(scenario).get(parameter)[2] = value;
							statistics.get(scenario).get(parameter)[1] += value / nodes / selectedTimes.size(); // Avg
						} else if (parameter.contains("porosity")) {
							porosity[index] = value;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			long endTime = (System.currentTimeMillis() - startTime) / 1000;
			System.out.println("    Reading " + dataFile.getName() + "... took " + Constants.formatSeconds(endTime));
		}
	}

	// Extracting scenarios, times, parameters, and xyz from the first Tecplot file
	public void extractTecplotStructure(File directory) {
		FileFilter fileFilter = new WildcardFileFilter("*.dat"); // Ignore any files in the directory that aren't
																	// Tecplot files
		// A quick check that we actually have Tecplot files in the directory
		if (directory.listFiles(fileFilter).length == 0) {
			System.out.println("No Tecplot files were found in the selected directory.");
			return;
		}
		// Add all the scenarios from file names
		for (File subFile : directory.listFiles(fileFilter)) {
			String scenario = subFile.getName().split("\\.")[0];
			if (!scenarios.contains(scenario))
				scenarios.add(scenario);
		}
		// Read the first file to get the times, parameters, and xyz
		File firstFile = directory.listFiles(fileFilter)[0];
		try (BufferedReader br = new BufferedReader(new FileReader(firstFile))) {
			String line;
			String key = "";
			int index = 0;
			int countNodes = 0;
			while ((line = br.readLine()) != null) { // We actually have to read the whole file... times are scattered
														// throughout
				// Add the parameters and units while generating an indexMap
				if (line.contains("VARIABLES")) {
					String[] tokens = line.split("\""); // Split by quotes to isolate parameter and unit
					for (String token : tokens) {
						if (token.contains("=") || token.trim().equals(""))
							continue; // Skip everything but the parameters
						String parameter = token.split(",")[0].trim().toLowerCase();
						if (parameter.toLowerCase().contains("porosity"))
							parameter = "porosity";
						indexMap.add(parameter); // Store the order which parameters are provided
						if (!parameters.contains(parameter) && !parameter.equals("x") && !parameter.equals("y")
								&& !parameter.equals("z") && !parameter.equals("volume")
								&& !parameter.equals("porosity")) // Skip these
							parameters.add(parameter); // Save parameters
						if (token.contains(",")) { // This means they give units
							String unit = token.split(",")[1].trim();
							units.put(parameter, unit); // Save units
						}
					}
				}
				// Add the times from this line, scattered through file
				else if (line.contains("ZONE")) { // This lists the zone name, which includes the timestep
					String[] tokens = line.split("\""); // Zone name is wrapped in quotes
					String time = tokens[1].replaceAll("[^0-9.]", "");
					try {
						times.add(Float.parseFloat(time)); // Parse value into float
					} catch (Exception e) {
						System.out.println("Years Error: " + time);
					}
					// Also add the number of nodes, as this will be used to count off blocks
					tokens = line.split("[ ,]"); // Header info is comma delimited
					for (int i = 0; i < tokens.length; i++) {
						try {
							if (tokens[i].equals("NODES")) {
								nodes = Integer.parseInt(tokens[i + 2]);
							} else if (tokens[i].equals("ELEMENTS")) {
								elements = Integer.parseInt(tokens[i + 2]);
							}
						} catch (Exception theException) {
							System.out.println("NODE/ELEMENTS: " + tokens[i]);
						}
					}
				}
				// This lists the data, from which we will get xyz (which are actually at edges)
				else if (!line.contains("=")
						&& (key.equals("") || key.equals("x") || key.equals("y") || key.equals("z"))) { // Limit to the
																										// keys we want
					key = indexMap.get(index);
					String[] tokens = line.split("\\s+"); // Space delimited
					for (String token : tokens) {
						countNodes++;
						float value = Float.parseFloat(token);
						if (key.equals("x") & !vertexX.contains(value))
							vertexX.add(value);
						else if (key.equals("y") & !vertexY.contains(value))
							vertexY.add(value);
						else if (key.equals("z") & !vertexZ.contains(value))
							vertexZ.add(value);
					}
					// When the counter is high enough, we have finished with the parameter
					if (countNodes >= nodes) {
						index++; // On to the next parameter
						countNodes = 0; // Reset the counter
					}
				}
			}
			System.out.println(units.toString() + " These are the units");
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Provided values are at the nodes (center) of each cell
		x = calculateCenters(vertexX);
		y = calculateCenters(vertexY);
		z = calculateCenters(vertexZ);
	}

	// TODO: Tecplot Data here...
	// Extracting data, statistics, and porosity from a list of Tecplot files
	public void extractTecplotData(File subFile) {
		String scenario = subFile.getName().split("\\.")[0];
		dataMap.put(scenario, new HashMap<String, float[][]>()); // Initialize dataMap for this scenario
		statistics.put(scenario, new HashMap<String, float[]>()); // Initialize statistics for this scenario
		System.out.print("Reading " + subFile.getName() + "...");
		long startTime = System.currentTimeMillis();
		int index = 0;
		String line;
		String parameter = indexMap.get(index);
		int timeIndex = 0;
		float[] tempData = new float[elements];
		float[] tempStats = new float[3];
		boolean nextHeader = false;
		int countElements = 0;
		int countNodes = 0;
		boolean skip = false;
		try (BufferedReader br = new BufferedReader(new FileReader(subFile))) {
			while ((line = br.readLine()) != null) {
				// Get the time index from the header
				if (line.contains("ZONE") && !nextHeader) {
					nextHeader = false;// This lists the zone name, which includes the timestep
					String[] tokens = line.split("\""); // Zone name is wrapped in quotes
					float time = Float.parseFloat(tokens[1].replaceAll("[^0-9.]", ""));
					if (!selectedTimes.contains(time)) {
						skip = true; // This means we don't want the timestep - skip it until we check at the next
										// timestep
					} else {
						timeIndex = selectedTimes.indexOf(time); // Parse value into float
						skip = false;
					}

				}
				// This is the data - we want all selected parameters and porosity
				if (!line.contains("=") && !line.trim().isEmpty() && !skip && !nextHeader) {
					String[] tokens = line.split("\\s+"); // Space delimited
					// Count the numbers so we know when we finish the block - x, y, z based on node
					// count
					if (parameter.equals("x") || parameter.equals("y") || parameter.equals("z")) {
						countNodes += tokens.length;
						// Read in data for all selected parameters and porosity, saving statistics as
						// we go
						// Any parameters that weren't selected are skipped, only the first porosity is
						// stored (special handling)
					} else if (selectedParameters.contains(parameter)
							|| (parameter.toLowerCase().contains("porosity") && porosity == null)) {
						for (String token : tokens) {
							float value = Float.parseFloat(token);
							tempData[countElements] = value;
							if (value < tempStats[0])
								tempStats[0] = value; // Min
							tempStats[1] += value / nodes; // Avg for timestep
							if (value > tempStats[2])
								tempStats[2] = value; // Max
							countElements++;
						}
						// Count the numbers so we know when we finish the block - parameters based on
						// elements count
					} else { // Unselected parameters
						countElements += tokens.length;
					}
					// When the counter is high enough, we have finished with the parameter and
					// should save
					if (countNodes >= nodes || countElements >= elements) {
						// Tecplot orders values differently and needs to be reordered into ijk
						tempData = reorderStomp(tempData);
						if (selectedParameters.contains(parameter)) { // Make sure we are looking at a selected
																		// parameter
							if (!dataMap.get(scenario).containsKey(parameter)) { // If data doesn't yet exist for the
																					// parameter
								dataMap.get(scenario).put(parameter, new float[selectedTimes.size()][elements]);
								statistics.get(scenario).put(parameter, new float[3]);
							}
							dataMap.get(scenario).get(parameter)[timeIndex] = tempData;
							if (tempStats[0] < statistics.get(scenario).get(parameter)[0])
								statistics.get(scenario).get(parameter)[0] = tempStats[0]; // Min
							statistics.get(scenario).get(parameter)[1] += tempStats[1] / selectedTimes.size(); // Avg
							if (tempStats[2] > statistics.get(scenario).get(parameter)[2])
								statistics.get(scenario).get(parameter)[2] = tempStats[2]; // Max
						}
						// Save porosity values
						else if (parameter.toLowerCase().contains("porosity") && porosity == null) { // Make sure we are
																										// looking at
																										// porosity
							porosity = tempData;
						}
						tempData = new float[elements]; // Reset these for the next parameter
						tempStats = new float[3];
						countNodes = 0; // Reset the counter
						countElements = 0; // Reset the counter
						System.out.print(parameter + " ");
						if (index < indexMap.size() - 1)
							index++;
						else if (index == indexMap.size() - 1 && !line.toLowerCase().contains("zone")) {
							nextHeader = true;
						} else
							index = 3; // Index of the first parameter that is not x, y, z
						parameter = indexMap.get(index);
					}
				}
			}
			long endTime = (System.currentTimeMillis() - startTime) / 1000;
			System.out.println(" took " + Constants.formatSeconds(endTime));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static ArrayList<Float> calculateEdges(ArrayList<Float> cellCenters) {
		ArrayList<Float> cellEdges = new ArrayList<Float>();
		for (int i = 1; i < cellCenters.size(); i++) {
			float half = (cellCenters.get(i) - cellCenters.get(i - 1)) / 2;
			if (i == 1)
				cellEdges.add(new Float(cellCenters.get(i - 1) - half).floatValue());
			cellEdges.add(new Float(cellCenters.get(i - 1) + half).floatValue());
			if (i == cellCenters.size() - 1)
				cellEdges.add(new Float(cellCenters.get(i) + half).floatValue());
		}
		return cellEdges;
	}

	private static ArrayList<Float> calculateCenters(ArrayList<Float> cellEdges) {
		ArrayList<Float> cellCenters = new ArrayList<Float>();
		for (int i = 1; i < cellEdges.size(); i++) {
			cellCenters.add(cellEdges.get(i) - (cellEdges.get(i) - cellEdges.get(i - 1)) / 2);
		}
		return cellCenters;
	}

	// A little script to remove common text beginning or trailing all parameter
	// names
	private void cleanParameters() {
		for (int i = 1; i < parameters.size(); i++) {
			char[] first = parameters.get(i - 1).toLowerCase().toCharArray();
			char[] second = parameters.get(i).toLowerCase().toCharArray();
			int minLength = Math.min(first.length, second.length); // So we don't exceed the array length
			int startCount = 0;
			int endCount = 0;
			// Finding the number of starting characters in common
			for (int j = 0; j < minLength; j++) {
				if (first[j] == second[j])
					startCount = j + 1;
				else
					break;
			}
			// Finding the number of ending characters in common
			for (int j = 1; j < minLength; j++) {
				if (first[first.length - j] == second[second.length - j])
					endCount = j;
				else
					break;
			}
			if (startCount < commonStartCount)
				commonStartCount = startCount;
			if (endCount < commonEndCount)
				commonEndCount = endCount;
		}
		// Now remove the common start and end from all parameters
		for (int i = 0; i < parameters.size(); i++) {
			String replacement = parameters.get(i).substring(commonStartCount,
					parameters.get(i).length() - commonEndCount);
			parameters.set(i, replacement);
		}
	}

	/**
	 * **\ Getters & Setters * * \*
	 */

	public String[] getScenarios() {
		return scenarios.toArray(new String[scenarios.size()]);
	}

	public Float[] getTimes() {
		return times.toArray(new Float[times.size()]);
	}

	public String[] getParameters() {
		return parameters.toArray(new String[parameters.size()]);
	}

	public void setSelected(JCheckBox[] listScenarios, JCheckBox[] listTimes, JCheckBox[] listParameters) {
		selectedScenarios.clear();
		selectedTimes.clear();
		selectedParameters.clear();
		for (JCheckBox scenario : listScenarios) {
			if (scenario.isSelected())
				selectedScenarios.add(scenario.getText());
		}
		for (JCheckBox time : listTimes) {
			if (time.isSelected())
				selectedTimes.add(Float.parseFloat(time.getText()));
		}
		for (JCheckBox parameter : listParameters) {
			if (parameter.isSelected())
				selectedParameters.add(parameter.getText());
		}
	}

	public ArrayList<String> getSelectedScenarios() {
		return selectedScenarios;
	}

	public ArrayList<Float> getSelectedTimes() {
		return selectedTimes;
	}

	public Float[] getSelectedTimesArray() {
		return selectedTimes.toArray(new Float[selectedTimes.size()]);
	}

	public int getSelectedTimeIndex(float time) {
		return selectedTimes.indexOf(time);
	}

	public ArrayList<String> getSelectedParameters() {
		return selectedParameters;
	}

	public Float[] getX() {
		return x.toArray(new Float[x.size()]);
	}

	public Float[] getY() {
		return y.toArray(new Float[y.size()]);
	}

	public Float[] getZ() {
		return z.toArray(new Float[z.size()]);
	}

	public Float[] getVertexX() {
		return vertexX.toArray(new Float[vertexX.size()]);
	}

	public Float[] getVertexY() {
		return vertexY.toArray(new Float[vertexY.size()]);
	}

	public Float[] getVertexZ() {
		return vertexZ.toArray(new Float[vertexZ.size()]);
	}

	public float[] getPorosity() {
		return porosity;
	}

	public void setPorosity(float[] porosity) {
		this.porosity = porosity;
	}

	public float[][] getData(String scenario, String parameter) {
		if (dataMap.get(scenario).containsKey(parameter))
			return dataMap.get(scenario).get(parameter);
		return null;
	}

	public float[] getStatistics(String scenario, String parameter) {
		if (statistics.get(scenario).containsKey(parameter))
			return statistics.get(scenario).get(parameter);
		return null;
	}

	public String getUnit(String parameter) {
		if (units.containsKey(parameter))
			return units.get(parameter);
		return "";
	}

	public void setUnit(final String theParameter, final String theValue) {
		units.put(theParameter, theValue);
	}

	public void setZOrientation(final String positiveDirection) {
		units.put(POSITIVE, positiveDirection);
	}

	public Float[] ZOrientationArray() {
		Float[] temp = { (float) -1 };
		return temp;
	}

	public String getZOrientation() {
		return units.get(POSITIVE);
	}
}
