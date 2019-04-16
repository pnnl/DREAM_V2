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
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;

/**
 * Page with all of the generic welcome information, disclaimers, and summaries.
 * @author port091
 * @author rodr144
 */

public class Page_Welcome  extends WizardPage implements AbstractWizardPage {

	private ScrolledComposite sc;
	private Composite container;
	private Composite rootContainer;

	private boolean isCurrentPage = false;
	private boolean hasBeenLoaded = false;

	protected Page_Welcome() {
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
		layout.numColumns = 4;
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
		DREAMWizard.errorMessage.setText("");
		if(!hasBeenLoaded){
		Font boldFont = new Font( container.getDisplay(), new FontData( "Helvetica", 12, SWT.BOLD ) );

		Label infoLabel1 = new Label(container, SWT.TOP | SWT.LEFT | SWT.WRAP );
		infoLabel1.setText("Welcome");
		GridData infoGridData1 = new GridData(GridData.FILL);
		infoGridData1.horizontalSpan = ((GridLayout)container.getLayout()).numColumns;
		infoGridData1.heightHint = 20;
		infoLabel1.setLayoutData(infoGridData1);
		
		infoLabel1.setFont(boldFont);	
		
		GridData aboutInfoData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		aboutInfoData.horizontalSpan = 2;
		aboutInfoData.widthHint = 440;
		aboutInfoData.heightHint = 260;
		String text = "The DREAM tool is an optimization software that determines subsurface monitoring configurations which detect carbon dioxide (CO2) leakage in the least amount of time. DREAM reads ensembles of CO2 leakage scenarios and determines optimal monitoring locations and techniques to deploy based on user-identified constraints. These data result in well configurations with the highest potential to detect leakage and minimize aquifer degradation in the shortest amount of time.  \n\nDREAM  was developed as part of the National Risk Assessment Partnership. For more information see: www.netl.doe.gov";
		Label aboutInfo = new Label(container,SWT.WRAP);
		FontData[] fd = aboutInfo.getFont().getFontData();
		fd[0].setHeight(11);
		aboutInfo.setFont(new Font(container.getDisplay(), fd)); 
		aboutInfo.setText(text);
		aboutInfo.setLayoutData(aboutInfoData);
		
		GridData dreamImageData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
		Image dreamImage = new Image(container.getDisplay(), "./DreamConcept.jpg");
		dreamImageData.horizontalSpan = 2;
		dreamImageData.heightHint = 260;
		CLabel dreamImageLabel = new CLabel(container, SWT.BORDER_SOLID);
		dreamImageLabel.setImage(dreamImage);
		dreamImageLabel.setLayoutData(dreamImageData);

		// NRAP logo at the bottom
		GridData nrapImageData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		nrapImageData.horizontalSpan = 2;
		nrapImageData.verticalSpan = 8;
		nrapImageData.heightHint = 110;
		Image nrapLogo = new Image(container.getDisplay(), "./NRAP.png");
		CLabel nrapLogoLabel = new CLabel(container, SWT.BORDER_SOLID);
		nrapLogoLabel.setImage(nrapLogo);
		nrapLogoLabel.setLayoutData(nrapImageData);

		
		
		new Label(container, SWT.BEGINNING).setText("\tPrimary contact: Yonkofski, C.");
		new Label(container, SWT.BEGINNING);

		new Label(container, SWT.BEGINNING).setText("\tEmail: catherine.yonkofski@pnnl.gov");
		Link acknowledgements = new Link(container, SWT.BEGINNING);
		acknowledgements.setText("                   <A>Acknowledgements</A>");
		acknowledgements.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {			
				MessageBox messageBox = new MessageBox(Page_Welcome.this.getShell(), SWT.OK );
				messageBox.setMessage("Acknowledgements");
				messageBox.setMessage("This work was completed as part of the National Risk Assessment Partnership (NRAP) project. Support for this project came from the U.S. Department of Energy's (DOE) Office of Fossil Energy's Crosscutting Research program. The authors wish to acknowledge Traci Rodosta (Carbon Storage Technology Manager), Kanwal Mahajan (Carbon Storage Division Director), M. Kylee Rice (Carbon Storage Division Project Manager), Mark Ackiewicz (Division of CCS Research Program Manager), Robert Romanosky (NETL Crosscutting Research, Office of Strategic Planning), and Regis Conrad (DOE Office of Fossil Energy) for programmatic guidance, direction, and support."); //Catherine TODO: Add acknowledgement for past developers		
				messageBox.setText("Acknowledgements");
				messageBox.open();
			}
		});
		
		new Label(container, SWT.BEGINNING).setText("\tVersion 2016.11-1.0");
		Link userManual = new Link(container, SWT.BEGINNING);
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
		
		new Label(container, SWT.BEGINNING).setText("\tDeveloper: Whiting, J.");
		Link references = new Link(container, SWT.BEGINNING);
		references.setText("                   <A>References</A>");
		references.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				MessageBox messageBox = new MessageBox(Page_Welcome.this.getShell(), SWT.OK );
				messageBox.setMessage("Yonkofski, C.M., Gastelum, J.A., Porter, E.A., Rodriguez, L.R., Bacon, D.H. and Brown, C.F., 2016. An optimization approach to design monitoring schemes for CO2 leakage detection. International Journal of Greenhouse Gas Control, 47, pp.233-239.");
				messageBox.setText("References");
				messageBox.open();
			}
		});
		
		// Lab logo at the bottom
		GridData imageData = new GridData(SWT.CENTER | SWT.BEGINNING);
		imageData.horizontalSpan = 4;
		imageData.heightHint = 100;
		Image labLogos = new Image(container.getDisplay(), "./DOE-LABS_S.png");
		CLabel labLogosLabel = new CLabel(container, SWT.BORDER_SOLID);
		labLogosLabel.setImage(labLogos);
		labLogosLabel.setLayoutData(imageData);
		
		}
		hasBeenLoaded = true;
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
