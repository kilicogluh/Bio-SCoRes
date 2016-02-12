package gov.nih.nlm.bioscores.agreement;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * Implementation of head word agreement constraint between 
 * the coreferential mention and the candidate referent. <p>
 * Agreement is predicted if the head lemmas of the mention and the
 * referent match.
 * 
 * @author Halil Kilicoglu
 *
 */
public class HeadWordAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		String headLemma = exp.getHead().getLemma();
		String candLemma = referent.getHead().getLemma();
		return candLemma.equalsIgnoreCase(headLemma);

	}

}
