package functions;

import hdf5Tool.HDF5Interface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import objects.E4DSensors;
import objects.ExtendedConfiguration;
import objects.InferenceResult;
import objects.NodeStructure;
import objects.ScenarioSet;
import objects.Scenario;
import objects.ExtendedSensor;
import objects.SensorSetting;
import objects.TimeStep;
import objects.SensorSetting.DeltaType;
import objects.SensorSetting.Trigger;
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
	public InferenceResult inference(ExtendedConfiguration configuration, ScenarioSet set, Scenario scenario) {

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
		for (final Scenario scenario : set.getScenarios()) {
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

	public void innerLoopParallel(ExtendedConfiguration con, ScenarioSet set, Scenario scenario) throws Exception {
		if (set.getScenarioWeights().get(scenario) > 0) {
			TimeStep ts = null;
			InferenceResult inferenceResult = null;
			for (TimeStep timeStep: set.getNodeStructure().getTimeSteps()) {
				ts = timeStep;
				long startTime = System.currentTimeMillis();	
				for(ExtendedSensor sensor: con.getExtendedSensors()) {
					String specificType = set.getSensorSettings(sensor.getSensorType()).getSpecificType();
					Boolean triggered = null;
					if(sensor.getSensorType().contains("Electrical Conductivity"))
						triggered = E4DSensors.ertSensorTriggered(timeStep, scenario, sensor.getNodeNumber(), set.getSensorSettings(sensor.getSensorType()).getLowerThreshold());
					else
						triggered = sensorTriggered(specificType, scenario.getScenario(), sensor.getNodeNumber(), timeStep);
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
	}
	
	
	// This method tells the Simulated Annealing process whether sensors have been triggered
	public static Boolean sensorTriggered(String specificType, String scenario, Integer nodeNumber, TimeStep timestep) throws Exception{
		Boolean triggered = false;
		
		// Return as triggered only if the timestep exceeds the detection value for the well pairing
		if(HDF5Interface.paretoMap.get(specificType).get(scenario).containsKey(nodeNumber)) {
			float detection = HDF5Interface.paretoMap.get(specificType).get(scenario).get(nodeNumber);
			if(detection!=0 && timestep.getTimeStep()>detection)
				triggered = true;
		}
		
		return triggered;
	}
	
	
	public static Float getParetoTTD(NodeStructure nodeStructure, SensorSetting setting, String specificType, String scenario, Integer nodeNumber) {
		if(!HDF5Interface.paretoMap.containsKey(specificType)) {
			HDF5Interface.createParetoMap(nodeStructure, setting, specificType);
			System.out.println("You just created a pareto map for " + specificType + "! Awesome! So Fast!");
		}
		return HDF5Interface.paretoMap.get(specificType).get(scenario).get(nodeNumber);
	}
	
	
	public static Boolean paretoSensorTriggered(SensorSetting setting, Float currentValue, Float valueAtTime0) {
		Boolean triggered = null;
		
		// See if we exceeded threshold
		if(currentValue != null && (setting.getTrigger() == Trigger.MINIMUM_THRESHOLD || setting.getTrigger() == Trigger.MAXIMUM_THRESHOLD)) {
			triggered = setting.getLowerThreshold() <= currentValue && currentValue <= setting.getUpperThreshold();
		} else if(currentValue != null && setting.getTrigger() == Trigger.RELATIVE_DELTA) {
			float change = valueAtTime0 == 0 ? 0 : ((currentValue - valueAtTime0) / valueAtTime0);
			if(setting.getDeltaType() == DeltaType.INCREASE) triggered = setting.getLowerThreshold() <= change;
			else if(setting.getDeltaType() == DeltaType.DECREASE) triggered = setting.getLowerThreshold() >= change;
			else if(setting.getDeltaType() == DeltaType.BOTH) triggered = setting.getLowerThreshold() <= Math.abs(change);					
		} else if(currentValue != null && setting.getTrigger() == Trigger.ABSOLUTE_DELTA) {
			float change = currentValue - valueAtTime0;
			if(setting.getDeltaType() == DeltaType.INCREASE) triggered = setting.getLowerThreshold() <= change;
			else if(setting.getDeltaType() == DeltaType.DECREASE) triggered = setting.getLowerThreshold() >= change;
			else if(setting.getDeltaType() == DeltaType.BOTH) triggered = setting.getLowerThreshold() <= Math.abs(change);		
		} else {
			triggered = false;
		}
		return triggered;
	}
}
