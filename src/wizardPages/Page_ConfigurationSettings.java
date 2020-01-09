package wizardPages;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import utilities.Constants;
import utilities.Point3i;
import wizardPages.DREAMWizard.STORMData;

/**
 * Set algorithmic limitations for the simulated annealing process. See line 131
 * 
 * @author port091
 * @author rodr144
 * @author whit162
 * @author huan482
 */

public class Page_ConfigurationSettings extends DreamWizardPage implements AbstractWizardPage {

	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private STORMData data;

	private Text costConstraint;
	private Text addPoint;
	private Text maxWells;
	private Text exclusionRadius;
	private Text wellCost;
	private Text wellDepthCost;
	private Text remediationCost;
	private Font myBoldFont;
	private String unit;
	private float cost = 0; // Since data.getSet().getCostConstraint is set at the end of the previous page,
							// use local variable

	private boolean isCurrentPage = false;

	protected Page_ConfigurationSettings(STORMData data) {
		super("Configuration Settings");
		// setDescription("Run setup");
		this.data = data;

	}

	@Override
	public void createControl(final Composite parent) {
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

	/**
	 * Loads Configuration Settings Page.
	 */
	@Override
	public void loadPage() {

		isCurrentPage = true;
		DREAMWizard.errorMessage.setText("");
		removeChildren(container);

		myBoldFont = new Font(container.getDisplay(), new FontData("Helvetica", 12, SWT.BOLD));
		unit = data.getSet().getNodeStructure().getUnit("x");

		createConfigurationSettingsLabel(myBoldFont);

		createInfoLinkLabel();

		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP);
		infoLabel.setText("Are there any cost or physical constraints on the monitoring configuration?");
		infoLabel.setLayoutData(theGridDataSpecifications());

		// Simple text label
		Label costLabel = new Label(container, SWT.NULL);
		costLabel.setText("Total Monitoring Budget");

		createCostConstraintLabel();

		createMaximumWellsLabel();

		createMinimumWellLabel();

		createWellCostLabel();

		createCostWellDepthLabel();

		if (Constants.buildDev) {
			createRemediationCostLabel();
		}

		container.layout();
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();

		DREAMWizard.visLauncher.setEnabled(true);
		DREAMWizard.convertDataButton.setEnabled(false);
	}

