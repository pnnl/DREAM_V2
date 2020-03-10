package wizardPages;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import functions.MutationFunction.MUTATE;
import hdf5Tool.HDF5Interface;
import utilities.Constants;
import wizardPages.DREAMWizard.STORMData;

/**
 * Select the directory with the HDF5 files to be used, as well as the style of
 * algorithm to run. Currently has 2 modes supported. See line 164
 * 
 * @author port091
 * @author rodr144
 * @author whit162
 * @author huan482
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
		//createOptionPane();
		createDialogBox();
//		if (!data.getSet().getNodeStructure().porosityIsSet()) {
//			PorosityDialog dialog = new PorosityDialog(container.getShell(), data);
//			dialog.open();
//		}
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
								+ "Read more about the DREAM HDF5 Conver	ter tool in the user manual. Note: The files must be directly available within the directory provided; they may not be in "
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
		if (counter == 0 && !System.getProperty("os.name").contains("Mac")) {
			//// Hack that allows Jonathan and Catherine automatic directory inputs ////
			if (directory.contains("whit162") && directory == Constants.homeDirectory && !directory.contains("Desktop"))
				directory = directory + "\\OneDrive - PNNL\\Desktop\\HDF5_Example";
			if (!System.getProperty("os.name").contains("Mac") && directory.contains("rupr404")
					&& directory == Constants.homeDirectory && !directory.contains("Desktop"))
				directory = directory + "\\OneDrive - PNNL\\Desktop\\BCO_new";
			if (directory.contains("d3x455") && directory  == Constants.homeDirectory && !directory.contains("Desktop"))
				directory = directory + "C:\\Users\\D3X455\\OneDrive - PNNL\\Desktop\\DREAM-FY19\\BCO_new";
			if (directory.contains("huan482") && directory == Constants.homeDirectory && !directory.contains("Desktop"))
				directory = directory + "\\OneDrive - PNNL\\Documents\\task6_rev";
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
				Constants.isH5 = file.getName().contains(".h5");
				Constants.isIAM = file.getName().contains(".iam");
				h5Error = false;
				break;
			}
		}
		return h5Error;
	}
	
	private void createDialogBox() {
		if (Constants.isH5) {
			if (data.getSet().getNodeStructure().getUnit("x").equals("")
					|| data.getSet().getNodeStructure().getUnit("times").equals("")
					|| data.getSet().getNodeStructure().getPositive().equals("")
					|| !data.getSet().getNodeStructure().porosityIsSet()) {
				initUnits();
			}
		} else if (Constants.isIAM) {
			//IAM Files will never have any of the units we want.
			initUnits();
		}
	}
	/**
	 * This method creates the dialog box that asks for the units to insert into the hdf5 files.
	 * @author huan482
	 */
	private void initUnits() {
		//Init Shell
		Shell shell = new Shell();
		shell.setLayout(new GridLayout());
		shell.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		shell.setText("Set Units, Porosity, or Elevation/Depth");
		shell.setSize(230, 300);
		//init drop down menus and text boxes.
		Label unitText = new Label(shell, SWT.NONE | SWT.CENTER);
		Combo unitDropDown = new Combo(shell, SWT.DROP_DOWN | SWT.BORDER | SWT.CENTER);
		Label zText = new Label(shell, SWT.NONE | SWT.CENTER);
		Combo zOrientationDropDown = new Combo(shell, SWT.DROP_DOWN | SWT.BORDER | SWT.CENTER);
		Label timeText = new Label(shell, SWT.NONE | SWT.CENTER);
		Combo timeDropDown = new Combo(shell, SWT.DROP_DOWN | SWT.BORDER | SWT.CENTER);
		Label porosity = new Label(shell, SWT.NONE | SWT.CENTER);
		Text porosityText = new Text(shell, SWT.BORDER | SWT.CENTER);

		Button okBtn = new Button(shell, SWT.PUSH | SWT.CENTER);

		okBtn.setText("OK");
		
		//center components.
		porosityText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		okBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		unitText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		unitDropDown.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		zText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		zOrientationDropDown.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		timeText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		timeDropDown.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		porosity.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		unitText.setText("XYZ Units:");
		unitDropDown.add("m");
		unitDropDown.add("ft");

		zText.setText("Z-Axis Positive Direction: ");
		zOrientationDropDown.add("up");
		zOrientationDropDown.add("down");

		timeText.setText("Time Units: ");
		timeDropDown.add("years");
		timeDropDown.add("months");
		timeDropDown.add("days");

		unitDropDown.select(0);
		zOrientationDropDown.select(0);
		timeDropDown.select(0);

		porosity.setText("Set Porosity: ");
		porosityText.setText("0.1");

		okBtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event theEvent) {
				//Set the units if it doesn't exist.
				if (data.getSet().getNodeStructure().getUnit("x").equals("")) {
					String distance = unitDropDown.getText();
					data.getSet().getNodeStructure().addUnit("x", distance);
					data.getSet().getNodeStructure().addUnit("y", distance);
					data.getSet().getNodeStructure().addUnit("z", distance);
				}
				if (data.getSet().getNodeStructure().getPositive().equals("")) {
					String ZOrient = zOrientationDropDown.getText();
					data.getSet().getNodeStructure().addPositive(ZOrient);
				}
				if (data.getSet().getNodeStructure().getUnit("times").equals("")) {
					String time = timeDropDown.getText();
					data.getSet().getNodeStructure().addUnit("times", time);
				}
				if (!data.getSet().getNodeStructure().porosityIsSet()) {
					data.getSet().getNodeStructure().setPorosity(Float.valueOf(porosityText.getText()));
				}
				shell.dispose();
			}
		});
		//If the unit already exists get rid of the corresponding dialog boxs.
		if (!data.getSet().getNodeStructure().getUnit("x").equals("")) {
			unitText.dispose();
			unitDropDown.dispose();
		}
		if (!data.getSet().getNodeStructure().getUnit("times").equals("")) {
			timeText.dispose();
			timeDropDown.dispose();
		}
		if (!data.getSet().getNodeStructure().getPositive().equals("")) {
			zOrientationDropDown.dispose();
			zText.dispose();
		}
		if (data.getSet().getNodeStructure().porosityIsSet()) {
			porosity.dispose();
			porosityText.dispose();
		}
		
		//center the pop-up box.
		Monitor primary = sc.getMonitor();
	    Rectangle bounds = primary.getBounds();
	    Rectangle rect = shell.getBounds();
	    
	    int x = bounds.x + (bounds.width - rect.width) / 2;
	    int y = bounds.y + (bounds.height - rect.height) / 2;
	    
	    shell.setLocation(x, y);
		// shell.pack();
		shell.open();
		
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