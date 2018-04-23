package wizardPages;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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

/**
 * Set algorithmic limitations for the simulated annealing process.
 * See line 131
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class Page_ConfigurationSettings extends DreamWizardPage implements AbstractWizardPage {

	private STORMData data;
	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private Text costConstraint;
	private Text addPoint;
	private Text maxWells;
	private Text exclusionRadius;
	private Text wellCost;
	private Text wellDepthCost;
	private Text remediationCost;
	private Button allowMultipleSensorsInWell;
	
	private float cost = 0; //Since data.getSet().getCostConstraint is set at the end of the previous page, use local variable
	private boolean isCurrentPage = false;

	protected Page_ConfigurationSettings(STORMData data) {
		super("Configuration Settings");
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
		DREAMWizard.errorMessage.setText("");
		for(Control control: container.getChildren()) {
			control.dispose(); // Remove the children.
		}
		container.layout();	
		Font boldFont = new Font( container.getDisplay(), new FontData( "Helvetica", 12, SWT.BOLD ) );		

		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel.setText("Configuration Settings");
		GridData infoGridData = new GridData(GridData.BEGINNING);
		infoGridData.horizontalSpan = ((GridLayout)container.getLayout()).numColumns - 1;
		infoGridData.verticalSpan = 2;
		infoLabel.setLayoutData(infoGridData);

		infoLabel.setFont(boldFont);

		GridData infoLinkData = new GridData(GridData.FILL_HORIZONTAL);
		infoLinkData.horizontalSpan = 1;
		infoLinkData.verticalSpan = 2;
		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.setAlignment(SWT.RIGHT);
		infoLink.addListener(SWT.MouseUp, new Listener(){
			@Override
			public void handleEvent(Event event) {
				// TODO: Catherine edit text here!
				MessageDialog.openInformation(container.getShell(), "Additional information", "The configuration settings specify hard constraints for the optimization algorithm. The solutions may not exceed the maximum cost or the maximum number of wells defined. The user must be careful to provide a high enough cost to meet the specified minimum requirements given in the previous window. The \"Add starting point\" field is the coordinate where the algorithm begins searching for new monitoring device placement. The Use average time to detection checkbox averages the time to first detection of leakage across all realizations using the same monitoring configuration.\nNote: DREAM assumes that wells span the entire z-axis of simulation grids and multiple detection devices may be placed within a single well.");	
			}			
		});
		infoLink.setLayoutData(infoLinkData);
		
		//Cost constraint
		Label costLabel = new Label(container, SWT.NULL);
		costLabel.setText("Sensor Budget");
		if (cost<data.getSet().getSensorCostConstraint())
			cost = data.getSet().getSensorCostConstraint();
		costConstraint = new Text(container, SWT.BORDER | SWT.SINGLE);
		costConstraint.setText(String.valueOf(cost));
		costConstraint.setForeground(Constants.black);
		costConstraint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		costConstraint.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				boolean numError = !Constants.isValidFloat(((Text)e.getSource()).getText());
				boolean minError = false;
				if (numError==true)
					((Text)e.getSource()).setForeground(Constants.red);
				else {
					((Text)e.getSource()).setForeground(Constants.black);
					if(Float.valueOf(((Text)e.getSource()).getText()) < data.getSet().getMinCost()) {
						minError = true;
						((Text)e.getSource()).setForeground(Constants.red);
					} else
						cost = Float.parseFloat(((Text)e.getSource()).getText());
				}
				errorFound(numError, "  Cost constraint is not a real number.");
				errorFound(minError, "  Cost constraint cannot be less the minimum sensor requirement.");
			}
		});

		//Maximum number of wells
		Label wellLabel = new Label(container, SWT.NULL);
		wellLabel.setText("Maximum Number of Wells");
		maxWells = new Text(container, SWT.BORDER | SWT.SINGLE);
		maxWells.setText(String.valueOf(data.getSet().getMaxWells()));
		maxWells.setForeground(Constants.black);
		maxWells.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		maxWells.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				boolean numError = !Constants.isValidInt(((Text)e.getSource()).getText());
				if (numError==true)
					((Text)e.getSource()).setForeground(Constants.red);
				else {
					((Text)e.getSource()).setForeground(Constants.black);
					data.getSet().setMaxWells(Integer.parseInt(((Text)e.getSource()).getText()));
				}
				errorFound(numError, "  Wells is not a real number.");
			}
		});

		//Minimum distance between wells
		Label exclusionRadiusLabel = new Label(container, SWT.NULL);
		exclusionRadiusLabel.setText("Minimum Distance Between Wells");
		exclusionRadius = new Text(container, SWT.BORDER | SWT.SINGLE);
		exclusionRadius.setText(String.valueOf(data.getSet().getExclusionRadius()));
		exclusionRadius.setForeground(Constants.black);
		exclusionRadius.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		exclusionRadius.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				boolean numError = !Constants.isValidFloat(((Text)e.getSource()).getText());
				if (numError==true)
					((Text)e.getSource()).setForeground(Constants.red);
				else {
					((Text)e.getSource()).setForeground(Constants.black);
					data.getSet().setExclusionRadius(Float.parseFloat(((Text)e.getSource()).getText()));
				}
				errorFound(numError, "  Distance is not a real number.");
			}
		});
		
		
		//Cost per well
		Label wellCostLabel = new Label(container, SWT.NULL);
		wellCostLabel.setText("Cost Per Well");
		wellCost = new Text(container, SWT.BORDER | SWT.SINGLE);
		wellCost.setText(String.valueOf(data.getSet().getWellCost()));
		wellCost.setForeground(Constants.black);
		wellCost.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		wellCost.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				boolean numError = !Constants.isValidFloat(((Text)e.getSource()).getText());
				if (numError==true)
					((Text)e.getSource()).setForeground(Constants.red);
				else {
					((Text)e.getSource()).setForeground(Constants.black);
					data.getSet().setWellCost(Float.parseFloat(((Text)e.getSource()).getText()));
				}
				errorFound(numError, "  Cost is not a real number.");
			}
		});
		
		//Cost per well depth
		Label wellDepthCostLabel = new Label(container, SWT.NULL);
		wellDepthCostLabel.setText("Cost of Well Per Unit Depth");
		wellDepthCost = new Text(container, SWT.BORDER | SWT.SINGLE);
		wellDepthCost.setText(String.valueOf(data.getSet().getWellCost()));
		wellDepthCost.setForeground(Constants.black);
		wellDepthCost.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		wellDepthCost.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				boolean numError = !Constants.isValidFloat(((Text)e.getSource()).getText());
				if (numError==true)
					((Text)e.getSource()).setForeground(Constants.red);
				else {
					((Text)e.getSource()).setForeground(Constants.black);
					data.getSet().setWellDepthCost(Float.parseFloat(((Text)e.getSource()).getText()));
				}
				errorFound(numError, "  Cost is not a real number.");
			}
		});
			
		if (Constants.buildDev) {
			//Remediation cost
			Label remediationCostLabel = new Label(container, SWT.NULL);
			remediationCostLabel.setText("Remediation Cost Per Water Unit");
			remediationCost = new Text(container, SWT.BORDER | SWT.SINGLE);
			remediationCost.setText(String.valueOf(data.getSet().getRemediationCost()));
			remediationCost.setForeground(Constants.black);
			remediationCost.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			remediationCost.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					boolean numError = !Constants.isValidFloat(((Text)e.getSource()).getText());
					if (numError==true)
						((Text)e.getSource()).setForeground(Constants.red);
					else {
						((Text)e.getSource()).setForeground(Constants.black);
						data.getSet().setRemediationCost(Float.parseFloat(((Text)e.getSource()).getText()));
					}
					errorFound(numError, "  Remediation cost is not a real number.");
				}
			});
		}

		//Allow multiple sensors per well check box
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
	
	public float getWellCost() {
		try {
			return Float.parseFloat(wellCost.getText());
		} catch(Exception e) {
			return 0;
		}
	}
	
	public float getWellDepthCost() {
		try {
			return Float.parseFloat(wellDepthCost.getText());
		} catch(Exception e) {
			return 0;
		}
	}
	
	public float getRemediationCost() {
		try {
			return Float.parseFloat(remediationCost.getText());
		} catch(Exception e) {
			return 0;
		}
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
		data.getSet().setUserSettings(getAddPoint(), getMaxWells(), getCostConstraint(), getExclusionRadius(), getWellCost(), getWellDepthCost(), getRemediationCost(), allowMultipleSensorsInWell.getSelection());
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
