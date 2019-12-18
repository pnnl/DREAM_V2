/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gridviz;

import gridviz.DataGrid.AXIS;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point2i;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;
/*
import xfer.GradientOption;
import xfer.TransferFunctionSingleton;
*/
/**
 * @brief  Provides a two-dimensional slice of grid data
 *         Also provides mechanism for adjusting the numeric range of the data
 *         and converting the slice data to an image
 * @author Tucker Beck
 * @date   3/7/12
 */
public class DataSlice {

	/// The values contained within this slice
	private float values[][];
	
	// Describes an epsilon value for floating point arithmetic
	private static final double EPS = 1e-6;
	
	/// The width of the slice
	private int w;

	/// The height of the slice
	private int h;

	private float step; // The distance 1 pixel represents
	private float intersection; // xyz of the intersection

	/// The extrema for the values in this slice
	private Extrema extrema; // This one will be set by the data in the slice
	private Extrema globalExtrema;	// This one will be set by the global extrema
	
	//private Extrema scaledExtrema;

	private boolean useGlobalExtrema;

	private Vector3f min; // In order to handle the zoom we need to know 
	private Vector3f max; // what the min and max xyz values are 

	private AXIS xAxis; // Grid axis that the x axis is showing
	private AXIS yAxis; // Grid axis that the y axis is showing
	private AXIS intersectionAxis; // Axis that the slice runs through

	private boolean renderAxis;
	private boolean renderTickMarks;
	private boolean renderMesh;
	
	private String fieldKey;

	private Map<AXIS, List<Integer>> gridLines;

	/// Describes options for annotating an image
	public enum Annotation {
		NONE,      ///< Draw no annotations
		INTERSECT, ///< Draw only annoations intersecting the slice
		ALL        ///< Draw all annotations
	}
	private List<AnnotatedPosition> annotations;
	private Annotation annotationOption;

	protected static final Color BACKGROUND_COLOR = new java.awt.Color(248, 248, 248);
	protected static final Color GRID_LINE_COLOR = new java.awt.Color(100, 100, 100);
	protected static final Color TICK_COLOR = new java.awt.Color(50, 50, 50);
	protected static final Color FONT_COLOR = new java.awt.Color(30,30,30);
	protected static final Font TITLE_FONT = new Font("Tahoma", Font.PLAIN, 12);
	protected static final Font LABEL_FONT = new Font("Tahoma", Font.PLAIN, 10);
	protected static final Font LABEL_FONT_BOLD = new Font("Tahoma", Font.BOLD, 10);	
	protected static final Font TICK_FONT = new Font("Tahoma", Font.PLAIN, 9);

	public DataSlice(int w, int h, Vector3f min, Vector3f max, AXIS xAxis, AXIS yAxis, AXIS intersectionAxis, float step, float intersection, 
			List<Integer> xGridLines, List<Integer> yGridLines, List<Integer> zGridLines, Extrema globalExtrema, String fieldKey) {
		// Image size (pixels)
		this.w = w;
		this.h = h;
		
		this.fieldKey = fieldKey;

		// Axis we're representing
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		this.intersectionAxis = intersectionAxis;

		// Values in the image
		values = new float[h][w];
		for (int i=0; i<h; i++)
			for (int j=0; j<w; j++)
				values[i][j] = 0.0f;

		// Min and max values from just the image
		extrema = new Extrema();

		// Min and max values from the whole grid
		this.globalExtrema = new Extrema(globalExtrema);

		// Zoom for the slice
		this.min = new Vector3f(min);
		this.max = new Vector3f(max);

		// Distance a pixel represents
		this.step = step;

		// Point of intersection
		this.intersection = intersection;

		// Location of the gird lines		
		List<Integer> xTemp = new ArrayList<Integer>();
		List<Integer> yTemp = new ArrayList<Integer>();
		List<Integer> zTemp = new ArrayList<Integer>();
		for(Integer x: xGridLines)
			xTemp.add(x);
		for(Integer y: yGridLines)
			yTemp.add(y);
		for(Integer z: zGridLines)
			zTemp.add(z);
		Collections.sort(xTemp);
		Collections.sort(yTemp);
		Collections.sort(zTemp);

		this.gridLines = new HashMap<AXIS, List<Integer>>();
		this.gridLines.put(AXIS.X, xTemp);
		this.gridLines.put(AXIS.Y, yTemp);
		this.gridLines.put(AXIS.Z, zTemp);

		// Default settings
		annotationOption = Annotation.ALL;	// Which to show
		useGlobalExtrema = true;	// Uses the global extrema for the gradient
		renderAxis = false;	// Renders the axis (XY, YZ, etc.)
		renderTickMarks = false;	// Renders tick marks
		renderMesh = false;	// Renders the mesh of the grid
	}

