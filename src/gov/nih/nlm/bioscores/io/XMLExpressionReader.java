package gov.nih.nlm.bioscores.io;

import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.core.CoreferenceSemanticItemFactory;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.io.XMLEntityReader;
import gov.nih.nlm.ling.io.XMLSemanticItemReader;
import gov.nih.nlm.ling.sem.Concept;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.SemanticItem;
import nu.xom.Element;

/**
 * Reader for coreferential mentions from XML. This is essentially the same as {@link XMLEntityReader}, 
 * but an {@link Expression} object is created, instead of the more generic {@link Entity} object.
 * 
 * @author Halil Kilicoglu
 *
 */
public class XMLExpressionReader implements XMLSemanticItemReader {
	private static final Logger log = Logger.getLogger(XMLExpressionReader.class.getName());

	@Override
	public SemanticItem read(Element element, Document doc) {
		String id = element.getAttributeValue("id");
		String type = element.getAttributeValue("type");
		String spStr = element.getAttributeValue("charOffset");
		String headSpStr = element.getAttributeValue("headOffset");
    	SpanList sp = new SpanList(spStr);
		SpanList headSp = null;
		if (headSpStr != null) headSp = new SpanList(headSpStr);
	   	String expStr = element.getAttributeValue("text");
		Sentence sent = doc.getSubsumingSentence(sp.getSpans().get(0));
		if (sent == null) {
			log.log(Level.WARNING,"No sentence can be associated with the XML: {0}", new Object[]{element.toXML()});
			return null;
		}
	   	CoreferenceSemanticItemFactory sif = (CoreferenceSemanticItemFactory)doc.getSemanticItemFactory();
	   	Expression exp = sif.newExpression(doc,type,sp,headSp,expStr);
	   	exp.setId(id);
	   	Concept.assignConceptsFromXml(element, exp);
	   	sent.synchSurfaceElements(exp.getSurfaceElement());
	   	sent.synchEmbeddings();
	   	log.log(Level.FINEST,"Generated expression {0} with the head {1}. ", 
	   			new String[]{exp.toString(), exp.getSurfaceElement().getHead().toString()});
	   	return exp;

	}

}
