package gov.nih.nlm.bioscores.candidate;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;

import java.util.List;

/**
 * This filter eliminates referents that do not constitute noun phrases.
 * 
 * @author Halil Kilicoglu
 *
 */
public class NounPhraseFilter implements CandidateFilter {

	@Override
	public void filter(SurfaceElement exp, CoreferenceType type, ExpressionType expType,
			List<SurfaceElement> candidates,
			List<SurfaceElement> filteredCandidates) {
		if (candidates == null) return;
		for (SurfaceElement cand: candidates) {
			if (cand.isNominal()) filteredCandidates.add(cand);
		}
	}

}