	public DataSlice makeCopy () {
		DataSlice temp = new DataSlice(w, h, min, max, xAxis, yAxis, intersectionAxis, step, intersection, 
				gridLines.get(AXIS.X), gridLines.get(AXIS.Y), gridLines.get(AXIS.Z), globalExtrema, fieldKey);
		for (int i=0; i<h; i++) 
			for (int j=0; j<w; j++)
				temp.setValue(i, j, values[i][j]);	
		temp.annotations = new ArrayList<AnnotatedPosition>();
		for(AnnotatedPosition annotation: annotations)
			temp.annotations.add(annotation);
		return temp;
	}

	public void setRenderTickMarks(boolean renderTickMarks) {
		this.renderTickMarks = renderTickMarks;
	}

	public void setRenderMesh(boolean renderMesh) {
		this.renderMesh = renderMesh;
	}	

	public void setRenderAxis(boolean renderAxis) {
		this.renderAxis = renderAxis;
	}

	public void setAnnotationOption(Annotation annotationOption) {
		this.annotationOption = annotationOption;
	}

	public void setUseGlobalExtrema(boolean useGlobalExtrema) {
		this.useGlobalExtrema = useGlobalExtrema;
	}

	public AXIS getXAxis() {
		return xAxis;
	}
	
	public AXIS getYAxis() {
		return yAxis;
	}
	
	public Vector3f getMin() {
		return new Vector3f(min);
	}
	
	public Vector3f getMax() {
		return new Vector3f(max);
	}
	
	public float getIntersection() {
		return intersection;
	}
	
	public String getFieldKey() {
		return fieldKey;
	}
	
