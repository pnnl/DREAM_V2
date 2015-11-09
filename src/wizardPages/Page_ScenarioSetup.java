package wizardPages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import objects.Scenario;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import wizardPages.DREAMWizard.STORMData;

public class Page_ScenarioSetup extends WizardPage implements AbstractWizardPage {

	Map<Scenario, Text> weights;
	Map<Scenario, Button> selectedScenarios;
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	
	private STORMData data;
	
	private boolean isCurrentPage;
	
	protected Page_ScenarioSetup(STORMData data) {
		super("Scenario Set");
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
		layout.numColumns = 6;
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
		
		List<Scenario> scenarios = new ArrayList<Scenario>(data.getSet().getScenarios());
		Collections.sort(scenarios);
		
		Label setLabel2 = new Label(container, SWT.NULL);
		setLabel2.setText("Realization");
		Label probabilityLabel2 = new Label(container, SWT.NULL);
		probabilityLabel2.setText("Weight");
		
		Label setLabel3 = new Label(container, SWT.NULL);
		setLabel3.setText("Realization");
		Label probabilityLabel3 = new Label(container, SWT.NULL);
		probabilityLabel3.setText("Weight");
		 
		for(Scenario scenario: scenarios) {
			Button button = new Button(container, SWT.CHECK);
			button.setSelection(true);
			button.setText(scenario.getScenario());
			Text text1 = new Text(container, SWT.BORDER | SWT.SINGLE);
			text1.setText(data.getSet().getScenarioWeights().get(scenario).toString()); // Do not format this
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			text1.setLayoutData(gd);
			selectedScenarios.put(scenario, button);
			weights.put(scenario, text1);
		}
		container.layout();	
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
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
