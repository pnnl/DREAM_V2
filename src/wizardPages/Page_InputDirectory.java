package wizardPages;

import java.io.File;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import functions.MutationFunction.MUTATE;
import hdf5Tool.HDF5Interface;
import utilities.Constants;
import utilities.PorosityDialog;
import wizardPages.DREAMWizard.STORMData;

/**
 * Select the directory with the HDF5 files to be used, as well as the style of
 * algorithm to run. Currently has 2 modes supported. See line 164
 * 
 * @author port091
 * @author rodr144
 * @author whit162
 */

public class Page_InputDirectory extends DreamWizardPage implements AbstractWizardPage {

	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;
	private STORMData data;
	private int counter = 0;
	private String simulation = "SimulatedAnnealing";
	private Text fileDirectoryText;
	private String directory = Constants.homeDirectory;

	private boolean isCurrentPage = false;

	private boolean isH5;

	private boolean isIAM;

	private static String positiveDirection;

	private JPanel mainPanel = new JPanel();

	protected Page_InputDirectory(final STORMData data) {
		super("Input Directory");
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
		layout.numColumns = 2;
		container.setLayout(layout);

		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		setControl(rootContainer);
		setPageComplete(true);
	}

	public void completePage() throws Exception {
		isCurrentPage = false;

		// We want to essentially reset everything at this point
		data.getSet().clearRun();
		HDF5Interface.statistics.clear();
		// Read in scenario and parameter information from the files
		Constants.homeDirectory = directory;
		data.setupScenarioSet(MUTATE.SENSOR, simulation, fileDirectoryText.getText());
		data.getSet().setScenarioEnsemble(
				fileDirectoryText.getText().substring(fileDirectoryText.getText().lastIndexOf(File.separator) + 1));
		// Ask for porosity input if it doesn't exist yet
		createOptionPane();
		if (!data.getSet().getNodeStructure().porosityIsSet()) {
			PorosityDialog dialog = new PorosityDialog(container.getShell(), data);
			dialog.open();
		}
	}

	@Override
	public void loadPage() {
		isCurrentPage = true;
		DREAMWizard.errorMessage.setText("");
		removeChildren(container);

		Font boldFont = new Font(container.getDisplay(), new FontData("Helvetica", 12, SWT.BOLD));

		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP);
		infoLabel1.setText("Input Directory");
		infoLabel1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
		infoLabel1.setFont(boldFont);

