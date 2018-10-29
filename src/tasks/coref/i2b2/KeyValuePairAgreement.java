package tasks.coref.i2b2;

import java.util.List;

import gov.nih.nlm.bioscores.agreement.Agreement;
import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SurfaceElement;

/**
 * Implementation of a simple agreement measure that predicts compatibility between key-value pairs,
 * as in <i>Attending: I BUN, M.D.</i>, where <i>I BUN, M.D.</i> is the name of the attending physician.
 * 
 * It looks for comma between the potentially coreferential textual units.
 * 
 * @author Halil Kilicoglu
 *
 */
public class KeyValuePairAgreement implements Agreement {

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		if (ExpressionType.PRONOMINAL_TYPES.contains(expType)) return false;
		int end = exp.getSpan().getBegin();
		int begin = referent.getSpan().getEnd();
		Document doc = exp.getSentence().getDocument();
		List<SurfaceElement> intvs = doc.getSurfaceElementsInSpan(new Span(begin,end));
		return (intvs.size() == 1 && intvs.get(0).containsToken(":"));
	}
}
