package wizardPages;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import objects.E4DSensors;
import objects.Scenario;
import objects.Sensor;
import objects.SensorSetting;
import objects.TimeStep;
import objects.SensorSetting.DeltaType;
import objects.SensorSetting.Trigger;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import functions.*;
import hdf5Tool.HDF5Interface;
import utilities.Constants;
import utilities.Point3i;
import utilities.Constants.ModelOption;
import utilities.E4DDialog;
import wizardPages.DREAMWizard.STORMData;

/**
 * Page for setting detection criteria for each sensor type to be used.
 * See line 608
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class Page_LeakageCriteria extends DreamWizardPage implements AbstractWizardPage {
		
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	protected Map<String, Integer> num_duplicates = new HashMap<String, Integer>();
	
	private STORMData data;
	private boolean isCurrentPage = false;
	private Button e4dButton;
	
	private Map<String, SensorData> sensorData;
	
		
	protected Page_LeakageCriteria(STORMData data) {
		super("Leakage Criteria");
		this.data = data;	
	}
	
	public class SensorData {

		private Button addButton;
		public String sensorType;
		public String sensorName;
		public String alias;
		public float cost;
		public Trigger trigger;
		public DeltaType deltaType;
		public float lowerThreshold;
		public float upperThreshold;
		private float detection;
		private float leakage;
		public float minZ;
		public float maxZ;
		public float minValue;
		public float maxValue;
		
		private Float maxZBound;
		private Float minZBound;
		public boolean isIncluded;
		private boolean isDuplicate;
		
		private Label nodeLabel;
		private Label sensorTypeLabel;
		private Label detectionLabel;
		private Label leakageLabel;
		private Label minZLabel;
		private Label maxZLabel;
		
		private Text aliasText;
		private Text costText;
		private Text detectionText;
		private Text leakageText;
		private Text minZText;
		private Text maxZText;
		
		private Combo thresholdCombo;
		
		
		//Class for storing the data about one particular sensor type.
		public SensorData(SensorSetting sensorSettings, String sensorName) {
			
			if(sensorSettings.getType().equals(sensorName)) isDuplicate = false;
			else isDuplicate = true;
			
			sensorType = sensorSettings.getType();
			this.sensorName = sensorName;
			alias = sensorName;
			if(sensorName.contains("Electrical Conductivity"))
				alias = "ERT";
			isIncluded = false; //By default
			cost = sensorSettings.getCost();
			minValue = sensorSettings.getMinValue();
			maxValue = sensorSettings.getMaxValue();
			minZ = minZBound = sensorSettings.getGlobalMinZ();
			maxZ = maxZBound = sensorSettings.getGlobalMaxZ();
			
			trigger = Trigger.MINIMUM_THRESHOLD;
			deltaType = sensorSettings.getDeltaType();
			
			// Trigger should be relative delta when pressure
			if(sensorType.toLowerCase().contains("pressure") || sensorType.trim().toLowerCase().equals("p"))
				trigger = Trigger.RELATIVE_DELTA;
			
			// Trigger should be maximum threshold when pH
			if(sensorType.trim().toLowerCase().equals("ph"))
				trigger = Trigger.MAXIMUM_THRESHOLD;
			
			// Default thresholds
			if(trigger == Trigger.MAXIMUM_THRESHOLD) { //Anything less than the upper threshold constitutes a leak
				lowerThreshold = Float.MIN_VALUE;
				upperThreshold = sensorSettings.getUpperThreshold();
			} else if(trigger == Trigger.MINIMUM_THRESHOLD) { //Anything greater than the lower threshold constitutes a leak
				lowerThreshold = sensorSettings.getLowerThreshold();
				upperThreshold = Float.MAX_VALUE;
			} else { //Deltas: Leakage is based on change, which varies by node... just store this for now.
				lowerThreshold = sensorSettings.getLowerThreshold();
				upperThreshold = sensorSettings.getUpperThreshold();
			}
		}	
		
		public void buildUI(String type) {
			//Add a button here
			if(isDuplicate){
				addButton = new Button(container, SWT.PUSH);
			    addButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event arg0) {
						sensorData.remove(sensorName);
						data.getSet().getSensorSettings().remove(sensorName);
						loadPage();
						fixMacBug();
					}
			    });
			    addButton.setText("-");
			}
			else{
				addButton = new Button(container, SWT.PUSH);
			    addButton.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event arg0) {
						if(!num_duplicates.containsKey(sensorType)) num_duplicates.put(sensorType, 1);
						addSensor(sensorType, sensorType + "_" + num_duplicates.get(sensorType));
						num_duplicates.put(sensorType, num_duplicates.get(sensorType)+1);
						loadPage();
						fixMacBug();
						if(num_duplicates.get(sensorType)==100) //rare, but prevent more than 99 duplicates so statistics doesn't throw an error
							for(SensorData temp: sensorData.values())
								if(temp.sensorName==sensorType)
									temp.addButton.setEnabled(false);
					}
			    });
			    addButton.setText("+");
			}
			
		    
			// Include button
			Button includeButton = new Button(container,  SWT.CHECK);
			includeButton.setSelection(isIncluded);
			includeButton.setText(sensorType);
			for(SensorData temp: sensorData.values()) {
				if(temp.isIncluded) {
					errorFound(false, "  Must select a monitoring parameter.");
					break;
				}
				errorFound(true, "  Must select a monitoring parameter.");
			}
			includeButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) { 
					//required to have this... not sure when it is triggered.
				}
				@Override
				public void widgetSelected(SelectionEvent e) {
					isIncluded = ((Button)e.getSource()).getSelection();
					if(isIncluded==true)
						data.getSet().getInferenceTest().setMinimumForType(((Button)e.getSource()).getText(), 1);
					else
						data.getSet().getInferenceTest().setMinimumForType(((Button)e.getSource()).getText(), 0);
					toggleEnabled();
					
					//Special handling if errors are negated when parameters are unchecked...
					//We have to search through all possible errors to see if any are negated
					boolean checkError = true;
					boolean commaError = false;
					boolean duplicateError = false;
					boolean emptyError = false;
					boolean costError = false;
					boolean detectionError = false;
					boolean leakageError = false;
					boolean botError = false;
					boolean botBoundError = false;
					boolean topError = false;
					boolean topBoundError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) //Skip unchecked parameters
							continue;
						else
							checkError = false;
						//Alias
						for(SensorData temp2: sensorData.values()) {
							if(!temp2.isIncluded) //Skip unchecked parameters
								continue;
							if(temp.alias.trim().equals(temp2.alias.trim()) && !temp.sensorName.equals(temp2.sensorName)) {
								duplicateError = true;
								temp.aliasText.setForeground(red);
							}
						}
						if(temp.alias.contains(",")) //Contains a comma
							commaError = true;
						if(temp.alias.isEmpty()) //No alias
							emptyError = true;
						//Cost
						if(!isValidFloat(temp.costText.getText()))
							costError = true;
						//Detection
						if(!isValidFloat(temp.detectionText.getText()))
							detectionError = true;
						//Leakage
						if(!isValidFloat(temp.leakageText.getText()))
							leakageError = true;
						//Zone bottom
						if(!isValidFloat(temp.minZText.getText()))
							botError = true;
						else {
							float minZValue = Float.parseFloat(temp.minZText.getText());
							if (minZValue < temp.minZBound || minZValue > temp.maxZBound)
								botBoundError = true;
						}
						//Zone top
						if(!isValidFloat(temp.maxZText.getText()))
							topError = true;
						else {
							float maxZValue = Float.parseFloat(temp.maxZText.getText());
							if (maxZValue < temp.minZBound || maxZValue > temp.maxZBound)
								topBoundError = true;
						}
					}
					errorFound(checkError, "  Must select a monitoring parameter.");
					errorFound(duplicateError, "  Duplicate alias.");
					errorFound(commaError, "  Cannot use commas in alias.");
					errorFound(emptyError, "  Need to enter an alias.");
					errorFound(costError, "  Cost is not a real number.");
					errorFound(detectionError, "  Detection is not a real number.");
					errorFound(leakageError, "  Leakage is not a real number.");
					errorFound(botError, "  Bottom is not a real number.");
					errorFound(botBoundError, "  Bottom outside domain bounds.");
					errorFound(topError, "  Top is not a real number.");
					errorFound(topBoundError, "  Top outside domain bounds.");
					
					//Special handling of red text for duplicates
					if (duplicateError==false)
						for(SensorData data: sensorData.values())
							if (data.isIncluded && !data.alias.contains(",") && !data.alias.isEmpty())
								data.aliasText.setForeground(black);
				}
			});
			
			
			//Alias Input
			aliasText = new Text(container, SWT.BORDER | SWT.SINGLE);
			aliasText.setText(sensorData.get(type).alias);
			aliasText.setForeground(black);
			aliasText.addModifyListener(new ModifyListener(){
				@Override
				public void modifyText(ModifyEvent e){
					aliasText = ((Text)e.getSource());
					boolean commaError = false;
					boolean duplicateError = false;
					boolean emptyError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						//temp.aliasText.setForeground(black);
						for(SensorData temp2: sensorData.values()) {
							if(!temp2.isIncluded) continue; //Skip unchecked parameters
							if(temp.aliasText.getText().trim().equals(temp2.aliasText.getText().trim()) && !temp.sensorName.equals(temp2.sensorName)) {
								temp.aliasText.setForeground(red);
								duplicateError = true;
							}
						}
						if(temp.aliasText.getText().contains(",")) { //Contains a comma
							temp.aliasText.setForeground(red);
							commaError = true;
						}
						if(temp.aliasText.getText().trim().isEmpty()) { //Empty alias
							temp.aliasText.setForeground(red);
							emptyError = true;
						}
						if (duplicateError==false && commaError==false && emptyError==false) { //No errors
							temp.aliasText.setForeground(black);
							temp.alias = temp.aliasText.getText();
						}
					}
					errorFound(duplicateError, "  Duplicate alias.");
					errorFound(commaError, "  Cannot use commas in alias.");
					errorFound(emptyError, "  Need to enter an alias.");
				}
			});
			GridData aliasTextData = new GridData(SWT.FILL, SWT.END, false, false);
			aliasTextData.widthHint = 60;
			aliasText.setLayoutData(aliasTextData);
			
			
			//Cost Input
			costText = new Text(container, SWT.BORDER | SWT.SINGLE);
			costText.setText(String.valueOf(sensorData.get(type).cost));
			costText.setForeground(black);
			costText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					costText = ((Text)e.getSource());
					boolean costError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						if(isValidFloat(temp.costText.getText())) { //Valid number
							temp.costText.setForeground(black);
							temp.cost = Float.valueOf(temp.costText.getText());
						} else { //Not a valid number
							temp.costText.setForeground(red);
							costError = true;
						}
					}
					errorFound(costError, "  Cost is not a real number.");
				}
			});
			GridData costTextData = new GridData(SWT.FILL, SWT.END, false, false);
			costTextData.widthHint = 60;
			costText.setLayoutData(costTextData);
			
			
			//Detection Criteria
			thresholdCombo = new Combo(container, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
			thresholdCombo.add(Trigger.MAXIMUM_THRESHOLD.toString());
			thresholdCombo.add(Trigger.MINIMUM_THRESHOLD.toString());
			thresholdCombo.add(Trigger.RELATIVE_DELTA.toString());
			thresholdCombo.add(Trigger.ABSOLUTE_DELTA.toString());
			thresholdCombo.setText(trigger.toString());
			if(trigger == Trigger.MAXIMUM_THRESHOLD)
				thresholdCombo.setToolTipText("Leak when concentration is less than value");
			else if(trigger == Trigger.MINIMUM_THRESHOLD)
				thresholdCombo.setToolTipText("Leak when concentration is greater than value");
			else if(trigger == Trigger.RELATIVE_DELTA)
				thresholdCombo.setToolTipText("Leak when change from original concentration relative to the initial concentration (decimal) exceeds value");
			else if(trigger == Trigger.ABSOLUTE_DELTA)
				thresholdCombo.setToolTipText("Leak when change from original concentration exceeds value");
			thresholdCombo.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					boolean isMinimumThreshold = ((Combo)e.getSource()).getText().equals(Trigger.MAXIMUM_THRESHOLD.toString());
					boolean isMaximumThreshold = ((Combo)e.getSource()).getText().equals(Trigger.MINIMUM_THRESHOLD.toString());
					boolean isRelativeDetla = ((Combo)e.getSource()).getText().equals(Trigger.RELATIVE_DELTA.toString());
					trigger = isMinimumThreshold ? Trigger.MAXIMUM_THRESHOLD : (isMaximumThreshold ? 
							Trigger.MINIMUM_THRESHOLD : isRelativeDetla ? Trigger.RELATIVE_DELTA : Trigger.ABSOLUTE_DELTA);	
					if(trigger == Trigger.MAXIMUM_THRESHOLD) { //Anything less than the upper threshold constitutes a leak
						lowerThreshold = Float.MIN_VALUE;
						upperThreshold = Math.min(detection, leakage);
					} else if(trigger == Trigger.MINIMUM_THRESHOLD) { //Anything greater than the lower threshold constitutes a leak
						lowerThreshold = Math.max(detection, leakage);
						upperThreshold = Float.MAX_VALUE;
					} else { //Deltas: Leakage is based on change, which varies by node... just store this for now.
						lowerThreshold = Math.max(detection, leakage);
						upperThreshold = Math.max(detection, leakage);
					}
					errorFound(false, "  No nodes were found for the provided parameters.");
					if(detectionText.getText().contains("+")) deltaType = DeltaType.INCREASE;
					else if(detectionText.getText().contains("-")) deltaType = DeltaType.DECREASE;
					else deltaType = DeltaType.BOTH;
					if(trigger == Trigger.MAXIMUM_THRESHOLD)
						thresholdCombo.setToolTipText("Leak when concentration is less than value");
					else if(trigger == Trigger.MINIMUM_THRESHOLD)
						thresholdCombo.setToolTipText("Leak when concentration is greater than value");
					else if(trigger == Trigger.RELATIVE_DELTA)
						thresholdCombo.setToolTipText("Leak when change from original concentration relative to the initial concentration (decimal) exceeds value");
					else if(trigger == Trigger.ABSOLUTE_DELTA)
						thresholdCombo.setToolTipText("Leak when change from original concentration exceeds value");
				}
			});
			GridData thresholdComboData = new GridData(SWT.FILL, SWT.END, false, false);
			thresholdComboData.widthHint = 105;
			thresholdCombo.setLayoutData(thresholdComboData);			
			
			//Detection Value
			//Below is a mapping of which value should be taken in different circumstances
			//The threshold represents the threshold of what constitutes a leak
			//Having better detection ability than a leak doesn't matter, we only care if the detection ability limits
			// if (threshold = max), pick smaller entry between D and L
			// if (threshold = min or delta), pick larger entry between D and L
			detectionText = new Text(container, SWT.BORDER | SWT.SINGLE);
			detectionText.setText(String.valueOf(sensorData.get(type).detection));
			detectionText.setToolTipText("Minimum = " + HDF5Interface.queryStatistic(type, 0) + "; Maximum = " + HDF5Interface.queryStatistic(type, 2));
			//detectionText.setToolTipText("Minimum = " + sensorData.get(type).minValue + "; Maximum = " + sensorData.get(type).maxValue);
			detectionText.setForeground(black);
			detectionText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					detectionText = ((Text)e.getSource());
					boolean detectionError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						if(isValidFloat(temp.detectionText.getText())) { //Valid number
							temp.detectionText.setForeground(black);
							temp.detection = Float.valueOf(temp.detectionText.getText());
							if(temp.trigger==Trigger.MAXIMUM_THRESHOLD) { //Anything less than the upper threshold constitutes a leak
								temp.lowerThreshold = Float.MIN_VALUE;
								temp.upperThreshold = Math.min(temp.detection, temp.leakage);
							} else if(temp.trigger==Trigger.MINIMUM_THRESHOLD) { //Anything greater than the lower threshold constitutes a leak
								temp.lowerThreshold = Math.max(temp.detection, temp.leakage);
								temp.upperThreshold = Float.MAX_VALUE;
							} else { //Deltas: Leakage is based on change, which varies by node... just store this for now.
								temp.lowerThreshold = Math.max(temp.detection, temp.leakage);
								temp.upperThreshold = Math.max(temp.detection, temp.leakage);
							}
						} else { //Not a valid number
							temp.detectionText.setForeground(red);
							detectionError = true;
						}
					}
					errorFound(detectionError, "  Detection is not a real number.");
					errorFound(false, "  No nodes were found for the provided parameters.");
					if(detectionText.getText().contains("+")) deltaType = DeltaType.INCREASE;
					else if(detectionText.getText().contains("-")) deltaType = DeltaType.DECREASE;
					else deltaType = DeltaType.BOTH;
				}
			});
			GridData detectionInputData = new GridData(SWT.FILL, SWT.END, false, false);
			detectionInputData.widthHint = 60;
			detectionText.setLayoutData(detectionInputData);
			
			
			//Leakage Criteria
			leakageText = new Text(container, SWT.BORDER | SWT.SINGLE);
			leakageText.setText(String.valueOf(sensorData.get(type).leakage));
			leakageText.setToolTipText("Minimum = " + HDF5Interface.queryStatistic(type, 0) + "; Maximum = " + HDF5Interface.queryStatistic(type, 2));
			//leakageText.setToolTipText("Minimum = " + sensorData.get(type).minValue + "; Maximum = " + sensorData.get(type).maxValue);
			leakageText.setForeground(black);
			leakageText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					leakageText = ((Text)e.getSource());
					boolean leakageError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						if(isValidFloat(temp.leakageText.getText())) { //Valid number
							temp.leakageText.setForeground(black);
							temp.leakage = Float.valueOf(temp.leakageText.getText());
							if(temp.trigger==Trigger.MAXIMUM_THRESHOLD) { //Anything less than the upper threshold constitutes a leak
								temp.lowerThreshold = Float.MIN_VALUE;
								temp.upperThreshold = Math.min(temp.detection, temp.leakage);
							} else if(temp.trigger==Trigger.MINIMUM_THRESHOLD) { //Anything greater than the lower threshold constitutes a leak
								temp.lowerThreshold = Math.max(temp.detection, temp.leakage);
								temp.upperThreshold = Float.MAX_VALUE;
							} else { //Deltas: Leakage is based on change, which varies by node... just store this for now.
								temp.lowerThreshold = Math.max(temp.detection, temp.leakage);
								temp.upperThreshold = Math.max(temp.detection, temp.leakage);
							}
						} else { //Not a valid number
							temp.leakageText.setForeground(red);
							leakageError = true;
						}
					}
					errorFound(leakageError, "  Leakage is not a real number.");
					errorFound(false, "  No nodes were found for the provided parameters.");
					if(leakageText.getText().contains("+")) deltaType = DeltaType.INCREASE;
					else if(leakageText.getText().contains("-")) deltaType = DeltaType.DECREASE;
					else deltaType = DeltaType.BOTH;
				}
			});
			GridData leakageInputData = new GridData(SWT.FILL, SWT.END, false, false);
			leakageInputData.widthHint = 60;
			leakageText.setLayoutData(leakageInputData);
			
			
			// Set minimum z
			minZText = new Text(container, SWT.BORDER | SWT.SINGLE);
			minZText.setText(String.valueOf(sensorData.get(type).minZ));
			minZText.setForeground(black);
			minZText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					minZText = ((Text)e.getSource());
					boolean botError = false;
					boolean botBoundError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						if(isValidFloat(temp.minZText.getText())) { //Valid number
							float minZValue = Float.valueOf(temp.minZText.getText());
							if (minZValue < minZBound || minZValue > maxZBound) {
								temp.minZText.setForeground(red);
								botBoundError = true;
							} else {
								temp.minZText.setForeground(black);
								temp.minZ = minZValue;
							}
						} else { //Not a valid number
							temp.minZText.setForeground(red);
							botError = true;
						}
					}
					errorFound(botError, "  Bottom is not a real number.");
					errorFound(botBoundError, "  Bottom outside domain bounds.");
				}
			});
			GridData minZTextData = new GridData(SWT.FILL, SWT.END, false, false);
			minZTextData.widthHint = 60;
			minZText.setLayoutData(minZTextData);
			
			
			// Set maximum z
			maxZText = new Text(container, SWT.BORDER | SWT.SINGLE);
			maxZText.setText(String.valueOf(sensorData.get(type).maxZ));
			maxZText.setForeground(black);
			maxZText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					maxZText = ((Text)e.getSource());
					boolean topError = false;
					boolean topBoundError = false;
					for(SensorData temp: sensorData.values()) {
						if(!temp.isIncluded) continue; //Skip unchecked parameters
						if(isValidFloat(temp.maxZText.getText())) { //Valid number
							float maxZValue = Float.valueOf(temp.maxZText.getText());
							if (maxZValue < minZBound || maxZValue > maxZBound) {
								temp.maxZText.setForeground(red);
								topBoundError = true;
							} else {
								temp.maxZText.setForeground(black);
								temp.maxZ = maxZValue;
							}
						} else { //Not a valid number
							temp.maxZText.setForeground(red);
							topError = true;
						}
					}
					errorFound(topError, "  Top is not a real number.");
					errorFound(topBoundError, "  Top outside domain bounds.");
				}
			});
			GridData maxZTextData = new GridData(SWT.FILL, SWT.END, false, false);
			maxZTextData.widthHint = 60;
			maxZText.setLayoutData(maxZTextData);
			
			// Hide unused fields for ERT sensors
			if (type.contains("Electrical Conductivity")) {
				addButton.setVisible(false);
				aliasText.setEnabled(false);
				thresholdCombo.setVisible(false);
				detectionText.setVisible(false);
				leakageText.setVisible(false);
				minZText.setVisible(false);
				maxZText.setVisible(false);
			}
			
			toggleEnabled();
		}
		
		private void toggleEnabled() {
			if(sensorTypeLabel != null && !sensorTypeLabel.isDisposed())
				sensorTypeLabel.setEnabled(isIncluded);
			if(aliasText != null && !aliasText.isDisposed() && !alias.contains("ERT"))
				aliasText.setEnabled(isIncluded);
			if(costText != null && !costText.isDisposed())
				costText.setEnabled(isIncluded);
			if(thresholdCombo != null && !thresholdCombo.isDisposed())
				thresholdCombo.setEnabled(isIncluded);					
			if(detectionText != null && !detectionText.isDisposed())
				detectionText.setEnabled(isIncluded);
			if(detectionLabel != null && !detectionLabel.isDisposed())
				detectionLabel.setEnabled(isIncluded);
			if(leakageText != null && !leakageText.isDisposed())
				leakageText.setEnabled(isIncluded);
			if(leakageLabel != null && !leakageLabel.isDisposed())
				leakageLabel.setEnabled(isIncluded);
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
	public void loadPage() {
		isCurrentPage = true;
		if(!DREAMWizard.errorMessage.getText().contains("  No nodes were found for the provided parameters."))
			DREAMWizard.errorMessage.setText("");
		if(sensorData == null || data.needToResetMonitoringParameters) {
			data.needToResetMonitoringParameters = false;
			// New UI
			sensorData = new TreeMap<String, SensorData>();
			
			//Only adds ERT sensor if a results matrix is detected in the correct location
			E4DSensors.addERTSensor(data);
			
			for(String dataType: data.getSet().getAllPossibleDataTypes())	
				sensorData.put(dataType, new SensorData(data.getSet().getSensorSettings(dataType), dataType));
		}
		
		for(Control control: container.getChildren())
			control.dispose(); // Remove the children.
		
		Font boldFont = new Font(container.getDisplay(), new FontData("Helvetica", 12, SWT.BOLD));
		Font boldFontSmall = new Font(container.getDisplay(), new FontData("Helvetica", 10, SWT.BOLD));
		
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
				MessageDialog.openInformation(container.getShell(), "Additional information", "After reading through the directory of realization outputs, DREAM will generate a table of monitoring parameters that the user can select. These parameters are specific to the included realizations. The selected monitoring parameters will be used in the optimization algorithm. The user may label what technology they will use to monitor each selected parameter in the \"Alias for Monitoring Technology\" box and then provide a realistic cost per monitoring technology if it is known; if not, the costs should be set equal. The detection criteria may be specified based on the relative change from initial conditions, absolute change from initial conditions, or a maximum or minimum threshold. If relative delta, absolute delta, or maximum threshold is selected, the given value and all values above are treated as detecting a leak. If minimum threshold is selected, that value and all values below are treated as detecting a leak.");	
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
		Label detectionLabel = new Label(container, SWT.LEFT);
		Label leakageLabel = new Label(container, SWT.LEFT);
		Label minZLabel = new Label(container, SWT.LEFT);
		Label maxZLabel = new Label(container, SWT.LEFT);
		
		blankFiller.setText("");
		monitorParams.setText("Monitoring Parameter");
		aliasLabel.setText("Alias for Monitoring Technology");
		costPerSensor.setText("Cost per Sensor");
		detectionCriteria.setText("Detection Criteria");
		detectionLabel.setText("Detection Value");
		leakageLabel.setText("Leakage Criteria");
		minZLabel.setText("Zone Bottom");
		maxZLabel.setText("Zone Top");
		
		monitorParams.setFont(boldFont1);
		aliasLabel.setFont(boldFont1);
		costPerSensor.setFont(boldFont1);
		detectionCriteria.setFont(boldFont1);
		detectionLabel.setFont(boldFont1);
		leakageLabel.setFont(boldFont1);
		minZLabel.setFont(boldFont1);
		maxZLabel.setFont(boldFont1);
		
		for(SensorData data: sensorData.values()) {
			data.buildUI(data.sensorName);
		}
		
		Group parametersGroup = new Group(container, SWT.SHADOW_NONE);
		parametersGroup.setText("Nodes Found for Each Parameter");
		parametersGroup.setFont(boldFontSmall);
		parametersGroup.setLayout(new GridLayout(4,true));
		GridData tempData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		tempData.horizontalSpan = 10;
		parametersGroup.setLayoutData(tempData);
		
		Button queryButton = new Button(parametersGroup, SWT.BALLOON);
		queryButton.setText("Find triggering nodes");
		queryButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				fixMacBug();
				HDF5Interface.hdf5CloudData.clear();
				boolean reset = true;
				Map<String, SensorData> sensorSettings = new HashMap<String, SensorData>();
				
				SensorSetting.sensorTypeToDataType = new HashMap<String, String>();
				Map<String, String> sensorAliases = new HashMap<String, String>();
				if(data.modelOption == ModelOption.ALL_SENSORS) sensorAliases.put("all", "all");
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
					data.setupSensors(reset, sensorSettings); //Passes along variables
					DREAMWizard.visLauncher.setEnabled(true);
					for(String label: sensorData.keySet()){
						SensorData temp = sensorData.get(label);
						if(temp.isIncluded &&  data.getSet().getSensorSettings(label).isSet())
							temp.nodeLabel.setText(label + ": " + data.getSet().getSensorSettings(label).getValidNodes(null).size());
						else
							temp.nodeLabel.setText(label + ": Not set");
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}	       
		});
		
		for (int i=0; i<3; i++)
			new Label(parametersGroup, SWT.NULL);
		
		for(String label: sensorData.keySet()){
			SensorData temp = sensorData.get(label);
			temp.nodeLabel = new Label(parametersGroup, SWT.WRAP);
			if(data.getSet().getSensorSettings(label) == null)
				data.getSet().resetSensorSettings(label, temp.lowerThreshold, temp.upperThreshold);
			if( data.getSet().getSensorSettings(label).isSet())
				temp.nodeLabel.setText(label+ ": " + data.getSet().getSensorSettings(label).getValidNodes(null).size());
			else
				temp.nodeLabel.setText(label+ ": Not set");
		}
		
		// If the user has the E4D module installed, allow the E4D buttons to show up
		String e4dModuleDirectory = Constants.userDir + "//e4d";
		File e4dDirectory = new File(e4dModuleDirectory);
		if (e4dDirectory.exists()) {
			Group e4dGroup = new Group(container, SWT.SHADOW_NONE);
			e4dGroup.setText("E4D");
			e4dGroup.setFont(boldFontSmall);
			e4dGroup.setLayout(new GridLayout(4,false));
			e4dGroup.setLayoutData(tempData);
			
			//Add an info icon to explain the E4D Buttons
			Label infoLinkE4D = new Label(e4dGroup, SWT.NULL);
		  	infoLinkE4D.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
	  		infoLinkE4D.addListener(SWT.MouseUp, new Listener(){
	  			@Override
	  			public void handleEvent(Event event) {
	  				// TODO: Catherine edit text here!
	  				MessageDialog.openInformation(container.getShell(), "Additional information", "After finding triggering nodes, the user may write input files for the E4D model. E4D is a three-dimensional (3D) modeling and inversion code designed for subsurface imaging and monitoring using static and time-lapse 3D electrical resistivity (ER) or spectral induced polarization (SIP) data.");	
	  			}
	  		});
	  		
	  		// Save the E4D files
		    e4dButton = new Button(e4dGroup, SWT.PUSH);
		    e4dButton.setText("  Write E4D Files  ");
			e4dButton.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event arg0) {
					fixMacBug();
					
					// Begin by identifying the parameter to build the file from
					List<String> list = new ArrayList<String>();
					String selectedParameter = null;
					for(String label: data.getSet().getSensorSettings().keySet()) {
						if (label.contains("Pressure"))
							list.add(label);
					}
					if (list.size() == 1) { // If only one pressure parameter, use that
						selectedParameter = list.get(0);
					} else if (list.size() > 1) { // If more than one pressure parameter, open dialog for user to choose
						E4DDialog dialog = new E4DDialog(container.getShell(), list);
						dialog.open();
						selectedParameter = dialog.getParameter();
						if(dialog.getReturnCode() == 1) // If the dialog box is closed, do nothing
							return;
					} else if (list.isEmpty()) { // If no pressure parameters, throw error
						DREAMWizard.errorMessage.setText("No pressure parameter exists to create an E4D file.");
						return;
					}
					
					// Returns the best well that fall within the threshold (currently 30)
					ArrayList<Point3i> wells = null;
					try {
						wells = E4DSensors.calculateE4DWells(data, selectedParameter);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					// For this to be empty, no change was seen at any node with the selected parameter (very rare)
					if (wells.isEmpty()) {
						DREAMWizard.errorMessage.setText("No change was detected with the selected pressure parameter.");
						return;
					}
					
					// Now that we have our wells, print it out
					StringBuilder ijStringBuilder = new StringBuilder();
					for(Point3i well: wells)
						ijStringBuilder.append(Point3i.toCleanString(well) + "\n");
					File e4dWellFile = new File(Constants.userDir + "//e4d", "ertWellLocationsIJ_" + data.getSet().getScenarioEnsemble() + "_" + data.getSet().getAllScenarios().size() + ".txt");
					try{
						e4dWellFile.createNewFile();
						FileUtils.writeStringToFile(e4dWellFile, ijStringBuilder.toString());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});
		}

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
	} //ends load page

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
        sc.setExpandHorizontal(true);
        sc.getVerticalBar().setIncrement(20);
        sc.setExpandVertical(true);
                
        container = new Composite(sc, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 12;
		layout.verticalSpacing = 12;
		container.setLayout(layout);
		layout.numColumns = 9;
		
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		setControl(rootContainer);
		setPageComplete(true);
	}
	
	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
		Map<String, SensorData> sensorSettings = new HashMap<String, SensorData>();
		Map<String, String> sensorAliases = new HashMap<String, String>();
		ArrayList<String> sensors = new ArrayList<String>();
		if(data.modelOption == ModelOption.ALL_SENSORS) sensorAliases.put("all", "all");
		SensorSetting.sensorTypeToDataType = new HashMap<String, String>();
		int count = 0;
		for(String label: sensorData.keySet()) {
			SensorData senData = sensorData.get(label);
			if (senData.isIncluded==true) {
				count += data.getSet().getInferenceTest().getMinimumForType(label);
				sensors.add(label);
			}
			sensorSettings.put(label, senData);
			sensorAliases.put(label, senData.alias);
			SensorSetting.sensorTypeToDataType.put(label, sensorData.get(label).sensorType);
		}
		Sensor.sensorAliases = sensorAliases;
		data.getSet().setSensors(sensors);
		if(count>data.getSet().getInferenceTest().getOverallMinimum()) //Initially set this at the sum of sensors
			data.getSet().getInferenceTest().setOverallMinimum(count);
		data.setupSensors(false, sensorSettings);
		data.needToResetWells = true;
		volumeOfAquiferDegraded();
		DREAMWizard.visLauncher.setEnabled(true);
	}
	
	
	private void addSensor(String dataType, String newName){
		data.getSet().addSensorSetting(newName, dataType);
		data.getSet().getInferenceTest().setMinimumForType(newName, 1);
		sensorData.put(newName, new SensorData(data.getSet().getSensorSettings(newName), newName));
	}
	
	
	private void volumeOfAquiferDegraded(){	
		long current = System.currentTimeMillis();
		
		HashSet<Integer> nodes = new HashSet<Integer>();
		boolean foundNodes = false;
		for(String sensorType: data.getSet().getSensorSettings().keySet()){			
			nodes.addAll(data.getSet().getSensorSettings().get(sensorType).getValidNodes(null));
			System.out.println(sensorType + ": Number of nodes = " + nodes.size());
			if(nodes.size()!=0) //At least one node was found for a type
				foundNodes = true;
		}
		//If no nodes are found, we want to throw an error message to let them know why the page didn't advance
		if (foundNodes==false)
			errorFound(true, "  No nodes were found for the provided parameters.");
		
		Map<Scenario, HashMap<Integer, Float>> timeToDegradationPerNode = new HashMap<Scenario, HashMap<Integer, Float>>();
		
		for(Scenario scenario: data.getSet().getScenarios()){
			timeToDegradationPerNode.put(scenario, new HashMap<Integer, Float>());
			for(Integer nodeNumber: nodes){	
				Float timeToDegredation = null;
				for (TimeStep timeStep: data.getSet().getNodeStructure().getTimeSteps()){
					for(String sensorType: data.getSet().getSensorSettings().keySet()){
						try {
							if (sensorType.contains("Electrical Conductivity")) {
								if(E4DSensors.ertSensorTriggered(data.getSet(), timeStep, scenario, nodeNumber))
									timeToDegredation = timeStep.getRealTime();
							} else {
								if(SimulatedAnnealing.sensorTriggered(data.getSet(), timeStep, scenario, sensorType, nodeNumber))
									timeToDegredation = timeStep.getRealTime();
							}
						} catch (Exception e) {
							System.out.println("Unable to get time to degradation");
							e.printStackTrace();
						}
						if(timeToDegredation != null) break;
					}
					//Break after finding the earliest time where a detection occurred for any sensor
					if(timeToDegredation != null) break;
				}
				//Add the time to degradation for each node
				if(timeToDegredation != null) timeToDegradationPerNode.get(scenario).put(nodeNumber, timeToDegredation);
			}
		}
		
		System.out.println("Number of scenarios = " + timeToDegradationPerNode.size());
		
		Map<Scenario, HashMap<Float, Float>> volumeDegradedByYear = new HashMap<Scenario, HashMap<Float, Float>>();
		HashSet<Float> years = new HashSet<Float>();
		for(Scenario scenario: timeToDegradationPerNode.keySet()){
			volumeDegradedByYear.put(scenario, new HashMap<Float, Float>());
			for(Integer nodeNumber: timeToDegradationPerNode.get(scenario).keySet()){
				Float year = timeToDegradationPerNode.get(scenario).get(nodeNumber);
				years.add(year);
				Point3i location = data.getScenarioSet().getNodeStructure().getIJKFromNodeNumber(nodeNumber);
				if(!volumeDegradedByYear.get(scenario).containsKey(year))
					volumeDegradedByYear.get(scenario).put(year, data.getSet().getNodeStructure().getVolumeOfNode(location));
				else
					volumeDegradedByYear.get(scenario).put(year, volumeDegradedByYear.get(scenario).get(year) + data.getSet().getNodeStructure().getVolumeOfNode(location));
			}
		}
		
		System.out.println("Number of time steps = " + years.size());
		
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
	
	//Hack to fix a bug on mac that would replace the contents of whatever field was selected with the alias of the first selected monitoring parameter.
	//This gets around the problem by selecting that alias field so that it replaces itself - not a real fix to the problem.
	public void fixMacBug() {
		if(System.getProperty("os.name").contains("Mac")){
			for(String sensor : sensorData.keySet()){
				if(sensorData.get(sensor).isIncluded){
					sensorData.get(sensor).aliasText.setFocus();
					break;
				}
			}
		}
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