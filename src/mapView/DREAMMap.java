package mapView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import utilities.Constants;

/**
 * Main class for the google map view, includes hacks to line up grid and select/deselect.
 * The locations and zoom level are approximate and based on a few best-guess constants.
 * @author rodr144
 */

public class DREAMMap {

	private List<Float> xlines = new ArrayList<Float>();
	private List<Float> ylines = new ArrayList<Float>();
	//Default window size
	private int width = 1000;
	private int height = 1000;

	//Values that will be calculated each time we draw but are needed by other functions
	private float pixelsPerMeter;
	private float gridZoomFactor = 1; //1 corresponds to showing the whole grid, zooms in as it gets larger
	private float gridZoomPixelsPerMeter;
	private int pxMax;
	private int pyMax;
	private int adjustedPxMax;
	private int adjustedPyMax;
	private float xMin;
	private float yMin;
	private float xWidth;
	private float yWidth;

	//All of the IJs that we can toggle (or not)
	private List<IJ> boxes;
	
	private DraggedRectangle rect = null;

	//Colors
	private final Color allowedColor = new Color(84,84,84,0);
	private final Color prohibitedColor = new Color(150,150,150,200);
	private final Color lineColor = new Color(255,0,0,255);
	private final Color backgroundColor = new Color(40,97,133,255);

	//Storage for the google images
	//final double circumference = 40075160f; //Circumference of the earth in meters
	private final double circumference = 6378137f*2*Math.PI * 270/200;
	private float lat = 46.3729672f; //latitude of center of domain
	private float lon = -119.2561704f; //longitude of center of domain
	private BufferedImage fullImage = null; //Stores the image once it has been cropped to the proper domain size
	private BufferedImage tempImage = null;
	private ImageObserver observer = null; //Needed for a function but never actually used.

	//Parameters from the controls initialized to defaults
	private int offsetX = 0;
	private int offsetY = 0;
	private int zoom = 50;
	private int rotate = 0;

	private float calculatedPixelsPerMeter;
	public Viewer viewer; // I want this... making it public, to lazy for getter


	public DREAMMap() {
		viewer = new Viewer();
		viewer.setVisible(true);
	}

	public DREAMMap(List<IJ> ijs, List<Float> xLines, List<Float> yLines) {
		this.boxes = ijs;
		this.xlines = Constants.makeLines((ArrayList<Float>) xLines);
		this.ylines = Constants.makeLines((ArrayList<Float>) yLines);
		viewer = new Viewer();
		viewer.setVisible(true);
	}

	public static void main(String[] args) {

		//Load this test set if running just the map tool
		List<Float> xlines = new ArrayList<Float>();
		List<Float> ylines = new ArrayList<Float>();

		int w = 900;
		int h = 900;
		
		//Random rand = new Random();
		//for(int i=0; i<10; i++){
		//	xlines.add(rand.nextFloat()*w);
		//	ylines.add(rand.nextFloat()*h);
		//}

		ylines.add(10f);
		ylines.add(20f);
		xlines.add((float)(w/2));
		ylines.add((float)(h/2));
		xlines.add((float)w);
		ylines.add((float)h);
		Collections.sort(xlines);
		Collections.sort(ylines);
		xlines = Constants.makeLines((ArrayList<Float>) xlines);
		ylines = Constants.makeLines((ArrayList<Float>) ylines);

		new DREAMMap(null, xlines, ylines);
	}


	/*
	 * Class to help keep drawing rectangles when the mouse is dragged
	 */
	public class DraggedRectangle{
		private Point p1, p2;

		public DraggedRectangle(final Point p1, final Point p2){
			this.p1 = p1;
			this.p2 = p2;
		}

		private Point getClick(){
			return this.p1;
		}

		private int xMin(){
			return Math.min(p1.x, p2.x);
		}

		private int xMax(){
			return Math.max(p1.x, p2.x);
		}

		private int yMin(){
			return Math.min(p1.y, p2.y);
		}

		private int yMax(){
			return Math.max(p1.y, p2.y);
		}

