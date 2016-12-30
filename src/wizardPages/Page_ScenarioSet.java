package wizardPages;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import functions.CCS9_1;
import functions.Function;
import functions.MutationFunction.MUTATE;
import utilities.Constants;
import utilities.Constants.ModelOption;
import utilities.PorosityDialog;
import wizardPages.DREAMWizard.STORMData;

public class Page_ScenarioSet extends WizardPage implements AbstractWizardPage {

	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private Combo scenarioSet;
	private String simulation = "CCS9.1";
	private String modelOption = Constants.ModelOption.INDIVIDUAL_SENSORS_2.toString();
	private STORMData data;
	private Text hdf5Text;
	private boolean isCurrentPage = false;
	private Label modelDescription;
	
	protected Page_ScenarioSet(STORMData data) {
		super("STORM");
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
		layout.numColumns = 2;
		container.setLayout(layout);

		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		setControl(rootContainer);
		setPageComplete(true);
	}

	public String getScenario() {
		return scenarioSet.getText();
	}

	public String getSimulation() {
		return simulation;
	}

	public String getModelOption() {
		//Change this back if we are going back to drop-downs
		//return modelOption.getText();
		return modelOption;
	}

	public void completePage() throws Exception {	
		isCurrentPage = false;
		
		ModelOption modelOption = ModelOption.INDIVIDUAL_SENSORS_2;
		String strOption = getModelOption();
		if(strOption.equals(ModelOption.INDIVIDUAL_SENSORS_2.toString()))
			modelOption = ModelOption.INDIVIDUAL_SENSORS_2;
		else if(strOption.equals(ModelOption.ALL_SENSORS.toString()))
			modelOption = ModelOption.ALL_SENSORS;
		
		data.setupScenarioSet(modelOption, getModelOption().toLowerCase().contains("sensor") ? MUTATE.SENSOR : MUTATE.WELL, getSimulation(), hdf5Text.getText());
		if(!data.getScenarioSet().getNodeStructure().porosityOfNodeIsSet()){
			PorosityDialog dialog = new PorosityDialog(container.getShell(), data);
			dialog.open();
			//data.getScenarioSet().getNodeStructure().setDefaultPorosityOfNode(dialog.getPorosity());
		}
	}

	@Override
	public void loadPage() {
		isCurrentPage = true;
		for(Control control: container.getChildren()) {
			control.dispose(); // Remove the children.
		}
		Font boldFont = new Font( container.getDisplay(), new FontData( "Helvetica", 12, SWT.BOLD ) );
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Input Directory");
		GridData infoGridData1 = new GridData(GridData.BEGINNING);
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
				MessageDialog.openInformation(container.getShell(), "Additional information", "Select the directory containing HDF5 files for all leakage simulations to be considered. If the user has not converted ASCII simulation output data into DREAM readable HDF5 input files, the Launch Converter button will open a pop-up file converter tool. Read more about the DREAM HDF5 Converter tool in the user manual. Note: The HDF5 files must be directly available within the directory provided; they may not be in subdirectories within the root directory.");	
			}			
		});
		infoLink.setLayoutData(infoLinkData);
				
		Label infoLabel = new Label(container,  SWT.TOP | SWT.LEFT | SWT.WRAP);	
		infoLabel.setText("Provide the path to a single directory containing hdf5 formatted files of all subsurface simulation output at specified plot times.");
		
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 4;
		infoLabel.setLayoutData(infoGridData);
			
		
		final DirectoryDialog directoryDialog = new DirectoryDialog(container.getShell());
		Button buttonSelectDir = new Button(container, SWT.PUSH);
		buttonSelectDir.setText("Select a directory");
		buttonSelectDir.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				directoryDialog.setFilterPath(hdf5Text.getText());
				directoryDialog.setMessage("Please select a directory and click OK");
				String dir = directoryDialog.open();
				if (dir != null) {
					Constants.homeDirectory = dir;
					hdf5Text.setText(dir);
					data.reset();
				}
			}
		});
		
		hdf5Text = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData myGd = new GridData(GridData.FILL_HORIZONTAL);
		hdf5Text.setText(Constants.homeDirectory);
		hdf5Text.setLayoutData(myGd);
	
		/*
		Label functionLabel = new Label(container, SWT.NULL);
		functionLabel.setText("Simulation tool");
		simulation = new Combo(container,  SWT.DROP_DOWN | SWT.READ_ONLY);
		for(Function function: new Function[]{new CCS9_1()}) {
			simulation.add("Sensor placement optimization: " + function.toString());
		}
		simulation.setText(simulation.getItem(0));
		GridData wellgd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		simulation.setLayoutData(wellgd);
		*/
		/*
		simulation = new Group(container, SWT.SHADOW_ETCHED_IN);
		simulation.setText("Simulation tool");
		simulation.setLayout(new RowLayout(SWT.VERTICAL));
		Button selected1 = new Button(simulation, SWT.RADIO);
		selected1.setText("Sensor placement optimization: " + (new CCS9_1()).toString());
		selected1.setSelection(true);
		//Add other options below
		*/
		/*
		Label modelOptionLabel = new Label(container, SWT.NULL);
		modelOptionLabel.setText("Model option");
		modelOption = new Combo(container,  SWT.DROP_DOWN | SWT.READ_ONLY);
		
		modelOption.add(Constants.ModelOption.INDIVIDUAL_SENSORS_2.toString());
		if(Constants.buildDev) modelOption.add(Constants.ModelOption.ALL_SENSORS.toString());
		if(Constants.buildDev) modelOption.add(Constants.ModelOption.INDIVIDUAL_SENSORS.toString());
		if(Constants.buildDev) modelOption.add(Constants.ModelOption.REALIZED__WELLS.toString());
		modelOption.setText(modelOption.getItem(0));
		GridData modelgd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		modelOption.setLayoutData(modelgd);
		*/

		GridData buttonGridData = new GridData(GridData.FILL_HORIZONTAL);
		buttonGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		Group radioButton = new Group(container, SWT.SHADOW_NONE);
		radioButton.setText("Model option");
		radioButton.setLayout(new RowLayout(SWT.VERTICAL));
		Button button1 = new Button(radioButton, SWT.RADIO);
		button1.setText(Constants.ModelOption.INDIVIDUAL_SENSORS_2.toString());
		button1.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				System.out.println("1111111");
				modelOption = Constants.ModelOption.INDIVIDUAL_SENSORS_2.toString();
			}
		});
		button1.setSelection(true);
		Button button2 = new Button(radioButton, SWT.RADIO);
		button2.setText(Constants.ModelOption.ALL_SENSORS.toString());
		button2.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event event) {
				System.out.println("2222222");
				modelOption = Constants.ModelOption.ALL_SENSORS.toString();
			}
		});
		
		
		radioButton.setLayoutData(buttonGridData);
		//Add other options here
				
		modelDescription = new Label(container, SWT.NULL);
		
		Label noteLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		noteLabel.setText("More info: The \"Launch Converter\" button will allow file format conversions from ASCII to hdf5 for common subsurface simulation output formats (currently: NUFT, STOMP). If the file converter is incompatible with the desired output file format, specific formatting requirements are given in the user manual. ");
		GridData noteGridData = new GridData(GridData.FILL_HORIZONTAL);
		noteGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		noteGridData.verticalSpan = 4;
		noteGridData.widthHint = 500;
		noteLabel.setLayoutData(noteGridData);
		
		container.layout();	
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();		

		DREAMWizard.visLauncher.setEnabled(false);
		DREAMWizard.convertDataButton.setEnabled(true);
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