package gov.nih.nlm.bioscores.io;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.core.CoreferenceChain;
import gov.nih.nlm.bioscores.core.CoreferenceSemanticItemFactory;
import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.io.XMLArgumentReader;
import gov.nih.nlm.ling.io.XMLImplicitRelationReader;
import gov.nih.nlm.ling.io.XMLRelationReader;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import nu.xom.Element;

/**
 * Reader for coreference chains from XML. This is essentially the same as {@link XMLImplicitRelationReader}; 
 * however, a further type check is performed to ensure that the type attribute of the XML element 
 * corresponds to the known coreference type, such as 'Anaphora'.
 * 
 * @author kilicogluh
 *
 */
public class XMLCoreferenceChainReader implements XMLRelationReader {
	private static final Logger log = Logger.getLogger(XMLCoreferenceChainReader.class.getName());

	@Override
	public Relation read(Element element, Document doc) {
		return read(element,doc,null,null);
	}
	
	@Override
	public CoreferenceChain read(Element element, Document doc, 
			Set<String> ignoreArgTypes, Map<String,SemanticItem> equivMap) {
		String id = element.getAttributeValue("id");
		String sem = element.getAttributeValue("type");
		SemanticItemFactory sif = doc.getSemanticItemFactory();
		List<Argument> args = XMLArgumentReader.readArguments(element,doc,ignoreArgTypes,equivMap);
    	if (args.size() == 0) {
    		log.log(Level.WARNING, "Coreference chain {0} with no valid arguments: {1}.", 
    				new Object[]{id,element.toXML()});
    		return null;
    	} 
	    try {
	    	CoreferenceType.valueOf(sem);
	    	CoreferenceChain cr = ((CoreferenceSemanticItemFactory)sif).newCoreferenceChain(doc, sem, args);
	    	cr.setId(id);
	    	log.log(Level.FINEST, "Generated coreference chain {0}. ", new String[]{cr.toString()});
	    	return cr;
	    } catch (IllegalArgumentException ie) {
	    	log.log(Level.WARNING, "Unknown coreference type {0}. ", new String[]{sem});
	   	}
    	return null;
	}

}
