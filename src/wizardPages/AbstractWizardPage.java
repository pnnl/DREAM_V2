package wizardPages;

public abstract interface AbstractWizardPage {
	
	public abstract void loadPage();
	
	public abstract void completePage() throws Exception;
	
	public abstract boolean isPageCurrent();
	public abstract void setPageCurrent(boolean current);
}
