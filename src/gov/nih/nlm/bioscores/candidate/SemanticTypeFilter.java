package gov.nih.nlm.bioscores.candidate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import gov.nih.nlm.bioscores.core.CoreferenceProperties;
import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.SemanticItem;

/**
 * This filter eliminates referents that are not associated with one of the
 * pre-defined semantic types, such as Disorder, Drug, etc.
 * 
 * If no semantic type is specified, all the semantic types defined through
 * {@link CoreferenceProperties#SEMTYPES} will be used.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SemanticTypeFilter implements CandidateFilter {

	private Collection<String> semanticTypes;
	
	/**
	 * Constructs a {@link SemanticTypeFilter} object with a collection of 
	 * semantic types
	 * 
	 * @param semanticTypes	the collection of semantic types
	 */
	public SemanticTypeFilter(Collection<String> semanticTypes) {
		this.semanticTypes = semanticTypes;
	}

	@Override
	public void filter(SurfaceElement exp, CoreferenceType type, ExpressionType expType,
			List<SurfaceElement> candidates, List<SurfaceElement> filteredCandidates) {
		if (candidates == null) return;
		if (semanticTypes == null) {
			semanticTypes = new ArrayList<>();
			for (String k: CoreferenceProperties.SEMTYPES.keySet()) {
				semanticTypes.addAll(CoreferenceProperties.SEMTYPES.get(k));
			}
		}
		if (semanticTypes.size() == 0) {
			filteredCandidates = candidates;
			return;
		}
		for (SurfaceElement cand: candidates) {
			Set<SemanticItem> sems = cand.getSemantics();
			if (sems == null) continue;
			for (SemanticItem sem: sems) {
				if (semanticTypes.contains(sem.getType())) {
					filteredCandidates.add(cand);
					break;
				}
			}
		}
	}

}
