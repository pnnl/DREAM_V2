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

public class RunPyScript {
	
	private static final String SCRIPT_NAME = "gravgrad.py";
	
	private boolean printToConsole = true;
	
	public void calculateGravity(final String[] pyScriptArgs) throws IOException {
		runGravityCalculationScript(pyScriptArgs);
	}
	
	
	private void runGravityCalculationScript(final String[] args) throws IOException {
		try {
	        InputStream in = ResultPrinter.class.getResourceAsStream("/"+ SCRIPT_NAME);
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
		StringBuilder command = new StringBuilder();
		//TODO: Change the script so that it runs on python 3.6+ instead of python 2.7
		command.append("py -3 \"" + script + "\"");
		for(String arg: args) {
			command.append(" \"" + arg + "\"");
		}
		try {
			Process p = Runtime.getRuntime().exec(command.toString());
			if(printToConsole) {
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
		} catch(Exception e) {
			System.out.println("Install python version 3.6 or higher to gravity calculation.");
			e.printStackTrace();
		}
	}

}
