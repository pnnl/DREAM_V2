package results;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import objects.Configuration;
import objects.ExtendedConfiguration;
import objects.ScenarioSet;

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
	public Map<Type, Map<Integer, Map<Integer, Float>>> objPerIterSumMap;

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
		bestObjValue = Float.MAX_VALUE;
		
		objPerIterSumMap = new HashMap<Type, Map<Integer, Map<Integer, Float>>>();
		objPerIterSumMap.put(Type.New, new LinkedHashMap<Integer, Map<Integer, Float>>());
		objPerIterSumMap.put(Type.Best, new LinkedHashMap<Integer, Map<Integer, Float>>());
		objPerIterSumMap.put(Type.Current, new LinkedHashMap<Integer, Map<Integer, Float>>());
		
		allConfigsMap = new HashMap<Type, Map<Integer, Map<Integer, Configuration>>>();
		allConfigsMap.put(Type.New, new LinkedHashMap<Integer, Map<Integer, Configuration>>());
		allConfigsMap.put(Type.Best, new LinkedHashMap<Integer, Map<Integer, Configuration>>());
		allConfigsMap.put(Type.Current, new LinkedHashMap<Integer, Map<Integer, Configuration>>());

		if(resultsPlots) {
			ttdPlots = new TimeToDetectionPlots(set.getIterations());
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
			float ttd = configuration.getTimeToDetection();	
			if(Float.compare(ttd, bestObjValue) < 0) {
				// Clear the list and set this as our new best objective value
				bestConfigSumList.clear();
				bestObjValue = ttd;
			}
			if(Float.compare(ttd, bestObjValue) == 0) {
				// We have a new best configuration, add it to the list
				bestConfigSumList.add(new Configuration(configuration));
			}
		}
		
		if(objPerIterSum) {
			
			if(!objPerIterSumMap.get(type).containsKey(iteration)) {
				objPerIterSumMap.get(type).put(iteration, new LinkedHashMap<Integer, Float>());
			}
			
			objPerIterSumMap.get(type).get(iteration).put(run, configuration.getTimeToDetection());
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
}
