package gridviz;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import hdf5Tool.FileBrowser;
import objects.NodeStructure;
import utilities.Constants;

/**
 * @brief  Provides a parser to extract DataGrid objects from Stomp data files
 * @author Tucker Beck
 * @date   3/7/12
 */
public class GridParser {
	
	public static enum FileType { PLOT, NTAB, TECPLOT};
	public FileType fileType = FileType.PLOT;
	
	/// Describes the data file from which this parser will extract data
	private File dataFile;
	private float timeStep = 0;
	private static List<Float> timesAsFloats = new ArrayList<Float>();
	
	private static enum Tecplot {
		TITLE("TITLE"),
		VARIABLES("VARIABLES"),
		X("X"), Y("Y"), Z("Z"),
		ZONE("ZONE"),
		SOLUTION_TIME("SOLUTIONTIME"),
		NODES("NODES"),
		ELEMENTS("ELEMENTS"),
		VARLOCATION("VARLOCATION"),
		NODAL("NODAL");
		private String key;
		Tecplot(String key) {
			this.key = key;
		}
	}
	
	@SuppressWarnings("unused")
	private static enum Ntab {
		INDEX("index"), I("i"), J("j"), K("k"), 
		ELEMENT_REF("element reference"), 
		NUFT_INDEX("nuft index"), 
		X("x"), Y("y"), Z("z"), DX("dx"), DY("dy"), DZ("dz"), VOLUME("volume"), DATA("data");
		private String key;
		Ntab(String key) {
			this.key = key;
		}
	}
	
	private List<File> filesToMerge = new ArrayList<File>();

	// STOMP keys used for parsing the header information
	private static enum Stomp {
		X_NODES("Number of X or R-Direction Nodes", true),
		Y_NODES("Number of Y or Theta-Direction Nodes", true),
		Z_NODES("Number of Z-Direction Nodes", true),

		ACTIVE_NODES("Number of Active Nodes", true),
		VERTICIES("Number of Vertices", true),

		X_ORGIN("X Origin"),
		X_ORGIN_SURFACE("X Origin -- Surface Positions"),
		X_ORGIN_HEXAHEDRA("X Origin -- Hexahedra Points"),

		Y_ORGIN("Y Origin"),
		Y_ORGIN_SURFACE("Y Origin -- Surface Positions"),
		Y_ORGIN_HEXAHEDRA("Y Origin -- Hexahedra Points"),

		Z_ORGIN("Z Origin"),
		Z_ORGIN_SURFACE("Z Origin -- Surface Positions"),
		Z_ORGIN_HEXAHEDRA("Z Origin -- Hexahedra Points");   	

		private final String key;
		private boolean integer;

		Stomp(String key, boolean integer) {
			this.key = key;
			this.integer = integer;
		};

		Stomp(String key) {
			this.key = key;
			this.integer = false;
		};

		public String getKey() {
			return key;
		}

		public boolean isInteger() {
			return integer;
		}
	}

	public class DataStructure {
		
		public int i;
		public int j;
		public int k;
		
		public float[] x;
		public float[] y;
		public float[] z;
		
		public float[] vertexX;
		public float[] vertexY;
		public float[] vertexZ;
		
		public float[] times;
		public Map<String, float[][]> data;
		public Map<String, float[]> statistics;
		public Map<String, String> units;
	}

	/**
	 * Construct the parser
	 * @param fileName The name of the file from which to extract data
	 */
	public GridParser(String fileName) {
		dataFile = new File(fileName);
	}

	public GridParser(String fileName, float timeStep) {
		dataFile = new File(fileName);
		fileType = FileType.NTAB;	
		this.timeStep = timeStep;
	}

	public GridParser(String fileName, FileType fileType, int timeStep) {
		dataFile = new File(fileName);
		this.fileType = fileType;	
		this.timeStep = timeStep;
	}

	public void mergeFile(File file) {
		filesToMerge.add(file);
	}
	