	public void setAnnotations(List<AnnotatedPosition> aps) throws GridError {
		try {
			if(annotations == null)
				annotations = new ArrayList<AnnotatedPosition>();
			boolean intersecting = false;
			for (AnnotatedPosition ap : aps) {
				Point2i ipos = new Point2i();
				if(ap.getPosition() != null) {
					Vector3f pos = ap.getPosition();
					ipos.x = (int) Math.round( ( getValue(pos, xAxis) - getValue(min, xAxis) ) / step);
					ipos.y = h - (int) Math.round( ( getValue(pos, yAxis) - getValue(min, yAxis) ) / step);
					intersecting = Math.abs(getValue(pos, intersectionAxis) - intersection) < 1e-6;
				} else if( ap.get_ijkLocation() != null) {
					Point3i ijk = ap.get_ijkLocation();
					Point3i finalIJK = new Point3i();
					for(AXIS axis: AXIS.values()) {
						List<Integer> lines = gridLines.get(axis);
						int index = getValue(ijk, axis);
						if(lines.size() == 1) { // Only one value here
							setValue(finalIJK, axis, lines.get(0));
						} else {
							float difference = lines.get(index)-lines.get(index-1);
							float middle = difference/2 + lines.get(index-1);
							setValue(finalIJK, axis, (int) Math.round(middle));
						}
					}
					ipos.x = getValue(finalIJK, xAxis);
					ipos.y = h - getValue(finalIJK, yAxis);
					intersecting = Math.abs((getValue(finalIJK, intersectionAxis)*step) - intersection) < 1e-6;
				} else {
					throw new GridError("No position data specified for annotation");
				}
				ap.setIntersectsSlice(intersecting);
				ap.setSlicePosition(ipos);
				annotations.add(ap);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new GridError(e.getMessage());
		}
	}	


	/// Sets the value for a position on the slice
	public void setValue(int i, int j, float value) {
		values[i][j] = value;
		extrema.min = Math.min(extrema.min, value);
		extrema.max = Math.max(extrema.max, value);
	}

	/// Normalizes the values in this slice using Min/Max normalization
	private void minMaxNormalize() {
		Extrema extrema = getExtrema();
		float range = extrema.max - extrema.min;
		float value;
		String exceptionDescription = "";
		for (int i=0; i<h; i++)
			for (int j=0; j<w; j++) {

				value = values[i][j];

				// Truncates the value if it is outside of the extrema
				if (value > extrema.max) {
					System.out.println("This should never happen! Value greater than extrema");
					exceptionDescription += "This should never happen! Value greater than extrema. value: " + value + " extrema.max: " + extrema.max + "\n";
					value = 1.0f;
				} else if (value < extrema.min) {
					exceptionDescription += "This should never happen! Value less than extrema. value: " + value + " extrema.min: " + extrema.min + "\n";
					System.out.println("This should never happen! Value less than extrema");
					value = 0.0f;
				} else {
					value = (value - extrema.min) / range;
				}
				if(Float.isNaN(value)) {
					value = 0;
				}
				values[i][j] = value;
			}
		if(exceptionDescription.length() > 0){
			throw new RuntimeException(exceptionDescription);
		}
	}

	/// Scales the data using a base 10 logarithm
	/*
	private void log10Scale() {

	//	Extrema extrema = getExtrema();

		for (int i=0; i<h; i++)
			for (int j=0; j<w; j++)
				values[i][j] = Math.log10(values[i][j] + 1.0 + Utilities.EPS);

	//	scaledExtrema = new Extrema();
	//	scaledExtrema.min = Math.log10(extrema.min + 1.0 + Utilities.EPS);
	//	scaledExtrema.max = Math.log10(extrema.max + 1.0 + Utilities.EPS);
	}*/

	/// Scales the data using a natural logarithm
	private void lnScale() {
	//	Extrema extrema = getExtrema();
		for (int i=0; i<h; i++)
			for (int j=0; j<w; j++)
				values[i][j] = (float)Math.log(values[i][j] + 1.0 + EPS);
		
	//	scaledExtrema = new Extrema();
	//	scaledExtrema.min = Math.log(extrema.min + 1.0 + Utilities.EPS);
	//	scaledExtrema.max = Math.log(extrema.max + 1.0 + Utilities.EPS);
	}

	/// Creates a gray-scale image from this slice
	/*
	public BufferedImage renderGray() {

		/// @todo Check Normalization
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
		byte pixels[] = new byte[w * h];

		WritableRaster raster = image.getRaster();

		int index = 0;
		for (int i=0; i<h; i++)
			for (int j=0; j<w; j++, index++)
				pixels[index] = (byte) (values[i][j] * 255);

		raster.setDataElements(0, 0, w, h, pixels);

		return finalizeImage(image);
	}*/

	/**
	 * @brief  Fetches a color image from this slice
	 *         The colors in the image are calculated using linear interpolation
	 *         between the two provided colors.
	 */
	/*
	private BufferedImage renderBGR(Color color0, Color color1) {

		int red0   = color0.getRed();
		int green0 = color0.getGreen();
		int blue0  = color0.getBlue();

		int dRed   = color1.getRed()   - red0;
		int dGreen = color1.getGreen() - green0;
		int dBlue  = color1.getBlue()  - blue0;

		Color color;

		int red, green, blue;

		BufferedImage image = new BufferedImage(w, h,
				BufferedImage.TYPE_3BYTE_BGR);

		for (int i=0; i<h; i++) {
			for (int j=0; j<w; j++) {

				red   = (int)(values[i][j] * dRed   + red0);
				green = (int)(values[i][j] * dGreen + green0);
				blue  = (int)(values[i][j] * dBlue  + blue0);
				color = new Color(red, green, blue);
				image.setRGB(j, i, color.getRGB());
			}
		}

		return finalizeImage(image);
	}*/

	/// Fetches an image that has been colored using a gradient
	public BufferedImage renderGradient(int gradientIndex) {

		// TODO: For now these are defaults
		minMaxNormalize();
		lnScale();
		
		// Fetch the gradient calculation object for the given index
	//	TransferFunctionSingleton xfer = TransferFunctionSingleton.getInstance();
	//	GradientOption gradient = xfer.getGradient(gradientIndex);
	
		BufferedImage image = new BufferedImage(w, h,
				BufferedImage.TYPE_3BYTE_BGR);

		for (int i=0; i<h; i++) {
			for (int j=0; j<w; j++) {
			//	Color color = gradient.getGradientColor((float)values[i][j]);
			//	image.setRGB(j, i, color.getRGB());
			}
		}

		return finalizeImage(image);
	}
	
	public List<Float> getGradientValues(float min, float max, float step) {
		List<Float> gradientVals = new ArrayList<Float>();
		gradientVals.add(min);
		while(gradientVals.get(gradientVals.size()-1)+step <= max) {
			gradientVals.add(gradientVals.get(gradientVals.size()-1)+step);
		}
		return gradientVals;
	}
	
	public List<Float> getGradientValues(float min, float max, int count) {
		Float step = (max - min) / (count-1);
		return getGradientValues(min, max, step);
	}
	
	public List<Float> getGradientValues(int count) {
		Extrema minMax = new Extrema(getExtrema());
		return getGradientValues((float)minMax.min, (float)minMax.max, count);
	}
	public List<Float> getGradientValues(float step) {
		Extrema minMax = new Extrema(getExtrema());
		return getGradientValues((float)minMax.min,(float) minMax.max, step);
	}
	
	
	/// Fetches an image that has been colored using a gradient
	public BufferedImage renderBanded(int gradientIndex, List<Float> gradientValues) throws GridError {

		// TODO: For now these are defaults
		minMaxNormalize();
		lnScale();
		Extrema extrema = new Extrema(getExtrema());
		
		// Make sure that our gradient values contains the min and max.
		if(!gradientValues.contains((float)extrema.min))
			gradientValues.add((float)extrema.min);
		
		if(!gradientValues.contains((float)extrema.max))
			gradientValues.add((float)extrema.max);		
		// Normalize
		List<Float> normalized = new ArrayList<Float>();
		if(Float.compare(extrema.max, extrema.min) == 0) {
			normalized.add(0.0f);
			normalized.add(1.0f);
		} else {
			for(float val: gradientValues) {	
				normalized.add((float) ((val - extrema.min)/(extrema.max-extrema.min)));
			}
		}

		List<Float> lnScaled = new ArrayList<Float>();
		for(float val: normalized) {
			lnScaled.add((float) Math.log(val + 1.0 + EPS));
		}
		Collections.sort(lnScaled);
		System.out.println("Scaled values: " + lnScaled.toString());
		
		// Fetch the gradient calculation object for the given index
	//	TransferFunctionSingleton xfer = TransferFunctionSingleton.getInstance();
	//	GradientOption gradient = xfer.getGradient(gradientIndex);
		
		Color color = null;

		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);

		for (int i=0; i<h; i++) {
			for (int j=0; j<w; j++) { 
				float current = (float) values[i][j];
				for(int x = 0; x < lnScaled.size()-1; x++) {
					if((Float.compare(lnScaled.get(x), current) <= 0) &&
							(Float.compare(lnScaled.get(x+1), current) >= 0)) {
					//	color = gradient.getGradientColor(lnScaled.get(x));
						break;
					} 
				}
				if(color == null)
					throw new GridError ("Could not find a color value for given value: " + current);
				image.setRGB(j, i, color.getRGB());
			}
		}

		return finalizeImage(image, gradientValues, lnScaled);// gradient);
	}

