package gov.nih.nlm.bioscores.candidate;

import java.util.Map;

import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * An interface to extend to define methods to identify the best referent 
 * among a set of scored candidate referents.
 * 
 * @author Halil Kilicoglu
 *
 */
public interface PostScoringCandidateFilter {

	/**
	 * The method to implement to filter candidate referents based on 
	 * some measure that takes into account scores and the coreferential mention.
	 * 
	 * @param mention	the coreferential mention
	 * @param scoreMap	the candidate referent scores
	 * @return	the updated score map with only the best referents
	 */
	public Map<SurfaceElement,Integer> postFilter(SurfaceElement mention, Map<SurfaceElement,Integer> scoreMap);
}
