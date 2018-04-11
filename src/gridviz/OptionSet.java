package gridviz;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import gridviz.DataSlice.Annotation;

public class OptionSet {

	Annotation annotationOption;
	boolean mesh;
	boolean tick;
	boolean axis;
	boolean globalExtrema;
	int scale;
	int color;

	public OptionSet(Annotation annotationOption,
			boolean mesh,
			boolean tick,
			boolean axis,
			boolean globalExtrema,
			int scale,
			int color) {

		this.annotationOption = annotationOption;
		this.mesh = mesh;
		this.tick = tick;
		this.axis = axis;
		this.globalExtrema = globalExtrema;
		this.scale = scale;
		this.color = color;
	}
	
	public void applyOptions(DataSlice slice) {
		slice.setAnnotationOption(annotationOption);
		slice.setRenderMesh(mesh);
		slice.setRenderTickMarks(tick);
		slice.setRenderAxis(axis);
		slice.setUseGlobalExtrema(globalExtrema);
	}
	
	public BufferedImage generateImage(DataSlice slice) {
		BufferedImage image = null;

		if(color == 0)
			image = slice.renderGradient(0);
		return image;
	}
	
	public File getFile(File directory, String xAxis, String yAxis) throws IOException {
		File colorDir = new File(directory, getColor());
		if(!colorDir.exists())
			colorDir.mkdir();
		
		File scaleDir = new File(colorDir, getScale());
		if(!scaleDir.exists())
			scaleDir.mkdir();
		
		File imageOut = new File(scaleDir, xAxis + "" + yAxis + "_" + toString() + ".png");
		if(!imageOut.exists())
			imageOut.createNewFile();
		else 
			return null; // Don't make another one.
		
		return imageOut;
	}

	private String getColor() {
		StringBuilder options = new StringBuilder();
		if(color == 0) {
			options.append("Gradient");
		} else if(color == 1) {
			options.append("Blue-Red");
		} else {
			options.append("Grayscale");
		}
		
		return options.toString();
	}
	
	private String getScale() {
		StringBuilder options = new StringBuilder();
		if(scale == 0) {
			options.append("lnScale");
		} else {
			options.append("log10Scale");
		}
		return options.toString();
	}
	
	@Override 
	public String toString() {
		StringBuilder options = new StringBuilder();
		if(annotationOption.equals(Annotation.ALL)) {
			options.append("(Annotations - All)");
		} else if(annotationOption.equals(Annotation.INTERSECT)) {
			options.append("(Annotations - Intersecting)");
		} else {
			options.append("(Annotations - None)");
		}
		options.append("_(Mesh - "+mesh+")");
		options.append("_(Ticks - "+tick+")");
		options.append("_(Axis - "+axis+")");
		options.append("_(Global Scale - "+globalExtrema+")");
		
		return options.toString();
	}
}