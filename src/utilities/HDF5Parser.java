package utilities;

import gridviz.DataGrid;
import gridviz.GridParser;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import org.apache.commons.io.FileUtils;

import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.object.Dataset;
import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.Group;
import ncsa.hdf.object.h5.*; 

/**
 * Code to properly and efficiently process hdf5 files.
 * For now, we read from the files and then close them.
 * @author port091
 */

public class HDF5Parser {


	public static boolean debug = false;

	public static void main(String[] args) throws Exception {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}  
		//	readFromDataset();
	//	readFromDatasetFaster();
		//readFromDatasetLeavingFilesOpen();
			new HDF5Parser();
	}

	public HDF5Parser() throws Exception { 

		// Open a folder
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File("N:\\STOMP Data"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnValue = chooser.showOpenDialog(null);
		if(returnValue == JFileChooser.APPROVE_OPTION) {

			File selection = chooser.getSelectedFile();
			File hdf5FileFolder = new File(selection.getAbsolutePath() + "_hdf5");
			hdf5FileFolder.mkdir();
			if(selection != null && selection.isDirectory()) {
				System.out.println("Loading directory: " + selection);
				Date startTime = Calendar.getInstance().getTime();
				// Each sub folder will be a new set, and a new hdf5 file
				for(File subFolder: selection.listFiles()) {
					File hdf5FileLocation = new File(hdf5FileFolder, subFolder.getName() +  ".h5");
					FileFormat hdf5Format = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
					H5File hdf5File  = (H5File)hdf5Format.createFile(hdf5FileLocation.getName(), FileFormat.FILE_CREATE_DELETE);
					hdf5File.open();
					boolean firstFile = true;
					if(debug)
						System.out.println("\t\t\t\tCreated hdf5 file - putting data");

					if(subFolder.isDirectory()) {
						// We are parsing a new set
						if(debug)
							System.out.println("\tProcessing set: " + subFolder.getName());
						// Look for the plot files
						for(File plotFile: subFolder.listFiles()) {
							if(plotFile.getName().contains("plot")) {
								if(debug)
									System.out.println("\t\tProcessing plot file: " + plotFile.getName());
								GridParser parser = new GridParser(plotFile.getAbsolutePath());
								addDataToSet(parser, hdf5File, plotFile.getName().replaceAll("\\.", ""), firstFile);
								firstFile = false;
							}
						}
					}
					if(debug)
						printData(hdf5File);
					if(debug)
						System.out.println("Writing the file to disk:");
					FileUtils.copyFile(hdf5File, hdf5FileLocation);
					if(debug)
						System.out.println("Done");
					hdf5File.close(); // Close the file
				}

				// Check time
				long duration = Calendar.getInstance().getTime().getTime()-startTime.getTime();
				System.out.println("Took: " + (((double)duration/1000)/60) + " min");
			}
		}

	}



	private void addDataToSet(GridParser parser, H5File hdf5File, String plotFileName, boolean addGridInfo) throws Exception {

		// Sending the print statements from the grid jar to a dummy output stream to keep them off the console
		PrintStream originalStream = System.out;
		if(!debug) {
			PrintStream dummyStream    = new PrintStream(new OutputStream(){
				public void write(int b) { }
			});
			System.setOut(dummyStream);
		}
		DataGrid grid = parser.extractData();
		// Restoring the console print stream
		System.setOut(originalStream);

		// Get the root
		Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();
		Datatype dtype = hdf5File.createDatatype(Datatype.CLASS_FLOAT, 4, Datatype.NATIVE, -1);

		if(addGridInfo) {
			Group gridGroup = hdf5File.createGroup("grid", root);

			hdf5File.createScalarDS("x", gridGroup, dtype, new long[]{grid.getSize().getX()}, null, null, 0, grid.getFieldValues("x").getValues());
			hdf5File.createScalarDS("y", gridGroup, dtype, new long[]{grid.getSize().getY()}, null, null, 0, grid.getFieldValues("y").getValues());
			hdf5File.createScalarDS("z", gridGroup, dtype, new long[]{grid.getSize().getZ()}, null, null, 0, grid.getFieldValues("z").getValues());			
		}

		// Create a group for each time in the set
		Group timeStepGroup = hdf5File.createGroup(plotFileName, root);

		// Get the dimensions of the grid
		long[] dims3D = {grid.getSize().getX(), grid.getSize().getY(), grid.getSize().getZ()};
		for(String field : grid.getFieldNames()) {
			if(field.equals("x") || field.equals("y") || field.equals("z"))
				continue;
			// Replacing strange characters in the field name
			String fieldClean = field.split(",")[0].replaceAll("\\+", "p").replaceAll("\\-", "n").replaceAll("\\(", "_").replaceAll("\\)", "");
			if(debug)
				System.out.print("\t\t\tAdding field: " + fieldClean + "...");	
			try {
				hdf5File.createScalarDS(fieldClean, timeStepGroup, dtype, dims3D, null, null, 0, grid.getFieldValues(field).getValues());
				if(debug)
					System.out.println("SUCCESS");
			} catch(Exception e) {
				if(debug)
					System.out.println("FAILED");
			}

		}
		if(debug)
			System.out.println("\t\tDone loading plot file: plotFileName");
	}




	private static void readFromDatasetLeavingFilesOpen() throws Exception {


		H5File realization26  = new H5File("C:\\Users\\port091\\Desktop\\CCUS Case_hdf52\\realization_26.h5", HDF5Constants.H5F_ACC_RDONLY);
		H5File realization27  = new H5File("C:\\Users\\port091\\Desktop\\CCUS Case_hdf52\\realization_27.h5", HDF5Constants.H5F_ACC_RDONLY);
		H5File realization32  = new H5File("C:\\Users\\port091\\Desktop\\CCUS Case_hdf52\\realization_32.h5", HDF5Constants.H5F_ACC_RDONLY);
		H5File realization35  = new H5File("C:\\Users\\port091\\Desktop\\CCUS Case_hdf52\\realization_35.h5", HDF5Constants.H5F_ACC_RDONLY);
		realization26.open();
		realization27.open();
		realization32.open();
		realization35.open();

		File resultsFile = new File("C:\\Users\\port091\\Desktop\\Results\\openFiles.txt");
		resultsFile.createNewFile();


		for(int totalNodes: new int[]{5, 10, 20, 50, 100, 150, 200}) {
			for(int totalTimes : new int[]{5,10,15,20}) {

				int totalQueries = 0;
				Date startTime = Calendar.getInstance().getTime();	
				for(H5File hdf5File: new H5File[]{realization26, realization27, realization32, realization35}) {
					for(int sensorType: new int[]{3,4}) {	// Will need the index
						for(int time = 1; time < totalTimes+1; time++) {
							for(int nodeNumber = 0; nodeNumber < totalNodes; nodeNumber++) {

								// Get the root node:
								Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();

								// Get the timestep
								Object group =  root.getMemberList().get(time);

								// Get the dataset
								Object child = ((Group)group).getMemberList().get(sensorType);	// Might be able to speed this up with an index

								// Get the data at the node
								Dataset dataset = (Dataset)child;
								float[] dataRead = (float[])dataset.read();
								float value = dataRead[nodeNumber];								

								totalQueries++;


							}
						}
					}
				}

				// Check time
				long duration = Calendar.getInstance().getTime().getTime()-startTime.getTime();
				String asString = String.valueOf((((double)duration/1000)/60));
				String[] split = asString.split("\\.");
				String minutes = split[0];
				String seconds = split[1];
				int mins = Integer.parseInt(minutes);
				int secs = (int) Math.round(Double.parseDouble("0." + seconds)*60);
				List<String> lines = FileUtils.readLines(resultsFile);
				lines.add("Queries: " + totalQueries + "\t" + mins + " minutes " + secs + " seconds\tTime:" + asString);
				FileUtils.writeLines(resultsFile, lines);

			}
		}

		realization26.close();
		realization27.close();
		realization32.close();
		realization35.close();


	}
	

	private static void readFromDatasetFaster() throws Exception {


		File resultsFile = new File("C:\\Users\\port091\\Desktop\\Results\\directIndexing.txt");
		resultsFile.createNewFile();

		for(int totalNodes: new int[]{5, 10, 20, 50, 100, 150, 200}) {
			for(int totalTimes : new int[]{5,10,15,20}) {

				int totalQueries = 0;
				Date startTime = Calendar.getInstance().getTime();	
				for(String scenario: new String[]{"realization_26", "realization_27", "realization_32", "realization_35"}) {
					for(int sensorType: new int[]{0,1}) {	// Will need the index
						for(int time = 1; time < totalTimes+1; time++) {
							for(int nodeNumber = 0; nodeNumber < totalNodes; nodeNumber++) {

								// Open the file							
								H5File hdf5File  = new H5File("C:\\Users\\port091\\Desktop\\CCUS Case_hdf53\\" + scenario +  ".h5", HDF5Constants.H5F_ACC_RDONLY);
								hdf5File.open();

								// Get the root node:
								Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();

								// Get the timestep
								Object group =  root.getMemberList().get(time);

								// Get the dataset
								Object child = ((Group)group).getMemberList().get(sensorType);	// Might be able to speed this up with an index

								// Get the data at the node
								Dataset dataset = (Dataset)child;
								float[] dataRead = (float[])dataset.read();
								float value = dataRead[nodeNumber];								

								hdf5File.close(); // Close the file
								totalQueries++;

							}
						}
					}
				}

				// Check time
				long duration = Calendar.getInstance().getTime().getTime()-startTime.getTime();
				String asString = String.valueOf((((double)duration/1000)/60));
				String[] split = asString.split("\\.");
				String minutes = split[0];
				String seconds = split[1];
				int mins = Integer.parseInt(minutes);
				int secs = (int) Math.round(Double.parseDouble("0." + seconds)*60);

				List<String> lines = FileUtils.readLines(resultsFile);
				lines.add("Queries: " + totalQueries + "\t" + mins + " minutes " + secs + " seconds");
				FileUtils.writeLines(resultsFile, lines);


			}
		}


	}

	private static void readFromDataset() throws Exception {


		File resultsFile = new File("C:\\Users\\port091\\Desktop\\Results\\searching.txt");
		resultsFile.createNewFile();

		for(int totalNodes: new int[]{5, 10, 20, 50, 100, 150, 200}) {
			for(int totalTimes : new int[]{5,10,15,20}) {

				int totalQueries = 0;
				Date startTime = Calendar.getInstance().getTime();	
				for(String scenario: new String[]{"realization_26", "realization_27", "realization_32", "realization_35"}) {
					for(String sensorType: new String[]{"Gas Saturation", "Gas Pressure"}) {
						int timer = 0;
						for(String time: new String[]{"plot10250", "plot10780", "plot1240", "plot125", "plot1770", "plot2300", "plot2830", "plot3360", "plot3890", "plot4420", "plot4950", "plot5480", "plot6010", "plot6540", "plot7070", "plot710", "plot7600", "plot8130", "plot8660", "plot9190", "plot9720"}){
							if(timer == totalTimes)
								break;
							for(int nodeNumber = 0; nodeNumber < totalNodes; nodeNumber++) {

								// Open the file							
								H5File hdf5File  = new H5File("C:\\Users\\port091\\Desktop\\CCUS Case_hdf5\\" + scenario +  ".h5", HDF5Constants.H5F_ACC_RDONLY);
								hdf5File.open();

								// Get the root node:
								Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();

								boolean foundIt = false;
								// Search for the time step
								for(Object group: root.getMemberList()) {
									if(group.toString().equals(time)) {										
										if(group instanceof Group) {
											// Get the dataset
											for(Object child: ((Group)group).getMemberList()) { 	// Might be able to speed this up with an index
												if(child.toString().equals(sensorType)) {
													// Get the data at the node
													Dataset dataset = (Dataset)child;
													float[] dataRead = (float[])dataset.read();
													float value = dataRead[nodeNumber];								
													foundIt = true;
													break;
												}
											}
										} else {
											throw new Exception("No group at " + time);
										}
										break;
									}
								}

								if(!foundIt)
									throw new Exception ("Had a problem finding data for: " + scenario + ", " + sensorType + ", " + time + ", " + nodeNumber);

								hdf5File.close(); // Close the file
								totalQueries++;

							}
							timer++;
						}
					}
				}

				// Check time
				long duration = Calendar.getInstance().getTime().getTime()-startTime.getTime();
				String asString = String.valueOf((((double)duration/1000)/60));
				String[] split = asString.split("\\.");
				String minutes = split[0];
				String seconds = split[1];
				int mins = Integer.parseInt(minutes);
				int secs = (int) Math.round(Double.parseDouble("0." + seconds)*60);

				List<String> lines = FileUtils.readLines(resultsFile);
				lines.add("Queries: " + totalQueries + "\t" + mins + " minutes " + secs + " seconds");
				FileUtils.writeLines(resultsFile, lines);		
			}
		}


	}


	private void printData(H5File hdf5File) throws Exception {
		// Get the root
		System.out.println("Printing File: " + hdf5File.getName());
		Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)hdf5File.getRootNode()).getUserObject();		
		for(Object obj: root.getMemberList()) {
			System.out.println("\tGroup: " + obj);
			if(obj instanceof Group) {
				for(Object child: ((Group)obj).getMemberList()) {
					System.out.println("\t\tDataset: " + child);
					if(child instanceof Dataset) {
						Dataset dataset = (Dataset)child;
						float[] dataRead = (float[])dataset.read();
						System.out.println("\t\t\tValues: " + (Arrays.toString(dataRead).length() > 50 ? 
								Arrays.toString(dataRead).substring(0, 50) + "..." : Arrays.toString(dataRead))); // Only showing first 50 characters
					}
				}
			}
		}
	}

}


