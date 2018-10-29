package gov.nih.nlm.ling.sem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This class attempts to capture domain-specific semantic information as a static set of 
 * key/value pairs from a <code>Properties</code> object. Values are list of strings, separated by semicolon.<p> 
 * Domain properties are expected to prefixed with <i>domain.</i> prefix. At the moment, keys can be 
 * two-level deep, each level separated by a period. An example of a single level domain 
 * property is <i>domain.semtypes</i>, an example of a two level domain property is  
 * <i>domain.semtype.DISORDER</i>. If more levels are needed, this class can be expanded.
 * 
 * @author Halil Kilicoglu
 *
 */
public class DomainProperties {
	
	private static Properties domainProperties = null;
	private static final String SEPARATOR = ";";
	
	/**
	 * Initialize the domain properties.
	 * @param props the domain properties to initialize
	 */
	public static void init(Properties props) {
		for (Object o : props.keySet()) {
			String str = (String)o;
			if (str.startsWith("domain.")) {
				if (domainProperties == null) domainProperties = new Properties();
				String key = str.substring(7);
				String value = props.getProperty(str);
				domainProperties.put(key, value);
			}
		}
	}
	
	/**
	 * 
	 * @return  all domain properties
	 */
	public static Properties getDomainProperties() {
		return domainProperties;
	}
	
	/**
	 * Gets the domain properties indicated with the key.
	 * The key is expected to be a single-level key.
	 * 
	 * @param key  the single-level key 
	 * @return  the list of all properties with the given key.
	 */
	public static List<String> parse1(String key) {
		List<String> vals = new ArrayList<>();
		if (domainProperties == null) return vals;
		String value = domainProperties.getProperty(key);
		if (value == null) return vals;
		return Arrays.asList(value.split(SEPARATOR));
	}
	
	/**
	 * Gets the domain properties indicated with the key.
	 * The key is expected to be a two-level key.
	 * 
	 * @param key  the two-level key 
	 * @return  the list of all properties with the given key.
	 */
	public static Map<String,List<String>> parse2(String key) {
		Map<String,List<String>> out = new HashMap<>();
		if (domainProperties == null) return out;
		for (Object o: domainProperties.keySet()) {
			String str = (String)o;
			if (str.startsWith(key)) {
				String key1 = str.substring(key.length()+1);
				List<String> vals = Arrays.asList(domainProperties.getProperty(str).split(SEPARATOR));
				out.put(key1, vals);
			}
		}
		return out;
	}
	
	/**
	 * Searches for the key in a map which has a given string as one of its values. Utility
	 * to deal with searching domain properties with two-level keys.
	 * 
	 * @param map  the domain properties map
	 * @param str  the value 
	 * @return  the key associated with the value
	 */
	public static String valueKey(Map<String,List<String>> map, String str) {
		for (String k: map.keySet()) {
			List<String> val = map.get(k);
			if (val.contains(str)) return k;
		}
		return null;
	}

}
