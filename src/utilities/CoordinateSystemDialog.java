package utilities;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
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
	private int theZone;
	
	private int theMinX = 0;
	
	private int theMinY = 0;
	
	private String theHemisphere;
	
	private ScrolledComposite theComposite;
	
	private Composite theContainer;
	
		
	public CoordinateSystemDialog(final Shell theShell) {
		super(theShell);
		theZone = 0;
	}
	
	@Override
	public void create() {
		super.create();
		setTitle("Set Map View Specifications");
		setMessage("Set the Zone (UTM), Hemisphere, and Minimum (x,y) coordinate (Origin Point).",
				IMessageProvider.INFORMATION);
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
		
		
		theContainer = new Composite(theComposite, SWT.NONE);
		createContainer();
		return area;
	}
	
	private void  createContainer(){
		for(Control c: theContainer.getChildren()){
			c.dispose();
		}
		theContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		theContainer.setLayout(layout);
		
		theContainer.layout();
		theComposite.setContent(theContainer);
		theComposite.setMinSize(theContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		theComposite.setLayout(new GridLayout(1,false));
		createTheInputBoxes();
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, OK, "OK", true);
	}
	
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton){
		if(id == IDialogConstants.CANCEL_ID) return null;
		return super.createButton(parent, id, label, defaultButton);
	}
	
	@Override
	protected boolean canHandleShellCloseEvent(){
		return false;
	}

	private void createTheInputBoxes() {
//		Label theCoordinate = new Label(theContainer, SWT.NONE);
//		theCoordinate.setText("Please set your coodinate System.");
		
		Label theDistanceLabel = new Label(theContainer, SWT.NONE);
		theDistanceLabel.setText("Set the Zone:");
		
		Combo UTMZone = new Combo(theContainer, SWT.DROP_DOWN);
		
		Label theUTMHemisphere = new Label(theContainer, SWT.NONE);
		theUTMHemisphere.setText("Set the Hempishere:");
		Text UTMHemi = new Text(theContainer, SWT.BORDER);
		
		Label theMinXLabel = new Label(theContainer, SWT.NONE);
		theMinXLabel.setText("Min x Coordinate:");
		Text minX = new Text(theContainer, SWT.BORDER);
		
		Label theMinYLabel = new Label(theContainer, SWT.NONE);
		theMinYLabel.setText("Min y Coordinate:");
		Text minY = new Text(theContainer, SWT.BORDER);
		
		for (int i = 1; i <= 20; i++) {
			UTMZone.add(i + "N");
		}
		
	    UTMZone.select(0);
	    minX.setText("0");
	    minY.setText("0");
	    theZone = Integer.parseInt(UTMZone.getText().replaceFirst("N", ""));
	    
	    UTMZone.addModifyListener(theEvent -> theZone =
	    		Integer.parseInt(UTMZone.getText().replaceFirst("N", "")));
	    
	    UTMHemi.addModifyListener(theEvent -> theHemisphere = UTMHemi.getText());
	    
	    minX.addModifyListener(theEvent -> theMinX = Integer.parseInt(minX.getText()));
	    
	    minY.addModifyListener(theEvent -> theMinY = Integer.parseInt(minY.getText()));
	    
	}
	
	
	public int getZone() {
		return theZone;
	}
	
	public String getHemisphere() {
		return theHemisphere;
	}
	
	public int getMinX() {
		return theMinX;
	}
	
	public int getMinY() {
		return theMinY;
	}
}
