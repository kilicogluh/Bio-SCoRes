package gov.nih.nlm.bioscores.agreement;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.Conjunction;
import gov.nih.nlm.ling.sem.ConjunctionDetection;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.util.SemUtils;

import java.util.ArrayList;
import java.util.Set;

/**
 * Agreement constraint based on semantic type equality. It is applicable
 * to nominal coreference. <p>
 * 
 * Agreement is predicted if the coreferential mention and the candidate referent
 * share semantic types.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SemanticTypeAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		if (!exp.hasSemantics() || !referent.hasSemantics()) return false;
		if (SemUtils.semTypeEquality(exp, referent,true)) return true;
		Set<SemanticItem> conjs = ConjunctionDetection.filterByConjunctions(referent);
		if (conjs.size() > 0) {
			Conjunction conjPred = (Conjunction)conjs.iterator().next();
			return (SemUtils.conjSemTypeEquality(new ArrayList<>(exp.getSemantics()), 
					new ArrayList<>(ConjunctionDetection.conjunctionSemanticTypes(conjPred))));
		}
		return false;
	}
	
}
