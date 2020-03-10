package mapView;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;

import utilities.Constants;
import wizardPages.DREAMWizard.STORMData;

/**
 * This class parses through all the information and outputs it into a .csv
 * file.
 * 
 * @author huan482
 *
 */
public class IncludeLocationResults {

	private List<Float> myEdgeXValues;

	private List<Float> myEdgeYValues;

	private List<Float> myEdgeZValues;

	private List<ExistingWell> myIncludedWells;

	private STORMData myData;

	private List<Integer> myNodeNumbers;

	private Map<String, Float> myParameterToTTD;

	private List<Map<String, Float>> outputForEachWell;

	private String userOutputdir;

	private List<Integer> scenariosThatHaveParamDetected;

	private int numberOfScenarios;

	private Map<Integer, List<Integer>> wellNumberToNodes;

	/**
	 * A lengthy constructor but we need a lot of information from different
	 * classes.
	 * 
	 * @param theEdgeX         - The list of Edge X values.
	 * @param theEdgeY         - The list of Edge Y values.
	 * @param theEdgeZ         - The list of Edge Z values.
	 * @param theIncludedWells - The included wells the user entered in.
	 * @param theData          - The data of the file the user entered.
	 * @param theOutputDir     - The output directory of the csv file.
	 */
	// Offset values are built into the existing well class.
	public IncludeLocationResults(final List<Float> theEdgeX, final List<Float> theEdgeY, final List<Float> theEdgeZ,
			final List<ExistingWell> theIncludedWells, final STORMData theData, final String theOutputDir) {
		myEdgeXValues = new ArrayList<Float>(theEdgeX);
		myEdgeYValues = new ArrayList<Float>(theEdgeY);
		myEdgeZValues = new ArrayList<Float>(theEdgeZ);
		myIncludedWells = new ArrayList<ExistingWell>(theIncludedWells);
		myData = theData;
//		myPreviousZValues = new ArrayList<Integer>();
		myParameterToTTD = new LinkedHashMap<String, Float>();
		outputForEachWell = new ArrayList<Map<String, Float>>();
		scenariosThatHaveParamDetected = new ArrayList<Integer>();
		userOutputdir = theOutputDir;
		wellNumberToNodes = new HashMap<Integer, List<Integer>>();
	}

	/**
	 * Calculates the IJK Points of the existing wells the user has entered, and
	 * then converts these points into node numbers of our set.
	 */
	private void calculateIJKPoints() {
		myNodeNumbers = new ArrayList<Integer>();
		int iIndex = 0;
		int jIndex = 0;
		int kIndex = 0;
		boolean foundAXLocation = false;
		boolean foundAYLocation = false;
		boolean foundAZLocation = false;
		boolean isNegative = false;
		if (myEdgeZValues.get(0) < 0) {
			isNegative = true;
		}
		for (int i = 0; i < myIncludedWells.size(); i++) {
			foundAXLocation = false;
			foundAYLocation = false;
			foundAZLocation = false;
			myNodeNumbers.clear();
			for (int j = 1; j < myEdgeXValues.size(); j++) {
				if (myEdgeXValues.get(j - 1) < myIncludedWells.get(i).getOriginalXLocation()
						&& myEdgeXValues.get(j) > myIncludedWells.get(i).getOriginalXLocation()) {
					iIndex = j;
					foundAXLocation = true;
				}
			}
			if (foundAXLocation) {
				for (int j = 1; j < myEdgeYValues.size(); j++) {
					if (myEdgeYValues.get(j - 1) < myIncludedWells.get(i).getOriginalYLocation()
							&& myEdgeYValues.get(j) > myIncludedWells.get(i).getOriginalYLocation()) {
						jIndex = j;
						foundAYLocation = true;
					}
				}
			}
			// Z location will be a little bit different than the other coordinates.
			// Have to take account for the Z-Axis positive orientation and if it's a negative value
			if (foundAYLocation) {
				for (int j = 1; j < myEdgeZValues.size(); j++) {
					if (myData.getSet().getNodeStructure().getPositive().equals("down")
							&& myEdgeZValues.get(j - 1) < myIncludedWells.get(i).getZ() &&
							myEdgeZValues.get(j) > myIncludedWells.get(i).getZ()) {
						foundAZLocation = true;
						kIndex = j;
					} else {
						//If the first number in the sorted list is negative we go to this branch.
						//Since the numbers were negative the ordering is slightly out of order so have to switch the >, < signs.
						if (isNegative && Math.abs(myEdgeZValues.get(j - 1)) > Math.abs(myIncludedWells.get(i).getZ()) &&
								Math.abs(myEdgeZValues.get(j)) < Math.abs(myIncludedWells.get(i).getZ())) {
							foundAZLocation = true;
							kIndex = j;
						} else {
							//If the output is positive then we just do this branch.
							if (!isNegative && myEdgeZValues.get(j - 1) < myIncludedWells.get(i).getZ() &&
									myEdgeZValues.get(j) > myIncludedWells.get(i).getZ()) {
								foundAZLocation = true;
								kIndex = j;
							}
						}
					}
				}
			}
			if (foundAZLocation) {
				int theNodeNumber = myData.getSet().getNodeStructure().getNodeNumber(iIndex, jIndex, kIndex);
				myNodeNumbers.add(theNodeNumber);
			}
			wellNumberToNodes.put(i, new ArrayList<Integer>(myNodeNumbers));
		}
	}

