package gov.nih.nlm.bioscores.agreement;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.AppositiveDetection;
import gov.nih.nlm.ling.sem.ConjunctionDetection;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.util.DiscourseUtils;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Agreement constraint based on appositive constructions. This agreement is 
 * useful for {@link CoreferenceType#Appositive} type of coreference. <p>
 * 
 * Agreement is predicted if an appositive construction is detected between the coreferential
 * mention and the candidate referent, based on dependency relations as well as adjacency and
 * intervening tokens.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SyntacticAppositiveAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		LinkedHashSet<SemanticItem> conjs = ConjunctionDetection.filterByConjunctions(referent);
		NumberAgreement.Number num = NumberAgreement.number(exp);
		if (conjs.size() == 0) {
			if (num == NumberAgreement.Number.Plural) {
				LinkedHashSet<SurfaceElement> args = ConjunctionDetection.getConjuncts(referent);
				if (args.size() > 0) {
					for (SurfaceElement arg: args) {
						if (AppositiveDetection.syntacticAppositive(exp, arg) 
								|| adjacent(exp, referent)) return true;
					}
				}
			}
			else 
				return (AppositiveDetection.syntacticAppositive(exp, referent));
		}
		else if (num == NumberAgreement.Number.Plural) {
			LinkedHashSet<SurfaceElement> args = ConjunctionDetection.getConjunctSurfaceElements(referent);
			for (SurfaceElement arg: args) {
				if (AppositiveDetection.syntacticAppositive(exp, arg) || adjacent(exp, referent)) return true;
			}
		}
		return false;
	}
	
	private boolean adjacent(SurfaceElement a, SurfaceElement b) {
		if (ConjunctionDetection.getConjuncts(a).contains(b)) return false;
		LinkedHashSet<SemanticItem> conjs = ConjunctionDetection.filterByConjunctions(b);
		if (conjs.size() > 0) {
			LinkedHashSet<SurfaceElement> args = ConjunctionDetection.getConjunctSurfaceElements(b);
			if (args.contains(a)) return false;
			for (SurfaceElement arg: args) {
				if (onlyCommaParenthesisIntervenes(a,arg)) return true;
			}
		}
		else {
			if (onlyCommaParenthesisIntervenes(a,b)) return true;
		}
		return false;
	}
	
	private boolean onlyCommaParenthesisIntervenes(SurfaceElement a, SurfaceElement b) {
		if (DiscourseUtils.sameSentence(a, b) == false) return false;
		Span sp = (SpanList.atLeft(a.getSpan(),b.getSpan()) ? 
				new Span(a.getSpan().getEnd(),b.getSpan().getBegin()) : 
					new Span(b.getSpan().getEnd(),a.getSpan().getBegin()));
		List<SurfaceElement> surfs = a.getSentence().getSurfaceElementsFromSpan(sp);
		if (surfs == null || surfs.size() == 0)  return false;
		for (SurfaceElement su: surfs) {
			if (su.isAlphanumeric()) return false;
			if (su.getText().equals(",") == false && su.getText().equals("(") == false) return false;
		}
		return true;
	}

}
