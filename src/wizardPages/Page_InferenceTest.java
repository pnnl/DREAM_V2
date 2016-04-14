package wizardPages;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
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

import utilities.Constants;
import objects.DREAMData;
import wizardPages.Page_SensorSetup.SensorData;

public class Page_InferenceTest extends WizardPage implements AbstractWizardPage {

	DREAMData data;
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	
	private Map<String, Text> minimumSensors;

	private boolean isCurrentPage = false;
	private Text minText;
	
	protected Page_InferenceTest(DREAMData data) {
		super("Inference Test");
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
		layout.numColumns = 2;
			
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
		infoLink.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				// TODO: Catherine edit text here!
				MessageDialog.openInformation(container.getShell(), "Additional information", "The Detection Criteria window prompts the user to specify how many monitoring devices must be triggered to signify a leak has occurred. A leak may be defined in two ways. DREAM reads input provided as an “or”-statement, where it will determine a leak has occurred if (1) the specified value for any specific parameter has been met or (2) the overall number of locations exceeding the detection criteria is equal to or greater than the provided value. This allows for various combinations of monitoring device configurations to be tested.");	
			}			
		});
		infoLink.setLayoutData(infoLinkData);
		
		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("Fix the minimum required number of triggered sensors to signify a leak.");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 2;
		infoLabel.setLayoutData(infoGridData);
		// TODO
		
		if(minimumSensors == null)
			minimumSensors = new HashMap<String, Text>();
		minimumSensors.clear();
		
		Font boldFont = new Font( container.getDisplay(), new FontData( "Arial", 10, SWT.BOLD ) );
		
		Label setLabel = new Label(container, SWT.NULL);
		setLabel.setText("Monitoring Parameter");
		Label probabilityLabel = new Label(container, SWT.NULL);
		probabilityLabel.setText("Minimum Triggered Sensors");
		
		setLabel.setFont(boldFont);
		probabilityLabel.setFont(boldFont);
		
		for(String dataType: data.getSet().getDataTypes()) {
			
			final Label dataLabel = new Label(container, SWT.NULL);
			dataLabel.setText(dataType);			
			
			Text minText = new Text(container, SWT.BORDER | SWT.SINGLE);
			if(data.getSet().getInferenceTest().getMinimumForType(dataType) > 0)
				minText.setText(Constants.decimalFormat.format(data.getSet().getInferenceTest().getMinimumForType(dataType)));
			if(minText.getText().isEmpty() ||minText.getText().trim().equals("0")) {
				(dataLabel).setForeground(new Color(container.getDisplay(), 255, 0, 0));						
			} else {
				(dataLabel).setForeground(new Color(container.getDisplay(), 0, 0, 0));						
			}
			minText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					try {
						Float.parseFloat(((Text)e.getSource()).getText());	
						((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 0, 0, 0));
						if(!dataLabel.isDisposed()) (dataLabel).setForeground(new Color(container.getDisplay(), 0, 0, 0));
						testReady();
					} catch (NumberFormatException ne) {
						((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
						if(!dataLabel.isDisposed()) (dataLabel).setForeground(new Color(container.getDisplay(), 255, 0, 0));
						testReady();
					}
					if(((Text)e.getSource()).getText().isEmpty() || ((Text)e.getSource()).getText().trim().equals("0")) {
						if(!dataLabel.isDisposed()) (dataLabel).setForeground(new Color(container.getDisplay(), 255, 0, 0));						
					} else {
						if(!dataLabel.isDisposed()) (dataLabel).setForeground(new Color(container.getDisplay(), 0, 0, 0));						
					}
					
				}				
			});
			GridData gdc = new GridData(GridData.FILL_HORIZONTAL);
			minText.setLayoutData(gdc);
			minimumSensors.put(dataType, minText);
			
		}		
		
		Label dataLabel = new Label(container, SWT.NULL);
		dataLabel.setText("Overall Minimum Required ");			
		minText = new Text(container, SWT.BORDER | SWT.SINGLE);
		if(data.getSet().getInferenceTest().getOverallMinimum() >= 0)
			minText.setText(Constants.decimalFormat.format(data.getSet().getInferenceTest().getOverallMinimum()));	
		GridData gdc = new GridData(GridData.FILL_HORIZONTAL);
		minText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					Float.parseFloat(((Text)e.getSource()).getText());	
					((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 0, 0, 0));
					testReady();
				} catch (NumberFormatException ne) {
					((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
					testReady();
				}
			}				
		});
		minText.setLayoutData(gdc);
		
		container.layout();	
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		

		DREAMWizard.visLauncher.setEnabled(true);
		DREAMWizard.convertDataButton.setEnabled(false);
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
		Map<String, Integer> requiredSensors = new HashMap<String, Integer>();
		for(String sensorType: minimumSensors.keySet())  {
			String minimum = minimumSensors.get(sensorType).getText();
			if(minimum.length() > 0)
				requiredSensors.put(sensorType, Integer.parseInt(minimum));
			else
				requiredSensors.put(sensorType, -1);
		}
		String minimum = minText.getText();
		int overallMin = minimum.length() > 0 ? Integer.parseInt(minimum) : -1;
		data.setupInferenceTest(requiredSensors, overallMin);
	}
	
	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}
	
	@Override
	public void setPageCurrent(boolean current) {
		isCurrentPage = current;
	}

	private void testReady() {
		boolean isReady = true;

		for(Text data: minimumSensors.values()) {


			if(data.getForeground().equals(new Color(container.getDisplay(), 255, 0, 0))) {
				isReady = false;
				break;
			}

		}	

		if(minText.getForeground().equals(new Color(container.getDisplay(), 255, 0, 0))) {
			isReady = false;
		}
		
		if(this.isPageComplete() != isReady)
			this.setPageComplete(isReady);
	}
}
