package gov.nih.nlm.ling.brat;

import gov.nih.nlm.ling.core.SpanList;

/**
 * The interface for an abstract natural language annotation. 
 * It also defines methods to compare two annotations for evaluation purposes.
 *
 * @author Halil Kilicoglu
 */
public interface Annotation extends Comparable<Annotation> {
	/**
	 * Gets the identifier of this annotation.
	 * 
	 * @return	the annotation identifier
	 */
	  public String getId();
	  
	  /**
	   * Gets the identifier of the document of this annotation.
	   *  
	   * @return	the document identifier
	   */
	  public String getDocId();
	  
	  /**
	   * Gets the annotation type.
	   * 
	   * @return	the annotation type
	   */
	  public String getType();
	  /**
	   * Gets the annotation class (Term, etc.)
	   * 
	   * @return	the annotation class
	   */
	  public String getAnnotationType();
	  /**
	   * Gets annotation span.
	   * 
	   * @return the annotation span.
	   */
	  public SpanList getSpan();
	  /**
	   * Indicates whether this annotation is an exact match to <var>annotation1</var>.
	   * 
	   * @param annotation1	the annotation to compare
	   * @return	true if the annotations are an exact match
	   */
	  public abstract boolean exactMatch(Annotation annotation1);
	  /**
	   * Indicates whether this annotation is an approximate match to <var>annotation1</var>.
	   * 
	   * @param annotation1	the annotation to compare
	   * @return	true if the annotations are an approximate match
	   */
	  public abstract boolean approximateMatch(Annotation annotation1);
	  /**
	   * Indicates whether this annotation matches <var>annotation1</var> based on span only.
	   * 
	   * @param annotation1	the annotation to compare
	   * @return	true if the annotations are a span match
	   */
	  public abstract boolean spanMatch(Annotation annotation1);
	  /**
	   * Indicates whether this annotation matches <var>annotation1</var> based on ontological mapping.
	   * 
	   * @param approximateMatch	true if the underlying comparison will be based on approximate matching
	   * @param annotation1			the annotation to compare
	   * @return	true if the annotations are a reference match
	   */
	  public abstract boolean referenceMatch(boolean approximateMatch, Annotation annotation1);
	  public abstract String toString();
	  /**
	   * Gets the resolved string representation of this annotation.
	   * Term identifiers are replaced with their text and span information.
	   * 
	   * @return	the resolved string representation
	   */
	  public abstract String toResolvedString();
	  /**
	   * Gets the standoff annotation string of this annotation.
	   * 
	   * @return	the standoff annotation string
	   */
	  public abstract String toStandoffAnnotation();
}
