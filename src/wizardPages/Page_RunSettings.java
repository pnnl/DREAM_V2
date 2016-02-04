package wizardPages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import utilities.Constants;
import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;
import wizardPages.Page_SensorSetup.SensorData;

public class Page_RunSettings extends WizardPage implements AbstractWizardPage {

	private STORMData data;
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	// private Text iterations;
	private Text costConstraint;
	private Text addPoint;
	private Text maxWells;
	private Text exclusionRadius;
	private Button allowMultipleSensorsInWell;
	private Button averageTTD;

	private boolean isCurrentPage = false;

	protected Page_RunSettings(STORMData data) {
		super("Run setup");
		//	setDescription("Run setup");
		this.data = data;			
	}

	@Override
	public void createControl(Composite parent) {
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
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 12;
		layout.verticalSpacing = 12;
		container.setLayout(layout);
		layout.numColumns = 2;

		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		setControl(rootContainer);
		setPageComplete(true);
	}

	@Override
	public void loadPage() {
		isCurrentPage = true;
		for(Control control: container.getChildren()) {
			control.dispose(); // Remove the children.
		}
		container.layout();	
		Font boldFont = new Font( container.getDisplay(), new FontData( "Arial", 10, SWT.BOLD ) );		

		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("Configuration Settings");
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData.verticalSpan = 4;
		infoLabel.setLayoutData(infoGridData);

		infoLabel.setFont(boldFont);

		/* Moving iterations to review and run page
		Label iterationLabel = new Label(container, SWT.NULL);
		iterationLabel.setText("Iterations");
		iterations= new Text(container, SWT.BORDER | SWT.SINGLE);
		iterations.setText(String.valueOf(data.getSet().getCostConstraint()));
		GridData costGD = new GridData(GridData.FILL_HORIZONTAL);
		iterations.setLayoutData(costGD);
		 */
		Label costLabel = new Label(container, SWT.NULL);
		costLabel.setText("Sensor Budget");
		costConstraint= new Text(container, SWT.BORDER | SWT.SINGLE);
		costConstraint.setText(String.valueOf(data.getSet().getCostConstraint()));
		GridData iterGD = new GridData(GridData.FILL_HORIZONTAL);
		costConstraint.setLayoutData(iterGD);
		costConstraint.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					Float.parseFloat(((Text)e.getSource()).getText());	
					((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 0, 0, 0));
					testReady();
				} catch (NumberFormatException ne) {
					((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
					testReady();
				}
			}				
		});

		Label wellLabel = new Label(container, SWT.NULL);
		wellLabel.setText("Maximum Number of Wells");
		maxWells= new Text(container, SWT.BORDER | SWT.SINGLE);
		maxWells.setText(String.valueOf(data.getSet().getMaxWells()));
		GridData maxWellGD = new GridData(GridData.FILL_HORIZONTAL);
		maxWells.setLayoutData(maxWellGD);
		maxWells.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					Float.parseFloat(((Text)e.getSource()).getText());	
					((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 0, 0, 0));
					testReady();
				} catch (NumberFormatException ne) {
					((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
					testReady();
				}
			}				
		});

		Label exclusionRadiusLabel = new Label(container, SWT.NULL);
		exclusionRadiusLabel.setText("Minimum Distance Between Wells");
		exclusionRadius= new Text(container, SWT.BORDER | SWT.SINGLE);
		exclusionRadius.setText(String.valueOf(data.getSet().getExclusionRadius()));
		GridData exclusionRadiusGD = new GridData(GridData.FILL_HORIZONTAL);
		exclusionRadius.setLayoutData(exclusionRadiusGD);
		exclusionRadius.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				try {
					Float.parseFloat(((Text)e.getSource()).getText());	
					((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 0, 0, 0));
					testReady();
				} catch (NumberFormatException ne) {
					((Text)e.getSource()).setForeground(new Color(container.getDisplay(), 255, 0, 0));
					testReady();
				}
			}				
		});

		Label addLabel = new Label(container, SWT.NULL);
		addLabel.setText("Add point");
		addPoint= new Text(container, SWT.BORDER | SWT.SINGLE);
		addPoint.setText(data.getSet().getAddPoint().toString());
		GridData addGD = new GridData(GridData.FILL_HORIZONTAL);
		addPoint.setLayoutData(addGD);

		addPoint.setVisible(Constants.buildDev);
		addLabel.setVisible(Constants.buildDev);		
		
		averageTTD = new Button(container, SWT.CHECK);
		averageTTD.setText("Use average time to detection");
		new Label(container, SWT.NULL);
		averageTTD.setSelection(true);

		averageTTD.setVisible(Constants.buildDev);

		allowMultipleSensorsInWell = new Button(container, SWT.CHECK);
		allowMultipleSensorsInWell.setText("Allow Multiple Sensors in a Well");
		new Label(container, SWT.NULL);
		allowMultipleSensorsInWell.setSelection(data.getSet().getAllowMultipleSensorsInWell());

		container.layout();	
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		
		DREAMWizard.visLauncher.setEnabled(true);
		DREAMWizard.convertDataButton.setEnabled(false);
	}

	/*
	public int getIterations() {
		try {
			return Integer.parseInt(iterations.getText());
		} catch(Exception e) {
			return 100;
		}
	}
	 */

	public float getCostConstraint() { 
		try {
			return Float.parseFloat(costConstraint.getText());
		} catch (Exception e) {
			return 0.0f;
		}
	}

	public Point3i getAddPoint() {
		try {
			String addPointText = addPoint.getText().replaceAll("[() ]*", "");			
			String[] tokens = addPointText.split(",");
			return new Point3i(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
		} catch(Exception e) {
			return new Point3i(1,1,1);
		}
	}

	public int getMaxWells() {
		try {
			return Integer.parseInt(maxWells.getText());
		} catch(Exception e) {
			return 0;
		}
	}

	public float getExclusionRadius() {
		try {
			return Float.parseFloat(exclusionRadius.getText());
		} catch(Exception e) {
			return 0;
		}
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
		Constants.returnAverageTTD = averageTTD.getSelection();
		data.getSet().setUserSettings(getAddPoint(), getMaxWells(), getCostConstraint(), getExclusionRadius(), allowMultipleSensorsInWell.getSelection());
	}

	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}
	@Override
	public void setPageCurrent(boolean current) {
		isCurrentPage = current;
	}


	private void testReady() {
		boolean isReady = true;

		if(costConstraint.getForeground().equals(new Color(container.getDisplay(), 255, 0, 0))) {
			isReady = false;
		}

		if(maxWells.getForeground().equals(new Color(container.getDisplay(), 255, 0, 0))) {
			isReady = false;

		}


		if(this.isPageComplete() != isReady)
			this.setPageComplete(isReady);
	}
}
