package functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import objects.E4DSensors;
import objects.ExtendedConfiguration;
import objects.InferenceResult;
import objects.ScenarioSet;
import objects.ExtendedSensor;
import objects.TimeStep;
import utilities.Constants;

/**
 * Will handle all the functions as defined in the RIMVA 9.1 Code
 * This is the function that we use by default/exclusively, at the moment
 * @author port091
 * @author whit162
 */

public class SimulatedAnnealing extends Function {

	public SimulatedAnnealing() {
	}

	public SimulatedAnnealing(MUTATE mutate) {
		this.mutate = mutate;
	}

	@Override
	public String toString() {
		return "SimulatedAnnealing";
	}
	
	// Inference is basically testing that enough sensors detected their threshold to count as a leak
	// The criteria for inference is set from Page_DetectionCriteria
	@Override
	public InferenceResult inference(ExtendedConfiguration configuration, ScenarioSet set, String scenario) {

		// Count how many of each type of sensor has triggered
		Map<String, Integer> totalByType = new HashMap<String, Integer>();
		Map<String, Integer> triggeredByType = new HashMap<String, Integer>();

		// Increment counters
		for(ExtendedSensor sensor: configuration.getExtendedSensors()) {
			if(!totalByType.containsKey(sensor.getSensorType())) {
				totalByType.put(sensor.getSensorType(), 0);
			}
			if(!triggeredByType.containsKey(sensor.getSensorType())) {
				triggeredByType.put(sensor.getSensorType(), 0);
			}

			// Only increment triggered totals if the current sensor is triggered
			if(sensor.isInferred() && sensor.isTriggeredInScenario(scenario)) {
				int triggered = triggeredByType.get(sensor.getSensorType())+1;
				triggeredByType.put(sensor.getSensorType(), triggered);
			}

			int count = totalByType.get(sensor.getSensorType())+1;
			totalByType.put(sensor.getSensorType(), count);
		}

		for(String type: totalByType.keySet()) {
			Constants.log(Level.FINEST, "CCS9_1 - inference", type + " total: " + totalByType.get(type) + "\ttriggering: " + triggeredByType.get(type));
		}

		Boolean inference = set.getInferenceTest().reachedInference(triggeredByType);
		InferenceResult result = new InferenceResult(inference);

		if(inference)
			result = new InferenceResult(inference, set.getInferenceTest().calculateGoodness(totalByType, triggeredByType));

		Constants.log(Level.FINEST, "CCS9_1 - inference", inference.toString());

		return result;

	}
	
