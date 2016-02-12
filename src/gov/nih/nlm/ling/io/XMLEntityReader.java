package gov.nih.nlm.ling.io;

import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.sem.Concept;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import nu.xom.Element;

/**
 * The reader for entity terms in XML. An entity element is expected to have id, type, 
 * charOffset, and headOffset attributes. If the element corresponding to the entity 
 * has children with the tag Concept, concepts from these elements will be assigned 
 * to the entity, additionally. 
 * 
 * @see Entity
 * 
 * @author Halil Kilicoglu
 *
 */
public class XMLEntityReader implements XMLSemanticItemReader {
	private static final Logger log = Logger.getLogger(XMLEntityReader.class.getName());
	
	public XMLEntityReader() {}

	@Override
	public SemanticItem read(Element element, Document doc) {
		String id = element.getAttributeValue("id");
		String type = element.getAttributeValue("type");
		String spStr = element.getAttributeValue("charOffset");
		String headSpStr = element.getAttributeValue("headOffset");
		SpanList sp = new SpanList(spStr);
		SpanList headSp = null;
		if (headSpStr != null) headSp = new SpanList(headSpStr);
		Sentence sent = doc.getSubsumingSentence(sp.getSpans().get(0));
		if (sent == null) {
			log.log(Level.WARNING,"No sentence can be associated with the XML: {0}", new Object[]{element.toXML()});
			return null;
		}
		String modSem= type;
		SemanticItemFactory sif = doc.getSemanticItemFactory();
		Entity ent = sif.newEntity(doc,sp,headSp,modSem);
		ent.setId(id);
		Concept.assignConceptsFromXml(element,ent);
//		if (reassignType) {
			if (ent.getConcepts() != null && ent.getConcepts().size() > 0) {
				Concept sense = ent.getSense();
				if (sense == null) 
					sense = ent.getConcepts().iterator().next();
				ent.setType(sense.getSemtypes().iterator().next());
			}
//		}

		sent.synchSurfaceElements(ent.getSurfaceElement());
		sent.synchEmbeddings();
	   	log.log(Level.FINEST,"Generated entity {0} with the head {1}. ", 
	   			new String[]{ent.toString(), ent.getSurfaceElement().getHead().toString()});
	   	return ent;

	}
	


}
