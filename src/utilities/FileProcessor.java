package utilities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

/**
 * Legacy code, not actually sure what this does... -Luke
 * @author port091
 */

public class FileProcessor {

	public enum Statistic {
		average, maximum
	};

	public static void main(String[] args) {
		// PostProcess("C:\\Users\\d3x078\\.STORM");
		//BestRun("C:\\Users\\d3x078\\.STORM\\1000", 15.0);				
		//ArrayList<String> result = GetSensorTypes("\\\\pnl\\projects\\sadovsky\\STOMP Data\\Diana Bacon Runs 03\\run_1\\plot.125");
		//System.out.println(result.size());
		AppendResults("C:\\Users\\d3x078\\.STORM\\20140103 Leak Location Variation");
	}
	
	public static void AppendResults(String d)
	{
		ArrayList<String> outToFile = new ArrayList<String>();
		outToFile.add("Run,Iteration,E(TFD)");
		double fileCounter = 0.0;
		File directory = new File(d);
		if (directory.isDirectory()) {
			for (File subFiles : directory.listFiles()) {
				if (!subFiles.getName().contains("Historgram")) {
					fileCounter = fileCounter + 1.0;
					System.out.println("Processing file: " + subFiles);
					try {
						List<String> lines = FileUtils.readLines(subFiles);
						for (String line : lines) {
							if (!line.contains("TTD")) {
								String[] tokens = line.split("\t");
								outToFile.add(Double.toString(fileCounter) + "," + tokens[0] +"," + tokens[1]);								
							}
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		try {
			String timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss")
					.format(Calendar.getInstance().getTime());
			FileUtils.writeLines(new File(d + "_PLOT_" + timeStamp + ".txt"), outToFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void PostProcess(String d) {
		HashMap<Double, Integer> summaryData = new HashMap<Double, Integer>();
		double fileCounter = 0.0;
		File directory = new File(d);
		if (directory.isDirectory()) {
			for (File subFiles : directory.listFiles()) {
				if (subFiles.getName().contains("Histogram")) {
					fileCounter = fileCounter + 1.0;
					System.out.println("Processing file: " + subFiles);
					try {
						List<String> lines = FileUtils.readLines(subFiles);
						for (String line : lines) {
							if (!line.contains("TTD")) {
								String[] tokens = line.split("\t");
								Double ttd = Double.parseDouble(tokens[0]);
								int count = Integer.parseInt(tokens[1]);
								if (!summaryData.containsKey(ttd)) {
									summaryData.put(ttd, count);
								} else {
									count += summaryData.get(ttd);
									summaryData.remove(ttd);
									summaryData.put(ttd, count);
								}
							}
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		ArrayList<String> averageStrings = new ArrayList<String>();
		ArrayList<Double> sortedKeys = new ArrayList<Double>();
		sortedKeys.addAll(summaryData.keySet());
		Collections.sort(sortedKeys);

		for (double key : sortedKeys) {
			double averageCount = summaryData.get(key) / fileCounter;
			averageStrings.add(Constants.exponentialFormat.format(key) + ", "
					+ Constants.decimalFormat.format(averageCount));

		}
		averageStrings.add(0, "TTD, Average count of occurences in "
				+ Constants.decimalFormat.format(fileCounter) + " runs.");
		try {
			String timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss")
					.format(Calendar.getInstance().getTime());
			FileUtils.writeLines(new File(d + "\\MetaData_" + timeStamp
					+ ".txt"), averageStrings);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void BestRun(String path, double d) {
		ArrayList<String> bestRuns = new ArrayList<String>();
		double bestValue = 0;
		File directory = new File(path);
		if (directory.isDirectory()) {
			for (File subFiles : directory.listFiles()) {
				if (subFiles.getName().contains("Histogram")) {
					System.out.println("Processing file: " + subFiles);
					try {
						List<String> lines = FileUtils.readLines(subFiles);
						for (String line : lines) {
							if (!line.contains("TTD")) {
								String[] tokens = line.split("\t");
								double ttd = Double.parseDouble(tokens[0]);
								int count = Integer.parseInt(tokens[1]);
								if (ttd == d && count == bestValue) {
									bestRuns.add(subFiles.getName());
									bestValue = count;
								}
								if (ttd == d && count > bestValue) {
									bestRuns.clear();
									bestRuns.add(subFiles.getName());
									bestValue = count;
								}
							}
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		System.out
				.println("\nThe run(s) returning the most solutions, where solutions = "
						+ Constants.decimalFormat.format(bestValue)
						+ ", for an objective of " + d);
		for (String run : bestRuns) {
			System.out.println(run);
		}

	}

	public static ArrayList<String> GetSensorTypes(String path) {
		ArrayList<String> validSensorTypes = new ArrayList<String>();
		validSensorTypes.add("Gas Saturation");
		validSensorTypes.add("Gas Pressure");		
		ArrayList<String> containedSensorTypes = new ArrayList<String>();
		File directory = new File(path);
		
		
		if (directory.isDirectory()) {
			System.out.println("Point me to a plot file.");
		} else {
			File file = new File(path);
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line = br.readLine();
				while(line != null){					
					//System.out.println(lineCounter);
					for(String s:validSensorTypes){
						if(line.contains(s)){
							if(!containedSensorTypes.contains(s)){
								containedSensorTypes.add(s);
								//System.out.println("s");
							}
						}
					}
					line = br.readLine();
				}
				br.close();
				
			} catch (Exception e) {
				System.out.println(e);
			}

		}
		return containedSensorTypes;
	}

}
