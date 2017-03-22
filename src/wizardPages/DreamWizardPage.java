package wizardPages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Display;

public abstract class DreamWizardPage extends WizardPage {

	protected DreamWizardPage(String pageName) {
		super(pageName);
	}

	//Method that turns text red and initiates an error message and cancels the next button
	public void redText(ModifyEvent e, boolean error, String errorText) {
		if (error==true) {
			((Text)e.getSource()).setForeground(new Color(Display.getCurrent(), 255, 0, 0));
			if (!DREAMWizard.errorMessage.getText().contains(errorText))
				DREAMWizard.errorMessage.setText(DREAMWizard.errorMessage.getText() + errorText);
			DREAMWizard.nextButton.setEnabled(false);
		}
		else {
			((Text)e.getSource()).setForeground(new Color(Display.getCurrent(), 0, 0, 0));
			if (DREAMWizard.errorMessage.getText().contains(errorText))
				DREAMWizard.errorMessage.setText(DREAMWizard.errorMessage.getText().replaceAll(errorText, ""));
			if (DREAMWizard.errorMessage.getText().trim().isEmpty()==true)
				DREAMWizard.nextButton.setEnabled(true);
		}
		DREAMWizard.errorMessage.getParent().layout();
	}
	
	//Method that only initiates an error message and cancels the next button (no red text)
	public void errorWithoutRedText(boolean error, String errorText) {
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