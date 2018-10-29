package gov.nih.nlm.ling.core;

/**
 * An interface for contiguous textual units, which extends <code>SurfaceElement</code> interface.
 * The classes that implement this interface represent textual units without gaps.
 * 
 * @see SurfaceElement
 * @see Word
 * @see MultiWord
 * 
 * @author Halil Kilicoglu
 *
 */
public interface ContiguousSurfaceElement  extends SurfaceElement {
	/**
	 * @return  the span of this textual unit 
	 */
	public Span getSingleSpan();
}
