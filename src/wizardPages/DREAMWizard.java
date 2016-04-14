package wizardPages;

import hdf5Tool.FileBrowser;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import objects.ExtendedConfiguration;
import objects.DREAMData;
import objects.Scenario;
import objects.ScenarioSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.DialogTray;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import results.ResultPrinter;
import utilities.Constants;
import utilities.Point3d;
import utilities.Constants.ModelOption;
import visualization.MultiDomainViewer;
import wizardPages.Page_SensorSetup.SensorData;
import functions.CCS9_1;
import functions.Function;
import functions.MutationFunction.MUTATE;

public class DREAMWizard extends Wizard {

	private DREAMData data;
	private MultiDomainViewer domainViewer;
	private WizardDialog dialog;

	public static Button convertDataButton;
	public static Button visLauncher;
	
	public DREAMWizard() {
		super();
		setWindowTitle(null);
		setWindowTitle("DREAM Wizard");
		setNeedsProgressMonitor(true);	
		ImageData imgData = new ImageData("./img/dream.png"); 
		this.setDefaultPageImageDescriptor(ImageDescriptor.createFromImageData(imgData));
		this.setTitleBarColor(new RGB(255,255,255));
		data = new DREAMData(this, dialog);
	}

	public void setDialog(WizardDialog dialog) {
		this.dialog = dialog;	
		data.setDialog(dialog);
	}

	public void linkViewer(MultiDomainViewer domainViewer) {
		this.setDomainViewer(domainViewer);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage current) {

		AbstractWizardPage currentPage = ((AbstractWizardPage)current);
		IWizardPage next = super.getNextPage(current);	

		// If we haven't loaded this page yet, load it
		if(!currentPage.isPageCurrent()) {
			// Float check that the next page isn't already loaded
			if(next != null && ((AbstractWizardPage)next).isPageCurrent()) 
				((AbstractWizardPage)next).setPageCurrent(false);

			System.out.println("LOAD: " + currentPage);
			currentPage.loadPage();	
			return next;
		}

		// Otherwise finalize this page
		System.out.println("COMPLETE " + currentPage);
		try {
			currentPage.completePage();
			return next;
		} catch (Exception e) {
			currentPage.loadPage();
			return current;	// Something went wrong stay on this page
		}
	}

	@Override
	public void addPages() {

		addPage(new Page_WelcomeScreen());	
		addPage(new Page_ScenarioSet(data));	
		addPage(new Page_ScenarioSetup(data));
		addPage(new Page_SensorSetup(data));
		addPage(new Page_InferenceTest(data));
		addPage(new Page_RunSettings(data));
		addPage(new Page_ExcludeWells(data));
		addPage(new Page_ReviewAndRun(data));
	}

	@Override
	public boolean performFinish() {
		return true;
	}
	
	
	public static void main(String[] args) {

		try {
//			UIManager.setLookAndFeel(
//					UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		} 

		final Display display = Display.getDefault();
		final Shell shell = new Shell(display);

		// Pop up the disclaimer, exit on cancel
		MessageBox messageBox = new MessageBox(shell, SWT.OK | SWT.CANCEL );
		messageBox.setMessage("The Software was produced by Battelle under Contract No. DE-AC05-76RL01830 with the Department of Energy.  The U.S. Government is granted for itself and others acting on its behalf a nonexclusive, paid-up, irrevocable worldwide license in this data to reproduce, prepare derivative works, distribute copies to the public, perform publicly and display publicly, and to permit others to do so.  The specific term of the license can be identified by inquiry made to Battelle or DOE."
				+ "\n\nNeither the United States Government, nor any of their employees, makes any warranty, express or implied, or assumes any legal liability or responsibility for the accuracy, completeness, or usefulness of any information, apparatus, product, or process disclosed, or represents that its use would not infringe privately owned rights. Reference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer, or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United States Government or any agency thereof. The views and opinions of authors expressed herein do not necessarily state or reflect those of the United States Government or any agency thereof.");
		messageBox.setText("NOTICE TO USERS");
		int response = messageBox.open();
		if (response == SWT.CANCEL)
			System.exit(0); // Exit if they don't accept

		final DREAMWizard wizard = new DREAMWizard();

		WizardDialog.setDefaultImage(new Image(Display.getDefault(),"./img/icon.png"));

		WizardDialog wizardDialog = new WizardDialog(null, wizard) {
			{
				setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.MODELESS | SWT.RESIZE | SWT.MAX | SWT.MIN | SWT.ICON);
			}
			
			@Override
			protected void finishPressed(){
				//On a mac, this was being "pressed" when enter was hit. This way it does nothing and does not exit.
			}
			
			
			
			@Override
			protected void createButtonsForButtonBar(Composite parent) {		


				convertDataButton = new Button(parent, SWT.PUSH); 	
				convertDataButton.setText("Launch Converter");
				convertDataButton.setToolTipText("Convert simulation data to DREAM h5 format"); 	
				convertDataButton.addSelectionListener(new SelectionListener() 
				{ 
					@Override 
					public void widgetSelected(SelectionEvent e) { 
						FileBrowser browser = new FileBrowser();
						browser.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
						browser.setVisible(true); 
					} 

					@Override 
					public void widgetDefaultSelected(SelectionEvent e) { 
						FileBrowser browser = new FileBrowser();
						browser.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
						browser.setVisible(true); 
					} 
				}); 

				visLauncher = new Button(parent, SWT.PUSH); 	
				visLauncher.setText("Launch Visualization"); 	
				visLauncher.addSelectionListener(new SelectionListener() 
				{ 
					@Override 
					public void widgetSelected(SelectionEvent e) { 
						wizard.launchVisWindow();
					} 

					@Override 
					public void widgetDefaultSelected(SelectionEvent e) { 
						widgetSelected(e); 
					} 
				}); 


				super.createButtonsForButtonBar(parent);

				// Hide the buttons we don't use
				Button cancel = super.getButton(IDialogConstants.CANCEL_ID);	
				Button finish = super.getButton(IDialogConstants.FINISH_ID);
				((GridData)cancel.getLayoutData()).exclude = true;
				((GridData)finish.getLayoutData()).exclude = true;
			}
		};

		wizard.setDialog(wizardDialog);
		wizardDialog.setTitleAreaColor(new RGB(255, 255, 255));//32,62,72));
		wizardDialog.open();
	}

	public void launchVisWindow() {
		System.out.println("Main Shell handling Button press, about to create child Shell");
		try {
		MultiDomainViewer domainViewer = new MultiDomainViewer(Display.getCurrent(), getScenarioSet()); 
		linkViewer(domainViewer);
		} catch (Exception e) {
			try {
				Thread.sleep(1000);

				MultiDomainViewer domainViewer = new MultiDomainViewer(Display.getCurrent(), getScenarioSet()); 
				linkViewer(domainViewer);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		}
	}

	public ScenarioSet getScenarioSet() {
		return data.getScenarioSet();
	}

	public MultiDomainViewer getDomainViewer() {
		return domainViewer;
	}

	public void setDomainViewer(MultiDomainViewer domainViewer) {
		this.domainViewer = domainViewer;
	}
} 