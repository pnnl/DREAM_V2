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
import objects.NodeStructure;
import objects.Scenario;
import objects.ScenarioSet;
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
import hdf5Tool.HDF5Wrapper;
import utilities.Constants;
import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;
import wizardPages.Page_LeakageCriteria.SensorData;

/**
 * Page for aquifer degradation criteria - stripped down version of Page_MonitoringParameters, for the most part
 * Info description line 421
 * @author port091
 * @author rodr144
 */

public class Page_DegradationCriteria extends WizardPage implements AbstractWizardPage {
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	
	private STORMData data;
	private boolean isCurrentPage = false;

	private Text volumeCostText;
	private double volumeCostValue;
	
	private Map<String, DegradationData> degradationData;
		
	protected Page_DegradationCriteria(STORMData data) {
		super("Degredation Criteria");
		this.data = data;	
	}
	
	/* Subclass meant for storing the criteria per sensor type */
	public class DegradationData {

		public String sensorType;
		public String sensorName;
		public boolean isIncluded;
		public float maxZ;
		public float minZ;
		public Trigger trigger;
		public DeltaType deltaType;
		
		private float dataMin;
		private float dataMax;
		
		public float min;
		public float max;

				
		// A couple of these need to be global
		private Label sensorTypeLabel;
		private Combo thresholdCombo;
		private Label valueLabel;
		private Text valueInput;
		private Label maxZLabel;
		private Text maxZText;
		private Label minZLabel;
		private Text minZText;
		
		public DegradationData(String sensorName) {
			
			sensorType = sensorName;
			this.sensorName = sensorName;
			isIncluded = false; // By default	
			maxZ = SensorSetting.globalMaxZ;
			minZ = SensorSetting.globalMinZ;
			
			trigger = Trigger.MINIMUM_THRESHOLD;
			deltaType = DeltaType.BOTH;
			
			// Try to be a little smarter about pressure
			if(sensorType.toLowerCase().contains("pressure") || sensorType.toLowerCase().equals("p"))
				trigger = Trigger.RELATIVE_DELTA;
			
			// Just need to make sure we never have other parameters with "ph"...
			if(sensorType.toLowerCase().equals("ph"))
				trigger = Trigger.MAXIMUM_THRESHOLD;
		}	
		
