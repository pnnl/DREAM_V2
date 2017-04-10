package swingwizard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * I don't think that this is actually used, but am not sure if it is something that provides the basis for th DREAM feel -Luke
 */

public class SwingWizard extends JFrame {

	private static final long serialVersionUID = 1L;

	private int preferredHeight = 1000;
	private int preferredWidth = 600;

	private Map<DefaultMutableTreeNode, JPanel> pages;	
	private JPanel content;

	// The pages
	private Page1SelectData page1;

	public static void main(String[] args) {
		try {
			// Set System L&F
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {

		}

		new SwingWizard().setVisible(true);
	}

	public SwingWizard() {

		pages = new LinkedHashMap<DefaultMutableTreeNode, JPanel>();
		page1 = new Page1SelectData();

		this.setSize(preferredWidth, preferredHeight);
		this.setTitle("STORM Wizard");
		this.setLayout(new BorderLayout());

		JPanel header = new JPanel();
		JPanel buttons = new JPanel();

		JPanel outline = new JPanel();

		header.setBackground(Color.YELLOW);
		header.setPreferredSize(new Dimension(preferredWidth, 60));

		outline.setBackground(Color.CYAN);
		outline.setPreferredSize(new Dimension(200, preferredHeight-120));
		initializeOutline(outline);

		content = new JPanel();
		content.setBackground(Color.GREEN);	

		buttons.setBackground(Color.MAGENTA);
		buttons.setPreferredSize(new Dimension(preferredWidth, 60));

		this.add(outline, BorderLayout.WEST);
		this.add(header, BorderLayout.NORTH);

		this.add(content, BorderLayout.CENTER);
		this.add(buttons, BorderLayout.SOUTH);
	}


	private void initializeOutline(JPanel outline) {

		DefaultMutableTreeNode top = new DefaultMutableTreeNode("Outline");
		DefaultMutableTreeNode initialize = new DefaultMutableTreeNode("Initialize tool");
		DefaultMutableTreeNode scenarioSetup = new DefaultMutableTreeNode("Initialize scenario set");

		DefaultMutableTreeNode selectData = new DefaultMutableTreeNode("Select data");
		DefaultMutableTreeNode selectModel = new DefaultMutableTreeNode("Select model");		

		DefaultMutableTreeNode scenarioProbabiliy = new DefaultMutableTreeNode("Assign scenario probabilities");

		initialize.add(selectData);
		initialize.add(selectModel);
		top.add(initialize);
		pages.put(initialize, page1);

		scenarioSetup.add(scenarioProbabiliy);
		top.add(scenarioSetup);
		JPanel scenarioSetupPanel = new JPanel();
		scenarioSetupPanel.setBackground(Color.ORANGE);
		pages.put(scenarioSetup, scenarioSetupPanel);

		JTree jTree = new JTree(top);
		jTree.addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if(e.getPath() != null) {
					System.out.println(e.getPath().getLastPathComponent());
					if(pages.containsKey(e.getPath().getLastPathComponent())) {
						JPanel newPanel = pages.get(e.getPath().getLastPathComponent());
						content.removeAll();
						content.setLayout(new BorderLayout());
						content.add(newPanel);
						content.revalidate();	
						content.repaint();						
					}
				}
			}			
		});

		for (int i = 0; i < jTree.getRowCount(); i++) {
			jTree.expandRow(i);
		}

		DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) jTree.getCellRenderer();
		renderer.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 11));
		renderer.setLeafIcon(null);
		renderer.setClosedIcon(null);
		renderer.setOpenIcon(null);

		jTree.setBackground(outline.getBackground());
		outline.setLayout(new BorderLayout());
		outline.add(jTree, BorderLayout.WEST);
	}

	private class Page1SelectData extends JPanel {

		private static final long serialVersionUID = 1L;

		JFileChooser fileChooser;

		public Page1SelectData() {
			this.setBackground(Color.BLUE);
			this.setLayout(new GridBagLayout());

			fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Select a directory containing hdf5 files");
			fileChooser.setCurrentDirectory(new File("."));
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			JButton selectDirectory = new JButton("Select a directory");
			selectDirectory.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int result = fileChooser.showOpenDialog(Page1SelectData.this);
					if(result == JFileChooser.APPROVE_OPTION) {
						System.out.println(fileChooser.getSelectedFile());
					}
				}				
			});

	        java.awt.GridBagConstraints gridBagConstraints;

	        JTextField jTextField1 = new javax.swing.JTextField();
	        JLabel jLabel1 = new javax.swing.JLabel();
	        JLabel jLabel2 = new javax.swing.JLabel();
	        JComboBox jComboBox1 = new javax.swing.JComboBox();
	        JComboBox jComboBox2 = new javax.swing.JComboBox();

	        setLayout(new java.awt.GridBagLayout());

	        jTextField1.setText("jTextField1");
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 1;
	        gridBagConstraints.gridy = 0;
	        gridBagConstraints.gridwidth = 2;
	        gridBagConstraints.ipadx = 269;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        gridBagConstraints.insets = new java.awt.Insets(12, 6, 0, 10);
	        add(jTextField1, gridBagConstraints);

	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 0;
	        gridBagConstraints.gridheight = 2;
	        gridBagConstraints.ipadx = 26;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        gridBagConstraints.insets = new java.awt.Insets(11, 10, 0, 0);
	        add(selectDirectory, gridBagConstraints);

	        jLabel1.setText("jLabel1");
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 2;
	        gridBagConstraints.ipadx = 65;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        gridBagConstraints.insets = new java.awt.Insets(14, 10, 0, 0);
	        add(jLabel1, gridBagConstraints);

	        jLabel2.setText("jLabel2");
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 0;
	        gridBagConstraints.gridy = 4;
	        gridBagConstraints.ipadx = 65;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        gridBagConstraints.insets = new java.awt.Insets(17, 10, 0, 0);
	        add(jLabel2, gridBagConstraints);

	        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 1;
	        gridBagConstraints.gridy = 2;
	        gridBagConstraints.gridheight = 2;
	        gridBagConstraints.ipadx = 75;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        gridBagConstraints.insets = new java.awt.Insets(11, 6, 0, 0);
	        add(jComboBox1, gridBagConstraints);

	        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
	        gridBagConstraints = new java.awt.GridBagConstraints();
	        gridBagConstraints.gridx = 1;
	        gridBagConstraints.gridy = 4;
	        gridBagConstraints.gridheight = 2;
	        gridBagConstraints.ipadx = 75;
	        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
	        gridBagConstraints.insets = new java.awt.Insets(14, 6, 201, 0);
	        add(jComboBox2, gridBagConstraints);

		}
	}

}
