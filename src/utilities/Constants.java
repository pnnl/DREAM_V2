package utilities;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import objects.NodeStructure;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.h5.H5File;

public class Constants {
	
	// Catherine change this to true when you want ot build production version!!!
	public static boolean buildDev = false;
	
	public enum ModelOption { 
		
		INDIVIDUAL_SENSORS_2("Individual Sensors 2.0", "Prioritize moving a well over adjusting sensors in a well."),
		INDIVIDUAL_SENSORS("Individual Sensors", "Prioritize adjusting sensors in a well over moving a well."),
		REALIZED__WELLS("Realized Wells", "Wells have all sensor types at each k index");
		
		String alias;
		String description;
		ModelOption(String alias, String description) {
			this.alias = alias;
		}
		
		public String getDescription() {
			return description;
		}
		
		@Override
		public String toString() {
			return alias;
		}
		
	}	

	// "Diana Bacon Runs 03"; 
	public static String RUN_TEST =  "STORM";//"Bacon Base Case with Flux 3-D";//"120517_small_domain_STOMP_runs";//"Bacon Base Case with Flux 3-D";

	public static Random random = new Random(1);/* {

		private static final long serialVersionUID = 1L;
		
		@Override 
		public float nextFloat() {
			float nextFloat = super.nextFloat();
			System.out.println(nextFloat);
			return nextFloat;
		}
		@Override 
		public double nextDouble() {
			double nextDouble = super.nextDouble();
			System.out.println(nextDouble);
			return nextDouble;
		}
		@Override 
		public int nextInt() {
			int nextInt = super.nextInt();
			System.out.println(nextInt);
			return nextInt;
		}
		@Override 
		public int nextInt(int max) {
			int nextInt = super.nextInt(max);
			if(nextInt == 102 || nextInt == 2 || nextInt == 55) {
				System.out.println("This one");
			}
			System.out.println(nextInt);
			return nextInt;
		}
	};*/

	public static DecimalFormat decimalFormat = new DecimalFormat("###.###");
	public static DecimalFormat percentageFormat = new DecimalFormat("###.##");
	public static DecimalFormat exponentialFormat = new DecimalFormat("0.00000000E00");
	
	private static Logger LOGGER;
	private static boolean loggerOn;
	
	public static String homeDirectory = System.getProperty("user.home");

	public static boolean returnAverageTTD = true;
	public static boolean scenarioUnion = true;
	
	public static Map<String, H5File> hdf5Files;
	
	// If we can store this in memory, try it?
	public static Map<String, Map<Integer, Map<String, float[]>>> hdf5Data = new HashMap<String, Map<Integer, Map<String, float[]>>>();
	
	public static Map<String, Map<Integer, Map<String, Map<Integer, Float>>>> hdf5CloudData = new HashMap<String, Map<Integer, Map<String, Map<Integer, Float>>>>();
	
	public static Timer timer = new Constants().new Timer();
			
