package wizardPages;

import java.util.List;

public class ScrollableDialogWrapper {
	ScrollableDialog dialog;
	public ScrollableDialogWrapper(Page_ReviewAndRun page, String what, int runs, int iterations) {
		dialog = new ScrollableDialog(page.getShell(), "Running "+what+"...", runs, iterations);
		dialog.setBlockOnOpen(false);
		dialog.create();
	}
	public void appendText(List<String> lines) {
		dialog.appendText(lines);
	}
	
}