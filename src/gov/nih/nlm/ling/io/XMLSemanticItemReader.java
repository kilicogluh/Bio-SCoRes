/**
 * 
 */
package gov.nih.nlm.ling.io;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.sem.SemanticItem;
import nu.xom.Element;

/**
 * Interface to implement concrete XML readers for <code>SemanticItem</code> objects.
 * 
 * @author Halil Kilicoglu
 *
 */
public interface XMLSemanticItemReader {
	
	/**
	 * Creates a <code>SemanticItem</code> object from an XML element 
	 * and associates it with a document.
	 * 
	 * @param element	the XML element corresponding to the semantic item
	 * @param doc		the document the semantic item belongs to.
	 * 
	 * @return			the <code>SemanticItem</code> resulting from the XML
	 */
	public SemanticItem read(Element element, Document doc);

}
