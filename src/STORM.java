import java.io.IOException;

import utilities.Constants;
//import org.eclipse.jface.wizard.WizardDialog;
//import org.eclipse.swt.widgets.*;

/**
 * Legacy code, not used
 * @author port091
 */

public class STORM {

	//private final String homeDirectory;

	public STORM() throws SecurityException, IOException {

		//Constants.initializeLogger(homeDirectory, STORM.class.getName());
		//Constants.turnLoggerOn(true);
		
		Controller controller = new Controller();

		// Select the dataset to use
		controller.selectDataset(Constants.RUN_TEST); // Optional, default is Constants.RUN_TEST

		// Remove some scenarios
		controller.removeScenario("scenario_0"); // None are removed by default
		controller.removeScenario("scenario_4");
		controller.removeScenario("scenario_9");

		// General Settings (all have defaults)
		controller.setIterations(100); // Default is 100
		controller.setSensorType("CO2");	// Default is CO2
		controller.setMaxSensors(8);	// Default is 10
		controller.setMaxWells(4);	// Default is 4
		//controller.setThreshold(0.0); // Default is halfway between min and max

		// Time step - Default is 1
		controller.setTimestep(1); // Can use either the real double time or the integer time step

		// Output
		controller.setResultsDir("C:\\Users\\Port091\\Desktop\\Results");
		controller.setResultsTag("run0");
		
		controller.execute();


	}


	public static void main(String[] args) throws Exception {

		new STORM();
		// TODO: UI
		/*
		 * Display display = new Display (); Shell shell = new Shell(display);
		 * shell.open (); while (!shell.isDisposed ()) { if
		 * (!display.readAndDispatch ()) display.sleep (); } display.dispose ();
		 */
	}


/*
 	Older code
  		
  		//	runner.fullEnumeration(new Configuration(), set);
 


		/** Run the storm wizard 
		homeDirectory = "C:\\Users\\port091\\.STORM";
		Constants.initializeLogger(homeDirectory, STORM.class.getName());
		Constants.turnLoggerOn(true);

		final Display display = new Display ();
		final Shell shell = new Shell(display);

		WizardDialog wizardDialog = new WizardDialog(shell, new STORMWizard()); 
		wizardDialog.setTitleAreaColor(new RGB(32,62,72));
		wizardDialog.open();

		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();
		 **/


		// /////////////Configure my run////////////////
		/*
		ScenarioSet set = new ScenarioSet();
		set.loadRunData(Constants.RUN_TEST);
		set.setUserSettings(new Point3i(1, 1, 1), 5, 1000, 400);
		// Remove sensor type
		set.removeSensorSettings("Pressure");
		// Remove a scenario
		set.removeScenario(set.getScenarios().get(0));
		set.removeScenario(set.getScenarios().get(1));
		// Re-set probability of remaining scenario
		// set.setScenarioProbability(set.getScenarios().get(0), 1.0);
		// Set number of triggering sensors required for inference, default = 3
		// of each
		set.getInferenceTest().setMinimumRequiredForType("Pressure", 0);
		set.getInferenceTest().setMinimumRequiredForType("CO2", 1);
		// Access sensor properties
		set.getSensorSettings("CO2").setThreshold(0.14);
		// ///////////////////////////////////////////////////

		Function runner = new CCS9_1();

		runner.setResultsDirectory("C:\\Users\\Port091\\Desktop\\Results");
		int runs = 3;
		int iterations = 3;		
		runner.run(new Configuration(), set, runs);
	//	runner.fullEnumeration(new Configuration(), set, iterations);
		runner.processSummaryResults(null, "ALG");

 *
 */
	
	
 

	/** To generate a sample set of scenario/plot files
	for(int scenario = 0; scenario < 10; scenario++) {

		// Time steps

		for(int time = 0; time < 10; time++) {
			StringBuilder builder = new StringBuilder();

			builder.append("Number of Time Steps =        10\n" +
							"Time = "+time+",s\n\n" +
							"Number of X or R-Direction Nodes =     20\n" +
							"Number of Y or Theta-Direction Nodes =     20\n" +
							"Number of Z-Direction Nodes =      20\n" +
							"X Origin, m =  0.500000000E+00\n" +
							"Y Origin, m =  0.500000000E+00\n" +
							"Z Origin, m =  0.500000000E+00\n");

			builder.append("\nX-Direction Node Positions, m\n");
			// Write the x
			for(int z = 0; z < 20; z++) {
				for(int y = 0; y < 20; y++) {
					String line = "";
					for(int x = 0; x < 20; x++) {
						line += String.valueOf(x+1) + (x < 9 ? "   " : "  ");
					}
					line += "\n";
					builder.append(line);
				}
			}
			builder.append("\nY-Direction Node Positions, m\n");
			// Write the y
			for(int z = 0; z < 20; z++) {
				for(int y = 0; y < 20; y++) {
					String line = "";
					for(int x = 0; x < 20; x++) {
						line += String.valueOf(y+1) + (y < 9 ? "   " : "  ");
					}
					line += "\n";
					builder.append(line);
				}
			}
			builder.append("\nZ-Direction Node Positions, m\n");
			// Write the z
			for(int z = 0; z < 20; z++) {
				for(int y = 0; y < 20; y++) {
					String line = "";
					for(int x = 0; x < 20; x++) {
						line += String.valueOf(z+1) + (z < 9 ? "   " : "  ");
					}
					line += "\n";
					builder.append(line);
				}
			}

			for(String sensor: new String[]{"Gas Saturation", "Gas Pressure, mpa"}) {
				builder.append("\n"+sensor+"\n");
				for(int z = 0; z < 20; z++) {
					for(int y = 0; y < 20; y++) {
						String line = "";
						for(int x = 0; x < 20; x++) {
							String temp = "0   ";

							// If this cell is special put a higher value
							int upper = 9+(z/2);
							int lower = 10-(z/2);

							double concentration = .1*time;
							if(y > lower && y < upper && x > lower && x < upper) {
								temp = String.valueOf(concentration).substring(0,3) + " ";
							}					

							line+= temp;
						}
						line += "\n";
						builder.append(line);
					}
				}

			}

			try {					
				File scenarioDirectory = new File("C:\\Users\\port091\\Desktop\\STORM\\scenario_"+scenario);
				if(!scenarioDirectory.exists())
					scenarioDirectory.mkdir();
				FileUtils.writeStringToFile(new File(scenarioDirectory, "plot." + time), builder.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	 */
}
