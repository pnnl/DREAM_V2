package mapView;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * class for putting together the UI as a whole, including controls to zoom in and out, pan, etc.
 * @author port091
 * @author rodr144
 */

public class Controls extends javax.swing.JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	private DREAMMap map = null;
	private CoordinateConversion converter = new CoordinateConversion();
	
	public int offsetX;
	public int offsetY;
	public int zoom;
	public int rotate; 
	
	public Controls() {
		
		offsetX = 0;
		offsetY = 0;
		zoom = 50;
		rotate = 0;
		
        initComponents();
    }
	
	public Controls(DREAMMap map) {
		
		this.map = map;
		offsetX = 0;
		offsetY = 0;
		zoom = 50;
		rotate = 0;
		
        initComponents();
    }

    private void initComponents() {

        latitudeField = new javax.swing.JTextField();
        latitudeLabel = new javax.swing.JLabel();
        longitudeField = new javax.swing.JTextField();
        longitudeLabel = new javax.swing.JLabel();
        selectLatLonButton = new javax.swing.JButton();
        utmField = new javax.swing.JTextField();
        utmLabel = new javax.swing.JLabel();
        selectUtmButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        zoomSlider = new javax.swing.JSlider();
        
        
        //Currently set to the same default as the DREAMMap class lat/long
        latitudeField.setText("46.3729672");
        longitudeField.setText("-119.2561704");
        
        selectLatLonButton.addActionListener(new ActionListener() {
        	@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Select button clicked!");
				offsetX = 0;
				offsetY = 0;
				rotate = 0;
				zoom = 50;				
				zoomSlider.setValue(zoom);
				
				float lat = Float.valueOf(latitudeField.getText());
				float lon = Float.valueOf(longitudeField.getText());
				utmField.setText(converter.latLon2UTM(lat, lon));
				map.redraw(lat, lon);
			}
        });
        
        utmField.setText(converter.latLon2UTM(46.3729672, -119.2561704));
        selectUtmButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				try{
					double[] x = converter.utm2LatLon(utmField.getText());
					offsetX = 0;
					offsetY = 0;
					rotate = 0;
					zoom = 50;				
					zoomSlider.setValue(zoom);
					latitudeField.setText(String.valueOf(x[0]));
					longitudeField.setText(String.valueOf(x[1]));
					map.redraw((float)x[0], (float)x[1]);
				}
				catch(Exception exception){
					System.out.println("Invalid UTM");
				}
			}
        	
        });
        // 0 to 100?
        zoomSlider.setMinimum(0);
        zoomSlider.setMaximum(100);

        panUpButton = newButton(new File("./img"), "arrow_up_blue", "Pan up");
        panRightButton = newButton(new File("./img"), "arrow_right_blue", "Pan right");
        panDownButton = newButton(new File("./img"), "arrow_down_blue", "Pan down");
        panLeftButton = newButton(new File("./img"), "arrow_left_blue", "Pan left");

        rotateCWButton = newButton(new File("./img"), "redo", "Rotate clockwise");
        rotateCCWButton  = newButton(new File("./img"), "undo", "Rotate counter clockwise");

        zoomInButton = new javax.swing.JButton("+");
        zoomOutButton = new javax.swing.JButton("-");
        latitudeLabel.setText("Latitude");
        latitudeLabel.setForeground(Color.white);
        longitudeLabel.setText("Longitude");
        longitudeLabel.setForeground(Color.white);
        utmLabel.setText("UTM Cords");
        utmLabel.setForeground(Color.white);
        selectLatLonButton.setText("Select");
        selectUtmButton.setText("Select");

        panUpButton.addActionListener(this);
        panDownButton.addActionListener(this);
        panLeftButton.addActionListener(this);
        panRightButton.addActionListener(this);
        rotateCWButton.addActionListener(this);
        rotateCCWButton.addActionListener(this);
        zoomInButton.addActionListener(this);
        zoomOutButton.addActionListener(this);
        
        panUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	if((evt.getModifiers() & java.awt.event.ActionEvent.SHIFT_MASK) != 0) offsetY -= 10;
            	else offsetY--;
            }
        });
        panDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	if((evt.getModifiers() & java.awt.event.ActionEvent.SHIFT_MASK) != 0) offsetY += 10;
            	else offsetY++;
            }
        });
        panLeftButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	if((evt.getModifiers() & java.awt.event.ActionEvent.SHIFT_MASK) != 0) offsetX -= 10;
            	else offsetX--;
            }
        });
        panRightButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	if((evt.getModifiers() & java.awt.event.ActionEvent.SHIFT_MASK) != 0) offsetX += 10;
            	else offsetX++;
            }
        });
        rotateCWButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	if((evt.getModifiers() & java.awt.event.ActionEvent.SHIFT_MASK) != 0) rotate += 10;
            	else rotate++;
            }
        });
        rotateCCWButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	if((evt.getModifiers() & java.awt.event.ActionEvent.SHIFT_MASK) != 0) rotate -= 10;
            	else rotate--;
            }
        });       
        zoomInButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	if((evt.getModifiers() & java.awt.event.ActionEvent.SHIFT_MASK) != 0) zoom += 10;
            	else zoom++;
            	if(zoom > 100)
            		zoom = 100;
            	if(zoom < 0)
            		zoom = 0;
            	zoomSlider.setValue(zoom);
            }
        });
        zoomOutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	if((evt.getModifiers() & java.awt.event.ActionEvent.SHIFT_MASK) != 0) zoom -= 10;
            	else zoom--;
            	if(zoom > 100)
            		zoom = 100;
            	if(zoom < 0)
            		zoom = 0;
            	zoomSlider.setValue(zoom);
