package mapView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utilities.Point3i;


/**
 * This class sets up the variables for the google maps.
 * @author huan482
 */

public class GMapInitVar {
	
	private final static float FT_TO_M_CONVERSION_FACTOR = (float) 3.281;
	
	private static List<Float> xlines = new ArrayList<Float>();
	
	private static List<Float> ylines = new ArrayList<Float>();

	private List<Double> myX;

	private List<Double> myY;

	
	
	private static int myZone;
	
	private static String myZoneDirection;
	
	private static String myUnit;

	private static Double minBoundsX;

	private static Double maxBoundsX;

	private static Double minBoundsY;

	private static Double maxBoundsY;
	
	private List<Float> myXEdge;
	
	private List<Float> myYEdge;
	
	private static List<Float> myXGrid;
	
	private static List<Float> myYGrid;
	
	private static List<Point3i> myValidNodePoints;
	
	private static List<Float> myNorthEastXLongitude;
	
	private static List<Float> myNorthEastYLatitude;
	
	private static List<Float> mySouthWestXLongitude;
	
	private static List<Float> mySouthWestYLatitude;
	
	private CoordinateConversion myConverter;
	
	private static List<IJ> myBoxes;
	
	private Map<Rectangle, String> rectangleToUtm;
	
	public GMapInitVar(final List<IJ> theBoxes,final List<Float> theXEdge, final List<Float> theYEdge,
			final int theZoneNumber, final String theZone, final String theUnit,
			final List<Point3i> theValidNodePoints) {
		myBoxes = theBoxes;
		myConverter = new CoordinateConversion();
		myNorthEastXLongitude = new ArrayList<Float>();
		myNorthEastYLatitude = new ArrayList<Float>();
		mySouthWestXLongitude = new ArrayList<Float>(); 
		mySouthWestYLatitude = new ArrayList<Float>();
		myValidNodePoints = theValidNodePoints;
		myX = new ArrayList<Double>();
		myY = new ArrayList<Double>();
		myXGrid = new ArrayList<Float>();
		myYGrid = new ArrayList<Float>();
		myXEdge = theXEdge;
		myYEdge = theYEdge;
		myUnit = theUnit;
		myZone = theZoneNumber;
		myZoneDirection = theZone;
		rectangleToUtm = new HashMap<Rectangle, String>();
	}
	
	/**
	 * Initializes all the variables that are going to be passed to the GMapView class.
	 */
	public void initVariables() {
		myBoundLines();
		findBounds();
		getNEandSWcoordinates();
	}
	
	/**
	 * Finds the bounds of the entire space. Also converts UTM to Lat Long.
	 */
	private void myBoundLines() {
		for (float x : myXEdge) {
			if (myUnit.equals("ft")) x /= FT_TO_M_CONVERSION_FACTOR;
			double[] myLatLong = myConverter.utm2LatLon(myZone + " " + myZoneDirection 
					+ " " + x + " " + 0);
			myXGrid.add((float) myLatLong[1]);
			myX.add(myLatLong[1]);
		}
		for (float y : myYEdge) {
			if (myUnit.equals("ft")) y /= FT_TO_M_CONVERSION_FACTOR;
			double[] myLatLong = myConverter.utm2LatLon(myZone + " " + myZoneDirection
					+ " " + 0 + " " + y);
			myYGrid.add((float) myLatLong[0]);
			myY.add(myLatLong[0]);
		}
		
	}
	
	/**
	 * Finds the NorthEast and SouthWest coordinates of the wells we're going to be drawn.
	 * Required to draw our rectangle well locations.
	 */
//	@SuppressWarnings({ "unlikely-arg-type"})
	//TODO: check for wells that are already created maybe start at the GMapView class
	private void getNEandSWcoordinates() {
		List<Float> myNorthEastX = new ArrayList<Float>();
		List<Float> myNorthEastY = new ArrayList<Float>();
		List<Float> mySouthWestX = new ArrayList<Float>();
		List<Float> mySouthWestY = new ArrayList<Float>();
		for (Point3i thePoint : myValidNodePoints) {
			if (myUnit.equals("ft")) {
				myNorthEastX.add(myXEdge.get(thePoint.getI() + 1) / FT_TO_M_CONVERSION_FACTOR);
				myNorthEastY.add(myYEdge.get(thePoint.getJ()) / FT_TO_M_CONVERSION_FACTOR);
				mySouthWestX.add(myXEdge.get(thePoint.getI()) / FT_TO_M_CONVERSION_FACTOR);
				mySouthWestY.add(myYEdge.get(thePoint.getJ() + 1) / FT_TO_M_CONVERSION_FACTOR);
			} else {
				myNorthEastX.add(myXEdge.get(thePoint.getI() + 1));
				myNorthEastY.add(myYEdge.get(thePoint.getJ()));
				mySouthWestX.add(myXEdge.get(thePoint.getI()));
				mySouthWestY.add(myYEdge.get(thePoint.getJ() + 1));
			}
		}
		for (int i = 0; i < myNorthEastX.size(); i++) {
			float tempNEX = myNorthEastX.get(i);
			float tempSWX = mySouthWestX.get(i);
			float tempNEY = myNorthEastY.get(i);
			float tempSWY = mySouthWestY.get(i);
			double [] myLatLongNEX = myConverter.utm2LatLon((myZone + " " + myZoneDirection +
					" " + tempNEX + " " + 0));
			double [] myLatLongSWX = myConverter.utm2LatLon((myZone + " " + myZoneDirection +
					" " + tempSWX + " " + 0));
			double [] myLatLongNEY = myConverter.utm2LatLon((myZone + " " + myZoneDirection +
					" " + 0 + " " + tempNEY));
			double [] myLatLongSWY = myConverter.utm2LatLon((myZone + " " + myZoneDirection +
					" " + 0 + " " + tempSWY));
			myNorthEastXLongitude.add((float) myLatLongNEX[1]);
			mySouthWestXLongitude.add((float) myLatLongSWX[1]);
			myNorthEastYLatitude.add((float) myLatLongNEY[0]);
			mySouthWestYLatitude.add((float) myLatLongSWY[0]);
		}
	}
	
	/**
	 * Finds the bounds of our entire space.
	 */
	private void findBounds() {
		minBoundsX = Collections.min(myX);
		maxBoundsX = Collections.max(myX);
		minBoundsY = Collections.min(myY);
		maxBoundsY = Collections.max(myY);
	}

	public static List<Float> getXLines() {
		return xlines;
	}
  
	public static List<Float> getYLines() {
		return ylines;
	}

	public static Double getMinBoundX() {
		return minBoundsX;
	}

	public static Double getMinBoundY() {
		return minBoundsY;
	}

	public static Double getMaxBoundX() {
		return maxBoundsX;
	}

	public static Double getMaxBoundY() {
		return maxBoundsY;
	}
	public static List<Float> getXGrid() {
		return myXGrid;
	}
	
	public static List<Float> getYGrid() {
		return myYGrid;
	}
	
	public static List<Float> getMyNorthEastXLongitude() {
		return myNorthEastXLongitude;
	}
	
	public static List<Float> getMyNorthEastYLatitude() {
		return myNorthEastYLatitude;
	}
	
	public static List<Float> getMySouthWestXLongitude() {
		return mySouthWestXLongitude;
	}
	
	public static List<Float> getMySouthWestYLatitude() {
		return mySouthWestYLatitude;
	}
	
	public static List<IJ> getMyBoxes() {
		return myBoxes;
	}
	
}
