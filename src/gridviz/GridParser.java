package gridviz;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
	
	private int timeStep = 0;
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
		
		public Map<String, float[][]> data;
		public Map<String, float[]> statistics;
	}

	/**
	 * Construct the parser
	 * @param fileName The name of the file from which to extract data
	 */
	public GridParser(String fileName) {
		dataFile = new File(fileName);
	}

	public GridParser(String fileName, int timeStep) {
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
	
	public int getTimeStep() {
		return timeStep;
	}
	
	public void extractStompTimes(Collection<GridParser> files) throws GridError, FileNotFoundException {
		System.out.println("Pulling time information from files...");
		for(GridParser file: files) {
			try (Scanner dataScanner = new Scanner(new FileReader(file.dataFile))) {
				while (dataScanner.hasNextLine()) {
					String line = dataScanner.nextLine().trim();
					
					if(line.startsWith("Time") && line.contains(",yr")) {
						int startIndex = line.indexOf(",wk") + 3;
						int endIndex = line.indexOf(",yr");
						String sub = line.substring(startIndex,endIndex).trim();
						try {
							float timestep = Float.parseFloat(sub);
							if (!timesAsFloats.contains(timestep))
								timesAsFloats.add(timestep);
						} catch (Exception e) {
							System.out.println("Years Error: " + sub);
						}
						break;
					}
				}
			}
		}
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
						
						grid.setTimestep(timesAsFloats.get(timeStep));
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
	
	
	public DataStructure extractTecplotData() throws Exception {

		DataStructure structure = new DataStructure();
		Map<Integer, Tecplot> indexOf = new HashMap<Integer, Tecplot>();
		Map<Integer, String> dataTypes = new HashMap<Integer, String>();

		Map<Float, Map<String, List<Float>>> data = new HashMap<Float, Map<String, List<Float>>>();

		// Need unique xyz's only
		HashSet<Float> uniqueXs = new HashSet<Float>();
		HashSet<Float> uniqueYs = new HashSet<Float>();
		HashSet<Float> uniqueZs = new HashSet<Float>();

		if(!filesToMerge.contains(dataFile)) {
			filesToMerge.add(dataFile);
		}

		for(File file: filesToMerge) {

			// Open the file
			Scanner sc = new Scanner(file);
			String firstLine = "";			

			int nodes = 0;
			int elements = 0;
			float timestep = 0;

			// Read the header, we may have multiple headers, not sure if all the info will be duplicated?
			boolean first = true;
			while(sc.hasNextLine()) {

				indexOf.clear(); // We won't have xyz second time around probably

				// Read until we find the variables
				while(sc.hasNextLine()) {
					firstLine = sc.nextLine();
					if(firstLine.startsWith(Tecplot.VARIABLES.key)) break;
					if(firstLine.startsWith(Tecplot.ZONE.key)) break;
					if(sc.hasNextFloat()) break; // Or we hit a numeric value
				}

				int index = 0;
				if(firstLine.startsWith(Tecplot.VARIABLES.key)) {
					for(String variable: firstLine.split("\"")) {
						variable = variable.split(",")[0].trim(); // Remove commas
						if(variable.isEmpty())
							continue;

						if(variable.startsWith(Tecplot.VARIABLES.key)) {

						} else if(variable.equalsIgnoreCase(Tecplot.X.key)) {
							indexOf.put(index, Tecplot.X);
							index++;
						} else if(variable.equalsIgnoreCase(Tecplot.Y.key)) {
							indexOf.put(index, Tecplot.Y);
							index++;
						} else if(variable.equalsIgnoreCase(Tecplot.Z.key)) {
							indexOf.put(index, Tecplot.Z);
							index++;
						} else {
							dataTypes.put(index, variable.replaceAll(" ", "_"));
							index++;
						}
					}
				}

				System.out.println("Index: " + indexOf);
				System.out.println("Variables: " + dataTypes);

				// Read until we find the zone 
				while(sc.hasNextLine()) {
					if(firstLine.startsWith(Tecplot.ZONE.key)) break;
					if(sc.hasNextFloat()) break;
					firstLine = sc.nextLine();
				}

				if(firstLine.startsWith(Tecplot.ZONE.key)) {
					for(String variable: firstLine.split(",")) {
						variable = variable.trim();
						if(variable.startsWith(Tecplot.NODES.key)) {
							nodes = Integer.parseInt(variable.split("=")[1].trim());
						} else if(variable.startsWith(Tecplot.ELEMENTS.key)) {
							elements = Integer.parseInt(variable.split("=")[1].trim());
						} else if(variable.startsWith(Tecplot.SOLUTION_TIME.key)) {
							timestep = Float.parseFloat(variable.split("=")[1].trim());
						}
					}
				}

				System.out.println("Nodes: " + nodes);
				System.out.println("Elements: " + elements);
				System.out.println("Timestep: " + timestep);

				// Read until we find a float
				while(sc.hasNextLine() && !sc.hasNextFloat()) { sc.nextLine(); }

				/*
				if(!firstLine.contains(Tecplot.NODAL.key)) {
					sc.close();
					throw new Exception("Only nodal format supported");
				} */

				if(sc.hasNextFloat()) {
					for(int i = 0; i < dataTypes.size()+indexOf.size(); i++) {

						// Reading x y or z: will have 1 per node
						if(indexOf.containsKey(i)) {

							for(int j = 0; j < nodes; j++) {
								if(indexOf.get(i).equals(Tecplot.X)) {
									uniqueXs.add(sc.nextFloat());
								} else if(indexOf.get(i).equals(Tecplot.Y)) {
									uniqueYs.add(sc.nextFloat());
								} else if(indexOf.get(i).equals(Tecplot.Z)) {
									uniqueZs.add(sc.nextFloat());
								}
							}


							// Reading data, will have 1 per element
						} else {

							String variable = dataTypes.get(first ? i : i + 3);

							if(!data.containsKey(timestep)) {
								data.put(timestep, new HashMap<String, List<Float>>());
							}
							if(!data.get(timestep).containsKey(variable)) {
								data.get(timestep).put(variable, new ArrayList<Float>());
							}
							for(int j = 0; j < elements; j++) {
								if(Float.compare(timestep, 40) == 0 && j == 5045) {
									System.out.println("HERE2");
								}
								data.get(timestep).get(variable).add(sc.nextFloat());
							}						
						}
					}
				}
				
				first = false;
			}

			// Read the file?
			sc.close();
		}

		// Build the structure
		ArrayList<Float> xs = new ArrayList<Float>(uniqueXs);
		ArrayList<Float> ys = new ArrayList<Float>(uniqueYs);
		ArrayList<Float> zs = new ArrayList<Float>(uniqueZs);

		Collections.sort(xs);
		Collections.sort(ys);
		Collections.sort(zs);

		/*
		float[] x = new float[xs.size()];
		for(int i = 0; i < xs.size(); i++) {
			x[i] = xs.get(i);
		}
		float[] y = new float[ys.size()];
		for(int i = 0; i < ys.size(); i++) {
			y[i] = ys.get(i);
		}
		float[] z = new float[zs.size()];
		for(int i = 0; i < zs.size(); i++) {
			z[i] = zs.get(i);
		}*/		
		
		float[] x = new float[xs.size() - 1];
		for(int i = 0; i < xs.size() - 1; i++) {
			x[i] = (xs.get(i+1) - xs.get(i))/2 + xs.get(i);
		}
		float[] y = new float[ys.size() - 1];
		for(int i = 0; i < ys.size() - 1; i++) {
			y[i] = (ys.get(i+1) - ys.get(i))/2 + ys.get(i);
		}
		float[] z = new float[zs.size() - 1];
		for(int i = 0; i < zs.size() - 1; i++) {
			z[i] = (zs.get(i+1) - zs.get(i))/2 + zs.get(i);
		}
		

		structure.x = x;
		structure.y = y;
		structure.z = z;
		structure.i = x.length; // Or is it plus 1?
		structure.j = y.length;
		structure.k = z.length;
		structure.data = new HashMap<String, float[][]>();

		List<String> sortedDataTypes = new ArrayList<String>(dataTypes.values());
		Collections.sort(sortedDataTypes);

		List<Float> sortedTimes = new ArrayList<Float>(data.keySet());
		Collections.sort(sortedTimes);

		
		for(int t = 0; t < sortedTimes.size(); t++) {
			for(String variable: sortedDataTypes) {
				List<Float> vars = data.get(sortedTimes.get(t)).get(variable);
				float[][] dataDoubleArray = new float[sortedTimes.size()][vars.size()];
				if(structure.data.containsKey(variable))
						dataDoubleArray = structure.data.get(variable);
				for(int i = 0; i < vars.size(); i++)
					dataDoubleArray[t][i] = vars.get(i);	
				structure.data.put(variable, dataDoubleArray);
			}
		}		

		return structure;
	}

	public static List<Integer> getTecplotTimestep(File file) throws Exception {

		// Open the file
		Scanner sc = new Scanner(file);
		String firstLine = "";			
		List<Integer> timesteps = new ArrayList<Integer>();

		// Read the header, we may have multiple headers, not sure if all the info will be duplicated?
		while(sc.hasNextLine()) {

			// Read until we find the zone 
			while(!(firstLine = sc.nextLine()).startsWith(Tecplot.ZONE.key) && sc.hasNextLine());

			for(String variable: firstLine.split(",")) {
				variable = variable.trim();
				if(variable.startsWith(Tecplot.SOLUTION_TIME.key)) {
					timesteps.add(Integer.parseInt(variable.split("=")[1].split(",")[0].replaceAll("\"", "").trim()));
				}
			}	
		}

		// didn't find it
		sc.close();
		return timesteps;
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
			while ((line = br.readLine()) != null) { // Read each line
				String[] lineList = line.split(" "); //space delimited
				
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
			
			List<Float> uniqueVertexX = NodeStructure.setEdge(uniqueXs);
			List<Float> uniqueVertexY = NodeStructure.setEdge(uniqueYs);
			List<Float> uniqueVertexZ = NodeStructure.setEdge(uniqueZs);
			structure.vertexX = Constants.listToArray(uniqueVertexX);
			structure.vertexY = Constants.listToArray(uniqueVertexY);
			structure.vertexZ = Constants.listToArray(uniqueVertexZ);
			
			filesToMerge.add(dataFile); // Not initially added to the list, need to include for the following loop
			
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
					statistics[0] = average + structure.statistics.get(fieldKey)[1];
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
			extractStompTimes(files);
			return extractStompData().getFieldNames().toArray();
		} else if(fileType.equals(FileBrowser.NTAB)) {
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
