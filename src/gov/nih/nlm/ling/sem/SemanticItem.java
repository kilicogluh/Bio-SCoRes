package gov.nih.nlm.ling.sem;

import java.util.LinkedHashSet;
import java.util.Map;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.SpanList;
import nu.xom.Element;

/**
 * The basic interface for all textually-grounded semantic item. <p>
 * Semantic objects can be atomic, which need to further implement {@link Term} interface,
 * or they can be complex, which need to further implement {@link Relation} interface.<p>
 * A semantic item has a <i>type</i> (one of potentially many <i>semtypes</i>), a <i>span</i>, 
 * and is associated with a {@link Document}. It may be normalized to an abstract <i>concept</i> or 
 * <i>indicator</i> (an ontological entity -- {@link Ontology}). 
 * It can also have a number of non-core <i>feature</i>s, maintained as key-value pairs.
 * 
 * @see Term
 * @see Relation
 * 
 * @author Halil Kilicoglu
 *
 */
public interface SemanticItem extends Comparable<SemanticItem> {
	
	/**
	 * 
	 * @return	the identifier of the semantic item.
	 */
	public String getId();
	/**
	 * Sets the identifier of the semantic item.
	 * 
	 * @param s the identifier
	 */
	public void setId(String s);

	/**
	 * 
	 * @return all semantic types associated with the semantic item
	 */
	public LinkedHashSet<String> getAllSemtypes();
	
	/**
	 * 
	 * @return  the primary semantic type associated with the semantic item
	 */
	public String getType();
	
	/**
	 * @param sem  the primary semantic type
	 */
	public void setType(String sem);
	/**
	 * 
	 * @return	the character offsets of the semantic item
	 */
	public SpanList getSpan();
	/**
	 * 
	 * @return	the document that the semantic item belongs to
	 */
	public Document getDocument();
	
	/**
	 * 
	 * @return the string representation for debugging
	 */
	public String toShortString();
	/**
	 * 
	 * @return the string representation in standoff annotation format
	 */
	public String toStandoffAnnotation();
	/**
	 * 
	 * @return  the XML representation
	 */
	public Element toXml();
	
	/**
	 * Gets the ontological information associated with the semantic item. This is 
	 * an abstract {@link Indicator} in the case of a {@link Predicate}, and a 
	 * {@link Concept} in the case of an {@link Entity}.
	 * 
	 * @return the abstract ontological information (no spans) associated with the semantic item
	 */
	public Ontology getOntology();
	
	/**
	 * 
	 * @param obj  another semantic item
	 * @return  true if ontological information associated with the current item is the same as that associated with <var>obj</var>
	 */
	public boolean ontologyEquals(Object obj);
	
	/**
	 * 
	 * @return	the non-core features of this semantic item
	 */
	public Map<String,Object> getFeatures();
	/**
	 * Sets the non-core features of this semantic item
	 * 
	 * @param features	the feature map to apply
	 */
	public void setFeatures(Map<String,Object> features);
	
	/**
	 * Adds a non-core feature for this semantic item
	 * 
	 * @param name		the name of the feature
	 * @param feature	the feature 
	 */
	public void addFeature(String name, Object feature);
}
