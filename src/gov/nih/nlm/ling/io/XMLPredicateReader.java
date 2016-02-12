package gov.nih.nlm.ling.io;

import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.sem.SemanticItemFactory;
import nu.xom.Element;

/**
 * The reader for predicate terms in XML. A predicate element is expected to have id, 
 * type, charOffset, and headOffset attributes.
 * 
 * @see Predicate
 * 
 * @author Halil Kilicoglu
 *
 */
public class XMLPredicateReader implements XMLSemanticItemReader {
	private static final Logger log = Logger.getLogger(XMLPredicateReader.class.getName());

	public XMLPredicateReader() {}

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
	   	SemanticItemFactory sif = doc.getSemanticItemFactory();
	   	Predicate pr = sif.newPredicate(doc,sp,headSp,type);
		pr.setId(id);
		sent.synchSurfaceElements(pr.getSurfaceElement());
	   	sent.synchEmbeddings();
	   	log.log(Level.FINEST,"Generated predicate {0} with the head {1}. ", 
	   			new String[]{pr.toString(), pr.getSurfaceElement().getHead().toString()});
	   	return pr;
	}

}
