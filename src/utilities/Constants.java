package utilities;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/**
 * Constants for use throughout code, the booleans at the top and random seed can be changed to alter functionality
 * @author port091
 * @author rodr144
 */

public class Constants {
	
	// These probably be eventually wrapped into the code directly, but for now give us functionality for debugging/developing extensions without breaking the working release
	public static boolean buildDev = false;
	public static boolean runThreaded = true;
	public static boolean runScripts = true;
	public static boolean useParetoOptimal = true;
	
	static File userDirectory = new File(System.getProperty("user.dir"));
	public static String userDir = userDirectory.getPath();
	public static String parentDir = userDirectory.getParent();
	
	public static Color black = new Color(Display.getCurrent(), 0, 0, 0);
	public static Color red = new Color(Display.getCurrent(), 255, 0, 0);
	public static Color grey = new Color(Display.getCurrent(), 109, 109, 109);
	public static Color white = new Color(Display.getCurrent(), 255, 255, 255);
	
	public enum ModelOption {
		
		INDIVIDUAL_SENSORS_2("Individual Sensors 2.0", "Prioritize moving a well over adjusting sensors in a well."),
		ALL_SENSORS("Aggregated Sensor Technology", "Place a sensor of every type at each monitoring location");
		//NO LONGER SUPPORTED: these used to be options and are either depreciated or redundant.
		//INDIVIDUAL_SENSORS("Individual Sensors", "Prioritize adjusting sensors in a well over moving a well."),
		//REALIZED__WELLS("Realized Wells", "Wells have all sensor types at each k index");
		
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

	public static Random random = new Random(1); //Right now this is seeded, this way we have reproducable results. Should probably un-seed for release.
	
	public static DecimalFormat decimalFormat = new DecimalFormat("###.###");
	public static DecimalFormat percentageFormat = new DecimalFormat("###.##");
	public static DecimalFormat exponentialFormat = new DecimalFormat("0.00000000E00");
	public static DecimalFormat exponentialFormatShort = new DecimalFormat("0.0E00");
	
	private static Logger LOGGER;
	private static boolean loggerOn;
	
	public static String homeDirectory = System.getProperty("user.home");
	
	public static Timer timer = new Constants().new Timer();
	
	// This function takes a 0-indexed index and returns a 1-indexed node number. (See above)
	public static int getNodeNumber(Point3i ijkDimensions, int index) {
		return getNodeNumber(ijkDimensions.getI(), ijkDimensions.getJ(), ijkDimensions.getK(), index);
	}
	
	public static Integer getNodeNumber(int iMax, int jMax, int kMax, int index) {
		return (index % kMax) * iMax * jMax + (index/kMax % jMax) * iMax + (index/(kMax*jMax) % iMax + 1);
	}
	
	// This function takes a 1-indexed node number and returns a 0-indexed index. (See above)
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
		// TODO: want this to let go of lock
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
	
	public static boolean isValidFloat(String string) {
		try {
			Float.parseFloat(string);
			return true;
		} catch (NumberFormatException ne) {
			return false;
		}
	}
	
	public static boolean isValidInt(String string) {
		try {
			Integer.parseInt(string);
			return true;
		} catch (NumberFormatException ne) {
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	public static boolean isValidFile(String string) {
		try {
			File fileTest = new File(string);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static float[] listToArray(List<Float> list) {
		float[] array = new float[list.size()];
		for(int i=0; i<list.size(); i++) {
			array[i] = list.get(i);
		}
		return array;
	}
	
	public static List<Float> arrayToList(float[] array) {
		List<Float> list = new ArrayList<Float>();
		for(int i=0; i<array.length; i++) {
			list.add(array[i]);
		}
		return list;
	}
}
