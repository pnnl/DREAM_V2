package wizardPages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import objects.SensorSetting;
import utilities.Constants;
import wizardPages.DREAMWizard.STORMData;

/**
 * Page for setting what sensors need to trigger to signify a detection
 * See line 110
 * @author port091
 * @author rodr144
 * @author whit162
 * @author huan482
 */

public class Page_DetectionCriteria extends DreamWizardPage implements AbstractWizardPage {
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private STORMData data;
	
	private List<DetectionCriteria> testList;
	private HashMap<String, String> textMap; // A map of <Sensor Name, Display Text>
	private HashMap<String, String> textMapReversed; // A map of <Display Text, Sensor Name>
	
	private boolean isCurrentPage = false;
	
	protected Page_DetectionCriteria(STORMData data) {
		super("Detection Criteria");
		this.data = data;	
	}
	
	public class DetectionCriteria {
		
		private Button removeSensorButton;
		private Text minText;
		private Button addSensorButton;
		private Combo addSensorMenu;
		private Button removeTestButton;
		public HashMap<String, Integer> activeTests;
		private HashMap<String, String> activeTestsText;
		
		public DetectionCriteria(HashMap<String, Integer> tests) {
			activeTests = new HashMap<String, Integer>();
			activeTestsText = new HashMap<String, String>();
			for(String sensorName: tests.keySet()) {
				activeTests.put(sensorName, tests.get(sensorName));
				activeTestsText.put(sensorName, Integer.toString(tests.get(sensorName)));
			}
		}
		
		public void buildUI(final int count) {
			Font boldFontSmall = new Font(container.getDisplay(), new FontData("Helvetica", 10, SWT.BOLD));
			
			// Add an "or" between tests
			if(count > 0) {
				new Label(container, SWT.NULL); // Blank filler
				Label orText = new Label(container, SWT.NULL);
				orText.setText("or");
				orText.setLayoutData(new GridData(SWT.NULL, SWT.NULL, false, false, 1, 1));
				new Label(container, SWT.NULL); // Blank filler
			}
			
			// Creates the group that contains all the tests
			Group group = new Group(container, SWT.SHADOW_NONE);
			group.setText("Test " + Integer.valueOf(count+1).toString());
			group.setLayout(new GridLayout(3,false));
			group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			
			// Creates the headers for the group
			new Label(group, SWT.NULL); // Blank filler
			Label sensorHeader = new Label(group, SWT.LEFT);
			Label minimumHeader = new Label(group, SWT.LEFT);
			sensorHeader.setText("Monitoring Technology");
			minimumHeader.setText("Minimum to Signify Leak");
			sensorHeader.setFont(boldFontSmall);
			minimumHeader.setFont(boldFontSmall);
			
			for(final String sensorName: activeTests.keySet()) {
				
				// Button that allows user to remove the sensor
				removeSensorButton = new Button(group, SWT.PUSH);
				removeSensorButton.setText("-");
				removeSensorButton.setToolTipText("Remove this sensor from the test");
				removeSensorButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event arg0) {
						activeTests.remove(sensorName);
						activeTestsText.remove(sensorName);
						checkForErrors(); //Check if an errors exist after changes
						data.getSet().getInferenceTest().copyInferenceTest(testList); //Save to InferenceTest class
						loadPage();
					}
			    });
				
				// Label for the sensor
				Label sensorLabel = new Label(group, SWT.LEFT);
				sensorLabel.setText(textMap.get(sensorName));
				sensorLabel.setLayoutData(new GridData(SWT.NULL, SWT.NULL, false, false, 1, 1));
				
