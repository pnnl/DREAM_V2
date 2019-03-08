package utilities;


import java.util.List;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


/**
 * Dialog that allows the user to select which pressure value should be used with the E4D file generation
 * This dialog box only shows if there is more than one sensor containing "pressure"
 * @author whit162
 */

public class E4DDialog extends TitleAreaDialog {
	
	private List<String> potentialParameters = new ArrayList<String>();
	private String selectedParameter;
	private Text wellText;
	private int maximumWells = 30;
	
	private ScrolledComposite sc;
	private Composite container;
	private Button ok;
	
	public E4DDialog(Shell parentShell, List<String> sensors) {
		super(parentShell);
		potentialParameters = sensors;
	}
	
	@Override
	public void create() {
		super.create();
		setTitle("Inputs to Calculate E4D Wells");
		String message = "You are about to generate a list of cadidate wells for E4D to use."
				+ "\nSelect a pressure parameter and the number of wells you want to assess.";
		setMessage(message, IMessageProvider.INFORMATION);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		
		sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FILL);
		sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 100).create());
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		
		container = new Composite(sc, SWT.NONE);
		buildThings();
		return area;
	}
	
	protected void buildThings() {
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		container.setLayout(new GridLayout(2, false));
		
		createRadioButtons();
		createUserInputs();
		
		container.layout();
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		if(id == IDialogConstants.CANCEL_ID) return null;
		return super.createButton(parent, id, label, defaultButton);
	}
	
	private void createRadioButtons() {
		GridData buttonGridData = new GridData(GridData.FILL_HORIZONTAL);
		buttonGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		Group radioButton = new Group(container, SWT.SHADOW_NONE);
		radioButton.setText("Pressure Parameters");
		radioButton.setLayout(new RowLayout(SWT.VERTICAL));
		GridData span2Columns = new GridData(SWT.FILL, SWT.CENTER, true, false);
		span2Columns.horizontalSpan = 2;
		radioButton.setLayoutData(span2Columns);
		
		selectedParameter = potentialParameters.get(0);
		for (String sensor: potentialParameters) {
			final Button button = new Button(radioButton, SWT.RADIO);
			button.setText(sensor);
			button.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					selectedParameter = button.getText();
				}
			});
		}
		radioButton.setLayoutData(buttonGridData);
	}
	
	// Creates the input section for number of wells
	private void createUserInputs() {
		// Add some white space to separate the dialog box
		Label spacer = new Label(container, SWT.NULL);
		GridData span2Columns = new GridData(GridData.FILL_HORIZONTAL);
		span2Columns.horizontalSpan = 2;
		spacer.setLayoutData(span2Columns);
		
		Label wellLabel = new Label(container,  SWT.TOP | SWT.LEFT | SWT.WRAP);	
		wellLabel.setText("  Number of Wells");
		wellLabel.setLayoutData(new GridData(SWT.NULL, SWT.NULL, false, false, 1, 1));
		wellText = new Text(container, SWT.BORDER | SWT.SINGLE);
		wellText.setText("30");
		wellText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		wellText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				wellText = (Text)e.getSource();
				ok = getButton(IDialogConstants.OK_ID);
				try {
					maximumWells = Integer.parseInt((wellText).getText());
					ok.setEnabled(true);
					wellText.setForeground(Constants.black);
				} catch (Exception ex) {
					// Not a real number
					ok.setEnabled(false);
					wellText.setForeground(Constants.red);
				}
				// Need to make sure they ask for 2 or more wells or else E4D won't run
				if(maximumWells < 2) {
					ok.setEnabled(false);
					wellText.setForeground(Constants.red);
				} else {
					ok.setEnabled(true);
					wellText.setForeground(Constants.black);
				}
			}
		});
	}
	
	@Override
	protected void buttonPressed(int id) {
		if(id == OK)
			super.okPressed();
	}
	
	public String getParameter() {
		return selectedParameter;
	}
	
	public int getMaximumWells() {
		return maximumWells;
	}
}