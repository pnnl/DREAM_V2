package gridviz;

import gridviz.DataGrid.AXIS;
import gridviz.DataSlice.Annotation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

/**
 * @brief  Provides a basic API for the DataGrid and GridParser
 * @author Tucker Beck
 * @date   3/7/12
 */
public class GridViz {

	private GridParser parser;
	private DataGrid   grid;



	/// Performs a simple example of slicing along an axis
	public static void main(String[] args) {

		try {
			for(String plotFile : new String[]{"plot.281", "plot.185", "plot.52", "plot.84", "plot.182", "plot.29", "2dplot.645", "cylplot2d.65", "cylplot3d.65", "plot.0",
					 "plot.2861"}) {
				String fileName = "plots/" + plotFile;

				GridViz tester = new GridViz();
				tester.loadData(fileName);

				System.out.format("Available data fields:\n");
				List<String> availableFieldNames = new ArrayList<String>();

				for(String name: tester.grid.getFieldNames()) {
					if(name.contains("Mass"))
						continue;
					if(name.contains("Volume"))
						continue;
					if(name.contains("Rock"))
						continue;
					if(name.contains("Node"))
						continue;
					availableFieldNames.add(name);
				}

				for(String name: availableFieldNames)
					System.out.format("  | %s\n", name);

				Point3i dim = tester.grid.getSize();
				System.out.format("Dimensions of Gird: <%d, %d, %d>\n",
						dim.x, dim.y, dim.z);


				System.out.format("Fetching centroid of data volume...\n");
				Vector3f lo = new Vector3f();
				Vector3f hi = new Vector3f();
				tester.grid.getExtents(lo, hi);
				
				Vector3f[][] quadrants = new Vector3f[3][2];
	
				// Full image
				quadrants[0][0] = lo;
				quadrants[0][1] = hi;
				
				// Lower left hand corner
				quadrants[1][0] = new Vector3f(lo.x, lo.y, lo.z);
				quadrants[1][1] = new Vector3f((hi.x-lo.x)/2+lo.x, (hi.y-lo.y)/2+lo.y, (hi.z-lo.z)/2+lo.z);
				
				// Upper right hand corner
				quadrants[2][0] = new Vector3f((hi.x-lo.x)/2+lo.x, (hi.y-lo.y)/2+lo.y, (hi.z-lo.z)/2+lo.z);
				quadrants[2][1] = new Vector3f(hi.x, hi.y, hi.z);
				
				String[] quadrantLabels = new String[]{"All", "SW", "NE"};//, "SE", "NW", "NE"};
				
				Vector3f centroid = new Vector3f(
						lo.x + (hi.x - lo.x)/2,
						lo.y + (hi.y - lo.y)/2,
						lo.z + (hi.z - lo.z)/2);

				Point3i max = tester.grid.getSize();
				System.out.println(max.toString());
				Point3i maxPt = tester.grid.hasOrigin() ? 
						new Point3i(max.getX(), max.getY(), max.getZ()) :								
						new Point3i(max.getX()-1, max.getY()-1, max.getZ()-1);
				Point3i minPt = new Point3i(1,1,1);
				Point3i centerPt = new Point3i(
						(maxPt.getX()-minPt.getX())/2+minPt.getX(), 
						(maxPt.getY()-minPt.getY())/2+minPt.getY(), 
						(maxPt.getZ()-minPt.getZ())/2+minPt.getZ());

				// Add some annotation points
				List<AnnotatedPosition> aps = new ArrayList<AnnotatedPosition> ();
				aps.add(new AnnotatedPosition(maxPt, maxPt.toString()));
				aps.add(new AnnotatedPosition(minPt, minPt.toString()));
				aps.add(new AnnotatedPosition(centerPt, centerPt.toString()));

				List<AXIS> axis = new ArrayList<AXIS>();
				for(AXIS ax: AXIS.values()) {
					axis.add(ax);
				}
				if(tester.grid.is2D()) {
					axis.remove(tester.grid.getNormalAxis());
				}
				// Get some options to test
				//List<OptionSet> options = tester.getListOfOptions(6);        

				File directory = new File("./output/" + plotFile);
				if(!directory.exists())
					directory.mkdir();

				for(String fieldName: availableFieldNames) {
					String fieldNm = ((String)fieldName).replaceAll("[ ,/^\\.]", "_");
					File fieldDir = new File(directory, fieldNm);
					if(!fieldDir.exists())
						fieldDir.mkdir();            	
					for(AXIS xAxis: axis) {
						for(AXIS yAxis: axis) {
							if(xAxis.equals(yAxis)) 
								continue;
							float center = 0; 
							if(!xAxis.equals(AXIS.X) && !yAxis.equals(AXIS.X))
								center = centroid.x;
							if(!xAxis.equals(AXIS.Y) && !yAxis.equals(AXIS.Y))
								center = centroid.y;
							if(!xAxis.equals(AXIS.Z) && !yAxis.equals(AXIS.Z))
								center = centroid.z;	
							int q = 0;
					//		for(int q= 0; q < quadrants.length; q++) {
								Vector3f myLo = quadrants[q][0];
								Vector3f myHi = quadrants[q][1];								
							//	System.out.println("Starting slice for : " + xAxis + "" + yAxis + "" + quadrantLabels[q] + " slice, middle of other axis = " + center);
						
								DataSlice beforeCopy = tester.grid.slice(xAxis, yAxis, center, myLo, myHi, fieldName, 256);
								beforeCopy.setAnnotations(aps);
								OptionSet set = new OptionSet(Annotation.ALL, false, true, true, false, 0,0);
							//	for(OptionSet set: options) {   
									File imageOut = set.getFile(fieldDir, xAxis.toString(), yAxis.toString() + "_" + quadrantLabels[q]);
									if(imageOut == null)
										continue; // Already exists
									DataSlice slice = beforeCopy.makeCopy();
									set.applyOptions(slice);   
									//BufferedImage image = set.generateImage(slice);
									List<Float> gradient = slice.getGradientValues(.005f);										
									BufferedImage image = slice.renderBanded(0, gradient);
									if (ImageIO.write(image, "png", imageOut) == false) {
										imageOut.delete();
										System.out.println("Couldn't write slice image to file");
									}								
							//	}		
									return;
							//}
						}
					}
				}

				System.out.format("Done...\n");
			}
		} catch (GridError err) {
			System.out.println(err.getMessage());
		} catch (Exception err) {
			System.out.println(err.getMessage());
		}
	}

