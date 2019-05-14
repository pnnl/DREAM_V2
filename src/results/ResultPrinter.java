package results;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;

import objects.Configuration;
import objects.ExtendedConfiguration;
import objects.ExtendedSensor;
import objects.NodeStructure;
import objects.ScenarioSet;
import objects.Sensor;
import objects.SensorSetting;
import results.Results.ObjectiveResult;
import results.Results.Type;
import utilities.Constants;
import utilities.Point3f;
import utilities.Point3i;

/**
 * Utility methods for printing results
 * @author port091
 * @author rodr144
 * @author whit162
 */
public class ResultPrinter {

	// Where to put the results
	public static String resultsDirectory;

	// The results object
	public static Results results;

	// Whether or not we want to run python scripts (disable for scatterplot runs)
	public static boolean runScripts = true;
	
	private static String timeUnit = "years";
	private static String xUnit;
	
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
	public static void printAll(NodeStructure nodeStructure) {
		xUnit = nodeStructure.getUnit("x");
		try {
			printAllConfigs();	
		} catch (Exception e) {
			System.out.println("Failed to print all configurations");
		}


		try {
			printBestConfigSum();	
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
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
				StringBuilder header = new StringBuilder();
				header.append("Iteration,Average TTD (Weighted),Scenarios Detected,Sensors");
				lines.add(header.toString());
				for(Integer iteration: results.allConfigsMap.get(type).get(run).keySet()) {
					if(iteration<0) continue; //Skip the initialization iterations
					
					Configuration configuration = results.allConfigsMap.get(type).get(run).get(iteration);
					
					float scenariosDetected = configuration.countScenariosDetected();
					float totalWeightsForDetectedScenarios = 0.0f;
					float weightedAverageTTD = 0.0f;
					
					// If we want weighted, we need to weight based on the normalized value of just the detected scenarios
					for(String detectingScenario: configuration.getTimesToDetection().keySet()) {
						totalWeightsForDetectedScenarios += results.set.getScenarioWeights().get(detectingScenario);
					}
					
					for(String detectingScenario: configuration.getTimesToDetection().keySet()) {
						float scenarioWeight = results.set.getScenarioWeights().get(detectingScenario);
						weightedAverageTTD += configuration.getTimesToDetection().get(detectingScenario) * (scenarioWeight/totalWeightsForDetectedScenarios);
					}
					
					StringBuilder line = new StringBuilder();
					line.append(iteration + "," + Constants.decimalFormat.format(weightedAverageTTD) + " " + timeUnit + "," + scenariosDetected);
					for(Sensor sensor: configuration.getSensors()) {
						if(sensor.getSensorType().contains("Electrical Conductivity"))
							line.append("," + sensor.getXYZ().toString() + "+" + ((ExtendedSensor)sensor).getPairXYZ().toString() + " " + Sensor.sensorAliases.get(sensor.getSensorType()));
						else
							line.append("," + sensor.getXYZ().toString() + " " + Sensor.sensorAliases.get(sensor.getSensorType()));
					}
					lines.add(line.toString());
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
		String fileName = "best_configurations";
		String ttdFileName = "best_configuration_ttds";
		String vadFileName = "best_configuration_vads";
		
		List<String> lines = new ArrayList<String>();
		List<String> ttdLines = new ArrayList<String>();
		List<String> vadLines = new ArrayList<String>();
		
		Map<Float, List<String>> linesToSort = new HashMap<Float, List<String>>();
		Map<Float, List<String>> ttdLinesToSort = new HashMap<Float, List<String>>();
		Map<Float, List<String>> vadLinesToSort = new HashMap<Float, List<String>>();
		
		lines.add(",Scenarios with Leak Detected,Scenarios with Leak Detected (Unweighted),Average TTD of Successful Scenarios,Average TTD of Successful Scenarios (Unweighted),"+
				  "Range of TTD over Successful Scenarios,Scenarios with No Leak Detected,Number of Wells,Cost of Configuration,Volume of Aquifer Degraded,Sensor Types (x y z)");

		List<String> scenariosThatDidNotDetect = new ArrayList<String>();
		for(ExtendedConfiguration configuration: results.bestConfigSumList) {
			String scenariosNotDetected = "";
			
			float scenariosDetected = configuration.countScenariosDetected();
			float totalScenarios = results.set.getScenarios().size();
			float globallyWeightedPercentage = 0;
			float unweightedAverageTTD = configuration.getUnweightedTimeToDetectionInDetectingScenarios() / scenariosDetected;
			int numberOfWells = results.set.countWells(configuration);
			float costOfConfig = results.set.costOfConfiguration(configuration);
			float volumeDegraded = SensorSetting.getVolumeDegradedByTTDs(configuration.getTimesToDetection(), results.set.getScenarios().size());
			float totalWeightsForDetectedScenarios = 0.0f;
			float weightedAverageTTD = 0.0f;
			
			//Determine the sum of scenario weights for detecting scenarios
			for(String scenario: results.set.getScenarios()) {
				if(configuration.getTimesToDetection().containsKey(scenario)) {	
					totalWeightsForDetectedScenarios += results.set.getScenarioWeights().get(scenario);
				} else {
					scenariosNotDetected += scenariosNotDetected.isEmpty() ? scenario : " " + scenario;	
				}
			}	
			
			// If we want weighted, we need to weight based on the normalized value of just the detected scenarios
			// If we wanted weighted percentages, just add up the globally normalized value of detected scenarios
			for(String detectingScenario: configuration.getTimesToDetection().keySet()) {
				float scenarioWeight = results.set.getScenarioWeights().get(detectingScenario);
				weightedAverageTTD += configuration.getTimesToDetection().get(detectingScenario) * (scenarioWeight/totalWeightsForDetectedScenarios);
				globallyWeightedPercentage += results.set.getGloballyNormalizedScenarioWeight(detectingScenario)*100;
			}
			
			//Determining the min and max years from the configuration
			float minYear = Float.MAX_VALUE;
			float maxYear = -Float.MAX_VALUE;			
			for(Sensor sensor: configuration.getSensors()) {
				if(sensor instanceof ExtendedSensor) {
					for(String scenario: ((ExtendedSensor)sensor).getScenariosUsed().keySet()) {
						if(!((ExtendedSensor)sensor).isTriggeredInScenario(scenario)) continue;	
						if(scenariosThatDidNotDetect.contains(scenario)) continue;
						TreeMap<Float, Double> ttds = ((ExtendedSensor)sensor).getScenariosUsed().get(scenario);
						for(Float ts: ttds.keySet()) {
							if(ts < minYear)
								minYear = ts;
							if(ts > maxYear) {
								maxYear = ts;
							}
						}
					}
				}								
			}
			
			StringBuilder line = new StringBuilder();
			line.append(Constants.percentageFormat.format(globallyWeightedPercentage) + "%," +
					  Constants.percentageFormat.format((scenariosDetected/totalScenarios)*100) + "%," + 
					  Constants.percentageFormat.format(weightedAverageTTD) + " " + timeUnit + "," + 
					  Constants.percentageFormat.format(unweightedAverageTTD) + " " + timeUnit);
			
			line.append("," + Constants.percentageFormat.format(minYear) + "-" + Constants.percentageFormat.format(maxYear) + " " + timeUnit + "," + scenariosNotDetected);
			
			line.append("," + numberOfWells);
			
			line.append("," + ((costOfConfig < 1000) ? Constants.percentageFormat.format(costOfConfig) : Constants.exponentialFormat.format(costOfConfig)));
			
			line.append("," + Constants.decimalFormat.format(volumeDegraded) + (xUnit.equals("") ? "" : " " + xUnit + "^3"));
			
			for(Sensor sensor: configuration.getSensors()) {
				Point3f xyz = results.set.getNodeStructure().getXYZCenterFromIJK(sensor.getIJK());
				// Special exception for ERT where no z is needed
				if(sensor.getSensorType().contains("Electrical Conductivity")) {
					Point3i ijkPair = results.set.getNodeStructure().getIJKFromNodeNumber(((ExtendedSensor)sensor).getNodePairNumber());
					Point3f xyzPair = results.set.getNodeStructure().getXYZCenterFromIJK(ijkPair);
					line.append("," + Sensor.sensorAliases.get(sensor.getSensorType()) + " (" + xyz.getX() + " " + xyz.getY() + ") (" + xyzPair.getX() + " " + xyzPair.getY() + ")");
				} else {
					line.append("," + Sensor.sensorAliases.get(sensor.getSensorType()) + " (" + xyz.getX() + " " + xyz.getY() + " " + xyz.getZ() + ")");
				}
			}
			
			// If cost is missing, add the key with a blank array
			if(!linesToSort.containsKey(costOfConfig)) {
				linesToSort.put(costOfConfig, new ArrayList<String>());
				ttdLinesToSort.put(costOfConfig, new ArrayList<String>());
				vadLinesToSort.put(costOfConfig, new ArrayList<String>());
			}
			
			// Store lines listing the best configurations
			linesToSort.get(costOfConfig).add(line.toString());
			
			// Store lines listing the TTD per best configuration
			Collection<Float> ttds = configuration.getTimesToDetection().values();
			StringBuilder ttdLine = new StringBuilder();
			for(float ttd: ttds)
				ttdLine.append("," + Constants.percentageFormat.format(ttd) + " " + timeUnit);
			ttdLinesToSort.get(costOfConfig).add(ttdLine.toString());
			
			// Store lines listing the VAD per best configuration
			Collection<Float> vads = SensorSetting.getVolumesDegraded(configuration.getTimesToDetection()).values();
			StringBuilder vadLine = new StringBuilder();
			for(float vad: vads)
				vadLine.append("," + Constants.decimalFormat.format(vad) + (xUnit.equals("") ? "" : " " + xUnit + "^3"));
			vadLinesToSort.get(costOfConfig).add(vadLine.toString());
		}
		
		// Sort the configurations by cost
		List<Float> keySet = new ArrayList<Float>();
		keySet.addAll(linesToSort.keySet());
		java.util.Collections.sort(keySet);
		
		// Assemble strings of the best configurations
		for(float key: keySet) {
			for(String line: linesToSort.get(key)) {
				int count = lines.size();
				lines.add("Config_" + count + "," + line);
			}
		}
		String ensemble = results.set.getScenarioEnsemble();
		File bestConfigFile = new File(resultsDirectory, fileName + ".csv");
		FileUtils.writeLines(bestConfigFile, lines);
		if(runScripts){
			//Try running script
			try {
				String script = Constants.userDir+File.separator+"scripts"+File.separator+"plot_dream_3panel.py";
				String solutionSpace = resultsDirectory + File.separator + ensemble + "_solutionSpace.txt";
				Runtime runtime = Runtime.getRuntime();
				String command = "python \"" + script + "\" \"" + solutionSpace + "\" \"" + bestConfigFile.getAbsolutePath() + "\"";
				runtime.exec(command);
			} catch(Exception e) {
				System.out.println("Install python3 and required libraries to create a PDF visualization");
			}
		}
		
		// Assemble strings of TTDs for the best configurations
		StringBuilder title = new StringBuilder();
		for(String scenario: results.set.getScenarios()) {
			title.append("," + scenario);
		}
		ttdLines.add(title.toString());
		for(float key: keySet) {
			for(String line: ttdLinesToSort.get(key)) {
				int count = ttdLines.size();
				ttdLines.add("Config_" + count + line);
			}
		}
		File ttdFile = new File(resultsDirectory, ttdFileName + ".csv");
		FileUtils.writeLines(ttdFile, ttdLines);
		if(runScripts){
			//Try running script
			try {
				String script = Constants.userDir+File.separator+"scripts"+File.separator+"plot_best_config_ttds.py";
				Runtime runtime = Runtime.getRuntime();
				String command = "python \"" + script + "\" \"" + ttdFile.getAbsolutePath() + "\"";
				runtime.exec(command);
			} catch(Exception e) {
				System.out.println("Install python3 and required libraries to create TTD plots");
			}
		}
		
		// Assemble strings of VADs for the best configurations
		vadLines.add(title.toString());
		for(float key: keySet){
			for(String line: vadLinesToSort.get(key)) {
				int count = vadLines.size();
				vadLines.add("Config_" + count + line);
			}
		}
		File vadFile = new File(resultsDirectory, vadFileName + ".csv");
		FileUtils.writeLines(vadFile, vadLines);
		if(runScripts){
			//Try running script
			try {
				String script = Constants.userDir+File.separator+"scripts"+File.separator+"plot_best_config_vads.py";
				Runtime runtime = Runtime.getRuntime();
				String command = "python \"" + script + "\" \"" + vadFile.getAbsolutePath() + "\"";
				runtime.exec(command);
			} catch(Exception e) {
				System.out.println("Install python3 and required libraries to create VAD plots");
			}
		}
	}


	public static void printObjPerIterSum() throws IOException {
		if(!results.objPerIterSum) 
			return;
		
		for(Type type: results.objPerIterSumMap.keySet()) {	
			String fileName = "objective_summary_" + type.toString();
			List<String> lines = new ArrayList<String>();
			for(Integer iteration: results.objPerIterSumMap.get(type).keySet()) {
				if(lines.isEmpty()) { // Add the heading
					StringBuilder line = new StringBuilder();
					line.append("Iteration");
					for(Integer run: results.objPerIterSumMap.get(type).get(iteration).keySet()) {
						line.append(",Run" + run + ": TTD,Run" + run + ": Scenarios Detected");
					}
					lines.add(line.toString());
				}
				StringBuilder line = new StringBuilder();
				line.append(String.valueOf(iteration));
				
				for(ObjectiveResult objRes: results.objPerIterSumMap.get(type).get(iteration).values()) {
					line.append(", " + (Double.isNaN(objRes.timeToDetectionInDetected) ? "" : Constants.percentageFormat.format(objRes.timeToDetectionInDetected) + " " + timeUnit) +
							"," + (Double.isNaN(objRes.percentScenariosDetected) ? "" : Constants.percentageFormat.format(objRes.percentScenariosDetected)) + "%");
				}
				lines.add(line.toString());
			}
			File fileToWrite = new File(resultsDirectory, fileName + ".csv");
			FileUtils.writeLines(fileToWrite, lines);
			
			if(runScripts){
				//Try running script
				try {
					String script = Constants.userDir+File.separator+"scripts"+File.separator+"plot_dreamout01.py";
					Runtime runtime = Runtime.getRuntime();
					String command = "python \"" + script + "\" \"" + fileToWrite.getAbsolutePath() + "\"";
					runtime.exec(command);
				} catch(Exception e) {
					System.out.println("Install python3 and required libraries to create a PDF visualization");
				}
			}
		}
	}

	public static void printPlotData(List<Float> percentScenariosDetected, List<Float> averageTTDInDetecting, List<Float> costOfConfig, List<Configuration> configs, List<Float> volumeDegraded) throws IOException{
		String fileName = "plot_data.csv";
		List<String> lines = new ArrayList<String>();
		lines.add("Percent of Scenarios Detected, Average TTD in Detecting Scenarios, Cost, Average Volume Degraded, Total Number of Sensors, Sensor Locations");
		int numConfigs = costOfConfig.size();
		for(int i=0; i<numConfigs; ++i){
			StringBuilder line = new StringBuilder();
			line.append("");
			line.append(percentScenariosDetected.get(i) + ",");
			line.append(averageTTDInDetecting.get(i) + ",");
			line.append(costOfConfig.get(i) + ",");
			line.append(volumeDegraded.get(i) + ",");
			line.append(configs.get(i).getSensors().size());
			for(Sensor sensor: configs.get(i).getSensors()){
				Point3f xyz =results.set.getNodeStructure().getXYZEdgeFromIJK(sensor.getIJK());
				line.append("," + Sensor.sensorAliases.get(sensor.getSensorType()) + " (" + xyz.getX() + " " + xyz.getY() + " " + xyz.getZ() + ")");
			}
			lines.add(line.toString());
		}
		FileUtils.writeLines(new File(resultsDirectory, fileName), lines);
	}
	
}
