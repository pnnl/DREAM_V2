package mapView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.event.UIEventType;
import com.lynden.gmapsfx.javascript.object.GoogleMap;
import com.lynden.gmapsfx.javascript.object.LatLong;
import com.lynden.gmapsfx.javascript.object.LatLongBounds;
import com.lynden.gmapsfx.javascript.object.MapOptions;
import com.lynden.gmapsfx.javascript.object.MapTypeIdEnum;
import mapView.Rectangle;
import com.lynden.gmapsfx.shapes.RectangleOptions;
import netscape.javascript.JSObject;
import javafx.scene.Scene;
/**
 * Implements Google Maps via GMapsFX 
 * Credits to rterp @ http://rterp.github.io/GMapsFX/
 * This class is the primary driver class that displays our google map with the well locations.
 * @author huan482
 *
 */
public class InitMapScene implements MapComponentInitializedListener {
	private static final int INIT_ZOOM = 13; 
	
	private GoogleMapView myMapView;
	
	private GoogleMap myMap;
	
	private MapOptions myMapOpts;
	
	private double myMinBoundsX;
	
	private double myMinBoundsY;
	
	private double myMaxBoundsX;
	
	private double myMaxBoundsY;
	
	private List<Float> mySouthWestXLongitudes;
	
	private List<Float> mySouthWestYLatitudes;
	
	private List<Float> myNorthEastXLongitudes;
	
	private List<Float> myNorthEastYLatitudes;
	
	private Map<Integer, IJ> myBoxMapping;
	
	private Map<Rectangle, IJ> rectToBox;
	/**
	 * Initializes the google map view and returns the scene containing the map.
	 * @return - The Scene containing the map.
	 */
	public Scene getScene() {
		//Google Maps API Key, 2nd parameter
		myMapView = new GoogleMapView("en", "AIzaSyCqMjOt2Q17PnE9-9843sutOpihbglC_6k");
		myMapView.addMapInializedListener(this);
		Scene temp = new Scene(myMapView);
		//My Scene
		return temp;
	}
	
	/**
	 * Initializes the map by specifying the launch center of our map and also draws the bounds and rectangles.
	 */
	@Override
	public void mapInitialized() {
		initVariables();
		myMapOpts = new MapOptions(); 
		//Opens up console for debugging purposes
//		myMapView.getWebview().getEngine().executeScript("if (!document.getElementById"
//				+ "('FirebugLite')){E = document['createElement' + 'NS']"
//				+ " && document.documentElement.namespaceURI;E = E ?"
//				+ " document['createElement' + 'NS'](E, 'script') "
//				+ ": document['createElement']('script');E['setAttribute']"
//				+ "('id', 'FirebugLite');E['setAttribute']"
//				+ "('src', 'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');"
//				+ "E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']"
//				+ "('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E"
//				+ " = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}");	
		
		myMapOpts.center(new LatLong((myMaxBoundsY + myMinBoundsY) / 2, (myMaxBoundsX + myMinBoundsX) / 2))
				 .mapType(MapTypeIdEnum.ROADMAP)
				 .overviewMapControl(false)
				 .panControl(false)
				 .rotateControl(false)
				 .scaleControl(false) 
				 .streetViewControl(false)
				 .zoomControl(false)
				 .zoom(INIT_ZOOM);
		
		myMap = myMapView.createMap(myMapOpts);
		drawBoundPolyLines();
		createRectangles();
	}
	/** 
	 * Grabs the variables from our GMapInitVar class.
	 */
	private void initVariables() {
		//Initalizes all variables.
		myMinBoundsX = InitMapVars.getMinBoundX();
		myMinBoundsY = InitMapVars.getMinBoundY();
		myMaxBoundsX = InitMapVars.getMaxBoundX();
		myMaxBoundsY= InitMapVars.getMaxBoundY();
		myBoxMapping = new HashMap<Integer, IJ>();
		myNorthEastXLongitudes = InitMapVars.getMyNorthEastXLongitude();
		myNorthEastYLatitudes = InitMapVars.getMyNorthEastYLatitude();
		mySouthWestXLongitudes = InitMapVars.getMySouthWestXLongitude();
		mySouthWestYLatitudes = InitMapVars.getMySouthWestYLatitude();
		myBoxMapping = InitMapVars.getTempMapping();
		rectToBox = new HashMap<Rectangle, IJ>();
	}
	
