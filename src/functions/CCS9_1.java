package functions;

import hdf5Tool.HDF5Wrapper;

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
		//TEST!
//		List<ExtendedSensor> toAdd = new ArrayList<ExtendedSensor>();
		//ENDTEST!
		for (ExtendedSensor sensor : configuration.getExtendedSensors()) {
			sensor.clearScenariosUsed();
			//TEST!
//			sensor.getSensorType();
//			for(String type: set.getDataTypes()){
//				if(type != sensor.getSensorType()){
//					toAdd.add(new ExtendedSensor(sensor.getNodeNumber(), type, set.getNodeStructure()));
//				}
//			}
			//ENDTEST!
		}
		//TEST!
//		final ExtendedConfiguration newConfiguration = configuration.makeCopy(set);
//		for(ExtendedSensor sensor : toAdd) newConfiguration.addSensor(sensor); //Comment out this line and hopefully results will change.
		//ENDTEST!
		// int processors = Runtime.getRuntime().availableProcessors(); (Scale by this?)
		for (final Scenario scenario : set.getScenarios())
		{
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
					innerLoopParallel(newConfiguration, set, scenario);
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

		return newConfiguration.getObjectiveValue();

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

		if(Constants.hdf5Data.isEmpty() && Constants.hdf5CloudData.isEmpty()) {
			currentValue = HDF5Wrapper.queryValueFromFile(set.getNodeStructure(), scenario.getScenario(), timeStep, dataType, nodeNumber);
			valueAtTime0 = HDF5Wrapper.queryValueFromFile(set.getNodeStructure(), scenario.getScenario(), set.getNodeStructure().getTimeSteps().get(0), dataType, nodeNumber);
		} else if(Constants.hdf5Data.isEmpty()) {
			currentValue =  HDF5Wrapper.queryValueFromCloud(set.getNodeStructure(), scenario.getScenario(), timeStep, dataType, nodeNumber);	
			valueAtTime0 =  HDF5Wrapper.queryValueFromCloud(set.getNodeStructure(), scenario.getScenario(), set.getNodeStructure().getTimeSteps().get(0), dataType, nodeNumber);	
		} else {
			currentValue =  HDF5Wrapper.queryValueFromMemory(set.getNodeStructure(), scenario.getScenario(), timeStep, dataType, nodeNumber);		
			valueAtTime0 =  HDF5Wrapper.queryValueFromMemory(set.getNodeStructure(), scenario.getScenario(), set.getNodeStructure().getTimeSteps().get(0), dataType, nodeNumber);				
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
		return triggered;
	}
	
	public static Boolean paretoSensorTriggered(SensorSetting setting, NodeStructure nodeStructure, TimeStep timeStep, Scenario scenario, String sensorType, Integer nodeNumber) throws Exception{
		Boolean triggered = null;
		
		String dataType = SensorSetting.sensorTypeToDataType.get(sensorType);

		// Get the value at the current time step
		Float currentValue = 0.0f;
		Float valueAtTime0 = 0.0f;

		if(Constants.hdf5Data.isEmpty() && Constants.hdf5CloudData.isEmpty()) {
			currentValue = HDF5Wrapper.queryValueFromFile(nodeStructure, scenario.getScenario(), timeStep, dataType, nodeNumber);
			valueAtTime0 = HDF5Wrapper.queryValueFromFile(nodeStructure, scenario.getScenario(), nodeStructure.getTimeSteps().get(0), dataType, nodeNumber);
		} else if(Constants.hdf5Data.isEmpty()) {
			currentValue =  HDF5Wrapper.queryValueFromCloud(nodeStructure, scenario.getScenario(), timeStep, dataType, nodeNumber);	
			valueAtTime0 =  HDF5Wrapper.queryValueFromCloud(nodeStructure, scenario.getScenario(), nodeStructure.getTimeSteps().get(0), dataType, nodeNumber);	
		} else {
			currentValue =  HDF5Wrapper.queryValueFromMemory(nodeStructure, scenario.getScenario(), timeStep, dataType, nodeNumber);		
			valueAtTime0 =  HDF5Wrapper.queryValueFromMemory(nodeStructure, scenario.getScenario(), nodeStructure.getTimeSteps().get(0), dataType, nodeNumber);				
		}

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

		if(Constants.hdf5Data.isEmpty() && Constants.hdf5CloudData.isEmpty()) {
			currentValue = HDF5Wrapper.queryValueFromFile(nodeStructure, scenario.getScenario(), timeStep, dataType, nodeNumber);
			valueAtTime0 = HDF5Wrapper.queryValueFromFile(nodeStructure, scenario.getScenario(), nodeStructure.getTimeSteps().get(0), dataType, nodeNumber);
		} else if(Constants.hdf5Data.isEmpty()) {
			currentValue =  HDF5Wrapper.queryValueFromCloud(nodeStructure, scenario.getScenario(), timeStep, dataType, nodeNumber);	
			valueAtTime0 =  HDF5Wrapper.queryValueFromCloud(nodeStructure, scenario.getScenario(), nodeStructure.getTimeSteps().get(0), dataType, nodeNumber);	
		} else {
			currentValue =  HDF5Wrapper.queryValueFromMemory(nodeStructure, scenario.getScenario(), timeStep, dataType, nodeNumber);		
			valueAtTime0 =  HDF5Wrapper.queryValueFromMemory(nodeStructure, scenario.getScenario(), nodeStructure.getTimeSteps().get(0), dataType, nodeNumber);				
		}

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