	/// Fetches the extrema for the values in this slice
	private Extrema getExtrema() {
		if(useGlobalExtrema)
			return globalExtrema;
		else
			return extrema;
	}

	private BufferedImage finalizeImage(BufferedImage image, List<Float> divisions, List<Float> values) {
		//BufferedImage imageOut = finalizeImage(image);
		
		// Create the gradient
		BufferedImage scale = createGradient(divisions,  values);
		
		return scale;
	}
	
	private BufferedImage finalizeImage(BufferedImage image) {

		boolean renderBorder = renderAxis || renderTickMarks || renderMesh || 
				(annotationOption != Annotation.NONE && annotations != null);

		int borderWidth = 0;
		if(renderBorder)
			borderWidth += 40;
		if(renderTickMarks)
			borderWidth += 50;
		else if(renderAxis)
			borderWidth += 20;
		
		BufferedImage border = createEmptyBorder(image, borderWidth);

		addBorder(border, borderWidth);


		if(renderAxis)
			addAxis(border, borderWidth);

		if(renderTickMarks)
			addTickMarks(border, borderWidth);

		// Attaches the border
		BufferedImage newImage = appendBorder(image, border, borderWidth);

		if(renderMesh) {
			addGridLines(newImage, borderWidth);
		}

		// Annotate the slice if the Annotation flag is ALL or INTERSECT
		if (annotationOption != Annotation.NONE && annotations != null) {
			for(AnnotatedPosition ap : annotations) {
				// Only add the annotation if it intersects the slice (or ALL)
				if (annotationOption == Annotation.ALL || ap.getIntersectsSlice()) {
					newImage = addAnnotation(newImage, ap, borderWidth); // We should add these to the final image so any run off goes into the margins
				}
			}
		}
		
		//if(addScale) {
		//	addScale();
		//}
		
		return newImage;
	}

