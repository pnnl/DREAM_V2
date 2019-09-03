package gravity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import results.ResultPrinter;

/**
 * Runs gravity python script.
 * 
 * @author huan482
 *
 */
public class RunPyScript {
	/**Name of the gravity script we're running. */
	private static final String SCRIPT_NAME = "gravgrad.py";
	
	private boolean printToConsole = true;
		
	private int myCounter = 0;
	
	//Loop through all the files given through
	public void calculateGravity(final File[] allFiles) throws IOException {
		for (File f : allFiles) {
			myCounter++;
			runGravityCalculationScript(f.getAbsolutePath());
		}
	}
	/**
	 * Execute the the gravity calculation by getting the .in file and outputting the .fwd out.
	 * @param fileName - The absolute path of the .in file inputted.
	 * @throws IOException
	 */
	private void runGravityCalculationScript(final String fileName) throws IOException {
		try {
			InputStream in = ResultPrinter.class.getResourceAsStream("/" + SCRIPT_NAME);
			File fileOut = new File(System.getProperty("java.io.tmpdir"), SCRIPT_NAME);
			OutputStream out = FileUtils.openOutputStream(fileOut);
			IOUtils.copy(in, out);
			in.close();
			out.close();
		} catch (Exception theException) {
			System.out.println("Unable to copy the script to the temp directory");
			theException.printStackTrace();
		}
		String script = System.getProperty("java.io.tmpdir") + File.separator + SCRIPT_NAME;
		//The String our script is represented.
		StringBuilder command = new StringBuilder();
		command.append("py -3 \"" + script + "\"");
		command.append(" \"" + fileName + "\"" + " " + myCounter);
		System.out.println(command.toString());
		try {
			Process p = Runtime.getRuntime().exec(command.toString());
			if (printToConsole) {
				BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
				BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String line;
				while ((line = bri.readLine()) != null)
					System.out.println(line);
				bri.close();
				while ((line = bre.readLine()) != null)
					System.out.println(line);
				bre.close();
			}
		} catch (Exception e) {
			System.out.println("Install python version 3.6 or higher to gravity calculation.");
			e.printStackTrace();
		}
	}

}
