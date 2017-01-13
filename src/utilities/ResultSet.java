package utilities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import functions.MutationFunction.MUTATE;

import objects.ExtendedConfiguration;
import objects.InferenceTest;
import objects.Result;
import objects.Scenario;
import objects.ScenarioSet;
import objects.SensorSetting;

/**
 * Depreciated code?
 * @author port091
 */

public class ResultSet {


	/**
	 * User settings - 
	 */
	private Point3i addPoint;
	private int maxWells;
	private int iterations;
	private double costConstraint;
	private MUTATE mutateLogic;
	
	private Map<String, Float> scenarioProbabilities;
	private Map<String, SensorSetting> sensorSettings;
	
	private InferenceTest inferenceTest;

	private String startTime; // When it was started
	private String endTime; // When it finished
	
	private List<Result> results; // The results
	
	// What else might we want to know?
	
	public ResultSet(ScenarioSet set, MUTATE mutateLogic) {
		
		addPoint = new Point3i(set.getAddPoint());
		maxWells = set.getMaxWells();
		iterations = set.getIterations();
		costConstraint = set.getCostConstraint();
		this.mutateLogic = mutateLogic;
		scenarioProbabilities = new HashMap<String, Float>();
		sensorSettings = new HashMap<String, SensorSetting>(); // TODO: Need to copy out useful information from sensorSetting
		for(Scenario scenario: set.getScenarios()) {
			scenarioProbabilities.put(scenario.getScenario(), set.getScenarioWeights().get(scenario));
			sensorSettings.put(scenario.getScenario(), set.getSensorSettings(scenario.getScenario()));
		}
		this.inferenceTest = set.getInferenceTest(); // TODO: Need to copy useful information from this.
		
		startTime = new SimpleDateFormat("yyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		results = new ArrayList<Result>();
	}
	
	public List<Result> getUniqueResults() {
	//List<Result> uniqueResults = new ArrayList<Result>();
	//	for(Result result: results) {
	//		if(!uniqueResults.contains(result)) {
	//			uniqueResults.add(result);
	//		} else {
	//			int index = uniqueResults.indexOf(result);
	//			uniqueResults.get(index).incrementDupliateCount(result.getIteration());
	//		}
	//	}
	//	System.out.println("Unique results: " + uniqueResults.toString());
	//	return uniqueResults;
		return results;
		
	}
	
	public List<Result> getResults() {
		return results;
	}
	
	public String getStartTime() {
		return startTime;
	}
	
	public void storeResults(ExtendedConfiguration configuration, int iteration) {
		if(configuration == null)
			return;
		results.add(new Result(configuration, iteration));
		endTime = new SimpleDateFormat("yyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
	}

	public void storeResults(ExtendedConfiguration configuration) {
		if(configuration == null)
			return;
		results.add(new Result(configuration));
		endTime = new SimpleDateFormat("yyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
	}
}
