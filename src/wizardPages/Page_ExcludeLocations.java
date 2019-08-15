package wizardPages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleBinaryOperator;

import mapView.CoordinateSystemDialog;
import mapView.ExistingWellsDialogBox;
import mapView.GMapInitVar;
import mapView.GMapView;
import mapView.IJ;
import objects.SensorSetting;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import javafx.application.Application;
import javafx.application.Platform;
import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;

/**
 * Page with checkboxes for choosing which wells to include in the final run.
 * See line 180
 * 
 * @author port091
 * @author rodr144
 * @author huan482
 */

public class Page_ExcludeLocations extends DreamWizardPage implements AbstractWizardPage {

	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer; 
	private STORMData data;

	private GridLayout layout;
	private Map<Integer, Map<Integer, Button>> buttons;
	private Map<Integer, List<Integer>> wells;
	private Map<Integer, Map<Integer, Boolean>> selection;

	private List<Float> myWellLocationsX;

	private List<Float> myWellLocationsY;
	
	private List<Point3i> myValidNodePoints = new ArrayList<Point3i>();
	
	private int minI = Integer.MAX_VALUE;
	private int maxI = -Integer.MAX_VALUE;
	private int minJ = Integer.MAX_VALUE;
	private int maxJ = -Integer.MAX_VALUE;

	private List<Integer> validXWells = new ArrayList<>();
	private List<Integer> validYWells = new ArrayList<>();

	private boolean isCurrentPage = false;

	private List<IJ> ijs;
	
	private boolean offsetRequired;
	
	private boolean offsetDone;
	
	public Page_ExcludeLocations(final STORMData data) {
		super("Exclude Locations");
		this.data = data;
	}

	@Override
	public void createControl(final Composite parent) {
		ijs = new ArrayList<IJ>();
		myWellLocationsX = new ArrayList<Float>();
		myWellLocationsY = new ArrayList<Float>();
		buttons = new HashMap<Integer, Map<Integer, Button>>();
		selection = new HashMap<Integer, Map<Integer, Boolean>>();
		rootContainer = new Composite(parent, SWT.NULL);
		rootContainer.setLayout(GridLayoutFactory.fillDefaults().create());

		sc = new ScrolledComposite(rootContainer, SWT.V_SCROLL | SWT.H_SCROLL);
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

		container = new Composite(sc, SWT.NULL);

		layout = new GridLayout();
		layout.horizontalSpacing = 4;
		layout.verticalSpacing = 4;
		container.setLayout(layout);
		layout.numColumns = 5;

		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		setControl(rootContainer);
		setPageComplete(true);
	}

	@Override
	public void loadPage() {
		DREAMWizard.nextButton.setVisible(true);
		DREAMWizard.errorMessage.setText("");
		isCurrentPage = true;
		removeChildren(container);
		container.layout();
		buttons.clear();
		ijs.clear();
		myWellLocationsX.clear();
		myWellLocationsY.clear();
		myValidNodePoints.clear();
		// Add check boxes for all valid well locations
		if (data.needToResetWells) {
			// This means that we have set new parameters and need to reset all values
			data.needToResetWells = false;
			wells = data.getSet().getAllPossibleWells();
			selection.clear();
			minI = Integer.MAX_VALUE;
			minJ = Integer.MAX_VALUE;
			maxI = Integer.MIN_VALUE;
			maxJ = Integer.MIN_VALUE;
		}

		for (Integer i : wells.keySet()) {
			if (i < minI) {
				minI = i;
			}
			if (i > maxI) {
				maxI = i;
			}
			for (Integer j : wells.get(i)) {
				if (j < minJ) {
					minJ = j;
				}
				if (j > maxJ) {
					maxJ = j;
				}
			}
		}
		validXWells.clear();
		validYWells.clear();
		populateXandYAxisWells();
		populatePointList();
		layout.numColumns = validXWells.size() + 1;
		Font boldFont = new Font(container.getDisplay(), new FontData("Helvetica", 12, SWT.BOLD));
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP);
		infoLabel1.setText("Exclude Locations");
		GridData infoGridData1 = new GridData(GridData.FILL);
		infoGridData1.horizontalSpan = ((GridLayout) container.getLayout()).numColumns - 1;
		infoGridData1.verticalSpan = 4;
		infoLabel1.setLayoutData(infoGridData1);
		infoLabel1.setFont(boldFont);

