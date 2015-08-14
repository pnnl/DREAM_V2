package wizardPages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import objects.SensorSetting;
import objects.SensorSetting.Trigger;

import org.eclipse.jface.layout.GridDataFactory;
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
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import utilities.Constants;
import wizardPages.DREAMWizard.STORMData;

public class Page_SensorSetup extends WizardPage implements AbstractWizardPage {
	
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	
	private STORMData data;
	private boolean isCurrentPage = false;
	private Button scenarioUnionButton;
	private Button sensorUnionButton;
	private boolean toggling = false;
	
	private Map<String, SensorData> sensorData;
		
	protected Page_SensorSetup(STORMData data) {
		super("Sensors");
		this.data = data;	
	}
	
	public class SensorData {
				
		public String sensorType;		
		public boolean isIncluded;
		public float cost;
		public Trigger trigger;
		
		private float dataMin;
		private float dataMax;
		
		public float min;
		public float max;
		
		public boolean asRelativeChange;
		public boolean asAbsoluteChange;
		
		// A couple of these need to be global
		private Label sensorTypeLabel;
		private Text costText;
		private Combo thresholdCombo;
		private Label valueLabel;
		private Text valueInput;
		
		private Label nodeLabel;
		private boolean hasErrors;	
		
		public SensorData(SensorSetting sensorSettings) {
			
			sensorType = sensorSettings.getType();			
			isIncluded = false; // By default	
			cost = sensorSettings.getCost();
			
			// These may be backwards?
			trigger = Trigger.MINIMUM_THRESHOLD;
			
			// Try to be a little smarter about pressure
			if(sensorType.toLowerCase().contains("pressure"))
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
				maxStr =  Constants.decimalFormat.format(dataMax);
		
			if(minStr.length() > 8) minStr = Constants.exponentialFormat.format(dataMin);
			if(maxStr.length() > 8) maxStr = Constants.exponentialFormat.format(dataMax);			
			
			includeButton.setText(sensorType);
			
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
				if(max != 0) // What if a user wants to set this to 0?
					valueInput.setText(Constants.decimalFormat.format(max));
			} else { 
				if(min != 0)
					valueInput.setText(Constants.decimalFormat.format(min));
			}
			
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
						
			toggleEnabled();
		}		
		
