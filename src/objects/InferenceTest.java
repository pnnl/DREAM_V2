package objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import utilities.Constants;
import wizardPages.Page_DetectionCriteria.DetectionCriteria;
/**
 * Class to store the criteria for what constitutes a detection.
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class InferenceTest {
	
	private List<HashMap<String, Integer>> activeTests;
	
	//Create a test, starting with only one sensor
	public InferenceTest(String sensorName, int min) {
		activeTests = new ArrayList<HashMap<String, Integer>>();
		HashMap<String, Integer> test = new HashMap<String, Integer>();
		test.put(sensorName, min);
		activeTests.add(test);
		Constants.log(Level.INFO, "Inference test: initialized", null);
		Constants.log(Level.CONFIG, "Inference test: configuration", this);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("Detection Criteria:\r\n");
		for(int i=0; i<activeTests.size(); i++) {
			builder.append("\tTest " + i+1 + "\r\n");
			for(String sensor: activeTests.get(i).keySet()) {
				builder.append("\t\t" + sensor + ": " + activeTests.get(i).get(sensor) + "\r\n");
			}
		}
		return builder.toString();
	}
	
	// Default inference test 
	public Boolean reachedInference(Map<String, Integer> triggeredByType) {
		
		// Loop through the active tests to compare with configuration
		for(HashMap<String, Integer> test: activeTests) {
			boolean checkTest = true;
			for(String sensor: test.keySet()) {
				// If the configuration doesn't match any part of the test, failure!
				if(!triggeredByType.containsKey(sensor) || test.get(sensor) > triggeredByType.get(sensor)) {
					checkTest = false;
					break;
				}
			}
			// By now, we checked each part of the test, success!
			if(checkTest)
				return true;
		}
		// All tests failed, bummer...
		return false;		
	}
	
	// TODO:
	public float calculateGoodness(Map<String, Integer> totalByType, Map<String, Integer> triggeredByType) {
	//	return 1;
		// Return 
		/*
		 *             for (int j = 0; j < GlobalVar.Runs[GlobalVar.CurrentRunId].selectedDataTypes.Count(); j++)
            {
                float tempTot = 0;
             //   for (int i = 0; i < GlobalVar.Runs[GlobalVar.CurrentRunId].NumScenarios; i++)
             ///   {
                    tempTot += ((float)data[i, j] / (float)required_sensors);
             //   }
                avgSen[j] = ((float) tempTot;// / (float)GlobalVar.Runs[GlobalVar.CurrentRunId].NumScenarios);
            }

            // Calculate goodness
            foreach (float a in avgSen)
            {
                goodness = goodness + ((float) 1 / (float) GlobalVar.Runs[GlobalVar.CurrentRunId].selectedDataTypes.Count()) * (a);
            }
		 */
		float goodness =  0;
		return goodness;
	}
	
	public void addActiveTest(HashMap<String, Integer> test) {
		activeTests.add(test);
	}
	
	public List<HashMap<String, Integer>> getActiveTests() {
		return activeTests;
	}
	
	// Reset active tests and replace with new list from Page_DetectionCriteria
	public void copyInferenceTest(List<DetectionCriteria> testList) {
		activeTests.clear();
		for(DetectionCriteria test: testList) {
			activeTests.add(test.activeTests);
		}
	}
}
