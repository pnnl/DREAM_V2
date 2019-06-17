package hdf5Tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;

import org.apache.commons.io.filefilter.WildcardFileFilter;


/**
 * @brief  Reads a variety of input files to convert into DREAM-compatible Hdf5 files
 * @author Jonathan Whiting
 * @date   February 18, 2019
 */
public class ParseRawFiles {
	
	private ArrayList<String> scenarios;
	private ArrayList<Float> times;
	private ArrayList<String> parameters;
	
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
	private Map<String, Map<String, float[][]>> dataMap; //scenario <parameter, float[time][nodes]>
	private Map<String, Map<String, float[]>> statistics; //scenario, parameter, float[min, avg, max]
	private Map<String, String> units; //parameter, units
	
	// Only used by STOMP
	private boolean nodal; //Determine whether parameters are given for nodes or vertices
	// Only used by NUFT and Tecplot
	private ArrayList<String> indexMap; //Maps parameters to columns or blocks
	// Only used by Tecplot
	private int elements;
	
	// Initialize variables
	public ParseRawFiles() {
		scenarios = new ArrayList<String>();
		times = new ArrayList<Float>();
		parameters = new ArrayList<String>();
		
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
		indexMap = new ArrayList<String>();
	}
	
	
	// Extracting scenarios, times, parameters, and xyz from a list of STOMP directories
	public void extractStompStructure(File parentDirectory) {
		// Loop through the list of directories in the parent folder
		for(File directory: parentDirectory.listFiles()) {
			if(!directory.isDirectory()) continue; //We only want folders - skip files
			// Add all the scenarios from folder names
			String scenarioName = directory.getName();
			if(!scenarios.contains(scenarioName)) scenarios.add(scenarioName);
			// A quick check that we actually have STOMP files in the directory
			if(directory.listFiles().length==0) {
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
				while ((line = br.readLine()) != null) { //We actually have to read the whole file... parameters are scattered throughout
					// We are assuming this is always the last line of the header, good until proven otherwise
					if(line.contains("Number of Vertices"))
						header = false;
					//These are the criteria to isolate the parameter text above blocks
					else if(!header && blankLine) {
						parameter = line.split(",")[0].trim().replaceAll("\\(", "_").replaceAll("\\)", ""); //New clean parameter
						// If it has Nodal/Node in the name, it is giving values for edges
						if(parameter.contains("Nodal") || line.contains("Node"))
							nodal = true;
						// We want to normalize the x, y, z
						if(parameter.startsWith("X or R-Direction Node Positions") ||
								parameter.startsWith("Radial-Direction Node Positions") ||
								parameter.startsWith("X-Direction Surface Positions") ||
								parameter.startsWith("X-Direction Node Positions") ||
								parameter.startsWith("X-Direction Nodal Vertices")) {
							parameter = "x";
						} else if(parameter.startsWith("Y or Theta-Direction Node Positions") ||
									parameter.startsWith("Theta-Direction Node Positions") ||
									parameter.startsWith("Y-Direction Surface Positions") ||
									parameter.startsWith("Y-Direction Node Positions") ||
									parameter.startsWith("Y-Direction Nodal Vertices")) {
							parameter = "y";
						} else if(parameter.startsWith("Z-Direction Surface Positions") ||
								parameter.startsWith("Z-Direction Node Positions") ||
								parameter.startsWith("Z-Direction Nodal Vertices")) {
							parameter = "z";
						// Store a list of parameters
						} else if(!parameters.contains(parameter) && !parameter.equals("Node Volume") && !parameter.equals("Node Map") && !parameter.toLowerCase().contains("porosity")) //Skip these
							parameters.add(parameter);
						// Save units for all parameters if they are available
						if(line.contains(",") && !line.contains("null")) { //This means they give units
							String unit = line.split(",")[1].trim();
							units.put(parameter, unit); //Save units
						} 
					// These are the criteria to isolate the xyz values
					} else if(!header && !line.equals("") && (parameter.equals("x") || parameter.equals("y") || parameter.equals("z") || parameter.equals("porosity"))) {
						String[] tokens = line.trim().split("\\s+"); //Split the line by any number of spaces between values
						for(String token: tokens) { //Loop through the tokens
							Float value = null;
							try {
								value = Float.parseFloat(token); //Parse value into float
								
								// Provided values are at the edge of each cell
								if(nodal) {
									if(parameter.equals("x") && !vertexX.contains(value))
										vertexX.add(value); //Store x values
									else if(parameter.equals("y") && !vertexY.contains(value))
										vertexY.add(value); //Store y values
									else if(parameter.equals("z") && !vertexZ.contains(value))
										vertexZ.add(value); //Store z values
								}
								// Provided values are at the center of each cell
								else {
									if(parameter.equals("x") && !x.contains(value))
										x.add(value); //Store x values
									else if(parameter.equals("y") && !y.contains(value))
										y.add(value); //Store y values
									else if(parameter.equals("z") && !z.contains(value))
										z.add(value); //Store z values
								}
							} catch (Exception e) {
								System.out.println("Error parsing the " + parameter + " value: " + token);
							}
						}
					}
					if(line.isEmpty()) blankLine = true;
					else blankLine = false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(nodal) { //Provided values are at the edge of each cell, calculate the center
				x = calculateCenters(vertexX);
				y = calculateCenters(vertexY);
				z = calculateCenters(vertexZ);
			} else { //Provided values are at the center of each cell, calculate the edge
				vertexX = calculateEdges(x);
				vertexY = calculateEdges(y);
				vertexZ = calculateEdges(z);
			}
			nodes = x.size()*y.size()*z.size();
			// Read the header of every file to get the times
			for(File subFile: directory.listFiles()) {
				try (BufferedReader br = new BufferedReader(new FileReader(subFile))) {
					while ((line = br.readLine()) != null) { //We just need to read the header for each file
						if(line.contains("Time =") & line.contains(",yr")) {
							units.put("Time", "Years");
							String year = line.substring(line.indexOf(",wk")+3, line.indexOf(",yr")).trim();
							try {
								Float timeStep = Math.round(Float.parseFloat(year) * 1000f) / 1000f; //This rounds to 3 decimal places
								if(!times.contains(timeStep)) times.add(timeStep);
							} catch (Exception e) {
								System.out.println("Years Error: " + year);
							}
							break; //No need to read the rest of the file
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	// Extracting data, statistics, and porosity from a list of STOMP directories
	public void extractStompData(File directory) {
		String scenario = directory.getName();
		dataMap.put(scenario, new HashMap<String, float[][]>()); //Initialize dataMap for this scenario
		statistics.put(scenario, new HashMap<String, float[]>()); //Initialize statistics for this scenario
		System.out.println("Reading variables: " + selectedParameters.toString());
		// Loop through the list of files in each directory
		for(File dataFile: directory.listFiles()) {
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
				while ((line = br.readLine()) != null) { //We are reading the entire file
					// Need to skip files that aren't selected times
					if(line.contains("Time =") & line.contains(",yr")) {
						String year = line.substring(line.indexOf(",wk")+3, line.indexOf(",yr")).trim();
						float time = Math.round(Float.parseFloat(year) * 1000f) / 1000f; //This rounds to 3 decimal places
						if(selectedTimes.contains(time))
							timeIndex = selectedTimes.indexOf(time); //Parse value into float
						else {
							System.out.println(" skipped (not selected)");
							break; //Stop reading this file and move to the next
						}
					}
					// We are assuming this is always the last line of the header, good until proven otherwise
					else if(line.contains("Number of Vertices"))
						header = false;
					// These are the criteria to isolate the parameter text above blocks
					else if(!header && blankLine) {
						// STOMP orders values differently and needs to be reordered into ijk
						tempData = reorderStomp(tempData);
						// Save the stored values for the selected parameter
						if(selectedParameters.contains(parameter) && countNodes>0) { //Make sure we actually have data for a selected parameter
							if(!dataMap.get(scenario).containsKey(parameter)) { //If data doesn't yet exist for the parameter
								dataMap.get(scenario).put(parameter, new float[selectedTimes.size()][nodes]);
								statistics.get(scenario).put(parameter, new float[3]);
							}
							dataMap.get(scenario).get(parameter)[timeIndex] = tempData;
							if(tempStats[0]<statistics.get(scenario).get(parameter)[0]) statistics.get(scenario).get(parameter)[0] = tempStats[0]; //Min
							statistics.get(scenario).get(parameter)[1] += tempStats[1]/selectedTimes.size(); //Avg
							if(tempStats[2]>statistics.get(scenario).get(parameter)[2]) statistics.get(scenario).get(parameter)[2] = tempStats[2]; //Max
						}
						// Save porosity values
						else if(parameter.equals("porosity") && countNodes>0) { //Make sure we actually have data for porosity
							porosity = tempData;
						}
						// Reset for the new parameter
						tempData = new float[nodes]; //Reset these for the next parameter
						tempStats = new float[3];
						countNodes = 0;
						parameter = line.split(",")[0].trim().replaceAll("\\(", "_").replaceAll("\\)", ""); //New clean parameter
						if(parameter.toLowerCase().contains("porosity")) parameter = "porosity"; //Override if porosity
					// These are the criteria to isolate the data
					} else if(!header && !line.equals("") && (selectedParameters.contains(parameter) || parameter.equals("porosity"))) {
						String[] tokens = line.trim().split("\\s+"); //Split the line by any number of spaces between values
						for(String token: tokens) { //Loop through the tokens
							Float value = null;
							try {
								value = Float.parseFloat(token); //Parse value into float
								tempData[countNodes] = value; //Save the value
								if(value<tempStats[0]) tempStats[0] = value; //Min
								tempStats[1] += value/nodes/selectedTimes.size(); //Avg
								if(value>tempStats[2]) tempStats[2] = value; //Max
								countNodes++;
							} catch (Exception e) {
								System.out.println("Error parsing the " + parameter + " value: " + token);
							}
						}
					}
					if(line.isEmpty()) blankLine = true;
					else blankLine = false;
				}
			} catch (IOException e) {
				System.out.println(" error reading the file");
				e.printStackTrace();
			}
			if(!parameter.equals(""))
				System.out.println(" took " + (System.currentTimeMillis() - startTime) + " ms");
		}
	}
	
	
	// STOMP orders values differently and needs to be reordered into an ijk index
	public float[] reorderStomp(float[] original) {
		float[] replacement = new float[original.length];
		int counter = 0;
		for(int i=0; i<x.size(); i++) {
			for(int j=0; j<y.size(); j++) {
				for(int k=0; k<z.size(); k++) {
					int nodeNumber = k*x.size()*y.size() + j*x.size() + i;
					replacement[counter] = original[nodeNumber];
					counter++;
				}
			}
		}
		return replacement;
	}
	
	
	// Extracting scenarios, times, parameters, and xyz from the first NUFT file
	public void extractNuftStructure(File directory) {
		FileFilter fileFilter = new WildcardFileFilter("*.ntab"); //Ignore any files in the directory that aren't NUFT files
		// A quick check that we actually have NUFT files in the directory
		if(directory.listFiles(fileFilter).length==0) {
			System.out.println("No NUFT files were found in the selected directory.");
			return;
		}
		// Add all the scenarios and parameters from file names
		for(File subFile: directory.listFiles(fileFilter)) {
			String scenario = "Scenario" + subFile.getName().split("\\.")[0].replaceAll("\\D+", "");
			if(!scenarios.contains(scenario)) scenarios.add(scenario);
			String parameter = subFile.getName().replace(".ntab","").replaceAll("\\d+","").replaceAll("\\.","_");
			if(!parameters.contains(parameter)) parameters.add(parameter);
		}
		// Read the first file to get the times and xyz
		File firstFile = directory.listFiles(fileFilter)[0];
		String line;
		try (BufferedReader br = new BufferedReader(new FileReader(firstFile))) {
			while ((line = br.readLine()) != null) { //We actually have to read the whole file... times are scattered throughout
				// index i j k element_ref nuft_ind x y z dx dy dz volume [times]
				String[] tokens = line.split("\\s+"); //The line is space delimited
				if(line.startsWith("index")) { //The header lists all the times
					// Create a map for each column to a value
					for(String token: tokens) {
						indexMap.add(token);
						if(token.equalsIgnoreCase("volume")) {
							indexMap.add("data");
							break; //Volume is the last index before times
						}
					}
					// Now we add the time
					for(int i=indexMap.indexOf("data"); i<tokens.length; i++) {
						String temp = tokens[i];
						if (temp.contains("y")) {
							units.put("Time", "Years");
						}
						String token = tokens[i].replaceAll("\\D+", ""); //Replace letters
						times.add(Float.parseFloat(token));
					}
				} else { //Break when we finish reading the header
					float xValue = Float.parseFloat(tokens[indexMap.indexOf("x")]);
					float yValue = Float.parseFloat(tokens[indexMap.indexOf("y")]);
					float zValue = Float.parseFloat(tokens[indexMap.indexOf("z")]);
					if(!x.contains(xValue)) x.add(xValue);
					if(!y.contains(yValue)) y.add(yValue);
					if(!z.contains(zValue)) z.add(zValue);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		nodes = x.size()*y.size()*z.size();
		//Provided values are at the nodes (center) of each cell
		vertexX = calculateEdges(x);
		vertexY = calculateEdges(y);
		vertexZ = calculateEdges(z);
	}
	
	
	// Extracting data, statistics, and porosity from a list of NUFT files
	public void extractNuftData(File directory, String scenarioThread) {
		FileFilter fileFilter = new WildcardFileFilter("*.ntab"); //Ignore any files in the directory that aren't NUFT files
		for(File subFile: directory.listFiles(fileFilter)) {
			String scenario = "Scenario" + subFile.getName().split("\\.")[0].replaceAll("\\D+", "");
			if(!scenarioThread.equals(scenario)) continue; //Skip all but the scenario assigned to this thread
			if(!dataMap.containsKey(scenario)) {
				dataMap.put(scenario, new HashMap<String, float[][]>()); //Initialize dataMap for this scenario
				statistics.put(scenario, new HashMap<String, float[]>()); //Initialize statistics for this scenario
			}
			String parameter = subFile.getName().replace(".ntab","").replaceAll("\\d+","").replaceAll("\\.","_");
			if(!selectedParameters.contains(parameter)) continue; //Skip parameters that weren't selected
			long startTime = System.currentTimeMillis();
			float[][] tempData = new float[selectedTimes.size()][nodes];
			float[] tempStats = new float[3];
			String line;
			try (BufferedReader br = new BufferedReader(new FileReader(subFile))) {
				while ((line = br.readLine()) != null) { //Read each line
					// index i j k element_ref nuft_ind x y z dx dy dz volume [times]
					String[] tokens = line.split("\\s+"); //The line is space delimited
					if(!tokens[0].equalsIgnoreCase("index")) { //Ignore the header
						int index = Integer.parseInt(tokens[indexMap.indexOf("index")]) - 1;
						for(int i=indexMap.indexOf("data"); i<tokens.length; i++) { //Only read data
							float time = times.get(i-indexMap.indexOf("data"));
							if(!selectedTimes.contains(time)) continue; //Skip times that weren't selected
							float value = Float.parseFloat(tokens[i]);
							tempData[selectedTimes.indexOf(time)][index] = value;
							if(value<tempStats[0]) tempStats[0] = value; //Min
							tempStats[1] += value/nodes/selectedTimes.size(); //Avg
							if(value>tempStats[2]) tempStats[2] = value; //Max
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(parameter.toLowerCase().contains("porosity")) {
				porosity = tempData[0];
			} else {
				dataMap.get(scenario).put(parameter, tempData);
				statistics.get(scenario).put(parameter, tempStats);
			}
			System.out.println("    Reading "+subFile.getName()+"... took "+(System.currentTimeMillis()-startTime)/1000+" s");
		}
	}
	
	
	// NUFT orders values differently and needs to be reordered into an ijk index
	public void orderNuft(String scenario) {
		for(String parameter: dataMap.get(scenario).keySet()) {
			for(int timeIndex=0; timeIndex<dataMap.get(scenario).get(parameter).length; timeIndex++) {
				float[] values = dataMap.get(scenario).get(parameter)[timeIndex];
				float[] temp = new float[values.length];
				int counter = 0;
				for(int i=0; i<x.size(); i++) {
					for(int j=0; j<y.size(); j++) {
						for(int k=0; k<z.size(); k++) {
							int index = i*z.size()*y.size() + j*z.size() + k;
							temp[counter] = values[index];
							counter++;
						}
					}
				}
				dataMap.get(scenario).get(parameter)[timeIndex] = temp;
			}
		}
	}
	
	
	// Extracting scenarios, times, parameters, and xyz from the first Tecplot file
	public void extractTecplotStructure(File directory) {
		FileFilter fileFilter = new WildcardFileFilter("*.dat"); //Ignore any files in the directory that aren't Tecplot files
		// A quick check that we actually have Tecplot files in the directory
		if(directory.listFiles(fileFilter).length==0) {
			System.out.println("No Tecplot files were found in the selected directory.");
			return;
		}
		// Add all the scenarios from file names
		for(File subFile: directory.listFiles(fileFilter)) {
			String scenario = subFile.getName().split("\\.")[0];
			if(!scenarios.contains(scenario)) scenarios.add(scenario);
		}
		// Read the first file to get the times, parameters, and xyz
		File firstFile = directory.listFiles(fileFilter)[0];
		try (BufferedReader br = new BufferedReader(new FileReader(firstFile))) {
			String line;
			String key = "";
			int index = 0;
			int countNodes = 0;
			while ((line = br.readLine()) != null) { //We actually have to read the whole file... times are scattered throughout
				// Add the parameters and units while generating an indexMap
				if(line.contains("VARIABLES")) {
					String[] tokens = line.split("\""); //Split by quotes to isolate parameter and unit
					for(String token: tokens) {
						if(token.contains("=") || token.trim().equals("")) continue; //Skip everything but the parameters
						String parameter = token.split(",")[0].trim().toLowerCase();
						if(parameter.toLowerCase().contains("porosity"))
							parameter = "porosity";
						indexMap.add(parameter); //Store the order which parameters are provided
						if(!parameters.contains(parameter) && !parameter.equals("x") && !parameter.equals("y") && !parameter.equals("z") 
								&& !parameter.equals("volume") && !parameter.equals("porosity")) //Skip these
							parameters.add(parameter); //Save parameters
						if(token.contains(",")) { //This means they give units
							String unit = token.split(",")[1].trim();
							units.put(parameter, unit); //Save units
						}
					}
				}
				// Add the times from this line, scattered through file
				else if(line.contains("ZONE")) { //This lists the zone name, which includes the timestep
					String[] tokens = line.split("\""); //Zone name is wrapped in quotes
					String time = tokens[1].replaceAll("\\D+", "").replaceAll("\\.", "");
					try {
						times.add(Float.parseFloat(time)); //Parse value into float
					} catch(Exception e) {
						System.out.println("Years Error: " + time);
					}
					// Also add the number of nodes, as this will be used to count off blocks
					tokens = line.split(","); //Header info is comma delimited
					for(String token: tokens) {
						try {
							if(token.toLowerCase().contains("nodes")) {
								nodes = Integer.parseInt(token.split("=")[1].trim());
							} else if(token.toLowerCase().contains("elements")) {
								elements = Integer.parseInt(token.split("=")[1].trim());
							}
						} catch(Exception e) {
							System.out.println("Node/Element Error: " + token.split("=")[1].trim());
						}
					}
				}
				// This lists the data, from which we will get xyz (which are actually at edges)
				else if(!line.contains("=") && (key.equals("") || key.equals("x") || key.equals("y") || key.equals("z"))) { //Limit to the keys we want
					key = indexMap.get(index);
					String[] tokens = line.split("\\s+"); //Space delimited
					for(String token: tokens) {
						countNodes++;
						float value = Float.parseFloat(token);
						if(key.equals("x") & !vertexX.contains(value))
							vertexX.add(value);
						else if(key.equals("y") & !vertexY.contains(value))
							vertexY.add(value);
						else if(key.equals("z") & !vertexZ.contains(value))
							vertexZ.add(value);
					}
					// When the counter is high enough, we have finished with the parameter
					if(countNodes >= nodes) {
						index++; //On to the next parameter
						countNodes = 0; //Reset the counter
					}
				}
			}
			System.out.println(units.toString() + " These are the units");
		} catch (IOException e) {
			e.printStackTrace();
		}
		//Provided values are at the nodes (center) of each cell
		x = calculateCenters(vertexX);
		y = calculateCenters(vertexY);
		z = calculateCenters(vertexZ);
	}
	
	
	//TODO: Tecplot Data here...
	// Extracting data, statistics, and porosity from a list of Tecplot files
	public void extractTecplotData(File subFile) {
		String scenario = subFile.getName().split("\\.")[0];
		dataMap.put(scenario, new HashMap<String, float[][]>()); //Initialize dataMap for this scenario
		statistics.put(scenario, new HashMap<String, float[]>()); //Initialize statistics for this scenario
		System.out.print("Reading " + subFile.getName() + "...");
		long startTime = System.currentTimeMillis();
		int index = 0;
		String line;
		String parameter = indexMap.get(index);
		int timeIndex = 0;
		float[] tempData = new float[elements];
		float[] tempStats = new float[3];
		
		int countElements = 0;
		int countNodes = 0;
		boolean skip = false;
		try (BufferedReader br = new BufferedReader(new FileReader(subFile))) {
			while ((line = br.readLine()) != null) {
				// Get the time index from the header
				if(line.contains("ZONE")) { //This lists the zone name, which includes the timestep
					String[] tokens = line.split("\""); //Zone name is wrapped in quotes
					float time = Float.parseFloat(tokens[1].replaceAll("\\D+", "").replaceAll("\\.", ""));
					if(!selectedTimes.contains(time)) {
						skip = true; //This means we don't want the timestep - skip it until we check at the next timestep
					} else {
						timeIndex = selectedTimes.indexOf(time); //Parse value into float
						skip = false;
					}
					
				}
				// This is the data - we want all selected parameters and porosity
				if(!line.contains("=") && !line.trim().isEmpty() && !skip) {
					String[] tokens = line.split("\\s+"); //Space delimited
					// Count the numbers so we know when we finish the block - x, y, z based on node count
					if(parameter.equals("x") || parameter.equals("y") || parameter.equals("z")) {
						countNodes += tokens.length;
					// Read in data for all selected parameters and porosity, saving statistics as we go
					// Any parameters that weren't selected are skipped, only the first porosity is stored (special handling)
					} else if(selectedParameters.contains(parameter) || (parameter.toLowerCase().contains("porosity") && porosity==null)) {
						for(String token: tokens) {
							float value = Float.parseFloat(token);
							tempData[countElements] = value;
							if(value<tempStats[0]) tempStats[0] = value; //Min
							tempStats[1] += value/nodes; //Avg for timestep
							if(value>tempStats[2]) tempStats[2] = value; //Max
							countElements++;
						}
					// Count the numbers so we know when we finish the block - parameters based on elements count
					} else { //Unselected parameters
						countElements += tokens.length;
					}
					// When the counter is high enough, we have finished with the parameter and should save
					if(countNodes >= nodes || countElements >= elements) {
						// Tecplot orders values differently and needs to be reordered into ijk
						tempData = reorderStomp(tempData);
						if(selectedParameters.contains(parameter)) { //Make sure we are looking at a selected parameter
							if(!dataMap.get(scenario).containsKey(parameter)) { //If data doesn't yet exist for the parameter
								dataMap.get(scenario).put(parameter, new float[selectedTimes.size()][elements]);
								statistics.get(scenario).put(parameter, new float[3]);
							}
							dataMap.get(scenario).get(parameter)[timeIndex] = tempData;
							if(tempStats[0]<statistics.get(scenario).get(parameter)[0]) statistics.get(scenario).get(parameter)[0] = tempStats[0]; //Min
							statistics.get(scenario).get(parameter)[1] += tempStats[1]/selectedTimes.size(); //Avg
							if(tempStats[2]>statistics.get(scenario).get(parameter)[2]) statistics.get(scenario).get(parameter)[2] = tempStats[2]; //Max
						}
						// Save porosity values
						else if(parameter.toLowerCase().contains("porosity") && porosity==null) { //Make sure we are looking at porosity
							porosity = tempData;
						}
						tempData = new float[elements]; //Reset these for the next parameter
						tempStats = new float[3];
						countNodes = 0; //Reset the counter
						countElements = 0; //Reset the counter
						System.out.print(parameter + " ");
						if(index<indexMap.size()-1)
							index++;
						else //Need special handling to reset for the next time block
							index = 3; //Index of the first parameter that is not x, y, z
						parameter = indexMap.get(index);
					}
				}
			}
			System.out.println(" took " + (System.currentTimeMillis() - startTime)/1000 + " s");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static ArrayList<Float> calculateEdges(ArrayList<Float> cellCenters) {
		ArrayList<Float> cellEdges = new ArrayList<Float>();		
		for(int i=1; i<cellCenters.size(); i++) {
			float half = (cellCenters.get(i)-cellCenters.get(i-1))/2;
			if(i == 1)
				cellEdges.add(new Float(cellCenters.get(i-1)-half).floatValue());
			cellEdges.add(new Float(cellCenters.get(i-1)+half).floatValue());
			if(i == cellCenters.size()-1) 
				cellEdges.add(new Float(cellCenters.get(i)+half).floatValue());
		}
		return cellEdges;
	}
	
	
	private static ArrayList<Float> calculateCenters(ArrayList<Float> cellEdges) {
		ArrayList<Float> cellCenters = new ArrayList<Float>();
		for(int i=1; i<cellEdges.size(); i++) {
			cellCenters.add(cellEdges.get(i) - (cellEdges.get(i)-cellEdges.get(i-1)) / 2);
		}
		return cellCenters;
	}
	
	
	/**					**\
	 * Getters & Setters *
	 * 					 *
	\*					 */
	
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
		for(JCheckBox scenario: listScenarios) {
			if(scenario.isSelected())
				selectedScenarios.add(scenario.getText());
		}
		for(JCheckBox time: listTimes) {
			if(time.isSelected())
				selectedTimes.add(Float.parseFloat(time.getText()));
		}
		for(JCheckBox parameter: listParameters) {
			if(parameter.isSelected())
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
		if(dataMap.get(scenario).containsKey(parameter))
			return dataMap.get(scenario).get(parameter);
		return null;
	}
	
	public float[] getStatistics(String scenario, String parameter) {
		if(statistics.get(scenario).containsKey(parameter))
			return statistics.get(scenario).get(parameter);
		return null;
	}
	
	public String getUnit(String parameter) {
		if(units.containsKey(parameter))
			return units.get(parameter);
		return "";
	}
	
	public void setUnit(final String theParameter, final String theValue) {
		units.put(theParameter,theValue);
	}
	
}
