package wizardPages;

import java.util.HashMap;
import java.util.Map;

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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import objects.Sensor;
import utilities.Constants.ModelOption;
import wizardPages.DREAMWizard.STORMData;

/**
 * Page for setting what sensors need to trigger to signify a detection
 * See line 110
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class Page_DetectionCriteria extends DreamWizardPage implements AbstractWizardPage {

	STORMData data;
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	
	private Map<String, Text> minimumSensors;

	private boolean isCurrentPage = false;
	private Text minText;
	private int count;
	
	protected Page_DetectionCriteria(STORMData data) {
		super("Detection Criteria");
	//	setDescription("Inference test");
		this.data = data;	
		
	}

	@Override
	public void createControl(Composite parent) {
		
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
		for(Control control: container.getChildren()) {
			control.dispose(); // Remove the children.
		}
		Font boldFont1 = new Font( container.getDisplay(), new FontData( "Helvetica", 12, SWT.BOLD ) );
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Detection Criteria");
		GridData infoGridData1 = new GridData(GridData.BEGINNING);
		infoGridData1.horizontalSpan = ((GridLayout)container.getLayout()).numColumns - 1;
		infoGridData1.verticalSpan = 2;
		infoLabel1.setLayoutData(infoGridData1);
		infoLabel1.setFont(boldFont1);
		
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
				MessageDialog.openInformation(container.getShell(), "Additional information", "The Detection Criteria window prompts the user to specify how many monitoring devices must be triggered to signify a leak has occurred. A leak may be defined in two ways. DREAM reads input provided as an \"or\"-statement, where it will determine a leak has occurred if (1) the specified value for any specific parameter has been met or (2) the overall number of locations exceeding the detection criteria is equal to or greater than the provided value. This allows for various combinations of monitoring device configurations to be tested.");	
			}			
		});
		infoLink.setLayoutData(infoLinkData);
		
		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("Fix the minimum required number of triggered sensors to signify a leak.");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 2;
		infoLabel.setLayoutData(infoGridData);
		
		if(minimumSensors == null)
			minimumSensors = new HashMap<String, Text>();
		minimumSensors.clear();
		
		Font boldFont = new Font( container.getDisplay(), new FontData( "Arial", 10, SWT.BOLD ) );
		
		Label setLabel = new Label(container, SWT.NULL);
		setLabel.setText("Monitoring Parameter");
		setLabel.setLayoutData(new GridData(SWT.NULL, SWT.NULL, false, false, 1, 1));
		new Label(container, SWT.NULL);
		Label probabilityLabel = new Label(container, SWT.NULL);
		probabilityLabel.setText("Minimum Triggered Sensors");
		probabilityLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		
		setLabel.setFont(boldFont);
		probabilityLabel.setFont(boldFont);
		
		if(!(data.modelOption == ModelOption.ALL_SENSORS)){
			for(final String dataType: data.getSet().getSensorList()) {
				
				final Label dataLabel = new Label(container, SWT.NULL);
				dataLabel.setText(Sensor.sensorAliases.get(dataType));
				dataLabel.setLayoutData(new GridData(SWT.NULL, SWT.NULL, false, false, 1, 1));
				new Label(container, SWT.NULL);
				
				Text indText = new Text(container, SWT.BORDER | SWT.SINGLE);
				indText.setText(String.valueOf(data.getSet().getInferenceTest().getMinimumForType(dataType)));
				indText.setForeground(black);
				indText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
				indText.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(ModifyEvent e) {
						if (isValidInt(((Text)e.getSource()).getText()))
							data.getSet().getInferenceTest().setMinimumForType(dataType, Integer.parseInt(((Text)e.getSource()).getText()));
						boolean individualError = false;
						count = 0;
						for(Text individualSensors: minimumSensors.values()) {
							if(isValidInt(individualSensors.getText())) { //Valid number
								individualSensors.setForeground(black);
								count += Integer.parseInt(individualSensors.getText());
							} else {
								individualSensors.setForeground(red);
								individualError = true;
							}
						}
						errorFound(individualError, "  Min is not a real number.");
						if (count > Integer.parseInt(minText.getText()))
							minText.setText(Integer.toString(count));
						else {
							minText.setForeground(black);
							errorFound(false, "  Overall cannot be less than the sum of individual sensors.");
						}
					}
				});
				minimumSensors.put(dataType, indText);
			}
		}
		else{
			for(String dataType: data.getSet().getDataTypes()) {
				minimumSensors.put(dataType, null);
			}
		}
		Label orFiller1 = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		orFiller1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		Label orText = new Label(container, SWT.NULL);
		orText.setText("or");
		orText.setLayoutData(new GridData(SWT.NULL, SWT.NULL, false, false, 1, 1));
		Label orFiller2 = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		orFiller2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
			
		Label dataLabel = new Label(container, SWT.NULL);
		dataLabel.setText("Overall Minimum Required ");
		dataLabel.setLayoutData(new GridData(SWT.NULL, SWT.NULL, false, false, 1, 1));
		new Label(container, SWT.NULL);
		minText = new Text(container, SWT.BORDER | SWT.SINGLE);
		minText.setText(String.valueOf(data.getSet().getInferenceTest().getOverallMinimum()));
		minText.setForeground(black);
		minText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		minText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				boolean numError = !isValidInt(((Text)e.getSource()).getText());
				boolean smallError = false;
				if(numError) { //Not a valid number
					((Text)e.getSource()).setForeground(red);
				} else { //Valid number
					((Text)e.getSource()).setForeground(black);
					data.getSet().getInferenceTest().setOverallMinimum(Integer.parseInt(((Text)e.getSource()).getText()));
					if(Integer.parseInt(((Text)e.getSource()).getText()) < count) {
						smallError = true;
						((Text)e.getSource()).setForeground(red);
					}
				}
				errorFound(numError, "  Overall min is not a real number.");
				errorFound(smallError, "  Overall cannot be less than the sum of individual sensors.");
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
		String firstMinimum = minText.getText();
		int overallMin = firstMinimum.length() > 0 ? Integer.parseInt(firstMinimum) : -1;
		Map<String, Integer> requiredSensors = new HashMap<String, Integer>();
		for(String sensorType: minimumSensors.keySet())  {
			if(!(data.modelOption == ModelOption.ALL_SENSORS)){
				String minimum = minimumSensors.get(sensorType).getText();
				if(minimum.length() > 0)
					requiredSensors.put(sensorType, Integer.parseInt(minimum));
				else
					requiredSensors.put(sensorType, -1);
			}
			else{
				requiredSensors.put(sensorType, overallMin);
			}
		}
		data.setupInferenceTest(requiredSensors, overallMin);
		
		//Need to calculate the minimum cost constraint
		float countMinCost = 0;
		int count = 0;
		float min = Integer.MAX_VALUE;
		for(String cost: data.getSet().getDataTypes()) {
			countMinCost += data.getSet().getInferenceTest().getMinimumForType(cost) * data.getSet().getSensorSettings(cost).getCost();
			count += data.getSet().getInferenceTest().getMinimumForType(cost);
			if(data.getSet().getSensorSettings(cost).getCost() < min)
				min = data.getSet().getSensorSettings(cost).getCost();
		}
		if(overallMin > count)
			countMinCost += min * (overallMin - count);
		data.getSet().setCostConstraint(countMinCost);
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
