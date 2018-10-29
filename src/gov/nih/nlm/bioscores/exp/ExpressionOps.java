package gov.nih.nlm.bioscores.exp;

import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * Interface that defines operations associated with recognizing coreferential mentions
 * of a given type.
 * 
 * @author Halil Kilicoglu
 *
 */
public interface ExpressionOps {
	/**
	 * Determines whether a given textual unit is a coreferential mention of a specific type.
	 * 
	 * @param surfaceElement	the textual unit
	 * @return	true if the textual unit is recognized as a mention of a specific type
	 */
	public boolean recognize(SurfaceElement surfaceElement);
}
