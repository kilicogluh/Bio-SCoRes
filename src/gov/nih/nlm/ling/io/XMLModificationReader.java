package gov.nih.nlm.ling.io;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Modification;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import nu.xom.Element;

/**
 * 
 * @author Halil Kilicoglu
 *
 */
public class XMLModificationReader implements XMLRelationReader {
	private static final Logger log = Logger.getLogger(XMLModificationReader.class.getName());

	public XMLModificationReader() {}

	@Override
	public Relation read(Element element, Document doc) {
		return read(element,doc,null,null);
	}
	
	@Override
	 public Relation read(Element el, Document doc, 
			 Set<String> ignoreArgTypes, Map<String,SemanticItem> equivMap) {
		String id = el.getAttributeValue("id");
		String type = el.getAttributeValue("type");
		String value = el.getAttributeValue("value");
		SemanticItemFactory sif = doc.getSemanticItemFactory();
		List<Argument> args = XMLArgumentReader.readArguments(el,doc,ignoreArgTypes,equivMap);
    	if (args.size() == 0) {
    		log.log(Level.WARNING, 
    				"Event modification {0} with no valid arguments: {1}. The event modification cannot be initialized.", 
    				new Object[]{id,el.toXML()});
    		return null;
    	}
		// This was necessary for composition.
    	Modification mod = sif.newModification(doc, type, args);
    	mod.setValue(value);
    	mod.setId(id);
		log.log(Level.FINEST, "Generated modification {0}. ", new String[]{mod.toString()});
		return mod;
	 }

}