				// Text area for the minimum to be set
				minText = new Text(group, SWT.BORDER | SWT.SINGLE);
				minText.setText(Integer.toString(activeTests.get(sensorName)));
				minText.setForeground(Constants.black);
				minText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
				minText.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(ModifyEvent e) {
						minText = ((Text)e.getSource());
						
						// Save new value into maps and set red text if necessary
						activeTestsText.put(sensorName, minText.getText());
						try { //The try statement checks if it is a real number
							if(Integer.valueOf(minText.getText()) < 0) //Also check that the number is positive
								minText.setForeground(Constants.red);
							else {
								activeTests.put(sensorName, Integer.valueOf(minText.getText()));
								minText.setForeground(Constants.black);
							}
						} catch (Exception ex) {
							minText.setForeground(Constants.red);
						}
						
						checkForErrors(); // Check if an errors exist after changes
						data.getSet().getInferenceTest().copyInferenceTest(testList); //Save to InferenceTest class
					}
				});
			}
			
			if(activeTests.size() != textMap.size()) { 
				
				// Button that allows user to add another sensor to the test
				addSensorButton = new Button(group, SWT.PUSH);
				addSensorButton.setText("+");
				addSensorButton.setToolTipText("Add an additional sensor to the test");
				addSensorButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						if(!addSensorMenu.getText().equals("")) {
							String sensorName = textMapReversed.get(addSensorMenu.getText());
							activeTests.put(sensorName, 1);
							activeTestsText.put(sensorName, Integer.toString(1));
							checkForErrors(); //Check if an error exist after changes
							data.getSet().getInferenceTest().copyInferenceTest(testList); //Save to InferenceTest class
							loadPage();
						}
					}
			    });
				
				// Drop down menu to add sensors that aren't added yet
				addSensorMenu = new Combo(group, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
				addSensorMenu.setToolTipText("Add an additional sensor to the test");
				for(String sensorName: textMap.keySet()) {
					if(!activeTests.containsKey(sensorName))
						addSensorMenu.add(textMap.get(sensorName));
				}
			} else {
				new Label(group, SWT.NULL); // Blank filler
				new Label(group, SWT.NULL); // Blank filler
			}
			
			// If not the first criteria, allow option to remove the criteria altogether
			if(count > 0) {
				removeTestButton = new Button(group, SWT.PUSH);
				removeTestButton.setText(" Remove Test ");
				removeTestButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false, 1, 1));
				removeTestButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						int size = testList.size(); //Necessary to avoid concurrent modifications
						for(int i=1; i<size; i++) { //Starts at 1 to skip first test
							if(testList.get(i).removeTestButton.getEnabled()) {
								testList.remove(i);
								break;
							}
						}
						data.getSet().getInferenceTest().copyInferenceTest(testList); //Save to InferenceTest class
						loadPage();
					}
			    });
			}
		}
	}
	
	public void checkForErrors() {
		boolean minError = false;
		boolean negError = false;
		boolean sumError = false;
		for(DetectionCriteria test: testList) {
			int sum = 0;
			for(String sensorName: test.activeTests.keySet()) {
				sum += test.activeTests.get(sensorName);
				if(!Constants.isValidInt(test.activeTestsText.get(sensorName))) //Not a valid number
					minError = true;
				if(test.activeTests.get(sensorName) < 0)
					negError = true;
			}
			if(sum==0)
				sumError = true;
		}
		errorFound(sumError, "  Must have at least one sensor.");
		errorFound(minError, "  Min is not a real number.");
		errorFound(negError, "  Min cannot be negative.");
	}
	
	@Override
	public void createControl(final Composite parent) {
		
		rootContainer = new Composite(parent, SWT.NULL);
		rootContainer.setLayout(GridLayoutFactory.fillDefaults().create());
		
		sc = new ScrolledComposite(rootContainer, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 200).create());
        sc.setExpandHorizontal(true);
        sc.setExpandVertical(true);
        
        container = new Composite(sc, SWT.NULL);

		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 12;
		layout.verticalSpacing = 12;
		container.setLayout(layout);
		layout.numColumns = 3;
			
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		setControl(rootContainer);
		setPageComplete(true);
	}

	@Override
	public void loadPage() {
		isCurrentPage = true;
		DREAMWizard.errorMessage.setText("");
		removeChildren(container);
		
		Font boldFont = new Font(container.getDisplay(), new FontData("Helvetica", 12, SWT.BOLD));
		
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Detection Criteria");
		infoLabel1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 2));
		infoLabel1.setFont(boldFont);
		
		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.setAlignment(SWT.RIGHT);
		infoLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
		infoLink.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Additional information", "The Detection Criteria window prompts the user to specify how many detections "
						+ "must be triggered to signify a leak has occurred. The user may create multiple tests with any combination of available monitoring technologies. "
						+ "\"Any Technology\" implies that any detection counts towards the test.");	
			}
		});
		
		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("How many detections are required to have confidence in a leak?");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 2;
		infoLabel.setLayoutData(infoGridData);
		
		// Map available sensors to display text with their costs
		textMap = new HashMap<String, String>();
		textMapReversed = new HashMap<String, String>();
		textMap.put("Any Technology", "Any Technology");
		textMapReversed.put("Any Technology", "Any Technology");
		for(String sensorType: data.getSet().getSensorSettings().keySet()) {
			SensorSetting sensorSetting = data.getSet().getSensorSettings(sensorType);
			textMap.put(sensorType, sensorSetting.getAlias()+" (Cost = "+sensorSetting.getSensorCost()+")");
			textMapReversed.put(sensorSetting.getAlias()+" (Cost = "+sensorSetting.getSensorCost()+")", sensorType);
		}
		
		// Copy the Inference Test settings from data.getSet().getInferenceTest() to local
		testList = new ArrayList<DetectionCriteria>();
		for(HashMap<String, Integer> masterActiveTests: data.getSet().getInferenceTest().getActiveTests()) {
			testList.add(new DetectionCriteria(masterActiveTests));
		}
		
		// Builds each test into the user interface
		int count = 0;
		for(DetectionCriteria inferenceTest: testList) {
			inferenceTest.buildUI(count);
			count++;
		}
		
		// Button that allows user to add another test
		Button addTestButton = new Button(container, SWT.PUSH);
		addTestButton.setText(" Add a new test ");
		addTestButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				HashMap<String, Integer> test = new HashMap<String, Integer>();
				test.put("Any Technology", 1);
				testList.add(new DetectionCriteria(test)); //Save to local class
				data.getSet().getInferenceTest().addActiveTest(test); //Save to InferenceTest class
				loadPage();
			}
	    });
		
		container.layout();
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();

		DREAMWizard.visLauncher.setEnabled(true);
		DREAMWizard.convertDataButton.setEnabled(false);
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
		
		// Determine the cheapest sensor for the "Any Technology" option
		float min = Float.MAX_VALUE;
		for(String sensor: data.getSet().getSensorSettings().keySet()) {
			float cost = data.getSet().getSensorSettings(sensor).getSensorCost();
			if(cost<min) min = cost;
		}
		
		// Calculate the minimum sensor budget for given tests
		float minTestCost = Float.MAX_VALUE;
		for(DetectionCriteria test: testList) {
			float testCost = 0;
			for(String sensor: test.activeTests.keySet()) {
				if(sensor.equals("Any Technology"))
					testCost += min * test.activeTests.get(sensor);
				else
					testCost += data.getSet().getSensorSettings(sensor).getSensorCost() * test.activeTests.get(sensor);
			}
			if(testCost<minTestCost) minTestCost = testCost;
		}
		data.getSet().setSensorCostConstraint(minTestCost);
	}
	
	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}
	
	@Override
	public void setPageCurrent(final boolean current) {
		isCurrentPage = current;
	}

}
