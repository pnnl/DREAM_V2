package wizardPages;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import objects.ExtendedConfiguration;
import objects.ExtendedSensor;
import objects.InferenceTest;
import objects.Scenario;
import objects.Sensor;
import objects.SensorSetting;
import objects.TimeStep;
import objects.SensorSetting.DeltaType;
import objects.SensorSetting.Trigger;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import functions.*;
import utilities.Constants;
import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;

public class Page_MonitoringParameters extends WizardPage implements AbstractWizardPage {
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	protected Map<String, Integer> num_duplicates = new HashMap<String, Integer>();
	
	private STORMData data;
	private boolean isCurrentPage = false;
	private Button scenarioUnionButton;
	private Button sensorUnionButton;
	private boolean toggling = false;
	
	private Map<String, SensorData> sensorData;
		
	protected Page_MonitoringParameters(STORMData data) {
		super("Sensors");
		this.data = data;	
	}
	
	public class SensorData {

		public String sensorType;
		public String sensorName;
		public String alias;
		public boolean isIncluded;
		public float cost;
		public float maxZ;
		public float minZ;
		public Trigger trigger;
		public DeltaType deltaType;
		
		private float dataMin;
		private float dataMax;
		
		public float min;
		public float max;

		public boolean asRelativeChange;
		public boolean asAbsoluteChange;
		
		private boolean isDuplicate;
		
		// A couple of these need to be global
		private Label sensorTypeLabel;
		private Text aliasText;
		private Text costText;
		private Combo thresholdCombo;
		private Label valueLabel;
		private Text valueInput;
		private Label maxZLabel;
		private Text maxZText;
		private Label minZLabel;
		private Text minZText;
		
		private Label nodeLabel;
		private boolean hasErrors;	
		
		public SensorData(SensorSetting sensorSettings, String sensorName) {
			
			if(sensorSettings.getType().equals(sensorName)) isDuplicate = false;
			else isDuplicate = true;
			
			sensorType = sensorSettings.getType();
			this.sensorName = sensorName;
			alias = sensorName;
			isIncluded = false; // By default	
			cost = sensorSettings.getCost();
			maxZ = sensorSettings.getMaxZ();
			minZ = sensorSettings.getMinZ();
			
			
			// These may be backwards?
			trigger = Trigger.MINIMUM_THRESHOLD;
			deltaType = sensorSettings.getDeltaType();
			
			// Try to be a little smarter about pressure
			if(sensorType.toLowerCase().contains("pressure") || sensorType.toLowerCase().equals("p"))
				trigger = Trigger.RELATIVE_DELTA;
			
			// Removing this, its buggy...
			if(sensorType.toLowerCase().equals("ph"))
				trigger = Trigger.MAXIMUM_THRESHOLD;
			
			dataMin = sensorSettings.getMin();
			dataMax = sensorSettings.getMax();
			
			// Default threshold goes from half way to max
			if(trigger == Trigger.MAXIMUM_THRESHOLD) {
				min = 0;
				max = (dataMax-dataMin)/2+dataMin;
			} else {
				min = (dataMax-dataMin)/2+dataMin;
				max = Float.MAX_VALUE;
			}
			
			asRelativeChange = true;
			asAbsoluteChange = false;
			
		}	
		
