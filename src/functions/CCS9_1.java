package functions;

import hdf5Tool.HDF5Interface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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
 * This is the function that we use by deafult/exclusively, at the moment
 * @author port091
 */

public class CCS9_1 extends Function {

	public CCS9_1() {
	}

	public CCS9_1(MUTATE mutate) {
		this.mutate = mutate;
	}

	@Override
	public String toString() {
		return "CCS9.1";
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

	/*	@Override
	public boolean mutate(Configuration configuration, ScenarioSet set) {
		return configruation.mutate(set);
	}
	 */
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
		for (final Scenario scenario : set.getScenarios())
		{
			if(runThreaded) {
				Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							long startTime = System.currentTimeMillis();	
							if(set.e4dInterface == null) innerLoopParallel(configuration, set, scenario); // E4D TODO: is this really how we want to check whether or not to run the E4D bit?
							else InnerLoopParallelE4D(configuration, set, scenario);
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
					if(set.e4dInterface == null) innerLoopParallel(configuration, set, scenario); // E4D TODO: is this really how we want to check whether or not to run the E4D bit?
					else InnerLoopParallelE4D(configuration, set, scenario);
				} catch (Exception e) {
					e.printStackTrace();
				}
				Constants.timer.addPerScenario(System.currentTimeMillis() - startTime);

			}
		}