	private BufferedImage createGradient(List<Float> divisions, List<Float> values) {
	
		int h = 200;
		int w = 100;
		BufferedImage newImage = new BufferedImage(w,h+20,BufferedImage.TYPE_3BYTE_BGR);
		/*
		
		for(int i = 0; i < h+20; i++) {
			for(int j = 0; j < w; j++) {
				newImage.setRGB(j, i, BACKGROUND_COLOR.getRGB());
			}
		}
		
		System.out.println("Divisions: " + divisions.toString());
		System.out.println("Values: " + values.toString());
		int numBlocks = (h/(values.size()-1));
		System.out.println("Number of blocks: " + numBlocks);
		Color previousColor = null;
		
		for(int i = 0; i < h; i++) {
			int currentColor = Math.round((float)i/(float)numBlocks);
		//	Color nextColor = gradient.getGradientColor(values.get(currentColor));
			for(int j = 0; j < 20; j++) {
				if(j == 0 || j == 19 || previousColor == null || !nextColor.equals(previousColor) || i==h-1) {
					newImage.setRGB(j, i+10, GRID_LINE_COLOR.getRGB());
				} else {
					//newImage.setRGB(j, i+10, nextColor.getRGB());
				}							
			}
			for(int j = 30; j < 50; j++) {
				if(j == 30 || j == 49 || i==0 || i == h-1)
					newImage.setRGB(j, i+10, GRID_LINE_COLOR.getRGB());
				else
					newImage.setRGB(j, i+10,gradient.getGradientColor((float)i/(float)h).getRGB());
			}
			previousColor = nextColor;	
		}
		*/
		return newImage;
	}
	
	private BufferedImage createEmptyBorder(BufferedImage image, int borderWidth) {

		int h = image.getHeight()+borderWidth;
		int w = image.getWidth()+borderWidth;

		BufferedImage newImage = new BufferedImage(w,h,image.getType());

		Graphics2D g2d = newImage.createGraphics();        

		for (int i=0; i<h; i++) {
			for (int j=0; j<w; j++) {
				newImage.setRGB(j, i, BACKGROUND_COLOR.getRGB());	// Fill with white
			}
		}

		g2d.dispose();
		return newImage;
	}

	private BufferedImage appendBorder(BufferedImage image, BufferedImage border, int borderWidth) {
		BufferedImage newImage = new BufferedImage(border.getWidth(),border.getHeight(),border.getType());
		Graphics2D g2d = newImage.createGraphics(); 
		int neBorder = (int) Math.round(borderWidth*.4);		
		g2d.drawImage(border, 0, 0, null);	// Draw the border
		boolean renderBorder = renderAxis || renderTickMarks || renderMesh || 
				(annotationOption != Annotation.NONE && annotations != null);
		if(renderBorder)
			g2d.drawImage(image, borderWidth-neBorder-1, 1+neBorder, null);	// Draw the new image
		else
			g2d.drawImage(image, 0, 0, null);	
		return newImage;
	}

	private void addBorder(BufferedImage border, int borderWidth) {

		int h = border.getHeight();
		int w = border.getWidth();
		int neBorder = (int) Math.round(borderWidth*.4);

		Graphics2D g2d = border.createGraphics();        
		for (int i=0; i<h; i++) {
			for (int j=0; j<w; j++) {
				// Border
				boolean north = i == 0 && j >= borderWidth-2;
				boolean south = i == (h-borderWidth)+1 && j >= borderWidth-2;
				boolean east = j == (borderWidth-2) && i <= (h-borderWidth);
				boolean west = j == w-1 && i <= (h-borderWidth); 
				if(north || south || east || west)
					border.setRGB(j-neBorder, i+neBorder, TICK_COLOR.getRGB());		
			}
		}

		g2d.dispose();
	}

