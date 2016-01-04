package results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import objects.Configuration;
import objects.ExtendedConfiguration;
import objects.ExtendedSensor;
import objects.Scenario;
import objects.ScenarioSet;
import objects.Sensor;
import utilities.Constants;

public class Results {

	public static enum Type { 
		New("new"), Best("best"), Current("current");
		String type;
		Type(String type) {
			this.type = type;
		}
		@Override
		public String toString() {
			return type;
		}
	};
	
	// Create a summary of all the unique best configurations
	//     value, node type: node id1, node type: nodeid2, node type: nodeid3
	// or  value, node type: (i, j, k), node type: (i, j, k), node type: (i, j, k)
	public boolean bestConfigSum = true;
	public float bestObjValue;
	public HashSet<Configuration> bestConfigSumList;
	public HashMap<Configuration, Float> bestConfigSumTTDs;
	public HashMap<Configuration, Float> bestConfigSumPercents;

	/*
	 * Print a file (one for new, current, best) for creating plots 
	 * of objective values per iteration.  This is mostly useful if 
	 * we are doing a set of runs
	 * 
	 * iter, obj1, obj2, obj3, ..., objn
	 * 1
	 * 2
	 * ...
	 * n
	 */
	public boolean objPerIterSum = true;
	public Map<Type, Map<Integer, Map<Integer, ObjectiveResult>>> objPerIterSumMap;

	// Print a file for each run, for each type, that contains all iterations,
	// their times to detection, and their configuration
	public boolean allConfigs = true;
	public Map<Type, Map<Integer, Map<Integer, Configuration>>> allConfigsMap;

	public boolean resultsPlots = true;
	public TimeToDetectionPlots ttdPlots;
	
	public ScenarioSet set;
	
	
	public Results(ScenarioSet set) {
		this.set = set;
		bestConfigSumList = new HashSet<Configuration>();
		bestConfigSumTTDs = new HashMap<Configuration, Float>();
		bestConfigSumPercents = new HashMap<Configuration, Float>();
		bestObjValue = Float.MAX_VALUE;
		
		objPerIterSumMap = new HashMap<Type, Map<Integer, Map<Integer, ObjectiveResult>>>();
		objPerIterSumMap.put(Type.New, new LinkedHashMap<Integer, Map<Integer, ObjectiveResult>>());
		objPerIterSumMap.put(Type.Best, new LinkedHashMap<Integer, Map<Integer, ObjectiveResult>>());
		objPerIterSumMap.put(Type.Current, new LinkedHashMap<Integer, Map<Integer, ObjectiveResult>>());
		
		allConfigsMap = new HashMap<Type, Map<Integer, Map<Integer, Configuration>>>();
		allConfigsMap.put(Type.New, new LinkedHashMap<Integer, Map<Integer, Configuration>>());
		allConfigsMap.put(Type.Best, new LinkedHashMap<Integer, Map<Integer, Configuration>>());
		allConfigsMap.put(Type.Current, new LinkedHashMap<Integer, Map<Integer, Configuration>>());

		if(resultsPlots) {
			ttdPlots = new TimeToDetectionPlots(set.getIterations(), set.getNodeStructure().getTimeSteps().get(set.getNodeStructure().getTimeSteps().size()-1).getRealTime());
		}
	}
	
	public void storeResult(int run, int iteration, ExtendedConfiguration newConfiguration,
			ExtendedConfiguration bestConfiguration, ExtendedConfiguration currentConfiguration, ScenarioSet set) {
		storeResult(run, iteration, Type.New, newConfiguration, set);
		storeResult(run, iteration, Type.Best, bestConfiguration, set);
		storeResult(run, iteration, Type.Current, currentConfiguration, set);		
	}

	public void storeResult(int run, int iteration, Type type, ExtendedConfiguration configuration, ScenarioSet set) {

		if(bestConfigSum) {
//			float ttd = configuration.getTimeToDetection();	
			float ttd = configuration.getNormalizedAverageTimeToDetection(set.getScenarioWeights());
			float percent = configuration.getNormalizedPercentScenariosDetected(set.getScenarioWeights(), set.getTotalScenarioWeight());
			float global_ttd = ttd + (1-percent)*1000000; //penalty
			//float ttd = configuration.getObjectiveValue();
			if(Float.compare(global_ttd, bestObjValue) < 0) {
				// Clear the list and set this as our new best objective value
				bestConfigSumList.clear();
				bestConfigSumTTDs.clear();
				bestConfigSumPercents.clear();
				bestObjValue = global_ttd;
			}
			if(Float.compare(global_ttd, bestObjValue) == 0) {
				Configuration newConfiguration = new Configuration();
				for(ExtendedSensor sensor: configuration.getExtendedSensors()) {
					if(sensor.isInferred())
						newConfiguration.addSensor(sensor.makeCopy());
				}
				newConfiguration.setTimesToDetection(configuration.getTimesToDetection());
				newConfiguration.setTimeToDetection(configuration.getTimeToDetection());	
				bestConfigSumList.add(newConfiguration);
				bestConfigSumTTDs.put(newConfiguration, ttd);
				bestConfigSumPercents.put(newConfiguration, percent);
			}
		}
		
		if(objPerIterSum) {
			if(!objPerIterSumMap.get(type).containsKey(iteration)) {
				objPerIterSumMap.get(type).put(iteration, new LinkedHashMap<Integer, ObjectiveResult>());
			}	
			
			ArrayList<Float> ttd = new ArrayList<Float>();
			ArrayList<Float> weights = new ArrayList<Float>();
			float scenariosDetected = 0;
			for(Scenario scenario: configuration.getTimesToDetection().keySet()) {
				float timeToDetection = configuration.getTimesToDetection().get(scenario);
				if(timeToDetection == 1000000) {
					// Do nothing...
				} else {
					ttd.add(timeToDetection);
					weights.add(set.getScenarioWeights().get(scenario));
					scenariosDetected += 100*set.getScenarioWeights().get(scenario)/set.getTotalScenarioWeight();
					//		scenariosDetected += results.set.getScenarioProbabilities().get(scenario);					
				}	
			}
			//float percentScenariosDetected = ((float)scenariosDetected)/((float)totalScenarios) * 100;
			float sum = 0;
			float timeToDetection = 0;
			for(float value: weights) sum += value;
			for(int i=0; i<ttd.size(); i++){
				timeToDetection += ttd.get(i)*weights.get(i)/sum;
			}
			
			objPerIterSumMap.get(type).get(iteration).put(run, new ObjectiveResult(timeToDetection, scenariosDetected));
		}
		
		if(allConfigs) {
			if(!allConfigsMap.get(type).containsKey(run)) {
				allConfigsMap.get(type).put(run, new LinkedHashMap<Integer, Configuration>());
			}
			allConfigsMap.get(type).get(run).put(iteration, new Configuration(configuration));
		}
		
		if(resultsPlots) {
			ttdPlots.addData(type, iteration, configuration, set);			
		}
	}
	
	public class ObjectiveResult {
		public float timeToDetectionInDetected;
		public float percentScenariosDetected;
		public ObjectiveResult(float ttd, float p) {
			this.timeToDetectionInDetected = ttd;
			this.percentScenariosDetected = p;
		}
	}
}
