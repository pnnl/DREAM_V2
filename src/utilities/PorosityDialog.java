package utilities;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import wizardPages.DREAMWizard.STORMData;

/**
 * Dialog that allows the user to select porosity values for the domain to be investigated.
 * Note that DREAM first looks for porosity information in the hdf5 file, but if it cannot find it it will launch this dialog.
 * Users can set up complex spaces using zones, and then save the data in an easily-injestable format for use in future runs.
 * Alternately, files can be written externally and then loaded into DREAM.
 * @author rodr144
 */

public class PorosityDialog extends TitleAreaDialog {
	
	private class ZoneObject{
		public int iMin;
		public int iMax;
		public int jMin;
		public int jMax;
		public int kMin;
		public int kMax;
		public float porosity;
		
		
		public ZoneObject(){
			Point3i dims = data.getSet().getNodeStructure().getIJKDimensions();
			iMin = 1;
			jMin = 1;
			kMin = 1;
			iMax = dims.getI();
			jMax = dims.getJ();
			kMax = dims.getK();
			porosity = 1;
		}
	}

	private ArrayList<ZoneObject> zones;
	private HashMap<Integer, Boolean> readyToGo;
	private ScrolledComposite sc;
	private Composite container;
	private int numZones = 1;
	private float porosity;
	private STORMData data;
	private Shell parentShell;
	private int saveFile = 10;
	private int loadFile = 20;
	
	public PorosityDialog(Shell parentShell, STORMData data) {
		super(parentShell);
		this.parentShell = parentShell;
		this.data = data;
		zones = new ArrayList<ZoneObject>();
		readyToGo = new HashMap<Integer, Boolean>();
	}
	
