package mapView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.scene.Scene;
import utilities.Point3i;

/**
 * This class sets up the variables for the google maps.
 * Note: this class can be cleaned up substantially (On my To Do list).
 * @author huan482
 */

public class InitMapVars {

	private final static float FT_TO_M_CONVERSION_FACTOR = (float) 3.281;

	private static int myZone;

	private static String myZoneDirection;

	private static String myUnit;

	private static Float minBoundsX;

	private static Float maxBoundsX;

	private static Float minBoundsY;

	private static Float maxBoundsY;

	private List<Float> myXEdge;

	private List<Float> myYEdge;

	private static List<Point3i> myValidNodePoints;

	private static List<Float> myNorthEastXLongitude;

	private static List<Float> myNorthEastYLatitude;

	private static List<Float> mySouthWestXLongitude;

	private static List<Float> mySouthWestYLatitude;

	private CoordinateConversion myConverter;

	private static List<IJ> myBoxes;
	
	private static Map<Integer, IJ> myTempMapping;
	
	private InitMapScene map;
	
	public InitMapVars(final List<IJ> theBoxes, final List<Float> theXEdge, final List<Float> theYEdge,
			final int theZoneNumber, final String theZone, final String theUnit,
			final List<Point3i> theValidNodePoints) {
		myBoxes = theBoxes;
		myConverter = new CoordinateConversion();
		myNorthEastXLongitude = new ArrayList<Float>();
		myNorthEastYLatitude = new ArrayList<Float>();
		mySouthWestXLongitude = new ArrayList<Float>();
		mySouthWestYLatitude = new ArrayList<Float>();
		myValidNodePoints = theValidNodePoints;
		myXEdge = theXEdge;
		myYEdge = theYEdge;
		myUnit = theUnit;
		myZone = theZoneNumber;
		myZoneDirection = theZone;
		myTempMapping = new HashMap<Integer, IJ>();
		map = new InitMapScene();
	}

	/**
	 * Initializes all the variables that are going to be passed to the GMapView
	 * class.
	 */
	public void initVariables() {
		getNEandSWcoordinates();
		findBounds();
	}

	/**
	 * Finds the NorthEast and SouthWest coordinates of the wells we're going to be
	 * drawn. Required to draw our rectangle well locations.
	 */
//	@SuppressWarnings({ "unlikely-arg-type"})
	// TODO: check for wells that are already created maybe start at the GMapView
	// class
	private void getNEandSWcoordinates() {
		int counter = 0;
		List<Float> myNorthEastX = new ArrayList<Float>();
		List<Float> myNorthEastY = new ArrayList<Float>();
		List<Float> mySouthWestX = new ArrayList<Float>();
		List<Float> mySouthWestY = new ArrayList<Float>();
		//Convert our points into Lat Long and more specifically the NorthEast and SouthWest corners.
		for (Point3i thePoint : myValidNodePoints) {
			counter++;
			if (myUnit.equals("ft")) {
				//If the units given are in feet we need to convert to meter
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
			//Maps correct Box to the location seen in our Google Map View.
			for (IJ box : myBoxes) {
				if (thePoint.getI() == box.i && thePoint.getJ() == box.j) {
					myTempMapping.put(counter, box);
				}
			}
		}
		//Converting my UTM values into Lat/Long for Google Maps.
		for (int i = 0; i < myNorthEastX.size(); i++) {
			double[] myLatLongNEY = myConverter.utm2LatLon(
					(myZone + " " + myZoneDirection + " " + myNorthEastX.get(i) + " " + myNorthEastY.get(i)));
			double[] myLatLongSWY = myConverter.utm2LatLon(
					(myZone + " " + myZoneDirection + " " + mySouthWestX.get(i) + " " + mySouthWestY.get(i)));
			myNorthEastXLongitude.add((float) myLatLongNEY[1]);
			mySouthWestXLongitude.add((float) myLatLongSWY[1]);
			myNorthEastYLatitude.add((float) myLatLongNEY[0]);
			mySouthWestYLatitude.add((float) myLatLongSWY[0]);
		}
	}

	/**
	 * This method finds the max and min bounds to create a rectangle that spans the our entire space.
	 */
	private void findBounds() {
		double[] boundsSW;
		double[] boundsNE;
		//Need to convert the bounds if they're feet.
		if (myUnit.equals("ft")) {
			boundsSW = myConverter.utm2LatLon(
					myZone + " " + myZoneDirection + " " + Collections.min(myXEdge) / FT_TO_M_CONVERSION_FACTOR
					+ " " + Collections.min(myYEdge) / FT_TO_M_CONVERSION_FACTOR);
			boundsNE = myConverter.utm2LatLon(
					myZone + " " + myZoneDirection + " " + Collections.max(myXEdge) / FT_TO_M_CONVERSION_FACTOR
					+ " " + Collections.max(myYEdge) / FT_TO_M_CONVERSION_FACTOR);
		} else {
			boundsSW = myConverter.utm2LatLon(
					myZone + " " + myZoneDirection + " " + Collections.min(myXEdge) + " " + Collections.min(myYEdge));
			boundsNE = myConverter.utm2LatLon(
					myZone + " " + myZoneDirection + " " + Collections.max(myXEdge) + " " + Collections.max(myYEdge));
		}
		minBoundsX = (float) boundsSW[1];
		maxBoundsX = (float) boundsNE[1];
		minBoundsY = (float) boundsSW[0];
		maxBoundsY = (float) boundsNE[0];
	}
	public static Float getMinBoundX() {
		return minBoundsX;
	}

	public static Float getMinBoundY() {
		return minBoundsY;
	}

	public static Float getMaxBoundX() {
		return maxBoundsX;
	}

	public static Float getMaxBoundY() {
		return maxBoundsY;
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
	
	public static Map<Integer, IJ> getTempMapping() {
		return myTempMapping;
	}
	public Scene getScene() {
		return map.getScene();
	}
}
