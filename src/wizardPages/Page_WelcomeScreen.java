package wizardPages;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;

public class Page_WelcomeScreen  extends WizardPage implements AbstractWizardPage {

	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;

	private boolean isCurrentPage = false;

	protected Page_WelcomeScreen() {
		super("Welcome");
	}

	@Override
	public void createControl(Composite parent) {		
		rootContainer = new Composite(parent, SWT.NULL);
		rootContainer.setLayout(GridLayoutFactory.fillDefaults().create());

		sc = new ScrolledComposite(rootContainer, SWT.V_SCROLL | SWT.H_SCROLL);
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
		layout.verticalSpacing = 2;
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		container.setLayout(layout);

		sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 480).create());

		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		setControl(rootContainer);
		setPageComplete(true);
	}

	@Override
	public void loadPage() {
		isCurrentPage = true;
		
		GridData aboutInfoData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		aboutInfoData.horizontalSpan = 2;
		aboutInfoData.widthHint = 500;
		String text = "The DREAM tool is a post-processor that optimizes monitoring configurations across user-provided numerically modeled subsurface leakage simulations. DREAM identifies monitoring configurations that minimize the time to first detection of CO2 leakage based on specified monitoring requirements and budgetary constraints. \n\nDREAM  was developed as part of the National Risk Assessment Partnership. For more information see: www.netl.doe.gov";
		Label aboutInfo = new Label(container,SWT.WRAP);
		aboutInfo.setText(text);
		aboutInfo.setLayoutData(aboutInfoData);
		
				GridData dreamImageData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
		Image dreamImage = new Image(container.getDisplay(), "./img/DreamConcept.jpg");
		CLabel dreamImageLabel = new CLabel(container, SWT.BORDER_SOLID);
		dreamImageLabel.setImage(dreamImage);
		dreamImageLabel.setLayoutData(dreamImageData);

		// NRAP logo at the bottom
		GridData nrapImageData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		nrapImageData.horizontalSpan = 2;
		nrapImageData.verticalSpan = 10;
		Image nrapLogo = new Image(container.getDisplay(), "./img/NRAP.png");
		CLabel nrapLogoLabel = new CLabel(container, SWT.BORDER_SOLID);
		nrapLogoLabel.setImage(nrapLogo);
		nrapLogoLabel.setLayoutData(nrapImageData);

		
		new Label(container, SWT.WRAP).setText("\tPrimary contact: Yonkofski, C.");
		new Label(container, SWT.WRAP).setText("\tEmail: catherine.yonkofski@pnnl.gov");
		new Label(container, SWT.WRAP).setText("\tVersion 1.0");
		new Label(container, SWT.WRAP).setText("\tDevelopers: Porter, E.; Rodriguez, L.");
		//	new Label(container, SWT.WRAP).setText(""); // Space
		
		new Label(container, SWT.WRAP);
		
		Link acknowledgements = new Link(container, SWT.WRAP);
		acknowledgements.setText("                   <A>Acknowledgements</A>");
		acknowledgements.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {			
				MessageBox messageBox = new MessageBox(Page_WelcomeScreen.this.getShell(), SWT.OK );
				messageBox.setMessage("Acknowledgements");
				messageBox.setMessage("This work was completed as part of the National Risk Assessment Partnership (NRAP) project. Support for this project came from the Department of Energy’s (DOE) Office of Fossil Energy’s Crosscutting Research program. The authors wish to acknowledge Traci Rodosta (NETL Strategic Center for Coal) and Mark Ackiewicz (DOE Office of Fossil Energy) for programmatic guidance, direction, and support.");		
				messageBox.setText("Acknowledgements");
				messageBox.open();
			}
		});
		
		Link userManual = new Link(container, SWT.WRAP);
		userManual.setText("                   <A>User manual</A>");
		userManual.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				File userManualPdf = new File("./docs/user_manual.pdf");
				try {
					Desktop.getDesktop().open(userManualPdf);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		Link references = new Link(container, SWT.WRAP);
		references.setText("                   <A>References</A>");
		references.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				MessageBox messageBox = new MessageBox(Page_WelcomeScreen.this.getShell(), SWT.OK );
				messageBox.setMessage("Yonkofski, C.R., Gastelum, J.A.,  M., Porter, E.A., Rodriguez, L.R., Bacon, D.H., and Brown, C. F. An Optimization Approach to Design Monitoring Schemes for CO2 Leakage Detection. IJGGC NRAP Special Issue.(2015). In Review. ");
				messageBox.setText("References");
				messageBox.open();
			}
		});
		
		// Lab logo at the bottom
		GridData imageData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.VERTICAL_ALIGN_END);
		imageData.horizontalSpan = 3;
		Image labLogos = new Image(container.getDisplay(), "./img/DOE-LABS_S.png");
		CLabel labLogosLabel = new CLabel(container, SWT.BORDER_SOLID);
		labLogosLabel.setImage(labLogos);
		labLogosLabel.setLayoutData(imageData);
	
		DREAMWizard.visLauncher.setEnabled(false);
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
