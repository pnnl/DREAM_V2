package gravity;

import java.util.OptionalDouble;

import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.event.UIEventType;
import com.lynden.gmapsfx.javascript.object.GoogleMap;
import com.lynden.gmapsfx.javascript.object.LatLong;
import com.lynden.gmapsfx.javascript.object.MapOptions;
import com.lynden.gmapsfx.javascript.object.MapTypeIdEnum;
import com.lynden.gmapsfx.javascript.object.Marker;
import com.lynden.gmapsfx.javascript.object.MarkerOptions;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class GravGMap extends Application implements MapComponentInitializedListener {

	private GoogleMapView myMapView;

	private GoogleMap myMap;

	private MapOptions myMapOpts;

	private Stage myStage;

	@Override
	public void mapInitialized() {
		myMapOpts = new MapOptions();
		// Opens up console for debugging purposes
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
		
		OptionalDouble averageLat = InitGMap.getLatVal().stream().mapToDouble(a -> a).average();
		OptionalDouble averageLong = InitGMap.getLongVal().stream().mapToDouble(b -> b).average();
		myMapOpts.center(new LatLong(averageLat.getAsDouble(), averageLong.getAsDouble()))
				 .mapType(MapTypeIdEnum.ROADMAP)
				 .overviewMapControl(false)
				 .panControl(false)
				 .rotateControl(false)
				 .scaleControl(false)
				 .streetViewControl(false)
				 .zoomControl(false).zoom(10);
		
		myMap = myMapView.createMap(myMapOpts);
		addMarkers();
	}

	@Override
	public void start(final Stage theStage) throws Exception {
		myStage = theStage;
		// Google Maps API Key, 2nd parameter
		myMapView = new GoogleMapView("en", "AIzaSyCqMjOt2Q17PnE9-9843sutOpihbglC_6k");
		myMapView.addMapInializedListener(this);

		// My Scene
		Scene scene = new Scene(myMapView);

		// The stage
		myStage.setTitle("Well Locations");
		myStage.setScene(scene);
		myStage.show();
	}

	private void addMarkers() {
		MarkerOptions markerOpts = new MarkerOptions();
		for (int i = 0; i < InitGMap.getLatVal().size(); i++) {
			LatLong ll = new LatLong(InitGMap.getLatVal().get(i), InitGMap.getLongVal().get(i));
			markerOpts.position(ll)
			  .title("Grav Obs Station")
			  .visible(true);
			Marker mark = new Marker(markerOpts);
			myMap.addUIEventHandler(mark,  UIEventType.click, (JSObject obj) -> {
				Label newLabel = new Label(mark.getTitle());
				StackPane newLayout = new StackPane();
				newLayout.getChildren().add(newLabel);
				Scene newScene = new Scene(newLayout, 200, 200);
				Stage newWindow = new Stage();
				newWindow.setTitle("Heatmap");
				newWindow.setScene(newScene);
				newWindow.show();
			});
			myMap.addMarker(mark);	
		}
	}
}
