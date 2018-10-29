package gov.nih.nlm.bioscores.agreement;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import gov.nih.nlm.bioscores.core.CoreferenceType;
import gov.nih.nlm.bioscores.core.ExpressionType;
import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.core.SpanList;
import gov.nih.nlm.ling.core.SurfaceElement;
import gov.nih.nlm.ling.core.Word;
import gov.nih.nlm.ling.sem.SemanticItem;
import gov.nih.nlm.ling.util.DiscourseUtils;

/**
 * A limited implementation of agreement based on sentence-initial discourse connectives
 * that indicate referents in the same sentence. It is useful for {@link CoreferenceType#Cataphora}
 * resolution of pronominal mentions. <p>
 * 
 * Agreement is predicted if there is a sentence-initial discourse connective followed by a 
 * coreferential mention and there is a comma between the mention and the candidate referent
 * in the sentence. 
 * 
 * It is currently limited to a few discourse connectives (<i>because, although, since</i>).
 * 
 * @author Halil Kilicoglu
 *
 */
public class DiscourseConnectiveAgreement implements Agreement {

	public static final List<String> SENTENCE_INITIAL_DISCOURSE_CONNECTIVES = Arrays.asList("because", "although","since");

	@Override
	public boolean agree(CoreferenceType corefType, ExpressionType expType, SurfaceElement exp, SurfaceElement referent) {
		if (ExpressionType.PRONOMINAL_TYPES.contains(expType) == false || 
			corefType != CoreferenceType.Cataphora ||
			DiscourseUtils.sameSentence(exp, referent) == false) return false;
		Sentence expSent = exp.getSentence();
		if (expSent.getWords().size() <4) return false;
		Word first = expSent.getWords().get(0);
		if (SENTENCE_INITIAL_DISCOURSE_CONNECTIVES.contains(first.getText().toLowerCase()) == false) return false;
		Document doc = expSent.getDocument();
		List<SurfaceElement> intv = expSent.getSurfaceElementsFromSpan(new Span(exp.getSpan().getEnd(),referent.getSpan().getBegin()));
		LinkedHashSet<SemanticItem> beforeExp = Document.getSemanticItemsBySpan(doc,
				new SpanList(expSent.getWords().get(1).getSpan().getBegin(),exp.getSpan().getBegin()) , false);
		if (beforeExp.size() > 0) return false;
		// require a comma somewhere between the referent and the mention (a bit adhoc)
		for (SurfaceElement in: intv) {
			if (in.getText().equals(",")) return true;
		}
		return false;
	}
	
}