		GridData infoLinkData = new GridData(GridData.FILL);
		infoLinkData.horizontalSpan = 1;
		infoLinkData.verticalSpan = 4;
		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.setAlignment(SWT.RIGHT);
		infoLink.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Additional information",
						"The user may need to exclude (x,y) locations from the monitoring configuration that are infeasible or unapproved. "
								+ "This window allows the user to manually deselect nodes that should not be used in the optimization algorithm.\nIf the user has an internet connection, the Launch Google map button "
								+ "will pop-up a map which the user can use to overlay the scenario grid over the location of interest. The input coordinates should align with the upper left corner of the simulation "
								+ "grid (in plan view). The toolbar allows the user to zoom in or out and pivot the google map to achieve the appropriate view. Nodes that exceed one or more of the threshold criteria "
								+ "are shown in gray and may be de-selected by clicking on the (x,y) location.");
			}
		});
		infoLink.setLayoutData(infoLinkData);

		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP);
		infoLabel.setText("Deselect unapproved or infeasible monitoring locations.");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout) container.getLayout()).numColumns;
		infoGridData.verticalSpan = 4;
		infoLabel.setLayoutData(infoGridData);

		Button launchMapButton = new Button(container, SWT.BUTTON1);
		GridData launchButtonData = new GridData(GridData.BEGINNING);
		launchButtonData.horizontalSpan = ((GridLayout) container.getLayout()).numColumns;
		launchButtonData.verticalSpan = 4;
		launchMapButton.setLayoutData(launchButtonData);
		
		Button launchExistingWellButton = new Button(container, SWT.BUTTON1);
		GridData launchExistingData = new GridData(GridData.BEGINNING);
		launchExistingData.horizontalSpan = ((GridLayout) container.getLayout()).numColumns;
		launchExistingData.verticalSpan = 4;
		launchExistingWellButton.setLayoutData(launchExistingData);
		launchExistingWellButton.setText("Include Locations");
		Label corner = new Label(container, SWT.NULL);
		/*
		 * This page can only handle so many checkboxes. If we exceed this amount, we
		 * disable this functionality and display none.
		 */
		if (validXWells.size() * validYWells.size() > 9000) {
			corner.setText("Domain is too large for individual well exclusion.");
		} else {
			corner.setText("Y | X");
			for (int xVals : validXWells) {
				Label label = new Label(container, SWT.NULL);
				label.setText(String.valueOf(data.getSet().getNodeStructure().getEdgeX().get(xVals - 1)));
				myWellLocationsX.add(data.getSet().getNodeStructure().getEdgeX().get(xVals - 1));
			}
			for (int yVals : validYWells) {
				Label label = new Label(container, SWT.NULL);
				label.setText(String.valueOf(data.getSet().getNodeStructure().getEdgeY().get(yVals - 1)));
				for (int xVals : validXWells) {
					// Wells
					if (wells.containsKey(xVals) && wells.get(xVals).contains(yVals)) {
						Button wellButton = new Button(container, SWT.CHECK);
						wellButton.setSelection(true);
						myWellLocationsY.add(data.getSet().getNodeStructure().getEdgeY().get(yVals - 1));
						if (selection.containsKey(xVals) && selection.get(xVals).containsKey(yVals)) {
							// Already have a button here, save the state of it
							wellButton.setSelection(selection.get(xVals).get(yVals));
							if (!selection.get(xVals).get(yVals))
								System.out.println("Restoring the state of a previously saved button selection: "
										+ selection.get(xVals).get(yVals));
						}
						if (!buttons.containsKey(xVals)) {
							buttons.put(xVals, new HashMap<Integer, Button>());
						}
						buttons.get(xVals).put(yVals, wellButton);
					} else {
						Label empty = new Label(container, SWT.NULL);
						empty.setText(" ");
					}
				}
			}
		}
		createBoxList();
		offsetRequired = Collections.min(data.getSet().getNodeStructure().getEdgeX()) == 0
				|| Collections.min(data.getSet().getNodeStructure().getEdgeY()) == 0;
		launchMapButton.setText("Launch Google map (requires internet connection)");
		launchMapButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event event) {
				CoordinateSystemDialog dialog = new CoordinateSystemDialog(container.getShell(),
						offsetRequired, false, false);
				dialog.open();
				offsetCalculation(dialog, false, (x , y) -> x + y);
				if  (dialog.getButtonPressed()) {
					GMapInitVar map = new GMapInitVar(
							ijs,
							new ArrayList<Float>(data.getSet().getNodeStructure().getEdgeX()),
							new ArrayList<Float>(data.getSet().getNodeStructure().getEdgeY()),
							dialog.getZone(),
							dialog.getZoneDirection(),
							data.getSet().getNodeStructure().getUnit("x"),
							myValidNodePoints);
					map.initVariables();
					Application.launch(GMapView.class);
					
					if (Platform.isImplicitExit()) {
						for (IJ box : ijs) {
							if (buttons.get(box.i).get(box.j) != null) {
								buttons.get(box.i).get(box.j).setSelection(box.prohibited);
							}
						}
					}
				}
				if (offsetDone) {
					offsetCalculation(dialog, false, (x , y) -> x - y);
					offsetDone = false;
				}
			}
		});
		
		launchExistingWellButton.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(final Event event) {
				ExistingWellsDialogBox wellDialog = new ExistingWellsDialogBox(container.getShell(), data);
				wellDialog.open();
			}
			
		});
		container.layout();
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		DREAMWizard.visLauncher.setEnabled(true);
		DREAMWizard.convertDataButton.setEnabled(false);
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
		// Need to remove from the set of available wells the ones we selected
		for (Integer i : buttons.keySet()) {
			for (Integer j : buttons.get(i).keySet()) {
				if (!selection.containsKey(i)) {
					selection.put(i, new HashMap<Integer, Boolean>());
				}
				selection.get(i).put(j, buttons.get(i).get(j).getSelection());

				if (!buttons.get(i).get(j).getSelection()) {
					System.out.println("Found a button that wasn't selected: " + i + ", " + j);
					// Not selected, remove this from the spaces we can go
					for (SensorSetting setting : data.getSet().getSensorSettings().values()) {
						List<Integer> nodes = new ArrayList<Integer>();
						for (Integer nodeNumber : setting.getValidNodes()) {
							Point3i ijk = data.getSet().getNodeStructure().getIJKFromNodeNumber(nodeNumber);
							myValidNodePoints.add(ijk);
							if (ijk.getI() == i && ijk.getJ() == j) {
								nodes.add(nodeNumber);
							}
						}
						for (Integer node : nodes) {
							setting.removeNode(node);
							System.out.println("Removing node #" + node + " from " + setting.toString());
						}
					}
				}
			}
		}
	}

	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}

	@Override
	public void setPageCurrent(final boolean current) {
		isCurrentPage = current;
	}

	/**
	 * @author huan482 5/9/2019 Populates 2 ArrayList with the x and y values of a
	 *         well respectively.
	 */
	private void populateXandYAxisWells() {
		for (int i = minI; i <= maxI; i++) {
			if (wells.containsKey(i)) {
				validXWells.add(i);
			}
		}
		for (int j = minJ; j <= maxJ; j++) {
			for (int temp : validXWells) {
				if (wells.containsKey(temp) && wells.get(temp).contains(j) && !validYWells.contains(j)) {
					validYWells.add(j);
				}
			}
		}
	}
	
	private void populatePointList() {
		for (SensorSetting setting : data.getSet().getSensorSettings().values()) {
			for (Integer nodeNumber : setting.getValidNodes()) {
				Point3i ijk = data.getSet().getNodeStructure().getIJKFromNodeNumber(nodeNumber);
				myValidNodePoints.add(ijk);
			}
		}
	}
	
	private void createBoxList() {
		for (int i = minI; i <= maxI; i++) {
			for (int j = minJ; j <= maxJ; j++) {
				boolean selectable = buttons.containsKey(i) && buttons.get(i).containsKey(j);
				if (selectable) {
					ijs.add(new IJ(i,j, selectable ? buttons.get(i).get(j).getSelection() : false, selectable));
				}
			}
		}
	}
	//Calculates the well locations after the offset is applied.
	private void offsetCalculation (final CoordinateSystemDialog dialog, final boolean includeButton,
			final DoubleBinaryOperator theOperation) {
		if (offsetRequired) {
			offsetDone = true;
			int sizeX = data.getSet().getNodeStructure().getEdgeX().size();
			for (int i = 0; i < sizeX; i++) {
				double temp = theOperation.applyAsDouble(
						(double) data.getSet().getNodeStructure().getEdgeX().get(i),
						(double) dialog.getMinX());
				
				data.getSet().getNodeStructure().getEdgeX().set(i, (float) temp);
				
			}
			int sizeY = data.getSet().getNodeStructure().getEdgeY().size();
			for (int i = 0; i < sizeY; i++) {
				double temp = theOperation.applyAsDouble(
						(double) data.getSet().getNodeStructure().getEdgeY().get(i),
						(double) dialog.getMinY());
				data.getSet().getNodeStructure().getEdgeY().set(i, (float) temp);
			}
		}
	}
}