	public static void loadHdf5Files(String location) {
		
		File hdf5Folder = new File(location);
		hdf5Files = new HashMap<String, H5File>();
		
		boolean readToMemory = false; // TODO: Need a check to see if our size is small enough...
		NodeStructure nodeStructure = null;
		int totalNodes = 0;
		int iMax = 0;
		int jMax = 0;
		int kMax = 0;
		
		long floatCount = 0;
		if(hdf5Folder.exists() && hdf5Folder.isDirectory()) {
			long size = 0;
			for(File file: hdf5Folder.listFiles()) {
				size += file.length();
			}
			System.out.println("Directory size: " + size); // TODO: Pick a size
			
			// Loop through contents for hdf5 files TODO: Check for file size
			for(File file: hdf5Folder.listFiles()) {
				String scenario = file.getName().replaceAll("\\.h5" , "");
				// FileFormat hdf5Format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
				try {
					H5File hdf5File  = new H5File(file.getAbsolutePath(), HDF5Constants.H5F_ACC_RDONLY);	
					hdf5Files.put(scenario,hdf5File);

					if(!readToMemory)
						continue;
					
					if(nodeStructure == null) {
						nodeStructure = new NodeStructure("");
						System.out.println("Domain size: " + nodeStructure.getIJKDimensions());
						totalNodes = nodeStructure.getIJKDimensions().getI()*nodeStructure.getIJKDimensions().getJ()*nodeStructure.getIJKDimensions().getK();
						iMax = nodeStructure.getIJKDimensions().getI();
						jMax = nodeStructure.getIJKDimensions().getJ();
						kMax = nodeStructure.getIJKDimensions().getK();
					}					
					
					// Try and store it into memory?
					if(hdf5Data == null)
						hdf5Data = new HashMap<String, Map<Integer, Map<String, float[]>>>(hdf5Folder.listFiles().length, 0.9f);
					if(!hdf5Data.containsKey(scenario))
						hdf5Data.put(scenario, new HashMap<Integer, Map<String, float[]>>(nodeStructure.getTimeSteps().size(), 0.9f));
					
					// Get the root node:
					Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
					// Get the data group
					for(int ts = 0; ts < root.getMemberList().size(); ts++) {
						String name = root.getMemberList().get(ts).getName();
						if(!name.startsWith("plot"))
							continue;
						Integer timeStep = Integer.parseInt(name.replaceAll("plot", ""));
						Object group =  root.getMemberList().get(ts); // timesteps	
						
						if(!hdf5Data.get(scenario).containsKey(timeStep))
							hdf5Data.get(scenario).put(timeStep, new HashMap<String, float[]>(((Group)group).getMemberList().size(), 0.9f));
						//Object group =  root.getMemberList().get(timestep+1); // timesteps
						for(int i = 0; i < ((Group)group).getMemberList().size(); i++) {
							Object child = ((Group)group).getMemberList().get(i);// z is index 0
							if(child instanceof Dataset) {
								Dataset dataset = (Dataset)child;
								String dataName = dataset.getName();
								int dataset_id = dataset.open();
								float[] dataRead = new float[totalNodes];	 // expecting an array with size of the grid
								H5.H5Dread(dataset_id, HDF5Constants.H5T_NATIVE_FLOAT, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, dataRead);				
								float[] nodeOrder = new float[totalNodes];
								for(int j = 0; j < totalNodes; j++) {
									nodeOrder[getNodeNumber(iMax, jMax, kMax, j)-1] = dataRead[j];
								}								
								hdf5Data.get(scenario).get(timeStep).put(dataName, nodeOrder);
								dataset.close(dataset_id);
								floatCount+=totalNodes;
					//			System.out.println(floatCount);
							}
						}
						
					}	
														
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("#Floats stored in memory: " + floatCount);
	}
	
	/*	
	 *  Example of node number vs. index. Each cell has: 
	 *  1) i,j,k			- each of the three dimensions are 1 <= dim <= dimMax
	 *  2) node number		- 1-indexed, used by DREAM to store which nodes are triggered and to query from nodes
	 *  3) index			- 0-indexed, used in reading values from the hdf5 files.
	 *  _________________________    _________________________    _________________________    
	 * 	| 1,1,1 | 1,2,1 | 1,3,1 |    | 1,1,2 | 1,2,2 | 1,3,2 |    | 1,1,3 | 1,2,3 | 1,3,3 |
	 * 	| 1     | 4     | 7     |    | 10    | 13    | 16    |    | 19    | 22    | 25    |
	 * 	| 0     | 3     | 6     |    | 1     | 4     | 7     |    | 2     | 5     | 8     |
	 * 	|_______|_______|_______|    |_______|_______|_______|    |_______|_______|_______|    
	 * 	| 2,1,1 | 2,2,1 | 2,3,1 |    | 2,1,2 | 2,2,2 | 2,3,2 |    | 2,1,3 | 2,2,3 | 2,3,3 |
	 * 	| 2     | 5     | 8     |    | 11    | 14    | 17    |    | 20    | 23    | 26    |
	 * 	| 9     | 12    | 15    |    | 10    | 13    | 16    |    | 11    | 14    | 17    |
	 * 	|_______|_______|_______|    |_______|_______|_______|    |_______|_______|_______|    
	 * 	| 3,1,1 | 3,2,1 | 3,3,1 |    | 3,1,2 | 3,2,2 | 3,3,2 |    | 3,1,3 | 3,2,3 | 3,3,3 |
	 * 	| 3     | 6     | 9     |    | 12    | 15    | 18    |    | 21    | 24    | 27    |
	 * 	| 18    | 21    | 24    |    | 19    | 22    | 25    |    | 20    | 23    | 26    |
	 * 	|_______|_______|_______|    |_______|_______|_______|    |_______|_______|_______|
	 * 
	 */
	
	/*
	 * This function takes a 0-indexed index and returns a 1-indexed node number. (See above)
	 */
	public static int getNodeNumber(Point3i ijkDimensions, int index) {
		return getNodeNumber(ijkDimensions.getI(), ijkDimensions.getJ(), ijkDimensions.getK(), index);
	}
	
	public static Integer getNodeNumber(int iMax, int jMax, int kMax, int index) {
		return (index % kMax) * iMax * jMax + (index/kMax % jMax) * iMax + (index/(kMax*jMax) % iMax + 1);
	}
	
	/*
	 * This function takes a 1-indexed node number and returns a 0-indexed index. (See above)
	 */
	public static int getIndex(Point3i ijkDimensions, int nodeNumber) {
		return getIndex(ijkDimensions.getI(), ijkDimensions.getJ(), ijkDimensions.getK(), nodeNumber);
	}
	
	public static Integer getIndex(int iMax, int jMax, int kMax, int nodeNumber){
		return ((nodeNumber-1)%iMax)*jMax*kMax + (((nodeNumber-1)/iMax)%jMax)*kMax + (((nodeNumber-1)/(iMax*jMax))%kMax);
	}

	public static void initializeLogger(String homeDirectory, String className) throws SecurityException, IOException {
		//getInstance().
		LOGGER = Logger.getLogger(className);
		loggerOn = true;
		//getInstance().
		LOGGER.setLevel(Level.FINER);	// Level.ALL will log everything (every inference)

		File home = new File(homeDirectory);

		if(!home.exists()) {
			home.mkdir();
		}

		File logFile = new File(home, "STORM.log");	// TODO: append a time stamp?

		if(!logFile.exists()) {
			logFile.createNewFile();
		}

		FileHandler handler = new FileHandler(logFile.getAbsolutePath(), 10485760*2, 1, true); // allow up to 1 meg files?
		handler.setFormatter(new SimpleFormatter());
		//getInstance().
		LOGGER.addHandler(handler);

		log(Level.INFO, "Initialized Logger", null);
	}

	public static void turnLoggerOn(boolean on) {
		loggerOn = on;
	}

	public static void log(Level level, String message, Object obj) {
		// Make sure only one thread modifies the log at a time
		if(!loggerOn)
			return;
		synchronized(LOGGER){//getInstance()){
			if(obj != null) {
				if(obj instanceof String)
					message += ": {0}\n";
				else
					message += "\n------------------------------------------------>\n{0}------------------------------------------------<\n";
			}
			//getInstance().
			LOGGER.log(level, message, obj);			
		}
	}

	public static void disposeLogger() {
		// TODO: wnat this to let go of lock
		//	LOGGER.getH
	}
	
	public class Timer {
		
		private long perConfiguration;
		private long perScenario;
		private long perTime;

		public synchronized void addPerConfiguration(long time) {
			perConfiguration += time;
		}
		

		public synchronized void addPerScenario(long time) {
			perScenario += time;
		}
		

		public synchronized void addPerTime(long time) {
			perTime += time;
		}
		
		@Override 
		public String toString() {
			return "Per configuration, scenario, time: " + perConfiguration + ", " + perScenario + ", " + perTime;
		}
	}
	
	public static ArrayList<Float> makeLines(ArrayList<Float> centers){
		ArrayList<Float> lines = new ArrayList<Float>();
		for(int x = 1; x < centers.size(); x++) {
			float half = (centers.get(x)-centers.get(x-1))/2;
			if(x == 1) 
				lines.add(new Float(centers.get(x-1)-half).floatValue());
			lines.add(new Float(centers.get(x-1)+half).floatValue());
			if(x == centers.size()-1) 
				lines.add(new Float(centers.get(x)+half).floatValue());
		}
		return lines;
	}

}
