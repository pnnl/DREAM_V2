package mapView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import utilities.Constants;
public class theMapView {
	
	private List<IJ> myGridBoxes;
	
	private static List<Float> myListofXLines;
	
	private static List<Float> myListofYLines;
	
	private static int myZone;
	
	private static String myZoneDirection;
	
	private int myMinX;
	
	private int myMinY;
	
	private static double myLatitude;
	
	private static double myLongitude;
	
//	private Viewer myViewer;
	
	
	public theMapView(final List<IJ> theGridBoxes, final List<Float> xLines, final List<Float> yLines,
			final int theZone, final String theZoneDirection, final int theOriginX, final int theOriginY) {
		
		myGridBoxes = theGridBoxes;
		myListofXLines = new ArrayList<Float>(Constants.makeLines((ArrayList<Float>) xLines));
		myListofYLines = new ArrayList<Float>(Constants.makeLines((ArrayList<Float>) yLines));
		myZone = theZone;
		myZoneDirection = theZoneDirection;
		myMinX = theOriginX;
		myMinY = theOriginY;
//		System.out.println(myListofXLines.size()+ " " + myListofYLines.size());
	}
	
	public static void main (final String theArgs[]) {
		buildMap();
	}
	
	private static void buildMap() {
		CoordinateConversion converter = new CoordinateConversion();
		double[] myLatLong = converter.utm2LatLon(myZone + " " + myZoneDirection); // TODO parse this correctly...
		myLatitude = myLatLong[0];
		myLongitude = myLatLong[1];
		int initZoom = 50;
		
		try {
			URL map = new URL("https://maps.googleapis.com/maps/api/staticmap?center=" + myLatitude + "," + myLongitude
					+ "&zoom=" + initZoom +
					"&size=640x640&maptype=satellite&scale=2&key=AIzaSyCqMjOt2Q17PnE9-9843sutOpihbglC_6k");
		} catch (MalformedURLException theException) {
			theException.printStackTrace();
		}
	}
	
}
