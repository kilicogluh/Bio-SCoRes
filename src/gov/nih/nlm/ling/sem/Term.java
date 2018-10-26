package gov.nih.nlm.ling.sem;

import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;

import java.util.Set;

/**
 * An interface for atomic semantic items. 
 * 
 * @see Entity
 * @see Predicate
 * 
 * @author Halil Kilicoglu
 *
 */
public interface Term extends SemanticItem {

	/**
	 * 
	 * @return the mention associated with the term
	 */
	public SurfaceElement getSurfaceElement();
	public void setSurfaceElement(SurfaceElement se);
	
	/**
	 * 
	 * @return the span of the mention head.
	 */
	public SpanList getHeadSpan();
	public void setHeadSpan(SpanList ls);
	
	/**
	 * 
	 * @return the string covered by the term
	 */
	public String getText();
	/**
	 * 
	 * @return the set of semantic types associated with the term
	 */
	public Set<String> getSemtypes();	
}