		private void toggleEnabled() {
			if(sensorTypeLabel != null && !sensorTypeLabel.isDisposed())
				sensorTypeLabel.setEnabled(isIncluded);
			if(costText != null && !costText.isDisposed())
				costText.setEnabled(isIncluded);
			if(thresholdCombo != null && !thresholdCombo.isDisposed())
				thresholdCombo.setEnabled(isIncluded);					
			if(valueInput != null && !valueInput.isDisposed())
				valueInput.setEnabled(isIncluded);
			if(valueLabel != null && !valueLabel.isDisposed())
				valueLabel.setEnabled(isIncluded);
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
		layout.numColumns = 4;
		
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		setControl(rootContainer);
		setPageComplete(true);
	}
	
	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;		
		Map<String, SensorData> sensorSettings = new HashMap<String, SensorData>();		
		for(SensorData data: sensorData.values()) {
			String dataType = data.sensorType;
			sensorSettings.put(dataType, data);
		}	
		data.setupSensors(false, sensorSettings);		
	}

	@Override
	public void loadPage() {
		isCurrentPage = true;
				
		if(sensorData == null) {
			// New UI
			sensorData = new TreeMap<String, SensorData>();
		
			for(String dataType: data.getSet().getAllPossibleDataTypes()) {
								
				sensorData.put(dataType, new SensorData(data.getSet().getSensorSettings(dataType)));
				
			}		
		}
		
		for(Control control: container.getChildren()) {
			control.dispose(); // Remove the children.
		}		
		Font boldFont = new Font( container.getDisplay(), new FontData( "Arial", 10, SWT.BOLD ) );

		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Leakage Criteria");
		GridData infoGridData1 = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData1.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData1.verticalSpan = 4;
		infoLabel1.setLayoutData(infoGridData1);
		
		infoLabel1.setFont(boldFont);
		
		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("Select the monitoring parameters of interest, include a cost per appropriate sensor type, and set the detection criteria. NOTE: The minimum and maximum values are read from the first realization read by DREAM. These are provided to give the user an idea of the values present.");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 2;
		infoGridData.widthHint = 200;
		infoLabel.setLayoutData(infoGridData);
				
		// Headers
		Label monitorParams = new Label(container, SWT.LEFT);
		Label costPerSensor = new Label(container, SWT.LEFT);
		Label detectionCriteria = new Label(container, SWT.LEFT);
		Label valueLabel = new Label(container, SWT.LEFT);
		
		monitorParams.setText("Monitoring Parameter");		
		costPerSensor.setText("Cost per Sensor");
		detectionCriteria.setText("Detection Criteria");
		valueLabel.setText("Value");
		
		monitorParams.setFont(boldFont);
		costPerSensor.setFont(boldFont);
		detectionCriteria.setFont(boldFont);
		valueLabel.setFont(boldFont);
				
		for(SensorData data: sensorData.values()) {
			data.buildUI();
		}

		Button queryButton = new Button(container, SWT.BALLOON);
		queryButton.setText("Find triggering nodes");

		new Label(container, SWT.NULL);
		new Label(container, SWT.NULL);
		new Label(container, SWT.NULL);
		
		int count = 0;
		for(SensorData temp: sensorData.values()) {		
			// Let these fill two spots
			GridData tempData = new GridData(GridData.FILL_HORIZONTAL);
			tempData.horizontalSpan = 2;
			temp.nodeLabel = new Label(container, SWT.WRAP);
			temp.nodeLabel.setLayoutData(tempData);
			if(data.getSet().getSensorSettings(temp.sensorType) == null)
				data.getSet().resetSensorSettings(temp.sensorType, temp.min, temp.max);
			if( data.getSet().getSensorSettings(temp.sensorType).isSet())
				temp.nodeLabel.setText(temp.sensorType+ ": " + data.getSet().getSensorSettings(temp.sensorType).getValidNodes().size());
			else 
				temp.nodeLabel.setText(temp.sensorType+ ": Not set");
			count+=2;
		}
		
		if(count % 4 != 0) {
			for(int i = 0; i < 4-(count % 4); i++)
				new Label(container, SWT.NULL);
		}
		
		queryButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				
				Constants.hdf5CloudData.clear();
				Constants.scenarioUnion = scenarioUnionButton.getSelection();
				boolean reset = true;							
				Map<String, SensorData> sensorSettings = new HashMap<String, SensorData>();	
				
				for(SensorData data: sensorData.values()) {
					String dataType = data.sensorType;
					sensorSettings.put(dataType, data);
				}	
				
				try {
					if(reset) {
						for(String dataType: data.getSet().getDataTypes()) {
							data.getSet().getSensorSettings(dataType).setNodesReady(false);
						}
					}
					data.setupSensors(reset, sensorSettings);
					for(SensorData temp: sensorData.values()) {
						if(temp.isIncluded &&  data.getSet().getSensorSettings(temp.sensorType).isSet())
							temp.nodeLabel.setText(temp.sensorType+ ": " + data.getSet().getSensorSettings(temp.sensorType).getValidNodes().size());
						else
							temp.nodeLabel.setText(temp.sensorType+ ": Not set");
					}			
					
					// Then we really only want the intersection
					if(!sensorUnionButton.getSelection()) {
						List<Integer> intersection = new ArrayList<Integer>();
						for(String dataType: data.getSet().getDataTypes()) {
							if(!data.getSet().getSensorSettings(dataType).isSet())
								continue;
							for(Integer nodeId: data.getSet().getSensorSettings(dataType).getValidNodes()) {
								boolean shared = true;
								for(String otherDataType: data.getSet().getDataTypes()) {
									if(dataType.equals(otherDataType))
										continue; // Don't need to check this one
									if(!data.getSet().getSensorSettings(otherDataType).isSet())
										continue;
									if(!data.getSet().getSensorSettings(otherDataType).getValidNodes().contains(nodeId)) {
										shared = false;
									}
									if(!shared)
										break;
								}		
								if(shared)
									intersection.add(nodeId);
							}
						}
						System.out.println("Intersection: (" + intersection.size() + ") " + intersection);
						for(SensorData temp: sensorData.values()) {
							data.getSet().getSensorSettings(temp.sensorType).setValidNodes(intersection);
							if(data.getSet().getSensorSettings(temp.sensorType).isSet())
								temp.nodeLabel.setText(temp.sensorType+ ": " + data.getSet().getSensorSettings(temp.sensorType).getValidNodes().size());
							else
								temp.nodeLabel.setText(temp.sensorType+ ": Not set");
						}
					}
										
					
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}	       
		});	
				
		Label col = new Label(container, SWT.NULL);
		col.setText("Set up the solution space using ...");
		new Label(container, SWT.NULL);
		new Label(container, SWT.NULL);
		new Label(container, SWT.NULL);
		
	    scenarioUnionButton = new Button(container, SWT.CHECK);
	    final Button scenarioIntersectionButton = new Button(container, SWT.CHECK);
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
	    
	    sensorUnionButton = new Button(container, SWT.CHECK);
	    final Button sensorIntersectionButton = new Button(container, SWT.CHECK);
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
