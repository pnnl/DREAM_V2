package wizardPages;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import objects.ExtendedConfiguration;
import objects.ExtendedSensor;
import objects.Sensor;
import utilities.Constants;
import utilities.Point3d;
import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;

public class Page_ReviewAndRun extends WizardPage implements AbstractWizardPage {

	private STORMData data;
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private Text outputFolder;
	private Text runs;
	private Text samples;
	private Text iterations;

	private boolean isCurrentPage = false;

	protected Page_ReviewAndRun(STORMData data) {
		super("Review");
		//	setDescription("Review");
		this.data = data;			
	}

	@Override
	public void createControl(Composite parent) {
		rootContainer = new Composite(parent, SWT.NULL);
		rootContainer.setLayout(GridLayoutFactory.fillDefaults().create());

		sc = new ScrolledComposite(rootContainer, SWT.V_SCROLL);
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
		for(Control control: container.getChildren()) {
			control.dispose(); // Remove the children.
		}
		container.layout();	


		Text summary = new Text(container, SWT.MULTI | SWT.WRAP| SWT.BORDER | SWT.V_SCROLL );
		summary.setEditable(false);
		GridData summaryGD = new GridData(GridData.FILL_BOTH);
		summaryGD.verticalSpan = 10;
		summaryGD.widthHint = 260;
		summaryGD.grabExcessVerticalSpace = true;
		summary.setText(data.getSet().toString());
		summary.setLayoutData(summaryGD);

		final DirectoryDialog directoryDialog = new DirectoryDialog(container.getShell());
		Button buttonSelectDir = new Button(container, SWT.PUSH);
		buttonSelectDir.setText("Select an output directory");
		buttonSelectDir.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				directoryDialog.setFilterPath(outputFolder.getText());
				directoryDialog.setMessage("Please select a directory and click OK");
				String dir = directoryDialog.open();
				if (dir != null) {
					outputFolder.setText(dir);
				}
			}
		});
		new Label(container, SWT.NULL); // Spacer

		outputFolder= new Text(container, SWT.BORDER | SWT.SINGLE);
		outputFolder.setText(new File(new File(Constants.homeDirectory).getParent(), "_results").getAbsolutePath());
		GridData costGD = new GridData(GridData.FILL_HORIZONTAL);
		costGD.horizontalSpan = 2;
		outputFolder.setLayoutData(costGD);

		Button button = new Button(container, SWT.BALLOON);
		button.setSelection(true);
		button.setText("Run Iterative Procedure");

		//Label temp2 = new Label(container, SWT.NULL);
		runs= new Text(container, SWT.BORDER | SWT.SINGLE);
		runs.setText("1");		
		GridData runsGD = new GridData(GridData.FILL_HORIZONTAL);
		runs.setLayoutData(runsGD);

		Label iterationLabel = new Label(container, SWT.NULL);
		iterationLabel.setText("Configurations to test");
		iterations = new Text(container, SWT.BORDER | SWT.SINGLE);
		iterations.setText(String.valueOf(data.getSet().getIterations()));
		GridData iterationGD = new GridData(GridData.FILL_HORIZONTAL);
		iterations.setLayoutData(iterationGD);

		button.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				String numRuns = runs.getText();
				int runs = numRuns.isEmpty() ? 1 : Integer.parseInt(numRuns);	
				int ittr = Integer.parseInt(iterations.getText());
				data.setWorkingDirectory(outputFolder.getText());
				data.getSet().setIterations(ittr);
				try {
					long startTime = System.currentTimeMillis();
					data.run(runs);
					System.out.println("Iterative procedure took: " + (System.currentTimeMillis() - startTime) + "ms");
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}	       
		});


		Button button3 = new Button(container, SWT.BALLOON);
		button3.setSelection(true);
		button3.setText(" Run Full Enumeration  ");

		button3.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				data.setWorkingDirectory(outputFolder.getText());
				// TODO: Calculate iterations here
				try {
					data.runEnumeration();	
				} catch (Exception e) {
					e.printStackTrace();
				}
			}	       
		});

		Button button4 = new Button(container, SWT.BALLOON);
		button4.setSelection(true);
		button4.setText("IJK to XYZ");
		button4.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				data.setWorkingDirectory(outputFolder.getText());
				FileDialog dialog = new FileDialog(container.getShell(), SWT.NULL);
				String path = dialog.open();
				if (path != null) {
					try {
						convertFile(new File(path));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}	       
		});

		//	Label temp = new Label(container, SWT.NULL);
		//	Label temp3 = new Label(container, SWT.NULL);
		Button button2 = new Button(container, SWT.BALLOON);
		button2.setSelection(true);
		button2.setText("  Run Random Sample   ");
		samples= new Text(container, SWT.BORDER | SWT.SINGLE);
		samples.setText("20");
		button2.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				int numSamples = Integer.parseInt(samples.getText());
				data.setWorkingDirectory(outputFolder.getText());
				try {
					data.randomEnumeration(numSamples);
				} catch (Exception e) {

				}
			}	       
		});		

		Button cloudButton = new Button(container, SWT.BALLOON);
		cloudButton.setSelection(true);
		cloudButton.setText("Cloud data");
		cloudButton.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				JFrame temp = new JFrame();
				JTextArea textArea = new JTextArea();

				String text = "TITLE = Cloud nodes\n";
				text += "VARIABLES  = \"X, m\"\t\"Y, m\"\t\"Z, m\"";
				for(String sensorType: data.getSet().getSensorSettings().keySet()) {
					text += "\t\"" + sensorType + "\"";
				}
				Point3i ijk = data.getSet().getNodeStructure().getIJKDimensions();
				int numNodes = (ijk.getI()) * (ijk.getJ()) * (ijk.getK());
				int numElements = 8*numNodes;
				text += "\n";
				text += "ZONE NODES = " + numNodes + ", ELEMENTS = " + numElements + ", DATAPACKING = BLOCK, ZONETYPE = FEBRICK\n";
				
				String varRanks = "[1, 2, 3, ";
				int rank = 4;
				for(String sensorType: data.getSet().getSensorSettings().keySet()) {
					varRanks += (rank++) + ", ";
				}
				varRanks = varRanks.substring(0, varRanks.length() - 2) + "]";
				text += "VARLOCATION = (" + varRanks + " = NODAL)\n";

				// X values
				for(int k = 0; k < ijk.getK(); k++) { 			
					for(int j = 0; j < ijk.getJ(); j++) { 
						float prevValue = 0.0f; // Assume center is at 0	
						for(int i = 0; i < ijk.getI(); i++) { 
							float nextValue = data.getSet().getNodeStructure().getX().get(i);	
							float var0f = prevValue;
							float var1f = prevValue + ((nextValue-prevValue)*2);
							prevValue = var1f;	
							String var0 = utilities.Constants.exponentialFormat.format(var0f);
							String var1 = utilities.Constants.exponentialFormat.format(var0f);							
							text += var0 + " " + var1 + " " + var0 + " " + var1 + " " + var0 + " " + var1 + " " + var0 + " " + var1 + "\n";	
						}
					}
				}
				text += "\n";
				// Y values	
				for(int k = 0; k < ijk.getK(); k++) { 
					float prevValue = 0.0f; // Assume center is at 0
					for(int j = 0; j < ijk.getJ(); j++) {
						float nextValue = data.getSet().getNodeStructure().getY().get(j);					
						float var0f = prevValue;
						float var1f = prevValue + ((nextValue-prevValue)*2);
						prevValue = var1f;	
						String var0 = utilities.Constants.exponentialFormat.format(var0f);
						String var1 = utilities.Constants.exponentialFormat.format(var0f);
						for(int i = 0; i < ijk.getI(); i++) { 
							text += var0 + " " + var0 + " " + var1 + " " + var1 + " " + var0 + " " + var0 + " " + var1 + " " + var1 + "\n";	
						}
					}
				}
				text += "\n";
				// Z values
				float prevValue = 0.0f; // Assume center is at 0				
				for(int k = 0; k < ijk.getK(); k++) { 
					float nextValue = data.getSet().getNodeStructure().getZ().get(k);					
					float var0f = prevValue;
					float var1f = prevValue + ((nextValue-prevValue)*2);
					prevValue = var1f;
					String var0 = utilities.Constants.exponentialFormat.format(var0f);
					String var1 = utilities.Constants.exponentialFormat.format(var0f);	
					for(int j = 0; j < ijk.getJ(); j++) {		
						for(int i = 0; i < ijk.getI(); i++) {				
							text += var0 + " " + var0 + " " + var0 + " " + var0 + " " + var1 + " " + var1 + " " + var1 + " " + var1 + "\n";	
						}
					}
				}
				text += "\n";
				
				// Variables
				for(String sensorType: data.getSet().getSensorSettings().keySet()) {	
					for(int k = 1; k <= ijk.getK(); k++) { for(int j = 1; j <= ijk.getJ(); j++) { for(int i = 1; i <= ijk.getI(); i++) { 
						int nodeId = data.getSet().getNodeStructure().getNodeNumber(new Point3i(i, j, k));
						String var0 = (data.getSet().getSensorSettings().get(sensorType).getValidNodes().contains(nodeId) ? "1" : "0");		
						text += var0 + " " + var0 + " " + var0 + " " + var0 + " " + var0 + " " + var0 + " " + var0 + " " + var0 + "\n";	
					}}}
					text += "\n";
				}
				text += "\n";
				// Connection list
				for(int k = 1; k <= ijk.getK(); k++) { for(int j = 1; j <= ijk.getJ(); j++) { for(int i = 1; i <= ijk.getI(); i++) { 
					int nodeId = data.getSet().getNodeStructure().getNodeNumber(new Point3i(i,   j,   k));
					int var1, var2, var3, var4, var5, var6, var7, var8;
					var1 = (nodeId-1)*8+1;
					var2 = (nodeId-1)*8+2;
					var3 = (nodeId-1)*8+3;
					var4 = (nodeId-1)*8+4;
					var5 = (nodeId-1)*8+5;
					var6 = (nodeId-1)*8+6;
					var7 = (nodeId-1)*8+7;
					var8 = (nodeId-1)*8+8;
					if(i != 1) {
						nodeId = data.getSet().getNodeStructure().getNodeNumber(new Point3i(i-1, j, k));
						var1 = (nodeId-1)*8+2;
						var3 = (nodeId-1)*8+4;
						var5 = (nodeId-1)*8+6;
						var7 = (nodeId-1)*8+8;
					}
					if(j != 1) {
						nodeId = data.getSet().getNodeStructure().getNodeNumber(new Point3i(i, j-1, k));
						var1 = (nodeId-1)*8+3;
						var2 = (nodeId-1)*8+4;
						var5 = (nodeId-1)*8+7;
						var6 = (nodeId-1)*8+8;
					}
					if(k != 1) {
						nodeId = data.getSet().getNodeStructure().getNodeNumber(new Point3i(i, j, k-1));
						var1 = (nodeId-1)*8+5;
						var2 = (nodeId-1)*8+6;
						var3 = (nodeId-1)*8+7;
						var4 = (nodeId-1)*8+8;
					}
					text += var1 + " " + var2 + " " + var4 + " " + var3 + " " + var5 + " " + var6 + " " + var8 + " " + var7 + "\n";
					
				}}}
				
				text += "\n";
				
				textArea.setText(text);
				temp.add(new JScrollPane(textArea));
				temp.setSize(400, 400);
				temp.setVisible(true);
			}	       
		});		
		

		Button bestTTDButton = new Button(container, SWT.BALLOON);
		bestTTDButton.setSelection(true);
		bestTTDButton.setText("Best TTD");
		bestTTDButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				ExtendedConfiguration configuration = new ExtendedConfiguration();
				for(String sensorType: data.getSet().getSensorSettings().keySet()) {	
					for(int nodeId: data.getSet().getSensorSettings().get(sensorType).getValidNodes()) {
						configuration.addSensor(new ExtendedSensor(nodeId, sensorType, data.getSet().getNodeStructure()));
					}
				}
				JOptionPane.showMessageDialog(null, "Best TTD: " + data.runObjective(configuration));				
			}	       
		});		

		GridData sampleGD = new GridData(GridData.FILL_HORIZONTAL);
		samples.setLayoutData(sampleGD);

		container.layout();	
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();	

	}

	public void convertFile(File file) throws IOException {
		/*
		System.out.println("i, x edge, x center");
		for(int i = 1; i <= data.getSet().getNodeStructure().getIJKDimensions().getI(); i++) {
			Point3i node = data.getSet().getNodeStructure().getIJKFromNodeNumber(data.getSet().getNodeStructure().getNodeNumber(i, 1, 1));
			double edge = data.getSet().getNodeStructure().getXYZFromIJK(node).getX();
			double center = data.getSet().getNodeStructure().getNodeCenteredXYZFromIJK(node).getX();
			System.out.println(i + ", " + edge + ", " + center);
		}
		System.out.println("\n\nj, y edge, y center");
		for(int j = 1; j <= data.getSet().getNodeStructure().getIJKDimensions().getJ(); j++) {
			Point3i node = data.getSet().getNodeStructure().getIJKFromNodeNumber(data.getSet().getNodeStructure().getNodeNumber(1, j, 1));
			double edge = data.getSet().getNodeStructure().getXYZFromIJK(node).getY();
			double center = data.getSet().getNodeStructure().getNodeCenteredXYZFromIJK(node).getY();
			System.out.println(j + ", " + edge + ", " + center);
		}
		System.out.println("\n\nk, z edge, z center");
		for(int k = 1; k <= data.getSet().getNodeStructure().getIJKDimensions().getK(); k++) {
			Point3i node = data.getSet().getNodeStructure().getIJKFromNodeNumber(data.getSet().getNodeStructure().getNodeNumber(1, 1, k));
			double edge = data.getSet().getNodeStructure().getXYZFromIJK(node).getZ();
			double center = data.getSet().getNodeStructure().getNodeCenteredXYZFromIJK(node).getZ();
			System.out.println(k + ", " + edge + ", " + center);
		}
		 */
		List<String> lines = FileUtils.readLines(file);
		StringBuffer fileOut = new StringBuffer();
		for(String line: lines) {
			Map<String, String> nodesToReplace = new HashMap<String, String>();
			// If the line contains any node ids, we need to convert them to xyz locations
			String[] groups = line.split("\\(");
			for(String group: groups) {
				String[] individualSensors = group.split(",");
				for(String individualSensor: individualSensors) {
					String[] parts = individualSensor.split(":");
					if(parts.length == 3) {
						int nodeID = Integer.parseInt(parts[0].trim());
						Point3d xyz = data.getSet().getNodeStructure().getXYZFromIJK(data.getSet().getNodeStructure().getIJKFromNodeNumber(nodeID));
						nodesToReplace.put(parts[0], xyz.toString());
					} 
				}
			}
			String lineOut = line;
			for(String nodeToReplace: nodesToReplace.keySet()) {
				lineOut = lineOut.replaceAll(nodeToReplace, nodesToReplace.get(nodeToReplace));
			}
			fileOut.append(lineOut + "\n");
		}
		System.out.println(fileOut.toString());
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
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
