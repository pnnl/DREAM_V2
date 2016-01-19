package wizardPages;

import java.util.HashMap;
import java.util.Map;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import utilities.Constants;
import wizardPages.DREAMWizard.STORMData;
import wizardPages.Page_SensorSetup.SensorData;

public class Page_InferenceTest extends WizardPage implements AbstractWizardPage {

	STORMData data;
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	
	private Map<String, Text> minimumSensors;

	private boolean isCurrentPage = false;
	private Text minText;
	
	protected Page_InferenceTest(STORMData data) {
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
		container.layout();	
		
		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("Fix the minimum required number of triggered sensors to signify a leak.");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 4;
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