	/*private List<OptionSet> getListOfOptions(int numOptions) {
		List<OptionSet> options = new ArrayList<OptionSet>();

		for(int color: new int[]{0, 1, 2}) {
			for(int scale: new int[]{0, 1}) {
				for(boolean showAxis: new boolean[]{true, false}) {
					for(boolean tick: new boolean[]{true, false}) {
						for(boolean globalExtrema: new boolean[]{true, false}) {
							for(boolean mesh: new boolean[]{true, false}) {
								for(Annotation annotation: new Annotation[]{Annotation.ALL, Annotation.NONE}){//Annotation.values()) {
									options.add(new OptionSet(annotation, mesh, tick, showAxis, globalExtrema, scale, color));	
									if(options.size() == numOptions)
										return options;
								}
							}
						}
					}
				}
			}
		}
		return options;
	}*/

	/// Loads grid data from a file
	public void loadData(String fileName) throws GridError {
		System.out.format("Parsing data file: %s\n", fileName);
		parser = new GridParser( fileName );
		grid = parser.extractStompData();
		System.out.format("Data loaded successfully.\n");
	}

	public void saveRawDataAscii(String fieldName, String outputPath) {
		try {
			grid.dumpDataAscii(outputPath, fieldName);
		} catch (GridError e) {
			e.printStackTrace();
		}    	
	}

	public void saveRawData(String fieldName, String outputPath) {
		try {
			grid.dumpData(outputPath, fieldName);
		} catch (GridError e) {
			e.printStackTrace();
		}
	}
}
