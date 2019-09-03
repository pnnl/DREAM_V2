package mapView;

import java.io.File;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import utilities.Constants;

/**
 * The pop-up box that allows the user to input specific parameters for their
 * map view.
 * 
 * @author huan482
 *
 */
public class CoordinateSystemDialog extends TitleAreaDialog {

	private boolean[] myEnableButtonCheck;

	private int theZoneNumber;

	private String theZone;

	private float theMinX = 0;

	private float theMinY = 0;

	private ScrolledComposite theComposite;

	private Composite theContainer;

	private boolean offsetRequired;

	private boolean isIncludeLocations;

	private Text outputFolder;
	
	private String theOutputDir;
	
	private boolean buttonPressed;
	
	private boolean generateMap = false;
	
	private String outputs = (Constants.runningJar ? Constants.userDir : Constants.parentDir) + File.separator
			+ "_results";

	public CoordinateSystemDialog(final Shell theShell, boolean offsetRequired, boolean isIncludeLocations,
			final boolean getMap) {
		super(theShell);
//		theZone = 0;
		this.offsetRequired = offsetRequired;
		this.isIncludeLocations = isIncludeLocations;
		generateMap = getMap;
	}

	@Override
	public void create() {
		myEnableButtonCheck = new boolean[4];
		super.create();
		setTitle("Set Specifications");
		setMessage("Input Parameters", IMessageProvider.INFORMATION);
		getButton(OK).setEnabled(false);
		if (generateMap) {
			getButton(OK).setEnabled(true);
		}
	}

	@Override
	protected Control createDialogArea(final Composite theParent) {
		Composite area = (Composite) super.createDialogArea(theParent);

		theComposite = new ScrolledComposite(theParent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FILL);
		theComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 200).create());
		;
		theComposite.setExpandHorizontal(true);
		theComposite.setExpandVertical(true);

		theComposite.addListener(SWT.Activate, new Listener() {
			public void handleEvent(Event e) {
				theComposite.setFocus();
			}
		});
		theComposite.addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(Event event) {
				int wheelCount = event.count;
				wheelCount = (int) Math.ceil(wheelCount / 3.0f);
				while (wheelCount < 0) {
					theComposite.getVerticalBar().setIncrement(4);
					wheelCount++;
				}

				while (wheelCount > 0) {
					theComposite.getVerticalBar().setIncrement(-4);
					wheelCount--;
				}
				theComposite.redraw();
			}
		});
		createContainer();
		return area;
	}

	/**
	 * This method sets the layouts and parent composites.
	 */
	private void createContainer() {
		theContainer = new Composite(theComposite, SWT.NONE);
		for (Control c : theContainer.getChildren()) {
			c.dispose();
		}

		theContainer.setLayoutData(new GridData(SWT.NONE, SWT.NONE, true, true));
		GridLayout layout = new GridLayout(3, false);
		theContainer.setLayout(layout);

		theContainer.layout();
		theComposite.setContent(theContainer);
		theComposite.setMinSize(theContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		theComposite.setLayout(new GridLayout(1, false));
		createTheInputBoxes();
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, OK, "OK", true);
	}

	protected Button createButton(Composite parent, final int id, final String label, final boolean defaultButton) {
		if (id == IDialogConstants.CANCEL_ID)
			return null;
		return super.createButton(parent, id, label, defaultButton);
	}

	@Override
	protected boolean canHandleShellCloseEvent() {
		return true;
	}