		public void draw(Graphics g){
			g.drawLine(p1.x, p1.y, p1.x, p2.y);
			g.drawLine(p1.x, p1.y, p2.x, p1.y);
			g.drawLine(p2.x, p2.y, p1.x, p2.y);
			g.drawLine(p2.x, p2.y, p2.x, p1.y);
		}
	}

	public class Viewer extends JFrame {
		private static final long serialVersionUID = 1L;

		JPanel theGrid;


		public Viewer () {
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			//Do other setup
			setLayout(new BorderLayout());
			setSize(width,height);
			//Initialize the boxes
			if(boxes == null) {
				boxes = new ArrayList<IJ>();
				for(int i=1; i< xlines.size(); i++){
					for(int j=1; j< ylines.size(); j++){ //Only iterate through size-1 as we care about boxes, not about corners
						boxes.add(new IJ(i,j, true, true));
					}
				}
			}

			/*
			 * This panel contains the image and the grid laid on top of it
			 */
			
			theGrid = new JPanel() {
				private static final long serialVersionUID = 1L;

				@Override
				public void paintComponent(Graphics g) {
					//TEST


					//Set a default background
					g.setColor(backgroundColor);
					g.fillRect(0, 0, this.getWidth(), this.getHeight());


					//Calculate values to help with drawing of this iteration
					int xSize = xlines.size();
					int ySize = ylines.size();
					xMin = xlines.get(0);
					yMin = ylines.get(0);
					xWidth = xlines.get(xSize-1) - xMin;
					yWidth = ylines.get(ySize-1) - yMin;
					pixelsPerMeter = Math.min(this.getWidth()/xWidth, this.getHeight()/yWidth);
					gridZoomPixelsPerMeter = pixelsPerMeter*gridZoomFactor;
					pxMax = (int)(xWidth*pixelsPerMeter);
					pyMax = (int)(yWidth*pixelsPerMeter);
					adjustedPxMax = (int)(pxMax*gridZoomPixelsPerMeter/pixelsPerMeter);
					adjustedPyMax = (int)(pyMax*gridZoomPixelsPerMeter/pixelsPerMeter);


					//Draw the map
					if(fullImage == null){
						int googleZoom = 1;


						while(true){
							calculatedPixelsPerMeter = (float) (1024*Math.pow(2, googleZoom)/circumference);
							if(1280/calculatedPixelsPerMeter < Math.sqrt(xWidth*xWidth + yWidth*yWidth)){
								googleZoom--;
								calculatedPixelsPerMeter = (float) (1024*Math.pow(2, googleZoom)/circumference);
								break;
							}
							googleZoom++;
						}
						if(googleZoom > 20) googleZoom = 20;

						System.out.println(pixelsPerMeter + " " + googleZoom );
						try {
							//Test logic for translating to center from upper-left
							CoordinateConversion c = new CoordinateConversion();
							String utm = c.latLon2UTM(lat, lon);
							String[] u = utm.split(" ");
							int easting = Integer.valueOf(u[2]);
							int northing = Integer.valueOf(u[3]);
							easting += xWidth/2;
							northing -= yWidth/2;
							utm = u[0] + " " + u[1] + " " + easting + " " + northing;
							double[] latLon = c.utm2LatLon(utm);
							lat = (float)latLon[0];
							lon = (float)latLon[1];
							//End of test logic
							 
							URL map = new URL("https://maps.googleapis.com/maps/api/staticmap?center=" + lat + "," + lon + "&zoom=" + googleZoom + "&size=640x640&maptype=satellite&scale=2&key=AIzaSyCqMjOt2Q17PnE9-9843sutOpihbglC_6k");
							BufferedImage before = ImageIO.read(map);
							int newImageWidth = (int) (xWidth*calculatedPixelsPerMeter);
							int newImageHeight = (int) (yWidth*calculatedPixelsPerMeter);
							fullImage = before;
							before = before.getSubimage((before.getWidth()-newImageWidth)/2, (before.getHeight()-newImageHeight)/2, newImageWidth, newImageHeight);
							AffineTransform at = new AffineTransform();
							at.scale(pxMax/(double)(before.getWidth()),pyMax/(double)(before.getHeight()));
							AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
							tempImage = scaleOp.filter(before, tempImage);
							g.drawImage(tempImage, 0, 0, observer);
						} catch (MalformedURLException e) {
							e.printStackTrace();
						} catch (IOException e){
							e.printStackTrace();
						}
						//Pad the full image with white
						BufferedImage white = new BufferedImage(2000, 2000, BufferedImage.TYPE_INT_ARGB);
						int startX = white.getWidth()/2 - fullImage.getWidth()/2;
						int endX = white.getWidth()/2 + fullImage.getWidth()/2;
						int startY = white.getHeight()/2 - fullImage.getHeight()/2;
						int endY = white.getHeight()/2 + fullImage.getHeight()/2;
						for(int i=0; i<2000; i++){
							for(int j=0; j<2000; j++){
								if(i >= startX && i < endX && j >=startY && j < endY) white.setRGB(i, j, fullImage.getRGB(i-(white.getWidth()-fullImage.getWidth())/2, j-(white.getHeight()-fullImage.getHeight())/2));
								else white.setRGB(i, j, 0);
							}
						}
						fullImage = white;

					}
					else {
						BufferedImage drawImage = null;
						int newImageWidth = (int) (xWidth*calculatedPixelsPerMeter*(1+(-zoom+50)/200f));
						int newImageHeight = (int) (yWidth*calculatedPixelsPerMeter*(1+(-zoom+50)/200f));
						//tempImage = fullImage.getSubimage((fullImage.getWidth()-newImageWidth)/2+offsetX, (fullImage.getHeight()-newImageHeight)/2+offsetY, newImageWidth, newImageHeight);
						tempImage = fullImage;
						AffineTransform at = new AffineTransform();
						//at.scale(pxMax/(double)(tempImage.getWidth()),pyMax/(double)(tempImage.getHeight()));
						
						//When the slider moves it scales this part of the map.
						//Need to scale the grid so I might need to take some variables and do some new
						//Calculations here.
						at.scale(pxMax/(double)(newImageWidth),pyMax/(double)(newImageHeight));

						at.translate((newImageWidth-fullImage.getWidth())/2-offsetX, (newImageHeight-fullImage.getHeight())/2-offsetY);

						at.rotate(rotate/90f*Math.PI/2, fullImage.getWidth()/2, fullImage.getWidth()/2);

						
						AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
						drawImage = scaleOp.filter(tempImage, drawImage);
						g.drawImage(drawImage, 0, 0, observer);
						makeGridLines(g);
					}



				}

			};
			final JPanel theControls = new Controls(DREAMMap.this) {
				private static final long serialVersionUID = 1L;
				@Override
				public void paintComponent(Graphics g) {
					g.setColor(backgroundColor);
					g.fillRect(0, 0, this.getWidth(), this.getHeight());
				}				
			};

			add(theGrid, BorderLayout.CENTER);
			add(theControls, BorderLayout.EAST);
			theControls.setPreferredSize(new Dimension(width/3, height));

			theGrid.addMouseListener(new MouseAdapter(){
				@Override
				public void mousePressed(MouseEvent e) {
					//System.out.println("Pressed");
					rect = new DraggedRectangle(e.getPoint(), e.getPoint());
					theGrid.validate();
					theGrid.repaint();
				}
				public void mouseReleased(MouseEvent e){
					//System.out.println("Released");

					int x1 = rect.xMin();
					int x2 = rect.xMax();
					int y1 = rect.yMin();
					int y2 = rect.yMax();
					ArrayList<Integer> iList = new ArrayList<Integer>();
					ArrayList<Integer> jList = new ArrayList<Integer>();
					for(int i=0; i<xlines.size()-1; i++){
						if(((int)((xlines.get(i+1)-xMin)*gridZoomPixelsPerMeter) > x1)
								&& ((int)((xlines.get(i)-xMin)*gridZoomPixelsPerMeter) < x2)){
							iList.add(i+1);
						} 
					}
					for(int j=0; j<ylines.size()-1; j++){
					//FLIP
					//	if((adjustedPyMax - (int)((ylines.get(j)-yMin)*gridZoomPixelsPerMeter) > y1)
					//			&& ((adjustedPyMax - (int)((ylines.get(j+1)-yMin)*gridZoomPixelsPerMeter)) < y2)){
						if(((int)((ylines.get(j+1)-yMin)*gridZoomPixelsPerMeter) > y1)
								&& ((int)((ylines.get(j)-yMin)*gridZoomPixelsPerMeter) < y2)){
								jList.add(j+1);
						}
					}
					for(IJ box : boxes){
						if(iList.contains(box.i) && jList.contains(box.j)){
							//Disallow for left click, allow for right
							if(box.selectable && e.getButton() == 1) box.prohibited = false;
							else if(box.selectable && e.getButton() == 3) box.prohibited = true;
							//Toggle for middle click, which will probably just annoy users :)
							else if(box.selectable) box.prohibited = !box.prohibited;
						}
					}

					rect = null;
					theGrid.validate();
					theGrid.repaint();
				}
			});

			theGrid.addMouseMotionListener(new MouseAdapter(){
				@Override
				public void mouseDragged(MouseEvent e){
					//System.out.println("Dragged");
					//Keep updating where the rectangle should be drawn
					rect = new DraggedRectangle(rect.getClick(), e.getPoint());
					theGrid.validate();
					theGrid.repaint();
				}
			});
		}
	}
	
	
	private void makeGridLines(final Graphics g) {
		//Fill Colors
		for(IJ box : boxes){
			int i = box.i;
			int j = box.j;
			int x1 = (int)((xlines.get(i-1)-xMin)*gridZoomPixelsPerMeter);
			int x2 = (int)((xlines.get(i)-xMin)*gridZoomPixelsPerMeter);
			int y1 = (int)((ylines.get(j-1)-yMin)*gridZoomPixelsPerMeter);
			int y2 = (int)((ylines.get(j)-yMin)*gridZoomPixelsPerMeter);
			// FLIP
//			int y1 = (int)(adjustedPyMax - ((ylines.get(j)-yMin))*gridZoomPixelsPerMeter);
//			int y2 = (int)(adjustedPyMax - ((ylines.get(j+1)-yMin))*gridZoomPixelsPerMeter);
			if(box.prohibited) g.setColor(prohibitedColor);
			else g.setColor(allowedColor);
			g.fillRect(x1, y1, x2-x1, y2-y1);
		}

		g.setColor(lineColor);
		//Draw Lines
		for(float x : xlines){
			int px = (int) ((x-xMin)*gridZoomPixelsPerMeter);
			
			g.drawLine(px, 0, px, adjustedPyMax);
//			System.out.println("px is " + px);
		}
//		System.out.println("Repainting" + " The xMin is " + xMin +
//				" the gridZoomPixelsPerMeter is " + gridZoomPixelsPerMeter);
		for(float y : ylines){
			// FLIP
			//int py = adjustedPyMax -(int)((y-yMin)*gridZoomPixelsPerMeter);
			int py = (int) ((y-yMin)*gridZoomPixelsPerMeter);
			g.drawLine(0, py, adjustedPxMax, py);
		}

		//Draw the rectangle
		g.setColor(Color.ORANGE);
		if(rect != null) rect.draw(g);
	}
	
	
//Need to re-initialize the theGrid JPanel with the new parameters
	public void repaint(int offsetX, int offsetY, int zoom, int rotate) {
		//DO THINGS WITH STUFF			
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.zoom = zoom;
		this.rotate = rotate;
		viewer.theGrid.repaint();
		viewer.theGrid.validate();
//		System.out.println("Calling this repaint method." + " Zoom Slider = " + zoom);
	}

	public void redraw(Float lat, Float lon) {
		this.lat = lat;
		this.lon = lon;
		this.zoom = 50;
		this.offsetX = 0;
		this.offsetY = 0;
		this.rotate = 0;
		this.fullImage = null;
		viewer.theGrid.repaint();
		viewer.theGrid.validate();
	}
}
