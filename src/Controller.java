import java.util.ArrayList;
import java.util.List;

import functions.Function;
import functions.Vortex;
import objects.ExtendedConfiguration;
import objects.Scenario;
import objects.ScenarioSet;
import objects.SensorSetting.Trigger;
import objects.TimeStep;
import utilities.Constants;
import utilities.Point3i;


/**
 * Helper class, will handle function calls and setup
 * @author port091
 *
 */
public class Controller {

	private ScenarioSet set;
	private String dataSet = Constants.RUN_TEST;
	private int wells = 4;
	private int sensors = 8;
	private String sensorType = "CO2";
	private float timestep = 1;
	private Float threshold;
	private int iterations = 100;
	private List<String> scenariosToRemove;
	private String resultDir;
	private String resultsTag = "";
	
	
	public Controller() {
		set = new ScenarioSet();
		scenariosToRemove = new ArrayList<String>();
	}

	public void execute() {
		set.loadRunData(dataSet);
		System.out.println("Loading data from: " + dataSet);
		
		// Remove scenarios and reset probabilities
		ArrayList<Scenario> scenariosToRemove = new ArrayList<Scenario>();
		for(Scenario scenario: set.getScenarios()) {
			if(this.scenariosToRemove.contains(scenario.getScenario())) {
				scenariosToRemove.add(scenario);
			}
		}
		for(Scenario scenario: scenariosToRemove) {
			set.removeScenario(scenario);
			System.out.println("\tRemoving scenario: " + scenario);
		}
		
//		float probability = 1.0f/set.getScenarios().size();
		float weight = 1.0f;
		for(Scenario scenario: set.getScenarios()) {
			set.setScenarioWeight(scenario, weight);
			System.out.println("\tSetting probability of scenario " + scenario + " to: " + weight);
		}
		
		set.setUserSettings(new Point3i(1, 1, 1), wells, sensors, 0, true);
		set.setIterations(iterations);
		
		// Remove all but one time step
		int times = set.getNodeStructure().getTimeSteps().size();
		System.out.print("\tRemoving timesteps: ");
		boolean foundIt = false;
		for(int i = 0; i < times-1; i++) {
			if(!foundIt) {
				TimeStep ts = set.getNodeStructure().getTimeSteps().get(0);
				if(Float.compare(ts.getRealTime(), timestep) == 0 || Float.compare(ts.getTimeStep(), timestep) == 0) {
					foundIt = true;
				}
			}
			System.out.print(set.getNodeStructure().getTimeSteps().get(foundIt ? 1 : 0) + " ");
			set.getNodeStructure().getTimeSteps().remove(foundIt ? 1 : 0);
		}
		System.out.println();
		
		ArrayList<String> sensorsToRemove = new ArrayList<String>();
		for(String sensor: set.getSensorSettings().keySet()) {
			if(!sensor.equals(this.sensorType)) {
				sensorsToRemove.add(sensor);
			}
		}
		for(String sensor: sensorsToRemove) {
			System.out.println("\tRemoving sensor: " + sensor);
			set.removeSensorSettings(sensor);
		}
		
		if(threshold != null) {
			set.getSensorSettings(this.sensorType).setTrigger(Trigger.MAXIMUM_THRESHOLD);
			set.getSensorSettings(this.sensorType).setUpperThreshold(threshold);
			System.out.println("Setting "+ sensorType +" threshold to: " + threshold);
		}
		// Set the cost to 1
		set.getSensorSettings(sensorType).setCost(1);
		
		Function runner = new Vortex();
		System.out.println("\tSetting results directory to: " + resultDir);
		runner.setResultsDirectory(resultDir);
	
		System.out.println("\tRunning...");
		runner.run(new ExtendedConfiguration(), set, resultsTag);
	}
	
	public void setThreshold(float threshold) {
		this.threshold = threshold;
	}
	
	public void setResultsTag(String resultsTag) {
		this.resultsTag = resultsTag;
	}
	
	public void setResultsDir(String resultsDirectory) {
		this.resultDir = resultsDirectory;
	}
	
	public void removeScenario(String scenario) {
		scenariosToRemove.add(scenario);
	}
	
	public void selectDataset(String whichDataset) {
		this.dataSet = whichDataset;
	}
		
	public void setSensorType(String type) {
		this.sensorType = type;
	}
	
	public void setMaxWells(int wells) {
		this.wells = wells;
	}
	
	public void setMaxSensors(int sensors) {
		this.sensors = sensors;
	}
	
	public void setIterations(int iterations) {
		this.iterations = iterations;
	}
	
	public void setTimestep(float time) {
		this.timestep = time;
	}
	
	
}
