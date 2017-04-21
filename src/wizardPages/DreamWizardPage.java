package wizardPages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public abstract class DreamWizardPage extends WizardPage {

	public Color black = new Color(Display.getCurrent(), 0, 0, 0);
	public Color red = new Color(Display.getCurrent(), 255, 0, 0);
	
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
	
	public boolean isValidFloat(String string) {
		try {
			Float.parseFloat(string);
			return true;
		} catch (NumberFormatException ne) {
			return false;
		}
	}
	
	public boolean isValidInt(String string) {
		try {
			Integer.parseInt(string);
			return true;
		} catch (NumberFormatException ne) {
			return false;
		}
	}
	
	@Override
	public boolean canFlipToNextPage() {
		super.canFlipToNextPage();
		return DREAMWizard.errorMessage.getText().trim().isEmpty();
	} 
}