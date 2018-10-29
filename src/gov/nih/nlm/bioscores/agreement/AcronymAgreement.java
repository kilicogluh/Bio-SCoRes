package gov.nih.nlm.bioscores.agreement;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.util.StringUtils;

/**
 * Approximation of one of the precise constructs in Stanford DCoref sieve #4. 
 * This agreement constraint is applicable to nominal mentions. <p>
 * 
 * Agreement is predicted if the mention is an acronym of the candidate referent.
 * 
 * @author Halil Kilicoglu
 */
// TODO Currently unused and needs work to be useful. 
// TODO We could also just use an existing acronym recognition tool, like Hearst's.
public class AcronymAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		if (ExpressionType.PRONOMINAL_TYPES.contains(expType)) return false;
		int refSize = referent.toWordList().size();
		int expSize = exp.toWordList().size();
		SurfaceElement shorter = null;
		SurfaceElement longer = null;
		if (refSize == 1 && expSize > 1)  {
			shorter = referent;
			longer = exp;
		} else if (expSize == 1 && refSize > 1) {
			shorter = exp;
			longer = referent;
		}
		if (shorter == null || longer == null ||
			StringUtils.containsUpperCase(shorter.getText()) == false ||
			shorter.getText().length() > 5) return false;
		StringBuffer buf = new StringBuffer();
		for (Word w: longer.toWordList()) {
			if (w.isDeterminer() || w.isPronominal()) continue;
			char first = w.getText().charAt(0);
			buf.append(Character.toUpperCase(first));
		}
		String acr = buf.toString();
		if (acr.length() < 2) return false;
		buf = new StringBuffer();
		for (int i=0; i <shorter.getText().length(); i++ ) {
			char c = shorter.getText().charAt(i);
			if (c >= 'A' && c <= 'Z') 
				buf.append(c);
		}
		
		return buf.length() > 1 && acr.equals(buf.toString());
	}
	

}
