package gov.nih.nlm.bioscores.candidate;

import java.util.LinkedHashSet;
import java.util.List;

import gov.nih.nlm.bioscores.core.CoreferenceChain;
import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * This filter eliminates referents that are singleton coreferential mentions (i.e.,
 * not part of a coreference chain).
 * 
 * @author Halil Kilicoglu
 *
 */
public class SingletonMentionFilter implements CandidateFilter {
	@Override
	public void filter(SurfaceElement exp, CoreferenceType type, ExpressionType expType, 
			List<SurfaceElement> candidates, List<SurfaceElement> filteredCandidates) {
		if (candidates == null) return;
		for (SurfaceElement cand: candidates) {
			if (Expression.filterByExpressions(cand).size() > 0) {
				LinkedHashSet<CoreferenceChain> chains = CoreferenceChain.getChainsWithMention(cand);
				if (chains.size() == 0) continue;
			}
			filteredCandidates.add(cand);
		}
	}
}