		public void buildUI() {
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
					} catch (NumberFormatException ex) {
						if(valueInput != null)
							valueInput.setForeground(new Color(container.getDisplay(), 255, 0, 0));
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
					} catch (NumberFormatException ne) {
						((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
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
					} catch (NumberFormatException ne) {
						((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
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
					} catch (NumberFormatException ne) {
						((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
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
		layout.numColumns = 5;
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
			ArrayList<SensorSetting> sensorSettings = new ArrayList<SensorSetting>();
			for(String label: degradationData.keySet()){
				DegradationData degData = degradationData.get(label);
				if(degData.isIncluded){
					SensorSetting x = new SensorSetting(data.getSet().getNodeStructure(), data.getSet(), label, data.getScenarioSet().getScenarios());
					x.setTrigger(degData.trigger);
					if(degData.trigger == Trigger.MAXIMUM_THRESHOLD){
						x.setUpperThreshold(degData.max);
					}
					x.setUserSettings(0f, null, degData.min, degData.max, degData.trigger, false, degData.deltaType, degData.minZ, degData.maxZ);
					sensorSettings.add(x);
				}
			}
			volumeOfAquiferDegraded(sensorSettings);			

		}
		catch(Exception e){
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	public void loadPage() {
		isCurrentPage = true;
		
		if(degradationData == null) {
			// New UI
			degradationData = new TreeMap<String, DegradationData>();
		
			for(String dataType: data.getSet().getAllPossibleDataTypes()) {
								
				degradationData.put(dataType, new DegradationData(dataType));
			}
		}
		
		for(Control control: container.getChildren()){
			control.dispose();
		}
		
		Font boldFont = new Font( container.getDisplay(), new FontData( "Helvetica", 12, SWT.BOLD ) );

		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Degradation Criteria");
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
				MessageDialog.openInformation(container.getShell(), "Additional information", "Whatcha want here Catherine?");
			}			
		});
		infoLink.setLayoutData(infoLinkData);
		
		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("Select the criteria for determining degradation of the aquifer");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 2;
		infoGridData.widthHint = 200;
		infoLabel.setLayoutData(infoGridData);
				
		// Headers
		
		Font boldFont1 = new Font( container.getDisplay(), new FontData( "Helvetica", 10, SWT.BOLD ) );
		
		Label degradeParams = new Label(container, SWT.LEFT);
		Label detectionCriteria = new Label(container, SWT.LEFT);
		Label valueLabel = new Label(container, SWT.LEFT);
		Label minZLabel = new Label(container, SWT.LEFT);
		Label maxZLabel = new Label(container, SWT.LEFT);
		
		degradeParams.setText("Degradation Parameter");
		detectionCriteria.setText("Detection Criteria");
		valueLabel.setText("Value");
		minZLabel.setText("Minimum Z");
		maxZLabel.setText("Maximum Z");
			
		degradeParams.setFont(boldFont1);
		detectionCriteria.setFont(boldFont1);
		valueLabel.setFont(boldFont1);
		minZLabel.setFont(boldFont1);
		maxZLabel.setFont(boldFont1);
		
		for(DegradationData data: degradationData.values()) {
			data.buildUI();
		}
		
		Label volumeCost = new Label(container, SWT.LEFT);
		volumeCost.setFont(boldFont1);
		volumeCost.setText("Cost per unit volume:");
		volumeCostText = new Text(container, SWT.BORDER | SWT.SINGLE);
		volumeCostText.setText(Constants.decimalFormat.format(0));
		volumeCostText.setSize(1000, 20);
		volumeCostText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					volumeCostValue = Float.parseFloat(((Text)e.getSource()).getText());	
					((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 0, 0, 0));
				} catch (NumberFormatException ne) {
					((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
				}
			}				
		});
		GridData volumeCostGridData = new GridData(SWT.FILL, SWT.END, false, false);
		volumeCostText.setLayoutData(volumeCostGridData);

		container.layout();
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		

		DREAMWizard.convertDataButton.setEnabled(false);
	}
	
	private void volumeOfAquiferDegraded(ArrayList<SensorSetting> sensorSettings){	
		long current = System.currentTimeMillis();
		
		HashSet<Integer> nodes = new HashSet<Integer>();
		Point3i dims = data.getSet().getNodeStructure().getIJKDimensions();
		for(SensorSetting sensorSetting: sensorSettings){
			for(Scenario scenario: data.getSet().getScenarios()) {
				// Query for valid nodes per scenario
				try {
					HashSet<Integer> innerNodes = Constants.hdf5Data.isEmpty() ? 
							HDF5Wrapper.queryNodesFromFiles(data.getSet().getNodeStructure(), scenario.toString(), sensorSetting.getType(),
									sensorSetting.getLowerThreshold(), sensorSetting.getUpperThreshold(), null) : 
								HDF5Wrapper.queryNodesFromMemory(data.getSet().getNodeStructure(), scenario.toString(), sensorSetting.getType(),
										sensorSetting.getLowerThreshold(), sensorSetting.getUpperThreshold(), null);
							nodes.addAll(innerNodes);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		Map<Scenario, HashMap<Integer, Float>> timeToDegradationPerNode = new HashMap<Scenario, HashMap<Integer, Float>>();

		for(Scenario scenario: data.getSet().getScenarios()){
			timeToDegradationPerNode.put(scenario, new HashMap<Integer, Float>());
			for(Integer nodeNumber: nodes){	
				Float timeToDegredation = null;
				for (TimeStep timeStep: data.getSet().getNodeStructure().getTimeSteps()){
					for(SensorSetting setting: sensorSettings){
						try {
							if(CCS9_1.volumeSensorTriggered(setting, data.getSet().getNodeStructure(), timeStep, scenario, setting.getType(), nodeNumber)) timeToDegredation = timeStep.getRealTime();
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
		
		//System.out.println(timeToDegradationPerNode.size());
		
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
		
		//System.out.println(years.size());
		if(years.size() == 0){
			SensorSetting.setVolumeDegradedByYear(volumeDegradedByYear, new ArrayList<Float>());
			return;
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
		SensorSetting.setVolumeDegradedByYear(volumeDegradedByYear, sortedYears);

		long total = System.currentTimeMillis() - current;
		//System.out.println("Updated volume of aquifer degraded time:\t" + total/1000 + "." + total%1000);
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
