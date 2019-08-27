package objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for a particular distribution of sensors and its quality
 * @author port091
 */

public class Configuration {
	
	protected List<Sensor> sensors = new ArrayList<Sensor>();	
	protected Map<String, Float> timesToDetection; // <Scenario, TTD>
	
	public Configuration() {
		sensors = new ArrayList<Sensor>();
	}
	
	public Configuration(ExtendedConfiguration configuration) {
		sensors = new ArrayList<Sensor>();

		for(ExtendedSensor sensor: configuration.getExtendedSensors())
			sensors.add(sensor.makeCopy());
		
		timesToDetection = new HashMap<String, Float>(configuration.getTimesToDetection());
	}
	
	public void addSensor(Sensor sensor) {
		sensors.add(sensor);
	}
	
	public List<Sensor> getSensors() {
		return sensors;
	}
	
	public float getUnweightedTimeToDetectionInDetectingScenarios() {
		float ttd = 0.0f;
		//if(timesToDetection != null)
			for(float ttdPerScenario: timesToDetection.values()) 
				ttd += ttdPerScenario;
		//else ttd = Float.MAX_VALUE;
		return ttd;
	}	
	
	@Override
	public boolean equals(Object object) {
		if(object instanceof Configuration) {
			List<Sensor> mySensors = new ArrayList<Sensor>(sensors);
			for(Sensor sensor: ((Configuration)object).sensors) {
				Sensor found = null;
				for(Sensor mySensor: mySensors) {
					if(sensor.hashCode() == mySensor.hashCode()) {
						found = mySensor;
						break;
					}
				}
				if(found != null)
					mySensors.remove(found);
				else
					return false;
			}
			return mySensors.isEmpty();
		} 
		return false;
	}
	
	public int countScenariosDetected() {
		return timesToDetection == null ? 0 : timesToDetection.keySet().size();
	}
	
	public Map<String, Float> getTimesToDetection() {
		return timesToDetection;
	}
}
