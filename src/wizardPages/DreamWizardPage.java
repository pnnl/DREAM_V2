package wizardPages;

import org.eclipse.jface.wizard.WizardPage;

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
	
	public boolean isValidNumber(String string) {
		try {
			Float.parseFloat(string);
			return true;
		} catch (NumberFormatException ne) {
			return false;
		}
	}
}