package gravity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mapView.CoordinateConversion;
import wizardPages.DREAMWizard.STORMData;

public class InitGMap {

	private static List<Float> latValues;

	private static List<Float> longValues;

	private float offSetX = 0;

	private float offSetY = 0;

	private String utmString = "";

//	private float timeStep = 0;

	private List<String> allValues = new ArrayList<String>();

	private STORMData data;

	private File theFolder;

	private int myZone;

	private String myZoneDirection;

	public InitGMap(final String outputDir, final int theZone, final String theZoneDirection, final STORMData theData) {
		data = theData;
		latValues = new ArrayList<Float>();
		longValues = new ArrayList<Float>();
		theFolder = new File(outputDir);
		myZone = theZone;
		myZoneDirection = theZoneDirection;
		calculateUTMValues(theFolder);
	}

	public void initVariables() {
		convertUTMtoLatLong();
	}
	
	private void calculateUTMValues(final File directory) {
		File[] listOfFiles = directory.listFiles((d, name) -> name.endsWith(".fwd"));
		for (int i = 0; i < listOfFiles.length; i++) {
			String theFileName = listOfFiles[i].getName().substring(0, listOfFiles[i].getName().indexOf("."));
			String[] fileTokens = theFileName.split("_");
			// Once the python script is changed these 3 tokens will always be near the end
			// of the file name.
			// TODO: Check if user needs to enter a offset. For right now not needed
			if (data.getSet().getNodeStructure().getUnit("x").equals("ft")) {
				// 3.281 = FT to M conversion factor
				offSetX = (float) (data.getSet().getNodeStructure().getEdgeX().get(0) / 3.281);
				offSetY = (float) (data.getSet().getNodeStructure().getEdgeY().get(0) / 3.281);
			} else {
				offSetX = data.getSet().getNodeStructure().getEdgeX().get(0);
				offSetY = data.getSet().getNodeStructure().getEdgeY().get(0);
			}
			float UTMX = Float.parseFloat(fileTokens[fileTokens.length - 3]) + offSetX;
			float UTMY = Float.parseFloat(fileTokens[fileTokens.length - 2]) + offSetY;
//			timeStep = Float.parseFloat(fileTokens[fileTokens.length - 1]);
			utmString = myZone + " " + myZoneDirection + " " + UTMX + " " + UTMY;
			allValues.add(utmString);
		}
	}
	
	private void convertUTMtoLatLong() {
		CoordinateConversion c = new CoordinateConversion();
		for (String UTM : allValues) {
			double[] theLatLong = c.utm2LatLon(UTM);
			float longitude = (float) theLatLong[1];
			float latitude = (float) theLatLong[0];
			latValues.add(latitude);
			longValues.add(longitude);
		}
	}

	public static List<Float> getLatVal() {
		return latValues;
	}

	public static List<Float> getLongVal() {
		return longValues;
	}
}