@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK) {
			buttonPressed = true;
		}
		super.buttonPressed(buttonId);
	}
	/**
	 * This class creates the input boxes inside theContainer composite, and creates
	 * the listeners.
	 */
	private void createTheInputBoxes() {

		if (!isIncludeLocations && !generateMap) {
			Label theDistanceLabel = new Label(theContainer, SWT.NONE);
			theDistanceLabel.setText("Set the Zone:");

			Text UTMZone = new Text(theContainer, SWT.BORDER);

			Combo zoneDirection = new Combo(theContainer, SWT.DROP_DOWN | SWT.READ_ONLY);

			zoneDirection.add("N");
			zoneDirection.add("S");
			// Mimicked for the rest of the modify listeners.
			zoneDirection.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					theZone = zoneDirection.getText();
			        //If no zone is entered then we set the button to false and the component in our array to false.
			        if (theZone.equals(null)) {
			    		myEnableButtonCheck[0] = false;
			    		if (!checkForBadInput()) getButton(OK).setEnabled(false);
			        } else {
			        	//If zone is entered we check if all the other components have good inputs.
			        	myEnableButtonCheck[0] = true;
			        	if (checkForBadInput()) getButton(OK).setEnabled(true);
			        }	
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			UTMZone.addModifyListener(theEvent -> {
				try {
					// If we can successful parse the text then we know it's good input.
					theZoneNumber = Integer.parseInt(UTMZone.getText());
					// Set the flag for this component to true.
					myEnableButtonCheck[1] = true;
					// If all the components have good inputs then we enable the button.
					if (checkForBadInput())
						getButton(OK).setEnabled(true);
					((Text) theEvent.getSource()).setForeground(Constants.black);
				} catch (Exception theException) {
					// Flag this component in our array. Basically saying this input is bad.
					myEnableButtonCheck[1] = false;
					if (!checkForBadInput())
						getButton(OK).setEnabled(false);
					((Text) theEvent.getSource()).setForeground(Constants.red);
				}

			});
		} else {
			myEnableButtonCheck[0] = true;
			myEnableButtonCheck[1] = true;
		}
		if (offsetRequired) {
			Label xLabel = new Label(theContainer, SWT.NONE);
			xLabel.setText("Offset X: ");
			Text xText = new Text(theContainer, SWT.BORDER);
			Label xBlank = new Label(theContainer, SWT.NONE);
			xBlank.setText("");
			// Mimicked for the rest of the modify listeners.
			xText.addModifyListener(theEvent -> {
				try {
					// If we can successful parse the text then we know it's good input.
					theMinX = Float.parseFloat(xText.getText());
					// Set the flag for this component to true.
					myEnableButtonCheck[2] = true;
					// If all the components have good inputs then we enable the button.
					if (checkForBadInput())
						getButton(OK).setEnabled(true);
					((Text) theEvent.getSource()).setForeground(Constants.black);
				} catch (Exception theException) {
					// Flag this component in our array. Basically saying this input is bad.
					myEnableButtonCheck[2] = false;
					if (!checkForBadInput())
						getButton(OK).setEnabled(false);
					((Text) theEvent.getSource()).setForeground(Constants.red);
				}

			});
		} else {
			myEnableButtonCheck[2] = true;
		}

		if (offsetRequired) {
			Label yLabel = new Label(theContainer, SWT.NONE);
			yLabel.setText("Offset Y: ");
			Text yText = new Text(theContainer, SWT.BORDER);
			Label yBlank = new Label(theContainer, SWT.NONE);
			yBlank.setText("");
			// Mimicked for the rest of the modify listeners.
			yText.addModifyListener(theEvent -> {
				try {
					// If we can successful parse the text then we know it's good input.
					theMinY = Float.parseFloat(yText.getText());
					((Text) theEvent.getSource()).setForeground(Constants.black);
					// Set the flag for this component to true.
					myEnableButtonCheck[3] = true;
					// If all the components have good inputs then we enable the button.
					if (checkForBadInput())
						getButton(OK).setEnabled(true);
					((Text) theEvent.getSource()).setForeground(Constants.black);
				} catch (Exception theException) {
					// Flag this component in our array. Basically saying this input is bad.
					myEnableButtonCheck[3] = false;
					if (!checkForBadInput())
						getButton(OK).setEnabled(false);
					((Text) theEvent.getSource()).setForeground(Constants.red);
				}

			});
		} else {
			myEnableButtonCheck[3] = true;
		}
		if (isIncludeLocations || generateMap) {
			createDirectoryGUI();
		}
	}

	/**
	 * Checks if their are any bad inputs in our text fields (non-integers).
	 * 
	 * @return - temp boolean.
	 */
	private boolean checkForBadInput() {
		// return true if all the values in the array are true.
		boolean temp = true;
		for (int i = 0; i < myEnableButtonCheck.length; i++) {
			// If any values in the array are false return false.
			if (!myEnableButtonCheck[i]) {
				temp = false;
				break;
			}
		}
		return temp;
	}

	private void createDirectoryGUI() {
		final DirectoryDialog directoryDialog = new DirectoryDialog(theContainer.getShell());
		Button buttonSelectDir = new Button(theContainer, SWT.PUSH);
		if (generateMap) {
			buttonSelectDir.setText("Input .in Files");
		} else {
			buttonSelectDir.setText("Output Directory");	
		}
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

		outputFolder = new Text(theContainer, SWT.BORDER | SWT.SINGLE);
		outputFolder.setText(outputs);
		outputFolder.setForeground(Constants.black);
		outputFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		theOutputDir = outputFolder.getText();
		outputFolder.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					File resultsFolder = new File(((Text) e.getSource()).getText());
					outputs = resultsFolder.getCanonicalPath();
					((Text) e.getSource()).setForeground(Constants.black);
					theOutputDir = outputFolder.getText();
				} catch (Exception ex) {
					((Text) e.getSource()).setForeground(Constants.red);
				}
			}
		});

	}
	
	public boolean getButtonPressed() {
		return buttonPressed;
	}
	
	public int getZone() {
		return theZoneNumber;
	}

	public String getZoneDirection() {
		return theZone;
	}

	public float getMinX() {
		return theMinX;
	}

	public float getMinY() {
		return theMinY;
	}
	
	public String getOutputDir() {
		return theOutputDir;
	}
}