	/**
	 * Draws the bounds of our entire space via poly lines.
	 */
	private void drawBoundPolyLines() {
		//To draw a rectangle find the NE and SW points of your rectangle (Lat,Long) then draw it.
		LatLong topRight = new LatLong(myMaxBoundsY, myMaxBoundsX);
		LatLong bottomleft = new LatLong(myMinBoundsY, myMinBoundsX);
		//LatLongBounds(SW,NE)
		LatLongBounds llb = new LatLongBounds(bottomleft, topRight);
		RectangleOptions rOpts = new RectangleOptions()
				.bounds(llb)
				.strokeColor("red")
				.strokeWeight(2)
				.fillColor(null)
				.fillOpacity(0);
		Rectangle rt = new Rectangle(rOpts);
		myMap.addMapShape(rt);
//		System.out.println(rOpts.getJSObject().getMember("strokeColor"));
//		for (int i = 0; i < myXGrid.size(); i += 2) {
//			LatLong[] ary = new LatLong[] {new LatLong(myMinBoundsY, myXGrid.get(i)),
//					new LatLong(myMaxBoundsY, myXGrid.get(i))};
//			MVCArray mvc = new MVCArray(ary);
//			PolylineOptions polyOpts = new PolylineOptions()
//					.path(mvc)
//					.strokeColor("red")
//					.strokeWeight(2);
//			Polyline poly = new Polyline(polyOpts);
//			myMap.addMapShape(poly);
//		} 
//		
//		for (int i = 0; i < myYGrid.size(); i+= 2) {
//			LatLong[] ary = new LatLong[] {new LatLong(myYGrid.get(i), myMinBoundsX),
//					new LatLong(myYGrid.get(i), myMaxBoundsX)};
//			MVCArray mvc = new MVCArray(ary);
//			PolylineOptions polyOpts = new PolylineOptions()
//					.path(mvc)
//					.strokeColor("red")
//					.strokeWeight(2);
//			Polyline poly = new Polyline(polyOpts);
//			myMap.addMapShape(poly);
//		}
	}
	
	/**
	 * Creates the rectangles that represent out well locations and also adds the action listeners
	 * required.
	 */
	private void createRectangles() {
		//Loop through all our NE and SW corners.
		for (int i = 0; i < myNorthEastXLongitudes.size(); i++) {
			RectangleOptions rectOpts;
			LatLong SouthWest = new LatLong(mySouthWestYLatitudes.get(i), mySouthWestXLongitudes.get(i));
			LatLong NorthEast = new LatLong(myNorthEastYLatitudes.get(i), myNorthEastXLongitudes.get(i));
			LatLongBounds rectangleBounds = new LatLongBounds(SouthWest,NorthEast);
			if (myBoxMapping.get(i + 1).prohibited) {
				rectOpts = new RectangleOptions()
						.bounds(rectangleBounds)
						.strokeColor("black")
						.strokeWeight(2)
						.clickable(true)
						.fillColor("black");
			} else {
				rectOpts = new RectangleOptions()
						.bounds(rectangleBounds)
						.strokeColor("black")
						.strokeWeight(2)
						.clickable(true)
						.fillColor("white");
			}
			Rectangle rect = new Rectangle(rectOpts);
			myMap.addMapShape(rect);
			//Map our rectangle to its corrpsonding box, so when the rectangle is checked or unchecked
			//It's reflected on our exclude locations page.
			rectToBox.put(rect, myBoxMapping.get(i + 1));
			myMap.addUIEventHandler(rect, UIEventType.click, (JSObject obj) -> {
				System.out.println(rect.getBounds());
				if (rect.getJSObject().getMember("fillColor").equals("black")) {
					//Change color and then uncheck our box.
					myMap.removeMapShape(rect);
					rectOpts.bounds(rect.getBounds()).fillColor("white");
					rect.setRectangleOptions(rectOpts);
					myMap.addMapShape(rect);
					rectToBox.get(rect).prohibited = false;
				} else {			
					myMap.removeMapShape(rect);
					rectOpts.bounds(rect.getBounds()).fillColor("black");
					rect.setRectangleOptions(rectOpts);
					myMap.addMapShape(rect);
					rectToBox.get(rect).prohibited = true;
				}						
			});
		}
	}
}
