package utilities;


import java.util.List;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;


/**
 * Dialog that allows the user to select which pressure value should be used with the E4D file generation
 * This dialog box only shows if there is more than one sensor containing "pressure"
 * @author whit162
 */

public class E4DDialog extends TitleAreaDialog {
	
	private List<String> potentialParameters = new ArrayList<String>();
	private String selectedParameter;
	
	private ScrolledComposite sc;
	private Composite container;
	
	public E4DDialog(Shell parentShell, List<String> sensors) {
		super(parentShell);
		potentialParameters = sensors;
	}
	
	@Override
	public void create() {
		super.create();
		setTitle("Select parameter for E4D");
		String message = "There were multiple pressure parameters detected in the hdf5 files.";
		message += "\nSelect the parameter you wish to use for the optimum well output.";
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
		container.setLayout(new GridLayout(1, false));
		
		createRadioButtons();
		
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
	
	@Override
	protected void buttonPressed(int id) {
		if(id == OK)
			super.okPressed();
	}
	
	public String getParameter() {
		return selectedParameter;
	}
}