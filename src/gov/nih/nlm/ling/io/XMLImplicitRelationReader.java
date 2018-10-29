package gov.nih.nlm.ling.io;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import nu.xom.Element;

/**
 * @author Halil Kilicoglu
 *
 */
public class XMLImplicitRelationReader implements XMLRelationReader {
	private static final Logger log = Logger.getLogger(XMLImplicitRelationReader.class.getName());

	public XMLImplicitRelationReader() {}

	@Override
	public Relation read(Element element, Document doc) {
		return read(element,doc,null,null);
	}
	
	@Override
	public Relation read(Element element, Document doc, 
			Set<String> ignoreArgTypes, Map<String,SemanticItem> equivMap) {
		String id = element.getAttributeValue("id");
		String sem = element.getAttributeValue("type");
		SemanticItemFactory sif = doc.getSemanticItemFactory();
		List<Argument> args = XMLArgumentReader.readArguments(element,doc,ignoreArgTypes,equivMap);
    	if (args.size() == 0) {
    		log.log(Level.WARNING, "Relation {0} with no valid arguments: {1}. Skipping..", 
    				new Object[]{id,element.toXML()});
    		return null;
    	}
    	Relation rel = sif.newImplicitRelation(doc, sem, args);
    	rel.setId(id);
    	log.log(Level.FINEST, "Generated relation {0}. ", new String[]{rel.toString()});
    	return rel;
	}

}
