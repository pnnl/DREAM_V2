package wizardPages;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import utilities.Constants;

/**
 * Need to add VM arguments that identify the jhdf5 library
 * Using a runtime argument to re-launch the main program with arguments
 * @author whit162
 * @author huan482
 * Source: http://blog.codejava.net/nam/trick-for-passing-vm-options-when-launching-jar-file/
 */

public class LaunchDREAM extends DREAMWizard {
	
	public static void main(String[] args) {
		String nameOfFile = new java.io.File(LaunchDREAM.class.getProtectionDomain().
				getCodeSource().getLocation().getPath()).getName();
        if (args.length == 0) {
        	try {
        		// We are creating a copy of jhdf5.dll in the user's temp directory
        		// This is because the jar compresses internal files so that we can't point directly at it
    	        InputStream in = LaunchDREAM.class.getResourceAsStream("/jhdf5.dll");
    	        File fileOut = new File(System.getProperty("java.io.tmpdir"),"jhdf5.dll"); //Within the user's temp directory
    	        OutputStream out = FileUtils.openOutputStream(fileOut);
    	        IOUtils.copy(in, out);
    	        in.close();
    	        out.close();
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
            try {
                // re-launch the program itself with VM option passed
                Runtime.getRuntime().exec(new String[] {"java", "-Dncsa.hdf.hdf5lib.H5.hdf5lib="+ 
                System.getProperty("java.io.tmpdir")+"/jhdf5.dll", "-jar", nameOfFile, "test"});
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            System.exit(0);
        }
        
        // Run the main program with the VM option set
        Constants.runningJar = true;
        DREAMWizard.main(args);
    }
}