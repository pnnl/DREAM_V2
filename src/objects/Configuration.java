package objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import results.Results.Type;

public class Configuration {

	protected List<Sensor> sensors = new ArrayList<Sensor>();	
	
	private Map<Scenario, Float> timesToDetection;

	// Used just for results
	// private float objectiveValue;
	private float timeToDetection;

	public Configuration() {
		sensors = new ArrayList<Sensor>();
	}

	public Configuration(ExtendedConfiguration configuration) {
		sensors = new ArrayList<Sensor>();

		for(ExtendedSensor sensor: configuration.getExtendedSensors()) {
			sensors.add(sensor.makeCopy());
		}
		// objectiveValue = configuration.get...;
		timeToDetection = configuration.getTimeToDetection();	
		
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

	public float getTimeToDetection() {
		return timeToDetection;
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

	public Map<Scenario, Float> getTimesToDetection() {
		return timesToDetection;
	}
}
