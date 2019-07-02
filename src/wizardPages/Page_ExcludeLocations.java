package wizardPages;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import mapView.CoordinateSystemDialog;
import mapView.DREAMMap;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;

/**
 * Page with checkboxes for choosing which wells to include in the final run.
 * See line 180
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
	
	private int minI = Integer.MAX_VALUE;
	private int maxI = -Integer.MAX_VALUE;
	private int minJ = Integer.MAX_VALUE;
	private int maxJ = -Integer.MAX_VALUE;
	private List<Integer> validXWells = new ArrayList<>();
	private List<Integer> validYWells = new ArrayList<>();
	private boolean isCurrentPage = false;
	public Page_ExcludeLocations(final STORMData data) {
		super("Exclude Locations");
		this.data = data;		
	}
	@Override
	public void createControl(final Composite parent) {
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
		// Add check boxes for all valid well locations
		if(data.needToResetWells){
			//This means that we have set new parameters and need to reset all values
			data.needToResetWells = false;
			wells = data.getSet().getAllPossibleWells();
			selection.clear();
			minI = Integer.MAX_VALUE;
			minJ = Integer.MAX_VALUE;	
			maxI = Integer.MIN_VALUE;
			maxJ = Integer.MIN_VALUE;
		}

		for(Integer i: wells.keySet()) {			
			if(i < minI) {
				minI = i;
			} 
			if(i > maxI) {
				maxI = i;
			}
			for(Integer j: wells.get(i)) {
				if(j < minJ) {
					minJ = j;
				}
				if(j > maxJ) {
					maxJ = j;
				}
			}				
		}
		validXWells.clear();
		validYWells.clear();
		populateXandYAxisWells();
		layout.numColumns = validXWells.size() + 1;
		Font boldFont = new Font( container.getDisplay(), new FontData( "Helvetica", 12, SWT.BOLD ) );		
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Exclude Locations");
		GridData infoGridData1 = new GridData(GridData.FILL);
		infoGridData1.horizontalSpan = ((GridLayout)container.getLayout()).numColumns - 1;
		infoGridData1.verticalSpan = 4;
		infoLabel1.setLayoutData(infoGridData1);
		infoLabel1.setFont(boldFont);
		
		GridData infoLinkData = new GridData(GridData.FILL);
		infoLinkData.horizontalSpan = 1;
		infoLinkData.verticalSpan = 4;
		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.setAlignment(SWT.RIGHT);
		infoLink.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Additional information", "The user may need to exclude (x,y) locations from the monitoring configuration that are infeasible or unapproved. "
						+ "This window allows the user to manually deselect nodes that should not be used in the optimization algorithm.\nIf the user has an internet connection, the Launch Google map button "
						+ "will pop-up a map which the user can use to overlay the scenario grid over the location of interest. The input coordinates should align with the upper left corner of the simulation "
						+ "grid (in plan view). The toolbar allows the user to zoom in or out and pivot the google map to achieve the appropriate view. Nodes that exceed one or more of the threshold criteria "
						+ "are shown in gray and may be de-selected by clicking on the (x,y) location.");	
			}			
		});
		infoLink.setLayoutData(infoLinkData);

		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("Deselect unapproved or infeasible monitoring locations.");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 4;
		infoLabel.setLayoutData(infoGridData);
		

		Button launchMapButton = new Button(container, SWT.BUTTON1);		
		GridData launchButtonData = new GridData(GridData.BEGINNING);
		launchButtonData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		launchButtonData.verticalSpan = 4;
		launchMapButton.setLayoutData(launchButtonData);
		
		Label corner = new Label(container, SWT.NULL);
		/* This page can only handle so many checkboxes.
		 * If we exceed this amount, we disable this functionality and display none. */
		if(validXWells.size() * validYWells.size() > 9000){
			corner.setText("Domain is too large for individual well exclusion.");
		}
		else {
			corner.setText("Y | X"); 
			for(int xVals: validXWells) {
				Label label = new Label(container, SWT.NULL);
				label.setText(String.valueOf(data.getSet().getNodeStructure().getX().get(xVals-1)));
		}	
			for(int yVals: validYWells) {
				Label label = new Label(container, SWT.NULL);
				label.setText(String.valueOf(data.getSet().getNodeStructure().getY().get(yVals-1)));
				for(int xVals: validXWells) {		
					// Wells
					if(wells.containsKey(xVals) && wells.get(xVals).contains(yVals)) {
						Button wellButton = new Button(container, SWT.CHECK);
						wellButton.setSelection(true);
						if(selection.containsKey(xVals) && selection.get(xVals).containsKey(yVals)) {
							// Already have a button here, save the state of it
							wellButton.setSelection(selection.get(xVals).get(yVals));
							if(!selection.get(xVals).get(yVals))
								System.out.println("Restoring the state of a previously saved button selection: "
							+ selection.get(xVals).get(yVals));
						}
						if(!buttons.containsKey(xVals)) {
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
		
		launchMapButton.setText("Launch Google map (requires internet connection)");
		launchMapButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event event) {
				CoordinateSystemDialog dialog = new CoordinateSystemDialog(container.getShell());
				dialog.open();
				final List<IJ> ijs = new ArrayList<IJ>();
				for(int i = minI; i <= maxI; i++) {
					for(int j = minJ; j <= maxJ; j++) {	
						boolean selectable = buttons.containsKey(i) && buttons.get(i).containsKey(j);
						ijs.add(new IJ(i, j, selectable ? buttons.get(i).get(j).getSelection() : false, selectable));						
					}
				}

				// Can I do this from the swt thread?
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						DREAMMap map = new DREAMMap(ijs, 
								new ArrayList<Float>(data.getSet().getNodeStructure().getX()),
								new ArrayList<Float>(data.getSet().getNodeStructure().getY()),
										dialog.getZone(), dialog.getZoneDirection(), dialog.getMinX(),
										dialog.getMinY(), data.getSet().getNodeStructure().getUnit("x"));
						
						map.viewer.addWindowListener(new WindowAdapter() {

							@Override
							public void windowClosed(WindowEvent e) {
								//Transform the data from the google map view back to the format we use here
								System.out.println("Window is closing!!");
								for(IJ ij: ijs) {	
									if(buttons.containsKey(ij.i) && buttons.get(ij.i).containsKey(ij.j)) {
										final int i = ij.i;
										final int j = ij.j;
										final boolean select = ij.prohibited;
										Display.getDefault().syncExec(new Runnable() {
										    public void run() {
										    	buttons.get(i).get(j).setSelection(select);
										    }
										});
									}										
								}								
							}
						});
					}

				});

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
		for(Integer i: buttons.keySet()) {
			for(Integer j: buttons.get(i).keySet()) {
				if(!selection.containsKey(i)) {
					selection.put(i, new HashMap<Integer, Boolean>());
				}
				selection.get(i).put(j, buttons.get(i).get(j).getSelection());

				if(!buttons.get(i).get(j).getSelection()) {
					System.out.println("Found a button that wasn't selected: " + i + ", " + j);
					// Not selected, remove this from the spaces we can go
					for(SensorSetting setting: data.getSet().getSensorSettings().values()) {
						List<Integer> nodes = new ArrayList<Integer>();
						for(Integer nodeNumber: setting.getValidNodes()) {
							Point3i ijk = data.getSet().getNodeStructure().getIJKFromNodeNumber(nodeNumber);
							if(ijk.getI() == i && ijk.getJ() == j) {
								nodes.add(nodeNumber);
							}
						}
						for(Integer node: nodes) {
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
	 * @author huan482
	 * 5/9/2019
	 * Populates 2 ArrayList with the x and y values of a well respectively.
	 */
	private void populateXandYAxisWells() {
		for (int i = minI; i <= maxI; i++) {
			if (wells.containsKey(i)) {
				validXWells.add(i);
			}
		}
		for (int j = minJ; j <= maxJ; j++) {
			for (int temp: validXWells) {
				if (wells.containsKey(temp) && wells.get(temp).contains(j) && !validYWells.contains(j)) {
					validYWells.add(j);
				}
			}
		}
	}
}
