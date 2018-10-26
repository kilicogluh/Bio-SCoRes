package gov.nih.nlm.bioscores.agreement;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;

/**
 * Implementation of one of the precise constructs in Stanford DCoref sieve #9.
 * It can be useful for nominal coreference, but is currently unused. <p>
 * 
 * Agreement is predicted if the head of the coreferential mention is found in
 * the candidate referent.
 * 
 * @author Halil Kilicoglu
 *
 */
public class RelaxedHeadMatchAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		if (exp.isNominal() == false || referent.isNominal() == false) return false;
		Word expHead = exp.getHead();
		boolean hasHead = false;
		for (Word w: referent.toWordList()) {
			if (w.getText().equalsIgnoreCase(expHead.getText())) hasHead = true;
		}
		return hasHead;
		// the sieve requires semtype equality, but this should probably just be run in addition to those agreement methods
	}

}
