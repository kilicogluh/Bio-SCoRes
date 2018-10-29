package gov.nih.nlm.bioscores.agreement;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * Agreement based on exact string match of the mention and the referent.
 * Same as Stanford DCoref sieve #2.
 * 
 * @author Halil Kilicoglu
 *
 */
public class ExactStringAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		return exp.getText().equalsIgnoreCase(referent.getText());
	}

}
