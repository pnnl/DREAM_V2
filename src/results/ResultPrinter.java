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

	public static void clearResults(ScenarioSet set, boolean makePlots) {
		results = new Results(set, makePlots);
	}
	
	public static void newTTDPlots(ScenarioSet set, int run) {
		if(results.resultsPlots){
			if(results.ttdPlots != null) results.ttdPlots.dispose(); //only display the current run
			results.ttdPlots = new TimeToDetectionPlots(set.getIterations(), set.getNodeStructure().getTimeSteps().get(set.getNodeStructure().getTimeSteps().size()-1).getRealTime(), run);
		}
	}

	public static void storeResults(int run, int iteration, ExtendedConfiguration newConfiguration,
			ExtendedConfiguration bestConfiguration, ExtendedConfiguration currentConfiguration, ScenarioSet set) {

		if(results == null)
			clearResults(set, true);

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
				
					float scenariosDetected = configuration.countScenariosDetected();
					float totalWeightsForDetectedScenarios = 0.0f;
					float weightedAverageTTD = 0.0f;

					// If we want weighted, we need to weight based on the normalized value of just the detected scenarios
					for(Scenario detectingScenario: configuration.getTimesToDetection().keySet()) {
						totalWeightsForDetectedScenarios += results.set.getScenarioWeights().get(detectingScenario);
					}	
					
					for(Scenario detectingScenario: configuration.getTimesToDetection().keySet()) {
						float scenarioWeight = results.set.getScenarioWeights().get(detectingScenario);
						weightedAverageTTD += configuration.getTimesToDetection().get(detectingScenario) * (scenarioWeight/totalWeightsForDetectedScenarios);
					}
					
					String line = iteration + ", " + Constants.percentageFormat.format(weightedAverageTTD) + ", " + scenariosDetected;
					for(Sensor sensor: configuration.getSensors()) {
						line += ", " + sensor.getNodeNumber() + ": " + Sensor.sensorAliases.get(sensor.getSensorType());
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
		lines.add("Scenarios with Leak Detected Weighted %, Scenarios with Leak Detected Un-Weighted %, Weighted Average TTD of Successful Scenarios, Unweighted Average TTD of Successful Scenarios, "+
				  "Unweighted Range of TTD over Successful Scenarios, Scenarios with No Leak Detected, Cost of Well Configuration ($20/ft), Sensor Types (x y z)");

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
				
				float scenariosDetected = configuration.countScenariosDetected();
				float totalScenarios = results.set.getScenarios().size();
				float globallyWeightedPercentage = 0;
				float unweightedAverageTTD = configuration.getUnweightedTimeToDetectionInDetectingScenarios() / scenariosDetected;
				float costOfConfig = results.set.costOfConfiguration(configuration);
				float totalWeightsForDetectedScenarios = 0.0f;
				float weightedAverageTTD = 0.0f;

				for(Scenario scenario: results.set.getScenarios()) {
					if(configuration.getTimesToDetection().containsKey(scenario)) {	
						totalWeightsForDetectedScenarios += results.set.getScenarioWeights().get(scenario);
					} else {
						scenariosNotDetected += scenariosNotDetected.isEmpty() ? scenario : " " + scenario;	
					}
				}	
				
				// If we want weighted, we need to weight based on the normalized value of just the detected scenarios
				// If we wanted weighted percentages, just add up the globally normalized value of detected scenarios
				for(Scenario detectingScenario: configuration.getTimesToDetection().keySet()) {
					float scenarioWeight = results.set.getScenarioWeights().get(detectingScenario);
					weightedAverageTTD += configuration.getTimesToDetection().get(detectingScenario) * (scenarioWeight/totalWeightsForDetectedScenarios);
					globallyWeightedPercentage += results.set.getGloballyNormalizedScenarioWeight(detectingScenario)*100;
				}
				
				
						
				
				float minYear = Float.MAX_VALUE;
				float maxYear = -Float.MAX_VALUE;			
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
				
				String line = Constants.percentageFormat.format(globallyWeightedPercentage) + ", " +
						  Constants.percentageFormat.format((scenariosDetected/totalScenarios)*100) + ", " + 
						  Constants.percentageFormat.format(weightedAverageTTD) + ", " + 
						  Constants.percentageFormat.format(unweightedAverageTTD);	
				
				line += ",[" + Constants.percentageFormat.format(minYear) + " " + Constants.percentageFormat.format(maxYear) + "]," + scenariosNotDetected;
				
				line += ", " + ((costOfConfig < 1000) ? Constants.decimalFormat.format(costOfConfig) : Constants.exponentialFormat.format(costOfConfig));
				
				for(Sensor sensor: configuration.getSensors()) {		
					Point3d xyz =results.set.getNodeStructure().getXYZEdgeFromIJK(sensor.getIJK());
					line += "," + Sensor.sensorAliases.get(sensor.getSensorType()) + " (" + xyz.getX() + " " + xyz.getY() + " " + xyz.getZ() + ")";
				}
				
				linesToSort.put(costOfConfig, line);
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
						line += ", run" + run + " TTD" + ", run" + run + " % scenarios detected";
					}
					lines.add(line);
				}	
				String line = String.valueOf(iteration);

				for(ObjectiveResult objRes: results.objPerIterSumMap.get(type).get(iteration).values()) {
					line += ", " + (Double.isNaN(objRes.timeToDetectionInDetected) ? "" : Constants.percentageFormat.format(objRes.timeToDetectionInDetected)) + ", " + 
								   (Double.isNaN(objRes.percentScenariosDetected) ? "" : Constants.percentageFormat.format(objRes.percentScenariosDetected));
				}
				lines.add(line);
			}	
			FileUtils.writeLines(new File(resultsDirectory, fileName + ".csv"), lines);
		}
	}

	public static void printPlotData(List<Float> percentScenariosDetected, List<Float> averageTTDInDetecting, List<Float> costOfConfig, List<Configuration> configs, List<Float> volumeDegraded) throws IOException{
		String fileName = "plot_data.csv";
		List<String> lines = new ArrayList<String>();
		lines.add("Percent of Scenarios Detected, Average TTD in Detecting Scenarios, Cost, Average Volume Degraded, Total Number of Sensors, Sensor Locations");
		int numConfigs = costOfConfig.size();
		for(int i=0; i<numConfigs; ++i){
			String line = "";
			line += percentScenariosDetected.get(i) + ",";
			line += averageTTDInDetecting.get(i) + ",";
			line += costOfConfig.get(i) + ",";
			line += volumeDegraded.get(i) + ",";
			line += configs.get(i).getSensors().size();
			for(Sensor sensor: configs.get(i).getSensors()){
				Point3d xyz =results.set.getNodeStructure().getXYZEdgeFromIJK(sensor.getIJK());
				line += "," + Sensor.sensorAliases.get(sensor.getSensorType()) + " (" + xyz.getX() + " " + xyz.getY() + " " + xyz.getZ() + ")";
			}
			lines.add(line);
		}
		FileUtils.writeLines(new File(resultsDirectory, fileName), lines);
	}
	
}