//            	repaint(0, 0, zoomSlider.getValue(), 0);
            }
        });
        
        zoomSlider.setOrientation(javax.swing.JSlider.VERTICAL);
        zoomSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				zoom = zoomSlider.getValue();
				actionOccurred();
			}       	
        });
     
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(rotateCCWButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(rotateCWButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(zoomOutButton, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(panLeftButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(32, 32, 32)
                                .addComponent(panRightButton, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(43, 43, 43)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(panDownButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(panUpButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(50, 50, 50)
                                .addComponent(zoomInButton, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addGap(18, 18, 18)
                .addComponent(zoomSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(panUpButton)
                            .addComponent(zoomInButton))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(panRightButton)
                                    .addComponent(panLeftButton))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panDownButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(rotateCCWButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(rotateCWButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(58, 58, 58))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(zoomOutButton))))
                    .addComponent(zoomSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                	.addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(selectLatLonButton))      
                	.addGroup(layout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addComponent(selectUtmButton))      
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(longitudeLabel)
                            .addComponent(latitudeLabel)
                            .addComponent(utmLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(latitudeField)
                            .addComponent(longitudeField)
                            .addComponent(utmField)))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(latitudeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(latitudeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(longitudeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(longitudeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectLatLonButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(utmField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(utmLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectUtmButton)
               .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(370, Short.MAX_VALUE))
        );
    }
    
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField latitudeField;
    private javax.swing.JLabel latitudeLabel;
    private javax.swing.JTextField longitudeField;
    private javax.swing.JLabel longitudeLabel;
    private javax.swing.JTextField utmField;
    private javax.swing.JLabel utmLabel;
    private javax.swing.JButton panDownButton;
    private javax.swing.JButton panLeftButton;
    private javax.swing.JButton panRightButton;
    private javax.swing.JButton panUpButton;
    private javax.swing.JButton rotateCCWButton;
    private javax.swing.JButton rotateCWButton;
    private javax.swing.JButton selectLatLonButton;
    private javax.swing.JButton selectUtmButton;
    private javax.swing.JButton zoomInButton;
    private javax.swing.JButton zoomOutButton;
    private javax.swing.JSlider zoomSlider;

	
    /**
     * Returns a button set up with icon, pressed icon, rollover icon, and toolerrorString.
     */
    public static JButton newButton(File iconFolder, String buttonName, String tooltip) {

      File base  = new File(iconFolder.getAbsolutePath() + "/" + buttonName + ".png");

      javax.swing.JButton newButton = new JButton(new ImageIcon(base.getAbsolutePath()));
      newButton.setPressedIcon(new ImageIcon(base.getAbsolutePath()));
      newButton.setRolloverIcon(new ImageIcon(base.getAbsolutePath()));
      newButton.setToolTipText(tooltip);
      newButton.setBorderPainted(false); // For Mac
      newButton.setContentAreaFilled(false);
      newButton.setFocusable(false);

      return newButton;

    }
  	
    
	@Override
	public void actionPerformed(ActionEvent e) {
		actionOccurred();
	}
	
	public void actionOccurred() {
		if(map != null){
			map.repaint(offsetX, offsetY, zoom, rotate);
		}
		System.out.println(offsetX + ", " + offsetY + ", " + zoom + ", " + rotate);
		
	}
	
	public void setLatLongUTM(final String theLat, final String theLong, final String theUTM) {
		latitudeField.setText(theLat);
		longitudeField.setText(theLong);
		utmField.setText(theUTM);
	}
	
}
