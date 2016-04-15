package wizardPages;

import java.util.ArrayList;
import java.util.List;

import objects.ExtendedSensor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import wizardPages.DREAMWizard.STORMData;

public class Page_InitialConfiguraiton extends WizardPage implements AbstractWizardPage {

	STORMData data;
	Composite container;
	List<ExtendedSensor> sensors;
	
	private boolean isCurrentPage = false;

	protected Page_InitialConfiguraiton(STORMData data) {
		super("Initial Sensors");
	//	setDescription("Initial Sensors");
		sensors = new ArrayList<ExtendedSensor>();	
		this.data = data;

	}

	@Override
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NULL);

		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 12;
		layout.verticalSpacing = 12;
		container.setLayout(layout);
		layout.numColumns = 2;
		
		setControl(container);		
	}

	@Override
	public void loadPage() {
		
		isCurrentPage = true;
		for(Control control: container.getChildren()) {
			control.dispose(); // Remove the children.
		}
		container.layout();	
		
		Label spacerLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		GridData spacerLabelData = new GridData(GridData.BEGINNING);
		spacerLabelData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns - 1;
		spacerLabelData.verticalSpan = 2;
		spacerLabel.setLayoutData(spacerLabelData);
		GridData infoLinkData = new GridData(GridData.FILL_HORIZONTAL);
		infoLinkData.horizontalSpan = 1;
		infoLinkData.verticalSpan = 2;
		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				// TODO: Catherine edit text here!
				MessageDialog.openInformation(container.getShell(), "Additional information!", "TODO");	
			}			
		});
		infoLink.setLayoutData(infoLinkData);
		
		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("Details about the tool.");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 4;
		infoLabel.setLayoutData(infoGridData);

		sensors.add(new ExtendedSensor(1, "Pressure", data.getSet().getNodeStructure()));
		
	    final Text text = new Text(container, SWT.BORDER);
	    text.setBounds(25, 240, 220, 25);
	    
		Table table = new Table(container, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		    table.setHeaderVisible(true);
		    String[] titles = { "Node number", "Data type"};//, "X", "Y", "Z", "I", "J", "K", "Node number"};

		    for (int loopIndex = 0; loopIndex < titles.length; loopIndex++) {
		      TableColumn column = new TableColumn(table, SWT.NULL);
		      column.setText(titles[loopIndex]);
		    }
		    
		    for(ExtendedSensor sensor: sensors) {
		    	TableItem item = new TableItem(table, SWT.NULL);
		    	item.setText(String.valueOf(sensor.getNodeNumber()));
		    	Combo combo = new Combo(container,  SWT.DROP_DOWN | SWT.READ_ONLY);
		    	for(String dataType: data.getSet().getDataTypes()) {
		    		combo.add(dataType);
		    	}
		    	combo.setText(sensor.getSensorType()); // This won't work, need anther approach...
		  //  	item.set
		    }
		    /*
		    for (int loopIndex = 0; loopIndex < 24; loopIndex++) {
		      TableItem item = new TableItem(table, SWT.NULL);
		      item.
		      item.setText("Item " + loopIndex);
		      item.setText(0, "Item " + loopIndex);
		      item.setText(1, "Yes");
		      item.setText(2, "No");
		      item.setText(3, "A table item");
		    }*/
		    
		    for (int loopIndex = 0; loopIndex < titles.length; loopIndex++) {
		      table.getColumn(loopIndex).pack();
		    }
		    

		    table.setBounds(25, 25, 220, 200);

		    table.addListener(SWT.Selection, new Listener() {
		      public void handleEvent(Event event) {
		        if (event.detail == SWT.CHECK) {
		          text.setText("You checked " + event.item);
		        } else {
		          text.setText("You selected " + event.item);
		        }
		      }
		    });

		    container.layout();	
		    

			DREAMWizard.visLauncher.setEnabled(true);
			DREAMWizard.convertDataButton.setEnabled(false);
		    
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
	}
	
	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}
	@Override
	public void setPageCurrent(boolean current) {
		isCurrentPage = current;
	}
}
