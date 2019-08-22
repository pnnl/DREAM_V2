package gravity;

import java.awt.BorderLayout;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class HeatMapWindow extends JFrame implements Runnable {

	private static final long serialVersionUID = 1L;

	private ImageIcon image;

	private int width;

	private int height;

	private JPanel imagePanel;

	private List<String> myTimeSteps;

	private Heatmap myHeatMap;

	private JLabel imageLabel;

	private int timeStep;

	private int myResolution;

	public HeatMapWindow(final Image theHeatMap, int width, int height, final List<String> timeSteps,
			Heatmap objHeatMap) {
		myHeatMap = objHeatMap;
		this.width = width;
		this.height = height;
		imagePanel = new JPanel(new BorderLayout());
		image = new ImageIcon(theHeatMap);
		myTimeSteps = new ArrayList<String>();
		myTimeSteps = timeSteps;
		myResolution = 20;
	}

	private void setJFrame() {
		setResizable(false);
		setTitle("Gravity Contour Map");
		ImageIcon temp = new ImageIcon(image.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
		imageLabel = new JLabel(temp);
		imageLabel.setVisible(true);

		makeJPanel(imageLabel);

		add(imagePanel);
		makeJDialog();
		add(new JLabel(new ImageIcon(myHeatMap.getColorScale().getScaledInstance(width / 10, (int) (height / 1.1), Image.SCALE_SMOOTH))),
				BorderLayout.EAST);
		setVisible(true);
		pack();
	}

	private void makeJPanel(JLabel image) {
		imagePanel.add(image, BorderLayout.CENTER);
	}

	private void makeJDialog() {
		JPanel temp = new JPanel();
		temp.setLayout(new BoxLayout(temp, BoxLayout.Y_AXIS));

		int max = myHeatMap.getDivisibleTick();
		int min = 1;
		JLabel title = new JLabel();
		title.setText("Resolution");
		JSlider resolution = new JSlider(JSlider.HORIZONTAL, min, max, 1);
		resolution.setMajorTickSpacing(1);
		resolution.setPaintTicks(true);
		resolution.setPaintLabels(true);
		resolution.addChangeListener(theEvent -> {
			myResolution = resolution.getValue();
			try {
				Image myImage = myHeatMap.getHeatMap(myResolution, timeStep);
				imageLabel.setIcon(new ImageIcon(myImage.getScaledInstance(width, height, Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		String[] temparr = myTimeSteps.toArray(new String[0]);
		JComboBox<String> selectTimeStep = new JComboBox<String>(temparr);
		selectTimeStep.addActionListener(theEvent -> {
			timeStep = Integer.parseInt((String) selectTimeStep.getSelectedItem());
			try {
				Image myImage = myHeatMap.getHeatMap(1, timeStep);
				imageLabel.setIcon(new ImageIcon(myImage.getScaledInstance(width, height, Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		// Just need to change the resolution and it'll be very good
		JDialog widgets = new JDialog(this, "Change Resolution & TimeStep");
		temp.add(title);
		temp.add(resolution);
		widgets.setSize(width / 3, height / 6);
		temp.add(selectTimeStep);
		widgets.add(temp);
		widgets.setVisible(true);

	}

	@Override
	public void run() {
		setJFrame();
	}
}
