package utilities;



import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import objects.Sensor;
import objects.SensorSetting.DeltaType;
import objects.SensorSetting.Trigger;
import wizardPages.DREAMWizard.STORMData;

public class ComparisonDialog extends TitleAreaDialog {

	private ScrolledComposite sc;
	private Composite container;
	private STORMData data;
	private Shell parentShell;
	
	public boolean readyToRun = false; //This is to tell if we pressed ok or not (if not, don't run the iterative procedure!)
	private boolean isReady = true; //this indicates that all values are valid (aka no red showing)

	private Combo sensor1Combo;
	private Combo sensor2Combo;
	private String sensor1;
	private String sensor2;
	private HashMap<String, String> aliasToType;


	
	public ComparisonDialog(Shell parentShell, STORMData data, Integer iterations) {
		super(parentShell);
		aliasToType = new HashMap<String, String>();
		for(String dataType: data.getSet().getDataTypes()) aliasToType.put(Sensor.sensorAliases.get(dataType) + " (" + dataType + ")", dataType);
		this.parentShell = parentShell;
		this.data = data;
	}
	
	@Override
	public void create() {
		super.create();
		setTitle("Compare two sensors");
		String message = "Choose which two sensors you would like to compare.";
		setMessage(message, IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		
		sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FILL);
		sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 200).create());
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		
		container = new Composite(sc, SWT.NONE);
		buildThings();
		return area;
	}
	
	@Override
	protected int getShellStyle(){
		return super.getShellStyle() & (~SWT.RESIZE);
	}
	
	protected void buildThings(){
		for(Control c: container.getChildren()){
			c.dispose();
		}
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);

		Label sensor1Label = new Label(container, SWT.NULL);
		sensor1Label.setText("Sensor 1:");
		
		sensor1Combo = new Combo(container, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
		for(String dataType: data.getSet().getDataTypes()) sensor1Combo.add(Sensor.sensorAliases.get(dataType) + " (" + dataType + ")");

		Label sensor2Label = new Label(container, SWT.NULL);
		sensor2Label.setText("Sensor 2:");
		
		sensor2Combo = new Combo(container, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
		for(String dataType: data.getSet().getDataTypes()) sensor2Combo.add(Sensor.sensorAliases.get(dataType) + " (" + dataType + ")");
		
		container.layout();
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.setLayout(new GridLayout(1,false));
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button cancelButton = createButton(parent, CANCEL, "Cancel", true);
		Button okButton = createButton(parent, OK, "Run", true);
	}

	protected Button createButton(Composite parent, int id, String label, boolean defaultButton){
		return super.createButton(parent, id, label, defaultButton);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}


	@Override
	protected void buttonPressed(int id){
		if(id == OK){
			sensor1 = sensor1Combo.getText();
			sensor2 = sensor2Combo.getText();
			System.out.println(sensor1 + "\t" + sensor2);
			if(isReady && !sensor1.equals(sensor2) && !sensor1.equals("") && !sensor2.equals("")){
				readyToRun = true;
				super.okPressed();
			}
			else System.out.println("Catchycatchy");
		}
		else if(id == CANCEL){
			super.close();
		}
	}
	
	public String getSensor1(){
		return aliasToType.get(sensor1);
	}
	
	public String getSensor2(){
		return aliasToType.get(sensor2);
	}
	
	private void testReady(){
		
	}
}
