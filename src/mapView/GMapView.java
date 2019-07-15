package mapView;

import java.util.List;
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
import javafx.application.Application;
import netscape.javascript.JSObject;
import javafx.scene.Scene;
import javafx.stage.Stage;
/**
 * Implements Google Maps via GMapsFX 
 * Credits to rterp @ http://rterp.github.io/GMapsFX/
 * This class is the primary driver class that displays our google map with the well locations.
 * @author huan482
 *
 */
public class GMapView extends Application implements MapComponentInitializedListener {
	private static final int INIT_ZOOM = 13; 
	
	private GoogleMapView myMapView;
	
	private GoogleMap myMap;
	
	private MapOptions myMapOpts;
	
	private Stage myStage;
	
	private double myMinBoundsX;
	
	private double myMinBoundsY;
	
	private double myMaxBoundsX;
	
	private double myMaxBoundsY;
	
//	private List<Float> myXGrid;
//	
//	private List<Float> myYGrid;
	
	private List<Float> mySouthWestXLongitudes;
	
	private List<Float> mySouthWestYLatitudes;
	
	private List<Float> myNorthEastXLongitudes;
	
	private List<Float> myNorthEastYLatitudes;
	
//	private List<IJ> myBoxes;
//	
//	private List<Float> myXLines;
//	
//	private List<Float> myYLines;
	
//	private CoordinateConversion converter = new CoordinateConversion();
	
//	private double myMinBoundsXUTM;
//	
//	private double myMaxBoundsXUTM;
//	
//	private double myMinBoundsYUTM;
//	
//	private double myMaxBoundsYUTM;
	
	/**
	 * JavaFX start method.
	 */
	@Override
	public void start(final Stage theStage) throws Exception {
//		myXGrid = new ArrayList<Float>();
//		myYGrid = new ArrayList<Float>();
		myStage = theStage;
		initVariables();
		//Google Maps API Key, 2nd parameter
		myMapView = new GoogleMapView("en", "AIzaSyCqMjOt2Q17PnE9-9843sutOpihbglC_6k");
		myMapView.addMapInializedListener(this);

		//My Scene
		Scene scene = new Scene(myMapView);
		
		//The stage
		myStage.setTitle("Well Locations");
		myStage.setScene(scene);
		myStage.show();
		
	}
	
	/**
	 * Initializes the map by specifying the launch center of our map and also draws the bounds and rectangles.
	 */
	@Override
	public void mapInitialized() {
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
//		myXLines = new  ArrayList<Float>();
//		myYLines = new  ArrayList<Float>();
//		myXGrid = GMapInitVar.getXGrid();
//		myYGrid = GMapInitVar.getYGrid();
//		myXLines = GMapInitVar.getMyXLines();
//		myYLines = GMapInitVar.getMyYLines();
//		myBoxes = GMapInitVar.getMyBoxes();
		myMinBoundsX = GMapInitVar.getMinBoundX();
		myMinBoundsY = GMapInitVar.getMinBoundY();
		myMaxBoundsX = GMapInitVar.getMaxBoundX();
		myMaxBoundsY= GMapInitVar.getMaxBoundY();
		
//		String[] tempMin = converter.latLon2UTM(myMinBoundsY, myMinBoundsX).split(" ");
//		String[] tempMax = converter.latLon2UTM(myMaxBoundsY, myMaxBoundsX).split(" ");
//		
//		myMinBoundsXUTM = Double.parseDouble(tempMin[2]);
//		myMaxBoundsXUTM = Double.parseDouble(tempMax[2]);
//		myMinBoundsYUTM = Double.parseDouble(tempMin[3]);
//		myMaxBoundsYUTM = Double.parseDouble(tempMax[3]);
		
		myNorthEastXLongitudes = GMapInitVar.getMyNorthEastXLongitude();
		myNorthEastYLatitudes = GMapInitVar.getMyNorthEastYLatitude();
		mySouthWestXLongitudes = GMapInitVar.getMySouthWestXLongitude();
		mySouthWestYLatitudes = GMapInitVar.getMySouthWestYLatitude();
//		System.out.println("Bounds: " + myMinBoundsX + " " + myMinBoundsY + " " + myMaxBoundsX + " " + myMaxBoundsY);
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
	 * Creates the rectangles that represent out well locations.
	 */
	private void createRectangles() {
		for (int i = 0; i < myNorthEastXLongitudes.size(); i++) {
			LatLong SouthWest = new LatLong(mySouthWestYLatitudes.get(i), mySouthWestXLongitudes.get(i));
			LatLong NorthEast = new LatLong(myNorthEastYLatitudes.get(i), myNorthEastXLongitudes.get(i));
			LatLongBounds rectangleBounds = new LatLongBounds(SouthWest,NorthEast);
			RectangleOptions rectOpts = new RectangleOptions()
					.bounds(rectangleBounds)
					.strokeColor("black")
					.strokeWeight(2)
					.clickable(true)
					.fillColor("black");
			Rectangle rect = new Rectangle(rectOpts);
			myMap.addMapShape(rect);
			myMap.addUIEventHandler(rect, UIEventType.click, (JSObject obj) -> {
				if (rect.getJSObject().getMember("fillColor").equals("black")) {
					myMap.removeMapShape(rect);
					rectOpts.bounds(rect.getBounds()).fillColor("white");
					rect.setRectangleOptions(rectOpts);
					myMap.addMapShape(rect);
				} else {			
					myMap.removeMapShape(rect);
					rectOpts.bounds(rect.getBounds()).fillColor("black");
					rect.setRectangleOptions(rectOpts);
					myMap.addMapShape(rect);
				}						
			});
		}
	} 
	
//	private void deselectWells(final LatLongBounds theBounds) {
//		ArrayList<Integer> iList = new ArrayList<Integer>();
//		ArrayList<Integer> jList = new ArrayList<Integer>();
//		
//		for (int i = 0; i < myXLines.size() - 1; i++) {
//			System.out.println(myXLines.get(i + 1) + " " + myMinBoundsXUTM);
//			if (Math.round(myXLines.get(i + 1)) > (int) myMinBoundsXUTM && Math.round(myXLines.get(i)) < myMaxBoundsXUTM) {
//				System.out.println("Got here X");
//				iList.add(i + 1);
//			}
//		}
//		System.out.println(iList.size() + " " + iList.toString());
//		for (int i = 0; i < myYLines.size() - 1; i++) {
//			System.out.println(myYLines.get(i + 1) + " " + myMinBoundsYUTM);
//			if (Math.round(myYLines.get(i + 1)) > (int) myMinBoundsYUTM && Math.round(myYLines.get(i)) < myMaxBoundsYUTM) {
//				System.out.println("Got here Y");
//				jList.add(i + 1);
//			}
//		}
//		double midX = ((theBounds.getSouthWest().getLongitude()
//				+ theBounds.getNorthEast().getLongitude()) / 2);
//		
//		
//		double midY = ((theBounds.getSouthWest().getLatitude() 
//				+ theBounds.getNorthEast().getLatitude()) / 2);	
//		
//		for (IJ box : myBoxes) {
////			System.out.println(iList.size());
////			System.out.println(box.i);
//			if (iList.contains(box.i) && jList.contains(box.j)) {
//				System.out.println("WoW it exists.");
//			}
////			System.out.println(box.i + " " + box.j + " " 
////					+ "midX & midY: " + midX + " " + midY);
//		}
//		
//						
//	}
	
}
