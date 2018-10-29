package gov.nih.nlm.bioscores.candidate;

import java.util.List;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * This filter eliminates referents that appear prior to the coreferential mention
 * in the document. 
 * It is useful for {@link CoreferenceType#Cataphora} type.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SubsequentDiscourseFilter implements CandidateFilter {
	private static Logger log = Logger.getLogger(SubsequentDiscourseFilter.class.getName());

	@Override
	public void filter(SurfaceElement exp, CoreferenceType type, ExpressionType expType,
					List<SurfaceElement> candidates, List<SurfaceElement> filteredCandidates) {
		if (candidates == null) return;
		if (type.getSearchDirection() == CoreferenceType.SEARCH_BACKWARD) {
			log.warning("Searching backward and SubsequentDiscourseFilter are incompatible.");
			return;
		}
		for (SurfaceElement cand: candidates) {
			if (cand.equals(exp)) continue;
			if (SpanList.atLeft(exp.getSpan(),cand.getSpan())) filteredCandidates.add(cand);
		}
	}
}
