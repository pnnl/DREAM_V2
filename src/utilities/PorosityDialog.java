package utilities;



import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class PorosityDialog extends TitleAreaDialog {

	private Text porosityText;

	private float porosity;

	public PorosityDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	public void create() {
		super.create();
		setTitle("Set default porosity");
		setMessage("No porosity information was detected in the hdf5 files included for this run. Please provide a default porosity between 0 and 1.", IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		
		createPorosity(container);

		return area;
	}
	
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton){
		if(id == IDialogConstants.CANCEL_ID) return null;
		return super.createButton(parent, id, label, defaultButton);
	}

	@Override
	protected boolean canHandleShellCloseEvent(){
		return false;
	}
	
	private void createPorosity(Composite container) {
		Label lbtFirstName = new Label(container, SWT.NONE);
		lbtFirstName.setText("Porosity:");

		GridData dataFirstName = new GridData();
		dataFirstName.grabExcessHorizontalSpace = true;
		dataFirstName.horizontalAlignment = GridData.FILL;

		porosityText = new Text(container, SWT.BORDER);
		porosityText.setLayoutData(dataFirstName);
		
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	// save content of the Text fields because they get disposed
	// as soon as the Dialog closes
	private boolean saveInput() {
		try{
			porosity = Float.parseFloat(porosityText.getText());
		} catch(NumberFormatException ne){
			return false;
		}
		if(porosity < 0 || porosity > 1) return false;
		return true;
	}

	
	@Override
	protected void okPressed() {
		
		if(!saveInput()) return;
		super.okPressed();
	}

	public Float getPorosity() {
		return porosity;
	}
}