		public void buildUI() {
			
			//Add a button here
			if(isDuplicate){
				Button addButton = new Button(container, SWT.PUSH);
			    addButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event arg0) {
						sensorData.remove(sensorName);
						data.getSet().getSensorSettings().remove(sensorName);
						loadPage();
					}
			    });
			    addButton.setText("-");
			}
			else{
				Button addButton = new Button(container, SWT.PUSH);
			    addButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event arg0) {
						if(!num_duplicates.containsKey(sensorType)) num_duplicates.put(sensorType, 1);
						addSensor(sensorType, sensorType + "_" + num_duplicates.get(sensorType));
						num_duplicates.put(sensorType, num_duplicates.get(sensorType)+1);
						loadPage();
					}
			    });
			    addButton.setText("+");
			}
			
		    
			// Include button
			Button includeButton = new Button(container,  SWT.CHECK);
			includeButton.setSelection(isIncluded);
			includeButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) { 
					isIncluded = ((Button)e.getSource()).getSelection(); 					
					toggleEnabled();
				}
				@Override
				public void widgetSelected(SelectionEvent e) { 
					isIncluded = ((Button)e.getSource()).getSelection(); 
					toggleEnabled();					
				}
			});
			
			// Label [min, max]
			String minStr =  Constants.decimalFormat.format(dataMin);		
			if(dataMin < 0.001)
				minStr =  Constants.exponentialFormat.format(dataMin);
			String maxStr =  Constants.decimalFormat.format(dataMax);
			if(dataMax < 0.001)
				maxStr =  Constants.exponentialFormat.format(dataMax);
		
			if(minStr.length() > 8) minStr = Constants.exponentialFormat.format(dataMin);
			if(maxStr.length() > 8) maxStr = Constants.exponentialFormat.format(dataMax);			
			
			includeButton.setText(sensorType);
			
			aliasText = new Text(container, SWT.BORDER | SWT.SINGLE);
			aliasText.setText(alias);
			aliasText.addModifyListener(new ModifyListener(){
				@Override
				public void modifyText(ModifyEvent e){
					alias = ((Text)e.getSource()).getText();
					if(alias.contains(",")) ((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
					else ((Text)e.getSource()).setForeground(new Color(container.getDisplay(),0,0,0));
				}
			});
			GridData aliasTextData = new GridData(SWT.FILL, SWT.END, false, false);
			aliasTextData.widthHint = 60;
			aliasText.setLayoutData(aliasTextData);
			
			
			// Cost
			costText = new Text(container, SWT.BORDER | SWT.SINGLE);
			costText.setText(Constants.decimalFormat.format(cost));
			costText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					try {
						cost = Float.parseFloat(((Text)e.getSource()).getText());	
						((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 0, 0, 0));
						testReady();
					} catch (NumberFormatException ne) {
						((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
						testReady();
					}
				}				
			});
			GridData costTextData = new GridData(SWT.FILL, SWT.END, false, false);
			costTextData.widthHint = 60;
			costText.setLayoutData(costTextData);
			
			// Drop down menu
			thresholdCombo = new Combo(container, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
			thresholdCombo.add(Trigger.MAXIMUM_THRESHOLD.toString());
			thresholdCombo.add(Trigger.MINIMUM_THRESHOLD.toString());
			thresholdCombo.add(Trigger.RELATIVE_DELTA.toString());
			thresholdCombo.add(Trigger.ABSOLUTE_DELTA.toString());
			thresholdCombo.setText(trigger.toString());
			thresholdCombo.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					boolean isMinimumThreshold = ((Combo)e.getSource()).getText().equals(Trigger.MAXIMUM_THRESHOLD.toString());
					boolean isMaximumThreshold = ((Combo)e.getSource()).getText().equals(Trigger.MINIMUM_THRESHOLD.toString());
					boolean isRelativeDetla = ((Combo)e.getSource()).getText().equals(Trigger.RELATIVE_DELTA.toString());
					trigger = isMinimumThreshold ? Trigger.MAXIMUM_THRESHOLD : (isMaximumThreshold ? 
							Trigger.MINIMUM_THRESHOLD : isRelativeDetla ? Trigger.RELATIVE_DELTA : Trigger.ABSOLUTE_DELTA);	
					try {
						if(trigger == Trigger.MAXIMUM_THRESHOLD) {
							min = 0;
							max = Float.parseFloat(valueInput.getText());
						} else {	
							min = Float.parseFloat(valueInput.getText());
							max = Float.MAX_VALUE;
						}
						
						if(valueInput.getText().contains("+")) deltaType = DeltaType.INCREASE;
						else if(valueInput.getText().contains("-")) deltaType = DeltaType.DECREASE;
						else deltaType = DeltaType.BOTH;
						
						if(valueInput != null)
							valueInput.setForeground(new Color(container.getDisplay(), 0, 0, 0));						
						testReady();
					} catch (NumberFormatException ex) {
						if(valueInput != null)
							valueInput.setForeground(new Color(container.getDisplay(), 255, 0, 0));
						testReady();
					}
					toggleEnabled();
				}				
			});
			GridData thresholdComboData = new GridData(SWT.FILL, SWT.END, false, false);
			thresholdComboData.widthHint = 140;
			thresholdCombo.setLayoutData(thresholdComboData);
			
			// Specifics fields
			valueInput = new Text(container, SWT.BORDER | SWT.SINGLE);
			if(trigger == Trigger.MAXIMUM_THRESHOLD) {
				if(max != 0) {// What if a user wants to set this to 0? 
					valueInput.setText(Constants.decimalFormat.format(max));
					if(max < 0.001)
						valueInput.setText(Constants.exponentialFormat.format(max));
				}
			} else { 
				if(min != 0) {
					valueInput.setText(Constants.decimalFormat.format(min));
					if(min < 0.001)
						valueInput.setText(Constants.exponentialFormat.format(min));
				}
			}
			
			if(deltaType == DeltaType.INCREASE) valueInput.setText("+" + valueInput.getText());
			valueInput.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					try {						
						
						if(trigger == Trigger.MAXIMUM_THRESHOLD) {
							min = 0;
							max = Float.parseFloat(valueInput.getText());
						} else {	
							min = Float.parseFloat(valueInput.getText());
							max = Float.MAX_VALUE;
						}
						
						if(valueInput.getText().contains("+")) deltaType = DeltaType.INCREASE;
						else if(valueInput.getText().contains("-")) deltaType = DeltaType.DECREASE;
						else deltaType = DeltaType.BOTH;
						
						((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 0, 0, 0));
						testReady();
					} catch (NumberFormatException ne) {
						((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
						testReady();
					}
				}				
			});
			GridData minInputData = new GridData(SWT.FILL, SWT.END, false, false);
			minInputData.widthHint = 60;
			valueInput.setLayoutData(minInputData);

			
			
			// Set minimum z
			minZText = new Text(container, SWT.BORDER | SWT.SINGLE);
			minZText.setText(Constants.decimalFormat.format(minZ));
			minZText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					try {
						minZ = Float.parseFloat(((Text)e.getSource()).getText());	
						((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 0, 0, 0));
						testReady();
					} catch (NumberFormatException ne) {
						((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
						testReady();
					}
				}				
			});
			GridData minZTextData = new GridData(SWT.FILL, SWT.END, false, false);
			minZTextData.widthHint = 60;
			minZText.setLayoutData(minZTextData);
			
			
			
			// Set maximum z
			maxZText = new Text(container, SWT.BORDER | SWT.SINGLE);
			maxZText.setText(Constants.decimalFormat.format(maxZ));
			maxZText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					try {
						maxZ = Float.parseFloat(((Text)e.getSource()).getText());	
						((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 0, 0, 0));
						testReady();
					} catch (NumberFormatException ne) {
						((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
						testReady();
					}
				}				
			});
			GridData maxZTextData = new GridData(SWT.FILL, SWT.END, false, false);
			maxZTextData.widthHint = 60;
			maxZText.setLayoutData(maxZTextData);
			
			toggleEnabled();
		}		
		
		private void toggleEnabled() {
			if(sensorTypeLabel != null && !sensorTypeLabel.isDisposed())
				sensorTypeLabel.setEnabled(isIncluded);
			if(aliasText != null && !aliasText.isDisposed())
				aliasText.setEnabled(isIncluded);
			if(costText != null && !costText.isDisposed())
				costText.setEnabled(isIncluded);
			if(thresholdCombo != null && !thresholdCombo.isDisposed())
				thresholdCombo.setEnabled(isIncluded);					
			if(valueInput != null && !valueInput.isDisposed())
				valueInput.setEnabled(isIncluded);
			if(valueLabel != null && !valueLabel.isDisposed())
				valueLabel.setEnabled(isIncluded);
			if(minZLabel != null && !minZLabel.isDisposed())
				minZLabel.setEnabled(isIncluded);
			if(minZText != null && !minZText.isDisposed())
				minZText.setEnabled(isIncluded);
			if(maxZLabel != null && !maxZLabel.isDisposed())
				maxZLabel.setEnabled(isIncluded);
			if(maxZText != null && !maxZText.isDisposed())
				maxZText.setEnabled(isIncluded);
		}		
	}

	@Override
	public void createControl(Composite parent) {
		rootContainer = new Composite(parent, SWT.NULL);
		rootContainer.setLayout(GridLayoutFactory.fillDefaults().create());

		sc = new ScrolledComposite(rootContainer, SWT.V_SCROLL | SWT.H_SCROLL);
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
		GridData compositeData = new GridData(GridData.FILL, GridData.FILL, true, true);
		compositeData.heightHint = 400;
		compositeData.minimumHeight = 400;
		sc.setLayoutData(compositeData);
	//	sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(720, 400).create());
        sc.setExpandHorizontal(true);
        sc.getVerticalBar().setIncrement(20);
        sc.setExpandVertical(true);
                
        container = new Composite(sc, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 12;
		layout.verticalSpacing = 12;
		container.setLayout(layout);
		layout.numColumns = 8;
//		layout.makeColumnsEqualWidth = false;
		
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		setControl(rootContainer);
		setPageComplete(true);
	}
	
	@Override
	public void completePage() throws Exception {
		try{
			isCurrentPage = false;		
			Map<String, SensorData> sensorSettings = new HashMap<String, SensorData>();
			Map<String, String> sensorAliases = new HashMap<String, String>();
			SensorSetting.sensorTypeToDataType = new HashMap<String, String>();
			for(String label: sensorData.keySet()) {
				SensorData data = sensorData.get(label);
				String alias = data.alias;
				sensorSettings.put(label, data);
				sensorAliases.put(label, alias.equals("") ? label: alias);
				SensorSetting.sensorTypeToDataType.put(label, sensorData.get(label).sensorType);
			}
			DREAMWizard.visLauncher.setEnabled(false);			
			Sensor.sensorAliases = sensorAliases;
			data.setupSensors(false, sensorSettings);
			data.needToResetWells = true;
			volumeOfAquiferDegraded();
			DREAMWizard.visLauncher.setEnabled(true);
			

		}
		catch(Exception e){
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	public void loadPage() {
		isCurrentPage = true;
				
		if(sensorData == null || data.needToResetMonitoringParameters) {
			data.needToResetMonitoringParameters = false;
			// New UI
			sensorData = new TreeMap<String, SensorData>();
		
			for(String dataType: data.getSet().getAllPossibleDataTypes()) {
								
				sensorData.put(dataType, new SensorData(data.getSet().getSensorSettings(dataType), dataType));
				//This is what we need to do to add
				//data.getSet().addSensorSetting(dataType+"1", dataType);
				//data.getSet().getInferenceTest().setMinimumRequiredForType(dataType+"1", -1);
				//sensorData.put(dataType+"1", new SensorData(data.getSet().getSensorSettings(dataType+"1"), dataType+"1"));
				//End here
			}
		}
		
		for(Control control: container.getChildren()) {
			control.dispose(); // Remove the children.
		}		
	
		Font boldFont = new Font( container.getDisplay(), new FontData( "Helvetica", 12, SWT.BOLD ) );

		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Leakage Criteria");
		GridData infoGridData1 = new GridData(GridData.FILL_HORIZONTAL);
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
				MessageDialog.openInformation(container.getShell(), "Additional information", "After reading through the directory of realization outputs, DREAM will generate a table of monitoring parameters that the user can select. These parameters are specific to the included realizations. The selected monitoring parameters will be used in the optimization algorithm. The user may label what technology they will use to monitor each selected parameter in the �Alias for Monitoring Technology� box and then provide a realistic cost per monitoring technology if it is known; if not, the costs should be set equal. The detection criteria may be specified based on the relative change from initial conditions, absolute change from initial conditions, or a maximum or minimum threshold. If relative delta, absolute delta, or maximum threshold is selected, the given value and all values above are treated as detecting a leak. If minimum threshold is selected, that value and all values below are treated as detecting a leak.");	
			}			
		});
		infoLink.setLayoutData(infoLinkData);
		
		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("Select the monitoring parameters of interest, include a cost per appropriate sensor type, and set the detection criteria. NOTE: The minimum and maximum values are read from the first realization read by DREAM. These are provided to give the user an idea of the values present.");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 2;
		infoGridData.widthHint = 200;
		infoLabel.setLayoutData(infoGridData);
				
		// Headers
		
		Font boldFont1 = new Font( container.getDisplay(), new FontData( "Helvetica", 10, SWT.BOLD ) );
		
		Label blankFiller = new Label(container, SWT.LEFT);	
		Label monitorParams = new Label(container, SWT.LEFT);
		Label aliasLabel = new Label(container, SWT.LEFT);
		Label costPerSensor = new Label(container, SWT.LEFT);
		Label detectionCriteria = new Label(container, SWT.LEFT);
		Label valueLabel = new Label(container, SWT.LEFT);
		Label minZLabel = new Label(container, SWT.LEFT);
		Label maxZLabel = new Label(container, SWT.LEFT);
		
		blankFiller.setText("");
		monitorParams.setText("Monitoring Parameter");
		aliasLabel.setText("Alias for Monitoring Technology");
		costPerSensor.setText("Cost per Sensor");
		detectionCriteria.setText("Detection Criteria");
		valueLabel.setText("Value");
		minZLabel.setText("Minimum Z");
		maxZLabel.setText("Maximum Z");
			
		monitorParams.setFont(boldFont1);
		aliasLabel.setFont(boldFont1);
		costPerSensor.setFont(boldFont1);
		detectionCriteria.setFont(boldFont1);
		valueLabel.setFont(boldFont1);
		minZLabel.setFont(boldFont1);
		maxZLabel.setFont(boldFont1);
				
		for(SensorData data: sensorData.values()) {
			data.buildUI();
		}

		Composite composite_scale = new Composite(container, SWT.BORDER);
		GridLayout gridLayout_scale = new GridLayout();
		gridLayout_scale.numColumns = 8;
		composite_scale.setLayout(gridLayout_scale);
		
		GridData gridData = new GridData();
		gridData.horizontalSpan=8;
		composite_scale.setLayoutData(gridData);
		
		Button queryButton = new Button(composite_scale, SWT.BALLOON);
		queryButton.setText("Find triggering nodes");

		new Label(composite_scale, SWT.NULL);
		new Label(composite_scale, SWT.NULL);
		new Label(composite_scale, SWT.NULL);
		new Label(composite_scale, SWT.NULL);
		new Label(composite_scale, SWT.NULL);
		new Label(composite_scale, SWT.NULL);
		new Label(composite_scale, SWT.NULL);
		
		int count = 0;
		
		for(String label: sensorData.keySet()){
			SensorData temp = sensorData.get(label);

			// Let these fill two spots
			GridData tempData = new GridData(GridData.FILL_HORIZONTAL);
			tempData.horizontalSpan = 2;
			temp.nodeLabel = new Label(composite_scale, SWT.WRAP);
			temp.nodeLabel.setLayoutData(tempData);
			if(data.getSet().getSensorSettings(label) == null)
				data.getSet().resetSensorSettings(label, temp.min, temp.max);
			if( data.getSet().getSensorSettings(label).isSet())
				temp.nodeLabel.setText(label+ ": " + data.getSet().getSensorSettings(label).getValidNodes(null).size());
			else 
				temp.nodeLabel.setText(label+ ": Not set");
			count+=2;
		}
		
		if(count % 8 != 0) {
			for(int i = 0; i < 8-(count % 8); i++)
				new Label(composite_scale, SWT.NULL);
		}
		
		queryButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				Constants.hdf5CloudData.clear();
				Constants.scenarioUnion = scenarioUnionButton.getSelection();
				boolean reset = true;							
				Map<String, SensorData> sensorSettings = new HashMap<String, SensorData>();	
				
				SensorSetting.sensorTypeToDataType = new HashMap<String, String>();
				Map<String, String> sensorAliases = new HashMap<String, String>();
				for(String label: sensorData.keySet()){
					SensorData temp = sensorData.get(label);
					sensorSettings.put(label, temp);
					String alias = temp.alias;
					sensorAliases.put(label, alias.equals("") ? label: alias);
					SensorSetting.sensorTypeToDataType.put(label, sensorData.get(label).sensorType);
				}	
				Sensor.sensorAliases = sensorAliases;

				
				try {
					if(reset) {
						for(String dataType: data.getSet().getDataTypes()) {
							data.getSet().getSensorSettings(dataType).setNodesReady(false);
						}
					}
					DREAMWizard.visLauncher.setEnabled(false);	
					data.setupSensors(reset, sensorSettings);
					//volumeOfAquiferDegraded(); //Don't need this because you have to run it when you hit "next", and this way the overhead time does not apply to finding a solution space that you like.
					DREAMWizard.visLauncher.setEnabled(true);
					for(String label: sensorData.keySet()){
						SensorData temp = sensorData.get(label);
						if(temp.isIncluded &&  data.getSet().getSensorSettings(label).isSet())
							temp.nodeLabel.setText(label + ": " + data.getSet().getSensorSettings(label).getValidNodes(null).size());
						else
							temp.nodeLabel.setText(label + ": Not set");
					}			
					
					// Then we really only want the intersection
					if(!sensorUnionButton.getSelection()) {
						List<Integer> intersection = new ArrayList<Integer>();
						for(String dataType: data.getSet().getDataTypes()) {
							if(!data.getSet().getSensorSettings(dataType).isSet())
								continue;
							for(Integer nodeNumber: data.getSet().getSensorSettings(dataType).getValidNodes(null)) {
								boolean shared = true;
								for(String otherDataType: data.getSet().getDataTypes()) {
									if(dataType.equals(otherDataType))
										continue; // Don't need to check this one
									if(!data.getSet().getSensorSettings(otherDataType).isSet())
										continue;
									if(!data.getSet().getSensorSettings(otherDataType).getValidNodes(null).contains(nodeNumber)) {
										shared = false;
									}
									if(!shared)
										break;
								}		
								if(shared)
									intersection.add(nodeNumber);
							}
						}
						System.out.println("Intersection: (" + intersection.size() + ") " + intersection);
						for(String label: sensorData.keySet()){
							SensorData temp = sensorData.get(label);
							if(data.getSet().getSensorSettings(label) != null && data.getSet().getSensorSettings(label).isSet()) {
								data.getSet().getSensorSettings(label).setValidNodes(intersection);
								temp.nodeLabel.setText(temp.alias + ": " + data.getSet().getSensorSettings(temp.alias).getValidNodes(null).size());
							} else
								temp.nodeLabel.setText(temp.alias + ": Not set");
						}
					}
					/*
					//TODO: remove this hard-coded z check
					for(String scenario: data.getScenarioSet().getAllPossibleDataTypes()){
						if(data.getSet().getSensorSettings(scenario) == null) continue;
						float minZ = sensorSettings.get(scenario).minZ;
						float maxZ = sensorSettings.get(scenario).maxZ;
						//Set these just in case we need them later
						data.getSet().getSensorSettings(scenario).setMaxZ(maxZ);
						data.getSet().getSensorSettings(scenario).setMinZ(minZ);
						//Find the nodes that fit this z restriction
						HashSet<Integer> temp = new HashSet<Integer>();
						for(Integer node: data.getSet().getSensorSettings(scenario).getValidNodes()){
							Point3d test = data.getScenarioSet().getNodeStructure().getXYZFromIJK(data.getScenarioSet().getNodeStructure().getIJKFromNodeNumber(node));
							if (minZ <= test.getZ() && test.getZ() <= maxZ)
								temp.add(node);
						}
						data.getSet().getSensorSettings(scenario).setValidNodes(temp);
					}
					*/

				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}	       
		});	
				

		//new Label(composite_scale, SWT.NULL);

		Label col = new Label(composite_scale, SWT.NULL);
		col.setText("Set up the solution space using ...");
		col.setFont(boldFont1);
		new Label(composite_scale, SWT.NULL);
		
	    scenarioUnionButton = new Button(composite_scale, SWT.CHECK);
	    final Button scenarioIntersectionButton = new Button(composite_scale, SWT.CHECK);
	    scenarioIntersectionButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if(!toggling) {
					toggling = true;
					scenarioUnionButton.setSelection(!scenarioIntersectionButton.getSelection());
					toggling = false;
				}
			}
	    });
	    scenarioUnionButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if(!toggling) {
					toggling = true;
					scenarioIntersectionButton.setSelection(!scenarioUnionButton.getSelection());
					toggling = false;
				}
			}
	    });
	    scenarioUnionButton.setSelection(true);
	    scenarioUnionButton.setText("union of scenarios");
	    scenarioIntersectionButton.setText("intersection of scenarios");
	    
	    sensorUnionButton = new Button(composite_scale, SWT.CHECK);
	    final Button sensorIntersectionButton = new Button(composite_scale, SWT.CHECK);
	    sensorIntersectionButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if(!toggling) {
					toggling = true;
					sensorUnionButton.setSelection(!sensorIntersectionButton.getSelection());
					toggling = false;
				}
			}
	    });
	    sensorUnionButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if(!toggling) {
					toggling = true;
					sensorIntersectionButton.setSelection(!sensorUnionButton.getSelection());
					toggling = false;
				}
			}
	    });
	    
	    sensorUnionButton.setSelection(true);
	    sensorUnionButton.setText("union of sensors");
	    sensorIntersectionButton.setText("intersection of sensors");
	    
		
		container.layout();	
		
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		boolean enableVis  = false;
		for(String label: sensorData.keySet()){
			SensorData temp = sensorData.get(label);
			if(temp.isIncluded &&  data.getSet().getSensorSettings(label).isSet())
				enableVis = true;
		}

		DREAMWizard.visLauncher.setEnabled(enableVis);
		DREAMWizard.convertDataButton.setEnabled(false);
	}
	
	private void addSensor(String dataType, String newName){
		data.getSet().addSensorSetting(newName, dataType);
		data.getSet().getInferenceTest().setMinimumRequiredForType(newName, -1);
		sensorData.put(newName, new SensorData(data.getSet().getSensorSettings(newName), newName));
	}
