package utilities;


import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import wizardPages.DREAMWizard.STORMData;

/**
 * Dialog for running the multi-run ensemble
 * @author rodr144
 * @author whit162
 */

public class EnsembleDialog extends TitleAreaDialog {

	private ScrolledComposite sc;
	private Composite container;
	private Shell parentShell;
	
	public boolean readyToRun = false; //This is to tell if we pressed ok or not (if not, don't run the iterative procedure!)
	private boolean isReady = true; //this indicates that all values are valid (aka no red showing)
	
	private Text minNumSensors;
	private Text maxNumSensors;
	private Text iterationsPerSensorNumber;
	private Text iterationsPerRun;
	private Integer minSensors;
	private Integer maxSensors;
	private Integer iterationsPerSensor;
	private Integer iterations;
	
	public EnsembleDialog(Shell parentShell, STORMData data, Integer iterations) {
		super(parentShell);
		this.iterations = iterations;
		this.parentShell = parentShell;
	}
	
	@Override
	public void create() {
		super.create();
		setTitle("WARNING: about to start multi-run ensemble");
		String message = "Please review the following information before hitting \"run\".";
		setMessage(message, IMessageProvider.WARNING);
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

		Label minNumSensorsLabel = new Label(container, SWT.NULL);
		minNumSensorsLabel.setText("Smallest number of sensors to test");
		minNumSensors = new Text(container, SWT.BORDER | SWT.SINGLE);
		minNumSensors.setText("1");
		GridData minNumSensorsGD = new GridData(GridData.FILL_HORIZONTAL);
		minNumSensors.setLayoutData(minNumSensorsGD);
		minNumSensors.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					Integer.parseInt(((Text)e.getSource()).getText());	
					((Text)e.getSource()).setForeground(Constants.black);
				} catch (NumberFormatException ne) {
					((Text)e.getSource()).setForeground(Constants.red);
				}
			}				
		});

		Label maxNumSensorsLabel = new Label(container, SWT.NULL);
		maxNumSensorsLabel.setText("Largest number of sensors to test");
		maxNumSensors = new Text(container, SWT.BORDER | SWT.SINGLE);
		maxNumSensors.setText("10");
		GridData maxNumSensorsGD = new GridData(GridData.FILL_HORIZONTAL);
		maxNumSensors.setLayoutData(maxNumSensorsGD);
		maxNumSensors.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					Integer.parseInt(((Text)e.getSource()).getText());	
					((Text)e.getSource()).setForeground(Constants.black);
				} catch (NumberFormatException ne) {
					((Text)e.getSource()).setForeground(Constants.red);
				}
			}				
		});

		Label iterationsPerSensorNumberLabel = new Label(container, SWT.NULL);
		iterationsPerSensorNumberLabel.setText("Number of full runs for each number of sensors");
		iterationsPerSensorNumber = new Text(container, SWT.BORDER | SWT.SINGLE);
		iterationsPerSensorNumber.setText("5");
		GridData iterationsPerSensorNumberGD = new GridData(GridData.FILL_HORIZONTAL);
		iterationsPerSensorNumber.setLayoutData(iterationsPerSensorNumberGD);	
		iterationsPerSensorNumber.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					Integer.parseInt(((Text)e.getSource()).getText());	
					((Text)e.getSource()).setForeground(Constants.black);
				} catch (NumberFormatException ne) {
					((Text)e.getSource()).setForeground(Constants.red);
				}
			}				
		});

		Label iterationsPerRunLabel = new Label(container, SWT.NULL);
		iterationsPerRunLabel.setText("Iterations in a single run");
		iterationsPerRun = new Text(container, SWT.BORDER | SWT.SINGLE);
		iterationsPerRun.setText(iterations.toString());
		GridData iterationsPerRunGD = new GridData(GridData.FILL_HORIZONTAL);
		iterationsPerRun.setLayoutData(iterationsPerRunGD);	
		iterationsPerRun.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					Integer.parseInt(((Text)e.getSource()).getText());	
					((Text)e.getSource()).setForeground(Constants.black);
				} catch (NumberFormatException ne) {
					((Text)e.getSource()).setForeground(Constants.red);
				}
			}
		});
		
		container.layout();
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.setLayout(new GridLayout(1,false));
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, CANCEL, "Cancel", true);
		createButton(parent, OK, "Run", true);
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
			if(isReady){
				minSensors = Integer.valueOf(minNumSensors.getText());
				maxSensors = Integer.valueOf(maxNumSensors.getText());
				iterationsPerSensor = Integer.valueOf(iterationsPerSensorNumber.getText());
				iterations = Integer.valueOf(iterationsPerRun.getText());
				readyToRun = true;
				super.okPressed();
			}
			else{
				String[] buttons = {"OK"};
				MessageDialog message = new MessageDialog(parentShell, "Save Error", null, "Please fix errors in text fields before running", 
						MessageDialog.NONE, buttons, 0);
				message.open();
			}
		}
		else if(id == CANCEL){
			super.close();
		}
	}
	
	public int getMin(){
		return minSensors;
	}
	
	public int getMax(){
		return maxSensors;
	}
	
	public int getIterationsPerSensor(){
		return iterationsPerSensor;
	}
	
	public int getIterationsPerRun(){
		return iterations;
	}
}
