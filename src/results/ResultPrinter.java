package results;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;

import objects.Configuration;
import objects.ExtendedConfiguration;
import objects.ExtendedSensor;
import objects.Scenario;
import objects.ScenarioSet;
import objects.Sensor;
import objects.TimeStep;
import results.Results.ObjectiveResult;
import results.Results.Type;
import utilities.Constants;
import utilities.Point3d;

/**
 * Utility methods for printing a results
 * @author port091
 *
 */
public class ResultPrinter {

	// Where to put the results
	public static String resultsDirectory;

	// The results object
	public static Results results;

	public static void clearResults(ScenarioSet set) {
		results = new Results(set);
	}
	
	public static void newTTDPlots(ScenarioSet set, int run) {
		if(results.ttdPlots != null) results.ttdPlots.dispose(); //only display the current run
		results.ttdPlots = new TimeToDetectionPlots(set.getIterations(), set.getNodeStructure().getTimeSteps().get(set.getNodeStructure().getTimeSteps().size()-1).getRealTime(), run);
	}

	public static void storeResults(int run, int iteration, ExtendedConfiguration newConfiguration,
			ExtendedConfiguration bestConfiguration, ExtendedConfiguration currentConfiguration, ScenarioSet set) {

		if(results == null)
			clearResults(set);

		results.storeResult(run, iteration, newConfiguration, bestConfiguration, currentConfiguration, set);
	}

	/**
	 * Prints everything we currently know how to prints
	 */
	public static void printAll() {

		try {
			printAllConfigs();	
		} catch (Exception e) {
			System.out.println("Failed to print all configurations");
		}


		try {
			printBestConfigSum();	
		} catch (Exception e) {
			System.out.println("Failed to print best configuration summary");
		}

		try {
			printObjPerIterSum();	
		} catch (Exception e) {
			System.out.println("Failed to print objective per iteration summary");
		}
	}

	/**
	 * If results have been stored for all configs:
	 * One file per type per run will be generated
	 * @throws IOException 
	 */
	public static void printAllConfigs() throws IOException {
		if(!results.allConfigs)
			return;

		for(Type type: results.allConfigsMap.keySet()) {			
			for(Integer run: results.allConfigsMap.get(type).keySet()) {
				String fileName = "run_" + run +"_" + type.toString();
				List<String> lines = new ArrayList<String>();
				for(Integer iteration: results.allConfigsMap.get(type).get(run).keySet()) {
					Configuration configuration = results.allConfigsMap.get(type).get(run).get(iteration);
					// Compute ttd for only detecting scenarios
					ArrayList<Float> ttd = new ArrayList<Float>();
					ArrayList<Float> weights = new ArrayList<Float>();
					float scenariosDetected = 0;
					for(Scenario scenario: results.set.getScenarios()) {
						if(configuration.getTimesToDetection().containsKey(scenario)) {
							float timeToDetection = configuration.getTimesToDetection().get(scenario);
							if(timeToDetection == 1000000) {
								// Do nothing...
							} else {
								ttd.add(timeToDetection);
								weights.add(results.set.getScenarioWeights().get(scenario));
								scenariosDetected += 100*results.set.getScenarioWeights().get(scenario)/results.set.getTotalScenarioWeight();
								//		scenariosDetected += results.set.getScenarioProbabilities().get(scenario);					
							}
						} else {
							// It didn't detect, do nothing.
						}
						
					}
					float weightedAverageTTD = 0;
					float sum = 0;
					for(float value: weights) sum += value;
					for(int i=0; i<ttd.size(); i++) weightedAverageTTD += ttd.get(i)*weights.get(i)/sum;
					
					String timeToDetection =  Constants.decimalFormat.format((weightedAverageTTD));	
					String line = iteration + ", " + timeToDetection + ", " + scenariosDetected;
					for(Sensor sensor: configuration.getSensors()) {
						line += ", " + sensor.getNodeNumber() + ": " + sensor.getSensorType();
					}
					lines.add(line);
				}
				FileUtils.writeLines(new File(resultsDirectory, fileName + ".csv"), lines);
			}	
		}

	}