	@Override
	public void create() {
		super.create();
		setTitle("Set default porosity");
		String message = "No porosity information was detected in the files included for this run.";
		message += "\nEither load the porosity information from an IJK-ordered data file, or set the porosity of zones within the domain using this dialog.";
		message += "\nYou may save this information in the correct format using the save button as well for easy re-loading on future runs.";
		setMessage(message, IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		
		sc = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FILL);
		sc.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 200).create());
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		
		sc.addListener(SWT.Activate, new Listener() {
			public void handleEvent(Event e) {
				sc.setFocus();
			}
		});
		sc.addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(Event event) {
				int wheelCount = event.count;
				wheelCount = (int) Math.ceil(wheelCount / 3.0f);
				while (wheelCount < 0) {
					sc.getVerticalBar().setIncrement(4);
					wheelCount++;
				}

				while (wheelCount > 0) {
					sc.getVerticalBar().setIncrement(-4);
					wheelCount--;
				}
				sc.redraw();
			}
		});
		
		
		container = new Composite(sc, SWT.NONE);
		buildThings();
		return area;
	}
	
	@Override
	protected int getShellStyle(){
		return super.getShellStyle() & (~SWT.RESIZE);
	}
	
	protected void buildThings(){
		for(Control c: container.getChildren()){
			c.dispose();
		}
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(9, false);
		container.setLayout(layout);
		
		createZoneHeaders();
		for(int i=0; i<numZones; i++){
			createZone(i+1);
		}
		createAddButton();

		container.layout();
		sc.setContent(container);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.setLayout(new GridLayout(1,false));
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, loadFile, "Load Porosities From File", false);
		createButton(parent, saveFile, "Save Current Porosities", false);
		createButton(parent, OK, "OK", true);
	}

	protected Button createButton(Composite parent, int id, String label, boolean defaultButton){
		if(id == IDialogConstants.CANCEL_ID) return null;
		return super.createButton(parent, id, label, defaultButton);
	}

	@Override
	protected boolean canHandleShellCloseEvent(){
		return true;
	}
	
	private void createZoneHeaders(){
		Label blank = new Label(container, SWT.NONE);
		blank.setText("");
		Label imin = new Label(container, SWT.NONE);
		imin.setText("Min i");
		Label imax = new Label(container, SWT.NONE);
		imax.setText("Max i");
		Label jmin = new Label(container, SWT.NONE);
		jmin.setText("Min j");
		Label jmax = new Label(container, SWT.NONE);
		jmax.setText("Max j");
		Label kmin = new Label(container, SWT.NONE);
		kmin.setText("Min k");
		Label kmax = new Label(container, SWT.NONE);
		kmax.setText("Max k");
		Label porosity = new Label(container, SWT.NONE);
		porosity.setText("Porosity");
		Label blank2 = new Label(container, SWT.NONE);
		blank2.setText("");
	}
	
	private void createZone(final int i) {
		/*deal with zones that currently exist*/
		ZoneObject zone;
		if(i < zones.size()){
			zone = zones.get(i-1);
		} else{
			zone = new ZoneObject();
			zones.add(zone);
			for(int j=0; j<7; j++){
				readyToGo.put(i*7+j, true);
			}
		}
		
		
		/*make UI stuff*/
		Label zoneLabel = new Label(container, SWT.NONE);
		zoneLabel.setText("Zone " + Integer.valueOf(i));

		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		
		Text iminText = new Text(container, SWT.BORDER);
		iminText.setLayoutData(gridData);
		iminText.setText(String.valueOf(zone.iMin));
		iminText.addModifyListener(new ModifyListener(){
			@Override
			public void modifyText(ModifyEvent e){
				String temp = ((Text)e.getSource()).getText();
				try{
					int x = Integer.valueOf(temp);
					if(x < 1 || x > data.getSet().getNodeStructure().getIJKDimensions().getI() || x > zones.get(i-1).iMax){
						((Text)e.getSource()).setForeground(Constants.red);
						readyToGo.put(7*(i-1), false);
					}
					else{
						zones.get(i-1).iMin = x;
						((Text)e.getSource()).setForeground(Constants.black);
						readyToGo.put(7*(i-1), true);
					}
				} 
				catch(NumberFormatException f){
					((Text)e.getSource()).setForeground(Constants.red);
					readyToGo.put(7*(i-1), false);
				}
			}
		});
		iminText.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {
				//Do nothing
			}
			@Override
			public void focusLost(FocusEvent e) {
				//If we lose focus and it's red, set it to the default so that the other value (max or min) can be set
				//properly. Otherwise we get confusing cases where the UI holds on to the first part of a number typed.
				if(((Text)e.getSource()).getForeground().equals(Constants.red))
					zones.get(i-1).iMin = 1;
			}
		});
		Text imaxText = new Text(container, SWT.BORDER);
		imaxText.setLayoutData(gridData);
		imaxText.setText(String.valueOf(zone.iMax));
		imaxText.addModifyListener(new ModifyListener(){
			@Override
			public void modifyText(ModifyEvent e){
				String temp = ((Text)e.getSource()).getText();
				try{
					int x = Integer.valueOf(temp);
					if(x < 1 || x > data.getSet().getNodeStructure().getIJKDimensions().getI() || x < zones.get(i-1).iMin){
						((Text)e.getSource()).setForeground(Constants.red);
						readyToGo.put(7*(i-1) + 1, false);
					}
					else{
						zones.get(i-1).iMax = x;
						((Text)e.getSource()).setForeground(Constants.black);
						readyToGo.put(7*(i-1) + 1, true);
					}
				} 
				catch(NumberFormatException f){
					((Text)e.getSource()).setForeground(Constants.red);
					readyToGo.put(7*(i-1) + 1, false);
				}
			}
		});
		imaxText.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {
				//Do nothing
			}
			@Override
			public void focusLost(FocusEvent e) {
				//If we lose focus and it's red, set it to the default so that the other value (max or min) can be set
				//properly. Otherwise we get confusing cases where the UI holds on to the first part of a number typed.
				if(((Text)e.getSource()).getForeground().equals(Constants.red))
					zones.get(i-1).iMax = data.getSet().getNodeStructure().getIJKDimensions().getI();
			}
		});
		Text jminText = new Text(container, SWT.BORDER);
		jminText.setLayoutData(gridData);
		jminText.setText(String.valueOf(zone.jMin));
		jminText.addModifyListener(new ModifyListener(){
			@Override
			public void modifyText(ModifyEvent e){
				String temp = ((Text)e.getSource()).getText();
				try{
					int x = Integer.valueOf(temp);
					if(x < 1 || x > data.getSet().getNodeStructure().getIJKDimensions().getJ() || x > zones.get(i-1).jMax){
						((Text)e.getSource()).setForeground(Constants.red);
						readyToGo.put(7*(i-1) + 2, false);
					}
					else{
						zones.get(i-1).jMin = x;
						((Text)e.getSource()).setForeground(Constants.black);
						readyToGo.put(7*(i-1) + 2, true);
					}
				} 
				catch(NumberFormatException f){
					((Text)e.getSource()).setForeground(Constants.red);
					readyToGo.put(7*(i-1) + 2, false);
				}
			}
		});
		jminText.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {
				//Do nothing
			}
			@Override
			public void focusLost(FocusEvent e) {
				//If we lose focus and it's red, set it to the default so that the other value (max or min) can be set
				//properly. Otherwise we get confusing cases where the UI holds on to the first part of a number typed.
				if(((Text)e.getSource()).getForeground().equals(Constants.red))
					zones.get(i-1).jMin = 1;
			}
		});
		Text jmaxText = new Text(container, SWT.BORDER);
		jmaxText.setLayoutData(gridData);
		jmaxText.setText(String.valueOf(zone.jMax));
		jmaxText.addModifyListener(new ModifyListener(){
			@Override
			public void modifyText(ModifyEvent e){
				String temp = ((Text)e.getSource()).getText();
				try{
					int x = Integer.valueOf(temp);
					if(x < 1 || x > data.getSet().getNodeStructure().getIJKDimensions().getJ() || x < zones.get(i-1).jMin){ 
						((Text)e.getSource()).setForeground(Constants.red);
						readyToGo.put(7*(i-1) + 3, false);
					}
					else{
						zones.get(i-1).jMax = x;
						((Text)e.getSource()).setForeground(Constants.black);
						readyToGo.put(7*(i-1) + 3, true);
					}
				} 
				catch(NumberFormatException f){
					((Text)e.getSource()).setForeground(Constants.red);
					readyToGo.put(7*(i-1) + 3, false);
				}
			}
		});
		jmaxText.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {
				//Do nothing
			}
			@Override
			public void focusLost(FocusEvent e) {
				//If we lose focus and it's red, set it to the default so that the other value (max or min) can be set
				//properly. Otherwise we get confusing cases where the UI holds on to the first part of a number typed.
				if(((Text)e.getSource()).getForeground().equals(Constants.red))
					zones.get(i-1).jMax = data.getSet().getNodeStructure().getIJKDimensions().getJ();
			}
		});
		Text kminText = new Text(container, SWT.BORDER);
		kminText.setLayoutData(gridData);
		kminText.setText(String.valueOf(zone.kMin));
		kminText.addModifyListener(new ModifyListener(){
			@Override
			public void modifyText(ModifyEvent e){
				String temp = ((Text)e.getSource()).getText();
				try{
					int x = Integer.valueOf(temp);
					if(x < 1 || x > data.getSet().getNodeStructure().getIJKDimensions().getK() || x > zones.get(i-1).kMax){ 
						((Text)e.getSource()).setForeground(Constants.red);
						readyToGo.put(7*(i-1) + 4, false);
					}
					else{
						zones.get(i-1).kMin = x;
						((Text)e.getSource()).setForeground(Constants.black);
						readyToGo.put(7*(i-1) + 4, true);
					}
				} 
				catch(NumberFormatException f){
					((Text)e.getSource()).setForeground(Constants.red);
					readyToGo.put(7*(i-1) + 4, false);
				}
			}
		});
		kminText.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {
				//Do nothing
			}
			@Override
			public void focusLost(FocusEvent e) {
				//If we lose focus and it's red, set it to the default so that the other value (max or min) can be set
				//properly. Otherwise we get confusing cases where the UI holds on to the first part of a number typed.
				if(((Text)e.getSource()).getForeground().equals(Constants.red))
					zones.get(i-1).kMin = 1;
			}
		});
		Text kmaxText = new Text(container, SWT.BORDER);
		kmaxText.setLayoutData(gridData);
		kmaxText.setText(String.valueOf(zone.kMax));
		kmaxText.addModifyListener(new ModifyListener(){
			@Override
			public void modifyText(ModifyEvent e){
				String temp = ((Text)e.getSource()).getText();
				try{
					int x = Integer.valueOf(temp);
					if(x < 1 || x > data.getSet().getNodeStructure().getIJKDimensions().getK() || x < zones.get(i-1).kMin){ 
						((Text)e.getSource()).setForeground(Constants.red);
						readyToGo.put(7*(i-1) + 5, false);
					}
					else{
						zones.get(i-1).kMax = x;
						((Text)e.getSource()).setForeground(Constants.black);
						readyToGo.put(7*(i-1) + 5, true);
					}
				} 
				catch(NumberFormatException f){
					((Text)e.getSource()).setForeground(Constants.red);
					readyToGo.put(7*(i-1) + 5, false);
				}
			}
		});
		kmaxText.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {
				//Do nothing
			}
			@Override
			public void focusLost(FocusEvent e) {
				//If we lose focus and it's red, set it to the default so that the other value (max or min) can be set
				//properly. Otherwise we get confusing cases where the UI holds on to the first part of a number typed.
				if(((Text)e.getSource()).getForeground().equals(Constants.red))
					zones.get(i-1).kMax = data.getSet().getNodeStructure().getIJKDimensions().getK();
			}
		});
		Text porosityText = new Text(container, SWT.BORDER);
		porosityText.setLayoutData(gridData);
		porosityText.setText(String.valueOf(zone.porosity));
		porosityText.addModifyListener(new ModifyListener(){
			@Override
			public void modifyText(ModifyEvent e){
				String temp = ((Text)e.getSource()).getText();
				try{
					float x = Float.valueOf(temp);
					if(x < 0 || x > 1){
						((Text)e.getSource()).setForeground(Constants.red);
						readyToGo.put(7*(i-1) + 6, false);
					}
					else{
						zones.get(i-1).porosity = x;
						((Text)e.getSource()).setForeground(Constants.black);
						readyToGo.put(7*(i-1) + 6, true);
					}
				} 
				catch(NumberFormatException f){
					((Text)e.getSource()).setForeground(Constants.red);
					readyToGo.put(7*(i-1) + 6, false);
				}
			}
		});
		
		if(i == 1){
			iminText.setEnabled(false);
			imaxText.setEnabled(false);
			jminText.setEnabled(false);
			jmaxText.setEnabled(false);
			kminText.setEnabled(false);
			kmaxText.setEnabled(false);			
			Label fillerLabel = new Label(container, SWT.NONE);
			fillerLabel.setText("");
		}
		else{
			createRemoveButton(i-1);
		}
	}
	
	private void createAddButton() {

		Button b = new Button(container, SWT.PUSH);
		b.setText("Add Zone");
		b.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event arg0) {
				numZones++;
				buildThings();
				sc.getVerticalBar().setSelection(sc.getVerticalBar().getMaximum());
				//createPorosity(container);
			}
		});
	}
	
	private void createRemoveButton(final int indexToRemove) {

		Button b = new Button(container, SWT.PUSH);
		b.setText("Remove Zone");
		b.addListener(SWT.Selection, new Listener(){
			@Override
			public void handleEvent(Event arg0) {
				numZones--;
				zones.remove(indexToRemove);
				for(int i=0; i<7; ++i) readyToGo.put(indexToRemove*7+i, true);
				buildThings();
				sc.getVerticalBar().setSelection(sc.getVerticalBar().getMaximum());
				//createPorosity(container);
			}
		});
	}

	@Override
	protected boolean isResizable() {
		return true;
	}


	@Override
	protected void buttonPressed(int id){
		if(id == OK){
			if(!goodToGo()) return;
			for(ZoneObject zone : zones){
				data.getSet().getNodeStructure().setPorositiesFromZone(zone.iMin, zone.iMax, zone.jMin, zone.jMax, zone.kMin, zone.kMax, zone.porosity);
			}
			super.okPressed();
		}
		else if(id == loadFile){
			FileDialog dialog = new FileDialog(parentShell, SWT.NULL);
			String path = dialog.open();
			if (path != null) {
				try {
					if(data.getSet().getNodeStructure().setPorositiesFromIJKOrderedFile(new File(path))) super.okPressed(); //Porosities loaded, we're done
					else{
						String[] buttons = {"OK"};
						MessageDialog message = new MessageDialog(parentShell, "Load Error", null, "File did not match the provided node structure", 
								MessageDialog.NONE, buttons, 0);
						message.open();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		else if(id == saveFile){
			if(goodToGo()){
				for(ZoneObject zone : zones){
					data.getSet().getNodeStructure().setPorositiesFromZone(zone.iMin, zone.iMax, zone.jMin, zone.jMax, zone.kMin, zone.kMax, zone.porosity);
				}
				FileDialog dialog = new FileDialog(parentShell, SWT.NULL);
				String path = dialog.open();
				if (path != null) {
					try{
						data.getSet().getNodeStructure().writePorositiesToIJKFile(new File(path));
					} catch (Exception e){
						e.printStackTrace();
						String[] buttons = {"OK"};
						MessageDialog message = new MessageDialog(parentShell, "Error Writing to File", null, "An error occurred while trying to write the porosity to the specified file", 
								MessageDialog.NONE, buttons, 0);
						message.open();
					}
				}
			}
			else{
				String[] buttons = {"OK"};
				MessageDialog message = new MessageDialog(parentShell, "Save Error", null, "Please fix errors in porosity dialog before saving the file", 
						MessageDialog.NONE, buttons, 0);
				message.open();
			}
		}
	}
	
	private boolean goodToGo(){
		for(Boolean value : readyToGo.values()){
			if(!value) return false;
		}
		return true;
	}

	public Float getPorosity() {
		return porosity;
	}
}
