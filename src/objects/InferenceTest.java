package objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import utilities.Constants;

public class InferenceTest {

	List<String> dataTypes;	
	Map<String, Integer> minimumRequiredPerType;
	int minimumRequired;
	
	public InferenceTest(Set<String> dataTypes) {
		
		this.dataTypes = new ArrayList<String>();
		
		minimumRequiredPerType = new HashMap<String, Integer>();
		minimumRequired = -1;

		for(String dataType: dataTypes) {
			this.dataTypes.add(dataType);
			minimumRequiredPerType.put(dataType, -1); // Every dataType must have 3 sensors triggering
		}
		
		
		Constants.log(Level.INFO, "Inference test: initialized", null);
		Constants.log(Level.CONFIG, "Inference test: configuration", this);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append("Inference Test\n");
		for(String dataType: minimumRequiredPerType.keySet()) {
			builder.append("\tRequired for " + dataType + ": " + minimumRequiredPerType.get(dataType) + "\n");
		}
		
		return builder.toString();
	}
	 
	/**
	 * if the user wants some custom settings!
	 * @param dataType
	 * @param minimumRequired
	 */
	public void setMinimumRequiredForType(String dataType, int minimumRequired) {
		minimumRequiredPerType.put(dataType, minimumRequired);
		Constants.log(Level.CONFIG, "Inference test: configuration", this);
	}
	
	public void setMinimum(int minimumRequired) {
		this.minimumRequired = minimumRequired;
	}	
	
	// Default inference test 
	public Boolean reachedInference(Map<String, Integer> triggeredByType) {
		// Assume we reached inference
		boolean inferred = true;
		int totalTriggering = 0;
		for(String dataType: minimumRequiredPerType.keySet()) {
			
			int required = minimumRequiredPerType.get(dataType);
			if(required <= 0){
				continue; // None required, continue on
			}
			
			if(!triggeredByType.containsKey(dataType)) {
				continue;
			}
			
			// Turn inference off if we don't have enough of a required type
			if(triggeredByType.get(dataType) < required)
				inferred = false; // Not enough triggering
			
			totalTriggering += triggeredByType.get(dataType);
		}
		
		// Additional check
		
		// Turn inference back on if we still had enough total sensors
		if(minimumRequired > 0 && totalTriggering >= minimumRequired)
			inferred = true;
		
		return inferred;		
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
		for(String dataType: totalByType.keySet()) {
			minimumRequiredPerType.get(dataType);
		}
		return goodness;
	}

	public int getMinimumForType(String dataType) {
		return minimumRequiredPerType.get(dataType);
	}

	public int getOverallMinimum() {
		return this.minimumRequired;
	}
	
	
}
