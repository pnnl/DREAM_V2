package objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configuration {

	protected List<Sensor> sensors = new ArrayList<Sensor>();	
	
	private Map<Scenario, Float> timesToDetection;

	public Configuration() {
		sensors = new ArrayList<Sensor>();
	}

	public Configuration(ExtendedConfiguration configuration) {
		sensors = new ArrayList<Sensor>();

		for(ExtendedSensor sensor: configuration.getExtendedSensors()) {
			sensors.add(sensor.makeCopy());
		}
		
		timesToDetection = new HashMap<Scenario, Float>(configuration.getTimesToDetection());
		
	}

	public Configuration(List<Sensor> sensors) {
		this.sensors = new ArrayList<Sensor>(sensors);
	}

	public void addSensor(Sensor sensor) {
		sensors.add(sensor);
	}

	public List<Sensor> getSensors() {
		return sensors;
	}

	public float getUnweightedTimeToDetectionInDetectingScenarios() {
		float ttd = 0.0f;
		for(float ttdPerScenario: timesToDetection.values()) 
			ttd += ttdPerScenario;
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

	@Override
	public int hashCode() {
		int hash = 7;
		for(Sensor sensor: sensors) {
			hash *= 31 * sensor.hashCode();
		}
		return hash;
	}
	
	public int countScenariosDetected() {
		return timesToDetection == null ? 0 : timesToDetection.keySet().size();
	}

	public Map<Scenario, Float> getTimesToDetection() {
		return timesToDetection;
	}
	
	public void setTimesToDetection(Map<Scenario, Float> map) {
		this.timesToDetection = new HashMap<Scenario, Float>(map);
	}
}