	/**
	 * Info label for the configuration settings.
	 * 
	 * @param TheBoldFont - The font used.
	 */
	private void createConfigurationSettingsLabel(final Font TheBoldFont) {
		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP);
		infoLabel1.setText("Configuration Settings");
		infoLabel1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
		infoLabel1.setFont(TheBoldFont);
	}

	/**
	 * This method creates the information that details the additional information.
	 */
	private void createInfoLinkLabel() {
		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.setAlignment(SWT.RIGHT);
		infoLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
		infoLink.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				MessageDialog.openInformation(container.getShell(), "Additional information",
						"The configuration settings specify hard constraints for the optimization algorithm. "
								+ "The solutions may not exceed the maximum cost, maximum number of wells,"
								+ " or include wells closer than the minimum specified distance. "
								+ "The user may not specify a cost that is less than the minimum requirement for the tests defined"
								+ " on the previous page. ");
			}
		});
	}

	/**
	 * This method sets the GridData's specifications.
	 * 
	 * @return infoGridData - The specifications for the GridData.
	 */
	private GridData theGridDataSpecifications() {
		GridData infoGridData = new GridData(GridData.FILL_HORIZONTAL);
		infoGridData.horizontalSpan = ((GridLayout) container.getLayout()).numColumns;
		infoGridData.verticalSpan = 2;
		return infoGridData;
	}

	/**
	 * This method creates the text for the cost constraints. If there is a error
	 * (i.e not a number) text will turn red color, otherwise black.
	 */
	private void createCostConstraintLabel() {
		if (cost < data.getSet().getSensorCostConstraint())
			cost = data.getSet().getSensorCostConstraint();

		costConstraint = new Text(container, SWT.BORDER | SWT.SINGLE);
		costConstraint.setText(String.valueOf(cost));
		costConstraint.setForeground(Constants.black);
		costConstraint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		// Method reference
		costConstraint.addModifyListener(this::createCostConstraintHelper);
	}

	/**
	 * Changes font color when error is encountered.
	 * 
	 * @param theEvent - The event.
	 */
	private void createCostConstraintHelper(final ModifyEvent theEvent) {
		try {
			boolean numError = !Constants.isValidFloat(((Text) theEvent.getSource()).getText());
			boolean negError = false;
			boolean minError = false;
			if (numError && Float.parseFloat(((Text) theEvent.getSource()).getText()) < 0)
				((Text) theEvent.getSource()).setForeground(Constants.red);
			else {
				((Text) theEvent.getSource()).setForeground(Constants.black);
				if (Float.valueOf(((Text) theEvent.getSource()).getText()) < data.getSet().getSensorCostConstraint()) {
					minError = true;
					((Text) theEvent.getSource()).setForeground(Constants.red);
				} else
					cost = Float.parseFloat(((Text) theEvent.getSource()).getText());
			}
			errorFound(numError, "  Cost constraint is not a real number.");
			errorFound(minError, "  Cost constraint cannot be less the minimum sensor requirement.");
			errorFound(negError, "  Cost constraint is a negative number");
		} catch (Exception theException) {
			System.out.println("Parsing negative number. ");
		}
	}

	/**
	 * Creates the label text for the maximum number of wells. Takes directly from
	 * the data set.
	 */
	private void createMaximumWellsLabel() {
		Label wellLabel = new Label(container, SWT.NULL);
		wellLabel.setText("Maximum Number of Wells");
		maxWells = new Text(container, SWT.BORDER | SWT.SINGLE);
		maxWells.setText(String.valueOf(data.getSet().getMaxWells()));
		createWellLabels(maxWells, true);
	}

	/**
	 * Creates the label text for the minimum distance between wells.
	 */
	private void createMinimumWellLabel() {
		Label exclusionRadiusLabel = new Label(container, SWT.NULL);
		exclusionRadiusLabel.setText("Minimum Distance Between Wells" + (unit.equals("") ? "" : " (" + unit + ")"));
		exclusionRadius = new Text(container, SWT.BORDER | SWT.SINGLE);
		exclusionRadius.setText(String.valueOf(data.getSet().getExclusionRadius()));
		createWellLabels(exclusionRadius, true);
	}

	/**
	 * Creates the label text for the cost per well.
	 */
	private void createWellCostLabel() {
		Label wellCostLabel = new Label(container, SWT.NULL);
		wellCostLabel.setText("Cost Per Well");
		wellCost = new Text(container, SWT.BORDER | SWT.SINGLE);
		wellCost.setText(String.valueOf(data.getSet().getWellCost()));
		createWellLabels(wellCost, true);
	}

	/**
	 * Creates the label text for the cost per well depth.
	 */
	private void createCostWellDepthLabel() {
		Label wellDepthCostLabel = new Label(container, SWT.NULL);
		wellDepthCostLabel.setText("Cost of Well Per " + (unit == "" ? "Unit" : unit) + " Depth");
		wellDepthCost = new Text(container, SWT.BORDER | SWT.SINGLE);
		wellDepthCost.setText(String.valueOf(data.getSet().getWellDepthCost()));
		createWellLabels(wellDepthCost, true);
	}

	/**
	 * Creates the label text for remediation cost.
	 */
	private void createRemediationCostLabel() {
		Label remediationCostLabel = new Label(container, SWT.NULL);
		remediationCostLabel.setText("Remediation Cost Per " + (unit.equals("") ? "Water Unit" : unit + "³"));
		remediationCost = new Text(container, SWT.BORDER | SWT.SINGLE);
		remediationCost.setText(String.valueOf(data.getSet().getRemediationCost()));
		createWellLabels(remediationCost, false);
	}

	/**
	 * Helper method that creates the Well labels and Remediation cost label.
	 * Created to remove repeated code.
	 * 
	 * @param theWellText - The name of the Text variable.
	 * @param isWell      - Throws different error messages if isWell or otherwise.
	 * @author huan482
	 */
	private void createWellLabels(final Text theWellText, final boolean isWell) {
		theWellText.setForeground(Constants.black);
		theWellText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false)); // Lambda function
		theWellText.addModifyListener(theEvent -> {
			try {
				boolean numError = !Constants.isValidFloat(((Text) theEvent.getSource()).getText());
				boolean negError = Float.parseFloat(((Text) theEvent.getSource()).getText()) < 0;
				if (numError || negError)
					((Text) theEvent.getSource()).setForeground(Constants.red);
				else {
					((Text) theEvent.getSource()).setForeground(Constants.black);
					data.getSet().setWellDepthCost(Float.parseFloat(((Text) theEvent.getSource()).getText()));
				}
				if (isWell) {
					errorFound(negError, "Cost is a negative number. ");
					errorFound(numError, "  Cost is not a real number.");
				} else {
					errorFound(numError, "  Remediation cost is not a real number.");
				}
			} catch (Exception theException) {
				System.out.println("Parsing negative number. ");
			}
		});
	}

	/**
	 * Getter for the cost constraint.
	 * 
	 * @return costConstraint
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
		} catch (Exception e) {
			return new Point3i(1, 1, 1);
		}
	}

	public int getMaxWells() {
		try {
			return Integer.parseInt(maxWells.getText());
		} catch (Exception e) {
			return 0;
		}
	}

	public float getExclusionRadius() {
		try {
			return Float.parseFloat(exclusionRadius.getText());
		} catch (Exception e) {
			return 0;
		}
	}

	public float getWellCost() {
		try {
			return Float.parseFloat(wellCost.getText());
		} catch (Exception e) {
			return 0;
		}
	}

	public float getWellDepthCost() {
		try {
			return Float.parseFloat(wellDepthCost.getText());
		} catch (Exception e) {
			return 0;
		}
	}

	public float getRemediationCost() {
		try {
			return Float.parseFloat(remediationCost.getText());
		} catch (Exception e) {
			return 0;
		}
	}

	@Override
	public void completePage() throws Exception {
		isCurrentPage = false;
		data.getSet().setUserSettings(getAddPoint(), getMaxWells(), getCostConstraint(), getExclusionRadius(),
				getWellCost(), getWellDepthCost(), getRemediationCost());
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
