package gridviz;

import java.io.BufferedReader;
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
import java.util.TreeMap;

import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.apache.commons.io.FileUtils;

/**
 * @brief  Provides a parser to extract DataGrid objects from Stomp data files
 * @author Tucker Beck
 * @date   3/7/12
 */
public class GridParser {


	public static enum FILE_TYPE { PLOT, NTAB};

	public FILE_TYPE fileType = FILE_TYPE.PLOT;

	/// Describes the data file from which this parser will extract data
	private File dataFile;

	private int timeStep = 0;

	private static enum NTAB_KEY {
		INDEX("index"), I("i"), J("j"), K("k"), 
		ELEMENT_REF("element reference"), 
		NUFT_INDEX("nuft index"), 
		X("x"), Y("y"), Z("z"), DX("dx"), DY("dy"), DZ("dz"), VOLUME("volume"), DATA("data");
		private String key;
		NTAB_KEY(String key) {
			this.key = key;
		}
		public String getKey() {
			return key;
		}
	}

	private List<File> filesToMerge = new ArrayList<File>();

	// STOMP keys used for parsing the header information
	private static enum STOMP_KEYS {
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

		STOMP_KEYS(String key, boolean integer) {
			this.key = key;
			this.integer = integer;
		};

		STOMP_KEYS(String key) {
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

	/**
	 * Construct the parser
	 * @param fileName The name of the file from which to extract data
	 */
	public GridParser(String fileName) {
		dataFile = new File(fileName);
	}

	public GridParser(String fileName, int timeStep) {
		dataFile = new File(fileName);
		fileType = FILE_TYPE.NTAB;	
		this.timeStep = timeStep;
	}

	public void mergeFile(File file) {
		filesToMerge.add(file);
	}

	public DataGrid parseStompFile() throws GridError {
		// will contain all the extracted data from the file
		DataGrid grid = null;

		// Prints file read information
		boolean debug = false;

		// This will hold the size of the grid to extract
		Point3i size = new Point3i(0, 0, 0);
		Vector3f center = new Vector3f();
		boolean hasCenter = false;

		// Attempt to open the data file
		Scanner dataScanner = null;

		Map<STOMP_KEYS, Object> info = new HashMap<STOMP_KEYS, Object>();

		float timestep = 0; // Time in years

		try {

			dataScanner = new Scanner(new FileReader(dataFile));

			// Fetch the grid configuration information from the plot file
			while (dataScanner.hasNextLine() && !dataScanner.hasNext("[XYZ]\\-Direction")) {

				String line = dataScanner.nextLine().trim();

				// Modified 2.11.13 to handle new data types

				int splitIndex = line.indexOf('=') + 1;
				String substring = line.substring(splitIndex).trim();

				if(line.startsWith("Time") && line.contains("=")) {
					String[] tokens = line.split("[, ]");
					if(tokens.length >= 2) {
						try {
							timestep = Float.parseFloat(tokens[tokens.length-2]);
						} catch (Exception e) {	
							System.out.println("years: " + tokens[tokens.length-2]);
						}

					}
				}

				boolean foundLine = false;
				for(STOMP_KEYS key: STOMP_KEYS.values()) {
					if(line.startsWith(key.getKey())) {
						if(key.isInteger())
							info.put(key, Integer.parseInt(substring));
						else
							info.put(key, Float.parseFloat(substring));
						if(debug) System.out.println("Reading [" + key.getKey() + "] - " + line);	
						foundLine = true;
						break; // Already handled it, skip the rest of the keys
					} 					
				}
				if(!foundLine && debug) {
					//zoe taking this out for now as it was spamming the log
					//System.out.println("Skipping - " + line);	
				}
			}

			size.x = (Integer)info.get(STOMP_KEYS.X_NODES);
			size.y = (Integer)info.get(STOMP_KEYS.Y_NODES);
			size.z = (Integer)info.get(STOMP_KEYS.Z_NODES);

			// If we have origin information we need to increment the size value
			for(STOMP_KEYS originKey: new STOMP_KEYS[] {STOMP_KEYS.X_ORGIN_HEXAHEDRA, STOMP_KEYS.X_ORGIN_SURFACE, STOMP_KEYS.X_ORGIN}) {
				if(info.containsKey(originKey)) {
					center.x = Float.parseFloat(info.get(originKey).toString());
					hasCenter= true;
				}
			}
			for(STOMP_KEYS originKey: new STOMP_KEYS[] {STOMP_KEYS.Y_ORGIN_HEXAHEDRA, STOMP_KEYS.Y_ORGIN_SURFACE, STOMP_KEYS.Y_ORGIN}) {
				if(info.containsKey(originKey)) {
					center.y = Float.parseFloat(info.get(originKey).toString());
					hasCenter= true;
				}
			}
			for(STOMP_KEYS originKey: new STOMP_KEYS[] {STOMP_KEYS.Z_ORGIN_HEXAHEDRA, STOMP_KEYS.Z_ORGIN_SURFACE, STOMP_KEYS.Z_ORGIN}) {
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

			/* The linear index will keep track of where to insert the
			 * data into the array that holds the node data for each field
			 */
			int linearIndex = -1;

			// This will hold the name of the field that is being extracted
			String fieldKey = "";

			// The field values will be first extracted then assigned
			FieldValues values = null;
			boolean nodal = false;

			// Now fill the grid with data
			boolean nextLine = false;
			while (dataScanner.hasNextLine()) {

				String line = dataScanner.nextLine();

				// Ignore empty lines
				if(line.contains("pH"))
					System.out.println("This one");
								
				if (line.isEmpty() || line.trim().isEmpty()) {
					nextLine = true;
					continue;

				} else if (line.matches("\\w.*") || line.contains(",") || nextLine) {	
					nextLine = false;
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
						fieldKey = line;	// Otherwise the key will stay as it is

					if(line.contains("Nodal"))
						nodal = true;

					// Fetch the value set for the given field
					values = grid.getFieldValues(fieldKey.trim());

					// Start the linear index at 0
					linearIndex = 0;
					continue;

				} else {
					nextLine = false;

					// Split the line by any number of spaces between values
					String[] items = line.trim().split("\\s+");

					// Store each value from the line into the value list

					if(nodal) {
						values.setNodalValue(linearIndex, items, grid);
						linearIndex++;
					} else {
						for(String item: items) {

							try{
								float value = Float.parseFloat(item);

								values.setValue(linearIndex, value);

							} catch (NumberFormatException err) {
								// Ignore values that aren't parseable
							} catch (GridError err) {
								System.out.println("Error reading " + fieldKey);
								throw err;
							}

							linearIndex++;
						}
					}
				}
				
			}	
			dataScanner.close();
		} catch (Exception err) {
			if(dataScanner != null)
				dataScanner.close();
			System.out.println(this.dataFile);
			throw new GridError("Extraction failed: " + err.getMessage());
		}


		if(dataScanner != null)
			dataScanner.close();
		
		// A bit of extra work for 2d meshes.
		if(grid.is2D()) {
			// Fetch the value set for the given field
			FieldValues values = grid.getFieldValues(grid.getNormalDirection());
			// Fill the missing edge with a bunch of 0's
			for(int i = 0; i < grid.getGridder().getL(); i++) {
				values.setValue(i, 0.0f);
			}
		}

		dataScanner.close();

		return grid;

	}

	/// Extracts grid data from a stomp file
	public DataGrid extractData() throws GridError {
		if(fileType.equals(FILE_TYPE.PLOT)) {
			return parseStompFile();
		} else {
			throw new GridError("Extract data should only be called for stomp files");
		}
	}

	public NTABStructure extractNTABData() throws Exception {

		NTABStructure structure = new NTABStructure();
		Map<NTAB_KEY, Integer> indexOf = new HashMap<NTAB_KEY, Integer>();

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
							indexOf.put(NTAB_KEY.DATA, dataIndex);
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
					indexOf.put(NTAB_KEY.Z, dataIndex);
				} else if(token.equalsIgnoreCase("y")) {
					indexOf.put(NTAB_KEY.Y, dataIndex);
				} else if(token.equalsIgnoreCase("x")) {
					indexOf.put(NTAB_KEY.X, dataIndex);
				} else if(token.equalsIgnoreCase("nuft_ind")) {
					//		indexOf.put(NTAB_KEY.NUFT_INDEX, dataIndex);
				} else if(token.equalsIgnoreCase("element_ref")) {
					//		indexOf.put(NTAB_KEY.ELEMENT_REF, dataIndex);
				} else if(token.equalsIgnoreCase("k")) {
					indexOf.put(NTAB_KEY.K, dataIndex);
				} else if(token.equalsIgnoreCase("j")) {
					indexOf.put(NTAB_KEY.J, dataIndex);
				} else if(token.equalsIgnoreCase("i")) {
					indexOf.put(NTAB_KEY.I, dataIndex);
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

		appendData(dataFile, structure, true, dataIndex, dataValues, lines, indexOf);
		for(File fileToMerge: filesToMerge) {
			appendData(fileToMerge, structure, false, dataIndex, dataValues, lines, indexOf);
		}	
		
		return structure;
	}

	public class NTABStructure {
		
		public float[] x;
		public float[] y;
		public float[] z;
		
		public int i;
		public int j;
		public int k;

		public Map<String, float[][]> data;
		
	}

	private void appendData(File file, NTABStructure structure, boolean appendMetaData, int metaDataLength, int timeSteps, int lineCount, Map<NTAB_KEY, Integer> indexOf) throws FileNotFoundException {

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
					for(NTAB_KEY key: indexOf.keySet()) {
						if(indexOf.get(key) == j) {						
							if(key == NTAB_KEY.I) {
								if(i == lineCount - 1)
									maxis = nextInt;
								ijks[0][i] = nextInt;
							} else if(key == NTAB_KEY.J) {
								ijks[1][i] = nextInt;
								if(i == lineCount - 1)
									maxjs = nextInt;
							} else if(key == NTAB_KEY.K) {
								ijks[2][i] = nextInt;		
								if(i == lineCount - 1)
									maxks = nextInt;
							} else if(key == NTAB_KEY.X) {
								uniqueXs.add(isInt ? nextInt: nextFloat);
							//  xyzs[0][i] = isInt ? nextInt: nextFloat;
							} else if(key == NTAB_KEY.Y) {
								uniqueYs.add(isInt ? nextInt: nextFloat);
							//	xyzs[1][i] = isInt ? nextInt: nextFloat;
							} else if(key == NTAB_KEY.Z) {
								uniqueZs.add(isInt ? nextInt: nextFloat);
							//	xyzs[2][i] = isInt ? nextInt: nextFloat;						
							}								
						}
					}
				}
				// Just skip the meta data
			}  else {
				for(int j = 0; j < metaDataLength; j++) {
					sc.next();
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
		//	structure.i = ijks[0];
		//	structure.j = ijks[1];
		//	structure.k = ijks[2];
			structure.x = x;
			structure.y = y;
			structure.z = z;
			structure.i = maxis;
			structure.j = maxjs;
			structure.k = maxks;
		}			
	}
	
	public Object[] getDataTypes(boolean ntab) throws GridError {
		if(!ntab) {
			return extractData().getFieldNames().toArray();
		} else {
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
			
		}
	}
}
