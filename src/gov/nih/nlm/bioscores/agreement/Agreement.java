package gov.nih.nlm.bioscores.agreement;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * The interface for defining and implementing agreement constraints 
 * between a coreferential mention and a candidate referent.
 * 
 * @author Halil Kilicoglu
 *
 */
public interface Agreement {
	
	/**
	 * Method to implement to calculate agreement between a coreferential mention and a candidate referent.
	 * 
	 * @param corefType		the type of coreference being considered (e.g., {@link CoreferenceType#Anaphora}) 
	 * @param expType		the type of coreferential mention type being considered (e.g., {@link ExpressionType#PersonalPronoun})
	 * @param expression  	the coreferential mention
	 * @param referent  	the candidate referent
	 * @return true if the mention and the candidate agree on the criterion established by the implementing class
	 */
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement expression, SurfaceElement referent);
}
