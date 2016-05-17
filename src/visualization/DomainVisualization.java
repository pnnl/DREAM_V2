package visualization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import objects.Configuration;
import objects.ExtendedConfiguration;
import objects.Scenario;
import objects.ScenarioSet;
import objects.Sensor;
import utilities.Constants;
import utilities.Point3f;
import utilities.Point3i;

public class DomainVisualization {

	private Shell shell;
	private Tree tree_configurationTree;
	private Table table_sensorTable;
	private DomainViewer domainViewer;

	private ScenarioSet set;

	private Button button_showMesh;
	private Button button_renderUniform;

	private Slider slider_scaleX;
	private Slider slider_scaleY;
	private Slider slider_scaleZ;

	private Slider slider_tickX;
	private Slider slider_tickY;
	private Slider slider_tickZ;
	
	private Text text_labelX;
	private Text text_labelY;
	private Text text_labelZ;
	
	private Map<String, SensorTableItem> sensorTableItems;
	private Map<Float, TreeDetectingPercentItem> configurations;
	
	public DomainVisualization(Display display, ScenarioSet set) {
		shell = new Shell(display, SWT.DIALOG_TRIM | SWT.MODELESS); 
		shell.setText("DREAM Visualization"); 
		shell.setLayout(new FillLayout());	
		this.set = set;

		// Main composite
		Composite composite = new Composite(shell, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		composite.setLayout(gridLayout);

		// GL canvas		
		domainViewer = new DomainViewer(display, composite, this);
		GridData visGridData = new GridData();
		visGridData.horizontalSpan = 2;
		visGridData.verticalSpan = 24;
		visGridData.widthHint = 680;
		visGridData.heightHint = 620;
		visGridData.grabExcessVerticalSpace = true;
		visGridData.grabExcessHorizontalSpace = true;
		domainViewer.setLayoutData(visGridData);		

		// Controls		
		Composite composite_scale = new Composite(composite, SWT.BORDER);
		GridLayout gridLayout_scale = new GridLayout();
		gridLayout_scale.numColumns = 3;
		composite_scale.setLayout(gridLayout_scale);
		
		Label label_controls = new Label(composite_scale, SWT.NONE);
		label_controls.setText("Controls");

		Label label_scaleX = new Label(composite_scale, SWT.NONE);
		label_scaleX.setText("Scale X");

		slider_scaleX = new Slider(composite_scale, SWT.NONE);
		slider_scaleX.setValues(50, 0, 100, 5, 5, 5);
		slider_scaleX.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Requires a reset
				domainViewer.reset();
				domainViewer.resetConfigurations();
			}
		});

		button_showMesh = new Button(composite_scale, SWT.CHECK);
		button_showMesh.setText("Show mesh");
		button_showMesh.setSelection(true);
		
		Label label_scaleY = new Label(composite_scale, SWT.NONE);
		label_scaleY.setText("Scale Y");

		slider_scaleY = new Slider(composite_scale, SWT.NONE);
		slider_scaleY.setValues(50, 0, 100, 5, 5, 5);
		slider_scaleY.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Requires a reset
				domainViewer.reset();
				domainViewer.resetConfigurations();
			}
		});
		
		button_renderUniform = new Button(composite_scale, SWT.CHECK);
		button_renderUniform.setText("Render uniform");
		button_renderUniform.setSelection(true);
		button_renderUniform.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Requires a reset
				domainViewer.reset();
				domainViewer.resetConfigurations();
			}
		});

		Label label_scaleZ = new Label(composite_scale, SWT.NONE);
		label_scaleZ.setText("Scale Z");

		slider_scaleZ = new Slider(composite_scale, SWT.NONE);
		slider_scaleZ.setValues(50, 0, 100, 5, 5, 5);
		slider_scaleZ.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Requires a reset
				domainViewer.reset();
				domainViewer.resetConfigurations();
			}
		});
		
		text_labelX = new Text(composite_scale, SWT.BORDER | SWT.SINGLE);
		text_labelX.setText("X Label                 ");	
		
		Label label_tickX = new Label(composite_scale, SWT.NONE);
		label_tickX.setText("Ticks");

		slider_tickX = new Slider(composite_scale, SWT.NONE);
		slider_tickX.setValues(5, 1, set.getNodeStructure().getX().size(), 5, 1, 5);
		
		text_labelY = new Text(composite_scale, SWT.BORDER | SWT.SINGLE);
		text_labelY.setText("Y Label                 ");

		Label label_tickY = new Label(composite_scale, SWT.NONE);
		label_tickY.setText("Ticks");

		slider_tickY = new Slider(composite_scale, SWT.NONE);
		slider_tickY.setValues(5, 1, set.getNodeStructure().getY().size(), 5, 1, 5);

		text_labelZ = new Text(composite_scale, SWT.BORDER | SWT.SINGLE);
		text_labelZ.setText("Z Label                 ");

		Label label_tickZ = new Label(composite_scale, SWT.NONE);
		label_tickZ.setText("Ticks");
		
		slider_tickZ = new Slider(composite_scale, SWT.NONE);
		slider_tickZ.setValues(5, 1, set.getNodeStructure().getZ().size(), 5, 1, 5);

		Label label_sensors = new Label(composite, SWT.NONE);
		label_sensors.setText("Solution Space");

		table_sensorTable = buildSensorTable(composite);
		GridData sensorTableGridData = new GridData();
		sensorTableGridData.verticalSpan = 10;
		sensorTableGridData.widthHint = 300;
		sensorTableGridData.heightHint = 160;

		table_sensorTable.setLayoutData(sensorTableGridData);	

		Label label_configurations = new Label(composite, SWT.NONE);
		label_configurations.setText("Configurations");

		// Tree
		tree_configurationTree = buildScenarioTree(composite);
		GridData scenarioTreeGridData = new GridData();
		scenarioTreeGridData.verticalSpan = 10;
		scenarioTreeGridData.widthHint = 300;
		scenarioTreeGridData.heightHint = 220;
		tree_configurationTree.setLayoutData(scenarioTreeGridData);	

		shell.pack();
		shell.open();

		shell.addListener(SWT.Close, new Listener() 
		{ 
			@Override 
			public void handleEvent(Event event) 
			{ 
				domainViewer.dispose();
				shell.dispose(); 
			} 
		}); 		
	}	
	
	
	public String getXLabel() {
		if(text_labelX == null)
			return "X";
		return text_labelX.getText();
	}
	
	public String getYLabel() {
		if(text_labelY == null)
			return "Y";
		return text_labelY.getText();
	}
	
	public String getZLabel() {
		if(text_labelZ == null)
			return "Z";
		return text_labelZ.getText();
	}

	public List<String> getAllSensorsToRender() {
		List<String> sensors = new ArrayList<String>();
		for(String key: sensorTableItems.keySet()) {
			sensors.add(key);
		}
		return sensors;
	}
	
	public boolean renderConfiguration(String uuid) {
		if(this.configurations == null)
			return false;
		synchronized(configurations) {
			for(TreeDetectingPercentItem configuration: this.configurations.values()) {
				for(TreeTTDItem ttdItem: configuration.children.values()) {
					for(TreeConfigItem configItem: ttdItem.children) {
						if(configItem.getUUID().equals(uuid)) {
							if(configItem.getTreeItem(null) != null)
								return configItem.getTreeItem(null).getChecked();
						}
					}
				}
			}
		}
		return false;
	}
	
	public List<Sensor> getSensorsInConfiguration(String uuid) {
		if(this.configurations == null)
			return new ArrayList<Sensor>();
		synchronized(configurations) {
			for(TreeDetectingPercentItem configuration: this.configurations.values()) {
				for(TreeTTDItem ttdItem: configuration.children.values()) {
					for(TreeConfigItem configItem: ttdItem.children) {
						if(configItem.getUUID().equals(uuid))
							return configItem.getConfiguration().getSensors();
					}
				}
			}
		}
		return new ArrayList<Sensor>();
	}
	
	public List<String> getAllConfigurationsToRender() {
		List<String> configurations = new ArrayList<String>();
		if(this.configurations == null)
			return configurations;
		for(TreeDetectingPercentItem configuration: this.configurations.values()) {
			for(TreeTTDItem ttdItem: configuration.children.values()) {
				for(TreeConfigItem configItem: ttdItem.children) {
					configurations.add(configItem.getUUID());
				}
			}
		}
		return configurations;		
	}
	
	public boolean renderSensor(String sensor) {
		if(sensorTableItems.containsKey(sensor))
			return sensorTableItems.get(sensor).getTableItem().getChecked();
		return false;
	}

	public List<Point3i> getCloudNodes(String sensor) {
		List<Point3i> cloudNodes = new ArrayList<Point3i>();
		Set<Integer> nodeNumbers = new HashSet<Integer>();
		if(set.getSensorSettings().containsKey(sensor))
			nodeNumbers.addAll(set.getSensorSettings().get(sensor).getValidNodes(null));
		for(Integer nodeNumber: nodeNumbers) {
			cloudNodes.add(set.getNodeStructure().getIJKFromNodeNumber(nodeNumber));
		}
		return cloudNodes;
	}

	public Point3i getColorOfSensor(String sensor) {
		Color color = sensorTableItems.get(sensor).getColor();
		return new Point3i(color.getRed(), color.getGreen(), color.getBlue());
	}

	public float getTransparency(String sensor) {
		return sensorTableItems.get(sensor).getTransparency();
	}

	public Point3i getMeshColor() {
		return new Point3i(120, 120, 120);
	}

	public boolean drawMesh() {
		return button_showMesh != null ? button_showMesh.getSelection() : true;
	}

	public float getScaleX() {
		return slider_scaleX != null ? slider_scaleX.getSelection()/50.0f : 1f;
	}

	public float getScaleY() {
		return slider_scaleY != null ? slider_scaleY.getSelection()/50.0f : 1f;
	}

	public float getScaleZ() {
		return slider_scaleZ != null ? slider_scaleZ.getSelection()/50.0f : 1f;
	}	

	public int getTickX() {
		return slider_tickX != null ? set.getNodeStructure().getX().size()/slider_tickX.getSelection() : 5;
	}

	public int getTickY() {
		return slider_tickY != null ? set.getNodeStructure().getY().size()/slider_tickY.getSelection() : 5;
	}

	public int getTickZ() {
		return slider_tickZ != null ? set.getNodeStructure().getZ().size()/slider_tickZ.getSelection() : 5;
	}	
	public boolean renderUniform() {
		return button_renderUniform != null ? button_renderUniform.getSelection() : false;
	}	

	private List<Float> getTrueCellBoundsX() {
		List<Float> xs = set.getNodeStructure().getX();
		List<Float> cellBoundsX = new ArrayList<Float>();		
		for(int x = 1; x < xs.size(); x++) {
			float half = (xs.get(x)-xs.get(x-1))/2;
			if(x == 1)
				cellBoundsX.add(new Float(xs.get(x-1)-half).floatValue());
			cellBoundsX.add(new Float(xs.get(x-1)+half).floatValue());
			if(x == xs.size()-1) 
				cellBoundsX.add(new Float(xs.get(x)+half).floatValue());
		}
		return cellBoundsX;
	}

	private List<Float> getTrueCellBoundsY() {
		List<Float> ys = set.getNodeStructure().getY();
		List<Float> cellBoundsY = new ArrayList<Float>();
		for(int y = 1; y < ys.size(); y++) {
			float half = (ys.get(y)-ys.get(y-1))/2;
			if(y == 1)
				cellBoundsY.add(new Float(ys.get(y-1)-half).floatValue());
			cellBoundsY.add(new Float(ys.get(y-1)+half).floatValue());
			if(y == ys.size()-1) 
				cellBoundsY.add(new Float(ys.get(y)+half).floatValue());
		}
		return cellBoundsY;
	}

	private List<Float> getTrueCellBoundsZ() {
		List<Float> zs = set.getNodeStructure().getZ();	
		List<Float> cellBoundsZ = new ArrayList<Float>();
		for(int z = 1; z < zs.size(); z++) {
			float half = (zs.get(z)-zs.get(z-1))/2;
			if(z == 1)
				cellBoundsZ.add(new Float(zs.get(z-1)-half).floatValue());
			cellBoundsZ.add(new Float(zs.get(z-1)+half).floatValue());
			if(z == zs.size()-1) 
				cellBoundsZ.add(new Float(zs.get(z)+half).floatValue());
		}
		return cellBoundsZ;
	}

	public List<Float> getRenderCellBoundsX() {
		List<Float> xs = getTrueCellBoundsX();
		List<Float> ys = getTrueCellBoundsY();
		List<Float> zs = getTrueCellBoundsZ();
		if(renderUniform()) {
			float deltaX = (xs.get(xs.size()-1) - xs.get(0))/xs.size();
			float deltaY = (ys.get(ys.size()-1) - ys.get(0))/ys.size();
			float deltaZ = (zs.get(zs.size()-1) - zs.get(0))/zs.size();
			float max = Math.max(Math.max(deltaX, deltaY), deltaZ);	
			// set them to the max delta
			List<Float> tempXs = new ArrayList<Float>();
			for(int i = 0; i < xs.size(); i++) {
				tempXs.add(i*max);
			}		
			xs = tempXs;
		}
		// scale	
		float scaleX = getScaleX();
		List<Float> tempXs = new ArrayList<Float>();
		for(int i = 0; i < xs.size(); i++) {
			tempXs.add(xs.get(i)*scaleX);
		}
		xs = tempXs;
		return xs;
	}

	public List<Float> getRenderCellBoundsY() {
		List<Float> xs = getTrueCellBoundsX();
		List<Float> ys = getTrueCellBoundsY();
		List<Float> zs = getTrueCellBoundsZ();
		if(renderUniform()) {
			float deltaX = (xs.get(xs.size()-1) - xs.get(0))/xs.size();
			float deltaY = (ys.get(ys.size()-1) - ys.get(0))/ys.size();
			float deltaZ = (zs.get(zs.size()-1) - zs.get(0))/zs.size();
			float max = Math.max(Math.max(deltaX, deltaY), deltaZ);	
			// set them to the max delta
			List<Float> tempYs = new ArrayList<Float>();
			for(int i = 0; i < ys.size(); i++) {
				tempYs.add(i*max);
			}
			ys = tempYs;
		}
		// scale	
		float scaleY = getScaleY();
		List<Float> tempYs = new ArrayList<Float>();
		for(int i = 0; i < ys.size(); i++) {
			tempYs.add(ys.get(i)*scaleY);
		}
		ys = tempYs;
		return ys;
	}

	public List<Float> getRenderCellBoundsZ() {
		List<Float> xs = getTrueCellBoundsX();
		List<Float> ys = getTrueCellBoundsY();
		List<Float> zs = getTrueCellBoundsZ();
		if(renderUniform()) {
			float deltaX = (xs.get(xs.size()-1) - xs.get(0))/xs.size();
			float deltaY = (ys.get(ys.size()-1) - ys.get(0))/ys.size();
			float deltaZ = (zs.get(zs.size()-1) - zs.get(0))/zs.size();
			float max = Math.max(Math.max(deltaX, deltaY), deltaZ);	
			// set them to the max delta
			List<Float> tempZs = new ArrayList<Float>();
			for(int i = 0; i < zs.size(); i++) {
				tempZs.add(i*max);
			}
			zs = tempZs;	
		}
		// scale	
		float scaleZ = getScaleZ();
		List<Float> tempZs = new ArrayList<Float>();
		for(int i = 0; i < zs.size(); i++) {
			tempZs.add(zs.get(i)*scaleZ);
		}
		zs = tempZs;	
		return zs;
	}

	public Point3f getRenderDistance() {
		List<Float> cellBoundsX = getRenderCellBoundsX();
		List<Float> cellBoundsY = getRenderCellBoundsY();
		List<Float> cellBoundsZ = getRenderCellBoundsZ();

		return new Point3f(
				cellBoundsX.get(cellBoundsX.size()-1), 
				cellBoundsY.get(cellBoundsY.size()-1), 
				cellBoundsZ.get(cellBoundsZ.size()-1));
	}

	private Table buildSensorTable(Composite composite) {
		final Table table = new Table(composite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		final int rowHeight = 24;

		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn tableColumn_sensorType = new TableColumn(table, SWT.CENTER);
		tableColumn_sensorType.setText("Include");

		TableColumn tableColumn_displayColor = new TableColumn(table, SWT.CENTER);
		tableColumn_displayColor.setText("Color");

		TableColumn tableColumn_transparency = new TableColumn(table, SWT.CENTER);
		tableColumn_transparency.setText("Transparency");

		TableColumn tableColumn_selectColor = new TableColumn(table, SWT.CENTER);
		tableColumn_selectColor.setText("");

		sensorTableItems = new HashMap<String, SensorTableItem>();
		for(String type: set.getDataTypes()) {
			SensorTableItem sensorTableItem = new SensorTableItem(table, rowHeight, type);
			sensorTableItems.put(type, sensorTableItem);
		}

		// resize the row height using a MeasureItem listener
		table.addListener(SWT.MeasureItem, new Listener() {
			public void handleEvent(Event event) {
				// height cannot be per row so simply set
				event.height = rowHeight; // lame
				if(event.index % 4 == 1)
					event.width = 40;
				if(event.index % 4 == 2)
					event.width = 40;
				if(event.index % 4 == 0) // label
					event.width = 100;
				if(event.index % 4 == 3) // button
					event.width = 60;
			}
		});

		table.addListener(SWT.EraseItem, new Listener() {
			// Copied from stack overflow, used to stop highlight color
			public void handleEvent(Event event) {
				// Selection:
				event.detail &= ~SWT.SELECTED;
				// Expect: selection now has no visual effect.
				// Actual: selection remains but changes from light blue to white.

				// MouseOver:
				event.detail &= ~SWT.HOT;
				// Expect: mouse over now has no visual effect.
				// Actual: behavior remains unchanged.

				GC gc = event.gc;
				TableItem item = (TableItem) event.item;
				gc.setBackground(item.getBackground(event.index));
				gc.fillRectangle(event.x, event.y, event.width, event.height);
			}
		});

		tableColumn_sensorType.pack();
		tableColumn_selectColor.pack();
		tableColumn_displayColor.pack();
		tableColumn_transparency.pack();

		return table;
	}
	
	public void addConfiguration(Configuration configuration) {
		if(configurations == null)
			configurations = new TreeMap<Float, TreeDetectingPercentItem>(Collections.reverseOrder());
	
		//float scenariosDetected = configuration.countScenariosDetected();
		//float totalScenarios = set.getScenarios().size();
		float globallyWeightedPercentage = 0;
		//float unweightedAverageTTD = configuration.getUnweightedTimeToDetectionInDetectingScenarios() / scenariosDetected;
		//float costOfConfig = set.costOfConfiguration(configuration);
		//float volumeDegraded = SensorSetting.getVolumeDegraded(configuration.getTimesToDetection(), set.getScenarios().size());
		float totalWeightsForDetectedScenarios = 0.0f;
		float weightedAverageTTD = 0.0f;

		for(Scenario scenario: set.getScenarios()) {
			if(configuration.getTimesToDetection().containsKey(scenario)) {	
				totalWeightsForDetectedScenarios += set.getScenarioWeights().get(scenario);
			}
		}	
		
		// If we want weighted, we need to weight based on the normalized value of just the detected scenarios
		// If we wanted weighted percentages, just add up the globally normalized value of detected scenarios
		for(Scenario detectingScenario: configuration.getTimesToDetection().keySet()) {
			float scenarioWeight = set.getScenarioWeights().get(detectingScenario);
			weightedAverageTTD += configuration.getTimesToDetection().get(detectingScenario) * (scenarioWeight/totalWeightsForDetectedScenarios);
			globallyWeightedPercentage += set.getGloballyNormalizedScenarioWeight(detectingScenario)*100;
		}
		
		// weighted or not?
		float ttd = weightedAverageTTD; // otherwise [unweightedAverageTTD]
		float percent = globallyWeightedPercentage; // otherwise [globallyWeightedPercentage]
			
		if(!configurations.containsKey(percent)) {
			configurations.put(percent, new TreeDetectingPercentItem(percent));
		}		
		if(!configurations.get(percent).children.containsKey(ttd)) {
			configurations.get(percent).children.put(ttd, new TreeTTDItem(ttd));
		}
		
		final TreeConfigItem treeItem = new TreeConfigItem(configuration);
		configurations.get(percent).children.get(ttd).addChild(treeItem);
			
		if(shell != null && !shell.isDisposed()) {
			shell.getDisplay().syncExec(new Runnable() {
				public void run() {
					if (shell != null && !shell.isDisposed()) {
						rebuildTree();
						treeItem.getTreeItem(null).setChecked(true);
					}
				}
			});
		}
		
	}
	
	public void clearConfiguration() {
		if(configurations != null) {
			configurations.clear();
		}			
		this.tree_configurationTree.clearAll(true);
	}
	
	public void rebuildTree() {
		if(configurations == null)
			return;
		for(TreeDetectingPercentItem percent: configurations.values())
			percent.clear();
		this.tree_configurationTree.removeAll();
		for(TreeDetectingPercentItem percent: configurations.values()) {
			TreeItem level1 = percent.getTreeItem(this.tree_configurationTree);
			for(TreeTTDItem ttd: percent.children.values()) {
				TreeItem level2 = ttd.getTreeItem(level1);
				for(TreeConfigItem child: ttd.children) {
					child.getTreeItem(level2);
				}
			}
		}
		domainViewer.resetConfigurations();
	}

	private Tree buildScenarioTree(Composite composite) {
		Tree tree = new Tree(composite, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		tree.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			//	domainViewer.resetConfigurations();
				if(e.detail != 32)
					return;
				for(TreeDetectingPercentItem percent: configurations.values()) {
					if(percent.getTreeItem(null) != null && percent.getTreeItem(null).equals(e.item)) {
						// select all children
						for(TreeTTDItem ttd: percent.children.values()) {
							if(ttd.getTreeItem(null) != null) {
								ttd.getTreeItem(null).setChecked(percent.getTreeItem(null).getChecked());
								for(TreeConfigItem child: ttd.children) {
									if(child.getTreeItem(null) != null) 
										child.getTreeItem(null).setChecked(percent.getTreeItem(null).getChecked());
								}
							}
						}
						return;
					} // check the children
					for(TreeTTDItem ttd: percent.children.values()) {
						if(ttd.getTreeItem(null) != null && ttd.getTreeItem(null).equals(e.item)) {
							for(TreeConfigItem child: ttd.children) {
								if(child.getTreeItem(null) != null) 
									child.getTreeItem(null).setChecked(ttd.getTreeItem(null).getChecked());
							}
							return;
						}
					}					
				} 
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}
			
		});
		tree.pack();
		return tree;
	}
	
	private class TreeDetectingPercentItem {
		private float detectingScenarios;
		private TreeItem treeItem;
		private Map<Float, TreeTTDItem> children;
		public TreeDetectingPercentItem(float detectingScenarios) {
			this.detectingScenarios = detectingScenarios;
			children = new TreeMap<Float, TreeTTDItem>();
		}
		public TreeItem getTreeItem(Tree tree) {
			if(treeItem == null) {
				treeItem = new TreeItem(tree, SWT.NONE);
				treeItem.setText(Constants.percentageFormat.format(detectingScenarios) + "% Scenarios Detected");	
			}
			return treeItem;
		}
		public void clear() {
			treeItem = null;
			for(TreeTTDItem child: children.values())
				child.clear();
		}
	}

	private class TreeTTDItem {
		private float ttd;
		private TreeItem treeItem;
		private List<TreeConfigItem> children;
		public TreeTTDItem(float ttd) {
			this.ttd = ttd;
			children = new ArrayList<TreeConfigItem>();
		}		
		public TreeItem getTreeItem(TreeItem parent) {
			if(treeItem == null) {
				treeItem = new TreeItem(parent, SWT.NONE);
				treeItem.setText("TTD:" + Constants.decimalFormat.format(ttd));
			}
			return treeItem;
		}
		public void clear() {
			treeItem = null;
			for(TreeConfigItem child: children)
				child.clear();
		}
		public void addChild(TreeConfigItem child) {
			children.add(child);
		}
	}
	
	private class TreeConfigItem {
	
		private String uuid;
		private String name;
		private TreeItem treeItem;
		private Configuration configuration;
		public TreeConfigItem(Configuration configuration) {
			String name = configuration.toString();
			if(configuration instanceof ExtendedConfiguration) {
				name = ((ExtendedConfiguration)configuration).getSummary();
			}
			this.configuration = ((ExtendedConfiguration)configuration).makeCopy(set);
			this.name = name;
			this.uuid = UUID.randomUUID().toString();
		}
		
		private Configuration getConfiguration() {
			return configuration;
		}
		
		public String getUUID() {
			return uuid;
		}
		
		public TreeItem getTreeItem(TreeItem parent) {
			if(parent != null && treeItem == null) {
				treeItem = new TreeItem(parent, SWT.NONE);
				treeItem.setText(name);
			}
			return treeItem;
		}
		public void clear() {
			treeItem = null;
		}
	}
	
	private class SensorTableItem {

		private float transparency = 0;
		private Color color;
		
		private TableItem tableItem;

		public SensorTableItem(Table table, int rowHeight, String sensorType) {
			tableItem = new TableItem(table, SWT.CENTER);
			tableItem.setText(sensorType);
			tableItem.setChecked(true);
			tableItem.setText(0, Sensor.sensorAliases.get(sensorType));
			int itemCount = table.getItemCount();
			float[][] colors = new float[][]{
				{1.0f, 0.0f, 0.0f, 0.8f}, 
				{0.0f, 0.0f, 1.0f, 0.8f}, 
				{0.0f, 1.0f, 0.0f, 0.03f}, 
				{0.0f, 1.0f, 1.0f, 0.03f}, 
				{1.0f, 1.0f, 0.8f, 0.03f}};
			float[] sensorColor = colors[itemCount%5];
			color = new Color(shell.getDisplay(), (int)sensorColor[0]*255, (int)sensorColor[1]*255, (int)sensorColor[2]*255);
			tableItem.setBackground(1, color);
			TableEditor editor = new TableEditor(table);
			Button button = new Button(table, SWT.PUSH | SWT.FLAT | SWT.CENTER);	
			button.setText("Select...");			
			button.computeSize(SWT.DEFAULT, rowHeight);
			editor.grabHorizontal = true;
			editor.grabVertical = true;
			editor.verticalAlignment = SWT.BOTTOM;
			editor.minimumHeight = rowHeight;
			editor.setEditor(button, tableItem, 3);
			final Slider slider = new Slider(table, SWT.NONE);
			slider.setMaximum(255);
			this.transparency = (60.0f/255.0f);
			slider.setSelection(60);
			slider.computeSize(SWT.DEFAULT, rowHeight);
			slider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					transparency = (slider.getSelection()/255.0f);
					domainViewer.reset();
				}
			});
			TableEditor editor2 = new TableEditor(table);
			editor2.grabHorizontal = true;
			editor2.grabVertical = true;
			editor2.verticalAlignment = SWT.BOTTOM;
			editor2.minimumHeight = rowHeight;
			editor2.setEditor(slider, tableItem, 2);	
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					ColorDialog dialog = new ColorDialog(shell);
					RGB rgb = dialog.open();
					if (rgb != null) {
						color = new Color(shell.getDisplay(), rgb);
						tableItem.setBackground(1, color);
						// Color change requires reset
						domainViewer.reset();
						domainViewer.resetConfigurations();						
					}
				}
			});   
		}

		public float getTransparency() {
			return transparency;
		}
		
		public TableItem getTableItem() {
			return tableItem;
		}

		public Color getColor() {
			return color;
		}
	}

	public static void main(String[] args) {
		new DomainVisualization(new Display(), null);
	}

}
