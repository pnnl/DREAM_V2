package mapView;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import utilities.Constants;
import wizardPages.DREAMWizard.STORMData;
/**
 * Creates the include locations dialog box.
 * @author huan482
 *
 */
public class ExistingWellsDialogBox extends TitleAreaDialog {

	private Map<Integer, Float> mapToValue;

	private Map<Integer, Boolean> checkAllInputs;

	private List<ExistingWell> myWells;

	private ScrolledComposite sc;

	private Composite container;

	private int myWellCounter;

	private int counterForCoordinate;
	
	private STORMData myData;
	
	public ExistingWellsDialogBox(Shell parentShell, STORMData data) {
		super(parentShell);
		myData = data; 
		myWellCounter = 1;
		counterForCoordinate = 0;
		checkAllInputs = new HashMap<Integer, Boolean>();
		mapToValue = new HashMap<Integer, Float>();
	}

	@Override
	public void create() {
		super.create();
		myWells = new ArrayList<ExistingWell>();
		setTitle("Include Existing Wells");
		String message = "Please enter the UTM coordinate of an existing well.";
		setMessage(message, IMessageProvider.INFORMATION);

	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);

		sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FILL);
		sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 200).create());
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);

		sc.addListener(SWT.Activate, new Listener() {
			public void handleEvent(Event e) {
				sc.setFocus();
			}
		});
		sc.addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(Event event) {
				int wheelCount = event.count;
				wheelCount = (int) Math.ceil(wheelCount / 3.0f);
				while (wheelCount < 0) {
					sc.getVerticalBar().setIncrement(4);
					wheelCount++;
				}

				while (wheelCount > 0) {
					sc.getVerticalBar().setIncrement(-4);
					wheelCount--;
				}
				sc.redraw();
			}
		});

		container = new Composite(sc, SWT.NONE);
		buildThings();
		return area;
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() & (~SWT.RESIZE);
	}

	protected void buildThings() {
		container.setLayoutData(new GridData(SWT.NONE, SWT.NONE, true, true));
		GridLayout layout = new GridLayout(4, false);
		container.setLayout(layout);

		if (myWellCounter == 1) {
			createZoneHeaders();
		}

		createZone();

		createAddButton();
		container.layout();
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.setLayout(new GridLayout(1, false));
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, OK, "OK", true);
	}

	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		if (id == IDialogConstants.CANCEL_ID)
			return null;
		return super.createButton(parent, id, label, defaultButton);
	}

	@Override
	protected boolean canHandleShellCloseEvent() {
		return true;
	}
	/**
	 * Creates the zone headers.
	 */
	private void createZoneHeaders() {
		Label blank = new Label(container, SWT.NONE);
		blank.setText("");
		Label xLabel = new Label(container, SWT.NONE);
		xLabel.setText("Easting");
		Label yLabel = new Label(container, SWT.NONE);
		yLabel.setText("Northing");
		Label zLabel = new Label(container, SWT.NONE);
		zLabel.setText("Depth");
	}
	/**
	 * Creates the zone text boxes.
	 */
	private void createZone() {

		Label wellLabel = new Label(container, SWT.NONE);
		wellLabel.setText("Include Well " + myWellCounter + ":");

		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;

		Text xLabel = new Text(container, SWT.BORDER);
		xLabel.setLayoutData(gridData);
		addLabelListeners(xLabel, counterForCoordinate);
		counterForCoordinate++;

		Text yLabel = new Text(container, SWT.BORDER);
		yLabel.setLayoutData(gridData);
		addLabelListeners(yLabel, counterForCoordinate);
		counterForCoordinate++;

		Text zLabel = new Text(container, SWT.BORDER);
		zLabel.setLayoutData(gridData);
		addLabelListeners(zLabel, counterForCoordinate);
		counterForCoordinate++;
	}

	private void createAddButton() {

		Button b = new Button(container, SWT.PUSH);
		b.setText("Add Another Well");
		b.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				b.dispose();
				myWellCounter++;
				buildThings();
				sc.getVerticalBar().setSelection(sc.getVerticalBar().getMaximum());

			}
		});
	}

//	private void createRemoveButton(final int indexToRemove) {
//
//		Button b = new Button(container, SWT.PUSH);
//		b.setText("Remove Zone");
//		b.addListener(SWT.Selection, new Listener(){
//			@Override
//			public void handleEvent(Event arg0) {
//				numZones--;
//				zones.remove(indexToRemove);
//				for(int i=0; i<7; ++i) readyToGo.put(indexToRemove*7+i, true);
//				buildThings();
//				sc.getVerticalBar().setSelection(sc.getVerticalBar().getMaximum());
//				//createPorosity(container);
//			}
//		});
//	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected void buttonPressed(int id) {
		if (id == OK) {
			CoordinateSystemDialog coordinateDialog = new CoordinateSystemDialog(container.getShell(),
					true, true, false);
			coordinateDialog.open();
			float minX = coordinateDialog.getMinX();
			float minY = coordinateDialog.getMinY();
			
			List<Float> temp = new ArrayList<Float>();
			for (Integer ints : mapToValue.keySet()) {
				temp.add(mapToValue.get(ints));
			}
			// Since their are 3 coordinates the ordering is always going to be (x,y,z) in
			// our list.
			// That is why I'm grabbing the first 3 values and iterating by 3 every loop.
			for (int i = 0; i < temp.size(); i += 3) {
				float offsetX = temp.get(i) - minX;
				float offsetY = temp.get(i + 1) - minY;
				myWells.add(new ExistingWell(temp.get(i), temp.get(i + 1), temp.get(i + 2), offsetX, offsetY));
			}
			IncludeLocationResults printOut = new IncludeLocationResults(
					myData.getSet().getNodeStructure().getEdgeX(),
					myData.getSet().getNodeStructure().getEdgeY(),
					myData.getSet().getNodeStructure().getEdgeZ(),
					myWells, myData, coordinateDialog.getOutputDir());
			printOut.printOutResults();
			super.okPressed();
		}
	}

	private void addLabelListeners(final Text theText, final int theNumberAtThisPoint) {
		theText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String temp = ((Text) e.getSource()).getText();
				try {
					float numberEntered = Float.valueOf(temp);
					checkAllInputs.put(theNumberAtThisPoint, true);
					mapToValue.put(theNumberAtThisPoint, numberEntered);
					if (checkInput()) {
						getButton(OK).setEnabled(true);
					}
					((Text) e.getSource()).setForeground(Constants.black);
				} catch (NumberFormatException f) {
					checkAllInputs.put(theNumberAtThisPoint, false);
					getButton(OK).setEnabled(false);
					((Text) e.getSource()).setForeground(Constants.red);
				}
			}
		});
	}

	private boolean checkInput() {
		for (Integer ints : checkAllInputs.keySet()) {
			// If a value is false
			if (!checkAllInputs.get(ints)) {
				return false;
			}
		}
		return true;
	}
	public List<ExistingWell> getMyWells() {
		return myWells;
	}
}
