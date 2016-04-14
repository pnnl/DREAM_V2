package utilities;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class SaveLoad {

	private final static Map<Class, String> ALIASES = new HashMap<Class, String>() {
		private static final long serialVersionUID = 1L;
		{
			// TODO: Luke - if we want to remove the object. we need to alias the classes
			//put(Material.class, "ObjectAlias");
			//put(Key.class, "key");
			//put(DataItem.class, "data");
		}
	};
	
	
	public static void Save(Object toSave, File file) throws IOException {

		String aliased = getAliasedXStream().toXML(toSave);
		Files.write(file.toPath(), aliased.getBytes(), new OpenOption[] { StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING });
		
	}
	
	public static Object Load(File file) throws IOException {
		
		byte[] data = Files.readAllBytes(file.toPath());
		return getAliasedXStream().fromXML(new String(data, Charset.defaultCharset()));
		
		
	}
	
	public static XStream getAliasedXStream() {
		XStream xStream = new XStream(new DomDriver());

		// xStream.setMode(XStream.NO_REFERENCES);
		for (Class aliasable : ALIASES.keySet())
			xStream.alias(ALIASES.get(aliasable), aliasable);
		
		return xStream;
	}

}
