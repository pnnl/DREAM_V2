package wizardPages;

/**
 * Abstract page implemented by each custom page
 * @author port091
 * @author rodr144
 */

public abstract interface AbstractWizardPage {
	
	public abstract void loadPage();
	
	public abstract void completePage() throws Exception;
	
	public abstract boolean isPageCurrent();
	public abstract void setPageCurrent(boolean current);
}
