package gov.nih.nlm.bioscores.candidate;

import java.util.List;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * This filter eliminates referents that do not appear in the same sentence
 * as the coreferential mention.
 * It is useful for {@link CoreferenceType#Appositive} and 
 * {@link CoreferenceType#PredicateNominative} types.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SameSentenceFilter implements CandidateFilter {
	private static Logger log = Logger.getLogger(SameSentenceFilter.class.getName());

	@Override
	public void filter(SurfaceElement exp, CoreferenceType type, ExpressionType expType, 
			List<SurfaceElement> candidates, List<SurfaceElement> filteredCandidates) {
		if (candidates == null) return;
		if (type.getSearchDirection() != CoreferenceType.SEARCH_BOTH) {
			log.warning("Searching both directions and SameSentenceFilter are incompatible.");
			return;
		}
		for (SurfaceElement cand: candidates) {
			if (cand.equals(exp) || SpanList.overlap(cand.getSpan(), exp.getSpan())) continue;
			if (cand.getSentence().equals(exp.getSentence())) filteredCandidates.add(cand);
		}
	}
}
