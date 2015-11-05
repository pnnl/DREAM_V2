package functions;

import hdf5Tool.HDF5Wrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import objects.ExtendedConfiguration;
import objects.InferenceResult;
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
	public Float objective(final ExtendedConfiguration configuration, final ScenarioSet set) {
		
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
		}
		for (Thread thread: threads)
		{
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Constants.timer.addPerConfiguration(System.currentTimeMillis() - startTime);

		return configuration.getTimeToDetection();

	}


	/**					**\
	 * Helper Methods	 *
	 * 					 *
	\*					 */

	public void innerLoopParallel(ExtendedConfiguration con, ScenarioSet set, Scenario scenario) throws Exception
	{
		if (set.getScenarioProbabilities().get(scenario) > 0)
		{
			int maxTime = 0;
			InferenceResult inferenceResult = null;
			for (TimeStep timeStep: set.getNodeStructure().getTimeSteps())
			{
				maxTime = timeStep.getTimeStep();
				long startTime = System.currentTimeMillis();	
				inferenceResult = runOneTime(con, set, timeStep, scenario, true);
				Constants.timer.addPerTime(System.currentTimeMillis() - startTime);
				if (inferenceResult.isInferred())
					break;
			}

			// maxTime is an index, we want the value there
			float timeInYears = 1000000;			
			if (inferenceResult.isInferred())
				timeInYears = set.getNodeStructure().getTimeAt(maxTime); // Only keep track if we've hit inference

			con.addObjectiveValue(scenario, timeInYears); // Did not detect a leak
			con.addTimeToDetection(scenario, timeInYears * set.getScenarioProbabilities().get(scenario)); // TODO: is this objective value?
			con.addInferenceResult(scenario, inferenceResult);
		}
	}

	private InferenceResult runOneTime(ExtendedConfiguration con, ScenarioSet set, TimeStep timeStep, Scenario scenario, boolean usedSensors) throws Exception
	{
		
		int time = timeStep.getTimeStep();

		for (ExtendedSensor sensor : con.getExtendedSensors())
		{
			if(sensor.getNodeNumber() == 30099) {
				System.out.println("HERE");
			}
			// Make sure the sensor is in the cloud...
			if (sensor.isInCloud(set))
			{
				// Only works for threshold?
				Boolean triggered = null;//getHistory(scenario, sensor.getNodeNumber(), time, sensor.getSensorType());
				// We haven't tested this before
				if(triggered == null) {
					// Get the settings
					SensorSetting temp = set.getSensorSettings().get(sensor.getSensorType());

					// Get the value at the current time step
					Float currentValue = 0.0f;
					Float valueAtTime0 = 0.0f;
					
					if(Constants.hdf5Data.isEmpty() && Constants.hdf5CloudData.isEmpty()) {
						currentValue = HDF5Wrapper.queryValueFromFile(set.getNodeStructure(), scenario.getScenario(), timeStep, sensor.getSensorType(), sensor.getNodeNumber());
						valueAtTime0 = HDF5Wrapper.queryValueFromFile(set.getNodeStructure(), scenario.getScenario(), set.getNodeStructure().getTimeSteps().get(0), sensor.getSensorType(), sensor.getNodeNumber());
					} else if(Constants.hdf5Data.isEmpty()) {
						currentValue =  HDF5Wrapper.queryValueFromCloud(set.getNodeStructure(), scenario.getScenario(), timeStep, sensor.getSensorType(), sensor.getNodeNumber());	
						valueAtTime0 =  HDF5Wrapper.queryValueFromCloud(set.getNodeStructure(), scenario.getScenario(), set.getNodeStructure().getTimeSteps().get(0), sensor.getSensorType(), sensor.getNodeNumber());	
					} else {
						currentValue =  HDF5Wrapper.queryValueFromMemory(set.getNodeStructure(), scenario.getScenario(), timeStep, sensor.getSensorType(), sensor.getNodeNumber());		
						valueAtTime0 =  HDF5Wrapper.queryValueFromMemory(set.getNodeStructure(), scenario.getScenario(), set.getNodeStructure().getTimeSteps().get(0), sensor.getSensorType(), sensor.getNodeNumber());				
					}
										
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
				}		
				
				
				// Store the result, set the sensor to triggered or not
				storeHistory(scenario, sensor.getNodeNumber(), time, sensor.getSensorType(), triggered);
				sensor.setTriggered(triggered, scenario, timeStep, 0.0); // TODO, we won't have the triggered value anymore, do we need it?
			}
		}
		return inference(con, set, scenario);
	}
}
