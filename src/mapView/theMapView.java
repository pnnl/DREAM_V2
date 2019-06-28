package mapView;

import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import utilities.Constants;
public class theMapView {
	
	private List<IJ> myGridBoxes;
	
	private static List<Float> myListofXLines;
	
	private static List<Float> myListofYLines;
	
	private static int myZone;
	
	private static String myZoneDirection;
	
	private static int myOffsetX;
	
	private static int myOffsetY;
	
	private static double myLatitude;
	
	private static double myLongitude;
	
	private static Float myEasting;
	
	private static Float myNorthing;
	
//	private Viewer myViewer;
	
	
	public theMapView(final List<IJ> theGridBoxes, final List<Float> xLines, final List<Float> yLines,
			final int theZone, final String theZoneDirection, final int theOriginX, final int theOriginY) {
		myEasting = xLines.get(0); //The minimum element of the UTM coordinate.
		myNorthing = yLines.get(0); 
		myGridBoxes = theGridBoxes;
		myListofXLines = new ArrayList<Float>(Constants.makeLines((ArrayList<Float>) xLines));
		myListofYLines = new ArrayList<Float>(Constants.makeLines((ArrayList<Float>) yLines));
		myZone = theZone;
		myZoneDirection = theZoneDirection;
		myOffsetX = theOriginX;
		myOffsetY = theOriginY;
//		System.out.println(myListofXLines.size()+ " " + myListofYLines.size());
	}
	
	public static void main (final String theArgs[]) {
		
	}
	
	public void paintComponent(final Graphics theGraphic) {
		buildMap(theGraphic);
		
	}
	/**
	 * First convert given UTM coordinates to Lat/Long then find the correct google map location.
	 * Build Map URL after this.
	 * @author huan482
	 * @date 6/28/2019
	 */
	private static void buildMap(final Graphics theGraphic) {
		CoordinateConversion converter = new CoordinateConversion();
		String[] temp = converter.latLon2UTM(myOffsetX, myOffsetY).split(" ");
		myEasting = myEasting + Float.parseFloat(temp[2]);
		myNorthing = myNorthing + Float.parseFloat(temp[3]);
		double[] myLatLong = converter.utm2LatLon(myZone + " " + myZoneDirection + " " + myEasting
				+ " " + myNorthing); 
		myLatitude = myLatLong[0];
		if (myLatitude > 90) {
			myLatitude = (myLatitude - 180);
		} else if (myLatitude < -90) myLatitude = (myLatitude + 180);
		
		myLongitude = myLatLong[1];
		int initZoom = 50;
		try {
			URL map = new URL("https://maps.googleapis.com/maps/api/staticmap?center=" + myLatitude + "," + myLongitude
					+ "&zoom=" + initZoom +
					"&size=640x640&maptype=satellite&scale=2&key=AIzaSyCqMjOt2Q17PnE9-9843sutOpihbglC_6k");
			BufferedImage image = ImageIO.read(map);
			AffineTransform at = new AffineTransform();
			AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
			image = scaleOp.filter(image, image);
			theGraphic.drawImage(image, 0, 0, null);
		} catch (IOException theException) {
			theException.printStackTrace();
		}
	}
	
//	private void findArea() {
//		if(myGridBoxes == null) {
//			boxes = new ArrayList<IJ>();
//			for(int i=1; i< xlines.size(); i++){
//				for(int j=1; j< ylines.size(); j++){ //Only iterate through size-1 as we care about boxes, not about corners
//					boxes.add(new IJ(i,j, true, true));
//				}
//			}
//		}
//	}
	
}
