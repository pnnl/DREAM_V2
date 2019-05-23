package wizardPages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Parent class of DreamWizard, contains functions called in multiple pages
 * @author whit162
 */

public abstract class DreamWizardPage extends WizardPage {
	
	protected DreamWizardPage(String pageName) {
		super(pageName);
	}
	
	//Method that initiates an error message and cancels the next button
	public void errorFound(boolean error, String errorText) {
		if (error==true) {
			if (!DREAMWizard.errorMessage.getText().contains(errorText))
				DREAMWizard.errorMessage.setText(DREAMWizard.errorMessage.getText() + errorText);
			DREAMWizard.nextButton.setEnabled(false);
		}
		else {
			if (DREAMWizard.errorMessage.getText().contains(errorText))
				DREAMWizard.errorMessage.setText(DREAMWizard.errorMessage.getText().replaceAll(errorText, ""));
			if (DREAMWizard.errorMessage.getText().trim().isEmpty()==true)
				DREAMWizard.nextButton.setEnabled(true);
		}
		DREAMWizard.errorMessage.getParent().layout();
	}
	
	@Override
	public boolean canFlipToNextPage() {
		super.canFlipToNextPage();
		return DREAMWizard.errorMessage.getText().trim().isEmpty();
	} 
	
	public void removeChildren(final Composite theContainer) {
		for(Control control: theContainer.getChildren()) {
			control.dispose(); 
		}
	}
}