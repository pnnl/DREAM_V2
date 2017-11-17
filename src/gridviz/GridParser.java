package gridviz;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import hdf5Tool.FileBrowser;

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
		//public String getKey() { // Not used
			//return key;
		//}		
	}

	private static enum Ntab {
		INDEX("index"), I("i"), J("j"), K("k"), 
		ELEMENT_REF("element reference"), 
		NUFT_INDEX("nuft index"), 
		X("x"), Y("y"), Z("z"), DX("dx"), DY("dy"), DZ("dz"), VOLUME("volume"), DATA("data");
		private String key;
		Ntab(String key) {
			this.key = key;
		}
		//public String getKey() { // Not used
			//return key;
		//}
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

		public float[] x;
		public float[] y;
		public float[] z;

		public int i;
		public int j;
		public int k;

		public Map<String, float[][]> data;

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
	
	public DataGrid extractStompData() throws GridError {
		
		DataGrid grid = null; // will contain all the extracted data from the file
		Point3i size = new Point3i(0, 0, 0); // This will hold the size of the grid to extract
		Vector3f center = new Vector3f();
		boolean hasCenter = false;
		Map<Stomp, Object> info = new HashMap<Stomp, Object>();
		float timestep = 0; // Time in years
		boolean emptyLine = false;
		boolean nodal = false;
		int linearIndex = -1; // The linear index will keep track of where to insert the data into the array that holds the node data for each field
		String fieldKey = ""; // This will hold the name of the field that is being extracted
		FieldValues values = null; // The field values will be first extracted then assigned
		
		try (Scanner dataScanner = new Scanner(new FileReader(dataFile))) {
			System.out.println("File: " + dataFile);
			while (dataScanner.hasNextLine()) {
				String line = dataScanner.nextLine().trim();
				
				if (line.isEmpty())
					emptyLine = true;
				
				// Reads in the Time Steps from the plot file
				else if(line.startsWith("Time") && line.contains(",yr")) {
					int startIndex = line.indexOf(",wk") + 3;
					int endIndex = line.indexOf(",yr");
					String sub = line.substring(startIndex,endIndex).trim();
					try {
						timestep = Float.parseFloat(sub);
					} catch (Exception e) {
						System.out.println("Years Error: " + sub);
					}
				}
				
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

						grid.setTimestep(timestep);
					}
				}
				
				// Reads in the grid data from the plot file
				else if ((line.contains(",") || emptyLine) && grid!=null) {
					emptyLine = false;
					nodal = false; // Reset
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
						fieldKey = line; // Otherwise the key will stay as it is
					if(line.contains("Nodal"))
						nodal = true;
					values = grid.getFieldValues(fieldKey.trim()); // Fetch the value set for the given field
					linearIndex = 0; // Start the linear index at 0
					continue;
				} else if (grid!=null){
					emptyLine = false;
					String[] items = line.trim().split("\\s+"); // Split the line by any number of spaces between values
					
					// Store each value from the line into the value list
					if(nodal) {
						if (fieldKey=="x") {
							if (linearIndex<grid.getSize().x) {
								values.addNodalValue(linearIndex, items);
								values.addVertex(linearIndex, items, 1);
							}
						} else if (fieldKey=="y") {
							if (linearIndex % grid.getSize().x == 0 && linearIndex<grid.getSize().x * grid.getSize().y) {
								int altIndex = linearIndex/grid.getSize().x;
								values.addNodalValue(altIndex, items);
								values.addVertex(altIndex, items, 2);
							}
						} else if (fieldKey=="z") {
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
		Map<Ntab, Integer> indexOf = new HashMap<Ntab, Integer>();

		// Determine how many nodes we have and where the time steps start
		Scanner sc = new Scanner(dataFile);
		String firstLine = "";			
		while(!(firstLine = sc.nextLine()).startsWith("index") && sc.hasNextLine());

		int dataIndex = 0;
		int dataValues = 0;

		if(firstLine.startsWith("index")) {
			// Everything else will be nodes
			String[] tokens = firstLine.split("\\s+");
			// skip these 
			// index i j k element_ref nuft_ind x y z dx dy dz
			boolean read = false;

			for(String token: tokens) {
				if(read) {
					String tmep = token;
					if(tmep.contains("."))
						tmep = tmep.split("\\.")[0];
					String years = tmep.replaceAll("\\D+", "");
					try {
						int yrs = Integer.parseInt(years);
						if(yrs == this.timeStep) {
							indexOf.put(Ntab.DATA, dataIndex);
							break;
						}
					} catch (NumberFormatException ne) {
						ne.printStackTrace();
					}
				}
				if(token.equalsIgnoreCase("volume")) {
					//		indexOf.put(NTAB_KEY.VOLUME, dataIndex);
					read = true;
				} else if(token.equalsIgnoreCase("dz")) {
					//		indexOf.put(NTAB_KEY.DZ, dataIndex);
				} else if(token.equalsIgnoreCase("dy")) {
					//		indexOf.put(NTAB_KEY.DY, dataIndex);
				} else if(token.equalsIgnoreCase("dx")) {
					//		indexOf.put(NTAB_KEY.DX, dataIndex);
				} else if(token.equalsIgnoreCase("z")) {
					indexOf.put(Ntab.Z, dataIndex);
				} else if(token.equalsIgnoreCase("y")) {
					indexOf.put(Ntab.Y, dataIndex);
				} else if(token.equalsIgnoreCase("x")) {
					indexOf.put(Ntab.X, dataIndex);
				} else if(token.equalsIgnoreCase("nuft_ind")) {
					//		indexOf.put(NTAB_KEY.NUFT_INDEX, dataIndex);
				} else if(token.equalsIgnoreCase("element_ref")) {
					//		indexOf.put(NTAB_KEY.ELEMENT_REF, dataIndex);
				} else if(token.equalsIgnoreCase("k")) {
					indexOf.put(Ntab.K, dataIndex);
				} else if(token.equalsIgnoreCase("j")) {
					indexOf.put(Ntab.J, dataIndex);
				} else if(token.equalsIgnoreCase("i")) {
					indexOf.put(Ntab.I, dataIndex);
				} else if(token.equalsIgnoreCase("index")) {
					//		indexOf.put(NTAB_KEY.INDEX, dataIndex);
				} 
				dataIndex++;
			}
			dataValues = tokens.length - dataIndex;
		} else {		
			sc.close();
			return null; // Failed to find a line starting with index
		}

		// Get the last line, expect index, i, j, k
		int lines = 0;
		while(sc.hasNextLine()) {
			sc.nextLine();
			lines++;
		}
		sc.close();
		// Get all the float data out of the file's that make up this scenario

		appendNTABData(dataFile, structure, true, dataIndex, dataValues, lines, indexOf);
		for(File fileToMerge: filesToMerge) {			
			appendNTABData(fileToMerge, structure, false, dataIndex, dataValues, lines, indexOf);
		}	

		return structure;
	}


	private void appendNTABData(File file, DataStructure structure, boolean appendMetaData, int metaDataLength, int timeSteps, int lineCount, Map<Ntab, Integer> indexOf) throws FileNotFoundException {

		System.out.println(file);
		Scanner sc = new Scanner(file); // Skip index line
		while(!sc.nextLine().startsWith("index") && sc.hasNextLine());

		float[][] dataDoubleArray = new float[timeSteps][lineCount];
		// Need unique xyz's only
		HashSet<Float> uniqueXs = new HashSet<Float>();
		HashSet<Float> uniqueYs = new HashSet<Float>();
		HashSet<Float> uniqueZs = new HashSet<Float>();

		int[][] ijks = new int[3][lineCount];

		int maxis = 0;
		int maxjs = 0;
		int maxks = 0;

		for(int i = 0; i < lineCount; i++) {
			// Meta data, only need to do this once
			if(appendMetaData) {
				for(int j = 0; j < metaDataLength; j++) {
					int nextInt = 0;
					float nextFloat = 0f;
					boolean isInt = false;
					if(sc.hasNextInt()) {
						nextInt = sc.nextInt();
						isInt = true;
					} else {
						nextFloat = sc.nextFloat();
					}
					for(Ntab key: indexOf.keySet()) {
						if(indexOf.get(key) == j) {						
							if(key == Ntab.I) {
								if(i == lineCount - 1)
									maxis = nextInt;
								ijks[0][i] = nextInt;
							} else if(key == Ntab.J) {
								ijks[1][i] = nextInt;
								if(i == lineCount - 1)
									maxjs = nextInt;
							} else if(key == Ntab.K) {
								ijks[2][i] = nextInt;		
								if(i == lineCount - 1)
									maxks = nextInt;
							} else if(key == Ntab.X) {
								uniqueXs.add(isInt ? nextInt: nextFloat);
								//  xyzs[0][i] = isInt ? nextInt: nextFloat;
							} else if(key == Ntab.Y) {
								uniqueYs.add(isInt ? nextInt: nextFloat);
								//	xyzs[1][i] = isInt ? nextInt: nextFloat;
							} else if(key == Ntab.Z) {
								uniqueZs.add(isInt ? nextInt: nextFloat);
								//	xyzs[2][i] = isInt ? nextInt: nextFloat;						
							}								
						}
					}
				}
				// Just skip the meta data
			}  else {
				for(int j = 0; j < metaDataLength; j++) {
					//if(sc.hasNext())
					sc.next();
					//else System.out.println(metaDataLength + " expected, at " + j);
				}
			}
			// sc should point to the first float, we can parse until the end of the line
			for(int j = 0; j < timeSteps; j++) {
				dataDoubleArray[j][i] = sc.nextFloat();
			}				
		}
		sc.close();		

		// Get a nice name for this files data set
		String fieldKey = "unknown";
		// Need to figure out the key -> file is named scenario.variable.extension
		String[] fileName = file.getName().split("\\.");
		if(fileName.length > 2)
			fieldKey = fileName[1];
		else 
			fieldKey = fileName[0].split("\\d+")[0];

		if(structure.data == null) {
			structure.data = new HashMap<String, float[][]>();
		}
		structure.data.put(fieldKey, dataDoubleArray);

		ArrayList<Float> xs = new ArrayList<Float>(uniqueXs);
		ArrayList<Float> ys = new ArrayList<Float>(uniqueYs);
		ArrayList<Float> zs = new ArrayList<Float>(uniqueZs);

		Collections.sort(xs);
		Collections.sort(ys);
		Collections.sort(zs);

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
		}

		if(appendMetaData) {
			structure.x = x;
			structure.y = y;
			structure.z = z;
			structure.i = maxis;
			structure.j = maxjs;
			structure.k = maxks;
		}			
	}

	public Object[] getDataTypes(String fileType) throws GridError {
		if(fileType.equals(FileBrowser.STOMP)) {
			return extractStompData().getFieldNames().toArray();
		} else if(fileType.equals(FileBrowser.NTAB)) {
			List<String> fieldNames = new ArrayList<String>();			
			List<File> allFiles = new ArrayList<File>(filesToMerge);
			allFiles.add(dataFile);
			for(File fileToMerge: allFiles) {
				String fieldKey = "unknown";				
				String[] fileName = fileToMerge.getName().split("\\.");
				if(fileName.length > 2)
					fieldKey = fileName[1];
				else 
					fieldKey = fileName[0].split("\\d+")[0];
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
}
