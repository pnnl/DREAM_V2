package wizardPages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import objects.Scenario;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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
 */

public class Page_ScenarioWeighting extends DreamWizardPage implements AbstractWizardPage {

	Map<Scenario, Text> weights;
	Map<Scenario, Button> selectedScenarios;
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	
	private STORMData data;
	
	private boolean isCurrentPage;
		
	protected Page_ScenarioWeighting(STORMData data) {
		super("Scenario Weighting");
		this.data = data;	
	}

	@Override
	public void createControl(Composite parent) {
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
		for(Control control: container.getChildren()) {
			control.dispose(); // Remove the children.
		}
		
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
				// TODO: Catherine edit text here!
				MessageDialog.openInformation(container.getShell(), "Additional information", "The Scenario Weighting window lists realizations by the naming convention used in the leakage simulation input files. If the user has prior knowledge of leakage probabilities or would like to choose to weight leakage scenarios unequally, they may do so in the Scenario Weighting window. By default all scenarios are considered equally likely.\nDREAM works to minimize the average time to leakage detection across all scenarios. Weighting scenarios non-uniformly has the effect of altering the average time to detection; therefore, monitoring configurations that solve for scenarios with higher weights will be given priority while DREAM iterates over monitoring configurations. Ultimately, results show both the unweighted and weighted times to detection to provide the user with an understanding of the impact scenario weighting has on each particular problem.");	
			}			
		});
		infoLink.setLayoutData(infoLinkData);
		
		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("Weight the included realizations based on the probability of occurrence. By default, all realizations are equally weighted.");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 4;
		infoLabel.setLayoutData(infoGridData);
		
		if(weights == null)
			weights = new HashMap<Scenario, Text>();
		weights.clear();

		if(selectedScenarios == null)
			selectedScenarios = new HashMap<Scenario, Button>();
		selectedScenarios.clear();
		
		Label setLabel = new Label(container, SWT.NULL);
		setLabel.setText("Realization");
		Label probabilityLabel = new Label(container, SWT.NULL);
		probabilityLabel.setText("Weight");
		
		List<Scenario> scenarios = new ArrayList<Scenario>(data.getSet().getAllScenarios());
		Collections.sort(scenarios);

		if(scenarios.size() > 1) {
			Label setLabel2 = new Label(container, SWT.NULL);
			setLabel2.setText("Realization");
			Label probabilityLabel2 = new Label(container, SWT.NULL);
			probabilityLabel2.setText("Weight");
			((GridLayout)container.getLayout()).numColumns = 4;
			infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
			infoGridData1.horizontalSpan = ((GridLayout)container.getLayout()).numColumns - 1;
		}

		if(scenarios.size() > 2) {
			Label setLabel3 = new Label(container, SWT.NULL);
			setLabel3.setText("Realization");
			Label probabilityLabel3 = new Label(container, SWT.NULL);
			probabilityLabel3.setText("Weight");
			((GridLayout)container.getLayout()).numColumns = 6;
			infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
			infoGridData1.horizontalSpan = ((GridLayout)container.getLayout()).numColumns - 1;
		}
		
		for(Scenario scenario: scenarios) {
			Button button = new Button(container, SWT.CHECK);
			final Text text1 = new Text(container, SWT.BORDER | SWT.SINGLE);
			
			if(data.getSet().getScenarios().contains(scenario)){
				button.setSelection(true);
				text1.setText(data.getSet().getScenarioWeights().get(scenario).toString()); // Do not format this
			} else{
				button.setSelection(false);
				text1.setText("1.0");
				if(!button.getSelection())
					text1.setEnabled(false);
			}
			button.setText(scenario.getScenario());
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			text1.setLayoutData(gd);
			selectedScenarios.put(scenario, button);
			weights.put(scenario, text1);
			
			//Add a listener for actions with the button
			button.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					//required to have this... not sure when it is triggered.
				}
				@Override
				public void widgetSelected(SelectionEvent e) {
					boolean isIncluded = ((Button)e.getSource()).getSelection();
					
					//Throws an error message when no scenarios are selected
					int scenarioCount = 0;
					for(Scenario scenario1: selectedScenarios.keySet()) {
						if(selectedScenarios.get(scenario1).getSelection())
							scenarioCount++;
					}
					if(scenarioCount>0)
						errorWithoutRedText(false, "  Select at least one scenario.");
					else
						errorWithoutRedText(true, "  Select at least one scenario.");
					
					//Special handling if weight is not real number and scenario unchecked...
					boolean error = false;
					for(Scenario scenario2: weights.keySet()) {
						if(selectedScenarios.get(scenario2).getSelection()) {
							try {
								Float.parseFloat(weights.get(scenario2).getText());
							} catch (NumberFormatException ne) {
								error = true;
							}
						} else { //Handles a bug where a disabled non-float wan't allowing the page to advance
							text1.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
							text1.setText("1.0");
						}
					}
					if(error==true)
						errorWithoutRedText(true, "  Weight is not a real number.");
					else
						errorWithoutRedText(false, "  Weight is not a real number.");
					
					text1.setEnabled(isIncluded);
				}
			});
			
			//Add a listener for actions with weight values
			text1.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					int countInvalid = 0;
					for(Scenario scenario3: weights.keySet()) {
						if(!selectedScenarios.get(scenario3).getSelection())
							continue;
						boolean valid = isValidNumber(weights.get(scenario3).getText());
						if(valid==false) {
							weights.get(scenario3).setForeground(new Color(Display.getCurrent(), 255, 0, 0));
							countInvalid++;
						}
						else
							weights.get(scenario3).setForeground(new Color(Display.getCurrent(), 0, 0, 0));
					}
					if(countInvalid>0)
						errorWithoutRedText(true, "  Weight is not a real number.");
					else
						errorWithoutRedText(false, "  Weight is not a real number.");
				}
			});
		}
		container.layout();	
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();		

		DREAMWizard.visLauncher.setEnabled(false);
		DREAMWizard.convertDataButton.setEnabled(false);
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;

		Map<Scenario, Float> scenarioWeights = new HashMap<Scenario, Float>();
		List<Scenario> scenariosToRemove = new ArrayList<Scenario>();
		
		// Save the weights
		for(Scenario scenario: weights.keySet()) {
			scenarioWeights.put(scenario, Float.valueOf(weights.get(scenario).getText()));
		}
		
		// Remove any unselected scenarios from the set
		for(Scenario scenario: selectedScenarios.keySet()) {
			if(!selectedScenarios.get(scenario).getSelection()) {
				scenariosToRemove.add(scenario);
				scenarioWeights.remove(scenario);
			}
		}
		
		data.setupScenarios(scenarioWeights, scenariosToRemove);
		
	}
	
	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}
	@Override
	public void setPageCurrent(boolean current) {
		isCurrentPage = current;
	}
}