	// Sorry to anyone who has to maintain this portion of the code after me. :(
	/**
	 * Goes through our detection map and checks if the well the user has entered
	 * (now a node number) is in our solution space. Grabs all the information we
	 * need to print out our output file.
	 * 
	 * 
	 */
	private void getValuesFromDetectionMap() {
		boolean firstValueNode = false;
		float sumTTDForScenario = 0;
		float averageTTDForDetectScenarios = 0;
		int counterForDetectingScenarios = 0;
		// For each well the user has entered.
		for (int z = 0; z < myIncludedWells.size(); z++) {
			myParameterToTTD.clear();
			// For each parameter the user has selected.
			for (String parameter : myData.getSet().getSensorSettings().keySet()) {
				String specificType = myData.getSet().getSensorSettings(parameter).specificType;
				numberOfScenarios = myData.getSet().getDetectionMap().get(specificType).size();
				counterForDetectingScenarios = 0;
				// For each scenario the user has entered.\
				sumTTDForScenario = 0;
				for (String scenario : myData.getSet().getDetectionMap().get(specificType).keySet()) {
					firstValueNode = false;
					for (int i = 0; i < wellNumberToNodes.get(z).size(); i++) {
						if (myData.getSet().getDetectionMap().get(specificType).get(scenario)
								.containsKey(wellNumberToNodes.get(z).get(i))) {
							// Get the TTD
							float ttd = myData.getSet().getDetectionMap().get(specificType).get(scenario)
									.get((wellNumberToNodes.get(z).get(i)));
							// Keep track of the number of detecting scenarios we have
							if (!firstValueNode) {
								firstValueNode = true;
								counterForDetectingScenarios++;
								sumTTDForScenario += ttd;
							} else  {
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
				scenariosThatHaveParamDetected.add(counterForDetectingScenarios);
				myParameterToTTD.put(specificType, averageTTDForDetectScenarios);
			}
			outputForEachWell.add(new LinkedHashMap<String, Float>(myParameterToTTD));
		}
	}

	/**
	 * Loops through all our data gathered and then writes it in a csv file.
	 */
	public void printOutResults() {
		StringBuilder outputText = new StringBuilder();
		calculateIJKPoints();
		getValuesFromDetectionMap();
		outputText.append("Parameter");
		for (String theParameter : myParameterToTTD.keySet()) {
			outputText.append("," + "Average TTD of Detecting Scenarios " + theParameter);
			outputText.append("," + "Percent of Detecting Scenarios " + theParameter);
		}
		int mycounter = 0;
		for (int i = 0; i < outputForEachWell.size(); i++) {
			outputText.append("\nIncluded Well: " + (i + 1));
			for (String theParameter : outputForEachWell.get(i).keySet()) {
				if (outputForEachWell.get(i).get(theParameter) == 0) {
					outputText.append("," + "N/A");
					outputText.append("," + Constants.percentageFormat.format(
							(float) (scenariosThatHaveParamDetected.get(mycounter) / (float) numberOfScenarios) * 100)
							+ "%");
					if (mycounter != scenariosThatHaveParamDetected.size() - 1) {
						mycounter++;
					}
				} else {
					outputText.append(
							"," + Constants.decimalFormatForCost.format(outputForEachWell.get(i).get(theParameter)) + " "
									+ myData.getSet().getNodeStructure().getUnit("times"));
					outputText.append("," + Constants.percentageFormat.format(
							(float) (scenariosThatHaveParamDetected.get(mycounter) / (float) numberOfScenarios) * 100)
							+ "%");
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
