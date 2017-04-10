package results;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import objects.Configuration;
import objects.ExtendedConfiguration;
import objects.ExtendedSensor;
import objects.Scenario;
import objects.ScenarioSet;

/**
 * Catch-all class used to store the results of a particular run.
 * @author port091
 * @author rodr144
 */

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
	public HashSet<ExtendedConfiguration> bestConfigSumList;
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
	
	
	public Results(ScenarioSet set, boolean makePlots) {
		this.set = set;
		bestConfigSumList = new HashSet<ExtendedConfiguration>();
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

		resultsPlots = makePlots;
		
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
				ExtendedConfiguration newExtendedConfiguration = new ExtendedConfiguration();
				for(ExtendedSensor sensor: configuration.getExtendedSensors()) {
					if(sensor.isInferred())
						newExtendedConfiguration.addSensor(sensor.makeCopy());
				}
				newExtendedConfiguration.setTimesToDetection(configuration.getTimesToDetection());
				bestConfigSumList.add(newExtendedConfiguration);
				bestConfigSumTTDs.put(newExtendedConfiguration, ttd);
				bestConfigSumPercents.put(newExtendedConfiguration, percent);
			}
		}
		
		if(objPerIterSum) {
			if(!objPerIterSumMap.get(type).containsKey(iteration)) {
				objPerIterSumMap.get(type).put(iteration, new LinkedHashMap<Integer, ObjectiveResult>());
			}	
			
			float scenariosDetected = 0;
			float totalWeightsForDetectedScenarios = 0.0f;
			float weightedAverageTTD = 0.0f;

			// If we want weighted, we need to weight based on the normalized value of just the detected scenarios
			for(Scenario detectingScenario: configuration.getTimesToDetection().keySet()) {
				totalWeightsForDetectedScenarios += set.getScenarioWeights().get(detectingScenario);
			}
			
			// If we want weighted percent of scenarios detected, we can just add up the globally normalized values for each detecting scenario
			for(Scenario detectingScenario: configuration.getTimesToDetection().keySet()) {
				scenariosDetected += set.getGloballyNormalizedScenarioWeight(detectingScenario)*100;
			}
			
			for(Scenario detectingScenario: configuration.getTimesToDetection().keySet()) {
				float scenarioWeight = set.getScenarioWeights().get(detectingScenario);
				weightedAverageTTD += configuration.getTimesToDetection().get(detectingScenario) * (scenarioWeight/totalWeightsForDetectedScenarios);
			}

			objPerIterSumMap.get(type).get(iteration).put(run, new ObjectiveResult(weightedAverageTTD, scenariosDetected));
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
