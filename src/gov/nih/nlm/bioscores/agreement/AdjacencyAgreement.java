package gov.nih.nlm.bioscores.agreement;

import java.util.LinkedHashSet;
import java.util.List;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.ConjunctionDetection;

/**
 * The implementation of adjacency constraint. It is useful for 
 * resolution of relative pronoun anaphora. <p>
 * Agreement is predicted if the coreferential mention and the referent
 * are adjacent OR if the intervening tokens are not alphanumeric.
 * 
 * @author Halil Kilicoglu
 *
 */
public class AdjacencyAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		Sentence sent = exp.getSentence();
		Span sp = (SpanList.atLeft(exp.getSpan(),referent.getSpan()) ? 
				new Span(exp.getSpan().getEnd(),referent.getSpan().getBegin()) : 
					new Span(referent.getSpan().getEnd(),exp.getSpan().getBegin()));
		List<SurfaceElement> surfs = sent.getSurfaceElementsFromSpan(sp);
		// no items in between
		if (surfs == null)  return true;
		else {
			boolean allNonAlpha = true;
			for (SurfaceElement su: surfs) {
				if (su.isAlphanumeric() && su.isPrepositional() == false) { allNonAlpha = false; break;}
			}
			// only non-alphanumeric characters in between
			if (allNonAlpha) return true;
			// handle conjunctions
			for (int i=surfs.size();i > 0; i--) {
				SurfaceElement su = surfs.get(i-1);
				if (ConjunctionDetection.isConjunctionArgument(referent, su))  {
					LinkedHashSet<SurfaceElement> conjArgs = ConjunctionDetection.getConjunctSurfaceElements(referent);
					if (conjArgs.size() < 2) return false;
					for (SurfaceElement arg: conjArgs) {
						if (arg.equals(su)) continue;
						if (SpanList.atLeft(exp.getSpan(), arg.getSpan())) return false;
						List<SurfaceElement> inter = sent.getSurfaceElementsFromSpan(new Span(arg.getSpan().getEnd(),referent.getSpan().getBegin()));
						for (SurfaceElement in : inter) {
							if (in.isRelativePronoun()) return false;
						}
					}
					return true;
				} else {
					if (su.isAlphanumeric() && su.isPrepositional() == false) return false;
				}
			} 
		}
		return false;
	}
}