/*
 // Keeping old method for comparison if the new one gives us weird results
	private void volumeOfAquiferDegraded(){
		long current = System.currentTimeMillis();
		int detectionCriteriaStorage = data.getSet().getInferenceTest().getOverallMinimum();
		Map<String, Integer> detectionCriteriaStorageByType = new HashMap<String, Integer>();
		InferenceTest test = data.getSet().getInferenceTest();
		for(String sensorType: data.getSet().getSensorSettings().keySet()){
			detectionCriteriaStorageByType.put(sensorType, test.getMinimumForType(sensorType));
			test.setMinimumRequiredForType(sensorType, 1);
		}
		test.setMinimum(1);
		data.getSet().setInferenceTest(test);
		
		Map<Scenario, HashMap<Integer, Float>> timeToDegradationPerNode = new HashMap<Scenario, HashMap<Integer, Float>>();
		
		HashSet<Integer> nodes = new HashSet<Integer>();
		
		for(String sensorType: data.getSet().getSensorSettings().keySet()){
			nodes.addAll(data.getSet().getSensorSettings().get(sensorType).getValidNodes(null)); //TODO: might be a bad fix here
		}
		for(Integer nodeNumber: nodes){
			ExtendedConfiguration configuration = new ExtendedConfiguration();
			for(String sensorType: data.getSet().getSensorSettings().keySet()){
				configuration.addSensor(new ExtendedSensor(nodeNumber, sensorType, data.getSet().getNodeStructure()));
			}
			data.runObjective(configuration, Constants.runThreaded);
			for(Scenario scenario: configuration.getTimesToDetection().keySet()){
				if(!timeToDegradationPerNode.containsKey(scenario)) timeToDegradationPerNode.put(scenario, new HashMap<Integer, Float>());
				timeToDegradationPerNode.get(scenario).put(nodeNumber, configuration.getTimesToDetection().get(scenario));
			}
		}
	
		Map<Scenario, HashMap<Float, Float>> volumeDegradedByYear = new HashMap<Scenario, HashMap<Float, Float>>();
		HashSet<Float> years = new HashSet<Float>();
		for(Scenario scenario: timeToDegradationPerNode.keySet()){
			volumeDegradedByYear.put(scenario, new HashMap<Float, Float>());
			for(Integer nodeNumber: timeToDegradationPerNode.get(scenario).keySet()){
				Float year = timeToDegradationPerNode.get(scenario).get(nodeNumber);
				years.add(year);
				Point3i location = data.getScenarioSet().getNodeStructure().getIJKFromNodeNumber(nodeNumber);
				if(!volumeDegradedByYear.get(scenario).containsKey(year)) volumeDegradedByYear.get(scenario).put(year, data.getSet().getNodeStructure().getVolumeOfNode(location));
				else volumeDegradedByYear.get(scenario).put(year, volumeDegradedByYear.get(scenario).get(year) + data.getSet().getNodeStructure().getVolumeOfNode(location));
			}
		}
		
		ArrayList<Float> sortedYears = new ArrayList<Float>(years);
		java.util.Collections.sort(sortedYears);
		for(Scenario scenario: volumeDegradedByYear.keySet()){
			if(!volumeDegradedByYear.get(scenario).containsKey(sortedYears.get(0))) volumeDegradedByYear.get(scenario).put(sortedYears.get(0), 0f);
			for(int i=1; i<sortedYears.size(); ++i){
				if(!volumeDegradedByYear.get(scenario).containsKey(sortedYears.get(i))) volumeDegradedByYear.get(scenario).put(sortedYears.get(i), 0f);
				volumeDegradedByYear.get(scenario).put(sortedYears.get(i), volumeDegradedByYear.get(scenario).get(sortedYears.get(i)) + volumeDegradedByYear.get(scenario).get(sortedYears.get(i-1)));
			}
		}
		
		//set the set back to the original parameters (not 1 overall)
		test.setMinimum(detectionCriteriaStorage);
		for(String sensorType: data.getSet().getSensorSettings().keySet()){
			test.setMinimumRequiredForType(sensorType, detectionCriteriaStorageByType.get(sensorType));
		}
		data.getSet().setInferenceTest(test);
		
		SensorSetting.setVolumeDegradedByYear(volumeDegradedByYear, sortedYears);

		long total = System.currentTimeMillis() - current;
		System.out.println("Volume of aquifer degraded time:\t" + total/1000 + "." + total%1000);
		//for right now, we're returning the straight sum of the volume degraded (max = nodes_in_cloud*number_of_scenarios)
	}
*/
	
	private void volumeOfAquiferDegraded(){	
		long current = System.currentTimeMillis();
		
		HashSet<Integer> nodes = new HashSet<Integer>();
		
		for(String sensorType: data.getSet().getSensorSettings().keySet()){
			System.out.println(sensorType);
			nodes.addAll(data.getSet().getSensorSettings().get(sensorType).getValidNodes(null)); //TODO: might be a bad fix here
			System.out.println(nodes.size());
		}
		
		Map<Scenario, HashMap<Integer, Float>> timeToDegradationPerNode = new HashMap<Scenario, HashMap<Integer, Float>>();

		for(Scenario scenario: data.getSet().getScenarios()){
			timeToDegradationPerNode.put(scenario, new HashMap<Integer, Float>());
			for(Integer nodeNumber: nodes){	
				Float timeToDegredation = null;
				for (TimeStep timeStep: data.getSet().getNodeStructure().getTimeSteps()){
					for(String sensorType: data.getSet().getSensorSettings().keySet()){
						try {
							if(CCS9_1.sensorTriggered(data.getSet(), timeStep, scenario, sensorType, nodeNumber)) timeToDegredation = timeStep.getRealTime();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if(timeToDegredation != null) break;
					}
					if(timeToDegredation != null) break;
				}
				if(timeToDegredation != null) timeToDegradationPerNode.get(scenario).put(nodeNumber, timeToDegredation);
			}
		}
		
		System.out.println(timeToDegradationPerNode.size());
		
		Map<Scenario, HashMap<Float, Float>> volumeDegradedByYear = new HashMap<Scenario, HashMap<Float, Float>>();
		HashSet<Float> years = new HashSet<Float>();
		for(Scenario scenario: timeToDegradationPerNode.keySet()){
			volumeDegradedByYear.put(scenario, new HashMap<Float, Float>());
			for(Integer nodeNumber: timeToDegradationPerNode.get(scenario).keySet()){
				Float year = timeToDegradationPerNode.get(scenario).get(nodeNumber);
				years.add(year);
				Point3i location = data.getScenarioSet().getNodeStructure().getIJKFromNodeNumber(nodeNumber);
				if(!volumeDegradedByYear.get(scenario).containsKey(year)) volumeDegradedByYear.get(scenario).put(year, data.getSet().getNodeStructure().getVolumeOfNode(location));
				else volumeDegradedByYear.get(scenario).put(year, volumeDegradedByYear.get(scenario).get(year) + data.getSet().getNodeStructure().getVolumeOfNode(location));
			}
		}
		
		System.out.println(years.size());
		
		ArrayList<Float> sortedYears = new ArrayList<Float>(years);
		java.util.Collections.sort(sortedYears);
		for(Scenario scenario: volumeDegradedByYear.keySet()){
			if(!volumeDegradedByYear.get(scenario).containsKey(sortedYears.get(0))) volumeDegradedByYear.get(scenario).put(sortedYears.get(0), 0f);
			for(int i=1; i<sortedYears.size(); ++i){
				if(!volumeDegradedByYear.get(scenario).containsKey(sortedYears.get(i))) volumeDegradedByYear.get(scenario).put(sortedYears.get(i), 0f);
				volumeDegradedByYear.get(scenario).put(sortedYears.get(i), volumeDegradedByYear.get(scenario).get(sortedYears.get(i)) + volumeDegradedByYear.get(scenario).get(sortedYears.get(i-1)));
			}
		}
		SensorSetting.setVolumeDegradedByYear(volumeDegradedByYear, sortedYears);

		long total = System.currentTimeMillis() - current;
		System.out.println("New volume of aquifer degraded time:\t" + total/1000 + "." + total%1000);
	}
/*
	private void countParetoRedundant(){	
		long current = System.currentTimeMillis();
		
		HashSet<Integer> nodes = new HashSet<Integer>();
		
		for(String sensorType: data.getSet().getSensorSettings().keySet()){
			System.out.println(sensorType);
			nodes.addAll(data.getSet().getSensorSettings().get(sensorType).getValidNodes(null)); //TODO: might be a bad fix here
			System.out.println(nodes.size());
		}
		
		HashMap<Integer, ArrayList<Float>> optimalSolutions = new HashMap<Integer, ArrayList<Float>>();
		List<Scenario> sortedScenarios = data.getSet().getScenarios();
		Collections.sort(sortedScenarios);
		
		for(String sensorType: data.getSet().getSensorSettings().keySet()){
			System.out.println(sensorType);
			for(Integer nodeNumber: nodes){
				//build up the string ID and the list of ttds (for the ones that detect)
				ArrayList<Float> ttds = new ArrayList<Float>();
				for(Scenario scenario: sortedScenarios){
					Float timeToDegredation = Float.MAX_VALUE;
					for (TimeStep timeStep: data.getSet().getNodeStructure().getTimeSteps()){
						try {
							if(CCS9_1.sensorTriggered(data.getSet(), timeStep, scenario, sensorType, nodeNumber)) timeToDegredation = timeStep.getRealTime();
						} catch (Exception e) {
							e.printStackTrace();
						}
						if(timeToDegredation != Float.MAX_VALUE) break;
					}
					ttds.add(timeToDegredation);
				}
				ArrayList<Integer> toRemove = new ArrayList<Integer>(); //If this new configuration replaces one, it might replace multiple.
				boolean everyReasonTo = false;
				boolean everyReasonNot = false;
				for(Integer paretoSolutionLocation: optimalSolutions.keySet()){
					ArrayList<Float> paretoSolution = optimalSolutions.get(paretoSolutionLocation);
					boolean greater = false;
					boolean less = false;
					for(int i=0; i<paretoSolution.size(); ++i){
						if(paretoSolution.get(i) < ttds.get(i)) greater = true;
						if(paretoSolution.get(i) > ttds.get(i)) less = true;
					}
					if(greater && less){
						//don't need to do anything, both of these are pairwise pareto optimal
					}
					else if(greater && !less){
						everyReasonNot = true; //This solution is redundant, as there is another that is parwise optimal
						break; //we don't need to look anymore, don't include this new configuration
					}
					else if(!greater && less){
						everyReasonTo = true; //This solution is pareto optimal to this stored one
						toRemove.add(paretoSolutionLocation); //We need to remove this one, it has been replaced
					}
					else if(!greater && !less){
						//everyReasonNot = true; //These two spots are equal, so we might as well get rid of the one we're looking at
						break; //We don't need to check other spots if these are equal.
					}
				}
				if(everyReasonTo){
					//We need to add this one and remove some.
					for(Integer x : toRemove){
						optimalSolutions.remove(x);
					}
					optimalSolutions.put(nodeNumber, ttds);
				}
				else if(everyReasonNot){
					//Lets not add this one, it's redundant
				}
				else{
					//No reason not to add it and it didn't replace one, it must be another pareto optimal answer. Let's add it.
					optimalSolutions.put(nodeNumber, ttds);
				}
			}
		}

		System.out.println("Number of initial spots: " + nodes.size());
		
		int count = optimalSolutions.size();
		
		System.out.println("Number of solutions left: " + count);
		
		StringBuilder x = new StringBuilder();
		for(Integer location: optimalSolutions.keySet()){
			Point3i point = data.getSet().getNodeStructure().getIJKFromNodeNumber(location);
			x.append(location + " (" + point.getI() + " " + point.getJ() + " " + point.getK() + "):,");
			for(Float y : optimalSolutions.get(location)){
				x.append(y + ", ");
			}
			x.append(optimalSolutions.get(location).size());
			x.append("\n");
		}
		try {
			FileUtils.write(new File("C:/Users/rodr144/Documents/SVN Projects/DREAM/DREAM/testoutput.csv"), x.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		long total = System.currentTimeMillis() - current;
		System.out.println("Pareto thing time:\t" + total/1000 + "." + total%1000);
	}
*/	
	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}

	@Override
	public void setPageCurrent(boolean current) {
		isCurrentPage = current;
	}
	
	private void testReady() {
		/*
		boolean isReady = true;
		
		for(SensorData data: sensorData.values()) {


			if(data.costText.getForeground().equals(new Color(container.getDisplay(), 255, 0, 0))) {
				isReady = false;
				break;
			}

			if(data.valueInput.getForeground().equals(new Color(container.getDisplay(), 255, 0, 0))) {
				isReady = false;
				break;
			}

		}
		if(this.isPageComplete() != isReady)
			this.setPageComplete(isReady);
			*/
	}
	
}
