package results;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

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
	
	private static String timeUnit;
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
		timeUnit = nodeStructure.getUnit("times");
		xUnit = nodeStructure.getUnit("x");
		try {
			printAllConfigs();	
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to print all configurations");
		}
		try {
			printBestConfig();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to print best configuration summary");
		}
		try {
			printBestConfigTTD();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to print best configuration TTDs");
		}
		try {
			printBestConfigVAD();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to print best configuration VADs");
		}
		try {
			printObjPerIterSum();	
		} catch (Exception e) {
			e.printStackTrace();
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
	 * Prints the best_configurations.csv
	 * All unique configurations are listed, ordered by cost of configuration
	 * Each configuration lists: 
	 * 		% scenarios detected, average TTD, range of TTD, scenarios not detected, number of wells,
	 * 		cost of configuration, volume of aquifer degraded, and a list of sensors
	 * @throws IOException
	 */
	public static void printBestConfig() throws IOException {
		List<String> lines = new ArrayList<String>();
		Map<Float, List<String>> linesToSort = new HashMap<Float, List<String>>();
		
		if(results.set.getEqualWeights()==false) {
			lines.add(",Scenarios with Leak Detected,Scenarios with Leak Detected (Unweighted),Average TTD of Successful Scenarios,Average TTD of Successful Scenarios (Unweighted),"+
					"Range of TTD over Successful Scenarios,Scenarios with No Leak Detected,Number of Wells,Cost of Configuration,Average Volume of Aquifer Degraded,Sensor Types (x y z)");
		} else {
			lines.add(",Scenarios with Leak Detected,Average TTD of Successful Scenarios,Range of TTD over Successful Scenarios,Scenarios with No Leak Detected,"+
					"Number of Wells,Cost of Configuration,Average Volume of Aquifer Degraded,Sensor Types (x y z)");
		}
		
		List<String> scenariosThatDidNotDetect = new ArrayList<String>();
		for(ExtendedConfiguration configuration: results.bestConfigSumList) {
			String scenariosNotDetected = "";
			float scenariosDetected = configuration.countScenariosDetected();
			float totalScenarios = results.set.getScenarios().size();
			float globallyWeightedPercentage = 0;
			float unweightedAverageTTD = configuration.getUnweightedTimeToDetectionInDetectingScenarios() / scenariosDetected;
			int numberOfWells = results.set.countWells(configuration);
			float costOfConfig = results.set.costOfConfiguration(configuration);
			float volumeDegraded = SensorSetting.getAverageVolumeDegraded(configuration.getTimesToDetection());
			float totalWeightsForDetectedScenarios = 0.0f;
			float weightedAverageTTD = 0.0f;
			
			//Determine the sum of scenario weights for detecting scenarios
			for(String scenario: results.set.getScenarios()) {
				if(configuration.getTimesToDetection().containsKey(scenario))
					totalWeightsForDetectedScenarios += results.set.getScenarioWeights().get(scenario);
				else
					scenariosNotDetected += scenariosNotDetected.isEmpty() ? scenario : " " + scenario;
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
			float maxYear = Float.MIN_VALUE;
			for(Sensor sensor: configuration.getSensors()) {
				if(sensor instanceof ExtendedSensor == false) continue;
				for(String scenario: ((ExtendedSensor)sensor).getScenariosUsed().keySet()) {
					if(!((ExtendedSensor)sensor).isTriggeredInScenario(scenario)) continue;
					if(scenariosThatDidNotDetect.contains(scenario)) continue;
					TreeMap<Float, Double> ttds = ((ExtendedSensor)sensor).getScenariosUsed().get(scenario);
					for(Float ts: ttds.keySet()) {
						if(ts < minYear)
							minYear = ts;
						if(ts > maxYear)
							maxYear = ts;
					}
				}
			}
			
			StringBuilder line = new StringBuilder();
			line.append(Constants.percentageFormat.format(globallyWeightedPercentage) + "%,");
			if(results.set.getEqualWeights()==false)
				line.append(Constants.percentageFormat.format((scenariosDetected/totalScenarios)*100) + "%,");
			line.append(Constants.percentageFormat.format(weightedAverageTTD) + " " + timeUnit + ",");
			if(results.set.getEqualWeights()==false)
				line.append(Constants.percentageFormat.format(unweightedAverageTTD) + " " + timeUnit + ",");
			line.append(Constants.percentageFormat.format(minYear) + "-" + Constants.percentageFormat.format(maxYear) + " " + timeUnit + "," + scenariosNotDetected);
			line.append("," + numberOfWells);
			line.append("," + ((costOfConfig < 1000) ? Constants.percentageFormat.format(costOfConfig) : Constants.exponentialFormat.format(costOfConfig)));
			line.append("," + Constants.decimalFormat.format(volumeDegraded) + (xUnit.equals("") ? "" : " " + xUnit + "�"));
			
			for(Sensor sensor: configuration.getSensors()) {
				Point3f xyz = results.set.getNodeStructure().getXYZFromIJK(sensor.getIJK());
				// Special exception for ERT where no z is needed
				if(sensor.getSensorType().contains("Electrical Conductivity")) {
					Point3i ijkPair = results.set.getNodeStructure().getIJKFromNodeNumber(((ExtendedSensor)sensor).getNodePairNumber());
					Point3f xyzPair = results.set.getNodeStructure().getXYZFromIJK(ijkPair);
					line.append("," + Sensor.sensorAliases.get(sensor.getSensorType()) + " (" + xyz.getX() + " " + xyz.getY() + ") (" + xyzPair.getX() + " " + xyzPair.getY() + ")");
				} else {
					line.append("," + Sensor.sensorAliases.get(sensor.getSensorType()) + " (" + xyz.getX() + " " + xyz.getY() + " " + xyz.getZ() + ")");
				}
			}
			
			// If cost is missing, add the key with a blank array
			if(!linesToSort.containsKey(costOfConfig))
				linesToSort.put(costOfConfig, new ArrayList<String>());
			
			// Store lines listing the best configurations
			linesToSort.get(costOfConfig).add(line.toString());
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
		File bestConfigFile = new File(resultsDirectory, "best_configurations.csv");
		FileUtils.writeLines(bestConfigFile, lines);
		if(runScripts) {
			String solutionSpace = resultsDirectory + File.separator + ensemble + "_solutionSpace.txt";
			runPythonScript("plot_dream_3panel.py", new String[] {solutionSpace, bestConfigFile.getAbsolutePath()});
		}
	}
	
	/**
	 * Prints the best_configurations_ttds.csv
	 * All unique configurations are listed, ordered by cost of configuration
	 * Each configuration lists the time to detection (TTD) per scenario
	 * @throws IOException
	 */
	public static void printBestConfigTTD() throws IOException {
		List<String> ttdLines = new ArrayList<String>();
		Map<Float, List<String>> ttdLinesToSort = new HashMap<Float, List<String>>();
		
		// Loop through the best configurations
		for(ExtendedConfiguration configuration: results.bestConfigSumList) {
			float costOfConfig = results.set.costOfConfiguration(configuration);
			// If cost is missing, add the key with a blank array
			if(!ttdLinesToSort.containsKey(costOfConfig))
				ttdLinesToSort.put(costOfConfig, new ArrayList<String>());
			// Store lines listing the TTD per best configuration
			Map<String, Float> timesToDetection = configuration.getTimesToDetection();
			StringBuilder ttdLine = new StringBuilder();
			for(String scenario: results.set.getScenarios()) {
				ttdLine.append(",");
				if(timesToDetection.containsKey(scenario))
					ttdLine.append(Constants.percentageFormat.format(timesToDetection.get(scenario)));
			}
			ttdLinesToSort.get(costOfConfig).add(ttdLine.toString());
		}
		
		// Sort the configurations by cost
		List<Float> keySet = new ArrayList<Float>();
		keySet.addAll(ttdLinesToSort.keySet());
		java.util.Collections.sort(keySet);
		
		// Assemble strings of TTDs for the best configurations
		StringBuilder title = new StringBuilder();
		title.append("Time to Detection (" + timeUnit + ")");
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
		File ttdFile = new File(resultsDirectory, "best_configuration_ttds.csv");
		FileUtils.writeLines(ttdFile, ttdLines);
		if(runScripts)
			runPythonScript("plot_best_config_ttds.py", new String[] {ttdFile.getAbsolutePath()});
	}
	
	/**
	 * Prints the best_configurations_vads.csv
	 * All unique configurations are listed, ordered by cost of configuration
	 * Each configuration lists the volume of aquifer degraded (VAD) per scenario
	 * @throws IOException
	 */
	public static void printBestConfigVAD() throws IOException {
		List<String> vadLines = new ArrayList<String>();
		Map<Float, List<String>> vadLinesToSort = new HashMap<Float, List<String>>();
		
		// Loop through the best configurations
		for(ExtendedConfiguration configuration: results.bestConfigSumList) {
			float costOfConfig = results.set.costOfConfiguration(configuration);
			// If cost is missing, add the key with a blank array
			if(!vadLinesToSort.containsKey(costOfConfig))
				vadLinesToSort.put(costOfConfig, new ArrayList<String>());
			// Store lines listing the TTD per best configuration
			Map<String, Float> ttds = configuration.getTimesToDetection();
			Map<String, Float> vadMap = SensorSetting.getVolumesDegraded(ttds);
			StringBuilder vadLine = new StringBuilder();
			for(String scenario: results.set.getScenarios()) {
				vadLine.append(",");
				if(vadMap.containsKey(scenario))
					vadLine.append(Constants.decimalFormat.format(vadMap.get(scenario)));
			}
			vadLinesToSort.get(costOfConfig).add(vadLine.toString());
		}
		
		// Sort the configurations by cost
		List<Float> keySet = new ArrayList<Float>();
		keySet.addAll(vadLinesToSort.keySet());
		java.util.Collections.sort(keySet);
		
		// Assemble strings of VADs for the best configurations
		StringBuilder title = new StringBuilder();
		title.append("Volume of Aquifer Degraded (" + xUnit + "�)");
		for(String scenario: results.set.getScenarios()) {
			title.append("," + scenario);
		}
		vadLines.add(title.toString());
		for(float key: keySet) {
			for(String line: vadLinesToSort.get(key)) {
				int count = vadLines.size();
				vadLines.add("Config_" + count + line);
			}
		}
		File vadFile = new File(resultsDirectory, "best_configuration_vads.csv");
		FileUtils.writeLines(vadFile, vadLines);
		if(runScripts)
			runPythonScript("plot_best_config_vads.py", new String[] {vadFile.getAbsolutePath()});
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
			
			if(runScripts) {
				runPythonScript("plot_dreamout01.py", new String[] {fileToWrite.getAbsolutePath()});
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
				Point3f xyz =results.set.getNodeStructure().getXYZFromIJK(sensor.getIJK());
				line.append("," + Sensor.sensorAliases.get(sensor.getSensorType()) + " (" + xyz.getX() + " " + xyz.getY() + " " + xyz.getZ() + ")");
			}
			lines.add(line.toString());
		}
		FileUtils.writeLines(new File(resultsDirectory, fileName), lines);
	}
	
	private static void runPythonScript(String scriptName, String[] args) {
		boolean printToConsole = true; //Determines whether we print Python outputs to console
		// We essentially create a local copy of the script so that it works when packaged in a JAR
		try {
	        InputStream in = ResultPrinter.class.getResourceAsStream("/"+scriptName);
	        File fileOut = new File(System.getProperty("java.io.tmpdir"),scriptName); //Within the user's temp directory
	        OutputStream out = FileUtils.openOutputStream(fileOut);
	        IOUtils.copy(in, out);
	        in.close();
	        out.close();
    	} catch (Exception e) {
    		System.out.println("Unable to copy the script to the temp directory");
    		e.printStackTrace();
    	}
		String script = System.getProperty("java.io.tmpdir")+File.separator+scriptName;
		StringBuilder command = new StringBuilder();
		if (System.getProperty("os.name").contains("Mac")) command.append("python3 " + script);
		else command.append("py -3 \"" + script + "\"");
		for(String arg: args) {
			command.append(" \"" + arg + "\"");
		}
		try {
			Process p;
			if (System.getProperty("os.name").contains("Mac")) {
				String temp = command.toString().replaceAll("\"", "");
				System.out.println(temp);
				p = Runtime.getRuntime().exec(temp);
			} else {
				p = Runtime.getRuntime().exec(command.toString());
			}
			if(printToConsole) {
				BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
				BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String line;
				while ((line = bri.readLine()) != null)
		            System.out.println(line);
		        bri.close();
				while ((line = bre.readLine()) != null)
		        	System.out.println(line);
		        bre.close();
			}
		} catch(Exception e) {
			System.out.println("Install python3 and required libraries to create a PDF visualization");
			e.printStackTrace();
		}
	}
	
	public void setTimeUnit(final String theUnit) {
		timeUnit = theUnit;
	}
}
