package gravity;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import gravity.RotatedIcon.Rotate;

public class HeatMapWindow extends JFrame implements Runnable {

	private static final long serialVersionUID = 1L;

	private ImageIcon image;

	private int width;

	private int height;

	private JPanel imagePanel;

	private List<Integer> myTimeSteps;

	private Heatmap myHeatMap;

	private JLabel imageLabel;
	
	private JLabel differenceImage;
	
	private int timeStep;

	private int myResolution;

	public HeatMapWindow(final Image theHeatMap, int width, int height, final List<Integer> timeSteps,
			Heatmap objHeatMap) {
		myHeatMap = objHeatMap;
		this.width = width;
		this.height = height;
		imagePanel = new JPanel(new BorderLayout());
		image = new ImageIcon(theHeatMap);
		myTimeSteps = new ArrayList<Integer>();
		myTimeSteps = timeSteps;
		myResolution = 20;
	}

	private void setJFrame() {
		setResizable(true);
		setTitle("Gravity Contour Map");
		ImageIcon temp = new ImageIcon(
				image.getImage().getScaledInstance((int) (width / 1.5), (int) (height / 1.5), Image.SCALE_SMOOTH));
		imageLabel = new JLabel(temp);
		imageLabel.setVisible(true);
		imagePanel.add(imageLabel, BorderLayout.CENTER);

		ImageIcon differenceMap = new ImageIcon(myHeatMap.getDifferenceMap().getScaledInstance((int) (width / 1.5),
				(int) (height / 1.5), Image.SCALE_SMOOTH));

		differenceImage = new JLabel(differenceMap);
		differenceImage.setVisible(true);
		imagePanel.add(differenceImage, BorderLayout.EAST);

		add(imagePanel);
		makeJDialog();
		RotatedIcon ri = new RotatedIcon(new ImageIcon(
				myHeatMap.getColorScale().getScaledInstance(width / 10, (int) (height / 1.1), Image.SCALE_SMOOTH)), Rotate.DOWN);
		add(new JLabel(ri),BorderLayout.SOUTH);
		setVisible(true);
		pack();
	}

	private void makeJDialog() {
		JPanel temp = new JPanel();
		
		int max = Collections.max(myTimeSteps);
		int min = Collections.min(myTimeSteps);
		String[] resolutionVals = new String[myHeatMap.getDivisibleTick()];
		
		for (int i = 0; i < myHeatMap.getDivisibleTick(); i++) {
			resolutionVals[i] = Integer.toString(i + 1);
		}
		JLabel title = new JLabel();
		title.setText("Select Resolution: 1:? ");
		JLabel titleTimeStep = new JLabel();
		titleTimeStep.setText("Select TimeStep: ");
		JComboBox<String> resolution = new JComboBox<String>(resolutionVals);
		
		JSlider selectTimeStep = new JSlider(JSlider.HORIZONTAL, min, max, 1);
		selectTimeStep.setSnapToTicks(true);
		selectTimeStep.setMajorTickSpacing(1);
		selectTimeStep.setPaintTicks(true);
		selectTimeStep.setPaintLabels(true);
		selectTimeStep.addChangeListener(theEvent -> {
			timeStep = selectTimeStep.getValue();
			try {
				// When they select a new time step revert resolution back to 1, and make a new
				// heat map.
				resolution.setSelectedIndex(0);
				Image myImage = myHeatMap.getHeatMap(1, timeStep);
				imageLabel.setIcon(new ImageIcon(
						myImage.getScaledInstance((int) (width / 1.5), (int) (height / 1.5), Image.SCALE_SMOOTH)));
				Image myDifferenceImage = myHeatMap.getDifferenceMap();
				differenceImage.setIcon(new ImageIcon(myDifferenceImage.getScaledInstance((int) (width / 1.5),
				(int) (height / 1.5), Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		
		resolution.addActionListener(theEvent -> {
			myResolution = Integer.parseInt((String) resolution.getSelectedItem());
			try {
				// When resolution is changed we want to make a new heat map.
				// Also we need to change the image icon to the new heat map.
				Image myImage = myHeatMap.getHeatMap(myResolution, timeStep);
				imageLabel.setIcon(new ImageIcon(
						myImage.getScaledInstance((int) (width / 1.5), (int) (height / 1.5), Image.SCALE_SMOOTH)));
				Image myDifferenceImage = myHeatMap.getDifferenceMap();
				differenceImage.setIcon(new ImageIcon(myDifferenceImage.getScaledInstance((int) (width / 1.5),
				(int) (height / 1.5), Image.SCALE_SMOOTH)));
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
