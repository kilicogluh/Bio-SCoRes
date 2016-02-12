package gov.nih.nlm.bioscores.candidate;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.Expression;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.Entity;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.util.SemUtils;

/**
 * This filter eliminates referents that are not associated with one of the
 * pre-defined semantic classes, such as {@link Entity}.
 * 
 * Relative pronoun mentions are eliminated regardless of whether {@link Expression}
 * objects are allowed.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SemanticClassFilter implements CandidateFilter {

	private Collection<Class> classes;
	
	/**
	 * Constructs a {@code SemanticClassFilter} object with a collection of 
	 * semantic classes.
	 * 
	 * @param classes	the collection of semantic classes
	 */
	public SemanticClassFilter(Collection<Class> classes) {
		this.classes = classes;
	}
	
	@Override
	public void filter(SurfaceElement exp, CoreferenceType type, ExpressionType expType,
			List<SurfaceElement> candidates,List<SurfaceElement> filteredCandidates) {
		if (candidates == null) return;
		for (SurfaceElement cand: candidates) {
			Set<SemanticItem> sems = cand.getSemantics();
			if (sems == null) continue;
			for (SemanticItem sem: sems) {
				Class<? extends SemanticItem> clazz = sem.getClass();
				if (sem instanceof Expression && 
						ExpressionType.valueOf(sem.getType()) == ExpressionType.RelativePronoun) continue;
				if (SemUtils.hasGeneralizationOf(clazz, classes)) filteredCandidates.add(cand);
				if (filteredCandidates.contains(cand)) break;
			}
		}
	}

}
