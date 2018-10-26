package gov.nih.nlm.bioscores.agreement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.sem.Predicate;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.wrappers.WordNetWrapper;

/**
 * Implementation of agreement based on predicates. 
 * This is currently unused but is expected to be useful for nominal event anaphora.<p>
 * 
 * It predicts agreement if the coreferential mention and the candidate referent have predicate
 * objects associated with them sharing semantic types and they are morphologically related.
 * For example, it can predict agreement between the noun phrase <i>this phosphorylation</i>
 * and a verbal phrase headed by<i>phosphorylated</i>, assuming Phosphorylation predicate
 * type is used.
 * 
 * @author Halil Kilicoglu
 *
 */
// TODO Needs more work to be useful.
public class PredicateAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		Set<SemanticItem> expItems = exp.filterByPredicates();
		Set<SemanticItem> refItems = referent.filterByPredicates();
		if (expItems == null || refItems == null) return false;
		if (!morphologicallyRelated(exp,referent)) return false;
		for (SemanticItem a: expItems) {
			Predicate expPred = (Predicate)a;
			for (SemanticItem c : refItems) {
				/// perhaps more accurate to match on IndicatorSense?
				Predicate refPred = (Predicate)c;
				if (refPred.getType().equals(expPred.getType())) 
					return true;
			}
		}
		return false;
	}
	
	// determines whether two textual units are morphologically related
	// currently based on WordNet, but I am not sure how well it works
	// SPECIALIST LEXICON could be better.
	private static boolean morphologicallyRelated(SurfaceElement exp, SurfaceElement ref) {
		String expLe = exp.getHead().getLemma();
		// better precision perhaps with just matching the head of the candidate
		List<String> refLes = new ArrayList<>();
		for (Word w: ref.toWordList()) {
			refLes.add(w.getLemma());
		}
		if (refLes.contains(expLe)) return true;
		if (exp.getHead().isNominal())  {
			String denominal = WordNetWrapper.getDenominal(exp);
			if (denominal == null) return false;
			if (refLes.contains(denominal)) return true;
		}
		return false;
	}

}
