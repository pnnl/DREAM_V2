package mapView;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Not used.
 */

public class MapView extends JFrame {

	private static final long serialVersionUID = 1L;

	Controls controls;
	
	public static void main(String[] args) {
		new MapView().setVisible(true);
	}
	
	public MapView() {
		
		controls = new Controls();
		
		setSize(new Dimension(600, 600));
		
		
		setLayout(new BorderLayout());
		add(new JPanel(), BorderLayout.CENTER); // The map
		add(controls, BorderLayout.EAST);
		
		
	}
}
