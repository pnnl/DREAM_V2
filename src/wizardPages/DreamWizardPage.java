package wizardPages;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Display;

public abstract class DreamWizardPage extends WizardPage {

	protected DreamWizardPage(String pageName) {
		super(pageName);
		// TODO Auto-generated constructor stub
	}

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

}