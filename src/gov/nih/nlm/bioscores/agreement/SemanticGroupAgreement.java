package gov.nih.nlm.bioscores.agreement;

import java.util.ArrayList;
import java.util.Set;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.sem.Conjunction;
import gov.nih.nlm.ling.sem.ConjunctionDetection;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.util.SemUtils;

/**
 * Agreement constraint based on semantic group equality. It is applicable
 * to nominal coreference. <p>
 * 
 * Agreement is predicted if the coreferential mention and the candidate referent
 * share semantic groups.
 * 
 * @author Halil Kilicoglu
 *
 */
public class SemanticGroupAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		if (!exp.hasSemantics() || !referent.hasSemantics()) return false;
		if (SemUtils.matchingSemGroup(exp,referent,true) != null) return true;
		Set<SemanticItem> conjs = ConjunctionDetection.filterByConjunctions(referent);
		if (conjs.size() > 0) {
			Conjunction conjPred = (Conjunction)conjs.iterator().next();
			return (SemUtils.conjSemGroupEquality(new ArrayList<>(exp.getSemantics()), 
					new ArrayList<>(ConjunctionDetection.conjunctionSemanticGroups(conjPred))));
		}
		return false;
	}

	
}