	/**
	 * Prints a unique list of all the best configurations found
	 * @throws IOException 
	 */
	public static void printBestConfigSum() throws IOException {
		if(!results.bestConfigSum)
			return;
		String fileName = "best_configurations";
		Map<Float, String> linesToSort = new HashMap<Float, String>();
		List<String> lines = new ArrayList<String>();	
		lines.add("Scenarios with Leak Detected %, Average ETFD of Successful Scenarios, Range of ETFD over Successful Scenarios, Scenarios with No Leak Detected, Cost of Configuration, Sensor Types (x y z)");

		Map<Integer, List<Configuration>> resultsByNumSensors = new TreeMap<Integer, List<Configuration>>();
		for(Configuration configuration: results.bestConfigSumList) {
			Integer sensors = configuration.getSensors().size();
			if(!resultsByNumSensors.containsKey(sensors))
				resultsByNumSensors.put(sensors, new ArrayList<Configuration>());
			resultsByNumSensors.get(sensors).add(configuration);
		}

		List<Scenario> scenariosThatDidNotDetect = new ArrayList<Scenario>();
		for(List<Configuration> configurations: resultsByNumSensors.values()) {
			for(Configuration configuration: configurations) {
				String scenariosNotDetected = "";
				ArrayList<Float> ttd = new ArrayList<Float>();
				ArrayList<Float> weights = new ArrayList<Float>();
				// float scenariosDetected = 0; // Uniform distribution among scenarios
				float scenariosDetected = 0;

				for(Scenario scenario: results.set.getScenarios()) {
					if(configuration.getTimesToDetection().containsKey(scenario)) {
						float timeToDetection = configuration.getTimesToDetection().get(scenario);
						if(timeToDetection == 1000000) {
							if(!scenariosThatDidNotDetect.contains(scenario))
								scenariosThatDidNotDetect.add(scenario);
							scenariosNotDetected += scenariosNotDetected.isEmpty() ? scenario : " " + scenario;
						} else {
							ttd.add(timeToDetection);
							weights.add(results.set.getScenarioWeights().get(scenario));
							scenariosDetected += 100*results.set.getScenarioWeights().get(scenario)/results.set.getTotalScenarioWeight();
							//		scenariosDetected += results.set.getScenarioProbabilities().get(scenario);					
						}
					} else {
						// DId not detect
						if(!scenariosThatDidNotDetect.contains(scenario))
							scenariosThatDidNotDetect.add(scenario);
						scenariosNotDetected += scenariosNotDetected.isEmpty() ? scenario : " " + scenario;
					}
				}
				float weightedAverageTTD = 0;
				float sum = 0;
				for(float value: weights) sum += value;
				for(int i=0; i<ttd.size(); i++) weightedAverageTTD += ttd.get(i)*weights.get(i)/sum;
				float minYear = Float.MAX_VALUE;
				float maxYear = -Float.MAX_VALUE;			
				String line = scenariosDetected + ", " + weightedAverageTTD;	
				for(Sensor sensor: configuration.getSensors()) {
					if(sensor instanceof ExtendedSensor) {
						for(Scenario scenario: ((ExtendedSensor)sensor).getScenariosUsed().keySet()) {
							if(!((ExtendedSensor)sensor).isTriggeredInScenario(scenario)) {
								continue;
							}			
							if(scenariosThatDidNotDetect.contains(scenario)) {
								continue;
							}
							TreeMap<TimeStep, Double> ttds = ((ExtendedSensor)sensor).getScenariosUsed().get(scenario);
							for(TimeStep ts: ttds.keySet()) {
								if(ts.getRealTime() < minYear)
									minYear = ts.getRealTime();
								if(ts.getRealTime() > maxYear) {
									maxYear = ts.getRealTime();
								}
							}
						}
					}								
				}
				line += ",[" + minYear + " " + maxYear + "]," + scenariosNotDetected;
				float costOfConfig = results.set.costOfConfiguration(configuration);
				if(costOfConfig < 1000) line += ", " + Constants.decimalFormat.format(costOfConfig);
				else line += ", " + Constants.exponentialFormat.format(costOfConfig);
				for(Sensor sensor: configuration.getSensors()) {		
					Point3d xyz =results.set.getNodeStructure().getXYZFromIJK(sensor.getIJK());
					line += "," + sensor.getSensorType() + " (" + xyz.getX() + " " + xyz.getY() + " " + xyz.getZ() + ")";
				}
				linesToSort.put(costOfConfig, line);
				//lines.add(line);
			}
		}
		
		List<Float> keySet = new ArrayList<Float>();
		keySet.addAll(linesToSort.keySet());
		java.util.Collections.sort(keySet);
		for(float key: keySet){
			lines.add(linesToSort.get(key));
		}

		FileUtils.writeLines(new File(resultsDirectory, fileName + ".csv"), lines);
	}


	public static void printObjPerIterSum() throws IOException {
		if(!results.objPerIterSum) 
			return;

		for(Type type: results.objPerIterSumMap.keySet()) {	
			String fileName = "objective_summary_" + type.toString();
			List<String> lines = new ArrayList<String>();
			for(Integer iteration: results.objPerIterSumMap.get(type).keySet()) {
				if(lines.isEmpty()) { // Add the heading
					String line = "Iteration";
					for(Integer run: results.objPerIterSumMap.get(type).get(iteration).keySet()) {
						line += ", run" + run + " ETFD" + ", run" + run + " % scenarios detected";
					}
					lines.add(line);
				}	
				String line = String.valueOf(iteration);
				for(ObjectiveResult objRes: results.objPerIterSumMap.get(type).get(iteration).values()) {
					line += ", " + (Double.isNaN(objRes.timeToDetectionInDetected) ? "" : objRes.timeToDetectionInDetected) + ", " + 
								   (Double.isNaN(objRes.percentScenariosDetected) ? "" : objRes.percentScenariosDetected);
				}
				lines.add(line);
			}	
			FileUtils.writeLines(new File(resultsDirectory, fileName + ".csv"), lines);
		}
	}

}
