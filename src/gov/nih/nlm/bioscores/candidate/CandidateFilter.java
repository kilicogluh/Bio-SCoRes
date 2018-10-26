package gov.nih.nlm.bioscores.candidate;

import java.util.List;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * An interface to extend when defining candidate referent filtering methods to 
 * eliminate candidate referents incompatible with the coreferential mention
 *  on some specific constraint.
 * 
 * @author Halil Kilicoglu
 *
 */
public interface CandidateFilter {
	/**
	 * Method to implement to filter candidate referents on some specific constraint.
	 * 
	 * @param exp				the coreferential mention
	 * @param corefType			the type of coreference being considered
	 * @param expType			the type of coreferential mention type being considered
	 * @param candidates  		the list of candidate referents
	 * @param filteredCandidates	 the list of remaining candidate referents after filtering
	 */
	public void filter(SurfaceElement exp, CoreferenceType corefType, ExpressionType expType, List<SurfaceElement> candidates, List<SurfaceElement> filteredCandidates);

}
