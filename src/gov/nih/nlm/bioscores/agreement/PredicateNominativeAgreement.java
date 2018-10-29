package gov.nih.nlm.bioscores.agreement;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.SynDependency;
import gov.nih.nlm.ling.sem.ConjunctionDetection;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.transform.DependencyDirectionReversal;

/**
 * Agreement constraint based on copular constructions. This agreement is 
 * useful for {@link CoreferenceType#PredicateNominative} type of coreference. <p>
 * 
 * Agreement is predicted if a copular construction is detected between the coreferential
 * mention and the candidate referent, based on dependency relations and word order.
 * 
 * @author Halil Kilicoglu
 *
 */
//TODO This can be enhanced with copular verbs other than 'be'.
public class PredicateNominativeAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		LinkedHashSet<SemanticItem> conjs = ConjunctionDetection.filterByConjunctions(referent);
		if (conjs.size() >0) {
			LinkedHashSet<SurfaceElement> args = ConjunctionDetection.getConjunctSurfaceElements(referent);
			for (SurfaceElement arg: args) {
				if (predicateNominativeWithDependency(exp,arg)) return true;
			}
			for (SurfaceElement arg: args) {
				if (onlyCopularIntervenes(exp,arg)) return true;
			}
		} else {
			if (predicateNominativeWithDependency(exp,referent)) return true;
			if (onlyCopularIntervenes(exp,referent)) return true;
		}
		return false;
	}
	
	// strictly dependency based recognition
	private boolean predicateNominativeWithDependency(SurfaceElement a, SurfaceElement b) {
		SurfaceElement left = (SpanList.atLeft(a.getSpan(),b.getSpan()) ? a: b);
		SurfaceElement right = (left.equals(a) ? b : a);
		List<SynDependency> embeddings = a.getSentence().getEmbeddings();
		List<SynDependency> copDeps = SynDependency.outDependenciesWithType(right, embeddings, "cop", true);
		if (copDeps.size() == 0) return false;

		Sentence sent = right.getSentence();
		List<SynDependency> negDeps = SynDependency.outDependenciesWithType(right, embeddings, "neg", true);
		if (sent.getTransformations().contains(DependencyDirectionReversal.class))
			negDeps = SynDependency.inDependenciesWithType(right, embeddings, "neg", true);
		if (negDeps.size() > 0) return false;
		
		List<SynDependency> subjDeps = SynDependency.outDependenciesWithTypes(right,embeddings, Arrays.asList("nsubj","csubj","xsubj"), true);
		for (SynDependency subj: subjDeps) {
			if (subj.getDependent().equals(left)) return true;
		}
		return false;
	}
	
	// word order based.
	private boolean onlyCopularIntervenes(SurfaceElement a, SurfaceElement b) {
		Span sp = (SpanList.atLeft(a.getSpan(),b.getSpan()) ? 
				new Span(a.getSpan().getEnd(),b.getSpan().getBegin()) : 
					new Span(b.getSpan().getEnd(),a.getSpan().getBegin()));
		List<SurfaceElement> surfs = a.getSentence().getSurfaceElementsFromSpan(sp);
		if (surfs == null || surfs.size() == 0)  return false;
//		surfs = removeParentheses(surfs);
		if (surfs.size() > 1) return false;
		for (SurfaceElement su: surfs) {
			if (su.containsLemma("be")) return true;
		}
		return false;
	}
	
	/*	private static List<SurfaceElement> removeParentheses(List<SurfaceElement> intervening) {
	List<SurfaceElement> out = new ArrayList<SurfaceElement>();
	boolean inParentheses = false;
	for (int i =0; i < intervening.size(); i++) {
		SurfaceElement s = intervening.get(i);
		if (s.containsToken("(")) inParentheses = true;
		if (s.containsToken(")")) {inParentheses = false; continue;}
		if (!inParentheses) out.add(s);
	}
	return out;
	}*/
}
