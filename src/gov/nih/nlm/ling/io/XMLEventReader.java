package gov.nih.nlm.ling.io;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.sem.Argument;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.Predication;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import nu.xom.Attribute;
import nu.xom.Element;

/**
 * @author Halil Kilicoglu
 *
 */
public class XMLEventReader implements XMLRelationReader {
	private static final Logger log = Logger.getLogger(XMLEventReader.class.getName());

	public XMLEventReader() {}

	@Override
	public Relation read(Element element, Document doc) {
		return read(element,doc,null,null);
	}
	
	@Override
	// TODO This needs further testing.
	 public Relation read(Element el, Document doc, 
			 Set<String> ignoreArgTypes, Map<String,SemanticItem> equivMap) {
//		List<String> roleTerms = new ArrayList<>();
		String id = el.getAttributeValue("id");
		String type = el.getAttributeValue("type");
		String predicateId = el.getAttributeValue("predicate");
		log.log(Level.FINEST,"Read event {0} predicate with id {1} and type {2}.", 
				new Object[]{id,predicateId,type});
		SemanticItemFactory sif = doc.getSemanticItemFactory();
		List<Argument> args = XMLArgumentReader.readArguments(el,doc,ignoreArgTypes,equivMap);
/*		Elements argEls = el.getChildElements();
		List<Argument> args = new ArrayList<>();
		for (int i=0; i < argEls.size(); i++) {
			Element argEl = argEls.get(i);
			String name = argEl.getLocalName();
			if (ignoreArgTypes != null && ignoreArgTypes.contains(name)) continue;
			String argId = argEl.getAttributeValue("idref");
	    	// no id given, skip
	    	if (name.length() ==0 || argId.length() == 0) {
		    	logger.log(Level.WARNING,"Argument of event {0} with no id or role: {1}", 
		    			new Object[]{id,argId});
	    		continue;
	    	}
	    	// a bit dangerous but sometimes for some reason Theme2 exists but not Theme.
	    	name = name.replaceAll("[0-9]*$", "");
	    	logger.log(Level.FINEST,"Read event {0} argument with id {1} and role {2}.", 
	    			new Object[]{id,argId,name});
	    	if (equivMap.containsKey(argId)) argId = equivMap.get(argId).getId();
	    	String roleTerm = name + "_" + argId;
	    	// This is a limitation?
	    	// if (roleTerms.contains(roleTerm)) continue;
	    	SemanticItem ob = doc.getSemanticItemById(argId);
	    	if (ob == null) {
	    		logger.log(Level.WARNING,"Unresolved event {0} argument: {1}. The event cannot be initialized. ", 
	    				new Object[]{id,argId});
	    		return null;
	    	}
	    	args.add(new Argument(name,ob));
	    	roleTerms.add(roleTerm);
		}*/
    	if (args.size() == 0) {
    		log.log(Level.WARNING, "Event {0} with no valid arguments: {1}. The event cannot be initialized.", 
    				new Object[]{id,el.toXML()});
    		return null;
    	}
    	for (Argument arg: args) {
    		String name = arg.getType();
	    	// a bit dangerous but sometimes for some reason Theme2 exists but not Theme.
	    	name = name.replaceAll("[0-9]*$", "");
	    	log.log(Level.FINEST,"Read event {0} argument with id {1} and role {2}.", 
	    			new Object[]{id,arg.getArg().getId(),name});
    	}
		Predicate pr = null;
		if (predicateId != null) {
			pr = (Predicate)doc.getSemanticItemById(predicateId);
			// could be null at this point, because the same span was tagged multiple times with the same type
			// and we only recorded one of them, so get it from the equivalence
			if (pr == null) {
				pr = (Predicate)equivMap.get(predicateId);
				if (pr == null) {
		    		log.log(Level.WARNING,"Unresolved event {0} predicate: {1}. The event cannot be initialized. ", 
		    				new Object[]{id,predicateId});
					return null;
				}
			}
		}
//		Event ev = sif.newEvent(doc, pr, args);
		// This was necessary for composition.
		Predication ev = sif.newPredication(doc, pr, args, null, null);
		ev.setId(id);
		for (int i=0; i < el.getAttributeCount();i++) {
			Attribute att = el.getAttribute(i);
			String name = att.getLocalName();
			if (name.equals("id") || name.equals("type") || name.equals("predicate")) continue;
			String val = att.getValue();
			ev.addFeature(name, val);
		}
		// TODO Why is this removed?? Does it even work?
//		Document.getSemanticItemsByClass(doc, Relation.class).remove(ev);
		log.log(Level.FINEST, "Generated event {0}. ", new String[]{ev.toString()});
		return ev;
	 }

}
