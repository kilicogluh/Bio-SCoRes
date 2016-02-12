package gov.nih.nlm.bioscores.candidate;

import java.util.List;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.CoreferenceUtils;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * This filter eliminates referents that do not have non-coreferential
 * semantics.
 * 
 * @author Halil Kilicoglu
 *
 */
public class HasSemanticsFilter implements CandidateFilter {

	@Override
	public void filter(SurfaceElement exp, CoreferenceType type, ExpressionType expType,
			List<SurfaceElement> candidates,
			List<SurfaceElement> filteredCandidates) {
		if (candidates == null) return;
		for (int i=0; i < candidates.size(); i++) {
			SurfaceElement sui = candidates.get(i);
			if (CoreferenceUtils.hasNonCoreferentialSemantics(sui)) 
				filteredCandidates.add(sui);
		}
	}

}
