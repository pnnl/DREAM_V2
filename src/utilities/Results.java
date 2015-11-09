package utilities;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

public class Results {

		
	private Map<Double, List<String>> myDetailedResults;
	public HashMap<Double, Integer> myHistoResults;
	
	public Results( Map<Double, List<String>> resultMap)
	{
		myDetailedResults = resultMap;
	}

	
	public void BuildHistoData()	
	{	
		List<String> histoFileResults = new ArrayList<String>();
		myHistoResults = new HashMap<Double, Integer>();
		Iterator<Entry<Double, List<String>>> myIterator = myDetailedResults.entrySet().iterator();
		while (myIterator.hasNext())
		{
			Entry<Double, List<String>> thisEntry = myIterator.next();
			Double key = thisEntry.getKey();			
			int occurences = thisEntry.getValue().size();			
			myHistoResults.put(key, occurences);
			System.out.println("E[FTD] = " + key + ", Occurences = " + occurences);	
			histoFileResults.add(key + "," + occurences);
		}
		
			try {
				String timeStamp = new SimpleDateFormat("yyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
				FileUtils.writeLines(new File("C:\\Users\\d3x078\\Desktop\\Results\\Histo_Results_"+ timeStamp + ".txt"), histoFileResults);
				} 
			catch (IOException e) 
			{
				System.out.println("You threw an IOException.");
				e.printStackTrace();
			}
		
		
		
	}
	
	public void PostProcessFiles()
	{
		
	}

}