	// This calculates an value describing how well the sensors detected for this configuration
	// A large penalty is given to sensors that do not detect
	@Override
	public Float objective(final ExtendedConfiguration configuration, final ScenarioSet set, boolean runThreaded) {
		
		// Start a timer	
		long startTime = System.currentTimeMillis();		
		List<Thread> threads = new ArrayList<Thread>();
		// Clear out previous information
		for (ExtendedSensor sensor : configuration.getExtendedSensors()) {
			sensor.clearScenariosUsed();
		}
		final int cores = Runtime.getRuntime().availableProcessors() - 1; //Use all but one core
		for(final String scenario: set.getScenarios()) {
			if(set.getScenarioWeights().get(scenario) <= 0) continue; //Skip any scenarios with a weighting of 0
			if(runThreaded) {
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							long startTime = System.currentTimeMillis();
							innerLoopParallel(configuration, set, scenario);
							Constants.timer.addPerScenario(System.currentTimeMillis() - startTime);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				if(threads.size() < cores) {
					thread.start();
					threads.add(thread);
				}
			} else {
				try {
					startTime = System.currentTimeMillis();	
					innerLoopParallel(configuration, set, scenario);
					Constants.timer.addPerScenario(System.currentTimeMillis() - startTime);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if(runThreaded) {
			for (Thread thread: threads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		Constants.timer.addPerConfiguration(System.currentTimeMillis() - startTime);
		
		return configuration.getObjectiveValue();
	}
	
	
	/**					**\
	 * Helper Methods	 *
	 * 					 *
	\*					 */

	public void innerLoopParallel(ExtendedConfiguration configuration, ScenarioSet set, String scenario) throws Exception {
		/*
		// Jonathan's attempt at simplifying the objective code without looping through time steps
		InferenceResult inferenceResult = null;
		for(ExtendedSensor sensor: configuration.getExtendedSensors()) { //Loop through sensors in configuration
			String specificType = set.getSensorSettings(sensor.getSensorType()).specificType;
			float sensorTTD = 1000000; // Default penalty for a scenario with no detection
			
			// ERT needs to be looking at a different detection map with well pairings
			if(specificType.contains("Electrical Conductivity"))
				sensorTTD = E4DSensors.ertDetectionValue(sensor, scenario);
			
			// Check to make sure the detection map has a result for this node number
			else if(set.getDetectionMap().get(specificType).get(scenario).containsKey(sensor.getNodeNumber()))
				sensorTTD = set.getDetectionMap().get(specificType).get(scenario).get(sensor.getNodeNumber());
			
			// Store each sensor TTD in configuration
			if(sensorTTD < 1000000)
				sensor.setTriggered(scenario, sensorTTD);
		}
		inferenceResult = inference(configuration, set, scenario);
		if(inferenceResult.isInferred())
			configuration.addTimeToDetection(scenario, ttd);
		else 
			configuration.getTimesToDetection().remove(scenario);
		configuration.addObjectiveValue(scenario, ttd * set.getGloballyNormalizedScenarioWeight(scenario));
		configuration.addInferenceResult(scenario, inferenceResult);
		*/
		TimeStep ts = null;
		InferenceResult inferenceResult = null;
		for(TimeStep timeStep: set.getNodeStructure().getTimeSteps()) {
			ts = timeStep;
			for(ExtendedSensor sensor: configuration.getExtendedSensors()) {
				String specificType = set.getSensorSettings(sensor.getSensorType()).specificType;
				Boolean triggered = null;
				if(sensor.getSensorType().contains("Electrical Conductivity")) {
					if(this.currentIteration==-3) //A hack to trigger the calculation of the best TTD for ERT (complicated because of well pairings)
						triggered = E4DSensors.ertBestSensorTriggered(timeStep, scenario, sensor.getNodeNumber(), set.getSensorSettings(sensor.getSensorType()).getDetectionThreshold());
					else
						triggered = E4DSensors.ertSensorTriggered(timeStep, scenario, sensor.getNodeNumber(), set.getSensorSettings(sensor.getSensorType()).getDetectionThreshold());
				} else
					triggered = sensorTriggered(set, specificType, scenario, sensor.getNodeNumber(), timeStep);
				sensor.setTriggered(triggered, scenario, timeStep.getRealTime(), 0.0);
			}
			inferenceResult = inference(configuration, set, scenario);
			if (inferenceResult.isInferred())
				break; // Stop once we've met the inference test - use this detection time
		}
		// Now store objective and inference results in the configuration
		float timeInYears = 1000000; // Default penalty for a scenario with no detection
		if (ts != null && inferenceResult.isInferred()) { // Store value if we have hit inference
			timeInYears = ts.getRealTime();
			configuration.addTimeToDetection(scenario, timeInYears);
		} else { // If no inference, no leak was detected for this scenario
			configuration.getTimesToDetection().remove(scenario);
		}
		configuration.addObjectiveValue(scenario, timeInYears * set.getGloballyNormalizedScenarioWeight(scenario));
		configuration.addInferenceResult(scenario, inferenceResult);
	}
	
	
	// This method tells the Simulated Annealing process whether sensors have been triggered
	public static Boolean sensorTriggered(ScenarioSet set, String specificType, String scenario, Integer nodeNumber, TimeStep timestep) {
		if(set.getDetectionMap().get(specificType).get(scenario).containsKey(nodeNumber)) {
			if(set.getDetectionMap().get(specificType).get(scenario).get(nodeNumber) < timestep.getRealTime()) {
				return true;
			}
		}
		return false;
	}
	
}
