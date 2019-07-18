package wizardPages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utilities.Constants;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import wizardPages.DREAMWizard.STORMData;

/**
 * Select which scenarios should be considered and give them relative weights. In the end, only relative rankings matter ({1,1,1} == {2,2,2} != {1,2,2})
 * See line 130
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class Page_ScenarioWeighting extends DreamWizardPage implements AbstractWizardPage {
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private STORMData data;
	
	private Map<String, Text> weights;
	private Map<String, Button> selectedScenarios;
	
	private boolean isCurrentPage = false;
	
	protected Page_ScenarioWeighting(final STORMData data) {
		super("Scenario Weighting");
		this.data = data;	
	}
	
	@Override
	public void createControl(final Composite parent) {
		rootContainer = new Composite(parent, SWT.NULL);
		rootContainer.setLayout(GridLayoutFactory.fillDefaults().create());

		sc = new ScrolledComposite(rootContainer, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.getVerticalBar().setIncrement(20);
		sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 200).create());
        sc.setExpandHorizontal(true);
        sc.setExpandVertical(true);
        sc.addListener(SWT.Activate, new Listener() {
	        public void handleEvent(Event e) {
	            sc.setFocus();
	        }
	    });
		sc.addListener(SWT.MouseWheel, new Listener() {
	        public void handleEvent(Event event) {
	            int wheelCount = event.count;
	            wheelCount = (int) Math.ceil(wheelCount / 3.0f);
	            while (wheelCount < 0) {
	                sc.getVerticalBar().setIncrement(4);
	                wheelCount++;
	            }

	            while (wheelCount > 0) {
	                sc.getVerticalBar().setIncrement(-4);
	                wheelCount--;
	            }
	            sc.redraw();
	        }
	    });
		
        container = new Composite(sc, SWT.NULL);
        GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 12;
		layout.verticalSpacing = 12;
		layout.numColumns = 2;
		container.setLayout(layout);
		
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
		
		Font boldFont = new Font( container.getDisplay(), new FontData( "Helvetica", 12, SWT.BOLD ) );

		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Scenario Weighting");
		GridData infoGridData1 = new GridData(GridData.BEGINNING);
		infoGridData1.horizontalSpan = ((GridLayout)container.getLayout()).numColumns - 1;
		infoGridData1.verticalSpan = 2;
		infoLabel1.setLayoutData(infoGridData1);
		
		infoLabel1.setFont(boldFont);
		
		GridData infoLinkData = new GridData(GridData.FILL_HORIZONTAL);
		infoLinkData.horizontalSpan = 1;
		infoLinkData.verticalSpan = 2;
		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.setAlignment(SWT.RIGHT);
		infoLink.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Additional information", "The Scenario Weighting window lists scenarios by the naming convention used in the leakage simulation input files. "
						+ "If the user has prior knowledge of leakage probabilities or would like to choose to weight leakage scenarios unequally, they may do so in the Scenario Weighting window. "
						+ "By default all scenarios are considered equally likely.\nDREAM works to minimize the average time to leakage detection across all scenarios. "
						+ "Weighting scenarios non-uniformly has the effect of altering the average time to detection; therefore, monitoring configurations that solve for scenarios with "
						+ "higher weights will be given priority while DREAM iterates over monitoring configurations. Ultimately, results show both the unweighted and weighted times to detection "
						+ "to provide the user with an understanding of the impact scenario weighting has on each particular problem.");	
			}			
		});
		infoLink.setLayoutData(infoLinkData);
		
		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("Weight the included scenarios based on the probability of occurrence. By default, all scenarios are equally weighted.");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 4;
		infoLabel.setLayoutData(infoGridData);
		
		if(weights == null)
			weights = new HashMap<String, Text>();
		weights.clear();

		if(selectedScenarios == null)
			selectedScenarios = new HashMap<String, Button>();
		selectedScenarios.clear();
		
		Label setLabel = new Label(container, SWT.NULL);
		setLabel.setText("Scenario");
		Label probabilityLabel = new Label(container, SWT.NULL);
		probabilityLabel.setText("Weight");
		
		List<String> scenarios = new ArrayList<String>(data.getSet().getAllScenarios());

		if(scenarios.size() > 1) {
			Label setLabel2 = new Label(container, SWT.NULL);
			setLabel2.setText("Scenario");
			Label probabilityLabel2 = new Label(container, SWT.NULL);
			probabilityLabel2.setText("Weight");
			((GridLayout)container.getLayout()).numColumns = 4;
			infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
			infoGridData1.horizontalSpan = ((GridLayout)container.getLayout()).numColumns - 1;
		}

		if(scenarios.size() > 2) {
			Label setLabel3 = new Label(container, SWT.NULL);
			setLabel3.setText("Scenario");
			Label probabilityLabel3 = new Label(container, SWT.NULL);
			probabilityLabel3.setText("Weight");
			((GridLayout)container.getLayout()).numColumns = 6;
			infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
			infoGridData1.horizontalSpan = ((GridLayout)container.getLayout()).numColumns - 1;
		}
		
		for(String scenario: scenarios) {
			Button checkBox = new Button(container, SWT.CHECK);
			final Text weightText = new Text(container, SWT.BORDER | SWT.SINGLE);
			if(data.getSet().getScenarios().contains(scenario)){
				checkBox.setSelection(true);
				weightText.setEnabled(true);
				weightText.setText(data.getSet().getScenarioWeights().get(scenario).toString());
			} else{
				checkBox.setSelection(false);
				weightText.setEnabled(false);
				weightText.setText("1.0");
			}
			checkBox.setText(scenario);
			weightText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			selectedScenarios.put(scenario, checkBox);
			weights.put(scenario, weightText);
			
			//Add a listener for actions with the button
			checkBox.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					//required to have this... not sure when it is triggered.
				}
				@Override
				public void widgetSelected(SelectionEvent e) {
					boolean isIncluded = ((Button)e.getSource()).getSelection();					
					boolean countError = true;
					boolean numberError = false;
					
					for(String scenario: selectedScenarios.keySet()) {
						if(((Button)e.getSource()).getText().contains(scenario))
							selectedScenarios.put(scenario, (Button)e.getSource());
						if(selectedScenarios.get(scenario).getSelection()) {//If scenario is checked
							countError = false;
							if(Constants.isValidFloat(weights.get(scenario).getText())) {//Valid number
								weights.get(scenario).setForeground(Constants.black);
							} else { //Not a valid number
								weights.get(scenario).setForeground(Constants.red);
								numberError = true;
							}
						} else {
							weightText.setEnabled(false);
						}
					}
					weightText.setEnabled(isIncluded);
					errorFound(countError, "  Select at least one scenario.");
					errorFound(numberError, "  Weight is not a real number.");
				}
			});
			
			//Add a listener for actions with weight values
			weightText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					boolean numberError = false;
					
					for(String scenario: weights.keySet()) {
						if(((Text)e.getSource()).getText().contains(scenario))
							weights.put(scenario, (Text)e.getSource());
						if(selectedScenarios.get(scenario).getSelection()) {//If scenario is checked
							if(Constants.isValidFloat(weights.get(scenario).getText())) {//Valid number
								weights.get(scenario).setForeground(Constants.black);
							} else { //Not a valid number
								weights.get(scenario).setForeground(Constants.red);
								numberError = true;
							}
						}
					}
					errorFound(numberError, "  Weight is not a real number.");
				}
			});
		}
		container.layout();	
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		//Temporary for already converted files.
		try {
			if (!Page_InputDirectory.getPositiveDirection().equals(null)) {
				data.getSet().getNodeStructure().addUnit("positive", Page_InputDirectory.getPositiveDirection());
				System.out.println(data.getSet().getNodeStructure().getUnit("positive"));
			}
		} catch (Exception e) {
			System.out.println("Didn't set Z-Axial direction");
		}
		DREAMWizard.visLauncher.setEnabled(false);
		DREAMWizard.convertDataButton.setEnabled(false);
		
		// If there is only one scenario, skip the weighting page
		if(scenarios.size()==1)
			DREAMWizard.nextButton.notifyListeners(SWT.Selection, new Event());
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
		
		data.needToResetMonitoringParameters = true;
		data.getSet().getScenarioWeights().clear();
		data.getSet().getScenarios().clear();
		
		// Save the weights
		for(String scenario: data.getSet().getAllScenarios()) {
			float weight = Float.valueOf(weights.get(scenario).getText());
			if(selectedScenarios.get(scenario).getSelection() && weight!=0) {//If scenario is checked and weight is not 0
				data.getSet().getScenarioWeights().put(scenario, weight);
				data.getSet().getScenarios().add(scenario);
			}
		}
		
		System.out.println("Number of scenarios = " + data.getSet().getScenarios().size() + " (" + data.getSet().getAllScenarios().size() + " available)");
		
		// Initialize the sensorSettings at the end of this page - resets LeakageCriteria page
		data.getSet().setupSensorSettings();
		data.needToResetMonitoringParameters = true;
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