	public float getTimeStep() {
		return timeStep;
	}
	
	
	public static Float extractStompTime(File subFile) {
		Float timeStep = null;
		try (BufferedReader br = new BufferedReader(new FileReader(subFile))) {
			String line;
			while ((line = br.readLine()) != null) { //The timestep is in the header
				if(line.startsWith("Time") & line.contains(",yr")) {
					int startIndex = line.indexOf(",wk") + 3;
					int endIndex = line.indexOf(",yr");
					String sub = line.substring(startIndex,endIndex).trim();
					try {
						timeStep = Math.round(Float.parseFloat(sub) * 1000f) / 1000f;
						if (!timesAsFloats.contains(timeStep))
							timesAsFloats.add(timeStep);
					} catch (Exception e) {
						System.out.println("Years Error: " + sub);
					}
					break; //No need to read the rest of the file
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return timeStep;
	}
	
	
	
	
	
	public DataGrid extractStompData() throws GridError {
		DataGrid grid = null; // will contain all the extracted data from the file
		Point3i size = new Point3i(0, 0, 0); // This will hold the size of the grid to extract
		Vector3f center = new Vector3f();
		boolean hasCenter = false;
		Map<Stomp, Object> info = new HashMap<Stomp, Object>();
		boolean emptyLine = false;
		boolean nodal = false;
		int linearIndex = -1; // The linear index will keep track of where to insert the data into the array that holds the node data for each field
		String fieldKey = ""; // This will hold the name of the field that is being extracted
		FieldValues values = null; // The field values will be first extracted then assigned
		String line;
		
		try (BufferedReader br = new BufferedReader(new FileReader(dataFile))) {
			System.out.println("File: " + dataFile);
			while ((line = br.readLine()) != null) {
				
				// Skip empty lines, but note when they occur
				if (line.isEmpty())
					emptyLine = true;
				
				// Ignore this line, as time is handled elsewhere
				else if(line.startsWith("Time") && line.contains(",yr"))
					continue;
				
				// Reads in the grid configuration information from the plot file
				else if(line.contains("=")) {
					for(Stomp key: Stomp.values()) {
						if(line.startsWith(key.getKey())) {
							int splitIndex = line.indexOf('=') + 1;
							String substring = line.substring(splitIndex).trim();
							if(key.isInteger())
								info.put(key, Integer.parseInt(substring));
							else
								info.put(key, Float.parseFloat(substring));
							//System.out.println("Reading [" + key.getKey() + "] - " + line);	
							break; // Already handled it, skip the rest of the keys
						}
					}
					if(info.size() > 2) {
						size.x = (Integer)info.get(Stomp.X_NODES);
						size.y = (Integer)info.get(Stomp.Y_NODES);
						size.z = (Integer)info.get(Stomp.Z_NODES);

						// If we have origin information we need to increment the size value
						for(Stomp originKey: new Stomp[] {Stomp.X_ORGIN_HEXAHEDRA, Stomp.X_ORGIN_SURFACE, Stomp.X_ORGIN}) {
							if(info.containsKey(originKey)) {
								center.x = Float.parseFloat(info.get(originKey).toString());
								hasCenter= true;
							}
						}
						for(Stomp originKey: new Stomp[] {Stomp.Y_ORGIN_HEXAHEDRA, Stomp.Y_ORGIN_SURFACE, Stomp.Y_ORGIN}) {
							if(info.containsKey(originKey)) {
								center.y = Float.parseFloat(info.get(originKey).toString());
								hasCenter= true;
							}
						}
						for(Stomp originKey: new Stomp[] {Stomp.Z_ORGIN_HEXAHEDRA, Stomp.Z_ORGIN_SURFACE, Stomp.Z_ORGIN}) {
							if(info.containsKey(originKey)) {
								center.z = Float.parseFloat(info.get(originKey).toString());
								hasCenter= true;
							}
						}
						
						// Create the data grid to store the field data
						if(hasCenter)
							grid = new DataGrid(size, center);
						else
							grid = new DataGrid(size);
						
						grid.setTimestep(timesAsFloats.get(timesAsFloats.indexOf(timeStep)));
					}
				}
				
				// Reads in the grid data from the plot file
				else if ((line.contains(",") || emptyLine) && grid!=null) {
					emptyLine = false; // Reset
					nodal = false; // Reset
					String[] lineList = line.split(",");
					// Need to be specific here on what makes up the grid
					if(line.startsWith("X or R-Direction Node Positions") ||
							line.startsWith("Radial-Direction Node Positions") ||
							line.startsWith("X-Direction Surface Positions") ||
							line.startsWith("X-Direction Node Positions") ||
							line.startsWith("X-Direction Nodal Vertices")) {
						fieldKey = "x";
					} else if(line.startsWith("Y or Theta-Direction Node Positions") ||
							line.startsWith("Theta-Direction Node Positions") ||
							line.startsWith("Y-Direction Surface Positions") ||
							line.startsWith("Y-Direction Node Positions") ||
							line.startsWith("Y-Direction Nodal Vertices")) {
						fieldKey = "y";
					} else if(line.startsWith("Z-Direction Node Positions") ||
							line.startsWith("Z-Direction Surface Positions") ||
							line.startsWith("Z-Direction Nodal Vertices")) {
						fieldKey = "z";
					} else
						fieldKey = lineList[0]; // Otherwise the key will stay as  is (strip out units)
					// Now set the units if it is designated - should follow the comma on this line
					if(lineList.length > 1)
						grid.setFieldUnit(fieldKey.trim(), lineList[1].trim());
					else
						grid.setFieldUnit(fieldKey.trim(), "");
					// If it has Nodal/Node in the name, it is giving values for edges.
					if(line.contains("Nodal") || line.contains("Node"))
						nodal = true;
					values = grid.getFieldValues(fieldKey.trim()); // Fetch the value set for the given field
					linearIndex = 0; // Start the linear index at 0
					continue;
				} else if (grid!=null){
					emptyLine = false;
					String[] items = line.trim().split("\\s+"); // Split the line by any number of spaces between values
					
					// Store each value from the line into the value list
					if(nodal) {
						if(fieldKey=="x") {
							if (linearIndex<grid.getSize().x) {
								values.addNodalValue(linearIndex, items);
								values.addVertex(linearIndex, items, 1);
							}
						} else if(fieldKey=="y") {
							if (linearIndex % grid.getSize().x == 0 && linearIndex<grid.getSize().x * grid.getSize().y) {
								int altIndex = linearIndex/grid.getSize().x;
								values.addNodalValue(altIndex, items);
								values.addVertex(altIndex, items, 2);
							}
						} else if(fieldKey=="z") {
							if (linearIndex % (grid.getSize().x * grid.getSize().y) == 0) {
								int altIndex = linearIndex/(grid.getSize().x*grid.getSize().y);
								values.addNodalValue(altIndex, items);
								values.addVertex(altIndex, items, 4);
							}
						} else {
							values.addNodalValue(linearIndex, items);
						}
						linearIndex++;
					} else {
						for(String item: items) {
							try{
								float value = Float.parseFloat(item);
								values.setValue(linearIndex, value);
							} catch (NumberFormatException err) {
								// Ignore values that can't be parsed
							} catch (GridError err) {
								System.out.println("Error reading " + fieldKey);
								throw err;
							}
							linearIndex++;
						}
					}
				}
			}
		} catch (Exception err) {
			System.out.println(this.dataFile);
			throw new GridError("Extraction failed: " + err.getMessage());
		}
		
		// A bit of extra work for 2d meshes.
		if(grid.is2D()) {
			// Fetch the value set for the given field
			values = grid.getFieldValues(grid.getNormalDirection());
			// Fill the missing edge with a bunch of 0's
			for(int i = 0; i < grid.getGridder().getL(); i++) {
				values.setValue(i, 0.0f);
			}
		}
		return grid;
	}
	
	
	//TODO: Actively working on writing this
	public DataStructure extractTecplotData() throws Exception {
		
		DataStructure structure = new DataStructure();
		structure.data = new HashMap<String, float[][]>();
		structure.statistics = new HashMap<String, float[]>();
		structure.units = new HashMap<String, String>();
		ArrayList<String> indexMap = new ArrayList<String>();
		ArrayList<Float> years = new ArrayList<Float>();
		String line;
		int nodes = 0;
		
		//////////////////////////////////////////////////////////////////////////////
		//// Read the first file to get the file structure and header information ////
		//////////////////////////////////////////////////////////////////////////////
		try (BufferedReader br = new BufferedReader(new FileReader(dataFile))) {
			ArrayList<Float> uniqueXs = new ArrayList<Float>();
			ArrayList<Float> uniqueYs = new ArrayList<Float>();
			ArrayList<Float> uniqueZs = new ArrayList<Float>();
			int index = 0;
			int countNodes = 0;
			while ((line = br.readLine()) != null) { // Read each line
				
				//// This is the header - we want to map out the variables and units and get the number of nodes ////
				if(line.contains("=")) {
					if(line.contains("VARIABLES")) {//This lists the data types and their units
						String[] lineList = line.split("\""); //Split by quotes to isolate parameter and unit
						for(String subString: lineList) {
							if(subString.contains("=") || subString.trim().equals("")) continue; //Skip everything but the parameters
							String[] split = subString.split(",");
							indexMap.add(split[0].trim().toLowerCase());
							structure.statistics.put(split[0].trim().toLowerCase(), new float[3]);
							if(split.length>1 && !split[1].trim().contains("null")) //If there were units, store them
								structure.units.put(split[0].trim().toLowerCase(), split[1].trim());
						}
					} else if(line.contains("ZONE")) {//This lists the number of nodes
						String[] lineList = line.split(","); //Comma delimited
						for(String subString:lineList) {
							String[] split = subString.split("=");
							if(split[0].contains("NODES")) {
								try {
									nodes = Integer.parseInt(split[1].trim());
									break;
								} catch(Exception e) {
									System.out.println("Nodes Error: " + split[1].trim());
								}
							}
						}
						String[] tokens = line.split("\""); //Zone name is wrapped in quotes
						float timestep = Float.parseFloat(tokens[1].replaceAll("\\D+", "").replaceAll("\\.", ""));
						years.add(timestep);
					}
				} 
				
				//// This is the data - we want to extract the X, Y, Z dimensions ////
				else {
					String key = indexMap.get(index);
					String[] lineList = line.split("\\s+"); //Space delimited
					for(String subString: lineList) {
						countNodes++;
						float value = Float.parseFloat(subString);
						if(key.equals("x") & !uniqueXs.contains(value))
							uniqueXs.add(value);
						else if(key.equals("y") & !uniqueYs.contains(value))
							uniqueYs.add(value);
						else if(key.equals("z") & !uniqueZs.contains(value))
							uniqueZs.add(value);
					}
					// When the counter is high enough, we have finished with the parameter
					if(countNodes >= nodes) {
						index++; //On to the next parameter
						countNodes = 0; //Reset the counter
					}
					// Once we finish x, y, and z we want to exit and store results
					if(!indexMap.get(index).equals("x") && !indexMap.get(index).equals("y") && !indexMap.get(index).equals("z")) {
						structure.i = uniqueXs.size();
						structure.j = uniqueYs.size();
						structure.k = uniqueZs.size();
						structure.x = Constants.listToArray(uniqueXs);
						structure.y = Constants.listToArray(uniqueYs);
						structure.z = Constants.listToArray(uniqueZs);
						structure.vertexX = Constants.listToArray(NodeStructure.setEdge(uniqueXs));
						structure.vertexY = Constants.listToArray(NodeStructure.setEdge(uniqueYs));
						structure.vertexZ = Constants.listToArray(NodeStructure.setEdge(uniqueZs));
						break;
					}
				}
			}
		}
		
		///////////////////////////////////////////////////////
		//// Loop through files to read and merge the data ////
		///////////////////////////////////////////////////////
		for(File dataFile: filesToMerge) {
			float[][] dataMap = new float[1][nodes];
			float max = Float.MIN_VALUE;
			float min = Float.MAX_VALUE;
			float sum = 0;
			int index = 3;
			int countNodes = 0;
			try (BufferedReader br = new BufferedReader(new FileReader(dataFile))) {
				while ((line = br.readLine()) != null) { // Read each line
					
					//// This is the data - we want all values ////
					if(!line.contains("=") && !line.trim().isEmpty()) {
						String[] lineList = line.trim().split(" "); //space delimited
						for(String subString: lineList) {
							float value = Float.parseFloat(subString);
							dataMap[0][countNodes] = value; //Save the value
							if(value<min) min = value;
							if(value>max) max = value;
							sum += value;
							countNodes++;
						}
						//When the counter is high enough, we have finished with the parameter for this timestep
						if(countNodes >= nodes) {
							String fieldKey = indexMap.get(index); //The parameter that these values belong to
							float average = sum / (structure.i * structure.j * structure.k); //Calculate the average
							if(!structure.data.containsKey(fieldKey)) { //If data doesn't yet exist for the parameter
								structure.data.put(fieldKey, dataMap); //Store values
								float[] statistics = new float[]{min, average, max};
								structure.statistics.put(fieldKey, statistics); //Store statistics
							} else { //Some data already exists for the timestep
								float[][] combined = new float[structure.data.get(fieldKey).length + 1][];
								System.arraycopy(structure.data.get(fieldKey), 0, combined, 0, structure.data.get(fieldKey).length);
								System.arraycopy(dataMap, 0, combined, structure.data.get(fieldKey).length, dataMap.length);
								structure.data.put(fieldKey, combined); //Store values
								float[] statistics = new float[3];
								statistics[0] = Math.min(structure.statistics.get(fieldKey)[0], min);
								statistics[1] = average + structure.statistics.get(fieldKey)[1]; //Need to divide by timesteps later
								statistics[2] = Math.max(structure.statistics.get(fieldKey)[2], max);
								structure.statistics.put(fieldKey, statistics); //Store statistics
							}
							index++; //On to the next parameter
							countNodes = 0; //Reset the counter
						}
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		// Last step - just need to divide the average by the number of timesteps for statistics
		// This is because at the time of storing, we don't know how many timesteps are in the file
		for(String key: structure.statistics.keySet())
			structure.statistics.get(key)[1] = structure.statistics.get(key)[1] / years.size();
		
		return structure;
	}
	
	
	public static List<String> getTecplotVariables(File file) throws Exception {

		// Open the file
		List<String> dataTypes = new ArrayList<String>();
		Scanner sc = new Scanner(file);
		String firstLine = "";			

		// Read until we find the zone 
		while(!(firstLine = sc.nextLine()).startsWith(Tecplot.VARIABLES.key) && sc.hasNextLine());

		for(String variable: firstLine.split("\"")) {
			variable = variable.split(",")[0].trim(); // Remove commas
			if(variable.isEmpty())
				continue;
			if(variable.startsWith(Tecplot.VARIABLES.key)) {

			} else if(variable.equalsIgnoreCase(Tecplot.X.key)) {

			} else if(variable.equalsIgnoreCase(Tecplot.Y.key)) {

			} else if(variable.equalsIgnoreCase(Tecplot.Z.key)) {

			} else {
				dataTypes.add(variable.replaceAll(" ", "_"));
			}
		}

		sc.close();
		return dataTypes;
	}
	
	
	// Extract all timesteps from a file
	public static List<Float> extractTecplotTimes(File subFile) {
		System.out.println("Pulling time information from files...");
		List<Float> timesteps = new ArrayList<Float>();
		try (BufferedReader br = new BufferedReader(new FileReader(subFile))) {
			String line;
			while ((line = br.readLine()) != null) { //We actually have to read the whole file... timesteps are scattered throughout
				if(line.contains("ZONE")) { //This lists the zone name, which includes the timestep
					String[] tokens = line.split("\""); //Zone name is wrapped in quotes
					String sub = tokens[1].replaceAll("\\D+", "").replaceAll("\\.", "");
					try {
						float timestep = Float.parseFloat(sub);
						timesteps.add(timestep);
					} catch(Exception e) {
						System.out.println("Years Error: " + sub);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return timesteps;
	}
	
	
	// Extract timesteps from the first file
	public static List<Float> extractNtabTimes(File subFile) {
		System.out.println("Pulling time information from files...");
		List<Float> timesteps = new ArrayList<Float>();
		try (BufferedReader br = new BufferedReader(new FileReader(subFile))) {
			String line;
			while ((line = br.readLine()) != null) { //This loop would go through whole file, but after header we break
				if(line.startsWith("index")) { //The header lists all the timesteps
					String[] tokens = line.split("\\s+"); //The line is space delimited
					// skip these 
					// index i j k element_ref nuft_ind x y z dx dy dz volume [times]
					int start = Arrays.asList(tokens).indexOf("volume"); //Last index before times
					for(int i=start+1; i<tokens.length; i++) {
						if(tokens[i].contains(".")) //Remove any decimals if they exist
							tokens[i] = tokens[i].split("\\.")[0];
						String sub = tokens[i].replaceAll("\\D+", "");
						try {
							float years = Float.parseFloat(sub);
							timesteps.add(years);
						} catch(Exception e) {
							System.out.println("Years Error: " + sub);
						}
					}
				}
				else //Break when we finish reading the header
					break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return timesteps;
	}
	
	
	/*// Extract data structure from the first file
	public DataStructure extractNtabStructure(File subFile) throws Exception {
		DataStructure structure = new DataStructure();
		ArrayList<String> indexMap = new ArrayList<String>();
		System.out.println("Pulling data structure from files...");
		
		// Start reading the file
		try (BufferedReader br = new BufferedReader(new FileReader(subFile))) {
			ArrayList<Float> uniqueXs = new ArrayList<Float>();
			ArrayList<Float> uniqueYs = new ArrayList<Float>();
			ArrayList<Float> uniqueZs = new ArrayList<Float>();
			String line;
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split("\\s+"); //The line is space delimited
				
				// This is the header from which we extract: timeSteps
				// index i j k element_ref nuft_ind x y z dx dy dz volume [times]
				if(line.startsWith("index")) {
					int start = Arrays.asList(tokens).indexOf("volume") + 1; //Last index before times
					for(int i=0; i<tokens.length; i++) {
						if(i<start) {
							indexMap.add(tokens[i]);
						} else {
							String sub = tokens[i].replaceAll("\\D+", ""); //Remove any letters
							try {
								float years = Float.parseFloat(sub);
								structure.times[i-start] = years;
							} catch(Exception e) {
								System.out.println("Years Error: " + sub);
							}
						}
					}
					break;
				}
				
				// This is the data from which we extract: x, y, z
				else {
					float x = Float.parseFloat(tokens[indexMap.indexOf("x")]);
					float y = Float.parseFloat(tokens[indexMap.indexOf("y")]);
					float z = Float.parseFloat(tokens[indexMap.indexOf("z")]);
					if(!uniqueXs.contains(x)) uniqueXs.add(x);
					if(!uniqueYs.contains(y)) uniqueYs.add(y);
					if(!uniqueZs.contains(z)) uniqueZs.add(z);
				}
			}
			structure.x = Constants.listToArray(uniqueXs);
			structure.y = Constants.listToArray(uniqueYs);
			structure.z = Constants.listToArray(uniqueZs);
			structure.vertexX = Constants.listToArray(NodeStructure.setEdge(uniqueXs));
			structure.vertexY = Constants.listToArray(NodeStructure.setEdge(uniqueYs));
			structure.vertexZ = Constants.listToArray(NodeStructure.setEdge(uniqueZs));
			structure.i = uniqueXs.size();
			structure.j = uniqueYs.size();
			structure.k = uniqueZs.size();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return structure;
	}*/
	
	
	// TODO: Copy this Code for TecPlot...
	public DataStructure extractNTABData() throws Exception {
		DataStructure structure = new DataStructure();
		structure.data = new HashMap<String, float[][]>();
		structure.statistics = new HashMap<String, float[]>();
		Map<String, Integer> indexMap = new HashMap<String, Integer>();
		ArrayList<Float> years = new ArrayList<Float>();
		String line;
		
		//////////////////////////////////////////////////////////////////////////////
		//// Read the first file to get the file structure and header information ////
		//////////////////////////////////////////////////////////////////////////////
		try (BufferedReader br = new BufferedReader(new FileReader(dataFile))) {
			String fieldKey = dataFile.getName().replace(".ntab","").replaceAll("\\d+","").replaceAll("\\.","_");
			ArrayList<Float> uniqueXs = new ArrayList<Float>();
			ArrayList<Float> uniqueYs = new ArrayList<Float>();
			ArrayList<Float> uniqueZs = new ArrayList<Float>();
			while ((line = br.readLine()) != null) { //Read each line
				String[] lineList = line.split("\\s+"); //Space delimited
				
				// Read the header information to map the desired variables
				if(lineList[0].equalsIgnoreCase("index")) {
					for(int i=0; i<lineList.length; i++) {
						String token = lineList[i];
						if(token.equalsIgnoreCase("i") || token.equalsIgnoreCase("j") || token.equalsIgnoreCase("k"))
							indexMap.put(token, i);
						else if(token.equalsIgnoreCase("x") || token.equalsIgnoreCase("y") || token.equalsIgnoreCase("z"))
							indexMap.put(token, i);
						else if(token.equalsIgnoreCase("index"))
							indexMap.put(token, i);
						else if(token.equalsIgnoreCase("dx") || token.equalsIgnoreCase("dy") || token.equalsIgnoreCase("dz")) {
							//do nothing
						} else if(token.equalsIgnoreCase("element_ref") || token.equalsIgnoreCase("nuft_ind") || token.equalsIgnoreCase("volume")) {
							//do nothing
						} else { //Denotes the start of the data values
							indexMap.put("data", i);
							break;
						}
					}
					for(int i=indexMap.get("data"); i<lineList.length; i++) {
						String token = lineList[i].replaceAll(fieldKey, "").replaceAll("y", "");
						years.add(Float.parseFloat(token));
					}
				} else {
					int i = Integer.parseInt(lineList[indexMap.get("i")]);
					int j = Integer.parseInt(lineList[indexMap.get("j")]);
					int k = Integer.parseInt(lineList[indexMap.get("k")]);
					if(i > structure.i) structure.i = i;
					if(j > structure.j) structure.j = j;
					if(k > structure.k) structure.k = k;
					
					float x = Float.parseFloat(lineList[indexMap.get("x")]);
					float y = Float.parseFloat(lineList[indexMap.get("y")]);
					float z = Float.parseFloat(lineList[indexMap.get("z")]);
					if(!uniqueXs.contains(x)) uniqueXs.add(x);
					if(!uniqueYs.contains(y)) uniqueYs.add(y);
					if(!uniqueZs.contains(z)) uniqueZs.add(z);
				}
			}
			structure.x = Constants.listToArray(uniqueXs);
			structure.y = Constants.listToArray(uniqueYs);
			structure.z = Constants.listToArray(uniqueZs);
			structure.vertexX = Constants.listToArray(NodeStructure.setEdge(uniqueXs));
			structure.vertexY = Constants.listToArray(NodeStructure.setEdge(uniqueYs));
			structure.vertexZ = Constants.listToArray(NodeStructure.setEdge(uniqueZs));
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		///////////////////////////////////////////////////////
		//// Loop through files to read and merge the data ////
		///////////////////////////////////////////////////////
		for(File dataFile: filesToMerge) {
			String fieldKey = dataFile.getName().replace(".ntab","").replaceAll("\\d+","").replaceAll("\\.","_");
			float[][] dataMap = new float[years.size()][structure.i * structure.j * structure.k];
			float max = Float.MIN_VALUE;
			float min = Float.MAX_VALUE;
			float sum = 0;
			try (BufferedReader br = new BufferedReader(new FileReader(dataFile))) {
				while ((line = br.readLine()) != null) { // Read each line
					String[] lineList = line.split(" "); //space delimited
					
					// Skip the header and read the data throughout the file
					if(!lineList[0].equalsIgnoreCase("index")) {
						int index = Integer.parseInt(lineList[indexMap.get("index")]) - 1;
						for(int i=indexMap.get("data"); i<lineList.length; i++) { // Only read data
							int timestep = i - indexMap.get("data");
							float value = Float.parseFloat(lineList[i]);
							dataMap[timestep][index] = value;
							if(value<min) min = value;
							if(value>max) max = value;
							sum += value;
						}
					}
				}
				structure.data.put(fieldKey, dataMap);
				
				float average = sum / (structure.i * structure.j * structure.k * years.size());
				float[] statistics;
				if(structure.statistics.containsKey(fieldKey)) {
					statistics = new float[3];
					statistics[0] = Math.min(structure.statistics.get(fieldKey)[0], min);
					statistics[1] = average + structure.statistics.get(fieldKey)[1];
					statistics[2] = Math.max(structure.statistics.get(fieldKey)[2], max);
				} else {
					statistics = new float[]{min, average, max};
				}
				structure.statistics.put(fieldKey, statistics);
				
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return structure;
	}

	public Object[] getDataTypes(String fileType, Collection<GridParser> files) throws GridError, FileNotFoundException {
		if(fileType.equals(FileBrowser.STOMP)) {
			return extractStompData().getFieldNames().toArray();
		} else if(fileType.equals(FileBrowser.NUFT)) {
			List<String> fieldNames = new ArrayList<String>();			
			List<File> allFiles = new ArrayList<File>(filesToMerge);
			allFiles.add(dataFile);
			for(File fileToMerge: allFiles) {
				String fieldKey = fileToMerge.getName().replace(".ntab","").replaceAll("\\d+","").replaceAll("\\.","_");
				fieldNames.add(fieldKey);
			}
			// Use the file names
			return fieldNames.toArray();
		} else if(fileType.equals(FileBrowser.TECPLOT)){
			try {
				return getTecplotVariables(dataFile).toArray();
			} catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
			}
		}
		return new Object[]{};
	}
	
	public static float getTime(int time) {
		return timesAsFloats.get(time);
	}
}
