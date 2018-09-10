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

	@Override
	public Float objective(final ExtendedConfiguration configuration, final ScenarioSet set, boolean runThreaded) {

		// Start a timer	
		long startTime = System.currentTimeMillis();		
		List<Thread> threads = new ArrayList<Thread>();
		// Clear out previous information
		for (ExtendedSensor sensor : configuration.getExtendedSensors()) {
			sensor.clearScenariosUsed();
		}
		// int processors = Runtime.getRuntime().availableProcessors(); (Scale by this?)
		for (final String scenario : set.getScenarios()) {
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
				
				thread.start();
				threads.add(thread);
			} else {
				startTime = System.currentTimeMillis();	
				try {
					innerLoopParallel(configuration, set, scenario);
				} catch (Exception e) {
					e.printStackTrace();
				}
				Constants.timer.addPerScenario(System.currentTimeMillis() - startTime);
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

	public void innerLoopParallel(ExtendedConfiguration con, ScenarioSet set, String scenario) throws Exception {
		if(set.getScenarioWeights().get(scenario) <= 0) return; //Skip any scenarios with a weighting of 0
		
		TimeStep ts = null;
		InferenceResult inferenceResult = null;
		for(TimeStep timeStep: set.getNodeStructure().getTimeSteps()) {
			ts = timeStep;
			long startTime = System.currentTimeMillis();	
			for(ExtendedSensor sensor: con.getExtendedSensors()) {
				String specificType = set.getSensorSettings(sensor.getSensorType()).specificType;
				Boolean triggered = null;
				if(sensor.getSensorType().contains("Electrical Conductivity")) {
					if(this.currentIteration==-3) //A hack to trigger the calculation of the best TTD for ERT (complicated because of well pairings)
						triggered = E4DSensors.ertBestSensorTriggered(timeStep, scenario, sensor.getNodeNumber(), set.getSensorSettings(sensor.getSensorType()).getDetectionThreshold());
					else
						triggered = E4DSensors.ertSensorTriggered(timeStep, scenario, sensor.getNodeNumber(), set.getSensorSettings(sensor.getSensorType()).getDetectionThreshold());
				} else
					triggered = sensorTriggered(specificType, scenario, sensor.getNodeNumber(), timeStep, set);
				sensor.setTriggered(triggered, scenario, timeStep, 0.0);
			}
			inferenceResult = inference(con, set, scenario);
			Constants.timer.addPerTime(System.currentTimeMillis() - startTime);
			if (inferenceResult.isInferred())
				break;
		}
		// maxTime is an index, we want the value there
		float timeInYears = 1000000;			
		if (ts != null && inferenceResult.isInferred()) {
			timeInYears = ts.getRealTime();
			// Only keep track if we've hit inference
			con.addTimeToDetection(scenario, timeInYears);
		} else {
			con.getTimesToDetection().remove(scenario);
		}
		con.addObjectiveValue(scenario, timeInYears * set.getGloballyNormalizedScenarioWeight(scenario));
		con.addInferenceResult(scenario, inferenceResult);
	}
	
	
	// This method tells the Simulated Annealing process whether sensors have been triggered
	public static Boolean sensorTriggered(String specificType, String scenario, Integer nodeNumber, TimeStep timestep, ScenarioSet set) {
		Boolean triggered = false;
		if(!set.getDetectionMap().get(specificType).containsKey(scenario))
			triggered = false;
		if(!set.getDetectionMap().get(specificType).get(scenario).containsKey(nodeNumber))
			triggered = false;
		else
			if(set.getDetectionMap().get(specificType).get(scenario).get(nodeNumber) < timestep.getRealTime())
				triggered = true;
		return triggered;
	}
	
}
