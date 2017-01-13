package hdf5Tool;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * checklist for the filebrowser interface
 * @author port091
 */

public class CheckList extends JScrollPane {
	
	private static final long serialVersionUID = 391900764189521629L;

	private JCheckBoxList jList;

	public CheckList() {
		jList = new JCheckBoxList();
		jList.setBackground(new java.awt.Color(240, 240, 240));
		jList.setListData(new JCheckBox[]{});
		setViewportView(jList);
	}
	
	public void setListData(Object[] data, boolean selected) {
		JCheckBox[] cbs = new JCheckBox[data.length];
		for(int i = 0; i < data.length; i++) {
			cbs[i] = new JCheckBox(data[i].toString());
			cbs[i] .setSelected(selected);
		}
		jList.setListData(cbs);	
	}
	
	public JCheckBox[] getListData() {
		JCheckBox[] cbs = new JCheckBox[jList.getModel().getSize()];
		for(int i = 0; i < cbs.length; i++) {
			cbs[i] = jList.getModel().getElementAt(i);
		}
		return cbs;
	}

	public class JCheckBoxList extends JList<JCheckBox>
	{
		private static final long serialVersionUID = 8986501004359689581L;
		protected final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

		public JCheckBoxList()
		{
			setCellRenderer(new CheckBoxCellRenderer());
			addMouseListener(new MouseAdapter()
			{
				public void mousePressed(MouseEvent e)
				{
					int index = locationToIndex(e.getPoint());
					if (index != -1)
					{
						JCheckBox checkbox = (JCheckBox) getModel().getElementAt(index);
						checkbox.setSelected(!checkbox.isSelected());
						repaint();
					}
				}
			});

			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}

		protected class CheckBoxCellRenderer implements ListCellRenderer<JCheckBox>
		{
			@Override
			public Component getListCellRendererComponent(JList<? extends JCheckBox> list, JCheckBox checkbox, int index, boolean isSelected, boolean cellHasFocus) {
				checkbox.setBackground(isSelected ? getSelectionBackground() : getBackground());
				//checkbox.setForeground(isSelected ? getSelectionForeground() : getForeground());

				checkbox.setEnabled(isEnabled());
				checkbox.setFont(getFont());
				checkbox.setFocusPainted(false);

				checkbox.setBorderPainted(true);
				checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);

				return checkbox;
			}
		}
	}
}
