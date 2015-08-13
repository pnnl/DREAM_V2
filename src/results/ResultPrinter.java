package results;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;

import objects.Configuration;
import objects.ExtendedConfiguration;
import objects.ExtendedSensor;
import objects.Scenario;
import objects.ScenarioSet;
import objects.Sensor;
import objects.TimeStep;
import results.Results.Type;
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
					String line = iteration + ", " + configuration.getTimeToDetection();
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
		List<String> lines = new ArrayList<String>();	
		lines.add("Scenarios with Leak Detected %, Average ETFD of Successful Scenarios, Range of ETFD over Successful Scenarios, Scenarios with No Leak Detected, Sensor Types (x y z)");
		for(Configuration configuration: results.bestConfigSumList) {
			String scenariosNotDetected = "";
			float ttd = 0;
			float scenariosDetected = 0;		
			for(Scenario scenario: configuration.getTimesToDetection().keySet()) {
				float timeToDetection = configuration.getTimesToDetection().get(scenario);
				if(timeToDetection == results.set.getScenarioProbabilities().get(scenario)*1000000) {
					scenariosNotDetected += scenariosNotDetected.isEmpty() ? scenario : " " + scenario;
				} else {
					ttd += timeToDetection;
					scenariosDetected += results.set.getScenarioProbabilities().get(scenario);					
				}			
			}
			float percentScenariosDetected = scenariosDetected * 100;
			float minYear = Float.MAX_VALUE;
			float maxYear = -Float.MAX_VALUE;			
			String line = percentScenariosDetected + ", " + ttd;	
			for(Sensor sensor: configuration.getSensors()) {
				if(sensor instanceof ExtendedSensor) {
					for(TreeMap<TimeStep, Double> ttds  : ((ExtendedSensor)sensor).getScenariosUsed().values()) {
						for(TimeStep ts: ttds.keySet()) {
							if(ts.getRealTime() < minYear)
								minYear = ts.getRealTime();
							if(ts.getRealTime() > maxYear)
								maxYear = ts.getRealTime();
						}
					}
				}								
			}
			line += ",[" + minYear + " " + maxYear + "]," + scenariosNotDetected;
			for(Sensor sensor: configuration.getSensors()) {		
				Point3d xyz =results.set.getNodeStructure().getXYZFromIJK(sensor.getIJK());
				line += "," + sensor.getSensorType() + " (" + xyz.getX() + " " + xyz.getY() + " " + xyz.getZ() + ")";
			}
			
			lines.add(line);
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
						line += ", run" + run;
					}
					lines.add(line);
				}	
				String line = String.valueOf(iteration);
				for(Float ttd: results.objPerIterSumMap.get(type).get(iteration).values()) {
					line += ", " + ttd;
				}
				lines.add(line);
			}	
			FileUtils.writeLines(new File(resultsDirectory, fileName + ".csv"), lines);
		}
	}
	
}