		Label infoLink = new Label(container, SWT.TOP | SWT.RIGHT);
		infoLink.setImage(container.getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		infoLink.setAlignment(SWT.RIGHT);
		infoLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
		infoLink.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				MessageDialog.openInformation(container.getShell(), "Additional information",
						"Select the directory containing HDF5 or NRAP-Open-IAM files for all leakage simulations to be considered. "
								+ "If the user has not converted ASCII simulation output data into DREAM readable HDF5 input files, the Launch Converter button will open a pop-up file converter tool. "
								+ "Read more about the DREAM HDF5 Converter tool in the user manual. Note: The files must be directly available within the directory provided; they may not be in "
								+ "subdirectories within the root directory.");
			}
		});

		Label infoLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP);
		infoLabel.setText("Browse to a single folder containing subsurface simulation files (.h5 or .iam)");
		infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 4));

		final DirectoryDialog directoryDialog = new DirectoryDialog(container.getShell());
		Button buttonSelectDir = new Button(container, SWT.PUSH);
		buttonSelectDir.setText(" Select a directory ");
		buttonSelectDir.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				directoryDialog.setFilterPath(fileDirectoryText.getText());
				directoryDialog.setMessage("Please select a directory and click OK");
				String dir = directoryDialog.open();
				if (dir != null) {
					Constants.homeDirectory = dir;
					fileDirectoryText.setText(dir);
					data.reset();
				}
			}
		});
		if (counter == 0) {
			//// Hack that allows Jonathan and Catherine automatic directory inputs ////
			if (directory.contains("whit162") && directory == Constants.homeDirectory && !directory.contains("Desktop"))
				directory = directory + "\\OneDrive - PNNL\\Desktop\\HDF5_Example";
			if (!System.getProperty("os.name").contains("Mac") && directory.contains("rupr404")
					&& directory == Constants.homeDirectory && !directory.contains("Desktop"))
				directory = directory + "\\OneDrive - PNNL\\Desktop\\BCO_new";
			if (directory.contains("d3x455") && directory == Constants.homeDirectory && !directory.contains("Desktop"))
				directory = directory + "C:\\Users\\D3X455\\OneDrive - PNNL\\Desktop\\DREAM-FY19\\BCO_new";
			if (directory.contains("huan482") && directory == Constants.homeDirectory && !directory.contains("Desktop"))
				directory = directory + "\\OneDrive - PNNL\\Documents\\DREAM Test Cases (5)";
			if (directory.contains("hann898") && directory == Constants.homeDirectory && !directory.contains("Desktop"))
				directory = directory + "\\OneDrive - PNNL\\Documents\\Dream\\BCO";
		}
		//// End of hack ////

		fileDirectoryText = new Text(container, SWT.BORDER | SWT.SINGLE);
		fileDirectoryText.setText(directory);
		fileDirectoryText.setForeground(Constants.black);
		fileDirectoryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		errorFound(validFileCheck(fileDirectoryText.getText()), "  Directory must contain an h5 or iam file.");
		fileDirectoryText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				File resultsFolder = new File(((Text) e.getSource()).getText());
				boolean dirError = !resultsFolder.isDirectory();
				boolean fileError = true;
				if (dirError == true) {
					((Text) e.getSource()).setForeground(Constants.red);
					fileError = false;
				} else {
					((Text) e.getSource()).setForeground(Constants.black);
					directory = ((Text) e.getSource()).getText();
					fileError = validFileCheck(resultsFolder.getPath());
				}
				errorFound(dirError, "  Invalid directory.");
				errorFound(fileError, "  Directory must contain an h5 or iam file.");
			}
		});

		Label noteLabel = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP);
		noteLabel.setText(
				"More info: The \"Launch Converter\" button will allow file format conversions from ASCII to HDF5 for common subsurface simulation output formats (currently: NUFT, STOMP). If the file converter is incompatible with the desired output file format, specific formatting requirements are given in the user manual. ");
		GridData noteGridData = new GridData(GridData.FILL_HORIZONTAL);
		noteGridData.horizontalSpan = ((GridLayout) container.getLayout()).numColumns;
		noteGridData.verticalSpan = 4;
		noteGridData.widthHint = 500;
		noteLabel.setLayoutData(noteGridData);

