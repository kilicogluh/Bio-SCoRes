package gov.nih.nlm.ling.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.SemanticItem;
import nu.xom.Element;
import nu.xom.Elements;

/**
 * Reader for <code>Argument</code> elements of <code>Relation</code> objects. Each argument XML element 
 * is expected to have id and idref attributes, idref corresponding to the argument <code>SemanticItem</code>
 * object. The name of the XML element is taken as the role of the argument.
 * 
 * @see Argument
 * 
 * @author Halil Kilicoglu
 *
 */
public class XMLArgumentReader {
	private static final Logger log = Logger.getLogger(XMLArgumentReader.class.getName());
	
	/**
	 * Reads the arguments of the XML element <var>el</var>, which corresponds to the relation
	 * object and its children to arguments.  The arguments with a role in <var>ignoreArgTypes</var>
	 * will be ignored. If the argument id is a key in <var>equivMap</var>, the corresponding value
	 * in the map will be used, instead.
	 * 
	 * @param el				the XML element corresponding to the enclosing <code>Relation</code> object
	 * @param doc				the document associated with the semantic items
	 * @param ignoreArgTypes	the argument roles to ignore
	 * @param equivMap			the equivalence map between semantic items in the document
	 * @return					the list of arguments for the enclosing <code>Relation</code> object
	 */
	public static List<Argument> readArguments(Element el, Document doc, 
			Set<String> ignoreArgTypes, Map<String,SemanticItem> equivMap) {
		String id = el.getAttributeValue("id");
		Elements argEls = el.getChildElements();
		List<Argument> args = new ArrayList<>();
    	for (int i=0; i < argEls.size(); i++) {
			Element argEl = argEls.get(i);
			String name = argEl.getLocalName();
			if (ignoreArgTypes != null && ignoreArgTypes.contains(name)) continue;
			String argId = argEl.getAttributeValue("idref");
	    	if (equivMap.containsKey(argId)) argId = equivMap.get(argId).getId();
	    	// no id given, skip
	    	if (name.length() ==0 || argId.length() == 0) {
		    	log.log(Level.WARNING,"Argument of relation {0} with no id or role: {1}", 
		    			new Object[]{id,argId});
	    		continue;
	    	}
	    	SemanticItem ob = doc.getSemanticItemById(argId);
	    	if (ob == null) {
	    		log.log(Level.WARNING,"Unresolved relation {0} argument: {1}. Th cannot be initialized. ", 
	    				new Object[]{id,argId});
	    		return new ArrayList<>();
		    }
	    	args.add(new Argument(name,ob));
    	}
    	return args;
	}

}
