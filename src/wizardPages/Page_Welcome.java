package wizardPages;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Year;

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
	public void createControl (Composite parent) {
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
		infoLabel1.setSize(20, 20);
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
		aboutInfo.setSize(11,11);
		aboutInfo.setLayoutData(aboutInfoData);
		
		GridData dreamImageData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_BEGINNING);
		Image dreamImage = new Image(container.getDisplay(), getClass().getResourceAsStream("/DreamConcept.jpg"));
		dreamImageData.horizontalSpan = 2;
		dreamImageData.heightHint = 262;
		dreamImageData.minimumHeight = 262;
		CLabel dreamImageLabel = new CLabel(container, SWT.BORDER_SOLID);
		dreamImageLabel.setImage(dreamImage);
		dreamImageLabel.setLayoutData(dreamImageData);

		// NRAP logo at the bottom
		GridData nrapImageData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		nrapImageData.horizontalSpan = 2;
		nrapImageData.verticalSpan = 8;
		nrapImageData.heightHint = 110;
		nrapImageData.minimumHeight = 110;
		Image nrapLogo = new Image(container.getDisplay(), getClass().getResourceAsStream("/NRAP.png"));
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
				messageBox.setMessage("This work was completed as part of the National Risk Assessment Partnership (NRAP) project. Support for this project came from the U.S. Department of Energy's (DOE) Office of Fossil Energy's Crosscutting Research program. "
						+ "The authors wish to acknowledge Traci Rodosta (Carbon Storage Technology Manager), Kanwal Mahajan (Carbon Storage Division Director), M. Kylee Rice (Carbon Storage Division Project Manager), Mark Ackiewicz (Division of CCS Research Program Manager),"
						+ "Robert Romanosky (NETL Crosscutting Research, Office of Strategic Planning), and Regis Conrad (DOE Office of Fossil Energy) for programmatic guidance, direction, and support. "
						+ "The authors wish to thank Art Sadovsky, Jason Gastelum, Ellen Porter, Luke Rodriguez for their early development work on the DREAM tool.");
				messageBox.setText("Acknowledgements");
				messageBox.open();
			}
		});
		
		new Label(container, SWT.BEGINNING).setText("\tVersion 2020.01-2.01");
		Link userManual = new Link(container, SWT.BEGINNING);
		userManual.setText("                   <A>User manual</A>");
		userManual.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				// We essentially create a local copy of the pdf so that it works when packaged in a JAR
				try {
			        Path tempOutput = Files.createTempFile("user_manual", ".pdf");
			        tempOutput.toFile().deleteOnExit();
			        InputStream is = getClass().getResourceAsStream("/user_manual.pdf");
			        Files.copy(is, tempOutput, StandardCopyOption.REPLACE_EXISTING);
			        Desktop.getDesktop().open(tempOutput.toFile());
			        is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		new Label(container, SWT.BEGINNING).setText("\tDevelopers: Whiting, J., Huang, B.");
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
		
		// Add a copyright notice
		new Label(container, SWT.NULL);
		Link copyright = new Link(container, SWT.BEGINNING);
		copyright.setText("                   <A>Copyright</A>");
		copyright.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				MessageBox messageBox = new MessageBox(Page_Welcome.this.getShell(), SWT.OK );
				messageBox.setText("Open source license (BSD-style)");
				String year = Year.now().toString();
				messageBox.setMessage("Designs for Risk Evaluation and Management (DREAM)"
						+ "\nCopyright \u00a9 "+year+", Battelle Memorial Institute"
						+ "\nAll rights reserved."
						
						+ "\n1. Battelle Memorial Institute (hereinafter Battelle) hereby grants permission to"
						+ "\n\u0020\u0020 any person or entity lawfully obtaining a copy of this software and associated"
						+ "\n\u0020\u0020 documentation files (hereinafter “the Software”) to redistribute and use the"
						+ "\n\u0020\u0020 Software in source and binary forms, with or without modification.  Such"
						+ "\n\u0020\u0020 person or entity may use, copy, modify, merge, publish, distribute, sublicense,"
						+ "\n\u0020\u0020 and/or sell copies of the Software, and may permit others to do so, subject to"
						+ "\n\u0020\u0020 the following conditions:"
						
						+ "\n\t\u2022 Redistributions of source code must retain the above copyright"
						+ "\n\t\u0020\u0020 notice, this list of conditions and the following disclaimers."
						
						+ "\n\t\u2022 Redistributions in binary form must reproduce the above copyright"
						+ "\n\t\u0020\u0020 notice, this list of conditions and the following disclaimer in the"
						+ "\n\t\u0020\u0020 documentation and/or other materials provided with the"
						+ "\n\t\u0020\u0020 distribution."
						
						+ "\n\t\u2022 Other than as used herein, neither the name Battelle Memorial"
						+ "\n\t\u0020\u0020 Institute or Battelle may be used in any form whatsoever"
						+ "\n\t\u0020\u0020 without the express written consent of Battelle."
						
						+ "\n2. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND"
						+ "\n\u0020\u0020 CONTRIBUTORS \"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES,"
						+ "\n\u0020\u0020 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF"
						+ "\n\u0020\u0020 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE"
						+ "\n\u0020\u0020 DISCLAIMED. IN NO EVENT SHALL BATTELLE OR CONTRIBUTORS BE LIABLE"
						+ "\n\u0020\u0020 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,"
						+ "\n\u0020\u0020 OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,"
						+ "\n\u0020\u0020 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,"
						+ "\n\u0020\u0020 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON"
						+ "\n\u0020\u0020 ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR"
						+ "\n\u0020\u0020 TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT"
						+ "\n\u0020\u0020 OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY"
						+ "\n\u0020\u0020 OF SUCH DAMAGE."
						);
				
				messageBox.open();
			}
		});
		
		// Lab logo at the bottom
		GridData imageData = new GridData(SWT.CENTER | SWT.BEGINNING);
		imageData.horizontalSpan = 4;
		imageData.heightHint = 100;
		imageData.minimumHeight = 86;
		Image labLogos = new Image(container.getDisplay(), getClass().getResourceAsStream("/DOE-LABS_S.png"));
		CLabel labLogosLabel = new CLabel(container, SWT.BORDER_SOLID);
		labLogosLabel.setImage(labLogos);
		labLogosLabel.setLayoutData(imageData);
		
		}
		hasBeenLoaded = true;
		DREAMWizard.visLauncher.setEnabled(false);
		DREAMWizard.convertDataButton.setEnabled(false);
		
		Page_Welcome.this.getShell().forceFocus(); //prevents the highlight of the acknowledgement link on load
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