//		addZAxialOptions();
		container.layout();
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();

		DREAMWizard.visLauncher.setEnabled(false);
		DREAMWizard.convertDataButton.setEnabled(true);
	}

	private boolean validFileCheck(final String folderDir) {
		counter = 1;
		boolean h5Error = true;
		File folder = new File(folderDir);
		File[] fList = folder.listFiles();
		for (File file : fList) {
			if (file.getName().contains(".h5") || file.getName().contains(".iam")) {
				isH5 = file.getName().contains(".h5");
				isIAM = file.getName().contains(".iam");
				h5Error = false;
				break;
			}
		}
		return h5Error;
	}

	private void createOptionPane() {
		mainPanel.removeAll();
		if (isH5) {
			if (data.getSet().getNodeStructure().getUnit("x").equals("")
					|| data.getSet().getNodeStructure().getUnit("times").equals("")
					|| data.getSet().getNodeStructure().getUnit("positive").equals("")
					|| !data.getSet().getNodeStructure().porosityIsSet()) {
				JComboBox<String> ZOrientation = new JComboBox<String>(new String[] { "up", "down" });
				JComboBox<String> distanceList = new JComboBox<String>(new String[] { "m", "ft" });
				JComboBox<String> timeList = new JComboBox<String>(new String[] { "years", "months", "days" });
				JTextField porosityText = new JTextField();

				int option = JOptionPane.showConfirmDialog(null,
						theDialogBoxes(distanceList, timeList, porosityText, ZOrientation),
						"Set Units, Porosity, or Elevation/Depth", JOptionPane.OK_CANCEL_OPTION);
				// When user clicks ok.
				if (option == JOptionPane.OK_OPTION) {
					// Put units into our unit HashMap.
					if (data.getSet().getNodeStructure().getUnit("x").equals("")) {
						String distance = distanceList.getSelectedItem().toString();
						data.getSet().getNodeStructure().addUnit("x", distance);
						data.getSet().getNodeStructure().addUnit("y", distance);
						data.getSet().getNodeStructure().addUnit("z", distance);
					}
					String ZOrient = ZOrientation.getSelectedItem().toString();
					data.getSet().getNodeStructure().addUnit("positive", ZOrient);
					if (data.getSet().getNodeStructure().getUnit("times").equals("")) {
						String time = timeList.getSelectedItem().toString();
						data.getSet().getNodeStructure().addUnit("times", time);
					}
					if(!data.getSet().getNodeStructure().porosityIsSet()) {
						data.getSet().getNodeStructure().setPorosity(Float.valueOf(porosityText.getText()));
					}
				}
			}
		} else if (isIAM) {
			JComboBox<String> ZOrientation = new JComboBox<String>(new String[] { "up", "down" });
			JComboBox<String> distanceList = new JComboBox<String>(new String[] { "m", "ft" });
			JComboBox<String> timeList = new JComboBox<String>(new String[] { "years", "months", "days" });
			JTextField porosityText = new JTextField();

			int option = JOptionPane.showConfirmDialog(null,
					theDialogBoxes(distanceList, timeList, porosityText, ZOrientation),
					"Set Units, Porosity, or Elevation/Depth", JOptionPane.OK_CANCEL_OPTION);
			// When user clicks ok.
			if (option == JOptionPane.OK_OPTION) {
				// Put units into our unit HashMap.
				if (data.getSet().getNodeStructure().getUnit("x").equals("")) {
					String distance = distanceList.getSelectedItem().toString();
					data.getSet().getNodeStructure().addUnit("x", distance);
					data.getSet().getNodeStructure().addUnit("y", distance);
					data.getSet().getNodeStructure().addUnit("z", distance);
				}
				String ZOrient = ZOrientation.getSelectedItem().toString();
				data.getSet().getNodeStructure().addUnit("positive", ZOrient);
				if (data.getSet().getNodeStructure().getUnit("times").equals("")) {
					String time = timeList.getSelectedItem().toString();
					data.getSet().getNodeStructure().addUnit("times", time);
				}
				if(!data.getSet().getNodeStructure().porosityIsSet()) {
					data.getSet().getNodeStructure().setPorosity(Float.valueOf(porosityText.getText()));
				}
			}
		}
	}

	private JPanel theDialogBoxes(final JComboBox<String> distanceList, final JComboBox<String> timeList,
			final JTextField porosityText, final JComboBox<String> theZOrientation) {
		mainPanel.setLayout(new java.awt.GridLayout(0, 1));

		if (data.getSet().getNodeStructure().getUnit("x").equals("")
				|| data.getSet().getNodeStructure().getUnit("times").equals("")
				|| data.getSet().getNodeStructure().getUnit("positive").equals("")
				|| !data.getSet().getNodeStructure().porosityIsSet()) {

			java.awt.Label distanceLabel = new java.awt.Label();
			distanceLabel.setText("XYZ Units:");

			java.awt.Label timeLabel = new java.awt.Label();
			timeLabel.setText("Time Units:");

			java.awt.Label ZOrientationLabel = new java.awt.Label();
			ZOrientationLabel.setText("Z-Axis Positive Direction: ");

			java.awt.Label porosityLabel = new java.awt.Label();
			porosityLabel.setText("Specify Porosity Value");

			porosityText.setText("0.1");

			mainPanel.add(distanceLabel);
			mainPanel.add(distanceList);

			mainPanel.add(timeLabel);
			mainPanel.add(timeList);

			mainPanel.add(ZOrientationLabel);
			mainPanel.add(theZOrientation);

			mainPanel.add(porosityLabel);
			mainPanel.add(porosityText);

			// If we already have these units, remove them from the JPanel they don't need
			// to be set.
			if (!data.getSet().getNodeStructure().getUnit("x").equals("")) {
				mainPanel.remove(distanceList);
				mainPanel.remove(distanceLabel);
			}
			if (!data.getSet().getNodeStructure().getUnit("times").equals("")) {
				mainPanel.remove(timeList);
				mainPanel.remove(timeLabel);
			}
			if (data.getSet().getNodeStructure().porosityIsSet()) {
				mainPanel.remove(porosityText);
				mainPanel.remove(porosityLabel);
			}
		}
		return mainPanel;
	}

	public static String getPositiveDirection() {
		return positiveDirection;
	}

	@Override
	public boolean isPageCurrent() {
		return isCurrentPage;
	}

	@Override
	public void setPageCurrent(final boolean current) {
		isCurrentPage = current;
	}

}