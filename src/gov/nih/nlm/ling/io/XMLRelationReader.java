package gov.nih.nlm.ling.io;

import java.util.Map;
import java.util.Set;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.sem.Relation;
import gov.nih.nlm.ling.sem.SemanticItem;
import nu.xom.Element;

/**
 * Interface for XML reader for <code>Relation</code> objects. It takes into account
 * the potential equivalence of terms in the <code>Document</code> and that some 
 * relation arguments may be ignored. If two terms are equivalent, they probably have 
 * the same semantic type and have the same character offsets.
 * 
 * @author Halil Kilicoglu
 *
 */
public interface XMLRelationReader extends XMLSemanticItemReader {
	
	/**
	 * Creates a <code>Relation</code> object from a XML element.
	 * 
	 * @param element			the XML element corresponding to the relation
	 * @param doc				the document the relation belongs to
	 * @param ignoreArgTypes	the relation argument roles to ignore
	 * @param equivMap			the map indicating the equivalence of terms 
	 * @return					a <code>Relation</code> object corresponding to the XML element
	 */
	public Relation read(Element element, Document doc, 
			Set<String> ignoreArgTypes, Map<String,SemanticItem> equivMap);
}