		if(runThreaded) {
			for (Thread thread: threads)
			{
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

	/**                                                                  **\
	 * Added by Luke: TODO - do we actually need this, and does it work?  *
	 \*
	
	/**					**\
	 * Helper Methods	 *
	 * 					 *
	\*					 */

	public void innerLoopParallel(ExtendedConfiguration con, ScenarioSet set, Scenario scenario) throws Exception
	{
		if (set.getScenarioWeights().get(scenario) > 0)
		{
			TimeStep ts = null;
			InferenceResult inferenceResult = null;
			for (TimeStep timeStep: set.getNodeStructure().getTimeSteps())
			{
				ts = timeStep;
				long startTime = System.currentTimeMillis();	
				inferenceResult = runOneTime(con, set, timeStep, scenario, true);
				Constants.timer.addPerTime(System.currentTimeMillis() - startTime);
				if (inferenceResult.isInferred())
					break;
			}
			// maxTime is an index, we want the value there
			float timeInYears = 1000000;			
			if (ts != null && inferenceResult.isInferred()) { //E4D TODO: change this logic to accommodate second inferenceResult from E4D matrix
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

	private void InnerLoopParallelE4D(ExtendedConfiguration con, ScenarioSet set, Scenario scenario) throws Exception
	{
		if (set.getScenarioWeights().get(scenario) > 0)
		{
			Float detectTimeE4D = set.e4dInterface.getDetectionTime(scenario.toString(), con);
			TimeStep ts = null;
			InferenceResult inferenceResult = null;
			for (TimeStep timeStep: set.getNodeStructure().getTimeSteps())
			{
				ts = timeStep;
				long startTime = System.currentTimeMillis();	
				inferenceResult = runOneTime(con, set, timeStep, scenario, true);
				Constants.timer.addPerTime(System.currentTimeMillis() - startTime);
				if (inferenceResult.isInferred()) //break here if non-E4D has detected
					break;
				//TODO: This next line hacks in setting the inference result to true if E4D has detected by this point.
				//		I have not given careful consideration to whether or not this is exactly how we want to handle it,
				//		but it seems like this is at least a reasonable place to start.
				if(ts.getRealTime() >= detectTimeE4D){ //also break if E4D has detected, and make sure inference result is true for later logic
					inferenceResult = new InferenceResult(true);
					break;
				}
			}
			// maxTime is an index, we want the value there
			float timeInYears = 1000000;			
			if (ts != null && inferenceResult.isInferred()) { //E4D TODO: How do we want to work E4D into the inference result? I'm not sure. Currently hacked in above.
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
	
	private InferenceResult runOneTime(ExtendedConfiguration con, ScenarioSet set, TimeStep timeStep, Scenario scenario, boolean usedSensors) throws Exception
	{

		//float realTime = timeStep.getRealTime();

		for (ExtendedSensor sensor : con.getExtendedSensors())
		{

			// Make sure the sensor is in the cloud...
			if (sensor.isInCloud(set))
			{
				// Only works for threshold?
				Boolean triggered = null;//getHistory(scenario, sensor.getNodeNumber(), time, sensor.getSensorType());
				// We haven't tested this before
				if(triggered == null) {
					//LUKE EDIT HERE - this is where we should loop over all nodenumbers that are within the radius we want
					// for the case in which we detect with a given radius from the individual sensor
					
					triggered = sensorTriggered(set, timeStep, scenario, sensor.getSensorType(), sensor.getNodeNumber());
				}		


				// Store the result, set the sensor to triggered or not
				// storeHistory(scenario, sensor.getNodeNumber(), realTime, sensor.getSensorType(), triggered);
				sensor.setTriggered(triggered, scenario, timeStep, 0.0); // TODO, we won't have the triggered value anymore, do we need it?
			}
		}
		return inference(con, set, scenario);
	}
	
	public static Boolean sensorTriggered(ScenarioSet set, TimeStep timeStep, Scenario scenario, String sensorType, Integer nodeNumber) throws Exception{
		Boolean triggered = null;
		
		String dataType = SensorSetting.sensorTypeToDataType.get(sensorType);
		SensorSetting temp = set.getSensorSettings().get(sensorType);

		// Get the value at the current time step
		Float currentValue = 0.0f;
		Float valueAtTime0 = 0.0f;
		currentValue = HDF5Interface.queryValue(set.getNodeStructure(), scenario.getScenario(), timeStep, dataType, nodeNumber);
		valueAtTime0 = HDF5Interface.queryValue(set.getNodeStructure(), scenario.getScenario(), set.getNodeStructure().getTimeSteps().get(0), dataType, nodeNumber);

		// See if we exceeded threshold
		if(currentValue != null && (temp.getTrigger() == Trigger.MINIMUM_THRESHOLD || temp.getTrigger() == Trigger.MAXIMUM_THRESHOLD)) {
			triggered = temp.getLowerThreshold() <= currentValue && currentValue <= temp.getUpperThreshold();
		} else if(currentValue != null && temp.getTrigger() == Trigger.RELATIVE_DELTA) {
			float change = valueAtTime0 == 0 ? 0 : ((currentValue - valueAtTime0) / valueAtTime0);
			if(temp.getDeltaType() == DeltaType.INCREASE) triggered = temp.getLowerThreshold() <= change;
			else if(temp.getDeltaType() == DeltaType.DECREASE) triggered = temp.getLowerThreshold() >= change;
			else if(temp.getDeltaType() == DeltaType.BOTH) triggered = temp.getLowerThreshold() <= Math.abs(change);					
		} else if(currentValue != null && temp.getTrigger() == Trigger.ABSOLUTE_DELTA) {
			float change = currentValue - valueAtTime0;
			if(temp.getDeltaType() == DeltaType.INCREASE) triggered = temp.getLowerThreshold() <= change;
			else if(temp.getDeltaType() == DeltaType.DECREASE) triggered = temp.getLowerThreshold() >= change;
			else if(temp.getDeltaType() == DeltaType.BOTH) triggered = temp.getLowerThreshold() <= Math.abs(change);		
		} else {
			triggered = false;
		}
		return triggered;
	}
	
	public static Boolean paretoSensorTriggered(SensorSetting setting, NodeStructure nodeStructure, TimeStep timeStep, Scenario scenario, String sensorType, Integer nodeNumber) throws Exception{
		Boolean triggered = null;
		
		String dataType = SensorSetting.sensorTypeToDataType.get(sensorType);

		// Get the value at the current time step
		Float currentValue = 0.0f;
		Float valueAtTime0 = 0.0f;
		currentValue = HDF5Interface.queryValue(nodeStructure, scenario.getScenario(), timeStep, dataType, nodeNumber);
		valueAtTime0 = HDF5Interface.queryValue(nodeStructure, scenario.getScenario(), nodeStructure.getTimeSteps().get(0), dataType, nodeNumber);

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
	
	public static Boolean volumeSensorTriggered(SensorSetting setting, NodeStructure nodeStructure, TimeStep timeStep, Scenario scenario, String dataType, Integer nodeNumber) throws Exception{
		Boolean triggered = null;
		
		// Get the value at the current time step
		Float currentValue = 0.0f;
		Float valueAtTime0 = 0.0f;
		currentValue = HDF5Interface.queryValue(nodeStructure, scenario.getScenario(), timeStep, dataType, nodeNumber);
		valueAtTime0 = HDF5Interface.queryValue(nodeStructure, scenario.getScenario(), nodeStructure.getTimeSteps().get(0), dataType, nodeNumber);
		
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
