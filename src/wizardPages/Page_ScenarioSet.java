package wizardPages;

import java.io.File;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import functions.CCS9_1;
import functions.Function;
import functions.MutationFunction.MUTATE;
import utilities.Constants;
import utilities.Constants.ModelOption;
import wizardPages.DREAMWizard.STORMData;

public class Page_ScenarioSet extends WizardPage implements AbstractWizardPage {

	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private Combo scenarioSet;
	private Combo simulation;
	private Combo modelOption;
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
		return simulation.getText();
	}

	public String getModelOption() {
		return modelOption.getText();
	}

	public void completePage() throws Exception {	
		isCurrentPage = false;
		
		ModelOption modelOption = ModelOption.INDIVIDUAL_SENSORS;
		String strOption = getModelOption();
		if(strOption.equals(ModelOption.INDIVIDUAL_SENSORS.toString()))
			modelOption = ModelOption.INDIVIDUAL_SENSORS;
		else if(strOption.equals(ModelOption.INDIVIDUAL_SENSORS_2.toString()))
			modelOption = ModelOption.INDIVIDUAL_SENSORS_2;
		else 
			modelOption = ModelOption.REALIZED__WELLS;
		
		data.setupScenarioSet(modelOption, getModelOption().toLowerCase().contains("sensors") ? MUTATE.SENSOR : MUTATE.WELL, getSimulation(), hdf5Text.getText());
	}

	@Override
	public void loadPage() {
		isCurrentPage = true;
		for(Control control: container.getChildren()) {
			control.dispose(); // Remove the children.
		}
		Font boldFont = new Font( container.getDisplay(), new FontData( "Arial", 10, SWT.BOLD ) );

		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Input Directory");
		GridData infoGridData1 = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData1.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData1.verticalSpan = 4;
		infoLabel1.setLayoutData(infoGridData1);
		
		infoLabel1.setFont(boldFont);
		
		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
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
				}
			}
		});
		
		hdf5Text = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData myGd = new GridData(GridData.FILL_HORIZONTAL);
		hdf5Text.setText(Constants.homeDirectory);
		hdf5Text.setLayoutData(myGd);
	
		Label functionLabel = new Label(container, SWT.NULL);
		functionLabel.setText("Simulation tool");
		simulation = new Combo(container,  SWT.DROP_DOWN | SWT.READ_ONLY);
		for(Function function: new Function[]{new CCS9_1()}) {
			simulation.add("Sensor placement optimization: " + function.toString());
		}
		simulation.setText(simulation.getItem(0));
		GridData wellgd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		simulation.setLayoutData(wellgd);

		Label modelOptionLabel = new Label(container, SWT.NULL);
		modelOptionLabel.setText("Model option");
		modelOption = new Combo(container,  SWT.DROP_DOWN | SWT.READ_ONLY);
		
		modelOption.add(Constants.ModelOption.INDIVIDUAL_SENSORS_2.toString());
		modelOption.add(Constants.ModelOption.INDIVIDUAL_SENSORS.toString());
		modelOption.add(Constants.ModelOption.REALIZED__WELLS.toString());
		modelOption.setText(modelOption.getItem(0));
		GridData modelgd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		modelOption.setLayoutData(modelgd);
		
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