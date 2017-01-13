package wizardPages;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Generic scrollable dialog.
 * Currently unused.
 * @author port091
 */

public class ScrollableDialog extends TitleAreaDialog {
	private String title;
	private String text;
	private String scrollableText;
	Text scrollable;
	Composite composite;
	List<String> lines = new ArrayList<String>();
	List<String> previousRuns = new ArrayList<String>();
	List<String> currentRuns = new ArrayList<String>();
	private ProgressBar progressBar = null; 
	private int progressMax;
	private int runs;
	private int iterations;
	private int currentIteration;
	private int currentRun;
	private Label temp;
	private Label pText;
	
	public ScrollableDialog(Shell parentShell, String title, int runs, int iterations) {
		super(parentShell);		
		setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.TITLE);
		setBlockOnOpen(false);
		this.title = title;
		this.runs = runs;
		this.iterations = iterations;
		this.text = "Run: " + currentRun + "/" + runs + "\tIteration:" + currentIteration + "/" + iterations;
		this.scrollableText = "";
		this.progressMax = runs*iterations;

	}
	
	public void incrementIteration() {
		currentIteration++;
		updateProgress();
	}
	
	public void incrementRun() {
		currentRun++;
		currentIteration = 0;
		updateProgress();
	}
	
	private void updateProgress() {
		this.text = "Run: " + currentRun + "/" + runs + "\tIteration:" + currentIteration + "/" + iterations;
		progressBar.setSelection((currentRun-1)*iterations + currentIteration);
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				temp.setText(text);
				temp.update();
				composite.update();
				composite.layout();
			}
		});
	}
	
	public void appendText(String newLine) {
		currentRuns.add(0, newLine);
		final StringBuilder contentBuilder = new StringBuilder();
		for(String line: this.currentRuns) {
			contentBuilder.append(line + "\n");
		}
		for(String previous: previousRuns) {
			contentBuilder.append(previous + "\n");	
		}
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				scrollable.setText(contentBuilder.toString());
				scrollable.update();
				composite.update();
				composite.layout();
			}
		});
	}
	
	public void appendText(List<String> lines) {
		currentRuns.clear();
		this.previousRuns.addAll(lines);
		final StringBuilder contentBuilder = new StringBuilder();
		for(String line: this.previousRuns) {
			contentBuilder.append(line + "\n");
		}
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				scrollable.setText(contentBuilder.toString());
				scrollable.update();
				composite.update();
				composite.layout();
			}
		});
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		composite = (Composite) super.createDialogArea (parent); // Let the dialog create the parent composite
    
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 20;
		layout.verticalSpacing = 2;
	//	layout.numColumns = 3;
		composite.setLayout(layout);
		
		Label temp3 = new Label(composite, SWT.NULL);
		GridData td3 = new GridData();
	//	td3.horizontalSpan = 3;
		temp3.setLayoutData(td3);
		
		temp = new Label(composite, SWT.NULL);
		GridData td = new GridData();
	//	td.horizontalSpan = 3;
		temp.setLayoutData(td);
		temp.setText(text);
		
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 3;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true; // Layout vertically, too! 
		gridData.verticalAlignment = GridData.FILL;

		scrollable = new Text(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		scrollable.setLayoutData(gridData);

		progressBar = new ProgressBar(composite, SWT.SMOOTH);
		GridData pbGrid = new GridData(GridData.FILL_HORIZONTAL);
	//	pbGrid.horizontalSpan = 2;
		progressBar.setLayoutData(pbGrid);
        progressBar.setMaximum(progressMax);
        
	//	pText = new Label(composite, SWT.NULL);
	//	pText.setText("0/0");		

		return composite;
	}

	@Override
	public void create() {
		super.create();       
		getShell ().setSize (600, 600);
		//setTitle(title);
		setMessage(title, IMessageProvider.INFORMATION);
		this.open();
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button okButton = createButton(parent, OK, "OK", true);
		okButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				close();
			}
		});
		
	}

	@Override
	protected boolean isResizable() {
		return true; // Allow the user to change the dialog size!
	}

}