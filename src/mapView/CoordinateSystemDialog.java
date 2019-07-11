package mapView;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * The pop-up box that allows the user to input specific parameters for their map view.
 * @author huan482
 *
 */
public class CoordinateSystemDialog extends TitleAreaDialog {
	
	private boolean[] myEnableButtonCheck;
	
	private int theZoneNumber;
	
	private String theZone;
	
	private int theMinX = 0;
	
	private int theMinY = 0;
	
	private ScrolledComposite theComposite;
	
	private Composite theContainer;
	
	public CoordinateSystemDialog(final Shell theShell) {
		super(theShell);
//		theZone = 0;
	}
	
	@Override
	public void create() {
		myEnableButtonCheck = new boolean[4];
		super.create();
		setTitle("Set Map View Specifications");
		setMessage("Set the Zone (UTM)",
				IMessageProvider.INFORMATION);
        getButton(OK).setEnabled(false);	
	}
	
	@Override
	protected Control createDialogArea(final Composite theParent) {
		Composite area = (Composite) super.createDialogArea(theParent);
		
		theComposite = new ScrolledComposite(theParent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FILL);
		theComposite.setLayoutData(GridDataFactory.fillDefaults().
				grab(true, true).hint(SWT.DEFAULT, 200).create());;
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
	private void  createContainer(){
		theContainer = new Composite(theComposite, SWT.NONE);
		for(Control c: theContainer.getChildren()){
			c.dispose();
		}
		
		theContainer.setLayoutData(new GridData(SWT.NONE, SWT.NONE, true, true));
		GridLayout layout = new GridLayout(3, false);
		theContainer.setLayout(layout);
		
		theContainer.layout();
		theComposite.setContent(theContainer);
		theComposite.setMinSize(theContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		theComposite.setLayout(new GridLayout(1,false));
		createTheInputBoxes();
	}
	
	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		createButton(parent, OK, "OK", true);
	}
	
	protected Button createButton(Composite parent, final int id,
			final String label, final boolean defaultButton){
		if(id == IDialogConstants.CANCEL_ID) return null;
		return super.createButton(parent, id, label, defaultButton);
	}
	
	@Override
	protected boolean canHandleShellCloseEvent(){
		return true;
	}
	
	/**
	 * This class creates the input boxes inside theContainer composite, and creates the listeners.
	 */
	private void createTheInputBoxes() {
		
	Label theDistanceLabel = new Label(theContainer, SWT.NONE);
	theDistanceLabel.setText("Set the Zone:");
	Text UTMZone = new Text(theContainer, SWT.BORDER);
	
	Combo zoneDirection = new Combo(theContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
	
	zoneDirection.add("N");
	zoneDirection.add("S");
	
    
	zoneDirection.addSelectionListener(new SelectionListener() {
		@Override
	    public void widgetSelected(final SelectionEvent theEvent)
	    {	
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
		public void widgetDefaultSelected(SelectionEvent theEvent) {
		}
	});
	
	//Mimicked for the rest of the modify listeners.
    UTMZone.addModifyListener(theEvent -> {
    	try {
    		//If we can successful parse the text then we know it's good input.
	    	theZoneNumber = Integer.parseInt(UTMZone.getText());
	    	//Set the flag for this component to true.
	    	myEnableButtonCheck[1] = true;
	    	//If all the components have good inputs then we enable the button.
	    	if (checkForBadInput()) getButton(OK).setEnabled(true);	
    	} catch (Exception theException) {
    		//Flag this component in our array. Basically saying this input is bad.
    		myEnableButtonCheck[1] = false;
    		if (!checkForBadInput()) getButton(OK).setEnabled(false);
    		theException.printStackTrace();
		}
    	
    });

	}
	/**
	 * Checks if their are any bad inputs in our text fields (non-integers).
	 * @return - temp boolean.
	 */
    private boolean checkForBadInput() {
    	//return true if all the values in the array are true.
    	boolean temp = true;
    	for (int i = 0; i <= 1; i++) {
    		//If any values in the array are false return false.
    		if (!myEnableButtonCheck[i]) {
    			temp = false;
    			break;
    		}
    	}
    	return temp;
    }
    
	public int getZone() {
		return theZoneNumber;
	}
	
	public String getZoneDirection() {
		return theZone;
	}
	
	public int getMinX() {
		return theMinX;
	}
	
	public int getMinY() {
		return theMinY;
	}
}