	private void addAxis(BufferedImage border, int borderWidth) {

		String xAxis = this.xAxis.toString();
		String yAxis = this.yAxis.toString();

		int h = border.getHeight();
		int w = border.getWidth();

		Graphics2D g2d = border.createGraphics();        
		for (int i=0; i<h; i++) {
			for (int j=0; j<w; j++) {

				boolean southAxis = i == h-9 && (j >= 9 && j <= 22);
				boolean eastAxis = j == 9 && (i <= h-9 && i >= h-22);
				if(southAxis || eastAxis)
					border.setRGB(j, i, GRID_LINE_COLOR.getRGB());
			}
		}

		// Set the color and font of the axis text and then draw it
		g2d.setPaint(FONT_COLOR);
		g2d.setFont(LABEL_FONT_BOLD); 
		g2d.drawString(xAxis, 29, h-6);
		g2d.drawString(yAxis,6, h-29);

		g2d.dispose();
	}

	private void addGridLines(BufferedImage border, int borderWidth) {

		int neBorder = (int) Math.round(borderWidth*.4);
		int h = border.getHeight();
		int w = border.getWidth();

		Graphics2D g2d = border.createGraphics(); 
		for (int i=0; i<h; i++) {
			for (int j=0; j<w; j++) {
				boolean inBounds = i >= 1 && i <= (h-borderWidth) && j >= (borderWidth-1) && j <= w-2;
				if(inBounds) {
					if(gridLines.get(xAxis).contains(j-borderWidth+1) || gridLines.get(yAxis).contains(i)) {
						border.setRGB(j-neBorder, i+neBorder, GRID_LINE_COLOR.getRGB());
					}
				}
			}
		}

		g2d.dispose();
	}

	private void addTickMarks(BufferedImage border, int borderWidth) {

		int neBorder = (int) Math.round(borderWidth*.4);
		int h = border.getHeight()-borderWidth;
		int w = border.getWidth()-borderWidth;

		// The lesser of either: 10 tick marks, or 1 tick every 9 pixels?
		int minWidthY = 9;
		int minWidthX = 38;

		int hDir = (9 < h/minWidthY) ? 9 : h/minWidthY;
		int wDir = (9 < w/minWidthX) ? 9 : w/minWidthX;

		int hSpacing = hDir == 0 ? minWidthY : Math.round(h/hDir);
		int wSpacing = wDir == 0 ? minWidthX: Math.round(w/wDir);

		Graphics2D g2d = border.createGraphics(); 

		Map<Point2i, Float> tickLabels = new HashMap<Point2i, Float>();

		for (int i=0; i<h+borderWidth; i++) {
			for (int j=0; j<w+borderWidth; j++) {
				// Border
				boolean south = (i >= h+1 && i <= h+3) && j >= borderWidth-2;
				boolean east = (j >= borderWidth-4 && j <= borderWidth-2) && i <= h+1;

				// 0 = neBorder+h+1
				if(east && !((i-h) % hSpacing == 0)) {
					east = false;
				}
				if(south && !((j - (borderWidth-1)) % wSpacing == 0)) {
					south = false;
				}

				if(south || east) {
					border.setRGB(j-neBorder, i+neBorder, TICK_COLOR.getRGB());	
				}

				if(south && i == h+3) {
					tickLabels.put(new Point2i(j, i+10+neBorder), getValue(min, xAxis)+(step*(j - (borderWidth-1))));
				}

				if(east && j == borderWidth-4) {
					tickLabels.put(new Point2i(j-4-neBorder, i+4+neBorder), getValue(min, yAxis)+(step*(h-i)));	
				}
			}
		}

		// Set the color and font of the axis text and then draw it
		g2d.setPaint(FONT_COLOR);
		g2d.setFont(TICK_FONT); 
		DecimalFormat exponentialFormat = new DecimalFormat("0.000E0");
		DecimalFormat floatFormat = new DecimalFormat("#.###");
		for(Point2i point: tickLabels.keySet()) {
			float value = tickLabels.get(point);
			String formatted = value < 0.001 || value > 1000 ? exponentialFormat.format(value) : floatFormat.format(value);
			g2d.drawString(formatted, point.getX()-5*formatted.length()+2, point.getY());
		}

		g2d.dispose();
	}

