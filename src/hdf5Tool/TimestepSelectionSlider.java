package hdf5Tool;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import gridviz.GridError;

public class TimestepSelectionSlider extends javax.swing.JPanel {


	JSlider timestepSlider;
	JLabel sliderIndexTitle;
	JLabel sliderIndexText;
	JLabel timestepsTitle;
	JLabel timestepsText;
	JLabel totalScenariosTitle;
	JLabel totalScenariosText;
	Map<Integer, List<String>> temp;
	Integer[] variableSteps;

	public TimestepSelectionSlider(Integer[] variableSteps, Map<Integer, List<String>> temp) {
		this.variableSteps = variableSteps;
		this.temp = temp;
		initComponents();
		setSize(500, 200);
	//	setVisible(true);
	}

	private void initComponents() {

		setBackground(new java.awt.Color(240, 240, 240));
		setBackground(new java.awt.Color(240, 240, 240));

		timestepSlider = new JSlider(JSlider.HORIZONTAL,0,variableSteps.length-1,variableSteps.length-1);
		timestepSlider.addChangeListener(new SliderListener());
		timestepSlider.setMajorTickSpacing(10);
		timestepSlider.setPaintTicks(true);

		sliderIndexTitle = 			new JLabel("Slider Value:                                                  ");
		sliderIndexText = 			new JLabel(String.valueOf(variableSteps.length-1));
		timestepsTitle = 			new JLabel("Number of Timesteps:                                ");
		timestepsText = 			new JLabel(String.valueOf(variableSteps[variableSteps.length-1]));
		totalScenariosTitle =		new JLabel("Total Number of Included Scenarios:     ");
		totalScenariosText = 		new JLabel(String.valueOf(temp.get(variableSteps[variableSteps.length-1]).size()));

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		//	javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		//	getContentPane().setLayout(layout);

		JPanel hPanel1 = new JPanel();
		hPanel1.setLayout(new BoxLayout(hPanel1, BoxLayout.X_AXIS));
		hPanel1.add(sliderIndexTitle);
		hPanel1.add(sliderIndexText);
		hPanel1.setAlignmentX(0f);
		add(hPanel1, BorderLayout.LINE_START);
		JPanel hPanel2 = new JPanel();
		hPanel2.setLayout(new BoxLayout(hPanel2, BoxLayout.X_AXIS));
		hPanel2.add(timestepsTitle);
		hPanel2.add(timestepsText);
		hPanel2.setAlignmentX(0f);
		add(hPanel2);
		JPanel hPanel3 = new JPanel();
		hPanel3.setLayout(new BoxLayout(hPanel3, BoxLayout.X_AXIS));
		hPanel3.add(totalScenariosTitle);
		hPanel3.add(totalScenariosText);
		hPanel3.setAlignmentX(0f);
		add(hPanel3);
		
		add(timestepSlider);

	}

	public static void main(String [] args) {
		Integer[] data = new Integer[]{1, 2, 5};

		List<String> strings1 = new ArrayList<String>();
		List<String> strings2 = new ArrayList<String>();
		List<String> strings3 = new ArrayList<String>();
		Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();
		Random rand = new Random();
		int rand1 = rand.nextInt(100);
		for(int i = 0; i < rand1; i++) {
			strings1.add(String.valueOf(rand.nextInt(1000)));
		}
		rand1 = rand.nextInt(100);
		for(int i = 0; i < rand1; i++) {
			strings2.add(String.valueOf(rand.nextInt(1000)));
		}
		rand1 = rand.nextInt(100);
		for(int i = 0; i < rand1; i++) {
			strings3.add(String.valueOf(rand.nextInt(1000)));
		}
		map.put(1, strings1);
		map.put(2, strings1);
		map.put(5, strings1);

		TimestepSelectionSlider slider = new TimestepSelectionSlider(data, map);
		int result = JOptionPane.showConfirmDialog(null, slider, "Timestep Selection", JOptionPane.DEFAULT_OPTION);

	}

	public int getValue(){
		return timestepSlider.getValue();
	}
	
	class SliderListener implements ChangeListener {
		public void stateChanged(ChangeEvent e){
			Integer totalScenarios = 0;
			for(int i = variableSteps.length-1; i>=timestepSlider.getValue(); i--) totalScenarios += temp.get(variableSteps[i]).size();

			sliderIndexText.setText(String.valueOf(timestepSlider.getValue()));
			timestepsText.setText(String.valueOf(variableSteps[timestepSlider.getValue()]));
			totalScenariosText.setText(String.valueOf(totalScenarios));

		}
	}


}
