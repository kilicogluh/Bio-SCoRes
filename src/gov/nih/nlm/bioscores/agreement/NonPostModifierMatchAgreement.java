package gov.nih.nlm.bioscores.agreement;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;

/**
 * Implementation of Stanford DCoref sieve #3. It is useful for nominal mentions. <p>
 * Agreement is predicted if the coreferential mention and the candidate referent 
 * have modifiers (non-determiner and non-pronominal) that match.
 * 
 * @author Halil Kilicoglu
 *
 */
public class NonPostModifierMatchAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
//		if (exp.isNominal() == false || referent.isNominal() == false) return false;
		if (ExpressionType.PRONOMINAL_TYPES.contains(expType)) return false;
		Document doc = exp.getSentence().getDocument();
		Word expHead = exp.getHead();
		Word refHead = referent.getHead();
		int expHeadEnd = expHead.getSpan().getEnd();
		int refHeadEnd = refHead.getSpan().getEnd();		
		int expNonBeg = exp.getSpan().getBegin();
		for (Word w: exp.toWordList()) {
			if (w.isDeterminer() || w.isPronominal()) continue;
			expNonBeg = w.getSpan().getBegin();
			break;
		}
		int refNonBeg = referent.getSpan().getBegin();
		for (Word w: referent.toWordList()) {
			if (w.isDeterminer() || w.isPronominal()) continue;
			refNonBeg = w.getSpan().getBegin();
			break;
		}

		// part-of-speech error likely (his IV), IV is CD.
		if (expNonBeg > expHeadEnd) {
			expHeadEnd = exp.getSpan().getEnd();
		}
		if (refNonBeg > refHeadEnd) {
			refHeadEnd = referent.getSpan().getEnd();
		}
		String expStr = doc.getText().substring(expNonBeg,expHeadEnd);
		String refStr = doc.getText().substring(refNonBeg,refHeadEnd);
		return refStr.equalsIgnoreCase(expStr);
	}

}
