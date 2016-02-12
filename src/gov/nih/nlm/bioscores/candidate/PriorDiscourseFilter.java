package gov.nih.nlm.bioscores.candidate;

import java.util.List;
import java.util.logging.Logger;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * This filter eliminates referents that appear subsequent to the coreferential mention
 * in the document. 
 * It is useful for {@link CoreferenceType#Anaphora} type.
 * 
 * @author Halil Kilicoglu
 *
 */
public class PriorDiscourseFilter implements CandidateFilter {
	private static Logger log = Logger.getLogger(PriorDiscourseFilter.class.getName());

	@Override
	public void filter(SurfaceElement exp, CoreferenceType type, ExpressionType expType,
			List<SurfaceElement> candidates, List<SurfaceElement> filteredCandidates) {
		if (candidates == null) return;
		if (type.getSearchDirection() == CoreferenceType.SEARCH_FORWARD) {
			log.warning("Searching forward and PriorDiscourseFilter are incompatible.");
			return;
		}
		for (SurfaceElement cand: candidates) {
			if (SpanList.atLeft(cand.getSpan(),exp.getSpan())) filteredCandidates.add(cand);
		}
	}
}
