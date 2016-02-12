package gov.nih.nlm.bioscores.exp;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.Expression;

/**
 * The interface to implement to determine whether to eliminate mentions from further
 * coreference consideration.
 * 
 * @see ExpressionFilterImpl
 * 
 * @author Halil Kilicoglu
 *
 */
public interface ExpressionFilter {

	/**
	 * Determines whether a mention needs to be
	 * resolved for a specific coreference type.
	 * 
	 * @param corefType	the coreference type
	 * @param exp		the mention annotation
	 * 
	 * @return true if the mention needs to resolved
	 */
	public boolean filter(CoreferenceType corefType, Expression exp);
}