	private BufferedImage addAnnotation(BufferedImage image, AnnotatedPosition ap, int borderWidth) {

		// Fetch the 2d position of the annotation on the image
		Point2i pos = new Point2i(ap.getSlicePosition());
		
		boolean renderBorder = renderAxis || renderTickMarks || renderMesh || 
				(annotationOption != Annotation.NONE && annotations != null);
		
		int yOffset = !renderBorder ? 0 : (int) Math.round(borderWidth*.4);
		int xOffset = !renderBorder ? 0 : (int) Math.round(borderWidth*.6)-1;

		pos.x += xOffset;
		pos.y += yOffset;

		int additionalX = ap.getAnnotation().length()*5;
		if(pos.x + additionalX > image.getWidth()) {
			image = expandImage(image, additionalX, 0);
		}

		Graphics2D g2d = image.createGraphics();

		// Specifies the size of the annotation point to draw
		int pointWidth = 6;

		// Intersecting ponts will be drawn in green, others in red
		if (ap.getIntersectsSlice()) {
			g2d.setPaint(Color.GREEN);
		} else {
			g2d.setPaint(Color.RED);
		}
		g2d.fillOval(
				pos.x - pointWidth / 2,
				pos.y - pointWidth / 2,
				pointWidth, pointWidth
				);

		// Set the color width of the point outline and then draw it
		g2d.setStroke(new BasicStroke(1));
		g2d.setPaint(Color.BLACK);
		g2d.drawOval(
				pos.x - pointWidth / 2,
				pos.y - pointWidth / 2,
				pointWidth, pointWidth
				);

		// Set the color and font of the annotation text and then draw it
		g2d.setPaint(FONT_COLOR);
		g2d.setFont(LABEL_FONT); 
		g2d.drawString(
				ap.getAnnotation(),
				pos.x + pointWidth,
				pos.y - pointWidth
				);

		g2d.dispose();

		return image;
	}

	private BufferedImage expandImage(BufferedImage imageIn, int xExpand, int yExpand) {
		int h = imageIn.getHeight()+yExpand;
		int w = imageIn.getWidth()+xExpand;

		BufferedImage newImage = new BufferedImage(w,h,imageIn.getType());

		Graphics2D g2d = newImage.createGraphics();        

		for (int i=0; i<h; i++) {
			for (int j=0; j<w; j++) {
				newImage.setRGB(j, i, BACKGROUND_COLOR.getRGB());	// Fill with white
			}
		}
		g2d.drawImage(imageIn, 0, 0, null);	// This will move the image down...
		g2d.dispose();
		return newImage;		
	}

	private float getValue(Vector3f vector, AXIS axis) {
		if(axis.equals(AXIS.X))
			return vector.x;
		if(axis.equals(AXIS.Y))
			return vector.y;
		if(axis.equals(AXIS.Z))
			return vector.z;
		return 0.0f;
	}


	private Integer getValue(Point3i point, AXIS axis) {
		if(axis.equals(AXIS.X))
			return point.x;
		if(axis.equals(AXIS.Y))
			return point.y;
		if(axis.equals(AXIS.Z))
			return point.z;
		return 0;
	}

	private void setValue(Point3i point, AXIS axis, int value) {
		if(axis.equals(AXIS.X))
			point.x = value;
		if(axis.equals(AXIS.Y))
			point.y = value;
		if(axis.equals(AXIS.Z))
			point.z = value;
	}

	public boolean getRenderAxis() {
		return renderAxis;
	}
	
	public boolean getRenderMesh() {
		return renderMesh;
	}
	
	public boolean getRenderTickMarks() {
		return renderTickMarks;
	}

	public List<AnnotatedPosition> getAnnotations() {
		List<AnnotatedPosition> annotations = new ArrayList<AnnotatedPosition>();
		for(AnnotatedPosition annotation: annotations) {
			annotations.add(annotation);
		}
		return annotations;
	}

	public Annotation getAnnotationOption() {
		return annotationOption;
	}
	
}
