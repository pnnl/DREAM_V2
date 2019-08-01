package mapView;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;

import wizardPages.DREAMWizard.STORMData;

public class IncludeLocationResults {

	private List<Float> myEdgeXValues;

	private List<Float> myEdgeYValues;

	private List<Float> myEdgeZValues;

	private List<ExistingWell> myIncludedWells;

	private STORMData myData;

	private List<Integer> myPreviousZValues;

	private List<Integer> myNodeNumbers;

	private Map<String, Float> myParameterToTTD;

	private List<Map<String, Float>> outputForEachWell;

	private String userOutputdir;

	private List<Integer> scenariosThatHaveParamDetected;

	private int numberOfScenarios;

	// Offset values are built into the existing well class.
	public IncludeLocationResults(final List<Float> theEdgeX, final List<Float> theEdgeY, final List<Float> theEdgeZ,
			final List<ExistingWell> theIncludedWells, final STORMData theData, final String theOutputDir) {
		myEdgeXValues = new ArrayList<Float>(theEdgeX);
		myEdgeYValues = new ArrayList<Float>(theEdgeY);
		myEdgeZValues = new ArrayList<Float>(theEdgeZ);
		myIncludedWells = new ArrayList<ExistingWell>(theIncludedWells);
		myData = theData;
		myPreviousZValues = new ArrayList<Integer>();
		myParameterToTTD = new LinkedHashMap<String, Float>();
		outputForEachWell = new ArrayList<Map<String, Float>>();
		scenariosThatHaveParamDetected = new ArrayList<Integer>();
		userOutputdir = theOutputDir;
	}

	private void calculateIJKPoints() {
		myNodeNumbers = new ArrayList<Integer>();
		int iIndex = 0;
		int jIndex = 0;
		boolean foundAXLocation = false;
		boolean foundAYLocation = false;
		boolean foundAZLocation = false;
		for (int i = 0; i < myIncludedWells.size(); i++) {
			foundAXLocation = false;
			foundAYLocation = false;
			foundAZLocation = false;
			myPreviousZValues.clear();
			for (int j = 1; j < myEdgeXValues.size(); j++) {
				if (myEdgeXValues.get(j - 1) < myIncludedWells.get(i).getOriginalXLocation()
						&& myEdgeXValues.get(j) > myIncludedWells.get(i).getOriginalXLocation()) {
					iIndex = j - 1;
					foundAXLocation = true;
				}
			}
			if (foundAXLocation) {
				for (int j = 1; j < myEdgeYValues.size(); j++) {
					if (myEdgeYValues.get(j - 1) < myIncludedWells.get(i).getOriginalYLocation()
							&& myEdgeYValues.get(j) > myIncludedWells.get(i).getOriginalYLocation()) {
						jIndex = j - 1;
						foundAYLocation = true;
					}
				}
			}
			// Z location will be a little bit different than the other coordinates.
			if (foundAYLocation) {
				for (int j = 0; j < myEdgeZValues.size(); j++) {
					if (myEdgeZValues.get(j) < myIncludedWells.get(i).getZ()) {
						foundAZLocation = true;
						myPreviousZValues.add(j);
					}
				}
			}
			if (foundAZLocation) {
				for (int j = 0; j < myPreviousZValues.size(); j++) {
					int theNodeNumber = myData.getSet().getNodeStructure().getNodeNumber(iIndex, jIndex,
							myPreviousZValues.get(j));
					myNodeNumbers.add(theNodeNumber);
				}
			}
		}
	}

	/**
	 * Sorry to anyone who has to maintain this portion of the code after me. ; - ;
	 * 
	 */
	private void getValuesFromDetectionMap() {
		boolean firstValueNode = false;
		float sumTTDForScenario = 0;
		float averageTTDForDetectScenarios = 0;
		int counterForDetectingScenarios = 0;
		int cc = 0;
		// For each parameter
		for (int z = 0; z < myIncludedWells.size(); z++) {
			myParameterToTTD.clear();
			for (String parameter : myData.getSet().getSensorSettings().keySet()) {
				String specificType = myData.getSet().getSensorSettings(parameter).specificType;
				counterForDetectingScenarios = 0;
				numberOfScenarios = myData.getSet().getDetectionMap().get(specificType).size();
				cc = 0;
				for (String scenario : myData.getSet().getDetectionMap().get(specificType).keySet()) {
					firstValueNode = false;
					sumTTDForScenario = 0;
					for (int i = 0; i < myNodeNumbers.size(); i++) {
						if (myData.getSet().getDetectionMap().get(specificType).get(scenario)
								.containsKey(myNodeNumbers.get(i))) {
							float ttd = myData.getSet().getDetectionMap().get(specificType).get(scenario)
									.get(myNodeNumbers.get(i));
							if (!firstValueNode) {
								firstValueNode = true;
								counterForDetectingScenarios++;
								sumTTDForScenario += ttd;
								cc++;
							} else {
								sumTTDForScenario += ttd;
							}
						}
					}
					if (counterForDetectingScenarios > 0) {
						averageTTDForDetectScenarios = sumTTDForScenario / counterForDetectingScenarios;
					} else {
						averageTTDForDetectScenarios = 0;
					}
				}
				// For that parameter out of all the scenarios and out of all the nodes in each
				// scenario
				// We put the lowest TTD out of all the scenarios nodes into this map.
				// That TTD will be the best TTD for the parameter
				scenariosThatHaveParamDetected.add(cc);
				myParameterToTTD.put(specificType, averageTTDForDetectScenarios);
			}
			outputForEachWell.add(new LinkedHashMap<String, Float>(myParameterToTTD));
		}
	}

	public void printOutResults() {
		StringBuilder outputText = new StringBuilder();
		calculateIJKPoints();
		getValuesFromDetectionMap();
		outputText.append("Parameter");
		for (String theParameter : myParameterToTTD.keySet()) {
			outputText.append("," + "Average TTD of Detecting Scenarios " + theParameter);
			outputText.append("," + "Percent of Detecting Scenarios" + theParameter);
		}
		int mycounter = 0;
		for (int i = 0; i < outputForEachWell.size(); i++) {
			outputText.append("\nIncluded Well: " + (i + 1));
			for (String theParameter : outputForEachWell.get(i).keySet()) {
				if (outputForEachWell.get(i).get(theParameter) == 0) {
					outputText.append("," + "N/A");
					outputText
					.append("," +  ((float) scenariosThatHaveParamDetected.get(mycounter) / (float) numberOfScenarios) * 100 + "%");
					if (mycounter != scenariosThatHaveParamDetected.size() - 1) {
						mycounter++;
					}
				} else {
					outputText.append("," + outputForEachWell.get(i).get(theParameter));
					outputText
							.append("," +  ((float) scenariosThatHaveParamDetected.get(mycounter) / (float) numberOfScenarios) * 100 + "%");
					if (mycounter != scenariosThatHaveParamDetected.size() - 1) {
						mycounter++;
					}
				}
			}
		}
		try {
			File outFolder = new File(userOutputdir);
			if (!outFolder.exists())
				outFolder.mkdirs();
			File csvOutput = new File(new File(userOutputdir), "Included_Wells_TTD.csv");
			if (!csvOutput.exists())
				csvOutput.createNewFile();
			FileUtils.writeStringToFile(csvOutput, outputText.toString());
			Desktop.getDesktop().open(csvOutput);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null,
					"Could not write to Included_Wells_TTD.csv, make sure the file is not currently open");
		}
	}
}
