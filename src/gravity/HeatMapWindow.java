package gravity;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
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
		setResizable(true);
		setTitle("Gravity Contour Map");
		ImageIcon temp = new ImageIcon(
				image.getImage().getScaledInstance((int) (width / 1.2), (int) (height / 1.2), Image.SCALE_SMOOTH));
		imageLabel = new JLabel(temp);
		imageLabel.setVisible(true);

		imagePanel.add(imageLabel, BorderLayout.CENTER);
		
		add(imagePanel);
		makeJDialog();
		add(new JLabel(new ImageIcon(
				myHeatMap.getColorScale().getScaledInstance(width / 10, (int) (height / 1.1), Image.SCALE_SMOOTH))),
				BorderLayout.EAST);
		setVisible(true);
		pack();
	}
	
	private void makeJDialog() {
		JPanel temp = new JPanel();


		int max = myHeatMap.getDivisibleTick();
		int min = 1;
		JLabel title = new JLabel();
		title.setText("Select Resolution: 1:? ");
		JLabel titleTimeStep = new JLabel();
		titleTimeStep.setText("Select TimeStep: ");
		JSlider resolution = new JSlider(JSlider.HORIZONTAL, min, max, 1);
		resolution.setMajorTickSpacing(1);
		resolution.setPaintTicks(true);
		resolution.setPaintLabels(true);
		resolution.addChangeListener(theEvent -> {
			myResolution = resolution.getValue();
			try {
				Image myImage = myHeatMap.getHeatMap(myResolution, timeStep);
				imageLabel.setIcon(new ImageIcon(
						myImage.getScaledInstance((int) (width / 1.2), (int) (height / 1.2), Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		String[] temparr = myTimeSteps.toArray(new String[0]);
		JComboBox<String> selectTimeStep = new JComboBox<String>(temparr);
		selectTimeStep.addActionListener(theEvent -> {
			timeStep = Integer.parseInt((String) selectTimeStep.getSelectedItem());
			try {
				resolution.setValue(1);
				Image myImage = myHeatMap.getHeatMap(1, timeStep);
				imageLabel.setIcon(new ImageIcon(
						myImage.getScaledInstance((int) (width / 1.2), (int) (height / 1.2), Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		temp.add(selectTimeStep, FlowLayout.LEFT);
		temp.add(titleTimeStep, FlowLayout.LEFT);
		temp.add(resolution, FlowLayout.LEFT);
		temp.add(title, FlowLayout.LEFT);
		add(temp, BorderLayout.NORTH);
	}

	@Override
	public void run() {
		setJFrame();
	}
}
