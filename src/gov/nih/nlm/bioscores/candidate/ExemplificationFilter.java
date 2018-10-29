package gov.nih.nlm.bioscores.candidate;

import java.util.List;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.AppositiveDetection;

/**
 * This filter eliminates potential referents that are exemplifications of
 * other textual units.
 * 
 * @author Halil Kilicoglu
 *
 */
public class ExemplificationFilter implements CandidateFilter {
	@Override
	public void filter(SurfaceElement exp, CoreferenceType type, ExpressionType expType, 
			List<SurfaceElement> candidates, List<SurfaceElement> filteredCandidates) {
		if (candidates == null) return;
		for (SurfaceElement cand: candidates) {
			if (AppositiveDetection.exemplifies(cand,false) == false) filteredCandidates.add(cand); 
		}
	}

}
