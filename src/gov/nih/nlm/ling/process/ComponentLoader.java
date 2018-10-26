package gov.nih.nlm.ling.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A class to instantiate processing components as specified with a <code>Properties</code> object.
 * It contains static methods to initialize various components, such as term annotators, spell correction
 * components, sentence segmenters, etc., from properties. 
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO Consider making this class more extensible.
public class ComponentLoader {
	private static Logger log = Logger.getLogger(ComponentLoader.class.getName());	
	
	/**
	 * Instantiates a list of term annotators from properties.
	 * It assumes the class names of the annotators are specified in a key/value pair, with the key
	 * 'termAnnotators' and the fully-qualified class names delimited with the semicolon(;) in the value.
	 * The classes specified should implement {@link TermAnnotator} interface.
	 *  
	 * @param props	the properties to read from
	 * @return		a list of term annotator instances
	 * @throws		ClassNotFoundException	if a term annotator class cannot be found
	 * @throws		IllegalAccessException	if there is a problem with accessing a term annotator
	 * @throws		InstantiationException	if there is a problem with instantiating a term annotator
	 */
	public static List<TermAnnotator> getTermAnnotators(Properties props) 
				throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		List<Object> objs = getComponents(props,"termAnnotators",";");
		List<TermAnnotator> annotators = new ArrayList<TermAnnotator>();
		for (Object o: objs) {
			if (o instanceof TermAnnotator) annotators.add((TermAnnotator)o);
		}
		if (annotators.size() == 0) {	
			throw new RuntimeException("No term annotator cannot be initialized: " + props.get("termAnnotators"));
		}
		return annotators;
	}
	
	/**
	 * Instantiates a spelling correction module from properties.
	 * It assumes the class name is specified in a key/value pair, with the key
	 * 'spellCorrection' and the fully-qualified class name as the value.
	 * The class specified should implement {@link SpellCorrection} interface.
	 * 	  
	 * @param props	the properties to read from
	 * @return  	a spelling correction module instance
	 * @throws 		ClassCastException		if the specified class does not except <code>SpellCorrection</code> interface
	 * @throws		ClassNotFoundException	if a spelling correction class cannot be found
	 * @throws		IllegalAccessException	if there is a problem with accessing a spelling correction method
	 * @throws		InstantiationException	if there is a problem with instantiating a spelling correction method
	 */
	public static SpellCorrection getSpellCorrection(Properties props) 
			throws ClassCastException, ClassNotFoundException, IllegalAccessException, InstantiationException {	
		Object obj = getComponent(props, "spellCorrection");
		if (!(obj instanceof SpellCorrection)) 
			throw new ClassCastException("Spell correction class cannot be cast to SpellCorrection.");
		return (SpellCorrection)obj;
	}
	
	/**
	 * Instantiates a sentence segmenter from properties.
	 * It assumes the class name is specified in a key/value pair, with the key
	 * 'sentenceSegmenter' and the fully-qualified class name as the value.
	 * The class specified should implement {@link SentenceSegmenter} interface.
	 * 	  
	 * @param props	the properties to read from
	 * @return		a sentence segmenter module instance
	 * @throws 		ClassCastException		if the specified class does not except <code>SentenceSegmenter</code> interface
	 * @throws		ClassNotFoundException	if the sentence segmenter class cannot be found
	 * @throws		IllegalAccessException	if there is a problem with accessing the sentence segmenter
	 * @throws		InstantiationException	if there is a problem with instantiating the sentence segmenter
	 */
	public static SentenceSegmenter getSentenceSegmenter(Properties props) 
			throws ClassNotFoundException, IllegalAccessException, InstantiationException, ClassCastException {	
		Object obj = getComponent(props, "sentenceSegmenter");
		if (!(obj instanceof SentenceSegmenter)) 
			throw new ClassCastException("Sentence segmenter class cannot be cast to SentenceSegmenter.");
		else return (SentenceSegmenter)obj;
	}
	
	/**
	 * Instantiates a section segmenter from properties.
	 * It assumes the class name is specified in a key/value pair, with the key
	 * 'sectionSegmenter' and the fully-qualified class name as the value.
	 * The class specified should implement {@link SectionSegmenter} interface.
	 * 	  
	 * @param props	the properties to read from
	 * @return		a section segmenter module instance
	 * @throws 		ClassCastException		if the specified class does not except <code>SectionSegmenter</code> interface
	 * @throws		ClassNotFoundException	if the section segmenter class cannot be found
	 * @throws		IllegalAccessException	if there is a problem with accessing the section segmenter
	 * @throws		InstantiationException	if there is a problem with instantiating the section segmenter
	 */
	public static SectionSegmenter getSectionSegmenter(Properties props) 
			throws ClassNotFoundException, IllegalAccessException, InstantiationException, ClassCastException {	
		Object obj = getComponent(props, "sectionSegmenter");
		if (!(obj instanceof SectionSegmenter)) 
			throw new ClassCastException("Section segmenter class cannot be cast to SectionSegmenter.");
		else return (SectionSegmenter)obj;
	}
	
	/**
	 * Returns an instance of a processing component, specified by a class name.
	 * 
	 * @param props		the properties to read from
	 * @param propName  the property that indicates the class name for the component
	 * @return			an instance of the processing component
	 * @throws		ClassNotFoundException	if the component class cannot be found
	 * @throws		IllegalAccessException	if there is a problem with accessing the component
	 * @throws		InstantiationException	if there is a problem with instantiating the component
	 */
	public static Object getComponent(Properties props, String propName) 
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		String prop = props.getProperty(propName);
		if (prop.trim().equals("")) {
			log.log(Level.WARNING, "Unable to find a property for propName: {0}.", prop);
			return null;
		}
		Class cl = Class.forName(prop);
		log.info("Returning an instance of " + cl.getName());
		return cl.newInstance();
	}
	
	/**
	 * Returns multiple instances of processing components, specified by their class names.
	 * 
	 * @param props		the properties to read from
	 * @param propName	the property that indicates the class names for the component
	 * @param separator	the delimiter between the class names
	 * @return			a list of the processing components
	 * @throws		ClassNotFoundException	if a component class cannot be found
	 * @throws		IllegalAccessException	if there is a problem with accessing a component
	 * @throws		InstantiationException	if there is a problem with instantiating a component
	 */
	public static List<Object> getComponents(Properties props, String propName, String separator) 
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		String comps = props.getProperty(propName);
		if (comps.trim().equals("")) {
			log.log(Level.WARNING, "Unable to find a property for propName: {0}.", comps);
			return null;
		}
		String[] clNames = comps.split(separator);
		List<Object> objs = new ArrayList<Object>();
		List<String> seen = new ArrayList<String>();
		for (int i=0; i < clNames.length; i++) {
			String n = clNames[i];
			if (seen.contains(n)) continue;
			Class cl = Class.forName(n);
			log.info("Returning an instance of " + cl.getName());
			Object obj = cl.newInstance();
			objs.add(obj);
			seen.add(n);
		}
		return objs;
	}

}
