package utilities;


import java.io.File;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import wizardPages.Page_LeakageCriteria.SensorData;


/**
 * Dialog that allows the user to set parameters to run E4D
 * User will select a storage file location and ensure that needed variables are mapped correctly
 * @author whit162
 */

public class E4DRunDialog extends TitleAreaDialog {
	
	private String ensemble;
	private Map<String, SensorData> sensorData;
	
	private String storage = Constants.userDir;
	private String brineSaturation;
	private String gasSaturation;
	private String saltConcentration;
	private float detectionThreshold = 0.001f;
	
	private Text storageText;
	private Combo brineCombo;
	private Combo gasCombo;
	private Combo saltCombo;
	private Text detectionText;
	
	private ScrolledComposite sc;
	private Composite container;
	private Button ok;
	
	public E4DRunDialog(Shell parentShell, String ensemble, String brineSaturation, String gasSaturation, String saltConcentration, Map<String, SensorData> sensorData) {
		super(parentShell);
		this.ensemble = ensemble;
		this.brineSaturation = brineSaturation;
		this.gasSaturation = gasSaturation;
		this.saltConcentration = saltConcentration;
		this.sensorData = sensorData;
	}
	
	
	@Override
	public void create() {
		super.create();
		setTitle("Setup for E4D Run");
		String message = "You are about to run the E4D Module using the loaded scenario ensemble and generated candidate well locations."
				+ " You must identify several additional inputs.";
		setMessage(message, IMessageProvider.INFORMATION);
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		
		sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FILL);
		sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 230).create());
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		
		container = new Composite(sc, SWT.NONE);
		buildThings();
		return area;
	}
	
	
	protected void buildThings() {
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(new GridLayout(2, false));
		
		createStorageInterface();
		createMappings();
		createUserInputs();
		
		container.layout();
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	
	// Creates the interface for selecting the storage file
	private void createStorageInterface() {
		Label storageLabel = new Label(container,  SWT.TOP | SWT.LEFT | SWT.WRAP);	
		storageLabel.setText("Provide the path to the storage data file (.h5) that pairs with the " + ensemble + " ensemble.");
		GridData span2Columns = new GridData(GridData.FILL_HORIZONTAL);
		span2Columns.horizontalSpan = 2;
		storageLabel.setLayoutData(span2Columns);
		
		Button buttonSelectDir = new Button(container, SWT.PUSH);
		buttonSelectDir.setText("Select a directory");
		buttonSelectDir.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				FileDialog dialog = new FileDialog(container.getShell());
				dialog.setFilterPath(storage);
				storage = dialog.open();
				File fileTest = new File(storage);
				if(fileTest != null)
					storageText.setText(storage);
				checkRequirements();
			}
		});
		
		storageText = new Text(container, SWT.BORDER | SWT.SINGLE);
		storageText.setText(storage);
		storageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		storageText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				storageText = (Text)e.getSource();
				try {
					File fileTest = new File(storageText.getText());
					if(fileTest != null)
						storage = storageText.getText();
				} catch (Exception ex) {
					// Not a real file
				}
				checkRequirements();
			}
		});
		
		// Add some white space to separate the dialog box
		Label spacer = new Label(container, SWT.NULL);
		spacer.setLayoutData(span2Columns);
	}
	
	
	// Creates the variable mapping section
	private void createMappings() {
		Font boldFont1 = new Font(container.getDisplay(), new FontData("Helvetica", 10, SWT.BOLD));
		
		Group variableMapping = new Group(container, SWT.SHADOW_NONE);
		variableMapping.setText("Variable Mapping");
		variableMapping.setLayout(new GridLayout(2,true));
		variableMapping.setFont(boldFont1);
		GridData span2Columns = new GridData(SWT.FILL, SWT.CENTER, true, false);
		span2Columns.horizontalSpan = 2;
		variableMapping.setLayoutData(span2Columns);
		
		//Brine Saturation
		Label brineLabel = new Label(variableMapping, SWT.NULL);
		brineLabel.setText("Brine Saturation");
		brineCombo = new Combo(variableMapping, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
		brineCombo.add("");
		for(String label: sensorData.keySet()) {
			if(!label.contains("Electrical Conductivity"))
				brineCombo.add(label);
		}
		brineCombo.setText(brineSaturation);
		brineCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				brineSaturation = ((Combo)e.getSource()).getText();
				checkRequirements();
			}
		});
		
		//Gas Saturation
		Label gasLabel = new Label(variableMapping, SWT.NULL);
		gasLabel.setText("Gas Saturation");
		gasCombo = new Combo(variableMapping, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
		gasCombo.add("");
		for(String label: sensorData.keySet()) {
			if(!label.contains("Electrical Conductivity"))
				gasCombo.add(label);
		}
		gasCombo.setText(gasSaturation);
		gasCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				gasSaturation = ((Combo)e.getSource()).getText();
				checkRequirements();
			}
		});
		
		//Salt Concentration
		Label saltLabel = new Label(variableMapping, SWT.NULL);
		saltLabel.setText("Salt Concentration");
		saltCombo = new Combo(variableMapping, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
		saltCombo.add("");
		for(String label: sensorData.keySet()) {
			if(!label.contains("Electrical Conductivity"))
				saltCombo.add(label);
		}
		saltCombo.setText(saltConcentration);
		saltCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				saltConcentration = ((Combo)e.getSource()).getText();
				checkRequirements();
			}
		});
	}
	
	
	// Creates the input section for detection threshold
	private void createUserInputs() {
		// Add some white space to separate the dialog box
		Label spacer = new Label(container, SWT.NULL);
		GridData span2Columns = new GridData(GridData.FILL_HORIZONTAL);
		span2Columns.horizontalSpan = 2;
		spacer.setLayoutData(span2Columns);
		
		Label detectionLabel = new Label(container,  SWT.TOP | SWT.LEFT | SWT.WRAP);	
		detectionLabel.setText("E4D Detection Threshold (%)");
		detectionLabel.setLayoutData(new GridData(SWT.NULL, SWT.NULL, false, false, 1, 1));
		detectionText = new Text(container, SWT.BORDER | SWT.SINGLE);
		detectionText.setText("0.001");
		detectionText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		detectionText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				detectionText = (Text)e.getSource();
				try {
					detectionThreshold = Float.parseFloat(detectionText.getText()) / 100;
				} catch (Exception ex) {
					// Not a real number
				}
				checkRequirements();
			}
		});
	}
	
	
	private void checkRequirements() {
		ok = getButton(IDialogConstants.OK_ID);
		boolean error = false;
		
		// Combo boxes need to be mapped to a variable
		if(brineCombo.getText().length()==0 || gasCombo.getText().length()==0 || saltCombo.getText().length()==0) {
			error = true;
			ok.setEnabled(false);
		}
		
		// Storage text needs to be a real file that contains ".H5" in the name"
		if(!storageText.getText().toLowerCase().contains(".h5") || !Constants.isValidFile(storageText.getText())) {
			error = true;
			storageText.setForeground(Constants.red);
			ok.setEnabled(false);
		} else
			storageText.setForeground(Constants.black);
		
		// Detection Text needs to be a float
		if(!Constants.isValidFloat(detectionText.getText())) {
			error = true;
			detectionText.setForeground(Constants.red);
			ok.setEnabled(false);
		} else
			detectionText.setForeground(Constants.black);
		
		// If no errors are found, they can click ok
		if(error==false)
			ok.setEnabled(true);
	}
	
	
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		if(id == IDialogConstants.CANCEL_ID) return null;
		return super.createButton(parent, id, label, defaultButton);
	}
	
	
	@Override
	protected void buttonPressed(int id) {
		checkRequirements();
		if(!ok.getEnabled())
			return;
		if(id == OK)
			super.okPressed();
	}
	
	
	public String getStorage() {
		return storage;
	}
	
	public String getBrineSaturation() {
		return brineSaturation;
	}
	
	public String getGasSaturation() {
		return gasSaturation;
	}
	
	public String getSaltConcentration() {
		return saltConcentration;
	}
	
	public float getDetectionThreshold() {
		return detectionThreshold;
	}
}