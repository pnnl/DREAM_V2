package wizardPages;

import java.util.List;

/**
 * Wrapper for the scrollable dialog, not actually used.
 * @author port091
 */

public class ScrollableDialogWrapper {
	ScrollableDialog dialog;
	public ScrollableDialogWrapper(Page_RunDREAM page, String what, int runs, int iterations) {
		dialog = new ScrollableDialog(page.getShell(), "Running "+what+"...", runs, iterations);
		dialog.setBlockOnOpen(false);
		dialog.create();
	}
	public void appendText(List<String> lines) {
		dialog.appendText(lines);
	}
	
}